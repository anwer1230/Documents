import { TelegramClient, Api } from 'telegram';
import { StringSession } from 'telegram/sessions/index.js';
import { LogLevel } from 'telegram/extensions/Logger.js';
import fs from 'fs';
import path from 'path';
import { ensureTelegramPatch } from './patchTelegram.js';
import { sqliteDatabase } from './sqliteService.js';

// Ensure 256-bit DH key padding patch is present
ensureTelegramPatch();

/**
 * FloodWaitQueue: Resilient rate-limiter and exponential backoff queue for MTProto FLOOD_WAIT
 */
export class FloodWaitQueue {
  public static async executeWithFloodRetry<T>(
    key: string,
    operation: () => Promise<T>,
    maxRetries = 3
  ): Promise<T> {
    let attempt = 0;
    while (attempt < maxRetries) {
      try {
        return await operation();
      } catch (err: any) {
        attempt++;
        let waitSeconds = 0;
        if (typeof err?.seconds === 'number' && err.seconds > 0) {
          waitSeconds = err.seconds;
        } else if (typeof err?.errorMessage === 'string' && err.errorMessage.startsWith('FLOOD_WAIT_')) {
          waitSeconds = parseInt(err.errorMessage.replace('FLOOD_WAIT_', ''), 10) || 2;
        } else if (typeof err?.message === 'string' && err.message.includes('FLOOD_WAIT_')) {
          const match = err.message.match(/FLOOD_WAIT_(\d+)/);
          if (match) waitSeconds = parseInt(match[1], 10);
        }

        if (waitSeconds > 0 && attempt < maxRetries) {
          const jitterMs = Math.floor(Math.random() * 500) + 150;
          const totalWaitMs = waitSeconds * 1000 + jitterMs;
          console.warn(
            `[FloodWaitQueue] FLOOD_WAIT caught (${waitSeconds}s) for [${key}]. Auto-pausing for ${totalWaitMs}ms (attempt ${attempt}/${maxRetries})...`
          );
          await new Promise((resolve) => setTimeout(resolve, totalWaitMs));
          continue;
        }
        throw err;
      }
    }
    throw new Error(`[FloodWaitQueue] Max retries (${maxRetries}) exceeded for ${key}`);
  }
}

// System configuration constants
export const TELEGRAM_API_ID = Number(process.env.TELEGRAM_API_ID || 22043994);
export const TELEGRAM_API_HASH = process.env.TELEGRAM_API_HASH || '56f64582b363d367280db96586b97801';
export const VAPID_PUBLIC_KEY = process.env.VAPID_PUBLIC_KEY || 'BE36BmheMRx2GxzjWpp_4bmXq_hZg55bP_M_vNVysfnjTxns9VCI0hiCHgnRBx0URe_LoxWaAgrS9G9QZbQhOh8';
export const VAPID_PRIVATE_KEY = process.env.VAPID_PRIVATE_KEY || '13NU1_GmeL7bDQcVtlFyuKqsnnsX3XkOyE--2rAQJw4';
export const VAPID_SUBJECT = process.env.VAPID_SUBJECT || 'mailto:anwrfwad178@gmail.com';

export const MAX_TELEGRAM_ACCOUNTS = 6;

export interface SavedAccount {
  id: string;
  sessionToken: string;
  user: {
    id: string;
    firstName: string;
    lastName?: string;
    username?: string;
    phone?: string;
    photoUrl?: string;
    isBot?: boolean;
    status?: string;
  };
  addedAt: number;
}

interface MultiAccountStorage {
  sessions: Record<string, string>;
  accounts: SavedAccount[];
  activeAccountId?: string;
}

interface ActiveSession {
  client: TelegramClient;
  phoneCodeHash?: string;
  phoneNumber?: string;
  isLoggedIn: boolean;
  user?: any;
  createdAt: number;
  entityCache?: Map<string, any>;
  fullEntityCache?: Map<string, any>;
}

const activeSessions = new Map<string, ActiveSession>();
const connectionLocks = new Map<string, Promise<TelegramClient>>();
const pendingAuthByHash = new Map<string, { sessionToken: string; phoneNumber: string; createdAt: number }>();
const pendingAuthByPhone = new Map<string, { sessionToken: string; phoneCodeHash?: string; createdAt: number }>();
const SESSIONS_FILE = path.join(process.cwd(), '.telegram_sessions.json');

// Helper to extract a clean string ID from any GramJS peer representation
export function extractPeerId(peer: any): string {
  if (!peer) return '';
  if (typeof peer === 'string' || typeof peer === 'number' || typeof peer === 'bigint') {
    let s = String(peer).trim();
    if (s.startsWith('{')) {
      try {
        const p = JSON.parse(s);
        return String(p.channelId || p.userId || p.chatId || p.id || s);
      } catch (_) {}
    }
    return s;
  }
  if (typeof peer === 'object') {
    if (peer.channelId) return peer.channelId.toString();
    if (peer.userId) return peer.userId.toString();
    if (peer.chatId) return peer.chatId.toString();
    if (peer.id) return extractPeerId(peer.id);
  }
  return String(peer);
}

// Helper to sanitize BigInt and non-serializable properties for JSON response
export function sanitizeData(data: any): any {
  if (data === null || data === undefined) return data;
  if (typeof data === 'bigint') return data.toString();
  if (Buffer.isBuffer(data)) return data.toString('base64');
  if (Array.isArray(data)) return data.map(sanitizeData);
  if (typeof data === 'object') {
    const res: Record<string, any> = {};
    for (const key of Object.keys(data)) {
      if (key.startsWith('_')) continue; // Skip private internal Telegram properties
      try {
        const val = data[key];
        if (typeof val === 'bigint') {
          res[key] = val.toString();
        } else if (val?.value !== undefined && typeof val.value === 'bigint') {
          res[key] = val.value.toString();
        } else {
          res[key] = sanitizeData(val);
        }
      } catch {
        // Skip properties that throw on access
      }
    }
    return res;
  }
  return data;
}

// Peer resolving helper for GramJS (handles usernames, string numeric IDs, channel IDs, peer objects)
export async function resolvePeer(client: TelegramClient, peerId: any): Promise<any> {
  if (!peerId) return null;
  if (
    peerId === 'me' ||
    peerId === 'self' ||
    peerId === 'saved' ||
    peerId === 'saved_messages' ||
    peerId === 'chat_saved_messages'
  ) {
    return 'me';
  }

  let target = peerId;

  // Handle object representation (e.g. { channelId: "...", className: "PeerChannel" })
  if (typeof target === 'object' && target !== null) {
    if (target.className?.includes('InputPeer')) {
      return target;
    }
    target = extractPeerId(target);
  }

  let str = String(target).trim();
  if (!str) return 'me';

  if (str.startsWith('{')) {
    try {
      const parsed = JSON.parse(str);
      str = String(parsed.channelId || parsed.userId || parsed.chatId || parsed.id || str);
    } catch (_) {}
  }

  // Strip UI/mock prefixes
  if (
    str === 'me' ||
    str === 'self' ||
    str === 'saved' ||
    str === 'saved_messages' ||
    str === 'chat_saved_messages'
  ) {
    return 'me';
  }
  str = str.replace(/^(chat_|user_|channel_)/, '');

  // Check in active session entity caches first
  for (const s of activeSessions.values()) {
    if (s.client === client && s.entityCache) {
      if (s.entityCache.has(str)) return s.entityCache.get(str);
      if (str.startsWith('-100') && s.entityCache.has(str.slice(4))) {
        return s.entityCache.get(str.slice(4));
      }
      if (s.entityCache.has('-100' + str)) {
        return s.entityCache.get('-100' + str);
      }
    }
  }

  // Handle usernames starting with @
  if (str.startsWith('@')) {
    const username = str.slice(1);
    try {
      return await client.getInputEntity(username);
    } catch {
      try {
        return await client.getEntity(username);
      } catch {
        return username;
      }
    }
  }

  // Helper to test multiple input formats for channels, chats, and users
  const tryCandidates = async (cands: any[]) => {
    for (const c of cands) {
      try {
        const res = await client.getInputEntity(c);
        if (res) return res;
      } catch (_) {}
    }
    return null;
  };

  // Build candidate representations
  const candidates: any[] = [];
  if (str.startsWith('-100')) {
    const numPart = str.slice(4);
    candidates.push(str);
    try { candidates.push(BigInt(str) as any); } catch (_) {}
    try { candidates.push(Number(str)); } catch (_) {}
    candidates.push(numPart);
    try { candidates.push(Number(numPart)); } catch (_) {}
    try { candidates.push(BigInt(numPart) as any); } catch (_) {}
  } else if (/^\d+$/.test(str)) {
    // Numeric string: could be a channel (requires -100) or user or chat
    candidates.push('-100' + str);
    try { candidates.push(BigInt('-100' + str) as any); } catch (_) {}
    candidates.push(str);
    try { candidates.push(Number(str)); } catch (_) {}
    try { candidates.push(BigInt(str) as any); } catch (_) {}
  } else {
    candidates.push(str);
  }

  // First attempt: resolve from existing client entity cache
  const firstAttempt = await tryCandidates(candidates);
  if (firstAttempt) return firstAttempt;

  // Second attempt: prime dialogs into GramJS entity cache if missing
  try {
    const dialogs = await client.getDialogs({ limit: 100 });
    for (const s of activeSessions.values()) {
      if (s.client === client) {
        if (!s.entityCache) s.entityCache = new Map();
        for (const d of dialogs) {
          const cleanId = extractPeerId(d.id || d.entity?.id || (d as any).peer);
          if (cleanId) {
            if (d.inputEntity) s.entityCache.set(cleanId, d.inputEntity);
            else if (d.entity) s.entityCache.set(cleanId, d.entity);
          }
        }
      }
    }
    const secondAttempt = await tryCandidates(candidates);
    if (secondAttempt) return secondAttempt;
  } catch (_) {}

  // Third attempt: fallback to getEntity lookup
  for (const c of candidates) {
    try {
      const ent = await client.getEntity(c);
      if (ent) return ent;
    } catch (_) {}
  }

  // Return null if completely unresolvable so callers can handle gracefully
  return null;
}

