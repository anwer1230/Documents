import React, { useState } from "react";
import {
  Search,
  MoreVertical,
  Camera,
  ChevronLeft,
  ChevronRight,
  User,
  MessageSquare,
  Shield,
  Bell,
  HardDrive,
  Folder,
  Smartphone,
  BatteryCharging,
  Globe,
  Star,
  Sparkles,
  Store,
  Gift,
  HelpCircle,
  FileQuestion,
  Lightbulb,
  ShieldCheck,
  Check,
  Copy,
  LogOut,
  Palette,
  Key,
} from "lucide-react";
import { TelegramUser, AppTheme } from "../types";
import { getStoredSession } from "../lib/telegramApi";

interface TelegramAndroidSettingsProps {
  currentUser: TelegramUser | null;
  currentTheme: AppTheme;
  onSelectTheme: (theme: AppTheme) => void;
  onLogout: () => void;
  onOpenAuth?: () => void;
  onOpenGhostMode?: () => void;
  onOpenMultiAccounts?: () => void;
}

export const TelegramAndroidSettings: React.FC<TelegramAndroidSettingsProps> = ({
  currentUser,
  currentTheme,
  onSelectTheme,
  onLogout,
  onOpenAuth,
  onOpenGhostMode,
  onOpenMultiAccounts,
}) => {
  const [copiedSession, setCopiedSession] = useState(false);
  const sessionString = getStoredSession() || "جلسة تجريبية (Demo Session)";

  const handleCopySession = () => {
    navigator.clipboard.writeText(sessionString);
    setCopiedSession(true);
    setTimeout(() => setCopiedSession(false), 2000);
  };

  const userDisplayName = currentUser?.firstName 
    ? `${currentUser.firstName} ${currentUser.lastName || ""}`.trim()
    : "أمل العنزي";
  const userPhone = currentUser?.phone || "+20 127 4386864";
  const userHandle = currentUser?.username ? `@${currentUser.username}` : "@Amalservices";

  return (
    <div className="flex-1 flex flex-col bg-[#f0f2f5] dark:bg-[#0e1621] overflow-y-auto select-none" dir="rtl">
      {/* Top Android Header */}
      <div className="sticky top-0 z-20 bg-white/90 dark:bg-[#17212b]/90 backdrop-blur-md px-4 py-3 flex items-center justify-between border-b border-slate-200/60 dark:border-slate-800">
        <div className="flex items-center gap-2">
          <span className="font-bold text-base text-slate-800 dark:text-white">في انتظار الشبكة...</span>
        </div>
        <div className="flex items-center gap-3 text-slate-600 dark:text-slate-300">
          <button className="w-9 h-9 rounded-full flex items-center justify-center hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors">
            <Search className="w-5 h-5" />
          </button>
          <button className="w-9 h-9 rounded-full flex items-center justify-center hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors">
            <MoreVertical className="w-5 h-5" />
          </button>
        </div>
      </div>

      <div className="p-3.5 max-w-xl mx-auto w-full space-y-3.5 pb-20">
        {/* Profile Card */}
        <div className="bg-white dark:bg-[#17212b] rounded-2xl p-5 shadow-2xs border border-slate-200/50 dark:border-slate-800 flex flex-col items-center text-center relative">
          <div className="relative mb-3">
            <div className="w-24 h-24 rounded-full overflow-hidden border-2 border-slate-100 dark:border-slate-700 bg-gradient-to-tr from-sky-400 to-indigo-600 flex items-center justify-center shadow-md">
              {currentUser?.photoUrl ? (
                <img src={currentUser.photoUrl} alt={userDisplayName} className="w-full h-full object-cover" />
              ) : (
                <span className="text-white text-3xl font-bold">{userDisplayName.charAt(0)}</span>
              )}
            </div>
            <button className="absolute bottom-0 right-0 w-8 h-8 rounded-full bg-[#2481cc] text-white flex items-center justify-center shadow-md hover:bg-[#1d6fa8] transition-transform active:scale-90 border-2 border-white dark:border-[#17212b]">
              <Camera className="w-4 h-4" />
            </button>
          </div>

          <h2 className="font-bold text-xl text-slate-900 dark:text-white mb-0.5">{userDisplayName}</h2>
          <p className="text-xs text-slate-500 dark:text-slate-400 font-medium" dir="ltr">
            {userPhone} • {userHandle}
          </p>
        </div>

        {/* Card 1: Accounts Switcher (الحسابات) */}
        <div className="bg-white dark:bg-[#17212b] rounded-2xl p-4 shadow-2xs border border-slate-200/50 dark:border-slate-800 space-y-3">
          <div className="flex items-center justify-between text-xs font-bold text-[#2481cc]">
            <span>الحسابات</span>
          </div>

          <div
            onClick={onOpenMultiAccounts}
            className="flex items-center justify-between p-2 rounded-xl hover:bg-slate-50 dark:hover:bg-slate-800/60 cursor-pointer transition-colors"
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-gradient-to-tr from-amber-400 to-rose-500 overflow-hidden flex items-center justify-center text-white font-bold text-sm shadow-xs">
                <span>L</span>
              </div>
              <div className="text-right">
                <span className="font-bold text-sm text-slate-800 dark:text-slate-100">Lamis</span>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-5 h-5 rounded-full bg-[#2481cc] text-white text-[11px] font-bold flex items-center justify-center">
                5
              </span>
              <ChevronLeft className="w-4 h-4 text-slate-400" />
            </div>
          </div>
        </div>

        {/* Card 2: Main Settings Group (الحساب، المحادثات، الخصوصية...) */}
        <div className="bg-white dark:bg-[#17212b] rounded-2xl shadow-2xs border border-slate-200/50 dark:border-slate-800 divide-y divide-slate-100 dark:divide-slate-800/80 overflow-hidden">
          {/* الحساب */}
          <div className="flex items-center justify-between p-3.5 hover:bg-slate-50 dark:hover:bg-slate-800/50 cursor-pointer transition-colors">
            <div className="flex items-center gap-3.5">
              <div className="w-9 h-9 rounded-xl bg-[#2481cc] text-white flex items-center justify-center shadow-xs">
                <User className="w-5 h-5" />
              </div>
              <div>
                <h4 className="font-bold text-sm text-slate-800 dark:text-slate-100">الحساب</h4>
                <p className="text-[11px] text-slate-400">الرقم، اسم المستخدم، النبذة</p>
              </div>
            </div>
          </div>

          {/* إعدادات المحادثات */}
          <div className="flex items-center justify-between p-3.5 hover:bg-slate-50 dark:hover:bg-slate-800/50 cursor-pointer transition-colors">
            <div className="flex items-center gap-3.5">
              <div className="w-9 h-9 rounded-xl bg-[#f08234] text-white flex items-center justify-center shadow-xs">
                <MessageSquare className="w-5 h-5" />
              </div>
              <div>
                <h4 className="font-bold text-sm text-slate-800 dark:text-slate-100">إعدادات المحادثات</h4>
                <p className="text-[11px] text-slate-400">خلفية الشاشة، الوضع الليلي، المؤثرات الحركية</p>
              </div>
            </div>
          </div>

          {/* الخصوصية والأمان */}
          <div className="flex items-center justify-between p-3.5 hover:bg-slate-50 dark:hover:bg-slate-800/50 cursor-pointer transition-colors">
            <div className="flex items-center gap-3.5">
              <div className="w-9 h-9 rounded-xl bg-[#4fae4e] text-white flex items-center justify-center shadow-xs">
                <Shield className="w-5 h-5" />
              </div>
              <div>
                <h4 className="font-bold text-sm text-slate-800 dark:text-slate-100">الخصوصية والأمان</h4>
                <p className="text-[11px] text-slate-400">آخر ظهور، الأجهزة، مفاتيح المرور</p>
              </div>
            </div>
          </div>

          {/* الإشعارات */}
          <div className="flex items-center justify-between p-3.5 hover:bg-slate-50 dark:hover:bg-slate-800/50 cursor-pointer transition-colors">
            <div className="flex items-center gap-3.5">
              <div className="w-9 h-9 rounded-xl bg-[#e53935] text-white flex items-center justify-center shadow-xs">
                <Bell className="w-5 h-5" />
              </div>
              <div>
                <h4 className="font-bold text-sm text-slate-800 dark:text-slate-100">الإشعارات</h4>
                <p className="text-[11px] text-slate-400">الأصوات، المكالمات، الشارات</p>
              </div>
            </div>
          </div>

          {/* البيانات والتخزين */}
          <div className="flex items-center justify-between p-3.5 hover:bg-slate-50 dark:hover:bg-slate-800/50 cursor-pointer transition-colors">
            <div className="flex items-center gap-3.5">
              <div className="w-9 h-9 rounded-xl bg-[#1e88e5] text-white flex items-center justify-center shadow-xs">
                <HardDrive className="w-5 h-5" />
              </div>
              <div>
                <h4 className="font-bold text-sm text-slate-800 dark:text-slate-100">البيانات والتخزين</h4>
                <p className="text-[11px] text-slate-400">إعدادات تنزيل الوسائط</p>
              </div>
            </div>
          </div>

          {/* مجلدات المحادثات */}
          <div className="flex items-center justify-between p-3.5 hover:bg-slate-50 dark:hover:bg-slate-800/50 cursor-pointer transition-colors">
            <div className="flex items-center gap-3.5">
              <div className="w-9 h-9 rounded-xl bg-[#29b6f6] text-white flex items-center justify-center shadow-xs">
                <Folder className="w-5 h-5" />
              </div>
              <div>
                <h4 className="font-bold text-sm text-slate-800 dark:text-slate-100">مجلدات المحادثات</h4>
                <p className="text-[11px] text-slate-400">فرز المحادثات في مجلدات</p>
              </div>
            </div>
          </div>

          {/* الأجهزة */}
          <div className="flex items-center justify-between p-3.5 hover:bg-slate-50 dark:hover:bg-slate-800/50 cursor-pointer transition-colors">
            <div className="flex items-center gap-3.5">
              <div className="w-9 h-9 rounded-xl bg-[#00acc1] text-white flex items-center justify-center shadow-xs">
                <Smartphone className="w-5 h-5" />
              </div>
              <div>
                <h4 className="font-bold text-sm text-slate-800 dark:text-slate-100">الأجهزة</h4>
                <p className="text-[11px] text-slate-400">إدارة الأجهزة المتصلة</p>
              </div>
            </div>
          </div>

          {/* توفير الطاقة */}
          <div className="flex items-center justify-between p-3.5 hover:bg-slate-50 dark:hover:bg-slate-800/50 cursor-pointer transition-colors">
            <div className="flex items-center gap-3.5">
              <div className="w-9 h-9 rounded-xl bg-[#fb8c00] text-white flex items-center justify-center shadow-xs">
                <BatteryCharging className="w-5 h-5" />
              </div>
              <div>
                <h4 className="font-bold text-sm text-slate-800 dark:text-slate-100">توفير الطاقة</h4>
                <p className="text-[11px] text-slate-400">تقليل استهلاك الطاقة عند انخفاض الشحن</p>
              </div>
            </div>
          </div>

          {/* اللغة */}
          <div className="flex items-center justify-between p-3.5 hover:bg-slate-50 dark:hover:bg-slate-800/50 cursor-pointer transition-colors">
            <div className="flex items-center gap-3.5">
              <div className="w-9 h-9 rounded-xl bg-[#8e24aa] text-white flex items-center justify-center shadow-xs">
                <Globe className="w-5 h-5" />
              </div>
              <div>
                <h4 className="font-bold text-sm text-slate-800 dark:text-slate-100">اللغة</h4>
                <p className="text-[11px] text-slate-400">العربية</p>
              </div>
            </div>
          </div>
        </div>

        {/* Card 3: Premium / Business / Stars */}
        <div className="bg-white dark:bg-[#17212b] rounded-2xl shadow-2xs border border-slate-200/50 dark:border-slate-800 divide-y divide-slate-100 dark:divide-slate-800/80 overflow-hidden">
          {/* تيليجرام المميز */}
          <div className="flex items-center justify-between p-3.5 hover:bg-slate-50 dark:hover:bg-slate-800/50 cursor-pointer transition-colors">
            <div className="flex items-center gap-3.5">
              <div className="w-9 h-9 rounded-xl bg-[#7c4dff] text-white flex items-center justify-center shadow-xs">
                <Star className="w-5 h-5 fill-white" />
              </div>
              <div>
                <h4 className="font-bold text-sm text-slate-800 dark:text-slate-100">تيليجرام المُميّز</h4>
              </div>
            </div>
          </div>

          {/* نجوم تيليجرام */}
          <div className="flex items-center justify-between p-3.5 hover:bg-slate-50 dark:hover:bg-slate-800/50 cursor-pointer transition-colors">
            <div className="flex items-center gap-3.5">
              <div className="w-9 h-9 rounded-xl bg-[#f59e0b] text-white flex items-center justify-center shadow-xs">
                <Sparkles className="w-5 h-5" />
              </div>
              <div>
                <h4 className="font-bold text-sm text-slate-800 dark:text-slate-100">نجوم تيليجرام</h4>
              </div>
            </div>
          </div>

          {/* تيليجرام الأعمال */}
          <div className="flex items-center justify-between p-3.5 hover:bg-slate-50 dark:hover:bg-slate-800/50 cursor-pointer transition-colors">
            <div className="flex items-center gap-3.5">
              <div className="w-9 h-9 rounded-xl bg-[#e11d48] text-white flex items-center justify-center shadow-xs">
                <Store className="w-5 h-5" />
              </div>
              <div>
                <h4 className="font-bold text-sm text-slate-800 dark:text-slate-100">تيليجرام الأعمال</h4>
              </div>
            </div>
          </div>

          {/* إرسال هدية */}
          <div className="flex items-center justify-between p-3.5 hover:bg-slate-50 dark:hover:bg-slate-800/50 cursor-pointer transition-colors">
            <div className="flex items-center gap-3.5">
              <div className="w-9 h-9 rounded-xl bg-[#f97316] text-white flex items-center justify-center shadow-xs">
                <Gift className="w-5 h-5" />
              </div>
              <div>
                <h4 className="font-bold text-sm text-slate-800 dark:text-slate-100">إرسال هدية</h4>
              </div>
            </div>
          </div>
        </div>

        {/* Card 4: Help (مساعدة) */}
        <div className="bg-white dark:bg-[#17212b] rounded-2xl p-4 shadow-2xs border border-slate-200/50 dark:border-slate-800 space-y-3">
          <div className="flex items-center justify-between text-xs font-bold text-[#2481cc]">
            <span>مساعدة</span>
          </div>

          <div className="divide-y divide-slate-100 dark:divide-slate-800/80">
            {/* اسأل سؤالاً */}
            <div className="flex items-center justify-between py-2.5 hover:bg-slate-50 dark:hover:bg-slate-800/50 cursor-pointer transition-colors">
              <div className="flex items-center gap-3.5">
                <div className="w-8 h-8 rounded-xl bg-[#f59e0b] text-white flex items-center justify-center shadow-xs">
                  <MessageSquare className="w-4 h-4" />
                </div>
                <h4 className="font-bold text-sm text-slate-800 dark:text-slate-100">اسأل سؤالاً</h4>
              </div>
            </div>

            {/* الأسئلة الشائعة */}
            <div className="flex items-center justify-between py-2.5 hover:bg-slate-50 dark:hover:bg-slate-800/50 cursor-pointer transition-colors">
              <div className="flex items-center gap-3.5">
                <div className="w-8 h-8 rounded-xl bg-[#2481cc] text-white flex items-center justify-center shadow-xs">
                  <FileQuestion className="w-4 h-4" />
                </div>
                <h4 className="font-bold text-sm text-slate-800 dark:text-slate-100">الأسئلة الشائعة</h4>
              </div>
            </div>

            {/* ميزات تيليجرام */}
            <div className="flex items-center justify-between py-2.5 hover:bg-slate-50 dark:hover:bg-slate-800/50 cursor-pointer transition-colors">
              <div className="flex items-center gap-3.5">
                <div className="w-8 h-8 rounded-xl bg-[#9333ea] text-white flex items-center justify-center shadow-xs">
                  <Lightbulb className="w-4 h-4" />
                </div>
                <h4 className="font-bold text-sm text-slate-800 dark:text-slate-100">ميزات تيليجرام</h4>
              </div>
            </div>

            {/* سياسة الخصوصية */}
            <div className="flex items-center justify-between py-2.5 hover:bg-slate-50 dark:hover:bg-slate-800/50 cursor-pointer transition-colors">
              <div className="flex items-center gap-3.5">
                <div className="w-8 h-8 rounded-xl bg-[#22c55e] text-white flex items-center justify-center shadow-xs">
                  <ShieldCheck className="w-4 h-4" />
                </div>
                <h4 className="font-bold text-sm text-slate-800 dark:text-slate-100">سياسة الخصوصية</h4>
              </div>
            </div>
          </div>
        </div>

        {/* MTProto Session & Technical Info Card */}
        <div className="bg-white dark:bg-[#17212b] rounded-2xl p-4 shadow-2xs border border-slate-200/50 dark:border-slate-800 space-y-2.5">
          <div className="flex items-center justify-between">
            <span className="font-bold text-xs text-slate-700 dark:text-slate-300 flex items-center gap-1.5">
              <Key className="w-3.5 h-3.5 text-[#2481cc]" />
              <span>جلسة تيليجرام MTProto الحالية</span>
            </span>
            <button
              onClick={handleCopySession}
              className="text-[11px] text-[#2481cc] font-bold flex items-center gap-1 hover:underline"
            >
              {copiedSession ? (
                <>
                  <Check className="w-3 h-3 text-emerald-500" />
                  <span className="text-emerald-500">تم النسخ</span>
                </>
              ) : (
                <>
                  <Copy className="w-3 h-3" />
                  <span>نسخ الجلسة</span>
                </>
              )}
            </button>
          </div>
          <div className="p-2.5 bg-slate-50 dark:bg-slate-900 rounded-xl font-mono text-[10px] text-slate-500 dark:text-slate-400 break-all select-all border border-slate-200/60 dark:border-slate-800">
            {sessionString.substring(0, 70)}...
          </div>
        </div>

        {/* Footer Version Label */}
        <div className="text-center pt-2 pb-6 text-slate-400 dark:text-slate-500 text-xs space-y-0.5 font-medium select-text">
          <p>تيليجرام للأندرويد v12.9.2 (6991)</p>
          <p className="text-[11px]">store bundled arm64-v8a</p>
        </div>
      </div>
    </div>
  );
};
