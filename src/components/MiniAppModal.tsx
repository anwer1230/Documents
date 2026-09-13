import React, { useState, useEffect, useRef } from 'react';
import {
  X,
  RotateCcw,
  ExternalLink,
  ShieldCheck,
  Sparkles,
  ArrowLeft,
  ArrowRight,
  Loader2,
  Vibrate,
  Coins,
  Settings,
  AlertCircle,
} from 'lucide-react';

interface MiniAppModalProps {
  isOpen: boolean;
  onClose: () => void;
  appName?: string;
  botUsername?: string;
  appUrl?: string;
  isDark: boolean;
  lang: 'ar' | 'en';
  onSendData?: (data: string) => void;
  starsBalance?: number;
}

const FEATURED_MINI_APPS = [
  {
    id: 'stars_clicker',
    name: 'Telegram Stars Clicker ⭐️',
    type: 'internal_stars_clicker',
    icon: '⭐️',
    description: 'تطبيق مصغر لكسب نجوم تيليجرام وتجربة HapticFeedback و MainButton',
  },
  {
    id: 'bridge_playground',
    name: 'WebApp Bridge Playground 🧪',
    type: 'internal_playground',
    icon: '🤖',
    description: 'لوحة اختبار كاملة لجميع دوال window.Telegram.WebApp وجسر الجافاسكريبت',
  },
  {
    id: 'wallet',
    name: 'Telegram Wallet (@wallet)',
    type: 'url',
    url: 'https://wallet.tg',
    icon: '💳',
    description: 'المحفظة الرقمية الرسمية عبر تيليجرام',
  },
  {
    id: 'core_demo',
    name: 'Telegram Core WebApps',
    type: 'url',
    url: 'https://core.telegram.org/bots/webapps',
    icon: '⚡',
    description: 'مستندات وتطبيقات تيليجرام التجريبية الرسمية',
  },
];

