/**
 * Official Telegram ChatObject Implementation (MTProto 2.0 & Android Architecture)
 * Handles Channels, Megagroups, Basic Groups, Admin Rights, and Banned/Restricted Rights.
 */

export interface ChatRights {
  view_messages?: boolean;
  send_messages?: boolean;
  send_media?: boolean;
  send_stickers?: boolean;
  send_gifs?: boolean;
  send_games?: boolean;
  send_inline?: boolean;
  embed_links?: boolean;
  send_polls?: boolean;
  change_info?: boolean;
  invite_users?: boolean;
  pin_messages?: boolean;
  manage_topics?: boolean;
  send_photos?: boolean;
  send_videos?: boolean;
  send_roundvideos?: boolean;
  send_audios?: boolean;
  send_voices?: boolean;
  send_docs?: boolean;
  send_plain?: boolean;
  post_messages?: boolean;
  edit_messages?: boolean;
  delete_messages?: boolean;
  ban_users?: boolean;
  add_admins?: boolean;
  anonymous?: boolean;
  manage_call?: boolean;
  other?: boolean;
}

export interface TelegramChatEntity {
  id: string | number;
  title?: string;
  type?: 'private' | 'group' | 'supergroup' | 'channel' | 'bot' | 'saved';
  broadcast?: boolean;
  megagroup?: boolean;
  gigagroup?: boolean;
  creator?: boolean;
  admin_rights?: ChatRights;
  default_banned_rights?: ChatRights;
  banned_rights?: ChatRights;
  left?: boolean;
  kicked?: boolean;
  deactivated?: boolean;
  call_active?: boolean;
  call_not_empty?: boolean;
  noforwards?: boolean;
  participants_count?: number;
  restricted?: boolean;
  restriction_reason?: string;
  isChannel?: boolean;
  isGroup?: boolean;
  isAdmin?: boolean;
  isCreator?: boolean;
  hasBannedRights?: boolean;
}

export class ChatObject {
  /**
   * Check if chat is a broadcast channel (NOT a megagroup / supergroup)
   */
  public static isChannelAndNotMegaGroup(chat?: TelegramChatEntity | null): boolean {
    if (!chat) return false;
    // In Telegram MTProto: channels have flags. If megagroup is true or type is 'group'/'supergroup', it is NOT a broadcast channel
    if (chat.megagroup === true || chat.type === 'group' || chat.type === 'supergroup' || chat.isGroup === true) {
      return false;
    }
    return Boolean(chat.broadcast || chat.type === 'channel' || (chat.isChannel && !chat.megagroup && !chat.isGroup));
  }

  /**
   * Check if chat is any channel-type (broadcast or supergroup)
   */
  public static isChannel(chat?: TelegramChatEntity | null): boolean {
    if (!chat) return false;
    return Boolean(
      chat.isChannel ||
      chat.type === 'channel' ||
      chat.type === 'supergroup' ||
      chat.broadcast ||
      chat.megagroup
    );
  }

  /**
   * Check if chat is a megagroup / supergroup
   */
  public static isMegagroup(chat?: TelegramChatEntity | null): boolean {
    if (!chat) return false;
    return Boolean(
      chat.megagroup ||
      chat.type === 'supergroup' ||
      (chat.type === 'group' && chat.isChannel)
    );
  }

  /**
   * Check if chat is a basic group or megagroup
   */
  public static isGroup(chat?: TelegramChatEntity | null): boolean {
    if (!chat) return false;
    return Boolean(
      chat.type === 'group' ||
      chat.type === 'supergroup' ||
      chat.megagroup ||
      chat.isGroup ||
      (!ChatObject.isChannelAndNotMegaGroup(chat) && chat.type !== 'private' && chat.type !== 'saved' && chat.type !== 'bot')
    );
  }

  /**
   * Check if the current user can post in this chat/channel
   */
  public static canPost(chat?: TelegramChatEntity | null): boolean {
    if (!chat) return true;
    if (chat.creator || chat.isCreator || chat.isAdmin) return true;
    if (chat.admin_rights?.post_messages || chat.admin_rights?.send_messages) return true;
    
    // Broadcast channels only allow admins/creators to post
    if (ChatObject.isChannelAndNotMegaGroup(chat)) {
      return false;
    }

    return ChatObject.canSendMessages(chat);
  }

