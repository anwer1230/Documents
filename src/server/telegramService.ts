import { TelegramClient, Api } from 'telegram';
import { StringSession } from 'telegram/sessions/index.js';
import { computeCheck } from 'telegram/Password.js';
import fs from 'fs';
import path from 'path';

// Production Telegram API Credentials
export const TELEGRAM_API_ID = Number(process.env.TELEGRAM_API_ID) || 22043994;
export const TELEGRAM_API_HASH = process.env.TELEGRAM_API_HASH || '56f64582b363d367280db96586b97801';

const SESSIONS_FILE = path.join(process.cwd(), 'data', 'telegram_sessions.json');

export interface TelegramUser {
  id: string;
  name: string;
  username?: string;
  phone: string;
  avatarUrl?: string;
}

export interface ChatDetails {
  id: string;
  participantsCount?: number;
  onlineCount?: number;
  about?: string;
  isChannel?: boolean;
  isGroup?: boolean;
  avatarUrl?: string;
}

export interface PendingAuth {
  client: TelegramClient;
  phone: string;
  phoneCodeHash: string;
  isCodeViaApp: boolean;
  createdAt: number;
}

export interface StoredSession {
  phone: string;
  sessionString: string;
  name: string;
  username?: string;
  userId?: string;
  savedAt: string;
}

// In-memory runtime state
let activeClient: TelegramClient | null = null;
let activeUser: TelegramUser | null = null;
let isLiveConnected = false;

const pendingAuths = new Map<string, PendingAuth>();

/**
 * Load stored sessions from disk
 */
function loadStoredSessions(): Record<string, StoredSession> {
  try {
    if (fs.existsSync(SESSIONS_FILE)) {
      const content = fs.readFileSync(SESSIONS_FILE, 'utf-8');
      const parsed = JSON.parse(content);
      return parsed?.sessions || {};
    }
  } catch (error) {
    console.error('[TelegramService] Error reading sessions file:', error);
  }
  return {};
}

/**
 * Save sessions to disk
 */
function saveStoredSessions(sessions: Record<string, StoredSession>) {
  try {
    const dir = path.dirname(SESSIONS_FILE);
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }
    fs.writeFileSync(SESSIONS_FILE, JSON.stringify({ sessions, updatedAt: new Date().toISOString() }, null, 2), 'utf-8');
  } catch (error) {
    console.error('[TelegramService] Error saving sessions file:', error);
  }
}

/**
 * Initialize Telegram Service on Server Startup
 */
export async function initTelegramService() {
  console.log(`[TelegramService] Initializing MTProto Client with API_ID: ${TELEGRAM_API_ID}`);
  const stored = loadStoredSessions();
  const sessionEntries = Object.values(stored);

  if (sessionEntries.length > 0) {
    const primary = sessionEntries[0];
    try {
      console.log(`[TelegramService] Found saved session for ${primary.phone}, attempting restore...`);
      const client = new TelegramClient(new StringSession(primary.sessionString), TELEGRAM_API_ID, TELEGRAM_API_HASH, {
        connectionRetries: 5,
        useWSS: false,
      });

      await client.connect();
      const isAuth = await client.isUserAuthorized();
      if (isAuth) {
        const me = (await client.getMe()) as any;
        activeClient = client;
        activeUser = {
          id: me.id?.toString() || primary.userId || 'tg_user',
          name: `${me.firstName || ''} ${me.lastName || ''}`.trim() || primary.name || 'حساب تيليجرام',
          username: me.username ? `@${me.username}` : primary.username,
          phone: me.phone ? `+${me.phone.replace(/^\+/, '')}` : primary.phone,
          avatarUrl: '/api/telegram/avatar/me',
        };
        isLiveConnected = true;
        console.log(`[TelegramService] Successfully restored live session for ${activeUser.name} (${activeUser.phone})`);
        return;
      }
    } catch (err: any) {
      console.warn(`[TelegramService] Failed to restore session for ${primary.phone}:`, err?.message || err);
    }
  }
}

/**
 * Normalize phone number format (e.g. +966501234567)
 */
export function normalizePhone(rawPhone: string): string {
  let cleaned = rawPhone.replace(/[^\d+]/g, '');
  if (!cleaned.startsWith('+')) {
    cleaned = `+${cleaned}`;
  }
  return cleaned;
}

/**
 * Start Telegram Authentication (Request Code)
 */
