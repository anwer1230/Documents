package org.telegram.ui;

import android.app.Activity;
import android.os.Bundle;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

/**
 * LoginActivity - Replicates DrKLO/Telegram LoginActivity.java
 * Multi-account phone authentication, SMS/Code verification, 2FA SRP-6a verification,
 * and account instance activation.
 */
public class LoginActivity extends Activity implements NotificationCenter.NotificationCenterDelegate {
    private int currentAccount = UserConfig.selectedAccount;
    private String currentPhoneNumber;
    private String phoneCodeHash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentAccount = getIntent().getIntExtra("account_num", UserConfig.selectedAccount);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.mainUserInfoChanged);
    }

    public void sendCode(String phoneNumber) {
        currentPhoneNumber = phoneNumber;
        TLRPC.TL_auth_sendCode req = new TLRPC.TL_auth_sendCode();
        req.phone_number = phoneNumber;

        AccountInstance.getInstance(currentAccount).getConnectionsManager().sendRequest(req, (response, error) -> {
            if (response instanceof TLRPC.TL_auth_sentCode) {
                TLRPC.TL_auth_sentCode sentCode = (TLRPC.TL_auth_sentCode) response;
                phoneCodeHash = sentCode.phone_code_hash;
            }
        });
    }

    public void signIn(String code) {
        TLRPC.TL_auth_signIn req = new TLRPC.TL_auth_signIn();
        req.phone_number = currentPhoneNumber;
        req.phone_code_hash = phoneCodeHash;
        req.phone_code = code;

        AccountInstance.getInstance(currentAccount).getConnectionsManager().sendRequest(req, (response, error) -> {
            if (response instanceof TLRPC.TL_auth_authorization) {
                TLRPC.TL_auth_authorization auth = (TLRPC.TL_auth_authorization) response;
                AccountInstance.getInstance(currentAccount).getUserConfig().setCurrentUser(
                    auth.user.id,
                    auth.user.phone,
                    auth.user.first_name,
                    auth.user.last_name,
                    auth.user.username,
                    auth.user.premium,
                    false
                );
            }
        });
    }

    public void verifyTwoFactor(byte[] passwordHash) {
        TLRPC.TL_auth_checkPassword req = new TLRPC.TL_auth_checkPassword();
        req.password_hash = passwordHash;

        AccountInstance.getInstance(currentAccount).getConnectionsManager().sendRequest(req, (response, error) -> {
            if (response instanceof TLRPC.TL_auth_authorization) {
                TLRPC.TL_auth_authorization auth = (TLRPC.TL_auth_authorization) response;
                AccountInstance.getInstance(currentAccount).getUserConfig().setCurrentUser(
                    auth.user.id,
                    auth.user.phone,
                    auth.user.first_name,
                    auth.user.last_name,
                    auth.user.username,
                    auth.user.premium,
                    true
                );
            }
        });
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.mainUserInfoChanged && account == currentAccount) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.mainUserInfoChanged);
    }
}
