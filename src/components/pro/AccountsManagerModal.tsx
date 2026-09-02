import React from 'react';
import { X, Users2, Shield, Globe } from 'lucide-react';
import { AccountsManagerTab } from '../tabs/AccountsManagerTab';
import { TelegramAccount } from '../../types';

interface AccountsManagerModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAccountSwitched?: (account: TelegramAccount) => void;
}

export const AccountsManagerModal: React.FC<AccountsManagerModalProps> = ({
  isOpen,
  onClose,
  onAccountSwitched,
}) => {
  if (!isOpen) return null;

  return (
    <div
      id="accounts-manager-modal-overlay"
      className="fixed inset-0 z-50 flex items-center justify-center p-2 sm:p-4 bg-black/70 backdrop-blur-sm animate-fade-in"
      dir="rtl"
    >
      <div
        id="accounts-manager-modal-container"
        className="w-full max-w-6xl h-[92vh] bg-slate-900 text-slate-100 rounded-2xl shadow-2xl border border-slate-800 flex flex-col overflow-hidden select-none"
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-950/80">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-sky-500/10 border border-sky-500/30 flex items-center justify-center text-sky-400">
              <Users2 className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-base font-bold text-white">إدارة الحسابات المتعددة والبروكسيات المستقلة</h2>
                <span className="px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 text-[10px] font-bold border border-emerald-500/30 flex items-center gap-1">
                  <Shield className="w-3 h-3" />
                  حماية ضد الحظر
                </span>
              </div>
              <p className="text-xs text-slate-400">
                تشغيل حسابات تيليجرام متعددة مع بروكسي SOCKS5/HTTP مستقل لكل حساب والنشر المتزامن بنقرة واحدة
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="w-9 h-9 rounded-xl flex items-center justify-center text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body Content */}
        <div className="flex-1 overflow-y-auto p-4 custom-scrollbar bg-slate-900/90">
          <AccountsManagerTab
            onAccountSwitched={(acc) => {
              if (onAccountSwitched) onAccountSwitched(acc);
            }}
          />
        </div>
      </div>
    </div>
  );
};
