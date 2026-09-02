import React, { useState, useEffect } from "react";
import {
  X,
  MessageSquare,
  Plus,
  Trash2,
  Edit2,
  Copy,
  Pin,
  Send,
  Sparkles,
  Check,
  Search,
  Tag,
  Bookmark,
  Share2,
  FolderHeart,
} from "lucide-react";
import { SavedMessageTemplate, TelegramDialog } from "../../types";
import {
  getSavedTemplates,
  saveSavedTemplates,
} from "../../lib/telegramProTools";
import { apiSendMessage, getStoredSession } from "../../lib/telegramApi";


interface MyMessagesModalProps {
  isOpen: boolean;
  onClose: () => void;
  selectedChatId: string | null;
  dialogs: TelegramDialog[];
  onInsertIntoInput?: (text: string) => void;
  onShowToast: (notif: { title: string; body: string; type: "success" | "error" | "system" | "message" }) => void;
}

export const MyMessagesModal: React.FC<MyMessagesModalProps> = ({
  isOpen,
  onClose,
  selectedChatId,
  dialogs,
  onInsertIntoInput,
  onShowToast,
}) => {
  const [templates, setTemplates] = useState<SavedMessageTemplate[]>(() => getSavedTemplates());
  const [activeCategory, setActiveCategory] = useState<string>("all");
  const [searchQuery, setSearchQuery] = useState("");
  const [editingTemplate, setEditingTemplate] = useState<SavedMessageTemplate | null>(null);
  const [isCreatingNew, setIsCreatingNew] = useState(false);

  // Form State
  const [formTitle, setFormTitle] = useState("");
  const [formContent, setFormContent] = useState("");
  const [formCategory, setFormCategory] = useState<SavedMessageTemplate["category"]>("general");
  const [formTags, setFormTags] = useState("");
  const [copiedId, setCopiedId] = useState<string | number | null>(null);

  useEffect(() => {
    if (isOpen) {
      setTemplates(getSavedTemplates());
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const categories = [
    { id: "all", label: "جميع الرسائل" },
    { id: "marketing", label: "تسويق وإعلانات" },
    { id: "quick_reply", label: "ردود سريعة" },
    { id: "academic", label: "أكاديمي ودراسي" },
    { id: "general", label: "عامة" },
    { id: "custom", label: "مخصصة" },
  ];

  const filteredTemplates = templates.filter((t) => {
    if (activeCategory !== "all" && t.category !== activeCategory) return false;
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const matchTitle = t.title.toLowerCase().includes(q);
      const matchContent = (t.content || t.text || "").toLowerCase().includes(q);
      const matchTag = t.tags?.some((tg) => tg.toLowerCase().includes(q));
      if (!matchTitle && !matchContent && !matchTag) return false;
    }
    return true;
  });

  const sortedTemplates = [...filteredTemplates].sort((a, b) => {
    if (a.isPinned && !b.isPinned) return -1;
    if (!a.isPinned && b.isPinned) return 1;
    const timeA = typeof a.createdAt === "number" ? a.createdAt : new Date(a.createdAt || 0).getTime();
    const timeB = typeof b.createdAt === "number" ? b.createdAt : new Date(b.createdAt || 0).getTime();
    return timeB - timeA;
  });

  const handleSaveForm = () => {
    if (!formTitle.trim() || !formContent.trim()) {
      onShowToast({
        title: "بيانات غير مكتملة",
        body: "يرجى ملء عنوان الرسالة ومحتواها.",
        type: "error",
      });
      return;
    }

    const tagList = formTags
      .split(/[,،]/)
      .map((t) => t.trim())
      .filter(Boolean);

    let updatedList: SavedMessageTemplate[];
    if (editingTemplate) {
      updatedList = templates.map((t) =>
        t.id === editingTemplate.id
          ? {
              ...t,
              title: formTitle.trim(),
              content: formContent.trim(),
              category: formCategory,
              tags: tagList,
            }
          : t
      );
      onShowToast({
        title: "تم تحديث القالب",
        body: `تم حفظ التعديلات على "${formTitle}"`,
        type: "success",
      });
    } else {
      const newTpl: SavedMessageTemplate = {
        id: `tpl_${Date.now()}`,
        title: formTitle.trim(),
        content: formContent.trim(),
        category: formCategory,
        tags: tagList,
        createdAt: Date.now(),
        isPinned: false,
      };
      updatedList = [newTpl, ...templates];
      onShowToast({
        title: "تمت إضافة الرسالة بنجاح",
        body: `تم حفظ قالب "${formTitle}" في مكتبة رسائلك`,
        type: "success",
      });
    }

    setTemplates(updatedList);
    saveSavedTemplates(updatedList);
    setIsCreatingNew(false);
    setEditingTemplate(null);
    resetForm();
  };

  const resetForm = () => {
    setFormTitle("");
    setFormContent("");
    setFormCategory("general");
    setFormTags("");
  };

  const handleEditClick = (t: SavedMessageTemplate) => {
    setEditingTemplate(t);
    setFormTitle(t.title);
    setFormContent(t.content);
    setFormCategory(t.category);
    setFormTags(t.tags?.join("، ") || "");
    setIsCreatingNew(true);
  };

  const handleDelete = (id: string | number) => {
    const updated = templates.filter((t) => t.id !== id);
    setTemplates(updated);
    saveSavedTemplates(updated);
    onShowToast({
      title: "تم حذف الرسالة",
      body: "تم إزالة القالب من بنك رسائلك.",
      type: "system",
    });
  };

  const handleTogglePin = (id: string | number) => {
    const updated = templates.map((t) =>
      t.id === id ? { ...t, isPinned: !t.isPinned } : t
    );
    setTemplates(updated);
    saveSavedTemplates(updated);
  };

  const handleCopyContent = (t: SavedMessageTemplate) => {
    navigator.clipboard.writeText(t.content || t.text || "");
    setCopiedId(t.id);
    setTimeout(() => setCopiedId(null), 2000);
    onShowToast({
      title: "تم نسخ النص",
      body: "تم نسخ محتوى الرسالة إلى الحافظة بنجاح.",
      type: "success",
    });
  };

  const handleInsertOrSend = async (content: string, directSend = false) => {
    if (directSend && selectedChatId) {
      const activeChat = dialogs.find((d) => d.id === selectedChatId);
      try {
        await apiSendMessage({
          sessionString: getStoredSession() || "",
          chatId: selectedChatId,
          message: content,
        });
        onShowToast({
          title: "تم إرسال الرسالة",
          body: `تم الإرسال فوراً إلى ${activeChat?.title || "المحادثة الحالية"}`,
          type: "success",
        });
        onClose();
      } catch (err: any) {

        onShowToast({
          title: "فشل الإرسال",
          body: err.message || "حدث خطأ أثناء الإرسال",
          type: "error",
        });
      }
    } else {
      if (onInsertIntoInput) {
        onInsertIntoInput(content);
        onShowToast({
          title: "تم إدراج الرسالة",
          body: "تم إدراج النص في حقل الكتابة.",
          type: "success",
        });
        onClose();
      } else {
        handleCopyContent({ content } as any);
      }
    }
  };

  const handleInsertVariable = (varName: string) => {
    setFormContent((prev) => `${prev} {${varName}}`);
  };

  return (
    <div
      id="my-messages-modal-backdrop"
      className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-3 animate-fade-in"
      dir="rtl"
    >
      <div
        id="my-messages-modal-card"
        className="w-full max-w-2xl bg-white dark:bg-slate-900 rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-800 flex flex-col max-h-[90vh] overflow-hidden animate-scale-up"
      >
        {/* Modal Header */}
        <div className="p-4 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between bg-gradient-to-r from-emerald-500/10 via-teal-500/5 to-transparent">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-emerald-500/10 text-emerald-500 flex items-center justify-center">
              <MessageSquare className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="font-bold text-sm text-slate-800 dark:text-white">
                  رسائلي وقوالب النشر (My Messages & Templates)
                </h2>
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-100 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400">
                  {templates.length} قوالب محفوظة
                </span>
              </div>
              <p className="text-xs text-slate-400">
                بنك الرسائل والنصوص الجاهزة للنشر والردود السريعة والإعلانات
              </p>
            </div>
          </div>
          <button
            id="close-my-messages-modal-btn"
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-slate-600 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {!isCreatingNew ? (
            <>
              {/* Search and New Template Action */}
              <div className="flex items-center gap-2">
                <div className="flex-1 relative">
                  <Search className="w-4 h-4 absolute right-3 top-1/2 -translate-y-1/2 text-slate-400" />
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="بحث في القوالب والنصوص المحفوظة..."
                    className="w-full pr-9 pl-4 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-xs focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
                <button
                  id="add-new-template-btn"
                  onClick={() => {
                    resetForm();
                    setEditingTemplate(null);
                    setIsCreatingNew(true);
                  }}
                  className="px-3 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-xl text-xs font-bold flex items-center gap-1.5 shadow-sm transition-transform active:scale-95 shrink-0"
                >
                  <Plus className="w-4 h-4" />
                  <span>قالب جديد</span>
                </button>
              </div>

              {/* Category Pills Bar */}
              <div className="flex items-center gap-1.5 overflow-x-auto pb-1 text-xs">
                {categories.map((cat) => (
                  <button
                    key={cat.id}
                    onClick={() => setActiveCategory(cat.id)}
                    className={`px-3 py-1.5 rounded-lg whitespace-nowrap transition-colors font-medium ${
                      activeCategory === cat.id
                        ? "bg-emerald-500 text-white font-bold shadow-xs"
                        : "bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 hover:bg-slate-200 dark:hover:bg-slate-700"
                    }`}
                  >
                    {cat.label}
                  </button>
                ))}
              </div>

              {/* Templates List */}
              <div className="space-y-3">
                {sortedTemplates.length === 0 ? (
                  <div className="text-center py-16 text-slate-400 text-xs">
                    <MessageSquare className="w-8 h-8 mx-auto mb-2 text-slate-300 dark:text-slate-700" />
                    <span>لا توجد رسائل محفوظة في هذا القسم. اضغط على "قالب جديد" لإضافة رسالة.</span>
                  </div>
                ) : (
                  sortedTemplates.map((t) => (
                    <div
                      key={t.id}
                      className={`p-3.5 rounded-2xl border transition-all space-y-2 relative group ${
                        t.isPinned
                          ? "bg-emerald-50/40 dark:bg-emerald-950/20 border-emerald-200 dark:border-emerald-900/60"
                          : "bg-slate-50/70 dark:bg-slate-800/60 border-slate-200 dark:border-slate-700/80 hover:border-slate-300"
                      }`}
                    >
                      {/* Item Header */}
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <h3 className="font-bold text-xs text-slate-800 dark:text-white flex items-center gap-1.5">
                            {t.title}
                            {t.isPinned && <Pin className="w-3.5 h-3.5 text-amber-500 fill-amber-500" />}
                          </h3>
                          <span className="text-[10px] px-2 py-0.5 rounded-md bg-slate-200/80 dark:bg-slate-700 text-slate-600 dark:text-slate-300">
                            {categories.find((c) => c.id === t.category)?.label || t.category}
                          </span>
                        </div>

                        {/* Top Action Icons */}
                        <div className="flex items-center gap-1">
                          <button
                            onClick={() => handleTogglePin(t.id)}
                            title={t.isPinned ? "إلغاء التثبيت" : "تثبيت في الأعلى"}
                            className={`p-1.5 rounded-lg text-xs transition-colors ${
                              t.isPinned ? "text-amber-500 bg-amber-100 dark:bg-amber-900/40" : "text-slate-400 hover:text-amber-500"
                            }`}
                          >
                            <Pin className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => handleEditClick(t)}
                            title="تعديل القالب"
                            className="p-1.5 text-slate-400 hover:text-sky-500 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
                          >
                            <Edit2 className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => handleDelete(t.id)}
                            title="حذف القالب"
                            className="p-1.5 text-slate-400 hover:text-red-500 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </div>

                      {/* Content Preview */}
                      <p className="text-xs text-slate-600 dark:text-slate-300 whitespace-pre-line leading-relaxed bg-white/70 dark:bg-slate-900/70 p-2.5 rounded-xl border border-slate-100 dark:border-slate-800 font-sans">
                        {t.content}
                      </p>

                      {/* Tags & Action Buttons */}
                      <div className="flex items-center justify-between pt-1">
                        <div className="flex flex-wrap gap-1">
                          {t.tags?.map((tg, i) => (
                            <span
                              key={i}
                              className="text-[10px] text-slate-500 dark:text-slate-400 flex items-center gap-0.5"
                            >
                              <Tag className="w-2.5 h-2.5" />
                              {tg}
                            </span>
                          ))}
                        </div>

                        <div className="flex items-center gap-1.5">
                          <button
                            onClick={() => handleCopyContent(t)}
                            className="px-2.5 py-1.5 bg-slate-200 dark:bg-slate-700 hover:bg-slate-300 dark:hover:bg-slate-600 text-slate-700 dark:text-slate-200 rounded-lg text-xs font-medium flex items-center gap-1 transition-colors"
                          >
                            {copiedId === t.id ? (
                              <>
                                <Check className="w-3.5 h-3.5 text-emerald-500" />
                                <span>تم النسخ</span>
                              </>
                            ) : (
                              <>
                                <Copy className="w-3.5 h-3.5" />
                                <span>نسخ</span>
                              </>
                            )}
                          </button>

                          <button
                            onClick={() => handleInsertOrSend(t.content, false)}
                            className="px-2.5 py-1.5 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg text-xs font-bold flex items-center gap-1 transition-colors"
                          >
                            <Send className="w-3.5 h-3.5" />
                            <span>إدراج في المحادثة</span>
                          </button>

                          {selectedChatId && (
                            <button
                              onClick={() => handleInsertOrSend(t.content, true)}
                              className="px-2.5 py-1.5 bg-sky-500 hover:bg-sky-600 text-white rounded-lg text-xs font-bold flex items-center gap-1 transition-colors"
                              title="إرسال فوري إلى المحادثة الحالية"
                            >
                              <span>إرسال فوري ⚡</span>
                            </button>
                          )}
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </>
          ) : (
            /* Create / Edit Form */
            <div className="space-y-4 text-xs">
              <div className="flex items-center justify-between border-b border-slate-100 dark:border-slate-800 pb-3">
                <h3 className="font-bold text-sm text-slate-800 dark:text-white">
                  {editingTemplate ? "تعديل القالب" : "إنشاء رسالة جديدة أو قالب"}
                </h3>
                <button
                  onClick={() => setIsCreatingNew(false)}
                  className="text-xs text-slate-400 hover:text-slate-600"
                >
                  إلغاء والعودة للقائمة
                </button>
              </div>

              <div className="space-y-1.5">
                <label className="font-bold text-slate-700 dark:text-slate-300">عنوان القالب:</label>
                <input
                  type="text"
                  value={formTitle}
                  onChange={(e) => setFormTitle(e.target.value)}
                  placeholder="مثال: رسالة ترحيبية، كود خصم، رد استفسار..."
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-xs focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1.5">
                  <label className="font-bold text-slate-700 dark:text-slate-300">التصنيف:</label>
                  <select
                    value={formCategory}
                    onChange={(e) => setFormCategory(e.target.value as any)}
                    className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-xs focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  >
                    <option value="general">عامة</option>
                    <option value="marketing">تسويق وإعلانات</option>
                    <option value="quick_reply">ردود سريعة</option>
                    <option value="academic">أكاديمي ودراسي</option>
                    <option value="custom">مخصصة</option>
                  </select>
                </div>

                <div className="space-y-1.5">
                  <label className="font-bold text-slate-700 dark:text-slate-300">وسوم للبحث (مفصولة بفاصلة):</label>
                  <input
                    type="text"
                    value={formTags}
                    onChange={(e) => setFormTags(e.target.value)}
                    placeholder="مثال: مهم، إعلان، عملاء"
                    className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-xs focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
              </div>

              {/* Text Area and Variable chips */}
              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <label className="font-bold text-slate-700 dark:text-slate-300">محتوى الرسالة:</label>
                  <div className="flex items-center gap-1 text-[11px] text-slate-400">
                    <span>متغيرات ذكية:</span>
                    {["name", "date", "time", "chat"].map((v) => (
                      <button
                        key={v}
                        type="button"
                        onClick={() => handleInsertVariable(v)}
                        className="px-1.5 py-0.5 bg-slate-100 dark:bg-slate-800 hover:bg-emerald-100 dark:hover:bg-emerald-950/50 hover:text-emerald-600 rounded text-[10px] font-mono transition-colors"
                      >
                        +{v}
                      </button>
                    ))}
                  </div>
                </div>
                <textarea
                  value={formContent}
                  onChange={(e) => setFormContent(e.target.value)}
                  rows={6}
                  placeholder="اكتب نص الرسالة هنا، يمكنك استخدام التنسيقات والرموز التعبيرية والروابط..."
                  className="w-full p-3 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-xs focus:ring-2 focus:ring-emerald-500 focus:outline-none resize-none leading-relaxed"
                />
              </div>

              <div className="flex items-center gap-2 pt-2">
                <button
                  type="button"
                  onClick={handleSaveForm}
                  className="flex-1 py-2.5 bg-emerald-500 hover:bg-emerald-600 text-white rounded-xl font-bold flex items-center justify-center gap-1.5 shadow-sm transition-transform active:scale-95"
                >
                  <Check className="w-4 h-4" />
                  <span>{editingTemplate ? "حفظ التعديلات" : "إضافة إلى بنك الرسائل"}</span>
                </button>
                <button
                  type="button"
                  onClick={() => setIsCreatingNew(false)}
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
            تُحفظ جميع الرسائل بشكل دائم وتكون متاحة للاستخدام بضغطة زر
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
