// @ts-ignore
import initSqlJs from 'sql.js/dist/sql-asm.js';
import type { Database } from 'sql.js';
import { get, set } from 'idb-keyval';
import { Chat, Message, User } from '../types';

/**
 * Official Telegram SQLite Database Architecture
 * Source Reference: DrKLO/Telegram (TMessagesProj/src/main/java/org/telegram/messenger/MessagesStorage.java)
 */
const SQLITE_STORAGE_KEY_PREFIX = 'telegram_sqlite_database_v2_acc_';

export interface TelegramUserConfigRecord {
  id: number;
  clientUserId: string | number;
  phone: string;
  sessionString: string;
  isAuthorized: boolean;
  syncTime: number;
  twoFaEnabled: boolean;
  dataJson?: string;
}

export interface TelegramDialogRecord {
  did: string | number;
  date: number;
  unreadCount: number;
  lastMid: string | number;
  inboxMax: number;
  outboxMax: number;
  pinned: boolean;
  flags: number;
  folderId: number;
  data: any;
}

export class TelegramSQLiteDatabase {
  private db: Database | null = null;
  private isInitialized = false;
  private initPromise: Promise<void> | null = null;
  private currentAccount: number = 0;

  constructor(account: number = 0) {
    this.currentAccount = account;
  }

  private get storageKey(): string {
    return `${SQLITE_STORAGE_KEY_PREFIX}${this.currentAccount}`;
  }

  public async init(): Promise<void> {
    if (this.isInitialized && this.db) return;
    if (this.initPromise) return this.initPromise;

    this.initPromise = (async () => {
      try {
        const SQL = await initSqlJs();
        if (!SQL) return;

        // Restore SQLite binary DB stored in IndexedDB (MMAP-like persistent local cache)
        const savedBinary = await get<Uint8Array>(this.storageKey);

        if (savedBinary && savedBinary.byteLength > 0) {
          this.db = new SQL.Database(savedBinary);
          console.log(`[SQLite DrKLO] Restored existing SQLite database for account ${this.currentAccount}`);
        } else {
          this.db = new SQL.Database();
          console.log(`[SQLite DrKLO] Created fresh SQLite database tables for account ${this.currentAccount}`);
        }

        this.bootstrapOfficialSchema();
        this.isInitialized = true;
      } catch (err) {
        console.warn(`[SQLite DrKLO] Fallback for account ${this.currentAccount} due to:`, err);
      }
    })();

    return this.initPromise;
  }

