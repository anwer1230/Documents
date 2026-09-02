import React, { useState } from "react";
import { X, Layers, Play, CheckCircle2, AlertCircle, Loader2, Link2, Sparkles } from "lucide-react";
import { apiResolveLink, apiJoinChat, getStoredSession } from "../../lib/telegramApi";

interface BulkJoinModalProps {
  isOpen: boolean;
  onClose: () => void;
  onBatchJoined: (joinedCount: number) => void;
}

interface JoinTaskResult {
  link: string;
  title?: string;
  status: "pending" | "processing" | "success" | "error";
  message?: string;
}

export const BulkJoinModal: React.FC<BulkJoinModalProps> = ({
  isOpen,
  onClose,
  onBatchJoined,
}) => {
  const [linksText, setLinksText] = useState("");
  const [tasks, setTasks] = useState<JoinTaskResult[]>([]);
  const [isRunning, setIsRunning] = useState(false);
  const [completedCount, setCompletedCount] = useState(0);

  if (!isOpen) return null;

  const handleStartBulkJoin = async () => {
    const lines = linksText
      .split("\n")
      .map((l) => l.trim())
      .filter((l) => l.length > 0);

    if (lines.length === 0) return;

    const initialTasks: JoinTaskResult[] = lines.map((link) => ({
      link,
      status: "pending",
    }));

    setTasks(initialTasks);
    setIsRunning(true);
    setCompletedCount(0);

    const sessionString = getStoredSession() || "";
    let successJoined = 0;

    for (let i = 0; i < initialTasks.length; i++) {
      setTasks((prev) =>
        prev.map((t, idx) => (idx === i ? { ...t, status: "processing" } : t))
      );

      const targetLink = initialTasks[i].link;
      try {
        const resolved = await apiResolveLink(targetLink, sessionString);
        await apiJoinChat({
          sessionString,
          hash: resolved.hash,
          chatId: resolved.id ? String(resolved.id) : resolved.username,
        });

        successJoined++;
        setTasks((prev) =>
          prev.map((t, idx) =>
            idx === i
              ? {
                  ...t,
                  title: resolved.title,
                  status: "success",
                  message: `تم الانضمام بنجاح (${resolved.title})`,
                }
              : t
          )
        );
      } catch (err: any) {
        setTasks((prev) =>
          prev.map((t, idx) =>
            idx === i
              ? {
                  ...t,
                  status: "error",
                  message: err.message || "تعذر الانضمام",
                }
              : t
          )
        );
      }

      setCompletedCount(i + 1);
      // Brief pause between requests to prevent Telegram flood wait limits
      await new Promise((r) => setTimeout(r, 600));
    }

    setIsRunning(false);
    onBatchJoined(successJoined);
  };

  return (
    <div
      id="bulk-join-modal-overlay"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fade-in"
      dir="rtl"
    >
      <div
        id="bulk-join-modal"
        className="w-full max-w-lg bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-200 dark:border-slate-800 overflow-hidden flex flex-col p-6 select-none max-h-[90vh]"
      >
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800">
          <div className="flex items-center gap-2.5 text-indigo-500 font-bold text-base">
            <Layers className="w-5 h-5" />
            <div>
              <h3 className="text-slate-800 dark:text-white">الانضمام الجماعي للقنوات والمجموعات (Bulk Joiner)</h3>
              <p className="text-[11px] text-slate-400 font-normal">انضمام لعدة روابط ومعرفات بضغطة واحدة</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Input Area */}
        <div className="mt-4">
          <label className="text-xs font-semibold text-slate-700 dark:text-slate-300 block mb-1">
            أدخل روابط القنوات أو معرفاتها (رابط في كل سطر):
          </label>
          <textarea
            disabled={isRunning}
            value={linksText}
            onChange={(e) => setLinksText(e.target.value)}
            placeholder={"t.me/telegram\nt.me/durov\nt.me/+AbCdEf123\n@channel_name"}
            className="w-full h-28 p-3 bg-slate-100 dark:bg-slate-800/80 rounded-2xl text-xs font-mono text-slate-800 dark:text-slate-100 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 resize-none dir-ltr text-left"
          />
        </div>

        {/* Progress & Task Status */}
        {tasks.length > 0 && (
          <div className="mt-3 space-y-2 overflow-y-auto flex-1 max-h-44 p-1">
            <div className="flex items-center justify-between text-xs font-semibold text-slate-600 dark:text-slate-400">
              <span>تقدم العملية:</span>
              <span>
                {completedCount} من {tasks.length}
              </span>
            </div>
            <div className="w-full h-2 bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
              <div
                className="h-full bg-indigo-500 transition-all duration-300 rounded-full"
                style={{ width: `${(completedCount / tasks.length) * 100}%` }}
              />
            </div>

            <div className="space-y-1.5 pt-1">
              {tasks.map((task, idx) => (
                <div
                  key={idx}
                  className="p-2 bg-slate-50 dark:bg-slate-800/40 rounded-xl text-xs flex items-center justify-between border border-slate-100 dark:border-slate-800"
                >
                  <span className="font-mono text-[11px] truncate max-w-[200px]" dir="ltr">
                    {task.link}
                  </span>
                  <div className="flex items-center gap-1 text-[11px]">
                    {task.status === "processing" && (
                      <span className="text-amber-500 flex items-center gap-1">
                        <Loader2 className="w-3.5 h-3.5 animate-spin" /> جاري الانضمام...
                      </span>
                    )}
                    {task.status === "success" && (
                      <span className="text-emerald-500 flex items-center gap-1 font-bold">
                        <CheckCircle2 className="w-3.5 h-3.5" /> {task.title || "تم بنجاح"}
                      </span>
                    )}
                    {task.status === "error" && (
                      <span className="text-red-500 flex items-center gap-1">
                        <AlertCircle className="w-3.5 h-3.5" /> فشل
                      </span>
                    )}
                    {task.status === "pending" && (
                      <span className="text-slate-400">في الانتظار</span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Footer Actions */}
        <div className="mt-4 pt-3 border-t border-slate-100 dark:border-slate-800 flex justify-end gap-2">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 font-bold text-xs rounded-xl"
          >
            إغلاق
          </button>
          <button
            disabled={isRunning || !linksText.trim()}
            onClick={handleStartBulkJoin}
            className="px-5 py-2.5 bg-indigo-500 hover:bg-indigo-600 active:scale-98 disabled:opacity-50 text-white font-bold text-xs rounded-xl shadow-md transition-all flex items-center gap-2"
          >
            {isRunning ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>جاري التنفيذ...</span>
              </>
            ) : (
              <>
                <Play className="w-4 h-4 fill-current" />
                <span>بدء الانضمام المتعدد</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
