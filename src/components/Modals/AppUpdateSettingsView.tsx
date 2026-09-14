import React, { useState, useEffect } from 'react';
import {
  ArrowLeft,
  Download,
  RefreshCw,
  Sparkles,
  ShieldCheck,
  CheckCircle2,
  HardDrive,
  Activity,
  Layers,
  FileCode,
  Zap,
} from 'lucide-react';
import { BuildVars } from '../../core/messenger/BuildVars';
import { appUpdateController, UpdateState } from '../../core/messenger/AppUpdateController';
import { useTelegram } from '../../context/TelegramContext';
import { UpdateAppActivityModal } from './UpdateAppActivityModal';

interface AppUpdateSettingsViewProps {
  onBack: () => void;
}

export const AppUpdateSettingsView: React.FC<AppUpdateSettingsViewProps> = ({ onBack }) => {
  const { settings, showToast } = useTelegram();
  const isArabic = settings.language === 'ar';
  const [updateState, setUpdateState] = useState<UpdateState>(appUpdateController.state);
  const [isChecking, setIsChecking] = useState(false);
  const [showFullActivity, setShowFullActivity] = useState(false);
  const [autoCheckEnabled, setAutoCheckEnabled] = useState(true);

  useEffect(() => {
    const unsub = appUpdateController.subscribe((st) => {
      setUpdateState(st);
      setIsChecking(st === 'checking');
    });
    return unsub;
  }, []);

  const handleManualCheck = async () => {
    setIsChecking(true);
    showToast(isArabic ? 'جارٍ التحقق من التحديثات عبر MTProto RPC...' : 'Checking for updates via MTProto...', '🔍');
    const result = await appUpdateController.checkAppUpdate(true);
    setIsChecking(false);
    if (result) {
      setShowFullActivity(true);
    }
  };

  return (
    <div id="app_update_settings_view" className="flex flex-col h-full bg-[#17212b] text-[#f5f5f5]">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3.5 bg-[#242f3d]/70 border-b border-[#242f3d]">
        <button
          onClick={onBack}
          className="w-8 h-8 rounded-full hover:bg-white/10 flex items-center justify-center text-gray-300 hover:text-white transition-colors"
        >
          <ArrowLeft className="w-5 h-5 rtl:rotate-180" />
        </button>
        <div>
          <h2 className="text-base font-bold text-white">
            {isArabic ? 'تحديث التطبيق (App Updates)' : 'App Updates & OTA'}
          </h2>
          <span className="text-[11px] text-gray-400">
            TMessagesProj • TLRPC.TL_help_getAppUpdate
          </span>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {/* Version Hero Card */}
        <div className="bg-gradient-to-br from-[#1c2c3e] to-[#121c27] p-5 rounded-2xl border border-[#2b5278]/40 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3.5">
            <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-[#2481cc] to-[#5288c1] flex items-center justify-center shadow-lg border border-white/10 shrink-0">
              <Download className="w-7 h-7 text-white" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-lg font-bold text-white">Telegram v{BuildVars.BUILD_VERSION_STRING}</h3>
                <span className="px-2 py-0.5 text-[10px] font-bold bg-blue-500/20 text-blue-300 rounded-md border border-blue-500/30">
                  Build {BuildVars.BUILD_VERSION}
                </span>
              </div>
              <p className="text-xs text-gray-300 mt-0.5">
                {isArabic ? 'النسخة الحالية مثبتة ومحدثة' : 'Current version installed and verified'}
              </p>
              <p className="text-[11px] text-[#5288c1] font-mono mt-0.5">
                App ID: {BuildVars.APP_ID} • {BuildVars.APPLICATION_ID}
              </p>
            </div>
          </div>

          <button
            onClick={handleManualCheck}
            disabled={isChecking}
            className="px-4 py-2.5 rounded-xl text-xs font-bold text-white bg-gradient-to-r from-[#2481cc] to-[#1c68a6] hover:brightness-110 active:scale-95 transition-all shadow-md flex items-center gap-2 shrink-0 disabled:opacity-50"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isChecking ? 'animate-spin' : ''}`} />
            {isChecking
              ? (isArabic ? 'جارٍ الفحص...' : 'Checking...')
              : (isArabic ? 'فحص التحديثات' : 'Check Updates')}
          </button>
        </div>

        {/* Quick Trigger Cards */}
        <div className="bg-[#0e1621] rounded-xl border border-[#242f3d] p-4 space-y-3">
          <div className="text-xs font-bold text-[#5288c1] uppercase tracking-wider flex items-center gap-2">
            <Zap className="w-4 h-4 text-amber-400" />
            {isArabic ? 'محاكاة وإجراءات التحديث' : 'Update Actions & Testing'}
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
            <button
              onClick={() => {
                appUpdateController.triggerTestUpdate('11.5.0');
                setShowFullActivity(true);
              }}
              className="p-3 bg-[#17212b] hover:bg-[#1e2a37] border border-[#242f3d] rounded-xl text-left rtl:text-right flex items-center justify-between transition-colors group"
            >
              <div>
                <div className="text-xs font-semibold text-white group-hover:text-blue-300">
                  {isArabic ? 'فتح شاشة التحديث (v11.5.0)' : 'Open Update Screen (v11.5.0)'}
                </div>
                <div className="text-[11px] text-gray-400">
                  {isArabic ? 'معاينة شريط التقدم وسجل التغييرات' : 'Linear progress bar & changelog'}
                </div>
              </div>
              <Sparkles className="w-4 h-4 text-amber-400 shrink-0" />
            </button>

            <button
              onClick={() => {
                appUpdateController.triggerTestUpdate('11.6.0');
                setShowFullActivity(true);
              }}
              className="p-3 bg-[#17212b] hover:bg-[#1e2a37] border border-[#242f3d] rounded-xl text-left rtl:text-right flex items-center justify-between transition-colors group"
            >
              <div>
                <div className="text-xs font-semibold text-white group-hover:text-purple-300">
                  {isArabic ? 'محاكاة ترقية رئيسية (v11.6.0)' : 'Simulate Major Release (v11.6.0)'}
                </div>
                <div className="text-[11px] text-gray-400">
                  {isArabic ? 'اختبار التثبيت السلس مع ثبات الجلسات' : 'Zero-logout OTA installation'}
                </div>
              </div>
              <Layers className="w-4 h-4 text-purple-400 shrink-0" />
            </button>
          </div>
        </div>

        {/* Technical RPC Specifications */}
        <div className="bg-[#0e1621] rounded-xl border border-[#242f3d] p-4 space-y-2.5 text-xs text-gray-300">
          <div className="text-xs font-bold text-[#5288c1] uppercase tracking-wider flex items-center gap-2">
            <FileCode className="w-4 h-4 text-emerald-400" />
            {isArabic ? 'معلومات بروتوكول التحديث (MTProto TL Schema)' : 'MTProto TL Schema Details'}
          </div>
          <div className="font-mono text-[11px] bg-[#17212b] p-2.5 rounded-lg border border-[#242f3d] text-emerald-400/90 leading-relaxed overflow-x-auto">
            {`// TL Constructor: 0x522d050f\nhelp.getAppUpdate#522d050f gcm_source:string = help.AppUpdate;\n\n// Available Responses:\nhelp.appUpdate#ccbb5c70 flags:# can_not_skip:flags.0?true id:int version:string text:string entities:Vector<MessageEntity> document:flags.1?Document url:flags.2?string = help.AppUpdate;\nhelp.appUpdateNotModified#c4b3f2e1 = help.AppUpdate;`}
          </div>
        </div>

        {/* Session Preservation Safety Note */}
        <div className="p-3.5 bg-emerald-950/20 border border-emerald-500/30 rounded-xl flex items-start gap-2.5 text-emerald-300 text-xs">
          <ShieldCheck className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
          <div>
            <span className="font-bold">{isArabic ? 'ثبات الجلسات التام:' : 'Zero Logout Guarantee:'}</span>{' '}
            {isArabic
              ? 'تتم معالجة التحديثات دون مسح الجلسات أو الرموز الأمنية، مع استمرار المحادثات وقواعد البيانات فوراً بعد الترقية.'
              : 'All sessions, chat databases, and encryption keys are preserved across updates.'}
          </div>
        </div>
      </div>

      {/* Embedded Activity Modal */}
      <UpdateAppActivityModal
        isOpen={showFullActivity}
        onClose={() => setShowFullActivity(false)}
      />
    </div>
  );
};
