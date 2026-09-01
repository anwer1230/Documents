import React, { useState, useEffect } from "react";
import {
  X,
  Bookmark,
  Plus,
  Trash2,
  Edit2,
  Copy,
  ExternalLink,
  Check,
  Search,
  Tag,
  Share2,
  Download,
  Upload,
  UserPlus,
  Layers,
  FolderHeart,
} from "lucide-react";
import { SavedTelegramLink } from "../../types";
import {
  getSavedLinks,
  saveSavedLinks,
  getAutoJoinQueue,
  saveAutoJoinQueue,
} from "../../lib/telegramProTools";
import { apiJoinChat } from "../../lib/telegramApi";

interface SavedLinksModalProps {
  isOpen: boolean;
  onClose: () => void;
  onShowToast: (notif: { title: string; body: string; type: "success" | "error" | "system" | "message" }) => void;
  onOpenAutoJoin?: () => void;
}

export const SavedLinksModal: React.FC<SavedLinksModalProps> = ({
  isOpen,
  onClose,
  onShowToast,
  onOpenAutoJoin,
}) => {
  const [links, setLinks] = useState<SavedTelegramLink[]>(() => getSavedLinks());
  const [activeCategory, setActiveCategory] = useState<string>("all");
  const [searchQuery, setSearchQuery] = useState("");
  const [isAddingNew, setIsAddingNew] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);

  // Form fields
  const [formTitle, setFormTitle] = useState("");
  const [formLink, setFormLink] = useState("");
  const [formCategory, setFormCategory] = useState("عام");
  const [formTags, setFormTags] = useState("");
  const [formNotes, setFormNotes] = useState("");

  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [joiningId, setJoiningId] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      setLinks(getSavedLinks());
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const categories = ["all", ...Array.from(new Set(links.map((l) => l.category || "عام")))];

  const filteredLinks = links.filter((item) => {
    if (activeCategory !== "all" && item.category !== activeCategory) return false;
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const matchTitle = item.title.toLowerCase().includes(q);
      const matchLink = item.link.toLowerCase().includes(q);
      const matchTag = item.tags?.some((t) => t.toLowerCase().includes(q));
      if (!matchTitle && !matchLink && !matchTag) return false;
    }
    return true;
  });

  const handleSaveForm = () => {
    if (!formTitle.trim() || !formLink.trim()) {
      onShowToast({
        title: "بيانات غير مكتملة",
        body: "يرجى كتابة عنوان الرابط وعنوان الـ URL.",
        type: "error",
      });
      return;
    }

    const tagList = formTags
      .split(/[,،]/)
      .map((t) => t.trim())
      .filter(Boolean);

    let cleanUsername = "";
    if (formLink.includes("t.me/")) {
      const match = formLink.match(/t\.me\/([a-zA-Z0-9_+]+)/);
      if (match && !match[1].startsWith("+")) {
        cleanUsername = match[1];
      }
    }

    let updatedList: SavedTelegramLink[];
    if (editingId) {
      updatedList = links.map((l) =>
        l.id === editingId
          ? {
              ...l,
              title: formTitle.trim(),
              link: formLink.trim(),
              username: cleanUsername || l.username,
              category: formCategory.trim() || "عام",
              tags: tagList,
              notes: formNotes.trim(),
            }
          : l
      );
      onShowToast({
        title: "تم تحديث الرابط",
        body: `تم تعديل بيانات "${formTitle}" بنجاح`,
        type: "success",
      });
    } else {
      const newLink: SavedTelegramLink = {
        id: `saved_${Date.now()}`,
        title: formTitle.trim(),
        link: formLink.trim(),
        username: cleanUsername || undefined,
        type: formLink.includes("+") || formLink.includes("joinchat") ? "invite" : "channel",
        category: formCategory.trim() || "عام",
        tags: tagList,
        notes: formNotes.trim(),
        savedAt: Date.now(),
      };
      updatedList = [newLink, ...links];
      onShowToast({
        title: "تم حفظ الرابط ⭐",
        body: `تمت إضافة "${formTitle}" إلى بنك الروابط`,
        type: "success",
      });
    }

    setLinks(updatedList);
    saveSavedLinks(updatedList);
    setIsAddingNew(false);
    setEditingId(null);
    resetForm();
  };

  const resetForm = () => {
    setFormTitle("");
    setFormLink("");
    setFormCategory("عام");
    setFormTags("");
    setFormNotes("");
  };

  const handleEditClick = (item: SavedTelegramLink) => {
    setEditingId(item.id);
    setFormTitle(item.title);
    setFormLink(item.link);
    setFormCategory(item.category);
    setFormTags(item.tags?.join("، ") || "");
    setFormNotes(item.notes || "");
    setIsAddingNew(true);
  };

  const handleDelete = (id: string) => {
    const updated = links.filter((l) => l.id !== id);
    setLinks(updated);
    saveSavedLinks(updated);
    onShowToast({
      title: "تم حذف الرابط",
      body: "تم إزالة الرابط من البنك.",
      type: "system",
    });
  };

  const handleCopy = (item: SavedTelegramLink) => {
    navigator.clipboard.writeText(item.link);
    setCopiedId(item.id);
    setTimeout(() => setCopiedId(null), 2000);
    onShowToast({
      title: "تم نسخ الرابط",
      body: item.link,
      type: "success",
    });
  };

  const handleJoinChat = async (item: SavedTelegramLink) => {
    setJoiningId(item.id);
    try {
      await apiJoinChat(item.username ? `@${item.username}` : item.link);
      const updated = links.map((l) => (l.id === item.id ? { ...l, isJoined: true } : l));
      setLinks(updated);
      saveSavedLinks(updated);

      onShowToast({
        title: "تم الانضمام بنجاح 🎉",
        body: `أصبحت عضواً في "${item.title}"`,
        type: "success",
      });
    } catch (err: any) {
      onShowToast({
        title: "فشل الانضمام",
        body: err.message || "حدث خطأ أثناء محاولة الانضمام",
        type: "error",
      });
    } finally {
      setJoiningId(null);
    }
  };

  const handleExportJson = () => {
    const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(links, null, 2));
    const downloadAnchor = document.createElement("a");
    downloadAnchor.setAttribute("href", dataStr);
    downloadAnchor.setAttribute("download", `telegram_saved_links_${Date.now()}.json`);
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
  };

  const handleSendAllToAutoJoin = () => {
    const queue = getAutoJoinQueue();
    const newItems = links.map((l) => ({
      id: `join_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`,
      linkOrUsername: l.username ? `@${l.username}` : l.link,
      title: l.title,
      status: "pending" as const,
    }));
    saveAutoJoinQueue([...queue, ...newItems]);
    onShowToast({
      title: "تم الإرسال لأداة الانضمام",
      body: `تم إدراج ${newItems.length} رابط في جدول الانضمام التلقائي.`,
      type: "success",
    });
    if (onOpenAutoJoin) {
      onClose();
      onOpenAutoJoin();
    }
  };

  return (
    <div
      id="saved-links-modal-backdrop"
      className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-3 animate-fade-in"
      dir="rtl"
    >
      <div
        id="saved-links-modal-card"
        className="w-full max-w-2xl bg-white dark:bg-slate-900 rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-800 flex flex-col max-h-[90vh] overflow-hidden animate-scale-up"
      >
        {/* Modal Header */}
        <div className="p-4 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between bg-gradient-to-r from-amber-500/10 via-orange-500/5 to-transparent">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-amber-500/10 text-amber-500 flex items-center justify-center">
              <Bookmark className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="font-bold text-sm text-slate-800 dark:text-white">
                  بنك الروابط المحفوظة (Saved Links Bank)
                </h2>
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-100 dark:bg-amber-950/60 text-amber-600 dark:text-amber-400">
                  {links.length} رابط محفوظ
                </span>
              </div>
              <p className="text-xs text-slate-400">
                مستودعك الخاص لتخزين وتنظيم روابط القنوات والمجموعات والمراجع
              </p>
            </div>
          </div>
          <button
            id="close-saved-links-modal-btn"
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-slate-600 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4 text-xs">
          {!isAddingNew ? (
            <>
              {/* Search and Top Actions Bar */}
              <div className="flex items-center gap-2">
                <div className="flex-1 relative">
                  <Search className="w-4 h-4 absolute right-3 top-1/2 -translate-y-1/2 text-slate-400" />
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="بحث في الروابط المحفوظة، التصنيفات، والوسوم..."
                    className="w-full pr-9 pl-4 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-xs focus:ring-2 focus:ring-amber-500 focus:outline-none"
                  />
                </div>

                <button
                  onClick={() => {
                    resetForm();
                    setEditingId(null);
                    setIsAddingNew(true);
                  }}
                  className="px-3 py-2 bg-amber-500 hover:bg-amber-600 text-white rounded-xl text-xs font-bold flex items-center gap-1.5 shadow-sm transition-transform active:scale-95 shrink-0"
                >
                  <Plus className="w-4 h-4" />
                  <span>إضافة رابط</span>
                </button>
              </div>

              {/* Category Pills & Actions */}
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-1.5 overflow-x-auto pb-1">
                  {categories.map((cat) => (
                    <button
                      key={cat}
                      onClick={() => setActiveCategory(cat)}
                      className={`px-3 py-1.5 rounded-lg whitespace-nowrap transition-colors font-medium ${
                        activeCategory === cat
                          ? "bg-amber-500 text-white font-bold shadow-xs"
                          : "bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 hover:bg-slate-200 dark:hover:bg-slate-700"
                      }`}
                    >
                      {cat === "all" ? "جميع الروابط" : cat}
                    </button>
                  ))}
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={handleExportJson}
                    title="تصدير نسخة احتياطية JSON"
                    className="p-1.5 text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition-colors flex items-center gap-1 text-[11px]"
                  >
                    <Download className="w-3.5 h-3.5" />
                    <span className="hidden sm:inline">تصدير</span>
                  </button>
                  {links.length > 0 && (
                    <button
                      onClick={handleSendAllToAutoJoin}
                      className="px-2.5 py-1 bg-sky-100 dark:bg-sky-950/60 text-sky-600 dark:text-sky-400 hover:bg-sky-200 rounded-lg text-[11px] font-bold flex items-center gap-1"
                    >
                      <Layers className="w-3.5 h-3.5" />
                      <span>انضمام للكل</span>
                    </button>
                  )}
                </div>
              </div>

              {/* Saved Links List */}
              <div className="space-y-3">
                {filteredLinks.length === 0 ? (
                  <div className="text-center py-16 text-slate-400">
                    <FolderHeart className="w-8 h-8 mx-auto mb-2 text-slate-300 dark:text-slate-700" />
                    <span>لا توجد روابط في هذا القسم. اضغط على "إضافة رابط" لحفظ رابطك الأول.</span>
                  </div>
                ) : (
                  filteredLinks.map((item) => (
                    <div
                      key={item.id}
                      className="p-3.5 bg-slate-50/80 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 rounded-2xl space-y-2 relative group transition-all hover:border-amber-300 dark:hover:border-amber-700"
                    >
                      {/* Item Top Header */}
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <h3 className="font-bold text-xs text-slate-800 dark:text-white">
                            {item.title}
                          </h3>
                          <span className="text-[10px] px-2 py-0.5 rounded-md bg-amber-100 dark:bg-amber-950/50 text-amber-700 dark:text-amber-300 font-medium">
                            {item.category}
                          </span>
                          {item.isJoined && (
                            <span className="text-[10px] px-2 py-0.5 rounded-md bg-emerald-100 dark:bg-emerald-950/50 text-emerald-600 dark:text-emerald-400 font-bold">
                              عضو ✓
                            </span>
                          )}
                        </div>

                        {/* Top Actions */}
                        <div className="flex items-center gap-1">
                          <button
                            onClick={() => handleEditClick(item)}
                            className="p-1.5 text-slate-400 hover:text-sky-500 rounded-lg transition-colors"
                            title="تعديل"
                          >
                            <Edit2 className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => handleDelete(item.id)}
                            className="p-1.5 text-slate-400 hover:text-red-500 rounded-lg transition-colors"
                            title="حذف"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </div>

                      {/* Link URL */}
                      <div className="p-2 bg-white dark:bg-slate-900 rounded-xl border border-slate-100 dark:border-slate-800 flex items-center justify-between">
                        <span className="font-mono text-slate-700 dark:text-slate-300 text-[11px] truncate" dir="ltr">
                          {item.link}
                        </span>
                        <button
                          onClick={() => handleCopy(item)}
                          className="px-2 py-1 text-slate-500 hover:text-slate-700 dark:hover:text-slate-200 rounded text-[10px] font-medium flex items-center gap-1 shrink-0"
                        >
                          {copiedId === item.id ? (
                            <>
                              <Check className="w-3 h-3 text-emerald-500" />
                              <span className="text-emerald-600">تم</span>
                            </>
                          ) : (
                            <>
                              <Copy className="w-3 h-3" />
                              <span>نسخ</span>
                            </>
                          )}
                        </button>
                      </div>

                      {/* Notes & Tags */}
                      {item.notes && (
                        <p className="text-slate-500 dark:text-slate-400 text-[11px] leading-relaxed">
                          {item.notes}
                        </p>
                      )}

                      {/* Bottom Footer Actions */}
                      <div className="flex items-center justify-between pt-1">
                        <div className="flex flex-wrap gap-1">
                          {item.tags?.map((tg, i) => (
                            <span key={i} className="text-[10px] text-slate-400 flex items-center gap-0.5">
                              <Tag className="w-2.5 h-2.5" />
                              {tg}
                            </span>
                          ))}
                        </div>

                        <div className="flex items-center gap-1.5">
                          <button
                            onClick={() => handleJoinChat(item)}
                            disabled={joiningId === item.id}
                            className="px-3 py-1.5 bg-amber-500 hover:bg-amber-600 disabled:opacity-50 text-white rounded-lg text-xs font-bold flex items-center gap-1 shadow-sm transition-transform active:scale-95"
                          >
                            <UserPlus className="w-3.5 h-3.5" />
                            <span>{joiningId === item.id ? "جاري الانضمام..." : "انضمام"}</span>
                          </button>

                          <a
                            href={item.link}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="p-1.5 bg-slate-200 dark:bg-slate-700 hover:bg-slate-300 dark:hover:bg-slate-600 text-slate-700 dark:text-slate-200 rounded-lg transition-colors"
                            title="فتح في تيليجرام مباشرة"
                          >
                            <ExternalLink className="w-3.5 h-3.5" />
                          </a>
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </>
          ) : (
            /* Add / Edit Link Form */
            <div className="space-y-4">
              <div className="flex items-center justify-between border-b border-slate-100 dark:border-slate-800 pb-3">
                <h3 className="font-bold text-sm text-slate-800 dark:text-white">
                  {editingId ? "تعديل بيانات الرابط" : "إضافة رابط جديد إلى البنك"}
                </h3>
                <button onClick={() => setIsAddingNew(false)} className="text-slate-400 hover:text-slate-600">
                  إلغاء
                </button>
              </div>

              <div className="space-y-1.5">
                <label className="font-bold text-slate-700 dark:text-slate-300">عنوان الرابط / اسم القناة:</label>
                <input
                  type="text"
                  value={formTitle}
                  onChange={(e) => setFormTitle(e.target.value)}
                  placeholder="مثال: قناة الأبحاث العلمية، مجموعة المطورين..."
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-xs focus:ring-2 focus:ring-amber-500 focus:outline-none"
                />
              </div>

              <div className="space-y-1.5">
                <label className="font-bold text-slate-700 dark:text-slate-300">رابط تيليجرام (URL أو معرف @):</label>
                <input
                  type="text"
                  value={formLink}
                  onChange={(e) => setFormLink(e.target.value)}
                  placeholder="https://t.me/channel_name أو https://t.me/+joinCode"
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-xs font-mono focus:ring-2 focus:ring-amber-500 focus:outline-none"
                  dir="ltr"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1.5">
                  <label className="font-bold text-slate-700 dark:text-slate-300">التصنيف أو المجلد:</label>
                  <input
                    type="text"
                    value={formCategory}
                    onChange={(e) => setFormCategory(e.target.value)}
                    placeholder="مثال: تقنية، أكاديمي، إعلانات"
                    className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-xs focus:ring-2 focus:ring-amber-500 focus:outline-none"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="font-bold text-slate-700 dark:text-slate-300">وسوم (Tags):</label>
                  <input
                    type="text"
                    value={formTags}
                    onChange={(e) => setFormTags(e.target.value)}
                    placeholder="مثال: برمجة، كتب، ذكاء اصطناعي"
                    className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-xs focus:ring-2 focus:ring-amber-500 focus:outline-none"
                  />
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="font-bold text-slate-700 dark:text-slate-300">ملاحظات إضافية:</label>
                <textarea
                  value={formNotes}
                  onChange={(e) => setFormNotes(e.target.value)}
                  rows={3}
                  placeholder="أدخل أي ملاحظات أو تفاصيل خاصة بهذه القناة..."
                  className="w-full p-3 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-xs focus:ring-2 focus:ring-amber-500 focus:outline-none resize-none"
                />
              </div>

              <div className="flex items-center gap-2 pt-2">
                <button
                  type="button"
                  onClick={handleSaveForm}
                  className="flex-1 py-2.5 bg-amber-500 hover:bg-amber-600 text-white rounded-xl font-bold flex items-center justify-center gap-1.5 shadow-sm transition-transform active:scale-95"
                >
                  <Check className="w-4 h-4" />
                  <span>{editingId ? "حفظ التعديلات" : "حفظ في البنك"}</span>
                </button>
                <button
                  type="button"
                  onClick={() => setIsAddingNew(false)}
                  className="px-4 py-2.5 bg-slate-200 dark:bg-slate-800 text-slate-700 dark:text-slate-300 rounded-xl font-bold transition-colors"
                >
                  إلغاء
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="p-4 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between bg-slate-50/50 dark:bg-slate-900/50 text-xs">
          <span className="text-[11px] text-slate-400">
            تُحفظ كافة الروابط وتصنيفاتها بشكل دائم ويمكنك تصديرها واستيرادها في أي وقت
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
