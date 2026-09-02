/**
 * Telegram Official ChatObject & Restriction Helper
 * Replicates org.telegram.messenger.ChatObject.java from DrKLO/Telegram
 * Encapsulates rights evaluation, banned rights calculation, and admin privileges.
 */

import { TLRPC } from '../tgnet/TLRPC';

export interface ChatRestrictionState {
  canSendMessages: boolean;
  canSendMedia: boolean;
  canSendStickers: boolean;
  canSendPolls: boolean;
  canEmbedLinks: boolean;
  isMutedForever: boolean;
  restrictionReason: string | null;
  isBanned: boolean;
  isKicked: boolean;
  isChannelBroadcast: boolean;
  isCreator: boolean;
  isAdmin: boolean;
  untilDate?: number;
}

export class ChatObject {
  public static isChannel(chat: any): boolean {
    return Boolean(
      chat && (chat.broadcast || chat.type === 'channel' || (chat.className === 'Channel' && !chat.megagroup) || chat.is_channel)
    );
  }

  public static isMegagroup(chat: any): boolean {
    return Boolean(
      chat && (chat.megagroup || chat.type === 'supergroup' || (chat.className === 'Channel' && chat.megagroup))
    );
  }

  public static hasAdminRights(chat: any): boolean {
    return Boolean(chat && (chat.creator || chat.is_creator || chat.admin_rights));
  }

  public static canPost(chat: any): boolean {
    if (!chat) return false;
    if (chat.creator || chat.is_creator) return true;
    if (chat.admin_rights && (chat.admin_rights.post_messages || chat.admin_rights.edit_messages)) return true;
    return false;
  }

