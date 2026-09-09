import './polyfills';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { registerSW } from 'virtual:pwa-register';
import App from './App.tsx';
import './index.css';

// Guard against rapid chunk load reload loops (allow max 1 reload per 60s)
window.addEventListener('error', (event) => {
  const isChunkError =
    event?.message &&
    (event.message.includes('Loading chunk') ||
      event.message.includes('Failed to fetch dynamically imported module') ||
      event.message.includes('error loading dynamically imported module'));

  if (isChunkError) {
    const lastReload = Number(sessionStorage.getItem('last_chunk_reload') || 0);
    const now = Date.now();

    if (now - lastReload < 60000) {
      console.warn('[App] Chunk error occurred, reload loop suppressed safely.');
      return;
    }

    sessionStorage.setItem('last_chunk_reload', String(now));
    console.warn('[App] Dynamic chunk missing, reloading once to synchronize latest assets...');
    window.location.reload();
  }
});

// PWA Service Worker Registration & Web Push Init (Unified & Safe)
if ('serviceWorker' in navigator) {
  // Clean up any stale or conflicting standalone registrations
  navigator.serviceWorker.getRegistrations().then((registrations) => {
    for (const reg of registrations) {
      if (reg.active?.scriptURL.includes('service-worker.js') && reg.active?.scriptURL.includes('sw.js')) {
        reg.unregister().catch(() => {});
      }
    }
  }).catch(() => {});

  // Register PWA service worker smoothly without abrupt reloads
  registerSW({
    immediate: false,
    onNeedRefresh() {
      console.log('[PWA] New version ready in background.');
      window.dispatchEvent(new CustomEvent('pwa:update_available'));
    },
    onOfflineReady() {
      console.log('[PWA] App is ready for offline usage.');
    },
    onRegisterError(error) {
      console.warn('[PWA] Service Worker registration failed:', error);
    },
  });

  // Background Web Push & Real-Time Sync initialization after window load
  window.addEventListener('load', () => {
    import('./services/WebPushManager').then(({ webPushManager }) => {
      webPushManager.initSSEListener();
      webPushManager.onSessionRevoked((reason) => {
        console.warn('[PWA] Remote forced logout received via Web Push / SSE:', reason);
        window.dispatchEvent(new CustomEvent('telegram:session_revoked', { detail: { reason } }));
      });
    }).catch(() => {});
  });
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
