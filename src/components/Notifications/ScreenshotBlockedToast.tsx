import React, { useEffect, useState } from 'react';
import { CameraOff, Shield, AlertTriangle } from 'lucide-react';
import { useTelegram } from '../../context/TelegramContext';

export const ScreenshotBlockedToast: React.FC = () => {
  const { settings, showToast } = useTelegram();
  const [isVisible, setIsVisible] = useState(false);
  const [reason, setReason] = useState<string>('');

  const isArabic = settings.language === 'ar';

  const triggerBlockedNotice = (customReason?: string) => {
    setReason(
      customReason ||
        (isArabic
          ? 'يحظر التطبيق أو تحظر مؤسستك التقاط لقطات شاشة.'
          : "Can't take screenshot. This app or your organization doesn't allow screenshots.")
    );
    setIsVisible(true);

    // Play subtle security alert vibration / haptic if supported
    if (typeof navigator !== 'undefined' && 'vibrate' in navigator) {
      try {
        navigator.vibrate([100, 50, 100]);
      } catch {}
    }

    setTimeout(() => {
      setIsVisible(false);
    }, 4000);
  };

  useEffect(() => {
    // Listen for custom event
    const handleEvent = (e: any) => {
      triggerBlockedNotice(e.detail?.reason);
    };

    // Listen for keyboard screenshot attempts (PrintScreen, F12, Cmd+Shift+3/4, etc.)
    const handleKeyDown = (e: KeyboardEvent) => {
      if (
        e.key === 'PrintScreen' ||
        (e.ctrlKey && e.key === 'p') ||
        (e.metaKey && e.shiftKey && (e.key === '3' || e.key === '4' || e.key === '5'))
      ) {
        // Intercept and show Android FLAG_SECURE notice
        e.preventDefault?.();
        triggerBlockedNotice();
      }
    };

    window.addEventListener('tg-screenshot-blocked' as any, handleEvent);
    window.addEventListener('keydown', handleKeyDown);

    return () => {
      window.removeEventListener('tg-screenshot-blocked' as any, handleEvent);
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isArabic]);

  if (!isVisible) return null;

  return (
    <div
      id="tg-screenshot-blocked-toast"
      className="fixed bottom-10 left-1/2 -translate-x-1/2 z-[9999] max-w-sm w-[90%] sm:w-auto px-4 py-3 bg-[#212121]/95 text-white backdrop-blur-md rounded-2xl shadow-2xl border border-white/10 flex items-center gap-3 animate-in fade-in slide-in-from-bottom-5 duration-200 pointer-events-auto select-none"
      style={{
        boxShadow: '0 12px 30px rgba(0, 0, 0, 0.6)',
      }}
    >
      <div className="w-9 h-9 rounded-full bg-red-500/20 text-red-400 flex items-center justify-center shrink-0">
        <CameraOff className="w-5 h-5" />
      </div>

      <div className="flex flex-col min-w-0">
        <span className="font-bold text-xs text-white">
          {isArabic ? 'تعذّر حفظ لقطة الشاشة' : "Couldn't save screenshot"}
        </span>
        <span className="text-[11px] text-gray-300 leading-snug">
          {reason || (isArabic ? 'يحظر التطبيق أو تحظر مؤسستك التقاط لقطات شاشة.' : "This app doesn't allow taking screenshots.")}
        </span>
      </div>
    </div>
  );
};
