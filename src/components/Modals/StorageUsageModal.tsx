import React, { useState, useEffect } from 'react';
import {
  X,
  HardDrive,
  Trash2,
  PieChart,
  Wifi,
  Smartphone,
  Globe,
  CheckCircle2,
  Sparkles,
  ChevronRight,
  RefreshCw,
  Clock,
  ShieldCheck,
  Film,
  Music,
  FileText,
  Image as ImageIcon,
  Layers,
} from 'lucide-react';
import { useTelegram } from '../../context/TelegramContext';
import { StorageCategoryStats, StorageStats } from '../../types';
import confetti from 'canvas-confetti';

interface StorageUsageModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const StorageUsageModal: React.FC<StorageUsageModalProps> = ({
  isOpen,
  onClose,
}) => {
  const { chats, settings, showToast } = useTelegram();
  const [activeTab, setActiveTab] = useState<'storage' | 'network'>('storage');
  const [retentionPeriod, setRetentionPeriod] = useState<'3days' | '1week' | '1month' | 'forever'>('1month');
  const [selectedCategories, setSelectedCategories] = useState<string[]>([
    'photos',
    'videos',
    'documents',
    'audio',
    'stickers',
    'cache_db',
  ]);

  const [categories, setCategories] = useState<StorageCategoryStats[]>([
    { category: 'videos', label: 'الفيديوهات والرسائل المرئية', sizeBytes: 342 * 1024 * 1024, itemCount: 48, color: '#2481cc' },
    { category: 'photos', label: 'الصور والوسائط الملتقطة', sizeBytes: 185 * 1024 * 1024, itemCount: 210, color: '#38bdf8' },
    { category: 'documents', label: 'المستندات والملفات (PDF/Zip)', sizeBytes: 120 * 1024 * 1024, itemCount: 32, color: '#f59e0b' },
    { category: 'audio', label: 'التسجيلات الصوتية والموسيقى', sizeBytes: 65 * 1024 * 1024, itemCount: 95, color: '#10b981' },
    { category: 'stickers', label: 'ملصقات Lottie والإيموجي التفاعلي', sizeBytes: 42 * 1024 * 1024, itemCount: 140, color: '#a855f7' },
    { category: 'cache_db', label: 'قاعدة بيانات MTProto المحلية (Dexie)', sizeBytes: 18 * 1024 * 1024, itemCount: 1250, color: '#ec4899' },
  ]);

  const isRtl = settings.language === 'ar';

  if (!isOpen) return null;

  const totalCacheBytes = categories.reduce((sum, c) => sum + c.sizeBytes, 0);

  const formatSize = (bytes: number) => {
    if (bytes >= 1024 * 1024 * 1024) {
      return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
    }
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const handleClearCache = () => {
    confetti({ particleCount: 50, spread: 70, origin: { y: 0.6 } });
    setCategories((prev) =>
      prev.map((c) =>
        selectedCategories.includes(c.category)
          ? { ...c, sizeBytes: c.category === 'cache_db' ? 2 * 1024 * 1024 : 0, itemCount: 0 }
          : c
      )
    );
    showToast(
      isRtl
        ? 'تم تنظيف الذاكرة المؤقتة بنجاح وتوفير المساحة'
        : 'Cache cleared successfully',
      '🧹'
    );
  };

  const toggleCategorySelection = (cat: string) => {
    setSelectedCategories((prev) =>
      prev.includes(cat) ? prev.filter((c) => c !== cat) : [...prev, cat]
    );
  };

  return (
    <div
      id="tg-storage-usage-modal"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-md animate-in fade-in duration-200"
      onClick={onClose}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-xl rounded-3xl overflow-hidden shadow-2xl border flex flex-col max-h-[90vh] animate-in zoom-in-95 duration-200"
        style={{
          backgroundColor: 'var(--tg-theme-surface, #17212b)',
          borderColor: 'var(--tg-theme-border, rgba(255,255,255,0.1))',
          color: 'var(--tg-theme-bubble-in-text, #ffffff)',
        }}
      >
        {/* Header */}
        <div className="p-4.5 border-b border-white/10 flex items-center justify-between bg-black/20">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-[#2481cc]/20 text-[#2481cc] flex items-center justify-center">
              <HardDrive className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-white">
                {isRtl ? 'استهلاك الذاكرة والشبكة (Telegram X)' : 'Storage & Network Usage'}
              </h2>
              <p className="text-xs text-gray-400">
                {isRtl ? 'إدارة التخزين المؤقت وتحليل البيانات' : 'Manage cache & data retention'}
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

        {/* Tab Toggle */}
        <div className="flex border-b border-white/10 bg-black/10 text-xs font-bold">
          <button
            onClick={() => setActiveTab('storage')}
            className={`flex-1 py-3 text-center flex items-center justify-center gap-2 border-b-2 transition-colors ${
              activeTab === 'storage'
                ? 'border-[#2481cc] text-[#2481cc]'
                : 'border-transparent text-gray-400 hover:text-white'
            }`}
          >
            <HardDrive className="w-4 h-4" />
            <span>{isRtl ? 'استهلاك التخزين' : 'Storage Usage'}</span>
          </button>
          <button
            onClick={() => setActiveTab('network')}
            className={`flex-1 py-3 text-center flex items-center justify-center gap-2 border-b-2 transition-colors ${
              activeTab === 'network'
                ? 'border-[#2481cc] text-[#2481cc]'
                : 'border-transparent text-gray-400 hover:text-white'
            }`}
          >
            <Wifi className="w-4 h-4" />
            <span>{isRtl ? 'بيانات الشبكة' : 'Network Usage'}</span>
          </button>
        </div>

        {/* Modal Body */}
        <div className="flex-1 overflow-y-auto p-5 space-y-6 no-scrollbar">
          {activeTab === 'storage' ? (
            <>
              {/* Storage Overview Bar */}
              <div className="p-4 rounded-2xl bg-black/20 border border-white/10 space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-gray-300">
                    {isRtl ? 'إجمالي الذاكرة المؤقتة المستخدمة' : 'Total Cache in Use'}
                  </span>
                  <span className="text-base font-extrabold text-[#2481cc] font-mono">
                    {formatSize(totalCacheBytes)}
                  </span>
                </div>

                {/* Visual Segmented Progress Bar */}
                <div className="h-3 w-full rounded-full bg-white/10 overflow-hidden flex">
                  {categories.map((cat) => {
                    const pct = totalCacheBytes > 0 ? (cat.sizeBytes / totalCacheBytes) * 100 : 0;
                    return (
                      <div
                        key={cat.category}
                        style={{ width: `${pct}%`, backgroundColor: cat.color }}
                        title={`${cat.label}: ${formatSize(cat.sizeBytes)}`}
                        className="h-full transition-all duration-300"
                      />
                    );
                  })}
                </div>

                {/* Keep Media Retention Setting */}
                <div className="pt-3 border-t border-white/10 space-y-2">
                  <div className="flex items-center justify-between text-xs">
                    <span className="text-gray-300 font-bold flex items-center gap-1.5">
                      <Clock className="w-4 h-4 text-sky-400" />
                      {isRtl ? 'الاحتفاظ بالوسائط (Keep Media)' : 'Keep Media Period'}
                    </span>
                    <span className="text-sky-400 font-bold">
                      {retentionPeriod === '3days'
                        ? isRtl ? '3 أيام' : '3 Days'
                        : retentionPeriod === '1week'
                        ? isRtl ? 'أسبوع واحد' : '1 Week'
                        : retentionPeriod === '1month'
                        ? isRtl ? 'شهر واحد' : '1 Month'
                        : isRtl ? 'إلى الأبد' : 'Forever'}
                    </span>
                  </div>

                  <div className="grid grid-cols-4 gap-1.5 text-[11px] font-bold">
                    {(['3days', '1week', '1month', 'forever'] as const).map((period) => (
                      <button
                        key={period}
                        onClick={() => setRetentionPeriod(period)}
                        className={`py-1.5 rounded-xl border transition-all ${
                          retentionPeriod === period
                            ? 'bg-[#2481cc] text-white border-[#2481cc]'
                            : 'bg-white/5 border-white/10 text-gray-400 hover:text-white'
                        }`}
                      >
                        {period === '3days'
                          ? '3d'
                          : period === '1week'
                          ? '1w'
                          : period === '1month'
                          ? '1m'
                          : '∞'}
                      </button>
                    ))}
                  </div>
                </div>
              </div>

              {/* Categories Checklist */}
              <div className="space-y-2">
                <h3 className="text-xs font-bold text-gray-400 px-1">
                  {isRtl ? 'تصنيفات الملفات المؤقتة:' : 'Cache Categories:'}
                </h3>
                {categories.map((cat) => {
                  const isChecked = selectedCategories.includes(cat.category);
                  return (
                    <div
                      key={cat.category}
                      onClick={() => toggleCategorySelection(cat.category)}
                      className="p-3 rounded-2xl bg-black/15 hover:bg-black/30 border border-white/5 flex items-center justify-between cursor-pointer transition-colors"
                    >
                      <div className="flex items-center gap-3">
                        <div
                          className="w-3.5 h-3.5 rounded-full shrink-0"
                          style={{ backgroundColor: cat.color }}
                        />
                        <div>
                          <div className="text-xs font-bold text-white">
                            {isRtl ? cat.label : cat.category.toUpperCase()}
                          </div>
                          <div className="text-[10px] text-gray-400">
                            {cat.itemCount} {isRtl ? 'عنصر' : 'items'}
                          </div>
                        </div>
                      </div>

                      <div className="flex items-center gap-3">
                        <span className="font-mono text-xs font-bold text-gray-200">
                          {formatSize(cat.sizeBytes)}
                        </span>
                        <input
                          type="checkbox"
                          checked={isChecked}
                          onChange={() => {}}
                          className="w-4 h-4 rounded text-[#2481cc] bg-white/10 border-white/20 focus:ring-0 cursor-pointer"
                        />
                      </div>
                    </div>
                  );
                })}
              </div>

              {/* Chat Breakdown Preview */}
              <div className="space-y-2">
                <h3 className="text-xs font-bold text-gray-400 px-1">
                  {isRtl ? 'المحادثات الأكثر استهلاكاً:' : 'Top Storage Chats:'}
                </h3>
                <div className="space-y-1.5">
                  {chats.slice(0, 4).map((chat, idx) => (
                    <div
                      key={chat.id}
                      className="p-2.5 rounded-xl bg-white/5 flex items-center justify-between text-xs"
                    >
                      <span className="font-medium text-white truncate max-w-[200px]">
                        {chat.title}
                      </span>
                      <span className="font-mono text-gray-400">
                        {((4 - idx) * 42.5).toFixed(1)} MB
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            </>
          ) : (
            /* Network Data Usage Tab */
            <div className="space-y-4">
              <div className="p-4 rounded-2xl bg-black/20 border border-white/10 space-y-4">
                <div className="flex items-center justify-between pb-3 border-b border-white/10">
                  <div className="flex items-center gap-2 text-xs font-bold text-sky-400">
                    <Wifi className="w-4 h-4" />
                    <span>{isRtl ? 'بيانات شبكة Wi-Fi' : 'Wi-Fi Data'}</span>
                  </div>
                  <span className="font-mono font-bold text-xs text-white">
                    1.42 GB
                  </span>
                </div>

                <div className="flex items-center justify-between pb-3 border-b border-white/10">
                  <div className="flex items-center gap-2 text-xs font-bold text-emerald-400">
                    <Smartphone className="w-4 h-4" />
                    <span>{isRtl ? 'بيانات الهاتف المحمول (Cellular)' : 'Mobile Cellular Data'}</span>
                  </div>
                  <span className="font-mono font-bold text-xs text-white">
                    480.6 MB
                  </span>
                </div>

                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 text-xs font-bold text-amber-400">
                    <Globe className="w-4 h-4" />
                    <span>{isRtl ? 'بيانات التجوال الدولي (Roaming)' : 'Roaming Data'}</span>
                  </div>
                  <span className="font-mono font-bold text-xs text-white">
                    0.0 KB
                  </span>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Footer Actions */}
        {activeTab === 'storage' && (
          <div className="p-4 border-t border-white/10 bg-black/20 flex items-center justify-between gap-3">
            <span className="text-xs text-gray-400">
              {isRtl ? 'سيتم تحرير المساحة المحددة فورياً' : 'Instant cache cleaning'}
            </span>

            <button
              onClick={handleClearCache}
              disabled={selectedCategories.length === 0 || totalCacheBytes === 0}
              className="px-5 py-2.5 rounded-2xl bg-[#2481cc] hover:bg-[#1c6fad] disabled:opacity-50 text-white text-xs font-bold shadow-lg transition-transform active:scale-95 flex items-center gap-2"
            >
              <Trash2 className="w-4 h-4" />
              <span>{isRtl ? 'تنظيف الذاكرة المؤقتة' : 'Clear Selected Cache'}</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
