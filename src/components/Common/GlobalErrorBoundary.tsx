import React, { ErrorInfo, ReactNode } from 'react';
import { RefreshCw, Trash2, AlertTriangle, ShieldCheck, Copy, Check, Terminal } from 'lucide-react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
  errorInfo: ErrorInfo | null;
  isResetting: boolean;
  copied: boolean;
}

export class GlobalErrorBoundary extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null,
      isResetting: false,
      copied: false,
    };
  }

  public static getDerivedStateFromError(error: Error): Partial<State> {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('[GlobalErrorBoundary] Telegram App Crash Caught:', error, errorInfo);
    this.setState({ errorInfo });
  }

  private handleHardReset = async () => {
    this.setState({ isResetting: true });
    try {
      // 1. Unregister all service workers and wait for completion
      if ('serviceWorker' in navigator) {
        try {
          const registrations = await navigator.serviceWorker.getRegistrations();
          await Promise.all(registrations.map((reg) => reg.unregister().catch(() => false)));
        } catch (swErr) {
          console.warn('[GlobalErrorBoundary] Error unregistering service workers:', swErr);
        }
      }

      // 2. Delete all CacheStorage entries and wait for completion
      if ('caches' in window) {
        try {
          const cacheKeys = await caches.keys();
          await Promise.all(cacheKeys.map((key) => caches.delete(key).catch(() => false)));
        } catch (cacheErr) {
          console.warn('[GlobalErrorBoundary] Error deleting caches:', cacheErr);
        }
      }

      // 3. Purge IndexedDB databases
      if (typeof window !== 'undefined' && 'indexedDB' in window) {
        const knownDbs = [
          'telegram_sqlite_database_v1',
          'TelegramDatabase',
          'telegram_indexed_db',
          'bot_storage',
          'keyval-store',
          'chatStore',
          'messages_cache',
          'telegramDb',
        ];
        for (const dbName of knownDbs) {
          try {
            indexedDB.deleteDatabase(dbName);
          } catch (_) {}
        }
      }

      // 4. Wipe client storage
      try {
        localStorage.clear();
        sessionStorage.clear();
      } catch (_) {}
    } catch (e) {
      console.error('[GlobalErrorBoundary] Error during comprehensive reset:', e);
    }

    // 5. Force reload bypassing browser HTTP cache with unique timestamp
    setTimeout(() => {
      window.location.replace(window.location.origin + window.location.pathname + '?hard_refresh=' + Date.now());
    }, 300);
  };

  private handleQuickReload = () => {
    window.location.reload();
  };

  private handleCopyError = () => {
    const errorText = [
      this.state.error?.name || 'Error',
      this.state.error?.message || 'Unknown error',
      this.state.error?.stack || '',
      this.state.errorInfo?.componentStack || '',
    ].filter(Boolean).join('\\n');

    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(errorText).then(() => {
        this.setState({ copied: true });
        setTimeout(() => this.setState({ copied: false }), 2500);
      }).catch(() => {});
    }
  };

  public render() {
    if (this.state.hasError) {
      const errorMessage = this.state.error?.message || this.state.error?.toString() || 'Unknown runtime exception';

      return (
        <div
          dir="rtl"
          style={{
            fontFamily: "'Cairo', system-ui, -apple-system, sans-serif",
            background: 'linear-gradient(135deg, #0e1621 0%, #17212b 100%)',
          }}
          className="fixed inset-0 z-[99999] flex items-center justify-center p-4 text-white select-none"
        >
          <div className="max-w-md w-full bg-[#182533] border border-white/10 rounded-2xl p-6 shadow-2xl text-center space-y-4">
            <div className="w-16 h-16 rounded-full bg-rose-500/20 text-rose-400 border border-rose-500/30 flex items-center justify-center mx-auto">
              <AlertTriangle className="w-8 h-8" />
            </div>

            <div className="space-y-1.5">
              <h2 className="text-xl font-bold text-white">حدث خطأ في تحميل الذاكرة أو البيانات</h2>
              <p className="text-xs text-gray-400 leading-relaxed">
                تم رصد تعارض في ملفات التخزين المؤقت القديمة (Cache) أو بيانات الجلسة السابقة في المتصفح. يمكنك استعادة النظام فوراً وتحديث الملفات بنقرة واحدة.
              </p>
            </div>

            {this.state.error && (
              <div className="bg-black/50 border border-white/10 rounded-xl p-3 text-right space-y-2">
                <div className="flex items-center justify-between text-[11px] text-gray-400 border-b border-white/5 pb-1.5">
                  <span className="flex items-center gap-1">
                    <Terminal className="w-3 h-3 text-amber-400" />
                    <span>رمز الخطأ البرمجي</span>
                  </span>
                  <button
                    type="button"
                    onClick={this.handleCopyError}
                    className="flex items-center gap-1 text-sky-400 hover:text-sky-300 transition-colors"
                  >
                    {this.state.copied ? (
                      <>
                        <Check className="w-3 h-3 text-emerald-400" />
                        <span className="text-emerald-400">تم النسخ</span>
                      </>
                    ) : (
                      <>
                        <Copy className="w-3 h-3" />
                        <span>نسخ التفاصيل</span>
                      </>
                    )}
                  </button>
                </div>
                <div className="max-h-24 overflow-y-auto">
                  <code className="text-[11px] font-mono text-rose-300 break-all leading-tight block select-text">
                    {errorMessage}
                  </code>
                </div>
              </div>
            )}

            <div className="pt-2 flex flex-col gap-2.5">
              <button
                type="button"
                disabled={this.state.isResetting}
                onClick={this.handleHardReset}
                className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-cyan-600 to-blue-600 hover:from-cyan-500 hover:to-blue-500 text-white text-sm font-bold flex items-center justify-center gap-2 shadow-lg transition-all active:scale-98 disabled:opacity-50 cursor-pointer"
              >
                <Trash2 className="w-4 h-4" />
                <span>{this.state.isResetting ? 'جاري مسح الكاش وتحديث النظام...' : 'إصلاح تلقائي ومسح الكاش التالف'}</span>
              </button>

              <button
                type="button"
                onClick={this.handleQuickReload}
                className="w-full py-2.5 px-4 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-gray-300 text-xs font-semibold flex items-center justify-center gap-2 transition-all cursor-pointer"
              >
                <RefreshCw className="w-3.5 h-3.5" />
                <span>إعادة تحميل الصفحة</span>
              </button>
            </div>

            <div className="pt-2 text-[10px] text-gray-500 flex items-center justify-center gap-1.5">
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
              <span>نظام الاسترداد الذكي • Telegram Web Pro (Layer 184)</span>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
