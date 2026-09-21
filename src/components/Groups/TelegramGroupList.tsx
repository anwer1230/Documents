import React, { useState, useMemo } from 'react';
import { 
  Search, 
  Filter, 
  Plus, 
  CheckSquare, 
  Square, 
  Activity, 
  Radio, 
  Trash2, 
  ArrowUpDown,
  Layers,
  Sparkles,
  CheckCircle2,
  Clock,
  ShieldAlert,
  WifiOff
} from 'lucide-react';
import { TelegramGroup, StatusFilter } from '../../types';
import { TelegramGroupCard } from './TelegramGroupCard';
import { GroupDetailsModal } from './GroupDetailsModal';
import { AddGroupModal } from './AddGroupModal';

interface TelegramGroupListProps {
  groups: TelegramGroup[];
  onUpdateGroups: (updated: TelegramGroup[]) => void;
  onToast: (msg: string, icon?: string) => void;
}

export const TelegramGroupList: React.FC<TelegramGroupListProps> = ({
  groups,
  onUpdateGroups,
  onToast,
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [activeFilter, setActiveFilter] = useState<StatusFilter>('all');
  const [sortBy, setSortBy] = useState<'title' | 'members' | 'latency' | 'status'>('latency');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc');
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [detailedGroup, setDetailedGroup] = useState<TelegramGroup | null>(null);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);

  // Filter and Sort calculation
  const filteredGroups = useMemo(() => {
    return groups
      .filter((group) => {
        // Search query check
        const q = searchQuery.toLowerCase().trim();
        const matchesSearch = 
          !q ||
          group.title.toLowerCase().includes(q) ||
          (group.username && group.username.toLowerCase().includes(q)) ||
          group.id.includes(q);

        if (!matchesSearch) return false;

        // Status filter check
        switch (activeFilter) {
          case 'active':
            return group.permissionStatus === 'can_post';
          case 'slowmode':
            return group.permissionStatus === 'slowmode';
          case 'restricted':
            return group.permissionStatus === 'admin_only' || group.permissionStatus === 'banned';
          case 'degraded':
            return group.connectionHealth === 'degraded' || group.connectionHealth === 'disconnected';
          case 'all':
          default:
            return true;
        }
      })
      .sort((a, b) => {
        let diff = 0;
        if (sortBy === 'title') {
          diff = a.title.localeCompare(b.title);
        } else if (sortBy === 'members') {
          diff = b.memberCount - a.memberCount;
        } else if (sortBy === 'latency') {
          diff = (a.latencyMs || 9999) - (b.latencyMs || 9999);
        } else if (sortBy === 'status') {
          diff = a.permissionStatus.localeCompare(b.permissionStatus);
        }
        return sortOrder === 'asc' ? diff : -diff;
      });
  }, [groups, searchQuery, activeFilter, sortBy, sortOrder]);

  // Handle individual selection
  const handleToggleSelect = (id: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  // Select all or deselect all
  const handleToggleSelectAll = () => {
    if (selectedIds.size === filteredGroups.length && filteredGroups.length > 0) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(filteredGroups.map((g) => g.id)));
    }
  };

  // Ping Check for single group
  const handlePingGroup = (id: string) => {
    const randomLatency = Math.floor(25 + Math.random() * 85);
    const updated = groups.map((g) => {
      if (g.id === id) {
        const health = randomLatency < 70 ? 'excellent' : randomLatency < 180 ? 'good' : 'degraded';
        return {
          ...g,
          latencyMs: randomLatency,
          connectionHealth: health as any,
          lastPingTimestamp: Date.now(),
        };
      }
      return g;
    });
    onUpdateGroups(updated);
    if (detailedGroup && detailedGroup.id === id) {
      setDetailedGroup(updated.find((g) => g.id === id) || null);
    }
    onToast(`📡 تم تحديث فحص الـ Ping: ${randomLatency}ms`, '⚡');
  };

  // Toggle Keyword Monitoring for single group
  const handleToggleMonitoring = (id: string) => {
    const updated = groups.map((g) => {
      if (g.id === id) {
        const nextMonitored = !g.isMonitored;
        return { ...g, isMonitored: nextMonitored };
      }
      return g;
    });
    onUpdateGroups(updated);
    const target = updated.find((g) => g.id === id);
    if (detailedGroup && detailedGroup.id === id) {
      setDetailedGroup(target || null);
    }
    onToast(
      target?.isMonitored ? `🔔 تم تفعيل رصد الكلمات لـ ${target.title}` : `🔕 تم إيقاف المراقبة لـ ${target?.title}`,
      '🎯'
    );
  };

  // Bulk Ping Check for selected groups
  const handleBulkPing = () => {
    if (selectedIds.size === 0) return;
    const updated = groups.map((g) => {
      if (selectedIds.has(g.id)) {
        const latency = Math.floor(30 + Math.random() * 70);
        return {
          ...g,
          latencyMs: latency,
          connectionHealth: (latency < 70 ? 'excellent' : 'good') as any,
          lastPingTimestamp: Date.now(),
        };
      }
      return g;
    });
    onUpdateGroups(updated);
    onToast(`⚡ تم فحص وتحديث اتصال ${selectedIds.size} مجموعة محددة بنجاح`, '✅');
  };

  // Bulk Monitoring Toggle
  const handleBulkToggleMonitor = (enable: boolean) => {
    if (selectedIds.size === 0) return;
    const updated = groups.map((g) => {
      if (selectedIds.has(g.id)) {
        return { ...g, isMonitored: enable };
      }
      return g;
    });
    onUpdateGroups(updated);
    onToast(
      enable ? `🔔 تم تفعيل المراقبة لـ ${selectedIds.size} مجموعة` : `🔕 تم إيقاف المراقبة لـ ${selectedIds.size} مجموعة`,
      '✨'
    );
  };

  // Bulk Delete
  const handleBulkDelete = () => {
    if (selectedIds.size === 0) return;
    const count = selectedIds.size;
    const updated = groups.filter((g) => !selectedIds.has(g.id));
    onUpdateGroups(updated);
    setSelectedIds(new Set());
    onToast(`🗑️ تم إزالة ${count} مجموعة من القائمة`, '🧹');
  };

  // Add group
  const handleAddGroup = (newG: TelegramGroup) => {
    onUpdateGroups([newG, ...groups]);
    onToast(`✨ تمت إضافة وفحص مجموعة ${newG.title} بنجاح`, '🚀');
  };

  return (
    <div className="space-y-4">
      {/* Controls Bar */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-4 shadow-lg flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        {/* Search Field */}
        <div className="relative flex-1 min-w-[240px]">
          <Search className="w-4 h-4 text-slate-400 absolute top-3.5 right-3.5" />
          <input
            id="groups-search-input"
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="البحث باسم المجموعة، المعرف، أو الرابط..."
            className="w-full bg-slate-800/80 border border-slate-700/80 rounded-xl pr-10 pl-4 py-2.5 text-xs text-white placeholder-slate-400 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
          />
          {searchQuery && (
            <button
              type="button"
              onClick={() => setSearchQuery('')}
              className="absolute top-2.5 left-3 text-xs text-slate-400 hover:text-white px-1.5 py-0.5 rounded bg-slate-700 cursor-pointer"
            >
              مسح
            </button>
          )}
        </div>

        {/* Filter Pills */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 lg:pb-0 text-xs">
          <button
            id="filter-tab-all"
            type="button"
            onClick={() => setActiveFilter('all')}
            className={`px-3 py-2 rounded-xl font-semibold transition-all whitespace-nowrap cursor-pointer ${
              activeFilter === 'all'
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'bg-slate-800/80 text-slate-300 hover:bg-slate-800'
            }`}
          >
            الكل ({groups.length})
          </button>

          <button
            id="filter-tab-active"
            type="button"
            onClick={() => setActiveFilter('active')}
            className={`flex items-center gap-1 px-3 py-2 rounded-xl font-semibold transition-all whitespace-nowrap cursor-pointer ${
              activeFilter === 'active'
                ? 'bg-emerald-600 text-white shadow-sm'
                : 'bg-slate-800/80 text-slate-300 hover:bg-slate-800'
            }`}
          >
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
            <span>نشر متاح ({groups.filter((g) => g.permissionStatus === 'can_post').length})</span>
          </button>

          <button
            id="filter-tab-slowmode"
            type="button"
            onClick={() => setActiveFilter('slowmode')}
            className={`flex items-center gap-1 px-3 py-2 rounded-xl font-semibold transition-all whitespace-nowrap cursor-pointer ${
              activeFilter === 'slowmode'
                ? 'bg-amber-600 text-white shadow-sm'
                : 'bg-slate-800/80 text-slate-300 hover:bg-slate-800'
            }`}
          >
            <Clock className="w-3.5 h-3.5 text-amber-400" />
            <span>وضع البطء ({groups.filter((g) => g.permissionStatus === 'slowmode').length})</span>
          </button>

          <button
            id="filter-tab-restricted"
            type="button"
            onClick={() => setActiveFilter('restricted')}
            className={`flex items-center gap-1 px-3 py-2 rounded-xl font-semibold transition-all whitespace-nowrap cursor-pointer ${
              activeFilter === 'restricted'
                ? 'bg-rose-600 text-white shadow-sm'
                : 'bg-slate-800/80 text-slate-300 hover:bg-slate-800'
            }`}
          >
            <ShieldAlert className="w-3.5 h-3.5 text-rose-400" />
            <span>مقيدة ({groups.filter((g) => g.permissionStatus === 'admin_only' || g.permissionStatus === 'banned').length})</span>
          </button>

          <button
            id="filter-tab-degraded"
            type="button"
            onClick={() => setActiveFilter('degraded')}
            className={`flex items-center gap-1 px-3 py-2 rounded-xl font-semibold transition-all whitespace-nowrap cursor-pointer ${
              activeFilter === 'degraded'
                ? 'bg-purple-600 text-white shadow-sm'
                : 'bg-slate-800/80 text-slate-300 hover:bg-slate-800'
            }`}
          >
            <WifiOff className="w-3.5 h-3.5 text-purple-400" />
            <span>ضعيفة/منفصلة ({groups.filter((g) => g.connectionHealth === 'degraded' || g.connectionHealth === 'disconnected').length})</span>
          </button>
        </div>

        {/* Action: Add Group Button */}
        <button
          id="add-new-group-btn"
          type="button"
          onClick={() => setIsAddModalOpen(true)}
          className="flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white text-xs font-bold shadow-md shadow-indigo-600/20 transition-all shrink-0 cursor-pointer"
        >
          <Plus className="w-4 h-4" />
          <span>إضافة مجموعة</span>
        </button>
      </div>

      {/* Bulk Action Strip (Visible when items are selected) */}
      {selectedIds.size > 0 && (
        <div className="bg-indigo-950/80 border border-indigo-700/60 rounded-2xl p-3 px-4 flex flex-wrap items-center justify-between gap-3 text-xs text-indigo-100 animate-fade-in shadow-lg">
          <div className="flex items-center gap-2">
            <span className="font-bold bg-indigo-600 text-white px-2 py-0.5 rounded-lg">
              {selectedIds.size}
            </span>
            <span>مجموعة محددة من أصل {filteredGroups.length}</span>
          </div>

          <div className="flex items-center gap-2 flex-wrap">
            <button
              id="bulk-ping-btn"
              type="button"
              onClick={handleBulkPing}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white font-semibold transition-colors cursor-pointer"
            >
              <Activity className="w-3.5 h-3.5" />
              <span>فحص Ping للمحدد</span>
            </button>

            <button
              id="bulk-enable-monitor-btn"
              type="button"
              onClick={() => handleBulkToggleMonitor(true)}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-purple-600/60 hover:bg-purple-600 text-white font-semibold transition-colors cursor-pointer"
            >
              <Radio className="w-3.5 h-3.5" />
              <span>تفعيل المراقبة</span>
            </button>

            <button
              id="bulk-disable-monitor-btn"
              type="button"
              onClick={() => handleBulkToggleMonitor(false)}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold transition-colors cursor-pointer"
            >
              <span>إيقاف المراقبة</span>
            </button>

            <button
              id="bulk-delete-btn"
              type="button"
              onClick={handleBulkDelete}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-rose-600/40 hover:bg-rose-600 text-rose-200 hover:text-white font-semibold transition-colors cursor-pointer"
            >
              <Trash2 className="w-3.5 h-3.5" />
              <span>حذف المحدد</span>
            </button>
          </div>
        </div>
      )}

      {/* List Header / Sorting & Selection Toggle */}
      <div className="flex items-center justify-between px-2 text-xs text-slate-400">
        <div className="flex items-center gap-2">
          <button
            id="toggle-select-all-btn"
            type="button"
            onClick={handleToggleSelectAll}
            className="flex items-center gap-1.5 hover:text-white transition-colors cursor-pointer"
          >
            {selectedIds.size === filteredGroups.length && filteredGroups.length > 0 ? (
              <CheckSquare className="w-4 h-4 text-indigo-400" />
            ) : (
              <Square className="w-4 h-4 text-slate-500" />
            )}
            <span>تحديد الكل ({filteredGroups.length})</span>
          </button>
        </div>

        {/* Sorting Dropdown */}
        <div className="flex items-center gap-2">
          <ArrowUpDown className="w-3.5 h-3.5 text-slate-500" />
          <span>ترتيب حسب:</span>
          <select
            id="sort-by-select"
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as any)}
            className="bg-slate-800 border border-slate-700 text-slate-300 rounded-lg px-2 py-1 focus:outline-none focus:border-indigo-500 cursor-pointer text-xs"
          >
            <option value="latency">زمن الاستجابة (Ping)</option>
            <option value="members">عدد الأعضاء</option>
            <option value="title">الاسم الأبجدي</option>
            <option value="status">حالة الصلاحيات</option>
          </select>
          <button
            id="toggle-sort-order-btn"
            type="button"
            onClick={() => setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc')}
            className="p-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors cursor-pointer"
            title={sortOrder === 'asc' ? 'تصاعدي' : 'تنازلي'}
          >
            {sortOrder === 'asc' ? '↑' : '↓'}
          </button>
        </div>
      </div>

      {/* The Groups List */}
      <div className="space-y-3">
        {filteredGroups.length > 0 ? (
          filteredGroups.map((group) => (
            <TelegramGroupCard
              key={group.id}
              group={group}
              isSelected={selectedIds.has(group.id)}
              onToggleSelect={handleToggleSelect}
              onPingCheck={handlePingGroup}
              onOpenDetails={(g) => setDetailedGroup(g)}
              onToggleMonitoring={handleToggleMonitoring}
            />
          ))
        ) : (
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-12 text-center text-slate-400 flex flex-col items-center justify-center">
            <Layers className="w-10 h-10 text-slate-600 mb-3" />
            <h3 className="text-sm font-bold text-white mb-1">لا توجد مجموعات تطابق معايير البحث والفلترة</h3>
            <p className="text-xs text-slate-400 mb-4 max-w-sm">
              جرب تغيير كلمة البحث أو اختيار تبويب فلترة آخر، أو أضف مجموعة تيليجرام جديدة.
            </p>
            <button
              type="button"
              onClick={() => {
                setSearchQuery('');
                setActiveFilter('all');
              }}
              className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold transition-colors cursor-pointer"
            >
              إعادة ضبط الفلاتر
            </button>
          </div>
        )}
      </div>

      {/* Modals */}
      <GroupDetailsModal
        group={detailedGroup}
        onClose={() => setDetailedGroup(null)}
        onPing={handlePingGroup}
        onToggleMonitoring={handleToggleMonitoring}
      />

      <AddGroupModal
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        existingGroups={groups}
        onAddGroup={handleAddGroup}
      />
    </div>
  );
};
