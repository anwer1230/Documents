import React, { useState } from 'react';
import { ChevronDown, ChevronUp, Eye, Sparkles } from 'lucide-react';
import { TelegramMessageEntity } from '../types';

interface TelegramMessageContentProps {
  text: string;
  entities?: TelegramMessageEntity[];
  isOut?: boolean;
  isDark?: boolean;
}

// Map custom emojis to rich vector badges
const CUSTOM_EMOJIS_MAP: Record<string, { label: string; icon: string; bg: string; border: string }> = {
  'star': { label: 'Star', icon: '⭐', bg: 'from-amber-400 to-yellow-500', border: 'border-amber-300' },
  'gem': { label: 'Gem', icon: '💎', bg: 'from-cyan-400 to-blue-500', border: 'border-cyan-300' },
  'fire': { label: 'Fire', icon: '🔥', bg: 'from-orange-500 to-red-600', border: 'border-orange-400' },
  'crown': { label: 'Crown', icon: '👑', bg: 'from-yellow-400 to-amber-600', border: 'border-yellow-300' },
  'party': { label: 'Party', icon: '🎉', bg: 'from-purple-500 to-pink-500', border: 'border-pink-300' },
  'rocket': { label: 'Rocket', icon: '🚀', bg: 'from-blue-500 to-indigo-600', border: 'border-blue-400' },
  'diamond': { label: 'Diamond', icon: '💠', bg: 'from-teal-400 to-emerald-500', border: 'border-teal-300' },
  'heart': { label: 'Heart', icon: '💖', bg: 'from-rose-500 to-pink-600', border: 'border-rose-300' },
};

