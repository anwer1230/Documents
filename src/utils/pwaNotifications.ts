/**
 * pwaNotifications.ts
 *
 * PWA Web Push & Notification Helper
 * Provides permission requesting and native OS notification dispatching.
 */

export async function requestNotificationPermission(): Promise<NotificationPermission> {
  if (typeof window === 'undefined' || !('Notification' in window)) {
    console.warn('[PWA] Notifications not supported in this browser.');
    return 'denied';
  }

  if (Notification.permission === 'granted') {
    return 'granted';
  }

  try {
    const permission = await Notification.requestPermission();
    console.log('[PWA] Notification permission status:', permission);
    return permission;
  } catch (err) {
    console.error('[PWA] Error requesting notification permission:', err);
    return 'denied';
  }
}

export function sendWebNotification(
  title: string,
  options?: {
    body?: string;
    icon?: string;
    badge?: string;
    tag?: string;
    data?: any;
    onClick?: () => void;
  }
): Notification | null {
  if (typeof window === 'undefined' || !('Notification' in window)) {
    return null;
  }

  if (Notification.permission !== 'granted') {
    return null;
  }

  try {
    const notif = new Notification(title, {
      body: options?.body,
      icon: options?.icon || 'https://telegram.org/img/t_logo.png',
      badge: options?.badge || 'https://telegram.org/img/t_logo.png',
      tag: options?.tag || 'telegram_notification',
      data: options?.data,
    });

    if (options?.onClick) {
      notif.onclick = () => {
        window.focus();
        options.onClick?.();
        notif.close();
      };
    }

    return notif;
  } catch (err) {
    console.warn('[PWA] Notification dispatch exception:', err);
    return null;
  }
}
