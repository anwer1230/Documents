import React, { useEffect, useState } from "react";
import { Download, X, CheckCircle2 } from "lucide-react";

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: "accepted" | "dismissed"; platform: string }>;
}

export const PwaInstallBanner: React.FC = () => {
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null);
  const [isVisible, setIsVisible] = useState(false);
  const [isInstalled, setIsInstalled] = useState(false);
  const [isInstalling, setIsInstalling] = useState(false);
  const [justInstalled, setJustInstalled] = useState(false);

  useEffect(() => {
    // Check if the application is running in standalone mode (already installed and opened as app)
    const checkIsStandalone = () => {
      const isStandaloneMedia = window.matchMedia("(display-mode: standalone)").matches;
      const isIosStandalone = (navigator as unknown as { standalone?: boolean }).standalone === true;
      const isAndroidApp = document.referrer.includes("android-app://");

      return Boolean(isStandaloneMedia || isIosStandalone || isAndroidApp);
    };

    if (checkIsStandalone()) {
      setIsInstalled(true);
      setIsVisible(false);
      return;
    }

    // Check if user dismissed the banner in current session
    const isDismissed = sessionStorage.getItem("tg_pwa_dismissed") === "true";

    // Listen to beforeinstallprompt event from browser
    const handleBeforeInstallPrompt = (e: Event) => {
      e.preventDefault();
      setDeferredPrompt(e as BeforeInstallPromptEvent);
      if (!isDismissed) {
        setIsVisible(true);
      }
    };

    // Listen to appinstalled event
    const handleAppInstalled = () => {
      setIsInstalled(true);
      setIsVisible(false);
      setDeferredPrompt(null);
      setJustInstalled(true);
      setTimeout(() => setJustInstalled(false), 3000);
    };

    window.addEventListener("beforeinstallprompt", handleBeforeInstallPrompt);
    window.addEventListener("appinstalled", handleAppInstalled);

    // Fallback: If in browser environment and not dismissed, show prompt after brief delay
    const timer = setTimeout(() => {
      if (!checkIsStandalone() && !isDismissed) {
        setIsVisible(true);
      }
    }, 1500);

    return () => {
      window.removeEventListener("beforeinstallprompt", handleBeforeInstallPrompt);
      window.removeEventListener("appinstalled", handleAppInstalled);
      clearTimeout(timer);
    };
  }, []);

  const handleInstallClick = async () => {
    if (deferredPrompt) {
      setIsInstalling(true);
      try {
        await deferredPrompt.prompt();
        const choice = await deferredPrompt.userChoice;
        if (choice.outcome === "accepted") {
          setIsVisible(false);
          setIsInstalled(true);
        }
      } catch (err) {
        console.error("Installation error:", err);
      } finally {
        setIsInstalling(false);
        setDeferredPrompt(null);
      }
    } else {
      // Fallback for browsers with no direct prompt or custom PWA handling
      setIsInstalling(true);
      setTimeout(() => {
        setIsInstalling(false);
        setIsVisible(false);
      }, 1000);
    }
  };

  const handleDismiss = () => {
    setIsVisible(false);
    sessionStorage.setItem("tg_pwa_dismissed", "true");
  };

  // If already installed as standalone app, DO NOT render anything
  if (isInstalled || !isVisible) {
    if (justInstalled) {
      return (
        <div
          id="pwa-installed-toast"
          className="fixed bottom-5 left-1/2 -translate-x-1/2 z-50 bg-[#2481cc] text-white px-4 py-2.5 rounded-xl shadow-xl flex items-center gap-2 text-xs font-bold animate-fade-in"
          dir="rtl"
        >
          <CheckCircle2 className="w-4 h-4 text-emerald-300" />
          <span>تم تثبيت تطبيق Telegram بنجاح!</span>
        </div>
      );
    }
    return null;
  }

  return (
    <div
      id="pwa-install-notification-banner"
      className="fixed bottom-4 left-4 right-4 sm:left-auto sm:right-6 sm:max-w-sm z-50 bg-white/95 dark:bg-[#182533]/95 backdrop-blur-md border border-slate-200 dark:border-slate-800 shadow-2xl rounded-2xl p-3 flex items-center justify-between gap-3 text-slate-800 dark:text-slate-100 animate-slide-up"
      dir="rtl"
    >
      {/* App Icon & Title */}
      <div className="flex items-center gap-3 min-w-0">
        <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-[#229ED9] to-[#2AABEE] flex items-center justify-center text-white shadow-sm shrink-0">
          <svg className="w-6 h-6 fill-current" viewBox="0 0 24 24">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm4.64 6.8c-.15 1.58-.8 5.42-1.13 7.19-.14.75-.42 1-.68 1.03-.58.05-1.02-.38-1.58-.75-.88-.58-1.38-.94-2.23-1.5-.99-.65-.35-1.01.22-1.59.15-.15 2.71-2.48 2.76-2.69a.2.2 0 00-.05-.18c-.06-.05-.14-.03-.21-.02-.09.02-1.49.95-4.22 2.79-.4.27-.76.41-1.08.4-.36-.01-1.04-.2-1.55-.37-.63-.2-1.12-.31-1.08-.66.02-.18.27-.36.75-.55 2.92-1.27 4.86-2.11 5.83-2.51 2.78-1.16 3.35-1.36 3.73-1.36.08 0 .27.02.39.12.1.08.13.19.14.27-.01.06.01.24 0 .38z" />
          </svg>
        </div>

        <div className="min-w-0">
          <h4 className="text-xs font-bold text-slate-900 dark:text-slate-100 truncate">
            تثبيت Telegram Web
          </h4>
          <p className="text-[11px] text-slate-500 dark:text-slate-400 truncate">
            تثبيت التطبيق على جهازك
          </p>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="flex items-center gap-1.5 shrink-0">
        <button
          id="btn-pwa-install-action"
          onClick={handleInstallClick}
          disabled={isInstalling}
          className="h-8 px-3.5 bg-[#2481cc] hover:bg-[#1d6fa5] active:scale-95 text-white text-xs font-bold rounded-xl transition-all shadow-xs flex items-center gap-1.5 disabled:opacity-50"
        >
          <Download className={`w-3.5 h-3.5 ${isInstalling ? "animate-bounce" : ""}`} />
          <span>تثبيت</span>
        </button>

        <button
          id="btn-pwa-dismiss-action"
          onClick={handleDismiss}
          className="w-8 h-8 rounded-xl flex items-center justify-center text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          title="إغلاق"
        >
          <X className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};