// Load saved multi-account storage
function loadStorage(): MultiAccountStorage {
  try {
    if (fs.existsSync(SESSIONS_FILE)) {
      const raw = fs.readFileSync(SESSIONS_FILE, 'utf-8');
      const data = JSON.parse(raw);
      if (data && typeof data === 'object') {
        if (!data.sessions && !data.accounts) {
          const sessions: Record<string, string> = {};
          const accounts: SavedAccount[] = [];
          for (const [k, v] of Object.entries(data)) {
            if (typeof v === 'string') {
              sessions[k] = v;
              accounts.push({
                id: 'acc_' + k.replace(/[^a-zA-Z0-9]/g, '').slice(-8),
                sessionToken: k,
                user: { id: 'me', firstName: 'مستخدم تيليجرام' },
                addedAt: Date.now(),
              });
            }
          }
          return { sessions, accounts: accounts.slice(0, MAX_TELEGRAM_ACCOUNTS) };
        }
        return {
          sessions: data.sessions || {},
          accounts: Array.isArray(data.accounts) ? data.accounts.slice(0, MAX_TELEGRAM_ACCOUNTS) : [],
          activeAccountId: data.activeAccountId,
        };
      }
    }
  } catch (err) {
    console.error('Error loading storage:', err);
  }
  return { sessions: {}, accounts: [] };
}

function saveStorage(storage: MultiAccountStorage) {
  try {
    storage.accounts = storage.accounts.slice(0, MAX_TELEGRAM_ACCOUNTS);
    fs.writeFileSync(SESSIONS_FILE, JSON.stringify(storage, null, 2), 'utf-8');
  } catch (err) {
    console.error('Error saving storage:', err);
  }
}

function persistSession(token: string, sessionString: string) {
  const storage = loadStorage();
  storage.sessions[token] = sessionString;
  saveStorage(storage);
}

function removePersistedSession(token: string) {
  const storage = loadStorage();
  delete storage.sessions[token];
  storage.accounts = storage.accounts.filter(a => a.sessionToken !== token);
  saveStorage(storage);
}

export class TelegramService {
  private static onUpdateCallback?: (sessionToken: string, update: any) => void;

  public static setOnUpdateCallback(cb: (sessionToken: string, update: any) => void) {
    TelegramService.onUpdateCallback = cb;
  }

  /**
   * Cleans up stale, revoked, or unregistered sessions immediately
   */
  public static purgeStaleSession(sessionToken: string) {
    if (!sessionToken) return;
    try {
      removePersistedSession(sessionToken);
      const session = activeSessions.get(sessionToken);
      if (session?.client) {
        try {
          session.client.disconnect();
        } catch (_) {}
      }
      activeSessions.delete(sessionToken);
      console.log(`[TelegramService] Cleaned up stale/unregistered session: ${sessionToken}`);
    } catch (err) {
      console.error(`[TelegramService] Error purging session ${sessionToken}:`, err);
    }
  }

  /**
   * Bootstraps continuous MTProto listeners for all saved accounts on server startup
   */
  public static async initAllSavedSessions(): Promise<void> {
    const storage = loadStorage();
    const tokens = Object.keys(storage.sessions);
    console.log(`[MTProto Updates Engine] Bootstrapping continuous listeners for ${tokens.length} saved sessions`);
    for (const token of tokens) {
      try {
        const client = await this.getOrCreateClient(token, storage.sessions[token]);
        const isAuth = await client.isUserAuthorized().catch(() => false);
        if (!isAuth) {
          console.warn(`[MTProto Updates Engine] Session ${token.slice(0, 10)}... is unregistered or unauthorized. Purging.`);
          this.purgeStaleSession(token);
          continue;
        }
        const me = await client.getMe().catch(() => null);
        if (me) {
          const s = activeSessions.get(token);
          if (s) {
            s.isLoggedIn = true;
            s.user = me;
          }
          this.saveAccount(token, me);
          console.log(`[MTProto Updates Engine] Connected & listening on session: ${token.slice(0, 10)}... (${me.firstName || 'User'})`);
        } else {
          this.purgeStaleSession(token);
        }
      } catch (err: any) {
        console.warn(`[MTProto Updates Engine] Warning restoring session ${token}:`, err?.message || err);
        if (
          err?.message?.includes('AUTH_KEY_UNREGISTERED') ||
          err?.errorMessage === 'AUTH_KEY_UNREGISTERED' ||
          err?.code === 401
        ) {
          this.purgeStaleSession(token);
        }
      }
    }
  }

  /**
   * Continuous UpdatesHandler matching Telegram Web K specification
   * Unpacks recursive Updates containers, new messages, reads, edits, deletes, typing indicators, and reactions
   */
  public static handleMtprotoUpdate(sessionToken: string, update: any, client: TelegramClient) {
    if (!TelegramService.onUpdateCallback || !update) return;

    try {
      const className = update.className || update.constructor?.name || '';

      // 1. Unpack bulk updates (Updates or UpdatesCombined container)
      if (className === 'Updates' || className === 'UpdatesCombined') {
        if (Array.isArray(update.updates)) {
          for (const subUpdate of update.updates) {
            this.handleMtprotoUpdate(sessionToken, subUpdate, client);
          }
        }
        return;
      }

      // 2. Unpack UpdateShort
      if (className === 'UpdateShort' && update.update) {
        this.handleMtprotoUpdate(sessionToken, update.update, client);
        return;
      }

      // 3. New Messages (Private, Group, Supergroup, Channel)
      if (
        className === 'UpdateNewMessage' ||
        className === 'UpdateNewChannelMessage' ||
        className === 'UpdateShortMessage' ||
        className === 'UpdateShortChatMessage'
      ) {
        const msg = update.message || update;
        const text = msg.message || msg.text || '';
        const rawPeer = msg.peerId;
        const peerId =
          rawPeer?.userId?.toString() ||
          rawPeer?.channelId?.toString() ||
          rawPeer?.chatId?.toString() ||
          msg.chatId?.toString() ||
          msg.fromId?.userId?.toString() ||
          msg.fromId?.channelId?.toString() ||
          msg.userId?.toString() ||
          'user';

        const isOut = !!msg.out;
        const senderId = isOut
          ? 'me'
          : (msg.fromId?.userId?.toString() || msg.fromId?.channelId?.toString() || msg.userId?.toString() || peerId);
        const senderName = isOut ? 'أنا' : 'Telegram';

        TelegramService.onUpdateCallback(sessionToken, {
          type: 'new_message',
          peerId,
          message: {
            id: msg.id?.toString() || 'msg_' + Date.now(),
            chatId: peerId,
            senderId,
            senderName,
            text,
            timestamp: (msg.date || Math.floor(Date.now() / 1000)) * 1000,
            isOut,
            status: isOut ? 'sent' : 'received',
            replyTo: msg.replyTo?.replyToMsgId ? { id: msg.replyTo.replyToMsgId.toString() } : undefined,
            media: msg.media ? sanitizeData(msg.media) : undefined,
          },
        });
        return;
      }

      // 4. Edited Messages
      if (className === 'UpdateEditMessage' || className === 'UpdateEditChannelMessage') {
        const msg = update.message;
        if (msg) {
          const rawPeer = msg.peerId;
          const peerId =
            rawPeer?.userId?.toString() ||
            rawPeer?.channelId?.toString() ||
            rawPeer?.chatId?.toString() ||
            msg.fromId?.userId?.toString() ||
            'user';

          TelegramService.onUpdateCallback(sessionToken, {
            type: 'message_edited',
            peerId,
            messageId: msg.id?.toString(),
            text: msg.message || '',
            editDate: (msg.editDate || Math.floor(Date.now() / 1000)) * 1000,
          });
        }
        return;
      }

      // 5. Deleted Messages
      if (className === 'UpdateDeleteMessages') {
        const messageIds = (update.messages || []).map((id: any) => id.toString());
        TelegramService.onUpdateCallback(sessionToken, {
          type: 'messages_deleted',
          messageIds,
        });
        return;
      }

      if (className === 'UpdateDeleteChannelMessages') {
        const channelId = update.channelId?.toString();
        const messageIds = (update.messages || []).map((id: any) => id.toString());
        TelegramService.onUpdateCallback(sessionToken, {
          type: 'messages_deleted',
          peerId: channelId,
          messageIds,
        });
        return;
      }

      // 6. Read History Updates
      if (className === 'UpdateReadHistoryOutbox' || className === 'UpdateReadChannelOutbox') {
        const peerId =
          update.peer?.userId?.toString() ||
          update.peer?.channelId?.toString() ||
          update.peer?.chatId?.toString() ||
          update.channelId?.toString();

        TelegramService.onUpdateCallback(sessionToken, {
          type: 'message_read',
          peerId,
          messageId: update.maxId?.toString(),
          isOutbox: true,
        });
        return;
      }

      if (className === 'UpdateReadHistoryInbox' || className === 'UpdateReadChannelInbox') {
        const peerId =
          update.peer?.userId?.toString() ||
          update.peer?.channelId?.toString() ||
          update.peer?.chatId?.toString() ||
          update.channelId?.toString();

        TelegramService.onUpdateCallback(sessionToken, {
          type: 'message_read',
          peerId,
          messageId: update.maxId?.toString(),
          isInbox: true,
        });
        return;
      }

      // 7. Typing Updates
      if (
        className === 'UpdateUserTyping' ||
        className === 'UpdateChatUserTyping' ||
        className === 'UpdateChannelUserTyping'
      ) {
        const peerId =
          update.chatId?.toString() ||
          update.channelId?.toString() ||
          update.userId?.toString() ||
          update.peer?.userId?.toString();

        let actionType: 'typing' | 'recording' | 'uploading' = 'typing';
        const actionName = update.action?.className || '';
        if (actionName.includes('RecordAudio') || actionName.includes('Voice')) {
          actionType = 'recording';
        } else if (actionName.includes('Upload') || actionName.includes('Document')) {
          actionType = 'uploading';
        }

        TelegramService.onUpdateCallback(sessionToken, {
          type: 'typing_status',
          peerId,
          action: actionType,
        });
        return;
      }

      // 8. Reactions Updates (UpdateMessageReactions, UpdateBotMessageReaction)
      if (className === 'UpdateMessageReactions' || className === 'UpdateBotMessageReaction') {
        const peerId =
          update.peer?.channelId?.toString() ||
          update.peer?.chatId?.toString() ||
          update.peer?.userId?.toString() ||
          update.channelId?.toString();

        const reactions = update.reactions?.results?.map((r: any) => ({
          emoji: r.reaction?.emoticon || '❤️',
          count: r.count,
          userReacted: !!r.chosenOrder,
        })) || [];

        TelegramService.onUpdateCallback(sessionToken, {
          type: 'message_reaction',
          peerId,
          messageId: update.msgId?.toString() || update.messageId?.toString(),
          reactions,
        });
        return;
      }

      // 9. User Presence & Last Seen Updates (UpdateUserStatus)
      if (className === 'UpdateUserStatus') {
        const userId = update.userId?.toString();
        const status = update.status;
        const statusClass = status?.className || '';
        const isOnline = statusClass === 'UserStatusOnline';
        const wasOnline = Number(status?.wasOnline || 0);
        const expires = Number(status?.expires || 0);

        if (userId) {
          try {
            sqliteDatabase.saveUserStatus(userId, statusClass, wasOnline, expires);
          } catch (_) {}

          TelegramService.onUpdateCallback(sessionToken, {
            type: 'user_status',
            userId,
            isOnline,
            statusClass,
            wasOnline,
            expires,
          });
        }
        return;
      }
    } catch (err) {
      console.warn('[MTProto Updates Engine] Error processing update:', err);
    }
  }

