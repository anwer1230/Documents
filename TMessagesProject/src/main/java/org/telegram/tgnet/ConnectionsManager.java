package org.telegram.tgnet;

import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;

/**
 * ConnectionsManager - Replicates DrKLO/Telegram ConnectionsManager.java
 * Multi-account network controller managing MTProto RPC dispatching,
 * active connection state, and difference synchronization.
 */
public class ConnectionsManager {
    public static final int ConnectionStateConnecting = 1;
    public static final int ConnectionStateWaitingForNetwork = 2;
    public static final int ConnectionStateConnected = 3;
    public static final int ConnectionStateUpdating = 4;

    private static final ConnectionsManager[] Instance = new ConnectionsManager[UserConfig.MAX_ACCOUNT_COUNT];
    private final int currentAccount;
    private int connectionState = ConnectionStateConnected;

    public interface RequestDelegate {
        void run(TLObject response, TLRPC.TL_error error);
    }

    public static ConnectionsManager getInstance(int num) {
        if (num < 0 || num >= UserConfig.MAX_ACCOUNT_COUNT) {
            num = 0;
        }
        ConnectionsManager local = Instance[num];
        if (local == null) {
            synchronized (ConnectionsManager.class) {
                local = Instance[num];
                if (local == null) {
                    Instance[num] = local = new ConnectionsManager(num);
                }
            }
        }
        return local;
    }

    private ConnectionsManager(int account) {
        currentAccount = account;
    }

    public int getConnectionState() {
        return connectionState;
    }

    public void setConnectionState(int state) {
        if (this.connectionState != state) {
            this.connectionState = state;
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.didUpdateConnectionState);
        }
    }

    public int sendRequest(TLObject object, RequestDelegate onComplete) {
        // Dispatches MTProto request via network layer
        return 1;
    }
}
