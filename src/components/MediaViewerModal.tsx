import React from 'react';
import { X, Download, ZoomIn } from 'lucide-react';

interface MediaViewerModalProps {
  isOpen: boolean;
  onClose: () => void;
  mediaUrl: string | null;
  title?: string;
  isDark: boolean;
}

export const MediaViewerModal: React.FC<MediaViewerModalProps> = ({
  isOpen,
  onClose,
  mediaUrl,
  title,
}) => {
  if (!isOpen || !mediaUrl) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/90 p-4 select-none backdrop-blur-sm animate-fade-in">
      {/* Top action bar */}
      <div className="absolute top-4 start-4 end-4 flex items-center justify-between text-white z-10">
        <span className="text-sm font-semibold truncate max-w-md">{title || 'عرض الوسائط'}</span>
        <div className="flex items-center gap-3">
          <a
            href={mediaUrl}
            download="telegram-media"
            target="_blank"
            rel="noreferrer"
            className="p-2 rounded-full bg-white/10 hover:bg-white/20 transition"
            title="تحميل"
          >
            <Download className="w-5 h-5 text-white" />
          </a>
          <button
            onClick={onClose}
            className="p-2 rounded-full bg-white/10 hover:bg-white/20 transition"
          >
            <X className="w-5 h-5 text-white" />
          </button>
        </div>
      </div>

      {/* Media Content */}
      <div className="max-w-4xl max-h-[85vh] flex items-center justify-center">
        <img
          src={mediaUrl}
          alt={title || 'Telegram media preview'}
          referrerPolicy="no-referrer"
          className="max-w-full max-h-[85vh] object-contain rounded-xl shadow-2xl"
        />
      </div>
    </div>
  );
};
