import React, { useState, useEffect } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  Check,
  X,
  Shield,
  Lock,
  Key,
  Mail,
  Sparkles,
  MessageSquare,
  Play,
  Video,
  Music,
  FileText,
  Link as LinkIcon,
  Mic,
  Image as ImageIcon,
  Share2,
  Smartphone,
  Laptop,
  Trash2,
  Download,
  Upload,
  RotateCw,
  RefreshCw,
  Layers,
  Sliders,
  Globe,
  Eye,
  EyeOff,
  Bell,
  Radio,
  Database,
  HardDrive,
  Zap,
  Tag,
  HelpCircle,
  Info,
  Star,
  Gift,
  CheckCircle,
  AlertCircle,
  ChevronRight,
  ChevronLeft,
  Bookmark,
  Archive,
  MessageCircle,
  BarChart2,
  DollarSign,
  Cloud,
  Copy,
  Phone,
  User as UserIcon,
  Calendar,
  Settings,
  Users,
  PlayCircle,
  ListOrdered,
  Menu,
  User,
  LayoutGrid,
  Megaphone,
  LogOut,
  Eraser,
  SlidersHorizontal,
  FolderSync,
  SlidersVertical,
  Volume2,
  Fingerprint,
} from 'lucide-react';
import { useTelegram } from '../../context/TelegramContext';
import { UserConfig } from '../../core/messenger/UserConfig';
import { AccountInstance } from '../../core/messenger/AccountInstance';

interface ViewProps {
  onBack?: () => void;
}

const SubHeader: React.FC<{ title: string; onBack?: () => void }> = ({ title, onBack }) => {
  const { settings } = useTelegram();
  const isArabic = settings.language === 'ar';
  const BackIcon = isArabic ? ArrowRight : ArrowLeft;

  return (
    <div className="flex items-center justify-between p-4 border-b border-white/10 sticky top-0 bg-[#17212b]/95 backdrop-blur-md z-10 select-none">
      <div className="flex items-center gap-3">
        {onBack && (
          <button onClick={onBack} className="p-2 hover:bg-white/10 rounded-full transition-colors text-white">
            <BackIcon size={20} />
          </button>
        )}
        <h2 className="font-bold text-base text-white">{title}</h2>
      </div>
    </div>
  );
};

const ToggleItem: React.FC<{
  title: string;
  subtitle?: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
  icon?: React.ReactNode;
}> = ({ title, subtitle, checked, onChange, icon }) => (
  <div
    onClick={() => onChange(!checked)}
    className="flex items-center justify-between p-3.5 hover:bg-white/5 cursor-pointer transition-colors"
  >
    <div className="flex items-center gap-3 min-w-0 flex-1">
      {icon && <div className="text-cyan-400 shrink-0">{icon}</div>}
      <div className="min-w-0 flex-1">
        <div className="text-sm font-medium text-white truncate">{title}</div>
        {subtitle && <div className="text-xs text-gray-400 mt-0.5 leading-relaxed">{subtitle}</div>}
      </div>
    </div>
    <div
      className={`w-11 h-6 rounded-full transition-colors relative shrink-0 ml-3 rtl:mr-3 rtl:ml-0 ${
        checked ? 'bg-[#2481cc]' : 'bg-gray-700'
      }`}
    >
      <div
        className={`w-5 h-5 rounded-full bg-white absolute top-0.5 transition-transform ${
          checked ? 'translate-x-5 rtl:-translate-x-5' : 'translate-x-0.5 rtl:-translate-x-0.5'
        }`}
      />
    </div>
  </div>
);

// ==========================================
// 1. PLUS GENERAL VIEW (عام)
// ==========================================
export const GeneralPlusSettingsView: React.FC<ViewProps> = ({ onBack }) => {
  const { settings, updateSettings, showToast } = useTelegram();
  const isArabic = settings.language === 'ar';

  const [directSend, setDirectSend] = useState(true);
  const [hideScrollFab, setHideScrollFab] = useState(false);
  const [tabsOnBottom, setTabsOnBottom] = useState(false);
  const [showFastSearch, setShowFastSearch] = useState(true);
  const [fontFamily, setFontFamily] = useState('system');
  const [customCornerRadius, setCustomCornerRadius] = useState(settings.bubbleCornerRadius || 14);

  return (
    <div className="flex-1 flex flex-col h-full bg-[#17212b] text-white overflow-y-auto font-sans">
      <SubHeader title={isArabic ? 'إعدادات عامة (Plus)' : 'General Plus Settings'} onBack={onBack} />

      <div className="p-4 space-y-6 max-w-2xl mx-auto w-full">
        {/* Appearance & Layout */}
        <div>
          <div className="text-xs font-semibold text-cyan-400 uppercase tracking-wider mb-2 px-1">
            {isArabic ? 'المظهر وتخطيط الشاشة' : 'Appearance & Layout'}
          </div>
          <div className="bg-[#242f3d] rounded-xl overflow-hidden border border-white/5 divide-y divide-white/5">
            <ToggleItem
              title={isArabic ? 'شريط التبويبات في الأسفل' : 'Tabs Bar at Bottom'}
              subtitle={isArabic ? 'عرض تصنيفات المحادثات في الجزء السفلي من الشاشة' : 'Display chat category tabs at the bottom bar'}
              checked={tabsOnBottom}
              onChange={(c) => {
                setTabsOnBottom(c);
                showToast(isArabic ? 'تم تحديث موقع شريط التبويبات' : 'Tabs position updated', '📱');
              }}
              icon={<LayoutGrid size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'إخفاء زر التمرير للأسفل (FAB)' : 'Hide Scroll Down Button'}
              subtitle={isArabic ? 'إخفاء الزر العائم للتمرير لأسفل المحادثة' : 'Hide floating action button for scrolling down'}
              checked={hideScrollFab}
              onChange={setHideScrollFab}
              icon={<SlidersVertical size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'شريط البحث السريع' : 'Fast Search Bar'}
              subtitle={isArabic ? 'تثبيت حقل البحث السريع في أعلى قائمة المحادثات' : 'Pin quick search bar at the top of chat list'}
              checked={showFastSearch}
              onChange={setShowFastSearch}
              icon={<Sparkles size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'زر الإرسال المباشر' : 'Direct Send Action'}
              subtitle={isArabic ? 'إرسال الوسائط والنصوص بنقرة زر واحدة فورية' : 'Send media and text messages with single-tap'}
              checked={directSend}
              onChange={setDirectSend}
              icon={<Zap size={18} />}
            />
          </div>
        </div>

        {/* Bubble Roundness */}
        <div>
          <div className="text-xs font-semibold text-cyan-400 uppercase tracking-wider mb-2 px-1">
            {isArabic ? 'انحناء فقاعات الرسائل' : 'Message Bubble Corners'}
          </div>
          <div className="bg-[#242f3d] p-4 rounded-xl border border-white/5 space-y-3">
            <div className="flex justify-between text-xs text-gray-300">
              <span>{isArabic ? 'مستوى الانحناء' : 'Corner Radius'}</span>
              <span className="font-bold text-cyan-400">{customCornerRadius}px</span>
            </div>
            <input
              type="range"
              min="4"
              max="24"
              value={customCornerRadius}
              onChange={(e) => {
                const val = Number(e.target.value);
                setCustomCornerRadius(val);
                updateSettings({ bubbleCornerRadius: val });
              }}
              className="w-full accent-cyan-400 cursor-pointer"
            />
          </div>
        </div>
      </div>
    </div>
  );
};

