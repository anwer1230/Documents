/**
 * indexedDBStorage.ts - High-Performance Local IndexedDB Storage Engine
 * 
 * Provides native client-side IndexedDB persistence for:
 * - Chats & Dialogs state
 * - Messages & Media metadata cache
 * - Multi-account sessions & MTProto auth keys
 * - Telegram Privacy Keys configuration
 * - WebAuthn / Passkeys credentials & App Lock state
 * - Chat Drafts
 */

import { TelegramChat, TelegramMessage, TelegramAccount } from '../types';

export interface WebAuthnPasskeyRecord {
  credentialId: string;
  rawIdBase64: string;
  name: string;
  deviceName: string;
  algorithm: string;
  createdAt: string;
  lastUsedAt?: string;
  transports?: string[];
  publicKeyBase64?: string;
}

export interface CachedMessageRecord extends TelegramMessage {
  compoundKey: string; // `${chatId}_${id}`
  chatId: string;
}

const DB_NAME = 'TelegramWebDB';
const DB_VERSION = 3;

class TelegramIndexedDB {
  private dbPromise: Promise<IDBDatabase> | null = null;
  private isAvailable: boolean = typeof window !== 'undefined' && 'indexedDB' in window;

  constructor() {
    if (this.isAvailable) {
      this.initDB();
    }
  }

  private initDB(): Promise<IDBDatabase> {
    if (this.dbPromise) return this.dbPromise;

    this.dbPromise = new Promise((resolve, reject) => {
      if (!this.isAvailable) {
        reject(new Error('IndexedDB is not supported in this environment'));
        return;
      }

      const request = indexedDB.open(DB_NAME, DB_VERSION);

      request.onupgradeneeded = (event) => {
        const db = (event.target as IDBOpenDBRequest).result;

        // 1. Chats Store
        if (!db.objectStoreNames.contains('chats')) {
          const chatStore = db.createObjectStore('chats', { keyPath: 'id' });
          chatStore.createIndex('unreadCount', 'unreadCount', { unique: false });
          chatStore.createIndex('pinned', 'pinned', { unique: false });
          chatStore.createIndex('type', 'type', { unique: false });
        }

        // 2. Messages Store (compound key: `${chatId}_${id}`)
        if (!db.objectStoreNames.contains('messages')) {
          const msgStore = db.createObjectStore('messages', { keyPath: 'compoundKey' });
          msgStore.createIndex('chatId', 'chatId', { unique: false });
          msgStore.createIndex('timestamp', 'timestamp', { unique: false });
          msgStore.createIndex('senderId', 'senderId', { unique: false });
        }

        // 3. Multi-account Sessions Store
        if (!db.objectStoreNames.contains('sessions')) {
          db.createObjectStore('sessions', { keyPath: 'id' });
        }

        // 4. Privacy & System Settings Store
        if (!db.objectStoreNames.contains('settings')) {
          db.createObjectStore('settings', { keyPath: 'key' });
        }

        // 5. WebAuthn Passkeys Store
        if (!db.objectStoreNames.contains('passkeys')) {
          db.createObjectStore('passkeys', { keyPath: 'credentialId' });
        }

        // 6. Drafts Store
        if (!db.objectStoreNames.contains('drafts')) {
          db.createObjectStore('drafts', { keyPath: 'chatId' });
        }
      };

      request.onsuccess = () => {
        resolve(request.result);
      };

      request.onerror = (e) => {
        console.warn('[IndexedDB] Failed to open database:', e);
        reject(request.error);
      };
    });

    return this.dbPromise;
  }

  private async getDB(): Promise<IDBDatabase> {
    return this.initDB();
  }

  // ==========================================
  // CHATS API
  // ==========================================

  public async saveChats(chats: TelegramChat[]): Promise<void> {
    if (!this.isAvailable) {
      try {
        localStorage.setItem('tg_cached_chats', JSON.stringify(chats));
      } catch {}
      return;
    }

    try {
      const db = await this.getDB();
      const tx = db.transaction('chats', 'readwrite');
      const store = tx.objectStore('chats');

      for (const chat of chats) {
        if (chat && chat.id) {
          store.put(chat);
        }
      }

      return new Promise((resolve, reject) => {
        tx.oncomplete = () => resolve();
        tx.onerror = () => reject(tx.error);
      });
    } catch (err) {
      console.warn('[IndexedDB] saveChats error:', err);
    }
  }

  public async getChats(): Promise<TelegramChat[]> {
    if (!this.isAvailable) {
      try {
        const saved = localStorage.getItem('tg_cached_chats');
        return saved ? JSON.parse(saved) : [];
      } catch {
        return [];
      }
    }

    try {
      const db = await this.getDB();
      return new Promise((resolve) => {
        const tx = db.transaction('chats', 'readonly');
        const store = tx.objectStore('chats');
        const req = store.getAll();
        req.onsuccess = () => resolve(req.result || []);
        req.onerror = () => resolve([]);
      });
    } catch (err) {
      console.warn('[IndexedDB] getChats error:', err);
      return [];
    }
  }

  public async saveChat(chat: TelegramChat): Promise<void> {
    return this.saveChats([chat]);
  }