export const TelegramMessageContent: React.FC<TelegramMessageContentProps> = ({
  text,
  entities,
  isOut,
  isDark,
}) => {
  // State for revealed spoilers in this message
  const [revealedSpoilers, setRevealedSpoilers] = useState<Record<string, boolean>>({});
  // State for expanded blockquotes
  const [expandedQuotes, setExpandedQuotes] = useState<Record<string, boolean>>({});

  const toggleSpoiler = (key: string) => {
    setRevealedSpoilers((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  const toggleQuote = (key: string) => {
    setExpandedQuotes((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  // If message contains syntax like ||spoiler|| or > blockquote or [emoji:name] or entities
  const renderFormattedText = (rawText: string) => {
    // Process custom emojis: [emoji:name]
    const partsWithCustomEmoji = rawText.split(/(\[emoji:[a-zA-Z0-9_]+\])/g);

    return partsWithCustomEmoji.map((part, index) => {
      const emojiMatch = part.match(/\[emoji:([a-zA-Z0-9_]+)\]/);
      if (emojiMatch) {
        const emojiKey = emojiMatch[1].toLowerCase();
        const customItem = CUSTOM_EMOJIS_MAP[emojiKey] || {
          label: emojiKey,
          icon: '✨',
          bg: 'from-blue-500 to-indigo-500',
          border: 'border-blue-300',
        };
        return (
          <span
            key={`emoji-${index}`}
            className="inline-flex items-center gap-0.5 px-1.5 py-0.5 mx-0.5 text-xs font-semibold rounded-md align-middle shadow-xs animate-pulse select-none"
            style={{
              background: 'linear-gradient(135deg, rgba(51, 144, 236, 0.15), rgba(0, 168, 232, 0.25))',
              border: '1px solid rgba(51, 144, 236, 0.3)',
            }}
            title={`Telegram Custom Emoji: ${customItem.label}`}
          >
            <span className="text-sm transform hover:scale-125 transition-transform inline-block">
              {customItem.icon}
            </span>
            <span className="text-[10px] text-[#3390ec] font-bold tracking-tight">
              {customItem.label}
            </span>
          </span>
        );
      }

      // Process spoilers: ||spoiler text||
      const spoilerParts = part.split(/(\|\|[\s\S]*?\|\|)/g);

      return spoilerParts.map((subPart, subIdx) => {
        if (subPart.startsWith('||') && subPart.endsWith('||')) {
          const spoilerContent = subPart.slice(2, -2);
          const spoilerId = `sp-${index}-${subIdx}`;
          const isRevealed = revealedSpoilers[spoilerId];

          return (
            <span
              key={spoilerId}
              onClick={(e) => {
                e.stopPropagation();
                toggleSpoiler(spoilerId);
              }}
              title={isRevealed ? '' : 'انقر لكشف المحتوى المخفي (Spoiler)'}
              className={`inline cursor-pointer rounded px-1 transition-all duration-300 relative select-none ${
                isRevealed
                  ? 'bg-black/10 dark:bg-white/10 select-text'
                  : 'bg-gray-400/40 dark:bg-gray-600/60 blur-[3.5px] hover:blur-[2px] active:scale-95'
              }`}
            >
              {spoilerContent}
              {!isRevealed && (
                <span className="absolute inset-0 flex items-center justify-center pointer-events-none opacity-40">
                  <span className="w-full h-full bg-[radial-gradient(#3390ec_1px,transparent_1px)] [background-size:6px_6px] animate-pulse" />
                </span>
              )}
            </span>
          );
        }

        // Process blockquotes: lines starting with >
        const lines = subPart.split('\n');
        const renderedLines: React.ReactNode[] = [];
        let quoteBuffer: string[] = [];
        let quoteIndex = 0;

        const flushQuoteBuffer = () => {
          if (quoteBuffer.length > 0) {
            const qId = `q-${index}-${subIdx}-${quoteIndex++}`;
            const fullQuote = quoteBuffer.join('\n');
            const isLong = quoteBuffer.length > 3 || fullQuote.length > 120;
            const isExpanded = expandedQuotes[qId];
            const displayQuote = isLong && !isExpanded ? quoteBuffer.slice(0, 2).join('\n') + '...' : fullQuote;

            renderedLines.push(
              <div
                key={qId}
                className={`my-1.5 p-2 rounded-lg border-s-4 text-xs select-text transition-all ${
                  isOut
                    ? isDark
                      ? 'bg-black/25 border-[#3390ec]'
                      : 'bg-black/5 border-[#2881da]'
                    : isDark
                    ? 'bg-[#182533]/80 border-[#3390ec]'
                    : 'bg-blue-50/60 border-[#3390ec]'
                }`}
              >
                <div className="flex items-start justify-between gap-1">
                  <div className="font-serif italic whitespace-pre-wrap leading-relaxed">
                    {displayQuote}
                  </div>
                  {isLong && (
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        toggleQuote(qId);
                      }}
                      className="p-0.5 rounded text-[#3390ec] hover:opacity-80 shrink-0 select-none"
                      title={isExpanded ? 'طي الاقتباس' : 'توسيع الاقتباس'}
                    >
                      {isExpanded ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
                    </button>
                  )}
                </div>
              </div>
            );
            quoteBuffer = [];
          }
        };

        for (let lIdx = 0; lIdx < lines.length; lIdx++) {
          const line = lines[lIdx];
          if (line.startsWith('> ') || line === '>') {
            quoteBuffer.push(line.replace(/^>\s?/, ''));
          } else {
            flushQuoteBuffer();
            renderedLines.push(
              <span key={`line-${lIdx}`}>
                {lIdx > 0 && <br />}
                {formatInlineMarkdown(line)}
              </span>
            );
          }
        }
        flushQuoteBuffer();

        return <React.Fragment key={`sub-${subIdx}`}>{renderedLines}</React.Fragment>;
      });
    });
  };

  // Helper for inline bold, italic, code
  const formatInlineMarkdown = (lineStr: string) => {
    // Bold: **text**
    const boldParts = lineStr.split(/(\*\*[\s\S]*?\*\*)/g);
    return boldParts.map((bPart, bIdx) => {
      if (bPart.startsWith('**') && bPart.endsWith('**')) {
        return <strong key={bIdx} className="font-bold">{bPart.slice(2, -2)}</strong>;
      }
      // Italic: *text*
      const italicParts = bPart.split(/(\*[\s\S]*?\*)/g);
      return italicParts.map((iPart, iIdx) => {
        if (iPart.startsWith('*') && iPart.endsWith('*')) {
          return <em key={iIdx} className="italic">{iPart.slice(1, -1)}</em>;
        }
        // Code: `code`
        const codeParts = iPart.split(/(`[\s\S]*?`)/g);
        return codeParts.map((cPart, cIdx) => {
          if (cPart.startsWith('`') && cPart.endsWith('`')) {
            return (
              <code
                key={cIdx}
                className="px-1 py-0.5 mx-0.5 rounded text-[12px] font-mono bg-black/10 dark:bg-white/10"
              >
                {cPart.slice(1, -1)}
              </code>
            );
          }
          return cPart;
        });
      });
    });
  };

  return (
    <div className="text-[14px] sm:text-[15px] whitespace-pre-wrap break-words leading-relaxed select-text">
      {renderFormattedText(text)}
    </div>
  );
};
