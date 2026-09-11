import React, { useState, useRef, useEffect } from 'react';
import { Play, Pause } from 'lucide-react';

interface VoiceWaveformPlayerProps {
  url?: string;
  duration?: number; // In seconds
  isOut?: boolean;
  isDark?: boolean;
}

// Deterministic pseudorandom waveform bar heights for natural look
const WAVEFORM_BARS = [
  30, 45, 60, 35, 70, 85, 40, 55, 90, 65, 40, 75, 95, 60, 45, 80,
  60, 35, 50, 80, 100, 70, 45, 60, 75, 40, 30, 50, 65, 40, 25
];

export const VoiceWaveformPlayer: React.FC<VoiceWaveformPlayerProps> = ({
  url,
  duration = 12,
  isOut = false,
  isDark = true,
}) => {
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [playbackRate, setPlaybackRate] = useState<number>(1);
  const [totalDuration, setTotalDuration] = useState(duration);
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const animationFrameRef = useRef<number | null>(null);

  // Initialize and clean up audio
  useEffect(() => {
    if (url) {
      const audio = new Audio(url);
      audioRef.current = audio;

      audio.onloadedmetadata = () => {
        if (audio.duration && !isNaN(audio.duration) && isFinite(audio.duration)) {
          setTotalDuration(Math.round(audio.duration));
        }
      };

      audio.onended = () => {
        setIsPlaying(false);
        setCurrentTime(0);
      };

      audio.onerror = () => {
        // Fallback gracefully to simulated playback
        console.warn('Audio URL not directly playable, using simulation');
      };

      return () => {
        audio.pause();
        audio.src = '';
      };
    }
  }, [url]);

  // Handle Play/Pause
  const togglePlay = () => {
    if (audioRef.current && url) {
      if (isPlaying) {
        audioRef.current.pause();
        setIsPlaying(false);
      } else {
        audioRef.current.playbackRate = playbackRate;
        audioRef.current.play().then(() => {
          setIsPlaying(true);
        }).catch(() => {
          // If browser blocks audio, start simulation
          setIsPlaying(true);
        });
      }
    } else {
      // Simulated playback
      setIsPlaying(!isPlaying);
    }
  };

  // Update progress
  useEffect(() => {
    if (!isPlaying) {
      if (animationFrameRef.current) cancelAnimationFrame(animationFrameRef.current);
      return;
    }

    const interval = setInterval(() => {
      if (audioRef.current && url && !isNaN(audioRef.current.currentTime)) {
        setCurrentTime(audioRef.current.currentTime);
      } else {
        setCurrentTime((prev) => {
          if (prev >= totalDuration) {
            setIsPlaying(false);
            return 0;
          }
          return prev + 0.2 * playbackRate;
        });
      }
    }, 100);

    return () => clearInterval(interval);
  }, [isPlaying, totalDuration, playbackRate, url]);

  // Speed multiplier cycle: 1x -> 1.5x -> 2x -> 1x
  const cyclePlaybackRate = (e: React.MouseEvent) => {
    e.stopPropagation();
    const nextRate = playbackRate === 1 ? 1.5 : playbackRate === 1.5 ? 2 : 1;
    setPlaybackRate(nextRate);
    if (audioRef.current) {
      audioRef.current.playbackRate = nextRate;
    }
  };

  const progressRatio = Math.min(1, Math.max(0, currentTime / (totalDuration || 1)));

  const formatTime = (secs: number) => {
    const s = Math.floor(secs);
    const m = Math.floor(s / 60);
    const remainder = s % 60;
    return `${m}:${remainder < 10 ? '0' : ''}${remainder}`;
  };

  return (
    <div className="flex items-center gap-3 py-1 px-2 select-none">
      {/* Play/Pause Button */}
      <button
        type="button"
        onClick={togglePlay}
        className={`w-10 h-10 rounded-full flex items-center justify-center shrink-0 shadow-sm transition-transform active:scale-95 ${
          isOut
            ? 'bg-white text-[#2b5278] hover:bg-gray-100'
            : 'bg-[#3390ec] text-white hover:bg-[#2881da]'
        }`}
        title={isPlaying ? 'Pause' : 'Play'}
      >
        {isPlaying ? (
          <Pause className="w-5 h-5 fill-current" />
        ) : (
          <Play className="w-5 h-5 fill-current ml-0.5" />
        )}
      </button>

      {/* Waveform and Timer */}
      <div className="flex-1 min-w-[150px] space-y-1.5">
        {/* Waveform Bars */}
        <div
          className="flex items-center gap-1 h-7 cursor-pointer"
          onClick={(e) => {
            const rect = e.currentTarget.getBoundingClientRect();
            const clickRatio = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
            const newTime = clickRatio * totalDuration;
            setCurrentTime(newTime);
            if (audioRef.current) {
              audioRef.current.currentTime = newTime;
            }
          }}
        >
          {WAVEFORM_BARS.map((heightPercent, idx) => {
            const barRatio = idx / WAVEFORM_BARS.length;
            const isPlayed = barRatio <= progressRatio;

            return (
              <div
                key={idx}
                className="w-1 rounded-full transition-all duration-100"
                style={{
                  height: `${heightPercent}%`,
                  backgroundColor: isPlayed
                    ? isOut
                      ? '#ffffff'
                      : '#3390ec'
                    : isOut
                    ? 'rgba(255, 255, 255, 0.4)'
                    : isDark
                    ? '#455768'
                    : '#c4d1dc',
                }}
              />
            );
          })}
        </div>

        {/* Time and Speed Pill */}
        <div className="flex items-center justify-between text-[11px]">
          <span className={isOut ? 'text-white/80 font-mono' : isDark ? 'text-gray-300 font-mono' : 'text-gray-600 font-mono'}>
            {isPlaying ? formatTime(currentTime) : formatTime(totalDuration)}
          </span>

          <button
            type="button"
            onClick={cyclePlaybackRate}
            className={`px-1.5 py-0.5 rounded text-[10px] font-bold uppercase transition-colors ${
              isOut
                ? 'bg-white/20 text-white hover:bg-white/30'
                : isDark
                ? 'bg-gray-700/60 text-gray-200 hover:bg-gray-700'
                : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
            }`}
            title="Toggle playback speed"
          >
            {playbackRate}X
          </button>
        </div>
      </div>
    </div>
  );
};
