export type MessageStatus = "sent" | "read" | "pending" | "sending" | "delivered" | "failed" | string;
export interface MessageReaction {
  emoji: string;
  emoticon?: string;
  count: number;
  isSelected?: boolean;
  chosen?: boolean;
  userIds?: (string | number)[];
  users?: (string | number)[];
  [key: string]: any;
}
// ==========================================
// UNIFIED TELEGRAM PRO & ENGINE TYPES (ALL 4 REPOS)
// ==========================================

declare global {
  interface Window {
    __pwa_deferred?: any;
    pwaInstallClick?: () => void;
    pwaDoInstall?: () => void;
    showNotification?: (msg: string, type?: "info" | "success" | "warning" | "error") => void;
    requestNotificationPermission?: () => void;
  }
}

export type ChatType =
  | "private"
  | "direct"
  | "group"
  | "supergroup"
  | "channel"
  | "bot"
  | "saved"
  | "secret"
  | "user";

export interface User {
  id: string | number;
  uid?: string | number;
  name?: string;
  first_name?: string;
  last_name?: string;
  firstName?: string;
  lastName?: string;
  username?: string;
  phone?: string;
  avatar?: string;
  photo?: string;
  isOnline?: boolean;
  is_online?: boolean;
  lastSeen?: string;
  last_seen?: string | number;
  bio?: string;
  status_text?: string;
  isVerified?: boolean;
  is_verified?: boolean;
  isBot?: boolean;
  is_bot?: boolean;
  isPremium?: boolean;
  is_premium?: boolean;
  premiumBadges?: string[];
  sessionString?: string;
  has_2fa?: boolean;
  hint_2fa?: string;
  [key: string]: any;
}

export type TelegramUser = User;
export type UserProfile = User;

export interface UserAccount {
  id: string;
  user: User;
  sessionString?: string;
  sessionToken?: string;
  isActive: boolean;
  unreadTotal?: number;
  unreadCount?: number;
  phone?: string;
  name?: string;
  avatar?: string;
  proxy?: AccountProxyConfig;
  settings?: any;
  chats?: any[];
  messages?: any;
}

export interface ProfileUserInfo {
  id: string | number;
  name?: string;
  username?: string;
  phone?: string;
  avatar?: string;
  bio?: string;
  isVerified?: boolean;
  isBot?: boolean;
  isOnline?: boolean;
  lastSeen?: string;
  isPremium?: boolean;
  sourceChatId?: string | number;
  sourceChatTitle?: string;
}

export interface Story {
  id: string | number;
  userId?: string | number;
  user_id?: string | number;
  userName?: string;
  user_name?: string;
  userAvatar?: string;
  user_avatar?: string;
  mediaUrl?: string;
  media_url?: string;
  mediaType?: "image" | "video";
  caption?: string;
  timestamp?: string | number;
  date?: string | number;
  expiresAt?: number;
  viewsCount?: number;
  views_count?: number;
  reactions_count?: number;
  isViewed?: boolean;
  isMyStory?: boolean;
  [key: string]: any;
}

export type TelegramStory = Story;

export interface Reaction {
  emoji: string;
  count: number;
  users?: string[];
  mine?: boolean;
  isLottie?: boolean;
}

export type ReactionItem = Reaction;

export interface ReplyInfo {
  messageId: string | number;
  senderName: string;
  textSnippet: string;
  mediaType?: string;
  [key: string]: any;
}

export interface InlineKeyboardButton {
  text: string;
  url?: string;
  callback_data?: string;
}

export interface BotInlineButton {
  text: string;
  url?: string;
  callback_data?: string;
  callbackData?: string;
  copy_text?: { text: string };
  web_app?: { url: string };
  [key: string]: any;
}

export interface MessageContent {
  type:
    | "text"
    | "photo"
    | "video"
    | "audio"
    | "voice"
    | "document"
    | "sticker"
    | "location"
    | "contact"
    | "poll"
    | "video_note";
  text?: string;
  caption?: string;
  filePath?: string;
  fileName?: string;
  fileSize?: number | string;
  stickerId?: string;
  duration?: number;
  width?: number;
  height?: number;
  mimeType?: string;
  waveform?: number[];
  pollQuestion?: string;
  pollOptions?: { text: string; votes: number }[];
  poll?: {
    id?: string;
    question: string;
    options: { id?: string | number; text: string; votes: number; total_voters?: number; totalVotes?: number }[];
    total_voters?: number;
    totalVotes?: number;
    is_closed?: boolean;
  };
}

export type SystemActionType =
  | "user_banned"
  | "user_unbanned"
  | "user_restricted"
  | "media_restricted"
  | "admin_added"
  | "admin_removed"
  | "user_joined"
  | "user_joined_by_link"
  | "user_left"
  | "chat_title_changed"
  | "chat_photo_changed"
  | "chat_photo_deleted"
  | "chat_migrated"
  | "channel_created"
  | "chat_created"
  | "message_pinned"
  | "info"
  | string;

