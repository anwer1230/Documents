import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  UserPlus,
  Link as LinkIcon,
  Globe,
  Search,
  CheckCircle2,
  XCircle,
  AlertCircle,
  Play,
  Square,
  X,
  Loader2,
  Clock,
  Trash2,
  Plus,
  CheckSquare,
  Square as SquareBox,
  Radio,
  Lock,
  Sparkles,
  Filter,
  Check,
  RefreshCw,
} from 'lucide-react';
import { useTelegram } from '../../context/TelegramContext';
import { notificationsService } from '../../core/NotificationsService';
import { AutoJoinerTask } from '../../types';

interface LinkItem {
  id: string;
  url: string;
  type: 'public' | 'private';
  selected: boolean;
}

export const AutoJoinerModal: React.FC = () => {
  const { activeModal, setActiveModal, settings, showToast } = useTelegram();
  const isArabic = settings?.language === 'ar';

  const [rawText, setRawText] = useState(
    'انضم إلى مجتمعنا التقني:\nhttps://t.me/tech_innovators_hub\nأو عبر الرابط الخاص: https://t.me/+Vip_Channel_2026\nتابعنا أيضاً على @flutter_devs_group\nhttps://t.me/ai_developers_cloud'
  );
  const [singleLinkInput, setSingleLinkInput] = useState('');
  const [linkItems, setLinkItems] = useState<LinkItem[]>([]);
  const [tasks, setTasks] = useState<AutoJoinerTask[]>([]);
  const [isProcessing, setIsProcessing] = useState(false);
  const [fetchWebLinks, setFetchWebLinks] = useState(false);
  const [searchByName, setSearchByName] = useState(false);
  const [progress, setProgress] = useState({ processed: 0, total: 0 });
  const [activeTab, setActiveTab] = useState<'selection' | 'raw_text'>('selection');

  // Helper to determine link type
  const getLinkType = (url: string): 'public' | 'private' => {
    return url.includes('+') || url.includes('joinchat') ? 'private' : 'public';
  };

  // Sync extracted links from rawText when initialized or changed
  const syncLinksFromText = useCallback((text: string) => {
    const extracted = notificationsService.extractLinksFromRawText(text);
    setLinkItems((prev) => {
      const existingMap = new Map(prev.map((item) => [item.url, item.selected]));
      return extracted.map((url, idx) => ({
        id: `link_${idx}_${url}`,
        url,
        type: getLinkType(url),
        // Preserve prior selection state if known, default to true for new links
        selected: existingMap.has(url) ? (existingMap.get(url) ?? true) : true,
      }));
    });
  }, []);

  // Initialize links list on mount
  useEffect(() => {
    syncLinksFromText(rawText);
  }, []);

  // Subscribe to background task updates
  useEffect(() => {
    const unsub = notificationsService.subscribe(() => {
      setTasks([...notificationsService.getAutoJoinTasks()]);
    });
    setTasks([...notificationsService.getAutoJoinTasks()]);
    return () => unsub();
  }, []);

  // Quick statistics
  const selectedCount = useMemo(() => linkItems.filter((i) => i.selected).length, [linkItems]);
  const totalCount = linkItems.length;

  const taskStatusMap = useMemo(() => {
    const map = new Map<string, AutoJoinerTask>();
    tasks.forEach((t) => map.set(t.url, t));
    return map;
  }, [tasks]);

  const stats = useMemo(() => {
    let joined = 0;
    let failed = 0;
    let joining = 0;
    let pending = 0;

    tasks.forEach((t) => {
      if (t.status === 'joined') joined++;
      else if (t.status === 'invalid' || t.status === 'banned' || t.status === 'rate_limited') failed++;
      else if (t.status === 'joining') joining++;
      else if (t.status === 'pending') pending++;
    });

    return { joined, failed, joining, pending };
  }, [tasks]);

  // Current joining channel URL for progress bar subtitle
  const currentJoiningTask = useMemo(() => {
    return tasks.find((t) => t.status === 'joining');
  }, [tasks]);

  if (activeModal !== ('auto-joiner' as any)) return null;

  // Toggle individual link selection
  const handleToggleSelect = (id: string) => {
    if (isProcessing) return;
    setLinkItems((prev) =>
      prev.map((item) => (item.id === id ? { ...item, selected: !item.selected } : item))
    );
  };

  // Select all links
  const handleSelectAll = () => {
    if (isProcessing) return;
    setLinkItems((prev) => prev.map((item) => ({ ...item, selected: true })));
    showToast(isArabic ? 'تم تحديد جميع الروابط' : 'Selected all invite links', '✓');
  };

  // Deselect all links
  const handleDeselectAll = () => {
    if (isProcessing) return;
    setLinkItems((prev) => prev.map((item) => ({ ...item, selected: false })));
    showToast(isArabic ? 'تم إلغاء تحديد الروابط' : 'Deselected all links', 'ℹ️');
  };

  // Filter selection by type
  const handleSelectByType = (type: 'public' | 'private') => {
    if (isProcessing) return;
    setLinkItems((prev) =>
      prev.map((item) => ({
        ...item,
        selected: item.type === type,
      }))
    );
    showToast(
      isArabic
        ? type === 'public'
          ? 'تم تحديد الروابط العامة فقط'
          : 'تم تحديد الروابط الخاصة فقط'
        : type === 'public'
        ? 'Selected public channels only'
        : 'Selected private invite links only',
      '🎯'
    );
  };

  // Delete a link from the list
  const handleDeleteLink = (id: string) => {
    if (isProcessing) return;
    setLinkItems((prev) => prev.filter((item) => item.id !== id));
  };

  // Add single link manually
  const handleAddSingleLink = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    const trimmed = singleLinkInput.trim();
    if (!trimmed) return;

    const extracted = notificationsService.extractLinksFromRawText(trimmed);
    const targetUrl = extracted.length > 0 ? extracted[0] : trimmed;

    if (linkItems.some((item) => item.url === targetUrl)) {
      showToast(isArabic ? 'هذا الرابط مضاف مسبقاً' : 'Link already exists in list', '⚠️');
      return;
    }

    const newItem: LinkItem = {
      id: `link_${Date.now()}_${Math.random().toString(36).substring(7)}`,
      url: targetUrl,
      type: getLinkType(targetUrl),
      selected: true,
    };

    setLinkItems((prev) => [newItem, ...prev]);
    setSingleLinkInput('');
    showToast(isArabic ? 'تمت إضافة الرابط بنجاح' : 'Link added to list', '✨');
  };

  // Start Bulk Join for selected links
  const handleStartJoin = async () => {
    const selectedUrls = linkItems.filter((i) => i.selected).map((i) => i.url);

    if (selectedUrls.length === 0) {
      showToast(
        isArabic
          ? 'يرجى تحديد رابط قناة أو مجموعة واحدة على الأقل للبدء'
          : 'Please select at least one channel or invite link to join',
        '⚠️'
      );
      return;
    }

    setIsProcessing(true);
    setProgress({ processed: 0, total: selectedUrls.length });

    fetch('/api/auto_join/advanced', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ links: selectedUrls, fetch_external: fetchWebLinks }),
    }).catch(() => {});

    await notificationsService.startAutoJoinTasks(selectedUrls, (processed, total) => {
      setProgress({ processed, total });
    });

    setIsProcessing(false);
    showToast(
      isArabic
        ? `اكتملت مهمة الانضمام الجماعي لـ ${selectedUrls.length} رابط 🎉`
        : `Bulk join completed for ${selectedUrls.length} links 🎉`,
      '✨'
    );
  };

  // Stop ongoing join operation
  const handleStopJoin = () => {
    notificationsService.stopAutoJoin();
    fetch('/api/auto_join/stop', { method: 'POST' }).catch(() => {});
    setIsProcessing(false);
    showToast(isArabic ? 'تم إيقاف عملية الانضمام الجماعي ⏹️' : 'Bulk join stopped ⏹️', '⚠️');
  };

  // Calculate percentage
  const progressPercent = progress.total > 0 ? Math.round((progress.processed / progress.total) * 100) : 0;

  return (
    <div
      id="modal-auto-joiner-activity"
      className="fixed inset-0 z-[9999] flex items-center justify-center p-3 sm:p-4 bg-black/85 backdrop-blur-md select-none"
      dir={isArabic ? 'rtl' : 'ltr'}
    >
      <div
        className="w-full max-w-2xl text-[#e8eaf6] rounded-3xl shadow-2xl overflow-hidden border border-emerald-500/30 my-auto animate-in zoom-in-95 duration-200 max-h-[92vh] flex flex-col"
        style={{
          background: 'linear-gradient(145deg, #071912, #0d2a1f, #040e0a)',
        }}
      >
        {/* Header */}
        <div className="p-4 sm:p-5 border-b border-white/10 flex items-center justify-between bg-black/40 shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center border border-emerald-400/30 shadow-inner">
              <UserPlus className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-bold text-sm text-white flex items-center gap-2">
                <span>{isArabic ? 'الانضمام التلقائي الجماعي' : 'Bulk Channel Auto-Joiner'}</span>
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 font-mono border border-emerald-500/30">
                  MTProto 2.0
                </span>
              </h3>
              <p className="text-[11px] text-emerald-300/80">
                {isArabic
                  ? 'تحديد روابط قنوات متعددة دفعة واحدة والانضمام مع شريط تقدم مباشر'
                  : 'Multi-select Telegram channel invite links for bulk joining with live progress'}
              </p>
            </div>
          </div>
          <button
            onClick={() => setActiveModal('none')}
            className="p-2 rounded-xl text-gray-400 hover:text-white hover:bg-white/10 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Tab Switcher & Quick Add Bar */}
        <div className="px-4 sm:px-6 pt-4 pb-2 border-b border-white/5 bg-black/20 flex flex-wrap items-center justify-between gap-3 shrink-0">
          <div className="flex items-center gap-1.5 p-1 rounded-xl bg-black/40 border border-white/5">
            <button
              type="button"
              onClick={() => setActiveTab('selection')}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all flex items-center gap-1.5 ${
                activeTab === 'selection'
                  ? 'bg-emerald-600 text-white shadow-md'
                  : 'text-gray-400 hover:text-gray-200 hover:bg-white/5'
              }`}
            >
              <CheckSquare className="w-3.5 h-3.5" />
              <span>{isArabic ? 'تحديد الروابط' : 'Select Links'}</span>
              <span className="text-[10px] px-1.5 py-0.2 rounded-full bg-white/20 text-white font-mono">
                {selectedCount}/{totalCount}
              </span>
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('raw_text')}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all flex items-center gap-1.5 ${
                activeTab === 'raw_text'
                  ? 'bg-emerald-600 text-white shadow-md'
                  : 'text-gray-400 hover:text-gray-200 hover:bg-white/5'
              }`}
            >
              <Globe className="w-3.5 h-3.5" />
              <span>{isArabic ? 'استخراج من نص' : 'Raw Text Extract'}</span>
            </button>
          </div>

          {/* Quick single link adder */}
          <form onSubmit={handleAddSingleLink} className="flex-1 max-w-sm flex items-center gap-1.5">
            <div className="relative flex-1">
              <input
                type="text"
                value={singleLinkInput}
                onChange={(e) => setSingleLinkInput(e.target.value)}
                placeholder={isArabic ? 'أضف رابطاً (@handle أو t.me/+)...' : 'Add invite link (@channel, t.me/+)...'}
                className="w-full bg-black/50 border border-white/10 rounded-xl px-3 py-1.5 text-xs text-white placeholder-gray-500 focus:outline-none focus:border-emerald-400 transition-colors font-mono"
              />
            </div>
            <button
              type="submit"
              disabled={!singleLinkInput.trim() || isProcessing}
              className="px-2.5 py-1.5 rounded-xl bg-emerald-600/80 hover:bg-emerald-500 disabled:opacity-50 text-white text-xs font-semibold transition-all flex items-center gap-1 shrink-0"
              title={isArabic ? 'إضافة الرابط إلى القائمة' : 'Add link to list'}
            >
              <Plus className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">{isArabic ? 'إضافة' : 'Add'}</span>
            </button>
          </form>
        </div>

        {/* Modal Scrollable Body */}
        <div className="p-4 sm:p-6 overflow-y-auto space-y-4 flex-1">
          {/* TAB 1: SELECTION & BULK MANAGEMENT */}
          {activeTab === 'selection' ? (
            <div className="space-y-3">
              {/* Bulk Selection Actions Bar */}
              <div className="p-3 rounded-2xl bg-black/30 border border-white/5 flex flex-wrap items-center justify-between gap-2 text-xs">
                <div className="flex items-center gap-2">
                  <span className="text-gray-300 font-semibold">
                    {isArabic ? 'الروابط المكتشفة:' : 'Discovered Links:'}
                  </span>
                  <span className="px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 font-mono font-bold text-[11px] border border-emerald-500/30">
                    {selectedCount} {isArabic ? 'محدد' : 'selected'} / {totalCount}
                  </span>
                </div>

                {/* Bulk Select Control Buttons */}
                <div className="flex items-center gap-1.5 flex-wrap">
                  <button
                    type="button"
                    onClick={handleSelectAll}
                    disabled={isProcessing || totalCount === 0}
                    className="px-2.5 py-1 rounded-lg bg-white/10 hover:bg-white/20 active:scale-95 disabled:opacity-40 text-gray-200 text-[11px] font-medium transition-all"
                  >
                    {isArabic ? 'تحديد الكل' : 'Select All'}
                  </button>
                  <button
                    type="button"
                    onClick={handleDeselectAll}
                    disabled={isProcessing || selectedCount === 0}
                    className="px-2.5 py-1 rounded-lg bg-white/10 hover:bg-white/20 active:scale-95 disabled:opacity-40 text-gray-200 text-[11px] font-medium transition-all"
                  >
                    {isArabic ? 'إلغاء التحديد' : 'Deselect All'}
                  </button>
                  <button
                    type="button"
                    onClick={() => handleSelectByType('public')}
                    disabled={isProcessing || totalCount === 0}
                    className="px-2 py-1 rounded-lg bg-sky-500/15 hover:bg-sky-500/25 active:scale-95 disabled:opacity-40 text-sky-300 text-[11px] font-medium transition-all flex items-center gap-1"
                    title={isArabic ? 'تحديد الروابط العامة فقط' : 'Select public channels only'}
                  >
                    <Radio className="w-3 h-3" />
                    <span>{isArabic ? 'العامة' : 'Public'}</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => handleSelectByType('private')}
                    disabled={isProcessing || totalCount === 0}
                    className="px-2 py-1 rounded-lg bg-amber-500/15 hover:bg-amber-500/25 active:scale-95 disabled:opacity-40 text-amber-300 text-[11px] font-medium transition-all flex items-center gap-1"
                    title={isArabic ? 'تحديد الروابط الخاصة فقط (+)' : 'Select private invite links only'}
                  >
                    <Lock className="w-3 h-3" />
                    <span>{isArabic ? 'الخاصة' : 'Private'}</span>
                  </button>
                </div>
              </div>

              {/* Selectable Links List */}
              {linkItems.length === 0 ? (
                <div className="p-8 rounded-2xl bg-black/20 border border-white/5 text-center text-xs text-gray-400 space-y-2">
                  <LinkIcon className="w-8 h-8 text-gray-600 mx-auto opacity-50" />
                  <p>{isArabic ? 'لم يتم العثور على أي روابط في القائمة' : 'No channel invite links found'}</p>
                  <button
                    type="button"
                    onClick={() => setActiveTab('raw_text')}
                    className="px-3 py-1.5 rounded-xl bg-emerald-600/30 hover:bg-emerald-600/50 text-emerald-300 text-xs font-semibold transition-all"
                  >
                    {isArabic ? 'لصق نص لاستخراج الروابط' : 'Paste text to extract links'}
                  </button>
                </div>
              ) : (
                <div className="space-y-1.5 max-h-64 overflow-y-auto pr-1">
                  {linkItems.map((item) => {
                    const task = taskStatusMap.get(item.url);
                    const status = task?.status;

                    return (
                      <div
                        key={item.id}
                        onClick={() => handleToggleSelect(item.id)}
                        className={`p-3 rounded-2xl border transition-all cursor-pointer flex items-center justify-between gap-2.5 text-xs ${
                          item.selected
                            ? 'bg-emerald-950/40 border-emerald-500/40 text-white shadow-sm'
                            : 'bg-black/40 border-white/5 text-gray-400 hover:bg-white/5'
                        }`}
                      >
                        {/* Checkbox + Icon + Link URL */}
                        <div className="flex items-center gap-2.5 min-w-0 overflow-hidden">
                          <input
                            type="checkbox"
                            checked={item.selected}
                            disabled={isProcessing}
                            onChange={() => {}} // Handled by container click
                            className="w-4 h-4 rounded accent-emerald-500 cursor-pointer shrink-0"
                          />

                          {/* Link Type Badge */}
                          {item.type === 'private' ? (
                            <span
                              className="w-6 h-6 rounded-lg bg-amber-500/15 border border-amber-500/30 text-amber-300 flex items-center justify-center shrink-0"
                              title={isArabic ? 'رابط دعوة خاص' : 'Private invite'}
                            >
                              <Lock className="w-3.5 h-3.5" />
                            </span>
                          ) : (
                            <span
                              className="w-6 h-6 rounded-lg bg-sky-500/15 border border-sky-500/30 text-sky-300 flex items-center justify-center shrink-0"
                              title={isArabic ? 'قناة أو معرف عام' : 'Public channel'}
                            >
                              <Radio className="w-3.5 h-3.5" />
                            </span>
                          )}

                          <span className="font-mono text-xs truncate dir-ltr select-text">
                            {item.url}
                          </span>
                        </div>

                        {/* Status / Task Feedback + Delete Button */}
                        <div className="flex items-center gap-2 shrink-0">
                          {status === 'joined' && (
                            <span className="px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 text-[10px] font-bold flex items-center gap-1 border border-emerald-500/30">
                              <CheckCircle2 className="w-3 h-3" />
                              <span>{isArabic ? 'تم الانضمام' : 'Joined'}</span>
                            </span>
                          )}

                          {status === 'joining' && (
                            <span className="px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-300 text-[10px] font-bold flex items-center gap-1 border border-amber-500/30">
                              <Loader2 className="w-3 h-3 animate-spin" />
                              <span>{isArabic ? 'جاري الانضمام...' : 'Joining...'}</span>
                            </span>
                          )}

                          {status === 'invalid' && (
                            <span className="px-2 py-0.5 rounded-full bg-rose-500/20 text-rose-300 text-[10px] font-bold flex items-center gap-1 border border-rose-500/30">
                              <XCircle className="w-3 h-3" />
                              <span>{task?.errorReason || (isArabic ? 'فشل الانضمام' : 'Failed')}</span>
                            </span>
                          )}

                          {!status && (
                            <span className="text-[10px] text-gray-500 font-mono">
                              {item.type === 'private' ? (isArabic ? 'خاص' : 'Private') : (isArabic ? 'عام' : 'Public')}
                            </span>
                          )}

                          {/* Delete Item */}
                          {!isProcessing && (
                            <button
                              type="button"
                              onClick={(e) => {
                                e.stopPropagation();
                                handleDeleteLink(item.id);
                              }}
                              className="p-1 rounded-lg text-gray-500 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
                              title={isArabic ? 'إزالة من القائمة' : 'Remove link'}
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          ) : (
            /* TAB 2: RAW TEXT EXTRACTOR */
            <div className="space-y-3">
              <div className="space-y-1.5">
                <label className="text-xs font-bold text-gray-200 flex items-center justify-between">
                  <span>{isArabic ? 'الصق النص الكامل لاستخراج الروابط منه:' : 'Paste raw message or text with links:'}</span>
                  <span className="text-[10px] text-emerald-400 font-mono">
                    {notificationsService.extractLinksFromRawText(rawText).length}{' '}
                    {isArabic ? 'روابط مكتشفة' : 'links detected'}
                  </span>
                </label>
                <textarea
                  value={rawText}
                  onChange={(e) => {
                    setRawText(e.target.value);
                    syncLinksFromText(e.target.value);
                  }}
                  rows={5}
                  placeholder={
                    isArabic
                      ? 'الصق نصوصاً طويلة، رسائل، أو روابط تيليجرام عامة وخاصة...'
                      : 'Paste messages, posts, or multiple links to extract automatically...'
                  }
                  className="w-full bg-black/40 border border-white/10 rounded-2xl p-3 text-xs text-white placeholder-gray-500 focus:outline-none focus:border-emerald-400 resize-none font-mono leading-relaxed"
                />
              </div>

              <div className="flex items-center justify-between">
                <button
                  type="button"
                  onClick={() => {
                    syncLinksFromText(rawText);
                    setActiveTab('selection');
                    showToast(isArabic ? 'تم استخراج الروابط وتحديث القائمة' : 'Links extracted & synced', '✨');
                  }}
                  className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold transition-all shadow flex items-center gap-1.5"
                >
                  <Sparkles className="w-3.5 h-3.5" />
                  <span>{isArabic ? 'تطبيق والانتقال لتحديد الروابط' : 'Extract & Open Selection'}</span>
                </button>
              </div>
            </div>
          )}

          {/* Options: Web Scraping & Name Search */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-1">
            <label className="p-3 rounded-2xl bg-black/30 border border-white/5 flex items-center justify-between cursor-pointer hover:bg-white/5 transition-colors">
              <span className="text-xs font-medium text-gray-200 flex items-center gap-2">
                <Globe className="w-4 h-4 text-emerald-400" />
                <span>{isArabic ? 'جلب الروابط من صفحات الويب' : 'Scrape web pages for links'}</span>
              </span>
              <input
                type="checkbox"
                checked={fetchWebLinks}
                onChange={(e) => setFetchWebLinks(e.target.checked)}
                className="w-4 h-4 rounded accent-emerald-500 cursor-pointer"
              />
            </label>

            <label className="p-3 rounded-2xl bg-black/30 border border-white/5 flex items-center justify-between cursor-pointer hover:bg-white/5 transition-colors">
              <span className="text-xs font-medium text-gray-200 flex items-center gap-2">
                <Search className="w-4 h-4 text-emerald-400" />
                <span>{isArabic ? 'البحث عن أسماء المجموعات' : 'Search groups by name'}</span>
              </span>
              <input
                type="checkbox"
                checked={searchByName}
                onChange={(e) => setSearchByName(e.target.checked)}
                className="w-4 h-4 rounded accent-emerald-500 cursor-pointer"
              />
            </label>
          </div>

          {/* ENHANCED REAL-TIME PROGRESS BAR */}
          {(isProcessing || progress.total > 0) && (
            <div
              id="tg-autojoin-progress-card"
              className="p-4 sm:p-5 rounded-3xl bg-black/60 border border-emerald-500/40 space-y-3 shadow-2xl relative overflow-hidden"
            >
              {/* Glowing decorative background aura */}
              <div className="absolute top-0 right-0 w-32 h-32 bg-emerald-500/10 rounded-full blur-2xl pointer-events-none" />

              {/* Progress Header */}
              <div className="flex items-center justify-between text-xs">
                <div className="flex items-center gap-2">
                  {isProcessing ? (
                    <Loader2 className="w-4 h-4 animate-spin text-emerald-400 shrink-0" />
                  ) : (
                    <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                  )}
                  <span className="font-bold text-white">
                    {isProcessing
                      ? isArabic
                        ? 'جاري الانضمام إلى القنوات المحددة...'
                        : 'Joining selected channels...'
                      : isArabic
                      ? 'اكتملت عملية الانضمام الجماعي'
                      : 'Bulk join operation completed'}
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  <span className="font-mono font-extrabold text-emerald-400 text-sm">
                    {progressPercent}%
                  </span>
                  <span className="text-[11px] text-gray-400 font-mono">
                    ({progress.processed} / {progress.total})
                  </span>
                </div>
              </div>

              {/* Visual Progress Bar */}
              <div className="w-full h-3 rounded-full bg-black/70 border border-white/10 p-0.5 overflow-hidden shadow-inner relative">
                <div
                  className="h-full rounded-full bg-gradient-to-r from-emerald-600 via-emerald-400 to-teal-300 transition-all duration-300 relative shadow-md shadow-emerald-500/50"
                  style={{
                    width: `${Math.min(100, Math.max(0, progressPercent))}%`,
                  }}
                >
                  {/* Subtle animated shine when processing */}
                  {isProcessing && (
                    <div className="absolute inset-0 bg-white/20 animate-pulse rounded-full" />
                  )}
                </div>
              </div>

              {/* Status Breakdown Indicators */}
              <div className="grid grid-cols-3 gap-2 pt-1 text-[11px]">
                <div className="p-2 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 flex items-center justify-between">
                  <span>{isArabic ? 'ناجحة:' : 'Success:'}</span>
                  <span className="font-bold font-mono">{stats.joined}</span>
                </div>

                <div className="p-2 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 flex items-center justify-between">
                  <span>{isArabic ? 'فشلت:' : 'Failed:'}</span>
                  <span className="font-bold font-mono">{stats.failed}</span>
                </div>

                <div className="p-2 rounded-xl bg-sky-500/10 border border-sky-500/20 text-sky-300 flex items-center justify-between">
                  <span>{isArabic ? 'متبقية:' : 'Remaining:'}</span>
                  <span className="font-bold font-mono">
                    {Math.max(0, progress.total - progress.processed)}
                  </span>
                </div>
              </div>

              {/* Current joining channel subtitle */}
              {isProcessing && currentJoiningTask && (
                <div className="text-[11px] text-cyan-300/90 font-mono flex items-center gap-1.5 truncate pt-0.5">
                  <span className="inline-block w-2 h-2 rounded-full bg-cyan-400 animate-ping" />
                  <span>
                    {isArabic ? 'المعالجة الحالية:' : 'Current target:'}{' '}
                    <strong className="text-white select-text">{currentJoiningTask.url}</strong>
                  </span>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer Actions */}
        <div className="p-4 bg-black/40 border-t border-white/10 flex items-center justify-between shrink-0">
          <button
            type="button"
            onClick={() => setActiveModal('none')}
            className="px-4 py-2.5 rounded-xl text-gray-400 hover:text-white text-xs font-bold transition-colors"
          >
            {isArabic ? 'إغلاق' : 'Close'}
          </button>

          <div className="flex items-center gap-2">
            {isProcessing ? (
              <button
                type="button"
                onClick={handleStopJoin}
                className="px-6 py-2.5 rounded-xl bg-rose-600 hover:bg-rose-500 active:scale-95 text-white text-xs font-bold transition-all shadow-lg shadow-rose-950/50 flex items-center gap-2 cursor-pointer"
              >
                <Square className="w-4 h-4" />
                <span>{isArabic ? 'إيقاف الانضمام' : 'Stop Joining'}</span>
              </button>
            ) : (
              <button
                type="button"
                onClick={handleStartJoin}
                disabled={selectedCount === 0}
                className="px-6 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 active:scale-95 disabled:opacity-40 disabled:pointer-events-none text-white text-xs font-bold transition-all shadow-lg shadow-emerald-950/60 flex items-center gap-2 cursor-pointer"
              >
                <Play className="w-4 h-4 fill-current" />
                <span>
                  {isArabic
                    ? `بدء الانضمام للمحدد (${selectedCount})`
                    : `Bulk Join Selected (${selectedCount})`}
                </span>
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
