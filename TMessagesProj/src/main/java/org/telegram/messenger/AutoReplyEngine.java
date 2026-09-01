/*
 * This is the source code of Telegram for Android v. 12.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2024.
 */

package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.tgnet.TLRPC;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * AutoReplyEngine - Real-Time Smart Background Auto-Responder Engine
 * 
 * Features:
 * 1. Rule-based automatic responses (Contains, Exact Match, Regular Expression).
 * 2. Scope filtering (Private Chats Only, Groups Only, All Conversations).
 * 3. Anti-Loop & Cooldown Mechanism: 2-minute per-user/chat cooldown to prevent reply wars.
 * 4. Direct MTProto reply dispatch via SendMessagesHelper.
 */
public class AutoReplyEngine extends BaseController {

    private static volatile AutoReplyEngine[] Instance = new AutoReplyEngine[UserConfig.MAX_ACCOUNT_COUNT];

    public static AutoReplyEngine getInstance(int num) {
        AutoReplyEngine localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (AutoReplyEngine.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new AutoReplyEngine(num);
                }
            }
        }
        return localInstance;
    }

    public enum MatchType {
        CONTAINS,
        EXACT,
        REGEX
    }

    public enum Scope {
        ALL,
        PRIVATE_ONLY,
        GROUPS_ONLY
    }

    public static class AutoReplyRule {
        public String id;
        public String keyword;
        public String replyText;
        public MatchType matchType = MatchType.CONTAINS;
        public Scope scope = Scope.ALL;
        public boolean isEnabled = true;
        public int timesTriggered = 0;

        public AutoReplyRule(String id, String keyword, String replyText, MatchType matchType, Scope scope) {
            this.id = id;
            this.keyword = keyword;
            this.replyText = replyText;
            this.matchType = matchType;
            this.scope = scope;
            this.isEnabled = true;
        }
    }

    private final List<AutoReplyRule> rules = Collections.synchronizedList(new ArrayList<>());
    private final ConcurrentHashMap<Long, Long> lastReplyTimestamps = new ConcurrentHashMap<>();
    private boolean isGlobalEnabled = true;

    // Cooldown: Minimum 2 minutes (120,000 ms) before replying to the same peer again
    private static final long PEER_COOLDOWN_MS = 120_000L;

    public AutoReplyEngine(int num) {
        super(num);
        loadDefaultRules();
    }

    private void loadDefaultRules() {
        rules.add(new AutoReplyRule("rule_1", "السلام عليكم", "وعليكم السلام ورحمة الله وبركاته! أهلاً بك، كيف يمكنني مساعدتك؟", MatchType.CONTAINS, Scope.ALL));
        rules.add(new AutoReplyRule("rule_2", "الاسعار", "أهلاً بك! يمكنك الاطلاع على باقاتنا وعروضنا الحصرية عبر التواصل المباشر.", MatchType.CONTAINS, Scope.PRIVATE_ONLY));
        rules.add(new AutoReplyRule("rule_3", "مرحبا", "أهلاً وسهلاً بك في مجتمعنا! يسعدنا تواصلك دائماً.", MatchType.EXACT, Scope.ALL));
    }

    // =========================================================================
    // 1. Rules Management
    // =========================================================================

    public void setGlobalEnabled(boolean enabled) {
        this.isGlobalEnabled = enabled;
    }

    public boolean isGlobalEnabled() {
        return isGlobalEnabled;
    }

    public List<AutoReplyRule> getRules() {
        return new ArrayList<>(rules);
    }

    public void addRule(AutoReplyRule rule) {
        if (rule != null) {
            rules.add(rule);
        }
    }

    public void removeRule(String ruleId) {
        rules.removeIf(r -> r.id.equals(ruleId));
    }

    public void toggleRule(String ruleId, boolean enabled) {
        for (AutoReplyRule r : rules) {
            if (r.id.equals(ruleId)) {
                r.isEnabled = enabled;
                break;
            }
        }
    }

    // =========================================================================
    // 2. Incoming Message Processing (MessagesController hook)
    // =========================================================================

    public void onIncomingMessage(TLRPC.Message message) {
        if (!isGlobalEnabled || message == null || message.out || message.message == null) {
            return;
        }

        final String text = message.message.trim();
        if (text.isEmpty()) return;

        final long peerId = message.peer_id;
        final boolean isPrivate = peerId > 0;

        // Anti-Loop / Cooldown check: 2 minutes cooldown per peer
        Long lastReply = lastReplyTimestamps.get(peerId);
        long now = System.currentTimeMillis();
        if (lastReply != null && (now - lastReply) < PEER_COOLDOWN_MS) {
            return;
        }

        // Match against active rules
        for (AutoReplyRule rule : rules) {
            if (!rule.isEnabled) continue;

            // Scope filter
            if (rule.scope == Scope.PRIVATE_ONLY && !isPrivate) continue;
            if (rule.scope == Scope.GROUPS_ONLY && isPrivate) continue;

            boolean isMatched = false;

            if (rule.matchType == MatchType.EXACT) {
                isMatched = text.equalsIgnoreCase(rule.keyword.trim());
            } else if (rule.matchType == MatchType.CONTAINS) {
                isMatched = text.toLowerCase().contains(rule.keyword.trim().toLowerCase());
            } else if (rule.matchType == MatchType.REGEX) {
                try {
                    Pattern pattern = Pattern.compile(rule.keyword.trim(), Pattern.CASE_INSENSITIVE);
                    isMatched = pattern.matcher(text).find();
                } catch (Exception ignored) {}
            }

            if (isMatched) {
                rule.timesTriggered++;
                lastReplyTimestamps.put(peerId, now);

                // Dispatch auto-reply with reply header to the incoming message
                AndroidUtilities.runOnUIThread(() -> {
                    SendMessagesHelper.getInstance(currentAccount).sendMessage(
                        rule.replyText,
                        peerId,
                        message.id
                    );
                });
                break; // Trigger only the first matching rule
            }
        }
    }
}