export interface SystemMessageData {
  chat_id: string | number;
  message: string;
  type: SystemActionType;
  date: number | string;
  is_system: boolean;
  is_me?: boolean;
  user_id?: string | number;
  user_name?: string;
  admin_id?: string | number;
  admin_name?: string;
  details?: any;
}

export interface Message {
  id: string;
  chatId?: string | number;
  chat_id?: string | number;
  senderId?: string | number;
  sender_id?: string | number;
  senderName?: string;
  sender_name?: string;
  senderUsername?: string;
  senderAvatar?: string;
  text?: string;
  timestamp?: string | number;
  date?: string | number;
  rawDate?: number | string;
  epoch?: number;
  isOutgoing?: boolean;
  is_outgoing?: boolean;
  status?: MessageStatus | string;
  media?: TelegramMedia;
  mediaType?: string;
  forwardedFrom?: any;
  replyToId?: string;
  replyTo?: any;
  reply_to_msg_id?: string;
  replyToMessage?: Message;
  reply_to_message?: Message;
  reactions?: MessageReaction[];
  views?: number;
  forwards?: number;
  isPinned?: boolean;
  is_pinned?: boolean;
  isEdited?: boolean;
  editDate?: number;
  edit_date?: number;
  isForwarded?: boolean;
  isSecret?: boolean;
  expiresAt?: number | string;
  forwardInfo?: {
    fromName?: string;
    fromChatId?: string | number;
    date?: string;
  };
  viaBotName?: string;
  isScheduled?: boolean;
  scheduleDate?: number;
  voiceNoteDuration?: number;
  audioDuration?: number;
  videoDuration?: number;
  selfDestructTime?: number;
  groupedId?: string;
  grouped_id?: string;
  hasTimer?: boolean;
  ttl?: number;
  isTopicMessage?: boolean;
  topicId?: number;
  pollData?: {
    question: string;
    options: { id: string; text: string; votes: number; isVoted?: boolean }[];
    totalVotes: number;
    isClosed?: boolean;
    isMultipleAnswers?: boolean;
    isAnonymous?: boolean;
  };
  content?: MessageContent;
  reply_markup?: any;
  effect?: string;
  is_silent?: boolean;
  is_system?: boolean;
  system_type?: SystemActionType;
  action_data?: any;
  [key: string]: any;
}

export type MessageItem = Message;
export type TelegramMessage = Message;

export interface RestrictionReason {
  platform: string;
  reason: string;
  text: string;
}

export interface ChatBannedRights {
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
  until_date?: number;
}

export interface ChatAdminRights {
  change_info?: boolean;
  post_messages?: boolean;
  edit_messages?: boolean;
  delete_messages?: boolean;
  ban_users?: boolean;
  invite_users?: boolean;
  pin_messages?: boolean;
  add_admins?: boolean;
  anonymous?: boolean;
  manage_call?: boolean;
}

export interface Chat {
  id: string | number;
  name?: string;
  title?: string;
  type: ChatType;
  avatar?: string;
  photo?: string;
  isOnline?: boolean;
  lastSeen?: string;
  unreadCount?: number;
  unread_count?: number;
  unread?: number;
  isPinned?: boolean;
  is_pinned?: boolean;
  pinned?: boolean;
  pinnedIndex?: number;
  isMuted?: boolean;
  is_muted?: boolean;
  isArchived?: boolean;
  is_archived?: boolean;
  isContact?: boolean;
  is_contact?: boolean;
  isGroup?: boolean;
  isChannel?: boolean;
  isVerified?: boolean;
  is_verified?: boolean;
  memberCount?: number;
  participants_count?: number;
  members_count?: number;
  onlineCount?: number;
  description?: string;
  username?: string;
  folder?: string;
  folderId?: number;
  folder_id?: number;
  customTheme?: string;
  chatWallpaper?: string;
  isSecret?: boolean;
  secretChatKey?: string;
  autoDeletePeriod?: number;
  slowModeSeconds?: number;
  requiresCaptcha?: boolean;
  isCaptchaSolved?: boolean;
  isReadOnly?: boolean;
  adminOnly?: boolean;
  draftTimestamp?: number | string;
  messages?: Message[];
  lastMessage?: Message;
  last_message?: Message;
  lastMsg?: string;
  lastMsgDate?: number;
  draft?: string;
  typingUsers?: string[];
  banned_rights?: ChatBannedRights;
  admin_rights?: ChatAdminRights;
  default_banned_rights?: ChatBannedRights;
  [key: string]: any;
}

export type ChatItem = Chat;
export type TelegramDialog = Chat;

export interface Folder {
  id: string;
  name: string;
  nameAr?: string;
  icon?: string;
  chatTypes?: ChatType[];
  title?: string;
  chat_ids?: any[];
  includedChatIds?: string[];
  excludedChatIds?: string[];
  includeTypes?: ChatType[];
  color?: string;
}

