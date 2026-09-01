import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  Settings,
  Users,
  PlayCircle,
  MessageSquare,
  ListOrdered,
  Menu,
  User,
  Bell,
  Lock,
  LayoutGrid,
  Download,
  Megaphone,
  ArrowRight,
  ArrowLeft,
  Check,
  Shield,
  Eye,
  EyeOff,
  Sparkles,
  Smartphone,
  FolderDown,
  UploadCloud,
  FileJson,
  CheckCircle2,
  RefreshCw,
  Volume2,
  Sliders,
  Type,
  Radio,
  Share2,
  Zap,
  Globe,
  HardDrive,
  Copy,
  Folder,
} from 'lucide-react';
import { PlusConfig, SettingsSubPage } from '../../types';
import { useTelegram } from '../../context/TelegramContext';

export const DEFAULT_PLUS_CONFIG: PlusConfig = {
  // 1. General
  fontFamily: 'default',
  keepScreenOn: true,
  proximitySensor: true,
  useExternalBrowser: false,
  hapticFeedback: true,
  bigEmojis: true,
  showDirectShare: true,
  cacheLimitGb: 5,

  // 2. Chats
  tabsEnabled: true,
  tabsPosition: 'top',
  showUnreadTabsCounter: true,
  hideMutedTabs: false,
  showOnlineStatusDot: true,
  doubleTapAction: 'reaction',
  chatSwipeAction: 'archive',
  confirmBeforeCall: true,

  // 3. Stories
  hideStoriesBar: false,
  stealthModeStories: true,
  autoSaveStories: false,
  highQualityPlayback: true,
  storySpeed: '1x',
  storyExpirationAlert: true,

  // 4. Messages
  forwardWithoutQuote: true,
  showUserIdOnMessages: true,
  showExactSeconds: true,
  showEditedHistory: true,
  confirmVoiceNotes: false,
  confirmStickers: false,
  autoTranslateIncoming: true,
  translationProvider: 'telegram',

  // 5. Topics
  topicsAsTabs: true,
  autoOpenGeneralTopic: true,
  unreadTopicBadges: true,
  quickTopicSearch: true,
  lastTopicMessagePreview: true,

  // 6. Navigation Drawer
  drawerShowNightMode: true,
  drawerShowSavedMessages: true,
  drawerShowCalls: true,
  drawerShowContacts: true,
  drawerShowPlusSettings: true,
  drawerShowAccounts: true,
  drawerHeaderStyle: 'standard',

  // 7. Profile
  profileShowUserId: true,
  profileCopyIdOnTap: true,
  profileShowCommonGroups: true,
  profileHidePhone: false,
  profileQuickActions: true,

  // 8. Notifications
  inAppNotificationStyle: 'banner',
  repeatUnreadAlerts: 'off',
  customPrivateTone: 'default',
  customGroupTone: 'default',
  vipPriorityAlerts: true,
  filterSpamAlerts: true,

  // 9. Privacy & Security
  ghostMode: false,
  hideOnlineStatus: false,
  hideReadReceipts: false,
  hideTypingIndicator: false,
  antiDeleteMessages: true,
  antiEditMessages: true,
  appLockPasscode: '',
  isAppLockEnabled: false,
  biometricsEnabled: true,
  hiddenChatsLocked: false,

  // 10. Shared Media
  defaultMediaTab: 'photos',
  gridColumnsCount: 3,
  highResThumbnailPreview: true,
  pipFloatingVideo: true,
  autoPauseAudioOnVideo: true,
  customMediaPath: '/Telegram/Media',

  // 11. Downloads
  autoDownloadWifi: true,
  autoDownloadCellular: false,
  downloadBooster: true,
  maxConcurrentDownloads: 4,
  downloadFinishSound: true,
  autoResumeDownloads: true,

  // 12. Ads
  blockSponsoredMessages: true,
  hidePromotedChannels: true,
  blockBotAds: true,
  disablePromoAlerts: true,
};

export const usePlusConfig = () => {
  const [config, setConfig] = useState<PlusConfig>(() => {
    try {
      const saved = localStorage.getItem('tg_plus_config_v2');
      if (saved) {
        return { ...DEFAULT_PLUS_CONFIG, ...JSON.parse(saved) };
      }
    } catch (e) {
      console.error('Failed to load plus config', e);
    }
    return DEFAULT_PLUS_CONFIG;
  });

  const updateConfig = (patch: Partial<PlusConfig>) => {
    setConfig((prev) => {
      const next = { ...prev, ...patch };
      try {
        localStorage.setItem('tg_plus_config_v2', JSON.stringify(next));
      } catch (e) {
        console.error('Failed to save plus config', e);
      }
      return next;
    });
  };

  const resetConfig = () => {
    setConfig(DEFAULT_PLUS_CONFIG);
    try {
      localStorage.setItem('tg_plus_config_v2', JSON.stringify(DEFAULT_PLUS_CONFIG));
    } catch {}
  };

  return { config, updateConfig, resetConfig, setConfig };
};

// Reusable Header Component
const SubPageHeader: React.FC<{ title: string; onBack: () => void }> = ({ title, onBack }) => {
  const { settings } = useTelegram();
  const isArabic = settings.language === 'ar';
  const BackIcon = isArabic ? ArrowRight : ArrowLeft;

  return (
    <div className="flex items-center gap-3 px-4 py-3.5 bg-[#2481cc] text-white shrink-0 shadow-md">
      <button
        onClick={onBack}
        className="p-1.5 rounded-full hover:bg-white/15 transition-colors"
        title={isArabic ? 'رجوع' : 'Back'}
      >
        <BackIcon className="w-5 h-5" />
      </button>
      <span className="font-bold text-base">{title}</span>
    </div>
  );
};

// Reusable Toggle Item
const ToggleRow: React.FC<{
  title: string;
  subtitle?: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
  icon?: React.ReactNode;
}> = ({ title, subtitle, checked, onChange, icon }) => (
  <div
    onClick={() => onChange(!checked)}
    className="flex items-center justify-between p-3.5 bg-[#17212b] hover:bg-white/5 transition-colors cursor-pointer border-b border-white/5"
  >
    <div className="flex items-center gap-3 flex-1 pr-2">
      {icon && <div className="text-cyan-400 shrink-0">{icon}</div>}
      <div>
        <div className="text-xs font-semibold text-white">{title}</div>
        {subtitle && <div className="text-[11px] text-gray-400 mt-0.5 leading-snug">{subtitle}</div>}
      </div>
    </div>
    <div className="relative inline-flex items-center cursor-pointer shrink-0">
      <input
        type="checkbox"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
        className="sr-only peer"
      />
      <div className="w-10 h-5 bg-gray-700 peer-focus:outline-hidden rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-[#2481cc]"></div>
    </div>
  </div>
);

