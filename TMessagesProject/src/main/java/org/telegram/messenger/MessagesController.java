package org.telegram.messenger;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * MessagesController - Replicates DrKLO/Telegram MessagesController.java
 * Multi-account dialog loader, message coordinator, and live update processor.
 */
public class MessagesController {
    private static final MessagesController[] Instance = new MessagesController[UserConfig.MAX_ACCOUNT_COUNT];
    private final int currentAccount;

    public final ArrayList<TLRPC.Dialog> dialogs = new ArrayList<>();
    public final HashMap<Long, TLRPC.Dialog> dialogs_dict = new HashMap<>();
    public final HashMap<Long, TLRPC.Chat> chats = new HashMap<>();
    public final HashMap<Long, TLRPC.User> users = new HashMap<>();
    public boolean loadingDialogs = false;

    public static MessagesController getInstance(int num) {
        if (num < 0 || num >= UserConfig.MAX_ACCOUNT_COUNT) {
            num = 0;
        }
        MessagesController local = Instance[num];
        if (local == null) {
            synchronized (MessagesController.class) {
                local = Instance[num];
                if (local == null) {
                    Instance[num] = local = new MessagesController(num);
                }
            }
        }
        return local;
    }

    private MessagesController(int account) {
        currentAccount = account;
    }

    public void loadDialogs(int folderId, int offset, int count, boolean fromCache) {
        if (loadingDialogs) return;
        loadingDialogs = true;

        TLRPC.TL_messages_getDialogs req = new TLRPC.TL_messages_getDialogs();
        req.folder_id = folderId;
        req.offset_id = offset;
        req.limit = count;

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            loadingDialogs = false;
            if (response instanceof TLRPC.messages_Dialogs) {
                TLRPC.messages_Dialogs res = (TLRPC.messages_Dialogs) response;
                for (TLRPC.User user : res.users) {
                    users.put(user.id, user);
                }
                for (TLRPC.Chat chat : res.chats) {
                    chats.put(chat.id, chat);
                }
                for (TLRPC.Dialog dialog : res.dialogs) {
                    if (!dialogs_dict.containsKey(dialog.id)) {
                        dialogs.add(dialog);
                        dialogs_dict.put(dialog.id, dialog);
                    }
                }
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload);
            }
        });
    }

    public void processUpdate(TLRPC.Message message) {
        if (message == null) return;
        TLRPC.Dialog dialog = dialogs_dict.get(message.peer_id);
        if (dialog != null) {
            dialog.top_message = message.id;
            if (message.unread && !message.out) {
                dialog.unread_count++;
            }
        }
        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.didReceivedNewMessages, message);
    }
}