export interface ChatFolder {
  id: string | number;
  title: string;
  icon?: string;
  emoticon?: string;
  color?: number;
  pinnedChatIds?: (string | number)[];
  includedChatIds?: (string | number)[];
  excludedChatIds?: (string | number)[];
  chat_ids?: (string | number)[];
  includeContacts?: boolean;
  includeNonContacts?: boolean;
  includeGroups?: boolean;
  includeChannels?: boolean;
  includeBots?: boolean;
  excludeMuted?: boolean;
  excludeRead?: boolean;
  excludeArchived?: boolean;
  filterFlags?: number;
  users?: (string | number)[];
}

export interface TelegramApiConfig {
  apiId: string;
  apiHash: string;
  dcId?: number;
  dcIp?: string;
  port?: number;
  connectionStatus?: string;
  sessionString?: string;
  mtprotoVersion?: string;
  pingMs?: number;
}

export interface AppSettings {
  language?: string;
  darkMode?: boolean;
  fontSize?: number;
  sendByEnter?: boolean;
  autoDownloadMedia?: boolean;
  passcodeEnabled?: boolean;
  notificationsEnabled?: boolean;
  soundEnabled?: boolean;
  ghostMode?: GhostModeSettings;
  streamAudio?: boolean;
  theme?: string;
  accentColor?: string;
  soundEffects?: boolean;
  chatWallpaper?: string;
  bubbleCornerRadius?: number;
  bubbleRadius?: number;
  [key: string]: any;
}

export interface ActiveSession {
  id: string | number;
  device?: string;
  device_name?: string;
  platform?: string;
  appVersion?: string;
  app_version?: string;
  ip?: string;
  location?: string;
  lastActive?: string | number;
  last_active?: string | number;
  isCurrent?: boolean;
  is_current?: boolean;
  official?: boolean;
  [key: string]: any;
}

export interface ChatMember {
  id: string | number;
  user_id?: string | number;
  name: string;
  username?: string;
  avatar?: string;
  role: "creator" | "admin" | "member" | "restricted" | "banned" | "owner" | "administrator" | string;
  customTitle?: string;
  joinedDate?: string;
  isOnline?: boolean;
  lastSeen?: string;
  banned_rights?: ChatBannedRights;
  admin_rights?: ChatAdminRights;
  [key: string]: any;
}

export interface StorageCategoryStats {
  photos?: number;
  videos?: number;
  files?: number;
  audio?: number;
  cache?: number;
  total?: number;
  [key: string]: any;
}

export interface StorageStats {
  usedBytes?: number;
  totalBytes?: number;
  freeBytes?: number;
  categories?: StorageCategoryStats;
  [key: string]: any;
}

export interface Contact {
  id: string;
  name: string;
  username?: string;
  phone: string;
  avatar?: string;
  isOnline?: boolean;
  lastSeen?: string;
}

export interface SystemUpdateStatus {
  hasUpdate?: boolean;
  has_update?: boolean;
  version?: string;
  current?: string;
  latest?: string;
  releaseNotes?: string;
  message?: string;
  downloadUrl?: string;
  isCritical?: boolean;
  [key: string]: any;
}

export interface PasscodeSettings {
  isEnabled: boolean;
  passcodeHash?: string;
  autoLockMinutes: number;
  allowBiometrics: boolean;
  isLocked: boolean;
  lastUnlockedAt?: number;
}

export interface PiPVideoTrack {
  id: string;
  videoUrl: string;
  url?: string;
  title: string;
  senderName: string;
  currentTime: number;
  isPlaying: boolean;
  duration?: number;
  isRoundVideoNote?: boolean;
  [key: string]: any;
}

export interface ChatPeekData {
  chat: Chat;
  chatId?: string | number;
  chatTitle?: string;
  chatAvatar?: string;
  isMuted?: boolean;
  isPinned?: boolean;
  isVerified?: boolean;
  memberCount?: number;
  messages: Message[];
  unreadCount: number;
  topOffset: number;
  leftOffset: number;
  [key: string]: any;
}

export interface GhostModeSettings {
  enabled?: boolean;
  hideOnline?: boolean;
  hideTyping?: boolean;
  hideReadReceipts?: boolean;
  hideRead?: boolean;
  hideStoryViews?: boolean;
}

export interface MultiAccount {
  id: string;
  name?: string;
  phone?: string;
  avatar?: string;
  isActive?: boolean;
  unreadTotal?: number;
  sessionToken?: string;
  sessionString?: string;
  user?: User;
  addedAt?: number;
}

export interface ScheduledMessage {
  id: string | number;
  chatId: string | number;
  chatTitle?: string;
  chatName?: string;
  text: string;
  scheduledTime: number;
  isSent?: boolean;
  createdAt?: number;
  [key: string]: any;
}

export interface AutoResponderRule {
  id: string;
  name?: string;
  keyword?: string;
  triggerKeyword?: string;
  response?: string;
  responseText?: string;
  isEnabled?: boolean;
  enabled?: boolean;
  onlyPrivate?: boolean;
  matchType?: "exact" | "contains" | "starts_with" | "regex";
  targetChats?: "all" | "private_only" | "groups_only";
}