// Reusable Select Item
const SelectRow: React.FC<{
  title: string;
  value: string;
  options: { label: string; value: string }[];
  onChange: (value: string) => void;
  icon?: React.ReactNode;
}> = ({ title, value, options, onChange, icon }) => (
  <div className="p-3.5 bg-[#17212b] border-b border-white/5 space-y-2">
    <div className="flex items-center gap-2">
      {icon && <div className="text-cyan-400 shrink-0">{icon}</div>}
      <span className="text-xs font-semibold text-white">{title}</span>
    </div>
    <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 pt-1">
      {options.map((opt) => {
        const isSelected = opt.value === value;
        return (
          <button
            key={opt.value}
            onClick={() => onChange(opt.value)}
            className={`px-3 py-2 rounded-xl text-xs font-medium transition-all text-center border ${
              isSelected
                ? 'bg-[#2481cc] text-white border-[#2481cc] shadow-sm'
                : 'bg-[#0e1621] text-gray-300 border-white/10 hover:border-white/20'
            }`}
          >
            {opt.label}
          </button>
        );
      })}
    </div>
  </div>
);

// =========================================================================
// 1. PLUS SETTINGS MAIN MENU (Exact UI from Screenshot 204101 & 204106)
// =========================================================================
export const PlusSettingsMainView: React.FC<{
  onNavigate: (page: SettingsSubPage) => void;
  onBack: () => void;
}> = ({ onNavigate, onBack }) => {
  const { settings, showToast } = useTelegram();
  const isArabic = settings.language === 'ar';
  const { config, setConfig } = usePlusConfig();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const sections: { id: SettingsSubPage; title: string; icon: React.ReactNode }[] = [
    { id: 'plus_general', title: isArabic ? 'عام' : 'General', icon: <Settings className="w-5 h-5 text-cyan-400" /> },
    { id: 'plus_chats', title: isArabic ? 'المحادثات' : 'Chats', icon: <Users className="w-5 h-5 text-cyan-400" /> },
    { id: 'plus_stories', title: isArabic ? 'القصص' : 'Stories', icon: <PlayCircle className="w-5 h-5 text-cyan-400" /> },
    { id: 'plus_messages', title: isArabic ? 'الرسائل' : 'Messages', icon: <MessageSquare className="w-5 h-5 text-cyan-400" /> },
    { id: 'plus_topics', title: isArabic ? 'Topics' : 'Topics', icon: <ListOrdered className="w-5 h-5 text-cyan-400" /> },
    { id: 'plus_drawer', title: isArabic ? 'درج التصفح' : 'Navigation Drawer', icon: <Menu className="w-5 h-5 text-cyan-400" /> },
    { id: 'plus_profile', title: isArabic ? 'الملف الشخصي' : 'Profile', icon: <User className="w-5 h-5 text-cyan-400" /> },
    { id: 'plus_notifications', title: isArabic ? 'الإشعارات' : 'Notifications', icon: <Bell className="w-5 h-5 text-cyan-400" /> },
    { id: 'plus_privacy', title: isArabic ? 'الخصوصية والأمان' : 'Privacy and Security', icon: <Lock className="w-5 h-5 text-cyan-400" /> },
    { id: 'plus_media', title: isArabic ? 'الوسائط المتبادلة' : 'Shared Media', icon: <LayoutGrid className="w-5 h-5 text-cyan-400" /> },
    { id: 'plus_downloads', title: isArabic ? 'التحميلات' : 'Downloads', icon: <Download className="w-5 h-5 text-cyan-400" /> },
    { id: 'plus_ads', title: isArabic ? 'Ads' : 'Ads', icon: <Megaphone className="w-5 h-5 text-cyan-400" /> },
  ];

  // Save Settings to JSON
  const handleSaveSettings = () => {
    try {
      const dataStr = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(config, null, 2));
      const downloadAnchor = document.createElement('a');
      downloadAnchor.setAttribute('href', dataStr);
      downloadAnchor.setAttribute('download', `Telegram_PlusSettings_${new Date().toISOString().slice(0, 10)}.json`);
      document.body.appendChild(downloadAnchor);
      downloadAnchor.click();
      downloadAnchor.remove();

      localStorage.setItem('tg_plus_backup_latest', JSON.stringify(config));
      showToast(isArabic ? "تم حفظ الإعدادات في مجلد 'Telegram' بنجاح" : "Settings saved to '/Telegram' backup folder", '💾');
    } catch (e) {
      showToast(isArabic ? 'فشل حفظ الإعدادات' : 'Failed to save settings', '⚠️');
    }
  };

  // Restore Settings from JSON
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const parsed = JSON.parse(event.target?.result as string);
        setConfig((prev) => {
          const next = { ...prev, ...parsed };
          localStorage.setItem('tg_plus_config_v2', JSON.stringify(next));
          return next;
        });
        showToast(isArabic ? 'تمت استعادة كافة إعدادات بلاس بنجاح!' : 'Plus Settings restored successfully!', '✅');
      } catch (err) {
        showToast(isArabic ? 'ملف إعدادات غير صالح' : 'Invalid settings JSON file', '❌');
      }
    };
    reader.readAsText(file);
  };

  const handleRestoreClick = () => {
    // If we have latest backup in localStorage, give immediate restore or file picker
    const savedBackup = localStorage.getItem('tg_plus_backup_latest');
    if (savedBackup) {
      try {
        const parsed = JSON.parse(savedBackup);
        setConfig(parsed);
        localStorage.setItem('tg_plus_config_v2', JSON.stringify(parsed));
        showToast(isArabic ? 'تمت استعادة الإعدادات المحفوظة من الجهاز' : 'Settings restored from local backup', '🔄');
        return;
      } catch {}
    }
    fileInputRef.current?.click();
  };

  return (
    <div className="flex flex-col h-full overflow-hidden bg-[#0e1621]">
      <SubPageHeader title={isArabic ? 'إعدادات بلاس' : 'Plus Settings'} onBack={onBack} />

      {/* Hidden file input for restore */}
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFileChange}
        accept=".json"
        className="hidden"
      />

      <div className="flex-1 overflow-y-auto divide-y divide-white/5 bg-[#17212b]">
        {sections.map((sec) => (
          <div
            key={sec.id}
            onClick={() => onNavigate(sec.id)}
            className="flex items-center gap-4 px-4 py-3.5 hover:bg-white/5 cursor-pointer transition-colors"
          >
            {sec.icon}
            <span className="text-[13.5px] font-medium text-white">{sec.title}</span>
          </div>
        ))}

        {/* Save & Restore Section (Exact layout from Screenshot 204106) */}
        <div className="p-4 bg-[#0e1621] space-y-3">
          <div
            onClick={handleSaveSettings}
            className="p-3 bg-[#17212b] rounded-xl border border-white/10 cursor-pointer hover:bg-white/5 transition-colors"
          >
            <div className="text-xs font-bold text-white flex items-center justify-between">
              <span>{isArabic ? 'حفظ الإعدادات' : 'Save Settings'}</span>
              <FolderDown className="w-4 h-4 text-[#5288c1]" />
            </div>
            <div className="text-[11px] text-gray-400 mt-0.5">
              {isArabic ? "مجلد حفظ الاعدادات هي (المفضلات أيضاً) 'Telegram'" : "Saved to '/Telegram' storage folder"}
            </div>
          </div>

          <div
            onClick={handleRestoreClick}
            className="p-3 bg-[#17212b] rounded-xl border border-white/10 cursor-pointer hover:bg-white/5 transition-colors"
          >
            <div className="text-xs font-bold text-white flex items-center justify-between">
              <span>{isArabic ? 'إستعادة الإعدادات' : 'Restore Settings'}</span>
              <UploadCloud className="w-4 h-4 text-[#5288c1]" />
            </div>
            <div className="text-[11px] text-gray-400 mt-0.5">
              {isArabic ? 'إستعادة الإعدادات من الجهاز' : 'Restore settings from local device backup'}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