export async function startPhoneAuth(sessionId: string, rawPhone: string) {
  const cleanPhone = normalizePhone(rawPhone);
  if (cleanPhone.length < 8) {
    throw new Error('PHONE_NUMBER_INVALID');
  }

  // Clean up any existing pending auth for this session
  const existing = pendingAuths.get(sessionId);
  if (existing) {
    try {
      await existing.client.disconnect();
    } catch {
      // ignore
    }
    pendingAuths.delete(sessionId);
  }

  console.log(`[TelegramService] Initiating sendCode for ${cleanPhone} via Telegram API ID: ${TELEGRAM_API_ID}...`);

  const client = new TelegramClient(new StringSession(''), TELEGRAM_API_ID, TELEGRAM_API_HASH, {
    connectionRetries: 5,
    useWSS: false,
  });

  await client.connect();

  try {
    const sendResult = await client.sendCode(
      {
        apiId: TELEGRAM_API_ID,
        apiHash: TELEGRAM_API_HASH,
      },
      cleanPhone
    );

    pendingAuths.set(sessionId, {
      client,
      phone: cleanPhone,
      phoneCodeHash: sendResult.phoneCodeHash,
      isCodeViaApp: sendResult.isCodeViaApp,
      createdAt: Date.now(),
    });

    console.log(`[TelegramService] Code sent successfully to ${cleanPhone}. isCodeViaApp=${sendResult.isCodeViaApp}`);

    return {
      state: 'code' as const,
      isCodeViaApp: sendResult.isCodeViaApp,
      phone: cleanPhone,
      apiId: TELEGRAM_API_ID,
    };
  } catch (err: any) {
    try {
      await client.disconnect();
    } catch {
      // ignore
    }

    const errCode = err.errorMessage || err.message || 'REQUEST_FAILED';
    console.error(`[TelegramService] sendCode failed for ${cleanPhone}:`, errCode);

    if (errCode.includes('PHONE_NUMBER_INVALID')) throw new Error('PHONE_NUMBER_INVALID');
    if (errCode.includes('PHONE_NUMBER_BANNED')) throw new Error('PHONE_NUMBER_BANNED');
    if (errCode.includes('FLOOD_WAIT')) throw new Error('FLOOD_WAIT');
    throw new Error(errCode);
  }
}

/**
 * Verify Telegram Authentication Code
 */
export async function verifyAuthCode(sessionId: string, code: string) {
  const pending = pendingAuths.get(sessionId);
  if (!pending) {
    throw new Error('AUTH_SESSION_EXPIRED');
  }

  const cleanCode = code.trim();
  if (!cleanCode) {
    throw new Error('PHONE_CODE_INVALID');
  }

  console.log(`[TelegramService] Verifying code for ${pending.phone}...`);

  try {
    await pending.client.invoke(
      new Api.auth.SignIn({
        phoneNumber: pending.phone,
        phoneCodeHash: pending.phoneCodeHash,
        phoneCode: cleanCode,
      })
    );

    // If successful, retrieve user profile
    const me = (await pending.client.getMe()) as any;
    const sessionString = (pending.client.session as any).save();

    const user: TelegramUser = {
      id: me.id?.toString() || `tg_${Date.now()}`,
      name: `${me.firstName || ''} ${me.lastName || ''}`.trim() || 'حساب تيليجرام',
      username: me.username ? `@${me.username}` : undefined,
      phone: pending.phone,
      avatarUrl: '/api/telegram/avatar/me',
    };

    // Save session
    const stored = loadStoredSessions();
    stored[pending.phone] = {
      phone: pending.phone,
      sessionString,
      name: user.name,
      username: user.username,
      userId: user.id,
      savedAt: new Date().toISOString(),
    };
    saveStoredSessions(stored);

    activeClient = pending.client;
    activeUser = user;
    isLiveConnected = true;
    pendingAuths.delete(sessionId);

    console.log(`[TelegramService] Authentication successful for ${user.name} (${user.phone})!`);

    return {
      state: 'ready' as const,
      authenticated: true,
      user,
      isLiveConnected: true,
      apiId: TELEGRAM_API_ID,
    };
  } catch (err: any) {
    const errCode = err.errorMessage || err.message || '';
    console.error(`[TelegramService] Code verification error for ${pending.phone}:`, errCode);

    if (errCode === 'SESSION_PASSWORD_NEEDED') {
      console.log(`[TelegramService] Account ${pending.phone} requires Two-Factor Authentication (2FA) password.`);
      return {
        state: 'password' as const,
        authenticated: false,
      };
    }

    if (errCode.includes('PHONE_CODE_INVALID')) throw new Error('PHONE_CODE_INVALID');
    if (errCode.includes('PHONE_CODE_EXPIRED')) throw new Error('PHONE_CODE_EXPIRED');
    if (errCode.includes('FLOOD_WAIT')) throw new Error('FLOOD_WAIT');
    throw new Error(errCode || 'PHONE_CODE_INVALID');
  }
}

