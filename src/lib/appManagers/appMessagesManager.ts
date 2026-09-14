/**
 * Telegram Web K Messages Manager (appMessagesManager.ts)
 * Based on morethanwords/tweb src/lib/appManagers/appMessagesManager.ts
 * Core engine for syncing, caching, formatting, and dispatching messages.
 */

import { idb } from '../storages/idb';
import { appCache } from '../storages/cache';
import { EventEmitter } from './utils';
import { appChatsManager } from './appChatsManager';
import { TelegramMessage } from '../../types';

export class AppMessagesManager extends EventEmitter {
  private messagesByPeer: Map<string, TelegramMessage[]> = new Map();

  public async getHistory(peerId: string, limit = 50, offsetDate = 0): Promise<TelegramMessage[]> {
    if (this.messagesByPeer.has(peerId)) {
      const mem = this.messagesByPeer.get(peerId)!;
      if (mem.length >= limit && offsetDate === 0) {
        return mem.slice(-limit);
      }
    }

    try {
      const records = await idb.getByIndex('messages', 'peerId', peerId);
      const list: TelegramMessage[] = records.map((r) => r.data || r);
      list.sort((a, b) => (a.timestamp || 0) - (b.timestamp || 0));

      this.messagesByPeer.set(peerId, list);
      appCache.set(`history_${peerId}`, list, 30000);
      return list.slice(-limit);
    } catch (err) {
      console.warn('[appMessagesManager] getHistory fallback to memory:', err);
      return (this.messagesByPeer.get(peerId) || []).slice(-limit);
    }
  }

  public async saveMessage(message: TelegramMessage): Promise<void> {
    const peerId = message.chatId;
    let list = this.messagesByPeer.get(peerId) || [];

    // Replace if exists, or append
    const idx = list.findIndex((m) => m.id === message.id);
    if (idx >= 0) {
      list[idx] = message;
    } else {
      list.push(message);
    }

    list.sort((a, b) => (a.timestamp || 0) - (b.timestamp || 0));
    this.messagesByPeer.set(peerId, list);

    // Save in IndexedDB
    try {
      await idb.put('messages', {
        id: message.id,
        peerId,
        date: message.timestamp || Date.now(),
        data: message,
      });
    } catch (err) {
      console.warn('[appMessagesManager] saveMessage IDB error:', err);
    }

    // Update parent chat's last message
    appChatsManager.updateLastMessage(peerId, message);

    this.emit('message', message);
  }

  public async handleUpdateNewMessage(rawMsg: any): Promise<TelegramMessage> {
    const message: TelegramMessage = {
      id: String(rawMsg.id || Date.now()),
      chatId: String(rawMsg.chatId || rawMsg.peer_id || rawMsg.to_id || ''),
      senderId: String(rawMsg.senderId || rawMsg.from_id || rawMsg.sender?.id || 'unknown'),
      senderName: rawMsg.senderName || rawMsg.sender?.firstName || 'مستخدم',
      senderAvatar: rawMsg.senderAvatar || rawMsg.sender?.photoUrl,
      text: rawMsg.message || rawMsg.text || '',
      timestamp: rawMsg.date ? rawMsg.date * 1000 : Date.now(),
      isOut: Boolean(rawMsg.out || rawMsg.isOut || rawMsg.isOutgoing),
      status: (rawMsg.out || rawMsg.isOut) ? 'sent' : 'read',
      media: rawMsg.media,
      replyTo: rawMsg.replyTo,
      replyMarkup: rawMsg.replyMarkup,
      reactions: rawMsg.reactions || [],
    };

    await this.saveMessage(message);
    return message;
  }

  public async handleUpdateEditMessage(rawMsg: any): Promise<void> {
    const id = String(rawMsg.id);
    const peerId = String(rawMsg.chatId || rawMsg.peer_id || '');
    const list = this.messagesByPeer.get(peerId) || [];
    const target = list.find((m) => m.id === id);

    if (target) {
      target.text = rawMsg.message || rawMsg.text || target.text;
      target.isEdited = true;
      target.reactions = rawMsg.reactions || target.reactions;
      await this.saveMessage(target);
      this.emit('message_edit', target);
    }
  }

  public async handleUpdateDeleteMessages(messageIds: string[], peerId?: string): Promise<void> {
    for (const id of messageIds) {
      try {
        await idb.delete('messages', id);
      } catch {}
    }

    if (peerId && this.messagesByPeer.has(peerId)) {
      const filtered = this.messagesByPeer.get(peerId)!.filter((m) => !messageIds.includes(m.id));
      this.messagesByPeer.set(peerId, filtered);
    }

    this.emit('messages_delete', { messageIds, peerId });
  }

  public handleReadHistoryInbox(peerId: string, maxId: string): void {
    const list = this.messagesByPeer.get(peerId) || [];
    list.forEach((m) => {
      if (!m.isOut && m.id <= maxId) {
        m.status = 'read';
      }
    });
    appChatsManager.markAsRead(peerId);
    this.emit('history_read_inbox', { peerId, maxId });
  }

  public handleReadHistoryOutbox(peerId: string, maxId: string): void {
    const list = this.messagesByPeer.get(peerId) || [];
    list.forEach((m) => {
      if (m.isOut && m.id <= maxId) {
        m.status = 'read';
      }
    });
    this.emit('history_read_outbox', { peerId, maxId });
  }
}

export const appMessagesManager = new AppMessagesManager();
export default appMessagesManager;
