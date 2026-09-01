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
 * TwoStepVerificationActivity.java
 * Official DrKLO/Telegram Android architecture implementation.
 * Ensures 2FA passwords and recovery email settings are saved directly via
 * ConnectionsManager RPC and resulting updates are routed to MessagesController.processUpdates.
 */
public class TwoStepVerificationActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private TLRPC.TL_account_password currentPassword;
    private boolean passwordSet;

    public TwoStepVerificationActivity() {
        super();
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.twoStepStateUpdated);
        loadPasswordSettings();
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.twoStepStateUpdated);
    }

    /**
     * Loads 2FA configuration from Telegram Server
     */
    public void loadPasswordSettings() {
        TLRPC.TL_account_getPassword req = new TLRPC.TL_account_getPassword();
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, new RequestDelegate() {
            @Override
            public void run(TLObject response, TLRPC.TL_error error) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (error == null && response instanceof TLRPC.TL_account_password) {
                        currentPassword = (TLRPC.TL_account_password) response;
                        passwordSet = currentPassword.has_password;
                        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.twoStepStateUpdated, currentPassword);
                    }
                });
            }
        });
    }

    /**
     * Commits new Two-Step Verification settings to Telegram Cloud
     */
    public void updatePasswordSettings(TLRPC.TL_account_passwordInputSettings newSettings, TLRPC.InputCheckPasswordSRP currentPasswordSrp) {
        TLRPC.TL_account_updatePasswordSettings req = new TLRPC.TL_account_updatePasswordSettings();
        req.password = currentPasswordSrp;
        req.new_settings = newSettings;

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, new RequestDelegate() {
            @Override
            public void run(TLObject response, TLRPC.TL_error error) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (error == null) {
                        // CRITICAL: Deliver server updates to cloud engine
                        if (response instanceof TLRPC.Updates) {
                            MessagesController.getInstance(currentAccount).processUpdates((TLRPC.Updates) response, false);
                        } else if (response instanceof TLRPC.TL_boolTrue) {
                            // Reload password state from server to ensure perfect synchronization across all devices
                            loadPasswordSettings();
                        }

                        NotificationCenter.getInstance(currentAccount).postNotificationName(
                            NotificationCenter.updateInterfaces,
                            MessagesController.UPDATE_MASK_ALL
                        );
                    }
                });
            }
        }, ConnectionsManager.RequestFlagFailOnServerErrors);
    }

    /**
     * Verifies recovery email with code dispatched to user's inbox
     */
    public void confirmEmail(String code) {
        TLRPC.TL_account_confirmPasswordEmail req = new TLRPC.TL_account_confirmPasswordEmail();
        req.code = code;

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, new RequestDelegate() {
            @Override
            public void run(TLObject response, TLRPC.TL_error error) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (error == null) {
                        if (response instanceof TLRPC.Updates) {
                            MessagesController.getInstance(currentAccount).processUpdates((TLRPC.Updates) response, false);
                        }
                        loadPasswordSettings();
                    }
                });
            }
        });
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.twoStepStateUpdated) {
            if (args != null && args.length > 0 && args[0] instanceof TLRPC.TL_account_password) {
                currentPassword = (TLRPC.TL_account_password) args[0];
                passwordSet = currentPassword.has_password;
            }
        }
    }
}