  /**
   * Exact send rights evaluation engine matching Telegram Android ChatObject.java
   */
  public static checkSendRights(
    currentChat: any,
    currentChatFull?: TLRPC.TL_chatFull | null,
    myParticipant?: TLRPC.TL_channelParticipant | any | null
  ): ChatRestrictionState {
    const state: ChatRestrictionState = {
      canSendMessages: true,
      canSendMedia: true,
      canSendStickers: true,
      canSendPolls: true,
      canEmbedLinks: true,
      isMutedForever: false,
      restrictionReason: null,
      isBanned: false,
      isKicked: false,
      isChannelBroadcast: false,
      isCreator: false,
      isAdmin: false,
    };

    if (!currentChat) {
      return state;
    }

    // 0. Explicit Forbidden Check (e.g. ChatForbidden / deleted channel)
    if (currentChat.is_forbidden || currentChat._type === 'chatForbidden' || currentChat.className === 'ChatForbidden') {
      state.canSendMessages = false;
      state.canSendMedia = false;
      state.canSendStickers = false;
      state.canSendPolls = false;
      state.canEmbedLinks = false;
      state.restrictionReason = currentChat.forbidden_reason || 'المحادثة مغلقة أو غير متاحة على خوادم تليجرام.';
      return state;
    }

    // 1. Channel / Broadcast Rights
    const isBroadcast = ChatObject.isChannel(currentChat) && !ChatObject.isMegagroup(currentChat);
    state.isChannelBroadcast = isBroadcast;

    if (isBroadcast) {
      const isCreator = Boolean(
        currentChat.creator ||
        currentChat.is_creator ||
        myParticipant?._type === 'TL_channelParticipantCreator' ||
        myParticipant?.className === 'ChannelParticipantCreator'
      );
      state.isCreator = isCreator;

      const adminRights = myParticipant?.admin_rights || currentChat.admin_rights;
      const isAdmin = Boolean(
        isCreator ||
        adminRights?.post_messages ||
        myParticipant?._type === 'TL_channelParticipantAdmin' ||
        myParticipant?.className === 'ChannelParticipantAdmin'
      );
      state.isAdmin = isAdmin;

      if (isCreator || (adminRights && adminRights.post_messages)) {
        state.canSendMessages = true;
        state.canSendMedia = true;
        state.canSendStickers = true;
      } else {
        state.canSendMessages = false;
        state.canSendMedia = false;
        state.canSendStickers = false;
        state.canSendPolls = false;
        state.canEmbedLinks = false;
        state.restrictionReason = 'القناة مخصصة للمنشورات من المشرفين فقط (Broadcast Channel)';
      }
      return state;
    }

    // 2. Creator or Full Admin in Supergroup
    if (
      currentChat.creator ||
      currentChat.is_creator ||
      myParticipant?._type === 'TL_channelParticipantCreator' ||
      myParticipant?.className === 'ChannelParticipantCreator'
    ) {
      state.isCreator = true;
      state.isAdmin = true;
      return state;
    }

    if (
      myParticipant?._type === 'TL_channelParticipantAdmin' ||
      myParticipant?.className === 'ChannelParticipantAdmin' ||
      (currentChat.admin_rights && currentChat.admin_rights.post_messages !== false)
    ) {
      state.isAdmin = true;
      return state;
    }

    // 3. User Banned or Kicked Individually (TL_channelParticipantBanned)
    if (
      currentChat.is_banned ||
      currentChat.is_kicked ||
      myParticipant?._type === 'TL_channelParticipantBanned' ||
      myParticipant?.className === 'ChannelParticipantBanned' ||
      myParticipant?.banned_rights
    ) {
      const bannedRights: TLRPC.TL_chatBannedRights =
        myParticipant?.banned_rights || currentChat.banned_rights || {};

      state.untilDate = bannedRights.until_date || currentChat.banned_rights?.until_date;

      if (currentChat.is_kicked || myParticipant?.kicked) {
        state.isKicked = true;
        state.canSendMessages = false;
        state.canSendMedia = false;
        state.canSendStickers = false;
        state.restrictionReason = 'تم طردك من هذه المجموعة بواسطة المشرفين (UserBannedInChannel).';
        return state;
      }

      state.isBanned = true;

      if (bannedRights.send_messages || bannedRights.send_plain || bannedRights.view_messages) {
        state.canSendMessages = false;
        state.restrictionReason = 'تم تقييدك من إرسال الرسائل في هذه المجموعة من قبل المشرفين.';
      }
      if (bannedRights.send_media) {
        state.canSendMedia = false;
        if (state.canSendMessages) {
          state.restrictionReason = 'تم تقييدك من إرسال الوسائط في هذه المجموعة.';
        }
      }
      if (bannedRights.send_stickers || bannedRights.send_gifs) {
        state.canSendStickers = false;
      }
      if (bannedRights.send_polls) {
        state.canSendPolls = false;
      }
      if (bannedRights.embed_links) {
        state.canEmbedLinks = false;
      }

      if (!state.canSendMessages) {
        return state;
      }
    }

    // 4. Default Banned Rights for Supergroup / Chat
    const defaultRights: TLRPC.TL_chatBannedRights | undefined =
      currentChatFull?.default_banned_rights || currentChat.default_banned_rights;

    if (defaultRights) {
      if (defaultRights.send_messages || defaultRights.send_plain) {
        state.canSendMessages = false;
        state.restrictionReason = 'الكتابة معطلة في هذه المجموعة لغير المشرفين.';
      }
      if (defaultRights.send_media) {
        state.canSendMedia = false;
      }
      if (defaultRights.send_stickers || defaultRights.send_gifs) {
        state.canSendStickers = false;
      }
      if (defaultRights.send_polls) {
        state.canSendPolls = false;
      }
      if (defaultRights.embed_links) {
        state.canEmbedLinks = false;
      }
    }

    // 5. Explicit slowmode info if any
    if (currentChatFull?.slowmode_seconds || currentChat.slowmode_seconds) {
      currentChat.slowmode_seconds = currentChatFull?.slowmode_seconds || currentChat.slowmode_seconds;
    }

    return state;
  }
}
