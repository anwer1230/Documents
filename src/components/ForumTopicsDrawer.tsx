import React, { useState } from 'react';
import {
  X,
  Hash,
  Plus,
  Lock,
  Pin,
  MessageSquare,
  Search,
  Check,
  ChevronRight,
  Shield,
  Layers,
} from 'lucide-react';
import { ForumTopic } from '../types';

interface ForumTopicsDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  topics: ForumTopic[];
  selectedTopicId: number | null;
  onSelectTopic: (topicId: number | null) => void;
  onCreateTopic: (title: string, iconColor: number) => Promise<void>;
  chatTitle: string;
  isDark: boolean;
  lang: 'ar' | 'en';
}

const TOPIC_COLOR_PALETTE = [
  0x6fb9f0, // Blue
  0xffd67e, // Amber
  0xcb86db, // Purple
  0x8eee98, // Green
  0xff93b2, // Pink
  0xfb6f5f, // Red
];

export const ForumTopicsDrawer: React.FC<ForumTopicsDrawerProps> = ({
  isOpen,
  onClose,
  topics,
  selectedTopicId,
  onSelectTopic,
  onCreateTopic,
  chatTitle,
  isDark,
  lang,
}) => {
  const isAr = lang === 'ar';
  const [searchQuery, setSearchQuery] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newTopicTitle, setNewTopicTitle] = useState('');
  const [selectedColor, setSelectedColor] = useState(TOPIC_COLOR_PALETTE[0]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isOpen) return null;

  const filteredTopics = topics.filter((t) =>
    t.title.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTopicTitle.trim() || isSubmitting) return;
    setIsSubmitting(true);
    try {
      await onCreateTopic(newTopicTitle.trim(), selectedColor);
      setNewTopicTitle('');
      setShowCreateModal(false);
    } catch (err) {
      console.error('Failed to create topic:', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const getHexColor = (colorNum?: number) => {
    if (!colorNum) return '#3390ec';
    return `#${colorNum.toString(16).padStart(6, '0')}`;
  };

  return (
    <div className="fixed inset-y-0 right-0 z-40 w-80 sm:w-96 shadow-2xl flex flex-col border-l transition-all duration-300 animate-in slide-in-from-right">
      <div
        className={`h-full flex flex-col ${
          isDark ? 'bg-[#17212b] border-gray-700/80 text-white' : 'bg-white border-gray-200 text-gray-900'
        }`}
      >
        {/* Header */}
        <div
          className={`px-4 py-3 border-b flex items-center justify-between shrink-0 ${
            isDark ? 'bg-[#242f3d] border-gray-700/60' : 'bg-gray-50 border-gray-200'
          }`}
        >
          <div className="flex items-center gap-2">
            <Layers className="w-5 h-5 text-[#3390ec]" />
            <div>
              <h3 className="font-semibold text-sm">
                {isAr ? 'مواضيع المنتدى' : 'Forum Topics'}
              </h3>
              <p className="text-[11px] text-gray-400 truncate max-w-[200px]">
                {chatTitle}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-full text-gray-400 hover:text-white hover:bg-white/10 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Search & Actions Bar */}
        <div className="p-3 border-b border-gray-700/30 flex items-center gap-2 shrink-0">
          <div
            className={`flex-1 flex items-center gap-2 px-3 py-1.5 rounded-xl border text-xs ${
              isDark ? 'bg-[#242f3d] border-gray-700 text-white' : 'bg-gray-100 border-gray-200 text-gray-900'
            }`}
          >
            <Search className="w-3.5 h-3.5 text-gray-400 shrink-0" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder={isAr ? 'بحث في المواضيع...' : 'Search topics...'}
              className="w-full bg-transparent outline-none text-xs"
            />
          </div>
          <button
            type="button"
            onClick={() => setShowCreateModal(true)}
            className="p-2 rounded-xl bg-[#3390ec] text-white hover:bg-[#2881da] transition shrink-0 shadow-sm"
            title={isAr ? 'إنشاء موضوع جديد' : 'New Topic'}
          >
            <Plus className="w-4 h-4" />
          </button>
        </div>

        {/* Topics List */}
        <div className="flex-1 overflow-y-auto divide-y divide-gray-700/20">
          {/* General / All Messages Item */}
          <button
            type="button"
            onClick={() => {
              onSelectTopic(null);
              onClose();
            }}
            className={`w-full px-4 py-3 flex items-center gap-3 text-left transition ${
              selectedTopicId === null
                ? isDark
                  ? 'bg-white/10'
                  : 'bg-blue-50'
                : isDark
                ? 'hover:bg-white/5'
                : 'hover:bg-gray-50'
            }`}
          >
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center text-white shrink-0 shadow-sm">
              <MessageSquare className="w-5 h-5" />
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center justify-between">
                <span className="font-semibold text-sm">
                  {isAr ? 'جميع الرسائل (العامة)' : 'All Messages (General)'}
                </span>
                {selectedTopicId === null && <Check className="w-4 h-4 text-[#3390ec]" />}
              </div>
              <p className="text-xs text-gray-400 truncate mt-0.5">
                {isAr ? 'استعراض تدفق الرسائل العام للمنتدى' : 'View full forum stream'}
              </p>
            </div>
          </button>

          {filteredTopics.map((topic) => {
            const isSelected = selectedTopicId === topic.id;
            const topicColor = getHexColor(topic.iconColor);

            return (
              <button
                key={topic.id}
                type="button"
                onClick={() => {
                  onSelectTopic(topic.id);
                  onClose();
                }}
                className={`w-full px-4 py-3 flex items-center gap-3 text-left transition ${
                  isSelected
                    ? isDark
                      ? 'bg-white/10'
                      : 'bg-blue-50'
                    : isDark
                    ? 'hover:bg-white/5'
                    : 'hover:bg-gray-50'
                }`}
              >
                <div
                  className="w-10 h-10 rounded-xl flex items-center justify-center text-white font-bold shrink-0 shadow-sm"
                  style={{ backgroundColor: topicColor }}
                >
                  {topic.iconEmojiId ? (
                    <span className="text-lg">{topic.iconEmojiId}</span>
                  ) : (
                    <Hash className="w-5 h-5" />
                  )}
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-1">
                    <span className="font-semibold text-sm truncate flex items-center gap-1.5">
                      {topic.title}
                      {topic.isPinned && <Pin className="w-3.5 h-3.5 text-amber-400 rotate-45" />}
                      {topic.isClosed && <Lock className="w-3.5 h-3.5 text-gray-400" />}
                    </span>
                    {topic.unreadCount ? (
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-[#3390ec] text-white">
                        {topic.unreadCount}
                      </span>
                    ) : isSelected ? (
                      <Check className="w-4 h-4 text-[#3390ec]" />
                    ) : null}
                  </div>
                  <p className="text-xs text-gray-400 truncate mt-0.5">
                    {topic.isClosed
                      ? isAr
                        ? 'موضوع مغلق'
                        : 'Closed topic'
                      : topic.isPinned
                      ? isAr
                        ? 'موضوع مثبت'
                        : 'Pinned topic'
                      : isAr
                      ? `معرف الموضوع: #${topic.id}`
                      : `Topic #${topic.id}`}
                  </p>
                </div>
              </button>
            );
          })}

          {filteredTopics.length === 0 && (
            <div className="p-8 text-center text-gray-400 text-xs">
              {isAr ? 'لم يتم العثور على مواضيع مطابقة' : 'No topics found'}
            </div>
          )}
        </div>

        {/* Modal: Create Topic */}
        {showCreateModal && (
          <div className="absolute inset-0 z-50 bg-black/70 backdrop-blur-sm p-4 flex items-center justify-center">
            <div
              className={`w-full max-w-xs rounded-2xl p-4 shadow-2xl border ${
                isDark ? 'bg-[#242f3d] border-gray-700 text-white' : 'bg-white border-gray-200 text-gray-900'
              }`}
            >
              <div className="flex items-center justify-between mb-3">
                <h4 className="font-bold text-sm">
                  {isAr ? 'إنشاء موضوع جديد' : 'New Forum Topic'}
                </h4>
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="p-1 rounded-full text-gray-400 hover:text-white"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              <form onSubmit={handleCreate} className="space-y-3">
                <div>
                  <label className="text-xs text-gray-400 block mb-1">
                    {isAr ? 'عنوان الموضوع' : 'Topic Title'}
                  </label>
                  <input
                    type="text"
                    value={newTopicTitle}
                    onChange={(e) => setNewTopicTitle(e.target.value)}
                    placeholder={isAr ? 'مثال: التحديثات، الدعم...' : 'e.g. Announcements, Support...'}
                    required
                    className={`w-full px-3 py-2 rounded-xl text-xs outline-none border ${
                      isDark ? 'bg-[#17212b] border-gray-700 text-white' : 'bg-gray-100 border-gray-300 text-gray-900'
                    }`}
                  />
                </div>

                <div>
                  <label className="text-xs text-gray-400 block mb-1">
                    {isAr ? 'لون أيقونة الموضوع' : 'Icon Color'}
                  </label>
                  <div className="flex items-center gap-2">
                    {TOPIC_COLOR_PALETTE.map((color) => (
                      <button
                        key={color}
                        type="button"
                        onClick={() => setSelectedColor(color)}
                        className={`w-7 h-7 rounded-lg transition-transform ${
                          selectedColor === color ? 'scale-110 ring-2 ring-white shadow-md' : 'opacity-80'
                        }`}
                        style={{ backgroundColor: getHexColor(color) }}
                      />
                    ))}
                  </div>
                </div>

                <div className="flex items-center gap-2 pt-2">
                  <button
                    type="button"
                    onClick={() => setShowCreateModal(false)}
                    className="flex-1 py-2 rounded-xl border border-gray-600 text-xs font-medium hover:bg-white/5 transition"
                  >
                    {isAr ? 'إلغاء' : 'Cancel'}
                  </button>
                  <button
                    type="submit"
                    disabled={!newTopicTitle.trim() || isSubmitting}
                    className="flex-1 py-2 rounded-xl bg-[#3390ec] text-white text-xs font-semibold hover:bg-[#2881da] transition disabled:opacity-50"
                  >
                    {isSubmitting
                      ? isAr
                        ? 'جارِ الإنشاء...'
                        : 'Creating...'
                      : isAr
                      ? 'إنشاء'
                      : 'Create'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
