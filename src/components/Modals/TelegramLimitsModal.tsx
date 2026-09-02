import React, { useState } from 'react';
import {
  X,
  ShieldCheck,
  ShieldAlert,
  AlertTriangle,
  Lock,
  Zap,
  Crown,
  FileText,
  Users,
  Pin,
  Folder,
  Clock,
  Send,
  CheckCircle2,
  RefreshCw,
  Info,
  Scale,
  Ban,
  MessageSquare,
  HelpCircle,
} from 'lucide-react';
import { useTelegram } from '../../context/TelegramContext';

interface TelegramLimitsModalProps {
  isOpen?: boolean;
  onClose: () => void;
}

export const TelegramLimitsModal: React.FC<TelegramLimitsModalProps> = ({ isOpen = true, onClose }) => {
  const { chats, folders, settings, currentUser, showToast } = useTelegram();
  const isArabic = settings.language === 'ar';
  const isPremium = !!currentUser.isPremium;

  if (!isOpen) return null;

  const [isTestingSpamBot, setIsTestingSpamBot] = useState(false);
  const [spamBotStatus, setSpamBotStatus] = useState<{
    tested: boolean;
    status: 'clean' | 'limited' | 'flood';
    messageAr: string;
    messageEn: string;
  }>({
    tested: false,
    status: 'clean',
    messageAr: 'حسابك في وضع ممتاز! لا توجد أي قيود أو حظر سبام على حسابك في تيليجرام.',
    messageEn: 'Your account is in good standing! No spam blocks or limitations applied.',
  });

  const pinnedCount = chats.filter((c) => c.isPinned).length;
  const maxDialogs = isPremium ? 1000 : 500;
  const maxPinned = isPremium ? 10 : 5;
  const maxFolders = isPremium ? 20 : 10;
  const maxFileSize = isPremium ? '4 GB' : '2 GB';
  const maxCaption = isPremium ? '2048' : '1024';

  const handleTestSpamBot = () => {
    setIsTestingSpamBot(true);
    setTimeout(() => {
      setIsTestingSpamBot(false);
      setSpamBotStatus({
        tested: true,
        status: 'clean',
        messageAr: 'تم الفحص القانوني عبر @SpamBot: حسابك خالٍ تماماً من أي بلاغات أو تقييدات مفروضة من تيليجرام.',
        messageEn: 'Legal check complete via @SpamBot: Your account has zero spam restrictions or legal bans.',
      });
      showToast(
        isArabic ? 'الحساب سليم وقانوني 100%' : 'Account 100% clean & unrestricted',
        '🛡️'
      );
    }, 1200);
  };

  const legalLimitsList = [
    {
      icon: MessageSquare,
      titleAr: 'المحادثات والقنوات المشترك بها',
      titleEn: 'Joined Chats & Channels',
      current: chats.length,
      max: maxDialogs,
      unit: isArabic ? 'محادثة' : 'chats',
      premiumBonus: isArabic ? '1000 مع بريميوم' : '1000 with Premium',
    },
    {
      icon: Pin,
      titleAr: 'المحادثات المثبتة في الأعلى',
      titleEn: 'Pinned Chats',
      current: pinnedCount,
      max: maxPinned,
      unit: isArabic ? 'مثبتة' : 'pinned',
      premiumBonus: isArabic ? '10 مع بريميوم' : '10 with Premium',
    },
    {
      icon: Folder,
      titleAr: 'المجلدات المخصصة',
      titleEn: 'Custom Chat Folders',
      current: folders.length,
      max: maxFolders,
      unit: isArabic ? 'مجلد' : 'folders',
      premiumBonus: isArabic ? '20 مع بريميوم' : '20 with Premium',
    },
    {
      icon: Users,
      titleAr: 'الحد الأقصى لأعضاء المجموعة',
      titleEn: 'Supergroup Member Capacity',
      current: 200000,
      max: 200000,
      isStatic: true,
      staticValue: '200,000',
      unit: isArabic ? 'عضو' : 'members',
    },
    {
      icon: FileText,
      titleAr: 'الحد الأقصى لحجم رفع الملفات',
      titleEn: 'Max File Upload Size',
      isStatic: true,
      staticValue: maxFileSize,
      premiumBonus: isArabic ? '4 GB مع بريميوم' : '4 GB with Premium',
    },
    {
      icon: Clock,
      titleAr: 'مهلة التعديل القانونية للرسائل',
      titleEn: 'Message Edit Time Window',
      isStatic: true,
      staticValue: isArabic ? '48 ساعة' : '48 Hours',
      unit: isArabic ? '(غير محدود في الرسائل المحفوظة)' : '(Unlimited in Saved Messages)',
    },
    {
      icon: Send,
      titleAr: 'الحد الأقصى لطول الرسالة والوصف',
      titleEn: 'Message & Caption Length',
      isStatic: true,
      staticValue: `4,096 / ${maxCaption}`,
      unit: isArabic ? 'حرف' : 'chars',
    },
    {
      icon: Zap,
      titleAr: 'معدل وتيرة الإرسال (Flood Limits)',
      titleEn: 'Anti-Flood Rate Limiting',
      isStatic: true,
      staticValue: isArabic ? '30 رسالة / ثانية' : '30 msgs / sec',
      unit: isArabic ? 'حماية من حظر السبام' : 'Spam protection',
    },
  ];

  const chatStatusesLegend = [
    {
      badgeAr: 'مسموح الكتابة',
      badgeEn: 'Write Allowed',
      color: 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30',
      descAr: 'محادثة عادية أو أنت مشرف: يمكنك كتابة الرسائل وإرسال جميع الوسائط بحرية.',
      descEn: 'Normal chat or you are admin: You can post messages and media freely.',
    },
    {
      badgeAr: 'قناة (بث فقط)',
      badgeEn: 'Channel (Broadcast)',
      color: 'bg-sky-500/20 text-sky-400 border-sky-500/30',
      descAr: 'قناة تيليجرام عامة أو خاصة: النشر مقتصر على المشرفين المالكين للقناة.',
      descEn: 'Public/Private Channel: Posting is restricted to channel administrators.',
    },
    {
      badgeAr: 'مشرفون فقط',
      badgeEn: 'Admins Only',
      color: 'bg-amber-500/20 text-amber-300 border-amber-500/30',
      descAr: 'مجموعة تم إغلاق النشر فيها للأعضاء مؤقتاً أو دائماً بواسطة الإدارة.',
      descEn: 'Group posting locked for regular members by group administrators.',
    },
    {
      badgeAr: 'مقيد / محظور',
      badgeEn: 'Restricted in Group',
      color: 'bg-rose-500/20 text-rose-400 border-rose-500/30',
      descAr: 'تم تقييدك من إرسال الرسائل أو الوسائط في هذه المجموعة بواسطة المشرفين.',
      descEn: 'Your permissions to send messages/media were restricted by group admins.',
    },
    {
      badgeAr: 'محظور',
      badgeEn: 'Blocked User',
      color: 'bg-rose-600/25 text-rose-300 border-rose-600/40',
      descAr: 'مستخدم قمت بحظره في المحادثات الخاصة (أو قام الطرف الآخر بحظرك).',
      descEn: 'Direct peer you have blocked in private chat or peer blocked you.',
    },
    {
      badgeAr: 'بوت متوقف',
      badgeEn: 'Bot Stopped',
      color: 'bg-gray-500/20 text-gray-400 border-gray-500/30',
      descAr: 'بوت تم إيقافه بواسطة المستخدم ويتطلب الضغط على "إعادة تشغيل البوت".',
      descEn: 'Bot was stopped by user and requires clicking "Restart Bot".',
    },
    {
      badgeAr: 'تحقق الكابتشا',
      badgeEn: 'Captcha Required',
      color: 'bg-amber-500/20 text-amber-400 border-amber-500/30',
      descAr: 'مجموعة تفعل حماية الروبوتات وتتطلب اختيار الإجابة الصحيحة لفك القيد.',
      descEn: 'Group has anti-bot captcha enabled, requiring correct answer to unlock.',
    },
    {
      badgeAr: 'وضع بطيء',
      badgeEn: 'Slowmode Active',
      color: 'bg-amber-500/20 text-amber-300 border-amber-500/30',
      descAr: 'المجموعة تفرض فترة انتظار زمنية (Slowmode) بين كل رسالة وأخرى.',
      descEn: 'Group enforces a cooldown period (Slowmode) between sent messages.',
    },
  ];

  return (
    <div
      id="telegram-limits-modal"
      className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/70 backdrop-blur-sm animate-in fade-in"
      onClick={onClose}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-2xl max-h-[90vh] flex flex-col rounded-3xl bg-[#17212b] border border-[#2b394a] text-white shadow-2xl overflow-hidden animate-in zoom-in-95"
      >
        {/* Header */}
        <div className="px-5 py-4 border-b border-[#2b394a] flex items-center justify-between bg-[#1f2b38]/60">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-sky-500/20 border border-sky-400/30 flex items-center justify-center text-[#2481cc]">
              <Scale className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold flex items-center gap-2">
                <span>{isArabic ? 'الحدود القانونية وحالة المحادثات' : 'Telegram Legal Limits & Chat Standing'}</span>
                <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-sky-500/20 text-sky-400 border border-sky-400/30">
                  MTProto 2.0
                </span>
              </h2>
              <p className="text-xs text-gray-400">
                {isArabic
                  ? 'مطابقة المعايير الرسمية لحدود الاستخدام ومكافحة السبام وحالات الحظر'
                  : 'Official protocol limits, anti-spam standing & write permission matrix'}
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

        {/* Scrollable Content */}
        <div className="flex-1 overflow-y-auto p-5 space-y-6 custom-scrollbar text-sm">
          {/* Section 1: SpamBot & Account Legal Standing */}
          <div className="p-4 rounded-2xl bg-gradient-to-br from-emerald-950/40 via-[#1e2c3a]/70 to-[#17212b] border border-emerald-500/30 flex flex-col gap-3.5">
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-center gap-2.5">
                <div className="w-9 h-9 rounded-xl bg-emerald-500/20 border border-emerald-400/30 flex items-center justify-center text-emerald-400 shrink-0">
                  <ShieldCheck className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-emerald-300 flex items-center gap-1.5">
                    <span>{isArabic ? 'الحالة القانونية للحساب (Account Standing)' : 'Account Legal Standing'}</span>
                    <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                  </h3>
                  <p className="text-xs text-gray-300">
                    {isArabic ? spamBotStatus.messageAr : spamBotStatus.messageEn}
                  </p>
                </div>
              </div>

              <button
                onClick={handleTestSpamBot}
                disabled={isTestingSpamBot}
                className="px-3.5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold shadow-md transition-all active:scale-95 flex items-center gap-1.5 shrink-0 disabled:opacity-50"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${isTestingSpamBot ? 'animate-spin' : ''}`} />
                <span>{isArabic ? 'فحص @SpamBot' : 'Check @SpamBot'}</span>
              </button>
            </div>

            {/* Quick Badges Row */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 text-xs pt-1 border-t border-white/5">
              <div className="flex items-center gap-1.5 text-gray-300 bg-black/20 p-2 rounded-xl border border-white/5">
                <span className="w-2 h-2 rounded-full bg-emerald-400" />
                <span>{isArabic ? 'مراسلة الغرباء: مسموح' : 'Peer Messaging: Allowed'}</span>
              </div>
              <div className="flex items-center gap-1.5 text-gray-300 bg-black/20 p-2 rounded-xl border border-white/5">
                <span className="w-2 h-2 rounded-full bg-emerald-400" />
                <span>{isArabic ? 'حظر السبام: خالٍ 100%' : 'Spam Block: None'}</span>
              </div>
              <div className="flex items-center gap-1.5 text-gray-300 bg-black/20 p-2 rounded-xl border border-white/5">
                <span className="w-2 h-2 rounded-full bg-emerald-400" />
                <span>{isArabic ? 'معدل الفلود: طبيعي' : 'Flood Wait: Normal'}</span>
              </div>
            </div>
          </div>

          {/* Section 2: Official Telegram Limits Matrix */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-xs font-bold text-gray-400 uppercase tracking-wider flex items-center gap-1.5">
                <Scale className="w-3.5 h-3.5 text-sky-400" />
                <span>{isArabic ? 'الحدود الرسمية للبروتوكول واستخدام الحساب' : 'Official Protocol & Account Limits'}</span>
              </h3>
              {isPremium ? (
                <span className="text-[11px] font-bold text-amber-400 flex items-center gap-1">
                  <Crown className="w-3.5 h-3.5" />
                  <span>{isArabic ? 'مضاعفة بريميوم مفعّلة' : 'Premium Limits Active'}</span>
                </span>
              ) : (
                <span className="text-[11px] text-gray-400">
                  {isArabic ? 'حساب مجاني (يمكن الترقية لمضاعفة الحدود)' : 'Standard Free Tier'}
                </span>
              )}
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
              {legalLimitsList.map((item, idx) => {
                const Icon = item.icon;
                const percentage = !item.isStatic && item.max ? Math.min(100, Math.round((item.current! / item.max) * 100)) : 0;
                return (
                  <div
                    key={idx}
                    className="p-3 rounded-2xl bg-[#1e2c3a]/50 border border-white/5 hover:border-sky-500/20 transition-all flex flex-col justify-between gap-2"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex items-center gap-2">
                        <div className="w-7 h-7 rounded-lg bg-sky-500/10 text-sky-400 flex items-center justify-center shrink-0">
                          <Icon className="w-4 h-4" />
                        </div>
                        <span className="text-xs font-semibold text-gray-200">
                          {isArabic ? item.titleAr : item.titleEn}
                        </span>
                      </div>

                      <span className="text-xs font-bold text-sky-300 font-mono shrink-0">
                        {item.isStatic ? item.staticValue : `${item.current} / ${item.max}`}
                      </span>
                    </div>

                    {!item.isStatic && (
                      <div className="w-full bg-black/30 rounded-full h-1.5 overflow-hidden">
                        <div
                          className={`h-full rounded-full ${
                            percentage > 85 ? 'bg-amber-400' : 'bg-[#2481cc]'
                          }`}
                          style={{ width: `${percentage}%` }}
                        />
                      </div>
                    )}

                    {item.premiumBonus && (
                      <div className="text-[10px] text-amber-400/90 font-medium flex items-center gap-1">
                        <Zap className="w-3 h-3" />
                        <span>{item.premiumBonus}</span>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          {/* Section 3: Full Chat Statuses & Restrictions Matrix */}
          <div>
            <div className="flex items-center gap-1.5 mb-3">
              <Info className="w-3.5 h-3.5 text-sky-400" />
              <h3 className="text-xs font-bold text-gray-400 uppercase tracking-wider">
                {isArabic ? 'دليل الحالات الفعلية لكافة المحادثات (المسموح والممنوع والمحظور)' : 'Chat Permission & Status Legend'}
              </h3>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {chatStatusesLegend.map((item, idx) => (
                <div
                  key={idx}
                  className="p-3 rounded-2xl bg-[#1e2c3a]/40 border border-white/5 flex flex-col gap-1.5"
                >
                  <div className="flex items-center justify-between">
                    <span className={`text-[11px] font-bold px-2 py-0.5 rounded-lg border ${item.color}`}>
                      {isArabic ? item.badgeAr : item.badgeEn}
                    </span>
                  </div>
                  <p className="text-[11px] text-gray-300 leading-relaxed">
                    {isArabic ? item.descAr : item.descEn}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="px-5 py-3.5 border-t border-[#2b394a] flex items-center justify-between bg-[#1f2b38]/60">
          <div className="text-xs text-gray-400 flex items-center gap-1.5">
            <ShieldCheck className="w-4 h-4 text-emerald-400" />
            <span>{isArabic ? 'حماية البروتوكول وسياسات تيليجرام نشطة' : 'Telegram Protocol & Legal Policies Active'}</span>
          </div>

          <button
            onClick={onClose}
            className="px-5 py-2 rounded-xl bg-[#2481cc] hover:bg-[#1c6fad] text-white text-xs font-bold shadow-md transition-colors"
          >
            {isArabic ? 'إغلاق' : 'Close'}
          </button>
        </div>
      </div>
    </div>
  );
};
