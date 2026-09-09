/**
 * Service Worker: service-worker.js
 * 
 * Handles:
 * 1. Background push notifications (FCM / Web Push / Custom payload)
 * 2. Notification click routing & client window focus
 * 3. Pre-caching and runtime asset caching for offline reliability
 * 4. Resilient network fallback for navigation and static resources
 */

const CACHE_NAME = 'app-cache-v1';
const STATIC_ASSETS = [
  '/',
  '/index.html',
  '/manifest.json',
  '/telegram-logo.svg',
  '/icon-192.png',
  '/icon-512.png',
  '/apple-touch-icon.png',
];

// ============================================================================
// 1. INSTALL & ACTIVATE LIFECYCLE
// ============================================================================

self.addEventListener('install', (event) => {
  console.log('[Service Worker] Install event triggered');
  event.waitUntil(
    caches
      .open(CACHE_NAME)
      .then(async (cache) => {
        console.log('[Service Worker] Pre-caching static assets for offline reliability');
        // Resilient precache: fetch each asset safely without failing the entire install
        await Promise.allSettled(
          STATIC_ASSETS.map(async (url) => {
            try {
              const response = await fetch(url);
              if (response && response.ok) {
                await cache.put(url, response);
              }
            } catch (err) {
              console.warn(`[Service Worker] Optional precache skipped for ${url}:`, err);
            }
          })
        );
      })
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  console.log('[Service Worker] Activate event triggered');
  event.waitUntil(
    caches
      .keys()
      .then((cacheNames) => {
        return Promise.all(
          cacheNames.map((cacheName) => {
            if (cacheName !== CACHE_NAME) {
              console.log('[Service Worker] Purging legacy cache:', cacheName);
              return caches.delete(cacheName);
            }
          })
        );
      })
      .then(() => self.clients.claim())
  );
});

// ============================================================================
// 2. ASSET CACHING & OFFLINE FETCH HANDLER
// ============================================================================

self.addEventListener('fetch', (event) => {
  const { request } = event;

  // Only handle GET requests
  if (request.method !== 'GET') {
    return;
  }

  const url = new URL(request.url);

  // Ignore non-http/https requests (e.g. chrome-extension://)
  if (!url.protocol.startsWith('http')) {
    return;
  }

  // A. Navigation (HTML document requests): Network-first with cache fallback
  if (request.mode === 'navigate' || request.headers.get('accept')?.includes('text/html')) {
    event.respondWith(
      fetch(request)
        .then((networkResponse) => {
          if (networkResponse && networkResponse.ok) {
            const responseClone = networkResponse.clone();
            caches.open(CACHE_NAME).then((cache) => {
              cache.put(request, responseClone);
            });
          }
          return networkResponse;
        })
        .catch(async () => {
          console.log('[Service Worker] Offline: Serving cached HTML fallback');
          const cachedResponse = await caches.match(request);
          if (cachedResponse) return cachedResponse;

          const rootFallback = await caches.match('/');
          if (rootFallback) return rootFallback;

          const indexFallback = await caches.match('/index.html');
          if (indexFallback) return indexFallback;

          return new Response(
            '<!DOCTYPE html><html><head><meta charset="utf-8"/><title>Offline</title></head><body style="font-family:sans-serif;background:#121212;color:#eee;display:flex;align-items:center;justify-content:center;height:100vh;margin:0;"><div style="text-align:center;padding:24px;"><h2>You are offline</h2><p>Please check your internet connection and reload.</p></div></body></html>',
            { headers: { 'Content-Type': 'text/html' } }
          );
        })
    );
    return;
  }

  // B. Static Assets (Scripts, Styles, Images, Fonts, WASM): Stale-While-Revalidate / Cache-First
  const isStaticAsset =
    url.pathname.match(/\.(js|css|png|jpg|jpeg|svg|webp|wasm|woff|woff2|ico|json)$/i) ||
    url.origin === self.location.origin;

  if (isStaticAsset) {
    event.respondWith(
      caches.match(request).then((cachedResponse) => {
        const fetchPromise = fetch(request)
          .then((networkResponse) => {
            if (networkResponse && networkResponse.ok) {
              const clone = networkResponse.clone();
              caches.open(CACHE_NAME).then((cache) => {
                cache.put(request, clone);
              });
            }
            return networkResponse;
          })
          .catch(() => {
            // Network failure is acceptable if cache hit exists
            return null;
          });

        return cachedResponse || fetchPromise;
      })
    );
    return;
  }

  // C. Default: Network with cache fallback
  event.respondWith(
    fetch(request).catch(async () => {
      return (await caches.match(request)) || Response.error();
    })
  );
});

// ============================================================================
// 3. BACKGROUND PUSH NOTIFICATIONS
// ============================================================================

self.addEventListener('push', (event) => {
  console.log('[Service Worker] Push event received');

  let payload = {
    title: 'New Notification',
    body: 'You have received a new update.',
    icon: '/icon-192.png',
    badge: '/icon-192.png',
    tag: 'default-notification',
    data: {
      url: '/',
      timestamp: Date.now(),
    },
    actions: [
      { action: 'open', title: 'Open' },
      { action: 'dismiss', title: 'Dismiss' },
    ],
  };

  if (event.data) {
    try {
      const parsed = event.data.json();
      payload = {
        ...payload,
        ...parsed,
        data: {
          ...payload.data,
          ...(parsed.data || {}),
        },
      };
    } catch {
      const text = event.data.text();
      if (text) {
        payload.body = text;
      }
    }
  }

  const notificationOptions = {
    body: payload.body,
    icon: payload.icon || '/icon-192.png',
    badge: payload.badge || '/icon-192.png',
    tag: payload.tag || `push-${Date.now()}`,
    data: payload.data || { url: '/' },
    vibrate: [100, 50, 100],
    actions: payload.actions || [
      { action: 'open', title: 'Open' },
      { action: 'dismiss', title: 'Dismiss' },
    ],
    renotify: true,
    requireInteraction: false,
  };

  event.waitUntil(
    self.registration.showNotification(payload.title, notificationOptions)
  );
});

// ============================================================================
// 4. NOTIFICATION INTERACTION ROUTING
// ============================================================================

self.addEventListener('notificationclick', (event) => {
  console.log('[Service Worker] Notification click received:', event.action);
  event.notification.close();

  // If user clicked explicit dismiss action
  if (event.action === 'dismiss') {
    return;
  }

  const targetUrl = (event.notification.data && event.notification.data.url) || '/';

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      // If a window is already open, focus it and post a message
      for (const client of clientList) {
        if (client.url && 'focus' in client) {
          client.postMessage({
            type: 'NOTIFICATION_CLICKED',
            action: event.action,
            data: event.notification.data,
          });
          return client.focus();
        }
      }

      // Otherwise open a new window
      if (clients.openWindow) {
        return clients.openWindow(targetUrl);
      }
    })
  );
});

self.addEventListener('notificationclose', (event) => {
  console.log('[Service Worker] Notification was dismissed:', event.notification.tag);
});

// ============================================================================
// 5. CLIENT COMMUNICATION & BACKGROUND SYNC
// ============================================================================

self.addEventListener('message', (event) => {
  if (event.data?.type === 'SKIP_WAITING') {
    self.skipWaiting();
  } else if (event.data?.type === 'CLEAR_CACHE') {
    event.waitUntil(
      caches.delete(CACHE_NAME).then(() => {
        console.log('[Service Worker] Cache cleared by client request');
      })
    );
  }
});

self.addEventListener('sync', (event) => {
  console.log('[Service Worker] Background sync triggered:', event.tag);
});
