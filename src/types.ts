export interface TelegramAccount {
  id: string;
  sessionToken: string;
  user: TelegramUser;
  isLoggedIn: boolean;
  isDemo?: boolean;
  addedAt: number;
}

export interface TelegramBotCommand {
  command: string;
  description: string;
}

export interface TelegramBotMenuButton {
  type: 'commands' | 'web_app' | 'default';
  text?: string;
  url?: string;
}

export interface TelegramInlineButton {
  text: string;
  url?: string;
  callbackData?: string;
  webApp?: {
    url: string;
  };
  switchInlineQuery?: string;
  switchInlineQueryCurrentChat?: string;
  buy?: boolean;
}

export interface TelegramReplyButton {
  text: string;
  requestContact?: boolean;
  requestLocation?: boolean;
  webApp?: {
    url: string;
  };
}

export interface TelegramReplyMarkup {
  type: 'inline' | 'keyboard' | 'remove';
  inlineKeyboard?: TelegramInlineButton[][];
  keyboard?: TelegramReplyButton[][];
  resizeKeyboard?: boolean;
  oneTimeKeyboard?: boolean;
  isPersistent?: boolean;
}

export interface TelegramInlineQueryResult {
  id: string;
  type: 'article' | 'photo' | 'gif' | 'video' | 'sticker';
  title: string;
  description?: string;
  thumbUrl?: string;
  url?: string;
  contentText: string;
  caption?: string;
  replyMarkup?: TelegramReplyMarkup;
}

export interface TelegramUser {
  id: string;
  firstName: string;
  lastName?: string;
  username?: string;
  phone?: string;
  photoUrl?: string;
  bio?: string;
  isBot?: boolean;
  isVerified?: boolean;
  isPremium?: boolean;
  starsBalance?: number;
  status?: 'online' | 'offline' | 'recently';
  botInfo?: {
    description?: string;
    about?: string;
    commands?: TelegramBotCommand[];
    menuButton?: TelegramBotMenuButton;
  };
}

export type ChatType = 'private' | 'group' | 'supergroup' | 'channel' | 'bot' | 'saved';

export interface TelegramReaction {
  emoji: string;
  count: number;
  users?: string[];
  userReacted?: boolean;
}

export interface TelegramMessageEntity {
  type: 'MessageEntitySpoiler' | 'MessageEntityBlockquote' | 'MessageEntityCustomEmoji' | 'MessageEntityBold' | 'MessageEntityItalic' | 'MessageEntityCode' | 'MessageEntityUrl';
  offset: number;
  length: number;
  documentId?: string; // For Custom Emojis
  collapsed?: boolean; // For collapsible blockquotes
}

export interface TelegramForumTopic {
  id: number;
  title: string;
  iconColor?: string;
  iconEmoji?: string;
  unreadCount?: number;
  isClosed?: boolean;
  isPinned?: boolean;
  isEdited?: boolean;
  lastMessage?: {
    text: string;
    timestamp: number;
    senderName?: string;
  };
}

export interface TelegramMedia {
  type: 'photo' | 'video' | 'audio' | 'voice' | 'document' | 'tgs_sticker';
  url?: string;
  title?: string;
  fileName?: string;
  fileSize?: string;
  duration?: number; // for audio/voice in seconds
  thumbnailUrl?: string;
  isTgs?: boolean;
  tgsUrl?: string;
  lottieData?: any;
}

export interface TelegramMessage {
  id: string;
  chatId: string;
  senderId: string;
  senderName: string;
  senderAvatar?: string;
  text: string;
  timestamp: number;
  isOut: boolean;
  status: 'sending' | 'sent' | 'read' | 'error';
  seenBy?: string[];
  replyTo?: {
    id: string;
    senderName: string;
    text: string;
  };
  media?: TelegramMedia;
  reactions?: TelegramReaction[];
  isPinned?: boolean;
  isEdited?: boolean;
  isForwarded?: boolean;
  forwardedFrom?: string;
  replyMarkup?: TelegramReplyMarkup;
  replyToTopId?: number; // Forum topic identifier (reply_to_top_id)
  topicId?: number;
  entities?: TelegramMessageEntity[];
  scheduledTime?: number;
  starGift?: {
    amount: number;
    message?: string;
    from: string;
  };
  starsGift?: {
    amount: number;
    message?: string;
    from: string;
  };
}

