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
  LayoutGrid,
  Bot,
  Terminal,
  Search,
  ExternalLink,
} from 'lucide-react';
import { TelegramMessage, TelegramChat, TelegramBotCommand, TelegramReplyMarkup, TelegramInlineQueryResult } from '../types';
import { StickerAndGifDrawer } from './StickerAndGifDrawer';

interface MessageInputProps {
  onSendMessage: (text: string, replyTo?: TelegramMessage, media?: any) => void;
  replyToMessage?: TelegramMessage | null;
  onCancelReply: () => void;
  onOpenMiniApp?: () => void;
  chat?: TelegramChat | null;
  activeReplyKeyboard?: TelegramReplyMarkup | null;
  lang: 'ar' | 'en';
  isDark: boolean;
}

export const MessageInput: React.FC<MessageInputProps> = ({
  onSendMessage,
  replyToMessage,
  onCancelReply,
  onOpenMiniApp,
  chat,
  activeReplyKeyboard,
  lang,
  isDark,
}) => {
  const isAr = lang === 'ar';
  const [text, setText] = useState('');
  const [showPicker, setShowPicker] = useState(false);
  const [showAttachMenu, setShowAttachMenu] = useState(false);
  const [showCommandsMenu, setShowCommandsMenu] = useState(false);
  const [showReplyKeyboard, setShowReplyKeyboard] = useState(true);

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

  const isBotChat = chat?.type === 'bot' || chat?.isBot;

  // Bot commands list
  const botCommands: TelegramBotCommand[] = chat?.botInfo?.commands || [
    { command: 'start', description: isAr ? 'تشغيل البوت وبدء المحادثة' : 'Start the bot' },
    { command: 'help', description: isAr ? 'دليل الأوامر والمساعدة' : 'Show help and guide' },
    { command: 'app', description: isAr ? 'فتح تطبيق الويب المصغر (Mini App)' : 'Launch Mini App' },
    { command: 'keyboard', description: isAr ? 'إظهار لوحة الأزرار التفاعلية' : 'Show interactive keyboard' },
    { command: 'settings', description: isAr ? 'إعدادات وتخصيص البوت' : 'Bot settings & config' },
    { command: 'inline', description: isAr ? 'دليل الاستعلام الفوري عبر @' : 'Inline query guide' },
  ];

  // Autocomplete command matching when text starts with '/'
  const isSlashCommand = text.startsWith('/') && !text.includes(' ');
  const matchingCommands = isSlashCommand
    ? botCommands.filter((c) =>
        c.command.toLowerCase().startsWith(text.slice(1).toLowerCase())
      )
    : [];

  // Inline Query detection: e.g. "@gif " or "@pic " or "@smart_helper_bot "
  const inlineMatch = text.match(/^@([a-zA-Z0-9_]+)\s*(.*)$/);
  const isInlineQuery = !!inlineMatch && text.includes(' ');
  const inlineBotName = inlineMatch ? inlineMatch[1] : '';
  const inlineQueryString = inlineMatch ? inlineMatch[2] : '';

  // Mock inline results based on bot name & query
  const getInlineResults = (): TelegramInlineQueryResult[] => {
    if (!isInlineQuery) return [];
    const bot = inlineBotName.toLowerCase();
    const q = inlineQueryString.toLowerCase();

    if (bot === 'gif') {
      const gifs = [
        {
          id: 'in_gif_1',
          type: 'gif' as const,
          title: 'Party & Celebration 🎉',
          thumbUrl: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300&auto=format&fit=crop&q=80',
          contentText: '🎉 احتفال مميز!',
        },
        {
          id: 'in_gif_2',
          type: 'gif' as const,
          title: 'Thumbs Up 👍',
          thumbUrl: 'https://images.unsplash.com/photo-1584447141267-3c72b223cb60?w=300&auto=format&fit=crop&q=80',
          contentText: '👍 رائع جداً وممتاز!',
        },
        {
          id: 'in_gif_3',
          type: 'gif' as const,
          title: 'Rocket Launch 🚀',
          thumbUrl: 'https://images.unsplash.com/photo-1517976487541-112df8b1a8d0?w=300&auto=format&fit=crop&q=80',
          contentText: '🚀 انطلاق إلى القمة!',
        },
      ];
      return q ? gifs.filter((g) => g.title.toLowerCase().includes(q)) : gifs;
    }

    if (bot === 'pic' || bot === 'bing') {
      const pics = [
        {
          id: 'in_pic_1',
          type: 'photo' as const,
          title: 'Nature Mountain 🏔️',
          thumbUrl: 'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=300&auto=format&fit=crop&q=80',
          contentText: '🏔️ منظر طبيعي خلاب للجبال',
        },
        {
          id: 'in_pic_2',
          type: 'photo' as const,
          title: 'Golden Sunset 🌅',
          thumbUrl: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=300&auto=format&fit=crop&q=80',
          contentText: '🌅 غروب الشمس الذهبي',
        },
      ];
      return q ? pics.filter((p) => p.title.toLowerCase().includes(q)) : pics;
    }

    return [
      {
        id: 'in_def_1',
        type: 'article' as const,
        title: `${isAr ? 'إرسال استعلام' : 'Send query'}: ${inlineQueryString || 'بحث عام'}`,
        description: `@${inlineBotName} • ${isAr ? 'نتيجة استعلام فوري' : 'Instant inline result'}`,
        contentText: `[استعلام فوري @${inlineBotName}]: ${inlineQueryString || 'تم استلام الاستعلام بنجاح'}`,
      },
      {
        id: 'in_def_2',
        type: 'article' as const,
        title: isAr ? 'دليل بوتات تيليجرام التفاعلية' : 'Telegram Bot Guide',
        description: 'https://core.telegram.org/bots',
        contentText: 'وثائق بوتات تيليجرام الرسمية: https://core.telegram.org/bots',
      },
    ];
  };

  const inlineResults = getInlineResults();

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

      {/* Bot Commands Menu / Autocomplete Popup */}
      {(showCommandsMenu || (isSlashCommand && matchingCommands.length > 0)) && (
        <div
          className={`absolute bottom-full mb-2 start-0 end-0 z-30 max-h-64 overflow-y-auto rounded-2xl shadow-2xl border backdrop-blur-md animate-scale-in p-2 ${
            isDark ? 'bg-[#17212b]/95 border-[#2f3f50] text-white' : 'bg-white/95 border-gray-200 text-gray-800'
          }`}
        >
          <div className="flex items-center justify-between px-3 py-1.5 border-b border-gray-700/20 mb-1">
            <div className="flex items-center gap-2 text-xs font-bold text-[#3390ec]">
              <Terminal className="w-3.5 h-3.5" />
              <span>{isAr ? 'أوامر البوت المتاحة' : 'Bot Commands'}</span>
              <span className="text-[10px] text-gray-400 font-normal">
                ({(isSlashCommand ? matchingCommands : botCommands).length})
              </span>
            </div>
            {showCommandsMenu && (
              <button
                onClick={() => setShowCommandsMenu(false)}
                className="p-1 rounded-full text-gray-400 hover:text-white hover:bg-white/10"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>

          <div className="space-y-1">
            {(isSlashCommand ? matchingCommands : botCommands).map((cmd) => (
              <button
                key={cmd.command}
                type="button"
                onClick={() => {
                  onSendMessage(`/${cmd.command}`, replyToMessage || undefined);
                  setText('');
                  setShowCommandsMenu(false);
                }}
                className={`w-full text-start px-3 py-2 rounded-xl text-xs flex items-center justify-between transition ${
                  isDark ? 'hover:bg-[#242f3d]' : 'hover:bg-gray-100'
                }`}
              >
                <div className="flex items-center gap-2">
                  <span className="font-mono font-bold text-[#3390ec]">/{cmd.command}</span>
                  <span className="text-gray-400 text-[11px] truncate">{cmd.description}</span>
                </div>
                <span className="text-[10px] text-gray-500 font-mono hidden sm:inline">↵</span>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Bot Inline Query Results Overlay (@bot query) */}
      {isInlineQuery && inlineResults.length > 0 && (
        <div
          className={`absolute bottom-full mb-2 start-0 end-0 z-30 max-h-72 overflow-y-auto rounded-2xl shadow-2xl border backdrop-blur-md animate-scale-in p-2 ${
            isDark ? 'bg-[#17212b]/95 border-[#2f3f50] text-white' : 'bg-white/95 border-gray-200 text-gray-800'
          }`}
        >
          <div className="flex items-center justify-between px-3 py-1.5 border-b border-gray-700/20 mb-1.5">
            <div className="flex items-center gap-2 text-xs font-bold text-[#3390ec]">
              <Search className="w-3.5 h-3.5" />
              <span>
                {isAr ? 'نتائج الاستعلام الفوري عبر' : 'Inline Bot Results from'} @{inlineBotName}
              </span>
            </div>
            <span className="text-[11px] text-gray-400">{inlineResults.length} {isAr ? 'نتيجة' : 'results'}</span>
          </div>

          {/* If results are media (gifs or photos), display in a responsive visual grid */}
          {inlineResults.some((r) => r.type === 'gif' || r.type === 'photo') ? (
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 p-1">
              {inlineResults.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => {
                    onSendMessage(item.contentText, replyToMessage || undefined, {
                      type: item.type === 'gif' ? 'video' : 'photo',
                      url: item.thumbUrl || item.url,
                      title: item.title,
                    });
                    setText('');
                  }}
                  className={`group relative rounded-xl overflow-hidden aspect-video border transition transform hover:scale-[1.02] active:scale-95 text-start ${
                    isDark ? 'border-gray-700/60 bg-[#242f3d]' : 'border-gray-200 bg-gray-100'
                  }`}
                >
                  <img
                    src={item.thumbUrl || item.url}
                    alt={item.title}
                    referrerPolicy="no-referrer"
                    className="w-full h-full object-cover group-hover:opacity-90 transition-opacity"
                  />
                  <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/80 to-transparent p-1.5">
                    <p className="text-[10px] text-white font-medium truncate">{item.title}</p>
                  </div>
                </button>
              ))}
            </div>
          ) : (
            <div className="space-y-1">
              {inlineResults.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => {
                    onSendMessage(item.contentText, replyToMessage || undefined);
                    setText('');
                  }}
                  className={`w-full text-start px-3 py-2 rounded-xl text-xs flex items-center justify-between transition ${
                    isDark ? 'hover:bg-[#242f3d]' : 'hover:bg-gray-100'
                  }`}
                >
                  <div className="flex flex-col">
                    <span className="font-semibold">{item.title}</span>
                    {item.description && (
                      <span className="text-[11px] text-gray-400 truncate">{item.description}</span>
                    )}
                  </div>
                  <ExternalLink className="w-3.5 h-3.5 text-gray-400 shrink-0 ms-2" />
                </button>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Bot Custom Reply Keyboard (ReplyKeyboardMarkup) */}
      {activeReplyKeyboard?.keyboard && activeReplyKeyboard.keyboard.length > 0 && showReplyKeyboard && (
        <div
          className={`mb-2.5 p-2 rounded-2xl border space-y-1.5 transition-all ${
            isDark ? 'bg-[#17212b]/80 border-[#2f3f50]' : 'bg-gray-50 border-gray-200'
          }`}
        >
          {activeReplyKeyboard.keyboard.map((row, rowIdx) => (
            <div key={rowIdx} className="flex gap-1.5 w-full">
              {row.map((btn, btnIdx) => (
                <button
                  key={btnIdx}
                  type="button"
                  onClick={() => onSendMessage(btn.text, replyToMessage || undefined)}
                  className={`flex-1 py-2 px-3 rounded-xl text-xs font-semibold text-center shadow-sm border transition transform active:scale-98 ${
                    isDark
                      ? 'bg-[#242f3d] hover:bg-[#3390ec] hover:text-white border-[#2f3f50] text-gray-200'
                      : 'bg-white hover:bg-[#3390ec] hover:text-white border-gray-200 text-gray-800'
                  }`}
                >
                  {btn.text}
                </button>
              ))}
            </div>
          ))}
        </div>
      )}

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

            {/* Bot Command Menu Button (Telegram Web K mechanism) */}
            {isBotChat && (
              <button
                type="button"
                onClick={() => {
                  setShowCommandsMenu(!showCommandsMenu);
                  setShowPicker(false);
                  setShowAttachMenu(false);
                }}
                className={`h-10 px-2.5 rounded-xl font-mono text-xs font-bold flex items-center gap-1.5 transition shrink-0 ${
                  showCommandsMenu
                    ? 'bg-[#3390ec] text-white'
                    : isDark
                    ? 'bg-[#242f3d] text-[#3390ec] hover:bg-[#2e3b4d]'
                    : 'bg-blue-50 text-[#3390ec] hover:bg-blue-100'
                }`}
                title={isAr ? 'أوامر البوت' : 'Bot Menu'}
              >
                <Bot className="w-4 h-4" />
                <span className="hidden sm:inline text-[11px] font-sans font-semibold">
                  {chat?.botInfo?.menuButton?.text || (isAr ? 'الأوامر' : 'Menu')}
                </span>
              </button>
            )}

            {/* Toggle Bot Reply Keyboard Button if available */}
            {activeReplyKeyboard?.keyboard && activeReplyKeyboard.keyboard.length > 0 && (
              <button
                type="button"
                onClick={() => setShowReplyKeyboard(!showReplyKeyboard)}
                className={`p-2.5 rounded-full transition shrink-0 ${
                  showReplyKeyboard
                    ? 'text-[#3390ec]'
                    : isDark
                    ? 'text-gray-400 hover:text-white hover:bg-[#202b36]'
                    : 'text-gray-500 hover:text-gray-900 hover:bg-gray-100'
                }`}
                title={isAr ? 'لوحة الأزرار' : 'Reply Keyboard'}
              >
                <LayoutGrid className="w-5 h-5" />
              </button>
            )}

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
