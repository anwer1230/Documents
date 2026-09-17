import React, { useState } from 'react';
import {
  X,
  Bell,
  BellOff,
  Share2,
  Trash2,
  LogOut,
  Flag,
  Image as ImageIcon,
  FileText,
  Link,
  Users,
  Shield,
  Smartphone,
  Info,
} from 'lucide-react';
import { TelegramChat, TelegramMessage } from '../types';
import { getSenderColor, getInitials } from '../utils/telegramColors';

interface ChatInfoDrawerProps {
  chat: TelegramChat;
  messages: TelegramMessage[];
  isOpen: boolean;
  onClose: () => void;
  onToggleMute: (chatId: string) => void;
  onOpenClearHistory: () => void;
  onOpenLeaveGroup: () => void;
  onOpenShareLink: () => void;
  onOpenReportChat: () => void;
  onOpenMediaViewer: (url: string, title?: string) => void;
  onToast?: (message: string, type?: 'success' | 'error' | 'info') => void;
  lang?: 'ar' | 'en';
  isDark?: boolean;
}

export const ChatInfoDrawer: React.FC<ChatInfoDrawerProps> = ({
  chat,
  messages,
  isOpen,
  onClose,
  onToggleMute,
  onOpenClearHistory,
  onOpenLeaveGroup,
  onOpenShareLink,
  onOpenReportChat,
  onOpenMediaViewer,
  onToast,
  lang = 'ar',
  isDark = true,
}) => {
  const [activeTab, setActiveTab] = useState<'media' | 'files' | 'links'>('media');
  const isAr = lang === 'ar';

  if (!isOpen) return null;

  // Extract media items
  const mediaMessages = messages.filter((m) => m.media && m.media.type === 'photo');
  const fileMessages = messages.filter((m) => m.media && (m.media.type === 'document' || m.media.type === 'audio'));
  const linkMessages = messages.filter((m) => m.text && (m.text.includes('http://') || m.text.includes('https://')));

  return (
    <div
      className={`w-80 lg:w-96 h-full flex flex-col border-s z-30 select-none shrink-0 transition-all ${
        isDark ? 'bg-[#17212b] border-[#242f3d] text-white' : 'bg-white border-gray-200 text-gray-900'
      }`}
    >
      {/* Header */}
      <div className="p-4 flex items-center justify-between border-b border-gray-700/30">
        <h3 className="font-bold text-base">{isAr ? 'معلومات المحادثة' : 'Chat Info'}</h3>
        <button
          onClick={onClose}
          className="p-1 rounded-lg hover:bg-white/10 text-gray-400 hover:text-white transition"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-6">
        {/* Profile Card */}
        <div className="flex flex-col items-center text-center">
          <div
            style={{ backgroundColor: chat.avatarColor || getSenderColor(chat.id) }}
            className="w-24 h-24 rounded-full flex items-center justify-center font-bold text-3xl text-white shadow-xl mb-3 overflow-hidden"
          >
            {chat.avatarUrl ? (
              <img src={chat.avatarUrl} alt="" className="w-full h-full object-cover" />
            ) : (
              getInitials(chat.title)
            )}
          </div>
          <h2 className="font-bold text-lg">{chat.title}</h2>
          {chat.username && (
            <p className="text-xs text-[#3390ec] font-mono mt-0.5">@{chat.username}</p>
          )}
          <p className="text-xs text-gray-400 mt-1">
            {chat.type === 'channel'
              ? `${chat.membersCount || 1200} ${isAr ? 'مشترك' : 'subscribers'}`
              : chat.type === 'group' || chat.type === 'supergroup'
              ? `${chat.membersCount || 450} ${isAr ? 'عضو' : 'members'}`
              : chat.isOnline
              ? isAr ? 'متصل الآن' : 'online'
              : isAr ? 'آخر ظهور مؤخراً' : 'last seen recently'}
          </p>
        </div>

        {/* Bio / Description */}
        {chat.botInfo?.description ? (
          <div className="p-3 rounded-2xl bg-black/20 text-xs text-gray-300 leading-relaxed">
            <span className="font-bold block text-gray-400 mb-1">{isAr ? 'الوصف' : 'Description'}</span>
            {chat.botInfo.description}
          </div>
        ) : null}

        {/* Quick Actions List */}
        <div className="space-y-1">
          <button
            onClick={() => onToggleMute(chat.id)}
            className="w-full flex items-center justify-between p-3 rounded-xl hover:bg-white/10 transition text-sm"
          >
            <div className="flex items-center gap-3">
              <Bell className="w-4 h-4 text-gray-400" />
              <span>{isAr ? 'كتم الإشعارات' : 'Mute Notifications'}</span>
            </div>
            <span className="text-xs text-gray-400">{isAr ? 'مفعل' : 'On'}</span>
          </button>

          <button
            onClick={onOpenShareLink}
            className="w-full flex items-center gap-3 p-3 rounded-xl hover:bg-white/10 transition text-sm"
          >
            <Share2 className="w-4 h-4 text-gray-400" />
            <span>{isAr ? 'مشاركة الرابط' : 'Share Link'}</span>
          </button>

          <button
            onClick={onOpenClearHistory}
            className="w-full flex items-center gap-3 p-3 rounded-xl hover:bg-white/10 transition text-sm text-rose-400"
          >
            <Trash2 className="w-4 h-4" />
            <span>{isAr ? 'مسح السجل' : 'Clear History'}</span>
          </button>

          <button
            onClick={onOpenLeaveGroup}
            className="w-full flex items-center gap-3 p-3 rounded-xl hover:bg-white/10 transition text-sm text-rose-400"
          >
            <LogOut className="w-4 h-4" />
            <span>{isAr ? 'مغادرة المحادثة' : 'Leave Chat'}</span>
          </button>

          <button
            onClick={onOpenReportChat}
            className="w-full flex items-center gap-3 p-3 rounded-xl hover:bg-white/10 transition text-sm text-amber-400"
          >
            <Flag className="w-4 h-4" />
            <span>{isAr ? 'إبلاغ' : 'Report'}</span>
          </button>
        </div>

        {/* Shared Media Tabs */}
        <div>
          <div className="flex items-center border-b border-gray-700/30 mb-3 text-xs font-bold">
            <button
              onClick={() => setActiveTab('media')}
              className={`pb-2 px-3 transition ${
                activeTab === 'media' ? 'text-[#3390ec] border-b-2 border-[#3390ec]' : 'text-gray-400'
              }`}
            >
              {isAr ? 'الوسائط' : 'Media'} ({mediaMessages.length})
            </button>
            <button
              onClick={() => setActiveTab('files')}
              className={`pb-2 px-3 transition ${
                activeTab === 'files' ? 'text-[#3390ec] border-b-2 border-[#3390ec]' : 'text-gray-400'
              }`}
            >
              {isAr ? 'الملفات' : 'Files'} ({fileMessages.length})
            </button>
            <button
              onClick={() => setActiveTab('links')}
              className={`pb-2 px-3 transition ${
                activeTab === 'links' ? 'text-[#3390ec] border-b-2 border-[#3390ec]' : 'text-gray-400'
              }`}
            >
              {isAr ? 'الروابط' : 'Links'} ({linkMessages.length})
            </button>
          </div>

          {activeTab === 'media' && (
            <div className="grid grid-cols-3 gap-2">
              {mediaMessages.length === 0 ? (
                <div className="col-span-3 text-center py-6 text-xs text-gray-500">
                  {isAr ? 'لا توجد وسائط مشاركة' : 'No shared media'}
                </div>
              ) : (
                mediaMessages.map((m) => (
                  <div
                    key={m.id}
                    onClick={() => m.media?.url && onOpenMediaViewer(m.media.url, m.media.title)}
                    className="aspect-square rounded-lg overflow-hidden bg-black/40 cursor-pointer group relative"
                  >
                    <img
                      src={m.media?.url}
                      alt=""
                      className="w-full h-full object-cover group-hover:scale-105 transition"
                    />
                  </div>
                ))
              )}
            </div>
          )}

          {activeTab === 'files' && (
            <div className="space-y-2">
              {fileMessages.length === 0 ? (
                <div className="text-center py-6 text-xs text-gray-500">
                  {isAr ? 'لا توجد ملفات مشاركة' : 'No shared files'}
                </div>
              ) : (
                fileMessages.map((m) => (
                  <div key={m.id} className="p-2 rounded-xl bg-black/20 flex items-center gap-3 text-xs">
                    <FileText className="w-5 h-5 text-[#3390ec] shrink-0" />
                    <div className="flex-1 min-w-0">
                      <p className="font-semibold truncate">{m.media?.fileName || m.media?.title || 'File'}</p>
                      <span className="text-[10px] text-gray-400">{m.media?.fileSize || '1.2 MB'}</span>
                    </div>
                  </div>
                ))
              )}
            </div>
          )}

          {activeTab === 'links' && (
            <div className="space-y-2">
              {linkMessages.length === 0 ? (
                <div className="text-center py-6 text-xs text-gray-500">
                  {isAr ? 'لا توجد روابط مشاركة' : 'No shared links'}
                </div>
              ) : (
                linkMessages.map((m) => (
                  <div key={m.id} className="p-2 rounded-xl bg-black/20 flex items-center gap-3 text-xs">
                    <Link className="w-5 h-5 text-[#3390ec] shrink-0" />
                    <div className="flex-1 min-w-0">
                      <p className="font-semibold truncate text-[#3390ec]">{m.text}</p>
                    </div>
                  </div>
                ))
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