  /**
   * Check if the current user can send normal messages
   */
  public static canSendMessages(chat?: TelegramChatEntity | null): boolean {
    if (!chat) return true;
    
    // Private chats, Saved Messages, Direct Bot chats
    if (chat.type === 'private' || chat.type === 'saved' || chat.type === 'bot') {
      return true;
    }

    // Owner or Admin with full rights
    if (chat.creator || chat.isCreator || chat.isAdmin) {
      return true;
    }

    // Broadcast channels (where megagroup is false)
    if (ChatObject.isChannelAndNotMegaGroup(chat)) {
      return Boolean(chat.admin_rights?.post_messages || chat.creator);
    }

    // Groups & Supergroups: evaluate banned_rights and default_banned_rights
    const userBanned = chat.banned_rights;
    const defaultBanned = chat.default_banned_rights;

    // Check specific user penalty first
    if (userBanned) {
      if (userBanned.send_messages === true || (userBanned.view_messages === true)) {
        return false;
      }
      if (userBanned.send_plain === true) {
        return false;
      }
    }

    // Check default chat permissions
    if (defaultBanned) {
      if (defaultBanned.send_messages === true) {
        return false;
      }
      if (defaultBanned.send_plain === true) {
        return false;
      }
    }

    return true;
  }

  /**
   * Check if user can send media (photos, videos, documents, voice)
   */
  public static canSendMedia(chat?: TelegramChatEntity | null): boolean {
    if (!chat) return true;
    if (chat.creator || chat.isCreator || chat.isAdmin) return true;
    if (ChatObject.isChannelAndNotMegaGroup(chat)) {
      return Boolean(chat.admin_rights?.post_messages || chat.admin_rights?.send_media);
    }

    if (chat.banned_rights?.send_media === true) return false;
    if (chat.default_banned_rights?.send_media === true) return false;

    return ChatObject.canSendMessages(chat);
  }

  /**
   * Check if user can send stickers / GIFs
   */
  public static canSendStickers(chat?: TelegramChatEntity | null): boolean {
    if (!chat) return true;
    if (chat.creator || chat.isCreator || chat.isAdmin) return true;
    if (ChatObject.isChannelAndNotMegaGroup(chat)) return false;

    if (chat.banned_rights?.send_stickers === true || chat.banned_rights?.send_gifs === true) return false;
    if (chat.default_banned_rights?.send_stickers === true || chat.default_banned_rights?.send_gifs === true) return false;

    return ChatObject.canSendMessages(chat);
  }

  /**
   * Check if user can embed links
   */
  public static canSendEmbed(chat?: TelegramChatEntity | null): boolean {
    if (!chat) return true;
    if (chat.creator || chat.isCreator || chat.isAdmin) return true;
    if (ChatObject.isChannelAndNotMegaGroup(chat)) return false;

    if (chat.banned_rights?.embed_links === true) return false;
    if (chat.default_banned_rights?.embed_links === true) return false;

    return ChatObject.canSendMessages(chat);
  }

  /**
   * Returns precise user restriction notice message for UI input banner
   */
  public static getRestrictedNotice(
    chat?: TelegramChatEntity | null,
    isArabic: boolean = true
  ): { restricted: boolean; reason?: 'channel' | 'banned' | 'kicked' | 'deactivated'; message?: string } {
    if (!chat) return { restricted: false };

    if (chat.kicked || chat.left) {
      return {
        restricted: true,
        reason: 'kicked',
        message: isArabic ? 'أنت لست عضواً في هذه المجموعة.' : 'You are not a member of this group.',
      };
    }

    if (chat.deactivated) {
      return {
        restricted: true,
        reason: 'deactivated',
        message: isArabic ? 'هذه المجموعة تم تعطيلها.' : 'This group has been deactivated.',
      };
    }

    // Broadcast channels ONLY
    if (ChatObject.isChannelAndNotMegaGroup(chat)) {
      const canAdminPost = Boolean(chat.creator || chat.isCreator || chat.isAdmin || chat.admin_rights?.post_messages);
      if (!canAdminPost) {
        return {
          restricted: true,
          reason: 'channel',
          message: isArabic ? 'القنوات مخصصة لبث الرسائل فقط.' : 'Channels are for broadcasting messages only.',
        };
      }
      return { restricted: false };
    }

    // Groups & Supergroups
    if (chat.creator || chat.isCreator || chat.isAdmin) {
      return { restricted: false };
    }

    const userBanned = chat.banned_rights;
    const defaultBanned = chat.default_banned_rights;

    if (userBanned?.send_messages === true || userBanned?.send_plain === true) {
      return {
        restricted: true,
        reason: 'banned',
        message: isArabic
          ? 'المشرفون قيدوا قدرتك على إرسال الرسائل في هذه المجموعة.'
          : 'Administrators have restricted your ability to send messages in this group.',
      };
    }

    if (defaultBanned?.send_messages === true || defaultBanned?.send_plain === true) {
      return {
        restricted: true,
        reason: 'banned',
        message: isArabic
          ? 'إرسال الرسائل مقيد لجميع الأعضاء في هذه المجموعة.'
          : 'Sending messages is restricted for all members in this group.',
      };
    }

    return { restricted: false };
  }
}
