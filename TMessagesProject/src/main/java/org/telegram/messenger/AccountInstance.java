package org.telegram.messenger;

import org.telegram.tgnet.ConnectionsManager;

/**
 * AccountInstance - Replicates DrKLO/Telegram AccountInstance.java
 * Provides unified isolated access to per-account services:
 * - MessagesController
 * - MessagesStorage
 * - ConnectionsManager
 * - NotificationCenter
 * - UserConfig
 */
public class AccountInstance {
    private final int currentAccount;
    private static final AccountInstance[] Instance = new AccountInstance[UserConfig.MAX_ACCOUNT_COUNT];

    public static AccountInstance getInstance(int num) {
        if (num < 0 || num >= UserConfig.MAX_ACCOUNT_COUNT) {
            num = 0;
        }
        AccountInstance local = Instance[num];
        if (local == null) {
            synchronized (AccountInstance.class) {
                local = Instance[num];
                if (local == null) {
                    Instance[num] = local = new AccountInstance(num);
                }
            }
        }
        return local;
    }

    private AccountInstance(int account) {
        currentAccount = account;
    }

    public int getCurrentAccount() {
        return currentAccount;
    }

    public UserConfig getUserConfig() {
        return UserConfig.getInstance(currentAccount);
    }

    public MessagesController getMessagesController() {
        return MessagesController.getInstance(currentAccount);
    }

    public MessagesStorage getMessagesStorage() {
        return MessagesStorage.getInstance(currentAccount);
    }

    public ConnectionsManager getConnectionsManager() {
        return ConnectionsManager.getInstance(currentAccount);
    }

    public NotificationCenter getNotificationCenter() {
        return NotificationCenter.getInstance(currentAccount);
    }
}
