/**
 * Official Telegram ChatObject Implementation (MTProto 2.0 & Android Architecture)
 * Handles Channels, Megagroups, Basic Groups, Admin Rights, and Banned/Restricted Rights.
 * Replicates org.telegram.messenger.ChatObject logic from DrKLO/Telegram Android source.
 */

import { TLRPC } from './TLRPC';

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
  until_date?: number;
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
  isReadOnly?: boolean;
  adminOnly?: boolean;
  // Raw TLRPC properties support
  _?: string;
  flags?: number;
}

export class ChatObject {
  /**
   * Check if chat is a broadcast channel (NOT a megagroup / supergroup)
   * Replicates ChatObject.isChannelAndNotMegaGroup(TLRPC.Chat) from Telegram Android
   */
  public static isChannelAndNotMegaGroup(chat?: TelegramChatEntity | TLRPC.Chat | null): boolean {
    if (!chat) return false;
    const c = chat as any;

    // In Telegram MTProto / Android:
    // If megagroup is true or explicitly marked as group/supergroup, it is NOT a broadcast channel
    if (c.megagroup === true || c.type === 'group' || c.type === 'supergroup' || c.isGroup === true) {
      return false;
    }

    // Check broadcast flag or channel type without megagroup
    if (c.broadcast === true) {
      return true;
    }

    if (c.type === 'channel') {
      return !c.megagroup;
    }

    if (c.isChannel && !c.megagroup && !c.isGroup) {
      return true;
    }

    return false;
  }

  /**
   * Check if chat is any channel-type (broadcast or supergroup)
   */
  public static isChannel(chat?: TelegramChatEntity | TLRPC.Chat | null): boolean {
    if (!chat) return false;
    const c = chat as any;
    return Boolean(
      c.isChannel ||
      c.type === 'channel' ||
      c.type === 'supergroup' ||
      c.broadcast ||
      c.megagroup ||
      c._ === 'channel' ||
      c._ === 'channelForbidden'
    );
  }

  /**
   * Check if chat is a megagroup / supergroup
   */
  public static isMegagroup(chat?: TelegramChatEntity | TLRPC.Chat | null): boolean {
    if (!chat) return false;
    const c = chat as any;
    return Boolean(
      c.megagroup ||
      c.type === 'supergroup' ||
      (c.type === 'group' && (c.isChannel || c.broadcast === false))
    );
  }

  /**
   * Check if chat is a basic group or megagroup
   */
  public static isGroup(chat?: TelegramChatEntity | TLRPC.Chat | null): boolean {
    if (!chat) return false;
    const c = chat as any;
    return Boolean(
      c.type === 'group' ||
      c.type === 'supergroup' ||
      c.megagroup ||
      c.isGroup ||
      (!ChatObject.isChannelAndNotMegaGroup(c) && c.type !== 'private' && c.type !== 'saved' && c.type !== 'bot')
    );
  }

  /**
   * Check if user is creator / owner of the chat
   */
  public static isCreator(chat?: TelegramChatEntity | TLRPC.Chat | null): boolean {
    if (!chat) return false;
    const c = chat as any;
    return Boolean(c.creator === true || c.isCreator === true || c.admin_rights?.add_admins);
  }

  /**
   * Check if user has administrator privileges
   */
  public static isAdmin(chat?: TelegramChatEntity | TLRPC.Chat | null): boolean {
    if (!chat) return false;
    const c = chat as any;
    if (ChatObject.isCreator(c)) return true;
    if (c.isAdmin === true) return true;
    if (c.admin_rights && (
      c.admin_rights.post_messages ||
      c.admin_rights.edit_messages ||
      c.admin_rights.delete_messages ||
      c.admin_rights.ban_users ||
      c.admin_rights.change_info ||
      c.admin_rights.invite_users ||
      c.admin_rights.pin_messages
    )) {
      return true;
    }
    return false;
  }

  /**
   * Check if the current user can post in this chat/channel
   */
  public static canPost(chat?: TelegramChatEntity | TLRPC.Chat | null): boolean {
    if (!chat) return true;
    const c = chat as any;
    if (ChatObject.isCreator(c) || ChatObject.isAdmin(c)) return true;
    if (c.admin_rights?.post_messages || c.admin_rights?.send_messages) return true;
    
    // Broadcast channels only allow admins/creators to post
    if (ChatObject.isChannelAndNotMegaGroup(c)) {
      return false;
    }

    return ChatObject.canSendMessages(c);
  }