// =========================================================================
// 2. PLUS GENERAL VIEW (عام)
// =========================================================================
export const PlusGeneralView: React.FC<{ onBack: () => void }> = ({ onBack }) => {
  const { settings, showToast } = useTelegram();
  const isArabic = settings.language === 'ar';
  const { config, updateConfig } = usePlusConfig();

  return (
    <div className="flex flex-col h-full overflow-hidden bg-[#0e1621]">
      <SubPageHeader title={isArabic ? 'عام' : 'General'} onBack={onBack} />
      <div className="flex-1 overflow-y-auto divide-y divide-white/5">
        <SelectRow
          title={isArabic ? 'نوع الخط في التطبيق' : 'App Font Family'}
          value={config.fontFamily}
          options={[
            { label: isArabic ? 'افتراضي النظام' : 'System Default', value: 'default' },
            { label: 'Cairo', value: 'cairo' },
            { label: 'IBM Plex Sans', value: 'ibm' },
            { label: 'Roboto', value: 'roboto' },
          ]}
          onChange={(v) => {
            updateConfig({ fontFamily: v });
            showToast(isArabic ? 'تم تغيير نوع الخط' : 'Font updated', '🔤');
          }}
          icon={<Type className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'إبقاء الشاشة مضاءة دائماً' : 'Keep Screen On'}
          subtitle={isArabic ? 'منع قفل الشاشة التلقائي أثناء استخدام التطبيق' : 'Prevent screen timeout during app use'}
          checked={config.keepScreenOn}
          onChange={(v) => updateConfig({ keepScreenOn: v })}
          icon={<Smartphone className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'مستشعر القرب للرسائل الصوتية' : 'Proximity Sensor for Voice'}
          subtitle={isArabic ? 'التحويل التلقائي لسماعة الأذن عند تقريب الهاتف' : 'Auto switch to ear-piece on ear proximity'}
          checked={config.proximitySensor}
          onChange={(v) => updateConfig({ proximitySensor: v })}
          icon={<Volume2 className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'استخدام متصفح خارجي' : 'Open in External Browser'}
          subtitle={isArabic ? 'فتح الروابط مباشرة في المتصفح الافتراضي للجهاز' : 'Directly open links in default browser'}
          checked={config.useExternalBrowser}
          onChange={(v) => updateConfig({ useExternalBrowser: v })}
          icon={<Globe className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'الاهتزاز عند اللمس والتفاعل' : 'Haptic Touch Feedback'}
          subtitle={isArabic ? 'استجابة اهتزازية خفيفة عند النقر على الأزرار' : 'Vibrate slightly on tap interactions'}
          checked={config.hapticFeedback}
          onChange={(v) => updateConfig({ hapticFeedback: v })}
          icon={<Zap className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'عرض الرموز التعبيرية بحجم كبير' : 'Big Emojis in Messages'}
          subtitle={isArabic ? 'تكبير حجم الإيموجي الفردي في الرسائل' : 'Render standalone emojis at large scale'}
          checked={config.bigEmojis}
          onChange={(v) => updateConfig({ bigEmojis: v })}
          icon={<Sparkles className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'عرض زر المشاركة السريع' : 'Direct Share Button'}
          subtitle={isArabic ? 'إظهار سهم المشاركة السريعة بجانب كل رسالة' : 'Show quick share action next to bubbles'}
          checked={config.showDirectShare}
          onChange={(v) => updateConfig({ showDirectShare: v })}
          icon={<Share2 className="w-4 h-4" />}
        />

        <div className="p-4 bg-[#17212b] space-y-2">
          <div className="text-xs font-semibold text-white flex justify-between">
            <span>{isArabic ? 'سقف حجم الذاكرة المؤقتة' : 'Max Cache Size Limit'}</span>
            <span className="text-cyan-400 font-mono">{config.cacheLimitGb} GB</span>
          </div>
          <input
            type="range"
            min={1}
            max={20}
            value={config.cacheLimitGb}
            onChange={(e) => updateConfig({ cacheLimitGb: Number(e.target.value) })}
            className="w-full accent-[#2481cc]"
          />
        </div>
      </div>
    </div>
  );
};

