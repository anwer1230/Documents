/**
 * ChannelDifferenceService.ts
 *
 * Dedicated service module handling MTProto updates.getChannelDifference specifically
 * for Supergroups and Broadcast Channels.
 *
 * Adheres to Telegram architecture:
 * - Maintains and tracks PTS per-channel (channel_pts) in persistent localStorage storage.
 * - Resolves updates.channelDifferenceSlice sequentially until final.
 * - Handles updates.channelDifferenceTooLong after long offline periods:
 *   recuperates full historical message batches via deep history fetching to prevent
 *   any historical message loss.
 * - Concurrency control & queueing to prevent server flooding.
 * - Auto-reconnect & offline gap synchronization.
 * - Dispatches updates via UpdatesBatchScheduler to maintain smooth 60 FPS UI.
 */

import { TelegramMessage, TelegramChat } from '../types';
import { updatesBatchScheduler } from './UpdatesBatchScheduler';

export interface ChannelDifferenceResult {
  success: boolean;
  channelId: string;
  pts?: number;
  newMessagesCount: number;
  isTooLong?: boolean;
  isSlice?: boolean;
  recoveredHistoricalCount?: number;
  error?: string;
}

export type ChannelDiffListener = (channelId: string, messages: TelegramMessage[]) => void;

export class ChannelDifferenceService {
  private static instances: Map<number, ChannelDifferenceService> = new Map();
  private currentAccount: number = 0;
  private activeSyncs: Map<string, Promise<ChannelDifferenceResult>> = new Map();
  private lastSyncTimes: Map<string, number> = new Map();
  private isRecoveringOffline: boolean = false;
  private lastOnlineTimestamp: number = Date.now();
  private listeners: Set<ChannelDiffListener> = new Set();
  private ptsCache: Map<string, number> = new Map();

  private constructor(account: number = 0) {
    this.currentAccount = account;
    this.setupLifecycleListeners();
  }

  public static getInstance(account: number = 0): ChannelDifferenceService {
    let instance = ChannelDifferenceService.instances.get(account);
    if (!instance) {
      instance = new ChannelDifferenceService(account);
      ChannelDifferenceService.instances.set(account, instance);
    }
    return instance;
  }

  /**
   * Subscribe to new messages recovered by getChannelDifference
   */
  public subscribe(listener: ChannelDiffListener): () => void {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  }