  public static async markAsRead(sessionToken: string, peerId: string) {
    const client = await this.getOrCreateClient(sessionToken);
    const { Api } = await import('telegram');
    const peer = await resolvePeer(client, peerId);
    await client.invoke(
      new Api.messages.ReadHistory({
        peer,
        maxId: 0,
      })
    );
    return { success: true };
  }

  public static async getOrCreateClient(sessionToken: string, sessionString: string = ''): Promise<TelegramClient> {
    const existing = activeSessions.get(sessionToken);
    if (existing && existing.client) {
      if (!existing.client.connected) {
        await existing.client.connect();
      }
      return existing.client;
    }

    if (connectionLocks.has(sessionToken)) {
      return connectionLocks.get(sessionToken)!;
    }

    const connectPromise = (async () => {
      if (!sessionString) {
        const storage = loadStorage();
        if (storage.sessions[sessionToken]) {
          sessionString = storage.sessions[sessionToken];
        }
      }

      const stringSession = new StringSession(sessionString || '');
      const client = new TelegramClient(stringSession, TELEGRAM_API_ID, TELEGRAM_API_HASH, {
        connectionRetries: 5,
        retryDelay: 1000,
        autoReconnect: true,
        timeout: 15000,
        deviceModel: 'Telegram Web Client',
        appVersion: '10.0',
        systemVersion: 'Web',
        useWSS: false,
      });

      client.setLogLevel(LogLevel.WARN);

      try {
        await client.connect();
      } catch (connErr: any) {
        if (sessionString) {
          removePersistedSession(sessionToken);
          const freshSession = new StringSession('');
          const fallbackClient = new TelegramClient(freshSession, TELEGRAM_API_ID, TELEGRAM_API_HASH, {
            connectionRetries: 3,
            retryDelay: 1000,
            timeout: 15000,
            useWSS: false,
          });
          fallbackClient.setLogLevel(LogLevel.WARN);
          await fallbackClient.connect();
          activeSessions.set(sessionToken, {
            client: fallbackClient,
            isLoggedIn: false,
            createdAt: Date.now(),
          });
          return fallbackClient;
        }
        throw connErr;
      }

      try {
        client.addEventHandler(async (update: any) => {
          TelegramService.handleMtprotoUpdate(sessionToken, update, client);
        });
      } catch (handlerErr) {
        console.warn('Could not attach update handler:', handlerErr);
      }

      activeSessions.set(sessionToken, {
        client,
        isLoggedIn: false,
        createdAt: Date.now(),
      });
      return client;
    })();

    connectionLocks.set(sessionToken, connectPromise);
    try {
      return await connectPromise;
    } finally {
      connectionLocks.delete(sessionToken);
    }
  }

  public static async sendCode(sessionToken: string, phoneNumber: string) {
    const client = await this.getOrCreateClient(sessionToken);
    const cleanedPhone = phoneNumber.replace(/[\s\-\(\)]/g, '');
    const res = await client.sendCode(
      {
        apiId: TELEGRAM_API_ID,
        apiHash: TELEGRAM_API_HASH,
      },
      cleanedPhone
    );

    const session = activeSessions.get(sessionToken);
    if (session) {
      session.phoneCodeHash = res.phoneCodeHash;
      session.phoneNumber = cleanedPhone;
    }

    pendingAuthByHash.set(res.phoneCodeHash, { sessionToken, phoneNumber: cleanedPhone, createdAt: Date.now() });
    pendingAuthByPhone.set(cleanedPhone, { sessionToken, phoneCodeHash: res.phoneCodeHash, createdAt: Date.now() });

    return {
      success: true,
      phoneCodeHash: res.phoneCodeHash,
      isCodeViaApp: res.isCodeViaApp,
      sessionToken,
    };
  }

  public static async signIn(sessionToken: string, phoneCode: string, phoneCodeHash?: string, phoneNumber?: string) {
    let session = activeSessions.get(sessionToken);
    let matchedToken = sessionToken;

    if (!session && phoneCodeHash && pendingAuthByHash.has(phoneCodeHash)) {
      const pending = pendingAuthByHash.get(phoneCodeHash)!;
      const s = activeSessions.get(pending.sessionToken);
      if (s) {
        session = s;
        matchedToken = pending.sessionToken;
      }
    }

    if (!session && phoneNumber) {
      const clean = phoneNumber.replace(/[\s\-\(\)]/g, '');
      if (pendingAuthByPhone.has(clean)) {
        const pending = pendingAuthByPhone.get(clean)!;
        const s = activeSessions.get(pending.sessionToken);
        if (s) {
          session = s;
          matchedToken = pending.sessionToken;
        }
      }
    }

    if (!session && phoneCodeHash) {
      for (const [tok, s] of activeSessions.entries()) {
        if (s.phoneCodeHash === phoneCodeHash) {
          session = s;
          matchedToken = tok;
          break;
        }
      }
    }

    if (!session) {
      for (const [tok, s] of activeSessions.entries()) {
        if (s.phoneCodeHash && !s.isLoggedIn) {
          session = s;
          matchedToken = tok;
          break;
        }
      }
    }

    const hash = phoneCodeHash || session?.phoneCodeHash;
    const phone = phoneNumber ? phoneNumber.replace(/[\s\-\(\)]/g, '') : session?.phoneNumber;

    if (!session && hash && phone) {
      const client = await this.getOrCreateClient(sessionToken);
      session = {
        client,
        phoneCodeHash: hash,
        phoneNumber: phone,
        isLoggedIn: false,
        createdAt: Date.now(),
      };
      activeSessions.set(sessionToken, session);
      matchedToken = sessionToken;
    }

    if (!session) {
      throw new Error('لم يتم العثور على جلسة مصادقة نشطة. يرجى طلب الرمز أولاً.');
    }

    if (sessionToken && !activeSessions.has(sessionToken)) {
      activeSessions.set(sessionToken, session);
    }

    if (!hash || !phone) {
      throw new Error('رقم الهاتف أو رمز التحقق غير مكتمل. يرجى إعادة المحاولة.');
    }

    const client = session.client;
    try {
      await client.invoke(
        new Api.auth.SignIn({
          phoneNumber: phone,
          phoneCodeHash: hash,
          phoneCode: phoneCode.trim(),
        })
      );

      const me = await client.getMe();
      session.isLoggedIn = true;
      session.user = me;

      const sessionString = client.session.save() as unknown as string;
      persistSession(sessionToken, sessionString);
      if (matchedToken !== sessionToken) {
        persistSession(matchedToken, sessionString);
      }
      this.saveAccount(sessionToken, me);

      if (hash) pendingAuthByHash.delete(hash);
      if (phone) pendingAuthByPhone.delete(phone);

      return {
        success: true,
        user: sanitizeData(me),
        sessionString,
        sessionToken,
      };
    } catch (err: any) {
      if (err.errorMessage === 'SESSION_PASSWORD_NEEDED') {
        return {
          success: false,
          needs2FA: true,
          message: 'يتطلب التحقق بخطوتين (2FA) كلمة المرور الخاصة بك.',
          sessionToken,
        };
      }
      throw err;
    }
  }

  public static async signInWithPassword(sessionToken: string, password: string) {
    let session = activeSessions.get(sessionToken);
    let matchedToken = sessionToken;

    if (!session) {
      for (const [tok, s] of activeSessions.entries()) {
        if (s.phoneCodeHash || s.phoneNumber) {
          session = s;
          matchedToken = tok;
          break;
        }
      }
    }

    if (!session) {
      for (const [tok, s] of activeSessions.entries()) {
        if (s.client && !s.isLoggedIn) {
          session = s;
          matchedToken = tok;
          break;
        }
      }
    }

    if (!session) {
      throw new Error('لا توجد جلسة نشطة. يرجى إعادة تسجيل الدخول.');
    }

    if (sessionToken && !activeSessions.has(sessionToken)) {
      activeSessions.set(sessionToken, session);
    }

    const client = session.client;
    await client.signInWithPassword(
      {
        apiId: TELEGRAM_API_ID,
        apiHash: TELEGRAM_API_HASH,
      },
      {
        password: async () => password,
        onError: (err) => {
          throw err;
        },
      }
    );

    const me = await client.getMe();
    session.isLoggedIn = true;
    session.user = me;

    const sessionString = client.session.save() as unknown as string;
    persistSession(sessionToken, sessionString);
    if (matchedToken !== sessionToken) {
      persistSession(matchedToken, sessionString);
    }
    this.saveAccount(sessionToken, me);

    return {
      success: true,
      user: sanitizeData(me),
      sessionString,
      sessionToken,
    };
  }

