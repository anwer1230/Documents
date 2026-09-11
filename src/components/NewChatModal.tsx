import React, { useState } from 'react';
import { X, Radio, Users, MessageSquare, Plus, Hash } from 'lucide-react';
import { TelegramChat } from '../types';

interface NewChatModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreateChat: (chat: Partial<TelegramChat>) => void;
  lang: 'ar' | 'en';
  isDark: boolean;
}

export const NewChatModal: React.FC<NewChatModalProps> = ({
  isOpen,
  onClose,
  onCreateChat,
  lang,
  isDark,
}) => {
  const isAr = lang === 'ar';
  const [tab, setTab] = useState<'channel' | 'group' | 'chat'>('channel');
  const [title, setTitle] = useState('');
  const [username, setUsername] = useState('');
  const [description, setDescription] = useState('');

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;

    onCreateChat({
      title: title.trim(),
      username: username.trim() || undefined,
      description: description.trim() || undefined,
      type: tab === 'channel' ? 'channel' : tab === 'group' ? 'group' : 'private',
      avatarColor: tab === 'channel' ? '#2b5278' : tab === 'group' ? '#e07a5f' : '#3390ec',
      unreadCount: 0,
      membersCount: tab === 'channel' ? 1 : tab === 'group' ? 1 : undefined,
    });

    setTitle('');
    setUsername('');
    setDescription('');
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 select-none">
      <div onClick={onClose} className="fixed inset-0 bg-black/60 backdrop-blur-xs" />

      <div
        className={`relative w-full max-w-md rounded-2xl p-6 shadow-2xl z-10 border ${
          isDark ? 'bg-[#17212b] border-[#232e3c] text-white' : 'bg-white border-gray-200 text-gray-900'
        }`}
      >
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-bold text-base">
            {isAr ? 'إنشاء محادثة أو قناة جديدة' : 'New Chat or Channel'}
          </h3>
          <button onClick={onClose} className="p-1 rounded-full text-gray-400 hover:text-white">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab Selection */}
        <div className="flex bg-black/20 p-1 rounded-xl mb-4 text-xs font-semibold">
          <button
            type="button"
            onClick={() => setTab('channel')}
            className={`flex-1 py-2 rounded-lg transition flex items-center justify-center gap-1.5 ${
              tab === 'channel' ? 'bg-[#3390ec] text-white' : 'text-gray-400 hover:text-white'
            }`}
          >
            <Radio className="w-4 h-4" />
            <span>{isAr ? 'قناة جديدة' : 'New Channel'}</span>
          </button>

          <button
            type="button"
            onClick={() => setTab('group')}
            className={`flex-1 py-2 rounded-lg transition flex items-center justify-center gap-1.5 ${
              tab === 'group' ? 'bg-[#3390ec] text-white' : 'text-gray-400 hover:text-white'
            }`}
          >
            <Users className="w-4 h-4" />
            <span>{isAr ? 'مجموعة جديدة' : 'New Group'}</span>
          </button>

          <button
            type="button"
            onClick={() => setTab('chat')}
            className={`flex-1 py-2 rounded-lg transition flex items-center justify-center gap-1.5 ${
              tab === 'chat' ? 'bg-[#3390ec] text-white' : 'text-gray-400 hover:text-white'
            }`}
          >
            <MessageSquare className="w-4 h-4" />
            <span>{isAr ? 'محادثة خاصة' : 'Direct Chat'}</span>
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4 text-xs">
          <div>
            <label className="block text-gray-400 mb-1 font-medium">
              {tab === 'channel'
                ? isAr ? 'اسم القناة' : 'Channel Name'
                : tab === 'group'
                ? isAr ? 'اسم المجموعة' : 'Group Name'
                : isAr ? 'اسم جهة الاتصال' : 'Contact Name'}
            </label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder={
                tab === 'channel'
                  ? isAr ? 'مثال: أخبار الرياضة' : 'e.g. Sports News'
                  : tab === 'group'
                  ? isAr ? 'مثال: فريق العمل' : 'e.g. Project Team'
                  : isAr ? 'مثال: محمد علي' : 'e.g. John Doe'
              }
              className={`w-full px-3 py-2.5 rounded-xl border focus:outline-none focus:border-[#3390ec] text-sm ${
                isDark ? 'bg-[#242f3d] border-[#2f3f50] text-white' : 'bg-gray-100 border-gray-300 text-gray-900'
              }`}
              required
              autoFocus
            />
          </div>

          <div>
            <label className="block text-gray-400 mb-1 font-medium">
              {isAr ? 'المعرف أو الرابط (@username)' : 'Username / Link'}
            </label>
            <div className="flex items-center">
              <span className="text-gray-400 text-sm pe-1">@</span>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="my_telegram_channel"
                className={`w-full px-3 py-2.5 rounded-xl border focus:outline-none focus:border-[#3390ec] text-sm font-mono ${
                  isDark ? 'bg-[#242f3d] border-[#2f3f50] text-white' : 'bg-gray-100 border-gray-300 text-gray-900'
                }`}
              />
            </div>
          </div>

          <div>
            <label className="block text-gray-400 mb-1 font-medium">
              {isAr ? 'الوصف (اختياري)' : 'Description (Optional)'}
            </label>
            <textarea
              rows={2}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder={isAr ? 'نبذة مختصرة عن القناة أو المجموعة...' : 'Brief description...'}
              className={`w-full px-3 py-2 rounded-xl border focus:outline-none focus:border-[#3390ec] text-sm resize-none ${
                isDark ? 'bg-[#242f3d] border-[#2f3f50] text-white' : 'bg-gray-100 border-gray-300 text-gray-900'
              }`}
            />
          </div>

          <button
            type="submit"
            className="w-full bg-[#3390ec] hover:bg-[#2b7ec9] text-white font-semibold py-3 rounded-xl transition flex items-center justify-center gap-2 shadow-lg shadow-[#3390ec]/25 text-sm"
          >
            <Plus className="w-4 h-4" />
            <span>
              {tab === 'channel'
                ? isAr ? 'إنشاء القناة' : 'Create Channel'
                : tab === 'group'
                ? isAr ? 'إنشاء المجموعة' : 'Create Group'
                : isAr ? 'بدء المحادثة' : 'Start Chat'}
            </span>
          </button>
        </form>
      </div>
    </div>
  );
};
