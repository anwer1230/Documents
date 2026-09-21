import Dexie, { Table } from 'dexie';
import { MyMessagesBatch, LiveDiscoveredLink, SenderBatch, PlusConfig, AppSettings } from '../types';

export interface SyncedPlusSettingsRecord {
  id: string; // 'global' or accountId
  config: PlusConfig;
  updatedAt: number;
}

export interface SyncedAccountSettingsRecord {
  accountId: string;
  settings: AppSettings;
  updatedAt: number;
}

export class TelegramDexieDatabase extends Dexie {
  myMessageBatches!: Table<MyMessagesBatch, string>;
  discoveredLinks!: Table<LiveDiscoveredLink, string>;
  senderBatches!: Table<SenderBatch, string>;
  plusSettings!: Table<SyncedPlusSettingsRecord, string>;
  accountSettings!: Table<SyncedAccountSettingsRecord, string>;

  constructor() {
    super('TelegramLocalDatabase');

    // Define tables and indexed keys
    this.version(2).stores({
      myMessageBatches: 'id, date, timestamp, groupsCount',
      discoveredLinks: 'id, url, sourceChatId, status, timestamp, autoJoined',
      senderBatches: 'id, createdAt, status, isScheduled',
      plusSettings: 'id, updatedAt',
      accountSettings: 'accountId, updatedAt',
    });
  }
}

export const telegramDb = new TelegramDexieDatabase();

// Ensure database tables are opened and ready
export async function initTelegramDexieDb(): Promise<void> {
  try {
    await telegramDb.open();
  } catch (err) {
    console.warn('[Dexie DB] IndexedDB initialization notice:', err);
  }
}