  public static async botLogin(sessionToken: string, botToken: string) {
    const client = await this.getOrCreateClient(sessionToken);
    await client.start({
      botAuthToken: botToken.trim(),
    });

    const me = await client.getMe();
    const session = activeSessions.get(sessionToken);
    if (session) {
      session.isLoggedIn = true;
      session.user = me;
    }

    const sessionString = client.session.save() as unknown as string;
    persistSession(sessionToken, sessionString);
    this.saveAccount(sessionToken, me);

    return {
      success: true,
      user: sanitizeData(me),
      sessionString,
    };
  }

  public static getActiveSessionToken(preferredToken?: string): string | null {
    if (preferredToken && activeSessions.has(preferredToken) && activeSessions.get(preferredToken)?.isLoggedIn) {
      return preferredToken;
    }
    const storage = loadStorage();
    if (storage.activeAccountId) {
      const acc = storage.accounts.find(a => a.id === storage.activeAccountId);
      if (acc?.sessionToken && storage.sessions[acc.sessionToken]) {
        return acc.sessionToken;
      }
    }
    if (storage.accounts.length > 0 && storage.accounts[0].sessionToken) {
      return storage.accounts[0].sessionToken;
    }
    const keys = Object.keys(storage.sessions);
    if (keys.length > 0) {
      return keys[0];
    }
    for (const [token, s] of activeSessions.entries()) {
      if (s.isLoggedIn) return token;
    }
    return null;
  }

  public static async getMe(sessionToken: string) {
    const session = activeSessions.get(sessionToken);
    if (session && session.client && session.client.connected) {
      try {
        const me = await session.client.getMe();
        if (me) {
          session.isLoggedIn = true;
          session.user = me;
          this.saveAccount(sessionToken, me);
          const sanitized = sanitizeData(me);
          const mePhoto = (me as any).photo;
          const hasMePhoto = Boolean(
            mePhoto &&
            mePhoto.className !== 'UserProfilePhotoEmpty' &&
            mePhoto.className !== 'ChatPhotoEmpty'
          );
          if (sanitized && hasMePhoto) {
            sanitized.photoUrl = `/api/telegram/avatar/me?token=${encodeURIComponent(sessionToken)}`;
            sanitized.avatarUrl = `/api/telegram/avatar/me?token=${encodeURIComponent(sessionToken)}`;
          }
          return { isLoggedIn: true, user: sanitized };
        }
      } catch (err: any) {
        if (
          err?.message?.includes('AUTH_KEY_UNREGISTERED') ||
          err?.errorMessage === 'AUTH_KEY_UNREGISTERED' ||
          err?.code === 401
        ) {
          console.warn(`[TelegramService] Auth key unregistered for session ${sessionToken}, purging.`);
          this.purgeStaleSession(sessionToken);
          return { isLoggedIn: false, user: null };
        }
      }
    }

    const storage = loadStorage();
    if (storage.sessions[sessionToken]) {
      try {
        const client = await this.getOrCreateClient(sessionToken, storage.sessions[sessionToken]);
        const isAuth = await client.isUserAuthorized().catch(() => false);
        if (!isAuth) {
          this.purgeStaleSession(sessionToken);
          return { isLoggedIn: false, user: null };
        }
        const me = await client.getMe();
        if (me) {
          const s = activeSessions.get(sessionToken);
          if (s) {
            s.isLoggedIn = true;
            s.user = me;
          }
          this.saveAccount(sessionToken, me);
          const sanitized = sanitizeData(me);
          const mePhoto = (me as any).photo;
          const hasMePhoto = Boolean(
            mePhoto &&
            mePhoto.className !== 'UserProfilePhotoEmpty' &&
            mePhoto.className !== 'ChatPhotoEmpty'
          );
          if (sanitized && hasMePhoto) {
            sanitized.photoUrl = `/api/telegram/avatar/me?token=${encodeURIComponent(sessionToken)}`;
            sanitized.avatarUrl = `/api/telegram/avatar/me?token=${encodeURIComponent(sessionToken)}`;
          }
          return { isLoggedIn: true, user: sanitized };
        }
      } catch (err: any) {
        console.warn('Session expired or could not be restored, cleaning up stale session token.');
        this.purgeStaleSession(sessionToken);
      }
    }

    return { isLoggedIn: false, user: null };
  }

  public static async isAuthorized(sessionToken: string): Promise<boolean> {
    if (!sessionToken) return false;
    const session = activeSessions.get(sessionToken);
    if (session && session.isLoggedIn && session.client) {
      return true;
    }
    const storage = loadStorage();
    if (storage.sessions[sessionToken]) {
      const meResult = await this.getMe(sessionToken);
      return !!meResult.isLoggedIn;
    }
    return false;
  }

  public static getAccounts() {
    const storage = loadStorage();
    const accounts = storage.accounts
      .filter(acc => !!storage.sessions[acc.sessionToken])
      .map(acc => {
        const active = activeSessions.get(acc.sessionToken);
        return {
          ...acc,
          isLoggedIn: !!(active?.isLoggedIn),
        };
      });
    return {
      accounts,
      activeAccountId: storage.activeAccountId || accounts[0]?.id,
      maxAccounts: MAX_TELEGRAM_ACCOUNTS,
    };
  }

  public static createAccountSlot() {
    const storage = loadStorage();
    if (storage.accounts.length >= MAX_TELEGRAM_ACCOUNTS) {
      throw new Error(`تم الوصول إلى الحد الأقصى للحسابات المسموح بها (${MAX_TELEGRAM_ACCOUNTS} حسابات)`);
    }
    const token = 'user_session_' + Math.random().toString(36).substring(2, 12);
    const accountId = 'acc_' + Date.now().toString(36) + '_' + Math.random().toString(36).substring(2, 6);
    return { sessionToken: token, accountId };
  }

  public static saveAccount(sessionToken: string, user: any) {
    const storage = loadStorage();
    const hasPhoto = Boolean(
      user.photo &&
      user.photo.className !== 'UserProfilePhotoEmpty' &&
      user.photo.className !== 'ChatPhotoEmpty'
    );
    const photoUrl =
      user.photoUrl ||
      user.avatarUrl ||
      (hasPhoto ? `/api/telegram/avatar/me?token=${encodeURIComponent(sessionToken)}` : undefined);

    const cleanUser = {
      id: user.id?.toString() || 'me',
      firstName: user.firstName || 'مستخدم تيليجرام',
      lastName: user.lastName,
      username: user.username,
      phone: user.phone,
      photoUrl,
      isBot: !!user.bot,
      status: 'online',
    };

    const existingIdx = storage.accounts.findIndex(
      a => a.sessionToken === sessionToken || a.user.id === cleanUser.id
    );

    if (existingIdx >= 0) {
      storage.accounts[existingIdx].sessionToken = sessionToken;
      storage.accounts[existingIdx].user = cleanUser;
    } else {
      if (storage.accounts.length >= MAX_TELEGRAM_ACCOUNTS) {
        console.warn('Max accounts reached, cannot add another');
        return storage.accounts[0];
      }
      const newAccount: SavedAccount = {
        id: 'acc_' + Date.now().toString(36) + '_' + Math.random().toString(36).substring(2, 6),
        sessionToken,
        user: cleanUser,
        addedAt: Date.now(),
      };
      storage.accounts.push(newAccount);
      storage.activeAccountId = newAccount.id;
    }
    saveStorage(storage);
  }

  public static async removeAccount(tokenOrId: string) {
    const storage = loadStorage();
    const account = storage.accounts.find(a => a.id === tokenOrId || a.sessionToken === tokenOrId);
    const token = account ? account.sessionToken : tokenOrId;
    const session = activeSessions.get(token);
    if (session && session.client) {
      try {
        await session.client.disconnect();
      } catch {}
    }
    activeSessions.delete(token);
    removePersistedSession(token);
    if (storage.activeAccountId === account?.id) {
      const remaining = storage.accounts.filter(a => a.sessionToken !== token);
      storage.activeAccountId = remaining[0]?.id;
      saveStorage(storage);
    }
    return { success: true };
  }

  public static setActiveAccount(tokenOrId: string) {
    const storage = loadStorage();
    const account = storage.accounts.find(a => a.id === tokenOrId || a.sessionToken === tokenOrId);
    if (account) {
      storage.activeAccountId = account.id;
      saveStorage(storage);
      return account;
    }
    return null;
  }

