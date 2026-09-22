import React, { useState } from 'react';
import {
  User,
  Users,
  Megaphone,
  Folder,
  Bookmark,
  Phone,
  Settings,
  PlusCircle,
  FolderKanban,
  Palette,
  Paintbrush,
  HelpCircle,
  ListOrdered,
  Moon,
  Sun,
  ChevronDown,
  ChevronUp,
  UserPlus,
  Check,
  X,
  Sparkles,
  Download,
  Radio,
  Zap,
  Send,
  Star,
  Lock,
  Eye,
  Layers,
  MessageSquare,
  Brain,
  Search,
  ExternalLink,
  ShieldCheck,
  LogOut,
} from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

export type DrawerAccount = {
  id: string;
  name: string;
  phone: string;
  username?: string;
  color?: string;
  role?: string;
};

interface NavigationDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  user: {
    name: string;
    phone?: string;
    username?: string;
    avatar?: string;
  } | null;
  accounts?: DrawerAccount[];
  activeAccountId?: string;
  onSwitchAccount?: (accId: string) => void;
  onOpenService: (serviceId: string) => void;
  onOpenSettings: (subpage?: string) => void;
  onOpenProfile: () => void;
  onToggleTheme: () => void;
  theme: 'dark' | 'light';
  onNewSecretChat?: () => void;
  onSavedMessages?: () => void;
  onLogout?: () => void;
}

