import React, { useState, useEffect } from 'react';
import {
  Search,
  Radio,
  ToggleLeft,
  ToggleRight,
  Download,
  Trash2,
  CheckCircle2,
  XCircle,
  Clock,
  ExternalLink,
  ShieldCheck,
  X,
  Filter,
  UserPlus,
  RefreshCw,
  Cpu,
  ShieldAlert,
  AlertTriangle,
  Lock,
  Users,
} from 'lucide-react';
import { useTelegram } from '../../context/TelegramContext';
import {
  backgroundSyncService,
  BackgroundWorkerStatus,
  TelegramSafeLimitStatus,
} from '../../core/BackgroundSyncService';
import { LiveDiscoveredLink } from '../../types';

export const LiveLinkDiscoverModal: React.FC = () => {
  const { activeModal, setActiveModal, showToast } = useTelegram();
  const [links, setLinks] = useState<LiveDiscoveredLink[]>([]);
  const [isInstantJoin, setIsInstantJoin] = useState(false);
  const [isScannerActive, setIsScannerActive] = useState(true);
  const [filterStatus, setFilterStatus] = useState<'all' | 'joined' | 'pending' | 'failed'>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [isJoiningId, setIsJoiningId] = useState<string | null>(null);
  const [workerStatus, setWorkerStatus] = useState<BackgroundWorkerStatus>(
    backgroundSyncService.getWorkerStatus()
  );
  const [safeStatus, setSafeStatus] = useState<TelegramSafeLimitStatus>(
    backgroundSyncService.getTelegramSafeStatus()
  );

  useEffect(() => {
    const unsub = backgroundSyncService.subscribe(() => {
      setLinks([...backgroundSyncService.getDiscoveredLinks()]);
      setIsInstantJoin(backgroundSyncService.isInstantJoinEnabled());
      setIsScannerActive(backgroundSyncService.isLiveDiscoverActive());
      setWorkerStatus(backgroundSyncService.getWorkerStatus());
      setSafeStatus(backgroundSyncService.getTelegramSafeStatus());
    });

    setLinks([...backgroundSyncService.getDiscoveredLinks()]);
    setIsInstantJoin(backgroundSyncService.isInstantJoinEnabled());
    setIsScannerActive(backgroundSyncService.isLiveDiscoverActive());
    setWorkerStatus(backgroundSyncService.getWorkerStatus());
    setSafeStatus(backgroundSyncService.getTelegramSafeStatus());

    const timer = setInterval(() => {
      setSafeStatus(backgroundSyncService.getTelegramSafeStatus());
    }, 1000);

    return () => {
      unsub();
      clearInterval(timer);
    };
  }, []);

  if (activeModal !== ('live-link-discover' as any) && activeModal !== ('link-monitor' as any)) {
    return null;
  }

  const handleToggleScanner = () => {
    const next = !isScannerActive;
    backgroundSyncService.toggleLiveDiscover(next);
    setIsScannerActive(next);
    showToast(next ? 'تم تفعيل رادار البحث اللحظي 🔍' : 'تم إيقاف الرادار ⏹️', '✨');
  };

  const handleToggleInstantJoin = () => {
    const next = !isInstantJoin;
    backgroundSyncService.toggleInstantAutoJoin(next);
    setIsInstantJoin(next);
    showToast(
      next
        ? 'تم تفعيل الانضمام التلقائي الفوري (للمجموعات العامة فقط ضمن الحدود الآمنة) 🚀'
        : 'تم إيقاف الانضمام التلقائي الفوري 🛑',
      '✨'
    );
  };

  const handleManualJoin = async (linkId: string) => {
    setIsJoiningId(linkId);
    const result = await backgroundSyncService.manualJoinDiscoveredLink(linkId);
    setIsJoiningId(null);
    if (result.success) {
      showToast('تم الانضمام للمجموعة العامة بنجاح 🎉', '✅');
    } else {
      showToast(result.reason || 'تعذر الانضمام (الرابط تالف أو قناة خاصة)', '⚠️');
    }
  };

  const handleExportReport = () => {
    if (links.length === 0) {
      showToast('لا توجد روابط لتصديرها', '⚠️');
      return;
    }
    const csvContent =
      'data:text/csv;charset=utf-8,' +
      'URL,Type,Source Chat,Sender,Timestamp,Status,Fail Reason\n' +
      links
        .map(
          (l) =>
            `"${l.url}","${l.linkType || 'unknown'}","${l.sourceChatTitle || ''}","${
              l.senderName || ''
            }","${l.timestamp}","${l.status}","${l.failReason || ''}"`
        )
        .join('\n');

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `telegram_links_report_${Date.now()}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    showToast('تم تصدير تقرير الروابط بنجاح 📥', '✨');
  };

  const filteredLinks = links.filter((l) => {
    if (filterStatus === 'joined' && l.status !== 'joined') return false;
    if (filterStatus === 'pending' && l.status !== 'pending' && l.status !== 'joining') return false;
    if (filterStatus === 'failed' && l.status !== 'failed' && l.status !== 'skipped') return false;
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      return (
        l.url.toLowerCase().includes(q) ||
        (l.sourceChatTitle && l.sourceChatTitle.toLowerCase().includes(q)) ||
        (l.senderName && l.senderName.toLowerCase().includes(q))
      );
    }
    return true;
  });

  const totalJoined = links.filter((l) => l.status === 'joined').length;
  const totalFailed = links.filter((l) => l.status === 'failed' || l.status === 'skipped').length;
  const totalPending = links.filter((l) => l.status === 'pending' || l.status === 'joining').length;

  const formatSeconds = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <div
      id="modal-live-link-discover-activity"
      className="fixed inset-0 z-[9999] flex items-center justify-center p-2 sm:p-4 bg-black/85 backdrop-blur-md select-none"
      dir="rtl"
    >
      <div
        className="w-full max-w-lg sm:max-w-xl text-[#e8eaf6] rounded-3xl shadow-2xl overflow-hidden border border-teal-500/30 my-auto animate-in zoom-in-95 duration-200 max-h-[92vh] flex flex-col"
        style={{
          background: 'linear-gradient(165deg, #04171a 0%, #08242b 45%, #020c0e 100%)',
        }}
      >
        {/* Top Header */}
        <div className="px-4 py-3.5 sm:px-5 sm:py-4 border-b border-white/10 flex items-center justify-between bg-black/30">
          <div className="flex items-center gap-3">
            <button
              id="btn-close-link-discover-top"
              onClick={() => setActiveModal('none')}
              className="p-1.5 rounded-full text-gray-400 hover:text-white hover:bg-white/10 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>

            <span className="px-3 py-1 rounded-full text-[11px] font-mono font-bold bg-[#0a353d] text-teal-300 border border-teal-400/40 flex items-center gap-1.5 shadow-inner">
              <span>Web Worker Radar</span>
              <Cpu className="w-3.5 h-3.5 text-teal-400 animate-spin" style={{ animationDuration: '6s' }} />
            </span>
          </div>

          <div className="text-right">
            <h3 className="font-bold text-sm sm:text-base text-white">وظيفة البحث والانضمام الفوري</h3>
            <p className="text-[10px] text-teal-300/80">رصد وحفظ الروابط المنشورة مع الانضمام الفوري التلقائي في الخلفية</p>
          </div>

          <div className="w-9 h-9 rounded-full bg-[#0a353d] flex items-center justify-center text-teal-400 border border-teal-400/30">
            <Search className="w-4 h-4" />
          </div>
        </div>

        {/* Modal Scrollable Body */}
        <div className="p-4 sm:p-5 overflow-y-auto space-y-3.5 flex-1 custom-scrollbar">
          {/* Top 4 KPI Metrics (Matching Screenshot) */}
          <div className="grid grid-cols-4 gap-2 text-center">
            {/* 1. Total Links */}
            <div className="p-2.5 rounded-2xl bg-[#092227]/90 border border-white/10 shadow-sm flex flex-col justify-center">
              <span className="text-[10px] text-gray-300 font-medium block">إجمالي الروابط</span>
              <span className="text-lg sm:text-xl font-bold text-white font-mono mt-0.5">{links.length}</span>
            </div>

            {/* 2. Joined */}
            <div className="p-2.5 rounded-2xl bg-[#06332a]/90 border border-emerald-500/30 shadow-sm flex flex-col justify-center">
              <span className="text-[10px] text-emerald-300 font-medium block">تم الانضمام</span>
              <span className="text-lg sm:text-xl font-bold text-emerald-400 font-mono mt-0.5">{totalJoined}</span>
            </div>

            {/* 3. Pending */}
            <div className="p-2.5 rounded-2xl bg-[#362705]/90 border border-amber-500/30 shadow-sm flex flex-col justify-center">
              <span className="text-[10px] text-amber-300 font-medium block">قيد الانتظار</span>
              <span className="text-lg sm:text-xl font-bold text-amber-400 font-mono mt-0.5">{totalPending}</span>
            </div>

            {/* 4. Failed / Skipped */}
            <div className="p-2.5 rounded-2xl bg-[#330e16]/90 border border-rose-500/30 shadow-sm flex flex-col justify-center">
              <span className="text-[10px] text-rose-300 font-medium block">فشل / تالف</span>
              <span className="text-lg sm:text-xl font-bold text-rose-400 font-mono mt-0.5">{totalFailed}</span>
            </div>
          </div>

          {/* Two Big Toggle Cards (Matching Screenshot) */}
          <div className="space-y-2.5">
            {/* Toggle 1: Live Radar */}
            <div className="p-3 sm:p-3.5 rounded-2xl bg-[#071f24] border border-teal-500/20 flex items-center justify-between shadow-md">
              <button
                id="btn-toggle-live-scanner-modal"
                onClick={handleToggleScanner}
                className="transition-transform active:scale-95"
              >
                {isScannerActive ? (
                  <div className="w-12 h-6 rounded-full bg-teal-500 p-0.5 flex items-center justify-end shadow-inner">
                    <div className="w-5 h-5 rounded-full bg-[#04171a] shadow-md transform" />
                  </div>
                ) : (
                  <div className="w-12 h-6 rounded-full bg-gray-700 p-0.5 flex items-center justify-start">
                    <div className="w-5 h-5 rounded-full bg-gray-400 shadow-md" />
                  </div>
                )}
              </button>

              <div className="flex items-center gap-3 text-right">
                <div>
                  <span className="font-bold text-xs sm:text-sm block text-white">رادار البحث اللحظي</span>
                  <span className="text-[10px] text-teal-300/70">استخراج الروابط من كل رسالة</span>
                </div>
                <div className="w-8 h-8 rounded-xl bg-teal-500/10 text-teal-400 flex items-center justify-center shrink-0">
                  <Radio className={`w-4 h-4 ${isScannerActive ? 'animate-pulse text-teal-400' : 'text-gray-500'}`} />
                </div>
              </div>
            </div>

            {/* Toggle 2: Instant Auto Join */}
            <div className="p-3 sm:p-3.5 rounded-2xl bg-[#06262a] border border-teal-500/30 flex items-center justify-between shadow-md">
              <button
                id="btn-toggle-instant-join-modal"
                onClick={handleToggleInstantJoin}
                className="transition-transform active:scale-95"
              >
                {isInstantJoin ? (
                  <div className="w-12 h-6 rounded-full bg-teal-400 p-0.5 flex items-center justify-end shadow-inner">
                    <div className="w-5 h-5 rounded-full bg-[#04171a] shadow-md" />
                  </div>
                ) : (
                  <div className="w-12 h-6 rounded-full bg-gray-700 p-0.5 flex items-center justify-start">
                    <div className="w-5 h-5 rounded-full bg-gray-400 shadow-md" />
                  </div>
                )}
              </button>

              <div className="flex items-center gap-3 text-right">
                <div>
                  <span className="font-bold text-xs sm:text-sm block text-teal-200">الانضمام التلقائي الفوري</span>
                  <span className="text-[10px] text-teal-300/80">انضمام فوري بدون تدخل يدوي</span>
                </div>
                <div className="w-8 h-8 rounded-xl bg-teal-400/15 text-teal-300 flex items-center justify-center shrink-0">
                  <ShieldCheck className="w-4 h-4" />
                </div>
              </div>
            </div>
          </div>

          {/* Telegram Safe Limits & Group Rules Indicator Banner */}
          <div className="p-3 rounded-2xl bg-[#07252a] border border-teal-500/25 text-xs text-teal-200 space-y-1.5">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-1.5 font-bold text-[11px] text-teal-300">
                <ShieldCheck className="w-3.5 h-3.5 text-teal-400" />
                <span>قوانين تيليجرام الرسمية للحماية:</span>
              </div>
              <span className="text-[10px] bg-teal-900/50 px-2 py-0.5 rounded-full border border-teal-500/30 text-teal-300 font-mono">
                {safeStatus.joinsInWindow} / {safeStatus.maxSafeLimit} انضمام
              </span>
            </div>

            <div className="text-[10px] text-gray-300 leading-relaxed flex items-center gap-1">
              <span>• قاعدة ثابتة: الانضمام مخصص <b>للمجموعات العامة فقط</b> (يتم استبعاد القنوات الخاصة تلقائياً).</span>
            </div>

            {safeStatus.isInSafeCooldown && (
              <div className="p-2 rounded-xl bg-amber-950/60 border border-amber-500/40 text-amber-200 text-[10px] flex items-center justify-between animate-pulse">
                <div className="flex items-center gap-1.5">
                  <Clock className="w-3.5 h-3.5 text-amber-400 shrink-0" />
                  <span>تم الوصول للحد الآمن لمنع الحظر. الروابط الجديدة محفوظة في الانتظار.</span>
                </div>
                <span className="font-mono font-bold bg-amber-500/20 px-2 py-0.5 rounded text-amber-300">
                  {formatSeconds(safeStatus.remainingSeconds)}
                </span>
              </div>
            )}
          </div>

          {/* Search Input Box (Matching Screenshot) */}
          <div className="relative">
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="بحث في الروابط أو المجموعات..."
              className="w-full bg-[#061e22] border border-teal-500/20 rounded-2xl pr-10 pl-4 py-2.5 text-xs text-white placeholder-gray-400 focus:outline-none focus:border-teal-400 shadow-inner"
            />
            <Search className="w-4 h-4 text-gray-400 absolute right-3.5 top-3" />
          </div>

          {/* Filter Pills + Download Button (Matching Screenshot) */}
          <div className="flex items-center justify-between gap-2">
            <button
              id="btn-export-links-csv"
              onClick={handleExportReport}
              className="p-2.5 rounded-xl bg-[#061e22] hover:bg-teal-500/20 border border-teal-500/20 text-teal-300 transition-colors shrink-0 shadow-sm"
              title="تصدير الروابط تقرير CSV"
            >
              <Download className="w-4 h-4" />
            </button>

            <div className="flex items-center gap-1 bg-[#061e22] p-1 rounded-2xl border border-teal-500/20 text-[11px] flex-1 justify-around">
              <button
                onClick={() => setFilterStatus('all')}
                className={`px-3 py-1 rounded-xl font-bold transition-all ${
                  filterStatus === 'all'
                    ? 'bg-[#0bbdbd] text-black shadow'
                    : 'text-gray-400 hover:text-white'
                }`}
              >
                الكل
              </button>
              <button
                onClick={() => setFilterStatus('joined')}
                className={`px-3 py-1 rounded-xl font-bold transition-all ${
                  filterStatus === 'joined'
                    ? 'bg-[#0bbdbd] text-black shadow'
                    : 'text-gray-400 hover:text-white'
                }`}
              >
                منضم
              </button>
              <button
                onClick={() => setFilterStatus('pending')}
                className={`px-3 py-1 rounded-xl font-bold transition-all ${
                  filterStatus === 'pending'
                    ? 'bg-[#0bbdbd] text-black shadow'
                    : 'text-gray-400 hover:text-white'
                }`}
              >
                انتظار
              </button>
              <button
                onClick={() => setFilterStatus('failed')}
                className={`px-3 py-1 rounded-xl font-bold transition-all ${
                  filterStatus === 'failed'
                    ? 'bg-[#0bbdbd] text-black shadow'
                    : 'text-gray-400 hover:text-white'
                }`}
              >
                فشل
              </button>
            </div>
          </div>

          {/* Captured Links Feed (Matching Screenshot) */}
          <div className="space-y-2 pt-1">
            {filteredLinks.length === 0 ? (
              <div className="p-8 rounded-2xl bg-[#061e22]/50 border border-white/5 text-center text-xs text-gray-400">
                لا توجد روابط مسجلة تطابق التصفية الحالية
              </div>
            ) : (
              <div className="space-y-2 max-h-64 overflow-y-auto custom-scrollbar pr-0.5">
                {filteredLinks.map((item) => {
                  const isPriv =
                    item.isPrivate ||
                    item.linkType === 'private_invite' ||
                    item.url.includes('+') ||
                    item.url.includes('joinchat');
                  const isBroadcast = item.linkType === 'broadcast_channel';

                  return (
                    <div
                      key={item.id}
                      className="p-3 rounded-2xl bg-[#061d21] border border-teal-500/15 hover:border-teal-500/35 transition-all flex items-center justify-between text-xs shadow-sm"
                    >
                      {/* Right side in RTL: Status Icon */}
                      <div className="flex items-center gap-3 overflow-hidden">
                        <div className="w-8 h-8 rounded-full bg-[#092b31] flex items-center justify-center shrink-0 border border-teal-500/20">
                          {item.status === 'joined' ? (
                            <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                          ) : item.status === 'failed' ? (
                            <XCircle className="w-4 h-4 text-rose-400" />
                          ) : isPriv || isBroadcast ? (
                            <Lock className="w-3.5 h-3.5 text-amber-400" />
                          ) : (
                            <Clock className="w-4 h-4 text-amber-400" />
                          )}
                        </div>

                        <div className="overflow-hidden text-right">
                          <div className="flex items-center gap-1.5">
                            <span className="font-mono font-bold text-teal-300 truncate text-[11px] sm:text-xs">
                              {item.url}
                            </span>
                          </div>

                          <div className="text-[10px] text-gray-400 flex items-center gap-1.5 flex-wrap mt-0.5">
                            <span>في: {item.sourceChatTitle || 'محادثة تلغرام'}</span>
                            <span>•</span>
                            <span>بواسطة: {item.senderName || 'مستخدم'}</span>
                            <span>•</span>
                            <span className="font-mono">{item.timestamp}</span>
                          </div>
                        </div>
                      </div>

                      {/* Left side in RTL: Action Button or Status Badge */}
                      <div className="flex items-center gap-2 shrink-0 mr-2">
                        {item.status === 'pending' && !isPriv && !isBroadcast && (
                          <button
                            onClick={() => handleManualJoin(item.id)}
                            disabled={isJoiningId === item.id || safeStatus.isInSafeCooldown}
                            className={`px-3 py-1.5 rounded-xl text-white text-[11px] font-bold shadow-md flex items-center gap-1 transition-all ${
                              safeStatus.isInSafeCooldown
                                ? 'bg-gray-700 cursor-not-allowed text-gray-400'
                                : 'bg-[#0bbdbd] hover:bg-[#12d2d2] text-black font-extrabold'
                            }`}
                          >
                            <UserPlus className="w-3 h-3" />
                            <span>{isJoiningId === item.id ? 'جاري...' : '+ انضمام'}</span>
                          </button>
                        )}

                        {item.status === 'pending' && isPriv && (
                          <span
                            className="text-[9px] font-bold px-2 py-1 rounded-xl bg-amber-500/15 text-amber-300 border border-amber-500/30"
                            title="مستبعد تلقائياً: مسموح بالمجموعات العامة فقط"
                          >
                            خاصة (تخطي تلقائي)
                          </span>
                        )}

                        {item.status === 'pending' && isBroadcast && (
                          <span
                            className="text-[9px] font-bold px-2 py-1 rounded-xl bg-indigo-500/15 text-indigo-300 border border-indigo-500/30"
                            title="قناة بث: مسموح بالمجموعات العامة فقط"
                          >
                            قناة بث (تخطي)
                          </span>
                        )}

                        {item.status === 'joining' && (
                          <span className="text-[10px] font-bold px-2.5 py-1 rounded-xl bg-teal-500/20 text-teal-300 animate-pulse">
                            جاري الانضمام...
                          </span>
                        )}

                        {item.status === 'joined' && (
                          <span className="text-[10px] font-bold px-2.5 py-1 rounded-xl bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                            {item.autoJoined ? 'انضمام فوري' : 'تم الانضمام'}
                          </span>
                        )}

                        {(item.status === 'failed' || item.status === 'skipped') && (
                          <span
                            className="text-[9px] font-bold px-2 py-1 rounded-xl bg-rose-500/20 text-rose-300 border border-rose-500/30 max-w-[110px] truncate"
                            title={item.failReason || 'فشل الانضمام'}
                          >
                            {item.failReason ? 'تخطي / غير صالح' : 'فشل'}
                          </span>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* Modal Bottom Footer (Matching Screenshot) */}
        <div className="p-3.5 sm:p-4 bg-black/40 border-t border-white/10 flex items-center justify-between">
          <button
            id="btn-close-link-discover-modal"
            onClick={() => setActiveModal('none')}
            className="px-6 py-2 rounded-2xl bg-[#092227] hover:bg-[#0e3037] border border-white/10 text-white text-xs font-bold transition-colors"
          >
            إغلاق
          </button>

          <button
            id="btn-clear-discovered-links"
            onClick={() => {
              backgroundSyncService.clearDiscoveredLinks();
              showToast('تم مسح سجل الروابط بالكامل', '🗑️');
            }}
            className="text-xs text-rose-400 hover:text-rose-300 font-bold flex items-center gap-1.5 px-3 py-1.5 rounded-xl hover:bg-rose-500/10 transition-colors"
          >
            <Trash2 className="w-4 h-4" />
            <span>مسح الروابط</span>
          </button>
        </div>
      </div>
    </div>
  );
};
