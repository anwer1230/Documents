import React from 'react';
import { 
  Users, 
  MessageSquare, 
  Clock, 
  ShieldAlert, 
  Lock, 
  CheckCircle2, 
  AlertOctagon, 
  Activity, 
  Radio, 
  ExternalLink,
  ChevronLeft,
  Sparkles
} from 'lucide-react';
import { TelegramGroup, GroupPermissionStatus, GroupConnectionHealth } from '../../types';

interface TelegramGroupCardProps {
  group: TelegramGroup;
  isSelected: boolean;
  onToggleSelect: (id: string) => void;
  onPingCheck: (id: string) => void;
  onOpenDetails: (group: TelegramGroup) => void;
  onToggleMonitoring: (id: string) => void;
}

export const TelegramGroupCard: React.FC<TelegramGroupCardProps> = ({
  group,
  isSelected,
  onToggleSelect,
  onPingCheck,
  onOpenDetails,
  onToggleMonitoring,
}) => {
  // Render permission status icon and label
  const renderPermissionBadge = (status: GroupPermissionStatus) => {
    switch (status) {
      case 'can_post':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold bg-emerald-500/10 text-emerald-300 border border-emerald-500/20">
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
            <span>نشر متاح</span>
          </span>
        );
      case 'slowmode':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold bg-amber-500/10 text-amber-300 border border-amber-500/20">
            <Clock className="w-3.5 h-3.5 text-amber-400" />
            <span>وضع البطء ({group.permissions.slowmodeDelaySeconds}ث)</span>
          </span>
        );
      case 'admin_only':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold bg-rose-500/10 text-rose-300 border border-rose-500/20">
            <Lock className="w-3.5 h-3.5 text-rose-400" />
            <span>للمشرفين فقط</span>
          </span>
        );
      case 'banned':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold bg-rose-500/20 text-rose-200 border border-rose-500/30">
            <ShieldAlert className="w-3.5 h-3.5 text-rose-400" />
            <span>محظور من النشر</span>
          </span>
        );
      case 'not_member':
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold bg-slate-700/50 text-slate-300 border border-slate-600">
            <AlertOctagon className="w-3.5 h-3.5 text-slate-400" />
            <span>غير منضم</span>
          </span>
        );
    }
  };

  // Connection health indicator
  const renderHealthIndicator = (health: GroupConnectionHealth, latency: number) => {
    switch (health) {
      case 'excellent':
        return (
          <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-emerald-950/40 border border-emerald-800/40 text-emerald-300 text-xs font-medium">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            <span>ممتاز ({latency}ms)</span>
          </div>
        );
      case 'good':
        return (
          <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-indigo-950/40 border border-indigo-800/40 text-indigo-300 text-xs font-medium">
            <span className="w-2 h-2 rounded-full bg-indigo-400" />
            <span>جيد ({latency}ms)</span>
          </div>
        );
      case 'degraded':
        return (
          <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-amber-950/40 border border-amber-800/40 text-amber-300 text-xs font-medium">
            <span className="w-2 h-2 rounded-full bg-amber-400" />
            <span>بطيء ({latency}ms)</span>
          </div>
        );
      case 'disconnected':
      default:
        return (
          <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-rose-950/40 border border-rose-800/40 text-rose-300 text-xs font-medium">
            <span className="w-2 h-2 rounded-full bg-rose-400" />
            <span>غير متصل</span>
          </div>
        );
    }
  };

  return (
    <div 
      id={`group-card-${group.id}`}
      className={`group relative rounded-2xl border transition-all duration-200 p-4 sm:p-5 ${
        isSelected
          ? 'bg-slate-800/90 border-indigo-500/70 shadow-lg shadow-indigo-500/10'
          : 'bg-slate-900/70 hover:bg-slate-800/60 border-slate-800/80 hover:border-slate-700/80'
      }`}
    >
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        {/* Left / Info Section */}
        <div className="flex items-start gap-3.5 flex-1 min-w-0">
          {/* Checkbox */}
          <div className="pt-1">
            <input
              id={`select-group-${group.id}`}
              type="checkbox"
              checked={isSelected}
              onChange={() => onToggleSelect(group.id)}
              className="w-4 h-4 rounded border-slate-700 bg-slate-900 text-indigo-600 focus:ring-indigo-500 cursor-pointer accent-indigo-600"
            />
          </div>

          {/* Group Avatar with Health Ring */}
          <div className="relative shrink-0">
            {group.avatarUrl ? (
              <img
                src={group.avatarUrl}
                alt={group.title}
                className="w-12 h-12 rounded-xl object-cover border border-slate-700 shadow-sm"
              />
            ) : (
              <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-indigo-600 to-purple-700 flex items-center justify-center text-white font-bold text-base shadow-sm">
                {group.title.charAt(0)}
              </div>
            )}
            
            {/* Status dot */}
            <span 
              className={`absolute -bottom-1 -left-1 w-3.5 h-3.5 rounded-full border-2 border-slate-900 ${
                group.connectionHealth === 'excellent' ? 'bg-emerald-400' :
                group.connectionHealth === 'good' ? 'bg-blue-400' :
                group.connectionHealth === 'degraded' ? 'bg-amber-400' : 'bg-rose-500'
              }`} 
              title={`حالة الاتصال: ${group.connectionHealth}`}
            />
          </div>

          {/* Title and Meta */}
          <div className="flex-1 min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <h3 className="text-sm sm:text-base font-bold text-white truncate max-w-[280px] sm:max-w-md">
                {group.title}
              </h3>
              <span className="text-[11px] px-2 py-0.5 rounded-full bg-slate-800 text-slate-400 border border-slate-700/60">
                {group.type === 'supergroup' ? 'مجموعة خارقة' : group.type === 'channel' ? 'قناة' : 'مجموعة'}
              </span>
              {group.isMonitored && (
                <span className="inline-flex items-center gap-1 text-[11px] px-2 py-0.5 rounded-full bg-purple-500/10 text-purple-300 border border-purple-500/30">
                  <Sparkles className="w-2.5 h-2.5 text-purple-400" />
                  <span>مراقبة الكلمات نشطة</span>
                </span>
              )}
            </div>

            <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-400 mt-1">
              {group.username && (
                <a
                  href={`https://t.me/${group.username}`}
                  target="_blank"
                  rel="noreferrer"
                  className="hover:text-indigo-400 flex items-center gap-1 transition-colors"
                >
                  <span>@{group.username}</span>
                  <ExternalLink className="w-2.5 h-2.5" />
                </a>
              )}
              <span className="flex items-center gap-1">
                <Users className="w-3 h-3 text-slate-500" />
                <span>{group.memberCount.toLocaleString()} عضو</span>
              </span>
              <span className="flex items-center gap-1">
                <MessageSquare className="w-3 h-3 text-slate-500" />
                <span>{group.lastActivityTime}</span>
              </span>
            </div>

            {/* Status reason description */}
            {group.statusReason && (
              <p className="text-xs text-slate-400 mt-2 line-clamp-1">
                {group.statusReason}
              </p>
            )}
          </div>
        </div>

        {/* Right / Badges and Actions */}
        <div className="flex sm:flex-col items-end justify-between sm:justify-center gap-3 shrink-0 pt-2 sm:pt-0 border-t sm:border-t-0 border-slate-800/80">
          <div className="flex items-center gap-2 flex-wrap justify-end">
            {renderHealthIndicator(group.connectionHealth, group.latencyMs)}
            {renderPermissionBadge(group.permissionStatus)}
          </div>

          <div className="flex items-center gap-1.5">
            <button
              id={`ping-btn-${group.id}`}
              type="button"
              onClick={() => onPingCheck(group.id)}
              className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition-colors border border-slate-700/60 cursor-pointer"
              title="فحص سرعة الاستجابة (Ping)"
            >
              <Activity className="w-3.5 h-3.5 text-indigo-400" />
            </button>

            <button
              id={`monitor-toggle-${group.id}`}
              type="button"
              onClick={() => onToggleMonitoring(group.id)}
              className={`p-1.5 rounded-lg border transition-colors cursor-pointer ${
                group.isMonitored
                  ? 'bg-purple-500/20 border-purple-500/40 text-purple-300'
                  : 'bg-slate-800 hover:bg-slate-700 border-slate-700/60 text-slate-400'
              }`}
              title={group.isMonitored ? 'إيقاف مراقبة الكلمات' : 'تفعيل مراقبة الكلمات'}
            >
              <Radio className="w-3.5 h-3.5" />
            </button>

            <button
              id={`details-btn-${group.id}`}
              type="button"
              onClick={() => onOpenDetails(group)}
              className="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-indigo-600/20 hover:bg-indigo-600/30 border border-indigo-500/40 text-indigo-300 text-xs font-medium transition-colors cursor-pointer"
            >
              <span>التفاصيل</span>
              <ChevronLeft className="w-3 h-3" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
