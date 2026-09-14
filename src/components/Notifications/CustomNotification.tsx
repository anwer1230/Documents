import React, { useState, useEffect, useCallback } from "react";
import { motion, AnimatePresence } from "motion/react";
import {
  MessageSquare,
  Megaphone,
  AtSign,
  PhoneCall,
  ShieldAlert,
  Heart,
  Pin,
  X,
  Reply,
  VolumeX,
  CornerDownLeft,
  UserCheck,
  ExternalLink,
  Flame,
  CheckCircle2,
} from "lucide-react";
import { InAppNotification } from "../../types";
import { notificationEngine } from "../../services/NotificationEngine";

export interface CustomNotificationProps {
  notification: InAppNotification;
  onDismiss: (id: string, direction?: "left" | "right" | "up" | "button") => void;
  onJumpToMessage?: (chatId: string, messageId: string) => void;
  onMessageSender?: (senderId: string, senderName: string, avatar?: string, username?: string) => void;
  onReply?: (chatId: string, messageId: string, senderName: string, textSnippet: string) => void;
  onMuteChat?: (chatId: string) => void;
  onOpenLink?: (link: string) => void;
  isArabic?: boolean;
}

const SESSION_KEY_COUNT = "tg_session_swipe_dismiss_count";
const SESSION_KEY_HIDDEN = "tg_session_notifications_hidden";

/**
 * CustomNotification - Custom Notification Component with Swipe-to-Dismiss Gesture Handling
 * 
 * Tracks swipe dismiss counts in local session state (sessionStorage).
 * Automatically hides notifications after 3 swipes per session.
 */
