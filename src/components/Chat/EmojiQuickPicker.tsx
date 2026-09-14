import React, { useState, useEffect, useRef, useMemo, useCallback } from "react";
import { Smile, Search, Clock, Sparkles, X, ChevronUp, ChevronDown, Loader2 } from "lucide-react";
import type { EmojiCategory } from "../../data/fullEmojiLibrary";

interface EmojiQuickPickerProps {
  onSelectEmoji: (emoji: string) => void;
  isArabic?: boolean;
  className?: string;
  onOpenStickerDrawer?: () => void;
}

// Built-in instant zero-overhead quick emojis (Zero heavy dataset required at startup)
const QUICK_DEFAULT_EMOJIS = [
  "👍", "❤️", "🔥", "😂", "😍", "👏", "🎉", "🙏", 
  "😊", "🥰", "✨", "💯", "🚀", "🤔", "😎", "👌", 
  "🥺", "😢", "🌹", "🤝", "☕️", "💡", "🎯", "⚡️"
];

const RECENT_STORAGE_KEY = "tg_quick_recent_emojis";

export const EmojiQuickPicker: React.FC<EmojiQuickPickerProps> = ({
  onSelectEmoji,
  isArabic = true,
  className = "",
  onOpenStickerDrawer,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [activeCategory, setActiveCategory] = useState<string>("smileys");
  const [recentEmojis, setRecentEmojis] = useState<string[]>([]);
  
  // Lazy state for the full heavy emoji library
  const [fullCategories, setFullCategories] = useState<EmojiCategory[] | null>(null);
  const [isLoadingLibrary, setIsLoadingLibrary] = useState(false);

  const containerRef = useRef<HTMLDivElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);

  // Load recent emojis on mount from localStorage
  useEffect(() => {
    try {
      const stored = localStorage.getItem(RECENT_STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored);
        if (Array.isArray(parsed) && parsed.length > 0) {
          setRecentEmojis(parsed.slice(0, 16));
        }
      }
    } catch (_) {}
  }, []);

  // Lazy-loader function: dynamically loads the full heavy emoji library chunk
  const loadFullLibraryIfNeeded = useCallback(async () => {
    if (fullCategories) return fullCategories;
    setIsLoadingLibrary(true);
    try {
      const mod = await import("../../data/fullEmojiLibrary");
      setFullCategories(mod.FULL_EMOJI_CATEGORIES);
      return mod.FULL_EMOJI_CATEGORIES;
    } catch (err) {
      console.warn("[EmojiQuickPicker] Failed to lazy load full emoji library:", err);
      return null;
    } finally {
      setIsLoadingLibrary(false);
    }
  }, [fullCategories]);

  // Expand picker and ensure full library is loaded
  const handleToggleExpand = async () => {
    if (!isExpanded) {
      setIsExpanded(true);
      await loadFullLibraryIfNeeded();
    } else {
      setIsExpanded(false);
      setSearchQuery("");
    }
  };

  // Search trigger: when typing in search, automatically load full library
  const handleSearchChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setSearchQuery(val);
    if (val.trim()) {
      if (!isExpanded) setIsExpanded(true);
      if (!fullCategories) {
        await loadFullLibraryIfNeeded();
      }
    }
  };

  // Handle emoji click
  const handleEmojiClick = (emoji: string) => {
    onSelectEmoji(emoji);

    // Save to recents
    try {
      setRecentEmojis((prev) => {
        const filtered = prev.filter((e) => e !== emoji);
        const updated = [emoji, ...filtered].slice(0, 16);
        localStorage.setItem(RECENT_STORAGE_KEY, JSON.stringify(updated));
        return updated;
      });
    } catch (_) {}
  };

  // Close when clicking outside
  useEffect(() => {
    if (!isOpen) return;
    const handlePointerDown = (e: MouseEvent | TouchEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
        setIsExpanded(false);
        setSearchQuery("");
      }
    };
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        setIsOpen(false);
        setIsExpanded(false);
        setSearchQuery("");
      }
    };

    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("touchstart", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("touchstart", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen]);

  // Filtered emojis when searching across all categories
  const searchResults = useMemo(() => {
    if (!searchQuery.trim() || !fullCategories) return [];
    const query = searchQuery.trim().toLowerCase();
    const results: string[] = [];

    for (const cat of fullCategories) {
      // If category matches name
      const matchesCategory = 
        cat.nameEn.toLowerCase().includes(query) || 
        cat.nameAr.includes(query) ||
        cat.id.includes(query);

      for (const emoji of cat.emojis) {
        if (matchesCategory || emoji === query) {
          if (!results.includes(emoji)) results.push(emoji);
        }
      }
    }

    // Also include default matching
    for (const emoji of QUICK_DEFAULT_EMOJIS) {
      if (!results.includes(emoji) && (emoji === query || query.length === 1)) {
        results.push(emoji);
      }
    }

    return results.slice(0, 72);
  }, [searchQuery, fullCategories]);

  // Selected category items
  const currentCategoryEmojis = useMemo(() => {
    if (!fullCategories) return [];
    const cat = fullCategories.find((c) => c.id === activeCategory);
    return cat ? cat.emojis : [];
  }, [fullCategories, activeCategory]);

  return (
    <div ref={containerRef} className={`relative inline-flex items-center ${className}`}>
      {/* Quick Picker Trigger Button */}
      <button
        type="button"
        id="tg-quick-emoji-btn"
        onClick={() => {
          setIsOpen((prev) => !prev);
          if (isOpen) {
            setIsExpanded(false);
            setSearchQuery("");
          }
        }}
        className={`p-2 rounded-xl transition-all flex items-center justify-center shrink-0 ${
          isOpen
            ? "text-amber-400 bg-amber-400/10 scale-105"
            : "text-gray-400 hover:text-amber-400 hover:bg-white/10 active:scale-95"
        }`}
        title={isArabic ? "رموز تعبيرية سريعة" : "Quick Emoji"}
        aria-label={isArabic ? "رموز تعبيرية سريعة" : "Quick Emoji"}
      >
        <Smile className="w-5 h-5" />
      </button>

      {/* Lightweight Quick Popover */}
      {isOpen && (
        <div
          id="tg-quick-emoji-popover"
          className={`absolute bottom-full mb-2 rtl:left-0 ltr:right-0 sm:ltr:left-auto sm:rtl:right-auto z-40 rounded-2xl shadow-2xl border backdrop-blur-xl animate-in zoom-in-95 duration-150 flex flex-col overflow-hidden ${
            isExpanded ? "w-80 sm:w-96 h-96" : "w-72 sm:w-80 h-auto max-h-72"
          }`}
          style={{
            backgroundColor: "var(--tg-theme-surface, #1e293b)",
            borderColor: "var(--tg-theme-border, rgba(255,255,255,0.1))",
            color: "var(--tg-theme-bubble-in-text, #f8fafc)",
          }}
        >
          {/* Header Bar */}
          <div className="flex items-center justify-between px-3 py-2 border-b border-white/10 bg-black/20 shrink-0">
            <div className="flex items-center gap-1.5 text-xs font-bold text-amber-400">
              <Sparkles className="w-3.5 h-3.5" />
              <span>{isArabic ? "الرموز التعبيرية" : "Emoji Picker"}</span>
            </div>

            <div className="flex items-center gap-1">
              {onOpenStickerDrawer && (
                <button
                  type="button"
                  onClick={() => {
                    setIsOpen(false);
                    onOpenStickerDrawer();
                  }}
                  className="px-2 py-0.5 rounded-lg text-[10px] font-medium text-sky-400 hover:bg-sky-500/10 transition-colors"
                  title={isArabic ? "الملصقات و Lottie" : "Stickers & Lottie"}
                >
                  {isArabic ? "ملصقات" : "Stickers"}
                </button>
              )}

              <button
                type="button"
                onClick={handleToggleExpand}
                className="p-1 rounded-lg text-gray-400 hover:text-white hover:bg-white/10 transition-colors flex items-center gap-0.5 text-[10px]"
                title={isExpanded ? (isArabic ? "تصغير" : "Compact") : (isArabic ? "عرض الكل" : "Expand")}
              >
                <span className="hidden sm:inline">
                  {isExpanded ? (isArabic ? "موجز" : "Compact") : (isArabic ? "المزيد" : "More")}
                </span>
                {isExpanded ? <ChevronDown className="w-3.5 h-3.5" /> : <ChevronUp className="w-3.5 h-3.5" />}
              </button>

              <button
                type="button"
                onClick={() => {
                  setIsOpen(false);
                  setIsExpanded(false);
                  setSearchQuery("");
                }}
                className="p-1 rounded-lg text-gray-400 hover:text-rose-400 hover:bg-white/10 transition-colors"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>

          {/* Search bar (Active when expanded or focused) */}
          {isExpanded && (
            <div className="p-2 border-b border-white/10 bg-black/10 shrink-0">
              <div className="relative flex items-center">
                <Search className="w-3.5 h-3.5 text-gray-400 absolute start-2.5 pointer-events-none" />
                <input
                  ref={searchInputRef}
                  type="text"
                  value={searchQuery}
                  onChange={handleSearchChange}
                  placeholder={isArabic ? "بحث في كافة الرموز التعبيرية..." : "Search all emojis..."}
                  className="w-full ps-8 pe-3 py-1.5 rounded-xl bg-white/5 border border-white/10 text-xs focus:outline-none focus:border-[#2481cc] text-white placeholder:text-gray-500 transition-colors"
                />
                {searchQuery && (
                  <button
                    type="button"
                    onClick={() => setSearchQuery("")}
                    className="absolute end-2 text-gray-400 hover:text-white p-0.5"
                  >
                    <X className="w-3 h-3" />
                  </button>
                )}
              </div>
            </div>
          )}

          {/* Category Tabs (Rendered when expanded) */}
          {isExpanded && fullCategories && (
            <div className="flex items-center gap-1 px-2 py-1.5 border-b border-white/10 overflow-x-auto no-scrollbar shrink-0 bg-black/15 text-xs">
              {fullCategories.map((cat) => (
                <button
                  key={cat.id}
                  type="button"
                  onClick={() => {
                    setActiveCategory(cat.id);
                    setSearchQuery("");
                  }}
                  className={`px-2 py-1 rounded-lg text-sm transition-all shrink-0 flex items-center gap-1 ${
                    activeCategory === cat.id && !searchQuery
                      ? "bg-[#2481cc]/20 border border-[#2481cc]/40 text-sky-300 font-bold scale-105"
                      : "text-gray-400 hover:text-white hover:bg-white/5"
                  }`}
                  title={isArabic ? cat.nameAr : cat.nameEn}
                >
                  <span>{cat.icon}</span>
                </button>
              ))}
            </div>
          )}

          {/* Emoji Grid Area */}
          <div className="flex-1 overflow-y-auto p-2.5 no-scrollbar select-none">
            {isLoadingLibrary ? (
              <div className="h-40 flex flex-col items-center justify-center gap-2 text-gray-400">
                <Loader2 className="w-6 h-6 animate-spin text-amber-400" />
                <span className="text-xs">{isArabic ? "جارٍ تحميل المكتبة الكاملة..." : "Loading emoji library..."}</span>
              </div>
            ) : isExpanded ? (
              searchQuery.trim() ? (
                /* Search Results */
                <div>
                  <div className="text-[11px] font-semibold text-gray-400 mb-2 px-1">
                    {isArabic ? `نتائج البحث (${searchResults.length})` : `Search results (${searchResults.length})`}
                  </div>
                  {searchResults.length > 0 ? (
                    <div className="grid grid-cols-7 sm:grid-cols-8 gap-1.5 text-xl">
                      {searchResults.map((emoji, idx) => (
                        <button
                          key={idx}
                          type="button"
                          onClick={() => handleEmojiClick(emoji)}
                          className="p-1.5 rounded-xl hover:scale-125 hover:bg-white/15 transition-all flex items-center justify-center active:scale-95"
                        >
                          {emoji}
                        </button>
                      ))}
                    </div>
                  ) : (
                    <div className="py-8 text-center text-xs text-gray-500">
                      {isArabic ? "لم يتم العثور على رموز مطابقة" : "No matching emojis found"}
                    </div>
                  )}
                </div>
              ) : (
                /* Category View */
                <div>
                  <div className="text-[11px] font-bold text-sky-400 mb-2 px-1 flex items-center justify-between">
                    <span>
                      {isArabic
                        ? fullCategories?.find((c) => c.id === activeCategory)?.nameAr
                        : fullCategories?.find((c) => c.id === activeCategory)?.nameEn}
                    </span>
                    <span className="text-[10px] text-gray-400 font-normal">
                      {currentCategoryEmojis.length} {isArabic ? "رمز" : "emojis"}
                    </span>
                  </div>
                  <div className="grid grid-cols-7 sm:grid-cols-8 gap-1 text-xl">
                    {currentCategoryEmojis.map((emoji, idx) => (
                      <button
                        key={idx}
                        type="button"
                        onClick={() => handleEmojiClick(emoji)}
                        className="p-1.5 rounded-xl hover:scale-125 hover:bg-white/15 transition-all flex items-center justify-center active:scale-95"
                      >
                        {emoji}
                      </button>
                    ))}
                  </div>
                </div>
              )
            ) : (
              /* Compact Quick Mode (Zero heavy library loaded!) */
              <div className="space-y-3">
                {/* Recently Used (if any) */}
                {recentEmojis.length > 0 && (
                  <div>
                    <div className="text-[11px] font-semibold text-gray-400 mb-1.5 px-1 flex items-center gap-1">
                      <Clock className="w-3 h-3 text-sky-400" />
                      <span>{isArabic ? "الأخيرة" : "Recently Used"}</span>
                    </div>
                    <div className="grid grid-cols-8 gap-1 text-xl bg-black/15 p-1.5 rounded-xl border border-white/5">
                      {recentEmojis.map((emoji, idx) => (
                        <button
                          key={idx}
                          type="button"
                          onClick={() => handleEmojiClick(emoji)}
                          className="p-1 rounded-lg hover:scale-125 hover:bg-white/15 transition-all flex items-center justify-center active:scale-95"
                        >
                          {emoji}
                        </button>
                      ))}
                    </div>
                  </div>
                )}

                {/* Instant Popular Emojis */}
                <div>
                  <div className="text-[11px] font-semibold text-gray-400 mb-1.5 px-1 flex items-center justify-between">
                    <span>{isArabic ? "رموز سريعة وشائعة" : "Popular & Quick"}</span>
                    <button
                      type="button"
                      onClick={handleToggleExpand}
                      className="text-[10px] text-sky-400 hover:text-sky-300 hover:underline font-normal"
                    >
                      {isArabic ? "المكتبة الكاملة ←" : "Full Library →"}
                    </button>
                  </div>
                  <div className="grid grid-cols-8 gap-1 text-xl">
                    {QUICK_DEFAULT_EMOJIS.map((emoji, idx) => (
                      <button
                        key={idx}
                        type="button"
                        onClick={() => handleEmojiClick(emoji)}
                        className="p-1 rounded-lg hover:scale-125 hover:bg-white/15 transition-all flex items-center justify-center active:scale-95"
                      >
                        {emoji}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Bottom quick toggle bar */}
          {!isExpanded && (
            <div className="p-1.5 bg-black/25 border-t border-white/5 flex items-center justify-between px-3 text-[11px] text-gray-400 shrink-0">
              <span className="text-[10px] text-gray-500">
                {isArabic ? "أداء فائق بدون تأخير" : "Instant 0ms picker"}
              </span>
              <button
                type="button"
                onClick={handleToggleExpand}
                className="text-amber-400 hover:text-amber-300 font-medium flex items-center gap-1 hover:underline"
              >
                <span>{isArabic ? "تصفح كافة الرموز (800+)" : "Browse all (800+)"}</span>
                <ChevronUp className="w-3 h-3" />
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