export interface TelegramChat {
  id: string;
  title: string;
  username?: string;
  type: ChatType;
  avatarUrl?: string;
  avatarColor?: string;
  isBot?: boolean;
  isForum?: boolean;
  topics?: TelegramForumTopic[];
  starsEarned?: number;
  botInfo?: {
    description?: string;
    about?: string;
    commands?: TelegramBotCommand[];
    menuButton?: TelegramBotMenuButton;
  };
  lastMessage?: {
    text: string;
    timestamp: number;
    senderId?: string;
    senderName?: string;
    senderAvatar?: string;
    isOut?: boolean;
    mediaType?: string;
  };
  unreadCount: number;
  isPinned?: boolean;
  isArchived?: boolean;
  isMuted?: boolean;
  isOnline?: boolean;
  isJoined?: boolean;
  isVerified?: boolean;
  participantsCount?: number;
  membersCount?: number;
  description?: string;
  inviteLink?: string;
  canSendMessages?: boolean;
  isBroadcast?: boolean;
  restrictionReason?: Array<{ platform: string; reason: string; text: string }>;
  peerSettings?: {
    reportSpam?: boolean;
    addContact?: boolean;
    blockContact?: boolean;
    shareContact?: boolean;
    needReq?: boolean;
  };
  members?: Array<{
    id: string;
    name: string;
    role?: 'owner' | 'admin' | 'member';
    isOnline?: boolean;
    avatarUrl?: string;
    avatarColor?: string;
  }>;
}

export type ChatFolder = 'all' | 'personal' | 'channels' | 'groups' | 'bots' | 'unread';

export interface TelegramThemeConfig {
  isDark: boolean;
  accentColor: string; // hex code
  fontSize: 'sm' | 'md' | 'lg';
  language: 'ar' | 'en';
  wallpaper?: string;
  animations?: boolean;
  sendOnEnter?: boolean;
}

export interface TypingStatus {
  chatId: string;
  userName?: string;
  action?: 'typing' | 'recording' | 'uploading';
  startedAt: number;
  expiresAt: number;
}

export interface TelegramStoryItem {
  id: string;
  peerId: string;
  date: number;
  caption?: string;
  mediaUrl?: string;
  mediaType?: 'photo' | 'video';
  isViewed?: boolean;
  reactionsCount?: number;
  userReaction?: string;
}

export interface TelegramPeerStories {
  peerId: string;
  peerTitle: string;
  peerAvatar?: string;
  hasUnread: boolean;
  maxReadId?: string;
  stories: TelegramStoryItem[];
}

export interface OcrWordBox {
  text: string;
  bbox: {
    x0: number;
    y0: number;
    x1: number;
    y1: number;
  };
  confidence: number;
}

export interface OcrResult {
  fullText: string;
  words: OcrWordBox[];
  imageWidth: number;
  imageHeight: number;
}

export interface ScheduledMessage {
  id: string;
  chatId: string;
  text: string;
  scheduledTime: number; // Unix timestamp in ms
  replyTo?: {
    id: string;
    senderName: string;
    text: string;
  };
  media?: TelegramMedia;
  topicId?: number;
  entities?: TelegramMessageEntity[];
}

export interface TelegramStarGift {
  id: string;
  targetChatId: string;
  targetTitle: string;
  starsAmount: number;
  message?: string;
  senderName: string;
  timestamp: number;
}

