export type ChatType = 'private' | 'group' | 'channel' | 'bot' | 'saved' | 'secret' | 'direct' | 'supergroup' | 'user';

export interface User {
  id: string;
  uid?: string;
  name?: string;
  firstName?: string;
  lastName?: string;
  first_name?: string;
  last_name?: string;
  username?: string;
  phone?: string;
  avatar?: string;
  isOnline?: boolean;
  lastSeen?: string;
  bio?: string;
  isVerified?: boolean;
  isBot?: boolean;
  isPremium?: boolean;
  premiumBadges?: string[];
  sessionString?: string;
  [key: string]: any;
}

export interface ProfileUserInfo {
  id: string;
  name: string;
  username?: string;
  phone?: string;
  avatar?: string;
  bio?: string;
  isVerified?: boolean;
  isBot?: boolean;
  isOnline?: boolean;
  lastSeen?: string;
  isPremium?: boolean;
  sourceChatId?: string;
  sourceChatTitle?: string;
}

export interface Story {
  id: string;
  userId?: string;
  user_id?: string;
  userName?: string;
  user_name?: string;
  userAvatar?: string;
  user_avatar?: string;
  mediaUrl?: string;
  media_url?: string;
  mediaType?: 'image' | 'video' | string;
  caption?: string;
  timestamp?: string;
  date?: string | number;
  expiresAt?: number;
  viewsCount?: number;
  views_count?: number;
  reactions_count?: number;
  isViewed?: boolean;
  is_viewed?: boolean;
  isMyStory?: boolean;
  [key: string]: any;
}

export interface Reaction {
  emoji: string;
  count: number;
  users: string[]; // user IDs who reacted
  isLottie?: boolean;
  mine?: boolean;
  isMine?: boolean;
  [key: string]: any;
}

export interface ReplyInfo {
  messageId?: string;
  id?: string;
  senderName?: string;
  textSnippet?: string;
  text?: string;
  mediaType?: 'photo' | 'audio' | 'document' | 'video';
}

export interface ForwardInfo {
  fromChatName?: string;
  fromChatId?: string;
  originalDate?: string;
  [key: string]: any;
}

export interface MessageMedia {
  type?: 'photo' | 'video' | 'audio' | 'voice' | 'document' | 'sticker' | 'poll' | 'video_note' | string;
  url?: string;
  fileName?: string;
  fileSize?: string | number;
  duration?: number; // for audio/voice in seconds
  waveform?: number[]; // waveform amplitudes (0..100)
  aspectRatio?: number;
  isLottie?: boolean;
  lottieData?: any;
  stickerId?: string;
  packName?: string;
  emoji?: string;
  caption?: string;
  pollData?: {
    question: string;
    options: { id: string; text: string; votes: number; voters: string[] }[];
    totalVotes: number;
    isClosed?: boolean;
    isMultipleAnswers?: boolean;
  };
  [key: string]: any;
}

export interface LinkPreviewData {
  url: string;
  displayUrl: string;
  siteName?: string;
  title: string;
  description?: string;
  image?: string;
  type?: 'telegram_channel' | 'telegram_message' | 'telegram_invite' | 'article' | 'video' | 'website';
  channelUsername?: string;
  memberCount?: number;
}