  public static async getDialogs(sessionToken: string, limit: number = 50) {
    if (!sessionToken || !(await this.isAuthorized(sessionToken))) {
      return [];
    }
    try {
      const client = await this.getOrCreateClient(sessionToken);
      const dialogs = await client.getDialogs({ limit });

      const s = activeSessions.get(sessionToken);
      if (s) {
        if (!s.entityCache) s.entityCache = new Map();
        if (!s.fullEntityCache) s.fullEntityCache = new Map();
        for (const d of dialogs) {
          const cleanId = extractPeerId(d.id || d.entity?.id || (d as any).peer);
          if (cleanId) {
            if (d.inputEntity) s.entityCache.set(cleanId, d.inputEntity);
            if (d.entity) {
              s.fullEntityCache.set(cleanId, d.entity);
              s.entityCache.set(cleanId, d.entity);
              if (cleanId.startsWith('-100')) {
                s.fullEntityCache.set(cleanId.slice(4), d.entity);
              }
            }
            const entUsername = (d.entity as any)?.username;
            if (entUsername) {
              s.entityCache.set(entUsername.toLowerCase(), d.inputEntity || d.entity);
              s.entityCache.set('@' + entUsername.toLowerCase(), d.inputEntity || d.entity);
              if (d.entity) {
                s.fullEntityCache.set(entUsername.toLowerCase(), d.entity);
                s.fullEntityCache.set('@' + entUsername.toLowerCase(), d.entity);
              }
            }
          }
        }
      }

      return dialogs.map((d: any) => {
        let type = 'private';
        if (d.isChannel) type = 'channel';
        else if (d.isGroup) type = 'group';
        else if (d.entity?.bot) type = 'bot';

        const lastMsg = d.message;
        let text = lastMsg?.text || '';
        if (!text && lastMsg?.media) {
          text = '[وسائط / ميديا]';
        }

        const peerId = extractPeerId(d.id || d.entity?.id || d.peer);
        const photoObj = (d.entity as any)?.photo || (d as any).photo;
        const hasPhoto = Boolean(
          photoObj &&
          photoObj.className !== 'UserProfilePhotoEmpty' &&
          photoObj.className !== 'ChatPhotoEmpty'
        );
        const avatarUrl = hasPhoto
          ? `/api/telegram/avatar/${encodeURIComponent(peerId)}?token=${encodeURIComponent(sessionToken)}`
          : undefined;

        return {
          id: peerId,
          title: d.title || d.name || 'محادثة',
          username: d.entity?.username || undefined,
          type,
          avatarUrl,
          unreadCount: d.unreadCount || 0,
          isPinned: !!d.isPinned,
          isMuted: !!d.isMuted,
          lastMessage: lastMsg
            ? {
                text,
                timestamp: (lastMsg.date || Math.floor(Date.now() / 1000)) * 1000,
                isOut: !!lastMsg.out,
              }
            : undefined,
        };
      });
    } catch (err: any) {
      if (
        err?.message?.includes('AUTH_KEY_UNREGISTERED') ||
        err?.errorMessage === 'AUTH_KEY_UNREGISTERED' ||
        err?.code === 401
      ) {
        console.warn(`[getDialogs] AUTH_KEY_UNREGISTERED detected for session ${sessionToken}, purging.`);
        this.purgeStaleSession(sessionToken);
      } else {
        console.warn(`[getDialogs] Notice fetching dialogs:`, err?.message || err);
      }
      return [];
    }
  }

  public static async getMessages(sessionToken: string, peerId: string, limit: number = 50) {
    if (!sessionToken || !(await this.isAuthorized(sessionToken))) {
      const cached = sqliteDatabase.getCachedMessages(peerId);
      return cached || [];
    }

    try {
      const client = await this.getOrCreateClient(sessionToken);
      const peer = await resolvePeer(client, peerId);
      if (!peer) {
        console.warn(`[getMessages] Notice: peer ${peerId} could not be resolved, returning cached messages.`);
        const cached = sqliteDatabase.getCachedMessages(peerId);
        return cached || [];
      }

      const messages = await client.getMessages(peer, { limit });

      return messages.map((m: any) => {
        let mediaType: string | undefined;
        let mediaTitle: string | undefined;
        let mediaUrl: string | undefined;

        if (m.media) {
          const className = m.media.className || '';
          mediaUrl = `/api/telegram/media/${encodeURIComponent(peerId)}/${m.id}?token=${encodeURIComponent(sessionToken)}`;
          if (className.includes('Photo')) {
            mediaType = 'photo';
          } else if (className.includes('Document')) {
            const mime = m.media.document?.mimeType || '';
            if (mime.startsWith('audio/') || mime.includes('ogg') || Boolean(m.media.voice)) {
              mediaType = 'voice';
            } else if (mime.startsWith('video/') || Boolean(m.media.video)) {
              mediaType = 'video';
            } else {
              mediaType = 'document';
            }
            mediaTitle = m.media.document?.attributes?.find((a: any) => a.fileName || a.title)?.fileName || 'مستند';
          }
        }

        let replyMarkup: any = undefined;
        if (m.replyMarkup) {
          const rmClass = m.replyMarkup.className || '';
          if (rmClass.includes('ReplyInlineMarkup')) {
            replyMarkup = {
              type: 'inline',
              inlineKeyboard: m.replyMarkup.rows?.map((row: any) =>
                row.buttons?.map((btn: any) => {
                  let callbackData: string | undefined;
                  if (btn.data) {
                    try {
                      callbackData = Buffer.isBuffer(btn.data) ? btn.data.toString('utf-8') : String(btn.data);
                    } catch {
                      callbackData = String(btn.data);
                    }
                  }
                  return {
                    text: btn.text,
                    url: btn.url,
                    callbackData,
                    webApp: btn.webApp?.url || (btn.url && btn.url.includes('t.me') ? { url: btn.url } : undefined),
                    switchInlineQuery: btn.query,
                    switchInlineQueryCurrentChat: btn.samePeer ? btn.query : undefined,
                  };
                })
              ) || [],
            };
          } else if (rmClass.includes('ReplyKeyboardMarkup')) {
            replyMarkup = {
              type: 'keyboard',
              keyboard: m.replyMarkup.rows?.map((row: any) =>
                row.buttons?.map((btn: any) => ({
                  text: btn.text,
                  requestContact: !!btn.requestContact,
                  requestLocation: !!btn.requestGeoLocation,
                }))
              ) || [],
              resizeKeyboard: !!m.replyMarkup.resize,
              oneTimeKeyboard: !!m.replyMarkup.singleUse,
              isPersistent: !!m.replyMarkup.persistent,
            };
          }
        }

        const senderId = m.fromId?.userId?.toString() || (m.out ? 'me' : peerId);
        const senderAvatar =
          !m.out && senderId && senderId !== 'me'
            ? `/api/telegram/avatar/${encodeURIComponent(senderId)}?token=${encodeURIComponent(sessionToken)}`
            : undefined;

        return {
          id: m.id?.toString(),
          chatId: peerId,
          senderId,
          senderName: m.out ? 'أنا' : 'عضو',
          senderAvatar,
          text: m.text || (mediaType ? `[${mediaType}]` : ''),
          timestamp: (m.date || Math.floor(Date.now() / 1000)) * 1000,
          isOut: !!m.out,
          status: m.out ? 'read' : 'sent',
          media: mediaType
            ? {
                type: mediaType,
                title: mediaTitle,
                url: mediaUrl,
              }
            : undefined,
          reactions: m.reactions?.results?.map((r: any) => ({
            emoji: r.reaction?.emoticon || '❤️',
            count: r.count,
            userReacted: !!r.chosenOrder,
          })),
          replyMarkup,
        };
      });
    } catch (err: any) {
      if (
        err?.message?.includes('AUTH_KEY_UNREGISTERED') ||
        err?.errorMessage === 'AUTH_KEY_UNREGISTERED' ||
        err?.code === 401
      ) {
        console.warn(`[getMessages] AUTH_KEY_UNREGISTERED detected for session ${sessionToken}, purging.`);
        this.purgeStaleSession(sessionToken);
      } else {
        console.warn(`[getMessages] Notice fetching messages for ${peerId}:`, err?.message || err);
      }
      const cached = sqliteDatabase.getCachedMessages(peerId);
      return cached || [];
    }
  }

  public static async sendMessage(
    sessionToken: string,
    peerId: string,
    text: string,
    replyTo?: number,
    media?: any
  ) {
    const client = await this.getOrCreateClient(sessionToken);
    const peer = await resolvePeer(client, peerId);

    // If media payload with url is provided, send via sendFile
    if (media && media.url) {
      return await this.sendFile(sessionToken, peerId, media.url, {
        caption: text,
        replyTo,
        fileName: media.fileName || media.title,
        voiceNote: media.type === 'voice',
        forceDocument: media.type === 'document',
      });
    }

    const res = await client.sendMessage(peer, {
      message: text,
      replyTo: replyTo ? Number(replyTo) : undefined,
    });
    return sanitizeData(res);
  }

  public static async sendFile(
    sessionToken: string,
    peerId: string,
    file: Buffer | string,
    options: {
      caption?: string;
      replyTo?: number;
      fileName?: string;
      voiceNote?: boolean;
      forceDocument?: boolean;
    } = {}
  ) {
    const client = await this.getOrCreateClient(sessionToken);
    const peer = await resolvePeer(client, peerId);

    let fileObj: any = file;
    if (typeof file === 'string' && file.startsWith('data:')) {
      const commaIdx = file.indexOf(',');
      const base64Data = file.slice(commaIdx + 1);
      fileObj = Buffer.from(base64Data, 'base64');
      if (options.fileName) {
        (fileObj as any).name = options.fileName;
      }
    }

    const res = await client.sendFile(peer, {
      file: fileObj,
      caption: options.caption,
      replyTo: options.replyTo ? Number(options.replyTo) : undefined,
      voiceNote: options.voiceNote,
      forceDocument: options.forceDocument,
    });
    return sanitizeData(res);
  }

  public static async sendReaction(
    sessionToken: string,
    peerId: string,
    messageId: number | string,
    emoji?: string
  ) {
    const client = await this.getOrCreateClient(sessionToken);
    const peer = await resolvePeer(client, peerId);
    const { Api } = await import('telegram');

    const reactions = emoji
      ? [new Api.ReactionEmoji({ emoticon: emoji })]
      : [];

    const res = await client.invoke(
      new Api.messages.SendReaction({
        peer,
        msgId: Number(messageId),
        reaction: reactions,
      })
    );
    return sanitizeData(res);
  }

  public static async deleteMessages(
    sessionToken: string,
    peerId: string,
    messageIds: number[],
    revoke: boolean = true
  ) {
    const client = await this.getOrCreateClient(sessionToken);
    const { Api } = await import('telegram');
    const res = await client.invoke(
      new Api.messages.DeleteMessages({
        id: messageIds,
        revoke,
      })
    );
    return { success: true, result: sanitizeData(res) };
  }

  public static async editMessage(
    sessionToken: string,
    peerId: string,
    messageId: number,
    text: string
  ) {
    const client = await this.getOrCreateClient(sessionToken);
    const peer = await resolvePeer(client, peerId);
    const res = await client.editMessage(peer, {
      message: messageId,
      text,
    });
    return sanitizeData(res);
  }

  public static async setTyping(sessionToken: string, peerId: string, action: string = 'typing') {
    const client = await this.getOrCreateClient(sessionToken);
    const { Api } = await import('telegram');
    const peer = await resolvePeer(client, peerId);

    let act: any = new Api.SendMessageTypingAction();
    if (action === 'recording') {
      act = new Api.SendMessageRecordAudioAction();
    } else if (action === 'uploading') {
      act = new Api.SendMessageUploadDocumentAction({ progress: 50 });
    }

    const res = await client.invoke(
      new Api.messages.SetTyping({
        peer,
        action: act,
      })
    );
    return sanitizeData(res);
  }

