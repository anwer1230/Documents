import React, { useState, useEffect, useMemo } from 'react';
import {
  Radio,
  Link as LinkIcon,
  RotateCw,
  Trash2,
  Calendar,
  Globe,
  User,
  Users,
  Hash,
  ArrowRight,
  ExternalLink,
  CheckCircle2,
  AlertTriangle,
  Clock,
  LogIn,
  Shield,
  Lock,
  Activity,
  ListOrdered,
  Sparkles,
  ShieldAlert,
  HelpCircle,
} from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { useTelegram } from '../../context/TelegramContext';
import { useLinkJoinQueueStore, JoinAttemptLog } from '../../stores/useLinkJoinQueueStore';
import { CapturedLink } from '../../types';

interface LinkMonitorModalProps {
  isOpen: boolean;
  onClose: () => void;
}

type ActiveTab = 'links' | 'logs';

export const LinkMonitorModal: React.FC<LinkMonitorModalProps> = ({ isOpen, onClose }) => {
  const {
    capturedLinks,
    autoJoinLinksEnabled,
    toggleAutoJoinLinks,
    clearCapturedLinks,
    showToast,
    joinChatByInviteLink,
    radarHourlyCount,
    radarLastJoinTime,
    radarQueueCount,
    radarRandomJitterMs,
  } = useTelegram();

  const queueStore = useLinkJoinQueueStore();
  const logs = queueStore.logs;

  const [activeTab, setActiveTab] = useState<ActiveTab>('links');
  const [logFilter, setLogFilter] = useState<'all' | 'success' | 'skipped_duplicate' | 'skipped_private' | 'rate_limited' | 'failed'>('all');
  const [isEnabled, setIsEnabled] = useState<boolean>(autoJoinLinksEnabled);
  const [links, setLinks] = useState<CapturedLink[]>(capturedLinks);
  const [lastUpdate, setLastUpdate] = useState<string>('');
  const [joiningUrls, setJoiningUrls] = useState<Record<string, boolean>>({});
  const [cooldownRemaining, setCooldownRemaining] = useState<number>(0);

  useEffect(() => {
    setIsEnabled(autoJoinLinksEnabled);
    setLinks(capturedLinks);
    setLastUpdate(new Date().toLocaleTimeString('ar-SA'));
  }, [autoJoinLinksEnabled, capturedLinks, isOpen]);

  // Live Cooldown & Random Delay Jitter Countdown
  useEffect(() => {
    const updateCooldown = () => {
      if (!radarLastJoinTime) {
        setCooldownRemaining(0);
        return;
      }
      const elapsed = Date.now() - radarLastJoinTime;
      const activeJitter = radarRandomJitterMs || queueStore.currentJitterMs || 8000;
      const totalCooldownMs = 60000 + activeJitter;
      const remaining = Math.max(0, Math.ceil((totalCooldownMs - elapsed) / 1000));
      setCooldownRemaining(remaining);
    };

    updateCooldown();
    const timer = setInterval(updateCooldown, 1000);
    return () => clearInterval(timer);
  }, [radarLastJoinTime, radarRandomJitterMs, queueStore.currentJitterMs]);

  // Toggle monitor
  const handleToggle = async () => {
    const nextState = !isEnabled;
    setIsEnabled(nextState);
    toggleAutoJoinLinks();
    if (nextState) {
      showToast('⚡ تم تفعيل رادار المراقبة والانضمام الفوري', '✨');
    } else {
      showToast('⏹ تم إيقاف رادار المراقبة', 'info');
    }
  };

  // Refresh
  const handleRefresh = () => {
    setLastUpdate(new Date().toLocaleTimeString('ar-SA'));
    setLinks([...capturedLinks]);
    showToast('🔄 تم تحديث قائمة الرادار', 'info');
  };

  // Clear All Links
  const handleClearAll = () => {
    if (window.confirm('هل أنت متأكد من مسح جميع الروابط المسجلة؟')) {
      clearCapturedLinks();
      setLinks([]);
      showToast('🧹 تم مسح قائمة الروابط بنجاح', 'info');
    }
  };

  // Clear Logs
  const handleClearLogs = () => {
    if (window.confirm('هل أنت متأكد من مسح سجل محاولات الانضمام والمراقبة؟')) {
      queueStore.clearLogs();
      showToast('🧹 تم مسح سجل المحاولات', 'info');
    }
  };

  // Join Link Now manually
  const handleJoinNow = async (url: string) => {
    setJoiningUrls((prev) => ({ ...prev, [url]: true }));
    try {
      const res = await joinChatByInviteLink(url);
      if (res.success) {
        showToast(res.message || '✅ تم الانضمام وتم إرسال الإشعار للرسائل المحفوظة!', '✨');
        setLinks((prev) =>
          prev.map((l) =>
            l.url === url
              ? { ...l, status: 'joined', status_text: '✅ تم الانضمام بنجاح', joined: true }
              : l
          )
        );
      } else {
        showToast(res.message || '❌ تعذر الانضمام للمجموعة', '⚠️');
      }
    } catch (e) {
      showToast('❌ خطأ في الاتصال بالخادم', '⚠️');
    } finally {
      setJoiningUrls((prev) => ({ ...prev, [url]: false }));
    }
  };

  // Delete single link
  const handleDeleteLink = (url: string) => {
    setLinks((prev) => prev.filter((l) => l.url !== url));
    showToast('🗑️ تم حذف الرابط', 'info');
  };

  // Compute stats
  const statTotal = links.length;
  const statJoined = links.filter((l) => l.status === 'joined' || l.joined || l.status === 'already_member').length;
  const statSkippedPrivate = links.filter((l) => l.status === 'skipped_private_channel').length;
  const statSkippedDuplicate = links.filter((l) => l.status === 'skipped_duplicate').length;

  const filteredLogs = useMemo(() => {
    if (logFilter === 'all') return logs;
    return logs.filter((log) => log.status === logFilter);
  }, [logs, logFilter]);

  if (!isOpen) return null;

  return (
    <AnimatePresence>
      <div
        id="modal-link-monitor-view"
        className="fixed inset-0 z-[9999] flex items-center justify-center p-2 sm:p-4 bg-black/85 backdrop-blur-md select-none overflow-y-auto"
        dir="rtl"
      >
        {/* Backdrop */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.2 }}
          onClick={onClose}
          className="fixed inset-0 cursor-pointer"
        />

        {/* Modal Content */}
        <motion.div
          initial={{ opacity: 0, scale: 0.94, y: 15 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.94, y: 15 }}
          transition={{ type: 'spring', damping: 28, stiffness: 320 }}
          className="relative z-10 w-full max-w-4xl text-[#e8eaf6] rounded-2xl shadow-2xl overflow-hidden border border-white/10 my-auto flex flex-col max-h-[92vh]"
          style={{
            background: '#0b0f19',
            fontFamily: "'Cairo', sans-serif",
          }}
        >
          {/* Top Bar Navigation */}
          <div className="px-4 py-3 border-b border-white/10 flex items-center justify-between flex-wrap gap-2 bg-white/[0.02]">
            <div className="flex items-center gap-3">
              <button
                onClick={onClose}
                className="bg-white/[0.05] hover:bg-white/[0.1] border border-white/10 rounded-lg px-3 py-1.5 text-[#e8eaf6] text-[0.8rem] flex items-center gap-1.5 transition-all"
              >
                <ArrowRight className="w-3.5 h-3.5" />
                <span>إغلاق</span>
              </button>
              <h4 className="text-[1.05rem] font-bold text-emerald-400 m-0 flex items-center gap-2">
                <Radio className="w-4 h-4 text-emerald-400 animate-pulse" />
                <span>رادار المراقبة والانضمام الفوري</span>
              </h4>
            </div>

            {/* Top Right Status Badge & Mode */}
            <div className="flex items-center gap-2">
              <span
                id="statusBadge"
                className={`badge px-3 py-1 rounded-full text-[0.7rem] font-bold flex items-center gap-1.5 ${
                  isEnabled
                    ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                    : 'bg-gray-500/20 text-gray-400 border border-gray-500/30'
                }`}
              >
                <span className={`w-2 h-2 rounded-full ${isEnabled ? 'bg-emerald-400 animate-ping' : 'bg-gray-500'}`} />
                {isEnabled ? 'رادار يعمل دائماً ومستقلاً' : 'متوقف'}
              </span>
            </div>
          </div>

          {/* Tab Navigation: Links vs. Attempt Logs */}
          <div className="px-4 pt-2 bg-white/[0.01] border-b border-white/5 flex items-center gap-2">
            <button
              onClick={() => setActiveTab('links')}
              className={`px-4 py-2 text-[0.8rem] font-bold border-b-2 transition-all flex items-center gap-2 ${
                activeTab === 'links'
                  ? 'border-emerald-400 text-emerald-400 bg-white/[0.03]'
                  : 'border-transparent text-gray-400 hover:text-gray-200'
              }`}
            >
              <ListOrdered className="w-3.5 h-3.5" />
              <span>الروابط المرصودة</span>
              <span className="text-[0.65rem] px-1.5 py-0.2 rounded-full bg-white/10 font-mono">
                {statTotal}
              </span>
            </button>

            <button
              onClick={() => setActiveTab('logs')}
              className={`px-4 py-2 text-[0.8rem] font-bold border-b-2 transition-all flex items-center gap-2 ${
                activeTab === 'logs'
                  ? 'border-cyan-400 text-cyan-400 bg-white/[0.03]'
                  : 'border-transparent text-gray-400 hover:text-gray-200'
              }`}
            >
              <Activity className="w-3.5 h-3.5" />
              <span>سجل المحاولات والمراقبة المباشر</span>
              {logs.length > 0 && (
                <span className="text-[0.65rem] px-1.5 py-0.2 rounded-full bg-cyan-500/20 text-cyan-300 font-mono">
                  {logs.length}
                </span>
              )}
            </button>
          </div>

          {/* Main Content Area */}
          <div className="p-3 sm:p-4 overflow-y-auto flex-1 space-y-3">
            {/* Rules and Anti-Ban Guidelines Card */}
            <div className="bg-emerald-950/20 border border-emerald-500/30 rounded-xl p-3 text-[0.78rem] text-emerald-200/90 flex flex-col gap-1.5">
              <div className="flex items-center gap-2 font-bold text-emerald-300">
                <Shield className="w-4 h-4 text-emerald-400" />
                <span>ضوابط وقواعد الرادار الصارمة ومنظومة الحماية من الحظر:</span>
              </div>
              <ul className="list-disc list-inside space-y-1 text-[0.73rem] text-gray-300 mr-2">
                <li><strong className="text-white">مراقبة منفصلة ودائمة:</strong> فحص أي رابط ينزل بأي محادثة بالخلفية بدون توقف وبمؤقت دقيقة منتظم.</li>
                <li><strong className="text-white">انضمام فوري للمجموعات العامة:</strong> ينضم تلقائياً ويوثق الرابط ويرسل إشعاراً كاملاً إلى الرسائل المحفوظة.</li>
                <li><strong className="text-white">تخطي القنوات الخاصة:</strong> روابط القنوات الخاصة تُترك ويتخطاها الرادار تلقائياً ولا ينضم إليها.</li>
                <li><strong className="text-white">منع التكرار التلقائي:</strong> منع محاولات الانضمام المتكررة لنفس الرابط خلال فترة زمنية قصيرة لحماية الحساب.</li>
                <li><strong className="text-white">تأخير عشوائي ذكي (Jitter):</strong> إضافة تأخير عشوائي (+5 إلى +25 ثانية) فوق الدقيقة الأساسية لمنع كشف الروبوتات من تيليجرام.</li>
                <li><strong className="text-white">حد أقصى 20 انضمام/ساعة:</strong> معدل أمان صارم لا يتجاوز 20 انضماماً في الساعة مع الاحتفاظ بدفعات الروابط في الطابور ومعالجتها تباعاً.</li>
              </ul>
            </div>

            {/* TAB 1: CAPTURED LINKS */}
            {activeTab === 'links' && (
              <div className="bg-white/[0.03] border border-white/[0.08] rounded-xl overflow-hidden backdrop-blur-md">
                {/* Card Header with Main Toggle */}
                <div className="px-4 py-3 bg-white/[0.04] border-b border-white/[0.06] flex items-center justify-between flex-wrap gap-2">
                  <h6 className="text-[0.9rem] font-bold text-white flex items-center gap-2 m-0">
                    <LinkIcon className="w-4 h-4 text-emerald-400" />
                    <span>تشغيل المراقبة الآلية المباشرة</span>
                  </h6>
                  <div className="flex items-center gap-2">
                    <span className="text-[0.75rem] text-gray-400" id="toggleLabel">
                      {isEnabled ? 'مفعّل دائم' : 'متوقف'}
                    </span>
                    <label className="relative inline-flex items-center cursor-pointer">
                      <input
                        type="checkbox"
                        id="linkMonitorToggle"
                        checked={isEnabled}
                        onChange={handleToggle}
                        className="sr-only peer"
                      />
                      <div className="w-12 h-6 bg-gray-700 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full rtl:peer-checked:after:-translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:start-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-emerald-500"></div>
                    </label>
                  </div>
                </div>

                {/* Card Body */}
                <div className="p-3 sm:p-4 space-y-3">
                  {/* 5 Stat Boxes & Rate Limits */}
                  <div className="grid grid-cols-2 sm:grid-cols-5 gap-2" id="statsRow">
                    <div className="bg-white/[0.04] rounded-xl p-2.5 text-center border border-white/5">
                      <div className="text-[1.2rem] font-bold text-cyan-400" id="statTotal">{statTotal}</div>
                      <div className="text-[0.68rem] text-gray-400">إجمالي الروابط المرصودة</div>
                    </div>
                    <div className="bg-white/[0.04] rounded-xl p-2.5 text-center border border-white/5">
                      <div className="text-[1.2rem] font-bold text-emerald-400" id="statJoined">{statJoined}</div>
                      <div className="text-[0.68rem] text-emerald-400">✅ منضم (مجموعات عامة)</div>
                    </div>
                    <div className="bg-white/[0.04] rounded-xl p-2.5 text-center border border-white/5">
                      <div className="text-[1.2rem] font-bold text-purple-400" id="statSkipped">{statSkippedPrivate}</div>
                      <div className="text-[0.68rem] text-purple-300">🔒 قنوات خاصة متخطاة</div>
                    </div>
                    <div className="bg-white/[0.04] rounded-xl p-2.5 text-center border border-white/5">
                      <div className="text-[1.2rem] font-bold text-amber-400" id="statDup">{statSkippedDuplicate}</div>
                      <div className="text-[0.68rem] text-amber-300">🛡️ منع تكرار (متخطي)</div>
                    </div>
                    <div className="bg-white/[0.04] rounded-xl p-2.5 text-center border border-white/5 col-span-2 sm:col-span-1">
                      <div className="text-[1.2rem] font-bold text-rose-400">
                        {radarHourlyCount ?? 0} <span className="text-xs text-gray-400 font-normal">/ 20</span>
                      </div>
                      <div className="text-[0.68rem] text-rose-300">حد الساعة (20/ساعة)</div>
                    </div>
                  </div>

                  {/* Cooldown & Rate Limit Status Bar */}
                  <div className="bg-black/40 border border-white/5 rounded-lg p-2.5 flex items-center justify-between text-[0.75rem] flex-wrap gap-2">
                    <div className="flex items-center gap-2">
                      <Clock className={`w-4 h-4 ${cooldownRemaining > 0 ? 'text-amber-400 animate-spin' : 'text-emerald-400'}`} />
                      <span>
                        فاصل الأمان والتأخير العشوائي:
                        {cooldownRemaining > 0 ? (
                          <strong className="text-amber-300 mr-1.5 font-mono">
                            انتظار {cooldownRemaining} ثانية (فاصل دقيقة + تأخير عشوائي لمنع الحظر)
                          </strong>
                        ) : (
                          <strong className="text-emerald-400 mr-1.5">
                            جاهز للانضمام الفوري فور استلام رابط عام
                          </strong>
                        )}
                      </span>
                    </div>

                    <div className="flex items-center gap-2">
                      {radarQueueCount > 0 && (
                        <span className="bg-cyan-500/20 text-cyan-300 border border-cyan-500/30 px-2 py-0.5 rounded-md text-[0.7rem] font-bold animate-pulse">
                          ⚡ طابور الدفعة: {radarQueueCount} رابط بانتظار دوره
                        </span>
                      )}
                      <span className="text-gray-400 font-mono text-[0.7rem]">
                        أقصى 20 انضمام/ساعة • فحص كل دقيقة
                      </span>
                    </div>
                  </div>

                  {/* Control Action Buttons */}
                  <div className="flex items-center gap-2 flex-wrap pt-1">
                    <button
                      onClick={handleRefresh}
                      className="bg-white/[0.1] hover:bg-white/[0.18] border border-white/15 text-[#e8eaf6] text-[0.78rem] px-3 py-1.5 rounded-lg flex items-center gap-1.5 transition-all"
                    >
                      <RotateCw className="w-3.5 h-3.5" />
                      <span>تحديث القائمة</span>
                    </button>
                    <button
                      onClick={handleClearAll}
                      className="bg-rose-500/10 hover:bg-rose-500/20 border border-rose-500/30 text-rose-400 text-[0.78rem] px-3 py-1.5 rounded-lg flex items-center gap-1.5 transition-all"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                      <span>مسح الروابط</span>
                    </button>
                    {lastUpdate && (
                      <span className="text-[0.68rem] text-gray-400 mr-auto" id="lastUpdate">
                        آخر فحص: {lastUpdate}
                      </span>
                    )}
                  </div>

                  {/* Links List */}
                  <div
                    id="linksList"
                    className="space-y-2 max-h-[420px] overflow-y-auto pr-1"
                  >
                    {links.length === 0 ? (
                      <div className="text-center py-10 text-gray-500">
                        <LinkIcon className="w-10 h-10 mx-auto mb-2 opacity-25 text-emerald-400" />
                        <div className="font-bold text-[0.85rem] text-gray-300">الرادار يرصد المحادثات الآن...</div>
                        <small className="text-[0.72rem] text-gray-500">
                          أي رابط ينزل بأي محادثة سيتم التقاطه هنا فورياً وفحصه والتتالي بالانضمام له
                        </small>
                      </div>
                    ) : (
                      links.map((link) => {
                        const isJoined = link.status === 'joined' || link.joined;
                        const isAlreadyMember = link.status === 'already_member';
                        const isJoining = link.status === 'joining' || Boolean(joiningUrls[link.url]);
                        const isSkippedPrivate = link.status === 'skipped_private_channel';
                        const isSkippedDuplicate = link.status === 'skipped_duplicate';
                        const isPendingApproval = link.failReason?.includes('موافقة المشرف');
                        const isFailed = link.status === 'failed';
                        const isPending = link.status === 'pending';

                        let badgeClass = 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30';
                        let badgeText = '✅ مجموعة عامة';
                        let borderRightColor = 'border-r-cyan-500';

                        if (isJoined) {
                          badgeClass = 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30';
                          badgeText = '✅ تم الانضمام بنجاح';
                          borderRightColor = 'border-r-emerald-500';
                        } else if (isAlreadyMember) {
                          badgeClass = 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30';
                          badgeText = '✅ أنت عضو بالفعل';
                          borderRightColor = 'border-r-emerald-400';
                        } else if (isJoining) {
                          badgeClass = 'bg-cyan-500/20 text-cyan-300 border-cyan-500/30';
                          badgeText = '🔄 جارِ الانضمام...';
                          borderRightColor = 'border-r-cyan-400';
                        } else if (isSkippedDuplicate) {
                          badgeClass = 'bg-amber-500/20 text-amber-300 border-amber-500/30';
                          badgeText = '🛡️ منع تكرار المحاولة';
                          borderRightColor = 'border-r-amber-500';
                        } else if (isPendingApproval) {
                          badgeClass = 'bg-blue-500/20 text-blue-300 border-blue-500/30';
                          badgeText = '⏳ بانتظار موافقة المشرف';
                          borderRightColor = 'border-r-blue-400';
                        } else if (isSkippedPrivate) {
                          badgeClass = 'bg-purple-500/20 text-purple-300 border-purple-500/30';
                          badgeText = '🔒 قناة خاصة (تم التخطي)';
                          borderRightColor = 'border-r-purple-500';
                        } else if (isFailed) {
                          badgeClass = 'bg-rose-500/20 text-rose-300 border-rose-500/30';
                          badgeText = `❌ فشل (${link.failReason || 'خطأ'})`;
                          borderRightColor = 'border-r-rose-500';
                        } else if (isPending) {
                          badgeClass = 'bg-amber-500/20 text-amber-300 border-amber-500/30';
                          badgeText = '⏳ بانتظار دور الانضمام (طابور الدفعة)';
                          borderRightColor = 'border-r-amber-400';
                        }

                        return (
                          <div
                            key={link.id || link.url}
                            className={`bg-white/[0.04] hover:bg-white/[0.08] rounded-lg p-2.5 sm:p-3 border-r-4 ${borderRightColor} border-t border-b border-l border-white/5 text-[0.78rem] transition-all flex justify-between items-start gap-2 flex-wrap`}
                          >
                            <div className="flex-1 min-w-0">
                              <div className="url flex items-center gap-2">
                                <a
                                  href={link.url}
                                  target="_blank"
                                  rel="noreferrer"
                                  className="text-cyan-400 hover:text-cyan-300 break-all font-mono"
                                >
                                  {link.url}
                                </a>
                              </div>

                              <div className="flex gap-2 flex-wrap mt-1.5 items-center">
                                <span className={`badge px-2 py-0.5 rounded-full text-[0.62rem] border ${badgeClass}`}>
                                  {badgeText}
                                </span>
                                <span className="text-[0.65rem] text-gray-400 flex items-center gap-1">
                                  <Hash className="w-3 h-3 text-gray-500" />
                                  <span>{link.source_chat || link.sourceChatTitle || 'محادثة عامة'}</span>
                                </span>
                                <span className="text-[0.65rem] text-gray-400 flex items-center gap-1">
                                  <User className="w-3 h-3 text-gray-500" />
                                  <span>{link.sender || link.sourceSenderName || 'عضو تيليجرام'}</span>
                                </span>
                                {link.chat_title && (
                                  <span className="text-[0.65rem] text-gray-400 flex items-center gap-1">
                                    <Users className="w-3 h-3 text-gray-500" />
                                    <span>{link.chat_title}</span>
                                  </span>
                                )}
                              </div>

                              <div className="flex gap-3 flex-wrap mt-1.5 text-[0.62rem] text-gray-500">
                                <span className="flex items-center gap-1">
                                  <Calendar className="w-3 h-3" />
                                  <span>التاريخ: {link.creation_date || 'اليوم'}</span>
                                </span>
                                <span className="flex items-center gap-1">
                                  <Globe className="w-3 h-3" />
                                  <span>الدولة: {link.country || '🇸🇦 السعودية'}</span>
                                </span>
                              </div>

                              {link.status_text && (
                                <div className="text-[0.67rem] mt-1.5 font-medium text-amber-200/90 bg-amber-950/20 px-2 py-1 rounded border border-amber-500/20">
                                  {link.status_text}
                                </div>
                              )}
                            </div>

                            <div className="flex flex-col items-end gap-1.5">
                              <div className="text-[0.58rem] text-gray-500 whitespace-nowrap">
                                {link.detected_at
                                  ? new Date(link.detected_at).toLocaleTimeString('ar-SA')
                                  : 'منذ قليل'}
                              </div>
                              <div className="flex items-center gap-1">
                                {!isJoined && !isSkippedPrivate && !isSkippedDuplicate && (
                                  <button
                                    type="button"
                                    disabled={joiningUrls[link.url]}
                                    onClick={() => handleJoinNow(link.url)}
                                    className="py-1 px-2 rounded bg-emerald-600/30 hover:bg-emerald-600/50 border border-emerald-500/40 text-emerald-300 text-[0.65rem] flex items-center gap-1 transition-all"
                                    title="انضمام فوري"
                                  >
                                    {joiningUrls[link.url] ? (
                                      <RotateCw className="w-3 h-3 animate-spin" />
                                    ) : (
                                      <LogIn className="w-3 h-3" />
                                    )}
                                    <span>انضمام</span>
                                  </button>
                                )}
                                <button
                                  type="button"
                                  onClick={() => handleDeleteLink(link.url)}
                                  className="p-1 rounded bg-rose-500/20 hover:bg-rose-500/40 border border-rose-500/40 text-rose-300 text-[0.6rem] transition-all"
                                  title="حذف"
                                >
                                  <Trash2 className="w-3 h-3" />
                                </button>
                              </div>
                            </div>
                          </div>
                        );
                      })
                    )}
                  </div>
                </div>
              </div>
            )}

            {/* TAB 2: LIVE ATTEMPT LOGS */}
            {activeTab === 'logs' && (
              <div className="bg-white/[0.03] border border-white/[0.08] rounded-xl overflow-hidden backdrop-blur-md p-3 sm:p-4 space-y-3">
                <div className="flex items-center justify-between flex-wrap gap-2 pb-2 border-b border-white/10">
                  <div className="flex items-center gap-2">
                    <Activity className="w-4 h-4 text-cyan-400" />
                    <h6 className="text-[0.88rem] font-bold text-white m-0">
                      سجل مراقبة ومحاولات الانضمام التلقائية المباشرة
                    </h6>
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={handleClearLogs}
                      className="bg-rose-500/10 hover:bg-rose-500/20 border border-rose-500/30 text-rose-400 text-[0.72rem] px-2.5 py-1 rounded-lg flex items-center gap-1.5 transition-all"
                    >
                      <Trash2 className="w-3 h-3" />
                      <span>مسح السجل</span>
                    </button>
                  </div>
                </div>

                {/* Filter chips */}
                <div className="flex items-center gap-1.5 flex-wrap text-[0.7rem]">
                  <span className="text-gray-400 text-xs">تصفية:</span>
                  {[
                    { id: 'all', label: 'الكل' },
                    { id: 'success', label: '✅ نجاح الانضمام' },
                    { id: 'skipped_duplicate', label: '🛡️ منع التكرار' },
                    { id: 'skipped_private', label: '🔒 قناة خاصة' },
                    { id: 'rate_limited', label: '⚠️ حد الساعة (20)' },
                    { id: 'failed', label: '❌ فشل' },
                  ].map((f) => (
                    <button
                      key={f.id}
                      onClick={() => setLogFilter(f.id as any)}
                      className={`px-2 py-0.5 rounded-md border transition-all ${
                        logFilter === f.id
                          ? 'bg-cyan-500/20 text-cyan-300 border-cyan-500/40 font-bold'
                          : 'bg-white/5 text-gray-400 border-white/5 hover:bg-white/10'
                      }`}
                    >
                      {f.label}
                    </button>
                  ))}
                </div>

                {/* Logs Listing */}
                <div className="space-y-2 max-h-[440px] overflow-y-auto pr-1 font-sans">
                  {filteredLogs.length === 0 ? (
                    <div className="text-center py-10 text-gray-500">
                      <Clock className="w-8 h-8 mx-auto mb-2 opacity-30 text-cyan-400" />
                      <div className="font-bold text-xs text-gray-400">لا توجد سجلات بعد</div>
                      <div className="text-[0.7rem] text-gray-500">
                        سيتم تسجيل كل محاولة انضمام، وتأخيرها العشوائي، ونتيجتها هنا تلقائياً
                      </div>
                    </div>
                  ) : (
                    filteredLogs.map((log) => {
                      let statusBadge = 'bg-cyan-500/20 text-cyan-300 border-cyan-500/30';
                      if (log.status === 'success') {
                        statusBadge = 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30';
                      } else if (log.status === 'skipped_duplicate') {
                        statusBadge = 'bg-amber-500/20 text-amber-300 border-amber-500/30';
                      } else if (log.status === 'skipped_private') {
                        statusBadge = 'bg-purple-500/20 text-purple-300 border-purple-500/30';
                      } else if (log.status === 'rate_limited') {
                        statusBadge = 'bg-rose-500/20 text-rose-300 border-rose-500/30';
                      } else if (log.status === 'failed') {
                        statusBadge = 'bg-rose-500/20 text-rose-300 border-rose-500/30';
                      }

                      return (
                        <div
                          key={log.id}
                          className="bg-white/[0.03] hover:bg-white/[0.06] border border-white/5 rounded-lg p-2.5 text-[0.75rem] transition-all space-y-1.5"
                        >
                          <div className="flex items-center justify-between flex-wrap gap-2">
                            <div className="flex items-center gap-2">
                              <span className={`px-2 py-0.5 rounded text-[0.65rem] font-bold border ${statusBadge}`}>
                                {log.statusLabel}
                              </span>
                              <span className="font-mono text-cyan-400 text-[0.72rem] break-all">
                                {log.url}
                              </span>
                            </div>
                            <span className="text-[0.62rem] text-gray-400 font-mono">
                              {new Date(log.timestamp).toLocaleTimeString('ar-SA')}
                            </span>
                          </div>

                          <div className="text-[0.72rem] text-gray-300 leading-relaxed">
                            {log.message}
                          </div>

                          <div className="flex items-center gap-3 text-[0.63rem] text-gray-400 flex-wrap pt-0.5 border-t border-white/[0.04]">
                            {log.jitterAppliedMs !== undefined && (
                              <span className="text-emerald-400 font-mono flex items-center gap-1">
                                <Sparkles className="w-3 h-3 text-emerald-400" />
                                تأخير عشوائي مطبق: +{Math.ceil(log.jitterAppliedMs / 1000)} ثانية
                              </span>
                            )}
                            {log.hourlyCount !== undefined && (
                              <span className="text-amber-400 font-mono">
                                معدل الساعة: {log.hourlyCount} / 20
                              </span>
                            )}
                            {log.queueRemaining !== undefined && (
                              <span className="text-cyan-400 font-mono">
                                متبقي في الطابور: {log.queueRemaining}
                              </span>
                            )}
                            {log.sourceChat && (
                              <span className="text-gray-500">
                                المصدر: {log.sourceChat}
                              </span>
                            )}
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>
              </div>
            )}
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
};
