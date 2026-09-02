/**
 * Telegram Official UserConfig
 * Replicates org.telegram.messenger.UserConfig.java from DrKLO/Telegram
 * Manages multi-account profiles, activation state, and isolated account keys.
 */

import { NotificationCenter } from './NotificationCenter';

export interface TelegramCurrentUser {
  id: number | string;
  first_name?: string;
  last_name?: string;
  username?: string;
  phone?: string;
  photo?: string;
  bio?: string;
  is_premium?: boolean;
  is_verified?: boolean;
  has_2fa?: boolean;
  two_factor_enabled?: boolean;
}

export class UserConfig {
  public static readonly MAX_ACCOUNT_COUNT = 4;
  public static selectedAccount: number = 0;
  private static instances: UserConfig[] = [];

  public readonly currentAccount: number;
  private currentUser: TelegramCurrentUser | null = null;
  private clientUserId: number | string = 0;
  private isActivated: boolean = false;

  private constructor(account: number) {
    this.currentAccount = account;
    this.loadConfig();
  }

  public static getInstance(account: number = UserConfig.selectedAccount): UserConfig {
    if (account < 0 || account >= UserConfig.MAX_ACCOUNT_COUNT) {
      account = 0;
    }
    if (!UserConfig.instances[account]) {
      UserConfig.instances[account] = new UserConfig(account);
    }
    return UserConfig.instances[account];
  }

  public static getActivatedAccountsCount(): number {
    let count = 0;
    for (let i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
      if (UserConfig.getInstance(i).isClientActivated()) {
        count++;
      }
    }
    return count;
  }

  public static getClientUserId(): number | string {
    return UserConfig.getInstance().getClientUserId();
  }

  public static getCurrentUser(): TelegramCurrentUser | null {
    return UserConfig.getInstance().getCurrentUser();
  }

  public static isClientActivated(): boolean {
    return UserConfig.getInstance().isClientActivated();
  }

  public getClientUserId(): number | string {
    if (!this.clientUserId) {
      this.loadConfig();
    }
    return this.clientUserId;
  }

  public getCurrentUser(): TelegramCurrentUser | null {
    if (!this.currentUser) {
      this.loadConfig();
    }
    return this.currentUser;
  }

  public isClientActivated(): boolean {
    return this.isActivated && Boolean(this.clientUserId);
  }

  public isPremium(): boolean {
    return Boolean(this.currentUser?.is_premium);
  }

  public setCurrentUser(user: TelegramCurrentUser | null): void {
    this.currentUser = user;
    if (user && user.id) {
      this.clientUserId = user.id;
      this.isActivated = true;
    } else {
      this.clientUserId = 0;
      this.isActivated = false;
    }
    this.saveConfig();
    NotificationCenter.getInstance(this.currentAccount).postNotificationName(NotificationCenter.mainUserInfoChanged);
  }

  public clearConfig(): void {
    this.currentUser = null;
    this.clientUserId = 0;
    this.isActivated = false;
    try {
      localStorage.removeItem(`tg_user_config_${this.currentAccount}`);
    } catch {}
    NotificationCenter.getInstance(this.currentAccount).postNotificationName(NotificationCenter.appDidLogout);
  }

  public loadConfig(): void {
    try {
      const saved = localStorage.getItem(`tg_user_config_${this.currentAccount}`) || (this.currentAccount === 0 ? localStorage.getItem('tg_user_config') : null);
      if (saved) {
        const parsed = JSON.parse(saved);
        this.currentUser = parsed.user || null;
        this.clientUserId = parsed.clientUserId || (parsed.user?.id || 0);
        this.isActivated = Boolean(this.clientUserId);
      }
    } catch {}
  }

  public saveConfig(): void {
    try {
      localStorage.setItem(
        `tg_user_config_${this.currentAccount}`,
        JSON.stringify({
          clientUserId: this.clientUserId,
          user: this.currentUser,
          currentAccount: this.currentAccount,
        })
      );
    } catch {}
  }
}
