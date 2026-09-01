import React, { useState } from "react";
import { X, Users, Plus, Check, Trash2, Key, Sparkles, Phone, ShieldCheck } from "lucide-react";
import { MultiAccount, TelegramUser } from "../../types";
import { addAccount, removeAccount, switchActiveAccount } from "../../lib/telegramProTools";

const getAvatarColor = (name: string) => {
  const colors = [
    'from-red-500 to-orange-500',
    'from-blue-500 to-cyan-500',
    'from-emerald-500 to-teal-500',
    'from-purple-500 to-indigo-500',
    'from-pink-500 to-rose-500',
    'from-amber-500 to-yellow-500',
  ];
  let hash = 0;
  for (let i = 0; i < (name || '').length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
  return colors[Math.abs(hash) % colors.length];
};

interface MultiAccountModalProps {
  isOpen: boolean;
  onClose: () => void;
  accounts: MultiAccount[];
  onAccountsChange: (accounts: MultiAccount[]) => void;
  onOpenAddAccountAuth: () => void;
}

export const MultiAccountModal: React.FC<MultiAccountModalProps> = ({
  isOpen,
  onClose,
  accounts,
  onAccountsChange,
  onOpenAddAccountAuth,
}) => {
  const [quickStringSession, setQuickStringSession] = useState("");
  const [quickName, setQuickName] = useState("");
  const [quickPhone, setQuickPhone] = useState("");
  const [showQuickForm, setShowQuickForm] = useState(false);

  if (!isOpen) return null;

  const handleSelectAccount = (accountId: string) => {
    const active = switchActiveAccount(accountId);
    if (active) {
      const updated = accounts.map((a) => ({ ...a, isActive: a.id === accountId }));
      onAccountsChange(updated);
      onClose();
      window.location.reload();
    }
  };

  const handleRemoveAccount = (e: React.MouseEvent, accountId: string) => {
    e.stopPropagation();
    if (accounts.length <= 1) {
      alert("لا يمكن حذف الحساب الوحيد المتبقي");
      return;
    }
    const updated = removeAccount(accountId);
    onAccountsChange(updated);
  };

  const handleAddQuickAccount = (e: React.FormEvent) => {
    e.preventDefault();
    if (!quickName.trim()) return;

    const newUser: TelegramUser = {
      id: `tg_user_${Date.now()}`,
      firstName: quickName.trim(),
      lastName: "Pro",
      phone: quickPhone.trim() || "+1 555 0199",
      username: quickName.toLowerCase().replace(/\s+/g, "_") + "_pro",
      isPremium: true,
      status: "online",
    };

    const session = quickStringSession.trim() || `session_string_${Date.now()}`;
    const updated = addAccount(newUser, session);
    onAccountsChange(updated);
    setShowQuickForm(false);
    setQuickName("");
    setQuickPhone("");
    setQuickStringSession("");
    onClose();
    window.location.reload();
  };

  return (
    <div
      id="multi-account-modal-overlay"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fade-in"
      dir="rtl"
    >
      <div
        id="multi-account-modal"
        className="w-full max-w-md bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-200 dark:border-slate-800 overflow-hidden flex flex-col p-6 select-none max-h-[90vh]"
      >
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800">
          <div className="flex items-center gap-2.5 text-sky-500 font-bold text-base">
            <div className="w-8 h-8 rounded-full bg-sky-100 dark:bg-sky-950 flex items-center justify-center">
              <Users className="w-5 h-5 text-sky-500" />
            </div>
            <div>
              <h3 className="text-slate-800 dark:text-white">إدارة وتعدد الحسابات (Multi-Account)</h3>
              <p className="text-[11px] text-slate-400 font-normal">تبديل فوري بين أكثر من حساب تيليجرام</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Account List */}
        <div className="mt-4 space-y-2.5 overflow-y-auto flex-1 max-h-60 p-1">
          {accounts.map((acc) => {
            const avatarColor = getAvatarColor(acc.user.firstName || "Account");
            return (
              <div
                key={acc.id}
                onClick={() => handleSelectAccount(acc.id)}
                className={`p-3 rounded-2xl flex items-center justify-between border cursor-pointer transition-all ${
                  acc.isActive
                    ? "bg-sky-50 dark:bg-sky-950/40 border-sky-400 shadow-xs"
                    : "bg-slate-50 dark:bg-slate-800/40 border-slate-200/80 dark:border-slate-800 hover:border-slate-300"
                }`}
              >
                <div className="flex items-center gap-3 min-w-0">
                  <div
                    className={`w-11 h-11 rounded-full bg-gradient-to-tr ${avatarColor} flex items-center justify-center text-white font-bold text-sm shadow-sm shrink-0`}
                  >
                    {acc.user.firstName.substring(0, 2).toUpperCase()}
                  </div>
                  <div className="min-w-0">
                    <div className="flex items-center gap-1.5 font-bold text-xs text-slate-800 dark:text-white truncate">
                      <span>{acc.user.firstName} {acc.user.lastName || ""}</span>
                      {acc.isActive && (
                        <span className="px-1.5 py-0.2 text-[9px] bg-sky-500 text-white rounded-full font-semibold">
                          الحساب النشط
                        </span>
                      )}
                    </div>
                    <span className="text-[11px] text-slate-400 block truncate" dir="ltr">
                      {acc.user.phone || "@" + (acc.user.username || "user")}
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-1">
                  {acc.isActive ? (
                    <div className="w-7 h-7 rounded-full bg-sky-500 text-white flex items-center justify-center shadow-xs">
                      <Check className="w-4 h-4" />
                    </div>
                  ) : (
                    <button
                      onClick={(e) => handleRemoveAccount(e, acc.id)}
                      className="w-7 h-7 rounded-full text-slate-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-950/40 flex items-center justify-center transition-colors"
                      title="حذف هذا الحساب"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>

        {/* Quick Add Form or Auth trigger */}
        {showQuickForm ? (
          <form onSubmit={handleAddQuickAccount} className="mt-4 p-4 bg-slate-50 dark:bg-slate-800/60 rounded-2xl border border-slate-200 dark:border-slate-700 space-y-3">
            <h4 className="text-xs font-bold text-slate-800 dark:text-white">إضافة حساب جديد سريع</h4>
            <div>
              <label className="text-[10px] font-semibold text-slate-500 block mb-1">اسم صاحب الحساب:</label>
              <input
                type="text"
                value={quickName}
                onChange={(e) => setQuickName(e.target.value)}
                placeholder="مثال: أحمد المحمدي (حساب العمل)"
                className="w-full px-3 py-2 bg-white dark:bg-slate-900 rounded-xl text-xs border border-slate-200 dark:border-slate-700 focus:outline-none focus:ring-2 focus:ring-sky-500"
                required
              />
            </div>
            <div>
              <label className="text-[10px] font-semibold text-slate-500 block mb-1">رقم الهاتف:</label>
              <input
                type="text"
                value={quickPhone}
                onChange={(e) => setQuickPhone(e.target.value)}
                placeholder="+966 50 000 0000"
                className="w-full px-3 py-2 bg-white dark:bg-slate-900 rounded-xl text-xs border border-slate-200 dark:border-slate-700 focus:outline-none focus:ring-2 focus:ring-sky-500 dir-ltr text-left"
              />
            </div>
            <div>
              <label className="text-[10px] font-semibold text-slate-500 block mb-1">StringSession (اختياري):</label>
              <input
                type="text"
                value={quickStringSession}
                onChange={(e) => setQuickStringSession(e.target.value)}
                placeholder="1BVtsOHQBu0x..."
                className="w-full px-3 py-2 bg-white dark:bg-slate-900 rounded-xl text-xs border border-slate-200 dark:border-slate-700 focus:outline-none focus:ring-2 focus:ring-sky-500 dir-ltr text-left"
              />
            </div>
            <div className="flex gap-2 pt-1">
              <button
                type="submit"
                className="flex-1 py-2 bg-sky-500 hover:bg-sky-600 text-white font-bold text-xs rounded-xl shadow-xs"
              >
                إضافة وتبديل
              </button>
              <button
                type="button"
                onClick={() => setShowQuickForm(false)}
                className="px-3 py-2 bg-slate-200 dark:bg-slate-700 text-slate-600 dark:text-slate-300 font-bold text-xs rounded-xl"
              >
                إلغاء
              </button>
            </div>
          </form>
        ) : (
          <div className="mt-4 flex gap-2">
            <button
              onClick={() => setShowQuickForm(true)}
              className="flex-1 py-2.5 bg-sky-50 dark:bg-sky-950/50 hover:bg-sky-100 text-sky-600 dark:text-sky-400 font-bold text-xs rounded-2xl border border-sky-200 dark:border-sky-800 transition-colors flex items-center justify-center gap-2"
            >
              <Plus className="w-4 h-4" />
              <span>إضافة حساب سريع</span>
            </button>
            <button
              onClick={() => {
                onClose();
                onOpenAddAccountAuth();
              }}
              className="flex-1 py-2.5 bg-sky-500 hover:bg-sky-600 text-white font-bold text-xs rounded-2xl shadow-sm transition-all flex items-center justify-center gap-2"
            >
              <Key className="w-4 h-4" />
              <span>تسجيل رقم رسمي (MTProto)</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
