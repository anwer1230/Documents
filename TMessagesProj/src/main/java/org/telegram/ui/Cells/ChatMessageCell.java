/*
 * This is the source code of Telegram for Android v. 12.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2024.
 */

package org.telegram.ui.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeColors;

/**
 * ChatMessageCell - Official DrKLO/Telegram Android metrics and sizing:
 * - Avatar: dp(35) x dp(35), circular, bottom-aligned for incoming group messages
 * - Bubble margins: dp(8) top/bottom, dp(16) start/end margin
 * - Bubble corner radius: dp(16) default (configurable via Theme.getChatBubbleRadius())
 * - Font sizes: Message text dp(16), Time dp(12), Name dp(14), Badge dp(11)
 */
public class ChatMessageCell extends View {

    public static final int ROLE_NONE = 0;
    public static final int ROLE_CREATOR = 1;
    public static final int ROLE_ADMIN = 2;
    public static final int ROLE_RESTRICTED = 3;
    public static final int ROLE_BANNED = 4;

    private MessageObject currentMessageObject;
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint statePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint avatarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint avatarTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint adminTagPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint adminTagBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF bubbleRect = new RectF();
    private final RectF adminTagRect = new RectF();

    private boolean isVisibleState = false;
    private String senderName;
    private String senderRank;
    private int senderRole = ROLE_NONE;
    private boolean showAvatar = true;

    public ChatMessageCell(Context context) {
        super(context);
        init();
    }

    private void init() {
        textPaint.setColor(Theme.getColor(ThemeColors.key_chat_messageTextIn));
        textPaint.setTextSize(AndroidUtilities.dp(Theme.getChatFontSize()));

        namePaint.setColor(Color.parseColor("#4ea4f6"));
        namePaint.setTextSize(AndroidUtilities.dp(14));
        namePaint.setFakeBoldText(true);

        avatarPaint.setAntiAlias(true);
        avatarTextPaint.setAntiAlias(true);
        avatarTextPaint.setColor(Color.WHITE);
        avatarTextPaint.setTextSize(AndroidUtilities.dp(14));
        avatarTextPaint.setTextAlign(Paint.Align.CENTER);

        statePaint.setAntiAlias(true);
        statePaint.setStrokeWidth(AndroidUtilities.dp(1.5f));
        statePaint.setStyle(Paint.Style.STROKE);

        adminTagPaint.setTextSize(AndroidUtilities.dp(11));
        adminTagPaint.setFakeBoldText(true);
        adminTagBgPaint.setStyle(Paint.Style.FILL);
    }

    public void setMessageObject(MessageObject messageObject) {
        this.currentMessageObject = messageObject;
        checkMessageState();
        invalidate();
    }

    public void setSenderInfo(String name, int role, String customRank, boolean showAvatar) {
        this.senderName = name;
        this.senderRole = role;
        this.senderRank = customRank;
        this.showAvatar = showAvatar;
        invalidate();
    }

    public void setParticipantRole(TLRPC.ChannelParticipant participant) {
        if (participant instanceof TLRPC.TL_channelParticipantCreator) {
            this.senderRole = ROLE_CREATOR;
            TLRPC.TL_channelParticipantCreator c = (TLRPC.TL_channelParticipantCreator) participant;
            this.senderRank = c.rank != null && !c.rank.isEmpty() ? c.rank : "مالك";
        } else if (participant instanceof TLRPC.TL_channelParticipantAdmin) {
            this.senderRole = ROLE_ADMIN;
            TLRPC.TL_channelParticipantAdmin a = (TLRPC.TL_channelParticipantAdmin) participant;
            this.senderRank = a.rank != null && !a.rank.isEmpty() ? a.rank : "مشرف";
        } else if (participant instanceof TLRPC.TL_channelParticipantBanned) {
            TLRPC.TL_channelParticipantBanned b = (TLRPC.TL_channelParticipantBanned) participant;
            if (b.banned_rights != null && b.banned_rights.view_messages) {
                this.senderRole = ROLE_BANNED;
                this.senderRank = "محظور";
            } else {
                this.senderRole = ROLE_RESTRICTED;
                this.senderRank = "مقيد";
            }
        } else {
            this.senderRole = ROLE_NONE;
            this.senderRank = null;
        }
        invalidate();
    }

    public MessageObject getMessageObject() {
        return currentMessageObject;
    }

