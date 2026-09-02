import React, { useState } from "react";
import { X, Link2, ArrowLeft, Loader2, AlertCircle, Sparkles } from "lucide-react";
import { apiResolveLink, getStoredSession } from "../lib/telegramApi";
import { ResolvedTelegramLink } from "../types";

interface LinkPromptModalProps {
  isOpen: boolean;
  onClose: () => void;
  onResolved: (data: ResolvedTelegramLink) => void;
}

export const LinkPromptModal: React.FC<LinkPromptModalProps> = ({
  isOpen,
  onClose,
  onResolved,
}) => {
  const [linkInput, setLinkInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!linkInput.trim()) return;

    setError(null);
    setLoading(true);

    try {
      const sessionString = getStoredSession() || "";
      const result = await apiResolveLink(linkInput.trim(), sessionString);
      onResolved(result);
      onClose();
      setLinkInput("");
    } catch (err: any) {
      setError(err.message || "تعذر العثور على الرابط أو القناة المحددة");
    } finally {
      setLoading(false);
    }
  };

  const sampleLinks = [
    { label: "قناة تيليجرام الرسمية", link: "t.me/telegram" },
    { label: "أخبار التحديثات", link: "t.me/durov" },
    { label: "بوت الألعاب", link: "t.me/gamebot" },
  ];

  return (
    <div
      id="link-prompt-modal-overlay"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fade-in"
      dir="rtl"
    >
      <div
        id="link-prompt-modal"
        className="w-full max-w-md bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-200 dark:border-slate-800 overflow-hidden flex flex-col p-6 select-none"
      >
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800">
          <div className="flex items-center gap-2 text-sky-500 font-bold text-base">
            <Link2 className="w-5 h-5" />
            <h3 className="text-slate-800 dark:text-white">الانضمام عبر رابط أو معرف تيليجرام</h3>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="mt-5 space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-2">
              أدخل رابط الدعوة أو اسم المستخدم العام:
            </label>
            <div className="relative">
              <input
                id="link-prompt-input"
                type="text"
                value={linkInput}
                onChange={(e) => setLinkInput(e.target.value)}
                placeholder="مثال: t.me/+AbCdEf123 أو @durov"
                className="w-full px-4 py-3 bg-slate-100 dark:bg-slate-800/80 rounded-2xl text-xs text-slate-800 dark:text-slate-100 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-sky-500 transition-all dir-ltr text-left"
                autoFocus
              />
            </div>
            <p className="text-[11px] text-slate-400 mt-1.5 leading-relaxed">
              يدعم روابط الدعوة الخاصة (t.me/+hash)، روابط القنوات (t.me/username)، ومعرفات @usernames.
            </p>
          </div>

          {/* Quick Suggestions */}
          <div>
            <span className="text-[11px] font-semibold text-slate-400 block mb-1.5">روابط سريعة للتجربة:</span>
            <div className="flex flex-wrap gap-1.5">
              {sampleLinks.map((s, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => setLinkInput(s.link)}
                  className="px-2.5 py-1 bg-slate-100 dark:bg-slate-800 hover:bg-sky-50 dark:hover:bg-sky-950/50 hover:text-sky-600 dark:hover:text-sky-400 rounded-lg text-[11px] font-medium transition-colors"
                >
                  {s.label} ({s.link})
                </button>
              ))}
            </div>
          </div>

          {/* Error Message */}
          {error && (
            <div className="p-3 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800/50 rounded-xl text-red-600 dark:text-red-400 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* Submit Action */}
          <div className="pt-2">
            <button
              id="link-prompt-submit-btn"
              type="submit"
              disabled={loading || !linkInput.trim()}
              className="w-full py-3 bg-sky-500 hover:bg-sky-600 active:scale-98 disabled:opacity-50 text-white font-bold text-xs rounded-2xl shadow-lg shadow-sky-500/25 transition-all flex items-center justify-center gap-2"
            >
              {loading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>جاري فحص الرابط مع خوادم تيليجرام...</span>
                </>
              ) : (
                <>
                  <Sparkles className="w-4 h-4" />
                  <span>فحص ومعاينة الرابط</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
