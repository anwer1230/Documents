import React, { useState } from 'react';
import {
  Clock,
  Send,
  Trash2,
  Calendar,
  X,
  CheckCircle2,
  AlertCircle,
  Plus,
  Sparkles,
} from 'lucide-react';
import { ScheduledMessage, TelegramChat } from '../types';

interface ScheduledMessagesModalProps {
  isOpen: boolean;
  onClose: () => void;
  chat: TelegramChat;
  scheduledMessages: ScheduledMessage[];
  onScheduleMessage: (msg: { text: string; scheduledTime: number; topicId?: number }) => void;
  onSendNow: (msgId: string) => void;
  onDeleteScheduled: (msgId: string) => void;
  isDark: boolean;
  lang: 'ar' | 'en';
  activeTopicId?: number | null;
}

export const ScheduledMessagesModal: React.FC<ScheduledMessagesModalProps> = ({
  isOpen,
  onClose,
  chat,
  scheduledMessages,
  onScheduleMessage,
  onSendNow,
  onDeleteScheduled,
  isDark,
  lang,
  activeTopicId,
}) => {
  const isAr = lang === 'ar';
  const [text, setText] = useState('');
  const [scheduleType, setScheduleType] = useState<'presets' | 'custom'>('presets');
  const [selectedMinutes, setSelectedMinutes] = useState(10);
  const [customDateTime, setCustomDateTime] = useState('');

  if (!isOpen) return null;

  const chatScheduled = scheduledMessages.filter((m) => m.chatId === chat.id);

  const PRESETS = [
    { label: isAr ? 'بعد 10 دقائق' : 'In 10 minutes', minutes: 10 },
    { label: isAr ? 'بعد 30 دقيقة' : 'In 30 minutes', minutes: 30 },
    { label: isAr ? 'بعد ساعة واحدة' : 'In 1 hour', minutes: 60 },
    { label: isAr ? 'بعد 3 ساعات' : 'In 3 hours', minutes: 180 },
    { label: isAr ? 'غداً الساعة 9:00 صباحاً' : 'Tomorrow at 9:00 AM', minutes: 1440 },
  ];

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!text.trim()) return;

    let targetTimestamp: number;
    if (scheduleType === 'presets') {
      targetTimestamp = Date.now() + selectedMinutes * 60 * 1000;
    } else {
      if (!customDateTime) return;
      targetTimestamp = new Date(customDateTime).getTime();
      if (isNaN(targetTimestamp) || targetTimestamp <= Date.now()) {
        alert(isAr ? 'يرجى اختيار وقت مستقبلي صحيح' : 'Please choose a future date/time');
        return;
      }
    }

    onScheduleMessage({
      text: text.trim(),
      scheduledTime: targetTimestamp,
      topicId: activeTopicId || undefined,
    });

    setText('');
    setScheduleType('presets');
  };

  const formatScheduledTime = (timestamp: number) => {
    const d = new Date(timestamp);
    return d.toLocaleString(isAr ? 'ar-EG' : 'en-US', {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-150">
      <div
        className={`w-full max-w-lg rounded-2xl shadow-2xl border flex flex-col max-h-[85vh] overflow-hidden ${
          isDark ? 'bg-[#17212b] border-gray-700 text-white' : 'bg-white border-gray-200 text-gray-900'
        }`}
      >
        {/* Header */}
        <div
          className={`flex items-center justify-between px-4 py-3 border-b shrink-0 ${
            isDark ? 'bg-[#242f3d] border-gray-700/60' : 'bg-gray-50 border-gray-200'
          }`}
        >
          <div className="flex items-center gap-2 font-bold text-sm">
            <Clock className="w-4 h-4 text-[#3390ec]" />
            <span>{isAr ? `الرسائل المجدولة (${chat.title})` : `Scheduled Messages (${chat.title})`}</span>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-full text-gray-400 hover:text-white transition"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Content Tabs / Form */}
        <div className="p-4 overflow-y-auto flex-1 space-y-4">
          {/* New Scheduled Message Form */}
          <form onSubmit={handleSubmit} className="space-y-3">
            <div>
              <label className="text-xs text-gray-400 block mb-1">
                {isAr ? 'نص الرسالة المجدولة' : 'Message to schedule'}
              </label>
              <textarea
                rows={2}
                value={text}
                onChange={(e) => setText(e.target.value)}
                placeholder={isAr ? 'اكتب ما تريد جدولته...' : 'Type scheduled message...'}
                className={`w-full p-2.5 rounded-xl text-xs outline-none border resize-none ${
                  isDark
                    ? 'bg-[#242f3d] border-gray-700 text-white placeholder-gray-400'
                    : 'bg-gray-100 border-gray-200 text-gray-900'
                }`}
              />
            </div>

            {/* Timing selection */}
            <div>
              <div className="flex items-center justify-between text-xs text-gray-400 mb-1.5">
                <span>{isAr ? 'وقت الإرسال' : 'Send Timing'}</span>
                <div className="flex items-center gap-2 text-[11px]">
                  <button
                    type="button"
                    onClick={() => setScheduleType('presets')}
                    className={`font-semibold ${
                      scheduleType === 'presets' ? 'text-[#3390ec]' : 'hover:text-gray-200'
                    }`}
                  >
                    {isAr ? 'أوقات سريعة' : 'Presets'}
                  </button>
                  <span>•</span>
                  <button
                    type="button"
                    onClick={() => setScheduleType('custom')}
                    className={`font-semibold ${
                      scheduleType === 'custom' ? 'text-[#3390ec]' : 'hover:text-gray-200'
                    }`}
                  >
                    {isAr ? 'تحديد مخصص' : 'Custom'}
                  </button>
                </div>
              </div>

              {scheduleType === 'presets' ? (
                <div className="flex flex-wrap gap-1.5">
                  {PRESETS.map((p, idx) => (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => setSelectedMinutes(p.minutes)}
                      className={`px-3 py-1.5 rounded-xl text-xs font-medium border transition ${
                        selectedMinutes === p.minutes
                          ? 'bg-[#3390ec] text-white border-[#3390ec] shadow-xs'
                          : isDark
                          ? 'bg-[#242f3d] border-gray-700 text-gray-300 hover:bg-white/5'
                          : 'bg-gray-100 border-gray-200 text-gray-700 hover:bg-white'
                      }`}
                    >
                      {p.label}
                    </button>
                  ))}
                </div>
              ) : (
                <input
                  type="datetime-local"
                  value={customDateTime}
                  onChange={(e) => setCustomDateTime(e.target.value)}
                  className={`w-full p-2 rounded-xl text-xs outline-none border ${
                    isDark ? 'bg-[#242f3d] border-gray-700 text-white' : 'bg-gray-100 border-gray-200 text-gray-900'
                  }`}
                />
              )}
            </div>

            <button
              type="submit"
              disabled={!text.trim()}
              className="w-full py-2.5 rounded-xl bg-[#3390ec] hover:bg-[#2881da] disabled:opacity-50 text-white text-xs font-bold transition flex items-center justify-center gap-2 shadow"
            >
              <Clock className="w-3.5 h-3.5" />
              <span>{isAr ? 'جدولة الرسالة الآن' : 'Schedule Message'}</span>
            </button>
          </form>

          <hr className={isDark ? 'border-gray-800' : 'border-gray-200'} />

          {/* Pending Scheduled List */}
          <div>
            <h4 className="text-xs font-bold text-gray-400 mb-2 flex items-center justify-between">
              <span>{isAr ? 'الرسائل المجدولة قيد الانتظار' : 'Pending Scheduled Messages'}</span>
              <span className="px-2 py-0.5 rounded-full text-[10px] bg-[#3390ec]/20 text-[#3390ec]">
                {chatScheduled.length}
              </span>
            </h4>

            {chatScheduled.length === 0 ? (
              <div className="py-6 text-center text-xs text-gray-400">
                <Clock className="w-8 h-8 mx-auto mb-2 opacity-30 text-[#3390ec]" />
                <p>{isAr ? 'لا توجد رسائل مجدولة لهذه المحادثة حالياً.' : 'No scheduled messages for this chat.'}</p>
              </div>
            ) : (
              <div className="space-y-2">
                {chatScheduled.map((msg) => (
                  <div
                    key={msg.id}
                    className={`p-3 rounded-xl border flex items-start justify-between gap-3 ${
                      isDark ? 'bg-[#242f3d]/60 border-gray-700' : 'bg-gray-50 border-gray-200'
                    }`}
                  >
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2 mb-1 text-[11px] text-[#3390ec] font-semibold">
                        <Calendar className="w-3.5 h-3.5 shrink-0" />
                        <span>{formatScheduledTime(msg.scheduledTime)}</span>
                      </div>
                      <p className="text-xs break-words leading-relaxed whitespace-pre-wrap">{msg.text}</p>
                    </div>

                    <div className="flex items-center gap-1 shrink-0">
                      <button
                        type="button"
                        onClick={() => onSendNow(msg.id)}
                        className="p-1.5 rounded-lg bg-[#3390ec]/10 text-[#3390ec] hover:bg-[#3390ec]/20 transition"
                        title={isAr ? 'إرسال الآن فوراً' : 'Send now'}
                      >
                        <Send className="w-3.5 h-3.5" />
                      </button>
                      <button
                        type="button"
                        onClick={() => onDeleteScheduled(msg.id)}
                        className="p-1.5 rounded-lg bg-red-500/10 text-red-400 hover:bg-red-500/20 transition"
                        title={isAr ? 'إلغاء الجدولة' : 'Cancel & delete'}
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