  /**
   * Official Telegram Schema Definitions (DrKLO MessagesStorage.java)
   */
  private bootstrapOfficialSchema(): void {
    if (!this.db) return;

    this.db.run(`
      -- 1. Users Table (DrKLO: users)
      CREATE TABLE IF NOT EXISTS users (
        uid TEXT PRIMARY KEY,
        name TEXT,
        username TEXT,
        phone TEXT,
        avatar TEXT,
        status INTEGER DEFAULT 0,
        is_premium INTEGER DEFAULT 0,
        bio TEXT,
        data BLOB
      );

      -- 2. Chats Table (DrKLO: chats)
      CREATE TABLE IF NOT EXISTS chats (
        uid TEXT PRIMARY KEY,
        type TEXT,
        title TEXT,
        username TEXT,
        avatar TEXT,
        participants_count INTEGER DEFAULT 0,
        is_verified INTEGER DEFAULT 0,
        is_secret INTEGER DEFAULT 0,
        data BLOB
      );

      -- 3. Messages Table (DrKLO: messages)
      CREATE TABLE IF NOT EXISTS messages (
        mid TEXT,
        uid TEXT,
        read_state INTEGER DEFAULT 0,
        send_state INTEGER DEFAULT 0,
        date INTEGER,
        sender_id TEXT,
        sender_name TEXT,
        text TEXT,
        out INTEGER DEFAULT 0,
        ttl INTEGER DEFAULT 0,
        media INTEGER DEFAULT 0,
        reply_to_mid TEXT,
        fwd_msg_id TEXT,
        reply_markup TEXT,
        is_channel INTEGER DEFAULT 0,
        is_secret INTEGER DEFAULT 0,
        expires_at INTEGER DEFAULT 0,
        media_json TEXT,
        data BLOB,
        PRIMARY KEY(mid, uid)
      );

      -- 4. Dialogs Table (DrKLO: dialogs)
      CREATE TABLE IF NOT EXISTS dialogs (
        did TEXT PRIMARY KEY,
        date INTEGER,
        unread_count INTEGER DEFAULT 0,
        last_mid TEXT,
        inbox_max INTEGER DEFAULT 0,
        outbox_max INTEGER DEFAULT 0,
        pinned INTEGER DEFAULT 0,
        flags INTEGER DEFAULT 0,
        folder_id INTEGER DEFAULT 0,
        title TEXT,
        type TEXT,
        username TEXT,
        avatar TEXT,
        is_muted INTEGER DEFAULT 0,
        last_message_text TEXT,
        last_message_time TEXT,
        data BLOB
      );

      -- 5. User Config & Session Table (DrKLO: user_config)
      CREATE TABLE IF NOT EXISTS user_config (
        id INTEGER PRIMARY KEY,
        client_user_id TEXT,
        phone TEXT,
        session_string TEXT,
        is_authorized INTEGER DEFAULT 0,
        sync_time INTEGER DEFAULT 0,
        two_fa_enabled INTEGER DEFAULT 0,
        data_json TEXT
      );

      -- 6. Params Table (DrKLO: params - pts, qts, seq, date)
      CREATE TABLE IF NOT EXISTS params (
        id INTEGER PRIMARY KEY,
        seq INTEGER DEFAULT 0,
        pts INTEGER DEFAULT 0,
        date INTEGER DEFAULT 0,
        qts INTEGER DEFAULT 0,
        lsv INTEGER DEFAULT 0,
        unread_count INTEGER DEFAULT 0
      );

      -- 7. Contacts Table (DrKLO: contacts)
      CREATE TABLE IF NOT EXISTS contacts (
        uid TEXT PRIMARY KEY,
        mutual INTEGER DEFAULT 1,
        data BLOB
      );

      -- 8. Secret Chats Table (DrKLO: secret_chats)
      CREATE TABLE IF NOT EXISTS secret_chats (
        did TEXT PRIMARY KEY,
        user_id TEXT,
        admin_id TEXT,
        dh_public_key TEXT,
        dh_shared_secret TEXT,
        fingerprint TEXT,
        ttl INTEGER DEFAULT 0,
        layer INTEGER DEFAULT 184,
        seq_in INTEGER DEFAULT 0,
        seq_out INTEGER DEFAULT 0
      );

      -- 9. Dialog Filters (Folders) Table (DrKLO: dialog_filter)
      CREATE TABLE IF NOT EXISTS dialog_filter (
        id INTEGER PRIMARY KEY,
        order_value INTEGER DEFAULT 0,
        unread_count INTEGER DEFAULT 0,
        title TEXT,
        flags INTEGER DEFAULT 0,
        filter_json TEXT
      );

      -- 10. Stories Table (DrKLO: stories)
      CREATE TABLE IF NOT EXISTS stories (
        id TEXT PRIMARY KEY,
        user_id TEXT,
        user_name TEXT,
        user_avatar TEXT,
        media_url TEXT,
        media_type TEXT,
        caption TEXT,
        timestamp TEXT,
        expires_at INTEGER,
        views_count INTEGER DEFAULT 0,
        is_viewed INTEGER DEFAULT 0,
        is_my_story INTEGER DEFAULT 0
      );

      -- Performance Indices
      CREATE INDEX IF NOT EXISTS idx_messages_uid_date ON messages(uid, date);
      CREATE INDEX IF NOT EXISTS idx_dialogs_pinned_date ON dialogs(pinned, date);
      CREATE INDEX IF NOT EXISTS idx_dialogs_folder ON dialogs(folder_id);
    `);

    this.persist();
  }

  public async persist(): Promise<void> {
    if (!this.db) return;
    try {
      const data = this.db.export();
      await set(this.storageKey, data);
    } catch (e) {
      console.warn('[SQLite Persistence] Error exporting database:', e);
    }
  }

  // =========================================================================
  // 1. DIALOGS & CHATS (DrKLO: getDialogs, putDialogs, setDialogFlags)
  // =========================================================================

  public saveChats(chats: Chat[]): void {
    if (!this.db) return;
    const stmt = this.db.prepare(`
      INSERT OR REPLACE INTO dialogs (
        did, date, unread_count, last_mid, pinned, flags, folder_id,
        title, type, username, avatar, is_muted, last_message_text, last_message_time, data
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `);

    for (const c of chats) {
      const now = Date.now();
      stmt.run([
        String(c.id),
        now,
        c.unreadCount || 0,
        c.lastMessage?.id ? String(c.lastMessage.id) : '',
        c.isPinned ? 1 : 0,
        0,
        c.folderId || 0,
        c.title || '',
        c.type || 'private',
        c.username || '',
        c.avatar || '',
        c.isMuted ? 1 : 0,
        c.lastMessage?.text || '',
        c.lastMessage?.timestamp || '',
        JSON.stringify(c),
      ]);
    }
    stmt.free();
    this.persist();
  }

