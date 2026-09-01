import React, { useState } from "react";
import { X, Lock, Unlock, KeyRound, AlertCircle, ShieldCheck } from "lucide-react";
import { getLockedPin, setLockedPin } from "../../lib/telegramProTools";

interface PinLockModalProps {
  isOpen: boolean;
  onClose: () => void;
  onUnlocked: () => void;
  mode?: "verify" | "setup";
}

export const PinLockModal: React.FC<PinLockModalProps> = ({
  isOpen,
  onClose,
  onUnlocked,
  mode = "verify",
}) => {
  const [pin, setPin] = useState("");
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const currentSavedPin = getLockedPin() || "1234";

  const handleKeyPress = (num: string) => {
    if (pin.length < 4) {
      const next = pin + num;
      setPin(next);
      setError(null);

      if (next.length === 4) {
        if (mode === "setup") {
          setLockedPin(next);
          onUnlocked();
          onClose();
        } else {
          if (next === currentSavedPin) {
            onUnlocked();
            onClose();
          } else {
            setError("رمز PIN غير صحيح، حاول مرة أخرى");
            setPin("");
          }
        }
      }
    }
  };

  const handleDelete = () => {
    setPin((prev) => prev.slice(0, -1));
    setError(null);
  };

  return (
    <div
      id="pin-lock-modal-overlay"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-md animate-fade-in"
      dir="rtl"
    >
      <div
        id="pin-lock-modal"
        className="w-full max-w-xs bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-200 dark:border-slate-800 overflow-hidden flex flex-col items-center p-6 select-none"
      >
        <button
          onClick={onClose}
          className="self-end -mt-2 -mr-2 w-7 h-7 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 transition-colors"
        >
          <X className="w-4 h-4" />
        </button>

        {/* Icon */}
        <div className="w-14 h-14 rounded-full bg-sky-100 dark:bg-sky-950 flex items-center justify-center text-sky-500 mb-3 shadow-md">
          <Lock className="w-7 h-7" />
        </div>

        <h3 className="text-sm font-bold text-slate-800 dark:text-white">
          {mode === "setup" ? "تعيين رمز PIN جديد للمحادثات" : "المحادثات المقفلة والمخفية"}
        </h3>
        <p className="text-[11px] text-slate-400 text-center mt-1 mb-4">
          {mode === "setup"
            ? "أدخل 4 أرقام لحماية محادثاتك السرية"
            : "أدخل رمز PIN السري لإلغاء القفل (الافتراضي: 1234)"}
        </p>

        {/* PIN Indicators */}
        <div className="flex gap-4 mb-5">
          {[0, 1, 2, 3].map((idx) => (
            <div
              key={idx}
              className={`w-3.5 h-3.5 rounded-full transition-all duration-200 ${
                pin.length > idx
                  ? "bg-sky-500 scale-110 shadow-sm shadow-sky-500/50"
                  : "bg-slate-200 dark:bg-slate-700"
              }`}
            />
          ))}
        </div>

        {error && (
          <div className="text-[11px] text-red-500 flex items-center gap-1 mb-3 animate-shake">
            <AlertCircle className="w-3.5 h-3.5" />
            <span>{error}</span>
          </div>
        )}

        {/* Keypad */}
        <div className="grid grid-cols-3 gap-3 w-full max-w-[200px]">
          {["1", "2", "3", "4", "5", "6", "7", "8", "9"].map((num) => (
            <button
              key={num}
              onClick={() => handleKeyPress(num)}
              className="w-14 h-14 rounded-2xl bg-slate-100 dark:bg-slate-800 hover:bg-sky-50 dark:hover:bg-sky-950/60 hover:text-sky-600 text-slate-800 dark:text-slate-100 font-bold text-lg flex items-center justify-center transition-transform active:scale-90"
            >
              {num}
            </button>
          ))}
          <div />
          <button
            onClick={() => handleKeyPress("0")}
            className="w-14 h-14 rounded-2xl bg-slate-100 dark:bg-slate-800 hover:bg-sky-50 dark:hover:bg-sky-950/60 hover:text-sky-600 text-slate-800 dark:text-slate-100 font-bold text-lg flex items-center justify-center transition-transform active:scale-90"
          >
            0
          </button>
          <button
            onClick={handleDelete}
            className="w-14 h-14 rounded-2xl bg-slate-100 dark:bg-slate-800 hover:bg-red-50 dark:hover:bg-red-950/40 hover:text-red-500 text-slate-500 font-bold text-xs flex items-center justify-center transition-transform active:scale-90"
          >
            مسح
          </button>
        </div>
      </div>
    </div>
  );
};
