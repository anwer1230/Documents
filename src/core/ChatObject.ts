/**
 * ChatObject.ts - org.telegram.messenger.ChatObject
 * Helper for chat and channel properties and restrictions
 */

import { Chat } from '../types';

export class ChatObject {
  public static isChannel(chat?: Chat | null): boolean {
    return chat?.type === 'channel';
  }

  public static isGroup(chat?: Chat | null): boolean {
    return chat?.type === 'group';
  }

  public static getRestrictedNotice(
    chat?: Chat | null,
    isArabic: boolean = false
  ): { restricted: boolean; message: string } {
    return { restricted: false, message: '' };
  }

  public static canSendMessages(chat?: Chat | null): boolean {
    return true;
  }

  public static canSendMedia(chat?: Chat | null): boolean {
    return true;
  }
}
