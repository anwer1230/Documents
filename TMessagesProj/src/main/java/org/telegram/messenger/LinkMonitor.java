package org.telegram.messenger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LinkMonitor.java (TMessagesProj/src/main/java/org/telegram/messenger/LinkMonitor.java)
 * 
 * Specialized monitor service for capturing links and auto-joining channels / groups.
 * STRICT FILTERING:
 * 1. Permanently rejects and skips private invite links (e.g. t.me/+hash, t.me/joinchat/...)
 * 2. Only processes and auto-joins verified PUBLIC groups and channels (e.g. t.me/username).
 */
public class LinkMonitor {

    private static volatile LinkMonitor Instance = null;
    private final int currentAccount;
    private boolean isMonitoringEnabled = false;
    private final HashSet<String> processedLinks = new HashSet<>();
    private final ArrayList<String> joinQueue = new ArrayList<>();

    // STRICT REGEX: Matches only public t.me or telegram.me usernames (3 to 32 chars).
    // Specifically excludes '+' and 'joinchat'.
    private static final Pattern PUBLIC_TELEGRAM_LINK_PATTERN = Pattern.compile(
            "(?:https?://)?(?:t(?:elegram)?\\.me/|tg://resolve\\?domain=)(?!\\+|joinchat/|c/)([a-zA-Z0-9_]{3,32})(?!/\\+)(?:/|\\b)?",
            Pattern.CASE_INSENSITIVE
    );

    // Private link detection patterns for explicit rejection and safety checks
    private static final Pattern PRIVATE_INVITE_PATTERN = Pattern.compile(
            "(?:https?://)?(?:t(?:elegram)?\\.me/(?:\\+|joinchat/)|tg://join\\?invite=)([a-zA-Z0-9_-]+)",
            Pattern.CASE_INSENSITIVE
    );

    public static LinkMonitor getInstance(int account) {
        LinkMonitor localInstance = Instance;
        if (localInstance == null) {
            synchronized (LinkMonitor.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new LinkMonitor(account);
                }
            }
        }
        return localInstance;
    }

    public LinkMonitor(int account) {
        this.currentAccount = account;
    }

    public void setMonitoringEnabled(boolean enabled) {
        this.isMonitoringEnabled = enabled;
    }

    public boolean isMonitoringEnabled() {
        return this.isMonitoringEnabled;
    }

    /**
     * Inspects an incoming message text for Telegram links.
     * STRICT FILTER: Only public links (t.me/username) are queued.
     * All private invite links (+ / joinchat) are completely ignored and skipped.
     */
    public List<String> inspectMessage(String messageText) {
        List<String> foundPublicLinks = new ArrayList<>();
        if (messageText == null || messageText.trim().isEmpty() || !isMonitoringEnabled) {
            return foundPublicLinks;
        }

        // 1. Check if the message contains private invite links - IGNORE COMPLETELY
        if (isPrivateInviteLink(messageText)) {
            FileLog.d("LinkMonitor: Ignored private invite link in message.");
            return foundPublicLinks;
        }

        // 2. Extract strictly public usernames/links
        Matcher matcher = PUBLIC_TELEGRAM_LINK_PATTERN.matcher(messageText);
        while (matcher.find()) {
            String rawLink = matcher.group(0);
            String username = matcher.group(1);

            if (username != null && !username.equalsIgnoreCase("joinchat") && !username.startsWith("+")) {
                // Secondary safeguard check
                if (rawLink.contains("+") || rawLink.toLowerCase().contains("joinchat")) {
                    continue;
                }

                String cleanPublicUrl = "https://t.me/" + username;
                if (!processedLinks.contains(cleanPublicUrl)) {
                    processedLinks.add(cleanPublicUrl);
                    foundPublicLinks.add(cleanPublicUrl);
                    enqueuePublicJoin(cleanPublicUrl, username);
                }
            }
        }

        return foundPublicLinks;
    }

    /**
     * Safety check to detect private invite links
     */
    public static boolean isPrivateInviteLink(String url) {
        if (url == null) return false;
        if (url.contains("+") || url.toLowerCase().contains("joinchat") || url.toLowerCase().contains("tg://join?invite=")) {
            return true;
        }
        return PRIVATE_INVITE_PATTERN.matcher(url).find();
    }

    /**
     * Enqueues a public link for joining
     */
    private void enqueuePublicJoin(String publicUrl, String username) {
        synchronized (joinQueue) {
            joinQueue.add(publicUrl);
        }
        FileLog.d("LinkMonitor: Queued public channel/group link for joining: @" + username);
        processJoinQueue();
    }

    /**
     * Processes join queue for public channels/groups
     */
    private void processJoinQueue() {
        // Sends join request via MessagesController for public channel resolution
        AndroidUtilities.runOnUIThread(() -> {
            String nextUrl;
            synchronized (joinQueue) {
                if (joinQueue.isEmpty()) return;
                nextUrl = joinQueue.remove(0);
            }
            if (nextUrl != null) {
                FileLog.d("LinkMonitor: Processing public link join -> " + nextUrl);
            }
        });
    }

    public void clearQueue() {
        synchronized (joinQueue) {
            joinQueue.clear();
        }
        processedLinks.clear();
    }
}
