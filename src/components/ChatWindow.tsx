import React, { useState, useRef, useEffect } from 'react';
import {
  Search,
  MoreVertical,
  Pin,
  Bookmark,
  Check,
  CheckCheck,
  Reply,
  Trash2,
  Play,
  Pause,
  Download,
  FileText,
  PanelRightClose,
  PanelRightOpen,
  VolumeX,
  Volume2,
  X,
  Sparkles,
  ChevronDown,
  Share2,
  Flag,
  LogOut,
  Copy,
  Forward,
  CheckSquare,
  Square,
  CornerDownRight,
  ExternalLink,
  Radio,
  CheckCircle2,
  Loader2,
  Bell,
  BellOff,
  UserPlus,
  Bot,
} from 'lucide-react';
import { TelegramChat, TelegramMessage, TelegramUser, TypingStatus } from '../types';
import { MessageInput } from './MessageInput';
import { VoiceWaveformPlayer } from './VoiceWaveformPlayer';
import { getSenderColor, formatTelegramDate } from '../utils/telegramColors';
import { ClearHistoryModal } from './modals/ClearHistoryModal';
import { LeaveGroupModal } from './modals/LeaveGroupModal';
import { ShareLinkModal } from './modals/ShareLinkModal';
import { ReportChatModal } from './modals/ReportChatModal';
import { DeleteMessageModal } from './modals/DeleteMessageModal';

interface ChatWindowProps {
  chat: TelegramChat | null;
  messages: TelegramMessage[];
  currentUser?: TelegramUser;
  typingStatus?: TypingStatus | null;
  onSimulateTyping?: () => void;
  onSendMessage: (text: string, replyTo?: TelegramMessage, media?: any) => void;
  onReactMessage: (messageId: string, emoji: string) => void;
  onPinMessage: (messageId: string) => void;
  onDeleteMessage: (messageId: string) => void;
  onDeleteMultipleMessages?: (messageIds: string[]) => void;
  onToggleChatInfo: () => void;
  isChatInfoOpen: boolean;
  onToggleMute: (chatId: string) => void;
  onClearHistory: (chatId: string, alsoForEveryone?: boolean) => void;
  onLeaveGroup: (chatId: string) => void;
  onReportChat: (chatId: string, reason: string, details?: string) => void;
  onOpenMediaViewer: (url: string, title?: string) => void;
  onOpenMiniApp?: (url?: string, appName?: string) => void;
  onBotCallback?: (messageId: string, callbackData: string) => Promise<void> | void;
  onJoinChannel?: (chatId: string) => Promise<void> | void;
  isJoiningChannel?: boolean;
  onToast: (message: string, type?: 'success' | 'info' | 'error') => void;
  lang: 'ar' | 'en';
  isDark: boolean;
}

