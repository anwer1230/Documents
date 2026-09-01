import React, { useState, useEffect } from 'react';
import {
  Lock,
  Unlock,
  Fingerprint,
  Delete,
  ShieldAlert,
  Sparkles,
} from 'lucide-react';
import { PasscodeSettings } from '../../types';
import { useTelegram } from '../../context/TelegramContext';
import confetti from 'canvas-confetti';

interface PasscodeLockOverlayProps {
  passcodeSettings?: PasscodeSettings;
  onUnlock?: () => void;
  lang?: 'ar' | 'en';
}

export const PasscodeLockOverlay: React.FC<PasscodeLockOverlayProps> = ({
  passcodeSettings: propSettings,
  onUnlock: propOnUnlock,
  lang: propLang,
}) => {
  const { passcodeSettings: ctxSettings, unlockApp, settings } = useTelegram();
  const passcodeSettings = propSettings || ctxSettings;
  const onUnlock = propOnUnlock || unlockApp;
  const lang = propLang || (settings.language === 'ar' ? 'ar' : 'en');

  const [pin, setPin] = useState('');
  const [isShaking, setIsShaking] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const isRtl = lang === 'ar';

  if (!passcodeSettings.isEnabled || !passcodeSettings.isLocked) return null;

  useEffect(() => {
    if (pin.length === 4) {
      if (pin === passcodeSettings.passcodeHash) {
        confetti({ particleCount: 30, spread: 50, origin: { y: 0.6 } });
        onUnlock();
      } else {
        setIsShaking(true);
        setErrorMsg(isRtl ? 'رمز المرور غير صحيح' : 'Incorrect Passcode');
        setTimeout(() => {
          setIsShaking(false);
          setPin('');
        }, 500);
      }
    }
  }, [pin, passcodeSettings.passcodeHash, onUnlock, isRtl]);

  const handleKeyPress = (digit: string) => {
    if (pin.length < 4) {
      setErrorMsg('');
      setPin((prev) => prev + digit);
    }
  };

  const handleDelete = () => {
    setPin((prev) => prev.slice(0, -1));
    setErrorMsg('');
  };

  const handleBiometricUnlock = () => {
    // Biometric scanner simulation
    confetti({ particleCount: 40, spread: 60, origin: { y: 0.6 } });
    onUnlock();
  };

  if (!passcodeSettings.isEnabled || !passcodeSettings.isLocked) {
    return null;
  }

  return (
    <div
      id="tg-passcode-lock-overlay"
      className="fixed inset-0 z-[9999] flex flex-col items-center justify-center p-6 bg-[#0e1621] text-white select-none animate-in fade-in duration-200"
    >
      <div className="flex flex-col items-center max-w-xs w-full space-y-6">
        {/* Lock Icon */}
        <div className="w-16 h-16 rounded-3xl bg-[#2481cc]/20 border border-[#2481cc]/40 text-[#2481cc] flex items-center justify-center shadow-2xl">
          <Lock className="w-8 h-8" />
        </div>

        {/* Title */}
        <div className="text-center space-y-1">
          <h2 className="text-lg font-bold text-white">
            {isRtl ? 'تيليجرام مقفل' : 'Telegram Locked'}
          </h2>
          <p className="text-xs text-gray-400">
            {isRtl ? 'أدخل رمز المرور للمتابعة' : 'Enter passcode to continue'}
          </p>
        </div>

        {/* PIN Dots */}
        <div
          className={`flex items-center justify-center gap-4 my-2 ${
            isShaking ? 'animate-bounce text-rose-500' : ''
          }`}
        >
          {[0, 1, 2, 3].map((idx) => (
            <div
              key={idx}
              className={`w-3.5 h-3.5 rounded-full transition-all duration-150 ${
                pin.length > idx
                  ? 'bg-[#2481cc] scale-125 shadow-lg shadow-sky-500/50'
                  : 'bg-white/20'
              }`}
            />
          ))}
        </div>

        {errorMsg && (
          <p className="text-xs text-rose-400 font-bold animate-pulse">
            {errorMsg}
          </p>
        )}

        {/* Numeric Keypad */}
        <div className="grid grid-cols-3 gap-4 w-full pt-4">
          {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map((num) => (
            <button
              key={num}
              onClick={() => handleKeyPress(num)}
              className="w-16 h-16 rounded-full bg-white/5 hover:bg-white/15 border border-white/10 text-2xl font-bold font-mono transition-all active:scale-90 flex items-center justify-center mx-auto"
            >
              {num}
            </button>
          ))}

          {/* Biometrics button */}
          {passcodeSettings.allowBiometrics ? (
            <button
              onClick={handleBiometricUnlock}
              className="w-16 h-16 rounded-full bg-indigo-500/20 hover:bg-indigo-500/30 text-indigo-400 transition-all active:scale-90 flex items-center justify-center mx-auto"
              title={isRtl ? 'فتح بالبصمة' : 'Biometric unlock'}
            >
              <Fingerprint className="w-7 h-7" />
            </button>
          ) : (
            <div className="w-16 h-16" />
          )}

          {/* Zero */}
          <button
            onClick={() => handleKeyPress('0')}
            className="w-16 h-16 rounded-full bg-white/5 hover:bg-white/15 border border-white/10 text-2xl font-bold font-mono transition-all active:scale-90 flex items-center justify-center mx-auto"
          >
            0
          </button>

          {/* Backspace */}
          <button
            onClick={handleDelete}
            className="w-16 h-16 rounded-full hover:bg-white/10 text-gray-400 hover:text-white transition-all active:scale-90 flex items-center justify-center mx-auto"
            title={isRtl ? 'مسح' : 'Delete'}
          >
            <Delete className="w-6 h-6" />
          </button>
        </div>
      </div>
    </div>
  );
};
