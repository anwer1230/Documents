import { TelegramClient, Api } from 'telegram';
import { StringSession } from 'telegram/sessions/index.js';
import { LogLevel } from 'telegram/extensions/Logger.js';
import fs from 'fs';
import path from 'path';
import { ensureTelegramPatch } from './patchTelegram.js';

// Ensure 256-bit DH key padding patch is present
ensureTelegramPatch();

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
}

const activeSessions = new Map<string, ActiveSession>();
const connectionLocks = new Map<string, Promise<TelegramClient>>();
const pendingAuthByHash = new Map<string, { sessionToken: string; phoneNumber: string; createdAt: number }>();
const pendingAuthByPhone = new Map<string, { sessionToken: string; phoneCodeHash?: string; createdAt: number }>();
const SESSIONS_FILE = path.join(process.cwd(), '.telegram_sessions.json');

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

// Peer resolving helper for GramJS (handles usernames, string numeric IDs, channel IDs)
export async function resolvePeer(client: TelegramClient, peerId: string | number): Promise<any> {
  if (peerId === 'me' || peerId === 'self') {
    return 'me';
  }
  if (typeof peerId === 'object' && peerId !== null) {
    return peerId;
  }
  const str = String(peerId).trim();
  if (!str) return 'me';

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

  // Handle channel IDs with -100 prefix
  if (str.startsWith('-100')) {
    const numPart = str.slice(4);
    try {
      return await client.getInputEntity(Number(numPart));
    } catch {
      try {
        return await client.getEntity(Number(numPart));
      } catch {
        // fallback
      }
    }
  }

  // Regular input entity lookup
  try {
    return await client.getInputEntity(str);
  } catch {
    try {
      return await client.getEntity(str);
    } catch {
      return str;
    }
  }
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
   * Bootstraps continuous MTProto listeners for all saved accounts on server startup
   */
  public static async initAllSavedSessions(): Promise<void> {
    const storage = loadStorage();
    const tokens = Object.keys(storage.sessions);
    console.log(`[MTProto Updates Engine] Bootstrapping continuous listeners for ${tokens.length} saved sessions`);
    for (const token of tokens) {
      try {
        await this.getOrCreateClient(token, storage.sessions[token]);
        console.log(`[MTProto Updates Engine] Connected & listening on session: ${token.slice(0, 10)}...`);
      } catch (err: any) {
        console.warn(`[MTProto Updates Engine] Warning restoring session ${token}:`, err?.message || err);
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

  public static async getMe(sessionToken: string) {
    const session = activeSessions.get(sessionToken);
    if (session && session.client && session.client.connected) {
      try {
        const me = await session.client.getMe();
        if (me) {
          session.isLoggedIn = true;
          session.user = me;
          this.saveAccount(sessionToken, me);
          return { isLoggedIn: true, user: sanitizeData(me) };
        }
      } catch {
        // Fall through
      }
    }

    const storage = loadStorage();
    if (storage.sessions[sessionToken]) {
      try {
        const client = await this.getOrCreateClient(sessionToken, storage.sessions[sessionToken]);
        const me = await client.getMe();
        if (me) {
          const s = activeSessions.get(sessionToken);
          if (s) {
            s.isLoggedIn = true;
            s.user = me;
          }
          this.saveAccount(sessionToken, me);
          return { isLoggedIn: true, user: sanitizeData(me) };
        }
      } catch (err) {
        console.warn('Session expired or could not be restored, cleaning up stale session token.');
        removePersistedSession(sessionToken);
        activeSessions.delete(sessionToken);
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
    const accounts = storage.accounts.map(acc => {
      const active = activeSessions.get(acc.sessionToken);
      return {
        ...acc,
        isLoggedIn: !!(active?.isLoggedIn || storage.sessions[acc.sessionToken]),
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
    const cleanUser = {
      id: user.id?.toString() || 'me',
      firstName: user.firstName || 'مستخدم تيليجرام',
      lastName: user.lastName,
      username: user.username,
      phone: user.phone,
      photoUrl: user.photoUrl,
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
    const client = await this.getOrCreateClient(sessionToken);
    const dialogs = await client.getDialogs({ limit });
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

      return {
        id: d.id?.toString() || d.entity?.id?.toString(),
        title: d.title || d.name || 'محادثة',
        username: d.entity?.username || undefined,
        type,
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
  }

  public static async getMessages(sessionToken: string, peerId: string, limit: number = 50) {
    const client = await this.getOrCreateClient(sessionToken);
    const peer = await resolvePeer(client, peerId);
    const messages = await client.getMessages(peer, { limit });

    return messages.map((m: any) => {
      let mediaType: string | undefined;
      let mediaTitle: string | undefined;
      let mediaUrl: string | undefined;

      if (m.media) {
        const className = m.media.className || '';
        if (className.includes('Photo')) {
          mediaType = 'photo';
        } else if (className.includes('Document')) {
          const mime = m.media.document?.mimeType || '';
          if (mime.startsWith('audio/') || mime.includes('ogg')) {
            mediaType = 'voice';
          } else {
            mediaType = 'document';
          }
          mediaTitle = m.media.document?.attributes?.find((a: any) => a.fileName)?.fileName || 'مستند';
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

      return {
        id: m.id?.toString(),
        chatId: peerId,
        senderId: m.fromId?.userId?.toString() || (m.out ? 'me' : peerId),
        senderName: m.out ? 'أنا' : 'عضو',
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
}
