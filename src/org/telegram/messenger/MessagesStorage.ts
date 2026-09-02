/**
 * Telegram Official MessagesStorage Engine (SQLite Virtual Layer & Fast Cache)
 * Replicates org.telegram.messenger.MessagesStorage.java from DrKLO/Telegram
 */

import { NotificationCenter } from './NotificationCenter';
import { UserConfig } from './UserConfig';
import { telegramDB } from '../../../utils/sqliteStorage';

export class SQLiteDatabase {
  private inMemoryTables: Map<string, Map<string | number, any>> = new Map();
  private readonly storagePrefix: string;

  constructor(prefix: string = 'account_0') {
    this.storagePrefix = prefix;
    this.initTables();
    this.restoreFromStorage();
  }

  private initTables(): void {
    this.inMemoryTables.set('dialogs', new Map());
    this.inMemoryTables.set('messages', new Map());
    this.inMemoryTables.set('users', new Map());
    this.inMemoryTables.set('chats', new Map());
    this.inMemoryTables.set('params', new Map());
  }

  private restoreFromStorage(): void {
    try {
      const raw = localStorage.getItem(`tg_db_${this.storagePrefix}`);
      if (raw) {
        const parsed = JSON.parse(raw);
        for (const [tableName, entries] of Object.entries(parsed)) {
          const map = new Map();
          if (Array.isArray(entries)) {
            for (const [key, val] of entries) {
              map.set(key, val);
            }
          }
          this.inMemoryTables.set(tableName, map);
        }
      }
    } catch {}
  }

  public persistToStorage(): void {
    try {
      const exportObj: Record<string, any[]> = {};
      for (const [tableName, map] of this.inMemoryTables.entries()) {
        exportObj[tableName] = Array.from(map.entries());
      }
      localStorage.setItem(`tg_db_${this.storagePrefix}`, JSON.stringify(exportObj));
    } catch {}
  }

  public async execute(sql: string, params: any[] = []): Promise<void> {
    this.persistToStorage();
    return new Promise((resolve) => setTimeout(resolve, 0));
  }

  public async queryFinalized(table: string, queryFn?: (item: any) => boolean): Promise<any[]> {
    const targetTable = this.inMemoryTables.get(table);
    if (!targetTable) return [];
    const items = Array.from(targetTable.values());
    if (queryFn) {
      return items.filter(queryFn);
    }
    return items;
  }

  public put(table: string, id: string | number, value: any): void {
    let targetTable = this.inMemoryTables.get(table);
    if (!targetTable) {
      targetTable = new Map();
      this.inMemoryTables.set(table, targetTable);
    }
    targetTable.set(id, value);
    this.persistToStorage();
  }

  public get(table: string, id: string | number): any {
    return this.inMemoryTables.get(table)?.get(id);
  }

  public remove(table: string, id: string | number): boolean {
    const res = this.inMemoryTables.get(table)?.delete(id) || false;
    this.persistToStorage();
    return res;
  }

  public clear(): void {
    this.initTables();
    try {
      localStorage.removeItem(`tg_db_${this.storagePrefix}`);
    } catch {}
  }
}

export class MessagesStorage {
  private static instances: MessagesStorage[] = [];
  public database: SQLiteDatabase;
  private readonly currentAccount: number;

  private constructor(account: number) {
    this.currentAccount = account;
    this.database = new SQLiteDatabase(`account_${account}`);
    telegramDB.init().catch(console.warn);
  }

  public static getInstance(account: number = UserConfig.selectedAccount): MessagesStorage {
    if (account < 0 || account >= UserConfig.MAX_ACCOUNT_COUNT) {
      account = 0;
    }
    if (!MessagesStorage.instances[account]) {
      MessagesStorage.instances[account] = new MessagesStorage(account);
    }
    return MessagesStorage.instances[account];
  }

  public async setDialogFlags(dialogId: string | number, flags: { pinned?: boolean; pinnedNum?: number; muted?: boolean; unread?: number }): Promise<void> {
    const dialog = this.database.get('dialogs', dialogId) || { id: dialogId };
    const updated = { ...dialog, ...flags };
    this.database.put('dialogs', dialogId, updated);
    if (flags.pinned !== undefined) {
      telegramDB.setDialogPinned(String(dialogId), flags.pinned);
    }
    await this.database.execute('UPDATE dialogs SET flags = ? WHERE id = ?', [JSON.stringify(flags), dialogId]);
    NotificationCenter.getInstance(this.currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload);
  }

  public async deleteDialog(dialogId: string | number, folderId: number = 0): Promise<void> {
    this.database.remove('dialogs', dialogId);
    telegramDB.deleteChat(String(dialogId));
    await this.database.execute('DELETE FROM dialogs WHERE id = ?', [dialogId]);
    await this.database.execute('DELETE FROM messages WHERE dialog_id = ?', [dialogId]);
    NotificationCenter.getInstance(this.currentAccount).postNotificationName(NotificationCenter.dialogDeleted, dialogId);
    NotificationCenter.getInstance(this.currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload);
  }

  public async markMessagesAsDeleted(dialogId: string | number, messageIds: Array<number | string>): Promise<void> {
    for (const mid of messageIds) {
      this.database.remove('messages', `${dialogId}_${mid}`);
    }
    telegramDB.deleteMessages(messageIds);
    await this.database.execute('DELETE FROM messages WHERE id IN (?)', [messageIds.join(',')]);
    NotificationCenter.getInstance(this.currentAccount).postNotificationName(NotificationCenter.messagesDeleted, messageIds, dialogId);
  }

  public async putMessages(messages: any[], isChannel: boolean = false): Promise<void> {
    for (const msg of messages) {
      const key = `${msg.chat_id || msg.dialog_id || msg.chatId}_${msg.id}`;
      this.database.put('messages', key, msg);
    }
    telegramDB.saveMessages(messages);
    NotificationCenter.getInstance(this.currentAccount).postNotificationName(NotificationCenter.didReceivedNewMessages, messages);
  }

  public async updateDialogsWithReadFlags(dialogId: string | number, maxId: number): Promise<void> {
    const dialog = this.database.get('dialogs', dialogId);
    if (dialog) {
      dialog.read_inbox_max_id = maxId;
      dialog.unread_count = 0;
      this.database.put('dialogs', dialogId, dialog);
    }
    NotificationCenter.getInstance(this.currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload);
  }

  public clearAll(): void {
    this.database.clear();
    telegramDB.cleanUp();
  }
}