  public static async searchGlobal(sessionToken: string, query: string) {
    if (!query || query.trim().length === 0) {
      return [];
    }
    const cleanQuery = query.trim().replace(/^@/, '');
    const client = await this.getOrCreateClient(sessionToken);
    const { Api } = await import('telegram');
    const foundPeers: any[] = [];

    // 1. Invoke MTProto contacts.search
    try {
      const searchResult: any = await client.invoke(
        new Api.contacts.Search({
          q: cleanQuery,
          limit: 25,
        })
      );
      if (searchResult) {
        if (Array.isArray(searchResult.chats)) {
          for (const c of searchResult.chats) {
            const isChannel = c.broadcast || c.className === 'Channel';
            const isSupergroup = c.megagroup;
            const type = isChannel ? 'channel' : isSupergroup ? 'supergroup' : 'group';
            foundPeers.push({
              id: c.id?.toString(),
              title: c.title || 'قناة تليجرام',
              username: c.username || undefined,
              type,
              participantsCount: c.participantsCount,
              description: c.about || undefined,
              isVerified: !!c.verified,
              isJoined: !c.left,
            });
          }
        }
        if (Array.isArray(searchResult.users)) {
          for (const u of searchResult.users) {
            const type = u.bot ? 'bot' : 'private';
            const name = [u.firstName, u.lastName].filter(Boolean).join(' ') || u.username || 'مستخدم';
            foundPeers.push({
              id: u.id?.toString(),
              title: name,
              username: u.username || undefined,
              type,
              isVerified: !!u.verified,
              isJoined: false,
            });
          }
        }
      }
    } catch (searchErr: any) {
      console.warn('Api.contacts.Search:', searchErr.message);
    }

    // 2. Direct username lookup
    if (cleanQuery.length >= 3 && !foundPeers.some(p => p.username?.toLowerCase() === cleanQuery.toLowerCase())) {
      try {
        const entity: any = await client.getEntity(cleanQuery);
        if (entity) {
          const isChannel = entity.broadcast || entity.className === 'Channel';
          const isSupergroup = entity.megagroup;
          const isBot = !!entity.bot;
          const type = isChannel ? 'channel' : isSupergroup ? 'supergroup' : isBot ? 'bot' : 'private';
          const title = entity.title || [entity.firstName, entity.lastName].filter(Boolean).join(' ') || entity.username;
          foundPeers.unshift({
            id: entity.id?.toString(),
            title: title || cleanQuery,
            username: entity.username || cleanQuery,
            type,
            participantsCount: entity.participantsCount,
            description: entity.about,
            isVerified: !!entity.verified,
            isJoined: !entity.left,
          });
        }
      } catch {
        // Not found
      }
    }

    return foundPeers;
  }

  public static async joinChannel(sessionToken: string, channelPeerOrUsername: string) {
    const client = await this.getOrCreateClient(sessionToken);
    const { Api } = await import('telegram');
    const entity = await resolvePeer(client, channelPeerOrUsername);

    const result: any = await client.invoke(
      new Api.channels.JoinChannel({
        channel: entity,
      })
    );

    return {
      success: true,
      channel: {
        id: entity?.id?.toString() || channelPeerOrUsername,
        title: entity?.title || 'قناة تليجرام',
        username: entity?.username,
        type: 'channel',
        isJoined: true,
      },
      result: sanitizeData(result),
    };
  }

  public static async leaveChannel(sessionToken: string, channelPeerOrUsername: string) {
    const client = await this.getOrCreateClient(sessionToken);
    const { Api } = await import('telegram');
    const entity = await resolvePeer(client, channelPeerOrUsername);

    const result: any = await client.invoke(
      new Api.channels.LeaveChannel({
        channel: entity,
      })
    );
    return { success: true, result: sanitizeData(result) };
  }

  public static async getAllStories(sessionToken: string) {
    if (!sessionToken) return [];
    try {
      const authorized = await this.isAuthorized(sessionToken);
      if (!authorized) {
        return [];
      }

      const client = await this.getOrCreateClient(sessionToken);
      const { Api } = await import('telegram');

      const res: any = await client.invoke(new Api.stories.GetAllStories({}));
      if (res && res.peerStories) {
        return res.peerStories.map((ps: any) => {
          const peerId = ps.peer?.userId?.toString() || ps.peer?.channelId?.toString() || 'user';
          const storiesList = (ps.stories || []).map((st: any) => {
            let mediaType: 'photo' | 'video' = 'photo';
            if (st.media) {
              const className = st.media.className || '';
              if (className.includes('Video') || className.includes('Document')) {
                mediaType = 'video';
              }
            }
            return {
              id: st.id?.toString() || String(st.id),
              peerId,
              date: (st.date || Math.floor(Date.now() / 1000)) * 1000,
              caption: st.caption || '',
              mediaType,
              isViewed: !st.unread,
              reactionsCount: st.views?.reactionsCount || 0,
            };
          });

          return {
            peerId,
            maxReadId: ps.maxReadId?.toString(),
            stories: storiesList,
          };
        });
      }
      return [];
    } catch (err: any) {
      if (err?.message && (err.message.includes('AUTH_KEY_UNREGISTERED') || err.message.includes('401'))) {
        return [];
      }
      console.warn('Api.stories.GetAllStories warning:', err?.message || err);
      return [];
    }
  }

  public static async readStories(sessionToken: string, peerId: string, maxId: number | string) {
    if (!sessionToken || !(await this.isAuthorized(sessionToken))) {
      return { success: false, notAuthorized: true };
    }
    const client = await this.getOrCreateClient(sessionToken);
    const { Api } = await import('telegram');
    const peer = await resolvePeer(client, peerId);

    const res = await client.invoke(
      new Api.stories.ReadStories({
        peer,
        maxId: Number(maxId),
      })
    );
    return { success: true, result: sanitizeData(res) };
  }

  public static async sendStoryReaction(
    sessionToken: string,
    peerId: string,
    storyId: number | string,
    emoji: string
  ) {
    if (!sessionToken || !(await this.isAuthorized(sessionToken))) {
      return { success: false, notAuthorized: true };
    }
    const client = await this.getOrCreateClient(sessionToken);
    const { Api } = await import('telegram');
    const peer = await resolvePeer(client, peerId);

    const res = await client.invoke(
      new Api.stories.SendReaction({
        peer,
        storyId: Number(storyId),
        reaction: new Api.ReactionEmoji({ emoticon: emoji }),
      })
    );
    return { success: true, result: sanitizeData(res) };
  }

  public static async getBotCallbackAnswer(
    sessionToken: string,
    peerId: string,
    msgId: number,
    data?: string,
    game?: boolean
  ) {
    const client = await this.getOrCreateClient(sessionToken);
    const { Api } = await import('telegram');
    const peer = await resolvePeer(client, peerId);

    const res: any = await client.invoke(
      new Api.messages.GetBotCallbackAnswer({
        peer,
        msgId: Number(msgId),
        data: data ? Buffer.from(data, 'utf-8') : undefined,
        game: !!game,
      })
    );

    return {
      success: true,
      message: res.message || '',
      alert: !!res.alert,
      url: res.url || undefined,
      hasUrl: !!res.hasUrl,
    };
  }

  public static async getBotInfo(sessionToken: string, botPeerId: string) {
    const client = await this.getOrCreateClient(sessionToken);
    const { Api } = await import('telegram');
    const bot = await resolvePeer(client, botPeerId);

    const BotInfoClass = (Api as any).bots?.GetBotInfo || (Api as any).messages?.GetBotInfo;
    const res: any = await client.invoke(
      new BotInfoClass({
        bot,
        langCode: 'ar',
      })
    );

    return {
      description: res.description || '',
      about: res.about || '',
      commands: res.commands?.map((c: any) => ({
        command: c.command,
        description: c.description,
      })) || [],
    };
  }

  public static async getInlineBotResults(sessionToken: string, botUsername: string, query: string) {
    const client = await this.getOrCreateClient(sessionToken);
    const { Api } = await import('telegram');
    const cleanBot = botUsername.trim().replace(/^@/, '');
    const botEntity = await resolvePeer(client, cleanBot);

    const res: any = await client.invoke(
      new Api.messages.GetInlineBotResults({
        bot: botEntity,
        peer: await resolvePeer(client, 'me'),
        query: query || '',
        offset: '',
      })
    );

    if (res && Array.isArray(res.results)) {
      return res.results.map((r: any) => ({
        id: r.id?.toString() || Math.random().toString(36).slice(2),
        type: r.type || 'article',
        title: r.title || r.id,
        description: r.description || '',
        thumbUrl: r.thumb?.url || r.photo?.url,
        url: r.url,
        contentText: r.sendMessage?.message || r.title || '',
      }));
    }
    return [];
  }

  public static async logout(sessionToken: string) {
    const session = activeSessions.get(sessionToken);
    if (session && session.client) {
      try {
        await session.client.disconnect();
      } catch {}
    }
    activeSessions.delete(sessionToken);
    removePersistedSession(sessionToken);
    return { success: true };
  }

  // ==========================================
  // PHASE 1 & PHASE 2: MTPROTO SYNC ARCHITECTURE
  // ==========================================

  /**
   * 1. Get Updates State (pts, qts, date, seq, unreadCount)
   * Official MTProto method: updates.getState
   */
  public static async getUpdatesState(sessionToken: string) {
    const defaultState = {
      pts: 1,
      qts: 0,
      date: Math.floor(Date.now() / 1000),
      seq: 0,
      unreadCount: 0,
    };
    if (!sessionToken || !(await this.isAuthorized(sessionToken))) {
      return defaultState;
    }

    try {
      const client = await this.getOrCreateClient(sessionToken);
      const state: any = await client.invoke(new Api.updates.GetState());
      return {
        pts: state.pts,
        qts: state.qts,
        date: state.date,
        seq: state.seq,
        unreadCount: state.unreadCount || 0,
      };
    } catch (err: any) {
      if (
        err?.message?.includes('AUTH_KEY_UNREGISTERED') ||
        err?.errorMessage === 'AUTH_KEY_UNREGISTERED' ||
        err?.code === 401
      ) {
        console.warn(`[getUpdatesState] AUTH_KEY_UNREGISTERED detected, purging session.`);
        this.purgeStaleSession(sessionToken);
      } else {
        console.warn(`[getUpdatesState] Notice fetching updates state:`, err?.message || err);
      }
      return defaultState;
    }
  }

