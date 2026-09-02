// TDLib Persistent Database Engine
// Implements Telegram Database Library's internal SQLite / IndexedDB hybrid storage model

import { TdChat, TdMessage, TdUser, TdFile, TdDraftMessage } from '../types/tdlib';

const TDLIB_DB_NAME = 'tdlib_database_v1';
const TDLIB_DB_VERSION = 1;

export class TdlibDatabase {
  private db: IDBDatabase | null = null;
  private isOpening = false;

  public async getDb(): Promise<IDBDatabase> {
    if (this.db) return this.db;
    if (this.isOpening) {
      // Wait for existing open promise
      await new Promise(r => setTimeout(r, 100));
      return this.getDb();
    }

    this.isOpening = true;
    try {
      this.db = await new Promise<IDBDatabase>((resolve, reject) => {
        const req = indexedDB.open(TDLIB_DB_NAME, TDLIB_DB_VERSION);

        req.onupgradeneeded = (e: any) => {
          const db = e.target.result as IDBDatabase;

          // 1. TDLib Chats Store
          if (!db.objectStoreNames.contains('td_chats')) {
            const chatStore = db.createObjectStore('td_chats', { keyPath: 'id' });
            chatStore.createIndex('title', 'title', { unique: false });
            chatStore.createIndex('unread_count', 'unread_count', { unique: false });
          }

          // 2. TDLib Messages Store
          if (!db.objectStoreNames.contains('td_messages')) {
            const msgStore = db.createObjectStore('td_messages', { keyPath: 'id' });
            msgStore.createIndex('chat_id', 'chat_id', { unique: false });
            msgStore.createIndex('date', 'date', { unique: false });
          }

          // 3. TDLib Users Store
          if (!db.objectStoreNames.contains('td_users')) {
            const userStore = db.createObjectStore('td_users', { keyPath: 'id' });
            userStore.createIndex('username', 'username', { unique: false });
            userStore.createIndex('phone_number', 'phone_number', { unique: false });
          }

          // 4. TDLib Files & Media Cache Store
          if (!db.objectStoreNames.contains('td_files')) {
            db.createObjectStore('td_files', { keyPath: 'id' });
          }

          // 5. TDLib Drafts Store
          if (!db.objectStoreNames.contains('td_drafts')) {
            db.createObjectStore('td_drafts', { keyPath: 'chat_id' });
          }

          // 6. TDLib Options Key-Value Store
          if (!db.objectStoreNames.contains('td_options')) {
            db.createObjectStore('td_options', { keyPath: 'name' });
          }
        };

        req.onsuccess = () => resolve(req.result);
        req.onerror = () => reject(req.error);
      });
      return this.db;
    } finally {
      this.isOpening = false;
    }
  }

  // ─── CHAT OPERATIONS ────────────────────────────────────────────────────────

