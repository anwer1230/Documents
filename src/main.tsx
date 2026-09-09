import './polyfills';
import {StrictMode} from 'react';
import {createRoot} from 'react-dom/client';
import {registerSW} from 'virtual:pwa-register';
import App from './App.tsx';
import './index.css';

// Handle dynamic import / chunk loading errors automatically
window.addEventListener('error', (event) => {
  if (event?.message && (event.message.includes('Loading chunk') || event.message.includes('Failed to fetch dynamically imported module'))) {
    console.warn('Chunk load error detected, reloading to fetch latest version...');
    window.location.reload();
  }
});

// Register service-worker.js to handle background push notifications and asset caching for offline reliability
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker
      .register('/service-worker.js', { scope: '/' })
      .then((registration) => {
        console.log('[Service Worker] service-worker.js registered for offline reliability:', registration.scope);
      })
      .catch((error) => {
        console.warn('[Service Worker] service-worker.js registration error:', error);
      });
  });
}

// PWA Service Worker Registration with automatic update checks (Google AI Studio pattern)
if ('serviceWorker' in navigator) {
  const updateSW = registerSW({
    immediate: true,
    onNeedRefresh() {
      console.log('[PWA] New content available. Update ready in background.');
      // Do NOT forcefully reload the window while the user is typing their login number or interacting
    },
    onOfflineReady() {
      console.log('[PWA] App is ready to work offline.');
    },
    onRegistered(registration) {
      console.log('[PWA] Service Worker registered successfully:', registration?.scope);
      // Periodically check for SW updates every 30 minutes
      if (registration) {
        setInterval(() => {
          registration.update().catch((err) => {
            console.warn('[PWA] Error checking for SW update:', err);
          });
        }, 30 * 60 * 1000);
      }
    },
    onRegisterError(error) {
      console.warn('[PWA] Service Worker registration failed:', error);
    },
  });

  // Background Web Push & Real-Time Sync initialization
  import('./services/WebPushManager').then(({ webPushManager }) => {
    webPushManager.initSSEListener();
    webPushManager.onSessionRevoked((reason) => {
      console.warn('[PWA] Remote forced logout received via Web Push / SSE:', reason);
      window.dispatchEvent(new CustomEvent('telegram:session_revoked', { detail: { reason } }));
    });
  }).catch(() => {});
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
