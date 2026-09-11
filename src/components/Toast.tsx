import React, { useEffect } from 'react';
import { CheckCircle, AlertCircle, Info, X } from 'lucide-react';

export interface ToastData {
  id: string;
  message: string;
  type?: 'success' | 'info' | 'error';
  duration?: number;
}

interface ToastProps {
  toast: ToastData | null;
  onClose: () => void;
}

export const Toast: React.FC<ToastProps> = ({ toast, onClose }) => {
  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => {
      onClose();
    }, toast.duration || 3000);
    return () => clearTimeout(timer);
  }, [toast, onClose]);

  if (!toast) return null;

  return (
    <div className="fixed bottom-14 left-1/2 -translate-x-1/2 z-50 pointer-events-auto transition-all animate-bounce-in">
      <div className="flex items-center gap-2.5 px-4 py-2.5 rounded-full bg-[#182533]/95 text-white text-xs font-medium shadow-2xl border border-white/15 backdrop-blur-md">
        {toast.type === 'success' && <CheckCircle className="w-4 h-4 text-emerald-400 shrink-0" />}
        {toast.type === 'error' && <AlertCircle className="w-4 h-4 text-red-400 shrink-0" />}
        {(!toast.type || toast.type === 'info') && <Info className="w-4 h-4 text-[#3390ec] shrink-0" />}
        <span className="truncate max-w-sm">{toast.message}</span>
        <button
          onClick={onClose}
          className="text-gray-400 hover:text-white ms-1 p-0.5 rounded-full transition"
        >
          <X className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
};
