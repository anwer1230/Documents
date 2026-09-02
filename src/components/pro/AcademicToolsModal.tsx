import React, { useState, useEffect } from "react";
import {
  X,
  GraduationCap,
  FileText,
  Video,
  Download,
  Search,
  Sparkles,
  RefreshCw,
  FolderOpen,
  Filter,
  FileCode,
  Layers,
  BookOpen,
  Eye,
  CheckCircle2,
} from "lucide-react";
import { AcademicResourceItem, AcademicExtractConfig, TelegramDialog } from "../../types";
import {
  getAcademicConfig,
  saveAcademicConfig,
  getAcademicResources,
  saveAcademicResources,
} from "../../lib/telegramProTools";

interface AcademicToolsModalProps {
  isOpen: boolean;
  onClose: () => void;
  dialogs: TelegramDialog[];
  onShowToast: (notif: { title: string; body: string; type: "success" | "error" | "system" | "message" }) => void;
}

export const AcademicToolsModal: React.FC<AcademicToolsModalProps> = ({
  isOpen,
  onClose,
  dialogs,
  onShowToast,
}) => {
  const [config, setConfig] = useState<AcademicExtractConfig>(() => getAcademicConfig());
  const [resources, setResources] = useState<AcademicResourceItem[]>(() => getAcademicResources());
  const [activeTab, setActiveTab] = useState<"library" | "scanner" | "summary">("library");
  const [selectedFileType, setSelectedFileType] = useState<string>("all");
  const [searchQuery, setSearchQuery] = useState("");
  const [isScanning, setIsScanning] = useState(false);
  const [selectedItemForSummary, setSelectedItemForSummary] = useState<AcademicResourceItem | null>(null);
  const [aiSummaryResult, setAiSummaryResult] = useState<string>("");
  const [isSummarizing, setIsSummarizing] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setConfig(getAcademicConfig());
      setResources(getAcademicResources());
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const formatFileSize = (bytes?: number | string) => {
    if (!bytes) return "حجم غير معروف";
    const num = typeof bytes === "string" ? parseFloat(bytes) : bytes;
    if (isNaN(num)) return String(bytes);
    if (num >= 1024 * 1024 * 1024) return `${(num / (1024 * 1024 * 1024)).toFixed(1)} GB`;
    if (num >= 1024 * 1024) return `${(num / (1024 * 1024)).toFixed(1)} MB`;
    if (num >= 1024) return `${(num / 1024).toFixed(0)} KB`;
    return `${num} B`;
  };

  const filteredResources = resources.filter((res) => {
    if (selectedFileType !== "all" && res.fileType !== selectedFileType) return false;
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const matchTitle = res.title.toLowerCase().includes(q);
      const matchFileName = res.fileName?.toLowerCase().includes(q);
      const matchChat = res.chatTitle.toLowerCase().includes(q);
      const matchSummary = res.summary?.toLowerCase().includes(q);
      if (!matchTitle && !matchFileName && !matchChat && !matchSummary) return false;
    }
    return true;
  });

  const handleStartScan = () => {
    setIsScanning(true);
    onShowToast({
      title: "بدء فحص القنوات الأكاديمية",
      body: "جاري البحث عن ملفات PDF، والمحاضرات، والمستندات في المحادثات المحددة...",
      type: "system",
    });

    setTimeout(() => {
      // Simulate discovering new academic resources
      const newItems: AcademicResourceItem[] = [
        {
          id: `acad_${Date.now()}_1`,
          title: "محاضرة أساسيات أمن المعلومات والتشفير",
          fileName: "CyberSecurity_Fundamentals_2026.pdf",
          fileType: "pdf",
          fileSize: 18500000,
          chatTitle: dialogs[0]?.title || "قناة علوم الحاسوب",
          chatId: dialogs[0]?.id || "777000",
          messageId: 884,
          date: Math.floor(Date.now() / 1000),
          summary: "مرجع شامل في مبادئ التشفير المتماثل وغير المتماثل وبروتوكولات أمان الشبكات.",
        },
        {
          id: `acad_${Date.now()}_2`,
          title: "ملف تدريبات ومسائل البرمجة التنافسية",
          fileName: "Competitive_Programming_CheatSheet.zip",
          fileType: "zip",
          fileSize: 32400000,
          chatTitle: dialogs[1]?.title || "مجموعة المطورين",
          chatId: dialogs[1]?.id || "777000",
          messageId: 910,
          date: Math.floor(Date.now() / 1000),
          summary: "حزمة برمجية تحتوي على مسائل محلولة بلغات C++ و Python لمسابقات البرمجة.",
        },
      ];

      const updated = [...newItems, ...resources];
      setResources(updated);
      saveAcademicResources(updated);
      setIsScanning(false);

      onShowToast({
        title: "اكتمل الفحص الأكاديمي 🎉",
        body: `تم استخراج ${newItems.length} مصادر تعليمية جديدة بنجاح.`,
        type: "success",
      });
      setActiveTab("library");
    }, 2000);
  };

  const handleGenerateSummary = (res: AcademicResourceItem) => {
    setSelectedItemForSummary(res);
    setIsSummarizing(true);
    setAiSummaryResult("");
    setActiveTab("summary");

    setTimeout(() => {
      setIsSummarizing(false);
      setAiSummaryResult(
        `📌 **ملخص ذكي للمستند الأكاديمي:**\n\n🔹 **الموضوع الرئيسي:** ${res.title}\n🔹 **الملف:** ${res.fileName}\n🔹 **الحجم:** ${formatFileSize(res.fileSize)}\n\n💡 **أهم النقاط المستخلصة:**\n1. يغطي المرجع المفاهيم النظرية والتطبيقية بطريقة مبسطة.\n2. يتضمن أمثلة وتمارين عملية تساعد على الفهم السريع.\n3. موصى به للطلاب والباحثين في هذا المجال.\n\n✨ تم التوليد بنجاح بواسطة المساعد الأكاديمي الذكي.`
      );
    }, 1200);
  };

  const handleDownload = (item: AcademicResourceItem) => {
    onShowToast({
      title: "بدء التنزيل",
      body: `جاري تحميل الملف: ${item.fileName || item.title}`,
      type: "success",
    });
    // Create mock download trigger
    const link = document.createElement("a");
    link.href = `/api/telegram/file/${item.chatId}/${item.messageId}`;
    link.setAttribute("download", item.fileName || "document.pdf");
    link.click();
  };

  return (
    <div
      id="academic-tools-modal-backdrop"
      className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-3 animate-fade-in"
      dir="rtl"
    >
      <div
        id="academic-tools-modal-card"
        className="w-full max-w-2xl bg-white dark:bg-slate-900 rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-800 flex flex-col max-h-[90vh] overflow-hidden animate-scale-up"
      >
        {/* Modal Header */}
        <div className="p-4 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between bg-gradient-to-r from-purple-500/10 via-indigo-500/5 to-transparent">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-purple-500/10 text-purple-500 flex items-center justify-center">
              <GraduationCap className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="font-bold text-sm text-slate-800 dark:text-white">
                  الأدوات الأكاديمية والملفات (Academic Tools & Extractor)
                </h2>
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-purple-100 dark:bg-purple-950/60 text-purple-600 dark:text-purple-400">
                  {resources.length} مرجع متاح
                </span>
              </div>
              <p className="text-xs text-slate-400">
                استخراج الكتب والمحاضرات والمراجع PDF من قنواتك مع التلخيص الذكي
              </p>
            </div>
          </div>
          <button
            id="close-academic-tools-modal-btn"
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-slate-600 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Modal Tabs */}
        <div className="flex items-center border-b border-slate-100 dark:border-slate-800 bg-slate-50 dark:bg-slate-900/50 px-4 text-xs font-medium">
          <button
            onClick={() => setActiveTab("library")}
            className={`py-3 px-4 flex items-center gap-2 border-b-2 transition-colors ${
              activeTab === "library"
                ? "border-purple-500 text-purple-600 dark:text-purple-400 font-bold"
                : "border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-200"
            }`}
          >
            <BookOpen className="w-3.5 h-3.5" />
            <span>مكتبة المراجع والملفات ({resources.length})</span>
          </button>
          <button
            onClick={() => setActiveTab("scanner")}
            className={`py-3 px-4 flex items-center gap-2 border-b-2 transition-colors ${
              activeTab === "scanner"
                ? "border-purple-500 text-purple-600 dark:text-purple-400 font-bold"
                : "border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-200"
            }`}
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isScanning ? "animate-spin text-purple-500" : ""}`} />
            <span>فحص واستخراج المراجع من القنوات</span>
          </button>
          <button
            onClick={() => setActiveTab("summary")}
            className={`py-3 px-4 flex items-center gap-2 border-b-2 transition-colors ${
              activeTab === "summary"
                ? "border-purple-500 text-purple-600 dark:text-purple-400 font-bold"
                : "border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-200"
            }`}
          >
            <Sparkles className="w-3.5 h-3.5" />
            <span>التلخيص الذكي بالذكاء الاصطناعي</span>
          </button>
        </div>

        {/* Modal Body */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4 text-xs">
          {activeTab === "library" && (
            <div className="space-y-4">
              {/* Search & File Filters */}
              <div className="flex items-center gap-2">
                <div className="flex-1 relative">
                  <Search className="w-4 h-4 absolute right-3 top-1/2 -translate-y-1/2 text-slate-400" />
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="ابحث في أسماء الكتب، المحاضرات، المقررات..."
                    className="w-full pr-9 pl-4 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-xs focus:ring-2 focus:ring-purple-500 focus:outline-none"
                  />
                </div>

                <div className="flex items-center gap-1">
                  {[
                    { id: "all", label: "الكل" },
                    { id: "pdf", label: "PDF" },
                    { id: "video", label: "فيديو" },
                    { id: "zip", label: "ZIP" },
                  ].map((ft) => (
                    <button
                      key={ft.id}
                      onClick={() => setSelectedFileType(ft.id)}
                      className={`px-2.5 py-2 rounded-xl font-medium transition-colors ${
                        selectedFileType === ft.id
                          ? "bg-purple-500 text-white font-bold"
                          : "bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 hover:bg-slate-200"
                      }`}
                    >
                      {ft.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* Resource Cards */}
              <div className="space-y-3">
                {filteredResources.length === 0 ? (
                  <div className="text-center py-16 text-slate-400">
                    <GraduationCap className="w-8 h-8 mx-auto mb-2 text-slate-300 dark:text-slate-700" />
                    <span>لا توجد ملفات أكاديمية مطابقة. قم بفحص قنواتك لاستخراج الملفات.</span>
                  </div>
                ) : (
                  filteredResources.map((res) => (
                    <div
                      key={res.id}
                      className="p-3.5 bg-slate-50/80 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 rounded-2xl space-y-2.5 transition-all hover:border-purple-300 dark:hover:border-purple-700"
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div className="flex items-start gap-2.5 min-w-0">
                          <div className="w-10 h-10 rounded-xl bg-purple-100 dark:bg-purple-950/60 text-purple-600 dark:text-purple-300 flex items-center justify-center shrink-0">
                            {res.fileType === "pdf" ? (
                              <FileText className="w-5 h-5" />
                            ) : res.fileType === "video" ? (
                              <Video className="w-5 h-5" />
                            ) : (
                              <FileCode className="w-5 h-5" />
                            )}
                          </div>
                          <div className="min-w-0">
                            <h3 className="font-bold text-xs text-slate-800 dark:text-white truncate">
                              {res.title}
                            </h3>
                            <div className="flex items-center gap-2 text-[10px] text-slate-400 mt-0.5">
                              <span className="font-mono">{res.fileName}</span>
                              <span>•</span>
                              <span>{formatFileSize(res.fileSize)}</span>
                              <span>•</span>
                              <span>{res.chatTitle}</span>
                            </div>
                          </div>
                        </div>

                        <span className="text-[10px] px-2 py-0.5 rounded-md bg-purple-100 dark:bg-purple-900/60 text-purple-700 dark:text-purple-300 font-bold uppercase shrink-0">
                          {res.fileType}
                        </span>
                      </div>

                      {res.summary && (
                        <p className="text-xs text-slate-600 dark:text-slate-300 leading-relaxed bg-white/60 dark:bg-slate-900/60 p-2.5 rounded-xl border border-slate-100 dark:border-slate-800/80">
                          {res.summary}
                        </p>
                      )}

                      <div className="flex items-center justify-end gap-2 pt-1 border-t border-slate-200/60 dark:border-slate-700/60">
                        <button
                          onClick={() => handleGenerateSummary(res)}
                          className="px-3 py-1.5 bg-purple-50 dark:bg-purple-950/50 hover:bg-purple-100 text-purple-600 dark:text-purple-300 border border-purple-200 dark:border-purple-800 rounded-lg text-xs font-medium flex items-center gap-1 transition-colors"
                        >
                          <Sparkles className="w-3.5 h-3.5" />
                          <span>تلخيص ذكي</span>
                        </button>

                        <button
                          onClick={() => handleDownload(res)}
                          className="px-3 py-1.5 bg-purple-500 hover:bg-purple-600 text-white rounded-lg text-xs font-bold flex items-center gap-1 shadow-sm transition-transform active:scale-95"
                        >
                          <Download className="w-3.5 h-3.5" />
                          <span>تحميل الملف</span>
                        </button>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

          {activeTab === "scanner" && (
            <div className="space-y-4">
              <div className="p-3.5 bg-purple-50/60 dark:bg-purple-950/20 border border-purple-100 dark:border-purple-900/50 rounded-2xl space-y-1">
                <h4 className="font-bold text-slate-800 dark:text-white flex items-center gap-1.5">
                  <RefreshCw className="w-4 h-4 text-purple-500" />
                  <span>فاحص القنوات والمجموعات الأكاديمي</span>
                </h4>
                <p className="text-slate-500 dark:text-slate-400 text-[11px] leading-relaxed">
                  يقوم الماسح الضوئي بفحص سجل الرسائل في القنوات والمجموعات لاستخراج ملفات الكتب والملفات الأكاديمية وتصنيفها تلقائياً.
                </p>
              </div>

              <div className="space-y-2">
                <label className="font-bold text-slate-700 dark:text-slate-300">
                  صيغ الملفات المستهدفة بالاستخراج:
                </label>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                  {[
                    { id: "pdf", label: "مستندات PDF (.pdf)" },
                    { id: "document", label: "وورد (.docx)" },
                    { id: "video", label: "محاضرات (.mp4)" },
                    { id: "zip", label: "حزم (.zip / .rar)" },
                  ].map((f) => (
                    <div
                      key={f.id}
                      className="p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl flex items-center gap-2"
                    >
                      <input type="checkbox" defaultChecked className="rounded text-purple-500 focus:ring-0" />
                      <span className="font-medium text-xs text-slate-700 dark:text-slate-300">{f.label}</span>
                    </div>
                  ))}
                </div>
              </div>

              <div className="space-y-2">
                <label className="font-bold text-slate-700 dark:text-slate-300">
                  اختر القنوات المراد فحصها ({dialogs.length} قناة ومجموعة):
                </label>
                <div className="max-h-40 overflow-y-auto space-y-1 p-2 bg-slate-50 dark:bg-slate-800/60 rounded-xl border border-slate-200 dark:border-slate-700">
                  {dialogs.map((d) => (
                    <div
                      key={d.id}
                      className="flex items-center justify-between p-2 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300"
                    >
                      <div className="flex items-center gap-2 truncate">
                        <input type="checkbox" defaultChecked className="rounded text-purple-500 focus:ring-0" />
                        <span className="font-medium truncate">{d.title}</span>
                      </div>
                      <span className="text-[10px] text-slate-400 shrink-0">({d.type})</span>
                    </div>
                  ))}
                </div>
              </div>

              <button
                onClick={handleStartScan}
                disabled={isScanning}
                className="w-full py-2.5 bg-purple-500 hover:bg-purple-600 disabled:opacity-50 text-white rounded-xl font-bold flex items-center justify-center gap-2 shadow-sm transition-transform active:scale-95"
              >
                <RefreshCw className={`w-4 h-4 ${isScanning ? "animate-spin" : ""}`} />
                <span>{isScanning ? "جاري الفحص واستخراج الملفات..." : "بدء استخراج الملفات الأكاديمية الآن"}</span>
              </button>
            </div>
          )}

          {activeTab === "summary" && (
            <div className="space-y-4">
              <div className="p-3.5 bg-purple-50 dark:bg-purple-950/20 border border-purple-200 dark:border-purple-900/50 rounded-2xl">
                <h4 className="font-bold text-purple-800 dark:text-purple-300 flex items-center gap-1.5 mb-1">
                  <Sparkles className="w-4 h-4 text-purple-500" />
                  <span>محرك التلخيص الأكاديمي</span>
                </h4>
                <p className="text-[11px] text-purple-700 dark:text-purple-300">
                  تلخيص فوري للكتب والمذكرات الأكاديمية واستخراج النقاط الأساسية منها.
                </p>
              </div>

              {selectedItemForSummary ? (
                <div className="space-y-3">
                  <div className="p-3 bg-slate-50 dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 flex items-center justify-between">
                    <div>
                      <span className="text-[10px] text-slate-400">الملف المحدد:</span>
                      <h4 className="font-bold text-xs text-slate-800 dark:text-white">{selectedItemForSummary.title}</h4>
                    </div>
                    <button
                      onClick={() => handleGenerateSummary(selectedItemForSummary)}
                      className="px-3 py-1.5 bg-purple-500 text-white rounded-lg font-bold text-xs flex items-center gap-1"
                    >
                      <RefreshCw className="w-3.5 h-3.5" />
                      <span>إعادة التلخيص</span>
                    </button>
                  </div>

                  {isSummarizing ? (
                    <div className="p-8 text-center space-y-2 bg-slate-50 dark:bg-slate-800/50 rounded-2xl border border-slate-200 dark:border-slate-700">
                      <Sparkles className="w-8 h-8 mx-auto text-purple-500 animate-spin" />
                      <p className="font-bold text-xs text-slate-700 dark:text-slate-300">
                        جاري تحليل المستند وتوليد الملخص الذكي...
                      </p>
                    </div>
                  ) : (
                    aiSummaryResult && (
                      <div className="p-4 bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 space-y-2 leading-relaxed whitespace-pre-line text-slate-700 dark:text-slate-200 font-sans text-xs">
                        {aiSummaryResult}
                      </div>
                    )
                  )}
                </div>
              ) : (
                <div className="text-center py-12 text-slate-400">
                  <span>اختر أي ملف من تبويب "مكتبة المراجع" واضغط على "تلخيص ذكي" لعرض محتواه هنا.</span>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="p-4 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between bg-slate-50/50 dark:bg-slate-900/50 text-xs">
          <span className="text-[11px] text-slate-400">
            يتم حفظ المراجع المستخرجة تلقائياً للرجوع إليها في أي وقت دون إعادة الفحص
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
