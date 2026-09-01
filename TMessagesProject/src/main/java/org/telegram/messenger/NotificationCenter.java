package org.telegram.messenger;

import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * NotificationCenter - Replicates DrKLO/Telegram NotificationCenter.java
 * Handles observer patterns for UI synchronization and multi-account state updates.
 */
public class NotificationCenter {
    public static final int didReceivedNewMessages = 1;
    public static final int updateInterfaces = 2;
    public static final int dialogsNeedReload = 3;
    public static final int messagesDeleted = 4;
    public static final int mainUserInfoChanged = 5;
    public static final int appDidLogout = 6;
    public static final int didUpdateConnectionState = 7;
    public static final int accountSwitched = 8;
    public static final int updatesDidReceived = 9;

    public interface NotificationCenterDelegate {
        void didReceivedNotification(int id, int account, Object... args);
    }

    private static final NotificationCenter[] Instance = new NotificationCenter[UserConfig.MAX_ACCOUNT_COUNT];
    private static NotificationCenter globalInstance;

    private final int currentAccount;
    private final HashMap<Integer, ArrayList<NotificationCenterDelegate>> observers = new HashMap<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static NotificationCenter getInstance(int num) {
        if (num < 0 || num >= UserConfig.MAX_ACCOUNT_COUNT) {
            num = 0;
        }
        NotificationCenter local = Instance[num];
        if (local == null) {
            synchronized (NotificationCenter.class) {
                local = Instance[num];
                if (local == null) {
                    Instance[num] = local = new NotificationCenter(num);
                }
            }
        }
        return local;
    }

    public static NotificationCenter getGlobalInstance() {
        if (globalInstance == null) {
            synchronized (NotificationCenter.class) {
                if (globalInstance == null) {
                    globalInstance = new NotificationCenter(-1);
                }
            }
        }
        return globalInstance;
    }

    private NotificationCenter(int account) {
        currentAccount = account;
    }

    public void addObserver(NotificationCenterDelegate observer, int id) {
        ArrayList<NotificationCenterDelegate> list = observers.computeIfAbsent(id, k -> new ArrayList<>());
        if (!list.contains(observer)) {
            list.add(observer);
        }
    }

    public void removeObserver(NotificationCenterDelegate observer, int id) {
        ArrayList<NotificationCenterDelegate> list = observers.get(id);
        if (list != null) {
            list.remove(observer);
        }
    }

    public void postNotificationName(int id, Object... args) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            postNotificationNameInternal(id, args);
        } else {
            mainHandler.post(() -> postNotificationNameInternal(id, args));
        }
    }

    private void postNotificationNameInternal(int id, Object... args) {
        ArrayList<NotificationCenterDelegate> list = observers.get(id);
        if (list != null && !list.isEmpty()) {
            ArrayList<NotificationCenterDelegate> copy = new ArrayList<>(list);
            for (NotificationCenterDelegate delegate : copy) {
                delegate.didReceivedNotification(id, currentAccount, args);
            }
        }
    }
}