    public void checkMessageState() {
        if (currentMessageObject == null || !currentMessageObject.isOutOwner()) {
            isVisibleState = false;
            return;
        }
        isVisibleState = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (currentMessageObject == null) {
            return;
        }

        boolean isOut = currentMessageObject.isOutOwner();
        int width = getWidth();
        int height = getHeight();

        int avatarSize = AndroidUtilities.dp(35);
        int bubbleRadius = AndroidUtilities.dp(Theme.getChatBubbleRadius());
        int paddingH = AndroidUtilities.dp(12);
        int paddingV = AndroidUtilities.dp(6);

        // 1. Draw avatar on left for incoming group messages (Official Telegram size = dp(35))
        if (!isOut && showAvatar) {
            float avatarX = AndroidUtilities.dp(8) + avatarSize / 2f;
            float avatarY = height - AndroidUtilities.dp(4) - avatarSize / 2f;
            avatarPaint.setColor(Color.parseColor("#50A7EA"));
            canvas.drawCircle(avatarX, avatarY, avatarSize / 2f, avatarPaint);

            String initial = senderName != null && !senderName.isEmpty() ? senderName.substring(0, 1).toUpperCase() : "U";
            Paint.FontMetrics fm = avatarTextPaint.getFontMetrics();
            float baseline = avatarY - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(initial, avatarX, baseline, avatarTextPaint);
        }

        // 2. Draw message bubble background with official margins
        if (isOut) {
            bgPaint.setColor(Theme.getColor(ThemeColors.key_chat_outBubble));
            bubbleRect.set(width * 0.35f, AndroidUtilities.dp(4), width - AndroidUtilities.dp(8), height - AndroidUtilities.dp(4));
        } else {
            bgPaint.setColor(Theme.getColor(ThemeColors.key_chat_inBubble));
            float left = showAvatar ? AndroidUtilities.dp(8 + 35 + 8) : AndroidUtilities.dp(8);
            bubbleRect.set(left, AndroidUtilities.dp(4), width * 0.75f, height - AndroidUtilities.dp(4));
        }

        canvas.drawRoundRect(bubbleRect, bubbleRadius, bubbleRadius, bgPaint);

        float textOffsetY = bubbleRect.top + AndroidUtilities.dp(20);

        // 3. Draw Sender Name & Role Badge (For incoming group messages)
        if (!isOut && senderName != null) {
            float nameX = bubbleRect.left + paddingH;
            float nameY = bubbleRect.top + AndroidUtilities.dp(16);
            canvas.drawText(senderName, nameX, nameY, namePaint);
            float nameWidth = namePaint.measureText(senderName);

            // Draw Role / Rank Badge if user is Admin, Creator or Restricted
            if (senderRole != ROLE_NONE && senderRank != null) {
                String badgeText = (senderRole == ROLE_CREATOR ? "👑 " : (senderRole == ROLE_ADMIN ? "🛡️ " : "")) + senderRank;
                float tagWidth = adminTagPaint.measureText(badgeText) + AndroidUtilities.dp(8);
                float tagLeft = nameX + nameWidth + AndroidUtilities.dp(6);
                float tagTop = nameY - AndroidUtilities.dp(12);
                float tagBottom = nameY + AndroidUtilities.dp(4);
                adminTagRect.set(tagLeft, tagTop, tagLeft + tagWidth, tagBottom);

                if (senderRole == ROLE_CREATOR) {
                    adminTagBgPaint.setColor(Color.parseColor("#33FFB300"));
                    adminTagPaint.setColor(Color.parseColor("#FFD54F"));
                } else if (senderRole == ROLE_ADMIN) {
                    adminTagBgPaint.setColor(Color.parseColor("#3329B6F6"));
                    adminTagPaint.setColor(Color.parseColor("#4FC3F7"));
                } else if (senderRole == ROLE_RESTRICTED) {
                    adminTagBgPaint.setColor(Color.parseColor("#33FF9800"));
                    adminTagPaint.setColor(Color.parseColor("#FFB74D"));
                } else if (senderRole == ROLE_BANNED) {
                    adminTagBgPaint.setColor(Color.parseColor("#33F44336"));
                    adminTagPaint.setColor(Color.parseColor("#E57373"));
                }
                canvas.drawRoundRect(adminTagRect, AndroidUtilities.dp(4), AndroidUtilities.dp(4), adminTagBgPaint);
                canvas.drawText(badgeText, tagLeft + AndroidUtilities.dp(4), nameY - AndroidUtilities.dp(2), adminTagPaint);
            }
            textOffsetY += AndroidUtilities.dp(18);
        }

        // 4. Draw message text
        String text = currentMessageObject.messageText != null ? currentMessageObject.messageText.toString() : "";
        textPaint.setColor(isOut ? Theme.getColor(ThemeColors.key_chat_messageTextOut) : Theme.getColor(ThemeColors.key_chat_messageTextIn));
        canvas.drawText(text, bubbleRect.left + paddingH, textOffsetY, textPaint);

        // 5. Draw Message Status Icon (🕒, ✓, ✓✓)
        if (isVisibleState && isOut) {
            float stateX = bubbleRect.right - AndroidUtilities.dp(24);
            float stateY = bubbleRect.bottom - AndroidUtilities.dp(12);

            if (currentMessageObject.isSending()) {
                statePaint.setColor(Color.parseColor("#80FFFFFF"));
                canvas.drawCircle(stateX, stateY, AndroidUtilities.dp(5), statePaint);
            } else if (currentMessageObject.isSent() || currentMessageObject.isOutOwner()) {
                statePaint.setColor(Color.parseColor("#4ea4f6"));
                if (currentMessageObject.isUnread()) {
                    canvas.drawLine(stateX - AndroidUtilities.dp(4), stateY, stateX, stateY + AndroidUtilities.dp(4), statePaint);
                    canvas.drawLine(stateX, stateY + AndroidUtilities.dp(4), stateX + AndroidUtilities.dp(6), stateY - AndroidUtilities.dp(4), statePaint);
                } else {
                    canvas.drawLine(stateX - AndroidUtilities.dp(7), stateY, stateX - AndroidUtilities.dp(3), stateY + AndroidUtilities.dp(4), statePaint);
                    canvas.drawLine(stateX - AndroidUtilities.dp(3), stateY + AndroidUtilities.dp(4), stateX + AndroidUtilities.dp(3), stateY - AndroidUtilities.dp(4), statePaint);

                    canvas.drawLine(stateX - AndroidUtilities.dp(2), stateY, stateX + AndroidUtilities.dp(2), stateY + AndroidUtilities.dp(4), statePaint);
                    canvas.drawLine(stateX + AndroidUtilities.dp(2), stateY + AndroidUtilities.dp(4), stateX + AndroidUtilities.dp(8), stateY - AndroidUtilities.dp(4), statePaint);
                }
            }
        }
    }
}
