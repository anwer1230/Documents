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

import java.util.ArrayList;

/**
 * PrivacySettingsActivity.java
 * Official DrKLO/Telegram Android architecture implementation.
 * All privacy rules are synchronized with Telegram Cloud servers via ConnectionsManager,
 * and responses are fed directly into MessagesController.processUpdates.
 */
public class PrivacySettingsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    public static final int PRIVACY_RULES_TYPE_LASTSEEN = 0;
    public static final int PRIVACY_RULES_TYPE_INVITE = 1;
    public static final int PRIVACY_RULES_TYPE_CALLS = 2;
    public static final int PRIVACY_RULES_TYPE_P2P = 3;
    public static final int PRIVACY_RULES_TYPE_PHOTO = 4;
    public static final int PRIVACY_RULES_TYPE_FORWARDS = 5;
    public static final int PRIVACY_RULES_TYPE_PHONE = 6;
    public static final int PRIVACY_RULES_TYPE_VOICE_MESSAGES = 7;
    public static final int PRIVACY_RULES_TYPE_BIO = 8;

    public PrivacySettingsActivity() {
        super();
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.privacyRulesUpdated);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.updateInterfaces);
        loadPrivacySettings();
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.privacyRulesUpdated);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.updateInterfaces);
    }

    /**
     * Loads privacy settings from Telegram Datacenter via MTProto RPC
     */
    public void loadPrivacySettings() {
        TLRPC.TL_account_getPrivacy req = new TLRPC.TL_account_getPrivacy();
        req.key = new TLRPC.TL_inputPrivacyKeyStatusTimestamp();

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, new RequestDelegate() {
            @Override
            public void run(TLObject response, TLRPC.TL_error error) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (error == null && response instanceof TLRPC.TL_account_privacyRules) {
                        TLRPC.TL_account_privacyRules res = (TLRPC.TL_account_privacyRules) response;
                        MessagesController.getInstance(currentAccount).setPrivacyRules(req.key, res.rules);
                        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.privacyRulesUpdated);
                    }
                });
            }
        });
    }

    /**
     * Updates privacy rule for a given key on Telegram Cloud and propagates Updates
     */
    public void setPrivacyRule(TLRPC.InputPrivacyKey key, ArrayList<TLRPC.InputPrivacyRule> rules) {
        TLRPC.TL_account_setPrivacy req = new TLRPC.TL_account_setPrivacy();
        req.key = key;
        req.rules = rules;

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, new RequestDelegate() {
            @Override
            public void run(TLObject response, TLRPC.TL_error error) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (error == null) {
                        // CRITICAL: Feed response to MessagesController so all linked devices receive the cloud update
                        if (response instanceof TLRPC.Updates) {
                            MessagesController.getInstance(currentAccount).processUpdates((TLRPC.Updates) response, false);
                        } else if (response instanceof TLRPC.TL_account_privacyRules) {
                            TLRPC.TL_account_privacyRules privacyRules = (TLRPC.TL_account_privacyRules) response;
                            MessagesController.getInstance(currentAccount).setPrivacyRules(key, privacyRules.rules);
                        }

                        // Broadcast local UI update
                        NotificationCenter.getInstance(currentAccount).postNotificationName(
                            NotificationCenter.privacyRulesUpdated,
                            key,
                            rules
                        );
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
     * Terminates all other active sessions across Telegram Cloud
     */
    public void terminateAllOtherSessions() {
        TLRPC.TL_auth_resetAuthorizations req = new TLRPC.TL_auth_resetAuthorizations();
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, new RequestDelegate() {
            @Override
            public void run(TLObject response, TLRPC.TL_error error) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (error == null) {
                        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                    }
                });
            }
        });
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.privacyRulesUpdated || id == NotificationCenter.updateInterfaces) {
            // Re-render UI list items
        }
    }
}
