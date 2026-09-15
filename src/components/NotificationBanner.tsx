import React, { useEffect, useState } from 'react';
import { X, MessageSquare } from 'lucide-react';

export interface InAppNotification {
  id: string;
  chatId: string;
  senderName: string;
  senderAvatar?: string;
  chatTitle?: string;
  text: string;
  timestamp: number;
}

interface NotificationBannerProps {
  notification: InAppNotification | null;
  onDismiss: () => void;
  onOpenChat: (chatId: string) => void;
  isDark: boolean;
  isAr: boolean;
}

export const NotificationBanner: React.FC<NotificationBannerProps> = ({
  notification,
  onDismiss,
  onOpenChat,
  isDark,
  isAr,
}) => {
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    if (notification) {
      setIsVisible(true);
      const timer = setTimeout(() => {
        setIsVisible(false);
        setTimeout(onDismiss, 250);
      }, 4500);

      return () => clearTimeout(timer);
    } else {
      setIsVisible(false);
    }
  }, [notification?.id, onDismiss]);

  if (!notification) return null;

  const title =
    notification.chatTitle && notification.chatTitle !== notification.senderName
      ? isAr
        ? `${notification.senderName} في ${notification.chatTitle}`
        : `${notification.senderName} in ${notification.chatTitle}`
      : notification.senderName;

  return (
    <div
      id="in-app-notification-banner"
      className={`fixed top-4 z-50 max-w-md w-[calc(100%-2rem)] transition-all duration-300 ease-out transform ${
        isAr ? 'left-4 md:left-auto md:right-4' : 'right-4 md:right-auto md:left-4'
      } ${
        isVisible ? 'translate-y-0 opacity-100 scale-100' : '-translate-y-4 opacity-0 scale-95 pointer-events-none'
      }`}
    >
      <div
        onClick={() => {
          onOpenChat(notification.chatId);
          onDismiss();
        }}
        className={`flex items-center gap-3 p-3.5 rounded-2xl shadow-xl border cursor-pointer select-none transition-all hover:scale-[1.01] active:scale-[0.99] backdrop-blur-md ${
          isDark
            ? 'bg-[#1e2c3a]/95 text-white border-white/10 hover:bg-[#233344]'
            : 'bg-white/95 text-gray-900 border-gray-200/80 hover:bg-gray-50'
        }`}
      >
        {/* Avatar */}
        <div className="relative shrink-0">
          {notification.senderAvatar ? (
            <img
              src={notification.senderAvatar}
              alt={notification.senderName}
              referrerPolicy="no-referrer"
              className="w-11 h-11 rounded-full object-cover shadow-sm border border-black/10 dark:border-white/10"
              onError={(e) => {
                e.currentTarget.style.display = 'none';
                const fallback = e.currentTarget.nextElementSibling as HTMLElement;
                if (fallback) fallback.style.display = 'flex';
              }}
            />
          ) : null}
          <div
            className={`w-11 h-11 rounded-full flex items-center justify-center text-white font-bold text-sm shadow-sm ${
              notification.senderAvatar ? 'hidden' : 'flex'
            } bg-[#3390ec]`}
          >
            {notification.senderName.slice(0, 1) || <MessageSquare className="w-5 h-5" />}
          </div>
        </div>

        {/* Content */}
        <div className="min-w-0 flex-1">
          <div className="flex items-center justify-between gap-2">
            <h4 className="font-bold text-sm truncate text-[#3390ec] dark:text-[#52a5f5]">
              {title}
            </h4>
            <span className="text-[11px] opacity-60 shrink-0">
              {isAr ? 'الآن' : 'now'}
            </span>
          </div>
          <p className="text-xs truncate opacity-85 mt-0.5 font-normal">
            {notification.text || (isAr ? 'رسالة جديدة' : 'New message')}
          </p>
        </div>

        {/* Close Button */}
        <button
          id="btn-dismiss-notification"
          onClick={(e) => {
            e.stopPropagation();
            setIsVisible(false);
            setTimeout(onDismiss, 200);
          }}
          className="p-1.5 rounded-full hover:bg-black/10 dark:hover:bg-white/10 text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 transition shrink-0"
          aria-label="Dismiss notification"
        >
          <X className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};
