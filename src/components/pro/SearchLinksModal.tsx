import React, { useState } from "react";
import {
  X,
  Search,
  Compass,
  Users,
  Megaphone,
  Bot,
  Link2,
  BookmarkPlus,
  Copy,
  Check,
  Sparkles,
  ExternalLink,
  Filter,
  CheckCircle2,
} from "lucide-react";
import { TelegramSearchResult, SavedTelegramLink } from "../../types";
import {
  getSavedLinks,
  saveSavedLinks,
  getAutoJoinQueue,
  saveAutoJoinQueue,
} from "../../lib/telegramProTools";
import { apiJoinChat } from "../../lib/telegramApi";

interface SearchLinksModalProps {
  isOpen: boolean;
  onClose: () => void;
  onShowToast: (notif: { title: string; body: string; type: "success" | "error" | "system" | "message" }) => void;
  onOpenAutoJoin?: () => void;
  onOpenSavedLinks?: () => void;
}

// Built-in verified Telegram directory database
const DEFAULT_DIRECTORY: TelegramSearchResult[] = [
  {
    id: "dir_1",
    title: "أخبار تيليجرام الرسمية بالعربية",
    username: "telegram_ar",
    type: "channel",
    participantsCount: 1450000,
    description: "القناة الإخبارية الرسمية لتحديثات وميزات تطبيق تيليجرام بالعربية.",
    verified: true,
    link: "https://t.me/telegram_ar",
  },
  {
    id: "dir_2",
    title: "عالم البرمجة والمطورين العرب",
    username: "arab_dev_community",
    type: "channel",
    participantsCount: 382000,
    description: "أكبر مجتمع عربي لتعليم البرمجة، هندسة البرمجيات، والذكاء الاصطناعي.",
    verified: false,
    link: "https://t.me/arab_dev_community",
  },
  {
    id: "dir_3",
    title: "مكتبة المراجع والكتب الأكاديمية PDF",
    username: "arabic_academic_pdf",
    type: "channel",
    participantsCount: 520000,
    description: "مكتبة إلكترونية للكتب الجامعية، الأبحاث العلمية، والمراجع الدراسية المجانية.",
    verified: true,
    link: "https://t.me/arabic_academic_pdf",
  },
  {
    id: "dir_4",
    title: "ملتقى طلاب الهندسة والتقنية",
    username: "engineering_students_hub",
    type: "group",
    participantsCount: 94000,
    description: "مجموعة نقاشات وتبادل ملفات ومشاريع لطلاب الهندسة والحاسب الآلي.",
    verified: false,
    link: "https://t.me/engineering_students_hub",
  },
  {
    id: "dir_5",
    title: "بوت تحميل الوسائط والملفات الذكي",
    username: "SmartMediaDownloaderBot",
    type: "bot",
    participantsCount: 890000,
    description: "بوت سريع لتحميل مقاطع الفيديو والملفات والصوتيات بروابط مباشرة.",
    verified: true,
    link: "https://t.me/SmartMediaDownloaderBot",
  },
  {
    id: "dir_6",
    title: "أخبار التكنولوجيا والذكاء الاصطناعي",
    username: "tech_ai_arabic",
    type: "channel",
    participantsCount: 290000,
    description: "تغطية يومية لأحدث تطورات الذكاء الاصطناعي والتقنيات الحديثة والابتكارات.",
    verified: false,
    link: "https://t.me/tech_ai_arabic",
  },
  {
    id: "dir_7",
    title: "وظائف وفرص عمل عن بعد",
    username: "remote_jobs_arabia",
    type: "channel",
    participantsCount: 410000,
    description: "فرص عمل يومية عن بعد، وظائف مستقلة (Freelancing) وتدريب احترافي.",
    verified: false,
    link: "https://t.me/remote_jobs_arabia",
  },
];

