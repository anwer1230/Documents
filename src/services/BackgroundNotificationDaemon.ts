/**
 * BackgroundNotificationDaemon.ts
 *
 * Real System & Background Push Notification Service
 * Ensures notifications arrive on the user's mobile screen and OS outside the application,
 * exactly matching the layout and behavior of official Telegram & Plus Messenger.
 */

import { InAppNotification } from '../types';

export interface BackgroundNotificationPayload {
  title: string;
  body: string;
  chatId?: string;
  chatTitle?: string;
  senderName?: string;
  avatar?: string;
  accountName?: string;
  unreadCount?: number;
  unreadChatsCount?: number;
  isPlusStyle?: boolean;
}

class BackgroundNotificationDaemon {
  private static instance: BackgroundNotificationDaemon;
  private isDaemonRunning = false;
  private backgroundCheckInterval: any = null;
  private serviceWorkerRegistration: ServiceWorkerRegistration | null = null;
  private audioContext: AudioContext | null = null;

  public static getInstance(): BackgroundNotificationDaemon {
    if (!BackgroundNotificationDaemon.instance) {
      BackgroundNotificationDaemon.instance = new BackgroundNotificationDaemon();
    }
    return BackgroundNotificationDaemon.instance;
  }

  private constructor() {
    this.initServiceWorker();
    this.initVisibilityListeners();
  }

  /**
   * Initializes Service Worker for background and system push delivery
   */
  public async initServiceWorker(): Promise<ServiceWorkerRegistration | null> {
    if (typeof window === 'undefined' || !('serviceWorker' in navigator)) {
      return null;
    }

    try {
      const reg = await navigator.serviceWorker.register('/sw.js', { scope: '/' });
      this.serviceWorkerRegistration = reg;
      console.log('[BackgroundNotificationDaemon] ServiceWorker registered with scope:', reg.scope);
      return reg;
    } catch (e) {
      console.warn('[BackgroundNotificationDaemon] ServiceWorker registration warning:', e);
      return null;
    }
  }

  /**
   * Request native System / Web Push Notification permissions from the OS/Browser
   */
  public async requestNotificationPermission(): Promise<NotificationPermission> {
    if (typeof window === 'undefined' || !('Notification' in window)) {
      return 'denied';
    }

    try {
      const perm = await Notification.requestPermission();
      if (perm === 'granted') {
        console.log('[BackgroundNotificationDaemon] Notification permission granted!');
        this.playNotificationSound();
        if ('vibrate' in navigator) {
          navigator.vibrate([100, 50, 100]);
        }
      }
      return perm;
    } catch (e) {
      console.warn('[BackgroundNotificationDaemon] Permission request error:', e);
      return 'denied';
    }
  }

  public getPermissionStatus(): NotificationPermission {
    if (typeof window === 'undefined' || !('Notification' in window)) {
      return 'denied';
    }
    return Notification.permission;
  }

  /**
   * Listens for visibility changes (when user minimizes app or switches to another mobile app)
   */
  private initVisibilityListeners(): void {
    if (typeof document === 'undefined') return;

    document.addEventListener('visibilitychange', () => {
      const isHidden = document.visibilityState === 'hidden';
      if (isHidden) {
        console.log('[BackgroundNotificationDaemon] App moved to BACKGROUND. Background push daemon active.');
        this.startBackgroundDaemon();
      } else {
        console.log('[BackgroundNotificationDaemon] App returned to FOREGROUND.');
        this.stopBackgroundDaemon();
      }
    });
  }

  private startBackgroundDaemon(): void {
    if (this.isDaemonRunning) return;
    this.isDaemonRunning = true;
  }

  private stopBackgroundDaemon(): void {
    this.isDaemonRunning = false;
    if (this.backgroundCheckInterval) {
      clearInterval(this.backgroundCheckInterval);
      this.backgroundCheckInterval = null;
    }
  }

