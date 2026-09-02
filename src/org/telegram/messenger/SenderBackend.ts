/**
 * Telegram Official SenderBackend
 * Implements Smart Sender, Link Resolution, Salam Protection Protocol, and Scheduler
 */

import { TLRPC } from '../tgnet/TLRPC';
import { ConnectionsManager } from '../tgnet/ConnectionsManager';
import { UserConfig } from './UserConfig';
import { NotificationCenter } from './NotificationCenter';
import { MessagesController } from './MessagesController';

export interface SendResult {
  success: boolean;
  messageId?: number;
  error?: string;
  isSalam?: boolean;
}

export class SenderBackend {
  private static instance: SenderBackend;
  private schedulerTimer: any = null;
  private isSchedulerRunning: boolean = false;

  public static getInstance(): SenderBackend {
    if (!SenderBackend.instance) {
      SenderBackend.instance = new SenderBackend();
    }
    return SenderBackend.instance;
  }

  /**
   * Resolves a Telegram link (public username or private invite hash) and sends message
   */
  public async resolveAndSend(
    linkOrPeer: string | number,
    text: string,
    useSalamProtection: boolean = false
  ): Promise<SendResult> {
    const raw = String(linkOrPeer).trim();

    if (raw.includes('t.me/') || raw.startsWith('@')) {
      const clean = raw.replace(/https?:\/\/t\.me\//, '').replace('@', '');

      if (clean.startsWith('+') || clean.includes('joinchat/')) {
        const hash = clean.replace('+', '').replace('joinchat/', '');
        // Private invite check
        const resp = await ConnectionsManager.getInstance().sendRequest('checkChatInvite', { hash });
        if (resp && resp.chat && resp.chat.id) {
          return this.sendMessage(resp.chat.id, text, useSalamProtection);
        }
        return { success: false, error: 'تعذر العثور على المجموعة من خلال رابط الدعوة الخاص' };
      } else {
        // Public username resolution
        const resp = await ConnectionsManager.getInstance().sendRequest('resolveUsername', { username: clean });
        if (resp && resp.peer) {
          const peerId = resp.peer.channel_id || resp.peer.chat_id || resp.peer.user_id;
          return this.sendMessage(peerId, text, useSalamProtection);
        }
        return { success: false, error: 'تعذر حل المعرف للمحادثة' };
      }
    } else {
      return this.sendMessage(raw, text, useSalamProtection);
    }
  }

  /**
   * Core message sender with optional Salam protection protocol
   */
  public async sendMessage(
    chatId: string | number,
    text: string,
    useSalamProtection: boolean = false
  ): Promise<SendResult> {
    if (useSalamProtection) {
      return this.sendSalamThenEdit(chatId, text, 30000);
    }

    try {
      const resp = await ConnectionsManager.getInstance().sendRequest('sendMessage', {
        peer: chatId,
        message: text,
      });

      if (!resp || !resp.success) {
        const err = resp?.error || 'CHAT_WRITE_FORBIDDEN';
        if (err.includes('CHAT_WRITE_FORBIDDEN')) {
          // Trigger Salam bypass automatically
          return this.sendSalamThenEdit(chatId, text, 25000);
        }
        return { success: false, error: err };
      }

      return { success: true, messageId: resp.message?.id || Date.now() };
    } catch (e: any) {
      return { success: false, error: e.message || 'فشل إرسال الرسالة' };
    }
  }

  /**
   * Salam Bypass Protocol: sends "السلام عليكم ورحمة الله وبركاته" then edits to full ad text
   */
  public async sendSalamThenEdit(
    chatId: string | number,
    finalText: string,
    delayMs: number = 30000
  ): Promise<SendResult> {
    try {
      // Step 1: Send Salam
      const salamResp = await ConnectionsManager.getInstance().sendRequest('sendMessage', {
        peer: chatId,
        message: 'السلام عليكم ورحمة الله وبركاته',
      });

      if (!salamResp || !salamResp.success) {
        return { success: false, error: salamResp?.error || 'تعذر إرسال رسالة التحية الأولى' };
      }

      const msgId = salamResp.message?.id || salamResp.id;

      // Step 2: Schedule edit after delay without freezing UI
      if (msgId) {
        setTimeout(async () => {
          try {
            await ConnectionsManager.getInstance().sendRequest('editMessage', {
              peer: chatId,
              id: msgId,
              message: finalText,
            });
            NotificationCenter.getInstance().postNotificationName(NotificationCenter.updateInterfaces);
          } catch (e) {
            console.error('[SalamProtocol] Failed to edit message:', e);
          }
        }, delayMs);
      }

      return { success: true, messageId: msgId, isSalam: true };
    } catch (e: any) {
      return { success: false, error: e.message };
    }
  }

  /**
   * Starts periodic background batch scheduler
   */
  public startScheduler(
    text: string,
    targetChatIds: Array<string | number>,
    intervalMinutes: number = 3,
    onProgress?: (index: number, total: number, result: SendResult) => void
  ): void {
    this.stopScheduler();
    this.isSchedulerRunning = true;

    const runBatch = async () => {
      if (!this.isSchedulerRunning) return;

      for (let i = 0; i < targetChatIds.length; i++) {
        if (!this.isSchedulerRunning) break;
        const cid = targetChatIds[i];
        const res = await this.resolveAndSend(cid, text, false);
        if (onProgress) onProgress(i + 1, targetChatIds.length, res);
        // Safe inter-message delay (2 seconds)
        await new Promise((r) => setTimeout(r, 2000));
      }
    };

    // Run first batch immediately
    runBatch();

    // Schedule next runs
    this.schedulerTimer = setInterval(runBatch, Math.max(1, intervalMinutes) * 60 * 1000);
  }

  public stopScheduler(): void {
    this.isSchedulerRunning = false;
    if (this.schedulerTimer) {
      clearInterval(this.schedulerTimer);
      this.schedulerTimer = null;
    }
  }

  public isRunning(): boolean {
    return this.isSchedulerRunning;
  }
}
