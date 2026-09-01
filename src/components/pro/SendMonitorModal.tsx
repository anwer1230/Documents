import React, { useState, useEffect } from "react";
import {
  X,
  Radio,
  Play,
  Square,
  RefreshCw,
  Plus,
  Trash2,
  Send,
  MessageSquare,
  Layers,
  Shield,
  Activity,
  History,
  Clock,
  Sparkles,
  AlertCircle,
  CheckCircle2,
  Sliders,
  Calendar,
  AlertTriangle,
  RotateCcw,
  Bot,
  Filter,
  Check,
  Zap,
  Save,
  Download,
  Upload,
  Database,
} from "lucide-react";
import {
  SendMonitorConfig,
  SendMonitorLog,
  TelegramDialog,
  ProtectedGroupAction,
  SendScheduleType,
} from "../../types";
import {
  getSendMonitorConfig,
  saveSendMonitorConfig,
  getSendMonitorLogs,
  addSendMonitorLog,
  saveSendMonitorLogs,
  DEFAULT_MONITOR_KEYWORDS,
  sanitizeMessage,
} from "../../lib/telegramProTools";
import { apiSendMessage, getStoredSession } from "../../lib/telegramApi";

interface SendMonitorModalProps {
  isOpen: boolean;
  onClose: () => void;
  dialogs: TelegramDialog[];
  onShowToast: (notif: {
    title: string;
    body: string;
    type: "success" | "error" | "system" | "message";
  }) => void;
}

