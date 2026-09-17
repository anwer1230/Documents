import React from 'react';
import {
  Menu,
  Search,
  PenSquare,
  Bookmark,
  Users,
  Megaphone,
  Bot,
  User,
  Check,
  CheckCheck,
  Pin,
  Sparkles,
  Plus,
  ChevronDown,
  X,
} from 'lucide-react';
import {
  TelegramChat,
  ChatFolder,
  TelegramUser,
  TelegramAccount,
  TypingStatus,
  TelegramPeerStories,
} from '../types';
import { getSenderColor, formatTelegramDate, getInitials } from '../utils/telegramColors';

interface SidebarProps {
  chats: TelegramChat[];
  selectedChatId: string;
  onSelectChat: (chatId: string) => void;
  onOpenMenu: () => void;
  onOpenNewChat: () => void;
  activeFolder: ChatFolder;
  onChangeFolder: (folder: ChatFolder) => void;
  searchQuery: string;
  onSearchChange: (query: string) => void;
  lang?: 'ar' | 'en';
  isDark?: boolean;
  currentUser: TelegramUser | null;
  accounts?: TelegramAccount[];
  onOpenAddAccount?: () => void;
  onSwitchAccount?: (accountId: string) => void;
  typingMap?: Record<string, TypingStatus>;
  peerStoriesList?: TelegramPeerStories[];
  onOpenStory?: (peerId: string) => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  chats,
  selectedChatId,
  onSelectChat,
  onOpenMenu,
  onOpenNewChat,
  activeFolder,
  onChangeFolder,
  searchQuery,
  onSearchChange,
  lang = 'ar',
  isDark = true,
  currentUser,
  accounts = [],
  onOpenAddAccount,
  onSwitchAccount,
  typingMap = {},
  peerStoriesList = [],
  onOpenStory,
}) => {
  const isAr = lang === 'ar';

  // Filter chats by search and active folder, deduplicating IDs to ensure unique React keys
  const seenChatIds = new Set<string>();
  const filteredChats = (chats || []).filter((chat, idx) => {
    if (!chat) return false;
    const resolvedId = String(chat.id || `chat_${idx}`);
    if (seenChatIds.has(resolvedId)) return false;
    seenChatIds.add(resolvedId);

    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const matchTitle = (chat.title || '').toLowerCase().includes(q);
      const matchUser = chat.username?.toLowerCase().includes(q);
      const matchLast = chat.lastMessage?.text?.toLowerCase().includes(q);
      if (!matchTitle && !matchUser && !matchLast) return false;
    }

    if (activeFolder === 'all') return true;
    if (activeFolder === 'personal') return chat.type === 'private' || chat.type === 'saved';
    if (activeFolder === 'groups') return chat.type === 'group' || chat.type === 'supergroup';
    if (activeFolder === 'channels') return chat.type === 'channel';
    if (activeFolder === 'bots') return chat.type === 'bot' || chat.isBot;
    return true;
  });

  const folders: { id: ChatFolder; label: string; icon: React.ReactNode }[] = [
    { id: 'all', label: isAr ? 'الكل' : 'All', icon: null },
    { id: 'personal', label: isAr ? 'الخاص' : 'Personal', icon: <User className="w-3.5 h-3.5" /> },
    { id: 'groups', label: isAr ? 'المجموعات' : 'Groups', icon: <Users className="w-3.5 h-3.5" /> },
    { id: 'channels', label: isAr ? 'القنوات' : 'Channels', icon: <Megaphone className="w-3.5 h-3.5" /> },
    { id: 'bots', label: isAr ? 'البوتات' : 'Bots', icon: <Bot className="w-3.5 h-3.5" /> },
  ];

  return (
    <div
      className={`w-full md:w-80 lg:w-96 h-full flex flex-col border-e select-none shrink-0 transition-colors ${
        isDark ? 'bg-[#17212b] border-[#242f3d] text-white' : 'bg-white border-gray-200 text-gray-900'
      }`}
    >
      {/* Top Header */}
      <div className="p-3 pb-2 flex items-center gap-2">
        <button
          onClick={onOpenMenu}
          aria-label="Open Menu"
          className="p-2 rounded-xl hover:bg-white/10 text-gray-400 hover:text-white transition"
        >
          <Menu className="w-5 h-5" />
        </button>

        {/* Search Bar */}
        <div
          className={`flex-1 flex items-center gap-2 px-3 py-1.5 rounded-full border transition-colors ${
            isDark
              ? 'bg-[#242f3d] border-transparent focus-within:border-[#3390ec]'
              : 'bg-gray-100 border-transparent focus-within:border-[#3390ec]'
          }`}
        >
          <Search className="w-4 h-4 text-gray-400 shrink-0" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
            placeholder={isAr ? 'بحث في الرسائل والمحادثات...' : 'Search...'}
            className="w-full bg-transparent text-xs outline-none placeholder-gray-400"
          />
          {searchQuery && (
            <button
              onClick={() => onSearchChange('')}
              className="p-0.5 rounded-full hover:bg-white/20 text-gray-400 hover:text-white"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
        </div>

        {/* New Chat Button */}
        <button
          onClick={onOpenNewChat}
          aria-label="New Chat"
          className="p-2 rounded-xl hover:bg-white/10 text-gray-400 hover:text-[#3390ec] transition"
        >
          <PenSquare className="w-5 h-5" />
        </button>
      </div>

      {/* Stories Carousel (If stories exist) */}
      {peerStoriesList.length > 0 && (
        <div className="px-3 py-2 flex items-center gap-3 overflow-x-auto no-scrollbar border-b border-gray-700/30 shrink-0">
          {peerStoriesList.map((peer, idx) => (
            <button
              key={peer.peerId ? `story-${peer.peerId}` : `story-${idx}`}
              onClick={() => onOpenStory && onOpenStory(peer.peerId)}
              className="flex flex-col items-center gap-1 group shrink-0"
            >
              <div
                className={`w-12 h-12 rounded-full p-0.5 transition-transform group-hover:scale-105 ${
                  peer.hasUnread
                    ? 'bg-gradient-to-tr from-amber-400 via-rose-500 to-[#3390ec] animate-pulse'
                    : 'bg-gray-600'
                }`}
              >
                <div
                  style={{ backgroundColor: getSenderColor(peer.peerId || idx) }}
                  className="w-full h-full rounded-full flex items-center justify-center text-xs font-bold text-white overflow-hidden"
                >
                  {peer.peerAvatar ? (
                    <img src={peer.peerAvatar} alt="" className="w-full h-full object-cover" />
                  ) : (
                    getInitials(peer.peerName || '')
                  )}
                </div>
              </div>
              <span className="text-[10px] text-gray-400 group-hover:text-white max-w-[50px] truncate">
                {peer.peerName || (isAr ? 'مستخدم' : 'User')}
              </span>
            </button>
          ))}
        </div>
      )}

      {/* Folders Tabs Bar */}
      <div className="flex items-center px-2 border-b border-gray-700/30 overflow-x-auto no-scrollbar shrink-0">
        {folders.map((folder) => {
          const isActive = activeFolder === folder.id;
          return (
            <button
              key={folder.id}
              onClick={() => onChangeFolder(folder.id)}
              className={`px-3.5 py-2.5 text-xs font-bold whitespace-nowrap transition-colors relative flex items-center gap-1.5 ${
                isActive
                  ? 'text-[#3390ec]'
                  : isDark
                  ? 'text-gray-400 hover:text-gray-200'
                  : 'text-gray-500 hover:text-gray-800'
              }`}
            >
              {folder.icon}
              <span>{folder.label}</span>
              {isActive && (
                <div className="absolute bottom-0 inset-x-2 h-0.5 bg-[#3390ec] rounded-t" />
              )}
            </button>
          );
        })}
      </div>

      {/* Chat List Items */}
      <div className="flex-1 overflow-y-auto divide-y divide-gray-800/10">
        {filteredChats.length === 0 ? (
          <div className="p-8 text-center text-gray-400 text-xs">
            {isAr ? 'لا توجد محادثات مطابقة' : 'No chats found'}
          </div>
        ) : (
          filteredChats.map((chat, idx) => {
            const isSelected = selectedChatId === chat.id;
            const typing = typingMap[chat.id];
            const isSaved = chat.type === 'saved';
            const chatKey = chat.id ? `chat-${chat.id}` : `chat-${idx}`;

            return (
              <div
                key={chatKey}
                onClick={() => onSelectChat(chat.id)}
                className={`p-3 flex items-center gap-3 cursor-pointer transition-colors relative ${
                  isSelected
                    ? isDark
                      ? 'bg-[#2b5278] text-white'
                      : 'bg-[#e3effa] text-gray-900'
                    : isDark
                    ? 'hover:bg-[#202b36]'
                    : 'hover:bg-gray-50'
                }`}
              >
                {/* Avatar Icon */}
                <div className="relative shrink-0">
                  <div
                    style={{ backgroundColor: chat.avatarColor || getSenderColor(chat.id) }}
                    className="w-12 h-12 rounded-full flex items-center justify-center font-bold text-sm text-white overflow-hidden shadow"
                  >
                    {isSaved ? (
                      <Bookmark className="w-5 h-5 fill-white" />
                    ) : chat.avatarUrl ? (
                      <img src={chat.avatarUrl} alt="" className="w-full h-full object-cover" />
                    ) : (
                      getInitials(chat.title || '')
                    )}
                  </div>
                  {chat.isOnline && !isSaved && (
                    <span className="absolute bottom-0 right-0 w-3 h-3 bg-emerald-500 rounded-full border-2 border-[#17212b]" />
                  )}
                </div>

                {/* Content */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-1 mb-0.5">
                    <div className="flex items-center gap-1 truncate font-semibold text-sm">
                      <span className="truncate">{chat.title || (isAr ? 'محادثة' : 'Chat')}</span>
                      {chat.isVerified && (
                        <span className="text-[#3390ec] shrink-0 text-xs">✓</span>
                      )}
                      {chat.isForum && (
                        <span className="text-amber-400 text-[10px] font-bold px-1 bg-amber-500/10 rounded">
                          Forum
                        </span>
                      )}
                    </div>
                    {chat.lastMessage && (
                      <span
                        className={`text-[11px] shrink-0 font-mono ${
                          isSelected
                            ? 'text-white/80'
                            : isDark
                            ? 'text-gray-400'
                            : 'text-gray-500'
                        }`}
                      >
                        {formatTelegramDate(chat.lastMessage.timestamp, lang as 'ar' | 'en')}
                      </span>
                    )}
                  </div>

                  <div className="flex items-center justify-between gap-1">
                    {typing ? (
                      <span className="text-xs text-[#3390ec] font-medium flex items-center gap-1">
                        <span className="w-1.5 h-1.5 rounded-full bg-[#3390ec] animate-bounce" />
                        <span>{typing.text || (isAr ? 'يكتب...' : 'typing...')}</span>
                      </span>
                    ) : (
                      <p
                        className={`text-xs truncate ${
                          isSelected
                            ? 'text-white/90'
                            : isDark
                            ? 'text-gray-400'
                            : 'text-gray-500'
                        }`}
                      >
                        {chat.lastMessage?.isOut && (
                          <span className="inline-block me-1 text-[#3390ec]">
                            <CheckCheck className="w-3 h-3 inline" />
                          </span>
                        )}
                        {chat.lastMessage?.text || (isAr ? 'لا توجد رسائل' : 'No messages')}
                      </p>
                    )}

                    <div className="flex items-center gap-1 shrink-0">
                      {chat.isPinned && (
                        <Pin className="w-3.5 h-3.5 text-gray-400 rotate-45" />
                      )}
                      {Number(chat.unreadCount) > 0 && (
                        <span className="px-1.5 py-0.5 rounded-full text-[10px] font-bold bg-[#3390ec] text-white min-w-[18px] text-center">
                          {chat.unreadCount}
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