export interface Message {
  id: string;
  chatId?: string;
  chat_id?: string;
  senderId?: string;
  sender_id?: string;
  senderName?: string;
  sender_name?: string;
  senderAvatar?: string;
  sender_avatar?: string;
  senderPhoto?: string;
  senderUsername?: string;
  senderRole?: 'owner' | 'admin' | 'member' | 'restricted' | 'banned';
  senderRank?: string;
  text: string;
  content?: any;
  timestamp?: string; // e.g. "10:42 AM"
  date?: string | number; // e.g. "2026-08-19" or unix timestamp
  isOutgoing?: boolean;
  is_outgoing?: boolean;
  out?: boolean;
  from_me?: boolean;
  replyToMsgId?: string | number;
  status?: 'sending' | 'sent' | 'delivered' | 'read' | 'error' | 'pending' | string;
  media?: MessageMedia;
  replyTo?: ReplyInfo;
  replyToMessage?: any;
  forwardedFrom?: ForwardInfo;
  forwardFrom?: any;
  reactions?: Reaction[];
  isPinned?: boolean;
  pinned?: boolean;
  is_pinned?: boolean;
  isStarred?: boolean;
  isEdited?: boolean;
  is_system?: boolean;
  system_type?: string;
  translatedText?: string;
  transcribedVoiceText?: string;
  views?: number;
  linkPreview?: LinkPreviewData;
  isSecret?: boolean;
  ttlSeconds?: number;
  expiresAt?: number;
  rawDate?: number;
  epoch?: number;
  [key: string]: any;
}

export interface Chat {
  id: string;
  type: ChatType;
  title: string;
  name?: string;
  username?: string;
  avatar?: string;
  photoUrl?: string;
  isVerified?: boolean;
  isMuted?: boolean;
  is_muted?: boolean;
  isPinned?: boolean;
  is_pinned?: boolean;
  pinned?: boolean;
  isContact?: boolean;
  isOnline?: boolean;
  is_online?: boolean;
  pinnedIndex?: number;
  isArchived?: boolean;
  archived?: boolean;
  isLocked?: boolean;
  adminOnly?: boolean;
  unreadCount: number;
  unread_count?: number;
  isMember?: boolean;
  pinnedMessageId?: string;
  typing_user?: string;
  lastMessage?: {
    id?: string;
    senderName?: string;
    text?: string;
    timestamp?: string;
    date?: string | number;
    isOutgoing?: boolean;
    out?: boolean;
    status?: 'sending' | 'sent' | 'delivered' | 'read' | string;
    mediaType?: string;
    [key: string]: any;
  };
  last_message?: any;
  memberCount?: number;
  onlineCount?: number;
  description?: string;
  inviteLink?: string;
  folderIds?: string[];
  folder_ids?: string[];
  customWallpaper?: string;
  draft?: string;
  draftTimestamp?: string;
  isRestricted?: boolean;
  restrictionReason?: string;
  requiresCaptcha?: boolean;
  captchaQuestion?: string;
  captchaAnswer?: string;
  captchaOptions?: string[];
  isCaptchaSolved?: boolean;
  isReadOnly?: boolean;
  slowModeSeconds?: number;
  // Secret Chat Specifics
  isSecret?: boolean;
  ttlSeconds?: number;
  secretFingerprint?: string;
  [key: string]: any;
}

export interface Folder {
  id: string;
  name: string;
  nameAr: string;
  icon: string;
  chatTypes?: ChatType[];
  includedChatIds?: string[];
  unreadCount?: number;
}

export interface TelegramApiConfig {
  apiId: string;
  apiHash: string;
  dcId: number;
  dcIp: string;
  port: number;
  connectionStatus: 'connected' | 'connecting' | 'disconnected';
  sessionString: string;
  mtprotoVersion: string;
  pingMs: number;
}

