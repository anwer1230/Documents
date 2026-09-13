import React, { useState } from 'react';
import {
  MessageSquare,
  Plus,
  Hash,
  Pin,
  Lock,
  ChevronLeft,
  ChevronRight,
  Layers,
  Sparkles,
  X,
} from 'lucide-react';
import { TelegramForumTopic } from '../types';

interface ForumTopicsBarProps {
  topics: TelegramForumTopic[];
  activeTopicId?: number | null; // null or undefined means "All Topics"
  onSelectTopic: (topicId: number | null) => void;
  onCreateTopic?: (topic: Omit<TelegramForumTopic, 'id'>) => void;
  lang: 'ar' | 'en';
  isDark: boolean;
}

export const ForumTopicsBar: React.FC<ForumTopicsBarProps> = ({
  topics,
  activeTopicId,
  onSelectTopic,
  onCreateTopic,
  lang,
  isDark,
}) => {
  const isAr = lang === 'ar';
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [newTitle, setNewTitle] = useState('');
  const [newEmoji, setNewEmoji] = useState('💬');
  const [newColor, setNewColor] = useState('#3390ec');

  const EMOJI_OPTIONS = ['💬', '📢', '🎮', '💡', '⭐', '🔥', '🚀', '🛠️', '🎨', '💼'];
  const COLOR_OPTIONS = ['#3390ec', '#f59e0b', '#10b981', '#8b5cf6', '#ef4444', '#06b6d4', '#ec4899'];

  const handleCreateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTitle.trim() || !onCreateTopic) return;
    onCreateTopic({
      title: newTitle.trim(),
      iconEmoji: newEmoji,
      iconColor: newColor,
      unreadCount: 0,
    });
    setNewTitle('');
    setIsCreateModalOpen(false);
  };

  return (
    <div
      className={`border-b shrink-0 select-none transition-colors ${
        isDark ? 'bg-[#1e2a37] border-gray-800' : 'bg-gray-50 border-gray-200'
      }`}
    >
      <div className="flex items-center justify-between px-3 py-1.5 overflow-x-auto gap-1.5 no-scrollbar">
        {/* All Topics Tab */}
        <button
          type="button"
          onClick={() => onSelectTopic(null)}
          className={`flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold whitespace-nowrap transition-all ${
            activeTopicId === null || activeTopicId === undefined
              ? 'bg-[#3390ec] text-white shadow-sm'
              : isDark
              ? 'text-gray-300 hover:bg-white/5'
              : 'text-gray-700 hover:bg-gray-200'
          }`}
        >
          <Layers className="w-3.5 h-3.5" />
          <span>{isAr ? 'كافة المواضيع' : 'All Topics'}</span>
        </button>

        {/* Individual Topic Tabs */}
        {topics.map((topic) => {
          const isActive = activeTopicId === topic.id;
          return (
            <button
              key={topic.id}
              type="button"
              onClick={() => onSelectTopic(topic.id)}
              className={`flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold whitespace-nowrap transition-all border ${
                isActive
                  ? 'border-transparent text-white shadow-sm'
                  : isDark
                  ? 'border-gray-700/60 text-gray-300 hover:bg-white/5'
                  : 'border-gray-200 text-gray-700 hover:bg-white'
              }`}
              style={
                isActive
                  ? { backgroundColor: topic.iconColor || '#3390ec' }
                  : undefined
              }
            >
              <span>{topic.iconEmoji || '💬'}</span>
              <span className="truncate max-w-[140px]">{topic.title}</span>
              {topic.isPinned && <Pin className="w-2.5 h-2.5 rotate-45 opacity-70" />}
              {topic.isClosed && <Lock className="w-2.5 h-2.5 opacity-70" />}
              {!!topic.unreadCount && topic.unreadCount > 0 && (
                <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-white/20 font-bold">
                  {topic.unreadCount}
                </span>
              )}
            </button>
          );
        })}

        {/* Create Topic Button */}
        {onCreateTopic && (
          <button
            type="button"
            onClick={() => setIsCreateModalOpen(true)}
            className={`flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium text-[#3390ec] transition ${
              isDark ? 'hover:bg-[#3390ec]/10' : 'hover:bg-blue-50'
            }`}
            title={isAr ? 'إنشاء موضوع جديد' : 'New Topic'}
          >
            <Plus className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">{isAr ? 'موضوع جديد' : 'New Topic'}</span>
          </button>
        )}
      </div>

      {/* Create Topic Modal */}
      {isCreateModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-150">
          <div
            className={`w-full max-w-sm rounded-2xl shadow-2xl border p-5 overflow-hidden ${
              isDark ? 'bg-[#17212b] border-gray-700 text-white' : 'bg-white border-gray-200 text-gray-900'
            }`}
          >
            <div className="flex items-center justify-between pb-3 border-b border-gray-700/30 mb-4">
              <h3 className="font-bold text-sm flex items-center gap-2">
                <Hash className="w-4 h-4 text-[#3390ec]" />
                <span>{isAr ? 'إنشاء موضوع جديد في المنتدى' : 'Create Forum Topic'}</span>
              </h3>
              <button
                onClick={() => setIsCreateModalOpen(false)}
                className="p-1 rounded-full text-gray-400 hover:text-white"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleCreateSubmit} className="space-y-4">
              <div>
                <label className="text-xs text-gray-400 block mb-1">
                  {isAr ? 'عنوان الموضوع' : 'Topic Title'}
                </label>
                <input
                  type="text"
                  required
                  autoFocus
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  placeholder={isAr ? 'مثال: أسئلة وأجوبة تقنية...' : 'e.g., Tech Q&A...'}
                  className={`w-full px-3 py-2 rounded-xl text-xs outline-none border ${
                    isDark ? 'bg-[#242f3d] border-gray-700 text-white' : 'bg-gray-100 border-gray-200 text-gray-900'
                  }`}
                />
              </div>

              <div>
                <label className="text-xs text-gray-400 block mb-1">
                  {isAr ? 'أيقونة الرمز التعبيري' : 'Icon Emoji'}
                </label>
                <div className="flex flex-wrap gap-1.5">
                  {EMOJI_OPTIONS.map((em) => (
                    <button
                      key={em}
                      type="button"
                      onClick={() => setNewEmoji(em)}
                      className={`w-8 h-8 rounded-lg flex items-center justify-center text-base transition ${
                        newEmoji === em
                          ? 'bg-[#3390ec] text-white ring-2 ring-[#3390ec]'
                          : isDark
                          ? 'bg-[#242f3d] hover:bg-white/10'
                          : 'bg-gray-100 hover:bg-gray-200'
                      }`}
                    >
                      {em}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="text-xs text-gray-400 block mb-1">
                  {isAr ? 'لون الموضوع' : 'Topic Color'}
                </label>
                <div className="flex items-center gap-2">
                  {COLOR_OPTIONS.map((c) => (
                    <button
                      key={c}
                      type="button"
                      onClick={() => setNewColor(c)}
                      className={`w-6 h-6 rounded-full transition transform ${
                        newColor === c ? 'scale-125 ring-2 ring-white shadow-md' : 'opacity-80 hover:opacity-100'
                      }`}
                      style={{ backgroundColor: c }}
                    />
                  ))}
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setIsCreateModalOpen(false)}
                  className={`px-4 py-2 rounded-xl text-xs font-semibold ${
                    isDark ? 'text-gray-400 hover:text-white' : 'text-gray-600 hover:text-gray-900'
                  }`}
                >
                  {isAr ? 'إلغاء' : 'Cancel'}
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 rounded-xl text-xs font-bold bg-[#3390ec] hover:bg-[#2881da] text-white shadow"
                >
                  {isAr ? 'إنشاء الموضوع' : 'Create Topic'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
