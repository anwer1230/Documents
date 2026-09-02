/*
 * This is the source code of Telegram for Android v. 12.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2024.
 */

package org.telegram.messenger;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.LaunchActivity;
import java.util.HashSet;
import java.util.Set;

/**
 * KeywordMonitor - Real-Time Background Intelligent Keyword Radar & Instant Alert System
 * 
 * Features:
 * 1. Hardcoded permanent list of target keywords/phrases.
 * 2. Enabled by default (always running in background).
 * 3. Deep link generator for target groups (https://t.me/c/...), sender profiles (tg://user?id=...), and direct message jumps.
 * 4. Dispatches both Rich Native System Notifications (with PendingIntent) and Telegram Saved Messages alerts.
 */
public class KeywordMonitor extends BaseController {

    private static volatile KeywordMonitor[] Instance = new KeywordMonitor[UserConfig.MAX_ACCOUNT_COUNT];

    public static KeywordMonitor getInstance(int num) {
        KeywordMonitor localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (KeywordMonitor.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new KeywordMonitor(num);
                }
            }
        }
        return localInstance;
    }

    // 1. Hardcoded Permanent Keyword List
    private static final String[] KEYWORDS = {
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
        "ابي مختص",
        "مين يعرف يحل واجب",
        "من يحل واجبات الجامعه",
        "أحتاج مساعدتكم",
        "ابي احد يسوي بحث",
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
        "ابي مختص",
        "مين يعرف يحل واجب",
        "من يحل واجبات الجامعه",
        "أحتاج مساعدتكم",
        "ابي احد يسوي بحث",
        "عندي بحث",
        "مين يعرف مختص",
        "من يعرف احد كويس"
    };

    // 2. Default Enabled State (Runs permanently in background)
    private static boolean isEnabled = true;

    private static final String NOTIFICATION_CHANNEL_ID = "telegram_keyword_radar_channel";
    private final Set<String> processedMessageKeys = new HashSet<>();

    public KeywordMonitor(int num) {
        super(num);
        initNotificationChannel();
    }

    public static boolean isMonitoringEnabled() {
        return isEnabled;
    }

    public static void setMonitoringEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    private void initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationManager nm = (NotificationManager) ApplicationLoader.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) {
                    NotificationChannel channel = new NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        "رادار الكلمات المفتاحية الذكي",
                        NotificationManager.IMPORTANCE_HIGH
                    );
                    channel.setDescription("تنبيهات فورية عند رصد الكلمات المفتاحية المستهدفة في المجموعات والقنوات");
                    channel.enableVibration(true);
                    channel.enableLights(true);
                    nm.createNotificationChannel(channel);
                }
            } catch (Exception e) {
                FileLog.e("KeywordMonitor: Failed to create notification channel: " + e.getMessage());
            }
        }
    }

    /**
     * Inspects incoming messages in real-time from MessagesController.processUpdates
     */
    public void inspectMessage(TLRPC.Message message) {
        if (!isEnabled || message == null || message.out || message.message == null) {
            return;
        }

        final String text = message.message.trim();
        if (text.isEmpty()) {
            return;
        }

        // Prevent duplicate processing
        String uniqueKey = message.peer_id + "_" + message.id;
        synchronized (processedMessageKeys) {
            if (processedMessageKeys.contains(uniqueKey)) {
                return;
            }
            if (processedMessageKeys.size() > 5000) {
                processedMessageKeys.clear();
            }
            processedMessageKeys.add(uniqueKey);
        }

        final String lowerCaseText = normalizeArabicText(text);

        for (String keyword : KEYWORDS) {
            String normalizedKeyword = normalizeArabicText(keyword.trim());
            if (lowerCaseText.contains(normalizedKeyword)) {
                onKeywordDetected(message, keyword, text);
                break;
            }
        }
    }

    private String normalizeArabicText(String input) {
        if (input == null) return "";
        return input.toLowerCase()
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
            .replaceAll("\\s+", " ")
            .trim();
    }

    /**
     * Handles detected keyword: sends rich alert to Saved Messages & triggers native Android Notification with jump PendingIntent
     */
    private void onKeywordDetected(TLRPC.Message message, String matchedKeyword, String originalText) {
        long dialogId = message.peer_id;
        long senderUserId = 0;
        String senderName = "مستخدم";
        String senderUsername = "";

        if (message.from_id != null) {
            senderUserId = message.from_id.user_id;
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(senderUserId);
            if (user != null) {
                senderName = ContactsController.formatName(user.first_name, user.last_name);
                if (user.username != null && !user.username.isEmpty()) {
                    senderUsername = user.username;
                }
            }
        }

        // Resolve Chat / Channel Title
        String chatTitle = "محادثة #" + dialogId;
        String chatUsername = "";
        boolean isChannelOrSupergroup = false;
        long channelId = 0;

        if (dialogId < 0) {
            long rawChatId = -dialogId;
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(rawChatId);
            if (chat != null) {
                chatTitle = chat.title != null ? chat.title : chatTitle;
                if (chat.username != null && !chat.username.isEmpty()) {
                    chatUsername = chat.username;
                }
                if (ChatObject.isChannel(chat)) {
                    isChannelOrSupergroup = true;
                    channelId = chat.id;
                }
            }
        }

        // =====================================================================
        // 1. Build Direct Jump Links (الروابط الحية التفاعلية)
        // =====================================================================

        // A. Direct Message Link (رابط الرسالة)
        String messageLink;
        if (chatUsername != null && !chatUsername.isEmpty()) {
            messageLink = "https://t.me/" + chatUsername + "/" + message.id;
        } else if (isChannelOrSupergroup && channelId != 0) {
            messageLink = "https://t.me/c/" + channelId + "/" + message.id;
        } else {
            messageLink = "https://t.me/c/" + Math.abs(dialogId) + "/" + message.id;
        }

        // B. Chat / Group Link (رابط المجموعة)
        String chatLink;
        if (chatUsername != null && !chatUsername.isEmpty()) {
            chatLink = "https://t.me/" + chatUsername;
        } else if (isChannelOrSupergroup && channelId != 0) {
            chatLink = "https://t.me/c/" + channelId + "/" + message.id;
        } else {
            chatLink = "tg://resolve?domain=" + Math.abs(dialogId);
        }

        // C. Sender Profile Link (معرف ورابط المرسل)
        String senderLink;
        if (senderUsername != null && !senderUsername.isEmpty()) {
            senderLink = "https://t.me/" + senderUsername;
        } else if (senderUserId != 0) {
            senderLink = "tg://user?id=" + senderUserId;
        } else {
            senderLink = "غير متوفر";
        }

        // =====================================================================
        // 2. Dispatch Saved Messages Alert (الرسائل المحفوظة)
        // =====================================================================
        final long mySavedMessagesId = UserConfig.getInstance(currentAccount).getClientUserId();

        final String savedMessageAlert = "🎯 <b>[رادار الكلمات المفتاحية الذكي]</b>\n\n" +
            "📌 <b>الكلمة المرصودة:</b> " + matchedKeyword + "\n" +
            "👥 <b>المجموعة / القناة:</b> <a href=\"" + chatLink + "\">" + chatTitle + "</a>\n" +
            "👤 <b>المرسل:</b> <a href=\"" + senderLink + "\">" + senderName + (senderUsername.isEmpty() ? "" : " (@" + senderUsername + ")") + "</a>\n" +
            "🔗 <b>الذهاب للرسالة مباشرة:</b> <a href=\"" + messageLink + "\">اضغط هنا للانتقال للرسالة ↗️</a>\n\n" +
            "💬 <b>نص الرسالة:</b>\n" + originalText;

        AndroidUtilities.runOnUIThread(() -> {
            if (mySavedMessagesId != 0) {
                SendMessagesHelper.getInstance(currentAccount).sendMessage(
                    savedMessageAlert,
                    mySavedMessagesId,
                    null,
                    null
                );
            }

            // =================================================================
            // 3. Post Native System Notification with Jump PendingIntent
            // =================================================================
            postSystemNotification(dialogId, message.id, chatTitle, matchedKeyword, originalText);
        });
    }

    /**
     * Builds and posts a High-Priority Android System Notification with PendingIntent
     */
    private void postSystemNotification(long dialogId, int messageId, String chatTitle, String keyword, String text) {
        try {
            Context context = ApplicationLoader.applicationContext;
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            // Create PendingIntent that opens LaunchActivity and jumps straight to the message
            Intent intent = new Intent(context, LaunchActivity.class);
            intent.setAction("org.telegram.messenger.OPEN_CHAT_" + dialogId + "_" + messageId);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("dialogId", dialogId);
            intent.putExtra("messageId", messageId);
            intent.putExtra("currentAccount", currentAccount);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) (dialogId ^ messageId),
                intent,
                flags
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle("🎯 رصد كلمة: " + keyword + " في " + chatTitle)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText("📌 الكلمة: " + keyword + "\n👥 المصدر: " + chatTitle + "\n💬 الرسالة: " + text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

            int notificationId = (int) (System.currentTimeMillis() % 100000);
            nm.notify(notificationId, builder.build());

        } catch (Exception e) {
            FileLog.e("KeywordMonitor: Error posting system notification: " + e.getMessage());
        }
    }
}