export type SettingsSubPage =
  | 'main'
  | 'account'
  | 'plus_settings'
  | 'plus_general'
  | 'plus_chats'
  | 'plus_stories'
  | 'plus_messages'
  | 'plus_topics'
  | 'plus_drawer'
  | 'plus_profile'
  | 'plus_notifications'
  | 'plus_privacy'
  | 'plus_media'
  | 'plus_downloads'
  | 'plus_ads'
  | 'theme_coloring'
  | 'chat_settings'
  | 'privacy_security'
  | 'privacy_control'
  | 'two_step_verification'
  | 'passcode_lock'
  | 'auto_delete'
  | 'sessions'
  | 'blocked_users'
  | 'notifications_sounds'
  | 'data_storage'
  | 'folders'
  | 'devices'
  | 'power_saving'
  | 'language'
  | 'themes_browser'
  | 'faq'
  | 'features'
  | 'apk_installer'
  | 'support_group'
  | 'stories'
  | 'premium';

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
  topicsAsTabs: boolean;
  autoOpenGeneralTopic: boolean;
  unreadTopicBadges: boolean;
  quickTopicSearch: boolean;
  lastTopicMessagePreview: boolean;

  // 6. Navigation Drawer (درج التصفح)
  drawerShowNightMode: boolean;
  drawerShowSavedMessages: boolean;
  drawerShowCalls: boolean;
  drawerShowContacts: boolean;
  drawerShowPlusSettings: boolean;
  drawerShowAccounts: boolean;
  drawerHeaderStyle: 'standard' | 'minimal' | 'custom';

  // 7. Profile (الملف الشخصي)
  profileShowUserId: boolean;
  profileCopyIdOnTap: boolean;
  profileShowCommonGroups: boolean;
  profileHidePhone: boolean;
  profileQuickActions: boolean;

  // 8. Notifications (الإشعارات)
  inAppNotificationStyle: 'banner' | 'pill' | 'silent';
  repeatUnreadAlerts: 'off' | '5min' | '15min';
  customPrivateTone: string;
  customGroupTone: string;
  vipPriorityAlerts: boolean;
  filterSpamAlerts: boolean;

  // 9. Privacy & Security (الخصوصية والأمان)
  ghostMode: boolean;
  hideOnlineStatus: boolean;
  hideReadReceipts: boolean;
  hideTypingIndicator: boolean;
  antiDeleteMessages: boolean;
  antiEditMessages: boolean;
  appLockPasscode: string;
  isAppLockEnabled: boolean;
  biometricsEnabled: boolean;
  hiddenChatsLocked: boolean;

  // 10. Shared Media (الوسائط المتبادلة)
  defaultMediaTab: 'photos' | 'videos' | 'files' | 'audio' | 'links' | 'voice';
  gridColumnsCount: number;
  highResThumbnailPreview: boolean;
  pipFloatingVideo: boolean;
  autoPauseAudioOnVideo: boolean;
  customMediaPath: string;

  // 11. Downloads (التحميلات)
  autoDownloadWifi: boolean;
  autoDownloadCellular: boolean;
  downloadBooster: boolean;
  maxConcurrentDownloads: number;
  downloadFinishSound: boolean;
  autoResumeDownloads: boolean;

  // 12. Ads (الإعلانات)
  blockSponsoredMessages: boolean;
  hidePromotedChannels: boolean;
  blockBotAds: boolean;
  disablePromoAlerts: boolean;

  [key: string]: any;
}

export interface AppSettings {
  theme: 'dark' | 'light' | 'night' | 'day';
  accentColor: string;
  fontSize: number; // 12 .. 30
  language: 'ar' | 'en';
  sendByEnter: boolean;
  soundEffects: boolean;
  autoDownloadMedia: boolean;
  chatWallpaper: string;
  bubbleCornerRadius?: number;
  chatListViewMode?: 'two_lines' | 'three_lines';
  appIcon?: string;
  autoNightMode?: boolean;
  inAppBrowser?: boolean;
  powerSavingThreshold?: number;
  enableAnimations?: boolean;
  swipeAction?: string;
  showTranslateButton?: boolean;
  inAppSounds?: boolean;
  inAppVibrate?: boolean;
  inAppPreview?: boolean;
  inChatSounds?: boolean;
  inAppPop?: boolean;
  autoDownloadMobile?: boolean;
  autoDownloadWifi?: boolean;
  autoDownloadRoaming?: boolean;
  streamingEnabled?: boolean;
  callDataSaving?: string;
  plusThemeEnabled?: boolean;
  useSQLiteMMAP?: boolean;
  biometricLock?: boolean;
}

export interface ActiveCall {
  chatId: string;
  chatTitle: string;
  chatAvatar: string;
  isVideo: boolean;
  isMuted: boolean;
  isCameraOff: boolean;
  isScreenSharing?: boolean;
  isNoiseSuppressed?: boolean;
  duration: number;
  status: 'calling' | 'connected' | 'ended';
  encryptionEmojis: [string, string, string, string];
  audioLevel?: number;
}