  /**
   * Check if the authenticated user can send normal messages.
   * Replicates ChatObject.canSendMessages(TLRPC.Chat) & ChatObject.hasAdminRights(TLRPC.Chat) from Telegram Android.
   *
   * Accurately parses:
   *  1. Private / Saved Messages / Bot chats (always permitted)
   *  2. Creator / Admin permissions (bypass standard and default restrictions)
   *  3. Broadcast channels vs Megagroups (broadcast requires admin_rights.post_messages)
   *  4. Specific user banned_rights vs Chat default_banned_rights vs Chat level restriction flags
   */
  public static canSendMessages(chat?: TelegramChatEntity | TLRPC.Chat | null): boolean {
    if (!chat) return true;
    const c = chat as any;
    
    // 1. Private chats, Saved Messages, Direct Bot chats
    if (c.type === 'private' || c.type === 'saved' || c.type === 'bot') {
      return true;
    }

    // 2. Left / Kicked / Deactivated chats
    if (c.left === true || c.kicked === true || c.deactivated === true) {
      return false;
    }

    // 3. Creator or Admin with elevated permissions
    if (ChatObject.isCreator(c) || ChatObject.isAdmin(c)) {
      return true;
    }

    // 4. Broadcast channels (where megagroup is FALSE)
    // Megagroups (supergroups) are NOT broadcast channels and must NOT be blocked by this rule!
    if (ChatObject.isChannelAndNotMegaGroup(c)) {
      return Boolean(c.admin_rights?.post_messages || c.creator || c.isCreator);
    }

    // 5. Check user-specific penalties (banned_rights from TLRPC.ChatParticipant / TLRPC.ChannelParticipantBanned)
    const userBanned = c.banned_rights as ChatRights | undefined;
    if (userBanned) {
      // Check if until_date expired
      const untilDate = userBanned.until_date || 0;
      const isExpired = untilDate > 0 && untilDate * 1000 < Date.now();
      if (!isExpired) {
        if (userBanned.send_messages === true || userBanned.view_messages === true || userBanned.send_plain === true) {
          return false;
        }
      }
    }

    // 6. Check chat default restrictions (default_banned_rights)
    const defaultBanned = c.default_banned_rights as ChatRights | undefined;
    if (defaultBanned) {
      if (defaultBanned.send_messages === true || defaultBanned.send_plain === true) {
        return false;
      }
    }

    // 7. General chat-level restricted or read-only flags (checked after megagroup/admin verification)
    if (c.isReadOnly === true && !ChatObject.isMegagroup(c)) {
      return false;
    }

    if (c.adminOnly === true) {
      return false;
    }

    return true;
  }

  /**
   * Check if user can send media (photos, videos, documents, voice)
   */
  public static canSendMedia(chat?: TelegramChatEntity | TLRPC.Chat | null): boolean {
    if (!chat) return true;
    const c = chat as any;
    if (ChatObject.isCreator(c) || ChatObject.isAdmin(c)) return true;
    if (ChatObject.isChannelAndNotMegaGroup(c)) {
      return Boolean(c.admin_rights?.post_messages || c.admin_rights?.send_media);
    }

    if (c.banned_rights?.send_media === true) return false;
    if (c.default_banned_rights?.send_media === true) return false;

    return ChatObject.canSendMessages(c);
  }

  /**
   * Check if user can send stickers / GIFs
   */
  public static canSendStickers(chat?: TelegramChatEntity | TLRPC.Chat | null): boolean {
    if (!chat) return true;
    const c = chat as any;
    if (ChatObject.isCreator(c) || ChatObject.isAdmin(c)) return true;
    if (ChatObject.isChannelAndNotMegaGroup(c)) return false;

    if (c.banned_rights?.send_stickers === true || c.banned_rights?.send_gifs === true) return false;
    if (c.default_banned_rights?.send_stickers === true || c.default_banned_rights?.send_gifs === true) return false;

    return ChatObject.canSendMessages(c);
  }

  /**
   * Check if user can embed links
   */
  public static canSendEmbed(chat?: TelegramChatEntity | TLRPC.Chat | null): boolean {
    if (!chat) return true;
    const c = chat as any;
    if (ChatObject.isCreator(c) || ChatObject.isAdmin(c)) return true;
    if (ChatObject.isChannelAndNotMegaGroup(c)) return false;

    if (c.banned_rights?.embed_links === true) return false;
    if (c.default_banned_rights?.embed_links === true) return false;

    return ChatObject.canSendMessages(c);
  }

  /**
   * Returns precise user restriction notice message for UI input banner
   */
  public static getRestrictedNotice(
    chat?: TelegramChatEntity | TLRPC.Chat | null,
    isArabic: boolean = true
  ): { restricted: boolean; reason?: 'channel' | 'banned' | 'kicked' | 'deactivated' | 'admin_only'; message?: string } {
    if (!chat) return { restricted: false };
    const c = chat as any;

    if (c.kicked || c.left) {
      return {
        restricted: true,
        reason: 'kicked',
        message: isArabic ? 'أنت لست عضواً في هذه المجموعة.' : 'You are not a member of this group.',
      };
    }

    if (c.deactivated) {
      return {
        restricted: true,
        reason: 'deactivated',
        message: isArabic ? 'هذه المجموعة تم تعطيلها.' : 'This group has been deactivated.',
      };
    }

    // Owner or Admin bypasses all posting restrictions
    if (ChatObject.isCreator(c) || ChatObject.isAdmin(c)) {
      return { restricted: false };
    }

    // Broadcast channels ONLY (where megagroup is false)
    if (ChatObject.isChannelAndNotMegaGroup(c)) {
      const canAdminPost = Boolean(c.creator || c.isCreator || c.isAdmin || c.admin_rights?.post_messages);
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
    const userBanned = c.banned_rights as ChatRights | undefined;
    const defaultBanned = c.default_banned_rights as ChatRights | undefined;

    if (userBanned) {
      const untilDate = userBanned.until_date || 0;
      const isExpired = untilDate > 0 && untilDate * 1000 < Date.now();
      if (!isExpired && (userBanned.send_messages === true || userBanned.send_plain === true)) {
        return {
          restricted: true,
          reason: 'banned',
          message: isArabic
            ? 'المشرفون قيدوا قدرتك على إرسال الرسائل في هذه المجموعة.'
            : 'Administrators have restricted your ability to send messages in this group.',
        };
      }
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

    if (c.adminOnly === true) {
      return {
        restricted: true,
        reason: 'admin_only',
        message: isArabic
          ? 'تم تفعيل وضع المشرفين فقط بواسطة الإدارة.'
          : 'Admin-only mode is active in this group.',
      };
    }

    return { restricted: false };
  }
}

