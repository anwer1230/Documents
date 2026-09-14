/**
 * Telegram Web K Instant Messaging Manager (appImManager.ts)
 * Based on morethanwords/tweb src/lib/appManagers/appImManager.ts
 */

import { apiManager } from './apiManager';
import { appMessagesManager } from './appMessagesManager';
import { idb } from '../storages/idb';
import { EventEmitter } from './utils';
import { TelegramMessage } from '../../types';

export class AppImManager extends EventEmitter {
  private typingTimeouts: Map<string, any> = new Map();

  public async sendMessage(
    chatId: string,
    text: string,
    replyToId?: string,
    media?: any
  ): Promise<TelegramMessage> {
    const tempId = `temp_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;
    const pendingMessage: TelegramMessage = {
      id: tempId,
      chatId,
      senderId: 'me',
      senderName: 'أنا',
      text,
      timestamp: Date.now(),
      isOut: true,
      status: 'sending',
      media,
      replyTo: replyToId ? { id: replyToId, senderName: '', text: '' } : undefined,
    };

    // 1. Optimistic local append
    await appMessagesManager.saveMessage(pendingMessage);
    await this.deleteDraft(chatId);

    // 2. Transmit via API
    try {
      const res = await apiManager.invoke('messages.sendMessage', {
        peer: chatId,
        message: text,
        reply_to_msg_id: replyToId,
        media,
      });

      if (res && res.id) {
        pendingMessage.id = String(res.id);
        pendingMessage.status = 'sent';
        await appMessagesManager.saveMessage(pendingMessage);
      }
      return pendingMessage;
    } catch (err) {
      pendingMessage.status = 'sending';
      await appMessagesManager.saveMessage(pendingMessage);
      throw err;
    }
  }

  public async sendTyping(chatId: string, action: 'typing' | 'record-audio' | 'upload-photo' = 'typing'): Promise<void> {
    const key = `${chatId}_${action}`;
    if (this.typingTimeouts.has(key)) return;

    this.typingTimeouts.set(key, true);
    setTimeout(() => this.typingTimeouts.delete(key), 4000);

    try {
      await apiManager.invoke('messages.setTyping', {
        peer: chatId,
        action,
      }, { timeout: 5000 });
    } catch {}
  }

  public async saveDraft(chatId: string, text: string): Promise<void> {
    if (!text || !text.trim()) {
      await this.deleteDraft(chatId);
      return;
    }
    await idb.put('drafts', { chatId, text, updatedAt: Date.now() });
    this.emit('draft_saved', { chatId, text });
  }

  public async getDraft(chatId: string): Promise<string | null> {
    const record = await idb.get('drafts', chatId);
    return record?.text || null;
  }

  public async deleteDraft(chatId: string): Promise<void> {
    await idb.delete('drafts', chatId);
    this.emit('draft_deleted', { chatId });
  }
}

export const appImManager = new AppImManager();
export default appImManager;