export interface ToastItem {
  id: string;
  text: string;
  icon?: string;
}

export interface ChatContextMenu {
  chatId: string;
  x: number;
  y: number;
}

export interface MessageContextMenu {
  message: Message;
  x: number;
  y: number;
}

export type NotificationCategory =
  | 'message'
  | 'channel_post'
  | 'mention'
  | 'reply'
  | 'call'
  | 'system_security'
  | 'reaction'
  | 'pinned'
  | 'keyword_alert';

export interface InAppNotification {
  id: string;
  category: NotificationCategory;
  title: string;
  body: string;
  avatar?: string;
  chatId?: string;
  chatTitle?: string;
  chatUsername?: string;
  messageId?: string;
  senderId?: string;
  senderName?: string;
  senderUsername?: string;
  timestamp: string;
  isSilent?: boolean;
  isPinned?: boolean;
  replyAction?: boolean;
  keyword?: string;
  messageText?: string;
}

export interface UserAccount {
  id: string;
  user: User;
  settings: AppSettings;
  chats: Chat[];
  messages: Record<string, Message[]>;
  unreadCount?: number;
  isActive?: boolean;
  sessionString?: string;
}

export interface CapturedLink {
  id: string;
  url: string;
  sourceChatId?: string;
  source_chat_id?: string;
  sourceChatTitle?: string;
  source_chat?: string;
  source_link?: string;
  sourceSenderName?: string;
  sender?: string;
  detectedAt?: string;
  detected_at?: string;
  type?: 'telegram_channel' | 'telegram_group' | 'telegram_invite' | 'external';
  extractedTitle?: string;
  chat_title?: string;
  memberCount?: number;
  joined: boolean;
  joinedAt?: string;
  autoJoined?: boolean;
  status: 'valid' | 'invalid' | 'joined' | 'already' | 'pending' | 'failed' | 'joining' | 'already_member' | 'expired';
  status_text?: string;
  join_status?: string;
  username?: string;
  creation_date?: string;
  country?: string;
}

// 1. Sender & Scheduler Types
export type ProtectionMode = 'salam' | 'skip' | 'smart_clean' | 'permanent_clean' | 'disabled';

export interface SenderBatch {
  id: string;
  text: string;
  images: string[];
  targetChats: { id: string; title: string; type: ChatType; status: 'sent' | 'failed' | 'skipped' | 'protected'; messageId?: string; error?: string }[];
  protectionMode: ProtectionMode;
  isScheduled: boolean;
  intervalMinutes?: number;
  durationHours?: number;
  createdAt: string;
  sentAt: string;
  totalSuccess: number;
  totalFailed: number;
  status: 'completed' | 'running' | 'paused' | 'stopped';
}

// 2. Monitor Types
export interface MonitorConfig {
  isEnabled: boolean;
  keywords: string[];
  sendAlertsToSavedMessages: boolean;
  browserPushAlerts: boolean;
  intervalMinutes?: number;
  durationHours?: number;
  startedAt?: string;
}

export interface MonitorAlert {
  id: string;
  keyword: string;
  sourceChatId: string;
  sourceChatTitle: string;
  senderName: string;
  messageText: string;
  timestamp: string;
}

// 3. My Messages (Batch Log)
export interface MyMessagesBatch {
  id: string;
  text: string;
  hasImages: boolean;
  imagesCount: number;
  groupsCount: number;
  targets: { chatId: string; chatTitle: string; messageId: string }[];
  date: string;
  timestamp: string;
}

// 4. Auto Joiner Advanced
export interface AutoJoinerTask {
  id: string;
  url: string;
  type: 'public' | 'private' | 'username';
  extractedFromText?: string;
  status: 'pending' | 'joining' | 'joined' | 'already_member' | 'invalid' | 'banned' | 'rate_limited';
  errorReason?: string;
  processedAt?: string;
}