  /**
   * 2. Comprehensive Gap Recovery & Difference Sync
   * Official MTProto method: updates.getDifference
   */
  public static async getDifference(
    sessionToken: string,
    pts: number,
    date?: number,
    qts: number = 0,
    ptsTotalLimit: number = 100
  ) {
    const defaultDiff = {
      className: 'updates.DifferenceEmpty',
      state: {
        pts: pts || 1,
        qts: qts || 0,
        date: date || Math.floor(Date.now() / 1000),
        seq: 0,
        unreadCount: 0,
      },
      newMessages: [],
      otherUpdates: [],
      isIntermediate: false,
    };
    if (!sessionToken || !(await this.isAuthorized(sessionToken))) {
      return defaultDiff;
    }

    try {
      const client = await this.getOrCreateClient(sessionToken);
      let targetPts = Number(pts) || 0;
      if (targetPts <= 0) {
        try {
          const state: any = await client.invoke(new Api.updates.GetState());
          targetPts = state?.pts || 1;
        } catch {
          targetPts = 1;
        }
      }
      const diff: any = await client.invoke(
        new Api.updates.GetDifference({
          pts: targetPts,
          date: Number(date) || Math.floor(Date.now() / 1000) - 86400,
          qts: Number(qts) || 0,
          ptsTotalLimit: Number(ptsTotalLimit) || 100,
        })
      );

      const newMessages = (diff.newMessages || []).map((m: any) => {
        const rawPeer = m.peerId;
        const peerId =
          rawPeer?.userId?.toString() ||
          rawPeer?.channelId?.toString() ||
          rawPeer?.chatId?.toString() ||
          m.fromId?.userId?.toString() ||
          'chat';

        let media: any = undefined;
        if (m.media) {
          const isPhoto = Boolean(m.media.photo);
          const isDoc = Boolean(m.media.document);
          const isVoice = Boolean(m.media.voice) || m.media.document?.mimeType?.includes('audio') || m.media.document?.mimeType?.includes('ogg');
          const isVideo = Boolean(m.media.video) || m.media.document?.mimeType?.includes('video');
          const type = isVoice ? 'voice' : isVideo ? 'video' : isPhoto ? 'photo' : 'document';
          const docAttr = m.media.document?.attributes?.find((a: any) => a.fileName || a.title);

          media = {
            type,
            url: `/api/telegram/media/${encodeURIComponent(peerId)}/${m.id}`,
            fileName: docAttr?.fileName || docAttr?.title,
          };
        }

        return {
          id: String(m.id),
          chatId: peerId,
          senderId: m.out ? 'me' : (m.fromId?.userId?.toString() || peerId),
          senderName: m.out ? 'أنا' : 'عضو',
          text: m.message || (media ? `[${media.type}]` : ''),
          timestamp: (m.date || Math.floor(Date.now() / 1000)) * 1000,
          isOut: !!m.out,
          status: m.out ? 'read' : 'sent',
          media,
        };
      });

      return {
        className: diff.className,
        state: diff.state ? {
          pts: diff.state.pts,
          qts: diff.state.qts,
          date: diff.state.date,
          seq: diff.state.seq,
          unreadCount: diff.state.unreadCount || 0,
        } : undefined,
        newMessages,
        otherUpdates: sanitizeData(diff.otherUpdates || []),
        isIntermediate: diff.className === 'updates.DifferenceSlice',
      };
    } catch (err: any) {
      if (
        err?.message?.includes('AUTH_KEY_UNREGISTERED') ||
        err?.errorMessage === 'AUTH_KEY_UNREGISTERED' ||
        err?.code === 401
      ) {
        console.warn(`[getDifference] AUTH_KEY_UNREGISTERED detected, purging session.`);
        this.purgeStaleSession(sessionToken);
      } else {
        console.warn(`[getDifference] Notice fetching updates difference:`, err?.message || err);
      }
      return defaultDiff;
    }
  }

  /**
   * 3. Channel & Supergroup Difference Sync
   * Official MTProto method: updates.getChannelDifference
   */
  public static async getChannelDifference(
    sessionToken: string,
    channelPeer: string,
    pts: number,
    limit: number = 100
  ) {
    const defaultDiff = {
      className: 'updates.ChannelDifferenceEmpty',
      pts: Number(pts) || 1,
      timeout: 0,
      newMessages: [],
      otherUpdates: [],
      isFinal: true,
    };
    if (!sessionToken || !(await this.isAuthorized(sessionToken))) {
      return defaultDiff;
    }

    try {
      const client = await this.getOrCreateClient(sessionToken);
      const peer = await resolvePeer(client, channelPeer);
      if (!peer) {
        return defaultDiff;
      }
      const diff: any = await client.invoke(
        new Api.updates.GetChannelDifference({
          channel: peer as any,
          filter: new Api.ChannelMessagesFilterEmpty(),
          pts: Number(pts) || 0,
          limit: Number(limit) || 100,
        })
      );

      const newMessages = (diff.newMessages || []).map((m: any) => {
        const peerId = String(channelPeer);
        let media: any = undefined;
        if (m.media) {
          media = {
            type: m.media.photo ? 'photo' : 'document',
            url: `/api/telegram/media/${encodeURIComponent(peerId)}/${m.id}`,
          };
        }
        return {
          id: String(m.id),
          chatId: peerId,
          senderId: m.out ? 'me' : (m.fromId?.userId?.toString() || peerId),
          senderName: m.out ? 'أنا' : 'قناة',
          text: m.message || '',
          timestamp: (m.date || Math.floor(Date.now() / 1000)) * 1000,
          isOut: !!m.out,
          status: 'sent',
          media,
        };
      });

      return {
        className: diff.className,
        pts: diff.pts,
        timeout: diff.timeout,
        newMessages,
        otherUpdates: sanitizeData(diff.otherUpdates || []),
        isFinal: diff.className === 'updates.ChannelDifference',
      };
    } catch (err: any) {
      if (
        err?.message?.includes('AUTH_KEY_UNREGISTERED') ||
        err?.errorMessage === 'AUTH_KEY_UNREGISTERED' ||
        err?.code === 401
      ) {
        console.warn(`[getChannelDifference] AUTH_KEY_UNREGISTERED detected, purging session.`);
        this.purgeStaleSession(sessionToken);
      } else {
        console.warn(`[getChannelDifference] Notice fetching channel difference:`, err?.message || err);
      }
      return defaultDiff;
    }
  }

  /**
   * 4. Binary Media Stream Downloader (Images, Audio, Voice, Video, Documents)
   */
  public static async downloadMedia(sessionToken: string, peerId: string, messageId: number) {
    if (!sessionToken || !(await this.isAuthorized(sessionToken))) {
      return null;
    }
    try {
      const client = await this.getOrCreateClient(sessionToken);
      const peer = await resolvePeer(client, peerId);
      if (!peer) return null;
      const msgs = await client.getMessages(peer, { ids: [Number(messageId)] });
      if (!msgs || msgs.length === 0 || !msgs[0]?.media) {
        return null;
      }

      const msg = msgs[0];
      const buffer = await client.downloadMedia(msg, {});
      if (!buffer || !(buffer instanceof Buffer) || buffer.length === 0) {
        return null;
      }

      let mimeType = 'application/octet-stream';
      let fileName: string | undefined = undefined;
      const mediaObj = msg.media as any;

      if (mediaObj.photo) {
        mimeType = 'image/jpeg';
        fileName = `photo_${messageId}.jpg`;
      } else if (mediaObj.document) {
        mimeType = mediaObj.document.mimeType || 'application/octet-stream';
        const docAttr = mediaObj.document.attributes?.find((a: any) => a.fileName || a.title);
        fileName = docAttr?.fileName || docAttr?.title || `file_${messageId}`;
      } else if (mediaObj.voice) {
        mimeType = 'audio/ogg';
        fileName = `voice_${messageId}.ogg`;
      }

      return { buffer, mimeType, fileName };
    } catch (err: any) {
      if (
        err?.message?.includes('AUTH_KEY_UNREGISTERED') ||
        err?.errorMessage === 'AUTH_KEY_UNREGISTERED' ||
        err?.code === 401
      ) {
        this.purgeStaleSession(sessionToken);
      }
      return null;
    }
  }

  /**
   * 5. Account Privacy Settings Subsystem (account.getPrivacy / account.setPrivacy)
   */
  public static async getPrivacy(sessionToken: string, key: string) {
    const client = await this.getOrCreateClient(sessionToken);
    let inputKey: any = new Api.InputPrivacyKeyStatusTimestamp();
    if (key === 'phoneNumber' || key === 'phone_number') inputKey = new Api.InputPrivacyKeyPhoneNumber();
    else if (key === 'profilePhotos' || key === 'profile_photos') inputKey = new Api.InputPrivacyKeyProfilePhoto();
    else if (key === 'forwards' || key === 'forwarded_messages') inputKey = new Api.InputPrivacyKeyForwards();
    else if (key === 'calls') inputKey = new Api.InputPrivacyKeyPhoneCall();
    else if (key === 'voiceMessages' || key === 'voice_messages') inputKey = new Api.InputPrivacyKeyVoiceMessages();
    else if (key === 'bio') inputKey = new Api.InputPrivacyKeyAbout();

    const res: any = await client.invoke(new Api.account.GetPrivacy({ key: inputKey }));
    const sanitized = sanitizeData(res);
    
    // Parse the effective privacy option ('everybody' | 'contacts' | 'nobody')
    let option: 'everybody' | 'contacts' | 'nobody' = 'contacts';
    const rulesList = Array.isArray(res.rules) ? res.rules : [];
    for (const r of rulesList) {
      const cls = r?.className || r?.constructor?.name || '';
      if (cls.includes('AllowAll') || cls === 'PrivacyValueAllowAll') {
        option = 'everybody';
        break;
      }
      if (cls.includes('AllowContacts') || cls === 'PrivacyValueAllowContacts') {
        option = 'contacts';
        break;
      }
      if (cls.includes('DisallowAll') || cls === 'PrivacyValueDisallowAll') {
        option = 'nobody';
        break;
      }
    }

    return {
      ...sanitized,
      key,
      option,
    };
  }

