/**
 * Telegram Web K State Storage Layer (state.ts)
 * Based on morethanwords/tweb src/lib/storages/state.ts
 */

import { idb } from './idb';

export interface TelegramAppState {
  pts: number;
  qts: number;
  date: number;
  seq: number;
  currentUserId?: string;
  activeChatId?: string;
  activeFolderId?: number;
  syncState?: 'connecting' | 'connected' | 'syncing' | 'synced' | 'broken';
  lastSyncTimestamp?: number;
}

const DEFAULT_STATE: TelegramAppState = {
  pts: 1,
  qts: 1,
  date: Math.floor(Date.now() / 1000),
  seq: 0,
  syncState: 'connected',
  lastSyncTimestamp: Date.now(),
};

export class AppStateStorage {
  private inMemoryState: TelegramAppState = { ...DEFAULT_STATE };
  private initialized = false;

  public async init(): Promise<TelegramAppState> {
    if (this.initialized) return this.inMemoryState;
    try {
      const record = await idb.get('state', 'app_state');
      if (record && record.value) {
        this.inMemoryState = { ...DEFAULT_STATE, ...record.value };
      }
    } catch (err) {
      console.warn('[AppStateStorage] init fallback to defaults:', err);
    }
    this.initialized = true;
    return this.inMemoryState;
  }

  public getState(): TelegramAppState {
    return { ...this.inMemoryState };
  }

  public async setState(updates: Partial<TelegramAppState>): Promise<TelegramAppState> {
    this.inMemoryState = { ...this.inMemoryState, ...updates };
    try {
      await idb.put('state', { key: 'app_state', value: this.inMemoryState, updatedAt: Date.now() });
    } catch (err) {
      console.warn('[AppStateStorage] persistence error:', err);
    }
    return { ...this.inMemoryState };
  }

  public get<K extends keyof TelegramAppState>(key: K): TelegramAppState[K] {
    return this.inMemoryState[key];
  }

  public async set<K extends keyof TelegramAppState>(key: K, value: TelegramAppState[K]): Promise<void> {
    this.inMemoryState[key] = value;
    await this.setState({ [key]: value });
  }
}

export const stateStorage = new AppStateStorage();
export default stateStorage;
