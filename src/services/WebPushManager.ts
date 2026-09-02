/**
 * WebPushManager.ts
 *
 * Professional Web Push & VAPID Subscription Manager with Background Synchronization
 * - Handles VAPID public key acquisition and Base64 URL Uint8Array conversion
 * - Registers and manages PushManager subscriptions in navigator.serviceWorker
 * - Synchronizes Push subscriptions with the backend (/api/web-push/subscribe)
 * - Listens for Real-Time SSE updates (SESSION_REVOKED, AUTH_KEY_UNREGISTERED, SETTINGS_SYNCED)
 * - Automatically purges local session and redirects to login when a revocation event is received
 */

export interface WebPushSyncStatus {
  isSupported: boolean;
  permission: NotificationPermission;
  isSubscribed: boolean;
  subscription: PushSubscription | null;
  vapidPublicKey: string | null;
}

type RevocationCallback = (reason: string) => void;
type SettingsSyncCallback = (settings: any) => void;

class WebPushManager {
  private static instance: WebPushManager;
  private vapidPublicKey: string | null = null;
  private sseEventSource: EventSource | null = null;
  private revocationListeners: Set<RevocationCallback> = new Set();
  private settingsSyncListeners: Set<SettingsSyncCallback> = new Set();
  private isConnectingSSE = false;

  public static getInstance(): WebPushManager {
    if (!WebPushManager.instance) {
      WebPushManager.instance = new WebPushManager();
    }
    return WebPushManager.instance;
  }

  private constructor() {
    if (typeof window !== 'undefined') {
      this.initSSEListener();
    }
  }

  /**
   * Helper to convert Base64 URL-safe string to Uint8Array (required by PushManager)
   */
  private urlBase64ToUint8Array(base64String: string): Uint8Array {
    const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
    const base64 = (base64String + padding).replace(/\-/g, '+').replace(/_/g, '/');
    const rawData = window.atob(base64);
    const outputArray = new Uint8Array(rawData.length);
    for (let i = 0; i < rawData.length; ++i) {
      outputArray[i] = rawData.charCodeAt(i);
    }
    return outputArray;
  }

  /**
   * Fetches the server VAPID public key from backend
   */
  public async getVapidPublicKey(): Promise<string | null> {
    if (this.vapidPublicKey) {
      return this.vapidPublicKey;
    }

    try {
      const res = await fetch('/api/web-push/vapid-public-key');
      if (res.ok) {
        const data = await res.json();
        if (data.success && data.publicKey) {
          this.vapidPublicKey = data.publicKey;
          return data.publicKey;
        }
      }
    } catch (err) {
      console.warn('[WebPushManager] Could not fetch VAPID public key from server:', err);
    }
    return null;
  }

  /**
   * Checks current Web Push support and subscription status
   */
  public async getStatus(): Promise<WebPushSyncStatus> {
    if (typeof window === 'undefined' || !('serviceWorker' in navigator) || !('PushManager' in window)) {
      return {
        isSupported: false,
        permission: 'denied',
        isSubscribed: false,
        subscription: null,
        vapidPublicKey: null,
      };
    }

    const permission = Notification.permission;
    let subscription: PushSubscription | null = null;
    let isSubscribed = false;

    try {
      const registration = await navigator.serviceWorker.ready;
      subscription = await registration.pushManager.getSubscription();
      isSubscribed = Boolean(subscription);
    } catch (err) {
      console.warn('[WebPushManager] getStatus pushManager error:', err);
    }

    return {
      isSupported: true,
      permission,
      isSubscribed,
      subscription,
      vapidPublicKey: this.vapidPublicKey,
    };
  }

