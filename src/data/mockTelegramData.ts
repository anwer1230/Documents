import { Chat, Folder, Message, TelegramApiConfig, User, UserAccount } from '../types';

export const CURRENT_USER: User = {
  id: '',
  name: '',
  username: '',
  phone: '',
  avatar: '',
  isOnline: false,
  bio: '',
  isVerified: false,
  isPremium: false,
};

export const DEFAULT_TELEGRAM_API_CONFIG: TelegramApiConfig = {
  apiId: '22043994',
  apiHash: '56f64582b363d367280db96586b97801',
  dcId: 4,
  dcIp: '149.154.167.91',
  port: 443,
  connectionStatus: 'connected',
  sessionString: '',
  mtprotoVersion: 'MTProto 2.0 (Layer 184 - Android)',
  pingMs: 24,
};

export const DEFAULT_FOLDERS: Folder[] = [
  { id: 'all', name: 'All Chats', nameAr: 'كل المحادثات', icon: 'Folder' },
  { id: 'personal', name: 'Personal', nameAr: 'شخصي', icon: 'User', chatTypes: ['private', 'saved'] },
  { id: 'channels', name: 'Channels', nameAr: 'قنوات', icon: 'Megaphone', chatTypes: ['channel'] },
  { id: 'groups', name: 'Groups', nameAr: 'مجموعات', icon: 'Users', chatTypes: ['group'] },
];

export const INITIAL_CHATS: Chat[] = [
  {
    id: 'chat_saved_messages',
    type: 'saved',
    title: 'Saved Messages',
    avatar: '',
    isPinned: true,
    unreadCount: 0,
    lastMessage: {
      id: 'm_saved_welcome',
      senderName: 'You',
      text: 'مرحباً بك في مساحتك السحابية الآمنة لحفظ الرسائل والملفات والملاحظات عبر السحابة المشفرة.',
      timestamp: '12:00 PM',
      isOutgoing: true,
      status: 'read',
    },
    description: 'مساحتك السحابية الخاصة لحفظ الرسائل، الصور، الفيديوهات، الملفات والملاحظات الصوتية مع مزامنة فورية على جميع أجهزتك.',
  },
  {
    id: 'chat_telegram_service',
    type: 'private',
    title: 'Telegram Notifications',
    username: 'service_notifications',
    avatar: 'https://images.unsplash.com/photo-1614680376593-902f749f7ffc?w=150&auto=format&fit=crop&q=80',
    isVerified: true,
    isPinned: true,
    unreadCount: 0,
    lastMessage: {
      id: 'm_tg_service_1',
      senderName: 'Telegram',
      text: 'Login code: 777000. Do not give this code to anyone, even if they say they are from Telegram!',
      timestamp: '11:45 AM',
      isOutgoing: false,
      status: 'read',
    },
    description: 'Official Telegram Service Notifications channel for login alerts and security codes.',
  },
  {
    id: 'chat_telegram_news',
    type: 'channel',
    title: 'Telegram News',
    username: 'telegram',
    avatar: 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150&auto=format&fit=crop&q=80',
    isVerified: true,
    isPinned: false,
    unreadCount: 1,
    memberCount: 9400000,
    lastMessage: {
      id: 'm_news_1',
      senderName: 'Telegram News',
      text: '⚡ Telegram MTProto 2.0 (Layer 184) update is now live with enhanced cloud sync, high-speed media, and rich multi-account support.',
      timestamp: '10:30 AM',
      isOutgoing: false,
      status: 'read',
    },
    description: 'The official channel for Telegram updates, major feature announcements, and platform news.',
  },
  {
    id: 'chat_botfather',
    type: 'bot',
    title: 'BotFather',
    username: 'botfather',
    avatar: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80',
    isVerified: true,
    isPinned: false,
    unreadCount: 0,
    lastMessage: {
      id: 'm_bf_1',
      senderName: 'BotFather',
      text: 'I can help you create and manage Telegram bots. Send /help to get started.',
      timestamp: 'Yesterday',
      isOutgoing: false,
      status: 'read',
    },
    description: 'BotFather is the one bot to rule them all. Use it to create new bot accounts and manage your existing bots.',
  },
  {
    id: 'chat_durov',
    type: 'channel',
    title: 'Pavel Durov',
    username: 'durov',
    avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
    isVerified: true,
    isPinned: false,
    unreadCount: 0,
    memberCount: 2850000,
    lastMessage: {
      id: 'm_durov_1',
      senderName: 'Pavel Durov',
      text: 'Privacy is not for sale, and human rights should not be compromised out of fear.',
      timestamp: 'Aug 24',
      isOutgoing: false,
      status: 'read',
    },
    description: 'Channel of the CEO & Founder of Telegram.',
  },
  {
    id: 'chat_ton_community',
    type: 'group',
    title: 'TON Community & Developers Hub',
    username: 'toncoin',
    avatar: 'https://images.unsplash.com/photo-1622979135225-d2ba269bc1df?w=150&auto=format&fit=crop&q=80',
    isVerified: false,
    isPinned: false,
    unreadCount: 0,
    memberCount: 48500,
    lastMessage: {
      id: 'm_ton_1',
      senderName: 'Alex Developer',
      text: 'Check out the new MTProto Layer 184 client implementation with full multi-account syncing!',
      timestamp: 'Aug 22',
      isOutgoing: false,
      status: 'read',
    },
    description: 'Global developer community building on Telegram MTProto, Mini Apps, and Web3 ecosystem.',
  },
];