/**
 * Verify 2FA Password
 */
export async function verify2FAPassword(sessionId: string, password: string) {
  const pending = pendingAuths.get(sessionId);
  if (!pending) {
    throw new Error('AUTH_SESSION_EXPIRED');
  }

  console.log(`[TelegramService] Verifying 2FA password for ${pending.phone}...`);

  try {
    const passwordSrpResult = await pending.client.invoke(new Api.account.GetPassword());
    const passwordSrpCheck = await computeCheck(passwordSrpResult, password);

    await pending.client.invoke(
      new Api.auth.CheckPassword({
        password: passwordSrpCheck,
      })
    );

    const me = (await pending.client.getMe()) as any;
    const sessionString = (pending.client.session as any).save();

    const user: TelegramUser = {
      id: me.id?.toString() || `tg_${Date.now()}`,
      name: `${me.firstName || ''} ${me.lastName || ''}`.trim() || 'حساب تيليجرام',
      username: me.username ? `@${me.username}` : undefined,
      phone: pending.phone,
      avatarUrl: '/api/telegram/avatar/me',
    };

    const stored = loadStoredSessions();
    stored[pending.phone] = {
      phone: pending.phone,
      sessionString,
      name: user.name,
      username: user.username,
      userId: user.id,
      savedAt: new Date().toISOString(),
    };
    saveStoredSessions(stored);

    activeClient = pending.client;
    activeUser = user;
    isLiveConnected = true;
    pendingAuths.delete(sessionId);

    console.log(`[TelegramService] 2FA authentication successful for ${user.name} (${user.phone})!`);

    return {
      state: 'ready' as const,
      authenticated: true,
      user,
      isLiveConnected: true,
      apiId: TELEGRAM_API_ID,
    };
  } catch (err: any) {
    const errCode = err.errorMessage || err.message || '';
    console.error(`[TelegramService] 2FA password verification error:`, errCode);

    if (errCode.includes('PASSWORD_HASH_INVALID')) throw new Error('PASSWORD_HASH_INVALID');
    if (errCode.includes('FLOOD_WAIT')) throw new Error('FLOOD_WAIT');
    throw new Error(errCode || 'PASSWORD_HASH_INVALID');
  }
}

/**
 * Logout from Telegram
 */
export async function logoutTelegram() {
  if (activeClient) {
    try {
      await activeClient.disconnect();
    } catch {
      // ignore
    }
  }
  activeClient = null;
  activeUser = null;
  isLiveConnected = false;

  // Clear saved sessions
  saveStoredSessions({});
  console.log('[TelegramService] Logged out successfully and sessions cleared.');
}

/**
 * Get current Telegram status
 */
export function getTelegramServiceStatus() {
  return {
    isLiveConnected,
    apiId: TELEGRAM_API_ID,
    apiHashConfigured: Boolean(TELEGRAM_API_HASH),
    user: activeUser,
    hasActiveClient: Boolean(activeClient),
  };
}

const avatarCache = new Map<string, { buffer: Buffer; mime: string; expires: number }>();
const peerEntityCache = new Map<string, any>();

/**
 * Fetch profile photo buffer for a user or chat
 */
export async function fetchProfilePhotoBuffer(peerId: string): Promise<{ buffer: Buffer; mime: string } | null> {
  if (!activeClient || !isLiveConnected) return null;

  const cleanPeer = peerId.startsWith('tg_') ? peerId.replace('tg_', '') : peerId;
  const cached = avatarCache.get(cleanPeer);
  if (cached && cached.expires > Date.now()) {
    return { buffer: cached.buffer, mime: cached.mime };
  }

  try {
    let target: any = cleanPeer;
    if (cleanPeer === 'me') {
      target = 'me';
    } else if (peerEntityCache.has(cleanPeer)) {
      target = peerEntityCache.get(cleanPeer);
    } else {
      try {
        if (/^-?\d+$/.test(cleanPeer)) {
          target = await activeClient.getEntity(BigInt(cleanPeer) as any);
        } else {
          target = await activeClient.getEntity(cleanPeer);
        }
      } catch {
        target = cleanPeer;
      }
    }

    const buffer = await activeClient.downloadProfilePhoto(target, { isBig: false });
    if (buffer && buffer.length > 0) {
      const result = { buffer: Buffer.from(buffer), mime: 'image/jpeg', expires: Date.now() + 1000 * 60 * 30 };
      avatarCache.set(cleanPeer, result);
      return result;
    }
  } catch (err: any) {
    // Entities without photo or access restrictions
  }
  return null;
}

