import React, { useState } from 'react';
import {
  X,
  Calendar,
  Clock,
  Send,
  Trash2,
  AlertCircle,
  FileText,
  CheckCircle2,
} from 'lucide-react';
import { ScheduledMessage } from '../types';

interface ScheduledMessagesModalProps {
  isOpen: boolean;
  onClose: () => void;
  chatId: string;
  chatTitle: string;
  scheduledMessages: ScheduledMessage[];
  onSendNow: (msgId: string) => Promise<void>;
  onDelete: (msgId: string) => Promise<void>;
  onScheduleNew: (text: string, date: number) => Promise<void>;
  isDark: boolean;
  lang: 'ar' | 'en';
}

export const ScheduledMessagesModal: React.FC<ScheduledMessagesModalProps> = ({
  isOpen,
  onClose,
  chatId,
  chatTitle,
  scheduledMessages,
  onSendNow,
  onDelete,
  onScheduleNew,
  isDark,
  lang,
}) => {
  const isAr = lang === 'ar';
  const [newText, setNewText] = useState('');
  const [selectedDate, setSelectedDate] = useState(() => {
    const d = new Date(Date.now() + 3600 * 1000); // 1 hour from now
    return d.toISOString().slice(0, 16);
  });
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isOpen) return null;

  const currentChatMessages = scheduledMessages.filter((m) => m.chatId === chatId);

  const handleSchedule = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newText.trim() || isSubmitting) return;
    const epochSec = Math.floor(new Date(selectedDate).getTime() / 1000);
    if (epochSec <= Math.floor(Date.now() / 1000)) {
      alert(isAr ? 'يرجى تحديد وقت مستقبلي للجدولة' : 'Please choose a future date/time');
      return;
    }

    setIsSubmitting(true);
    try {
      await onScheduleNew(newText.trim(), epochSec);
      setNewText('');
    } catch (err) {
      console.error('Failed to schedule message:', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const formatScheduledTime = (timestampSec: number) => {
    const d = new Date(timestampSec * 1000);
    return d.toLocaleString(isAr ? 'ar-EG' : 'en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200">
      <div
        className={`w-full max-w-lg rounded-2xl shadow-2xl flex flex-col max-h-[85vh] overflow-hidden border ${
          isDark ? 'bg-[#17212b] border-gray-700/80 text-white' : 'bg-white border-gray-200 text-gray-900'
        }`}
      >
        {/* Header */}
        <div
          className={`px-5 py-4 border-b flex items-center justify-between shrink-0 ${
            isDark ? 'bg-[#242f3d] border-gray-700/60' : 'bg-gray-50 border-gray-200'
          }`}
        >
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-[#3390ec]/20 text-[#3390ec]">
              <Calendar className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-bold text-sm">
                {isAr ? 'الرسائل المجدولة' : 'Scheduled Messages'}
              </h3>
              <p className="text-[11px] text-gray-400">{chatTitle}</p>
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

        {/* Schedule New Message Input Form */}
        <form onSubmit={handleSchedule} className="p-4 border-b border-gray-700/30 space-y-3 shrink-0">
          <div>
            <textarea
              rows={2}
              value={newText}
              onChange={(e) => setNewText(e.target.value)}
              placeholder={isAr ? 'اكتب نص الرسالة لجدولتها...' : 'Write message to schedule...'}
              className={`w-full px-3 py-2 rounded-xl text-xs outline-none resize-none border ${
                isDark ? 'bg-[#242f3d] border-gray-700 text-white placeholder-gray-400' : 'bg-gray-100 border-gray-200'
              }`}
            />
          </div>

          <div className="flex items-center gap-2">
            <div
              className={`flex-1 flex items-center gap-2 px-3 py-1.5 rounded-xl border text-xs ${
                isDark ? 'bg-[#242f3d] border-gray-700 text-white' : 'bg-gray-100 border-gray-200'
              }`}
            >
              <Clock className="w-4 h-4 text-gray-400 shrink-0" />
              <input
                type="datetime-local"
                value={selectedDate}
                onChange={(e) => setSelectedDate(e.target.value)}
                className="w-full bg-transparent outline-none text-xs"
              />
            </div>
            <button
              type="submit"
              disabled={!newText.trim() || isSubmitting}
              className="px-4 py-2 rounded-xl bg-[#3390ec] text-white text-xs font-semibold hover:bg-[#2881da] transition flex items-center gap-1.5 disabled:opacity-50 shadow-sm"
            >
              <Send className="w-3.5 h-3.5" />
              <span>{isAr ? 'جدولة' : 'Schedule'}</span>
            </button>
          </div>
        </form>

        {/* Pending Scheduled Messages List */}
        <div className="flex-1 overflow-y-auto p-4 space-y-3">
          {currentChatMessages.length === 0 ? (
            <div className="py-12 text-center text-gray-400 space-y-2">
              <Calendar className="w-10 h-10 mx-auto opacity-40 text-[#3390ec]" />
              <p className="text-xs">
                {isAr
                  ? 'لا توجد رسائل مجدولة في هذه المحادثة حالياً'
                  : 'No scheduled messages for this chat yet'}
              </p>
            </div>
          ) : (
            currentChatMessages.map((msg) => (
              <div
                key={msg.id}
                className={`p-3.5 rounded-xl border transition flex items-start justify-between gap-3 ${
                  isDark ? 'bg-[#242f3d]/60 border-gray-700/60' : 'bg-gray-50 border-gray-200'
                }`}
              >
                <div className="space-y-1.5 flex-1 min-w-0">
                  <div className="flex items-center gap-2 text-xs font-medium text-[#3390ec]">
                    <Clock className="w-3.5 h-3.5" />
                    <span>{formatScheduledTime(msg.scheduledDate)}</span>
                  </div>
                  <p className="text-xs break-words">{msg.text}</p>
                </div>

                <div className="flex items-center gap-1 shrink-0">
                  <button
                    type="button"
                    onClick={() => onSendNow(msg.id)}
                    className="p-1.5 rounded-lg text-emerald-400 hover:bg-emerald-500/10 transition"
                    title={isAr ? 'إرسال الآن' : 'Send now'}
                  >
                    <Send className="w-4 h-4" />
                  </button>
                  <button
                    type="button"
                    onClick={() => onDelete(msg.id)}
                    className="p-1.5 rounded-lg text-red-400 hover:bg-red-500/10 transition"
                    title={isAr ? 'حذف' : 'Delete'}
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};
