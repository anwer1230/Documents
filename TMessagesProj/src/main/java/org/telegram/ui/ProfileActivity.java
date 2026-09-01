/*
 * This is the source code of Telegram for Android v. 12.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2024.
 */

package org.telegram.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;

public class ProfileActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private long userId;
    private TLRPC.User currentUser;
    private TLRPC.UserFull userInfo;

    public ProfileActivity(Bundle args) {
        super(args);
        if (args != null) {
            this.userId = args.getLong("user_id", 0);
        }
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.userFullInfoDidLoad);
        loadUserProfile();
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.userFullInfoDidLoad);
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(android.R.drawable.ic_menu_revert);
        actionBar.setTitle("User Profile");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });

        fragmentView = new FrameLayout(context);
        return fragmentView;
    }

    public void loadUserProfile() {
        TLRPC.TL_users_getFullUser req = new TLRPC.TL_users_getFullUser();
        req.id = MessagesController.getInstance(currentAccount).getInputUser(userId);

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            if (response instanceof TLRPC.TL_users_userFull) {
                userInfo = ((TLRPC.TL_users_userFull) response).full_user;
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.userFullInfoDidLoad, userId, userInfo);
            }
        });
    }

    public ImageLocation getUserAvatarLocation(TLRPC.User user) {
        if (user == null || user.photo == null) return null;
        return ImageLocation.getForUser(user, ImageLocation.TYPE_BIG);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.userFullInfoDidLoad) {
            long uid = (long) args[0];
            if (uid == userId && fragmentView != null) {
                fragmentView.invalidate();
            }
        }
    }
}
