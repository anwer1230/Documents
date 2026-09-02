/**
 * Telegram Official Android Utilities (Theme & Metrics Engine)
 * Replicates org.telegram.messenger.AndroidUtilities & org.telegram.ui.ActionBar.Theme
 * Includes precise dp/px calculations, message bubble typography scales, icon dimensions, and haptic feedback.
 */

export class AndroidUtilities {
  public static density: number = typeof window !== 'undefined' ? (window.devicePixelRatio || 1) : 1;
  public static displaySize: { width: number; height: number } = {
    width: typeof window !== 'undefined' ? window.innerWidth : 360,
    height: typeof window !== 'undefined' ? window.innerHeight : 640,
  };

  /**
   * dp to px conversion exact formula: Math.ceil(density * dp)
   */
  public static dp(value: number): number {
    if (value === 0) return 0;
    return Math.ceil(this.density * value);
  }

  public static dp2(value: number): number {
    if (value === 0) return 0;
    return Math.floor(this.density * value);
  }

  public static dpr(value: number): number {
    if (value === 0) return 0;
    return Math.round(this.density * value);
  }

  public static runOnUIThread(runnable: () => void, delay: number = 0): void {
    if (delay === 0) {
      if (typeof window !== 'undefined' && window.requestAnimationFrame) {
        window.requestAnimationFrame(runnable);
      } else {
        setTimeout(runnable, 0);
      }
    } else {
      setTimeout(runnable, delay);
    }
  }

  public static vibrate(pattern: number | number[] = 20): void {
    if (typeof navigator !== 'undefined' && navigator.vibrate) {
      try {
        navigator.vibrate(pattern);
      } catch (e) {
        // Ignore iframe restrictions
      }
    }
  }

  /**
   * Formats message timestamps according to Telegram Android rules
   */
  public static formatMessageTime(timestamp: number | string): string {
    const date = typeof timestamp === 'string' ? new Date(timestamp) : new Date(timestamp * (timestamp < 10000000000 ? 1000 : 1));
    if (isNaN(date.getTime())) return '';
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
  }

  /**
   * Formats chat item date headers (Today, Yesterday, Date)
   */
  public static formatChatDate(timestamp: number | string): string {
    const date = typeof timestamp === 'string' ? new Date(timestamp) : new Date(timestamp * (timestamp < 10000000000 ? 1000 : 1));
    if (isNaN(date.getTime())) return '';
    const now = new Date();
    const isToday = now.toDateString() === date.toDateString();
    if (isToday) return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });

    const yesterday = new Date(now);
    yesterday.setDate(now.getDate() - 1);
    if (yesterday.toDateString() === date.toDateString()) {
      return 'أمس';
    }

    return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
  }
}

/**
 * Telegram Official Theme & Metrics System
 */
export class TelegramTheme {
  // Font sizes in SP/DP matching official Telegram Android Theme.java
  public static messageFontSize: number = 16;
  public static messageMediaFontSize: number = 14;
  public static dialogsTitleFontSize: number = 17;
  public static dialogsSubtitleFontSize: number = 14;
  public static dialogsDateFontSize: number = 12;
  public static chatHeaderTitleSize: number = 18;
  public static chatHeaderSubtitleSize: number = 13;

  // Telegram Icon & Avatar standard dimensions
  public static readonly AVATAR_SIZE_SMALL: number = 42;
  public static readonly AVATAR_SIZE_NORMAL: number = 54;
  public static readonly AVATAR_SIZE_LARGE: number = 64;
  public static readonly AVATAR_SIZE_PROFILE: number = 100;
  
  public static readonly ICON_SIZE_ACTION_BAR: number = 24;
  public static readonly ICON_SIZE_MENU: number = 22;
  public static readonly ICON_SIZE_CHECKMARK: number = 14;
  public static readonly BUBBLE_RADIUS_NORMAL: number = 16;
  public static readonly BUBBLE_RADIUS_SMALL: number = 6;

  public static setFontSize(size: number): void {
    this.messageFontSize = Math.max(12, Math.min(24, size));
    if (typeof document !== 'undefined') {
      document.documentElement.style.setProperty('--tg-msg-font-size', `${this.messageFontSize}px`);
    }
  }

  public static getMessageStyle(): React.CSSProperties {
    return {
      fontSize: `${this.messageFontSize}px`,
      lineHeight: '1.45',
    };
  }
}
