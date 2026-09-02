import React from "react";
import { X, BarChart3, Users, Eye, Share2, ShieldCheck, CheckCircle2, TrendingUp } from "lucide-react";
import { TelegramDialog } from "../../types";

interface ChannelAnalyticsModalProps {
  isOpen: boolean;
  onClose: () => void;
  dialog: TelegramDialog | null;
}

export const ChannelAnalyticsModal: React.FC<ChannelAnalyticsModalProps> = ({
  isOpen,
  onClose,
  dialog,
}) => {
  if (!isOpen || !dialog) return null;

  const sampleMetrics = {
    membersCount: dialog.memberCount || 12450,
    dailyGrowth: "+148 عضو اليوم",
    viewsPerPost: "8.4K مشاهدة",
    forwardsRatio: "14.2%",
    engagementRate: "68.5%",
    topPostingHour: "07:00 PM - 09:00 PM",
  };

  const sampleAdmins = [
    { name: "المالك (Owner)", role: "المنشئ والمالك الرئيسي", rights: "كامل الصلاحيات" },
    { name: "مشرف النشر والمحتوى", role: "مشرف محتوى", rights: "نشر، تعديل، وحذف الرسائل" },
    { name: "بوت الإحصائيات الآلي", role: "بوت إداري", rights: "قراءة الإحصائيات ودعوة الأعضاء" },
  ];

  return (
    <div
      id="channel-analytics-modal-overlay"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fade-in"
      dir="rtl"
    >
      <div
        id="channel-analytics-modal"
        className="w-full max-w-lg bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-200 dark:border-slate-800 overflow-hidden flex flex-col p-6 select-none max-h-[90vh]"
      >
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800">
          <div className="flex items-center gap-2.5 text-sky-500 font-bold text-base">
            <BarChart3 className="w-5 h-5" />
            <div>
              <h3 className="text-slate-800 dark:text-white">إحصائيات وتحليل القناة (Analytics)</h3>
              <p className="text-[11px] text-slate-400 font-normal">{dialog.title || dialog.name}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Metrics Grid */}
        <div className="mt-4 grid grid-cols-3 gap-2.5">
          <div className="p-3 rounded-2xl bg-sky-50 dark:bg-sky-950/40 border border-sky-100 dark:border-sky-900/40">
            <div className="flex items-center gap-1.5 text-sky-600 dark:text-sky-400 text-xs font-bold mb-1">
              <Users className="w-4 h-4" />
              <span>المشتركون</span>
            </div>
            <div className="text-lg font-extrabold text-slate-800 dark:text-white font-mono">
              {sampleMetrics.membersCount.toLocaleString()}
            </div>
            <span className="text-[10px] text-emerald-500 font-semibold">{sampleMetrics.dailyGrowth}</span>
          </div>

          <div className="p-3 rounded-2xl bg-indigo-50 dark:bg-indigo-950/40 border border-indigo-100 dark:border-indigo-900/40">
            <div className="flex items-center gap-1.5 text-indigo-600 dark:text-indigo-400 text-xs font-bold mb-1">
              <Eye className="w-4 h-4" />
              <span>متوسط المشاهدات</span>
            </div>
            <div className="text-lg font-extrabold text-slate-800 dark:text-white font-mono">
              {sampleMetrics.viewsPerPost}
            </div>
            <span className="text-[10px] text-slate-400">لكل منشور</span>
          </div>

          <div className="p-3 rounded-2xl bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-100 dark:border-emerald-900/40">
            <div className="flex items-center gap-1.5 text-emerald-600 dark:text-emerald-400 text-xs font-bold mb-1">
              <TrendingUp className="w-4 h-4" />
              <span>معدل التفاعل</span>
            </div>
            <div className="text-lg font-extrabold text-slate-800 dark:text-white font-mono">
              {sampleMetrics.engagementRate}
            </div>
            <span className="text-[10px] text-emerald-600 font-semibold">تفاعل ممتاز</span>
          </div>
        </div>

        {/* Admins & Privileges */}
        <div className="mt-4">
          <h4 className="text-xs font-bold text-slate-700 dark:text-slate-300 mb-2 flex items-center gap-1.5">
            <ShieldCheck className="w-4 h-4 text-sky-500" />
            <span>المشرفون والصلاحيات الإدارية:</span>
          </h4>
          <div className="space-y-2 max-h-36 overflow-y-auto p-1">
            {sampleAdmins.map((adm, idx) => (
              <div
                key={idx}
                className="p-2.5 bg-slate-50 dark:bg-slate-800/40 rounded-xl flex items-center justify-between text-xs border border-slate-100 dark:border-slate-800"
              >
                <div>
                  <span className="font-bold text-slate-800 dark:text-white block">{adm.name}</span>
                  <span className="text-[10px] text-slate-400">{adm.role}</span>
                </div>
                <span className="text-[11px] font-semibold text-sky-600 dark:text-sky-400 bg-sky-50 dark:bg-sky-950 px-2 py-0.5 rounded-lg border border-sky-100 dark:border-sky-900">
                  {adm.rights}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Footer */}
        <div className="mt-4 pt-3 border-t border-slate-100 dark:border-slate-800 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 font-bold text-xs rounded-xl"
          >
            إغلاق
          </button>
        </div>
      </div>
    </div>
  );
};