// ==========================================
// FCM Push Notifications & Diagnostics
// ==========================================
export interface FcmPushPacket {
  id: string;
  timestamp: string;
  receivedAt: number;
  dialog_id: string;
  sender_id: string;
  sender_name: string;
  msg_id: string;
  title: string;
  body: string;
  sound?: string;
  badge?: number;
  rawPayload?: any;
  status: "alerted" | "suppressed_active_dialog" | "muted" | "background_synced" | "error" | string;
  account_id: number | string;
  user_id: string;
  routingDecision: string;
}

export interface FcmDiagnosticInfo {
  status: "listening" | "unsupported" | "registered" | "connected" | "permission_denied" | "error" | string;
  token: string | null;
  endpoint?: string;
  lastHeartbeat: string;
  activeAccountId: number | string;
  activeUserId: string;
  activeDialogId: string | null;
  registrationId: string;
  lastReceivedPacket: FcmPushPacket | null;
  history: FcmPushPacket[];
  isSubscribedToPush: boolean;
  permissionState: NotificationPermission | "unsupported";
}

// ==========================================
// AI Tone Selection & Assistants
// ==========================================
export type AiToneId = "neutral" | "formal" | "casual" | "concise" | "friendly" | "poetic" | string;

export interface AiComposeTone {
  id: AiToneId;
  name: string;
  nameAr: string;
  icon: string;
  description: string;
  descriptionAr: string;
}

// ==========================================
// Contact Birthdays
// ==========================================
export interface ContactBirthday {
  userId: string;
  name: string;
  avatar: string;
  username: string;
  birthDate: string;
  isToday: boolean;
  daysRemaining: number;
  age: number;
  hasCelebrated?: boolean;
}

// ==========================================
// Cache Usage By Chats
// ==========================================
export interface ChatCacheUsageInfo {
  chatId: string;
  chatTitle: string;
  chatAvatar: string;
  photosBytes: number;
  videosBytes: number;
  audioBytes: number;
  documentsBytes: number;
  totalBytes: number;
  keepMediaMode: "3_days" | "1_week" | "1_month" | "forever" | string;
}

// ==========================================
// Channel Boosts
// ==========================================
export interface ChannelBoostPerk {
  level: number;
  title: string;
  titleAr: string;
  description: string;
  isUnlocked: boolean;
}

export interface ChannelBoostData {
  chatId: string;
  currentLevel: number;
  currentBoosts: number;
  boostsToNextLevel: number;
  myBoostsCount: number;
  canBoost: boolean;
  boostUrl: string;
  unlockedPerks: ChannelBoostPerk[];
}

// ==========================================
// Fact-Checking
// ==========================================
export interface MessageFactCheck {
  messageId: string;
  chatId: string;
  country: string;
  organization: string;
  organizationLogo?: string;
  text: string;
  sourceUrl: string;
  checkedAt: string;
  isExpanded: boolean;
}

// ==========================================
// Star Gifts & Auctions
// ==========================================
export interface StarGiftItem {
  id: string;
  title: string;
  emoji: string;
  starsPrice: number;
  isLimited?: boolean;
  totalAvailable?: number;
  soldCount?: number;
  badge?: string;
}

export interface GiftAuctionBid {
  bidId: string;
  userId: string;
  userName: string;
  userAvatar: string;
  amountStars: number;
  timestamp: string;
}

export interface GiftAuctionAttribute {
  key: string;
  value: string;
  rarityPercentage: number;
}

export interface GiftAuctionItem {
  id: string;
  giftId: string;
  title: string;
  symbol: string;
  currentBidStars: number;
  highestBidderId: string;
  highestBidderName: string;
  highestBidderAvatar: string;
  minNextBid: number;
  endsAt: number;
  totalBidsCount: number;
  recentBids: GiftAuctionBid[];
  attributes: GiftAuctionAttribute[];
}