// 5. Auto Responder
export interface AutoReplyRule {
  id: string;
  keyword?: string;
  replyText?: string;
  trigger?: string;
  match?: string;
  response?: string;
  reply?: string;
  matchType?: 'exact' | 'contains' | 'regex';
  scope?: 'all' | 'private' | 'groups';
  isEnabled?: boolean;
  enabled?: boolean;
  isActive?: boolean;
  timesTriggered?: number;
  used_count?: number;
  lastTriggeredAt?: string;
  last_used?: string | number;
}

// 6. Smart AI Learn (Groq LLM)
export interface SmartAiService {
  id: string;
  name: string;
  description: string;
  keywords: string[];
}

export interface SmartAiPattern {
  id: string;
  triggerContext: string;
  recommendedReply: string;
  learnedDate: string;
  isAccepted: boolean;
}

// 7. Live Link Discover & Instant Auto-Join
export interface LiveDiscoveredLink {
  id: string;
  url: string;
  sourceChatTitle: string;
  sourceChatId: string;
  senderName: string;
  timestamp: string;
  status: 'pending' | 'joining' | 'joined' | 'failed' | 'already_member' | 'expired';
  failReason?: string;
  autoJoined: boolean;
}

// 8. Protocol Buffers & Diagnostics Types
export { GoogleProtobuf } from './core/ProtobufCodec';

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

export interface ChatFolder {
  id: number | string;
  title: string;
  icon?: string;
  emoticon?: string;
  color?: number | string;
  filterFlags?: number;
  includeGroups?: boolean;
  includeChannels?: boolean;
  includeBots?: boolean;
  includeContacts?: boolean;
  includeNonContacts?: boolean;
  excludeMuted?: boolean;
  excludeRead?: boolean;
  excludeArchived?: boolean;
  includedChatIds: string[];
  excludedChatIds?: string[];
  pinnedChatIds?: string[];
  chat_ids?: (string | number)[];
  [key: string]: any;
}

export type UserProfile = User;
export type ChatItem = Chat;
export type MessageItem = Message;
export type TelegramUser = User;
export type TelegramDialog = Chat;
export type TelegramMessage = Message;
export type TelegramStory = Story;

export interface ActiveSession {
  id: string;
  device?: string;
  platform?: string;
  ip?: string;
  location?: string;
  lastActive?: string | number;
  isCurrent?: boolean;
  [key: string]: any;
}

export type SystemActionType = 'pin' | 'unpin' | 'clear' | 'delete' | 'mute' | 'unmute' | 'archive' | 'unarchive' | string;
export type AppTheme = 'dark' | 'light' | 'night' | 'day' | string;

export interface SystemUpdateInfo {
  has_update?: boolean;
  hasUpdate?: boolean;
  current?: string;
  latest?: string;
  message?: string;
  [key: string]: any;
}
export type SystemUpdateStatus = SystemUpdateInfo | string | any;

export interface ActiveAudioTrack {
  id?: string;
  title?: string;
  subtitle?: string;
  performer?: string;
  audioUrl?: string;
  url?: string;
  duration?: number;
  currentTime?: number;
  isPlaying?: boolean;
  chatId?: string;
  messageId?: string;
  [key: string]: any;
}

export interface SystemMessageData {
  id: string;
  title: string;
  body: string;
  message?: string;
  chat_id?: string;
  date: string;
  isRead: boolean;
}

export interface ResolvedTelegramLink {
  type: 'channel' | 'group' | 'bot' | 'user' | 'message' | 'invite' | 'sticker' | 'proxy';
  username?: string;
  chatId?: string;
  messageId?: string;
  inviteHash?: string;
  hash?: string;
  id?: string;
  title?: string;
  resolved: boolean;
  [key: string]: any;
}

