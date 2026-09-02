import React, { useState, useRef, useEffect } from "react";
import {
  Send,
  Paperclip,
  Smile,
  Mic,
  MoreVertical,
  Search,
  Check,
  CheckCheck,
  Pin,
  X,
  Reply,
  Trash2,
  Phone,
  Video,
  Info,
  Download,
  Image as ImageIcon,
  FileText,
  Heart,
  Flame,
  ThumbsUp,
  Sparkles,
  VolumeX,
  Volume2,
  Globe,
  CornerUpRight,
  Lock,
  Unlock,
  BarChart3,
  Ghost,
  Loader2,
  Languages,
  ChevronDown,
  ChevronUp,
  Copy,
  Share2,
  ArrowRight,
  Edit3,
  Star,
  CheckSquare,
  Square,
  Play,
  Pause,
  Layers,
  Film,
  Sticker as StickerIcon,
  MessageSquare,
} from "lucide-react";
import { TelegramDialog, TelegramMessage, TelegramUser, ActiveAudioTrack, TelegramMedia } from "../types";
import { AudioPlayer } from "./AudioPlayer";
import { LinkPreviewCard } from "./LinkPreviewCard";
import { TelegramAvatar } from "./TelegramAvatar";
import { AttachmentSheet } from "./AttachmentSheet";
import { translateMessageText, transcribeVoiceAudio } from "../lib/telegramProTools";
import { TELEGRAM_STICKER_PACKS, TELEGRAM_GIFS } from "../lib/telegramStickers";

interface ChatViewProps {
  dialog: TelegramDialog | null;
  messages: TelegramMessage[];
  currentUser: TelegramUser | null;
  onBack?: () => void;
  onSendMessage: (text: string, replyToMsgId?: string | number, media?: TelegramMedia) => void;
  onEditMessage?: (msgId: string | number, newText: string) => void;
  onDeleteMessage: (msgId: string | number) => void;
  onDeleteMultipleMessages?: (msgIds: (string | number)[]) => void;
  onTogglePin: (msgId: string | number) => void;
  onToggleStar?: (msgId: string | number) => void;
  onReact: (msgId: string | number, emoji: string) => void;
  onToggleChatInfo: () => void;
  onOpenInviteModal?: (url: string) => void;
  onOpenForward?: (msg: TelegramMessage) => void;
  onOpenForwardMultiple?: (messages: TelegramMessage[]) => void;
  onToggleLockChat?: (chatId: string) => void;
  onOpenAnalytics?: (dialog: TelegramDialog) => void;
  onPlayAudioTrack?: (track: ActiveAudioTrack) => void;
  wallpaperTheme?: string;
  isGhostModeActive?: boolean;
}

