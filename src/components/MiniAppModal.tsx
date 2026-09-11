import React, { useState, useEffect, useRef } from 'react';
import {
  X,
  MoreVertical,
  RotateCcw,
  ExternalLink,
  ShieldCheck,
  Sparkles,
  ArrowLeft,
  ArrowRight,
  Loader2,
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
}

const POPULAR_MINI_APPS = [
  {
    id: 'wallet',
    name: 'Telegram Wallet (@wallet)',
    url: 'https://wallet.tg',
    icon: '💳',
    description: 'إدارة العملات الرقمية والتحويلات المباشرة عبر تيليجرام',
  },
  {
    id: 'notcoin',
    name: 'Community Hub & Games',
    url: 'https://telegram.org',
    icon: '⚡',
    description: 'تطبيق ويب مصغر للمجتمع والألعاب المصغرة',
  },
  {
    id: 'bot_demo',
    name: 'Telegram Mini App Playground',
    url: 'https://core.telegram.org/bots/webapps',
    icon: '🤖',
    description: 'واجهة تجريبية لتطبيقات الويب المصغرة لتيليجرام',
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
}) => {
  const isAr = lang === 'ar';
  const [selectedApp, setSelectedApp] = useState(POPULAR_MINI_APPS[0]);
  const [customUrl, setCustomUrl] = useState('');
  const [currentUrl, setCurrentUrl] = useState(appUrl || POPULAR_MINI_APPS[0].url);
  const [iframeKey, setIframeKey] = useState(0);

  // Official Telegram WebApp SDK State
  const [mainButton, setMainButton] = useState<{
    isVisible: boolean;
    text: string;
    color: string;
    textColor: string;
    isActive: boolean;
    isProgressVisible: boolean;
  }>({
    isVisible: false,
    text: 'Continue',
    color: '#3390ec',
    textColor: '#ffffff',
    isActive: true,
    isProgressVisible: false,
  });

  const [hasBackButton, setHasBackButton] = useState(false);
  const iframeRef = useRef<HTMLIFrameElement>(null);

  // Sync currentUrl when appUrl prop changes
  useEffect(() => {
    if (appUrl) {
      setCurrentUrl(appUrl);
      setIframeKey((prev) => prev + 1);
    }
  }, [appUrl]);

  // Telegram WebApp postMessage Bridge (matches official Telegram Web K SDK)
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

  const handleReload = () => {
    setIframeKey((prev) => prev + 1);
  };

  const handleSelectPreset = (app: typeof POPULAR_MINI_APPS[0]) => {
    setSelectedApp(app);
    setCurrentUrl(app.url);
    setIframeKey((prev) => prev + 1);
  };

  const handleLoadCustomUrl = (e: React.FormEvent) => {
    e.preventDefault();
    if (!customUrl.trim()) return;
    let target = customUrl.trim();
    if (!target.startsWith('http://') && !target.startsWith('https://')) {
      target = 'https://' + target;
    }
    setCurrentUrl(target);
    setIframeKey((prev) => prev + 1);
  };

  const handleMainButtonClick = () => {
    if (!mainButton.isActive || mainButton.isProgressVisible) return;
    if (iframeRef.current && iframeRef.current.contentWindow) {
      iframeRef.current.contentWindow.postMessage(
        JSON.stringify({ eventType: 'main_button_pressed' }),
        '*'
      );
    }
  };

  const handleBackButtonClick = () => {
    if (iframeRef.current && iframeRef.current.contentWindow) {
      iframeRef.current.contentWindow.postMessage(
        JSON.stringify({ eventType: 'back_button_pressed' }),
        '*'
      );
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-2 sm:p-4 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200">
      <div
        className={`w-full max-w-2xl h-[90vh] max-h-[780px] rounded-2xl shadow-2xl flex flex-col overflow-hidden border ${
          isDark ? 'bg-[#17212b] border-gray-700/80 text-white' : 'bg-white border-gray-200 text-gray-900'
        }`}
      >
        {/* Header (Matching Telegram Web K WebApp window) */}
        <div
          className={`flex items-center justify-between px-4 py-3 border-b select-none shrink-0 ${
            isDark ? 'bg-[#242f3d] border-gray-700/60' : 'bg-gray-50 border-gray-200'
          }`}
        >
          <div className="flex items-center gap-3">
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
              {selectedApp.icon || '🤖'}
            </div>
            <div>
              <div className="flex items-center gap-1.5 font-semibold text-sm">
                <span>{selectedApp.name || appName}</span>
                <ShieldCheck className="w-4 h-4 text-[#3390ec]" />
              </div>
              <p className="text-[11px] text-gray-400">
                @{botUsername} • {isAr ? 'تطبيق ويب مصغر لتيليجرام' : 'Telegram Mini App Webview'}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-1">
            <button
              type="button"
              onClick={handleReload}
              className="p-2 rounded-full text-gray-400 hover:text-white hover:bg-white/10 transition-colors"
              title={isAr ? 'إعادة تحميل' : 'Reload'}
            >
              <RotateCcw className="w-4 h-4" />
            </button>
            <a
              href={currentUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="p-2 rounded-full text-gray-400 hover:text-white hover:bg-white/10 transition-colors"
              title={isAr ? 'فتح في نافذة جديدة' : 'Open in new tab'}
            >
              <ExternalLink className="w-4 h-4" />
            </a>
            <button
              type="button"
              onClick={onClose}
              className="p-2 rounded-full text-gray-400 hover:text-red-400 hover:bg-red-500/10 transition-colors ml-1"
              title={isAr ? 'إغلاق' : 'Close'}
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Quick App Preset Switcher */}
        <div
          className={`px-4 py-2 border-b flex items-center gap-2 overflow-x-auto text-xs shrink-0 ${
            isDark ? 'bg-[#1c2733] border-gray-800' : 'bg-gray-100 border-gray-200'
          }`}
        >
          <span className="text-gray-400 text-[11px] shrink-0 font-medium">
            {isAr ? 'تطبيقات مميزة:' : 'Featured Apps:'}
          </span>
          {POPULAR_MINI_APPS.map((app) => (
            <button
              key={app.id}
              type="button"
              onClick={() => handleSelectPreset(app)}
              className={`px-3 py-1 rounded-full text-xs shrink-0 transition-all flex items-center gap-1.5 ${
                selectedApp.id === app.id
                  ? 'bg-[#3390ec] text-white font-medium shadow-sm'
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

        {/* Custom Mini App URL Input */}
        <form
          onSubmit={handleLoadCustomUrl}
          className={`px-4 py-2 border-b flex items-center gap-2 shrink-0 ${
            isDark ? 'bg-[#18222d] border-gray-800' : 'bg-gray-50 border-gray-200'
          }`}
        >
          <input
            type="text"
            value={customUrl}
            onChange={(e) => setCustomUrl(e.target.value)}
            placeholder={
              isAr
                ? 'أدخل رابط تطبيق ويب مصغر (https://...)'
                : 'Enter Mini App URL (https://...)'
            }
            className={`flex-1 px-3 py-1.5 rounded-lg text-xs outline-none border ${
              isDark
                ? 'bg-[#242f3d] border-gray-700 text-white placeholder-gray-400'
                : 'bg-white border-gray-300 text-gray-900'
            }`}
          />
          <button
            type="submit"
            className="px-3 py-1.5 rounded-lg bg-[#3390ec] text-white text-xs font-medium hover:bg-[#2881da] transition-colors"
          >
            {isAr ? 'تشغيل' : 'Launch'}
          </button>
        </form>

        {/* Mini App Frame */}
        <div className="flex-1 w-full bg-white relative overflow-hidden">
          <iframe
            ref={iframeRef}
            key={iframeKey}
            src={currentUrl}
            title="Telegram Mini App"
            className="w-full h-full border-none"
            sandbox="allow-scripts allow-same-origin allow-forms allow-popups allow-modals allow-top-navigation-by-user-activation"
          />
        </div>

        {/* Telegram WebApp Dynamic MainButton (Official Telegram Web K component) */}
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
              className="w-full py-3 px-4 rounded-xl font-bold text-sm shadow-md transition transform active:scale-[0.99] flex items-center justify-center gap-2 disabled:opacity-60"
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