export interface TelegramMedia {
  type?: 'photo' | 'video' | 'document' | 'audio' | 'voice' | 'sticker' | 'webpage' | 'contact' | string;
  url?: string;
  fileName?: string;
  fileSize?: string | number;
  size?: number;
  duration?: number;
  caption?: string;
  emoji?: string;
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
  id?: string;
  accountNum?: number;
  user: User;
  sessionString: string;
  isActive: boolean;
  addedAt?: number;
}

export interface ScheduledMessage {
  id: string;
  chatId: string;
  chatTitle?: string;
  text: string;
  scheduledTime: number;
  isSent?: boolean;
  status?: string;
  createdAt?: number;
  [key: string]: any;
}

export interface AutoResponderRule {
  id: string;
  name?: string;
  trigger?: string;
  response?: string;
  triggerKeyword?: string;
  responseText?: string;
  responseTemplate?: string;
  isActive?: boolean;
  enabled?: boolean;
  onlyPrivate?: boolean;
}

export interface AutoForwardRule {
  id: string;
  sourceChatId: string;
  targetChatId: string;
  isActive?: boolean;
  enabled?: boolean;
  isEnabled?: boolean;
  sourceChatTitle?: string;
  targetChatTitle?: string;
  removeQuote?: boolean;
  [key: string]: any;
}

export interface SendMonitorConfig {
  id?: string;
  enabled?: boolean;
  targetChatIds?: string[];
  keywords?: string[];
  replyMessage?: string;
  actionType?: string;
  broadcastIntervalMinutes?: number;
  antiFloodDelaySeconds?: number;
  sendType?: string;
  repeatIntervalMinutes?: number;
  scheduledTime?: string;
  protectedGroupAction?: string;
  smartSalamWaitMinutes?: number;
  smartSalamRequiredMessages?: number;
  intervalSeconds?: number;
  maxRetries?: number;
  active?: boolean;
}

export interface SendMonitorLog {
  id: string;
  chatId: string;
  chatTitle?: string;
  matchedKeyword?: string;
  triggerKeyword?: string;
  actionTaken?: string;
  sentMessageText?: string;
  timestamp?: string | number;
  sentTime?: number;
  status: 'sent' | 'failed' | 'queued' | 'skipped_protection' | 'waiting_salam' | 'success' | 'skipped' | string;
  statusLabel?: string;
  antiFloodDelay?: number;
  error?: string;
  messageText?: string;
  details?: string | any;
}

export interface SavedMessageTemplate {
  id: string;
  title: string;
  category?: string;
  content: string;
  text?: string;
  isPinned?: boolean;
  tags: string[];
  createdAt?: number;
}

export interface AutoJoinConfig {
  delaySeconds?: number;
  randomDelayRange?: number;
  autoStartOnPaste?: boolean;
  maxJoinsPerBatch?: number;
  delayBetweenJoinsMs?: number;
  maxJoinsPerDay?: number;
  autoArchiveJoined?: boolean;
}

export interface AutoJoinQueueItem {
  id: string;
  link?: string;
  linkOrUsername?: string;
  rawInput?: string;
  type?: 'public' | 'private' | 'bot' | 'proxy';
  status: 'queued' | 'joining' | 'joined' | 'failed' | 'skipped' | 'already_joined' | 'already_member' | 'pending' | string;
  error?: string;
  chatTitle?: string;
  title?: string;
  timestamp?: string;
  addedAt?: number;
  joinedAt?: number;
  [key: string]: any;
}

export interface SavedTelegramLink {
  id: string;
  title: string;
  link?: string;
  url?: string;
  username?: string;
  type?: string;
  category?: string;
  tags?: string[];
  notes?: string;
  isJoined?: boolean;
  savedAt?: number;
  addedAt?: string;
}

export interface AcademicExtractConfig {
  sourceChatIds?: string[];
  fileTypes?: string[];
  searchQuery?: string;
  summarizeWithAI?: boolean;
  extractPdfs?: boolean;
  extractSlides?: boolean;
  saveToSavedMessages?: boolean;
}