// =========================================================================
// 3. PLUS CHATS VIEW (المحادثات)
// =========================================================================
export const PlusChatsView: React.FC<{ onBack: () => void }> = ({ onBack }) => {
  const { settings } = useTelegram();
  const isArabic = settings.language === 'ar';
  const { config, updateConfig } = usePlusConfig();

  return (
    <div className="flex flex-col h-full overflow-hidden bg-[#0e1621]">
      <SubPageHeader title={isArabic ? 'المحادثات' : 'Chats'} onBack={onBack} />
      <div className="flex-1 overflow-y-auto divide-y divide-white/5">
        <ToggleRow
          title={isArabic ? 'تفعيل شريط التبويبات' : 'Enable Categorized Tabs'}
          subtitle={isArabic ? 'عرض تبويبات المحادثات (المستخدمين، المجموعات، القنوات، البوتات)' : 'Show categorized tabs (Users, Groups, Channels, Bots)'}
          checked={config.tabsEnabled}
          onChange={(v) => updateConfig({ tabsEnabled: v })}
          icon={<LayoutGrid className="w-4 h-4" />}
        />

        <SelectRow
          title={isArabic ? 'موضع شريط التبويبات' : 'Tabs Bar Position'}
          value={config.tabsPosition}
          options={[
            { label: isArabic ? 'أعلى الشاشة' : 'Top', value: 'top' },
            { label: isArabic ? 'أسفل الشاشة' : 'Bottom', value: 'bottom' },
          ]}
          onChange={(v) => updateConfig({ tabsPosition: v as any })}
          icon={<Sliders className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'إظهار عداد غير المقروء على التبويبات' : 'Unread Counter Badges'}
          subtitle={isArabic ? 'عرض عدد الرسائل غير المقروءة فوق كل تبويب' : 'Show badge counts on each tab'}
          checked={config.showUnreadTabsCounter}
          onChange={(v) => updateConfig({ showUnreadTabsCounter: v })}
          icon={<Bell className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'إخفاء المحادثات المكتومة من التبويبات' : 'Hide Muted Chats from Tabs'}
          subtitle={isArabic ? 'عدم احتساب إشعارات المحادثات المكتومة في الشارة' : 'Exclude muted chats from unread calculation'}
          checked={config.hideMutedTabs}
          onChange={(v) => updateConfig({ hideMutedTabs: v })}
          icon={<EyeOff className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'إظهار نقطة متصل الآن على الصورة' : 'Show Online Status Dot on Avatars'}
          subtitle={isArabic ? 'نقطة خضراء دائرية بجانب صورة المستخدم المتصل' : 'Green badge on avatar for online users'}
          checked={config.showOnlineStatusDot}
          onChange={(v) => updateConfig({ showOnlineStatusDot: v })}
          icon={<Radio className="w-4 h-4" />}
        />

        <SelectRow
          title={isArabic ? 'إجراء الضغط المزدوج على الرسالة' : 'Double Tap Message Action'}
          value={config.doubleTapAction}
          options={[
            { label: isArabic ? 'إضافة تفاعل سريع' : 'Quick Reaction', value: 'reaction' },
            { label: isArabic ? 'رد سريع' : 'Reply', value: 'reply' },
            { label: isArabic ? 'نسخ النص' : 'Copy Text', value: 'copy' },
            { label: isArabic ? 'تثبيت' : 'Pin', value: 'pin' },
          ]}
          onChange={(v) => updateConfig({ doubleTapAction: v as any })}
          icon={<Zap className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'تأكيد قبل إجراء المكالمة' : 'Confirm Before Calling'}
          subtitle={isArabic ? 'عرض نافذة تأكيد لمنع الاتصال العرضي' : 'Show prompt before placing voice/video calls'}
          checked={config.confirmBeforeCall}
          onChange={(v) => updateConfig({ confirmBeforeCall: v })}
          icon={<Shield className="w-4 h-4" />}
        />
      </div>
    </div>
  );
};

// =========================================================================
// 4. PLUS STORIES VIEW (القصص)
// =========================================================================
export const PlusStoriesView: React.FC<{ onBack: () => void }> = ({ onBack }) => {
  const { settings } = useTelegram();
  const isArabic = settings.language === 'ar';
  const { config, updateConfig } = usePlusConfig();

  return (
    <div className="flex flex-col h-full overflow-hidden bg-[#0e1621]">
      <SubPageHeader title={isArabic ? 'القصص' : 'Stories'} onBack={onBack} />
      <div className="flex-1 overflow-y-auto divide-y divide-white/5">
        <ToggleRow
          title={isArabic ? 'الوضع الخفي لمشاهدة القصص (Incognito)' : 'Stealth Mode for Viewing Stories'}
          subtitle={isArabic ? 'مشاهدة قصص جهات الاتصال دون تسجيل اسمك في المشاهدات' : 'View stories without appearing in the viewer list'}
          checked={config.stealthModeStories}
          onChange={(v) => updateConfig({ stealthModeStories: v })}
          icon={<EyeOff className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'إخفاء شريط القصص من الشاشة الرئيسية' : 'Hide Stories Header Bar'}
          subtitle={isArabic ? 'توفير مساحة لقائمة المحادثات' : 'Save space in main chat list'}
          checked={config.hideStoriesBar}
          onChange={(v) => updateConfig({ hideStoriesBar: v })}
          icon={<LayoutGrid className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'حفظ القصص تلقائياً إلى المعرض' : 'Auto-Save Stories to Gallery'}
          subtitle={isArabic ? 'تنزيل الصور ومقاطع الفيديو من القصص إلى الذاكرة' : 'Save story media directly to device gallery'}
          checked={config.autoSaveStories}
          onChange={(v) => updateConfig({ autoSaveStories: v })}
          icon={<Download className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'تشغيل القصص بأقصى دقة فائقة' : 'High Quality Story Playback'}
          subtitle={isArabic ? 'تحميل القصص بجودة Full HD دون ضغط إضافي' : 'Load stories at maximum crisp resolution'}
          checked={config.highQualityPlayback}
          onChange={(v) => updateConfig({ highQualityPlayback: v })}
          icon={<Sparkles className="w-4 h-4" />}
        />

        <SelectRow
          title={isArabic ? 'سرعة تشغيل الفيديو في القصص' : 'Story Video Speed'}
          value={config.storySpeed}
          options={[
            { label: '1.0x (طبيعي)', value: '1x' },
            { label: '1.5x (سريع)', value: '1.5x' },
            { label: '2.0x (مضاعف)', value: '2x' },
          ]}
          onChange={(v) => updateConfig({ storySpeed: v as any })}
          icon={<PlayCircle className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'تنبيه قبل انتهاء صلاحية القصة' : 'Story Expiration Alert'}
          subtitle={isArabic ? 'إشعار قبل اختفاء قصتك بـ ساعتين' : 'Notify 2 hours before your story expires'}
          checked={config.storyExpirationAlert}
          onChange={(v) => updateConfig({ storyExpirationAlert: v })}
          icon={<Bell className="w-4 h-4" />}
        />
      </div>
    </div>
  );
};

