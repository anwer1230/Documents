import React, { useState } from "react";
import { Bookmark, Bot, Megaphone, Users } from "lucide-react";
import { getStoredSession } from "../lib/telegramApi";

// DrKLO/Telegram Android official 7-color signature avatar palettes
const TG_AVATAR_GRADIENTS = [
  { name: "red", bg: "from-[#ff516a] to-[#ff885e]", text: "text-white" },
  { name: "orange", bg: "from-[#ff8e01] to-[#ffaa00]", text: "text-white" },
  { name: "violet", bg: "from-[#8c6dfd] to-[#a682ff]", text: "text-white" },
  { name: "green", bg: "from-[#54cb68] to-[#6bd37e]", text: "text-white" },
  { name: "cyan", bg: "from-[#28c9b7] to-[#32dfcc]", text: "text-white" },
  { name: "blue", bg: "from-[#2a9ef5] to-[#45b4ff]", text: "text-white" },
  { name: "pink", bg: "from-[#e15480] to-[#ea6d94]", text: "text-white" },
];

export function getTelegramAvatarGradient(key?: string): string {
  if (!key) return TG_AVATAR_GRADIENTS[5].bg;
  let hash = 0;
  for (let i = 0; i < key.length; i++) {
    hash = key.charCodeAt(i) + ((hash << 5) - hash);
  }
  const index = Math.abs(hash) % TG_AVATAR_GRADIENTS.length;
  return TG_AVATAR_GRADIENTS[index].bg;
}

export function getTelegramInitials(name?: string): string {
  if (!name) return "TG";
  const trimmed = name.trim();
  if (!trimmed) return "TG";
  
  // Handle Arabic and multi-word names
  const parts = trimmed.split(/\s+/).filter(Boolean);
  if (parts.length >= 2) {
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }
  return trimmed.substring(0, Math.min(2, trimmed.length)).toUpperCase();
}

interface TelegramAvatarProps {
  id?: string;
  name?: string;
  photoUrl?: string;
  type?: "user" | "group" | "channel" | "bot" | "saved" | "private" | string;
  size?: "xs" | "sm" | "md" | "lg" | "xl" | "2xl" | "3xl" | number;
  isOnline?: boolean;
  className?: string;
  onClick?: () => void;
  showSpecialIcon?: boolean;
}

export const TelegramAvatar: React.FC<TelegramAvatarProps> = ({
  id,
  name,
  photoUrl,
  type = "user",
  size = "md",
  isOnline = false,
  className = "",
  onClick,
  showSpecialIcon = false,
}) => {
  const [imageError, setImageError] = useState(false);
  const session = getStoredSession();

  // Resolve size classes
  let sizeClass = "w-10 h-10 text-sm";
  let dotSizeClass = "w-3 h-3 border-2";
  let iconSize = 18;

  if (typeof size === "string") {
    switch (size) {
      case "xs":
        sizeClass = "w-6 h-6 text-[10px]";
        dotSizeClass = "w-2 h-2 border";
        iconSize = 12;
        break;
      case "sm":
        sizeClass = "w-8 h-8 text-xs";
        dotSizeClass = "w-2.5 h-2.5 border";
        iconSize = 14;
        break;
      case "md":
        sizeClass = "w-10 h-10 text-sm";
        dotSizeClass = "w-3 h-3 border-2";
        iconSize = 18;
        break;
      case "lg":
        sizeClass = "w-12 h-12 text-base";
        dotSizeClass = "w-3.5 h-3.5 border-2";
        iconSize = 22;
        break;
      case "xl":
        sizeClass = "w-14 h-14 text-lg";
        dotSizeClass = "w-4 h-4 border-2";
        iconSize = 26;
        break;
      case "2xl":
        sizeClass = "w-18 h-18 text-2xl";
        dotSizeClass = "w-4.5 h-4.5 border-2";
        iconSize = 32;
        break;
      case "3xl":
        sizeClass = "w-24 h-24 text-3xl";
        dotSizeClass = "w-6 h-6 border-4";
        iconSize = 44;
        break;
    }
  }

  // Determine effective photo URL
  let resolvedUrl = photoUrl;
  if (!resolvedUrl && id && id !== "saved" && session) {
    resolvedUrl = `/api/telegram/photo/${id}?sessionString=${encodeURIComponent(session)}`;
  } else if (resolvedUrl && !resolvedUrl.startsWith("http") && !resolvedUrl.startsWith("data:") && !resolvedUrl.includes("sessionString=") && session) {
    const separator = resolvedUrl.includes("?") ? "&" : "?";
    resolvedUrl = `${resolvedUrl}${separator}sessionString=${encodeURIComponent(session)}`;
  }

  const gradient = getTelegramAvatarGradient(id || name);
  const initials = getTelegramInitials(name);

  // Saved messages special avatar (Official Telegram Blue with Bookmark/Cloud icon)
  if (type === "saved" || name === "الرسائل المحفوظة" || name === "Saved Messages") {
    return (
      <div
        onClick={onClick}
        className={`relative shrink-0 select-none ${sizeClass} ${className} ${onClick ? "cursor-pointer" : ""}`}
      >
        <div className="w-full h-full rounded-full bg-gradient-to-tr from-sky-500 to-blue-600 flex items-center justify-center text-white shadow-xs">
          <Bookmark className="w-1/2 h-1/2 fill-current" />
        </div>
      </div>
    );
  }

  return (
    <div
      onClick={onClick}
      className={`relative shrink-0 select-none ${sizeClass} ${className} ${onClick ? "cursor-pointer" : ""}`}
    >
      {resolvedUrl && !imageError ? (
        <img
          src={resolvedUrl}
          alt={name || "Avatar"}
          onError={() => setImageError(true)}
          className="w-full h-full rounded-full object-cover shadow-xs border border-black/5 dark:border-white/5"
          referrerPolicy="no-referrer"
          loading="lazy"
        />
      ) : (
        <div
          className={`w-full h-full rounded-full bg-gradient-to-tr ${gradient} flex items-center justify-center text-white font-bold tracking-tight shadow-xs`}
        >
          {showSpecialIcon && type === "channel" ? (
            <Megaphone style={{ width: iconSize, height: iconSize }} />
          ) : showSpecialIcon && type === "group" ? (
            <Users style={{ width: iconSize, height: iconSize }} />
          ) : showSpecialIcon && type === "bot" ? (
            <Bot style={{ width: iconSize, height: iconSize }} />
          ) : (
            <span>{initials}</span>
          )}
        </div>
      )}

      {/* Online indicator dot */}
      {isOnline && (
        <span
          className={`absolute bottom-0 left-0 rounded-full bg-emerald-500 border-white dark:border-slate-900 ${dotSizeClass} shadow-xs`}
          title="متصل الآن"
        />
      )}
    </div>
  );
};
