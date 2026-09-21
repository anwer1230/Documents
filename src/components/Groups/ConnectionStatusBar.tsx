import React from 'react';
import { 
  Activity, 
  Wifi, 
  WifiOff, 
  ShieldCheck, 
  AlertTriangle, 
  RefreshCw, 
  Radio
} from 'lucide-react';
import { TelegramGroup } from '../../types';

interface ConnectionStatusBarProps {
  groups: TelegramGroup[];
  isCheckingHealth: boolean;
  onRefreshAllHealth: () => void;
}

export const ConnectionStatusBar: React.FC<ConnectionStatusBarProps> = ({
  groups,
  isCheckingHealth,
  onRefreshAllHealth,
}) => {
  const total = groups.length;
  const connectedCount = groups.filter((g) => g.connectionHealth !== 'disconnected').length;
  const writableCount = groups.filter((g) => g.permissionStatus === 'can_post').length;
  const restrictedCount = groups.filter((g) => g.permissionStatus === 'admin_only' || g.permissionStatus === 'banned').length;
  
  const connectedGroups = groups.filter((g) => g.latencyMs > 0);
  const avgLatency = connectedGroups.length > 0 
    ? Math.round(connectedGroups.reduce((acc, g) => acc + g.latencyMs, 0) / connectedGroups.length)
    : 0;

  const healthScore = total > 0 
    ? Math.round(((connectedCount * 0.6) + (writableCount * 0.4)) / total * 100) 
    : 0;

  return (
    <div className="bg-slate-900/90 border border-slate-800/80 rounded-2xl p-4 sm:p-5 text-slate-100 shadow-xl backdrop-blur-md">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
        <div className="flex items-center gap-3">
          <div className="relative flex items-center justify-center w-11 h-11 rounded-xl bg-gradient-to-br from-indigo-500/20 to-purple-500/20 border border-indigo-500/30 text-indigo-400">
            <Radio className="w-5 h-5 animate-pulse" />
            <span className="absolute -top-1 -right-1 flex h-3 w-3">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-3 w-3 bg-emerald-500"></span>
            </span>
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-base sm:text-lg font-bold text-white tracking-tight">
                مؤشر صحة اتصال تيليجرام (MTProto Health)
              </h2>
              <span className="px-2 py-0.5 text-xs font-semibold rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                متصل
              </span>
            </div>
            <p className="text-xs text-slate-400">
              مراقبة آنية لجودة الاتصال والـ Ping والصلاحيات لجميع المجموعات والقنوات
            </p>
          </div>
        </div>

        <button
          id="refresh-all-health-btn"
          type="button"
          onClick={onRefreshAllHealth}
          disabled={isCheckingHealth}
          className="self-start sm:self-auto flex items-center gap-2 px-4 py-2 text-xs font-semibold rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white transition-all shadow-md shadow-indigo-600/20 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isCheckingHealth ? 'animate-spin' : ''}`} />
          <span>{isCheckingHealth ? 'جاري فحص الصحة...' : 'فحص صحة الاتصال'}</span>
        </button>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-4">
        {/* Metric 1: Overall Health */}
        <div className="bg-slate-800/50 border border-slate-700/50 rounded-xl p-3 flex items-center gap-3">
          <div className="p-2.5 rounded-lg bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            <ShieldCheck className="w-4 h-4" />
          </div>
          <div>
            <div className="text-xs text-slate-400">معدل كفاءة الشبكة</div>
            <div className="text-lg font-bold text-emerald-300">{healthScore}%</div>
          </div>
        </div>

        {/* Metric 2: Average Latency */}
        <div className="bg-slate-800/50 border border-slate-700/50 rounded-xl p-3 flex items-center gap-3">
          <div className="p-2.5 rounded-lg bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
            <Activity className="w-4 h-4" />
          </div>
          <div>
            <div className="text-xs text-slate-400">متوسط زمن الاستجابة</div>
            <div className="text-lg font-bold text-indigo-300">{avgLatency} ms</div>
          </div>
        </div>

        {/* Metric 3: Active Postable */}
        <div className="bg-slate-800/50 border border-slate-700/50 rounded-xl p-3 flex items-center gap-3">
          <div className="p-2.5 rounded-lg bg-blue-500/10 text-blue-400 border border-blue-500/20">
            <Wifi className="w-4 h-4" />
          </div>
          <div>
            <div className="text-xs text-slate-400">جاهزة للنشر الفوري</div>
            <div className="text-lg font-bold text-blue-300">{writableCount} <span className="text-xs font-normal text-slate-400">/ {total}</span></div>
          </div>
        </div>

        {/* Metric 4: Restricted / Warning */}
        <div className="bg-slate-800/50 border border-slate-700/50 rounded-xl p-3 flex items-center gap-3">
          <div className={`p-2.5 rounded-lg border ${
            restrictedCount > 0 
              ? 'bg-amber-500/10 text-amber-400 border-amber-500/20' 
              : 'bg-slate-700/30 text-slate-400 border-slate-700'
          }`}>
            {restrictedCount > 0 ? <AlertTriangle className="w-4 h-4" /> : <WifiOff className="w-4 h-4" />}
          </div>
          <div>
            <div className="text-xs text-slate-400">المقيدة / المحظورة</div>
            <div className={`text-lg font-bold ${restrictedCount > 0 ? 'text-amber-300' : 'text-slate-300'}`}>
              {restrictedCount}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
