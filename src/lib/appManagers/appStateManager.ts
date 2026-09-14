/**
 * Telegram Web K State Manager (appStateManager.ts)
 * Based on morethanwords/tweb src/lib/appManagers/appStateManager.ts
 */

import { stateStorage, TelegramAppState } from '../storages/state';
import { EventEmitter } from './utils';

export class AppStateManager extends EventEmitter {
  private state: TelegramAppState = {
    pts: 1,
    qts: 1,
    date: Math.floor(Date.now() / 1000),
    seq: 0,
    syncState: 'connected',
    lastSyncTimestamp: Date.now(),
  };

  public async init(): Promise<TelegramAppState> {
    this.state = await stateStorage.init();
    return this.state;
  }

  public getState(): TelegramAppState {
    return { ...this.state };
  }

  public async setSyncState(syncState: TelegramAppState['syncState']): Promise<void> {
    this.state.syncState = syncState;
    this.state.lastSyncTimestamp = Date.now();
    await stateStorage.setState({ syncState, lastSyncTimestamp: this.state.lastSyncTimestamp });
    this.emit('sync_state_change', syncState);
  }

  public async setActiveChat(chatId?: string): Promise<void> {
    this.state.activeChatId = chatId;
    await stateStorage.set('activeChatId', chatId);
    this.emit('active_chat_change', chatId);
  }

  public async setCurrentUser(userId: string): Promise<void> {
    this.state.currentUserId = userId;
    await stateStorage.set('currentUserId', userId);
    this.emit('current_user_change', userId);
  }
}

export const appStateManager = new AppStateManager();
export default appStateManager;