export const CustomNotification: React.FC<CustomNotificationProps> = ({
  notification,
  onDismiss,
  onJumpToMessage,
  onMessageSender,
  onReply,
  onMuteChat,
  onOpenLink,
  isArabic = false,
}) => {
  const [exitDirection, setExitDirection] = useState<"left" | "right" | "up">("right");
  const [isDragging, setIsDragging] = useState(false);
  const [sessionDismissCount, setSessionDismissCount] = useState<number>(() => {
    try {
      if (typeof window === "undefined") return 0;
      const count = sessionStorage.getItem(SESSION_KEY_COUNT);
      return count ? parseInt(count, 10) || 0 : 0;
    } catch {
      return 0;
    }
  });
  const [isSessionHidden, setIsSessionHidden] = useState<boolean>(() => {
    try {
      if (typeof window === "undefined") return false;
      return sessionStorage.getItem(SESSION_KEY_HIDDEN) === "true";
    } catch {
      return false;
    }
  });

  // Sync with session storage changes across tabs or instances
  useEffect(() => {
    const handleStorageChange = () => {
      try {
        const count = sessionStorage.getItem(SESSION_KEY_COUNT);
        const hidden = sessionStorage.getItem(SESSION_KEY_HIDDEN) === "true";
        setSessionDismissCount(count ? parseInt(count, 10) || 0 : 0);
        setIsSessionHidden(hidden);
      } catch {}
    };

    window.addEventListener("storage", handleStorageChange);
    return () => window.removeEventListener("storage", handleStorageChange);
  }, []);

  // Do not render if notifications are hidden for this session (after 3 swipes)
  if (isSessionHidden) {
    return null;
  }

  const isKeywordAlert = notification.category === "keyword_alert";

  const getCategoryIcon = () => {
    switch (notification.category) {
      case "keyword_alert":
        return <Flame className="w-3.5 h-3.5 text-amber-400 fill-amber-400 animate-pulse" />;
      case "channel_post":
        return <Megaphone className="w-3.5 h-3.5 text-sky-400" />;
      case "mention":
        return <AtSign className="w-3.5 h-3.5 text-amber-400" />;
      case "call":
        return <PhoneCall className="w-3.5 h-3.5 text-emerald-400 animate-pulse" />;
      case "system_security":
        return <ShieldAlert className="w-3.5 h-3.5 text-red-400" />;
      case "reaction":
        return <Heart className="w-3.5 h-3.5 text-rose-400 fill-rose-400" />;
      case "pinned":
        return <Pin className="w-3.5 h-3.5 text-yellow-400" />;
      default:
        return <MessageSquare className="w-3.5 h-3.5 text-sky-400" />;
    }
  };

  const handleSwipeDismiss = (direction: "left" | "right" | "up") => {
    setExitDirection(direction);
    // Record dismiss count in local session state
    try {
      const currentCount = sessionDismissCount;
      const nextCount = currentCount + 1;
      setSessionDismissCount(nextCount);
      sessionStorage.setItem(SESSION_KEY_COUNT, String(nextCount));
      sessionStorage.setItem("tg_inapp_left_swipe_count", String(nextCount));

      if (nextCount >= 3) {
        setIsSessionHidden(true);
        sessionStorage.setItem(SESSION_KEY_HIDDEN, "true");
        sessionStorage.setItem("tg_inapp_session_muted", "true");
      }
    } catch {}

    onDismiss(notification.id, direction);
  };

  const handleClickBanner = () => {
    if (isDragging) return;
    if (notification.chatId && notification.messageId && onJumpToMessage) {
      onJumpToMessage(notification.chatId, notification.messageId);
    }
    onDismiss(notification.id, "button");
  };

  return (
    <motion.div
      key={notification.id}
      id={`custom-notification-${notification.id}`}
      initial={{ opacity: 0, y: -45, scale: 0.94 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      exit={{
        opacity: 0,
        x: exitDirection === "right" ? 400 : exitDirection === "left" ? -400 : 0,
        y: exitDirection === "up" ? -80 : 0,
        scale: 0.9,
        transition: { duration: 0.22, ease: "easeOut" },
      }}
      transition={{ type: "spring", damping: 26, stiffness: 360 }}
      drag="x"
      dragConstraints={{ left: 0, right: 0 }}
      dragElastic={0.65}
      whileDrag={{ scale: 0.98, opacity: 0.9 }}
      onDragStart={() => setIsDragging(true)}
      onDragEnd={(_e, info) => {
        setTimeout(() => setIsDragging(false), 50);
        const threshold = 55;
        const velocityThreshold = 200;
        if (info.offset.x > threshold || info.velocity.x > velocityThreshold) {
          handleSwipeDismiss("right");
        } else if (info.offset.x < -threshold || info.velocity.x < -velocityThreshold) {
          handleSwipeDismiss("left");
        }
      }}
      className="w-full pointer-events-auto select-none touch-pan-y cursor-grab active:cursor-grabbing"
      dir={isArabic ? "rtl" : "ltr"}
    >
      <div
        onClick={handleClickBanner}
        className={`group relative overflow-hidden rounded-3xl p-4 shadow-2xl backdrop-blur-2xl border transition-all hover:scale-[1.008] active:scale-[0.99] ${
          isKeywordAlert
            ? "border-amber-500/50 bg-[#16130b]/95 shadow-amber-950/40"
            : "border-white/15 bg-[#1c2834]/95 shadow-black/60"
        }`}
        style={{
          boxShadow: isKeywordAlert
            ? "0 16px 40px -6px rgba(245, 158, 11, 0.25), 0 0 0 1px rgba(245, 158, 11, 0.3)"
            : "0 16px 40px -6px rgba(0, 0, 0, 0.65)",
        }}
      >
        {/* Subtle Swipe Indicator Bar */}
        <div className="flex items-center justify-between px-1 -mt-1 mb-2">
          <div className="w-10 h-1 bg-white/20 hover:bg-white/40 rounded-full mx-auto transition-colors pointer-events-none" />
          <span className="text-[10px] text-gray-400 font-mono select-none">
            {isArabic
              ? `سحب: ${sessionDismissCount}/3`
              : `Swipe: ${sessionDismissCount}/3`}
          </span>
        </div>

        {/* Glow Accent Top Bar */}
        <div
          className={`absolute top-0 left-0 right-0 h-1 ${
            isKeywordAlert
              ? "bg-gradient-to-r from-amber-500 via-rose-500 to-yellow-400 animate-pulse"
              : "bg-gradient-to-r from-sky-400 via-[#2481cc] to-indigo-500"
          }`}
        />

        <div className="flex items-start gap-3.5">
          {/* Avatar / Category Badge */}
          <div
            className={`relative shrink-0 w-12 h-12 rounded-2xl overflow-hidden flex items-center justify-center font-bold text-white shadow-lg ${
              isKeywordAlert
                ? "bg-gradient-to-br from-amber-500 to-rose-600 ring-2 ring-amber-400/40"
                : "bg-gradient-to-tr from-sky-600 to-cyan-500"
            }`}
          >
            {notification.avatar ? (
              <img
                src={notification.avatar}
                alt={notification.title}
                className="w-full h-full object-cover"
                referrerPolicy="no-referrer"
              />
            ) : (
              <span className="text-base">{notification.title.charAt(0).toUpperCase()}</span>
            )}
            <div
              className={`absolute -bottom-1 -right-1 w-5 h-5 rounded-full flex items-center justify-center border-2 border-gray-900 shadow-md ${
                isKeywordAlert ? "bg-amber-500 text-black" : "bg-gray-900"
              }`}
            >
              {getCategoryIcon()}
            </div>
          </div>

          {/* Body Content */}
          <div className="flex-1 min-w-0 pr-1">
            {/* Header: Title + Timestamp */}
            <div className="flex items-center justify-between gap-2 mb-1">
              <div className="flex items-center gap-1.5 min-w-0">
                <span
                  className={`font-black text-xs sm:text-[13px] truncate ${
                    isKeywordAlert ? "text-amber-300 flex items-center gap-1" : "text-white"
                  }`}
                >
                  {isKeywordAlert ? (
                    <>
                      <span className="text-rose-400">🚨</span>
                      <span>{isArabic ? "كلمة مراقبة:" : "Keyword Alert:"}</span>
                      <span className="px-1.5 py-0.5 rounded-md bg-amber-400/20 text-amber-200 border border-amber-400/30 text-[11px] font-mono">
                        {notification.keyword || notification.title.replace(/.*\\[(.*)\\].*/, "$1")}
                      </span>
                    </>
                  ) : (
                    notification.title
                  )}
                </span>
              </div>
              <span className="text-[10px] text-gray-400 shrink-0 font-mono">
                {notification.timestamp}
              </span>
            </div>

            {/* Message Body */}
            {isKeywordAlert ? (
              <div className="space-y-1 my-1.5 text-xs text-gray-200">
                <div className="flex items-start gap-1.5 line-clamp-2 leading-relaxed bg-black/30 p-2 rounded-xl border border-white/5">
                  <span className="shrink-0 text-amber-400 font-bold">
                    💬 {isArabic ? "الرسالة:" : "Message:"}
                  </span>
                  <span className="text-gray-100 font-medium break-words">
                    {notification.messageText || notification.body}
                  </span>
                </div>
                <div className="flex items-center justify-between gap-2 text-[11px] text-gray-400 pt-0.5">
                  <div className="flex items-center gap-1 truncate">
                    <span className="text-rose-400 font-bold">
                      📍 {isArabic ? "المصدر:" : "Source:"}
                    </span>
                    <span className="text-gray-300 font-semibold truncate">
                      {notification.chatTitle || notification.senderName || "المجموعة"}
                    </span>
                  </div>
                  {notification.chatUsername && onOpenLink && (
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onOpenLink(notification.chatUsername!);
                      }}
                      className="flex items-center gap-1 px-1.5 py-0.5 rounded-md bg-sky-500/20 hover:bg-sky-500/30 text-sky-300 font-mono text-[10px] shrink-0 border border-sky-400/30 transition-colors"
                    >
                      <span>t.me/{notification.chatUsername.replace("@", "")}</span>
                      <ExternalLink className="w-2.5 h-2.5" />
                    </button>
                  )}
                </div>
              </div>
            ) : (
              <p className="text-xs text-gray-300 line-clamp-2 leading-snug break-words mb-2">
                {notification.body}
              </p>
            )}

            {/* Action Buttons */}
            <div className="mt-2.5 flex items-center flex-wrap gap-2 pt-1 border-t border-white/10">
              {notification.chatId && notification.messageId && onJumpToMessage && (
                <button
                  id="tg-notif-action-jump-to-message"
                  onClick={(e) => {
                    e.stopPropagation();
                    onJumpToMessage(notification.chatId!, notification.messageId!);
                    onDismiss(notification.id, "button");
                  }}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold bg-[#2481cc] hover:bg-[#1f70b3] text-white shadow-md shadow-sky-950/40 active:scale-95 transition-all"
                >
                  <CornerDownLeft className="w-3.5 h-3.5" />
                  <span>{isArabic ? "الانتقال إلى الرسالة" : "Jump to Message"}</span>
                </button>
              )}

              {onMessageSender && (
                <button
                  id="tg-notif-action-message-sender"
                  onClick={(e) => {
                    e.stopPropagation();
                    const sId = notification.senderId || "user_unknown";
                    const sName = notification.senderName || "مستخدم تيليجرام";
                    onMessageSender(sId, sName, notification.avatar, notification.senderUsername);
                    onDismiss(notification.id, "button");
                  }}
                  className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold transition-all active:scale-95 shadow-md ${
                    isKeywordAlert
                      ? "bg-amber-500 hover:bg-amber-400 text-black shadow-amber-950/30"
                      : "bg-white/15 hover:bg-white/25 text-white"
                  }`}
                >
                  <UserCheck className="w-3.5 h-3.5" />
                  <span>{isArabic ? "متابعة المرسل" : "Message Sender"}</span>
                </button>
              )}

              {!isKeywordAlert && notification.replyAction && onReply && notification.chatId && (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onReply(
                      notification.chatId!,
                      notification.messageId || notification.id,
                      notification.senderName || notification.title,
                      notification.messageText || notification.body
                    );
                    onDismiss(notification.id, "button");
                  }}
                  className="flex items-center gap-1 px-2.5 py-1.5 rounded-xl text-xs font-medium bg-white/10 hover:bg-white/20 text-gray-200 transition-colors"
                >
                  <Reply className="w-3 h-3" />
                  <span>{isArabic ? "رد" : "Reply"}</span>
                </button>
              )}

              {notification.chatId && onMuteChat && (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onMuteChat(notification.chatId!);
                    onDismiss(notification.id, "button");
                  }}
                  className="flex items-center gap-1 px-2.5 py-1.5 rounded-xl text-xs font-medium bg-white/5 hover:bg-white/10 text-gray-400 hover:text-gray-200 transition-colors mr-auto rtl:mr-0 rtl:ml-auto"
                >
                  <VolumeX className="w-3 h-3" />
                  <span>{isArabic ? "كتم" : "Mute"}</span>
                </button>
              )}
            </div>
          </div>

          {/* Dismiss button */}
          <button
            id="custom-notif-dismiss-btn"
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              setExitDirection(isArabic ? "left" : "right");
              onDismiss(notification.id, "button");
            }}
            className="shrink-0 p-1.5 rounded-xl text-gray-400 hover:text-white hover:bg-white/15 active:scale-90 transition-all cursor-pointer"
            title={
              isArabic
                ? "إغلاق الإشعار (اسحب لإخفائه تلقائياً بعد 3 مرات بالجلسة)"
                : "Dismiss notification (swipe to hide automatically after 3 times per session)"
            }
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      </div>
    </motion.div>
  );
};