export interface AutoForwardRule {
  id: string | number;
  sourceChatId: string | number;
  sourceChatName?: string;
  sourceChatTitle?: string;
  targetChatId: string | number;
  targetChatName?: string;
  targetChatTitle?: string;
  filterKeywords?: string[];
  isEnabled?: boolean;
  enabled?: boolean;
  removeQuote?: boolean;
  [key: string]: any;
}

export interface SendMonitorConfig {
  id?: string;
  enabled?: boolean;
  isEnabled?: boolean;
  sourceChats?: string[];
  targetChats?: string[];
  targetChatIds?: string[];
  keywordFilter?: string;
  keywords?: string[];
  replyMessage?: string;
  actionType?: string;
  sendType?: string;
  browserPushAlerts?: boolean;
  sendAlertsToSavedMessages?: boolean;
  broadcastIntervalMinutes?: number;
  repeatIntervalMinutes?: number;
  scheduledTime?: string;
  protectedGroupAction?: string;
  antiFloodDelaySeconds?: number;
  autoForward?: boolean;
  intervalSec?: number;
  [key: string]: any;
}

export interface SendMonitorLog {
  id: string | number;
  timestamp: string | number;
  sourceChat?: string;
  targetChat?: string;
  textSnippet?: string;
  status?: "forwarded" | "skipped" | "error" | string;
  reason?: string;
  [key: string]: any;
}

export interface SavedMessageTemplate {
  id: string;
  title: string;
  content: string;
  text?: string;
  category: string;
  tags?: string[];
  isPinned?: boolean;
  createdAt?: number | string;
  [key: string]: any;
}

export interface AutoJoinConfig {
  delaySeconds?: number;
  maxPerDay?: number;
  autoLeaveUnwanted?: boolean;
  keywordsWhitelist?: string[];
  randomDelayRange?: [number, number] | number;
  autoStartOnPaste?: boolean;
  maxJoinsPerBatch?: number;
}

export interface AutoJoinQueueItem {
  id: string;
  link?: string;
  linkOrUsername?: string;
  title?: string;
  status: "queued" | "joining" | "joined" | "failed" | "cooldown" | "pending" | "already_member" | string;
  timestamp?: string | number;
  error?: string;
  [key: string]: any;
}

export interface SavedTelegramLink {
  id: string;
  url?: string;
  link?: string;
  title?: string;
  username?: string;
  type?: "channel" | "group" | "bot" | "user" | "post" | "sticker" | "invite" | string;
  category?: string;
  tags?: string[];
  isJoined?: boolean;
  addedAt?: string | number;
  savedAt?: string | number;
  notes?: string;
  [key: string]: any;
}

export interface AcademicExtractConfig {
  targetFolder?: string;
  fileTypes?: string[];
  autoSummarize?: boolean;
  academicKeywords?: string[];
  sourceChatIds?: string[];
  searchQuery?: string;
  summarizeWithAI?: boolean;
}

export interface AcademicResourceItem {
  id: string | number;
  title: string;
  fileName?: string;
  fileType?: string;
  size?: string | number;
  fileSize?: number | string;
  sourceChat?: string;
  chatTitle?: string;
  chatId?: string | number;
  messageId?: string | number;
  downloadUrl?: string;
  summary?: string;
  date?: string | number;
  [key: string]: any;
}

export interface LinkMonitorConfig {
  enabled?: boolean;
  scanFrequencySec?: number;
  autoExtractMetadata?: boolean;
  autoJoinSafeLinks?: boolean;
  saveToSavedLinks?: boolean;
  monitoredChatIds?: string[];
  autoJoinCapturedTelegramLinks?: boolean;
  autoSaveToLinkBank?: boolean;
  soundAlert?: boolean;
  filterKeywords?: string[];
}

export interface CapturedLinkItem {
  id: string;
  url?: string;
  link?: string;
  linkType?: string;
  messageText?: string;
  chatTitle?: string;
  chatId?: string | number;
  sourceChatId?: string | number;
  sourceChatTitle?: string;
  senderName?: string;
  timestamp?: string | number;
  detectedAt?: string | number;
  detected_at?: string | number;
  type?: any;
  channelUsername?: string;
  metaTitle?: string;
  extractedTitle?: string;
  memberCount?: number;
  source_chat?: string;
  source_chat_id?: string | number;
  sender?: string;
  sourceSenderName?: string;
  autoJoined?: boolean;
  joined?: boolean;
  join_status?: string;
  status_text?: string;
  chat_title?: string;
  creation_date?: string;
  country?: string;
  status?: "pending" | "joined" | "failed" | "ignored" | "valid" | "invalid" | string;
  metadata?: {
    title?: string;
    members?: number;
    type?: string;
    [key: string]: any;
  };
  [key: string]: any;
}

export type CapturedLink = CapturedLinkItem;

export type SanitizeMode = "clean" | "keep-entities" | "strip-all" | "none" | "salam" | "remove_links" | "full" | string;
export type SendType = "direct" | "broadcast" | "scheduled" | "auto" | "manual" | string;

export interface WelcomeMessageSettings {
  enabled: boolean;
  template: string;
  sendToDirect: boolean;
  sendToGroup: boolean;
  includeBio: boolean;
}

