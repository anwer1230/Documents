import React, { useState } from 'react';
import { Megaphone, Users, Lock, X, Hash } from 'lucide-react';
import { TelegramChat, ChatType } from '../types';

interface NewChatModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreateChat: (chat: Partial<TelegramChat>) => void;
  lang?: 'ar' | 'en';
  isDark?: boolean;
}

export const NewChatModal: React.FC<NewChatModalProps> = ({
  isOpen,
  onClose,
  onCreateChat,
  lang = 'ar',
  isDark = true,
}) => {
  const isAr = lang === 'ar';
  const [step, setStep] = useState<'type' | 'details'>('type');
  const [chatType, setChatType] = useState<ChatType>('channel');
  const [title, setTitle] = useState('');
  const [username, setUsername] = useState('');
  const [description, setDescription] = useState('');
  const [isForum, setIsForum] = useState(false);

  if (!isOpen) return null;

  const handleSelectType = (type: ChatType) => {
    setChatType(type);
    setStep('details');
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;

    onCreateChat({
      title: title.trim(),
      username: username.trim() || undefined,
      type: chatType,
      isForum: chatType === 'supergroup' ? isForum : false,
      botInfo: description ? { description } : undefined,
    });

    // Reset and close
    setTitle('');
    setUsername('');
    setDescription('');
    setStep('type');
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in select-none">
      <div
        className={`w-full max-w-md rounded-3xl shadow-2xl p-6 border transition-colors ${
          isDark ? 'bg-[#17212b] border-[#242f3d] text-white' : 'bg-white border-gray-200 text-gray-900'
        }`}
      >
        <div className="flex items-center justify-between pb-3 border-b border-gray-700/40">
          <h3 className="font-bold text-base">
            {step === 'type'
              ? isAr
                ? 'إنشاء محادثة جديدة'
                : 'New Chat'
              : chatType === 'channel'
              ? isAr
                ? 'إنشاء قناة جديدة'
                : 'New Channel'
              : chatType === 'supergroup'
              ? isAr
                ? 'إنشاء مجموعة جديدة'
                : 'New Group'
              : isAr
              ? 'محادثة سرية جديدة'
              : 'New Secret Chat'}
          </h3>
          <button
            onClick={() => {
              setStep('type');
              onClose();
            }}
            className="p-1 rounded-lg hover:bg-white/10 text-gray-400 hover:text-white transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {step === 'type' ? (
          <div className="mt-4 space-y-2">
            <button
              onClick={() => handleSelectType('channel')}
              className="w-full p-4 rounded-2xl flex items-center gap-4 hover:bg-white/10 transition text-start group border border-transparent hover:border-[#3390ec]/30"
            >
              <div className="w-12 h-12 rounded-full bg-gradient-to-tr from-sky-500 to-blue-600 flex items-center justify-center text-white shadow-md shrink-0 group-hover:scale-105 transition">
                <Megaphone className="w-6 h-6" />
              </div>
              <div>
                <h4 className="font-bold text-sm">{isAr ? 'قناة جديدة' : 'New Channel'}</h4>
                <p className="text-xs text-gray-400 mt-0.5">
                  {isAr ? 'لبث رسائلك وأخبارك لجمهور غير محدود' : 'For broadcasting your messages to unlimited audiences'}
                </p>
              </div>
            </button>

            <button
              onClick={() => handleSelectType('supergroup')}
              className="w-full p-4 rounded-2xl flex items-center gap-4 hover:bg-white/10 transition text-start group border border-transparent hover:border-emerald-500/30"
            >
              <div className="w-12 h-12 rounded-full bg-gradient-to-tr from-emerald-500 to-teal-600 flex items-center justify-center text-white shadow-md shrink-0 group-hover:scale-105 transition">
                <Users className="w-6 h-6" />
              </div>
              <div>
                <h4 className="font-bold text-sm">{isAr ? 'مجموعة جديدة' : 'New Group'}</h4>
                <p className="text-xs text-gray-400 mt-0.5">
                  {isAr ? 'تتسع لـ 200,000 عضو مع دعم منتديات المواضيع' : 'Up to 200,000 members with forum topics'}
                </p>
              </div>
            </button>

            <button
              onClick={() => handleSelectType('private')}
              className="w-full p-4 rounded-2xl flex items-center gap-4 hover:bg-white/10 transition text-start group border border-transparent hover:border-violet-500/30"
            >
              <div className="w-12 h-12 rounded-full bg-gradient-to-tr from-violet-500 to-purple-600 flex items-center justify-center text-white shadow-md shrink-0 group-hover:scale-105 transition">
                <Lock className="w-6 h-6" />
              </div>
              <div>
                <h4 className="font-bold text-sm">{isAr ? 'محادثة سرية' : 'New Secret Chat'}</h4>
                <p className="text-xs text-gray-400 mt-0.5">
                  {isAr ? 'تشفير شامل من الطرفين وتدمير ذاتي للرسائل' : 'End-to-end encryption with self-destructing messages'}
                </p>
              </div>
            </button>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="mt-4 space-y-4">
            <div>
              <label className="block text-xs font-semibold text-gray-400 mb-1">
                {isAr ? 'الاسم' : 'Name'} *
              </label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder={isAr ? 'أدخل اسم القناة أو المجموعة' : 'Enter chat title'}
                required
                autoFocus
                className={`w-full px-4 py-2.5 rounded-xl text-sm outline-none border transition ${
                  isDark
                    ? 'bg-black/30 border-gray-700 text-white focus:border-[#3390ec]'
                    : 'bg-white border-gray-300 text-gray-900 focus:border-[#3390ec]'
                }`}
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-gray-400 mb-1">
                {isAr ? 'اسم المستخدم (الرابط العام)' : 'Username (Public Link)'}
              </label>
              <div className="relative">
                <span className="absolute inset-y-0 start-3 flex items-center text-gray-400 text-sm font-mono">@</span>
                <input
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value.replace(/[^a-zA-Z0-9_]/g, ''))}
                  placeholder="channel_name"
                  dir="ltr"
                  className={`w-full ps-8 pe-4 py-2.5 rounded-xl text-sm font-mono outline-none border transition ${
                    isDark
                      ? 'bg-black/30 border-gray-700 text-white focus:border-[#3390ec]'
                      : 'bg-white border-gray-300 text-gray-900 focus:border-[#3390ec]'
                  }`}
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-gray-400 mb-1">
                {isAr ? 'الوصف (اختياري)' : 'Description (optional)'}
              </label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder={isAr ? 'اكتب نبذة مختصرة...' : 'Short description...'}
                rows={2}
                className={`w-full px-4 py-2 rounded-xl text-xs outline-none border transition ${
                  isDark
                    ? 'bg-black/30 border-gray-700 text-white focus:border-[#3390ec]'
                    : 'bg-white border-gray-300 text-gray-900 focus:border-[#3390ec]'
                }`}
              />
            </div>

            {chatType === 'supergroup' && (
              <label className="flex items-center gap-3 p-3 rounded-xl bg-black/20 hover:bg-black/30 cursor-pointer transition">
                <input
                  type="checkbox"
                  checked={isForum}
                  onChange={(e) => setIsForum(e.target.checked)}
                  className="w-4 h-4 rounded text-[#3390ec] focus:ring-[#3390ec] accent-[#3390ec]"
                />
                <div className="text-xs">
                  <span className="font-bold block text-white">
                    {isAr ? 'تفعيل منتديات المواضيع (Forum Topics)' : 'Enable Forum Topics'}
                  </span>
                  <span className="text-gray-400">
                    {isAr ? 'تنظيم محادثات المجموعة في مواضيع منفصلة' : 'Organize chats into discrete topic threads'}
                  </span>
                </div>
              </label>
            )}

            <div className="flex items-center justify-between pt-2">
              <button
                type="button"
                onClick={() => setStep('type')}
                className={`px-4 py-2 rounded-xl text-xs font-semibold transition ${
                  isDark ? 'hover:bg-white/10 text-gray-400' : 'hover:bg-gray-100 text-gray-600'
                }`}
              >
                {isAr ? 'رجوع' : 'Back'}
              </button>
              <button
                type="submit"
                className="px-6 py-2.5 rounded-xl text-sm font-bold bg-[#3390ec] hover:bg-[#2b7ec9] text-white shadow-lg shadow-[#3390ec]/25 transition active:scale-95"
              >
                {isAr ? 'إنشاء' : 'Create'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};
