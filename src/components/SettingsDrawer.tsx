import React, { useState, useEffect } from 'react';
import {
  X,
  User,
  Moon,
  Sun,
  Globe,
  Palette,
  Shield,
  ShieldCheck,
  LogOut,
  Bookmark,
  Users,
  Check,
  Edit2,
  Lock,
  Cpu,
  Save,
  ChevronDown,
  ChevronRight,
  UserPlus,
  Trash2,
  CheckCircle2,
  Bell,
  Send,
  AlertCircle,
  Loader2,
  ArrowLeft,
  HardDrive,
  Folder,
  Smartphone,
  Laptop,
  Sparkles,
  HelpCircle,
  Download,
  Radio,
  Camera,
  Key,
  RefreshCw,
  Volume2,
  VolumeX,
  Eye,
  EyeOff,
  QrCode,
  Archive,
  Sliders,
  MoreVertical,
} from 'lucide-react';
import { TelegramUser, TelegramThemeConfig, TelegramAccount, TelegramChat } from '../types';
import {
  subscribeToWebPush,
  unsubscribeFromWebPush,
  sendTestWebPush,
  getPushSubscription,
} from '../utils/pushNotifications';
import api, { Api } from '../services/api';
import { csrfFetch } from '../services/csrfFetch';
import { PrivacySettings } from './PrivacySettings';
import { PasswordStrengthIndicator } from './PasswordStrengthIndicator';
import { VAPID_SUBJECT } from '../config';

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
  chats?: TelegramChat[];
  onSelectChat?: (chatId: string) => void;
  onToggleArchive?: (chatId: string) => void;
}

type SettingsSection =
  | 'menu'
  | 'settings-root'
  | 'edit-profile'
  | 'chat-settings'
  | 'privacy-security'
  | 'two-step-verification'
  | 'notifications-sounds'
  | 'data-storage'
  | 'chat-folders'
  | 'language'
  | 'devices'
  | 'api-info';

const ACCENT_COLORS = [
  { name: 'Classic Blue', hex: '#3390ec' },
  { name: 'Emerald Green', hex: '#10b981' },
  { name: 'Deep Violet', hex: '#8b5cf6' },
  { name: 'Warm Amber', hex: '#f59e0b' },
  { name: 'Rose Red', hex: '#f43f5e' },
  { name: 'Cyan Blue', hex: '#06b6d4' },
];

const WALLPAPERS = [
  { id: 'default', name: 'Telegram Doodle', preview: 'bg-[#0e1621]' },
  { id: 'midnight', name: 'Midnight Dark', preview: 'bg-black' },
  { id: 'ocean', name: 'Sky Azure', preview: 'bg-sky-900' },
  { id: 'forest', name: 'Forest Green', preview: 'bg-emerald-950' },
  { id: 'violet', name: 'Sunset Violet', preview: 'bg-purple-950' },
  { id: 'sand', name: 'Desert Sand', preview: 'bg-amber-950' },
];