export interface WelcomedUserRecord {
  userId: string;
  welcomedAt: number;
  chatId: string;
}

export interface TelegramSettings {
  apiId?: string;
  apiHash?: string;
  phoneNumber?: string;
  sessionString?: string;
  isLoggedIn?: boolean;
}

export interface WhatsAppSettings {
  instanceId?: string;
  token?: string;
  isConnected?: boolean;
  message?: string;
  groups?: string[];
  watch_words?: string[];
  sanitize_mode?: string;
  send_type?: string;
  interval_seconds?: number;
  schedule_duration_hours?: number;
  [key: string]: any;
}

export interface BatchEntry {
  id: string;
  recipient: string;
  status: "pending" | "sent" | "failed";
  error?: string;
}

export interface SentBatch {
  id: string;
  name?: string;
  text?: string;
  images?: string[];
  total?: number;
  successful?: number;
  failed?: number;
  totalSuccess?: number;
  totalFailed?: number;
  status?: string;
  timestamp?: string;
  createdAt?: string | number;
  sentAt?: string | number;
  edited_at?: string | number;
  sent_at?: string | number;
  group_count?: number;
  sent_count?: number;
  has_media?: boolean;
  entries?: BatchEntry[];
  targetChats?: any[];
  protectionMode?: any;
  isScheduled?: boolean;
  intervalMinutes?: number;
  durationHours?: number;
  [key: string]: any;
}

export interface SavedLink {
  id: string;
  url: string;
  title: string;
  type?: string;
  category?: string;
  tags?: string[];
  notes?: string;
  isJoined?: boolean;
  savedAt?: string | number;
  [key: string]: any;
}

export interface AutoJoinItem {
  id: string;
  link: string;
  title?: string;
  status: "pending" | "joining" | "joined" | "failed" | "already_member";
  autoJoined?: boolean;
  failReason?: string;
  timestamp?: string;
}

export interface AutoJoinProgressEvent {
  total: number;
  completed: number;
  currentLink?: string;
  status: "running" | "paused" | "completed" | "error" | string;
  message?: string;
  counts?: any;
  url?: string;
  reason?: string;
  [key: string]: any;
}

export interface LearningService {
  id?: string;
  name?: string;
  status?: "active" | "training" | "idle" | string;
  accuracy?: number;
  lastUpdated?: string;
  description?: string;
  keywords?: string[];
  price_range?: string;
  time_range?: string;
  [key: string]: any;
}

export interface UnknownRequest {
  id: string;
  query: string;
  timestamp: string;
  handled: boolean;
}

export interface StatsResult {
  totalMessages: number;
  totalChats: number;
  activeToday: number;
  storageUsedBytes: number;
}

export interface ActivityLog {
  id: string;
  action?: string;
  details?: string;
  timestamp: string;
  level?: "info" | "warning" | "error" | string;
  type?: string;
  message?: string;
  [key: string]: any;
}

export interface AcademicAnalysisResult {
  id?: string;
  documentTitle?: string;
  keyInsights?: string[];
  summaryText?: string;
  suggestedTags?: string[];
  readingTimeMinutes?: number;
  stats?: any;
  histogram_bars?: any[];
  summary?: any;
  [key: string]: any;
}

export interface LogUpdate {
  message: string;
  type?: "info" | "success" | "warning" | "error";
  timestamp: string;
}

export interface AccountProxyConfig {
  enabled?: boolean;
  type?: "socks5" | "http" | "mtproxy" | string;
  server?: string;
  port?: number;
  username?: string;
  password?: string;
  secret?: string;
  [key: string]: any;
}

export interface TelegramAccount {
  id: string;
  session_name?: string;
  phoneNumber?: string;
  phone?: string;
  firstName?: string;
  lastName?: string;
  username?: string;
  avatar?: string;
  isActive?: boolean;
  is_active?: boolean;
  isLoggedIn?: boolean;
  proxy?: AccountProxyConfig;
  status?: "connected" | "disconnected" | "connecting" | "banned" | string;
  lastSync?: string | number;
  sessionString?: string;
  [key: string]: any;
}

export interface MultiAccountBroadcastResult {
  accountId: string;
  session_name?: string;
  phone?: string;
  status?: string;
  error?: string;
  successCount: number;
  failCount: number;
  [key: string]: any;
}

export type ScrapedLinkType = "telegram" | "whatsapp" | "channel" | "group" | "bot" | "user" | "invite" | "chat" | "link" | "external" | "other" | string;
export type ScrapedLinkTypeWrapper = ScrapedLinkType;
export type LinkVerifyStatus = "valid" | "invalid" | "checking" | "unknown";

export interface ScrapedLinkItem {
  id: string;
  url: string;
  sourceChatTitle?: string;
  source_title?: string;
  senderName?: string;
  sender_name?: string;
  timestamp?: string | number;
  type?: ScrapedLinkType;
  channelUsername?: string;
  metaTitle?: string;
  memberCount?: number;
  joined?: boolean;
  autoJoined?: boolean;
  [key: string]: any;
}

