/**
 * Telegram Web K Updates Manager (appUpdatesManager.ts)
 * Based on morethanwords/tweb src/lib/appManagers/appUpdatesManager.ts
 * The brain of MTProto synchronization: PTS sequencing, gap detection, and getDifference catchup.
 */

import { stateStorage } from '../storages/state';
import { EventEmitter } from './utils';
import { appMessagesManager } from './appMessagesManager';
import { appChatsManager } from './appChatsManager';
import { appUsersManager } from './appUsersManager';
import { apiManager } from './apiManager';

export interface TelegramUpdate {
  _: string;
  pts?: number;
  pts_count?: number;
  date?: number;
  message?: any;
  messages?: any[];
  id?: any;
  user_id?: string;
  status?: any;
  max_id?: string;
  chat?: any;
  [key: string]: any;
}

export class AppUpdatesManager extends EventEmitter {
  private currentPts = 1;
  private currentQts = 1;
  private currentDate = Math.floor(Date.now() / 1000);
  private currentSeq = 0;
  private isCatchingUp = false;
  private pendingUpdatesQueue: TelegramUpdate[] = [];

  public async init(): Promise<void> {
    const st = await stateStorage.init();
    this.currentPts = st.pts || 1;
    this.currentQts = st.qts || 1;
    this.currentDate = st.date || Math.floor(Date.now() / 1000);
    this.currentSeq = st.seq || 0;
  }

  public getPts(): number {
    return this.currentPts;
  }

  public setPts(pts: number): void {
    this.currentPts = pts;
    stateStorage.set('pts', pts).catch(() => {});
  }

  /**
   * Process incoming batch or single update
   */
  public async handleUpdates(updateData: any): Promise<void> {
    if (!updateData) return;

    if (updateData._ === 'updatesTooLong') {
      await this.handleUpdatesTooLong();
      return;
    }

    if (Array.isArray(updateData.updates)) {
      for (const upd of updateData.updates) {
        await this.processSingleUpdate(upd);
      }
      return;
    }

    if (Array.isArray(updateData)) {
      for (const upd of updateData) {
        await this.processSingleUpdate(upd);
      }
      return;
    }

    await this.processSingleUpdate(updateData);
  }

  private async processSingleUpdate(update: TelegramUpdate): Promise<void> {
    const updatePts = update.pts;

    // PTS verification check
    if (updatePts !== undefined && updatePts > 0) {
      if (updatePts <= this.currentPts) {
        // Stale or duplicate update; ignore safely
        return;
      }

      if (updatePts > this.currentPts + (update.pts_count || 1)) {
        // Gap detected! Queue and trigger catch-up
        this.pendingUpdatesQueue.push(update);
        if (!this.isCatchingUp) {
          await this.getDifference();
        }
        return;
      }

      // Exact in-order sequence
      this.currentPts = updatePts;
      await stateStorage.set('pts', this.currentPts);
    }

    await this.routeUpdate(update);
  }

  private async routeUpdate(update: TelegramUpdate): Promise<void> {
    const type = update._ || update.type || '';

    switch (type) {
      case 'updateNewMessage':
      case 'updateNewChannelMessage':
        if (update.message) {
          await appMessagesManager.handleUpdateNewMessage(update.message);
        }
        break;

      case 'updateEditMessage':
      case 'updateEditChannelMessage':
        if (update.message) {
          await appMessagesManager.handleUpdateEditMessage(update.message);
        }
        break;

      case 'updateDeleteMessages':
      case 'updateDeleteChannelMessages':
        if (Array.isArray(update.messages || update.id)) {
          await appMessagesManager.handleUpdateDeleteMessages(
            (update.messages || update.id).map(String),
            update.channel_id ? `-100${update.channel_id}` : undefined
          );
        }
        break;

      case 'updateReadHistoryInbox':
      case 'updateReadChannelInbox':
        appMessagesManager.handleReadHistoryInbox(
          String(update.peer_id || update.channel_id || ''),
          String(update.max_id || '')
        );
        break;

      case 'updateReadHistoryOutbox':
      case 'updateReadChannelOutbox':
        appMessagesManager.handleReadHistoryOutbox(
          String(update.peer_id || update.channel_id || ''),
          String(update.max_id || '')
        );
        break;

      case 'updateNewChat':
      case 'updateChannel':
        if (update.chat) {
          appChatsManager.saveChat(update.chat);
        }
        break;

      case 'updateUserStatus':
        if (update.user_id) {
          const status = update.status?._ === 'userStatusOnline' ? 'online' : 'offline';
          const lastSeen = update.status?.was_online ? update.status.was_online * 1000 : undefined;
          appUsersManager.handleUserStatusUpdate(String(update.user_id), status, lastSeen);
        }
        break;

      case 'updatePts':
        if (update.pts) {
          this.currentPts = update.pts;
          await stateStorage.set('pts', update.pts);
        }
        break;

      default:
        break;
    }

    this.emit('update', update);
  }

  /**
   * Catch up missing updates via updates.getDifference
   */
  public async getDifference(): Promise<void> {
    if (this.isCatchingUp) return;
    this.isCatchingUp = true;
    this.emit('sync_state', 'syncing');

    try {
      const diff = await apiManager.invoke('updates.getDifference', {
        pts: this.currentPts,
        date: this.currentDate,
        qts: this.currentQts,
      });

      if (diff) {
        if (Array.isArray(diff.users)) {
          diff.users.forEach((u: any) => appUsersManager.saveUser(u));
        }
        if (Array.isArray(diff.chats)) {
          diff.chats.forEach((c: any) => appChatsManager.saveChat(c));
        }
        if (Array.isArray(diff.new_messages)) {
          for (const m of diff.new_messages) {
            await appMessagesManager.handleUpdateNewMessage(m);
          }
        }
        if (Array.isArray(diff.other_updates)) {
          for (const upd of diff.other_updates) {
            await this.routeUpdate(upd);
          }
        }

        if (diff.state?.pts) {
          this.currentPts = diff.state.pts;
          this.currentDate = diff.state.date || this.currentDate;
          this.currentQts = diff.state.qts || this.currentQts;
          await stateStorage.setState({
            pts: this.currentPts,
            qts: this.currentQts,
            date: this.currentDate,
          });
        }
      }

      // Re-apply queued updates
      const queued = [...this.pendingUpdatesQueue];
      this.pendingUpdatesQueue = [];
      for (const item of queued) {
        await this.processSingleUpdate(item);
      }

      this.emit('sync_state', 'synced');
    } catch (err) {
      console.warn('[appUpdatesManager] getDifference failed, retrying later:', err);
      this.emit('sync_state', 'connected');
    } finally {
      this.isCatchingUp = false;
    }
  }

  public async handleUpdatesTooLong(): Promise<void> {
    try {
      const state = await apiManager.invoke('updates.getState');
      if (state && state.pts) {
        this.currentPts = state.pts;
        this.currentQts = state.qts || this.currentQts;
        this.currentDate = state.date || this.currentDate;
        this.currentSeq = state.seq || this.currentSeq;
        await stateStorage.setState({
          pts: this.currentPts,
          qts: this.currentQts,
          date: this.currentDate,
          seq: this.currentSeq,
        });
      }
      await this.getDifference();
    } catch (err) {
      console.error('[appUpdatesManager] handleUpdatesTooLong failed:', err);
    }
  }
}

export const appUpdatesManager = new AppUpdatesManager();
export default appUpdatesManager;