  public static async setPrivacy(sessionToken: string, key: string, rules?: any) {
    const client = await this.getOrCreateClient(sessionToken);
    let inputKey: any = new Api.InputPrivacyKeyStatusTimestamp();
    if (key === 'phoneNumber' || key === 'phone_number') inputKey = new Api.InputPrivacyKeyPhoneNumber();
    else if (key === 'profilePhotos' || key === 'profile_photos') inputKey = new Api.InputPrivacyKeyProfilePhoto();
    else if (key === 'forwards' || key === 'forwarded_messages') inputKey = new Api.InputPrivacyKeyForwards();
    else if (key === 'calls') inputKey = new Api.InputPrivacyKeyPhoneCall();
    else if (key === 'voiceMessages' || key === 'voice_messages') inputKey = new Api.InputPrivacyKeyVoiceMessages();
    else if (key === 'bio') inputKey = new Api.InputPrivacyKeyAbout();

    let finalRules: any[] = [];
    let chosenOption: 'everybody' | 'contacts' | 'nobody' = 'everybody';

    if (typeof rules === 'string') {
      if (rules === 'contacts' || rules === 'allow_contacts') {
        finalRules = [new Api.InputPrivacyValueAllowContacts()];
        chosenOption = 'contacts';
      } else if (rules === 'nobody' || rules === 'disallow_all') {
        finalRules = [new Api.InputPrivacyValueDisallowAll()];
        chosenOption = 'nobody';
      } else {
        finalRules = [new Api.InputPrivacyValueAllowAll()];
        chosenOption = 'everybody';
      }
    } else if (Array.isArray(rules) && rules.length > 0) {
      finalRules = rules.map((r: any) => {
        if (typeof r === 'string') {
          if (r === 'contacts' || r === 'allow_contacts') { chosenOption = 'contacts'; return new Api.InputPrivacyValueAllowContacts(); }
          if (r === 'nobody' || r === 'disallow_all') { chosenOption = 'nobody'; return new Api.InputPrivacyValueDisallowAll(); }
          chosenOption = 'everybody';
          return new Api.InputPrivacyValueAllowAll();
        }
        if (r?.className === 'InputPrivacyValueAllowContacts') { chosenOption = 'contacts'; return new Api.InputPrivacyValueAllowContacts(); }
        if (r?.className === 'InputPrivacyValueDisallowAll') { chosenOption = 'nobody'; return new Api.InputPrivacyValueDisallowAll(); }
        if (r?.className === 'InputPrivacyValueAllowAll') { chosenOption = 'everybody'; return new Api.InputPrivacyValueAllowAll(); }
        return r;
      });
    } else {
      finalRules = [new Api.InputPrivacyValueAllowAll()];
    }

    const res: any = await client.invoke(new Api.account.SetPrivacy({ key: inputKey, rules: finalRules }));
    const sanitized = sanitizeData(res);
    return {
      ...sanitized,
      key,
      option: chosenOption,
    };
  }

  /**
   * 5b. Two-Factor Authentication Subsystem (account.getPassword)
   */
  public static async getPassword(sessionToken: string) {
    const client = await this.getOrCreateClient(sessionToken);
    const pwd: any = await client.invoke(new Api.account.GetPassword());
    return {
      hasPassword: Boolean(pwd.hasPassword),
      hasRecovery: Boolean(pwd.hasRecovery),
      hint: pwd.hint || '',
      loginEmailPattern: pwd.loginEmailPattern || pwd.emailUnconfirmedPattern || '',
      emailUnconfirmedPattern: pwd.emailUnconfirmedPattern || '',
      pendingResetDate: pwd.pendingResetDate || undefined,
    };
  }

  /**
   * 6. Chat Notification Settings (Mute / Unmute / Sound)
   */
  public static async getNotifySettings(sessionToken: string, peerId: string) {
    const client = await this.getOrCreateClient(sessionToken);
    const peer = await resolvePeer(client, peerId);
    const inputPeer = await client.getInputEntity(peer);
    const res: any = await client.invoke(
      new Api.account.GetNotifySettings({
        peer: new Api.InputNotifyPeer({ peer: inputPeer as any }),
      })
    );
    return sanitizeData(res);
  }

  public static async updateNotifySettings(sessionToken: string, peerId: string, muteUntil: number = 0) {
    const client = await this.getOrCreateClient(sessionToken);
    const peer = await resolvePeer(client, peerId);
    const inputPeer = await client.getInputEntity(peer);
    const settings = new Api.InputPeerNotifySettings({
      muteUntil,
      showPreviews: true,
      silent: muteUntil > 0,
    });
    const res = await client.invoke(
      new Api.account.UpdateNotifySettings({
        peer: new Api.InputNotifyPeer({ peer: inputPeer as any }),
        settings,
      })
    );
    return { success: !!res, muteUntil };
  }

  /**
   * 7. Contacts Block & Unblock
   */
  public static async blockUser(sessionToken: string, peerId: string) {
    const client = await this.getOrCreateClient(sessionToken);
    const peer = await resolvePeer(client, peerId);
    const inputPeer = await client.getInputEntity(peer);
    const res = await client.invoke(new Api.contacts.Block({ id: inputPeer as any }));
    return { success: !!res };
  }

  public static async unblockUser(sessionToken: string, peerId: string) {
    const client = await this.getOrCreateClient(sessionToken);
    const peer = await resolvePeer(client, peerId);
    const inputPeer = await client.getInputEntity(peer);
    const res = await client.invoke(new Api.contacts.Unblock({ id: inputPeer as any }));
    return { success: !!res };
  }

  public static async getBlocked(sessionToken: string) {
    const client = await this.getOrCreateClient(sessionToken);
    const res: any = await client.invoke(new Api.contacts.GetBlocked({ offset: 0, limit: 100 }));
    return sanitizeData(res);
  }

  /**
   * 8. Profile Photo & Avatar Binary Downloader (photos.getUserPhotos / downloadProfilePhoto)
   */
  public static async downloadProfilePhoto(sessionToken: string, peerId: string, isBig: boolean = false) {
    return FloodWaitQueue.executeWithFloodRetry(`download_photo_${peerId}`, async () => {
      try {
        let validToken = sessionToken;
        if (!validToken || !(await this.isAuthorized(validToken))) {
          const active = this.getActiveSessionToken(validToken);
          if (active) validToken = active;
        }
        const client = await this.getOrCreateClient(validToken);
        const s = activeSessions.get(validToken);
        let entity: any;

        if (peerId === 'me' || peerId === 'self') {
          entity = await client.getMe();
        } else {
          const cleanId = extractPeerId(peerId);
          // 1. Check fullEntityCache
          if (s?.fullEntityCache?.has(cleanId)) {
            entity = s.fullEntityCache.get(cleanId);
          } else if (cleanId.startsWith('-100') && s?.fullEntityCache?.has(cleanId.slice(4))) {
            entity = s.fullEntityCache.get(cleanId.slice(4));
          } else if (!cleanId.startsWith('-100') && s?.fullEntityCache?.has('-100' + cleanId)) {
            entity = s.fullEntityCache.get('-100' + cleanId);
          }

          // 2. If not found in cache, attempt client.getEntity
          if (!entity) {
            try {
              entity = await client.getEntity(cleanId);
            } catch {
              try {
                if (!cleanId.startsWith('-100')) {
                  entity = await client.getEntity('-100' + cleanId);
                }
              } catch {
                entity = await resolvePeer(client, peerId);
              }
            }
          }
        }
        if (!entity) return null;

        const buffer = await client.downloadProfilePhoto(entity, { isBig });
        if (!buffer || !(buffer instanceof Buffer) || buffer.length === 0) {
          return null;
        }
        return {
          buffer,
          mimeType: 'image/jpeg',
          fileName: `avatar_${peerId}.jpg`,
        };
      } catch (err: any) {
        console.warn(`[TelegramService] Warning downloading profile photo for ${peerId}:`, err?.message || err);
        return null;
      }
    });
  }

  /**
   * 9. Active Authorizations & Devices Management (account.getAuthorizations, account.resetAuthorization, auth.resetAuthorizations)
   */
  public static async getAuthorizations(sessionToken: string) {
    return FloodWaitQueue.executeWithFloodRetry('account.getAuthorizations', async () => {
      const client = await this.getOrCreateClient(sessionToken);
      const res: any = await client.invoke(new Api.account.GetAuthorizations());
      return sanitizeData(res);
    });
  }

  public static async resetAuthorization(sessionToken: string, hash: string | number | bigint) {
    return FloodWaitQueue.executeWithFloodRetry(`account.resetAuthorization_${hash}`, async () => {
      const client = await this.getOrCreateClient(sessionToken);
      const res: any = await client.invoke(
        new Api.account.ResetAuthorization({ hash: BigInt(String(hash)) as any })
      );
      return { success: Boolean(res) };
    });
  }

  public static async resetAllOtherAuthorizations(sessionToken: string) {
    return FloodWaitQueue.executeWithFloodRetry('auth.resetAuthorizations', async () => {
      const client = await this.getOrCreateClient(sessionToken);
      const res: any = await client.invoke(new Api.auth.ResetAuthorizations());
      return { success: Boolean(res) };
    });
  }

  public static async setAuthorizationTTL(sessionToken: string, days: number) {
    return FloodWaitQueue.executeWithFloodRetry('account.setAuthorizationTTL', async () => {
      const client = await this.getOrCreateClient(sessionToken);
      const res: any = await client.invoke(
        new Api.account.SetAuthorizationTTL({
          authorizationTtlDays: Number(days) || 180,
        })
      );
      return { success: Boolean(res) };
    });
  }

}