export type ScrapeTimeRange = "all" | "today" | "week" | "month" | "10_days" | "30_days" | "24_hours" | "custom" | string;

export interface LinkScrapeProgressEvent {
  currentChatTitle?: string;
  scannedChats?: number;
  scanned_chats?: number;
  totalChats?: number;
  total_chats?: number;
  discoveredCount?: number;
  validCount?: number;
  percent?: number;
  isComplete?: boolean;
  [key: string]: any;
}

export interface LiveCapturedLinkItem extends CapturedLinkItem {}

export interface LiveMonitorState {
  isActive: boolean;
  monitoredChats: string[];
  capturedCount: number;
  autoJoinEnabled: boolean;
}

export interface LiveDiscoveredLink {
  id: string;
  url: string;
  cleanHandle?: string;
  sourceChatId?: string | number;
  sourceChatTitle?: string;
  senderName?: string;
  timestamp: string | number;
  discoveredAt?: string | number;
  status: "pending" | "joining" | "joined" | "failed" | "skipped" | "ignored" | "invalid";
  linkType?: "public_group" | "private_invite" | "broadcast_channel" | "unknown" | ScrapedLinkType;
  isPrivate?: boolean;
  isGroup?: boolean;
  metaTitle?: string;
  membersCount?: number;
  autoJoined?: boolean;
  failReason?: string;
  joinAttemptTime?: number;
}

export interface SenderBatch extends SentBatch {}

export type NotificationCategory =
  | "message"
  | "mention"
  | "call"
  | "reaction"
  | "system"
  | "system_security"
  | "pinned"
  | "channel"
  | "group"
  | "channel_post"
  | "keyword_alert"
  | "reply"
  | string;

export interface InAppNotification {
  id: string | number;
  chatId?: string | number;
  title?: string;
  message?: string;
  body?: string;
  avatar?: string;
  category?: NotificationCategory;
  timestamp?: number | string;
  duration?: number;
  actionUrl?: string;
  isRead?: boolean;
  type?: string;
  isSilent?: boolean;
  chatTitle?: string;
  chatUsername?: string;
  messageId?: string | number;
  senderId?: string | number;
  senderName?: string;
  senderUsername?: string;
  keyword?: string;
  messageText?: string;
  replyAction?: any;
  [key: string]: any;
}

export type TelegramNotification = InAppNotification;
export type NotificationItem = InAppNotification;

export interface LinkPreviewData {
  url: string;
  displayUrl?: string;
  title?: string;
  description?: string;
  image?: string;
  siteName?: string;
  type?: string;
  favicon?: string;
  channelUsername?: string;
  memberCount?: number;
}

export interface ResolvedTelegramLink {
  type: "channel" | "group" | "user" | "bot" | "msg" | "sticker" | "unknown";
  peerId?: string | number;
  username?: string;
  title?: string;
  photo?: string;
  isMember?: boolean;
  hash?: string;
  id?: string | number;
  [key: string]: any;
}

export interface ActiveAudioTrack {
  id?: string;
  title?: string;
  performer?: string;
  url?: string;
  duration?: number;
  isPlaying?: boolean;
  currentTime?: number;
  [key: string]: any;
}

export interface TelegramSearchResult {
  id?: string | number;
  title?: string;
  username?: string;
  type?: string;
  membersCount?: number;
  description?: string;
  [key: string]: any;
}

export type ProtectedGroupAction = 'allow' | 'warn' | 'block' | 'skip' | string;
export type SendScheduleType = 'immediate' | 'interval' | 'specific_time' | string;

export interface TelegramMedia {
  type: "photo" | "video" | "audio" | "voice" | "document" | "sticker" | "location" | "contact" | "poll" | "animation" | string;
  url?: string;
  previewUrl?: string;
  name?: string;
  fileName?: string;
  fileSize?: number | string;
  size?: number | string;
  duration?: number;
  waveform?: number[];
  mimeType?: string;
  width?: number;
  height?: number;
  thumbnail?: string;
  isVoice?: boolean;
  isRoundVideo?: boolean;
  title?: string;
  performer?: string;
  pollData?: any;
  emoji?: string;
  caption?: string;
  [key: string]: any;
}

export type SettingsSubPage =
  | "main"
  | "edit-profile"
  | "notifications"
  | "privacy"
  | "appearance"
  | "chat-folders"
  | "folders"
  | "devices"
  | "language"
  | "storage"
  | "saved-messages"
  | "my-stories"
  | "stickers"
  | "passcode"
  | "auto-night-mode"
  | "plus_settings"
  | "themes_browser"
  | "theme_coloring"
  | "support_group"
  | "chat_settings"
  | string;

export interface AppTheme {
  id?: string;
  name?: string;
  previewUrl?: string;
  colors?: Record<string, string>;
  isCustom?: boolean;
  [key: string]: any;
}

export interface AutoJoinerTask {
  id: string;
  link?: string;
  url?: string;
  type?: string;
  status: "pending" | "joining" | "joined" | "failed" | "invalid";
  timestamp?: number | string;
  processedAt?: number | string;
  error?: string;
  errorReason?: string;
}

