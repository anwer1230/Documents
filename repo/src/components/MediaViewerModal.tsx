import React from 'react';
import { X, Download, ZoomIn, ZoomOut, ExternalLink } from 'lucide-react';

interface MediaViewerModalProps {
  isOpen: boolean;
  onClose: () => void;
  mediaUrl: string | null;
  title?: string;
  isDark?: boolean;
}

export const MediaViewerModal: React.FC<MediaViewerModalProps> = ({
  isOpen,
  onClose,
  mediaUrl,
  title,
}) => {
  if (!isOpen || !mediaUrl) return null;

  return (
    <div className="fixed inset-0 z-50 flex flex-col bg-black/95 select-none animate-fade-in">
      {/* Top Controls Bar */}
      <div className="p-4 flex items-center justify-between text-white border-b border-gray-800 bg-black/40">
        <span className="text-sm font-semibold truncate max-w-md">{title || 'Media Lightbox'}</span>
        <div className="flex items-center gap-3">
          <a
            href={mediaUrl}
            target="_blank"
            rel="noreferrer"
            download
            className="p-2 rounded-xl bg-white/10 hover:bg-white/20 text-gray-300 hover:text-white transition"
            title="Download / Open"
          >
            <Download className="w-5 h-5" />
          </a>
          <button
            onClick={onClose}
            className="p-2 rounded-xl bg-white/10 hover:bg-rose-600 text-gray-300 hover:text-white transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* Main Image Container */}
      <div className="flex-1 flex items-center justify-center p-4 overflow-hidden relative">
        <img
          src={mediaUrl}
          alt={title || ''}
          className="max-w-full max-h-[85vh] object-contain rounded-lg shadow-2xl transition-all"
        />
      </div>
    </div>
  );
};
