/**
 * Desktop Browser Notifications Manager
 * Handles HTML5 Notification API permission and dispatch
 */

export async function requestNotificationPermission(): Promise<NotificationPermission> {
  if (typeof window === 'undefined' || !('Notification' in window)) {
    return 'denied';
  }
  if (Notification.permission === 'default') {
    try {
      return await Notification.requestPermission();
    } catch (e) {
      console.warn('[DesktopNotifications] Request permission error:', e);
    }
  }
  return Notification.permission;
}

export interface DesktopNotificationOptions {
  title: string;
  body: string;
  icon?: string;
  tag?: string;
  onClick?: () => void;
}

export function showDesktopNotification(options: DesktopNotificationOptions): Notification | null {
  if (typeof window === 'undefined' || !('Notification' in window)) {
    return null;
  }

  if (Notification.permission !== 'granted') {
    return null;
  }

  try {
    const notif = new Notification(options.title, {
      body: options.body || 'رسالة جديدة في تيليجرام',
      icon: options.icon || 'https://telegram.org/img/t_logo.png',
      badge: 'https://telegram.org/img/t_logo.png',
      tag: options.tag,
      silent: true, // Audio is handled via synthesized chime
    });

    notif.onclick = () => {
      try {
        window.focus();
      } catch (_) {}
      if (options.onClick) {
        options.onClick();
      }
      notif.close();
    };

    // Auto close desktop notification after 6 seconds
    setTimeout(() => {
      try {
        notif.close();
      } catch (_) {}
    }, 6000);

    return notif;
  } catch (err) {
    console.warn('[DesktopNotifications] Notification display error:', err);
    return null;
  }
}
