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

/**
 * Fetch Real Telegram Dialogs/Chats
 */
export async function fetchTelegramDialogs(limit = 30) {
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
export async function sendRealTelegramMessage(peerId: string, message: string) {
  if (!activeClient || !isLiveConnected) {
    return null;
  }

  try {
    // If peerId starts with tg_, strip it
    const cleanPeer = peerId.startsWith('tg_') ? peerId.replace('tg_', '') : peerId;
    const sent = await activeClient.sendMessage(cleanPeer, { message });
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
export async function fetchTelegramMessages(peerId: string, limit = 40) {
  if (!activeClient || !isLiveConnected) {
    return null;
  }

  try {
    const cleanPeer = peerId.startsWith('tg_') ? peerId.replace('tg_', '') : peerId;
    const messages = await activeClient.getMessages(cleanPeer, { limit });
    return messages.map(msg => ({
      id: msg.id.toString(),
      text: msg.message || '',
      time: msg.date ? new Date(msg.date * 1000).toISOString() : new Date().toISOString(),
      outgoing: Boolean(msg.out),
      read: true,
    })).reverse();
  } catch (err: any) {
    console.warn('[TelegramService] Error fetching messages for peer:', peerId, err?.message);
    return null;
  }
}
