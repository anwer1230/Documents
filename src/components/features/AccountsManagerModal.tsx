import { useState, useEffect, type FormEvent } from 'react';
import {
  Users,
  UserCheck,
  Plus,
  ArrowRightLeft,
  Check,
  ShieldCheck,
  Smartphone,
  X,
  RefreshCw,
  LogOut,
  KeyRound,
  ExternalLink,
  Lock
} from 'lucide-react';

type SystemAccount = {
  id: string;
  name: string;
  username: string;
  phone: string;
  role: string;
  status: 'connected' | 'ready' | 'offline';
  color: string;
  lastActive: string;
};

export function AccountsManagerModal({
  onClose,
  onOpenLoginFlow,
  onAccountSwitched,
}: {
  onClose: () => void;
  onOpenLoginFlow?: () => void;
  onAccountSwitched?: (acc: SystemAccount) => void;
}) {
  const [accounts, setAccounts] = useState<SystemAccount[]>([]);
  const [activeId, setActiveId] = useState<string>('acc_1');
  const [loading, setLoading] = useState(true);
  const [switchingId, setSwitchingId] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [showAddForm, setShowAddForm] = useState(false);
  const [newPhone, setNewPhone] = useState('');
  const [newName, setNewName] = useState('');
  const [newRole, setNewRole] = useState('نشر تلقائي ومراقبة');

  const fetchAccounts = () => {
    fetch('/api/telegram/accounts')
      .then(res => res.json())
      .then(res => {
        setAccounts(res.accounts || []);
        setActiveId(res.activeId || 'acc_1');
        setLoading(false);
      })
      .catch(() => {
        setLoading(false);
      });
  };

  useEffect(() => {
    fetchAccounts();
  }, []);

  const handleSwitchAccount = async (account: SystemAccount) => {
    if (account.id === activeId) return;
    setSwitchingId(account.id);
    try {
      const res = await fetch('/api/telegram/accounts/switch', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ accountId: account.id }),
      });
      const data = await res.json();
      if (data.success) {
        setActiveId(account.id);
        setFeedback(`تم التبديل بنجاح إلى حساب: ${account.name}`);
        onAccountSwitched?.(account);
        setTimeout(() => setFeedback(null), 3000);
      }
    } catch {
      setFeedback('تعذر التبديل بين الحسابات');
    } finally {
      setSwitchingId(null);
    }
  };

  const handleAddAccountSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!newPhone.trim()) return;

    try {
      const res = await fetch('/api/telegram/accounts/add', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          phone: newPhone.trim(),
          name: newName.trim() || `حساب ${newPhone.trim()}`,
          role: newRole,
        }),
      });
      const data = await res.json();
      if (data.success) {
        setFeedback('تمت إضافة الحساب وتفعيله في النظام الرئيسي');
        setShowAddForm(false);
        setNewPhone('');
        setNewName('');
        fetchAccounts();
        if (data.account) {
          onAccountSwitched?.(data.account);
        }
      }
    } catch {
      setFeedback('فشل إضافة الحساب');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-3 backdrop-blur-sm sm:p-5" dir="rtl">
      <div className="relative flex max-h-[92vh] w-full max-w-2xl flex-col overflow-hidden rounded-3xl border border-border bg-card shadow-2xl fade-up">
        {/* Header */}
        <div className="flex flex-none items-center justify-between border-b border-border bg-card/95 px-6 py-4 backdrop-blur">
          <div className="flex items-center gap-3">
            <div className="grid h-11 w-11 place-items-center rounded-2xl bg-primary/10 text-primary shadow-sm">
              <Users size={22} />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-base font-bold text-foreground">إدارة الحسابات وجلسات العمل</h2>
                <span className="rounded-full bg-emerald-500/15 px-2.5 py-0.5 text-[10px] font-bold text-emerald-600 dark:text-emerald-400">
                  متصل بجلسة Telegram
                </span>
              </div>
              <p className="mt-0.5 text-xs text-muted-foreground">
                التبديل الفوري بين حسابات النشر والمراقبة وربطها بنظام تسجيل الدخول
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="grid h-9 w-9 place-items-center rounded-xl text-muted-foreground transition hover:bg-muted hover:text-foreground cursor-pointer"
            aria-label="إغلاق"
          >
            <X size={18} />
          </button>
        </div>

        {/* Feedback Alert */}
        {feedback && (
          <div className="mx-6 mt-4 flex items-center gap-2 rounded-xl bg-primary/10 px-4 py-2.5 text-xs font-bold text-primary border border-primary/20">
            <UserCheck size={16} />
            <span>{feedback}</span>
          </div>
        )}

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-5 sm:p-6 space-y-5">
          {loading ? (
            <div className="flex h-56 items-center justify-center text-xs text-muted-foreground">
              <RefreshCw className="h-5 w-5 animate-spin text-primary ml-2" />
              جارٍ تحميل بيانات الحسابات والجلسات...
            </div>
          ) : (
            <>
              {/* Active Account Highlight Card */}
              {(() => {
                const current = accounts.find(a => a.id === activeId) || accounts[0];
                if (!current) return null;
                return (
                  <div className="rounded-2xl border border-primary/30 bg-primary/5 p-4 space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-[11px] font-bold text-primary flex items-center gap-1.5">
                        <ShieldCheck size={14} />
                        الحساب النشط حالياً في النظام
                      </span>
                      <span className="rounded-full bg-emerald-500/15 px-2 py-0.5 text-[10px] font-bold text-emerald-700 dark:text-emerald-300">
                        {current.lastActive}
                      </span>
                    </div>

                    <div className="flex items-center justify-between gap-3">
                      <div className="flex items-center gap-3">
                        <div className="grid h-12 w-12 place-items-center rounded-2xl bg-primary text-primary-foreground font-bold text-base shadow-sm">
                          {current.name.slice(0, 2)}
                        </div>
                        <div>
                          <h3 className="text-sm font-bold text-foreground">{current.name}</h3>
                          <div className="flex items-center gap-2 mt-0.5 text-xs text-muted-foreground">
                            <span className="font-mono" dir="ltr">{current.phone}</span>
                            <span>•</span>
                            <span className="text-primary font-medium">{current.role}</span>
                          </div>
                        </div>
                      </div>

                      <span className="inline-flex items-center gap-1 rounded-xl bg-primary/10 px-3 py-1.5 text-xs font-bold text-primary">
                        <Check size={14} />
                        نشط ومفعل
                      </span>
                    </div>
                  </div>
                );
              })()}

              {/* Accounts List for Instant Switching */}
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-foreground">الحسابات المتاحة للتبديل الفوري</span>
                  <span className="text-[11px] text-muted-foreground font-mono">
                    {accounts.length} حسابات مهيأة
                  </span>
                </div>

                <div className="space-y-2.5">
                  {accounts.map(acc => {
                    const isCurrent = acc.id === activeId;
                    const isSwitching = switchingId === acc.id;

                    return (
                      <div
                        key={acc.id}
                        className={`flex items-center justify-between rounded-2xl border p-3.5 transition ${
                          isCurrent
                            ? 'border-primary/40 bg-card shadow-xs'
                            : 'border-border bg-card/60 hover:border-primary/30 hover:bg-card'
                        }`}
                      >
                        <div className="flex items-center gap-3">
                          <div
                            className="grid h-10 w-10 place-items-center rounded-xl font-bold text-sm text-white shadow-xs"
                            style={{ backgroundColor: acc.color || '#377c79' }}
                          >
                            {acc.name.slice(0, 1)}
                          </div>
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="text-xs font-bold text-foreground">{acc.name}</span>
                              <span className="font-mono text-[10px] text-muted-foreground" dir="ltr">{acc.username}</span>
                            </div>
                            <div className="flex items-center gap-2 mt-0.5 text-[11px] text-muted-foreground">
                              <span className="font-mono" dir="ltr">{acc.phone}</span>
                              <span>•</span>
                              <span>{acc.role}</span>
                            </div>
                          </div>
                        </div>

                        <div className="flex items-center gap-2">
                          {isCurrent ? (
                            <span className="rounded-xl bg-secondary px-3 py-1.5 text-[11px] font-bold text-foreground">
                              الحساب الحالي
                            </span>
                          ) : (
                            <button
                              type="button"
                              onClick={() => handleSwitchAccount(acc)}
                              disabled={isSwitching}
                              className="inline-flex items-center gap-1.5 rounded-xl border border-border bg-secondary px-3.5 py-1.5 text-xs font-bold text-foreground transition hover:border-primary hover:text-primary disabled:opacity-50 cursor-pointer"
                            >
                              {isSwitching ? (
                                <RefreshCw className="h-3.5 w-3.5 animate-spin" />
                              ) : (
                                <ArrowRightLeft size={13} />
                              )}
                              <span>تبديل</span>
                            </button>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Login Interface Connection Box */}
              <div className="rounded-2xl border border-dashed border-border bg-muted/20 p-4 space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <KeyRound size={16} className="text-primary" />
                    <span className="text-xs font-bold text-foreground">ربط حساب جديد عبر واجهة تسجيل الدخول الرئيسية</span>
                  </div>
                  <button
                    type="button"
                    onClick={onOpenLoginFlow}
                    className="inline-flex items-center gap-1 rounded-xl bg-primary px-3 py-1.5 text-xs font-bold text-primary-foreground shadow-sm hover:brightness-105 cursor-pointer"
                  >
                    <span>فتح شاشة الدخول الرئيسية</span>
                    <ExternalLink size={12} />
                  </button>
                </div>
                <p className="text-[11px] text-muted-foreground leading-relaxed">
                  يمكنك تسجيل الدخول برقم هاتف جديد وتأكيد الرمز وكلمة المرور الإضافية 2FA وسيتم حفظ الجلسة وربطها تلقائياً بجميع أدوات النشر والمراقبة.
                </p>

                {/* Quick In-modal Add Toggle */}
                <div className="pt-1">
                  {!showAddForm ? (
                    <button
                      type="button"
                      onClick={() => setShowAddForm(true)}
                      className="text-[11px] font-bold text-primary hover:underline cursor-pointer flex items-center gap-1"
                    >
                      <Plus size={13} />
                      <span>إضافة حساب يدوي سريع داخل هذه اللوحة</span>
                    </button>
                  ) : (
                    <form onSubmit={handleAddAccountSubmit} className="space-y-3 pt-2">
                      <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                        <div>
                          <label className="text-[10px] text-muted-foreground block mb-1">اسم الحساب أو المعرف</label>
                          <input
                            type="text"
                            value={newName}
                            onChange={e => setNewName(e.target.value)}
                            placeholder="مثال: حساب التسويق رقم 2"
                            className="h-9 w-full rounded-xl border border-input bg-background px-3 text-xs outline-none focus:ring-2 focus:ring-primary/25"
                          />
                        </div>
                        <div>
                          <label className="text-[10px] text-muted-foreground block mb-1">رقم الهاتف الدولي</label>
                          <input
                            type="text"
                            value={newPhone}
                            onChange={e => setNewPhone(e.target.value)}
                            placeholder="+9665XXXXXXXX"
                            className="h-9 w-full rounded-xl border border-input bg-background px-3 text-xs outline-none focus:ring-2 focus:ring-primary/25"
                            dir="ltr"
                          />
                        </div>
                      </div>

                      <div className="flex items-center justify-end gap-2 pt-1">
                        <button
                          type="button"
                          onClick={() => setShowAddForm(false)}
                          className="rounded-xl px-3 py-1.5 text-xs text-muted-foreground hover:bg-muted cursor-pointer"
                        >
                          إلغاء
                        </button>
                        <button
                          type="submit"
                          disabled={!newPhone.trim()}
                          className="rounded-xl bg-primary px-4 py-1.5 text-xs font-bold text-primary-foreground disabled:opacity-40 cursor-pointer"
                        >
                          حفظ وتفعيل الحساب
                        </button>
                      </div>
                    </form>
                  )}
                </div>
              </div>
            </>
          )}
        </div>

        {/* Footer */}
        <div className="flex flex-none items-center justify-between border-t border-border bg-card/95 px-6 py-4 backdrop-blur">
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <Lock size={13} className="text-emerald-500" />
            <span>الجلسات مشفرة ومحفوظة محلياً وفق معايير أمان Telegram MTProto</span>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="rounded-xl border border-border bg-secondary px-5 py-2 text-xs font-medium text-foreground transition hover:bg-muted cursor-pointer"
          >
            إغلاق
          </button>
        </div>
      </div>
    </div>
  );
}
