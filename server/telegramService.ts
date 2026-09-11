import { TelegramClient, Api } from 'telegram';
import { StringSession } from 'telegram/sessions/index.js';
import { LogLevel } from 'telegram/extensions/Logger.js';
import fs from 'fs';
import path from 'path';
import { ensureTelegramPatch } from './patchTelegram.js';

// Ensure 256-bit DH key padding patch is present
ensureTelegramPatch();

// Constants permanently embedded as requested by the user
export const TELEGRAM_API_ID = 22043994;
export const TELEGRAM_API_HASH = '56f64582b363d367280db96586b97801';
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

// Load saved multi-account storage
function loadStorage(): MultiAccountStorage {
  try {
    if (fs.existsSync(SESSIONS_FILE)) {
      const raw = fs.readFileSync(SESSIONS_FILE, 'utf-8');
      const data = JSON.parse(raw);
      if (data && typeof data === 'object') {
        // Migration from legacy flat format { [token]: stringSession }
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
  public static async getOrCreateClient(sessionToken: string, sessionString: string = ''): Promise<TelegramClient> {
    const existing = activeSessions.get(sessionToken);
    if (existing && existing.client) {
      if (!existing.client.connected) {
        await existing.client.connect();
      }
      return existing.client;
    }

    // If an existing connection attempt is in-flight for this sessionToken, await it
    if (connectionLocks.has(sessionToken)) {
      return connectionLocks.get(sessionToken)!;
    }

    const connectPromise = (async () => {
      // Check if saved on disk
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

      // Avoid noisy debug logs in console
      client.setLogLevel(LogLevel.WARN);

      try {
        await client.connect();
      } catch (connErr: any) {
        // If saved session was corrupt or invalidated, purge it
        if (sessionString) {
          removePersistedSession(sessionToken);
          // Try a clean unauthenticated session
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

    return {
      success: true,
      phoneCodeHash: res.phoneCodeHash,
      isCodeViaApp: res.isCodeViaApp,
    };
  }

  public static async signIn(sessionToken: string, phoneCode: string, phoneCodeHash?: string) {
    const session = activeSessions.get(sessionToken);
    if (!session) {
      throw new Error('No active authentication session found. Please request a code first.');
    }

    const hash = phoneCodeHash || session.phoneCodeHash;
    const phone = session.phoneNumber;

    if (!hash || !phone) {
      throw new Error('Phone number or phone code hash is missing. Please restart login.');
    }

    const client = session.client;

    try {
      const result = await client.invoke(
        new Api.auth.SignIn({
          phoneNumber: phone,
          phoneCodeHash: hash,
          phoneCode: phoneCode.trim(),
        })
      );

      const me = await client.getMe();
      session.isLoggedIn = true;
      session.user = me;

      // Save StringSession
      const sessionString = client.session.save() as unknown as string;
      persistSession(sessionToken, sessionString);
      this.saveAccount(sessionToken, me);

      return {
        success: true,
        user: sanitizeData(me),
        sessionString,
      };
    } catch (err: any) {
      if (err.errorMessage === 'SESSION_PASSWORD_NEEDED') {
        return {
          success: false,
          needs2FA: true,
          message: 'Two-Step Verification (2FA) password is required.',
        };
      }
      throw err;
    }
  }

  public static async signInWithPassword(sessionToken: string, password: string) {
    const session = activeSessions.get(sessionToken);
    if (!session) {
      throw new Error('No active session.');
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
    this.saveAccount(sessionToken, me);

    return {
      success: true,
      user: sanitizeData(me),
      sessionString,
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

    // Try loading saved session
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

  public static async getDialogs(sessionToken: string, limit: number = 40) {
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
    const messages = await client.getMessages(peerId, { limit });

    return messages.map((m: any) => {
      let mediaType: string | undefined;
      let mediaTitle: string | undefined;

      if (m.media) {
        const className = m.media.className || '';
        if (className.includes('Photo')) mediaType = 'photo';
        else if (className.includes('Document')) {
          const mime = m.media.document?.mimeType || '';
          if (mime.startsWith('audio/') || mime.includes('ogg')) mediaType = 'voice';
          else mediaType = 'document';
          mediaTitle = m.media.document?.attributes?.find((a: any) => a.fileName)?.fileName || 'مستند';
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
            }
          : undefined,
        reactions: m.reactions?.results?.map((r: any) => ({
          emoji: r.reaction?.emoticon || '❤️',
          count: r.count,
        })),
      };
    });
  }

  public static async sendMessage(sessionToken: string, peerId: string, text: string, replyTo?: number) {
    const client = await this.getOrCreateClient(sessionToken);
    const res = await client.sendMessage(peerId, {
      message: text,
      replyTo: replyTo ? Number(replyTo) : undefined,
    });
    return sanitizeData(res);
  }

  public static async setTyping(sessionToken: string, peerId: string, action: string = 'typing') {
    try {
      const client = await this.getOrCreateClient(sessionToken);
      const { Api } = await import('telegram');
      let act: any = new Api.SendMessageTypingAction();
      if (action === 'recording') {
        act = new Api.SendMessageRecordAudioAction();
      } else if (action === 'uploading') {
        act = new Api.SendMessageUploadDocumentAction({ progress: 50 });
      }
      await client.invoke(
        new Api.messages.SetTyping({
          peer: peerId,
          action: act,
        })
      );
      return { success: true };
    } catch (err: any) {
      // In case peer is simulated or client not yet ready
      return { success: true, simulated: true };
    }
  }

  public static async logout(sessionToken: string) {
    const session = activeSessions.get(sessionToken);
    if (session && session.client) {
      try {
        await session.client.disconnect();
      } catch {
        // Ignore
      }
    }
    activeSessions.delete(sessionToken);
    removePersistedSession(sessionToken);
    return { success: true };
  }
}
