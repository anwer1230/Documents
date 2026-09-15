import React, { useState } from 'react';
import { X, Sparkles, Copy, Check, Loader2, RefreshCw, MessageSquare } from 'lucide-react';

interface AiSummaryModalProps {
  isOpen: boolean;
  onClose: () => void;
  chatTitle: string;
  summary: string;
  isLoading: boolean;
  error?: string | null;
  onRefresh?: () => void;
  isDark: boolean;
  lang: 'ar' | 'en';
}

export const AiSummaryModal: React.FC<AiSummaryModalProps> = ({
  isOpen,
  onClose,
  chatTitle,
  summary,
  isLoading,
  error,
  onRefresh,
  isDark,
  lang,
}) => {
  const [copied, setCopied] = useState(false);
  const isAr = lang === 'ar';

  if (!isOpen) return null;

  const handleCopy = () => {
    if (!summary) return;
    navigator.clipboard.writeText(summary);
    setCopied(true);
    setTimeout(() => setCopied(false), 2500);
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in"
      onClick={onClose}
      dir={isAr ? 'rtl' : 'ltr'}
    >
      <div
        className={`w-full max-w-lg rounded-2xl shadow-2xl overflow-hidden border flex flex-col max-h-[85vh] animate-scale-in ${
          isDark ? 'bg-[#17212b] border-[#242f3d] text-white' : 'bg-white border-gray-200 text-gray-900'
        }`}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div
          className={`flex items-center justify-between px-6 py-4 border-b ${
            isDark ? 'border-[#242f3d] bg-[#0e1621]' : 'border-gray-100 bg-gray-50/80'
          }`}
        >
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-[#8A2BE2] to-[#3390ec] flex items-center justify-center text-white shadow-md">
              <Sparkles className="w-5 h-5 animate-pulse" />
            </div>
            <div>
              <h3 className="text-sm font-bold flex items-center gap-2">
                <span>{isAr ? 'ملخص الذكاء الاصطناعي' : 'AI Chat Summary'}</span>
                <span className="text-[10px] uppercase font-semibold px-2 py-0.5 rounded-full bg-[#3390ec]/20 text-[#3390ec]">
                  Gemini
                </span>
              </h3>
              <p className="text-xs text-gray-400 truncate max-w-[240px]">
                {chatTitle}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className={`p-1.5 rounded-xl transition ${
              isDark ? 'hover:bg-[#242f3d] text-gray-400 hover:text-white' : 'hover:bg-gray-200 text-gray-500'
            }`}
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Body */}
        <div className="flex-1 overflow-y-auto p-6 space-y-4 text-sm leading-relaxed">
          {isLoading ? (
            <div className="py-12 flex flex-col items-center justify-center gap-3 text-center">
              <div className="w-12 h-12 rounded-2xl bg-[#3390ec]/15 text-[#3390ec] flex items-center justify-center animate-spin">
                <Loader2 className="w-6 h-6" />
              </div>
              <p className="text-sm font-medium">
                {isAr ? 'جاري تحليل وتلخيص المحادثة بواسطة Gemini...' : 'Analyzing and summarizing chat with Gemini...'}
              </p>
              <p className="text-xs text-gray-400">
                {isAr ? 'يتم استخراج النقاط والقرارات الرئيسية تلقائياً' : 'Extracting key takeaways and discussion points automatically'}
              </p>
            </div>
          ) : error ? (
            <div className="py-8 flex flex-col items-center justify-center gap-3 text-center">
              <div className="w-10 h-10 rounded-full bg-rose-500/20 text-rose-500 flex items-center justify-center">
                <X className="w-5 h-5" />
              </div>
              <p className="text-xs text-rose-400 font-medium max-w-sm">{error}</p>
              {onRefresh && (
                <button
                  onClick={onRefresh}
                  className="mt-2 inline-flex items-center gap-2 px-3 py-1.5 rounded-xl text-xs font-semibold bg-[#3390ec] text-white hover:bg-[#2880db] transition"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                  <span>{isAr ? 'إعادة المحاولة' : 'Try Again'}</span>
                </button>
              )}
            </div>
          ) : summary ? (
            <div className="space-y-3">
              <div
                className={`p-4 rounded-xl border text-xs leading-relaxed whitespace-pre-wrap ${
                  isDark ? 'bg-[#0e1621] border-[#242f3d] text-gray-200' : 'bg-gray-50 border-gray-100 text-gray-700'
                }`}
              >
                {summary}
              </div>
              <p className="text-[11px] text-gray-400 flex items-center gap-1.5 pt-1">
                <Sparkles className="w-3.5 h-3.5 text-[#3390ec]" />
                <span>
                  {isAr
                    ? 'تم التوليد تلقائياً عبر نموذج Google Gemini بناءً على رسائل المحادثة الأخيرة.'
                    : 'Generated automatically via Google Gemini model based on recent conversation messages.'}
                </span>
              </p>
            </div>
          ) : (
            <div className="py-8 text-center text-xs text-gray-400">
              <MessageSquare className="w-8 h-8 mx-auto mb-2 opacity-50" />
              <p>{isAr ? 'لا توجد رسائل كافية للتلخيص حالياً' : 'Not enough messages to summarize yet'}</p>
            </div>
          )}
        </div>

        {/* Footer */}
        <div
          className={`flex items-center justify-between px-6 py-3 border-t ${
            isDark ? 'border-[#242f3d] bg-[#0e1621]/80' : 'border-gray-100 bg-gray-50/80'
          }`}
        >
          {summary ? (
            <button
              onClick={handleCopy}
              disabled={isLoading || !summary}
              className={`flex items-center gap-2 px-3 py-1.5 rounded-xl text-xs font-semibold transition ${
                copied
                  ? 'bg-emerald-500/20 text-emerald-400'
                  : isDark
                  ? 'bg-[#242f3d] text-gray-200 hover:bg-[#2e3b4d]'
                  : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
              }`}
            >
              {copied ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
              <span>{copied ? (isAr ? 'تم النسخ!' : 'Copied!') : isAr ? 'نسخ الملخص' : 'Copy Summary'}</span>
            </button>
          ) : (
            <div />
          )}

          <div className="flex items-center gap-2">
            {onRefresh && summary && (
              <button
                onClick={onRefresh}
                disabled={isLoading}
                className={`p-2 rounded-xl text-xs transition ${
                  isDark ? 'hover:bg-[#242f3d] text-gray-400' : 'hover:bg-gray-200 text-gray-600'
                }`}
                title={isAr ? 'تحديث الملخص' : 'Refresh Summary'}
              >
                <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
              </button>
            )}
            <button
              onClick={onClose}
              className="px-4 py-1.5 rounded-xl text-xs font-semibold bg-[#3390ec] text-white hover:bg-[#2880db] transition shadow-sm"
            >
              {isAr ? 'إغلاق' : 'Close'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AiSummaryModal;
