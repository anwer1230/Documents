package org.telegram.messenger;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * AdvancedJoiner
 * Sequential channel/group joiner with a strict rate limiter (max 20 joins per hour).
 */
public class AdvancedJoiner extends BaseController {

    private static volatile AdvancedJoiner[] Instance = new AdvancedJoiner[UserConfig.MAX_ACCOUNT_COUNT];

    public static AdvancedJoiner getInstance(int num) {
        AdvancedJoiner localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (AdvancedJoiner.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new AdvancedJoiner(num);
                }
            }
        }
        return localInstance;
    }

    private final Queue<String> joinQueue = new LinkedList<>();
    private final ArrayList<Long> joinTimestamps = new ArrayList<>();
    private static final int MAX_JOINS_PER_HOUR = 20;
    private static final long ONE_HOUR_MS = 60 * 60 * 1000L;
    private boolean isProcessing = false;

    public AdvancedJoiner(int num) {
        super(num);
    }

    public synchronized void enqueueLink(String linkOrUsername) {
        if (linkOrUsername == null || linkOrUsername.trim().isEmpty()) return;
        joinQueue.add(linkOrUsername.trim());
        processNext();
    }

    public synchronized void processNext() {
        if (isProcessing || joinQueue.isEmpty()) return;

        long now = System.currentTimeMillis();
        // Clean timestamps older than 1 hour
        joinTimestamps.removeIf(ts -> now - ts > ONE_HOUR_MS);

        // Strict rate limit: Max 20 joins per 3600 seconds
        if (joinTimestamps.size() >= MAX_JOINS_PER_HOUR) {
            long oldest = joinTimestamps.get(0);
            long delay = ONE_HOUR_MS - (now - oldest) + 1000;
            AndroidUtilities.runOnUIThread(this::processNext, Math.max(5000, delay));
            return;
        }

        final String target = joinQueue.poll();
        if (target == null) return;

        isProcessing = true;
        joinTimestamps.add(now);

        if (target.contains("/+") || target.contains("/joinchat/")) {
            String hash = target.substring(target.lastIndexOf("/") + 1).replace("+", "");
            TLRPC.TL_messages_importChatInvite req = new TLRPC.TL_messages_importChatInvite();
            req.hash = hash;
            getConnectionsManager().sendRequest(req, (response, error) -> handleJoinResponse(response, error));
        } else {
            String username = target.replace("https://t.me/", "").replace("t.me/", "").replace("@", "");
            TLRPC.TL_contacts_resolveUsername resolveReq = new TLRPC.TL_contacts_resolveUsername();
            resolveReq.username = username;
            getConnectionsManager().sendRequest(resolveReq, (res, err) -> {
                if (err == null && res instanceof TLRPC.TL_contacts_resolvedPeer) {
                    TLRPC.TL_contacts_resolvedPeer resolved = (TLRPC.TL_contacts_resolvedPeer) res;
                    if (!resolved.chats.isEmpty()) {
                        TLRPC.TL_channels_joinChannel joinReq = new TLRPC.TL_channels_joinChannel();
                        joinReq.channel = MessagesController.getInputChannel(resolved.chats.get(0));
                        getConnectionsManager().sendRequest(joinReq, (joinRes, joinErr) -> handleJoinResponse(joinRes, joinErr));
                        return;
                    }
                }
                isProcessing = false;
                processNext();
            });
        }
    }

    private void handleJoinResponse(org.telegram.tgnet.TLObject response, TLRPC.TL_error error) {
        AndroidUtilities.runOnUIThread(() -> {
            isProcessing = false;
            if (error == null && response instanceof TLRPC.Updates) {
                getMessagesController().processUpdates((TLRPC.Updates) response, false);
            }
            // Delay 3 seconds between requests to avoid instant trigger
            AndroidUtilities.runOnUIThread(this::processNext, 3000);
        });
    }
}
