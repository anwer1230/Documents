/**
 * Telegram Web K Chats Manager (appChatsManager.ts)
 * Based on morethanwords/tweb src/lib/appManagers/appChatsManager.ts
 */

import { idb } from '../storages/idb';
import { appCache } from '../storages/cache';
import { EventEmitter } from './utils';
import { appPeersManager } from './appPeersManager';
import { TelegramChat } from '../../types';

export class AppChatsManager extends EventEmitter {
  private chats: Map<string, TelegramChat> = new Map();

  public async getChat(chatId: string): Promise<TelegramChat | null> {
    if (this.chats.has(chatId)) return this.chats.get(chatId)!;

    const cached = appCache.get<TelegramChat>(`chat_${chatId}`);
    if (cached) {
      this.chats.set(chatId, cached);
      return cached;
    }

    try {
      const record = await idb.get('chats', chatId);
      if (record) {
        const c = record.data || record;
        this.chats.set(chatId, c);
        appCache.set(`chat_${chatId}`, c, 30000);
        return c;
      }
    } catch {}

    return null;
  }

  public async getAllChats(): Promise<TelegramChat[]> {
    try {
      const records = await idb.getAll('chats');
      const list = records.map((r) => r.data || r);
      list.forEach((c) => this.chats.set(c.id, c));
      return this.sortChats(list);
    } catch {
      return Array.from(this.chats.values());
    }
  }

  public saveChat(chat: TelegramChat): void {
    this.chats.set(chat.id, chat);
    appCache.set(`chat_${chat.id}`, chat, 30000);

    idb.put('chats', {
      id: chat.id,
      title: chat.title,
      date: chat.lastMessage?.timestamp || Date.now(),
      data: chat,
    }).catch(() => {});

    appPeersManager.savePeer({
      id: chat.id,
      type: chat.type === 'channel' ? 'channel' : (chat.type === 'group' || chat.type === 'supergroup') ? 'chat' : 'user',
      title: chat.title,
      photoUrl: chat.avatarUrl,
      isVerified: chat.isVerified,
    });

    this.emit('chat_update', chat);
  }

  public updateLastMessage(chatId: string, message: any): void {
    const chat = this.chats.get(chatId);
    if (chat) {
      chat.lastMessage = {
        text: message.text || (message.media ? `[${message.media.type}]` : ''),
        senderName: message.senderName || 'أنا',
        timestamp: message.timestamp || Date.now(),
        isOut: message.isOut,
        
      };
      if (!message.isOut) {
        chat.unreadCount = (chat.unreadCount || 0) + 1;
      }
      this.saveChat(chat);
      this.emit('chat_last_message', { chatId, lastMessage: chat.lastMessage });
    }
  }

  public markAsRead(chatId: string): void {
    const chat = this.chats.get(chatId);
    if (chat && chat.unreadCount > 0) {
      chat.unreadCount = 0;
      this.saveChat(chat);
      this.emit('chat_read', { chatId });
    }
  }

  public sortChats(chats: TelegramChat[]): TelegramChat[] {
    return [...chats].sort((a, b) => {
      if (a.isPinned && !b.isPinned) return -1;
      if (!a.isPinned && b.isPinned) return 1;
      const timeA = a.lastMessage?.timestamp || 0;
      const timeB = b.lastMessage?.timestamp || 0;
      return timeB - timeA;
    });
  }
}

export const appChatsManager = new AppChatsManager();
export default appChatsManager;
