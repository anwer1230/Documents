import React, { useState, useEffect } from 'react';
import {
  X,
  User,
  Moon,
  Sun,
  Globe,
  Palette,
  Shield,
  LogOut,
  Bookmark,
  Users,
  Check,
  Edit2,
  Lock,
  Cpu,
  Save,
  ChevronDown,
  UserPlus,
  Trash2,
  CheckCircle2,
  Bell,
  Send,
  AlertCircle,
  Loader2,
} from 'lucide-react';
import { TelegramUser, TelegramThemeConfig, TelegramAccount } from '../types';
import {
  subscribeToWebPush,
  unsubscribeFromWebPush,
  sendTestWebPush,
  getPushSubscription,
} from '../utils/pushNotifications';

interface SettingsDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  currentUser: TelegramUser;
  onUpdateUser: (updated: Partial<TelegramUser>) => void;
  themeConfig: TelegramThemeConfig;
  onUpdateTheme: (theme: Partial<TelegramThemeConfig>) => void;
  onLogout: () => void;
  onOpenSavedMessages: () => void;
  onOpenContacts: () => void;
  accounts?: TelegramAccount[];
  activeAccountId?: string;
  onSwitchAccount?: (accountId: string) => void;
  onOpenAddAccount?: () => void;
  onRemoveAccount?: (accountId: string) => void;
}

const ACCENT_COLORS = [
  { name: 'Classic Blue', hex: '#3390ec' },
  { name: 'Emerald Green', hex: '#10b981' },
  { name: 'Deep Violet', hex: '#8b5cf6' },
  { name: 'Warm Amber', hex: '#f59e0b' },
  { name: 'Rose Red', hex: '#f43f5e' },
  { name: 'Cyan Blue', hex: '#06b6d4' },
];

