import { Chat, Message, User } from '../types/telegram';

export const CURRENT_USER: User = {
  id: 'me',
  name: 'أنور سيف',
  username: 'anwer1230',
  avatar: 'https://telegram.org/img/t_logo.png',
  avatarColor: 'from-blue-500 to-indigo-600',
  phone: '+967 779 123 456',
  bio: '',
  status: 'online',
  isPremium: true,
};

export const INITIAL_CHATS: Chat[] = [
  {
    id: 'saved_messages',
    type: 'saved',
    title: 'الرسائل المحفوظة',
    avatarColor: 'from-sky-400 to-blue-600',
    about: 'سحابتك الشخصية لتخزين الرسائل والملفات والروابط والوسائط في تليجرام بلا حدود.',
    isPinned: true,
    unreadCount: 0,
    lastMessage: {
      id: 'msg-saved-1',
      chatId: 'saved_messages',
      senderId: 'me',
      text: 'سحابتك الشخصية في تليجرام جاهزة للمزامنة السريعة.',
      timestamp: 'الآن',
      isOutgoing: true,
      status: 'read',
    },
  },
];

export const INITIAL_MESSAGES: Record<string, Message[]> = {
  saved_messages: [
    {
      id: 'msg-saved-1',
      chatId: 'saved_messages',
      senderId: 'me',
      text: 'سحابتك الشخصية في تليجرام جاهزة للمزامنة السريعة.',
      timestamp: 'الآن',
      isOutgoing: true,
      status: 'read',
      isPinned: true,
    },
  ],
};