export interface AutoReplyRule {
  id: string;
  pattern?: string;
  match?: string;
  reply?: string;
  response?: string;
  used_count?: number;
  last_used?: string | number;
  isEnabled?: boolean;
  enabled?: boolean;
  [key: string]: any;
}

export interface SmartAiService {
  id: string;
  name: string;
  status?: "active" | "idle" | "training";
  accuracy?: number;
  description?: string;
  keywords?: string[];
}

export interface SmartAiPattern {
  id: string;
  pattern?: string;
  response?: string;
  frequency?: number;
  triggerContext?: string;
  recommendedReply?: string;
  learnedDate?: string | number;
  isAccepted?: boolean;
}

export type ProtectionMode = string | {
  antiForward?: boolean;
  antiScreenshot?: boolean;
  watermark?: boolean;
  selfDestructDefault?: number;
};

export interface MyMessagesBatch {
  id: string;
  title?: string;
  messagesCount?: number;
  imagesCount?: number;
  groupsCount?: number;
  text?: string;
  targets?: any[];
  hasImages?: boolean;
  createdAt?: number | string;
  date?: string | number;
  timestamp?: string | number;
}


export type MonitorConfig = SendMonitorConfig;

export interface MonitorAlert {
  id: string;
  message?: string;
  keyword?: string;
  timestamp: number | string;
  sourceChatId?: string | number;
  sourceChatTitle?: string;
  senderName?: string;
  messageText?: string;
  [key: string]: any;
}

export type MessageMedia = TelegramMedia;


export interface ActiveCall {
  id?: string;
  chatId?: string | number;
  chatTitle?: string;
  chatAvatar?: string;
  type?: "audio" | "video" | string;
  isVideo?: boolean;
  isMuted?: boolean;
  isCameraOff?: boolean;
  isVideoEnabled?: boolean;
  isScreenSharing?: boolean;
  durationSeconds?: number;
  duration?: number;
  status?: string;
  state?: "calling" | "connected" | "ended" | string;
  encryptionEmojis?: [string, string, string, string];
  [key: string]: any;
}

export interface ChatContextMenu {
  x: number;
  y: number;
  chatId: string | number;
  [key: string]: any;
}

export interface MessageContextMenu {
  x: number;
  y: number;
  messageId?: string | number;
  message?: Message;
  [key: string]: any;
}

export interface ToastItem {
  id: string;
  message?: string;
  text?: string;
  icon?: string;
  type?: "info" | "success" | "warning" | "error";
  duration?: number;
  [key: string]: any;
}

// -------------------------------------------------------------
// Extended Types from Documents/master
// -------------------------------------------------------------

export type AiToneId =
  | 'formal'
  | 'friendly'
  | 'academic'
  | 'persuasive'
  | 'concise'
  | 'urgent'
  | 'creative'
  | 'bullet_summary'
  | 'neutral'
  | 'casual'
  | 'poetic'
  | 'humorous'
  | 'pirate';

export interface AiComposeTone {
  id: AiToneId;
  name?: string;
  nameAr?: string;
  label?: string;
  labelAr?: string;
  icon?: string;
  description?: string;
  descriptionAr?: string;
  systemPrompt?: string;
  prompt?: string;
}

export interface ContactBirthday {
  userId: string;
  name?: string;
  username?: string;
  avatar?: string;
  birthDate?: string;
  age?: number;
  day?: number;
  month?: number;
  year?: number;
  isToday?: boolean;
  daysRemaining?: number;
  hasCelebrated?: boolean;
}

export interface ChatCacheUsageInfo {
  chatId: string;
  chatTitle: string;
  chatAvatar?: string;
  sizeBytes?: number;
  totalBytes?: number;
  photosCount?: number;
  videosCount?: number;
  docsCount?: number;
  audiosCount?: number;
  photosBytes?: number;
  videosBytes?: number;
  audioBytes?: number;
  documentsBytes?: number;
  otherBytes?: number;
  keepMediaMode?: string;
}

export interface ChannelBoostData {
  chatId?: string;
  boostsCount?: number;
  level?: number;
  currentBoosts?: number;
  neededBoosts?: number;
  hasMyBoost?: boolean;
  canBoost?: boolean;
  boostUrl?: string;
  myBoostsCount?: number;
  boostsToNextLevel?: number;
  currentLevel?: number;
  unlockedPerks?: any[];
}

export interface MessageFactCheck {
  messageId: string;
  chatId?: string;
  country: string;
  text: string;
  hash?: string;
  organization?: string;
  organizationLogo?: string;
  sourceUrl?: string;
  url?: string;
  isExpanded?: boolean;
  checkedAt?: number | string;
}

export interface GiftAuctionItem {
  id: string;
  giftId: string;
  title: string;
  symbol?: string;
  minBid?: number;
  currentBid?: number;
  minNextBid?: number;
  currentBidStars?: number;
  highestBidderId?: string;
  highestBidderName?: string;
  highestBidderAvatar?: string;
  totalBidsCount?: number;
  recentBids?: any[];
  attributes?: any;
  topBidder?: string;
  endsAt: number;
}

