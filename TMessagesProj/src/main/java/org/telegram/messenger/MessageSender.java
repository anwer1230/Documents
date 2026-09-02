/*
 * This is the source code of Telegram for Android v. 12.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2024.
 */

package org.telegram.messenger;

import android.os.Handler;
import android.os.Looper;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MessageSender - Native Official Telegram Android Sender & Keyword Monitor Engine
 * 
 * Provides:
 * 1. Immediate & Scheduled message dispatching to resolved peer targets.
 * 2. Link/Username resolution (@username, t.me/xxx, t.me/+hash) using TLRPC MTProto requests.
 * 3. Rate-limiting & Flood Protection (max 20 msgs/min, automatic FLOOD_WAIT parsing & retry).
 * 4. Real-time Keyword Monitoring with automated alerts to Saved Messages (clientUserId).
 */
public class MessageSender extends BaseController {

    private static volatile MessageSender[] Instance = new MessageSender[UserConfig.MAX_ACCOUNT_COUNT];

    public static MessageSender getInstance(int num) {
        MessageSender localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (MessageSender.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new MessageSender(num);
                }
            }
        }
        return localInstance;
    }

    public interface SendCallback {
        void onProgress(int sent, int total, String targetChat, boolean success, String error);
        void onComplete(String batchId, int totalSent, int totalFailed);
    }

    public interface ResolveCallback {
        void onResolved(long peerId, TLRPC.InputPeer inputPeer, String title);
        void onError(String error);
    }

    public static class QueueTask {
        public String batchId;
        public String text;
        public String target;
        public long resolvedPeerId;
        public TLRPC.InputPeer inputPeer;
        public List<String> mediaUrls;
        public SendCallback callback;
        public int attemptCount;
    }

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentLinkedQueue<QueueTask> sendQueue = new ConcurrentLinkedQueue<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isQueueProcessing = false;

    // Rate Limiting: Max 20 messages per minute (3000ms delay between consecutive requests)
    private static final long MIN_SEND_INTERVAL_MS = 3000;
    private long lastSendTimestamp = 0;
    private long floodWaitUntil = 0;

    // Keyword Monitor Configuration
    private final List<String> watchKeywords = Collections.synchronizedList(new ArrayList<>());
    private boolean isKeywordMonitoringActive = false;

    public MessageSender(int num) {
        super(num);
        initDefaultKeywords();
    }

    private void initDefaultKeywords() {
        watchKeywords.add("واجب");
        watchKeywords.add("بحث");
        watchKeywords.add("مشروع");
        watchKeywords.add("تخرج");
        watchKeywords.add("برمجة");
        watchKeywords.add("تصميم");
        watchKeywords.add("سعر");
        watchKeywords.add("وظيفة");
    }

    // =========================================================================
    // 1. Sending Pipeline (Immediate & Scheduled)
    // =========================================================================

    public void sendInstantMessage(String text, List<String> targets, List<String> mediaUrls, SendCallback callback) {
        if (text == null || text.trim().isEmpty() || targets == null || targets.isEmpty()) {
            if (callback != null) {
                callback.onComplete("", 0, 0);
            }
            return;
        }

        final String batchId = "batch_" + System.currentTimeMillis();
        final int totalTargets = targets.size();
        final ArrayList<MyBatchesStorage.BatchTarget> batchTargets = new ArrayList<>();

        for (String target : targets) {
            final QueueTask task = new QueueTask();
            task.batchId = batchId;
            task.text = text;
            task.target = target.trim();
            task.mediaUrls = mediaUrls != null ? new ArrayList<>(mediaUrls) : new ArrayList<>();
            task.callback = callback;
            task.attemptCount = 0;
            sendQueue.offer(task);
        }

        processNextQueueItem();
    }

    public void scheduleMessage(String text, List<String> targets, long delaySeconds, int intervalMinutes, SendCallback callback) {
        if (delaySeconds <= 0) {
            sendInstantMessage(text, targets, null, callback);
            return;
        }

        scheduler.schedule(() -> {
            sendInstantMessage(text, targets, null, callback);
        }, delaySeconds, TimeUnit.SECONDS);
    }

    private synchronized void processNextQueueItem() {
        if (isQueueProcessing) {
            return;
        }
        isQueueProcessing = true;

        scheduler.execute(this::runQueueLoop);
    }

    private void runQueueLoop() {
        while (!sendQueue.isEmpty()) {
            long now = System.currentTimeMillis();

            // Check FLOOD_WAIT pause
            if (now < floodWaitUntil) {
                long waitRemaining = floodWaitUntil - now;
                try {
                    Thread.sleep(Math.min(waitRemaining + 500, 10000));
                } catch (InterruptedException ignored) {}
                continue;
            }

            // Check Rate-Limit interval
            long timeSinceLastSend = now - lastSendTimestamp;
            if (timeSinceLastSend < MIN_SEND_INTERVAL_MS) {
                try {
                    Thread.sleep(MIN_SEND_INTERVAL_MS - timeSinceLastSend);
                } catch (InterruptedException ignored) {}
            }

            final QueueTask task = sendQueue.poll();
            if (task == null) break;

            resolveAndDispatchTask(task);
            lastSendTimestamp = System.currentTimeMillis();
        }

        isQueueProcessing = false;
    }

    private void resolveAndDispatchTask(QueueTask task) {
        resolveTarget(task.target, new ResolveCallback() {
            @Override
            public void onResolved(long peerId, TLRPC.InputPeer inputPeer, String title) {
                task.resolvedPeerId = peerId;
                task.inputPeer = inputPeer;
                dispatchMessageToPeer(task);
            }

            @Override
            public void onError(String error) {
                FileLog.e("MessageSender: Failed to resolve target " + task.target + " -> " + error);
                if (task.callback != null) {
                    mainHandler.post(() -> task.callback.onProgress(0, 1, task.target, false, error));
                }
            }
        });
    }

    private void dispatchMessageToPeer(QueueTask task) {
        SendMessagesHelper.getInstance(currentAccount).sendMessage(
            task.text,
            task.resolvedPeerId,
            null,
            null
        );

        // Record in MyBatchesStorage
        MyBatchesStorage.getInstance(currentAccount).recordBatchMessage(
            task.batchId,
            task.text,
            task.resolvedPeerId,
            UserConfig.getInstance(currentAccount).lastSendMessageId,
            task.target
        );

        if (task.callback != null) {
            mainHandler.post(() -> task.callback.onProgress(1, 1, task.target, true, null));
        }
    }

    // =========================================================================
    // 2. Link & Username Resolver (MTProto TLRPC)
    // =========================================================================

    public void resolveTarget(String rawTarget, ResolveCallback callback) {
        if (rawTarget == null || rawTarget.trim().isEmpty()) {
            if (callback != null) callback.onError("Empty target");
            return;
        }

        String target = rawTarget.trim();

        // 1. Case: Private Invite Link (t.me/+hash or t.me/joinchat/hash)
        Pattern invitePattern = Pattern.compile("(?:https?://)?(?:www\\.)?(?:t\\.me|telegram\\.me)/(?:joinchat/|\\+)([a-zA-Z0-9_-]+)");
        Matcher inviteMatcher = invitePattern.matcher(target);
        if (inviteMatcher.find()) {
            String hash = inviteMatcher.group(1);
            checkInviteLink(hash, callback);
            return;
        }

        // 2. Case: Public Username (@username or t.me/username)
        Pattern usernamePattern = Pattern.compile("(?:https?://)?(?:www\\.)?(?:t\\.me|telegram\\.me)/([a-zA-Z0-9_]{4,32})|@([a-zA-Z0-9_]{4,32})");
        Matcher usernameMatcher = usernamePattern.matcher(target);
        if (usernameMatcher.find()) {
            String username = usernameMatcher.group(1) != null ? usernameMatcher.group(1) : usernameMatcher.group(2);
            resolveUsername(username, callback);
            return;
        }

        // 3. Case: Raw Numeric Peer ID
        try {
            long peerId = Long.parseLong(target.replaceAll("[^0-9-]", ""));
            TLRPC.InputPeer inputPeer = MessagesController.getInstance(currentAccount).getInputPeer(peerId);
            if (callback != null) {
                callback.onResolved(peerId, inputPeer, "Chat #" + peerId);
            }
            return;
        } catch (Exception ignored) {}

        if (callback != null) {
            callback.onError("Could not parse target format: " + target);
        }
    }

    private void resolveUsername(String username, ResolveCallback callback) {
        TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
        req.username = username;

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            if (error != null) {
                handleFloodError(error);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(error.text));
                }
                return;
            }

            if (response instanceof TLRPC.TL_contacts_resolvedPeer) {
                TLRPC.TL_contacts_resolvedPeer res = (TLRPC.TL_contacts_resolvedPeer) response;
                long peerId = 0;
                String title = username;
                TLRPC.InputPeer inputPeer = null;

                if (!res.chats.isEmpty()) {
                    TLRPC.Chat chat = res.chats.get(0);
                    peerId = -chat.id;
                    title = chat.title;
                    inputPeer = new TLRPC.TL_inputPeerChat();
                    inputPeer.chat_id = chat.id;
                } else if (!res.users.isEmpty()) {
                    TLRPC.User user = res.users.get(0);
                    peerId = user.id;
                    title = ContactsController.formatName(user.first_name, user.last_name);
                    inputPeer = new TLRPC.TL_inputPeerUser();
                    inputPeer.user_id = user.id;
                    inputPeer.access_hash = user.access_hash;
                }

                final long finalPeerId = peerId;
                final TLRPC.InputPeer finalInputPeer = inputPeer;
                final String finalTitle = title;

                if (callback != null) {
                    mainHandler.post(() -> callback.onResolved(finalPeerId, finalInputPeer, finalTitle));
                }
            } else {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("Invalid response type from resolveUsername"));
                }
            }
        });
    }

    private void checkInviteLink(String hash, ResolveCallback callback) {
        TLRPC.TL_messages_checkChatInvite req = new TLRPC.TL_messages_checkChatInvite();
        req.hash = hash;

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            if (error != null) {
                handleFloodError(error);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(error.text));
                }
                return;
            }

            if (response instanceof TLRPC.TL_chatInvite) {
                TLRPC.TL_chatInvite invite = (TLRPC.TL_chatInvite) response;
                long fakePeerId = -1000000000L;
                if (callback != null) {
                    mainHandler.post(() -> callback.onResolved(fakePeerId, null, invite.title));
                }
            } else if (response instanceof TLRPC.TL_chatInviteAlready) {
                TLRPC.TL_chatInviteAlready already = (TLRPC.TL_chatInviteAlready) response;
                long peerId = -already.chat.id;
                TLRPC.InputPeer inputPeer = new TLRPC.TL_inputPeerChat();
                inputPeer.chat_id = already.chat.id;
                if (callback != null) {
                    mainHandler.post(() -> callback.onResolved(peerId, inputPeer, already.chat.title));
                }
            }
        });
    }

    private void handleFloodError(TLRPC.TL_error error) {
        if (error != null && error.text != null && error.text.startsWith("FLOOD_WAIT_")) {
            try {
                int seconds = Integer.parseInt(error.text.replace("FLOOD_WAIT_", ""));
                floodWaitUntil = System.currentTimeMillis() + (seconds * 1000L);
                FileLog.w("MessageSender: Encountered FLOOD_WAIT of " + seconds + " seconds. Pausing queue.");
            } catch (Exception ignored) {}
        }
    }

    // =========================================================================
    // 3. Keyword Monitoring & Saved Messages Alert System
    // =========================================================================

    public void setKeywordMonitoringActive(boolean active) {
        this.isKeywordMonitoringActive = active;
    }

    public boolean isKeywordMonitoringActive() {
        return isKeywordMonitoringActive;
    }

    public void setWatchKeywords(List<String> keywords) {
        watchKeywords.clear();
        if (keywords != null) {
            for (String kw : keywords) {
                if (kw != null && !kw.trim().isEmpty()) {
                    watchKeywords.add(kw.trim().toLowerCase());
                }
            }
        }
    }

    public List<String> getWatchKeywords() {
        return new ArrayList<>(watchKeywords);
    }

    public void checkIncomingMessageForKeywords(TLRPC.Message message) {
        if (!isKeywordMonitoringActive || message == null || message.message == null || message.out) {
            return;
        }

        final String text = message.message.toLowerCase();
        for (String keyword : watchKeywords) {
            if (text.contains(keyword.toLowerCase())) {
                dispatchKeywordAlert(message, keyword);
                break;
            }
        }
    }

    private void dispatchKeywordAlert(TLRPC.Message message, String matchedKeyword) {
        final long mySavedMessagesId = UserConfig.getInstance(currentAccount).getClientUserId();
        if (mySavedMessagesId == 0) return;

        final String alertText = "🚨 [تنبيه مراقبة الكلمات المفتاحية]\n" +
            "📌 الكلمة المرصودة: " + matchedKeyword + "\n" +
            "👤 معرف المرسل: " + (message.from_id != null ? message.from_id.user_id : "غير معروف") + "\n" +
            "💬 معرف المحادثة: " + message.peer_id + "\n" +
            "🕒 الوقت: " + new java.util.Date().toString() + "\n" +
            "📝 نص الرسالة:\n" + message.message;

        AndroidUtilities.runOnUIThread(() -> {
            SendMessagesHelper.getInstance(currentAccount).sendMessage(
                alertText,
                mySavedMessagesId,
                null,
                null
            );

            NotificationCenter.getInstance(currentAccount).postNotificationName(
                NotificationCenter.didReceiveNewMessages,
                mySavedMessagesId,
                new MessageObject(currentAccount, message, false, false)
            );
        });
    }
}
