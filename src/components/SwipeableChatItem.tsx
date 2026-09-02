import React, { useState, useRef } from "react";
import {
  Archive,
  ArchiveRestore,
  Volume2,
  VolumeX,
  Pin,
  Trash2,
  CheckCheck,
  Mail,
  GripVertical,
  Lock,
} from "lucide-react";
import { TelegramDialog } from "../types";
import { TelegramAvatar } from "./TelegramAvatar";

interface SwipeableChatItemProps {
  dialog: TelegramDialog;
  isSelected: boolean;
  onSelectChat: (chatId: string) => void;
  onArchive?: (chatId: string) => void;
  onToggleMute?: (chatId: string) => void;
  onTogglePin?: (chatId: string) => void;
  onDelete?: (chatId: string) => void;
  onToggleRead?: (chatId: string) => void;
  // Drag & Drop props
  index: number;
  onDragStartItem: (e: React.DragEvent, index: number) => void;
  onDragOverItem: (e: React.DragEvent, index: number) => void;
  onDragEndItem: () => void;
  isDraggingCurrent: boolean;
  isDragOverCurrent: boolean;
}

export const SwipeableChatItem: React.FC<SwipeableChatItemProps> = ({
  dialog,
  isSelected,
  onSelectChat,
  onArchive,
  onToggleMute,
  onTogglePin,
  onDelete,
  onToggleRead,
  index,
  onDragStartItem,
  onDragOverItem,
  onDragEndItem,
  isDraggingCurrent,
  isDragOverCurrent,
}) => {
  const [offsetX, setOffsetX] = useState(0);
  const [isSwiping, setIsSwiping] = useState(false);
  const startXRef = useRef(0);
  const startYRef = useRef(0);
  const isHorizontalSwipeRef = useRef<boolean | null>(null);

  // Format last message time
  const formatTime = (timestamp?: number | string) => {
    if (!timestamp) return "";
    if (typeof timestamp === 'string') {
      const parsed = Date.parse(timestamp);
      if (isNaN(parsed)) return timestamp;
      const d = new Date(parsed);
      return d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
    }
    const date = new Date(timestamp < 1e11 ? timestamp * 1000 : timestamp);
    const now = new Date();
    const isToday = date.toDateString() === now.toDateString();
    if (isToday) {
      return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
    }
    return date.toLocaleDateString([], { month: "numeric", day: "numeric" });
  };

  // Touch Handlers
  const handleTouchStart = (e: React.TouchEvent) => {
    startXRef.current = e.touches[0].clientX;
    startYRef.current = e.touches[0].clientY;
    isHorizontalSwipeRef.current = null;
    setIsSwiping(true);
  };

  const handleTouchMove = (e: React.TouchEvent) => {
    if (!isSwiping) return;
    const currentX = e.touches[0].clientX;
    const currentY = e.touches[0].clientY;
    const diffX = currentX - startXRef.current;
    const diffY = currentY - startYRef.current;

    // Determine direction on initial move
    if (isHorizontalSwipeRef.current === null) {
      if (Math.abs(diffX) > 8 || Math.abs(diffY) > 8) {
        isHorizontalSwipeRef.current = Math.abs(diffX) > Math.abs(diffY);
      }
    }

    if (isHorizontalSwipeRef.current) {
      // Clamp swipe distance
      const clamped = Math.max(-140, Math.min(140, diffX));
      setOffsetX(clamped);
    }
  };

  const handleTouchEnd = () => {
    setIsSwiping(false);
    // Snap back or keep open if swiped significantly
    if (Math.abs(offsetX) > 70) {
      // Auto trigger if swiped far enough, or keep open
      if (offsetX > 110 && onArchive) {
        onArchive(String(dialog.id));
        setOffsetX(0);
        return;
      } else if (offsetX < -110 && onDelete) {
        onDelete(String(dialog.id));
        setOffsetX(0);
        return;
      }
    }
    // Snap back with smooth animation
    setOffsetX(0);
  };

  return (
    <div
      id={`sidebar-dialog-container-${dialog.id}`}
      draggable
      onDragStart={(e) => onDragStartItem(e, index)}
      onDragOver={(e) => onDragOverItem(e, index)}
      onDragEnd={onDragEndItem}
      className={`relative overflow-hidden transition-all select-none my-0.5 rounded-xl ${
        isDraggingCurrent ? "opacity-40 scale-95 border-2 border-dashed border-sky-500" : ""
      } ${isDragOverCurrent ? "border-t-2 border-sky-500" : ""}`}
    >
      {/* Background Quick Action Buttons revealed on Swipe */}
      <div className="absolute inset-0 flex items-center justify-between pointer-events-auto z-0 px-2">
        {/* Left Actions (revealed when swiped right) */}
        <div className="flex items-center gap-1">
          {onArchive && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                onArchive(String(dialog.id));
                setOffsetX(0);
              }}
              className="w-10 h-10 rounded-xl bg-[#2481cc] text-white flex flex-col items-center justify-center text-[10px] shadow-sm hover:brightness-110 active:scale-95 transition-all"
              title={dialog.archived ? "إلغاء الأرشفة" : "أرشفة المحادثة"}
            >
              {dialog.archived ? <ArchiveRestore className="w-4 h-4" /> : <Archive className="w-4 h-4" />}
            </button>
          )}

          {onTogglePin && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                onTogglePin(String(dialog.id));
                setOffsetX(0);
              }}
              className="w-10 h-10 rounded-xl bg-sky-500 text-white flex flex-col items-center justify-center text-[10px] shadow-sm hover:brightness-110 active:scale-95 transition-all"
              title={dialog.pinned ? "إلغاء التثبيت" : "تثبيت في الأعلى"}
            >
              <Pin className="w-4 h-4" />
            </button>
          )}
        </div>

        {/* Right Actions (revealed when swiped left) */}
        <div className="flex items-center gap-1">
          {onToggleMute && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                onToggleMute(String(dialog.id));
                setOffsetX(0);
              }}
              className={`w-10 h-10 rounded-xl text-white flex flex-col items-center justify-center text-[10px] shadow-sm hover:brightness-110 active:scale-95 transition-all ${
                dialog.isMuted ? "bg-purple-600" : "bg-amber-500"
              }`}
              title={dialog.isMuted ? "إلغاء الكتم" : "كتم الإشعارات"}
            >
              {dialog.isMuted ? <Volume2 className="w-4 h-4" /> : <VolumeX className="w-4 h-4" />}
            </button>
          )}

          {onToggleRead && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                onToggleRead(String(dialog.id));
                setOffsetX(0);
              }}
              className="w-10 h-10 rounded-xl bg-emerald-600 text-white flex flex-col items-center justify-center text-[10px] shadow-sm hover:brightness-110 active:scale-95 transition-all"
              title={dialog.unreadCount > 0 ? "تحديد كمقروء" : "تحديد كغير مقروء"}
            >
              {dialog.unreadCount > 0 ? <CheckCheck className="w-4 h-4" /> : <Mail className="w-4 h-4" />}
            </button>
          )}

          {onDelete && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                onDelete(String(dialog.id));
                setOffsetX(0);
              }}
              className="w-10 h-10 rounded-xl bg-red-500 text-white flex flex-col items-center justify-center text-[10px] shadow-sm hover:brightness-110 active:scale-95 transition-all"
              title="حذف المحادثة"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          )}
        </div>
      </div>

      {/* Foreground Swipeable Chat Card */}
      <div
        id={`sidebar-dialog-item-${dialog.id}`}
        onClick={() => onSelectChat(String(dialog.id))}
        onTouchStart={handleTouchStart}
        onTouchMove={handleTouchMove}
        onTouchEnd={handleTouchEnd}
        style={{
          transform: `translateX(${offsetX}px)`,
          transition: isSwiping ? "none" : "transform 0.25s cubic-bezier(0.2, 0.8, 0.2, 1)",
        }}
        className={`relative z-10 px-3 py-2.5 flex items-center gap-3 cursor-pointer transition-colors bg-white dark:bg-[#17212b] ${
          isSelected
            ? "bg-sky-500/15 dark:bg-sky-500/25 text-slate-900 dark:text-white"
            : "hover:bg-slate-50 dark:hover:bg-slate-800/60 text-slate-900 dark:text-slate-100"
        }`}
      >
        {/* Selected Accent Indicator */}
        {isSelected && (
          <div className="absolute right-0 top-2.5 bottom-2.5 w-1 bg-[#2481cc] rounded-l-full" />
        )}

        {/* Drag Handle on hover */}
        <div
          className="text-slate-300 dark:text-slate-600 hover:text-slate-500 opacity-0 group-hover:opacity-100 transition-opacity cursor-grab active:cursor-grabbing shrink-0"
          title="اسحب لإعادة الترتيب"
        >
          <GripVertical className="w-3.5 h-3.5" />
        </div>

        {/* Avatar */}
        <TelegramAvatar
          id={String(dialog.id)}
          name={dialog.title || dialog.name}
          photoUrl={dialog.photoUrl}
          type={dialog.type}
          size="lg"
          isOnline={dialog.isOnline}
        />

        {/* Chat Text Info */}
        <div className="flex-1 min-w-0">
          {/* Line 1: Name + Badges + Time */}
          <div className="flex items-center justify-between mb-1">
            <div className="flex items-center gap-1 min-w-0">
              <span
                className={`font-bold text-xs truncate ${
                  isSelected
                    ? "text-[#2481cc] dark:text-sky-400"
                    : "text-slate-900 dark:text-slate-100"
                }`}
              >
                {dialog.title || dialog.name}
              </span>
              {dialog.isVerified && (
                <span className="text-[#2481cc] text-[10px] font-bold">✓</span>
              )}
              {dialog.isMuted && (
                <VolumeX className="w-3 h-3 text-slate-400" />
              )}
              {dialog.isLocked && (
                <Lock className="w-3 h-3 text-amber-500" />
              )}
              {dialog.archived && (
                <span className="px-1 py-0.2 bg-slate-200 dark:bg-slate-700 text-slate-600 dark:text-slate-300 rounded text-[9px]">
                  مؤرشف
                </span>
              )}
            </div>
            <span
              className={`text-[10px] shrink-0 ${
                isSelected
                  ? "text-[#2481cc] dark:text-sky-400 font-medium"
                  : "text-slate-400"
              }`}
            >
              {formatTime(dialog.lastMessage?.date)}
            </span>
          </div>

          {/* Line 2: Last Message Preview + Unread/Pin Counter */}
          <div className="flex items-center justify-between gap-1">
            <div
              className={`text-xs truncate ${
                isSelected
                  ? "text-slate-700 dark:text-slate-300"
                  : "text-slate-500 dark:text-slate-400"
              }`}
            >
              {dialog.lastMessage ? (
                (() => {
                  const text = dialog.lastMessage.text || "";
                  const isJoinAction =
                    text.startsWith("انضم ") ||
                    text.includes("لتيليجرام!") ||
                    text.startsWith("تمت إضافة ") ||
                    text.startsWith("أضاف ") ||
                    text.startsWith("لقد سمحت ");

                  return (
                    <div className="flex items-center truncate">
                      {dialog.lastMessage.out && (
                        <CheckCheck className="w-3.5 h-3.5 inline ml-1 text-[#2481cc] shrink-0" />
                      )}
                      {dialog.lastMessage.senderName && dialog.type === "group" && !isJoinAction && (
                        <span className="font-semibold text-[#2481cc] dark:text-sky-400 ml-1 shrink-0">
                          {dialog.lastMessage.senderName}:
                        </span>
                      )}
                      <span className={`truncate ${isJoinAction ? "text-[#2481cc] dark:text-sky-400 font-medium" : ""}`}>
                        {text}
                      </span>
                    </div>
                  );
                })()
              ) : (
                <span className="italic text-slate-400">لا توجد رسائل</span>
              )}
            </div>

            {/* Unread / Pinned Badges */}
            <div className="flex items-center gap-1 shrink-0">
              {dialog.pinned && (
                <Pin className="w-3.5 h-3.5 text-slate-400" />
              )}
              {dialog.unreadCount > 0 && (
                <span
                  className={`px-2 py-0.5 rounded-full text-[10px] font-bold min-w-[20px] text-center ${
                    dialog.isMuted
                      ? "bg-slate-300 dark:bg-slate-700 text-slate-700 dark:text-slate-300"
                      : "bg-[#2481cc] text-white shadow-2xs"
                  }`}
                >
                  {dialog.unreadCount}
                </span>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