export const NavigationDrawer: React.FC<NavigationDrawerProps> = ({
  isOpen,
  onClose,
  user,
  accounts = [],
  activeAccountId = '',
  onSwitchAccount,
  onOpenService,
  onOpenSettings,
  onOpenProfile,
  onToggleTheme,
  theme,
  onNewSecretChat,
  onSavedMessages,
  onLogout,
}) => {
  const [isAccountsExpanded, setIsAccountsExpanded] = useState(false);

  const handleAction = (cb: () => void) => {
    onClose();
    setTimeout(() => {
      cb();
    }, 150);
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-50 flex select-none font-sans" dir="rtl">
          {/* Backdrop with smooth blur and fade */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.25, ease: 'easeOut' }}
            onClick={onClose}
            className="fixed inset-0 bg-black/60 backdrop-blur-xs cursor-pointer"
          />

          {/* Drawer Sheet with Native Spring Motion */}
          <motion.div
            initial={{ x: '100%' }}
            animate={{ x: 0 }}
            exit={{ x: '100%' }}
            transition={{
              type: 'spring',
              damping: 32,
              stiffness: 350,
              mass: 0.8,
            }}
            className="relative w-80 max-w-[85vw] h-full flex flex-col bg-[#17212b] text-white shadow-2xl z-10 overflow-hidden border-l border-white/10"
          >
            {/* Header: User Profile Info with Rich Background */}
            <div className="relative pt-6 pb-4 px-4 bg-gradient-to-b from-[#2481cc]/30 via-[#1e3448]/40 to-[#17212b] border-b border-white/10">
              <div className="flex items-center justify-between mb-3">
                {/* User Avatar */}
                <div className="relative">
                  <div className="w-14 h-14 rounded-full bg-gradient-to-tr from-[#377c79] to-[#2481cc] flex items-center justify-center text-white text-xl font-black shadow-lg ring-2 ring-white/20">
                    {user?.avatar ? (
                      <img src={user.avatar} alt="" className="w-full h-full rounded-full object-cover" />
                    ) : (
                      <span>{user?.name ? user.name.slice(0, 1) : 'T'}</span>
                    )}
                  </div>
                  <span className="absolute bottom-0 left-0 w-3.5 h-3.5 rounded-full bg-emerald-500 ring-2 ring-[#17212b]" title="متصل الآن" />
                </div>

                {/* Top Quick Actions: Night/Day Toggle & Close */}
                <div className="flex items-center gap-1">
                  <button
                    type="button"
                    onClick={onToggleTheme}
                    title={theme === 'dark' ? 'التحويل للوضع النهاري' : 'التحويل للوضع الليلي'}
                    className="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 text-white/90 flex items-center justify-center transition-colors cursor-pointer"
                  >
                    {theme === 'dark' ? <Sun size={16} className="text-amber-300" /> : <Moon size={16} className="text-sky-300" />}
                  </button>
                  <button
                    type="button"
                    onClick={onClose}
                    title="إغلاق القائمة"
                    className="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 text-white/80 flex items-center justify-center transition-colors cursor-pointer"
                  >
                    <X size={16} />
                  </button>
                </div>
              </div>

              {/* User Identity Details & Multi-Account Dropdown Toggle */}
              <div
                onClick={() => setIsAccountsExpanded(!isAccountsExpanded)}
                className="flex items-center justify-between cursor-pointer group pt-1 select-none"
              >
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-1.5 font-bold text-sm text-white truncate">
                    <span className="truncate">{user?.name || 'حساب تيليجرام'}</span>
                    {user?.username && (
                      <span className="text-[10px] bg-white/15 px-1.5 py-0.5 rounded-full font-mono text-sky-200">
                        {user.username}
                      </span>
                    )}
                  </div>
                  {user?.phone && (
                    <div className="text-xs text-white/70 font-mono mt-0.5" dir="ltr">
                      {user.phone}
                    </div>
                  )}
                </div>

                <div className="p-1 rounded-full text-white/80 group-hover:bg-white/15 transition-colors">
                  {isAccountsExpanded ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
                </div>
              </div>
            </div>

            {/* Expandable Multi-Account Drawer Dropdown */}
            <AnimatePresence>
              {isAccountsExpanded && (
                <motion.div
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: 'auto', opacity: 1 }}
                  exit={{ height: 0, opacity: 0 }}
                  transition={{ duration: 0.2 }}
                  className="bg-[#0e1621] border-b border-white/10 py-2 px-2 max-h-56 overflow-y-auto space-y-1"
                >
                  {accounts.length === 0 ? (
                    <div className="text-center py-2 px-3 text-[11px] text-gray-400">
                      لا توجد حسابات إضافية متصلة
                    </div>
                  ) : (
                    accounts.map((acc) => {
                      const isActive = acc.id === activeAccountId;
                      return (
                        <button
                          key={acc.id}
                          type="button"
                          onClick={() => {
                            onSwitchAccount?.(acc.id);
                          }}
                          className={`w-full flex items-center justify-between px-3 py-2 rounded-xl transition-all ${
                            isActive ? 'bg-[#2481cc]/25 text-white border border-[#2481cc]/40' : 'hover:bg-white/5 text-gray-300'
                          }`}
                        >
                          <div className="flex items-center gap-2.5 min-w-0 text-right">
                            <div
                              className="w-7 h-7 rounded-full flex items-center justify-center text-white font-bold text-[10px] shrink-0"
                              style={{ backgroundColor: acc.color || '#0088cc' }}
                            >
                              {acc.name.charAt(0)}
                            </div>
                            <div className="min-w-0">
                              <div className="text-xs font-semibold truncate text-white">{acc.name}</div>
                              <div className="text-[10px] text-gray-400 font-mono truncate" dir="ltr">{acc.phone}</div>
                            </div>
                          </div>
                          {isActive && <Check size={14} className="text-sky-400 shrink-0" />}
                        </button>
                      );
                    })
                  )}

                  <button
                    type="button"
                    onClick={() => handleAction(() => onOpenService('accounts'))}
                    className="w-full flex items-center gap-2 px-3 py-2 mt-1 rounded-xl text-xs font-semibold text-sky-400 hover:bg-sky-500/10 transition-colors"
                  >
                    <div className="w-6 h-6 rounded-full border border-dashed border-sky-400 flex items-center justify-center">
                      <UserPlus size={12} className="text-sky-400" />
                    </div>
                    <span>إضافة وإدارة الحسابات</span>
                  </button>
                </motion.div>
              )}
            </AnimatePresence>

            {/* Navigation Items List */}
            <div className="flex-1 overflow-y-auto py-2 divide-y divide-white/5 scrollbar-thin">
              {/* Group 1: Core Telegram Navigation */}
              <div className="py-1 space-y-0.5">
                <button
                  type="button"
                  onClick={() => handleAction(onOpenProfile)}
                  className="w-full flex items-center gap-3.5 px-4 py-2.5 text-xs font-medium text-gray-200 hover:bg-white/5 hover:text-white transition-all text-right"
                >
                  <User size={18} className="text-gray-400 shrink-0" />
                  <span>الملف الشخصي</span>
                </button>

                <button
                  type="button"
                  onClick={() => handleAction(() => onOpenService('premium'))}
                  className="w-full flex items-center justify-between px-4 py-2.5 text-xs font-medium text-purple-300 hover:bg-purple-500/10 transition-all text-right group"
                >
                  <div className="flex items-center gap-3.5">
                    <Star size={18} className="text-amber-400 fill-amber-400/30 shrink-0 group-hover:scale-110 transition-transform" />
                    <span className="font-bold">Telegram Premium</span>
                  </div>
                  <span className="px-2 py-0.5 text-[9px] font-extrabold bg-gradient-to-r from-purple-500 to-pink-500 text-white rounded-full shadow-xs">
                    STAR & 4GB
                  </span>
                </button>

                <button
                  type="button"
                  onClick={() => handleAction(() => onNewSecretChat?.())}
                  className="w-full flex items-center justify-between px-4 py-2.5 text-xs font-medium text-emerald-300 hover:bg-emerald-500/10 transition-all text-right"
                >
                  <div className="flex items-center gap-3.5">
                    <Lock size={18} className="text-emerald-400 shrink-0" />
                    <span>محادثة سرية جديدة</span>
                  </div>
                  <span className="px-1.5 py-0.2 text-[9px] font-mono bg-emerald-500/20 text-emerald-300 rounded">
                    E2EE
                  </span>
                </button>

                <button
                  type="button"
                  onClick={() => handleAction(() => onOpenService('contacts'))}
                  className="w-full flex items-center gap-3.5 px-4 py-2.5 text-xs font-medium text-gray-200 hover:bg-white/5 hover:text-white transition-all text-right"
                >
                  <Users size={18} className="text-gray-400 shrink-0" />
                  <span>جهات الاتصال</span>
                </button>

                <button
                  type="button"
                  onClick={() => handleAction(() => onOpenService('services_center'))}
                  className="w-full flex items-center justify-between px-4 py-2.5 text-xs font-medium text-sky-300 hover:bg-sky-500/10 transition-all text-right"
                >
                  <div className="flex items-center gap-3.5">
                    <Sparkles size={18} className="text-sky-400 shrink-0" />
                    <span>تطبيقات وخدمات مصغرة (Mini Apps)</span>
                  </div>
                  <span className="px-1.5 py-0.5 text-[9px] font-bold bg-sky-500/20 text-sky-300 rounded-full">
                    NEW
                  </span>
                </button>
              </div>

              {/* Group 2: Automation & Smart Tools Suite (حزمة الأدوات والوظائف الذكية) */}
              <div className="py-2 space-y-0.5 bg-sky-950/20 border-y border-sky-500/15">
                <div className="px-4 pt-1 pb-1 text-[11px] font-bold text-sky-400 uppercase tracking-wider flex items-center justify-between">
                  <span>⚡ الأدوات والوظائف الذكية</span>
                  <span className="text-[9px] bg-sky-500/20 text-sky-300 px-1.5 py-0.2 rounded font-mono">PRO</span>
                </div>

                {/* 1. الإرسال والمراقبة */}
                <button
                  type="button"
                  onClick={() => handleAction(() => onOpenService('publishing_monitoring'))}
                  className="w-full flex items-center justify-between px-4 py-2.5 text-xs font-medium text-gray-100 hover:bg-sky-500/15 transition-all text-right group"
                >
                  <div className="flex items-center gap-3.5">
                    <Send size={18} className="text-sky-400 group-hover:scale-110 transition-transform shrink-0" />
                    <div className="flex flex-col text-right">
                      <span className="font-semibold text-white">الإرسال والمراقبة</span>
                      <span className="text-[10px] text-gray-400">نشر دوري ورصد فوري للكلمات</span>
                    </div>
                  </div>
                  <span className="px-1.5 py-0.5 text-[9px] font-bold bg-sky-500/25 text-sky-200 border border-sky-400/30 rounded font-mono">
                    TLRPC
                  </span>
                </button>

                {/* 2. رسائلي (سجل الدفعات) */}
                <button
                  type="button"
                  onClick={() => handleAction(() => onOpenService('saved_links'))}
                  className="w-full flex items-center justify-between px-4 py-2.5 text-xs font-medium text-gray-100 hover:bg-amber-500/15 transition-all text-right group"
                >
                  <div className="flex items-center gap-3.5">
                    <Layers size={18} className="text-amber-400 group-hover:scale-110 transition-transform shrink-0" />
                    <div className="flex flex-col text-right">
                      <span className="font-semibold text-white">رسائلي (سجل الدفعات)</span>
                      <span className="text-[10px] text-gray-400">إدارة الرسائل المرسلة والمجدولة</span>
                    </div>
                  </div>
                  <span className="px-1.5 py-0.5 text-[9px] font-bold bg-amber-500/25 text-amber-200 border border-amber-400/30 rounded font-mono">
                    BATCH
                  </span>
                </button>

                {/* 3. الردود التلقائية */}
                <button
                  type="button"
                  onClick={() => handleAction(() => onOpenService('autoreplies'))}
                  className="w-full flex items-center justify-between px-4 py-2.5 text-xs font-medium text-gray-100 hover:bg-purple-500/15 transition-all text-right group"
                >
                  <div className="flex items-center gap-3.5">
                    <MessageSquare size={18} className="text-purple-400 group-hover:scale-110 transition-transform shrink-0" />
                    <div className="flex flex-col text-right">
                      <span className="font-semibold text-white">الردود التلقائية</span>
                      <span className="text-[10px] text-gray-400">قواعد رد ذكية مع فلترة فورية</span>
                    </div>
                  </div>
                  <span className="px-1.5 py-0.5 text-[9px] font-bold bg-purple-500/25 text-purple-200 border border-purple-400/30 rounded font-mono">
                    AUTO
                  </span>
                </button>

                {/* 4. الانضمام الذكي بالقوائم والروابط */}
                <button
                  type="button"
                  onClick={() => handleAction(() => onOpenService('join'))}
                  className="w-full flex items-center justify-between px-4 py-2.5 text-xs font-medium text-gray-100 hover:bg-rose-500/15 transition-all text-right group"
                >
                  <div className="flex items-center gap-3.5">
                    <Zap size={18} className="text-rose-400 group-hover:scale-110 transition-transform shrink-0" />
                    <div className="flex flex-col text-right">
                      <span className="font-semibold text-white">الانضمام الذكي بالقوائم</span>
                      <span className="text-[10px] text-gray-400">سحب وانضمام تلقائي وسريع</span>
                    </div>
                  </div>
                  <span className="px-1.5 py-0.5 text-[9px] font-bold bg-rose-500/25 text-rose-200 border border-rose-400/30 rounded font-mono">
                    JOIN
                  </span>
                </button>

                {/* 5. نظام التعلم الذكي للرسائل */}
                <button
                  type="button"
                  onClick={() => handleAction(() => onOpenService('learning'))}
                  className="w-full flex items-center justify-between px-4 py-2.5 text-xs font-medium text-gray-100 hover:bg-blue-500/15 transition-all text-right group"
                >
                  <div className="flex items-center gap-3.5">
                    <Brain size={18} className="text-blue-400 group-hover:scale-110 transition-transform shrink-0" />
                    <div className="flex flex-col text-right">
                      <span className="font-semibold text-white">نظام التعلم الذكي للرسائل</span>
                      <span className="text-[10px] text-gray-400">تحليل وتوليد بالذكاء الاصطناعي</span>
                    </div>
                  </div>
                  <span className="px-1.5 py-0.5 text-[9px] font-bold bg-blue-500/25 text-blue-200 border border-blue-400/30 rounded font-mono">
                    AI
                  </span>
                </button>

                {/* 6. النشر الدوري المجدول */}
                <button
                  type="button"
                  onClick={() => handleAction(() => onOpenService('rotating'))}
                  className="w-full flex items-center justify-between px-4 py-2.5 text-xs font-medium text-gray-100 hover:bg-emerald-500/15 transition-all text-right group"
                >
                  <div className="flex items-center gap-3.5">
                    <Radio size={18} className="text-emerald-400 group-hover:scale-110 transition-transform shrink-0" />
                    <div className="flex flex-col text-right">
                      <span className="font-semibold text-white">النشر الدوري المجدول</span>
                      <span className="text-[10px] text-gray-400">جدولة فترات ودفعات النشر</span>
                    </div>
                  </div>
                  <span className="px-1.5 py-0.5 text-[9px] font-bold bg-emerald-500/25 text-emerald-200 border border-emerald-400/30 rounded font-mono">
                    ROTATE
                  </span>
                </button>

                {/* 7. الروابط والمجموعات المحفوظة */}
                <button
                  type="button"
                  onClick={() => handleAction(() => onOpenService('saved_links'))}
                  className="w-full flex items-center justify-between px-4 py-2.5 text-xs font-medium text-gray-100 hover:bg-cyan-500/15 transition-all text-right group"
                >
                  <div className="flex items-center gap-3.5">
                    <Search size={18} className="text-cyan-400 group-hover:scale-110 transition-transform shrink-0" />
                    <div className="flex flex-col text-right">
                      <span className="font-semibold text-white">الروابط والمجموعات المحفوظة</span>
                      <span className="text-[10px] text-gray-400">استعراض وبحث الروابط المخزنة</span>
                    </div>
                  </div>
                  <span className="px-1.5 py-0.5 text-[9px] font-bold bg-cyan-500/25 text-cyan-200 border border-cyan-400/30 rounded font-mono">
                    LINKS
                  </span>
                </button>
              </div>

              {/* Group 3: Core Chat Folders & Calls */}
              <div className="py-1 space-y-0.5">
                <button
                  type="button"
                  onClick={() => handleAction(() => onOpenSettings('folders'))}
                  className="w-full flex items-center gap-3.5 px-4 py-2.5 text-xs font-medium text-gray-200 hover:bg-white/5 hover:text-white transition-all text-right"
                >
                  <Folder size={18} className="text-gray-400 shrink-0" />
                  <span>مجلدات المحادثات</span>
                </button>

                <button
                  type="button"
                  onClick={() => handleAction(() => onSavedMessages?.())}
                  className="w-full flex items-center gap-3.5 px-4 py-2.5 text-xs font-medium text-gray-200 hover:bg-white/5 hover:text-white transition-all text-right"
                >
                  <Bookmark size={18} className="text-gray-400 shrink-0" />
                  <span>الرسائل المحفوظة</span>
                </button>

                <button
                  type="button"
                  onClick={() => handleAction(() => onOpenService('calls'))}
                  className="w-full flex items-center gap-3.5 px-4 py-2.5 text-xs font-medium text-gray-200 hover:bg-white/5 hover:text-white transition-all text-right"
                >
                  <Phone size={18} className="text-gray-400 shrink-0" />
                  <span>المكالمات الصوتية</span>
                </button>

                {/* Direct Install & APK Suite (DrKLO Engine) */}
                <button
                  type="button"
                  onClick={() => handleAction(() => onOpenService('install'))}
                  className="w-full flex items-center justify-between px-4 py-2.5 text-xs font-medium text-emerald-300 hover:bg-emerald-500/10 transition-all text-right"
                >
                  <div className="flex items-center gap-3.5">
                    <Download size={18} className="text-emerald-400 shrink-0" />
                    <span>تثبيت التطبيق على الجوال</span>
                  </div>
                  <span className="px-2 py-0.5 text-[10px] font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 rounded-full">
                    v12.9.2
                  </span>
                </button>

                <button
                  type="button"
                  onClick={() => handleAction(() => onOpenSettings('main'))}
                  className="w-full flex items-center gap-3.5 px-4 py-2.5 text-xs font-medium text-gray-200 hover:bg-white/5 hover:text-white transition-all text-right"
                >
                  <Settings size={18} className="text-gray-400 shrink-0" />
                  <span>الإعدادات</span>
                </button>
              </div>

              {/* Group 4: Plus Settings & Themes (إعدادات بلاس والتخصيص) */}
              <div className="py-1 space-y-0.5">
                <button
                  type="button"
                  onClick={() => handleAction(() => onOpenSettings('plus_settings'))}
                  className="w-full flex items-center gap-3.5 px-4 py-2.5 text-xs font-medium text-gray-200 hover:bg-white/5 hover:text-white transition-all text-right"
                >
                  <PlusCircle size={18} className="text-[#5288c1] shrink-0" />
                  <span>إعدادات بلاس</span>
                </button>

                <button
                  type="button"
                  onClick={() => handleAction(() => onOpenSettings('folders'))}
                  className="w-full flex items-center gap-3.5 px-4 py-2.5 text-xs font-medium text-gray-200 hover:bg-white/5 hover:text-white transition-all text-right"
                >
                  <FolderKanban size={18} className="text-gray-400 shrink-0" />
                  <span>التصنيفات</span>
                </button>

                <button
                  type="button"
                  onClick={() => handleAction(() => onOpenSettings('themes_browser'))}
                  className="w-full flex items-center gap-3.5 px-4 py-2.5 text-xs font-medium text-gray-200 hover:bg-white/5 hover:text-white transition-all text-right"
                >
                  <Palette size={18} className="text-gray-400 shrink-0" />
                  <span>تنزيل أنماط</span>
                </button>

                <button
                  type="button"
                  onClick={() => handleAction(() => onOpenSettings('theme_coloring'))}
                  className="w-full flex items-center gap-3.5 px-4 py-2.5 text-xs font-medium text-gray-200 hover:bg-white/5 hover:text-white transition-all text-right"
                >
                  <Paintbrush size={18} className="text-gray-400 shrink-0" />
                  <span>تلوين النمط</span>
                </button>

                <button
                  type="button"
                  onClick={() => handleAction(() => onOpenService('support'))}
                  className="w-full flex items-center gap-3.5 px-4 py-2.5 text-xs font-medium text-gray-200 hover:bg-white/5 hover:text-white transition-all text-right"
                >
                  <HelpCircle size={18} className="text-gray-400 shrink-0" />
                  <span>مجموعة الدعم والمساعدة</span>
                </button>

                <button
                  type="button"
                  onClick={() => handleAction(() => onOpenSettings('chat_settings'))}
                  className="w-full flex items-center gap-3.5 px-4 py-2.5 text-xs font-medium text-gray-200 hover:bg-white/5 hover:text-white transition-all text-right"
                >
                  <ListOrdered size={18} className="text-gray-400 shrink-0" />
                  <span>عدادات المحادثات</span>
                </button>

                {/* Logout Option */}
                <button
                  type="button"
                  onClick={() => handleAction(() => onLogout?.())}
                  className="w-full flex items-center gap-3.5 px-4 py-2.5 text-xs font-semibold text-rose-300 hover:bg-rose-500/15 hover:text-rose-200 transition-all text-right cursor-pointer"
                >
                  <LogOut size={18} className="text-rose-400 shrink-0" />
                  <span>تسجيل الخروج من الحساب</span>
                </button>
              </div>

              {/* Footer: Exact Telegram_anwer saif (DrKLO Official Build) & Developer Information */}
              <div className="py-3.5 px-4 text-xs text-gray-400 space-y-2 border-t border-white/10 mt-1 bg-black/25">
                <div className="flex items-center gap-2.5">
                  <div className="relative shrink-0 w-8 h-8 rounded-full bg-gradient-to-tr from-sky-600 to-blue-500 flex items-center justify-center shadow-lg shadow-sky-500/20">
                    <svg className="w-4.5 h-4.5 text-white -translate-x-0.5 relative z-10" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm4.64 6.8c-.15 1.58-.8 5.42-1.13 7.19-.14.75-.42 1-.68 1.03-.58.05-1.02-.38-1.58-.75-.88-.58-1.38-.94-2.23-1.5-.99-.65-.35-1.01.22-1.59.15-.15 2.71-2.48 2.76-2.69a.2.2 0 00-.05-.18c-.06-.05-.14-.03-.21-.02-.09.02-1.49.95-4.22 2.79-.4.27-.76.41-1.08.4-.36-.01-1.04-.2-1.55-.37-.63-.2-1.12-.31-1.08-.66.02-.18.27-.36.74-.55 2.92-1.27 4.86-2.11 5.83-2.52 2.77-1.16 3.35-1.36 3.73-1.36.08 0 .27.02.39.12.1.08.13.19.14.27-.01.06.01.24 0 .37z" />
                    </svg>
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="font-bold text-[12px] text-white tracking-tight leading-tight">
                      Telegram_anwer saif (DrKLO Official Build)
                    </div>
                    <div className="text-[10.5px] text-sky-400 font-mono font-semibold mt-0.5">
                      v12.9.2.0 (2246) universal arm64-v8a
                    </div>
                  </div>
                </div>

                <div className="pt-2 border-t border-white/10 space-y-1.5">
                  <div className="flex items-center justify-between gap-1 text-[11px]">
                    <span className="text-gray-400 font-semibold">المطور:</span>
                    <span className="text-white font-medium">انور فواد محمد علي سيف</span>
                  </div>

                  <div className="flex items-center justify-between gap-1.5 p-1.5 rounded-lg bg-sky-500/10 border border-sky-500/25 text-sky-300 text-[10.5px]">
                    <span className="flex items-center gap-1 font-semibold">
                      <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                      بوابة تيليجرام MTProto:
                    </span>
                    <span className="font-mono font-bold text-sky-200">ID: 22043994</span>
                  </div>

                  <div className="flex items-center justify-between gap-2 p-1.5 rounded-lg bg-emerald-500/10 border border-emerald-500/25 text-emerald-400">
                    <span className="text-[10px] text-emerald-300 font-semibold">رقم التواصل المباشر:</span>
                    <a
                      href="tel:+966510349663"
                      className="text-[11px] font-mono font-bold hover:underline text-emerald-300"
                      dir="ltr"
                    >
                      +966 51 034 9663
                    </a>
                  </div>

                  <div className="flex flex-wrap items-center justify-between gap-x-2 gap-y-1 text-[10px] font-mono text-gray-400 pt-0.5" dir="ltr">
                    <a href="tel:+966562570935" className="hover:text-sky-300">
                      +966 562 570 935
                    </a>
                    <a href="tel:+967772997043" className="hover:text-sky-300">
                      +967 772 997 043
                    </a>
                  </div>
                </div>
              </div>
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
};