  async saveChat(chat: TdChat): Promise<void> {
    const db = await this.getDb();
    return new Promise((resolve, reject) => {
      const tx = db.transaction('td_chats', 'readwrite');
      const store = tx.objectStore('td_chats');
      store.put(chat);
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  }

  async saveChats(chats: TdChat[]): Promise<void> {
    const db = await this.getDb();
    return new Promise((resolve, reject) => {
      const tx = db.transaction('td_chats', 'readwrite');
      const store = tx.objectStore('td_chats');
      for (const chat of chats) {
        store.put(chat);
      }
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  }

  async getChat(chatId: string | number): Promise<TdChat | null> {
    const db = await this.getDb();
    return new Promise((resolve, reject) => {
      const tx = db.transaction('td_chats', 'readonly');
      const store = tx.objectStore('td_chats');
      const req = store.get(chatId);
      req.onsuccess = () => resolve(req.result || null);
      req.onerror = () => reject(req.error);
    });
  }

  async getAllChats(limit: number = 100): Promise<TdChat[]> {
    const db = await this.getDb();
    return new Promise((resolve, reject) => {
      const tx = db.transaction('td_chats', 'readonly');
      const store = tx.objectStore('td_chats');
      const req = store.getAll();
      req.onsuccess = () => {
        const res: TdChat[] = req.result || [];
        resolve(res.slice(0, limit));
      };
      req.onerror = () => reject(req.error);
    });
  }

  // ─── MESSAGE OPERATIONS ─────────────────────────────────────────────────────

  async saveMessage(msg: TdMessage): Promise<void> {
    const db = await this.getDb();
    return new Promise((resolve, reject) => {
      const tx = db.transaction('td_messages', 'readwrite');
      const store = tx.objectStore('td_messages');
      store.put(msg);
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  }

  async saveMessages(messages: TdMessage[]): Promise<void> {
    const db = await this.getDb();
    return new Promise((resolve, reject) => {
      const tx = db.transaction('td_messages', 'readwrite');
      const store = tx.objectStore('td_messages');
      for (const msg of messages) {
        store.put(msg);
      }
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  }

  async getChatMessages(chatId: string | number, limit: number = 50, fromMessageId?: string | number): Promise<TdMessage[]> {
    const db = await this.getDb();
    return new Promise((resolve, reject) => {
      const tx = db.transaction('td_messages', 'readonly');
      const store = tx.objectStore('td_messages');
      const index = store.index('chat_id');
      const req = index.getAll(chatId);
      req.onsuccess = () => {
        let msgs: TdMessage[] = req.result || [];
        // Sort by date ascending
        msgs.sort((a, b) => (a.date || 0) - (b.date || 0));
        if (fromMessageId) {
          const idx = msgs.findIndex(m => String(m.id) === String(fromMessageId));
          if (idx !== -1) {
            msgs = msgs.slice(0, idx);
          }
        }
        resolve(msgs.slice(-limit));
      };
      req.onerror = () => reject(req.error);
    });
  }

  async deleteMessages(messageIds: (string | number)[]): Promise<void> {
    const db = await this.getDb();
    return new Promise((resolve, reject) => {
      const tx = db.transaction('td_messages', 'readwrite');
      const store = tx.objectStore('td_messages');
      for (const id of messageIds) {
        store.delete(id);
      }
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  }

  // ─── USER & DRAFT OPERATIONS ────────────────────────────────────────────────

  async saveUser(user: TdUser): Promise<void> {
    const db = await this.getDb();
    return new Promise((resolve, reject) => {
      const tx = db.transaction('td_users', 'readwrite');
      const store = tx.objectStore('td_users');
      store.put(user);
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  }

  async getUser(userId: string | number): Promise<TdUser | null> {
    const db = await this.getDb();
    return new Promise((resolve, reject) => {
      const tx = db.transaction('td_users', 'readonly');
      const store = tx.objectStore('td_users');
      const req = store.get(userId);
      req.onsuccess = () => resolve(req.result || null);
      req.onerror = () => reject(req.error);
    });
  }

  async saveDraft(chatId: string | number, draft: TdDraftMessage | null): Promise<void> {
    const db = await this.getDb();
    return new Promise((resolve, reject) => {
      const tx = db.transaction('td_drafts', 'readwrite');
      const store = tx.objectStore('td_drafts');
      if (draft) {
        store.put({ chat_id: chatId, ...draft });
      } else {
        store.delete(chatId);
      }
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  }

  async getDraft(chatId: string | number): Promise<TdDraftMessage | null> {
    const db = await this.getDb();
    return new Promise((resolve, reject) => {
      const tx = db.transaction('td_drafts', 'readonly');
      const store = tx.objectStore('td_drafts');
      const req = store.get(chatId);
      req.onsuccess = () => resolve(req.result || null);
      req.onerror = () => reject(req.error);
    });
  }

  // ─── FULL TEXT SEARCH ENGINE ────────────────────────────────────────────────

  async searchMessages(query: string, limit: number = 20): Promise<TdMessage[]> {
    const q = query.trim().toLowerCase();
    if (!q) return [];
    const db = await this.getDb();
    return new Promise((resolve, reject) => {
      const tx = db.transaction('td_messages', 'readonly');
      const store = tx.objectStore('td_messages');
      const req = store.getAll();
      req.onsuccess = () => {
        const msgs: TdMessage[] = req.result || [];
        const matches = msgs.filter(m => {
          if (m.content['@type'] === 'messageText') {
            return m.content.text.text.toLowerCase().includes(q);
          }
          if ('caption' in m.content && m.content.caption?.text) {
            return m.content.caption.text.toLowerCase().includes(q);
          }
          return false;
        });
        resolve(matches.slice(-limit));
      };
      req.onerror = () => reject(req.error);
    });
  }

  // ─── DATABASE STATS ─────────────────────────────────────────────────────────

  async getDatabaseStats(): Promise<{
    chatsCount: number;
    messagesCount: number;
    usersCount: number;
    draftsCount: number;
    storageUsedBytes: number;
  }> {
    const db = await this.getDb();
    const countStore = (storeName: string): Promise<number> => {
      return new Promise((resolve) => {
        try {
          const tx = db.transaction(storeName, 'readonly');
          const store = tx.objectStore(storeName);
          const req = store.count();
          req.onsuccess = () => resolve(req.result || 0);
          req.onerror = () => resolve(0);
        } catch {
          resolve(0);
        }
      });
    };

    const [chatsCount, messagesCount, usersCount, draftsCount] = await Promise.all([
      countStore('td_chats'),
      countStore('td_messages'),
      countStore('td_users'),
      countStore('td_drafts'),
    ]);

    let storageUsedBytes = 0;
    if (navigator.storage && navigator.storage.estimate) {
      try {
        const est = await navigator.storage.estimate();
        storageUsedBytes = est.usage || 0;
      } catch (e) {
        // ignore
      }
    }

    return {
      chatsCount,
      messagesCount,
      usersCount,
      draftsCount,
      storageUsedBytes,
    };
  }
}

export const tdlibDb = new TdlibDatabase();
