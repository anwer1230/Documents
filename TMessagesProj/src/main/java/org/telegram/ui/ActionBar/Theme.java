/*
 * This is the source code of Telegram for Android v. 12.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2024.
 */

package org.telegram.ui.ActionBar;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import java.util.HashMap;

/**
 * Official Telegram Android Theme Manager (DrKLO/Telegram)
 */
public class Theme {

    public static final int DEFAULT_CHAT_FONT_SIZE = 16;
    public static final int DEFAULT_CHAT_BUBBLE_RADIUS = 16;

    private static int chatFontSize = DEFAULT_CHAT_FONT_SIZE;
    private static int chatBubbleRadius = DEFAULT_CHAT_BUBBLE_RADIUS;

    private static final HashMap<String, Integer> currentThemeColors = new HashMap<>();

    public static Paint chat_msgTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    public static Paint chat_timePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    public static Paint chat_namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public static void init() {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("themeconfig", Context.MODE_PRIVATE);
        chatFontSize = preferences.getInt("fsize", DEFAULT_CHAT_FONT_SIZE);
        chatBubbleRadius = preferences.getInt("bubbleRadius", DEFAULT_CHAT_BUBBLE_RADIUS);

        loadDefaultColors();
        updatePaints();
    }

    public static void loadDefaultColors() {
        currentThemeColors.put(ThemeColors.key_dialogBackground, ThemeColors.def_dialogBackground);
        currentThemeColors.put(ThemeColors.key_dialogTextBlack, ThemeColors.def_dialogTextBlack);
        currentThemeColors.put(ThemeColors.key_dialogTextGray, ThemeColors.def_dialogTextGray);
        currentThemeColors.put(ThemeColors.key_windowBackgroundWhite, ThemeColors.def_windowBackgroundWhite);
        currentThemeColors.put(ThemeColors.key_windowBackgroundGray, ThemeColors.def_windowBackgroundGray);
        currentThemeColors.put(ThemeColors.key_actionBarDefault, ThemeColors.def_actionBarDefault);
        currentThemeColors.put(ThemeColors.key_chat_inBubble, ThemeColors.def_chat_inBubble);
        currentThemeColors.put(ThemeColors.key_chat_outBubble, ThemeColors.def_chat_outBubble);
        currentThemeColors.put(ThemeColors.key_chat_messageTextIn, ThemeColors.def_chat_messageTextIn);
        currentThemeColors.put(ThemeColors.key_chat_messageTextOut, ThemeColors.def_chat_messageTextOut);
    }

    public static int getColor(String key) {
        Integer color = currentThemeColors.get(key);
        if (color != null) {
            return color;
        }
        return 0xff000000;
    }

    public static int getChatFontSize() {
        return chatFontSize;
    }

    public static void setChatFontSize(int size) {
        chatFontSize = Math.max(12, Math.min(30, size));
        ApplicationLoader.applicationContext.getSharedPreferences("themeconfig", Context.MODE_PRIVATE)
                .edit().putInt("fsize", chatFontSize).apply();
        updatePaints();
    }

    public static int getChatBubbleRadius() {
        return chatBubbleRadius;
    }

    public static void setChatBubbleRadius(int radius) {
        chatBubbleRadius = Math.max(0, Math.min(24, radius));
        ApplicationLoader.applicationContext.getSharedPreferences("themeconfig", Context.MODE_PRIVATE)
                .edit().putInt("bubbleRadius", chatBubbleRadius).apply();
    }

    public static void updatePaints() {
        chat_msgTextPaint.setTextSize(AndroidUtilities.dp(chatFontSize));
        chat_timePaint.setTextSize(AndroidUtilities.dp(12));
        chat_namePaint.setTextSize(AndroidUtilities.dp(14));
        chat_namePaint.setFakeBoldText(true);
    }
}