export const SettingsDrawer: React.FC<SettingsDrawerProps> = ({
  isOpen,
  onClose,
  currentUser,
  onUpdateUser,
  themeConfig,
  onUpdateTheme,
  onLogout,
  onOpenSavedMessages,
  onOpenContacts,
  accounts = [],
  activeAccountId,
  onSwitchAccount,
  onOpenAddAccount,
  onRemoveAccount,
}) => {
  const isAr = themeConfig.language === 'ar';
  const [activeSection, setActiveSection] = useState<'menu' | 'edit-profile' | 'settings' | 'api-info'>('menu');
  const [showAccountsList, setShowAccountsList] = useState(true);

  // Edit profile state
  const [firstName, setFirstName] = useState(currentUser.firstName);
  const [lastName, setLastName] = useState(currentUser.lastName || '');
  const [bio, setBio] = useState(currentUser.bio || '');
  const [username, setUsername] = useState(currentUser.username || '');

  // Web Push Notifications state
  const [pushEnabled, setPushEnabled] = useState(false);
  const [pushLoading, setPushLoading] = useState(false);
  const [pushStatusText, setPushStatusText] = useState('');

  useEffect(() => {
    // Check current push subscription status
    getPushSubscription().then((sub) => {
      if (sub && Notification.permission === 'granted') {
        setPushEnabled(true);
      }
    });
  }, []);

  const handleTogglePush = async () => {
    setPushLoading(true);
    setPushStatusText('');
    if (pushEnabled) {
      await unsubscribeFromWebPush();
      setPushEnabled(false);
      setPushStatusText(isAr ? 'تم تعطيل إشعارات الويب' : 'Web Push disabled');
    } else {
      const res = await subscribeToWebPush();
      if (res.success) {
        setPushEnabled(true);
        setPushStatusText(isAr ? 'تم تفعيل إشعارات VAPID بنجاح! 🎉' : 'Web Push enabled! 🎉');
      } else {
        setPushStatusText(res.error || (isAr ? 'فشل تفعيل الإشعارات' : 'Failed to enable push'));
      }
    }
    setPushLoading(false);
  };

  const handleTestPush = async () => {
    setPushLoading(true);
    setPushStatusText(isAr ? 'جارٍ إرسال إشعار تجريبي...' : 'Sending test push...');
    const res = await sendTestWebPush();
    if (res.success) {
      setPushStatusText(isAr ? 'تم إرسال الإشعار بنجاح! تحقق من جهازك' : 'Test push sent successfully!');
    } else {
      setPushStatusText(res.error || (isAr ? 'فشل إرسال الإشعار التجريبي' : 'Failed to send test push'));
    }
    setPushLoading(false);
  };

  if (!isOpen) return null;

  const handleSaveProfile = (e: React.FormEvent) => {
    e.preventDefault();
    onUpdateUser({
      firstName,
      lastName,
      bio,
      username,
    });
    setActiveSection('menu');
  };

  return (
    <div className="fixed inset-0 z-50 flex select-none">
      {/* Backdrop */}
      <div
        onClick={onClose}
        className="fixed inset-0 bg-black/50 backdrop-blur-xs transition-opacity"
      />

      {/* Drawer */}
      <aside
        className={`relative w-84 sm:w-96 h-full shadow-2xl flex flex-col z-10 transition-transform ${
          themeConfig.isDark ? 'bg-[#17212b] text-white' : 'bg-white text-gray-900'
        }`}
      >
        {/* Drawer Header with User Card */}
        <div className="p-4 bg-gradient-to-b from-[#2b5278] to-[#17212b] text-white relative">
          <button
            onClick={onClose}
            className="absolute top-4 end-4 p-1.5 rounded-full hover:bg-white/10 text-white transition"
          >
            <X className="w-5 h-5" />
          </button>

          <div className="flex items-center gap-3 mt-2">
            {currentUser.photoUrl ? (
              <img
                src={currentUser.photoUrl}
                alt={currentUser.firstName}
                referrerPolicy="no-referrer"
                className="w-14 h-14 rounded-full object-cover border-2 border-white/40 shadow-md"
              />
            ) : (
              <div className="w-14 h-14 rounded-full bg-[#3390ec] flex items-center justify-center font-bold text-xl border-2 border-white/40 shadow-md">
                {currentUser.firstName.slice(0, 2)}
              </div>
            )}

            <div className="min-w-0 flex-1">
              <h3 className="font-bold text-base truncate">
                {currentUser.firstName} {currentUser.lastName || ''}
              </h3>
              {currentUser.phone && (
                <p className="text-xs text-blue-200 font-mono" dir="ltr">{currentUser.phone}</p>
              )}
              {currentUser.username && (
                <p className="text-xs text-blue-300 font-mono">@{currentUser.username}</p>
              )}
            </div>

            {/* Account Switcher Expand Button */}
            <button
              onClick={() => setShowAccountsList(!showAccountsList)}
              className="p-2 rounded-full hover:bg-white/15 text-white transition self-center"
              title={isAr ? 'تبديل الحسابات' : 'Switch accounts'}
            >
              <ChevronDown className={`w-5 h-5 transition-transform ${showAccountsList ? 'rotate-180' : ''}`} />
            </button>
          </div>
        </div>

        {/* Multi-Account Drawer / List Section */}
        {showAccountsList && activeSection === 'menu' && (
          <div className={`p-2 border-b ${themeConfig.isDark ? 'bg-[#1b2734] border-[#232e3c]' : 'bg-gray-50 border-gray-200'}`}>
            <div className="flex items-center justify-between px-2 py-1 mb-1">
              <span className="text-xs font-bold text-[#3390ec] uppercase tracking-wider flex items-center gap-1.5">
                <Users className="w-3.5 h-3.5" />
                {isAr ? 'الحسابات المتصلة' : 'Accounts'} ({accounts.length} / 6)
              </span>
              {accounts.length < 6 && onOpenAddAccount && (
                <button
                  onClick={() => {
                    onOpenAddAccount();
                    onClose();
                  }}
                  className="text-xs text-[#3390ec] hover:underline font-semibold flex items-center gap-1"
                >
                  <UserPlus className="w-3.5 h-3.5" />
                  {isAr ? 'إضافة حساب' : 'Add'}
                </button>
              )}
            </div>

            <div className="space-y-1">
              {accounts.map((acc) => {
                const isActive = acc.id === activeAccountId || acc.user.id === currentUser.id;
                return (
                  <div
                    key={acc.id}
                    className={`flex items-center justify-between px-2.5 py-2 rounded-xl transition ${
                      isActive
                        ? themeConfig.isDark
                          ? 'bg-[#2b5278] text-white shadow-xs'
                          : 'bg-[#3390ec] text-white shadow-xs'
                        : themeConfig.isDark
                        ? 'hover:bg-[#232e3c] text-gray-200'
                        : 'hover:bg-gray-200/70 text-gray-800'
                    }`}
                  >
                    <button
                      onClick={() => {
                        if (!isActive && onSwitchAccount) {
                          onSwitchAccount(acc.id);
                          onClose();
                        }
                      }}
                      className="flex items-center gap-2.5 flex-1 min-w-0 text-start"
                    >
                      <div className="w-8 h-8 rounded-full bg-[#3390ec] flex items-center justify-center text-white text-xs font-bold shrink-0 shadow-xs border border-white/20">
                        {acc.user.firstName.slice(0, 2)}
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="text-xs font-bold truncate">
                          {acc.user.firstName} {acc.user.lastName || ''}
                        </div>
                        <div className={`text-[10px] truncate ${isActive ? 'text-blue-100' : 'text-gray-400'}`}>
                          {acc.user.phone || (acc.user.username ? `@${acc.user.username}` : (acc.isDemo ? (isAr ? 'حساب تجريبي' : 'Demo') : (isAr ? 'حساب سحابي' : 'Cloud')))}
                        </div>
                      </div>
                    </button>

                    <div className="flex items-center gap-1.5 shrink-0">
                      {isActive ? (
                        <span className="w-2 h-2 rounded-full bg-emerald-400 shadow-xs" title={isAr ? 'الحساب النشط' : 'Active'} />
                      ) : (
                        accounts.length > 1 && onRemoveAccount && (
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              if (confirm(isAr ? 'هل تريد تسجيل الخروج من هذا الحساب؟' : 'Log out from this account?')) {
                                onRemoveAccount(acc.id);
                              }
                            }}
                            className="p-1 rounded-full text-gray-400 hover:text-red-400 hover:bg-red-500/10 transition"
                            title={isAr ? 'إزالة الحساب' : 'Remove account'}
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        )
                      )}
                    </div>
                  </div>
                );
              })}

              {/* Add Account Row Button */}
              {accounts.length < 6 && onOpenAddAccount && (
                <button
                  onClick={() => {
                    onOpenAddAccount();
                    onClose();
                  }}
                  className={`w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-bold text-[#3390ec] transition ${
                    themeConfig.isDark ? 'hover:bg-[#232e3c]' : 'hover:bg-gray-100'
                  }`}
                >
                  <div className="w-8 h-8 rounded-full border border-dashed border-[#3390ec] flex items-center justify-center">
                    <UserPlus className="w-4 h-4" />
                  </div>
                  <span>{isAr ? 'إضافة حساب آخر (حتى 6 حسابات)' : 'Add Another Account (up to 6)'}</span>
                </button>
              )}
            </div>
          </div>
        )}

        {/* Content Navigation Area */}
        <div className="flex-1 overflow-y-auto p-3 space-y-1">
          {activeSection === 'menu' && (
            <>
              {/* Quick Actions */}
              <button
                onClick={() => {
                  onOpenSavedMessages();
                  onClose();
                }}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-semibold transition ${
                  themeConfig.isDark ? 'hover:bg-[#232e3c]' : 'hover:bg-gray-100'
                }`}
              >
                <Bookmark className="w-5 h-5 text-[#3390ec]" />
                <span>{isAr ? 'الرسائل المحفوظة' : 'Saved Messages'}</span>
              </button>

              <button
                onClick={() => {
                  onOpenContacts();
                  onClose();
                }}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-semibold transition ${
                  themeConfig.isDark ? 'hover:bg-[#232e3c]' : 'hover:bg-gray-100'
                }`}
              >
                <Users className="w-5 h-5 text-emerald-400" />
                <span>{isAr ? 'جهات الاتصال' : 'Contacts'}</span>
              </button>

              <button
                onClick={() => setActiveSection('edit-profile')}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-semibold transition ${
                  themeConfig.isDark ? 'hover:bg-[#232e3c]' : 'hover:bg-gray-100'
                }`}
              >
                <Edit2 className="w-5 h-5 text-purple-400" />
                <span>{isAr ? 'تعديل الملف الشخصي' : 'Edit Profile'}</span>
              </button>

              <button
                onClick={() => setActiveSection('settings')}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-semibold transition ${
                  themeConfig.isDark ? 'hover:bg-[#232e3c]' : 'hover:bg-gray-100'
                }`}
              >
                <Palette className="w-5 h-5 text-amber-400" />
                <span>{isAr ? 'الإعدادات والمظهر' : 'Settings & Appearance'}</span>
              </button>

              <button
                onClick={() => setActiveSection('api-info')}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-semibold transition ${
                  themeConfig.isDark ? 'hover:bg-[#232e3c]' : 'hover:bg-gray-100'
                }`}
              >
                <Cpu className="w-5 h-5 text-cyan-400" />
                <span>{isAr ? 'معلومات MTProto API الثابتة' : 'Telegram MTProto Info'}</span>
              </button>

              <div
                className={`my-2 border-b ${
                  themeConfig.isDark ? 'border-[#232e3c]' : 'border-gray-200'
                }`}
              />

              {/* Quick Night Mode Switch */}
              <div
                className={`flex items-center justify-between px-4 py-3 rounded-xl ${
                  themeConfig.isDark ? 'hover:bg-[#232e3c]' : 'hover:bg-gray-100'
                }`}
              >
                <div className="flex items-center gap-3 text-sm font-semibold">
                  {themeConfig.isDark ? (
                    <Moon className="w-5 h-5 text-blue-400" />
                  ) : (
                    <Sun className="w-5 h-5 text-amber-500" />
                  )}
                  <span>{isAr ? 'الوضع الليلي' : 'Night Mode'}</span>
                </div>
                <button
                  onClick={() => onUpdateTheme({ isDark: !themeConfig.isDark })}
                  className={`w-10 h-6 rounded-full transition-colors relative ${
                    themeConfig.isDark ? 'bg-[#3390ec]' : 'bg-gray-400'
                  }`}
                >
                  <span
                    className={`w-4 h-4 rounded-full bg-white absolute top-1 transition-transform ${
                      themeConfig.isDark ? 'start-5' : 'start-1'
                    }`}
                  />
                </button>
              </div>

              {/* Logout Button */}
              <button
                onClick={() => {
                  onLogout();
                  onClose();
                }}
                className="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-semibold text-red-400 hover:bg-red-500/10 transition mt-4"
              >
                <LogOut className="w-5 h-5" />
                <span>{isAr ? 'تسجيل الخروج' : 'Log Out'}</span>
              </button>
            </>
          )}

          {/* Edit Profile Sub-view */}
          {activeSection === 'edit-profile' && (
            <form onSubmit={handleSaveProfile} className="space-y-4 p-2">
              <div className="flex items-center justify-between mb-2">
                <h4 className="font-bold text-sm">{isAr ? 'تعديل الملف الشخصي' : 'Edit Profile'}</h4>
                <button
                  type="button"
                  onClick={() => setActiveSection('menu')}
                  className="text-xs text-[#3390ec]"
                >
                  {isAr ? 'رجوع' : 'Back'}
                </button>
              </div>

              <div>
                <label className="block text-xs text-gray-400 mb-1">{isAr ? 'الاسم الأول' : 'First Name'}</label>
                <input
                  type="text"
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  className={`w-full text-sm rounded-xl px-3 py-2 border focus:outline-none focus:border-[#3390ec] ${
                    themeConfig.isDark ? 'bg-[#242f3d] border-[#2f3f50]' : 'bg-gray-100 border-gray-300'
                  }`}
                  required
                />
              </div>

              <div>
                <label className="block text-xs text-gray-400 mb-1">{isAr ? 'اسم العائلة' : 'Last Name'}</label>
                <input
                  type="text"
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  className={`w-full text-sm rounded-xl px-3 py-2 border focus:outline-none focus:border-[#3390ec] ${
                    themeConfig.isDark ? 'bg-[#242f3d] border-[#2f3f50]' : 'bg-gray-100 border-gray-300'
                  }`}
                />
              </div>

              <div>
                <label className="block text-xs text-gray-400 mb-1">{isAr ? 'اسم المستخدم' : 'Username'}</label>
                <div className="flex items-center">
                  <span className="text-gray-400 text-sm pe-1">@</span>
                  <input
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    className={`w-full text-sm rounded-xl px-3 py-2 border focus:outline-none focus:border-[#3390ec] font-mono ${
                      themeConfig.isDark ? 'bg-[#242f3d] border-[#2f3f50]' : 'bg-gray-100 border-gray-300'
                    }`}
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs text-gray-400 mb-1">{isAr ? 'نبذة تعريفية (Bio)' : 'Bio'}</label>
                <textarea
                  rows={3}
                  value={bio}
                  onChange={(e) => setBio(e.target.value)}
                  className={`w-full text-sm rounded-xl px-3 py-2 border focus:outline-none focus:border-[#3390ec] resize-none ${
                    themeConfig.isDark ? 'bg-[#242f3d] border-[#2f3f50]' : 'bg-gray-100 border-gray-300'
                  }`}
                />
              </div>

              <button
                type="submit"
                className="w-full bg-[#3390ec] hover:bg-[#2b7ec9] text-white text-sm font-semibold py-2.5 rounded-xl transition flex items-center justify-center gap-2 shadow"
              >
                <Save className="w-4 h-4" />
                <span>{isAr ? 'حفظ التغييرات' : 'Save Changes'}</span>
              </button>
            </form>
          )}

          {/* Settings & Appearance */}
          {activeSection === 'settings' && (
            <div className="space-y-4 p-2">
              <div className="flex items-center justify-between mb-2">
                <h4 className="font-bold text-sm">{isAr ? 'المظهر والإعدادات' : 'Appearance & Settings'}</h4>
                <button
                  type="button"
                  onClick={() => setActiveSection('menu')}
                  className="text-xs text-[#3390ec]"
                >
                  {isAr ? 'رجوع' : 'Back'}
                </button>
              </div>

              {/* Language Switch */}
              <div>
                <label className="block text-xs text-gray-400 mb-2 flex items-center gap-1.5">
                  <Globe className="w-4 h-4 text-[#3390ec]" />
                  <span>{isAr ? 'لغة التطبيق' : 'Language'}</span>
                </label>
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <button
                    type="button"
                    onClick={() => onUpdateTheme({ language: 'ar' })}
                    className={`py-2 px-3 rounded-xl font-bold border transition ${
                      themeConfig.language === 'ar'
                        ? 'bg-[#3390ec] text-white border-[#3390ec]'
                        : themeConfig.isDark
                        ? 'border-[#2f3f50] text-gray-300 hover:bg-[#242f3d]'
                        : 'border-gray-200 text-gray-700 hover:bg-gray-100'
                    }`}
                  >
                    العربية (Arabic)
                  </button>
                  <button
                    type="button"
                    onClick={() => onUpdateTheme({ language: 'en' })}
                    className={`py-2 px-3 rounded-xl font-bold border transition ${
                      themeConfig.language === 'en'
                        ? 'bg-[#3390ec] text-white border-[#3390ec]'
                        : themeConfig.isDark
                        ? 'border-[#2f3f50] text-gray-300 hover:bg-[#242f3d]'
                        : 'border-gray-200 text-gray-700 hover:bg-gray-100'
                    }`}
                  >
                    English
                  </button>
                </div>
              </div>

              {/* Accent Colors */}
              <div>
                <label className="block text-xs text-gray-400 mb-2 flex items-center gap-1.5">
                  <Palette className="w-4 h-4 text-purple-400" />
                  <span>{isAr ? 'لون الثيم المميز' : 'Accent Color'}</span>
                </label>
                <div className="grid grid-cols-6 gap-2">
                  {ACCENT_COLORS.map((c) => (
                    <button
                      key={c.hex}
                      onClick={() => onUpdateTheme({ accentColor: c.hex })}
                      className="w-10 h-10 rounded-full flex items-center justify-center transition hover:scale-110 shadow-sm"
                      style={{ backgroundColor: c.hex }}
                      title={c.name}
                    >
                      {themeConfig.accentColor === c.hex && <Check className="w-4 h-4 text-white" />}
                    </button>
                  ))}
                </div>
              </div>

              {/* Web Push Notifications (VAPID) */}
              <div className={`p-3 rounded-xl border space-y-2 mt-4 ${
                themeConfig.isDark ? 'bg-[#242f3d]/70 border-[#2f3f50]' : 'bg-gray-50 border-gray-200'
              }`}>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Bell className="w-4 h-4 text-[#3390ec]" />
                    <span className="text-xs font-semibold">
                      {isAr ? 'إشعارات الويب (Web Push)' : 'Web Push Notifications'}
                    </span>
                  </div>
                  <button
                    type="button"
                    onClick={handleTogglePush}
                    disabled={pushLoading}
                    className={`w-10 h-6 rounded-full transition-colors relative ${
                      pushEnabled ? 'bg-[#3390ec]' : 'bg-gray-400'
                    }`}
                  >
                    <span
                      className={`w-4 h-4 rounded-full bg-white absolute top-1 transition-transform ${
                        pushEnabled ? 'start-5' : 'start-1'
                      }`}
                    />
                  </button>
                </div>

                <p className="text-[11px] text-gray-400 leading-normal">
                  {isAr
                    ? 'تفعيل إشعارات الدفع عبر مفاتيح VAPID المثبتة وخدمة Service Worker لتلقي الرسائل فور ورودها.'
                    : 'Receive real-time push alerts via fixed VAPID keys and Service Worker.'}
                </p>

                {pushStatusText && (
                  <div className={`text-[11px] p-2 rounded-lg ${
                    pushStatusText.includes('فشل') || pushStatusText.includes('Failed')
                      ? 'bg-red-500/10 text-red-400'
                      : 'bg-emerald-500/10 text-emerald-400'
                  }`}>
                    {pushStatusText}
                  </div>
                )}

                {pushEnabled && (
                  <button
                    type="button"
                    onClick={handleTestPush}
                    disabled={pushLoading}
                    className="w-full mt-2 py-1.5 px-3 rounded-lg bg-[#3390ec]/20 hover:bg-[#3390ec]/30 text-[#3390ec] text-xs font-medium flex items-center justify-center gap-1.5 transition"
                  >
                    {pushLoading ? (
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    ) : (
                      <Send className="w-3.5 h-3.5" />
                    )}
                    <span>{isAr ? 'إرسال إشعار تجريبي (Test Push)' : 'Send Test Notification'}</span>
                  </button>
                )}
              </div>
            </div>
          )}

          {/* Fixed API Info Sub-view */}
          {activeSection === 'api-info' && (
            <div className="space-y-4 p-2 text-xs">
              <div className="flex items-center justify-between mb-2">
                <h4 className="font-bold text-sm flex items-center gap-1.5">
                  <Shield className="w-4 h-4 text-emerald-400" />
                  <span>{isAr ? 'بيانات API الثابتة' : 'Fixed API Config'}</span>
                </h4>
                <button
                  type="button"
                  onClick={() => setActiveSection('menu')}
                  className="text-xs text-[#3390ec]"
                >
                  {isAr ? 'رجوع' : 'Back'}
                </button>
              </div>

              <div
                className={`p-3 rounded-xl border space-y-2 ${
                  themeConfig.isDark ? 'bg-[#242f3d]/70 border-[#2f3f50]' : 'bg-gray-50 border-gray-200'
                }`}
              >
                <div>
                  <span className="text-gray-400 block text-[11px]">TELEGRAM_API_ID (ثابت):</span>
                  <span className="font-mono font-bold text-emerald-400 text-sm">22043994</span>
                </div>

                <div>
                  <span className="text-gray-400 block text-[11px]">TELEGRAM_API_HASH (ثابت):</span>
                  <span className="font-mono text-gray-300 break-all text-[11px]">
                    56f64582b363d367280db96586b97801
                  </span>
                </div>
                <div>
                  <span className="text-gray-400 block text-[11px]">VAPID_PUBLIC_KEY (ثابت):</span>
                  <span className="font-mono text-emerald-400/90 break-all text-[10px]">
                    BE36BmheMRx2GxzjWpp_4bmXq_hZg55bP_M_vNVysfnjTxns9VCI0hiCHgnRBx0URe_LoxWaAgrS9G9QZbQhOh8
                  </span>
                </div>
                <div>
                  <span className="text-gray-400 block text-[11px]">VAPID_PRIVATE_KEY (ثابت):</span>
                  <span className="font-mono text-gray-300 break-all text-[10px]">
                    13NU1_GmeL7bDQcVtlFyuKqsnnsX3XkOyE--2rAQJw4
                  </span>
                </div>
                <div>
                  <span className="text-gray-400 block text-[11px]">VAPID_SUBJECT (ثابت):</span>
                  <span className="font-mono text-gray-300 break-all text-[10px]">
                    mailto:anwrfwad178@gmail.com
                  </span>
                </div>

                <div className="pt-2 border-t border-gray-700/20">
                  <span className="text-gray-400 block text-[11px]">{isAr ? 'حالة الربط:' : 'Connection Status:'}</span>
                  <span className="inline-flex items-center gap-1.5 text-emerald-400 font-semibold mt-1">
                    <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                    <span>{isAr ? 'متصل بخوادم تيليجرام MTProto' : 'Connected to Telegram MTProto'}</span>
                  </span>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Footer info */}
        <div className="p-4 border-t border-gray-700/20 text-center text-[11px] text-gray-400">
          Telegram Web K/Z • MTProto v2.0
        </div>
      </aside>
    </div>
  );
};
