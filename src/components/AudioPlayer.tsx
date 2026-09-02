import React, { useState, useEffect, useRef } from "react";
import { Play, Pause, Volume2 } from "lucide-react";

interface AudioPlayerProps {
  url?: string;
  duration?: number;
  isOut?: boolean;
}

export const AudioPlayer: React.FC<AudioPlayerProps> = ({ duration = 12, isOut = false }) => {
  const [isPlaying, setIsPlaying] = useState(false);
  const [progress, setProgress] = useState(0);
  const [playbackSpeed, setPlaybackSpeed] = useState<1 | 1.5 | 2>(1);
  const intervalRef = useRef<any>(null);

  useEffect(() => {
    if (isPlaying) {
      intervalRef.current = setInterval(() => {
        setProgress((prev) => {
          if (prev >= 100) {
            setIsPlaying(false);
            return 0;
          }
          return prev + (100 / (duration * 10)) * playbackSpeed;
        });
      }, 100);
    } else {
      if (intervalRef.current) clearInterval(intervalRef.current);
    }
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [isPlaying, duration, playbackSpeed]);

  const togglePlay = () => {
    setIsPlaying(!isPlaying);
  };

  const cycleSpeed = () => {
    if (playbackSpeed === 1) setPlaybackSpeed(1.5);
    else if (playbackSpeed === 1.5) setPlaybackSpeed(2);
    else setPlaybackSpeed(1);
  };

  const currentSeconds = Math.floor((progress / 100) * duration);
  const minutes = Math.floor(currentSeconds / 60);
  const seconds = currentSeconds % 60;
  const timeFormatted = `${minutes}:${seconds < 10 ? "0" : ""}${seconds}`;

  // Fake wave heights for authentic Telegram voice visualization
  const waveBars = [
    25, 45, 70, 95, 60, 35, 80, 100, 75, 50, 30, 65, 85, 90, 60, 40, 75, 95, 55, 30, 60, 80, 50, 35, 70, 90, 45, 20
  ];

  return (
    <div id="telegram-voice-player" className="flex items-center gap-3 py-1 min-w-[220px] max-w-[280px]">
      <button
        id="voice-player-toggle"
        onClick={togglePlay}
        className={`w-10 h-10 rounded-full flex items-center justify-center transition-transform active:scale-95 shadow-sm ${
          isOut
            ? "bg-blue-600 hover:bg-blue-700 text-white"
            : "bg-blue-500 hover:bg-blue-600 text-white dark:bg-blue-600 dark:hover:bg-blue-500"
        }`}
      >
        {isPlaying ? <Pause className="w-4 h-4 fill-current" /> : <Play className="w-4 h-4 fill-current ml-0.5" />}
      </button>

      <div className="flex-1 flex flex-col justify-center">
        {/* Waveform Bars */}
        <div
          className="flex items-center gap-[2px] h-6 cursor-pointer"
          onClick={(e) => {
            const rect = e.currentTarget.getBoundingClientRect();
            const clickX = e.clientX - rect.left;
            const newProg = Math.max(0, Math.min(100, (clickX / rect.width) * 100));
            setProgress(newProg);
          }}
        >
          {waveBars.map((height, idx) => {
            const barProgress = (idx / waveBars.length) * 100;
            const isFilled = barProgress <= progress;
            return (
              <div
                key={idx}
                className="w-[3px] rounded-full transition-colors"
                style={{
                  height: `${Math.max(4, (height / 100) * 22)}px`,
                  backgroundColor: isFilled
                    ? isOut
                      ? "#1d4ed8"
                      : "#2563eb"
                    : isOut
                    ? "rgba(37, 99, 235, 0.25)"
                    : "rgba(148, 163, 184, 0.5)",
                }}
              />
            );
          })}
        </div>

        {/* Time and Speed */}
        <div className="flex items-center justify-between text-[11px] font-medium text-slate-500 dark:text-slate-400 mt-0.5">
          <span>{isPlaying ? timeFormatted : `${Math.floor(duration / 60)}:${duration % 60 < 10 ? "0" : ""}${duration % 60}`}</span>
          <button
            id="voice-player-speed-btn"
            onClick={cycleSpeed}
            className="px-1 py-0.5 rounded bg-slate-200/60 dark:bg-slate-700/60 text-[10px] hover:bg-slate-300 dark:hover:bg-slate-600 transition-colors"
          >
            {playbackSpeed}x
          </button>
        </div>
      </div>
    </div>
  );
};