export const SendMonitorModal: React.FC<SendMonitorModalProps> = ({
  isOpen,
  onClose,
  dialogs,
  onShowToast,
}) => {
  const [activeTab, setActiveTab] = useState<
    "settings" | "protection" | "broadcast" | "logs" | "test"
  >("settings");
  const [config, setConfig] = useState<SendMonitorConfig>(() =>
    getSendMonitorConfig()
  );
  const [logs, setLogs] = useState<SendMonitorLog[]>(() =>
    getSendMonitorLogs()
  );
  const [isRunning, setIsRunning] = useState(false);
  const [newKeyword, setNewKeyword] = useState("");
  const [bulkKeywordsText, setBulkKeywordsText] = useState("");
  const [showBulkInput, setShowBulkInput] = useState(false);
  const [selectedChatForTest, setSelectedChatForTest] = useState<string>("");
  const [statusMessage, setStatusMessage] = useState<string>("");
  const [isSavedSuccess, setIsSavedSuccess] = useState(false);

  useEffect(() => {
    if (isOpen) {
      const saved = getSendMonitorConfig();
      setConfig(saved);
      setIsRunning(saved.enabled);
      setLogs(getSendMonitorLogs());
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleSaveConfig = (updated: SendMonitorConfig) => {
    setConfig(updated);
    saveSendMonitorConfig(updated);
    setIsSavedSuccess(true);
    setTimeout(() => setIsSavedSuccess(false), 2500);
  };

  const handleExplicitSave = () => {
    saveSendMonitorConfig(config);
    setIsSavedSuccess(true);
    onShowToast({
      title: "تم حفظ الإعدادات في التخزين الدائم",
      body: `تم حفظ ${config.keywords.length} كلمة رصد ووضع الحماية [${config.protectedGroupAction}] بنجاح`,
      type: "success",
    });
    setTimeout(() => setIsSavedSuccess(false), 3000);
  };

  const handleToggleRunning = () => {
    const nextState = !isRunning;
    setIsRunning(nextState);
    const updated = {
      ...config,
      enabled: nextState,
      lastRunTimestamp: Date.now(),
    };
    handleSaveConfig(updated);

    if (nextState) {
      onShowToast({
        title: "تم تشغيل نظام الإرسال والمراقبة المستمر",
        body: `مراقبة ${config.keywords.length} كلمة مفتاحية | حماية: ${config.protectedGroupAction}`,
        type: "success",
      });
      addSendMonitorLog({
        chatId: "system",
        chatTitle: "النظام المركزي",
        actionTaken: "بدء تشغيل خدمة المراقبة والإرسال في الخلفية",
        status: "success",
        details: `مراقبة ${config.keywords.length} كلمة مفتاحية | وضع الحماية: ${config.protectedGroupAction} | نمط الإرسال: ${config.sendType}`,
      });
      setLogs(getSendMonitorLogs());
    } else {
      onShowToast({
        title: "تم إيقاف خدمة الإرسال والمراقبة",
        body: "تم إيقاف المراقبة المؤقتة في الخلفية.",
        type: "system",
      });
      addSendMonitorLog({
        chatId: "system",
        chatTitle: "النظام المركزي",
        actionTaken: "إيقاف المراقبة يدوياً",
        status: "success",
      });
      setLogs(getSendMonitorLogs());
    }
  };

  const handleAddKeyword = () => {
    if (!newKeyword.trim()) return;
    const clean = newKeyword.trim();
    if (config.keywords.includes(clean)) {
      setNewKeyword("");
      return;
    }
    const updated = {
      ...config,
      keywords: [...config.keywords, clean],
    };
    handleSaveConfig(updated);
    setNewKeyword("");
  };

  const handleAddBulkKeywords = () => {
    if (!bulkKeywordsText.trim()) return;
    const items = bulkKeywordsText
      .split(/[\n,]+/)
      .map((k) => k.trim())
      .filter((k) => k.length > 0);

    const merged = Array.from(new Set([...config.keywords, ...items]));
    const updated = { ...config, keywords: merged };
    handleSaveConfig(updated);
    setBulkKeywordsText("");
    setShowBulkInput(false);
    onShowToast({
      title: "تمت إضافة الكلمات الجماعية",
      body: `إجمالي الكلمات المخزنة الآن: ${merged.length} كلمة`,
      type: "success",
    });
  };

  const handleRemoveKeyword = (kw: string) => {
    const updated = {
      ...config,
      keywords: config.keywords.filter((k) => k !== kw),
    };
    handleSaveConfig(updated);
  };

  const handleRestoreDefaultKeywords = () => {
    const merged = Array.from(
      new Set([...config.keywords, ...DEFAULT_MONITOR_KEYWORDS])
    );
    const updated = { ...config, keywords: merged };
    handleSaveConfig(updated);
    onShowToast({
      title: "تم استعادة وتثبيت الكلمات القياسية",
      body: `تم حفظ ${merged.length} كلمة مفتاحية دائمة في النظام.`,
      type: "success",
    });
  };

  const handleAddPresetCategory = (keywordsList: string[], categoryName: string) => {
    const merged = Array.from(new Set([...config.keywords, ...keywordsList]));
    const updated = { ...config, keywords: merged };
    handleSaveConfig(updated);
    onShowToast({
      title: `تم تضمين كلمات ${categoryName}`,
      body: `تمت إضافة ${keywordsList.length} كلمة وحفظها في التخزين الدائم.`,
      type: "success",
    });
  };

  const handleToggleTargetChat = (chatId: string) => {
    let updatedIds: string[];
    if (config.targetChatIds.includes(chatId)) {
      updatedIds = config.targetChatIds.filter((id) => id !== chatId);
    } else {
      updatedIds = [...config.targetChatIds, chatId];
    }
    const updated = { ...config, targetChatIds: updatedIds };
    handleSaveConfig(updated);
  };

  const handleSelectAllChannels = () => {
    const channelIds = dialogs
      .filter((d) => d.type === "channel" || d.type === "group")
      .map((d) => String(d.id));
    const updated = { ...config, targetChatIds: channelIds };
    handleSaveConfig(updated);
  };

  const handleClearSelectedChats = () => {
    const updated = { ...config, targetChatIds: [] };
    handleSaveConfig(updated);
  };

  const handleTestTrigger = async () => {
    if (!selectedChatForTest) {
      setStatusMessage("يرجى اختيار محادثة أو قناة للتجربة أولاً");
      return;
    }
    const targetChat = dialogs.find((d) => d.id === selectedChatForTest);
    const chatTitle = targetChat?.title || selectedChatForTest;

    try {
      setStatusMessage("جاري فحص المجموعة ومعالجة وضع الحماية...");

      let finalMessage =
        config.replyMessage || "رسالة اختبارية من نظام الإرسال والمراقبة";
      if (
        config.protectedGroupAction === "always" ||
        config.protectedGroupAction === "smart"
      ) {
        finalMessage = sanitizeMessage(finalMessage, "clean");
      }

      if (config.protectedGroupAction === "salam") {
        await apiSendMessage({
          sessionString: getStoredSession() || "",
          chatId: selectedChatForTest,
          message: "السلام عليكم",
        });

        addSendMonitorLog({
          chatId: selectedChatForTest,
          chatTitle,
          triggerKeyword: config.keywords[0] || "تجربة ذكية",
          actionTaken: `بدء وضع السلام الذكي (Salam Mode) في ${chatTitle}`,
          status: "success",
          details: `تم إرسال رسالة "السلام عليكم" المبدئية. سيتولى النظام التعديل التلقائي للنص الأصلي بعد وصول ${
            config.smartSalamRequiredMessages || 3
          } رسائل أو ${config.smartSalamWaitMinutes || 5} دقائق.`,
        });

        setLogs(getSendMonitorLogs());
        setStatusMessage(
          `تم إرسال رسالة السلام بنجاح (وضع السلام الذكي Salam Mode)`
        );
        onShowToast({
          title: "وضع السلام الذكي نشط",
          body: `تم إرسال السلام المبدئي إلى ${chatTitle} لتجنب كشف البوتات`,
          type: "success",
        });
      } else {
        await apiSendMessage({
          sessionString: getStoredSession() || "",
          chatId: selectedChatForTest,
          message: finalMessage,
        });

        addSendMonitorLog({
          chatId: selectedChatForTest,
          chatTitle,
          triggerKeyword: config.keywords[0] || "تجربة يدوية",
          actionTaken: `إرسال رسالة رد ومراقبة إلى ${chatTitle} (${config.protectedGroupAction})`,
          status: "success",
          details: finalMessage,
        });

        setLogs(getSendMonitorLogs());
        setStatusMessage("تم الإرسال بنجاح وتسجيل العملية في السجلات!");
        onShowToast({
          title: "نجاح الإرسال الاختباري",
          body: `تم إرسال الرد إلى ${chatTitle}`,
          type: "success",
        });
      }
    } catch (err: any) {
      addSendMonitorLog({
        chatId: selectedChatForTest,
        chatTitle,
        actionTaken: "فشل الإرسال الاختباري",
        status: "failed",
        details: err.message,
      });
      setLogs(getSendMonitorLogs());
      setStatusMessage(`فشل الإرسال: ${err.message}`);
    }
  };

  const handleClearLogs = () => {
    saveSendMonitorLogs([]);
    setLogs([]);
  };

  const PROTECTED_MODES: {
    id: ProtectedGroupAction;
    icon: string;
    label: string;
    tag: string;
    desc: string;
    badgeColor: string;
    risk: string;
  }[] = [
    {
      id: "salam",
      icon: "🤖",
      label: "ذكي (Salam) – الوضع الافتراضي",
      tag: "الأكثر تقدماً وأماناً",
      desc: "يرسل 'السلام عليكم' أولاً، وينتظر تفاعل الأعضاء بعدد رسائل محدد (مثلاً 3 رسائل)، ثم يقوم بتعديل الرسالة إلى النص الأصلي. إذا لم يتفاعل أحد يحذفها لمنع الشك وتجنب الحظر.",
      badgeColor:
        "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20",
      risk: "أمان فائق",
    },
    {
      id: "skip",
      icon: "⏭️",
      label: "تخطي (Skip)",
      tag: "أمان تام",
      desc: "يتجاهل أي مجموعة تكتشف فيها بوتات حماية (@MissRose, @Shieldy, إلخ) ولا يرسل فيها نهائياً لتفادي أي بلاغ أو تقييد للحساب.",
      badgeColor:
        "bg-blue-500/10 text-blue-600 dark:text-blue-400 border-blue-500/20",
      risk: "صفر مخاطرة",
    },
    {
      id: "smart",
      icon: "🧠",
      label: "تنقية ذكية (Smart)",
      tag: "تجاوز ذكي",
      desc: "يفحص المجموعة؛ وإذا كانت محمية يقوم تلقائياً بإزالة روابط تيليجرام وواتساب وأرقام الهواتف والكلمات الإعلانية من الرسالة ثم يرسلها.",
      badgeColor:
        "bg-purple-500/10 text-purple-600 dark:text-purple-400 border-purple-500/20",
      risk: "أمان عالي",
    },
    {
      id: "always",
      icon: "🛡️",
      label: "تنقية دائمة (Always)",
      tag: "حماية شاملة",
      desc: "يقوم بتنقية وتجريد الرسالة من الروابط والأرقام دائماً قبل إرسالها لجميع القنوات والمجموعات بدون استثناء.",
      badgeColor:
        "bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20",
      risk: "أمان متوسط",
    },
    {
      id: "off",
      icon: "🚫",
      label: "معطّل (Off)",
      tag: "تجاوز كامل (خطر عالي)",
      desc: "يتجاوز فحص الحماية تماماً ويرسل النص الخام الأصلي كما هو دون أي تعديل أو فحص (قد يؤدي لحظر الحساب من قِبل بوتات المجموعات).",
      badgeColor:
        "bg-red-500/10 text-red-600 dark:text-red-400 border-red-500/20",
      risk: "خطر حظر الحساب",
    },
  ];

  return (
    <div
      id="send-monitor-modal-overlay"
      className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/60 backdrop-blur-xs animate-fade-in"
      dir="rtl"
    >
      <div
        id="send-monitor-modal-content"
        className="w-full max-w-3xl bg-white dark:bg-[#17212b] rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-800 flex flex-col max-h-[92vh] overflow-hidden animate-scale-up"
      >
        {/* Modal Header */}
        <div className="px-5 py-3.5 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between bg-slate-50/80 dark:bg-[#202b36]/60">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-sky-500 to-indigo-600 flex items-center justify-center text-white shadow-md shadow-sky-500/20">
              <Radio className="w-5 h-5 animate-pulse" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="font-bold text-sm text-slate-800 dark:text-white">
                  الإرسال والمراقبة الذكية (Send & Monitoring)
                </h2>
                <span
                  className={`text-[10px] font-bold px-2 py-0.5 rounded-full flex items-center gap-1 ${
                    isRunning
                      ? "bg-emerald-100 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 border border-emerald-300 dark:border-emerald-800"
                      : "bg-slate-100 dark:bg-slate-800 text-slate-500"
                  }`}
                >
                  <span
                    className={`w-1.5 h-1.5 rounded-full ${
                      isRunning ? "bg-emerald-500 animate-ping" : "bg-slate-400"
                    }`}
                  />
                  {isRunning ? "يعمل في الخلفية" : "متوقف"}
                </span>
              </div>
              <p className="text-xs text-slate-400">
                الرصد التلقائي للكلمات المفتاحية مع نظام حماية المجموعات والتخزين الدائم
              </p>
            </div>
          </div>
          <button
            id="close-send-monitor-modal-btn"
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-slate-600 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Modal Navigation Tabs */}
        <div className="flex items-center border-b border-slate-100 dark:border-slate-800 bg-slate-50 dark:bg-slate-900/50 px-3 overflow-x-auto no-scrollbar text-xs font-medium">
          <button
            onClick={() => setActiveTab("settings")}
            className={`py-3 px-3.5 flex items-center gap-1.5 border-b-2 transition-colors whitespace-nowrap ${
              activeTab === "settings"
                ? "border-sky-500 text-sky-600 dark:text-sky-400 font-bold"
                : "border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-200"
            }`}
          >
            <Sliders className="w-3.5 h-3.5" />
            <span>⚙️ الإعدادات والكلمات المفتاحية</span>
          </button>
          <button
            onClick={() => setActiveTab("protection")}
            className={`py-3 px-3.5 flex items-center gap-1.5 border-b-2 transition-colors whitespace-nowrap ${
              activeTab === "protection"
                ? "border-sky-500 text-sky-600 dark:text-sky-400 font-bold"
                : "border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-200"
            }`}
          >
            <Shield className="w-3.5 h-3.5" />
            <span>🛡️ أوضاع الحماية (5 أوضاع)</span>
          </button>
          <button
            onClick={() => setActiveTab("broadcast")}
            className={`py-3 px-3.5 flex items-center gap-1.5 border-b-2 transition-colors whitespace-nowrap ${
              activeTab === "broadcast"
                ? "border-sky-500 text-sky-600 dark:text-sky-400 font-bold"
                : "border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-200"
            }`}
          >
            <Zap className="w-3.5 h-3.5" />
            <span>🚀 الجدولة والقنوات</span>
          </button>
          <button
            onClick={() => setActiveTab("logs")}
            className={`py-3 px-3.5 flex items-center gap-1.5 border-b-2 transition-colors whitespace-nowrap ${
              activeTab === "logs"
                ? "border-sky-500 text-sky-600 dark:text-sky-400 font-bold"
                : "border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-200"
            }`}
          >
            <History className="w-3.5 h-3.5" />
            <span>📜 السجل ({logs.length})</span>
          </button>
          <button
            onClick={() => setActiveTab("test")}
            className={`py-3 px-3.5 flex items-center gap-1.5 border-b-2 transition-colors whitespace-nowrap ${
              activeTab === "test"
                ? "border-sky-500 text-sky-600 dark:text-sky-400 font-bold"
                : "border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-200"
            }`}
          >
            <Send className="w-3.5 h-3.5" />
            <span>🧪 اختبار وتجربة</span>
          </button>
        </div>

        {/* Modal Content Body */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {/* TAB 1: SETTINGS & TRIGGER KEYWORDS */}
          {activeTab === "settings" && (
            <div className="space-y-4 text-xs">
              {/* Quick Status and Run Toggle */}
              <div className="p-3 bg-sky-50/70 dark:bg-sky-950/30 border border-sky-100 dark:border-sky-900/50 rounded-xl flex items-center justify-between">
                <div>
                  <h3 className="font-bold text-slate-800 dark:text-slate-100 flex items-center gap-1.5">
                    <span>حالة رصد الكلمات المفتاحية في الخلفية</span>
                    <span className="text-[10px] bg-sky-500 text-white px-2 py-0.2 rounded-full font-bold">
                      {config.keywords.length} كلمة نشطة
                    </span>
                  </h3>
                  <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
                    الخادم والعامل في الخلفية يراقبان كافة الرسائل الواردة بشكل مستمر ويطبقان وضع الحماية:{" "}
                    <strong className="text-sky-600 dark:text-sky-400">
                      {config.protectedGroupAction.toUpperCase()}
                    </strong>
                  </p>
                </div>
                <button
                  id="toggle-send-monitor-btn"
                  onClick={handleToggleRunning}
                  className={`px-4 py-2 rounded-xl font-bold flex items-center gap-2 shadow-sm transition-transform active:scale-95 ${
                    isRunning
                      ? "bg-red-500 hover:bg-red-600 text-white"
                      : "bg-emerald-500 hover:bg-emerald-600 text-white"
                  }`}
                >
                  {isRunning ? (
                    <>
                      <Square className="w-4 h-4" />
                      <span>إيقاف المراقبة</span>
                    </>
                  ) : (
                    <>
                      <Play className="w-4 h-4" />
                      <span>بدء المراقبة الآن</span>
                    </>
                  )}
                </button>
              </div>

              {/* 1. Permanent Keywords Manager Section */}
              <div className="p-3.5 bg-slate-50 dark:bg-[#1e2936] rounded-xl border border-slate-200 dark:border-slate-700 space-y-3">
                <div className="flex items-center justify-between">
                  <div>
                    <label className="font-bold text-slate-800 dark:text-slate-100 flex items-center gap-1.5 text-xs">
                      <Database className="w-4 h-4 text-sky-500" />
                      <span>قائمة كلمات المراقبة الدائمة في النظام (Persistent Trigger Keywords)</span>
                    </label>
                    <p className="text-[11px] text-slate-400">
                      يتم حفظ هذه الكلمات في التخزين الدائم للنظام ورصدها فورياً في كافة المجموعات
                    </p>
                  </div>

                  <div className="flex items-center gap-2">
                    <button
                      onClick={handleExplicitSave}
                      className={`px-3 py-1.5 rounded-lg text-xs font-bold flex items-center gap-1 transition-all ${
                        isSavedSuccess
                          ? "bg-emerald-500 text-white shadow-sm"
                          : "bg-sky-500 hover:bg-sky-600 text-white"
                      }`}
                    >
                      {isSavedSuccess ? (
                        <>
                          <Check className="w-3.5 h-3.5" />
                          <span>تم الحفظ في النظام!</span>
                        </>
                      ) : (
                        <>
                          <Save className="w-3.5 h-3.5" />
                          <span>حفظ دائم للنظام</span>
                        </>
                      )}
                    </button>
                  </div>
                </div>

                {/* Add Keyword Input + Bulk Option */}
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={newKeyword}
                    onChange={(e) => setNewKeyword(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") {
                        e.preventDefault();
                        handleAddKeyword();
                      }
                    }}
                    placeholder="أدخل كلمة أو عبارة جديدة (مثال: اريد مساعدة، من يحل، عندي بحث، سكليف)..."
                    className="flex-1 px-3 py-2 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl text-xs focus:ring-2 focus:ring-sky-500 focus:outline-none"
                  />
                  <button
                    onClick={handleAddKeyword}
                    className="px-3.5 py-2 bg-[#2481cc] hover:bg-sky-600 text-white rounded-xl font-bold flex items-center gap-1 transition-colors"
                  >
                    <Plus className="w-4 h-4" />
                    <span>إضافة</span>
                  </button>
                  <button
                    onClick={() => setShowBulkInput(!showBulkInput)}
                    className="px-3 py-2 bg-slate-200 dark:bg-slate-800 text-slate-700 dark:text-slate-300 hover:bg-slate-300 rounded-xl font-medium flex items-center gap-1 transition-colors"
                    title="إضافة قائمة كلمات دفعة واحدة"
                  >
                    <Layers className="w-4 h-4" />
                    <span>إضافة جماعية</span>
                  </button>
                </div>

                {/* Bulk Keywords Collapsible Textarea */}
                {showBulkInput && (
                  <div className="p-3 bg-white dark:bg-slate-900 rounded-xl border border-sky-200 dark:border-sky-900 space-y-2 animate-slide-down">
                    <label className="font-bold text-slate-700 dark:text-slate-300 text-[11px]">
                      ألصق الكلمات المفتاحية (مفصولة بفواصل أو سطر جديد):
                    </label>
                    <textarea
                      value={bulkKeywordsText}
                      onChange={(e) => setBulkKeywordsText(e.target.value)}
                      rows={3}
                      placeholder="اريد مساعدة&#10;من يحل واجب&#10;عندي بحث&#10;ابي سكليف&#10;ابي شخص مضمون..."
                      className="w-full p-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-xs resize-none"
                    />
                    <div className="flex items-center justify-end gap-2">
                      <button
                        onClick={() => setShowBulkInput(false)}
                        className="px-2.5 py-1 text-slate-400 hover:text-slate-600 text-[11px]"
                      >
                        إلغاء
                      </button>
                      <button
                        onClick={handleAddBulkKeywords}
                        className="px-3 py-1 bg-sky-500 hover:bg-sky-600 text-white rounded-lg text-xs font-bold"
                      >
                        إضافة وحفظ الكلمات
                      </button>
                    </div>
                  </div>
                )}

                {/* Quick Presets Buttons */}
                <div className="space-y-1.5">
                  <span className="text-[11px] font-bold text-slate-500 dark:text-slate-400 block">
                    قوالب الكلمات الجاهزة السريعة:
                  </span>
                  <div className="flex flex-wrap gap-1.5">
                    <button
                      onClick={() =>
                        handleAddPresetCategory(
                          [
                            "اريد مساعدة",
                            "ابي مساعدة",
                            "احتاج مساعدة",
                            "من يساعدني",
                            "أحتاج مساعدتكم",
                            "هيليب",
                            "من يستطيع",
                          ],
                          "المساعدة العامة"
                        )
                      }
                      className="px-2.5 py-1 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-700 dark:text-slate-300 hover:bg-sky-50 dark:hover:bg-sky-950/40 text-[11px] font-medium transition-colors"
                    >
                      + طلبات المساعدة
                    </button>
                    <button
                      onClick={() =>
                        handleAddPresetCategory(
                          [
                            "من يحل",
                            "معي واجب",
                            "من يسوي تكليف",
                            "مين يعرف يحل واجب",
                            "من يحل واجبات الجامعه",
                          ],
                          "الواجبات والتكاليف"
                        )
                      }
                      className="px-2.5 py-1 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-700 dark:text-slate-300 hover:bg-sky-50 dark:hover:bg-sky-950/40 text-[11px] font-medium transition-colors"
                    >
                      + الواجبات والتكاليف
                    </button>
                    <button
                      onClick={() =>
                        handleAddPresetCategory(
                          [
                            "عندي بحث",
                            "عندي اسايمنت",
                            "من يسوي اسايمنت",
                            "ابي احد يسوي بحث",
                            "مشروع تخرج",
                          ],
                          "البحوث والاسايمنت"
                        )
                      }
                      className="px-2.5 py-1 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-700 dark:text-slate-300 hover:bg-sky-50 dark:hover:bg-sky-950/40 text-[11px] font-medium transition-colors"
                    >
                      + البحوث والاسايمنت
                    </button>
                    <button
                      onClick={() =>
                        handleAddPresetCategory(
                          ["ابي سكليف", "ابي عذر", "من يسوي سكليف", "عذر طبي"],
                          "الأعذار والسكليف"
                        )
                      }
                      className="px-2.5 py-1 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-700 dark:text-slate-300 hover:bg-sky-50 dark:hover:bg-sky-950/40 text-[11px] font-medium transition-colors"
                    >
                      + الأعذار والسكليف
                    </button>
                    <button
                      onClick={() =>
                        handleAddPresetCategory(
                          [
                            "ابي شخص مضمون",
                            "ابي مختص",
                            "من يعرف مختص",
                            "تعرفون احد",
                            "تعرفون شخص",
                          ],
                          "المختصين"
                        )
                      }
                      className="px-2.5 py-1 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-700 dark:text-slate-300 hover:bg-sky-50 dark:hover:bg-sky-950/40 text-[11px] font-medium transition-colors"
                    >
                      + مختصين ومضمونين
                    </button>
                    <button
                      onClick={handleRestoreDefaultKeywords}
                      className="px-2.5 py-1 bg-sky-50 dark:bg-sky-950/60 text-sky-600 dark:text-sky-400 border border-sky-200 dark:border-sky-800 rounded-lg text-[11px] font-bold flex items-center gap-1 hover:bg-sky-100 transition-colors"
                    >
                      <RotateCcw className="w-3 h-3" />
                      <span>استعادة القائمة القياسية الشاملة (28 كلمة)</span>
                    </button>
                  </div>
                </div>

                {/* Stored Keywords Badge Grid */}
                <div className="space-y-1">
                  <div className="flex items-center justify-between text-[11px] text-slate-400">
                    <span>الكلمات المخزنة حالياً في النظام:</span>
                    <span>{config.keywords.length} كلمة</span>
                  </div>
                  <div className="flex flex-wrap gap-1.5 max-h-48 overflow-y-auto p-2.5 bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-700">
                    {config.keywords.map((kw) => (
                      <span
                        key={kw}
                        className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-800 dark:text-slate-200 text-xs font-medium shadow-2xs group hover:border-sky-400 transition-colors"
                      >
                        <span>{kw}</span>
                        <button
                          onClick={() => handleRemoveKeyword(kw)}
                          className="text-slate-400 hover:text-red-500 transition-colors p-0.5 rounded"
                          title={`حذف "${kw}"`}
                        >
                          <X className="w-3 h-3" />
                        </button>
                      </span>
                    ))}
                  </div>
                </div>
              </div>

              {/* 2. Protected Group Action Selection in Settings */}
              <div className="p-3.5 bg-slate-50 dark:bg-[#1e2936] rounded-xl border border-slate-200 dark:border-slate-700 space-y-2.5">
                <label className="font-bold text-slate-800 dark:text-slate-100 flex items-center gap-1.5">
                  <Shield className="w-4 h-4 text-emerald-500" />
                  <span>الإجراء المطبق على المجموعات المحمية عند رصد الكلمات (Protected Group Action):</span>
                </label>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                  {[
                    {
                      id: "salam",
                      label: "🤖 ذكي (Salam Mode)",
                      desc: "إرسال 'السلام عليكم' ثم تعديل النص لاحقاً",
                    },
                    {
                      id: "skip",
                      label: "⏭️ تخطي (Skip)",
                      desc: "تخطي المجموعات المحمية تماماً لتفادي الحظر",
                    },
                    {
                      id: "smart",
                      label: "🧠 تنقية ذكية (Smart)",
                      desc: "حذف الروابط والأرقام تلقائياً قبل الإرسال",
                    },
                  ].map((mode) => (
                    <button
                      key={mode.id}
                      onClick={() =>
                        handleSaveConfig({
                          ...config,
                          protectedGroupAction: mode.id as ProtectedGroupAction,
                        })
                      }
                      className={`p-2.5 rounded-xl border text-right transition-all ${
                        config.protectedGroupAction === mode.id
                          ? "border-emerald-500 bg-emerald-50 dark:bg-emerald-950/40 text-emerald-900 dark:text-emerald-200 ring-1 ring-emerald-500/30 font-bold"
                          : "border-slate-200 dark:border-slate-700 hover:bg-white dark:hover:bg-slate-800 text-slate-600 dark:text-slate-400"
                      }`}
                    >
                      <div className="text-xs">{mode.label}</div>
                      <div className="text-[10px] text-slate-400 font-normal mt-0.5">
                        {mode.desc}
                      </div>
                    </button>
                  ))}
                </div>

                {/* Smart Salam Extra Parameters */}
                {config.protectedGroupAction === "salam" && (
                  <div className="pt-2 border-t border-slate-200/60 dark:border-slate-700/60 grid grid-cols-2 gap-3">
                    <div className="space-y-1">
                      <span className="text-[11px] text-slate-600 dark:text-slate-300 block font-medium">
                        رسائل التفاعل المطلوبة للتعديل:
                      </span>
                      <div className="flex items-center gap-2">
                        <input
                          type="number"
                          min={1}
                          max={20}
                          value={config.smartSalamRequiredMessages || 3}
                          onChange={(e) =>
                            handleSaveConfig({
                              ...config,
                              smartSalamRequiredMessages:
                                parseInt(e.target.value, 10) || 3,
                            })
                          }
                          className="w-20 px-2 py-1 bg-white dark:bg-slate-900 border border-slate-300 dark:border-slate-600 rounded-lg text-xs font-bold text-center"
                        />
                        <span className="text-[11px] text-slate-400">
                          رسائل من الأعضاء
                        </span>
                      </div>
                    </div>

                    <div className="space-y-1">
                      <span className="text-[11px] text-slate-600 dark:text-slate-300 block font-medium">
                        الحد الأقصى للانتظار قبل الحذف:
                      </span>
                      <div className="flex items-center gap-2">
                        <input
                          type="number"
                          min={1}
                          max={60}
                          value={config.smartSalamWaitMinutes || 5}
                          onChange={(e) =>
                            handleSaveConfig({
                              ...config,
                              smartSalamWaitMinutes:
                                parseInt(e.target.value, 10) || 5,
                            })
                          }
                          className="w-20 px-2 py-1 bg-white dark:bg-slate-900 border border-slate-300 dark:border-slate-600 rounded-lg text-xs font-bold text-center"
                        />
                        <span className="text-[11px] text-slate-400">
                          دقائق
                        </span>
                      </div>
                    </div>
                  </div>
                )}
              </div>

              {/* 3. Reply / Broadcast Message Content */}
              <div className="p-3.5 bg-slate-50 dark:bg-[#1e2936] rounded-xl border border-slate-200 dark:border-slate-700 space-y-1.5">
                <label className="font-bold text-slate-800 dark:text-slate-100 flex items-center justify-between">
                  <span>نص الرد التلقائي / المحتوى المرسل:</span>
                  <span className="text-[10px] text-slate-400 font-normal">
                    {config.replyMessage.length} حرف
                  </span>
                </label>
                <textarea
                  value={config.replyMessage}
                  onChange={(e) =>
                    handleSaveConfig({
                      ...config,
                      replyMessage: e.target.value,
                    })
                  }
                  rows={3}
                  placeholder="أدخل نص الرسالة التي سيتم إرسالها آلياً عند رصد الكلمات المفتاحية..."
                  className="w-full p-3 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl text-xs focus:ring-2 focus:ring-sky-500 focus:outline-none resize-none leading-relaxed"
                />
              </div>
            </div>
          )}

          {/* TAB 2: PROTECTION MODES DETAILS */}
          {activeTab === "protection" && (
            <div className="space-y-3 text-xs">
              <div className="p-3 bg-slate-50 dark:bg-slate-800/60 rounded-xl border border-slate-200 dark:border-slate-700">
                <h3 className="font-bold text-slate-800 dark:text-slate-100 mb-1">
                  نظام حماية الحسابات والمجموعات (Group Protection Shield)
                </h3>
                <p className="text-[11px] text-slate-500 dark:text-slate-400">
                  يتعرف النظام تلقائياً على أشهر بوتات الإشراف والحظر (@MissRose, @Shieldy, @GroupGuard, إلخ) لمنع تقييد الحساب
                </p>
              </div>

              <div className="space-y-2">
                {PROTECTED_MODES.map((mode) => {
                  const isSelected = config.protectedGroupAction === mode.id;
                  return (
                    <div
                      key={mode.id}
                      onClick={() =>
                        handleSaveConfig({
                          ...config,
                          protectedGroupAction: mode.id,
                        })
                      }
                      className={`p-3 rounded-xl border cursor-pointer transition-all ${
                        isSelected
                          ? "border-sky-500 bg-sky-50/60 dark:bg-sky-950/40 shadow-xs ring-1 ring-sky-500/30"
                          : "border-slate-200 dark:border-slate-700/80 hover:bg-slate-50 dark:hover:bg-slate-800/60"
                      }`}
                    >
                      <div className="flex items-center justify-between mb-1">
                        <div className="flex items-center gap-2">
                          <span className="text-lg">{mode.icon}</span>
                          <span className="font-bold text-slate-800 dark:text-slate-100">
                            {mode.label}
                          </span>
                          <span
                            className={`text-[10px] font-bold px-2 py-0.5 rounded-full border ${mode.badgeColor}`}
                          >
                            {mode.tag}
                          </span>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className="text-[10px] text-slate-400">
                            مستوى الأمان: {mode.risk}
                          </span>
                          <input
                            type="radio"
                            checked={isSelected}
                            onChange={() => {}}
                            className="text-sky-500"
                          />
                        </div>
                      </div>
                      <p className="text-[11px] text-slate-500 dark:text-slate-400 leading-relaxed pr-7">
                        {mode.desc}
                      </p>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* TAB 3: BROADCAST & TARGET CHANNELS */}
          {activeTab === "broadcast" && (
            <div className="space-y-4 text-xs">
              {/* Send Mode Selection */}
              <div className="p-3 bg-slate-50 dark:bg-slate-800/60 rounded-xl border border-slate-200 dark:border-slate-700 space-y-2.5">
                <label className="font-bold text-slate-800 dark:text-slate-200 flex items-center gap-1.5">
                  <Zap className="w-4 h-4 text-sky-500" />
                  <span>نمط تكرار الإرسال والجدولة:</span>
                </label>
                <div className="grid grid-cols-3 gap-2">
                  {[
                    {
                      id: "auto_interval",
                      label: "تكرار دوري آلي",
                      desc: "إرسال متكرر بفترة زمنية محددة",
                    },
                    {
                      id: "manual",
                      label: "رصد فوري فقط",
                      desc: "الإرسال فقط عند رصد الكلمات",
                    },
                    {
                      id: "scheduled",
                      label: "مجدول بتوقيت محدد",
                      desc: "تحديد تاريخ ووقت مستهدف للإرسال",
                    },
                  ].map((mode) => (
                    <button
                      key={mode.id}
                      onClick={() =>
                        handleSaveConfig({
                          ...config,
                          sendType: mode.id as SendScheduleType,
                        })
                      }
                      className={`p-2.5 rounded-xl border text-right transition-all ${
                        config.sendType === mode.id
                          ? "border-sky-500 bg-sky-50 dark:bg-sky-950/50 text-sky-800 dark:text-sky-200 shadow-xs ring-1 ring-sky-500/30 font-bold"
                          : "border-slate-200 dark:border-slate-700 hover:bg-white dark:hover:bg-slate-800 text-slate-600 dark:text-slate-400"
                      }`}
                    >
                      <div className="text-xs">{mode.label}</div>
                      <div className="text-[10px] text-slate-400 font-normal">
                        {mode.desc}
                      </div>
                    </button>
                  ))}
                </div>

                {config.sendType === "auto_interval" && (
                  <div className="pt-2 border-t border-slate-200/60 dark:border-slate-700/60 space-y-2">
                    <div className="flex items-center justify-between">
                      <span className="font-medium text-slate-700 dark:text-slate-300 flex items-center gap-1">
                        <Clock className="w-3.5 h-3.5 text-indigo-500" />
                        <span>الفترة الزمنية للتكرار التلقائي:</span>
                      </span>
                      <span className="text-sky-600 font-bold">
                        كل {config.repeatIntervalMinutes} دقيقة
                      </span>
                    </div>

                    <div className="flex flex-wrap gap-1.5">
                      {[15, 30, 60, 120, 240, 480, 1440].map((val) => (
                        <button
                          key={val}
                          onClick={() =>
                            handleSaveConfig({
                              ...config,
                              repeatIntervalMinutes: val,
                            })
                          }
                          className={`px-2.5 py-1 rounded-lg text-[11px] font-medium transition-colors ${
                            config.repeatIntervalMinutes === val
                              ? "bg-indigo-500 text-white font-bold"
                              : "bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 hover:bg-slate-100"
                          }`}
                        >
                          {val >= 60 ? `${val / 60} ساعة` : `${val} دقيقة`}
                        </button>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              {/* Target Chats Selection */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <label className="font-bold text-slate-700 dark:text-slate-300">
                    القنوات والمجموعات المستهدفة (
                    {config.targetChatIds.length > 0
                      ? `${config.targetChatIds.length} محددة`
                      : "جميع القنوات والمجموعات"}
                    )
                  </label>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={handleSelectAllChannels}
                      className="text-sky-600 hover:underline text-[11px] font-bold"
                    >
                      تحديد كل القنوات
                    </button>
                    <span className="text-slate-300">|</span>
                    <button
                      onClick={handleClearSelectedChats}
                      className="text-slate-400 hover:text-slate-600 text-[11px]"
                    >
                      الكل تلقائياً
                    </button>
                  </div>
                </div>

                <div className="max-h-48 overflow-y-auto space-y-1 p-2 bg-slate-50 dark:bg-slate-800/60 rounded-xl border border-slate-200 dark:border-slate-700">
                  {dialogs.length === 0 ? (
                    <p className="text-center py-4 text-slate-400 text-xs">
                      لا توجد محادثات محملة حالياً
                    </p>
                  ) : (
                    dialogs.map((dialog) => {
                      const isSelected = config.targetChatIds.includes(
                        String(dialog.id)
                      );
                      return (
                        <div
                          key={String(dialog.id)}
                          onClick={() => handleToggleTargetChat(String(dialog.id))}
                          className={`flex items-center justify-between p-2 rounded-lg cursor-pointer transition-colors ${
                            isSelected
                              ? "bg-sky-100 dark:bg-sky-950/60 text-sky-800 dark:text-sky-200"
                              : "hover:bg-slate-100 dark:hover:bg-slate-700/60 text-slate-700 dark:text-slate-300"
                          }`}
                        >
                          <div className="flex items-center gap-2 min-w-0">
                            <input
                              type="checkbox"
                              checked={isSelected}
                              onChange={() => {}}
                              className="rounded text-sky-500 focus:ring-0"
                            />
                            <span className="truncate font-medium text-xs">
                              {dialog.title}
                            </span>
                            <span className="text-[10px] text-slate-400 shrink-0">
                              (
                              {dialog.type === "channel"
                                ? "قناة"
                                : dialog.type === "group"
                                ? "مجموعة"
                                : "خاص"}
                              )
                            </span>
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>
              </div>
            </div>
          )}

          {/* TAB 4: LOGS */}
          {activeTab === "logs" && (
            <div className="space-y-3 text-xs">
              <div className="flex items-center justify-between">
                <span className="font-bold text-slate-700 dark:text-slate-300">
                  سجل عمليات الرصد والإرسال الفورية ({logs.length})
                </span>
                {logs.length > 0 && (
                  <button
                    onClick={handleClearLogs}
                    className="text-[11px] text-red-500 hover:text-red-700 flex items-center gap-1 font-bold"
                  >
                    <Trash2 className="w-3 h-3" />
                    <span>مسح السجل</span>
                  </button>
                )}
              </div>

              <div className="space-y-1.5 max-h-96 overflow-y-auto">
                {logs.length === 0 ? (
                  <div className="p-8 text-center text-slate-400 bg-slate-50 dark:bg-slate-800/40 rounded-xl border border-dashed border-slate-200 dark:border-slate-700">
                    <Activity className="w-8 h-8 mx-auto mb-2 opacity-40 text-slate-400" />
                    <p className="font-semibold">لا توجد عمليات مسجلة بعد</p>
                    <p className="text-[11px] mt-0.5">
                      ستظهر هنا عمليات رصد الكلمات والإرسال التلقائي مع تفاصيل الحماية
                    </p>
                  </div>
                ) : (
                  logs.map((log) => (
                    <div
                      key={log.id}
                      className={`p-2.5 rounded-xl border text-xs flex items-start justify-between gap-3 ${
                        log.status === "success"
                          ? "bg-emerald-50/50 dark:bg-emerald-950/20 border-emerald-200 dark:border-emerald-900/50"
                          : log.status === "skipped"
                          ? "bg-blue-50/50 dark:bg-blue-950/20 border-blue-200 dark:border-blue-900/50"
                          : "bg-red-50/50 dark:bg-red-950/20 border-red-200 dark:border-red-900/50"
                      }`}
                    >
                      <div className="space-y-0.5 min-w-0 flex-1">
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-slate-800 dark:text-slate-100 truncate">
                            {log.chatTitle}
                          </span>
                          {log.triggerKeyword && (
                            <span className="text-[10px] bg-sky-100 dark:bg-sky-900/60 text-sky-700 dark:text-sky-300 px-1.5 py-0.2 rounded-md font-bold">
                              كلمة: {log.triggerKeyword}
                            </span>
                          )}
                        </div>
                        <p className="text-[11px] text-slate-600 dark:text-slate-300">
                          {log.actionTaken}
                        </p>
                        {log.details && (
                          <p className="text-[10px] text-slate-400 truncate dir-ltr text-right">
                            {log.details}
                          </p>
                        )}
                      </div>
                      <span className="text-[10px] text-slate-400 shrink-0">
                        {new Date(log.timestamp).toLocaleTimeString([], {
                          hour: "2-digit",
                          minute: "2-digit",
                          second: "2-digit",
                        })}
                      </span>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

          {/* TAB 5: TEST LAB */}
          {activeTab === "test" && (
            <div className="space-y-3 text-xs">
              <div className="p-3 bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-900 rounded-xl space-y-1">
                <h4 className="font-bold text-amber-800 dark:text-amber-200 flex items-center gap-1.5">
                  <Sparkles className="w-4 h-4 text-amber-600" />
                  <span>مختبر فحص واختبار الحماية الفوري</span>
                </h4>
                <p className="text-[11px] text-amber-700 dark:text-amber-300">
                  قم باختيار مجموعة لاختبار كيفية معالجة نظام الرصد وإجراء الحماية (Salam, Smart, Skip, Always)
                </p>
              </div>

              <div className="space-y-2">
                <label className="font-bold text-slate-700 dark:text-slate-300">
                  اختر المجموعة أو القناة للاختبار:
                </label>
                <select
                  value={selectedChatForTest}
                  onChange={(e) => setSelectedChatForTest(e.target.value)}
                  className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-xs"
                >
                  <option value="">-- اختر محادثة أو قناة للتجربة --</option>
                  {dialogs.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.title} ({d.type})
                    </option>
                  ))}
                </select>
              </div>

              {statusMessage && (
                <div className="p-3 bg-slate-100 dark:bg-slate-800 rounded-xl font-medium text-slate-800 dark:text-slate-200">
                  {statusMessage}
                </div>
              )}

              <button
                onClick={handleTestTrigger}
                className="w-full py-2.5 bg-[#2481cc] hover:bg-sky-600 text-white rounded-xl font-bold flex items-center justify-center gap-2 shadow-sm transition-all"
              >
                <Send className="w-4 h-4" />
                <span>إرسال وتطبيق وضع الحماية الحالي ({config.protectedGroupAction.toUpperCase()})</span>
              </button>
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="px-5 py-3 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between bg-slate-50 dark:bg-[#1c2732] text-xs">
          <div className="flex items-center gap-2 text-slate-500">
            <span className="w-2 h-2 rounded-full bg-emerald-500 inline-block" />
            <span>التخزين الدائم: محفوظ في النظام</span>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handleExplicitSave}
              className="px-4 py-1.5 bg-sky-500 hover:bg-sky-600 text-white font-bold rounded-xl flex items-center gap-1.5 shadow-xs transition-colors"
            >
              <Save className="w-3.5 h-3.5" />
              <span>حفظ التعديلات</span>
            </button>
            <button
              onClick={onClose}
              className="px-4 py-1.5 bg-slate-200 dark:bg-slate-800 hover:bg-slate-300 text-slate-700 dark:text-slate-300 font-medium rounded-xl transition-colors"
            >
              إغلاق
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
