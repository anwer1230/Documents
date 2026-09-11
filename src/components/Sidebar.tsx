import React, { useState, useEffect } from 'react';
import {
  Menu,
  Search,
  X,
  Bookmark,
  Radio,
  Users,
  Bot,
  MessageSquare,
  Pin,
  Check,
  CheckCheck,
  Plus,
  VolumeX,
  CheckCircle2,
  Loader2,
  Globe,
  UserPlus,
  Edit2,
} from 'lucide-react';
import { TelegramChat, ChatFolder, TelegramUser, TelegramAccount, TypingStatus } from '../types';

interface SidebarProps {
  chats: TelegramChat[];
  selectedChatId?: string;
  onSelectChat: (chat: TelegramChat) => void;
  onOpenMenu: () => void;
  onOpenNewChat: () => void;
  activeFolder: ChatFolder;
  onChangeFolder: (folder: ChatFolder) => void;
  searchQuery: string;
  onSearchChange: (q: string) => void;
  lang: 'ar' | 'en';
  isDark: boolean;
  currentUser?: TelegramUser;
  accounts?: TelegramAccount[];
  onOpenAddAccount?: () => void;
  onSwitchAccount?: (accountId: string) => void;
  typingMap?: Record<string, TypingStatus>;
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
  lang,
  isDark,
  currentUser,
  accounts = [],
  onOpenAddAccount,
  onSwitchAccount,
  typingMap = {},
}) => {
  const isAr = lang === 'ar';
  const [isSearchFocused, setIsSearchFocused] = useState(false);
  const [globalResults, setGlobalResults] = useState<any[]>([]);
  const [isSearchingGlobal, setIsSearchingGlobal] = useState(false);

  // Debounced Telegram Global Search (MTProto contacts.search & channels lookup)
  useEffect(() => {
    if (!searchQuery || searchQuery.trim().length === 0) {
      setGlobalResults([]);
      setIsSearchingGlobal(false);
      return;
    }

    const timer = setTimeout(async () => {
      setIsSearchingGlobal(true);
      try {
        const res = await fetch(`/api/telegram/search-global?q=${encodeURIComponent(searchQuery.trim())}`);
        if (res.ok) {
          const data = await res.json();
          if (data.results) {
            setGlobalResults(data.results);
          }
        }
      } catch (err) {
        console.error('Error in Telegram global search:', err);
      } finally {
        setIsSearchingGlobal(false);
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [searchQuery]);

  const formatFollowers = (count?: number) => {
    if (!count) return '';
    if (count >= 1000000) return `${(count / 1000000).toFixed(1)}M`;
    if (count >= 1000) return `${(count / 1000).toFixed(0)}K`;
    return count.toLocaleString();
  };

  const handleSelectGlobalResult = (result: any) => {
    const existing = chats.find(
      (c) => c.id === result.id || (c.username && c.username.toLowerCase() === result.username?.toLowerCase())
    );
    if (existing) {
      onSelectChat(existing);
      return;
    }

    const newChat: TelegramChat = {
      id: result.id || `channel_${result.username || Date.now()}`,
      title: result.title || result.username || 'قناة تليجرام',
      username: result.username,
      type: result.type || 'channel',
      isJoined: result.isJoined ?? false,
      isVerified: result.isVerified ?? false,
      membersCount: result.participantsCount || 12000,
      description: result.description,
      unreadCount: 0,
      avatarColor: '#3390ec',
    };
    onSelectChat(newChat);
  };

  // Filter chats by Folder and Search query
  const filteredChats = chats.filter((chat) => {
    // Search query filter
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const matchTitle = chat.title.toLowerCase().includes(q);
      const matchUsername = chat.username?.toLowerCase().includes(q);
      const matchMsg = chat.lastMessage?.text?.toLowerCase().includes(q);
      if (!matchTitle && !matchUsername && !matchMsg) return false;
    }

    // Folder filter
    if (activeFolder === 'all') return true;
    if (activeFolder === 'personal') return chat.type === 'private' || chat.type === 'saved';
    if (activeFolder === 'channels') return chat.type === 'channel';
    if (activeFolder === 'groups') return chat.type === 'group' || chat.type === 'supergroup';
    if (activeFolder === 'bots') return chat.type === 'bot';
    if (activeFolder === 'unread') return chat.unreadCount > 0;
    return true;
  });

  const pinnedChats = filteredChats.filter((c) => c.isPinned);
  const regularChats = filteredChats.filter((c) => !c.isPinned);

  // Folder definitions with counts
  const folders: { id: ChatFolder; label: string; icon: any }[] = [
    { id: 'all', label: isAr ? 'الكل' : 'All', icon: MessageSquare },
    { id: 'personal', label: isAr ? 'الخاص' : 'Direct', icon: Bookmark },
    { id: 'channels', label: isAr ? 'القنوات' : 'Channels', icon: Radio },
    { id: 'groups', label: isAr ? 'المجموعات' : 'Groups', icon: Users },
    { id: 'bots', label: isAr ? 'البوتات' : 'Bots', icon: Bot },
  ];

  // Helper to format timestamp
  const formatTime = (ts?: number) => {
    if (!ts) return '';
    const date = new Date(ts);
    const now = new Date();
    const isToday = date.toDateString() === now.toDateString();
    if (isToday) {
      return date.toLocaleTimeString(isAr ? 'ar-SA' : 'en-US', {
        hour: '2-digit',
        minute: '2-digit',
        hour12: false,
      });
    }
    return date.toLocaleDateString(isAr ? 'ar-SA' : 'en-US', {
      month: 'short',
      day: 'numeric',
    });
  };

  const renderChatItem = (chat: TelegramChat) => {
    const isSelected = chat.id === selectedChatId;
    return (
      <button
        key={chat.id}
        onClick={() => onSelectChat(chat)}
        className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl transition-colors text-start relative group select-none ${
          isSelected
            ? isDark
              ? 'bg-[#2b5278] text-white'
              : 'bg-[#3390ec] text-white'
            : isDark
            ? 'hover:bg-[#202b36] text-gray-200'
            : 'hover:bg-[#f4f4f5] text-gray-900'
        }`}
      >
        {/* Avatar */}
        <div className="relative shrink-0">
          {chat.avatarUrl ? (
            <img
              src={chat.avatarUrl}
              alt={chat.title}
              referrerPolicy="no-referrer"
              className="w-12 h-12 rounded-full object-cover"
            />
          ) : (
            <div
              className="w-12 h-12 rounded-full flex items-center justify-center text-white font-bold text-base shadow-sm"
              style={{
                backgroundColor:
                  chat.avatarColor || (chat.type === 'saved' ? '#3390ec' : '#5682a3'),
              }}
            >
              {chat.type === 'saved' ? (
                <Bookmark className="w-5 h-5 fill-white text-white" />
              ) : (
                chat.title.slice(0, 2)
              )}
            </div>
          )}

          {/* Online badge */}
          {chat.isOnline && (
            <span className="absolute bottom-0 right-0 w-3.5 h-3.5 bg-emerald-500 border-2 border-[#17212b] rounded-full" />
          )}
        </div>

        {/* Chat Info */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between gap-1 mb-0.5">
            <div className="flex items-center gap-1 min-w-0">
              <span
                className={`font-semibold text-sm truncate ${
                  isSelected ? 'text-white' : isDark ? 'text-white' : 'text-gray-900'
                }`}
              >
                {chat.title}
              </span>
              {chat.isMuted && (
                <VolumeX
                  className={`w-3.5 h-3.5 shrink-0 ${
                    isSelected ? 'text-blue-100' : 'text-gray-400'
                  }`}
                />
              )}
            </div>
            {chat.lastMessage && (
              <span
                className={`text-[11px] shrink-0 font-medium ${
                  isSelected ? 'text-blue-100' : isDark ? 'text-gray-400' : 'text-gray-500'
                }`}
              >
                {formatTime(chat.lastMessage.timestamp)}
              </span>
            )}
          </div>

          <div className="flex items-center justify-between gap-1 text-xs">
            {typingMap && typingMap[chat.id] && Date.now() < typingMap[chat.id].expiresAt ? (
              <div
                className={`flex items-center gap-1.5 flex-1 truncate font-medium ${
                  isSelected ? 'text-white' : 'text-[#3390ec]'
                }`}
              >
                <span className="inline-flex items-center gap-0.5 shrink-0" aria-hidden="true">
                  <span
                    className={`w-1 h-1 rounded-full animate-bounce [animation-delay:-0.3s] ${
                      isSelected ? 'bg-white' : 'bg-[#3390ec]'
                    }`}
                  />
                  <span
                    className={`w-1 h-1 rounded-full animate-bounce [animation-delay:-0.15s] ${
                      isSelected ? 'bg-white' : 'bg-[#3390ec]'
                    }`}
                  />
                  <span
                    className={`w-1 h-1 rounded-full animate-bounce ${
                      isSelected ? 'bg-white' : 'bg-[#3390ec]'
                    }`}
                  />
                </span>
                <span className="truncate">
                  {chat.type === 'group' || chat.type === 'supergroup'
                    ? `${typingMap[chat.id].userName || ''} ${isAr ? 'يكتب...' : 'is typing...'}`
                    : isAr
                    ? 'يكتب...'
                    : 'typing...'}
                </span>
              </div>
            ) : (
              <p
                className={`truncate flex-1 text-start ${
                  isSelected ? 'text-blue-100' : isDark ? 'text-gray-400' : 'text-gray-500'
                }`}
              >
                {chat.lastMessage ? (
                  <>
                    {chat.lastMessage.isOut && (
                      <span className="inline-flex items-center gap-0.5 me-1 text-emerald-400 align-middle">
                        <CheckCheck className="w-3.5 h-3.5 inline" />
                      </span>
                    )}
                    {chat.lastMessage.senderName && chat.type !== 'private' && (
                      <span className="font-medium me-1">{chat.lastMessage.senderName}:</span>
                    )}
                    <span>{chat.lastMessage.text}</span>
                  </>
                ) : (
                  <span className="italic text-gray-500">
                    {isAr ? 'لا توجد رسائل بعد' : 'No messages yet'}
                  </span>
                )}
              </p>
            )}

            <div className="flex items-center gap-1.5 shrink-0">
              {chat.isPinned && (
                <Pin
                  className={`w-3.5 h-3.5 fill-current ${
                    isSelected ? 'text-white' : isDark ? 'text-gray-400' : 'text-gray-500'
                  }`}
                />
              )}
              {chat.unreadCount > 0 && (
                <span
                  className={`px-1.5 py-0.5 text-[11px] font-bold rounded-full min-w-[20px] text-center ${
                    isSelected
                      ? 'bg-white text-[#3390ec]'
                      : 'bg-[#3390ec] text-white'
                  }`}
                >
                  {chat.unreadCount}
                </span>
              )}
            </div>
          </div>
        </div>
      </button>
    );
  };

  return (
    <aside
      className={`w-full md:w-80 lg:w-96 flex flex-col h-full border-e relative z-10 transition-colors ${
        isDark ? 'bg-[#17212b] border-[#0e1621]' : 'bg-white border-gray-200'
      }`}
    >
      {/* Top Header: Hamburger + Search */}
      <div className="p-3 pb-2 flex items-center gap-2">
        <button
          onClick={onOpenMenu}
          className={`p-2.5 rounded-full transition-colors ${
            isDark ? 'hover:bg-[#232e3c] text-gray-300' : 'hover:bg-gray-100 text-gray-700'
          }`}
          title={isAr ? 'القائمة الرئيسية' : 'Main Menu'}
          aria-label="menu"
        >
          <Menu className="w-5 h-5" />
        </button>

        <div className="relative flex-1">
          <input
            type="text"
            placeholder={isAr ? 'بحث في المحادثات...' : 'Search...'}
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
            onFocus={() => setIsSearchFocused(true)}
            onBlur={() => setIsSearchFocused(false)}
            className={`w-full py-2 ps-9 pe-8 text-sm rounded-full transition-colors focus:outline-none ${
              isDark
                ? 'bg-[#242f3d] text-white placeholder-gray-400 focus:bg-[#2b394a]'
                : 'bg-gray-100 text-gray-900 placeholder-gray-500 focus:bg-white focus:ring-1 focus:ring-[#3390ec]'
            }`}
          />
          <Search className="w-4 h-4 text-gray-400 absolute start-3 top-2.5 pointer-events-none" />
          {searchQuery && (
            <button
              onClick={() => onSearchChange('')}
              className="absolute end-2.5 top-2.5 text-gray-400 hover:text-gray-200 p-0.5"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
        </div>

        {/* Quick Add User / Account Badge */}
        <div className="flex items-center gap-1 shrink-0">
          {onOpenAddAccount && accounts.length < 6 && (
            <button
              onClick={onOpenAddAccount}
              className={`p-2 rounded-full transition-colors flex items-center justify-center ${
                isDark ? 'hover:bg-[#232e3c] text-[#3390ec]' : 'hover:bg-gray-100 text-[#3390ec]'
              }`}
              title={isAr ? `إضافة مستخدم (${accounts.length}/6)` : `Add user (${accounts.length}/6)`}
            >
              <Plus className="w-4 h-4" />
            </button>
          )}

          {currentUser && (
            <button
              onClick={onOpenMenu}
              className="relative p-0.5 rounded-full hover:ring-2 hover:ring-[#3390ec] transition"
              title={isAr ? `الحسابات النشطة: ${accounts.length}/6` : `Active accounts: ${accounts.length}/6`}
            >
              {currentUser.photoUrl ? (
                <img
                  src={currentUser.photoUrl}
                  alt={currentUser.firstName}
                  referrerPolicy="no-referrer"
                  className="w-7 h-7 rounded-full object-cover"
                />
              ) : (
                <div className="w-7 h-7 rounded-full bg-[#3390ec] text-white text-[11px] font-bold flex items-center justify-center">
                  {currentUser.firstName.slice(0, 1)}
                </div>
              )}
              {accounts.length > 1 && (
                <span className="absolute -top-1 -end-1 w-4 h-4 rounded-full bg-emerald-500 text-white text-[9px] font-black flex items-center justify-center shadow-xs">
                  {accounts.length}
                </span>
              )}
            </button>
          )}
        </div>
      </div>

      {/* Folders / Tabs bar */}
      <div
        className={`flex items-center gap-1 px-3 py-1.5 overflow-x-auto no-scrollbar border-b ${
          isDark ? 'border-[#0e1621]' : 'border-gray-100'
        }`}
      >
        {folders.map((f) => {
          const isActive = activeFolder === f.id;
          const Icon = f.icon;
          return (
            <button
              key={f.id}
              onClick={() => onChangeFolder(f.id)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-all flex items-center gap-1.5 ${
                isActive
                  ? isDark
                    ? 'bg-[#2b5278] text-white'
                    : 'bg-[#3390ec] text-white shadow-sm'
                  : isDark
                  ? 'text-gray-400 hover:text-gray-200 hover:bg-[#202b36]'
                  : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
              }`}
            >
              <Icon className="w-3.5 h-3.5" />
              <span>{f.label}</span>
            </button>
          );
        })}
      </div>

      {/* Chat List Scroll Area */}
      <div className="flex-1 overflow-y-auto px-2 py-2 space-y-1 divide-y divide-transparent">
        {searchQuery.trim() ? (
          <div>
            {/* Section 1: Chats and Contacts */}
            {filteredChats.length > 0 && (
              <div className="mb-3">
                <div className="px-3 py-1.5 text-[11px] font-bold uppercase tracking-wider text-gray-400 flex items-center justify-between">
                  <span>{isAr ? 'المحادثات وجهات الاتصال' : 'Chats and Contacts'}</span>
                  <span className="text-[10px] bg-gray-500/10 px-2 py-0.5 rounded-full">{filteredChats.length}</span>
                </div>
                <div className="space-y-1">
                  {filteredChats.map(renderChatItem)}
                </div>
              </div>
            )}

            {/* Section 2: Telegram Global Search */}
            <div className="mt-2">
              <div className="px-3 py-1.5 text-[11px] font-bold uppercase tracking-wider text-[#3390ec] flex items-center justify-between">
                <span className="flex items-center gap-1.5">
                  <Globe className="w-3.5 h-3.5" />
                  <span>{isAr ? 'البحث العام في تيليجرام' : 'Global Search'}</span>
                </span>
                {isSearchingGlobal && <Loader2 className="w-3.5 h-3.5 animate-spin text-[#3390ec]" />}
              </div>

              {isSearchingGlobal ? (
                <div className="py-6 text-center text-xs text-gray-400 flex items-center justify-center gap-2">
                  <Loader2 className="w-4 h-4 animate-spin text-[#3390ec]" />
                  <span>{isAr ? 'جارٍ البحث عن القنوات في تيليجرام...' : 'Searching channels on Telegram...'}</span>
                </div>
              ) : globalResults.length > 0 ? (
                <div className="space-y-1">
                  {globalResults.map((item) => {
                    const isChannel = item.type === 'channel';
                    const isGroup = item.type === 'group' || item.type === 'supergroup';
                    const isSelected = selectedChatId === item.id;
                    const isAlreadyMember =
                      item.isJoined ||
                      chats.some(
                        (c) =>
                          c.id === item.id ||
                          (c.username && c.username.toLowerCase() === item.username?.toLowerCase() && c.isJoined !== false)
                      );

                    return (
                      <button
                        key={item.id || item.username}
                        onClick={() => handleSelectGlobalResult(item)}
                        className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all text-start relative group select-none ${
                          isSelected
                            ? isDark
                              ? 'bg-[#2b5278] text-white'
                              : 'bg-[#3390ec] text-white'
                            : isDark
                            ? 'hover:bg-[#202b36] text-gray-200'
                            : 'hover:bg-[#f4f4f5] text-gray-900'
                        }`}
                      >
                        {/* Avatar */}
                        <div className="relative shrink-0">
                          <div
                            className="w-12 h-12 rounded-full flex items-center justify-center font-bold text-base shadow-sm text-white"
                            style={{
                              backgroundColor: isChannel ? '#3390ec' : isGroup ? '#2fa28a' : '#8e54e9',
                            }}
                          >
                            {isChannel ? (
                              <Radio className="w-5 h-5" />
                            ) : isGroup ? (
                              <Users className="w-5 h-5" />
                            ) : (
                              <Bot className="w-5 h-5" />
                            )}
                          </div>
                        </div>

                        {/* Title & info */}
                        <div className="min-w-0 flex-1">
                          <div className="flex items-center justify-between gap-1 mb-0.5">
                            <div className="flex items-center gap-1.5 min-w-0">
                              <span className="font-semibold text-sm truncate">{item.title}</span>
                              {item.isVerified && (
                                <CheckCircle2 className="w-3.5 h-3.5 text-[#3390ec] fill-[#3390ec]/20 shrink-0" />
                              )}
                            </div>
                            {isAlreadyMember ? (
                              <span className="text-[10px] px-2 py-0.5 rounded-md font-medium shrink-0 bg-green-500/15 text-green-400">
                                {isAr ? 'عضو' : 'Joined'}
                              </span>
                            ) : (
                              <span className="text-[10px] px-2 py-0.5 rounded-md font-medium shrink-0 bg-[#3390ec]/15 text-[#3390ec] group-hover:bg-[#3390ec] group-hover:text-white transition">
                                {isChannel ? (isAr ? 'قناة' : 'Channel') : isGroup ? (isAr ? 'مجموعة' : 'Group') : (isAr ? 'بوت' : 'Bot')}
                              </span>
                            )}
                          </div>

                          <div className="flex items-center gap-2 text-xs text-gray-400">
                            {item.username && <span>@{item.username}</span>}
                            {item.participantsCount && (
                              <>
                                <span>•</span>
                                <span>
                                  {formatFollowers(item.participantsCount)}{' '}
                                  {isChannel
                                    ? isAr
                                      ? 'مشترك'
                                      : 'subscribers'
                                    : isAr
                                    ? 'عضو'
                                    : 'members'}
                                </span>
                              </>
                            )}
                          </div>
                        </div>
                      </button>
                    );
                  })}
                </div>
              ) : filteredChats.length === 0 ? (
                <div className="text-center py-12 px-4">
                  <MessageSquare className="w-12 h-12 mx-auto text-gray-500/40 mb-3" />
                  <p className="text-sm text-gray-400 font-medium">
                    {isAr ? 'لا توجد نتائج مطابقة لبحثك في تيليجرام' : 'No results found on Telegram'}
                  </p>
                </div>
              ) : null}
            </div>
          </div>
        ) : filteredChats.length === 0 ? (
          <div className="text-center py-12 px-4">
            <MessageSquare className="w-12 h-12 mx-auto text-gray-500/40 mb-3" />
            <p className="text-sm text-gray-400 font-medium">
              {isAr ? 'لا توجد محادثات في هذا المجلد' : 'No chats in this folder'}
            </p>
          </div>
        ) : (
          <>
            {pinnedChats.map(renderChatItem)}
            {pinnedChats.length > 0 && regularChats.length > 0 && (
              <div
                className={`my-1 border-b ${
                  isDark ? 'border-[#232e3c]' : 'border-gray-200'
                }`}
              />
            )}
            {regularChats.map(renderChatItem)}
          </>
        )}
      </div>

      {/* Floating Action Button (New Message / Channel - Telegram Web K Pencil) */}
      <div className="absolute bottom-5 end-5 z-20">
        <button
          onClick={onOpenNewChat}
          className="w-13 h-13 bg-[#3390ec] hover:bg-[#2881da] text-white rounded-full flex items-center justify-center shadow-xl shadow-[#3390ec]/30 transition-transform hover:scale-105 active:scale-95"
          title={isAr ? 'محادثة أو قناة جديدة' : 'New Message or Channel'}
        >
          <Edit2 className="w-5 h-5" />
        </button>
      </div>
    </aside>
  );
};
