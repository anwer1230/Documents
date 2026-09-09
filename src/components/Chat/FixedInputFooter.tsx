import React from "react";
import { useTelegram } from "../../context/TelegramContext";

export interface FixedInputFooterProps {
  children?: React.ReactNode;
  className?: string;
  id?: string;
}

/**
 * FixedInputFooter - Base fixed input footer container for the chat interface.
 * 
 * Forms the bottom anchor of the Telegram chat view. 
 * Remains fixed at the viewport bottom on mobile and desktop,
 * with safe-area insets for mobile gesture bars / home indicators.
 */
export const FixedInputFooter: React.FC<FixedInputFooterProps> = ({
  children,
  className = "",
  id = "tg-fixed-input-footer",
}) => {
  const { settings } = useTelegram();
  const isArabic = settings.language === "ar";

  return (
    <footer
      id={id}
      data-testid="fixed-input-footer"
      className={`sticky bottom-0 shrink-0 w-full z-20 select-none transition-colors ${className}`}
      dir={isArabic ? "rtl" : "ltr"}
    >
      {children}
    </footer>
  );
};