// =========================================================================
// 5. PLUS MESSAGES VIEW (الرسائل)
// =========================================================================
export const PlusMessagesView: React.FC<{ onBack: () => void }> = ({ onBack }) => {
  const { settings } = useTelegram();
  const isArabic = settings.language === 'ar';
  const { config, updateConfig } = usePlusConfig();

  return (
    <div className="flex flex-col h-full overflow-hidden bg-[#0e1621]">
      <SubPageHeader title={isArabic ? 'الرسائل' : 'Messages'} onBack={onBack} />
      <div className="flex-1 overflow-y-auto divide-y divide-white/5">
        <ToggleRow
          title={isArabic ? 'إعادة التوجيه بدون اقتباس' : 'Forward Without Quoting'}
          subtitle={isArabic ? 'إخفاء اسم المرسل والمصدر الأصلي عند إعادة التوجيه' : 'Remove sender attribution upon forwarding'}
          checked={config.forwardWithoutQuote}
          onChange={(v) => updateConfig({ forwardWithoutQuote: v })}
          icon={<Share2 className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'إظهار معرف المستخدم (User ID)' : 'Show Numerical User ID'}
          subtitle={isArabic ? 'عرض رقم ID الخاص بالمرسل في معلومات الرسالة' : 'Display numeric account ID in message header'}
          checked={config.showUserIdOnMessages}
          onChange={(v) => updateConfig({ showUserIdOnMessages: v })}
          icon={<User className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'عرض الوقت بالثواني بدقة' : 'Show Exact Time with Seconds'}
          subtitle={isArabic ? 'مثال: 10:42:15 م بدلاً من 10:42 م' : 'Format timestamps as HH:MM:SS'}
          checked={config.showExactSeconds}
          onChange={(v) => updateConfig({ showExactSeconds: v })}
          icon={<Zap className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'إظهار سجل الرسائل المعدلة' : 'Show Edited Message History'}
          subtitle={isArabic ? 'الاحتفاظ بالنص الأصلي للرسالة قبل قيام الطرف الآخر بتعديلها' : 'Preserve and view original text before edits'}
          checked={config.showEditedHistory}
          onChange={(v) => updateConfig({ showEditedHistory: v })}
          icon={<Shield className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'تأكيد قبل إرسال الرسائل الصوتية' : 'Confirm Voice Messages'}
          subtitle={isArabic ? 'إمكانية الاستماع للتسجيل الصوتي قبل إرساله' : 'Review recorded audio note before dispatch'}
          checked={config.confirmVoiceNotes}
          onChange={(v) => updateConfig({ confirmVoiceNotes: v })}
          icon={<Volume2 className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'تأكيد قبل إرسال الملصقات و GIFs' : 'Confirm Stickers & GIFs'}
          subtitle={isArabic ? 'منع الإرسال الفوري بالخطأ عند النقر على الملصق' : 'Prompt confirmation before sending sticker'}
          checked={config.confirmStickers}
          onChange={(v) => updateConfig({ confirmStickers: v })}
          icon={<Sparkles className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'الترجمة التلقائية للرسائل الواردة' : 'Auto-Translate Foreign Messages'}
          subtitle={isArabic ? 'ترجمة الرسائل بلغات أجنبية إلى لغتك المفضلة مباشرة' : 'Translate incoming messages in real-time'}
          checked={config.autoTranslateIncoming}
          onChange={(v) => updateConfig({ autoTranslateIncoming: v })}
          icon={<Globe className="w-4 h-4" />}
        />

        <SelectRow
          title={isArabic ? 'محرك الترجمة المفضل' : 'Translation Provider Engine'}
          value={config.translationProvider}
          options={[
            { label: 'Telegram AI MTProto', value: 'telegram' },
            { label: 'Google Cloud Translate', value: 'google' },
            { label: 'DeepL Pro', value: 'deepl' },
          ]}
          onChange={(v) => updateConfig({ translationProvider: v as any })}
          icon={<Globe className="w-4 h-4" />}
        />
      </div>
    </div>
  );
};

// =========================================================================
// 6. PLUS TOPICS VIEW (Topics)
// =========================================================================
export const PlusTopicsView: React.FC<{ onBack: () => void }> = ({ onBack }) => {
  const { settings } = useTelegram();
  const isArabic = settings.language === 'ar';
  const { config, updateConfig } = usePlusConfig();

  return (
    <div className="flex flex-col h-full overflow-hidden bg-[#0e1621]">
      <SubPageHeader title={isArabic ? 'Topics (مواضيع المجموعات)' : 'Forum Topics'} onBack={onBack} />
      <div className="flex-1 overflow-y-auto divide-y divide-white/5">
        <ToggleRow
          title={isArabic ? 'عرض المواضيع كتبويبات في الأعلى' : 'Show Topics as Tabs'}
          subtitle={isArabic ? 'شريط علوي سريع للتبديل بين المواضيع داخل المنتديات' : 'Top scrollable tab bar inside forum groups'}
          checked={config.topicsAsTabs}
          onChange={(v) => updateConfig({ topicsAsTabs: v })}
          icon={<ListOrdered className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'فتح الموضوع العام تلقائياً' : 'Auto-Open General Topic'}
          subtitle={isArabic ? 'الدخول المباشر للموضوع الرئيسي عند فتح المجموعة' : 'Navigate directly to General thread'}
          checked={config.autoOpenGeneralTopic}
          onChange={(v) => updateConfig({ autoOpenGeneralTopic: v })}
          icon={<CheckCircle2 className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'إظهار شارة غير مقروء لكل موضوع' : 'Unread Badges per Topic'}
          subtitle={isArabic ? 'عرض عدد الرسائل الجديدة في كل موضوع بشكل منفصل' : 'Individual unread counts for each sub-topic'}
          checked={config.unreadTopicBadges}
          onChange={(v) => updateConfig({ unreadTopicBadges: v })}
          icon={<Bell className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'شريط البحث السريع في المواضيع' : 'Quick Topic Filter & Search'}
          subtitle={isArabic ? 'تصفية المواضيع والمحادثات بالاسم مباشرة' : 'Filter topics instantly by keyword'}
          checked={config.quickTopicSearch}
          onChange={(v) => updateConfig({ quickTopicSearch: v })}
          icon={<Sliders className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'معاينة آخر رسالة في قائمة المواضيع' : 'Last Message Preview in Topics List'}
          subtitle={isArabic ? 'عرض سطر مختصر من آخر نشاط داخل كل موضوع' : 'Show snippet of the latest post'}
          checked={config.lastTopicMessagePreview}
          onChange={(v) => updateConfig({ lastTopicMessagePreview: v })}
          icon={<MessageSquare className="w-4 h-4" />}
        />
      </div>
    </div>
  );
};

// =========================================================================
// 7. PLUS DRAWER VIEW (درج التصفح)
// =========================================================================
export const PlusDrawerView: React.FC<{ onBack: () => void }> = ({ onBack }) => {
  const { settings } = useTelegram();
  const isArabic = settings.language === 'ar';
  const { config, updateConfig } = usePlusConfig();

  return (
    <div className="flex flex-col h-full overflow-hidden bg-[#0e1621]">
      <SubPageHeader title={isArabic ? 'درج التصفح' : 'Navigation Drawer'} onBack={onBack} />
      <div className="flex-1 overflow-y-auto divide-y divide-white/5">
        <SelectRow
          title={isArabic ? 'تصميم رأس القائمة الجانبية' : 'Drawer Header Style'}
          value={config.drawerHeaderStyle}
          options={[
            { label: isArabic ? 'قياسي (صورة ورقم)' : 'Standard (Avatar & Phone)', value: 'standard' },
            { label: isArabic ? 'مصغر ومضغوط' : 'Minimal Compact', value: 'minimal' },
            { label: isArabic ? 'مخصص شفاف' : 'Custom Glass', value: 'custom' },
          ]}
          onChange={(v) => updateConfig({ drawerHeaderStyle: v as any })}
          icon={<Menu className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'إظهار مفتاح الوضع الليلي' : 'Show Night Mode Switch'}
          subtitle={isArabic ? 'أيقونة الهلال والتبديل السريع للوضع الداكن' : 'Dark theme toggle button in header'}
          checked={config.drawerShowNightMode}
          onChange={(v) => updateConfig({ drawerShowNightMode: v })}
          icon={<Sparkles className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'إظهار الرسائل المحفوظة' : 'Show Saved Messages'}
          subtitle={isArabic ? 'الوصول السريع إلى محادثة الرسائل المحفوظة' : 'Quick link to Cloud Saved Messages'}
          checked={config.drawerShowSavedMessages}
          onChange={(v) => updateConfig({ drawerShowSavedMessages: v })}
          icon={<Folder className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'إظهار المكالمات' : 'Show Calls Tab'}
          subtitle={isArabic ? 'سجل المكالمات الصوتية والفيديو' : 'Recent calls log entry'}
          checked={config.drawerShowCalls}
          onChange={(v) => updateConfig({ drawerShowCalls: v })}
          icon={<Radio className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'إظهار جهات الاتصال' : 'Show Contacts'}
          subtitle={isArabic ? 'قائمة الأصدقاء المسجلين' : 'Address book contacts list'}
          checked={config.drawerShowContacts}
          onChange={(v) => updateConfig({ drawerShowContacts: v })}
          icon={<Users className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'إظهار زر إعدادات بلاس' : 'Show Plus Settings in Drawer'}
          subtitle={isArabic ? 'زر مباشر للوصول لإعدادات بلاس' : 'Direct shortcut to Plus Settings'}
          checked={config.drawerShowPlusSettings}
          onChange={(v) => updateConfig({ drawerShowPlusSettings: v })}
          icon={<Settings className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'إظهار مبدل الحسابات المتعددة' : 'Show Multi-Account Switcher'}
          subtitle={isArabic ? 'التبديل بين الحسابات وأرقام الهاتف من رأس القائمة' : 'Switch between telegram numbers seamlessly'}
          checked={config.drawerShowAccounts}
          onChange={(v) => updateConfig({ drawerShowAccounts: v })}
          icon={<User className="w-4 h-4" />}
        />
      </div>
    </div>
  );
};

