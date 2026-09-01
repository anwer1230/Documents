/*
 * This is the source code of Telegram for Android v. 11.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2024.
 */

package org.telegram.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.AlertsCreator;

public class ChatActionBarBuilder {

    public static final int menu_search = 1;
    public static final int menu_mute = 2;
    public static final int menu_leave = 3;
    public static final int menu_share = 4;
    public static final int menu_clear_history = 5;

    public static void buildMenu(BaseFragment fragment, ActionBar actionBar, TLRPC.Chat currentChat, long dialogId) {
        if (actionBar == null || fragment == null) return;
        final int currentAccount = fragment.getCurrentAccount();
        final long did = dialogId != 0 ? dialogId : (currentChat != null ? -currentChat.id : 0);

        ActionBarMenu menu = actionBar.createMenu();

        // 1. زر البحث في سجل المحادثة
        ActionBarMenuItem searchItem = menu.addItem(menu_search, R.drawable.ic_ab_search);
        if (searchItem != null) {
            searchItem.setIsSearchField(true);
        }

        // 2. القائمة المنسدلة الثلاثية النقاط
        ActionBarMenuItem otherItem = menu.addItem(0, R.drawable.ic_ab_other);
        if (otherItem != null) {
            boolean isMuted = did != 0 && MessagesController.getInstance(currentAccount).isDialogMuted(did, 0);

            // عنصر كتم أو تفعيل الإشعارات
            ActionBarMenuSubItem muteSubItem = otherItem.addSubItem(
                menu_mute,
                isMuted ? R.drawable.msg_unmute : R.drawable.msg_mute,
                LocaleController.getString(isMuted ? "UnmuteNotifications" : "MuteNotifications",
                    isMuted ? R.string.UnmuteNotifications : R.string.MuteNotifications)
            );

            otherItem.addSubItem(menu_share, R.drawable.msg_share, LocaleController.getString("ShareContact", R.string.ShareContact));
            otherItem.addSubItem(menu_clear_history, R.drawable.msg_clear, LocaleController.getString("ClearHistory", R.string.ClearHistory));

            if (currentChat != null) {
                boolean isChannel = ChatObject.isChannel(currentChat) && !currentChat.megagroup;
                otherItem.addSubItem(menu_leave, R.drawable.msg_leave, 
                    LocaleController.getString(isChannel ? "LeaveChannel" : "LeaveMegaMenu", 
                    isChannel ? R.string.LeaveChannel : R.string.LeaveMegaMenu));
            }
        }

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == menu_mute) {
                    toggleMuteChat(fragment, currentAccount, did, otherItem);
                } else if (id == menu_leave) {
                    GroupActionsHelper.leaveChatOrChannel(fragment, currentAccount, currentChat);
                } else if (id == menu_share) {
                    shareChatLink(fragment, currentChat, did);
                } else if (id == menu_clear_history) {
                    clearChatHistory(fragment, currentAccount, did);
                }
            }
        });
    }

    /**
     * تنفيذ طلب TLRPC.TL_messages_updateDialogNotificationsSettings لكتم وإلغاء كتم الإشعارات
     */
    public static void toggleMuteChat(BaseFragment fragment, int currentAccount, long dialogId, ActionBarMenuItem parentMenuItem) {
        if (dialogId == 0 || fragment == null) return;

        boolean isCurrentlyMuted = MessagesController.getInstance(currentAccount).isDialogMuted(dialogId, 0);
        final boolean newMuteState = !isCurrentlyMuted;

        // إعداد كائن إعدادات الإشعارات
        TLRPC.TL_inputPeerNotifySettings notifySettings = new TLRPC.TL_inputPeerNotifySettings();
        notifySettings.flags |= 1; // flag for mute_until
        notifySettings.mute_until = newMuteState ? Integer.MAX_VALUE : 0;
        notifySettings.silent = newMuteState;

        // إعداد طلب تحديث إشعارات المحادثة الرسمي
        TLRPC.TL_messages_updateDialogNotificationsSettings req = new TLRPC.TL_messages_updateDialogNotificationsSettings();
        TLRPC.TL_inputDialogPeer inputDialogPeer = new TLRPC.TL_inputDialogPeer();
        inputDialogPeer.peer = MessagesController.getInstance(currentAccount).getInputPeer(dialogId);
        req.peer = inputDialogPeer;
        req.settings = notifySettings;

        // إرسال الطلب عبر ConnectionsManager
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            AndroidUtilities.runOnUIThread(() -> {
                if (error == null) {
                    // تحديث التفضيلات المحلية والشاشات
                    SharedPreferences preferences = MessagesController.getNotificationsSettings(currentAccount);
                    SharedPreferences.Editor editor = preferences.edit();
                    if (newMuteState) {
                        editor.putInt("notify2_" + dialogId, 2);
                        editor.putInt("notifyuntil_" + dialogId, Integer.MAX_VALUE);
                    } else {
                        editor.putInt("notify2_" + dialogId, 0);
                        editor.remove("notifyuntil_" + dialogId);
                    }
                    editor.commit();

                    // تحديث واجهة القائمة المنسدلة
                    if (parentMenuItem != null) {
                        ActionBarMenuSubItem subItem = parentMenuItem.findSubItem(menu_mute);
                        if (subItem != null) {
                            subItem.setIcon(newMuteState ? R.drawable.msg_unmute : R.drawable.msg_mute);
                            subItem.setText(LocaleController.getString(
                                newMuteState ? "UnmuteNotifications" : "MuteNotifications",
                                newMuteState ? R.string.UnmuteNotifications : R.string.MuteNotifications
                            ));
                        }
                    }

                    // إرسال إشعار NotificationCenter لتحديث كافة المراقبين وواجهات المحادثة
                    NotificationCenter.getInstance(currentAccount).postNotificationName(
                        NotificationCenter.notificationsSettingsUpdated
                    );

                    // إظهار رسالة تأكيد للمستخدم
                    Context context = fragment.getParentActivity();
                    if (context != null) {
                        String message = LocaleController.getString(
                            newMuteState ? "NotificationsMuted" : "NotificationsUnmuted",
                            newMuteState ? R.string.NotificationsMuted : R.string.NotificationsUnmuted
                        );
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    AlertsCreator.showSimpleAlert(fragment, error.text);
                }
            });
        });
    }

    private static void shareChatLink(BaseFragment fragment, TLRPC.Chat chat, long dialogId) {
        if (fragment == null || fragment.getParentActivity() == null) return;
        String link = (chat != null && chat.username != null && !chat.username.isEmpty())
            ? "https://t.me/" + chat.username
            : "https://t.me/c/" + Math.abs(dialogId);

        AndroidUtilities.addToClipboard(link);
        Toast.makeText(fragment.getParentActivity(), LocaleController.getString("LinkCopied", R.string.LinkCopied), Toast.LENGTH_SHORT).show();
    }

    private static void clearChatHistory(BaseFragment fragment, int currentAccount, long dialogId) {
        if (fragment == null || fragment.getParentActivity() == null || dialogId == 0) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity());
        builder.setTitle(LocaleController.getString("ClearHistory", R.string.ClearHistory));
        builder.setMessage(LocaleController.getString("AreYouSureClearHistory", R.string.AreYouSureClearHistory));
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialog, which) -> {
            MessagesController.getInstance(currentAccount).deleteDialogHistory(dialogId, true);
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.messagesDidLoad, dialogId);
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        fragment.showDialog(builder.create());
    }
}
