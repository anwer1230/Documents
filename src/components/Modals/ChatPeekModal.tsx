import React, { useEffect, useRef } from 'react';
import {
  X,
  Volume2,
  VolumeX,
  Pin,
  CheckCheck,
  Mail,
  ExternalLink,
  Lock,
  Trash2,
  ShieldCheck,
  Sparkles,
  Users,
  EyeOff,
  Image as ImageIcon,
  FileText,
  Mic,
  Play,
} from 'lucide-react';
import { useTelegram } from '../../context/TelegramContext';
import { ChatPeekData, Message } from '../../types';
import { TelegramAvatar } from '../TelegramAvatar';
import confetti from 'canvas-confetti';

interface ChatPeekModalProps {
  peekData?: ChatPeekData | null;
  onClose?: () => void;
  onOpenChat?: (chatId: string) => void;
}

export const ChatPeekModal: React.FC<ChatPeekModalProps> = ({
  peekData: propPeekData,
  onClose: propOnClose,
  onOpenChat: propOnOpenChat,
}) => {
  const {
    peekChatData,
    setPeekChatData,
    setActiveChatId,
    messages,
    togglePinChat,
    toggleMuteChat,
    markChatReadUnread,
    deleteChat,
    settings,
    showToast,
  } = useTelegram();

  const peekData = propPeekData !== undefined ? propPeekData : peekChatData;
  const onClose = propOnClose || (() => setPeekChatData(null));
  const onOpenChat = propOnOpenChat || ((id: string) => {
    setActiveChatId(id);
    setPeekChatData(null);
  });

  const isRtl = settings.language === 'ar';
  const modalRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  if (!peekData) return null;

  const targetChatId = String(peekData.chatId || peekData.chat?.id || '');
  const targetTitle = peekData.chatTitle || peekData.chat?.title || '';
  const targetAvatar = peekData.chatAvatar || peekData.chat?.avatar;
  const targetIsMuted = peekData.isMuted ?? peekData.chat?.isMuted;
  const targetIsPinned = peekData.isPinned ?? peekData.chat?.isPinned;
  const targetIsVerified = peekData.isVerified ?? peekData.chat?.isVerified;

  const chatMessages: Message[] = peekData.messages || (targetChatId ? messages[targetChatId] : []) || [];
  const recentMessages = chatMessages.slice(-8);

  const handleMute = () => {
    if (!targetChatId) return;
    toggleMuteChat(targetChatId);
    showToast(
      targetIsMuted
        ? isRtl
          ? 'تم إلغاء كتم المحادثة'
          : 'Chat unmuted'
        : isRtl
        ? 'تم كتم إشعارات المحادثة'
        : 'Chat muted',
      '🔔'
    );
  };

  const handlePin = () => {
    if (!targetChatId) return;
    togglePinChat(targetChatId);
    showToast(
      targetIsPinned
        ? isRtl
          ? 'تم إلغاء تثبيت المحادثة'
          : 'Chat unpinned'
        : isRtl
        ? 'تم تثبيت المحادثة في الأعلى'
        : 'Chat pinned',
      '📌'
    );
  };

  const handleMarkRead = () => {
    if (!targetChatId) return;
    markChatReadUnread(targetChatId);
    showToast(isRtl ? 'تم تغيير حالة القراءة' : 'Marked read/unread', '✉️');
  };

  const handleDelete = () => {
    if (!targetChatId) return;
    if (
      confirm(
        isRtl
          ? 'هل أنت متأكد من حذف هذه المحادثة بالكامل؟'
          : 'Are you sure you want to delete this chat?'
      )
    ) {
      deleteChat(targetChatId);
      onClose();
      showToast(isRtl ? 'تم حذف المحادثة' : 'Chat deleted', '🗑️');
    }
  };

  const handleStartSecret = () => {
    confetti({ particleCount: 40, spread: 60, origin: { y: 0.6 } });
    showToast(
      isRtl
        ? 'تم تفعيل التشفير المتقدم (End-to-End MTProto Secret Mode)'
        : 'MTProto End-to-End Secret Mode Activated',
      '🔐'
    );
    if (targetChatId) {
      onOpenChat(targetChatId);
    }
    onClose();
  };

  return (
    <div
      id="tg-chat-peek-overlay"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-md animate-in fade-in duration-200"
      onClick={onClose}
    >
      <div
        ref={modalRef}
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-lg rounded-3xl overflow-hidden shadow-2xl border flex flex-col max-h-[85vh] animate-in zoom-in-95 duration-200"
        style={{
          backgroundColor: 'var(--tg-theme-surface, #17212b)',
          borderColor: 'var(--tg-theme-border, rgba(255,255,255,0.1))',
          color: 'var(--tg-theme-bubble-in-text, #ffffff)',
        }}
      >
        {/* Header with Ghost Mode Badge */}
        <div className="p-4 flex items-center justify-between border-b border-white/10 bg-black/20">
          <div className="flex items-center gap-3 min-w-0">
            <TelegramAvatar
              photoUrl={targetAvatar}
              name={targetTitle}
              size={44}
            />
            <div className="min-w-0">
              <div className="flex items-center gap-1.5">
                <h3 className="font-bold text-sm truncate text-white">
                  {targetTitle}
                </h3>
                {targetIsVerified && (
                  <ShieldCheck className="w-4 h-4 text-sky-400 shrink-0" />
                )}
              </div>
              <div className="flex items-center gap-2 text-xs text-gray-400">
                <span className="flex items-center gap-1 text-emerald-400 font-medium">
                  <EyeOff className="w-3.5 h-3.5" />
                  {isRtl ? 'معاينة شبحية (بدون قراءة)' : 'Ghost Peek (No Read Receipt)'}
                </span>
                {peekData.memberCount && (
                  <span className="flex items-center gap-1 text-gray-400">
                    <Users className="w-3 h-3" />
                    {peekData.memberCount.toLocaleString()} {isRtl ? 'عضو' : 'members'}
                  </span>
                )}
              </div>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-2 rounded-full hover:bg-white/10 text-gray-400 hover:text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Message Preview Feed */}
        <div
          className="flex-1 overflow-y-auto p-4 space-y-3 bg-black/10 no-scrollbar min-h-[220px]"
          style={{
            backgroundImage:
              'radial-gradient(circle at center, rgba(36,129,204,0.04) 0%, transparent 70%)',
          }}
        >
          {recentMessages.length === 0 ? (
            <div className="text-center py-12 text-gray-400 text-xs">
              {isRtl
                ? 'لا توجد رسائل سابقة في هذه المحادثة بعد'
                : 'No recent messages in this conversation yet'}
            </div>
          ) : (
            recentMessages.map((msg) => (
              <div
                key={msg.id}
                className={`flex flex-col max-w-[85%] rounded-2xl p-3 shadow-sm text-xs ${
                  msg.isOutgoing
                    ? 'ml-auto rtl:ml-0 rtl:mr-auto bg-[#2481cc]/25 border border-[#2481cc]/40 text-white rounded-br-none rtl:rounded-br-2xl rtl:rounded-bl-none'
                    : 'mr-auto rtl:mr-0 rtl:ml-auto bg-white/10 border border-white/10 text-gray-200 rounded-bl-none rtl:rounded-bl-2xl rtl:rounded-br-none'
                }`}
              >
                {!msg.isOutgoing && msg.senderName && (
                  <span className="text-[10px] font-bold text-sky-400 mb-1">
                    {msg.senderName}
                  </span>
                )}

                {/* Media Preview inside bubble */}
                {msg.media && (
                  <div className="mb-2 rounded-xl overflow-hidden border border-white/10 bg-black/20">
                    {msg.media.type === 'photo' && msg.media.url && (
                      <img
                        src={msg.media.url}
                        alt="Photo"
                        className="w-full max-h-48 object-cover rounded-lg"
                        referrerPolicy="no-referrer"
                      />
                    )}
                    {msg.media.type === 'video' && msg.media.url && (
                      <div className="relative flex items-center justify-center bg-black/40 h-36">
                        <Play className="w-8 h-8 text-white/80" />
                        <span className="absolute bottom-2 right-2 text-[10px] bg-black/60 px-1.5 py-0.5 rounded text-white font-mono">
                          {msg.media.duration || 15}s
                        </span>
                      </div>
                    )}
                    {msg.media.type === 'audio' || msg.media.type === 'voice' ? (
                      <div className="flex items-center gap-2 p-2">
                        <Mic className="w-4 h-4 text-sky-400" />
                        <span className="text-[11px] text-gray-300 font-mono">
                          0:{msg.media.duration || 12}
                        </span>
                      </div>
                    ) : null}
                  </div>
                )}

                <p className="leading-relaxed whitespace-pre-wrap">{msg.text}</p>

                <div className="flex items-center justify-end gap-1 mt-1 text-[10px] text-gray-400">
                  <span>{msg.timestamp}</span>
                  {msg.isOutgoing && (
                    <CheckCheck className="w-3 h-3 text-[#4fae4e]" />
                  )}
                </div>
              </div>
            ))
          )}
        </div>

        {/* Telegram X Quick Action Strip */}
        <div className="p-3 border-t border-white/10 bg-black/20 grid grid-cols-3 sm:grid-cols-6 gap-2">
          {/* Open Full Chat */}
          <button
            onClick={() => {
              onOpenChat(targetChatId);
              onClose();
            }}
            className="flex flex-col items-center justify-center p-2 rounded-2xl hover:bg-sky-500/20 text-sky-400 transition-colors"
            title={isRtl ? 'فتح المحادثة بالكامل' : 'Open Full Chat'}
          >
            <ExternalLink className="w-5 h-5 mb-1" />
            <span className="text-[10px] font-bold">
              {isRtl ? 'فتح' : 'Open'}
            </span>
          </button>

          {/* Mute / Unmute */}
          <button
            onClick={handleMute}
            className="flex flex-col items-center justify-center p-2 rounded-2xl hover:bg-white/10 text-gray-300 hover:text-white transition-colors"
            title={peekData.isMuted ? 'Unmute' : 'Mute'}
          >
            {peekData.isMuted ? (
              <Volume2 className="w-5 h-5 mb-1 text-emerald-400" />
            ) : (
              <VolumeX className="w-5 h-5 mb-1" />
            )}
            <span className="text-[10px] font-bold">
              {peekData.isMuted
                ? isRtl
                  ? 'تشغيل الصوت'
                  : 'Unmute'
                : isRtl
                ? 'كتم'
                : 'Mute'}
            </span>
          </button>

          {/* Pin / Unpin */}
          <button
            onClick={handlePin}
            className="flex flex-col items-center justify-center p-2 rounded-2xl hover:bg-white/10 text-gray-300 hover:text-white transition-colors"
            title={peekData.isPinned ? 'Unpin' : 'Pin'}
          >
            <Pin
              className={`w-5 h-5 mb-1 ${
                peekData.isPinned ? 'text-amber-400 rotate-45' : ''
              }`}
            />
            <span className="text-[10px] font-bold">
              {peekData.isPinned
                ? isRtl
                  ? 'إلغاء التثبيت'
                  : 'Unpin'
                : isRtl
                ? 'تثبيت'
                : 'Pin'}
            </span>
          </button>

          {/* Mark Read/Unread */}
          <button
            onClick={handleMarkRead}
            className="flex flex-col items-center justify-center p-2 rounded-2xl hover:bg-white/10 text-gray-300 hover:text-white transition-colors"
          >
            <Mail className="w-5 h-5 mb-1 text-indigo-400" />
            <span className="text-[10px] font-bold">
              {isRtl ? 'قراءة' : 'Read'}
            </span>
          </button>

          {/* Secret Chat Mode */}
          <button
            onClick={handleStartSecret}
            className="flex flex-col items-center justify-center p-2 rounded-2xl hover:bg-emerald-500/20 text-emerald-400 transition-colors"
            title={isRtl ? 'محادثة مشفرة E2E' : 'End-to-End Secret'}
          >
            <Lock className="w-5 h-5 mb-1" />
            <span className="text-[10px] font-bold">
              {isRtl ? 'سري' : 'Secret'}
            </span>
          </button>

          {/* Delete */}
          <button
            onClick={handleDelete}
            className="flex flex-col items-center justify-center p-2 rounded-2xl hover:bg-rose-500/20 text-rose-400 transition-colors"
            title={isRtl ? 'حذف المحادثة' : 'Delete Chat'}
          >
            <Trash2 className="w-5 h-5 mb-1" />
            <span className="text-[10px] font-bold">
              {isRtl ? 'حذف' : 'Delete'}
            </span>
          </button>
        </div>
      </div>
    </div>
  );
};
