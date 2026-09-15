/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.find;
import static org.telegram.messenger.AndroidUtilities.replaceTags;
import static org.telegram.messenger.LocaleController.formatPluralSpannable;
import static org.telegram.messenger.LocaleController.formatPluralString;
import static org.telegram.messenger.LocaleController.formatPluralStringComma;
import static org.telegram.messenger.LocaleController.formatSpannable;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Base64;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.collection.LongSparseArray;
import androidx.core.graphics.ColorUtils;

import org.telegram.PhoneFormat.PhoneFormat;
import org.telegram.messenger.browser.Browser;
import org.telegram.messenger.ringtone.RingtoneDataStore;
import org.telegram.messenger.utils.tlutils.AmountUtils;
import org.telegram.messenger.utils.tlutils.TLKeyboardHelper;
import org.telegram.messenger.utils.tlutils.TlUtils;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_keyboard;
import org.telegram.tgnet.tl.TL_iv;
import org.telegram.tgnet.tl.TL_stories;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Business.QuickRepliesController;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.MultiLayoutTypingAnimator;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.AnimatedEmojiSpan;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.ButtonBounce;
import org.telegram.ui.Components.ColoredImageSpan;
import org.telegram.ui.Components.EllipsizeSpanAnimator;
import org.telegram.ui.Components.FormattedDateSpan;
import org.telegram.ui.Components.Forum.ForumBubbleDrawable;
import org.telegram.ui.Components.Forum.ForumUtilities;
import org.telegram.ui.Components.QuoteSpan;
import org.telegram.ui.Components.Reactions.ReactionsLayoutInBubble;
import org.telegram.ui.Components.Reactions.ReactionsUtils;
import org.telegram.ui.Components.SquigglyLinesSpan;
import org.telegram.ui.Components.Text;
import org.telegram.ui.Components.TextStyleSpan;
import org.telegram.ui.Components.TranscribeButton;
import org.telegram.ui.Components.TypefaceSpan;
import org.telegram.ui.Components.URLSpanBotCommand;
import org.telegram.ui.Components.URLSpanBrowser;
import org.telegram.ui.Components.URLSpanMono;
import org.telegram.ui.Components.URLSpanNoUnderline;
import org.telegram.ui.Components.URLSpanNoUnderlineBold;
import org.telegram.ui.Components.URLSpanReplacement;
import org.telegram.ui.Components.URLSpanUserMention;
import org.telegram.ui.Components.VideoPlayer;
import org.telegram.ui.Components.WebPlayerView;
import org.telegram.ui.Components.poll.PollAttachedMediaPack;
import org.telegram.ui.Components.spoilers.SpoilerEffect;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.PeerColorActivity;
import org.telegram.ui.PhotoViewer;
import org.telegram.ui.Stars.StarsController;
import org.telegram.ui.Stars.StarsIntroActivity;
import org.telegram.ui.Stories.StoriesController;
import org.telegram.ui.community.CommunityUtils;
import org.telegram.ui.web.BotWebViewContainer;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.vkryl.core.BitwiseUtils;

public class MessageObject {
    private static final int MESSAGE_ID_RESERVED_BITS_MASK = 0x70000000;
    private static final int MESSAGE_ID_EPHEMERAL_BITS_MASK = 0x60000000;

    public static final int MESSAGE_SEND_STATE_SENT = 0;
    public static final int MESSAGE_SEND_STATE_SENDING = 1;
    public static final int MESSAGE_SEND_STATE_SEND_ERROR = 2;
    public static final int MESSAGE_SEND_STATE_EDITING = 3;

    public static final int TYPE_TEXT = 0;
    public static final int TYPE_PHOTO = 1;
    public static final int TYPE_VOICE = 2;
    public static final int TYPE_VIDEO = 3;
    public static final int TYPE_GEO = 4; // TL_messageMediaGeo, TL_messageMediaVenue, TL_messageMediaGeoLive
    public static final int TYPE_ROUND_VIDEO = 5;
    public static final int TYPE_LOADING = 6;
    public static final int TYPE_GIF = 8;
    public static final int TYPE_FILE = 9;
    public static final int TYPE_DATE = 10;
    public static final int TYPE_ACTION_PHOTO = 11;
    public static final int TYPE_CONTACT = 12;
    public static final int TYPE_STICKER = 13;
    public static final int TYPE_MUSIC = 14;
    public static final int TYPE_ANIMATED_STICKER = 15;
    public static final int TYPE_PHONE_CALL = 16;
    public static final int TYPE_POLL = 17; // polls and todos
    public static final int TYPE_GIFT_PREMIUM = 18;
    public static final int TYPE_EMOJIS = 19;
    public static final int TYPE_EXTENDED_MEDIA_PREVIEW = 20;
    public static final int TYPE_SUGGEST_PHOTO = 21;
    public static final int TYPE_ACTION_WALLPAPER = 22;
    public static final int TYPE_STORY = 23;
    public static final int TYPE_STORY_MENTION = 24;
    public static final int TYPE_GIFT_PREMIUM_CHANNEL = 25;
    public static final int TYPE_GIVEAWAY = 26;
    public static final int TYPE_JOINED_CHANNEL = 27; // recommendations list
    public static final int TYPE_GIVEAWAY_RESULTS = 28;
    public static final int TYPE_PAID_MEDIA = 29; // messageMediaPaidMedia with stars
    public static final int TYPE_GIFT_STARS = 30;
    public static final int TYPE_GIFT_THEME_UPDATE = 31;
    public static final int TYPE_SUGGEST_BIRTHDAY = 32;
    public static final int TYPE_GIFT_OFFER = 33;
    public static final int TYPE_GIFT_OFFER_REJECTED = 34;
    public static final int TYPE_SHARING_OFFER = 35;
    public static final int TYPE_ARTICLE = 36;
    public static final int TYPE_COMMUNITY_CHANGED = 37;
    public static final int TYPE_UNSUPPORTED = 38;

    public int localType;
    public String localName;
    public String localUserName;
    public long localGroupId;
    public long localSentGroupId;
    public boolean localChannel;
    public boolean localSupergroup;
    public Boolean cachedIsSupergroup;
    public boolean localEdit;
    public StoriesController.StoriesList parentStoriesList;
    public TLRPC.Message messageOwner;
    public TL_stories.StoryItem storyItem;
    public StoriesController.UploadingStory uploadingStory;
    public TLRPC.Document emojiAnimatedSticker;
    public Long emojiAnimatedStickerId;
    public boolean isTopicMainMessage;
    public boolean settingAvatar;
    public boolean flickerLoading;
    public TLRPC.VideoSize emojiMarkup;
    private boolean emojiAnimatedStickerLoading;
    public String emojiAnimatedStickerColor;
    public CharSequence messageText;
    public CharSequence messageTextShort;
    public CharSequence messageTextForReply;
    public CharSequence linkDescription;
    public CharSequence caption;
    public CharSequence quizExplanation;
    public CharSequence youtubeDescription;
    public MessageObject replyMessageObject;
    public int type = 1000;
    public long reactionsLastCheckTime;
    public long extendedMediaLastCheckTime;
    public String customName;
    public boolean reactionsChanged;
    public boolean isReactionPush;
    public boolean isStoryReactionPush;
    public boolean isStoryPush, isStoryMentionPush, isStoryPushHidden, isLiveStoryPush;
    public boolean isOauthPush;
    public boolean putInDownloadsStore;
    public boolean isDownloadingFile;
    public boolean forcePlayEffect;
    private int isRoundVideoCached;
    public long eventId;
    public int contentType;
    public int realDate;
    public String dateKey;
    public int dateKeyInt;
    public String monthKey;
    public boolean deleted;
    public boolean deletedByThanos;
    public float audioProgress;
    public float forceSeekTo = -1;
    public int audioProgressMs;
    public float bufferedProgress;
    public float gifState;
    public int audioProgressSec;
    public int audioPlayerDuration;
    public double attributeDuration;
    public boolean isDateObject;
    public boolean isVideoConversionObject;
    public boolean isPlayingExplanationObject;
    public TLObject photoThumbsObject;
    public TLObject photoThumbsObject2;
    public ArrayList<TLRPC.PhotoSize> photoThumbs;
    public ArrayList<TLRPC.PhotoSize> photoThumbs2;
    public VideoEditedInfo videoEditedInfo;
    public ArrayList<Integer> pollMediaMapping;
    public boolean shouldRemoveVideoEditedInfo;
    public boolean viewsReloaded;
    public boolean pollVisibleOnScreen;
    public long pollLastCheckTime;
    public int wantedBotKeyboardWidth;
    public boolean attachPathExists;
    public boolean mediaExists;
    public MediaController.PhotoEntry sendPreviewEntry;
    public boolean sendPreview;
    public boolean resendAsIs;
    public String customReplyName;
    public boolean useCustomPhoto;
    public StringBuilder botButtonsLayout;
    public boolean isRestrictedMessage;
    public long loadedFileSize;
    public boolean forceExpired;
    public long actionDeleteGroupEventId = -1;
    public HashSet<Integer> expandedQuotes;
    public boolean expandedExplanation;
    public boolean forceShowPollResults;

    public boolean isSpoilersRevealed;
    public boolean isMediaSpoilersRevealed;
    public boolean isMediaSpoilersRevealedInSharedMedia;
    public boolean revealingMediaSpoilers;
    public byte[] sponsoredId;
    public String sponsoredTitle, sponsoredUrl;
    public boolean sponsoredRecommended;
    public TLRPC.Photo sponsoredPhoto;
    public String sponsoredInfo, sponsoredAdditionalInfo;
    public String sponsoredButtonText;
    public TLRPC.PeerColor sponsoredColor;
    public TLRPC.MessageMedia sponsoredMedia;
    public boolean sponsoredCanReport;

    public boolean replyTextEllipsized;
    public boolean replyTextRevealed;
    public int overrideLinkColor = -1;
    public long overrideLinkEmoji = -1;
    public TLRPC.TL_peerColorCollectible overrideLinkPeerColor;
    private boolean channelJoined;
    public boolean channelJoinedExpanded;

    public TLRPC.TL_forumTopic replyToForumTopic; // used only for reply message in view all messages

    public boolean animateComments;

    public boolean loadingCancelled;

    public int stableId;

    public boolean wasUnread;
    public boolean playedGiftAnimation;

    public boolean hadAnimationNotReadyLoading;

    public boolean cancelEditing;

    public boolean scheduled;
    public boolean scheduledSent;
    public boolean preview;
    public boolean previewForward;
    public boolean sentHighQuality;

    public boolean notime;
    
    public int richMessageMediaType;

    public int getChatMode() {
        if (scheduled) {
            return ChatActivity.MODE_SCHEDULED;
        } else if (isWelcomeMessage()) {
            return ChatActivity.MODE_WELCOME_MESSAGES;
        } else if (isQuickReply()) {
            return ChatActivity.MODE_QUICK_REPLIES;
        }
        return 0;
    }

    public ArrayList<TLRPC.PollAnswer> checkedVotes;

    public CharSequence editingMessage;
    public ArrayList<TLRPC.MessageEntity> editingMessageEntities;
    public boolean editingMessageSearchWebPage;
    public ArrayList<TLRPC.MessageEntity> webPageDescriptionEntities;

    public String previousMessage;
    public TLRPC.MessageMedia previousMedia;
    public TLRPC.Document documentToPollAddOption;
    public ArrayList<TLRPC.MessageEntity> previousMessageEntities;
    public String previousAttachPath;

    public SvgHelper.SvgDrawable pathThumb;
    public BitmapDrawable strippedThumb;
    public Bitmap audioCover;

    public int currentAccount;

    public TLRPC.TL_channelAdminLogEvent currentEvent;

    public boolean forceUpdate;

    public SendAnimationData sendAnimationData;

    private boolean hasUnwrappedEmoji;
    public int emojiOnlyCount, animatedEmojiCount;
    public int totalAnimatedEmojiCount;
    private boolean layoutCreated;
    private int generatedWithMinSize;
    private float generatedWithDensity;
    private float generatedWithFontSize;
    public boolean wasJustSent;
    public boolean isBotPendingDraft;

    public static Pattern urlPattern;
    public static Pattern instagramUrlPattern;
    public static Pattern videoTimeUrlPattern;

    public CharSequence vCardData;

    public ArrayList<String> highlightedWords;
    public boolean messageTrimmedToHighlightCut = true;
    public CharSequence messageTrimmedToHighlight;
    public int parentWidth;

    public ImageLocation mediaThumb;
    public ImageLocation mediaSmallThumb;

    public Object lastGeoWebFileSet;
    public Object lastGeoWebFileLoaded;

    // forwarding preview params
    public boolean hideSendersName;
    public TLRPC.Peer sendAsPeer;
    public Drawable[] topicIconDrawable = new Drawable[1];

    static final String[] excludeWords = new String[]{
            " vs. ",
            " vs ",
            " versus ",
            " ft. ",
            " ft ",
            " featuring ",
            " feat. ",
            " feat ",
            " presents ",
            " pres. ",
            " pres ",
            " and ",
            " & ",
            " . "
    };
    public boolean isRepostPreview;
    public boolean isRepostVideoPreview;
    public boolean business;
    public boolean forceAvatar;
    public Drawable customAvatarDrawable;
    public boolean isSaved;
    public boolean isSavedFiltered;
    public String quick_reply_shortcut;
    public int searchType;
    private BotInlineKeyboard.Source inlineKeyboardSource;

    private byte[] randomWaveform;
    public boolean drawServiceWithDefaultTypeface;

    public static boolean hasUnreadReactions(TLRPC.Message message) {
        if (message == null) {
            return false;
        }
        return hasUnreadReactions(message.reactions);
    }

    public static boolean hasUnreadReactions(TLRPC.TL_messageReactions reactions) {
        if (reactions == null) {
            return false;
        }
        for (int i = 0; i < reactions.recent_reactions.size(); i++) {
            if (reactions.recent_reactions.get(i).unread) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPremiumSticker(TLRPC.Document document) {
        if (document == null || document.thumbs == null) {
            return false;
        }
        for (int i = 0; i < document.video_thumbs.size(); i++) {
            if ("f".equals(document.video_thumbs.get(i).type)) {
                return true;
            }
        }
        return false;
    }

    private static long getTopicId(MessageObject message) {
        if (message == null) {
            return 0;
        }
        return getTopicId(message.currentAccount, message.messageOwner, false);
    }

    private static long getTopicId(int currentAccount, TLRPC.Message message) {
        return getTopicId(currentAccount, message, false);
    }

    public long getMonoForumTopicId() {
        return getMonoForumTopicId(messageOwner);
    }

    public static long getMonoForumTopicId(TLRPC.Message message) {
        if (message != null && message.saved_peer_id != null) {
            if (message.saved_peer_id.chat_id != 0) {
                return -message.saved_peer_id.chat_id;
            } else if (message.saved_peer_id.channel_id != 0) {
                return -message.saved_peer_id.channel_id;
            } else {
                return message.saved_peer_id.user_id;
            }
        }
        return 0;
    }

    public long getTopicId() {
        final TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-getDialogId());
        final TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(getDialogId());

        return getTopicId(currentAccount, messageOwner, getForumFlags(chat, user));
    }

    private static int getForumFlags(TLRPC.Chat chat, TLRPC.User user) {
        int flags = 0;
        flags = BitwiseUtils.setFlag(flags, MessagesStorage.FORUM_TYPE_CHAT, ChatObject.isForum(chat));
        flags = BitwiseUtils.setFlag(flags, MessagesStorage.FORUM_TYPE_DIRECT, ChatObject.isMonoForum(chat));
        flags = BitwiseUtils.setFlag(flags, MessagesStorage.FORUM_TYPE_BOT, UserObject.isBotForum(user));
        return flags;
    }

    public static long getTopicId(int currentAccount, TLRPC.Message message, int sureIsForumTypeFlags) {
        final long topicId = getTopicId(currentAccount, message,
            BitwiseUtils.hasFlag(sureIsForumTypeFlags, MessagesStorage.FORUM_TYPE_CHAT),
            BitwiseUtils.hasFlag(sureIsForumTypeFlags, MessagesStorage.FORUM_TYPE_DIRECT)
        );

        if (topicId == 0 && BitwiseUtils.hasFlag(sureIsForumTypeFlags, MessagesStorage.FORUM_TYPE_BOT)) {
            return -1;   // pending topic
        }

        return topicId;
    }

    @Deprecated
    public static long getTopicId(int currentAccount, TLRPC.Message message, boolean sureIsForum) {
        return getTopicId(currentAccount, message, sureIsForum, false);
    }

    @Deprecated
    private static long getTopicId(int currentAccount, TLRPC.Message message, boolean sureIsForum, boolean sureIsMonoForum) {
        final long selfId = UserConfig.getInstance(currentAccount).getClientUserId();
        if (sureIsMonoForum) {
            return getMonoForumTopicId(message);
        }
        if ((message.flags & 1073741824) != 0 && DialogObject.getPeerDialogId(message.peer_id) == selfId) {
            return message.quick_reply_shortcut_id;
        }
        if (!sureIsForum && message != null && currentAccount >= 0 && DialogObject.getPeerDialogId(message.peer_id) == selfId) {
            return getSavedDialogId(selfId, message);
        }
        if (message != null && message.action instanceof TLRPC.TL_messageActionTopicCreate) {
            return message.id;
        }
        if (message == null || message.reply_to == null || !message.reply_to.forum_topic) {
            return sureIsForum ? 1 : 0; // 1 = general topic
        }
        if (message instanceof TLRPC.TL_messageService && !(message.action instanceof TLRPC.TL_messageActionPinMessage)) {
            int topicId = message.reply_to.reply_to_msg_id;
            if (topicId == 0) {
                topicId = message.reply_to.reply_to_top_id;
            }
            return topicId;
        } else {
            int topicId = message.reply_to.reply_to_top_id;
            if (topicId == 0) {
                topicId = message.reply_to.reply_to_msg_id;
            }
            return topicId;
        }
    }

    public static boolean isTopicActionMessage(MessageObject message) {
        if (message == null || message.messageOwner == null) {
            return false;
        }
        return message.messageOwner.action instanceof TLRPC.TL_messageActionTopicCreate ||
                message.messageOwner.action instanceof TLRPC.TL_messageActionTopicEdit;
    }

    public static boolean canCreateStripedThubms() {
        return SharedConfig.getDevicePerformanceClass() == SharedConfig.PERFORMANCE_CLASS_HIGH;
    }

    public static void normalizeFlags(TLRPC.Message message) {
        if (message.from_id == null) {
            message.flags &= ~256;
        }
        if (message.from_id == null) {
            message.flags &= ~4;
        }
        if (message.reply_to == null) {
            message.flags &= ~8;
        }
        if (message.media == null) {
            message.flags &= ~512;
        }
        if (message.reply_markup == null) {
            message.flags &= ~64;
        }
        if (message.replies == null) {
            message.flags &= ~8388608;
        }
        if (message.reactions == null) {
            message.flags &= ~1048576;
        }
    }

    public static double getDocumentDuration(TLRPC.Document document) {
        if (document == null) {
            return 0;
        }
        for (int a = 0, size = document.attributes.size(); a < size; a++) {
            TLRPC.DocumentAttribute attribute = document.attributes.get(a);
            if (attribute instanceof TLRPC.TL_documentAttributeVideo) {
                return attribute.duration;
            } else if (attribute instanceof TLRPC.TL_documentAttributeAudio) {
                return attribute.duration;
            }
        }
        return 0;
    }

    public static int getVideoWidth(TLRPC.Document document) {
        if (document == null) {
            return 0;
        }
        for (int a = 0, size = document.attributes.size(); a < size; a++) {
            TLRPC.DocumentAttribute attribute = document.attributes.get(a);
            if (attribute instanceof TLRPC.TL_documentAttributeVideo) {
                return attribute.w;
            }
        }
        return 0;
    }

    public static int getVideoHeight(TLRPC.Document document) {
        if (document == null) {
            return 0;
        }
        for (int a = 0, size = document.attributes.size(); a < size; a++) {
            TLRPC.DocumentAttribute attribute = document.attributes.get(a);
            if (attribute instanceof TLRPC.TL_documentAttributeVideo) {
                return attribute.h;
            }
        }
        return 0;
    }

    public static String getVideoCodec(TLRPC.Document document) {
        if (document == null) {
            return null;
        }
        for (int a = 0, size = document.attributes.size(); a < size; a++) {
            TLRPC.DocumentAttribute attribute = document.attributes.get(a);
            if (attribute instanceof TLRPC.TL_documentAttributeVideo) {
                return ((TLRPC.TL_documentAttributeVideo) attribute).video_codec;
            }
        }
        return null;
    }

    public boolean isWallpaperAction() {
        return type == TYPE_ACTION_WALLPAPER || (messageOwner != null && messageOwner.action instanceof TLRPC.TL_messageActionSetSameChatWallPaper);
    }

    public boolean isWallpaperForBoth() {
        return isWallpaperAction() && messageOwner != null && messageOwner.action instanceof TLRPC.TL_messageActionSetChatWallPaper && ((TLRPC.TL_messageActionSetChatWallPaper) messageOwner.action).for_both;
    }

    public boolean isCurrentWallpaper() {
        if (!isWallpaperAction() || messageOwner == null || messageOwner.action == null || messageOwner.action.wallpaper == null)
            return false;
        TLRPC.UserFull userFull = MessagesController.getInstance(currentAccount).getUserFull(getDialogId());
        if (userFull == null || userFull.wallpaper == null || !userFull.wallpaper_overridden)
            return false;
        return messageOwner.action.wallpaper.id == userFull.wallpaper.id;
    }

    public int getEmojiOnlyCount() {
        return emojiOnlyCount;
    }

    public boolean hasMediaSpoilers() {
        return !isRepostPreview && (messageOwner.media != null && messageOwner.media.spoiler || needDrawBluredPreview()) || isHiddenSensitive();
    }

    public Boolean isSensitiveCached;
    public boolean isSensitive() {
        if (isSensitiveCached != null) return isSensitiveCached;
        if (messageOwner == null) return false;
        if (!canBeSensitive()) return false;
        if (!messageOwner.restriction_reason.isEmpty()) {
            for (int i = 0; i < messageOwner.restriction_reason.size(); ++i) {
                TLRPC.RestrictionReason reason = messageOwner.restriction_reason.get(i);
                if (
                    "sensitive".equals(reason.reason) &&
                    (
                        "all".equals(reason.platform) ||
                        "android".equals(reason.platform) && (!ApplicationLoader.isStandaloneBuild() && !BuildVars.isBetaApp() || BuildVars.DEBUG_PRIVATE_VERSION) ||
                        "android-all".equals(reason.platform)
                    )
                ) {
                    return isSensitiveCached = true;
                }
            }
        }
        if (getDialogId() < 0) {
            final TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-getDialogId());
            if (chat != null && chat.restriction_reason != null) {
                for (int i = 0; i < chat.restriction_reason.size(); ++i) {
                    TLRPC.RestrictionReason reason = chat.restriction_reason.get(i);
                    if (
                        "sensitive".equals(reason.reason) &&
                        (
                            "all".equals(reason.platform) ||
                            "android".equals(reason.platform) && (!ApplicationLoader.isStandaloneBuild() && !BuildVars.isBetaApp() || BuildVars.DEBUG_PRIVATE_VERSION) ||
                            "android-all".equals(reason.platform)
                        )
                    ) {
                        return isSensitiveCached = true;
                    }
                }
            }
        }
        return isSensitiveCached = false;
    }

    public boolean isHiddenSensitive() {
        return isSensitive() && !MessagesController.getInstance(currentAccount).showSensitiveContent();
    }

    public boolean canBeSensitive() {
        return messageOwner != null && (
            type == TYPE_PHOTO ||
            type == TYPE_VIDEO ||
            type == TYPE_FILE ||
            type == TYPE_GIF ||
            type == TYPE_ROUND_VIDEO
        ) && !sendPreview && !isRepostPreview && !isOutOwner() && messageOwner.send_state == MessageObject.MESSAGE_SEND_STATE_SENT;
    }

    public boolean shouldDrawReactions() {
        if (isRepostPreview) {
            return false;
        }
        return true;
    }

    public boolean shouldDrawReactionsInLayout() {
        return true;
    }

    public TLRPC.MessagePeerReaction getRandomUnreadReaction() {
        if (messageOwner.reactions == null || messageOwner.reactions.recent_reactions == null || messageOwner.reactions.recent_reactions.isEmpty()) {
            return null;
        }
        return messageOwner.reactions.recent_reactions.get(0);
    }

    public void markPollVotesAsRead() {
        if (messageOwner != null && messageOwner.media instanceof TLRPC.TL_messageMediaPoll) {
            ((TLRPC.TL_messageMediaPoll) messageOwner.media).results.has_unread_votes = false;
        }
    }

    public void markReactionsAsRead() {
        if (messageOwner.reactions == null || messageOwner.reactions.recent_reactions == null) {
            return;
        }
        boolean changed = false;
        for (int i = 0; i < messageOwner.reactions.recent_reactions.size(); i++) {
            if (messageOwner.reactions.recent_reactions.get(i).unread) {
                messageOwner.reactions.recent_reactions.get(i).unread = false;
                changed = true;
            }
        }
        if (changed) {
            MessagesStorage.getInstance(currentAccount).markMessageReactionsAsRead(messageOwner.dialog_id, getTopicId(currentAccount, messageOwner), messageOwner.id);
        }
    }

    public boolean isPremiumSticker() {
        if (getMedia(messageOwner) != null && getMedia(messageOwner).nopremium) {
            return false;
        }
        return isPremiumSticker(getDocument());
    }

    public TLRPC.VideoSize getPremiumStickerAnimation() {
        return getPremiumStickerAnimation(getDocument());
    }

    public static TLRPC.VideoSize getPremiumStickerAnimation(TLRPC.Document document) {
        if (document == null || document.thumbs == null) {
            return null;
        }
        for (int i = 0; i < document.video_thumbs.size(); i++) {
            if ("f".equals(document.video_thumbs.get(i).type)) {
                return document.video_thumbs.get(i);
            }
        }
        return null;
    }

    public void copyStableParams(MessageObject old) {
        stableId = old.stableId;
        messageOwner.premiumEffectWasPlayed = old.messageOwner.premiumEffectWasPlayed;
        forcePlayEffect = old.forcePlayEffect;
        wasJustSent = old.wasJustSent;
        if (messageOwner.reactions != null && messageOwner.reactions.results != null && !messageOwner.reactions.results.isEmpty() && old.messageOwner.reactions != null && old.messageOwner.reactions.results != null) {
            for (int i = 0; i < messageOwner.reactions.results.size(); i++) {
                TLRPC.ReactionCount reactionCount = messageOwner.reactions.results.get(i);
                for (int j = 0; j < old.messageOwner.reactions.results.size(); j++) {
                    TLRPC.ReactionCount oldReaction = old.messageOwner.reactions.results.get(j);
                    if (ReactionsLayoutInBubble.equalsTLReaction(reactionCount.reaction, oldReaction.reaction)) {
                        reactionCount.lastDrawnPosition = oldReaction.lastDrawnPosition;
                    }
                }
            }
        }
        isSpoilersRevealed = old.isSpoilersRevealed;
        messageOwner.replyStory = old.messageOwner.replyStory;
        if (messageOwner.media != null && old.messageOwner.media != null) {
            messageOwner.media.storyItem = old.messageOwner.media.storyItem;
        }
        if (isSpoilersRevealed && textLayoutBlocks != null) {
            for (TextLayoutBlock block : textLayoutBlocks) {
                block.spoilers.clear();
            }
        }
    }

    public ArrayList<ReactionsLayoutInBubble.VisibleReaction> getChoosenReactions() {
        ArrayList<ReactionsLayoutInBubble.VisibleReaction> choosenReactions = new ArrayList<>();
        TLRPC.ReactionCount newReaction = null;
        if (messageOwner.reactions == null) {
            return choosenReactions;
        }
        for (int i = 0; i < messageOwner.reactions.results.size(); i++) {
            if (messageOwner.reactions.results.get(i).chosen) {
                choosenReactions.add(ReactionsLayoutInBubble.VisibleReaction.fromTL(messageOwner.reactions.results.get(i).reaction));
            }
        }
        return choosenReactions;
    }

    public boolean isReplyToStory() {
        return !(replyMessageObject != null && replyMessageObject.messageOwner instanceof TLRPC.TL_messageEmpty) && messageOwner.reply_to != null && messageOwner.reply_to.story_id != 0 && (messageOwner.flags & TLRPC.MESSAGE_FLAG_REPLY) != 0;
    }

    public boolean isUnsupported() {
        return getMedia(messageOwner) instanceof TLRPC.TL_messageMediaUnsupported;
    }

    public boolean isExpiredStory() {
        return (type == MessageObject.TYPE_STORY || type == MessageObject.TYPE_STORY_MENTION) && messageOwner.media.storyItem instanceof TL_stories.TL_storyItemDeleted;
    }

    public static class SendAnimationData {
        public float x;
        public float y;
        public float width;
        public float height;
        public float currentScale;
        public float currentX;
        public float currentY;
        public float timeAlpha;
        public float progress;

        public boolean fromPreview;
        public ChatMessageCell.TransitionParams fromParams;
    }

    public static class VCardData {

        private String company;
        private ArrayList<String> emails = new ArrayList<>();
        private ArrayList<String> phones = new ArrayList<>();

        public static CharSequence parse(String data) {
            try {
                VCardData currentData = null;
                boolean finished = false;
                BufferedReader bufferedReader = new BufferedReader(new StringReader(data));

                String line;
                String originalLine;
                String pendingLine = null;
                while ((originalLine = line = bufferedReader.readLine()) != null) {
                    if (originalLine.startsWith("PHOTO")) {
                        continue;
                    } else {
                        if (originalLine.indexOf(':') >= 0) {
                            if (originalLine.startsWith("BEGIN:VCARD")) {
                                currentData = new VCardData();
                            } else if (originalLine.startsWith("END:VCARD")) {
                                if (currentData != null) {
                                    finished = true;
                                }
                            }
                        }
                    }
                    if (pendingLine != null) {
                        pendingLine += line;
                        line = pendingLine;
                        pendingLine = null;
                    }
                    if (line.contains("=QUOTED-PRINTABLE") && line.endsWith("=")) {
                        pendingLine = line.substring(0, line.length() - 1);
                        continue;
                    }
                    int idx = line.indexOf(":");
                    String[] args;
                    if (idx >= 0) {
                        args = new String[]{
                                line.substring(0, idx),
                                line.substring(idx + 1).trim()
                        };
                    } else {
                        args = new String[]{line.trim()};
                    }
                    if (args.length < 2 || currentData == null) {
                        continue;
                    }
                    if (args[0].startsWith("ORG")) {
                        String nameEncoding = null;
                        String nameCharset = null;
                        String[] params = args[0].split(";");
                        for (String param : params) {
                            String[] args2 = param.split("=");
                            if (args2.length != 2) {
                                continue;
                            }
                            if (args2[0].equals("CHARSET")) {
                                nameCharset = args2[1];
                            } else if (args2[0].equals("ENCODING")) {
                                nameEncoding = args2[1];
                            }
                        }
                        currentData.company = args[1];
                        if (nameEncoding != null && nameEncoding.equalsIgnoreCase("QUOTED-PRINTABLE")) {
                            byte[] bytes = AndroidUtilities.decodeQuotedPrintable(AndroidUtilities.getStringBytes(currentData.company));
                            if (bytes != null && bytes.length != 0) {
                                currentData.company = new String(bytes, nameCharset);
                            }
                        }
                        currentData.company = currentData.company.replace(';', ' ');
                    } else if (args[0].startsWith("TEL")) {
                        if (args[1].length() > 0) {
                            currentData.phones.add(args[1]);
                        }
                    } else if (args[0].startsWith("EMAIL")) {
                        String email = args[1];
                        if (email.length() > 0) {
                            currentData.emails.add(email);
                        }
                    }
                }
                try {
                    bufferedReader.close();
                } catch (Exception e) {
                    FileLog.e(e);
                }
                if (finished) {
                    StringBuilder result = new StringBuilder();
                    for (int a = 0; a < currentData.phones.size(); a++) {
                        if (result.length() > 0) {
                            result.append('\n');
                        }
                        String phone = currentData.phones.get(a);
                        if (phone.contains("#") || phone.contains("*")) {
                            result.append(phone);
                        } else {
                            result.append(PhoneFormat.getInstance().format(phone));
                        }
                    }
                    for (int a = 0; a < currentData.emails.size(); a++) {
                        if (result.length() > 0) {
                            result.append('\n');
                        }
                        result.append(PhoneFormat.getInstance().format(currentData.emails.get(a)));
                    }
                    if (!TextUtils.isEmpty(currentData.company)) {
                        if (result.length() > 0) {
                            result.append('\n');
                        }
                        result.append(currentData.company);
                    }
                    return result;
                }
            } catch (Throwable ignore) {

            }
            return null;
        }
    }

    public static boolean expandedQuotesEquals(HashSet<Integer> a, HashSet<Integer> b) {
        if (a == null && b == null)
            return true;
        if ((a == null ? 0 : a.size()) != (b == null ? 0 : b.size()))
            return false;
        return a != null && a.equals(b);
    }

    public static class TextLayoutBlock implements MultiLayoutTypingAnimator.Block {
        public final static int FLAG_RTL = 1, FLAG_NOT_RTL = 2;

        @Override
        public Layout getLayout() {
            return textLayout;
        }

        @Override
        public View getParentView() {
            return null;
        }

        public boolean first, last;
        public int index;
        public int start;

        public AtomicReference<Layout> spoilersPatchedTextLayout = new AtomicReference<>();
        public StaticLayout textLayout;
        public int padTop, padBottom;
        public int charactersOffset;
        public int charactersEnd;
        public int originalWidth;
        public int height;
        public int heightByOffset;
        public byte directionFlags;
        public List<SpoilerEffect> spoilers = new ArrayList<>();
        public float maxRight;

        public MessageObject messageObject;
        public int collapsedHeight;
        public ButtonBounce collapsedBounce;

        public boolean code;
        public boolean quote;
        public boolean quoteCollapse;

        public String language;
        public Text languageLayout;
        public int languageHeight; // included in padTop

        public boolean hasCodeCopyButton;
        public int copyIconColor;
        public Drawable copyIcon;
        public Text copyText;
        public int copySelectorColor;
        public Drawable copySelector;
        public Paint copySeparator;

        public int heightCollapsed() {
            return quoteCollapse ? collapsedHeight : height;
        }

        public int height() {
            return quoteCollapse && collapsed() ? collapsedHeight : height;
        }

        public int height(ChatMessageCell.TransitionParams tp) {
            if (!quoteCollapse)
                return height;
            return AndroidUtilities.lerp(height, collapsedHeight, collapsed(tp));
        }

        public float collapsed(ChatMessageCell.TransitionParams tp) {
            final boolean pastCollapsed = tp.animateExpandedQuotes ? (tp.animateExpandedQuotesFrom == null ? true : !tp.animateExpandedQuotesFrom.contains(index)) : collapsed();
            return AndroidUtilities.lerp(pastCollapsed ? 1f : 0f, collapsed() ? 1f : 0f, tp.animateChangeProgress);
        }

        public boolean collapsed() {
            return messageObject == null || messageObject.expandedQuotes == null || !messageObject.expandedQuotes.contains(index);
        }

        public float textYOffset(ArrayList<TextLayoutBlock> blocks) {
            if (blocks == null) return 0;
            int h = 0;
            for (int i = 0; i < blocks.size(); ++i) {
                TextLayoutBlock block = blocks.get(i);
                if (block == this) break;
                h += block.padTop + block.height() + block.padBottom;
            }
            return h;
        }

        public float textYOffset(ArrayList<TextLayoutBlock> blocks, ChatMessageCell.TransitionParams tp) {
            if (blocks == null) return 0;
            int h = 0;
            for (int i = 0; i < blocks.size(); ++i) {
                TextLayoutBlock block = blocks.get(i);
                if (block == this) break;
                h += block.padTop + block.height(tp) + block.padBottom;
            }
            return h;
        }

        public void layoutCode(String lng, int codeLength, boolean noforwards) {
            hasCodeCopyButton = codeLength >= 75 && !noforwards;
            if (hasCodeCopyButton) {
                copyText = new Text(getString(R.string.CopyCode).toUpperCase(), SharedConfig.fontSize - 3, AndroidUtilities.bold());
                copyIcon = ApplicationLoader.applicationContext.getResources().getDrawable(R.drawable.msg_copy).mutate();
                copyIcon.setColorFilter(new PorterDuffColorFilter(copyIconColor, PorterDuff.Mode.SRC_IN));
                copySelector = Theme.createRadSelectorDrawable(copySelectorColor, 0, 0, Math.min(5, SharedConfig.bubbleRadius), 0);
                copySeparator = new Paint(Paint.ANTI_ALIAS_FLAG);
            }
            if (TextUtils.isEmpty(lng)) {
                language = null;
                languageLayout = null;
                return;
            }
            language = lng;
            languageLayout = new Text(
                capitalizeLanguage(lng),
                SharedConfig.fontSize - 1 - CodeHighlighting.getTextSizeDecrement(codeLength) / 2,
                AndroidUtilities.bold()
            );
            languageHeight = (int) (languageLayout.getTextSize() * 1.714f) + dp(4);
        }

        public void drawCopyCodeButton(Canvas canvas, RectF bounds, int textColor, int backgroundColor, float alpha) {
            if (!hasCodeCopyButton) {
                return;
            }

            final int selectorColor = Theme.multAlpha(textColor, .10f);
            if (copySelectorColor != selectorColor) {
                Theme.setSelectorDrawableColor(copySelector, copySelectorColor = selectorColor, true);
            }
            copySelector.setBounds((int) bounds.left + dp(3), (int) (bounds.bottom - dp(38)), (int) bounds.right, (int) bounds.bottom);
            copySelector.setAlpha((int) (0xFF * alpha));
            if (copySelector.getCallback() != null) {
                copySelector.draw(canvas);
            }

            copySeparator.setColor(ColorUtils.setAlphaComponent(backgroundColor, 0x26));
            canvas.drawRect(bounds.left + dp(10), bounds.bottom - dp(38) - AndroidUtilities.getShadowHeight(), bounds.right - dp(6.66f), bounds.bottom - dp(38), copySeparator);

            final float iconScale = .8f;
            final float contentWidth = Math.min(bounds.width() - dp(12), copyIcon.getIntrinsicWidth() * iconScale + dp(5) + copyText.getCurrentWidth());
            float x = bounds.centerX() - contentWidth / 2f;
            final float cy = bounds.bottom - dp(38) / 2f;

            if (copyIconColor != textColor) {
                copyIcon.setColorFilter(new PorterDuffColorFilter(copyIconColor = textColor, PorterDuff.Mode.SRC_IN));
            }
            copyIcon.setAlpha((int) (0xFF * alpha));
            copyIcon.setBounds(
                (int) x,
                (int) (cy - copyIcon.getIntrinsicHeight() * iconScale / 2f),
                (int) (x + copyIcon.getIntrinsicWidth() * iconScale),
                (int) (cy + copyIcon.getIntrinsicHeight() * iconScale / 2f)
            );
            copyIcon.draw(canvas);

            x += copyIcon.getIntrinsicWidth() * iconScale + dp(5);
            copyText
                .ellipsize((int) (contentWidth - (copyIcon.getIntrinsicWidth() * iconScale + dp(5))) + dp(12))
                .draw(canvas, x, cy, textColor, alpha);
        }

        public static String capitalizeLanguage(String lng) {
            if (lng == null) return null;
            String llng = lng.toLowerCase().replaceAll("\\W|lang$", "");
            switch (llng) {
                case "js":
                case "javascript":
                    return "JavaScript";
                case "ts":
                case "typescript":
                    return "TypeScript";
                case "objc":
                case "objectivec":
                    return "Objective-C";
                case "md":
                case "markdown":
                    return "Markdown";
                case "rb":
                case "ruby":
                    return "Ruby";
                case "py":
                case "python":
                    return "Python";
                case "visual-basic":
                    return "Visual Basic";
                case "vbnet":
                    return "VB.NET";
                case "gdscript":
                    return "GDScript";
                case "bbcode":
                    return "BBCode";
                case "actionscript": return "ActionScript";
                case "autohotkey": return "AutoHotKey";
                case "cpp": return "C++";
                case "csharp":
                case "cs":
                    return "C#";
                case "fsharp":
                    return "F#";
                case "aspnet": return "ASP.NET";
                case "http":
                case "html":
                case "css":
                case "scss":
                case "less":
                case "asm":
                case "nasm":
                case "wasm":
                case "xml":
                case "yaml":
                case "yml":
                case "php":
                case "json":
                case "json5":
                case "r":
                case "ini":
                case "glsl":
                case "hlsl":
                case "csv":
                case "cobol":
                case "jsx":
                case "tsx":
                case "tl":
                case "sql":
                case "qml":
                case "sass":
                case "matlab":
                case "jsonp":
                    return lng.toUpperCase();
                case "tl-b":
                case "tlb":
                    return "TL-B";
                case "func":
                    return "FunC";
            }
            return capitalizeFirst(lng);
        }

        private static String capitalizeFirst(String str) {
            return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
        }

        public boolean isRtl() {
            return (directionFlags & FLAG_RTL) != 0 && (directionFlags & FLAG_NOT_RTL) == 0;
        }
    }

    public static final int POSITION_FLAG_LEFT = 1;
    public static final int POSITION_FLAG_RIGHT = 2;
    public static final int POSITION_FLAG_TOP = 4;
    public static final int POSITION_FLAG_BOTTOM = 8;

    public static class GroupedMessagePosition {
        public byte minX;
        public byte maxX;
        public byte minY;
        public byte maxY;
        public int pw;
        public float ph;
        public float aspectRatio;
        public boolean last;
        public int spanSize;
        public int leftSpanOffset;
        public boolean edge;
        public int flags;
        public float[] siblingHeights;

        public int photoWidth;
        public int photoHeight;

        public float top; // sum of ph of media above
        public float left; // sum of pw of media on the left side

        public void set(int minX, int maxX, int minY, int maxY, int w, float h, int flags) {
            this.minX = (byte) minX;
            this.maxX = (byte) maxX;
            this.minY = (byte) minY;
            this.maxY = (byte) maxY;
            this.pw = w;
            this.spanSize = w;
            this.ph = h;
            this.flags = (byte) flags;
        }
    }

    public static class GroupedMessages {
        public long groupId;
        public boolean hasSibling;
        public boolean hasCaption;
        public ArrayList<MessageObject> messages = new ArrayList<>();
        public ArrayList<GroupedMessagePosition> posArray = new ArrayList<>();
        public HashMap<MessageObject, GroupedMessagePosition> positions = new HashMap<>();
        public LongSparseArray<GroupedMessagePosition> positionsArray = new LongSparseArray<>();
        public MessageObject captionMessage;
        public boolean isDocuments;
        public boolean captionAbove;

        public int cachedWidthForCaption = -1;

        public GroupedMessagePosition getPosition(MessageObject msg) {
            if (msg == null) {
                return null;
            }
            GroupedMessagePosition pos = positions.get(msg);
            if (pos == null) {
                pos = positionsArray.get(msg.getId());
            }
            return pos;
        }

        private int maxSizeWidth = 800;

        public final TransitionParams transitionParams = new TransitionParams();

        private static class MessageGroupedLayoutAttempt {

            public int[] lineCounts;
            public float[] heights;

            public MessageGroupedLayoutAttempt(int i1, int i2, float f1, float f2) {
                lineCounts = new int[]{i1, i2};
                heights = new float[]{f1, f2};
            }

            public MessageGroupedLayoutAttempt(int i1, int i2, int i3, float f1, float f2, float f3) {
                lineCounts = new int[]{i1, i2, i3};
                heights = new float[]{f1, f2, f3};
            }

            public MessageGroupedLayoutAttempt(int i1, int i2, int i3, int i4, float f1, float f2, float f3, float f4) {
                lineCounts = new int[]{i1, i2, i3, i4};
                heights = new float[]{f1, f2, f3, f4};
            }
        }

        private float multiHeight(float[] array, int start, int end) {
            float sum = 0;
            for (int a = start; a < end; a++) {
                sum += array[a];
            }
            return maxSizeWidth / sum;
        }

        public boolean reversed;

        public void calculate() {
            posArray.clear();
            positions.clear();
            positionsArray.clear();
            captionMessage = null;

            maxSizeWidth = 800;
            int firstSpanAdditionalSize = 200;

            int count = messages.size();
            if (count == 1) {
                captionMessage = messages.get(0);
                return;
            } else if (count < 1) {
                return;
            }

            float maxSizeHeight = 814.0f;
            StringBuilder proportions = new StringBuilder();
            float averageAspectRatio = 1.0f;
            boolean isOut = false;
            int maxX = 0;
            boolean forceCalc = false;
            boolean needShare = false;
            boolean isMusic = false;
            hasSibling = false;

            hasCaption = false;
            boolean checkCaption = true;

            captionAbove = false;
            for (int a = (reversed ? count - 1 : 0); (reversed ? a >= 0 : a < count);) {
                MessageObject messageObject = messages.get(a);
                if (a == (reversed ? count - 1 : 0)) {
                    messageObject.isOutOwnerCached = null;
                    isOut = messageObject.isOutOwner();
                    needShare = !isOut && (
                            messageObject.messageOwner.fwd_from != null && messageObject.messageOwner.fwd_from.saved_from_peer != null ||
                                    messageObject.messageOwner.from_id instanceof TLRPC.TL_peerUser && (messageObject.messageOwner.peer_id.channel_id != 0 || messageObject.messageOwner.peer_id.chat_id != 0 ||
                                            getMedia(messageObject.messageOwner) instanceof TLRPC.TL_messageMediaGame || getMedia(messageObject.messageOwner) instanceof TLRPC.TL_messageMediaInvoice)
                    );
                    if (messageObject.isMusic() || messageObject.isDocument()) {
                        isDocuments = true;
                    }
                }
                if (messageObject.messageOwner != null && messageObject.messageOwner.invert_media) {
                    captionAbove = true;
                }
                TLRPC.PhotoSize photoSize = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, AndroidUtilities.getPhotoSize());
                GroupedMessagePosition position = new GroupedMessagePosition();
                position.last = (reversed ? a == 0 : a == count - 1);
                position.aspectRatio = photoSize == null ? 1.0f : photoSize.w / (float) photoSize.h;

                if (position.aspectRatio > 1.2f) {
                    proportions.append("w");
                } else if (position.aspectRatio < 0.8f) {
                    proportions.append("n");
                } else {
                    proportions.append("q");
                }

                averageAspectRatio += position.aspectRatio;

                if (position.aspectRatio > 2.0f) {
                    forceCalc = true;
                }

                positions.put(messageObject, position);
                positionsArray.put(messageObject.getId(), position);
                posArray.add(position);

                if (messageObject.caption != null) {
                    if (checkCaption && captionMessage == null) {
                        captionMessage = messageObject;
                        checkCaption = false;
                    } else if (!isDocuments) {
                        captionMessage = null;
                    }
                    hasCaption = true;
                }

                if (reversed) {
                    a--;
                } else {
                    a++;
                }
            }
            if (isDocuments) {
                for (int a = 0; a < count; a++) {
                    GroupedMessagePosition pos = posArray.get(a);
                    pos.flags = POSITION_FLAG_LEFT | POSITION_FLAG_RIGHT;
                    if (a == 0) {
                        pos.flags |= POSITION_FLAG_TOP;
                        pos.last = false;
                    } else if (a == count - 1) {
                        pos.flags |= POSITION_FLAG_BOTTOM;
                        pos.last = true;
                    } else {
                        pos.last = false;
                    }
                    pos.edge = true;
                    pos.aspectRatio = 1.0f;
                    pos.minX = 0;
                    pos.maxX = 0;
                    pos.minY = (byte) a;
                    pos.maxY = (byte) a;
                    pos.spanSize = 1000;
                    pos.pw = maxSizeWidth;
                    pos.ph = 100;
                }
                return;
            }

            if (needShare) {
                maxSizeWidth -= 50;
                firstSpanAdditionalSize += 50;
            }

            int minHeight = dp(120);
            int minWidth = (int) (dp(120) / (Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) / (float) maxSizeWidth));
            int paddingsWidth = (int) (dp(40) / (Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) / (float) maxSizeWidth));

            float maxAspectRatio = maxSizeWidth / maxSizeHeight;
            averageAspectRatio = averageAspectRatio / count;

            float minH = dp(100) / maxSizeHeight;

            if (count == 1) {
                MessageObject.GroupedMessagePosition position1 = posArray.get(0);
                float height = Math.round(Math.min(maxSizeWidth / position1.aspectRatio, Math.min(maxSizeWidth / position1.aspectRatio, maxSizeHeight / 2.0f))) / maxSizeHeight;
                position1.set(0, 0, 0, 0, maxSizeWidth, height, POSITION_FLAG_LEFT | POSITION_FLAG_RIGHT | POSITION_FLAG_TOP | POSITION_FLAG_BOTTOM);
            } else if (!forceCalc && (count == 2 || count == 3 || count == 4)) {
                if (count == 2) {
                    GroupedMessagePosition position1 = posArray.get(0);
                    GroupedMessagePosition position2 = posArray.get(1);
                    String pString = proportions.toString();
                    if (pString.equals("ww") && averageAspectRatio > 1.4 * maxAspectRatio && position1.aspectRatio - position2.aspectRatio < 0.2) {
                        float height = Math.round(Math.min(maxSizeWidth / position1.aspectRatio, Math.min(maxSizeWidth / position2.aspectRatio, maxSizeHeight / 2.0f))) / maxSizeHeight;
                        position1.set(0, 0, 0, 0, maxSizeWidth, height, POSITION_FLAG_LEFT | POSITION_FLAG_RIGHT | POSITION_FLAG_TOP);
                        position2.set(0, 0, 1, 1, maxSizeWidth, height, POSITION_FLAG_LEFT | POSITION_FLAG_RIGHT | POSITION_FLAG_BOTTOM);
                    } else if (pString.equals("ww") || pString.equals("qq")) {
                        int width = maxSizeWidth / 2;
                        float height = Math.round(Math.min(width / position1.aspectRatio, Math.min(width / position2.aspectRatio, maxSizeHeight))) / maxSizeHeight;
                        position1.set(0, 0, 0, 0, width, height, POSITION_FLAG_LEFT | POSITION_FLAG_BOTTOM | POSITION_FLAG_TOP);
                        position2.set(1, 1, 0, 0, width, height, POSITION_FLAG_RIGHT | POSITION_FLAG_BOTTOM | POSITION_FLAG_TOP);
                        maxX = 1;
                    } else {
                        int secondWidth = (int) Math.max(0.4f * maxSizeWidth, Math.round((maxSizeWidth / position1.aspectRatio / (1.0f / position1.aspectRatio + 1.0f / position2.aspectRatio))));
                        int firstWidth = maxSizeWidth - secondWidth;
                        if (firstWidth < minWidth) {
                            int diff = minWidth - firstWidth;
                            firstWidth = minWidth;
                            secondWidth -= diff;
                        }

                        float height = Math.min(maxSizeHeight, Math.round(Math.min(firstWidth / position1.aspectRatio, secondWidth / position2.aspectRatio))) / maxSizeHeight;
                        position1.set(0, 0, 0, 0, firstWidth, height, POSITION_FLAG_LEFT | POSITION_FLAG_BOTTOM | POSITION_FLAG_TOP);
                        position2.set(1, 1, 0, 0, secondWidth, height, POSITION_FLAG_RIGHT | POSITION_FLAG_BOTTOM | POSITION_FLAG_TOP);
                        maxX = 1;
                    }
                } else if (count == 3) {
                    GroupedMessagePosition position1 = posArray.get(0);
                    GroupedMessagePosition position2 = posArray.get(1);
                    GroupedMessagePosition position3 = posArray.get(2);
                    if (proportions.charAt(0) == 'n') {
                        float thirdHeight = Math.min(maxSizeHeight * 0.5f, Math.round(position2.aspectRatio * maxSizeWidth / (position3.aspectRatio + position2.aspectRatio)));
                        float secondHeight = maxSizeHeight - thirdHeight;
                        int rightWidth = (int) Math.max(minWidth, Math.min(maxSizeWidth * 0.5f, Math.round(Math.min(thirdHeight * position3.aspectRatio, secondHeight * position2.aspectRatio))));

                        int leftWidth = Math.round(Math.min(maxSizeHeight * position1.aspectRatio + paddingsWidth, maxSizeWidth - rightWidth));
                        position1.set(0, 0, 0, 1, leftWidth, 1.0f, POSITION_FLAG_LEFT | POSITION_FLAG_BOTTOM | POSITION_FLAG_TOP);

                        position2.set(1, 1, 0, 0, rightWidth, secondHeight / maxSizeHeight, POSITION_FLAG_RIGHT | POSITION_FLAG_TOP);

                        position3.set(0, 1, 1, 1, rightWidth, thirdHeight / maxSizeHeight, POSITION_FLAG_RIGHT | POSITION_FLAG_BOTTOM);
                        position3.spanSize = maxSizeWidth;

                        position1.siblingHeights = new float[]{thirdHeight / maxSizeHeight, secondHeight / maxSizeHeight};

                        if (isOut) {
                            position1.spanSize = maxSizeWidth - rightWidth;
                        } else {
                            position2.spanSize = maxSizeWidth - leftWidth;
                            position3.leftSpanOffset = leftWidth;
                        }
                        hasSibling = true;
                        maxX = 1;
                    } else {
                        float firstHeight = Math.round(Math.min(maxSizeWidth / position1.aspectRatio, (maxSizeHeight) * 0.66f)) / maxSizeHeight;
                        position1.set(0, 1, 0, 0, maxSizeWidth, firstHeight, POSITION_FLAG_LEFT | POSITION_FLAG_RIGHT | POSITION_FLAG_TOP);

                        int width = maxSizeWidth / 2;
                        float secondHeight = Math.min(maxSizeHeight - firstHeight, Math.round(Math.min(width / position2.aspectRatio, width / position3.aspectRatio))) / maxSizeHeight;
                        if (secondHeight < minH) {
                            secondHeight = minH;
                        }
                        position2.set(0, 0, 1, 1, width, secondHeight, POSITION_FLAG_LEFT | POSITION_FLAG_BOTTOM);
                        position3.set(1, 1, 1, 1, width, secondHeight, POSITION_FLAG_RIGHT | POSITION_FLAG_BOTTOM);
                        maxX = 1;
                    }
                } else {
                    GroupedMessagePosition position1 = posArray.get(0);
                    GroupedMessagePosition position2 = posArray.get(1);
                    GroupedMessagePosition position3 = posArray.get(2);
                    GroupedMessagePosition position4 = posArray.get(3);
                    if (proportions.charAt(0) == 'w') {
                        float h0 = Math.round(Math.min(maxSizeWidth / position1.aspectRatio, maxSizeHeight * 0.66f)) / maxSizeHeight;
                        position1.set(0, 2, 0, 0, maxSizeWidth, h0, POSITION_FLAG_LEFT | POSITION_FLAG_RIGHT | POSITION_FLAG_TOP);

                        float h = Math.round(maxSizeWidth / (position2.aspectRatio + position3.aspectRatio + position4.aspectRatio));
                        int w0 = (int) Math.max(minWidth, Math.min(maxSizeWidth * 0.4f, h * position2.aspectRatio));
                        int w2 = (int) Math.max(Math.max(minWidth, maxSizeWidth * 0.33f), h * position4.aspectRatio);
                        int w1 = maxSizeWidth - w0 - w2;
                        if (w1 < dp(58)) {
                            int diff = dp(58) - w1;
                            w1 = dp(58);
                            w0 -= diff / 2;
                            w2 -= (diff - diff / 2);
                        }
                        h = Math.min(maxSizeHeight - h0, h);
                        h /= maxSizeHeight;
                        if (h < minH) {
                            h = minH;
                        }
                        position2.set(0, 0, 1, 1, w0, h, POSITION_FLAG_LEFT | POSITION_FLAG_BOTTOM);
                        position3.set(1, 1, 1, 1, w1, h, POSITION_FLAG_BOTTOM);
                        position4.set(2, 2, 1, 1, w2, h, POSITION_FLAG_RIGHT | POSITION_FLAG_BOTTOM);
                        maxX = 2;
                    } else {
                        int w = Math.max(minWidth, Math.round(maxSizeHeight / (1.0f / position2.aspectRatio + 1.0f / position3.aspectRatio + 1.0f / position4.aspectRatio)));
                        float h0 = Math.min(0.33f, Math.max(minHeight, w / position2.aspectRatio) / maxSizeHeight);
                        float h1 = Math.min(0.33f, Math.max(minHeight, w / position3.aspectRatio) / maxSizeHeight);
                        float h2 = 1.0f - h0 - h1;
                        int w0 = Math.round(Math.min(maxSizeHeight * position1.aspectRatio + paddingsWidth, maxSizeWidth - w));

                        position1.set(0, 0, 0, 2, w0, h0 + h1 + h2, POSITION_FLAG_LEFT | POSITION_FLAG_TOP | POSITION_FLAG_BOTTOM);

                        position2.set(1, 1, 0, 0, w, h0, POSITION_FLAG_RIGHT | POSITION_FLAG_TOP);

                        position3.set(0, 1, 1, 1, w, h1, POSITION_FLAG_RIGHT);
                        position3.spanSize = maxSizeWidth;

                        position4.set(0, 1, 2, 2, w, h2, POSITION_FLAG_RIGHT | POSITION_FLAG_BOTTOM);
                        position4.spanSize = maxSizeWidth;

                        if (isOut) {
                            position1.spanSize = maxSizeWidth - w;
                        } else {
                            position2.spanSize = maxSizeWidth - w0;
                            position3.leftSpanOffset = w0;
                            position4.leftSpanOffset = w0;
                        }
                        position1.siblingHeights = new float[]{h0, h1, h2};
                        hasSibling = true;
                        maxX = 1;
                    }
                }
            } else {
                float[] croppedRatios = new float[posArray.size()];
                for (int a = 0; a < count; a++) {
                    if (averageAspectRatio > 1.1f) {
                        croppedRatios[a] = Math.max(1.0f, posArray.get(a).aspectRatio);
                    } else {
                        croppedRatios[a] = Math.min(1.0f, posArray.get(a).aspectRatio);
                    }
                    croppedRatios[a] = Math.max(0.66667f, Math.min(1.7f, croppedRatios[a]));
                }

                int firstLine;
                int secondLine;
                int thirdLine;
                int fourthLine;
                ArrayList<MessageGroupedLayoutAttempt> attempts = new ArrayList<>();
                for (firstLine = 1; firstLine < croppedRatios.length; firstLine++) {
                    secondLine = croppedRatios.length - firstLine;
                    if (firstLine > 3 || secondLine > 3) {
                        continue;
                    }
                    attempts.add(new MessageGroupedLayoutAttempt(firstLine, secondLine, multiHeight(croppedRatios, 0, firstLine), multiHeight(croppedRatios, firstLine, croppedRatios.length)));
                }

                for (firstLine = 1; firstLine < croppedRatios.length - 1; firstLine++) {
                    for (secondLine = 1; secondLine < croppedRatios.length - firstLine; secondLine++) {
                        thirdLine = croppedRatios.length - firstLine - secondLine;
                        if (firstLine > 3 || secondLine > (averageAspectRatio < 0.85f ? 4 : 3) || thirdLine > 3) {
                            continue;
                        }
                        attempts.add(new MessageGroupedLayoutAttempt(firstLine, secondLine, thirdLine, multiHeight(croppedRatios, 0, firstLine), multiHeight(croppedRatios, firstLine, firstLine + secondLine), multiHeight(croppedRatios, firstLine + secondLine, croppedRatios.length)));
                    }
                }

                for (firstLine = 1; firstLine < croppedRatios.length - 2; firstLine++) {
                    for (secondLine = 1; secondLine < croppedRatios.length - firstLine; secondLine++) {
                        for (thirdLine = 1; thirdLine < croppedRatios.length - firstLine - secondLine; thirdLine++) {
                            fourthLine = croppedRatios.length - firstLine - secondLine - thirdLine;
                            if (firstLine > 3 || secondLine > 3 || thirdLine > 3 || fourthLine > 3) {
                                continue;
                            }
                            attempts.add(new MessageGroupedLayoutAttempt(firstLine, secondLine, thirdLine, fourthLine, multiHeight(croppedRatios, 0, firstLine), multiHeight(croppedRatios, firstLine, firstLine + secondLine), multiHeight(croppedRatios, firstLine + secondLine, firstLine + secondLine + thirdLine), multiHeight(croppedRatios, firstLine + secondLine + thirdLine, croppedRatios.length)));
                        }
                    }
                }

                MessageGroupedLayoutAttempt optimal = null;
                float optimalDiff = 0.0f;
                float maxHeight = maxSizeWidth / 3 * 4;
                for (int a = 0; a < attempts.size(); a++) {
                    MessageGroupedLayoutAttempt attempt = attempts.get(a);
                    float height = 0;
                    float minLineHeight = Float.MAX_VALUE;
                    for (int b = 0; b < attempt.heights.length; b++) {
                        height += attempt.heights[b];
                        if (attempt.heights[b] < minLineHeight) {
                            minLineHeight = attempt.heights[b];
                        }
                    }

                    float diff = Math.abs(height - maxHeight);
                    if (attempt.lineCounts.length > 1) {
                        if (attempt.lineCounts[0] > attempt.lineCounts[1] || (attempt.lineCounts.length > 2 && attempt.lineCounts[1] > attempt.lineCounts[2]) || (attempt.lineCounts.length > 3 && attempt.lineCounts[2] > attempt.lineCounts[3])) {
                            diff *= 1.2f;
                        }
                    }

                    if (minLineHeight < minWidth) {
                        diff *= 1.5f;
                    }

                    if (optimal == null || diff < optimalDiff) {
                        optimal = attempt;
                        optimalDiff = diff;
                    }
                }
                if (optimal == null) {
                    return;
                }

                int index = 0;
                float y = 0.0f;

                for (int i = 0; i < optimal.lineCounts.length; i++) {
                    int c = optimal.lineCounts[i];
                    float lineHeight = optimal.heights[i];
                    int spanLeft = maxSizeWidth;
                    GroupedMessagePosition posToFix = null;
                    maxX = Math.max(maxX, c - 1);
                    for (int k = 0; k < c; k++) {
                        float ratio = croppedRatios[index];
                        int width = (int) (ratio * lineHeight);
                        spanLeft -= width;
                        GroupedMessagePosition pos = posArray.get(index);
                        int flags = 0;
                        if (i == 0) {
                            flags |= POSITION_FLAG_TOP;
                        }
                        if (i == optimal.lineCounts.length - 1) {
                            flags |= POSITION_FLAG_BOTTOM;
                        }
                        if (k == 0) {
                            flags |= POSITION_FLAG_LEFT;
                            if (isOut) {
                                posToFix = pos;
                            }
                        }
                        if (k == c - 1) {
                            flags |= POSITION_FLAG_RIGHT;
                            if (!isOut) {
                                posToFix = pos;
                            }
                        }
                        pos.set(k, k, i, i, width, Math.max(minH, lineHeight / maxSizeHeight), flags);
                        index++;
                    }
                    posToFix.pw += spanLeft;
                    posToFix.spanSize += spanLeft;
                    y += lineHeight;
                }
            }
            int avatarOffset = 108;
            for (int a = 0; a < count; a++) {
                GroupedMessagePosition pos = posArray.get(a);
                if (isOut) {
                    if (pos.minX == 0) {
                        pos.spanSize += firstSpanAdditionalSize;
                    }
                    if ((pos.flags & POSITION_FLAG_RIGHT) != 0) {
                        pos.edge = true;
                    }
                } else {
                    if (pos.maxX == maxX || (pos.flags & POSITION_FLAG_RIGHT) != 0) {
                        pos.spanSize += firstSpanAdditionalSize;
                    }
                    if ((pos.flags & POSITION_FLAG_LEFT) != 0) {
                        pos.edge = true;
                    }
                }
                MessageObject messageObject = messages.get(a);
                if (!isOut && messageObject.needDrawAvatarInternal()) {
                    if (pos.edge) {
                        if (pos.spanSize != 1000) {
                            pos.spanSize += avatarOffset;
                        }
                        pos.pw += avatarOffset;
                    } else if ((pos.flags & POSITION_FLAG_RIGHT) != 0) {
                        if (pos.spanSize != 1000) {
                            pos.spanSize -= avatarOffset;
                        } else if (pos.leftSpanOffset != 0) {
                            pos.leftSpanOffset += avatarOffset;
                        }
                    }
                }
            }
        }

        public int getMaxEditDate() {
            int result = 0;
            for (int i = 0; i < messages.size(); ++i) {
                MessageObject object = messages.get(i);
                if (object != null && object.messageOwner != null) {
                    result = Math.max(result, object.messageOwner.edit_date);
                }
            }
            return result;
        }

        public MessageObject findPrimaryMessageObject() {
            return findMessageWithFlags(reversed ? MessageObject.POSITION_FLAG_BOTTOM | MessageObject.POSITION_FLAG_RIGHT : MessageObject.POSITION_FLAG_TOP | MessageObject.POSITION_FLAG_LEFT);
        }

        public MessageObject findCaptionMessageObject() {
            if (!messages.isEmpty() && positions.isEmpty()) {
                calculate();
            }

            MessageObject result = null;
            for (int i = 0; i < messages.size(); ++i) {
                MessageObject object = messages.get(i);
                if (!TextUtils.isEmpty(object.caption)) {
                    if (result != null) {
                        return null;
                    } else {
                        result = object;
                    }
                }
            }
            return result;
        }

        public MessageObject findMessageWithFlags(int flags) {
            if (!messages.isEmpty() && positions.isEmpty()) {
                calculate();
            }
            for (int i = 0; i < messages.size(); i++) {
                MessageObject object = messages.get(i);
                MessageObject.GroupedMessagePosition position = positions.get(object);
                if (position != null && (position.flags & (flags)) == flags) {
                    return object;
                }
            }
            return null;
        }

        public static class TransitionParams {
            public int left;
            public int top;
            public int right;
            public int bottom;

            public float offsetLeft;
            public float offsetTop;
            public float offsetRight;
            public float offsetBottom;

            public boolean drawBackgroundForDeletedItems;
            public boolean backgroundChangeBounds;

            public boolean pinnedTop;
            public boolean pinnedBotton;

            public ChatMessageCell cell;
            public float captionEnterProgress = 1f;
            public boolean drawCaptionLayout;
            public boolean isNewGroup;

            public void reset() {
                captionEnterProgress = 1f;
                offsetBottom = 0;
                offsetTop = 0;
                offsetRight = 0;
                offsetLeft = 0;
                backgroundChangeBounds = false;
            }
        }

        public boolean contains(int messageId) {
            if (messages == null) {
                return false;
            }
            for (int i = 0; i < messages.size(); ++i) {
                MessageObject msg = messages.get(i);
                if (msg != null && msg.getId() == messageId) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final int LINES_PER_BLOCK = 10;
    private static final int LINES_PER_BLOCK_WITH_EMOJI = 5;

    public int getLastLineWidth() {
        if (richLayout != null) {
            return richLayout.getLastLineWidth();
        }
        return lastLineWidth;
    }

    public int lastLineWidth;
    public int textWidth;
    public boolean hasRtl;
    public float textXOffset;
    public RichMessageLayout richLayout;
    public boolean richCheckboxEcho; // next edit-echo for this message is our own checkbox toggle -> reuse layout, skip relayout
    public ArrayList<TextLayoutBlock> textLayoutBlocks;
    public boolean hasCode;
    public boolean hasWideCode;
    public boolean hasCodeAtTop, hasCodeAtBottom;
    public boolean hasQuote;
    public boolean hasSingleQuote;
    public boolean hasSingleCode;
    public boolean hasQuoteAtBottom;

    public MessageObject(int accountNum, TL_stories.StoryItem storyItem) {
        currentAccount = accountNum;
        this.storyItem = storyItem;
        if (storyItem != null) {
            messageOwner = new TLRPC.TL_message();
            messageOwner.id = storyItem.messageId;
            messageOwner.realId = storyItem.id;
            messageOwner.date = storyItem.date;
            messageOwner.dialog_id = storyItem.dialogId;
            messageOwner.message = storyItem.caption;
            messageOwner.entities = storyItem.entities;
            messageOwner.media = storyItem.media;
            messageOwner.attachPath = storyItem.attachPath;
        }
        photoThumbs = new ArrayList<>();
        photoThumbs2 = new ArrayList<>();
    }

    public MessageObject(int accountNum, TLRPC.Message message, String formattedMessage, String name, String userName, boolean localMessage, boolean isChannel, boolean supergroup, boolean edit) {
        localType = localMessage ? 2 : 1;
        currentAccount = accountNum;
        localName = name;
        localUserName = userName;
        messageText = formattedMessage;
        messageOwner = message;
        localChannel = isChannel;
        localSupergroup = supergroup;
        localEdit = edit;
    }

    public MessageObject(int accountNum, TLRPC.Message message, AbstractMap<Long, TLRPC.User> users, boolean generateLayout, boolean checkMediaExists) {
        this(accountNum, message, users, null, generateLayout, checkMediaExists);
    }

    public MessageObject(int accountNum, TLRPC.Message message, LongSparseArray<TLRPC.User> users, boolean generateLayout, boolean checkMediaExists) {
        this(accountNum, message, users, null, generateLayout, checkMediaExists);
    }

    public MessageObject(int accountNum, TLRPC.Message message, boolean generateLayout, boolean checkMediaExists) {
        this(accountNum, message, null, null, null, null, null, generateLayout, checkMediaExists, 0);
    }

    public MessageObject(int accountNum, TLRPC.Message message, MessageObject replyToMessage, boolean generateLayout, boolean checkMediaExists) {
        this(accountNum, message, replyToMessage, null, null, null, null, generateLayout, checkMediaExists, 0);
    }

    public MessageObject(int accountNum, TLRPC.Message message, AbstractMap<Long, TLRPC.User> users, AbstractMap<Long, TLRPC.Chat> chats, boolean generateLayout, boolean checkMediaExists) {
        this(accountNum, message, users, chats, generateLayout, checkMediaExists, 0);
    }

    public MessageObject(int accountNum, TLRPC.Message message, LongSparseArray<TLRPC.User> users, LongSparseArray<TLRPC.Chat> chats, boolean generateLayout, boolean checkMediaExists) {
        this(accountNum, message, null, null, null, users, chats, generateLayout, checkMediaExists, 0, false, false, false);
    }

    public MessageObject(int accountNum, TLRPC.Message message, LongSparseArray<TLRPC.User> users, LongSparseArray<TLRPC.Chat> chats, boolean generateLayout, boolean checkMediaExists,  boolean isSavedMessages) {
        this(accountNum, message, null, null, null, users, chats, generateLayout, checkMediaExists, 0, false, false, isSavedMessages);
    }

    public MessageObject(int accountNum, TLRPC.Message message, AbstractMap<Long, TLRPC.User> users, AbstractMap<Long, TLRPC.Chat> chats, boolean generateLayout, boolean checkMediaExists, long eid) {
        this(accountNum, message, null, users, chats, null, null, generateLayout, checkMediaExists, eid);
    }

    public MessageObject(int accountNum, TLRPC.Message message, MessageObject replyToMessage, AbstractMap<Long, TLRPC.User> users, AbstractMap<Long, TLRPC.Chat> chats, LongSparseArray<TLRPC.User> sUsers, LongSparseArray<TLRPC.Chat> sChats, boolean generateLayout, boolean checkMediaExists, long eid) {
        this(accountNum, message, replyToMessage, users, chats, sUsers, sChats, generateLayout, checkMediaExists, eid, false, false, false);
    }

    public MessageObject(int accountNum, TLRPC.Message message, MessageObject replyToMessage, AbstractMap<Long, TLRPC.User> users, AbstractMap<Long, TLRPC.Chat> chats, LongSparseArray<TLRPC.User> sUsers, LongSparseArray<TLRPC.Chat> sChats, boolean generateLayout, boolean checkMediaExists, long eid, boolean isRepostPreview, boolean isRepostVideoPreview, boolean isSavedMessages) {
        this(accountNum, message, replyToMessage, users, chats, sUsers, sChats, generateLayout, checkMediaExists, eid, isRepostPreview, isRepostVideoPreview, isSavedMessages, 0);
    }

    public MessageObject(int accountNum, TLRPC.Message message, MessageObject replyToMessage, AbstractMap<Long, TLRPC.User> users, AbstractMap<Long, TLRPC.Chat> chats, LongSparseArray<TLRPC.User> sUsers, LongSparseArray<TLRPC.Chat> sChats, boolean generateLayout, boolean checkMediaExists, long eid, boolean isRepostPreview, boolean isRepostVideoPreview, boolean isSavedMessages, int searchType) {
        Theme.createCommonMessageResources();

        this.isRepostPreview = isRepostPreview;
        this.isRepostVideoPreview = isRepostVideoPreview;
        this.isSaved = isSavedMessages || getDialogId(message) == UserConfig.getInstance(accountNum).getClientUserId();
        this.searchType = searchType;

        currentAccount = accountNum;
        messageOwner = message;
        replyMessageObject = replyToMessage;
        eventId = eid;
        wasUnread = !messageOwner.out && messageOwner.unread;

        if (message.replyMessage != null) {
            replyMessageObject = new MessageObject(currentAccount, message.replyMessage, null, users, chats, sUsers, sChats, false, checkMediaExists, eid);
        }

        TLRPC.User fromUser = null;
        if (message.from_id instanceof TLRPC.TL_peerUser) {
            fromUser = getUser(users, sUsers, message.from_id.user_id);
        }

        updateMessageText(users, chats, sUsers, sChats);
        setType();
        if (generateLayout) {
            updateTranslation(false);
        }
        measureInlineBotButtons();

        Calendar rightNow = new GregorianCalendar();
        rightNow.setTimeInMillis((long) (messageOwner.date) * 1000);
        int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
        int dateYear = rightNow.get(Calendar.YEAR);
        int dateMonth = rightNow.get(Calendar.MONTH);
        dateKey = String.format("%d_%02d_%02d", dateYear, dateMonth, dateDay);
        dateKeyInt = dateYear + 10000 * dateMonth + 10000 * 100 * dateDay;
        monthKey = String.format("%d_%02d", dateYear, dateMonth);

        createMessageSendInfo();
        generateCaption();
        if (generateLayout) {
            TextPaint paint;
            if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaGame) {
                paint = Theme.chat_msgGameTextPaint;
            } else {
                paint = Theme.chat_msgTextPaint;
            }
            int[] emojiOnly = allowsBigEmoji() ? new int[1] : null;
            messageText = Emoji.replaceEmoji(messageText, paint.getFontMetricsInt(), false, emojiOnly);
            messageText = replaceAnimatedEmoji(messageText, paint.getFontMetricsInt());
            if (emojiOnly != null && emojiOnly[0] > 1) {
                replaceEmojiToLottieFrame(messageText, emojiOnly);
            }
            checkEmojiOnly(emojiOnly);
            checkBigAnimatedEmoji();
            setType();
            createPathThumb();
        }
        layoutCreated = generateLayout;
        generateThumbs(false);
        if (checkMediaExists) {
            checkMediaExistance();
        }
    }

    protected void checkBigAnimatedEmoji() {
        emojiAnimatedSticker = null;
        emojiAnimatedStickerId = null;
        if (emojiOnlyCount == 1 && !(getMedia(messageOwner) instanceof TLRPC.TL_messageMediaWebPage) && !(getMedia(messageOwner) instanceof TLRPC.TL_messageMediaInvoice) && (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaEmpty || getMedia(messageOwner) == null) && this.messageOwner.grouped_id == 0) {
            if (messageOwner.entities.isEmpty()) {
                CharSequence emoji = messageText;
                int index;
                if ((index = TextUtils.indexOf(emoji, "\uD83C\uDFFB")) >= 0) {
                    emojiAnimatedStickerColor = "_c1";
                    emoji = emoji.subSequence(0, index);
                } else if ((index = TextUtils.indexOf(emoji, "\uD83C\uDFFC")) >= 0) {
                    emojiAnimatedStickerColor = "_c2";
                    emoji = emoji.subSequence(0, index);
                } else if ((index = TextUtils.indexOf(emoji, "\uD83C\uDFFD")) >= 0) {
                    emojiAnimatedStickerColor = "_c3";
                    emoji = emoji.subSequence(0, index);
                } else if ((index = TextUtils.indexOf(emoji, "\uD83C\uDFFE")) >= 0) {
                    emojiAnimatedStickerColor = "_c4";
                    emoji = emoji.subSequence(0, index);
                } else if ((index = TextUtils.indexOf(emoji, "\uD83C\uDFFF")) >= 0) {
                    emojiAnimatedStickerColor = "_c5";
                    emoji = emoji.subSequence(0, index);
                } else {
                    emojiAnimatedStickerColor = "";
                }
                if (!TextUtils.isEmpty(emojiAnimatedStickerColor) && index + 2 < messageText.length()) {
                    emoji = emoji.toString() + messageText.subSequence(index + 2, messageText.length()).toString();
                }
                if (TextUtils.isEmpty(emojiAnimatedStickerColor) || EmojiData.emojiColoredMap.contains(emoji.toString())) {
                    emojiAnimatedSticker = MediaDataController.getInstance(currentAccount).getEmojiAnimatedSticker(emoji);
                }
            } else if (messageOwner.entities.size() == 1 && messageOwner.entities.get(0) instanceof TLRPC.TL_messageEntityCustomEmoji) {
                try {
                    emojiAnimatedStickerId = ((TLRPC.TL_messageEntityCustomEmoji) messageOwner.entities.get(0)).document_id;
                    emojiAnimatedSticker = AnimatedEmojiDrawable.findDocument(currentAccount, emojiAnimatedStickerId);
                    if (emojiAnimatedSticker == null && messageText instanceof Spanned) {
                        AnimatedEmojiSpan[] animatedEmojiSpans = ((Spanned) messageText).getSpans(0, messageText.length(), AnimatedEmojiSpan.class);
                        if (animatedEmojiSpans != null && animatedEmojiSpans.length == 1) {
                            emojiAnimatedSticker = animatedEmojiSpans[0].document;
                        }
                    }
                } catch (Exception ignore) {
                }
            }
        }
        if (emojiAnimatedSticker == null && emojiAnimatedStickerId == null) {
            generateLayout(null);
        } else if (isSticker()) {
            type = TYPE_STICKER;
        } else if (isAnimatedSticker()) {
            type = TYPE_ANIMATED_STICKER;
        } else {
            type = 1000;
        }
    }

    private void createPathThumb() {
        TLRPC.Document document = getDocument();
        if (document == null) {
            return;
        }
        pathThumb = DocumentObject.getSvgThumb(document, Theme.key_chat_serviceBackground, 1.0f);
    }

    public void createStrippedThumb() {
        if (photoThumbs == null || !canCreateStripedThubms() && !hasExtendedMediaPreview() || strippedThumb != null) {
            return;
        }
        try {
            String filter = "b";
            if (isRoundVideo()) {
                filter += "r";
            }
            for (int a = 0, N = photoThumbs.size(); a < N; a++) {
                TLRPC.PhotoSize photoSize = photoThumbs.get(a);
                if (photoSize instanceof TLRPC.TL_photoStrippedSize) {
                    strippedThumb = new BitmapDrawable(ApplicationLoader.applicationContext.getResources(), ImageLoader.getStrippedPhotoBitmap(photoSize.bytes, filter));
                    break;
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private void createDateArray(int accountNum, TLRPC.TL_channelAdminLogEvent event, ArrayList<MessageObject> messageObjects, HashMap<String, ArrayList<MessageObject>> messagesByDays, boolean addToEnd) {
        ArrayList<MessageObject> dayArray = messagesByDays.get(dateKey);
        if (dayArray == null) {
            dayArray = new ArrayList<>();
            messagesByDays.put(dateKey, dayArray);
            TLRPC.TL_message dateMsg = new TLRPC.TL_message();
            dateMsg.message = LocaleController.formatDateChat(event.date);
            dateMsg.id = 0;
            dateMsg.date = event.date;
            MessageObject dateObj = new MessageObject(accountNum, dateMsg, false, false);
            dateObj.type = TYPE_DATE;
            dateObj.contentType = 1;
            dateObj.isDateObject = true;
            if (addToEnd) {
                messageObjects.add(0, dateObj);
            } else {
                messageObjects.add(dateObj);
            }
        }
    }

    public void checkForScam() {

    }

    private void checkEmojiOnly(int[] emojiOnly) {
        checkEmojiOnly(emojiOnly == null ? null : emojiOnly[0]);
    }

    private void checkEmojiOnly(Integer emojiOnly) {
        if (emojiOnly != null && emojiOnly >= 1 && messageOwner != null && !hasNonEmojiEntities()) {
            Emoji.EmojiSpan[] spans = ((Spannable) messageText).getSpans(0, messageText.length(), Emoji.EmojiSpan.class);
            AnimatedEmojiSpan[] aspans = ((Spannable) messageText).getSpans(0, messageText.length(), AnimatedEmojiSpan.class);
            emojiOnlyCount = Math.max(emojiOnly, (spans == null ? 0 : spans.length) + (aspans == null ? 0 : aspans.length));
            totalAnimatedEmojiCount = aspans == null ? 0 : aspans.length;
            animatedEmojiCount = 0;
            if (aspans != null) {
                for (int i = 0; i < aspans.length; ++i) {
                    if (!aspans[i].standard) {
                        animatedEmojiCount++;
                    }
                }
            }
            hasUnwrappedEmoji = emojiOnlyCount - (spans == null ? 0 : spans.length) - (aspans == null ? 0 : aspans.length) > 0;
            if (emojiOnlyCount == 0 || hasUnwrappedEmoji) {
                if (aspans != null && aspans.length > 0) {
                    for (int a = 0; a < aspans.length; a++) {
                        aspans[a].replaceFontMetrics(Theme.chat_msgTextPaint.getFontMetricsInt(), (int) (Theme.chat_msgTextPaint.getTextSize() + dp(4)), -1);
                        aspans[a].full = false;
                    }
                }
                return;
            }
            boolean large = emojiOnlyCount == animatedEmojiCount;
            int cacheType = -1;
            TextPaint emojiPaint;
            switch (Math.max(emojiOnlyCount, animatedEmojiCount)) {
                case 0:
                case 1:
                case 2:
                    cacheType = AnimatedEmojiDrawable.CACHE_TYPE_MESSAGES_LARGE;
                    emojiPaint = large ? Theme.chat_msgTextPaintEmoji[0] : Theme.chat_msgTextPaintEmoji[2];
                    break;
                case 3:
                    cacheType = AnimatedEmojiDrawable.CACHE_TYPE_MESSAGES_LARGE;
                    emojiPaint = large ? Theme.chat_msgTextPaintEmoji[1] : Theme.chat_msgTextPaintEmoji[3];
                    break;
                case 4:
                    cacheType = AnimatedEmojiDrawable.CACHE_TYPE_MESSAGES_LARGE;
                    emojiPaint = large ? Theme.chat_msgTextPaintEmoji[2] : Theme.chat_msgTextPaintEmoji[4];
                    break;
                case 5:
                    cacheType = AnimatedEmojiDrawable.CACHE_TYPE_KEYBOARD;
                    emojiPaint = large ? Theme.chat_msgTextPaintEmoji[3] : Theme.chat_msgTextPaintEmoji[5];
                    break;
                case 6:
                    cacheType = AnimatedEmojiDrawable.CACHE_TYPE_KEYBOARD;
                    emojiPaint = large ? Theme.chat_msgTextPaintEmoji[4] : Theme.chat_msgTextPaintEmoji[5];
                    break;
                case 7:
                case 8:
                case 9:
                default:
                    if (emojiOnlyCount > 9) {
                        cacheType = AnimatedEmojiDrawable.CACHE_TYPE_MESSAGES;
                    }
                    emojiPaint = Theme.chat_msgTextPaintEmoji[5];
                    break;
            }
            int size = (int) (emojiPaint.getTextSize() + dp(large ? 4 : 4));
            if (spans != null && spans.length > 0) {
                for (int a = 0; a < spans.length; a++) {
                    spans[a].replaceFontMetrics(emojiPaint.getFontMetricsInt(), size);
                }
            }
            if (aspans != null && aspans.length > 0) {
                for (int a = 0; a < aspans.length; a++) {
                    aspans[a].replaceFontMetrics(emojiPaint.getFontMetricsInt(), size, cacheType);
                    aspans[a].full = true;
                }
            }
        } else {
            AnimatedEmojiSpan[] aspans = ((Spannable) messageText).getSpans(0, messageText.length(), AnimatedEmojiSpan.class);
            if (aspans != null && aspans.length > 0) {
                totalAnimatedEmojiCount = aspans.length;
                for (int a = 0; a < aspans.length; a++) {
                    aspans[a].replaceFontMetrics(Theme.chat_msgTextPaint.getFontMetricsInt(), (int) (Theme.chat_msgTextPaint.getTextSize() + dp(4)), -1);
                    aspans[a].full = false;
                }
            } else {
                totalAnimatedEmojiCount = 0;
            }
        }
    }

    public TextPaint getTextPaint() {
        if (emojiOnlyCount >= 1 && messageOwner != null && !hasNonEmojiEntities()) {
            boolean large = emojiOnlyCount == animatedEmojiCount;
            switch (Math.max(emojiOnlyCount, animatedEmojiCount)) {
                case 0:
                case 1:
                case 2:
                    return large ? Theme.chat_msgTextPaintEmoji[0] : Theme.chat_msgTextPaintEmoji[2];
                case 3:
                    return large ? Theme.chat_msgTextPaintEmoji[1] : Theme.chat_msgTextPaintEmoji[3];
                case 4:
                    return large ? Theme.chat_msgTextPaintEmoji[2] : Theme.chat_msgTextPaintEmoji[4];
                case 5:
                    return large ? Theme.chat_msgTextPaintEmoji[3] : Theme.chat_msgTextPaintEmoji[5];
                case 6:
                    return large ? Theme.chat_msgTextPaintEmoji[4] : Theme.chat_msgTextPaintEmoji[5];
                case 7:
                case 8:
                case 9:
                default:
                    return Theme.chat_msgTextPaintEmoji[5];
            }
        } else {
            return Theme.chat_msgTextPaint;
        }
    }

    public MessageObject(int accountNum, TLRPC.TL_channelAdminLogEvent event, ArrayList<MessageObject> messageObjects, HashMap<String, ArrayList<MessageObject>> messagesByDays, TLRPC.Chat chat, int[] mid, boolean addToEnd) {
        currentEvent = event;
        currentAccount = accountNum;

        TLRPC.User fromUser = null;
        if (event.user_id > 0) {
            fromUser = MessagesController.getInstance(currentAccount).getUser(event.user_id);
        }

        Calendar rightNow = new GregorianCalendar();
        rightNow.setTimeInMillis((long) (event.date) * 1000);
        int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
        int dateYear = rightNow.get(Calendar.YEAR);
        int dateMonth = rightNow.get(Calendar.MONTH);
        dateKey = String.format("%d_%02d_%02d", dateYear, dateMonth, dateDay);
        dateKeyInt = dateYear + 1000 * dateMonth + 1000 * 100 * dateDay;
        monthKey = String.format("%d_%02d", dateYear, dateMonth);

        TLRPC.Peer peer_id = new TLRPC.TL_peerChannel();
        peer_id.channel_id = chat.id;

        TLRPC.Message message = null;
        ArrayList<TLRPC.MessageEntity> webPageDescriptionEntities = null;
        if (event.action instanceof TLRPC.TL_channelAdminLogEventActionChangeTitle) {
            String title = ((TLRPC.TL_channelAdminLogEventActionChangeTitle) event.action).new_value;
            if (chat.megagroup) {
                messageText = replaceWithLink(formatString("EventLogEditedGroupTitle", R.string.EventLogEditedGroupTitle, title), "un1", fromUser);
            } else {
                messageText = replaceWithLink(formatString("EventLogEditedChannelTitle", R.string.EventLogEditedChannelTitle, title), "un1", fromUser);
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionChangePhoto) {
            TLRPC.TL_channelAdminLogEventActionChangePhoto action = (TLRPC.TL_channelAdminLogEventActionChangePhoto) event.action;
            messageOwner = new TLRPC.TL_messageService();
            if (action.new_photo instanceof TLRPC.TL_photoEmpty) {
                messageOwner.action = new TLRPC.TL_messageActionChatDeletePhoto();
                if (chat.megagroup) {
                    messageText = replaceWithLink(getString(R.string.EventLogRemovedWGroupPhoto), "un1", fromUser);
                } else {
                    messageText = replaceWithLink(getString(R.string.EventLogRemovedChannelPhoto), "un1", fromUser);
                }
            } else {
                messageOwner.action = new TLRPC.TL_messageActionChatEditPhoto();
                messageOwner.action.photo = action.new_photo;

                if (chat.megagroup) {
                    if (isVideoAvatar()) {
                        messageText = replaceWithLink(getString(R.string.EventLogEditedGroupVideo), "un1", fromUser);
                    } else {
                        messageText = replaceWithLink(getString(R.string.EventLogEditedGroupPhoto), "un1", fromUser);
                    }
                } else {
                    if (isVideoAvatar()) {
                        messageText = replaceWithLink(getString(R.string.EventLogEditedChannelVideo), "un1", fromUser);
                    } else {
                        messageText = replaceWithLink(getString(R.string.EventLogEditedChannelPhoto), "un1", fromUser);
                    }
                }
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionParticipantJoin) {
            if (chat.megagroup) {
                messageText = replaceWithLink(getString(R.string.EventLogGroupJoined), "un1", fromUser);
            } else {
                messageText = replaceWithLink(getString(R.string.EventLogChannelJoined), "un1", fromUser);
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionParticipantLeave) {
            messageOwner = new TLRPC.TL_messageService();
            messageOwner.action = new TLRPC.TL_messageActionChatDeleteUser();
            messageOwner.action.user_id = event.user_id;
            if (chat.megagroup) {
                messageText = replaceWithLink(getString(R.string.EventLogLeftGroup), "un1", fromUser);
            } else {
                messageText = replaceWithLink(getString(R.string.EventLogLeftChannel), "un1", fromUser);
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionParticipantInvite) {
            TLRPC.TL_channelAdminLogEventActionParticipantInvite action = (TLRPC.TL_channelAdminLogEventActionParticipantInvite) event.action;
            messageOwner = new TLRPC.TL_messageService();
            messageOwner.action = new TLRPC.TL_messageActionChatAddUser();
            long peerId = getPeerId(action.participant.peer);
            TLObject whoUser;
            if (peerId > 0) {
                whoUser = MessagesController.getInstance(currentAccount).getUser(peerId);
            } else {
                whoUser = MessagesController.getInstance(currentAccount).getChat(-peerId);
            }
            if (messageOwner.from_id instanceof TLRPC.TL_peerUser && peerId == messageOwner.from_id.user_id) {
                if (chat.megagroup) {
                    messageText = replaceWithLink(getString(R.string.EventLogGroupJoined), "un1", fromUser);
                } else {
                    messageText = replaceWithLink(getString(R.string.EventLogChannelJoined), "un1", fromUser);
                }
            } else {
                messageText = replaceWithLink(getString(R.string.EventLogAdded), "un2", whoUser);
                messageText = replaceWithLink(messageText, "un1", fromUser);
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionParticipantToggleAdmin ||
                event.action instanceof TLRPC.TL_channelAdminLogEventActionParticipantToggleBan && ((TLRPC.TL_channelAdminLogEventActionParticipantToggleBan) event.action).prev_participant instanceof TLRPC.TL_channelParticipantAdmin && ((TLRPC.TL_channelAdminLogEventActionParticipantToggleBan) event.action).new_participant instanceof TLRPC.TL_channelParticipant) {
            TLRPC.ChannelParticipant prev_participant;
            TLRPC.ChannelParticipant new_participant;
            if (event.action instanceof TLRPC.TL_channelAdminLogEventActionParticipantToggleAdmin) {
                TLRPC.TL_channelAdminLogEventActionParticipantToggleAdmin action = (TLRPC.TL_channelAdminLogEventActionParticipantToggleAdmin) event.action;
                prev_participant = action.prev_participant;
                new_participant = action.new_participant;
            } else {
                TLRPC.TL_channelAdminLogEventActionParticipantToggleBan action = (TLRPC.TL_channelAdminLogEventActionParticipantToggleBan) event.action;
                prev_participant = action.prev_participant;
                new_participant = action.new_participant;
            }
            messageOwner = new TLRPC.TL_message();
            messageOwner.realId = -1;
            long peerId = MessageObject.getPeerId(prev_participant.peer);
            TLObject whoUser;
            if (peerId > 0) {
                whoUser = MessagesController.getInstance(currentAccount).getUser(peerId);
            } else {
                whoUser = MessagesController.getInstance(currentAccount).getUser(-peerId);
            }
            StringBuilder rights;
            if (!(prev_participant instanceof TLRPC.TL_channelParticipantCreator) && new_participant instanceof TLRPC.TL_channelParticipantCreator) {
                String str = getString(R.string.EventLogChangedOwnership);
                int offset = str.indexOf("%1$s");
                rights = new StringBuilder(String.format(str, getUserName(whoUser, messageOwner.entities, offset)));
            } else {
                TLRPC.TL_chatAdminRights o = prev_participant.admin_rights;
                TLRPC.TL_chatAdminRights n = new_participant.admin_rights;
                if (o == null) {
                    o = new TLRPC.TL_chatAdminRights();
                }
                if (n == null) {
                    n = new TLRPC.TL_chatAdminRights();
                }
                String str;
                if (n.other) {
                    str = getString(R.string.EventLogPromotedNoRights);
                } else {
                    str = getString(R.string.EventLogPromoted);
                }
                int offset = str.indexOf("%1$s");
                rights = new StringBuilder(String.format(str, getUserName(whoUser, messageOwner.entities, offset)));
                rights.append("\n");
                if (!TextUtils.equals(prev_participant.rank, new_participant.rank)) {
                    if (TextUtils.isEmpty(new_participant.rank)) {
                        rights.append('\n').append('-').append(' ');
                        rights.append(getString(R.string.EventLogPromotedRemovedTitle));
                    } else {
                        rights.append('\n').append('+').append(' ');
                        rights.append(formatString("EventLogPromotedTitle", R.string.EventLogPromotedTitle, new_participant.rank));
                    }
                }
                if (o.change_info != n.change_info) {
                    rights.append('\n').append(n.change_info ? '+' : '-').append(' ');
                    rights.append(chat.megagroup ? getString(R.string.EventLogPromotedChangeGroupInfo) : getString(R.string.EventLogPromotedChangeChannelInfo));
                }
                if (!chat.megagroup) {
                    if (o.post_messages != n.post_messages) {
                        rights.append('\n').append(n.post_messages ? '+' : '-').append(' ');
                        rights.append(getString(R.string.EventLogPromotedPostMessages));
                    }
                    if (o.edit_messages != n.edit_messages) {
                        rights.append('\n').append(n.edit_messages ? '+' : '-').append(' ');
                        rights.append(getString(R.string.EventLogPromotedEditMessages));
                    }
                }
                if (o.post_stories != n.post_stories) {
                    rights.append('\n').append(n.post_stories ? '+' : '-').append(' ');
                    rights.append(getString(R.string.EventLogPromotedPostStories));
                }
                if (o.edit_stories != n.edit_stories) {
                    rights.append('\n').append(n.edit_stories ? '+' : '-').append(' ');
                    rights.append(getString(R.string.EventLogPromotedEditStories));
                }
                if (o.delete_stories != n.delete_stories) {
                    rights.append('\n').append(n.delete_stories ? '+' : '-').append(' ');
                    rights.append(getString(R.string.EventLogPromotedDeleteStories));
                }
                if (o.delete_messages != n.delete_messages) {
                    rights.append('\n').append(n.delete_messages ? '+' : '-').append(' ');
                    rights.append(getString(R.string.EventLogPromotedDeleteMessages));
                }
                if (o.add_admins != n.add_admins) {
                    rights.append('\n').append(n.add_admins ? '+' : '-').append(' ');
                    rights.append(getString(R.string.EventLogPromotedAddAdmins));
                }
                if (o.anonymous != n.anonymous) {
                    rights.append('\n').append(n.anonymous ? '+' : '-').append(' ');
                    rights.append(getString(R.string.EventLogPromotedSendAnonymously));
                }
                if (chat.megagroup) {
                    if (o.ban_users != n.ban_users) {
                        rights.append('\n').append(n.ban_users ? '+' : '-').append(' ');
                        rights.append(getString(R.string.EventLogPromotedBanUsers));
                    }
                    if (o.manage_call != n.manage_call) {
                        rights.append('\n').append(n.manage_call ? '+' : '-').append(' ');
                        rights.append(getString(R.string.EventLogPromotedManageCall));
                    }
                }
                if (o.invite_users != n.invite_users) {
                    rights.append('\n').append(n.invite_users ? '+' : '-').append(' ');
                    rights.append(getString(R.string.EventLogPromotedAddUsers));
                }
                if (chat.megagroup) {
                    if (o.pin_messages != n.pin_messages) {
                        rights.append('\n').append(n.pin_messages ? '+' : '-').append(' ');
                        rights.append(getString(R.string.EventLogPromotedPinMessages));
                    }
                    if (o.manage_ranks != n.manage_ranks) {
                        rights.append('\n').append(n.manage_ranks ? '+' : '-').append(' ');
                        rights.append(getString(R.string.EventLogPromotedEditRanks));
                    }
                    if (o.manage_topics != n.manage_topics) {
                        rights.append('\n').append(n.manage_topics ? '+' : '-').append(' ');
                        rights.append(getString(R.string.EventLogPromotedManageTopics));
                    }
                }
                if (o.manage_direct_messages != n.manage_direct_messages) {
                    rights.append('\n').append(n.manage_direct_messages ? '+' : '-').append(' ');
                    rights.append(getString(R.string.EventLogPromotedManageDirect));
                }
                if (o.manage_welcome_messages != n.manage_welcome_messages) {
                    rights.append('\n').append(n.manage_welcome_messages ? '+' : '-').append(' ');
                    rights.append(getString(R.string.EventLogPromotedManageWelcomeMessages));
                }
            }
            messageText = rights.toString();
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionDefaultBannedRights) {
            TLRPC.TL_channelAdminLogEventActionDefaultBannedRights bannedRights = (TLRPC.TL_channelAdminLogEventActionDefaultBannedRights) event.action;
            messageOwner = new TLRPC.TL_message();
            messageOwner.realId = -1;

            TLRPC.TL_chatBannedRights o = bannedRights.prev_banned_rights;
            TLRPC.TL_chatBannedRights n = bannedRights.new_banned_rights;
            StringBuilder rights = new StringBuilder(getString(R.string.EventLogDefaultPermissions));
            boolean added = false;
            if (o == null) {
                o = new TLRPC.TL_chatBannedRights();
            }
            if (n == null) {
                n = new TLRPC.TL_chatBannedRights();
            }
            if (o.send_messages != n.send_messages) {
                rights.append('\n');
                added = true;
                rights.append('\n').append(!n.send_messages ? '+' : '-').append(' ');
                rights.append(getString(R.string.EventLogRestrictedSendMessages));
            }
            if (o.send_stickers != n.send_stickers || o.send_inline != n.send_inline || o.send_gifs != n.send_gifs || o.send_games != n.send_games) {
                if (!added) {
                    rights.append('\n');
                    added = true;
                }
                rights.append('\n').append(!n.send_stickers ? '+' : '-').append(' ');
                rights.append(getString(R.string.EventLogRestrictedSendStickers));
            }
            if (o.send_media != n.send_media) {
                if (!added) {
                    rights.append('\n');
                    added = true;
                }
                rights.append('\n').append(!n.send_media ? '+' : '-').append(' ');
                rights.append(getString(R.string.EventLogRestrictedSendMedia));
            }
            if (o.send_polls != n.send_polls) {
                if (!added) {
                    rights.append('\n');
                    added = true;
                }
                rights.append('\n').append(!n.send_polls ? '+' : '-').append(' ');
                rights.append(getString(R.string.EventLogRestrictedSendPolls));
            }
            if (o.embed_links != n.embed_links) {
                if (!added) {
                    rights.append('\n');
                    added = true;
                }
                rights.append('\n').append(!n.embed_links ? '+' : '-').append(' ');
                rights.append(getString(R.string.EventLogRestrictedSendEmbed));
            }

            if (o.change_info != n.change_info) {
                if (!added) {
                    rights.append('\n');
                    added = true;
                }
                rights.append('\n').append(!n.change_info ? '+' : '-').append(' ');
                rights.append(getString(R.string.EventLogRestrictedChangeInfo));
            }
            if (o.invite_users != n.invite_users) {
                if (!added) {
                    rights.append('\n');
                    added = true;
                }
                rights.append('\n').append(!n.invite_users ? '+' : '-').append(' ');
                rights.append(getString(R.string.EventLogRestrictedInviteUsers));
            }
            if (o.pin_messages != n.pin_messages) {
                if (!added) {
                    rights.append('\n');
                }
                rights.append('\n').append(!n.pin_messages ? '+' : '-').append(' ');
                rights.append(getString(R.string.EventLogRestrictedPinMessages));
            }
            if (o.edit_rank != n.edit_rank) {
                if (!added) {
                    rights.append('\n');
                }
                rights.append('\n').append(!n.edit_rank ? '+' : '-').append(' ');
                rights.append(getString(R.string.EventLogRestrictedEditRank));
            }
            if (o.send_reactions != n.send_reactions) {
                if (!added) {
                    rights.append('\n');
                }
                rights.append('\n').append(!n.send_reactions ? '+' : '-').append(' ');
                rights.append(getString(R.string.EventLogRestrictedSendReactions));
            }
            messageText = rights.toString();
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionParticipantToggleBan) {
            TLRPC.TL_channelAdminLogEventActionParticipantToggleBan action = (TLRPC.TL_channelAdminLogEventActionParticipantToggleBan) event.action;
            messageOwner = new TLRPC.TL_message();
            messageOwner.realId = -1;
            long peerId = getPeerId(action.prev_participant.peer);
            TLObject whoUser;
            if (peerId > 0) {
                whoUser = MessagesController.getInstance(currentAccount).getUser(peerId);
            } else {
                whoUser = MessagesController.getInstance(currentAccount).getChat(-peerId);
            }
            TLRPC.TL_chatBannedRights o = action.prev_participant.banned_rights;
            TLRPC.TL_chatBannedRights n = action.new_participant.banned_rights;
            if (chat.megagroup && (n == null || !n.view_messages || o != null && n.until_date != o.until_date)) {
                StringBuilder rights;
                StringBuilder bannedDuration;
                if (n != null && !AndroidUtilities.isBannedForever(n)) {
                    bannedDuration = new StringBuilder();
                    int duration = n.until_date - event.date;
                    int days = duration / 60 / 60 / 24;
                    duration -= days * 60 * 60 * 24;
                    int hours = duration / 60 / 60;
                    duration -= hours * 60 * 60;
                    int minutes = duration / 60;
                    int count = 0;
                    for (int a = 0; a < 3; a++) {
                        String addStr = null;
                        if (a == 0) {
                            if (days != 0) {
                                addStr = LocaleController.formatPluralString("Days", days);
                                count++;
                            }
                        } else if (a == 1) {
                            if (hours != 0) {
                                addStr = LocaleController.formatPluralString("Hours", hours);
                                count++;
                            }
                        } else {
                            if (minutes != 0) {
                                addStr = LocaleController.formatPluralString("Minutes", minutes);
                                count++;
                            }
                        }
                        if (addStr != null) {
                            if (bannedDuration.length() > 0) {
                                bannedDuration.append(", ");
                            }
                            bannedDuration.append(addStr);
                        }
                        if (count == 2) {
                            break;
                        }
                    }
                } else {
                    bannedDuration = new StringBuilder(getString(R.string.UserRestrictionsUntilForever));
                }
                String str = getString(R.string.EventLogRestrictedUntil);
                int offset = str.indexOf("%1$s");
                rights = new StringBuilder(String.format(str, getUserName(whoUser, messageOwner.entities, offset), bannedDuration.toString()));
                boolean added = false;
                if (o == null) {
                    o = new TLRPC.TL_chatBannedRights();
                }
                if (n == null) {
                    n = new TLRPC.TL_chatBannedRights();
                }
                if (o.view_messages != n.view_messages) {
                    rights.append('\n');
                    added = true;
                    rights.append('\n').append(!n.view_messages ? '+' : '-').append(' ');
                    rights.append(getString(R.string.EventLogRestrictedReadMessages));
                }
                if (o.send_messages != n.send_messages) {
                    if (!added) {
                        rights.append('\n');
                        added = true;
                    }
                    rights.append('\n').append(!n.send_messages ? '+' : '-').append(' ');
                    rights.append(getString(R.string.EventLogRestrictedSendMessages));
                }
                if (o.send_stickers != n.send_stickers ||
                        o.send_inline != n.send_inline ||
                        o.send_gifs != n.send_gifs ||
                        o.send_games != n.send_games) {
                    if (!added) {
                        rights.append('\n');
                        added = true;
                    }
                    rights.append('\n').append(!n.send_stickers ? '+' : '-').append(' ');
                    rights.append(getString(R.string.EventLogRestrictedSendStickers));
                }
                if (o.send_media != n.send_media) {
                    if (!added) {
                        rights.append('\n');
                        added = true;
                    }
                    rights.append('\n').append(!n.send_media ? '+' : '-').append(' ');
                    rights.append(getString(R.string.EventLogRestrictedSendMedia));
                }
                if (o.send_polls != n.send_polls) {
                    if (!added) {
                        rights.append('\n');
                        added = true;
                    }
                    rights.append('\n').append(!n.send_polls ? '+' : '-').append(' ');
                    rights.append(getString(R.string.EventLogRestrictedSendPolls));
                }
                if (o.embed_links != n.embed_links) {
                    if (!added) {
                        rights.append('\n');
                        added = true;
                    }
                    rights.append('\n').append(!n.embed_links ? '+' : '-').append(' ');
                    rights.append(getString(R.string.EventLogRestrictedSendEmbed));
                }

                if (o.change_info != n.change_info) {
                    if (!added) {
                        rights.append('\n');
                        added = true;
                    }
                    rights.append('\n').append(!n.change_info ? '+' : '-').append(' ');
                    rights.append(getString(R.string.EventLogRestrictedChangeInfo));
                }
                if (o.invite_users != n.invite_users) {
                    if (!added) {
                        rights.append('\n');
                        added = true;
                    }
                    rights.append('\n').append(!n.invite_users ? '+' : '-').append(' ');
                    rights.append(getString(R.string.EventLogRestrictedInviteUsers));
                }
                if (o.pin_messages != n.pin_messages) {
                    if (!added) {
                        rights.append('\n');
                    }
                    rights.append('\n').append(!n.pin_messages ? '+' : '-').append(' ');
                    rights.append(getString(R.string.EventLogRestrictedPinMessages));
                }
                if (o.edit_rank != n.edit_rank) {
                    if (!added) {
                        rights.append('\n');
                    }
                    rights.append('\n').append(!n.edit_rank ? '+' : '-').append(' ');
                    rights.append(getString(R.string.EventLogRestrictedEditRank));
                }
                if (o.send_reactions != n.send_reactions) {
                    if (!added) {
                        rights.append('\n');
                    }
                    rights.append('\n').append(!n.send_reactions ? '+' : '-').append(' ');
                    rights.append(getString(R.string.EventLogRestrictedSendReactions));
                }
                messageText = rights.toString();
            } else {
                String str;
                if (n != null && (o == null || n.view_messages)) {
                    str = getString(R.string.EventLogChannelRestricted);
                } else {
                    str = getString(R.string.EventLogChannelUnrestricted);
                }
                int offset = str.indexOf("%1$s");
                messageText = String.format(str, getUserName(whoUser, messageOwner.entities, offset));
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionUpdatePinned) {
            TLRPC.TL_channelAdminLogEventActionUpdatePinned action = (TLRPC.TL_channelAdminLogEventActionUpdatePinned) event.action;
            message = action.message;
            if (fromUser != null && fromUser.id == 136817688 && action.message.fwd_from != null && action.message.fwd_from.from_id instanceof TLRPC.TL_peerChannel) {
                TLRPC.Chat channel = MessagesController.getInstance(currentAccount).getChat(action.message.fwd_from.from_id.channel_id);
                if (action.message instanceof TLRPC.TL_messageEmpty || !action.message.pinned) {
                    messageText = replaceWithLink(getString(R.string.EventLogUnpinnedMessages), "un1", channel);
                } else {
                    messageText = replaceWithLink(getString(R.string.EventLogPinnedMessages), "un1", channel);
                }
            } else {
                if (action.message instanceof TLRPC.TL_messageEmpty || !action.message.pinned) {
                    messageText = replaceWithLink(getString(R.string.EventLogUnpinnedMessages), "un1", fromUser);
                } else {
                    messageText = replaceWithLink(getString(R.string.EventLogPinnedMessages), "un1", fromUser);
                }
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionStopPoll) {
            TLRPC.TL_channelAdminLogEventActionStopPoll action = (TLRPC.TL_channelAdminLogEventActionStopPoll) event.action;
            message = action.message;
            if (getMedia(message) instanceof TLRPC.TL_messageMediaPoll && ((TLRPC.TL_messageMediaPoll) getMedia(message)).poll.quiz) {
                messageText = replaceWithLink(getString(R.string.EventLogStopQuiz), "un1", fromUser);
            } else {
                messageText = replaceWithLink(getString(R.string.EventLogStopPoll), "un1", fromUser);
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionToggleSignatures) {
            if (((TLRPC.TL_channelAdminLogEventActionToggleSignatures) event.action).new_value) {
                messageText = replaceWithLink(getString(R.string.EventLogToggledSignaturesOn), "un1", fromUser);
            } else {
                messageText = replaceWithLink(getString(R.string.EventLogToggledSignaturesOff), "un1", fromUser);
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionParticipantSubExtend) {
            messageText = replaceWithLink(getString(R.string.EventLogSubExtend), "un1", fromUser);
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionToggleSignatureProfiles) {
            if (((TLRPC.TL_channelAdminLogEventActionToggleSignatureProfiles) event.action).value) {
                messageText = replaceWithLink(getString(R.string.EventLogToggledSignaturesProfilesOn), "un1", fromUser);
            } else {
                messageText = replaceWithLink(getString(R.string.EventLogToggledSignaturesProfilesOff), "un1", fromUser);
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionToggleInvites) {
            if (((TLRPC.TL_channelAdminLogEventActionToggleInvites) event.action).new_value) {
                messageText = replaceWithLink(getString(R.string.EventLogToggledInvitesOn), "un1", fromUser);
            } else {
                messageText = replaceWithLink(getString(R.string.EventLogToggledInvitesOff), "un1", fromUser);
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionDeleteMessage) {
            message = ((TLRPC.TL_channelAdminLogEventActionDeleteMessage) event.action).message;
            if (fromUser != null && fromUser.id == MessagesController.getInstance(currentAccount).telegramAntispamUserId) {
                messageText = getString(R.string.EventLogDeletedMessages).replace("un1", UserObject.getUserName(fromUser));
            } else {
                messageText = replaceWithLink(getString(R.string.EventLogDeletedMessages), "un1", fromUser);
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionChangeLinkedChat) {
            long newChatId = ((TLRPC.TL_channelAdminLogEventActionChangeLinkedChat) event.action).new_value;
            long oldChatId = ((TLRPC.TL_channelAdminLogEventActionChangeLinkedChat) event.action).prev_value;
            if (chat.megagroup) {
                if (newChatId == 0) {
                    TLRPC.Chat oldChat = MessagesController.getInstance(currentAccount).getChat(oldChatId);
                    messageText = replaceWithLink(getString(R.string.EventLogRemovedLinkedChannel), "un1", fromUser);
                    messageText = replaceWithLink(messageText, "un2", oldChat);
                } else {
                    TLRPC.Chat newChat = MessagesController.getInstance(currentAccount).getChat(newChatId);
                    messageText = replaceWithLink(getString(R.string.EventLogChangedLinkedChannel), "un1", fromUser);
                    messageText = replaceWithLink(messageText, "un2", newChat);
                }
            } else {
                if (newChatId == 0) {
                    TLRPC.Chat oldChat = MessagesController.getInstance(currentAccount).getChat(oldChatId);
                    messageText = replaceWithLink(getString(R.string.EventLogRemovedLinkedGroup), "un1", fromUser);
                    messageText = replaceWithLink(messageText, "un2", oldChat);
                } else {
                    TLRPC.Chat newChat = MessagesController.getInstance(currentAccount).getChat(newChatId);
                    messageText = replaceWithLink(getString(R.string.EventLogChangedLinkedGroup), "un1", fromUser);
                    messageText = replaceWithLink(messageText, "un2", newChat);
                }
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionTogglePreHistoryHidden) {
            if (((TLRPC.TL_channelAdminLogEventActionTogglePreHistoryHidden) event.action).new_value) {
                messageText = replaceWithLink(getString(R.string.EventLogToggledInvitesHistoryOff), "un1", fromUser);
            } else {
                messageText = replaceWithLink(getString(R.string.EventLogToggledInvitesHistoryOn), "un1", fromUser);
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionChangeAbout) {
            messageText = replaceWithLink(chat.megagroup ? getString(R.string.EventLogEditedGroupDescription) : getString(R.string.EventLogEditedChannelDescription), "un1", fromUser);
            message = new TLRPC.TL_message();
            message.out = false;
            message.unread = false;
            message.from_id = new TLRPC.TL_peerUser();
            message.from_id.user_id = event.user_id;
            message.peer_id = peer_id;
            message.date = event.date;
            message.message = ((TLRPC.TL_channelAdminLogEventActionChangeAbout) event.action).new_value;
            if (!TextUtils.isEmpty(((TLRPC.TL_channelAdminLogEventActionChangeAbout) event.action).prev_value)) {
                message.media = new TLRPC.TL_messageMediaWebPage();
                message.media.webpage = new TLRPC.TL_webPage();
                message.media.webpage.flags = 10;
                message.media.webpage.display_url = "";
                message.media.webpage.url = "";
                message.media.webpage.site_name = getString(R.string.EventLogPreviousGroupDescription);
                message.media.webpage.description = ((TLRPC.TL_channelAdminLogEventActionChangeAbout) event.action).prev_value;
            } else {
                message.media = new TLRPC.TL_messageMediaEmpty();
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionChangeTheme) {
            messageText = replaceWithLink(chat.megagroup ? getString(R.string.EventLogEditedGroupTheme) : getString(R.string.EventLogEditedChannelTheme), "un1", fromUser);
            message = new TLRPC.TL_message();
            message.out = false;
            message.unread = false;
            message.from_id = new TLRPC.TL_peerUser();
            message.from_id.user_id = event.user_id;
            message.peer_id = peer_id;
            message.date = event.date;
            message.message = ((TLRPC.TL_channelAdminLogEventActionChangeTheme) event.action).new_value;
            if (!TextUtils.isEmpty(((TLRPC.TL_channelAdminLogEventActionChangeTheme) event.action).prev_value)) {
                message.media = new TLRPC.TL_messageMediaWebPage();
                message.media.webpage = new TLRPC.TL_webPage();
                message.media.webpage.flags = 10;
                message.media.webpage.display_url = "";
                message.media.webpage.url = "";
                message.media.webpage.site_name = getString(R.string.EventLogPreviousGroupTheme);
                message.media.webpage.description = ((TLRPC.TL_channelAdminLogEventActionChangeTheme) event.action).prev_value;
            } else {
                message.media = new TLRPC.TL_messageMediaEmpty();
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionChangeUsername) {
            String newLink = ((TLRPC.TL_channelAdminLogEventActionChangeUsername) event.action).new_value;
            if (!TextUtils.isEmpty(newLink)) {
                messageText = replaceWithLink(chat.megagroup ? getString(R.string.EventLogChangedGroupLink) : getString(R.string.EventLogChangedChannelLink), "un1", fromUser);
            } else {
                messageText = replaceWithLink(chat.megagroup ? getString(R.string.EventLogRemovedGroupLink) : getString(R.string.EventLogRemovedChannelLink), "un1", fromUser);
            }
            message = new TLRPC.TL_message();
            message.out = false;
            message.unread = false;
            message.from_id = new TLRPC.TL_peerUser();
            message.from_id.user_id = event.user_id;
            message.peer_id = peer_id;
            message.date = event.date;
            if (!TextUtils.isEmpty(newLink)) {
                message.message = "https://" + MessagesController.getInstance(currentAccount).linkPrefix + "/" + newLink;
            } else {
                message.message = "";
            }
            TLRPC.TL_messageEntityUrl url = new TLRPC.TL_messageEntityUrl();
            url.offset = 0;
            url.length = message.message.length();
            message.entities.add(url);
            if (!TextUtils.isEmpty(((TLRPC.TL_channelAdminLogEventActionChangeUsername) event.action).prev_value)) {
                message.media = new TLRPC.TL_messageMediaWebPage();
                message.media.webpage = new TLRPC.TL_webPage();
                message.media.webpage.flags = 10;
                message.media.webpage.display_url = "";
                message.media.webpage.url = "";
                message.media.webpage.site_name = getString(R.string.EventLogPreviousLink);
                message.media.webpage.description = "https://" + MessagesController.getInstance(currentAccount).linkPrefix + "/" + ((TLRPC.TL_channelAdminLogEventActionChangeUsername) event.action).prev_value;
            } else {
                message.media = new TLRPC.TL_messageMediaEmpty();
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionEditMessage) {
            message = new TLRPC.TL_message();
            message.out = false;
            message.unread = false;
            message.peer_id = peer_id;
            message.date = event.date;
            TLRPC.Message newMessage = ((TLRPC.TL_channelAdminLogEventActionEditMessage) event.action).new_message;
            TLRPC.Message oldMessage = ((TLRPC.TL_channelAdminLogEventActionEditMessage) event.action).prev_message;
            if (oldMessage != null) {
                message.reply_to = oldMessage.reply_to;
                message.id = oldMessage.id;
            } else if (newMessage != null) {
                message.reply_to = newMessage.reply_to;
                message.id = newMessage.id;
            }
            if (newMessage != null && newMessage.from_id != null) {
                message.from_id = newMessage.from_id;
            } else {
                message.from_id = new TLRPC.TL_peerUser();
                message.from_id.user_id = event.user_id;
            }
            if (getMedia(newMessage) != null && !(getMedia(newMessage) instanceof TLRPC.TL_messageMediaEmpty) && !(getMedia(newMessage) instanceof TLRPC.TL_messageMediaWebPage)/* && TextUtils.isEmpty(newMessage.message)*/) {
                boolean changedCaption;
                boolean changedMedia;
                boolean addedMedia = false;
                if (!TextUtils.equals(newMessage.message, oldMessage.message)) {
                    changedCaption = true;
                } else {
                    changedCaption = false;
                }
                TLRPC.MessageMedia newMedia = getMedia(newMessage);
                TLRPC.MessageMedia oldMedia = getMedia(oldMessage);
                if (oldMedia == null) {
                    addedMedia = true;
                    changedMedia = false;
                } else if (
                    newMedia.getClass() != oldMedia.getClass() ||
                    newMedia.photo != null && oldMedia.photo != null && newMedia.photo.id != oldMedia.photo.id ||
                    newMedia.document != null && oldMedia.document != null && getMedia(newMessage).document.id != oldMedia.document.id) {
                    addedMedia = false;
                    changedMedia = true;
                } else {
                    addedMedia = false;
                    changedMedia = false;
                }
                if (addedMedia) {
                    messageText = replaceWithLink(getString(R.string.EventLogAddedMedia), "un1", fromUser);
                } else if (changedMedia && changedCaption) {
                    messageText = replaceWithLink(getString(R.string.EventLogEditedMediaCaption), "un1", fromUser);
                } else if (changedCaption) {
                    messageText = replaceWithLink(getString(R.string.EventLogEditedCaption), "un1", fromUser);
                } else {
                    messageText = replaceWithLink(getString(R.string.EventLogEditedMedia), "un1", fromUser);
                }
                message.media = getMedia(newMessage);
                if (changedCaption) {
                    message.media.webpage = new TLRPC.TL_webPage();
                    message.media.webpage.site_name = getString(R.string.EventLogOriginalCaption);
                    if (TextUtils.isEmpty(oldMessage.message)) {
                        message.media.webpage.description = getString(R.string.EventLogOriginalCaptionEmpty);
                    } else {
                        message.media.webpage.description = oldMessage.message;
                        webPageDescriptionEntities = oldMessage.entities;
                    }
                }
            } else {
                messageText = replaceWithLink(getString(R.string.EventLogEditedMessages), "un1", fromUser);
                if (newMessage.action instanceof TLRPC.TL_messageActionGroupCall) {
                    message = newMessage;
                    message.media = new TLRPC.TL_messageMediaEmpty();
                } else {
                    message.message = newMessage.message;
                    message.entities = newMessage.entities;
                    message.media = new TLRPC.TL_messageMediaWebPage();
                    message.media.webpage = new TLRPC.TL_webPage();
                    message.media.webpage.site_name = getString(R.string.EventLogOriginalMessages);
                    if (TextUtils.isEmpty(oldMessage.message)) {
                        message.media.webpage.description = getString(R.string.EventLogOriginalCaptionEmpty);
                    } else {
                        message.media.webpage.description = oldMessage.message;
                        webPageDescriptionEntities = oldMessage.entities;
                    }
                }
            }
            message.reply_markup = newMessage.reply_markup;
            if (message.media.webpage != null) {
                message.media.webpage.flags = 10;
                message.media.webpage.display_url = "";
                message.media.webpage.url = "";
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionChangeEmojiStickerSet) {
            TLRPC.InputStickerSet newPack = ((TLRPC.TL_channelAdminLogEventActionChangeEmojiStickerSet) event.action).new_stickerset;
            TLRPC.InputStickerSet oldPack = ((TLRPC.TL_channelAdminLogEventActionChangeEmojiStickerSet) event.action).new_stickerset;
            if (newPack == null || newPack instanceof TLRPC.TL_inputStickerSetEmpty) {
                messageText = replaceWithLink(getString(R.string.EventLogRemovedEmojiPack), "un1", fromUser);
            } else {
                messageText = replaceWithLink(getString(R.string.EventLogChangedEmojiPack), "un1", fromUser);
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionChangeStickerSet) {
            TLRPC.InputStickerSet newStickerset = ((TLRPC.TL_channelAdminLogEventActionChangeStickerSet) event.action).new_stickerset;
            TLRPC.InputStickerSet oldStickerset = ((TLRPC.TL_channelAdminLogEventActionChangeStickerSet) event.action).new_stickerset;
            if (newStickerset == null || newStickerset instanceof TLRPC.TL_inputStickerSetEmpty) {
                messageText = replaceWithLink(getString(R.string.EventLogRemovedStickersSet), "un1", fromUser);
            } else {
                messageText = replaceWithLink(getString(R.string.EventLogChangedStickersSet), "un1", fromUser);
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionChangeLocation) {
            TLRPC.TL_channelAdminLogEventActionChangeLocation location = (TLRPC.TL_channelAdminLogEventActionChangeLocation) event.action;
            if (location.new_value instanceof TLRPC.TL_channelLocationEmpty) {
                messageText = replaceWithLink(getString(R.string.EventLogRemovedLocation), "un1", fromUser);
            } else {
                TLRPC.TL_channelLocation channelLocation = (TLRPC.TL_channelLocation) location.new_value;
                messageText = replaceWithLink(formatString("EventLogChangedLocation", R.string.EventLogChangedLocation, channelLocation.address), "un1", fromUser);
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionToggleSlowMode) {
            TLRPC.TL_channelAdminLogEventActionToggleSlowMode slowMode = (TLRPC.TL_channelAdminLogEventActionToggleSlowMode) event.action;
            if (slowMode.new_value == 0) {
                messageText = replaceWithLink(getString(R.string.EventLogToggledSlowmodeOff), "un1", fromUser);
            } else {
                String string;
                if (slowMode.new_value < 60) {
                    string = LocaleController.formatPluralString("Seconds", slowMode.new_value);
                } else if (slowMode.new_value < 60 * 60) {
                    string = LocaleController.formatPluralString("Minutes", slowMode.new_value / 60);
                } else {
                    string = LocaleController.formatPluralString("Hours", slowMode.new_value / 60 / 60);
                }
                messageText = replaceWithLink(formatString(R.string.EventLogToggledSlowmodeOn, string), "un1", fromUser);
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionToggleAutotranslation) {
            final TLRPC.TL_channelAdminLogEventActionToggleAutotranslation action = (TLRPC.TL_channelAdminLogEventActionToggleAutotranslation) event.action;
            messageText = replaceWithLink(getString(action.new_value ? R.string.EventLogToggledAutotranslationOn : R.string.EventLogToggledAutotranslationOff), "un1", fromUser);
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionStartGroupCall) {
            if (ChatObject.isChannel(chat) && (!chat.megagroup || chat.gigagroup)) {
                messageText = replaceWithLink(getString(R.string.EventLogStartedLiveStream), "un1", fromUser);
            } else {
                messageText = replaceWithLink(getString(R.string.EventLogStartedVoiceChat), "un1", fromUser);
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionDiscardGroupCall) {
            if (ChatObject.isChannel(chat) && (!chat.megagroup || chat.gigagroup)) {
                messageText = replaceWithLink(getString(R.string.EventLogEndedLiveStream), "un1", fromUser);
            } else {
                messageText = replaceWithLink(getString(R.string.EventLogEndedVoiceChat), "un1", fromUser);
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionParticipantMute) {
            TLRPC.TL_channelAdminLogEventActionParticipantMute action = (TLRPC.TL_channelAdminLogEventActionParticipantMute) event.action;
            long id = getPeerId(action.participant.peer);
            TLObject object;
            if (id > 0) {
                object = MessagesController.getInstance(currentAccount).getUser(id);
            } else {
                object = MessagesController.getInstance(currentAccount).getChat(-id);
            }
            messageText = replaceWithLink(getString(R.string.EventLogVoiceChatMuted), "un1", fromUser);
            messageText = replaceWithLink(messageText, "un2", object);
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionParticipantUnmute) {
            TLRPC.TL_channelAdminLogEventActionParticipantUnmute action = (TLRPC.TL_channelAdminLogEventActionParticipantUnmute) event.action;
            long id = getPeerId(action.participant.peer);
            TLObject object;
            if (id > 0) {
                object = MessagesController.getInstance(currentAccount).getUser(id);
            } else {
                object = MessagesController.getInstance(currentAccount).getChat(-id);
            }
            messageText = replaceWithLink(getString(R.string.EventLogVoiceChatUnmuted), "un1", fromUser);
            messageText = replaceWithLink(messageText, "un2", object);
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionToggleGroupCallSetting) {
            TLRPC.TL_channelAdminLogEventActionToggleGroupCallSetting action = (TLRPC.TL_channelAdminLogEventActionToggleGroupCallSetting) event.action;
            if (action.join_muted) {
                messageText = replaceWithLink(getString(R.string.EventLogVoiceChatNotAllowedToSpeak), "un1", fromUser);
            } else {
                messageText = replaceWithLink(getString(R.string.EventLogVoiceChatAllowedToSpeak), "un1", fromUser);
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionParticipantJoinByInvite) {
            TLRPC.TL_channelAdminLogEventActionParticipantJoinByInvite action = (TLRPC.TL_channelAdminLogEventActionParticipantJoinByInvite) event.action;
            if (action.via_chatlist) {
                messageText = replaceWithLink(getString(ChatObject.isChannelAndNotMegaGroup(chat) ? R.string.ActionInviteChannelUserFolder : R.string.ActionInviteUserFolder), "un1", fromUser);
            } else {
                messageText = replaceWithLink(getString(ChatObject.isChannelAndNotMegaGroup(chat) ? R.string.ActionInviteChannelUser : R.string.ActionInviteUser), "un1", fromUser);
            }
            if (action.invite != null && !TextUtils.isEmpty(action.invite.link)) {
                messageText = TextUtils.concat(messageText, " ", action.invite.link);
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionToggleNoForwards) {
            TLRPC.TL_channelAdminLogEventActionToggleNoForwards action = (TLRPC.TL_channelAdminLogEventActionToggleNoForwards) event.action;
            boolean isChannel = ChatObject.isChannel(chat) && !chat.megagroup;
            if (action.new_value) {
                if (isChannel) {
                    messageText = replaceWithLink(getString(R.string.ActionForwardsRestrictedChannel), "un1", fromUser);
                } else {
                    messageText = replaceWithLink(getString(R.string.ActionForwardsRestrictedGroup), "un1", fromUser);
                }
            } else {
                if (isChannel) {
                    messageText = replaceWithLink(getString(R.string.ActionForwardsEnabledChannel), "un1", fromUser);
                } else {
                    messageText = replaceWithLink(getString(R.string.ActionForwardsEnabledGroup), "un1", fromUser);
                }
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionExportedInviteDelete) {
            TLRPC.TL_channelAdminLogEventActionExportedInviteDelete action = (TLRPC.TL_channelAdminLogEventActionExportedInviteDelete) event.action;
            messageText = replaceWithLink(formatString(R.string.ActionDeletedInviteLinkClickable), "un1", fromUser);
            messageText = replaceWithLink(messageText, "un2", action.invite);
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionExportedInviteRevoke) {
            TLRPC.TL_channelAdminLogEventActionExportedInviteRevoke action = (TLRPC.TL_channelAdminLogEventActionExportedInviteRevoke) event.action;
            messageText = replaceWithLink(formatString(R.string.ActionRevokedInviteLinkClickable, action.invite.link), "un1", fromUser);
            messageText = replaceWithLink(messageText, "un2", action.invite);
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionExportedInviteEdit) {
            TLRPC.TL_channelAdminLogEventActionExportedInviteEdit action = (TLRPC.TL_channelAdminLogEventActionExportedInviteEdit) event.action;
            if (action.prev_invite.link != null && action.prev_invite.link.equals(action.new_invite.link)) {
                messageText = replaceWithLink(formatString(R.string.ActionEditedInviteLinkToSameClickable), "un1", fromUser);
            } else {
                messageText = replaceWithLink(formatString(R.string.ActionEditedInviteLinkClickable), "un1", fromUser);
            }
            messageText = replaceWithLink(messageText, "un2", action.prev_invite);
            messageText = replaceWithLink(messageText, "un3", action.new_invite);
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionParticipantVolume) {
            TLRPC.TL_channelAdminLogEventActionParticipantVolume action = (TLRPC.TL_channelAdminLogEventActionParticipantVolume) event.action;
            long id = getPeerId(action.participant.peer);
            TLObject object;
            if (id > 0) {
                object = MessagesController.getInstance(currentAccount).getUser(id);
            } else {
                object = MessagesController.getInstance(currentAccount).getChat(-id);
            }
            double vol = ChatObject.getParticipantVolume(action.participant) / 100.0;
            messageText = replaceWithLink(formatString("ActionVolumeChanged", R.string.ActionVolumeChanged, (int) (vol > 0 ? Math.max(vol, 1) : 0)), "un1", fromUser);
            messageText = replaceWithLink(messageText, "un2", object);
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionChangeHistoryTTL) {
            TLRPC.TL_channelAdminLogEventActionChangeHistoryTTL action = (TLRPC.TL_channelAdminLogEventActionChangeHistoryTTL) event.action;
            if (!chat.megagroup) {
                if (action.new_value != 0) {
                    messageText = formatString("ActionTTLChannelChanged", R.string.ActionTTLChannelChanged, LocaleController.formatTTLString(action.new_value));
                } else {
                    messageText = getString(R.string.ActionTTLChannelDisabled);
                }
            } else if (action.new_value == 0) {
                messageText = replaceWithLink(getString(R.string.ActionTTLDisabled), "un1", fromUser);
            } else {
                String time;
                if (action.new_value > 24 * 60 * 60) {
                    time = LocaleController.formatPluralString("Days", action.new_value / (24 * 60 * 60));
                } else if (action.new_value >= 60 * 60) {
                    time = LocaleController.formatPluralString("Hours", action.new_value / (60 * 60));
                } else if (action.new_value >= 60) {
                    time = LocaleController.formatPluralString("Minutes", action.new_value / 60);
                } else {
                    time = LocaleController.formatPluralString("Seconds", action.new_value);
                }
                messageText = replaceWithLink(formatString(R.string.ActionTTLChanged, time), "un1", fromUser);
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionParticipantJoinByRequest) {
            TLRPC.TL_channelAdminLogEventActionParticipantJoinByRequest action = (TLRPC.TL_channelAdminLogEventActionParticipantJoinByRequest) event.action;
            if (action.invite instanceof TLRPC.TL_chatInviteExported && "https://t.me/+PublicChat".equals(((TLRPC.TL_chatInviteExported) action.invite).link) ||
                    action.invite instanceof TLRPC.TL_chatInvitePublicJoinRequests) {
                messageText = replaceWithLink(getString(R.string.JoinedViaRequestApproved), "un1", fromUser);
                messageText = replaceWithLink(messageText, "un2", MessagesController.getInstance(currentAccount).getUser(action.approved_by));
            } else {
                messageText = replaceWithLink(getString(R.string.JoinedViaInviteLinkApproved), "un1", fromUser);
                messageText = replaceWithLink(messageText, "un2", action.invite);
                messageText = replaceWithLink(messageText, "un3", MessagesController.getInstance(currentAccount).getUser(action.approved_by));
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionSendMessage) {
            message = ((TLRPC.TL_channelAdminLogEventActionSendMessage) event.action).message;
            messageText = replaceWithLink(getString(R.string.EventLogSendMessages), "un1", fromUser);
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionParticipantEditRank) {
            final TLRPC.TL_channelAdminLogEventActionParticipantEditRank action = (TLRPC.TL_channelAdminLogEventActionParticipantEditRank) event.action;
            if (action.user_id == event.user_id) {
                if (!TextUtils.isEmpty(action.prev_rank) && !TextUtils.isEmpty(action.new_rank)) {
                    messageText = formatString(R.string.EventLogRankSelfEdit, action.prev_rank, action.new_rank);
                    messageText = replaceWithLink(messageText, "un1", fromUser);
                } else if (TextUtils.isEmpty(action.prev_rank) && !TextUtils.isEmpty(action.new_rank)) {
                    messageText = formatString(R.string.EventLogRankSelfAdd, action.new_rank);
                    messageText = replaceWithLink(messageText, "un1", fromUser);
                } else {
                    messageText = formatString(R.string.EventLogRankSelfRemove, action.prev_rank);
                    messageText = replaceWithLink(messageText, "un1", fromUser);
                }
            } else {
                final TLRPC.User ofUser = MessagesController.getInstance(currentAccount).getUser(action.user_id);
                if (!TextUtils.isEmpty(action.prev_rank) && !TextUtils.isEmpty(action.new_rank)) {
                    messageText = formatString(R.string.EventLogRankEdit, action.prev_rank, action.new_rank);
                    messageText = replaceWithLink(messageText, "un1", fromUser);
                    messageText = replaceWithLink(messageText, "un2", ofUser);
                } else if (TextUtils.isEmpty(action.prev_rank) && !TextUtils.isEmpty(action.new_rank)) {
                    messageText = formatString(R.string.EventLogRankAdd, action.new_rank);
                    messageText = replaceWithLink(messageText, "un1", fromUser);
                    messageText = replaceWithLink(messageText, "un2", ofUser);
                } else {
                    messageText = formatString(R.string.EventLogRankRemove, action.prev_rank);
                    messageText = replaceWithLink(messageText, "un1", fromUser);
                    messageText = replaceWithLink(messageText, "un2", ofUser);
                }
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionChangeAvailableReactions) {
            TLRPC.TL_channelAdminLogEventActionChangeAvailableReactions eventActionChangeAvailableReactions = (TLRPC.TL_channelAdminLogEventActionChangeAvailableReactions) event.action;
            boolean customReactionsChanged = eventActionChangeAvailableReactions.prev_value instanceof TLRPC.TL_chatReactionsSome
                    && eventActionChangeAvailableReactions.new_value instanceof TLRPC.TL_chatReactionsSome;
            CharSequence newReactions = getStringFrom(eventActionChangeAvailableReactions.new_value);
            String newStr = "**new**";
            String oldStr = "**old**";
            if (customReactionsChanged) {
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(replaceWithLink(formatString(R.string.ActionReactionsChangedList, newStr), "un1", fromUser));
                int i = spannableStringBuilder.toString().indexOf(newStr);
                if (i > 0) {
                    spannableStringBuilder.replace(i, i + newStr.length(), newReactions);
                }
                messageText = spannableStringBuilder;
            } else {
                CharSequence oldReactions = getStringFrom(eventActionChangeAvailableReactions.prev_value);
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(replaceWithLink(formatString(R.string.ActionReactionsChanged, oldStr, newStr), "un1", fromUser));
                int i = spannableStringBuilder.toString().indexOf(oldStr);
                if (i > 0) {
                    spannableStringBuilder.replace(i, i + oldStr.length(), oldReactions);
                }
                i = spannableStringBuilder.toString().indexOf(newStr);
                if (i > 0) {
                    spannableStringBuilder.replace(i, i + newStr.length(), newReactions);
                }
                messageText = spannableStringBuilder;
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionChangeUsernames) {
            TLRPC.TL_channelAdminLogEventActionChangeUsernames log = (TLRPC.TL_channelAdminLogEventActionChangeUsernames) event.action;

            ArrayList<String> oldUsernames = log.prev_value;
            ArrayList<String> newUsernames = log.new_value;

            messageText = null;

            if (oldUsernames != null && newUsernames != null) {
                if (newUsernames.size() + 1 == oldUsernames.size()) {
                    String removed = null;
                    for (int i = 0; i < oldUsernames.size(); ++i) {
                        String username = oldUsernames.get(i);
                        if (!newUsernames.contains(username)) {
                            if (removed == null) {
                                removed = username;
                            } else {
                                removed = null;
                                break;
                            }
                        }
                    }
                    if (removed != null) {
                        messageText = replaceWithLink(
                            formatString("EventLogDeactivatedUsername", R.string.EventLogDeactivatedUsername, "@" + removed),
                            "un1", fromUser
                        );
                    }
                } else if (oldUsernames.size() + 1 == newUsernames.size()) {
                    String added = null;
                    for (int i = 0; i < newUsernames.size(); ++i) {
                        String username = newUsernames.get(i);
                        if (!oldUsernames.contains(username)) {
                            if (added == null) {
                                added = username;
                            } else {
                                added = null;
                                break;
                            }
                        }
                    }
                    if (added != null) {
                        messageText = replaceWithLink(
                            formatString("EventLogActivatedUsername", R.string.EventLogActivatedUsername, "@" + added),
                            "un1", fromUser
                        );
                    }
                }
            }

            if (messageText == null) {
                messageText = replaceWithLink(
                    formatString("EventLogChangeUsernames", R.string.EventLogChangeUsernames, getUsernamesString(oldUsernames), getUsernamesString(newUsernames)),
                    "un1", fromUser
                );
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionToggleForum) {
            TLRPC.TL_channelAdminLogEventActionToggleForum toggleForum = (TLRPC.TL_channelAdminLogEventActionToggleForum) event.action;
            if (toggleForum.new_value) {
                messageText = replaceWithLink(
                        formatString("EventLogSwitchToForum", R.string.EventLogSwitchToForum),
                        "un1", fromUser
                );
            } else {
                messageText = replaceWithLink(
                        formatString("EventLogSwitchToGroup", R.string.EventLogSwitchToGroup),
                        "un1", fromUser
                );
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionCreateTopic) {
            TLRPC.TL_channelAdminLogEventActionCreateTopic createTopic = (TLRPC.TL_channelAdminLogEventActionCreateTopic) event.action;
            messageText = replaceWithLink(
                    formatString("EventLogCreateTopic", R.string.EventLogCreateTopic),
                    "un1", fromUser
            );
            messageText = replaceWithLink(messageText, "un2", createTopic.topic);
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionEditTopic) {
            TLRPC.TL_channelAdminLogEventActionEditTopic editTopic = (TLRPC.TL_channelAdminLogEventActionEditTopic) event.action;
            if (
                editTopic.prev_topic instanceof TLRPC.TL_forumTopic && editTopic.new_topic instanceof TLRPC.TL_forumTopic &&
                ((TLRPC.TL_forumTopic) editTopic.prev_topic).hidden != ((TLRPC.TL_forumTopic) editTopic.new_topic).hidden
            ) {
                String text = ((TLRPC.TL_forumTopic) editTopic.new_topic).hidden ? getString(R.string.TopicHidden2) : getString(R.string.TopicShown2);
                messageText = replaceWithLink(text, "%s", fromUser);
            } else if (
                editTopic.prev_topic instanceof TLRPC.TL_forumTopic && editTopic.new_topic instanceof TLRPC.TL_forumTopic &&
                ((TLRPC.TL_forumTopic) editTopic.prev_topic).closed != ((TLRPC.TL_forumTopic) editTopic.new_topic).closed
            ) {
                if (((TLRPC.TL_forumTopic) editTopic.new_topic).closed) {
                    messageText = replaceWithLink(getString(R.string.EventLogClosedTopic), "%s", fromUser);
                } else {
                    messageText = replaceWithLink(getString(R.string.EventLogReopenedTopic), "%s", fromUser);
                }
                messageText = replaceWithLink(messageText, "un2", editTopic.new_topic);
            } else {
                messageText = replaceWithLink(
                    getString(R.string.EventLogEditTopic),
                    "un1", fromUser
                );
                messageText = replaceWithLink(messageText, "un2", editTopic.prev_topic);
                messageText = replaceWithLink(messageText, "un3", editTopic.new_topic);
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionDeleteTopic) {
            TLRPC.TL_channelAdminLogEventActionDeleteTopic deleteTopic = (TLRPC.TL_channelAdminLogEventActionDeleteTopic) event.action;
            messageText = replaceWithLink(
                    getString(R.string.EventLogDeleteTopic),
                    "un1", fromUser
            );
            messageText = replaceWithLink(messageText, "un2", deleteTopic.topic);
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionPinTopic) {
            TLRPC.TL_channelAdminLogEventActionPinTopic pinTopic = (TLRPC.TL_channelAdminLogEventActionPinTopic) event.action;
            if (pinTopic.new_topic instanceof TLRPC.TL_forumTopic && ((TLRPC.TL_forumTopic)pinTopic.new_topic).pinned) {
                messageText = replaceWithLink(
                        formatString("EventLogPinTopic", R.string.EventLogPinTopic),
                        "un1", fromUser
                );
                messageText = replaceWithLink(messageText, "un2", pinTopic.new_topic);
            } else {
                messageText = replaceWithLink(
                        formatString("EventLogUnpinTopic", R.string.EventLogUnpinTopic),
                        "un1", fromUser
                );
                messageText = replaceWithLink(messageText, "un2", pinTopic.new_topic);
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionToggleAntiSpam) {
            TLRPC.TL_channelAdminLogEventActionToggleAntiSpam action = (TLRPC.TL_channelAdminLogEventActionToggleAntiSpam) event.action;
            messageText = replaceWithLink(
                action.new_value ?
                    getString(R.string.EventLogEnabledAntiSpam) :
                    getString(R.string.EventLogDisabledAntiSpam),
                "un1",
                fromUser
            );
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionChangeColor) {
            boolean isChannel = ChatObject.isChannelAndNotMegaGroup(chat);
            TLRPC.TL_channelAdminLogEventActionChangeColor action = (TLRPC.TL_channelAdminLogEventActionChangeColor) event.action;
            messageText = replaceWithLink(formatString(isChannel ? R.string.EventLogChangedColor : R.string.EventLogChangedColorGroup, AvatarDrawable.colorName(action.prev_value).toLowerCase(), AvatarDrawable.colorName(action.new_value).toLowerCase()), "un1", fromUser);
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionChangePeerColor) {
            boolean isChannel = ChatObject.isChannelAndNotMegaGroup(chat);
            TLRPC.TL_channelAdminLogEventActionChangePeerColor action = (TLRPC.TL_channelAdminLogEventActionChangePeerColor) event.action;
            SpannableStringBuilder ssb = new SpannableStringBuilder(getString(isChannel ? R.string.EventLogChangedPeerColorIcon : R.string.EventLogChangedPeerColorIconGroup));

            SpannableStringBuilder prev = new SpannableStringBuilder();
            if ((action.prev_value.flags & 1) != 0) {
                prev.append("c");
                prev.setSpan(new PeerColorActivity.PeerColorSpan(false, currentAccount, action.prev_value.color).setSize(dp(18)), prev.length() - 1, prev.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if ((action.prev_value.flags & 2) != 0) {
                if (prev.length() > 0)
                    prev.append(", ");
                prev.append("e");
                prev.setSpan(new AnimatedEmojiSpan(action.prev_value.background_emoji_id, Theme.chat_actionTextPaint.getFontMetricsInt()), prev.length() - 1, prev.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (prev.length() == 0) {
                prev.append(getString(R.string.EventLogEmojiNone));
            }

            SpannableStringBuilder next = new SpannableStringBuilder();
            if ((action.new_value.flags & 1) != 0) {
                next.append("c");
                next.setSpan(new PeerColorActivity.PeerColorSpan(false, currentAccount, action.new_value.color).setSize(dp(18)), next.length() - 1, next.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if ((action.new_value.flags & 2) != 0) {
                if (next.length() > 0)
                    next.append(", ");
                next.append("e");
                next.setSpan(new AnimatedEmojiSpan(action.new_value.background_emoji_id, Theme.chat_actionTextPaint.getFontMetricsInt()), next.length() - 1, next.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (next.length() == 0) {
                next.append(getString(R.string.EventLogEmojiNone));
            }

            ssb = AndroidUtilities.replaceCharSequence("%1$s", ssb, prev);
            ssb = AndroidUtilities.replaceCharSequence("%2$s", ssb, next);

            messageText = replaceWithLink(ssb, "un1", fromUser);
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionChangeProfilePeerColor) {
            boolean isChannel = ChatObject.isChannelAndNotMegaGroup(chat);
            TLRPC.TL_channelAdminLogEventActionChangeProfilePeerColor action = (TLRPC.TL_channelAdminLogEventActionChangeProfilePeerColor) event.action;
            SpannableStringBuilder ssb = new SpannableStringBuilder(getString(isChannel ? R.string.EventLogChangedProfileColorIcon : R.string.EventLogChangedProfileColorIconGroup));

            SpannableStringBuilder prev = new SpannableStringBuilder();
            if ((action.prev_value.flags & 1) != 0) {
                prev.append("c");
                prev.setSpan(new PeerColorActivity.PeerColorSpan(true, currentAccount, action.prev_value.color).setSize(dp(18)), prev.length() - 1, prev.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if ((action.prev_value.flags & 2) != 0) {
                if (prev.length() > 0)
                    prev.append(", ");
                prev.append("e");
                prev.setSpan(new AnimatedEmojiSpan(action.prev_value.background_emoji_id, Theme.chat_actionTextPaint.getFontMetricsInt()), prev.length() - 1, prev.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (prev.length() == 0) {
                prev.append(getString(R.string.EventLogEmojiNone));
            }

            SpannableStringBuilder next = new SpannableStringBuilder();
            if ((action.new_value.flags & 1) != 0) {
                next.append("c");
                next.setSpan(new PeerColorActivity.PeerColorSpan(true, currentAccount, action.new_value.color).setSize(dp(18)), next.length() - 1, next.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if ((action.new_value.flags & 2) != 0) {
                if (next.length() > 0)
                    next.append(", ");
                next.append("e");
                next.setSpan(new AnimatedEmojiSpan(action.new_value.background_emoji_id, Theme.chat_actionTextPaint.getFontMetricsInt()), next.length() - 1, next.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (next.length() == 0) {
                next.append(getString(R.string.EventLogEmojiNone));
            }

            ssb = AndroidUtilities.replaceCharSequence("%1$s", ssb, prev);
            ssb = AndroidUtilities.replaceCharSequence("%2$s", ssb, next);

            messageText = replaceWithLink(ssb, "un1", fromUser);
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionChangeEmojiStatus) {
            boolean isChannel = ChatObject.isChannelAndNotMegaGroup(chat);
            TLRPC.TL_channelAdminLogEventActionChangeEmojiStatus action = (TLRPC.TL_channelAdminLogEventActionChangeEmojiStatus) event.action;

            boolean prevNone = false;
            SpannableString prev;
            if (action.prev_value instanceof TLRPC.TL_emojiStatusEmpty) {
                prev = new SpannableString(getString(R.string.EventLogEmojiNone));
                prevNone = true;
            } else {
                prev = new SpannableString("e");
                prev.setSpan(new AnimatedEmojiSpan(DialogObject.getEmojiStatusDocumentId(action.prev_value), Theme.chat_actionTextPaint.getFontMetricsInt()), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            final int until = DialogObject.getEmojiStatusUntil(action.new_value);

            SpannableString next;
            if (action.new_value instanceof TLRPC.TL_emojiStatusEmpty) {
                next = new SpannableString(getString(R.string.EventLogEmojiNone));
            } else {
                next = new SpannableString("e");
                next.setSpan(new AnimatedEmojiSpan(DialogObject.getEmojiStatusDocumentId(action.new_value), Theme.chat_actionTextPaint.getFontMetricsInt()), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            SpannableStringBuilder ssb = new SpannableStringBuilder(getString(
                prevNone ? (
                    until != 0 ? (isChannel ? R.string.EventLogChangedEmojiStatusFor : R.string.EventLogChangedEmojiStatusForGroup) : (isChannel ? R.string.EventLogChangedEmojiStatus : R.string.EventLogChangedEmojiStatusGroup)
                ) : (
                    until != 0 ? (isChannel ? R.string.EventLogChangedEmojiStatusFromFor : R.string.EventLogChangedEmojiStatusFromForGroup) : (isChannel ? R.string.EventLogChangedEmojiStatusFrom : R.string.EventLogChangedEmojiStatusFromGroup)
                )
            ));

            ssb = AndroidUtilities.replaceCharSequence("%1$s", ssb, prev);
            ssb = AndroidUtilities.replaceCharSequence("%2$s", ssb, next);
            if (until != 0) {
                final String untilString = LocaleController.formatTTLString((int) ((until - event.date) * 1.05f));
                ssb = AndroidUtilities.replaceCharSequence("%3$s", ssb, untilString);
            }

            messageText = replaceWithLink(ssb, "un1", fromUser);
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionChangeWallpaper) {
            TLRPC.TL_channelAdminLogEventActionChangeWallpaper action = (TLRPC.TL_channelAdminLogEventActionChangeWallpaper) event.action;
            boolean isChannel = ChatObject.isChannelAndNotMegaGroup(chat);
            if (action.new_value instanceof TLRPC.TL_wallPaperNoFile && action.new_value.id == 0 && action.new_value.settings == null) {
                messageText = replaceWithLink(getString(isChannel ? R.string.EventLogRemovedWallpaper : R.string.EventLogRemovedWallpaperGroup), "un1", fromUser);
            } else {
                photoThumbs = new ArrayList<>();
                if (action.new_value.document != null) {
                    photoThumbs.addAll(action.new_value.document.thumbs);
                    photoThumbsObject = action.new_value.document;
                }
                messageText = replaceWithLink(getString(isChannel ? R.string.EventLogChangedWallpaper : R.string.EventLogChangedWallpaperGroup), "un1", fromUser);
            }
        } else if (event.action instanceof TLRPC.TL_channelAdminLogEventActionChangeBackgroundEmoji) {
            boolean isChannel = ChatObject.isChannelAndNotMegaGroup(chat);
            TLRPC.TL_channelAdminLogEventActionChangeBackgroundEmoji action = (TLRPC.TL_channelAdminLogEventActionChangeBackgroundEmoji) event.action;
            messageText = replaceWithLink(getString(isChannel ? R.string.EventLogChangedEmoji : R.string.EventLogChangedEmojiGroup), "un1", fromUser);

            SpannableString emoji1;
            if (action.prev_value == 0) {
                emoji1 = new SpannableString(getString(R.string.EventLogEmojiNone));
            } else {
                emoji1 = new SpannableString("e");
                emoji1.setSpan(new AnimatedEmojiSpan(action.prev_value, Theme.chat_actionTextPaint.getFontMetricsInt()), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            messageText = AndroidUtilities.replaceCharSequence("%1$s", messageText, emoji1);

            SpannableString emoji2;
            if (action.new_value == 0) {
                emoji2 = new SpannableString(getString(R.string.EventLogEmojiNone));
            } else {
                emoji2 = new SpannableString("e");
                emoji2.setSpan(new AnimatedEmojiSpan(action.new_value, Theme.chat_actionTextPaint.getFontMetricsInt()), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            messageText = AndroidUtilities.replaceCharSequence("%2$s", messageText, emoji2);
        } else {
            messageText = "unsupported " + event.action;
        }

        if (messageOwner == null) {
            messageOwner = new TLRPC.TL_messageService();
        }
        messageOwner.message = messageText.toString();
        messageOwner.from_id = new TLRPC.TL_peerUser();
        messageOwner.from_id.user_id = event.user_id;
        messageOwner.date = event.date;
        messageOwner.id = mid[0]++;
        eventId = event.id;
        messageOwner.out = false;
        messageOwner.peer_id = new TLRPC.TL_peerChannel();
        messageOwner.peer_id.channel_id = chat.id;
        messageOwner.unread = false;
        MediaController mediaController = MediaController.getInstance();
        isOutOwnerCached = null;

        if (message instanceof TLRPC.TL_messageEmpty) {
            message = null;
        }

        if (message != null) {
            message.out = false;
            message.realId = message.id;
            message.id = mid[0]++;
            message.flags = message.flags & ~TLRPC.MESSAGE_FLAG_EDITED;
            message.dialog_id = -chat.id;
            int realDate = 0;
            if (event.action instanceof TLRPC.TL_channelAdminLogEventActionDeleteMessage) {
                realDate = message.date;
                message.date = event.date;
            }
            MessageObject messageObject = new MessageObject(currentAccount, message, null, null, true, true, eventId);
            messageObject.realDate = realDate;
            messageObject.currentEvent = event;
            if (messageObject.contentType >= 0) {
                if (mediaController.isPlayingMessage(messageObject)) {
                    MessageObject player = mediaController.getPlayingMessageObject();
                    messageObject.audioProgress = player.audioProgress;
                    messageObject.audioProgressSec = player.audioProgressSec;
                }
                createDateArray(currentAccount, event, messageObjects, messagesByDays, addToEnd);
                if (addToEnd) {
                    messageObjects.add(0, messageObject);
                } else {
                    messageObjects.add(messageObjects.size() - 1, messageObject);
                }
            } else {
                contentType = -1;
            }
            if (webPageDescriptionEntities != null) {
                messageObject.webPageDescriptionEntities = webPageDescriptionEntities;
                messageObject.linkDescription = null;
                messageObject.generateLinkDescription();
            }
        }
        if (event.action instanceof TLRPC.TL_channelAdminLogEventActionDeleteMessage) {
            return;
        }
        if (contentType >= 0) {
            createDateArray(currentAccount, event, messageObjects, messagesByDays, addToEnd);
            if (addToEnd) {
                messageObjects.add(0, this);
            } else {
                messageObjects.add(messageObjects.size() - 1, this);
            }
        } else {
            return;
        }

        if (messageText == null) {
            messageText = "";
        }

        TextPaint paint;
        if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaGame) {
            paint = Theme.chat_msgGameTextPaint;
        } else {
            paint = Theme.chat_msgTextPaint;
        }

        int[] emojiOnly = allowsBigEmoji() ? new int[1] : null;
        messageText = Emoji.replaceEmoji(messageText, paint.getFontMetricsInt(), false, emojiOnly);
        messageText = replaceAnimatedEmoji(messageText, paint.getFontMetricsInt());
        if (emojiOnly != null && emojiOnly[0] > 1) {
            replaceEmojiToLottieFrame(messageText, emojiOnly);
        }
        checkEmojiOnly(emojiOnly);

        setType();
        measureInlineBotButtons();
        generateCaption();

        if (mediaController.isPlayingMessage(this)) {
            MessageObject player = mediaController.getPlayingMessageObject();
            audioProgress = player.audioProgress;
            audioProgressSec = player.audioProgressSec;
        }
        generateLayout(fromUser);
        layoutCreated = true;
        generateThumbs(false);
        checkMediaExistance();
    }

    private boolean spoiledLoginCode = false;
    private static Pattern loginCodePattern;
    public void spoilLoginCode() { // spoil login code from +42777
        if (!spoiledLoginCode && messageText != null && messageOwner != null && messageOwner.entities != null && messageOwner.from_id instanceof TLRPC.TL_peerUser && (messageOwner.from_id.user_id == 777000 || messageOwner.from_id.user_id == UserObject.VERIFY)) {
            if (loginCodePattern == null) {
                loginCodePattern = Pattern.compile("[\\d\\-]{5,8}");
            }
            try {
                Matcher matcher = loginCodePattern.matcher(messageText);
                if (matcher.find()) {
                    TLRPC.TL_messageEntitySpoiler spoiler = new TLRPC.TL_messageEntitySpoiler();
                    spoiler.offset = matcher.start();
                    spoiler.length = matcher.end() - spoiler.offset;
                    messageOwner.entities.add(spoiler);
                }
            } catch (Exception e) {
                FileLog.e(e, false);
            }
            spoiledLoginCode = true;
        }
    }

    public boolean didSpoilLoginCode() {
        return spoiledLoginCode;
    }

    private CharSequence getStringFrom(TLRPC.ChatReactions reactions) {
        if (reactions instanceof TLRPC.TL_chatReactionsAll) {
            return getString(R.string.AllReactions);
        }
        if (reactions instanceof TLRPC.TL_chatReactionsSome) {
            TLRPC.TL_chatReactionsSome reactionsSome = (TLRPC.TL_chatReactionsSome) reactions;
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
            for (int i = 0; i < reactionsSome.reactions.size(); i++) {
                if (i != 0) {
                    spannableStringBuilder.append(" ");
                }
                CharSequence reaction = ReactionsUtils.reactionToCharSequence(reactionsSome.reactions.get(i));
                spannableStringBuilder.append(Emoji.replaceEmoji(reaction, null, false));
            }
            return spannableStringBuilder;
        }
        return getString(R.string.NoReactions);
    }

    private String getUsernamesString(ArrayList<String> usernames) {
        if (usernames == null || usernames.size() == 0) {
            return getString(R.string.UsernameEmpty).toLowerCase();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < usernames.size(); ++i) {
            sb.append("@");
            sb.append(usernames.get(i));
            if (i < usernames.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private String getUserName(TLObject object, ArrayList<TLRPC.MessageEntity> entities, int offset) {
        String name;
        String username;
        long id;
        if (object == null) {
            name = "";
            username = null;
            id = 0;
        } else if (object instanceof TLRPC.User) {
            TLRPC.User user = (TLRPC.User) object;
            if (user.deleted) {
                name = getString(R.string.HiddenName);
            } else {
                name = ContactsController.formatName(user.first_name, user.last_name);
            }
            username = UserObject.getPublicUsername(user);
            id = user.id;
        } else {
            TLRPC.Chat chat = (TLRPC.Chat) object;
            name = chat.title;
            username = ChatObject.getPublicUsername(chat);
            id = -chat.id;
        }
        if (offset >= 0) {
            TLRPC.TL_messageEntityMentionName entity = new TLRPC.TL_messageEntityMentionName();
            entity.user_id = id;
            entity.offset = offset;
            entity.length = name.length();
            entities.add(entity);
        }
        if (!TextUtils.isEmpty(username)) {
            if (offset >= 0) {
                TLRPC.TL_messageEntityMentionName entity = new TLRPC.TL_messageEntityMentionName();
                entity.user_id = id;
                entity.offset = offset + name.length() + 2;
                entity.length = username.length() + 1;
                entities.add(entity);
            }
            return String.format("%1$s (@%2$s)", name, username);
        }
        return name;
    }

    public boolean updateTranslation() {
        return updateTranslation(false);
    }

    public boolean translated = false;
    public boolean summarized = false;
    public boolean updateTranslation(boolean force) {
        boolean replyUpdated = replyMessageObject != null && replyMessageObject != this && replyMessageObject.updateTranslation(force);
        TranslateController translateController = MessagesController.getInstance(currentAccount).getTranslateController();
        final TLRPC.TL_textWithEntities translatedText = messageOwner != null ? (messageOwner.voiceTranscriptionOpen ? messageOwner.translatedVoiceTranscription : messageOwner.translatedText) : null;
        final TLRPC.TL_textWithEntities summarizedText = messageOwner != null && messageOwner.summarizedOpen ? messageOwner.summaryText : null;
        final TLRPC.TL_textWithEntities summarizeTranslatedText = messageOwner != null && messageOwner.summarizedOpen ? messageOwner.translatedSummaryText : null;
        if (
            summarizeTranslatedText != null &&
            messageOwner != null &&
            messageOwner.summarizedOpen &&
            TranslateController.isSummarizable(this) &&
            TranslateController.isTranslatable(this) &&
            translateController.isTranslatingDialog(getDialogId()) &&
            !translateController.isTranslateDialogHidden(getDialogId()) &&
            TextUtils.equals(translateController.getDialogTranslateTo(getDialogId()), messageOwner.translatedSummaryLanguage)
        ) {
            if (summarized && translated) {
                return replyUpdated || false;
            }
            summarized = true;
            translated = true;
            applyNewText(summarizeTranslatedText.text);
            generateCaption();
            return replyUpdated || true;
        } else if (
            messageOwner != null &&
            messageOwner.summarizedOpen &&
            TranslateController.isSummarizable(this) &&
            summarizedText != null
        ) {
            if (summarized && !translated) {
                return replyUpdated || false;
            }
            summarized = true;
            translated = false;
            applyNewText(summarizedText.text);
            generateCaption();
            return replyUpdated || true;
        } else if (
            messageOwner != null &&
            TranslateController.isTranslatable(this) &&
            translateController.isTranslatingDialog(getDialogId()) &&
            !translateController.isTranslateDialogHidden(getDialogId()) &&
            (translatedText != null || messageOwner.translatedPoll != null || messageOwner.translatedRichMessage != null) &&
            TextUtils.equals(translateController.getDialogTranslateTo(getDialogId()), messageOwner.translatedToLanguage)
        ) {
            if (translated && !summarized) {
                return replyUpdated || false;
            }
            translated = true;
            summarized = false;
            if (type == TYPE_ARTICLE) {
                generateLayout(null);
            } else if (translatedText != null) {
                applyNewText(translatedText.text);
                generateCaption();
            }
            return replyUpdated || true;
        } else if (messageOwner != null && (force || translated || summarized)) {
            translated = false;
            summarized = false;
            if (type == TYPE_ARTICLE) {
                generateLayout(null);
            } else {
                applyNewText(messageOwner.message);
                generateCaption();
            }
            return replyUpdated || true;
        }
        return replyUpdated || false;
    }

    public void applyNewText() {
        translated = false;
        summarized = false;
        applyNewText(messageOwner.message);
    }

    public void applyNewText(CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        TLRPC.User fromUser = null;
        if (isFromUser()) {
            fromUser = MessagesController.getInstance(currentAccount).getUser(messageOwner.from_id.user_id);
        }
        messageText = text;
        final ArrayList<TLRPC.MessageEntity> entities = getEntities();
        final TextPaint paint;
        if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaGame) {
            paint = Theme.chat_msgGameTextPaint;
        } else {
            paint = Theme.chat_msgTextPaint;
        }
        int[] emojiOnly = allowsBigEmoji() ? new int[1] : null;
        messageText = Emoji.replaceEmoji(messageText, paint.getFontMetricsInt(), false, emojiOnly);
        messageText = replaceAnimatedEmoji(messageText, entities, paint.getFontMetricsInt());
        if (emojiOnly != null && emojiOnly[0] > 1) {
            replaceEmojiToLottieFrame(messageText, emojiOnly);
        }
        checkEmojiOnly(emojiOnly);
        generateLayout(fromUser);
        setType();
    }

    private boolean allowsBigEmoji() {
        if (!SharedConfig.allowBigEmoji) {
            return false;
        }
        if (messageOwner == null || messageOwner.peer_id == null || messageOwner.peer_id.channel_id == 0 && messageOwner.peer_id.chat_id == 0) {
            return true;
        }
        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(messageOwner.peer_id.channel_id != 0 ? messageOwner.peer_id.channel_id : messageOwner.peer_id.chat_id);
        return chat != null && chat.gigagroup || (!ChatObject.isActionBanned(chat, ChatObject.ACTION_SEND_STICKERS) || ChatObject.hasAdminRights(chat));
    }

    public void generateGameMessageText(TLRPC.User fromUser) {
        if (fromUser == null && isFromUser()) {
            fromUser = MessagesController.getInstance(currentAccount).getUser(messageOwner.from_id.user_id);
        }
        TLRPC.TL_game game = null;
        if (replyMessageObject != null && getMedia(replyMessageObject) != null && getMedia(replyMessageObject).game != null) {
            game = getMedia(replyMessageObject).game;
        }
        if (game == null) {
            if (fromUser != null && fromUser.id == UserConfig.getInstance(currentAccount).getClientUserId()) {
                messageText = formatString("ActionYouScored", R.string.ActionYouScored, LocaleController.formatPluralString("Points", messageOwner.action.score));
            } else {
                messageText = replaceWithLink(formatString("ActionUserScored", R.string.ActionUserScored, LocaleController.formatPluralString("Points", messageOwner.action.score)), "un1", fromUser);
            }
        } else {
            if (fromUser != null && fromUser.id == UserConfig.getInstance(currentAccount).getClientUserId()) {
                messageText = formatString("ActionYouScoredInGame", R.string.ActionYouScoredInGame, LocaleController.formatPluralString("Points", messageOwner.action.score));
            } else {
                messageText = replaceWithLink(formatString("ActionUserScoredInGame", R.string.ActionUserScoredInGame, LocaleController.formatPluralString("Points", messageOwner.action.score)), "un1", fromUser);
            }
            messageText = replaceWithLink(messageText, "un2", game);
        }
    }

    public boolean hasValidReplyMessageObject() {
        if (messageOwner != null && messageOwner.reply_to != null && messageOwner.reply_to.forum_topic && messageOwner.reply_to.reply_to_msg_id == messageOwner.reply_to.reply_to_top_id) {
            return false;
        }
        return !(replyMessageObject == null || replyMessageObject.messageOwner instanceof TLRPC.TL_messageEmpty || replyMessageObject.messageOwner.action instanceof TLRPC.TL_messageActionHistoryClear || replyMessageObject.messageOwner.action instanceof TLRPC.TL_messageActionTopicCreate);
    }

    @Deprecated
    public void generateSuggestionApprovalMessageText() {
        /*
        final String channelName = ForumUtilities.getMonoForumTitle(currentAccount, DialogObject.getPeerDialogId(messageOwner.peer_id), true);
        final String userName = MessagesController.getInstance(currentAccount).getPeerName(DialogObject.getPeerDialogId(messageOwner.saved_peer_id));

        messageText = getActionSuggestionApprovalText(channelName, userName);
        */
    }

    public void generatePaymentSentMessageText(TLRPC.User fromUser, boolean me) {
        if (fromUser == null) {
            fromUser = MessagesController.getInstance(currentAccount).getUser(getDialogId());
        }
        final String name = fromUser != null ? UserObject.getFirstName(fromUser) : "";
        String currency;
        try {
            if (StarsController.currency.equals(messageOwner.action.currency)) {
                currency = StarsController.currency + " " + messageOwner.action.total_amount;
            } else {
                currency = LocaleController.getInstance().formatCurrencyString(messageOwner.action.total_amount, messageOwner.action.currency);
            }
        } catch (Exception e) {
            currency = "<error>";
            FileLog.e(e);
        }
        if (replyMessageObject != null && getMedia(replyMessageObject) instanceof TLRPC.TL_messageMediaInvoice) {
            if (messageOwner.action.subscription_until_date != 0) {
                if (me) {
                    messageText = formatString(R.string.PaymentSuccessfullyPaidMeSubscription, name, currency, getMedia(replyMessageObject).title, LocaleController.formatDateTime(messageOwner.action.subscription_until_date, false));
                } else {
                    messageText = formatString(R.string.PaymentSuccessfullyPaidSubscription, currency, name, getMedia(replyMessageObject).title, LocaleController.formatDateTime(messageOwner.action.subscription_until_date, false));
                }
            } else if (messageOwner.action.recurring_init && !me) {
                messageText = formatString(R.string.PaymentSuccessfullyPaidRecurrent, currency, name, getMedia(replyMessageObject).title);
            } else {
                messageText = formatString(R.string.PaymentSuccessfullyPaid, currency, name, getMedia(replyMessageObject).title);
            }
        } else {
            if (messageOwner.action.subscription_until_date != 0) {
                if (me) {
                    messageText = formatString(R.string.PaymentSuccessfullyPaidMeNoItemSubscription, name, currency, LocaleController.formatDateTime(messageOwner.action.subscription_until_date, false));
                } else {
                    messageText = formatString(R.string.PaymentSuccessfullyPaidSubscriptionNoItem, currency, name, LocaleController.formatDateTime(messageOwner.action.subscription_until_date, false));
                }
            } else if (messageOwner.action.recurring_init && !me) {
                messageText = formatString(R.string.PaymentSuccessfullyPaidNoItemRecurrent, currency, name);
            } else {
                messageText = formatString(R.string.PaymentSuccessfullyPaidNoItem, currency, name);
            }
        }
        messageText = StarsIntroActivity.replaceStars(messageText);
    }

    public void generatePinMessageText(TLRPC.User fromUser, TLRPC.Chat chat) {
        if (fromUser == null && chat == null) {
            if (isFromUser()) {
                fromUser = MessagesController.getInstance(currentAccount).getUser(messageOwner.from_id.user_id);
            }
            if (fromUser == null) {
                if (messageOwner.peer_id instanceof TLRPC.TL_peerChannel) {
                    chat = MessagesController.getInstance(currentAccount).getChat(messageOwner.peer_id.channel_id);
                } else if (messageOwner.peer_id instanceof TLRPC.TL_peerChat) {
                    chat = MessagesController.getInstance(currentAccount).getChat(messageOwner.peer_id.chat_id);
                }
            }
        }
        if (replyMessageObject == null || replyMessageObject.messageOwner instanceof TLRPC.TL_messageEmpty || replyMessageObject.messageOwner.action instanceof TLRPC.TL_messageActionHistoryClear) {
            messageText = replaceWithLink(getString(R.string.ActionPinnedNoText), "un1", fromUser != null ? fromUser : chat);
        } else {
            if (replyMessageObject.isMusic()) {
                messageText = replaceWithLink(getString(R.string.ActionPinnedMusic), "un1", fromUser != null ? fromUser : chat);
            } else if (replyMessageObject.isVideo()) {
                messageText = replaceWithLink(getString(R.string.ActionPinnedVideo), "un1", fromUser != null ? fromUser : chat);
            } else if (replyMessageObject.isGif()) {
                messageText = replaceWithLink(getString(R.string.ActionPinnedGif), "un1", fromUser != null ? fromUser : chat);
            } else if (replyMessageObject.isVoice()) {
                messageText = replaceWithLink(getString(R.string.ActionPinnedVoice), "un1", fromUser != null ? fromUser : chat);
            } else if (replyMessageObject.isRoundVideo()) {
                messageText = replaceWithLink(getString(R.string.ActionPinnedRound), "un1", fromUser != null ? fromUser : chat);
            } else if ((replyMessageObject.isSticker() || replyMessageObject.isAnimatedSticker()) && !replyMessageObject.isAnimatedEmoji()) {
                messageText = replaceWithLink(getString(R.string.ActionPinnedSticker), "un1", fromUser != null ? fromUser : chat);
            } else if (getMedia(replyMessageObject) instanceof TLRPC.TL_messageMediaDocument) {
                messageText = replaceWithLink(getString(R.string.ActionPinnedFile), "un1", fromUser != null ? fromUser : chat);
            } else if (getMedia(replyMessageObject) instanceof TLRPC.TL_messageMediaGeo) {
                messageText = replaceWithLink(getString(R.string.ActionPinnedGeo), "un1", fromUser != null ? fromUser : chat);
            } else if (getMedia(replyMessageObject) instanceof TLRPC.TL_messageMediaGeoLive) {
                messageText = replaceWithLink(getString(R.string.ActionPinnedGeoLive), "un1", fromUser != null ? fromUser : chat);
            } else if (getMedia(replyMessageObject) instanceof TLRPC.TL_messageMediaContact) {
                messageText = replaceWithLink(getString(R.string.ActionPinnedContact), "un1", fromUser != null ? fromUser : chat);
            } else if (getMedia(replyMessageObject) instanceof TLRPC.TL_messageMediaPoll) {
                if (((TLRPC.TL_messageMediaPoll) getMedia(replyMessageObject)).poll.quiz) {
                    messageText = replaceWithLink(getString(R.string.ActionPinnedQuiz), "un1", fromUser != null ? fromUser : chat);
                } else {
                    messageText = replaceWithLink(getString(R.string.ActionPinnedPoll), "un1", fromUser != null ? fromUser : chat);
                }
            } else if (getMedia(replyMessageObject) instanceof TLRPC.TL_messageMediaPhoto) {
                messageText = replaceWithLink(getString(R.string.ActionPinnedPhoto), "un1", fromUser != null ? fromUser : chat);
            } else if (getMedia(replyMessageObject) instanceof TLRPC.TL_messageMediaPaidMedia) {
                TLRPC.TL_messageMediaPaidMedia paidMedia = (TLRPC.TL_messageMediaPaidMedia) getMedia(replyMessageObject);
                String username = chat != null ? chat.title : UserObject.getUserName(fromUser);
                messageText = LocaleController.formatPluralString("NotificationPinnedPaidMedia", (int) paidMedia.stars_amount, username);
            } else if (getMedia(replyMessageObject) instanceof TLRPC.TL_messageMediaGame) {
                messageText = replaceWithLink(formatString("ActionPinnedGame", R.string.ActionPinnedGame, "\uD83C\uDFAE " + getMedia(replyMessageObject).game.title), "un1", fromUser != null ? fromUser : chat);
                messageText = Emoji.replaceEmoji(messageText, Theme.chat_msgTextPaint.getFontMetricsInt(), false);
            } else if (replyMessageObject.messageText != null && replyMessageObject.messageText.length() > 0) {
                CharSequence mess = AnimatedEmojiSpan.cloneSpans(replyMessageObject.messageText);
                boolean ellipsize = false;
                if (mess.length() > 20) {
                    mess = mess.subSequence(0, 20);
                    ellipsize = true;
                }
                mess = Emoji.replaceEmoji(mess, Theme.chat_msgTextPaint.getFontMetricsInt(), true);
                if (replyMessageObject != null && replyMessageObject.messageOwner != null) {
                    mess = replyMessageObject.replaceAnimatedEmoji(mess, Theme.chat_msgTextPaint.getFontMetricsInt());
                }
                MediaDataController.addTextStyleRuns(replyMessageObject, (Spannable) mess);
                if (ellipsize) {
                    if (mess instanceof SpannableStringBuilder) {
                        ((SpannableStringBuilder) mess).append("...");
                    } else if (mess != null) {
                        mess = new SpannableStringBuilder(mess).append("...");
                    }
                }
                messageText = replaceWithLink(AndroidUtilities.formatSpannable(getString(R.string.ActionPinnedText), mess), "un1", fromUser != null ? fromUser : chat);
            } else {
                messageText = replaceWithLink(getString(R.string.ActionPinnedNoText), "un1", fromUser != null ? fromUser : chat);
            }
        }
    }

    public static void updateReactions(TLRPC.Message message, TLRPC.TL_messageReactions reactions) {
        if (message == null || reactions == null) {
            return;
        }
        boolean chosenReactionfound = false;
        if (message.reactions != null) {
            for (int a = 0, N = message.reactions.results.size(); a < N; a++) {
                TLRPC.ReactionCount reaction = message.reactions.results.get(a);
                for (int b = 0, N2 = reactions.results.size(); b < N2; b++) {
                    TLRPC.ReactionCount newReaction = reactions.results.get(b);
                    if (ReactionsLayoutInBubble.equalsTLReaction(reaction.reaction, newReaction.reaction)) {
                        if (!chosenReactionfound && reactions.min && reaction.chosen) {
                            newReaction.chosen = true;
                            chosenReactionfound = true;
                        }
                        newReaction.lastDrawnPosition = reaction.lastDrawnPosition;
                    }
                }
                if (reaction.chosen) {
                    chosenReactionfound = true;
                }
            }
        }
        message.reactions = reactions;
        message.flags |= 1048576;
    }

    public boolean hasReactions() {
        return messageOwner.reactions != null && !messageOwner.reactions.results.isEmpty();
    }

    public boolean hasReaction(ReactionsLayoutInBubble.VisibleReaction reaction) {
        if (!hasReactions() || reaction == null) return false;
        for (int i = 0; i < messageOwner.reactions.results.size(); ++i) {
            TLRPC.ReactionCount rc = messageOwner.reactions.results.get(i);
            if (reaction.isSame(rc.reaction)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasChosenReaction(ReactionsLayoutInBubble.VisibleReaction reaction) {
        if (!hasReactions() || reaction == null) return false;
        for (int i = 0; i < messageOwner.reactions.results.size(); ++i) {
            TLRPC.ReactionCount rc = messageOwner.reactions.results.get(i);
            if (reaction.isSame(rc.reaction)) {
                return rc.chosen;
            }
        }
        return false;
    }


    public static void updatePollResults(TLRPC.TL_messageMediaPoll media, TLRPC.PollResults results) {
        if (media == null || results == null) {
            return;
        }
        if ((results.flags & 2) != 0) {
            ArrayList<byte[]> chosen = null;
            byte[] correct = null;
            if (results.min && media.results.results != null) {
                for (int b = 0, N2 = media.results.results.size(); b < N2; b++) {
                    TLRPC.PollAnswerVoters answerVoters = media.results.results.get(b);
                    if (answerVoters.chosen) {
                        if (chosen == null) {
                            chosen = new ArrayList<>();
                        }
                        chosen.add(answerVoters.option);
                    }
                    if (answerVoters.correct) {
                        correct = answerVoters.option;
                    }
                }
            }
            media.results.results = results.results;
            if (chosen != null || correct != null) {
                for (int b = 0, N2 = media.results.results.size(); b < N2; b++) {
                    TLRPC.PollAnswerVoters answerVoters = media.results.results.get(b);
                    if (chosen != null) {
                        for (int a = 0, N = chosen.size(); a < N; a++) {
                            if (Arrays.equals(answerVoters.option, chosen.get(a))) {
                                answerVoters.chosen = true;
                                chosen.remove(a);
                                break;
                            }
                        }
                        if (chosen.isEmpty()) {
                            chosen = null;
                        }
                    }
                    if (correct != null && Arrays.equals(answerVoters.option, correct)) {
                        answerVoters.correct = true;
                        correct = null;
                    }
                    if (chosen == null && correct == null) {
                        break;
                    }
                }
            }
            media.results.flags |= 2;
        } else if (!results.min && media.poll != null && media.poll.hide_results_until_close && (results.results == null || results.results.isEmpty())) {
            media.results.results = new ArrayList<>();
            media.results.flags = BitwiseUtils.setFlag(media.results.flags, 2, false);
        }
        if ((results.flags & 4) != 0) {
            media.results.total_voters = results.total_voters;
            media.results.flags |= 4;
        }
        if ((results.flags & 8) != 0) {
            media.results.recent_voters = results.recent_voters;
            media.results.flags |= 8;
        }
        if ((results.flags & 16) != 0) {
            media.results.solution = results.solution;
            media.results.solution_entities = results.solution_entities;
            media.results.flags |= 16;
        }
        if (!results.min) {
            media.results.has_unread_votes = results.has_unread_votes;
            media.results.can_view_stats = results.can_view_stats;
        }
    }

    public void loadAnimatedEmojiDocument() {
        if (emojiAnimatedSticker != null || emojiAnimatedStickerId == null || emojiAnimatedStickerLoading) {
            return;
        }
        emojiAnimatedStickerLoading = true;
        AnimatedEmojiDrawable.getDocumentFetcher(currentAccount).fetchDocument(emojiAnimatedStickerId, document -> {
            AndroidUtilities.runOnUIThread(() -> {
                this.emojiAnimatedSticker = document;
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.animatedEmojiDocumentLoaded, this);
            });
        });
    }

    public boolean isPollClosed() {
        if (type != TYPE_POLL) {
            return false;
        }
        TLRPC.MessageMedia m = getMedia(messageOwner);
        if (!(m instanceof TLRPC.TL_messageMediaPoll)) {
            return false;
        }
        return ((TLRPC.TL_messageMediaPoll) m).poll.closed;
    }

    public boolean isQuiz() {
        if (type != TYPE_POLL) {
            return false;
        }
        TLRPC.MessageMedia m = getMedia(messageOwner);
        if (!(m instanceof TLRPC.TL_messageMediaPoll)) {
            return false;
        }
        return ((TLRPC.TL_messageMediaPoll) m).poll.quiz;
    }

    public boolean isPublicPoll() {
        if (type != TYPE_POLL) {
            return false;
        }
        TLRPC.MessageMedia m = getMedia(messageOwner);
        if (!(m instanceof TLRPC.TL_messageMediaPoll)) {
            return false;
        }
        return ((TLRPC.TL_messageMediaPoll) m).poll.public_voters;
    }

    public boolean isPoll() {
        return type == TYPE_POLL && getMedia(messageOwner) instanceof TLRPC.TL_messageMediaPoll;
    }

    public boolean isTodo() {
        return type == TYPE_POLL && getMedia(messageOwner) instanceof TLRPC.TL_messageMediaToDo;
    }

    public boolean canCompleteTodo() {
        TLRPC.MessageMedia m = getMedia(messageOwner);
        if (!(m instanceof TLRPC.TL_messageMediaToDo)) {
            return false;
        }
        if (isForwarded()) {
            return false;
        }
        final TLRPC.TL_messageMediaToDo todo = (TLRPC.TL_messageMediaToDo) m;
        return isOutOwner() || todo.todo.others_can_complete;
    }

    public boolean canAppendToTodo() {
        TLRPC.MessageMedia m = getMedia(messageOwner);
        if (!(m instanceof TLRPC.TL_messageMediaToDo)) {
            return false;
        }
        if (isForwarded()) {
            return false;
        }
        final TLRPC.TL_messageMediaToDo todo = (TLRPC.TL_messageMediaToDo) m;
        if (todo.todo.list.size() >= MessagesController.getInstance(currentAccount).todoItemsMax) {
            return false;
        }
        return isOutOwner() || todo.todo.others_can_complete && todo.todo.others_can_append;
    }

    public boolean canUnvote() {
        if (type != TYPE_POLL) {
            return false;
        }
        TLRPC.MessageMedia m = getMedia(messageOwner);
        if (!(m instanceof TLRPC.TL_messageMediaPoll)) {
            return false;
        }
        return canUnvote((TLRPC.TL_messageMediaPoll) m);
    }

    public static boolean canUnvote(TLRPC.TL_messageMediaPoll mediaPoll) {
        if (mediaPoll == null || mediaPoll.results == null || mediaPoll.results.results.isEmpty() || mediaPoll.poll.revoting_disabled) {
            return false;
        }
        for (int a = 0, N = mediaPoll.results.results.size(); a < N; a++) {
            TLRPC.PollAnswerVoters answer = mediaPoll.results.results.get(a);
            if (answer.chosen) {
                return true;
            }
        }
        return false;
    }

    public static TLRPC.PollAnswerVoters getPollResult(TLRPC.TL_messageMediaPoll mediaPoll, byte[] option) {
        if (mediaPoll == null || mediaPoll.results == null || mediaPoll.results.results.isEmpty()) {
            return null;
        }
        for (int a = 0, N = mediaPoll.results.results.size(); a < N; a++) {
            TLRPC.PollAnswerVoters answer = mediaPoll.results.results.get(a);
            if (Arrays.equals(answer.option, option)) {
                return answer;
            }

        }
        return null;
    }

    public static boolean canShowVotersList(TLRPC.TL_messageMediaPoll mediaPoll) {
        if (mediaPoll == null || mediaPoll.results == null || mediaPoll.results.results.isEmpty()) {
            return false;
        }
        return mediaPoll.poll.public_voters && (
            mediaPoll.poll.closed || mediaPoll.poll.creator || isVoted(mediaPoll) && !mediaPoll.poll.hide_results_until_close
        );
    }

    public static boolean isVoted(TLRPC.TL_messageMediaPoll mediaPoll) {
        if (mediaPoll == null || mediaPoll.results == null || mediaPoll.results.results.isEmpty()) {
            return false;
        }
        for (int a = 0, N = mediaPoll.results.results.size(); a < N; a++) {
            TLRPC.PollAnswerVoters answer = mediaPoll.results.results.get(a);
            if (answer.chosen) {
                return true;
            }
        }
        return false;
    }

    public boolean isVotedButResultsHiddenUntilClose() {
        if (type != TYPE_POLL) {
            return false;
        }
        TLRPC.MessageMedia m = getMedia(messageOwner);
        if (!(m instanceof TLRPC.TL_messageMediaPoll)) {
            return false;
        }
        return isVotedButResultsHiddenUntilClose((TLRPC.TL_messageMediaPoll) m);
    }

    public static boolean isVotedButResultsHiddenUntilClose(TLRPC.TL_messageMediaPoll mediaPoll) {
        return mediaPoll != null && !mediaPoll.poll.closed && mediaPoll.poll.hide_results_until_close
            && isVoted(mediaPoll) && !isVoteResultsIsNotEmpty(mediaPoll);
    }

    public static boolean isVoteResultsIsNotEmpty(TLRPC.TL_messageMediaPoll mediaPoll) {
        if (mediaPoll == null || mediaPoll.results == null || mediaPoll.results.results.isEmpty()) {
            return false;
        }
        for (int a = 0, N = mediaPoll.results.results.size(); a < N; a++) {
            TLRPC.PollAnswerVoters answer = mediaPoll.results.results.get(a);
            if (answer.voters > 0) {
                return true;
            }
        }
        return false;
    }

    public boolean hasVoteResults() {
        if (type != TYPE_POLL) {
            return false;
        }
        TLRPC.MessageMedia m = getMedia(messageOwner);
        if (!(m instanceof TLRPC.TL_messageMediaPoll)) {
            return false;
        }
        return isVoteResultsIsNotEmpty((TLRPC.TL_messageMediaPoll) m);
    }

    public boolean isVoted() {
        if (type != TYPE_POLL) {
            return false;
        }
        TLRPC.MessageMedia m = getMedia(messageOwner);
        if (!(m instanceof TLRPC.TL_messageMediaPoll)) {
            return false;
        }
        return isVoted((TLRPC.TL_messageMediaPoll) m);
    }

    public boolean isSponsored() {
        return sponsoredId != null;
    }

    public long getPollId() {
        if (type != TYPE_POLL) {
            return 0;
        }
        TLRPC.MessageMedia media = getMedia(messageOwner);
        if (media instanceof TLRPC.TL_messageMediaPoll) {
            return ((TLRPC.TL_messageMediaPoll) media).poll.id;
        } /*else if (media instanceof TLRPC.TL_messageMediaToDo) {
            return ((TLRPC.TL_messageMediaToDo) media).todo.id;
        }*/
        return 0;
    }

    private TLRPC.Photo getPhotoWithId(TLRPC.WebPage webPage, long id) {
        if (webPage == null || webPage.cached_page == null) {
            return null;
        }
        if (webPage.photo != null && webPage.photo.id == id) {
            return webPage.photo;
        }
        for (int a = 0; a < webPage.cached_page.photos.size(); a++) {
            TLRPC.Photo photo = webPage.cached_page.photos.get(a);
            if (photo.id == id) {
                return photo;
            }
        }
        return null;
    }

    private TLRPC.Document getDocumentWithId(TLRPC.WebPage webPage, long id) {
        if (webPage == null || webPage.cached_page == null) {
            return null;
        }
        if (webPage.document != null && webPage.document.id == id) {
            return webPage.document;
        }
        for (int a = 0; a < webPage.cached_page.documents.size(); a++) {
            TLRPC.Document document = webPage.cached_page.documents.get(a);
            if (document.id == id) {
                return document;
            }
        }
        return null;
    }

    public boolean isSupergroup() {
        if (localSupergroup) {
            return true;
        }
        if (cachedIsSupergroup != null) {
            return cachedIsSupergroup;
        }
        if (messageOwner.peer_id != null && messageOwner.peer_id.channel_id != 0) {
            TLRPC.Chat chat = getChat(null, null, messageOwner.peer_id.channel_id);
            if (chat != null) {
                return (cachedIsSupergroup = chat.megagroup);
            } else {
                return false;
            }
        } else {
            cachedIsSupergroup = false;
        }
        return false;
    }

    private MessageObject getMessageObjectForBlock(TLRPC.WebPage webPage, TL_iv.PageBlock pageBlock) {
        TLRPC.TL_message message = null;
        if (pageBlock instanceof TL_iv.pageBlockPhoto) {
            TL_iv.pageBlockPhoto pageBlockPhoto = (TL_iv.pageBlockPhoto) pageBlock;
            TLRPC.Photo photo = getPhotoWithId(webPage, pageBlockPhoto.photo_id);
            if (photo == webPage.photo) {
                return this;
            }
            message = new TLRPC.TL_message();
            message.media = new TLRPC.TL_messageMediaPhoto();
            message.media.photo = photo;
        } else if (pageBlock instanceof TL_iv.pageBlockVideo) {
            TL_iv.pageBlockVideo pageBlockVideo = (TL_iv.pageBlockVideo) pageBlock;
            TLRPC.Document document = getDocumentWithId(webPage, pageBlockVideo.video_id);
            if (document == webPage.document) {
                return this;
            }
            message = new TLRPC.TL_message();
            message.media = new TLRPC.TL_messageMediaDocument();
            message.media.document = getDocumentWithId(webPage, pageBlockVideo.video_id);
        }
        message.message = "";
        message.realId = getId();
        message.id = Utilities.random.nextInt();
        message.date = messageOwner.date;
        message.peer_id = messageOwner.peer_id;
        message.out = messageOwner.out;
        message.from_id = messageOwner.from_id;
        return new MessageObject(currentAccount, message, false, true);
    }

    public ArrayList<MessageObject> getWebPagePhotos(ArrayList<MessageObject> array, ArrayList<TL_iv.PageBlock> blocksToSearch) {
        ArrayList<MessageObject> messageObjects = array == null ? new ArrayList<>() : array;
        if (getMedia(messageOwner) == null || getMedia(messageOwner).webpage == null) {
            return messageObjects;
        }
        TLRPC.WebPage webPage = getMedia(messageOwner).webpage;
        if (webPage.cached_page == null) {
            return messageObjects;
        }
        ArrayList<TL_iv.PageBlock> blocks = blocksToSearch == null ? webPage.cached_page.blocks : blocksToSearch;
        for (int a = 0; a < blocks.size(); a++) {
            TL_iv.PageBlock block = blocks.get(a);
            if (block instanceof TL_iv.pageBlockSlideshow) {
                TL_iv.pageBlockSlideshow slideshow = (TL_iv.pageBlockSlideshow) block;
                for (int b = 0; b < slideshow.items.size(); b++) {
                    messageObjects.add(getMessageObjectForBlock(webPage, slideshow.items.get(b)));
                }
            } else if (block instanceof TL_iv.pageBlockCollage) {
                TL_iv.pageBlockCollage slideshow = (TL_iv.pageBlockCollage) block;
                for (int b = 0; b < slideshow.items.size(); b++) {
                    messageObjects.add(getMessageObjectForBlock(webPage, slideshow.items.get(b)));
                }
            }
        }
        return messageObjects;
    }

    public void createMessageSendInfo() {
        createMessageSendInfo(false);
    }
    public void createMessageSendInfo(boolean force) {
        boolean notReadyYet = videoEditedInfo != null && videoEditedInfo.notReadyYet;
        if (messageOwner.message != null && (messageOwner.id < 0 || isEditing()) && messageOwner.params != null) {
            String param;

            final TLRPC.MessageMedia messageMedia = getMedia(this);
            boolean allowVideoEditedInfo = force || isVideo() || isNewGif() || isRoundVideo() || isVideoSticker() || isPaidVideo(messageMedia);

            if (!allowVideoEditedInfo && messageMedia instanceof TLRPC.TL_messageMediaPoll) {
                int pollIndex = PollAttachedMediaPack.INDEX_NONE;
                String pollIndexStr = messageOwner.params.get("pollMediaIndex");
                if (pollIndexStr != null) {
                    try {
                        pollIndex = Integer.parseInt(pollIndexStr);
                    } catch (Throwable t) {}
                }
                TLRPC.MessageMedia pollMessageMedia = PollAttachedMediaPack.getMedia(((TLRPC.TL_messageMediaPoll) messageMedia), pollIndex);
                if (pollMessageMedia != null && isVideoDocument(pollMessageMedia.document)) {
                    allowVideoEditedInfo = true;
                }
            }

            if ((param = messageOwner.params.get("ve")) != null && allowVideoEditedInfo) {
                videoEditedInfo = new VideoEditedInfo();
                if (!videoEditedInfo.parseString(param)) {
                    videoEditedInfo = null;
                } else {
                    videoEditedInfo.roundVideo = isRoundVideo();
                    videoEditedInfo.notReadyYet = notReadyYet;
                }
            }
            if (messageOwner.send_state == MESSAGE_SEND_STATE_EDITING && (param = messageOwner.params.get("prevMedia")) != null) {
                SerializedData serializedData = new SerializedData(Base64.decode(param, Base64.DEFAULT));
                int constructor = serializedData.readInt32(false);
                previousMedia = TLRPC.MessageMedia.TLdeserialize(serializedData, constructor, false);
                previousMessage = serializedData.readString(false);
                previousAttachPath = serializedData.readString(false);
                int count = serializedData.readInt32(false);
                previousMessageEntities = new ArrayList<>(count);
                for (int a = 0; a < count; a++) {
                    constructor = serializedData.readInt32(false);
                    TLRPC.MessageEntity entity = TLRPC.MessageEntity.TLdeserialize(serializedData, constructor, false);
                    previousMessageEntities.add(entity);
                }
                serializedData.cleanup();
            }
        }
    }

    public static boolean isPaidVideo(TLRPC.MessageMedia m) {
        return m instanceof TLRPC.TL_messageMediaPaidMedia && m.extended_media.size() == 1 && isExtendedVideo(m.extended_media.get(0));
    }

    public static boolean isExtendedVideo(TLRPC.MessageExtendedMedia em) {
        if (em instanceof TLRPC.TL_messageExtendedMedia) {
            TLRPC.TL_messageExtendedMedia eem = (TLRPC.TL_messageExtendedMedia) em;
            return eem.media instanceof TLRPC.TL_messageMediaDocument && isVideoDocument(eem.media.document);
        } else if (em instanceof TLRPC.TL_messageExtendedMediaPreview) {
            TLRPC.TL_messageExtendedMediaPreview eem = (TLRPC.TL_messageExtendedMediaPreview) em;
            return (eem.flags & 4) != 0;
        }
        return false;
    }

    public boolean hasSuggestionInlineButtons() {
        boolean needShow = messageOwner != null
            && messageOwner.suggested_post != null
            && !messageOwner.suggested_post.rejected
            && !messageOwner.suggested_post.accepted
            && !isSendError()
            && !isSending();

        if (needShow) {
            final long selfId = UserConfig.getInstance(currentAccount).getClientUserId();
            final long topicId = DialogObject.getPeerDialogId(messageOwner.saved_peer_id);
            final long senderId = DialogObject.getPeerDialogId(messageOwner.from_id);

            final boolean isUser = selfId == topicId;
            final boolean isAdmin = !isUser;
            final boolean messageFromUser = topicId == senderId;
            final boolean messageFromAdmin = !messageFromUser;

            if (isUser && messageFromUser || isAdmin && messageFromAdmin) {
                needShow = false;
            }
        }

        return needShow;
    }

    public BotInlineKeyboard.Source getInlineBotButtons() {
        return inlineKeyboardSource;
    }

    public boolean hasInlineBotButtons() {
        return !isRestrictedMessage && !isRepostPreview && messageOwner != null && (messageOwner.reply_markup instanceof TLRPC.TL_replyInlineMarkup && !((TLRPC.TL_replyInlineMarkup) messageOwner.reply_markup).rows.isEmpty() || getInlineBotButtons() != null);
    }

    public void measureInlineBotButtons() {
        if (isRestrictedMessage) {
            return;
        }
        wantedBotKeyboardWidth = 0;

        inlineKeyboardSource = null;

        BotInlineKeyboard.Builder builder = new BotInlineKeyboard.Builder();

        if (messageOwner != null && messageOwner.reply_markup instanceof TLRPC.TL_replyInlineMarkup && ((TLRPC.TL_replyInlineMarkup) messageOwner.reply_markup).rows != null) {
            builder.addBotKeyboard((TLRPC.TL_replyInlineMarkup) messageOwner.reply_markup);
        }
        if (hasSuggestionInlineButtons()) {
            builder.addSeparator();
            builder.addSuggestionKeyboard();
        }
        if (builder.isNotEmpty()) {
            inlineKeyboardSource = builder.build();
        }


        final BotInlineKeyboard.Source inlineKeyboard = inlineKeyboardSource;


        if (inlineKeyboard != null && !hasExtendedMedia() || messageOwner.reactions != null && !messageOwner.reactions.results.isEmpty()) {
            Theme.createCommonMessageResources();
            if (botButtonsLayout == null) {
                botButtonsLayout = new StringBuilder();
            } else {
                botButtonsLayout.setLength(0);
            }
        }

        if (inlineKeyboard != null && !hasExtendedMedia()) {
            for (int a = 0; a < inlineKeyboard.getRowsCount(); a++) {
                int maxButtonSize = 0;
                int size = inlineKeyboard.getColumnsCount(a);
                for (int b = 0; b < size; b++) {
                    BotInlineKeyboard.Button button = inlineKeyboard.getButton(a, b);
                    botButtonsLayout.append(a).append(b);
                    CharSequence text;
                    if ((button instanceof BotInlineKeyboard.ButtonBot) && TLKeyboardHelper.isType(((BotInlineKeyboard.ButtonBot) button).button, TL_keyboard.TL_inlineButtonTypeBuy.class) && (getMedia(messageOwner).flags & 4) != 0) {
                        text = getString(R.string.PaymentReceipt);
                    } else {
                        String str = button.getText();
                        if (str == null) {
                            str = "";
                        }
                        text = Emoji.replaceEmoji(str, Theme.chat_msgBotButtonPaint.getFontMetricsInt(), false);
                    }
                    StaticLayout staticLayout = new StaticLayout(text, Theme.chat_msgBotButtonPaint, dp(2000), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                    if (staticLayout.getLineCount() > 0) {
                        float width = staticLayout.getLineWidth(0);
                        float left = staticLayout.getLineLeft(0);
                        if (left < width) {
                            width -= left;
                        }
                        if (button.getIconRes() != 0) {
                            width += dp(24 + 12);
                        }
                        if (button.getIconEmoji() != 0) {
                            width += dp(24 + 12);
                        }
                        maxButtonSize = Math.max(maxButtonSize, (int) Math.ceil(width) + dp(4));
                    }
                }
                wantedBotKeyboardWidth = Math.max(wantedBotKeyboardWidth, (maxButtonSize + dp(12)) * size + dp(5) * (size - 1));
            }
        }
    }

    public boolean isVideoAvatar() {
        return messageOwner.action != null && messageOwner.action.photo != null && !messageOwner.action.photo.video_sizes.isEmpty();
    }

    public boolean isFcmMessage() {
        return localType != 0;
    }

    private TLRPC.User getUser(AbstractMap<Long, TLRPC.User> users, LongSparseArray<TLRPC.User> sUsers, long userId) {
        TLRPC.User user = null;
        if (users != null) {
            user = users.get(userId);
        } else if (sUsers != null) {
            user = sUsers.get(userId);
        }
        if (user == null) {
            user = MessagesController.getInstance(currentAccount).getUser(userId);
        }
        return user;
    }

    private TLRPC.Chat getChat(AbstractMap<Long, TLRPC.Chat> chats, LongSparseArray<TLRPC.Chat> sChats, long chatId) {
        TLRPC.Chat chat = null;
        if (chats != null) {
            chat = chats.get(chatId);
        } else if (sChats != null) {
            chat = sChats.get(chatId);
        }
        if (chat == null) {
            chat = MessagesController.getInstance(currentAccount).getChat(chatId);
        }
        return chat;
    }

    public void updateMessageText() {
        updateMessageText(MessagesController.getInstance(currentAccount).getUsers(), MessagesController.getInstance(currentAccount).getChats(), null, null);
    }

    private void updateMessageText(AbstractMap<Long, TLRPC.User> users, AbstractMap<Long, TLRPC.Chat> chats, LongSparseArray<TLRPC.User> sUsers, LongSparseArray<TLRPC.Chat> sChats) {
        TLRPC.User fromUser = null;
        TLRPC.Chat fromChat = null;
        if (messageOwner.from_id instanceof TLRPC.TL_peerUser) {
            fromUser = getUser(users, sUsers, messageOwner.from_id.user_id);
        } else if (messageOwner.from_id instanceof TLRPC.TL_peerChannel) {
            fromChat = getChat(chats, sChats, messageOwner.from_id.channel_id);
        }
        TLObject fromObject = fromUser != null ? fromUser : fromChat;
        drawServiceWithDefaultTypeface = false;

        channelJoined = false;
        if (messageOwner instanceof TLRPC.TL_messageService) {
            if (messageOwner.action != null) {
                if (messageOwner.action instanceof TLRPC.TL_messageActionNewCreatorPending) {
                    final TLRPC.TL_messageActionNewCreatorPending action = (TLRPC.TL_messageActionNewCreatorPending) messageOwner.action;
                    final TLRPC.User new_creator = getUser(users, sUsers, action.new_creator_id);
                    messageText = getString(R.string.ActionNewCreatorPending);
                    messageText = replaceWithLink(messageText, "un1", new_creator);
                    messageText = replaceWithLink(messageText, "un2", fromObject);
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionChangeCreator) {
                    final TLRPC.TL_messageActionChangeCreator action = (TLRPC.TL_messageActionChangeCreator) messageOwner.action;
                    final TLRPC.User new_creator = getUser(users, sUsers, action.new_creator_id);
                    messageText = getString(R.string.ActionChangeCreator);
                    messageText = replaceWithLink(messageText, "un1", fromObject);
                    messageText = replaceWithLink(messageText, "un2", new_creator);
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionManagedBotCreated) {
                    final TLRPC.TL_messageActionManagedBotCreated action = (TLRPC.TL_messageActionManagedBotCreated) messageOwner.action;
                    final TLRPC.User bot = getUser(users, sUsers, action.bot_id);
                    final TLRPC.User toBot = getUser(users, sUsers, messageOwner.peer_id.user_id);
                    messageText = getString(R.string.ActionManagedBotCreated);
                    messageText = replaceWithLink(messageText, "un1", bot);
                    messageText = replaceWithLink(messageText, "un2", toBot);
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionSetSameChatWallPaper) {
                    contentType = 1;
                    type = TYPE_DATE;
                    TLRPC.TL_messageActionSetSameChatWallPaper action = (TLRPC.TL_messageActionSetSameChatWallPaper) messageOwner.action;
                    TLRPC.User user = getUser(users, sUsers, isOutOwner() ? 0 : getDialogId());
                    photoThumbs = new ArrayList<>();
                    if (action.wallpaper.document != null) {
                        photoThumbs.addAll(action.wallpaper.document.thumbs);
                        photoThumbsObject = action.wallpaper.document;
                    }
                    if (user != null) {
                        if (user.id == UserConfig.getInstance(currentAccount).clientUserId) {
                            messageText = formatString(R.string.ActionSetSameWallpaperForThisChatSelf);
                        } else {
                            messageText = formatString(R.string.ActionSetSameWallpaperForThisChat, user.first_name);
                        }
                    } else if (fromChat != null) {
                        messageText = getString(ChatObject.isChannelAndNotMegaGroup(fromChat) ? R.string.ActionSetWallpaperForThisChannel : R.string.ActionSetWallpaperForThisGroup);
                    } else if (fromUser != null) {
                        messageText = formatString(R.string.ActionSetWallpaperForThisGroupByUser, UserObject.getFirstName(fromUser));
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionSetChatWallPaper) {
                    contentType = 1;
                    TLRPC.TL_messageActionSetChatWallPaper wallPaper = (TLRPC.TL_messageActionSetChatWallPaper) messageOwner.action;
                    type = TYPE_ACTION_WALLPAPER;
                    photoThumbs = new ArrayList<>();
                    if (wallPaper.wallpaper.document != null) {
                        photoThumbs.addAll(wallPaper.wallpaper.document.thumbs);
                        photoThumbsObject = wallPaper.wallpaper.document;
                    }
                    TLRPC.User user = getUser(users, sUsers, isOutOwner() ? 0 : getDialogId());
                    TLRPC.User partner = getUser(users, sUsers, getDialogId());
                    if (user != null) {
                        if (user.id == UserConfig.getInstance(currentAccount).clientUserId) {
                            if (wallPaper.same) {
                                type = TYPE_DATE;
                                messageText = formatString(R.string.ActionSetSameWallpaperForThisChatSelf);
                            } else if (wallPaper.for_both && partner != null) {
                                messageText = getString(R.string.ActionSetWallpaperForThisChatSelfBoth);
                                CharSequence partnerName = new SpannableString(UserObject.getFirstName(partner));
                                ((SpannableString) partnerName).setSpan(new TypefaceSpan(AndroidUtilities.bold()), 0, partnerName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                messageText = AndroidUtilities.replaceCharSequence("%s", messageText, partnerName);
                            } else {
                                messageText = getString(R.string.ActionSetWallpaperForThisChatSelf);
                            }
                        } else {
                            CharSequence userName = new SpannableString(UserObject.getFirstName(user));
                            ((SpannableString) userName).setSpan(new TypefaceSpan(AndroidUtilities.bold()), 0, userName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            if (wallPaper.same) {
                                type = TYPE_DATE;
                                messageText = getString(R.string.ActionSetSameWallpaperForThisChat);
                            } else if (wallPaper.for_both) {
                                messageText = getString(R.string.ActionSetWallpaperForThisChatBoth);
                            } else {
                                messageText = getString(R.string.ActionSetWallpaperForThisChat);
                            }
                            messageText = AndroidUtilities.replaceCharSequence("%s", messageText, userName);
                        }
                    } else if (fromChat != null) {
                        messageText = getString(ChatObject.isChannelAndNotMegaGroup(fromChat) ? R.string.ActionSetWallpaperForThisChannel : R.string.ActionSetWallpaperForThisGroup);
                    } else if (fromUser != null) {
                        messageText = formatString(R.string.ActionSetWallpaperForThisGroupByUser, UserObject.getFirstName(fromUser));
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionGroupCallScheduled) {
                    TLRPC.TL_messageActionGroupCallScheduled action = (TLRPC.TL_messageActionGroupCallScheduled) messageOwner.action;
                    if (messageOwner.peer_id instanceof TLRPC.TL_peerChat || isSupergroup()) {
                        messageText = formatString(R.string.ActionGroupCallScheduled, LocaleController.formatStartsTime(action.schedule_date, 3, false));
                    } else {
                        messageText = formatString(R.string.ActionChannelCallScheduled, LocaleController.formatStartsTime(action.schedule_date, 3, false));
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionGroupCall) {
                    if (messageOwner.action.duration != 0) {
                        String time;
                        int days = messageOwner.action.duration / (3600 * 24);
                        if (days > 0) {
                            time = formatPluralString("Days", days);
                        } else {
                            int hours = messageOwner.action.duration / 3600;
                            if (hours > 0) {
                                time = formatPluralString("Hours", hours);
                            } else {
                                int minutes = messageOwner.action.duration / 60;
                                if (minutes > 0) {
                                    time = formatPluralString("Minutes", minutes);
                                } else {
                                    time = formatPluralString("Seconds", messageOwner.action.duration);
                                }
                            }
                        }

                        if (messageOwner.peer_id instanceof TLRPC.TL_peerChat || isSupergroup()) {
                            if (isOut()) {
                                messageText = formatString(R.string.ActionGroupCallEndedByYou, time);
                            } else {
                                messageText = replaceWithLink(formatString(R.string.ActionGroupCallEndedBy, time), "un1", fromObject);
                            }
                        } else {
                            messageText = formatString(R.string.ActionChannelCallEnded, time);
                        }
                    } else {
                        if (messageOwner.peer_id instanceof TLRPC.TL_peerChat || isSupergroup()) {
                            if (isOut()) {
                                messageText = getString(R.string.ActionGroupCallStartedByYou);
                            } else {
                                messageText = replaceWithLink(getString(R.string.ActionGroupCallStarted), "un1", fromObject);
                            }
                        } else {
                            messageText = getString(R.string.ActionChannelCallJustStarted);
                        }
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionInviteToGroupCall) {
                    long singleUserId = messageOwner.action.user_id;
                    if (singleUserId == 0 && messageOwner.action.users.size() == 1) {
                        singleUserId = messageOwner.action.users.get(0);
                    }
                    if (singleUserId != 0) {
                        TLRPC.User whoUser = getUser(users, sUsers, singleUserId);

                        if (isOut()) {
                            messageText = replaceWithLink(getString(R.string.ActionGroupCallYouInvited), "un2", whoUser);
                        } else if (singleUserId == UserConfig.getInstance(currentAccount).getClientUserId()) {
                            messageText = replaceWithLink(getString(R.string.ActionGroupCallInvitedYou), "un1", fromObject);
                        } else {
                            messageText = replaceWithLink(getString(R.string.ActionGroupCallInvited), "un2", whoUser);
                            messageText = replaceWithLink(messageText, "un1", fromObject);
                        }
                    } else {
                        if (isOut()) {
                            messageText = replaceWithLink(getString(R.string.ActionGroupCallYouInvited), "un2", messageOwner.action.users, users, sUsers);
                        } else {
                            messageText = replaceWithLink(getString(R.string.ActionGroupCallInvited), "un2", messageOwner.action.users, users, sUsers);
                            messageText = replaceWithLink(messageText, "un1", fromObject);
                        }
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionGeoProximityReached) {
                    TLRPC.TL_messageActionGeoProximityReached action = (TLRPC.TL_messageActionGeoProximityReached) messageOwner.action;
                    long fromId = getPeerId(action.from_id);
                    TLObject from;
                    if (fromId > 0) {
                        from = getUser(users, sUsers, fromId);
                    } else {
                        from = getChat(chats, sChats, -fromId);
                    }
                    long toId = getPeerId(action.to_id);
                    long selfUserId = UserConfig.getInstance(currentAccount).getClientUserId();
                    if (toId == selfUserId) {
                        messageText = replaceWithLink(formatString(R.string.ActionUserWithinRadius, LocaleController.formatDistance(action.distance, 2)), "un1", from);
                    } else {
                        TLObject to;
                        if (toId > 0) {
                            to = getUser(users, sUsers, toId);
                        } else {
                            to = getChat(chats, sChats, -toId);
                        }
                        if (fromId == selfUserId) {
                            messageText = replaceWithLink(formatString(R.string.ActionUserWithinYouRadius, LocaleController.formatDistance(action.distance, 2)), "un1", to);
                        } else {
                            messageText = replaceWithLink(formatString(R.string.ActionUserWithinOtherRadius, LocaleController.formatDistance(action.distance, 2)), "un2", to);
                            messageText = replaceWithLink(messageText, "un1", from);
                        }
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionCustomAction) {
                    messageText = messageOwner.action.message;
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionChatCreate) {
                    if (isOut()) {
                        messageText = getString(R.string.ActionYouCreateGroup);
                    } else {
                        messageText = replaceWithLink(getString(R.string.ActionCreateGroup), "un1", fromObject);
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionChatDeleteUser) {
                    if (isFromUser() && messageOwner.action.user_id == messageOwner.from_id.user_id) {
                        if (isOut()) {
                            messageText = getString(R.string.ActionYouLeftUser);
                        } else {
                            messageText = replaceWithLink(getString(R.string.ActionLeftUser), "un1", fromObject);
                        }
                    } else {
                        TLRPC.User whoUser = getUser(users, sUsers, messageOwner.action.user_id);
                        if (isOut()) {
                            messageText = replaceWithLink(getString(R.string.ActionYouKickUser), "un2", whoUser);
                        } else if (messageOwner.action.user_id == UserConfig.getInstance(currentAccount).getClientUserId()) {
                            messageText = replaceWithLink(getString(R.string.ActionKickUserYou), "un1", fromObject);
                        } else {
                            messageText = replaceWithLink(getString(R.string.ActionKickUser), "un2", whoUser);
                            messageText = replaceWithLink(messageText, "un1", fromObject);
                        }
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionPaymentRefunded) {
                    TLRPC.TL_messageActionPaymentRefunded action = (TLRPC.TL_messageActionPaymentRefunded) messageOwner.action;
                    long did = DialogObject.getPeerDialogId(action.peer);
                    TLObject who;
                    if (did >= 0) {
                        who = getUser(users, sUsers, did);
                    } else {
                        who = getChat(chats, sChats, -did);
                    }
                    messageText = StarsIntroActivity.replaceStars(replaceWithLink(formatString(R.string.ActionRefunded, action.currency + " " + LocaleController.formatNumber(action.total_amount, ',')), "un1", who));
                } else if (TlUtils.isInstance(messageOwner.action, TLRPC.TL_messageActionSuggestedPostRefund.class, TLRPC.TL_messageActionSuggestedPostSuccess.class)) {
                    String channelName = ForumUtilities.getMonoForumTitle(currentAccount, DialogObject.getPeerDialogId(messageOwner.peer_id), true);
                    if (channelName == null) {
                        TLRPC.Chat chat = getChat(chats, sChats, -DialogObject.getPeerDialogId(messageOwner.peer_id));
                        if (chat != null && chat.linked_monoforum_id != 0) {
                            TLRPC.Chat chat2 = getChat(chats, sChats, chat.linked_monoforum_id);
                            if (chat2 != null) {
                                chat = chat2;
                            }
                        }

                        channelName = DialogObject.getDialogTitle(chat);
                    }
                    final String userName = DialogObject.getName(getUser(users, sUsers, DialogObject.getPeerDialogId(messageOwner.saved_peer_id)));
                    final MessageSuggestionParams sp = obtainSuggestionOfferFromReply();

                    if (messageOwner.action instanceof TLRPC.TL_messageActionSuggestedPostRefund) {
                        final boolean refundByUser = ((TLRPC.TL_messageActionSuggestedPostRefund) messageOwner.action).payer_initiated;
                        if (sp != null && sp.amount != null) {
                            final int key = refundByUser ? R.string.SuggestedOfferRefundByUserAmountF : R.string.SuggestedOfferRefundByAdminAmountF;
                            messageText = StarsIntroActivity.replaceStars(sp.amount.currency == AmountUtils.Currency.TON,
                                LocaleController.formatString(key, userName, channelName, sp.amount.asDecimalString()));
                        } else {
                            final int key = refundByUser ?
                                R.string.SuggestedOfferRefundByUserAmountUnknown :
                                R.string.SuggestedOfferRefundByAdminAmountUnknown;

                            messageText = LocaleController.formatString(key, userName, channelName);
                        }
                    } else if (messageOwner.action instanceof TLRPC.TL_messageActionSuggestedPostSuccess) {
                        if (sp != null && sp.amount != null) {
                            messageText = StarsIntroActivity.replaceStars(sp.amount.currency == AmountUtils.Currency.TON,
                                LocaleController.formatString(R.string.SuggestedOfferCompleteAmountF, channelName, sp.amount.asDecimalString()));
                        } else {
                            messageText = LocaleController.formatString(R.string.SuggestedOfferCompleteAmountUnknown, channelName);
                        }
                    }

                } else if (messageOwner.action instanceof TLRPC.TL_messageActionChatAddUser) {
                    long singleUserId = messageOwner.action.user_id;
                    if (singleUserId == 0 && messageOwner.action.users.size() == 1) {
                        singleUserId = messageOwner.action.users.get(0);
                    }
                    if (singleUserId != 0) {
                        TLRPC.User whoUser = getUser(users, sUsers, singleUserId);
                        TLRPC.Chat chat = null;
                        if (messageOwner.peer_id.channel_id != 0) {
                            chat = getChat(chats, sChats, messageOwner.peer_id.channel_id);
                        }
                        if (messageOwner.from_id != null && singleUserId == messageOwner.from_id.user_id) {
                            if (ChatObject.isChannel(chat) && !chat.megagroup) {
                                channelJoined = true;
                                messageText = getString(R.string.ChannelJoined);
                            } else {
                                if (messageOwner.peer_id.channel_id != 0) {
                                    if (singleUserId == UserConfig.getInstance(currentAccount).getClientUserId()) {
                                        messageText = getString(R.string.ChannelMegaJoined);
                                    } else {
                                        messageText = replaceWithLink(getString(R.string.ActionAddUserSelfMega), "un1", fromObject);
                                    }
                                } else if (isOut()) {
                                    messageText = getString(R.string.ActionAddUserSelfYou);
                                } else {
                                    messageText = replaceWithLink(getString(R.string.ActionAddUserSelf), "un1", fromObject);
                                }
                            }
                        } else {
                            if (isOut()) {
                                messageText = replaceWithLink(getString(R.string.ActionYouAddUser), "un2", whoUser);
                            } else if (singleUserId == UserConfig.getInstance(currentAccount).getClientUserId()) {
                                if (messageOwner.peer_id.channel_id != 0) {
                                    if (chat != null && chat.megagroup) {
                                        messageText = replaceWithLink(getString(R.string.MegaAddedBy), "un1", fromObject);
                                    } else {
                                        messageText = replaceWithLink(getString(R.string.ChannelAddedBy), "un1", fromObject);
                                    }
                                } else {
                                    messageText = replaceWithLink(getString(R.string.ActionAddUserYou), "un1", fromObject);
                                }
                            } else {
                                messageText = replaceWithLink(getString(R.string.ActionAddUser), "un2", whoUser);
                                messageText = replaceWithLink(messageText, "un1", fromObject);
                            }
                        }
                    } else {
                        if (isOut()) {
                            messageText = replaceWithLink(getString(R.string.ActionYouAddUser), "un2", messageOwner.action.users, users, sUsers);
                        } else {
                            messageText = replaceWithLink(getString(R.string.ActionAddUser), "un2", messageOwner.action.users, users, sUsers);
                            messageText = replaceWithLink(messageText, "un1", fromObject);
                        }
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionChatJoinedViaCommunity) {
                    final TLRPC.Chat communityChat = getChat(chats, sChats, ((TLRPC.TL_messageActionChatJoinedViaCommunity) messageOwner.action).community_id);
                    final TLRPC.Chat peer = getChat(chats, sChats, -DialogObject.getPeerDialogId(messageOwner.peer_id));
                    final boolean isChannel = ChatObject.isChannelAndNotMegaGroup(peer);
                    if (isOut()) {
                        messageText = replaceWithLink(getString(isChannel ?
                            R.string.ActionJoinedFromCommunityYouChannel :
                            R.string.ActionJoinedFromCommunityYou
                        ), "un1", communityChat);
                    } else {
                        messageText = replaceWithLink(getString(R.string.ActionJoinedFromCommunityUser), "un1", communityChat);
                        messageText = replaceWithLink(messageText, "un2", fromObject);
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionChatJoinedByLink) {
                    if (isOut()) {
                        messageText = getString(R.string.ActionInviteYou);
                    } else {
                        messageText = replaceWithLink(getString(R.string.ActionInviteUser), "un1", fromObject);
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionGiveawayLaunch) {
                    TLRPC.TL_messageActionGiveawayLaunch giveawayLaunch = (TLRPC.TL_messageActionGiveawayLaunch) messageOwner.action;
                    TLRPC.Chat chat = messageOwner.peer_id != null && messageOwner.peer_id.channel_id != 0 ? getChat(chats, sChats, messageOwner.peer_id.channel_id) : null;
                    boolean isChannel = ChatObject.isChannelAndNotMegaGroup(chat);
                    boolean isStars = (giveawayLaunch.flags & 1) != 0;
                    if (isStars) {
                        messageText = formatPluralStringComma(isChannel ? "BoostingStarsGiveawayJustStarted" : "BoostingStarsGiveawayJustStartedGroup", (int) giveawayLaunch.stars, chat != null ? chat.title : "");
                    } else {
                        messageText = formatString(isChannel ? R.string.BoostingGiveawayJustStarted : R.string.BoostingGiveawayJustStartedGroup, chat != null ? chat.title : "");
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionBoostApply) {
                    TLRPC.Chat chat = messageOwner.peer_id != null && messageOwner.peer_id.channel_id != 0 ? getChat(chats, sChats, messageOwner.peer_id.channel_id) : null;
                    boolean isChannel = ChatObject.isChannelAndNotMegaGroup(chat);
                    TLRPC.TL_messageActionBoostApply messageActionBoostApply = (TLRPC.TL_messageActionBoostApply) messageOwner.action;
                    String name = "";
                    boolean self = false;
                    if (fromObject instanceof TLRPC.User) {
                        TLRPC.User user = (TLRPC.User) fromObject;
                        self = UserObject.isUserSelf(user);
                        name = UserObject.getFirstName(user);
                    } else if (fromObject instanceof TLRPC.Chat) {
                        name = ((TLRPC.Chat) fromObject).title;
                    }
                    if (self) {
                        if (messageActionBoostApply.boosts <= 1) {
                            messageText = getString(isChannel ? R.string.BoostingBoostsChannelByYouServiceMsg : R.string.BoostingBoostsGroupByYouServiceMsg);
                        } else {
                            messageText = LocaleController.formatPluralString(isChannel ? "BoostingBoostsChannelByYouServiceMsgCount" : "BoostingBoostsGroupByYouServiceMsgCount", messageActionBoostApply.boosts);
                        }
                    } else {
                        if (messageActionBoostApply.boosts <= 1) {
                            messageText = formatString(isChannel ? R.string.BoostingBoostsChannelByUserServiceMsg : R.string.BoostingBoostsGroupByUserServiceMsg, name);
                        } else {
                            messageText = LocaleController.formatPluralString(isChannel ? "BoostingBoostsChannelByUserServiceMsgCount" : "BoostingBoostsGroupByUserServiceMsgCount", messageActionBoostApply.boosts, name);
                        }
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionGiveawayResults) {
                    TLRPC.Chat chat = messageOwner.peer_id != null && messageOwner.peer_id.channel_id != 0 ? getChat(chats, sChats, messageOwner.peer_id.channel_id) : null;
                    boolean isChannel = ChatObject.isChannelAndNotMegaGroup(chat);
                    TLRPC.TL_messageActionGiveawayResults giveawayResults = (TLRPC.TL_messageActionGiveawayResults) messageOwner.action;
                    SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
                    if (giveawayResults.stars) {
                        stringBuilder.append(formatPluralStringComma("BoostingStarsGiveawayServiceWinnersSelected", giveawayResults.winners_count));
                        if (giveawayResults.unclaimed_count > 0) {
                            stringBuilder.append("\n");
                            stringBuilder.append(LocaleController.formatPluralString(isChannel ? "BoostingStarsGiveawayServiceUndistributed" : "BoostingStarsGiveawayServiceUndistributedGroup", giveawayResults.unclaimed_count));
                        }
                    } else {
                        stringBuilder.append(LocaleController.formatPluralString("BoostingGiveawayServiceWinnersSelected", giveawayResults.winners_count));
                        if (giveawayResults.unclaimed_count > 0) {
                            stringBuilder.append("\n");
                            stringBuilder.append(LocaleController.formatPluralString(isChannel ? "BoostingGiveawayServiceUndistributed" : "BoostingGiveawayServiceUndistributedGroup", giveawayResults.unclaimed_count));
                        }
                    }
                    messageText = stringBuilder;
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionPrizeStars) {
                    final TLRPC.TL_messageActionPrizeStars action = (TLRPC.TL_messageActionPrizeStars) messageOwner.action;
                    final long chatId = -DialogObject.getPeerDialogId(action.boost_peer);
                    final TLRPC.Chat chat = getChat(chats, sChats, chatId);
                    messageText = replaceWithLink(AndroidUtilities.replaceTags(formatPluralStringComma("ActionStarGiveawayPrize", (int) action.stars)), "un1", chat);
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionStarGift) {
                    final TLRPC.TL_messageActionStarGift action = (TLRPC.TL_messageActionStarGift) messageOwner.action;
                    int stars = 0;
                    if (action.prepaid_upgrade) {
                        stars += (int) action.upgrade_stars;
                    } else {
                        if (action.gift != null) {
                            stars = (int) action.gift.stars;
                        }
                        if (!action.upgrade_separate) {
                            stars += (int) action.upgrade_stars;
                        }
                    }
//                    if (action.can_upgrade && action.upgrade_stars == 0) {
//                    }
                    final boolean isForChannel = action.peer != null && DialogObject.getPeerDialogId(action.peer) < 0;
                    TLRPC.User user = getUser(users, sUsers, messageOwner.peer_id.user_id);
                    TLObject obj = fromObject;
                    if (!action.prepaid_upgrade && action.from_id != null) {
                        final long fromId = DialogObject.getPeerDialogId(action.from_id);
                        if (fromId >= 0) {
                            obj = getUser(users, sUsers, fromId);
                        } else {
                            obj = getChat(chats, sChats, -fromId);
                        }
                    }
                    TLObject peerObj = null;
                    if (action.peer != null) {
                        final long peerId = DialogObject.getPeerDialogId(action.peer);
                        if (peerId >= 0) {
                            peerObj = getUser(users, sUsers, peerId);
                        } else {
                            peerObj = getChat(chats, sChats, -peerId);
                        }
                    }
                    if (action.prepaid_upgrade) {
                        if (obj instanceof TLRPC.User && ((TLRPC.User) obj).self && !action.forceIn) {
                            messageText = replaceWithLink(AndroidUtilities.replaceTags(getString(R.string.ActionPrepaidGiftOutbound)), "un1", user);
                        } else {
                            messageText = replaceWithLink(AndroidUtilities.replaceTags(getString(R.string.ActionPrepaidGiftInbound)), "un1", obj);
                        }
                    } else if (UserObject.isService(getDialogId()) && action.from_id == null) {
                        messageText = AndroidUtilities.replaceTags(getString(action.auction_acquired ? R.string.ActionGiftAuctionSelf : R.string.ActionGiftSomeone));
                        messageTextShort = getString(R.string.ActionStarGift);
                    } else if (isForChannel) {
                        messageText = AndroidUtilities.replaceTags(formatPluralStringComma("ActionGiftChannel", stars));
                        messageText = replaceWithLink(messageText, "un1", obj);
                        messageText = replaceWithLink(messageText, "un2", peerObj);
                        messageTextShort = getString(R.string.ActionStarGift);
                    } else if (UserObject.isUserSelf(user)) {
                        messageText = AndroidUtilities.replaceTags(getString(action.auction_acquired ? R.string.ActionGiftAuctionSelf : R.string.ActionGiftSelf));
                        messageTextShort = getString(R.string.ActionStarGift);
                    } else if (obj instanceof TLRPC.User && ((TLRPC.User) obj).self && !action.forceIn) {
                        messageText = replaceWithLink(AndroidUtilities.replaceTags(getString(R.string.ActionGiftOutbound)), "un1", user);
                        if (action.message != null && !TextUtils.isEmpty(action.message.text)) {
                            SpannableStringBuilder stringBuilder = new SpannableStringBuilder(action.message.text);
                            addEntitiesToText(stringBuilder, action.message.entities, isOutOwner(), false, false, false);
                            messageTextShort = stringBuilder;
                        } else {
                            messageTextShort = getString(R.string.ActionStarGift);
                        }
                    } else if (obj instanceof TLRPC.User && UserObject.isService(((TLRPC.User) obj).id)) {
                        messageText = TextUtils.replace(AndroidUtilities.replaceTags(getString(R.string.ActionGiftInbound)), new String[] {"un1"}, new CharSequence[]{ getString(R.string.StarsTransactionUnknown) });
                    } else {
                        messageText = replaceWithLink(AndroidUtilities.replaceTags(getString(R.string.ActionGiftInbound)), "un1", obj);
                        if (action.message != null && !TextUtils.isEmpty(action.message.text)) {
                            SpannableStringBuilder stringBuilder = new SpannableStringBuilder(action.message.text);
                            addEntitiesToText(stringBuilder, action.message.entities, isOutOwner(), false, false, false);
                            messageTextShort = stringBuilder;
                        } else {
                            messageTextShort = getString(R.string.ActionStarGift);
                        }
                    }
                    int i = messageText.toString().indexOf("un2");
                    if (i != -1) {
                        SpannableStringBuilder sb = SpannableStringBuilder.valueOf(messageText);
                        messageText = sb.replace(i, i + 3, formatPluralStringComma("Gift2StarsCount", (int) stars));
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionStarGiftUnique) {
                    TLRPC.TL_messageActionStarGiftUnique action = (TLRPC.TL_messageActionStarGiftUnique) messageOwner.action;
                    TLRPC.User user = getUser(users, sUsers, getDialogId());
                    if (action.resale_amount != null) {
                        final AmountUtils.Amount amount = AmountUtils.Amount.ofSafe(action.resale_amount);
                        long fromId = getDialogId();
                        if (action.from_id != null) {
                            fromId = DialogObject.getPeerDialogId(action.from_id);
                        }
                        TLObject obj;
                        if (fromId >= 0) {
                            obj = getUser(users, sUsers, fromId);
                        } else {
                            obj = getChat(chats, sChats, -fromId);
                        }
                        if (action.craft) {
                            messageText = AndroidUtilities.replaceTags(getString(R.string.ActionUniqueGiftCrafted));
                        } else if (action.peer != null) {
                            long peerId = DialogObject.getPeerDialogId(action.peer);
                            TLObject peer;
                            if (peerId >= 0) {
                                peer = getUser(users, sUsers, peerId);
                            } else {
                                peer = getChat(chats, sChats, -peerId);
                            }
                            if (amount.currency == AmountUtils.Currency.TON) {
                                messageText = AndroidUtilities.replaceTags(LocaleController.formatString(R.string.ActionUniqueGiftResaleServiceTON, amount.asFormatString()));
                            } else {
                                messageText = AndroidUtilities.replaceTags(formatPluralStringComma("ActionUniqueGiftResaleService", (int) amount.asDecimal()));
                            }
                            messageText = replaceWithLink(messageText, "un1", obj);
                            messageText = replaceWithLink(messageText, "un2", peer);
                        } else if (action.from_offer) {
                            if (amount.currency == AmountUtils.Currency.TON) {
                                messageText = AndroidUtilities.replaceTags(formatString(isOutOwner() ? R.string.ActionUniqueGiftResaleSoldOutboundTON : R.string.ActionUniqueGiftResaleOutboundTON, amount.asFormatString()));
                            } else {
                                messageText = AndroidUtilities.replaceTags(formatPluralStringComma(isOutOwner() ? "ActionUniqueGiftResaleSoldOutbound" : "ActionUniqueGiftResaleOutbound", (int) amount.asDecimal()));
                            }
                        } else {
                            if (amount.currency == AmountUtils.Currency.TON) {
                                messageText = replaceWithLink(AndroidUtilities.replaceTags(formatString(isOutOwner() ? R.string.ActionUniqueGiftResaleOutboundTON : R.string.ActionUniqueGiftResaleInboundTON, amount.asFormatString())), "un1", obj);
                            } else {
                                messageText = replaceWithLink(AndroidUtilities.replaceTags(formatPluralStringComma(isOutOwner() ? "ActionUniqueGiftResaleOutbound" : "ActionUniqueGiftResaleInbound", (int) amount.asDecimal())), "un1", obj);
                            }
                        }
                    } else if (action.upgrade) {
                        if (action.peer != null) {
                            long peerId = DialogObject.getPeerDialogId(action.peer);
                            TLObject peer;
                            if (peerId >= 0) {
                                peer = getUser(users, sUsers, peerId);
                            } else {
                                peer = getChat(chats, sChats, -peerId);
                            }
                            messageText = replaceWithLink(AndroidUtilities.replaceTags(getString(R.string.ActionUniqueGiftUpgradeInboundChannel)), "un1", peer);
                        } else if (UserObject.isUserSelf(user)) {
                            messageText = AndroidUtilities.replaceTags(getString(R.string.ActionUniqueGiftUpgradeSelf));
                        } else {
                            messageText = replaceWithLink(AndroidUtilities.replaceTags(getString(isOutOwner() ? R.string.ActionUniqueGiftUpgradeOutbound : R.string.ActionUniqueGiftUpgradeInbound)), "un1", user);
                        }
                    } else {
                        long fromId = getDialogId();
                        if (action.from_id != null) {
                            fromId = DialogObject.getPeerDialogId(action.from_id);
                        }
                        TLObject obj;
                        if (fromId >= 0) {
                            obj = getUser(users, sUsers, fromId);
                        } else {
                            obj = getChat(chats, sChats, -fromId);
                        }
                        if (action.craft) {
                            messageText = AndroidUtilities.replaceTags(getString(R.string.ActionUniqueGiftCrafted));
                        } else if (action.peer != null) {
                            long peerId = DialogObject.getPeerDialogId(action.peer);
                            TLObject peer;
                            if (peerId >= 0) {
                                peer = getUser(users, sUsers, peerId);
                            } else {
                                peer = getChat(chats, sChats, -peerId);
                            }
                            messageText = AndroidUtilities.replaceTags(getString(R.string.ActionUniqueGiftTransferService));
                            messageText = replaceWithLink(messageText, "un1", obj);
                            messageText = replaceWithLink(messageText, "un2", peer);
                        } else if (action.assigned) {
                            String giftName = action.gift.title + " #" + LocaleController.formatNumber(action.gift.num, ',');
                            messageText = replaceWithLink(AndroidUtilities.replaceTags(formatString(R.string.ActionUniqueGiftTransferOutboundAssigned, giftName)), "un1", obj);
                        } else {
                            messageText = replaceWithLink(AndroidUtilities.replaceTags(getString(isOutOwner() ? R.string.ActionUniqueGiftTransferOutbound : R.string.ActionUniqueGiftTransferInbound)), "un1", obj);
                        }
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionGiftStars) {
                    if (fromObject instanceof TLRPC.User && ((TLRPC.User) fromObject).self) {
                        TLRPC.User user = getUser(users, sUsers, messageOwner.peer_id.user_id);
                        messageText = replaceWithLink(AndroidUtilities.replaceTags(getString(R.string.ActionGiftOutbound)), "un1", user);
                    } else if (fromObject instanceof TLRPC.User && UserObject.isService(((TLRPC.User) fromObject).id)) {
                        messageText = TextUtils.replace(AndroidUtilities.replaceTags(getString(R.string.ActionGiftInbound)), new String[] {"un1"}, new CharSequence[]{ getString(R.string.StarsTransactionUnknown) });
                    } else {
                        messageText = replaceWithLink(AndroidUtilities.replaceTags(getString(R.string.ActionGiftInbound)), "un1", fromObject);
                    }
                    int i = messageText.toString().indexOf("un2");
                    if (i != -1) {
                        SpannableStringBuilder sb = SpannableStringBuilder.valueOf(messageText);
                        CharSequence price = BillingController.getInstance().formatCurrency(messageOwner.action.amount, messageOwner.action.currency);
                        if ((messageOwner.action.flags & 1) != 0) {
                            price = String.format("%.2f", (messageOwner.action.cryptoAmount * Math.pow(10, -9))) + " " + messageOwner.action.cryptoCurrency + " (~ " + price + ")";
                        }
                        messageText = sb.replace(i, i + 3, price);
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionGiftCode && ((TLRPC.TL_messageActionGiftCode) messageOwner.action).boost_peer != null) {
                    messageText = getString(R.string.BoostingReceivedGiftNoName);
                } else if (TlUtils.isInstance(messageOwner.action, TLRPC.TL_messageActionGiftPremium.class, TLRPC.TL_messageActionGiftCode.class, TLRPC.TL_messageActionGiftTon.class)) {
                    if (fromObject instanceof TLRPC.User && ((TLRPC.User) fromObject).self) {
                        TLRPC.User user = getUser(users, sUsers, messageOwner.peer_id.user_id);
                        messageText = replaceWithLink(AndroidUtilities.replaceTags(getString(R.string.ActionGiftOutbound)), "un1", user);
                    } else if (messageOwner.action instanceof TLRPC.TL_messageActionGiftTon) {
                        messageText = AndroidUtilities.replaceTags(getString(R.string.ActionGiftTonInbound));
                    } else {
                        messageText = replaceWithLink(AndroidUtilities.replaceTags(getString(R.string.ActionGiftInbound)), "un1", fromObject);
                    }
                    int i = messageText.toString().indexOf("un2");
                    if (i != -1) {
                        SpannableStringBuilder sb = SpannableStringBuilder.valueOf(messageText);
                        CharSequence price = BillingController.getInstance().formatCurrency(messageOwner.action.amount, messageOwner.action.currency);
                        if ((messageOwner.action.flags & 1) != 0) {
                            price = String.format("%.2f", (messageOwner.action.cryptoAmount * Math.pow(10, -9))) + " " + messageOwner.action.cryptoCurrency + " (~ " + price + ")";
                        }
                        messageText = sb.replace(i, i + 3, price);
                    }
                    messageText = StarsIntroActivity.replaceStars(messageText);
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionSuggestBirthday) {
                    if (isOutOwner()) {
                        messageText = getString(R.string.ActionYouSuggestBirthday);
                    } else {
                        messageText = replaceWithLink(AndroidUtilities.replaceTags(getString(R.string.ActionSuggestBirthday)), "un1", fromObject);
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionSuggestProfilePhoto) {
                    if (messageOwner.action.photo != null && messageOwner.action.photo.video_sizes != null && !messageOwner.action.photo.video_sizes.isEmpty()) {
                        messageText = getString(R.string.ActionSuggestVideoShort);
                    } else {
                        messageText = getString(R.string.ActionSuggestPhotoShort);
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionChatEditPhoto) {
                    TLRPC.Chat chat = messageOwner.peer_id != null && messageOwner.peer_id.channel_id != 0 ? getChat(chats, sChats, messageOwner.peer_id.channel_id) : null;
                    if (ChatObject.isChannel(chat) && !chat.megagroup) {
                        if (isVideoAvatar()) {
                            messageText = getString(R.string.ActionChannelChangedVideo);
                        } else {
                            messageText = getString(R.string.ActionChannelChangedPhoto);
                        }
                    } else {
                        if (isOut()) {
                            if (isVideoAvatar()) {
                                messageText = getString(R.string.ActionYouChangedVideo);
                            } else {
                                messageText = getString(R.string.ActionYouChangedPhoto);
                            }
                        } else {
                            if (isVideoAvatar()) {
                                messageText = replaceWithLink(getString(R.string.ActionChangedVideo), "un1", fromObject);
                            } else {
                                messageText = replaceWithLink(getString(R.string.ActionChangedPhoto), "un1", fromObject);
                            }
                        }
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionChatEditTitle) {
                    TLRPC.Chat chat = messageOwner.peer_id != null && messageOwner.peer_id.channel_id != 0 ? getChat(chats, sChats, messageOwner.peer_id.channel_id) : null;
                    if (ChatObject.isChannel(chat) && !chat.megagroup) {
                        messageText = getString(R.string.ActionChannelChangedTitle).replace("un2", messageOwner.action.title);
                    } else {
                        if (isOut()) {
                            messageText = getString(R.string.ActionYouChangedTitle).replace("un2", messageOwner.action.title);
                        } else {
                            messageText = replaceWithLink(getString(R.string.ActionChangedTitle).replace("un2", messageOwner.action.title), "un1", fromObject);
                        }
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionChatDeletePhoto) {
                    TLRPC.Chat chat = messageOwner.peer_id != null && messageOwner.peer_id.channel_id != 0 ? getChat(chats, sChats, messageOwner.peer_id.channel_id) : null;
                    if (ChatObject.isChannel(chat) && !chat.megagroup) {
                        messageText = getString(R.string.ActionChannelRemovedPhoto);
                    } else {
                        if (isOut()) {
                            messageText = getString(R.string.ActionYouRemovedPhoto);
                        } else {
                            messageText = replaceWithLink(getString(R.string.ActionRemovedPhoto), "un1", fromObject);
                        }
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionTTLChange) {
                    if (messageOwner.action.ttl != 0) {
                        if (isOut()) {
                            messageText = formatString(R.string.MessageLifetimeChangedOutgoing, LocaleController.formatTTLString(messageOwner.action.ttl));
                        } else {
                            messageText = formatString(R.string.MessageLifetimeChanged, UserObject.getFirstName(fromUser), LocaleController.formatTTLString(messageOwner.action.ttl));
                        }
                    } else {
                        if (isOut()) {
                            messageText = getString(R.string.MessageLifetimeYouRemoved);
                        } else {
                            messageText = formatString(R.string.MessageLifetimeRemoved, UserObject.getFirstName(fromUser));
                        }
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionRequestedPeer) {
                    List<TLObject> peerObjects = new ArrayList<>();
                    int sharedUsers = 0;
                    int sharedChannels = 0;
                    int sharedChats = 0;
                    List<TLRPC.Peer> peers = ((TLRPC.TL_messageActionRequestedPeer) messageOwner.action).peers;
                    for (TLRPC.Peer peer : peers) {
                        TLObject peerObject = null;
                        if (peer instanceof TLRPC.TL_peerUser) {
                            peerObject = MessagesController.getInstance(currentAccount).getUser(peer.user_id);
                            if (peerObject == null) {
                                peerObject = getUser(users, sUsers, peer.user_id);
                            }
                        } else if (peer instanceof TLRPC.TL_peerChat) {
                            peerObject = MessagesController.getInstance(currentAccount).getChat(peer.chat_id);
                            if (peerObject == null) {
                                peerObject = getChat(chats, sChats, peer.chat_id);
                            }
                        } else if (peer instanceof TLRPC.TL_peerChannel) {
                            peerObject = MessagesController.getInstance(currentAccount).getChat(peer.channel_id);
                            if (peerObject == null) {
                                peerObject = getChat(chats, sChats, peer.channel_id);
                            }
                        }
                        if (peer instanceof TLRPC.TL_peerUser) {
                            sharedUsers++;
                        } else if (peer instanceof TLRPC.TL_peerChat) {
                            sharedChats++;
                        } else {
                            sharedChannels++;
                        }
                        if (peerObject != null) {
                            peerObjects.add(peerObject);
                        }
                    }
                    if (sharedUsers > 0 && sharedUsers != peerObjects.size()) {
                        messageText = LocaleController.getPluralString("ActionRequestedPeerUserPlural", sharedUsers);
                    } else if (sharedChannels > 0 && sharedChannels != peerObjects.size()) {
                        messageText = LocaleController.getPluralString("ActionRequestedPeerChannelPlural", sharedChannels);
                    } else if (sharedChats > 0 && sharedChats != peerObjects.size()) {
                        messageText = LocaleController.getPluralString("ActionRequestedPeerChatPlural", sharedChats);
                    } else {
                        String separator = ", ";
                        SpannableStringBuilder names = new SpannableStringBuilder();
                        for (int i = 0; i < peerObjects.size(); i++) {
                            names.append(replaceWithLink("un1", "un1", peerObjects.get(i)));
                            if (i < peerObjects.size() - 1) {
                                names.append(separator);
                            }
                        }
                        messageText = AndroidUtilities.replaceCharSequence("un1", getString(R.string.ActionRequestedPeer), names);
                    }
                    TLRPC.User bot = MessagesController.getInstance(currentAccount).getUser(getDialogId());
                    if (bot == null) {
                        bot = getUser(users, sUsers, getDialogId());
                    }
                    messageText = replaceWithLink(messageText, "un2", bot);
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionSetMessagesTTL) {
                    TLRPC.TL_messageActionSetMessagesTTL action = (TLRPC.TL_messageActionSetMessagesTTL) messageOwner.action;
                    TLRPC.Chat chat = messageOwner.peer_id != null && messageOwner.peer_id.channel_id != 0 ? getChat(chats, sChats, messageOwner.peer_id.channel_id) : null;
                    if (chat != null && !chat.megagroup) {
                        if (action.period != 0) {
                            messageText = formatString(R.string.ActionTTLChannelChanged, LocaleController.formatTTLString(action.period));
                        } else {
                            messageText = getString(R.string.ActionTTLChannelDisabled);
                        }
                    } else if (action.auto_setting_from != 0) {
                        drawServiceWithDefaultTypeface = true;
                        if (action.auto_setting_from == UserConfig.getInstance(currentAccount).clientUserId) {
                            messageText = AndroidUtilities.replaceTags(formatString(R.string.AutoDeleteGlobalActionFromYou, LocaleController.formatTTLString(action.period)));
                        } else {
                            TLObject object = null;
                            if (sUsers != null) {
                                object = sUsers.get(action.auto_setting_from);
                            }
                            if (object == null && users != null) {
                                object = users.get(action.auto_setting_from);
                            }
                            if (object == null && chats != null) {
                                object = chats.get(action.auto_setting_from);
                            }
                            if (object == null) {
                                if (action.auto_setting_from > 0) {
                                    object = MessagesController.getInstance(currentAccount).getUser(action.auto_setting_from);
                                } else {
                                    object = MessagesController.getInstance(currentAccount).getChat(-action.auto_setting_from);
                                }
                            }
                            if (object == null) {
                                object = fromObject;
                            }
                            messageText = replaceWithLink(AndroidUtilities.replaceTags(formatString(R.string.AutoDeleteGlobalAction, LocaleController.formatTTLString(action.period))), "un1", object);
                        }
                    } else if (action.period != 0) {
                        if (isOut()) {
                            messageText = formatString(R.string.ActionTTLYouChanged, LocaleController.formatTTLString(action.period));
                        } else {
                            messageText = replaceWithLink(formatString(R.string.ActionTTLChanged, LocaleController.formatTTLString(action.period)), "un1", fromObject);
                        }
                    } else {
                        if (isOut()) {
                            messageText = getString(R.string.ActionTTLYouDisabled);
                        } else {
                            messageText = replaceWithLink(getString(R.string.ActionTTLDisabled), "un1", fromObject);
                        }
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionLoginUnknownLocation) {
                    String date;
                    long time = ((long) messageOwner.date) * 1000;
                    if (LocaleController.getInstance().getFormatterDay() != null && LocaleController.getInstance().getFormatterYear() != null) {
                        date = formatString(R.string.formatDateAtTime, LocaleController.getInstance().getFormatterYear().format(time), LocaleController.getInstance().getFormatterDay().format(time));
                    } else {
                        date = "" + messageOwner.date;
                    }
                    TLRPC.User to_user = UserConfig.getInstance(currentAccount).getCurrentUser();
                    if (to_user == null) {
                        to_user = getUser(users, sUsers, messageOwner.peer_id.user_id);
                    }
                    String name = to_user != null ? UserObject.getFirstName(to_user) : "";
                    messageText = formatString(R.string.NotificationUnrecognizedDevice, name, date, messageOwner.action.title, messageOwner.action.address);
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionUserJoined || messageOwner.action instanceof TLRPC.TL_messageActionContactSignUp) {
                    messageText = formatString(R.string.NotificationContactJoined, UserObject.getUserName(fromUser));
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionUserUpdatedPhoto) {
                    messageText = formatString(R.string.NotificationContactNewPhoto, UserObject.getUserName(fromUser));
                } else if (messageOwner.action instanceof TLRPC.TL_messageEncryptedAction) {
                    if (messageOwner.action.encryptedAction instanceof TLRPC.TL_decryptedMessageActionScreenshotMessages) {
                        if (isOut()) {
                            messageText = getString(R.string.ActionTakeScreenshootYou);
                        } else {
                            messageText = replaceWithLink(getString(R.string.ActionTakeScreenshoot), "un1", fromObject);
                        }
                    } else if (messageOwner.action.encryptedAction instanceof TLRPC.TL_decryptedMessageActionSetMessageTTL) {
                        TLRPC.TL_decryptedMessageActionSetMessageTTL action = (TLRPC.TL_decryptedMessageActionSetMessageTTL) messageOwner.action.encryptedAction;
                        if (action.ttl_seconds != 0) {
                            if (isOut()) {
                                messageText = formatString(R.string.MessageLifetimeChangedOutgoing, LocaleController.formatTTLString(action.ttl_seconds));
                            } else {
                                messageText = formatString(R.string.MessageLifetimeChanged, UserObject.getFirstName(fromUser), LocaleController.formatTTLString(action.ttl_seconds));
                            }
                        } else {
                            if (isOut()) {
                                messageText = getString(R.string.MessageLifetimeYouRemoved);
                            } else {
                                messageText = formatString(R.string.MessageLifetimeRemoved, UserObject.getFirstName(fromUser));
                            }
                        }
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionScreenshotTaken) {
                    if (isOut()) {
                        messageText = getString(R.string.ActionTakeScreenshootYou);
                    } else {
                        messageText = replaceWithLink(getString(R.string.ActionTakeScreenshoot), "un1", fromObject);
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionCreatedBroadcastList) {
                    messageText = getString(R.string.YouCreatedBroadcastList);
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionChannelCreate) {
                    TLRPC.Chat chat = messageOwner.peer_id != null && messageOwner.peer_id.channel_id != 0 ? getChat(chats, sChats, messageOwner.peer_id.channel_id) : null;
                    if (ChatObject.isChannel(chat) && chat.megagroup) {
                        messageText = getString(R.string.ActionCreateMega);
                    } else {
                        messageText = getString(R.string.ActionCreateChannel);
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionChatMigrateTo) {
                    messageText = getString(R.string.ActionMigrateFromGroup);
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionChannelMigrateFrom) {
                    messageText = getString(R.string.ActionMigrateFromGroup);
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionPinMessage) {
                    TLRPC.Chat chat;
                    if (fromUser == null) {
                        chat = getChat(chats, sChats, messageOwner.peer_id.channel_id);
                    } else {
                        chat = null;
                    }
                    generatePinMessageText(fromUser, chat);
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionHistoryClear) {
                    messageText = getString(R.string.HistoryCleared);
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionTopicCreate) {
                    messageText = getString(R.string.TopicCreated);

                    TLRPC.TL_messageActionTopicCreate createAction = (TLRPC.TL_messageActionTopicCreate) messageOwner.action;
                    TLRPC.TL_forumTopic forumTopic = new TLRPC.TL_forumTopic();
                    forumTopic.icon_emoji_id = createAction.icon_emoji_id;
                    forumTopic.title = createAction.title;
                    forumTopic.icon_color = createAction.icon_color;

                    messageTextShort = AndroidUtilities.replaceCharSequence("%s", getString(R.string.TopicWasCreatedAction), ForumUtilities.getTopicSpannedName(forumTopic, null, false));
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionTopicEdit) {
                    TLRPC.TL_messageActionTopicEdit editAction = (TLRPC.TL_messageActionTopicEdit) messageOwner.action;

                    String name = null;
                    TLObject object = null;
                    if (fromUser != null) {
                        name = ContactsController.formatName(fromUser.first_name, fromUser.last_name);
                        object = fromUser;
                    } else if (fromChat != null) {
                        name = fromChat.title;
                        object = fromChat;
                    }
                    if (name != null) {
                        name = name.trim();
                    } else {
                        name = "DELETED";
                    }

                    if ((messageOwner.action.flags & 8) > 0) {
                        if (((TLRPC.TL_messageActionTopicEdit) messageOwner.action).hidden) {
                            messageText = replaceWithLink(getString(R.string.TopicHidden2), "%s", object);
                            messageTextShort = getString(R.string.TopicHidden);
                        } else {
                            messageText = replaceWithLink(getString(R.string.TopicShown2), "%s", object);
                            messageTextShort = getString(R.string.TopicShown);
                        }
                    } else if ((messageOwner.action.flags & 4) > 0) {
                        if (((TLRPC.TL_messageActionTopicEdit) messageOwner.action).closed) {
                            messageText = replaceWithLink(getString(R.string.TopicClosed2), "%s", object);
                            messageTextShort = getString(R.string.TopicClosed);
                        } else {
                            messageText = replaceWithLink(getString(R.string.TopicRestarted2), "%s", object);
                            messageTextShort = getString(R.string.TopicRestarted);
                        }
                    } else {
                        if ((messageOwner.action.flags & 2) != 0 && (messageOwner.action.flags & 1) != 0) {
                            TLRPC.TL_forumTopic forumTopic = new TLRPC.TL_forumTopic();
                            forumTopic.icon_emoji_id = editAction.icon_emoji_id;
                            forumTopic.title = editAction.title;
                            forumTopic.icon_color = ForumBubbleDrawable.serverSupportedColor[0];

                            CharSequence topicName = ForumUtilities.getTopicSpannedName(forumTopic, null, topicIconDrawable, false);
                            CharSequence str = AndroidUtilities.replaceCharSequence("%1$s", getString(R.string.TopicChangeIconAndTitleTo), name);
                            messageText = AndroidUtilities.replaceCharSequence("%2$s", str,  topicName);
                            messageTextShort = getString(R.string.TopicRenamed);
                            messageTextForReply = AndroidUtilities.replaceCharSequence("%s", getString(R.string.TopicChangeIconAndTitleToInReply), topicName);
                        } else if ((messageOwner.action.flags & 2) != 0) {
                            TLRPC.TL_forumTopic forumTopic = new TLRPC.TL_forumTopic();
                            forumTopic.icon_emoji_id = editAction.icon_emoji_id;
                            forumTopic.title = "";
                            forumTopic.icon_color = ForumBubbleDrawable.serverSupportedColor[0];
                            CharSequence topicName = ForumUtilities.getTopicSpannedName(forumTopic, null, topicIconDrawable, false);
                            CharSequence str = AndroidUtilities.replaceCharSequence("%1$s", getString(R.string.TopicIconChangedTo), name);
                            messageText = AndroidUtilities.replaceCharSequence("%2$s", str, topicName);
                            messageTextShort = getString(R.string.TopicIconChanged);
                            messageTextForReply = AndroidUtilities.replaceCharSequence("%s", getString(R.string.TopicIconChangedToInReply), topicName);
                        } else if ((messageOwner.action.flags & 1) != 0) {
                            CharSequence str = AndroidUtilities.replaceCharSequence("%1$s", getString(R.string.TopicRenamedTo), name);
                            messageText = AndroidUtilities.replaceCharSequence("%2$s", str, editAction.title);
                            messageTextShort = getString(R.string.TopicRenamed);
                            messageTextForReply = AndroidUtilities.replaceCharSequence("%s", getString(R.string.TopicRenamedToInReply), editAction.title);
                        }
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionGameScore) {
                    generateGameMessageText(fromUser);
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionConferenceCall) {
                    final TLRPC.TL_messageActionConferenceCall call = (TLRPC.TL_messageActionConferenceCall) messageOwner.action;
                    if (isOutOwner()) {
                        messageText = getString(R.string.ConferenceCallOutgoing);
                    } else if (call.missed) {
                        messageText = getString(R.string.ConferenceCallMissed);
                    } else {
                        messageText = getString(R.string.ConferenceCallIncoming);
                    }
                    if (call.duration > 0) {
                        String duration = LocaleController.formatCallDuration(call.duration);
                        messageText = formatString(R.string.CallMessageWithDuration, messageText, duration);
                        String _messageText = messageText.toString();
                        int start = _messageText.indexOf(duration);
                        if (start != -1) {
                            SpannableString sp = new SpannableString(messageText);
                            int end = start + duration.length();
                            if (start > 0 && _messageText.charAt(start - 1) == '(') {
                                start--;
                            }
                            if (end < _messageText.length() && _messageText.charAt(end) == ')') {
                                end++;
                            }
                            sp.setSpan(new TypefaceSpan(Typeface.DEFAULT), start, end, 0);
                            messageText = sp;
                        }
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionPhoneCall) {
                    final TLRPC.TL_messageActionPhoneCall call = (TLRPC.TL_messageActionPhoneCall) messageOwner.action;
                    boolean isMissed = call.reason instanceof TLRPC.TL_phoneCallDiscardReasonMissed;
                    if (isFromUser() && messageOwner.from_id.user_id == UserConfig.getInstance(currentAccount).getClientUserId()) {
                        if (isMissed) {
                            if (call.video) {
                                messageText = getString(R.string.CallMessageVideoOutgoingMissed);
                            } else {
                                messageText = getString(R.string.CallMessageOutgoingMissed);
                            }
                        } else {
                            if (call.video) {
                                messageText = getString(R.string.CallMessageVideoOutgoing);
                            } else {
                                messageText = getString(R.string.CallMessageOutgoing);
                            }
                        }
                    } else {
                        if (isMissed) {
                            if (call.video) {
                                messageText = getString(R.string.CallMessageVideoIncomingMissed);
                            } else {
                                messageText = getString(R.string.CallMessageIncomingMissed);
                            }
                        } else if (call.reason instanceof TLRPC.TL_phoneCallDiscardReasonBusy) {
                            if (call.video) {
                                messageText = getString(R.string.CallMessageVideoIncomingDeclined);
                            } else {
                                messageText = getString(R.string.CallMessageIncomingDeclined);
                            }
                        } else {
                            if (call.video) {
                                messageText = getString(R.string.CallMessageVideoIncoming);
                            } else {
                                messageText = getString(R.string.CallMessageIncoming);
                            }
                        }
                    }
                    if (call.duration > 0) {
                        String duration = LocaleController.formatCallDuration(call.duration);
                        messageText = formatString(R.string.CallMessageWithDuration, messageText, duration);
                        String _messageText = messageText.toString();
                        int start = _messageText.indexOf(duration);
                        if (start != -1) {
                            SpannableString sp = new SpannableString(messageText);
                            int end = start + duration.length();
                            if (start > 0 && _messageText.charAt(start - 1) == '(') {
                                start--;
                            }
                            if (end < _messageText.length() && _messageText.charAt(end) == ')') {
                                end++;
                            }
                            sp.setSpan(new TypefaceSpan(Typeface.DEFAULT), start, end, 0);
                            messageText = sp;
                        }
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionPaymentSent) {
                    final TLRPC.User user = getUser(users, sUsers, getDialogId());
                    generatePaymentSentMessageText(user, false);
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionPaymentSentMe) {
                    final TLRPC.User user = getUser(users, sUsers, getDialogId());
                    generatePaymentSentMessageText(user, true);
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionBotAllowed) {
                    String domain = ((TLRPC.TL_messageActionBotAllowed) messageOwner.action).domain;
                    TLRPC.BotApp botApp = ((TLRPC.TL_messageActionBotAllowed) messageOwner.action).app;
                    if (((TLRPC.TL_messageActionBotAllowed) messageOwner.action).from_request) {
                        messageText = getString(R.string.ActionBotAllowedWebapp);
                    } else if (botApp != null) {
                        String botAppTitle = botApp.title;
                        if (botAppTitle == null) {
                            botAppTitle = "";
                        }
                        String text = getString(R.string.ActionBotAllowedApp);
                        int start = text.indexOf("%1$s");
                        SpannableString str = new SpannableString(String.format(text, botAppTitle));
                        TLRPC.User bot = getUser(users, sUsers, getDialogId());
                        if (start >= 0 && bot != null) {
                            final String username = UserObject.getPublicUsername(bot);
                            if (username != null) {
                                final String link = "https://" + MessagesController.getInstance(currentAccount).linkPrefix + "/" + username + "/" + botApp.short_name;
                                str.setSpan(new URLSpanNoUnderlineBold(link), start, start + botAppTitle.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                        }
                        messageText = str;
                    } else {
                        if (domain == null) {
                            domain = "";
                        }
                        String text = getString(R.string.ActionBotAllowed);
                        int start = text.indexOf("%1$s");
                        SpannableString str = new SpannableString(String.format(text, domain));
                        if (start >= 0 && !TextUtils.isEmpty(domain)) {
                            str.setSpan(new URLSpanNoUnderlineBold("http://" + domain), start, start + domain.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        messageText = str;
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionAttachMenuBotAllowed || messageOwner.action instanceof TLRPC.TL_messageActionBotAllowed && ((TLRPC.TL_messageActionBotAllowed) messageOwner.action).attach_menu) {
                    messageText = getString(R.string.ActionAttachMenuBotAllowed);
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionSecureValuesSent) {
                    TLRPC.TL_messageActionSecureValuesSent valuesSent = (TLRPC.TL_messageActionSecureValuesSent) messageOwner.action;
                    StringBuilder str = new StringBuilder();
                    for (int a = 0, size = valuesSent.types.size(); a < size; a++) {
                        TLRPC.SecureValueType type = valuesSent.types.get(a);
                        if (str.length() > 0) {
                            str.append(", ");
                        }
                        if (type instanceof TLRPC.TL_secureValueTypePhone) {
                            str.append(getString(R.string.ActionBotDocumentPhone));
                        } else if (type instanceof TLRPC.TL_secureValueTypeEmail) {
                            str.append(getString(R.string.ActionBotDocumentEmail));
                        } else if (type instanceof TLRPC.TL_secureValueTypeAddress) {
                            str.append(getString(R.string.ActionBotDocumentAddress));
                        } else if (type instanceof TLRPC.TL_secureValueTypePersonalDetails) {
                            str.append(getString(R.string.ActionBotDocumentIdentity));
                        } else if (type instanceof TLRPC.TL_secureValueTypePassport) {
                            str.append(getString(R.string.ActionBotDocumentPassport));
                        } else if (type instanceof TLRPC.TL_secureValueTypeDriverLicense) {
                            str.append(getString(R.string.ActionBotDocumentDriverLicence));
                        } else if (type instanceof TLRPC.TL_secureValueTypeIdentityCard) {
                            str.append(getString(R.string.ActionBotDocumentIdentityCard));
                        } else if (type instanceof TLRPC.TL_secureValueTypeUtilityBill) {
                            str.append(getString(R.string.ActionBotDocumentUtilityBill));
                        } else if (type instanceof TLRPC.TL_secureValueTypeBankStatement) {
                            str.append(getString(R.string.ActionBotDocumentBankStatement));
                        } else if (type instanceof TLRPC.TL_secureValueTypeRentalAgreement) {
                            str.append(getString(R.string.ActionBotDocumentRentalAgreement));
                        } else if (type instanceof TLRPC.TL_secureValueTypeInternalPassport) {
                            str.append(getString(R.string.ActionBotDocumentInternalPassport));
                        } else if (type instanceof TLRPC.TL_secureValueTypePassportRegistration) {
                            str.append(getString(R.string.ActionBotDocumentPassportRegistration));
                        } else if (type instanceof TLRPC.TL_secureValueTypeTemporaryRegistration) {
                            str.append(getString(R.string.ActionBotDocumentTemporaryRegistration));
                        }
                    }
                    TLRPC.User user = null;
                    if (messageOwner.peer_id != null) {
                        user = getUser(users, sUsers, messageOwner.peer_id.user_id);
                    }
                    messageText = formatString(R.string.ActionBotDocuments, UserObject.getFirstName(user), str.toString());
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionWebViewDataSent) {
                    TLRPC.TL_messageActionWebViewDataSent dataSent = (TLRPC.TL_messageActionWebViewDataSent) messageOwner.action;
                    messageText = formatString(R.string.ActionBotWebViewData, dataSent.text);
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionSetChatTheme) {
                    final TLRPC.ChatTheme actionTheme = ((TLRPC.TL_messageActionSetChatTheme) messageOwner.action).theme;
                    final String title = TlUtils.getThemeEmoticonOrGiftTitle(actionTheme);
                    CharSequence emoticon = title;
                    if (title != null && actionTheme instanceof TLRPC.TL_chatThemeUniqueGift) {
                        final SpannableStringBuilder ssb = new SpannableStringBuilder(title);
                        final TLRPC.Document document = TlUtils.getGiftDocument(((TLRPC.TL_chatThemeUniqueGift) actionTheme).gift);
                        if (document != null) {
                            //ssb.setSpan(new AnimatedEmojiSpan(document, /*Theme.chat_actionTextPaint.getFontMetricsInt()*/ null), 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        emoticon = ssb;
                    }

                    String userName = UserObject.getFirstName(fromUser);
                    boolean isChannel = fromUser == null && fromChat != null;
                    if (isChannel) {
                        userName = fromChat.title;
                    }
                    boolean isUserSelf = UserObject.isUserSelf(fromUser);
                    if (TextUtils.isEmpty(emoticon)) {
                        messageText = isUserSelf
                            ? formatSpannable(R.string.ChatThemeDisabledYou)
                            : formatSpannable(isChannel ? R.string.ChannelThemeDisabled : R.string.ChatThemeDisabled, userName, emoticon);
                    } else {
                        messageText = isUserSelf
                            ? formatSpannable(R.string.ChatThemeChangedYou, emoticon)
                            : formatSpannable(isChannel ? R.string.ChannelThemeChangedTo : R.string.ChatThemeChangedTo, userName, emoticon);
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionChatJoinedByRequest) {
                    if (UserObject.isUserSelf(fromUser)) {
                        boolean isChannel = ChatObject.isChannelAndNotMegaGroup(messageOwner.peer_id.channel_id, currentAccount);
                        messageText = isChannel
                                ? getString(R.string.RequestToJoinChannelApproved)
                                : getString(R.string.RequestToJoinGroupApproved);
                    } else {
                        messageText = replaceWithLink(getString(R.string.UserAcceptedToGroupAction), "un1", fromObject);
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionPaidMessagesPrice) {
                    final TLRPC.TL_messageActionPaidMessagesPrice action = (TLRPC.TL_messageActionPaidMessagesPrice) messageOwner.action;
                    if (ChatObject.isChannel(fromChat) && fromChat != null && fromChat.broadcast) {
                        if ((action.flags & 1) != 0) {
                            if (action.stars > 0) {
                                messageText = replaceWithLink(formatPluralStringComma("PostSuggestionsPriceUpdated", (int) action.stars), "un1", fromObject);
                            } else {
                                messageText = replaceWithLink(LocaleController.getString(R.string.PostSuggestionsEnabledUpdated), "un1", fromObject);
                            }
                        } else {
                            messageText = replaceWithLink(LocaleController.getString(R.string.PostSuggestionsDisabledUpdated), "un1", fromObject);
                        }
                    } else if (isOutOwner()) {
                        messageText = formatPluralStringComma("PaidMessagesPriceUpdatedOut", (int) action.stars);
                    } else {
                        messageText = replaceWithLink(formatPluralStringComma("PaidMessagesPriceUpdated", (int) action.stars), "un1", fromObject);
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionPaidMessagesRefunded) {
                    final TLRPC.TL_messageActionPaidMessagesRefunded action = (TLRPC.TL_messageActionPaidMessagesRefunded) messageOwner.action;
                    if (isOutOwner()) {
                        final TLRPC.User user;
                        if (messageOwner.saved_peer_id != null) {
                            user = getUser(users, sUsers, DialogObject.getPeerDialogId(messageOwner.saved_peer_id));
                        } else {
                            user = getUser(users, sUsers, DialogObject.getPeerDialogId(messageOwner.peer_id));
                        }
                        messageText = replaceWithLink(formatPluralStringComma("PaidMessagesRefundedOut", (int) action.stars), "un1", user);
                    } else {
                        messageText = replaceWithLink(formatPluralStringComma("PaidMessagesRefunded", (int) action.stars), "un1", fromObject);
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionTodoCompletions) {
                    final TLRPC.TL_messageActionTodoCompletions action = (TLRPC.TL_messageActionTodoCompletions) messageOwner.action;
                    boolean completed = !action.completed.isEmpty();
                    int id = !action.completed.isEmpty() ? action.completed.get(0) : !action.incompleted.isEmpty() ? action.incompleted.get(0) : 0;
                    TLRPC.TodoItem task = null;
                    if (replyMessageObject != null && getMedia(replyMessageObject.messageOwner) instanceof TLRPC.TL_messageMediaToDo) {
                        TLRPC.TL_messageMediaToDo m = (TLRPC.TL_messageMediaToDo) getMedia(replyMessageObject.messageOwner);
                        for (int i = 0; i < m.todo.list.size(); ++i) {
                            TLRPC.TodoItem item = m.todo.list.get(i);
                            if (item.id == id) {
                                task = item;
                                break;
                            }
                        }
                    }
                    if (task == null) {
                        if (isOutOwner()) {
                            messageText = getString(completed ? R.string.TodoTaskCompletedOutUnknown : R.string.TodoTaskNotCompletedOutUnknown);
                        } else {
                            messageText = replaceWithLink(getString(completed ? R.string.TodoTaskCompletedUnknown : R.string.TodoTaskNotCompletedUnknown), "un1", fromObject);
                        }
                    } else {
                        Theme.createCommonChatResources();
                        CharSequence taskText = formatTaskTitle(task);
                        if (isOutOwner()) {
                            messageText = formatSpannable(completed ? R.string.TodoTaskCompletedOut : R.string.TodoTaskNotCompletedOut, taskText);
                        } else {
                            messageText = replaceWithLink(formatSpannable(completed ? R.string.TodoTaskCompleted : R.string.TodoTaskNotCompleted, taskText), "un1", fromObject);
                        }
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionTodoAppendTasks) {
                    final TLRPC.TL_messageActionTodoAppendTasks action = (TLRPC.TL_messageActionTodoAppendTasks) messageOwner.action;
                    if (isOutOwner()) {
                        if (action.list.size() == 1) {
                            messageText = formatSpannable(R.string.TodoAddedOneTaskOut, formatTaskTitle(action.list.get(0)));
                        } else if (action.list.size() == 2) {
                            messageText = formatSpannable(R.string.TodoAddedTwoTaskOut, formatTaskTitle(action.list.get(0)), formatTaskTitle(action.list.get(1)));
                        } else if (action.list.size() == 3) {
                            messageText = formatSpannable(R.string.TodoAddedThreeTaskOut, formatTaskTitle(action.list.get(0)), formatTaskTitle(action.list.get(1)), formatTaskTitle(action.list.get(2)));
                        } else if (action.list.size() >= 4) {
                            messageText = formatPluralSpannable("TodoAddedMoreTasksOut", action.list.size() - 3, formatTaskTitle(action.list.get(0)), formatTaskTitle(action.list.get(1)), formatTaskTitle(action.list.get(2)));
                        }
                    } else {
                        if (action.list.size() == 1) {
                            messageText = replaceWithLink(formatSpannable(R.string.TodoAddedOneTask, formatTaskTitle(action.list.get(0))), "un1", fromObject);
                        } else if (action.list.size() == 2) {
                            messageText = replaceWithLink(formatSpannable(R.string.TodoAddedTwoTask, formatTaskTitle(action.list.get(0)), formatTaskTitle(action.list.get(1))), "un1", fromObject);
                        } else if (action.list.size() == 3) {
                            messageText = replaceWithLink(formatSpannable(R.string.TodoAddedThreeTask, formatTaskTitle(action.list.get(0)), formatTaskTitle(action.list.get(1)), formatTaskTitle(action.list.get(2))), "un1", fromObject);
                        } else if (action.list.size() >= 4) {
                            messageText = replaceWithLink(formatPluralSpannable("TodoAddedMoreTasks", action.list.size() - 3, formatTaskTitle(action.list.get(0)), formatTaskTitle(action.list.get(1)), formatTaskTitle(action.list.get(2))), "un1", fromObject);
                        }
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionSuggestedPostApproval) {
                    String channelName = ForumUtilities.getMonoForumTitle(currentAccount, DialogObject.getPeerDialogId(messageOwner.peer_id), true);
                    if (channelName == null) {
                        TLRPC.Chat chat = getChat(chats, sChats, -DialogObject.getPeerDialogId(messageOwner.peer_id));
                        if (chat != null && chat.linked_monoforum_id != 0) {
                            TLRPC.Chat chat2 = getChat(chats, sChats, chat.linked_monoforum_id);
                            if (chat2 != null) {
                                chat = chat2;
                            }
                        }

                        channelName = DialogObject.getDialogTitle(chat);
                    }
                    final String userName = DialogObject.getName(getUser(users, sUsers, DialogObject.getPeerDialogId(messageOwner.saved_peer_id)));
                    messageText = getActionSuggestionApprovalText(channelName, userName);
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionStarGiftPurchaseOffer) {
                    final TLRPC.TL_messageActionStarGiftPurchaseOffer action = (TLRPC.TL_messageActionStarGiftPurchaseOffer) messageOwner.action;
                    final AmountUtils.Amount amount = AmountUtils.Amount.ofSafe(action.price);
                    final String giftTitle = action.gift.title + " #" + LocaleController.formatNumber(action.gift.num, ',');
                    final String userName = DialogObject.getShortName(getUser(users, sUsers, DialogObject.getPeerDialogId(messageOwner.peer_id)));

                    SpannableStringBuilder ssb = new SpannableStringBuilder();
                    ssb.append(replaceTags(formatString(amount.currency == AmountUtils.Currency.STARS ?
                                    isOut() ? R.string.GiftOfferOfferedTextStarsOut : R.string.GiftOfferOfferedTextStars2 :
                                    isOut() ? R.string.GiftOfferOfferedTextTONOut : R.string.GiftOfferOfferedTextTON2,
                            userName, amount.asFormatString(), giftTitle)));
                    messageText = ssb;
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionChangeCommunity) {
                    final TLRPC.TL_messageActionChangeCommunity action = (TLRPC.TL_messageActionChangeCommunity) messageOwner.action;
                    final long peerId = DialogObject.getPeerDialogId(messageOwner.peer_id);
                    final long fromId = DialogObject.getPeerDialogId(messageOwner.from_id);
                    final boolean isChannel = peerId < 0 && ChatObject.isChannelAndNotMegaGroup(getChat(chats, sChats, -peerId));
                    final boolean isBot = peerId > 0;
                    final String communityName = DialogObject.getShortName(getChat(chats, sChats, action.community_id));
                    final String userName = fromId > 0 ?
                        DialogObject.getShortName(getUser(users, sUsers, fromId)) :
                        DialogObject.getShortName(getChat(chats, sChats, -fromId));

                    SpannableStringBuilder ssb = new SpannableStringBuilder();
                    ssb.append(CommunityUtils.buildServiceMessageText(this, communityName, userName, isChannel, isBot));
                    messageText = ssb;
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionStarGiftPurchaseOfferDeclined) {
                    final TLRPC.TL_messageActionStarGiftPurchaseOfferDeclined action = (TLRPC.TL_messageActionStarGiftPurchaseOfferDeclined) messageOwner.action;
                    final AmountUtils.Amount amount = AmountUtils.Amount.ofSafe(action.price);
                    final String giftName = action.gift.title + " #" + LocaleController.formatNumber(action.gift.num, ',');
                    final String userName = DialogObject.getShortName(getUser(users, sUsers, DialogObject.getPeerDialogId(messageOwner.peer_id)));
                    final String amountFmt = amount.asFormatString();

                    final int key;
                    if (action.expired) {
                        if (amount.currency == AmountUtils.Currency.STARS) {
                            key = isOut() ? R.string.GiftOfferOfferedTextStarsExpiredOut : R.string.GiftOfferOfferedTextStarsExpired;
                        } else {
                            key = isOut() ? R.string.GiftOfferOfferedTextTONExpiredOut : R.string.GiftOfferOfferedTextTONExpired;
                        }
                    } else {
                        if (amount.currency == AmountUtils.Currency.STARS) {
                            key = isOut() ? R.string.GiftOfferOfferedTextStarsRejectedOut : R.string.GiftOfferOfferedTextStarsRejected;
                        } else {
                            key = isOut() ? R.string.GiftOfferOfferedTextTONRejectedOut : R.string.GiftOfferOfferedTextTONRejected;
                        }
                    }

                    messageText = replaceTags(formatString(key, userName, amountFmt, giftName));
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionPollAppendAnswer) {
                    final TLRPC.TL_messageActionPollAppendAnswer action = (TLRPC.TL_messageActionPollAppendAnswer) messageOwner.action;
                    if (isOut()) {
                        messageText = formatSpannable(R.string.PollAddingActionYou, formatTextWithEntities(action.answer.text, isOutOwner()));
                    } else {
                        final String userName = DialogObject.getShortName(getUser(users, sUsers, DialogObject.getPeerDialogId(messageOwner.from_id)));
                        messageText = formatSpannable(R.string.PollAddingActionOther, userName, formatTextWithEntities(action.answer.text, isOutOwner()));
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionPollDeleteAnswer) {
                    final TLRPC.TL_messageActionPollDeleteAnswer action = (TLRPC.TL_messageActionPollDeleteAnswer) messageOwner.action;
                    if (isOut()) {
                        messageText = formatSpannable(R.string.PollRemovedActionYou, formatTextWithEntities(action.answer.text, isOutOwner()));
                    } else {
                        final String userName = DialogObject.getShortName(getUser(users, sUsers, DialogObject.getPeerDialogId(messageOwner.from_id)));
                        messageText = formatSpannable(R.string.PollRemovedActionOther, userName, formatTextWithEntities(action.answer.text, isOutOwner()));
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionNoForwardsToggle) {
                    TLRPC.TL_messageActionNoForwardsToggle action = (TLRPC.TL_messageActionNoForwardsToggle) messageOwner.action;
                    if (action.new_value == action.prev_value) {
                        messageText = getString(action.new_value ? R.string.DisableSharingActionStillDisabled : R.string.DisableSharingActionStillEnabled);
                    } else {
                        if (isOut()) {
                            messageText = getString(action.new_value ? R.string.DisableSharingActionYou : R.string.EnableSharingActionYou);
                        } else {
                            final String userName = DialogObject.getShortName(getUser(users, sUsers, DialogObject.getPeerDialogId(messageOwner.peer_id)));
                            messageText = formatString(action.new_value ? R.string.DisableSharingActionOther : R.string.EnableSharingActionOther, userName);
                        }
                    }
                } else if (messageOwner.action instanceof TLRPC.TL_messageActionNoForwardsRequest) {
                    final TLRPC.TL_messageActionNoForwardsRequest action = (TLRPC.TL_messageActionNoForwardsRequest) messageOwner.action;
                    final String userName = DialogObject.getShortName(getUser(users, sUsers, DialogObject.getPeerDialogId(messageOwner.peer_id)));
                    if (action.new_value) {
                        messageText = isOut() ? getString(R.string.SharingOfferDisableHeaderShortYou) : replaceTags(formatString(R.string.SharingOfferDisableHeaderShortOther, userName));
                    } else {
                        messageText = isOut() ? getString(R.string.SharingOfferEnableHeaderShortYou) : replaceTags(formatString(R.string.SharingOfferEnableHeaderShortOther, userName));
                    }
                }
            }
        } else {
            isRestrictedMessage = false;
            String restrictionReason = MessagesController.getInstance(currentAccount).getRestrictionReason(messageOwner.restriction_reason);
            if (!TextUtils.isEmpty(restrictionReason)) {
                messageText = restrictionReason;
                isRestrictedMessage = true;
            } else if (messageOwner.rich_message != null) {
                messageText = formatRichMessage(messageOwner.rich_message, isOutOwner());
                messageText = AndroidUtilities.replaceNewLines(messageText);
            } else if (!isMediaEmpty() && !isSponsored()) {
//                messageText = getMediaTitle(getMedia(messageOwner)); // I'm afraid doing this
                if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaGiveaway) {
                    boolean isChannel;
                    if (messageOwner.fwd_from != null && messageOwner.fwd_from.from_id instanceof TLRPC.TL_peerChannel) {
                        TLRPC.Chat chat = getChat(chats, sChats, messageOwner.fwd_from.from_id.channel_id);
                        isChannel = ChatObject.isChannelAndNotMegaGroup(chat);
                    } else {
                        isChannel = ChatObject.isChannelAndNotMegaGroup(fromChat);
                    }
                    messageText = getString(isChannel ? R.string.BoostingGiveawayChannelStarted : R.string.BoostingGiveawayGroupStarted);
                } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaGiveawayResults) {
                    messageText = getString(R.string.BoostingGiveawayResults);
                } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaStory) {
                    if (getMedia(messageOwner).via_mention) {
                        TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(getMedia(messageOwner).user_id);
                        String link = null, username;
                        if (user != null && (username = UserObject.getPublicUsername(user)) != null) {
                            link = MessagesController.getInstance(currentAccount).linkPrefix + "/" + username + "/s/" + getMedia(messageOwner).id;
                        }
                        if (link != null) {
                            messageText = new SpannableString(link);
                            ((SpannableString) messageText).setSpan(new URLSpanReplacement("https://" + link, new TextStyleSpan.TextStyleRun()), 0, messageText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        } else {
                            messageText = "";
                        }
                    } else {
                        messageText = getString(R.string.ForwardedStory);
                    }
                } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaDice) {
                    messageText = getDiceEmoji();
                } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaPoll) {
                    if (((TLRPC.TL_messageMediaPoll) getMedia(messageOwner)).poll.quiz) {
                        messageText = getString(R.string.QuizPoll);
                    } else {
                        messageText = getString(R.string.Poll);
                    }
                } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaToDo) {
                    final TLRPC.TL_messageMediaToDo media = (TLRPC.TL_messageMediaToDo) getMedia(messageOwner);
                    if (media.todo != null && media.todo.title != null && !media.todo.title.text.isEmpty()) {
                        messageText = formatSpannable(R.string.MessageTodoName, formatTextWithEntities(media.todo.title, isOutOwner()));
                    } else {
                        messageText = getString(R.string.MessageTodo);
                    }
                } else if (isVoiceOnce()) {
                    messageText = getString(R.string.AttachOnceAudio);
                } else if (isRoundOnce()) {
                    messageText = getString(R.string.AttachOnceRound);
                } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaPaidMedia) {
                    TLRPC.TL_messageMediaPaidMedia paidMedia = (TLRPC.TL_messageMediaPaidMedia) getMedia(messageOwner);
                    final int count = paidMedia.extended_media.size();
                    boolean video = false;
                    for (int i = 0; i < count; ++i) {
                        TLRPC.MessageExtendedMedia emedia = paidMedia.extended_media.get(i);
                        if (emedia instanceof TLRPC.TL_messageExtendedMedia) {
                            video = ((TLRPC.TL_messageExtendedMedia) emedia).media instanceof TLRPC.TL_messageMediaDocument &&
                                isVideoDocument(((TLRPC.TL_messageExtendedMedia) emedia).media.document);
                        } else if (emedia instanceof TLRPC.TL_messageExtendedMediaPreview) {
                            video = (((TLRPC.TL_messageExtendedMediaPreview) emedia).flags & 4) != 0;
                        }
                        if (video) break;
                    }
                    messageText = count == 1 ? getString(video ? R.string.AttachVideo : R.string.AttachPhoto) : LocaleController.formatPluralString(video ? "Media" : "Photos", count);
                    messageText = StarsIntroActivity.replaceStars(formatString(R.string.AttachPaidMedia, messageText));
                } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaPhoto) {
                    if (getMedia(messageOwner).ttl_seconds != 0 && !(messageOwner instanceof TLRPC.TL_message_secret)) {
                        messageText = getString(R.string.AttachDestructingPhoto);
                    } else if (getGroupId() != 0) {
                        messageText = getString(R.string.Album);
                    } else if (isLivePhoto()) {
                        messageText = getString(R.string.AttachLivePhoto);
                    } else {
                        messageText = getString(R.string.AttachPhoto);
                    }
                } else if (isVideo() || getMedia(messageOwner) instanceof TLRPC.TL_messageMediaDocument && (getDocument() instanceof TLRPC.TL_documentEmpty || getDocument() == null) && getMedia(messageOwner).ttl_seconds != 0) {
                    if (getMedia(messageOwner).ttl_seconds != 0 && !(messageOwner instanceof TLRPC.TL_message_secret)) {
                        if (getMedia(messageOwner).voice) {
                            messageText = getString(R.string.AttachVoiceExpired);
                        } else if (getMedia(messageOwner).round) {
                            messageText = getString(R.string.AttachRoundExpired);
                        } else {
                            messageText = getString(R.string.AttachDestructingVideo);
                        }
                    } else {
                        messageText = getString(R.string.AttachVideo);
                    }
                } else if (isVoice()) {
                    messageText = getString(R.string.AttachAudio);
                } else if (isRoundVideo()) {
                    messageText = getString(R.string.AttachRound);
                } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaGeo || getMedia(messageOwner) instanceof TLRPC.TL_messageMediaVenue) {
                    messageText = getString(R.string.AttachLocation);
                } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaGeoLive) {
                    messageText = getString(R.string.AttachLiveLocation);
                } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaContact) {
                    messageText = getString(R.string.AttachContact);
                    if (!TextUtils.isEmpty(getMedia(messageOwner).vcard)) {
                        vCardData = VCardData.parse(getMedia(messageOwner).vcard);
                    }
                } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaGame) {
                    messageText = messageOwner.message;
                } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaInvoice) {
                    messageText = getMedia(messageOwner).description;
                } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaUnsupported) {
                    messageText = getString(R.string.UnsupportedMedia2);
                } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaDocument) {
                    if (isSticker() || isAnimatedStickerDocument(getDocument(), true)) {
                        String sch = getStickerChar();
                        if (sch != null && sch.length() > 0) {
                            messageText = String.format("%s %s", sch, getString(R.string.AttachSticker));
                        } else {
                            messageText = getString(R.string.AttachSticker);
                        }
                    } else if (isMusic()) {
                        messageText = getString(R.string.AttachMusic);
                    } else if (isGif()) {
                        messageText = getString(R.string.AttachGif);
                    } else {
                        String name = FileLoader.getDocumentFileName(getDocument());
                        if (!TextUtils.isEmpty(name)) {
                            messageText = name;
                        } else {
                            messageText = getString(R.string.AttachDocument);
                        }
                    }
                }
            } else {
                if (messageOwner.message != null) {
                    try {
                        if (messageOwner.message.length() > 200) {
                            messageText = AndroidUtilities.BAD_CHARS_MESSAGE_LONG_PATTERN.matcher(messageOwner.message).replaceAll("\u200C");
                        } else {
                            messageText = AndroidUtilities.BAD_CHARS_MESSAGE_PATTERN.matcher(messageOwner.message).replaceAll("\u200C");
                        }
                    } catch (Throwable e) {
                        messageText = messageOwner.message;
                    }
                } else {
                    messageText = messageOwner.message;
                }
            }
        }

        if (messageText == null) {
            messageText = "";
        }

        isEmbedVideoCached = null;
        cachedStartsTimestamp = null;
        cachedSavedTimestamp = null;
    }

    private CharSequence formatTaskTitle(TLRPC.TodoItem task) {
        CharSequence text = formatTextWithEntities(task.title, isOutOwner());
        if (!(text instanceof Spannable)) {
            text = new SpannableStringBuilder(text);
        }
        ((Spannable) text).setSpan(new URLSpanNoUnderline("task?" + task.id, true), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return text;
    }

    public static CharSequence formatTextWithEntities(TLRPC.TL_textWithEntities text) {
        return formatTextWithEntities(text, false);
    }

    public static CharSequence formatTextWithEntities(TLRPC.TL_textWithEntities text, boolean out) {
        Theme.createCommonChatResources();
        TextPaint paint = Theme.chat_actionTextPaint;
        if (paint == null) {
            paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            paint.setTypeface(AndroidUtilities.bold());
            paint.setTextSize(dp(Math.max(16, SharedConfig.fontSize) - 2));
        }
        return formatTextWithEntities(text, out, paint);
    }

    public static CharSequence formatTextWithEntities(TLRPC.TL_textWithEntities text, boolean out, TextPaint paint) {
        return formatTextWithEntities(text, out, false, paint);
    }

    public static CharSequence formatTextWithEntities(TLRPC.TL_textWithEntities text, boolean out, boolean photoViewer, TextPaint paint) {
        CharSequence taskText = new SpannableStringBuilder(text.text);
        addEntitiesToText(taskText, text.entities, out, false, photoViewer, false);
        taskText = Emoji.replaceEmoji(taskText, paint.getFontMetricsInt(), false);
        taskText = replaceAnimatedEmoji(taskText, text.entities, paint.getFontMetricsInt());
        return taskText;
    }

    public static CharSequence formatRichMessage(TL_iv.RichMessage richMessage, boolean isOut) {
        return formatRichMessage(richMessage, isOut, false, 1024);
    }
    public static CharSequence formatRichMessage(TL_iv.RichMessage richMessage, boolean isOut, boolean photoViewer, int maxLength) {
        final SpannableStringBuilder out = new SpannableStringBuilder();
        if (richMessage != null) {
            for (int i = 0 ; i < richMessage.blocks.size(); ++i) {
                final TL_iv.PageBlock block = richMessage.blocks.get(i);
                if (i > 0) out.append("  ");
                formatRichBlock(block, isOut, photoViewer, maxLength, out, richMessage);
                if (out.length() >= maxLength) {
                    out.delete(maxLength, out.length());
                    out.append("‚Ä¶");
                    break;
                }
            }
        }
        return out;
    }
    public static boolean isBlueBlock(TL_iv.PageBlock block) {
        return block instanceof TL_iv.pageBlockTable || block instanceof TL_iv.pageBlockMath || block instanceof TL_iv.pageBlockAudio || block instanceof TL_iv.pageBlockDocument || block instanceof TL_iv.pageBlockMap || block instanceof TL_iv.pageBlockPhoto || block instanceof TL_iv.pageBlockVideo || block instanceof TL_iv.pageBlockCollage || block instanceof TL_iv.pageBlockSlideshow;
    }
    public static SpannableStringBuilder span(String str, int icon) {
        final SpannableStringBuilder sb = new SpannableStringBuilder(str);
        final ColoredImageSpan span = new ColoredImageSpan(icon);
        sb.setSpan(span, 0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }
    public static SpannableStringBuilder checkboxSpan(boolean enabled) {
        return span(enabled ? "‚úÖ" : "‚òëÔ∏è", enabled ? R.drawable.iv_checkbox_on : R.drawable.iv_checkbox_off);
    }
    public static CharSequence formatRichBlock(TL_iv.PageBlock block, boolean isOut, boolean photoViewer, int maxLength, SpannableStringBuilder out, TL_iv.RichMessage richMessage) {
        if (
            block instanceof TL_iv.pageBlockTitle ||
            block instanceof TL_iv.pageBlockHeader ||
            block instanceof TL_iv.pageBlockSubheader ||
            block instanceof TL_iv.pageBlockHeading1 ||
            block instanceof TL_iv.pageBlockHeading2 ||
            block instanceof TL_iv.pageBlockHeading3 ||
            block instanceof TL_iv.pageBlockHeading4 ||
            block instanceof TL_iv.pageBlockHeading5 ||
            block instanceof TL_iv.pageBlockHeading6 ||
            block instanceof TL_iv.pageBlockBlockquote ||
            block instanceof TL_iv.pageBlockPullquote
        ) {
            formatRichText(block.text, isOut, photoViewer, maxLength, out, TextStyleSpan.FLAG_STYLE_BOLD);
        } else if (
            block instanceof TL_iv.pageBlockParagraph ||
            block instanceof TL_iv.pageBlockFooter ||
            block instanceof TL_iv.pageBlockKicker
        ) {
            formatRichText(block.text, isOut, photoViewer, maxLength, out, 0);
        } else if (block instanceof TL_iv.pageBlockBlockquoteBlocks) {
            final TL_iv.pageBlockBlockquoteBlocks quote = (TL_iv.pageBlockBlockquoteBlocks) block;
            for (int i = 0; i < quote.blocks.size(); ++i) {
                if (i > 0) out.append("  ");
                formatRichBlock(quote.blocks.get(i), isOut, photoViewer, maxLength, out, richMessage);
                if (out.length() >= maxLength) {
                    out.delete(maxLength, out.length());
                    out.append("‚Ä¶");
                    return out;
                }
            }
        } else if (block instanceof TL_iv.pageBlockDetails) {
            formatRichText(((TL_iv.pageBlockDetails) block).title, isOut, photoViewer, maxLength, out, 0);
        } else if (block instanceof TL_iv.pageBlockAuthorDate) {
            formatRichText(((TL_iv.pageBlockAuthorDate) block).author, isOut, photoViewer, maxLength, out, 0);
        } else if (block instanceof TL_iv.pageBlockMath) {
            out.append(span("fx", R.drawable.iv_formula)).append(" ").append(getString(R.string.AccDescrIVFormula));
        } else if (block instanceof TL_iv.pageBlockMap) {
            out.append(getString(R.string.Map));
            if (block.caption != null && !(block.caption.text instanceof TL_iv.textEmpty)) {
                out.append("  ");
                formatRichText(block.caption.text, isOut, photoViewer, maxLength, out, 0);
            }
        } else if (block instanceof TL_iv.pageBlockPreformatted) {
            formatRichText(block.text, isOut, photoViewer, maxLength, out, TextStyleSpan.FLAG_STYLE_MONO);
        } else if (block instanceof TL_iv.pageBlockList) {
            final TL_iv.pageBlockList list = (TL_iv.pageBlockList) block;
            for (int j = 0; j < list.items.size(); ++j) {
                if (j > 0) out.append("  ");
                final TL_iv.PageListItem item = list.items.get(j);
                if (item instanceof TL_iv.TL_pageListItemText) {
                    final TL_iv.TL_pageListItemText i = (TL_iv.TL_pageListItemText) item;
                    if (i.checkbox) {
                        out.append(checkboxSpan(i.checked)).append(" ");
                    } else {
                        out.append("‚Ä¢ ");
                    }
                    formatRichText(i.text, isOut, photoViewer, maxLength, out, 0);
                } else if (item instanceof TL_iv.TL_pageListItemBlocks) {
                    final TL_iv.TL_pageListItemBlocks i = (TL_iv.TL_pageListItemBlocks) item;
                    if (i.checkbox) {
                        out.append(checkboxSpan(i.checked)).append(" ");
                    } else {
                        out.append("‚Ä¢ ");
                    }
                    for (int k = 0; k < i.blocks.size(); ++k) {
                        if (k > 0) out.append("  ");
                        final TL_iv.PageBlock block0 = i.blocks.get(k);
                        formatRichBlock(block0, isOut, photoViewer, maxLength, out, richMessage);
                        if (out.length() >= maxLength) {
                            out.delete(maxLength, out.length());
                            out.append("‚Ä¶");
                            return out;
                        }
                    }
                }
                if (out.length() >= maxLength) {
                    out.delete(maxLength, out.length());
                    out.append("‚Ä¶");
                    return out;
                }
            }
        } else if (block instanceof TL_iv.pageBlockOrderedList) {
            final TL_iv.pageBlockOrderedList list = (TL_iv.pageBlockOrderedList) block;
            for (int j = 0; j < list.items.size(); ++j) {
                if (j > 0) out.append("  ");
                final TL_iv.PageListOrderedItem item = list.items.get(j);
                if (item instanceof TL_iv.TL_pageListOrderedItemText) {
                    final TL_iv.TL_pageListOrderedItemText i = (TL_iv.TL_pageListOrderedItemText) item;
                    out.append(i.num);
                    out.append(". ");
                    if (i.checkbox) {
                        out.append(checkboxSpan(i.checked)).append(" ");
                    }
                    formatRichText(i.text, isOut, photoViewer, maxLength, out, 0);
                } else if (item instanceof TL_iv.TL_pageListOrderedItemBlocks) {
                    final TL_iv.TL_pageListOrderedItemBlocks i = (TL_iv.TL_pageListOrderedItemBlocks) item;
                    out.append(i.num);
                    out.append(". ");
                    if (i.checkbox) {
                        out.append(checkboxSpan(i.checked)).append(" ");
                    }
                    for (int k = 0; k < i.blocks.size(); ++k) {
                        if (k > 0) out.append("  ");
                        final TL_iv.PageBlock block0 = i.blocks.get(k);
                        formatRichBlock(block0, isOut, photoViewer, maxLength, out, richMessage);
                        if (out.length() >= maxLength) {
                            out.delete(maxLength, out.length());
                            out.append("‚Ä¶");
                            return out;
                        }
                    }
                }
                if (out.length() >= maxLength) {
                    out.delete(maxLength, out.length());
                    out.append("‚Ä¶");
                    return out;
                }
            }
        } else if (block instanceof TL_iv.pageBlockTable) {
            final TL_iv.pageBlockTable table = (TL_iv.pageBlockTable) block;
            out.append(span("‚äû", R.drawable.iv_preview_table)).append(" ");
            if (table.title != null && !(table.title instanceof TL_iv.textEmpty)) {
                formatRichText(table.title, isOut, photoViewer, maxLength, out, 0);
            } else {
                out.append(getString(R.string.AccDescrIVTable));
            }
        } else if (block instanceof TL_iv.pageBlockAudio) {
            final TL_iv.pageBlockAudio audio = (TL_iv.pageBlockAudio) block;
            TLRPC.Document document = null;
            for (int i = 0; i < richMessage.documents.size(); ++i) {
                if (richMessage.documents.get(i).id == audio.audio_id) {
                    document = richMessage.documents.get(i);
                    break;
                }
            }
            if (document != null) {
                final TLRPC.TL_documentAttributeAudio audioAttr = find(document.attributes, TLRPC.TL_documentAttributeAudio.class);
                final TLRPC.TL_documentAttributeFilename fileAttr = find(document.attributes, TLRPC.TL_documentAttributeFilename.class);
                if (audioAttr != null) {
                    if (!TextUtils.isEmpty(audioAttr.title) && !TextUtils.isEmpty(audioAttr.performer)) {
                        out.append(span("üéµ", R.drawable.iv_audio_preview)).append(" ").append(audioAttr.performer).append(" ‚Äì ").append(audioAttr.title);
                    } else if (!TextUtils.isEmpty(audioAttr.title)) {
                        out.append(span("üéµ", R.drawable.iv_audio_preview)).append(" ").append(audioAttr.title);
                    } else if (fileAttr != null && fileAttr.file_name != null) {
                        out.append(span("üéµ", R.drawable.iv_audio_preview)).append(" ").append(fileAttr.file_name);
                    }
                } else if (fileAttr != null && fileAttr.file_name != null) {
                    out.append(span("üéµ", R.drawable.iv_audio_preview)).append(" ").append(fileAttr.file_name);
                }
            }
        } else if (block instanceof TL_iv.pageBlockDocument) {
            final TLRPC.Document document = AndroidUtilities.findDocument(richMessage.documents, ((TL_iv.pageBlockDocument) block).document_id);
            if (document != null) {
                final String name = FileLoader.getDocumentFileName(document);
                out.append(span("üìé", R.drawable.msg_filled_data_files)).append(" ").append(TextUtils.isEmpty(name) ? getString(R.string.AttachDocument) : name);
            }
        } else if (block instanceof TL_iv.pageBlockCover) {
            formatRichBlock(((TL_iv.pageBlockCover) block).cover, isOut, photoViewer, maxLength, out, richMessage);
        } else if (block instanceof TL_iv.pageBlockPhoto) {
            out.append(getString(R.string.AttachPhoto));
        } else if (block instanceof TL_iv.pageBlockVideo) {
            out.append(getString(R.string.AttachVideo));
        } else if (block instanceof TL_iv.pageBlockCollage) {
            out.append(getString(R.string.AccDescrCollage));
        } else if (block instanceof TL_iv.pageBlockSlideshow) {
            out.append(getString(R.string.AccDescrIVSlideshow));
        } else if (block instanceof TL_iv.pageBlockUnsupported) {
            out.append(getString(R.string.UnsupportedAttachment));
        } else if (block instanceof TL_iv.pageBlockButtonRow) {
            final TL_iv.pageBlockButtonRow buttonRow = (TL_iv.pageBlockButtonRow) block;
            for (int i = 0; i < buttonRow.buttons.size(); ++i) {
                if (i > 0) out.append("  ");
                formatRichText(buttonRow.buttons.get(i).text, isOut, photoViewer, maxLength, out, 0);
            }
        }
        return out;
    }
    public static CharSequence formatRichText(TL_iv.RichText text, boolean isOut, boolean photoViewer, int maxLength, SpannableStringBuilder out, int flags) {
        if (text == null) return out;
        int start = out.length();
        if (text instanceof TL_iv.textPlain) {
            out.append(((TL_iv.textPlain) text).text);
        } else if (text instanceof TL_iv.textButton) {
            formatRichText(text.text, isOut, photoViewer, maxLength, out, flags);
        } else if (text instanceof TL_iv.textBold) {
            formatRichText(text.text, isOut, photoViewer, maxLength, out, flags | TextStyleSpan.FLAG_STYLE_BOLD);
        } else if (text instanceof TL_iv.textItalic) {
            formatRichText(text.text, isOut, photoViewer, maxLength, out, flags | TextStyleSpan.FLAG_STYLE_ITALIC);
        } else if (text instanceof TL_iv.textUnderline) {
            formatRichText(text.text, isOut, photoViewer, maxLength, out, flags | TextStyleSpan.FLAG_STYLE_UNDERLINE);
        } else if (text instanceof TL_iv.textStrike) {
            formatRichText(text.text, isOut, photoViewer, maxLength, out, flags | TextStyleSpan.FLAG_STYLE_STRIKE);
        } else if (text instanceof TL_iv.textFixed) {
            formatRichText(text.text, isOut, photoViewer, maxLength, out, flags | TextStyleSpan.FLAG_STYLE_MONO);
        } else if (text instanceof TL_iv.textSpoiler) {
            formatRichText(text.text, isOut, photoViewer, maxLength, out, flags | TextStyleSpan.FLAG_STYLE_SPOILER);
        } else if (text instanceof TL_iv.textUrl) {
            formatRichText(text.text, isOut, photoViewer, maxLength, out, flags);
            if (out.length() > start) {
                final TextStyleSpan.TextStyleRun run = new TextStyleSpan.TextStyleRun();
                run.flags = flags;
                out.setSpan(new URLSpanBrowser(text.url, run), start, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } else if (text instanceof TL_iv.textEmail) {
            formatRichText(text.text, isOut, photoViewer, maxLength, out, flags);
            if (out.length() > start) {
                final TextStyleSpan.TextStyleRun run = new TextStyleSpan.TextStyleRun();
                run.flags = flags;
                out.setSpan(new URLSpanReplacement("mailto:" + text.email, run), start, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } else if (text instanceof TL_iv.textMath) {
            out.append(span("fx", R.drawable.iv_formula)).append(" ").append(getString(R.string.AccDescrIVFormula));
        } else if (text instanceof TL_iv.textPhone) {
            formatRichText(text.text, isOut, photoViewer, maxLength, out, flags);
            if (out.length() > start) {
                String tel = PhoneFormat.stripExceptNumbers(((TL_iv.textPhone) text).phone);
                if (((TL_iv.textPhone) text).phone.startsWith("+")) {
                    tel = "+" + tel;
                }
                final TextStyleSpan.TextStyleRun run = new TextStyleSpan.TextStyleRun();
                run.flags = flags;
                out.setSpan(new URLSpanNoUnderline("tel:" + tel, run), start, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } else if (text instanceof TL_iv.textConcat) {
            final TL_iv.textConcat concat = (TL_iv.textConcat) text;
            for (TL_iv.RichText textChild : concat.texts) {
                formatRichText(textChild, isOut, photoViewer, maxLength, out, flags);
                if (out.length() >= maxLength) {
                    out.delete(maxLength, out.length());
                    out.append("‚Ä¶");
                    return out;
                }
            }
        }
        if (out.length() > start && flags != 0) {
            final TextStyleSpan.TextStyleRun run = new TextStyleSpan.TextStyleRun();
            run.flags = flags;
            out.setSpan(new TextStyleSpan(run), start, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return out;
    }

    public static TLRPC.TL_textWithEntities removeLinks(TLRPC.TL_textWithEntities text) {
        final TLRPC.TL_textWithEntities newText = new TLRPC.TL_textWithEntities();
        newText.text = text.text;
        for (int i = 0; i < text.entities.size(); ++i) {
            final TLRPC.MessageEntity e = text.entities.get(i);
            if (!(e instanceof TLRPC.TL_messageEntityUrl || e instanceof TLRPC.TL_messageEntityTextUrl)) {
                newText.entities.add(e);
            }
        }
        return newText;
    }

    public CharSequence getMediaTitle(TLRPC.MessageMedia media) {
        if (media instanceof TLRPC.TL_messageMediaGiveaway) {
            return getString(R.string.BoostingGiveaway);
        } else if (media instanceof TLRPC.TL_messageMediaGiveawayResults) {
            return getString(R.string.BoostingGiveawayResults);
        } else if (media instanceof TLRPC.TL_messageMediaStory) {
            if (media.via_mention) {
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(media.user_id);
                String link = null, username;
                if (user != null && (username = UserObject.getPublicUsername(user)) != null) {
                    link = MessagesController.getInstance(currentAccount).linkPrefix + "/" + username + "/s/" + media.id;
                }
                if (link != null) {
                    SpannableString str = new SpannableString(link);
                    ((SpannableString) str).setSpan(new URLSpanReplacement("https://" + link, new TextStyleSpan.TextStyleRun()), 0, str.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    return str;
                } else {
                    return "";
                }
            } else {
                return getString(R.string.ForwardedStory);
            }
        } else if (media instanceof TLRPC.TL_messageMediaDice) {
            return getDiceEmoji((TLRPC.TL_messageMediaDice) media);
        } else if (media instanceof TLRPC.TL_messageMediaPoll) {
            if (((TLRPC.TL_messageMediaPoll) media).poll.quiz) {
                return getString(R.string.QuizPoll);
            } else {
                return getString(R.string.Poll);
            }
        } else if (media instanceof TLRPC.TL_messageMediaToDo) {
            final TLRPC.TL_messageMediaToDo mediaTodo = (TLRPC.TL_messageMediaToDo) media;
            if (mediaTodo.todo != null && mediaTodo.todo.title != null && !mediaTodo.todo.title.text.isEmpty()) {
                return formatSpannable(R.string.MessageTodoName, formatTextWithEntities(mediaTodo.todo.title, isOutOwner()));
            } else {
                return getString(R.string.MessageTodo);
            }
        } else if (media instanceof TLRPC.TL_messageMediaPaidMedia) {
            TLRPC.TL_messageMediaPaidMedia paidMedia = (TLRPC.TL_messageMediaPaidMedia) media;
            final int count = paidMedia.extended_media.size();
            boolean video = false;
            for (int i = 0; i < count; ++i) {
                TLRPC.MessageExtendedMedia emedia = paidMedia.extended_media.get(i);
                if (emedia instanceof TLRPC.TL_messageExtendedMedia) {
                    video = ((TLRPC.TL_messageExtendedMedia) emedia).media instanceof TLRPC.TL_messageMediaDocument &&
                            isVideoDocument(((TLRPC.TL_messageExtendedMedia) emedia).media.document);
                } else if (emedia instanceof TLRPC.TL_messageExtendedMediaPreview) {
                    video = (((TLRPC.TL_messageExtendedMediaPreview) emedia).flags & 4) != 0;
                }
                if (video) break;
            }
            CharSequence s = count == 1 ? getString(video ? R.string.AttachVideo : R.string.AttachPhoto) : LocaleController.formatPluralString(video ? "Media" : "Photos", count);
            return StarsIntroActivity.replaceStars(formatString(R.string.AttachPaidMedia, s));
        } else if (media instanceof TLRPC.TL_messageMediaPhoto) {
            if (media.ttl_seconds != 0 && !(messageOwner instanceof TLRPC.TL_message_secret)) {
                return getString(R.string.AttachDestructingPhoto);
            } else if (getGroupId() != 0) {
                return getString(R.string.Album);
            } else if (isLivePhoto()) {
                return getString(R.string.AttachLivePhoto);
            } else {
                return getString(R.string.AttachPhoto);
            }
        } else if (media != null && (isVideoDocument(media.document) || media instanceof TLRPC.TL_messageMediaDocument && (media.document instanceof TLRPC.TL_documentEmpty || media.document == null) && media.ttl_seconds != 0)) {
            if (media.ttl_seconds != 0 && !(messageOwner instanceof TLRPC.TL_message_secret)) {
                if (media.voice) {
                    return getString(R.string.AttachVoiceExpired);
                } else if (media.round) {
                    return getString(R.string.AttachRoundExpired);
                } else {
                    return getString(R.string.AttachDestructingVideo);
                }
            } else {
                return getString(R.string.AttachVideo);
            }
        } else if (media != null && isVoiceDocument(media.document)) {
            return getString(R.string.AttachAudio);
        } else if (media != null && isRoundVideoDocument(media.document)) {
            return getString(R.string.AttachRound);
        } else if (media instanceof TLRPC.TL_messageMediaGeo || media instanceof TLRPC.TL_messageMediaVenue) {
            return getString(R.string.AttachLocation);
        } else if (media instanceof TLRPC.TL_messageMediaGeoLive) {
            return getString(R.string.AttachLiveLocation);
        } else if (media instanceof TLRPC.TL_messageMediaContact) {
//            if (!TextUtils.isEmpty(media.vcard)) {
//                vCardData = VCardData.parse(media.vcard);
//            }
            return getString(R.string.AttachContact);
        } else if (media instanceof TLRPC.TL_messageMediaGame) {
            return messageOwner.message;
        } else if (media instanceof TLRPC.TL_messageMediaInvoice) {
            return media.description;
        } else if (media instanceof TLRPC.TL_messageMediaUnsupported) {
            return getString(R.string.UnsupportedMedia2);
        } else if (media instanceof TLRPC.TL_messageMediaDocument) {
            if (isStickerDocument(media.document) || isAnimatedStickerDocument(media.document, true)) {
                String sch = getStickerChar();
                if (sch != null && sch.length() > 0) {
                    return String.format("%s %s", sch, getString(R.string.AttachSticker));
                } else {
                    return getString(R.string.AttachSticker);
                }
            } else if (isMusic()) {
                return getString(R.string.AttachMusic);
            } else if (isGif()) {
                return getString(R.string.AttachGif);
            } else {
                String name = FileLoader.getDocumentFileName(media.document);
                if (!TextUtils.isEmpty(name)) {
                    return name;
                } else {
                    return getString(R.string.AttachDocument);
                }
            }
        }
        return null;
    }

    public static TLRPC.MessageMedia getMedia(MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return null;
        }
        if (messageObject.sponsoredMedia != null) {
            return messageObject.sponsoredMedia;
        }
        return getMedia(messageObject.messageOwner);
    }

    public static TLRPC.MessageMedia getMedia(TLRPC.Message messageOwner) {
        if (
            messageOwner.media != null &&
            !(messageOwner.media instanceof TLRPC.TL_messageMediaPaidMedia) &&
            !messageOwner.media.extended_media.isEmpty() &&
            messageOwner.media.extended_media.get(0) instanceof TLRPC.TL_messageExtendedMedia
        ) {
            return ((TLRPC.TL_messageExtendedMedia) messageOwner.media.extended_media.get(0)).media;
        }
        return messageOwner.media;
    }

    public static <T extends TLRPC.MessageMedia> T getMedia(TLRPC.Message messageOwner, Class<T> tClass) {
        TLRPC.MessageMedia media = getMedia(messageOwner);
        if (tClass.isInstance(media)) {
            return tClass.cast(media);
        }
        return null;
    }

    public boolean hasRevealedExtendedMedia() {
        return (
            messageOwner.media != null &&
            !(messageOwner.media instanceof TLRPC.TL_messageMediaPaidMedia) &&
            !messageOwner.media.extended_media.isEmpty() &&
            messageOwner.media.extended_media.get(0) instanceof TLRPC.TL_messageExtendedMedia
        );
    }

    public boolean hasExtendedMedia() {
        return messageOwner.media != null && !(messageOwner.media instanceof TLRPC.TL_messageMediaPaidMedia) && !messageOwner.media.extended_media.isEmpty();
    }

    public boolean hasPaidMediaPreview() {
        return (
            messageOwner.media != null &&
            messageOwner.media instanceof TLRPC.TL_messageMediaPaidMedia &&
            !messageOwner.media.extended_media.isEmpty() &&
            messageOwner.media.extended_media.get(0) instanceof TLRPC.TL_messageExtendedMediaPreview
        );
    }

    public boolean hasExtendedMediaPreview() {
        return (
            messageOwner.media != null &&
            !(messageOwner.media instanceof TLRPC.TL_messageMediaPaidMedia) &&
            !messageOwner.media.extended_media.isEmpty() &&
            messageOwner.media.extended_media.get(0) instanceof TLRPC.TL_messageExtendedMediaPreview
        );
    }

    private boolean hasNonEmojiEntities() {
        if (messageOwner == null || messageOwner.entities == null)
            return false;
        for (int i = 0; i < messageOwner.entities.size(); ++i)
            if (!(messageOwner.entities.get(i) instanceof TLRPC.TL_messageEntityCustomEmoji))
                return true;
        return false;
    }

    public void setType() {
        int oldType = type;
        type = 1000;
        isRoundVideoCached = 0;
        if (isSponsored()) {
            type = TYPE_TEXT;
        } else if (channelJoined) {
            contentType = 0;
            type = TYPE_JOINED_CHANNEL;
            channelJoinedExpanded = MessagesController.getInstance(currentAccount).getMainSettings().getBoolean("c" + getDialogId() + "_rec", true);
        } else if (messageOwner instanceof TLRPC.TL_message || messageOwner instanceof TLRPC.TL_messageForwarded_old2) {
            if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaUnsupported) {
                contentType = 10;
                type = TYPE_UNSUPPORTED;
            } else if (isRestrictedMessage) {
                type = TYPE_TEXT;
            } else if (messageOwner.rich_message != null) {
                type = TYPE_ARTICLE;
//                contentType = 9;
            } else if (emojiAnimatedSticker != null || emojiAnimatedStickerId != null) {
                if (isSticker()) {
                    type = TYPE_STICKER;
                } else {
                    type = TYPE_ANIMATED_STICKER;
                }
            } else if (messageOwner.media instanceof TLRPC.TL_messageMediaPaidMedia) {
                type = TYPE_PAID_MEDIA;
            } else if (isMediaEmpty(false) && !isDice() && !isSponsored() && emojiOnlyCount >= 1 && !hasUnwrappedEmoji && messageOwner != null && !hasNonEmojiEntities()) {
                type = TYPE_EMOJIS;
            } else if (isMediaEmpty()) {
                type = TYPE_TEXT;
                if (TextUtils.isEmpty(messageText) && eventId == 0) {
                    messageText = "";
                }
            } else if (hasExtendedMediaPreview()) {
                type = TYPE_EXTENDED_MEDIA_PREVIEW;
            } else if (getMedia(messageOwner).ttl_seconds != 0 && (getMedia(messageOwner).photo instanceof TLRPC.TL_photoEmpty || getDocument() instanceof TLRPC.TL_documentEmpty || getMedia(messageOwner) instanceof TLRPC.TL_messageMediaDocument && getDocument() == null || forceExpired)) {
                contentType = 1;
                type = TYPE_DATE;
            } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaGiveaway) {
                type = TYPE_GIVEAWAY;
            } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaGiveawayResults) {
                type = TYPE_GIVEAWAY_RESULTS;
            } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaDice) {
                type = TYPE_ANIMATED_STICKER;
                if (getMedia(messageOwner).document == null) {
                    getMedia(messageOwner).document = new TLRPC.TL_document();
                    getMedia(messageOwner).document.file_reference = new byte[0];
                    getMedia(messageOwner).document.mime_type = "application/x-tgsdice";
                    getMedia(messageOwner).document.dc_id = Integer.MIN_VALUE;
                    getMedia(messageOwner).document.id = Integer.MIN_VALUE;
                    TLRPC.TL_documentAttributeImageSize attributeImageSize = new TLRPC.TL_documentAttributeImageSize();
                    attributeImageSize.w = 512;
                    attributeImageSize.h = 512;
                    getMedia(messageOwner).document.attributes.add(attributeImageSize);
                }
            } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaPhoto) {
                type = isVideo() ? TYPE_VIDEO : TYPE_PHOTO;
            } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaGeo || getMedia(messageOwner) instanceof TLRPC.TL_messageMediaVenue || getMedia(messageOwner) instanceof TLRPC.TL_messageMediaGeoLive) {
                type = TYPE_GEO;
            } else if (isRoundVideo()) {
                type = TYPE_ROUND_VIDEO;
            } else if (isVideo()) {
                type = TYPE_VIDEO;
            } else if (isVoice()) {
                type = TYPE_VOICE;
            } else if (isMusic()) {
                type = TYPE_MUSIC;
            } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaContact) {
                type = TYPE_CONTACT;
            } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaPoll || getMedia(messageOwner) instanceof TLRPC.TL_messageMediaToDo) {
                type = TYPE_POLL;
                checkedVotes = new ArrayList<>();
            } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaUnsupported) {
                type = TYPE_TEXT;
            } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaDocument) {
                TLRPC.Document document = getDocument();
                if (document != null && document.mime_type != null) {
                    if (isGifDocument(document, hasValidGroupId())) {
                        type = TYPE_GIF;
                    } else if (isSticker()) {
                        type = TYPE_STICKER;
                    } else if (isAnimatedSticker()) {
                        type = TYPE_ANIMATED_STICKER;
                    } else {
                        type = TYPE_FILE;
                    }
                } else {
                    type = TYPE_FILE;
                }
            } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaGame) {
                type = TYPE_TEXT;
            } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaInvoice) {
                type = TYPE_TEXT;
            } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaStory) {
                type = getMedia(messageOwner).via_mention ? TYPE_STORY_MENTION : TYPE_STORY;
                if (type == TYPE_STORY_MENTION) {
                    contentType = 1;
                }
            }
        } else if (currentEvent != null && currentEvent.action instanceof TLRPC.TL_channelAdminLogEventActionChangeWallpaper) {
            TLRPC.TL_channelAdminLogEventActionChangeWallpaper wallPaper = (TLRPC.TL_channelAdminLogEventActionChangeWallpaper) currentEvent.action;
            contentType = 1;
            if (wallPaper.new_value instanceof TLRPC.TL_wallPaperNoFile && wallPaper.new_value.id == 0 && wallPaper.new_value.settings == null) {
                type = TYPE_DATE;
            } else {
                type = TYPE_ACTION_WALLPAPER;
                photoThumbs = new ArrayList<>();
                if (wallPaper.new_value.document != null) {
                    photoThumbs.addAll(wallPaper.new_value.document.thumbs);
                    photoThumbsObject = wallPaper.new_value.document;
                }
            }
        } else if (messageOwner instanceof TLRPC.TL_messageService) {
            if (messageOwner.action instanceof TLRPC.TL_messageActionChangeCreator || messageOwner.action instanceof TLRPC.TL_messageActionNewCreatorPending || messageOwner.action instanceof TLRPC.TL_messageActionManagedBotCreated) {
                contentType = 1;
                type = TYPE_DATE;
            } else if (messageOwner.action instanceof TLRPC.TL_messageActionSetSameChatWallPaper) {
                contentType = 1;
                type = TYPE_DATE;
            } else if (messageOwner.action instanceof TLRPC.TL_messageActionSetChatWallPaper) {
                contentType = 1;
                type = TYPE_ACTION_WALLPAPER;
                TLRPC.TL_messageActionSetChatWallPaper wallPaper = (TLRPC.TL_messageActionSetChatWallPaper) messageOwner.action;
                photoThumbs = new ArrayList<>();
                if (wallPaper.wallpaper.document != null) {
                    photoThumbs.addAll(wallPaper.wallpaper.document.thumbs);
                    photoThumbsObject = wallPaper.wallpaper.document;
                }
            } else if (messageOwner.action instanceof TLRPC.TL_messageActionSuggestProfilePhoto) {
                contentType = 1;
                type = TYPE_SUGGEST_PHOTO;
                photoThumbs = new ArrayList<>();
                photoThumbs.addAll(messageOwner.action.photo.sizes);
                photoThumbsObject = messageOwner.action.photo;
            } else if (messageOwner.action instanceof TLRPC.TL_messageActionLoginUnknownLocation) {
                type = TYPE_TEXT;
            } else if (messageOwner.action instanceof TLRPC.TL_messageActionGiftCode && ((TLRPC.TL_messageActionGiftCode) messageOwner.action).boost_peer != null) {
                contentType = 1;
                type = TYPE_GIFT_PREMIUM_CHANNEL;
            } else if (messageOwner.action instanceof TLRPC.TL_messageActionGiftPremium || messageOwner.action instanceof TLRPC.TL_messageActionGiftCode) {
                contentType = 1;
                type = TYPE_GIFT_PREMIUM;
            } else if (messageOwner.action instanceof TLRPC.TL_messageActionGiftStars || messageOwner.action instanceof TLRPC.TL_messageActionStarGift || messageOwner.action instanceof TLRPC.TL_messageActionPrizeStars || messageOwner.action instanceof TLRPC.TL_messageActionStarGiftUnique && ((TLRPC.TL_messageActionStarGiftUnique) messageOwner.action).refunded || messageOwner.action instanceof TLRPC.TL_messageActionGiftTon) {
                contentType = 1;
                type = TYPE_GIFT_STARS;
            } else if (messageOwner.action instanceof TLRPC.TL_messageActionChatEditPhoto || messageOwner.action instanceof TLRPC.TL_messageActionUserUpdatedPhoto) {
                contentType = 1;
                type = TYPE_ACTION_PHOTO;
            } else if (messageOwner.action instanceof TLRPC.TL_messageEncryptedAction) {
                if (messageOwner.action.encryptedAction instanceof TLRPC.TL_decryptedMessageActionScreenshotMessages || messageOwner.action.encryptedAction instanceof TLRPC.TL_decryptedMessageActionSetMessageTTL) {
                    contentType = 1;
                    type = TYPE_DATE;
                } else {
                    contentType = -1;
                    type = -1;
                }
            } else if (messageOwner.action instanceof TLRPC.TL_messageActionHistoryClear) {
                contentType = -1;
                type = -1;
            } else if (messageOwner.action instanceof TLRPC.TL_messageActionPhoneCall || messageOwner.action instanceof TLRPC.TL_messageActionConferenceCall) {
                type = TYPE_PHONE_CALL;
            } else if (messageOwner.action instanceof TLRPC.TL_messageActionSetChatTheme && ((TLRPC.TL_messageActionSetChatTheme) messageOwner.action).theme instanceof TLRPC.TL_chatThemeUniqueGift) {
                type = TYPE_GIFT_THEME_UPDATE;
                contentType = 1;
            } else if (messageOwner.action instanceof TLRPC.TL_messageActionStarGiftPurchaseOffer) {
                type = TYPE_GIFT_OFFER;
                contentType = 1;
            } else if (messageOwner.action instanceof TLRPC.TL_messageActionChangeCommunity && ((TLRPC.TL_messageActionChangeCommunity) messageOwner.action).community_id != 0) {
                type = TYPE_COMMUNITY_CHANGED;
                contentType = 1;
            } else if (messageOwner.action instanceof TLRPC.TL_messageActionStarGiftPurchaseOfferDeclined) {
                type = TYPE_GIFT_OFFER_REJECTED;
                contentType = 1;
            } else if (messageOwner.action instanceof TLRPC.TL_messageActionSuggestBirthday) {
                type = TYPE_SUGGEST_BIRTHDAY;
                contentType = 1;
            } else if (messageOwner.action instanceof TLRPC.TL_messageActionNoForwardsRequest) {
                type = TYPE_SHARING_OFFER;
                contentType = 1;
            } else {
                contentType = 1;
                type = TYPE_DATE;
            }
        }
        if (oldType != 1000 && oldType != type && type != TYPE_EMOJIS) {
            updateMessageText(MessagesController.getInstance(currentAccount).getUsers(), MessagesController.getInstance(currentAccount).getChats(), null, null);
            generateThumbs(false);
        }
    }

    public boolean checkLayout() {
        if (type != TYPE_TEXT && type != TYPE_EMOJIS && type != TYPE_ARTICLE || messageOwner.peer_id == null || messageText == null || messageText.length() == 0 && !isBotPendingDraft) {
            return false;
        }
        if (layoutCreated) {
            int newMinSize = AndroidUtilities.isTablet() ? AndroidUtilities.getMinTabletSide() : AndroidUtilities.displaySize.x;
            float newFontSize = Theme.chat_msgTextPaint != null ? Theme.chat_msgTextPaint.getTextSize() : 0;
            if (Math.abs(generatedWithMinSize - newMinSize) > dp(52) || generatedWithDensity != AndroidUtilities.density || generatedWithFontSize != newFontSize) {
                layoutCreated = false;
            }
        }
        if (!layoutCreated) {
            layoutCreated = true;
            TLRPC.User fromUser = null;
            if (isFromUser()) {
                fromUser = MessagesController.getInstance(currentAccount).getUser(messageOwner.from_id.user_id);
            }
            TextPaint paint;
            if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaGame) {
                paint = Theme.chat_msgGameTextPaint;
            } else {
                paint = Theme.chat_msgTextPaint;
            }
            int[] emojiOnly = allowsBigEmoji() ? new int[1] : null;
            messageText = Emoji.replaceEmoji(messageText, paint.getFontMetricsInt(), false, emojiOnly);
            messageText = replaceAnimatedEmoji(messageText, paint.getFontMetricsInt());
            if (emojiOnly != null && emojiOnly[0] > 1) {
                replaceEmojiToLottieFrame(messageText, emojiOnly);
            }
            checkEmojiOnly(emojiOnly);
            checkBigAnimatedEmoji();
            setType();
            generateLayout(fromUser);
            if (caption != null) {
                caption = null;
                generateCaption();
            }
            return true;
        }
        return false;
    }

    public void resetLayout() {
        layoutCreated = false;
    }

    public String getMimeType() {
        TLRPC.Document document = getDocument();
        if (document != null) {
            return document.mime_type;
        } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaInvoice) {
            TLRPC.WebDocument photo = ((TLRPC.TL_messageMediaInvoice) getMedia(messageOwner)).webPhoto;
            if (photo != null) {
                return photo.mime_type;
            }
        } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaPhoto) {
            return "image/jpeg";
        } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaWebPage) {
            if (getMedia(messageOwner).webpage.photo != null) {
                return "image/jpeg";
            }
        }
        return "";
    }

    public boolean canPreviewDocument() {
        return canPreviewDocument(getDocument());
    }

    public static boolean isAnimatedStickerDocument(TLRPC.Document document) {
        return document != null && document.mime_type.equals("video/webm");
    }

    public static boolean isStaticStickerDocument(TLRPC.Document document) {
        return document != null && document.mime_type.equals("image/webp");
    }

    public static boolean isGifDocument(WebFile document) {
        return document != null && (document.mime_type.equals("image/gif") || isNewGifDocument(document));
    }

    public static boolean isGifDocument(TLRPC.Document document) {
        return isGifDocument(document, false);
    }

    public static boolean isGifDocument(TLRPC.Document document, boolean hasGroup) {
        return document != null && document.mime_type != null && (document.mime_type.equals("image/gif") && !hasGroup || isNewGifDocument(document));
    }

    public static boolean isDocumentHasThumb(TLRPC.Document document) {
        if (document == null || document.thumbs.isEmpty()) {
            return false;
        }
        for (int a = 0, N = document.thumbs.size(); a < N; a++) {
            TLRPC.PhotoSize photoSize = document.thumbs.get(a);
            if (photoSize != null && !(photoSize instanceof TLRPC.TL_photoSizeEmpty) && (!(photoSize.location instanceof TLRPC.TL_fileLocationUnavailable) || photoSize.bytes != null)) {
                return true;
            }
        }
        return false;
    }

    public static boolean canPreviewDocument(TLRPC.Document document) {
        if (document != null && document.mime_type != null) {
            String mime = document.mime_type;
            if (isDocumentHasThumb(document) && (mime.equalsIgnoreCase("image/png") || mime.equalsIgnoreCase("image/jpg") || mime.equalsIgnoreCase("image/jpeg")) || (Build.VERSION.SDK_INT >= 26 && (mime.equalsIgnoreCase("image/heic")))) {
                for (int a = 0; a < document.attributes.size(); a++) {
                    TLRPC.DocumentAttribute attribute = document.attributes.get(a);
                    if (attribute instanceof TLRPC.TL_documentAttributeImageSize) {
                        TLRPC.TL_documentAttributeImageSize size = (TLRPC.TL_documentAttributeImageSize) attribute;
                        return size.w < 6000 && size.h < 6000;
                    }
                }
            } else if (BuildVars.DEBUG_PRIVATE_VERSION) {
                String fileName = FileLoader.getDocumentFileName(document);
                if (fileName.startsWith("tg_secret_sticker") && fileName.endsWith("json")) {
                    return true;
                } else if (fileName.endsWith(".svg")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isRoundVideoDocument(TLRPC.Document document) {
        if (document != null && "video/mp4".equals(document.mime_type)) {
            int width = 0;
            int height = 0;
            boolean round = false;
            for (int a = 0; a < document.attributes.size(); a++) {
                TLRPC.DocumentAttribute attribute = document.attributes.get(a);
                if (attribute instanceof TLRPC.TL_documentAttributeVideo) {
                    width = attribute.w;
                    height = attribute.h;
                    round = attribute.round_message;
                }
            }
            if (round && width <= 1280 && height <= 1280) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNewGifDocument(WebFile document) {
        if (document != null && "video/mp4".equals(document.mime_type)) {
            int width = 0;
            int height = 0;
            //boolean animated = false;
            for (int a = 0; a < document.attributes.size(); a++) {
                TLRPC.DocumentAttribute attribute = document.attributes.get(a);
                if (attribute instanceof TLRPC.TL_documentAttributeAnimated) {
                    //animated = true;
                } else if (attribute instanceof TLRPC.TL_documentAttributeVideo) {
                    width = attribute.w;
                    height = attribute.h;
                }
            }
            if (/*animated && */width <= 1280 && height <= 1280) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNewGifDocument(TLRPC.Document document) {
        if (document != null && "video/mp4".equals(document.mime_type)) {
            int width = 0;
            int height = 0;
            boolean animated = false;
            for (int a = 0; a < document.attributes.size(); a++) {
                TLRPC.DocumentAttribute attribute = document.attributes.get(a);
                if (attribute instanceof TLRPC.TL_documentAttributeAnimated) {
                    animated = true;
                } else if (attribute instanceof TLRPC.TL_documentAttributeVideo) {
                    width = attribute.w;
                    height = attribute.h;
                }
            }
            if (animated && width <= 1280 && height <= 1280) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSystemSignUp(MessageObject message) {
        return message != null && message.messageOwner instanceof TLRPC.TL_messageService && ((TLRPC.TL_messageService) message.messageOwner).action instanceof TLRPC.TL_messageActionContactSignUp;
    }

    public void generateThumbs(boolean update) {
        if (hasExtendedMediaPreview()) {
            TLRPC.TL_messageExtendedMediaPreview preview = (TLRPC.TL_messageExtendedMediaPreview) messageOwner.media.extended_media.get(0);
            if (!update) {
                photoThumbs = new ArrayList<>(Collections.singletonList(preview.thumb));
            } else {
                updatePhotoSizeLocations(photoThumbs, Collections.singletonList(preview.thumb));
            }
            photoThumbsObject = messageOwner;

            if (strippedThumb == null) {
                createStrippedThumb();
            }
        } else if (messageOwner instanceof TLRPC.TL_messageService) {
            if (messageOwner.action instanceof TLRPC.TL_messageActionChatEditPhoto) {
                TLRPC.Photo photo = messageOwner.action.photo;
                if (!update) {
                    photoThumbs = new ArrayList<>(photo.sizes);
                } else if (photoThumbs != null && !photoThumbs.isEmpty()) {
                    for (int a = 0; a < photoThumbs.size(); a++) {
                        TLRPC.PhotoSize photoObject = photoThumbs.get(a);
                        for (int b = 0; b < photo.sizes.size(); b++) {
                            TLRPC.PhotoSize size = photo.sizes.get(b);
                            if (size instanceof TLRPC.TL_photoSizeEmpty) {
                                continue;
                            }
                            if (size.type.equals(photoObject.type)) {
                                photoObject.location = size.location;
                                break;
                            }
                        }
                    }
                }
                if (photo.dc_id != 0 && photoThumbs != null) {
                    for (int a = 0, N = photoThumbs.size(); a < N; a++) {
                        TLRPC.FileLocation location = photoThumbs.get(a).location;
                        if (location == null) {
                            continue;
                        }
                        location.dc_id = photo.dc_id;
                        location.file_reference = photo.file_reference;
                    }
                }
                photoThumbsObject = messageOwner.action.photo;
            }
        } else if (emojiAnimatedSticker != null || emojiAnimatedStickerId != null) {
            if (TextUtils.isEmpty(emojiAnimatedStickerColor) && isDocumentHasThumb(emojiAnimatedSticker)) {
                if (!update || photoThumbs == null) {
                    photoThumbs = new ArrayList<>();
                    photoThumbs.addAll(emojiAnimatedSticker.thumbs);
                } else if (!photoThumbs.isEmpty()) {
                    updatePhotoSizeLocations(photoThumbs, emojiAnimatedSticker.thumbs);
                }
                photoThumbsObject = emojiAnimatedSticker;
            }
        } else if (getMedia(messageOwner) != null && !(getMedia(messageOwner) instanceof TLRPC.TL_messageMediaEmpty)) {
            if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaPhoto) {
                TLRPC.Photo photo = getMedia(messageOwner).photo;
                if (!update || photoThumbs != null && photoThumbs.size() != photo.sizes.size()) {
                    photoThumbs = new ArrayList<>(photo.sizes);
                } else if (photoThumbs != null && !photoThumbs.isEmpty()) {
                    for (int a = 0; a < photoThumbs.size(); a++) {
                        TLRPC.PhotoSize photoObject = photoThumbs.get(a);
                        if (photoObject == null) {
                            continue;
                        }
                        for (int b = 0; b < photo.sizes.size(); b++) {
                            TLRPC.PhotoSize size = photo.sizes.get(b);
                            if (size == null || size instanceof TLRPC.TL_photoSizeEmpty) {
                                continue;
                            }
                            if (size.type.equals(photoObject.type)) {
                                photoObject.location = size.location;
                                break;
                            } else if ("s".equals(photoObject.type) && size instanceof TLRPC.TL_photoStrippedSize) {
                                photoThumbs.set(a, size);
                                break;
                            }
                        }
                    }
                }
                photoThumbsObject = getMedia(messageOwner).photo;
            } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaDocument) {
                TLRPC.TL_messageMediaDocument mediaDocument = (TLRPC.TL_messageMediaDocument) getMedia(messageOwner);
                if (mediaDocument.video_cover != null) {
                    TLRPC.Photo photo = mediaDocument.video_cover;
                    if (!update || photoThumbs != null && photoThumbs.size() != photo.sizes.size()) {
                        photoThumbs = new ArrayList<>(photo.sizes);
                    } else if (photoThumbs != null && !photoThumbs.isEmpty()) {
                        for (int a = 0; a < photoThumbs.size(); a++) {
                            TLRPC.PhotoSize photoObject = photoThumbs.get(a);
                            if (photoObject == null) {
                                continue;
                            }
                            for (int b = 0; b < photo.sizes.size(); b++) {
                                TLRPC.PhotoSize size = photo.sizes.get(b);
                                if (size == null || size instanceof TLRPC.TL_photoSizeEmpty) {
                                    continue;
                                }
                                if (size.type.equals(photoObject.type)) {
                                    photoObject.location = size.location;
                                    break;
                                } else if ("s".equals(photoObject.type) && size instanceof TLRPC.TL_photoStrippedSize) {
                                    photoThumbs.set(a, size);
                                    break;
                                }
                            }
                        }
                    }
                    photoThumbsObject = photo;
                } else {
                    TLRPC.Document document = getDocument();
                    if (isDocumentHasThumb(document)) {
                        if (!update || photoThumbs == null) {
                            photoThumbs = new ArrayList<>();
                            photoThumbs.addAll(document.thumbs);
                        } else if (!photoThumbs.isEmpty()) {
                            updatePhotoSizeLocations(photoThumbs, document.thumbs);
                        }
                        photoThumbsObject = document;
                    }
                }
            } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaGame) {
                TLRPC.Document document = getMedia(messageOwner).game.document;
                if (document != null) {
                    if (isDocumentHasThumb(document)) {
                        if (!update) {
                            photoThumbs = new ArrayList<>();
                            photoThumbs.addAll(document.thumbs);
                        } else if (photoThumbs != null && !photoThumbs.isEmpty()) {
                            updatePhotoSizeLocations(photoThumbs, document.thumbs);
                        }
                        photoThumbsObject = document;
                    }
                }
                TLRPC.Photo photo = getMedia(messageOwner).game.photo;
                if (photo != null) {
                    if (!update || photoThumbs2 == null) {
                        photoThumbs2 = new ArrayList<>(photo.sizes);
                    } else if (!photoThumbs2.isEmpty()) {
                        updatePhotoSizeLocations(photoThumbs2, photo.sizes);
                    }
                    photoThumbsObject2 = photo;
                }
                if (photoThumbs == null && photoThumbs2 != null) {
                    photoThumbs = photoThumbs2;
                    photoThumbs2 = null;
                    photoThumbsObject = photoThumbsObject2;
                    photoThumbsObject2 = null;
                }
            } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaWebPage) {
                TLRPC.Photo photo = getMedia(messageOwner).webpage.photo;
                TLRPC.Document document = getMedia(messageOwner).webpage.document;
                if (photo != null) {
                    if (!update || photoThumbs == null) {
                        photoThumbs = new ArrayList<>(photo.sizes);
                    } else if (!photoThumbs.isEmpty()) {
                        updatePhotoSizeLocations(photoThumbs, photo.sizes);
                    }
                    photoThumbsObject = photo;
                } else if (document != null) {
                    if (isDocumentHasThumb(document)) {
                        if (!update) {
                            photoThumbs = new ArrayList<>();
                            photoThumbs.addAll(document.thumbs);
                        } else if (photoThumbs != null && !photoThumbs.isEmpty()) {
                            updatePhotoSizeLocations(photoThumbs, document.thumbs);
                        }
                        photoThumbsObject = document;
                    }
                }
            }
        } else if (messageOwner != null && messageOwner.rich_message != null) {
            final TLRPC.Document video = findVideo(messageOwner.rich_message);
            final TLRPC.Photo photo = findPhoto(messageOwner.rich_message);
            if (video != null) {
                if (isDocumentHasThumb(video)) {
                    if (!update) {
                        photoThumbs = new ArrayList<>();
                        photoThumbs.addAll(video.thumbs);
                    } else if (photoThumbs != null && !photoThumbs.isEmpty()) {
                        updatePhotoSizeLocations(photoThumbs, video.thumbs);
                    }
                    photoThumbsObject = video;
                }
            } else if (photo != null) {
                if (!update || photoThumbs == null) {
                    photoThumbs = new ArrayList<>(photo.sizes);
                } else if (!photoThumbs.isEmpty()) {
                    updatePhotoSizeLocations(photoThumbs, photo.sizes);
                }
                photoThumbsObject = photo;
                if (strippedThumb == null) {
                    createStrippedThumb();
                }
            }
        } else if (sponsoredMedia != null) {
            TLRPC.Photo photo = sponsoredMedia.photo;
            TLRPC.Document document = sponsoredMedia.document;
            if (photo != null) {
                if (!update || photoThumbs == null) {
                    photoThumbs = new ArrayList<>(photo.sizes);
                } else if (!photoThumbs.isEmpty()) {
                    updatePhotoSizeLocations(photoThumbs, photo.sizes);
                }
                photoThumbsObject = photo;
            } else if (document != null) {
                if (isDocumentHasThumb(document)) {
                    if (!update) {
                        photoThumbs = new ArrayList<>();
                        photoThumbs.addAll(document.thumbs);
                    } else if (photoThumbs != null && !photoThumbs.isEmpty()) {
                        updatePhotoSizeLocations(photoThumbs, document.thumbs);
                    }
                    photoThumbsObject = document;
                }
            }
        } else if (sponsoredPhoto != null) {
            if (!update || photoThumbs == null) {
                photoThumbs = new ArrayList<>(sponsoredPhoto.sizes);
            } else if (!photoThumbs.isEmpty()) {
                updatePhotoSizeLocations(photoThumbs, sponsoredPhoto.sizes);
            }
            photoThumbsObject = sponsoredPhoto;
            if (strippedThumb == null) {
                createStrippedThumb();
            }
        }
    }

    private static void updatePhotoSizeLocations(ArrayList<TLRPC.PhotoSize> o, List<TLRPC.PhotoSize> n) {
        for (int a = 0, N = o.size(); a < N; a++) {
            TLRPC.PhotoSize photoObject = o.get(a);
            if (photoObject == null) {
                continue;
            }
            for (int b = 0, N2 = n.size(); b < N2; b++) {
                TLRPC.PhotoSize size = n.get(b);
                if (size instanceof TLRPC.TL_photoSizeEmpty || size instanceof TLRPC.TL_photoCachedSize || size == null) {
                    continue;
                }
                if (size.type.equals(photoObject.type)) {
                    photoObject.location = size.location;
                    break;
                }
            }
        }
    }

    public CharSequence replaceWithLink(CharSequence source, String param, ArrayList<Long> uids, AbstractMap<Long, TLRPC.User> usersDict, LongSparseArray<TLRPC.User> sUsersDict) {
        int start = TextUtils.indexOf(source, param);
        if (start >= 0) {
            SpannableStringBuilder names = new SpannableStringBuilder("");
            for (int a = 0; a < uids.size(); a++) {
                TLRPC.User user = null;
                if (usersDict != null) {
                    user = usersDict.get(uids.get(a));
                } else if (sUsersDict != null) {
                    user = sUsersDict.get(uids.get(a));
                }
                if (user == null) {
                    user = MessagesController.getInstance(currentAccount).getUser(uids.get(a));
                }
                if (user != null) {
                    String name = UserObject.getUserName(user);
                    start = names.length();
                    if (names.length() != 0) {
                        names.append(", ");
                    }
                    names.append(name);
                    names.setSpan(new URLSpanNoUnderlineBold("" + user.id), start, start + name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            return TextUtils.replace(source, new String[]{param}, new CharSequence[]{names});
        }
        return source;
    }

    public static CharSequence replaceWithLink(CharSequence source, String param, CharSequence object) {
        int start = TextUtils.indexOf(source, param);
        if (start >= 0) {
            return TextUtils.replace(source, new String[]{param}, new CharSequence[]{object});
        }
        return source;
    }

    public static CharSequence replaceWithLink(CharSequence source, String param, TLObject object) {
        int start = TextUtils.indexOf(source, param);
        if (start >= 0) {
            CharSequence name;
            String id;
            TLObject spanObject = null;
            if (object instanceof TLRPC.User) {
                name = UserObject.getUserName((TLRPC.User) object).replace('\n', ' ');
                id = "" + ((TLRPC.User) object).id;
            } else if (object instanceof TLRPC.Chat) {
                name = ((TLRPC.Chat) object).title.replace('\n', ' ');
                id = "" + -((TLRPC.Chat) object).id;
            } else if (object instanceof TLRPC.TL_game) {
                TLRPC.TL_game game = (TLRPC.TL_game) object;
                name = game.title.replace('\n', ' ');
                id = "game";
            } else if (object instanceof TLRPC.TL_chatInviteExported) {
                TLRPC.TL_chatInviteExported invite = (TLRPC.TL_chatInviteExported) object;
                name = invite.link.replace('\n', ' ');
                id = "invite";
                spanObject = invite;
            } else if (object instanceof TLRPC.ForumTopic) {
                name = ForumUtilities.getTopicSpannedName((TLRPC.ForumTopic) object, null, false);
                id = "topic";
                spanObject = object;
            } else {
                name = "";
                id = "0";
            }
            SpannableStringBuilder builder = new SpannableStringBuilder(TextUtils.replace(source, new String[]{param}, new CharSequence[]{name}));
            URLSpanNoUnderlineBold span = new URLSpanNoUnderlineBold("" + id);
            span.setObject(spanObject);
            builder.setSpan(span, start, start + name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return builder;
        }
        return source;
    }

    public String getExtension() {
        String fileName = getFileName();
        int idx = fileName.lastIndexOf('.');
        String ext = null;
        if (idx != -1) {
            ext = fileName.substring(idx + 1);
        }
        if (ext == null || ext.length() == 0) {
            ext = getDocument().mime_type;
        }
        if (ext == null) {
            ext = "";
        }
        ext = ext.toUpperCase();
        return ext;
    }

    public String getFileName() {
        if (getDocument() != null) {
            return getFileName(getDocument());
        }
        return getFileName(messageOwner);
    }

    public String getFileNameFast() {
        if (getDocumentFast() != null) {
            return getFileName(getDocumentFast());
        }
        return getFileName(messageOwner);
    }

    public static String getFileName(TLRPC.Message messageOwner) {
        if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaDocument) {
            return getFileName(getDocument(messageOwner));
        } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaPhoto) {
            ArrayList<TLRPC.PhotoSize> sizes = getMedia(messageOwner).photo.sizes;
            if (sizes.size() > 0) {
                TLRPC.PhotoSize sizeFull = FileLoader.getClosestPhotoSizeWithSize(sizes, AndroidUtilities.getPhotoSize());
                if (sizeFull != null) {
                    return FileLoader.getAttachFileName(sizeFull);
                }
            }
        } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaWebPage && getMedia(messageOwner).webpage != null) {
            return getFileName(getMedia(messageOwner).webpage.document);
        }
        return "";
    }

    public static String getFileName(TLRPC.Document document) {
        return FileLoader.getAttachFileName(document);
    }

    public static String getFileName(TLRPC.MessageMedia media) {
        if (media instanceof TLRPC.TL_messageMediaDocument) {
            return FileLoader.getAttachFileName(media.document);
        } else if (media instanceof TLRPC.TL_messageMediaPhoto) {
            ArrayList<TLRPC.PhotoSize> sizes = media.photo.sizes;
            if (sizes.size() > 0) {
                TLRPC.PhotoSize sizeFull = FileLoader.getClosestPhotoSizeWithSize(sizes, AndroidUtilities.getPhotoSize());
                if (sizeFull != null) {
                    return FileLoader.getAttachFileName(sizeFull);
                }
            }
        } else if (media instanceof TLRPC.TL_messageMediaWebPage && media.webpage != null) {
            return FileLoader.getAttachFileName(media.webpage.document);
        }
        return "";
    }

    public int getMediaType() {
        if (isVideo()) {
            return FileLoader.MEDIA_DIR_VIDEO;
        } else if (isVoice()) {
            return FileLoader.MEDIA_DIR_AUDIO;
        } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaDocument) {
            return FileLoader.MEDIA_DIR_DOCUMENT;
        } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaPhoto) {
            return FileLoader.MEDIA_DIR_IMAGE;
        }
        return FileLoader.MEDIA_DIR_CACHE;
    }

    public static boolean containsUrls(CharSequence message) {
        if (message == null || message.length() < 2 || message.length() > 1024 * 20) {
            return false;
        }

        int length = message.length();

        int digitsInRow = 0;
        int schemeSequence = 0;
        int dotSequence = 0;

        char lastChar = 0;

        for (int i = 0; i < length; i++) {
            char c = message.charAt(i);

            if (c >= '0' && c <= '9') {
                digitsInRow++;
                if (digitsInRow >= 6) {
                    return true;
                }
                schemeSequence = 0;
                dotSequence = 0;
            } else if (!(c != ' ' && digitsInRow > 0)) {
                digitsInRow = 0;
            }
            if ((c == '@' || c == '#' || c == '/' || c == '$') && i == 0 || i != 0 && (message.charAt(i - 1) == ' ' || message.charAt(i - 1) == '\n')) {
                return true;
            }
            if (c == ':') {
                if (schemeSequence == 0) {
                    schemeSequence = 1;
                } else {
                    schemeSequence = 0;
                }
            } else if (c == '/') {
                if (schemeSequence == 2) {
                    return true;
                }
                if (schemeSequence == 1) {
                    schemeSequence++;
                } else {
                    schemeSequence = 0;
                }
            } else if (c == '.') {
                if (dotSequence == 0 && lastChar != ' ') {
                    dotSequence++;
                } else {
                    dotSequence = 0;
                }
            } else if (c != ' ' && lastChar == '.' && dotSequence == 1) {
                return true;
            } else {
                dotSequence = 0;
            }
            lastChar = c;
        }
        return false;
    }

    public void generateLinkDescription() {
        if (linkDescription != null) {
            return;
        }
        boolean allowUsernames = false;
        int hashtagsType = 0;
        TLRPC.WebPage webpage = null;
        if (storyMentionWebpage != null) {
            webpage = storyMentionWebpage;
        } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaWebPage) {
            webpage = ((TLRPC.TL_messageMediaWebPage) getMedia(messageOwner)).webpage;
        }
        if (webpage != null) {
            for (int i = 0; i < webpage.attributes.size(); ++i) {
                TLRPC.WebPageAttribute attr = webpage.attributes.get(i);
                if (attr instanceof TLRPC.TL_webPageAttributeStory) {
                    TLRPC.TL_webPageAttributeStory storyAttr = (TLRPC.TL_webPageAttributeStory) attr;
                    if (storyAttr.storyItem != null && storyAttr.storyItem.caption != null) {
                        linkDescription = new SpannableStringBuilder(storyAttr.storyItem.caption);
                        webPageDescriptionEntities = storyAttr.storyItem.entities;
                        allowUsernames = true;
                        break;
                    }
                }
            }
        }
        if (linkDescription == null) {
            if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaWebPage && getMedia(messageOwner).webpage instanceof TLRPC.TL_webPage && getMedia(messageOwner).webpage.description != null) {
                linkDescription = Spannable.Factory.getInstance().newSpannable(getMedia(messageOwner).webpage.description);
                String siteName = getMedia(messageOwner).webpage.site_name;
                if (siteName != null) {
                    siteName = siteName.toLowerCase();
                }
                if ("instagram".equals(siteName)) {
                    hashtagsType = 1;
                } else if ("twitter".equals(siteName)) {
                    hashtagsType = 2;
                }
            } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaGame && getMedia(messageOwner).game.description != null) {
                linkDescription = Spannable.Factory.getInstance().newSpannable(getMedia(messageOwner).game.description);
            } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaInvoice && getMedia(messageOwner).description != null) {
                linkDescription = Spannable.Factory.getInstance().newSpannable(getMedia(messageOwner).description);
            }
        }
        if (!TextUtils.isEmpty(linkDescription)) {
            if (containsUrls(linkDescription)) {
                try {
                    AndroidUtilities.addLinksSafe((Spannable) linkDescription, Linkify.WEB_URLS, false, true);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            linkDescription = Emoji.replaceEmoji(linkDescription, Theme.chat_msgTextPaint.getFontMetricsInt(), false);
            if (webPageDescriptionEntities != null) {
                addEntitiesToText(linkDescription, webPageDescriptionEntities, isOut(), allowUsernames, false, !allowUsernames);
                replaceAnimatedEmoji(linkDescription, webPageDescriptionEntities, Theme.chat_msgTextPaint.getFontMetricsInt());
            }
            if (hashtagsType != 0) {
                if (!(linkDescription instanceof Spannable)) {
                    linkDescription = new SpannableStringBuilder(linkDescription);
                }
                addUrlsByPattern(isOutOwner(), linkDescription, false, hashtagsType, 0, false);
            }
        }
    }

    public CharSequence getVoiceTranscription() {
        if (messageOwner == null || messageOwner.voiceTranscription == null) {
            return null;
        }
        if (TextUtils.isEmpty(messageOwner.voiceTranscription)) {
            SpannableString ssb = new SpannableString(getString(R.string.NoWordsRecognized));
            ssb.setSpan(new CharacterStyle() {
                @Override
                public void updateDrawState(TextPaint textPaint) {
                    textPaint.setTextSize(textPaint.getTextSize() * .8f);
                    textPaint.setColor(Theme.chat_timePaint.getColor());
                }
            }, 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return ssb;
        }
        CharSequence text = translated && messageOwner.translatedVoiceTranscription != null ? messageOwner.translatedVoiceTranscription.text : messageOwner.voiceTranscription;
        if (!TextUtils.isEmpty(text)) {
            text = Emoji.replaceEmoji(text, Theme.chat_msgTextPaint.getFontMetricsInt(), false);
        }
        return text;
    }

    public float measureVoiceTranscriptionHeight() {
        CharSequence voiceTranscription = getVoiceTranscription();
        if (voiceTranscription == null) {
            return 0;
        }
        int width = AndroidUtilities.displaySize.x - dp(this.needDrawAvatar() ? 147 : 95);
        StaticLayout captionLayout;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            captionLayout = StaticLayout.Builder.obtain(voiceTranscription, 0, voiceTranscription.length(), Theme.chat_msgTextPaint, width)
                    .setBreakStrategy(StaticLayout.BREAK_STRATEGY_HIGH_QUALITY)
                    .setHyphenationFrequency(StaticLayout.HYPHENATION_FREQUENCY_NONE)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .build();
        } else {
            captionLayout = new StaticLayout(voiceTranscription, Theme.chat_msgTextPaint, width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        }
        return captionLayout.getHeight();
    }

    public boolean isVoiceTranscriptionOpen() {
        return (
            messageOwner != null &&
            (isVoice() || isRoundVideo() && TranscribeButton.isVideoTranscriptionOpen(this)) &&
            messageOwner.voiceTranscriptionOpen &&
            messageOwner.voiceTranscription != null &&
            (messageOwner.voiceTranscriptionFinal || TranscribeButton.isTranscribing(this))
        );
    }

    private boolean captionTranslated;
    private boolean captionSummarized;

    public void generateExplanation() {
        if (type != TYPE_POLL) {
            return;
        }
        String text = null;
        ArrayList<TLRPC.MessageEntity> entities = null;
        TLRPC.MessageMedia m = getMedia(messageOwner);
        if (m instanceof TLRPC.TL_messageMediaPoll) {
            TLRPC.TL_messageMediaPoll media = (TLRPC.TL_messageMediaPoll) m;
            if (media.results != null) {
                text = media.results.solution;
                entities = media.results.solution_entities;
            }
        }

        if (text != null) {
            quizExplanation = Emoji.replaceEmoji(text, Theme.chat_explanationTextPaint.getFontMetricsInt(), false);
            quizExplanation = replaceAnimatedEmoji(quizExplanation, entities, Theme.chat_explanationTextPaint.getFontMetricsInt(), false);
            addEntitiesToText(quizExplanation, entities, isOutOwner(), true, false, false);
        } else {
            quizExplanation = null;
        }
    }

    public void generateCaption() {
        if (isRoundVideo()) return;
        if (caption != null &&
            (translated && (messageOwner.translatedText != null || summarized && messageOwner.translatedSummaryText != null)) == captionTranslated &&
            summarized == captionSummarized
        ) {
            return;
        }
        String text = messageOwner.message;
        ArrayList<TLRPC.MessageEntity> entities = messageOwner.entities;
        boolean forceManualEntities = false;
        if (type == TYPE_STORY) {
            if (messageOwner.media != null && messageOwner.media.storyItem != null) {
                text = messageOwner.media.storyItem.caption;
                entities = messageOwner.media.storyItem.entities;
                forceManualEntities = true;
            } else {
                text = "";
                entities = new ArrayList<>();
            }
        } else if (hasExtendedMedia()) {
            text = messageOwner.message = messageOwner.media.description;
        }
        if (messageOwner.translatedSummaryText != null && summarized && translated) {
            captionSummarized = true;
            captionTranslated = true;
            text = messageOwner.translatedSummaryText.text;
            entities = messageOwner.translatedSummaryText.entities;
        } else if (messageOwner.summaryText != null && summarized) {
            captionSummarized = true;
            captionTranslated = false;
            text = messageOwner.summaryText.text;
            entities = messageOwner.summaryText.entities;
        } else if (messageOwner.translatedText != null && translated) {
            captionSummarized = false;
            captionTranslated = true;
            text = messageOwner.translatedText.text;
            entities = messageOwner.translatedText.entities;
        } else {
            captionSummarized = false;
            captionTranslated = false;
        }
        if (!isMediaEmpty() && !(getMedia(messageOwner) instanceof TLRPC.TL_messageMediaGame) && !TextUtils.isEmpty(text)) {
            caption = Emoji.replaceEmoji(text, Theme.chat_msgTextPaint.getFontMetricsInt(), false);
            caption = replaceAnimatedEmoji(caption, entities, Theme.chat_msgTextPaint.getFontMetricsInt(), false);

            boolean hasEntities;
            if (messageOwner.send_state != MESSAGE_SEND_STATE_SENT) {
                hasEntities = false;
            } else {
                hasEntities = !entities.isEmpty();
            }

            boolean useManualParse = forceManualEntities || !hasEntities && (
                eventId != 0 ||
                getMedia(messageOwner) instanceof TLRPC.TL_messageMediaPhoto_old ||
                getMedia(messageOwner) instanceof TLRPC.TL_messageMediaPhoto_layer68 ||
                getMedia(messageOwner) instanceof TLRPC.TL_messageMediaPhoto_layer74 ||
                getMedia(messageOwner) instanceof TLRPC.TL_messageMediaDocument_old ||
                getMedia(messageOwner) instanceof TLRPC.TL_messageMediaDocument_layer68 ||
                getMedia(messageOwner) instanceof TLRPC.TL_messageMediaDocument_layer74 ||
                isOut() && messageOwner.send_state != MESSAGE_SEND_STATE_SENT ||
                messageOwner.id < 0
            );

            if (useManualParse) {
                if (containsUrls(caption)) {
                    try {
                        AndroidUtilities.addLinksSafe((Spannable) caption, Linkify.WEB_URLS | Linkify.PHONE_NUMBERS, false, true);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }
                addUrlsByPattern(isOutOwner(), caption, true, 0, 0, true);
            }

            addEntitiesToText(caption, useManualParse);
            caption = FormattedDateSpan.applyFormatedDateEntities(caption);
            if (isVideo()) {
                addUrlsByPattern(isOutOwner(), caption, true, 3, (int) getDuration(), false);
            } else if (isMusic() || isVoice()) {
                addUrlsByPattern(isOutOwner(), caption, true, 4, (int) getDuration(), false);
            }
            applyTimestampsHighlightForReplyMsg(caption);
        }
    }

    public static void addUrlsByPattern(boolean isOut, CharSequence charSequence, boolean botCommands, int patternType, int duration, boolean check) {
        if (charSequence == null) {
            return;
        }
        try {
            Matcher matcher;
            if (patternType == 3 || patternType == 4) {
                if (videoTimeUrlPattern == null) {
                    videoTimeUrlPattern = Pattern.compile("\\b(?:(\\d{1,2}):)?(\\d{1,3}):([0-5][0-9])\\b(?: - |)([^\\n]*)");
                }
                matcher = videoTimeUrlPattern.matcher(charSequence);
            } else if (patternType == 1) {
                if (instagramUrlPattern == null) {
                    instagramUrlPattern = Pattern.compile("(^|\\s|\\()@[a-zA-Z\\d_.]{1,32}|(^|\\s|\\()#[\\w.]+");
                }
                matcher = instagramUrlPattern.matcher(charSequence);
            } else {
                if (urlPattern == null) {
                    urlPattern = Pattern.compile("(^|\\s)/[a-zA-Z@\\d_]{1,255}|(^|\\s|\\()@[a-zA-Z\\d_]{1,32}|(^|\\s|\\()#[^0-9][\\w.]+(@[^0-9][\\w.]+)?|(^|\\s|\\()\\$[^0-9][\\w.]+(@[^0-9][\\w.]+)?|(^|\\s)\\$[A-Z]{3,8}([ ,.]|$)");
                }
                matcher = urlPattern.matcher(charSequence);
            }
            if (!(charSequence instanceof Spannable)) {
                return;
            }
            Spannable spannable = (Spannable) charSequence;
            int totalCount = 0;
            while (matcher.find() && totalCount < 100) {
                int start = matcher.start();
                int end = matcher.end();
                URLSpanNoUnderline url = null;
                if (patternType == 3 || patternType == 4) {
                    int count = matcher.groupCount();
                    int s1 = matcher.start(1);
                    int e1 = matcher.end(1);
                    int s2 = matcher.start(2);
                    int e2 = matcher.end(2);
                    int s3 = matcher.start(3);
                    int e3 = matcher.end(3);
                    int s4 = matcher.start(4);
                    int e4 = matcher.end(4);
                    int minutes = Utilities.parseInt(charSequence.subSequence(s2, e2));
                    int seconds = Utilities.parseInt(charSequence.subSequence(s3, e3));
                    int hours = s1 >= 0 && e1 >= 0 ? Utilities.parseInt(charSequence.subSequence(s1, e1)) : -1;
                    String label = s4 < 0 || e4 < 0 ? null : charSequence.subSequence(s4, e4).toString();
                    if (s4 >= 0 || e4 >= 0) {
                        end = e3;
                    }
                    URLSpan[] spans = spannable.getSpans(start, end, URLSpan.class);
                    if (spans != null && spans.length > 0) {
                        continue;
                    }
                    seconds += minutes * 60;
                    if (hours > 0) {
                        seconds += hours * 60 * 60;
                    }
                    if (seconds > duration) {
                        continue;
                    }
                    if (patternType == 3) {
                        url = new URLSpanNoUnderline("video?" + seconds);
                    } else {
                        url = new URLSpanNoUnderline("audio?" + seconds);
                    }
                    url.label = label;
                } else {
                    char ch = charSequence.charAt(start);
                    if (patternType != 0) {
                        if (ch != '@' && ch != '#') {
                            start++;
                        }
                        ch = charSequence.charAt(start);
                        if (ch != '@' && ch != '#') {
                            continue;
                        }
                    } else {
                        if (ch != '@' && ch != '#' && ch != '/' && ch != '$') {
                            start++;
                        }
                    }
                    if (patternType == 1) {
                        if (ch == '@') {
                            url = new URLSpanNoUnderline("https://instagram.com/" + charSequence.subSequence(start + 1, end).toString());
                        } else {
                            url = new URLSpanNoUnderline("https://www.instagram.com/explore/tags/" + charSequence.subSequence(start + 1, end).toString());
                        }
                    } else if (patternType == 2) {
                        if (ch == '@') {
                            url = new URLSpanNoUnderline("https://twitter.com/" + charSequence.subSequence(start + 1, end).toString());
                        } else {
                            url = new URLSpanNoUnderline("https://twitter.com/hashtag/" + charSequence.subSequence(start + 1, end).toString());
                        }
                    } else {
                        if (charSequence.charAt(start) == '/') {
                            if (botCommands) {
                                url = new URLSpanBotCommand(charSequence.subSequence(start, end).toString(), isOut ? 1 : 0);
                            }
                        } else {
                            String uri = charSequence.subSequence(start, end).toString();
                            if (uri != null) {
                                uri = uri.replaceAll("‚àï|‚ÅÑ|%E2%81%84|%E2%88%95", "/");
                            }
                            url = new URLSpanNoUnderline(uri);
                        }
                    }
                }
                if (url != null) {
                    if (check) {
                        ClickableSpan[] spans = spannable.getSpans(start, end, ClickableSpan.class);
                        if (spans != null && spans.length > 0) {
                            spannable.removeSpan(spans[0]);
                        }
                    }
                    spannable.setSpan(url, start, end, 0);
                    totalCount++;
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static int[] getWebDocumentWidthAndHeight(TLRPC.WebDocument document) {
        if (document == null) {
            return null;
        }
        for (int a = 0, size = document.attributes.size(); a < size; a++) {
            TLRPC.DocumentAttribute attribute = document.attributes.get(a);
            if (attribute instanceof TLRPC.TL_documentAttributeImageSize) {
                return new int[]{attribute.w, attribute.h};
            } else if (attribute instanceof TLRPC.TL_documentAttributeVideo) {
                return new int[]{attribute.w, attribute.h};
            }
        }
        return null;
    }

    public static double getWebDocumentDuration(TLRPC.WebDocument document) {
        if (document == null) {
            return 0;
        }
        for (int a = 0, size = document.attributes.size(); a < size; a++) {
            TLRPC.DocumentAttribute attribute = document.attributes.get(a);
            if (attribute instanceof TLRPC.TL_documentAttributeVideo) {
                return attribute.duration;
            } else if (attribute instanceof TLRPC.TL_documentAttributeAudio) {
                return attribute.duration;
            }
        }
        return 0;
    }

    public static int[] getInlineResultWidthAndHeight(TLRPC.BotInlineResult inlineResult) {
        int[] result = getWebDocumentWidthAndHeight(inlineResult.content);
        if (result == null) {
            result = getWebDocumentWidthAndHeight(inlineResult.thumb);
            if (result == null) {
                result = new int[]{0, 0};
            }
        }
        return result;
    }

    public static int getInlineResultDuration(TLRPC.BotInlineResult inlineResult) {
        int result = (int) getWebDocumentDuration(inlineResult.content);
        if (result == 0) {
            result = (int) getWebDocumentDuration(inlineResult.thumb);
        }
        return result;
    }

    // only set in searching with tags
    public boolean isPrimaryGroupMessage;
    public boolean hasValidGroupId() {
        return getGroupId() != 0 && (photoThumbs != null && !photoThumbs.isEmpty() || type == TYPE_VIDEO || type == TYPE_PHOTO || isMusic() || isDocument());
    }
    public boolean hasValidGroupIdFast() {
        return getGroupId() != 0 && (photoThumbs != null && !photoThumbs.isEmpty() || type == TYPE_VIDEO || type == TYPE_PHOTO || type == TYPE_MUSIC || type == TYPE_FILE);
    }

    public long getGroupIdForUse() {
        return localSentGroupId != 0 ? localSentGroupId : messageOwner.grouped_id;
    }

    public long getGroupId() {
        return localGroupId != 0 ? localGroupId : getGroupIdForUse();
    }

    public static void addLinks(boolean isOut, CharSequence messageText) {
        addLinks(isOut, messageText, true, false);
    }

    public static void addLinks(boolean isOut, CharSequence messageText, boolean botCommands, boolean check) {
        addLinks(isOut, messageText, botCommands, check, false);
    }

    public static void addLinks(boolean isOut, CharSequence messageText, boolean botCommands, boolean check, boolean internalOnly) {
        if (messageText instanceof Spannable && containsUrls(messageText)) {
            try {
                AndroidUtilities.addLinksSafe((Spannable) messageText, Linkify.WEB_URLS, internalOnly, false);
            } catch (Exception e) {
                FileLog.e(e);
            }
            addPhoneLinks(messageText);
            addUrlsByPattern(isOut, messageText, botCommands, 0, 0, check);
        }
    }

    public static void addPhoneLinks(CharSequence messageText) {
//        if (messageText == null || !(messageText instanceof Spannable))
//            return;
//        Spannable spannable = (Spannable) messageText;
//        SpannableString otherText = new SpannableString(spannable);
//        AndroidUtilities.doSafe(() -> Linkify.addLinks(otherText, Linkify.PHONE_NUMBERS));
//        URLSpan[] spans = otherText.getSpans(0, otherText.length(), URLSpan.class);
//        for (int i = 0; spans != null && i < spans.length; ++i) {
//            if (spans[i].getURL().startsWith("tel:")) {
//                spannable.setSpan(
//                    new URLSpanNoUnderline(spans[i].getURL()),
//                    otherText.getSpanStart(spans[i]),
//                    otherText.getSpanEnd(spans[i]),
//                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
//                );
//            }
//        }
    }

    public void resetPlayingProgress() {
        audioProgress = 0.0f;
        audioProgressSec = 0;
        bufferedProgress = 0.0f;
    }

    private boolean addEntitiesToText(CharSequence text, boolean useManualParse) {
        return addEntitiesToText(text, false, useManualParse);
    }

    public boolean addEntitiesToText(CharSequence text, boolean photoViewer, boolean useManualParse) {
        if (text == null) {
            return false;
        }
        if (isRestrictedMessage || getMedia(messageOwner) instanceof TLRPC.TL_messageMediaUnsupported) {
            ArrayList<TLRPC.MessageEntity> entities = new ArrayList<>();
            TLRPC.TL_messageEntityItalic entityItalic = new TLRPC.TL_messageEntityItalic();
            entityItalic.offset = 0;
            entityItalic.length = text.length();
            entities.add(entityItalic);
            return addEntitiesToText(text, entities, isOutOwner(), true, photoViewer, useManualParse);
        } else {
            return addEntitiesToText(text, getEntities(), isOutOwner(), true, photoViewer, useManualParse);
        }
    }

    public void replaceEmojiToLottieFrame(CharSequence text, int[] emojiOnly) {
        if (!(text instanceof Spannable)) {
            return;
        }
        Spannable spannable = (Spannable) text;
        Emoji.EmojiSpan[] spans = spannable.getSpans(0, spannable.length(), Emoji.EmojiSpan.class);
        AnimatedEmojiSpan[] aspans = spannable.getSpans(0, spannable.length(), AnimatedEmojiSpan.class);

        if (spans == null || (emojiOnly == null ? 0 : emojiOnly[0]) - spans.length - (aspans == null ? 0 : aspans.length) > 0) {
            return;
        }
        for (int i = 0; i < spans.length; ++i) {
            CharSequence emoji = spans[i].emoji;
            boolean invert = false;
            if (Emoji.endsWithRightArrow(emoji)) {
                emoji = emoji.subSequence(0, emoji.length() - 2);
                invert = true;
            }
            TLRPC.Document lottieDocument = MediaDataController.getInstance(currentAccount).getEmojiAnimatedSticker(emoji);
            if (lottieDocument != null) {
                int start = spannable.getSpanStart(spans[i]);
                int end = spannable.getSpanEnd(spans[i]);
                spannable.removeSpan(spans[i]);
                AnimatedEmojiSpan span = new AnimatedEmojiSpan(lottieDocument, spans[i].fontMetrics);
                span.standard = true;
                span.invert = invert;
                spannable.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    public ArrayList<TLRPC.MessageEntity> getEntities() {
        if (messageOwner == null) return null;
        if (summarized) {
            if (translated && messageOwner.translatedSummaryText != null) {
                return messageOwner.translatedSummaryText.entities;
            } else if (messageOwner.summaryText != null) {
                return messageOwner.summaryText.entities;
            }
            return null;
        }
        if (translated) {
            if (messageOwner.voiceTranscriptionOpen) {
                return messageOwner.translatedVoiceTranscription != null ? messageOwner.translatedVoiceTranscription.entities : null;
            } else {
                return messageOwner.translatedText != null ? messageOwner.translatedText.entities : null;
            }
        }
        return messageOwner.entities;
    }

    public Spannable replaceAnimatedEmoji(CharSequence text, Paint.FontMetricsInt fontMetricsInt) {
        return replaceAnimatedEmoji(text, getEntities(), fontMetricsInt, false);
    }

    public static Spannable replaceAnimatedEmoji(CharSequence text, ArrayList<TLRPC.MessageEntity> entities, Paint.FontMetricsInt fontMetricsInt) {
        return replaceAnimatedEmoji(text, entities, fontMetricsInt, false);
    }

    public static Spannable replaceAnimatedEmoji(CharSequence text, ArrayList<TLRPC.MessageEntity> entities, Paint.FontMetricsInt fontMetricsInt, boolean top) {
        return replaceAnimatedEmoji(text, entities, fontMetricsInt, top, 1.2f, 0);
    }

    public static Spannable replaceAnimatedEmoji(CharSequence text, ArrayList<TLRPC.MessageEntity> entities, Paint.FontMetricsInt fontMetricsInt, boolean top, float scale, int minusLimit) {
        if (text == null) {
            return null;
        }
        Spannable spannable = text instanceof Spannable ? (Spannable) text : new SpannableString(text);
        if (entities == null) {
            return spannable;
        }
        int limitCount = (SharedConfig.getDevicePerformanceClass() >= SharedConfig.PERFORMANCE_CLASS_HIGH ? 100 : 50) - minusLimit;
        Emoji.EmojiSpan[] emojiSpans = spannable.getSpans(0, spannable.length(), Emoji.EmojiSpan.class);
        for (int i = 0; i < entities.size(); ++i) {
            if (limitCount <= 0) break;
            TLRPC.MessageEntity messageEntity = entities.get(i);
            if (messageEntity instanceof TLRPC.TL_messageEntityCustomEmoji) {
                TLRPC.TL_messageEntityCustomEmoji entity = (TLRPC.TL_messageEntityCustomEmoji) messageEntity;
                for (int j = 0; j < emojiSpans.length; ++j) {
                    Emoji.EmojiSpan span = emojiSpans[j];
                    if (span != null) {
                        int start = spannable.getSpanStart(span);
                        int end = spannable.getSpanEnd(span);
                        if (AndroidUtilities.intersect1d(entity.offset, entity.offset + entity.length, start, end)) {
                            spannable.removeSpan(span);
                            emojiSpans[j] = null;
                        }
                    }
                }

                if (messageEntity.offset + messageEntity.length <= spannable.length()) {
                    AnimatedEmojiSpan[] animatedSpans = spannable.getSpans(messageEntity.offset, messageEntity.offset + messageEntity.length, AnimatedEmojiSpan.class);
                    if (animatedSpans != null && animatedSpans.length > 0) {
                        for (int j = 0; j < animatedSpans.length; ++j) {
                            spannable.removeSpan(animatedSpans[j]);
                        }
                    }

                    AnimatedEmojiSpan span;
                    if (entity.document != null) {
                        span = new AnimatedEmojiSpan(entity.document, scale, fontMetricsInt);
                    } else {
                        span = new AnimatedEmojiSpan(entity.document_id, scale, fontMetricsInt);
                    }
                    span.top = top;
                    spannable.setSpan(span, messageEntity.offset, messageEntity.offset + messageEntity.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    limitCount--;
                }
            }
        }
        return spannable;
    }

    public static final int ENTITIES_ALL = 0;
    public static final int ENTITIES_ONLY_HASHTAGS = 1;

    public static boolean addEntitiesToText(CharSequence text, ArrayList<TLRPC.MessageEntity> entities, boolean out, boolean usernames, boolean photoViewer, boolean useManualParse) {
        return addEntitiesToText(text, entities, out, usernames, photoViewer, useManualParse, ENTITIES_ALL);
    }

    public static boolean addEntitiesToText(CharSequence text, ArrayList<TLRPC.MessageEntity> entities, boolean out, boolean usernames, boolean photoViewer, boolean useManualParse, int allowed) {
        if (!(text instanceof Spannable)) {
            return false;
        }
        text = FormattedDateSpan.restoreFormatedDateEntities(text);
        Spannable spannable = (Spannable) text;
        URLSpan[] spans = spannable.getSpans(0, text.length(), URLSpan.class);
        boolean hasUrls = spans != null && spans.length > 0;
        if (entities == null || entities.isEmpty()) {
            return hasUrls;
        }

        byte t;
        if (photoViewer) {
            t = 2;
        } else if (out) {
            t = 1;
        } else {
            t = 0;
        }

        ArrayList<TextStyleSpan.TextStyleRun> runs = new ArrayList<>();
        ArrayList<TLRPC.MessageEntity> entitiesCopy = new ArrayList<>(entities);

        Collections.sort(entitiesCopy, (o1, o2) -> {
            if (o1.offset > o2.offset) {
                return 1;
            } else if (o1.offset < o2.offset) {
                return -1;
            }
            return 0;
        });
        for (int a = 0, N = entitiesCopy.size(); a < N; a++) {
            TLRPC.MessageEntity entity = entitiesCopy.get(a);
            if (entity.length <= 0 || entity.offset < 0 || entity.offset >= text.length()) {
                continue;
            } else if (entity.offset + entity.length > text.length()) {
                entity.length = text.length() - entity.offset;
            }

            if (!useManualParse ||
                    entity instanceof TLRPC.TL_messageEntityBold ||
                    entity instanceof TLRPC.TL_messageEntityItalic ||
                    entity instanceof TLRPC.TL_messageEntityStrike ||
                    entity instanceof TLRPC.TL_messageEntityUnderline ||
                    entity instanceof TLRPC.TL_messageEntityBlockquote ||
                    entity instanceof TLRPC.TL_messageEntityFormattedDate ||
                    entity instanceof TLRPC.TL_messageEntityCode ||
                    entity instanceof TLRPC.TL_messageEntityPre ||
                    entity instanceof TLRPC.TL_messageEntityMentionName ||
                    entity instanceof TLRPC.TL_inputMessageEntityMentionName ||
                    entity instanceof TLRPC.TL_messageEntityTextUrl ||
                    entity instanceof TLRPC.TL_messageEntitySpoiler ||
                    entity instanceof TLRPC.TL_messageEntityCustomEmoji ||
                    entity instanceof TLRPC.TL_messageEntityDiffInsert ||
                    entity instanceof TLRPC.TL_messageEntityDiffReplace ||
                    entity instanceof TLRPC.TL_messageEntityDiffDelete) {
                if (spans != null && spans.length > 0) {
                    for (int b = 0; b < spans.length; b++) {
                        if (spans[b] == null) {
                            continue;
                        }
                        int start = spannable.getSpanStart(spans[b]);
                        int end = spannable.getSpanEnd(spans[b]);
                        if (entity.offset <= start && entity.offset + entity.length >= start || entity.offset <= end && entity.offset + entity.length >= end) {
                            spannable.removeSpan(spans[b]);
                            spans[b] = null;
                        }
                    }
                }
            }

            if (allowed == ENTITIES_ONLY_HASHTAGS && !(entity instanceof TLRPC.TL_messageEntityHashtag))
                continue;

            if (
                entity instanceof TLRPC.TL_messageEntityCustomEmoji ||
                entity instanceof TLRPC.TL_messageEntityBlockquote ||
                entity instanceof TLRPC.TL_messageEntityPre ||
                entity instanceof TLRPC.TL_messageEntityDiffReplace
            ) {
                continue;
            }

            TextStyleSpan.TextStyleRun newRun = new TextStyleSpan.TextStyleRun();
            newRun.start = entity.offset;
            newRun.end = newRun.start + entity.length;
            TLRPC.MessageEntity urlEntity = null;
            if (entity instanceof TLRPC.TL_messageEntitySpoiler) {
                newRun.flags = TextStyleSpan.FLAG_STYLE_SPOILER;
            } else if (entity instanceof TLRPC.TL_messageEntityStrike) {
                newRun.flags = TextStyleSpan.FLAG_STYLE_STRIKE;
            } else if (entity instanceof TLRPC.TL_messageEntityDiffDelete) {
                newRun.flags = TextStyleSpan.FLAG_STYLE_STRIKE_RED;
            } else if (entity instanceof TLRPC.TL_messageEntityUnderline) {
                newRun.flags = TextStyleSpan.FLAG_STYLE_UNDERLINE;
            } else if (entity instanceof TLRPC.TL_messageEntityBold) {
                newRun.flags = TextStyleSpan.FLAG_STYLE_BOLD;
            } else if (entity instanceof TLRPC.TL_messageEntityItalic) {
                newRun.flags = TextStyleSpan.FLAG_STYLE_ITALIC;
            } else if (entity instanceof TLRPC.TL_messageEntityCode) {
                newRun.flags = TextStyleSpan.FLAG_STYLE_MONO;
            } else if (entity instanceof TLRPC.TL_messageEntityDiffInsert) {
                newRun.flags = TextStyleSpan.FLAG_STYLE_ACCENT;
            } else if (entity instanceof TLRPC.TL_messageEntityMentionName) {
                if (!usernames) {
                    continue;
                }
                newRun.flags = TextStyleSpan.FLAG_STYLE_MENTION;
                newRun.urlEntity = entity;
            } else if (entity instanceof TLRPC.TL_inputMessageEntityMentionName) {
                if (!usernames) {
                    continue;
                }
                newRun.flags = TextStyleSpan.FLAG_STYLE_MENTION;
                newRun.urlEntity = entity;
            } else {
                if (useManualParse && !(entity instanceof TLRPC.TL_messageEntityTextUrl || entity instanceof TLRPC.TL_messageEntityFormattedDate)) {
                    continue;
                }
                if ((entity instanceof TLRPC.TL_messageEntityUrl || entity instanceof TLRPC.TL_messageEntityTextUrl) && Browser.isPassportUrl(entity.url)) {
                    continue;
                }
                if (entity instanceof TLRPC.TL_messageEntityMention && !usernames) {
                    continue;
                }
                newRun.flags = TextStyleSpan.FLAG_STYLE_URL;
                newRun.urlEntity = entity;

                if (entity instanceof TLRPC.TL_messageEntityTextUrl) {
                    newRun.flags |= TextStyleSpan.FLAG_STYLE_TEXT_URL;
                }
            }

            for (int b = 0, N2 = runs.size(); b < N2; b++) {
                TextStyleSpan.TextStyleRun run = runs.get(b);
                if ((run.flags & TextStyleSpan.FLAG_STYLE_SPOILER) != 0 && newRun.start >= run.start && newRun.end <= run.end) {
                    continue;
                }

                if (newRun.start > run.start) {
                    if (newRun.start >= run.end) {
                        continue;
                    }

                    if (newRun.end < run.end) {
                        TextStyleSpan.TextStyleRun r = new TextStyleSpan.TextStyleRun(newRun);
                        r.merge(run);
                        b++;
                        N2++;
                        runs.add(b, r);

                        r = new TextStyleSpan.TextStyleRun(run);
                        r.start = newRun.end;
                        b++;
                        N2++;
                        runs.add(b, r);
                    } else {
                        TextStyleSpan.TextStyleRun r = new TextStyleSpan.TextStyleRun(newRun);
                        r.merge(run);
                        r.end = run.end;
                        b++;
                        N2++;
                        runs.add(b, r);
                    }

                    int temp = newRun.start;
                    newRun.start = run.end;
                    run.end = temp;
                } else {
                    if (run.start >= newRun.end) {
                        continue;
                    }
                    int temp = run.start;
                    if (newRun.end == run.end) {
                        run.merge(newRun);
                    } else if (newRun.end < run.end) {
                        TextStyleSpan.TextStyleRun r = new TextStyleSpan.TextStyleRun(run);
                        r.merge(newRun);
                        r.end = newRun.end;
                        b++;
                        N2++;
                        runs.add(b, r);

                        run.start = newRun.end;
                    } else {
                        TextStyleSpan.TextStyleRun r = new TextStyleSpan.TextStyleRun(newRun);
                        r.start = run.end;
                        b++;
                        N2++;
                        runs.add(b, r);

                        run.merge(newRun);
                    }
                    newRun.end = temp;
                }
            }
            if (newRun.start < newRun.end) {
                runs.add(newRun);
            }
        }

        final int count = Math.min(MediaDataController.MAX_STYLE_RUNS_COUNT, runs.size());
        int linksCount = 0, spoilersCount = 0, codesCount = 0;
        for (int a = 0; a < count; a++) {
            TextStyleSpan.TextStyleRun run = runs.get(a);

            if (allowed == ENTITIES_ONLY_HASHTAGS && !(run.urlEntity instanceof TLRPC.TL_messageEntityHashtag))
                continue;

            boolean setRun = false;
            String url = run.urlEntity != null ? TextUtils.substring(text, run.urlEntity.offset, run.urlEntity.offset + run.urlEntity.length) : null;
            if (run.urlEntity instanceof TLRPC.TL_messageEntityBotCommand) {
                if (linksCount >= MediaDataController.MAX_LINKS_COUNT) continue;
                linksCount++;
                spannable.setSpan(new URLSpanBotCommand(url, t, run), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (run.urlEntity instanceof TLRPC.TL_messageEntityHashtag || run.urlEntity instanceof TLRPC.TL_messageEntityMention || run.urlEntity instanceof TLRPC.TL_messageEntityCashtag) {
                if (linksCount >= MediaDataController.MAX_LINKS_COUNT) continue;
                linksCount++;
                spannable.setSpan(new URLSpanNoUnderline(url, run), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (run.urlEntity instanceof TLRPC.TL_messageEntityEmail) {
                if (linksCount >= MediaDataController.MAX_LINKS_COUNT) continue;
                linksCount++;
                spannable.setSpan(new URLSpanReplacement("mailto:" + url, run), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (run.urlEntity instanceof TLRPC.TL_messageEntityUrl) {
                if (linksCount >= MediaDataController.MAX_LINKS_COUNT) continue;
                linksCount++;
                hasUrls = true;
                String lowerCase = url.toLowerCase();
                url = !lowerCase.contains("://") ? (BotWebViewContainer.isTonsite(url) ? "tonsite://" : "http://") + url : url;
                if (url != null) {
                    url = url.replaceAll("‚àï|‚ÅÑ|%E2%81%84|%E2%88%95", "/");
                }
                if (Browser.isTonsitePunycode(url)) continue;
                spannable.setSpan(new URLSpanBrowser(url, run), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (run.urlEntity instanceof TLRPC.TL_messageEntityFormattedDate) {
                if (linksCount >= MediaDataController.MAX_LINKS_COUNT) continue;
                linksCount++;
                spannable.setSpan(new FormattedDateSpan(url, run, (TLRPC.TL_messageEntityFormattedDate) run.urlEntity), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (run.urlEntity instanceof TLRPC.TL_messageEntityBankCard) {
                hasUrls = true;
                spannable.setSpan(new URLSpanNoUnderline("card:" + url, run), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (run.urlEntity instanceof TLRPC.TL_messageEntityPhone) {
                hasUrls = true;
                String tel = PhoneFormat.stripExceptNumbers(url);
                if (url.startsWith("+")) {
                    tel = "+" + tel;
                }
                spannable.setSpan(new URLSpanNoUnderline("tel:" + tel, run), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (run.urlEntity instanceof TLRPC.TL_messageEntityTextUrl) {
                if (linksCount >= MediaDataController.MAX_LINKS_COUNT) continue;
                linksCount++;
                url = run.urlEntity.url;
                if (url != null) {
                    url = url.replaceAll("‚àï|‚ÅÑ|%E2%81%84|%E2%88%95", "/");
                }
                if (Browser.isTonsitePunycode(url)) continue;
                spannable.setSpan(new URLSpanReplacement(url, run), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (run.urlEntity instanceof TLRPC.TL_messageEntityMentionName) {
                spannable.setSpan(new URLSpanUserMention("" + ((TLRPC.TL_messageEntityMentionName) run.urlEntity).user_id, t, run), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (run.urlEntity instanceof TLRPC.TL_inputMessageEntityMentionName) {
                spannable.setSpan(new URLSpanUserMention("" + ((TLRPC.TL_inputMessageEntityMentionName) run.urlEntity).user_id.user_id, t, run), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if ((run.flags & TextStyleSpan.FLAG_STYLE_MONO) != 0) {
                spannable.setSpan(new URLSpanMono(spannable, run.start, run.end, t, run), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                setRun = true;
                spannable.setSpan(new TextStyleSpan(run), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (!setRun && (run.flags & TextStyleSpan.FLAG_STYLE_SPOILER) != 0) {
                if (spoilersCount >= SpoilerEffect.MAX_SPOILERS_COUNT) continue;
                spoilersCount++;
                spannable.setSpan(new TextStyleSpan(run), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        for (int a = 0, N = entitiesCopy.size(); a < N; a++) {
            TLRPC.MessageEntity entity = entitiesCopy.get(a);
            if (entity.length <= 0 || entity.offset < 0 || entity.offset >= text.length()) {
                continue;
            } else if (entity.offset + entity.length > text.length()) {
                entity.length = text.length() - entity.offset;
            }

            if (entity instanceof TLRPC.TL_messageEntityBlockquote) {
                QuoteSpan.putQuote(spannable, entity.offset, entity.offset + entity.length, entity.collapsed);
            } else if (entity instanceof TLRPC.TL_messageEntityPre) {
                if (codesCount >= 50) continue;
                codesCount++;
                final int start = entity.offset;
                final int end = entity.offset + entity.length;
                spannable.setSpan(new CodeHighlighting.Span(true, 0, null, entity.language, spannable.subSequence(start, end).toString()), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (entity instanceof TLRPC.TL_messageEntityDiffReplace) {
                final int start = entity.offset;
                final int end = entity.offset + entity.length;
                spannable.setSpan(new SquigglyLinesSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return hasUrls;
    }

    public boolean needDrawShareButton() {
        if (isRepostPreview) {
            return false;
        }
        if (sideMenuEnabled) {
            return false;
        }
        if (getDialogId() == UserObject.VERIFY) {
            return false;
        }
        if (isSaved) {
            long selfId = UserConfig.getInstance(currentAccount).clientUserId;
            long dialogId = MessageObject.getSavedDialogId(selfId, messageOwner);
            if (dialogId == selfId || dialogId == UserObject.ANONYMOUS) {
                return false;
            }
            if (messageOwner == null || messageOwner.fwd_from == null) {
                return false;
            }
            if (messageOwner.fwd_from.from_id == null && messageOwner.fwd_from.saved_from_id == null) {
                return false;
            }
            return true;
        }
        if (type == TYPE_JOINED_CHANNEL) {
            return false;
        } else if (isSponsored()) {
            return false;
        } else if (hasCode) {
            return false;
        } else if (preview) {
            return false;
        } else if (scheduled) {
            return false;
        } else if (eventId != 0) {
            return false;
        } else if (searchType == ChatActivity.SEARCH_PUBLIC_POSTS) {
            return true;
        } else if (messageOwner.noforwards) {
            return false;
        } else if (messageOwner.fwd_from != null && !isOutOwner() && messageOwner.fwd_from.saved_from_peer != null && getDialogId() == UserConfig.getInstance(currentAccount).getClientUserId()) {
            return true;
        } else if (type == TYPE_STICKER || type == TYPE_ANIMATED_STICKER || type == TYPE_EMOJIS) {
            return false;
        } else if (messageOwner.fwd_from != null && messageOwner.fwd_from.from_id instanceof TLRPC.TL_peerChannel && !isOutOwner()) {
            return true;
        } else if (isFromUser()) {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(messageOwner.from_id.user_id);
            if (user != null && user.bot && ("reviews_bot".equals(UserObject.getPublicUsername(user)) || "ReviewInsightsBot".equals(UserObject.getPublicUsername(user)))) {
                return true;
            }
            if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaEmpty || getMedia(messageOwner) == null || getMedia(messageOwner) instanceof TLRPC.TL_messageMediaWebPage && !(getMedia(messageOwner).webpage instanceof TLRPC.TL_webPage)) {
                return false;
            }
            if (user != null && user.bot && !hasExtendedMedia()) {
                return true;
            }
            if (!isOut()) {
                if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaGame || getMedia(messageOwner) instanceof TLRPC.TL_messageMediaInvoice && !hasExtendedMedia() || getMedia(messageOwner) instanceof TLRPC.TL_messageMediaWebPage) {
                    return true;
                }
                TLRPC.Chat chat = messageOwner.peer_id != null && messageOwner.peer_id.channel_id != 0 ? getChat(null, null, messageOwner.peer_id.channel_id) : null;
                if (ChatObject.isChannel(chat) && chat.megagroup) {
                    return ChatObject.isPublic(chat) && !(getMedia(messageOwner) instanceof TLRPC.TL_messageMediaContact) && !(getMedia(messageOwner) instanceof TLRPC.TL_messageMediaGeo);
                }
            }
        } else if (messageOwner.from_id instanceof TLRPC.TL_peerChannel || messageOwner.post) {
            if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaWebPage && !isOutOwner()) {
                return true;
            }
            if (isSupergroup()) {
                return false;
            }
            if (messageOwner.peer_id.channel_id != 0 && (messageOwner.via_bot_id == 0 && messageOwner.reply_to == null || type != TYPE_STICKER && type != TYPE_ANIMATED_STICKER)) {
                return true;
            }
        }
        return false;
    }

    public boolean isYouTubeVideo() {
        return getMedia(messageOwner) instanceof TLRPC.TL_messageMediaWebPage && getMedia(messageOwner).webpage != null && !TextUtils.isEmpty(getMedia(messageOwner).webpage.embed_url) && "YouTube".equals(getMedia(messageOwner).webpage.site_name);
    }

    private Boolean isEmbedVideoCached;
    public boolean isEmbedVideo() {
        if (isEmbedVideoCached != null) return messageOwner != null && messageOwner.media != null && messageOwner.media.webpage != null && isEmbedVideoCached;
        return isEmbedVideoCached = messageOwner != null && messageOwner.media != null && messageOwner.media.webpage != null && !TextUtils.isEmpty(WebPlayerView.getYouTubeVideoId(messageOwner.media.webpage.url));
    }

    private int getParentWidth() {
        if (preview && parentWidth > 0)
            return parentWidth;
        if (AndroidUtilities.isTablet())
            return AndroidUtilities.getMinTabletSide();
        if (AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y)
            return AndroidUtilities.displaySize.x - dp(50);
        return AndroidUtilities.displaySize.x;
    }

    public boolean sideMenuEnabled;
    public int getMaxMessageTextWidth() {
        int maxWidth = 0;
        if (AndroidUtilities.isTablet() && eventId != 0) {
            generatedWithMinSize = dp(530);
        } else {
            generatedWithMinSize = getParentWidth();
        }
        generatedWithDensity = AndroidUtilities.density;
        generatedWithFontSize = Theme.chat_msgTextPaint != null ? Theme.chat_msgTextPaint.getTextSize() : 0;
        if (hasCode && !isSaved) {
            maxWidth = generatedWithMinSize - dp(45 + 15);
            if (sideMenuEnabled) {
                maxWidth -= dp(64);
            } else if (needDrawAvatarInternal() && !isOutOwner() && !messageOwner.isThreadMessage) {
                maxWidth -= dp(52);
            }
        } else if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaWebPage && getMedia(messageOwner).webpage != null && "telegram_background".equals(getMedia(messageOwner).webpage.type)) {
            try {
                Uri uri = Uri.parse(getMedia(messageOwner).webpage.url);
                String segment = uri.getLastPathSegment();
                if (uri.getQueryParameter("bg_color") != null) {
                    maxWidth = dp(220);
                } else if (segment.length() == 6 || segment.length() == 13 && segment.charAt(6) == '-') {
                    maxWidth = dp(200);
                }
            } catch (Exception ignore) {}
        } else if (isAndroidTheme()) {
            maxWidth = dp(200);
        }
        if (maxWidth == 0) {
            final boolean needDrawAvatarInternal = needDrawAvatarInternal();
            maxWidth = generatedWithMinSize - dp(type == TYPE_ARTICLE ? 40 : 80);
            if (sideMenuEnabled) {
                maxWidth -= dp(64);
            } else if (needDrawAvatarInternal && !isOutOwner() && !messageOwner.isThreadMessage) {
                maxWidth -= dp(52);
            }
            if (needDrawShareButton() && (isSaved || !isOutOwner())) {
                maxWidth -= dp(isSaved && isOutOwner() ? 40 : 14);
            }
            if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaGame) {
                maxWidth -= dp(10);
            }
        }
        if (emojiOnlyCount >= 1 && totalAnimatedEmojiCount <= 100 && (emojiOnlyCount - totalAnimatedEmojiCount) < (SharedConfig.getDevicePerformanceClass() >= SharedConfig.PERFORMANCE_CLASS_HIGH ? 100 : 50) && (hasValidReplyMessageObject() || isForwarded())) {
            maxWidth = Math.min(maxWidth, (int) (generatedWithMinSize * .65f));
        }
        return maxWidth;
    }

    public boolean updateSideMenuEnabled(boolean enabled) {
        if (sideMenuEnabled == enabled) return false;
        final boolean wasEnabled = sideMenuEnabled;
//        if (!wasEnabled && enabled || !enabled) {
            sideMenuEnabled = enabled;
            generateLayout(null);
//        }
        return true;
    }

    public void applyTimestampsHighlightForReplyMsg() {
        applyTimestampsHighlightForReplyMsg(messageText);
    }
    public void applyTimestampsHighlightForReplyMsg(CharSequence text) {
        final MessageObject replyMsg = replyMessageObject;
        if (replyMsg == null) return;

        if (replyMsg.isYouTubeVideo()) {
            addUrlsByPattern(isOutOwner(), text, false, 3, Integer.MAX_VALUE, false);
            return;
        }

        if (replyMsg.isVideo()) {
            addUrlsByPattern(isOutOwner(), text, false, 3, (int) replyMsg.getDuration(), false);
            return;
        }

        if (replyMsg.isMusic() || replyMsg.isVoice()) {
            addUrlsByPattern(isOutOwner(), text, false, 4, (int) replyMsg.getDuration(), false);
        }

        if (text == messageText) {
            if (messageOwner != null && (messageOwner.action instanceof TLRPC.TL_messageActionTodoCompletions || messageOwner.action instanceof TLRPC.TL_messageActionTodoAppendTasks)) {
                updateMessageText();
            }
        }
    }

    private boolean applyEntities() {
        generateLinkDescription();
        spoilLoginCode();

        boolean hasEntities;
        if (messageOwner.send_state != MESSAGE_SEND_STATE_SENT) {
            hasEntities = false;
        } else {
            hasEntities = !getEntities().isEmpty();
        }

        boolean useManualParse = !hasEntities && (
            eventId != 0 ||
            messageOwner instanceof TLRPC.TL_message_old ||
            messageOwner instanceof TLRPC.TL_message_old2 ||
            messageOwner instanceof TLRPC.TL_message_old3 ||
            messageOwner instanceof TLRPC.TL_message_old4 ||
            messageOwner instanceof TLRPC.TL_messageForwarded_old ||
            messageOwner instanceof TLRPC.TL_messageForwarded_old2 ||
            messageOwner instanceof TLRPC.TL_message_secret ||
            getMedia(messageOwner) instanceof TLRPC.TL_messageMediaInvoice ||
            isOut() && messageOwner.send_state != MESSAGE_SEND_STATE_SENT ||
            messageOwner.id < 0
        );

        if (useManualParse) {
            addLinks(isOutOwner(), messageText, true, true);
        } else {
            addPhoneLinks(messageText);
        }
        if (isYouTubeVideo()) {
            addUrlsByPattern(isOutOwner(), messageText, false, 3, Integer.MAX_VALUE, false);
        } else {
            applyTimestampsHighlightForReplyMsg();
        }

        if (!(messageText instanceof Spannable)) {
            messageText = new SpannableStringBuilder(messageText);
        }
        return addEntitiesToText(messageText, useManualParse);
    }

    public static StaticLayout makeStaticLayout(CharSequence text_, TextPaint paint, int width, float lineSpacingMult, float lineSpacingAdd, boolean dontIncludePad) {
        return makeStaticLayout(text_, paint, width, lineSpacingMult, lineSpacingAdd, dontIncludePad, Layout.Alignment.ALIGN_NORMAL);
    }

    public static StaticLayout makeStaticLayout(CharSequence text_, TextPaint paint, int width, float lineSpacingMult, float lineSpacingAdd, boolean dontIncludePad, Layout.Alignment alignment) {
        if (width <= 0) width = 1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            final CharSequence text = /* Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ?
                PrecomputedText.create(text_, new PrecomputedText.Params.Builder(paint).build()) :*/ text_;

            StaticLayout.Builder builder =
                    StaticLayout.Builder.obtain(text, 0, text.length(), paint, width)
                            .setLineSpacing(lineSpacingAdd, lineSpacingMult)
                            .setBreakStrategy(StaticLayout.BREAK_STRATEGY_HIGH_QUALITY)
                            .setHyphenationFrequency(StaticLayout.HYPHENATION_FREQUENCY_NONE)
                            .setAlignment(alignment);
            if (dontIncludePad) {
                builder.setIncludePad(false);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                builder.setUseLineSpacingFromFallbacks(false);
            }
            StaticLayout layout = builder.build();

            boolean realWidthLarger = false;
            for (int l = 0; l < layout.getLineCount(); ++l) {
                if (layout.getLineRight(l) > width) {
                    realWidthLarger = true;
                    break;
                }
            }
            if (realWidthLarger) {
                builder = StaticLayout.Builder.obtain(text, 0, text.length(), paint, width)
                                .setLineSpacing(lineSpacingAdd, lineSpacingMult)
                                .setBreakStrategy(StaticLayout.BREAK_STRATEGY_SIMPLE)
                                .setHyphenationFrequency(StaticLayout.HYPHENATION_FREQUENCY_NONE)
                                .setAlignment(alignment);
                if (dontIncludePad) {
                    builder.setIncludePad(false);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    builder.setUseLineSpacingFromFallbacks(false);
                }
                layout = builder.build();
            }

            return layout;
        } else {
            return new StaticLayout(text_, paint, width, alignment, lineSpacingMult, lineSpacingAdd, false);
        }
    }

    public TL_iv.RichMessage getDisplayRichMessage() {
        if (messageOwner == null) {
            return null;
        }
        if (translated && messageOwner.translatedRichMessage != null) {
            return messageOwner.translatedRichMessage;
        }
        return messageOwner.rich_message;
    }

    public void generateLayout(TLRPC.User fromUser) {
        if (type == TYPE_ARTICLE) {
            final int maxWidth = getMaxMessageTextWidth();
            if (richLayout == null || messageOwner != null && richLayout.needsUpdate(getDisplayRichMessage(), maxWidth)) {
                final RichMessageLayout old = richLayout;
                richLayout = new RichMessageLayout(this, maxWidth, old);
                if (old != null && old.view != null) {
                    old.detach(old.view);
                }
            }
            return;
        }
        if (type != TYPE_TEXT && type != TYPE_EMOJIS && type != TYPE_STORY_MENTION || messageOwner.peer_id == null || TextUtils.isEmpty(messageText) && !isBotPendingDraft) {
            return;
        }
        boolean hasUrls = applyEntities();
        boolean noforwards = messageOwner != null && messageOwner.noforwards;
        if (!noforwards) {
            final long dialogId = getDialogId();
            noforwards = MessagesController.getInstance(currentAccount).isPeerNoForwards(dialogId);
        }

        textLayoutBlocks = new ArrayList<>();
        textWidth = 0;

        hasCode = messageText instanceof Spanned && ((Spanned) messageText).getSpans(0, messageText.length(), CodeHighlighting.Span.class).length > 0;
        hasQuote = messageText instanceof Spanned && ((Spanned) messageText).getSpans(0, messageText.length(), QuoteSpan.QuoteStyleSpan.class).length > 0;
        hasSingleQuote = false;
        hasSingleCode = false;
        messageText = FormattedDateSpan.applyFormatedDateEntities(messageText);

        if (messageText instanceof Spanned) {
            Spanned spanned = (Spanned) messageText;
            QuoteSpan[] quoteSpans = spanned.getSpans(0, spanned.length(), QuoteSpan.class);
            for (int i = 0; i < quoteSpans.length; ++i) {
                quoteSpans[i].adaptLineHeight = false;
            }
            hasSingleQuote = quoteSpans.length == 1 && spanned.getSpanStart(quoteSpans[0]) == 0 && spanned.getSpanEnd(quoteSpans[0]) == spanned.length();

            CodeHighlighting.Span[] codeSpans = spanned.getSpans(0, spanned.length(), CodeHighlighting.Span.class);
            hasSingleCode = codeSpans.length == 1 && spanned.getSpanStart(codeSpans[0]) == 0 && spanned.getSpanEnd(codeSpans[0]) == spanned.length();
        }

        if (isBotPendingDraft) {
            messageText = BotForumHelper.applyTypingAnimationSpan(messageText);
        }

        int maxWidth = getMaxMessageTextWidth();

        if (hasSingleQuote) {
            maxWidth -= AndroidUtilities.dp(32);
        } else if (hasSingleCode) {
            maxWidth -= AndroidUtilities.dp(15);
        }

        int textLayoutOriginalWidth;
        StaticLayout textLayout;

        TextPaint paint;
        if (getMedia(messageOwner) instanceof TLRPC.TL_messageMediaGame) {
            paint = Theme.chat_msgGameTextPaint;
        } else {
            paint = Theme.chat_msgTextPaint;
        }

        CharSequence text = messageText;
        try {
            textLayoutOriginalWidth = maxWidth;
            textLayout = makeStaticLayout(text, paint, maxWidth, 1f, totalAnimatedEmojiCount >= 4 ? -1 : 0, emojiOnlyCount > 0);
        } catch (Exception e) {
            FileLog.e(e);
            return;
        }

        if (isRepostPreview) {
            int maxLines = 22;
            if (type != MessageObject.TYPE_TEXT) {
                maxLines = hasValidGroupId() ? 7 : 12;
            }
            if (isWebpage()) {
                maxLines -= 8;
            }
            if (textLayout.getLineCount() > maxLines) {
                String readMore = getString(R.string.ReadMore);
                int readMoreWidth = (int) Math.ceil(paint.measureText("‚Ä¶ " + readMore) + AndroidUtilities.dp(1));

                float maxRight = 0;
                for (int i = 0; i < maxLines; ++i) {
                    maxRight = Math.max(maxRight, textLayout.getLineRight(i));
                }

                int start = textLayout.getLineStart(maxLines - 1);
                int end = textLayout.getLineEnd(maxLines - 1) - 1;
                int offset = end;
                for (; offset >= start; --offset) {
                    if (textLayout.getPrimaryHorizontal(offset) < maxRight - readMoreWidth) {
                        break;
                    }
                }
                for (; offset >= start; --offset) {
                    if (Character.isWhitespace(text.charAt(offset))) {
                        break;
                    }
                }
                text = new SpannableStringBuilder(text.subSequence(0, offset)).append("‚Ä¶ ").append(readMore);
                ((SpannableStringBuilder) text).setSpan(new CharacterStyle() {
                    @Override
                    public void updateDrawState(TextPaint tp) {
                        tp.setColor(Theme.chat_msgTextPaint.linkColor);
                    }
                }, text.length() - readMore.length(), text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                try {
                    textLayoutOriginalWidth = maxWidth;
                    textLayout = makeStaticLayout(text, paint, maxWidth, 1f, totalAnimatedEmojiCount >= 4 ? -1 : 0, emojiOnlyCount > 0);
                } catch (Exception e) {
                    FileLog.e(e);
                    return;
                }
            }
        }

        if (hasSingleQuote) {
            maxWidth += AndroidUtilities.dp(32);
        } else if (hasSingleCode) {
            maxWidth += AndroidUtilities.dp(15);
        }

//        textHeight = 0;
        int linesCount = textLayout.getLineCount();
        int linesPreBlock = totalAnimatedEmojiCount >= 50 ? LINES_PER_BLOCK_WITH_EMOJI : LINES_PER_BLOCK;

        int blocksCount;
        boolean singleLayout = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && totalAnimatedEmojiCount < 50;
        if (singleLayout) {
            blocksCount = 1;
        } else {
            blocksCount = (int) Math.ceil((float) linesCount / linesPreBlock);
        }
        int linesOffset = 0;
        float prevOffset = 0;

        ArrayList<TextRange> textRanges = new ArrayList<>();
        if (text instanceof Spanned && (hasQuote || hasCode)) {
            singleLayout = false;
            cutIntoRanges(text, textRanges);
        } else if (singleLayout || blocksCount == 1) {
            textRanges.add(new TextRange(0, textLayout.getText().length()));
        } else {
            for (int a = 0; a < blocksCount; a++) {
                int currentBlockLinesCount;
                if (singleLayout) {
                    currentBlockLinesCount = linesCount;
                } else {
                    currentBlockLinesCount = Math.min(linesPreBlock, linesCount - linesOffset);
                }

                int startCharacter = textLayout.getLineStart(linesOffset);
                int endCharacter = textLayout.getLineEnd(linesOffset + currentBlockLinesCount - 1);
                if (endCharacter - 1 >= 0 && endCharacter - 1 < textLayout.getText().length()) {
                    if (textLayout.getText().charAt(endCharacter - 1) == '\n')
                        endCharacter--;
                }
                if (endCharacter < startCharacter) {
                    continue;
                }

                textRanges.add(new TextRange(startCharacter, endCharacter));

                linesOffset += currentBlockLinesCount;
            }
        }
        blocksCount = textRanges.size();

        hasCodeAtTop = false;
        hasCodeAtBottom = false;
        hasQuoteAtBottom = false;
        hasSingleQuote = false;
        hasSingleCode = false;
        for (int a = 0; a < textRanges.size(); a++) {
            TextLayoutBlock block = new TextLayoutBlock();

            TextRange range = textRanges.get(a);

            block.code = range.code;
            block.quote = range.quote;
            block.quoteCollapse = range.collapse;
            if (block.quoteCollapse) {
                block.messageObject = this;
            }

            block.index = a;
            block.start = range.start;
            block.first = a == 0;
            block.last = a == textRanges.size() - 1;

            if (block.first) {
                hasCodeAtTop = block.code;
            }
            if (block.last) {
                hasQuoteAtBottom = block.quote;
                hasCodeAtBottom = block.code;
            }
            hasSingleQuote = block.first && block.last && block.quote;
            hasSingleCode = block.first && block.last && !block.quote && block.code;

            if (block.quote) {
                if (block.first && block.last) {
                    block.padTop = block.padBottom = dp(6);
                } else {
                    block.padTop = dp(block.first ? 8 : 6);
                    block.padBottom = dp(7);
                }
            } else if (block.code) {
                block.layoutCode(range.language, range.end - range.start, noforwards);
                block.padTop = dp(4) + block.languageHeight + (block.first ? 0 : dp(5));
                block.padBottom = dp(4) + (block.last ? 0 : dp(7)) + (block.hasCodeCopyButton ? dp(38) : 0);
            }

            TextPaint layoutPaint = paint;
            if (block.code) {
                final int length = range.end - range.start;
                if (length > 220) {
                    layoutPaint = Theme.chat_msgTextCode3Paint;
                } else if (length > 80) {
                    layoutPaint = Theme.chat_msgTextCode2Paint;
                } else {
                    layoutPaint = Theme.chat_msgTextCodePaint;
                }
            }

            CharSequence blockText = text.subSequence(range.start, range.end);
            int blockMaxWidth = maxWidth;
            if (block.quote) {
                blockMaxWidth -= dp(24);
            } else if (block.code) {
                blockMaxWidth -= dp(15);
            }
            if (blocksCount == 1) {
                if (block.code && !block.quote && textLayout.getText() instanceof Spannable) {
                    SpannableString sb;
                    if (!TextUtils.isEmpty(range.language)) {
                        sb = CodeHighlighting.getHighlighted(blockText.toString(), range.language);
                    } else {
                        sb = new SpannableString(blockText.toString());
                    }
                    block.originalWidth = textLayoutOriginalWidth = blockMaxWidth;
                    textLayout = makeStaticLayout(sb, layoutPaint, blockMaxWidth, 1f, totalAnimatedEmojiCount >= 4 ? -1 : 0, emojiOnlyCount > 0);
                } else {
                    block.originalWidth = textLayoutOriginalWidth;
                }

                block.textLayout = textLayout;
                block.charactersOffset = 0;
                block.charactersEnd = textLayout.getText().length();

                block.height = textLayout.getHeight();
                block.collapsedHeight = (int) Math.min(paint.getTextSize() * 1.4f * 3, block.height);
                if (emojiOnlyCount != 0) {
                    switch (emojiOnlyCount) {
                        case 1:
                            block.padTop -= dp(5.3f);
                            break;
                        case 2:
                            block.padTop -= dp(4.5f);
                            break;
                        case 3:
                            block.padTop -= dp(4.2f);
                            break;
                    }
                }
            } else {
                int startCharacter = range.start;
                int endCharacter = range.end;
                if (endCharacter < startCharacter) {
                    continue;
                }
                block.charactersOffset = startCharacter;
                block.charactersEnd = endCharacter;
                try {
                    SpannableString sb;
                    if (block.code && !block.quote) {
                        sb = CodeHighlighting.getHighlighted(blockText.toString(), range.language);
                    } else {
                        sb = SpannableString.valueOf(blockText);
                    }
                    block.originalWidth = blockMaxWidth;
                    block.textLayout = makeStaticLayout(sb, layoutPaint, blockMaxWidth, 1f, totalAnimatedEmojiCount >= 4 ? -1 : 0, false);

                    block.height = block.textLayout.getHeight();//Math.max(block.height, block.textLayout.getLineBottom(block.textLayout.getLineCount() - 1));
                    block.collapsedHeight = (int) Math.min(paint.getTextSize() * 1.4f * 3, block.height);
                } catch (Exception e) {
                    FileLog.e(e);
                    continue;
                }
            }

            textLayoutBlocks.add(block);

            final int currentBlockLinesCount = block.textLayout.getLineCount();

            float lastLeft;
            try {
                lastLeft = block.textLayout.getLineLeft(currentBlockLinesCount - 1);
                if (a == 0 && lastLeft >= 0) {
                    textXOffset = lastLeft;
                }
            } catch (Exception e) {
                lastLeft = 0;
                if (a == 0) {
                    textXOffset = 0;
                }
                FileLog.e(e);
            }

            float lastLine;
            try {
                lastLine = block.textLayout.getLineWidth(currentBlockLinesCount - 1);
            } catch (Exception e) {
                lastLine = 0;
                FileLog.e(e);
            }
            if (block.quote) {
                lastLine += AndroidUtilities.dp(32);
            } else if (block.code) {
                lastLine += AndroidUtilities.dp(15);
            }

            int linesMaxWidth = (int) Math.ceil(lastLine);
            if (linesMaxWidth > maxWidth + 80) {
                linesMaxWidth = maxWidth;
            }
            int lastLineWidthWithLeft;
            int linesMaxWidthWithLeft;

            if (a == blocksCount - 1) {
                lastLineWidth = linesMaxWidth;
            }

            linesMaxWidthWithLeft = lastLineWidthWithLeft = (int) Math.ceil(linesMaxWidth + Math.max(0, lastLeft));

            if (block.quote) {
                block.maxRight = 0;
                for (int n = 0; n < currentBlockLinesCount; n++) {
                    try {
                        block.maxRight = Math.max(block.maxRight, block.textLayout.getLineRight(n));
                    } catch (Exception ignore) {
                        block.maxRight = textWidth;
                    }
                }
            }

            if (currentBlockLinesCount > 1) {
                boolean hasNonRTL = false;
                float textRealMaxWidth = 0, textRealMaxWidthWithLeft = 0, lineWidth, lineLeft;
                for (int n = 0; n < currentBlockLinesCount; n++) {
                    try {
                        lineWidth = block.textLayout.getLineWidth(n);
                    } catch (Exception ignore) {
                        lineWidth = 0;
                    }

                    if (block.quote) {
                        lineWidth += AndroidUtilities.dp(32);
                    } else if (block.code) {
                        lineWidth += AndroidUtilities.dp(15);
                    }

                    try {
                        lineLeft = block.textLayout.getLineLeft(n);
                    } catch (Exception ignore) {
                        lineLeft = 0;
                    }

                    if (lineWidth > maxWidth + 20) {
                        lineWidth = maxWidth;
                        lineLeft = 0;
                    }

                    if (lineLeft > 0 || block.textLayout.getParagraphDirection(n) == Layout.DIR_RIGHT_TO_LEFT) {
                        textXOffset = Math.min(textXOffset, lineLeft);
                        block.directionFlags |= TextLayoutBlock.FLAG_RTL;
                        hasRtl = true;
                    } else {
                        block.directionFlags |= TextLayoutBlock.FLAG_NOT_RTL;
                    }

                    try {
                        if (!hasNonRTL && lineLeft == 0 && block.textLayout.getParagraphDirection(n) == Layout.DIR_LEFT_TO_RIGHT) {
                            hasNonRTL = true;
                        }
                    } catch (Exception ignore) {
                        hasNonRTL = true;
                    }

                    textRealMaxWidth = Math.max(textRealMaxWidth, lineWidth);
                    textRealMaxWidthWithLeft = Math.max(textRealMaxWidthWithLeft, lineWidth + lineLeft);
                    linesMaxWidth = Math.max(linesMaxWidth, (int) Math.ceil(lineWidth));
                    linesMaxWidthWithLeft = Math.max(linesMaxWidthWithLeft, (int) Math.ceil(lineWidth + lineLeft));
                }
                if (hasNonRTL) {
                    textRealMaxWidth = textRealMaxWidthWithLeft;
                    if (a == blocksCount - 1) {
                        lastLineWidth = lastLineWidthWithLeft;
                    }
                } else if (a == blocksCount - 1) {
                    lastLineWidth = linesMaxWidth;
                }
                textWidth = Math.max(textWidth, (int) Math.ceil(textRealMaxWidth));
            } else {
                if (lastLeft > 0) {
                    textXOffset = Math.min(textXOffset, lastLeft);
                    if (textXOffset == 0) {
                        linesMaxWidth += lastLeft;
                    }
                    hasRtl = blocksCount != 1;
                    block.directionFlags |= TextLayoutBlock.FLAG_RTL;
                } else {
                    block.directionFlags |= TextLayoutBlock.FLAG_NOT_RTL;
                }

                textWidth = Math.max(textWidth, Math.min(maxWidth, linesMaxWidth));
            }
            if (block.languageLayout != null) {
                textWidth = (int) Math.max(textWidth, Math.min(block.languageLayout.getCurrentWidth() + dp(15), block.textLayout == null ? 0 : block.textLayout.getWidth()));
            }

            linesOffset += currentBlockLinesCount;

            block.spoilers.clear();
            if (!isSpoilersRevealed && !spoiledLoginCode) {
                int right = linesMaxWidthWithLeft;
                if (block.quote) {
                    right -= AndroidUtilities.dp(32);
                } else if (block.code) {
                    right -= AndroidUtilities.dp(15);
                }
                SpoilerEffect.addSpoilers(null, block.textLayout, -1, right, null, block.spoilers);
            }
        }

        hasWideCode = hasCode && textWidth > generatedWithMinSize - dp(80 + (needDrawAvatarInternal() && !isOutOwner() && !messageOwner.isThreadMessage ? 52 : 0));
        factCheckText = null;
    }

    private Integer cachedTextHeight;
    public int textHeightCached() {
        if (richLayout != null) return richLayout.getHeight();
        if (cachedTextHeight != null) return cachedTextHeight;
        if (textLayoutBlocks == null) return cachedTextHeight = 0;
        int h = 0;
        for (int i = 0; i < textLayoutBlocks.size(); ++i) {
            h += textLayoutBlocks.get(i).padTop + textLayoutBlocks.get(i).heightCollapsed() + textLayoutBlocks.get(i).padBottom;
        }
        return cachedTextHeight = h;
    }
    public int textHeight() {
        if (richLayout != null) return richLayout.getHeight();
        if (textLayoutBlocks == null) return 0;
        int h = 0;
        for (int i = 0; i < textLayoutBlocks.size(); ++i) {
            h += textLayoutBlocks.get(i).padTop + textLayoutBlocks.get(i).height() + textLayoutBlocks.get(i).padBottom;
        }
        return h;
    }

    public int textHeight(ChatMessageCell.TransitionParams tp) {
        if (textLayoutBlocks == null) return 0;
        int h = 0;
        for (int i = 0; i < textLayoutBlocks.size(); ++i) {
            h += textLayoutBlocks.get(i).padTop + textLayoutBlocks.get(i).height(tp) + textLayoutBlocks.get(i).padBottom;
        }
        return h;
    }

    public static class TextLayoutBlocks {

        public final CharSequence text;
        public int lastLineWidth;
        public int textWidth;
        public boolean hasRtl;
        public float textXOffset;
        public final ArrayList<TextLayoutBlock> textLayoutBlocks = new ArrayList<>();
        public boolean hasCode, hasCodeAtTop, hasCodeAtBottom, hasSingleCode;
        public boolean hasQuote, hasQuoteAtBottom, hasSingleQuote;

        public int textHeight() {
            int h = 0;
            for (int i = 0; i < textLayoutBlocks.size(); ++i) {
                h += textLayoutBlocks.get(i).padTop + textLayoutBlocks.get(i).height() + textLayoutBlocks.get(i).padBottom;
            }
            return h;
        }

        public int textHeight(ChatMessageCell.TransitionParams tp) {
            int h = 0;
            for (int i = 0; i < textLayoutBlocks.size(); ++i) {
                h += textLayoutBlocks.get(i).padTop + textLayoutBlocks.get(i).height(tp) + textLayoutBlocks.get(i).padBottom;
            }
            return h;
        }

        public void bounceFrom(TextLayoutBlocks from) {
            if (from == null) return;
            for (int i = 0; i < Math.min(textLayoutBlocks.size(), from.textLayoutBlocks.size()); ++i) {
                textLayoutBlocks.get(i).collapsedBounce = from.textLayoutBlocks.get(i).collapsedBounce;
            }
        }

        public TextLayoutBlocks(MessageObject messageObject, @NonNull CharSequence text, TextPaint textPaint, int width) {
            this.text = text;
            textWidth = 0;
            boolean noforwards = messageObject != null && messageObject.messageOwner != null && messageObject.messageOwner.noforwards;
            if (messageObject != null && !noforwards) {
                final long dialogId = messageObject.getDialogId();
                noforwards = MessagesController.getInstance(messageObject.currentAccount).isPeerNoForwards(dialogId);
            }

            hasCode = text instanceof Spanned && ((Spanned) text).getSpans(0, text.length(), CodeHighlighting.Span.class).length > 0;
            hasQuote = text instanceof Spanned && ((Spanned) text).getSpans(0, text.length(), QuoteSpan.QuoteStyleSpan.class).length > 0;
            hasSingleQuote = false;
            hasSingleCode = false;

            if (text instanceof Spanned) {
                Spanned spanned = (Spanned) text;
                QuoteSpan[] quoteSpans = spanned.getSpans(0, spanned.length(), QuoteSpan.class);
                for (int i = 0; i < quoteSpans.length; ++i) {
                    quoteSpans[i].adaptLineHeight = false;
                }
                hasSingleQuote = quoteSpans.length == 1 && spanned.getSpanStart(quoteSpans[0]) == 0 && spanned.getSpanEnd(quoteSpans[0]) == spanned.length();

                CodeHighlighting.Span[] codeSpans = spanned.getSpans(0, spanned.length(), CodeHighlighting.Span.class);
                hasSingleCode = codeSpans.length == 1 && spanned.getSpanStart(codeSpans[0]) == 0 && spanned.getSpanEnd(codeSpans[0]) == spanned.length();
            }

            StaticLayout textLayout;

            if (hasSingleQuote) {
                width -= AndroidUtilities.dp(32);
            } else if (hasSingleCode) {
                width -= AndroidUtilities.dp(15);
            }

            int textLayoutOriginalWidth;
            try {
                textLayout = makeStaticLayout(text, textPaint, textLayoutOriginalWidth = width, 1f, 0f, false);
            } catch (Exception e) {
                FileLog.e(e);
                return;
            }
            if (messageObject != null && messageObject.isRepostPreview) {
                int maxLines = 22;
                if (messageObject.type != MessageObject.TYPE_TEXT) {
                    maxLines = messageObject.hasValidGroupId() ? 7 : 12;
                }
                if (messageObject.isWebpage()) {
                    maxLines -= 8;
                }
                if (textLayout.getLineCount() > maxLines) {
                    String readMore = getString(R.string.ReadMore);
                    int readMoreWidth = (int) Math.ceil(textPaint.measureText("‚Ä¶ " + readMore) + AndroidUtilities.dp(1));

                    float maxRight = 0;
                    for (int i = 0; i < maxLines; ++i) {
                        maxRight = Math.max(maxRight, textLayout.getLineRight(i));
                    }

                    int start = textLayout.getLineStart(maxLines - 1);
                    int end = textLayout.getLineEnd(maxLines - 1) - 1;
                    int offset = end;
                    for (; offset >= start; --offset) {
                        if (textLayout.getPrimaryHorizontal(offset) < maxRight - readMoreWidth) {
                            break;
                        }
                    }
                    for (; offset >= start; --offset) {
                        if (Character.isWhitespace(text.charAt(offset))) {
                            break;
                        }
                    }
                    text = new SpannableStringBuilder(text.subSequence(0, offset)).append("‚Ä¶ ").append(readMore);
                    ((SpannableStringBuilder) text).setSpan(new CharacterStyle() {
                        @Override
                        public void updateDrawState(TextPaint tp) {
                            tp.setColor(Theme.chat_msgTextPaint.linkColor);
                        }
                    }, text.length() - readMore.length(), text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    try {
                        textLayoutOriginalWidth = width;
                        textLayout = makeStaticLayout(text, textPaint, width, 1f, 0f, false);
                    } catch (Exception e) {
                        FileLog.e(e);
                        return;
                    }
                }
            }

            if (hasSingleQuote) {
                width += AndroidUtilities.dp(32);
            } else if (hasSingleCode) {
                width += AndroidUtilities.dp(15);
            }

            int linesCount = textLayout.getLineCount();
            int linesPreBlock = LINES_PER_BLOCK;

            int blocksCount;
            boolean singleLayout = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
            if (singleLayout) {
                blocksCount = 1;
            } else {
                blocksCount = (int) Math.ceil((float) linesCount / linesPreBlock);
            }
            int linesOffset = 0;
            float prevOffset = 0;

            ArrayList<TextRange> textRanges = new ArrayList<>();
            if (text instanceof Spanned && (hasQuote || hasCode)) {
                singleLayout = false;
                cutIntoRanges(text, textRanges);
            } else if (singleLayout || blocksCount == 1) {
                textRanges.add(new TextRange(0, textLayout.getText().length()));
            } else {
                for (int a = 0; a < blocksCount; a++) {
                    int currentBlockLinesCount = Math.min(linesPreBlock, linesCount - linesOffset);

                    int startCharacter = textLayout.getLineStart(linesOffset);
                    int endCharacter = textLayout.getLineEnd(linesOffset + currentBlockLinesCount - 1);
                    if (endCharacter < startCharacter) {
                        continue;
                    }

                    textRanges.add(new TextRange(startCharacter, endCharacter));

                    linesOffset += currentBlockLinesCount;
                }
            }
            blocksCount = textRanges.size();

            hasCodeAtTop = false;
            hasCodeAtBottom = false;
            hasQuoteAtBottom = false;
            hasSingleQuote = false;
            for (int a = 0; a < textRanges.size(); a++) {
                TextLayoutBlock block = new TextLayoutBlock();

                TextRange range = textRanges.get(a);

                block.code = range.code;
                block.quote = range.quote;
                block.quoteCollapse = range.collapse;
                if (block.quoteCollapse) {
                    block.messageObject = messageObject;
                }

                block.index = a;
                block.start = range.start;
                block.first = a == 0;
                block.last = a == textRanges.size() - 1;

                if (block.first) {
                    hasCodeAtTop = block.code;
                }
                if (block.last) {
                    hasQuoteAtBottom = block.quote;
                    hasCodeAtBottom = block.code;
                }
                hasSingleQuote = block.first && block.last && block.quote;

                if (block.quote) {
                    if (block.first && block.last) {
                        block.padTop = block.padBottom = dp(6);
                    } else {
                        block.padTop = dp(block.first ? 8 : 6);
                        block.padBottom = dp(7);
                    }
                } else if (block.code) {
                    block.layoutCode(range.language, range.end - range.start, noforwards);
                    block.padTop = dp(4) + block.languageHeight + (block.first ? 0 : dp(5));
                    block.padBottom = dp(4) + (block.last ? 0 : dp(7)) + (block.hasCodeCopyButton ? dp(38) : 0);
                }

                TextPaint layoutPaint = textPaint;
                if (block.code) {
                    final int length = range.end - range.start;
                    if (length > 220) {
                        layoutPaint = Theme.chat_msgTextCode3Paint;
                    } else if (length > 80) {
                        layoutPaint = Theme.chat_msgTextCode2Paint;
                    } else {
                        layoutPaint = Theme.chat_msgTextCodePaint;
                    }
                }

                int blockMaxWidth = width;
                if (block.quote) {
                    blockMaxWidth -= dp(32);
                } else if (block.code) {
                    blockMaxWidth -= dp(15);
                }
                if (blocksCount == 1) {
                    if (block.code && !block.quote && textLayout.getText() instanceof Spannable) {
                        SpannableString sb;
                        if (!TextUtils.isEmpty(range.language)) {
                            sb = CodeHighlighting.getHighlighted(text.subSequence(range.start, range.end).toString(), range.language);
                        } else {
                            sb = new SpannableString(text.subSequence(range.start, range.end));
                        }
                        block.originalWidth = textLayoutOriginalWidth = blockMaxWidth;
                        textLayout = makeStaticLayout(sb, layoutPaint, blockMaxWidth, 1f, 0f, false);
                    } else {
                        block.originalWidth = textLayoutOriginalWidth;
                    }

                    block.textLayout = textLayout;
                    block.charactersOffset = 0;
                    block.charactersEnd = textLayout.getText().length();

                    block.height = textLayout.getHeight();
                    block.collapsedHeight = (int) Math.min(textPaint.getTextSize() * 1.4f * 3, block.height);
                } else {
                    int startCharacter = range.start;
                    int endCharacter = range.end;
                    if (endCharacter < startCharacter) {
                        continue;
                    }
                    block.charactersOffset = startCharacter;
                    block.charactersEnd = endCharacter;
                    try {
                        SpannableString sb;
                        if (block.code && !block.quote) {
                            sb = CodeHighlighting.getHighlighted(text.subSequence(startCharacter, endCharacter).toString(), range.language);
                        } else {
                            sb = SpannableString.valueOf(text.subSequence(startCharacter, endCharacter));
                        }
                        block.originalWidth = blockMaxWidth;
                        block.textLayout = makeStaticLayout(sb, layoutPaint, blockMaxWidth, 1f, 0f, false);

                        block.height = block.textLayout.getHeight();
                        block.collapsedHeight = (int) Math.min(textPaint.getTextSize() * 1.4f * 3, block.height);
                    } catch (Exception e) {
                        FileLog.e(e);
                        continue;
                    }
                }

                if (block.code && block.textLayout.getText() instanceof Spannable && TextUtils.isEmpty(range.language)) {
                    CodeHighlighting.highlight((Spannable) block.textLayout.getText(), 0, block.textLayout.getText().length(), range.language, 0, null, true);
                }

                textLayoutBlocks.add(block);

                final int currentBlockLinesCount = block.textLayout.getLineCount();

                float lastLeft;
                try {
                    lastLeft = block.textLayout.getLineLeft(currentBlockLinesCount - 1);
                    if (a == 0 && lastLeft >= 0) {
                        textXOffset = lastLeft;
                    }
                } catch (Exception e) {
                    lastLeft = 0;
                    if (a == 0) {
                        textXOffset = 0;
                    }
                    FileLog.e(e);
                }

                float lastLine;
                try {
                    lastLine = block.textLayout.getLineWidth(currentBlockLinesCount - 1);
                } catch (Exception e) {
                    lastLine = 0;
                    FileLog.e(e);
                }

                int linesMaxWidth = (int) Math.ceil(lastLine);
                if (linesMaxWidth > width + 80) {
                    linesMaxWidth = width;
                }
                int lastLineWidthWithLeft;
                int linesMaxWidthWithLeft;

                if (a == blocksCount - 1) {
                    lastLineWidth = linesMaxWidth;
                }

                linesMaxWidthWithLeft = lastLineWidthWithLeft = (int) Math.ceil(linesMaxWidth + Math.max(0, lastLeft));

                if (block.quote) {
                    block.maxRight = 0;
                    for (int n = 0; n < currentBlockLinesCount; n++) {
                        try {
                            block.maxRight = Math.max(block.maxRight, block.textLayout.getLineRight(n));
                        } catch (Exception ignore) {
                            block.maxRight = textWidth;
                        }
                    }
                }

                if (currentBlockLinesCount > 1) {
                    boolean hasNonRTL = false;
                    float textRealMaxWidth = 0, textRealMaxWidthWithLeft = 0, lineWidth, lineLeft;
                    for (int n = 0; n < currentBlockLinesCount; n++) {
                        try {
                            lineWidth = block.textLayout.getLineWidth(n);
                        } catch (Exception ignore) {
                            lineWidth = 0;
                        }

                        if (block.quote) {
                            lineWidth += AndroidUtilities.dp(32);
                        } else if (block.code) {
                            lineWidth += AndroidUtilities.dp(15);
                        }

                        try {
                            lineLeft = block.textLayout.getLineLeft(n);
                        } catch (Exception ignore) {
                            lineLeft = 0;
                        }

                        if (lineWidth > width + 20) {
                            lineWidth = width;
                            lineLeft = 0;
                        }

                        if (lineLeft > 0 || block.textLayout.getParagraphDirection(n) == Layout.DIR_RIGHT_TO_LEFT) {
                            textXOffset = Math.min(textXOffset, lineLeft);
                            block.directionFlags |= TextLayoutBlock.FLAG_RTL;
                            hasRtl = true;
                        } else {
                            block.directionFlags |= TextLayoutBlock.FLAG_NOT_RTL;
                        }

                        try {
                            if (!hasNonRTL && lineLeft == 0 && block.textLayout.getParagraphDirection(n) == Layout.DIR_LEFT_TO_RIGHT) {
                                hasNonRTL = true;
                            }
                        } catch (Exception ignore) {
                            hasNonRTL = true;
                        }



                        textRealMaxWidth = Math.max(textRealMaxWidth, lineWidth);
                        textRealMaxWidthWithLeft = Math.max(textRealMaxWidthWithLeft, lineWidth + lineLeft);
                        linesMaxWidth = Math.max(linesMaxWidth, (int) Math.ceil(lineWidth));
                        linesMaxWidthWithLeft = Math.max(linesMaxWidthWithLeft, (int) Math.ceil(lineWidth + lineLeft));
                    }
                    if (hasNonRTL) {
                        textRealMaxWidth = textRealMaxWidthWithLeft;
                        if (a == blocksCount - 1) {
                            lastLineWidth = lastLineWidthWithLeft;
                        }
                    } else if (a == blocksCount - 1) {
                        lastLineWidth = linesMaxWidth;
                    }
                    textWidth = Math.max(textWidth, (int) Math.ceil(textRealMaxWidth));
                } else {
                    if (lastLeft > 0) {
                        textXOffset = Math.min(textXOffset, lastLeft);
                        if (textXOffset == 0) {
                            linesMaxWidth += lastLeft;
                        }
                        hasRtl = blocksCount != 1;
                        block.directionFlags |= TextLayoutBlock.FLAG_RTL;
                    } else {
                        block.directionFlags |= TextLayoutBlock.FLAG_NOT_RTL;
                    }

                    textWidth = Math.max(textWidth, Math.min(width, linesMaxWidth));
                }
                if (block.languageLayout != null) {
                    textWidth = (int) Math.max(textWidth, Math.min(block.languageLayout.getCurrentWidth() + dp(15), block.textLayout == null ? 0 : block.textLayout.getWidth()));
                }

                linesOffset += currentBlockLinesCount;
                if (messageObject != null && !messageObject.isSpoilersRevealed && !messageObject.spoiledLoginCode) {
                    int right = linesMaxWidthWithLeft;
                    if (block.quote) {
                        right -= AndroidUtilities.dp(32);
                    } else if (block.code) {
                        right -= AndroidUtilities.dp(15);
                    }
                    SpoilerEffect.addSpoilers(null, block.textLayout, -1, right, null, block.spoilers);
                }
            }
        }

    }

    public boolean isOut() {
        return messageOwner.out;
    }

    public Boolean isOutOwnerCached;
    public boolean isOutOwner() {
        if (previewForward) {
            return true;
        }
        if (isOutOwnerCached != null) {
            return isOutOwnerCached;
        }

        if (isWelcomeMessage() && !isWelcomeAnchored()) {
            return isOutOwnerCached = false;
        }

        long selfUserId = UserConfig.getInstance(currentAccount).getClientUserId();
        if (isSaved || getDialogId() == selfUserId) {
            if (messageOwner.fwd_from != null) {
                return isOutOwnerCached = messageOwner.fwd_from.from_id != null && messageOwner.fwd_from.from_id.user_id == selfUserId || messageOwner.fwd_from.saved_out;
            } else {
                return isOutOwnerCached = true;
            }
        }
        TLRPC.Chat chat = messageOwner.peer_id != null && messageOwner.peer_id.channel_id != 0 ? getChat(null, null, messageOwner.peer_id.channel_id) : null;
        if (!messageOwner.out || !(messageOwner.from_id instanceof TLRPC.TL_peerUser) && (!(messageOwner.from_id instanceof TLRPC.TL_peerChannel) || ChatObject.isChannelAndNotMegaGroup(chat)) /*|| ChatObject.isMonoForum(chat)*/ || messageOwner.post) {
            return isOutOwnerCached = false;
        }
        if (messageOwner.fwd_from == null) {
            return isOutOwnerCached = true;
        }
        return isOutOwnerCached = messageOwner.fwd_from.saved_from_peer == null || messageOwner.fwd_from.saved_from_peer.user_id == selfUserId;
    }

    public boolean needDrawAvatar() {
        if (type == TYPE_JOINED_CHANNEL) {
            return false;
        }
        if (isRepostPreview) {
            return true;
        }
        if (isSaved) {
            return true;
        }
        if (forceAvatar || customAvatarDrawable != null) {
            return true;
        }
        if (searchType != 0) {
            return true;
        }
        boolean channelSignatureProfiles = false;
        if (getDialogId() < 0) {
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-getDialogId());
            if (isEphemeral() && ChatObject.isChannelAndNotMegaGroup(chat)) {
                return false;
            }

            channelSignatureProfiles = (chat != null && chat.signature_profiles);
        } else {
            channelSignatureProfiles = getDialogId() == UserObject.VERIFY;
        }
        return !isSponsored() && (isFromUser() || isFromGroup() || channelSignatureProfiles || eventId != 0 || messageOwner.fwd_from != null && messageOwner.fwd_from.saved_from_peer != null);
    }

    private boolean needDrawAvatarInternal() {
        if (isRepostPreview) {
            return true;
        }
        if (isSaved) {
            return true;
        }
        if (forceAvatar || customAvatarDrawable != null || messageOwner != null && messageOwner.guestchat_via_from != null) {
            return true;
        }
        if (searchType != 0) {
            return true;
        }
        boolean channelSignatureProfiles = false;
        if (getDialogId() < 0) {
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-getDialogId());
            if (isEphemeral() && ChatObject.isChannelAndNotMegaGroup(chat)) {
                return false;
            }
            channelSignatureProfiles = (chat != null && chat.signature_profiles);
        } else {
            channelSignatureProfiles = getDialogId() == UserObject.VERIFY;
        }
        return !isSponsored() && (isFromChat() && isFromUser() || isFromGroup() || channelSignatureProfiles || eventId != 0 || messageOwner.fwd_from != null && messageOwner.fwd_from.saved_from_peer != null);
    }

    public boolean isFromChat() {
        if (getDialogId() == UserConfig.getInstance(currentAccount).clientUserId) {
            return true;
        }
        TLRPC.Chat chat = messageOwner.peer_id != null && messageOwner.peer_id.channel_id != 0 ? getChat(null, null, messageOwner.peer_id.channel_id) : null;
        if (ChatObject.isChannel(chat) && chat.megagroup || messageOwner.peer_id != null && messageOwner.peer_id.chat_id != 0) {
            return true;
        }
        if (messageOwner.peer_id != null && messageOwner.peer_id.channel_id != 0) {
            return chat != null && chat.megagroup;
        }
        return false;
    }

    public static long getFromChatId(TLRPC.Message message) {
        return getPeerId(message.from_id);
    }

    public static long getObjectPeerId(TLObject peer) {
        if (peer == null) {
            return 0;
        }
        if (peer instanceof TLRPC.Chat) {
            return -((TLRPC.Chat) peer).id;
        } else if (peer instanceof TLRPC.User) {
            return ((TLRPC.User) peer).id;
        }
        return 0;
    }

    public static long getPeerId(TLRPC.Peer peer) {
        if (peer == null) {
            return 0;
        }
        if (peer instanceof TLRPC.TL_peerChat) {
            return -peer.chat_id;
        } else if (peer instanceof TLRPC.TL_peerChannel) {
            return -peer.channel_id;
        } else {
            return peer.user_id;
        }
    }

    public static boolean peersEqual(TLRPC.InputPeer a, TLRPC.InputPeer b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a instanceof TLRPC.TL_inputPeerChat && b instanceof TLRPC.TL_inputPeerChat) {
            return a.chat_id == b.chat_id;
        }
        if (a instanceof TLRPC.TL_inputPeerChannel && b instanceof TLRPC.TL_inputPeerChannel) {
            return a.channel_id == b.channel_id;
        }
        if (a instanceof TLRPC.TL_inputPeerUser && b instanceof TLRPC.TL_inputPeerUser) {
            return a.user_id == b.user_id;
        }
        if (a instanceof TLRPC.TL_inputPeerSelf && b instanceof TLRPC.TL_inputPeerSelf) {
            return true;
        }
        return false;
    }

    public static boolean peersEqual(TLRPC.InputPeer a, TLRPC.Peer b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a instanceof TLRPC.TL_inputPeerChat && b instanceof TLRPC.TL_peerChat) {
            return a.chat_id == b.chat_id;
        }
        if (a instanceof TLRPC.TL_inputPeerChannel && b instanceof TLRPC.TL_peerChannel) {
            return a.channel_id == b.channel_id;
        }
        if (a instanceof TLRPC.TL_inputPeerUser && b instanceof TLRPC.TL_peerUser) {
            return a.user_id == b.user_id;
        }
        return false;
    }

    public static boolean peersEqual(TLRPC.Peer a, TLRPC.Peer b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a instanceof TLRPC.TL_peerChat && b instanceof TLRPC.TL_peerChat) {
            return a.chat_id == b.chat_id;
        }
        if (a instanceof TLRPC.TL_peerChannel && b instanceof TLRPC.TL_peerChannel) {
            return a.channel_id == b.channel_id;
        }
        if (a instanceof TLRPC.TL_peerUser && b instanceof TLRPC.TL_peerUser) {
            return a.user_id == b.user_id;
        }
        return false;
    }

    public static boolean peersEqual(TLRPC.Chat a, TLRPC.Peer b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (ChatObject.isChannel(a) && b instanceof TLRPC.TL_peerChannel) {
            return a.id == b.channel_id;
        }
        if (!ChatObject.isChannel(a) && b instanceof TLRPC.TL_peerChat) {
            return a.id == b.chat_id;
        }
        return false;
    }

    public long getFromChatId() {
        return getFromChatId(messageOwner);
    }

    public long getChatId() {
        if (messageOwner.peer_id instanceof TLRPC.TL_peerChat) {
            return messageOwner.peer_id.chat_id;
        } else if (messageOwner.peer_id instanceof TLRPC.TL_peerChannel) {
            return messageOwner.peer_id.channel_id;
        }
        return 0;
    }

    public TLRPC.Peer getFromPeer() {
        if (messageOwner != null) {
            return messageOwner.from_id;
        }
        return null;
    }

    public TLObject getFromPeerObject() {
        if (messageOwner != null) {
            if (messageOwner.from_id instanceof TLRPC.TL_peerChannel_layer131 ||
                messageOwner.from_id instanceof TLRPC.TL_peerChannel) {
                return MessagesController.getInstance(currentAccount).getChat(messageOwner.from_id.channel_id);
            } else if (
                messageOwner.from_id instanceof TLRPC.TL_peerUser_layer131 ||
                messageOwner.from_id instanceof TLRPC.TL_peerUser
            ) {
                return MessagesController.getInstance(currentAccount).getUser(messageOwner.from_id.user_id);
            } else if (
                messageOwner.from_id instanceof TLRPC.TL_peerChat_layer131 ||
                messageOwner.from_id instanceof TLRPC.TL_peerChat
            ) {
                return MessagesController.getInstance(currentAccount).getChat(messageOwner.from_id.chat_id);
            }
        }
        return null;
    }

    public TLObject getPeerObject() {
        if (messageOwner != null) {
            if (messageOwner.peer_id instanceof TLRPC.TL_peerChannel_layer131 ||
                messageOwner.peer_id instanceof TLRPC.TL_peerChannel) {
                return MessagesController.getInstance(currentAccount).getChat(messageOwner.peer_id.channel_id);
            } else if (
                messageOwner.peer_id instanceof TLRPC.TL_peerUser_layer131 ||
                messageOwner.peer_id instanceof TLRPC.TL_peerUser
            ) {
                return MessagesController.getInstance(currentAccount).getUser(messageOwner.peer_id.user_id);
            } else if (
                messageOwner.peer_id instanceof TLRPC.TL_peerChat_layer131 ||
                messageOwner.peer_id instanceof TLRPC.TL_peerChat
            ) {
                return MessagesController.getInstance(currentAccount).getChat(messageOwner.peer_id.chat_id);
            }
        }
        return null;
    }

    public static String getPeerObjectName(TLObject object) {
        if (object instanceof TLRPC.User) {
            return UserObject.getUserName((TLRPC.User) object);
        } else if (object instanceof TLRPC.Chat) {
            return ((TLRPC.Chat) object).title;
        }
        return "DELETED";
    }

    public boolean isFromUser() {
        return messageOwner.from_id instanceof TLRPC.TL_peerUser && !messageOwner.post;
    }

    public boolean isFromChannel() {
        TLRPC.Chat chat = messageOwner.peer_id != null && messageOwner.peer_id.channel_id != 0 ? getChat(null, null, messageOwner.peer_id.channel_id) : null;
        if (messageOwner.peer_id instanceof TLRPC.TL_peerChannel && ChatObject.isChannelAndNotMegaGroup(chat)) {
            return true;
        }
        chat = messageOwner.from_id != null && messageOwner.from_id.channel_id != 0 ? getChat(null, null, messageOwner.from_id.channel_id) : null;
        if (messageOwner.from_id instanceof TLRPC.TL_peerChannel && ChatObject.isChannelAndNotMegaGroup(chat)) {
            return true;
        }
        return false;
    }

    public boolean isFromGroup() {
        if (messageOwner == null) return false;
        TLRPC.Chat chat = messageOwner.peer_id != null && messageOwner.peer_id.channel_id != 0 ? getChat(null, null, messageOwner.peer_id.channel_id) : null;
        return messageOwner.from_id instanceof TLRPC.TL_peerChannel && ChatObject.isChannel(chat) && chat.megagroup;
    }

    public boolean isForwardedChannelPost() {
        return messageOwner.from_id instanceof TLRPC.TL_peerChannel && messageOwner.fwd_from != null && messageOwner.fwd_from.channel_post != 0 && messageOwner.fwd_from.saved_from_peer instanceof TLRPC.TL_peerChannel && messageOwner.from_id.channel_id == messageOwner.fwd_from.saved_from_peer.channel_id;
    }

    public boolean isUnread() {
        return messageOwner != null && messageOwner.unread;
    }

    public boolean isEdited() {
        return messageOwner != null && (messageOwner.flags & TLRPC.MESSAGE_FLAG_EDITED) != 0 && messageOwner.edit_date != 0 && !messageOwner.edit_hide;
    }

    public boolean isContentUnread() {
        return messageOwner.media_unread;
    }

    public void setIsRead() {
        messageOwner.unread = false;
    }

    public static int getUnreadFlags(TLRPC.Message message) {
        int flags = 0;
        if (!message.unread) {
            flags |= 1;
        }
        if (!message.media_unread) {
            flags |= 2;
        }
        return flags;
    }

    public void setContentIsRead() {
        messageOwner.media_unread = false;
    }

    public int getId() {
        if (messageOwner.ephemeralAnchorMsgId != 0) {
            return messageOwner.ephemeralAnchorMsgId;
        }

        return messageOwner.id;
    }

    public int getRealId() {
        return messageOwner.realId != 0 ? messageOwner.realId : messageOwner.id;
    }

    public static long getMessageSize(TLRPC.Message message) {
        return getMediaSize(getMedia(message));
    }

    public static long getMediaSize(TLRPC.MessageMedia media) {
        TLRPC.Document document;
        if (media instanceof TLRPC.TL_messageMediaWebPage && media.webpage != null) {
            document = media.webpage.document;
        } else if (media instanceof TLRPC.TL_messageMediaGame) {
            document = media.game.document;
        } else {
            document = media != null ? media.document : null;
        }
        if (document != null) {
            return document.size;
        }
        return 0;
    }

    public long getSize() {
        if (highestQuality != null) {
            return highestQuality.document.size;
        } else if (thumbQuality != null) {
            return thumbQuality.document.size;
        } else if (cachedQuality != null) {
            return cachedQuality.document.size;
        }
        return getMessageSize(messageOwner);
    }

    public static void fixMessagePeer(ArrayList<TLRPC.Message> messages, long channelId) {
        if (messages == null || messages.isEmpty() || channelId == 0) {
            return;
        }
        for (int a = 0; a < messages.size(); a++) {
            TLRPC.Message message = messages.get(a);
            if (message instanceof TLRPC.TL_messageEmpty) {
                message.peer_id = new TLRPC.TL_peerChannel();
                message.peer_id.channel_id = channelId;
            }
        }
    }

    public long getChannelId() {
        return getChannelId(messageOwner);
    }

    public static long getChannelId(TLRPC.Message message) {
        if (message.peer_id != null) {
            return message.peer_id.channel_id;
        }
        return 0;
    }

    public static long getChatId(TLRPC.Message message) {
        if (message == null) {
            return 0;
        }
        if (message.peer_id instanceof TLRPC.TL_peerChat) {
            return message.peer_id.chat_id;
        } else if (message.peer_id instanceof TLRPC.TL_peerChannel) {
            return message.peer_id.channel_id;
        }
        return 0;
    }

    public static boolean shouldEncryptPhotoOrVideo(int currentAccount, TLRPC.Message message) {
        if (message != null && message.media != null && (isVoiceDocument(getDocument(message)) || isRoundVideoMessage(message)) && message.media.ttl_seconds == 0x7FFFFFFF) {
            return true;
        }
        if (getMedia(message) instanceof TLRPC.TL_messageMediaPaidMedia) {
            return true;
        }
//        if (MessagesController.getInstance(currentAccount).isChatNoForwards(getChatId(message)) || message !=xúÏ}Îv€8“‡ˇy
∆ªßGû8äoâùã”«±Âƒ3æd,'ô>=Ω:î€ÏH¢ö§Ï¯õÓÔÏÏÔ˝˜Ω√>Ÿ˜ã¬Öƒ• ííÏ§{¢sK$P(‘†P∆”·0¯Óª`D≤,º$Ìqrë§7aeÀ¡ø˛Ù¯q†|RíO”qêßSÚByı€ü‰∑¯"h	@A<ŒÚp< …Ep~tˆnØ}~‘ÔzPX– æuIÚc≈°µÏÉ≈JæªJÚ$¯ı◊ Œ>ƒIé˘ª¿≤⁄√<ØÇUÛ—Àù‡ÈÍã°ﬂ2Ã»‚1l^u?LGdú≥NX’w†g2é≤‡¡N†v·O¸ˆg2Ì„A–Oí!	«AvïLáQg<Ho'9CÓ4e§k©\˝uL”î¢∂;$”qæ"	zz3&ÈÚ§}⁄’\A#Œ∫L4†ú
ÇáAAß}MbvF˚°≤v'bX≥«w–’{óWÔãêˆΩÅÒñ˛ÿ‚”à†ﬂÑ«c«ÑD˚ixÛz8MIÙ.%◊1πiôÇByE&Iñã˜")çÈ» à´0Î|Œ…8"'èl åèØn&2Î:É‰∆„< ≥á˘U{~÷ qW,ösSÆ~˘ätiŒZñ»p çáÒñ–Ü7ÒEãKa!M5¢ÔÁ"Y]·=°≤'ßjÇ1IeËœMòÎZ5èæH+ãy%≥N3ºüª=´
H∂Œ'¢ñXöjdÏÂsEv?$ÒÄH	WYSÄoR}GR-\àÎ$éÇå‰Ô«)	£Éaxô·ƒ
3ü¥ÄJTÈπMYujY[P"¯.X[v¥ÒIñÊ=´¸∫Zæ¬Ÿ·∏V˚9∫n
kµ∞óåÈxïœÿê⁄≈
MØItê&£cr^¶…t‚U¯ˆ≈M‘ª†≈UIE¥3ÄÃæˆ&Ñ*I”
Ì¡U8ìaèät]ì8MˆÆ¬<¿tLÂ`Ì“d8§0®\
6fJÀj„˝Û`Éèæ Í¥ˇ3‰Ì8+â	®Õ£%ßNßyc9H¶®°&„KP˘˝8&óá6û®Ø+ÁîÀA8ÓÊTËFˆDV∏aÏ≈ﬂß·0ŒcíÅÒF].ﬁQi^ÇH~Ÿaxâ_-Ö∞ æ,%Dç∂‚f ‰K1Ø&—,æe˜*§Œ+ïπã¯≤ù±ﬁÔá¨üıºÀ‚€EíRoó‚`àËüó˛Ì0œ”∏?ÕI÷Œ‚ˇ†√}ˇ!Æí@ª≤NP‘¶ê1êî™≠–lË\YœGø¢¥◊JœpÌl:ô$iûı8≈‚Ò•ﬁ¯oı©}¸Èå “54˛¯Û£QHµ?˚.µ…/T–≤B.⁄£xDz˘ÌÑ‘Ù˚õ*´•WM&¬Ìà’s„ê∫*HòAêkkV° E¡\Â.À	§—‡ëà¡À	RÀ˚,vìvú ¶ôÅÊ$ì*œM•:õóÖ/ß$ÀYßÆÈ®cuQ51ùfÏ/äf3à¢#ànô¥`˙>Ì+¨æ^Ê=EGh[’X:å¡a°Ât˝ÖH7ˆ4#√ãï†ëÒaªÅ¬h*’FêÈë∑v•–Zµ™¥¶≤Yü∫8¡·–j ¢Ü≤2®#0œGxè'YÈDzhƒ%HxbT¨ﬁ—j¶#„Ñø\moÍ`-çR5∂@§∆mz‡Å˙âæÔûúû¸p|˙æ€L(å‹∆«7[‚j‰7¥›≠~˚‰€Â∆9∆Zd6Ö\ùÛ»´Ø@E™@‹õ("¬Pß%…MÈ^¬3Ëñ°ñL÷`NVêëZ•ø3ıÈnÒ]˙ÙÈÍÍÍQsúø&*∫P∞j¢HÚ3Ú8#K˛ù…ë4:◊ËΩì≤∫Às]í^«≤Ï1|] ®f=:ˇËMí,ã˚C•yk·^ZGt∂SµÑ€ŒhπòRñ‚∏”ÌÓæÈÙ∫ùì˝^˜|˜ú=<yc≠ßPNæƒsâD'¢”ÔE —Ÿ?<w Ò™&¯*Û|ã√®¿8T'P•-f]ËZÔºã£ç–)&´—†g#U{F™\'Mìt1"◊Îúùùû9§&[Ÿ‡äD”!âBa=é†¡óùeå	◊±„pLﬂVØﬁÒGÁtVO˚ˆ»X©dÀΩIöËS*g‘2ÕæûÅMÄÁÙ?Ôr#^ÂnK-∫ù+Û–*Å% pôÿ˛	ÙÓà\ËX@	±	 Ô id±œÒ¬i€ùß…- F':‡b£ru%pÇy4/˜|K§
>:µ“¯hæw¶]ÚÀî–ÜæÈtJøv'·¯EU¡w√ñT†‘8—ÕS”n}[fï/GEΩ£ãÿ1eÕÁ≠˛¡ºDΩ´rÛ[ÃíÃAÄØ„êb¢OK◊Kàá∞óìîDá#,°rVB0ﬂµŒ⁄Qﬁ Ãˆ(«=ÿcOh´\ C'?O√q6§‹˘GÎQ4im,{ ~å£¸™EK≠°≈Z≠¢KÀFóó°:C`≠TñıÌ!_R‡À+ú.$jwﬂÌûÙ:ˇÿ;zﬂ=¸–)øôsDå—Á‰s˛>èáYõäÓ Ã^≠KkK®»€äçÇ≤"gùq•∫æ´ ^Çï3U<OôªWñz,eK÷ËWUÎ1òQZu¥‰k]Õ\Bjñ¬tb
Ë¨¬9°ıÑÑ¬ÇµMê9S‰jH%"ë≤èNâîfìHÀx:$Q∂≤LE«@!Z vÀI»∆Yk,<àá‰(	#nÚeix jh6ı"?í˛qÀ±Ñ PÏÒ(”Q±pC˙#ﬂ>@-tÿÆFó>˚DgQÛ°•+ËÈ˚Ë.oDCƒ±gWê∫F≥3∞∆QÉ⁄÷úcvΩÄ≠/¯,r˚K¢ﬁpL√N·‘Rv	ƒs‚OpI≤˙—G&œdΩ˛4*EﬂD  <TB€õfy2Íåíü„*°˙ä$E–‡mH„o"‚jõï=q˛ê:™Ì~–BK`Õƒ„…4†)Ÿ;£I~kÕEñÍ[ﬁw(ªcjÛr’4+E’p8Ln>∆˘UB;G*ÖÜ≈√-Öì	≈"Ñ	È„œèÚKA9Ø±bœÛ´È®üµ„åQë«⁄P£x@ºqÿƒÕ”°JÊ¸ÊQàï‡ƒ!πÖBPÖ9˘ËEÖÃŒ™›´$en,"ÂrﬂùŸÁ{Tµ€ø≈„ËÙBt˝4eË56√éŸökæ¯∫8±¸¬ÒLqtkË]IRõ††°–¬ÒÓ4O`flÍ3ûñ€ ófW#¨ùs!fêè‚ú'CÃ&“Q´xtp¥˚¶∑{rxº{ﬁÅ•÷√ΩøuŒ∫Ωøu~x}∫{∂_oBsfüæπ≤ç¸î%Ÿ◊bÍXˇ7˛π¯∑;çb4 S!v…ÿk Ê˝±Ì#Èú£ﬂa≈(∏)ü!Fy´∫{ „“˜í^ŸR4xú\^.’≥l…pa∏=`ßT
X'Íß4œ¿ElâïÇö∏3ãz«tUë„3Œö»O≥xMWÁ—’3+´DÍAπ",Á65‚£·#VÑ°êJåR0Ú‰(π!È^ò,ÿÑÌr—¬Üb^√¡”µ_2≠uΩõL3ÁÀœè‡ıCV∂±J	“¿+S¿dêì¸èB_bÍ\±^yõå#°Bmﬁè/;†r©‰˛Uu%ô=<áŸ¨ÛıÜÿñ7g◊7Lçv(®$æØèõÀô-R√Èë¸f/âPu2J(⁄Ñ2^P…áL!uZÅEºÅù©@ Å«W$æº ıÁ‡£'¨v
«<]á3˝¥9Y4Qp	WS…àçªP¡ñ≤‘’¢fñR–P˙®b∏»ÈÏÅêM¨QEnÀÓ¬√ﬁÿZûq≈û0\Q˛Ï‘WD>Éˆà≤ÌaòÂáÙÇö^sCô≈≤*Øë¯BÔË™BÕ¶˝åoRÛ⁄É5« HE•E+Wô˚Z¢Á"å¬QXÂ“˜*X[ﬂfZB–¯{Å“cïÍ≈z bﬁcY.≈≤F•¡eÍç≠íß¶©glñ“ÖéƒŸMúÆXI›ˇi_ÖŸ’^{p–¡ÍÁµ≠Ìg´œÀ_œ6CÂW∏6P≠Ø=«aÑOîrÉµMÌó˙.Z›¬alØn+Â∂W√mÂ◊⁄F®˛⁄v‡±ΩˆÏB-GTàk*ƒÕ’–c3®Â»¶˙ÎÇ(øûPq[[
M∑∑5¨∂˚*åg[.<¬µu•\¯D≠5x¢b≠ªhEJ≠g´œÍØP˝µA8ågõ´*åÕH°È≥ß€
_ûmmF[dM-GTà[ ØÌ5=Ëõµ‹ˆSÌW_˘Æı0jüõ[⁄/>âxÑ´´
o√uUj√çıg Ø'œ4üDõj9ï¶·÷ÜˆãÚá±µ©Â∂7¥_°ˆÀ®]˛ä¥ûEÎ
é˝’pá—_çﬁˆ◊÷7‘_€ë˙k‡†G=Ta¨á
˙kD˝µ·Ç±±™ÂT˘Ë?›P¯“ﬂﬁtËm˚ô⁄ÁmU{˙œ¢æ˙À•/˝¡∫¢∑Ùó
q™ÙÙ|ÈG}ïd†‘¨n=Sáú÷Tﬁˆ£H¡j∞—W$‰ÈÖ∆ñJ∑¡ñ™/É≠mÖgÉgƒ!É˛∫ZndU˝’W±réÙM®ïS¥x@B´hıôC˜∑ülhˆM’˝M¢…ñC>∂¬-U"÷∑ﬁ÷¢g*kÆæ¨´cC?T,Ÿ(ﬂ¨Sñmı|â∂∂J[µ±±A6îüõ€—fIÚ‰	
ccÛŸ`ªDüÚÔ	%´ÇÀjŸõ(|ä¬X'´OCnnmêmEwâ2¬Dœpz¨G—ì~®`ˇl{†Zê’ãRB¢Õgx_û∂ïë˛$[%?⁄Xºæ∫Å„q±ææ©äÊ≈EI”Ëb†BÍ?6∂7Vü(dª(’6Z[›zn*ﬂ∆yK≠›ÄîÇ™ì∞ø~±E6JªF"‹m–∆¬íï˝'õœ÷6ô†ÂÈ&ZìÙ£çu≤U™∆”¡`u∞nê>Œ°à\Ñ”aÓ¨ÅzÛ∫wl,ÅËÖÜo˚î∆v_≈ÅL¨äkvÅ‰
Ô˛äŒ]HñÛß∑ÌÈÖã£5ûyÑ⁄· Ïnb–YÊ}lÏµï?î„ç“8"GA”xp’3ﬁ£'Ì.bôz´jdyiû—#Èøsß‚≤SBT&K©`Àã˚PX5oêµ'ó∞¨∞`∫yíﬁ‚ÀvXI*!ˇN–ÚA≥–∞WTú6˚ˇ0'⁄©a„O\UY† .U
a˝’D◊waÃ”J÷¢mQ:òﬂ\4V ◊†sØMD≤ÀÔ/_µÖïå5†©≥_9˜ıWÀ¢È	RiY˝1jV‡∞Ï‰„ñ◊‹ì¬Ù=¢eÖ=wûXß´Y¢ºKë&Ÿ{œﬂ53º‡{5πæ_á…eáq˝#\ˆ2hﬁ‚C`9A·u˚åFrD#í¡çˆÊ™°æ;cç{j›vò>e+¬ëﬂö†øõ¶·ÌQúÂ/yGÄ#Øˆ´@6—¨áºñ∫≠◊†œlãâ"éÂáèÖÃÚö‘àâQïxlw•I¡æ7s&€@•¬——®/m•N´Ö7√∞YªéVB@e,©B{/˚PñàX»ï:1‡p©ô≥Óê6ö]%V∫Âz∏)’ÎcÁV˚Ê
ÃÇ)ÓEÅyKV`ÙﬂHÅù¡/ãU`—åêI*r
Ãπ˘u*∞∑/°¿‹*=ì˚–^ﬁ“¸⁄k‚˛o§∫hŒï*’EËŒº5LiEBY±{PZé·◊©¥‹ÓWi≠√~Båkg)v;ÛÏ»2rZŸ1}´Ïe¨£’∆÷Lãø«≥Mkâ·tÇ»!Ãü€Y]ç9ïMô¡âMH_ƒÑ•O[kaVèæÆ£n:Ø4¥ï‘™…¥&é<9ø∞TÆ∑ëo@°è‚k2[Âdåß COû,Xe¥√,ÛÍ◊Æç‡]¨ò!„’KµæTU¥´h™Òﬁƒ_òtÍ©É;%úø°“vHS¡„ì®»w[ãûˆuMwKV¸Ú#Aµä]€˚aA≠ˆö	◊µ)Î~—uI:√ŒÈW≥ÿ8ã'ÑÚ”ó'•tøà◊Àì´Uqˆµëî÷ ◊X/Léª|aì™6ºSçÆj™ô2üêõ/? q$Óá|ïm5£∏É˜Í∏BÉı‘¢Òê∏kmûú©ÿ¿Ø‹Nr¶™ÚqÆÈ¨Ëél¬˝çÚã‡!·dπ8ŒQuÍv|›»¬œåëh®r5ÚPœ∞t¶⁄£j$Î]cd/Ã‘9¯Z¿D´é®’Zê≠MÇEùôsù˜˚£%¡RÉ-6Yà~ÍmñºQ˛J:ßlé≈1TµYÖGrIãëÖ˛G_'„	L‹0 iÏ“?ß˚€˚ÙˇŒÍ™˜‰˚¨à≠5√·∞>Õà©¨æ¬UÓ>0]	«9π§Æg|>H“CÌõ•e–ˇ–¶AQËòOÊ"¿ÂéÔÍãí@Âp
ó∑cÿ∏ÇﬂáyXÎF)PDS0c!ÚŸ&>\_O„aD%èÕ ¬Íõ÷ñ·ZíƒHªG∂'!lπUPE¬	?>=°BV (DÔ·√âàFb—Ûw@ PX‚e	$n‚Ëä⁄%(]náC‡<§“Sûºå#ÁATı√(⁄'êAü%¬0T aè˛7¯SG⁄
Üï5ä∫ù'2y;é≥›vç—,÷∆t+FÌ0bàÀmcéÏ+fìÖnHkúπÕÒ]X_óSf„ ù	’+_øRI˛ÔbØ·ÛÕfªmˆø±…Öæ2π®ÏÀæ»Lo◊:têÆ{Ì0˙bZ`‡ÃÈ¢~TÀúÕnè#Éfü}‰6
Vê[!±^Ô@EkÁÓ©Ü°πÓ˜ïÕÊèhœgŒæÈ&ZÎRBæ±ÌÓÿ¶úGÒC(Z_n_Pû‹´¡\±ÀIÓïﬂ¯Çût òkPﬁôZcAÒﬂE~ÊYG;‹wÂw◊Õ≠≠?}∂π∫Òlk˝È⁄⁄ˆ”ßO7∑éêN)>ã‹ÁT@{ê–ªî˛rÒÿÌúöJ¿ƒµÄB*èfŸ î>;†∏ÚI ÖÀg"X˙ÇU…Ó™¬˘t5µâﬁò8FW/‘#%£x %ºﬂô¯b¯ÒÊ%í–‹öëUf#%—´íÉ∆|íãIBIßr¢‡Tˇ&·À¡⁄¶ylß¸˜Jí›W˝WÖ™Õ¬◊“@Ì%◊$%Q0‡M∂ä«*’≈#ãΩ ÛY¨Ã´¬´@Q-Ÿ>j}KÖ	™Ï€˜™Dã,˝WÍy—UˆWÒﬂîôI%á_Tmdó{kÎá—ÇùGk’^ÌÔ1çe≈–]nÈ©$≈EB)Ïjj{≤1ﬂvî&A,Óóÿm˛&E˜Ω1‹Œ‡ûñöô@+I,QÇª]µÛ˚ıb4g0™ÇÁòTrÌnÆ“±∂2læÕ@qóÒÓdí&üŸÚ‘[ñ~‘ëB¡.«∆√è˜˘õ!\9mU}Qè=V◊ ’„ Y≤[†ÏmZÈöﬁﬁie›¸∑Z‘îNﬂEòY÷2´Ç%<ˇ·]ßwﬁ˘«πu»OM
›‘kÇëA¢Á Cò≈<ú˘jtOêΩ∏]F⁄cênDÌÔ∏uuu0[Eåfúùë…–±-:¸pÄlÆ◊π’˜ ‡≠r∞O£1%qÁdø≥ﬂ;ÓÏÓˆﬁùu>v>:!Ît§Ã“Â£KekN˜:‡¥k[Î5`ÏùûúÔÓYÇ°@Y´Â‡»É0™∆õŒ©ƒ⁄f«Ôªá{n €YcIí5à›∑ªgá'ozßù3ÎÌõ√ÉÛ WΩ≥Œ_;{Áù}7buËCπtxz“{˜ˆÙ¸oéä€Ò·˚c¸Â˘€Œqß˜˛›˛Óyáñ∞tœwœ∫^¿ΩΩ∑ª''ù#õJÔﬂºÈtœ9nÓn>©”M	Îı·Ÿ˘€˝›‹‡÷7Í»ƒŸÈ˚ì˝ﬁá√}ßxYz»2≈ã)ËbΩ?>˝Îa◊øë]≠+‚Œ+ãÊ•Xñ{2L¬<Öüãa»Í~gpÙº}¸%Xmo^º¿Å∞∞mÛkÅå≥sÿuÀqÉ,a»¿ èyÌn9µFOå–[À´`´˝åÉµFKvÊ-rèÇV‡£}ˇ|;úí†UN'c BoÑ¥—ùˇfŸõÜÓ,ø»ùbÌ€V÷»ÓΩmA´Ò∂ÚÍı”OI¯	]µgm_«£bÏ∏r@Gòª\™ÆçáFµÓC{ˆ‡ÙVﬁP4Âƒç7ˆó≈†<V[v‡XØK^íΩ*4ªäp;^Î±Çv∑œ¶!2‚ÿµì ∫}xÒ'´Éß∆pNKÑ√Çn]‡…*ÍµrÊWÌQ<∂õ”åÍJ≈Ë‚BÂOV˚Ö»(ÿ8ƒ⁄íî
Gªí≤µuw[©fE∆=^◊«FIX√ÓzÒ]Ï_[∂)iŒ+Â«Æ+ô√nå“v√î« µ ﬂÛ˜œµ◊ÍNE'\íâ‚†_ƒµ7L2íïÇõ∑°îF)5òb’¢Çâo®f˛S6áÑÕª·Á2“õˆµXÚµÀr1`_j'	lk–q;ç{Ç¿Fuçì¯J\dÔÄ°8¿W∞K±X∑ÔÆ1]ØèËÊû˜]0&$Ç¯∞◊√iJ¢w)πé…ç;òm{≠bŸ–\?1m§AüZç-‘jª1™Ax›§YcÒÉ1≤iÌ)Ãw#Y˙Ôˇ˙øˇ{©lZf◊™.nfGúRÄ¯Ó‚?Ø‘É’√,3VßK¯Í-'…°@#z“W+Rø∑ÚúTóﬂgÎ´®ÌÏßëH›”S	…€W•êµà´≤ºbóÅÀÿ‚∂˛Õ7≤`_/VP€QÂJ{—íÒe~Ey¸*X•^å^Î9≤6√ÓIÀ◊¨:ú»^8bª'Í˙πÃ‚Yáﬂe%t∞~ÿÒsWÎ-≠´3·Bï1CCII†B‡˘vMQX@∑ΩÌç°±TÆ‡kÑBæÎ¿=∆ﬁ!&ıØD¬∆SK«j©¢€˚ÒÄ8ˆÚfŸ⁄wúÃWsÜü¯EVÀ∆òdÇF≈∑Ωwek¯)îN¬TÏD9¸»KÉŸë·=˙ˇ¡Îı•ï≤ñyJ√€È»€„ccﬁÄ´õ∆√À÷a:h⁄ô˛ÀÚ~ôE/ôÊÉ§º≠”"e˝†"IãèÒxwgí‹(¨(Ï¶+kk-›Œìq/dMÉı˜å%ﬁ∫5Á∑2bÅï-zNàÓg#¥è¬_PÏÿ%¶3JôµÒÅ⁄à¨«å/‰*9ı%úù∆M€>Oc©ô[TÓK>˛X¬1£EÛPπ]ÀŸ¥aè›l}Bµß >Àh<p]B√;^ámˆºÀGãœ—}®|<~¸˜˝üˇÁ¶®≥ÖvJ&√êíåÇº ´t‹]¬”u`Ùw”-b‰0V	˝º)∏q˙DwBìh6:àË"ËÂáp8%ñŒ·‘Õ¢kxsÀÌk@Œ£dxl©ïÆ⁄Í€Bá|À´´ñuñÅ”»Óª4âµ‚[Éﬂo'<´Ì|≥Öπ;dÜ`=õ-6+¯vµÃtpéLÿ|*∂àtÿÆÀ˜µ ~t~Ê_w©ºî‹ìbªdÚJÂ‘– ∫g‘O©!∑ã«—È&ÅÆ'±
»6ŸU2≤Öuÿ{°#ÁÎpÈí≈ÿ`hAè'…8KR!É¸b0UÅΩ‡ÙÌ|ûƒ)0#IokÎ!:±≥JjWá‹•É’ùtœ≥Ñ ”[‚´fÚKƒ	pÜú‰8P5zÄê®€QMYÆR∞.,LmK˛*”&∑™ñ9
mEó:î◊Í:¥Ÿûƒ\ºK¯|ûˇ≤ïµb5.q¨º©rñ’;{¡r|U†H"Qn∫πÔ}€ŒÛa/#‘KâÿVÒÍÁ≠˛Òc Á∆TÂÎ◊ÄÆD#™öøµØ A¨}a˝…W~ªñ≥t@∑á¥15˘≥∑ü¬dZ[(>÷—ÛöS√ôO¡˜‡.PÿÀÏ◊ëñìÓà˛OÙ¶ïÇ _„Ó“1súÒ’Sæ∫Ó1≠êÁùïÚUπcd„L™ÿÉ®P¥Zó’VÎ@≈∞Ö‰mn–Y·hÌ¿$)¢„k’î	∫é7L®?õD¡ÀÀè,K·›SS¯6Ëñ»Äã√4Ú6 ´Y]}"¢æ‰aœh†	VÃÎæa˜BË√˛˜¡Z<X˜zÈnﬁ)$wfÖ£’%„à•ï≥©yfùfyvpUV◊rºcr¥•?Z®+œŒ¿À|ùT2!ÉWπ≤’%`’A?9Ô™ÂeUòÌÊ9#Í‚Œ±@Äﬁ Ê›H“Ô5ú#bÂ⁄wyˆ[9…ÌπÙπÓ 5¢âñ,!ûc$Ÿ ˘6|4Å©:ÿ>{î|g>C¥%í$Œ:@Ø] ‰0˙ÂEåÛG~ûÆbsßg˘¨[€≈Ã«wQÉBΩŒ.æì˛f˘•Z≠∏≤¢¡: ‘;ø"£⁄ã∂’¶”∏Ïàq´Ç?äZ:≥›Ê¢/N®¨;7Ö0∏“äÙ·‡ :˝Î≥&Çmà1È5jlLﬂac¥†û„™åMŒ»P»èÎ%yÚ7°YŒV:
eÏñÎÂf)#†äß6(ﬂ˘§ŸO%©·tz.ﬂ	§•p2°äƒ¸†«üÂó9ê‚Q»µiIFoÉŒ(ë¯puR[‚#ŒG<$YIç@r*óïyQÔxp*˜…ÿB÷yú„ÀêzÌ,{%8i‘¶„O„‰f|?â˛ŒãÛm^õÕ|2ÃÏ¬Ï„Õ˚!?Ó4≥¢:ÃØáD…@LCg(tz¬;÷≤&◊ﬁÏ¬x{B‚rê4Õ`±'nC!*îã>ÏAÍ;4Ÿ†~(EJ<eŸmœ√∆ﬁEfŸå∞&œ$B,òÉ%E?kgÏKõQ˚=√T“ÉMì¡“6·îV¶b≈ûÕ€Îâ=€m¬Ó◊∆™≈Ÿﬂßt*‡… ÅÙé≠I“◊‘ºçi´ácÄCº˚“„%æ8T6≤œW0I0œUìWA‚ŒU…w¶µ∏IÑÀÙÃBÔ»Ï,·¢ëLe^8–ƒÆ›™´GKîÅÃô¢ﬂ3¬5˘o®π aöﬂˇ° ÁH◊‘æ{l{eÛ≈ÿÙÿÛÿÚW_ îƒ4kf†©	p™ø"¸Ûj|î–øÑ9ú”‘ﬁx–ÑYñ¿Ç=Õ£&≤,æiµÄìC LF≈ÈôïV•rÃ1eF6Ã$"emu	⁄ÿ4ÛÃkîÓ∫∂ø™≈±Åa]≈X¿¯
Úéé–$≠√Yª4¯lå]"ÆØX¥aÙYº786nœ¡…e◊Lw7ÕoíÙ”˚tXÃt≥Q®£íØ,mDåÒJµ|H•Ob·2«!D∆£‘„‰Ú≤…≤J›„çqΩ\71_Uä´CÒÍPÉiª?˝¶Ôx∏ï	Ia¢¿ÍñmOqœa÷52Æ-y'[ò¡$ü√iD>&iîâIπwÌP~‘ﬂãpiÍè·OtŒ∏Æèc$n0∆Ωæí ˙!2›+¨±ÙË^ä¡—…”€j†Kai2û?~ÁS:õÖÎ‡®ü<HFè3¶É´Ôsíév`Z˚˛Ï®3$‡Íˆ∑Ï}ª<
†Î•Í˚ÛÉG€KêılÈ;™q~ªì%„ÀÔÜÒ(Œw6°hãŸ•‡{Z"€Y[
ûŒõ¸~a>∏
Zùœ2·ßA/«I ÙfŒ˘ÌIgŸÑpwö_%hÙ≠Q¢ÓR©(~ﬂk•F§ Ë◊√ô‡ÛmA-^êÃ≥/Pï√cC-qÆ2;5U*&S‘ˇà3ﬂöfÂÇVE◊˛∫VÎæ∆úØJ~âZ›u˚
é¡È4g”>ú§-&^‹DΩã49cˇdÅ6¸◊ãÒã&Ñ§Ô3*[Uµ€SZ™«Ô“Å
p¨?æÙ\^∏ÃìA≈Ù	îgáQ™…Ñ¯E‡ád⁄h˜Å˜èuÃs–vYv‹c¿ÿ∏ÀÚ®ÿ;·E`<&ﬁ˚z"$%´uá$g≠à„]∞w¿õß_˝ièÓñŒ¨U˜–˝¸KˆD’◊w!≈Õ˚Ä+˙.4˚n˚7‘πcãÔµ!5Dı+º˘‰ÌÎ≤!sõ—ùáAóÙ@Çº€øÏû›˙¨	·©ºıt÷9x1TÍ´! PÅ_l†TÛöÒ¢~[P‹∑ñ1«‰±â€\g€QL
Áﬁw¸(Û†¡ƒÓ€úÏÎ—¸é”K˚>S|%∆,’ ∫˙ Io¬4"ËYfÂu„¿môço@Ω 9S¬∂¯DêµqûπŸyvF\û‡ó.—⁄áŒŸ·ÅÎéÔ±¸8ÎÜ◊˙ÕÉÚïvñLŸü«>3˘∂2≈G≤$6^¬·¢Ygçn»–‰~T„çÆÛÂáÅÄ˛0ZN»ELˇÓå≤ÊŸWÊ{![˙+áœ2O„¬®LÖ-€`(~I∫^láX¸~†	⁄Ó…È…«ßÔª/0ISÍà&( Òç>„Ù«$±HF¢˜k^f¡wÚ¯jß€›}”ÈÌæÈ|‹gÙÏ™˝Lmıaß0ééyV2◊ÏDπ«iè&Iö÷≈z≤†Í–?´}yóöçnrÌÈ>tkV›ÑÜ4kÂÒ°.m¥vjYbéòwI zÚR≤‹‚∂àÅàH™åÏX›jˆ_ ˚m[3≠y˜˝ôb  ˆÙ„w-¥DÒ•7 .ÂT…ªté#.â|neπõ÷gùwG?pj˚) îP¶°GFmÂ}ìÏe-˘P(9Ã¨˚´⁄Z1Î÷m€s
+VŸ?K]ÑÎ*
÷K-ÚtŒ&À)N8&7´®.™©v—oZˆø¨2ı¨e®ä ÑºöÎöá^íÕu9uOîÒ	ÛUòâ`È \«)/Á\ÛÔãrØ¸Üaé?ƒ‰Ê¸*%°ÌnkêE∫a:¡™îZO)1Á≤Ê18wÿ‡qûLé≥K9BV«ΩdƒQ-å˙–ﬂÏQ<˛D¢ÛÑ≠|1oV`£∞hÒöÃÕDáÜUÈû–r¢-EëOpjœJ{Y©Ô˝r˘ºR.;QúÎÓI±CÑ9t÷7Åmi8‹G'I˛ë!a%O`¿”›*}ñ≤¬öX	28>>úD8¢]QÁ<’A⁄Sπ√*¥Ñè'ªlg©–è|‡
l_YYqo¢qÁ"~i¢ºË∞…Ìí˛k)˜Nèèﬂüûˇ¿.l|7O‚*¸»∫n!§@¢ïxÊ*«})bŸ¡H—"Éê†ÎÔﬁûûPºwèéDm#ú>'úù^ìR»ìô±FÕ<(ﬂÚÃoù©iﬁÁŒ)·KÜa,–‹u˛
+Iôñ}‰ñÄ“ïXÕ¿W˜æ.G’M¢†€í›Òm>ãíıf4?Ü[Ü√Ü—≈ÑÔq=Ë+˛	≤`&˜é¸éõ:Ä™|tfhPñp˝Ar]˙+xíd:*äYÉ∫a¯bL[∆ª k!ƒLÅaÂZ≥’Ωé√^?…ëômõ>zÈ<‡]-÷öÖ»`A1ÎãW˛Z3/ºÿ6ﬂ$ﬁœ|˜‘Ú.Înƒ÷€|U∫úë!ëH‰⁄à≠ﬁÉ5±ˆùU¥ó©]ÁW á"/úXàkq tg;ï#rBR’I· ∑È¯Ê	‹†√73√hè{)\†•πÀ÷À6’Ωº»ê?Á$¡íi.[∞Z\(∆˙U"\Tè»E8ÊΩ>P,B‡<p” 6%¡„«µG,Ô °9¡)¯-7ΩIöËÉx|ImKhÊÛÔE'+"æî>(¢îπR™Â!‘3⁄?ßÿouµUG$™Üãˆ¥ºr;á¨*'íä)ß!æ≈U˛X“≈ó®1a#zvéÈãj¡‚èòä-èÇß3ç‹¶Ö„‹íãºêÉO‡„˝çñi≥acÜ=Çtu√x¸©ÓúcèÂ>5æ|M.k/eµ”xp’3)ÎÙëçÏ¨∏_år˘¬za·ˇÚ*NWkﬂs∆Q/õ^RB±ÈMí,ÔQÓßöA¡
ÂâßÚÔÓÛJe™ìıÜB™n_≠ü¸ïé…Z!Sâçó^Ö÷ ÷eZ±M:Sm±ﬁ3cÌrgF bGivÙ’pÜô úS√Y^-7ãı¯65˙ùMçò≠ypÏRÍ∞üîaÆ‹£y]9∏ùµ°IÖü¡âÖ˙sç†∂Ÿ≈“ÑôA$  ªÄÒpˆÉQ vñqÜjDÀGGù⁄∆p>EË≥∂dç ñøÖ8´s`"∑ˇÁ !ìÎŒGTi¢Áv⁄AéÈuÁG∂ô3‘¿¨0∑)o8_vÿRpt”ß:[Ùï'òµ∂∆Ã©-_á¶Ã©%sk»¥c>Õ–¥B{Îq{ÌC¬Û$Ñ„}2$yq°Ck1ñÎä«I‰_-îcî.’ôºË•Ó;»úwæeêô\”ó2 ‚ÜÂñÒ°vÑÀ"Éπ.eíŸ›3∂á[äœZomL« [Û3gŒΩ¿Ey÷|_¸ò·7`ÑKGªÈz ˇkSüÚıÌ˘eJl€Ÿƒ„Ú.Õ1s∆÷ÉæûIu›ô6*ÈbÜ∞35Ô∞1ep.TWù!0i)ÍôÎíÙ⁄qòKﬁjsfÆ\ÖûTÅ¡Ì'ª–.¡-æX≠î⁄›;?<=ÈÌwé:ÁùûLÓzª#√cufËk°üπΩ?ŸÀJˆÜ◊≤7>tgŒwôâ›q~Ω•∫bÏŸt¡loy4≤Xá>1oQ™<<^u(i!iÏDÛãY2)‘=[„L¨q Ø‚ò˛Ãá‡ÔèjéÒ_/…\IÏÏ#ÕI6WﬁÜ∫'Ωg:Â=´≤?ÃíıaÜdaGâa©ÿâ5Ø≠™q◊8n⁄f¨gfEp‘Á>:hÎa&ë .Ö\Ç…»ãÉòmı>◊Ìÿls ÃXÀ—;%∞¸VÜÁWáïk«Øê∏rÙï#¿\≈@9#∞h$Úd“â"`öíCÿRûéf¿´•T˜UìG ÷aÕx’öΩ:;ƒfû¸ö¡Zñ]$∆°∞ÇÖ‚Tﬁ—÷ô$fLvl¬);aLäÄÖ÷ó˙ÓÜ‰sEoKSböëJï⁄#ZeŸ9Sj*]?ß4∏r¡¯d’b)ÆL.ı@r›Ø=JUzó∆◊aNÑ=≠¥ËÅf;›E≈[uù©8
=R8Úê5÷æTS∏˛S,M5ò[∏€4Om7ûåò'∞ÂVêK2ÔjVﬁtwŒóΩ»⁄—˚π=Fˇπv'aE~p;éÂ‘ÿ^2˝äH~'ì\ªGuÁ©wÉ°3•ZmÙÚô÷€-Yw!¥˘Ô‘|ìÀØ@.m∏|ãb≤Ñ êKtú©'ìG0ôÚ±™ÒºVÀÿ∂4FR{ß¡„úW∏ä√·pNà3Ò˘]ﬁ5¨˙YK9íÀ4ı˙·‡”%À€\‹B·”f◊R¯˚iﬂ∫˚E˚»Ó0ùª{b
≈7Ë?«„ò!Î~.-∆ÁÕ;Í\R$ÂWÔ¬¸ä’∑.‘Ê’~›	÷‹sPŸ∏ªÚ∫G>Y!¨s◊	UB∏æ‡Î_—'”°›	≠Ÿ˚|dIÜ|’ÎòÇKzÂƒöö¢A~z®e◊Õ≤øπ∫åb©`ÖΩw¶gqB,ÚLfn˜⁄Û∞f‰ÔS2’ñ+‡e0ÄÈŸ73√5BcXßG’)˝Œ?Œ;'˚ù˝ﬁqgˇp∑˜Ó¨Û·∞Û—d§ïÈsÛ; "∑ÒWΩUŸS~Y∑±<îüàÇ=˛“[Æ"°“¢©6ª{√∑XÕhx#W“˘ñ'ÅºÁ….#µﬁ÷
Œ?|YˇÅGU‹¢ê)‡?wØw!!ÿ	6ˆ&u„ˇhr≈q9,j2ÒÓÌÈ˘).,¥
Zë1ÏÅXà6Iº7L2íÂEùèq~[ÁsËR∂à[⁄aÈ#Œcû”¥®¬ñ∑õø#10B∫DËK1p§Ç®'c™P–ªﬂê⁄Ìg…pö@Æ≈ÆáÅ€dññ±©‚øDkNÈl föıQõµnﬂ{sx`%Ï¯p∏ﬂ9µûŸYC>úÓŸOèﬂw˜¨ßgßÔOˆ9 E§“Ë:™ "Â˝«»5x·¬®Ú÷	°1f˛,±ÉCT9%v_HU∆SBªr©∞êñâ„îèŸÏ;x∆|S1´Ÿ∆7 I†Uâ∞e9mÜSØ”3ÃÂ±?¿á˛sHõè§æ•Ä;¡ìπd3 ÍZ‰Ú§:>ßÓù;Í·ﬁsÏ˛n«`Ó9>ó©£áa°ç‡ÏBWès"`u≈åÅëFñGõ∂Ÿ;úÍ¢ZyËô=â _}V}›Û=Û¡∆êœÊ‡bCß8eı˜i»ex&cQÀ3å!S¿Ó∞'ÈmKƒ‹˝?T*ÿ#·g-he\Ù`j9€ªâìëWÒÂ’ÇIƒÆtDØTÇuû∆£§!|++5+Ω7Õ—€æL!*9±õ¶·ÌÂ€KﬁÒW¡åÓ¶t‰.ãºRyÀzKã∞ømZy‘ZnÁ…QrC“= >µ,á˝„Oº0 /*f*≠•ˇ◊?ˇ9˘◊—oˇ„˛ÙÆXÙ »o}<H“HVn∏◊Ÿ.ˆ∂ô&9ÈÂ¥¥≈e!6E‰ªcwºÑPA1˘ˆ Œg”Ÿ©2Üv…)Û5fLî“aâÇ∂]π
«—êÖ[eΩïí}“¨àiL‹të”‚FPˆ”!j]ùˇ–ô›·∞≈Ä˛ @Z¨:˛[«eêG3.À>¸Œ9+ºßﬂ)k5'€ÂJ>¬\ Ç;≠·RB8t®ï¨ïpÔá∑‰/êπ;˙õ∏€›“çL&ÄÕ≈$hQ…∆¢∑ìy”¥ ÇU3áìs˚÷&ú,¶Ç/ûWáÏôJ»ÏØOD5àêQ¯‘¥A∂◊j…+«™Ü∞¬g&Å’QƒWö˜ÚV+!+Íe Üs°îuFß¸™!î¢_É%QÒπ{c^‹˚ıâﬂ˚ı)xYéR≈mÎüÏ€æ≈LÁÔ™õ
U¸Ùìs±õ.ó_¬óMI v<vñ’∞è9ˆ1fD^X„ñ±Å·∞¶g±ùs…è´çØB∏Óû£Ì ÜÑˆ4—)fD>Éaˆƒ„ÙBßÎ‡*Lwa2È∞G
‰<Â,=‰Üp‚VçG·gúœ+g˘≥;ﬂΩãÇzb6ÌÛãƒî ıñ6)ﬁ£x\`èg∆˚¬T?∞∂u·SàÈœ\L¶îß≠—/∏PJb∞f?fW2`ú˛Ÿ{…1CÎ·C)Fì™k$˚)	?9Í÷\<Êty;⁄	¿Ÿ%PÌ'5«8\gYRûŸ3óÀ"<8öØdX¿|S¸djÆûˇ;ÃWUz‘EUT≈V]ÓYÔöç:d:Wﬁd}V∂Û9‰√[{üüÑm‰ï*<¶{ˆ W°àJs\ WÄçx?ˇ‰UD££ÓÀ‚uÊ◊;µ=^éf\∫TÙ|càIÈ†C5ŒÉ+\4¨ge≤÷≥9ÕRˆsöı÷Õm¯‡∑u∫/‹}P¥^)èº´)%◊§?zT˚r_èÃ©LvÌ√géÿÒ‹_Ì,IsÕ»µËølm9xÙä˛)}≈GAÊá·#¸+6)◊πiF‡ îP©K±ôúQéçLF!¯ K‘%DÁ ï∂ËÂõò≤à∑ı⁄rv◊êç€u∫êáÉŒÍXEWΩeÛ»‚› ú ∑ú¬/`äb∑(EeπËN’q·¥>‘;ˆ¨ár<ÊÒdH‘∫≠•éóV+¡RÄMÇyF6˙’ŒâÉ≠ÉW-YW†ÓN¬Ò8Ï	Ù◊”xQÄs¡Ur„(VΩ‰éã>ïw)ZÁ	¥“*⁄[ÒˆààZ+<Î+µp≤‘?.}÷;¬;zHÕ2ﬁ—÷“/(˚ò¥$Cñ;hD±Öj%,ˆã√3µŒ⁄Qﬁ@0Eà{¨í£`;#˘È5I”8"jã≥]hÂÁ'r€ª°;πy]0ºäsÚz8eﬁtô∫¢5hâ·™µø¨Æk+úB$jwﬂÌûÙ:ˇÿ;zﬂ=¸–)ø9†Í‰™¢’Â6µBdï“Q<˘Û?«.~®†¨_—)0,Ó®µ€<hêÚ›1-ÜY1i¶’ã—√3«~[jµ¶∂éTÃΩ≠1∆ôŒãª∑£~2ªø∂éÃãÈØ’ægûÓBÅèŸ%}^©8BR≠©Ûÿ
tà CW'˛TﬁpÑ1˜lôéje	⁄k.—6ÑOc9ùv[b±RŒ·ãπë)pË(Ú«˜≈∑ñº˙vö’æÿ∞*vù)S‹K¨¸>ˇS√∏!üêHe«‚[∞õ≤ €õ∏(À^ﬂÔeÙ5°¨ÿaNFAV|s.ÙElÁ¶¨≠åå≈√∂ñ„Ÿ6ÇÖ@ä§Í∏æ’â+€+çxsÌÿ"Ÿè¶É·ó/j6Ùãﬁ∏=ü≥Ωçı’b∞-CL8å
I°-≤qPÊ>oÛd≈&ùä›J¡ÿÓ(k¡Œ+Åz˝…Í–∞Ü4’‚√jÛoai*}ôè©"’
"¶ ¬Õ≈agC¢uZ´q Œƒu	Ωspwäo4¥`Ì^òV/ËjEœ÷6TCíQsØTﬁ≥%˘≤õâ›•È∏{ìÅπE 5‘oçUÏ®ßµr£ ê˘≤§ûñ≈N-b6H˙?õû}4kÇ◊Úú*K_B°P`m˘ì´IëpG}_>Ùı ŒŒŒÕvØ√x”BåxpOqÁläÜ_¸
˜¥≤8"˛VI¡+ü|ûƒ)‰¶.2¬qR q™Ö–≤Ïﬁ’ïqUæ —1+oòAll°áDøe”aÓ]¸v„Q‘ã“ÚjÂKËu˝\Eéñå\˛).åÿ°‹(RY¡„Ü#=92oqÖg‡ÅÎMYeƒÖ8J≥ıs)1≥ÃîÉX‚[/Ãz9?(]ô‘Jåfsd…ÆÖUë^FHog0bƒd©y#2‘KºËñÛç±òÂª!∑lw•`(nE∞UPôÒ¯Vmã’ éà‹˙ÓR€ä<à%— C˚ï§UPòÓkY€Œx≈@ ∞3∆a≠`F@PÜÒ®=B7‹îJÌpúåo)ú'µÏúª'ß'?üæÔ:ÁXj)‚åfºŸ"ê3mÎW‰ﬂi˜<≤fÛê…P¡.SõàŸùI÷7a˙¬$±ËRT`¿Ü¨H¸Z¨	õ€z‚Ö\•¯ÂÃó%mtºídî„dµt5î[^P§Ÿÿ˘œ`_”0’µgZëäImÌ◊ù˙ç	ﬁ4»IÀlZ!ÑMï w°£ÑhN4;ˇ÷ÿá˛Êò-ƒ1˚=O:{˚CÓ_ÕKÂí©œ%æ…ÈëSNõbﬁÕ¬@SÌóiKßí≠w©Vì’:†zè¥k» ©*ñ⁄KìÔ⁄Àí–?ΩÊ-ƒTímÉ⁄+¥É®´’†Ö—"n<«W+Jﬁÿw;RÔûM¨hPƒ\πxV,zîèt[∆´"∞-k¨X˜£K≥´è◊≥mF’›≠O≠ñ†ç◊˚’∫ã‘∂=9$l¥ë%«ú ¶˘5ÿï⁄™ÔÎ≠3ï}m≥Ñõ95Í+ÒP9sÜÈL-o“BÑÎ˙Ã†Wí¥Åpuqπ:â,[ŒÙ9Pˆä~Æ∫  ÜùÏ)ﬁ9ÉÕBYéˆZ3PñâDî^eYH<U}ì‡πÁ~3ﬂlàê˚bÇ! ˜]rkpΩ∂íhb•jJ‚nÀ7ıÕ©ÀÒµUåËG·m2•}|=ÌCdËá8ãÈ_˘:∏÷ó€˝¯≤¸ô~˜⁄ 9'ﬂ¶)˜:M)æñá°_Í0"ú$u§B¡‹1Zºê”ı°°{ºë”4bww)—õãù©À~~üqÜi2≤ÒQvqç7ı›DÙl∞¡ìr¸påá>Ùz	£˙+ìŒ§oª‹PÎ/øŸ¬‰√ÇuRº∞9£BÛΩ3J~é]˚ÜYk(ÃbÒ˝Î˜çé”vZ-^J7ñ°˘öó˘∞]Ë9è¨Ë™9ÉÃW±¥	Â˜¶Yûå—_∆Qâ+íÔê¸r$‚∂,7ä_Ñ	FﬁîñÈ(¬u¯m∆€‚¯üÇ$+I«qG@éÛ.bnÍÿ““6˜ûé√œ0ûÈêZj>ØÖuõ‰™¸ÚÏ€)≈ Èï„ßVzÛËn¿≠ÇNó>≤$‹CµøÕµªÜ[R˜å¨›®W˘†∆Z¿ÃÚ–Ù;€*õCä‹OùÎ^w*pÖ§ÒSÖÉd4	S“˘`≈44ﬁ3πuë'aŒå≤òh⁄¢Q%:Eõp-…%â<Ìø‚ÎÕd8n¨ÍÚpÓémê,ÖÁI2Áä4)^„RÏhÕõÇ%ËÉñ™˘"„wﬂ·#¢≤Ê"}Åbízi´ßQ*©œ„ÚÒ√¿êØ`yp÷[4 zÌ¥U≥”,„V–ißÎŸh¶wŸ¿"Ä∆7c˛ÕòœkÃSsfó; 9X¨+e3˘Å |¡q¸Kπ¢£põ3tn0®HÁs’Vﬂö'€%‚P	dÛ-68çÑ©≈b◊‰G°ë3uRcGQ%¸√`M.8(B‡	ú@Ï±ﬁÇ±»T{≥Fu®ï’só'ú\≠ªhXn›‡.∂¢:ËdAwà,◊_eç]_å·;÷zÕCÿ†Æ±“™Ø,˛˙´QéáX¡§Wﬁ'mÕÌ^ké”DÂR,òÒÇq8ˇêíûT∑;∏∏åˆ$F≈y%©7£'Á<Î¸p}Ón÷ekË$b´˛p¢á6Çﬂd◊·ŒêqÀﬂÍ	π1Ëàç “≈∞¯‰\C´fEZ°·lyÀm‡^O3 ókjàêò6cY“  RM‘µµ(´⁄zîg9™z«¥réf≠¯‡{Hì4Èá˝·ÌY<æÃì1AŒﬂ+◊ø∏9Û.3LÄÜ¥˛pNä¥SÒ§;ùkÆè„9øù(πh5hÌ}ﬂcW √aÂ;pR©O€PÁ
hmÍß±3}ûs»=Áê∂¢7Êy˜ß9)]ÂwïıÉΩª≤^P@0è¯™∞¡#ª% Ã´àÃwßQú¯úñ^;ö¶Ïú#Ìµ3!"*hÊßâøj uáYﬁÊ‰«üÄd√kB95≤WóGˇAE}„âgùSŒ˛E≤^g˚ç†ßz¿~+r’xWˆŸ9gá:¬†GG´0'ˇÚ´∏:mô¬3©zq&‚˙*«	|∂é£dÙ— Í%Ÿ(¬dsm}ı'ª3h4ó£»¯!∫”b∑˙c¸÷ÇVóÉ÷˙ì'‘ñ± ^MFÁ_˝c‰’¡î£‚@Jy∆Ué˝óY’Jµ‚áÛ:…Àõ…<¿uËÚ‰0äñ@Ù∆DRR=a|∆Sœå¡~Ts√‹Ù“‡«Ÿ,√Ù†Hi3Ê˛rC£T$©—€qï‰¡∫¢∑Ê3–Ü{$R◊´.ëzØ/-^ﬁ
Ã⁄]2£J Ö}b4aZÌhN/©6ï⁄Ã•22Bñı&iVêG?ÕÑ¥ Æ7√€-y7y≈√Ó'.^™πç|-`ºï<R∆ò¿Ow!/®àÌHß⁄u˘∂u’s+W˜¸ÙÏáﬁqÁ.ÁBŸ˚õ~_ì&DÌçßŸ7á:ªw®¸4ïﬂŒ¯J÷öÜãˆ±*’kÕ”FçıŒ:›˜GÁ][ª„€7ÒEﬁ∏çÉs ±›≥Æz˘ß´‡ª≥ŒÒ·˚„E{{owON:G®©ÒjÆLÍÇ2B.@ÕN#EãÇXóãw´ãÍBcﬁ¢iÚÂ[ó/QæÁy’.≠ˇ‰d≠√ü™ ≥"æ<âKS33•ïë2kêàíß⁄Ñ÷Õ;!XÚ…I8 ›A8$ˇ† €œ.Á÷ÈZ29uO≤!ïX–∞≤©Fì÷öΩdÚ¯±ç±çV{Îb%†ˇïZ-ú\À*aóãóŸlô--óJÆâü[»Õ9©%˜EI{∆$ﬂ∏§I)‡ï•ÀÖ…RvŸªH h÷cçõ3Ñ9πXthÒ<4RÕ¡¡46¨≈C•¨≈EÂùãèZ/'w¡I—¸Çy©tjÒ‹¥òÉûÕ“xn&\ªØ m9ö%3jÅGjÊì-[`L<ó…·è^_O_¿˛Ë2Ín,
ı2r<'˘Ia›îÕj—Öx√å‚Ln;ãØÜG]"∆ÄW\ªÏËó˙ë37∑Fï^GëqrzÀÔ âÅ9ª}n™∞O˝TúÏ7Û_X ÎØ(˘1ºLƒ®√¿–£Ÿ‚
°6g"Ä¿ô»Äﬂ°∂ iÃ‡©i†N»IíìÀê¯Xﬂ¶˙π:¸¢ ı¯›r3Úªp'‹ÖŒ¯%ÊÁ1@q≤ô5Òqöe&fC˘y˘]¬h¿rs†YZZË ŒD≥i¥öûÓq¡8’´"ÿ>GÊ í¢ôF"AC‰	à(⁄è√AJ≤&CâeÓ»~?¬ª\e∑ø¨AÆ¶¥b≤kúuı™õ>SÉ4ítÕ.ˆ–ÑªN'«:∞lWGJk7£›Ê“bù	3g!9Z }È∂+ÏåV–h1.ä¯#?tU`Ri\ºq.{´ëdæ&∞Aø¶ÑÒÖ¬3sà1≥´h0Û§WD´î€|ﬁfı¿±ç$Ok•d(ùm√â◊¡i2·òç·uQY—úÜ„}!_Bòz‘"~^«≈h=73Mx_SîæÊ6EÃ≈d‰¢≠zÇ◊Y£Ü∫¶cÆè@3œ¶ €ê]uúÉJ_¢Js;Ød›âùØë‚ ÖZSU^úóH†T8E`Q⁄™¥⁄q‰áƒi5m”ÃeX˝.ïGF°äÑ.Â-{^q™∫Ö*ë©i≤påUiÒ„‹LÆÊ«⁄¡„ÇtÍ˝C%j˛0è-*a8ûNtC§&RéÊe1FÌNî},m√ÆW;kÛK∂€pÌ˝Î›VøÇãÁ«ü¯ïS…q¯â∞+¥)ïÓVã"jàä∆∏_ öÑXm%ä›ÜYn
ë°75·µeàWŒº0:ÊäÒ1:˙}3ûõôÀX[ÚJy^ËúG›ËK¥%-|»Ñ$Ø∫–—„°!æà„|•Ÿ*–íÌÍ˜Ø∞` XDd Ç≈Ì0¯-§€Å* ëlê∆ì‚–æª§XY∞⁄≥.´x¸8Ä˝6æì†DÙ^”+ñ¡Y#r≤ıè%ô¶BaÉ	'‹u‹~q[§∞ÓS‹kE3æœúpÅ◊>∏ÆäC*t‘êp4SG◊úúœ÷€IO◊6\Äy<^d7‡-`πfOÕfÊÊè∂0–
∞>!u“ÿ…¢Ç?f™Gæç øè‰L4í/`â:X∏‡“qfií&Òê∏
 –“<∏+(°∑˝ƒ°0ÿñG.‘}Ü*≠ï
ÉØóıq/NUvÎ∫œ$iÂ+òµj+U˙ª».Ÿ`f›38(¶ÿ(QÆ∏[x%†n∑r¸‘∞wÏﬁ‰Æ∑É$ÚΩ√If∫=§òMôô1x∑4·+E’ú<¿ív&Ó˝e_ÿÔi=˙ñuT>ˇ≠I£+:=V4¨X=^1ª∏(§ãw˙÷`OÒÇÃî∑O	Ä•d±∏UóΩ†≈KûÈtt'≠@N <aîÕZ⁄Nø‹^…v(9
Œ°–‚ÊÜªçﬁª—ÖÒóü gu’3"<.Ã>Ü¡Zñgƒ©X]a˜uõS}cHY˙˚˚”Ûãù>◊ÛíõE:'˚‡≈`ˆN˜KõŒ¬∂ª	¢∑wzt¥˚Æ€tûæ0q>O	Èí¸%e!π$È+ŒŒà|&E&OQBK:…+ø≥´„p"+Ø*îÛDÍ Dñ|•Â,b!Çcvÿµ%π∆XQ∂«ˆJŸ˜¸[~;d?¸â8|Ñ$∂dèÿ˝‡‚ô‰ÎäZõYUµõ»¡Æ≤=ﬂ°Æ≤‘èÒOmF·$ß~yKƒΩ‹‚§æ¿'mì›vÅxKÉåd€‡V≈®⁄GVE≠f…v¶3sè‰i{2Õ[¬~∂ ßÚêÔﬂ»≠®€Ú≈k8Ô(?áã_ÉB§8€„v
¨Î˜∏ ?W/WÙF_?òÒwÙÇçFfÿC—ÉBïÒtÚÚsã1%{d…à–ë2€d ïZ_ß‚upsE∆ q£0áª+ÈLÃı~ÇÁøäwò√!À$-’ˆ0ÿQ{ã⁄Y.ûπÎ∞M›˘™]ˆ‚~i@bë.Í∞=f%ÌÌ^^‚%œ<Õº⁄±tmŸëígDâŒ`9+ñ(ä#i:sb$„K/…JS7^µœø¢‚µÃ6AX~Y´d3{ÂZDóùßÆ]∫+qÅ·ÏœˇˇŸ’!ùûèXñ÷8ﬁWB¸ˇ   ˇˇÏks€6Ú{ù/µßä&Øπfí∫WñSµvÏZJ{wmGCKî≈DëJìõ∂ø˝∞ã7∞ IÀûÙ:ßô8	,ã≈bÏ>äà8Æ÷ì·ä¢——Ü¿¯äi0eâl=ˆJ%∑ÖÉ[!°eïÊöÑ‘H:´∂b¬πV∆*(-E¨SÔìKX≠I|J))µZHA—∆‚%KÔiu÷çP¡*ã""∫TYÄ6Ö‹‚Œ<(8ì\K Á^l=nàX©∏z§†¨ëV≤>N⁄©2°õ;âä!∏¨á_Ÿj-º«B∞òeŸ∏ˇÖ© f|Ê=íª$ˆ/î≈ŒîŒäQî∂•ç?ΩCxÚıt¥‡¸⁄eØ}L∞f9*—…‡ÜÜ∞£H÷1ıkõQÒß≈ÍËuŸ(¶äÑJ}Q?ˆPÆvÃ°MäaÃaıñOM*ò£]çK/ŒÀ©WÓ‡Í¨§:‚ ò˙6Bk•€õ°Ùà«˙mm∑öâΩΩˇ≈wFªÒŒó3™U∑-∑‚é›sµ"rôV≈ıı"∆ºóŸ§X.ŸZëÚHÇ&∆Ÿ6ßÅÇ{‚∞Ó;¶^g”>Õ¶‰1∂¶í˚a!áõ Œ!O<(-¨;›lö≥E¡„8XSø·≠Ç3õ™ñ—˚}oº…&˜:	ŸO85‚∏˙ê”ız¶ß6∏‡2ùOÏ}Aﬂröü◊à‚ L≠∞„b6+3œûﬁ<≤Å	*Æ¢Ü ˜Üÿÿl	Ä‹
yÃääÎ˘å◊
fÒ±`~¡‘1$!Që*¡Ld_P¬¢B -≥â&Á¯†‘øN;∂™≥ﬁdÔ≠™L#Ê∞Í´c§P’2'≠Ù‹ê`Ì‚FkVq&˝æÖÑ<3ë?ÔÎˆÇ÷.^ûùàÉ31Áq&¸∞Õ'o—‹^29XÂq∂ŒeL¶Û!s”;o±±#™àæ|¸ÂìáO=y.˝0ˆÂºÿTlâÁÂ ‘âˆrß4èN«˘ä…›€°Ä0ò&ò%X√’b%ÄÆèV´ﬁ¿ûI|%âV¢:¨€ˆ≤∂è «≤T⁄πãµú9°wdïÌÒ•‚◊∂LiZ2N±í Ê≥ ÀAˇ¿ûR≤ú§ÜAZ≥`dµ UBÀB¨yiú/ºå0íë™14∫5˝∞	Ÿx‘¬~ﬂˇ…O4íbq¬Z¬T’5tÚì£bS[◊	»N≈ô¶ÅÛfYìÅí1ù4Iææ*<»$]–@Ω3 ñÎ∫+Kƒﬂéd}äÁ	áU∑àeæôRb¨¸ÒŸi}ÌvÊÎsuSûéà¶P¯ø Î∆…ú·ë¢¿ÁÂzë˙≠z!9Î\%⁄öi≤õñ™P?·ﬁÈØ;Œ:èœtçñ\§ı´Æç◊qm/g®ÿµ¿ÙñÕnÌ¢ªÀËîê˝£∞&—%£ªÔI6Éø&d5â“˜iæ ˚¸˛lld¯∑F~Ω~∏w§√#Fà'<jπU¢‚⁄[D≠ˆ3|FR=ÓU5G∫ﬁrx›#∞§êT3K'UoûMﬁ 'Ú5˙Íes ixF§[ö˜ÚÁπ˘Œ≤ùQÖG "‡L~bñ§¬í™¿úL@‘ÛA:˝Ì–!£-oårzÿ’√Æe·C!bı[mOÙˆ◊jc±¢›eí“˙ıÚ≥±∆?éjìNß˝UÖÒFíﬂﬂq:ﬁÕD·ì3Á€
9ŒZÒVÀ˛œΩIr»bµCPçãFáQ»ï'ªºgÚùl;Y#@é˙ˇπ6ëVÅœΩ~¥ƒ≈∑Á£Û8å¡q?^‚Â‡$˙˛dp⁄WoÎ|é‰ËC~âa∂y√Ÿ‚Lâ
∏oK7øÆbm:(	HÖ3'Æ≈M#p±Ñ`ˇ>&ôx@ÅeÂ•ÌŒó&‘ãy± @:4(˚M∫z€K7ƒÅwM
Ã⁄àÛ:TÍ‚jª≤Id}n‰ébæ÷Z˛‡™—Ÿ¢HQ?∏ÿ◊Vãj¡ÃÆ*	qmê†YÙ€îG/È•ìy6µñ	mvâ.OLıEj|¸⁄©o’¥√⁄'ÔxïQ1LﬂKk5Q“Ñåﬂ_oÚdû_œ≥≤-uíjæ]^©_ƒT¸|öW?ZÿQ‰Òûæ¡˘ÍTëo∂eví/0ï≈UZf?l≥≠∑o£Z{™Õ«Ã“˙´<÷jä•ãJÂ0r4÷ÑŒ¢˚J•Í«Ú∑Î≥ö&£‡éEé@Ã≠⁄p!»√ùA:Ùÿ˘H∫‰`jlHÏ7*Å•æ>6«á∫˚Ò§ÿg/ÿös&oÂÍ¡YìŒá◊3_«°˝¡`Uìy≤ﬂˇ0…–á&Òåﬂ·CrZ\w≥˝ÉM9Ê…Ù∞«/O7˚hä`N≤®5FæârºŸû77€Ä|{5¬'Eˇì˜•ßÇPïu a€∏Û∂]Ïtë‡˚‰µ´¢¶¬)xK∑]OŸ≤Ê„Ç9◊ôæ5íO•¢%ÏïÄ*±Ù&ÔòËrÊò´àπU’⁄∫euﬂu∑õúÃ‹∑Ìrj
"6@üö^∑'ÕnIäÌ.ΩË)ìn≤QzÌOóù4vù¶≤¶&uòafyﬂ∆i9Æñ±éÎlïM+pèÖ›†•0
KGAE¥˜(G9k∂JókKÔ«ÊH^ßúéöÇÊù0F⁄îÄˆÑKæëÖåÜ}hôç{T∑°;9˙∫;`†!ßå+˘≈Q!|XiL°6·£jΩ`≥:¸î÷'k»uönWì9,$ÔAb3Ç®∑∞]èﬁ}&S∫ÎtSf˚.~=rÂè,–u„h3Û	näDàtE3©⁄;q6ÖÇ÷. >)¢¿Ö∏îh≥ò6¡ß ⁄Ñõ®ù§eÂrb§uF§¥Q£àﬁça/$çzÖ{,±À¥©›˘ }c–ÇÉ*Ã€Wøì˝ÿKΩ∑¶Ê™,(ÊÏH`ÃW3‡›Õ&´wåQF¢í√äGrÉ€2wÎ©>•∞
4J¿gmîak…Z≥ rå∆`4WÇ}(yt·\w{}Õ∏û};ZØ7≈˚tÅ©‚VNÿ?æB£-Òl+Çﬁ˙,ÌÈœ6õ9EYI`∆ø‘8v∫±˛NµÔeá“Ëx\C†Zìt≠&™ØBVË±∫H7È≤LJı‡|63£&ª%ª≈L!q‡y°ßp”eâ'å–øìtuñÆ@´Çiã€•wj–<ÙùåÀÊ˘≤ÜÆ ´¯%¡ÅìŸYv≤{ï.Ä7∆UQå≈o.≈‡H(‚ı"ùpÂ‘[ıOH§a\PqóA7hñ1≠ÿöëÅ0|UT˝U±Ωû√í\v¸ÌÉòW÷À⁄ú™üõÏf7v;(«ÙÍ£S?ó®∫c˙»øä-}êsìàŸFMcjÀÑ7ô–¸  ¢|T”äåΩJw`#U±Œ'ª∑¬ÉÄ“·%µDX∑E£°„>=6çèˆ¯Lñ@,G∆ä≥¸∫6R8∫ΩBy*¨§¸hF†œ∆2\jg´∑l˝∏.œéπ x—yÌKL^$±)uúM`ô%ü5*9ΩÍŸ-¥◊„»∑hV‘pΩÕöJ¢∏‡a4Ô(~zaÆ€œÙÇÌ9k¬—Üä™—ø˜ÀÍóïõ⁄HñëπÉ@ñ_fãîmã0M4O◊}:;Ë`9Â	p≤ôOZÂR=≥}∏∞5 —äN|~ÔÛxÅqn’§K±»◊•  —*_Ç+d∆Áh±ûßXÓ—√/%÷;í¶&7ÅûÀÛbªÅ∏›<hkŸ2·¢	’R[âb⁄Dæ¬€KÿtΩ^ÂU˜€Û◊óCäA¡Kÿ©mŸŸ@Õ•¯—
oÖCkˆ1{ÕZ É◊⁄‘>ÊÕ∞V?6‚S}˛˛ëà6ƒ¶7Ω÷âc†íˆ\NK•œ¬Ê'&‰.3‹C#òáælm	÷(‚í'8˙Nzrëñ’0_ÆYr;ù“ õ¨Ù∑∏.Xª8‡/∏Nê∫Ó™ßqs⁄n≤í=ª»6h´G”∑1]-`éπ;R˙DÙDó≥?Î¶KÙ‡4tÁ=∫[äˇùmàLÍzLÙ¶mTÄ
 ƒÂ¿‰#®ãG¯àØı=Òº;:Eê,†ö¬ï€í5òu7ﬂ2ô<Û®£ôN®≥i….∂l.lFæ‘›-`æG-p!»ÏpgPÉíÂj¥®‹ëÂ†âÿ–MoãiÑolﬂàâº≥ÊE#¿˙ ‡âqppIÖ˙¬§
≠õπãÁ1Îˇ≠¨4èqNS ¢´Ëùıπ‚vzÜ†}˚ªLØ[]ÓQÅˇ€Nô'∑5eû¸µ¶Ãì€õ2O˛?eÓh êA∫d≤ÖÚä∫ıﬁ;‚ûêas¥*À6~™lπo¥.Õ_p∆Se?ˇöTi˘vl{–7øì∑@:∑å{Ô€P ßŸ≈ê «”"xâ≠∫∑èÍ"™v◊ …≤ 4ﬂtS$x–N©Æ§°7’@Ã“€˛*GoN»ƒåK¬∏ ªZtkD¸<´º±ô_Ë˛“‚d¿X˘X˛àÛ1FÂ˙‘L<*éã[db ◊Äâ±’8WXübbx”]‰eM†ﬁr∞Ç^œæjÃÊ’ BúÀxïßH$∆ˇŒUoó{¡TŸ¥É˛¢¸È\˝œ1hÂ
™JAÔ»!i9“®ÒËÑô&–h∞°o‘Î©jâÜ`M#ph"È2∆Ñ¢ØI?õÓÕΩvÃ¯„<††KÕoW;M∏“˙=√ªˇé{Í˛˜(∞∏q¶+’∫õËjFúF∑–n ΩP÷5>lQ"üé´ù≠`BvA˜∆ƒ!≤†q∞ÃD
áÒ’«‰&…∑¡n`_≤vqe0≠]‘Ñﬂ›1†G‚Íﬂ5±ˇ§«8Ëúæ±«8˙fÓIˇœÛ””Ä4@Irjô§ñH√¡_·”lRZÉÿ<p˝zŒ»Y~»»&∂—ûu•ysw4·+%¨Òöñ|Æﬂ
U≤Rcﬂ’ãMQ°Õê«dø¬a Ô˙ì7Çlª]ïÈ,ÛÉZÌÁm´+Èaøw˛ÍxH47ª~ﬂ%º`ckOÔØ–çóËê≈`qÅ£áˆ}CåF˚å úÆÕ∏5˛N;∆–@àkì[µ≠Gxg≥R∫[5˚ûV 	¡†,Îø)<„πø=k`›üÊå:1^≠G∞πy¢ke¥>¶˘8¢'kÅ9|˝Úe8úøüúΩ˜è£Ò≈eo–◊ŸjZ’Œ∞jÚ’W7´A!D˝G7®∆˛I èmr·œ‰é‚…¬u˙"ˆ‘I±P¸£X;ºt˛;ª5¶eÍ÷Ã⁄∂Rb"•U$˝‘e£êŸı4Pµâ∂EˆÃ.Ä¢4bKS»Ü√ïpÇ®õŸÍï⁄C7F·ûyÃøÀ£LâlwΩ…'€Z4 ˚Íà‘ö√ìãRÛƒxr»∂Ò‘ﬁ°n”z”Æiòõ°ñ£K—k¡u#åÊÈ^Ä∑√ÉÕÒÊxLRqåL‡ ﬁÌÿ~l{Ä‹…“FuN¢àz»çºû¡òƒ	ñj†n{Å[g∑£z°ÛUä˘∑1˚SÆ2∆B•ÍFÚ◊Àz‹,<E«Ω√CU’xJz° ÿ	ûçˆÌ7°ÛVóv∂¡tÈ#b˘âsÆP°€OÔâv∞p;©2CoﬂÈ¶|4´ø	Ü¥Ìø¬J °®Ôæl4 ≤í;∆ÛO9nƒg$º◊w8ak…∂∞=;l´œtWÉ=l’∫Ãk
πô\›]œ¡3Ωçä—&]ï∂:ÓøÑå¨*Ûrô\€øÒˆÁ_•~5òuΩ¥Ú≈îßç˜Ú:∑ºı‚∆òÀe∫…ˇ„˚JŸ«5:Põ√¢¬í$È“(è–Ïµ‘˙gÁﬂÜòÙ \ËË’‡Ïh‘?≥Ì}ﬂø¨).JB„˚É´üÈ•.@ˆ¯Zèé»(…AÙœIB≠jôïóp,GØ5¥ﬂ=|Ùz¬Ô˚kŒ¸Ã˝j¸êH∂"îì¡¡—u?§ DÎ‡È#c#‹Ò∞ÜÜµM»÷o7<√n$¿ˆ8¶…Ä•Õ≈À¥ÓŸ?}|Ã˛?|ö‹SòpPVºH∂◊∫wè5eº¢Ç2 Oê·U!Mc	Å€0–QÃ#≠⁄S(/µbfüO”ó5-'U±™ÿ%&’:÷d„N+Yº…ª3ˆÎ#k£Ñê?Á3|Xj∑ÎJ5H®)J$ÿd@ƒ∂¶‡fÿZ'ô…™®%¡ÉgôÚõ/}ç∂kNà÷Ûlôm“KXø&nB0 ìbôâ£;e⁄C?ZMÊ"jgº*‘æÍÚ—j˙™®§Úÿek[ì;≤:ﬁ›ùI&ù…•„ıjùNﬁ⁄G≥9œ[H5´¢K@óŸ$Àﬂgõo
7rÏ˛≈âfóë–B8x&'4ç¿MÊÅy3B3ôë∏3=Ü¿,gÂı@§∆iÉæ‚¥]ÒØE´V∆‹πΩ–ƒmF+tó8Î5‘®˜SÉk3AÛ·%öÚ3/…KS∂Ìc´a8<zŸé«ó˝aˇÚG¶w~3«gG√ÔÒú¿(—ø¯∂÷ø<:’E"haÍV°5H˝YÉ’Ô∑çîêLA¥h bVƒ¯œ::>SÈõhAæ ≤È%xµMög´>¶±Û—0¢t0Ω%û√Jng+áå#!ö¯¶X,î¢{7+‡Çﬁ3√l◊ ”÷òÈ∞ŒjÀøµÙ‚∂¬… i™Fô©Ÿt¿©®08åõÀñ≤4∫+#i\TkX§¢w0Z¬w*DﬂpDﬂ(D)Cœ7°Û>∑
`ÛÊ@ò2M"ÁB5:övbì•okœ[<ç±°≥r Ît(!YÔèœ˛  ˇˇ i˚c