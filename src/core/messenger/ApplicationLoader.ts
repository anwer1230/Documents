/**
 * ApplicationLoader.ts - Telegram Application Lifecycle & Bootstrap Controller
 * 
 * Replicated directly from DrKLO/Telegram Android:
 * org.telegram.messenger.ApplicationLoader.java
 */

import { BuildVars } from './BuildVars';
import { appUpdateController } from './AppUpdateController';

export class ApplicationLoader {
  public static applicationContext: any = null;
  public static isInitialized: boolean = false;
  private static updateTimer: any = null;
  private static gcmToken: string = 'fcm_tg_token_live_' + Math.random().toString(36).substring(2, 10);

  /**
   * Initializes application subsystems, database instances, and schedules OTA update checks
   */
  public static initApplication(): void {
    if (this.isInitialized) return;
    this.isInitialized = true;

    // Load persisted push tokens
    const savedToken = localStorage.getItem('tg_gcm_source');
    if (savedToken) {
      this.gcmToken = savedToken;
    } else {
      localStorage.setItem('tg_gcm_source', this.gcmToken);
    }

    console.log(`[ApplicationLoader] Booting Telegram v${BuildVars.BUILD_VERSION_STRING} (Build ${BuildVars.BUILD_VERSION})...`);

    // Schedule automatic update checks
    this.scheduleAppUpdateCheck();
  }

  public static getGcmToken(): string {
    return this.gcmToken;
  }

  public static getApplicationId(): string {
    return BuildVars.APPLICATION_ID;
  }

  /**
   * Schedules background periodic update checks without interrupting user experience
   */
  public static scheduleAppUpdateCheck(): void {
    if (this.updateTimer) {
      clearInterval(this.updateTimer);
    }

    // Perform initial check shortly after app boot (1.5s delay to let UI render)
    setTimeout(() => {
      appUpdateController.checkAppUpdate(false);
    }, 1500);

    // Periodic check every 24 hours (or configured interval)
    this.updateTimer = setInterval(() => {
      appUpdateController.checkAppUpdate(false);
    }, BuildVars.UPDATE_CHECK_INTERVAL_MS);
  }

  /**
   * Safe reload & update application while strictly preserving session and local state
   */
  public static applyUpdateAndPreserveSession(): void {
    console.log('[ApplicationLoader] Applying OTA update. Session and state preserved.');
    
    // Save timestamp of update
    localStorage.setItem('tg_last_update_installed', Date.now().toString());
    
    // Refresh the application cleanly to apply latest bundles
    window.location.reload();
  }
}
