import React from "react";
import { Send, Users, Megaphone, ArrowUpRight } from "lucide-react";

interface LinkPreviewCardProps {
  url: string;
  isOut?: boolean;
  onOpenInviteModal: (url: string) => void;
}

export const LinkPreviewCard: React.FC<LinkPreviewCardProps> = ({
  url,
  isOut = false,
  onOpenInviteModal,
}) => {
  const isTelegram = url.includes("t.me/") || url.includes("tg://");
  const isInvite = url.includes("/+") || url.includes("joinchat");

  // Extract clean username or slug
  let slug = "";
  try {
    const cleanUrl = url.replace(/^(https?:\/\/)?(www\.)?/, "");
    const parts = cleanUrl.split("/");
    slug = parts[parts.length - 1] || "";
  } catch {
    slug = "";
  }

  // Format title for display
  const displayTitle = slug
    ? decodeURIComponent(slug).replace(/^[+]/, "")
    : "رابط تيليجرام";

  return (
    <div
      id="telegram-link-preview-card"
      className={`mt-2 rounded-xl p-2.5 text-xs transition-all border-r-3 border-[#2481cc] ${
        isOut
          ? "bg-[#224466]/40 dark:bg-[#1f374e]/60 text-white"
          : "bg-slate-50 dark:bg-[#1d2733] border-slate-200 dark:border-slate-700/80 text-slate-800 dark:text-slate-100"
      }`}
    >
      {/* Brand Header */}
      <div className="flex items-center gap-1.5 mb-1 text-[#2481cc] dark:text-[#5288c1] font-bold text-[11px]">
        <Send className="w-3 h-3 rotate-45 shrink-0" />
        <span>Telegram</span>
      </div>

      {/* Channel/Group Title */}
      <div className="font-bold text-xs text-slate-900 dark:text-white mb-0.5 truncate">
        {displayTitle.startsWith("Maths") ? "اساسيات الرياضيات 📚" : displayTitle}
      </div>

      {/* Subtitle / Handle */}
      <p className="text-[11px] text-slate-500 dark:text-slate-400 truncate mb-2">
        {isInvite ? "رابط دعوة لمجموعة تيليجرام خاصة" : `@${displayTitle}`}
      </p>

      {/* Action Button */}
      {isTelegram && (
        <button
          onClick={(e) => {
            e.stopPropagation();
            onOpenInviteModal(url);
          }}
          className="w-full py-1.5 px-3 rounded-lg text-xs font-bold flex items-center justify-center gap-1.5 transition-all bg-[#2481cc] hover:bg-[#1d6fa5] text-white shadow-xs"
        >
          {isInvite ? <Users className="w-3.5 h-3.5" /> : <ArrowUpRight className="w-3.5 h-3.5" />}
          <span>{isInvite ? "فتح المجموعة للانضمام" : "عرض القناة"}</span>
        </button>
      )}
    </div>
  );
};