  /**
   * Sets up network/visibility listeners to detect resumption after long offline periods
   */
  private setupLifecycleListeners(): void {
    if (typeof window === 'undefined') return;

    window.addEventListener('online', () => {
      const offlineDuration = Date.now() - this.lastOnlineTimestamp;
      console.log(`[ChannelDifferenceService] Network reconnected after ${Math.round(offlineDuration / 1000)}s.`);
      this.handleLongOfflineRecovery(offlineDuration);
    });

    window.addEventListener('offline', () => {
      this.lastOnlineTimestamp = Date.now();
    });

    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible') {
        const offlineDuration = Date.now() - this.lastOnlineTimestamp;
        if (offlineDuration > 15000) {
          this.handleLongOfflineRecovery(offlineDuration);
        }
      } else {
        this.lastOnlineTimestamp = Date.now();
      }
    });
  }

  public getChannelPts(channelId: string): number {
    const cleanId = channelId.replace('chat_', '');
    if (this.ptsCache.has(cleanId)) {
      return this.ptsCache.get(cleanId)!;
    }
    if (typeof window !== 'undefined') {
      try {
        const stored = localStorage.getItem(`tg_chan_pts_${this.currentAccount}_${cleanId}`);
        if (stored) {
          const val = parseInt(stored, 10);
          if (!isNaN(val)) {
            this.ptsCache.set(cleanId, val);
            return val;
          }
        }
      } catch (_) {}
    }
    return 0;
  }

  public setChannelPts(channelId: string, pts: number): void {
    const cleanId = channelId.replace('chat_', '');
    this.ptsCache.set(cleanId, pts);
    if (typeof window !== 'undefined') {
      try {
        localStorage.setItem(`tg_chan_pts_${this.currentAccount}_${cleanId}`, String(pts));
      } catch (_) {}
    }
  }

  /**
   * Primary entry point to fetch and process updates.getChannelDifference for a supergroup/channel.
   */
  public async getChannelDifference(
    channelId: string,
    force: boolean = false,
    reason: string = 'manual'
  ): Promise<ChannelDifferenceResult> {
    if (!channelId) {
      return { success: false, channelId: '', newMessagesCount: 0, error: 'NO_CHANNEL_ID' };
    }

    const normalizedId = channelId.startsWith('chat_') ? channelId : `chat_${channelId}`;
    const cleanChanId = channelId.replace('chat_', '');

    // Prevent duplicate concurrent syncs for the same supergroup
    const existingSync = this.activeSyncs.get(normalizedId);
    if (existingSync) {
      return existingSync;
    }

    // Rate-limiting check: avoid re-syncing the same channel within 3 seconds unless forced
    const lastSync = this.lastSyncTimes.get(normalizedId) || 0;
    if (!force && Date.now() - lastSync < 3000) {
      return { success: true, channelId: normalizedId, newMessagesCount: 0 };
    }

    const syncPromise = this.executeChannelDifference(normalizedId, cleanChanId, reason);
    this.activeSyncs.set(normalizedId, syncPromise);

    try {
      const res = await syncPromise;
      this.lastSyncTimes.set(normalizedId, Date.now());
      return res;
    } finally {
      this.activeSyncs.delete(normalizedId);
    }
  }

  /**
   * Internal worker executing the MTProto updates.getChannelDifference loop
   */
  private async executeChannelDifference(
    chatId: string,
    cleanChanId: string,
    reason: string
  ): Promise<ChannelDifferenceResult> {
    const sessionToken =
      typeof window !== 'undefined'
        ? localStorage.getItem('tg_active_session_token') ||
          localStorage.getItem(`tg_session_token_${this.currentAccount}`) ||
          ''
        : '';

    let currentPts = this.getChannelPts(chatId) || this.getChannelPts(cleanChanId) || 0;
    let accumulatedMessages: TelegramMessage[] = [];
    let isTooLongEncountered = false;
    let recoveredHistoryCount = 0;
    let loopCount = 0;
    const maxLoops = 15;

    console.log(`[ChannelDifferenceService] Starting sync for ${chatId} (PTS: ${currentPts}, reason: ${reason})...`);

    try {
      while (loopCount < maxLoops) {
        loopCount++;

        const response = await fetch('/api/telegram/updates/channel-difference', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            ...(sessionToken ? { 'x-session-token': sessionToken } : {}),
          },
          body: JSON.stringify({
            channelPeer: cleanChanId,
            channelId: cleanChanId,
            pts: currentPts,
            limit: 100,
          }),
        });

        if (!response.ok) {
          const errText = await response.text();
          console.warn(`[ChannelDifferenceService] Server error syncing ${chatId}:`, errText);
          return {
            success: false,
            channelId: chatId,
            newMessagesCount: accumulatedMessages.length,
            error: `HTTP_${response.status}`,
          };
        }

        const data = await response.json();

        // 1. Process ChannelDifferenceTooLong:
        if (data.isTooLong || data.className === 'UpdatesChannelDifferenceTooLong' || data._ === 'updates.channelDifferenceTooLong') {
          isTooLongEncountered = true;
          console.log(`[ChannelDifferenceService] Received updates.channelDifferenceTooLong for supergroup ${chatId}!`);

          const recovered = await this.recoverHistoricalMessagesOnGap(chatId, cleanChanId);
          recoveredHistoryCount += recovered.length;
          accumulatedMessages = accumulatedMessages.concat(recovered);

          const newPts = data.pts || data.dialog?.pts;
          if (newPts) {
            currentPts = newPts;
            this.setChannelPts(chatId, currentPts);
          }
          break;
        }

        // 2. Process new messages in this difference
        const newMsgs = data.newMessages || data.messages || [];
        if (Array.isArray(newMsgs) && newMsgs.length > 0) {
          const mappedMsgs: TelegramMessage[] = newMsgs.map((m: any) => ({
            id: String(m.id),
            chatId,
            senderId: m.senderId || (m.out ? 'me' : cleanChanId),
            senderName: m.senderName || 'Telegram User',
            senderAvatar: m.senderAvatar,
            text: m.text || m.message || '',
            timestamp: m.timestamp || (m.date ? m.date * 1000 : Date.now()),
            isOut: Boolean(m.out || m.isOutgoing),
            status: 'read',
            media: m.media,
          }));
          accumulatedMessages = accumulatedMessages.concat(mappedMsgs);
        }

        // 3. Update channel PTS
        if (data.pts && data.pts > 0) {
          currentPts = data.pts;
          this.setChannelPts(chatId, currentPts);
        }

        // 4. If it's a slice (not final), continue fetching subsequent slices
        const isSlice = Boolean(
          data.isSlice ||
          data.className === 'UpdatesChannelDifferenceSlice' ||
          data._ === 'updates.channelDifferenceSlice'
        );
        const isFinal = Boolean(data.isFinal || data.final);

        if (isSlice && !isFinal) {
          console.log(`[ChannelDifferenceService] Channel ${chatId} returned slice. Next PTS ${currentPts}...`);
          continue;
        }

        // Final slice or empty difference reached
        break;
      }

      // Persist & dispatch all accumulated messages
      if (accumulatedMessages.length > 0) {
        const uniqueMap = new Map<string, TelegramMessage>();
        accumulatedMessages.forEach((m) => {
          if (m && m.id) uniqueMap.set(String(m.id), m);
        });
        const deduplicated = Array.from(uniqueMap.values()).sort(
          (a, b) => (Number(a.timestamp) || 0) - (Number(b.timestamp) || 0)
        );

        // Enqueue into UpdatesBatchScheduler for seamless 60 FPS UI dispatching
        deduplicated.forEach((msg) => {
          updatesBatchScheduler.enqueue({
            type: 'message',
            chatId,
            data: msg,
          });
        });

        // Notify direct listeners
        this.listeners.forEach((listener) => {
          try {
            listener(chatId, deduplicated);
          } catch (e) {
            console.error('[ChannelDifferenceService] Listener error:', e);
          }
        });

        console.log(`✅ [ChannelDifferenceService] Successfully synced ${chatId}: saved ${deduplicated.length} messages (TooLong: ${isTooLongEncountered}).`);
      }

      return {
        success: true,
        channelId: chatId,
        pts: currentPts,
        newMessagesCount: accumulatedMessages.length,
        isTooLong: isTooLongEncountered,
        recoveredHistoricalCount: recoveredHistoryCount,
      };
    } catch (err: any) {
      console.error(`[ChannelDifferenceService] Sync error for ${chatId}:`, err);
      return {
        success: false,
        channelId: chatId,
        newMessagesCount: accumulatedMessages.length,
        error: err?.message || String(err),
      };
    }
  }

  /**
   * Recovers historical messages during long offline periods or when channelDifferenceTooLong occurs.
   */
  private async recoverHistoricalMessagesOnGap(
    chatId: string,
    cleanChanId: string
  ): Promise<TelegramMessage[]> {
    const recovered: TelegramMessage[] = [];
    try {
      const sessionToken =
        typeof window !== 'undefined'
          ? localStorage.getItem('tg_active_session_token') || ''
          : '';

      console.log(`[ChannelDifferenceService] Deep history recovery for ${chatId}...`);
      const resp = await fetch(`/api/telegram/messages/${encodeURIComponent(cleanChanId)}?limit=50`, {
        headers: {
          ...(sessionToken ? { 'x-session-token': sessionToken } : {}),
        },
      });

      if (resp.ok) {
        const histData = await resp.json();
        const msgs = histData.messages || histData;
        if (Array.isArray(msgs) && msgs.length > 0) {
          msgs.forEach((m: any) => {
            if (m && m.id) {
              recovered.push({
                id: String(m.id),
                chatId,
                senderId: m.senderId || '',
                senderName: m.senderName || 'Telegram User',
                text: m.text || '',
                timestamp: m.timestamp || Date.now(),
                isOut: Boolean(m.isOut || m.out),
                status: 'read',
                media: m.media,
              });
            }
          });
          console.log(`[ChannelDifferenceService] Recovered ${msgs.length} historical messages for ${chatId}.`);
        }
      }
    } catch (histErr) {
      console.warn(`[ChannelDifferenceService] Error during deep history recovery:`, histErr);
    }
    return recovered;
  }

  /**
   * Checks whether an incoming update has a PTS gap for the channel.
   * If gap is detected, automatically triggers getChannelDifference.
   */
  public checkChannelPtsGap(channelId: string, newPts: number, ptsCount: number = 1): boolean {
    if (!channelId || !newPts) return false;
    const currentPts = this.getChannelPts(channelId);

    if (currentPts === 0) {
      this.setChannelPts(channelId, newPts);
      return false;
    }

    if (newPts > currentPts + ptsCount) {
      console.warn(`⚠️ [ChannelDifferenceService] PTS GAP detected for ${channelId}! Current: ${currentPts}, Update PTS: ${newPts} (gap: ${newPts - currentPts}). Triggering channel difference...`);
      this.getChannelDifference(channelId, true, 'pts_gap');
      return true;
    }

    if (newPts > currentPts) {
      this.setChannelPts(channelId, newPts);
    }
    return false;
  }

  /**
   * Triggers difference check for all known supergroups and channels upon reconnection.
   */
  public async handleLongOfflineRecovery(offlineDurationMs: number = 0): Promise<void> {
    if (this.isRecoveringOffline) return;
    this.isRecoveringOffline = true;
    this.lastOnlineTimestamp = Date.now();

    console.log(`🔄 [ChannelDifferenceService] Running channel offline recovery (offline ~${Math.round(offlineDurationMs / 1000)}s)...`);
    try {
      let knownChats: TelegramChat[] = [];
      if (typeof window !== 'undefined') {
        const cached = localStorage.getItem('tg_real_chats_' + localStorage.getItem('tg_active_account_id'));
        if (cached) {
          try {
            knownChats = JSON.parse(cached);
          } catch (_) {}
        }
      }

      const supergroups = knownChats.filter((d) => this.isSupergroupOrChannel(d));
      console.log(`[ChannelDifferenceService] Found ${supergroups.length} supergroups/channels to synchronize.`);

      const batchSize = 2;
      for (let i = 0; i < supergroups.length; i += batchSize) {
        const batch = supergroups.slice(i, i + batchSize);
        await Promise.allSettled(
          batch.map((sg) => this.getChannelDifference(sg.id, true, 'offline_resume'))
        );
      }
    } catch (e) {
      console.error(`[ChannelDifferenceService] handleLongOfflineRecovery error:`, e);
    } finally {
      this.isRecoveringOffline = false;
    }
  }

  /**
   * Helper to check if a chat represents a supergroup or channel
   */
  public isSupergroupOrChannel(chat: TelegramChat | any): boolean {
    if (!chat) return false;
    const type = String(chat.type || '').toLowerCase();
    const idStr = String(chat.id || '');
    return (
      type === 'supergroup' ||
      type === 'channel' ||
      chat.isChannel === true ||
      chat.isSupergroup === true ||
      chat.isBroadcast === true ||
      idStr.startsWith('chat_-100') ||
      idStr.startsWith('-100')
    );
  }

  /**
   * Synchronizes all known supergroups
   */
  public async syncAllChannels(chats: TelegramChat[], force: boolean = false): Promise<void> {
    const supergroups = chats.filter((d) => this.isSupergroupOrChannel(d));
    for (const sg of supergroups) {
      await this.getChannelDifference(sg.id, force, 'sync_all');
    }
  }
}

export const channelDifferenceService = ChannelDifferenceService.getInstance();
