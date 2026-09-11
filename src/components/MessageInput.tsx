import React, { useState, useRef, useEffect } from 'react';
import {
  Send,
  Smile,
  Paperclip,
  Mic,
  Image,
  FileText,
  X,
  Trash2,
  Sparkles,
  Layers,
} from 'lucide-react';
import { TelegramMessage } from '../types';
import { StickerAndGifDrawer } from './StickerAndGifDrawer';

interface MessageInputProps {
  onSendMessage: (text: string, replyTo?: TelegramMessage, media?: any) => void;
  replyToMessage?: TelegramMessage | null;
  onCancelReply: () => void;
  onOpenMiniApp?: () => void;
  lang: 'ar' | 'en';
  isDark: boolean;
}

export const MessageInput: React.FC<MessageInputProps> = ({
  onSendMessage,
  replyToMessage,
  onCancelReply,
  onOpenMiniApp,
  lang,
  isDark,
}) => {
  const isAr = lang === 'ar';
  const [text, setText] = useState('');
  const [showPicker, setShowPicker] = useState(false);
  const [showAttachMenu, setShowAttachMenu] = useState(false);

  // Real Voice recording with MediaRecorder
  const [isRecording, setIsRecording] = useState(false);
  const [recordingSeconds, setRecordingSeconds] = useState(0);
  const timerRef = useRef<any>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);
  const streamRef = useRef<MediaStream | null>(null);

  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const imageInputRef = useRef<HTMLInputElement>(null);

  // Recording timer
  useEffect(() => {
    if (isRecording) {
      timerRef.current = setInterval(() => {
        setRecordingSeconds((prev) => prev + 1);
      }, 1000);
    } else {
      if (timerRef.current) clearInterval(timerRef.current);
      setRecordingSeconds(0);
    }
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [isRecording]);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleSend = () => {
    if (!text.trim()) return;
    onSendMessage(text.trim(), replyToMessage || undefined);
    setText('');
    setShowPicker(false);
    setShowAttachMenu(false);
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }
  };

  const handleSelectEmoji = (emoji: string) => {
    setText((prev) => prev + emoji);
    if (textareaRef.current) {
      textareaRef.current.focus();
    }
  };

  const handleSelectSticker = (sticker: { id: string; emoji: string; name: string; img: string }) => {
    onSendMessage(`[ملصق تليجرام ${sticker.emoji}]`, replyToMessage || undefined, {
      type: 'photo',
      url: sticker.img,
      title: sticker.name,
    });
    setShowPicker(false);
  };

  const handleSelectGif = (gif: { id: string; title: string; url: string }) => {
    onSendMessage(`[GIF: ${gif.title}]`, replyToMessage || undefined, {
      type: 'video',
      url: gif.url,
      title: gif.title,
    });
    setShowPicker(false);
  };

  // Start Real Voice Recording
  const handleStartRecording = async () => {
    try {
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        throw new Error('المتصفح لا يدعم تسجيل الصوت');
      }

      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      streamRef.current = stream;

      const mediaRecorder = new MediaRecorder(stream);
      mediaRecorderRef.current = mediaRecorder;
      audioChunksRef.current = [];

      mediaRecorder.ondataavailable = (event) => {
        if (event.data && event.data.size > 0) {
          audioChunksRef.current.push(event.data);
        }
      };

      mediaRecorder.start();
      setIsRecording(true);
      setShowAttachMenu(false);
      setShowPicker(false);
    } catch (err: any) {
      console.warn('Microphone access fallback to simulation:', err);
      // Simulated voice recording fallback
      setIsRecording(true);
    }
  };

  // Finish and Send Recording
  const handleFinishRecording = () => {
    if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
      mediaRecorderRef.current.onstop = () => {
        const audioBlob = new Blob(audioChunksRef.current, { type: 'audio/webm' });
        const audioUrl = URL.createObjectURL(audioBlob);
        
        onSendMessage(
          isAr ? `🎙️ رسالة صوتية (${recordingSeconds} ثانية)` : `🎙️ Voice message (${recordingSeconds}s)`,
          replyToMessage || undefined,
          {
            type: 'voice',
            url: audioUrl,
            duration: recordingSeconds || 4,
            title: isAr ? 'رسالة صوتية' : 'Voice note',
          }
        );

        if (streamRef.current) {
          streamRef.current.getTracks().forEach((track) => track.stop());
        }
      };

      mediaRecorderRef.current.stop();
    } else {
      // Send simulated voice note
      onSendMessage(
        isAr ? `🎙️ رسالة صوتية (${recordingSeconds || 4} ثانية)` : `🎙️ Voice message (${recordingSeconds || 4}s)`,
        replyToMessage || undefined,
        {
          type: 'voice',
          duration: recordingSeconds || 4,
          title: isAr ? 'رسالة صوتية' : 'Voice note',
        }
      );
    }

    setIsRecording(false);
  };

  // Cancel Recording
  const handleCancelRecording = () => {
    if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
      mediaRecorderRef.current.stop();
    }
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop());
    }
    setIsRecording(false);
  };

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>, type: 'photo' | 'document') => {
    const file = e.target.files?.[0];
    if (!file) return;

    setShowAttachMenu(false);

    const reader = new FileReader();
    reader.onload = () => {
      const dataUrl = reader.result as string;
      onSendMessage(
        type === 'photo' ? (isAr ? '📷 صورة' : '📷 Photo') : `📎 ${file.name}`,
        replyToMessage || undefined,
        {
          type: type === 'photo' ? 'photo' : 'document',
          url: dataUrl,
          fileName: file.name,
          fileSize: (file.size / 1024).toFixed(1) + ' KB',
          title: file.name,
        }
      );
    };
    reader.readAsDataURL(file);
    e.target.value = '';
  };

  const formatTimer = (sec: number) => {
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  };

  return (
    <div className={`p-3 relative border-t ${isDark ? 'bg-[#17212b] border-[#242f3d]' : 'bg-white border-gray-200'}`}>
      {/* Hidden File Inputs */}
      <input
        ref={imageInputRef}
        type="file"
        accept="image/*,video/*"
        className="hidden"
        onChange={(e) => handleFileUpload(e, 'photo')}
      />
      <input
        ref={fileInputRef}
        type="file"
        className="hidden"
        onChange={(e) => handleFileUpload(e, 'document')}
      />

      {/* Reply Banner */}
      {replyToMessage && (
        <div className={`flex items-center justify-between px-3 py-2 mb-2 rounded-xl border text-xs ${
          isDark ? 'bg-[#242f3d] border-[#2f3f50] text-gray-200' : 'bg-gray-100 border-gray-300 text-gray-800'
        }`}>
          <div className="flex items-center gap-2 overflow-hidden">
            <div className="w-1 h-7 bg-[#3390ec] rounded-full shrink-0" />
            <div className="truncate">
              <span className="font-semibold text-[#3390ec] block">
                {replyToMessage.senderName}
              </span>
              <span className="text-gray-400 truncate block">
                {replyToMessage.text || (replyToMessage.media ? `[${replyToMessage.media.type}]` : '')}
              </span>
            </div>
          </div>
          <button
            onClick={onCancelReply}
            className="p-1 rounded-full text-gray-400 hover:text-gray-200 hover:bg-black/10"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Telegram K Attachment Menu */}
      {showAttachMenu && (
        <div
          className={`absolute bottom-full mb-3 ${isAr ? 'right-4' : 'left-4'} p-2 rounded-2xl shadow-2xl border flex flex-col gap-1 z-40 animate-in fade-in slide-in-from-bottom-2 ${
            isDark ? 'bg-[#242f3d] border-[#2f3f50] text-white' : 'bg-white border-gray-200 text-gray-800'
          }`}
        >
          <button
            onClick={() => imageInputRef.current?.click()}
            className="flex items-center gap-3 px-3 py-2 rounded-xl text-xs font-medium hover:bg-[#3390ec]/10 hover:text-[#3390ec] transition"
          >
            <div className="w-8 h-8 rounded-full bg-blue-500/20 text-blue-500 flex items-center justify-center">
              <Image className="w-4 h-4" />
            </div>
            <span>{isAr ? 'صورة أو فيديو' : 'Photo or Video'}</span>
          </button>

          <button
            onClick={() => fileInputRef.current?.click()}
            className="flex items-center gap-3 px-3 py-2 rounded-xl text-xs font-medium hover:bg-[#3390ec]/10 hover:text-[#3390ec] transition"
          >
            <div className="w-8 h-8 rounded-full bg-purple-500/20 text-purple-500 flex items-center justify-center">
              <FileText className="w-4 h-4" />
            </div>
            <span>{isAr ? 'ملف أو مستند' : 'File or Document'}</span>
          </button>

          {onOpenMiniApp && (
            <button
              onClick={() => {
                setShowAttachMenu(false);
                onOpenMiniApp();
              }}
              className="flex items-center gap-3 px-3 py-2 rounded-xl text-xs font-medium hover:bg-[#3390ec]/10 hover:text-[#3390ec] transition"
            >
              <div className="w-8 h-8 rounded-full bg-amber-500/20 text-amber-500 flex items-center justify-center">
                <Sparkles className="w-4 h-4" />
              </div>
              <span>{isAr ? 'تطبيقات الويب المصغرة' : 'Telegram Mini Apps'}</span>
            </button>
          )}
        </div>
      )}

      {/* Tabbed Sticker, GIF & Emoji Drawer */}
      <StickerAndGifDrawer
        isOpen={showPicker}
        onClose={() => setShowPicker(false)}
        onSelectEmoji={handleSelectEmoji}
        onSelectSticker={handleSelectSticker}
        onSelectGif={handleSelectGif}
        isDark={isDark}
        lang={lang}
      />

      {/* Main Input Row */}
      <div className="flex items-end gap-2">
        {isRecording ? (
          // Recording Live View
          <div className="flex-1 flex items-center justify-between bg-red-500/10 border border-red-500/30 rounded-2xl px-4 py-3">
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 bg-red-500 rounded-full animate-ping" />
              <span className="text-red-500 text-xs font-bold font-mono">
                {formatTimer(recordingSeconds)}
              </span>
              
              {/* Dynamic waveform visualizer dots */}
              <div className="flex items-center gap-0.5 ms-2">
                {[12, 24, 16, 32, 20, 28, 14, 22, 30, 18].map((h, i) => (
                  <div
                    key={i}
                    className="w-1 bg-red-500 rounded-full animate-pulse"
                    style={{
                      height: `${h}px`,
                      animationDelay: `${i * 100}ms`,
                    }}
                  />
                ))}
              </div>

              <span className="text-xs text-gray-400 ms-2 hidden sm:inline">
                {isAr ? 'جارٍ تسجيل الصوت...' : 'Recording voice note...'}
              </span>
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={handleCancelRecording}
                className="p-1.5 rounded-full text-gray-400 hover:text-red-400 hover:bg-red-500/10 transition"
                title={isAr ? 'إلغاء التسجيل' : 'Cancel recording'}
              >
                <Trash2 className="w-4 h-4" />
              </button>

              <button
                onClick={handleFinishRecording}
                className="w-8 h-8 rounded-full bg-[#3390ec] hover:bg-[#2b7ec9] text-white flex items-center justify-center shadow transition-transform active:scale-95"
                title={isAr ? 'إرسال الرسالة الصوتية' : 'Send voice note'}
              >
                <Send className="w-4 h-4" />
              </button>
            </div>
          </div>
        ) : (
          <>
            {/* Attachment Button */}
            <button
              onClick={() => {
                setShowAttachMenu(!showAttachMenu);
                setShowPicker(false);
              }}
              className={`p-2.5 rounded-full transition ${
                isDark ? 'text-gray-400 hover:text-white hover:bg-[#202b36]' : 'text-gray-500 hover:text-gray-900 hover:bg-gray-100'
              }`}
              title={isAr ? 'إرفاق وسائط' : 'Attach'}
            >
              <Paperclip className="w-5 h-5" />
            </button>

            {/* Input Box with Emoji Button inside */}
            <div
              className={`flex-1 flex items-end rounded-2xl px-3 py-1.5 border transition ${
                isDark
                  ? 'bg-[#242f3d] border-[#2f3f50] focus-within:border-[#3390ec]'
                  : 'bg-gray-100 border-gray-200 focus-within:bg-white focus-within:border-[#3390ec]'
              }`}
            >
              <textarea
                ref={textareaRef}
                rows={1}
                value={text}
                onChange={(e) => {
                  setText(e.target.value);
                  e.target.style.height = 'auto';
                  e.target.style.height = `${Math.min(e.target.scrollHeight, 120)}px`;
                }}
                onKeyDown={handleKeyDown}
                placeholder={isAr ? 'اكتب رسالة...' : 'Write a message...'}
                className={`flex-1 bg-transparent text-sm resize-none focus:outline-none max-h-32 py-1 ${
                  isDark ? 'text-white placeholder-gray-400' : 'text-gray-900 placeholder-gray-500'
                }`}
              />

              <button
                onClick={() => {
                  setShowPicker(!showPicker);
                  setShowAttachMenu(false);
                }}
                className={`p-1.5 rounded-full transition ms-1 ${
                  showPicker
                    ? 'text-[#3390ec]'
                    : isDark
                    ? 'text-gray-400 hover:text-amber-400'
                    : 'text-gray-500 hover:text-amber-500'
                }`}
                title={isAr ? 'الرموز والملصقات وGIFs' : 'Emojis, Stickers & GIFs'}
              >
                <Smile className="w-5 h-5" />
              </button>
            </div>

            {/* Right Action Button: Send or Microphone */}
            {text.trim() ? (
              <button
                onClick={handleSend}
                className="w-10 h-10 rounded-full bg-[#3390ec] hover:bg-[#2b7ec9] text-white flex items-center justify-center shadow-md transition-transform hover:scale-105 active:scale-95 shrink-0"
                title={isAr ? 'إرسال' : 'Send'}
              >
                <Send className="w-5 h-5 translate-x-[-1px] translate-y-[1px]" />
              </button>
            ) : (
              <button
                onClick={handleStartRecording}
                className={`w-10 h-10 rounded-full flex items-center justify-center transition shrink-0 ${
                  isDark
                    ? 'text-gray-400 hover:text-white hover:bg-[#202b36]'
                    : 'text-gray-500 hover:text-gray-900 hover:bg-gray-100'
                }`}
                title={isAr ? 'تسجيل رسالة صوتية' : 'Record voice note'}
              >
                <Mic className="w-5 h-5" />
              </button>
            )}
          </>
        )}
      </div>
    </div>
  );
};