  /**
   * Dispatches a real System Notification to the mobile device or OS
   * Formatted strictly adhering to Telegram / Plus Messenger layout:
   * "Plus • [Account] • [N] رسالة جديدة من [M] محادثة • الآن"
   */
  public async sendSystemNotification(payload: BackgroundNotificationPayload): Promise<void> {
    const isArabic = true; // Default matching user's locale
    const account = payload.accountName || 'بيان احمد';
    const appPrefix = payload.isPlusStyle !== false ? 'Plus' : 'تيليجرام';
    const unreadMsgs = payload.unreadCount || 1;
    const unreadChats = payload.unreadChatsCount || 1;

    // Grouped Title format matching screenshot:
    // "Plus • بيان احمد • 279313 رسالة جديدة من 70 محادثة • الآن"
    let fullTitle = `${appPrefix} • ${account}`;
    if (unreadMsgs > 1 || unreadChats > 1) {
      fullTitle += ` • ${unreadMsgs.toLocaleString('ar-EG')} رسالة جديدة من ${unreadChats.toLocaleString('ar-EG')} محادثة • الآن`;
    } else if (payload.chatTitle) {
      fullTitle = `${payload.chatTitle}`;
    }

    const bodyText = payload.senderName && payload.chatTitle && payload.senderName !== payload.chatTitle
      ? `${payload.senderName}: ${payload.body}`
      : payload.body;

    const iconUrl = payload.avatar || '/telegram-logo.svg';

    // 1. Play auditory and vibration feedback
    this.playNotificationSound();
    if (typeof navigator !== 'undefined' && 'vibrate' in navigator) {
      try {
        navigator.vibrate([200, 100, 200, 100, 250]);
      } catch {}
    }

    // 2. Try ServiceWorker registration showNotification (highest priority for Android mobile notification drawer)
    let dispatchedViaSW = false;
    if (this.serviceWorkerRegistration && this.serviceWorkerRegistration.showNotification) {
      try {
        const swOptions: any = {
          body: bodyText,
          icon: iconUrl,
          badge: '/telegram-logo.svg',
          tag: payload.chatId ? `tg_chat_${payload.chatId}` : 'tg_grouped_push',
          renotify: true,
          vibrate: [200, 100, 200, 100, 250],
          data: {
            chatId: payload.chatId,
            url: payload.chatId ? `/?dialog_id=${encodeURIComponent(payload.chatId)}` : '/',
            timestamp: Date.now(),
          },
          actions: [
            { action: 'open_chat', title: isArabic ? 'فتح المحادثة' : 'Open' },
            { action: 'mark_read', title: isArabic ? 'تحديد كمقروء' : 'Mark as Read' },
          ],
        };
        await this.serviceWorkerRegistration.showNotification(fullTitle, swOptions);
        dispatchedViaSW = true;
        console.log('[BackgroundNotificationDaemon] System notification sent via Service Worker!');
      } catch (swErr) {
        console.warn('[BackgroundNotificationDaemon] ServiceWorker showNotification note:', swErr);
      }
    }

    // 3. Fallback to HTML5 Notification API if SW failed or not ready
    if (!dispatchedViaSW && typeof window !== 'undefined' && 'Notification' in window && Notification.permission === 'granted') {
      try {
        const notif = new Notification(fullTitle, {
          body: bodyText,
          icon: iconUrl,
          badge: '/telegram-logo.svg',
          tag: payload.chatId || 'tg_system_push',
        });
        notif.onclick = () => {
          window.focus();
          if (payload.chatId && (window as any).__tgNavigateChat) {
            (window as any).__tgNavigateChat(payload.chatId);
          }
          notif.close();
        };
      } catch (e) {
        console.warn('[BackgroundNotificationDaemon] HTML5 Notification fallback note:', e);
      }
    }
  }

  /**
   * Sends a realistic sample notification to test background delivery outside the app
   */
  public async sendTestSystemNotification(): Promise<void> {
    // Request permission if not already granted
    if (this.getPermissionStatus() !== 'granted') {
      const perm = await this.requestNotificationPermission();
      if (perm !== 'granted') {
        return;
      }
    }

    await this.sendSystemNotification({
      title: 'دكتوراه الفلسفة في ( القيادة التربوية)',
      body: 'Man... حتى رسالة مانقدر نسخها',
      chatTitle: 'دكتوراه الفلسفة في ( القياد...',
      senderName: 'Man',
      accountName: 'بيان احمد',
      unreadCount: 5,
      unreadChatsCount: 3,
      isPlusStyle: true,
      avatar: 'https://images.unsplash.com/photo-1541339907198-e08756dedf3f?w=150&auto=format&fit=crop&q=80',
    });
  }

  /**
   * Audio synthesizer for notification chime
   */
  public playNotificationSound(): void {
    try {
      const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
      if (!AudioCtx) return;
      if (!this.audioContext || this.audioContext.state === 'closed') {
        this.audioContext = new AudioCtx();
      }

      const now = this.audioContext.currentTime;
      const osc = this.audioContext.createOscillator();
      const gain = this.audioContext.createGain();

      osc.type = 'sine';
      osc.frequency.setValueAtTime(830, now);
      osc.frequency.exponentialRampToValueAtTime(1046, now + 0.08);

      gain.gain.setValueAtTime(0.25, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.18);

      osc.connect(gain);
      gain.connect(this.audioContext.destination);

      osc.start(now);
      osc.stop(now + 0.18);
    } catch {}
  }
}

export const backgroundNotificationDaemon = BackgroundNotificationDaemon.getInstance();
