import React, { useEffect } from 'react';
import { CheckCircle2, AlertCircle, Info, AlertTriangle, X } from 'lucide-react';

export interface ToastData {
  id?: string;
  message: string;
  type?: 'success' | 'error' | 'info' | 'warning';
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
    }, toast.duration || 3200);
    return () => clearTimeout(timer);
  }, [toast, onClose]);

  if (!toast) return null;

  const getBg = () => {
    switch (toast.type) {
      case 'success':
        return 'bg-emerald-600 text-white shadow-emerald-500/20';
      case 'error':
        return 'bg-rose-600 text-white shadow-rose-500/20';
      case 'warning':
        return 'bg-amber-600 text-white shadow-amber-500/20';
      case 'info':
      default:
        return 'bg-[#242f3d] text-white border border-gray-700/60 shadow-black/40';
    }
  };

  const getIcon = () => {
    switch (toast.type) {
      case 'success':
        return <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-200" />;
      case 'error':
        return <AlertCircle className="w-4 h-4 shrink-0 text-rose-200" />;
      case 'warning':
        return <AlertTriangle className="w-4 h-4 shrink-0 text-amber-200" />;
      case 'info':
      default:
        return <Info className="w-4 h-4 shrink-0 text-[#3390ec]" />;
    }
  };

  return (
    <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 max-w-md w-auto px-4 py-2.5 rounded-xl shadow-xl flex items-center gap-3 text-sm font-medium backdrop-blur-md animate-fade-in transition-all">
      <div className={`px-4 py-2.5 rounded-xl flex items-center gap-3 ${getBg()}`}>
        {getIcon()}
        <span className="leading-snug">{toast.message}</span>
        <button
          onClick={onClose}
          className="ms-2 text-gray-300 hover:text-white transition p-0.5 rounded-md hover:bg-white/10"
        >
          <X className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
};
