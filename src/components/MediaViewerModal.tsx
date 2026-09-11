import React, { useState, useRef, useEffect } from 'react';
import { X, Download, ScanText, Copy, Check, Loader2 } from 'lucide-react';
import recognizeImageText, { OcrResult } from '../helpers/ocr/recognizeImageText';
import attachTextSelectionLayer from '../helpers/ocr/attachTextSelectionLayer';

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
  const [ocrLoading, setOcrLoading] = useState(false);
  const [ocrResult, setOcrResult] = useState<OcrResult | null>(null);
  const [copied, setCopied] = useState(false);
  const [ocrError, setOcrError] = useState<string | null>(null);
  const imgRef = useRef<HTMLImageElement | null>(null);
  const layerRef = useRef<HTMLElement | null>(null);

  // Clean up OCR layer on modal close or URL change
  useEffect(() => {
    if (layerRef.current) {
      layerRef.current.remove();
      layerRef.current = null;
    }
    setOcrResult(null);
    setOcrError(null);
    setCopied(false);
    setOcrLoading(false);
  }, [mediaUrl, isOpen]);

  if (!isOpen || !mediaUrl) return null;

  const handleRunOcr = async () => {
    if (ocrLoading) return;
    if (layerRef.current) {
      // Toggle off
      layerRef.current.remove();
      layerRef.current = null;
      setOcrResult(null);
      return;
    }

    setOcrLoading(true);
    setOcrError(null);

    try {
      const result = await recognizeImageText(mediaUrl);
      setOcrResult(result);

      if (!result.lines || result.lines.length === 0 || !result.text) {
        setOcrError('لم يتم العثور على نص في الصورة (No text found on the image)');
      } else if (imgRef.current) {
        const layer = attachTextSelectionLayer(imgRef.current, result.lines);
        layerRef.current = layer;
      }
    } catch (err: any) {
      console.error('OCR Error:', err);
      setOcrError('فشل التعرف على النص في الصورة');
    } finally {
      setOcrLoading(false);
    }
  };

  const handleCopyText = async () => {
    if (!ocrResult?.text) return;
    try {
      await navigator.clipboard.writeText(ocrResult.text);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // ignore
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/90 p-4 select-none backdrop-blur-sm animate-fade-in">
      {/* Top action bar */}
      <div className="absolute top-4 start-4 end-4 flex items-center justify-between text-white z-30">
        <span className="text-sm font-semibold truncate max-w-md">{title || 'عرض الوسائط'}</span>
        <div className="flex items-center gap-2">
          {/* OCR Select Text Button */}
          <button
            onClick={handleRunOcr}
            disabled={ocrLoading}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium transition ${
              layerRef.current
                ? 'bg-blue-600 text-white shadow-lg'
                : 'bg-white/10 hover:bg-white/20 text-white'
            }`}
            title="تحديد واستخراج النص من الصورة (OCR)"
          >
            {ocrLoading ? (
              <Loader2 className="w-4 h-4 animate-spin text-blue-400" />
            ) : (
              <ScanText className="w-4 h-4" />
            )}
            <span className="hidden sm:inline">
              {layerRef.current ? 'إخفاء التحديد' : 'تحديد النص (OCR)'}
            </span>
          </button>

          {/* Download Button */}
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

          {/* Close Button */}
          <button
            onClick={onClose}
            className="p-2 rounded-full bg-white/10 hover:bg-white/20 transition"
            title="إغلاق"
          >
            <X className="w-5 h-5 text-white" />
          </button>
        </div>
      </div>

      {/* OCR Status/Copy Banner */}
      {ocrResult && ocrResult.text && (
        <div className="absolute bottom-6 left-1/2 -translate-x-1/2 z-30 flex items-center gap-3 bg-neutral-900/95 border border-white/10 text-white px-4 py-2.5 rounded-2xl shadow-2xl backdrop-blur-md max-w-lg w-[90%] justify-between">
          <div className="text-xs truncate max-w-[280px] sm:max-w-xs text-neutral-300">
            {ocrResult.text.replace(/\s+/g, ' ')}
          </div>
          <button
            onClick={handleCopyText}
            className="flex items-center gap-1.5 px-3 py-1 bg-blue-600 hover:bg-blue-700 text-white text-xs font-medium rounded-lg transition shrink-0"
          >
            {copied ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
            <span>{copied ? 'تم النسخ' : 'نسخ النص'}</span>
          </button>
        </div>
      )}

      {/* OCR Error / No text notification */}
      {ocrError && (
        <div className="absolute bottom-6 left-1/2 -translate-x-1/2 z-30 bg-neutral-900/90 text-amber-300 text-xs px-4 py-2 rounded-xl shadow-lg border border-amber-500/20">
          {ocrError}
        </div>
      )}

      {/* Media Content with text selection layer container */}
      <div className="relative max-w-4xl max-h-[85vh] flex items-center justify-center">
        <img
          ref={imgRef}
          src={mediaUrl}
          alt={title || 'Telegram media preview'}
          crossOrigin="anonymous"
          referrerPolicy="no-referrer"
          className="max-w-full max-h-[85vh] object-contain rounded-xl shadow-2xl"
        />
      </div>
    </div>
  );
};