export const SearchLinksModal: React.FC<SearchLinksModalProps> = ({
  isOpen,
  onClose,
  onShowToast,
  onOpenAutoJoin,
  onOpenSavedLinks,
}) => {
  const [searchQuery, setSearchQuery] = useState("");
  const [filterType, setFilterType] = useState<"all" | "channel" | "group" | "bot">("all");
  const [results, setResults] = useState<TelegramSearchResult[]>(DEFAULT_DIRECTORY);
  const [copiedLink, setCopiedLink] = useState<string | null>(null);
  const [joiningId, setJoiningId] = useState<string | null>(null);
  const [savedLinkIds, setSavedLinkIds] = useState<string[]>(() =>
    getSavedLinks().map((l) => l.link)
  );

  if (!isOpen) return null;

  const handleSearch = (query: string, type = filterType) => {
    setSearchQuery(query);
    const q = query.trim().toLowerCase();
    
    let filtered = DEFAULT_DIRECTORY;
    if (type !== "all") {
      filtered = filtered.filter((item) => item.type === type);
    }
    
    if (q) {
      filtered = filtered.filter(
        (item) =>
          item.title.toLowerCase().includes(q) ||
          item.username?.toLowerCase().includes(q) ||
          item.description?.toLowerCase().includes(q)
      );
    }
    
    setResults(filtered);
  };

  const handleFilterChange = (type: "all" | "channel" | "group" | "bot") => {
    setFilterType(type);
    handleSearch(searchQuery, type);
  };

  const handleCopyLink = (link: string) => {
    navigator.clipboard.writeText(link);
    setCopiedLink(link);
    setTimeout(() => setCopiedLink(null), 2000);
    onShowToast({
      title: "تم نسخ الرابط",
      body: `تم نسخ ${link} إلى الحافظة.`,
      type: "success",
    });
  };

  const handleJoinChat = async (item: TelegramSearchResult) => {
    setJoiningId(String(item.id));
    try {
      await apiJoinChat(item.username || item.link);
      onShowToast({
        title: "تم الانضمام بنجاح 🎉",
        body: `أصبحت الآن عضواً في "${item.title}"`,
        type: "success",
      });
    } catch (err: any) {
      onShowToast({
        title: "فشل الانضمام",
        body: err.message || "تعذر الانضمام، قد تكون المحادثة خاصة أو تتطلب موافقة.",
        type: "error",
      });
    } finally {
      setJoiningId(null);
    }
  };

  const handleSaveToBank = (item: TelegramSearchResult) => {
    const currentSaved = getSavedLinks();
    if (currentSaved.some((l) => l.link === item.link)) {
      onShowToast({
        title: "الرابط محفوظ مسبقاً",
        body: "هذه القناة موجودة بالفعل في بنك الروابط المحفوظة.",
        type: "system",
      });
      return;
    }

    const newSaved: SavedTelegramLink = {
      id: `saved_${Date.now()}`,
      title: item.title,
      link: item.link,
      username: item.username,
      type: item.type as any,
      category: item.type === "channel" ? "قنوات" : item.type === "group" ? "مجموعات" : "بوتات",
      tags: ["دليل عام", "مستكشف"],
      savedAt: Date.now(),
    };

    const updated = [newSaved, ...currentSaved];
    saveSavedLinks(updated);
    setSavedLinkIds(updated.map((l) => l.link));

    onShowToast({
      title: "تم الحفظ في بنك الروابط ⭐",
      body: `تمت إضافة "${item.title}" إلى روابطك المحفوظة`,
      type: "success",
    });
  };

  const handleSendToAutoJoin = (item: TelegramSearchResult) => {
    const queue = getAutoJoinQueue();
    const newItem = {
      id: `join_${Date.now()}`,
      linkOrUsername: item.username ? `@${item.username}` : item.link,
      title: item.title,
      status: "pending" as const,
    };
    saveAutoJoinQueue([...queue, newItem]);
    onShowToast({
      title: "تمت الإضافة لقائمة الانضمام",
      body: `تم إرسال "${item.title}" إلى أداة الانضمام التلقائي.`,
      type: "success",
    });
    if (onOpenAutoJoin) {
      onClose();
      onOpenAutoJoin();
    }
  };

  const formatSubscribers = (num?: number) => {
    if (!num) return "";
    if (num >= 1000000) return `${(num / 1000000).toFixed(1)}M مشترك`;
    if (num >= 1000) return `${(num / 1000).toFixed(0)}K مشترك`;
    return `${num} مشترك`;
  };

  return (
    <div
      id="search-links-modal-backdrop"
      className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-3 animate-fade-in"
      dir="rtl"
    >
      <div
        id="search-links-modal-card"
        className="w-full max-w-2xl bg-white dark:bg-slate-900 rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-800 flex flex-col max-h-[90vh] overflow-hidden animate-scale-up"
      >
        {/* Modal Header */}
        <div className="p-4 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between bg-gradient-to-r from-sky-500/10 via-teal-500/5 to-transparent">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-sky-500/10 text-sky-500 flex items-center justify-center">
              <Compass className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="font-bold text-sm text-slate-800 dark:text-white">
                  البحث واستكشاف القنوات والروابط (Search Links)
                </h2>
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-sky-100 dark:bg-sky-950/60 text-sky-600 dark:text-sky-400">
                  دليل تيليجرام
                </span>
              </div>
              <p className="text-xs text-slate-400">
                ابحث في الدليل المفتوح عن القنوات والمجموعات والبوتات وانضم إليها بضغطة زر
              </p>
            </div>
          </div>
          <button
            id="close-search-links-modal-btn"
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-slate-600 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4 text-xs">
          {/* Search Bar */}
          <div className="relative">
            <Search className="w-4 h-4 absolute right-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => handleSearch(e.target.value)}
              placeholder="ابحث عن قنوات (مثال: برمجة، أكاديمي، أخبار، وظائف، تقنية)..."
              className="w-full pr-9 pl-4 py-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-xs focus:ring-2 focus:ring-sky-500 focus:outline-none"
            />
          </div>

          {/* Filters Bar */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-1.5 overflow-x-auto">
              {[
                { id: "all", label: "الكل" },
                { id: "channel", label: "قنوات 📢" },
                { id: "group", label: "مجموعات 👥" },
                { id: "bot", label: "بوتات 🤖" },
              ].map((f) => (
                <button
                  key={f.id}
                  onClick={() => handleFilterChange(f.id as any)}
                  className={`px-3 py-1.5 rounded-lg font-medium transition-colors ${
                    filterType === f.id
                      ? "bg-sky-500 text-white font-bold shadow-xs"
                      : "bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 hover:bg-slate-200 dark:hover:bg-slate-700"
                  }`}
                >
                  {f.label}
                </button>
              ))}
            </div>

            <span className="text-[11px] text-slate-400">
              {results.length} نتيجة مطابقة
            </span>
          </div>

          {/* Results List */}
          <div className="space-y-3">
            {results.length === 0 ? (
              <div className="text-center py-16 text-slate-400">
                <Search className="w-8 h-8 mx-auto mb-2 text-slate-300 dark:text-slate-700" />
                <span>لم يتم العثور على نتائج تطابق بحثك. جرب كلمات بحث أخرى.</span>
              </div>
            ) : (
              results.map((item) => {
                const isSaved = savedLinkIds.includes(item.link);

                return (
                  <div
                    key={item.id}
                    className="p-3.5 bg-slate-50/80 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700/80 rounded-2xl space-y-2.5 transition-all hover:border-sky-300 dark:hover:border-sky-700"
                  >
                    {/* Item Top info */}
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex items-start gap-2.5 min-w-0">
                        <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-sky-500 to-indigo-600 text-white flex items-center justify-center font-bold text-sm shrink-0 shadow-xs">
                          {item.type === "bot" ? (
                            <Bot className="w-5 h-5" />
                          ) : item.type === "group" ? (
                            <Users className="w-5 h-5" />
                          ) : (
                            <Megaphone className="w-5 h-5" />
                          )}
                        </div>

                        <div className="min-w-0">
                          <div className="flex items-center gap-1.5">
                            <h3 className="font-bold text-xs text-slate-800 dark:text-white truncate">
                              {item.title}
                            </h3>
                            {item.verified && (
                              <CheckCircle2 className="w-3.5 h-3.5 text-sky-500 shrink-0" />
                            )}
                          </div>
                          <div className="flex items-center gap-2 text-[10px] text-slate-400 mt-0.5">
                            <span dir="ltr" className="font-mono text-sky-600 dark:text-sky-400">
                              @{item.username}
                            </span>
                            <span>•</span>
                            <span>{formatSubscribers(item.participantsCount)}</span>
                          </div>
                        </div>
                      </div>

                      {/* Type Badge */}
                      <span className="text-[10px] px-2 py-0.5 rounded-md bg-slate-200 dark:bg-slate-700 text-slate-600 dark:text-slate-300 shrink-0">
                        {item.type === "channel" ? "قناة" : item.type === "group" ? "مجموعة" : "بوت"}
                      </span>
                    </div>

                    {/* Description */}
                    {item.description && (
                      <p className="text-xs text-slate-600 dark:text-slate-300 leading-relaxed">
                        {item.description}
                      </p>
                    )}

                    {/* Actions Bar */}
                    <div className="flex items-center justify-between pt-1 border-t border-slate-200/60 dark:border-slate-700/60">
                      <button
                        onClick={() => handleCopyLink(item.link)}
                        className="px-2.5 py-1 text-slate-500 hover:text-slate-700 dark:hover:text-slate-200 flex items-center gap-1 text-[11px]"
                      >
                        {copiedLink === item.link ? (
                          <>
                            <Check className="w-3.5 h-3.5 text-emerald-500" />
                            <span className="text-emerald-600">تم النسخ</span>
                          </>
                        ) : (
                          <>
                            <Copy className="w-3.5 h-3.5" />
                            <span>نسخ الرابط</span>
                          </>
                        )}
                      </button>

                      <div className="flex items-center gap-1.5">
                        <button
                          onClick={() => handleSaveToBank(item)}
                          className={`px-2.5 py-1.5 rounded-lg text-xs font-medium flex items-center gap-1 transition-colors ${
                            isSaved
                              ? "bg-amber-100 dark:bg-amber-950/50 text-amber-600 dark:text-amber-300"
                              : "bg-slate-200 dark:bg-slate-700 hover:bg-slate-300 dark:hover:bg-slate-600 text-slate-700 dark:text-slate-200"
                          }`}
                        >
                          <BookmarkPlus className="w-3.5 h-3.5" />
                          <span>{isSaved ? "محفوظ ⭐" : "حفظ في البنك"}</span>
                        </button>

                        <button
                          onClick={() => handleSendToAutoJoin(item)}
                          className="px-2.5 py-1.5 bg-slate-200 dark:bg-slate-700 hover:bg-slate-300 dark:hover:bg-slate-600 text-slate-700 dark:text-slate-200 rounded-lg text-xs font-medium flex items-center gap-1 transition-colors"
                        >
                          <Link2 className="w-3.5 h-3.5" />
                          <span>قائمة الانضمام</span>
                        </button>

                        <button
                          onClick={() => handleJoinChat(item)}
                          disabled={joiningId === item.id}
                          className="px-3 py-1.5 bg-sky-500 hover:bg-sky-600 disabled:opacity-50 text-white rounded-lg text-xs font-bold flex items-center gap-1 shadow-sm transition-transform active:scale-95"
                        >
                          <span>{joiningId === item.id ? "جاري الانضمام..." : "انضمام الآن"}</span>
                        </button>
                      </div>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* Modal Footer */}
        <div className="p-4 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between bg-slate-50/50 dark:bg-slate-900/50 text-xs">
          <span className="text-[11px] text-slate-400">
            يمكنك حفظ أي رابط في بنك الروابط أو إرساله مباشرة لجدول الانضمام التلقائي
          </span>
          <button
            onClick={onClose}
            className="px-4 py-2 bg-slate-200 dark:bg-slate-800 hover:bg-slate-300 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 rounded-xl font-bold transition-colors"
          >
            إغلاق
          </button>
        </div>
      </div>
    </div>
  );
};
