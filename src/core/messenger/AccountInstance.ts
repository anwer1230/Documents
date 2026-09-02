/**
 * AccountInstance.ts - org.telegram.messenger.AccountInstance
 * Replicated directly from DrKLO/Telegram Android
 * Provides isolated context for multi-account management
 */

import { connectionsManager, ConnectionsManager } from '../ConnectionsManager';
import { messagesController, MessagesController } from '../MessagesController';
import { notificationsController, NotificationsController } from '../NotificationsController';
import { NotificationCenter } from '../NotificationCenter';
import { TdClient, tdClient } from '../tdlib/TdClient';
import { UserConfig } from './UserConfig';
import { ContactsController } from './ContactsController';
import { MediaDataController } from './MediaDataController';
import { SendMessagesHelper } from './SendMessagesHelper';
import { SecretChatHelper } from './SecretChatHelper';
import { PrivacySettingsController, PrivacySettingsState, PrivacyTarget } from './PrivacySettingsController';
import { TwoStepVerificationController, TwoStepState } from './TwoStepVerificationController';
import { TLRPC } from '../TLRPC';

export class AccountInstance {
  private static instances = new Map<number, AccountInstance>();
  private currentAccount: number;

  public static getInstance(accountNum: number = UserConfig.selectedAccount || 0): AccountInstance {
    if (!AccountInstance.instances.has(accountNum)) {
      AccountInstance.instances.set(accountNum, new AccountInstance(accountNum));
    }
    return AccountInstance.instances.get(accountNum)!;
  }

  private constructor(accountNum: number) {
    this.currentAccount = accountNum;
  }

  public getCurrentAccount(): number {
    return this.currentAccount;
  }

  public getUserConfig(): UserConfig {
    return UserConfig.getInstance(this.currentAccount);
  }

  public getConnectionsManager(): ConnectionsManager {
    return ConnectionsManager.getInstance(this.currentAccount);
  }

  public getMessagesController(): MessagesController {
    return MessagesController.getInstance(this.currentAccount);
  }

  public getNotificationCenter(): NotificationCenter {
    return NotificationCenter.getInstance(this.currentAccount);
  }

  public getPrivacySettingsController(): PrivacySettingsController {
    return PrivacySettingsController.getInstance(this.currentAccount);
  }

  public getTwoStepVerificationController(): TwoStepVerificationController {
    return TwoStepVerificationController.getInstance(this.currentAccount);
  }

  public getContactsController(): ContactsController {
    return ContactsController.getInstance();
  }

  public getMediaDataController(): MediaDataController {
    return MediaDataController.getInstance(this.currentAccount);
  }

  public getSendMessagesHelper(): SendMessagesHelper {
    return SendMessagesHelper.getInstance();
  }

  public getSecretChatHelper(): SecretChatHelper {
    return SecretChatHelper.getInstance();
  }

  public getNotificationsController(): NotificationsController {
    return notificationsController;
  }

  public getTdClient(): TdClient {
    return tdClient;
  }

  /**
   * Sync layer: Fetches and stores TLRPC.TL_account_getPrivacy settings
   * isolated per authenticated account session
   */
  public async syncPrivacySettings(target?: PrivacyTarget): Promise<PrivacySettingsState> {
    const privacyController = this.getPrivacySettingsController();
    if (target) {
      await privacyController.loadPrivacy(target);
    } else {
      await privacyController.loadAllPrivacyRules();
    }
    return privacyController.getState();
  }

  /**
   * Sync layer: Fetches and stores TLRPC.TL_account_getPassword settings
   * isolated per authenticated account session
   */
  public async syncPasswordSettings(): Promise<TLRPC.TL_account_password> {
    const twoStepController = this.getTwoStepVerificationController();
    const passwordSettings = await twoStepController.getPassword();
    return passwordSettings;
  }

  /**
   * Complete sync layer: Fetches and stores both privacy rules and 2FA password configuration
   * guaranteeing isolated storage per current authenticated user session
   */
  public async syncAccountSecurityAndPrivacy(): Promise<{
    accountNum: number;
    privacy: PrivacySettingsState;
    password: TLRPC.TL_account_password;
  }> {
    const [privacy, password] = await Promise.all([
      this.syncPrivacySettings(),
      this.syncPasswordSettings(),
    ]);

    return {
      accountNum: this.currentAccount,
      privacy,
      password,
    };
  }
}
