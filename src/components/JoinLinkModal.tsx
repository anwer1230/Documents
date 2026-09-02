import React, { useState } from "react";
import {
  X,
  Users,
  Megaphone,
  Check,
  Loader2,
  ExternalLink,
  ShieldCheck,
  Sparkles,
  ArrowRight,
  AlertCircle,
} from "lucide-react";
import { ResolvedTelegramLink } from "../types";
import { apiJoinChat, getStoredSession, playTelegramSound } from "../lib/telegramApi";

const getAvatarColor = (name: string) => {
  const colors = [
    'from-red-500 to-orange-500',
    'from-blue-500 to-cyan-500',
    'from-emerald-500 to-teal-500',
    'from-purple-500 to-indigo-500',
    'from-pink-500 to-rose-500',
    'from-amber-500 to-yellow-500',
  ];
  let hash = 0;
  for (let i = 0; i < (name || '').length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
  return colors[Math.abs(hash) % colors.length];
};

interface JoinLinkModalProps {
  isOpen: boolean;
  onClose: () => void;
  resolvedLink: ResolvedTelegramLink | null;
  onJoinedSuccess: (chatId: string, title: string) => void;
}

export const JoinLinkModal: React.FC<JoinLinkModalProps> = ({
  isOpen,
  onClose,
  resolvedLink,
  onJoinedSuccess,
}) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen || !resolvedLink) return null;

  const handleJoin = async () => {
    setError(null);
    setLoading(true);

    try {
      const sessionString = getStoredSession() || "";
      const result = await apiJoinChat({
        sessionString,
        hash: resolvedLink.hash,
        username: resolvedLink.username,
        chatId: resolvedLink.id ? String(resolvedLink.id) : undefined,
      });

      playTelegramSound("join");
      onJoinedSuccess(result.chatId, resolvedLink.title);
      onClose();
    } catch (err: any) {
      setError(err.message || "فشل الانضمام إلى الرابط المحدد");
    } finally {
      setLoading(false);
    }
  };

  const isChannel = resolvedLink.isChannel;
  const isGroup = resolvedLink.isGroup;
  const avatarColorClass = getAvatarColor(resolvedLink.title || "TG");

  return (
    <div
      id="telegram-join-link-overlay"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fade-in"
      dir="rtl"
    >
      <div
        id="telegram-join-link-modal"
        className="w-full max-w-sm bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-200 dark:border-slate-800 overflow-hidden flex flex-col p-6 text-center select-none animate-scale-in"
      >
        {/* Close Button */}
        <div className="flex justify-end -mt-2 -mr-2 mb-2">
          <button
            id="join-modal-close-btn"
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Channel/Group Avatar */}
        <div className="relative mx-auto mb-4">
          <div
            className={`w-24 h-24 rounded-full bg-gradient-to-tr ${avatarColorClass} flex items-center justify-center text-white font-bold text-3xl shadow-xl shadow-sky-500/20`}
          >
            {resolvedLink.title ? resolvedLink.title.substring(0, 2).toUpperCase() : "TG"}
          </div>
          <div className="absolute -bottom-1 -right-1 w-8 h-8 rounded-full bg-sky-500 text-white flex items-center justify-center shadow-md border-2 border-white dark:border-slate-900">
            {isChannel ? <Megaphone className="w-4 h-4" /> : <Users className="w-4 h-4" />}
          </div>
        </div>

        {/* Title & Verified Badge */}
        <h3 className="text-lg font-bold text-slate-800 dark:text-white flex items-center justify-center gap-1.5 px-2">
          <span className="truncate">{resolvedLink.title}</span>
          {resolvedLink.verified && (
            <span className="text-sky-500 text-sm font-bold shrink-0">✓</span>
          )}
        </h3>

        {/* Username if present */}
        {resolvedLink.username && (
          <p className="text-xs font-mono text-sky-500 mt-0.5" dir="ltr">
            @{resolvedLink.username}
          </p>
        )}

        {/* Member / Subscriber Count */}
        <p className="text-xs text-slate-400 mt-1 font-medium">
          {resolvedLink.participantsCount
            ? isChannel
              ? `${resolvedLink.participantsCount.toLocaleString()} مشترك`
              : `${resolvedLink.participantsCount.toLocaleString()} عضو`
            : isChannel
            ? "قناة تيليجرام"
            : "مجموعة تيليجرام"}
        </p>

        {/* Description / About */}
        {resolvedLink.about && (
          <div className="my-4 p-3 bg-slate-50 dark:bg-slate-800/60 rounded-2xl text-xs text-slate-600 dark:text-slate-300 leading-relaxed text-right max-h-24 overflow-y-auto">
            {resolvedLink.about}
          </div>
        )}

        {/* Error Banner */}
        {error && (
          <div className="mb-3 p-2.5 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800/50 rounded-xl text-red-600 dark:text-red-400 text-xs flex items-center gap-2 text-right">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span className="truncate">{error}</span>
          </div>
        )}

        {/* Join or Open Chat Action Button */}
        <div className="mt-2 space-y-2">
          <button
            id="join-modal-submit-btn"
            onClick={handleJoin}
            disabled={loading}
            className="w-full py-3 bg-sky-500 hover:bg-sky-600 active:scale-98 disabled:opacity-50 text-white font-bold text-sm rounded-2xl shadow-lg shadow-sky-500/25 transition-all flex items-center justify-center gap-2"
          >
            {loading ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>جاري الانضمام...</span>
              </>
            ) : resolvedLink.alreadyJoined ? (
              <>
                <span>فتح المحادثة (عضو بالفعل)</span>
                <ArrowRight className="w-4 h-4 -scale-x-100" />
              </>
            ) : (
              <>
                <Sparkles className="w-4 h-4" />
                <span>{isChannel ? "الانضمام إلى القناة" : "الانضمام إلى المجموعة"}</span>
              </>
            )}
          </button>

          <button
            id="join-modal-cancel-btn"
            type="button"
            onClick={onClose}
            className="w-full py-2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-300 text-xs font-semibold rounded-xl transition-colors"
          >
            إلغاء
          </button>
        </div>
      </div>
    </div>
  );
};