  // ==========================================
  // MESSAGES API
  // ==========================================

  public async saveMessages(chatId: string, messages: TelegramMessage[]): Promise<void> {
    if (!this.isAvailable || !chatId) return;

    try {
      const db = await this.getDB();
      const tx = db.transaction('messages', 'readwrite');
      const store = tx.objectStore('messages');

      for (const msg of messages) {
        if (msg && msg.id) {
          const record: CachedMessageRecord = {
            ...msg,
            chatId,
            compoundKey: `${chatId}_${msg.id}`,
          };
          store.put(record);
        }
      }

      return new Promise((resolve, reject) => {
        tx.oncomplete = () => resolve();
        tx.onerror = () => reject(tx.error);
      });
    } catch (err) {
      console.warn('[IndexedDB] saveMessages error:', err);
    }
  }

  public async getMessages(chatId: string, limit: number = 100): Promise<TelegramMessage[]> {
    if (!this.isAvailable || !chatId) return [];

    try {
      const db = await this.getDB();
      return new Promise((resolve) => {
        const tx = db.transaction('messages', 'readonly');
        const store = tx.objectStore('messages');
        const index = store.index('chatId');
        const req = index.getAll(IDBKeyRange.only(chatId));

        req.onsuccess = () => {
          const results: CachedMessageRecord[] = req.result || [];
          // Sort ascending by timestamp / date
          results.sort((a, b) => {
            const timeA = typeof a.timestamp === 'number' ? a.timestamp : new Date((a as any).date || a.timestamp || 0).getTime();
            const timeB = typeof b.timestamp === 'number' ? b.timestamp : new Date((b as any).date || b.timestamp || 0).getTime();
            return timeA - timeB;
          });
          resolve(results.slice(-limit));
        };
        req.onerror = () => resolve([]);
      });
    } catch (err) {
      console.warn('[IndexedDB] getMessages error:', err);
      return [];
    }
  }

  public async clearChatMessages(chatId: string): Promise<void> {
    if (!this.isAvailable || !chatId) return;
    try {
      const db = await this.getDB();
      const tx = db.transaction('messages', 'readwrite');
      const store = tx.objectStore('messages');
      const index = store.index('chatId');
      const req = index.openKeyCursor(IDBKeyRange.only(chatId));

      req.onsuccess = () => {
        const cursor = req.result;
        if (cursor) {
          store.delete(cursor.primaryKey);
          cursor.continue();
        }
      };
    } catch (err) {
      console.warn('[IndexedDB] clearChatMessages error:', err);
    }
  }

  // ==========================================
  // SESSIONS & MULTI-ACCOUNT API
  // ==========================================

  public async saveAccounts(accounts: TelegramAccount[]): Promise<void> {
    if (!this.isAvailable) {
      try {
        localStorage.setItem('tg_multi_accounts', JSON.stringify(accounts));
      } catch {}
      return;
    }

    try {
      const db = await this.getDB();
      const tx = db.transaction('sessions', 'readwrite');
      const store = tx.objectStore('sessions');
      store.clear();
      for (const acc of accounts) {
        if (acc && acc.id) {
          store.put(acc);
        }
      }
    } catch (err) {
      console.warn('[IndexedDB] saveAccounts error:', err);
    }
  }

  public async getAccounts(): Promise<TelegramAccount[]> {
    if (!this.isAvailable) {
      try {
        const saved = localStorage.getItem('tg_multi_accounts');
        return saved ? JSON.parse(saved) : [];
      } catch {
        return [];
      }
    }

    try {
      const db = await this.getDB();
      return new Promise((resolve) => {
        const tx = db.transaction('sessions', 'readonly');
        const store = tx.objectStore('sessions');
        const req = store.getAll();
        req.onsuccess = () => resolve(req.result || []);
        req.onerror = () => resolve([]);
      });
    } catch (err) {
      console.warn('[IndexedDB] getAccounts error:', err);
      return [];
    }
  }

  // ==========================================
  // PRIVACY & SYSTEM SETTINGS API
  // ==========================================

  public async setSetting<T = any>(key: string, value: T): Promise<void> {
    try {
      localStorage.setItem(`tg_setting_${key}`, JSON.stringify(value));
    } catch {}

    if (!this.isAvailable) return;

    try {
      const db = await this.getDB();
      const tx = db.transaction('settings', 'readwrite');
      tx.objectStore('settings').put({ key, value, updatedAt: new Date().toISOString() });
    } catch (err) {
      console.warn('[IndexedDB] setSetting error:', err);
    }
  }

  public async getSetting<T = any>(key: string, defaultValue?: T): Promise<T> {
    if (!this.isAvailable) {
      try {
        const local = localStorage.getItem(`tg_setting_${key}`);
        return local !== null ? JSON.parse(local) : (defaultValue as T);
      } catch {
        return defaultValue as T;
      }
    }

    try {
      const db = await this.getDB();
      return new Promise((resolve) => {
        const tx = db.transaction('settings', 'readonly');
        const req = tx.objectStore('settings').get(key);
        req.onsuccess = () => {
          if (req.result && req.result.value !== undefined) {
            resolve(req.result.value);
          } else {
            // fallback to local storage
            try {
              const local = localStorage.getItem(`tg_setting_${key}`);
              resolve(local !== null ? JSON.parse(local) : (defaultValue as T));
            } catch {
              resolve(defaultValue as T);
            }
          }
        };
        req.onerror = () => resolve(defaultValue as T);
      });
    } catch {
      return defaultValue as T;
    }
  }

