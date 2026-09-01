package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * ScheduledPublisher
 * Manages queued message publishing across multiple dialogs/channels with a strict rate limiter (max 20 msgs / min).
 */
public class ScheduledPublisher extends BaseController {

    private static volatile ScheduledPublisher[] Instance = new ScheduledPublisher[UserConfig.MAX_ACCOUNT_COUNT];

    public static ScheduledPublisher getInstance(int num) {
        ScheduledPublisher localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (ScheduledPublisher.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new ScheduledPublisher(num);
                }
            }
        }
        return localInstance;
    }

    public static class ScheduledTask {
        public long dialogId;
        public String message;
        public long scheduleDate;
        public boolean isProtected;
        public int retryCount;

        public ScheduledTask(long dialogId, String message, long scheduleDate, boolean isProtected) {
            this.dialogId = dialogId;
            this.message = message;
            this.scheduleDate = scheduleDate;
            this.isProtected = isProtected;
            this.retryCount = 0;
        }
    }

    private final Queue<ScheduledTask> taskQueue = new LinkedList<>();
    private final ArrayList<Long> dispatchedTimestamps = new ArrayList<>();
    private static final int MAX_MESSAGES_PER_MINUTE = 20;
    private static final long ONE_MINUTE_MS = 60 * 1000L;
    private boolean isRunning = false;

    public ScheduledPublisher(int num) {
        super(num);
    }

    public synchronized void enqueueTask(long dialogId, String message, long scheduleDate, boolean isProtected) {
        taskQueue.add(new ScheduledTask(dialogId, message, scheduleDate, isProtected));
        checkAndDispatch();
    }

    public synchronized void checkAndDispatch() {
        if (isRunning || taskQueue.isEmpty()) return;

        long now = System.currentTimeMillis();
        // Clean timestamps older than 1 minute
        dispatchedTimestamps.removeIf(ts -> now - ts > ONE_MINUTE_MS);

        // Strict rate limiter: Max 20 messages per 60 seconds
        if (dispatchedTimestamps.size() >= MAX_MESSAGES_PER_MINUTE) {
            long oldest = dispatchedTimestamps.get(0);
            long delay = ONE_MINUTE_MS - (now - oldest) + 500;
            AndroidUtilities.runOnUIThread(this::checkAndDispatch, Math.max(1000, delay));
            return;
        }

        final ScheduledTask task = taskQueue.poll();
        if (task == null) return;

        isRunning = true;
        dispatchedTimestamps.add(now);

        TLRPC.TL_messages_sendMessage req = new TLRPC.TL_messages_sendMessage();
        req.peer = getMessagesController().getInputPeer(task.dialogId);
        req.message = task.isProtected ? "السلام عليكم ورحمة الله وبركاته" : task.message;
        req.random_id = Utilities.random.nextLong();

        getConnectionsManager().sendRequest(req, (response, error) -> {
            AndroidUtilities.runOnUIThread(() -> {
                isRunning = false;
                if (error == null) {
                    if (response instanceof TLRPC.Updates) {
                        getMessagesController().processUpdates((TLRPC.Updates) response, false);
                    }
                    // If Salam Protection mode is enabled, edit message after 30s
                    if (task.isProtected) {
                        scheduleSalamEdit(task);
                    }
                } else {
                    if (error.text != null && error.text.startsWith("FLOOD_WAIT")) {
                        // Re-queue task with flood wait penalty
                        task.retryCount++;
                        taskQueue.add(task);
                    }
                }
                checkAndDispatch();
            });
        });
    }

    private void scheduleSalamEdit(ScheduledTask task) {
        AndroidUtilities.runOnUIThread(() -> {
            TLRPC.TL_messages_editMessage editReq = new TLRPC.TL_messages_editMessage();
            editReq.peer = getMessagesController().getInputPeer(task.dialogId);
            editReq.message = task.message;
            getConnectionsManager().sendRequest(editReq, (res, err) -> {
                if (res instanceof TLRPC.Updates) {
                    getMessagesController().processUpdates((TLRPC.Updates) res, false);
                }
            });
        }, 30000);
    }
}
