import React, { useState } from "react";
import {
  X,
  Bell,
  BellOff,
  Image as ImageIcon,
  FileText,
  Mic,
  Link,
  Users,
  Shield,
  Phone,
  AtSign,
  Share2,
  Trash2,
} from "lucide-react";
import { TelegramDialog } from "../types";
import { TelegramAvatar } from "./TelegramAvatar";

interface ChatInfoDrawerProps {
  dialog: TelegramDialog;
  isOpen: boolean;
  onClose: () => void;
}

export const ChatInfoDrawer: React.FC<ChatInfoDrawerProps> = ({
  dialog,
  isOpen,
  onClose,
}) => {
  const [activeMediaTab, setActiveMediaTab] = useState<"media" | "files" | "audio" | "links">("media");
  const [isMuted, setIsMuted] = useState(dialog.isMuted || false);

  if (!isOpen) return null;

  return (
    <div
      id="telegram-chat-info-drawer"
      className="w-80 h-full bg-white dark:bg-slate-900 border-r border-slate-200 dark:border-slate-800 flex flex-col shadow-xl z-20 shrink-0 select-none animate-slide-right"
      dir="rtl"
    >
      {/* Top Header */}
      <div className="h-16 px-4 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between">
        <h3 className="font-bold text-sm text-slate-800 dark:text-white">
          معلومات {dialog.type === "channel" ? "القناة" : dialog.type === "group" ? "المجموعة" : "المستخدم"}
        </h3>
        <button
          id="chat-info-close-btn"
          onClick={onClose}
          className="w-8 h-8 rounded-full flex items-center justify-center text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      {/* Drawer Body Scroll */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {/* Profile Card */}
        <div className="text-center pb-2 flex flex-col items-center">
          <TelegramAvatar
            id={String(dialog.id)}
            name={dialog.title || dialog.name}
            photoUrl={dialog.photoUrl}
            type={dialog.type}
            size="3xl"
            isOnline={dialog.isOnline}
            className="mb-3"
            showSpecialIcon={true}
          />
          <h2 className="font-bold text-base text-slate-800 dark:text-white flex items-center justify-center gap-1">
            <span>{dialog.title}</span>
            {dialog.isVerified && <span className="text-sky-500 text-xs font-bold">✓</span>}
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            {dialog.type === "channel"
              ? `${(dialog.memberCount || 142000).toLocaleString()} مشترك`
              : dialog.type === "group"
              ? `${(dialog.memberCount || 3840).toLocaleString()} عضو`
              : dialog.isOnline
              ? "متصل الآن"
              : "آخر ظهور مؤخراً"}
          </p>
        </div>

        {/* Info Items Box */}
        <div className="bg-slate-50 dark:bg-slate-800/50 rounded-2xl p-3 space-y-3 border border-slate-100 dark:border-slate-800 text-xs">
          {dialog.username && (
            <div className="flex items-center gap-3 text-slate-700 dark:text-slate-300">
              <AtSign className="w-4 h-4 text-sky-500 shrink-0" />
              <div className="min-w-0 flex-1">
                <span className="text-[10px] text-slate-400 block">اسم المستخدم</span>
                <span className="font-mono text-sky-600 dark:text-sky-400">@{dialog.username}</span>
              </div>
            </div>
          )}

          <div className="flex items-center justify-between text-slate-700 dark:text-slate-300">
            <div className="flex items-center gap-3">
              {isMuted ? (
                <BellOff className="w-4 h-4 text-slate-400 shrink-0" />
              ) : (
                <Bell className="w-4 h-4 text-amber-500 shrink-0" />
              )}
              <span>الإشعارات</span>
            </div>
            <button
              onClick={() => setIsMuted(!isMuted)}
              className={`w-10 h-5 rounded-full transition-colors relative p-0.5 ${
                !isMuted ? "bg-sky-500" : "bg-slate-300 dark:bg-slate-700"
              }`}
            >
              <div
                className={`w-4 h-4 rounded-full bg-white transition-transform ${
                  !isMuted ? "-translate-x-5" : "translate-x-0"
                }`}
              />
            </button>
          </div>
        </div>

        {/* Shared Media Tabs */}
        <div>
          <div className="flex items-center justify-around border-b border-slate-200 dark:border-slate-800 text-xs font-semibold text-slate-500 pb-1">
            <button
              onClick={() => setActiveMediaTab("media")}
              className={`pb-1 px-2 border-b-2 transition-colors ${
                activeMediaTab === "media"
                  ? "border-sky-500 text-sky-500 font-bold"
                  : "border-transparent hover:text-slate-800 dark:hover:text-slate-200"
              }`}
            >
              الصور (14)
            </button>
            <button
              onClick={() => setActiveMediaTab("files")}
              className={`pb-1 px-2 border-b-2 transition-colors ${
                activeMediaTab === "files"
                  ? "border-sky-500 text-sky-500 font-bold"
                  : "border-transparent hover:text-slate-800 dark:hover:text-slate-200"
              }`}
            >
              الملفات (3)
            </button>
            <button
              onClick={() => setActiveMediaTab("audio")}
              className={`pb-1 px-2 border-b-2 transition-colors ${
                activeMediaTab === "audio"
                  ? "border-sky-500 text-sky-500 font-bold"
                  : "border-transparent hover:text-slate-800 dark:hover:text-slate-200"
              }`}
            >
              صوتيات (8)
            </button>
          </div>

          <div className="pt-3">
            {activeMediaTab === "media" && (
              <div className="grid grid-cols-3 gap-1.5">
                {[
                  "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=300&auto=format&fit=crop&q=60",
                  "https://images.unsplash.com/photo-1620641788421-7a1c342ea42e?w=300&auto=format&fit=crop&q=60",
                  "https://images.unsplash.com/photo-1634017839464-5c339ebe3cb4?w=300&auto=format&fit=crop&q=60",
                ].map((src, i) => (
                  <img
                    key={i}
                    src={src}
                    alt="Media thumbnail"
                    className="w-full h-20 object-cover rounded-lg hover:opacity-90 transition-opacity cursor-pointer"
                  />
                ))}
              </div>
            )}

            {activeMediaTab === "files" && (
              <div className="space-y-2 text-xs">
                <div className="flex items-center gap-2 p-2 rounded-xl bg-slate-50 dark:bg-slate-800/50">
                  <FileText className="w-4 h-4 text-sky-500 shrink-0" />
                  <div className="min-w-0 flex-1">
                    <p className="font-semibold truncate">telegram_mtproto_specs.pdf</p>
                    <span className="text-[10px] text-slate-400">1.2 MB • 14 Aug</span>
                  </div>
                </div>
              </div>
            )}

            {activeMediaTab === "audio" && (
              <div className="space-y-2 text-xs">
                <div className="flex items-center gap-2 p-2 rounded-xl bg-slate-50 dark:bg-slate-800/50">
                  <Mic className="w-4 h-4 text-emerald-500 shrink-0" />
                  <div className="min-w-0 flex-1">
                    <p className="font-semibold truncate">تسجيل صوتي 0:45</p>
                    <span className="text-[10px] text-slate-400">Voice note • 320 KB</span>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
