import React, { useState, useRef, useEffect } from 'react';
import {
  Search,
  Phone,
  Video,
  MoreVertical,
  Pin,
  Bookmark,
  Check,
  CheckCheck,
  Reply,
  Trash2,
  Smile,
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
} from 'lucide-react';
import { TelegramChat, TelegramMessage, TelegramUser } from '../types';
import { MessageInput } from './MessageInput';

interface ChatWindowProps {
  chat: TelegramChat | null;
  messages: TelegramMessage[];
  currentUser?: TelegramUser;
  onSendMessage: (text: string, replyTo?: TelegramMessage, media?: any) => void;
  onReactMessage: (messageId: string, emoji: string) => void;
  onPinMessage: (messageId: string) => void;
  onDeleteMessage: (messageId: string) => void;
  onToggleChatInfo: () => void;
  isChatInfoOpen: boolean;
  onToggleMute: (chatId: string) => void;
  onClearHistory: (chatId: string) => void;
  onOpenMediaViewer: (url: string, title?: string) => void;
  lang: 'ar' | 'en';
  isDark: boolean;
}

export const ChatWindow: React.FC<ChatWindowProps> = ({
  chat,
  messages,
  currentUser,
  onSendMessage,
  onReactMessage,
  onPinMessage,
  onDeleteMessage,
  onToggleChatInfo,
  isChatInfoOpen,
  onToggleMute,
  onClearHistory,
  onOpenMediaViewer,
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
  const [quickReactionForMsg, setQuickReactionForMsg] = useState<string | null>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Auto scroll to bottom when messages change
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages.length, chat?.id]);

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
    if (playingVoiceId === msgId) {
      setPlayingVoiceId(null);
    } else {
      setPlayingVoiceId(msgId);
    }
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
        {/* Left: Avatar + Title + Status */}
        <div
          onClick={onToggleChatInfo}
          className="flex items-center gap-3 cursor-pointer min-w-0 flex-1 hover:opacity-85 transition"
        >
          {chat.avatarUrl ? (
            <img
              src={chat.avatarUrl}
              alt={chat.title}
              referrerPolicy="no-referrer"
              className="w-10 h-10 rounded-full object-cover shrink-0"
            />
          ) : (
            <div
              className="w-10 h-10 rounded-full flex items-center justify-center text-white font-bold shrink-0 text-sm shadow"
              style={{ backgroundColor: chat.avatarColor || '#3390ec' }}
            >
              {chat.type === 'saved' ? <Bookmark className="w-5 h-5 fill-white" /> : chat.title.slice(0, 2)}
            </div>
          )}

          <div className="min-w-0">
            <h2 className="font-bold text-sm truncate flex items-center gap-1.5">
              <span>{chat.title}</span>
              {chat.isMuted && <VolumeX className="w-3.5 h-3.5 text-gray-400" />}
            </h2>
            <p className="text-xs text-gray-400 truncate">
              {chat.type === 'saved'
                ? isAr
                  ? 'مساحتك السحابية الخاصة'
                  : 'Your cloud storage'
                : chat.type === 'channel'
                ? isAr
                  ? `${chat.membersCount?.toLocaleString() || '12,400'} مشترك`
                  : `${chat.membersCount?.toLocaleString() || '12,400'} subscribers`
                : chat.type === 'supergroup' || chat.type === 'group'
                ? isAr
                  ? `${chat.membersCount?.toLocaleString() || '150'} عضو`
                  : `${chat.membersCount?.toLocaleString() || '150'} members`
                : chat.isOnline
                ? isAr
                  ? 'متصل الآن'
                  : 'online'
                : isAr
                ? 'آخر ظهور مؤخراً'
                : 'last seen recently'}
            </p>
          </div>
        </div>

        {/* Right Header Actions */}
        <div className="flex items-center gap-1">
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

          <button
            onClick={() => onToggleMute(chat.id)}
            className={`p-2 rounded-full transition ${
              isDark ? 'hover:bg-[#232e3c] text-gray-300' : 'hover:bg-gray-100 text-gray-600'
            }`}
            title={chat.isMuted ? (isAr ? 'إلغاء الكتم' : 'Unmute') : isAr ? 'كتم الإشعارات' : 'Mute'}
          >
            {chat.isMuted ? <VolumeX className="w-5 h-5 text-amber-400" /> : <Volume2 className="w-5 h-5" />}
          </button>

          <button
            onClick={onToggleChatInfo}
            className={`p-2 rounded-full transition ${
              isDark ? 'hover:bg-[#232e3c] text-gray-300' : 'hover:bg-gray-100 text-gray-600'
            }`}
            title={isAr ? 'معلومات المحادثة' : 'Chat info'}
          >
            {isChatInfoOpen ? <PanelRightClose className="w-5 h-5" /> : <PanelRightOpen className="w-5 h-5" />}
          </button>

          {/* More Menu Dropdown */}
          <div className="relative">
            <button
              onClick={() => setShowMoreMenu(!showMoreMenu)}
              className={`p-2 rounded-full transition ${
                isDark ? 'hover:bg-[#232e3c] text-gray-300' : 'hover:bg-gray-100 text-gray-600'
              }`}
            >
              <MoreVertical className="w-5 h-5" />
            </button>

            {showMoreMenu && (
              <div
                className={`absolute end-0 top-12 rounded-xl shadow-2xl py-1.5 w-44 z-30 border ${
                  isDark ? 'bg-[#242f3d] border-[#2f3f50] text-white' : 'bg-white border-gray-200 text-gray-800'
                }`}
              >
                <button
                  onClick={() => {
                    onClearHistory(chat.id);
                    setShowMoreMenu(false);
                  }}
                  className={`w-full text-start px-4 py-2 text-xs flex items-center gap-2 ${
                    isDark ? 'hover:bg-[#2b394a]' : 'hover:bg-gray-100'
                  }`}
                >
                  <Trash2 className="w-4 h-4 text-red-400" />
                  <span>{isAr ? 'مسح سجل المحادثة' : 'Clear History'}</span>
                </button>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Pinned Message Banner */}
      {pinnedMessage && (
        <div
          className={`px-4 py-2 flex items-center justify-between text-xs border-b select-none shadow-sm z-10 ${
            isDark
              ? 'bg-[#1e2a38] border-[#0e1621] text-gray-200'
              : 'bg-[#f4f7f9] border-gray-200 text-gray-800'
          }`}
        >
          <div className="flex items-center gap-2 min-w-0 flex-1">
            <Pin className="w-4 h-4 text-[#3390ec] shrink-0 fill-current" />
            <div className="min-w-0">
              <span className="font-bold text-[#3390ec] block text-[11px]">
                {isAr ? 'رسالة مثبتة' : 'Pinned Message'}
              </span>
              <p className="truncate text-gray-400">{pinnedMessage.text}</p>
            </div>
          </div>
        </div>
      )}

      {/* Messages Thread Container */}
      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-3">
        {/* Telegram Wallpaper Pattern Overlay */}
        <div className="text-center my-2">
          <span
            className={`inline-block px-3 py-1 rounded-full text-[11px] font-semibold select-none shadow-sm ${
              isDark ? 'bg-[#182533]/80 text-gray-300' : 'bg-gray-300/80 text-gray-700'
            }`}
          >
            {isAr ? 'اليوم' : 'Today'}
          </span>
        </div>

        {displayedMessages.map((msg) => {
          const isOut = msg.isOut;
          const isHovered = hoveredMessageId === msg.id;

          return (
            <div
              key={msg.id}
              id={`msg-${msg.id}`}
              onMouseEnter={() => setHoveredMessageId(msg.id)}
              onMouseLeave={() => {
                setHoveredMessageId(null);
                setQuickReactionForMsg(null);
              }}
              className={`flex flex-col group relative ${isOut ? 'items-end' : 'items-start'}`}
            >
              {/* Message Bubble */}
              <div
                className={`max-w-[85%] md:max-w-[70%] rounded-2xl p-3 shadow-md relative transition-all ${
                  isOut
                    ? isDark
                      ? 'bg-[#2b5278] text-white rounded-br-xs'
                      : 'bg-[#eeffde] text-gray-900 rounded-br-xs'
                    : isDark
                    ? 'bg-[#182533] text-gray-100 rounded-bl-xs'
                    : 'bg-white text-gray-900 rounded-bl-xs'
                }`}
              >
                {/* Sender name for groups/channels */}
                {!isOut && msg.senderName && chat.type !== 'private' && (
                  <span className="text-xs font-bold text-[#3390ec] block mb-1">
                    {msg.senderName}
                  </span>
                )}

                {/* Reply quote block */}
                {msg.replyTo && (
                  <div
                    className={`mb-2 p-2 rounded-lg text-xs border-s-3 border-[#3390ec] select-none ${
                      isDark ? 'bg-black/20' : 'bg-black/5'
                    }`}
                  >
                    <span className="font-bold text-[#3390ec] block">
                      {msg.replyTo.senderName}
                    </span>
                    <p className="truncate opacity-80">{msg.replyTo.text}</p>
                  </div>
                )}

                {/* Photo Media */}
                {msg.media?.type === 'photo' && msg.media.url && (
                  <div className="mb-2 rounded-xl overflow-hidden cursor-pointer">
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
                  <div className="flex items-center gap-3 py-1 px-2 select-none min-w-[200px]">
                    <button
                      onClick={() => toggleVoicePlay(msg.id)}
                      className="w-9 h-9 rounded-full bg-[#3390ec] hover:bg-[#2b7ec9] text-white flex items-center justify-center shadow"
                    >
                      {playingVoiceId === msg.id ? (
                        <Pause className="w-4 h-4 fill-white" />
                      ) : (
                        <Play className="w-4 h-4 fill-white translate-x-0.5" />
                      )}
                    </button>

                    <div className="flex-1">
                      {/* Audio wave lines */}
                      <div className="flex items-center gap-1 h-5">
                        {[40, 75, 55, 90, 30, 80, 60, 100, 45, 70, 35, 85].map((h, i) => (
                          <span
                            key={i}
                            className={`w-1 rounded-full transition-all ${
                              playingVoiceId === msg.id ? 'bg-[#3390ec] animate-pulse' : 'bg-gray-400/50'
                            }`}
                            style={{ height: `${h}%` }}
                          />
                        ))}
                      </div>
                      <span className="text-[10px] text-gray-400 font-mono">
                        0:{msg.media.duration?.toString().padStart(2, '0') || '04'}
                      </span>
                    </div>
                  </div>
                )}

                {/* Document File */}
                {msg.media?.type === 'document' && (
                  <div
                    className={`flex items-center gap-3 p-2 rounded-xl mb-1 ${
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
                        className="p-2 text-gray-400 hover:text-white"
                      >
                        <Download className="w-4 h-4" />
                      </a>
                    )}
                  </div>
                )}

                {/* Message Text */}
                <p className="text-sm whitespace-pre-wrap break-words leading-relaxed select-text">
                  {msg.text}
                </p>

                {/* Message Footer: Time + Status */}
                <div className="flex items-center justify-end gap-1 mt-1 text-[10px] text-gray-400 select-none">
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

                {/* Message Reactions Row */}
                {msg.reactions && msg.reactions.length > 0 && (
                  <div className="flex flex-wrap gap-1 mt-1.5 pt-1">
                    {msg.reactions.map((r, rIdx) => (
                      <button
                        key={rIdx}
                        onClick={() => onReactMessage(msg.id, r.emoji)}
                        className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs transition border select-none ${
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
              </div>

              {/* Hover Floating Actions Menu (Reply, React, Pin, Delete) */}
              {isHovered && (
                <div
                  className={`absolute -top-3 z-20 flex items-center gap-0.5 p-1 rounded-full shadow-lg border backdrop-blur-sm ${
                    isOut ? 'end-2' : 'start-2'
                  } ${
                    isDark
                      ? 'bg-[#17212b]/95 border-[#2f3f50] text-gray-300'
                      : 'bg-white/95 border-gray-200 text-gray-700'
                  }`}
                >
                  {/* Emoji reactions bar */}
                  <div className="flex items-center gap-0.5 px-1 border-e border-gray-600/30">
                    {['❤️', '👍', '🔥', '😂', '🎉'].map((em) => (
                      <button
                        key={em}
                        onClick={() => onReactMessage(msg.id, em)}
                        className="hover:scale-130 transition-transform p-0.5 text-sm"
                      >
                        {em}
                      </button>
                    ))}
                  </div>

                  <button
                    onClick={() => setReplyingMessage(msg)}
                    className="p-1 rounded-full hover:bg-gray-500/20"
                    title={isAr ? 'رد' : 'Reply'}
                  >
                    <Reply className="w-3.5 h-3.5" />
                  </button>

                  <button
                    onClick={() => onPinMessage(msg.id)}
                    className="p-1 rounded-full hover:bg-gray-500/20"
                    title={isAr ? 'تثبيت' : 'Pin'}
                  >
                    <Pin className="w-3.5 h-3.5" />
                  </button>

                  {isOut && (
                    <button
                      onClick={() => onDeleteMessage(msg.id)}
                      className="p-1 rounded-full hover:bg-red-500/20 text-red-400"
                      title={isAr ? 'حذف' : 'Delete'}
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  )}
                </div>
              )}
            </div>
          );
        })}

        <div ref={messagesEndRef} />
      </div>

      {/* Bottom Message Input Bar */}
      <MessageInput
        onSendMessage={onSendMessage}
        replyToMessage={replyingMessage}
        onCancelReply={() => setReplyingMessage(null)}
        lang={lang}
        isDark={isDark}
      />
    </div>
  );
};
