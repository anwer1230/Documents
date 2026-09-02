import React, { useState, useRef, useEffect } from 'react';
import {
  X,
  Send,
  Trash2,
  Camera,
  RefreshCw,
  Sparkles,
  Lock,
  Radio,
  StopCircle,
} from 'lucide-react';
import { useTelegram } from '../../context/TelegramContext';
import confetti from 'canvas-confetti';

interface RoundVideoRecorderProps {
  isOpen?: boolean;
  onClose: () => void;
  onSendVideoNote?: (mediaBlobUrl: string, durationSec: number) => void;
  onSendVideo?: (videoBlob: Blob, videoUrl: string, duration: number) => void;
}

export const RoundVideoRecorder: React.FC<RoundVideoRecorderProps> = ({
  isOpen = true,
  onClose,
  onSendVideoNote,
  onSendVideo,
}) => {
  const { settings, showToast } = useTelegram();
  const [isRecording, setIsRecording] = useState(false);
  const [seconds, setSeconds] = useState(0);
  const [facingMode, setFacingMode] = useState<'user' | 'environment'>('user');
  const [hasCameraPermission, setHasCameraPermission] = useState<boolean | null>(null);

  const videoRef = useRef<HTMLVideoElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const recordedChunksRef = useRef<Blob[]>([]);
  const timerRef = useRef<number | null>(null);

  const isRtl = settings.language === 'ar';

  useEffect(() => {
    if (isOpen) {
      startCamera();
    } else {
      stopCamera();
    }
    return () => {
      stopCamera();
    };
  }, [isOpen, facingMode]);

  const startCamera = async () => {
    try {
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((t) => t.stop());
      }

      const stream = await navigator.mediaDevices.getUserMedia({
        video: {
          facingMode,
          width: { ideal: 480 },
          height: { ideal: 480 },
          aspectRatio: 1,
        },
        audio: true,
      });

      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
      }
      setHasCameraPermission(true);
      startRecording();
    } catch (err) {
      console.warn('Camera access fallback (simulation mode active)', err);
      setHasCameraPermission(false);
      startRecording(); // fallback simulation recording
    }
  };

  const stopCamera = () => {
    if (timerRef.current) {
      window.clearInterval(timerRef.current);
      timerRef.current = null;
    }
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((t) => t.stop());
      streamRef.current = null;
    }
    if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
      mediaRecorderRef.current.stop();
    }
    setIsRecording(false);
    setSeconds(0);
  };

  const startRecording = () => {
    setIsRecording(true);
    setSeconds(0);
    recordedChunksRef.current = [];

    if (streamRef.current) {
      try {
        const recorder = new MediaRecorder(streamRef.current, {
          mimeType: 'video/webm;codecs=vp8,opus',
        });
        recorder.ondataavailable = (e) => {
          if (e.data.size > 0) {
            recordedChunksRef.current.push(e.data);
          }
        };
        recorder.start(250);
        mediaRecorderRef.current = recorder;
      } catch (e) {
        console.warn('MediaRecorder error', e);
      }
    }

    timerRef.current = window.setInterval(() => {
      setSeconds((prev) => {
        if (prev >= 59) {
          handleFinishAndSend();
          return 60;
        }
        return prev + 1;
      });
    }, 1000);
  };

  const handleFinishAndSend = () => {
    if (timerRef.current) {
      window.clearInterval(timerRef.current);
      timerRef.current = null;
    }

    const duration = Math.max(1, seconds);
    confetti({ particleCount: 30, spread: 50, origin: { y: 0.8 } });

    if (recordedChunksRef.current.length > 0) {
      const blob = new Blob(recordedChunksRef.current, { type: 'video/webm' });
      const blobUrl = URL.createObjectURL(blob);
      if (onSendVideo) {
        onSendVideo(blob, blobUrl, duration);
      }
      if (onSendVideoNote) {
        onSendVideoNote(blobUrl, duration);
      }
    } else {
      // High-quality Telegram video note placeholder
      const sampleVideoUrl = 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4';
      if (onSendVideo) {
        const dummyBlob = new Blob([], { type: 'video/mp4' });
        onSendVideo(dummyBlob, sampleVideoUrl, duration);
      }
      if (onSendVideoNote) {
        onSendVideoNote(sampleVideoUrl, duration);
      }
    }

    stopCamera();
    onClose();
    showToast(isRtl ? 'تم إرسال رسالة الفيديو المرئية' : 'Round Video Note Sent', '🎥');
  };

  const toggleCamera = () => {
    setFacingMode((prev) => (prev === 'user' ? 'environment' : 'user'));
  };

  if (!isOpen) return null;

  const progressPercent = (seconds / 60) * 100;

  return (
    <div
      id="tg-round-video-recorder-overlay"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-lg animate-in fade-in duration-150 select-none"
    >
      <div className="flex flex-col items-center gap-6 max-w-sm w-full">
        {/* Top Header info */}
        <div className="flex items-center justify-between w-full text-white px-2">
          <div className="flex items-center gap-2 text-xs font-bold text-sky-400">
            <Radio className="w-4 h-4 animate-pulse" />
            <span>{isRtl ? 'تسجيل رسالة مرئية دائرية' : 'Telegram Video Note (Telescope)'}</span>
          </div>

          <button
            onClick={() => {
              stopCamera();
              onClose();
            }}
            className="p-2 rounded-full bg-white/10 hover:bg-white/20 text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Circular Viewfinder with Animated Progress Ring */}
        <div className="relative w-64 h-64 sm:w-72 sm:h-72 flex items-center justify-center">
          {/* SVG Progress Ring */}
          <svg className="absolute inset-0 w-full h-full -rotate-90 pointer-events-none" viewBox="0 0 100 100">
            <circle
              cx="50"
              cy="50"
              r="47"
              fill="none"
              stroke="rgba(255,255,255,0.15)"
              strokeWidth="4"
            />
            <circle
              cx="50"
              cy="50"
              r="47"
              fill="none"
              stroke="#2481cc"
              strokeWidth="4"
              strokeDasharray="295.3"
              strokeDashoffset={295.3 - (295.3 * progressPercent) / 100}
              strokeLinecap="round"
              className="transition-all duration-300 ease-linear"
            />
          </svg>

          {/* Video Container inside circle */}
          <div className="w-[88%] h-[88%] rounded-full overflow-hidden bg-slate-900 border-2 border-white/20 shadow-2xl relative flex items-center justify-center">
            {hasCameraPermission !== false ? (
              <video
                ref={videoRef}
                autoPlay
                playsInline
                muted
                className="w-full h-full object-cover scale-x-[-1]"
              />
            ) : (
              <div className="w-full h-full bg-gradient-to-tr from-sky-900 to-indigo-950 flex flex-col items-center justify-center p-4 text-center">
                <Camera className="w-12 h-12 text-sky-400 animate-pulse mb-2" />
                <span className="text-xs text-white font-bold">
                  {isRtl ? 'كاميرا محاكاة نشطة' : 'Camera Simulation Active'}
                </span>
                <span className="text-[10px] text-gray-400 mt-1">
                  0:{seconds < 10 ? `0${seconds}` : seconds} / 1:00
                </span>
              </div>
            )}

            {/* Rec Dot & Duration Tag */}
            <div className="absolute top-4 bg-black/60 backdrop-blur-md px-3 py-1 rounded-full flex items-center gap-1.5 border border-white/10">
              <span className="w-2.5 h-2.5 rounded-full bg-rose-500 animate-ping" />
              <span className="text-xs font-mono font-bold text-white">
                0:{seconds < 10 ? `0${seconds}` : seconds}
              </span>
            </div>
          </div>
        </div>

        {/* Action Controls */}
        <div className="flex items-center justify-center gap-6 w-full">
          {/* Cancel */}
          <button
            onClick={() => {
              stopCamera();
              onClose();
            }}
            className="p-3.5 rounded-full bg-white/10 hover:bg-rose-500/20 text-gray-300 hover:text-rose-400 transition-all active:scale-95"
            title={isRtl ? 'إلغاء' : 'Cancel'}
          >
            <Trash2 className="w-6 h-6" />
          </button>

          {/* Finish & Send */}
          <button
            onClick={handleFinishAndSend}
            className="p-5 rounded-full bg-[#2481cc] hover:bg-[#1c6fad] text-white shadow-xl hover:scale-105 active:scale-95 transition-all flex items-center justify-center"
            title={isRtl ? 'إنهاء وإرسال' : 'Finish & Send'}
          >
            <Send className="w-7 h-7 ml-0.5 rtl:ml-0 rtl:mr-0.5" />
          </button>

          {/* Flip Camera */}
          <button
            onClick={toggleCamera}
            className="p-3.5 rounded-full bg-white/10 hover:bg-white/20 text-gray-300 hover:text-white transition-all active:scale-95"
            title={isRtl ? 'تبديل الكاميرا' : 'Switch camera'}
          >
            <RefreshCw className="w-6 h-6" />
          </button>
        </div>
      </div>
    </div>
  );
};
