import React from "react";
import { X, Sparkles, Megaphone, Users, MessageSquare } from "lucide-react";
import { TelegramNotification } from "../types";

interface NotificationToastProps {
  notification: TelegramNotification | null;
  onDismiss: () => void;
  onClick: (chatId?: string) => void;
}

export const NotificationToast: React.FC<NotificationToastProps> = ({
  notification,
  onDismiss,
  onClick,
}) => {
  if (!notification) return null;

  return (
    <div
      id="telegram-notification-toast"
      onClick={() => onClick(notification.chatId ? String(notification.chatId) : undefined)}
      className="fixed top-9 left-1/2 -translate-x-1/2 z-50 w-full max-w-md px-4 pointer-events-auto cursor-pointer animate-slide-down"
      dir="rtl"
    >
      <div className="bg-white/95 dark:bg-slate-900/95 backdrop-blur-md border border-slate-200/80 dark:border-slate-700/80 shadow-2xl rounded-2xl p-3 flex items-center gap-3 transition-transform hover:scale-[1.01] active:scale-[0.99]">
        {/* Icon / Avatar */}
        <div className="w-10 h-10 rounded-full bg-gradient-to-tr from-sky-500 to-blue-600 flex items-center justify-center text-white shrink-0 shadow-sm">
          {notification.type === "join" ? (
            <Sparkles className="w-5 h-5" />
          ) : notification.type === "link" ? (
            <Megaphone className="w-5 h-5" />
          ) : (
            <MessageSquare className="w-5 h-5" />
          )}
        </div>

        {/* Content */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between">
            <h4 className="font-bold text-xs text-slate-800 dark:text-white truncate">
              {notification.title}
            </h4>
            <span className="text-[10px] text-slate-400">الآن</span>
          </div>
          <p className="text-xs text-slate-500 dark:text-slate-300 truncate mt-0.5">
            {notification.body}
          </p>
        </div>

        {/* Close Button */}
        <button
          onClick={(e) => {
            e.stopPropagation();
            onDismiss();
          }}
          className="w-6 h-6 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-600 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors shrink-0"
        >
          <X className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
};
