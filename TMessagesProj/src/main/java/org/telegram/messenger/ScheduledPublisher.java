package org.telegram.messenger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

/**
 * ScheduledPublisher.java (TMessagesProj/src/main/java/org/telegram/messenger/ScheduledPublisher.java)
 * 
 * Isolated smart dispatcher and publisher for Telegram channels and groups.
 * 
 * Features:
 * 1. Reads, parses and extracts links from text inputs (public @usernames, t.me/username, and t.me/+hash).
 * 2. Resolves links into accurate Chat IDs / Channel IDs using official MTProto RPC requests:
 *    - TLRPC.TL_contacts_resolveUsername for public links
 *    - TLRPC.TL_messages_checkChatInvite / importChatInvite for private invite links
 * 3. Sends messages sequentially and safely using SendMessagesHelper.
 * 4. Generates an automated, detailed delivery report dispatched straight to "Saved Messages" (الرسائل المحفوظة).
 */
public class ScheduledPublisher {

    private static volatile ScheduledPublisher[] Instance = new ScheduledPublisher[UserConfig.MAX_ACCOUNT_COUNT];
    private final int currentAccount;

    // Regex for public usernames (@name or t.me/name)
    private static final Pattern PUBLIC_LINK_PATTERN = Pattern.compile(
            "(?:https?://)?(?:t(?:elegram)?\\.me/|tg://resolve\\?domain=)?@?([a-zA-Z0-9_]{3,32})",
            Pattern.CASE_INSENSITIVE
    );

    // Regex for private invite links (t.me/+hash, t.me/joinchat/hash, tg://join?invite=hash)
    private static final Pattern PRIVATE_INVITE_PATTERN = Pattern.compile(
            "(?:https?://)?(?:t(?:elegram)?\\.me/(?:\\+|joinchat/)|tg://join\\?invite=)([a-zA-Z0-9_-]+)",
            Pattern.CASE_INSENSITIVE
    );

    public interface ResolveCallback {
        void onSuccess(long dialogId, String displayName);
        void onError(String errorCode, String errorMessage);
    }

    public interface PublishCallback {
        void onProgress(int current, int total, String target);
        void onComplete(int successCount, int failedCount, List<String> failureReasons);
    }

    public static class PublishResult {
        public String target;
        public boolean success;
        public String errorReason;

        public PublishResult(String target, boolean success, String errorReason) {
            this.target = target;
            this.success = success;
            this.errorReason = errorReason;
        }
    }

    public static ScheduledPublisher getInstance(int account) {
        ScheduledPublisher localInstance = Instance[account];
        if (localInstance == null) {
            synchronized (ScheduledPublisher.class) {
                localInstance = Instance[account];
                if (localInstance == null) {
                    Instance[account] = localInstance = new ScheduledPublisher(account);
                }
            }
        }
        return localInstance;
    }

    public ScheduledPublisher(int account) {
        this.currentAccount = account;
    }

    /**
     * Extracts all unique links / usernames from an input text block (separated by spaces, commas, or newlines).
     */
    public List<String> extractLinksFromText(String rawInput) {
        List<String> links = new ArrayList<>();
        if (rawInput == null || rawInput.trim().isEmpty()) {
            return links;
        }

        String[] tokens = rawInput.split("[\\s,\n\r]+");
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) continue;

            // Check if private invite
            Matcher privateMatcher = PRIVATE_INVITE_PATTERN.matcher(trimmed);
            if (privateMatcher.find()) {
                if (!links.contains(trimmed)) {
                    links.add(trimmed);
                }
                continue;
            }