// ==========================================
// Member Join Requests
// ==========================================
export interface MemberJoinRequestItem {
  id: string;
  chatId: string;
  chatTitle: string;
  userId: string;
  userName: string;
  userAvatar: string;
  userBio?: string;
  requestedAt: string;
  status: "pending" | "accepted" | "approved" | "declined" | "dismissed";
}

// ==========================================
// Plus Configuration Types
// ==========================================
export interface PlusConfig {
  fontFamily: string;
  keepScreenOn: boolean;
  proximitySensor: boolean;
  useExternalBrowser: boolean;
  hapticFeedback: boolean;
  bigEmojis: boolean;
  showDirectShare: boolean;
  cacheLimitGb: number;

  tabsEnabled: boolean;
  tabsPosition: "top" | "bottom";
  showUnreadTabsCounter: boolean;
  hideMutedTabs: boolean;
  showOnlineStatusDot: boolean;
  doubleTapAction: "reply" | "reaction" | "copy" | "pin";
  chatSwipeAction: "archive" | "mute" | "delete" | "pin" | "read";
  confirmBeforeCall: boolean;

  hideStoriesBar: boolean;
  stealthModeStories: boolean;
  autoSaveStories: boolean;
  highQualityPlayback: boolean;
  storySpeed: "1x" | "1.5x" | "2x";
  storyExpirationAlert: boolean;

  forwardWithoutQuote: boolean;
  showUserIdOnMessages: boolean;
  showExactSeconds: boolean;
  showEditedHistory: boolean;
  confirmVoiceNotes: boolean;
  confirmStickers: boolean;
  autoTranslateIncoming: boolean;
  translationProvider: "telegram" | "google" | "deepl";

  topicsAsTabs?: boolean;
  autoOpenGeneralTopic?: boolean;
  unreadTopicBadges?: boolean;
  quickTopicSearch?: boolean;
  lastTopicMessagePreview?: boolean;

  drawerShowNightMode?: boolean;
  drawerShowSavedMessages?: boolean;
  drawerShowCalls?: boolean;
  drawerShowContacts?: boolean;
  drawerShowPlusSettings?: boolean;
  drawerShowAccounts?: boolean;
  drawerHeaderStyle?: "standard" | "minimal" | "custom";

  profileShowUserId?: boolean;
  profileCopyIdOnTap?: boolean;
  profileShowCommonGroups?: boolean;
  profileHidePhone?: boolean;
  profileQuickActions?: boolean;

  inAppNotificationStyle?: "banner" | "pill" | "silent";
  repeatUnreadAlerts?: "off" | "5min" | "15min";
  customPrivateTone?: string;
  customGroupTone?: string;
  vipPriorityAlerts?: boolean;
  filterSpamAlerts?: boolean;

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

  defaultMediaTab?: "photos" | "videos" | "files" | "audio" | "links" | "voice";
  gridColumnsCount?: number;
  highResThumbnailPreview?: boolean;
  pipFloatingVideo?: boolean;
  autoPauseAudioOnVideo?: boolean;
  customMediaPath?: string;

  autoDownloadWifi?: boolean;
  autoDownloadCellular?: boolean;
  downloadBooster?: boolean;
  maxConcurrentDownloads?: number;
  downloadFinishSound?: boolean;
  autoResumeDownloads?: boolean;

  blockSponsoredMessages?: boolean;
  hidePromotedChannels?: boolean;
  blockBotAds?: boolean;
  disablePromoAlerts?: boolean;
  cleanChatBackground?: boolean;
}

// ==========================================
// App Update State
// ==========================================
export interface AppUpdateState {
  hasUpdate: boolean;
  updateCount: number;
  showUpdateNotification: boolean;
  isUpdating: boolean;
  commitHash?: string;
  fullCommitHash?: string;
  commitMessage?: string;
  commitAuthor?: string;
  commitDate?: string;
  currentCommitHash?: string;
  commits?: Array<{
    sha: string;
    message: string;
    author: string;
    date: string;
  }>;
  error?: string;
}
