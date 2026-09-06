import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  Activity,
  Wifi,
  WifiOff,
  Clock,
  AlertTriangle,
  CheckCircle2,
  Trash2,
  RefreshCw,
  Copy,
  Check,
  X,
  Play,
  Shield,
  Zap,
  Info,
  Sliders,
} from 'lucide-react';
import {
  TelemetryEvent,
  getTelemetryLogs,
  clearTelemetryLogs,
  measureServerLatency,
  isTelemetryEnabled,
  setTelemetryEnabled,
  MAX_TELEMETRY_LOGS,
} from '../../utils/telemetry';

interface TelemetryLogModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const TelemetryLogModal: React.FC<TelemetryLogModalProps> = ({ isOpen, onClose }) => {
  const [logs, setLogs] = useState<TelemetryEvent[]>(() => getTelemetryLogs());
  const [categoryFilter, setCategoryFilter] = useState<'all' | 'network' | 'latency' | 'sync'>('all');
  const [copied, setCopied] = useState<boolean>(false);
  const [isPinging, setIsPinging] = useState<boolean>(false);
  const [lastPingResult, setLastPingResult] = useState<{ clientMs: number; serverMs?: number } | null>(null);
  const [enabled, setEnabled] = useState<boolean>(() => isTelemetryEnabled());

  // Reload logs
  const refreshLogs = useCallback(() => {
    setLogs(getTelemetryLogs());
    setEnabled(isTelemetryEnabled());
  }, []);

  // Subscribe to live telemetry update events
  useEffect(() => {
    if (!isOpen) return;
    refreshLogs();

    const handleUpdate = () => {
      setLogs(getTelemetryLogs());
    };
    const handleStatus = (e: any) => {
      if (typeof e?.detail?.enabled === 'boolean') {
        setEnabled(e.detail.enabled);
      }
    };

    window.addEventListener('tg_telemetry_updated', handleUpdate);
    window.addEventListener('tg_telemetry_status_changed', handleStatus);

    return () => {
      window.removeEventListener('tg_telemetry_updated', handleUpdate);
      window.removeEventListener('tg_telemetry_status_changed', handleStatus);
    };
  }, [isOpen, refreshLogs]);

  // Filter logs
  const filteredLogs = useMemo(() => {
    if (categoryFilter === 'all') return logs;
    return logs.filter((l) => l.category === categoryFilter);
  }, [logs, categoryFilter]);

  // Statistics
  const stats = useMemo(() => {
    let networkCount = 0;
    let latencyCount = 0;
    let syncErrorCount = 0;
    let avgLatency = 0;
    let latencySum = 0;
    let latencyEvents = 0;

    logs.forEach((l) => {
      if (l.category === 'network') networkCount++;
      if (l.category === 'latency') {
        latencyCount++;
        if (typeof l.durationMs === 'number') {
          latencySum += l.durationMs;
          latencyEvents++;
        }
      }
      if (l.category === 'sync' && l.type === 'sync_error') syncErrorCount++;
    });

    avgLatency = latencyEvents > 0 ? Math.round(latencySum / latencyEvents) : 0;

    return {
      total: logs.length,
      networkCount,
      latencyCount,
      syncErrorCount,
      avgLatency,
    };
  }, [logs]);

  // Ping Trigger
  const handleRunPing = async () => {
    if (isPinging) return;
    setIsPinging(true);
    try {
      const res = await measureServerLatency(4);
      if (res.success) {
        setLastPingResult({ clientMs: res.clientRoundTripMs, serverMs: res.serverDurationMs });
      }
    } finally {
      setIsPinging(false);
      refreshLogs();
    }
  };

  // Copy Logs to Clipboard
  const handleCopyLogs = () => {
    try {
      const payload = {
        exportedAt: new Date().toISOString(),
        totalEvents: logs.length,
        enabled,
        userAgent: navigator.userAgent,
        onLine: navigator.onLine,
        events: logs,
      };
      navigator.clipboard.writeText(JSON.stringify(payload, null, 2));
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.warn('[Telemetry] Copy failed:', err);
    }
  };

