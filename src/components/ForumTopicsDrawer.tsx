import React, { useState } from 'react';
import {
  X,
  MessageSquare,
  Plus,
  Pin,
  Lock,
  Search,
  Check,
  Hash,
} from 'lucide-react';
import { TelegramChat, ForumTopic } from '../types';

interface ForumTopicsDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  chat: TelegramChat | null;
  selectedTopicId: number | null;
  onSelectTopic: (topicId: number | null) => void;
  onCreateTopic?: (title: string, color?: number) => void;
  lang?: 'ar' | 'en';
  isDark?: boolean;
}

const TOPIC_COLORS = [
  0x6fb9f0, // Blue
  0xffd67e, // Amber
  0xcb86db, // Purple
  0x8eee98, // Green
  0xff93b2, // Rose
  0xfb6f5f, // Coral
];

export const ForumTopicsDrawer: React.FC<ForumTopicsDrawerProps> = ({
  isOpen,
  onClose,
  chat,
  selectedTopicId,
  onSelectTopic,
  onCreateTopic,
  lang = 'ar',
  isDark = true,
}) => {
  const isAr = lang === 'ar';
  const [searchQuery, setSearchQuery] = useState('');
  const [isCreating, setIsCreating] = useState(false);
  const [newTitle, setNewTitle] = useState('');
  const [newColor, setNewColor] = useState(TOPIC_COLORS[0]);

  if (!isOpen || !chat) return null;

  const topics: ForumTopic[] = chat.topics || [];
  const filteredTopics = topics.filter((t) =>
    t.title.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTitle.trim()) return;
    if (onCreateTopic) {
      onCreateTopic(newTitle.trim(), newColor);
    }
    setNewTitle('');
    setIsCreating(false);
  };

  return (
    <div
      id="forum_topics_backdrop"
      className="fixed inset-0 z-50 flex bg-black/40 backdrop-blur-xs transition-opacity"
      onClick={onClose}
    >
      <div
        id="forum_topics_drawer"
        dir={isAr ? 'rtl' : 'ltr'}
        onClick={(e) => e.stopPropagation()}
        className={`w-full max-w-md h-full flex flex-col shadow-2xl transition-all ${
          isDark ? 'bg-[#18222d] text-white' : 'bg-white text-slate-900'
        } ${isAr ? 'mr-auto' : 'ml-auto'}`}
      >
        {/* Header */}
        <div
          className={`px-4 py-3 border-b flex items-center justify-between shrink-0 ${
            isDark ? 'border-slate-800 bg-[#1c2733]' : 'border-slate-200 bg-slate-50'
          }`}
        >
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-sky-500/10 text-sky-500 flex items-center justify-center">
              <MessageSquare className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-sm font-bold">
                {isAr ? 'مواضيع المنتدى (Topics)' : 'Forum Topics'}
              </h3>
              <p className="text-[11px] text-slate-400">{chat.title}</p>
            </div>
          </div>

          <div className="flex items-center gap-1">
            {onCreateTopic && !isCreating && (
              <button
                id="create_topic_toggle_btn"
                onClick={() => setIsCreating(true)}
                className="p-1.5 rounded-lg text-sky-500 hover:bg-sky-500/10 transition-colors"
                title={isAr ? 'موضوع جديد' : 'New Topic'}
              >
                <Plus className="w-4 h-4" />
              </button>
            )}
            <button
              id="close_topics_drawer_btn"
              onClick={onClose}
              className="p-1.5 rounded-lg text-slate-400 hover:text-slate-200 transition-colors"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Search & "All Topics" filter */}
        <div className="p-3 space-y-2 border-b border-slate-800/60">
          <div
            className={`flex items-center gap-2 px-3 py-1.5 rounded-xl border ${
              isDark ? 'bg-[#131b24] border-slate-800 text-slate-200' : 'bg-slate-100 border-slate-200 text-slate-800'
            }`}
          >
            <Search className="w-3.5 h-3.5 text-slate-400 shrink-0" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder={isAr ? 'بحث في المواضيع...' : 'Search topics...'}
              className="w-full bg-transparent text-xs outline-hidden placeholder-slate-400"
            />
          </div>

          <button
            id="topic_all_btn"
            onClick={() => onSelectTopic(null)}
            className={`w-full px-3 py-2 rounded-xl text-xs font-semibold flex items-center justify-between transition-all ${
              selectedTopicId === null
                ? 'bg-sky-500 text-white shadow-sm'
                : isDark
                ? 'bg-slate-800/60 hover:bg-slate-800 text-slate-300'
                : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
            }`}
          >
            <div className="flex items-center gap-2">
              <Hash className="w-4 h-4" />
              <span>{isAr ? 'كافة المواضيع والرسائل' : 'All Topics & Messages'}</span>
            </div>
            {selectedTopicId === null && <Check className="w-3.5 h-3.5" />}
          </button>
        </div>

        {/* Create Topic Form */}
        {isCreating && (
          <form
            onSubmit={handleCreate}
            className={`p-3 border-b space-y-2.5 ${
              isDark ? 'bg-[#1c2733]/80 border-slate-800' : 'bg-slate-50 border-slate-200'
            }`}
          >
            <div className="flex items-center justify-between text-xs font-bold text-sky-500">
              <span>{isAr ? 'إنشاء موضوع منتدى جديد' : 'Create New Forum Topic'}</span>
              <button
                type="button"
                onClick={() => setIsCreating(false)}
                className="text-slate-400 hover:text-slate-200 text-[11px]"
              >
                {isAr ? 'إلغاء' : 'Cancel'}
              </button>
            </div>

            <input
              type="text"
              value={newTitle}
              onChange={(e) => setNewTitle(e.target.value)}
              placeholder={isAr ? 'عنوان الموضوع...' : 'Topic title...'}
              autoFocus
              className={`w-full px-3 py-2 text-xs rounded-xl border outline-hidden ${
                isDark ? 'bg-[#131b24] border-slate-700 text-white' : 'bg-white border-slate-300 text-slate-900'
              }`}
            />

            <div className="flex items-center justify-between">
              <div className="flex items-center gap-1.5">
                {TOPIC_COLORS.map((c) => (
                  <button
                    key={c}
                    type="button"
                    onClick={() => setNewColor(c)}
                    style={{ backgroundColor: `#${c.toString(16).padStart(6, '0')}` }}
                    className={`w-5 h-5 rounded-full transition-transform ${
                      newColor === c ? 'scale-125 ring-2 ring-white ring-offset-1' : 'opacity-80'
                    }`}
                  />
                ))}
              </div>

              <button
                type="submit"
                disabled={!newTitle.trim()}
                className="px-3 py-1.5 bg-sky-500 disabled:opacity-50 text-white text-xs font-bold rounded-xl transition"
              >
                {isAr ? 'إنشاء' : 'Create'}
              </button>
            </div>
          </form>
        )}

        {/* Topics List */}
        <div className="flex-1 overflow-y-auto p-2 space-y-1 custom-scrollbar">
          {filteredTopics.length === 0 ? (
            <div className="p-8 text-center text-xs text-slate-400">
              {topics.length === 0
                ? isAr
                  ? 'لا توجد مواضيع في هذه المجموعة بعد.'
                  : 'No topics in this group yet.'
                : isAr
                ? 'لا توجد نتائج مطابقة لبحثك.'
                : 'No matching topics found.'}
            </div>
          ) : (
            filteredTopics.map((topic) => {
              const isSelected = selectedTopicId === topic.id;
              const hexColor = topic.iconColor
                ? `#${topic.iconColor.toString(16).padStart(6, '0')}`
                : '#6fb9f0';

              return (
                <button
                  key={topic.id}
                  id={`topic_item_${topic.id}`}
                  onClick={() => onSelectTopic(topic.id)}
                  className={`w-full p-2.5 rounded-xl text-left flex items-center justify-between gap-3 transition-all ${
                    isSelected
                      ? 'bg-sky-500/15 border border-sky-500/40 text-sky-400 font-semibold'
                      : isDark
                      ? 'hover:bg-slate-800/60 text-slate-200 border border-transparent'
                      : 'hover:bg-slate-100 text-slate-800 border border-transparent'
                  }`}
                >
                  <div className="flex items-center gap-2.5 min-w-0">
                    <div
                      style={{ backgroundColor: hexColor }}
                      className="w-7 h-7 rounded-lg flex items-center justify-center text-white shrink-0 text-xs font-bold shadow-xs"
                    >
                      {topic.iconEmojiId || topic.title.charAt(0).toUpperCase()}
                    </div>

                    <div className="min-w-0 text-left">
                      <div className="flex items-center gap-1.5 truncate text-xs">
                        <span className="truncate">{topic.title}</span>
                        {topic.isGeneral && (
                          <span className="text-[10px] px-1 rounded bg-slate-700/60 text-slate-300">
                            {isAr ? 'عام' : 'General'}
                          </span>
                        )}
                        {topic.isPinned && <Pin className="w-3 h-3 text-amber-400 shrink-0 rotate-45" />}
                        {topic.isClosed && <Lock className="w-3 h-3 text-rose-400 shrink-0" />}
                      </div>
                    </div>
                  </div>

                  {topic.unreadCount && topic.unreadCount > 0 ? (
                    <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-sky-500 text-white shrink-0">
                      {topic.unreadCount}
                    </span>
                  ) : null}
                </button>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
};

export default ForumTopicsDrawer;