export const INITIAL_MESSAGES: Record<string, Message[]> = {
  chat_saved_messages: [
    {
      id: 'm_saved_1',
      chatId: 'chat_saved_messages',
      senderId: 'user_me',
      senderName: 'You',
      text: '📌 مرحباً بك في مساحتك السحابية المشفرة (Saved Messages).\n\nيمكنك هنا:\n• كتابة الملاحظات والأفكار والمذكرات\n• حفظ ومشاركة الروابط والملفات والمستندات\n• إعادة توجيه الرسائل من القنوات والمحادثات للرجوع إليها لاحقاً\n• إرسال الرسائل الصوتية والصور بجودة كاملة',
      timestamp: '12:00 PM',
      date: '2026-08-23',
      isOutgoing: true,
      status: 'read',
      isPinned: true,
    },
  ],
  chat_telegram_service: [
    {
      id: 'm_tg_service_1',
      chatId: 'chat_telegram_service',
      senderId: 'sys_telegram',
      senderName: 'Telegram',
      text: '🔒 Official Security Notification:\n\nYour Telegram account was successfully authenticated via MTProto 2.0 (Layer 184).\n\nIf this was you, no action is needed.',
      timestamp: '11:45 AM',
      date: '2026-08-25',
      isOutgoing: false,
      status: 'read',
    },
  ],
  chat_telegram_news: [
    {
      id: 'm_news_1',
      chatId: 'chat_telegram_news',
      senderId: 'sys_news',
      senderName: 'Telegram News',
      text: '⚡ Telegram MTProto 2.0 (Layer 184) update is now live with enhanced cloud sync, high-speed media, and rich multi-account support.',
      timestamp: '10:30 AM',
      date: '2026-08-25',
      isOutgoing: false,
      status: 'read',
    },
  ],
  chat_botfather: [
    {
      id: 'm_bf_1',
      chatId: 'chat_botfather',
      senderId: 'botfather',
      senderName: 'BotFather',
      text: 'I can help you create and manage Telegram bots. Send /help to get a list of commands.',
      timestamp: 'Yesterday',
      date: '2026-08-24',
      isOutgoing: false,
      status: 'read',
    },
  ],
  chat_durov: [
    {
      id: 'm_durov_1',
      chatId: 'chat_durov',
      senderId: 'user_durov',
      senderName: 'Pavel Durov',
      text: 'Privacy is not for sale, and human rights should not be compromised out of fear.',
      timestamp: 'Aug 24',
      date: '2026-08-24',
      isOutgoing: false,
      status: 'read',
    },
  ],
  chat_ton_community: [
    {
      id: 'm_ton_1',
      chatId: 'chat_ton_community',
      senderId: 'user_alex',
      senderName: 'Alex Developer',
      text: 'Check out the new MTProto Layer 184 client implementation with full multi-account syncing!',
      timestamp: 'Aug 22',
      date: '2026-08-22',
      isOutgoing: false,
      status: 'read',
    },
  ],
};

export const TELEGRAM_STICKERS = [
  { id: 'st_duck_1', name: 'Duck Thumbs Up', emoji: '👍', url: 'https://fonts.gstatic.com/s/e/notoemoji/latest/1f44d/512.webp' },
  { id: 'st_duck_2', name: 'Party Popper', emoji: '🎉', url: 'https://fonts.gstatic.com/s/e/notoemoji/latest/1f389/512.webp' },
  { id: 'st_cat_1', name: 'Cool Face', emoji: '😎', url: 'https://fonts.gstatic.com/s/e/notoemoji/latest/1f60e/512.webp' },
  { id: 'st_cat_2', name: 'Red Heart', emoji: '❤️', url: 'https://fonts.gstatic.com/s/e/notoemoji/latest/2764_fe0f/512.webp' },
];

export const POPULAR_REACTIONS = ['👍', '❤️', '🔥', '🎉', '👏', '😍', '🤔', '⚡', '💯', '🚀'];

export const DEMO_USER = CURRENT_USER;
export const INITIAL_DIALOGS = INITIAL_CHATS;
export const INITIAL_MESSAGES_MAP = INITIAL_MESSAGES;

export const DEFAULT_ACCOUNTS: UserAccount[] = [];
