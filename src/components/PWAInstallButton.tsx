import React, { useState, useEffect, useCallback } from 'react';
import { Download, CheckCircle2, Loader2, Smartphone, Share, X, Info } from 'lucide-react';

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>;
}

export interface PWAInstallButtonProps {
  className?: string;
  isArabic?: boolean;
}

export const PWAInstallButton: React.FC<PWAInstallButtonProps> = ({
  className = '',
  isArabic = true,
}) => {
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null);
  const [isStandalone, setIsStandalone] = useState<boolean>(false);
  const [isJustInstalled, setIsJustInstalled] = useState<boolean>(false);
  const [isInstalling, setIsInstalling] = useState<boolean>(false);
  const [showToastMessage, setShowToastMessage] = useState<string | null>(null);
  const [showIOSGuide, setShowIOSGuide] = useState<boolean>(false);
  const [isIOS, setIsIOS] = useState<boolean>(false);

  // 1. Initial standalone & iOS detection
  useEffect(() => {
    const checkStandalone = () => {
      const standalone =
        (typeof window !== 'undefined' &&
          (window.matchMedia('(display-mode: standalone)').matches ||
            (window.navigator as unknown as { standalone?: boolean }).standalone === true ||
            document.referrer.includes('android-app://'))) ||
        false;
      setIsStandalone(standalone);
    };

    checkStandalone();

    // Listen for display-mode changes
    const mediaQuery = window.matchMedia('(display-mode: standalone)');
    const handleModeChange = (e: MediaQueryListEvent) => {
      setIsStandalone(e.matches);
    };
    if (mediaQuery?.addEventListener) {
      mediaQuery.addEventListener('change', handleModeChange);
    } else if (mediaQuery?.addListener) {
      mediaQuery.addListener(handleModeChange);
    }

    // Detect iOS devices
    const ua = typeof window !== 'undefined' ? window.navigator.userAgent.toLowerCase() : '';
    const isIOSDevice = /iphone|ipad|ipod/.test(ua);
    setIsIOS(isIOSDevice);

    // 2. Listen for beforeinstallprompt event (Phase 1: Initial Install)
    const handleBeforeInstallPrompt = (e: Event) => {
      e.preventDefault();
      setDeferredPrompt(e as BeforeInstallPromptEvent);
    };

    // 3. Listen for appinstalled event
    const handleAppInstalled = () => {
      setIsJustInstalled(true);
      setIsInstalling(false);
      setDeferredPrompt(null);
      setShowToastMessage(
        isArabic
          ? 'تم تثبيت التطبيق بنجاح! سيتم تطبيق التحديثات تلقائياً وبسرعة.'
          : 'App installed successfully! Updates will be applied automatically and quickly.'
      );
    };

    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    window.addEventListener('appinstalled', handleAppInstalled);

    return () => {
      if (mediaQuery?.removeEventListener) {
        mediaQuery.removeEventListener('change', handleModeChange);
      } else if (mediaQuery?.removeListener) {
        mediaQuery.removeListener(handleModeChange);
      }
      window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
      window.removeEventListener('appinstalled', handleAppInstalled);
    };
  }, [isArabic]);

  // Auto-dismiss toast after 4 seconds
  useEffect(() => {
    if (!showToastMessage) return;
    const timer = setTimeout(() => {
      setShowToastMessage(null);
    }, 4000);
    return () => clearTimeout(timer);
  }, [showToastMessage]);

  // Phase 1: Handle first-time installation click
  const handleInstallClick = useCallback(async () => {
    if (!deferredPrompt) {
      if (isIOS) {
        setShowIOSGuide(true);
      }
      return;
    }

    // 1. Show "Installing..." state
    setIsInstalling(true);
    setShowToastMessage(isArabic ? 'جاري التثبيت...' : 'Installing...');

    try {
      // 2. Call deferredPrompt.prompt() to open native browser install dialog
      await deferredPrompt.prompt();

      // Wait for user choice
      const choiceResult = await deferredPrompt.userChoice;

      if (choiceResult.outcome === 'accepted') {
        // 3. On success, hide button by marking installed
        setIsJustInstalled(true);
        setDeferredPrompt(null);
        setShowToastMessage(
          isArabic
            ? 'تم قبول التثبيت بنجاح! التطبيق مثبت الآن.'
            : 'Installation accepted! The app is now installed.'
        );
      } else {
        // User cancelled / dismissed the install prompt
        setShowToastMessage(
          isArabic
            ? 'تم إلغاء التثبيت. يمكنك التثبيت في أي وقت.'
            : 'Installation cancelled. You can install anytime.'
        );
      }
    } catch (err) {
      console.error('[PWAInstallButton] Error during prompt():', err);
    } finally {
      setIsInstalling(false);
    }
  }, [deferredPrompt, isArabic, isIOS]);

  // Phase 2: Handle click when already installed / standalone
  const handleAlreadyInstalledClick = useCallback(() => {
    setShowToastMessage(
      isArabic
        ? 'التطبيق مثبت بالفعل. سيتم تطبيق التحديثات تلقائياً وبسرعة'
        : 'The application is already installed. Updates will be applied automatically and quickly.'
    );
  }, [isArabic]);

  // CASE A: App was just installed during this session in normal browser view -> Hide install button
  if (isJustInstalled && !isStandalone) {
    return (
      <>
        {showToastMessage && (
          <div className="fixed bottom-5 right-5 z-[99999] flex items-center gap-3 rounded-2xl bg-[#1e293b]/95 border border-emerald-500/40 text-white px-4 py-3 shadow-2xl backdrop-blur-md animate-in fade-in slide-in-from-bottom-3 duration-300">
            <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
            <p className="text-xs font-semibold">{showToastMessage}</p>
            <button
              onClick={() => setShowToastMessage(null)}
              className="text-gray-400 hover:text-white p-1 rounded-lg transition-colors"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        )}
      </>
    );
  }

  // CASE B: App is already installed (standalone mode active)
  if (isStandalone) {
    return (
      <div className="relative inline-flex items-center">
        <button
          type="button"
          onClick={handleAlreadyInstalledClick}
          className={`flex items-center gap-2 rounded-xl bg-emerald-500/10 hover:bg-emerald-500/20 border border-emerald-500/30 px-3 py-1.5 text-xs font-semibold text-emerald-400 shadow-sm transition-all active:scale-95 ${className}`}
          title={
            isArabic
              ? 'التطبيق مثبت بالفعل. سيتم تطبيق التحديثات تلقائياً وبسرعة'
              : 'The application is already installed. Updates will be applied automatically and quickly.'
          }
        >
          <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
          <span>{isArabic ? 'التطبيق مثبت (تحديث تلقائي)' : 'App Installed (Auto-Update)'}</span>
        </button>

        {/* Floating Toast Notification on click */}
        {showToastMessage && (
          <div className="fixed bottom-5 right-5 z-[99999] flex items-center gap-3 rounded-2xl bg-[#1e293b]/95 border border-emerald-500/40 text-white px-4 py-3 shadow-2xl backdrop-blur-md animate-in fade-in slide-in-from-bottom-3 duration-300">
            <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
            <p className="text-xs font-semibold">{showToastMessage}</p>
            <button
              onClick={() => setShowToastMessage(null)}
              className="text-gray-400 hover:text-white p-1 rounded-lg transition-colors"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        )}
      </div>
    );
  }

  // CASE C: First run - Installable (deferredPrompt ready)
  if (deferredPrompt) {
    return (
      <div className="relative inline-flex items-center">
        <button
          type="button"
          onClick={handleInstallClick}
          disabled={isInstalling}
          className={`flex items-center gap-2 rounded-xl bg-sky-500 hover:bg-sky-600 disabled:bg-sky-600/60 px-3.5 py-2 text-xs font-semibold text-white shadow-md transition-all active:scale-95 ${className}`}
          title={isArabic ? 'تثبيت التطبيق على جهازك' : 'Install App on your device'}
        >
          {isInstalling ? (
            <>
              <Loader2 className="w-4 h-4 shrink-0 animate-spin" />
              <span>{isArabic ? 'جاري التثبيت...' : 'Installing...'}</span>
            </>
          ) : (
            <>
              <Download className="w-4 h-4 shrink-0" />
              <span>{isArabic ? 'تثبيت التطبيق (PWA)' : 'Install App'}</span>
            </>
          )}
        </button>

        {/* Floating Toast */}
        {showToastMessage && (
          <div className="fixed bottom-5 right-5 z-[99999] flex items-center gap-3 rounded-2xl bg-[#1e293b]/95 border border-sky-500/40 text-white px-4 py-3 shadow-2xl backdrop-blur-md animate-in fade-in slide-in-from-bottom-3 duration-300">
            {isInstalling ? (
              <Loader2 className="w-5 h-5 text-sky-400 shrink-0 animate-spin" />
            ) : (
              <Info className="w-5 h-5 text-sky-400 shrink-0" />
            )}
            <p className="text-xs font-semibold">{showToastMessage}</p>
            <button
              onClick={() => setShowToastMessage(null)}
              className="text-gray-400 hover:text-white p-1 rounded-lg transition-colors"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        )}
      </div>
    );
  }

  // CASE D: iOS Safari instructions
  if (isIOS) {
    return (
      <>
        <button
          type="button"
          onClick={() => setShowIOSGuide(true)}
          className={`flex items-center gap-2 rounded-xl border border-sky-500/30 bg-sky-500/10 px-3 py-1.5 text-xs font-medium text-sky-400 hover:bg-sky-500/20 transition ${className}`}
        >
          <Smartphone className="w-3.5 h-3.5 shrink-0" />
          <span>{isArabic ? 'تثبيت على آيفون' : 'Install on iOS'}</span>
        </button>

        {showIOSGuide && (
          <div className="fixed inset-0 z-[99999] flex items-center justify-center bg-black/75 backdrop-blur-sm p-4 animate-in fade-in select-none">
            <div
              className="w-full max-w-sm rounded-2xl bg-[#1e293b] border border-white/10 p-6 shadow-2xl text-right rtl:text-right"
              dir={isArabic ? 'rtl' : 'ltr'}
            >
              <div className="flex items-center justify-between pb-3 border-b border-white/10">
                <h3 className="text-base font-bold text-white flex items-center gap-2">
                  <Smartphone className="w-5 h-5 text-sky-400" />
                  <span>{isArabic ? 'التثبيت على iPhone / iPad' : 'Install on iPhone / iPad'}</span>
                </h3>
                <button
                  type="button"
                  onClick={() => setShowIOSGuide(false)}
                  className="p-1 rounded-lg text-gray-400 hover:text-white hover:bg-white/10"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="mt-4 space-y-3 text-sm text-gray-300">
                <div className="flex items-start gap-3 bg-white/5 p-3 rounded-xl">
                  <div className="w-6 h-6 rounded-full bg-sky-500/20 text-sky-400 flex items-center justify-center font-bold text-xs shrink-0">
                    1
                  </div>
                  <p>
                    {isArabic ? (
                      <>
                        اضغط على زر <strong className="text-sky-400">المشاركة (Share <Share className="inline w-3.5 h-3.5" />)</strong> في شريط سفاري السفلي.
                      </>
                    ) : (
                      <>
                        Tap the <strong>Share</strong> button in Safari toolbar.
                      </>
                    )}
                  </p>
                </div>

                <div className="flex items-start gap-3 bg-white/5 p-3 rounded-xl">
                  <div className="w-6 h-6 rounded-full bg-sky-500/20 text-sky-400 flex items-center justify-center font-bold text-xs shrink-0">
                    2
                  </div>
                  <p>
                    {isArabic ? (
                      <>
                        مرر لأسفل واختر <strong className="text-sky-400">إضافة إلى الشاشة الرئيسية (Add to Home Screen)</strong>.
                      </>
                    ) : (
                      <>
                        Scroll down and select <strong>Add to Home Screen</strong>.
                      </>
                    )}
                  </p>
                </div>
              </div>

              <button
                type="button"
                onClick={() => setShowIOSGuide(false)}
                className="mt-5 w-full rounded-xl bg-sky-500 py-2.5 text-sm font-semibold text-white hover:bg-sky-600 transition"
              >
                {isArabic ? 'فهمت ذلك' : 'Got it'}
              </button>
            </div>
          </div>
        )}
      </>
    );
  }

  // Not installable in this browser mode and not standalone
  return null;
};

export default PWAInstallButton;