export const ChatWindow: React.FC<ChatWindowProps> = ({
  chat,
  messages,
  currentUser,
  typingStatus,
  onSimulateTyping,
  onSendMessage,
  onReactMessage,
  onPinMessage,
  onDeleteMessage,
  onDeleteMultipleMessages,
  onToggleChatInfo,
  isChatInfoOpen,
  onToggleMute,
  onClearHistory,
  onLeaveGroup,
  onReportChat,
  onOpenMediaViewer,
  onOpenMiniApp,
  onBotCallback,
  onJoinChannel,
  isJoiningChannel,
  onToast,
  lang,
  isDark,
}) => {
  const isAr = lang === 'ar';
  const [replyingMessage, setReplyingMessage] = useState<TelegramMessage | null>(null);
  const [inChatSearch, setInChatSearch] = useState(false);
  const [searchWord, setSearchWord] = useState('');
  const [showMoreMenu, setShowMoreMenu] = useState(false);
  const [playingVoiceId, setPlayingVoiceId] = useState<string | null>(null);
  const [hoveredMessageId, setHoveredMessageId] = useState<string | null>(null);
  const [showScrollBottom, setShowScrollBottom] = useState(false);
  const [highlightedMsgId, setHighlightedMsgId] = useState<string | null>(null);

  // Context Menu State
  const [contextMenu, setContextMenu] = useState<{
    visible: boolean;
    x: number;
    y: number;
    message: TelegramMessage | null;
  }>({ visible: false, x: 0, y: 0, message: null });

  // Selection Mode State
  const [isSelectionMode, setIsSelectionMode] = useState(false);
  const [selectedMsgIds, setSelectedMsgIds] = useState<string[]>([]);

  // Modals state
  const [isClearModalOpen, setIsClearModalOpen] = useState(false);
  const [isLeaveModalOpen, setIsLeaveModalOpen] = useState(false);
  const [isShareModalOpen, setIsShareModalOpen] = useState(false);
  const [isReportModalOpen, setIsReportModalOpen] = useState(false);
  const [deleteModalMsgId, setDeleteModalMsgId] = useState<string | null>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const scrollContainerRef = useRef<HTMLDivElement>(null);

  // Auto-scroll on new message
  useEffect(() => {
    if (!showScrollBottom) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages.length, chat?.id]);

  // Scroll listener for bottom button
  const handleScroll = () => {
    if (!scrollContainerRef.current) return;
    const { scrollTop, scrollHeight, clientHeight } = scrollContainerRef.current;
    const distanceToBottom = scrollHeight - scrollTop - clientHeight;
    setShowScrollBottom(distanceToBottom > 220);
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  // Close context menu on outside click
  useEffect(() => {
    const handleOutside = () => {
      if (contextMenu.visible) {
        setContextMenu({ visible: false, x: 0, y: 0, message: null });
      }
      if (showMoreMenu) {
        setShowMoreMenu(false);
      }
    };
    window.addEventListener('click', handleOutside);
    return () => window.removeEventListener('click', handleOutside);
  }, [contextMenu.visible, showMoreMenu]);

  if (!chat) {
    return (
      <div
        className={`flex-1 flex flex-col items-center justify-center p-6 text-center select-none ${
          isDark ? 'bg-[#0e1621] text-gray-400' : 'bg-[#eef2f5] text-gray-600'
        }`}
      >
        <div className="w-24 h-24 rounded-full bg-[#3390ec]/10 flex items-center justify-center mb-4 text-[#3390ec]">
          <Bookmark className="w-10 h-10" />
        </div>
        <h3 className="text-xl font-bold mb-2">
          {isAr ? 'حدد محادثة لبدء التراسل' : 'Select a chat to start messaging'}
        </h3>
        <p className="text-sm max-w-sm text-gray-400 leading-relaxed">
          {isAr
            ? 'يمكنك الدردشة مع جهات الاتصال، متابعة القنوات المفضلة، أو حفظ الملاحظات في الرسائل المحفوظة.'
            : 'Choose from your contacts, channels, or use Saved Messages to store notes.'}
        </p>
      </div>
    );
  }

  // Filter messages if searching in chat
  const displayedMessages = searchWord.trim()
    ? messages.filter((m) => m.text.toLowerCase().includes(searchWord.toLowerCase()))
    : messages;

  // Pinned message
  const pinnedMessage = messages.find((m) => m.isPinned);

  const formatMessageTime = (ts: number) => {
    const d = new Date(ts);
    return d.toLocaleTimeString(isAr ? 'ar-SA' : 'en-US', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    });
  };

  const toggleVoicePlay = (msgId: string) => {
    setPlayingVoiceId(playingVoiceId === msgId ? null : msgId);
  };

  // Jump to replied message
  const handleJumpToMessage = (targetMsgId: string) => {
    const el = document.getElementById(`msg-${targetMsgId}`);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      setHighlightedMsgId(targetMsgId);
      setTimeout(() => setHighlightedMsgId(null), 2000);
    }
  };

  // Right-click context menu
  const handleContextMenu = (e: React.MouseEvent, msg: TelegramMessage) => {
    e.preventDefault();
    e.stopPropagation();
    const x = Math.min(e.clientX, window.innerWidth - 200);
    const y = Math.min(e.clientY, window.innerHeight - 250);
    setContextMenu({ visible: true, x, y, message: msg });
  };

  // Selection mode toggles
  const toggleSelectMessage = (msgId: string) => {
    setSelectedMsgIds((prev) =>
      prev.includes(msgId) ? prev.filter((id) => id !== msgId) : [...prev, msgId]
    );
  };

  const handleCopyMessageText = (text: string) => {
    navigator.clipboard.writeText(text);
    onToast(isAr ? 'تم نسخ النص إلى الحافظة' : 'Text copied to clipboard', 'success');
  };

  const handleCopyMessageLink = (msgId: string) => {
    const link = `https://t.me/${chat.username || chat.id}/${msgId}`;
    navigator.clipboard.writeText(link);
    onToast(isAr ? 'تم نسخ رابط الرسالة' : 'Message link copied', 'success');
  };

  const handleDeleteSelected = () => {
    if (selectedMsgIds.length === 0) return;
    if (onDeleteMultipleMessages) {
      onDeleteMultipleMessages(selectedMsgIds);
    } else {
      selectedMsgIds.forEach((id) => onDeleteMessage(id));
    }
    onToast(
      isAr ? `تم حذف ${selectedMsgIds.length} رسالة` : `Deleted ${selectedMsgIds.length} messages`,
      'success'
    );
    setSelectedMsgIds([]);
    setIsSelectionMode(false);
  };

  return (
    <div
      className={`flex-1 flex flex-col h-full relative overflow-hidden ${
        isDark ? 'bg-[#0e1621]' : 'bg-[#e6ebf0]'
      }`}
    >
      {/* Top Bar Header */}
      <header
        className={`h-16 px-4 flex items-center justify-between border-b shrink-0 z-10 select-none ${
          isDark ? 'bg-[#17212b] border-[#0e1621] text-white' : 'bg-white border-gray-200 text-gray-900'
        }`}
      >
        {isSelectionMode ? (
          /* Selection Mode Header */
          <div className="flex items-center justify-between w-full">
            <div className="flex items-center gap-3">
              <button
                onClick={() => {
                  setIsSelectionMode(false);
                  setSelectedMsgIds([]);
                }}
                className="p-1.5 rounded-full hover:bg-black/10 dark:hover:bg-white/10 transition"
              >
                <X className="w-5 h-5" />
              </button>
              <span className="font-bold text-sm">
                {selectedMsgIds.length} {isAr ? 'رسائل محددة' : 'selected'}
              </span>
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={handleDeleteSelected}
                disabled={selectedMsgIds.length === 0}
                className={`p-2 rounded-full transition flex items-center gap-1.5 text-xs font-semibold ${
                  selectedMsgIds.length > 0
                    ? 'text-red-500 hover:bg-red-500/10'
                    : 'text-gray-400 opacity-50 cursor-not-allowed'
                }`}
                title={isAr ? 'حذف المحدد' : 'Delete'}
              >
                <Trash2 className="w-4 h-4" />
                <span className="hidden sm:inline">{isAr ? 'حذف' : 'Delete'}</span>
              </button>
            </div>
          </div>
        ) : (
          /* Normal Header */
          <>
            {/* Left: Avatar + Title + Status */}
            <div
              onClick={onToggleChatInfo}
              className="flex items-center gap-3 cursor-pointer min-w-0 flex-1 hover:opacity-90 transition"
            >
              {chat.avatarUrl ? (
                <img
                  src={chat.avatarUrl}
                  alt={chat.title}
                  referrerPolicy="no-referrer"
                  className="w-10 h-10 rounded-full object-cover shrink-0 shadow"
                />
              ) : (
                <div
                  className="w-10 h-10 rounded-full flex items-center justify-center text-white font-bold shrink-0 text-sm shadow"
                  style={{ backgroundColor: chat.avatarColor || '#3390ec' }}
                >
                  {chat.type === 'saved' ? <Bookmark className="w-5 h-5 fill-white" /> : chat.title.slice(0, 2)}
                </div>
              )}

              <div className="min-w-0 flex-1">
                <h2 className="font-bold text-sm truncate flex items-center gap-1.5">
                  <span>{chat.title}</span>
                  {chat.isVerified && (
                    <CheckCircle2 className="w-3.5 h-3.5 text-[#3390ec] fill-[#3390ec]/20 shrink-0" />
                  )}
                  {chat.isMuted && <VolumeX className="w-3.5 h-3.5 text-gray-400 shrink-0" />}
                </h2>
                {typingStatus && typingStatus.chatId === chat.id && Date.now() < typingStatus.expiresAt ? (
                  <div className="flex items-center gap-1.5 text-xs text-[#3390ec] font-semibold transition-all">
                    <span className="truncate">
                      {chat.type === 'supergroup' || chat.type === 'group'
                        ? isAr
                          ? `${typingStatus.userName || 'أحد الأعضاء'} يكتب...`
                          : `${typingStatus.userName || 'Someone'} is typing...`
                        : isAr
                        ? 'يكتب الآن...'
                        : 'typing...'}
                    </span>
                    <span className="inline-flex items-center gap-0.5 shrink-0">
                      <span className="w-1.5 h-1.5 rounded-full bg-[#3390ec] animate-bounce [animation-delay:-0.3s]" />
                      <span className="w-1.5 h-1.5 rounded-full bg-[#3390ec] animate-bounce [animation-delay:-0.15s]" />
                      <span className="w-1.5 h-1.5 rounded-full bg-[#3390ec] animate-bounce" />
                    </span>
                  </div>
                ) : (
                  <p className="text-xs text-gray-400 truncate">
                    {chat.type === 'saved'
                      ? isAr
                        ? 'مساحتك السحابية الخاصة'
                        : 'Your cloud storage'
                      : chat.type === 'channel'
                      ? isAr
                        ? `${chat.membersCount?.toLocaleString() || '48,920'} مشترك`
                        : `${chat.membersCount?.toLocaleString() || '48,920'} subscribers`
                      : chat.type === 'supergroup' || chat.type === 'group'
                      ? isAr
                        ? `${chat.membersCount?.toLocaleString() || '1,420'} عضو`
                        : `${chat.membersCount?.toLocaleString() || '1,420'} members`
                      : chat.isOnline
                      ? isAr
                        ? 'متصل الآن'
                        : 'online'
                      : isAr
                      ? 'آخر ظهور مؤخراً'
                      : 'last seen recently'}
                  </p>
                )}
              </div>
            </div>

            {/* Right Header Actions */}
            <div className="flex items-center gap-1">
              {/* Quick MTProto Typing Simulator Button */}
              {onSimulateTyping && chat.type !== 'saved' && (
                <button
                  onClick={onSimulateTyping}
                  className={`flex items-center gap-1 px-2.5 py-1 text-xs rounded-full border transition font-medium ${
                    typingStatus && typingStatus.chatId === chat.id && Date.now() < typingStatus.expiresAt
                      ? 'bg-[#3390ec] text-white border-[#3390ec] shadow-sm animate-pulse'
                      : isDark
                      ? 'bg-[#242f3d]/70 text-[#3390ec] border-[#3390ec]/30 hover:bg-[#3390ec]/20'
                      : 'bg-blue-50 text-[#3390ec] border-blue-200 hover:bg-blue-100'
                  }`}
                  title={isAr ? 'محاكاة نشاط الطرف الآخر (MTProto)' : 'Simulate MTProto activity'}
                >
                  <Sparkles className="w-3.5 h-3.5 shrink-0" />
                  <span className="hidden sm:inline">
                    {isAr ? 'محاكاة نشاط' : 'Simulate'}
                  </span>
                </button>
              )}

              {/* In-chat search */}
              {inChatSearch ? (
                <div className="flex items-center gap-1 bg-[#242f3d]/30 px-2 py-1 rounded-lg">
                  <input
                    type="text"
                    value={searchWord}
                    onChange={(e) => setSearchWord(e.target.value)}
                    placeholder={isAr ? 'بحث في الرسائل...' : 'Search in chat...'}
                    className="bg-transparent text-xs text-white focus:outline-none w-32"
                    autoFocus
                  />
                  <button
                    onClick={() => {
                      setInChatSearch(false);
                      setSearchWord('');
                    }}
                    className="text-gray-400 hover:text-white"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>
              ) : (
                <button
                  onClick={() => setInChatSearch(true)}
                  className={`p-2 rounded-full transition ${
                    isDark ? 'hover:bg-[#232e3c] text-gray-300' : 'hover:bg-gray-100 text-gray-600'
                  }`}
                  title={isAr ? 'بحث في المحادثة' : 'Search'}
                >
                  <Search className="w-5 h-5" />
                </button>
              )}

              {/* Mute Toggle Button */}
              <button
                onClick={() => onToggleMute(chat.id)}
                className={`p-2 rounded-full transition ${
                  isDark ? 'hover:bg-[#232e3c] text-gray-300' : 'hover:bg-gray-100 text-gray-600'
                }`}
                title={chat.isMuted ? (isAr ? 'إلغاء الكتم' : 'Unmute') : isAr ? 'كتم الإشعارات' : 'Mute'}
              >
                {chat.isMuted ? <VolumeX className="w-5 h-5 text-amber-400" /> : <Volume2 className="w-5 h-5" />}
              </button>

              {/* Chat Info Drawer Toggle */}
              <button
                onClick={onToggleChatInfo}
                className={`p-2 rounded-full transition ${
                  isDark ? 'hover:bg-[#232e3c] text-gray-300' : 'hover:bg-gray-100 text-gray-600'
                }`}
                title={isAr ? 'معلومات المحادثة' : 'Chat info'}
              >
                {isChatInfoOpen ? <PanelRightClose className="w-5 h-5" /> : <PanelRightOpen className="w-5 h-5" />}
              </button>

              {/* Mini Apps Launcher */}
              {onOpenMiniApp && (
                <button
                  onClick={onOpenMiniApp}
                  className={`p-2 rounded-full transition ${
                    isDark ? 'hover:bg-[#232e3c] text-amber-400' : 'hover:bg-gray-100 text-amber-500'
                  }`}
                  title={isAr ? 'تطبيقات الويب المصغرة' : 'Telegram Mini Apps'}
                >
                  <Sparkles className="w-5 h-5" />
                </button>
              )}

              {/* Official Telegram Web K 3-Dots More Menu */}
              <div className="relative">
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setShowMoreMenu(!showMoreMenu);
                  }}
                  className={`p-2 rounded-full transition ${
                    isDark ? 'hover:bg-[#232e3c] text-gray-300' : 'hover:bg-gray-100 text-gray-600'
                  }`}
                  title={isAr ? 'خيارات إضافية' : 'More options'}
                >
                  <MoreVertical className="w-5 h-5" />
                </button>

                {showMoreMenu && (
                  <div
                    onClick={(e) => e.stopPropagation()}
                    className={`absolute end-0 top-12 rounded-2xl shadow-2xl py-2 w-52 z-30 border backdrop-blur-md animate-scale-in ${
                      isDark ? 'bg-[#242f3d]/95 border-[#2f3f50] text-white' : 'bg-white/95 border-gray-200 text-gray-800'
                    }`}
                  >
                    {/* Search */}
                    <button
                      onClick={() => {
                        setInChatSearch(true);
                        setShowMoreMenu(false);
                      }}
                      className={`w-full text-start px-4 py-2 text-xs flex items-center gap-3 ${
                        isDark ? 'hover:bg-[#2b394a]' : 'hover:bg-gray-100'
                      }`}
                    >
                      <Search className="w-4 h-4 text-gray-400" />
                      <span>{isAr ? 'بحث في المحادثة' : 'Search in chat'}</span>
                    </button>

                    {/* Mute */}
                    <button
                      onClick={() => {
                        onToggleMute(chat.id);
                        setShowMoreMenu(false);
                      }}
                      className={`w-full text-start px-4 py-2 text-xs flex items-center gap-3 ${
                        isDark ? 'hover:bg-[#2b394a]' : 'hover:bg-gray-100'
                      }`}
                    >
                      {chat.isMuted ? (
                        <>
                          <Volume2 className="w-4 h-4 text-gray-400" />
                          <span>{isAr ? 'إلغاء كتم الصوت' : 'Unmute'}</span>
                        </>
                      ) : (
                        <>
                          <VolumeX className="w-4 h-4 text-gray-400" />
                          <span>{isAr ? 'كتم الإشعارات' : 'Mute notifications'}</span>
                        </>
                      )}
                    </button>

                    {/* Copy / Share Link for groups and channels */}
                    {(chat.type === 'group' || chat.type === 'supergroup' || chat.type === 'channel') && (
                      <button
                        onClick={() => {
                          setIsShareModalOpen(true);
                          setShowMoreMenu(false);
                        }}
                        className={`w-full text-start px-4 py-2 text-xs flex items-center gap-3 ${
                          isDark ? 'hover:bg-[#2b394a]' : 'hover:bg-gray-100'
                        }`}
                      >
                        <Share2 className="w-4 h-4 text-[#3390ec]" />
                        <span>{isAr ? 'مشاركة رابط المجموعة' : 'Share group link'}</span>
                      </button>
                    )}

                    {/* Select Messages Mode */}
                    <button
                      onClick={() => {
                        setIsSelectionMode(true);
                        setShowMoreMenu(false);
                      }}
                      className={`w-full text-start px-4 py-2 text-xs flex items-center gap-3 ${
                        isDark ? 'hover:bg-[#2b394a]' : 'hover:bg-gray-100'
                      }`}
                    >
                      <CheckSquare className="w-4 h-4 text-gray-400" />
                      <span>{isAr ? 'تحديد الرسائل' : 'Select messages'}</span>
                    </button>

                    <div className="my-1 border-t border-gray-700/20" />

                    {/* Clear History */}
                    <button
                      onClick={() => {
                        setIsClearModalOpen(true);
                        setShowMoreMenu(false);
                      }}
                      className={`w-full text-start px-4 py-2 text-xs flex items-center gap-3 ${
                        isDark ? 'hover:bg-[#2b394a]' : 'hover:bg-gray-100'
                      }`}
                    >
                      <Trash2 className="w-4 h-4 text-gray-400" />
                      <span>{isAr ? 'مسح سجل المحادثة' : 'Clear history'}</span>
                    </button>

                    {/* Report Chat */}
                    {chat.type !== 'saved' && (
                      <button
                        onClick={() => {
                          setIsReportModalOpen(true);
                          setShowMoreMenu(false);
                        }}
                        className={`w-full text-start px-4 py-2 text-xs flex items-center gap-3 ${
                          isDark ? 'hover:bg-[#2b394a]' : 'hover:bg-gray-100'
                        }`}
                      >
                        <Flag className="w-4 h-4 text-amber-400" />
                        <span>{isAr ? 'الإبلاغ' : 'Report'}</span>
                      </button>
                    )}

                    {/* Leave Group / Delete and Exit */}
                    {chat.type !== 'saved' && (
                      <button
                        onClick={() => {
                          setIsLeaveModalOpen(true);
                          setShowMoreMenu(false);
                        }}
                        className={`w-full text-start px-4 py-2 text-xs flex items-center gap-3 text-red-500 ${
                          isDark ? 'hover:bg-[#2b394a]' : 'hover:bg-gray-100'
                        }`}
                      >
                        <LogOut className="w-4 h-4 text-red-500" />
                        <span>
                          {chat.type === 'channel'
                            ? isAr
                              ? 'مغادرة القناة'
                              : 'Leave channel'
                            : chat.type === 'group' || chat.type === 'supergroup'
                            ? isAr
                              ? 'مغادرة المجموعة'
                              : 'Leave group'
                            : isAr
                            ? 'حذف المحادثة'
                            : 'Delete chat'}
                        </span>
                      </button>
                    )}
                  </div>
                )}
              </div>
            </div>
          </>
        )}
      </header>

      {/* Pinned Message Banner */}
      {pinnedMessage && (
        <div
          onClick={() => handleJumpToMessage(pinnedMessage.id)}
          className={`px-4 py-2 flex items-center justify-between text-xs border-b select-none shadow-xs z-10 cursor-pointer transition ${
            isDark
              ? 'bg-[#1e2a38] hover:bg-[#233142] border-[#0e1621] text-gray-200'
              : 'bg-[#f4f7f9] hover:bg-[#ebf0f4] border-gray-200 text-gray-800'
          }`}
        >
          <div className="flex items-center gap-2 min-w-0 flex-1">
            <Pin className="w-4 h-4 text-[#3390ec] shrink-0 fill-current" />
            <div className="min-w-0">
              <span className="font-bold text-[#3390ec] block text-[11px]">
                {isAr ? 'رسالة مثبتة' : 'Pinned Message'}
              </span>
              <p className="truncate text-gray-400 text-xs">{pinnedMessage.text}</p>
            </div>
          </div>
          <button
            onClick={(e) => {
              e.stopPropagation();
              onPinMessage(pinnedMessage.id);
            }}
            className="text-gray-400 hover:text-white p-1 rounded-full"
            title={isAr ? 'إلغاء التثبيت' : 'Unpin'}
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Messages Scroll Area with Telegram Web K Layout Geometry */}
      <div
        ref={scrollContainerRef}
        onScroll={handleScroll}
        className="flex-1 overflow-y-auto px-3 sm:px-4 py-3 relative space-y-1"
      >
        {/* Telegram Web K Bot Profile Intro Card ("What can this bot do?") */}
        {chat && (chat.type === 'bot' || chat.isBot) && (
          <div
            className={`max-w-md mx-auto my-4 p-5 rounded-2xl border text-center shadow-lg backdrop-blur-md select-none transition-all ${
              isDark ? 'bg-[#17212b]/90 border-[#2f3f50] text-white' : 'bg-white/90 border-gray-200 text-gray-800'
            }`}
          >
            <div className="w-16 h-16 mx-auto rounded-full bg-[#3390ec]/15 text-[#3390ec] flex items-center justify-center font-bold text-2xl mb-3 shadow-inner">
              {chat.avatarUrl ? (
                <img
                  src={chat.avatarUrl}
                  alt={chat.name}
                  referrerPolicy="no-referrer"
                  className="w-full h-full rounded-full object-cover"
                />
              ) : (
                <Bot className="w-8 h-8 text-[#3390ec]" />
              )}
            </div>
            <div className="flex items-center justify-center gap-1.5 mb-1">
              <h3 className="font-bold text-base">{chat.name}</h3>
              <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-[#3390ec]/20 text-[#3390ec]">
                bot
              </span>
            </div>
            <p className="text-xs text-[#3390ec] font-semibold mb-2">
              {isAr ? 'ماذا يستطيع هذا البوت فعله؟' : 'What can this bot do?'}
            </p>
            <p className="text-xs text-gray-400 whitespace-pre-wrap leading-relaxed mb-4">
              {chat.botInfo?.description ||
                chat.bio ||
                (isAr
                  ? 'منصة بوتات تيليجرام الرسمية تدعم الردود التفاعلية وتطبيقات الويب المصغرة وأزرار الـ Inline.'
                  : 'Official Telegram bot platform supporting interactive inline replies, mini apps, and bot commands.')}
            </p>

            {chat.botInfo?.commands && chat.botInfo.commands.length > 0 && (
              <div
                className={`text-start rounded-xl p-3 mb-3 space-y-1.5 text-xs border ${
                  isDark ? 'bg-[#1c2733]/80 border-[#2f3f50]' : 'bg-gray-50 border-gray-200'
                }`}
              >
                <p className="text-[11px] font-bold text-gray-400 uppercase tracking-wider mb-1">
                  {isAr ? 'الأوامر الشائعة:' : 'Common Commands:'}
                </p>
                {chat.botInfo.commands.slice(0, 4).map((cmd) => (
                  <button
                    key={cmd.command}
                    type="button"
                    onClick={() => onSendMessage(`/${cmd.command}`)}
                    className="w-full flex items-center justify-between hover:text-[#3390ec] py-0.5 text-start transition"
                  >
                    <span className="font-mono text-[#3390ec] font-semibold">/{cmd.command}</span>
                    <span className="text-gray-400 text-[11px] truncate ms-2">{cmd.description}</span>
                  </button>
                ))}
              </div>
            )}

            <button
              type="button"
              onClick={() => onSendMessage('/start')}
              className="w-full py-2.5 px-4 rounded-xl bg-[#3390ec] hover:bg-[#2881da] active:scale-98 text-white font-bold text-xs uppercase tracking-wider shadow-md transition flex items-center justify-center gap-2"
            >
              <span>{isAr ? 'بدء المحادثة (/start)' : 'START BOT (/start)'}</span>
            </button>
          </div>
        )}

        {displayedMessages.map((msg, index) => {
          const isOut = msg.isOut;
          const prevMsg = displayedMessages[index - 1];
          const nextMsg = displayedMessages[index + 1];

          // Consecutive grouping logic (Web K algorithm)
          const isSameSenderPrev =
            prevMsg &&
            prevMsg.senderId === msg.senderId &&
            Math.abs(msg.timestamp - prevMsg.timestamp) < 5 * 60 * 1000;

          const isSameSenderNext =
            nextMsg &&
            nextMsg.senderId === msg.senderId &&
            Math.abs(nextMsg.timestamp - msg.timestamp) < 5 * 60 * 1000;

          const isFirstInGroup = !isSameSenderPrev;
          const isLastInGroup = !isSameSenderNext;

          // Date Separator calculation
          const showDateSeparator =
            !prevMsg ||
            new Date(prevMsg.timestamp).toDateString() !== new Date(msg.timestamp).toDateString();

          const isHovered = hoveredMessageId === msg.id;
          const isSelected = selectedMsgIds.includes(msg.id);
          const isHighlighted = highlightedMsgId === msg.id;

          const senderColor = getSenderColor(msg.senderId);

          return (
            <React.Fragment key={msg.id}>
              {/* Centered Date Separator Pill */}
              {showDateSeparator && (
                <div className="flex justify-center my-3 sticky top-2 z-10 select-none">
                  <span
                    className={`inline-block px-3 py-0.5 rounded-full text-[11px] font-semibold shadow-xs backdrop-blur-md ${
                      isDark ? 'bg-[#182533]/85 text-gray-300' : 'bg-gray-300/80 text-gray-700'
                    }`}
                  >
                    {formatTelegramDate(msg.timestamp, lang)}
                  </span>
                </div>
              )}

              {/* Message Row */}
              <div
                id={`msg-${msg.id}`}
                onMouseEnter={() => setHoveredMessageId(msg.id)}
                onMouseLeave={() => setHoveredMessageId(null)}
                onContextMenu={(e) => handleContextMenu(e, msg)}
                onClick={() => {
                  if (isSelectionMode) {
                    toggleSelectMessage(msg.id);
                  }
                }}
                className={`flex items-end gap-2 group relative transition-colors ${
                  isLastInGroup ? 'mb-2.5' : 'mb-0.5'
                } ${isOut ? 'justify-end' : 'justify-start'} ${
                  isSelectionMode ? 'cursor-pointer hover:bg-[#3390ec]/5 p-1 rounded-xl' : ''
                } ${isSelected ? 'bg-[#3390ec]/15 rounded-xl' : ''} ${
                  isHighlighted ? 'ring-2 ring-[#3390ec] rounded-2xl animate-pulse' : ''
                }`}
              >
                {/* Selection Checkbox (Web K style) */}
                {isSelectionMode && (
                  <div className="shrink-0 self-center">
                    <div
                      className={`w-5 h-5 rounded-full border-2 flex items-center justify-center transition-all ${
                        isSelected
                          ? 'bg-[#3390ec] border-[#3390ec] text-white'
                          : isDark
                          ? 'border-gray-500 bg-transparent'
                          : 'border-gray-400 bg-transparent'
                      }`}
                    >
                      {isSelected && <Check className="w-3.5 h-3.5 stroke-[3]" />}
                    </div>
                  </div>
                )}

                {/* Left Avatar for Incoming Group/Supergroup/Channel Messages */}
                {!isOut && chat.type !== 'private' && (
                  <div className="w-8 shrink-0 select-none">
                    {isLastInGroup ? (
                      msg.senderAvatar ? (
                        <img
                          src={msg.senderAvatar}
                          alt={msg.senderName}
                          referrerPolicy="no-referrer"
                          className="w-8 h-8 rounded-full object-cover shadow-sm"
                        />
                      ) : (
                        <div
                          className="w-8 h-8 rounded-full flex items-center justify-center text-white text-[11px] font-bold shadow-sm"
                          style={{ backgroundColor: senderColor }}
                        >
                          {msg.senderName.slice(0, 1)}
                        </div>
                      )
                    ) : (
                      <div className="w-8" />
                    )}
                  </div>
                )}

                {/* Bubble Container */}
                <div
                  className={`max-w-[85%] md:max-w-[68%] relative shadow-xs transition-all text-sm ${
                    /* Outgoing bubble colors and borders */
                    isOut
                      ? isDark
                        ? 'bg-[#2b5278] text-white'
                        : 'bg-[#eeffde] text-gray-900 border border-[#d2ecbb]/50'
                      : isDark
                      ? 'bg-[#182533] text-gray-100'
                      : 'bg-white text-gray-900 border border-gray-200/60'
                  } ${
                    /* Telegram Web K rounded geometry & tails */
                    isOut
                      ? `rounded-2xl ${isLastInGroup ? 'rounded-br-xs' : 'rounded-br-lg'} ${
                          !isFirstInGroup ? 'rounded-tr-lg' : ''
                        }`
                      : `rounded-2xl ${isLastInGroup ? 'rounded-bl-xs' : 'rounded-bl-lg'} ${
                          !isFirstInGroup ? 'rounded-tl-lg' : ''
                        }`
                  } px-3 pt-2 pb-1.5`}
                >
                  {/* Sender Name in Groups (with official Telegram Color) */}
                  {!isOut && isFirstInGroup && chat.type !== 'private' && (
                    <span
                      className="text-xs font-bold block mb-1 hover:underline cursor-pointer select-none"
                      style={{ color: senderColor }}
                    >
                      {msg.senderName}
                    </span>
                  )}

                  {/* Forwarded Header */}
                  {msg.isForwarded && (
                    <div className="text-[11px] text-[#3390ec] font-semibold flex items-center gap-1 mb-1 select-none">
                      <Forward className="w-3.5 h-3.5" />
                      <span>
                        {isAr ? 'محولة من' : 'Forwarded from'} {msg.forwardedFrom || 'تيليجرام'}
                      </span>
                    </div>
                  )}

                  {/* Reply Quote Block (Clickable to jump) */}
                  {msg.replyTo && (
                    <div
                      onClick={() => handleJumpToMessage(msg.replyTo!.id)}
                      className={`mb-2 p-2 rounded-lg text-xs border-s-3 border-[#3390ec] cursor-pointer hover:opacity-90 select-none transition ${
                        isDark ? 'bg-black/25' : 'bg-black/5'
                      }`}
                    >
                      <span className="font-bold text-[#3390ec] block">
                        {msg.replyTo.senderName}
                      </span>
                      <p className="truncate opacity-80">{msg.replyTo.text}</p>
                    </div>
                  )}

                  {/* Photo / Video Media */}
                  {msg.media?.type === 'photo' && msg.media.url && (
                    <div className="mb-1.5 rounded-xl overflow-hidden cursor-pointer">
                      <img
                        src={msg.media.url}
                        alt="Telegram media"
                        referrerPolicy="no-referrer"
                        onClick={() => onOpenMediaViewer(msg.media!.url!, msg.media?.title)}
                        className="w-full max-h-72 object-cover rounded-xl hover:opacity-95 transition"
                      />
                    </div>
                  )}

                  {/* Voice Note Audio */}
                  {msg.media?.type === 'voice' && (
                    <VoiceWaveformPlayer
                      url={msg.media.url}
                      duration={msg.media.duration}
                      isOut={msg.isOut}
                      isDark={isDark}
                    />
                  )}

                  {/* Document File */}
                  {msg.media?.type === 'document' && (
                    <div
                      className={`flex items-center gap-3 p-2 rounded-xl mb-1.5 ${
                        isDark ? 'bg-black/20' : 'bg-black/5'
                      }`}
                    >
                      <div className="w-9 h-9 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center shrink-0">
                        <FileText className="w-5 h-5" />
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="text-xs font-semibold truncate">{msg.media.fileName}</p>
                        <span className="text-[10px] text-gray-400">{msg.media.fileSize}</span>
                      </div>
                      {msg.media.url && (
                        <a
                          href={msg.media.url}
                          download={msg.media.fileName}
                          className="p-1.5 text-gray-400 hover:text-white"
                        >
                          <Download className="w-4 h-4" />
                        </a>
                      )}
                    </div>
                  )}

                  {/* Message Text with responsive wrapping */}
                  <p className="text-[14px] sm:text-[15px] whitespace-pre-wrap break-words leading-relaxed select-text">
                    {msg.text}
                  </p>

                  {/* Inline Footer Time + Status checkmarks */}
                  <div className="flex items-center justify-end gap-1 mt-0.5 text-[10px] text-gray-400 select-none float-end ms-2">
                    <span>{formatMessageTime(msg.timestamp)}</span>
                    {isOut && (
                      <span className="inline-flex">
                        {msg.status === 'read' ? (
                          <CheckCheck className="w-3.5 h-3.5 text-emerald-400" />
                        ) : (
                          <Check className="w-3.5 h-3.5 text-gray-400" />
                        )}
                      </span>
                    )}
                  </div>
                  <div className="clear-both" />

                  {/* Emoji Reactions Pills under message */}
                  {msg.reactions && msg.reactions.length > 0 && (
                    <div className="flex flex-wrap gap-1 mt-1.5 pt-1 select-none">
                      {msg.reactions.map((r, rIdx) => (
                        <button
                          key={rIdx}
                          onClick={() => onReactMessage(msg.id, r.emoji)}
                          className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs transition border ${
                            r.userReacted
                              ? 'bg-[#3390ec]/20 border-[#3390ec] text-[#3390ec] font-bold'
                              : isDark
                              ? 'bg-[#17212b] border-[#2f3f50] text-gray-300 hover:bg-[#232e3c]'
                              : 'bg-white border-gray-200 text-gray-700 hover:bg-gray-100'
                          }`}
                        >
                          <span>{r.emoji}</span>
                          <span className="text-[11px] font-semibold">{r.count}</span>
                        </button>
                      ))}
                    </div>
                  )}

                  {/* Telegram Web K Inline Keyboard (InlineKeyboardMarkup) */}
                  {msg.replyMarkup?.inlineKeyboard && msg.replyMarkup.inlineKeyboard.length > 0 && (
                    <div className="mt-2 space-y-1 select-none">
                      {msg.replyMarkup.inlineKeyboard.map((row, rIdx) => (
                        <div key={rIdx} className="flex gap-1 w-full">
                          {row.map((btn, bIdx) => (
                            <button
                              key={bIdx}
                              type="button"
                              onClick={(e) => {
                                e.stopPropagation();
                                if (btn.webApp) {
                                  onOpenMiniApp ? onOpenMiniApp(btn.webApp.url, btn.text) : window.open(btn.webApp.url, '_blank');
                                } else if (btn.url) {
                                  window.open(btn.url, '_blank', 'noopener,noreferrer');
                                } else if (btn.callbackData) {
                                  if (onBotCallback) {
                                    onBotCallback(msg.id, btn.callbackData);
                                  } else {
                                    onToast(isAr ? `تم إرسال الاستجابة: ${btn.text}` : `Callback sent: ${btn.text}`, 'info');
                                  }
                                } else if (btn.switchInlineQuery !== undefined) {
                                  onSendMessage(`@${chat?.username || chat?.id || 'bot'} ${btn.switchInlineQuery}`);
                                }
                              }}
                              className={`flex-1 py-1.5 px-2.5 rounded-xl text-xs font-semibold text-center transition flex items-center justify-center gap-1.5 shadow-xs border ${
                                isDark
                                  ? 'bg-[#242f3d] hover:bg-[#2e3c4e] text-[#3390ec] border-[#2f3f50]'
                                  : 'bg-white hover:bg-gray-50 text-[#3390ec] border-gray-200'
                              }`}
                            >
                              {btn.webApp && <Sparkles className="w-3.5 h-3.5 text-amber-400 shrink-0" />}
                              {btn.url && <ExternalLink className="w-3.5 h-3.5 text-gray-400 shrink-0" />}
                              <span className="truncate">{btn.text}</span>
                            </button>
                          ))}
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                {/* Floating Quick Action Bar on Hover (Reply, Reactions, More) */}
                {isHovered && !isSelectionMode && (
                  <div
                    className={`absolute -top-3.5 z-20 flex items-center gap-0.5 p-1 rounded-full shadow-lg border backdrop-blur-md animate-fade-in ${
                      isOut ? 'end-4' : 'start-4'
                    } ${
                      isDark
                        ? 'bg-[#17212b]/95 border-[#2f3f50] text-gray-300'
                        : 'bg-white/95 border-gray-200 text-gray-700'
                    }`}
                  >
                    {/* Emoji reactions bar */}
                    <div className="flex items-center gap-0.5 px-1 border-e border-gray-600/30">
                      {['❤️', '👍', '🔥', '😂', '🎉', '👏'].map((em) => (
                        <button
                          key={em}
                          onClick={() => onReactMessage(msg.id, em)}
                          className="hover:scale-130 transition-transform p-0.5 text-sm"
                        >
                          {em}
                        </button>
                      ))}
                    </div>

                    {/* Reply */}
                    <button
                      onClick={() => setReplyingMessage(msg)}
                      className="p-1 rounded-full hover:bg-gray-500/20"
                      title={isAr ? 'رد' : 'Reply'}
                    >
                      <Reply className="w-3.5 h-3.5" />
                    </button>

                    {/* Copy Text */}
                    <button
                      onClick={() => handleCopyMessageText(msg.text)}
                      className="p-1 rounded-full hover:bg-gray-500/20"
                      title={isAr ? 'نسخ' : 'Copy'}
                    >
                      <Copy className="w-3.5 h-3.5" />
                    </button>

                    {/* Pin */}
                    <button
                      onClick={() => onPinMessage(msg.id)}
                      className="p-1 rounded-full hover:bg-gray-500/20"
                      title={isAr ? 'تثبيت' : 'Pin'}
                    >
                      <Pin className="w-3.5 h-3.5" />
                    </button>

                    {/* Delete */}
                    {isOut && (
                      <button
                        onClick={() => setDeleteModalMsgId(msg.id)}
                        className="p-1 rounded-full hover:bg-red-500/20 text-red-400"
                        title={isAr ? 'حذف' : 'Delete'}
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    )}
                  </div>
                )}
              </div>
            </React.Fragment>
          );
        })}

        <div ref={messagesEndRef} />
      </div>

      {/* Floating Scroll to Bottom Button with Unread Badge */}
      {showScrollBottom && (
        <button
          onClick={scrollToBottom}
          className="absolute bottom-20 end-5 z-20 w-10 h-10 rounded-full bg-[#17212b]/90 text-[#3390ec] shadow-xl border border-gray-700/40 flex items-center justify-center hover:scale-110 active:scale-95 transition-all backdrop-blur-md"
          title={isAr ? 'الانتقال لأسفل المحادثة' : 'Scroll to bottom'}
        >
          <ChevronDown className="w-5 h-5" />
        </button>
      )}

      {/* Context Menu Dropdown (Right-click on message) */}
      {contextMenu.visible && contextMenu.message && (
        <div
          style={{ top: contextMenu.y, left: contextMenu.x }}
          onClick={(e) => e.stopPropagation()}
          className={`fixed z-50 rounded-2xl shadow-2xl py-1.5 w-48 border backdrop-blur-md animate-scale-in select-none ${
            isDark ? 'bg-[#242f3d]/95 border-[#2f3f50] text-white' : 'bg-white/95 border-gray-200 text-gray-800'
          }`}
        >
          <button
            onClick={() => {
              setReplyingMessage(contextMenu.message);
              setContextMenu({ visible: false, x: 0, y: 0, message: null });
            }}
            className={`w-full text-start px-3.5 py-2 text-xs flex items-center gap-2.5 ${
              isDark ? 'hover:bg-[#2b394a]' : 'hover:bg-gray-100'
            }`}
          >
            <Reply className="w-4 h-4 text-[#3390ec]" />
            <span>{isAr ? 'رد' : 'Reply'}</span>
          </button>

          <button
            onClick={() => {
              handleCopyMessageText(contextMenu.message!.text);
              setContextMenu({ visible: false, x: 0, y: 0, message: null });
            }}
            className={`w-full text-start px-3.5 py-2 text-xs flex items-center gap-2.5 ${
              isDark ? 'hover:bg-[#2b394a]' : 'hover:bg-gray-100'
            }`}
          >
            <Copy className="w-4 h-4 text-gray-400" />
            <span>{isAr ? 'نسخ النص' : 'Copy text'}</span>
          </button>

          <button
            onClick={() => {
              handleCopyMessageLink(contextMenu.message!.id);
              setContextMenu({ visible: false, x: 0, y: 0, message: null });
            }}
            className={`w-full text-start px-3.5 py-2 text-xs flex items-center gap-2.5 ${
              isDark ? 'hover:bg-[#2b394a]' : 'hover:bg-gray-100'
            }`}
          >
            <ExternalLink className="w-4 h-4 text-gray-400" />
            <span>{isAr ? 'نسخ رابط الرسالة' : 'Copy link'}</span>
          </button>

          <button
            onClick={() => {
              onPinMessage(contextMenu.message!.id);
              setContextMenu({ visible: false, x: 0, y: 0, message: null });
            }}
            className={`w-full text-start px-3.5 py-2 text-xs flex items-center gap-2.5 ${
              isDark ? 'hover:bg-[#2b394a]' : 'hover:bg-gray-100'
            }`}
          >
            <Pin className="w-4 h-4 text-gray-400" />
            <span>
              {contextMenu.message.isPinned
                ? isAr
                  ? 'إلغاء التثبيت'
                  : 'Unpin'
                : isAr
                ? 'تثبيت الرسالة'
                : 'Pin'}
            </span>
          </button>

          <button
            onClick={() => {
              setIsSelectionMode(true);
              setSelectedMsgIds([contextMenu.message!.id]);
              setContextMenu({ visible: false, x: 0, y: 0, message: null });
            }}
            className={`w-full text-start px-3.5 py-2 text-xs flex items-center gap-2.5 ${
              isDark ? 'hover:bg-[#2b394a]' : 'hover:bg-gray-100'
            }`}
          >
            <CheckSquare className="w-4 h-4 text-gray-400" />
            <span>{isAr ? 'تحديد' : 'Select'}</span>
          </button>

          <div className="my-1 border-t border-gray-700/20" />

          {contextMenu.message.isOut && (
            <button
              onClick={() => {
                setDeleteModalMsgId(contextMenu.message!.id);
                setContextMenu({ visible: false, x: 0, y: 0, message: null });
              }}
              className={`w-full text-start px-3.5 py-2 text-xs flex items-center gap-2.5 text-red-400 ${
                isDark ? 'hover:bg-[#2b394a]' : 'hover:bg-gray-100'
              }`}
            >
              <Trash2 className="w-4 h-4 text-red-400" />
              <span>{isAr ? 'حذف' : 'Delete'}</span>
            </button>
          )}
        </div>
      )}

      {/* Bottom Action Bar: JOIN CHANNEL or MUTE/UNMUTE or Message Input */}
      {chat.type === 'channel' && chat.isJoined === false ? (
        <div
          className={`w-full px-4 py-3 border-t flex flex-col sm:flex-row items-center justify-between gap-3 backdrop-blur-md transition-colors ${
            isDark ? 'bg-[#17212b]/95 border-[#0e1621]' : 'bg-white/95 border-gray-200'
          }`}
        >
          <div className="flex items-center gap-2 text-xs text-gray-400">
            <Radio className="w-4 h-4 text-[#3390ec]" />
            <span>
              {isAr
                ? 'أنت في وضع معاينة القناة. انضم لتصلك منشورات وتحديثات القناة.'
                : 'Preview mode. Join to receive posts and updates from this channel.'}
            </span>
          </div>
          <button
            onClick={() => onJoinChannel && onJoinChannel(chat.id)}
            disabled={isJoiningChannel}
            className="w-full sm:w-auto min-w-[200px] py-2.5 px-6 rounded-xl bg-[#3390ec] hover:bg-[#2b7ec9] active:scale-98 text-white font-bold text-xs uppercase tracking-wider flex items-center justify-center gap-2 shadow-lg shadow-[#3390ec]/25 transition disabled:opacity-60"
          >
            {isJoiningChannel ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>{isAr ? 'جارٍ الانضمام...' : 'JOINING...'}</span>
              </>
            ) : (
              <>
                <UserPlus className="w-4 h-4" />
                <span>{isAr ? 'الانضمام إلى القناة' : 'JOIN CHANNEL'}</span>
              </>
            )}
          </button>
        </div>
      ) : chat.type === 'channel' && chat.isJoined !== false ? (
        <div
          className={`w-full px-4 py-2.5 border-t flex items-center justify-between gap-3 backdrop-blur-md transition-colors ${
            isDark ? 'bg-[#17212b]/95 border-[#0e1621]' : 'bg-white/95 border-gray-200'
          }`}
        >
          <div className="text-xs text-gray-400 flex items-center gap-2">
            <Radio className="w-3.5 h-3.5 text-[#3390ec]" />
            <span>
              {isAr
                ? 'قناة إذاعية عامة • تُنشر المنشورات بواسطة الإدارة'
                : 'Broadcast channel • Only admins can post'}
            </span>
          </div>

          <button
            onClick={() => onToggleMute(chat.id)}
            className={`px-4 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-2 transition ${
              chat.isMuted
                ? isDark
                  ? 'bg-[#242f3d] text-gray-300 hover:bg-[#2b394a]'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                : 'bg-[#3390ec]/10 text-[#3390ec] hover:bg-[#3390ec]/20'
            }`}
          >
            {chat.isMuted ? (
              <>
                <Volume2 className="w-4 h-4" />
                <span>{isAr ? 'إلغاء كتم الإشعارات' : 'UNMUTE'}</span>
              </>
            ) : (
              <>
                <VolumeX className="w-4 h-4" />
                <span>{isAr ? 'كتم الإشعارات' : 'MUTE'}</span>
              </>
            )}
          </button>
        </div>
      ) : (
        /* Bottom Message Input Bar */
        <MessageInput
          onSendMessage={onSendMessage}
          replyToMessage={replyingMessage}
          onCancelReply={() => setReplyingMessage(null)}
          onOpenMiniApp={onOpenMiniApp}
          chat={chat}
          activeReplyKeyboard={
            [...messages].reverse().find((m) => m.replyMarkup?.keyboard)?.replyMarkup || null
          }
          lang={lang}
          isDark={isDark}
        />
      )}

      {/* Modals */}
      {/* 1. Clear History Modal */}
      <ClearHistoryModal
        isOpen={isClearModalOpen}
        onClose={() => setIsClearModalOpen(false)}
        onConfirm={(alsoForEveryone) => {
          onClearHistory(chat.id, alsoForEveryone);
          onToast(isAr ? 'تم مسح سجل المحادثة' : 'Chat history cleared', 'info');
        }}
        chat={chat}
        lang={lang}
        isDark={isDark}
      />

      {/* 2. Leave Group Modal */}
      <LeaveGroupModal
        isOpen={isLeaveModalOpen}
        onClose={() => setIsLeaveModalOpen(false)}
        onConfirm={() => {
          onLeaveGroup(chat.id);
          onToast(isAr ? 'تمت مغادرة المجموعة' : 'Left the group', 'info');
        }}
        chat={chat}
        lang={lang}
        isDark={isDark}
      />

      {/* 3. Share Link Modal */}
      <ShareLinkModal
        isOpen={isShareModalOpen}
        onClose={() => setIsShareModalOpen(false)}
        chat={chat}
        onToast={onToast}
        lang={lang}
        isDark={isDark}
      />

      {/* 4. Report Chat Modal */}
      <ReportChatModal
        isOpen={isReportModalOpen}
        onClose={() => setIsReportModalOpen(false)}
        chat={chat}
        onReportSubmitted={(reason, details) => {
          onReportChat(chat.id, reason, details);
          onToast(
            isAr ? 'تم إرسال بلاغك بنجاح للتحقق' : 'Report submitted successfully',
            'success'
          );
        }}
        lang={lang}
        isDark={isDark}
      />

      {/* 5. Delete Message Modal */}
      <DeleteMessageModal
        isOpen={!!deleteModalMsgId}
        onClose={() => setDeleteModalMsgId(null)}
        onConfirm={(alsoForEveryone) => {
          if (deleteModalMsgId) {
            onDeleteMessage(deleteModalMsgId);
            onToast(isAr ? 'تم حذف الرسالة' : 'Message deleted', 'info');
          }
        }}
        lang={lang}
        isDark={isDark}
      />
    </div>
  );
};