            // Check if public username / link
            if (trimmed.startsWith("@") || trimmed.contains("t.me/") || trimmed.matches("^[a-zA-Z0-9_]{4,32}$")) {
                if (!links.contains(trimmed)) {
                    links.add(trimmed);
                }
            }
        }
        return links;
    }

    /**
     * Resolves a raw target string (e.g. "@channel", "https://t.me/channel", "https://t.me/+hash")
     * into a Telegram dialog ID (negative for groups/channels).
     */
    public void resolveChatId(String input, ResolveCallback callback) {
        if (input == null || input.trim().isEmpty()) {
            if (callback != null) callback.onError("INVALID_INPUT", "الرابط المدخل فارغ أو غير صالح");
            return;
        }

        String target = input.trim();

        // 1. Check for private invite links (t.me/+hash or t.me/joinchat/hash)
        Matcher privateMatcher = PRIVATE_INVITE_PATTERN.matcher(target);
        if (privateMatcher.find()) {
            String hash = privateMatcher.group(1);
            resolvePrivateInviteHash(hash, target, callback);
            return;
        }

        // 2. Extract public username
        String cleanUsername = extractUsername(target);
        if (cleanUsername != null && !cleanUsername.isEmpty()) {
            resolvePublicUsername(cleanUsername, target, callback);
            return;
        }

        if (callback != null) {
            callback.onError("UNRECOGNIZED_FORMAT", "صيغة الرابط غير معروفة");
        }
    }

    /**
     * Resolves public username using TLRPC.TL_contacts_resolveUsername
     */
    private void resolvePublicUsername(String username, String originalTarget, ResolveCallback callback) {
        TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
        req.username = username;

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            AndroidUtilities.runOnUIThread(() -> {
                if (error == null && response instanceof TLRPC.TL_contacts_resolvedPeer) {
                    TLRPC.TL_contacts_resolvedPeer res = (TLRPC.TL_contacts_resolvedPeer) response;

                    // Cache users and chats in MessagesController
                    MessagesController.getInstance(currentAccount).putUsers(res.users, false);
                    MessagesController.getInstance(currentAccount).putChats(res.chats, false);
                    MessagesStorage.getInstance(currentAccount).putUsersAndChats(res.users, res.chats, false, true);

                    long dialogId = 0;
                    String title = "@" + username;

                    if (res.peer instanceof TLRPC.TL_peerChannel) {
                        dialogId = -res.peer.channel_id;
                        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(res.peer.channel_id);
                        if (chat != null && chat.title != null) {
                            title = chat.title;
                        }
                    } else if (res.peer instanceof TLRPC.TL_peerChat) {
                        dialogId = -res.peer.chat_id;
                        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(res.peer.chat_id);
                        if (chat != null && chat.title != null) {
                            title = chat.title;
                        }
                    } else if (res.peer instanceof TLRPC.TL_peerUser) {
                        dialogId = res.peer.user_id;
                        TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(res.peer.user_id);
                        if (user != null) {
                            title = UserObject.getUserName(user);
                        }
                    }

                    if (dialogId != 0) {
                        if (callback != null) callback.onSuccess(dialogId, title);
                    } else {
                        if (callback != null) callback.onError("PEER_NOT_FOUND", "لم يتم العثور على محادثة للرابط: " + originalTarget);
                    }
                } else {
                    String errorText = error != null ? error.text : "UNKNOWN_ERROR";
                    if (callback != null) callback.onError(errorText, parseErrorDescription(errorText));
                }
            });
        });
    }

    /**
     * Resolves private invite hash using TLRPC.TL_messages_checkChatInvite
     */
    private void resolvePrivateInviteHash(String hash, String originalTarget, ResolveCallback callback) {
        TLRPC.TL_messages_checkChatInvite req = new TLRPC.TL_messages_checkChatInvite();
        req.hash = hash;

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            AndroidUtilities.runOnUIThread(() -> {
                if (error == null) {
                    if (response instanceof TLRPC.TL_chatInviteAlready) {
                        TLRPC.TL_chatInviteAlready already = (TLRPC.TL_chatInviteAlready) response;
                        if (already.chat != null) {
                            long dialogId = -already.chat.id;
                            MessagesController.getInstance(currentAccount).putChat(already.chat, false);
                            if (callback != null) callback.onSuccess(dialogId, already.chat.title != null ? already.chat.title : originalTarget);
                            return;
                        }
                    } else if (response instanceof TLRPC.TL_chatInvite) {
                        TLRPC.TL_chatInvite invite = (TLRPC.TL_chatInvite) response;
                        if (invite.chat != null) {
                            long dialogId = -invite.chat.id;
                            MessagesController.getInstance(currentAccount).putChat(invite.chat, false);
                            if (callback != null) callback.onSuccess(dialogId, invite.chat.title != null ? invite.chat.title : originalTarget);
                            return;
                        }

                        // If not joined yet, import invite
                        importInviteAndResolve(hash, originalTarget, callback);
                        return;
                    }
                    if (callback != null) callback.onError("CANNOT_RESOLVE_INVITE", "تعذر تحويل رابط الدعوة إلى معرف");
                } else {
                    String errorText = error.text != null ? error.text : "INVITE_ERROR";
                    if (callback != null) callback.onError(errorText, parseErrorDescription(errorText));
                }
            });
        });
    }

    /**
     * Imports chat invite if not already present
     */
    private void importInviteAndResolve(String hash, String originalTarget, ResolveCallback callback) {
        TLRPC.TL_messages_importChatInvite req = new TLRPC.TL_messages_importChatInvite();
        req.hash = hash;

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            AndroidUtilities.runOnUIThread(() -> {
                if (error == null && response instanceof TLRPC.Updates) {
                    TLRPC.Updates updates = (TLRPC.Updates) response;
                    if (updates.chats != null && !updates.chats.isEmpty()) {
                        TLRPC.Chat chat = updates.chats.get(0);
                        long dialogId = -chat.id;
                        MessagesController.getInstance(currentAccount).putChat(chat, false);
                        if (callback != null) callback.onSuccess(dialogId, chat.title != null ? chat.title : originalTarget);
                        return;
                    }
                }
                String err = error != null ? error.text : "IMPORT_FAILED";
                if (callback != null) callback.onError(err, parseErrorDescription(err));
            });
        });
    }

    /**
     * Helper to clean username from different URL / prefix formats
     */
    private String extractUsername(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.startsWith("@")) {
            return s.substring(1);
        }
        Matcher matcher = PUBLIC_LINK_PATTERN.matcher(s);
        if (matcher.find()) {
            String u = matcher.group(1);
            if (u != null && !u.equalsIgnoreCase("joinchat") && !u.startsWith("+")) {
                return u;
            }
        }
        if (s.matches("^[a-zA-Z0-9_]{3,32}$")) {
            return s;
        }
        return null;
    }

    /**
     * Main dispatching function: parses input links, resolves each one to Chat ID, sends message,
     * and dispatches a comprehensive delivery summary to Saved Messages.
     */
    public void startSmartPublishing(String messageText, String linksInput, PublishCallback callback) {
        if (messageText == null || messageText.trim().isEmpty()) {
            if (callback != null) callback.onComplete(0, 0, Collections.singletonList("نص الرسالة فارغ"));
            return;
        }

        List<String> targetLinks = extractLinksFromText(linksInput);
        if (targetLinks.isEmpty()) {
            if (callback != null) callback.onComplete(0, 0, Collections.singletonList("لم يتم العثور على روابط صالحة للإرسال"));
            return;
        }

        final int totalTargets = targetLinks.size();
        final List<PublishResult> results = Collections.synchronizedList(new ArrayList<>());
        final AtomicInteger completedCount = new AtomicInteger(0);

        FileLog.d("ScheduledPublisher: Starting smart publishing for " + totalTargets + " targets.");

        for (int i = 0; i < totalTargets; i++) {
            final String targetLink = targetLinks.get(i);
            final int targetIndex = i;

            // Small progressive stagger to prevent MTProto flood limits
            AndroidUtilities.runOnUIThread(() -> {
                if (callback != null) {
                    callback.onProgress(targetIndex + 1, totalTargets, targetLink);
                }

                resolveChatId(targetLink, new ResolveCallback() {
                    @Override
                    public void onSuccess(long dialogId, String displayName) {
                        try {
                            // Send message using SendMessagesHelper
                            SendMessagesHelper.getInstance(currentAccount).sendMessage(
                                SendMessagesHelper.SendMessageParams.of(
                                    messageText,
                                    dialogId,
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

                            results.add(new PublishResult(targetLink, true, null));
                            checkAndFinalize(completedCount.incrementAndGet(), totalTargets, messageText, results, callback);
                        } catch (Exception e) {
                            FileLog.e(e);
                            results.add(new PublishResult(targetLink, false, "EXCEPTION: " + e.getMessage()));
                            checkAndFinalize(completedCount.incrementAndGet(), totalTargets, messageText, results, callback);
                        }
                    }

                    @Override
                    public void onError(String errorCode, String errorMessage) {
                        results.add(new PublishResult(targetLink, false, errorCode + " (" + errorMessage + ")"));
                        checkAndFinalize(completedCount.incrementAndGet(), totalTargets, messageText, results, callback);
                    }
                });
            }, i * 350L);
        }
    }

    /**
     * Checks if all targets are processed, builds report and sends notification to Saved Messages.
     */
    private void checkAndFinalize(int current, int total, String originalMessage, List<PublishResult> results, PublishCallback callback) {
        if (current < total) {
            return;
        }

        int successCount = 0;
        int failedCount = 0;
        List<String> failureDetails = new ArrayList<>();

        for (PublishResult res : results) {
            if (res.success) {
                successCount++;
            } else {
                failedCount++;
                failureDetails.add("- " + res.target + ": " + (res.errorReason != null ? res.errorReason : "فشل غير معروف"));
            }
        }

        if (callback != null) {
            callback.onComplete(successCount, failedCount, failureDetails);
        }

        // Send Final Report to Saved Messages
        sendFinalReportToSavedMessages(successCount, failedCount, failureDetails, originalMessage);
    }

    /**
     * Builds and sends the final execution summary to user's "Saved Messages"
     */
    private void sendFinalReportToSavedMessages(int successCount, int failedCount, List<String> failureDetails, String originalMessage) {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                long selfUserId = UserConfig.getInstance(currentAccount).getClientUserId();
                if (selfUserId == 0) {
                    return;
                }

                StringBuilder report = new StringBuilder();
                report.append("📊 تقرير الإرسال المباشر والذكي:\n\n");
                report.append("✅ نجح الإرسال إلى ").append(successCount).append(" مجموعات.\n");

                if (failedCount > 0) {
                    report.append("❌ فشل الإرسال إلى ").append(failedCount).append(" مجموعات:\n");
                    for (String fail : failureDetails) {
                        report.append(fail).append("\n");
                    }
                } else {
                    report.append("🎉 تم الإرسال لكافة المجموعات بنجاح تام دون أي أخطاء.\n");
                }

                report.append("\n📝 نص الرسالة المرسلة:\n");
                if (originalMessage.length() > 150) {
                    report.append(originalMessage.substring(0, 150)).append("...");
                } else {
                    report.append(originalMessage);
                }

                // Send report directly to Saved Messages
                SendMessagesHelper.getInstance(currentAccount).sendMessage(
                    SendMessagesHelper.SendMessageParams.of(
                        report.toString(),
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

                FileLog.d("ScheduledPublisher: Report successfully sent to Saved Messages.");
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    /**
     * Formats raw Telegram RPC error codes into clear Arabic explanations
     */
    private String parseErrorDescription(String errorText) {
        if (errorText == null) return "خطأ غير معروف";
        if (errorText.startsWith("FLOOD_WAIT_")) {
            String seconds = errorText.replace("FLOOD_WAIT_", "");
            return "قيود تيليجرام المؤقتة FLOOD_WAIT (يرجى الانتظار " + seconds + " ثانية)";
        }
        switch (errorText) {
            case "USERNAME_NOT_OCCUPIED":
                return "اسم المستخدم غير مسجل أو غير موجود";
            case "USERNAME_INVALID":
                return "اسم المستخدم غير صالح";
            case "CHANNEL_PRIVATE":
                return "القناة أو المجموعة خاصة وغير متاحة";
            case "INVITE_HASH_EXPIRED":
                return "رابط الدعوة منتهي الصلاحية";
            case "INVITE_HASH_INVALID":
                return "رابط الدعوة غير صالح";
            case "USER_NOT_PARTICIPANT":
                return "لست عضواً في هذه المجموعة";
            case "CHAT_WRITE_FORBIDDEN":
                return "ليس لديك صلاحية النشر في هذه المجموعة";
            case "USER_BANNED_IN_CHANNEL":
                return "تم تقييد حسابك في هذه القناة أو المجموعة";
            default:
                return errorText;
        }
    }
}
