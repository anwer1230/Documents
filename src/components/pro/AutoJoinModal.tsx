import React, { useState, useEffect, useRef } from "react";
import {
  X,
  UserPlus,
  Play,
  Square,
  RefreshCw,
  Trash2,
  Layers,
  CheckCircle2,
  AlertCircle,
  Clock,
  Sliders,
  Sparkles,
  Link2,
  ExternalLink,
  ShieldCheck,
} from "lucide-react";
import { AutoJoinQueueItem, AutoJoinConfig } from "../../types";
import {
  getAutoJoinConfig,
  saveAutoJoinConfig,
  getAutoJoinQueue,
  saveAutoJoinQueue,
} from "../../lib/telegramProTools";
import { apiJoinChat } from "../../lib/telegramApi";

interface AutoJoinModalProps {
  isOpen: boolean;
  onClose: () => void;
  onShowToast: (notif: { title: string; body: string; type: "success" | "error" | "system" | "message" }) => void;
  onRefreshDialogs?: () => void;
}

export const AutoJoinModal: React.FC<AutoJoinModalProps> = ({
  isOpen,
  onClose,
  onShowToast,
  onRefreshDialogs,
}) => {
  const [config, setConfig] = useState<AutoJoinConfig>(() => getAutoJoinConfig());
  const [queue, setQueue] = useState<AutoJoinQueueItem[]>(() => getAutoJoinQueue());
  const [inputText, setInputText] = useState("");
  const [isRunning, setIsRunning] = useState(false);
  const [currentIndex, setCurrentIndex] = useState<number>(-1);
  const [countdown, setCountdown] = useState<number>(0);
  const timerRef = useRef<any>(null);
  const countdownTimerRef = useRef<any>(null);

  useEffect(() => {
    if (isOpen) {
      setConfig(getAutoJoinConfig());
      setQueue(getAutoJoinQueue());
    }
  }, [isOpen]);

  useEffect(() => {
    saveAutoJoinQueue(queue);
  }, [queue]);

  useEffect(() => {
    saveAutoJoinConfig(config);
  }, [config]);

  // Background join engine
  useEffect(() => {
    if (!isRunning) {
      if (timerRef.current) clearTimeout(timerRef.current);
      if (countdownTimerRef.current) clearInterval(countdownTimerRef.current);
      return;
    }

    const nextPendingIdx = queue.findIndex((item) => item.status === "pending");
    if (nextPendingIdx === -1) {
      setIsRunning(false);
      onShowToast({
        title: "اكتمل الانضمام التلقائي 🎉",
        body: "تم الانتهاء من معالجة جميع الروابط في القائمة بنجاح.",
        type: "success",
      });
      if (onRefreshDialogs) onRefreshDialogs();
      return;
    }

    setCurrentIndex(nextPendingIdx);
    const targetItem = queue[nextPendingIdx];

    // Compute random jitter delay
    const baseDelay = config.delaySeconds || 12;
    const range = typeof config.randomDelayRange === 'number' ? config.randomDelayRange : (Array.isArray(config.randomDelayRange) ? config.randomDelayRange[0] : 4);
    const randomOffset = Math.floor(Math.random() * (range * 2 + 1)) - range;
    const actualDelay = Math.max(5, baseDelay + randomOffset);

    setCountdown(actualDelay);
    if (countdownTimerRef.current) clearInterval(countdownTimerRef.current);
    countdownTimerRef.current = setInterval(() => {
      setCountdown((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);

    // Update item status to joining
    setQueue((prev) =>
      prev.map((it, idx) => (idx === nextPendingIdx ? { ...it, status: "joining" } : it))
    );

    timerRef.current = setTimeout(async () => {
      try {
        const result = await apiJoinChat(targetItem.linkOrUsername);
        setQueue((prev) =>
          prev.map((it, idx) =>
            idx === nextPendingIdx
              ? {
                  ...it,
                  status: "joined",
                  title: result.title || targetItem.linkOrUsername,
                  joinedAt: Date.now(),
                }
              : it
          )
        );
        onShowToast({
          title: "تم الانضمام بنجاح",
          body: `تم الانضمام إلى: ${result.title || targetItem.linkOrUsername}`,
          type: "success",
        });
      } catch (err: any) {
        const errMsg = err.message || "";
        const isAlready = errMsg.includes("ALREADY_PARTICIPANT") || errMsg.includes("already");
        setQueue((prev) =>
          prev.map((it, idx) =>
            idx === nextPendingIdx
              ? {
                  ...it,
                  status: isAlready ? "already_member" : "failed",
                  error: isAlready ? "عضو بالفعل في القناة" : errMsg || "فشل الانضمام",
                }
              : it
          )
        );
      }
    }, actualDelay * 1000);

    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
      if (countdownTimerRef.current) clearInterval(countdownTimerRef.current);
    };
  }, [isRunning, queue, config]);

  if (!isOpen) return null;

  const handleParseAndAddLinks = () => {
    if (!inputText.trim()) return;

    // Split by newlines, spaces, or commas
    const lines = inputText
      .split(/[\n, ]+/)
      .map((l) => l.trim())
      .filter((l) => l.length > 2);

    const newItems: AutoJoinQueueItem[] = lines.map((link) => ({
      id: `join_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`,
      linkOrUsername: link,
      status: "pending",
    }));

    const updated = [...queue, ...newItems];
    setQueue(updated);
    setInputText("");

    onShowToast({
      title: "تمت إضافة الروابط",
      body: `تمت إضافة ${newItems.length} رابط إلى قائمة الانتظار.`,
      type: "success",
    });

    if (config.autoStartOnPaste && !isRunning) {
      setIsRunning(true);
    }
  };

  const handleClearAll = () => {
    setIsRunning(false);
    setQueue([]);
  };

  const handleClearCompleted = () => {
    setQueue((prev) => prev.filter((it) => it.status === "pending" || it.status === "joining"));
  };

  const handleRetryFailed = () => {
    setQueue((prev) =>
      prev.map((it) => (it.status === "failed" ? { ...it, status: "pending", error: undefined } : it))
    );
  };

  const totalItems = queue.length;
  const joinedCount = queue.filter((i) => i.status === "joined" || i.status === "already_member").length;
  const failedCount = queue.filter((i) => i.status === "failed").length;
  const pendingCount = queue.filter((i) => i.status === "pending" || i.status === "joining").length;
  const progressPercent = totalItems > 0 ? Math.round(((joinedCount + failedCount) / totalItems) * 100) : 0;

  return (
    <div
      id="auto-join-modal-backdrop"
      className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-3 animate-fade-in"
      dir="rtl"
    >
      <div
        id="auto-join-modal-card"
        className="w-full max-w-2xl bg-white dark:bg-slate-900 rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-800 flex flex-col max-h-[90vh] overflow-hidden animate-scale-up"
      >
        {/* Modal Header */}
        <div className="p-4 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between bg-gradient-to-r from-sky-500/10 via-blue-500/5 to-transparent">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-sky-500/10 text-sky-500 flex items-center justify-center">
              <UserPlus className={`w-5 h-5 ${isRunning ? "animate-bounce text-sky-600" : ""}`} />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="font-bold text-sm text-slate-800 dark:text-white">
                  الانضمام التلقائي المتقدم للقنوات (Auto Join Advanced)
                </h2>
                <span
                  className={`text-[10px] font-bold px-2 py-0.5 rounded-full flex items-center gap-1 ${
                    isRunning
                      ? "bg-emerald-100 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 border border-emerald-300"
                      : "bg-slate-100 dark:bg-slate-800 text-slate-500"
                  }`}
                >
                  <span className={`w-1.5 h-1.5 rounded-full ${isRunning ? "bg-emerald-500 animate-ping" : "bg-slate-400"}`} />
                  {isRunning ? `جاري المعالجة (متبقي ${countdown}ث)` : "متوقف"}
                </span>
              </div>
              <p className="text-xs text-slate-400">
                انضمام جماعي لروابط القنوات والمجموعات مع نظام حماية ذكي ضد الحظر (Anti-Flood)
              </p>
            </div>
          </div>
          <button
            id="close-auto-join-modal-btn"
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-slate-600 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4 text-xs">
          {/* Paste Links Input Box */}
          <div className="space-y-1.5">
            <label className="font-bold text-slate-700 dark:text-slate-300 flex items-center justify-between">
              <span>أدخل أو الصق روابط القنوات والمجموعات (رابط لكل سطر):</span>
              <span className="text-[11px] text-slate-400 font-normal">يدعم t.me/+ و t.me/joinchat و @username</span>
            </label>
            <textarea
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
              rows={3}
              placeholder="https://t.me/channel1&#10;https://t.me/+inviteCode&#10;@mygroup"
              className="w-full p-3 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl font-mono text-xs focus:ring-2 focus:ring-sky-500 focus:outline-none resize-none leading-relaxed"
            />
            <div className="flex items-center justify-between pt-1">
              <button
                onClick={handleParseAndAddLinks}
                disabled={!inputText.trim()}
                className="px-4 py-2 bg-sky-500 hover:bg-sky-600 disabled:opacity-50 text-white rounded-xl font-bold flex items-center gap-1.5 shadow-sm transition-transform active:scale-95"
              >
                <Link2 className="w-4 h-4" />
                <span>إضافة إلى قائمة الانضمام</span>
              </button>

              {queue.length > 0 && (
                <button
                  onClick={() => setIsRunning(!isRunning)}
                  className={`px-4 py-2 rounded-xl font-bold flex items-center gap-1.5 shadow-sm transition-transform active:scale-95 ${
                    isRunning
                      ? "bg-red-500 hover:bg-red-600 text-white"
                      : "bg-emerald-500 hover:bg-emerald-600 text-white"
                  }`}
                >
                  {isRunning ? (
                    <>
                      <Square className="w-4 h-4" />
                      <span>إيقاف مؤقت</span>
                    </>
                  ) : (
                    <>
                      <Play className="w-4 h-4" />
                      <span>بدء الانضمام المتسلسل</span>
                    </>
                  )}
                </button>
              )}
            </div>
          </div>

          {/* Settings & Anti-Flood configuration */}
          <div className="p-3 bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 rounded-2xl space-y-2">
            <div className="flex items-center justify-between">
              <h4 className="font-bold text-slate-800 dark:text-white flex items-center gap-1.5">
                <ShieldCheck className="w-4 h-4 text-sky-500" />
                <span>إعدادات الحماية والفاصل الزمني (Anti-Flood Guard)</span>
              </h4>
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              <div className="space-y-1">
                <label className="text-[11px] text-slate-500 dark:text-slate-400">الفاصل الزمني الأساسي (ثوانٍ):</label>
                <input
                  type="number"
                  min={5}
                  max={120}
                  value={config.delaySeconds}
                  onChange={(e) => setConfig({ ...config, delaySeconds: parseInt(e.target.value, 10) || 10 })}
                  className="w-full px-2.5 py-1.5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-xs"
                />
              </div>

              <div className="space-y-1">
                <label className="text-[11px] text-slate-500 dark:text-slate-400">تغير عشوائي (+/- ثانية):</label>
                <input
                  type="number"
                  min={0}
                  max={20}
                  value={typeof config.randomDelayRange === 'number' ? config.randomDelayRange : 0}
                  onChange={(e) => setConfig({ ...config, randomDelayRange: parseInt(e.target.value, 10) || 0 })}
                  className="w-full px-2.5 py-1.5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-xs"
                />
              </div>

              <div className="space-y-1 col-span-2 sm:col-span-1 flex flex-col justify-center">
                <label className="flex items-center gap-2 cursor-pointer pt-2">
                  <input
                    type="checkbox"
                    checked={config.autoStartOnPaste}
                    onChange={(e) => setConfig({ ...config, autoStartOnPaste: e.target.checked })}
                    className="rounded text-sky-500 focus:ring-0"
                  />
                  <span className="text-[11px] text-slate-700 dark:text-slate-300 font-medium">بدء تلقائي عند اللصق</span>
                </label>
              </div>
            </div>
          </div>

          {/* Progress Overview */}
          {totalItems > 0 && (
            <div className="space-y-2">
              <div className="flex items-center justify-between text-xs font-bold text-slate-700 dark:text-slate-300">
                <div className="flex items-center gap-3">
                  <span>التقدم العام: {progressPercent}%</span>
                  <span className="text-emerald-500 font-medium">✓ ناجح: {joinedCount}</span>
                  <span className="text-red-500 font-medium">✗ فشل: {failedCount}</span>
                  <span className="text-slate-400 font-medium">⏳ متبقي: {pendingCount}</span>
                </div>
                <div className="flex items-center gap-2">
                  {failedCount > 0 && (
                    <button
                      onClick={handleRetryFailed}
                      className="text-amber-600 hover:underline text-[11px]"
                    >
                      إعادة محاولة الفاشل
                    </button>
                  )}
                  <button
                    onClick={handleClearCompleted}
                    className="text-slate-400 hover:text-slate-600 text-[11px]"
                  >
                    مسح المكتمل
                  </button>
                  <button
                    onClick={handleClearAll}
                    className="text-red-500 hover:text-red-600 text-[11px]"
                  >
                    مسح الكل
                  </button>
                </div>
              </div>

              <div className="w-full h-2 bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
                <div
                  className="h-full bg-gradient-to-r from-sky-500 to-emerald-500 transition-all duration-500"
                  style={{ width: `${progressPercent}%` }}
                />
              </div>
            </div>
          )}

          {/* Queue List */}
          <div className="space-y-2">
            <h4 className="font-bold text-slate-700 dark:text-slate-300">
              قائمة الانتظار والمعالجة ({queue.length})
            </h4>

            {queue.length === 0 ? (
              <div className="text-center py-12 text-slate-400 text-xs border border-dashed border-slate-200 dark:border-slate-800 rounded-2xl">
                <Layers className="w-8 h-8 mx-auto mb-2 text-slate-300 dark:text-slate-700" />
                <span>القائمة فارغة. الصق روابط القنوات أعلاه لبدء الانضمام التلقائي.</span>
              </div>
            ) : (
              <div className="max-h-60 overflow-y-auto space-y-1.5 p-1">
                {queue.map((item, idx) => (
                  <div
                    key={item.id}
                    className={`p-2.5 rounded-xl border flex items-center justify-between transition-all ${
                      item.status === "joining"
                        ? "bg-sky-50 dark:bg-sky-950/40 border-sky-300 dark:border-sky-800 animate-pulse"
                        : item.status === "joined" || item.status === "already_member"
                        ? "bg-emerald-50/50 dark:bg-emerald-950/20 border-emerald-200 dark:border-emerald-900/50"
                        : item.status === "failed"
                        ? "bg-red-50/50 dark:bg-red-950/20 border-red-200 dark:border-red-900/50"
                        : "bg-slate-50 dark:bg-slate-800/60 border-slate-200 dark:border-slate-700"
                    }`}
                  >
                    <div className="flex items-center gap-2 min-w-0">
                      <span className="text-[10px] text-slate-400 w-5 text-center font-mono">#{idx + 1}</span>
                      <div className="min-w-0">
                        <div className="font-bold text-xs text-slate-800 dark:text-white truncate">
                          {item.title || item.linkOrUsername}
                        </div>
                        {item.title && (
                          <div className="text-[10px] text-slate-400 font-mono truncate" dir="ltr">
                            {item.linkOrUsername}
                          </div>
                        )}
                        {item.error && (
                          <div className="text-[10px] text-red-500 font-medium truncate">
                            {item.error}
                          </div>
                        )}
                      </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      {item.status === "pending" && (
                        <span className="text-[10px] px-2 py-0.5 rounded-md bg-slate-200 dark:bg-slate-700 text-slate-600 dark:text-slate-300">
                          قيد الانتظار
                        </span>
                      )}
                      {item.status === "joining" && (
                        <span className="text-[10px] px-2 py-0.5 rounded-md bg-sky-500 text-white font-bold flex items-center gap-1">
                          <RefreshCw className="w-3 h-3 animate-spin" />
                          <span>جاري الانضمام...</span>
                        </span>
                      )}
                      {item.status === "joined" && (
                        <span className="text-[10px] px-2 py-0.5 rounded-md bg-emerald-100 dark:bg-emerald-900/60 text-emerald-600 dark:text-emerald-300 font-bold flex items-center gap-1">
                          <CheckCircle2 className="w-3 h-3" />
                          <span>تم الانضمام</span>
                        </span>
                      )}
                      {item.status === "already_member" && (
                        <span className="text-[10px] px-2 py-0.5 rounded-md bg-teal-100 dark:bg-teal-900/60 text-teal-600 dark:text-teal-300 font-bold">
                          عضو مسبقاً
                        </span>
                      )}
                      {item.status === "failed" && (
                        <span className="text-[10px] px-2 py-0.5 rounded-md bg-red-100 dark:bg-red-900/60 text-red-600 dark:text-red-300 font-bold flex items-center gap-1">
                          <AlertCircle className="w-3 h-3" />
                          <span>فشل</span>
                        </span>
                      )}
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
            {isRunning ? "🟢 تستمر عملية الانضمام في الخلفية حتى اكتمال القائمة" : "⚪ العملية متوقفة حالياً"}
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
