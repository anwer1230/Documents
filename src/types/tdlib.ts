// TDLib (Telegram Database Library) Standard Types & Schemas
// Matches the official td_api.tl specification

export interface TdObject {
  '@type': string;
  '@extra'?: string | number;
  [key: string]: any;
}

export interface TdError extends TdObject {
  '@type': 'error';
  code: number;
  message: string;
}

export interface TdOk extends TdObject {
  '@type': 'ok';
}

// ─── PARAMETERS & AUTHENTICATION ─────────────────────────────────────────────

export interface TdlibParameters {
  '@type': 'tdlibParameters';
  use_test_dc?: boolean;
  database_directory?: string;
  files_directory?: string;
  use_file_database?: boolean;
  use_chat_info_database?: boolean;
  use_message_database?: boolean;
  use_secret_chats?: boolean;
  api_id: number;
  api_hash: string;
  system_language_code: string;
  device_model: string;
  system_version: string;
  application_version: string;
  enable_storage_optimizer?: boolean;
  ignore_file_names?: boolean;
}

export type AuthorizationState =
  | { '@type': 'authorizationStateWaitTdlibParameters' }
  | { '@type': 'authorizationStateWaitEncryptionKey'; is_encrypted: boolean }
  | { '@type': 'authorizationStateWaitPhoneNumber' }
  | { '@type': 'authorizationStateWaitCode'; is_registered: boolean; code_info?: { phone_number: string; timeout: number } }
  | { '@type': 'authorizationStateWaitOtherDeviceConfirmation'; link: string }
  | { '@type': 'authorizationStateWaitRegistration'; terms_of_service: any }
  | { '@type': 'authorizationStateWaitPassword'; password_hint?: string; has_recovery_email_address?: boolean }
  | { '@type': 'authorizationStateReady' }
  | { '@type': 'authorizationStateLoggingOut' }
  | { '@type': 'authorizationStateClosing' }
  | { '@type': 'authorizationStateClosed' };

// ─── USER & CHAT OBJECTS ─────────────────────────────────────────────────────

export interface TdUser extends TdObject {
  '@type': 'user';
  id: number | string;
  first_name: string;
  last_name: string;
  username: string;
  phone_number: string;
  status: TdUserStatus;
  profile_photo?: {
    '@type': 'profilePhoto';
    id: string;
    small: TdFile;
    big: TdFile;
  };
  is_contact?: boolean;
  is_mutual_contact?: boolean;
  is_verified?: boolean;
  is_premium?: boolean;
  is_support?: boolean;
  restriction_reason?: string;
  have_access?: boolean;
  type?: { '@type': 'userTypeRegular' | 'userTypeBot' | 'userTypeDeleted' | 'userTypeUnknown' };
  language_code?: string;
}

export type TdUserStatus =
  | { '@type': 'userStatusEmpty' }
  | { '@type': 'userStatusOnline'; expires: number }
  | { '@type': 'userStatusOffline'; was_online: number }
  | { '@type': 'userStatusRecently' }
  | { '@type': 'userStatusLastWeek' }
  | { '@type': 'userStatusLastMonth' };

export interface TdChatPosition {
  '@type': 'chatPosition';
  list: { '@type': 'chatListMain' | 'chatListArchive' | 'chatListFolder'; chat_folder_id?: number };
  order: string;
  is_pinned: boolean;
  source?: any;
}

export interface TdChat extends TdObject {
  '@type': 'chat';
  id: number | string;
  type: TdChatType;
  title: string;
  photo?: {
    '@type': 'chatPhotoInfo';
    small: TdFile;
    big: TdFile;
    has_animation?: boolean;
  };
  permissions?: TdChatPermissions;
  last_message?: TdMessage;
  positions: TdChatPosition[];
  is_marked_as_unread?: boolean;
  is_blocked?: boolean;
  has_scheduled_messages?: boolean;
  can_be_deleted_only_for_self?: boolean;
  can_be_deleted_for_all_users?: boolean;
  can_be_reported?: boolean;
  default_disable_notification?: boolean;
  unread_count: number;
  last_read_inbox_message_id?: number | string;
  last_read_outbox_message_id?: number | string;
  unread_mention_count?: number;
  notification_settings?: {
    '@type': 'chatNotificationSettings';
    use_default_mute_for?: boolean;
    mute_for?: number;
    use_default_sound?: boolean;
    sound_id?: string;
    use_default_show_preview?: boolean;
    show_preview?: boolean;
  };
  draft_message?: TdDraftMessage;
  client_data?: string;
}

