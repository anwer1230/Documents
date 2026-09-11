export interface TelegramAccount {
  id: string;
  sessionToken: string;
  user: TelegramUser;
  isLoggedIn: boolean;
  isDemo?: boolean;
  addedAt: number;
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
  status?: 'online' | 'offline' | 'recently';
}

export type ChatType = 'private' | 'group' | 'supergroup' | 'channel' | 'bot' | 'saved';

export interface TelegramReaction {
  emoji: string;
  count: number;
  users?: string[];
  userReacted?: boolean;
}

export interface TelegramMedia {
  type: 'photo' | 'video' | 'audio' | 'voice' | 'document';
  url?: string;
  title?: string;
  fileName?: string;
  fileSize?: string;
  duration?: number; // for audio/voice in seconds
  thumbnailUrl?: string;
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
  status: 'sending' | 'sent' | 'read';
  replyTo?: {
    id: string;
    senderName: string;
    text: string;
  };
  media?: TelegramMedia;
  reactions?: TelegramReaction[];
  isPinned?: boolean;
  isForwarded?: boolean;
  forwardedFrom?: string;
}

export interface TelegramChat {
  id: string;
  title: string;
  username?: string;
  type: ChatType;
  avatarUrl?: string;
  avatarColor?: string;
  lastMessage?: {
    text: string;
    timestamp: number;
    senderName?: string;
    isOut?: boolean;
    mediaType?: string;
  };
  unreadCount: number;
  isPinned?: boolean;
  isMuted?: boolean;
  isOnline?: boolean;
  membersCount?: number;
  description?: string;
}

export type ChatFolder = 'all' | 'personal' | 'channels' | 'groups' | 'bots' | 'unread';

export interface TelegramThemeConfig {
  isDark: boolean;
  accentColor: string; // hex code
  fontSize: 'sm' | 'md' | 'lg';
  language: 'ar' | 'en';
}

export interface TypingStatus {
  chatId: string;
  userName?: string;
  action?: 'typing' | 'recording' | 'uploading';
  startedAt: number;
  expiresAt: number;
}