const TELEGRAM_SENDER_COLORS = [
  '#e56555', // Red
  '#e08244', // Orange
  '#a667e5', // Violet
  '#439fe0', // Blue
  '#4fae4e', // Green
  '#c45479', // Pink
  '#3ca3b5', // Cyan
];

/**
 * Fetch Real Telegram Dialogs/Chats
 */
export async function fetchTelegramDialogs(limit = 40) {
  if (!activeClient || !isLiveConnected) {
    return null;
  }

  try {
    const dialogs = await activeClient.getDialogs({ limit });
    return dialogs.map(dialog => {
      const entity = dialog.entity as any;
      const isChannel = Boolean(dialog.isChannel);
      const isGroup = Boolean(dialog.isGroup);
      const name = dialog.title || dialog.name || (entity?.firstName ? `${entity.firstName} ${entity.lastName || ''}`.trim() : 'محادثة تيليجرام');
      const unread = dialog.unreadCount || 0;
      const preview = dialog.message?.message || (dialog.message as any)?.text || '';
      const date = dialog.date ? new Date(dialog.date * 1000).toLocaleTimeString('ar-SA', { hour: '2-digit', minute: '2-digit' }) : null;
      const hasPhoto = Boolean(entity?.photo);
      const avatarPeerId = entity?.id ? entity.id.toString() : dialog.id.toString();
      const avatarUrl = hasPhoto ? `/api/telegram/avatar/${avatarPeerId}` : undefined;

      // Cache entity for instant avatar resolution
      if (dialog.id) peerEntityCache.set(String(dialog.id), entity);
      if (entity?.id) peerEntityCache.set(String(entity.id), entity);

      const participantsCount = entity?.participantsCount ?? entity?.participants?.length ?? undefined;

      return {
        id: `tg_${dialog.id}`,
        name,
        preview: preview.slice(0, 80) || 'محادثة نشطة',
        time: date,
        unread,
        pinned: Boolean(dialog.pinned),
        archived: Boolean(dialog.archived),
        kind: isChannel ? 'channel' : isGroup ? 'group' : 'direct',
        isRealTelegram: true,
        avatarUrl,
        participantsCount,
      };
    });
  } catch (err) {
    console.warn('[TelegramService] Error fetching dialogs from Telegram:', err);
    return null;
  }
}

/**
 * Send real message via Telegram MTProto
 */
export async function sendRealTelegramMessage(peerId: string, message: string, replyToMsgId?: number) {
  if (!activeClient || !isLiveConnected) {
    return null;
  }

  try {
    const cleanPeer = peerId.startsWith('tg_') ? peerId.replace('tg_', '') : peerId;
    const sent = await activeClient.sendMessage(cleanPeer, {
      message,
      replyTo: replyToMsgId ? Number(replyToMsgId) : undefined,
    });
    return {
      id: sent.id.toString(),
      text: sent.message,
      time: new Date().toISOString(),
      outgoing: true,
      read: true,
    };
  } catch (err: any) {
    console.warn('[TelegramService] Error sending message via Telegram:', err);
    return null;
  }
}

/**
 * Fetch real messages for a Telegram chat/dialog
 */
