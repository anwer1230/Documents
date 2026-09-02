import React from 'react';
import { ExternalLink, Sparkles, Bot, CornerDownRight } from 'lucide-react';
import { BotInlineButton } from '../../types';
import { useTelegram } from '../../context/TelegramContext';
import confetti from 'canvas-confetti';

interface BotInlineKeyboardProps {
  keyboard?: BotInlineButton[][];
  messageId: string;
}

export const BotInlineKeyboard: React.FC<BotInlineKeyboardProps> = ({
  keyboard,
  messageId,
}) => {
  const { showToast, setActiveModal, settings } = useTelegram();
  const isRtl = settings.language === 'ar';

  if (!keyboard || keyboard.length === 0) return null;

  const handleButtonClick = (btn: BotInlineButton) => {
    if (btn.url) {
      window.open(btn.url, '_blank');
      return;
    }

    if (btn.web_app) {
      confetti({ particleCount: 30, spread: 50, origin: { y: 0.6 } });
      setActiveModal('mini-apps');
      showToast(isRtl ? `فتح تطبيق الويب: ${btn.text}` : `Launching Mini App: ${btn.text}`, '🚀');
      return;
    }

    if (btn.callback_data) {
      confetti({ particleCount: 20, spread: 40, origin: { y: 0.7 } });
      showToast(
        isRtl
          ? `تم إرسال استعلام البوت: ${btn.text} (${btn.callback_data})`
          : `Bot Callback Triggered: ${btn.callback_data}`,
        '🤖'
      );
      return;
    }

    if (btn.switch_inline_query !== undefined) {
      showToast(
        isRtl
          ? `استعلام البوت المضمن: @bot ${btn.switch_inline_query}`
          : `Inline Query: @bot ${btn.switch_inline_query}`,
        '🔍'
      );
    }
  };

  return (
    <div
      id={`tg-bot-inline-keyboard-${messageId}`}
      className="mt-2 flex flex-col gap-1.5 w-full select-none"
    >
      {keyboard.map((row, rowIdx) => (
        <div key={rowIdx} className="flex items-center gap-1.5 w-full">
          {row.map((btn, btnIdx) => (
            <button
              key={btnIdx}
              onClick={() => handleButtonClick(btn)}
              className="flex-1 py-1.5 px-3 rounded-xl bg-black/30 hover:bg-black/50 border border-white/10 text-white font-bold text-xs shadow-sm transition-all active:scale-95 flex items-center justify-center gap-1.5 backdrop-blur-sm truncate"
            >
              <span className="truncate">{btn.text}</span>
              {btn.url && <ExternalLink className="w-3 h-3 text-sky-400 shrink-0" />}
              {btn.web_app && <Sparkles className="w-3 h-3 text-amber-400 shrink-0" />}
            </button>
          ))}
        </div>
      ))}
    </div>
  );
};
