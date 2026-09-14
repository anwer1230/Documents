import React, { forwardRef, useState, useRef } from "react";
import { ArrowDown } from "lucide-react";
import { useTelegram } from "../../context/TelegramContext";

export interface MessageListContainerProps {
  children?: React.ReactNode;
  className?: string;
  onScrollToBottom?: () => void;
  showScrollBottomButton?: boolean;
  unreadCount?: number;
  id?: string;
}

/**
 * MessageListContainer - Base message list viewport container
 * 
 * Provides responsive layout for mobile (full-bleed, touch scrolling) 
 * and desktop (contained width, custom scrollbars, wallpaper pattern).
 * Forms the primary upper structure of the Telegram-like chat interface.
 */
export const MessageListContainer = forwardRef<HTMLDivElement, MessageListContainerProps>(
  (
    {
      children,
      className = "",
      onScrollToBottom,
      showScrollBottomButton = false,
      unreadCount = 0,
      id = "tg-message-list-container",
    },
    ref
  ) => {
    const { settings } = useTelegram();
    const isArabic = settings.language === "ar";
    const [scrolledUp, setScrolledUp] = useState(false);
    const containerRef = useRef<HTMLDivElement | null>(null);

    const handleScrollBottomClick = () => {
      if (onScrollToBottom) {
        onScrollToBottom();
      } else if (containerRef.current) {
        const scroller = containerRef.current.querySelector("[data-list-scroller]") || containerRef.current;
        scroller.scrollTo({
          top: scroller.scrollHeight,
          behavior: "smooth",
        });
      }
    };

    return (
      <div
        id={id}
        data-testid="message-list-container"
        ref={(node) => {
          containerRef.current = node;
          if (typeof ref === "function") {
            ref(node);
          } else if (ref) {
            (ref as any).current = node;
          }
        }}
        className={`flex-1 min-h-0 w-full overflow-hidden relative flex flex-col select-text tg-wallpaper-pattern ${className}`}
        style={{
          backgroundColor: "var(--tg-theme-chat-bg)",
        }}
        dir={isArabic ? "rtl" : "ltr"}
      >
        {/* Responsive Content Wrapper: Full-bleed on mobile, centered on larger screens */}
        <div className="w-full h-full flex-1 min-h-0 flex flex-col relative transition-all duration-150">
          {children}
        </div>

        {/* Floating Scroll to Bottom Action Button */}
        {(showScrollBottomButton || scrolledUp) && (
          <div
            className={`fixed md:absolute bottom-20 z-30 transition-all duration-200 ${
              isArabic ? "left-4 sm:left-6" : "right-4 sm:right-6"
            }`}
          >
            <button
              id="tg-scroll-to-bottom-btn"
              type="button"
              onClick={handleScrollBottomClick}
              className="relative p-2.5 sm:p-3 rounded-full bg-[#17212b]/95 hover:bg-[#202b36] border border-white/10 text-gray-200 hover:text-white shadow-2xl backdrop-blur-md active:scale-95 transition-all flex items-center justify-center cursor-pointer group"
              title={isArabic ? "الانتقال إلى أحدث الرسائل" : "Scroll to bottom"}
              aria-label={isArabic ? "الانتقال للأسفل" : "Scroll to bottom"}
            >
              <ArrowDown className="w-4 h-4 sm:w-5 sm:h-5 text-[#2481cc] group-hover:translate-y-0.5 transition-transform" />
              {unreadCount > 0 && (
                <span className="absolute -top-1 -right-1 px-1.5 py-0.5 min-w-[18px] text-[10px] font-bold rounded-full bg-[#2481cc] text-white border border-[#17212b] shadow-md flex items-center justify-center">
                  {unreadCount > 99 ? "99+" : unreadCount}
                </span>
              )}
            </button>
          </div>
        )}
      </div>
    );
  }
);

MessageListContainer.displayName = "MessageListContainer";