  public async getAllSettings(): Promise<Record<string, any>> {
    if (!this.isAvailable) return {};

    try {
      const db = await this.getDB();
      return new Promise((resolve) => {
        const tx = db.transaction('settings', 'readonly');
        const req = tx.objectStore('settings').getAll();
        req.onsuccess = () => {
          const map: Record<string, any> = {};
          (req.result || []).forEach((item: any) => {
            if (item && item.key) {
              map[item.key] = item.value;
            }
          });
          resolve(map);
        };
        req.onerror = () => resolve({});
      });
    } catch {
      return {};
    }
  }

  // ==========================================
  // WEBAUTHN / PASSKEYS API
  // ==========================================

  public async savePasskey(passkey: WebAuthnPasskeyRecord): Promise<void> {
    if (!this.isAvailable) {
      try {
        const list = await this.getPasskeys();
        const updated = [...list.filter((p) => p.credentialId !== passkey.credentialId), passkey];
        localStorage.setItem('tg_webauthn_passkeys', JSON.stringify(updated));
      } catch {}
      return;
    }

    try {
      const db = await this.getDB();
      const tx = db.transaction('passkeys', 'readwrite');
      tx.objectStore('passkeys').put(passkey);
    } catch (err) {
      console.warn('[IndexedDB] savePasskey error:', err);
    }
  }

  public async getPasskeys(): Promise<WebAuthnPasskeyRecord[]> {
    if (!this.isAvailable) {
      try {
        const saved = localStorage.getItem('tg_webauthn_passkeys');
        return saved ? JSON.parse(saved) : [];
      } catch {
        return [];
      }
    }

    try {
      const db = await this.getDB();
      return new Promise((resolve) => {
        const tx = db.transaction('passkeys', 'readonly');
        const req = tx.objectStore('passkeys').getAll();
        req.onsuccess = () => resolve(req.result || []);
        req.onerror = () => resolve([]);
      });
    } catch (err) {
      console.warn('[IndexedDB] getPasskeys error:', err);
      return [];
    }
  }

  public async deletePasskey(credentialId: string): Promise<void> {
    try {
      const list = await this.getPasskeys();
      const filtered = list.filter((p) => p.credentialId !== credentialId);
      localStorage.setItem('tg_webauthn_passkeys', JSON.stringify(filtered));
    } catch {}

    if (!this.isAvailable) return;

    try {
      const db = await this.getDB();
      const tx = db.transaction('passkeys', 'readwrite');
      tx.objectStore('passkeys').delete(credentialId);
    } catch (err) {
      console.warn('[IndexedDB] deletePasskey error:', err);
    }
  }

  // ==========================================
  // CHAT DRAFTS API
  // ==========================================

  public async saveDraft(chatId: string, text: string, replyToMsgId?: string): Promise<void> {
    if (!chatId) return;
    if (!this.isAvailable) {
      try {
        localStorage.setItem(`tg_draft_${chatId}`, JSON.stringify({ text, replyToMsgId }));
      } catch {}
      return;
    }

    try {
      const db = await this.getDB();
      const tx = db.transaction('drafts', 'readwrite');
      if (!text || text.trim() === '') {
        tx.objectStore('drafts').delete(chatId);
      } else {
        tx.objectStore('drafts').put({
          chatId,
          text,
          replyToMsgId,
          updatedAt: new Date().toISOString(),
        });
      }
    } catch (err) {
      console.warn('[IndexedDB] saveDraft error:', err);
    }
  }

  public async getDraft(chatId: string): Promise<{ text: string; replyToMsgId?: string } | null> {
    if (!chatId) return null;
    if (!this.isAvailable) {
      try {
        const item = localStorage.getItem(`tg_draft_${chatId}`);
        return item ? JSON.parse(item) : null;
      } catch {
        return null;
      }
    }

    try {
      const db = await this.getDB();
      return new Promise((resolve) => {
        const tx = db.transaction('drafts', 'readonly');
        const req = tx.objectStore('drafts').get(chatId);
        req.onsuccess = () => resolve(req.result || null);
        req.onerror = () => resolve(null);
      });
    } catch {
      return null;
    }
  }

  // ==========================================
  // PURGE ALL DATA (Logout / Clear Cache)
  // ==========================================

  public async purgeAll(): Promise<void> {
    try {
      if (this.isAvailable) {
        const db = await this.getDB();
        const storeNames = Array.from(db.objectStoreNames);
        const tx = db.transaction(storeNames, 'readwrite');
        for (const name of storeNames) {
          tx.objectStore(name).clear();
        }
      }
    } catch (err) {
      console.warn('[IndexedDB] purgeAll error:', err);
    }
  }
}

export const indexedDBStorage = new TelegramIndexedDB();
