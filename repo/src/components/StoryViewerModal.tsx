import React, { useState, useEffect } from 'react';
import { X, ChevronLeft, ChevronRight, Heart, Send } from 'lucide-react';
import { TelegramPeerStories } from '../types';
import { getSenderColor, getInitials } from '../utils/telegramColors';

interface StoryViewerModalProps {
  isOpen: boolean;
  onClose: () => void;
  peerStoriesList: TelegramPeerStories[];
  initialPeerId?: string;
  onReact?: (peerId: string, storyId: string, emoji: string) => void;
  onRead?: (peerId: string, storyId: string) => void;
  isDark?: boolean;
  lang?: 'ar' | 'en';
}

export const StoryViewerModal: React.FC<StoryViewerModalProps> = ({
  isOpen,
  onClose,
  peerStoriesList,
  initialPeerId,
  onReact,
  onRead,
  isDark = true,
  lang = 'ar',
}) => {
  const [currentPeerIndex, setCurrentPeerIndex] = useState(0);
  const [currentStoryIndex, setCurrentStoryIndex] = useState(0);
  const isAr = lang === 'ar';

  useEffect(() => {
    if (initialPeerId && peerStoriesList.length > 0) {
      const idx = peerStoriesList.findIndex((p) => p.peerId === initialPeerId);
      if (idx !== -1) {
        setCurrentPeerIndex(idx);
        setCurrentStoryIndex(0);
      }
    }
  }, [initialPeerId, peerStoriesList]);

  if (!isOpen || peerStoriesList.length === 0) return null;

  const currentPeer = peerStoriesList[currentPeerIndex] || peerStoriesList[0];
  const stories = currentPeer?.stories || [];
  const currentStory = stories[currentStoryIndex] || stories[0];
  const peerName = (currentPeer as any).peerName || currentPeer.peerTitle || 'Story';

  // Mark as read
  useEffect(() => {
    if (currentPeer && currentStory && onRead) {
      onRead(currentPeer.peerId, currentStory.id);
    }
  }, [currentPeer, currentStory, onRead]);

  const handleNext = () => {
    if (currentStoryIndex < stories.length - 1) {
      setCurrentStoryIndex((prev) => prev + 1);
    } else if (currentPeerIndex < peerStoriesList.length - 1) {
      setCurrentPeerIndex((prev) => prev + 1);
      setCurrentStoryIndex(0);
    } else {
      onClose();
    }
  };

  const handlePrev = () => {
    if (currentStoryIndex > 0) {
      setCurrentStoryIndex((prev) => prev - 1);
    } else if (currentPeerIndex > 0) {
      setCurrentPeerIndex((prev) => prev - 1);
      const prevPeerStories = peerStoriesList[currentPeerIndex - 1]?.stories || [];
      setCurrentStoryIndex(Math.max(0, prevPeerStories.length - 1));
    }
  };

  const reactions = ['❤️', '🔥', '👏', '😍', '🎉', '🚀'];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/95 select-none animate-fade-in">
      {/* Story Stage Container */}
      <div className="relative w-full max-w-md h-[88vh] rounded-3xl overflow-hidden shadow-2xl flex flex-col bg-neutral-900 border border-neutral-800">
        {/* Top Segmented Progress Bars */}
        <div className="absolute top-3 inset-x-3 z-30 flex items-center gap-1.5">
          {stories.map((s, idx) => (
            <div key={s.id || idx} className="flex-1 h-1 rounded-full bg-white/30 overflow-hidden">
              <div
                className={`h-full bg-white transition-all duration-300 ${
                  idx < currentStoryIndex
                    ? 'w-full'
                    : idx === currentStoryIndex
                    ? 'w-full'
                    : 'w-0'
                }`}
              />
            </div>
          ))}
        </div>

        {/* Top Header info */}
        <div className="absolute top-7 inset-x-4 z-30 flex items-center justify-between text-white">
          <div className="flex items-center gap-2.5">
            <div
              style={{ backgroundColor: getSenderColor(currentPeer.peerId) }}
              className="w-10 h-10 rounded-full flex items-center justify-center font-bold text-xs text-white overflow-hidden shadow"
            >
              {currentPeer.peerAvatar ? (
                <img src={currentPeer.peerAvatar} alt="" className="w-full h-full object-cover" />
              ) : (
                getInitials(peerName)
              )}
            </div>
            <div>
              <h4 className="font-bold text-sm leading-tight">{peerName}</h4>
              <span className="text-[11px] text-white/70">
                {currentStory?.timestamp
                  ? new Date(currentStory.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
                  : 'منذ قليل'}
              </span>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-1.5 rounded-full bg-black/40 hover:bg-black/70 text-white transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Story Media */}
        <div className="flex-1 flex items-center justify-center relative bg-black">
          {currentStory?.mediaUrl ? (
            <img
              src={currentStory.mediaUrl}
              alt=""
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="p-8 text-center text-white">
              <p className="text-xl font-bold">{currentStory?.caption || 'Story text message'}</p>
            </div>
          )}

          {/* Click to Navigate Tap Targets */}
          <div
            onClick={handlePrev}
            className="absolute inset-y-0 start-0 w-1/3 cursor-pointer z-10"
          />
          <div
            onClick={handleNext}
            className="absolute inset-y-0 end-0 w-1/3 cursor-pointer z-10"
          />

          {/* Side Arrows */}
          <button
            onClick={handlePrev}
            className="absolute start-2 top-1/2 -translate-y-1/2 p-2 rounded-full bg-black/40 text-white hover:bg-black/60 z-20"
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
          <button
            onClick={handleNext}
            className="absolute end-2 top-1/2 -translate-y-1/2 p-2 rounded-full bg-black/40 text-white hover:bg-black/60 z-20"
          >
            <ChevronRight className="w-5 h-5" />
          </button>
        </div>

        {/* Caption & Bottom Reaction Bar */}
        <div className="p-4 bg-gradient-to-t from-black via-black/80 to-transparent z-20">
          {currentStory?.caption && (
            <p className="text-sm text-white font-medium mb-3 text-center px-2">
              {currentStory.caption}
            </p>
          )}

          <div className="flex items-center justify-center gap-2">
            {reactions.map((emoji) => (
              <button
                key={emoji}
                onClick={() =>
                  currentStory && onReact && onReact(currentPeer.peerId, currentStory.id, emoji)
                }
                className="w-10 h-10 rounded-full bg-white/10 hover:bg-white/20 active:scale-125 flex items-center justify-center text-lg transition"
              >
                {emoji}
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
