import './polyfills';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { registerSW } from 'virtual:pwa-register';
import App from './App.tsx';
import './index.css';

// Guard against chunk load errors without forced reloads
window.addEventListener('error', (event) => {
  const isChunkError =
    event?.message &&
    (event.message.includes('Loading chunk') ||
      event.message.includes('Failed to fetch dynamically imported module') ||
      event.message.includes('error loading dynamically imported module'));

  if (isChunkError) {
    console.warn('[App] Chunk error occurred, suppressed safely to avoid unwanted reload loops.');
  }
});

// PWA Service Worker Registration & Web Push Init (Unified & Safe via virtual:pwa-register)
if ('serviceWorker' in navigator) {
  // Clean up any stale or conflicting legacy service workers (e.g., service-worker.js)
  navigator.serviceWorker.getRegistrations().then((registrations) => {
    for (const reg of registrations) {
      if (reg.active?.scriptURL.includes('service-worker.js')) {
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
