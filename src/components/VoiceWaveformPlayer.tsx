import React, { useState, useRef, useEffect } from 'react';
import { Play, Pause } from 'lucide-react';

interface VoiceWaveformPlayerProps {
  url?: string;
  duration?: number;
  isOut?: boolean;
  isDark?: boolean;
}

export const VoiceWaveformPlayer: React.FC<VoiceWaveformPlayerProps> = ({
  url,
  duration = 12,
  isOut = false,
  isDark = true,
}) => {
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const audioRef = useRef<HTMLAudioElement | null>(null);

  // Generate realistic fixed waveform bars based on duration
  const bars = [
    4, 8, 14, 22, 18, 12, 16, 24, 30, 20, 14, 18, 26, 22, 16, 12, 8, 14, 20, 28, 24, 16, 10, 6,
    12, 18, 22, 16, 12, 8, 4,
  ];

  useEffect(() => {
    if (!url) return;
    const audio = new Audio(url);
    audioRef.current = audio;

    audio.ontimeupdate = () => {
      setCurrentTime(audio.currentTime);
    };

    audio.onended = () => {
      setIsPlaying(false);
      setCurrentTime(0);
    };

    return () => {
      audio.pause();
      audio.src = '';
    };
  }, [url]);

  const togglePlay = () => {
    if (!audioRef.current) {
      // Simulate playback if no audio file url
      setIsPlaying(!isPlaying);
      return;
    }

    if (isPlaying) {
      audioRef.current.pause();
      setIsPlaying(false);
    } else {
      audioRef.current.play().catch(() => {
        // Fallback simulated playing
      });
      setIsPlaying(true);
    }
  };

  const formatTime = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = Math.floor(secs % 60);
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  };

  const progress = duration > 0 ? (currentTime / duration) : 0;

  return (
    <div className="flex items-center gap-3 py-1.5 min-w-[210px] select-none">
      <button
        onClick={togglePlay}
        className={`w-9 h-9 rounded-full flex items-center justify-center shrink-0 shadow transition active:scale-95 ${
          isOut
            ? 'bg-[#2b5278] hover:bg-[#224263] text-white'
            : 'bg-[#3390ec] hover:bg-[#2b7ec9] text-white'
        }`}
      >
        {isPlaying ? <Pause className="w-4 h-4 fill-white" /> : <Play className="w-4 h-4 fill-white ms-0.5" />}
      </button>

      <div className="flex-1 flex flex-col justify-center gap-1">
        {/* Waveform Visualization Bars */}
        <div className="flex items-center gap-[2.5px] h-6 cursor-pointer">
          {bars.map((height, i) => {
            const barProgress = i / bars.length;
            const isPlayed = barProgress <= progress;
            return (
              <div
                key={i}
                style={{ height: `${height}px` }}
                className={`w-[2.5px] rounded-full transition-colors ${
                  isPlayed
                    ? isOut
                      ? 'bg-white'
                      : 'bg-[#3390ec]'
                    : isOut
                    ? 'bg-white/40'
                    : isDark
                    ? 'bg-gray-600'
                    : 'bg-gray-300'
                }`}
              />
            );
          })}
        </div>

        {/* Duration / Progress counter */}
        <div className={`text-[10px] font-mono ${isOut ? 'text-white/80' : isDark ? 'text-gray-400' : 'text-gray-500'}`}>
          {isPlaying ? formatTime(currentTime) : formatTime(duration)}
        </div>
      </div>
    </div>
  );
};
