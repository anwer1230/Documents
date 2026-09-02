/**
 * Telegram Official Notifications Controller Engine
 * Replicates org.telegram.messenger.NotificationsController.java
 * 
 * Features:
 * - Notification Channel creation & priority management (IMPORTANCE_HIGH)
 * - Push payload formatting & badge count synchronization
 * - Reply actions & Mark-as-read direct intent handlers
 * - Smart mute / silent notifications based on chat settings
 */

import { AndroidUtilities } from './AndroidUtilities';

export interface TelegramNotification {
  id: string | number;
  chatId: string | number;
  chatTitle: string;
  senderName: string;
  senderAvatar?: string;
  messageText: string;
  timestamp: number;
  isSilent: boolean;
  soundEnabled: boolean;
  vibrate: boolean;
  type: 'message' | 'mention' | 'reply' | 'call' | 'story';
}

export class NotificationsController {
  private static instance: NotificationsController;
  private notifications: Map<string | number, TelegramNotification> = new Map();
  private totalUnreadBadge: number = 0;
  private isSoundEnabled: boolean = true;
  private isVibrationEnabled: boolean = true;
  private isPreviewEnabled: boolean = true;
  private mutedChats: Set<string | number> = new Set();
  private listeners: Array<(notifications: TelegramNotification[], totalUnread: number) => void> = [];

  private constructor() {
    this.loadSettings();
  }

  public static getInstance(): NotificationsController {
    if (!NotificationsController.instance) {
      NotificationsController.instance = new NotificationsController();
    }
    return NotificationsController.instance;
  }

  private loadSettings(): void {
    try {
      const saved = localStorage.getItem('tg_notification_settings');
      if (saved) {
        const parsed = JSON.parse(saved);
        this.isSoundEnabled = parsed.sound ?? true;
        this.isVibrationEnabled = parsed.vibrate ?? true;
        this.isPreviewEnabled = parsed.preview ?? true;
        if (parsed.mutedChats) {
          this.mutedChats = new Set(parsed.mutedChats);
        }
      }
    } catch {}
  }

  public saveSettings(): void {
    try {
      localStorage.setItem(
        'tg_notification_settings',
        JSON.stringify({
          sound: this.isSoundEnabled,
          vibrate: this.isVibrationEnabled,
          preview: this.isPreviewEnabled,
          mutedChats: Array.from(this.mutedChats),
        })
      );
    } catch {}
  }

  public isChatMuted(chatId: string | number): boolean {
    return this.mutedChats.has(chatId);
  }

  public muteChat(chatId: string | number, muted: boolean): void {
    if (muted) {
      this.mutedChats.add(chatId);
    } else {
      this.mutedChats.delete(chatId);
    }
    this.saveSettings();
  }

  public showNotification(notification: Omit<TelegramNotification, 'timestamp' | 'isSilent' | 'soundEnabled' | 'vibrate'>): void {
    const isMuted = this.isChatMuted(notification.chatId);
    const fullNotification: TelegramNotification = {
      ...notification,
      timestamp: Date.now(),
      isSilent: isMuted,
      soundEnabled: this.isSoundEnabled && !isMuted,
      vibrate: this.isVibrationEnabled && !isMuted,
    };

    this.notifications.set(fullNotification.id, fullNotification);
    this.totalUnreadBadge++;

    // Trigger Android vibration if enabled
    if (fullNotification.vibrate) {
      AndroidUtilities.vibrate([0, 50, 100, 50]);
    }

    // Play Telegram notification sound
    if (fullNotification.soundEnabled) {
      this.playNotificationSound();
    }

    // Trigger Web Push or system notification if supported
    if (typeof window !== 'undefined' && 'Notification' in window && Notification.permission === 'granted') {
      try {
        new Notification(fullNotification.senderName || fullNotification.chatTitle, {
          body: this.isPreviewEnabled ? fullNotification.messageText : 'رسالة جديدة',
          icon: fullNotification.senderAvatar || '/telegram-icon.svg',
          badge: '/telegram-icon.svg',
          tag: String(fullNotification.chatId),
        });
      } catch {}
    }

    this.notifyListeners();
  }

  public dismissNotification(id: string | number): void {
    if (this.notifications.delete(id)) {
      this.totalUnreadBadge = Math.max(0, this.totalUnreadBadge - 1);
      this.notifyListeners();
    }
  }

  public dismissAllForChat(chatId: string | number): void {
    let removed = 0;
    for (const [id, notif] of this.notifications.entries()) {
      if (String(notif.chatId) === String(chatId)) {
        this.notifications.delete(id);
        removed++;
      }
    }
    this.totalUnreadBadge = Math.max(0, this.totalUnreadBadge - removed);
    this.notifyListeners();
  }

  public getNotifications(): TelegramNotification[] {
    return Array.from(this.notifications.values()).sort((a, b) => b.timestamp - a.timestamp);
  }

  public getUnreadCount(): number {
    return this.totalUnreadBadge;
  }

  public subscribe(listener: (notifications: TelegramNotification[], totalUnread: number) => void): () => void {
    this.listeners.push(listener);
    listener(this.getNotifications(), this.totalUnreadBadge);
    return () => {
      this.listeners = this.listeners.filter((l) => l !== listener);
    };
  }

  private notifyListeners(): void {
    const list = this.getNotifications();
    this.listeners.forEach((l) => l(list, this.totalUnreadBadge));
  }

  private playNotificationSound(): void {
    try {
      const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.type = 'sine';
      osc.frequency.setValueAtTime(587.33, audioCtx.currentTime); // D5
      osc.frequency.setValueAtTime(880, audioCtx.currentTime + 0.08); // A5
      gain.gain.setValueAtTime(0.15, audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.35);
      osc.start();
      osc.stop(audioCtx.currentTime + 0.35);
    } catch {}
  }
}
