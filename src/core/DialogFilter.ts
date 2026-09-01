/**
 * DialogFilter.ts - Telegram Chat Folder / Dialog Filter Architecture
 * 
 * Replicated directly from DrKLO/Telegram Android:
 * org.telegram.messenger.support.customtabs.CustomTabsClient / 
 * org.telegram.messenger.MessagesController.DialogFilter
 * org.telegram.ui.DialogsActivity.java
 * 
 * Manages chat folder filters (All, Personal, Work, Channels, Groups, Bots, Custom Filters),
 * peer flags, included/excluded peers, and syncs with Telegram MTProto flags.
 */

import { Chat, ChatFolder } from '../types';

export class DialogFilter {
  public id: number = 0;
  public name: string = '';
  public nameAr?: string;
  public icon?: string;
  public emoticon?: string;
  public color: number = 0;
  public flags: number = 0;
  public pinnedDialogs: Set<string> = new Set();
  public alwaysShow: Set<string> = new Set();
  public neverShow: Set<string> = new Set();

  // Telegram Filter Bitmask Flags (from TLRPC.TL_dialogFilter)
  public static readonly FLAG_CONTACTS = 0x0001;
  public static readonly FLAG_NON_CONTACTS = 0x0002;
  public static readonly FLAG_GROUPS = 0x0004;
  public static readonly FLAG_CHANNELS = 0x0008;
  public static readonly FLAG_BOTS = 0x0010;
  public static readonly FLAG_EXCLUDE_MUTED = 0x0020;
  public static readonly FLAG_EXCLUDE_READ = 0x0040;
  public static readonly FLAG_EXCLUDE_ARCHIVED = 0x0080;

  constructor(data?: Partial<ChatFolder> | { id: number; name: string; flags?: number }) {
    if (data) {
      this.id = typeof data.id === 'string' ? parseInt(data.id, 10) || 0 : (data.id || 0);
      this.name = (data as any).name || (data as any).title || 'Folder';
      this.nameAr = (data as any).nameAr;
      this.icon = (data as any).icon;
      this.emoticon = (data as any).emoticon;
      this.color = (data as any).color || 0;
      this.flags = (data as any).filterFlags || (data as any).flags || 0;

      if ((data as any).pinnedChatIds) {
        (data as any).pinnedChatIds.forEach((id: any) => this.pinnedDialogs.add(String(id)));
      }
      if ((data as any).includedChatIds) {
        (data as any).includedChatIds.forEach((id: any) => this.alwaysShow.add(String(id)));
      }
      if ((data as any).excludedChatIds) {
        (data as any).excludedChatIds.forEach((id: any) => this.neverShow.add(String(id)));
      }
    }
  }

  /**
   * Tests if a chat belongs to this folder according to DrKLO Telegram matching rules
   */
  public includesChat(chat: Chat, isMuted: boolean = false, isArchived: boolean = false): boolean {
    const chatId = String(chat.id);

    // 1. Explicitly excluded
    if (this.neverShow.has(chatId)) {
      return false;
    }

    // 2. Archived filter flag
    if ((this.flags & DialogFilter.FLAG_EXCLUDE_ARCHIVED) && (isArchived || chat.isArchived)) {
      return false;
    }

    // 3. Muted filter flag
    if ((this.flags & DialogFilter.FLAG_EXCLUDE_MUTED) && (isMuted || chat.isMuted)) {
      return false;
    }

    // 4. Read filter flag
    if ((this.flags & DialogFilter.FLAG_EXCLUDE_READ) && !chat.unreadCount) {
      return false;
    }

    // 5. Explicitly included
    if (this.alwaysShow.has(chatId) || this.pinnedDialogs.has(chatId)) {
      return true;
    }

    // 6. Category Filter Flags
    if (this.flags === 0) {
      return true; // "All" or fallback
    }

    if ((this.flags & DialogFilter.FLAG_GROUPS) && ((chat.type as string) === 'group' || (chat.type as string) === 'supergroup')) {
      return true;
    }

    if ((this.flags & DialogFilter.FLAG_CHANNELS) && chat.type === 'channel') {
      return true;
    }

    if ((this.flags & DialogFilter.FLAG_BOTS) && chat.type === 'bot') {
      return true;
    }

    if ((this.flags & DialogFilter.FLAG_CONTACTS) && (chat.type === 'private' || (chat.type as any) === 'direct' || chat.type === 'saved')) {
      return true;
    }

    if ((this.flags & DialogFilter.FLAG_NON_CONTACTS) && (chat.type === 'private' || (chat.type as any) === 'direct') && !chat.isContact) {
      return true;
    }

    return false;
  }

  /**
   * Converts to UI Folder model
   */
  public toChatFolder(): ChatFolder {
    return {
      id: this.id,
      title: this.name,
      icon: this.icon,
      emoticon: this.emoticon,
      color: this.color,
      pinnedChatIds: Array.from(this.pinnedDialogs),
      includedChatIds: Array.from(this.alwaysShow),
      excludedChatIds: Array.from(this.neverShow),
      filterFlags: this.flags,
      includeGroups: !!(this.flags & DialogFilter.FLAG_GROUPS),
      includeChannels: !!(this.flags & DialogFilter.FLAG_CHANNELS),
      includeBots: !!(this.flags & DialogFilter.FLAG_BOTS),
      includeContacts: !!(this.flags & DialogFilter.FLAG_CONTACTS),
      includeNonContacts: !!(this.flags & DialogFilter.FLAG_NON_CONTACTS),
      excludeMuted: !!(this.flags & DialogFilter.FLAG_EXCLUDE_MUTED),
      excludeRead: !!(this.flags & DialogFilter.FLAG_EXCLUDE_READ),
      excludeArchived: !!(this.flags & DialogFilter.FLAG_EXCLUDE_ARCHIVED),
    };
  }
}
