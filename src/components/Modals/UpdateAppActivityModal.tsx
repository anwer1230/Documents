import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  Download,
  CheckCircle2,
  RefreshCw,
  Zap,
  ShieldCheck,
  ArrowLeft,
  X,
  FileCode,
  HardDrive,
  Activity,
  Layers,
  Sparkles,
  Server,
  Play,
  RotateCcw,
} from 'lucide-react';
import { appUpdateController, UpdateState } from '../../core/messenger/AppUpdateController';
import { BuildVars } from '../../core/messenger/BuildVars';
import { useTelegram } from '../../context/TelegramContext';
import confetti from 'canvas-confetti';

interface UpdateAppActivityModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const UpdateAppActivityModal: React.FC<UpdateAppActivityModalProps> = ({
  isOpen,
  onClose,
}) => {
  const { settings, showToast } = useTelegram();
  const isArabic = settings.language === 'ar';
  const [updateState, setUpdateState] = useState<UpdateState>(appUpdateController.state);
  const [progress, setProgress] = useState(appUpdateController.downloadProgress);
  const [downloadedBytes, setDownloadedBytes] = useState(appUpdateController.downloadedBytes);
  const [totalBytes, setTotalBytes] = useState(appUpdateController.totalBytes);
  const [speed, setSpeed] = useState(appUpdateController.downloadSpeed);
  const [simulatedVersion, setSimulatedVersion] = useState('11.5.0');

  useEffect(() => {
    const unsubscribe = appUpdateController.subscribe((state) => {
      setUpdateState(state);
      setProgress(appUpdateController.downloadProgress);
      setDownloadedBytes(appUpdateController.downloadedBytes);
      setTotalBytes(appUpdateController.totalBytes);
      setSpeed(appUpdateController.downloadSpeed);

      if (state === 'ready_to_install') {
        confetti({
          particleCount: 70,
          spread: 60,
          origin: { y: 0.6 },
        });
      }
    });

    const timer = setInterval(() => {
      setProgress(appUpdateController.downloadProgress);
      setDownloadedBytes(appUpdateController.downloadedBytes);
      setTotalBytes(appUpdateController.totalBytes);
      setSpeed(appUpdateController.downloadSpeed);
      setUpdateState(appUpdateController.state);
    }, 150);

    return () => {
      unsubscribe();
      clearInterval(timer);
    };
  }, []);

  if (!isOpen) return null;

  const info = appUpdateController.updateInfo;
  const currentVer = BuildVars.BUILD_VERSION_STRING;
  const targetVer = info?.version || '11.5.0';

  const formatMB = (bytes: number) => (bytes / (1024 * 1024)).toFixed(1);

  return (
    <AnimatePresence>
      <div
        id="update_app_activity_backdrop"
        className="fixed inset-0 z-[130] bg-black/75 backdrop-blur-md flex items-center justify-center p-2 sm:p-4"
        onClick={onClose}
      >
        <motion.div
          id="update_app_activity_window"
          initial={{ opacity: 0, scale: 0.94, y: 30 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.94, y: 30 }}
          transition={{ type: 'spring', damping: 25, stiffness: 300 }}
          className="bg-[#17212b] border border-[#242f3d] w-full max-w-xl rounded-2xl shadow-2xl overflow-hidden text-[#f5f5f5] flex flex-col max-h-[90vh]"
          onClick={(e) => e.stopPropagation()}
        >
          {/* Top Bar (ActionBar replica) */}
          <div className="flex items-center justify-between px-5 py-4 bg-[#242f3d]/60 border-b border-[#242f3d]">
            <div className="flex items-center gap-3">
              <button
                onClick={onClose}
                className="w-8 h-8 rounded-full hover:bg-white/10 flex items-center justify-center text-gray-300 hover:text-white transition-colors"
              >
                <ArrowLeft className="w-5 h-5 rtl:rotate-180" />
              </button>
              <div>
                <h2 className="text-base font-bold text-white flex items-center gap-2">
                  {isArabic ? 'تحديث التطبيق (OTA Direct Update)' : 'In-App Update (UpdateAppActivity)'}
                </h2>
                <span className="text-[11px] text-gray-400">
                  TMessagesProj • org.telegram.ui.UpdateAppActivity
                </span>
              </div>
            </div>
            <button
              onClick={onClose}
              className="w-8 h-8 rounded-full hover:bg-white/10 flex items-center justify-center text-gray-400 hover:text-white"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          {/* Scrollable Container */}
          <div className="flex-1 overflow-y-auto p-5 space-y-5">
            {/* Version Hero Badge */}
            <div className="bg-gradient-to-br from-[#1c2c3e] to-[#121c27] p-5 rounded-2xl border border-[#2b5278]/40 relative overflow-hidden flex flex-col sm:flex-row items-center justify-between gap-4">
              <div className="flex items-center gap-4">
                <div className="w-16 h-16 rounded-2xl bg-gradient-to-tr from-[#2481cc] via-[#2d6cb3] to-[#5288c1] flex items-center justify-center shadow-xl border border-white/15 shrink-0">
                  <Download className="w-8 h-8 text-white animate-bounce" />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="text-xl font-extrabold text-white">Telegram v{targetVer}</h3>
                    <span className="px-2 py-0.5 text-[10px] font-bold bg-amber-500/20 border border-amber-500/30 text-amber-300 rounded-md">
                      Build {info?.versionCode || 110500}
                    </span>
                  </div>
                  <p className="text-xs text-gray-300 mt-1">
                    {isArabic ? `الإصدار المثبت حالياً: v${currentVer}` : `Installed Version: v${currentVer}`}
                    {' • '}
                    <span className="text-[#5288c1] font-semibold">{info?.sizeFormatted || '61.3 MB'}</span>
                  </p>
                  <p className="text-[11px] text-gray-400 mt-0.5">
                    {isArabic ? 'توزيع مباشر وموقع رقمياً (Direct APK Distribution)' : 'Signed Standalone APK Distribution'}
                  </p>
                </div>
              </div>

              {/* Status Pill */}
              <div className="shrink-0">
                {updateState === 'downloading' && (
                  <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-blue-500/20 border border-blue-500/30 text-blue-400 text-xs font-semibold animate-pulse">
                    <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                    {isArabic ? `جارٍ التنزيل (${progress}%)` : `Downloading (${progress}%)`}
                  </span>
                )}
                {updateState === 'verifying' && (
                  <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-purple-500/20 border border-purple-500/30 text-purple-400 text-xs font-semibold animate-pulse">
                    <ShieldCheck className="w-3.5 h-3.5" />
                    {isArabic ? 'التحقق من التوقيع...' : 'Verifying SHA-256...'}
                  </span>
                )}
                {updateState === 'ready_to_install' && (
                  <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-emerald-500/20 border border-emerald-500/30 text-emerald-400 text-xs font-semibold">
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    {isArabic ? 'جاهز للتثبيت' : 'Ready to Install'}
                  </span>
                )}
                {updateState === 'up_to_date' && (
                  <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-gray-500/20 border border-gray-500/30 text-gray-300 text-xs font-semibold">
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                    {isArabic ? 'التطبيق محدث' : 'Up to Date'}
                  </span>
                )}
              </div>
            </div>

            {/* Linear Progress Bar Component */}
            <div className="bg-[#0e1621] p-4 rounded-xl border border-[#242f3d] space-y-3">
              <div className="flex items-center justify-between text-xs">
                <span className="font-semibold text-gray-200 flex items-center gap-1.5">
                  <Activity className="w-4 h-4 text-[#5288c1]" />
                  {updateState === 'ready_to_install'
                    ? (isArabic ? 'اكتمل تنزيل الحزمة بنجاح' : 'Package Download Complete')
                    : updateState === 'verifying'
                    ? (isArabic ? 'فحص سلامة الحزمة والتوقيع الرقمي' : 'Verifying cryptographic signature')
                    : updateState === 'downloading'
                    ? (isArabic ? `جارٍ التنزيل: ${speed}` : `Downloading at ${speed}`)
                    : (isArabic ? 'جاهز لبدء التنزيل المباشر' : 'Ready to download')}
                </span>
                <span className="font-mono text-[#5288c1] font-bold text-sm">
                  {progress}%
                </span>
              </div>

              {/* Real Linear Track */}
              <div className="w-full h-3 bg-[#17212b] rounded-full overflow-hidden p-0.5 border border-[#242f3d]">
                <motion.div
                  className="h-full bg-gradient-to-r from-[#2481cc] via-[#5288c1] to-[#60a5fa] rounded-full relative overflow-hidden"
                  initial={{ width: 0 }}
                  animate={{ width: `${progress}%` }}
                  transition={{ ease: 'easeOut', duration: 0.2 }}
                >
                  <div className="absolute inset-0 bg-white/20 animate-[shimmer_1.5s_infinite] -skew-x-12" />
                </motion.div>
              </div>

              {/* Bandwidth & File Metrics */}
              <div className="flex items-center justify-between text-[11px] text-gray-400 font-mono">
                <span>
                  {formatMB(downloadedBytes)} MB / {formatMB(totalBytes)} MB
                </span>
                <span>
                  {updateState === 'downloading' ? `السرعة: ${speed}` : `الملف: Telegram_v${targetVer}.apk`}
                </span>
              </div>
            </div>

            {/* Release Changelog Section */}
            <div className="bg-[#0e1621] p-4 rounded-xl border border-[#242f3d] space-y-2">
              <div className="flex items-center gap-2 text-xs font-bold text-[#5288c1] uppercase tracking-wider">
                <Sparkles className="w-4 h-4 text-amber-400" />
                {isArabic ? 'سجل التغييرات ومميزات الإصدار' : 'Release Notes & Highlights'}
              </div>
              <div className="text-xs text-gray-300 whitespace-pre-line leading-relaxed bg-[#17212b]/60 p-3 rounded-lg border border-[#242f3d]/60 font-sans">
                {isArabic
                  ? (info?.changelogAr || `✨ تحديث تيليجرام ${targetVer} الجديد:\n• مزادات هدايا النجوم الحصرية ومزايدات حية\n• منتقي نبرات الرسائل وصياغتها بالذكاء الاصطناعي\n• تعزيز القنوات وفتح مزايا القصص الحصرية\n• تحسينات شاملة وسرعة فائقة في معالجة الوسائط والمزامنة`)
                  : (info?.changelogEn || `✨ Telegram ${targetVer} Highlights:\n• Exclusive Star Gift Live Auctions & Bidding\n• AI Message Tones & Custom Styler\n• Channel Boosts & Exclusive Story Perks\n• Blazing fast caching and synchronization`)}
              </div>
            </div>

            {/* Session Persistence Guarantee Badge */}
            <div className="flex items-start gap-3 p-3.5 bg-emerald-950/20 border border-emerald-500/30 rounded-xl text-emerald-300 text-xs leading-relaxed">
              <ShieldCheck className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
              <div>
                <span className="font-bold">{isArabic ? 'ضمان ثبات الجلسة:' : 'Session Persistence Guaranteed:'}</span>{' '}
                {isArabic
                  ? 'يتم تطبيق التحديث مباشرة فوق ملفات النظام دون مسح البيانات أو قواعد البيانات المحلية (SQLite / IndexedDB)، ولن يطلب منك تسجيل الدخول مجدداً.'
                  : 'Update applies cleanly over the existing container and storage. No sessions or messages will be lost or logged out.'}
              </div>
            </div>

            {/* Simulator Controls for Developer & Testing */}
            <div className="p-3 bg-[#131b24] border border-[#242f3d] rounded-xl space-y-2">
              <div className="text-[11px] font-bold text-gray-400 uppercase tracking-wider flex items-center justify-between">
                <span>{isArabic ? 'أدوات اختبار الاستدعاءات (RPC & UI Testing)' : 'Testing & RPC Triggers'}</span>
                <span className="text-[10px] text-gray-500">TLRPC.TL_help_getAppUpdate</span>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <button
                  onClick={() => appUpdateController.checkAppUpdate(true)}
                  className="px-2.5 py-1.5 bg-[#242f3d] hover:bg-[#2b394a] text-white text-[11px] rounded-lg flex items-center gap-1 transition-colors"
                >
                  <RefreshCw className="w-3 h-3" />
                  {isArabic ? 'طلب help.getAppUpdate من السيرفر' : 'Call Server RPC'}
                </button>
                <button
                  onClick={() => appUpdateController.triggerTestUpdate('11.5.0')}
                  className="px-2.5 py-1.5 bg-blue-600/30 hover:bg-blue-600/50 text-blue-300 border border-blue-500/30 text-[11px] rounded-lg flex items-center gap-1 transition-colors"
                >
                  <Sparkles className="w-3 h-3" />
                  {isArabic ? 'محاكاة إصدار v11.5.0' : 'Simulate v11.5.0'}
                </button>
                <button
                  onClick={() => appUpdateController.triggerTestUpdate('11.6.0')}
                  className="px-2.5 py-1.5 bg-purple-600/30 hover:bg-purple-600/50 text-purple-300 border border-purple-500/30 text-[11px] rounded-lg flex items-center gap-1 transition-colors"
                >
                  <Sparkles className="w-3 h-3" />
                  {isArabic ? 'محاكاة إصدار v11.6.0' : 'Simulate v11.6.0'}
                </button>
              </div>
            </div>
          </div>

          {/* Action Footer Bar */}
          <div className="px-5 py-4 bg-[#131b24] border-t border-[#242f3d] flex items-center justify-between gap-3">
            <div className="text-xs text-gray-400">
              {updateState === 'downloading'
                ? (isArabic ? 'يتم التنزيل في الخلفية...' : 'Downloading in background...')
                : updateState === 'ready_to_install'
                ? (isArabic ? 'جاهز للتثبيت الفوري' : 'Ready to apply')
                : (isArabic ? 'انقر على الزر للبدء' : 'Click to start download')}
            </div>

            <div className="flex items-center gap-2.5">
              <button
                onClick={onClose}
                className="px-4 py-2 rounded-xl text-xs font-medium text-gray-400 hover:text-white hover:bg-white/5 transition-colors"
              >
                {isArabic ? 'إغلاق' : 'Close'}
              </button>

              {updateState === 'ready_to_install' ? (
                <button
                  id="install_update_now_btn"
                  onClick={() => {
                    showToast(isArabic ? 'جارٍ تثبيت التحديث وتحديث الواجهة مع الحفاظ على الجلسات...' : 'Applying update and refreshing...', '🚀');
                    setTimeout(() => {
                      appUpdateController.installNow();
                    }, 800);
                  }}
                  className="px-5 py-2.5 rounded-xl text-xs font-bold text-white bg-gradient-to-r from-emerald-600 to-teal-600 hover:brightness-110 active:scale-95 transition-all shadow-lg flex items-center gap-2 animate-bounce"
                >
                  <Zap className="w-4 h-4" />
                  {isArabic ? 'تثبيت التحديث وتحديث التطبيق الآن' : 'Install & Apply Now'}
                </button>
              ) : updateState === 'downloading' ? (
                <button
                  disabled
                  className="px-5 py-2.5 rounded-xl text-xs font-bold text-white bg-[#2481cc]/50 cursor-not-allowed flex items-center gap-2"
                >
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  {isArabic ? `جارٍ التنزيل (${progress}%)` : `Downloading (${progress}%)`}
                </button>
              ) : (
                <button
                  id="start_download_btn"
                  onClick={() => {
                    appUpdateController.startDownload();
                  }}
                  className="px-5 py-2.5 rounded-xl text-xs font-bold text-white bg-gradient-to-r from-[#2481cc] to-[#1c68a6] hover:brightness-110 active:scale-95 transition-all shadow-md flex items-center gap-2"
                >
                  <Download className="w-4 h-4" />
                  {isArabic ? 'بدء تنزيل التحديث' : 'Start Download'}
                </button>
              )}
            </div>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
};