export const MiniAppModal: React.FC<MiniAppModalProps> = ({
  isOpen,
  onClose,
  appName = 'Telegram Mini App',
  botUsername = 'telegram_bot',
  appUrl,
  isDark,
  lang,
  onSendData,
  starsBalance = 250,
}) => {
  const isAr = lang === 'ar';
  const [selectedApp, setSelectedApp] = useState(FEATURED_MINI_APPS[0]);
  const [customUrl, setCustomUrl] = useState('');
  const [currentUrl, setCurrentUrl] = useState(appUrl || '');
  const [iframeKey, setIframeKey] = useState(0);

  // Haptic feedback indicator state
  const [hapticFeedbackNotice, setHapticFeedbackNotice] = useState<string | null>(null);

  // Official Telegram WebApp MainButton state
  const [mainButton, setMainButton] = useState<{
    isVisible: boolean;
    text: string;
    color: string;
    textColor: string;
    isActive: boolean;
    isProgressVisible: boolean;
  }>({
    isVisible: true,
    text: 'Claim 50 Stars ⭐',
    color: '#3390ec',
    textColor: '#ffffff',
    isActive: true,
    isProgressVisible: false,
  });

  // Official Telegram WebApp BackButton state
  const [hasBackButton, setHasBackButton] = useState(false);

  // Internal Stars Clicker Demo State
  const [clickerStars, setClickerStars] = useState(starsBalance);
  const [clickerStreak, setClickerStreak] = useState(0);

  const iframeRef = useRef<HTMLIFrameElement>(null);

  // Trigger haptic feedback (hardware vibration + visual feedback toast)
  const triggerHaptic = (style: string) => {
    if (typeof navigator !== 'undefined' && navigator.vibrate) {
      if (style === 'heavy' || style === 'error') {
        navigator.vibrate([40, 30, 40]);
      } else if (style === 'medium' || style === 'warning') {
        navigator.vibrate(30);
      } else {
        navigator.vibrate(15);
      }
    }
    setHapticFeedbackNotice(style);
    setTimeout(() => setHapticFeedbackNotice(null), 1200);
  };

  // Sync appUrl when changed from parent
  useEffect(() => {
    if (appUrl) {
      setCurrentUrl(appUrl);
      setSelectedApp({
        id: 'external_custom',
        name: appName,
        type: 'url',
        url: appUrl,
        icon: '🚀',
        description: 'رابط خارجي لتطبيق مصغر',
      });
      setIframeKey((k) => k + 1);
    }
  }, [appUrl, appName]);

  // Telegram WebApp postMessage Bridge handler
  useEffect(() => {
    const handleMessage = (event: MessageEvent) => {
      try {
        let data = event.data;
        if (typeof data === 'string') {
          try {
            data = JSON.parse(data);
          } catch {
            return;
          }
        }
        if (!data || !data.eventType) return;

        switch (data.eventType) {
          case 'web_app_setup_main_button':
            if (data.eventData) {
              setMainButton((prev) => ({
                ...prev,
                isVisible: !!data.eventData.is_visible,
                text: data.eventData.text || prev.text,
                color: data.eventData.color || prev.color,
                textColor: data.eventData.text_color || prev.textColor,
                isActive: data.eventData.is_active !== false,
                isProgressVisible: !!data.eventData.is_progress_visible,
              }));
            }
            break;

          case 'web_app_setup_back_button':
            if (data.eventData) {
              setHasBackButton(!!data.eventData.is_visible);
            }
            break;

          case 'web_app_trigger_haptic_feedback':
            if (data.eventData) {
              const style =
                data.eventData.impact_style ||
                data.eventData.notification_type ||
                'medium';
              triggerHaptic(style);
            }
            break;

          case 'web_app_close':
            onClose();
            break;

          case 'web_app_open_link':
            if (data.eventData?.url) {
              window.open(data.eventData.url, '_blank', 'noopener,noreferrer');
            }
            break;

          case 'web_app_data_send':
            if (data.eventData?.data) {
              onSendData?.(data.eventData.data);
              onClose();
            }
            break;

          default:
            break;
        }
      } catch (err) {
        console.warn('MiniApp bridge event error:', err);
      }
    };

    window.addEventListener('message', handleMessage);
    return () => window.removeEventListener('message', handleMessage);
  }, [onClose, onSendData]);

  if (!isOpen) return null;

  const handleMainButtonClick = () => {
    if (!mainButton.isActive || mainButton.isProgressVisible) return;
    triggerHaptic('heavy');

    if (selectedApp.type === 'internal_stars_clicker') {
      setClickerStars((s) => s + 50);
      onSendData?.(JSON.stringify({ action: 'claimed_stars', amount: 50 }));
      alert(isAr ? 'تم استلام 50 نجمة تليجرام بنجاح! ⭐️' : 'Successfully claimed 50 Telegram Stars! ⭐️');
      return;
    }

    if (iframeRef.current && iframeRef.current.contentWindow) {
      iframeRef.current.contentWindow.postMessage(
        JSON.stringify({ eventType: 'main_button_pressed' }),
        '*'
      );
    }
  };

  const handleBackButtonClick = () => {
    triggerHaptic('light');
    if (iframeRef.current && iframeRef.current.contentWindow) {
      iframeRef.current.contentWindow.postMessage(
        JSON.stringify({ eventType: 'back_button_pressed' }),
        '*'
      );
    } else {
      setHasBackButton(false);
    }
  };

  const handleSelectPreset = (app: typeof FEATURED_MINI_APPS[0]) => {
    setSelectedApp(app);
    if (app.type === 'url' && app.url) {
      setCurrentUrl(app.url);
      setIframeKey((k) => k + 1);
    }
    if (app.id === 'stars_clicker') {
      setMainButton({
        isVisible: true,
        text: isAr ? 'استلام النجوم ⭐️' : 'Claim Stars ⭐️',
        color: '#f59e0b',
        textColor: '#000000',
        isActive: true,
        isProgressVisible: false,
      });
      setHasBackButton(true);
    } else if (app.id === 'bridge_playground') {
      setMainButton({
        isVisible: true,
        text: isAr ? 'زر WebApp الرئيسي (MainButton)' : 'Telegram MainButton',
        color: '#3390ec',
        textColor: '#ffffff',
        isActive: true,
        isProgressVisible: false,
      });
      setHasBackButton(true);
    }
  };

  const handleLoadCustomUrl = (e: React.FormEvent) => {
    e.preventDefault();
    if (!customUrl.trim()) return;
    let target = customUrl.trim();
    if (!target.startsWith('http://') && !target.startsWith('https://')) {
      target = 'https://' + target;
    }
    setCurrentUrl(target);
    setSelectedApp({
      id: 'custom_url',
      name: 'Custom Mini App',
      type: 'url',
      url: target,
      icon: '🌐',
      description: target,
    });
    setIframeKey((prev) => prev + 1);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-2 sm:p-4 bg-black/65 backdrop-blur-sm animate-in fade-in duration-200">
      <div
        className={`w-full max-w-2xl h-[92vh] max-h-[800px] rounded-3xl shadow-2xl flex flex-col overflow-hidden border relative ${
          isDark ? 'bg-[#17212b] border-gray-700/80 text-white' : 'bg-white border-gray-200 text-gray-900'
        }`}
      >
        {/* Haptic Visual Toast Notification */}
        {hapticFeedbackNotice && (
          <div className="absolute top-16 left-1/2 -translate-x-1/2 z-50 px-3 py-1 rounded-full text-xs font-bold bg-purple-600 text-white shadow-lg flex items-center gap-1.5 animate-in fade-in zoom-in-90 duration-150 pointer-events-none">
            <Vibrate className="w-3.5 h-3.5 animate-bounce" />
            <span>
              Telegram Haptic: {hapticFeedbackNotice}
            </span>
          </div>
        )}

        {/* Telegram WebApp Header */}
        <div
          className={`flex items-center justify-between px-4 py-3 border-b select-none shrink-0 ${
            isDark ? 'bg-[#242f3d] border-gray-700/60' : 'bg-gray-50 border-gray-200'
          }`}
        >
          <div className="flex items-center gap-2.5">
            {hasBackButton && (
              <button
                type="button"
                onClick={handleBackButtonClick}
                className="p-1.5 rounded-full hover:bg-white/10 text-[#3390ec] transition"
                title={isAr ? 'رجوع' : 'Back'}
              >
                {isAr ? <ArrowRight className="w-5 h-5" /> : <ArrowLeft className="w-5 h-5" />}
              </button>
            )}

            <div className="w-9 h-9 rounded-full bg-[#3390ec] flex items-center justify-center text-lg text-white font-bold shadow-sm">
              {selectedApp.icon}
            </div>
            <div>
              <div className="flex items-center gap-1.5 font-bold text-sm">
                <span>{selectedApp.name}</span>
                <ShieldCheck className="w-4 h-4 text-[#3390ec]" />
              </div>
              <p className="text-[11px] text-gray-400">
                @{botUsername} • {isAr ? 'بيئة JS Bridge الكاملة لتيليجرام' : 'Telegram WebApp JS Bridge v7.10'}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-1">
            <button
              type="button"
              onClick={() => setIframeKey((k) => k + 1)}
              className="p-2 rounded-full text-gray-400 hover:text-white hover:bg-white/10 transition"
              title={isAr ? 'إعادة تحميل' : 'Reload'}
            >
              <RotateCcw className="w-4 h-4" />
            </button>
            {selectedApp.type === 'url' && currentUrl && (
              <a
                href={currentUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="p-2 rounded-full text-gray-400 hover:text-white hover:bg-white/10 transition"
                title={isAr ? 'فتح في نافذة جديدة' : 'Open in new tab'}
              >
                <ExternalLink className="w-4 h-4" />
              </a>
            )}
            <button
              type="button"
              onClick={onClose}
              className="p-2 rounded-full text-gray-400 hover:text-red-400 hover:bg-red-500/10 transition ml-1"
              title={isAr ? 'إغلاق' : 'Close'}
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Featured Mini Apps Switcher */}
        <div
          className={`px-4 py-2 border-b flex items-center gap-2 overflow-x-auto text-xs shrink-0 ${
            isDark ? 'bg-[#1c2733] border-gray-800' : 'bg-gray-100 border-gray-200'
          }`}
        >
          <span className="text-gray-400 text-[11px] shrink-0 font-medium">
            {isAr ? 'التطبيقات المصغرة:' : 'Mini Apps:'}
          </span>
          {FEATURED_MINI_APPS.map((app) => (
            <button
              key={app.id}
              type="button"
              onClick={() => handleSelectPreset(app)}
              className={`px-3 py-1 rounded-full text-xs shrink-0 transition-all flex items-center gap-1.5 ${
                selectedApp.id === app.id
                  ? 'bg-[#3390ec] text-white font-bold shadow-sm'
                  : isDark
                  ? 'bg-white/5 text-gray-300 hover:bg-white/10'
                  : 'bg-white text-gray-700 hover:bg-gray-200'
              }`}
            >
              <span>{app.icon}</span>
              <span>{app.name.split(' ')[0]}</span>
            </button>
          ))}
        </div>

        {/* Mini App Body / Iframe / Interactive Component */}
        <div className="flex-1 w-full relative overflow-hidden bg-black/5 dark:bg-[#0e1621]">
          {selectedApp.id === 'stars_clicker' ? (
            /* Telegram Stars Clicker Mini App (Simulates real Telegram tap-to-earn game with Haptics) */
            <div className="h-full flex flex-col items-center justify-center p-6 text-center select-none bg-gradient-to-b from-amber-500/10 via-transparent to-black/20">
              <div className="mb-2">
                <span className="px-3 py-1 rounded-full text-xs font-bold bg-amber-400/20 text-amber-400 border border-amber-400/30">
                  {isAr ? 'رصيد النجوم' : 'Stars Balance'}: {clickerStars.toLocaleString()} ⭐️
                </span>
              </div>

              <button
                type="button"
                onClick={() => {
                  triggerHaptic('medium');
                  setClickerStars((s) => s + 1);
                  setClickerStreak((st) => st + 1);
                }}
                className="w-40 h-40 my-6 rounded-full bg-gradient-to-tr from-amber-400 to-yellow-300 text-6xl flex items-center justify-center shadow-[0_0_40px_rgba(251,191,36,0.5)] transform active:scale-90 transition-all cursor-pointer border-4 border-amber-200"
              >
                ⭐️
              </button>

              <h3 className="font-extrabold text-base mb-1">
                {isAr ? 'انقر لكسب النجوم واختبار Haptic Feedback' : 'Tap to Earn Stars & Test Haptics'}
              </h3>
              <p className="text-xs text-gray-400 max-w-sm">
                {isAr
                  ? 'كل نقرة تولد نبضة HapticFeedback حقيقية، ويمكنك الضغط على زر MainButton بالأسفل لتأكيد المكافأة.'
                  : 'Every tap triggers genuine HapticFeedback. Press MainButton below to claim rewards.'}
              </p>
            </div>
          ) : selectedApp.id === 'bridge_playground' ? (
            /* Interactive WebApp JS Bridge Playground */
            <div className="h-full p-4 overflow-y-auto space-y-4 text-xs">
              <div
                className={`p-3 rounded-2xl border ${
                  isDark ? 'bg-[#242f3d]/70 border-gray-700' : 'bg-white border-gray-200 shadow-sm'
                }`}
              >
                <h4 className="font-bold text-sm text-[#3390ec] mb-2 flex items-center gap-1.5">
                  <Vibrate className="w-4 h-4" />
                  <span>{isAr ? 'اختبار HapticFeedback' : 'HapticFeedback API'}</span>
                </h4>
                <div className="grid grid-cols-3 gap-2">
                  {(['light', 'medium', 'heavy', 'rigid', 'soft'] as const).map((style) => (
                    <button
                      key={style}
                      type="button"
                      onClick={() => triggerHaptic(style)}
                      className={`p-2 rounded-xl border text-center font-bold transition hover:border-[#3390ec] ${
                        isDark ? 'bg-[#17212b] border-gray-700' : 'bg-gray-50 border-gray-200'
                      }`}
                    >
                      {style}
                    </button>
                  ))}
                </div>
              </div>

              <div
                className={`p-3 rounded-2xl border ${
                  isDark ? 'bg-[#242f3d]/70 border-gray-700' : 'bg-white border-gray-200 shadow-sm'
                }`}
              >
                <h4 className="font-bold text-sm text-[#3390ec] mb-2 flex items-center gap-1.5">
                  <Settings className="w-4 h-4" />
                  <span>{isAr ? 'التحكم في أزرار WebApp' : 'WebApp Buttons Controls'}</span>
                </h4>
                <div className="flex flex-wrap gap-2">
                  <button
                    type="button"
                    onClick={() => setHasBackButton(!hasBackButton)}
                    className="px-3 py-1.5 rounded-xl bg-[#3390ec] text-white font-bold"
                  >
                    {hasBackButton ? 'إخفاء BackButton' : 'إظهار BackButton'}
                  </button>
                  <button
                    type="button"
                    onClick={() =>
                      setMainButton((prev) => ({
                        ...prev,
                        isVisible: !prev.isVisible,
                      }))
                    }
                    className="px-3 py-1.5 rounded-xl bg-[#3390ec] text-white font-bold"
                  >
                    {mainButton.isVisible ? 'إخفاء MainButton' : 'إظهار MainButton'}
                  </button>
                  <button
                    type="button"
                    onClick={() =>
                      setMainButton((prev) => ({
                        ...prev,
                        isProgressVisible: !prev.isProgressVisible,
                      }))
                    }
                    className="px-3 py-1.5 rounded-xl bg-amber-500 text-white font-bold"
                  >
                    {mainButton.isProgressVisible ? 'إيقاف Progress' : 'تفعيل Progress'}
                  </button>
                </div>
              </div>

              <div
                className={`p-3 rounded-2xl border font-mono text-[11px] ${
                  isDark ? 'bg-black/30 border-gray-800 text-gray-300' : 'bg-gray-100 border-gray-300 text-gray-700'
                }`}
              >
                <p className="font-bold text-[#3390ec] mb-1">// window.Telegram.WebApp Context:</p>
                <p>version: "7.10"</p>
                <p>platform: "weba"</p>
                <p>colorScheme: "{isDark ? 'dark' : 'light'}"</p>
                <p>isExpanded: true</p>
                <p>viewportHeight: 700</p>
                <p>isMainButtonVisible: {mainButton.isVisible ? 'true' : 'false'}</p>
                <p>isBackButtonVisible: {hasBackButton ? 'true' : 'false'}</p>
              </div>
            </div>
          ) : (
            /* External URL iframe with sandbox & full bridge support */
            <iframe
              ref={iframeRef}
              key={iframeKey}
              src={currentUrl}
              title="Telegram Mini App"
              className="w-full h-full border-none"
              sandbox="allow-scripts allow-same-origin allow-forms allow-popups allow-modals allow-top-navigation-by-user-activation"
            />
          )}
        </div>

        {/* Telegram WebApp MainButton (Official Telegram component) */}
        {mainButton.isVisible && (
          <div
            className={`p-3 border-t shrink-0 ${
              isDark ? 'bg-[#242f3d] border-gray-700' : 'bg-gray-50 border-gray-200'
            }`}
          >
            <button
              type="button"
              onClick={handleMainButtonClick}
              disabled={!mainButton.isActive || mainButton.isProgressVisible}
              className="w-full py-3.5 px-4 rounded-2xl font-bold text-sm shadow-md transition transform active:scale-[0.99] flex items-center justify-center gap-2 disabled:opacity-60"
              style={{
                backgroundColor: mainButton.color || '#3390ec',
                color: mainButton.textColor || '#ffffff',
              }}
            >
              {mainButton.isProgressVisible && <Loader2 className="w-4 h-4 animate-spin" />}
              <span>{mainButton.text}</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
