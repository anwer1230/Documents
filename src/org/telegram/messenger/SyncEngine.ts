/**
 * Telegram Official Sync Engine (UpdatesHandler & Sequence Manager)
 * Replicates org.telegram.messenger.UpdatesHandler from DrKLO/Telegram
 * Handles PTS, QTS, Seq, Date difference synchronization and real-time cross-device updates.
 */

import { AccountInstance } from './AccountInstance';
import { NotificationCenter } from './NotificationCenter';
import { UserConfig } from './UserConfig';
import { ConnectionsManager } from '../tgnet/ConnectionsManager';

export interface SequenceState {
  pts: number;
  qts: number;
  seq: number;
  date: number;
  unreadCount: number;
}

export class SyncEngine {
  private static instances: SyncEngine[] = [];
  public readonly currentAccount: number;
  private sequenceState: SequenceState = {
    pts: 100,
    qts: 0,
    seq: 1,
    date: Math.floor(Date.now() / 1000),
    unreadCount: 0,
  };
  private isSyncing: boolean = false;
  private eventSource: EventSource | null = null;

  private constructor(account: number) {
    this.currentAccount = account;
    this.loadState();
    this.initRealtimeStream();
  }

  public static getInstance(account: number = UserConfig.selectedAccount): SyncEngine {
    if (account < 0 || account >= UserConfig.MAX_ACCOUNT_COUNT) {
      account = 0;
    }
    if (!SyncEngine.instances[account]) {
      SyncEngine.instances[account] = new SyncEngine(account);
    }
    return SyncEngine.instances[account];
  }

  private loadState(): void {
    try {
      const raw = localStorage.getItem(`tg_sync_state_${this.currentAccount}`);
      if (raw) {
        this.sequenceState = JSON.parse(raw);
      }
    } catch {}
  }

  public saveState(): void {
    try {
      localStorage.setItem(`tg_sync_state_${this.currentAccount}`, JSON.stringify(this.sequenceState));
    } catch {}
  }

  public getSequenceState(): SequenceState {
    return { ...this.sequenceState };
  }

  /**
   * Fetches updates difference from Telegram MTProto backend (updates.getDifference)
   */
  public async getDifference(): Promise<void> {
    if (this.isSyncing) return;
    this.isSyncing = true;

    try {
      const res = await ConnectionsManager.getInstance(this.currentAccount).sendRequest('updates.getDifference', {
        pts: this.sequenceState.pts,
        pts_total_limit: 100,
        date: this.sequenceState.date,
        qts: this.sequenceState.qts,
      });

      if (res && res.success) {
        if (res.state) {
          this.sequenceState.pts = res.state.pts || this.sequenceState.pts;
          this.sequenceState.qts = res.state.qts || this.sequenceState.qts;
          this.sequenceState.seq = res.state.seq || this.sequenceState.seq;
          this.sequenceState.date = res.state.date || this.sequenceState.date;
          this.saveState();
        }

        // Process new messages
        if (Array.isArray(res.new_messages)) {
          for (const msg of res.new_messages) {
            await AccountInstance.getInstance(this.currentAccount).getMessagesController().processUpdate({ message: msg });
          }
        }

        // Process other updates
        if (Array.isArray(res.other_updates)) {
          for (const update of res.other_updates) {
            await AccountInstance.getInstance(this.currentAccount).getMessagesController().processUpdate(update);
          }
        }

        NotificationCenter.getInstance(this.currentAccount).postNotificationName(NotificationCenter.updatesDidReceived);
      }
    } catch (e) {
      console.warn(`[SyncEngine_${this.currentAccount}] getDifference error:`, e);
    } finally {
      this.isSyncing = false;
    }
  }

  /**
   * Ingest incoming live MTProto update
   */
  public async processIncomingUpdate(update: any): Promise<void> {
    if (!update) return;

    if (update.pts) {
      if (update.pts > this.sequenceState.pts + 1) {
        // Gap detected -> Trigger difference fetch
        this.getDifference();
        return;
      }
      this.sequenceState.pts = update.pts;
    }

    if (update.seq) {
      this.sequenceState.seq = update.seq;
    }

    this.saveState();
    await AccountInstance.getInstance(this.currentAccount).getMessagesController().processUpdate(update);
  }

  /**
   * Realtime SSE / WebSocket connection to server
   */
  private initRealtimeStream(): void {
    if (typeof window === 'undefined') return;

    try {
      this.eventSource = new EventSource('/api/events');

      this.eventSource.onmessage = (event) => {
        try {
          const parsed = JSON.parse(event.data);
          const targetAccount = parsed.data?.account_id ?? parsed.data?.account ?? 0;
          
          if (targetAccount === this.currentAccount || targetAccount === -1) {
            if (parsed.type === 'new_message' || parsed.type === 'update_message') {
              this.processIncomingUpdate(parsed.data);
            } else if (parsed.type === 'dialogs_reload' || parsed.type === 'updateChats') {
              NotificationCenter.getInstance(this.currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload);
            }
          }
        } catch (err) {
          console.warn('[SyncEngine] Error parsing SSE payload:', err);
        }
      };

      this.eventSource.onerror = () => {
        // Automatically handled by browser reconnect
      };
    } catch (e) {
      console.warn('[SyncEngine] SSE initialization error:', e);
    }
  }

  public destroy(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }
}
