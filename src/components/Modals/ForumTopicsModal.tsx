import React from 'react';
import { useTelegram } from '../../context/TelegramContext';
import { Hash, MessageSquare, X, Plus, Pin, Lock } from 'lucide-react';

export const ForumTopicsModal: React.FC = () => {
  const { activeModal, setActiveModal, activeChat, settings } = useTelegram();
  const isArabic = settings.language === 'ar';

  if ((activeModal as any) !== 'forum-topics') return null;

  const topics = [
    { id: 1, title: isArabic ? 'العام والنقاشات' : 'General & Discussions', count: 142, icon: '💬', pinned: true },
    { id: 2, title: isArabic ? 'الإعلانات الرسمية' : 'Official Announcements', count: 28, icon: '📢', pinned: true },
    { id: 3, title: isArabic ? 'الروابط والمصادر' : 'Links & Resources', count: 95, icon: '🔗', pinned: false },
    { id: 4, title: isArabic ? 'الدعم الفني والأسئلة' : 'Tech Support & Q&A', count: 64, icon: '❓', pinned: false },
  ];

  return (
    <div
      id="forum-topics-modal-backdrop"
      className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4"
      onClick={() => setActiveModal('none')}
    >
      <div
        id="forum-topics-modal-container"
        className="w-full max-w-md bg-[var(--tg-theme-bg,#17212b)] text-white rounded-2xl shadow-2xl overflow-hidden border border-[var(--tg-theme-border,rgba(255,255,255,0.08))]"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between px-5 py-4 border-b border-[var(--tg-theme-border,rgba(255,255,255,0.08))]">
          <div className="flex items-center space-x-2 rtl:space-x-reverse">
            <Hash className="w-5 h-5 text-[#2481cc]" />
            <h3 className="font-bold text-base">
              {isArabic ? 'مواضيع المنتدى (Topics)' : 'Forum Topics'}
            </h3>
          </div>
          <button
            id="close-forum-topics-btn"
            onClick={() => setActiveModal('none')}
            className="p-1 rounded-full hover:bg-white/10 text-gray-400 hover:text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-4 space-y-2 max-h-96 overflow-y-auto custom-scrollbar">
          <div className="text-xs text-gray-400 mb-2 px-1">
            {isArabic ? `المواضيع في ${activeChat?.title || 'المجموعة'}` : `Topics in ${activeChat?.title || 'Group'}`}
          </div>
          {topics.map((t) => (
            <div
              key={t.id}
              className="flex items-center justify-between p-3 rounded-xl bg-white/5 hover:bg-white/10 cursor-pointer transition-colors"
              onClick={() => setActiveModal('none')}
            >
              <div className="flex items-center space-x-3 rtl:space-x-reverse">
                <span className="text-xl">{t.icon}</span>
                <div>
                  <div className="flex items-center space-x-1.5 rtl:space-x-reverse">
                    <span className="font-medium text-sm text-white">{t.title}</span>
                    {t.pinned && <Pin className="w-3.5 h-3.5 text-amber-400 fill-amber-400 rotate-45" />}
                  </div>
                  <span className="text-xs text-gray-400">{t.count} {isArabic ? 'رسالة' : 'messages'}</span>
                </div>
              </div>
              <MessageSquare className="w-4 h-4 text-gray-500" />
            </div>
          ))}
        </div>

        <div className="p-3 border-t border-[var(--tg-theme-border,rgba(255,255,255,0.08))] flex justify-end">
          <button
            id="create-topic-btn"
            onClick={() => setActiveModal('none')}
            className="flex items-center space-x-2 rtl:space-x-reverse px-4 py-2 bg-[#2481cc] hover:bg-[#1c70b4] text-white text-xs font-semibold rounded-lg transition-colors"
          >
            <Plus className="w-4 h-4" />
            <span>{isArabic ? 'إنشاء موضوع جديد' : 'New Topic'}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
