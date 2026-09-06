/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState } from 'react';
import { RefreshCw, Sparkles, X, CheckCircle2, AlertTriangle, GitCommit } from 'lucide-react';
import { useTelegram } from '../../context/TelegramContext';

export const UpdateNotification: React.FC = () => {
  const { updateState, triggerAppUpdate, dismissUpdateNotification, settings } = useTelegram();
  const isArabic = settings.language === 'ar';
  const [deployTriggered, setDeployTriggered] = useState(false);
  const [localError, setLocalError] = useState<string | null>(null);

  // If update notification is dismissed or no update available, do not render
  if (!updateState.showUpdateNotification || !updateState.hasUpdate) {
    return null;
  }

  const handleUpdateClick = async () => {
    setLocalError(null);
    try {
      const success = await triggerAppUpdate();
      if (success) {
        setDeployTriggered(true);
        // Wait 2.5 seconds to let user read confirmation, then reload the page
        setTimeout(() => {
          window.location.reload();
        }, 2500);
      } else {
        setLocalError(
          updateState.error ||
            (isArabic
              ? 'تعذر إرسال طلب التحديث إلى Render. يرجى المحاولة مرة أخرى.'
              : 'Failed to send deploy request to Render. Please try again.')
        );
      }
    } catch (err: any) {
      setLocalError(err?.message || (isArabic ? 'حدث خطأ في الاتصال بالخادم' : 'Connection error'));
    }
  };

  return (
    <aside
      id="tg-smart-update-notification"
      role="banner"
      aria-label={isArabic ? 'إشعار توفر تحديث جديد' : 'New update notification'}
      dir={isArabic ? 'rtl' : 'ltr'}
      className="w-full z-[100] transition-all duration-300 select-none animate-in fade-in slide-in-from-top-2 bg-gradient-to-r from-[#17212b] via-[#1d2a3a] to-[#17212b] border-b border-[#5288c1]/40 text-white shadow-lg"
    >
      <div className="max-w-7xl mx-auto px-3.5 py-2.5 sm:px-5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
        {/* Left / Info Section */}
        <div className="flex items-start sm:items-center gap-3 flex-1 min-w-0">
          <div className="w-8 h-8 rounded-full bg-[#5288c1]/20 border border-[#5288c1]/40 flex items-center justify-center shrink-0 text-[#5288c1] mt-0.5 sm:mt-0">
            {updateState.isUpdating || deployTriggered ? (
              <RefreshCw className="w-4 h-4 animate-spin text-sky-400" />
            ) : (
              <Sparkles className="w-4 h-4 text-amber-400 animate-pulse" />
            )}
          </div>

          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 flex-wrap">
              <span className="font-semibold text-xs sm:text-sm text-white">
                {deployTriggered
                  ? (isArabic ? 'جاري تحديث التطبيق...' : 'Updating application...')
                  : (isArabic ? 'يتوفر إصدار جديد من التطبيق!' : 'A new update is available!')}
              </span>

              {updateState.commitHash && (
                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-mono font-medium bg-sky-500/15 text-sky-300 border border-sky-500/30">
                  <GitCommit className="w-3 h-3" />
                  {updateState.commitHash}
                </span>
              )}
            </div>

            <p className="text-[11px] sm:text-xs text-slate-300 mt-0.5 truncate max-w-xl">
              {deployTriggered ? (
                <span className="text-emerald-300 font-medium">
                  {isArabic
                    ? 'تم إرسال طلب إعادة النشر إلى Render بنجاح! جاري إعادة تحميل الصفحة...'
                    : 'Deploy hook triggered successfully! Reloading application...'}
                </span>
              ) : updateState.commitMessage ? (
                <span>
                  {isArabic ? 'آخر تعديل: ' : 'Latest commit: '}
                  <span className="text-white font-medium">{updateState.commitMessage}</span>
                </span>
              ) : (
                <span>
                  {isArabic
                    ? 'تم دفع تغييرات برمجية جديدة إلى المستودع. انقر على تحديث لتطبيقها فوراً.'
                    : 'New commits were pushed to repository. Click update to apply immediately.'}
                </span>
              )}
            </p>

            {(localError || updateState.error) && !deployTriggered && (
              <p className="text-[11px] text-rose-300 flex items-center gap-1 mt-1">
                <AlertTriangle className="w-3 h-3 shrink-0" />
                {localError || updateState.error}
              </p>
            )}
          </div>
        </div>

        {/* Action Controls */}
        <div className="flex items-center gap-2 self-end sm:self-center shrink-0">
          {deployTriggered ? (
            <div className="flex items-center gap-1.5 text-xs text-emerald-400 font-medium px-3 py-1.5 rounded-lg bg-emerald-500/10 border border-emerald-500/20">
              <CheckCircle2 className="w-4 h-4" />
              <span>{isArabic ? 'جاري التحميل...' : 'Reloading...'}</span>
            </div>
          ) : (
            <>
              <button
                id="tg-update-trigger-btn"
                type="button"
                onClick={handleUpdateClick}
                disabled={updateState.isUpdating}
                className="px-3.5 py-1.5 rounded-lg bg-[#5288c1] hover:bg-[#4375a8] active:bg-[#386696] disabled:opacity-60 text-white text-xs font-medium flex items-center gap-1.5 shadow-sm transition-all cursor-pointer min-h-[36px]"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${updateState.isUpdating ? 'animate-spin' : ''}`} />
                <span>
                  {updateState.isUpdating
                    ? (isArabic ? 'جاري تحديث التطبيق...' : 'Updating...')
                    : (isArabic ? 'تحديث الآن' : 'Update Now')}
                </span>
              </button>

              <button
                id="tg-update-dismiss-btn"
                type="button"
                onClick={dismissUpdateNotification}
                disabled={updateState.isUpdating}
                title={isArabic ? 'إغلاق الإشعار' : 'Dismiss notification'}
                className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-white/10 active:bg-white/15 transition-colors cursor-pointer min-h-[36px] min-w-[36px] flex items-center justify-center"
              >
                <X className="w-4 h-4" />
              </button>
            </>
          )}
        </div>
      </div>
    </aside>
  );
};

export default UpdateNotification;
