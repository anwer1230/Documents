/**
 * ReactionsLayoutInBubble.tsx
 * Replicated directly from DrKLO/Telegram Android:
 * org.telegram.ui.Components.ReactionsLayoutInBubble.java
 * 
 * Renders interactive animated reaction badges inside or under chat bubbles,
 * handles user toggle states, reaction counts, and calls TLRPC.TL_messages_sendReaction.
 */

import React from 'react';
import { MessageReaction } from '../../types';
import { messagesController } from '../../core/MessagesController';
import { useTelegram } from '../../context/TelegramContext';

interface ReactionsLayoutInBubbleProps {
  chatId: string;
  messageId: string;
  reactions?: MessageReaction[];
  isOutgoing?: boolean;
}

export const ReactionsLayoutInBubble: React.FC<ReactionsLayoutInBubbleProps> = ({
  chatId,
  messageId,
  reactions = [],
  isOutgoing = false,
}) => {
  const { currentUser, toggleReaction } = useTelegram();

  if (!reactions || reactions.length === 0) {
    return null;
  }

  const handleReactionClick = (e: React.MouseEvent, emoji: string) => {
    e.stopPropagation();
    // 1. Context state toggle
    toggleReaction(messageId, emoji);
    // 2. DrKLO RPC Pipeline (TLRPC.TL_messages_sendReaction)
    messagesController.sendReaction(chatId, messageId, emoji, true).catch(() => {});
  };

  return (
    <div
      id={`reactions-layout-${messageId}`}
      className={`flex flex-wrap gap-1 mt-1.5 select-none ${
        isOutgoing ? 'justify-end' : 'justify-start'
      }`}
    >
      {reactions.map((reaction) => {
        const isChosen =
          reaction.users.includes(currentUser.id) ||
          reaction.users.includes('user_self') ||
          reaction.users.includes('me');

        return (
          <button
            key={reaction.emoji}
            id={`reaction-btn-${messageId}-${reaction.emoji}`}
            onClick={(e) => handleReactionClick(e, reaction.emoji)}
            className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold border transition-all duration-200 active:scale-95 ${
              isChosen
                ? 'bg-sky-500/25 border-sky-400 text-sky-300 shadow-sm scale-105 ring-1 ring-sky-400/30'
                : 'bg-black/25 border-white/10 text-gray-300 hover:bg-white/15 hover:border-white/20'
            }`}
            title={`${reaction.emoji} (${reaction.count})`}
          >
            <span className="text-sm leading-none animate-in zoom-in duration-150">
              {reaction.emoji}
            </span>
            <span className="text-[10px] font-mono font-bold opacity-90">
              {reaction.count}
            </span>
          </button>
        );
      })}
    </div>
  );
};