// ==========================================
// 2. PLUS CHATS VIEW (المحادثات & إدارة المجموعات)
// ==========================================
export const ChatsPlusSettingsView: React.FC<ViewProps> = ({ onBack }) => {
  const { settings, chats, activeChatId, leaveGroup, deleteGroupMessages, deleteGroup, showToast } = useTelegram();
  const isArabic = settings.language === 'ar';

  const [unlimitedPinned, setUnlimitedPinned] = useState(true);
  const [confirmAudioCall, setConfirmAudioCall] = useState(true);
  const [confirmVideoCall, setConfirmVideoCall] = useState(true);
  const [hideChannelTabs, setHideChannelTabs] = useState(false);
  const [hideBotTabs, setHideBotTabs] = useState(false);
  const [swipeArchive, setSwipeArchive] = useState(true);

  const groupChats = chats.filter((c) => c.type === 'group' || c.type === 'channel');
  const [selectedChatForAction, setSelectedChatForAction] = useState<string>(
    activeChatId && groupChats.some((c) => c.id === activeChatId) ? activeChatId : groupChats[0]?.id || ''
  );

  const [confirmModal, setConfirmModal] = useState<{
    type: 'leave' | 'clear' | 'delete';
    title: string;
    description: string;
    action: () => void;
  } | null>(null);

  return (
    <div className="flex-1 flex flex-col h-full bg-[#17212b] text-white overflow-y-auto font-sans">
      <SubHeader title={isArabic ? 'إعدادات المحادثات وإدارة المجموعات' : 'Chats & Group Management'} onBack={onBack} />

      <div className="p-4 space-y-6 max-w-2xl mx-auto w-full">
        {/* Dedicated Group Operations Toolbox */}
        <div>
          <div className="text-xs font-semibold text-cyan-400 uppercase tracking-wider mb-2 px-1">
            {isArabic ? 'أدوات إدارة المجموعات السريعة' : 'Group Management Quick Actions'}
          </div>
          <div className="bg-[#242f3d] p-4 rounded-xl border border-white/5 space-y-4">
            <div>
              <label className="block text-xs font-medium text-gray-300 mb-1.5">
                {isArabic ? 'اختر المجموعة المستهدفة:' : 'Select Target Group:'}
              </label>
              {groupChats.length > 0 ? (
                <select
                  value={selectedChatForAction}
                  onChange={(e) => setSelectedChatForAction(e.target.value)}
                  className="w-full bg-[#17212b] border border-white/10 text-white rounded-xl px-3 py-2.5 text-xs focus:outline-none focus:border-cyan-400"
                >
                  {groupChats.map((g) => (
                    <option key={g.id} value={g.id}>
                      {g.title} ({g.type === 'group' ? (isArabic ? 'مجموعة' : 'Group') : (isArabic ? 'قناة' : 'Channel')})
                    </option>
                  ))}
                </select>
              ) : (
                <div className="text-xs text-gray-400 italic">
                  {isArabic ? 'لا توجد مجموعات نشطة حالياً' : 'No active groups found'}
                </div>
              )}
            </div>

            {selectedChatForAction && (
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 pt-1">
                {/* Clear Messages */}
                <button
                  onClick={() => {
                    const target = groupChats.find((c) => c.id === selectedChatForAction);
                    setConfirmModal({
                      type: 'clear',
                      title: isArabic ? 'حذف رسائل المجموعة' : 'Clear Group Messages',
                      description: isArabic
                        ? `هل تريد بالتأكيد حذف وتفريغ جميع الرسائل من "${target?.title}"؟`
                        : `Are you sure you want to clear all messages from "${target?.title}"?`,
                      action: () => deleteGroupMessages(selectedChatForAction, true),
                    });
                  }}
                  className="flex items-center justify-center gap-2 p-2.5 rounded-xl bg-amber-500/15 hover:bg-amber-500/25 text-amber-400 border border-amber-400/20 text-xs font-bold transition-colors"
                >
                  <Eraser size={16} />
                  <span>{isArabic ? 'حذف الرسائل' : 'Clear Messages'}</span>
                </button>

                {/* Leave Group */}
                <button
                  onClick={() => {
                    const target = groupChats.find((c) => c.id === selectedChatForAction);
                    setConfirmModal({
                      type: 'leave',
                      title: isArabic ? 'الخروج من المجموعة ومغادرتها' : 'Leave Group',
                      description: isArabic
                        ? `هل تريد بالتأكيد مغادرة "${target?.title}" والخروج منها؟`
                        : `Are you sure you want to leave "${target?.title}"?`,
                      action: () => leaveGroup(selectedChatForAction),
                    });
                  }}
                  className="flex items-center justify-center gap-2 p-2.5 rounded-xl bg-rose-500/15 hover:bg-rose-500/25 text-rose-400 border border-rose-400/20 text-xs font-bold transition-colors"
                >
                  <LogOut size={16} />
                  <span>{isArabic ? 'مغادرة المجموعة' : 'Leave Group'}</span>
                </button>

                {/* Delete Group */}
                <button
                  onClick={() => {
                    const target = groupChats.find((c) => c.id === selectedChatForAction);
                    setConfirmModal({
                      type: 'delete',
                      title: isArabic ? 'حذف المجموعة نهائياً' : 'Delete Group Permanently',
                      description: isArabic
                        ? `سيتم حذف المجموعة "${target?.title}" بالكامل من الخوادم.`
                        : `Delete group "${target?.title}" permanently.`,
                      action: () => deleteGroup(selectedChatForAction),
                    });
                  }}
                  className="flex items-center justify-center gap-2 p-2.5 rounded-xl bg-red-600/15 hover:bg-red-600/25 text-red-500 border border-red-500/20 text-xs font-bold transition-colors"
                >
                  <Trash2 size={16} />
                  <span>{isArabic ? 'حذف المجموعة' : 'Delete Group'}</span>
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Chat List & Tabs Preferences */}
        <div>
          <div className="text-xs font-semibold text-cyan-400 uppercase tracking-wider mb-2 px-1">
            {isArabic ? 'خيارات التبويبات والمحادثات' : 'Tabs & Chat List Preferences'}
          </div>
          <div className="bg-[#242f3d] rounded-xl overflow-hidden border border-white/5 divide-y divide-white/5">
            <ToggleItem
              title={isArabic ? 'تثبيت غير محدود للمحادثات' : 'Unlimited Pinned Chats'}
              subtitle={isArabic ? 'تجاوز حد التيليجرام الافتراضي لتثبيت أي عدد من المحادثات' : 'Bypass default pin limit to pin unlimited chats'}
              checked={unlimitedPinned}
              onChange={setUnlimitedPinned}
              icon={<Bookmark size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'تأكيد الاتصال الصوتي' : 'Confirm Audio Calls'}
              subtitle={isArabic ? 'عرض نافذة تأكيد قبل بدء المكالمة الصوتية لمنع الاتصال بالخطأ' : 'Show confirmation dialog before dialing audio calls'}
              checked={confirmAudioCall}
              onChange={setConfirmAudioCall}
              icon={<Phone size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'تأكيد الاتصال المرئي' : 'Confirm Video Calls'}
              subtitle={isArabic ? 'عرض نافذة تأكيد قبل بدء مكالمة الفيديو' : 'Show confirmation dialog before starting video calls'}
              checked={confirmVideoCall}
              onChange={setConfirmVideoCall}
              icon={<Video size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'إخفاء تبويب القنوات' : 'Hide Channels Tab'}
              subtitle={isArabic ? 'إخفاء تصنيف القنوات من شريط التبويبات العلوي' : 'Hide channels category from top tabs'}
              checked={hideChannelTabs}
              onChange={setHideChannelTabs}
              icon={<Megaphone size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'إخفاء تبويب البوتات' : 'Hide Bots Tab'}
              subtitle={isArabic ? 'إخفاء الروبوتات من التبويبات' : 'Hide bots from category tabs'}
              checked={hideBotTabs}
              onChange={setHideBotTabs}
              icon={<Sparkles size={18} />}
            />
          </div>
        </div>
      </div>

      {/* Confirmation Modal */}
      {confirmModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-sm bg-[#17212b] border border-white/10 rounded-2xl p-5 shadow-2xl space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-rose-500/20 text-rose-400 flex items-center justify-center shrink-0">
                <AlertCircle className="w-5 h-5" />
              </div>
              <h3 className="font-bold text-white text-base">{confirmModal.title}</h3>
            </div>
            <p className="text-xs text-gray-300 leading-relaxed">{confirmModal.description}</p>
            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                onClick={() => setConfirmModal(null)}
                className="px-4 py-2 text-xs font-semibold text-gray-300 hover:bg-white/5 rounded-xl transition-colors"
              >
                {isArabic ? 'إلغاء' : 'Cancel'}
              </button>
              <button
                onClick={() => {
                  confirmModal.action();
                  setConfirmModal(null);
                }}
                className="px-4 py-2 text-xs font-semibold bg-rose-600 hover:bg-rose-500 text-white rounded-xl shadow-lg transition-colors"
              >
                {isArabic ? 'تأكيد الإجراء' : 'Confirm'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

// ==========================================
// 3. PLUS STORIES VIEW (القصص)
// ==========================================
export const StoriesPlusSettingsView: React.FC<ViewProps> = ({ onBack }) => {
  const { settings, showToast } = useTelegram();
  const isArabic = settings.language === 'ar';

  const [hideStoriesBar, setHideStoriesBar] = useState(false);
  const [stealthMode, setStealthMode] = useState(true);
  const [saveToGallery, setSaveToGallery] = useState(true);
  const [fullQuality, setFullQuality] = useState(true);

  return (
    <div className="flex-1 flex flex-col h-full bg-[#17212b] text-white overflow-y-auto font-sans">
      <SubHeader title={isArabic ? 'إعدادات القصص (Stories)' : 'Stories Settings'} onBack={onBack} />

      <div className="p-4 space-y-6 max-w-2xl mx-auto w-full">
        <div>
          <div className="text-xs font-semibold text-cyan-400 uppercase tracking-wider mb-2 px-1">
            {isArabic ? 'تخصيص شريط وعرض القصص' : 'Stories Customization'}
          </div>
          <div className="bg-[#242f3d] rounded-xl overflow-hidden border border-white/5 divide-y divide-white/5">
            <ToggleItem
              title={isArabic ? 'إخفاء شريط القصص من الشاشة الرئيسية' : 'Hide Stories Header Bar'}
              subtitle={isArabic ? 'إخفاء دوائر القصص لتوفير مساحة في قائمة المحادثات' : 'Hide stories avatars to save space in chats'}
              checked={hideStoriesBar}
              onChange={setHideStoriesBar}
              icon={<EyeOff size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'الوضع المتخفي عند مشاهدة القصص' : 'Stealth Mode for Stories'}
              subtitle={isArabic ? 'مشاهدة قصص الآخرين بدون أن تظهر في قائمة المشاهدين' : 'View stories without appearing in viewer list'}
              checked={stealthMode}
              onChange={setStealthMode}
              icon={<Shield size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'الحفظ التلقائي في المعرض' : 'Save Stories to Gallery'}
              subtitle={isArabic ? 'حفظ القصص المنشورة تلقائياً في جهازك' : 'Automatically download published stories to local gallery'}
              checked={saveToGallery}
              onChange={setSaveToGallery}
              icon={<Download size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'تنزيل بأعلى جودة أصلية' : 'Full Original Quality Download'}
              subtitle={isArabic ? 'تحميل فيديوهات وصور القصص بدقة 1080p كاملة' : 'Download stories in lossless 1080p resolution'}
              checked={fullQuality}
              onChange={setFullQuality}
              icon={<Sparkles size={18} />}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

// ==========================================
// 4. PLUS MESSAGES VIEW (الرسائل)
// ==========================================
export const MessagesPlusSettingsView: React.FC<ViewProps> = ({ onBack }) => {
  const { settings, showToast } = useTelegram();
  const isArabic = settings.language === 'ar';

  const [directForward, setDirectForward] = useState(true);
  const [showEditIndicator, setShowEditIndicator] = useState(true);
  const [ghostTyping, setGhostTyping] = useState(false);
  const [instantTranslate, setInstantTranslate] = useState(true);
  const [oneTapCopy, setOneTapCopy] = useState(true);

  return (
    <div className="flex-1 flex flex-col h-full bg-[#17212b] text-white overflow-y-auto font-sans">
      <SubHeader title={isArabic ? 'إعدادات الرسائل المتقدمة' : 'Messages Settings'} onBack={onBack} />

      <div className="p-4 space-y-6 max-w-2xl mx-auto w-full">
        <div>
          <div className="text-xs font-semibold text-cyan-400 uppercase tracking-wider mb-2 px-1">
            {isArabic ? 'خيارات إرسال وتوجيه الرسائل' : 'Messaging & Forwarding Options'}
          </div>
          <div className="bg-[#242f3d] rounded-xl overflow-hidden border border-white/5 divide-y divide-white/5">
            <ToggleItem
              title={isArabic ? 'إعادة التوجيه بدون اقتباس (Direct Forward)' : 'Forward without Quote'}
              subtitle={isArabic ? 'إعادة توجيه الرسائل دون إظهار اسم المرسل الأصلي' : 'Forward messages without author attribution header'}
              checked={directForward}
              onChange={setDirectForward}
              icon={<Share2 size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'إخفاء حالة "يكتب الآن..." (Ghost Typing)' : 'Ghost Typing Mode'}
              subtitle={isArabic ? 'عدم إظهار جاري الكتابة أو تسجيل الصوت للطرف الآخر' : 'Hide typing / recording indicator while composing messages'}
              checked={ghostTyping}
              onChange={setGhostTyping}
              icon={<EyeOff size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'زر الترجمة الفورية' : 'Instant Message Translation'}
              subtitle={isArabic ? 'عرض زر الترجمة المباشر بجانب كل رسالة بلغة أجنبية' : 'Show one-tap translation button beside foreign messages'}
              checked={instantTranslate}
              onChange={setInstantTranslate}
              icon={<Globe size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'نسخ الروابط والنصوص بنقرة واحدة' : 'One-Tap Link & Text Copy'}
              subtitle={isArabic ? 'نسخ النصوص فوراً عند النقر السريع' : 'Quickly copy messages to clipboard on single tap'}
              checked={oneTapCopy}
              onChange={setOneTapCopy}
              icon={<Copy size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'إظهار مؤشر التعديل والحذف' : 'Show Edited Message History'}
              subtitle={isArabic ? 'تنبيه عند قيام الطرف الآخر بتعديل رسالته' : 'Display visual indicator when messages are modified'}
              checked={showEditIndicator}
              onChange={setShowEditIndicator}
              icon={<MessageSquare size={18} />}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

// ==========================================
// 5. PLUS TOPICS VIEW (Topics)
// ==========================================
export const TopicsPlusSettingsView: React.FC<ViewProps> = ({ onBack }) => {
  const { settings, showToast } = useTelegram();
  const isArabic = settings.language === 'ar';

  const [enableForEveryGroup, setEnableForEveryGroup] = useState(true);
  const [hideClosedTopics, setHideClosedTopics] = useState(false);
  const [quickTopicSwitch, setQuickTopicSwitch] = useState(true);

  return (
    <div className="flex-1 flex flex-col h-full bg-[#17212b] text-white overflow-y-auto font-sans">
      <SubHeader title={isArabic ? 'إعدادات المواضيع (Topics)' : 'Topics Settings'} onBack={onBack} />

      <div className="p-4 space-y-6 max-w-2xl mx-auto w-full">
        <div>
          <div className="text-xs font-semibold text-cyan-400 uppercase tracking-wider mb-2 px-1">
            {isArabic ? 'إدارة مواضيع المجموعات والمنتديات' : 'Forum Topics Management'}
          </div>
          <div className="bg-[#242f3d] rounded-xl overflow-hidden border border-white/5 divide-y divide-white/5">
            <ToggleItem
              title={isArabic ? 'التبديل السريع بين المواضيع' : 'Quick Topics Switcher'}
              subtitle={isArabic ? 'عرض شريط جانبي للتنقل الفوري بين مواضيع المجموعة' : 'Show sidebar drawer to switch topics seamlessly'}
              checked={quickTopicSwitch}
              onChange={setQuickTopicSwitch}
              icon={<ListOrdered size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'إخفاء المواضيع المغلقة والمؤرشفة' : 'Hide Closed Topics'}
              subtitle={isArabic ? 'عدم إظهار المواضيع المقفلة لتسهيل تصفح النقاشات النشطة' : 'Hide archived topics from discussion feed'}
              checked={hideClosedTopics}
              onChange={setHideClosedTopics}
              icon={<EyeOff size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'تفعيل نظام المواضيع لجميع المجموعات' : 'Enable Topics Support'}
              subtitle={isArabic ? 'دعم تقسيم نقاشات المجموعات الكبيرة إلى مواضيع منفصلة' : 'Support forum topics hierarchy across all supergroups'}
              checked={enableForEveryGroup}
              onChange={setEnableForEveryGroup}
              icon={<Layers size={18} />}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

// ==========================================
// 6. PLUS NAVIGATION DRAWER VIEW (درج التصفح)
// ==========================================
export const NavigationDrawerPlusSettingsView: React.FC<ViewProps> = ({ onBack }) => {
  const { settings, showToast } = useTelegram();
  const isArabic = settings.language === 'ar';

  const [showAccountsInDrawer, setShowAccountsInDrawer] = useState(true);
  const [showUsernameInHeader, setShowUsernameInHeader] = useState(true);
  const [showPhoneInHeader, setShowPhoneInHeader] = useState(true);
  const [showNightToggle, setShowNightToggle] = useState(true);

  return (
    <div className="flex-1 flex flex-col h-full bg-[#17212b] text-white overflow-y-auto font-sans">
      <SubHeader title={isArabic ? 'إعدادات درج التصفح (القائمة الجانبية)' : 'Navigation Drawer Settings'} onBack={onBack} />

      <div className="p-4 space-y-6 max-w-2xl mx-auto w-full">
        <div>
          <div className="text-xs font-semibold text-cyan-400 uppercase tracking-wider mb-2 px-1">
            {isArabic ? 'تخصيص عناصر القائمة الجانبية' : 'Drawer Customization'}
          </div>
          <div className="bg-[#242f3d] rounded-xl overflow-hidden border border-white/5 divide-y divide-white/5">
            <ToggleItem
              title={isArabic ? 'إظهار قائمة الحسابات المتعددة' : 'Show Multi-Accounts List'}
              subtitle={isArabic ? 'عرض الحسابات الـ 4 في رأس القائمة للتبديل الفوري' : 'Display account switcher in drawer header'}
              checked={showAccountsInDrawer}
              onChange={setShowAccountsInDrawer}
              icon={<Users size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'إظهار المعرف @username تحت الاسم' : 'Show Username in Header'}
              subtitle={isArabic ? 'عرض اسم المستخدم بجانب صورة الحساب' : 'Show @username next to profile header'}
              checked={showUsernameInHeader}
              onChange={setShowUsernameInHeader}
              icon={<Tag size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'إظهار رقم الهاتف' : 'Show Phone Number'}
              subtitle={isArabic ? 'عرض رقم الهاتف المربوط بالحساب' : 'Display registered phone number'}
              checked={showPhoneInHeader}
              onChange={setShowPhoneInHeader}
              icon={<Phone size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'زر الوضع الليلي السريع' : 'Quick Dark Mode Toggle'}
              subtitle={isArabic ? 'إظهار أيقونة التبديل السريع بين الوضع النهاري والليلي' : 'Show day/night toggle icon in drawer'}
              checked={showNightToggle}
              onChange={setShowNightToggle}
              icon={<Sparkles size={18} />}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

// ==========================================
// 7. PLUS PROFILE VIEW (الملف الشخصي)
// ==========================================
export const ProfilePlusSettingsView: React.FC<ViewProps> = ({ onBack }) => {
  const { settings, currentUser, showToast } = useTelegram();
  const isArabic = settings.language === 'ar';

  const [showIdAndDc, setShowIdAndDc] = useState(true);
  const [showRegistrationDate, setShowRegistrationDate] = useState(true);
  const [highResAvatar, setHighResAvatar] = useState(true);

  return (
    <div className="flex-1 flex flex-col h-full bg-[#17212b] text-white overflow-y-auto font-sans">
      <SubHeader title={isArabic ? 'إعدادات الملف الشخصي' : 'Profile Settings'} onBack={onBack} />

      <div className="p-4 space-y-6 max-w-2xl mx-auto w-full">
        <div>
          <div className="text-xs font-semibold text-cyan-400 uppercase tracking-wider mb-2 px-1">
            {isArabic ? 'المعلومات التقنية وهوية الحساب' : 'Technical & Account Identity'}
          </div>
          <div className="bg-[#242f3d] rounded-xl overflow-hidden border border-white/5 divide-y divide-white/5">
            <ToggleItem
              title={isArabic ? 'إظهار المعرف الرقمي (User ID) ومركز البيانات (DC)' : 'Show User ID & DC Number'}
              subtitle={isArabic ? 'عرض ID: 62918402 و DC: 4 تحت اسم الحساب' : 'Show numeric user ID and active Data Center cluster'}
              checked={showIdAndDc}
              onChange={setShowIdAndDc}
              icon={<Key size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'إظهار تاريخ التسجيل التقريبي' : 'Show Account Registration Date'}
              subtitle={isArabic ? 'عرض تاريخ إنشاء الحساب في تيليجرام' : 'Display approximate account creation date'}
              checked={showRegistrationDate}
              onChange={setShowRegistrationDate}
              icon={<Calendar size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'عرض الصور الشخصية بدقة فائقة' : 'High-Res Full Avatar Viewer'}
              subtitle={isArabic ? 'تنزيل وعرض صور الحساب بأعلى جودة ممكنة' : 'View and download full-resolution avatars'}
              checked={highResAvatar}
              onChange={setHighResAvatar}
              icon={<ImageIcon size={18} />}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

// ==========================================
// 8. PLUS NOTIFICATIONS VIEW (الإشعارات)
// ==========================================
export const NotificationsPlusSettingsView: React.FC<ViewProps> = ({ onBack }) => {
  const { settings, showToast, currentUser } = useTelegram();
  const isArabic = settings.language === 'ar';

  const [bgNotifications, setBgNotifications] = useState(true);
  const [groupedPushStyle, setGroupedPushStyle] = useState(true);
  const [flashNotify, setFlashNotify] = useState(false);
  const [contactOnlineNotify, setContactOnlineNotify] = useState(false);
  const [avatarChangeNotify, setAvatarChangeNotify] = useState(true);
  const [permissionStatus, setPermissionStatus] = useState<string>(() => {
    return typeof window !== 'undefined' && 'Notification' in window ? Notification.permission : 'default';
  });

  const handleRequestPermission = async () => {
    if (typeof window === 'undefined' || !('Notification' in window)) {
      showToast(isArabic ? 'متصفحك لا يدعم إشعارات النظام' : 'Browser does not support system notifications', '⚠️');
      return;
    }

    try {
      const res = await Notification.requestPermission();
      setPermissionStatus(res);
      if (res === 'granted') {
        showToast(isArabic ? 'تم تفعيل إشعارات النظام وخارج التطبيق بنجاح' : 'System notifications enabled successfully', '🔔');
      } else {
        showToast(isArabic ? 'تم رفض إذن الإشعارات' : 'Notification permission denied', '❌');
      }
    } catch {
      showToast(isArabic ? 'خطأ في طلب إذن الإشعارات' : 'Error requesting permission', '❌');
    }
  };

  const handleSendTestPush = async () => {
    try {
      if (permissionStatus !== 'granted') {
        await handleRequestPermission();
      }

      if (typeof window !== 'undefined' && 'Notification' in window && Notification.permission === 'granted') {
        // Dispatch real system notification matching the user's screenshot
        const notifTitle = `Plus • ${currentUser.name || 'بيان احمد'} • 279313 رسالة جديدة من 70 محادثة • الآن`;
        const notifBody = `جامعة الامام محمد بن سعود :M: احد عنده قروب فقة العباد...\nالتوفر المتق.طع 🇾🇪🇰🇼🇷🇺🇹🇭 ➖➖➖➖➖➖➖(WhatsApp)`;

        if ('serviceWorker' in navigator && navigator.serviceWorker.controller) {
          navigator.serviceWorker.controller.postMessage({
            type: 'TRIGGER_BACKGROUND_NOTIFICATION',
            notification: {
              title: notifTitle,
              body: notifBody,
              avatar: 'https://images.unsplash.com/photo-1541339907198-e08756dedf3f?w=150&auto=format&fit=crop&q=80',
              chatId: 'chat_group_imam',
            },
          });
        } else {
          new Notification(notifTitle, {
            body: notifBody,
            icon: 'https://images.unsplash.com/photo-1541339907198-e08756dedf3f?w=150&auto=format&fit=crop&q=80',
          });
        }

        if ('vibrate' in navigator) {
          navigator.vibrate([200, 100, 200]);
        }

        showToast(
          isArabic
            ? 'تم إرسال إشعار حقيقي إلى شاشة الجوال والنظام بنجاح!'
            : 'Real system push notification sent to device!',
          '🔔'
        );
      } else {
        showToast(isArabic ? 'يرجى السماح بإذن الإشعارات أولاً' : 'Please grant permission first', '⚠️');
      }
    } catch {
      showToast(isArabic ? 'فشل إرسال الإشعار التجريبي' : 'Failed to send test push', '❌');
    }
  };

  return (
    <div className="flex-1 flex flex-col h-full bg-[#17212b] text-white overflow-y-auto font-sans">
      <SubHeader title={isArabic ? 'إعدادات الإشعارات المتقدمة' : 'Notifications Settings'} onBack={onBack} />

      <div className="p-4 space-y-6 max-w-2xl mx-auto w-full">
        {/* Section 1: Real System & Mobile Screen Push */}
        <div>
          <div className="text-xs font-semibold text-cyan-400 uppercase tracking-wider mb-2 px-1">
            {isArabic ? 'إشعارات الهاتف الحقيقية (خارج التطبيق)' : 'System & Background Notifications'}
          </div>
          <div className="bg-[#242f3d] rounded-xl overflow-hidden border border-white/5 divide-y divide-white/5">
            <ToggleItem
              title={isArabic ? 'إشعارات الخلفية وشاشة القفل (Background Push)' : 'Background & Lockscreen Push'}
              subtitle={
                isArabic
                  ? 'وصول الإشعارات إلى شاشة الجوال والنظام حتى عند تصغير أو إغلاق التطبيق'
                  : 'Receive notifications on device screen even when app is minimized or in background'
              }
              checked={bgNotifications}
              onChange={setBgNotifications}
              icon={<Smartphone size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'تنسيق الإشعارات المجمعة (Plus & Telegram Grouping)' : 'Grouped Notifications Style'}
              subtitle={
                isArabic
                  ? 'تجميع الرسائل بتنسيق بلاس (Plus • الحساب • عدد الرسائل والمحادثات) مع العدادات'
                  : 'Group incoming messages into stacked cards with unread badges'
              }
              checked={groupedPushStyle}
              onChange={setGroupedPushStyle}
              icon={<Layers size={18} />}
            />

            {/* Permission Action Button */}
            <div className="p-4 flex items-center justify-between bg-black/20">
              <div>
                <div className="text-sm font-semibold text-white">
                  {isArabic ? 'إذن إشعارات النظام والمتصفح' : 'System Notification Permission'}
                </div>
                <div className="text-xs text-gray-400 mt-0.5">
                  {isArabic ? 'الحالة الحالية:' : 'Current Status:'}{' '}
                  <span
                    className={`font-bold ${
                      permissionStatus === 'granted' ? 'text-emerald-400' : 'text-amber-400'
                    }`}
                  >
                    {permissionStatus === 'granted'
                      ? isArabic
                        ? 'مفعل ومسموح به ✅'
                        : 'Granted ✅'
                      : isArabic
                      ? 'يحتاج إلى تفعيل ⚠️'
                      : 'Not Granted ⚠️'}
                  </span>
                </div>
              </div>

              {permissionStatus !== 'granted' ? (
                <button
                  onClick={handleRequestPermission}
                  className="px-3.5 py-1.5 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-black font-bold text-xs shadow-md transition-all active:scale-95"
                >
                  {isArabic ? 'تفعيل الآن' : 'Enable'}
                </button>
              ) : (
                <span className="text-xs px-2.5 py-1 rounded-lg bg-emerald-500/20 text-emerald-300 font-semibold">
                  {isArabic ? 'جاهز ونشط' : 'Active'}
                </span>
              )}
            </div>

            {/* Test Push Button */}
            <div className="p-4 flex items-center justify-between bg-white/[0.02]">
              <div>
                <div className="text-sm font-semibold text-white">
                  {isArabic ? 'تجربة إشعار حقيقي خارج التطبيق' : 'Send Test Mobile Push'}
                </div>
                <div className="text-xs text-gray-400 mt-0.5">
                  {isArabic
                    ? 'إرسال إشعار فوري إلى واجهة الهاتف للتحقق من التنسيق والاهتزاز والصوت'
                    : 'Dispatch immediate push notification to test vibration, sound and styling'}
                </div>
              </div>

              <button
                onClick={handleSendTestPush}
                className="px-3.5 py-1.5 rounded-xl bg-[#2481cc] hover:bg-[#1f70b3] text-white font-bold text-xs shadow-md transition-all active:scale-95 flex items-center gap-1.5 shrink-0"
              >
                <Bell size={14} />
                <span>{isArabic ? 'إرسال تجربة' : 'Send Test'}</span>
              </button>
            </div>
          </div>
        </div>

        {/* Section 2: Contact Alerts & Flash */}
        <div>
          <div className="text-xs font-semibold text-cyan-400 uppercase tracking-wider mb-2 px-1">
            {isArabic ? 'تنبيهات جهات الاتصال والفلاش' : 'Contacts Alerts & Flash'}
          </div>
          <div className="bg-[#242f3d] rounded-xl overflow-hidden border border-white/5 divide-y divide-white/5">
            <ToggleItem
              title={isArabic ? 'إشعار عند اتصال جهة اتصال بالإنترنت' : 'Notify when Contact is Online'}
              subtitle={isArabic ? 'إرسال تنبيه فوري عند فتح جهة الاتصال لتطبيق تيليجرام' : 'Instant notification when a contact becomes online'}
              checked={contactOnlineNotify}
              onChange={setContactOnlineNotify}
              icon={<UserIcon size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'إشعار عند تغيير جهة الاتصال لصورتها' : 'Notify on Avatar Change'}
              subtitle={isArabic ? 'تنبيه فوري عند تحديث جهة الاتصال لصورة ملفها الشخصي' : 'Get notified whenever a contact updates profile picture'}
              checked={avatarChangeNotify}
              onChange={setAvatarChangeNotify}
              icon={<ImageIcon size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'تنبيه فلاش الكاميرا (LED Flash)' : 'Camera Flash Alerts'}
              subtitle={isArabic ? 'وميض فلاش الكاميرا عند ورود رسالة جديدة' : 'Flash camera LED when receiving messages'}
              checked={flashNotify}
              onChange={setFlashNotify}
              icon={<Zap size={18} />}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

// ==========================================
// 9. PLUS PRIVACY & SECURITY VIEW (الخصوصية والأمان)
// ==========================================
export const PrivacySecurityPlusSettingsView: React.FC<ViewProps> = ({ onBack }) => {
  const { settings, showToast } = useTelegram();
  const isArabic = settings.language === 'ar';

  const [ghostReadMode, setGhostReadMode] = useState(false);
  const [preventScreenshots, setPreventScreenshots] = useState(false);
  const [biometricLock, setBiometricLock] = useState(true);
  const [autoClearOnExit, setAutoClearOnExit] = useState(false);

  return (
    <div className="flex-1 flex flex-col h-full bg-[#17212b] text-white overflow-y-auto font-sans">
      <SubHeader title={isArabic ? 'الخصوصية والأمان المتقدم' : 'Privacy & Security Settings'} onBack={onBack} />

      <div className="p-4 space-y-6 max-w-2xl mx-auto w-full">
        <div>
          <div className="text-xs font-semibold text-cyan-400 uppercase tracking-wider mb-2 px-1">
            {isArabic ? 'حماية المحادثات والهوية' : 'Chat & Identity Protection'}
          </div>
          <div className="bg-[#242f3d] rounded-xl overflow-hidden border border-white/5 divide-y divide-white/5">
            <ToggleItem
              title={isArabic ? 'وضع القراءة المخفية (Ghost Read Mode)' : 'Ghost Read Receipts'}
              subtitle={isArabic ? 'قراءة الرسائل دون وضع علامتي الصح الزرقاء للطرف الآخر' : 'Read incoming messages without sending read receipts'}
              checked={ghostReadMode}
              onChange={setGhostReadMode}
              icon={<EyeOff size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'قفل التطبيق بالبصمة الحيوية' : 'Biometric App Lock'}
              subtitle={isArabic ? 'طلب بصمة الإصبع أو الوجه لفتح المحادثات' : 'Require fingerprint or face ID to access Telegram'}
              checked={biometricLock}
              onChange={setBiometricLock}
              icon={<Fingerprint size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'منع التقاط لقطات الشاشة (Screenshot Guard)' : 'Prevent Screen Capture'}
              subtitle={isArabic ? 'حظر لقطات وتسجيلات الشاشة داخل المحادثات لحماية الخصوصية' : 'Block screenshots and screen recording inside chats'}
              checked={preventScreenshots}
              onChange={setPreventScreenshots}
              icon={<Lock size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'مسح الذاكرة المؤقتة التلقائي عند الخروج' : 'Auto Clear Cache on Exit'}
              subtitle={isArabic ? 'حذف الملفات المؤقتة فور إغلاق التطبيق لتوفير المساحة' : 'Purge temporary cache files immediately upon exit'}
              checked={autoClearOnExit}
              onChange={setAutoClearOnExit}
              icon={<Trash2 size={18} />}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

// ==========================================
// 10. PLUS SHARED MEDIA VIEW (الوسائط المتبادلة)
// ==========================================
export const SharedMediaPlusSettingsView: React.FC<ViewProps> = ({ onBack }) => {
  const { settings, showToast } = useTelegram();
  const isArabic = settings.language === 'ar';

  const [gridColumns, setGridColumns] = useState(3);
  const [losslessDownload, setLosslessDownload] = useState(true);
  const [autoGroupMedia, setAutoGroupMedia] = useState(true);

  return (
    <div className="flex-1 flex flex-col h-full bg-[#17212b] text-white overflow-y-auto font-sans">
      <SubHeader title={isArabic ? 'الوسائط المتبادلة' : 'Shared Media Settings'} onBack={onBack} />

      <div className="p-4 space-y-6 max-w-2xl mx-auto w-full">
        <div>
          <div className="text-xs font-semibold text-cyan-400 uppercase tracking-wider mb-2 px-1">
            {isArabic ? 'تنسيق شبكة الوسائط' : 'Media Grid Configuration'}
          </div>
          <div className="bg-[#242f3d] p-4 rounded-xl border border-white/5 space-y-4">
            <div className="flex justify-between text-xs text-gray-300">
              <span>{isArabic ? 'عدد الأعمدة في الشبكة' : 'Grid Columns'}</span>
              <span className="font-bold text-cyan-400">{gridColumns} أعمدة</span>
            </div>
            <div className="grid grid-cols-3 gap-2">
              {[3, 4, 5].map((cols) => (
                <button
                  key={cols}
                  onClick={() => setGridColumns(cols)}
                  className={`p-2.5 rounded-xl border text-xs font-bold transition-all ${
                    gridColumns === cols
                      ? 'bg-cyan-500/20 border-cyan-400 text-cyan-400'
                      : 'bg-white/5 border-white/10 text-gray-300 hover:bg-white/10'
                  }`}
                >
                  {cols}x{cols}
                </button>
              ))}
            </div>
          </div>
        </div>

        <div className="bg-[#242f3d] rounded-xl overflow-hidden border border-white/5 divide-y divide-white/5">
          <ToggleItem
            title={isArabic ? 'تجميع الوسائط في ألبومات منظمة' : 'Group Media into Albums'}
            subtitle={isArabic ? 'ترتيب الصور والفيديوهات المتتالية في ألبوم واحد' : 'Group consecutive photos and videos into unified albums'}
            checked={autoGroupMedia}
            onChange={setAutoGroupMedia}
            icon={<LayoutGrid size={18} />}
          />
          <ToggleItem
            title={isArabic ? 'حفظ الوسائط بجودة غير مضغوطة' : 'Lossless Media Download'}
            subtitle={isArabic ? 'تنزيل الصور بدقتها الكاملة بدون ضغط' : 'Save photos and documents with original dimensions'}
            checked={losslessDownload}
            onChange={setLosslessDownload}
            icon={<Download size={18} />}
          />
        </div>
      </div>
    </div>
  );
};

// ==========================================
// 11. PLUS DOWNLOADS VIEW (التحميلات)
// ==========================================
export const DownloadsPlusSettingsView: React.FC<ViewProps> = ({ onBack }) => {
  const { settings, showToast } = useTelegram();
  const isArabic = settings.language === 'ar';

  const [multiThreaded, setMultiThreaded] = useState(true);
  const [autoResume, setAutoResume] = useState(true);
  const [downloadFolder, setDownloadFolder] = useState('/storage/emulated/0/Telegram/Telegram Documents');

  return (
    <div className="flex-1 flex flex-col h-full bg-[#17212b] text-white overflow-y-auto font-sans">
      <SubHeader title={isArabic ? 'إعدادات التحميلات' : 'Downloads Settings'} onBack={onBack} />

      <div className="p-4 space-y-6 max-w-2xl mx-auto w-full">
        <div>
          <div className="text-xs font-semibold text-cyan-400 uppercase tracking-wider mb-2 px-1">
            {isArabic ? 'سرعة وكفاءة التنزيل' : 'Download Acceleration & Management'}
          </div>
          <div className="bg-[#242f3d] rounded-xl overflow-hidden border border-white/5 divide-y divide-white/5">
            <ToggleItem
              title={isArabic ? 'التحميل المتوازي متعدد المسارات (Multi-Threaded)' : 'Multi-Threaded Parallel Downloads'}
              subtitle={isArabic ? 'مضاعفة سرعة تحميل الملفات الكبيرة عبر 8 اتصالات متزامنة' : 'Accelerate download speeds via 8 concurrent MTProto streams'}
              checked={multiThreaded}
              onChange={setMultiThreaded}
              icon={<Zap size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'الاستئناف التلقائي للتنزيلات المنقطعة' : 'Auto-Resume Broken Downloads'}
              subtitle={isArabic ? 'إعادة مواصلة التحميل فور عودة الاتصال دون إعادة من البداية' : 'Resume incomplete downloads automatically upon reconnection'}
              checked={autoResume}
              onChange={setAutoResume}
              icon={<RotateCw size={18} />}
            />
          </div>
        </div>

        {/* Custom Path */}
        <div>
          <div className="text-xs font-semibold text-cyan-400 uppercase tracking-wider mb-2 px-1">
            {isArabic ? 'مجلد الحفظ في الجهاز' : 'Storage Directory'}
          </div>
          <div className="bg-[#242f3d] p-3.5 rounded-xl border border-white/5 flex items-center justify-between">
            <div className="min-w-0 flex-1">
              <div className="text-xs text-gray-400">{isArabic ? 'مسار التخزين المخصص:' : 'Custom Path:'}</div>
              <div className="text-xs font-mono text-cyan-400 truncate mt-0.5">{downloadFolder}</div>
            </div>
            <button
              onClick={() => showToast(isArabic ? 'تم تأكيد مسار التخزين' : 'Directory confirmed', '📁')}
              className="px-3 py-1.5 bg-white/10 hover:bg-white/15 text-white rounded-lg text-xs font-semibold shrink-0 transition-colors"
            >
              {isArabic ? 'تغيير' : 'Change'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

// ==========================================
// 12. PLUS ADS VIEW (Ads)
// ==========================================
export const AdsPlusSettingsView: React.FC<ViewProps> = ({ onBack }) => {
  const { settings, showToast } = useTelegram();
  const isArabic = settings.language === 'ar';

  const [blockSponsoredAds, setBlockSponsoredAds] = useState(true);
  const [hidePromoMessages, setHidePromoMessages] = useState(true);
  const [supportCreators, setSupportCreators] = useState(true);

  return (
    <div className="flex-1 flex flex-col h-full bg-[#17212b] text-white overflow-y-auto font-sans">
      <SubHeader title={isArabic ? 'إعدادات الإعلانات (Ads)' : 'Ads Settings'} onBack={onBack} />

      <div className="p-4 space-y-6 max-w-2xl mx-auto w-full">
        {/* Ads Stats */}
        <div className="bg-gradient-to-r from-cyan-900/40 to-blue-900/40 border border-cyan-500/20 rounded-2xl p-4 flex items-center justify-between">
          <div>
            <div className="text-xs text-cyan-300 font-semibold">{isArabic ? 'الإعلانات المحجوبة' : 'Blocked Ads'}</div>
            <div className="text-2xl font-bold text-white mt-1">4,280</div>
            <div className="text-[11px] text-gray-300 mt-0.5">{isArabic ? 'تم توفير ~145 MB من البيانات' : 'Saved ~145 MB of network bandwidth'}</div>
          </div>
          <div className="w-12 h-12 rounded-2xl bg-cyan-500/20 text-cyan-400 flex items-center justify-center">
            <Shield size={24} />
          </div>
        </div>

        <div>
          <div className="text-xs font-semibold text-cyan-400 uppercase tracking-wider mb-2 px-1">
            {isArabic ? 'خيارات حظر وتصفية الإعلانات' : 'Ad Blocking & Filtering'}
          </div>
          <div className="bg-[#242f3d] rounded-xl overflow-hidden border border-white/5 divide-y divide-white/5">
            <ToggleItem
              title={isArabic ? 'حظر الإعلانات الممولة في القنوات' : 'Block Sponsored Ads in Channels'}
              subtitle={isArabic ? 'إخفاء الإعلانات الترويجية التي تظهر أسفل منشورات القنوات' : 'Remove sponsored promotional posts at channel footer'}
              checked={blockSponsoredAds}
              onChange={setBlockSponsoredAds}
              icon={<Shield size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'إخفاء الرسائل الترويجية الموصى بها' : 'Hide Recommended Promo Messages'}
              subtitle={isArabic ? 'منع النوافذ المنبثقة للترويج للقنوات الخارجية' : 'Suppress recommendation popups for external channels'}
              checked={hidePromoMessages}
              onChange={setHidePromoMessages}
              icon={<EyeOff size={18} />}
            />
            <ToggleItem
              title={isArabic ? 'دعم صناع المحتوى (TON & Stars)' : 'Creator Support System'}
              subtitle={isArabic ? 'السماح بالتبرع المباشر لصناع القنوات عبر Telegram Stars' : 'Allow direct donation tips to channel creators with Stars'}
              checked={supportCreators}
              onChange={setSupportCreators}
              icon={<Star size={18} />}
            />
          </div>
        </div>
      </div>
    </div>
  );
};
