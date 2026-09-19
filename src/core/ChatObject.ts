/**
 * ChatObject.ts - org.telegram.messenger.ChatObject
 * Helper for chat and channel properties and restrictions
 */

import { Chat } from '../types';

export class ChatObject {
  /**
   * Matches org.telegram.messenger.ChatObject.isChannel:
   * In Telegram, a chat is a broadcast channel ONLY if it is a channel AND NOT a megagroup!
   */
  public static isChannel(chat?: Chat | null): boolean {
    if (!chat) return false;
    // Megagroups (supergroups) are groups, NOT broadcast channels
    if (chat.isMegagroup) return false;
    if (chat.type === 'group' || chat.type === 'saved' || chat.type === 'private' || chat.type === 'bot') {
      return false;
    }
    // If explicitly marked as not broadcast, it's a group
    if (chat.isBroadcast === false) return false;

    // Guard against misclassified group titles (e.g. from invite links)
    const title = (chat.title || '').toLowerCase();
    const hasGroupKeyword = title.includes('مجموعة') || title.includes('group') || title.includes('نقاش') || title.includes('قروب') || title.includes('مجتمع');
    const hasChannelKeyword = title.includes('قناة') || title.includes('channel') || title.includes('news');
    if (hasGroupKeyword && !hasChannelKeyword) {
      return false;
    }

    return chat.type === 'channel';
  }

  public static isMegagroup(chat?: Chat | null): boolean {
    if (!chat) return false;
    return Boolean(chat.isMegagroup || chat.type === 'group');
  }

  public static isGroup(chat?: Chat | null): boolean {
    if (!chat) return false;
    if (chat.type === 'group' || chat.isMegagroup) return true;
    return !this.isChannel(chat) && (chat.type as string) !== 'private' && (chat.type as string) !== 'saved' && (chat.type as string) !== 'bot';
  }

  public static getRestrictedNotice(
    chat?: Chat | null,
    isArabic: boolean = false
  ): { restricted: boolean; message: string } {
    if (!chat) return { restricted: false, message: '' };
    if (chat.isRestricted && chat.restrictionReason) {
      return { restricted: true, message: chat.restrictionReason };
    }
    return { restricted: false, message: '' };
  }

  public static canSendMessages(chat?: Chat | null): boolean {
    if (!chat) return false;
    if (this.isChannel(chat)) {
      return Boolean(chat.isCreator || chat.adminRights?.post_messages);
    }
    if (chat.defaultBannedRights?.send_messages) return false;
    if (chat.bannedRights?.send_messages) return false;
    if (chat.isReadOnly) return false;
    return true;
  }

  public static canSendMedia(chat?: Chat | null): boolean {
    if (!chat) return false;
    if (!this.canSendMessages(chat)) return false;
    if (chat.defaultBannedRights?.send_media) return false;
    if (chat.bannedRights?.send_media) return false;
    return true;
  }
}