  // Clear Logs
  const handleClear = () => {
    clearTelemetryLogs();
    setLogs([]);
  };

  // Toggle Enable
  const handleToggleEnable = () => {
    const next = !enabled;
    setTelemetryEnabled(next);
    setEnabled(next);
  };

  if (!isOpen) return null;

  return (
    <div
      id="telemetry-log-modal-overlay"
      className="fixed inset-0 z-[130] flex items-center justify-center p-3 sm:p-5 bg-black/80 backdrop-blur-md animate-fadeIn"
      onClick={onClose}
    >
      <div
        id="telemetry-log-modal-container"
        className="w-full max-w-3xl h-[88vh] max-h-[820px] bg-[#0e1621] border border-cyan-500/30 rounded-2xl shadow-2xl flex flex-col text-white select-none overflow-hidden"
        dir="rtl"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-white/10 bg-[#17212b] shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-cyan-500/15 border border-cyan-500/30 flex items-center justify-center text-cyan-400 shadow-sm">
              <Activity className="w-5 h-5 animate-pulse" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-bold text-white m-0 flex items-center gap-1.5">
                  <span>سجل بيانات القياس وتشخيص المزامنة</span>
                  <span className="text-[10px] px-2 py-0.5 rounded-full bg-cyan-500/20 text-cyan-300 border border-cyan-500/30 font-mono">
                    Telemetry Listener
                  </span>
                </h3>
              </div>
              <p className="text-xs text-gray-400 m-0 mt-0.5">
                مراقبة أداء الشبكة، زمن استجابة الخادم (Latency)، وتشخيص أسباب بطء أو توقف المزامنة
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {/* Enable/Disable Toggle */}
            <button
              type="button"
              onClick={handleToggleEnable}
              className={`px-2.5 py-1 rounded-lg text-xs font-semibold border flex items-center gap-1.5 transition-all ${
                enabled
                  ? 'bg-emerald-500/20 border-emerald-500/40 text-emerald-300'
                  : 'bg-rose-500/20 border-rose-500/40 text-rose-300'
              }`}
              title={enabled ? 'التتبع مفعل حالياً' : 'التتبع معطل'}
            >
              <span className={`w-2 h-2 rounded-full ${enabled ? 'bg-emerald-400 animate-ping' : 'bg-rose-400'}`} />
              <span>{enabled ? 'التتبع نشط' : 'معطل'}</span>
            </button>

            <button
              type="button"
              onClick={onClose}
              className="p-1.5 rounded-lg bg-white/5 hover:bg-white/10 text-gray-400 hover:text-white border border-white/10 transition-colors"
              title="إغلاق"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Security & Strict Rules Badge */}
        <div className="px-5 py-2 bg-emerald-500/10 border-b border-emerald-500/20 flex items-center justify-between text-[11px] text-emerald-200">
          <div className="flex items-center gap-2">
            <Shield className="w-4 h-4 text-emerald-400 shrink-0" />
            <span>
              <strong>أمان وخصوصية 100%:</strong> لا يتم تسجيل محتوى الرسائل أو أي بيانات شخصية، ولا يتم إرسال أي بيانات لخوادم خارجية.
            </span>
          </div>
          <span className="font-mono text-[10px] text-emerald-300/80">Max: {MAX_TELEMETRY_LOGS} Events</span>
        </div>

        {/* KPI Summary Cards */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 p-4 shrink-0 bg-[#0e1621]">
          <div className="bg-white/[0.03] border border-white/10 rounded-xl p-3 flex items-center gap-3">
            <div className="w-9 h-9 rounded-lg bg-cyan-500/20 border border-cyan-500/30 flex items-center justify-center text-cyan-400 shrink-0">
              <Activity className="w-4 h-4" />
            </div>
            <div className="min-w-0">
              <span className="text-[11px] text-gray-400 block truncate">إجمالي الأحداث</span>
              <span className="text-base font-bold text-white font-mono">{stats.total} / {MAX_TELEMETRY_LOGS}</span>
            </div>
          </div>

          <div className="bg-white/[0.03] border border-blue-500/20 rounded-xl p-3 flex items-center gap-3">
            <div className="w-9 h-9 rounded-lg bg-blue-500/20 border border-blue-500/30 flex items-center justify-center text-blue-400 shrink-0">
              <Clock className="w-4 h-4" />
            </div>
            <div className="min-w-0">
              <span className="text-[11px] text-blue-300/80 block truncate">متوسط الاستجابة (Ping)</span>
              <span className="text-base font-bold text-blue-300 font-mono">
                {stats.avgLatency > 0 ? `${stats.avgLatency}ms` : '--'}
              </span>
            </div>
          </div>

          <div className="bg-white/[0.03] border border-emerald-500/20 rounded-xl p-3 flex items-center gap-3">
            <div className="w-9 h-9 rounded-lg bg-emerald-500/20 border border-emerald-500/30 flex items-center justify-center text-emerald-400 shrink-0">
              <Wifi className="w-4 h-4" />
            </div>
            <div className="min-w-0">
              <span className="text-[11px] text-emerald-300/80 block truncate">حالة الاتصال بالمتصفح</span>
              <span className="text-sm font-bold text-emerald-300">
                {typeof navigator !== 'undefined' && navigator.onLine ? 'متصل (Online)' : 'منقطع (Offline)'}
              </span>
            </div>
          </div>

          <div className="bg-white/[0.03] border border-rose-500/20 rounded-xl p-3 flex items-center gap-3">
            <div className="w-9 h-9 rounded-lg bg-rose-500/20 border border-rose-500/30 flex items-center justify-center text-rose-400 shrink-0">
              <AlertTriangle className="w-4 h-4" />
            </div>
            <div className="min-w-0">
              <span className="text-[11px] text-rose-300/80 block truncate">أخطاء المزامنة</span>
              <span className="text-base font-bold text-rose-300 font-mono">{stats.syncErrorCount}</span>
            </div>
          </div>
        </div>

        {/* Action Toolbar */}
        <div className="px-5 py-2.5 border-y border-white/[0.08] bg-[#131b26] flex flex-wrap items-center justify-between gap-2 shrink-0">
          {/* Filter Pills */}
          <div className="flex items-center gap-1.5 overflow-x-auto">
            <button
              type="button"
              onClick={() => setCategoryFilter('all')}
              className={`px-3 py-1 rounded-lg text-xs font-medium border transition-all ${
                categoryFilter === 'all'
                  ? 'bg-cyan-500/20 border-cyan-500/40 text-cyan-300 font-bold'
                  : 'bg-white/[0.03] border-white/10 text-gray-400 hover:text-white'
              }`}
            >
              الكل ({logs.length})
            </button>
            <button
              type="button"
              onClick={() => setCategoryFilter('network')}
              className={`px-3 py-1 rounded-lg text-xs font-medium border transition-all ${
                categoryFilter === 'network'
                  ? 'bg-emerald-500/20 border-emerald-500/40 text-emerald-300 font-bold'
                  : 'bg-white/[0.03] border-white/10 text-gray-400 hover:text-white'
              }`}
            >
              الاتصال ({stats.networkCount})
            </button>
            <button
              type="button"
              onClick={() => setCategoryFilter('latency')}
              className={`px-3 py-1 rounded-lg text-xs font-medium border transition-all ${
                categoryFilter === 'latency'
                  ? 'bg-blue-500/20 border-blue-500/40 text-blue-300 font-bold'
                  : 'bg-white/[0.03] border-white/10 text-gray-400 hover:text-white'
              }`}
            >
              الاستجابة ({stats.latencyCount})
            </button>
            <button
              type="button"
              onClick={() => setCategoryFilter('sync')}
              className={`px-3 py-1 rounded-lg text-xs font-medium border transition-all ${
                categoryFilter === 'sync'
                  ? 'bg-rose-500/20 border-rose-500/40 text-rose-300 font-bold'
                  : 'bg-white/[0.03] border-white/10 text-gray-400 hover:text-white'
              }`}
            >
              المزامنة ({logs.filter((l) => l.category === 'sync').length})
            </button>
          </div>

          {/* Action Buttons */}
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={handleRunPing}
              disabled={isPinging}
              className="px-3 py-1.5 rounded-lg text-xs font-bold text-black bg-cyan-400 hover:bg-cyan-300 disabled:opacity-50 flex items-center gap-1.5 transition-all shadow-sm"
              title="إجراء فحص فوري لزمن الاستجابة مع الخادم"
            >
              <Zap className={`w-3.5 h-3.5 ${isPinging ? 'animate-spin' : ''}`} />
              <span>{isPinging ? 'جاري الفحص...' : 'قياس الاستجابة (Ping)'}</span>
            </button>

            <button
              type="button"
              onClick={handleCopyLogs}
              disabled={logs.length === 0}
              className="px-3 py-1.5 rounded-lg text-xs font-medium bg-white/5 hover:bg-white/10 border border-white/10 text-gray-300 hover:text-white flex items-center gap-1.5 transition-all disabled:opacity-40"
              title="نسخ الأحداث للحافظة بصيغة JSON"
            >
              {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
              <span>{copied ? 'تم النسخ!' : 'نسخ السجل'}</span>
            </button>

            <button
              type="button"
              onClick={refreshLogs}
              className="p-1.5 rounded-lg bg-white/5 hover:bg-white/10 border border-white/10 text-gray-300 hover:text-white transition-all"
              title="تحديث البيانات"
            >
              <RefreshCw className="w-4 h-4" />
            </button>

            {logs.length > 0 && (
              <button
                type="button"
                onClick={handleClear}
                className="p-1.5 rounded-lg bg-rose-500/10 hover:bg-rose-500/20 border border-rose-500/20 text-rose-300 transition-all"
                title="مسح السجل"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            )}
          </div>
        </div>

        {/* Diagnostic Guide Banner */}
        <div className="px-5 py-2.5 bg-cyan-950/30 border-b border-cyan-500/20 flex items-start gap-2.5 text-xs text-cyan-200 shrink-0">
          <Info className="w-4 h-4 text-cyan-400 shrink-0 mt-0.5" />
          <div className="leading-relaxed">
            <span className="font-bold text-white">كيف تكتشف سبب تعليق "جاري المزامنة"؟ </span>
            <span>
              إذا ظهرت أخطاء مثل <code className="bg-black/40 px-1 py-0.5 rounded text-amber-300 font-mono">AUTH_KEY_UNREGISTERED</code> فهذا يعني إلغاء الجلسة من تيليجرام.
              إذا ظهر <code className="bg-black/40 px-1 py-0.5 rounded text-rose-300 font-mono">FLOOD_WAIT</code> فهناك حظر مؤقت.
              وإذا كان <code className="bg-black/40 px-1 py-0.5 rounded text-blue-300 font-mono">Ping</code> مرتفعاً جداً أو تكرر حدث <code className="bg-black/40 px-1 py-0.5 rounded text-gray-300 font-mono">network_offline</code> فالمشكلة في بطء أو انقطاع الاتصال.
            </span>
          </div>
        </div>

        {/* Events List */}
        <div className="flex-1 overflow-y-auto p-4 space-y-2">
          {filteredLogs.length === 0 ? (
            <div className="h-64 flex flex-col items-center justify-center text-center p-6 bg-white/[0.01] border border-dashed border-white/10 rounded-xl">
              <Activity className="w-10 h-10 text-gray-500 mb-2" />
              <p className="text-sm font-semibold text-gray-300 mb-1">لا توجد أحداث مسجلة حالياً</p>
              <p className="text-xs text-gray-500 max-w-sm mb-3">
                اضغط على زر "قياس الاستجابة (Ping)" لاختبار زمن الاتصال وتسجيل أول حدث قياس فوراً.
              </p>
              <button
                type="button"
                onClick={handleRunPing}
                className="px-4 py-2 rounded-lg text-xs font-bold text-black bg-cyan-400 hover:bg-cyan-300 flex items-center gap-1.5 shadow-sm"
              >
                <Zap className="w-3.5 h-3.5" />
                <span>إجراء فحص Ping الآن</span>
              </button>
            </div>
          ) : (
            filteredLogs.map((item, index) => {
              const isNetwork = item.category === 'network';
              const isLatency = item.category === 'latency';
              const isSyncError = item.type === 'sync_error';
              const isSyncSuccess = item.type === 'sync_success';
              const isOnline = item.type === 'network_online';
              const isOffline = item.type === 'network_offline';

              return (
                <div
                  key={item.id || index}
                  className={`p-3 rounded-xl border transition-all text-xs flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 ${
                    isSyncError || isOffline
                      ? 'bg-rose-500/[0.05] border-rose-500/30'
                      : isOnline || isSyncSuccess
                      ? 'bg-emerald-500/[0.05] border-emerald-500/30'
                      : isLatency
                      ? 'bg-blue-500/[0.05] border-blue-500/30'
                      : 'bg-white/[0.02] border-white/10'
                  }`}
                >
                  {/* Left info */}
                  <div className="flex items-start sm:items-center gap-3">
                    <div
                      className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 ${
                        isSyncError
                          ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30'
                          : isOffline
                          ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30'
                          : isOnline
                          ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                          : isSyncSuccess
                          ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                          : 'bg-blue-500/20 text-blue-400 border border-blue-500/30'
                      }`}
                    >
                      {isOffline ? (
                        <WifiOff className="w-4 h-4" />
                      ) : isOnline ? (
                        <Wifi className="w-4 h-4" />
                      ) : isSyncError ? (
                        <AlertTriangle className="w-4 h-4" />
                      ) : isSyncSuccess ? (
                        <CheckCircle2 className="w-4 h-4" />
                      ) : (
                        <Clock className="w-4 h-4" />
                      )}
                    </div>

                    <div>
                      <div className="flex items-center gap-2 flex-wrap mb-0.5">
                        <span
                          className={`font-mono text-[11px] font-bold px-1.5 py-0.5 rounded border ${
                            isSyncError
                              ? 'bg-rose-500/20 border-rose-500/40 text-rose-300'
                              : isOnline || isSyncSuccess
                              ? 'bg-emerald-500/20 border-emerald-500/40 text-emerald-300'
                              : 'bg-blue-500/20 border-blue-500/40 text-blue-300'
                          }`}
                        >
                          {item.type}
                        </span>

                        {item.reason && (
                          <span className="font-semibold text-gray-200">
                            {item.reason}
                          </span>
                        )}
                      </div>

                      <div className="flex items-center gap-3 text-[11px] text-gray-400 flex-wrap">
                        <span>
                          {new Date(item.timestamp).toLocaleTimeString('ar-EG', {
                            hour: '2-digit',
                            minute: '2-digit',
                            second: '2-digit',
                          })}
                        </span>
                        <span className="text-gray-600">•</span>
                        <span className="font-mono text-[10px] text-gray-500">
                          {item.timestamp}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Right metrics / duration */}
                  <div className="flex items-center gap-2 shrink-0 pr-11 sm:pr-0">
                    {typeof item.durationMs === 'number' && (
                      <div className="px-2.5 py-1 rounded-lg bg-black/40 border border-white/10 font-mono text-[11px] text-cyan-300 flex items-center gap-1">
                        <span className="text-gray-400 text-[10px]">Client:</span>
                        <span className="font-bold">{item.durationMs}ms</span>
                      </div>
                    )}

                    {typeof item.serverDurationMs === 'number' && (
                      <div className="px-2.5 py-1 rounded-lg bg-black/40 border border-white/10 font-mono text-[11px] text-emerald-300 flex items-center gap-1">
                        <span className="text-gray-400 text-[10px]">Server:</span>
                        <span className="font-bold">{item.serverDurationMs}ms</span>
                      </div>
                    )}
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Footer */}
        <div className="px-5 py-3 border-t border-white/10 bg-[#17212b] flex items-center justify-between text-xs text-gray-400 shrink-0">
          <span>
            يتم الاحتفاظ بآخر <strong>50 حدثاً</strong> محلياً لتشخيص مشاكل الاتصال بدون أي تأثير على الأداء.
          </span>
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-1.5 rounded-lg bg-white/10 hover:bg-white/20 text-white font-semibold transition-colors"
          >
            إغلاق
          </button>
        </div>
      </div>
    </div>
  );
};

export default TelemetryLogModal;
