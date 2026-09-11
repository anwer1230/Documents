import React, { useRef } from 'react';
import { Plus } from 'lucide-react';
import { TelegramPeerStories } from '../types';

interface StoriesListBarProps {
  peerStoriesList: TelegramPeerStories[];
  onOpenStory: (peerId: string) => void;
  isDark?: boolean;
  lang?: 'ar' | 'en';
}

export const StoriesListBar: React.FC<StoriesListBarProps> = ({
  peerStoriesList,
  onOpenStory,
  isDark = false,
  lang = 'ar',
}) => {
  const scrollRef = useRef<HTMLDivElement | null>(null);

  // Wheel handling with standard deltaY as fixed in claude/story-viewer-not-opening-owlr57
  const handleWheel = (e: React.WheelEvent) => {
    if (scrollRef.current) {
      scrollRef.current.scrollLeft += e.deltaY;
    }
  };

  if (!peerStoriesList || peerStoriesList.length === 0) {
    return null;
  }

  return (
    <div
      ref={scrollRef}
      onWheel={handleWheel}
      className={`flex items-center gap-3 px-3 py-2 overflow-x-auto no-scrollbar select-none border-b transition-colors ${
        isDark ? 'border-neutral-800 bg-neutral-900/50' : 'border-neutral-100 bg-neutral-50/50'
      }`}
    >
      {peerStoriesList.map((item) => {
        const hasUnread = item.stories.some((s) => !s.isViewed);
        const title = item.peerId === 'me' ? (lang === 'ar' ? 'قصتك' : 'Your Story') : item.peerId;

        return (
          <button
            key={item.peerId}
            onClick={() => onOpenStory(item.peerId)}
            className="flex flex-col items-center gap-1 shrink-0 focus:outline-none group cursor-pointer"
            title={title}
          >
            <div
              className={`p-[2px] rounded-full transition-transform group-hover:scale-105 active:scale-95 ${
                hasUnread
                  ? 'bg-gradient-to-tr from-cyan-500 via-blue-500 to-indigo-600 shadow-sm'
                  : isDark
                  ? 'bg-neutral-700'
                  : 'bg-neutral-300'
              }`}
            >
              <div
                className={`w-12 h-12 rounded-full flex items-center justify-center font-bold text-sm text-white ${
                  isDark ? 'border-2 border-neutral-900' : 'border-2 border-white'
                } bg-gradient-to-br from-indigo-500 to-purple-600 shadow-inner`}
              >
                {item.peerId.slice(0, 2).toUpperCase()}
              </div>
            </div>
            <span
              className={`text-[11px] max-w-[56px] truncate leading-tight ${
                hasUnread
                  ? isDark
                    ? 'text-white font-medium'
                    : 'text-neutral-900 font-medium'
                  : 'text-neutral-500'
              }`}
            >
              {title}
            </span>
          </button>
        );
      })}
    </div>
  );
};