// =========================================================================
// 8. PLUS PROFILE VIEW (الملف الشخصي)
// =========================================================================
export const PlusProfileView: React.FC<{ onBack: () => void }> = ({ onBack }) => {
  const { settings } = useTelegram();
  const isArabic = settings.language === 'ar';
  const { config, updateConfig } = usePlusConfig();

  return (
    <div className="flex flex-col h-full overflow-hidden bg-[#0e1621]">
      <SubPageHeader title={isArabic ? 'الملف الشخصي' : 'Profile'} onBack={onBack} />
      <div className="flex-1 overflow-y-auto divide-y divide-white/5">
        <ToggleRow
          title={isArabic ? 'عرض معرف المستخدم الرقمي (User ID & DC)' : 'Show Numerical ID & Data Center'}
          subtitle={isArabic ? 'عرض المعرف ومركز البيانات DC في بطاقة البروفايل' : 'Display numeric ID & DC identifier'}
          checked={config.profileShowUserId}
          onChange={(v) => updateConfig({ profileShowUserId: v })}
          icon={<User className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'نسخ المعرف والاسم بنقرة واحدة' : 'One-Tap Copy ID & Username'}
          subtitle={isArabic ? 'نسخ البيانات مباشرة إلى الحافظة عند الضغط عليها' : 'Copy to clipboard immediately on click'}
          checked={config.profileCopyIdOnTap}
          onChange={(v) => updateConfig({ profileCopyIdOnTap: v })}
          icon={<Copy className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'عرض المجموعات المشتركة مباشرة' : 'Direct Common Groups List'}
          subtitle={isArabic ? 'قائمة المجموعات التي تجمعك مع جهة الاتصال' : 'Show shared group memberships'}
          checked={config.profileShowCommonGroups}
          onChange={(v) => updateConfig({ profileShowCommonGroups: v })}
          icon={<Users className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'إخفاء رقم الهاتف في الملف الشخصي' : 'Hide Phone Number in Profile Card'}
          subtitle={isArabic ? 'عدم عرض رقم الهاتف لزيادة الخصوصية' : 'Conceal mobile number for enhanced privacy'}
          checked={config.profileHidePhone}
          onChange={(v) => updateConfig({ profileHidePhone: v })}
          icon={<EyeOff className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'شريط الإجراءات السريعة في البروفايل' : 'Quick Action Action Bar'}
          subtitle={isArabic ? 'أزرار سريعة للاتصال، الكتم، الحظر، والمشاركة' : 'Direct buttons for Call, Mute, Block, Share'}
          checked={config.profileQuickActions}
          onChange={(v) => updateConfig({ profileQuickActions: v })}
          icon={<Zap className="w-4 h-4" />}
        />
      </div>
    </div>
  );
};

// =========================================================================
// 9. PLUS NOTIFICATIONS VIEW (الإشعارات)
// =========================================================================
export const PlusNotificationsView: React.FC<{ onBack: () => void }> = ({ onBack }) => {
  const { settings } = useTelegram();
  const isArabic = settings.language === 'ar';
  const { config, updateConfig } = usePlusConfig();

  return (
    <div className="flex flex-col h-full overflow-hidden bg-[#0e1621]">
      <SubPageHeader title={isArabic ? 'الإشعارات' : 'Notifications'} onBack={onBack} />
      <div className="flex-1 overflow-y-auto divide-y divide-white/5">
        <SelectRow
          title={isArabic ? 'نمط الإشعارات داخل التطبيق' : 'In-App Notification Style'}
          value={config.inAppNotificationStyle}
          options={[
            { label: isArabic ? 'شريط علوي منبثق' : 'Top Banner', value: 'banner' },
            { label: isArabic ? 'كبسولة عائمة' : 'Floating Pill', value: 'pill' },
            { label: isArabic ? 'صامت تماماً' : 'Silent', value: 'silent' },
          ]}
          onChange={(v) => updateConfig({ inAppNotificationStyle: v as any })}
          icon={<Bell className="w-4 h-4" />}
        />

        <SelectRow
          title={isArabic ? 'تكرار التنبيهات للرسائل غير المقروءة' : 'Repeat Unread Alerts'}
          value={config.repeatUnreadAlerts}
          options={[
            { label: isArabic ? 'إيقاف' : 'Off', value: 'off' },
            { label: isArabic ? 'كل 5 دقائق' : 'Every 5 min', value: '5min' },
            { label: isArabic ? 'كل 15 دقيقة' : 'Every 15 min', value: '15min' },
          ]}
          onChange={(v) => updateConfig({ repeatUnreadAlerts: v as any })}
          icon={<RefreshCw className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'أولوية قصوى لجهات الاتصال المهمة (VIP)' : 'High Priority VIP Alerts'}
          subtitle={isArabic ? 'إصدار رنين خاص حتى في وضع عدم الإزعاج' : 'Bypass DND for starred contacts'}
          checked={config.vipPriorityAlerts}
          onChange={(v) => updateConfig({ vipPriorityAlerts: v })}
          icon={<Sparkles className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'تصفية إشعارات السبام والمجموعات المزعجة' : 'Filter Spam & Noisy Notifications'}
          subtitle={isArabic ? 'كتم التنبيهات المتكررة تلقائياً' : 'Suppress rapid consecutive alerts'}
          checked={config.filterSpamAlerts}
          onChange={(v) => updateConfig({ filterSpamAlerts: v })}
          icon={<Shield className="w-4 h-4" />}
        />
      </div>
    </div>
  );
};

