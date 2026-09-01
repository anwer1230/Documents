import React from 'react';
import { X, Rocket, Maximize2, Minimize2 } from 'lucide-react';
import { SendMonitorTab } from './tabs/SendMonitorTab';

interface SendOnlyModalProps {
  isOpen: boolean;
  onClose: () => void;
  lang?: 'ar' | 'en';
}

export const SendOnlyModal: React.FC<SendOnlyModalProps> = ({
  isOpen,
  onClose,
}) => {
  const [isFullscreen, setIsFullscreen] = React.useState(false);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[2500] flex items-center justify-center p-2 sm:p-4 bg-black/85 backdrop-blur-md select-none font-['Cairo',sans-serif]">
      <div
        className={`bg-zinc-950 border border-zinc-800 text-zinc-100 flex flex-col rounded-2xl shadow-2xl transition-all duration-300 overflow-hidden ${
          isFullscreen ? 'w-full h-full rounded-none' : 'w-full max-w-7xl h-[94vh]'
        }`}
        dir="rtl"
      >
        {/* Modal Top Bar */}
        <div className="flex items-center justify-between px-4 sm:px-6 py-3 bg-zinc-900/90 border-b border-zinc-800 shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-amber-500/15 border border-amber-500/30 flex items-center justify-center text-amber-400 font-bold shadow-inner">
              <Rocket className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-base sm:text-lg font-black text-amber-300">
                  الإرسال والمراقبة المدمجة (Send & Monitor)
                </h2>
                <span className="text-[10px] bg-amber-500/20 text-amber-300 border border-amber-500/30 px-2 py-0.5 rounded-full font-bold">
                  ⭐ مدمج أصلي
                </span>
              </div>
              <p className="text-xs text-zinc-400">
                منظومة الأتمتة الموحدة لإرسال الرسائل وجدولتها مع رادار المراقبة التلقائي المباشر
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setIsFullscreen(!isFullscreen)}
              className="p-2 rounded-xl text-zinc-400 hover:text-zinc-100 hover:bg-zinc-800 transition-colors"
              title={isFullscreen ? 'تصغير' : 'ملء الشاشة'}
            >
              {isFullscreen ? <Minimize2 className="w-5 h-5" /> : <Maximize2 className="w-5 h-5" />}
            </button>
            <button
              onClick={onClose}
              className="p-2 rounded-xl text-zinc-400 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
              title="إغلاق"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Content Body */}
        <div className="flex-1 overflow-y-auto p-3 sm:p-5 bg-zinc-950/80">
          <SendMonitorTab onBack={onClose} />
        </div>
      </div>
    </div>
  );
};
