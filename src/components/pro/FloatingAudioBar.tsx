import React, { useState, useEffect } from "react";
import { Play, Pause, X, FastForward, Volume2, Music, Mic } from "lucide-react";
import { ActiveAudioTrack } from "../../types";

interface FloatingAudioBarProps {
  track: ActiveAudioTrack | null;
  onClose: () => void;
}

export const FloatingAudioBar: React.FC<FloatingAudioBarProps> = ({ track, onClose }) => {
  const [isPlaying, setIsPlaying] = useState(true);
  const [progress, setProgress] = useState(0);
  const [speed, setSpeed] = useState<1 | 1.5 | 2>(1);

  useEffect(() => {
    if (!track) return;
    setIsPlaying(true);
    setProgress(0);

    const interval = setInterval(() => {
      setProgress((prev) => {
        if (prev >= 100) {
          setIsPlaying(false);
          return 100;
        }
        return prev + 2 * speed;
      });
    }, 500);

    return () => clearInterval(interval);
  }, [track, speed]);

  if (!track) return null;

  const toggleSpeed = () => {
    if (speed === 1) setSpeed(1.5);
    else if (speed === 1.5) setSpeed(2);
    else setSpeed(1);
  };

  return (
    <div
      id="telegram-floating-audio-bar"
      className="w-full bg-white/95 dark:bg-slate-900/95 backdrop-blur-md border-b border-slate-200 dark:border-slate-800 px-4 py-2 flex items-center justify-between shadow-md z-30 transition-all text-xs"
      dir="rtl"
    >
      {/* Track Info */}
      <div className="flex items-center gap-3 min-w-0">
        <button
          onClick={() => setIsPlaying(!isPlaying)}
          className="w-8 h-8 rounded-full bg-sky-500 hover:bg-sky-600 text-white flex items-center justify-center shadow-sm shrink-0 transition-transform active:scale-95"
        >
          {isPlaying ? (
            <Pause className="w-4 h-4 fill-current" />
          ) : (
            <Play className="w-4 h-4 fill-current ml-0.5" />
          )}
        </button>

        <div className="min-w-0">
          <div className="flex items-center gap-1.5 font-bold text-slate-800 dark:text-white truncate">
            <Mic className="w-3.5 h-3.5 text-sky-500 shrink-0" />
            <span className="truncate">{track.title}</span>
          </div>
          <span className="text-[10px] text-slate-400 truncate block">
            {track.subtitle}
          </span>
        </div>
      </div>

      {/* Progress Bar & Visualizer */}
      <div className="flex-1 mx-4 max-w-md hidden sm:flex items-center gap-2">
        <div className="w-full h-1.5 bg-slate-200 dark:bg-slate-700 rounded-full overflow-hidden relative cursor-pointer">
          <div
            className="h-full bg-sky-500 rounded-full transition-all duration-300"
            style={{ width: `${progress}%` }}
          />
        </div>
        <span className="text-[10px] text-slate-400 font-mono">
          {Math.floor((progress / 100) * track.duration)}s
        </span>
      </div>

      {/* Controls: Speed + Close */}
      <div className="flex items-center gap-2 shrink-0">
        <button
          onClick={toggleSpeed}
          className="px-2 py-0.5 rounded-md bg-slate-100 dark:bg-slate-800 text-sky-600 dark:text-sky-400 font-bold text-[11px] hover:bg-sky-50 transition-colors"
          title="تغيير سرعة التشغيل"
        >
          {speed}x
        </button>

        <button
          onClick={onClose}
          className="w-7 h-7 rounded-full text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 flex items-center justify-center hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          title="إغلاق المشغل"
        >
          <X className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};
