import React, { useState } from 'react';
import { useSettingsStore } from '../../stores/settingsStore';
import { RotateCcw, AlertTriangle, Check } from 'lucide-react';

export const ResetSettings: React.FC = () => {
  const { resetToDefault } = useSettingsStore();
  const [showConfirm, setShowConfirm] = useState(false);
  const [isDone, setIsDone] = useState(false);

  const handleReset = async () => {
    await resetToDefault();
    setIsDone(true);
    setShowConfirm(false);
    setTimeout(() => {
      setIsDone(false);
    }, 2500);
  };

  return (
    <div className="pt-2">
      {!showConfirm ? (
        <button
          type="button"
          onClick={() => setShowConfirm(true)}
          className="w-full flex items-center justify-center gap-2 py-3 px-4 rounded-xl border border-rose-500/30 bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 font-semibold text-sm transition-all cursor-pointer"
        >
          <RotateCcw className="w-4 h-4" />
          <span>استعادة بيانات العرض التجريبي الأصلية</span>
        </button>
      ) : (
        <div className="p-3.5 bg-rose-500/15 border border-rose-500/30 rounded-xl space-y-3">
          <div className="flex items-center gap-2.5 text-rose-300 text-xs font-medium">
            <AlertTriangle className="w-4 h-4 shrink-0 text-rose-400" />
            <span>هل أنت متأكد من استعادة جميع إعدادات تليجرام إلى الوضع الافتراضي؟</span>
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={handleReset}
              className="flex-1 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-lg text-xs font-bold transition-colors cursor-pointer"
            >
              نعم، استعادة الآن
            </button>
            <button
              type="button"
              onClick={() => setShowConfirm(false)}
              className="px-4 py-2 bg-white/10 hover:bg-white/15 text-gray-300 rounded-lg text-xs font-medium transition-colors cursor-pointer"
            >
              إلغاء
            </button>
          </div>
        </div>
      )}

      {isDone && (
        <div className="mt-2 text-center text-xs text-emerald-400 font-medium flex items-center justify-center gap-1">
          <Check className="w-3.5 h-3.5" />
          <span>تمت استعادة الإعدادات الافتراضية بنجاح!</span>
        </div>
      )}
    </div>
  );
};
