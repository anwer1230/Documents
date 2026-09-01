import React from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { Sparkles, Download, Clock, CheckCircle2, ShieldCheck, X } from 'lucide-react';
import { appUpdateController, AppUpdateInfo } from '../../core/messenger/AppUpdateController';
import { BuildVars } from '../../core/messenger/BuildVars';
import { useTelegram } from '../../context/TelegramContext';

interface AppUpdateAlertDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onOpenFullActivity: () => void;
  updateInfo?: AppUpdateInfo | null;
}

export const AppUpdateAlertDialog: React.FC<AppUpdateAlertDialogProps> = ({
  isOpen,
  onClose,
  onOpenFullActivity,
  updateInfo,
}) => {
  const { settings } = useTelegram();
  const isArabic = settings.language === 'ar';
  const info = updateInfo || appUpdateController.updateInfo;

  if (!isOpen || !info) return null;

  return (
    <AnimatePresence>
      <div
        id="app_update_alert_dialog_backdrop"
        className="fixed inset-0 z-[120] bg-black/60 backdrop-blur-sm flex items-center justify-center p-4"
        onClick={onClose}
      >
        <motion.div
          id="app_update_alert_dialog_container"
          initial={{ opacity: 0, scale: 0.92, y: 20 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.92, y: 20 }}
          transition={{ type: 'spring', damping: 25, stiffness: 300 }}
          className="bg-[#17212b] border border-[#242f3d] w-full max-w-md rounded-2xl shadow-2xl overflow-hidden text-[#f5f5f5]"
          onClick={(e) => e.stopPropagation()}
        >
          {/* Header Banner */}
          <div className="bg-gradient-to-r from-[#2b5278] to-[#1e3b56] p-5 relative overflow-hidden">
            <div className="absolute top-0 right-0 -mr-8 -mt-8 w-32 h-32 bg-white/5 rounded-full blur-2xl pointer-events-none" />
            
            <button
              onClick={onClose}
              className="absolute top-3.5 right-3.5 w-8 h-8 rounded-full bg-black/20 hover:bg-black/40 text-gray-300 hover:text-white flex items-center justify-center transition-colors"
              title={isArabic ? 'إغلاق' : 'Close'}
            >
              <X className="w-4 h-4" />
            </button>

            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-xl bg-gradient-to-tr from-[#5288c1] to-[#3a6d99] flex items-center justify-center shadow-lg border border-white/10 shrink-0">
                <Sparkles className="w-6 h-6 text-white animate-pulse" />
              </div>
              <div>
                <h3 className="text-lg font-bold text-white tracking-wide flex items-center gap-2">
                  {isArabic ? 'تحديث تيليجرام متاح' : 'Telegram Update Available'}
                  <span className="px-2 py-0.5 text-[11px] font-semibold bg-[#2481cc] text-white rounded-full">
                    v{info.version}
                  </span>
                </h3>
                <p className="text-xs text-blue-200 mt-0.5">
                  {isArabic ? `الإصدار الحالي: v${BuildVars.BUILD_VERSION_STRING}` : `Current: v${BuildVars.BUILD_VERSION_STRING}`}
                  {' • '}
                  <span>{info.sizeFormatted || '61.3 MB'}</span>
                </p>
              </div>
            </div>
          </div>

          {/* Body Content / Changelog */}
          <div className="p-5 space-y-4">
            <div className="bg-[#0e1621] rounded-xl p-3.5 border border-[#242f3d]/60 space-y-2">
              <div className="text-xs font-semibold text-[#5288c1] uppercase tracking-wider flex items-center gap-1.5">
                <CheckCircle2 className="w-3.5 h-3.5 text-blue-400" />
                {isArabic ? 'ما الجديد في هذا الإصدار؟' : "What's New in this Version"}
              </div>
              <div className="text-xs text-gray-300 leading-relaxed whitespace-pre-line max-h-48 overflow-y-auto pr-1">
                {isArabic ? info.changelogAr : info.changelogEn}
              </div>
            </div>

            {/* Session Preservation Assurance */}
            <div className="flex items-center gap-2.5 px-3 py-2 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-emerald-400 text-xs">
              <ShieldCheck className="w-4 h-4 shrink-0" />
              <span>
                {isArabic
                  ? 'يتم التحديث بنظام OTA السلس مع الحفاظ التام على جلساتك ورسائلك دون خروج.'
                  : 'Seamless in-app update. Your active sessions & local databases remain intact.'}
              </span>
            </div>
          </div>

          {/* Dialog Action Buttons */}
          <div className="p-4 bg-[#131b24] border-t border-[#242f3d] flex items-center justify-end gap-2.5">
            <button
              id="app_update_postpone_btn"
              onClick={() => {
                appUpdateController.dismissCurrentUpdate();
                onClose();
              }}
              className="px-4 py-2.5 rounded-xl text-xs font-medium text-gray-400 hover:text-white hover:bg-white/5 transition-colors"
            >
              {isArabic ? 'لاحقاً' : 'Later'}
            </button>

            <button
              id="app_update_now_btn"
              onClick={() => {
                onClose();
                onOpenFullActivity();
                appUpdateController.startDownload();
              }}
              className="px-5 py-2.5 rounded-xl text-xs font-bold text-white bg-gradient-to-r from-[#2481cc] to-[#1c68a6] hover:brightness-110 active:scale-95 transition-all shadow-md flex items-center gap-1.5"
            >
              <Download className="w-4 h-4" />
              {isArabic ? 'تحديث الآن' : 'Update Now'}
            </button>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
};
