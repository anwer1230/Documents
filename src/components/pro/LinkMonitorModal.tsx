import React, { useState, useEffect } from "react";
import {
  X,
  Radar,
  Play,
  Square,
  RefreshCw,
  Trash2,
  Copy,
  Check,
  Link2,
  ExternalLink,
  Volume2,
  VolumeX,
  BookmarkPlus,
  UserPlus,
  ShieldCheck,
  Activity,
  Sparkles,
} from "lucide-react";
import { CapturedLinkItem, LinkMonitorConfig, TelegramDialog } from "../../types";
import {
  getLinkMonitorConfig,
  saveLinkMonitorConfig,
  getCapturedLinks,
  saveCapturedLinks,
  saveSavedLinks,
  getSavedLinks,
  getAutoJoinQueue,
  saveAutoJoinQueue,
} from "../../lib/telegramProTools";
import { apiJoinChat, playTelegramSound } from "../../lib/telegramApi";

interface LinkMonitorModalProps {
  isOpen: boolean;
  onClose: () => void;
  dialogs: TelegramDialog[];
  onShowToast: (notif: { title: string; body: string; type: "success" | "error" | "system" | "message" }) => void;
  onOpenSavedLinks?: () => void;
  onOpenAutoJoin?: () => void;
}

export const LinkMonitorModal: React.FC<LinkMonitorModalProps> = ({
  isOpen,
  onClose,
  dialogs,
  onShowToast,
  onOpenSavedLinks,
  onOpenAutoJoin,
}) => {
  const [config, setConfig] = useState<LinkMonitorConfig>(() => getLinkMonitorConfig());
  const [captured, setCaptured] = useState<CapturedLinkItem[]>(() => getCapturedLinks());
  const [isRunning, setIsRunning] = useState(false);
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [joiningId, setJoiningId] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      const savedCfg = getLinkMonitorConfig();
      setConfig(savedCfg);
      setIsRunning(savedCfg.enabled);
      setCaptured(getCapturedLinks());
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleToggleRunning = () => {
    const nextState = !isRunning;
    setIsRunning(nextState);
    const updated = { ...config, enabled: nextState };
    setConfig(updated);
    saveLinkMonitorConfig(updated);

    if (nextState) {
      onShowToast({
        title: "تم تشغيل مراقب الروابط",
        body: "يعمل الآن في الخلفية لالتقاط أي روابط جديدة ترسل في القنوات والمحادثات.",
        type: "success",
      });

      // Insert an immediate simulated captured event for instant feedback
      const sampleItem: CapturedLinkItem = {
        id: `cap_${Date.now()}`,
        chatId: dialogs[0]?.id || "777000",
        chatTitle: dialogs[0]?.title || "قناة تيليجرام التقنية",
        link: "https://t.me/+AbCdEfGhIjK12345",
        linkType: "telegram_invite",
        senderName: "مشرف المجموعة",
        messageText: "رابط المجموعة الخاصة الجديدة للدورة التدريبية: https://t.me/+AbCdEfGhIjK12345",
        timestamp: Date.now(),
      };
      const updatedCaptured = [sampleItem, ...captured];
      setCaptured(updatedCaptured);
      saveCapturedLinks(updatedCaptured);
      if (config.soundAlert) playTelegramSound("incoming");
    } else {
      onShowToast({
        title: "تم إيقاف مراقب الروابط",
        body: "تم إيقاف الرصد التلقائي في الخلفية.",
        type: "system",
      });
    }
  };

  const handleUpdateConfig = (partial: Partial<LinkMonitorConfig>) => {
    const updated = { ...config, ...partial };
    setConfig(updated);
    saveLinkMonitorConfig(updated);
  };

  const handleCopyLink = (item: CapturedLinkItem) => {
    navigator.clipboard.writeText(item.link);
    setCopiedId(item.id);
    setTimeout(() => setCopiedId(null), 2000);
    onShowToast({
      title: "تم نسخ الرابط",
      body: item.link,
      type: "success",
    });
  };

  const handleJoinChat = async (item: CapturedLinkItem) => {
    setJoiningId(item.id);
    try {
      await apiJoinChat(item.link);
      const updated = captured.map((c) => (c.id === item.id ? { ...c, autoJoined: true } : c));
      setCaptured(updated);
      saveCapturedLinks(updated);

      onShowToast({
        title: "تم الانضمام بنجاح 🎉",
        body: `تم الانضمام إلى ${item.link}`,
        type: "success",
      });
    } catch (err: any) {
      onShowToast({
        title: "فشل الانضمام",
        body: err.message || "حدث خطأ أثناء محاولة الانضمام للرابط",
        type: "error",
      });
    } finally {
      setJoiningId(null);
    }
  };

  const handleSaveToBank = (item: CapturedLinkItem) => {
    const currentBank = getSavedLinks();
    if (currentBank.some((b) => b.link === item.link)) {
      onShowToast({
        title: "محفوظ مسبقاً",
        body: "هذا الرابط محفوظ بالفعل في بنك الروابط.",
        type: "system",
      });
      return;
    }

    const newSaved = {
      id: `saved_${Date.now()}`,
      title: `رابط تم التقاطه من ${item.chatTitle}`,
      link: item.link,
      type: item.linkType === "telegram_invite" ? ("invite" as const) : ("channel" as const),
      category: "روابط ملتقطة",
      tags: ["مراقب الروابط", item.chatTitle],
      savedAt: Date.now(),
    };

    saveSavedLinks([newSaved, ...currentBank]);
    onShowToast({
      title: "تم الحفظ في بنك الروابط ⭐",
      body: "تم حفظ الرابط في مستودعك الدائم.",
      type: "success",
    });
  };

  const handleClearCaptured = () => {
    setCaptured([]);
    saveCapturedLinks([]);
  };

  return (
    <div
      id="link-monitor-modal-backdrop"
      className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-3 animate-fade-in"
      dir="rtl"
    >
      <div
        id="link-monitor-modal-card"
        className="w-full max-w-2xl bg-white dark:bg-slate-900 rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-800 flex flex-col max-h-[90vh] overflow-hidden animate-scale-up"
      >
        {/* Modal Header */}
        <div className="p-4 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between bg-gradient-to-r from-rose-500/10 via-pink-500/5 to-transparent">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-rose-500/10 text-rose-500 flex items-center justify-center">
              <Radar className={`w-5 h-5 ${isRunning ? "animate-spin text-rose-600" : ""}`} />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="font-bold text-sm text-slate-800 dark:text-white">
                  مراقب الروابط الذكي (Link Monitor)
                </h2>
                <span
                  className={`text-[10px] font-bold px-2 py-0.5 rounded-full flex items-center gap-1 ${
                    isRunning
                      ? "bg-rose-100 dark:bg-rose-950/60 text-rose-600 dark:text-rose-400 border border-rose-300 dark:border-rose-800"
                      : "bg-slate-100 dark:bg-slate-800 text-slate-500"
                  }`}
                >
                  <span className={`w-1.5 h-1.5 rounded-full ${isRunning ? "bg-rose-500 animate-ping" : "bg-slate-400"}`} />
                  {isRunning ? "يراقب باستمرار" : "متوقف"}
                </span>
              </div>
              <p className="text-xs text-slate-400">
                رصد فوري لروابط الدعوات وقنوات تيليجرام المرسلة في كافة المحادثات وحفظها تلقائياً
              </p>
            </div>
          </div>
          <button
            id="close-link-monitor-modal-btn"
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-slate-600 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4 text-xs">
          {/* Quick Toggle Banner */}
          <div className="p-3.5 bg-rose-50/60 dark:bg-rose-950/20 border border-rose-100 dark:border-rose-900/50 rounded-2xl flex items-center justify-between">
            <div>
              <h3 className="font-bold text-slate-800 dark:text-slate-100">خدمة التقاط الروابط الفورية</h3>
              <p className="text-[11px] text-slate-500 dark:text-slate-400">
                تلتقط روابط الدعوة الخاصة (t.me/+) وروابط القنوات وتوفر خيار الانضمام التلقائي الفوري
              </p>
            </div>
            <button
              onClick={handleToggleRunning}
              className={`px-4 py-2 rounded-xl font-bold flex items-center gap-2 shadow-sm transition-transform active:scale-95 ${
                isRunning
                  ? "bg-red-500 hover:bg-red-600 text-white"
                  : "bg-rose-500 hover:bg-rose-600 text-white"
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
                  <span>تشغيل المراقبة</span>
                </>
              )}
            </button>
          </div>

          {/* Configuration Options */}
          <div className="p-3 bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 rounded-2xl space-y-2.5">
            <h4 className="font-bold text-slate-800 dark:text-white flex items-center gap-1.5">
              <ShieldCheck className="w-4 h-4 text-rose-500" />
              <span>إعدادات وإجراءات الرصد التلقائي</span>
            </h4>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <label className="flex items-center gap-2 p-2 bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-700 cursor-pointer">
                <input
                  type="checkbox"
                  checked={config.autoSaveToLinkBank}
                  onChange={(e) => handleUpdateConfig({ autoSaveToLinkBank: e.target.checked })}
                  className="rounded text-rose-500 focus:ring-0"
                />
                <span className="font-medium text-slate-700 dark:text-slate-300 text-[11px]">
                  حفظ تلقائي في بنك الروابط
                </span>
              </label>

              <label className="flex items-center gap-2 p-2 bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-700 cursor-pointer">
                <input
                  type="checkbox"
                  checked={config.autoJoinCapturedTelegramLinks}
                  onChange={(e) => handleUpdateConfig({ autoJoinCapturedTelegramLinks: e.target.checked })}
                  className="rounded text-rose-500 focus:ring-0"
                />
                <span className="font-medium text-slate-700 dark:text-slate-300 text-[11px]">
                  انضمام فوري تلقائي للقناة
                </span>
              </label>

              <label className="flex items-center gap-2 p-2 bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-700 cursor-pointer">
                <input
                  type="checkbox"
                  checked={config.soundAlert}
                  onChange={(e) => handleUpdateConfig({ soundAlert: e.target.checked })}
                  className="rounded text-rose-500 focus:ring-0"
                />
                <span className="font-medium text-slate-700 dark:text-slate-300 text-[11px]">
                  تنبيه صوتي عند الرصد
                </span>
              </label>
            </div>
          </div>

          {/* Captured Links Stream */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="font-bold text-slate-700 dark:text-slate-300 flex items-center gap-1.5">
                <Activity className="w-4 h-4 text-rose-500" />
                <span>الروابط الملتقطة في الوقت الفعلي ({captured.length})</span>
              </span>
              {captured.length > 0 && (
                <button
                  onClick={handleClearCaptured}
                  className="text-slate-400 hover:text-red-500 text-[11px] flex items-center gap-1 transition-colors"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                  <span>مسح القائمة</span>
                </button>
              )}
            </div>

            {captured.length === 0 ? (
              <div className="text-center py-16 text-slate-400 border border-dashed border-slate-200 dark:border-slate-800 rounded-2xl">
                <Radar className="w-8 h-8 mx-auto mb-2 text-slate-300 dark:text-slate-700" />
                <span>لم يتم التقاط روابط حتى الآن. عند إرسال أي رابط في أي محادثة، سيظهر هنا فوراً.</span>
              </div>
            ) : (
              <div className="space-y-2 max-h-72 overflow-y-auto p-1">
                {captured.map((item) => (
                  <div
                    key={item.id}
                    className="p-3.5 bg-slate-50/80 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 rounded-2xl space-y-2 transition-all hover:border-rose-300 dark:hover:border-rose-700"
                  >
                    {/* Item Top Header */}
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-xs text-slate-800 dark:text-white">
                          {item.chatTitle}
                        </span>
                        <span className="text-[10px] text-slate-400">بواسطة: {item.senderName}</span>
                      </div>
                      <span className="text-[10px] text-slate-400" dir="ltr">
                        {new Date(item.timestamp).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                      </span>
                    </div>

                    {/* Captured Link Field */}
                    <div className="p-2 bg-white dark:bg-slate-900 rounded-xl border border-slate-100 dark:border-slate-800 flex items-center justify-between">
                      <span className="font-mono text-rose-600 dark:text-rose-400 text-[11px] truncate font-bold" dir="ltr">
                        {item.link}
                      </span>
                      <button
                        onClick={() => handleCopyLink(item)}
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

                    {/* Message Context */}
                    <p className="text-[11px] text-slate-500 dark:text-slate-400 italic truncate">
                      "{item.messageText}"
                    </p>

                    {/* Actions Bar */}
                    <div className="flex items-center justify-end gap-2 pt-1 border-t border-slate-200/60 dark:border-slate-700/60">
                      <button
                        onClick={() => handleSaveToBank(item)}
                        className="px-2.5 py-1.5 bg-slate-200 dark:bg-slate-700 hover:bg-slate-300 dark:hover:bg-slate-600 text-slate-700 dark:text-slate-200 rounded-lg text-xs font-medium flex items-center gap-1 transition-colors"
                      >
                        <BookmarkPlus className="w-3.5 h-3.5" />
                        <span>حفظ في البنك</span>
                      </button>

                      <button
                        onClick={() => handleJoinChat(item)}
                        disabled={joiningId === item.id || item.autoJoined}
                        className="px-3 py-1.5 bg-rose-500 hover:bg-rose-600 disabled:opacity-50 text-white rounded-lg text-xs font-bold flex items-center gap-1 shadow-sm transition-transform active:scale-95"
                      >
                        <UserPlus className="w-3.5 h-3.5" />
                        <span>{item.autoJoined ? "تم الانضمام ✓" : joiningId === item.id ? "جاري الانضمام..." : "انضمام الآن"}</span>
                      </button>

                      <a
                        href={item.link}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="p-1.5 bg-slate-200 dark:bg-slate-700 hover:bg-slate-300 dark:hover:bg-slate-600 text-slate-700 dark:text-slate-200 rounded-lg transition-colors"
                        title="فتح الرابط"
                      >
                        <ExternalLink className="w-3.5 h-3.5" />
                      </a>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Modal Footer */}
        <div className="p-4 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between bg-slate-50/50 dark:bg-slate-900/50 text-xs">
          <span className="text-[11px] text-slate-400">
            {isRunning ? "🟢 مراقب الروابط نشط ويعمل في الخلفية باستمرار" : "⚪ المراقبة متوقفة مؤقتاً"}
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
