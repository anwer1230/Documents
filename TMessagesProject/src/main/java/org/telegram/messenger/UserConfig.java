package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * UserConfig - Replicates DrKLO/Telegram UserConfig.java
 * Manages Multi-Account state (up to MAX_ACCOUNT_COUNT), active user profiles,
 * and account isolation tokens.
 */
public class UserConfig {
    public static final int MAX_ACCOUNT_COUNT = 4;
    public static int selectedAccount = 0;

    private static final UserConfig[] Instance = new UserConfig[MAX_ACCOUNT_COUNT];
    private final int currentAccount;

    public long clientUserId;
    public String currentPhoneNumber;
    public String currentFirstName;
    public String currentLastName;
    public String currentUsername;
    public boolean isPremium;
    public boolean has2fa;
    public boolean registered;

    public static UserConfig getInstance(int account) {
        if (account < 0 || account >= MAX_ACCOUNT_COUNT) {
            account = 0;
        }
        UserConfig local = Instance[account];
        if (local == null) {
            synchronized (UserConfig.class) {
                local = Instance[account];
                if (local == null) {
                    Instance[account] = local = new UserConfig(account);
                }
            }
        }
        return local;
    }

    private UserConfig(int account) {
        currentAccount = account;
        loadConfig();
    }

    public static int getActivatedAccountsCount() {
        int count = 0;
        for (int a = 0; a < MAX_ACCOUNT_COUNT; a++) {
            if (getInstance(a).isClientActivated()) {
                count++;
            }
        }
        return count;
    }

    public boolean isClientActivated() {
        return clientUserId != 0 && registered;
    }

    public void setCurrentUser(long userId, String phone, String firstName, String lastName, String username, boolean premium, boolean twoFa) {
        this.clientUserId = userId;
        this.currentPhoneNumber = phone;
        this.currentFirstName = firstName;
        this.currentLastName = lastName;
        this.currentUsername = username;
        this.isPremium = premium;
        this.has2fa = twoFa;
        this.registered = (userId != 0);
        saveConfig(true);
        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.mainUserInfoChanged);
    }

    public void clearConfig() {
        this.clientUserId = 0;
        this.currentPhoneNumber = null;
        this.currentFirstName = null;
        this.currentLastName = null;
        this.currentUsername = null;
        this.isPremium = false;
        this.has2fa = false;
        this.registered = false;
        saveConfig(true);
        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.appDidLogout);
    }

    public void loadConfig() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) return;
        SharedPreferences preferences = context.getSharedPreferences("userconfig_" + currentAccount, Context.MODE_PRIVATE);
        clientUserId = preferences.getLong("clientUserId", 0);
        currentPhoneNumber = preferences.getString("phone", null);
        currentFirstName = preferences.getString("firstName", null);
        currentLastName = preferences.getString("lastName", null);
        currentUsername = preferences.getString("username", null);
        isPremium = preferences.getBoolean("isPremium", false);
        has2fa = preferences.getBoolean("has2fa", false);
        registered = preferences.getBoolean("registered", clientUserId != 0);
    }

    public void saveConfig(boolean notify) {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) return;
        SharedPreferences preferences = context.getSharedPreferences("userconfig_" + currentAccount, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong("clientUserId", clientUserId);
        editor.putString("phone", currentPhoneNumber);
        editor.putString("firstName", currentFirstName);
        editor.putString("lastName", currentLastName);
        editor.putString("username", currentUsername);
        editor.putBoolean("isPremium", isPremium);
        editor.putBoolean("has2fa", has2fa);
        editor.putBoolean("registered", registered);
        editor.apply();
    }
}
