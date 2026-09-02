/**
 * Telegram Official NotificationCenter Observer System
 * Replicates org.telegram.messenger.NotificationCenter.java from DrKLO/Telegram
 */

import { UserConfig } from './UserConfig';

export class NotificationCenter {
  private static instances: NotificationCenter[] = [];
  private static globalInstance: NotificationCenter;
  private readonly currentAccount: number;
  private observers: Map<number, Set<(id: number, account: number, ...args: any[]) => void>> = new Map();

  // Telegram Event IDs
  public static readonly dialogsNeedReload: number = 1;
  public static readonly updateInterfaces: number = 2;
  public static readonly messagesDidLoad: number = 3;
  public static readonly didLoadedReplyMessages: number = 4;
  public static readonly userFullInfoDidLoad: number = 5;
  public static readonly chatInfoDidLoad: number = 6;
  public static readonly encryptedChatCreated: number = 7;
  public static readonly dialogPhotosLoaded: number = 8;
  public static readonly notificationsSettingsUpdated: number = 9;
  public static readonly didReceivedNewMessages: number = 10;
  public static readonly messagesDeleted: number = 11;
  public static readonly messageReceivedByAck: number = 12;
  public static readonly messageSendError: number = 13;
  public static readonly chatDidCreated: number = 14;
  public static readonly dialogDeleted: number = 15;
  public static readonly totalUnreadPushedOnce: number = 16;
  public static readonly mainUserInfoChanged: number = 17;
  public static readonly privacyRulesUpdated: number = 18;
  public static readonly proxySettingsChanged: number = 19;
  public static readonly appDidLogout: number = 20;
  public static readonly accountSwitched: number = 21;
  public static readonly didUpdateConnectionState: number = 22;
  public static readonly updatesDidReceived: number = 23;
  public static readonly twoStepStateChanged: number = 24;

  private constructor(account: number) {
    this.currentAccount = account;
  }

  public static getInstance(account: number = UserConfig.selectedAccount): NotificationCenter {
    if (account < 0 || account >= UserConfig.MAX_ACCOUNT_COUNT) {
      account = 0;
    }
    if (!NotificationCenter.instances[account]) {
      NotificationCenter.instances[account] = new NotificationCenter(account);
    }
    return NotificationCenter.instances[account];
  }

  public static getGlobalInstance(): NotificationCenter {
    if (!NotificationCenter.globalInstance) {
      NotificationCenter.globalInstance = new NotificationCenter(-1);
    }
    return NotificationCenter.globalInstance;
  }

  public addObserver(observer: (id: number, account: number, ...args: any[]) => void, id: number): void {
    let set = this.observers.get(id);
    if (!set) {
      set = new Set();
      this.observers.set(id, set);
    }
    set.add(observer);
  }

  public removeObserver(observer: (id: number, account: number, ...args: any[]) => void, id: number): void {
    const set = this.observers.get(id);
    if (set) {
      set.delete(observer);
      if (set.size === 0) {
        this.observers.delete(id);
      }
    }
  }

  public postNotificationName(id: number, ...args: any[]): void {
    const set = this.observers.get(id);
    if (set && set.size > 0) {
      const copy = Array.from(set);
      copy.forEach((observer) => {
        try {
          observer(id, this.currentAccount, ...args);
        } catch (e) {
          console.error(`[NotificationCenter] Error in observer for event ${id}:`, e);
        }
      });
    }

    // Also notify global observers if this is an account instance
    if (this.currentAccount >= 0 && NotificationCenter.globalInstance) {
      NotificationCenter.globalInstance.postNotificationName(id, ...args);
    }
  }

  public hasObservers(id: number): boolean {
    const set = this.observers.get(id);
    return Boolean(set && set.size > 0);
  }
}