const LANGUAGES = [
  { code: 'ar', name: 'العربية (Arabic)', native: 'العربية' },
  { code: 'en', name: 'English', native: 'English' },
  { code: 'es', name: 'Español (Spanish)', native: 'Español' },
  { code: 'fr', name: 'Français (French)', native: 'Français' },
  { code: 'de', name: 'Deutsch (German)', native: 'Deutsch' },
  { code: 'ru', name: 'Русский (Russian)', native: 'Русский' },
  { code: 'tr', name: 'Türkçe (Turkish)', native: 'Türkçe' },
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
  chats = [],
  onSelectChat,
  onToggleArchive,
}) => {
  const isAr = themeConfig.language === 'ar';
  const [activeSection, setActiveSection] = useState<SettingsSection>('menu');
  const [showAccountsList, setShowAccountsList] = useState(false);

  // Edit profile state
  const [firstName, setFirstName] = useState(currentUser.firstName);
  const [lastName, setLastName] = useState(currentUser.lastName || '');
  const [bio, setBio] = useState(currentUser.bio || '');
  const [username, setUsername] = useState(currentUser.username || '');
  const [photoUrl, setPhotoUrl] = useState(currentUser.photoUrl || '');
  const [profileSavedToast, setProfileSavedToast] = useState(false);

  // Chat settings state
  const [fontSizeSlider, setFontSizeSlider] = useState<number>(
    themeConfig.fontSize === 'sm' ? 14 : themeConfig.fontSize === 'lg' ? 18 : 16
  );
  const [selectedWallpaper, setSelectedWallpaper] = useState(themeConfig.wallpaper || 'default');
  const [sendOnEnter, setSendOnEnter] = useState(themeConfig.sendOnEnter ?? true);
  const [animationsEnabled, setAnimationsEnabled] = useState(themeConfig.animations ?? true);
  const [autoplayMedia, setAutoplayMedia] = useState(true);

  // Notifications state
  const [notifyPrivate, setNotifyPrivate] = useState(true);
  const [notifyGroups, setNotifyGroups] = useState(true);
  const [notifyChannels, setNotifyChannels] = useState(true);
  const [soundEnabled, setSoundEnabled] = useState(true);

  // Web Push state
  const [pushEnabled, setPushEnabled] = useState(false);
  const [pushLoading, setPushLoading] = useState(false);
  const [pushStatusText, setPushStatusText] = useState('');

  // Privacy state (Synced with Telegram Cloud)
  const [phonePrivacy, setPhonePrivacy] = useState<'everybody' | 'contacts' | 'nobody'>('contacts');
  const [lastSeenPrivacy, setLastSeenPrivacy] = useState<'everybody' | 'contacts' | 'nobody'>('everybody');
  const [forwardsPrivacy, setForwardsPrivacy] = useState<'everybody' | 'contacts' | 'nobody'>('everybody');
  const [twoStepPassword, setTwoStepPassword] = useState('');
  const [show2faPassword, setShow2faPassword] = useState(false);
  const [twoStepEnabled, setTwoStepEnabled] = useState(false);
  const [twoStepHint, setTwoStepHint] = useState('');
  const [twoStepDetails, setTwoStepDetails] = useState<{
    hasRecovery?: boolean;
    loginEmailPattern?: string;
    emailUnconfirmedPattern?: string;
    pendingResetDate?: number;
  }>({});
  const [privacyLoading, setPrivacyLoading] = useState(false);
  const [twoStepLoading, setTwoStepLoading] = useState(false);
  const [newTwoStepHint, setNewTwoStepHint] = useState('');
  const [isSaving2FA, setIsSaving2FA] = useState(false);
  const [twoStepSuccessMsg, setTwoStepSuccessMsg] = useState<string | null>(null);
  const [privacySyncSuccess, setPrivacySyncSuccess] = useState<string | null>(null);
  const [privacySyncError, setPrivacySyncError] = useState<string | null>(null);
  const [updatingPrivacyKey, setUpdatingPrivacyKey] = useState<string | null>(null);
  const [passcodeLock, setPasscodeLock] = useState(false);

  // Devices & Authorizations State (MTProto account.getAuthorizations)
  const [authorizations, setAuthorizations] = useState<any[]>([]);
  const [authLoading, setAuthLoading] = useState(false);
  const [terminatingHash, setTerminatingHash] = useState<string | null>(null);
  const [isTerminatingAll, setIsTerminatingAll] = useState(false);
  const [authSuccessMsg, setAuthSuccessMsg] = useState<string | null>(null);
  const [authErrorMsg, setAuthErrorMsg] = useState<string | null>(null);
  const [authTtlDays, setAuthTtlDays] = useState<number>(180);

  // Storage state
  const [cacheSize, setCacheSize] = useState('67.9 MB');
  const [isClearingCache, setIsClearingCache] = useState(false);
  const [cacheCleared, setCacheCleared] = useState(false);

  // Modals state
  const [showArchivedModal, setShowArchivedModal] = useState(false);
  const [showFeaturesModal, setShowFeaturesModal] = useState(false);
  const [showBugModal, setShowBugModal] = useState(false);
  const [showLogoutModal, setShowLogoutModal] = useState(false);
  const [showLinkQrModal, setShowLinkQrModal] = useState(false);
  const [showThreeDotsMenu, setShowThreeDotsMenu] = useState(false);
  const [bugText, setBugText] = useState('');
  const [bugSent, setBugSent] = useState(false);

  // Folders custom state
  const [folderList, setFolderList] = useState([
    { id: 'all', name: isAr ? 'كل المحادثات' : 'All Chats', count: chats.length },
    { id: 'personal', name: isAr ? 'المحادثات الخاصة' : 'Personal', count: chats.filter((c) => c.type === 'private').length },
    { id: 'channels', name: isAr ? 'القنوات' : 'Channels', count: chats.filter((c) => c.type === 'channel').length },
    { id: 'groups', name: isAr ? 'المجموعات' : 'Groups', count: chats.filter((c) => c.type === 'group' || c.type === 'supergroup').length },
    { id: 'bots', name: isAr ? 'البوتات' : 'Bots', count: chats.filter((c) => c.type === 'bot').length },
  ]);
  const [newFolderName, setNewFolderName] = useState('');
  const [showNewFolderModal, setShowNewFolderModal] = useState(false);

  useEffect(() => {
    getPushSubscription().then((sub) => {
      if (sub && Notification.permission === 'granted') {
        setPushEnabled(true);
      }
    });
  }, []);

  useEffect(() => {
    setFirstName(currentUser.firstName);
    setLastName(currentUser.lastName || '');
    setBio(currentUser.bio || '');
    setUsername(currentUser.username || '');
    setPhotoUrl(currentUser.photoUrl || '');
  }, [currentUser]);

  // Synchronize Privacy and 2FA status from Telegram Cloud
  const fetchCloudPrivacy = async () => {
    setPrivacyLoading(true);
    setPrivacySyncError(null);
    try {
      // 1. Phone number privacy (Api.account.GetPrivacy)
      const phoneRes: any = await api.privacy.getPrivacy('phoneNumber');
      if (phoneRes?.option) setPhonePrivacy(phoneRes.option);
      else if (phoneRes?.rule) setPhonePrivacy(phoneRes.rule);

      // 2. Last seen privacy (Api.account.GetPrivacy)
      const lastSeenRes: any = await api.privacy.getPrivacy('statusTimestamp');
      if (lastSeenRes?.option) setLastSeenPrivacy(lastSeenRes.option);
      else if (lastSeenRes?.rule) setLastSeenPrivacy(lastSeenRes.rule);

      // 3. Forwarded messages privacy (Api.account.GetPrivacy)
      const forwardsRes: any = await api.privacy.getPrivacy('forwards');
      if (forwardsRes?.option) setForwardsPrivacy(forwardsRes.option);
      else if (forwardsRes?.rule) setForwardsPrivacy(forwardsRes.rule);
    } catch (err: any) {
      console.warn('[SettingsDrawer] Failed to fetch cloud privacy:', err);
    } finally {
      setPrivacyLoading(false);
    }
  };

  const fetchCloud2FA = async () => {
    setTwoStepLoading(true);
    try {
      const res = await csrfFetch('/api/account/2fa/get');
      const data = await res.json();
      if (data) {
        setTwoStepEnabled(Boolean(data.hasPassword));
        setTwoStepHint(data.hint || '');
        setNewTwoStepHint(data.hint || '');
        setTwoStepDetails({
          hasRecovery: Boolean(data.hasRecovery),
          loginEmailPattern: data.loginEmailPattern || data.emailUnconfirmedPattern || '',
          emailUnconfirmedPattern: data.emailUnconfirmedPattern || '',
          pendingResetDate: data.pendingResetDate,
        });
      }
    } catch (err: any) {
      console.warn('[SettingsDrawer] Failed to fetch cloud 2FA password:', err);
    } finally {
      setTwoStepLoading(false);
    }
  };

  const fetchAuthorizations = async () => {
    setAuthLoading(true);
    setAuthErrorMsg(null);
    try {
      const res = await fetch('/api/telegram/account/authorizations', {
        headers: { 'x-session-token': activeAccountId || 'guest' },
      });
      const data = await res.json();
      let list: any[] = [];
      if (Array.isArray(data?.authorizations)) {
        list = data.authorizations;
      } else if (Array.isArray(data)) {
        list = data;
      } else if (data?.authorizations && Array.isArray(data.authorizations.authorizations)) {
        list = data.authorizations.authorizations;
      }
      setAuthorizations(list);
      if (data?.authorizationTtlDays) {
        setAuthTtlDays(data.authorizationTtlDays);
      }
    } catch (err: any) {
      console.warn('[SettingsDrawer] Failed to fetch authorizations:', err);
      setAuthErrorMsg(isAr ? 'فشل جلب قائمة الجلسات من السحابة' : 'Failed to fetch sessions from cloud');
    } finally {
      setAuthLoading(false);
    }
  };

  const handleTerminateSession = async (hash: string | number) => {
    const hashStr = String(hash);
    setTerminatingHash(hashStr);
    setAuthErrorMsg(null);
    setAuthSuccessMsg(null);
    try {
      const res = await fetch('/api/telegram/account/terminate-session', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-session-token': activeAccountId || 'guest',
        },
        body: JSON.stringify({ hash: hashStr }),
      });
      const data = await res.json();
      if (data?.success || data?.status === 'ok') {
        setAuthSuccessMsg(isAr ? 'تم إنهاء الجلسة بنجاح' : 'Session terminated successfully');
        setAuthorizations((prev) => prev.filter((a) => String(a.hash) !== hashStr));
        setTimeout(() => setAuthSuccessMsg(null), 3500);
      } else {
        throw new Error(data?.error || 'Failed to terminate');
      }
    } catch (err: any) {
      setAuthErrorMsg(err?.message || (isAr ? 'فشل إنهاء الجلسة' : 'Failed to terminate session'));
      setTimeout(() => setAuthErrorMsg(null), 4000);
    } finally {
      setTerminatingHash(null);
    }
  };

  const handleTerminateAllOtherSessions = async () => {
    setIsTerminatingAll(true);
    setAuthErrorMsg(null);
    setAuthSuccessMsg(null);
    try {
      const res = await fetch('/api/telegram/account/terminate-all-other-sessions', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-session-token': activeAccountId || 'guest',
        },
      });
      const data = await res.json();
      if (data?.success || data?.status === 'ok') {
        setAuthSuccessMsg(
          isAr
            ? 'تم إنهاء كافة الجلسات الأخرى بنجاح عبر MTProto'
            : 'All other sessions terminated successfully via MTProto'
        );
        // Retain current session only
        setAuthorizations((prev) => prev.filter((a) => a.current));
        setTimeout(() => setAuthSuccessMsg(null), 3500);
      } else {
        throw new Error(data?.error || 'Failed to terminate sessions');
      }
    } catch (err: any) {
      setAuthErrorMsg(err?.message || (isAr ? 'فشل إنهاء الجلسات الأخرى' : 'Failed to terminate other sessions'));
      setTimeout(() => setAuthErrorMsg(null), 4000);
    } finally {
      setIsTerminatingAll(false);
    }
  };

  const handleUpdateTTL = async (days: number) => {
    setAuthTtlDays(days);
    try {
      await fetch('/api/telegram/account/ttl', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-session-token': activeAccountId || 'guest',
        },
        body: JSON.stringify({ days }),
      });
      setAuthSuccessMsg(isAr ? 'تم حفظ مهلة الحساب بنجاح' : 'Session TTL saved successfully');
      setTimeout(() => setAuthSuccessMsg(null), 3000);
    } catch (err) {
      console.warn('Error saving TTL:', err);
    }
  };

  useEffect(() => {
    if (isOpen) {
      if (activeSection === 'privacy-security' || activeSection === 'two-step-verification' || activeSection === 'settings-root') {
        fetchCloud2FA();
      }
      if (activeSection === 'privacy-security') {
        fetchCloudPrivacy();
      }
      if (activeSection === 'devices') {
        fetchAuthorizations();
      }
    }
  }, [isOpen, activeSection]);

  const handleUpdatePrivacy = async (
    key: 'phoneNumber' | 'statusTimestamp' | 'forwards',
    value: 'everybody' | 'contacts' | 'nobody'
  ) => {
    // Instant UI update
    if (key === 'phoneNumber') setPhonePrivacy(value);
    else if (key === 'statusTimestamp') setLastSeenPrivacy(value);
    else if (key === 'forwards') setForwardsPrivacy(value);

    setUpdatingPrivacyKey(key);
    setPrivacySyncError(null);
    try {
      // Immediate cloud update (Api.account.SetPrivacy)
      await api.privacy.setPrivacy(key, value);
      const label =
        key === 'phoneNumber'
          ? (isAr ? 'خصوصية رقم الهاتف' : 'Phone number privacy')
          : key === 'statusTimestamp'
          ? (isAr ? 'خصوصية آخر ظهور' : 'Last seen privacy')
          : (isAr ? 'خصوصية الرسائل المحولة' : 'Forwarded messages privacy');
      setPrivacySyncSuccess(isAr ? `تم تحديث ${label} فورياً على سيرفرات تيليجرام` : `${label} updated immediately on Telegram Cloud`);
      setTimeout(() => setPrivacySyncSuccess(null), 3500);
    } catch (err: any) {
      console.error('[SettingsDrawer] Error updating cloud privacy:', err);
      setPrivacySyncError(isAr ? 'تعذر مزامنة الإعداد مع تيليجرام' : 'Failed to sync privacy setting with Telegram');
      setTimeout(() => setPrivacySyncError(null), 4000);
    } finally {
      setUpdatingPrivacyKey(null);
    }
  };

  if (!isOpen) return null;

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

  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    onUpdateUser({
      firstName,
      lastName,
      bio,
      username: username.replace(/^@/, ''),
      photoUrl: photoUrl.trim() || undefined,
    });
    try {
      await csrfFetch('/api/account/update-profile', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          firstName,
          lastName,
          about: bio,
        }),
      });
    } catch (err) {
      console.warn('Could not sync profile to cloud:', err);
    }
    setProfileSavedToast(true);
    setTimeout(() => {
      setProfileSavedToast(false);
      setActiveSection('settings-root');
    }, 900);
  };

  const handleFontSizeChange = (val: number) => {
    setFontSizeSlider(val);
    const sizeCategory = val <= 14 ? 'sm' : val >= 18 ? 'lg' : 'md';
    onUpdateTheme({ fontSize: sizeCategory });
  };

  const handleClearCache = async () => {
    setIsClearingCache(true);
    try {
      await csrfFetch('/api/account/clear-cache', { method: 'POST' });
    } catch (err) {
      console.warn('Error clearing backend cache:', err);
    }
    setTimeout(() => {
      setCacheSize('0.0 KB');
      setIsClearingCache(false);
      setCacheCleared(true);
      setTimeout(() => setCacheCleared(false), 2500);
    }, 600);
  };

  const handleCreateFolder = () => {
    if (!newFolderName.trim()) return;
    setFolderList((prev) => [
      ...prev,
      { id: `custom_${Date.now()}`, name: newFolderName.trim(), count: 0 },
    ]);
    setNewFolderName('');
    setShowNewFolderModal(false);
  };

  const archivedChats = chats.filter((c) => c.isArchived);

  return (
    <div className="fixed inset-0 z-50 flex select-none">
      {/* Backdrop */}
      <div
        onClick={onClose}
        className="fixed inset-0 bg-black/55 backdrop-blur-xs transition-opacity"
      />

      {/* Drawer Container */}
      <aside
        className={`relative w-84 sm:w-96 h-full shadow-2xl flex flex-col z-10 overflow-hidden transition-all duration-300 ${
          themeConfig.isDark ? 'bg-[#17212b] text-white' : 'bg-white text-gray-900'
        }`}
      >
        {/* ========================================================================= */}
        {/* 1. SLIDE-OUT MAIN MENU (Telegram Web K Hamburger Menu) */}
        {/* ========================================================================= */}
        {activeSection === 'menu' && (
          <div className="flex flex-col h-full overflow-hidden">
            {/* Header User Banner */}
            <div className="p-4 bg-gradient-to-b from-[#2b5278] to-[#17212b] text-white relative">
              <button
                onClick={onClose}
                className="absolute top-3 end-3 p-1.5 rounded-full hover:bg-white/10 text-white/80 hover:text-white transition"
                title={isAr ? 'إغلاق' : 'Close'}
              >
                <X className="w-5 h-5" />
              </button>

              <div className="flex items-center gap-3 mt-1">
                {currentUser.photoUrl ? (
                  <div className="relative w-14 h-14 shrink-0">
                    <img
                      src={currentUser.photoUrl}
                      alt={currentUser.firstName}
                      referrerPolicy="no-referrer"
                      className="w-14 h-14 rounded-full object-cover border-2 border-white/30 shadow-md"
                      onError={(e) => {
                        e.currentTarget.style.display = 'none';
                        const fallback = e.currentTarget.nextElementSibling as HTMLElement;
                        if (fallback) fallback.style.display = 'flex';
                      }}
                    />
                    <div className="w-14 h-14 rounded-full bg-[#3390ec] items-center justify-center font-bold text-xl border-2 border-white/30 shadow-md hidden">
                      {currentUser.firstName.slice(0, 2).toUpperCase()}
                    </div>
                  </div>
                ) : (
                  <div className="w-14 h-14 rounded-full bg-[#3390ec] flex items-center justify-center font-bold text-xl border-2 border-white/30 shadow-md">
                    {currentUser.firstName.slice(0, 2).toUpperCase()}
                  </div>
                )}
                <div className="flex-1 min-w-0">
                  <div className="font-semibold text-lg leading-tight truncate flex items-center gap-1.5">
                    {currentUser.firstName} {currentUser.lastName || ''}
                    {currentUser.isVerified && (
                      <CheckCircle2 className="w-4 h-4 text-[#3390ec] fill-white shrink-0" />
                    )}
                  </div>
                  <div className="text-xs text-white/70 truncate mt-0.5">
                    {currentUser.phone || (currentUser.username ? `@${currentUser.username}` : (isAr ? 'متصل' : 'Online'))}
                  </div>
                </div>

                {/* Account Switcher Arrow */}
                {accounts.length > 0 && (
                  <button
                    onClick={() => setShowAccountsList(!showAccountsList)}
                    className="p-2 rounded-full hover:bg-white/10 text-white/80 transition"
                    title={isAr ? 'تبديل الحساب' : 'Switch Account'}
                  >
                    <ChevronDown
                      className={`w-5 h-5 transition-transform duration-200 ${
                        showAccountsList ? 'rotate-180' : ''
                      }`}
                    />
                  </button>
                )}
              </div>

              {/* Multi-Accounts Dropdown List */}
              {showAccountsList && (
                <div className="mt-3 pt-3 border-t border-white/15 space-y-1.5 animate-fadeIn">
                  <div className="text-[11px] font-medium text-white/60 px-1 uppercase tracking-wider">
                    {isAr ? 'الحسابات المتصلة' : 'Connected Accounts'} ({accounts.length}/6)
                  </div>
                  {accounts.map((acc) => (
                    <div
                      key={acc.id}
                      onClick={() => {
                        if (acc.id !== activeAccountId && onSwitchAccount) {
                          onSwitchAccount(acc.id);
                        }
                      }}
                      className={`flex items-center justify-between p-2 rounded-xl cursor-pointer transition ${
                        acc.id === activeAccountId
                          ? 'bg-white/20 font-medium'
                          : 'hover:bg-white/10 text-white/80'
                      }`}
                    >
                      <div className="flex items-center gap-2.5 min-w-0">
                        <div className="w-8 h-8 rounded-full bg-[#3390ec] flex items-center justify-center text-xs font-bold text-white shrink-0">
                          {acc.user.firstName.slice(0, 2).toUpperCase()}
                        </div>
                        <div className="min-w-0">
                          <div className="text-sm truncate">
                            {acc.user.firstName} {acc.user.lastName || ''}
                          </div>
                          <div className="text-[10px] text-white/60 truncate">
                            {acc.user.phone || (acc.user.username ? `@${acc.user.username}` : (isAr ? 'حساب نشط' : 'Active'))}
                          </div>
                        </div>
                      </div>

                      <div className="flex items-center gap-1">
                        {acc.id === activeAccountId && (
                          <Check className="w-4 h-4 text-[#3390ec]" />
                        )}
                        {accounts.length > 1 && onRemoveAccount && (
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              onRemoveAccount(acc.id);
                            }}
                            className="p-1 hover:text-red-400 text-white/40 transition"
                            title={isAr ? 'حذف الحساب' : 'Remove'}
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        )}
                      </div>
                    </div>
                  ))}

                  {accounts.length < 6 && onOpenAddAccount && (
                    <button
                      onClick={() => {
                        onClose();
                        onOpenAddAccount();
                      }}
                      className="w-full mt-1 flex items-center gap-2 px-3 py-2 text-sm text-[#3390ec] hover:bg-white/10 rounded-xl font-medium transition"
                    >
                      <UserPlus className="w-4 h-4" />
                      <span>{isAr ? 'إضافة حساب جديد...' : 'Add Account...'}</span>
                    </button>
                  )}
                </div>
              )}
            </div>

            {/* Menu Items List */}
            <div className="flex-1 overflow-y-auto py-2 px-1">
              {/* Saved Messages */}
              <button
                onClick={() => {
                  onClose();
                  onOpenSavedMessages();
                }}
                className={`w-full flex items-center gap-4 px-4 py-3 text-sm font-medium rounded-xl transition ${
                  themeConfig.isDark ? 'hover:bg-[#232e3c] text-white' : 'hover:bg-gray-100 text-gray-800'
                }`}
              >
                <Bookmark className="w-5 h-5 text-[#3390ec]" />
                <span className="flex-1 text-start">{isAr ? 'الرسائل المحفوظة' : 'Saved Messages'}</span>
              </button>

              {/* Archived Chats */}
              <button
                onClick={() => setShowArchivedModal(true)}
                className={`w-full flex items-center gap-4 px-4 py-3 text-sm font-medium rounded-xl transition ${
                  themeConfig.isDark ? 'hover:bg-[#232e3c] text-white' : 'hover:bg-gray-100 text-gray-800'
                }`}
              >
                <Archive className="w-5 h-5 text-[#3390ec]" />
                <span className="flex-1 text-start">{isAr ? 'المحادثات المؤرشفة' : 'Archived Chats'}</span>
                {archivedChats.length > 0 && (
                  <span className="text-xs bg-[#3390ec]/20 text-[#3390ec] font-bold px-2 py-0.5 rounded-full">
                    {archivedChats.length}
                  </span>
                )}
              </button>

              {/* My Stories */}
              <button
                onClick={() => setShowFeaturesModal(true)}
                className={`w-full flex items-center gap-4 px-4 py-3 text-sm font-medium rounded-xl transition ${
                  themeConfig.isDark ? 'hover:bg-[#232e3c] text-white' : 'hover:bg-gray-100 text-gray-800'
                }`}
              >
                <Camera className="w-5 h-5 text-[#3390ec]" />
                <span className="flex-1 text-start">{isAr ? 'قصصي' : 'My Stories'}</span>
                <span className="text-[10px] bg-gradient-to-r from-purple-500 to-pink-500 text-white font-bold px-1.5 py-0.5 rounded-full">
                  NEW
                </span>
              </button>

              {/* Contacts */}
              <button
                onClick={() => {
                  onClose();
                  onOpenContacts();
                }}
                className={`w-full flex items-center gap-4 px-4 py-3 text-sm font-medium rounded-xl transition ${
                  themeConfig.isDark ? 'hover:bg-[#232e3c] text-white' : 'hover:bg-gray-100 text-gray-800'
                }`}
              >
                <Users className="w-5 h-5 text-[#3390ec]" />
                <span className="flex-1 text-start">{isAr ? 'جهات الاتصال' : 'Contacts'}</span>
              </button>

              {/* Settings (Transitions to Settings Screen) */}
              <button
                onClick={() => setActiveSection('settings-root')}
                className={`w-full flex items-center gap-4 px-4 py-3 text-sm font-medium rounded-xl transition ${
                  themeConfig.isDark ? 'hover:bg-[#232e3c] text-white' : 'hover:bg-gray-100 text-gray-800'
                }`}
              >
                <Sliders className="w-5 h-5 text-[#3390ec]" />
                <span className="flex-1 text-start">{isAr ? 'الإعدادات' : 'Settings'}</span>
                <ChevronRight className="w-4 h-4 text-gray-400 rtl:rotate-180" />
              </button>

              <div className={`my-2 border-t ${themeConfig.isDark ? 'border-gray-700/50' : 'border-gray-200'}`} />

              {/* Night Mode Inline Toggle */}
              <div
                onClick={() => onUpdateTheme({ isDark: !themeConfig.isDark })}
                className={`w-full flex items-center justify-between px-4 py-3 text-sm font-medium rounded-xl cursor-pointer transition ${
                  themeConfig.isDark ? 'hover:bg-[#232e3c] text-white' : 'hover:bg-gray-100 text-gray-800'
                }`}
              >
                <div className="flex items-center gap-4">
                  {themeConfig.isDark ? (
                    <Moon className="w-5 h-5 text-[#3390ec]" />
                  ) : (
                    <Sun className="w-5 h-5 text-amber-500" />
                  )}
                  <span>{isAr ? 'الوضع الليلي' : 'Night Mode'}</span>
                </div>
                {/* Switch indicator */}
                <div
                  className={`w-11 h-6 rounded-full transition-colors relative flex items-center p-0.5 ${
                    themeConfig.isDark ? 'bg-[#3390ec]' : 'bg-gray-300'
                  }`}
                >
                  <div
                    className={`w-5 h-5 rounded-full bg-white shadow-md transform transition-transform ${
                      themeConfig.isDark ? (isAr ? '-translate-x-5' : 'translate-x-5') : 'translate-x-0'
                    }`}
                  />
                </div>
              </div>

              {/* Animations Inline Toggle */}
              <div
                onClick={() => {
                  const nextVal = !animationsEnabled;
                  setAnimationsEnabled(nextVal);
                  onUpdateTheme({ animations: nextVal });
                }}
                className={`w-full flex items-center justify-between px-4 py-3 text-sm font-medium rounded-xl cursor-pointer transition ${
                  themeConfig.isDark ? 'hover:bg-[#232e3c] text-white' : 'hover:bg-gray-100 text-gray-800'
                }`}
              >
                <div className="flex items-center gap-4">
                  <Sparkles className="w-5 h-5 text-[#3390ec]" />
                  <span>{isAr ? 'الحركات والمؤثرات' : 'Animations'}</span>
                </div>
                <div
                  className={`w-11 h-6 rounded-full transition-colors relative flex items-center p-0.5 ${
                    animationsEnabled ? 'bg-[#3390ec]' : 'bg-gray-300'
                  }`}
                >
                  <div
                    className={`w-5 h-5 rounded-full bg-white shadow-md transform transition-transform ${
                      animationsEnabled ? (isAr ? '-translate-x-5' : 'translate-x-5') : 'translate-x-0'
                    }`}
                  />
                </div>
              </div>

              {/* Telegram Features */}
              <button
                onClick={() => setShowFeaturesModal(true)}
                className={`w-full flex items-center gap-4 px-4 py-3 text-sm font-medium rounded-xl transition ${
                  themeConfig.isDark ? 'hover:bg-[#232e3c] text-white' : 'hover:bg-gray-100 text-gray-800'
                }`}
              >
                <HelpCircle className="w-5 h-5 text-[#3390ec]" />
                <span className="flex-1 text-start">{isAr ? 'ميزات تليجرام' : 'Telegram Features'}</span>
              </button>

              {/* Report a Bug */}
              <button
                onClick={() => setShowBugModal(true)}
                className={`w-full flex items-center gap-4 px-4 py-3 text-sm font-medium rounded-xl transition ${
                  themeConfig.isDark ? 'hover:bg-[#232e3c] text-white' : 'hover:bg-gray-100 text-gray-800'
                }`}
              >
                <AlertCircle className="w-5 h-5 text-[#3390ec]" />
                <span className="flex-1 text-start">{isAr ? 'الإبلاغ عن خطأ' : 'Report a Bug'}</span>
              </button>

              {/* MTProto API fixed info */}
              <button
                onClick={() => setActiveSection('api-info')}
                className={`w-full flex items-center gap-4 px-4 py-3 text-sm font-medium rounded-xl transition ${
                  themeConfig.isDark ? 'hover:bg-[#232e3c] text-white' : 'hover:bg-gray-100 text-gray-800'
                }`}
              >
                <Cpu className="w-5 h-5 text-[#3390ec]" />
                <span className="flex-1 text-start">{isAr ? 'بيانات API والاتصال' : 'Telegram MTProto API'}</span>
                <span className="text-[10px] bg-[#3390ec]/20 text-[#3390ec] font-mono px-1.5 py-0.5 rounded">
                  22043994
                </span>
              </button>

              {/* Log Out */}
              <button
                onClick={() => setShowLogoutModal(true)}
                className="w-full flex items-center gap-4 px-4 py-3 text-sm font-medium rounded-xl text-red-500 hover:bg-red-500/10 transition mt-1"
              >
                <LogOut className="w-5 h-5" />
                <span className="flex-1 text-start">{isAr ? 'تسجيل الخروج' : 'Log Out'}</span>
              </button>
            </div>

            {/* Footer Version Info */}
            <div className={`p-3 text-center text-xs font-mono border-t ${
              themeConfig.isDark ? 'border-gray-800 text-gray-500' : 'border-gray-100 text-gray-400'
            }`}>
              Telegram Web K 2.1.0 (Z) • MTProto Layer 198
            </div>
          </div>
        )}

        {/* ========================================================================= */}
        {/* 2. SETTINGS ROOT VIEW (Telegram Web K Settings) */}
        {/* ========================================================================= */}
        {activeSection === 'settings-root' && (
          <div className="flex flex-col h-full overflow-hidden">
            {/* Top Bar */}
            <div className={`p-4 flex items-center justify-between border-b ${
              themeConfig.isDark ? 'border-gray-800 bg-[#17212b]' : 'border-gray-200 bg-white'
            }`}>
              <div className="flex items-center gap-3">
                <button
                  onClick={() => setActiveSection('menu')}
                  className="p-1.5 rounded-full hover:bg-gray-500/10 text-gray-400 hover:text-white transition"
                  title={isAr ? 'رجوع' : 'Back'}
                >
                  <ArrowLeft className="w-5 h-5 rtl:rotate-180" />
                </button>
                <h2 className="font-bold text-lg">{isAr ? 'الإعدادات' : 'Settings'}</h2>
              </div>

              {/* 3-dots Menu */}
              <div className="relative">
                <button
                  onClick={() => setShowThreeDotsMenu(!showThreeDotsMenu)}
                  className="p-1.5 rounded-full hover:bg-gray-500/10 text-gray-400 hover:text-white transition"
                >
                  <MoreVertical className="w-5 h-5" />
                </button>

                {showThreeDotsMenu && (
                  <div
                    className={`absolute end-0 top-9 w-44 rounded-xl shadow-xl py-1 z-30 border ${
                      themeConfig.isDark ? 'bg-[#232e3c] border-gray-700 text-white' : 'bg-white border-gray-200 text-gray-800'
                    }`}
                  >
                    <button
                      onClick={() => {
                        setShowThreeDotsMenu(false);
                        setActiveSection('edit-profile');
                      }}
                      className="w-full text-start px-4 py-2.5 text-sm hover:bg-[#3390ec]/20 flex items-center gap-2"
                    >
                      <Edit2 className="w-4 h-4 text-[#3390ec]" />
                      <span>{isAr ? 'تعديل الملف الشخصي' : 'Edit Profile'}</span>
                    </button>
                    <button
                      onClick={() => {
                        setShowThreeDotsMenu(false);
                        setShowLogoutModal(true);
                      }}
                      className="w-full text-start px-4 py-2.5 text-sm text-red-400 hover:bg-red-500/10 flex items-center gap-2"
                    >
                      <LogOut className="w-4 h-4" />
                      <span>{isAr ? 'تسجيل الخروج' : 'Log Out'}</span>
                    </button>
                  </div>
                )}
              </div>
            </div>

            {/* Profile Overview Card */}
            <div className={`p-5 flex flex-col items-center border-b ${
              themeConfig.isDark ? 'border-gray-800 bg-[#0e1621]/40' : 'border-gray-100 bg-gray-50'
            }`}>
              <div className="relative group cursor-pointer" onClick={() => setActiveSection('edit-profile')}>
                {currentUser.photoUrl ? (
                  <div className="relative w-20 h-20">
                    <img
                      src={currentUser.photoUrl}
                      alt={currentUser.firstName}
                      referrerPolicy="no-referrer"
                      className="w-20 h-20 rounded-full object-cover border-2 border-[#3390ec] shadow-lg"
                      onError={(e) => {
                        e.currentTarget.style.display = 'none';
                        const fallback = e.currentTarget.nextElementSibling as HTMLElement;
                        if (fallback) fallback.style.display = 'flex';
                      }}
                    />
                    <div className="w-20 h-20 rounded-full bg-[#3390ec] items-center justify-center font-bold text-2xl text-white border-2 border-white/20 shadow-lg hidden">
                      {currentUser.firstName.slice(0, 2).toUpperCase()}
                    </div>
                  </div>
                ) : (
                  <div className="w-20 h-20 rounded-full bg-[#3390ec] flex items-center justify-center font-bold text-2xl text-white border-2 border-white/20 shadow-lg">
                    {currentUser.firstName.slice(0, 2).toUpperCase()}
                  </div>
                )}
                <div className="absolute bottom-0 end-0 p-1.5 rounded-full bg-[#3390ec] text-white shadow hover:scale-110 transition">
                  <Camera className="w-4 h-4" />
                </div>
              </div>

              <div className="font-bold text-lg mt-3 flex items-center gap-1.5">
                {currentUser.firstName} {currentUser.lastName || ''}
                {currentUser.isVerified && <CheckCircle2 className="w-4 h-4 text-[#3390ec]" />}
              </div>
              <div className="text-sm text-gray-400">
                {currentUser.phone || (currentUser.username ? `@${currentUser.username}` : (isAr ? 'مستخدم تليجرام' : 'Telegram User'))}
              </div>
              {currentUser.bio && (
                <div className="text-xs text-gray-400/80 mt-1 max-w-[280px] text-center italic">
                  "{currentUser.bio}"
                </div>
              )}
            </div>

            {/* Settings Categories List */}
            <div className="flex-1 overflow-y-auto p-2 space-y-1">
              {/* Edit Profile */}
              <button
                onClick={() => setActiveSection('edit-profile')}
                className={`w-full flex items-center gap-4 px-4 py-3 text-sm font-medium rounded-xl transition ${
                  themeConfig.isDark ? 'hover:bg-[#232e3c] text-white' : 'hover:bg-gray-100 text-gray-800'
                }`}
              >
                <User className="w-5 h-5 text-[#3390ec]" />
                <span className="flex-1 text-start">{isAr ? 'تعديل الملف الشخصي' : 'Edit Profile'}</span>
                <ChevronRight className="w-4 h-4 text-gray-400 rtl:rotate-180" />
              </button>

              {/* Chat Settings */}
              <button
                onClick={() => setActiveSection('chat-settings')}
                className={`w-full flex items-center gap-4 px-4 py-3 text-sm font-medium rounded-xl transition ${
                  themeConfig.isDark ? 'hover:bg-[#232e3c] text-white' : 'hover:bg-gray-100 text-gray-800'
                }`}
              >
                <Palette className="w-5 h-5 text-[#3390ec]" />
                <span className="flex-1 text-start">{isAr ? 'إعدادات المحادثات' : 'Chat Settings'}</span>
                <ChevronRight className="w-4 h-4 text-gray-400 rtl:rotate-180" />
              </button>

              {/* Privacy and Security */}
              <button
                onClick={() => setActiveSection('privacy-security')}
                className={`w-full flex items-center gap-4 px-4 py-3 text-sm font-medium rounded-xl transition ${
                  themeConfig.isDark ? 'hover:bg-[#232e3c] text-white' : 'hover:bg-gray-100 text-gray-800'
                }`}
              >
                <Shield className="w-5 h-5 text-[#3390ec]" />
                <span className="flex-1 text-start">{isAr ? 'الخصوصية والأمان' : 'Privacy and Security'}</span>
                <ChevronRight className="w-4 h-4 text-gray-400 rtl:rotate-180" />
              </button>

              {/* Two-Step Verification (2FA) */}
              <button
                onClick={() => {
                  fetchCloud2FA();
                  setActiveSection('two-step-verification');
                }}
                className={`w-full flex items-center gap-4 px-4 py-3 text-sm font-medium rounded-xl transition ${
                  themeConfig.isDark ? 'hover:bg-[#232e3c] text-white' : 'hover:bg-gray-100 text-gray-800'
                }`}
              >
                <Key className="w-5 h-5 text-[#3390ec]" />
                <div className="flex-1 text-start">
                  <div className="flex items-center gap-2">
                    <span>{isAr ? 'التحقق بخطوتين (2FA)' : 'Two-Step Verification'}</span>
                    {twoStepLoading ? (
                      <RefreshCw className="w-3 h-3 text-[#3390ec] animate-spin" />
                    ) : twoStepEnabled ? (
                      <span className="text-[10px] px-1.5 py-0.5 rounded-full bg-green-500/20 text-green-400 font-semibold">
                        {isAr ? 'مفعل' : 'Active'}
                      </span>
                    ) : (
                      <span className="text-[10px] px-1.5 py-0.5 rounded-full bg-amber-500/20 text-amber-400 font-semibold">
                        {isAr ? 'غير مفعل' : 'Off'}
                      </span>
                    )}
                  </div>
                  {twoStepEnabled && twoStepHint ? (
                    <div className="text-[11px] text-gray-400 truncate max-w-[220px]">
                      {isAr ? `تلميح: ${twoStepHint}` : `Hint: ${twoStepHint}`}
                    </div>
                  ) : (
                    <div className="text-[11px] text-gray-400">
                      {isAr ? 'كلمة مرور إضافية لحماية الحساب' : 'Extra password for account safety'}
                    </div>
                  )}
                </div>
                <ChevronRight className="w-4 h-4 text-gray-400 rtl:rotate-180" />
              </button>

              {/* Notifications and Sounds */}
              <button
                onClick={() => setActiveSection('notifications-sounds')}
                className={`w-full flex items-center gap-4 px-4 py-3 text-sm font-medium rounded-xl transition ${
                  themeConfig.isDark ? 'hover:bg-[#232e3c] text-white' : 'hover:bg-gray-100 text-gray-800'
                }`}
              >
                <Bell className="w-5 h-5 text-[#3390ec]" />
                <span className="flex-1 text-start">{isAr ? 'الإشعارات والأصوات' : 'Notifications and Sounds'}</span>
                <ChevronRight className="w-4 h-4 text-gray-400 rtl:rotate-180" />
              </button>

              {/* Data and Storage */}
              <button
                onClick={() => setActiveSection('data-storage')}
                className={`w-full flex items-center gap-4 px-4 py-3 text-sm font-medium rounded-xl transition ${
                  themeConfig.isDark ? 'hover:bg-[#232e3c] text-white' : 'hover:bg-gray-100 text-gray-800'
                }`}
              >
                <HardDrive className="w-5 h-5 text-[#3390ec]" />
                <span className="flex-1 text-start">{isAr ? 'البيانات والتخزين' : 'Data and Storage'}</span>
                <ChevronRight className="w-4 h-4 text-gray-400 rtl:rotate-180" />
              </button>

              {/* Chat Folders */}
              <button
                onClick={() => setActiveSection('chat-folders')}
                className={`w-full flex items-center gap-4 px-4 py-3 text-sm font-medium rounded-xl transition ${
                  themeConfig.isDark ? 'hover:bg-[#232e3c] text-white' : 'hover:bg-gray-100 text-gray-800'
                }`}
              >
                <Folder className="w-5 h-5 text-[#3390ec]" />
                <span className="flex-1 text-start">{isAr ? 'مجلدات المحادثات' : 'Chat Folders'}</span>
                <ChevronRight className="w-4 h-4 text-gray-400 rtl:rotate-180" />
              </button>

              {/* Language */}
              <button
                onClick={() => setActiveSection('language')}
                className={`w-full flex items-center gap-4 px-4 py-3 text-sm font-medium rounded-xl transition ${
                  themeConfig.isDark ? 'hover:bg-[#232e3c] text-white' : 'hover:bg-gray-100 text-gray-800'
                }`}
              >
                <Globe className="w-5 h-5 text-[#3390ec]" />
                <span className="flex-1 text-start">{isAr ? 'اللغة' : 'Language'}</span>
                <span className="text-xs text-gray-400">{isAr ? 'العربية' : 'English'}</span>
                <ChevronRight className="w-4 h-4 text-gray-400 rtl:rotate-180" />
              </button>

              {/* Devices / Active Sessions */}
              <button
                onClick={() => setActiveSection('devices')}
                className={`w-full flex items-center gap-4 px-4 py-3 text-sm font-medium rounded-xl transition ${
                  themeConfig.isDark ? 'hover:bg-[#232e3c] text-white' : 'hover:bg-gray-100 text-gray-800'
                }`}
              >
                <Smartphone className="w-5 h-5 text-[#3390ec]" />
                <span className="flex-1 text-start">{isAr ? 'الأجهزة والجلسات' : 'Devices'}</span>
                <ChevronRight className="w-4 h-4 text-gray-400 rtl:rotate-180" />
              </button>

              {/* MTProto API Fixed Data */}
              <button
                onClick={() => setActiveSection('api-info')}
                className={`w-full flex items-center gap-4 px-4 py-3 text-sm font-medium rounded-xl transition ${
                  themeConfig.isDark ? 'hover:bg-[#232e3c] text-white' : 'hover:bg-gray-100 text-gray-800'
                }`}
              >
                <Cpu className="w-5 h-5 text-[#3390ec]" />
                <span className="flex-1 text-start">{isAr ? 'بيانات API والاتصال' : 'Telegram MTProto API'}</span>
                <ChevronRight className="w-4 h-4 text-gray-400 rtl:rotate-180" />
              </button>
            </div>
          </div>
        )}

        {/* ========================================================================= */}
        {/* 3. SUB-VIEW: EDIT PROFILE */}
        {/* ========================================================================= */}
        {activeSection === 'edit-profile' && (
          <div className="flex flex-col h-full overflow-hidden">
            <div className={`p-4 flex items-center justify-between border-b ${
              themeConfig.isDark ? 'border-gray-800 bg-[#17212b]' : 'border-gray-200 bg-white'
            }`}>
              <div className="flex items-center gap-3">
                <button
                  onClick={() => setActiveSection('settings-root')}
                  className="p-1.5 rounded-full hover:bg-gray-500/10 text-gray-400 hover:text-white transition"
                >
                  <ArrowLeft className="w-5 h-5 rtl:rotate-180" />
                </button>
                <h2 className="font-bold text-lg">{isAr ? 'تعديل الملف الشخصي' : 'Edit Profile'}</h2>
              </div>
              <button
                onClick={handleSaveProfile}
                className="p-2 text-[#3390ec] hover:text-[#2b7ec9] font-medium transition"
                title={isAr ? 'حفظ التعديلات' : 'Save'}
              >
                <Check className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSaveProfile} className="flex-1 overflow-y-auto p-4 space-y-4">
              {/* Photo Input */}
              <div className="flex flex-col items-center mb-4">
                <div className="relative group mb-3">
                  {photoUrl ? (
                    <img
                      src={photoUrl}
                      alt="Avatar Preview"
                      referrerPolicy="no-referrer"
                      className="w-24 h-24 rounded-full object-cover border-2 border-[#3390ec] shadow-md"
                    />
                  ) : (
                    <div className="w-24 h-24 rounded-full bg-[#3390ec] flex items-center justify-center font-bold text-2xl text-white border-2 border-white/20 shadow-md">
                      {firstName.slice(0, 2).toUpperCase()}
                    </div>
                  )}
                </div>
                <div className="w-full">
                  <label className="block text-xs text-gray-400 mb-1">{isAr ? 'رابط صورة الملف الشخصي' : 'Profile Photo URL'}</label>
                  <input
                    type="url"
                    value={photoUrl}
                    onChange={(e) => setPhotoUrl(e.target.value)}
                    placeholder="https://..."
                    className={`w-full px-3 py-2 text-sm rounded-xl border focus:outline-none focus:border-[#3390ec] ${
                      themeConfig.isDark ? 'bg-[#0e1621] border-gray-700 text-white' : 'bg-gray-50 border-gray-300 text-gray-900'
                    }`}
                  />
                </div>
              </div>

              {/* First Name */}
              <div>
                <label className="block text-xs text-gray-400 mb-1">{isAr ? 'الاسم الأول (مطلوب)' : 'First Name (required)'}</label>
                <input
                  type="text"
                  required
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  className={`w-full px-3 py-2.5 text-sm rounded-xl border focus:outline-none focus:border-[#3390ec] ${
                    themeConfig.isDark ? 'bg-[#0e1621] border-gray-700 text-white' : 'bg-gray-50 border-gray-300 text-gray-900'
                  }`}
                />
              </div>

              {/* Last Name */}
              <div>
                <label className="block text-xs text-gray-400 mb-1">{isAr ? 'اسم العائلة' : 'Last Name'}</label>
                <input
                  type="text"
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  className={`w-full px-3 py-2.5 text-sm rounded-xl border focus:outline-none focus:border-[#3390ec] ${
                    themeConfig.isDark ? 'bg-[#0e1621] border-gray-700 text-white' : 'bg-gray-50 border-gray-300 text-gray-900'
                  }`}
                />
              </div>

              {/* Username */}
              <div>
                <label className="block text-xs text-gray-400 mb-1">{isAr ? 'اسم المستخدم' : 'Username'}</label>
                <div className="relative">
                  <span className="absolute start-3 top-2.5 text-sm text-gray-400 font-mono">@</span>
                  <input
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value.replace(/^@/, ''))}
                    placeholder="username"
                    className={`w-full ps-8 pe-3 py-2.5 text-sm rounded-xl border font-mono focus:outline-none focus:border-[#3390ec] ${
                      themeConfig.isDark ? 'bg-[#0e1621] border-gray-700 text-white' : 'bg-gray-50 border-gray-300 text-gray-900'
                    }`}
                  />
                </div>
                <div className="text-[11px] text-gray-400 mt-1">
                  {isAr ? 'يمكن للأشخاص إيجادك ومراسلتك على تليجرام عبر هذا الاسم' : 'People can find and message you on Telegram using this username.'}
                </div>
              </div>

              {/* Bio */}
              <div>
                <label className="block text-xs text-gray-400 mb-1">
                  {isAr ? 'نبذة تعريفية (Bio)' : 'Bio'}
                  <span className="float-end text-gray-500">{70 - bio.length}</span>
                </label>
                <textarea
                  rows={3}
                  maxLength={70}
                  value={bio}
                  onChange={(e) => setBio(e.target.value)}
                  placeholder={isAr ? 'أخبر الناس بنبذة عنك...' : 'A few words about yourself...'}
                  className={`w-full px-3 py-2.5 text-sm rounded-xl border resize-none focus:outline-none focus:border-[#3390ec] ${
                    themeConfig.isDark ? 'bg-[#0e1621] border-gray-700 text-white' : 'bg-gray-50 border-gray-300 text-gray-900'
                  }`}
                />
              </div>

              {profileSavedToast && (
                <div className="p-3 bg-emerald-500/20 border border-emerald-500/40 text-emerald-400 rounded-xl text-xs flex items-center gap-2 animate-fadeIn">
                  <CheckCircle2 className="w-4 h-4 shrink-0" />
                  <span>{isAr ? 'تم حفظ التعديلات بنجاح!' : 'Profile updated successfully!'}</span>
                </div>
              )}

              <button
                type="submit"
                className="w-full py-3 bg-[#3390ec] hover:bg-[#2b7ec9] text-white rounded-xl text-sm font-semibold transition shadow-md shadow-[#3390ec]/20"
              >
                {isAr ? 'حفظ التغييرات' : 'Save Changes'}
              </button>
            </form>
          </div>
        )}

        {/* ========================================================================= */}
        {/* 4. SUB-VIEW: CHAT SETTINGS */}
        {/* ========================================================================= */}
        {activeSection === 'chat-settings' && (
          <div className="flex flex-col h-full overflow-hidden">
            <div className={`p-4 flex items-center justify-between border-b ${
              themeConfig.isDark ? 'border-gray-800 bg-[#17212b]' : 'border-gray-200 bg-white'
            }`}>
              <div className="flex items-center gap-3">
                <button
                  onClick={() => setActiveSection('settings-root')}
                  className="p-1.5 rounded-full hover:bg-gray-500/10 text-gray-400 hover:text-white transition"
                >
                  <ArrowLeft className="w-5 h-5 rtl:rotate-180" />
                </button>
                <h2 className="font-bold text-lg">{isAr ? 'إعدادات المحادثات' : 'Chat Settings'}</h2>
              </div>
            </div>

            <div className="flex-1 overflow-y-auto p-4 space-y-5">
              {/* Message Text Size Slider */}
              <div className="space-y-2">
                <div className="flex items-center justify-between text-sm font-semibold">
                  <span>{isAr ? 'حجم نص الرسائل' : 'Message Text Size'}</span>
                  <span className="text-[#3390ec] font-mono">{fontSizeSlider}px</span>
                </div>
                <input
                  type="range"
                  min={12}
                  max={20}
                  step={1}
                  value={fontSizeSlider}
                  onChange={(e) => handleFontSizeChange(Number(e.target.value))}
                  className="w-full accent-[#3390ec] cursor-pointer"
                />

                {/* Live Message Bubble Preview */}
                <div className={`p-3 rounded-xl border space-y-2 ${
                  themeConfig.isDark ? 'bg-[#0e1621] border-gray-800' : 'bg-gray-100 border-gray-200'
                }`}>
                  <div className="flex gap-2">
                    <div className="w-7 h-7 rounded-full bg-[#3390ec] flex items-center justify-center text-xs text-white font-bold shrink-0">
                      TG
                    </div>
                    <div
                      style={{ fontSize: `${fontSizeSlider}px` }}
                      className={`p-2.5 rounded-2xl rounded-ss-none max-w-[80%] leading-relaxed ${
                        themeConfig.isDark ? 'bg-[#2b5278] text-white' : 'bg-white text-gray-900 shadow-sm'
                      }`}
                    >
                      {isAr ? 'أهلاً! هكذا ستبدو رسائلك في تليجرام ويب K.' : 'Hello! This is how messages will look in Telegram Web K.'}
                      <div className="text-[10px] text-end text-white/60 mt-1">11:42</div>
                    </div>
                  </div>

                  <div className="flex justify-end">
                    <div
                      style={{ fontSize: `${fontSizeSlider}px` }}
                      className="p-2.5 rounded-2xl rounded-ee-none max-w-[80%] bg-[#3390ec] text-white leading-relaxed"
                    >
                      {isAr ? 'ممتاز، الحجم رائع وواضح جداً! 👍' : 'Looks great, crisp and readable! 👍'}
                      <div className="text-[10px] text-end text-white/80 mt-1">11:43 ✓✓</div>
                    </div>
                  </div>
                </div>
              </div>

              {/* Chat Wallpapers Selection */}
              <div className="space-y-2">
                <div className="text-sm font-semibold">{isAr ? 'خلفية المحادثة' : 'Chat Wallpaper'}</div>
                <div className="grid grid-cols-3 gap-2">
                  {WALLPAPERS.map((wp) => (
                    <button
                      key={wp.id}
                      onClick={() => {
                        setSelectedWallpaper(wp.id);
                        onUpdateTheme({ wallpaper: wp.id });
                      }}
                      className={`h-16 rounded-xl border-2 flex flex-col items-center justify-center p-1 relative transition overflow-hidden ${
                        wp.preview
                      } ${selectedWallpaper === wp.id ? 'border-[#3390ec] ring-2 ring-[#3390ec]/50' : 'border-transparent'}`}
                    >
                      <span className="text-[11px] font-medium text-white/90 drop-shadow">
                        {wp.name}
                      </span>
                      {selectedWallpaper === wp.id && (
                        <div className="absolute top-1 end-1 w-4 h-4 bg-[#3390ec] rounded-full flex items-center justify-center text-white">
                          <Check className="w-2.5 h-2.5" />
                        </div>
                      )}
                    </button>
                  ))}
                </div>
              </div>

              {/* Theme Accent Colors */}
              <div className="space-y-2">
                <div className="text-sm font-semibold">{isAr ? 'لون السمة والواجهة' : 'Accent Color'}</div>
                <div className="flex items-center gap-2">
                  {ACCENT_COLORS.map((col) => (
                    <button
                      key={col.hex}
                      onClick={() => onUpdateTheme({ accentColor: col.hex })}
                      style={{ backgroundColor: col.hex }}
                      className="w-9 h-9 rounded-full flex items-center justify-center text-white transition hover:scale-110 shadow-sm"
                      title={col.name}
                    >
                      {themeConfig.accentColor === col.hex && <Check className="w-4 h-4 stroke-[3]" />}
                    </button>
                  ))}
                </div>
              </div>

              <div className={`border-t ${themeConfig.isDark ? 'border-gray-800' : 'border-gray-200'}`} />

              {/* Switches */}
              <div className="space-y-3">
                {/* Send on Enter */}
                <div
                  onClick={() => {
                    const next = !sendOnEnter;
                    setSendOnEnter(next);
                    onUpdateTheme({ sendOnEnter: next });
                  }}
                  className="flex items-center justify-between cursor-pointer"
                >
                  <div>
                    <div className="text-sm font-medium">{isAr ? 'الإرسال بزر Enter' : 'Send by Enter'}</div>
                    <div className="text-xs text-gray-400">
                      {sendOnEnter ? (isAr ? 'زر Enter يرسل، و Shift+Enter سطر جديد' : 'Enter to send, Shift+Enter for new line') : (isAr ? 'Ctrl+Enter للإرسال' : 'Ctrl+Enter to send')}
                    </div>
                  </div>
                  <div
                    className={`w-10 h-5 rounded-full transition-colors relative flex items-center p-0.5 ${
                      sendOnEnter ? 'bg-[#3390ec]' : 'bg-gray-400'
                    }`}
                  >
                    <div
                      className={`w-4 h-4 rounded-full bg-white shadow transform transition-transform ${
                        sendOnEnter ? (isAr ? '-translate-x-5' : 'translate-x-5') : 'translate-x-0'
                      }`}
                    />
                  </div>
                </div>

                {/* Autoplay Media */}
                <div
                  onClick={() => setAutoplayMedia(!autoplayMedia)}
                  className="flex items-center justify-between cursor-pointer"
                >
                  <div>
                    <div className="text-sm font-medium">{isAr ? 'تشغيل الوسائط التلقائي' : 'Autoplay Media'}</div>
                    <div className="text-xs text-gray-400">{isAr ? 'الصور المتحركة ومقاطع الفيديو' : 'GIFs and Videos'}</div>
                  </div>
                  <div
                    className={`w-10 h-5 rounded-full transition-colors relative flex items-center p-0.5 ${
                      autoplayMedia ? 'bg-[#3390ec]' : 'bg-gray-400'
                    }`}
                  >
                    <div
                      className={`w-4 h-4 rounded-full bg-white shadow transform transition-transform ${
                        autoplayMedia ? (isAr ? '-translate-x-5' : 'translate-x-5') : 'translate-x-0'
                      }`}
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ========================================================================= */}
        {/* 5. SUB-VIEW: PRIVACY & SECURITY */}
        {/* ========================================================================= */}
        {activeSection === 'privacy-security' && (
          <div className="flex flex-col h-full overflow-hidden">
            <div className={`p-4 flex items-center justify-between border-b ${
              themeConfig.isDark ? 'border-gray-800 bg-[#17212b]' : 'border-gray-200 bg-white'
            }`}>
              <div className="flex items-center gap-3">
                <button
                  onClick={() => setActiveSection('settings-root')}
                  className="p-1.5 rounded-full hover:bg-gray-500/10 text-gray-400 hover:text-white transition"
                >
                  <ArrowLeft className="w-5 h-5 rtl:rotate-180" />
                </button>
                <h2 className="font-bold text-lg">{isAr ? 'الخصوصية والأمان' : 'Privacy and Security'}</h2>
              </div>
            </div>

            <div className="flex-1 overflow-y-auto p-4 space-y-4">
              {/* Cloud Sync Status Header */}
              <div className="flex items-center justify-between pb-2 border-b border-gray-700/30">
                <div className="flex items-center gap-2 text-xs text-gray-400">
                  <Shield className="w-4 h-4 text-[#3390ec]" />
                  <span>{isAr ? 'مزامنة سحابية مع حساب تيليجرام' : 'Cloud sync with Telegram account'}</span>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    fetchCloudPrivacy();
                    fetchCloud2FA();
                  }}
                  disabled={privacyLoading || twoStepLoading}
                  className="px-2 py-1 rounded-lg hover:bg-gray-500/10 text-gray-400 hover:text-white transition flex items-center gap-1.5 text-xs font-medium"
                  title={isAr ? 'تحديث الآن من السحابة' : 'Refresh from cloud'}
                >
                  <RefreshCw className={`w-3.5 h-3.5 ${privacyLoading || twoStepLoading ? 'animate-spin text-[#3390ec]' : ''}`} />
                  <span>{privacyLoading || twoStepLoading ? (isAr ? 'جارٍ المزامنة...' : 'Syncing...') : (isAr ? 'تحديث' : 'Refresh')}</span>
                </button>
              </div>

              {/* Feedback messages */}
              {privacySyncSuccess && (
                <div className="p-2.5 rounded-xl bg-green-500/10 border border-green-500/30 text-green-400 text-xs flex items-center gap-2 animate-fadeIn">
                  <CheckCircle2 className="w-4 h-4 shrink-0" />
                  <span>{privacySyncSuccess}</span>
                </div>
              )}
              {privacySyncError && (
                <div className="p-2.5 rounded-xl bg-red-500/10 border border-red-500/30 text-red-400 text-xs flex items-center gap-2 animate-fadeIn">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>{privacySyncError}</span>
                </div>
              )}

              {/* Passcode Lock */}
              <div
                onClick={() => setPasscodeLock(!passcodeLock)}
                className="flex items-center justify-between cursor-pointer p-3 rounded-xl bg-gray-500/5 hover:bg-gray-500/10 transition"
              >
                <div className="flex items-center gap-3">
                  <Lock className="w-5 h-5 text-[#3390ec]" />
                  <div>
                    <div className="text-sm font-medium">{isAr ? 'قفل برمز مرور' : 'Passcode Lock'}</div>
                    <div className="text-xs text-gray-400">{passcodeLock ? (isAr ? 'مفعل' : 'Enabled') : (isAr ? 'معطل' : 'Disabled')}</div>
                  </div>
                </div>
                <div
                  className={`w-10 h-5 rounded-full transition-colors relative flex items-center p-0.5 ${
                    passcodeLock ? 'bg-[#3390ec]' : 'bg-gray-400'
                  }`}
                >
                  <div
                    className={`w-4 h-4 rounded-full bg-white shadow transform transition-transform ${
                      passcodeLock ? (isAr ? '-translate-x-5' : 'translate-x-5') : 'translate-x-0'
                    }`}
                  />
                </div>
              </div>

              {/* Two-Step Verification 2FA (Api.account.GetPassword) */}
              <div className="p-3 rounded-xl bg-gray-500/5 space-y-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Key className="w-5 h-5 text-[#3390ec]" />
                    <div>
                      <div className="text-sm font-medium">{isAr ? 'التحقق بخطوتين (2FA)' : 'Two-Step Verification'}</div>
                      <div className="text-xs text-gray-400">
                        {twoStepLoading ? (
                          isAr ? 'جارٍ التحقق من السحابة...' : 'Checking cloud password...'
                        ) : twoStepEnabled ? (
                          <span className="text-green-400 font-medium">{isAr ? 'كلمة المرور السحابية مفعلة' : 'Cloud password active'}</span>
                        ) : (
                          isAr ? 'غير مفعلة - أضف كلمة مرور سحابية لحماية حسابك' : 'Not enabled - Add extra password to secure your account'
                        )}
                      </div>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => setTwoStepEnabled(!twoStepEnabled)}
                    className="text-xs text-[#3390ec] font-bold px-2 py-1 rounded hover:bg-[#3390ec]/10"
                  >
                    {twoStepEnabled ? (isAr ? 'تعديل' : 'Edit') : (isAr ? 'إعداد' : 'Setup')}
                  </button>
                </div>

                {/* Show password hint if present from Api.account.GetPassword */}
                {twoStepHint && (
                  <div className="p-2 rounded-lg bg-[#3390ec]/10 border border-[#3390ec]/20 text-xs text-[#3390ec] flex items-center justify-between animate-fadeIn">
                    <span className="font-semibold">{isAr ? 'تلميح كلمة المرور السحابية (Hint):' : 'Password Hint:'}</span>
                    <span className="font-mono px-2 py-0.5 rounded bg-black/20 text-white font-medium">{twoStepHint}</span>
                  </div>
                )}

                {twoStepEnabled && (
                  <div className="pt-2 border-t border-gray-700/50 space-y-2 animate-fadeIn">
                    <label className="block text-xs text-gray-400">{isAr ? 'كلمة المرور السحابية' : 'Cloud Password'}</label>
                    <div className="relative">
                      <input
                        type={show2faPassword ? 'text' : 'password'}
                        value={twoStepPassword}
                        onChange={(e) => setTwoStepPassword(e.target.value)}
                        placeholder="••••••••"
                        className={`w-full px-3 py-2 text-sm rounded-lg border pe-9 ${
                          themeConfig.isDark ? 'bg-[#0e1621] border-gray-700 text-white' : 'bg-white border-gray-300 text-gray-900'
                        }`}
                      />
                      <button
                        type="button"
                        onClick={() => setShow2faPassword(!show2faPassword)}
                        className="absolute end-2.5 top-2.5 text-gray-400 hover:text-white"
                      >
                        {show2faPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>
                    </div>
                  </div>
                )}

                {/* Button to open the full dedicated 2FA section */}
                <button
                  type="button"
                  onClick={() => {
                    fetchCloud2FA();
                    setActiveSection('two-step-verification');
                  }}
                  className="w-full mt-2 py-2 px-3 rounded-lg bg-[#3390ec]/15 hover:bg-[#3390ec]/25 text-[#3390ec] text-xs font-semibold flex items-center justify-between transition"
                >
                  <div className="flex items-center gap-2">
                    <Key className="w-3.5 h-3.5" />
                    <span>{isAr ? 'عرض وتعديل إعدادات التحقق بخطوتين بالتفصيل' : 'Open Two-Step Verification Section'}</span>
                  </div>
                  <ChevronRight className="w-4 h-4 rtl:rotate-180" />
                </button>
              </div>

              {/* PrivacySettings Component (Api.account.GetPrivacy & Api.account.SetPrivacy) */}
              <div className="pt-2">
                <PrivacySettings
                  isAr={isAr}
                  themeConfig={themeConfig}
                  className="rounded-2xl border border-gray-700/40"
                />
              </div>

              {/* Devices Link */}
              <button
                onClick={() => setActiveSection('devices')}
                className={`w-full flex items-center justify-between p-3 rounded-xl border ${
                  themeConfig.isDark ? 'border-gray-700 hover:bg-[#232e3c]' : 'border-gray-200 hover:bg-gray-50'
                }`}
              >
                <div className="flex items-center gap-3">
                  <Smartphone className="w-5 h-5 text-[#3390ec]" />
                  <div className="text-start">
                    <div className="text-sm font-medium">{isAr ? 'الجلسات والأجهزة النشطة' : 'Active Sessions'}</div>
                    <div className="text-xs text-gray-400">{isAr ? '1 جهاز متصل حالياً' : '1 active device'}</div>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-gray-400 rtl:rotate-180" />
              </button>
            </div>
          </div>
        )}

        {/* ========================================================================= */}
        {/* 5B. SUB-VIEW: TWO-STEP VERIFICATION (2FA) */}
        {/* ========================================================================= */}
        {activeSection === 'two-step-verification' && (
          <div className="flex flex-col h-full overflow-hidden">
            <div className={`p-4 flex items-center justify-between border-b ${
              themeConfig.isDark ? 'border-gray-800 bg-[#17212b]' : 'border-gray-200 bg-white'
            }`}>
              <div className="flex items-center gap-3">
                <button
                  onClick={() => setActiveSection('privacy-security')}
                  className="p-1.5 rounded-full hover:bg-gray-500/10 text-gray-400 hover:text-white transition"
                  title={isAr ? 'رجوع للخصوصية والأمان' : 'Back to Privacy & Security'}
                >
                  <ArrowLeft className="w-5 h-5 rtl:rotate-180" />
                </button>
                <h2 className="font-bold text-lg">{isAr ? 'التحقق بخطوتين (2FA)' : 'Two-Step Verification'}</h2>
              </div>
              <button
                type="button"
                onClick={fetchCloud2FA}
                disabled={twoStepLoading}
                className="p-1.5 rounded-full hover:bg-gray-500/10 text-gray-400 hover:text-white transition"
                title={isAr ? 'فحص الحالة الآن عبر Api.account.GetPassword' : 'Check Status Now via Api.account.GetPassword'}
              >
                <RefreshCw className={`w-4 h-4 ${twoStepLoading ? 'animate-spin text-[#3390ec]' : ''}`} />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-4 space-y-4">
              {/* Primary Status Banner */}
              <div className={`p-4 rounded-2xl border transition ${
                twoStepEnabled
                  ? themeConfig.isDark
                    ? 'bg-green-500/10 border-green-500/30 text-white'
                    : 'bg-green-50 border-green-200 text-gray-900'
                  : themeConfig.isDark
                    ? 'bg-amber-500/10 border-amber-500/30 text-white'
                    : 'bg-amber-50 border-amber-200 text-gray-900'
              }`}>
                <div className="flex items-start gap-3.5">
                  <div className={`w-11 h-11 rounded-2xl flex items-center justify-center shrink-0 ${
                    twoStepEnabled ? 'bg-green-500 text-white shadow-lg shadow-green-500/20' : 'bg-amber-500 text-white shadow-lg shadow-amber-500/20'
                  }`}>
                    {twoStepEnabled ? <ShieldCheck className="w-6 h-6" /> : <Lock className="w-6 h-6" />}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-bold text-base">
                        {twoStepEnabled
                          ? (isAr ? 'التحقق بخطوتين مفعل ومحمي' : 'Two-Step Verification is Active')
                          : (isAr ? 'التحقق بخطوتين غير مفعل' : 'Two-Step Verification is Off')}
                      </span>
                      {twoStepLoading ? (
                        <Loader2 className="w-3.5 h-3.5 animate-spin text-[#3390ec]" />
                      ) : (
                        <span className={`text-[11px] px-2 py-0.5 rounded-full font-semibold ${
                          twoStepEnabled
                            ? 'bg-green-500/20 text-green-400 border border-green-500/30'
                            : 'bg-amber-500/20 text-amber-400 border border-amber-500/30'
                        }`}>
                          {twoStepEnabled ? (isAr ? 'آمن ومحمي' : 'Protected') : (isAr ? 'يوصى بالتفعيل' : 'Recommended')}
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-gray-400 mt-1 leading-relaxed">
                      {twoStepEnabled
                        ? (isAr
                            ? 'حسابك محمي بكلمة مرور إضافية يتم التحقق منها عبر خوادم تيليجرام السحابية عند تسجيل الدخول من أي جلسة جديدة.'
                            : 'Your account is secured with a cloud password checked by Telegram MTProto servers upon new logins.')
                        : (isAr
                            ? 'أضف كلمة مرور سحابية لحماية حسابك من الاختراق أو الوصول غير المصرح به حتى إذا تم اعتراض رمز SMS.'
                            : 'Add a cloud password to protect your account from unauthorized access even if SMS code is intercepted.')}
                    </p>
                  </div>
                </div>
              </div>

              {/* Active Password Hint Display (Calling Api.account.GetPassword and showing hint if active) */}
              {twoStepEnabled && (
                <div className={`p-4 rounded-2xl border space-y-2.5 ${
                  themeConfig.isDark ? 'bg-[#17212b] border-gray-800' : 'bg-white border-gray-200 shadow-sm'
                }`}>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Key className="w-4 h-4 text-[#3390ec]" />
                      <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">
                        {isAr ? 'تلميح كلمة المرور السحابية' : 'Cloud Password Hint'}
                      </span>
                    </div>
                    <span className="text-[10px] px-2 py-0.5 rounded bg-[#3390ec]/20 text-[#3390ec] font-semibold">
                      {isAr ? 'Api.account.GetPassword' : 'Api.account.GetPassword'}
                    </span>
                  </div>

                  {twoStepHint ? (
                    <div className="p-3.5 rounded-xl bg-gray-500/10 border border-[#3390ec]/30 flex items-center justify-between">
                      <div className="space-y-1">
                        <div className="text-xs text-gray-400">{isAr ? 'التلميح الحالي المحفوظ بالسحابة:' : 'Current Saved Cloud Hint:'}</div>
                        <div className="text-base font-bold text-[#3390ec] font-mono select-all tracking-wide">{twoStepHint}</div>
                      </div>
                      <div className="w-9 h-9 rounded-full bg-[#3390ec]/15 flex items-center justify-center text-[#3390ec]">
                        <CheckCircle2 className="w-5 h-5" />
                      </div>
                    </div>
                  ) : (
                    <div className="p-3 rounded-xl bg-gray-500/10 text-xs text-gray-400 italic">
                      {isAr ? 'لا يوجد تلميح محفوظ لكلمة المرور الحالية.' : 'No hint is set for the current password.'}
                    </div>
                  )}

                  {/* Recovery Email details if available */}
                  {twoStepDetails.loginEmailPattern && (
                    <div className="pt-2 border-t border-gray-700/30 flex items-center justify-between text-xs">
                      <span className="text-gray-400">{isAr ? 'بريد الاسترداد المرتبط:' : 'Linked Recovery Email:'}</span>
                      <span className="font-mono text-gray-300 bg-gray-800/60 px-2 py-0.5 rounded border border-gray-700/50">
                        {twoStepDetails.loginEmailPattern}
                      </span>
                    </div>
                  )}
                </div>
              )}

              {/* Password Management & Hint Setting */}
              <div className={`p-4 rounded-2xl border space-y-3 ${
                themeConfig.isDark ? 'bg-[#17212b] border-gray-800' : 'bg-white border-gray-200 shadow-sm'
              }`}>
                <div className="text-sm font-bold flex items-center gap-2">
                  <Lock className="w-4 h-4 text-[#3390ec]" />
                  <span>{twoStepEnabled ? (isAr ? 'تحديث إعدادات كلمة المرور السحابية' : 'Update Cloud Password Settings') : (isAr ? 'تعيين كلمة مرور سحابية جديدة' : 'Set New Cloud Password')}</span>
                </div>

                {twoStepSuccessMsg && (
                  <div className="p-2.5 rounded-xl bg-green-500/15 border border-green-500/30 text-xs text-green-400 flex items-center gap-2">
                    <CheckCircle2 className="w-4 h-4 shrink-0" />
                    <span>{twoStepSuccessMsg}</span>
                  </div>
                )}

                <div className="space-y-1.5">
                  <label className="text-xs font-medium text-gray-400">{isAr ? 'كلمة المرور' : 'Password'}</label>
                  <div className="relative">
                    <input
                      type={show2faPassword ? 'text' : 'password'}
                      value={twoStepPassword}
                      onChange={(e) => setTwoStepPassword(e.target.value)}
                      placeholder={twoStepEnabled ? '••••••••' : (isAr ? 'أدخل كلمة مرور قوية' : 'Enter strong password')}
                      className={`w-full px-3.5 py-2.5 text-sm rounded-xl border pe-10 transition ${
                        themeConfig.isDark ? 'bg-[#0e1621] border-gray-700 text-white focus:border-[#3390ec]' : 'bg-white border-gray-300 text-gray-900 focus:border-[#3390ec]'
                      }`}
                    />
                    <button
                      type="button"
                      onClick={() => setShow2faPassword(!show2faPassword)}
                      className="absolute end-3 top-3 text-gray-400 hover:text-white"
                    >
                      {show2faPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                  {/* Real-time Password Strength Visualizer */}
                  <PasswordStrengthIndicator password={twoStepPassword} lang={isAr ? 'ar' : 'en'} />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-medium text-gray-400">{isAr ? 'تلميح كلمة المرور (Hint)' : 'Password Hint'}</label>
                  <input
                    type="text"
                    value={newTwoStepHint}
                    onChange={(e) => setNewTwoStepHint(e.target.value)}
                    placeholder={isAr ? 'مثال: تاريخ مهم أو كلمة مميزة' : 'e.g., childhood pet, memorable city'}
                    className={`w-full px-3.5 py-2.5 text-sm rounded-xl border transition ${
                      themeConfig.isDark ? 'bg-[#0e1621] border-gray-700 text-white focus:border-[#3390ec]' : 'bg-white border-gray-300 text-gray-900 focus:border-[#3390ec]'
                    }`}
                  />
                  <p className="text-[11px] text-gray-500">
                    {isAr ? 'سيظهر هذا التلميح في حالة نسيت كلمة المرور لمساعدتك على تذكرها.' : 'This hint will be displayed if you forget your password.'}
                  </p>
                </div>

                <div className="flex items-center gap-2 pt-2">
                  <button
                    type="button"
                    onClick={async () => {
                      setIsSaving2FA(true);
                      setTwoStepSuccessMsg(null);
                      try {
                        await csrfFetch('/api/account/2fa/set', {
                          method: 'POST',
                          headers: { 'Content-Type': 'application/json' },
                          body: JSON.stringify({
                            password: twoStepPassword || undefined,
                            hint: newTwoStepHint || undefined,
                          }),
                        });
                        setTwoStepHint(newTwoStepHint);
                        setTwoStepEnabled(true);
                        setTwoStepSuccessMsg(isAr ? 'تم تحديث إعدادات التحقق بخطوتين وتعيين التلميح بنجاح!' : 'Two-step verification and hint updated successfully!');
                        await fetchCloud2FA();
                      } catch (err: any) {
                        console.warn('Failed to update 2FA:', err);
                      } finally {
                        setIsSaving2FA(false);
                      }
                    }}
                    disabled={isSaving2FA}
                    className="flex-1 py-2.5 px-4 rounded-xl bg-[#3390ec] hover:bg-[#2b7ac9] text-white font-semibold text-xs transition flex items-center justify-center gap-2 shadow-md shadow-[#3390ec]/20 disabled:opacity-50"
                  >
                    {isSaving2FA ? (
                      <>
                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                        <span>{isAr ? 'جارٍ الحفظ...' : 'Saving...'}</span>
                      </>
                    ) : (
                      <>
                        <Save className="w-3.5 h-3.5" />
                        <span>{isAr ? 'حفظ إعدادات التحقق بخطوتين' : 'Save 2FA Settings'}</span>
                      </>
                    )}
                  </button>

                  <button
                    type="button"
                    onClick={fetchCloud2FA}
                    disabled={twoStepLoading}
                    className={`p-2.5 rounded-xl border text-xs font-semibold flex items-center justify-center transition ${
                      themeConfig.isDark ? 'border-gray-700 hover:bg-gray-800 text-gray-300' : 'border-gray-300 hover:bg-gray-100 text-gray-700'
                    }`}
                    title={isAr ? 'إعادة فحص الحالة من خوادم تيليجرام' : 'Re-check status from Telegram servers'}
                  >
                    <RefreshCw className={`w-4 h-4 ${twoStepLoading ? 'animate-spin text-[#3390ec]' : ''}`} />
                  </button>
                </div>
              </div>

              {/* Security Recommendations Card */}
              <div className={`p-4 rounded-2xl border space-y-2.5 ${
                themeConfig.isDark ? 'bg-gray-800/30 border-gray-800 text-gray-300' : 'bg-gray-50 border-gray-200 text-gray-700'
              }`}>
                <div className="text-xs font-bold text-gray-400 uppercase tracking-wider flex items-center gap-1.5">
                  <ShieldCheck className="w-3.5 h-3.5 text-[#3390ec]" />
                  <span>{isAr ? 'إرشادات أمان تيليجرام' : 'Telegram Security Guidelines'}</span>
                </div>
                <ul className="text-xs space-y-1.5 list-disc list-inside text-gray-400 leading-relaxed">
                  <li>{isAr ? 'اختر كلمة مرور مميزة لا تستخدمها في حسابات أخرى.' : 'Choose a unique password not used on other services.'}</li>
                  <li>{isAr ? 'تأكد من أن التلميح يذكرك أنت فقط ولا يكشف كلمة المرور للآخرين.' : 'Ensure the hint reminds you without giving the password away.'}</li>
                  <li>{isAr ? 'قم بربط بريد إلكتروني صالح لاستعادة الحساب إذا نسيت كلمة المرور.' : 'Link a valid recovery email in case you forget the password.'}</li>
                </ul>
              </div>
            </div>
          </div>
        )}

        {/* ========================================================================= */}
        {/* 6. SUB-VIEW: NOTIFICATIONS & SOUNDS */}
        {/* ========================================================================= */}
        {activeSection === 'notifications-sounds' && (
          <div className="flex flex-col h-full overflow-hidden">
            <div className={`p-4 flex items-center justify-between border-b ${
              themeConfig.isDark ? 'border-gray-800 bg-[#17212b]' : 'border-gray-200 bg-white'
            }`}>
              <div className="flex items-center gap-3">
                <button
                  onClick={() => setActiveSection('settings-root')}
                  className="p-1.5 rounded-full hover:bg-gray-500/10 text-gray-400 hover:text-white transition"
                >
                  <ArrowLeft className="w-5 h-5 rtl:rotate-180" />
                </button>
                <h2 className="font-bold text-lg">{isAr ? 'الإشعارات والأصوات' : 'Notifications and Sounds'}</h2>
              </div>
            </div>

            <div className="flex-1 overflow-y-auto p-4 space-y-4">
              {/* Chats notifications */}
              <div className="space-y-3">
                <div
                  onClick={() => setNotifyPrivate(!notifyPrivate)}
                  className="flex items-center justify-between cursor-pointer"
                >
                  <span className="text-sm font-medium">{isAr ? 'المحادثات الخاصة' : 'Private Chats'}</span>
                  <div className={`w-10 h-5 rounded-full transition-colors relative flex items-center p-0.5 ${notifyPrivate ? 'bg-[#3390ec]' : 'bg-gray-400'}`}>
                    <div className={`w-4 h-4 rounded-full bg-white shadow transform transition-transform ${notifyPrivate ? (isAr ? '-translate-x-5' : 'translate-x-5') : 'translate-x-0'}`} />
                  </div>
                </div>

                <div
                  onClick={() => setNotifyGroups(!notifyGroups)}
                  className="flex items-center justify-between cursor-pointer"
                >
                  <span className="text-sm font-medium">{isAr ? 'المجموعات' : 'Groups'}</span>
                  <div className={`w-10 h-5 rounded-full transition-colors relative flex items-center p-0.5 ${notifyGroups ? 'bg-[#3390ec]' : 'bg-gray-400'}`}>
                    <div className={`w-4 h-4 rounded-full bg-white shadow transform transition-transform ${notifyGroups ? (isAr ? '-translate-x-5' : 'translate-x-5') : 'translate-x-0'}`} />
                  </div>
                </div>

                <div
                  onClick={() => setNotifyChannels(!notifyChannels)}
                  className="flex items-center justify-between cursor-pointer"
                >
                  <span className="text-sm font-medium">{isAr ? 'القنوات' : 'Channels'}</span>
                  <div className={`w-10 h-5 rounded-full transition-colors relative flex items-center p-0.5 ${notifyChannels ? 'bg-[#3390ec]' : 'bg-gray-400'}`}>
                    <div className={`w-4 h-4 rounded-full bg-white shadow transform transition-transform ${notifyChannels ? (isAr ? '-translate-x-5' : 'translate-x-5') : 'translate-x-0'}`} />
                  </div>
                </div>

                <div
                  onClick={() => setSoundEnabled(!soundEnabled)}
                  className="flex items-center justify-between cursor-pointer"
                >
                  <span className="text-sm font-medium">{isAr ? 'أصوات التنبيه' : 'Sound Effects'}</span>
                  <div className={`w-10 h-5 rounded-full transition-colors relative flex items-center p-0.5 ${soundEnabled ? 'bg-[#3390ec]' : 'bg-gray-400'}`}>
                    <div className={`w-4 h-4 rounded-full bg-white shadow transform transition-transform ${soundEnabled ? (isAr ? '-translate-x-5' : 'translate-x-5') : 'translate-x-0'}`} />
                  </div>
                </div>
              </div>

              <div className={`border-t ${themeConfig.isDark ? 'border-gray-800' : 'border-gray-200'}`} />

              {/* Web Push VAPID Card */}
              <div className={`p-4 rounded-2xl border space-y-3 ${
                themeConfig.isDark ? 'bg-[#0e1621] border-gray-700' : 'bg-gray-50 border-gray-200'
              }`}>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <Bell className="w-5 h-5 text-[#3390ec]" />
                    <span className="font-semibold text-sm">{isAr ? 'إشعارات الويب (VAPID)' : 'Web Push (VAPID)'}</span>
                  </div>
                  <button
                    onClick={handleTogglePush}
                    disabled={pushLoading}
                    className={`w-10 h-5 rounded-full transition-colors relative flex items-center p-0.5 ${
                      pushEnabled ? 'bg-[#3390ec]' : 'bg-gray-400'
                    }`}
                  >
                    <div
                      className={`w-4 h-4 rounded-full bg-white shadow transform transition-transform ${
                        pushEnabled ? (isAr ? '-translate-x-5' : 'translate-x-5') : 'translate-x-0'
                      }`}
                    />
                  </button>
                </div>

                <p className="text-xs text-gray-400 leading-relaxed">
                  {isAr
                    ? 'استلم إشعارات الرسائل الفورية حتى عند إغلاق متصفح الويب باستخدام معيار Web Push VAPID.'
                    : 'Receive real-time message alerts even when the browser is closed via Web Push VAPID.'}
                </p>

                <div className="text-[11px] font-mono text-gray-500 bg-gray-500/10 p-2 rounded-lg truncate">
                  {VAPID_SUBJECT}
                </div>

                {pushStatusText && (
                  <div className="text-xs text-[#3390ec] font-medium animate-fadeIn">
                    {pushStatusText}
                  </div>
                )}

                <button
                  onClick={handleTestPush}
                  disabled={pushLoading}
                  className="w-full py-2 px-3 bg-[#3390ec]/15 hover:bg-[#3390ec]/25 text-[#3390ec] rounded-xl text-xs font-semibold flex items-center justify-center gap-2 transition"
                >
                  {pushLoading ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : (
                    <Send className="w-4 h-4" />
                  )}
                  <span>{isAr ? 'إرسال إشعار تجريبي لاختبار VAPID' : 'Send Test Web Push Alert'}</span>
                </button>
              </div>
            </div>
          </div>
        )}

        {/* ========================================================================= */}
        {/* 7. SUB-VIEW: DATA & STORAGE */}
        {/* ========================================================================= */}
        {activeSection === 'data-storage' && (
          <div className="flex flex-col h-full overflow-hidden">
            <div className={`p-4 flex items-center justify-between border-b ${
              themeConfig.isDark ? 'border-gray-800 bg-[#17212b]' : 'border-gray-200 bg-white'
            }`}>
              <div className="flex items-center gap-3">
                <button
                  onClick={() => setActiveSection('settings-root')}
                  className="p-1.5 rounded-full hover:bg-gray-500/10 text-gray-400 hover:text-white transition"
                >
                  <ArrowLeft className="w-5 h-5 rtl:rotate-180" />
                </button>
                <h2 className="font-bold text-lg">{isAr ? 'البيانات والتخزين' : 'Data and Storage'}</h2>
              </div>
            </div>

            <div className="flex-1 overflow-y-auto p-4 space-y-4">
              {/* Storage breakdown */}
              <div className={`p-4 rounded-2xl border space-y-3 ${
                themeConfig.isDark ? 'bg-[#0e1621] border-gray-700' : 'bg-gray-50 border-gray-200'
              }`}>
                <div className="flex items-center justify-between">
                  <span className="font-semibold text-sm">{isAr ? 'ذاكرة التخزين المؤقت' : 'Cache Storage'}</span>
                  <span className="font-mono text-sm text-[#3390ec] font-bold">{cacheSize}</span>
                </div>

                {/* Progress bar */}
                <div className="h-2 w-full bg-gray-700/40 rounded-full overflow-hidden flex">
                  <div className="h-full bg-blue-500 w-[40%]" title="Photos" />
                  <div className="h-full bg-emerald-500 w-[30%]" title="Videos" />
                  <div className="h-full bg-amber-500 w-[15%]" title="Files" />
                  <div className="h-full bg-purple-500 w-[15%]" title="System" />
                </div>

                <div className="grid grid-cols-2 gap-2 text-xs text-gray-400 pt-1">
                  <div className="flex items-center gap-1.5">
                    <span className="w-2.5 h-2.5 rounded-full bg-blue-500" />
                    <span>{isAr ? 'الصور: 14.8 MB' : 'Photos: 14.8 MB'}</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <span className="w-2.5 h-2.5 rounded-full bg-emerald-500" />
                    <span>{isAr ? 'الفيديو: 32.4 MB' : 'Videos: 32.4 MB'}</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <span className="w-2.5 h-2.5 rounded-full bg-amber-500" />
                    <span>{isAr ? 'المستندات: 8.6 MB' : 'Docs: 8.6 MB'}</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <span className="w-2.5 h-2.5 rounded-full bg-purple-500" />
                    <span>{isAr ? 'بيانات تليجرام: 12.1 MB' : 'Cache: 12.1 MB'}</span>
                  </div>
                </div>

                <button
                  onClick={handleClearCache}
                  disabled={isClearingCache || cacheSize === '0.0 KB'}
                  className="w-full mt-2 py-2.5 bg-red-500/10 hover:bg-red-500/20 text-red-400 rounded-xl text-xs font-semibold flex items-center justify-center gap-2 transition"
                >
                  {isClearingCache ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : cacheCleared ? (
                    <Check className="w-4 h-4 text-emerald-400" />
                  ) : (
                    <Trash2 className="w-4 h-4" />
                  )}
                  <span>
                    {isClearingCache
                      ? (isAr ? 'جارٍ تنظيف الذاكرة المؤقتة...' : 'Clearing Cache...')
                      : cacheCleared
                      ? (isAr ? 'تم تنظيف الذاكرة المؤقتة بنجاح!' : 'Cache Cleared Successfully!')
                      : (isAr ? 'مسح ذاكرة التخزين المؤقت' : 'Clear Telegram Cache')}
                  </span>
                </button>
              </div>

              {/* Automatic download settings */}
              <div className="space-y-3 pt-2">
                <div className="text-sm font-semibold">{isAr ? 'التنزيل التلقائي للوسائط' : 'Automatic Media Download'}</div>
                <div className="flex items-center justify-between text-sm py-1">
                  <span>{isAr ? 'عند الاتصال بالواي فاي' : 'When Connected to Wi-Fi'}</span>
                  <span className="text-xs text-[#3390ec]">{isAr ? 'الكل مفعل' : 'All Media'}</span>
                </div>
                <div className="flex items-center justify-between text-sm py-1">
                  <span>{isAr ? 'باستخدام بيانات الهاتف' : 'When using Mobile Data'}</span>
                  <span className="text-xs text-[#3390ec]">{isAr ? 'الصور فقط' : 'Photos Only'}</span>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ========================================================================= */}
        {/* 8. SUB-VIEW: CHAT FOLDERS */}
        {/* ========================================================================= */}
        {activeSection === 'chat-folders' && (
          <div className="flex flex-col h-full overflow-hidden">
            <div className={`p-4 flex items-center justify-between border-b ${
              themeConfig.isDark ? 'border-gray-800 bg-[#17212b]' : 'border-gray-200 bg-white'
            }`}>
              <div className="flex items-center gap-3">
                <button
                  onClick={() => setActiveSection('settings-root')}
                  className="p-1.5 rounded-full hover:bg-gray-500/10 text-gray-400 hover:text-white transition"
                >
                  <ArrowLeft className="w-5 h-5 rtl:rotate-180" />
                </button>
                <h2 className="font-bold text-lg">{isAr ? 'مجلدات المحادثات' : 'Chat Folders'}</h2>
              </div>
            </div>

            <div className="flex-1 overflow-y-auto p-4 space-y-3">
              <p className="text-xs text-gray-400 leading-relaxed">
                {isAr
                  ? 'أنشئ مجلدات مخصصة لتصنيف وتنظيم محادثاتك وقنواتك وبوتاتك في واجهة تليجرام ويب K.'
                  : 'Create custom folders for different groups of chats to quickly switch between them.'}
              </p>

              <div className="space-y-2">
                {folderList.map((f) => (
                  <div
                    key={f.id}
                    className={`flex items-center justify-between p-3 rounded-xl border ${
                      themeConfig.isDark ? 'bg-[#0e1621]/60 border-gray-700/60' : 'bg-gray-50 border-gray-200'
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <Folder className="w-5 h-5 text-[#3390ec]" />
                      <div>
                        <div className="text-sm font-medium">{f.name}</div>
                        <div className="text-xs text-gray-400">
                          {f.count} {isAr ? 'محادثة' : 'chats'}
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              <button
                onClick={() => setShowNewFolderModal(true)}
                className="w-full mt-2 py-3 border-2 border-dashed border-[#3390ec]/40 hover:border-[#3390ec] text-[#3390ec] rounded-xl text-sm font-semibold flex items-center justify-center gap-2 transition"
              >
                <Folder className="w-4 h-4" />
                <span>{isAr ? 'إنشاء مجلد جديد' : 'Create New Folder'}</span>
              </button>
            </div>
          </div>
        )}

        {/* ========================================================================= */}
        {/* 9. SUB-VIEW: LANGUAGE */}
        {/* ========================================================================= */}
        {activeSection === 'language' && (
          <div className="flex flex-col h-full overflow-hidden">
            <div className={`p-4 flex items-center justify-between border-b ${
              themeConfig.isDark ? 'border-gray-800 bg-[#17212b]' : 'border-gray-200 bg-white'
            }`}>
              <div className="flex items-center gap-3">
                <button
                  onClick={() => setActiveSection('settings-root')}
                  className="p-1.5 rounded-full hover:bg-gray-500/10 text-gray-400 hover:text-white transition"
                >
                  <ArrowLeft className="w-5 h-5 rtl:rotate-180" />
                </button>
                <h2 className="font-bold text-lg">{isAr ? 'اللغة' : 'Language'}</h2>
              </div>
            </div>

            <div className="flex-1 overflow-y-auto p-2 space-y-1">
              {LANGUAGES.map((lang) => {
                const isSelected =
                  (lang.code === 'ar' && themeConfig.language === 'ar') ||
                  (lang.code === 'en' && themeConfig.language === 'en');

                return (
                  <button
                    key={lang.code}
                    onClick={() => {
                      if (lang.code === 'ar' || lang.code === 'en') {
                        onUpdateTheme({ language: lang.code });
                      }
                    }}
                    className={`w-full flex items-center justify-between px-4 py-3 text-sm font-medium rounded-xl transition ${
                      isSelected
                        ? 'bg-[#3390ec]/15 text-[#3390ec]'
                        : themeConfig.isDark
                        ? 'hover:bg-[#232e3c] text-white'
                        : 'hover:bg-gray-100 text-gray-800'
                    }`}
                  >
                    <div>
                      <div className="font-medium text-start">{lang.native}</div>
                      <div className="text-xs text-gray-400 text-start">{lang.name}</div>
                    </div>
                    {isSelected && <Check className="w-5 h-5 text-[#3390ec]" />}
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* ========================================================================= */}
        {/* 10. SUB-VIEW: DEVICES / ACTIVE SESSIONS */}
        {/* ========================================================================= */}
        {activeSection === 'devices' && (
          <div className="flex flex-col h-full overflow-hidden">
            <div className={`p-4 flex items-center justify-between border-b ${
              themeConfig.isDark ? 'border-gray-800 bg-[#17212b]' : 'border-gray-200 bg-white'
            }`}>
              <div className="flex items-center gap-3">
                <button
                  onClick={() => setActiveSection('settings-root')}
                  className="p-1.5 rounded-full hover:bg-gray-500/10 text-gray-400 hover:text-white transition"
                >
                  <ArrowLeft className="w-5 h-5 rtl:rotate-180" />
                </button>
                <h2 className="font-bold text-lg">{isAr ? 'الأجهزة والجلسات' : 'Devices & Sessions'}</h2>
              </div>
              <button
                onClick={fetchAuthorizations}
                disabled={authLoading}
                className="p-2 rounded-xl hover:bg-gray-500/10 text-gray-400 hover:text-[#3390ec] transition disabled:opacity-50"
                title={isAr ? 'تحديث الجلسات' : 'Refresh sessions'}
              >
                <RefreshCw className={`w-4 h-4 ${authLoading ? 'animate-spin text-[#3390ec]' : ''}`} />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-4 space-y-5">
              {/* Notification Banners */}
              {authSuccessMsg && (
                <div className="p-3 bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs rounded-xl flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 shrink-0" />
                  <span>{authSuccessMsg}</span>
                </div>
              )}
              {authErrorMsg && (
                <div className="p-3 bg-red-500/10 border border-red-500/20 text-red-400 text-xs rounded-xl flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>{authErrorMsg}</span>
                </div>
              )}

              {/* QR Code link hero */}
              <div className="flex flex-col items-center text-center p-3">
                <div className="w-16 h-16 rounded-full bg-[#3390ec]/20 flex items-center justify-center text-[#3390ec] mb-3">
                  <Smartphone className="w-8 h-8" />
                </div>
                <h3 className="font-bold text-base">{isAr ? 'ربط جهاز حاسوب أو هاتف' : 'Link Desktop Device'}</h3>
                <p className="text-xs text-gray-400 mt-1 max-w-xs">
                  {isAr ? 'امسح رمز الاستجابة السريعة (QR Code) لتسجيل الدخول السريع في جهاز آخر.' : 'Scan QR Code with Telegram app to log in instantly.'}
                </p>
                <button
                  onClick={() => setShowLinkQrModal(true)}
                  className="mt-3 px-4 py-2 bg-[#3390ec] hover:bg-[#2b7ec9] text-white rounded-xl text-xs font-semibold flex items-center gap-2 transition"
                >
                  <QrCode className="w-4 h-4" />
                  <span>{isAr ? 'إظهار رمز الاستجابة السريعة' : 'Show QR Code'}</span>
                </button>
              </div>

              {/* Current Active Session */}
              {(() => {
                const currentSession = authorizations.find((a) => a.current) || authorizations[0];
                const otherSessions = authorizations.filter((a) => a !== currentSession);

                return (
                  <div className="space-y-4">
                    <div className="space-y-2">
                      <div className="text-xs font-semibold text-gray-400 uppercase tracking-wider">
                        {isAr ? 'هذا الجهاز (الجلسة الحالية)' : 'This Device (Current Session)'}
                      </div>
                      <div className={`p-3.5 rounded-xl border ${
                        themeConfig.isDark ? 'bg-[#0e1621] border-gray-800' : 'bg-gray-50 border-gray-200'
                      }`}>
                        <div className="flex items-start justify-between gap-2">
                          <div className="flex items-center gap-3">
                            <div className="w-10 h-10 rounded-xl bg-[#3390ec]/15 text-[#3390ec] flex items-center justify-center">
                              <Laptop className="w-5 h-5" />
                            </div>
                            <div>
                              <div className="font-semibold text-sm">
                                {currentSession?.deviceModel || 'Telegram Web Client'}
                              </div>
                              <div className="text-xs text-emerald-400 mt-0.5 flex items-center gap-1.5">
                                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
                                {isAr ? 'نشط الآن' : 'Active now'}
                                {currentSession?.ip ? ` • ${currentSession.ip}` : ''}
                                {currentSession?.country ? ` (${currentSession.country})` : ''}
                              </div>
                            </div>
                          </div>
                        </div>
                        <div className="text-[11px] text-gray-400 mt-2.5 pt-2 border-t border-gray-700/40 flex flex-wrap gap-x-3 gap-y-1">
                          <span>{currentSession?.appName || 'Telegram Web'} {currentSession?.appVersion || 'v1.0'}</span>
                          <span>•</span>
                          <span>{currentSession?.platform || 'Web/Browser'}</span>
                          <span>•</span>
                          <span className="text-emerald-500">MTProto 2.0 Cloud Connected</span>
                        </div>
                      </div>
                    </div>

                    {/* Terminate All Other Sessions Button */}
                    <button
                      onClick={handleTerminateAllOtherSessions}
                      disabled={isTerminatingAll || otherSessions.length === 0}
                      className="w-full py-3 bg-red-500/10 hover:bg-red-500/20 text-red-500 rounded-xl text-xs font-semibold transition flex items-center justify-center gap-2 disabled:opacity-40 disabled:cursor-not-allowed"
                    >
                      {isTerminatingAll ? (
                        <>
                          <Loader2 className="w-4 h-4 animate-spin" />
                          <span>{isAr ? 'جارٍ إنهاء الجلسات...' : 'Terminating sessions...'}</span>
                        </>
                      ) : (
                        <>
                          <LogOut className="w-4 h-4" />
                          <span>
                            {isAr
                              ? `إنهاء كافة الجلسات الأخرى (${otherSessions.length})`
                              : `Terminate All Other Sessions (${otherSessions.length})`}
                          </span>
                        </>
                      )}
                    </button>

                    {/* Other Active Sessions List */}
                    <div className="space-y-2">
                      <div className="text-xs font-semibold text-gray-400 uppercase tracking-wider">
                        {isAr ? 'الجلسات النشطة الأخرى' : 'Other Active Sessions'}
                      </div>

                      {authLoading && (
                        <div className="py-6 flex flex-col items-center justify-center text-gray-400 gap-2">
                          <Loader2 className="w-6 h-6 animate-spin text-[#3390ec]" />
                          <span className="text-xs">{isAr ? 'جارٍ جلب الجلسات من تيليجرام...' : 'Fetching active sessions...'}</span>
                        </div>
                      )}

                      {!authLoading && otherSessions.length === 0 && (
                        <div className={`p-4 rounded-xl text-center text-xs text-gray-400 border ${
                          themeConfig.isDark ? 'bg-[#0e1621] border-gray-800' : 'bg-gray-50 border-gray-200'
                        }`}>
                          {isAr ? 'لا توجد أجهزة أو جلسات أخرى مسجلة حالياً.' : 'No other active sessions found.'}
                        </div>
                      )}

                      {!authLoading && otherSessions.map((session) => {
                        const hash = String(session.hash || '');
                        const isTerminating = terminatingHash === hash;
                        const isPhone = (session.platform || '').toLowerCase().includes('android') || (session.platform || '').toLowerCase().includes('ios');

                        return (
                          <div
                            key={hash || Math.random()}
                            className={`p-3.5 rounded-xl border flex items-center justify-between gap-3 ${
                              themeConfig.isDark ? 'bg-[#0e1621] border-gray-800' : 'bg-gray-50 border-gray-200'
                            }`}
                          >
                            <div className="flex items-center gap-3 min-w-0">
                              <div className="w-10 h-10 rounded-xl bg-gray-500/10 text-gray-400 flex items-center justify-center shrink-0">
                                {isPhone ? <Smartphone className="w-5 h-5" /> : <Laptop className="w-5 h-5" />}
                              </div>
                              <div className="min-w-0">
                                <div className="font-semibold text-sm truncate">
                                  {session.deviceModel || session.appName || 'Telegram Client'}
                                </div>
                                <div className="text-xs text-gray-400 mt-0.5 truncate">
                                  {session.appName} {session.appVersion} • {session.platform || 'Client'}
                                </div>
                                <div className="text-[11px] text-gray-500 mt-0.5">
                                  {session.ip} {session.country ? `• ${session.country}` : ''}
                                  {session.dateActive
                                    ? ` • ${new Date(session.dateActive * 1000).toLocaleDateString()}`
                                    : ''}
                                </div>
                              </div>
                            </div>

                            <button
                              onClick={() => handleTerminateSession(hash)}
                              disabled={isTerminating}
                              className="px-3 py-1.5 rounded-lg bg-red-500/10 hover:bg-red-500/20 text-red-500 text-xs font-semibold shrink-0 transition flex items-center gap-1.5 disabled:opacity-50"
                              title={isAr ? 'إنهاء هذه الجلسة' : 'Terminate this session'}
                            >
                              {isTerminating ? (
                                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                              ) : (
                                <Trash2 className="w-3.5 h-3.5" />
                              )}
                              <span>{isAr ? 'إنهاء' : 'Terminate'}</span>
                            </button>
                          </div>
                        );
                      })}
                    </div>

                    {/* Automatic Termination TTL Setting */}
                    <div className="space-y-2 pt-3 border-t border-gray-800">
                      <div className="text-xs font-semibold text-gray-400 uppercase tracking-wider">
                        {isAr ? 'الإنهاء التلقائي للجلسات غير النشطة' : 'Automatically Terminate Old Sessions'}
                      </div>
                      <div className="grid grid-cols-4 gap-2">
                        {[
                          { label: isAr ? 'شهر' : '1 Mo', days: 30 },
                          { label: isAr ? '3 أشهر' : '3 Mo', days: 90 },
                          { label: isAr ? '6 أشهر' : '6 Mo', days: 180 },
                          { label: isAr ? 'سنة' : '1 Yr', days: 365 },
                        ].map((opt) => (
                          <button
                            key={opt.days}
                            onClick={() => handleUpdateTTL(opt.days)}
                            className={`py-2 px-1 text-xs font-medium rounded-xl border transition ${
                              authTtlDays === opt.days
                                ? 'bg-[#3390ec]/15 border-[#3390ec] text-[#3390ec]'
                                : themeConfig.isDark
                                ? 'bg-[#0e1621] border-gray-800 text-gray-400 hover:border-gray-700'
                                : 'bg-white border-gray-200 text-gray-600 hover:border-gray-300'
                            }`}
                          >
                            {opt.label}
                          </button>
                        ))}
                      </div>
                      <p className="text-[11px] text-gray-500">
                        {isAr
                          ? 'يتم إغلاق وتسجيل الخروج تلقائياً من أي جهاز لم يكن نشطاً خلال هذه المدة.'
                          : 'Automatically log out and terminate any sessions inactive for this period.'}
                      </p>
                    </div>
                  </div>
                );
              })()}
            </div>
          </div>
        )}

        {/* ========================================================================= */}
        {/* 11. SUB-VIEW: MTPROTO API INFO */}
        {/* ========================================================================= */}
        {activeSection === 'api-info' && (
          <div className="flex flex-col h-full overflow-hidden">
            <div className={`p-4 flex items-center justify-between border-b ${
              themeConfig.isDark ? 'border-gray-800 bg-[#17212b]' : 'border-gray-200 bg-white'
            }`}>
              <div className="flex items-center gap-3">
                <button
                  onClick={() => setActiveSection('menu')}
                  className="p-1.5 rounded-full hover:bg-gray-500/10 text-gray-400 hover:text-white transition"
                >
                  <ArrowLeft className="w-5 h-5 rtl:rotate-180" />
                </button>
                <h2 className="font-bold text-lg">{isAr ? 'بيانات API والاتصال' : 'Telegram MTProto API'}</h2>
              </div>
            </div>

            <div className="flex-1 overflow-y-auto p-4 space-y-4">
              <div className="p-3 bg-[#3390ec]/15 rounded-xl border border-[#3390ec]/30 flex items-center gap-3">
                <Shield className="w-6 h-6 text-[#3390ec] shrink-0" />
                <div className="text-xs leading-relaxed text-[#3390ec]">
                  {isAr
                    ? 'المشروع متصل رسمياً بسيرفرات تليجرام السحابية عبر بروتوكول MTProto v2.0 مع مفاتيح API الثابتة.'
                    : 'Officially connected to Telegram MTProto v2.0 servers using the fixed enterprise credentials.'}
                </div>
              </div>

              <div className="space-y-3 font-mono text-xs">
                <div className={`p-3 rounded-xl border ${themeConfig.isDark ? 'bg-[#0e1621] border-gray-700' : 'bg-gray-50 border-gray-300'}`}>
                  <div className="text-gray-400 text-[10px] uppercase">{isAr ? 'معرف التطبيق (API ID)' : 'TELEGRAM_API_ID'}</div>
                  <div className="font-bold text-white text-sm mt-0.5 select-all">22043994</div>
                </div>

                <div className={`p-3 rounded-xl border ${themeConfig.isDark ? 'bg-[#0e1621] border-gray-700' : 'bg-gray-50 border-gray-300'}`}>
                  <div className="text-gray-400 text-[10px] uppercase">{isAr ? 'هاش التطبيق (API HASH)' : 'TELEGRAM_API_HASH'}</div>
                  <div className="font-bold text-white text-xs mt-0.5 break-all select-all">
                    56f64582b363d367280db96586b97801
                  </div>
                </div>

                <div className={`p-3 rounded-xl border ${themeConfig.isDark ? 'bg-[#0e1621] border-gray-700' : 'bg-gray-50 border-gray-300'}`}>
                  <div className="text-gray-400 text-[10px] uppercase">{isAr ? 'بريد VAPID المعتمد' : 'VAPID SUBJECT'}</div>
                  <div className="font-bold text-white text-xs mt-0.5 select-all">
                    {VAPID_SUBJECT}
                  </div>
                </div>

                <div className={`p-3 rounded-xl border ${themeConfig.isDark ? 'bg-[#0e1621] border-gray-700' : 'bg-gray-50 border-gray-300'}`}>
                  <div className="text-gray-400 text-[10px] uppercase">{isAr ? 'بروتوكول وطبقة الاتصال' : 'PROTOCOL LAYER'}</div>
                  <div className="font-bold text-emerald-400 text-xs mt-0.5">
                    MTProto 2.0 (Layer 198) • Active & Synchronized
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </aside>

      {/* ========================================================================= */}
      {/* MODAL 1: ARCHIVED CHATS MODAL */}
      {/* ========================================================================= */}
      {showArchivedModal && (
        <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fadeIn">
          <div className={`w-full max-w-md rounded-2xl shadow-2xl p-5 border ${
            themeConfig.isDark ? 'bg-[#17212b] border-gray-700 text-white' : 'bg-white border-gray-200 text-gray-900'
          }`}>
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2 font-bold text-lg">
                <Archive className="w-5 h-5 text-[#3390ec]" />
                <span>{isAr ? 'المحادثات المؤرشفة' : 'Archived Chats'}</span>
              </div>
              <button onClick={() => setShowArchivedModal(false)} className="p-1 rounded-full hover:bg-gray-500/20">
                <X className="w-5 h-5" />
              </button>
            </div>

            {archivedChats.length === 0 ? (
              <div className="text-center py-8">
                <div className="w-14 h-14 rounded-full bg-gray-500/10 flex items-center justify-center mx-auto mb-3 text-gray-400">
                  <Archive className="w-7 h-7" />
                </div>
                <div className="font-semibold text-sm">{isAr ? 'لا توجد محادثات مؤرشفة' : 'No Archived Chats'}</div>
                <div className="text-xs text-gray-400 mt-1 max-w-xs mx-auto">
                  {isAr ? 'المحادثات التي تقوم بأرشفتها ستبقى مخفية هنا للحفاظ على تنظيم القائمة الرئيسية.' : 'Chats you archive will remain stored here to keep your main list clean.'}
                </div>
              </div>
            ) : (
              <div className="max-h-72 overflow-y-auto space-y-2">
                {archivedChats.map((chat) => (
                  <div
                    key={chat.id}
                    onClick={() => {
                      setShowArchivedModal(false);
                      onClose();
                      if (onSelectChat) onSelectChat(chat.id);
                    }}
                    className={`flex items-center justify-between p-2.5 rounded-xl cursor-pointer transition ${
                      themeConfig.isDark ? 'hover:bg-[#232e3c]' : 'hover:bg-gray-100'
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-full bg-[#3390ec] flex items-center justify-center font-bold text-white">
                        {chat.title.slice(0, 2)}
                      </div>
                      <div>
                        <div className="font-medium text-sm">{chat.title}</div>
                        <div className="text-xs text-gray-400 truncate max-w-[180px]">
                          {chat.lastMessage?.text || (isAr ? 'محادثة مؤرشفة' : 'Archived chat')}
                        </div>
                      </div>
                    </div>

                    {onToggleArchive && (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          onToggleArchive(chat.id);
                        }}
                        className="text-xs text-[#3390ec] hover:underline px-2 py-1"
                      >
                        {isAr ? 'إلغاء الأرشفة' : 'Unarchive'}
                      </button>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL 2: TELEGRAM FEATURES MODAL */}
      {/* ========================================================================= */}
      {showFeaturesModal && (
        <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fadeIn">
          <div className={`w-full max-w-md rounded-2xl shadow-2xl p-6 border ${
            themeConfig.isDark ? 'bg-[#17212b] border-gray-700 text-white' : 'bg-white border-gray-200 text-gray-900'
          }`}>
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2 font-bold text-lg">
                <Sparkles className="w-5 h-5 text-[#3390ec]" />
                <span>{isAr ? 'ميزات تليجرام ويب K' : 'Telegram Web K Features'}</span>
              </div>
              <button onClick={() => setShowFeaturesModal(false)} className="p-1 rounded-full hover:bg-gray-500/20">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-3 text-xs leading-relaxed">
              <div className="p-3 rounded-xl bg-[#3390ec]/10 border border-[#3390ec]/20 flex items-start gap-2.5">
                <CheckCircle2 className="w-4 h-4 text-[#3390ec] shrink-0 mt-0.5" />
                <div>
                  <div className="font-bold text-sm text-[#3390ec]">{isAr ? 'سرعة فائقة وخفة تامة' : 'Ultra-Fast Performance'}</div>
                  <div className="text-gray-300 mt-0.5">{isAr ? 'نسخة K تم بناؤها لتعمل بأقصى سرعة ممكنة مع سلاسة الرسوم المتحركة 60fps.' : 'Version K is built for light, responsive 60fps speed and low bandwidth usage.'}</div>
                </div>
              </div>

              <div className="p-3 rounded-xl bg-purple-500/10 border border-purple-500/20 flex items-start gap-2.5">
                <Camera className="w-4 h-4 text-purple-400 shrink-0 mt-0.5" />
                <div>
                  <div className="font-bold text-sm text-purple-400">{isAr ? 'القصص التفاعلية (Stories)' : 'Interactive Stories'}</div>
                  <div className="text-gray-300 mt-0.5">{isAr ? 'شارك لحظاتك اليومية مع جهات اتصالك بدقة عالية وخصوصية مخصصة.' : 'Share moments with your contacts with rich privacy controls.'}</div>
                </div>
              </div>

              <div className="p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-start gap-2.5">
                <Cpu className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                <div>
                  <div className="font-bold text-sm text-emerald-400">{isAr ? 'تطبيقات البوت المصغرة (Mini Apps)' : 'Bot Mini Apps & JS Bridge'}</div>
                  <div className="text-gray-300 mt-0.5">{isAr ? 'دعم كامل لمنصة Telegram WebApp SDK والتطبيقات المدمجة داخل المحادثات.' : 'Full support for Telegram WebApp JS SDK and in-chat Mini Apps.'}</div>
                </div>
              </div>
            </div>

            <button
              onClick={() => setShowFeaturesModal(false)}
              className="w-full mt-4 py-2.5 bg-[#3390ec] text-white rounded-xl text-xs font-semibold"
            >
              {isAr ? 'حسناً، رائع!' : 'Got it!'}
            </button>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL 3: REPORT BUG */}
      {/* ========================================================================= */}
      {showBugModal && (
        <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fadeIn">
          <div className={`w-full max-w-md rounded-2xl shadow-2xl p-6 border ${
            themeConfig.isDark ? 'bg-[#17212b] border-gray-700 text-white' : 'bg-white border-gray-200 text-gray-900'
          }`}>
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2 font-bold text-lg text-amber-400">
                <AlertCircle className="w-5 h-5" />
                <span>{isAr ? 'الإبلاغ عن مشكلة' : 'Report a Bug'}</span>
              </div>
              <button onClick={() => setShowBugModal(false)} className="p-1 rounded-full hover:bg-gray-500/20">
                <X className="w-5 h-5" />
              </button>
            </div>

            {bugSent ? (
              <div className="text-center py-6">
                <CheckCircle2 className="w-12 h-12 text-emerald-400 mx-auto mb-2" />
                <div className="font-bold text-sm">{isAr ? 'شكراً لك! تم استلام بلاغك بنجاح' : 'Thank you! Report received.'}</div>
                <div className="text-xs text-gray-400 mt-1">{isAr ? 'يعمل فريق تليجرام على تحسين التجربة باستمرار.' : 'We are constantly improving Telegram Web K.'}</div>
              </div>
            ) : (
              <div className="space-y-3">
                <p className="text-xs text-gray-400">
                  {isAr ? 'يرجى وصف المشكلة أو السلوك غير المتوقع لمساعدتنا في إصلاحه:' : 'Please describe the bug or unexpected behavior:'}
                </p>
                <textarea
                  rows={4}
                  value={bugText}
                  onChange={(e) => setBugText(e.target.value)}
                  placeholder={isAr ? 'اكتب تفاصيل المشكلة هنا...' : 'Describe what happened...'}
                  className={`w-full p-3 text-xs rounded-xl border resize-none focus:outline-none focus:border-[#3390ec] ${
                    themeConfig.isDark ? 'bg-[#0e1621] border-gray-700 text-white' : 'bg-gray-50 border-gray-300 text-gray-900'
                  }`}
                />
                <button
                  onClick={() => {
                    if (!bugText.trim()) return;
                    setBugSent(true);
                    setTimeout(() => {
                      setBugSent(false);
                      setBugText('');
                      setShowBugModal(false);
                    }, 1800);
                  }}
                  className="w-full py-2.5 bg-[#3390ec] text-white rounded-xl text-xs font-semibold transition"
                >
                  {isAr ? 'إرسال البلاغ' : 'Submit Report'}
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL 4: LINK QR CODE MODAL */}
      {/* ========================================================================= */}
      {showLinkQrModal && (
        <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fadeIn">
          <div className={`w-full max-w-sm rounded-2xl shadow-2xl p-6 text-center border ${
            themeConfig.isDark ? 'bg-[#17212b] border-gray-700 text-white' : 'bg-white border-gray-200 text-gray-900'
          }`}>
            <div className="flex justify-end">
              <button onClick={() => setShowLinkQrModal(false)} className="p-1 rounded-full hover:bg-gray-500/20">
                <X className="w-5 h-5" />
              </button>
            </div>

            <h3 className="font-bold text-lg mt-1">{isAr ? 'مسح رمز الاستجابة السريعة' : 'Scan QR Code'}</h3>
            <p className="text-xs text-gray-400 mt-1 mb-4">
              {isAr ? 'افتح تطبيق تليجرام في هاتفك -> الإعدادات -> الأجهزة -> ربط جهاز' : 'Open Telegram on your phone -> Settings -> Devices -> Link Desktop Device'}
            </p>

            <div className="w-52 h-52 mx-auto bg-white p-3 rounded-2xl shadow-lg flex items-center justify-center">
              <svg viewBox="0 0 100 100" className="w-full h-full text-[#17212b]">
                <path
                  d="M10,10 h30 v30 h-30 z M15,15 v20 h20 v-20 z M20,20 h10 v10 h-10 z M60,10 h30 v30 h-30 z M65,15 v20 h20 v-20 z M70,20 h10 v10 h-10 z M10,60 h30 v30 h-30 z M15,65 v20 h20 v-20 z M20,70 h10 v10 h-10 z M48,15 h4 v15 h-4 z M48,35 h4 v4 h-4 z M60,60 h10 v10 h-10 z M75,60 h15 v4 h-15 z M60,75 h6 v15 h-6 z M75,75 h15 v15 h-15 z"
                  fill="currentColor"
                />
                <circle cx="50" cy="50" r="8" fill="#3390ec" />
              </svg>
            </div>

            <button
              onClick={() => setShowLinkQrModal(false)}
              className="mt-5 w-full py-2.5 bg-[#3390ec] text-white rounded-xl text-xs font-semibold"
            >
              {isAr ? 'إغلاق' : 'Close'}
            </button>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL 5: CREATE FOLDER MODAL */}
      {/* ========================================================================= */}
      {showNewFolderModal && (
        <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fadeIn">
          <div className={`w-full max-w-sm rounded-2xl shadow-2xl p-5 border ${
            themeConfig.isDark ? 'bg-[#17212b] border-gray-700 text-white' : 'bg-white border-gray-200 text-gray-900'
          }`}>
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-bold text-base">{isAr ? 'مجلد محادثات جديد' : 'New Chat Folder'}</h3>
              <button onClick={() => setShowNewFolderModal(false)} className="p-1 rounded-full hover:bg-gray-500/20">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-3">
              <div>
                <label className="block text-xs text-gray-400 mb-1">{isAr ? 'اسم المجلد' : 'Folder Name'}</label>
                <input
                  type="text"
                  value={newFolderName}
                  onChange={(e) => setNewFolderName(e.target.value)}
                  placeholder={isAr ? 'مثال: العمل، الأصدقاء' : 'e.g., Work, Friends'}
                  className={`w-full px-3 py-2 text-xs rounded-xl border focus:outline-none focus:border-[#3390ec] ${
                    themeConfig.isDark ? 'bg-[#0e1621] border-gray-700 text-white' : 'bg-gray-50 border-gray-300 text-gray-900'
                  }`}
                />
              </div>

              <div className="flex gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowNewFolderModal(false)}
                  className="flex-1 py-2 rounded-xl text-xs font-medium border border-gray-600 text-gray-400 hover:bg-white/5"
                >
                  {isAr ? 'إلغاء' : 'Cancel'}
                </button>
                <button
                  type="button"
                  onClick={handleCreateFolder}
                  className="flex-1 py-2 bg-[#3390ec] text-white rounded-xl text-xs font-semibold hover:bg-[#2b7ec9]"
                >
                  {isAr ? 'إنشاء' : 'Create'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL 6: LOGOUT CONFIRMATION MODAL */}
      {/* ========================================================================= */}
      {showLogoutModal && (
        <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fadeIn">
          <div className={`w-full max-w-sm rounded-2xl shadow-2xl p-6 border ${
            themeConfig.isDark ? 'bg-[#17212b] border-gray-700 text-white' : 'bg-white border-gray-200 text-gray-900'
          }`}>
            <div className="w-12 h-12 rounded-full bg-red-500/10 text-red-500 flex items-center justify-center mx-auto mb-3">
              <LogOut className="w-6 h-6" />
            </div>

            <h3 className="font-bold text-base text-center">{isAr ? 'تسجيل الخروج من تليجرام' : 'Log Out from Telegram'}</h3>
            <p className="text-xs text-gray-400 text-center mt-1 mb-5">
              {isAr
                ? 'هل أنت متأكد من رغبتك في تسجيل الخروج؟ ستتمكن من تسجيل الدخول مرة أخرى في أي وقت.'
                : 'Are you sure you want to log out? You can log back in anytime.'}
            </p>

            <div className="flex gap-2">
              <button
                onClick={() => setShowLogoutModal(false)}
                className="flex-1 py-2.5 rounded-xl text-xs font-medium border border-gray-600 text-gray-300 hover:bg-white/5"
              >
                {isAr ? 'إلغاء' : 'Cancel'}
              </button>
              <button
                onClick={() => {
                  setShowLogoutModal(false);
                  onClose();
                  onLogout();
                }}
                className="flex-1 py-2.5 bg-red-500 hover:bg-red-600 text-white rounded-xl text-xs font-semibold shadow-md shadow-red-500/20"
              >
                {isAr ? 'تسجيل الخروج' : 'Log Out'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export { PrivacySettings } from './PrivacySettings';
export default SettingsDrawer;