export interface AcademicResourceItem {
  id: string;
  title: string;
  fileName?: string;
  fileType?: string;
  fileSize?: number;
  chatTitle?: string;
  chatId?: string;
  messageId?: number;
  date?: number | string;
  summary?: string;
  type?: string;
  fileUrl?: string;
}

export interface LinkMonitorConfig {
  enabled?: boolean;
  monitoredChatIds?: string[];
  autoJoinCapturedTelegramLinks?: boolean;
  autoSaveToLinkBank?: boolean;
  soundAlert?: boolean;
  filterKeywords?: string[];
  autoJoin?: boolean;
  notifyOnDiscovery?: boolean;
}

export interface CapturedLinkItem {
  id: string;
  link: string;
  url?: string;
  type?: string;
  source_title?: string;
  status_text?: string;
  action_taken?: string;
  timestamp?: string | number;
  messageText?: string;
  sourceChat?: string;
  chatTitle?: string;
  senderName?: string;
  detectedAt?: number | string;
  autoJoined?: boolean;
  savedToBank?: boolean;
  [key: string]: any;
}

export type LiveCapturedLinkItem = CapturedLinkItem;
export type SavedLink = SavedTelegramLink;

export type ScrapedLinkType = 'telegram_channel' | 'telegram_group' | 'telegram_bot' | 'telegram_user' | 'telegram_invite' | 'external_url' | 'proxy';

export type AiToneId = 'formal' | 'friendly' | 'academic' | 'persuasive' | 'concise' | 'urgent' | 'creative' | 'bullet_summary' | 'neutral' | 'casual' | 'poetic' | 'humorous' | 'pirate';

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

export type SanitizeMode = 'none' | 'clean_spaces' | 'remove_emojis' | 'remove_links' | 'full' | 'salam';
export type SendType = 'broadcast' | 'direct' | 'monitor' | 'reply' | 'forward' | 'manual';

export interface WhatsAppSettings {
  autoReconnect?: boolean;
  readReceipts?: boolean;
  alwaysOnline?: boolean;
  keepAliveInterval?: number;
  watch_words?: string[];
  sanitize_mode?: SanitizeMode;
  send_type?: SendType;
  interval_seconds?: number;
  schedule_duration_hours?: number;
  message?: string;
  [key: string]: any;
}

export interface AcademicAnalysisResult {
  title?: string;
  summary: string;
  topics?: string[];
  keyPoints?: string[];
  examQuestions?: string[];
  references?: string[];
  stats?: any;
  histogram_bars?: any[];
  [key: string]: any;
}

export type ProtectedGroupAction = 'skip' | 'salam_first' | 'notify_only' | 'force_send' | 'smart_salam' | string;
export type SendScheduleType = 'immediate' | 'interval' | 'scheduled' | 'cron' | string;

export interface TelegramSearchResult {
  id?: string;
  title?: string;
  username?: string;
  link?: string;
  memberCount?: number;
  description?: string;
  type?: string;
  [key: string]: any;
}

export interface TelegramAccount {
  id: string;
  phone?: string | any;
  name?: string;
  first_name?: string;
  username?: string;
  avatar?: string;
  isActive?: boolean;
  is_active?: boolean;
  status?: string;
  session_name?: string | any;
  has_2fa?: boolean;
  proxy?: AccountProxyConfig | any;
  sessionString?: string;
  addedAt?: number | string;
  stats?: any;
  flood_wait_seconds?: number;
  [key: string]: any;
}

export interface AccountProxyConfig {
  id?: string;
  host?: string;
  port?: number;
  username?: string;
  password?: string;
  type?: 'socks5' | 'http' | 'mtproto' | string;
  secret?: string;
  enabled?: boolean;
  [key: string]: any;
}

export interface MultiAccountBroadcastResult {
  successCount?: number;
  failureCount?: number;
  totalChats?: number;
  durationMs?: number;
  status?: string;
  session_name?: string;
  phone?: string;
  error?: string;
  [key: string]: any;
}

