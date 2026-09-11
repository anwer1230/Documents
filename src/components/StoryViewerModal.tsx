import React, { useState, useEffect, useRef } from 'react';
import { X, ChevronLeft, ChevronRight, Heart, Flame, ThumbsUp, PartyPopper, Smile, Share2, Eye } from 'lucide-react';
import { TelegramPeerStories, TelegramStoryItem } from '../types';

interface StoryViewerModalProps {
  isOpen: boolean;
  onClose: () => void;
  peerStoriesList: TelegramPeerStories[];
  initialPeerId?: string;
  onReact?: (peerId: string, storyId: string, emoji: string) => void;
  onRead?: (peerId: string, storyId: string) => void;
  isDark?: boolean;
}

const STORY_DURATION = 5000; // 5 seconds per story

export const StoryViewerModal: React.FC<StoryViewerModalProps> = ({
  isOpen,
  onClose,
  peerStoriesList,
  initialPeerId,
  onReact,
  onRead,
}) => {
  const [currentPeerIndex, setCurrentPeerIndex] = useState(0);
  const [currentStoryIndex, setCurrentStoryIndex] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  const [progress, setProgress] = useState(0);
  const [selectedEmoji, setSelectedEmoji] = useState<string | null>(null);
  const [reactionAnim, setReactionAnim] = useState<string | null>(null);

  const timerRef = useRef<number | null>(null);
  const lastTimeRef = useRef<number>(Date.now());

  // Initialize peer index from initialPeerId
  useEffect(() => {
    if (isOpen && initialPeerId && peerStoriesList.length > 0) {
      const idx = peerStoriesList.findIndex((p) => p.peerId === initialPeerId);
      if (idx >= 0) {
        setCurrentPeerIndex(idx);
        setCurrentStoryIndex(0);
      }
    }
  }, [isOpen, initialPeerId, peerStoriesList]);

  const currentPeer = peerStoriesList[currentPeerIndex];
  const currentStories = currentPeer?.stories || [];
  const currentStory: TelegramStoryItem | undefined = currentStories[currentStoryIndex];

  // Mark as read when viewing story
  useEffect(() => {
    if (isOpen && currentPeer && currentStory) {
      onRead?.(currentPeer.peerId, currentStory.id);
      setProgress(0);
      lastTimeRef.current = Date.now();
    }
  }, [isOpen, currentPeerIndex, currentStoryIndex, currentPeer?.peerId, currentStory?.id]);

  // Timer loop for story progress
  useEffect(() => {
    if (!isOpen || isPaused || !currentStory) return;

    const interval = setInterval(() => {
      const now = Date.now();
      const delta = now - lastTimeRef.current;
      lastTimeRef.current = now;

      setProgress((prev) => {
        const next = prev + (delta / STORY_DURATION) * 100;
        if (next >= 100) {
          handleNextStory();
          return 0;
        }
        return next;
      });
    }, 50);

    return () => clearInterval(interval);
  }, [isOpen, isPaused, currentStoryIndex, currentPeerIndex, currentStories.length]);

  const handleNextStory = () => {
    if (currentStoryIndex < currentStories.length - 1) {
      setCurrentStoryIndex((prev) => prev + 1);
      setProgress(0);
      lastTimeRef.current = Date.now();
    } else if (currentPeerIndex < peerStoriesList.length - 1) {
      // Move to next user's stories
      setCurrentPeerIndex((prev) => prev + 1);
      setCurrentStoryIndex(0);
      setProgress(0);
      lastTimeRef.current = Date.now();
    } else {
      onClose();
    }
  };

  const handlePrevStory = () => {
    if (currentStoryIndex > 0) {
      setCurrentStoryIndex((prev) => prev - 1);
      setProgress(0);
      lastTimeRef.current = Date.now();
    } else if (currentPeerIndex > 0) {
      // Move to previous user's stories
      setCurrentPeerIndex((prev) => prev - 1);
      const prevPeerStories = peerStoriesList[currentPeerIndex - 1]?.stories || [];
      setCurrentStoryIndex(Math.max(0, prevPeerStories.length - 1));
      setProgress(0);
      lastTimeRef.current = Date.now();
    }
  };

  // Wheel handling standard deltaY as fixed in tweb (claude/story-viewer-not-opening-owlr57)
  const handleWheel = (e: React.WheelEvent) => {
    if (Math.abs(e.deltaY) > 20) {
      if (e.deltaY > 0) {
        handleNextStory();
      } else {
        handlePrevStory();
      }
    }
  };

  // Keyboard navigation
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'ArrowRight' || e.key === ' ') {
        handleNextStory();
      } else if (e.key === 'ArrowLeft') {
        handlePrevStory();
      } else if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, currentStoryIndex, currentPeerIndex, currentStories.length]);

  const handleSendReaction = (emoji: string) => {
    if (!currentPeer || !currentStory) return;
    setSelectedEmoji(emoji);
    setReactionAnim(emoji);
    setTimeout(() => setReactionAnim(null), 1200);
    onReact?.(currentPeer.peerId, currentStory.id, emoji);
  };

  if (!isOpen || !currentPeer || !currentStory) return null;

  const quickReactions = ['❤️', '🔥', '👍', '🎉', '😍', '👏'];

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/95 select-none backdrop-blur-md animate-fade-in"
      onWheel={handleWheel}
    >
      {/* Reaction Flying Animation */}
      {reactionAnim && (
        <div className="absolute inset-0 pointer-events-none z-50 flex items-center justify-center">
          <div className="text-8xl animate-bounce transform scale-150 transition-all duration-700">
            {reactionAnim}
          </div>
        </div>
      )}

      {/* Main Story Container */}
      <div
        className="relative w-full max-w-md h-[90vh] max-h-[820px] bg-neutral-900 rounded-2xl overflow-hidden shadow-2xl flex flex-col justify-between"
        onMouseDown={() => setIsPaused(true)}
        onMouseUp={() => setIsPaused(false)}
        onTouchStart={() => setIsPaused(true)}
        onTouchEnd={() => setIsPaused(false)}
      >
        {/* Progress Segments */}
        <div className="absolute top-3 inset-x-3 z-30 flex gap-1.5 pointer-events-none">
          {currentStories.map((st, idx) => {
            let fillPercent = 0;
            if (idx < currentStoryIndex) fillPercent = 100;
            else if (idx === currentStoryIndex) fillPercent = progress;

            return (
              <div
                key={st.id || idx}
                className="h-1 flex-1 bg-white/30 rounded-full overflow-hidden"
              >
                <div
                  className="h-full bg-white transition-all duration-75 ease-linear rounded-full"
                  style={{ width: `${fillPercent}%` }}
                />
              </div>
            );
          })}
        </div>

        {/* Top Header */}
        <div className="absolute top-7 inset-x-4 z-30 flex items-center justify-between text-white pointer-events-auto">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-full bg-gradient-to-tr from-blue-500 to-indigo-600 flex items-center justify-center text-white font-bold text-sm shadow-md border-2 border-white/40">
              {currentPeer.peerId.slice(0, 2).toUpperCase()}
            </div>
            <div>
              <div className="text-sm font-semibold leading-tight drop-shadow-sm">
                {currentPeer.peerId === 'me' ? 'قصتي' : `مستخدم ${currentPeer.peerId}`}
              </div>
              <div className="text-[11px] text-white/70">
                {new Date(currentStory.date).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={onClose}
              className="p-1.5 rounded-full bg-black/40 hover:bg-black/60 text-white transition"
              title="إغلاق"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Tap areas for left / right navigation */}
        <div className="absolute inset-y-16 inset-x-0 z-20 flex">
          <div
            className="w-1/3 h-full cursor-pointer"
            onClick={handlePrevStory}
            title="السابق"
          />
          <div
            className="w-2/3 h-full cursor-pointer"
            onClick={handleNextStory}
            title="التالي"
          />
        </div>

        {/* Story Visual Media */}
        <div className="relative w-full h-full flex items-center justify-center bg-neutral-950">
          {currentStory.mediaUrl ? (
            currentStory.mediaType === 'video' ? (
              <video
                src={currentStory.mediaUrl}
                autoPlay
                playsInline
                className="w-full h-full object-cover"
              />
            ) : (
              <img
                src={currentStory.mediaUrl}
                alt="Story"
                referrerPolicy="no-referrer"
                className="w-full h-full object-cover"
              />
            )
          ) : (
            <div className="flex flex-col items-center justify-center p-8 text-center text-white bg-gradient-to-br from-blue-900/60 via-purple-900/50 to-neutral-900 w-full h-full">
              <div className="text-5xl mb-4">✨</div>
              <p className="text-lg font-medium text-white/90 max-w-xs">
                {currentStory.caption || 'قصة جديدة من تيليجرام'}
              </p>
            </div>
          )}

          {/* Story Caption Overlay */}
          {currentStory.caption && (
            <div className="absolute bottom-20 inset-x-4 z-30 p-3 rounded-xl bg-black/60 backdrop-blur-md text-white text-sm text-center">
              {currentStory.caption}
            </div>
          )}
        </div>

        {/* Bottom Reaction Bar */}
        <div className="absolute bottom-4 inset-x-4 z-30 flex items-center justify-between gap-2 p-2 rounded-2xl bg-black/50 backdrop-blur-md border border-white/10 text-white pointer-events-auto">
          <div className="flex items-center gap-1.5 overflow-x-auto py-0.5">
            {quickReactions.map((emoji) => (
              <button
                key={emoji}
                onClick={() => handleSendReaction(emoji)}
                className={`p-1.5 text-lg rounded-full hover:bg-white/20 transition transform hover:scale-125 active:scale-95 ${
                  selectedEmoji === emoji ? 'bg-blue-600/60 scale-110' : ''
                }`}
                title={`تفاعل ${emoji}`}
              >
                {emoji}
              </button>
            ))}
          </div>

          {currentStory.reactionsCount !== undefined && currentStory.reactionsCount > 0 && (
            <div className="flex items-center gap-1 text-xs text-white/80 pr-2 shrink-0">
              <Heart className="w-3.5 h-3.5 text-red-500 fill-red-500" />
              <span>{currentStory.reactionsCount}</span>
            </div>
          )}
        </div>
      </div>

      {/* Prev / Next Outer Buttons (Desktop) */}
      {currentPeerIndex > 0 || currentStoryIndex > 0 ? (
        <button
          onClick={handlePrevStory}
          className="hidden md:flex absolute left-8 p-3 rounded-full bg-white/10 hover:bg-white/20 text-white transition backdrop-blur-sm z-30"
          title="السابق"
        >
          <ChevronLeft className="w-6 h-6" />
        </button>
      ) : null}

      {currentPeerIndex < peerStoriesList.length - 1 || currentStoryIndex < currentStories.length - 1 ? (
        <button
          onClick={handleNextStory}
          className="hidden md:flex absolute right-8 p-3 rounded-full bg-white/10 hover:bg-white/20 text-white transition backdrop-blur-sm z-30"
          title="التالي"
        >
          <ChevronRight className="w-6 h-6" />
        </button>
      ) : null}
    </div>
  );
};
