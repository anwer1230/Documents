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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AutoJoiner - Automatic Group/Channel Joining Engine with Anti-Flood Queue
 * 
 * Features:
 * 1. Deep link extraction from text (Public: t.me/xxx, @xxx; Private: t.me/+xxx, t.me/joinchat/xxx).
 * 2. Uses TLRPC.TL_channels_joinChannel for public groups and TLRPC.TL_messages_importChatInvite for private.
 * 3. Anti-Flood & Rate Control: 30-60s randomized delay between joins, max 20 joins/hour, FLOOD_WAIT auto-pause.
 * 4. Real-time Live Link Radar for automatic joining from incoming chat streams.
 */
public class AutoJoiner extends BaseController {

    private static volatile AutoJoiner[] Instance = new AutoJoiner[UserConfig.MAX_ACCOUNT_COUNT];

    public static AutoJoiner getInstance(int num) {
        AutoJoiner localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (AutoJoiner.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new AutoJoiner(num);
                }
            }
        }
        return localInstance;
    }

    public interface AutoJoinCallback {
        void onStart(int totalLinks);
        void onProgress(int current, int total, String link, boolean success, String title, String error);
        void onComplete(int successCount, int failedCount);
    }

    public static class JoinTask {
        public String originalLink;
        public boolean isPrivate;
        public String identifier; // username or invite hash
        public int attempts = 0;
    }

    private final ConcurrentLinkedQueue<JoinTask> joinQueue = new ConcurrentLinkedQueue<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private boolean isRunning = false;
    private boolean isRadarActive = false;
    private int totalTaskCount = 0;
    private int currentProcessed = 0;
    private int successCount = 0;
    private int failedCount = 0;
    private AutoJoinCallback activeCallback;

    // Flood limits: Max 20 joins/hour, 30-60s delay
    private static final int MAX_JOINS_PER_HOUR = 20;
    private final LinkedList<Long> hourlyJoinTimestamps = new LinkedList<>();
    private long floodWaitUntil = 0;

    public AutoJoiner(int num) {
        super(num);
    }

    // =========================================================================
    // 1. Link Extraction Regex Utility
    // =========================================================================

    public static List<String> extractLinks(String text) {
        List<String> links = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return links;

        // Pattern handles t.me/+hash, t.me/joinchat/hash, t.me/username, @username
        Pattern pattern = Pattern.compile("(?i)(?:https?://)?(?:www\\.)?(?:t(?:elegram)?\\.me|telegram\\.dog)/(?:joinchat/|\\+)?([a-zA-Z0-9_-]+)|@([a-zA-Z0-9_]{4,32})");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String fullMatch = matcher.group();
            if (!links.contains(fullMatch)) {
                links.add(fullMatch);
            }
        }
        return links;
    }

    // =========================================================================
    // 2. Queue & Join Execution
    // =========================================================================

    public void startAutoJoin(List<String> links, AutoJoinCallback callback) {
        if (links == null || links.isEmpty()) {
            if (callback != null) callback.onComplete(0, 0);
            return;
        }

        this.activeCallback = callback;
        this.joinQueue.clear();
        this.currentProcessed = 0;
        this.successCount = 0;
        this.failedCount = 0;

        for (String rawLink : links) {
            JoinTask task = parseLinkToTask(rawLink);
            if (task != null) {
                joinQueue.offer(task);
            }
        }

        this.totalTaskCount = joinQueue.size();
        if (callback != null) {
            callback.onStart(totalTaskCount);
        }

        if (!isRunning) {
            isRunning = true;
            Utilities.globalQueue.postRunnable(this::processNextTask);
        }
    }

    public void stopAutoJoin() {
        isRunning = false;
        joinQueue.clear();
        if (activeCallback != null) {
            mainHandler.post(() -> activeCallback.onComplete(successCount, failedCount));
        }
    }

    private JoinTask parseLinkToTask(String link) {
        if (link == null || link.trim().isEmpty()) return null;
        String clean = link.trim();

        JoinTask task = new JoinTask();
        task.originalLink = clean;

        // Private invite link
        Pattern privatePattern = Pattern.compile("(?i)(?:https?://)?(?:www\\.)?(?:t\\.me|telegram\\.me)/(?:joinchat/|\\+)([a-zA-Z0-9_-]+)");
        Matcher privateMatcher = privatePattern.matcher(clean);
        if (privateMatcher.find()) {
            task.isPrivate = true;
            task.identifier = privateMatcher.group(1);
            return task;
        }

        // Public username
        Pattern publicPattern = Pattern.compile("(?i)(?:https?://)?(?:www\\.)?(?:t\\.me|telegram\\.me)/([a-zA-Z0-9_]{4,32})|@([a-zA-Z0-9_]{4,32})");
        Matcher publicMatcher = publicPattern.matcher(clean);
        if (publicMatcher.find()) {
            task.isPrivate = false;
            task.identifier = publicMatcher.group(1) != null ? publicMatcher.group(1) : publicMatcher.group(2);
            return task;
        }

        return null;
    }

    private void processNextTask() {
        if (!isRunning || joinQueue.isEmpty()) {
            isRunning = false;
            if (activeCallback != null) {
                mainHandler.post(() -> activeCallback.onComplete(successCount, failedCount));
            }
            return;
        }

        long now = System.currentTimeMillis();

        // 1. Check FLOOD_WAIT
        if (now < floodWaitUntil) {
            long waitDuration = floodWaitUntil - now;
            FileLog.w("AutoJoiner: Paused due to FLOOD_WAIT for " + (waitDuration / 1000) + "s");
            Utilities.globalQueue.postRunnable(this::processNextTask, Math.min(waitDuration + 1000, 10000));
            return;
        }

        // 2. Check Hourly Join Limit (Max 20/hour)
        cleanOldHourlyJoins(now);
        if (hourlyJoinTimestamps.size() >= MAX_JOINS_PER_HOUR) {
            long earliest = hourlyJoinTimestamps.peekFirst();
            long waitHour = (earliest + 3600000L) - now;
            FileLog.w("AutoJoiner: Hourly limit reached (20/hr). Waiting " + (waitHour / 1000) + "s");
            Utilities.globalQueue.postRunnable(this::processNextTask, Math.max(waitHour, 5000));
            return;
        }

        final JoinTask task = joinQueue.poll();
        if (task == null) {
            processNextTask();
            return;
        }

        if (task.isPrivate) {
            importPrivateInvite(task);
        } else {
            resolveAndJoinPublicChannel(task);
        }
    }

    private void cleanOldHourlyJoins(long now) {
        while (!hourlyJoinTimestamps.isEmpty() && now - hourlyJoinTimestamps.peekFirst() > 3600000L) {
            hourlyJoinTimestamps.pollFirst();
        }
    }

    // =========================================================================
    // 3. Public Channels (TL_channels_joinChannel)
    // =========================================================================

    private void resolveAndJoinPublicChannel(JoinTask task) {
        TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
        req.username = task.identifier;

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            if (error != null) {
                handleJoinError(task, error);
                return;
            }

            if (response instanceof TLRPC.TL_contacts_resolvedPeer) {
                TLRPC.TL_contacts_resolvedPeer res = (TLRPC.TL_contacts_resolvedPeer) response;
                if (!res.chats.isEmpty()) {
                    TLRPC.Chat chat = res.chats.get(0);
                    TLRPC.TL_channels_joinChannel joinReq = new TLRPC.TL_channels_joinChannel();
                    TLRPC.TL_inputChannel inputChannel = new TLRPC.TL_inputChannel();
                    inputChannel.channel_id = chat.id;
                    inputChannel.access_hash = chat.access_hash;
                    joinReq.channel = inputChannel;

                    ConnectionsManager.getInstance(currentAccount).sendRequest(joinReq, (joinRes, joinErr) -> {
                        if (joinErr != null) {
                            handleJoinError(task, joinErr);
                        } else {
                            handleJoinSuccess(task, chat.title);
                        }
                    });
                } else {
                    handleJoinError(task, "Not a valid group/channel");
                }
            }
        });
    }

    // =========================================================================
    // 4. Private Channels (TL_messages_importChatInvite)
    // =========================================================================

    private void importPrivateInvite(JoinTask task) {
        TLRPC.TL_messages_importChatInvite req = new TLRPC.TL_messages_importChatInvite();
        req.hash = task.identifier;

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            if (error != null) {
                handleJoinError(task, error);
            } else {
                handleJoinSuccess(task, "Private Group (" + task.identifier + ")");
            }
        });
    }

    private void handleJoinSuccess(JoinTask task, String title) {
        currentProcessed++;
        successCount++;
        hourlyJoinTimestamps.add(System.currentTimeMillis());

        if (activeCallback != null) {
            mainHandler.post(() -> activeCallback.onProgress(currentProcessed, totalTaskCount, task.originalLink, true, title, null));
        }

        // Apply anti-flood delay (30-60s randomized)
        int delaySeconds = 30 + random.nextInt(31);
        Utilities.globalQueue.postRunnable(this::processNextTask, delaySeconds * 1000L);
    }

    private void handleJoinError(JoinTask task, Object errorObj) {
        currentProcessed++;
        failedCount++;
        String errorText = "Unknown error";

        if (errorObj instanceof TLRPC.TL_error) {
            TLRPC.TL_error err = (TLRPC.TL_error) errorObj;
            errorText = err.text;
            if (err.text != null && err.text.startsWith("FLOOD_WAIT_")) {
                try {
                    int seconds = Integer.parseInt(err.text.replace("FLOOD_WAIT_", ""));
                    floodWaitUntil = System.currentTimeMillis() + (seconds * 1000L);
                } catch (Exception ignored) {}
            }
        } else if (errorObj instanceof String) {
            errorText = (String) errorObj;
        }

        final String finalError = errorText;
        if (activeCallback != null) {
            mainHandler.post(() -> activeCallback.onProgress(currentProcessed, totalTaskCount, task.originalLink, false, null, finalError));
        }

        // Continue next after small pause
        Utilities.globalQueue.postRunnable(this::processNextTask, 5000);
    }

    // =========================================================================
    // 5. Live Radar Auto-Join
    // =========================================================================

    public void setRadarActive(boolean active) {
        this.isRadarActive = active;
    }

    public boolean isRadarActive() {
        return isRadarActive;
    }

    public void onIncomingChatMessage(String messageText) {
        if (!isRadarActive || messageText == null) return;
        List<String> extracted = extractLinks(messageText);
        if (!extracted.isEmpty()) {
            for (String link : extracted) {
                JoinTask t = parseLinkToTask(link);
                if (t != null) {
                    joinQueue.offer(t);
                }
            }
            if (!isRunning) {
                isRunning = true;
                Utilities.globalQueue.postRunnable(this::processNextTask);
            }
        }
    }
}