export interface AutoJoinItem {
  id: string;
  link: string;
  status: 'pending' | 'joining' | 'joined' | 'failed';
  title?: string;
  error?: string;
}

export interface AutoJoinProgressEvent {
  total?: number;
  completed?: number;
  successful?: number;
  failed?: number;
  currentLink?: string;
  counts?: any;
  url?: string;
  reason?: string;
  [key: string]: any;
}

export interface SentBatch {
  id: string;
  title?: string;
  totalMessages?: number;
  sentCount?: number;
  failedCount?: number;
  createdAt?: number | string;
  status?: 'completed' | 'in_progress' | 'failed' | 'paused' | string;
  text?: string;
  edited_at?: any;
  sent_at?: any;
  group_count?: number;
  sent_count?: number;
  has_media?: boolean;
  [key: string]: any;
}

export interface LearningService {
  id?: string;
  keyword?: string;
  keywords?: string[];
  response?: string;
  description?: string;
  confidence?: number;
  category?: string;
  price_range?: string;
  time_range?: string;
  [key: string]: any;
}

export interface UnknownRequest {
  id: string;
  text: string;
  count: number;
  firstSeen: number | string;
  lastSeen: number | string;
  resolved?: boolean;
}

export interface ScrapedLinkItem {
  id: string;
  url: string;
  title?: string;
  type: string;
  sourceChat?: string;
  source_title?: string;
  sender_name?: string;
  timestamp?: string | number;
  discoveredAt?: number | string;
  status?: string;
}

export type ScrapeTimeRange = 'all' | 'today' | 'week' | 'month' | 'custom' | '10_days' | '30_days' | '24_hours';

export interface LinkScrapeProgressEvent {
  totalChatsScanned?: number;
  total_chats?: number;
  scanned_chats?: number;
  found_total?: number;
  current_chat_title?: string;
  new_link?: any;
  linksFound?: number;
  currentChat?: string;
  isFinished?: boolean;
  [key: string]: any;
}

export interface PiPVideoTrack {
  id?: string;
  url: string;
  title?: string;
  chatId?: string;
  messageId?: string;
  currentTime?: number;
  duration?: number;
  isPlaying?: boolean;
  [key: string]: any;
}

export interface PasscodeSettings {
  enabled?: boolean;
  isLocked?: boolean;
  passcode?: string;
  timeoutMinutes?: number;
  autoLock?: number;
  useBiometrics?: boolean;
  [key: string]: any;
}

export interface InlineKeyboardButton {
  text: string;
  url?: string;
  callback_data?: string;
  switch_inline_query?: string;
  [key: string]: any;
}

export interface ActivityLog {
  id: string;
  type?: string;
  action?: string;
  message?: string;
  timestamp?: string | number;
  time?: string;
  status?: string;
  details?: any;
  [key: string]: any;
}

export interface ChatMember {
  id: string;
  userId?: string;
  name?: string;
  first_name?: string;
  last_name?: string;
  username?: string;
  avatar?: string;
  role?: 'creator' | 'admin' | 'member' | 'restricted' | 'banned' | string;
  status?: string;
  isOnline?: boolean;
  joinedDate?: string | number;
  [key: string]: any;
}

export interface ChatPeekData {
  chat: Chat;
  messages?: Message[];
  x?: number;
  y?: number;
  [key: string]: any;
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

export interface StorageCategoryStats {
  name?: string;
  bytes?: number;
  category?: string;
  label?: string;
  sizeBytes?: number;
  itemCount?: number;
  count?: number;
  color?: string;
  [key: string]: any;
}

export interface StorageStats {
  totalBytes: number;
  categories: StorageCategoryStats[];
  cachedFilesCount?: number;
  databaseSize?: number;
  [key: string]: any;
}

export interface TelegramNotification {
  id: string;
  title: string;
  body: string;
  avatar?: string;
  chatId?: string;
  timestamp?: string | number;
  [key: string]: any;
}

export type BotInlineButton = InlineKeyboardButton;
export type MessageReaction = Reaction;




