import React, { useState, useRef, useEffect } from 'react';
import {
  X,
  Play,
  Pause,
  Maximize2,
  Minimize2,
  Volume2,
  VolumeX,
  RotateCcw,
  FastForward,
  Sparkles,
  Radio,
} from 'lucide-react';
import { PiPVideoTrack } from '../../types';
import { useTelegram } from '../../context/TelegramContext';

interface FloatingPiPPlayerProps {
  track?: PiPVideoTrack | null;
  onClose?: () => void;
  onExpand?: (track: PiPVideoTrack) => void;
}

export const FloatingPiPPlayer: React.FC<FloatingPiPPlayerProps> = ({
  track: propTrack,
  onClose: propOnClose,
  onExpand,
}) => {
  const { pipVideoTrack, setPipVideoTrack, setViewerMedia, settings } = useTelegram();
  const track = propTrack !== undefined ? propTrack : pipVideoTrack;
  const onClose = propOnClose || (() => setPipVideoTrack(null));
  const [isPlaying, setIsPlaying] = useState(true);
  const [isMuted, setIsMuted] = useState(false);
  const [playbackSpeed, setPlaybackSpeed] = useState<number>(1);
  const [progress, setProgress] = useState(0);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(track?.duration || 30);
  const [isMinimized, setIsMinimized] = useState(false);
  const [position, setPosition] = useState<{ x: number; y: number }>({
    x: window.innerWidth - 280,
    y: 80,
  });

  const videoRef = useRef<HTMLVideoElement>(null);
  const isDraggingRef = useRef(false);
  const dragOffsetRef = useRef({ x: 0, y: 0 });

  const isRtl = settings.language === 'ar';

  useEffect(() => {
    if (track) {
      setIsPlaying(true);
      setProgress(0);
      setCurrentTime(0);
    }
  }, [track?.url]);

  if (!track) return null;

  const handleMouseDown = (e: React.MouseEvent) => {
    if ((e.target as HTMLElement).closest('button') || (e.target as HTMLElement).closest('input')) {
      return;
    }
    isDraggingRef.current = true;
    dragOffsetRef.current = {
      x: e.clientX - position.x,
      y: e.clientY - position.y,
    };

    const handleMouseMove = (moveEvent: MouseEvent) => {
      if (!isDraggingRef.current) return;
      const newX = Math.max(10, Math.min(window.innerWidth - (isMinimized ? 160 : 260), moveEvent.clientX - dragOffsetRef.current.x));
      const newY = Math.max(10, Math.min(window.innerHeight - (isMinimized ? 60 : 200), moveEvent.clientY - dragOffsetRef.current.y));
      setPosition({ x: newX, y: newY });
    };

    const handleMouseUp = () => {
      isDraggingRef.current = false;
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };

    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);
  };

  const togglePlay = () => {
    if (videoRef.current) {
      if (isPlaying) {
        videoRef.current.pause();
      } else {
        videoRef.current.play();
      }
      setIsPlaying(!isPlaying);
    } else {
      setIsPlaying(!isPlaying);
    }
  };

  const cycleSpeed = () => {
    const speeds = [1, 1.25, 1.5, 2, 0.5];
    const currentIndex = speeds.indexOf(playbackSpeed);
    const nextSpeed = speeds[(currentIndex + 1) % speeds.length];
    setPlaybackSpeed(nextSpeed);
    if (videoRef.current) {
      videoRef.current.playbackRate = nextSpeed;
    }
  };

  const handleTimeUpdate = () => {
    if (videoRef.current) {
      setCurrentTime(videoRef.current.currentTime);
      setDuration(videoRef.current.duration || duration);
      setProgress((videoRef.current.currentTime / (videoRef.current.duration || 1)) * 100);
    }
  };

  const formatTime = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = Math.floor(secs % 60);
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  };

  return (
    <div
      id="tg-pip-floating-player"
      onMouseDown={handleMouseDown}
      style={{
        left: `${position.x}px`,
        top: `${position.y}px`,
        touchAction: 'none',
      }}
      className={`fixed z-50 select-none shadow-2xl rounded-3xl overflow-hidden border border-white/20 backdrop-blur-xl transition-all duration-150 ${
        isMinimized ? 'w-44 h-16 bg-[#17212b]/95' : 'w-64 sm:w-72 bg-[#0e1621]/95'
      }`}
    >
      {/* Video Content / Circular Telescope Mode */}
      {track.isRoundVideoNote ? (
        <div className="relative p-2 flex items-center justify-center bg-black/60">
          <div className="w-28 h-28 rounded-full overflow-hidden border-2 border-sky-400 shadow-inner relative bg-black">
            {track.url ? (
              <video
                ref={videoRef}
                src={track.url}
                autoPlay={isPlaying}
                loop
                muted={isMuted}
                onTimeUpdate={handleTimeUpdate}
                className="w-full h-full object-cover"
              />
            ) : (
              <div className="w-full h-full flex items-center justify-center bg-gradient-to-tr from-sky-600 to-indigo-600 text-white font-bold text-lg">
                <Radio className="w-8 h-8 animate-pulse text-white" />
              </div>
            )}
          </div>

          <div className="absolute top-2 right-2 flex items-center gap-1">
            <button
              onClick={onClose}
              className="p-1 rounded-full bg-black/60 text-white hover:bg-rose-500 transition-colors"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      ) : (
        <div className="relative bg-black h-36 flex items-center justify-center group">
          {track.url ? (
            <video
              ref={videoRef}
              src={track.url}
              autoPlay={isPlaying}
              loop
              muted={isMuted}
              onTimeUpdate={handleTimeUpdate}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full bg-slate-900 flex flex-col items-center justify-center text-gray-400">
              <Sparkles className="w-8 h-8 text-sky-400 animate-pulse mb-1" />
              <span className="text-xs text-gray-300 font-medium">Telegram X PiP</span>
            </div>
          )}

          {/* Quick Floating Overlays */}
          <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
            <button
              onClick={togglePlay}
              className="p-2 rounded-full bg-white/20 hover:bg-white/40 text-white backdrop-blur-sm transition-transform active:scale-95"
            >
              {isPlaying ? <Pause className="w-5 h-5" /> : <Play className="w-5 h-5 ml-0.5" />}
            </button>
          </div>

          {/* Top Bar Controls */}
          <div className="absolute top-1.5 right-1.5 flex items-center gap-1">
            <button
              onClick={() => onExpand?.(track)}
              className="p-1 rounded-full bg-black/50 text-white hover:bg-sky-500 transition-colors"
              title={isRtl ? 'تكبير للشاشة الكاملة' : 'Fullscreen'}
            >
              <Maximize2 className="w-3.5 h-3.5" />
            </button>
            <button
              onClick={onClose}
              className="p-1 rounded-full bg-black/50 text-white hover:bg-rose-500 transition-colors"
              title={isRtl ? 'إغلاق' : 'Close'}
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>

          {/* Speed Badge */}
          <div className="absolute bottom-1.5 left-1.5">
            <button
              onClick={cycleSpeed}
              className="px-1.5 py-0.5 rounded-full bg-black/60 text-sky-400 hover:bg-black/80 font-mono text-[10px] font-bold border border-sky-400/30"
              title={isRtl ? 'تغيير سرعة التشغيل' : 'Playback Speed'}
            >
              {playbackSpeed}x
            </button>
          </div>
        </div>
      )}

      {/* Progress Bar & Mini Controls */}
      <div className="p-2 bg-[#17212b] flex flex-col gap-1.5">
        <div className="flex items-center justify-between text-[11px] text-gray-300 font-medium">
          <span className="truncate max-w-[140px] text-white font-bold">
            {track.title || (isRtl ? 'فيديو تيليجرام' : 'Telegram Video')}
          </span>
          <span className="font-mono text-[10px] text-gray-400">
            {formatTime(currentTime)} / {formatTime(duration)}
          </span>
        </div>

        <div className="w-full bg-white/10 h-1 rounded-full overflow-hidden cursor-pointer relative">
          <div
            className="bg-[#2481cc] h-full rounded-full transition-all duration-75"
            style={{ width: `${progress}%` }}
          />
        </div>

        <div className="flex items-center justify-between text-xs pt-0.5 text-gray-300">
          <button
            onClick={() => setIsMuted(!isMuted)}
            className="p-1 hover:text-white transition-colors"
          >
            {isMuted ? <VolumeX className="w-4 h-4 text-rose-400" /> : <Volume2 className="w-4 h-4" />}
          </button>

          <button
            onClick={togglePlay}
            className="p-1 text-white hover:text-sky-400 transition-colors"
          >
            {isPlaying ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4" />}
          </button>

          <button
            onClick={cycleSpeed}
            className="font-mono text-[10px] px-1.5 py-0.5 rounded bg-white/10 text-sky-400 hover:bg-white/20"
          >
            {playbackSpeed}x
          </button>
        </div>
      </div>
    </div>
  );
};
