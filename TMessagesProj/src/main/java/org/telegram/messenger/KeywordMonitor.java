package org.telegram.messenger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.telegram.tgnet.TLRPC;

/**
 * KeywordMonitor.java (TMessagesProj/src/main/java/org/telegram/messenger/KeywordMonitor.java)
 * 
 * Specialized monitor service for tracking targeted keyword phrases in incoming messages
 * and instantly forwarding structured alerts to "Saved Messages" (الرسائل المحفوظة).
 */
public class KeywordMonitor {

    private static volatile KeywordMonitor[] Instance = new KeywordMonitor[UserConfig.MAX_ACCOUNT_COUNT];
    private final int currentAccount;
    private boolean isEnabled = true;

    // Fixed list of targeted sentences/phrases
    public static final String[] TARGET_KEYWORDS = {
        "اريد مساعدة",
        "ابي مساعدة",
        "من يسوي تكليف",
        "من يحل",
        "عندي بحث",
        "معي واجب",
        "عندي اسايمنت",
        "من يسوي اسايمنت",
        "ابي سكليف",
        "ابي عذر",
        "من يسوي سكليف",
        "ابي شخص مضمون",
        "ابي مختص",
        "هيليب",
        "من يستطيع",
        "تعرفون احد",
        "تعرفون شخص",
        "من يساعدني",
        "من يعرف مختص",
        "مين يعرف يحل واجب",
        "من يحل واجبات الجامعه",
        "أحتاج مساعدتكم",
        "ابي احد يسوي بحث",
        "عندي بحث",
        "مين يعرف مختص",
        "من يعرف احد كويس"
    };

    public static KeywordMonitor getInstance(int account) {
        KeywordMonitor localInstance = Instance[account];
        if (localInstance == null) {
            synchronized (KeywordMonitor.class) {
                localInstance = Instance[account];
                if (localInstance == null) {
                    Instance[account] = localInstance = new KeywordMonitor(account);
                }
            }
        }
        return localInstance;
    }

    public KeywordMonitor(int account) {
        this.currentAccount = account;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    /**
     * Inspects incoming message against targeted phrase list and notifies Saved Messages upon match.
     */
    public void inspectMessage(TLRPC.Message message) {
        if (!isEnabled || message == null || message.message == null || message.message.trim().isEmpty()) {
            return;
        }

        // Skip outgoing messages sent by self
        if (message.out) {
            return;
        }

        String text = message.message.trim();
        String matchedPhrase = findMatchingPhrase(text);

        if (matchedPhrase != null) {
            notifySavedMessages(message, matchedPhrase);
        }
    }

    /**
     * Matches full phrase within the incoming text
     */
    public String findMatchingPhrase(String text) {
        if (text == null) return null;
        String normalizedText = text.toLowerCase(Locale.getDefault());

        for (String phrase : TARGET_KEYWORDS) {
            if (phrase != null && !phrase.isEmpty()) {
                String normalizedPhrase = phrase.toLowerCase(Locale.getDefault());
                if (normalizedText.contains(normalizedPhrase)) {
                    return phrase;
                }
            }
        }
        return null;
    }

    /**
     * Dispatches notification to Saved Messages
     */
    private void notifySavedMessages(TLRPC.Message message, String matchedPhrase) {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                long selfUserId = UserConfig.getInstance(currentAccount).getClientUserId();
                if (selfUserId == 0) {
                    return;
                }

                // 1. Get Chat / Group Name
                String chatName = "محادثة خاصة";
                long dialogId = MessageObject.getDialogId(message);
                if (dialogId < 0) {
                    TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
                    if (chat != null && chat.title != null) {
                        chatName = chat.title;
                    }
                } else {
                    TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(dialogId);
                    if (user != null) {
                        chatName = UserObject.getUserName(user);
                    }
                }

                // 2. Get Sender Name
                String senderName = "مستخدم";
                long fromId = MessageObject.getFromChatId(message);
                if (fromId > 0) {
                    TLRPC.User senderUser = MessagesController.getInstance(currentAccount).getUser(fromId);
                    if (senderUser != null) {
                        senderName = UserObject.getUserName(senderUser);
                    }
                } else if (fromId < 0) {
                    TLRPC.Chat senderChat = MessagesController.getInstance(currentAccount).getChat(-fromId);
                    if (senderChat != null && senderChat.title != null) {
                        senderName = senderChat.title;
                    }
                }

                // 3. Format Date / Time
                String timeFormatted;
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd - HH:mm:ss", Locale.getDefault());
                    timeFormatted = sdf.format(new Date(message.date * 1000L));
                } catch (Exception e) {
                    timeFormatted = String.valueOf(message.date);
                }

                // 4. Construct Structured Notification Message
                StringBuilder notificationText = new StringBuilder();
                notificationText.append("🔔 تنبيه: تم رصد عبارة مستهدفة!\n\n");
                notificationText.append("📌 العبارة المطابقة: ").append(matchedPhrase).append("\n");
                notificationText.append("👥 المجموعة / المحادثة: ").append(chatName).append("\n");
                notificationText.append("👤 اسم المرسل: ").append(senderName).append("\n");
                notificationText.append("⏰ الوقت: ").append(timeFormatted).append("\n\n");
                notificationText.append("💬 نص الرسالة:\n").append(message.message);

                // 5. Send message to Saved Messages (Peer = selfUserId)
                SendMessagesHelper.getInstance(currentAccount).sendMessage(
                    SendMessagesHelper.SendMessageParams.of(
                        notificationText.toString(),
                        selfUserId,
                        null,
                        null,
                        null,
                        true,
                        null,
                        null,
                        null,
                        true,
                        0,
                        null,
                        false
                    )
                );

                FileLog.d("KeywordMonitor: Successfully notified Saved Messages for matched keyword: " + matchedPhrase);
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }
}