  public getChats(): Chat[] {
    if (!this.db) return [];
    try {
      const res = this.db.exec('SELECT * FROM dialogs ORDER BY pinned DESC, date DESC');
      if (res.length > 0 && res[0].values) {
        const cols = res[0].columns;
        return res[0].values.map((row) => {
          const obj: any = {};
          cols.forEach((col, idx) => {
            obj[col] = row[idx];
          });
          if (obj.data) {
            try {
              return JSON.parse(obj.data);
            } catch {}
          }
          return {
            id: obj.did,
            type: obj.type || 'private',
            title: obj.title || '',
            username: obj.username || undefined,
            avatar: obj.avatar || '',
            unreadCount: obj.unread_count || 0,
            isPinned: Boolean(obj.pinned),
            isMuted: Boolean(obj.is_muted),
            folderId: obj.folder_id || 0,
            lastMessage: obj.last_message_text
              ? {
                  id: obj.last_mid || 'm_last',
                  senderName: '',
                  text: obj.last_message_text,
                  timestamp: obj.last_message_time || '',
                  isOutgoing: false,
                  status: 'read',
                }
              : undefined,
          };
        });
      }
    } catch (e) {
      console.error('[SQLite] getChats error:', e);
    }
    return [];
  }

  public deleteChat(chatId: string): void {
    if (!this.db) return;
    try {
      this.db.run('DELETE FROM dialogs WHERE did = ?', [chatId]);
      this.db.run('DELETE FROM messages WHERE uid = ?', [chatId]);
      this.persist();
    } catch (e) {
      console.error('[SQLite] deleteChat error:', e);
    }
  }

  public setDialogPinned(chatId: string, isPinned: boolean): void {
    if (!this.db) return;
    try {
      this.db.run('UPDATE dialogs SET pinned = ? WHERE did = ?', [isPinned ? 1 : 0, chatId]);
      this.persist();
    } catch (e) {
      console.error('[SQLite] setDialogPinned error:', e);
    }
  }

  // =========================================================================
  // 2. MESSAGES (DrKLO: getMessages, putMessages, deleteMessages)
  // =========================================================================