export async function fetchTelegramMessages(peerId: string, limit = 50) {
  if (!activeClient || !isLiveConnected) {
    return null;
  }

  try {
    const cleanPeer = peerId.startsWith('tg_') ? peerId.replace('tg_', '') : peerId;
    const messages = await activeClient.getMessages(cleanPeer, { limit });

    return messages.map(msg => {
      let senderName = '';
      let senderId = '';
      let senderAvatar: string | undefined = undefined;
      let senderColor = TELEGRAM_SENDER_COLORS[0];
      let senderInitials = 'ت';

      const sender = (msg as any).sender;
      if (sender) {
        senderId = sender.id?.toString() || '';
        senderName = sender.firstName
          ? `${sender.firstName} ${sender.lastName || ''}`.trim()
          : (sender.title || sender.username || '');
        if (sender.photo) {
          senderAvatar = `/api/telegram/avatar/${senderId}`;
        }
      } else if ((msg as any).fromId) {
        const from = (msg as any).fromId;
        senderId = (from.userId || from.channelId || from.chatId)?.toString() || '';
      }

      if (!senderName && msg.out && activeUser) {
        senderName = activeUser.name;
        senderId = activeUser.id;
      }

      if (senderName) {
        senderInitials = senderName.split(/\s+/).filter(Boolean).slice(0, 2).map(w => w[0]).join('') || 'ت';
      }

      if (senderId) {
        const colorIndex = Math.abs([...senderId].reduce((acc, c) => acc + c.charCodeAt(0), 0)) % TELEGRAM_SENDER_COLORS.length;
        senderColor = TELEGRAM_SENDER_COLORS[colorIndex];
        if (!senderAvatar) {
          senderAvatar = `/api/telegram/avatar/${senderId}`;
        }
      }

      // Quoted reply info
      let replyTo: { id: string; senderName?: string; text: string } | undefined = undefined;
      const replyHeader = (msg as any).replyTo;
      if (replyHeader && replyHeader.replyToMsgId) {
        const replyId = replyHeader.replyToMsgId.toString();
        const replied = messages.find(m => m.id.toString() === replyId);
        if (replied) {
          const rSender = (replied as any).sender;
          const rName = rSender?.firstName
            ? `${rSender.firstName} ${rSender.lastName || ''}`.trim()
            : (rSender?.title || 'رسالة');
          replyTo = {
            id: replyId,
            senderName: rName,
            text: (replied.message || '').slice(0, 70),
          };
        } else {
          replyTo = {
            id: replyId,
            senderName: 'رد على رسالة',
            text: '...',
          };
        }
      }

      return {
        id: msg.id.toString(),
        text: msg.message || '',
        time: msg.date ? new Date(msg.date * 1000).toISOString() : new Date().toISOString(),
        outgoing: Boolean(msg.out),
        read: true,
        senderName: senderName || undefined,
        senderId: senderId || undefined,
        senderAvatar,
        senderColor,
        senderInitials,
        replyTo,
      };
    }).reverse();
  } catch (err: any) {
    console.warn('[TelegramService] Error fetching messages for peer:', peerId, err?.message);
    return null;
  }
}

/**
 * Fetch detailed chat information (subscribers, online members count, about/bio)
 */
export async function fetchTelegramChatDetails(chatId: string): Promise<ChatDetails | null> {
  if (!activeClient || !isLiveConnected) return null;

  const cleanPeer = chatId.startsWith('tg_') ? chatId.replace('tg_', '') : chatId;

  try {
    let entity: any = peerEntityCache.get(cleanPeer);
    if (!entity) {
      try {
        if (/^-?\d+$/.test(cleanPeer)) {
          entity = await activeClient.getEntity(BigInt(cleanPeer) as any);
        } else {
          entity = await activeClient.getEntity(cleanPeer);
        }
      } catch {
        entity = await activeClient.getInputEntity(cleanPeer);
      }
    }

    let participantsCount: number | undefined;
    let onlineCount: number | undefined;
    let about: string | undefined;

    if (entity?.className === 'Channel' || entity?.broadcast || entity?.megagroup) {
      try {
        const full = (await activeClient.invoke(new Api.channels.GetFullChannel({ channel: entity }))) as any;
        const fullChat = full?.fullChat;
        participantsCount = fullChat?.participantsCount ?? entity?.participantsCount;
        onlineCount = fullChat?.onlineCount;
        about = fullChat?.about;
      } catch (e: any) {
        console.warn('[TelegramService] GetFullChannel error:', e?.message);
        participantsCount = entity?.participantsCount;
      }
    } else if (entity?.className === 'Chat') {
      try {
        const full = (await activeClient.invoke(new Api.messages.GetFullChat({ chatId: entity.id }))) as any;
        const fullChat = full?.fullChat;
        const participants = fullChat?.participants?.participants || [];
        participantsCount = participants.length;
        if (full?.users) {
          onlineCount = full.users.filter((u: any) => u.status?.className === 'UserStatusOnline').length;
        }
        about = fullChat?.about;
      } catch (e: any) {
        console.warn('[TelegramService] GetFullChat error:', e?.message);
      }
    } else if (entity?.className === 'User') {
      try {
        const full = (await activeClient.invoke(new Api.users.GetFullUser({ id: entity }))) as any;
        about = full?.fullUser?.about;
        if (entity.status?.className === 'UserStatusOnline') {
          onlineCount = 1;
        }
      } catch (e: any) {
        console.warn('[TelegramService] GetFullUser error:', e?.message);
      }
    }

    const peerIdStr = entity?.id ? entity.id.toString() : cleanPeer;
    return {
      id: chatId,
      participantsCount,
      onlineCount,
      about,
      isChannel: Boolean(entity?.broadcast),
      isGroup: Boolean(entity?.megagroup || entity?.className === 'Chat'),
      avatarUrl: entity?.photo ? `/api/telegram/avatar/${peerIdStr}` : undefined,
    };
  } catch (err: any) {
    console.warn(`[TelegramService] Error getting full chat details for ${chatId}:`, err?.message);
    return null;
  }
}

