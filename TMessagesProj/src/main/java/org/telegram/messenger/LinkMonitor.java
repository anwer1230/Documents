package org.telegram.messenger;

import org.telegram.tgnet.TLRPC;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * LinkMonitor
 * Live monitoring radar that tracks incoming messages for keywords/phrases and channel invite links.
 */
public class LinkMonitor extends BaseController {

    private static volatile LinkMonitor[] Instance = new LinkMonitor[UserConfig.MAX_ACCOUNT_COUNT];

    public static LinkMonitor getInstance(int num) {
        LinkMonitor localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (LinkMonitor.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new LinkMonitor(num);
                }
            }
        }
        return localInstance;
    }

    private final Set<String> targetPhrases = new HashSet<>();
    private boolean isMonitoringEnabled = true;

    public LinkMonitor(int num) {
        super(num);
        initDefaultPhrases();
    }

    private void initDefaultPhrases() {
        targetPhrases.add("اريد مساعدة");
        targetPhrases.add("ابي مساعدة");
        targetPhrases.add("من يسوي تكليف");
        targetPhrases.add("من يحل");
        targetPhrases.add("عندي بحث");
        targetPhrases.add("معي واجب");
        targetPhrases.add("عندي اسايمنت");
        targetPhrases.add("من يسوي اسايمنت");
        targetPhrases.add("ابي سكليف");
        targetPhrases.add("ابي عذر");
        targetPhrases.add("ابي شخص مضمون");
        targetPhrases.add("ابي مختص");
        targetPhrases.add("من يستطيع");
        targetPhrases.add("تعرفون احد");
        targetPhrases.add("من يساعدني");
    }

    public void setMonitoringEnabled(boolean enabled) {
        this.isMonitoringEnabled = enabled;
    }

    public boolean isMonitoringEnabled() {
        return isMonitoringEnabled;
    }

    /**
     * Called on incoming TLRPC.UpdateShortMessage or TLRPC.UpdateNewMessage
     */
    public void inspectMessage(TLRPC.Message message) {
        if (!isMonitoringEnabled || message == null || message.message == null) return;

        String text = message.message.toLowerCase();
        for (String phrase : targetPhrases) {
            if (text.contains(phrase)) {
                onPhraseMatched(message, phrase);
                break;
            }
        }

        // Detect links for automatic tracking
        if (text.contains("t.me/") || text.contains("telegram.me/")) {
            onLinkDetected(message, text);
        }
    }

    private void onPhraseMatched(TLRPC.Message message, String matchedPhrase) {
        AndroidUtilities.runOnUIThread(() -> {
            long dialogId = MessageObject.getDialogId(message);
            NotificationsController.getInstance(currentAccount).showBulletNotification(
                "رادار المراقبة اللحظية 🎯",
                "تم رصد عبارة: " + matchedPhrase + " في المحادثة #" + dialogId,
                dialogId
            );
        });
    }

    private void onLinkDetected(TLRPC.Message message, String text) {
        // Automatically notify or pass to AdvancedJoiner if relevant
    }
}
