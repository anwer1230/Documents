package org.telegram.ui.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * DialogCell - Official Telegram Chat list item view
 * Renders avatar circle, title, date, snippet text, and unread badge.
 */
public class DialogCell extends FrameLayout {

    private final TextView nameTextView;
    private final TextView messageTextView;
    private final TextView timeTextView;
    private final TextView countTextView;
    private final Paint avatarPaint;
    private final Paint avatarTextPaint;
    private final Paint dividerPaint;

    private String avatarLetter = "";
    private int avatarColor = Color.parseColor("#50A7EA");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public DialogCell(Context context) {
        super(context);

        avatarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        avatarTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        avatarTextPaint.setColor(Color.WHITE);
        avatarTextPaint.setTextSize(AndroidUtilities.dp(18));
        avatarTextPaint.setTextAlign(Paint.Align.CENTER);

        dividerPaint = new Paint();
        dividerPaint.setColor(Color.parseColor("#E0E0E0"));
        dividerPaint.setStrokeWidth(AndroidUtilities.dp(1));

        setWillNotDraw(false);

        // Name
        nameTextView = new TextView(context);
        nameTextView.setTextColor(Color.parseColor("#222222"));
        nameTextView.setTextSize(16);
        nameTextView.setSingleLine(true);
        nameTextView.setEllipsize(TextUtils.TruncateAt.END);
        LayoutParams lpName = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lpName.leftMargin = AndroidUtilities.dp(72);
        lpName.topMargin = AndroidUtilities.dp(12);
        lpName.rightMargin = AndroidUtilities.dp(60);
        addView(nameTextView, lpName);

        // Message Snippet
        messageTextView = new TextView(context);
        messageTextView.setTextColor(Color.parseColor("#8E8E93"));
        messageTextView.setTextSize(14);
        messageTextView.setSingleLine(true);
        messageTextView.setEllipsize(TextUtils.TruncateAt.END);
        LayoutParams lpMsg = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lpMsg.leftMargin = AndroidUtilities.dp(72);
        lpMsg.topMargin = AndroidUtilities.dp(36);
        lpMsg.rightMargin = AndroidUtilities.dp(44);
        addView(messageTextView, lpMsg);

        // Time
        timeTextView = new TextView(context);
        timeTextView.setTextColor(Color.parseColor("#A0A0A5"));
        timeTextView.setTextSize(12);
        LayoutParams lpTime = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lpTime.gravity = Gravity.TOP | Gravity.RIGHT;
        lpTime.topMargin = AndroidUtilities.dp(12);
        lpTime.rightMargin = AndroidUtilities.dp(16);
        addView(timeTextView, lpTime);

        // Unread Badge
        countTextView = new TextView(context);
        countTextView.setTextColor(Color.WHITE);
        countTextView.setTextSize(11);
        countTextView.setGravity(Gravity.CENTER);
        countTextView.setBackgroundColor(Color.parseColor("#4DA6EA"));
        LayoutParams lpCount = new LayoutParams(AndroidUtilities.dp(20), AndroidUtilities.dp(20));
        lpCount.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        lpCount.bottomMargin = AndroidUtilities.dp(12);
        lpCount.rightMargin = AndroidUtilities.dp(16);
        addView(countTextView, lpCount);
    }

    public void setDialog(TLRPC.Dialog dialog, int currentAccount) {
        if (dialog == null) return;
        MessagesController mc = MessagesController.getInstance(currentAccount);

        String title = "Chat";
        if (dialog.id > 0) {
            TLRPC.User user = mc.getUser(dialog.id);
            if (user != null) {
                title = (user.first_name != null ? user.first_name : "") + " " + (user.last_name != null ? user.last_name : "");
                title = title.trim().isEmpty() ? "User" : title;
            }
        } else {
            TLRPC.Chat chat = mc.getChat(-dialog.id);
            if (chat != null && chat.title != null) {
                title = chat.title;
            }
        }

        nameTextView.setText(title);
        avatarLetter = title.length() > 0 ? title.substring(0, 1).toUpperCase() : "T";
        avatarColor = getAvatarColorForId(dialog.id);

        TLRPC.Message lastMessage = mc.dialogMessage.get(dialog.id);
        if (lastMessage != null && lastMessage.message != null) {
            messageTextView.setText(lastMessage.message);
            timeTextView.setText(dateFormat.format(new Date((long) lastMessage.date * 1000)));
        } else {
            messageTextView.setText("لا توجد رسائل سابقة");
            timeTextView.setText("");
        }

        if (dialog.unread_count > 0) {
            countTextView.setVisibility(VISIBLE);
            countTextView.setText(String.valueOf(dialog.unread_count));
        } else {
            countTextView.setVisibility(GONE);
        }

        invalidate();
    }

    private int getAvatarColorForId(long id) {
        int[] colors = {
            Color.parseColor("#E17076"),
            Color.parseColor("#FAA774"),
            Color.parseColor("#A695E7"),
            Color.parseColor("#7BC862"),
            Color.parseColor("#6EC9CB"),
            Color.parseColor("#65AADD"),
            Color.parseColor("#EE7AAE")
        };
        return colors[(int) (Math.abs(id) % colors.length)];
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(72), MeasureSpec.EXACTLY));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw Avatar Circle
        float cx = AndroidUtilities.dp(36);
        float cy = AndroidUtilities.dp(36);
        float radius = AndroidUtilities.dp(24);
        avatarPaint.setColor(avatarColor);
        canvas.drawCircle(cx, cy, radius, avatarPaint);

        // Draw Avatar Letter
        Paint.FontMetrics fontMetrics = avatarTextPaint.getFontMetrics();
        float baseline = cy - (fontMetrics.ascent + fontMetrics.descent) / 2;
        canvas.drawText(avatarLetter, cx, baseline, avatarTextPaint);

        // Draw Bottom Divider
        canvas.drawLine(AndroidUtilities.dp(72), getHeight() - 1, getWidth(), getHeight() - 1, dividerPaint);
    }
}