// =========================================================================
// 10. PLUS PRIVACY VIEW (الخصوصية والأمان)
// =========================================================================
export const PlusPrivacyView: React.FC<{ onBack: () => void }> = ({ onBack }) => {
  const { settings, showToast } = useTelegram();
  const isArabic = settings.language === 'ar';
  const { config, updateConfig } = usePlusConfig();

  return (
    <div className="flex flex-col h-full overflow-hidden bg-[#0e1621]">
      <SubPageHeader title={isArabic ? 'الخصوصية والأمان' : 'Privacy and Security'} onBack={onBack} />
      <div className="flex-1 overflow-y-auto divide-y divide-white/5">
        <ToggleRow
          title={isArabic ? 'وضع الشبح الكامل (Ghost Mode)' : 'Master Ghost Stealth Mode'}
          subtitle={isArabic ? 'إخفاء حالة الاتصال وصحين القراءة وجاري الكتابة معاً' : 'Hide online status, read receipts, and typing simultaneously'}
          checked={config.ghostMode}
          onChange={(v) => {
            updateConfig({
              ghostMode: v,
              hideOnlineStatus: v,
              hideReadReceipts: v,
              hideTypingIndicator: v,
            });
            showToast(v ? (isArabic ? 'تم تفعيل وضع الشبح 👻' : 'Ghost Mode Activated 👻') : (isArabic ? 'تم إيقاف وضع الشبح' : 'Ghost Mode Deactivated'), '👻');
          }}
          icon={<EyeOff className="w-4 h-4 text-purple-400" />}
        />

        <ToggleRow
          title={isArabic ? 'إخفاء صحين القراءة (Hide Read Receipts)' : 'Hide Read Receipts (Ticks)'}
          subtitle={isArabic ? 'قراءة الرسائل دون أن يتحول الصح إلى أزرق/مقروء' : 'Read incoming messages without sender knowing'}
          checked={config.hideReadReceipts}
          onChange={(v) => updateConfig({ hideReadReceipts: v })}
          icon={<Check className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'إخفاء جاري الكتابة (Hide Typing Indicator)' : 'Hide Typing Status'}
          subtitle={isArabic ? 'عدم إظهار "جاري الكتابة..." أو "يسجل صوتاً..."' : 'Suppress typing / recording status'}
          checked={config.hideTypingIndicator}
          onChange={(v) => updateConfig({ hideTypingIndicator: v })}
          icon={<Zap className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'منع حذف الرسائل (Anti-Delete Messages)' : 'Anti-Delete Messages Protection'}
          subtitle={isArabic ? 'الاحتفاظ بالرسائل المحذوفة من قبل الطرف الآخر مع علامة 🗑️' : 'Preserve deleted messages with marked badge'}
          checked={config.antiDeleteMessages}
          onChange={(v) => updateConfig({ antiDeleteMessages: v })}
          icon={<Shield className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'منع تعديل الرسائل (Anti-Edit Messages)' : 'Anti-Edit Message History'}
          subtitle={isArabic ? 'عرض النص الأصلي قبل التعديل' : 'View original text before edits'}
          checked={config.antiEditMessages}
          onChange={(v) => updateConfig({ antiEditMessages: v })}
          icon={<CheckCircle2 className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'قفل المحادثات المخفية برمز سري' : 'Lock Hidden Chats with PIN'}
          subtitle={isArabic ? 'إخفاء محادثات محددة وحمايتها برمز سري خاص' : 'Protect confidential chats behind extra code'}
          checked={config.hiddenChatsLocked}
          onChange={(v) => updateConfig({ hiddenChatsLocked: v })}
          icon={<Lock className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'إلغاء القفل بالبصمة البيومترية' : 'Biometric Fingerprint Unlock'}
          subtitle={isArabic ? 'استخدام مستشعر البصمة أو التعرف على الوجه' : 'Use fingerprint / Face ID to unlock'}
          checked={config.biometricsEnabled}
          onChange={(v) => updateConfig({ biometricsEnabled: v })}
          icon={<HardDrive className="w-4 h-4" />}
        />
      </div>
    </div>
  );
};

// =========================================================================
// 11. PLUS SHARED MEDIA VIEW (الوسائط المتبادلة)
// =========================================================================
export const PlusMediaView: React.FC<{ onBack: () => void }> = ({ onBack }) => {
  const { settings } = useTelegram();
  const isArabic = settings.language === 'ar';
  const { config, updateConfig } = usePlusConfig();

  return (
    <div className="flex flex-col h-full overflow-hidden bg-[#0e1621]">
      <SubPageHeader title={isArabic ? 'الوسائط المتبادلة' : 'Shared Media'} onBack={onBack} />
      <div className="flex-1 overflow-y-auto divide-y divide-white/5">
        <SelectRow
          title={isArabic ? 'التبويب الافتراضي عند فتح الوسائط' : 'Default Shared Media Tab'}
          value={config.defaultMediaTab}
          options={[
            { label: isArabic ? 'الصور' : 'Photos', value: 'photos' },
            { label: isArabic ? 'الفيديوهات' : 'Videos', value: 'videos' },
            { label: isArabic ? 'الملفات' : 'Files', value: 'files' },
            { label: isArabic ? 'الصوتيات' : 'Audio', value: 'audio' },
            { label: isArabic ? 'الروابط' : 'Links', value: 'links' },
            { label: isArabic ? 'التسجيلات' : 'Voice', value: 'voice' },
          ]}
          onChange={(v) => updateConfig({ defaultMediaTab: v as any })}
          icon={<LayoutGrid className="w-4 h-4" />}
        />

        <SelectRow
          title={isArabic ? 'عدد أعمدة شبكة الصور' : 'Media Grid Columns'}
          value={String(config.gridColumnsCount)}
          options={[
            { label: isArabic ? '3 أعمدة (كبير)' : '3 Columns', value: '3' },
            { label: isArabic ? '4 أعمدة (متوسط)' : '4 Columns', value: '4' },
            { label: isArabic ? '5 أعمدة (مكثف)' : '5 Columns', value: '5' },
          ]}
          onChange={(v) => updateConfig({ gridColumnsCount: Number(v) })}
          icon={<Sliders className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'معاينة الصور بدقة كاملة' : 'High Resolution Thumbnails'}
          subtitle={isArabic ? 'عدم ضغط صور المعاينة في الشبكة' : 'Load full quality previews'}
          checked={config.highResThumbnailPreview}
          onChange={(v) => updateConfig({ highResThumbnailPreview: v })}
          icon={<Sparkles className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'نافذة الفيديو العائمة المصغرة (Picture-in-Picture)' : 'Picture-in-Picture Floating Video'}
          subtitle={isArabic ? 'استمرار تشغيل الفيديو أثناء تصفح التطبيق' : 'Keep video playing in floating window'}
          checked={config.pipFloatingVideo}
          onChange={(v) => updateConfig({ pipFloatingVideo: v })}
          icon={<PlayCircle className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'إيقاف الصوتيات تلقائياً عند تشغيل الفيديو' : 'Auto-Pause Music on Video Play'}
          subtitle={isArabic ? 'إيقاف الموسيقى عند تشغيل أي فيديو أو رسالة مرئية' : 'Pause audio tracks when video begins'}
          checked={config.autoPauseAudioOnVideo}
          onChange={(v) => updateConfig({ autoPauseAudioOnVideo: v })}
          icon={<Volume2 className="w-4 h-4" />}
        />
      </div>
    </div>
  );
};

