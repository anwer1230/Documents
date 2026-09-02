import React from 'react';
import { X, Cpu, Activity } from 'lucide-react';
import { NertTerminal } from './NertTerminal';
import { TelegramDialog, TelegramUser } from '../types';

interface NertTerminalModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentUser: TelegramUser;
  dialogs: TelegramDialog[];
  onSelectChat: (chatId: string | number) => void;
  onOpenSettings: () => void;
  onLogout: () => void;
  selectedChatId: string | number | null;
}

export const NertTerminalModal: React.FC<NertTerminalModalProps> = ({
  isOpen,
  onClose,
  currentUser,
  dialogs,
  onSelectChat,
  onOpenSettings,
  onLogout,
  selectedChatId,
}) => {
  if (!isOpen) return null;

  return (
    <div
      id="nert-terminal-modal-overlay"
      className="fixed inset-0 z-50 flex items-center justify-center p-1 sm:p-3 bg-black/80 backdrop-blur-md animate-fade-in"
      dir="rtl"
    >
      <div
        id="nert-terminal-modal-container"
        className="w-full max-w-7xl h-[95vh] bg-[#0c1017] text-slate-100 rounded-2xl shadow-2xl border border-cyan-900/40 flex flex-col overflow-hidden select-none"
      >
        {/* Top Minimal Bar */}
        <div className="h-10 px-4 bg-[#080b10] border-b border-cyan-950/60 flex items-center justify-between">
          <div className="flex items-center gap-2 text-cyan-400 text-xs font-mono">
            <Cpu className="w-4 h-4 text-cyan-400 animate-pulse" />
            <span className="font-bold">NERT TERMINAL & INTELLIGENCE OPERATIONS</span>
            <span className="text-[10px] text-emerald-400 bg-emerald-950/60 px-1.5 py-0.5 rounded border border-emerald-800/40">
              NODE ACTIVE
            </span>
          </div>

          <button
            onClick={onClose}
            className="w-7 h-7 rounded-lg flex items-center justify-center text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Nert Terminal Body */}
        <div className="flex-1 overflow-hidden">
          <NertTerminal
            userProfile={currentUser as any}
            chats={dialogs as any}
            onSelectChat={onSelectChat}
            onOpenAutomation={() => {}}
            onOpenAcademic={() => {}}
            onOpenSettings={onOpenSettings}
            onOpenSecurity={() => {}}
            onOpenSystemMonitor={() => {}}
            onLogout={onLogout}
            selectedChatId={selectedChatId}
          />
        </div>
      </div>
    </div>
  );
};