export type TdChatType =
  | { '@type': 'chatTypePrivate'; user_id: number | string }
  | { '@type': 'chatTypeBasicGroup'; basic_group_id: number | string }
  | { '@type': 'chatTypeSupergroup'; supergroup_id: number | string; is_channel: boolean }
  | { '@type': 'chatTypeSecret'; secret_chat_id: number | string; user_id: number | string };

export interface TdChatPermissions {
  '@type': 'chatPermissions';
  can_send_basic_messages: boolean;
  can_send_audios: boolean;
  can_send_documents: boolean;
  can_send_photos: boolean;
  can_send_videos: boolean;
  can_send_video_notes: boolean;
  can_send_voice_notes: boolean;
  can_send_polls: boolean;
  can_send_other_messages: boolean;
  can_add_web_page_previews: boolean;
  can_change_info: boolean;
  can_invite_users: boolean;
  can_pin_messages: boolean;
  can_manage_topics?: boolean;
}

// ─── MESSAGES & CONTENT ──────────────────────────────────────────────────────

export interface TdMessage extends TdObject {
  '@type': 'message';
  id: number | string;
  sender_id: { '@type': 'messageSenderUser'; user_id: number | string } | { '@type': 'messageSenderChat'; chat_id: number | string };
  chat_id: number | string;
  sending_state?: { '@type': 'messageSendingStatePending' | 'messageSendingStateFailed' };
  is_outgoing: boolean;
  is_pinned?: boolean;
  can_be_edited?: boolean;
  can_be_forwarded?: boolean;
  can_be_saved?: boolean;
  can_be_deleted_only_for_self?: boolean;
  can_be_deleted_for_all_users?: boolean;
  can_get_viewers?: boolean;
  can_get_media_timestamp_links?: boolean;
  has_timestamped_media?: boolean;
  is_channel_post?: boolean;
  contains_unread_mention?: boolean;
  date: number;
  edit_date?: number;
  reply_to_message_id?: number | string;
  content: TdMessageContent;
  reply_markup?: any;
}

export type TdMessageContent =
  | { '@type': 'messageText'; text: TdFormattedText; web_page?: any }
  | { '@type': 'messagePhoto'; photo: { sizes: { type: string; photo: TdFile; width: number; height: number }[] }; caption: TdFormattedText; is_secret?: boolean }
  | { '@type': 'messageVideo'; video: { duration: number; width: number; height: number; video: TdFile; thumbnail?: any; mime_type?: string }; caption: TdFormattedText }
  | { '@type': 'messageAudio'; audio: { duration: number; title?: string; performer?: string; audio: TdFile; mime_type?: string }; caption: TdFormattedText }
  | { '@type': 'messageDocument'; document: { file_name: string; mime_type: string; document: TdFile }; caption: TdFormattedText }
  | { '@type': 'messageVoiceNote'; voice_note: { duration: number; waveform?: string; mime_type: string; voice: TdFile }; caption: TdFormattedText }
  | { '@type': 'messageSticker'; sticker: { set_id?: string; width: number; height: number; emoji: string; format: { '@type': 'stickerFormatWebp' | 'stickerFormatTgs' | 'stickerFormatWebm' }; sticker: TdFile } }
  | { '@type': 'messageContact'; contact: { phone_number: string; first_name: string; last_name: string; vcard?: string; user_id?: number } }
  | { '@type': 'messageLocation'; location: { latitude: number; longitude: number; horizontal_accuracy?: number } }
  | { '@type': 'messagePoll'; poll: { id: string; question: string; options: { text: string; voter_count: number; vote_percentage: number; is_chosen: boolean }[]; total_voter_count: number; is_closed: boolean; is_anonymous: boolean; type: any } }
  | { '@type': 'messageChatAddMembers'; member_user_ids: (number | string)[] }
  | { '@type': 'messageChatDeleteMember'; user_id: number | string }
  | { '@type': 'messagePinMessage'; message_id: number | string }
  | { '@type': 'messageBasicGroupChatCreate'; title: string; member_user_ids: (number | string)[] }
  | { '@type': 'messageSupergroupChatCreate'; title: string }
  | { '@type': 'messageCustomServiceAction'; text: string };

