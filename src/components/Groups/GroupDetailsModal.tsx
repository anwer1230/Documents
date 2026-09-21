import React from 'react';
import { 
  X, 
  Users, 
  Activity, 
  Clock, 
  CheckCircle, 
  XCircle, 
  Send, 
  Image, 
  Smile, 
  Link2, 
  Radio
} from 'lucide-react';
import { TelegramGroup } from '../../types';

interface GroupDetailsModalProps {
  group: TelegramGroup | null;
  onClose: () => void;
  onPing: (id: string) => void;
  onToggleMonitoring: (id: string) => void;
}

export const GroupDetailsModal: React.FC<GroupDetailsModalProps> = ({
  group,
  onClose,
  onPing,
  onToggleMonitoring,
}) => {
  if (!group) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-fade-in">
      <div 
        id="group-details-modal-box"
        className="w-full max-w-lg bg-slate-900 border border-slate-800 rounded-3xl p-6 text-slate-100 shadow-2xl relative overflow-hidden"
      >
        {/* Header */}
        <div className="flex items-start justify-between pb-4 border-b border-slate-800">
          <div className="flex items-center gap-3">
            {group.avatarUrl ? (
              <img
                src={group.avatarUrl}
                alt={group.title}
                className="w-12 h-12 rounded-2xl object-cover border border-slate-700"
              />
            ) : (
              <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-indigo-600 to-purple-600 flex items-center justify-center text-white font-bold text-lg">
                {group.title.charAt(0)}
              </div>
            )}
            <div>
              <h3 className="text-base font-bold text-white line-clamp-1">{group.title}</h3>
              <p className="text-xs text-slate-400">المعرف: <span className="font-mono text-slate-300">{group.id}</span></p>
            </div>
          </div>

          <button
            id="close-group-details-btn"
            type="button"
            onClick={onClose}
            className="p-2 rounded-xl bg-slate-800/80 hover:bg-slate-800 text-slate-400 hover:text-white transition-colors cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Health & Latency Highlight */}
        <div className="my-5 p-4 rounded-2xl bg-slate-800/60 border border-slate-700/60 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
              <Activity className="w-5 h-5" />
            </div>
            <div>
              <div className="text-xs text-slate-400">زمن استجابة الشبكة (Latency)</div>
              <div className="text-base font-bold text-indigo-300">
                {group.latencyMs > 0 ? `${group.latencyMs} ms` : 'غير متصل'}
              </div>
            </div>
          </div>

          <button
            id="modal-test-ping-btn"
            type="button"
            onClick={() => onPing(group.id)}
            className="px-3.5 py-1.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold shadow-sm transition-all cursor-pointer"
          >
            إعادة فحص Ping
          </button>
        </div>

        {/* Status Breakdown */}
        <div className="space-y-4">
          <div>
            <h4 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
              تشخيص الحالة والصلاحيات الحالية
            </h4>
            <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800/80 text-xs">
              <div className="text-slate-300 font-medium">{group.statusReason || 'الحالة اعتيادية ومستقرة'}</div>
              {group.permissions.slowmodeDelaySeconds > 0 && (
                <div className="flex items-center gap-1.5 text-amber-400 mt-2 font-medium">
                  <Clock className="w-3.5 h-3.5" />
                  <span>مفعل وضع البطء الإجباري: {group.permissions.slowmodeDelaySeconds} ثانية بين كل رسالة</span>
                </div>
              )}
            </div>
          </div>

          {/* Granular Telegram Permissions Matrix */}
          <div>
            <h4 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
              صلاحيات النشر الفعلية في المجموعة
            </h4>
            <div className="grid grid-cols-2 gap-2">
              <div className="p-2.5 rounded-xl bg-slate-800/40 border border-slate-800 flex items-center justify-between text-xs">
                <span className="flex items-center gap-2 text-slate-300">
                  <Send className="w-3.5 h-3.5 text-slate-400" />
                  <span>إرسال نصوص</span>
                </span>
                {group.permissions.canSendMessages ? (
                  <CheckCircle className="w-4 h-4 text-emerald-400" />
                ) : (
                  <XCircle className="w-4 h-4 text-rose-400" />
                )}
              </div>

              <div className="p-2.5 rounded-xl bg-slate-800/40 border border-slate-800 flex items-center justify-between text-xs">
                <span className="flex items-center gap-2 text-slate-300">
                  <Image className="w-3.5 h-3.5 text-slate-400" />
                  <span>إرسال وسائط</span>
                </span>
                {group.permissions.canSendMedia ? (
                  <CheckCircle className="w-4 h-4 text-emerald-400" />
                ) : (
                  <XCircle className="w-4 h-4 text-rose-400" />
                )}
              </div>

              <div className="p-2.5 rounded-xl bg-slate-800/40 border border-slate-800 flex items-center justify-between text-xs">
                <span className="flex items-center gap-2 text-slate-300">
                  <Smile className="w-3.5 h-3.5 text-slate-400" />
                  <span>ملصقات ومتحركات</span>
                </span>
                {group.permissions.canSendStickers ? (
                  <CheckCircle className="w-4 h-4 text-emerald-400" />
                ) : (
                  <XCircle className="w-4 h-4 text-rose-400" />
                )}
              </div>

              <div className="p-2.5 rounded-xl bg-slate-800/40 border border-slate-800 flex items-center justify-between text-xs">
                <span className="flex items-center gap-2 text-slate-300">
                  <Link2 className="w-3.5 h-3.5 text-slate-400" />
                  <span>معاينة الروابط</span>
                </span>
                {group.permissions.canAddWebPagePreviews ? (
                  <CheckCircle className="w-4 h-4 text-emerald-400" />
                ) : (
                  <XCircle className="w-4 h-4 text-rose-400" />
                )}
              </div>
            </div>
          </div>

          {/* Monitoring status toggle */}
          <div className="pt-2">
            <button
              id="modal-toggle-monitoring-btn"
              type="button"
              onClick={() => onToggleMonitoring(group.id)}
              className={`w-full py-2.5 px-4 rounded-xl border flex items-center justify-center gap-2 text-xs font-bold transition-all cursor-pointer ${
                group.isMonitored
                  ? 'bg-purple-600/20 border-purple-500/40 text-purple-200 hover:bg-purple-600/30'
                  : 'bg-slate-800 border-slate-700 text-slate-300 hover:bg-slate-700 hover:text-white'
              }`}
            >
              <Radio className="w-4 h-4" />
              <span>{group.isMonitored ? 'إيقاف مراقبة الكلمات المفتاحية' : 'تفعيل رصد ومراقبة الكلمات لهذه المجموعة'}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
