import React, { useState, useRef, useEffect } from 'react';
import {
  Send,
  Smile,
  Paperclip,
  Mic,
  Image,
  FileText,
  X,
  Square,
  Play,
  Pause,
} from 'lucide-react';
import { TelegramMessage } from '../types';

interface MessageInputProps {
  onSendMessage: (text: string, replyTo?: TelegramMessage, media?: any) => void;
  replyToMessage?: TelegramMessage | null;
  onCancelReply: () => void;
  lang: 'ar' | 'en';
  isDark: boolean;
}

const COMMON_EMOJIS = [
  '😀', '😂', '🤣', '😍', '🥰', '😘', '😊', '😎', '🥳', '🤔',
  '👍', '👎', '👏', '🙌', '🙏', '🔥', '❤️', '💖', '🎉', '✨',
  '🚀', '💯', '⚡', '💡', '🌟', '👀', '🤝', '💪', '👌', '✌️',
  '☕', '🍕', '🍰', '🌸', '🌹', '💻', '📱', '🎮', '⚽', '🎯'
];

const TELEGRAM_STICKERS = [
  { id: 'stk_duck1', emoji: '🦆', name: 'Telegram Duck Welcome', img: 'https://images.unsplash.com/photo-1555861496-0666c8981751?w=160&auto=format&fit=crop&q=80' },
  { id: 'stk_cat2', emoji: '🐱', name: 'Cool Cat Glasses', img: 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=160&auto=format&fit=crop&q=80' },
  { id: 'stk_dog3', emoji: '🐶', name: 'Happy Doggy', img: 'https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=160&auto=format&fit=crop&q=80' },
  { id: 'stk_fox4', emoji: '🦊', name: 'Smart Fox', img: 'https://images.unsplash.com/photo-1474511320723-9a56873867b5?w=160&auto=format&fit=crop&q=80' },
];

export const MessageInput: React.FC<MessageInputProps> = ({
  onSendMessage,
  replyToMessage,
  onCancelReply,
  lang,
  isDark,
}) => {
  const isAr = lang === 'ar';
  const [text, setText] = useState('');
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  const [pickerTab, setPickerTab] = useState<'emojis' | 'stickers'>('emojis');
  const [showAttachMenu, setShowAttachMenu] = useState(false);

  // Voice recording simulation
  const [isRecording, setIsRecording] = useState(false);
  const [recordingSeconds, setRecordingSeconds] = useState(0);
  const timerRef = useRef<any>(null);

  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const imageInputRef = useRef<HTMLInputElement>(null);

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
    setShowEmojiPicker(false);
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

  const handleSendSticker = (sticker: any) => {
    onSendMessage(`[ملصق تليجرام ${sticker.emoji}]`, replyToMessage || undefined, {
      type: 'photo',
      url: sticker.img,
      title: sticker.name,
    });
    setShowEmojiPicker(false);
  };

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>, type: 'photo' | 'document') => {
    const file = e.target.files?.[0];
    if (!file) return;

    setShowAttachMenu(false);

    // Read as Data URL
    const reader = new FileReader();
    reader.onload = () => {
      const dataUrl = reader.result as string;
      onSendMessage(
        type === 'photo' ? (isAr ? '📷 صورة' : '📷 Photo') : `📎 ${file.name}`,
        replyToMessage || undefined,
        {
          type,
          url: dataUrl,
          fileName: file.name,
          fileSize: `${(file.size / 1024).toFixed(1)} KB`,
        }
      );
    };
    reader.readAsDataURL(file);
    e.target.value = '';
  };

  // Voice recording toggle
  const handleStartRecording = () => {
    setIsRecording(true);
  };

  const handleCancelRecording = () => {
    setIsRecording(false);
  };

  const handleFinishRecording = () => {
    setIsRecording(false);
    onSendMessage(isAr ? '🎤 رسالة صوتية' : '🎤 Voice message', replyToMessage || undefined, {
      type: 'voice',
      duration: recordingSeconds || 4,
    });
  };

  const formatTimer = (sec: number) => {
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <div
      className={`p-3 relative border-t select-none ${
        isDark ? 'bg-[#17212b] border-[#0e1621]' : 'bg-white border-gray-200'
      }`}
    >
      {/* Hidden File Inputs */}
      <input
        type="file"
        ref={imageInputRef}
        accept="image/*"
        className="hidden"
        onChange={(e) => handleFileUpload(e, 'photo')}
      />
      <input
        type="file"
        ref={fileInputRef}
        accept="*/*"
        className="hidden"
        onChange={(e) => handleFileUpload(e, 'document')}
      />

      {/* Reply Preview Bar */}
      {replyToMessage && (
        <div
          className={`flex items-center justify-between mb-2 p-2 rounded-xl text-xs border-s-4 border-[#3390ec] ${
            isDark ? 'bg-[#202b36]' : 'bg-gray-100'
          }`}
        >
          <div className="min-w-0 flex-1">
            <span className="font-bold text-[#3390ec] block">
              {isAr ? 'الرد على' : 'Reply to'} {replyToMessage.senderName}
            </span>
            <p className="truncate text-gray-400 mt-0.5">{replyToMessage.text}</p>
          </div>
          <button
            onClick={onCancelReply}
            className="p-1 rounded-full text-gray-400 hover:text-white"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Attachment Popover */}
      {showAttachMenu && (
        <div
          className={`absolute bottom-16 start-3 rounded-2xl shadow-2xl p-2 z-30 flex flex-col gap-1 w-48 border ${
            isDark ? 'bg-[#242f3d] border-[#2f3f50] text-white' : 'bg-white border-gray-200 text-gray-800'
          }`}
        >
          <button
            onClick={() => imageInputRef.current?.click()}
            className={`flex items-center gap-3 px-3 py-2.5 rounded-xl text-xs font-semibold transition ${
              isDark ? 'hover:bg-[#2b394a]' : 'hover:bg-gray-100'
            }`}
          >
            <div className="w-8 h-8 rounded-full bg-blue-500/20 text-blue-400 flex items-center justify-center">
              <Image className="w-4 h-4" />
            </div>
            <span>{isAr ? 'صورة أو فيديو' : 'Photo or Video'}</span>
          </button>

          <button
            onClick={() => fileInputRef.current?.click()}
            className={`flex items-center gap-3 px-3 py-2.5 rounded-xl text-xs font-semibold transition ${
              isDark ? 'hover:bg-[#2b394a]' : 'hover:bg-gray-100'
            }`}
          >
            <div className="w-8 h-8 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center">
              <FileText className="w-4 h-4" />
            </div>
            <span>{isAr ? 'ملف أو مستند' : 'Document'}</span>
          </button>
        </div>
      )}

      {/* Emoji & Sticker Picker Popover */}
      {showEmojiPicker && (
        <div
          className={`absolute bottom-16 start-3 rounded-2xl shadow-2xl p-3 z-30 w-80 max-h-72 flex flex-col border ${
            isDark ? 'bg-[#242f3d] border-[#2f3f50]' : 'bg-white border-gray-200'
          }`}
        >
          {/* Tabs */}
          <div className="flex border-b border-gray-700/30 pb-2 mb-2">
            <button
              onClick={() => setPickerTab('emojis')}
              className={`flex-1 text-xs py-1.5 font-bold rounded-lg ${
                pickerTab === 'emojis'
                  ? 'bg-[#3390ec] text-white'
                  : isDark
                  ? 'text-gray-400 hover:text-white'
                  : 'text-gray-600 hover:text-black'
              }`}
            >
              {isAr ? 'الرموز التعبيرية' : 'Emojis'}
            </button>
            <button
              onClick={() => setPickerTab('stickers')}
              className={`flex-1 text-xs py-1.5 font-bold rounded-lg ${
                pickerTab === 'stickers'
                  ? 'bg-[#3390ec] text-white'
                  : isDark
                  ? 'text-gray-400 hover:text-white'
                  : 'text-gray-600 hover:text-black'
              }`}
            >
              {isAr ? 'الملصقات' : 'Stickers'}
            </button>
          </div>

          {/* Emoji Grid */}
          {pickerTab === 'emojis' && (
            <div className="grid grid-cols-8 gap-1 overflow-y-auto max-h-52 p-1 text-xl">
              {COMMON_EMOJIS.map((em, idx) => (
                <button
                  key={idx}
                  onClick={() => handleSelectEmoji(em)}
                  className="hover:scale-125 transition-transform p-1.5 rounded-lg flex items-center justify-center"
                >
                  {em}
                </button>
              ))}
            </div>
          )}

          {/* Stickers Grid */}
          {pickerTab === 'stickers' && (
            <div className="grid grid-cols-2 gap-2 overflow-y-auto max-h-52 p-1">
              {TELEGRAM_STICKERS.map((stk) => (
                <button
                  key={stk.id}
                  onClick={() => handleSendSticker(stk)}
                  className={`p-2 rounded-xl border flex flex-col items-center gap-1 transition hover:scale-105 ${
                    isDark ? 'border-[#2f3f50] hover:bg-[#2b394a]' : 'border-gray-200 hover:bg-gray-100'
                  }`}
                >
                  <img
                    src={stk.img}
                    alt={stk.name}
                    referrerPolicy="no-referrer"
                    className="w-16 h-16 object-cover rounded-lg"
                  />
                  <span className="text-[10px] text-gray-400 truncate max-w-full">
                    {stk.name}
                  </span>
                </button>
              ))}
            </div>
          )}
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
              <span className="text-xs text-gray-400 ms-2">
                {isAr ? 'جارٍ تسجيل الصوت...' : 'Recording voice note...'}
              </span>
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={handleCancelRecording}
                className="text-xs text-gray-400 hover:text-red-400 font-medium px-2 py-1"
              >
                {isAr ? 'إلغاء' : 'Cancel'}
              </button>
              <button
                onClick={handleFinishRecording}
                className="w-8 h-8 rounded-full bg-[#3390ec] hover:bg-[#2b7ec9] text-white flex items-center justify-center shadow"
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
                setShowEmojiPicker(false);
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
                  // auto expand
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
                  setShowEmojiPicker(!showEmojiPicker);
                  setShowAttachMenu(false);
                }}
                className={`p-1.5 rounded-full transition ms-1 ${
                  isDark ? 'text-gray-400 hover:text-amber-400' : 'text-gray-500 hover:text-amber-500'
                }`}
                title={isAr ? 'الرموز والملصقات' : 'Emojis and stickers'}
              >
                <Smile className="w-5 h-5" />
              </button>
            </div>

            {/* Right Action Button: Send or Microphone */}
            {text.trim() ? (
              <button
                onClick={handleSend}
                className="w-10 h-10 rounded-full bg-[#3390ec] hover:bg-[#2b7ec9] text-white flex items-center justify-center shadow-md transition-transform hover:scale-105 active:scale-95"
                title={isAr ? 'إرسال' : 'Send'}
              >
                <Send className="w-5 h-5 translate-x-[-1px] translate-y-[1px]" />
              </button>
            ) : (
              <button
                onClick={handleStartRecording}
                className={`w-10 h-10 rounded-full flex items-center justify-center transition ${
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