// =========================================================================
// 12. PLUS DOWNLOADS VIEW (التحميلات)
// =========================================================================
export const PlusDownloadsView: React.FC<{ onBack: () => void }> = ({ onBack }) => {
  const { settings } = useTelegram();
  const isArabic = settings.language === 'ar';
  const { config, updateConfig } = usePlusConfig();

  return (
    <div className="flex flex-col h-full overflow-hidden bg-[#0e1621]">
      <SubPageHeader title={isArabic ? 'التحميلات' : 'Downloads'} onBack={onBack} />
      <div className="flex-1 overflow-y-auto divide-y divide-white/5">
        <ToggleRow
          title={isArabic ? 'التنزيل التلقائي عبر Wi-Fi' : 'Auto-Download on Wi-Fi'}
          subtitle={isArabic ? 'تنزيل الصور والفيديوهات والمستندات تلقائياً' : 'Automatically download incoming media on Wi-Fi'}
          checked={config.autoDownloadWifi}
          onChange={(v) => updateConfig({ autoDownloadWifi: v })}
          icon={<Download className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'التنزيل التلقائي عبر بيانات الهاتف' : 'Auto-Download on Cellular Data'}
          subtitle={isArabic ? 'تقييد التنزيل للصور فقط للحفاظ على الباقة' : 'Limit to photos on mobile data'}
          checked={config.autoDownloadCellular}
          onChange={(v) => updateConfig({ autoDownloadCellular: v })}
          icon={<Smartphone className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'مسرع التنزيل المتعدد (MTProto Multi-Thread Booster)' : 'MTProto Parallel Download Booster'}
          subtitle={isArabic ? 'تحميل الملفات الكبيرة عبر 4 مسارات متزامنة للوصول لأقصى سرعة' : 'Download chunks concurrently for maximum throughput'}
          checked={config.downloadBooster}
          onChange={(v) => updateConfig({ downloadBooster: v })}
          icon={<Zap className="w-4 h-4 text-amber-400" />}
        />

        <SelectRow
          title={isArabic ? 'الحد الأقصى للتنزيلات المتزامنة' : 'Max Concurrent Downloads'}
          value={String(config.maxConcurrentDownloads)}
          options={[
            { label: isArabic ? '1 ملف' : '1 File', value: '1' },
            { label: isArabic ? '2 ملفات' : '2 Files', value: '2' },
            { label: isArabic ? '4 ملفات' : '4 Files', value: '4' },
            { label: isArabic ? '8 ملفات' : '8 Files', value: '8' },
          ]}
          onChange={(v) => updateConfig({ maxConcurrentDownloads: Number(v) })}
          icon={<Sliders className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'تنبيه صوتي عند اكتمال التنزيل' : 'Sound on Download Completion'}
          subtitle={isArabic ? 'رنة إشعار قصيرة عند انتهاء تحميل الملف' : 'Play chime when file transfer finishes'}
          checked={config.downloadFinishSound}
          onChange={(v) => updateConfig({ downloadFinishSound: v })}
          icon={<Bell className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'استئناف التنزيل المنقطع تلقائياً' : 'Auto-Resume Interrupted Downloads'}
          subtitle={isArabic ? 'متابعة التحميل فور عودة الاتصال دون إعادة من البداية' : 'Resume broken downloads automatically'}
          checked={config.autoResumeDownloads}
          onChange={(v) => updateConfig({ autoResumeDownloads: v })}
          icon={<RefreshCw className="w-4 h-4" />}
        />
      </div>
    </div>
  );
};

// =========================================================================
// 13. PLUS ADS VIEW (Ads)
// =========================================================================
export const PlusAdsView: React.FC<{ onBack: () => void }> = ({ onBack }) => {
  const { settings, showToast } = useTelegram();
  const isArabic = settings.language === 'ar';
  const { config, updateConfig } = usePlusConfig();

  return (
    <div className="flex flex-col h-full overflow-hidden bg-[#0e1621]">
      <SubPageHeader title={isArabic ? 'Ads (الإعلانات)' : 'Ads'} onBack={onBack} />
      <div className="flex-1 overflow-y-auto divide-y divide-white/5">
        <ToggleRow
          title={isArabic ? 'حظر الرسائل الإعلانية في القنوات (Block Sponsored Ads)' : 'Block Sponsored Messages in Channels'}
          subtitle={isArabic ? 'إخفاء الإعلانات والمنشورات الممولة في القنوات العامة' : 'Suppress sponsored promotional posts'}
          checked={config.blockSponsoredMessages}
          onChange={(v) => {
            updateConfig({ blockSponsoredMessages: v });
            showToast(v ? (isArabic ? 'تم حظر الإعلانات الممولة' : 'Sponsored ads blocked') : (isArabic ? 'تم السماح بالإعلانات' : 'Ads allowed'), '🛡️');
          }}
          icon={<Shield className="w-4 h-4 text-emerald-400" />}
        />

        <ToggleRow
          title={isArabic ? 'إخفاء القنوات المقترحة في البحث العام' : 'Hide Promoted Channels in Search'}
          subtitle={isArabic ? 'عرض نتائج البحث الحقيقية فقط بدون قنوات تجارية مقترحة' : 'Remove sponsored results from global search'}
          checked={config.hidePromotedChannels}
          onChange={(v) => updateConfig({ hidePromotedChannels: v })}
          icon={<EyeOff className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'حظر إعلانات البوتات التلقائية' : 'Block Inline Bot Advertisements'}
          subtitle={isArabic ? 'تصفية الروابط الترويجية المرسلة من البوتات' : 'Filter out promotional bot broadcasts'}
          checked={config.blockBotAds}
          onChange={(v) => updateConfig({ blockBotAds: v })}
          icon={<Megaphone className="w-4 h-4" />}
        />

        <ToggleRow
          title={isArabic ? 'تعطيل إشعارات العروض الترويجية' : 'Disable Promotional Push Alerts'}
          subtitle={isArabic ? 'منع الإشعارات الترويجية لتيليجرام بريميوم والهدايا' : 'Mute Telegram Premium & Stars promotions'}
          checked={config.disablePromoAlerts}
          onChange={(v) => updateConfig({ disablePromoAlerts: v })}
          icon={<Bell className="w-4 h-4" />}
        />
      </div>
    </div>
  );
};
