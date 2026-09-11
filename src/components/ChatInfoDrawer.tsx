import React, { useState } from 'react';
import {
  X,
  Bell,
  BellOff,
  Image,
  FileText,
  Music,
  Link,
  Users,
  Bookmark,
  Trash2,
  LogOut,
  Shield,
} from 'lucide-react';
import { TelegramChat, TelegramMessage } from '../types';

interface ChatInfoDrawerProps {
  chat: TelegramChat;
  messages: TelegramMessage[];
  isOpen: boolean;
  onClose: () => void;
  onToggleMute: (chatId: string) => void;
  onClearHistory: (chatId: string) => void;
  onOpenMediaViewer: (url: string, title?: string) => void;
  lang: 'ar' | 'en';
  isDark: boolean;
}

export const ChatInfoDrawer: React.FC<ChatInfoDrawerProps> = ({
  chat,
  messages,
  isOpen,
  onClose,
  onToggleMute,
  onClearHistory,
  onOpenMediaViewer,
  lang,
  isDark,
}) => {
  const isAr = lang === 'ar';
  const [activeTab, setActiveTab] = useState<'media' | 'files' | 'members'>('media');

  if (!isOpen) return null;

  // Filter media from messages
  const mediaMessages = messages.filter((m) => m.media?.type === 'photo');
  const fileMessages = messages.filter((m) => m.media?.type === 'document');

  return (
    <aside
      className={`w-80 border-s flex flex-col h-full z-20 shrink-0 select-none animate-slide-in ${
        isDark ? 'bg-[#17212b] border-[#0e1621] text-white' : 'bg-white border-gray-200 text-gray-900'
      }`}
    >
      {/* Top Header */}
      <div className="h-16 px-4 flex items-center justify-between border-b border-gray-700/20">
        <h3 className="font-bold text-sm">
          {isAr ? 'معلومات المحادثة' : 'Chat Info'}
        </h3>
        <button
          onClick={onClose}
          className={`p-2 rounded-full transition ${
            isDark ? 'hover:bg-[#232e3c] text-gray-400' : 'hover:bg-gray-100 text-gray-600'
          }`}
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      {/* Main Info Scroll Area */}
      <div className="flex-1 overflow-y-auto p-4 space-y-6">
        {/* Profile Card */}
        <div className="flex flex-col items-center text-center">
          {chat.avatarUrl ? (
            <img
              src={chat.avatarUrl}
              alt={chat.title}
              referrerPolicy="no-referrer"
              className="w-24 h-24 rounded-full object-cover shadow-lg mb-3"
            />
          ) : (
            <div
              className="w-24 h-24 rounded-full flex items-center justify-center text-white font-bold text-3xl shadow-lg mb-3"
              style={{ backgroundColor: chat.avatarColor || '#3390ec' }}
            >
              {chat.type === 'saved' ? <Bookmark className="w-10 h-10 fill-white" /> : chat.title.slice(0, 2)}
            </div>
          )}

          <h2 className="text-lg font-bold">{chat.title}</h2>
          {chat.username && (
            <p className="text-xs text-[#3390ec] font-mono mt-0.5">@{chat.username}</p>
          )}
          <p className="text-xs text-gray-400 mt-1">
            {chat.type === 'saved'
              ? isAr ? 'الرسائل المحفوظة' : 'Saved Messages'
              : chat.type === 'channel'
              ? isAr ? `${chat.membersCount?.toLocaleString()} مشترك` : `${chat.membersCount?.toLocaleString()} subscribers`
              : chat.type === 'supergroup' || chat.type === 'group'
              ? isAr ? `${chat.membersCount?.toLocaleString()} عضو` : `${chat.membersCount?.toLocaleString()} members`
              : chat.isOnline
              ? isAr ? 'متصل الآن' : 'online'
              : isAr ? 'آخر ظهور مؤخراً' : 'last seen recently'}
          </p>
        </div>

        {/* Description / Bio */}
        {chat.description && (
          <div
            className={`p-3 rounded-xl border text-xs leading-relaxed ${
              isDark ? 'bg-[#242f3d]/60 border-[#2f3f50] text-gray-300' : 'bg-gray-50 border-gray-200 text-gray-700'
            }`}
          >
            <span className="font-bold block text-gray-400 mb-1 text-[11px]">
              {isAr ? 'الوصف' : 'Description'}
            </span>
            {chat.description}
          </div>
        )}

        {/* Notifications Toggle */}
        <div
          className={`flex items-center justify-between p-3 rounded-xl border ${
            isDark ? 'bg-[#242f3d]/60 border-[#2f3f50]' : 'bg-gray-50 border-gray-200'
          }`}
        >
          <div className="flex items-center gap-2 text-xs">
            {chat.isMuted ? (
              <BellOff className="w-4 h-4 text-amber-400" />
            ) : (
              <Bell className="w-4 h-4 text-[#3390ec]" />
            )}
            <span>{isAr ? 'الإشعارات' : 'Notifications'}</span>
          </div>

          <button
            onClick={() => onToggleMute(chat.id)}
            className={`w-10 h-6 rounded-full transition-colors relative ${
              chat.isMuted ? 'bg-gray-600' : 'bg-[#3390ec]'
            }`}
          >
            <span
              className={`w-4 h-4 rounded-full bg-white absolute top-1 transition-transform ${
                chat.isMuted ? 'start-1' : 'start-5'
              }`}
            />
          </button>
        </div>

        {/* Shared Media Tabs */}
        <div>
          <div className="flex border-b border-gray-700/20 mb-3 text-xs">
            <button
              onClick={() => setActiveTab('media')}
              className={`flex-1 py-2 font-bold border-b-2 transition ${
                activeTab === 'media'
                  ? 'border-[#3390ec] text-[#3390ec]'
                  : 'border-transparent text-gray-400 hover:text-white'
              }`}
            >
              {isAr ? 'الوسائط' : 'Media'} ({mediaMessages.length})
            </button>
            <button
              onClick={() => setActiveTab('files')}
              className={`flex-1 py-2 font-bold border-b-2 transition ${
                activeTab === 'files'
                  ? 'border-[#3390ec] text-[#3390ec]'
                  : 'border-transparent text-gray-400 hover:text-white'
              }`}
            >
              {isAr ? 'الملفات' : 'Files'} ({fileMessages.length})
            </button>
          </div>

          {activeTab === 'media' && (
            <div className="grid grid-cols-3 gap-2">
              {mediaMessages.length === 0 ? (
                <div className="col-span-3 text-center py-6 text-xs text-gray-500">
                  {isAr ? 'لا توجد وسائط بعد' : 'No media yet'}
                </div>
              ) : (
                mediaMessages.map((m) => (
                  <img
                    key={m.id}
                    src={m.media?.url}
                    alt="Media preview"
                    referrerPolicy="no-referrer"
                    onClick={() => onOpenMediaViewer(m.media!.url!, m.media?.title)}
                    className="w-full h-20 object-cover rounded-lg cursor-pointer hover:opacity-80 transition"
                  />
                ))
              )}
            </div>
          )}

          {activeTab === 'files' && (
            <div className="space-y-2">
              {fileMessages.length === 0 ? (
                <div className="text-center py-6 text-xs text-gray-500">
                  {isAr ? 'لا توجد ملفات بعد' : 'No files yet'}
                </div>
              ) : (
                fileMessages.map((m) => (
                  <div
                    key={m.id}
                    className="flex items-center gap-2 p-2 rounded-lg bg-black/10 text-xs"
                  >
                    <FileText className="w-4 h-4 text-emerald-400" />
                    <span className="truncate flex-1 font-mono">{m.media?.fileName}</span>
                    <span className="text-[10px] text-gray-400">{m.media?.fileSize}</span>
                  </div>
                ))
              )}
            </div>
          )}
        </div>

        {/* Clear History Action */}
        <div className="pt-4 border-t border-gray-700/20 space-y-2">
          <button
            onClick={() => onClearHistory(chat.id)}
            className="w-full py-2.5 px-3 rounded-xl text-xs font-semibold text-red-400 hover:bg-red-500/10 transition flex items-center justify-center gap-2"
          >
            <Trash2 className="w-4 h-4" />
            <span>{isAr ? 'مسح سجل المحادثة' : 'Clear Chat History'}</span>
          </button>
        </div>
      </div>
    </aside>
  );
};