export interface StarGiftItem {
  id: string;
  name?: string;
  title?: string;
  priceStars?: number;
  starsPrice?: number;
  icon?: string;
  emoji?: string;
  limitedCount?: number;
  remainingCount?: number;
  soldCount?: number;
  isLimited?: boolean;
  badge?: string;
  totalAvailable?: number;
}

export interface MemberJoinRequestItem {
  id?: string;
  userId: string;
  chatId: string;
  chatTitle?: string;
  userName: string;
  userAvatar: string;
  bio?: string;
  userBio?: string;
  date?: number;
  requestedAt?: number | string;
  status?: 'pending' | 'accepted' | 'declined' | 'approved';
}

export interface FcmPushPacket {
  id: string;
  title?: string;
  body?: string;
  data?: any;
  timestamp?: string | number;
  status?: string;
  [key: string]: any;
}

export interface PlusConfig {
  // 1. General (عام)
  fontFamily: string;
  keepScreenOn: boolean;
  proximitySensor: boolean;
  useExternalBrowser: boolean;
  hapticFeedback: boolean;
  bigEmojis: boolean;
  showDirectShare: boolean;
  cacheLimitGb: number;
  // 2. Chats (المحادثات)
  tabsEnabled: boolean;
  tabsPosition: 'top' | 'bottom';
  showUnreadTabsCounter: boolean;
  hideMutedTabs: boolean;
  showOnlineStatusDot: boolean;
  doubleTapAction: 'reply' | 'reaction' | 'copy' | 'pin';
  chatSwipeAction: 'archive' | 'mute' | 'delete' | 'pin' | 'read';
  confirmBeforeCall: boolean;
  // 3. Stories (القصص)
  hideStoriesBar: boolean;
  stealthModeStories: boolean;
  autoSaveStories: boolean;
  highQualityPlayback: boolean;
  storySpeed: '1x' | '1.5x' | '2x';
  storyExpirationAlert: boolean;
  // 4. Messages (الرسائل)
  forwardWithoutQuote: boolean;
  showUserIdOnMessages: boolean;
  showExactSeconds: boolean;
  showEditedHistory: boolean;
  confirmVoiceNotes: boolean;
  confirmStickers: boolean;
  autoTranslateIncoming: boolean;
  translationProvider: 'telegram' | 'google' | 'deepl';
  // 5. Topics (المواضيع)
  topicsAsTabs?: boolean;
  autoOpenGeneralTopic?: boolean;
  unreadTopicBadges?: boolean;
  quickTopicSearch?: boolean;
  lastTopicMessagePreview?: boolean;
  // 6. Navigation Drawer (درج التصفح)
  drawerShowNightMode?: boolean;
  drawerShowSavedMessages?: boolean;
  drawerShowCalls?: boolean;
  drawerShowContacts?: boolean;
  drawerShowPlusSettings?: boolean;
  drawerShowAccounts?: boolean;
  drawerHeaderStyle?: 'standard' | 'minimal' | 'custom';
  // 7. Profile (الملف الشخصي)
  profileShowUserId?: boolean;
  profileCopyIdOnTap?: boolean;
  profileShowCommonGroups?: boolean;
  profileHidePhone?: boolean;
  profileQuickActions?: boolean;
  // 8. Notifications (الإشعارات)
  inAppNotificationStyle?: 'banner' | 'pill' | 'silent';
  repeatUnreadAlerts?: 'off' | '5min' | '15min';
  customPrivateTone?: string;
  customGroupTone?: string;
  vipPriorityAlerts?: boolean;
  filterSpamAlerts?: boolean;
  // 9. Privacy & Security (الخصوصية والأمان)
  ghostMode?: boolean;
  hideOnlineStatus?: boolean;
  hideReadReceipts?: boolean;
  hideTypingIndicator?: boolean;
  antiDeleteMessages?: boolean;
  antiEditMessages?: boolean;
  appLockPasscode?: string;
  isAppLockEnabled?: boolean;
  biometricsEnabled?: boolean;
  hiddenChatsLocked?: boolean;
  // 10. Shared Media (الوسائط المتبادلة)
  defaultMediaTab?: 'photos' | 'videos' | 'files' | 'audio' | 'links' | 'voice';
  gridColumnsCount?: number;
  highResThumbnailPreview?: boolean;
  pipFloatingVideo?: boolean;
  autoPauseAudioOnVideo?: boolean;
  customMediaPath?: string;
  // 11. Downloads (التحميلات)
  autoDownloadWifi?: boolean;
  autoDownloadCellular?: boolean;
  downloadBooster?: boolean;
  maxConcurrentDownloads?: number;
  downloadFinishSound?: boolean;
  autoResumeDownloads?: boolean;
  // 12. Ads (الإعلانات)
  blockSponsoredMessages?: boolean;
  hidePromotedChannels?: boolean;
  blockBotAds?: boolean;
  disablePromoAlerts?: boolean;
  [key: string]: any;
}

