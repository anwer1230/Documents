import React, { useState } from "react";
import { X, Ghost, Check, EyeOff, Radio, Shield, Sparkles } from "lucide-react";
import { GhostModeSettings } from "../../types";
import { saveGhostModeSettings } from "../../lib/telegramProTools";

interface GhostModeModalProps {
  isOpen: boolean;
  onClose: () => void;
  settings: GhostModeSettings;
  onSave: (newSettings: GhostModeSettings) => void;
}

export const GhostModeModal: React.FC<GhostModeModalProps> = ({
  isOpen,
  onClose,
  settings,
  onSave,
}) => {
  const [localSettings, setLocalSettings] = useState<GhostModeSettings>(settings);

  if (!isOpen) return null;

  const handleToggleMaster = () => {
    const next = { ...localSettings, enabled: !localSettings.enabled };
    setLocalSettings(next);
    saveGhostModeSettings(next);
    onSave(next);
  };

  const handleToggleProp = (key: keyof GhostModeSettings) => {
    const next = { ...localSettings, [key]: !localSettings[key] };
    setLocalSettings(next);
    saveGhostModeSettings(next);
    onSave(next);
  };

  return (
    <div
      id="ghost-mode-modal-overlay"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fade-in"
      dir="rtl"
    >
      <div
        id="ghost-mode-modal"
        className="w-full max-w-md bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-200 dark:border-slate-800 overflow-hidden flex flex-col p-6 select-none"
      >
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800">
          <div className="flex items-center gap-2.5 text-indigo-500 font-bold text-base">
            <div className="w-8 h-8 rounded-full bg-indigo-100 dark:bg-indigo-950 flex items-center justify-center">
              <Ghost className="w-5 h-5 text-indigo-500" />
            </div>
            <div>
              <h3 className="text-slate-800 dark:text-white">وضع الشبح والتخفي الاحترافي (Ghost Mode)</h3>
              <p className="text-[11px] text-slate-400 font-normal">تصفح وقراءة سرية بدون أي أثر</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Master Switch */}
        <div className="mt-5 p-4 rounded-2xl bg-gradient-to-r from-indigo-500/10 via-purple-500/10 to-pink-500/10 border border-indigo-200 dark:border-indigo-900/50 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-indigo-500 text-white flex items-center justify-center shadow-md shadow-indigo-500/30">
              <Ghost className="w-6 h-6" />
            </div>
            <div>
              <h4 className="text-xs font-bold text-slate-800 dark:text-white">تفعيل وضع الشبح العام</h4>
              <p className="text-[11px] text-slate-500 dark:text-slate-400">
                {localSettings.enabled ? "وضع التخفي مفعل ونشط الآن" : "وضع التخفي معطل"}
              </p>
            </div>
          </div>
          <button
            id="ghost-master-toggle-btn"
            onClick={handleToggleMaster}
            className={`w-12 h-6 rounded-full transition-colors relative flex items-center p-0.5 ${
              localSettings.enabled ? "bg-indigo-600" : "bg-slate-300 dark:bg-slate-700"
            }`}
          >
            <span
              className={`w-5 h-5 rounded-full bg-white shadow-md transform transition-transform ${
                localSettings.enabled ? "-translate-x-6" : "translate-x-0"
              }`}
            />
          </button>
        </div>

        {/* Sub Toggles */}
        <div className="mt-4 space-y-3">
          {/* Hide Read Receipts */}
          <div className="p-3 bg-slate-50 dark:bg-slate-800/50 rounded-2xl flex items-center justify-between border border-slate-100 dark:border-slate-800">
            <div className="flex items-center gap-3">
              <EyeOff className="w-4 h-4 text-sky-500 shrink-0" />
              <div>
                <span className="text-xs font-semibold text-slate-800 dark:text-slate-200 block">
                  إخفاء علامة القراءة (Seen / Read Receipt)
                </span>
                <span className="text-[10px] text-slate-400">
                  قراءة الرسائل الواردة دون إرسال إشعار للطرف الآخر.
                </span>
              </div>
            </div>
            <button
              onClick={() => handleToggleProp("hideRead")}
              className={`w-9 h-5 rounded-full transition-colors relative flex items-center p-0.5 shrink-0 ${
                localSettings.hideRead ? "bg-sky-500" : "bg-slate-300 dark:bg-slate-700"
              }`}
            >
              <span
                className={`w-4 h-4 rounded-full bg-white shadow-sm transform transition-transform ${
                  localSettings.hideRead ? "-translate-x-4" : "translate-x-0"
                }`}
              />
            </button>
          </div>

          {/* Hide Typing Status */}
          <div className="p-3 bg-slate-50 dark:bg-slate-800/50 rounded-2xl flex items-center justify-between border border-slate-100 dark:border-slate-800">
            <div className="flex items-center gap-3">
              <Radio className="w-4 h-4 text-emerald-500 shrink-0" />
              <div>
                <span className="text-xs font-semibold text-slate-800 dark:text-slate-200 block">
                  إخفاء جاري الكتابة / تسجيل الصوت
                </span>
                <span className="text-[10px] text-slate-400">
                  منع إظهار "Typing..." أو "Recording voice..." للآخرين.
                </span>
              </div>
            </div>
            <button
              onClick={() => handleToggleProp("hideTyping")}
              className={`w-9 h-5 rounded-full transition-colors relative flex items-center p-0.5 shrink-0 ${
                localSettings.hideTyping ? "bg-emerald-500" : "bg-slate-300 dark:bg-slate-700"
              }`}
            >
              <span
                className={`w-4 h-4 rounded-full bg-white shadow-sm transform transition-transform ${
                  localSettings.hideTyping ? "-translate-x-4" : "translate-x-0"
                }`}
              />
            </button>
          </div>

          {/* Hide Online Presence */}
          <div className="p-3 bg-slate-50 dark:bg-slate-800/50 rounded-2xl flex items-center justify-between border border-slate-100 dark:border-slate-800">
            <div className="flex items-center gap-3">
              <Shield className="w-4 h-4 text-amber-500 shrink-0" />
              <div>
                <span className="text-xs font-semibold text-slate-800 dark:text-slate-200 block">
                  إخفاء حالة الاتصال (Offline Presence)
                </span>
                <span className="text-[10px] text-slate-400">
                  الظهور كـ "غير متصل" أو "شوهد قريباً" دائماً.
                </span>
              </div>
            </div>
            <button
              onClick={() => handleToggleProp("hideOnline")}
              className={`w-9 h-5 rounded-full transition-colors relative flex items-center p-0.5 shrink-0 ${
                localSettings.hideOnline ? "bg-amber-500" : "bg-slate-300 dark:bg-slate-700"
              }`}
            >
              <span
                className={`w-4 h-4 rounded-full bg-white shadow-sm transform transition-transform ${
                  localSettings.hideOnline ? "-translate-x-4" : "translate-x-0"
                }`}
              />
            </button>
          </div>
        </div>

        {/* Footer info */}
        <div className="mt-5 pt-3 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
          <div className="flex items-center gap-1.5 text-[11px] text-indigo-500 font-medium">
            <Sparkles className="w-3.5 h-3.5" />
            <span>يتم تطبيق إعدادات الحماية فورياً</span>
          </div>
          <button
            onClick={onClose}
            className="px-4 py-2 bg-indigo-500 hover:bg-indigo-600 text-white font-bold text-xs rounded-xl shadow-sm transition-all"
          >
            حفظ وإغلاق
          </button>
        </div>
      </div>
    </div>
  );
};
