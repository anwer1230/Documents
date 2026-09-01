package org.telegram.messenger;

import org.telegram.tgnet.TLRPC;
import java.util.HashMap;
import java.util.Map;

/**
 * AutoReplyManager
 * Handles automatic instant replies to incoming direct messages matching configured keyword triggers.
 */
public class AutoReplyManager extends BaseController {

    private static volatile AutoReplyManager[] Instance = new AutoReplyManager[UserConfig.MAX_ACCOUNT_COUNT];

    public static AutoReplyManager getInstance(int num) {
        AutoReplyManager localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (AutoReplyManager.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new AutoReplyManager(num);
                }
            }
        }
        return localInstance;
    }

    private boolean isEnabled = false;
    private final Map<String, String> triggerReplies = new HashMap<>();

    public AutoReplyManager(int num) {
        super(num);
        initDefaultRules();
    }

    private void initDefaultRules() {
        triggerReplies.put("سلام", "وعليكم السلام ورحمة الله وبركاته، كيف يمكنني مساعدتك؟");
        triggerReplies.put("مرحبا", "أهلاً وسهلاً بك! تفضل كيف أستطيع خدمتك؟");
        triggerReplies.put("اسعار", "أهلاً بك، يمكنك الاطلاع على باقات الأسعار المتاحة لدينا.");
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void inspectAndReply(TLRPC.Message message) {
        if (!isEnabled || message == null || message.message == null || message.out) return;

        String incoming = message.message.trim().toLowerCase();
        for (Map.Entry<String, String> entry : triggerReplies.entrySet()) {
            if (incoming.contains(entry.getKey())) {
                sendAutoReply(message, entry.getValue());
                break;
            }
        }
    }

    private void sendAutoReply(TLRPC.Message incomingMessage, String replyText) {
        TLRPC.TL_messages_sendMessage req = new TLRPC.TL_messages_sendMessage();
        req.peer = getMessagesController().getInputPeer(MessageObject.getDialogId(incomingMessage));
        req.message = replyText;
        req.random_id = Utilities.random.nextLong();

        getConnectionsManager().sendRequest(req, (response, error) -> {
            if (error == null && response instanceof TLRPC.Updates) {
                getMessagesController().processUpdates((TLRPC.Updates) response, false);
            }
        });
    }
}