  /**
   * Requests Notification Permission and registers a PushSubscription with the backend
   */
  public async subscribeUserToPush(metadata?: { phone?: string; sessionString?: string; accountId?: string }): Promise<PushSubscription | null> {
    if (typeof window === 'undefined' || !('serviceWorker' in navigator) || !('PushManager' in window)) {
      console.warn('[WebPushManager] Push messaging is not supported in this browser.');
      return null;
    }

    try {
      // 1. Request notification permission if not yet granted
      const permission = await Notification.requestPermission();
      if (permission !== 'granted') {
        console.warn('[WebPushManager] Notification permission denied by user.');
        return null;
      }

      // 2. Fetch VAPID Public Key from server
      const publicKey = await this.getVapidPublicKey();
      if (!publicKey) {
        console.warn('[WebPushManager] Cannot subscribe without a valid VAPID public key.');
        return null;
      }

      // 3. Register or reuse service worker registration
      const registration = await navigator.serviceWorker.ready;

      // 4. Check existing subscription or create new
      let subscription = await registration.pushManager.getSubscription();
      if (!subscription) {
        const convertedVapidKey = this.urlBase64ToUint8Array(publicKey);
        subscription = await registration.pushManager.subscribe({
          userVisibleOnly: true,
          applicationServerKey: convertedVapidKey,
        });
      }

      // 5. Send Subscription to backend endpoint (/api/web-push/subscribe)
      const subJson = subscription.toJSON();
      await fetch('/api/web-push/subscribe', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          subscription: subJson,
          phone: metadata?.phone,
          sessionString: metadata?.sessionString,
          accountId: metadata?.accountId,
          userAgent: navigator.userAgent,
        }),
      });

      console.log('[WebPushManager] User successfully subscribed to Web Push notifications!');
      return subscription;
    } catch (err) {
      console.warn('[WebPushManager] Failed to subscribe to Web Push:', err);
      return null;
    }
  }

  /**
   * Unsubscribes the user from Web Push
   */
  public async unsubscribeUser(): Promise<boolean> {
    try {
      if (!('serviceWorker' in navigator)) return false;
      const registration = await navigator.serviceWorker.ready;
      const subscription = await registration.pushManager.getSubscription();

      if (subscription) {
        await fetch('/api/web-push/unsubscribe', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ endpoint: subscription.endpoint }),
        }).catch(() => {});

        return await subscription.unsubscribe();
      }
      return true;
    } catch (err) {
      console.warn('[WebPushManager] Failed to unsubscribe:', err);
      return false;
    }
  }

  /**
   * Sends a test Web Push notification via the server
   */
  public async sendTestPushNotification(): Promise<boolean> {
    try {
      const res = await fetch('/api/web-push/test', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          title: 'تيليجرام: إشعار دفع خلفي (Web Push)',
          body: 'تم استلام الإشعار بنجاح عبر Web Push API حتى في حال إغلاق المتصفح!',
          data: { timestamp: Date.now(), url: '/' },
        }),
      });
      const data = await res.json();
      return Boolean(data.success);
    } catch (err) {
      console.warn('[WebPushManager] Test push error:', err);
      return false;
    }
  }

  /**
   * Initializes SSE stream for real-time events (Session Revocation & Settings Sync)
   */
  public initSSEListener(): void {
    if (this.sseEventSource || this.isConnectingSSE || typeof window === 'undefined') return;
    this.isConnectingSSE = true;

    try {
      const es = new EventSource('/api/telegram/updates/stream');
      this.sseEventSource = es;

      es.onopen = () => {
        this.isConnectingSSE = false;
        console.log('[WebPushManager] Real-time SSE event stream connected.');
      };

      es.onmessage = (event) => {
        try {
          if (!event.data || event.data.startsWith(':')) return;
          const payload = JSON.parse(event.data);

          // 1. Session Revocation / Unregistered event
          if (
            payload.type === 'SESSION_REVOKED' ||
            payload.type === 'AUTH_KEY_UNREGISTERED' ||
            (payload.type === 'updateNewAuthorization' && payload.is_current_revoked)
          ) {
            console.warn('[WebPushManager] SESSION_REVOKED event received from server:', payload);
            this.handleLocalSessionPurge(payload.reason || 'SESSION_REVOKED');
          }

          // 2. Settings Synchronized event
          if (payload.type === 'SETTINGS_SYNCED' && payload.settings) {
            this.settingsSyncListeners.forEach((cb) => cb(payload.settings));
          }
        } catch (_) {}
      };

      es.onerror = () => {
        this.isConnectingSSE = false;
        if (es.readyState === EventSource.CLOSED) {
          this.sseEventSource = null;
          // Reconnect with backoff
          setTimeout(() => this.initSSEListener(), 10000);
        }
      };
    } catch (e) {
      this.isConnectingSSE = false;
      console.warn('[WebPushManager] SSE Init note:', e);
    }
  }

  /**
   * Purges local session from storage and notifies listeners
   */
  public handleLocalSessionPurge(reason: string = 'SESSION_REVOKED'): void {
    try {
      // Clear localStorage session items
      localStorage.removeItem('tg_session_string');
      localStorage.removeItem('telegram_session');
      localStorage.removeItem('tg_auth_user');
      localStorage.removeItem('tg_user_profile');
      sessionStorage.clear();

      // Notify registered UI callbacks
      this.revocationListeners.forEach((cb) => cb(reason));

      // Post message to Service Worker
      if (navigator.serviceWorker?.controller) {
        navigator.serviceWorker.controller.postMessage({
          type: 'SESSION_REVOKED',
          reason,
        });
      }
    } catch (e) {
      console.warn('[WebPushManager] Purge local session error:', e);
    }
  }

  /**
   * Register revocation event callback
   */
  public onSessionRevoked(callback: RevocationCallback): () => void {
    this.revocationListeners.add(callback);
    return () => this.revocationListeners.delete(callback);
  }

  /**
   * Register settings sync event callback
   */
  public onSettingsSynced(callback: SettingsSyncCallback): () => void {
    this.settingsSyncListeners.add(callback);
    return () => this.settingsSyncListeners.delete(callback);
  }
}

export const webPushManager = WebPushManager.getInstance();
