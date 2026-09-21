/**
 * PWA Service Worker Registration Fallback
 * Provides seamless no-op or native ServiceWorker registration when vite-plugin-pwa is absent
 */
export interface RegisterSWOptions {
  immediate?: boolean;
  onNeedRefresh?: () => void;
  onOfflineReady?: () => void;
  onRegistered?: (registration: ServiceWorkerRegistration | undefined) => void;
  onRegisterError?: (error: any) => void;
}

export function registerSW(options?: RegisterSWOptions): (reloadPage?: boolean) => Promise<void> {
  if (typeof window !== 'undefined' && 'serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker
        .register('/sw.js', { scope: '/' })
        .then((reg) => {
          options?.onRegistered?.(reg);
        })
        .catch((err) => {
          options?.onRegisterError?.(err);
        });
    });
  }

  return async (reloadPage?: boolean) => {
    if (reloadPage && typeof window !== 'undefined') {
      window.location.reload();
    }
  };
}

export default registerSW;
