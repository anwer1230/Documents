import React, { useState } from 'react';
import { Download, Share, X, Smartphone } from 'lucide-react';
import { usePWAInstall } from '../../hooks/usePWAInstall';

export const PWAInstallButton: React.FC<{ className?: string; isArabic?: boolean }> = ({
  className = '',
  isArabic = true,
}) => {
  const { isInstallable, isInstalled, isIOS, install } = usePWAInstall();
  const [showIOSGuide, setShowIOSGuide] = useState(false);

  // If already running as an installed PWA, hide the button
  if (isInstalled) {
    return null;
  }

  // Chromium / Android / Desktop flow
  if (isInstallable) {
    return (
      <button
        onClick={install}
        className={`flex items-center gap-2 rounded-xl bg-sky-500 hover:bg-sky-600 px-3.5 py-2 text-xs font-semibold text-white shadow-md transition-all active:scale-95 ${className}`}
        title={isArabic ? 'تثبيت تطبيق تيليجرام على جهازك' : 'Install Telegram on your device'}
      >
        <Download className="w-4 h-4 shrink-0" />
        <span>{isArabic ? 'تثبيت التطبيق (PWA)' : 'Install App'}</span>
      </button>
    );
  }

  // iOS Safari flow (beforeinstallprompt is not supported by WebKit)
  if (isIOS) {
    return (
      <>
        <button
          onClick={() => setShowIOSGuide(true)}
          className={`flex items-center gap-2 rounded-xl border border-sky-500/30 bg-sky-500/10 px-3 py-1.5 text-xs font-medium text-sky-400 hover:bg-sky-500/20 transition ${className}`}
        >
          <Smartphone className="w-3.5 h-3.5" />
          <span>{isArabic ? 'تثبيت على آيفون' : 'Install on iOS'}</span>
        </button>

        {showIOSGuide && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
            <div className="w-full max-w-sm rounded-2xl bg-[#212d3b] border border-white/10 p-6 shadow-2xl text-right rtl:text-right">
              <div className="flex items-center justify-between pb-3 border-b border-white/10">
                <h3 className="text-base font-bold text-white flex items-center gap-2">
                  <Smartphone className="w-5 h-5 text-sky-400" />
                  {isArabic ? 'التثبيت على iPhone / iPad' : 'Install on iPhone / iPad'}
                </h3>
                <button
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

  return null;
};
