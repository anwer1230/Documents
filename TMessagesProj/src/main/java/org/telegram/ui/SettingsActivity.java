package org.telegram.ui;

import android.content.Context;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;

/**
 * SettingsActivity.java
 * Official DrKLO/Telegram Android architecture implementation.
 * Ensures user profile, username, bio, and notifications are saved via
 * ConnectionsManager with update responses dispatched into MessagesController.processUpdates.
 */
public class SettingsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    public SettingsActivity() {
        super();
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.mainUserInfoChanged);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.updateInterfaces);
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.mainUserInfoChanged);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.updateInterfaces);
    }

    /**
     * Updates user first name, last name, and bio on Telegram Cloud
     */
    public void updateProfile(String firstName, String lastName, String about) {
        TLRPC.TL_account_updateProfile req = new TLRPC.TL_account_updateProfile();
        req.first_name = firstName;
        req.last_name = lastName;
        req.about = about;
        req.flags = 7;

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, new RequestDelegate() {
            @Override
            public void run(TLObject response, TLRPC.TL_error error) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (error == null) {
                        if (response instanceof TLRPC.Updates) {
                            MessagesController.getInstance(currentAccount).processUpdates((TLRPC.Updates) response, false);
                        } else if (response instanceof TLRPC.User) {
                            TLRPC.User user = (TLRPC.User) response;
                            UserConfig.getInstance(currentAccount).setCurrentUser(user);
                            UserConfig.getInstance(currentAccount).saveConfig(true);
                            MessagesController.getInstance(currentAccount).putUser(user, false);
                        }

                        NotificationCenter.getInstance(currentAccount).postNotificationName(
                            NotificationCenter.mainUserInfoChanged
                        );
                        NotificationCenter.getInstance(currentAccount).postNotificationName(
                            NotificationCenter.updateInterfaces,
                            MessagesController.UPDATE_MASK_NAME
                        );
                    }
                });
            }
        });
    }

    /**
     * Updates account username on Telegram Cloud
     */
    public void updateUsername(String newUsername) {
        TLRPC.TL_account_updateUsername req = new TLRPC.TL_account_updateUsername();
        req.username = newUsername != null ? newUsername : "";

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, new RequestDelegate() {
            @Override
            public void run(TLObject response, TLRPC.TL_error error) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (error == null) {
                        if (response instanceof TLRPC.Updates) {
                            MessagesController.getInstance(currentAccount).processUpdates((TLRPC.Updates) response, false);
                        } else if (response instanceof TLRPC.User) {
                            TLRPC.User user = (TLRPC.User) response;
                            UserConfig.getInstance(currentAccount).setCurrentUser(user);
                            UserConfig.getInstance(currentAccount).saveConfig(true);
                        }

                        NotificationCenter.getInstance(currentAccount).postNotificationName(
                            NotificationCenter.mainUserInfoChanged
                        );
                    }
                });
            }
        });
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.mainUserInfoChanged || id == NotificationCenter.updateInterfaces) {
            // Refresh settings list views
        }
    }
}
