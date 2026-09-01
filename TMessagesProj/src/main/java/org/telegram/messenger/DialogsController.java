package org.telegram.messenger;

import org.telegram.tgnet.TLRPC;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * DialogsController - Manages dialogs ordering, pinning, folder sorting, and active top message updates.
 * Follows DrKLO/Telegram DialogsController architecture.
 */
public class DialogsController {

    private static final DialogsController[] Instance = new DialogsController[UserConfig.MAX_ACCOUNT_COUNT];
    private final int currentAccount;

    public static final Comparator<TLRPC.Dialog> dialogComparator = (d1, d2) -> {
        if (d1.pinned && !d2.pinned) {
            return -1;
        } else if (!d1.pinned && d2.pinned) {
            return 1;
        } else if (d1.pinned && d2.pinned) {
            return Integer.compare(d2.pinnedNum, d1.pinnedNum);
        }
        
        MessagesController mc = MessagesController.getInstance(UserConfig.selectedAccount);
        TLRPC.Message m1 = mc.dialogMessage.get(d1.id);
        TLRPC.Message m2 = mc.dialogMessage.get(d2.id);
        
        int date1 = (m1 != null) ? m1.date : 0;
        int date2 = (m2 != null) ? m2.date : 0;

        return Integer.compare(date2, date1);
    };

    public static DialogsController getInstance(int num) {
        if (num < 0 || num >= UserConfig.MAX_ACCOUNT_COUNT) {
            num = 0;
        }
        DialogsController local = Instance[num];
        if (local == null) {
            synchronized (DialogsController.class) {
                local = Instance[num];
                if (local == null) {
                    Instance[num] = local = new DialogsController(num);
                }
            }
        }
        return local;
    }

    private DialogsController(int account) {
        currentAccount = account;
    }

    public void sortDialogs(ArrayList<TLRPC.Dialog> list) {
        if (list == null || list.isEmpty()) return;
        Collections.sort(list, dialogComparator);
    }

    public void updateDialogWithMessage(TLRPC.Message message) {
        if (message == null) return;
        MessagesController mc = MessagesController.getInstance(currentAccount);
        TLRPC.Dialog dialog = mc.dialogs_dict.get(message.peer_id);

        if (dialog == null) {
            dialog = new TLRPC.Dialog();
            dialog.id = message.peer_id;
            dialog.top_message = message.id;
            dialog.unread_count = (message.unread && !message.out) ? 1 : 0;
            mc.dialogs.add(dialog);
            mc.dialogs_dict.put(dialog.id, dialog);
        } else {
            dialog.top_message = message.id;
            if (message.unread && !message.out) {
                dialog.unread_count++;
            }
        }

        mc.dialogMessage.put(dialog.id, message);
        sortDialogs(mc.dialogs);

        MessagesStorage.getInstance(currentAccount).putDialog(dialog, message);
        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload);
    }
}