const EMOJI_CATEGORIES = [
  { name: "شائعة", emojis: ["😀", "😂", "😍", "🔥", "👍", "❤️", "🎉", "👏", "🚀", "💡", "✨", "💯", "🙏", "😎", "🥳", "🤔", "🤩", "🤝", "👌", "💪", "🕊️", "🌹", "⚡", "🎯"] },
  { name: "وجوه", emojis: ["😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "🥲", "🥹", "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚", "😋", "😛"] },
  { name: "إيماءات", emojis: ["👍", "👎", "👊", "✊", "🤛", "🤜", "👏", "🙌", "👐", "🤲", "🤝", "🙏", "✍️", "💅", "🤳", "💪", "🦾", "🦿", "🦵", "🦶", "👂", "🦻", "👃", "🫀"] },
  { name: "قلوب ورموز", emojis: ["❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "❤️‍🔥", "❤️‍🩹", "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟", "☮️", "✝️", "☪️"] },
];

const QUICK_REACTION_EMOJIS = ["👍", "❤️", "🔥", "🎉", "👏", "😂", "😱", "😢", "🤔", "💯", "🙏", "🚀", "💡", "✨"];

export const ChatView: React.FC<ChatViewProps> = ({
  dialog,
  messages,
  currentUser,
  onBack,
  onSendMessage,
  onEditMessage,
  onDeleteMessage,
  onDeleteMultipleMessages,
  onTogglePin,
  onToggleStar,
  onReact,
  onToggleChatInfo,
  onOpenInviteModal,
  onOpenForward,
  onOpenForwardMultiple,
  onToggleLockChat,
  onOpenAnalytics,
  onPlayAudioTrack,
  wallpaperTheme = "default",
  isGhostModeActive = false,
}) => {
  const [inputText, setInputText] = useState("");
  const [replyingTo, setReplyingTo] = useState<TelegramMessage | null>(null);
  const [editingMessage, setEditingMessage] = useState<TelegramMessage | null>(null);
  const [showMediaPicker, setShowMediaPicker] = useState(false);
  const [mediaPickerTab, setMediaPickerTab] = useState<"emoji" | "stickers" | "gifs">("emoji");
  const [selectedStickerPackId, setSelectedStickerPackId] = useState<string>("telegram_ducks");
  const [showAttachmentSheet, setShowAttachmentSheet] = useState(false);
  const [isRecordingVoice, setIsRecordingVoice] = useState(false);
  const [recordSeconds, setRecordSeconds] = useState(0);
  const [activeReactionMenuMsgId, setActiveReactionMenuMsgId] = useState<string | number | null>(null);
  const [contextMenuMsgId, setContextMenuMsgId] = useState<string | number | null>(null);
  const [lightboxImage, setLightboxImage] = useState<string | null>(null);
  const [showScrollBottom, setShowScrollBottom] = useState(false);
  const [highlightedMsgId, setHighlightedMsgId] = useState<string | number | null>(null);
  const [copyToast, setCopyToast] = useState<string | null>(null);

  // In-Chat Search State
  const [isSearchingInChat, setIsSearchingInChat] = useState(false);
  const [chatSearchQuery, setChatSearchQuery] = useState("");
  const [searchMatchIndex, setSearchMatchIndex] = useState(0);

  // Multi-select mode
  const [isSelectionMode, setIsSelectionMode] = useState(false);
  const [selectedMsgIds, setSelectedMsgIds] = useState<(string | number)[]>([]);

  // Delete modal state
  const [deleteConfirmModal, setDeleteConfirmModal] = useState<{
    isOpen: boolean;
    msgIds: (string | number)[];
  }>({ isOpen: false, msgIds: [] });

  // Translation & Transcription state
  const [translationsMap, setTranslationsMap] = useState<Record<string | number, string>>({});
  const [translatingMsgIds, setTranslatingMsgIds] = useState<(string | number)[]>([]);
  const [transcriptionsMap, setTranscriptionsMap] = useState<Record<string | number, string>>({});
  const [transcribingMsgIds, setTranscribingMsgIds] = useState<(string | number)[]>([]);

  // Audio Playback speed
  const [audioPlaySpeed, setAudioPlaySpeed] = useState<number>(1);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const recordTimerRef = useRef<any>(null);

  const handleScroll = () => {
    if (!messagesContainerRef.current) return;
    const { scrollTop, scrollHeight, clientHeight } = messagesContainerRef.current;
    const isUp = scrollHeight - scrollTop - clientHeight > 200;
    setShowScrollBottom(isUp);
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, dialog?.id]);

  useEffect(() => {
    if (isRecordingVoice) {
      setRecordSeconds(0);
      recordTimerRef.current = setInterval(() => {
        setRecordSeconds((s) => s + 1);
      }, 1000);
    } else {
      if (recordTimerRef.current) clearInterval(recordTimerRef.current);
    }
    return () => {
      if (recordTimerRef.current) clearInterval(recordTimerRef.current);
    };
  }, [isRecordingVoice]);

  // Jump and flash target message on click
  const handleJumpToMessage = (msgId: string | number) => {
    const el = document.getElementById(`chat-msg-${msgId}`);
    if (el) {
      el.scrollIntoView({ behavior: "smooth", block: "center" });
      setHighlightedMsgId(msgId);
      setTimeout(() => setHighlightedMsgId(null), 2500);
    }
  };

  // Trigger reply to message
  const handleStartReply = (msg: TelegramMessage) => {
    setEditingMessage(null);
    setReplyingTo(msg);
    if (inputRef.current) {
      inputRef.current.focus();
    }
  };

  // Trigger editing a message
  const handleStartEdit = (msg: TelegramMessage) => {
    setReplyingTo(null);
    setEditingMessage(msg);
    setInputText(msg.text || "");
    if (inputRef.current) {
      inputRef.current.focus();
    }
  };

  const handleCancelEdit = () => {
    setEditingMessage(null);
    setInputText("");
  };

  const showToast = (text: string) => {
    setCopyToast(text);
    setTimeout(() => setCopyToast(null), 2500);
  };

  const handleCopyMessageText = (text: string) => {
    navigator.clipboard?.writeText(text);
    showToast("تم نسخ النص إلى الحافظة");
    setContextMenuMsgId(null);
  };

  // Multi-selection Handlers
  const handleToggleSelectMessage = (msgId: string | number) => {
    if (selectedMsgIds.includes(msgId)) {
      const next = selectedMsgIds.filter((id) => id !== msgId);
      setSelectedMsgIds(next);
      if (next.length === 0) setIsSelectionMode(false);
    } else {
      setSelectedMsgIds([...selectedMsgIds, msgId]);
      setIsSelectionMode(true);
    }
  };

  const handleSelectAll = () => {
    setSelectedMsgIds(messages.map((m) => m.id));
  };

  const handleCancelSelection = () => {
    setIsSelectionMode(false);
    setSelectedMsgIds([]);
  };

  const handleDeleteSelected = () => {
    if (selectedMsgIds.length === 0) return;
    setDeleteConfirmModal({
      isOpen: true,
      msgIds: selectedMsgIds,
    });
  };

  const handleConfirmDelete = (forEveryone: boolean) => {
    const idsToDelete = deleteConfirmModal.msgIds;
    if (idsToDelete.length === 1) {
      onDeleteMessage(idsToDelete[0]);
    } else if (onDeleteMultipleMessages) {
      onDeleteMultipleMessages(idsToDelete);
    } else {
      idsToDelete.forEach((id) => onDeleteMessage(id));
    }
    setDeleteConfirmModal({ isOpen: false, msgIds: [] });
    handleCancelSelection();
    showToast("تم حذف الرسائل");
  };

  const handleForwardSelected = () => {
    const msgs = messages.filter((m) => selectedMsgIds.includes(m.id));
    if (msgs.length === 1 && onOpenForward) {
      onOpenForward(msgs[0]);
    } else if (onOpenForwardMultiple) {
      onOpenForwardMultiple(msgs);
    }
    handleCancelSelection();
  };

  const handleStarSelected = () => {
    if (onToggleStar) {
      selectedMsgIds.forEach((id) => onToggleStar(id));
      showToast("تم تحديث تمييز الرسائل");
    }
    handleCancelSelection();
  };

  if (!dialog) {
    return (
      <div
        id="telegram-chat-empty-state"
        className="flex-1 h-full flex flex-col items-center justify-center bg-slate-50 dark:bg-[#0e1621] p-6 text-center select-none"
        dir="rtl"
      >
        <div className="w-20 h-20 rounded-full bg-sky-100 dark:bg-sky-950/60 flex items-center justify-center text-sky-500 mb-4 shadow-inner">
          <Send className="w-10 h-10 -rotate-45 ml-1 mt-0.5 fill-sky-500/20" />
        </div>
        <h3 className="text-lg font-bold text-slate-800 dark:text-slate-200">
          اختر محادثة لبدء المراسلة
        </h3>
        <p className="text-xs text-slate-400 mt-1 max-w-sm">
          تطبيق تيليجرام متصل بالخادم الرسمي عبر بروتوكول MTProto المشفر.
        </p>
      </div>
    );
  }

  const handleSend = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!inputText.trim()) return;

    if (editingMessage) {
      if (onEditMessage) {
        onEditMessage(editingMessage.id, inputText.trim());
      }
      setEditingMessage(null);
      setInputText("");
      showToast("تم تعديل الرسالة بنجاح");
      return;
    }

    onSendMessage(inputText.trim(), replyingTo ? replyingTo.id : undefined);
    setInputText("");
    setReplyingTo(null);
    setShowMediaPicker(false);
    setShowAttachmentSheet(false);
    if (inputRef.current) {
      inputRef.current.style.height = "auto";
      inputRef.current.focus();
    }
  };

  const handleSendSticker = (sticker: { name: string; url: string; emoji: string }) => {
    onSendMessage("", replyingTo ? replyingTo.id : undefined, {
      type: "sticker",
      url: sticker.url,
      fileName: sticker.name,
      emoji: sticker.emoji,
    });
    setReplyingTo(null);
    setShowMediaPicker(false);
  };

  const handleSendGif = (gif: { title: string; url: string }) => {
    onSendMessage("", replyingTo ? replyingTo.id : undefined, {
      type: "video",
      url: gif.url,
      fileName: gif.title,
    });
    setReplyingTo(null);
    setShowMediaPicker(false);
  };

  const handleSendAttachmentMedia = (media: TelegramMedia, caption?: string) => {
    onSendMessage(caption || "", replyingTo ? replyingTo.id : undefined, media);
    setReplyingTo(null);
    setShowAttachmentSheet(false);
    if (inputRef.current) {
      inputRef.current.focus();
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    } else if (e.key === "Escape") {
      if (editingMessage) handleCancelEdit();
      if (replyingTo) setReplyingTo(null);
      if (isSelectionMode) handleCancelSelection();
    }
  };

  const formatMessageTime = (timestamp?: number | string | Date) => {
    if (!timestamp) return "";
    let date: Date;
    if (timestamp instanceof Date) {
      date = timestamp;
    } else if (typeof timestamp === "number") {
      date = new Date(timestamp > 1e11 ? timestamp : timestamp * 1000);
    } else {
      const parsed = Date.parse(String(timestamp));
      date = isNaN(parsed) ? new Date() : new Date(parsed);
    }
    return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
  };

  const handleTranslateMessage = async (msgId: string | number, text: string) => {
    if (translationsMap[msgId]) {
      setTranslationsMap((prev) => {
        const copy = { ...prev };
        delete copy[msgId];
        return copy;
      });
      return;
    }

    setTranslatingMsgIds((prev) => [...prev, msgId]);
    try {
      const translated = await translateMessageText(text);
      setTranslationsMap((prev) => ({ ...prev, [msgId]: translated }));
    } catch {
    } finally {
      setTranslatingMsgIds((prev) => prev.filter((id) => id !== msgId));
    }
  };

  const handleTranscribeVoice = async (msgId: string | number) => {
    if (transcriptionsMap[msgId]) {
      setTranscriptionsMap((prev) => {
        const copy = { ...prev };
        delete copy[msgId];
        return copy;
      });
      return;
    }

    setTranscribingMsgIds((prev) => [...prev, msgId]);
    try {
      const text = await transcribeVoiceAudio(15);
      setTranscriptionsMap((prev) => ({ ...prev, [msgId]: text }));
    } catch {
    } finally {
      setTranscribingMsgIds((prev) => prev.filter((id) => id !== msgId));
    }
  };

  // Pinned message
  const pinnedMessage = messages.find((m) => m.pinned);

  // Search filtered matches
  const searchMatches = chatSearchQuery.trim()
    ? messages.filter((m) => m.text?.toLowerCase().includes(chatSearchQuery.toLowerCase()))
    : [];

  const handleNavigateSearchMatch = (direction: "next" | "prev") => {
    if (searchMatches.length === 0) return;
    let nextIdx = direction === "next" ? searchMatchIndex + 1 : searchMatchIndex - 1;
    if (nextIdx >= searchMatches.length) nextIdx = 0;
    if (nextIdx < 0) nextIdx = searchMatches.length - 1;
    setSearchMatchIndex(nextIdx);
    handleJumpToMessage(searchMatches[nextIdx].id);
  };

  return (
    <div
      id="telegram-chat-view"
      className="flex-1 h-full flex flex-col bg-slate-100 dark:bg-[#0e1621] relative overflow-hidden select-none"
      dir="rtl"
    >
      {/* Toast Notification for Copy/Actions */}
      {copyToast && (
        <div
          id="chat-copy-toast"
          className="absolute top-16 left-1/2 -translate-x-1/2 z-50 bg-[#2481cc] text-white text-xs font-bold px-4 py-2 rounded-xl shadow-2xl flex items-center gap-2 animate-fade-in"
        >
          <Check className="w-4 h-4 text-emerald-300" />
          <span>{copyToast}</span>
        </div>
      )}

      {/* Top Header Bar / Multi-Select Bar */}
      {isSelectionMode ? (
        /* Multi-selection Action Header (Telegram Standard) */
        <div
          id="chat-multi-select-header"
          className="h-14 px-4 bg-[#2481cc] text-white flex items-center justify-between z-20 shrink-0 shadow-md animate-fade-in"
        >
          <div className="flex items-center gap-3">
            <button
              onClick={handleCancelSelection}
              className="w-8 h-8 rounded-full flex items-center justify-center hover:bg-white/20 transition-colors"
              title="إلغاء التحديد"
            >
              <X className="w-5 h-5" />
            </button>
            <span className="font-bold text-sm">
              تم تحديد {selectedMsgIds.length} من الرسائل
            </span>
          </div>

          <div className="flex items-center gap-1 sm:gap-2">
            <button
              onClick={handleSelectAll}
              className="px-2.5 py-1 text-xs bg-white/15 hover:bg-white/25 rounded-lg transition-colors"
            >
              تحديد الكل
            </button>

            {onOpenForward && (
              <button
                onClick={handleForwardSelected}
                className="p-2 hover:bg-white/20 rounded-full transition-colors"
                title="إعادة توجيه"
              >
                <CornerUpRight className="w-5 h-5 -scale-x-100" />
              </button>
            )}

            {onToggleStar && (
              <button
                onClick={handleStarSelected}
                className="p-2 hover:bg-white/20 rounded-full transition-colors"
                title="تمييز بنجمة"
              >
                <Star className="w-5 h-5" />
              </button>
            )}

            <button
              onClick={() => {
                const selectedTexts = messages
                  .filter((m) => selectedMsgIds.includes(m.id) && m.text)
                  .map((m) => m.text)
                  .join("\n\n");
                if (selectedTexts) {
                  navigator.clipboard?.writeText(selectedTexts);
                  showToast("تم نسخ الرسائل المحددة");
                  handleCancelSelection();
                }
              }}
              className="p-2 hover:bg-white/20 rounded-full transition-colors"
              title="نسخ النصوص"
            >
              <Copy className="w-5 h-5" />
            </button>

            <button
              onClick={handleDeleteSelected}
              className="p-2 hover:bg-red-500/80 rounded-full transition-colors text-red-100 hover:text-white"
              title="حذف المحدد"
            >
              <Trash2 className="w-5 h-5" />
            </button>
          </div>
        </div>
      ) : (
        /* Regular Chat Header */
        <div
          id="chat-header-bar"
          className="h-14 px-3 sm:px-4 bg-white/95 dark:bg-[#17212b]/95 backdrop-blur-md border-b border-slate-200 dark:border-slate-800 flex items-center justify-between z-20 shrink-0 gap-2"
        >
          {/* Left Section: Back button + Chat Avatar & Info */}
          <div className="flex items-center gap-2 min-w-0 flex-1">
            {onBack && (
              <button
                id="chat-header-back-btn"
                onClick={(e) => {
                  e.stopPropagation();
                  onBack();
                }}
                className="md:hidden w-8 h-8 rounded-full flex items-center justify-center text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors shrink-0"
                title="الرجوع إلى قائمة المحادثات"
              >
                <ArrowRight className="w-5 h-5" />
              </button>
            )}

            <div
              onClick={onToggleChatInfo}
              className="flex items-center gap-2.5 sm:gap-3 cursor-pointer hover:opacity-90 transition-opacity min-w-0 flex-1"
            >
              <TelegramAvatar
                id={String(dialog.id)}
                name={dialog.title || dialog.name}
                photoUrl={dialog.photoUrl}
                type={dialog.type}
                size="md"
                isOnline={dialog.isOnline}
                showSpecialIcon={true}
              />

              <div className="min-w-0">
                <div className="flex items-center gap-1.5">
                  <h2 className="text-xs sm:text-sm font-bold text-slate-800 dark:text-slate-100 truncate">
                    {dialog.title || dialog.name}
                  </h2>
                  {dialog.isVerified && (
                    <span className="text-sky-500 text-[10px] font-bold">✓</span>
                  )}
                  {dialog.isLocked && (
                    <span className="px-1.5 py-0.2 bg-amber-500/20 text-amber-500 rounded text-[9px] font-bold flex items-center gap-0.5">
                      <Lock className="w-2.5 h-2.5" /> مقفلة
                    </span>
                  )}
                </div>
                <p className="text-[11px] text-slate-400 truncate flex items-center gap-1.5">
                  {dialog.isOnline ? (
                    <span className="text-emerald-500 font-semibold">متصل الآن</span>
                  ) : dialog.type === "channel" ? (
                    <span>{dialog.memberCount ? `${dialog.memberCount.toLocaleString()} مشترك` : "قناة عامة"}</span>
                  ) : dialog.type === "group" ? (
                    <span>{dialog.memberCount ? `${dialog.memberCount} عضو` : "مجموعة"}</span>
                  ) : dialog.type === "bot" ? (
                    <span className="text-sky-500">روبوت تيليجرام</span>
                  ) : (
                    <span>آخر ظهور قريباً</span>
                  )}
                </p>
              </div>
            </div>
          </div>

          {/* Right Header Actions */}
          <div className="flex items-center gap-1">
            {/* Search in Chat Button */}
            <button
              id="chat-header-search-btn"
              onClick={() => setIsSearchingInChat(!isSearchingInChat)}
              className={`p-2 rounded-full transition-colors ${
                isSearchingInChat
                  ? "bg-sky-500/15 text-[#2481cc]"
                  : "text-slate-500 dark:text-slate-400 hover:text-sky-500 hover:bg-slate-100 dark:hover:bg-slate-800"
              }`}
              title="بحث في المحادثة"
            >
              <Search className="w-4 h-4" />
            </button>

            {/* Selection Mode Button */}
            <button
              id="chat-header-select-mode-btn"
              onClick={() => {
                setIsSelectionMode(true);
                setSelectedMsgIds([]);
              }}
              className="p-2 text-slate-500 dark:text-slate-400 hover:text-sky-500 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-full transition-colors hidden sm:flex"
              title="وضع التحديد المتعدد"
            >
              <CheckSquare className="w-4 h-4" />
            </button>

            {/* Lock/Unlock chat */}
            {onToggleLockChat && (
              <button
                onClick={() => onToggleLockChat(String(dialog.id))}
                className={`p-2 rounded-full transition-colors ${
                  dialog.isLocked
                    ? "text-amber-500 hover:bg-amber-500/10"
                    : "text-slate-400 hover:text-amber-500 hover:bg-slate-100 dark:hover:bg-slate-800"
                }`}
                title={dialog.isLocked ? "إلغاء قفل المحادثة" : "قفل المحادثة برمز PIN"}
              >
                {dialog.isLocked ? <Lock className="w-4 h-4" /> : <Unlock className="w-4 h-4" />}
              </button>
            )}

            {/* Analytics button for channels */}
            {dialog.type === "channel" && onOpenAnalytics && (
              <button
                onClick={() => onOpenAnalytics(dialog)}
                className="p-2 text-slate-500 dark:text-slate-400 hover:text-sky-500 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-full transition-colors"
                title="إحصائيات القناة"
              >
                <BarChart3 className="w-4 h-4" />
              </button>
            )}

            {/* Chat Info Drawer Toggle */}
            <button
              id="chat-header-info-btn"
              onClick={onToggleChatInfo}
              className="p-2 text-slate-500 dark:text-slate-400 hover:text-sky-500 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-full transition-colors"
              title="معلومات المحادثة"
            >
              <Info className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* In-Chat Search Bar */}
      {isSearchingInChat && (
        <div
          id="chat-search-bar-inline"
          className="px-4 py-2 bg-white dark:bg-[#17212b] border-b border-slate-200 dark:border-slate-800 flex items-center justify-between gap-3 z-20 animate-slide-down"
        >
          <div className="flex-1 flex items-center gap-2 bg-slate-100 dark:bg-[#242f3d] px-3 py-1.5 rounded-xl">
            <Search className="w-4 h-4 text-slate-400" />
            <input
              type="text"
              autoFocus
              value={chatSearchQuery}
              onChange={(e) => {
                setChatSearchQuery(e.target.value);
                setSearchMatchIndex(0);
              }}
              placeholder="ابحث في الرسائل..."
              className="bg-transparent text-xs text-slate-800 dark:text-slate-100 focus:outline-none flex-1"
            />
            {chatSearchQuery && (
              <button onClick={() => setChatSearchQuery("")} className="text-slate-400 hover:text-slate-600">
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>

          <div className="flex items-center gap-2 text-xs">
            {searchMatches.length > 0 ? (
              <span className="text-slate-500 font-semibold">
                {searchMatchIndex + 1} من {searchMatches.length}
              </span>
            ) : chatSearchQuery ? (
              <span className="text-slate-400">لا توجد نتائج</span>
            ) : null}

            <button
              onClick={() => handleNavigateSearchMatch("prev")}
              disabled={searchMatches.length === 0}
              className="p-1 rounded hover:bg-slate-100 dark:hover:bg-slate-800 disabled:opacity-30"
              title="السابق"
            >
              <ChevronUp className="w-4 h-4" />
            </button>
            <button
              onClick={() => handleNavigateSearchMatch("next")}
              disabled={searchMatches.length === 0}
              className="p-1 rounded hover:bg-slate-100 dark:hover:bg-slate-800 disabled:opacity-30"
              title="التالي"
            >
              <ChevronDown className="w-4 h-4" />
            </button>

            <button
              onClick={() => {
                setIsSearchingInChat(false);
                setChatSearchQuery("");
              }}
              className="p-1 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* Pinned Message Bar */}
      {pinnedMessage && (
        <div
          id="chat-pinned-message-bar"
          onClick={() => handleJumpToMessage(pinnedMessage.id)}
          className="h-10 px-4 bg-white/95 dark:bg-[#17212b]/95 backdrop-blur-md border-b border-slate-200/80 dark:border-slate-800/80 flex items-center justify-between text-xs cursor-pointer hover:bg-slate-50 dark:hover:bg-[#202b36] transition-colors z-10 shrink-0"
        >
          <div className="flex items-center gap-2.5 min-w-0 border-r-2 border-[#2481cc] pr-2">
            <Pin className="w-3.5 h-3.5 text-[#2481cc] shrink-0" />
            <div className="min-w-0">
              <span className="font-bold text-[#2481cc] block text-[10px]">
                رسالة مثبتة
              </span>
              <p className="text-slate-600 dark:text-slate-300 truncate text-[11px]">
                {pinnedMessage.text || (pinnedMessage.media ? `[${pinnedMessage.media.type}]` : "رسالة")}
              </p>
            </div>
          </div>
          <button
            onClick={(e) => {
              e.stopPropagation();
              onTogglePin(pinnedMessage.id);
            }}
            className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-1 rounded-full hover:bg-slate-200/50 dark:hover:bg-slate-700/50"
            title="إلغاء التثبيت"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {/* Messages Scroll Area */}
      <div
        id="telegram-messages-container"
        ref={messagesContainerRef}
        onScroll={handleScroll}
        className="flex-1 overflow-y-auto px-3 sm:px-6 py-4 space-y-3 relative"
      >
        {messages.length === 0 ? (
          <div className="h-full flex flex-col items-center justify-center text-center p-6 text-slate-400">
            <div className="w-16 h-16 rounded-full bg-slate-200/60 dark:bg-slate-800/60 flex items-center justify-center mb-3 text-slate-400">
              <MessageSquare className="w-8 h-8" />
            </div>
            <p className="text-xs font-semibold">لا توجد رسائل بعد في هذه المحادثة</p>
            <p className="text-[11px] text-slate-400 mt-1">ابدأ بكتابة رسالة أو إرسال ملصق أو صورة أدناه</p>
          </div>
        ) : (
          messages.map((msg) => {
            const isOut = msg.out || (currentUser && String(msg.senderId) === String(currentUser.id));
            const translated = translationsMap[msg.id] || msg.translatedText;
            const isTranslating = translatingMsgIds.includes(msg.id);
            const voiceTranscript = transcriptionsMap[msg.id] || msg.transcribedVoiceText;
            const isTranscribing = transcribingMsgIds.includes(msg.id);
            const isHighlighted = highlightedMsgId === msg.id;
            const isSelected = selectedMsgIds.includes(msg.id);
            const isSticker = msg.media?.type === "sticker";

            return (
              <div
                key={msg.id}
                id={`chat-msg-${msg.id}`}
                onClick={() => {
                  if (isSelectionMode) {
                    handleToggleSelectMessage(msg.id);
                  }
                }}
                onDoubleClick={() => onReact(msg.id, "❤️")}
                className={`group relative flex items-start gap-2 text-xs transition-all duration-300 ${
                  isOut ? "justify-start flex-row-reverse" : "justify-start"
                } ${isHighlighted ? "scale-102 ring-2 ring-[#2481cc] rounded-2xl bg-sky-50/50 dark:bg-sky-950/30 p-1" : ""} ${
                  isSelected ? "bg-[#2481cc]/15 rounded-2xl p-1" : ""
                }`}
              >
                {/* Selection Checkbox */}
                {isSelectionMode && (
                  <div
                    onClick={(e) => {
                      e.stopPropagation();
                      handleToggleSelectMessage(msg.id);
                    }}
                    className="self-center cursor-pointer p-1"
                  >
                    {isSelected ? (
                      <div className="w-5 h-5 rounded-md bg-[#2481cc] text-white flex items-center justify-center">
                        <Check className="w-3.5 h-3.5" />
                      </div>
                    ) : (
                      <div className="w-5 h-5 rounded-md border-2 border-slate-300 dark:border-slate-600" />
                    )}
                  </div>
                )}

                {/* Avatar for Incoming Messages */}
                {!isOut && (dialog.type as string) !== "user" && dialog.type !== "direct" && dialog.type !== "saved" && (
                  <TelegramAvatar
                    id={String(msg.senderId || 0)}
                    name={msg.senderName}
                    photoUrl={msg.senderPhoto}
                    size="sm"
                    className="shrink-0 mt-1"
                  />
                )}

                {/* Message Bubble */}
                <div
                  className={`relative max-w-[85%] sm:max-w-[70%] transition-shadow ${
                    isSticker
                      ? "bg-transparent p-0 shadow-none"
                      : isOut
                      ? "bg-[#2b5278] dark:bg-[#2b5278] text-white rounded-2xl rounded-tr-xs p-3 shadow-xs"
                      : "bg-white dark:bg-[#182533] text-slate-800 dark:text-slate-100 rounded-2xl rounded-tl-xs p-3 shadow-xs border border-slate-200/50 dark:border-slate-800"
                  }`}
                >
                  {/* Sender Name for group chats */}
                  {!isOut && (dialog.type as string) !== "user" && dialog.type !== "direct" && dialog.type !== "saved" && !isSticker && (
                    <div className="font-bold text-[11px] text-[#2481cc] dark:text-[#5288c1] mb-1">
                      {msg.senderName}
                    </div>
                  )}

                  {/* Starred Badge */}
                  {msg.isStarred && (
                    <span className="absolute -top-1.5 -left-1.5 w-4 h-4 rounded-full bg-amber-400 text-amber-950 flex items-center justify-center text-[10px] shadow-xs">
                      ★
                    </span>
                  )}

                  {/* Pinned Marker Badge */}
                  {msg.pinned && (
                    <div className="flex items-center gap-1 text-[10px] text-sky-300 font-bold mb-1 opacity-90">
                      <Pin className="w-3 h-3" />
                      <span>رسالة مثبتة</span>
                    </div>
                  )}

                  {/* Reply Quote Preview */}
                  {msg.replyToMessage && (
                    <div
                      onClick={() => handleJumpToMessage(msg.replyToMessage!.id)}
                      className={`mb-2 p-2 rounded-lg border-r-3 border-[#2481cc] text-[11px] cursor-pointer transition-colors ${
                        isOut
                          ? "bg-white/10 hover:bg-white/20 text-white"
                          : "bg-slate-100 dark:bg-[#242f3d] hover:bg-slate-200 dark:hover:bg-[#2c3847] text-slate-700 dark:text-slate-200"
                      }`}
                    >
                      <span className="font-bold block text-[10px] text-[#2481cc] dark:text-[#5288c1]">
                        {msg.replyToMessage.senderName}
                      </span>
                      <p className="truncate opacity-90">{msg.replyToMessage.text}</p>
                    </div>
                  )}

                  {/* Forwarded Tag */}
                  {msg.forwardFrom && (
                    <div className="text-[10px] opacity-75 mb-1 flex items-center gap-1 font-semibold">
                      <CornerUpRight className="w-3 h-3 -scale-x-100" />
                      <span>محولة من {msg.forwardFrom}</span>
                    </div>
                  )}

                  {/* Media Content */}
                  {msg.media && (
                    <div className="mb-2">
                      {msg.media.type === "sticker" && (
                        <div className="flex flex-col items-center">
                          <img
                            src={msg.media.url}
                            alt={msg.media.fileName || "Sticker"}
                            className="w-36 h-36 object-contain cursor-pointer hover:scale-105 transition-transform"
                            onClick={() => setLightboxImage(msg.media?.url || null)}
                          />
                          {msg.media.emoji && (
                            <span className="text-sm mt-0.5">{msg.media.emoji}</span>
                          )}
                        </div>
                      )}

                      {msg.media.type === "photo" && (
                        <img
                          src={msg.media.url}
                          alt="Photo"
                          onClick={() => setLightboxImage(msg.media?.url || null)}
                          className="rounded-xl max-h-72 w-full object-cover cursor-pointer hover:opacity-95 transition-opacity"
                        />
                      )}

                      {msg.media.type === "video" && (
                        <div className="rounded-xl overflow-hidden">
                          {msg.media.url?.endsWith(".gif") || msg.media.url?.includes("giphy") ? (
                            <img
                              src={msg.media.url}
                              alt="GIF"
                              className="rounded-xl max-h-72 w-full object-cover"
                            />
                          ) : (
                            <video
                              src={msg.media.url}
                              controls
                              className="rounded-xl max-h-72 w-full bg-black"
                            />
                          )}
                        </div>
                      )}

                      {msg.media.type === "voice" && (
                        <div className="flex flex-col gap-1.5 py-1">
                          <div className="flex items-center gap-2.5">
                            <AudioPlayer
                              url={msg.media.url || ""}
                              duration={msg.media.duration || 15}
                              isOut={isOut}
                            />

                            {/* Audio Playback Speed Toggle */}
                            <button
                              onClick={() => {
                                const speeds = [1, 1.5, 2];
                                const next = speeds[(speeds.indexOf(audioPlaySpeed) + 1) % speeds.length];
                                setAudioPlaySpeed(next);
                              }}
                              className={`px-1.5 py-0.5 rounded text-[9px] font-bold ${
                                isOut ? "bg-white/20 text-white" : "bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-300"
                              }`}
                              title="سرعة التشغيل"
                            >
                              {audioPlaySpeed}x
                            </button>

                            {onPlayAudioTrack && (
                              <button
                                onClick={() =>
                                  onPlayAudioTrack({
                                    url: msg.media?.url || "",
                                    title: `تسجيل صوتي من ${msg.senderName}`,
                                    subtitle: formatMessageTime(msg.date),
                                    duration: msg.media?.duration || 15,
                                    chatId: msg.chatId,
                                    messageId: msg.id,
                                  })
                                }
                                className="px-2 py-1 bg-white/20 hover:bg-white/30 rounded-lg text-[10px] font-semibold"
                                title="تشغيل في المشغل العائم"
                              >
                                مشغل عائم
                              </button>
                            )}
                          </div>

                          {/* Transcribe Voice Button */}
                          <button
                            onClick={() => handleTranscribeVoice(msg.id)}
                            className={`text-[10px] flex items-center gap-1 font-semibold transition-colors mt-1 ${
                              isOut ? "text-sky-100 hover:text-white" : "text-[#2481cc] dark:text-[#5288c1]"
                            }`}
                          >
                            {isTranscribing ? (
                              <>
                                <Loader2 className="w-3 h-3 animate-spin" />
                                <span>جاري تحويل الصوت إلى نص...</span>
                              </>
                            ) : (
                              <>
                                <FileText className="w-3 h-3" />
                                <span>{voiceTranscript ? "إخفاء النص المحول" : "تحويل الصوت إلى نص"}</span>
                              </>
                            )}
                          </button>

                          {/* Transcribed Text Bubble */}
                          {voiceTranscript && (
                            <div
                              className={`p-2 rounded-xl text-[11px] mt-1 border ${
                                isOut
                                  ? "bg-white/15 border-white/20 text-white"
                                  : "bg-slate-100 dark:bg-slate-700/60 border-slate-200 dark:border-slate-600 text-slate-800 dark:text-slate-100"
                              }`}
                            >
                              <span className="font-bold block text-[10px] opacity-80">النص المنطوق:</span>
                              <span>{voiceTranscript}</span>
                            </div>
                          )}
                        </div>
                      )}

                      {msg.media.type === "document" && (
                        <div className="flex items-center gap-2 p-2 bg-black/10 rounded-xl">
                          <FileText className="w-8 h-8 text-sky-300 shrink-0" />
                          <div className="min-w-0 flex-1">
                            <span className="font-bold block truncate">
                              {msg.media.fileName || "Document.pdf"}
                            </span>
                            <span className="text-[10px] opacity-75">
                              {typeof msg.media.fileSize === 'number'
                                ? `${(msg.media.fileSize / (1024 * 1024)).toFixed(1)} MB`
                                : msg.media.fileSize || "1.0 MB"}
                            </span>
                          </div>
                          <button className="p-1.5 rounded-full hover:bg-black/10 transition-colors">
                            <Download className="w-4 h-4" />
                          </button>
                        </div>
                      )}
                    </div>
                  )}

                  {/* Text Content */}
                  {msg.text && (
                    <div className="whitespace-pre-wrap break-words leading-relaxed text-xs sm:text-[13px]">
                      {msg.text}
                    </div>
                  )}

                  {/* Inline Translation Display */}
                  {translated && (
                    <div
                      className={`mt-2 p-2 rounded-xl border text-[11px] ${
                        isOut
                          ? "bg-white/15 border-white/25 text-white"
                          : "bg-sky-50 dark:bg-sky-950/50 border-sky-200 dark:border-sky-900 text-sky-950 dark:text-sky-100"
                      }`}
                    >
                      <div className="flex items-center gap-1 font-bold text-[10px] mb-0.5 opacity-80">
                        <Globe className="w-3 h-3" />
                        <span>الترجمة الفورية:</span>
                      </div>
                      <span>{translated}</span>
                    </div>
                  )}

                  {/* Rich Link Previews */}
                  {msg.text &&
                    msg.text.match(/(https?:\/\/[^\s]+|t\.me\/[^\s]+|tg:\/\/[^\s]+)/g)?.map((url, uIdx) => (
                      <LinkPreviewCard
                        key={uIdx}
                        url={url}
                        isOut={isOut}
                        onOpenInviteModal={(link) => onOpenInviteModal && onOpenInviteModal(link)}
                      />
                    ))}

                  {/* Bottom Meta info (Time + Double checkmark + views + edited) */}
                  <div
                    className={`flex items-center justify-end gap-1 mt-1 text-[10px] ${
                      isOut ? "text-sky-100/90" : "text-slate-400"
                    }`}
                  >
                    {msg.isEdited && <span className="text-[9px] opacity-75">معدلة</span>}
                    {msg.views && (
                      <span className="text-[9px] mr-1 opacity-80">
                        👁 {msg.views.toLocaleString()}
                      </span>
                    )}
                    <span>{formatMessageTime(msg.date)}</span>
                    {isOut && <CheckCheck className="w-3.5 h-3.5 inline text-sky-200" />}
                  </div>

                  {/* Reactions Badge */}
                  {msg.reactions && msg.reactions.length > 0 && (
                    <div className="flex flex-wrap gap-1 mt-1.5">
                      {msg.reactions.map((r, rIdx) => {
                        const emojiChar = (r as any).emoji || (r as any).emoticon || "👍";
                        const isChosen = (r as any).chosen || (r as any).mine || (r as any).isSelected;
                        return (
                          <button
                            key={rIdx}
                            onClick={(e) => {
                              e.stopPropagation();
                              onReact(msg.id, emojiChar);
                            }}
                            className={`px-2 py-0.5 rounded-full text-[11px] flex items-center gap-1 border transition-all ${
                              isChosen
                                ? "bg-[#2481cc]/20 border-[#2481cc] text-[#2481cc] dark:text-[#5288c1] font-bold scale-105"
                                : "bg-black/5 dark:bg-white/10 border-transparent text-slate-700 dark:text-slate-200"
                            }`}
                          >
                            <span>{emojiChar}</span>
                            <span className="text-[10px] font-semibold">{r.count}</span>
                          </button>
                        );
                      })}
                    </div>
                  )}
                </div>

                {/* Quick Action Floating Bar on Hover */}
                {!isSelectionMode && (
                  <div
                    className={`opacity-0 group-hover:opacity-100 transition-opacity flex items-center gap-0.5 bg-white dark:bg-[#182533] shadow-md border border-slate-200 dark:border-slate-700 rounded-full px-1.5 py-0.5 absolute -top-4.5 ${
                      isOut ? "right-2" : "left-2"
                    } z-10`}
                  >
                    {/* Reply Button */}
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleStartReply(msg);
                      }}
                      className="p-1 hover:text-[#2481cc] text-slate-500 rounded transition-colors"
                      title="رد"
                    >
                      <Reply className="w-3.5 h-3.5 -scale-x-100" />
                    </button>

                    {/* Reactions toggle */}
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setActiveReactionMenuMsgId(
                          activeReactionMenuMsgId === msg.id ? null : msg.id
                        );
                      }}
                      className="p-1 hover:text-amber-500 text-slate-500 rounded transition-colors"
                      title="تفاعل"
                    >
                      <Smile className="w-3.5 h-3.5" />
                    </button>

                    {/* Edit Message (if sender) */}
                    {isOut && (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleStartEdit(msg);
                        }}
                        className="p-1 hover:text-[#2481cc] text-slate-500 rounded transition-colors"
                        title="تعديل الرسالة"
                      >
                        <Edit3 className="w-3.5 h-3.5" />
                      </button>
                    )}

                    {/* Copy Text */}
                    {msg.text && (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleCopyMessageText(msg.text);
                        }}
                        className="p-1 hover:text-[#2481cc] text-slate-500 rounded transition-colors"
                        title="نسخ النص"
                      >
                        <Copy className="w-3.5 h-3.5" />
                      </button>
                    )}

                    {/* Pin Message */}
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onTogglePin(msg.id);
                      }}
                      className={`p-1 rounded transition-colors ${
                        msg.pinned
                          ? "text-[#2481cc] fill-sky-500/20"
                          : "hover:text-[#2481cc] text-slate-500"
                      }`}
                      title={msg.pinned ? "إلغاء التثبيت" : "تثبيت"}
                    >
                      <Pin className="w-3.5 h-3.5" />
                    </button>

                    {/* Star / Bookmark */}
                    {onToggleStar && (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          onToggleStar(msg.id);
                        }}
                        className={`p-1 rounded transition-colors ${
                          msg.isStarred
                            ? "text-amber-500"
                            : "hover:text-amber-500 text-slate-500"
                        }`}
                        title={msg.isStarred ? "إلغاء التمييز" : "تمييز بنجمة"}
                      >
                        <Star className="w-3.5 h-3.5" />
                      </button>
                    )}

                    {/* Forward Message */}
                    {onOpenForward && (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          onOpenForward(msg);
                        }}
                        className="p-1 hover:text-[#2481cc] text-slate-500 rounded transition-colors"
                        title="إعادة توجيه"
                      >
                        <CornerUpRight className="w-3.5 h-3.5 -scale-x-100" />
                      </button>
                    )}

                    {/* Delete Message */}
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setDeleteConfirmModal({
                          isOpen: true,
                          msgIds: [msg.id],
                        });
                      }}
                      className="p-1 hover:text-red-500 text-slate-500 rounded transition-colors"
                      title="حذف"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                )}

                {/* Inline Reaction Picker Popover */}
                {activeReactionMenuMsgId === msg.id && (
                  <div
                    className={`absolute bottom-full mb-1 ${
                      isOut ? "right-0" : "left-0"
                    } z-30 bg-white dark:bg-[#182533] border border-slate-200 dark:border-slate-700 shadow-2xl rounded-full px-3 py-1.5 flex items-center gap-1.5 animate-scale-up`}
                  >
                    {QUICK_REACTION_EMOJIS.map((emoji) => (
                      <button
                        key={emoji}
                        onClick={(e) => {
                          e.stopPropagation();
                          onReact(msg.id, emoji);
                          setActiveReactionMenuMsgId(null);
                        }}
                        className="hover:scale-130 transition-transform text-lg p-0.5 active:scale-95"
                      >
                        {emoji}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            );
          })
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Floating Scroll to Bottom Button */}
      {showScrollBottom && (
        <button
          id="chat-scroll-to-bottom-btn"
          onClick={scrollToBottom}
          className="absolute bottom-20 left-6 z-20 w-10 h-10 rounded-full bg-white/90 dark:bg-[#182533]/90 text-slate-600 dark:text-slate-200 shadow-xl border border-slate-200/80 dark:border-slate-700/80 backdrop-blur-md flex items-center justify-center hover:scale-110 active:scale-95 transition-all animate-bounce"
          title="التمرير لأسفل"
        >
          <ChevronDown className="w-5 h-5" />
        </button>
      )}

      {/* Reply Banner Above Input */}
      {replyingTo && (
        <div
          id="chat-reply-banner"
          className="bg-white/95 dark:bg-[#17212b]/95 backdrop-blur-md px-4 py-2 border-t border-slate-200 dark:border-slate-800 flex items-center justify-between text-xs z-20 animate-fade-in"
        >
          <div className="flex items-center gap-2.5 min-w-0 border-r-3 border-[#2481cc] pr-2">
            <Reply className="w-4 h-4 text-[#2481cc] shrink-0 -scale-x-100" />
            <div className="min-w-0">
              <span className="font-bold text-[#2481cc] block text-[10px]">
                الرد على {replyingTo.senderName}
              </span>
              <p className="text-slate-600 dark:text-slate-300 truncate text-xs">
                {replyingTo.text || (replyingTo.media?.caption ? `[${replyingTo.media.type}] ${replyingTo.media.caption}` : "[وسائط]")}
              </p>
            </div>
          </div>
          <button
            onClick={() => setReplyingTo(null)}
            className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-1 rounded-full hover:bg-slate-100 dark:hover:bg-slate-800"
            title="إلغاء الرد (Esc)"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Edit Message Banner Above Input */}
      {editingMessage && (
        <div
          id="chat-edit-banner"
          className="bg-white/95 dark:bg-[#17212b]/95 backdrop-blur-md px-4 py-2 border-t border-amber-500/30 flex items-center justify-between text-xs z-20 animate-fade-in"
        >
          <div className="flex items-center gap-2.5 min-w-0 border-r-3 border-amber-500 pr-2">
            <Edit3 className="w-4 h-4 text-amber-500 shrink-0" />
            <div className="min-w-0">
              <span className="font-bold text-amber-500 block text-[10px]">
                تعديل الرسالة
              </span>
              <p className="text-slate-600 dark:text-slate-300 truncate text-xs">
                {editingMessage.text}
              </p>
            </div>
          </div>
          <button
            onClick={handleCancelEdit}
            className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-1 rounded-full hover:bg-slate-100 dark:hover:bg-slate-800"
            title="إلغاء التعديل (Esc)"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Tabbed Stickers, Emojis & GIFs Panel (Telegram Standard) */}
      {showMediaPicker && (
        <div
          id="chat-media-picker-container"
          className="bg-white dark:bg-[#17212b] border-t border-slate-200 dark:border-slate-800 shadow-2xl z-20 flex flex-col h-72 animate-slide-up"
        >
          {/* Picker Navigation Tabs */}
          <div className="flex items-center justify-between px-3 border-b border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-[#182533]">
            <div className="flex items-center gap-2 text-xs font-bold">
              <button
                onClick={() => setMediaPickerTab("emoji")}
                className={`py-2 px-3 flex items-center gap-1.5 border-b-2 transition-all ${
                  mediaPickerTab === "emoji"
                    ? "border-[#2481cc] text-[#2481cc] dark:text-[#5288c1]"
                    : "border-transparent text-slate-500 hover:text-slate-800"
                }`}
              >
                <Smile className="w-4 h-4" />
                <span>الرموز التعبيرية</span>
              </button>

              <button
                onClick={() => setMediaPickerTab("stickers")}
                className={`py-2 px-3 flex items-center gap-1.5 border-b-2 transition-all ${
                  mediaPickerTab === "stickers"
                    ? "border-[#2481cc] text-[#2481cc] dark:text-[#5288c1]"
                    : "border-transparent text-slate-500 hover:text-slate-800"
                }`}
              >
                <StickerIcon className="w-4 h-4" />
                <span>الملصقات</span>
              </button>

              <button
                onClick={() => setMediaPickerTab("gifs")}
                className={`py-2 px-3 flex items-center gap-1.5 border-b-2 transition-all ${
                  mediaPickerTab === "gifs"
                    ? "border-[#2481cc] text-[#2481cc] dark:text-[#5288c1]"
                    : "border-transparent text-slate-500 hover:text-slate-800"
                }`}
              >
                <Film className="w-4 h-4" />
                <span>صور متحركة (GIF)</span>
              </button>
            </div>

            <button
              onClick={() => setShowMediaPicker(false)}
              className="text-slate-400 hover:text-slate-600 p-1.5 rounded-full"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          {/* Tab Content */}
          <div className="flex-1 overflow-y-auto p-3">
            {mediaPickerTab === "emoji" && (
              <div className="space-y-4">
                {EMOJI_CATEGORIES.map((cat, cIdx) => (
                  <div key={cIdx}>
                    <h5 className="text-[11px] font-bold text-slate-400 mb-2">{cat.name}</h5>
                    <div className="grid grid-cols-8 sm:grid-cols-12 gap-1 text-2xl">
                      {cat.emojis.map((emoji, eIdx) => (
                        <button
                          key={eIdx}
                          onClick={() => {
                            setInputText((prev) => prev + emoji);
                            if (inputRef.current) inputRef.current.focus();
                          }}
                          className="hover:scale-130 transition-transform p-1.5 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 flex items-center justify-center"
                        >
                          {emoji}
                        </button>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            )}

            {mediaPickerTab === "stickers" && (
              <div className="flex flex-col h-full">
                {/* Packs Bar */}
                <div className="flex items-center gap-2 pb-2 border-b border-slate-100 dark:border-slate-800 overflow-x-auto no-scrollbar mb-3">
                  {TELEGRAM_STICKER_PACKS.map((pack) => (
                    <button
                      key={pack.id}
                      onClick={() => setSelectedStickerPackId(pack.id)}
                      className={`px-3 py-1 rounded-full text-xs font-bold flex items-center gap-1.5 shrink-0 transition-colors ${
                        selectedStickerPackId === pack.id
                          ? "bg-[#2481cc] text-white shadow-xs"
                          : "bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-200"
                      }`}
                    >
                      <span>{pack.icon}</span>
                      <span>{pack.title}</span>
                    </button>
                  ))}
                </div>

                {/* Stickers Grid */}
                <div className="grid grid-cols-3 sm:grid-cols-6 gap-3 overflow-y-auto flex-1 p-1">
                  {TELEGRAM_STICKER_PACKS.find((p) => p.id === selectedStickerPackId)?.stickers.map(
                    (sticker) => (
                      <button
                        key={sticker.id}
                        onClick={() => handleSendSticker(sticker)}
                        className="group flex flex-col items-center p-2 rounded-2xl hover:bg-slate-100 dark:hover:bg-slate-800/80 transition-all hover:scale-105"
                      >
                        <img
                          src={sticker.url}
                          alt={sticker.name}
                          className="w-20 h-20 object-contain rounded-xl"
                        />
                        <span className="text-[10px] text-slate-500 group-hover:text-slate-800 dark:group-hover:text-slate-200 mt-1">
                          {sticker.name} {sticker.emoji}
                        </span>
                      </button>
                    )
                  )}
                </div>
              </div>
            )}

            {mediaPickerTab === "gifs" && (
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 overflow-y-auto p-1">
                {TELEGRAM_GIFS.map((gif) => (
                  <button
                    key={gif.id}
                    onClick={() => handleSendGif(gif)}
                    className="group relative rounded-xl overflow-hidden aspect-video bg-black/10 hover:opacity-95 transition-opacity"
                  >
                    <img
                      src={gif.url}
                      alt={gif.title}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform"
                    />
                    <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent flex items-end p-2">
                      <span className="text-white text-xs font-bold">{gif.title}</span>
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Message Input Bar (DrKLO/Telegram Android standard) */}
      <div
        id="chat-input-bar-container"
        className="p-3 bg-white dark:bg-[#17212b] border-t border-slate-200 dark:border-slate-800 flex items-end gap-2 relative z-20 shrink-0"
      >
        {/* Attachment Sheet Menu */}
        <AttachmentSheet
          isOpen={showAttachmentSheet}
          onClose={() => setShowAttachmentSheet(false)}
          onSendMedia={handleSendAttachmentMedia}
        />

        {isRecordingVoice ? (
          /* Voice Recording Mode */
          <div className="flex-1 flex items-center justify-between bg-red-50 dark:bg-red-950/40 rounded-full px-4 py-2 border border-red-200 dark:border-red-900/50">
            <div className="flex items-center gap-2 text-red-500 font-bold text-xs">
              <span className="w-2.5 h-2.5 rounded-full bg-red-500 animate-pulse" />
              <span>جاري تسجيل رسالة صوتية... {recordSeconds}s</span>
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setIsRecordingVoice(false)}
                className="text-slate-400 hover:text-slate-600 text-xs px-2 py-1 font-semibold"
              >
                إلغاء
              </button>
              <button
                onClick={() => {
                  setIsRecordingVoice(false);
                  onSendMessage("🎙️ رسالة صوتية مسجلة", replyingTo ? replyingTo.id : undefined, {
                    type: "voice",
                    duration: recordSeconds || 5,
                    url: "https://actions.google.com/sounds/v1/water/rain_heavy.ogg",
                  });
                }}
                className="w-8 h-8 rounded-full bg-[#2481cc] text-white flex items-center justify-center shadow-md"
              >
                <Send className="w-4 h-4 -rotate-45 ml-0.5 fill-current" />
              </button>
            </div>
          </div>
        ) : (
          /* Standard Text & Media Input */
          <>
            {/* Paperclip Button */}
            <button
              id="chat-attach-file-btn"
              type="button"
              onClick={() => setShowAttachmentSheet(!showAttachmentSheet)}
              className={`p-2.5 rounded-full transition-all shrink-0 ${
                showAttachmentSheet
                  ? "bg-[#2481cc] text-white shadow-md shadow-sky-500/20"
                  : "text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800"
              }`}
              title="إرفاق ملف، صورة، فيديو، استطلاع رأي، أو موقع"
            >
              <Paperclip className="w-5 h-5 -rotate-45" />
            </button>

            {/* Expanding Textarea Box */}
            <div className="flex-1 bg-slate-100 dark:bg-[#242f3d] rounded-2xl px-3.5 py-1.5 flex items-center gap-2 border border-transparent focus-within:border-[#2481cc] transition-colors min-h-[44px]">
              <textarea
                id="chat-message-textarea"
                ref={inputRef}
                rows={1}
                value={inputText}
                onChange={(e) => {
                  setInputText(e.target.value);
                  e.target.style.height = "auto";
                  e.target.style.height = `${Math.min(e.target.scrollHeight, 120)}px`;
                }}
                onKeyDown={handleKeyDown}
                placeholder={editingMessage ? "تعديل الرسالة..." : "اكتب رسالة..."}
                className="flex-1 bg-transparent text-xs sm:text-sm text-slate-800 dark:text-slate-100 placeholder-slate-400 focus:outline-none resize-none max-h-28 py-1.5 leading-relaxed"
              />

              <button
                id="chat-emoji-toggle-btn"
                type="button"
                onClick={() => setShowMediaPicker(!showMediaPicker)}
                className={`p-1.5 rounded-full transition-colors ${
                  showMediaPicker
                    ? "text-[#2481cc] bg-[#2481cc]/10"
                    : "text-slate-400 hover:text-amber-500 hover:bg-slate-200/50 dark:hover:bg-slate-700/50"
                }`}
                title="رموز وملصقات وصور متحركة"
              >
                <Smile className="w-5 h-5" />
              </button>
            </div>

            {/* Send or Voice Record Button */}
            {inputText.trim() || editingMessage ? (
              <button
                id="chat-send-message-btn"
                onClick={() => handleSend()}
                className="w-11 h-11 rounded-full bg-[#2481cc] hover:bg-[#1d6fa5] text-white flex items-center justify-center shadow-lg shadow-sky-500/25 transition-all hover:scale-105 active:scale-95 shrink-0"
                title={editingMessage ? "حفظ التعديل (Enter)" : "إرسال (Enter)"}
              >
                {editingMessage ? (
                  <Check className="w-5 h-5" />
                ) : (
                  <Send className="w-5 h-5 -rotate-45 ml-0.5 mt-0.5 fill-current" />
                )}
              </button>
            ) : (
              <button
                id="chat-voice-record-btn"
                onClick={() => setIsRecordingVoice(true)}
                className="w-11 h-11 rounded-full bg-slate-100 dark:bg-[#242f3d] text-slate-600 dark:text-slate-300 hover:text-[#2481cc] hover:bg-sky-50 dark:hover:bg-sky-950 flex items-center justify-center transition-all hover:scale-105 active:scale-95 shrink-0"
                title="تسجيل رسالة صوتية"
              >
                <Mic className="w-5 h-5" />
              </button>
            )}
          </>
        )}
      </div>

      {/* Lightbox Photo Preview Modal */}
      {lightboxImage && (
        <div
          id="photo-lightbox-modal"
          onClick={() => setLightboxImage(null)}
          className="fixed inset-0 z-50 bg-black/90 backdrop-blur-md flex items-center justify-center p-4 animate-fade-in"
        >
          <button
            onClick={() => setLightboxImage(null)}
            className="absolute top-4 right-4 text-white hover:text-slate-300 p-2 rounded-full hover:bg-white/10"
          >
            <X className="w-6 h-6" />
          </button>
          <img
            src={lightboxImage}
            alt="Preview"
            className="max-w-full max-h-[85vh] object-contain rounded-2xl shadow-2xl"
          />
        </div>
      )}

      {/* Delete Confirmation Modal (Telegram Standard) */}
      {deleteConfirmModal.isOpen && (
        <div
          id="delete-confirm-modal-overlay"
          onClick={() => setDeleteConfirmModal({ isOpen: false, msgIds: [] })}
          className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 animate-fade-in"
          dir="rtl"
        >
          <div
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-sm bg-white dark:bg-[#182533] rounded-2xl p-5 shadow-2xl border border-slate-200 dark:border-slate-800 text-slate-800 dark:text-slate-100 animate-scale-up"
          >
            <h3 className="text-sm font-bold mb-2">
              حذف {deleteConfirmModal.msgIds.length > 1 ? `${deleteConfirmModal.msgIds.length} من الرسائل` : "الرسالة"}؟
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mb-5 leading-relaxed">
              هل أنت متأكد من رغبتك في حذف {deleteConfirmModal.msgIds.length > 1 ? "هذه الرسائل المحددة" : "هذه الرسالة"}؟ لا يمكن التراجع عن هذا الإجراء.
            </p>

            <div className="flex flex-col gap-2">
              <button
                onClick={() => handleConfirmDelete(true)}
                className="w-full py-2.5 bg-red-500 hover:bg-red-600 text-white rounded-xl text-xs font-bold transition-colors shadow-xs"
              >
                حذف لدى الجميع
              </button>

              <button
                onClick={() => handleConfirmDelete(false)}
                className="w-full py-2.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 rounded-xl text-xs font-semibold transition-colors"
              >
                حذف لدي فقط
              </button>

              <button
                onClick={() => setDeleteConfirmModal({ isOpen: false, msgIds: [] })}
                className="w-full py-2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-300 text-xs font-semibold transition-colors mt-1"
              >
                إلغاء
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
