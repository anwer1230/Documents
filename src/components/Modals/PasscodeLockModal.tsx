import React, { useState } from 'react';
import {
  X,
  Lock,
  Unlock,
  KeyRound,
  Fingerprint,
  Clock,
  ShieldCheck,
  CheckCircle2,
  AlertTriangle,
} from 'lucide-react';
import { useTelegram } from '../../context/TelegramContext';
import { PasscodeSettings } from '../../types';
import confetti from 'canvas-confetti';

interface PasscodeLockModalProps {
  isOpen: boolean;
  onClose: () => void;
  passcodeSettings?: PasscodeSettings;
  onUpdatePasscodeSettings?: (settings: PasscodeSettings) => void;
}

export const PasscodeLockModal: React.FC<PasscodeLockModalProps> = ({
  isOpen,
  onClose,
  passcodeSettings: propSettings,
  onUpdatePasscodeSettings: propOnUpdate,
}) => {
  const { passcodeSettings: ctxSettings, setPasscodeSettings, settings, showToast } = useTelegram();
  const passcodeSettings = propSettings || ctxSettings;
  const onUpdatePasscodeSettings = propOnUpdate || setPasscodeSettings;

  const [pin, setPin] = useState('');
  const [confirmPin, setConfirmPin] = useState('');
  const [step, setStep] = useState<'create' | 'confirm' | 'manage'>('manage');
  const [autoLockMinutes, setAutoLockMinutes] = useState(passcodeSettings.autoLockMinutes || 5);
  const [allowBiometrics, setAllowBiometrics] = useState(passcodeSettings.allowBiometrics ?? true);

  const isRtl = settings.language === 'ar';

  if (!isOpen) return null;

  const handleSaveNewPin = () => {
    if (pin.length !== 4) {
      showToast(isRtl ? 'يجب أن يتكون رمز المرور من 4 أرقام' : 'PIN must be 4 digits', '⚠️');
      return;
    }
    if (pin !== confirmPin) {
      showToast(isRtl ? 'الرمزان غير متطابقين، حاول مجدداً' : 'PINs do not match', '❌');
      return;
    }

    onUpdatePasscodeSettings({
      isEnabled: true,
      passcodeHash: pin,
      autoLockMinutes,
      allowBiometrics,
      isLocked: false,
      lastUnlockedAt: Date.now(),
    });

    confetti({ particleCount: 40, spread: 60, origin: { y: 0.6 } });
    showToast(isRtl ? 'تم تفعيل قفل رمز المرور بنجاح' : 'Passcode Lock enabled', '🔒');
    onClose();
  };

  const handleDisablePasscode = () => {
    onUpdatePasscodeSettings({
      isEnabled: false,
      passcodeHash: undefined,
      autoLockMinutes: 0,
      allowBiometrics: false,
      isLocked: false,
    });
    showToast(isRtl ? 'تم تعطيل رمز المرور' : 'Passcode disabled', '🔓');
    onClose();
  };

  return (
    <div
      id="tg-passcode-lock-modal"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-md animate-in fade-in duration-200"
      onClick={onClose}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-md rounded-3xl overflow-hidden shadow-2xl border flex flex-col max-h-[85vh] animate-in zoom-in-95 duration-200"
        style={{
          backgroundColor: 'var(--tg-theme-surface, #17212b)',
          borderColor: 'var(--tg-theme-border, rgba(255,255,255,0.1))',
          color: 'var(--tg-theme-bubble-in-text, #ffffff)',
        }}
      >
        {/* Header */}
        <div className="p-4.5 border-b border-white/10 flex items-center justify-between bg-black/20">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-indigo-500/20 text-indigo-400 flex items-center justify-center">
              <Lock className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-white">
                {isRtl ? 'قفل التطبيق برمز مرور (Passcode)' : 'Passcode & Biometric Lock'}
              </h2>
              <p className="text-xs text-gray-400">
                {isRtl ? 'حماية تيليجرام برمز PIN أو البصمة' : 'Secure Telegram with PIN or Biometrics'}
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-2 rounded-full hover:bg-white/10 text-gray-400 hover:text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 space-y-6 overflow-y-auto no-scrollbar">
          {passcodeSettings.isEnabled ? (
            /* Passcode is currently Active */
            <div className="space-y-4">
              <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 flex items-center gap-3">
                <ShieldCheck className="w-6 h-6 text-emerald-400 shrink-0" />
                <div className="text-xs">
                  <div className="font-bold text-emerald-300">
                    {isRtl ? 'قفل التطبيق مفعل حالياً' : 'App Lock is Active'}
                  </div>
                  <div className="text-gray-300">
                    {isRtl ? 'تطبيقك محمي برمز PIN سري' : 'Your Telegram is secured by PIN code'}
                  </div>
                </div>
              </div>

              {/* Auto-Lock timer setting */}
              <div className="space-y-2">
                <label className="text-xs font-bold text-gray-300 flex items-center gap-2">
                  <Clock className="w-4 h-4 text-sky-400" />
                  <span>{isRtl ? 'القفل التلقائي بعد عدم النشاط:' : 'Auto-Lock Inactivity:'}</span>
                </label>
                <div className="grid grid-cols-4 gap-2 text-xs font-bold">
                  {[
                    { m: 1, label: '1 min' },
                    { m: 5, label: '5 min' },
                    { m: 60, label: '1 hour' },
                    { m: 300, label: '5 hours' },
                  ].map((item) => (
                    <button
                      key={item.m}
                      onClick={() => setAutoLockMinutes(item.m)}
                      className={`py-2 rounded-xl border transition-colors ${
                        autoLockMinutes === item.m
                          ? 'bg-[#2481cc] text-white border-[#2481cc]'
                          : 'bg-white/5 border-white/10 text-gray-400 hover:text-white'
                      }`}
                    >
                      {item.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* Biometrics Toggle */}
              <div
                onClick={() => setAllowBiometrics(!allowBiometrics)}
                className="p-3.5 rounded-2xl bg-black/20 border border-white/5 flex items-center justify-between cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <Fingerprint className="w-5 h-5 text-purple-400" />
                  <div className="text-xs">
                    <div className="font-bold text-white">
                      {isRtl ? 'فتح القفل بالبصمة / FaceID' : 'Biometric / Fingerprint Unlock'}
                    </div>
                    <div className="text-gray-400 text-[10px]">
                      {isRtl ? 'استخدام مستشعر الجهاز لفتح القفل سريعاً' : 'Use device sensor for fast unlock'}
                    </div>
                  </div>
                </div>
                <input
                  type="checkbox"
                  checked={allowBiometrics}
                  onChange={() => {}}
                  className="w-4 h-4 rounded text-[#2481cc]"
                />
              </div>

              <div className="pt-2 flex flex-col gap-2">
                <button
                  onClick={() => {
                    onUpdatePasscodeSettings({ ...passcodeSettings, isLocked: true });
                    onClose();
                  }}
                  className="w-full py-2.5 rounded-2xl bg-[#2481cc] hover:bg-[#1c6fad] text-white text-xs font-bold shadow-md transition-transform active:scale-95 flex items-center justify-center gap-2"
                >
                  <Lock className="w-4 h-4" />
                  <span>{isRtl ? 'قفل التطبيق الآن' : 'Lock Application Now'}</span>
                </button>

                <button
                  onClick={handleDisablePasscode}
                  className="w-full py-2.5 rounded-2xl bg-rose-500/15 hover:bg-rose-500/25 border border-rose-500/30 text-rose-400 text-xs font-bold transition-colors flex items-center justify-center gap-2"
                >
                  <Unlock className="w-4 h-4" />
                  <span>{isRtl ? 'تعطيل رمز المرور' : 'Turn Off Passcode'}</span>
                </button>
              </div>
            </div>
          ) : (
            /* Set New Passcode PIN */
            <div className="space-y-4">
              <div className="space-y-2">
                <label className="text-xs font-bold text-gray-300">
                  {isRtl ? 'أدخل رمز PIN جديد (4 أرقام):' : 'Enter 4-digit PIN:'}
                </label>
                <input
                  type="password"
                  maxLength={4}
                  value={pin}
                  onChange={(e) => setPin(e.target.value.replace(/\D/g, ''))}
                  placeholder="••••"
                  className="w-full text-center text-2xl tracking-[1em] py-3 bg-black/30 border border-white/15 rounded-2xl text-white font-mono focus:border-[#2481cc] focus:outline-none"
                />
              </div>

              <div className="space-y-2">
                <label className="text-xs font-bold text-gray-300">
                  {isRtl ? 'تأكيد رمز PIN:' : 'Confirm 4-digit PIN:'}
                </label>
                <input
                  type="password"
                  maxLength={4}
                  value={confirmPin}
                  onChange={(e) => setConfirmPin(e.target.value.replace(/\D/g, ''))}
                  placeholder="••••"
                  className="w-full text-center text-2xl tracking-[1em] py-3 bg-black/30 border border-white/15 rounded-2xl text-white font-mono focus:border-[#2481cc] focus:outline-none"
                />
              </div>

              <button
                onClick={handleSaveNewPin}
                disabled={pin.length !== 4 || confirmPin.length !== 4}
                className="w-full py-3 rounded-2xl bg-[#2481cc] hover:bg-[#1c6fad] disabled:opacity-50 text-white text-xs font-bold shadow-lg transition-transform active:scale-95 flex items-center justify-center gap-2"
              >
                <KeyRound className="w-4 h-4" />
                <span>{isRtl ? 'تفعيل وحفظ رمز المرور' : 'Enable Passcode'}</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