export interface TdFormattedText {
  '@type': 'formattedText';
  text: string;
  entities?: TdTextEntity[];
}

export interface TdTextEntity {
  '@type': 'textEntity';
  offset: number;
  length: number;
  type: { '@type': 'textEntityTypeBold' | 'textEntityTypeItalic' | 'textEntityTypeCode' | 'textEntityTypeUrl' | 'textEntityTypeMention' | 'textEntityTypeHashtag' | 'textEntityTypeCustomEmoji' | 'textEntityTypeSpoiler' };
}

export interface TdDraftMessage {
  '@type': 'draftMessage';
  reply_to_message_id?: number | string;
  date: number;
  input_message_text: {
    '@type': 'inputMessageText';
    text: TdFormattedText;
    disable_web_page_preview?: boolean;
    clear_draft?: boolean;
  };
}

export interface TdFile {
  '@type': 'file';
  id: number | string;
  size: number;
  expected_size: number;
  local: {
    '@type': 'localFile';
    path: string;
    can_be_downloaded: boolean;
    can_be_deleted: boolean;
    is_downloading_active: boolean;
    is_downloading_completed: boolean;
    download_offset: number;
    downloaded_prefix_size: number;
    downloaded_size: number;
  };
  remote: {
    '@type': 'remoteFile';
    id: string;
    unique_id: string;
    is_uploading_active: boolean;
    is_uploading_completed: boolean;
    uploaded_size: number;
  };
}

// ─── TDLib UPDATES (Event Stream) ─────────────────────────────────────────────

export type TdUpdate =
  | { '@type': 'updateAuthorizationState'; authorization_state: AuthorizationState }
  | { '@type': 'updateNewMessage'; message: TdMessage }
  | { '@type': 'updateMessageSendSucceeded'; message: TdMessage; old_message_id: number | string }
  | { '@type': 'updateMessageSendFailed'; message: TdMessage; old_message_id: number | string; error_code: number; error_message: string }
  | { '@type': 'updateMessageContent'; chat_id: number | string; message_id: number | string; new_content: TdMessageContent }
  | { '@type': 'updateMessageEdited'; chat_id: number | string; message_id: number | string; edit_date: number; reply_markup?: any }
  | { '@type': 'updateDeleteMessages'; chat_id: number | string; message_ids: (number | string)[]; is_permanent: boolean; from_cache: boolean }
  | { '@type': 'updateChatPosition'; chat_id: number | string; position: TdChatPosition }
  | { '@type': 'updateChatLastMessage'; chat_id: number | string; last_message?: TdMessage; positions: TdChatPosition[] }
  | { '@type': 'updateChatDraftMessage'; chat_id: number | string; draft_message?: TdDraftMessage; positions: TdChatPosition[] }
  | { '@type': 'updateChatReadInbox'; chat_id: number | string; last_read_inbox_message_id: number | string; unread_count: number }
  | { '@type': 'updateChatReadOutbox'; chat_id: number | string; last_read_outbox_message_id: number | string }
  | { '@type': 'updateChatUnreadCount'; chat_id: number | string; unread_count: number }
  | { '@type': 'updateUser'; user: TdUser }
  | { '@type': 'updateUserStatus'; user_id: number | string; status: TdUserStatus }
  | { '@type': 'updateFile'; file: TdFile }
  | { '@type': 'updateConnectionState'; state: { '@type': 'connectionStateWaitingForNetwork' | 'connectionStateConnectingToProxy' | 'connectionStateConnecting' | 'connectionStateUpdating' | 'connectionStateReady' } }
  | { '@type': 'updateOption'; name: string; value: any }
  | { '@type': 'updateNewChat'; chat: TdChat }
  | { '@type': 'updateChatTitle'; chat_id: number | string; title: string }
  | { '@type': 'updateChatPhoto'; chat_id: number | string; photo?: any }
  | { '@type': 'updateChatPermissions'; chat_id: number | string; permissions: TdChatPermissions };

// ─── TDLib JSON REQUEST PAYLOADS ──────────────────────────────────────────────

export interface TdRequest {
  '@type': string;
  '@extra'?: string | number;
  [key: string]: any;
}
