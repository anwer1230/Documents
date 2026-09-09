/**
 * ServiceWorkerManager.ts
 * 
 * Service Worker Manager for Background Notifications & Cache Management
 * Ensures high reliability when the application is active or running in the background.
 */

export interface CacheStats {
  cachesCount: number;
  cacheNames: string[];
  totalCachedEntries: number;
}

class ServiceWorkerManager {
  private static instance: ServiceWorkerManager;
  private registration: ServiceWorkerRegistration | null = null;
  private isReady = false;

  public static getInstance(): ServiceWorkerManager {
    if (!ServiceWorkerManager.instance) {
      ServiceWorkerManager.instance = new ServiceWorkerManager();
    }
    return ServiceWorkerManager.instance;
  }

  private constructor() {
    if (typeof window !== "undefined" && "serviceWorker" in navigator) {
      this.initRegistration();
    }
  }

  /**
   * Initializes and acquires the active Service Worker registration
   */
  public async initRegistration(): Promise<ServiceWorkerRegistration | null> {
    if (typeof window === "undefined" || !("serviceWorker" in navigator)) {
      return null;
    }

    try {
      this.registration = await navigator.serviceWorker.ready;
      this.isReady = true;
      console.log("[SWManager] Service Worker registration ready:", this.registration.scope);

      // Listen for updates or messages from the Service Worker
      navigator.serviceWorker.addEventListener("message", (event) => {
        const { data } = event;
        if (data?.type === "BACKGROUND_PUSH_RECEIVED") {
          console.log("[SWManager] Background push notification received by SW:", data.payload);
        } else if (data?.type === "BACKGROUND_SYNC_COMPLETED") {
          console.log("[SWManager] Background sync completed for tag:", data.tag);
        }
      });

      return this.registration;
    } catch (err) {
      console.warn("[SWManager] Failed to obtain SW registration:", err);
      return null;
    }
  }

  /**
   * Triggers a native background notification through the Service Worker.
   * Works even if the tab is blurred or running in background.
   */
  public async triggerBackgroundNotification(payload: {
    title: string;
    body: string;
    icon?: string;
    badge?: string;
    tag?: string;
    data?: any;
    actions?: Array<{ action: string; title: string }>;
  }): Promise<boolean> {
    try {
      const reg = this.registration || (await this.initRegistration());
      if (reg && "showNotification" in reg) {
        await reg.showNotification(payload.title, {
          body: payload.body,
          icon: payload.icon || "/icon-192.png",
          badge: payload.badge || "/icon-192.png",
          tag: payload.tag || "tg_bg_notification",
          data: payload.data || {},
          vibrate: [200, 100, 200],
          actions: payload.actions || [
            { action: "open_chat", title: "Open Chat" },
            { action: "mark_read", title: "Mark Read" },
          ],
        } as NotificationOptions & { vibrate?: number[] });
        return true;
      }

      // Fallback: Post message to active SW controller
      if (navigator.serviceWorker.controller) {
        navigator.serviceWorker.controller.postMessage({
          type: "TRIGGER_BACKGROUND_NOTIFICATION",
          notification: payload,
        });
        return true;
      }

      return false;
    } catch (err) {
      console.warn("[SWManager] Error showing background notification:", err);
      return false;
    }
  }

  /**
   * Enqueues a Background Sync tag ensuring offline actions complete
   * reliably when network connectivity is restored.
   */
  public async registerBackgroundSync(tag: string = "tg_messages_sync"): Promise<boolean> {
    try {
      const reg = this.registration || (await this.initRegistration());
      if (reg && "sync" in reg) {
        await (reg as any).sync.register(tag);
        console.log(`[SWManager] Background sync registered for tag: ${tag}`);
        return true;
      }
      return false;
    } catch (err) {
      console.warn(`[SWManager] Background sync registration not supported or failed for ${tag}:`, err);
      return false;
    }
  }

  /**
   * Cache Management: Retrieves cache statistics
   */
  public async getCacheStats(): Promise<CacheStats> {
    if (typeof window === "undefined" || !("caches" in window)) {
      return { cachesCount: 0, cacheNames: [], totalCachedEntries: 0 };
    }

    try {
      const cacheNames = await caches.keys();
      let totalEntries = 0;

      for (const name of cacheNames) {
        const cache = await caches.open(name);
        const keys = await cache.keys();
        totalEntries += keys.length;
      }

      return {
        cachesCount: cacheNames.length,
        cacheNames,
        totalCachedEntries: totalEntries,
      };
    } catch (err) {
      console.warn("[SWManager] Error getting cache stats:", err);
      return { cachesCount: 0, cacheNames: [], totalCachedEntries: 0 };
    }
  }

  /**
   * Cache Management: Purges outdated caches, preserving current version
   */
  public async purgeOutdatedCaches(currentVersionPattern: string = "v13"): Promise<number> {
    if (typeof window === "undefined" || !("caches" in window)) {
      return 0;
    }

    try {
      const cacheNames = await caches.keys();
      let deletedCount = 0;

      await Promise.all(
        cacheNames.map(async (name) => {
          if (!name.includes(currentVersionPattern)) {
            await caches.delete(name);
            deletedCount++;
            console.log(`[SWManager] Purged outdated cache: ${name}`);
          }
        })
      );

      // Notify service worker to claim clients and purge
      if (navigator.serviceWorker.controller) {
        navigator.serviceWorker.controller.postMessage({
          type: "CACHE_MANAGEMENT_PURGE_OUTDATED",
        });
      }

      return deletedCount;
    } catch (err) {
      console.warn("[SWManager] Error purging outdated caches:", err);
      return 0;
    }
  }

  /**
   * Cache Management: Clears all caches
   */
  public async clearAllCaches(): Promise<boolean> {
    if (typeof window === "undefined" || !("caches" in window)) {
      return false;
    }

    try {
      const cacheNames = await caches.keys();
      await Promise.all(cacheNames.map((name) => caches.delete(name)));
      console.log("[SWManager] All caches successfully cleared.");

      if (navigator.serviceWorker.controller) {
        navigator.serviceWorker.controller.postMessage({
          type: "CACHE_MANAGEMENT_CLEAR_ALL",
        });
      }

      return true;
    } catch (err) {
      console.warn("[SWManager] Error clearing all caches:", err);
      return false;
    }
  }

  /**
   * Cache Management: Pre-caches critical URLs for offline reliability
   */
  public async precacheUrls(urls: string[]): Promise<boolean> {
    if (typeof window === "undefined" || !("caches" in window)) {
      return false;
    }

    try {
      const cache = await caches.open("telegram-runtime-v13.0.0");
      await cache.addAll(urls);
      console.log(`[SWManager] Pre-cached ${urls.length} URLs successfully.`);
      return true;
    } catch (err) {
      console.warn("[SWManager] Error pre-caching URLs:", err);
      return false;
    }
  }
}

export const serviceWorkerManager = ServiceWorkerManager.getInstance();
