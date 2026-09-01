/*
 * This is the source code of Telegram for Android v. 12.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2024.
 */

package org.telegram.ui;

import android.content.Context;
import android.view.View;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SessionSecurityManager;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import java.util.ArrayList;

/**
 * SessionsActivity displays the active devices, desktop clients, browsers,
 * and allows terminating individual sessions or terminating all other sessions.
 */
public class SessionsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private TLRPC.TL_authorization currentSession;
    private ArrayList<TLRPC.TL_authorization> otherSessions = new ArrayList<>();
    private int ttlDays = 180;
    private boolean loading;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.updateInterfaces);
        loadSessions();
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.updateInterfaces);
    }

    public void loadSessions() {
        loading = true;
        TLRPC.TL_account_getAuthorizations req = new TLRPC.TL_account_getAuthorizations();
        org.telegram.tgnet.ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            AndroidUtilities.runOnUIThread(() -> {
                loading = false;
                if (error == null && response instanceof TLRPC.TL_account_authorizations) {
                    TLRPC.TL_account_authorizations auths = (TLRPC.TL_account_authorizations) response;
                    otherSessions.clear();
                    for (TLRPC.TL_authorization a : auths.authorizations) {
                        if (a.current || (a.flags & 1) != 0) {
                            currentSession = a;
                        } else {
                            otherSessions.add(a);
                        }
                    }
                    ttlDays = auths.authorization_ttl_days;
                    updateUI();
                } else {
                    FileLog.e("Failed to load authorizations: " + (error != null ? error.text : "UNKNOWN"));
                    updateUI();
                }
            });
        });
    }

    public void terminateSession(TLRPC.TL_authorization authorization) {
        if (authorization == null) return;
        terminateSession(authorization.hash);
    }

    public void terminateSession(long hash) {
        TLRPC.TL_account_resetAuthorization req = new TLRPC.TL_account_resetAuthorization();
        req.hash = hash;
        org.telegram.tgnet.ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            AndroidUtilities.runOnUIThread(() -> {
                if (error == null) {
                    otherSessions.removeIf(a -> a.hash == hash);
                    updateUI();
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, NotificationCenter.UPDATE_MASK_ALL);
                } else {
                    FileLog.e("Terminate session failed: " + (error != null ? error.text : ""));
                }
            });
        });
    }

    public void terminateAllOtherSessions() {
        SessionSecurityManager.getInstance(currentAccount).terminateAllOtherSessions((success, error) -> {
            if (success) {
                otherSessions.clear();
                AndroidUtilities.runOnUIThread(() -> updateUI());
            } else {
                FileLog.e("Terminate all sessions failed: " + error);
            }
        });
    }

    private void updateUI() {
        // UI render delegate
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.updateInterfaces) {
            loadSessions();
        }
    }
}