  public saveMessage(msg: Message, isSecret: boolean = false, expiresAt: number = 0): void {
    if (!this.db) return;
    try {
      this.db.run(
        `INSERT OR REPLACE INTO messages (
          mid, uid, read_state, send_state, date, sender_id, sender_name,
          text, out, ttl, media, reply_to_mid, is_secret, expires_at, media_json, data
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        [
          String(msg.id),
          String(msg.chatId),
          msg.status === 'read' ? 1 : 0,
          0,
          Date.now(),
          msg.senderId || '',
          msg.senderName || '',
          msg.text || '',
          msg.isOutgoing ? 1 : 0,
          0,
          msg.media ? 1 : 0,
          msg.replyTo?.id ? String(msg.replyTo.id) : null,
          isSecret ? 1 : 0,
          expiresAt || 0,
          msg.media ? JSON.stringify(msg.media) : null,
          JSON.stringify(msg),
        ]
      );
      this.persist();
    } catch (e) {
      console.error('[SQLite] saveMessage error:', e);
    }
  }

  public saveMessages(messages: Message[]): void {
    if (!this.db || messages.length === 0) return;
    const stmt = this.db.prepare(`
      INSERT OR REPLACE INTO messages (
        mid, uid, read_state, send_state, date, sender_id, sender_name,
        text, out, ttl, media, reply_to_mid, is_secret, expires_at, media_json, data
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `);

    for (const msg of messages) {
      stmt.run([
        String(msg.id),
        String(msg.chatId),
        msg.status === 'read' ? 1 : 0,
        0,
        Date.now(),
        msg.senderId || '',
        msg.senderName || '',
        msg.text || '',
        msg.isOutgoing ? 1 : 0,
        0,
        msg.media ? 1 : 0,
        msg.replyTo?.id ? String(msg.replyTo.id) : null,
        0,
        0,
        msg.media ? JSON.stringify(msg.media) : null,
        JSON.stringify(msg),
      ]);
    }
    stmt.free();
    this.persist();
  }

  public async getMessages(chatId: string): Promise<Message[]> {
    if (!this.db) return [];
    try {
      const stmt = this.db.prepare('SELECT data, media_json, mid, uid, sender_id, sender_name, text, out, read_state FROM messages WHERE uid = ? ORDER BY date ASC, mid ASC');
      stmt.bind([String(chatId)]);
      const results: Message[] = [];
      while (stmt.step()) {
        const row: any = stmt.getAsObject();
        if (row.data) {
          try {
            results.push(JSON.parse(row.data));
            continue;
          } catch {}
        }
        let media = undefined;
        if (row.media_json) {
          try {
            media = JSON.parse(row.media_json);
          } catch {}
        }
        results.push({
          id: String(row.mid),
          chatId: String(row.uid),
          senderId: String(row.sender_id),
          senderName: String(row.sender_name || ''),
          text: String(row.text || ''),
          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          date: new Date().toISOString().split('T')[0],
          isOutgoing: Boolean(row.out),
          status: row.read_state === 1 ? 'read' : 'sent',
          media,
        });
      }
      stmt.free();
      return results;
    } catch (e) {
      console.error('[SQLite] getMessages error:', e);
      return [];
    }
  }

  public getMessagesForChat(chatId: string): Message[] {
    if (!this.db) return [];
    try {
      const stmt = this.db.prepare('SELECT data, media_json, mid, uid, sender_id, sender_name, text, out, read_state FROM messages WHERE uid = ? ORDER BY date ASC, mid ASC');
      stmt.bind([String(chatId)]);
      const results: Message[] = [];
      while (stmt.step()) {
        const row: any = stmt.getAsObject();
        if (row.data) {
          try {
            results.push(JSON.parse(row.data));
            continue;
          } catch {}
        }
        let media = undefined;
        if (row.media_json) {
          try {
            media = JSON.parse(row.media_json);
          } catch {}
        }
        results.push({
          id: String(row.mid),
          chatId: String(row.uid),
          senderId: String(row.sender_id),
          senderName: String(row.sender_name || ''),
          text: String(row.text || ''),
          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          date: new Date().toISOString().split('T')[0],
          isOutgoing: Boolean(row.out),
          status: row.read_state === 1 ? 'read' : 'sent',
          media,
        });
      }
      stmt.free();
      return results;
    } catch (e) {
      console.error('[SQLite] getMessagesForChat error:', e);
      return [];
    }
  }

  public async deleteMessage(id: string): Promise<void> {
    if (!this.db) return;
    try {
      this.db.run('DELETE FROM messages WHERE mid = ?', [String(id)]);
      this.persist();
    } catch (e) {
      console.error('[SQLite] deleteMessage error:', e);
    }
  }

  public async deleteMessages(messageIds: Array<string | number>): Promise<void> {
    if (!this.db || messageIds.length === 0) return;
    try {
      const placeholders = messageIds.map(() => '?').join(',');
      this.db.run(`DELETE FROM messages WHERE mid IN (${placeholders})`, messageIds.map(String));
      this.persist();
    } catch (e) {
      console.error('[SQLite] deleteMessages error:', e);
    }
  }

  // =========================================================================
  // 3. USER CONFIG & SESSION PERSISTENCE (DrKLO: UserConfig.java)
  // =========================================================================

  public saveUserConfig(config: TelegramUserConfigRecord): void {
    if (!this.db) return;
    try {
      this.db.run(
        `INSERT OR REPLACE INTO user_config (id, client_user_id, phone, session_string, is_authorized, sync_time, two_fa_enabled, data_json)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
        [
          config.id || 0,
          String(config.clientUserId || ''),
          config.phone || '',
          config.sessionString || '',
          config.isAuthorized ? 1 : 0,
          config.syncTime || Date.now(),
          config.twoFaEnabled ? 1 : 0,
          config.dataJson || null,
        ]
      );
      this.persist();
    } catch (e) {
      console.error('[SQLite] saveUserConfig error:', e);
    }
  }

  public getUserConfig(id: number = 0): TelegramUserConfigRecord | null {
    if (!this.db) return null;
    try {
      const stmt = this.db.prepare('SELECT * FROM user_config WHERE id = ?');
      stmt.bind([id]);
      if (stmt.step()) {
        const row: any = stmt.getAsObject();
        stmt.free();
        return {
          id: row.id,
          clientUserId: row.client_user_id,
          phone: row.phone,
          sessionString: row.session_string,
          isAuthorized: Boolean(row.is_authorized),
          syncTime: row.sync_time,
          twoFaEnabled: Boolean(row.two_fa_enabled),
          dataJson: row.data_json,
        };
      }
      stmt.free();
    } catch (e) {
      console.error('[SQLite] getUserConfig error:', e);
    }
    return null;
  }

  public clearUserConfig(id: number = 0): void {
    if (!this.db) return;
    try {
      this.db.run('DELETE FROM user_config WHERE id = ?', [id]);
      this.persist();
    } catch (e) {
      console.error('[SQLite] clearUserConfig error:', e);
    }
  }

  // =========================================================================
  // 4. USERS & CONTACTS (DrKLO: users, contacts)
  // =========================================================================

  public saveContacts(contacts: User[]): void {
    if (!this.db) return;
    const stmt = this.db.prepare(`
      INSERT OR REPLACE INTO users (uid, name, username, phone, avatar, status, is_premium, bio, data)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    `);

    for (const u of contacts) {
      stmt.run([
        String(u.id),
        u.name,
        u.username || '',
        u.phone || '',
        u.avatar || '',
        u.isOnline ? 1 : 0,
        u.isPremium ? 1 : 0,
        u.bio || '',
        JSON.stringify(u),
      ]);
    }
    stmt.free();
    this.persist();
  }

  public getContacts(): User[] {
    if (!this.db) return [];
    try {
      const res = this.db.exec('SELECT * FROM users ORDER BY name ASC');
      if (res.length > 0 && res[0].values) {
        const cols = res[0].columns;
        return res[0].values.map((row) => {
          const obj: any = {};
          cols.forEach((col, idx) => {
            obj[col] = row[idx];
          });
          if (obj.data) {
            try {
              return JSON.parse(obj.data);
            } catch {}
          }
          return {
            id: obj.uid,
            name: obj.name,
            username: obj.username || undefined,
            phone: obj.phone || undefined,
            avatar: obj.avatar || '',
            isOnline: Boolean(obj.status),
            isPremium: Boolean(obj.is_premium),
            bio: obj.bio || '',
          };
        });
      }
    } catch (e) {
      console.error('[SQLite] getContacts error:', e);
    }
    return [];
  }

  // =========================================================================
  // 5. SECRET SESSIONS (DrKLO: secret_chats)
  // =========================================================================

  public saveSecretSession(chatId: string, fingerprint: string, sharedKey: string, ttl: number): void {
    if (!this.db) return;
    try {
      this.db.run(
        `INSERT OR REPLACE INTO secret_chats (did, user_id, admin_id, dh_public_key, dh_shared_secret, fingerprint, ttl, layer)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
        [String(chatId), String(chatId), 'self', 'DH_PUB_' + Math.random().toString(36).substring(7), sharedKey, fingerprint, ttl, 184]
      );
      this.persist();
    } catch (e) {
      console.error('[SQLite] saveSecretSession error:', e);
    }
  }

  public getSecretSession(chatId: string): any {
    if (!this.db) return null;
    try {
      const stmt = this.db.prepare('SELECT * FROM secret_chats WHERE did = ?');
      stmt.bind([String(chatId)]);
      if (stmt.step()) {
        const res = stmt.getAsObject();
        stmt.free();
        return res;
      }
      stmt.free();
    } catch (e) {
      console.error('[SQLite] getSecretSession error:', e);
    }
    return null;
  }

  public purgeExpiredSecretMessages(): void {
    if (!this.db) return;
    try {
      const now = Date.now();
      this.db.run('DELETE FROM messages WHERE is_secret = 1 AND expires_at > 0 AND expires_at < ?', [now]);
      this.persist();
    } catch (e) {
      console.error('[SQLite] Error purging expired messages:', e);
    }
  }

  /**
   * DrKLO: cleanUp - Purges all tables for this account
   */
  public cleanUp(): void {
    if (!this.db) return;
    try {
      this.db.run('DELETE FROM dialogs');
      this.db.run('DELETE FROM messages');
      this.db.run('DELETE FROM users');
      this.db.run('DELETE FROM chats');
      this.db.run('DELETE FROM user_config');
      this.db.run('DELETE FROM secret_chats');
      this.persist();
    } catch (e) {
      console.error('[SQLite] cleanUp error:', e);
    }
  }
}

export const telegramDB = new TelegramSQLiteDatabase(0);

