/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.lerp;
import static org.telegram.messenger.LocaleController.formatPluralStringComma;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;
import static org.telegram.ui.bots.AffiliateProgramFragment.percents;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
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
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.util.Pair;
import android.util.Property;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import org.telegram.ui.recyclerview.ChatListItemAnimator;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManagerFixed;
import androidx.recyclerview.widget.LinearLayoutManager;
import org.telegram.ui.recyclerview.LinearSmoothScrollerCustom;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.zxing.common.detector.MathUtils;

import org.telegram.PhoneFormat.PhoneFormat;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BotForumHelper;
import org.telegram.messenger.BotInlineKeyboard;
import org.telegram.messenger.BotWebViewVibrationEffect;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.ChannelBoostsController;
import org.telegram.messenger.ChatMessageSharedResources;
import org.telegram.messenger.ChatMessagesMetadataController;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ChatThemeController;
import org.telegram.messenger.CodeHighlighting;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.DocumentObject;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.EmojiData;
import org.telegram.messenger.FactCheckController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.FlagSecureReason;
import org.telegram.messenger.HashtagSearchController;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LanguageDetector;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.SendMessageChatArguments;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagePreviewParams;
import org.telegram.messenger.MessageSuggestionParams;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;
import org.telegram.messenger.SecretChatHelper;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.SvgHelper;
import org.telegram.messenger.Timer;
import org.telegram.messenger.TranslateController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.VideoEditedInfo;
import org.telegram.messenger.browser.Browser;
import org.telegram.messenger.camera.CameraView;
import org.telegram.messenger.support.LongSparseIntArray;
import org.telegram.messenger.utils.FBool;
import org.telegram.messenger.utils.OnPostDrawView;
import org.telegram.messenger.utils.PhotoUtilities;
import org.telegram.messenger.utils.RectFMergeBounding;
import org.telegram.messenger.utils.ViewOutlineProviderImpl;
import org.telegram.messenger.utils.tlutils.TLKeyboardHelper;
import org.telegram.messenger.utils.tlutils.TlUtils;
import org.telegram.messenger.voip.VoIPService;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.tgnet.tl.TL_bots;
import org.telegram.tgnet.tl.TL_keyboard;
import org.telegram.tgnet.tl.TL_iv;
import org.telegram.tgnet.tl.TL_phone;
import org.telegram.tgnet.tl.TL_stats;
import org.telegram.tgnet.tl.TL_stories;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarLayout;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.AdjustPanLayoutHelper;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.EdgeToEdgeSupportMode;
import org.telegram.ui.ActionBar.EmojiThemes;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ActionBar.MessageDrawable;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.ActionBar.theme.ThemeKey;
import org.telegram.ui.Adapters.FiltersView;
import org.telegram.ui.Adapters.MentionsAdapter;
import org.telegram.ui.Adapters.MessagesSearchAdapter;
import org.telegram.ui.Business.BusinessBotButton;
import org.telegram.ui.Business.BusinessLinksActivity;
import org.telegram.ui.Business.BusinessLinksController;
import org.telegram.ui.Business.BusinessLinksEmptyView;
import org.telegram.ui.Business.QuickRepliesActivity;
import org.telegram.ui.Business.QuickRepliesController;
import org.telegram.ui.Business.QuickRepliesEmptyView;
import org.telegram.ui.Cells.BaseCell;
import org.telegram.ui.Cells.BotAskCell;
import org.telegram.ui.Cells.BotHelpCell;
import org.telegram.ui.Cells.BotSwitchCell;
import org.telegram.ui.Cells.ChatActionCell;
import org.telegram.ui.Cells.ChatLoadingCell;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Cells.ChatMessageUnsupportedCell;
import org.telegram.ui.Cells.ChatUnreadCell;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Cells.ContextLinkCell;
import org.telegram.ui.Cells.DialogCell;
import org.telegram.ui.Cells.IMessageCell;
import org.telegram.ui.Cells.MentionCell;
import org.telegram.ui.Cells.ProfileChannelCell;
import org.telegram.ui.Cells.ShareDialogCell;
import org.telegram.ui.Cells.StickerCell;
import org.telegram.ui.Cells.TextSelectionHelper;
import org.telegram.ui.Cells.UserInfoCell;
import org.telegram.ui.Components.*;
import org.telegram.ui.Components.FloatingDebug.FloatingDebugController;
import org.telegram.ui.Components.FloatingDebug.FloatingDebugProvider;
import org.telegram.ui.Components.Forum.ForumUtilities;
import org.telegram.ui.Components.Premium.GiftPremiumBottomSheet;
import org.telegram.ui.Components.Premium.LimitReachedBottomSheet;
import org.telegram.ui.Components.Premium.PremiumFeatureBottomSheet;
import org.telegram.ui.Components.Premium.PremiumPreviewBottomSheet;
import org.telegram.ui.Components.Premium.boosts.BoostDialogs;
import org.telegram.ui.Components.Premium.boosts.GiftInfoBottomSheet;
import org.telegram.ui.Components.Premium.boosts.PremiumPreviewGiftLinkBottomSheet;
import org.telegram.ui.Components.Reactions.ChatSelectionReactionMenuOverlay;
import org.telegram.ui.Components.Reactions.ReactionsEffectOverlay;
import org.telegram.ui.Components.Reactions.ReactionsLayoutInBubble;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.DownscaleScrollableNoiseSuppressor;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundColorProviderThemed;
import org.telegram.ui.Components.blur3.drawable.color.impl.BlurredBackgroundProviderImpl;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceWrapped;
import org.telegram.ui.Components.blur3.utils.Blur3Utils;
import org.telegram.ui.Components.chat.ChatActivityBottomViewsVisibilityController;
import org.telegram.ui.Components.chat.ChatActivityDraftMessageMeasureController;
import org.telegram.ui.Components.chat.ChatActivityMessageMetricsView;
import org.telegram.ui.Components.chat.ChatActivitySearchContainer;
import org.telegram.ui.Components.chat.layouts.ChatActivityActionsButtonsLayout;
import org.telegram.ui.Components.chat.layouts.ChatActivityChannelButtonsLayout;
import org.telegram.ui.Components.chat.ChatInputViewsContainer;
import org.telegram.ui.Components.chat.ChatListViewPaddingsAnimator;
import org.telegram.ui.Components.chat.ViewPositionWatcher;
import org.telegram.ui.Components.chat.WallpaperBitmapProvider;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSource;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceBitmap;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceRenderNode;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.chat.layouts.ChatActivityFadeView;
import org.telegram.ui.Components.chat.layouts.ChatActivitySideControlsButtonsLayout;
import org.telegram.ui.Components.inset.WindowInsetsStateHolder;
import org.telegram.ui.Components.poll.FileState;
import org.telegram.ui.Components.poll.PollAddOptionFieldLayout;
import org.telegram.ui.Components.poll.PollAttachedMediaPack;
import org.telegram.ui.Components.poll.PollSendParams;
import org.telegram.ui.Components.poll.PollUtils;
import org.telegram.ui.Components.poll.sheets.PollStatisticsBottomSheet;
import org.telegram.ui.Components.quickforward.QuickShareSelectorOverlayLayout;
import org.telegram.ui.Components.spoilers.SpoilerEffect;
import org.telegram.ui.Components.voip.CellFlickerDrawable;
import org.telegram.ui.Components.voip.VoIPHelper;
import org.telegram.ui.Delegates.ChatActivityMemberRequestsDelegate;
import org.telegram.ui.Gifts.GiftSheet;
import org.telegram.ui.Stars.StarReactionsOverlay;
import org.telegram.ui.Stars.StarsController;
import org.telegram.ui.Stars.StarsIntroActivity;
import org.telegram.ui.Stars.StarsReactionsSheet;
import org.telegram.ui.Stars.MessageSuggestionOfferSheet;
import org.telegram.messenger.utils.tlutils.AmountUtils;
import org.telegram.ui.Stories.StoriesListPlaceProvider;
import org.telegram.ui.Stories.StoriesUtilities;
import org.telegram.ui.Stories.PublicStoriesList;
import org.telegram.ui.Stories.recorder.HintView2;
import org.telegram.ui.Stories.recorder.PreviewView;
import org.telegram.ui.Stories.recorder.StoryEntry;
import org.telegram.ui.Stories.recorder.StoryRecorder;
import org.telegram.ui.TON.TONIntroActivity;
import org.telegram.ui.bots.BotAdView;
import org.telegram.ui.bots.BotCommandsMenuContainer;
import org.telegram.ui.bots.BotCommandsMenuView;
import org.telegram.ui.bots.BotWebViewSheet;
import org.telegram.ui.bots.WebViewRequestProps;
import org.telegram.ui.community.CommunitySheet;
import org.telegram.ui.iv.BlockRow;
import org.telegram.ui.iv.ChatAttachAlertRichLayout;
import org.telegram.ui.iv.RichEditor;
import org.telegram.ui.iv.RichEditorListView;
import org.telegram.ui.iv.RichHtml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import me.vkryl.android.animator.BoolAnimator;
import me.vkryl.android.animator.FactorAnimator;
import me.vkryl.core.BitwiseUtils;
import me.vkryl.core.reference.ReferenceList;

@SuppressWarnings("unchecked")
public class ChatActivity extends BaseFragment implements
        NotificationCenter.NotificationCenterDelegate,
        DialogsActivity.DialogsActivityDelegate,
        LocationActivity.LocationActivityDelegate,
        ChatAttachAlertDocumentLayout.DocumentSelectActivityDelegate,
        ChatActivityInterface,
        FloatingDebugProvider,
        InstantCameraView.Delegate,
        FactorAnimator.Target
{
    private final static boolean PULL_DOWN_BACK_FRAGMENT = false;
    private final static boolean DISABLE_PROGRESS_VIEW = true;
    private final static int SKELETON_DISAPPEAR_MS = 200;
    public static final int ACTION_BAR_BLUR_ALPHA = 178;

    private static int SKELETON_LIGHT_OVERLAY_ALPHA = 22;
    private static float SKELETON_SATURATION = 1.4f;

    public final static int DEBUG_SHARE_ALERT_MODE_NORMAL = 0,
            DEBUG_SHARE_ALERT_MODE_LESS = 1,
            DEBUG_SHARE_ALERT_MODE_MORE = 2;

    public int shareAlertDebugMode = DEBUG_SHARE_ALERT_MODE_NORMAL;
    public boolean shareAlertDebugTopicsSlowMotion;

    public boolean justCreatedTopic = false;
    public boolean justCreatedChat = false;
    protected TLRPC.Chat currentChat;
    protected TLRPC.User currentUser;
    protected TLRPC.EncryptedChat currentEncryptedChat;
    private boolean userBlocked;

    private long chatInviterId;

    //private static final LongSparseArray<ArrayList<ChatMessageCell>> chatMessageCellsCache = new LongSparseArray<ArrayList<ChatMessageCell>>();

    private HashMap<MessageObject, Boolean> alreadyPlayedStickers = new HashMap<>();

    private final WindowInsetsStateHolder windowInsetsStateHolder = new WindowInsetsStateHolder(this::checkInsets);

    private BlurredBackgroundColorProviderThemed blurredBackgroundColorProvider;
    private BlurredBackgroundColorProviderThemed blurredBackgroundColorProviderWhite;

    private final ReferenceList<View> glassAttachedViews = new ReferenceList<>();
    private final ReferenceList<BlurredBackgroundDrawable> glassAttachedDrawables = new ReferenceList<>();
    private final @Nullable DownscaleScrollableNoiseSuppressor scrollableViewNoiseSuppressor;
    private final int recommendedAdditionalSizeY;

    private final @Nullable BlurredBackgroundSourceRenderNode glassBackgroundSourceRenderNode;
    private final @Nullable BlurredBackgroundSourceRenderNode glassBackgroundSourceFrostedRenderNode;
    private final @NonNull BlurredBackgroundDrawableViewFactory glassBackgroundDrawableFactory;
    private final @NonNull BlurredBackgroundDrawableViewFactory glassBackgroundDrawableFactoryFrosted;

    private final @NonNull BlurredBackgroundSourceWrapped navbarContentSourceWallpaper;
    private final @NonNull BlurredBackgroundDrawableViewFactory navbarContentDrawableFactory;

    private Dialog closeChatDialog;
    private boolean showCloseChatDialogLater;
    private FrameLayout progressView;
    private View progressView2;
    private FrameLayout bottomOverlay;
    public ChatInputViewsContainer chatInputViewsContainer;
    private View roundVideoRecordBackground;

    private FrameLayout chatInputBubbleContainer;
    private FrameLayout chatInputInAppContainer;
    private WallpaperBitmapProvider wallpaperBitmapProvider = new WallpaperBitmapProvider();

    private ChatActivityFadeView chatActivityFadeView;
    protected ChatActivityEnterView chatActivityEnterView;
    private ChatActivityEnterTopView chatActivityEnterTopView;
    private ChatReplyContainer replyLayout;
    private int chatActivityEnterViewAnimateFromTop;
    private boolean chatActivityEnterViewAnimateBeforeSending;
    private ActionBarMenuItem.Item timeItem2;
    private ComposeDrawable otherIcon;
    private ActionBarMenu.LazyItem attachItem;
    private ActionBarMenuItem.Item savedChatsItem, savedChatsGap;;
    private ActionBarMenuItem headerItem;
    private ActionBarMenu.LazyItem editTextItem;
    protected ActionBarMenuItem searchItem;
    protected ActionBarMenuItem topicCreateItem;
    private ActionBarMenuItem.Item translateItem;
    private ActionBarMenuItem searchIconItem;
    private ActionBarMenu.LazyItem audioCallIconItem;
    private boolean searchItemVisible;
    private RadialProgressView progressBar;
    private ActionBarMenuItem.Item addContactItem;
    private ActionBarMenuItem.Item clearHistoryItem;
    private ActionBarMenuItem.Item viewAsTopics;
    private ActionBarMenuItem.Item closeTopicItem;
    private ActionBarMenuItem.Item openForumItem;
    private ClippingImageView animatingImageView;
    private ThanosEffect chatListThanosEffect;
    private ChatListViewPaddingsAnimator chatListViewPaddingsAnimator;
    private ChatListRecyclerView chatListView;
    private ChatListItemAnimator chatListItemAnimator;
    private GridLayoutManagerFixed chatLayoutManager;
    private ChatActivityAdapter chatAdapter;
    private UnreadCounterTextView bottomOverlayChatText;
    private boolean bottomOverlayLinks;
    private LinkSpanDrawable.LinksTextView bottomOverlayLinksText;
    private TextView bottomOverlayText;
    private TextView bottomOverlayStartButton;
    private RadialProgressView bottomOverlayProgress;
    private AnimatorSet bottomOverlayAnimation;
    private boolean bottomOverlayChatWaitsReply;
    private HintView2 bottomGiftHintView;
    private HintView2 guestBotHintView;
    private HintView2 bottomSuggestHintView;
    private ChatActivityTopPanelLayout topPanelLayout;

    private boolean ignoreItemAnimation;
    private ChatActivityChannelButtonsLayout bottomChannelButtonsLayout;
    private ChatActivityActionsButtonsLayout actionsButtonsLayout;
    @Nullable
    private FrameLayout emptyViewContainer;
    private LinearLayout emptyViewContent;
    private ChatGreetingsView greetingsViewContainer;
    private ChatActionCell greetingsInfo;
    private QuickRepliesEmptyView quickRepliesEmptyView;
    private BusinessLinksEmptyView businessLinksEmptyView;
    private ViewPositionWatcher viewPositionWatcher;
    public ChatActivityFragmentView contentView;
    private ChatBigEmptyView bigEmptyView;
    private ArrayList<View> actionModeViews = new ArrayList<>();
    public ChatAvatarContainer avatarContainer;
    private AnimatedTextView selectedMessagesCountTextView;
    private RecyclerListView.OnItemClickListener mentionsOnItemClickListener;
    private SuggestEmojiView suggestEmojiPanel;
    private ActionBarMenuItem.Item muteItem;
    private ActionBarMenuItem.Item muteItemGap;
    private ActionBarMenuItem.Item feeItemGap;
    private ActionBarMenuItem.Item feeItemText;
    private ChatNotificationsPopupWrapper chatNotificationsPopupWrapper;
    // private ChatActivitySideControlsButtonsLayout topButtonsLayout;
    private ChatActivitySideControlsButtonsLayout sideControlsButtonsLayout;
    private boolean pagedownButtonShowedByScroll;
    private int reactionsMentionCount;
    private int pollVotesMentionCount;
    public Bulletin messageSeenPrivacyBulletin;
    TextView webBotTitle;
    public SearchTagsList actionBarSearchTags;
    public ChatSearchTabs hashtagSearchTabs;
    private ViewPagerFixed searchViewPager;
    private int defaultSearchPage;
    private boolean requestClearSearchPages;
    private HashtagHistoryView hashtagHistoryView;
    private AlertDialog scheduleNowDialog;

    private HintView2 savedMessagesHint;
	private HintView2 savedMessagesSearchHint;
    private HintView2 savedMessagesTagHint;
    private HintView2 groupEmojiPackHint;
    private HintView2 botMessageHint;
    private HintView2 factCheckHint;
    private HintView2 videoConversionTimeHint;
    private float videoConversionTimeHintY;

    private TL_stories.TL_premium_boostsStatus boostsStatus;
    private ChannelBoostsController.CanApplyBoost canApplyBoosts;

    private boolean showTapForForwardingOptionsHit;
    private Runnable tapForForwardingOptionsHitRunnable;
    private ImageView replyCloseImageView;
    private MentionsContainerView mentionContainer;
    private AnimatorSet mentionListAnimation;
    public ChatAttachAlert chatAttachAlert;
    @Nullable
    private FrameLayout topChatPanelView;
    @Nullable
    private TextView addToContactsButton;
    private boolean addToContactsButtonArchive;
    @Nullable
    private TextView reportSpamButton;
    @Nullable
    private TextView restartTopicButton;
    @Nullable
    private TranslateButton translateButton;
    private TextView addProfilePictureButton;
    public TopicsTabsView topicsTabs;
    @Nullable
    private BusinessBotButton bizBotButton;
    @Nullable
    private LinkSpanDrawable.LinksTextView emojiStatusSpamHint;
    @Nullable
    private ImageView closeReportSpam;
    private BotAdView botAdView;
    private TextView chatWithAdminTextView;
    private FragmentContextView fragmentContextView;
    private FrameLayout fragmentContextViewWrapper;
    private FragmentContextView fragmentLocationContextView;
    private FrameLayout fragmentLocationContextViewWrapper;
    private TextView emptyView;
    private FlickerLoadingView hashtagLoadingView;
    private StickerEmptyView hashtagSearchEmptyView;
    private HintView gifHintTextView;
    private HintView emojiHintTextView;
    private HintView mediaBanTooltip;
    private HintView scheduledOrNoSoundHint;
    private boolean scheduledOrNoSoundHintShown;
    private HintView scheduledHint;
    private boolean scheduledHintShown;
    private boolean searchAsListHintShown;
    private HintView fwdRestrictedTopHint;
    private HintView fwdRestrictedBottomHint;
    private HintView slowModeHint;
    private HintView pollHintView;
    private HintView timerHintView;
    private ChatMessageCell pollHintCell;
    private int pollHintX;
    private int pollHintY;
    private HintView voiceHintTextView;
    private HintView noSoundHintView;
    private HintView forwardHintView;
    private ChecksHintView checksHintView;
    private View emojiButtonRed;
    private FrameLayout pinnedMessageView;
    private BluredView blurredView;
    private PinnedLineView pinnedLineView;
    private boolean setPinnedTextTranslationX;
    private BackupImageView[] pinnedMessageImageView = new BackupImageView[2];
    private TrackingWidthSimpleTextView[] pinnedNameTextView = new TrackingWidthSimpleTextView[2];
    private SimpleTextView[] pinnedMessageTextView = new SimpleTextView[2];
    private PinnedMessageButton[] pinnedMessageButton = new PinnedMessageButton[2];
    private NumberTextView pinnedCounterTextView;
    private int pinnedCounterTextViewX;
    private AnimatorSet[] pinnedNextAnimation = new AnimatorSet[2];
    private boolean pinnedMessageButtonShown = false;
    private ImageView closePinned;
    private RadialProgressView pinnedProgress;
    private ImageView pinnedListButton;
    private AnimatorSet pinnedListAnimator;
    @Nullable
    private FrameLayout alertView;
    private Runnable hideAlertViewRunnable;
    private TextView alertNameTextView;
    private TextView alertTextView;
    private final int searchContainerHeight = 44;
    private FrameLayout searchContainer;
    private ImageView searchCalendarButton;
    private ImageView searchUserButton;
    private AnimatedTextView searchCountText;
    private AnimatedTextView searchExpandList;
    private AnimatedTextView searchOtherButton;
    private ChatActionCell floatingDateView;
    private TopicSeparator.Cell floatingTopicSeparator;
    private float intoTopViewTop;
    private ChatActionCell infoTopView;
    private int hideDateDelay = 500;
    public InstantCameraView instantCameraView;
    private View overlayView;
    private boolean currentFloatingDateOnScreen;
    private boolean currentFloatingTopicOnScreen;
    private boolean currentFloatingTopIsNotMessage;
    private AnimatorSet floatingDateAnimation;
    private ValueAnimator floatingTopicAnimation;
    private float floatingTopicViewAlpha;
    private boolean scrollingFloatingDate;
    private boolean scrollingFloatingTopic;
    private boolean scrollingChatListView;
    private boolean checkTextureViewPosition;
    private boolean searchingForUser;
    private TLRPC.User searchingUserMessages;
    private TLRPC.Chat searchingChatMessages;
    public static boolean scrolling;
    public ReactionsLayoutInBubble.VisibleReaction searchingReaction;
    public ReactionsLayoutInBubble.VisibleReaction getFilterTag() {
        return chatAdapter != null && chatAdapter.isFiltered ? searchingReaction : null;
    }
    public String getFilterQuery() {
        return chatAdapter != null && chatAdapter.isFiltered ? searchingQuery : null;
    }
    public boolean isFiltered() {
        return chatAdapter != null && chatAdapter.isFiltered;
    }
    public ArrayList<MessageObject> getFilteredMessages() {
        return chatAdapter != null ? chatAdapter.filteredMessages : null;
    }
    private boolean searchingFiltered;
    private boolean searching;
    private String searchingQuery;
    private String searchingHashtag;
    private int hashtagSearchSelectedIndex;
    private int searchLastCount;
    private int searchLastIndex;
    private UndoView undoView;
    private UndoView topUndoView;
    private Bulletin pinBulletin;
    private boolean showPinBulletin;
    private int pinBullerinTag;
    protected boolean openKeyboardOnAttachMenuClose;
    private FlagSecureReason flagSecure;
    public boolean isFullyVisible;

    private MessageObject hintMessageObject;
    private int hintMessageType;
    private MessageObject hint2MessageObject;
    private MessageObject hint3MessageObject;

    private ChatActivitySearchContainer messagesSearchListContainer;
    public RecyclerListView messagesSearchListView;
    private MessagesSearchAdapter messagesSearchAdapter;
    private ChatActivityMessageMetricsView messageMetricsView;

    public static final int MODE_DEFAULT = 0;
    public static final int MODE_SCHEDULED = 1;
    public static final int MODE_PINNED = 2;
    public static final int MODE_SAVED = 3;
    public static final int MODE_QUICK_REPLIES = 5;
    public static final int MODE_EDIT_BUSINESS_LINK = 6;
    public static final int MODE_SEARCH = 7;
    public static final int MODE_SUGGESTIONS = 8;
    public static final int MODE_WELCOME_MESSAGES = 9;

    public static final int SEARCH_THIS_CHAT = 0;
    public static final int SEARCH_MY_MESSAGES = 1;
    public static final int SEARCH_PUBLIC_POSTS = 2;
    public static final int SEARCH_CHANNEL_POSTS = 3;
    private int searchType;

    public TL_account.TL_businessChatLink businessLink = null;

    public String quickReplyShortcut;
    private int chatMode;
    private int scheduledMessagesCount = -1;
    public boolean isSubscriberSuggestions;

    private String reportTitle;
    private byte[] reportOption;
    private String reportMessage;
    public boolean isReport() {
        return !TextUtils.isEmpty(reportTitle);
    }

    @Nullable
    private MessageObject threadMessageObject;
    private MessageObject topicStarterMessageObject;
    private boolean threadMessageVisible = true;
    private ArrayList<MessageObject> threadMessageObjects;
    private MessageObject replyMessageHeaderObject;
    private TLRPC.TL_forumTopic forumTopic;
    private long threadMessageId;
    private int replyOriginalMessageId;
    public TLRPC.Chat replyOriginalChat;
    public boolean isComments;
    public boolean isTopic;
    private boolean threadMessageAdded;
    private boolean scrollToThreadMessage;
    private int threadMaxInboxReadId;
    private int threadMaxOutboxReadId;
    private int replyMaxReadId;
    private Runnable delayedReadRunnable;
    private final SparseArray<MessageObject> pendingSendMessagesDict = new SparseArray<>();
    private final ArrayList<MessageObject> pendingSendMessages = new ArrayList<>();
    private int threadUnreadMessagesCount;
    private boolean convertingToast, convertingToastShown;
    private int convertingToastMessageId;

    public ArrayList<MessageObject> animatingMessageObjects = new ArrayList<>();
    private final HashMap<TLRPC.Document, Integer> animatingDocuments = new HashMap<>();
    private MessageObject needAnimateToMessage;

    private int scrollToPositionOnRecreate = -1;
    private int scrollToOffsetOnRecreate = 0;

    private final ArrayList<MessageObject> pollsToCheck = new ArrayList<>(10);

    private int editTextStart;
    private int editTextEnd;

    private Runnable checkPaddingsRunnable;

    private boolean wasManualScroll;
    private boolean fixPaddingsInLayout;
    private boolean globalIgnoreLayout;

    private int topViewWasVisible;

    private ArrayList<Integer> pinnedMessageIds = new ArrayList<>();
    private int maxPinnedMessageId;
    private HashMap<Integer, MessageObject> pinnedMessageObjects = new HashMap<>();
    private SparseArray<Boolean> loadingPinnedMessages = new SparseArray<>();
    private int currentPinnedMessageId;
    private int[] currentPinnedMessageIndex = new int[1];
    private int forceNextPinnedMessageId;
    private boolean forceScrollToFirst;
    private int loadedPinnedMessagesCount;
    private int totalPinnedMessagesCount;
    public boolean loadingPinnedMessagesList;
    private boolean pinnedEndReached;

    public void reloadPinnedMessages() {
        pinnedMessageIds.clear();
        pinnedMessageObjects.clear();
        currentPinnedMessageId = 0;
        loadedPinnedMessagesCount = 0;
        totalPinnedMessagesCount = 0;
        updatePinnedMessageView(true);
        getMediaDataController().loadPinnedMessages(getDialogId(), 0, chatInfo == null ? 0 : chatInfo.pinned_msg_id);
        loadingPinnedMessagesList = true;
        updatePinnedTopicStarterMessage();
    }

    private AnimatorSet forwardButtonAnimation;

    SparseIntArray dateObjectsStableIds = new SparseIntArray();
    SparseIntArray conversionObjectsStableIds = new SparseIntArray();
    public static int lastStableId = 10;

    private boolean openSearchKeyboard;

    private boolean waitingForReplyMessageLoad;

    private boolean ignoreAttachOnPause;

    private boolean allowStickersPanel = true;
    private boolean allowContextBotPanel;
    private boolean allowContextBotPanelSecond = true;
    private AnimatorSet runningAnimation;
    private int runningAnimationIndex = -1;

    private MessageObject selectedObjectToEditCaption;
    private MessageObject selectedObject;
    private MessageObject.GroupedMessages selectedObjectGroup;
    private boolean forbidForwardingWithDismiss;
    public MessagePreviewParams messagePreviewParams;
    public MessageSuggestionParams messageSuggestionParams;
    private CharSequence formwardingNameText;
    private MessageObject forwardingMessage;
    private MessageObject.GroupedMessages forwardingMessageGroup;
    private MessageObject.GroupedMessages replyingQuoteGroup;
    public MessageObject replyingTopMessage;
    private ReplyQuote replyingQuote;
    private boolean ignoreDraft;
    private MessageObject replyingMessageObject;
    private int editingMessageObjectReqId;
    public MessageObject editingMessageObject;
    private boolean paused = true;
    private boolean pausedOnLastMessage;
    private boolean wasPaused;
    boolean firstOpen = true;
    private int replyImageSize;
    private int replyImageCacheType;
    private TLRPC.PhotoSize replyImageLocation;
    private TLRPC.PhotoSize replyImageThumbLocation;
    private TLObject replyImageLocationObject;
    private int pinnedImageSize;
    private int pinnedImageCacheType;
    private boolean pinnedImageHasBlur;
    private TLRPC.PhotoSize pinnedImageLocation;
    private TLRPC.PhotoSize pinnedImageThumbLocation;
    private TLObject pinnedImageLocationObject;
    private int linkSearchRequestId;
    public TLRPC.WebPage foundWebPage;
    private ArrayList<CharSequence> foundUrls;
    private String pendingLinkSearchString;
    private Runnable pendingWebPageTimeoutRunnable;
    private Runnable waitingForCharaterEnterRunnable;
    private Runnable onChatMessagesLoaded;

    private TLRPC.ChatInvite chatInvite;
    private Runnable chatInviteRunnable;

    private LongSparseIntArray clearingHistoryArr = new LongSparseIntArray();
    boolean isClearingHistory() {
        return clearingHistoryArr.get(getThreadId(), 0) != 0;
    }

    void setClearingHistory(long threadId, boolean isClearingHistory) {
        clearingHistoryArr.put(threadId, isClearingHistory ? 1 : 0);
    }

    public boolean openAnimationEnded;
    public boolean fragmentOpened;
    private long openAnimationStartTime;

    private boolean scrollToTopOnResume;
    private boolean forceScrollToTop;
    private boolean scrollToTopUnReadOnResume;
    private long dialog_id;
    private Long dialog_id_Long;
    private int lastLoadIndex = 1;
    private SparseArray<MessageObject>[] selectedMessagesIds = new SparseArray[]{new SparseArray<>(), new SparseArray<>()};
    private SparseArray<MessageObject>[] selectedMessagesCanCopyIds = new SparseArray[]{new SparseArray<>(), new SparseArray<>()};
    private SparseArray<MessageObject>[] selectedMessagesCanStarIds = new SparseArray[]{new SparseArray<>(), new SparseArray<>()};
    private boolean hasUnfavedSelected;
    private int cantDeleteMessagesCount;
    private int cantForwardMessagesCount;
    private int canForwardMessagesCount;
    private int canEditMessagesCount;
    private int cantSaveMessagesCount;
    private int canSaveMusicCount;
    private int canSaveDocumentsCount;
    private ArrayList<Integer> waitingForLoad = new ArrayList<>();
    private boolean needRemovePreviousSameChatActivity = true;

    private int newUnreadMessageCount;
    private int prevSetUnreadCount = Integer.MIN_VALUE;
    private int newMentionsCount;
    private boolean hasAllMentionsLocal;

    private ArrayList<ChatMessageCell> animateSendingViews = new ArrayList<>();

    private SparseArray<MessageObject>[] messagesDict = new SparseArray[]{new SparseArray<>(), new SparseArray<>()};
    private SparseArray<MessageObject> repliesMessagesDict = new SparseArray<>();
    private SparseArray<ArrayList<Integer>> replyMessageOwners = new SparseArray<>();
    private HashMap<String, ArrayList<MessageObject>> messagesByDays = new HashMap<>();
    private SparseArray<ArrayList<MessageObject>> messagesByDaysSorted = new SparseArray<>();
    private LongSparseArray<MessageObject> conversionMessages = new LongSparseArray<>();
    public ArrayList<MessageObject> messages = new ArrayList<>();
    private SparseArray<MessageObject> waitingForReplies = new SparseArray<>();
    private LongSparseArray<ArrayList<MessageObject>> polls = new LongSparseArray<>();
    private LongSparseArray<MessageObject.GroupedMessages> groupedMessagesMap = new LongSparseArray<>();
    private int[] maxMessageId = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE};
    private int[] minMessageId = new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE};
    private int[] maxDate = new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE};
    private int[] minDate = new int[2];
    private boolean[] endReached = new boolean[2];
    private boolean[] cacheEndReached = new boolean[2];
    private boolean[] forwardEndReached = new boolean[]{true, true};
    private boolean hideForwardEndReached;
    private boolean loading = true;
    private boolean firstLoading = true;
    private boolean chatWasReset;
    private boolean firstUnreadSent;
    private int loadsCount;
    private int last_message_id = 0;
    private long mergeDialogId;
    private boolean sentBotStart;

    private long startMessageAppearTransitionMs;
    private List<MessageSkeleton> messageSkeletons = new ArrayList<>();
    private int lastSkeletonCount;
    private int lastSkeletonMessageCount;
    private Paint skeletonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint skeletonServicePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ColorMatrix skeletonColorMatrix = new ColorMatrix();
    private MessageDrawable.PathDrawParams skeletonBackgroundCacheParams = new MessageDrawable.PathDrawParams();
    private MessageDrawable skeletonBackgroundDrawable = new MessageDrawable(MessageDrawable.TYPE_TEXT, false, false, new Theme.ResourcesProvider() {
        @Override
        public int getColor(int key) {
            return getThemedColor(key);
        }
    });
    private long skeletonLastUpdateTime;
    private int skeletonGradientWidth;
    private int skeletonTotalTranslation;
    private Matrix skeletonMatrix = new Matrix();
    private LinearGradient skeletonGradient;
    private int skeletonColor0;
    private int skeletonColor1;

    private Paint skeletonOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Matrix skeletonOutlineMatrix = new Matrix();
    private LinearGradient skeletonOutlineGradient;
    public boolean forceDisallowApplyWallpeper;
    public boolean forceDisallowRedrawThemeDescriptions;
    private boolean waitingForGetDifference;
    private int initialMessagesSize;
    private boolean loadInfo;
    private boolean historyPreloaded;
    private int migrated_to;
    private boolean firstMessagesLoaded;
    private boolean clearOnLoad;
    private boolean clearOnLoadButIsNewTopic;
    private int clearOnLoadAndScrollMessageId = -1, clearOnLoadAndScrollOffset;
    private boolean topicChangedFromMessage;
    private Runnable closeInstantCameraAnimation;

    {
        skeletonOutlinePaint.setStyle(Paint.Style.STROKE);
        skeletonOutlinePaint.setStrokeWidth(AndroidUtilities.dp(1));
    }

    private String textToSet;
    private boolean premiumInvoiceBot;
    private boolean showScrollToMessageError;
    private int startLoadFromMessageId;
    private int startReplyTo;
    private int startLoadFromDate;
    private int startLoadFromMessageIdSaved;
    private int startLoadFromMessageOffset = Integer.MAX_VALUE;
    private int startFromVideoTimestamp = -1;
    private int startFromVideoMessageId;
    private boolean needSelectFromMessageId;
    private int returnToMessageId;
    private int returnToLoadIndex;
    private int createUnreadMessageAfterId;
    private boolean createUnreadMessageAfterIdLoading;
    private boolean loadingFromOldPosition;

    private boolean first = true;
    private int first_unread_id;
    private boolean loadingForward;
    private MessageObject unreadMessageObject;
    private MessageObject scrollToMessage;
    public int highlightMessageId = Integer.MAX_VALUE;
    public boolean showNoQuoteAlert;
    public boolean highlightMessageQuoteFirst;
    private long highlightMessageQuoteFirstTime;
    public String highlightMessageQuote;
    public Integer highlightTaskId;
    public byte[] highlightPollOptionId;
    public int highlightMessageQuoteOffset = -1;
    private int scrollToMessagePosition = -10000;
    private Runnable unselectRunnable;

    private String currentPicturePath;

    private ChatObject.Call groupCall;
    private boolean lastCallCheckFromServer;
    private boolean createGroupCall;
    protected TLRPC.ChatFull chatInfo;
    protected TLRPC.UserFull userInfo;

    public ProfileChannelCell.ChannelMessageFetcher profileChannelMessageFetcher;
    public ProfileBirthdayEffect.BirthdayEffectFetcher birthdayAssetsFetcher;

    public final LongSparseArray<TL_bots.BotInfo> botInfo = new LongSparseArray<>();
    private String botUser;
    private long inlineReturn;
    private String voiceChatHash;
    private boolean openVideoChat;
    private boolean livestream;
    private String attachMenuBotToOpen;
    private String attachMenuBotStartCommand;
    private MessageObject botButtons;
    private MessageObject botReplyButtons;
    private int botsCount;
    private boolean hasBotsCommands;
    private boolean hasQuickReplies;
    private boolean hasBotWebView;
    private long chatEnterTime;
    private long chatLeaveTime;

    private boolean locationAlertShown;

    private String startVideoEdit;

    private FrameLayout videoPlayerContainer;
    private ChatMessageCell drawLaterRoundProgressCell;
    private AspectRatioFrameLayout aspectRatioFrameLayout;
    private TextureView videoTextureView;
    private boolean scrollToVideo;
    private Path aspectPath;
    private Paint aspectPaint;
    private Runnable destroyTextureViewRunnable = () -> {
        destroyTextureView();
    };

    private final BlurredBackgroundSourceBitmap scrimBlur3SourceBitmap = new BlurredBackgroundSourceBitmap();
    private final BlurredBackgroundDrawableViewFactory scrimBlur3Factory = new BlurredBackgroundDrawableViewFactory(scrimBlur3SourceBitmap);
    private Bitmap scrimBlurBitmap;
    private BitmapShader scrimBlurBitmapShader;
    private Paint scrimBlurBitmapPaint;
    private Matrix scrimBlurMatrix;

    private Paint scrimPaint;
    private Paint actionBarBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float scrimPaintAlpha = 0f;
    private boolean scrimProgressDirection;
    private View scrimView;
    private float scrimViewAlpha = 1f;
    private float scrimViewProgress = 0f;
    private Integer scrimViewReaction;
    private Integer scrimViewTask;
    private int scrimViewReactionOffset;
    private boolean scrimViewReactionAnimated;
    private int popupAnimationIndex = -1;
    private AnimatorSet scrimAnimatorSet;
    public ActionBarPopupWindow scrimPopupWindow;
    private boolean scrimPopupWindowHideDimOnDismiss = true;
    private int scrimPopupX, scrimPopupY;
    private ActionBarMenuSubItem[] scrimPopupWindowItems;
    private ActionBarMenuSubItem menuDeleteItem;
    private final Runnable updateDeleteItemRunnable = new Runnable() {
        @Override
        public void run() {
            if (selectedObject == null || menuDeleteItem == null) {
                return;
            }
            int remaining = Math.max(0, selectedObject.messageOwner.ttl_period - (getConnectionsManager().getCurrentTime() - selectedObject.messageOwner.date));
            String remainingStr;
            if (remaining < 24 * 60 * 60) {
                remainingStr = AndroidUtilities.formatDuration(remaining, false, true);
            } else {
                remainingStr = LocaleController.formatPluralString("Days", Math.round(remaining / (24 * 60 * 60.0f)));
            }
            menuDeleteItem.setSubtext(LocaleController.formatString(R.string.AutoDeleteIn, remainingStr));
            AndroidUtilities.runOnUIThread(updateDeleteItemRunnable, 1000);
        }
    };

    private ChatActivityDelegate chatActivityDelegate;
    private RecyclerAnimationScrollHelper chatScrollHelper;

    private int postponedScrollMinMessageId;
    private int postponedScrollToLastMessageQueryIndex;
    private int postponedScrollMessageId;
    private boolean fakePostponedScroll;
    private boolean postponedScrollIsCanceled;
    private ChatActivityTextSelectionHelper textSelectionHelper;
    private View slidingView;

    private final float[] tmpOverlayPos = new float[2];
    private float[] computeArticleOverlayPos(View overlay) {
        tmpOverlayPos[0] = 0;
        tmpOverlayPos[1] = 0;
        View v = overlay;
        while (v != null && v != contentView) {
            tmpOverlayPos[0] += v.getX();
            tmpOverlayPos[1] += v.getY();
            v = v.getParent() instanceof View ? (View) v.getParent() : null;
        }
        return tmpOverlayPos;
    }

    public boolean hasTextSelection() {
        return textSelectionHelper != null && textSelectionHelper.isInSelectionMode();
    }

    public boolean isTryingTextSelection() {
        return textSelectionHelper != null && textSelectionHelper.isTryingSelect();
    }

    public void clearTextSelection() {
        if (textSelectionHelper != null && textSelectionHelper.isInSelectionMode()) textSelectionHelper.clear();
    }

    private MessageObject getSlidingMessageObject() {
        if (slidingView instanceof ChatMessageCell) return ((ChatMessageCell) slidingView).getMessageObject();
        return null;
    }

    private float getSlidingNonAnimationTranslationX(boolean update) {
        if (slidingView instanceof ChatMessageCell) return ((ChatMessageCell) slidingView).getNonAnimationTranslationX(update);
        return 0;
    }

    private void slidingViewSetOffset(float offset) {
        if (slidingView instanceof ChatMessageCell) ((ChatMessageCell) slidingView).setSlidingOffset(offset);
    }

    private float slidingViewGetOffsetX() {
        if (slidingView instanceof ChatMessageCell) return ((ChatMessageCell) slidingView).getSlidingOffsetX();
        return 0;
    }
    private boolean maybeStartTrackingSlidingView;
    private boolean startedTrackingSlidingView;

    private boolean canShowPagedownButton;
    private TextSelectionHint textSelectionHint;
    private boolean textSelectionHintWasShowed;
    private float lastTouchY;
    ContentPreviewViewer.ContentPreviewViewerDelegate contentPreviewViewerDelegate;

    private ChatMessageCell dummyMessageCell;
    protected FireworksOverlay fireworksOverlay;

    private boolean swipeBackEnabled = true;

    public static Pattern publicMsgUrlPattern;
    public static Pattern voiceChatUrlPattern;
    public static Pattern privateMsgUrlPattern;
    private boolean waitingForSendingMessageLoad;
    private ValueAnimator changeBoundAnimator;
    private Animator messageEditTextAnimator;

    private boolean openImport;

    public float chatListViewPaddingTop;
    public float paddingTopHeight;
    public int chatListViewPaddingVisibleOffset;

    private int contentPaddingTop;
    private float contentPanTranslation;
    private float contentPanTranslationT;
    private float floatingDateViewOffset;
    private float floatingTopicViewOffset;
    private float topViewOffset;
    private TLRPC.Document preloadedGreetingsSticker;
    private boolean forceHistoryEmpty;
    private boolean invalidateChatListViewTopPadding;
    private long activityResumeTime;

    private int transitionAnimationIndex;
    private int transitionAnimationGlobalIndex;
    private int scrollAnimationIndex;
    private int scrollCallbackAnimationIndex;

    public boolean allowExpandPreviewByClick;
    private boolean showSearchAsIcon;
    private boolean showAudioCallAsIcon;
    public MessageEnterTransitionContainer messageEnterTransitionContainer;
    private float pullingDownOffset, pullingBottomOffset;
    private ChatPullingDownDrawable pullingDownDrawable;
    private Animator pullingDownBackAnimator;
    private boolean fromPullingDownTransition;
    private boolean toPullingDownTransition;
    private ChatActivity pullingDownAnimateToActivity;
    private float pullingDownAnimateProgress;
    private AnimatorSet fragmentTransition;
    private ChatActivity backToPreviousFragment;
    private Runnable fragmentTransitionRunnable = new Runnable() {
        @Override
        public void run() {
            if (fragmentTransition != null && !fragmentTransition.isRunning()) {
                fragmentTransition.start();
            }
        }
    };

    private QuickShareSelectorOverlayLayout quickShareSelectorOverlay;
    private ChatSelectionReactionMenuOverlay selectionReactionsOverlay;
    private SecretVoicePlayer secretVoicePlayer;

    private boolean isPauseOnThemePreview;
    private ChatThemeBottomSheet chatThemeBottomSheet;
    private ThemeDelegate parentThemeDelegate;
    private ChatActivity parentChatActivity;
    public ThemeDelegate themeDelegate;
    private ChatActivityMemberRequestsDelegate pendingRequestsDelegate;
    private final ChatMessagesMetadataController chatMessagesMetadataController = new ChatMessagesMetadataController(this);
    private TLRPC.TL_channels_sendAsPeers sendAsPeersObj;

    private TL_account.resolvedBusinessChatLinks resolvedChatLink;

    private boolean switchFromTopics;
    private boolean switchingFromTopics;
    private float switchingFromTopicsProgress;

    public final static int OPTION_RETRY = 0;
    public final static int OPTION_DELETE = 1;
    public final static int OPTION_FORWARD = 2;
    public final static int OPTION_COPY = 3;
    public final static int OPTION_SAVE_TO_GALLERY = 4;
    public final static int OPTION_APPLY_LOCALIZATION_OR_THEME = 5;
    public final static int OPTION_SHARE = 6;
    public final static int OPTION_SAVE_TO_GALLERY2 = 7;
    public final static int OPTION_REPLY = 8;
    public final static int OPTION_ADD_TO_STICKERS_OR_MASKS = 9;
    public final static int OPTION_SAVE_TO_DOWNLOADS_OR_MUSIC = 10;
    public final static int OPTION_ADD_TO_GIFS = 11;
    public final static int OPTION_EDIT = 12;
    public final static int OPTION_PIN = 13;
    public final static int OPTION_UNPIN = 14;
    public final static int OPTION_ADD_CONTACT = 15;
    public final static int OPTION_COPY_PHONE_NUMBER = 16;
    public final static int OPTION_CALL = 17;
    public final static int OPTION_CALL_AGAIN = 18;
    public final static int OPTION_RATE_CALL = 19;
    public final static int OPTION_ADD_STICKER_TO_FAVORITES = 20;
    public final static int OPTION_DELETE_STICKER_FROM_FAVORITES = 21;
    public final static int OPTION_COPY_LINK = 22;
    public final static int OPTION_REPORT_CHAT = 23;
    public final static int OPTION_CANCEL_SENDING = 24;
    public final static int OPTION_UNVOTE = 25;
    public final static int OPTION_STOP_POLL_OR_QUIZ = 26;
    public final static int OPTION_VIEW_REPLIES_OR_THREAD = 27;
    public final static int OPTION_STATISTICS = 28;
    public final static int OPTION_TRANSLATE = 29;
    public final static int OPTION_TRANSCRIBE = 30;
    public final static int OPTION_HIDE_SPONSORED_MESSAGE = 31;
    public final static int OPTION_VIEW_IN_TOPIC = 32;
    public final static int OPTION_ABOUT_REVENUE_SHARING_ADS = 33;
    public final static int OPTION_REPORT_AD = 34;
    public final static int OPTION_REMOVE_ADS = 35;
    public final static int OPTION_SEND_NOW = 100;
    public final static int OPTION_EDIT_SCHEDULE_TIME = 102;
    public final static int OPTION_SPEED_PROMO = 103;
    public final static int OPTION_OPEN_PROFILE = 104;
    public final static int OPTION_FACT_CHECK = 106;
    public final static int OPTION_EDIT_PRICE = 107;
    public final static int OPTION_GIFT = 108;
    public final static int OPTION_EDIT_TODO = 109;
    public final static int OPTION_ADD_TO_TODO = 110;

    public final static int OPTION_SUGGESTION_EDIT_PRICE = 111;
    public final static int OPTION_SUGGESTION_EDIT_TIME = 112;
    public final static int OPTION_SUGGESTION_EDIT_MESSAGE = 113;
    public final static int OPTION_SUGGESTION_ADD_OFFER = 114;

    public final static int OPTION_VIEW_STATISTICS = 115;
    public final static int OPTION_WELCOME_REVERT = 116;

    private final static int[] allowedNotificationsDuringChatListAnimations = new int[]{
            NotificationCenter.messagesRead,
            NotificationCenter.threadMessagesRead,
            NotificationCenter.monoForumMessagesRead,
            NotificationCenter.commentsRead,
            NotificationCenter.messagesReadEncrypted,
            NotificationCenter.messagesReadContent,
            NotificationCenter.didLoadPinnedMessages,
            NotificationCenter.newDraftReceived,
            NotificationCenter.updateMentionsCount,
            NotificationCenter.didUpdateConnectionState,
            //NotificationCenter.updateInterfaces,
            NotificationCenter.updateDefaultSendAsPeer,
            NotificationCenter.closeChats,
            NotificationCenter.chatInfoCantLoad,
            NotificationCenter.userInfoDidLoad,
            NotificationCenter.pinnedInfoDidLoad,
            NotificationCenter.didSetNewWallpapper,
            NotificationCenter.savedMessagesDialogsUpdate,
            NotificationCenter.didApplyNewTheme,
            NotificationCenter.messageReceivedByServer2
    };

    private final DialogInterface.OnCancelListener postponedScrollCancelListener = dialog -> {
        postponedScrollIsCanceled = true;
        postponedScrollMessageId = 0;
        nextScrollToMessageId = 0;
        forceNextPinnedMessageId = 0;
        invalidateMessagesVisiblePart();
        showPinnedProgress(false);
    };

    private NotificationCenter.PostponeNotificationCallback postponeNotificationsWhileLoadingCallback = new NotificationCenter.PostponeNotificationCallback() {
        @Override
        public boolean needPostpone(int id, int currentAccount, Object[] args) {
            if (id == NotificationCenter.didReceiveNewMessages) {
                long did = (Long) args[0];
                if (firstLoading && did == dialog_id) {
                    return true;
                }
            }
            return false;
        }
    };
    private int chatEmojiViewPadding;
    private int fixedKeyboardHeight = -1;
    private Runnable cancelFixedPositionRunnable;
    private boolean invalidateMessagesVisiblePart;
    private boolean scrollByTouch;
    private long welcomeMessagesChatId;
    int dialogFolderId;
    int dialogFilterId;
    boolean pulled = false;
    private static boolean replacingChatActivity = false;

    private PinchToZoomHelper pinchToZoomHelper;
    public EmojiAnimationsOverlay emojiAnimationsOverlay;
    public float drawingChatListViewYoffset;
    public int blurredViewTopOffset;
    public int blurredViewBottomOffset;
    public ChatMessageSharedResources sharedResources;

    private ValueAnimator searchExpandAnimator;
    private float searchExpandProgress;

    public static ChatActivity of(long dialogId) {
        Bundle bundle = new Bundle();
        if (dialogId >= 0) {
            bundle.putLong("user_id", dialogId);
        } else {
            bundle.putLong("chat_id", -dialogId);
        }
        return new ChatActivity(bundle);
    }

    public static ChatActivity of(long dialogId, int messageId) {
        Bundle bundle = new Bundle();
        if (dialogId >= 0) {
            bundle.putLong("user_id", dialogId);
        } else {
            bundle.putLong("chat_id", -dialogId);
        }
        bundle.putInt("message_id", messageId);
        return new ChatActivity(bundle);
    }

    public void deleteHistory(int dateSelectedStart, int dateSelectedEnd, boolean forAll) {
        chatAdapter.frozenMessages.clear();
        for (int i = 0; i < messages.size(); i++) {
            MessageObject messageObject = messages.get(i);
            if (messageObject.messageOwner.date <= dateSelectedStart || messageObject.messageOwner.date >= dateSelectedEnd) {
                chatAdapter.frozenMessages.add(messageObject);
            }
        }
        if (chatListView != null) {
            chatListView.setEmptyView(null);
        }
        if (chatAdapter.frozenMessages.isEmpty()) {
            showProgressView(true);
        }
        chatAdapter.isFrozen = true;
        chatAdapter.notifyDataSetChanged(true);
        UndoView undoView = getUndoView();
        if (undoView == null) {
            return;
        }

        undoView.showWithAction(dialog_id, UndoView.ACTION_CLEAR_DATES, () -> {
            getMessagesController().deleteMessagesRange(dialog_id, ChatObject.isChannel(currentChat) ? dialog_id : 0, dateSelectedStart, dateSelectedEnd, forAll, () -> {
                chatAdapter.frozenMessages.clear();
                chatAdapter.isFrozen = false;
                chatAdapter.notifyDataSetChanged(true);
                showProgressView(false);
            });
        }, () -> {
            chatAdapter.frozenMessages.clear();
            chatAdapter.isFrozen = false;
            chatAdapter.notifyDataSetChanged(true);
            showProgressView(false);
        });
    }

    public void showHeaderItem(boolean show) {
        if (show) {
            if (chatActivityEnterView.hasText() && TextUtils.isEmpty(chatActivityEnterView.getSlowModeTimer())) {
                if (attachItem != null) {
                    attachItem.setVisibility(View.VISIBLE);
                }
                if (headerItem != null) {
                    headerItem.setVisibility(View.GONE);
                }
                if (otherIcon != null) {
                    otherIcon.setIconVisible(true);
                }
            } else {
                if (attachItem != null) {
                    attachItem.setVisibility(View.GONE);
                }
                if (headerItem != null) {
                    headerItem.setVisibility(View.VISIBLE);
                }
                if (otherIcon != null) {
                    otherIcon.setIconVisible(false);
                }
            }
        } else {
            if (attachItem != null) {
                attachItem.setVisibility(View.GONE);
            }
            if (headerItem != null) {
                headerItem.setVisibility(View.GONE);
            }
            if (otherIcon != null) {
                otherIcon.setIconVisible(false);
            }
        }
        if (avatarContainer != null) {
            avatarContainer.ignoreTouches = !show;
        }
    }

    public long getTopicId() {
        return isTopic || chatMode == MODE_SAVED || chatMode == MODE_QUICK_REPLIES || chatMode == MODE_SUGGESTIONS ? threadMessageId : 0L;
    }

    public SendMessageChatArguments getMessageChatSendParams() {
        final SendMessageChatArguments.Builder builder = new SendMessageChatArguments.Builder();
        if (chatMode == MODE_WELCOME_MESSAGES) {
            builder.setWelcomeMessageChatId(welcomeMessagesChatId);
        }
        if (chatMode == MODE_QUICK_REPLIES) {
            builder.setQuickReplyShortcut(quickReplyShortcut, getQuickReplyId());
        }

        return builder.build();
    }

    public int getQuickReplyId() {
        return chatMode == MODE_QUICK_REPLIES ? (int) threadMessageId : 0;
    }

    public long getSavedDialogId() {
        return chatMode == MODE_SAVED ? threadMessageId : 0L;
    }

    public boolean isForumInViewAsMessagesMode() {
        return ChatObject.isForum(currentChat) && !isTopic || ChatObject.isMonoForum(currentChat) && getTopicId() == 0L && !isSubscriberSuggestions;
    }

    @Override
    public List<FloatingDebugController.DebugItem> onGetDebugItems() {
        List<FloatingDebugController.DebugItem> items = new ArrayList<>();
        if (ChatObject.isChannel(currentChat)) {
            items.add(new FloatingDebugController.DebugItem(LocaleController.getString(R.string.DebugShareAlert)));
            String mode;
            switch (shareAlertDebugMode) {
                default:
                    mode = LocaleController.getString(R.string.DebugShareAlertDialogsModeNormal);
                    break;
                case DEBUG_SHARE_ALERT_MODE_LESS:
                    mode = LocaleController.getString(R.string.DebugShareAlertDialogsModeLess);
                    break;
                case DEBUG_SHARE_ALERT_MODE_MORE:
                    mode = LocaleController.getString(R.string.DebugShareAlertDialogsModeMore);
                    break;
            }
            items.add(new FloatingDebugController.DebugItem(LocaleController.formatString(R.string.DebugShareAlertSwitchDialogsMode, mode), () -> {
                shareAlertDebugMode++;
                shareAlertDebugMode %= 3;
            }));

            items.add(new FloatingDebugController.DebugItem(LocaleController.getString(R.string.DebugShareAlertTopicsSlowMotion), ()-> shareAlertDebugTopicsSlowMotion = !shareAlertDebugTopicsSlowMotion));
        }
        if (currentUser == null) {
            items.add(new FloatingDebugController.DebugItem(LocaleController.getString(R.string.DebugMessageSkeletons)));
            items.add(new FloatingDebugController.DebugItem(LocaleController.getString(R.string.DebugMessageSkeletonsLightOverlayAlpha), 0, 255, new AnimationProperties.FloatProperty("") {
                @Override
                public void setValue(Object object, float value) {
                    SKELETON_LIGHT_OVERLAY_ALPHA = (int) value;
                }

                @Override
                public Object get(Object object) {
                    return (float) SKELETON_LIGHT_OVERLAY_ALPHA;
                }
            }));
            items.add(new FloatingDebugController.DebugItem(LocaleController.getString(R.string.DebugMessageSkeletonsSaturation), 1f, 10f, new AnimationProperties.FloatProperty("") {
                @Override
                public void setValue(Object object, float value) {
                    SKELETON_SATURATION = value;
                    skeletonColorMatrix.setSaturation(value);
                    skeletonServicePaint.setColorFilter(new ColorMatrixColorFilter(skeletonColorMatrix));
                }

                @Override
                public Object get(Object object) {
                    return SKELETON_SATURATION;
                }
            }));
        }
        return items;
    }

    public boolean allowSendPhotos() {
        if (currentChat != null && !ChatObject.canSendPhoto(currentChat)) {
            return false;
        } else {
            return true;
        }
    }

    public ThemeDelegate createThemeDelegate() {
        return new ThemeDelegate();
    }

    public void updateMessages(ArrayList<MessageObject> messageObjects, boolean replace) {
        for (int i = 0; i < messageObjects.size(); i++) {
            chatAdapter.updateRowWithMessageObject(messageObjects.get(i), false, replace);
        }
    }

    public TextView getOrCreateWebBotTitleView() {
        if (webBotTitle == null) {
            webBotTitle = new TextView(getContext());
            webBotTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            webBotTitle.setTypeface(AndroidUtilities.bold());
            webBotTitle.setGravity(Gravity.CENTER_VERTICAL);
            webBotTitle.setSingleLine(true);
            webBotTitle.setEllipsize(TextUtils.TruncateAt.END);
            actionBar.addView(webBotTitle, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, 0,  72, 0, 72 + 34, 0));
        }
        return webBotTitle;
    }

    private interface ChatActivityDelegate {
        default void openReplyMessage(int mid) {

        }

        default void openHashtagSearch(String hashtag) {


        }

        default void onUnpin(boolean all, boolean hide) {

        }

        default void onReport() {

        }
    }

    MessagePreviewView forwardingPreviewView;

    private PhotoViewer.PhotoViewerProvider photoViewerProvider = new PhotoViewer.EmptyPhotoViewerProvider() {

        @Override
        public PhotoViewer.PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index, boolean needPreview, boolean closing) {
            return ChatActivity.this.getPlaceForPhoto(messageObject, fileLocation, index, needPreview, false);
        }

        @Override
        public boolean validateGroupId(long groupId) {
            MessageObject.GroupedMessages groupedMessages = groupedMessagesMap.get(groupId);
            return groupedMessages != null && groupedMessages.messages.size() > 1;
        }
    };
    private PhotoViewer.PhotoViewerProvider photoViewerPaidMediaProvider = new PhotoViewer.EmptyPhotoViewerProvider() {

        @Override
        public PhotoViewer.PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index, boolean needPreview, boolean closing) {
            return ChatActivity.this.getPlaceForPhoto(messageObject, fileLocation, index, needPreview, false);
        }

        @Override
        public boolean validateGroupId(long groupId) {
            MessageObject.GroupedMessages groupedMessages = groupedMessagesMap.get(groupId);
            return groupedMessages != null && groupedMessages.messages.size() > 1;
        }

        @Override
        public boolean forceAllInGroup() {
            return true;
        }
    };

    private ArrayList<Object> botContextResults;
    private PhotoViewer.PhotoViewerProvider botContextProvider = new PhotoViewer.EmptyPhotoViewerProvider() {

        @Override
        public PhotoViewer.PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index, boolean needPreview, boolean closing) {
            if (index < 0 || index >= botContextResults.size() || mentionContainer == null || mentionContainer.getListView() == null) {
                return null;
            }
            int count = mentionContainer.getListView().getChildCount();
            Object result = botContextResults.get(index);

            for (int a = 0; a < count; a++) {
                ImageReceiver imageReceiver = null;
                View view = mentionContainer.getListView().getChildAt(a);
                if (view instanceof ContextLinkCell) {
                    ContextLinkCell cell = (ContextLinkCell) view;
                    if (cell.getResult() == result) {
                        imageReceiver = cell.getPhotoImage();
                    }
                }

                if (imageReceiver != null) {
                    int[] coords = new int[2];
                    view.getLocationInWindow(coords);
                    PhotoViewer.PlaceProviderObject object = new PhotoViewer.PlaceProviderObject();
                    object.viewX = coords[0];
                    object.viewY = coords[1];
//                    object.clipTopAddition = (int) (chatListViewPaddingTop - chatListViewPaddingVisibleOffset - AndroidUtilities.dp(4));
                    object.parentView = mentionContainer.getListView();
                    object.imageReceiver = imageReceiver;
                    object.thumb = imageReceiver.getBitmapSafe();
                    object.radius = imageReceiver.getRoundRadius(true);
                    return object;
                }
            }
            return null;
        }

        @Override
        public void sendButtonPressed(int index, VideoEditedInfo videoEditedInfo, boolean notify, int scheduleDate, int scheduleRepeatPeriod, boolean forceDocument) {
            if (index < 0 || index >= botContextResults.size()) {
                return;
            }
            sendBotInlineResult((TLRPC.BotInlineResult) botContextResults.get(index), notify, scheduleDate, 0);
        }
    };

    private final static int copy = 10;
    private final static int forward = 11;
    private final static int delete = 12;
    private final static int chat_enc_timer = 13;
    private final static int chat_menu_attach = 14;
    private final static int chat_menu_search = -1;
    private final static int chat_menu_options = -2;
    private final static int chat_menu_edit_text_options = -3;
    private final static int clear_history = 15;
    private final static int delete_chat = 16;
    private final static int share_contact = 17;
    private final static int mute = 18;
    private final static int report = 21;
    private final static int star = 22;
    private final static int edit = 23;
    private final static int add_shortcut = 24;
    private final static int save_to = 25;
    private final static int auto_delete_timer = 26;
    private final static int change_colors = 27;
    private final static int tag_message = 28;
    private final static int boost_group = 29;

    private final static int bot_help = 30;
    private final static int bot_settings = 31;
    private final static int call = 32;
    private final static int video_call = 33;

    private final static int attach_photo = 0;
    private final static int attach_gallery = 1;
    private final static int attach_video = 2;

    private final static int text_bold = 50;
    private final static int text_italic = 51;
    private final static int text_mono = 52;
    private final static int text_link = 53;
    private final static int text_regular = 54;
    private final static int text_strike = 55;
    private final static int text_underline = 56;
    private final static int text_spoiler = 57;
    private final static int text_quote = 58;
    private final static int text_date = 74;

    private final static int view_as_topics = 59;

    private final static int search = 40;

    private final static int topic_close = 60;
    private final static int open_forum = 61;

    private final static int translate = 62;
    private final static int scheduled = 63;
    private final static int edit_quick_reply = 64;

    private final static int copy_business_link = 65;
    private final static int share_business_link = 66;
    private final static int rename_business_link = 67;
    private final static int delete_business_link = 68;

    private final static int share = 69;
    private final static int open_direct = 70;
    private final static int remove_fee = 71;
    private final static int charge_fee = 72;

    private final static int chat_menu_topic_create = 73;

    private final static int id_chat_compose_panel = 1000;

    RecyclerListView.OnItemLongClickListenerExtended onItemLongClickListener = new RecyclerListView.OnItemLongClickListenerExtended() {
        @Override
        public boolean onItemClick(View view, int position, float x, float y) {
            if (isTryingTextSelection() || hasTextSelection() || inPreviewMode || isInsideContainer) {
                return false;
            }
            wasManualScroll = true;
            boolean result = true;
            boolean showMenu = true;
            if (view instanceof ChatActionCell) {
                ChatActionCell actionCell = (ChatActionCell) view;
                MessageObject messageObject = actionCell.getMessageObject();
                if (messageObject == null) return false;
                showMenu = messageObject.messageOwner.action instanceof TLRPC.TL_messageActionSetMessagesTTL || actionCell.getMessageObject().type == MessageObject.TYPE_SUGGEST_PHOTO || actionCell.getMessageObject().isWallpaperAction() || actionCell.getMessageObject().type == MessageObject.TYPE_GIFT_STARS;
            }
            if (!actionBar.isActionModeShowed() && (!isReport() || showMenu)) {
                result = createMenu(view, false, true, x, y, true);
            } else {
                boolean outside = false;
                if (view instanceof ChatMessageCell) {
                    outside = !((ChatMessageCell) view).isInsideBackground(x, y);
                }
                processRowSelect(view, outside, x, y);
            }
            if (view instanceof ChatMessageCell && (((ChatMessageCell) view).getMessageObject() != null && ((ChatMessageCell) view).getMessageObject().type != MessageObject.TYPE_JOINED_CHANNEL)) {
                startMultiselect(position);
                result = true;
            }
            return result;
        }
    };

    public RecyclerListView getChatListView() {
        return chatListView;
    }

    private void startMultiselect(int position) {
        if (isInsideContainer) {
            return;
        }
        int indexOfMessage = position - chatAdapter.messagesStartRow;
        if (indexOfMessage < 0 || indexOfMessage >= messages.size()) {
            return;
        }
        MessageObject messageObject = messages.get(indexOfMessage);
        final boolean unselect = selectedMessagesIds[0].get(messageObject.getId(), null) == null && selectedMessagesIds[1].get(messageObject.getId(), null) == null;
        SparseArray<MessageObject> alreadySelectedMessagesIds = new SparseArray<>();
        for (int i = 0; i < selectedMessagesIds[0].size(); i++) {
            alreadySelectedMessagesIds.put(selectedMessagesIds[0].keyAt(i), selectedMessagesIds[0].valueAt(i));
        }
        for (int i = 0; i < selectedMessagesIds[1].size(); i++) {
            alreadySelectedMessagesIds.put(selectedMessagesIds[1].keyAt(i), selectedMessagesIds[1].valueAt(i));
        }
        chatListView.startMultiselect(position, false, new RecyclerListView.onMultiSelectionChanged() {
            boolean limitReached;
            @Override
            public void onSelectionChanged(int position, boolean selected, float x, float y) {
                int i = position - chatAdapter.messagesStartRow;
                if (unselect) {
                    selected = !selected;
                }
                if (i >= 0 && i < messages.size()) {
                    MessageObject messageObject = messages.get(i);
                    if (selected && (selectedMessagesIds[0].indexOfKey(messageObject.getId()) >= 0 || selectedMessagesIds[1].indexOfKey(messageObject.getId()) >= 0)) {
                        return;
                    }
                    if (!selected && selectedMessagesIds[0].indexOfKey(messageObject.getId()) < 0 && selectedMessagesIds[1].indexOfKey(messageObject.getId()) < 0) {
                        return;
                    }
                    if (messageObject.contentType == 0) {
                        if (selected && selectedMessagesIds[0].size() + selectedMessagesIds[1].size() >= 100) {
                            limitReached = true;
                        } else {
                            limitReached = false;
                        }
                        RecyclerView.ViewHolder holder = chatListView.findViewHolderForAdapterPosition(position);
                        if (holder != null && holder.itemView instanceof ChatMessageCell) {
                            processRowSelect(holder.itemView, false, x, y);
                        } else {
                            addToSelectedMessages(messageObject, false);
                            updateActionModeTitle();
                            updateVisibleRows();
                        }
                    }
                }
            }

            @Override
            public boolean canSelect(int position) {
                int i = position - chatAdapter.messagesStartRow;
                if (i >= 0 && i < messages.size()) {
                    MessageObject messageObject = messages.get(i);
                    if (messageObject.contentType == 0) {
                        if (!unselect && alreadySelectedMessagesIds.get(messageObject.getId(), null) == null) {
                            return true;
                        }
                        if (unselect && alreadySelectedMessagesIds.get(messageObject.getId(), null) != null) {
                            return true;
                        }
                    }
                }
                return false;
            }

            @Override
            public int checkPosition(int position, boolean selectionTop) {
                int i = position - chatAdapter.messagesStartRow;
                if (i >= 0 && i < messages.size()) {
                    MessageObject messageObject = messages.get(i);
                    if (messageObject.contentType == 0 && messageObject.hasValidGroupId()) {
                        MessageObject.GroupedMessages groupedMessages = groupedMessagesMap.get(messageObject.getGroupId());
                        if (groupedMessages != null) {
                            MessageObject messageObject1 = groupedMessages.messages.get(selectionTop ? 0 : groupedMessages.messages.size() - 1);
                            return chatAdapter.messagesStartRow + messages.indexOf(messageObject1);
                        }
                    }
                }
                return position;
            }

            @Override
            public boolean limitReached() {
                return limitReached;
            }

            @Override
            public void getPaddings(int[] paddings) {
                paddings[0] = (int) chatListViewPaddingTop;
                paddings[1] = blurredViewBottomOffset;
            }

            @Override
            public void scrollBy(int dy) {
                chatListView.scrollBy(0, dy);
            }
        });
    }

    RecyclerListView.OnItemClickListenerExtended onItemClickListener = new RecyclerListView.OnItemClickListenerExtended() {
        @Override
        public void onItemClick(View view, int position, float x, float y) {
            if (inPreviewMode) {
                return;
            }
            wasManualScroll = true;
            if (view instanceof ChatActionCell && ((ChatActionCell) view).getMessageObject().isDateObject) {
                if (isInsideContainer) {
                    return;
                }
                Bundle bundle = new Bundle();
                int date = ((ChatActionCell) view).getMessageObject().messageOwner.date;
                bundle.putLong("dialog_id", dialog_id);
                bundle.putLong("topic_id", getTopicId());
                bundle.putInt("type", CalendarActivity.TYPE_CHAT_ACTIVITY);
                CalendarActivity calendarActivity = new CalendarActivity(bundle, SharedMediaLayout.FILTER_PHOTOS_AND_VIDEOS, date);
                calendarActivity.setChatActivity(ChatActivity.this);
                presentFragment(calendarActivity);
                return;
            }
            if (view instanceof ChatActionCell && ((ChatActionCell) view).getMessageObject() != null && ((ChatActionCell) view).getMessageObject().messageOwner.action instanceof TLRPC.TL_messageActionBoostApply) {
                getNotificationCenter().postNotificationName(NotificationCenter.openBoostForUsersDialog, dialog_id);
                return;
            }
            if (view instanceof ChatActionCell && ((ChatActionCell) view).getMessageObject() != null && ((ChatActionCell) view).getMessageObject().messageOwner.action instanceof TLRPC.TL_messageActionSetSameChatWallPaper) {
                int messageId = ((ChatActionCell) view).getMessageObject().getReplyMsgId();
                AndroidUtilities.runOnUIThread(() -> {
                    scrollToMessageId(messageId, 0, true, 0, true, 0);
                }, 16);
                return;
            }
            if (actionBar.isActionModeShowed() || isReport()) {
                boolean outside = false;
                if (view instanceof ChatMessageCell) {
                    if (textSelectionHelper.isSelected(((ChatMessageCell) view).getMessageObject())) {
                        return;
                    }
                    outside = !((ChatMessageCell) view).isInsideBackground(x, y);
                }
                processRowSelect(view, outside, x, y);
                return;
            }
            if (view instanceof ChatMessageCell) {
                MessageObject msg = ((ChatMessageCell) view).getMessageObject();
                if (msg != null && msg.type == MessageObject.TYPE_JOINED_CHANNEL) {
                    msg.toggleChannelRecommendations();
                    msg.forceUpdate = true;
                    ((ChatMessageCell) view).forceResetMessageObject();
                    view.requestLayout();
                    if (position >= 0) {
                        chatAdapter.notifyItemChanged(position);
                    }
                    return;
                }
            }
            createMenu(view, true, false, x, y, false);
        }

        @Override
        public boolean hasDoubleTap(View view, int position) {
            if (isQuickRepliesOrWelcomeMessagesMode()) return false;
            String reactionStringSetting = getMediaDataController().getDoubleTapReaction();
            TLRPC.TL_availableReaction reaction = getMediaDataController().getReactionsMap().get(reactionStringSetting);
            if (reaction == null && (reactionStringSetting == null || !reactionStringSetting.startsWith("animated_"))) {
                return false;
            }
            boolean available = dialog_id >= 0;
            if (!available && chatInfo != null) {
                available = ChatObject.reactionIsAvailable(chatInfo, reaction == null ? reactionStringSetting : reaction.reaction);
            }
            if (!available) {
                return false;
            }
            MessageObject messageObject;
            if (view instanceof ChatMessageCell) {
                messageObject = ((ChatMessageCell) view).getPrimaryMessageObject();
            } else if (view instanceof ChatActionCell) {
                messageObject = ((ChatActionCell) view).getMessageObject();
            } else {
                return false;
            }
            return messageObject != null && !messageObject.isDateObject && !messageObject.isSending() && messageObject.canSetReaction() && !messageObject.isEditing() && !actionBar.isActionModeShowed() && !isSecretChat() && !isInScheduleMode() && !messageObject.isSponsored();
        }

        @Override
        public void onDoubleTap(View view, int position, float x, float y) {
            if (getParentActivity() == null || isSecretChat() || isInScheduleMode() || isInPreviewMode() || isQuickRepliesOrWelcomeMessagesMode()) {
                return;
            }
            MessageObject messageObject;
            if (view instanceof ChatMessageCell) {
                messageObject = ((ChatMessageCell) view).getPrimaryMessageObject();
            } else if (view instanceof ChatActionCell) {
                messageObject = ((ChatActionCell) view).getMessageObject();
                if (messageObject.isDateObject) {
                    return;
                }
            } else {
                return;
            }
            if (messageObject.isSecret() || !messageObject.canSetReaction() || messageObject.isExpiredStory() || messageObject.type == MessageObject.TYPE_JOINED_CHANNEL) {
                return;
            }
            if (!(currentChat == null || ChatObject.isChannelAndNotMegaGroup(currentChat) || ChatObject.canUserDoAction(currentChat, ChatObject.ACTION_SEND_REACTIONS))) {
                return;
            }

            ReactionsEffectOverlay.removeCurrent(false);
            String reactionString = getMediaDataController().getDoubleTapReaction();
            if (reactionString.startsWith("animated_")) {
                boolean available = dialog_id >= 0;
                if (!available && chatInfo != null) {
                    available = ChatObject.reactionIsAvailable(chatInfo, reactionString);
                }
                if (!available) {
                    return;
                }
                selectReaction(view, messageObject, null, null, x, y, ReactionsLayoutInBubble.VisibleReaction.fromEmojicon(reactionString), true, false, false, false);
            } else {
                TLRPC.TL_availableReaction reaction = getMediaDataController().getReactionsMap().get(reactionString);
                if (reaction == null || messageObject.isSponsored()) {
                    return;
                }
                boolean available = dialog_id >= 0;
                if (!available && chatInfo != null) {
                    available = ChatObject.reactionIsAvailable(chatInfo, reaction.reaction);
                }
                if (!available) {
                    return;
                }
                selectReaction(view, messageObject, null, null, x, y, ReactionsLayoutInBubble.VisibleReaction.fromEmojicon(reaction), true, false, false, false);
            }
        }
    };

    private class ChatActivityEnterViewDelegate implements ChatActivityEnterView.ChatActivityEnterViewDelegate {

        int lastSize;
        boolean isEditTextItemVisibilitySuppressed;

        @Override
        public int getContentViewHeight() {
            return contentView.getHeight();
        }

        @Override
        public int measureKeyboardHeight() {
            return contentView.measureKeyboardHeight();
        }

        @Override
        public TLRPC.TL_channels_sendAsPeers getSendAsPeers() {
            return sendAsPeersObj;
        }

        @Override
        public void onMessageSend(CharSequence message, boolean notify, int scheduleDate, int scheduleRepeatPeriod, long payStars) {
            if (chatListItemAnimator != null) {
                chatActivityEnterViewAnimateFromTop = chatActivityEnterView.getBackgroundTop();
                if (chatActivityEnterViewAnimateFromTop != 0) {
                    chatActivityEnterViewAnimateBeforeSending = true;
                }
            }
            if (mentionContainer != null && mentionContainer.getAdapter() != null) {
                mentionContainer.getAdapter().addHashtagsFromMessage(message);
            }
            if (scheduleDate != 0) {
                if (scheduledMessagesCount == -1) {
                    scheduledMessagesCount = 0;
                }
                if (message != null) {
                    scheduledMessagesCount++;
                }
                if (messagePreviewParams != null && messagePreviewParams.forwardMessages != null && !messagePreviewParams.forwardMessages.messages.isEmpty()) {
                    scheduledMessagesCount += messagePreviewParams.forwardMessages.messages.size();
                }
                updateScheduledInterface(false);
            }
            if (!TextUtils.isEmpty(message) && messagePreviewParams != null && messagePreviewParams.forwardMessages != null && !messagePreviewParams.forwardMessages.messages.isEmpty() && messagePreviewParams.quote == null && payStars <= 0) {
                final ArrayList<MessageObject> messagesToForward = new ArrayList<>();
                messagePreviewParams.forwardMessages.getSelectedMessages(messagesToForward);
                boolean showReplyHint = messagesToForward.size() > 0;
                TLRPC.Peer toPeer = getMessagesController().getPeer(dialog_id);
                for (int i = 0; i < messagesToForward.size(); ++i) {
                    MessageObject msg = messagesToForward.get(i);
                    if (msg != null && msg.messageOwner != null && !MessageObject.peersEqual(msg.messageOwner.peer_id, toPeer)) {
                        showReplyHint = false;
                        break;
                    }
                }

                if (showReplyHint) {
                    Bulletin bulletin = BulletinFactory.of(ChatActivity.this)
                        .createSimpleBulletin(
                            R.raw.hint_swipe_reply,
                            LocaleController.getString(R.string.SwipeToReplyHint),
                            LocaleController.getString(R.string.SwipeToReplyHintMessage)
                        );
                    RLottieImageView imageView = ((Bulletin.TwoLineLottieLayout) bulletin.getLayout()).imageView;
                    imageView.setScaleX(1.8f);
                    imageView.setScaleY(1.8f);
                    bulletin.show(true);
                }
            }
            if (ChatObject.isForum(currentChat) && !isTopic && replyingMessageObject != null) {
                long topicId = replyingMessageObject.replyToForumTopic != null ? replyingMessageObject.replyToForumTopic.id : MessageObject.getTopicId(currentAccount, replyingMessageObject.messageOwner, true);
                if (topicId != 0) {
                    getMediaDataController().cleanDraft(dialog_id, topicId, false);
                }
            }

            hideFieldPanel(notify, scheduleDate, payStars, true);
            if (chatActivityEnterView != null && chatActivityEnterView.getEmojiView() != null) {
                chatActivityEnterView.getEmojiView().onMessageSend();
            }

            if (!getMessagesController().premiumFeaturesBlocked() && getMessagesController().transcribeAudioTrialWeeklyNumber <= 0 && !getMessagesController().didPressTranscribeButtonEnough() && !getUserConfig().isPremium() && !TextUtils.isEmpty(message) && messages != null) {
                for (int i = 1; i < Math.min(5, messages.size()); ++i) {
                    MessageObject msg = messages.get(i);
                    if (msg != null && !msg.isOutOwner() && (msg.isVoice() || msg.isRoundVideo()) && msg.isContentUnread()) {
                        TranscribeButton.showOffTranscribe(msg);
                    }
                }
            }
        }

        @Override
        public void didPressStreamingStop() {
            BotForumHelper.getInstance(currentAccount).stopStreaming(dialog_id, (int) getTopicId());
            checkSendButtonBlockedByTyping(true);
        }

        @Override
        public void onEditTextScroll() {
            if (suggestEmojiPanel != null) {
                suggestEmojiPanel.forceClose();
            }
        }

        @Override
        public void onContextMenuOpen() {
            if (suggestEmojiPanel != null) {
                suggestEmojiPanel.forceClose();
            }
        }

        @Override
        public void onContextMenuClose() {
            if (suggestEmojiPanel != null) {
                suggestEmojiPanel.fireUpdate();
            }
        }

        @Override
        public void onSwitchRecordMode(boolean video) {
            showVoiceHint(false, video);
        }

        @Override
        public void onPreAudioVideoRecord() {
            showVoiceHint(true, false);
        }

        @Override
        public void onUpdateSlowModeButton(View button, boolean show, CharSequence time) {
            showSlowModeHint(button, show, time);
            if (headerItem != null && headerItem.getVisibility() != View.VISIBLE) {
                headerItem.setVisibility(View.VISIBLE);
                if (attachItem != null) {
                    attachItem.setVisibility(View.GONE);
                }
                if (otherIcon != null) {
                    otherIcon.setIconVisible(false);
                }
            }
        }

        @Override
        public boolean checkCanRemoveRestrictionsByBoosts() {
            return ChatActivity.this.checkCanRemoveRestrictionsByBoosts();
        }

        @Override
        public void onTextSelectionChanged(int start, int end) {
            if (editTextItem == null) {
                return;
            }
            ActionBarMenu menu = actionBar.createMenu();
            if (suggestEmojiPanel != null) {
                suggestEmojiPanel.onTextSelectionChanged(start, end);
            }
            if (end - start > 0) {
                if (editTextItem.getTag() == null) {
                    editTextItem.setTag(1);

                    if (editTextItem.getVisibility() != View.VISIBLE) {
                        if (chatMode == MODE_SAVED && getSavedDialogId() == getUserConfig().getClientUserId() || chatMode == 0 && (threadMessageId == 0 || isTopic) && !UserObject.isReplyUser(currentUser) && !isReport()) {
                            editTextItem.setVisibility(View.VISIBLE);
                            checkEditTextItemMenu();
                            if (headerItem != null) {
                                headerItem.setVisibility(View.GONE);
                            }
                            if (attachItem != null) {
                                attachItem.setVisibility(View.GONE);
                            }
                            if (otherIcon != null) {
                                otherIcon.setIconVisible(false);
                            }
                        } else {
                            ValueAnimator valueAnimator = ValueAnimator.ofFloat(AndroidUtilities.dp(48), 0);
                            valueAnimator.setDuration(220);
                            valueAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
                            valueAnimator.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationStart(Animator animation) {
                                    actionBar.setMenuOffsetSuppressed(true);
                                    checkEditTextItemMenu();
                                    editTextItem.setVisibility(View.VISIBLE);
                                    menu.translateXItems(AndroidUtilities.dp(48));
                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    actionBar.setMenuOffsetSuppressed(false);
                                }
                            });
                            valueAnimator.addUpdateListener(animation -> menu.translateXItems((float) animation.getAnimatedValue()));
                            valueAnimator.start();
                        }
                    }
                }
                editTextStart = start;
                editTextEnd = end;
            } else {
                if (editTextItem.getTag() != null) {
                    editTextItem.setTag(null);
                    if (editTextItem.getVisibility() != View.GONE) {
                        if (chatMode == MODE_SAVED && getSavedDialogId() == getUserConfig().getClientUserId() || chatMode == 0 && (threadMessageId == 0 || isTopic) && !UserObject.isReplyUser(currentUser) && !isReport()) {
                            editTextItem.setVisibility(View.GONE);

                            if (chatActivityEnterView.hasText() && TextUtils.isEmpty(chatActivityEnterView.getSlowModeTimer())) {
                                if (headerItem != null) {
                                    headerItem.setVisibility(View.GONE);
                                }
                                if (attachItem != null) {
                                    attachItem.setVisibility(View.VISIBLE);
                                }
                                if (otherIcon != null) {
                                    otherIcon.setIconVisible(true);
                                }
                            } else {
                                if (headerItem != null) {
                                    headerItem.setVisibility(View.VISIBLE);
                                }
                                if (attachItem != null) {
                                    attachItem.setVisibility(View.GONE);
                                }
                                if (otherIcon != null) {
                                    otherIcon.setIconVisible(false);
                                }
                            }
                        } else {
                            ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, AndroidUtilities.dp(48));
                            valueAnimator.setDuration(220);
                            valueAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
                            valueAnimator.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationStart(Animator animation) {
                                    actionBar.setMenuOffsetSuppressed(true);
                                    isEditTextItemVisibilitySuppressed = true;
                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    editTextItem.setVisibility(View.GONE);
                                    menu.translateXItems(0);

                                    actionBar.setMenuOffsetSuppressed(false);
                                    isEditTextItemVisibilitySuppressed = false;
                                }
                            });
                            valueAnimator.addUpdateListener(animation -> menu.translateXItems((float) animation.getAnimatedValue()));
                            valueAnimator.start();
                        }
                    }
                }
            }
        }

        @Override
        public void onTextChanged(final CharSequence text, boolean bigChange, boolean fromDraft) {
            MediaController.getInstance().setInputFieldHasText(!TextUtils.isEmpty(text) || chatActivityEnterView.isEditingMessage());
            if (mentionContainer != null && mentionContainer.getAdapter() != null) {
                mentionContainer.getAdapter().searchUsernameOrHashtag(text, chatActivityEnterView.getCursorPosition(), messages, false, false);
            }
            if (waitingForCharaterEnterRunnable != null) {
                AndroidUtilities.cancelRunOnUIThread(waitingForCharaterEnterRunnable);
                waitingForCharaterEnterRunnable = null;
            }
            if ((currentChat == null || ChatObject.canSendEmbed(currentChat)) && chatActivityEnterView.isMessageWebPageSearchEnabled() && (!chatActivityEnterView.isEditingMessage() || !chatActivityEnterView.isEditingCaption())) {
                if (bigChange) {
                    searchLinks(text, true);
                } else {
                    checkEditLinkRemoved(text);
                    waitingForCharaterEnterRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if (this == waitingForCharaterEnterRunnable) {
                                searchLinks(text, false);
                                waitingForCharaterEnterRunnable = null;
                            }
                        }
                    };
                    AndroidUtilities.runOnUIThread(waitingForCharaterEnterRunnable, AndroidUtilities.WEB_URL == null ? 3000 : 1000);
                }
            }
            if (emojiAnimationsOverlay != null) {
                emojiAnimationsOverlay.cancelAllAnimations();
            }
            ReactionsEffectOverlay.dismissAll();
            if (!fromDraft) {
                if ((scheduledOrNoSoundHint != null && scheduledOrNoSoundHint.getVisibility() == View.VISIBLE)
                        || (scheduledHint != null && scheduledHint.getVisibility() == View.VISIBLE)) {
                    hideSendButtonHints();
                } else {
                    showScheduledHint();
                }
            }
        }

        @Override
        public void onTextSpansChanged(CharSequence text) {
            searchLinks(text, true);
        }

        @Override
        public void needSendTyping() {
            if (isQuickRepliesOrWelcomeMessagesMode() || chatMode == MODE_EDIT_BUSINESS_LINK || chatMode == MODE_SUGGESTIONS) return;
            getMessagesController().sendTyping(dialog_id, threadMessageId, 0, classGuid);
        }

        @Override
        public void onAttachButtonHidden() {
            if (actionBar.isSearchFieldVisible()) {
                return;
            }
            if (editTextItem != null && !isEditTextItemVisibilitySuppressed) {
                editTextItem.setVisibility(View.GONE);
            }
            if (TextUtils.isEmpty(chatActivityEnterView.getSlowModeTimer())) {
                if (headerItem != null) {
                    headerItem.setVisibility(View.GONE);
                }
                if (attachItem != null) {
                    attachItem.setVisibility(View.VISIBLE);
                }
                if (otherIcon != null) {
                    otherIcon.setIconVisible(true);
                }
            }
        }

        @Override
        public void onAttachButtonShow() {
            if (actionBar.isSearchFieldVisible()) {
                return;
            }
            if (headerItem != null) {
                headerItem.setVisibility(View.VISIBLE);
            }
            if (editTextItem != null && !isEditTextItemVisibilitySuppressed) {
                editTextItem.setVisibility(View.GONE);
            }
            if (attachItem != null) {
                attachItem.setVisibility(View.GONE);
            }
            if (otherIcon != null) {
                otherIcon.setIconVisible(false);
            }
        }

        @Override
        public void onMessageEditEnd(boolean loading) {
            if (chatListItemAnimator != null) {
                chatActivityEnterViewAnimateFromTop = chatActivityEnterView.getBackgroundTop();
                if (chatActivityEnterViewAnimateFromTop != 0) {
                    chatActivityEnterViewAnimateBeforeSending = true;
                }
            }
            if (!loading) {
                if (mentionContainer != null) {
                    mentionContainer.getAdapter().setNeedBotContext(true);
                }
                if (editingMessageObject != null) {
                    AndroidUtilities.runOnUIThread(() -> hideFieldPanel(true), 30);
                }
                boolean waitingForKeyboard = false;
                if (chatActivityEnterView.isPopupShowing()) {
                    chatActivityEnterView.setFieldFocused();
                    waitingForKeyboard = true;
                }
                chatActivityEnterView.setAllowStickersAndGifs(true, true, true, waitingForKeyboard);
                if (editingMessageObjectReqId != 0) {
                    getConnectionsManager().cancelRequest(editingMessageObjectReqId, true);
                    editingMessageObjectReqId = 0;
                }
                updatePinnedMessageView(true);
                updateBottomOverlay();
                updateVisibleRows();
            }
        }

        @Override
        public void onWindowSizeChanged(int size) {
            if (size < AndroidUtilities.dp(72) + ActionBar.getCurrentActionBarHeight()) {
                allowStickersPanel = false;
                if (suggestEmojiPanel.getVisibility() == View.VISIBLE) {
                    suggestEmojiPanel.setVisibility(View.INVISIBLE);
                }
            } else {
                allowStickersPanel = true;
                if (suggestEmojiPanel.getVisibility() == View.INVISIBLE && !isInPreviewMode()) {
                    suggestEmojiPanel.setVisibility(View.VISIBLE);
                }
            }

            allowContextBotPanel = !chatActivityEnterView.isPopupShowing();
//                checkContextBotPanel();
            int size2 = size + (chatActivityEnterView.isPopupShowing() ? 1 << 16 : 0);
            if (lastSize != size2) {
                chatActivityEnterViewAnimateFromTop = 0;
                chatActivityEnterViewAnimateBeforeSending = false;
            }
            lastSize = size2;
        }

        @Override
        public void onStickersTab(boolean opened) {
            if (emojiButtonRed != null) {
                emojiButtonRed.setVisibility(View.GONE);
            }
            allowContextBotPanelSecond = !opened;
//                checkContextBotPanel();
        }

        @Override
        public void didPressAttachButton() {
            if (chatAttachAlert != null) {
                chatAttachAlert.setEditingMessageObject(0, null);
            }
            openAttachMenu();
        }

        @Override
        public void didPressSuggestionButton() {
            new MessageSuggestionOfferSheet(getContext(), currentAccount, dialog_id, messageSuggestionParams != null ? messageSuggestionParams: MessageSuggestionParams.empty(), ChatActivity.this, getResourceProvider(), MessageSuggestionOfferSheet.MODE_INPUT, ChatActivity.this::showFieldPanelForSuggestionParams).show();
        }

        @Override
        public void toggleVideoRecordingPause() {
            if (instantCameraView != null) {
                instantCameraView.togglePause();
            }
        }

        @Override
        public boolean isVideoRecordingPaused() {
            return instantCameraView != null && instantCameraView.isPaused();
        }

        @Override
        public void needStartRecordVideo(int state, boolean notify, int scheduleDate, int scheduleRepeatPeriod, int ttl, long effectId, long stars) {
            checkInstantCameraView();
            if (instantCameraView != null) {
                if (state == 0) {
                    instantCameraView.showCamera(false);
                    chatListView.stopScroll();
                    chatAdapter.updateRowsSafe();
                } else if (state == 1 || state == 3 || state == 4) {
                    instantCameraView.send(state, notify, scheduleDate, 0, ttl, effectId, stars);
                } else if (state == 2 || state == 5) {
                    instantCameraView.cancel(state == 2);
                }
            }
        }

        @Override
        public void needChangeVideoPreviewState(int state, float seekProgress) {
            if (instantCameraView != null) {
                instantCameraView.changeVideoPreviewState(state, seekProgress);
            }
        }

        @Override
        public void needStartRecordAudio(int state) {
            int visibility = state == 0 ? View.GONE : View.VISIBLE;
            if (overlayView.getVisibility() != visibility) {
                overlayView.setVisibility(visibility);
            }
        }

        @Override
        public void needShowMediaBanHint() {
            showMediaBannedHint();
        }

        @Override
        public void onEmojiViewTabChanged() {
            final boolean isExpanded = chatActivityEnterView.isStickersExpanded();
            final boolean isEmoji = chatActivityEnterView.isCurrentPageEmoji();
            animatorHideTopPanelByEmojiKeyboardExpanded.setValue(isExpanded && !isEmoji, true);
        }

        @Override
        public void onStickersExpandedChange() {
            checkRaiseSensors();

            final boolean isExpanded = chatActivityEnterView.isStickersExpanded();
            final boolean isEmoji = chatActivityEnterView.isCurrentPageEmoji();
            animatorHideTopPanelByEmojiKeyboardExpanded.setValue(isExpanded && !isEmoji, true);

            if (isExpanded) {
                AndroidUtilities.setAdjustResizeToNothing(getParentActivity(), classGuid);
                if (Bulletin.getVisibleBulletin() != null && Bulletin.getVisibleBulletin().isShowing()) {
                    Bulletin.getVisibleBulletin().hide();
                }
            } else {
                AndroidUtilities.requestAdjustResize(getParentActivity(), classGuid);
            }
            if (mentionContainer != null) {
                mentionContainer.animate().alpha(isExpanded || isInPreviewMode() ? 0 : 1f).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
            }
            if (suggestEmojiPanel != null) {
                suggestEmojiPanel.setVisibility(View.VISIBLE);
                suggestEmojiPanel.animate().alpha(isExpanded || isInPreviewMode() ? 0 : 1f).setInterpolator(CubicBezierInterpolator.DEFAULT).withEndAction(() -> {
                    if (suggestEmojiPanel != null && isExpanded) {
                        suggestEmojiPanel.setVisibility(View.GONE);
                    }
                }).start();
            }
        }

        @Override
        public void scrollToSendingMessage() {
            int id = getSendMessagesHelper().getSendingMessageId(dialog_id);
            if (id != 0) {
                scrollToMessageId(id, 0, true, 0, true, 0);
            }
        }

        @Override
        public boolean hasScheduledMessages() {
            if (getMessagesController().isForum(getDialogId()) && !isTopic || chatMode == MODE_WELCOME_MESSAGES) {
                return false;
            }
            return scheduledMessagesCount > 0 && (chatMode == 0 || chatMode == MODE_SAVED && getSavedDialogId() == getUserConfig().getClientUserId());
        }

        @Override
        public void onSendLongClick() {
            if (scheduledOrNoSoundHint != null) {
                scheduledOrNoSoundHint.hide();
            }
            if (scheduledHint != null) {
                scheduledHint.hide();
            }
        }

        @Override
        public void openScheduledMessages() {
            ChatActivity.this.openScheduledMessages();
        }

        @Override
        public void onAudioVideoInterfaceUpdated() {
            updatePagedownButtonVisibility(true);
        }

        @Override
        public void bottomPanelTranslationYChanged(float translation) {
            if (translation != 0) {
                wasManualScroll = true;
            }

            invalidateChatListViewTopPadding();
            invalidateMessagesVisiblePart();
            updateTextureViewPosition(false, false);
            contentView.invalidate();
            updateBulletinLayout();
        }

        @Override
        public void prepareMessageSending() {
            waitingForSendingMessageLoad = true;
            if (chatAdapter != null) {
                chatAdapter.checkRemoveBotForumRowsStartThreadRow(true);
            }
        }

        @Override
        public void onTrendingStickersShowed(boolean show) {
            if (show) {
                AndroidUtilities.setAdjustResizeToNothing(getParentActivity(), classGuid);
                fragmentView.requestLayout();
            } else {
                AndroidUtilities.requestAdjustResize(getParentActivity(), classGuid);
            }
        }

        @Override
        public boolean hasForwardingMessages() {
            return messagePreviewParams != null && messagePreviewParams.forwardMessages != null && !messagePreviewParams.forwardMessages.messages.isEmpty();
        }

        @Override
        public void onKeyboardRequested() {
            checkAdjustResize();
        }

        @Override
        public boolean onceVoiceAvailable() {
            return currentUser != null && !UserObject.isUserSelf(currentUser) && !currentUser.bot && currentEncryptedChat == null && chatMode == 0;
        }

        @Override
        public ReplyQuote getReplyQuote() {
            return replyingQuote;
        }
    }

    private final ChatScrollCallback chatScrollHelperCallback = new ChatScrollCallback();

    private final Runnable showScheduledOrNoSoundRunnable = () -> {
        if (getParentActivity() == null || fragmentView == null || chatActivityEnterView == null) {
            return;
        }
        View anchor = chatActivityEnterView.getSendButton();
        if (anchor == null || chatActivityEnterView.getEditField() == null || chatActivityEnterView.getEditField().getText().length() < 5) {
            return;
        }
        SharedConfig.increaseScheduledOrNoSoundHintShowed();
        if (scheduledOrNoSoundHint == null) {
            scheduledOrNoSoundHint = new HintView(getParentActivity(), 4, themeDelegate) {
                @Override
                protected int offsetCx() {
                    return dp(100 - 44) / 2;
                }
            };
            scheduledOrNoSoundHint.createCloseButton();
            scheduledOrNoSoundHint.setAlpha(0);
            scheduledOrNoSoundHint.setVisibility(View.INVISIBLE);
            scheduledOrNoSoundHint.setText(getString(R.string.ScheduledOrNoSoundHint));
            contentView.addView(scheduledOrNoSoundHint, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 10, 0, 10, 0));
        }
        scheduledOrNoSoundHint.showForView(anchor, true);
        scheduledOrNoSoundHintShown = true;
    };

    private final Runnable showScheduledHintRunnable = () -> {
        if (getParentActivity() == null || fragmentView == null || chatActivityEnterView == null || forwardingPreviewView != null || getMessagesController().getSendPaidMessagesStars(getDialogId()) > 0) {
            return;
        }
        View anchor = chatActivityEnterView.getSendButton();
        if (anchor == null || chatActivityEnterView.getEditField() == null || chatActivityEnterView.getEditField().getText().length() == 0) {
            return;
        }
        SharedConfig.increaseScheduledHintShowed();
        if (scheduledHint == null) {
            scheduledHint = new HintView(getParentActivity(), 4, themeDelegate);
            scheduledHint.createCloseButton();
            scheduledHint.setAlpha(0);
            scheduledHint.setVisibility(View.INVISIBLE);
            scheduledHint.setText(LocaleController.getString(R.string.ScheduledHint));
            contentView.addView(scheduledHint, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 10, 0, 10, 0));
        }
        scheduledHint.showForView(anchor, true);
        scheduledHintShown = true;
    };

    public boolean isInsideContainer;
    public boolean reversed;
    private long wallpaperRandomSeed;

    public ChatActivity(Bundle args) {
        super(args);

        navbarContentSourceWallpaper = new BlurredBackgroundSourceWrapped();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SharedConfig.chatBlurEnabled()) {
            scrollableViewNoiseSuppressor = new DownscaleScrollableNoiseSuppressor();

            recommendedAdditionalSizeY = Math.max(0, dp(48) - Math.min(AndroidUtilities.navigationBarHeight, AndroidUtilities.statusBarHeight));

            glassBackgroundSourceFrostedRenderNode = new BlurredBackgroundSourceRenderNode(navbarContentSourceWallpaper);
            glassBackgroundSourceFrostedRenderNode.setOnDrawablesRelativePositionChangeListener(this::invalidateMergedVisibleBlurredPositionsAndSourcesPositions);
            glassBackgroundSourceFrostedRenderNode.setScrollableNoiseSuppressor(scrollableViewNoiseSuppressor, DownscaleScrollableNoiseSuppressor.DRAW_FROSTED_GLASS);
            glassBackgroundSourceFrostedRenderNode.setUnderSource(navbarContentSourceWallpaper);

            glassBackgroundDrawableFactoryFrosted = new BlurredBackgroundDrawableViewFactory(glassBackgroundSourceFrostedRenderNode);
            glassBackgroundDrawableFactoryFrosted.setLiquidGlassEffectAllowed(LiteMode.isEnabled(LiteMode.FLAG_LIQUID_GLASS));

            if (LiteMode.isEnabled(LiteMode.FLAG_LIQUID_GLASS)) {
                glassBackgroundSourceRenderNode = new BlurredBackgroundSourceRenderNode(navbarContentSourceWallpaper);
                glassBackgroundSourceRenderNode.setOnDrawablesRelativePositionChangeListener(this::invalidateMergedVisibleBlurredPositionsAndSourcesPositions);
                glassBackgroundSourceRenderNode.setScrollableNoiseSuppressor(scrollableViewNoiseSuppressor, DownscaleScrollableNoiseSuppressor.DRAW_GLASS);
                glassBackgroundSourceRenderNode.setUnderSource(navbarContentSourceWallpaper);
                glassBackgroundDrawableFactory = new BlurredBackgroundDrawableViewFactory(glassBackgroundSourceRenderNode);
                glassBackgroundDrawableFactory.setLiquidGlassEffectAllowed(LiteMode.isEnabled(LiteMode.FLAG_LIQUID_GLASS));
            } else {
                glassBackgroundSourceRenderNode = null;
                glassBackgroundDrawableFactory = glassBackgroundDrawableFactoryFrosted;
            }
        } else {
            scrollableViewNoiseSuppressor = null;
            recommendedAdditionalSizeY = 0;

            glassBackgroundSourceRenderNode = null;
            glassBackgroundSourceFrostedRenderNode = null;

            glassBackgroundDrawableFactory = new BlurredBackgroundDrawableViewFactory(navbarContentSourceWallpaper);
            glassBackgroundDrawableFactoryFrosted = new BlurredBackgroundDrawableViewFactory(navbarContentSourceWallpaper);
        }
        navbarContentDrawableFactory = new BlurredBackgroundDrawableViewFactory(navbarContentSourceWallpaper);
        navbarContentDrawableFactory.setLinkedViewsRef(glassAttachedViews);
        glassBackgroundDrawableFactory.setLinkedViewsRef(glassAttachedViews);
        glassBackgroundDrawableFactoryFrosted.setLinkedViewsRef(glassAttachedViews);
        scrimBlur3Factory.setLinkedViewsRef(new ReferenceList<>());

        navbarContentDrawableFactory.setLinkedDrawablesRef(glassAttachedDrawables);
        glassBackgroundDrawableFactory.setLinkedDrawablesRef(glassAttachedDrawables);
        glassBackgroundDrawableFactoryFrosted.setLinkedDrawablesRef(glassAttachedDrawables);
        scrimBlur3Factory.setLinkedDrawablesRef(glassAttachedDrawables);
    }

    private NotificationCenter.ObserversGroup observersGroup;
    private NotificationCenter.ObserversGroup globalObserversGroup;

    @Override
    public boolean onFragmentCreate() {
        final long chatId = arguments.getLong("chat_id", 0);
        final long userId = arguments.getLong("user_id", 0);
        final int encId = arguments.getInt("enc_id", 0);
        dialogFolderId = arguments.getInt("dialog_folder_id", 0);
        dialogFilterId = arguments.getInt("dialog_filter_id", 0);
        chatMode = arguments.getInt("chatMode", 0);
        quickReplyShortcut = arguments.getString("quick_reply", null);
        welcomeMessagesChatId = arguments.getLong("welcome_messages_chat_id", 0);
        voiceChatHash = arguments.getString("voicechat", null);
        openVideoChat = arguments.getBoolean("videochat", false);
        livestream = !TextUtils.isEmpty(arguments.getString("livestream", null));
        attachMenuBotToOpen = arguments.getString("attach_bot", null);
        attachMenuBotStartCommand = arguments.getString("attach_bot_start_command", null);
        inlineReturn = arguments.getLong("inline_return", 0);
        final String inlineQuery = arguments.getString("inline_query");
        textToSet = arguments.getString("start_text");
        premiumInvoiceBot = arguments.getBoolean("premium_bot", false);
        startLoadFromMessageId = arguments.getInt("message_id", 0);
        if (highlightTaskId == null) {
            highlightTaskId = arguments.containsKey("task_id") ? arguments.getInt("task_id", 0) : null;
        }
        if (highlightPollOptionId == null) {
            highlightPollOptionId = arguments.containsKey("poll_option_id") ? arguments.getByteArray("poll_option_id") : null;
        }
        startReplyTo = arguments.getInt("reply_to", 0);
        startLoadFromDate = arguments.getInt("start_from_date", 0);
        startFromVideoTimestamp = arguments.getInt("video_timestamp", -1);
        threadUnreadMessagesCount = arguments.getInt("unread_count", 0);
        convertingToast = arguments.getBoolean("converting_toast", false);
        convertingToastMessageId = arguments.getInt("converting_toast_from", 0);
        isSubscriberSuggestions = arguments.getBoolean("isSubscriberSuggestions", false);
        if (startFromVideoTimestamp >= 0) {
            startFromVideoMessageId = startLoadFromMessageId;
        }
        reportTitle = arguments.getString("reportTitle", null);
        reportOption = arguments.getByteArray("reportOption");
        reportMessage = arguments.getString("reportMessage", null);
        pulled = arguments.getBoolean("pulled", false);
        historyPreloaded = arguments.getBoolean("historyPreloaded", false);
        if (highlightMessageId != 0 && highlightMessageId != Integer.MAX_VALUE) {
            startLoadFromMessageId = highlightMessageId;
        }
        migrated_to = arguments.getInt("migrated_to", 0);
        scrollToTopOnResume = arguments.getBoolean("scrollToTopOnResume", false);
        needRemovePreviousSameChatActivity = arguments.getBoolean("need_remove_previous_same_chat_activity", true);
        justCreatedChat = arguments.getBoolean("just_created_chat", false);
        wallpaperRandomSeed = Utilities.random.nextLong();
        if (quickReplyShortcut != null) {
            QuickRepliesController.QuickReply quickReply = QuickRepliesController.getInstance(currentAccount).findReply(quickReplyShortcut);
            if (quickReply != null) {
                setQuickReplyId(quickReply.id);
            }
        }

        if (chatId != 0) {
            currentChat = getMessagesController().getChat(chatId);
            if (currentChat == null) {
                final CountDownLatch countDownLatch = new CountDownLatch(1);
                final MessagesStorage messagesStorage = getMessagesStorage();
                messagesStorage.getStorageQueue().postRunnable(() -> {
                    currentChat = messagesStorage.getChat(chatId);
                    countDownLatch.countDown();
                });
                try {
                    countDownLatch.await();
                } catch (Exception e) {
                    FileLog.e(e);
                }
                if (currentChat != null) {
                    getMessagesController().putChat(currentChat, true);
                } else {
                    return false;
                }
            }
            if (ChatObject.isMonoForum(currentChat)) {
                chatMode = MODE_SUGGESTIONS;
                isSubscriberSuggestions = !ChatObject.canManageMonoForum(currentAccount, currentChat);
            }
            dialog_id = -chatId;
            if (ChatObject.isChannel(currentChat)) {
                if (ChatObject.isNotInChat(currentChat) && !ChatObject.isMonoForum(currentChat) && !isThreadChat() && !isInScheduleMode()) {
                    waitingForGetDifference = true;
                    getMessagesController().startShortPoll(currentChat, classGuid, false, isGettingDifference -> {
                        waitingForGetDifference = isGettingDifference;
                        if (!waitingForGetDifference) {
                            firstLoadMessages();
                        }
                    });
                } else {
                    getMessagesController().startShortPoll(currentChat, classGuid, false);
                }
            }
        } else if (userId != 0) {
            currentUser = getMessagesController().getUser(userId);
            if (currentUser == null) {
                final MessagesStorage messagesStorage = getMessagesStorage();
                final CountDownLatch countDownLatch = new CountDownLatch(1);
                messagesStorage.getStorageQueue().postRunnable(() -> {
                    currentUser = messagesStorage.getUser(userId);
                    countDownLatch.countDown();
                });
                try {
                    countDownLatch.await();
                } catch (Exception e) {
                    FileLog.e(e);
                }
                if (currentUser != null) {
                    getMessagesController().putUser(currentUser, true);
                } else {
                    return false;
                }
            }
            dialog_id = userId;
            botUser = arguments.getString("botUser");
            if (inlineQuery != null) {
                getMessagesController().sendBotStart(currentUser, inlineQuery);
            } else if (premiumInvoiceBot && !TextUtils.isEmpty(botUser)) {
                getMessagesController().sendBotStart(currentUser, botUser);

                botUser = null;
                premiumInvoiceBot = false;
            }
            hasQuickReplies = false;
            if (currentUser != null && chatMode == 0 && !currentUser.bot) {
                QuickRepliesController.getInstance(currentAccount).load();
//                hasQuickReplies = QuickRepliesController.getInstance(currentAccount).hasReplies();
            }
        } else if (encId != 0) {
            currentEncryptedChat = getMessagesController().getEncryptedChat(encId);
            final MessagesStorage messagesStorage = getMessagesStorage();
            if (currentEncryptedChat == null) {
                final CountDownLatch countDownLatch = new CountDownLatch(1);
                messagesStorage.getStorageQueue().postRunnable(() -> {
                    currentEncryptedChat = messagesStorage.getEncryptedChat(encId);
                    countDownLatch.countDown();
                });
                try {
                    countDownLatch.await();
                } catch (Exception e) {
                    FileLog.e(e);
                }
                if (currentEncryptedChat != null) {
                    getMessagesController().putEncryptedChat(currentEncryptedChat, true);
                } else {
                    return false;
                }
            }
            currentUser = getMessagesController().getUser(currentEncryptedChat.user_id);
            if (currentUser == null) {
                final CountDownLatch countDownLatch = new CountDownLatch(1);
                messagesStorage.getStorageQueue().postRunnable(() -> {
                    currentUser = messagesStorage.getUser(currentEncryptedChat.user_id);
                    countDownLatch.countDown();
                });
                try {
                    countDownLatch.await();
                } catch (Exception e) {
                    FileLog.e(e);
                }
                if (currentUser != null) {
                    getMessagesController().putUser(currentUser, true);
                } else {
                    return false;
                }
            }
            dialog_id = DialogObject.makeEncryptedDialogId(encId);
            maxMessageId[0] = maxMessageId[1] = Integer.MIN_VALUE;
            minMessageId[0] = minMessageId[1] = Integer.MAX_VALUE;
        } else if (chatMode == MODE_EDIT_BUSINESS_LINK) {
            String businessLinkArgument = arguments.getString("business_link");
            if (businessLinkArgument == null) {
                return false;
            }
            businessLink = BusinessLinksController.getInstance(currentAccount).findLink(businessLinkArgument);
            if (businessLink == null) {
                return false;
            }
            forceEmptyHistory();
        } else if (chatMode == MODE_SEARCH) {
            searchType = arguments.getInt("searchType", 0);
            searchingHashtag = arguments.getString("searchHashtag", null);
            searchingQuery = searchingHashtag;
            if (searchType == 0 || searchingHashtag == null) {
                return false;
            }
        } else {
            return false;
        }

        dialog_id_Long = dialog_id;

        transitionAnimationGlobalIndex = NotificationCenter.getGlobalInstance().setAnimationInProgress(transitionAnimationGlobalIndex, new int[0]);

        if (currentUser != null && Build.VERSION.SDK_INT < 23) {
            MediaController.getInstance().startMediaObserver();
        }

        observersGroup = getNotificationCenter().createObserversGroup(this);
        globalObserversGroup = NotificationCenter.getGlobalInstance().createObserversGroup(this);

        getNotificationCenter().addPostponeNotificationsCallback(postponeNotificationsWhileLoadingCallback);
        getNotificationCenter().addObserver(this, NotificationCenter.closeChats);

        if (chatMode != MODE_SCHEDULED) {
            if (threadMessageId == 0) {
                observersGroup
                    .add(NotificationCenter.screenshotTook)
                    .add(NotificationCenter.encryptedChatUpdated)
                    .add(NotificationCenter.messagesReadEncrypted)
                    .add(NotificationCenter.updateMentionsCount)
                    .add(NotificationCenter.newDraftReceived)
                    .add(NotificationCenter.chatOnlineCountDidLoad)
                    .add(NotificationCenter.peerSettingsDidLoad)
                    .add(NotificationCenter.didLoadPinnedMessages)
                    .add(NotificationCenter.commentsRead)
                    .add(NotificationCenter.changeRepliesCounter)
                    .add(NotificationCenter.messagesRead)
                    .add(NotificationCenter.didLoadChatInviter)
                    .add(NotificationCenter.groupCallUpdated);
            } else {
                observersGroup.add(NotificationCenter.threadMessagesRead);
                if (isTopic) {
                    observersGroup.add(NotificationCenter.updateMentionsCount);
                    observersGroup.add(NotificationCenter.didLoadPinnedMessages);
                }
            }
            observersGroup
                .add(NotificationCenter.monoForumMessagesRead)
                .add(NotificationCenter.botKeyboardDidLoad)
                .add(NotificationCenter.removeAllMessagesFromDialog)
                .add(NotificationCenter.messagesReadContent)
                .add(NotificationCenter.chatSearchResultsAvailable)
                .add(NotificationCenter.chatSearchResultsLoading)
                .add(NotificationCenter.didUpdateMessagesViews)
                .add(NotificationCenter.didUpdatePollResults)
                .add(NotificationCenter.availableEffectsUpdate)
                .add(NotificationCenter.starReactionAnonymousUpdate);
            if (currentEncryptedChat != null) {
                observersGroup.add(NotificationCenter.didVerifyMessagesStickers);
            }
        }
        if (chatMode != MODE_PINNED) {
            observersGroup.add(NotificationCenter.didReceiveNewMessages);
        }
        if (chatMode == 0) {
            observersGroup.add(NotificationCenter.didLoadSponsoredMessages);
        }
        observersGroup
            .add(NotificationCenter.updatedChatRanks)
            .add(NotificationCenter.premiumFloodWaitReceived)
            .add(NotificationCenter.messagesDidLoad)
            .add(NotificationCenter.loadingMessagesFailed)
            .add(NotificationCenter.didUpdateConnectionState)
            .add(NotificationCenter.updateInterfaces)
            .add(NotificationCenter.updateDefaultSendAsPeer)
            .add(NotificationCenter.userIsPremiumBlockedUpadted)
            .add(NotificationCenter.didLoadSendAsPeers)
            .add(NotificationCenter.closeChatActivity)
            .add(NotificationCenter.messagesDeleted)
            .add(NotificationCenter.historyCleared)
            .add(NotificationCenter.messageReceivedByServer)
            .add(NotificationCenter.messageReceivedByAck)
            .add(NotificationCenter.messageSendError)
            .add(NotificationCenter.chatInfoDidLoad)
            .add(NotificationCenter.groupRestrictionsUnlockedByBoosts)
            .add(NotificationCenter.customStickerCreated)
            .add(NotificationCenter.contactsDidLoad)
            .add(NotificationCenter.messagePlayingProgressDidChanged)
            .add(NotificationCenter.messagePlayingDidReset)
            .add(NotificationCenter.messagePlayingGoingToStop)
            .add(NotificationCenter.messagePlayingPlayStateChanged)
            .add(NotificationCenter.blockedUsersDidLoad)
            .add(NotificationCenter.fileNewChunkAvailable)
            .add(NotificationCenter.didCreatedNewDeleteTask)
            .add(NotificationCenter.messagePlayingDidStart)
            .add(NotificationCenter.updateMessageMedia)
            .add(NotificationCenter.voiceTranscriptionUpdate)
            .add(NotificationCenter.animatedEmojiDocumentLoaded)
            .add(NotificationCenter.replaceMessagesObjects)
            .add(NotificationCenter.notificationsSettingsUpdated)
            .add(NotificationCenter.replyMessagesDidLoad)
            .add(NotificationCenter.didReceivedWebpages)
            .add(NotificationCenter.didReceivedWebpagesInUpdates)
            .add(NotificationCenter.botInfoDidLoad)
            .add(NotificationCenter.chatInfoCantLoad)
            .add(NotificationCenter.userInfoDidLoad)
            .add(NotificationCenter.pinnedInfoDidLoad)
            .add(NotificationCenter.topicsDidLoaded)
            .add(NotificationCenter.chatWasBoostedByUser)
            .add(NotificationCenter.channelRightsUpdated)
            .add(NotificationCenter.audioRecordTooShort)
            .add(NotificationCenter.didUpdateReactions)
            .add(NotificationCenter.savedReactionTagsUpdate)
            .add(NotificationCenter.updateAllMessages)
            .add(NotificationCenter.didUpdateExtendedMedia)
            .add(NotificationCenter.videoLoadingStateChanged)
            .add(NotificationCenter.scheduledMessagesUpdated)
            .add(NotificationCenter.diceStickersDidLoad)
            .add(NotificationCenter.dialogDeleted)
            .add(NotificationCenter.chatAvailableReactionsUpdated)
            .add(NotificationCenter.dialogsUnreadReactionsCounterChanged)
            .add(NotificationCenter.dialogsUnreadPollVotesCounterChanged)
            .add(NotificationCenter.groupStickersDidLoad)
            .add(NotificationCenter.dialogTranslate)
            .add(NotificationCenter.dialogIsTranslatable)
            .add(NotificationCenter.messageTranslated)
            .add(NotificationCenter.messageTranslating)
            .add(NotificationCenter.onReceivedChannelDifference)
            .add(NotificationCenter.storiesUpdated)
            .add(NotificationCenter.channelRecommendationsLoaded)
            .add(NotificationCenter.updateTranscriptionLock)
            .add(NotificationCenter.savedMessagesDialogsUpdate)
            .add(NotificationCenter.quickRepliesDeleted)
            .add(NotificationCenter.quickRepliesUpdated)
            .add(NotificationCenter.factCheckLoaded)
            .add(NotificationCenter.messagesFeeUpdated)
            .add(NotificationCenter.starBalanceUpdated)
            .add(NotificationCenter.botForumTopicDidCreate)
            .add(NotificationCenter.botForumDraftUpdate)
            .add(NotificationCenter.botForumDraftDelete)
            .add(NotificationCenter.joinedGroup);

        globalObserversGroup
            .add(NotificationCenter.emojiLoaded)
            .add(NotificationCenter.invalidateMotionBackground)
            .add(NotificationCenter.didSetNewWallpapper)
            .add(NotificationCenter.didApplyNewTheme)
            .add(NotificationCenter.goingToPreviewTheme);

        if (chatMode == MODE_EDIT_BUSINESS_LINK) {
            observersGroup.add(NotificationCenter.businessLinksUpdated);
        }
        if (chatMode == MODE_SEARCH) {
            observersGroup.add(NotificationCenter.hashtagSearchUpdated);
        }

        super.onFragmentCreate();

        if (chatMode == MODE_PINNED) {
            ArrayList<MessageObject> messageObjects = new ArrayList<>();
            for (int a = 0, N = pinnedMessageIds.size(); a < N; a++) {
                Integer id = pinnedMessageIds.get(a);
                MessageObject object = pinnedMessageObjects.get(id);
                if (object != null) {
                    MessageObject o = new MessageObject(object.currentAccount, object.messageOwner, true, false);
                    o.replyMessageObject = object.replyMessageObject;
                    o.mediaExists = object.mediaExists;
                    o.attachPathExists = object.attachPathExists;
                    messageObjects.add(o);
                }
            }
            int loadIndex = lastLoadIndex++;
            waitingForLoad.add(loadIndex);
            getNotificationCenter().postNotificationName(NotificationCenter.messagesDidLoad, dialog_id, messageObjects.size(), messageObjects, false, 0, last_message_id, 0, 0, 2, true, classGuid, loadIndex, pinnedMessageIds.get(0), 0, MODE_PINNED);
        } else if (!forceHistoryEmpty) {
            loading = true;
        }
        if (isThreadChat() && !isTopic) {
            if (highlightMessageId == startLoadFromMessageId) {
                needSelectFromMessageId = true;
            }
        } else {
            getMessagesController().setLastCreatedDialogId(dialog_id, chatMode == MODE_SCHEDULED, true);
            if (chatMode == 0 || chatMode == MODE_SAVED) {
                if (currentEncryptedChat == null) {
                    getMediaDataController().loadBotKeyboard(MessagesStorage.TopicKey.of(dialog_id, getTopicId()));
                }
                getMessagesController().loadPeerSettings(currentUser, currentChat);

                if (startLoadFromMessageId == 0) {
                    SharedPreferences sharedPreferences = MessagesController.getNotificationsSettings(currentAccount);
                    int messageId = sharedPreferences.getInt("diditem" + NotificationsController.getSharedPrefKey(dialog_id, getTopicId()), 0);
                    if (messageId != 0) {
                        wasManualScroll = true;
                        loadingFromOldPosition = true;
                        startLoadFromMessageOffset = sharedPreferences.getInt("diditemo" + NotificationsController.getSharedPrefKey(dialog_id, getTopicId()), 0);
                        startLoadFromMessageId = messageId;
                    }
                } else {
                    showScrollToMessageError = true;
                    needSelectFromMessageId = true;
                }
            }
        }

        loadInfo = false;
        if (currentChat != null) {
            chatInfo = getMessagesController().getChatFull(currentChat.id);
            groupCall = getMessagesController().getGroupCall(currentChat.id, true);
            if (ChatObject.isChannel(currentChat) && !getMessagesController().isChannelAdminsLoaded(currentChat.id) && !ChatObject.isMonoForum(currentChat)) {
                getMessagesController().loadChannelAdmins(currentChat.id, true);
            }
            fillInviterId(false);
            if (chatMode != MODE_PINNED) {
                getMessagesStorage().loadChatInfo(currentChat.id, ChatObject.isChannel(currentChat), null, true, false, startLoadFromMessageId);
            }
            if (chatMode == 0 && chatInfo != null && ChatObject.isChannel(currentChat) && chatInfo.migrated_from_chat_id != 0 && !isThreadChat()) {
                mergeDialogId = -chatInfo.migrated_from_chat_id;
                maxMessageId[1] = chatInfo.migrated_from_max_id;
            }
            loadInfo = chatInfo == null;
            checkGroupCallJoin(false);
            gotChatInfo();
        } else if (currentUser != null) {
            if (chatMode != MODE_PINNED) {
                getMessagesController().loadUserInfo(currentUser, true, classGuid, startLoadFromMessageId);
            }
            loadInfo = userInfo == null;
        }

        if (forceHistoryEmpty) {
            endReached[0] = endReached[1] = true;
            forwardEndReached[0] = forwardEndReached[1] = true;
            firstLoading = false;
            loading = false;
            checkDispatchHideSkeletons(false);
        }
        if (chatMode != MODE_PINNED && !forceHistoryEmpty) {
            if (SharedConfig.deviceIsHigh()) {
                initialMessagesSize = (isThreadChat() && !isTopic) ? 30 : 25;
            } else {
                initialMessagesSize = (isThreadChat() && !isTopic) ? 20 : 15;
            }
            if (!waitingForGetDifference) {
                firstLoadMessages();
            }
        }

        if (chatMode == 0) {
            if (userId != 0 && currentUser.bot) {
                AndroidUtilities.runOnUIThread(()-> getMediaDataController().loadBotInfo(userId, userId, true, classGuid));
            } else if (chatInfo instanceof TLRPC.TL_chatFull) {
                for (int a = 0; a < chatInfo.participants.participants.size(); a++) {
                    TLRPC.ChatParticipant participant = chatInfo.participants.participants.get(a);
                    TLRPC.User user = getMessagesController().getUser(participant.user_id);
                    if (user != null && user.bot) {
                        getMediaDataController().loadBotInfo(user.id, -chatInfo.id, true, classGuid);
                    }
                }
            }
            if (AndroidUtilities.isTablet() && !isComments) {
                getNotificationCenter().postNotificationName(NotificationCenter.openedChatChanged, dialog_id, getTopicId(), false);
            }

            if (currentUser != null && !UserObject.isReplyUser(currentUser)) {
                userBlocked = getMessagesController().blockePeers.indexOfKey(currentUser.id) >= 0;
            }

            if (currentEncryptedChat != null && AndroidUtilities.getMyLayerVersion(currentEncryptedChat.layer) != SecretChatHelper.CURRENT_SECRET_CHAT_LAYER) {
                getSecretChatHelper().sendNotifyLayerMessage(currentEncryptedChat, null);
            }
        }
        if (chatInfo != null && chatInfo.linked_chat_id != 0) {
            TLRPC.Chat chat = getMessagesController().getChat(chatInfo.linked_chat_id);
            if (chat != null && chat.megagroup) {
                getMessagesController().startShortPoll(chat, classGuid, false, null);
            }
        }

        if (currentUser != null) {
            TLRPC.UserFull userFull = getMessagesController().getUserFull(currentUser.id);
            if (userFull != null && userFull.theme != null) {
                ChatThemeController.getInstance(currentAccount).putThemeIfNeeded(userFull.theme);
            }
        }


        themeDelegate = parentThemeDelegate != null ? parentThemeDelegate : new ThemeDelegate();
        if (themeDelegate.isThemeChangeAvailable(false)) {
            globalObserversGroup.add(NotificationCenter.needSetDayNightTheme);
        }

        if (chatInvite != null) {
            int timeout = chatInvite.expires - getConnectionsManager().getCurrentTime();
            if (timeout < 0) {
                timeout = 10;
            }
            AndroidUtilities.runOnUIThread(chatInviteRunnable = () -> {
                chatInviteRunnable = null;
                if (getParentActivity() == null) {
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), themeDelegate);
                if (ChatObject.isChannel(currentChat) && !currentChat.megagroup) {
                    builder.setMessage(getString(R.string.JoinByPeekChannelText));
                    builder.setTitle(getString(R.string.JoinByPeekChannelTitle));
                } else {
                    builder.setMessage(getString(R.string.JoinByPeekGroupText));
                    builder.setTitle(getString(R.string.JoinByPeekGroupTitle));
                }
                builder.setPositiveButton(getString(R.string.JoinByPeekJoin), (dialogInterface, i) -> {
                    if (bottomOverlayChatText != null) {
                        bottomOverlayChatText.callOnClick();
                    }
                });
                builder.setNegativeButton(LocaleController.getString(R.string.Cancel), (dialogInterface, i) -> finishFragment());
                showDialog(builder.create());
            }, timeout * 1000L);
        }

        if (ChatObject.isMonoForum(currentChat)) {
            // reload balance if needed
            StarsController.getTonInstance(currentAccount).canUseTon();
        }

        if (isTopic || getMessagesController().isMonoForumWithManageRights(dialog_id) && getTopicId() != 0) {
            getMessagesController().getTopicsController().getTopicRepliesCount(dialog_id, getTopicId());
        }
        if (chatMode != MODE_EDIT_BUSINESS_LINK) {
            getMessagesController().getSavedMessagesController().preloadDialogs(false);
        }
        if (chatMode == MODE_SAVED) {
            getMessagesController().getSavedMessagesController().checkSavedDialogCount(getTopicId());
        }

        return true;
    }

    protected void updateSearchingHashtag(String hashtag) {
        if (chatMode != MODE_SEARCH) {
            return;
        }
        if (!TextUtils.equals(searchingHashtag, hashtag)) {
            createSearchHashtagViewsIfNeeded();
            showMessagesSearchListView(true);
            searchingHashtag = hashtag;
            searchingQuery = searchingHashtag;
            checkHashtagStories(false);
            clearChatData(true);
            startMessageAppearTransitionMs = 0;
            firstMessagesLoaded = false;
            HashtagSearchController.getInstance(currentAccount).clearSearchResults(searchType);
            messagesSearchAdapter.notifyDataSetChanged();
            messagesSearchListView.requestLayout();
            if (messagesSearchListView.getLayoutManager() != null) {
                messagesSearchListView.getLayoutManager().scrollToPosition(0);
            }
            updateSearchListEmptyView();
            hashtagSearchEmptyView.showProgress(true);
            firstLoadMessages();
        }
    }

    public void resetForReload() {
        getConnectionsManager().cancelRequestsForGuid(classGuid);
        getMessagesStorage().cancelTasksForGuid(classGuid);
        classGuid = ConnectionsManager.generateClassGuid();

        startLoadFromMessageId = 0;
        firstMessagesLoaded = false;
        clearOnLoad = true;
        waitingForLoad.clear();
    }

    public void savePositionForTopicChange(long intoTopic) {
        if (chatListView == null || chatLayoutManager == null || chatLayoutManager.hasPendingScrollPosition()) {
            clearOnLoadAndScrollMessageId = -1;
            return;
        }
        int centerY = 0;//(chatListView.getHeight() / 2) - chatListView.getPaddingBottom() - chatListView.getPaddingTop();
        int top = 0;
        int messageId = -1;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = chatListView.getChildCount() - 1; i >= 0; i--) {
            final View v = chatListView.getChildAt(i);
            final int vposition = chatListView.getChildAdapterPosition(v);
            if (vposition < 0) continue;
            if (v instanceof ChatMessageCell) {
                final MessageObject messageObject = ((ChatMessageCell) v).getMessageObject();
                if (messageObject == null || messageObject.getTopicId() != intoTopic)
                    continue;
                final int thisTop = getScrollingOffsetForView(v);
                final int distance = Math.abs(thisTop + centerY);
                if (distance < bestDistance) {
                    messageId = messageObject.getId();
                    top = thisTop;
                    bestDistance = distance;
                }
            }
        }
        clearOnLoadAndScrollMessageId = messageId;
        clearOnLoadAndScrollOffset = top;
    }

    public void firstLoadMessages() {
        if (firstMessagesLoaded) {
            return;
        }
        firstMessagesLoaded = true;
        final Runnable load = () -> {
            waitingForLoad.add(lastLoadIndex);
            if (chatMode == MODE_SEARCH) {
                HashtagSearchController.getInstance(currentAccount).searchHashtag(searchingHashtag, classGuid, searchType, lastLoadIndex++);
            } else if (startLoadFromDate != 0) {
                getMessagesController().loadMessages(dialog_id, mergeDialogId, false, 30, 0, startLoadFromDate, true, 0, classGuid, 4, 0, chatMode, threadMessageId, replyMaxReadId, lastLoadIndex++, isTopic);
            } else if (startLoadFromMessageId != 0 && (!isThreadChat() || startLoadFromMessageId == highlightMessageId || isTopic)) {
                startLoadFromMessageIdSaved = startLoadFromMessageId;
                if (migrated_to != 0) {
                    mergeDialogId = migrated_to;
                    getMessagesController().loadMessages(mergeDialogId, 0, loadInfo, initialMessagesSize, startLoadFromMessageId, 0, true, 0, classGuid, MessagesController.LOAD_AROUND_MESSAGE, 0, chatMode, threadMessageId, replyMaxReadId, lastLoadIndex++, isTopic);
                } else {
                    getMessagesController().loadMessages(dialog_id, mergeDialogId, loadInfo, initialMessagesSize, startLoadFromMessageId, 0, true, 0, classGuid, MessagesController.LOAD_AROUND_MESSAGE, 0, chatMode, threadMessageId, replyMaxReadId, lastLoadIndex++, isTopic);
                }
            } else {
                if (historyPreloaded) {
                    lastLoadIndex++;
                } else {
                    getMessagesController().loadMessages(dialog_id, mergeDialogId, loadInfo, initialMessagesSize, startLoadFromMessageId, 0, true, 0, classGuid, 2, 0, chatMode, threadMessageId, replyMaxReadId, lastLoadIndex++, isTopic);
                }
            }
            if ((chatMode == 0 || chatMode == MODE_SAVED && getSavedDialogId() == getUserConfig().getClientUserId()) && (!isThreadChat() || isTopic)) {
                waitingForLoad.add(lastLoadIndex);
                getMessagesController().loadMessages(dialog_id, mergeDialogId, false, 1, 0, 0, true, 0, classGuid, 2, 0, MODE_SCHEDULED, chatMode == MODE_SAVED ? 0 : threadMessageId, replyMaxReadId, lastLoadIndex++, isTopic);
            }
        };
        getMessagesController().checkSensitive(this, dialog_id, load, this::finishFragment);
    }

    private void fillInviterId(boolean load) {
        if (currentChat == null || chatInfo == null || ChatObject.isNotInChat(currentChat) || currentChat.creator) {
            return;
        }
        if (chatInfo.inviterId != 0) {
            chatInviterId = chatInfo.inviterId;
            return;
        }
        if (chatInfo.participants != null) {
            if (chatInfo.participants.self_participant != null) {
                chatInviterId = chatInfo.participants.self_participant.inviter_id;
                return;
            }
            long selfId = getUserConfig().getClientUserId();
            for (int a = 0, N = chatInfo.participants.participants.size(); a < N; a++) {
                TLRPC.ChatParticipant participant = chatInfo.participants.participants.get(a);
                if (participant.user_id == selfId) {
                    chatInviterId = participant.inviter_id;
                    return;
                }
            }
        }
        if (load && chatInviterId == 0) {
            getMessagesController().checkChatInviter(currentChat.id, false);
        }
    }

    private void hideUndoViews() {
        if (undoView != null) {
            undoView.hide(true, 0);
        }
        if (pinBulletin != null) {
            pinBulletin.hide(false, 0);
        }
        if (topUndoView != null) {
            topUndoView.hide(true, 0);
        }
    }

    public int getOtherSameChatsDiff() {
        if (parentLayout == null || parentLayout.getFragmentStack() == null) {
            return 0;
        }
        int cur = parentLayout.getFragmentStack().indexOf(this);
        if (cur == -1) {
            cur = parentLayout.getFragmentStack().size();
        }
        int i = cur;
        for (int a = 0; a < parentLayout.getFragmentStack().size(); a++) {
            BaseFragment fragment = parentLayout.getFragmentStack().get(a);
            if (fragment != this && fragment instanceof ChatActivity) {
                ChatActivity chatActivity = (ChatActivity) fragment;
                if (chatActivity.dialog_id == dialog_id) {
                    i = a;
                    break;
                }
            }
        }
        return i - cur;
    }

    @Override
    public void onBeginSlide() {
        super.onBeginSlide();

        if (selectionReactionsOverlay != null && selectionReactionsOverlay.isVisible()) {
            selectionReactionsOverlay.setHiddenByScroll(true);
        }
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        if (messageMetricsView != null) {
            messageMetricsView.finish();
        }
        if (chatActivityEnterView != null) {
            chatActivityEnterView.onDestroy();
        }
        if (avatarContainer != null) {
            avatarContainer.onDestroy();
        }
        if (mentionContainer != null && mentionContainer.getAdapter() != null) {
            mentionContainer.getAdapter().onDestroy();
        }
        if (chatAttachAlert != null) {
            chatAttachAlert.dismissInternal();
        }
        ContentPreviewViewer.getInstance().clearDelegate(contentPreviewViewerDelegate);
        getNotificationCenter().onAnimationFinish(transitionAnimationIndex);
        NotificationCenter.getGlobalInstance().onAnimationFinish(transitionAnimationGlobalIndex);
        getNotificationCenter().onAnimationFinish(scrollAnimationIndex);
        getNotificationCenter().onAnimationFinish(scrollCallbackAnimationIndex);
        hideUndoViews();
        if (chatInviteRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(chatInviteRunnable);
            chatInviteRunnable = null;
        }
        getNotificationCenter().removePostponeNotificationsCallback(postponeNotificationsWhileLoadingCallback);
        getMessagesController().setLastCreatedDialogId(dialog_id, chatMode == MODE_SCHEDULED, false);

        if (observersGroup != null) {
            observersGroup.removeAllObservers();
            observersGroup = null;
        }
        if (globalObserversGroup != null) {
            globalObserversGroup.removeAllObservers();
            globalObserversGroup = null;
        }

        getNotificationCenter().removeObserver(this, NotificationCenter.closeChats);

        if (chatMode == 0 && AndroidUtilities.isTablet()) {
            getNotificationCenter().postNotificationName(NotificationCenter.openedChatChanged, dialog_id, getTopicId(), true);
        }
        if (currentUser != null) {
            MediaController.getInstance().stopMediaObserver();
        }

        if (flagSecure != null) {
            flagSecure.detach();
        }
        if (currentUser != null) {
            getMessagesController().cancelLoadFullUser(currentUser.id);
        }
        AndroidUtilities.removeAdjustResize(getParentActivity(), classGuid);
        if (chatAttachAlert != null) {
            chatAttachAlert.onDestroy();
        }
        AndroidUtilities.unlockOrientation(getParentActivity());
        if (ChatObject.isChannel(currentChat)) {
            getMessagesController().startShortPoll(currentChat, classGuid, true);
            if (chatInfo != null && chatInfo.linked_chat_id != 0) {
                TLRPC.Chat chat = getMessagesController().getChat(chatInfo.linked_chat_id);
                getMessagesController().startShortPoll(chat, classGuid, true);
            }
        }
        if (textSelectionHelper != null) {
            textSelectionHelper.clear();
        }
        if (chatListItemAnimator != null) {
            chatListItemAnimator.onDestroy();
        }
        if (pinchToZoomHelper != null) {
            pinchToZoomHelper.clear();
        }
        chatThemeBottomSheet = null;

        INavigationLayout parentLayout = getParentLayout();
        if (parentLayout != null && parentLayout.getFragmentStack() != null) {
            BackButtonMenu.clearPulledDialogs(this, parentLayout.getFragmentStack().indexOf(this) - (replacingChatActivity ? 0 : 1));
        }
        replacingChatActivity = false;

        if (progressDialogCurrent != null) {
            progressDialogCurrent.cancel();
            progressDialogCurrent = null;
        }
        chatMessagesMetadataController.onFragmentDestroy();
        if (birthdayAssetsFetcher != null) {
            birthdayAssetsFetcher.detach(true);
            birthdayAssetsFetcher = null;
        }
        if (starReactionsOverlay != null) {
            starReactionsOverlay.setMessageCell(null);
            AndroidUtilities.removeFromParent(starReactionsOverlay);
            starReactionsOverlay = null;
        }
    }

    private static class ChatActivityTextSelectionHelper extends TextSelectionHelper.ChatListTextSelectionHelper {
        ChatActivity chatActivity;
        public void setChatActivity(ChatActivity chatActivity) {
            cancelAllAnimators();
            clear();
            textSelectionOverlay = null;
            this.chatActivity = chatActivity;
        }

        @Override
        public int getParentTopPadding() {
            return chatActivity == null ? 0 : (int) chatActivity.chatListViewPaddingTop;
        }

        @Override
        public int getParentBottomPadding() {
            return chatActivity == null ? 0 : chatActivity.blurredViewBottomOffset;
        }

        @Override
        protected int getThemedColor(int key) {
            return Theme.getColor(key, chatActivity.themeDelegate);
        }

        @Override
        protected Theme.ResourcesProvider getResourcesProvider() {
            if (chatActivity != null) {
                return chatActivity.themeDelegate;
            }
            return null;
        }

        @Override
        protected boolean canShowQuote() {
            if (chatActivity != null && chatActivity.getDialogId() == UserObject.VERIFY) {
                return false;
            }
            final boolean noforwards = (
                chatActivity != null && chatActivity.isPeerNoForwards() ||
                selectedView != null && selectedView.getMessageObject() != null && selectedView.getMessageObject().messageOwner != null && selectedView.getMessageObject().messageOwner.noforwards
            );
            return !isFactCheck && (
                chatActivity != null && chatActivity.getCurrentEncryptedChat() == null &&
                (selectedView == null ||
                    selectedView.getMessageObject() != null && selectedView.getMessageObject().type != MessageObject.TYPE_STORY &&
                    !selectedView.getMessageObject().isVoiceTranscriptionOpen() && !selectedView.getMessageObject().isInvoice() &&
                    selectedView.getMessageObject().richLayout == null &&
                    !chatActivity.textSelectionHelper.isDescription
                ) &&
                !chatActivity.getMessagesController().getTranslateController().isTranslatingDialog(chatActivity.dialog_id) &&
                !UserObject.isService(chatActivity.dialog_id) &&
                (!noforwards || (chatActivity.getCurrentChat() == null || ChatObject.canWriteToChat(chatActivity.getCurrentChat())))
            );
        }

        @Override
        protected boolean canCopy() {
            if (chatActivity != null && chatActivity.getDialogId() == UserObject.VERIFY) {
                return true;
            }
            return chatActivity == null || !(
                chatActivity.getDialogId() < 0 && chatActivity.getMessagesController().isPeerNoForwards(chatActivity.getDialogId()) ||
                selectedView != null && selectedView.getMessageObject() != null && (selectedView.getMessageObject().messageOwner != null && selectedView.getMessageObject().messageOwner.noforwards)
            );
        }

        @Override
        protected void onQuoteClick(MessageObject messageObject, int start, int end, CharSequence text) {
            if (messageObject == null) {
                return;
            }
            if (chatActivity != null) {
                end = Math.min(end, start + chatActivity.getMessagesController().quoteLengthMax);
                if (messageObject.getGroupId() != 0) {
                    MessageObject.GroupedMessages group = chatActivity.getGroup(messageObject.getGroupId());
                    if (group != null && !group.isDocuments) {
                        messageObject = group.captionMessage;
                    }
                }
                if (messageObject == null) {
                    return;
                }
                ReplyQuote quote = ReplyQuote.from(messageObject, start, end);
                if (quote.getText() == null) {
                    return;
                }
                if (chatActivity.chatActivityEnterView == null || chatActivity.chatActivityEnterView.getVisibility() != View.VISIBLE) {
                    chatActivity.replyingQuote = quote;
                    chatActivity.replyingMessageObject = messageObject;
                    chatActivity.forbidForwardingWithDismiss = false;
                    chatActivity.messagePreviewParams = new MessagePreviewParams(chatActivity.currentEncryptedChat != null, chatActivity.isPeerNoForwards(), ChatObject.isMonoForum(chatActivity.currentChat));
                    chatActivity.messagePreviewParams.updateReply(chatActivity.replyingMessageObject, chatActivity.getGroup(messageObject.getGroupId()), chatActivity.getDialogId(), chatActivity.replyingQuote);
                    Bundle args = new Bundle();
                    args.putBoolean("onlySelect", true);
                    args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_FORWARD);
                    args.putBoolean("quote", true);
                    args.putInt("messagesCount", 1);
                    args.putBoolean("canSelectTopics", true);
                    DialogsActivity fragment = new DialogsActivity(args);
                    fragment.setDelegate(chatActivity);
                    chatActivity.presentFragment(fragment);
                } else {
                    if (chatActivity.actionBar != null && chatActivity.actionBar.isActionModeShowed()) {
                        chatActivity.clearSelectionMode();
                    }
                    chatActivity.showFieldPanelForReplyQuote(messageObject, quote);
                    if (chatActivity.chatActivityEnterView != null) {
                        chatActivity.chatActivityEnterView.openKeyboard();
                    }
                }
            }
        }
    }

    private Runnable justForTest;

    @Override
    public View createView(Context context) {
        Timer t = Timer.create("ChatActivity.createView");

        blurredBackgroundColorProvider = new BlurredBackgroundColorProviderThemed(themeDelegate, Theme.key_chat_messagePanelBackground) {
            @Override
            public int getBackgroundColor() {
                if (!BlurredBackgroundProviderImpl.checkBlurEnabled(currentAccount, themeDelegate)) {
                    return ColorUtils.setAlphaComponent(getThemedColor(Theme.key_chat_messagePanelBackground), 255);
                }

                final boolean isThemeLight = themeDelegate != null && !themeDelegate.isDark();
                if (isThemeLight) {
                    return ColorUtils.setAlphaComponent(super.getBackgroundColor(), 216);
                }
                return super.getBackgroundColor();
            }
        };
        blurredBackgroundColorProviderWhite = new BlurredBackgroundColorProviderThemed(themeDelegate, Theme.key_windowBackgroundWhite) {
            @Override
            public int getBackgroundColor() {
                if (!BlurredBackgroundProviderImpl.checkBlurEnabled(currentAccount, themeDelegate)) {
                    return ColorUtils.setAlphaComponent(getThemedColor(Theme.key_windowBackgroundWhite), 255);
                }

                final boolean isThemeLight = themeDelegate != null && !themeDelegate.isDark();
                if (isThemeLight) {
                    return ColorUtils.setAlphaComponent(super.getBackgroundColor(), 216);
                }
                return super.getBackgroundColor();
            }
        };

        if (textSelectionHelper == null) {
            Timer.Task t1 = Timer.start(t, "new ChatActivityTextSelectionHelper");
            textSelectionHelper = new ChatActivityTextSelectionHelper();
            textSelectionHelper.setChatActivity(this);
            Timer.done(t1);
        }

        if (isReport()) {
            actionBar.setBackgroundColor(getThemedColor(Theme.key_actionBarActionModeDefault));
            actionBar.setItemsColor(getThemedColor(Theme.key_actionBarActionModeDefaultIcon), false);
            actionBar.setItemsBackgroundColor(getThemedColor(Theme.key_actionBarActionModeDefaultSelector), false);
            actionBar.setTitleColor(getThemedColor(Theme.key_actionBarActionModeDefaultIcon));
            actionBar.setSubtitleColor(getThemedColor(Theme.key_actionBarActionModeDefaultIcon));
        }
        if (isInsideContainer) {
            actionBar.setVisibility(View.GONE);
        }
        actionBarBackgroundPaint.setColor(getThemedColor(Theme.key_actionBarDefault));
        sharedResources = new ChatMessageSharedResources(context);

        //ArrayList<ChatMessageCell> chatMessagesCache = chatMessageCellsCache.get(currentAccount);
        //if (chatMessagesCache == null) {
        //    chatMessageCellsCache.put(currentAccount, chatMessagesCache = new ArrayList<>());
        //}
        //if (chatMessagesCache.size() < 10) {
        //    int n = 15 - chatMessagesCache.size();
        //    Timer.Task t2 = Timer.start(t, "create ChatMessageCell n=" + n);
        //    for (int a = 0; a < n; a++) {
        //        chatMessagesCache.add(new ChatMessageCell(context, currentAccount,true, sharedResources, themeDelegate));
        //    }
        //    Timer.done(t2);
        //}
        for (int a = 1; a >= 0; a--) {
            selectedMessagesIds[a].clear();
            selectedMessagesCanCopyIds[a].clear();
            selectedMessagesCanStarIds[a].clear();
        }
        scheduledOrNoSoundHint = null;
        scheduledHint = null;
        infoTopView = null;
        aspectRatioFrameLayout = null;
        videoTextureView = null;
        mediaBanTooltip = null;
        noSoundHintView = null;
        forwardHintView = null;
        checksHintView = null;
        textSelectionHint = null;
        emojiButtonRed = null;
        gifHintTextView = null;
        emojiHintTextView = null;
        pollHintView = null;
        timerHintView = null;
        videoPlayerContainer = null;
        voiceHintTextView = null;
        blurredView = null;
        dummyMessageCell = null;
        cantDeleteMessagesCount = 0;
        canEditMessagesCount = 0;
        cantForwardMessagesCount = 0;
        canForwardMessagesCount = 0;
        cantSaveMessagesCount = 0;
        canSaveMusicCount = 0;
        canSaveDocumentsCount = 0;

        hasOwnBackground = true;
        if (chatAttachAlert != null) {
            try {
                if (chatAttachAlert.isShowing()) {
                    chatAttachAlert.dismiss();
                }
            } catch (Exception ignore) {

            }
            chatAttachAlert.onDestroy();
            chatAttachAlert = null;
        }

        Theme.createChatResources(context, false);

        actionBar.setAddToContainer(false);
        actionBar.setCastShadows(false);
        actionBar.setBackground(null);
        // actionBar.setOccupyStatusBar(false);
        if (inPreviewMode) {
            actionBar.setBackButtonDrawable(null);
        } else {
            actionBar.setBackButtonDrawable(new BackDrawable(isReport()));
        }

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(final int id) {
                if (id == -1) {
                    if (isInPollAddOptionMode()) {
                        pollAddOptionModeClose();
                    } else if (actionBar.isActionModeShowed()) {
                        clearSelectionMode();
                    } else {
                        if (chatMode == MODE_QUICK_REPLIES && (messages.isEmpty() || threadMessageId == 0)) {
                            showQuickRepliesRemoveAlert();
                            return;
                        }
                        if (chatMode == MODE_EDIT_BUSINESS_LINK && chatActivityEnterView.businessLinkHasChanges()) {
                            showBusinessLinksDiscardAlert(() -> {
                                finishFragment();
                            });
                            return;
                        }
                        if (!checkRecordLocked(true, true)) {
                            finishFragment();
                        }
                    }
                } else if (id == view_as_topics) {
                    if (getUserConfig().getClientUserId() == dialog_id) {
                        getMessagesController().setSavedViewAs(true);
                        avatarContainer.openProfile(false, true, true);
                    } else {
                        getMessagesController().getTopicsController().toggleViewForumAsMessages(-dialog_id, false);
                        TopicsFragment.prepareToSwitchAnimation(ChatActivity.this);
                    }
                } else if (id == copy) {
                    SpannableStringBuilder str = new SpannableStringBuilder();
                    long previousUid = 0;
                    for (int a = 1; a >= 0; a--) {
                        ArrayList<Integer> ids = new ArrayList<>();
                        for (int b = 0; b < selectedMessagesCanCopyIds[a].size(); b++) {
                            ids.add(selectedMessagesCanCopyIds[a].keyAt(b));
                        }
                        if (currentEncryptedChat == null) {
                            Collections.sort(ids);
                        } else {
                            Collections.sort(ids, Collections.reverseOrder());
                        }
                        for (int b = 0; b < ids.size(); b++) {
                            Integer messageId = ids.get(b);
                            MessageObject messageObject = selectedMessagesCanCopyIds[a].get(messageId);
                            if (str.length() != 0) {
                                str.append("\n\n");
                            }
                            str.append(getMessageContent(messageObject, previousUid, ids.size() != 1 && (currentUser == null || !currentUser.self)));
                            previousUid = messageObject.getFromChatId();
                        }
                    }
                    if (str.length() != 0) {
                        AndroidUtilities.addToClipboard(str);
                        createUndoView();
                        undoView.showWithAction(0, UndoView.ACTION_TEXT_COPIED, null);
                    }
                    clearSelectionMode();
                } else if (id == delete) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    createDeleteMessagesAlert(null, null);
                } else if (id == forward) {
                    openForward(true);
                } else if (id == share) {
                    share();
                } else if (id == open_direct) {
                    if (currentChat == null) return;
                    presentFragment(ChatActivity.of(-currentChat.linked_monoforum_id));
                } else if (id == charge_fee ) {
                    long user_id = dialog_id;
                    long parent_id = 0;
                    if (ChatObject.isMonoForum(currentChat) && ChatObject.canManageMonoForum(currentAccount, currentChat)) {
                        user_id = getThreadId();
                        parent_id = dialog_id;
                    }
                    StarsController.getInstance(currentAccount).stopPaidMessages(user_id, parent_id, false, false);
                } else if (id == remove_fee) {
                    long _user_id = dialog_id;
                    long _parent_id = 0;
                    if (ChatObject.isMonoForum(currentChat) && ChatObject.canManageMonoForum(currentAccount, currentChat)) {
                        _user_id = getThreadId();
                        _parent_id = dialog_id;
                    }
                    final long user_id = _user_id;
                    final long parent_id = _parent_id;
                    StarsController.getInstance(currentAccount).getPaidRevenue(user_id, parent_id, revenue -> {
                        if (getContext() == null) return;
                        AlertsCreator.showAlertWithCheckboxWithBalance(
                            getContext(),
                            getString(R.string.RemoveMessageFeeTitle),
                            AndroidUtilities.replaceTags(formatString(ChatObject.isMonoForum(currentChat) ? R.string.RemoveMessageFeeMessageChannel : R.string.RemoveMessageFeeMessage, DialogObject.getShortName(user_id))),
                            revenue > 0 ? formatPluralStringComma("RemoveMessageFeeRefund", (int) (long) revenue) : null,
                            getString(R.string.Confirm),
                            refund -> StarsController.getInstance(currentAccount).stopPaidMessages(user_id, parent_id, revenue > 0 && refund, true),
                            resourceProvider
                        );
                    });
                } else if (id == tag_message) {
                    if (tagSelector == null) {
                        showTagSelector();
                    } else {
                        hideTagSelector();
                    }
                } else if (id == save_to) {
                    ArrayList<MessageObject> messageObjects = new ArrayList<>();
                    for (int a = 1; a >= 0; a--) {
                        for (int b = 0; b < selectedMessagesIds[a].size(); b++) {
                            messageObjects.add(selectedMessagesIds[a].valueAt(b));
                        }
                        selectedMessagesIds[a].clear();
                        selectedMessagesCanCopyIds[a].clear();
                        selectedMessagesCanStarIds[a].clear();
                    }
                    boolean isMusic = canSaveMusicCount > 0;
                    hideActionMode();
                    updatePinnedMessageView(true);
                    updateVisibleRows();
                    MediaController.saveFilesFromMessages(getParentActivity(), getAccountInstance(), messageObjects, (count) -> {
                        if (count > 0) {
                            if (getParentActivity() == null) {
                                return;
                            }
                            BulletinFactory.of(ChatActivity.this).createDownloadBulletin(isMusic ? BulletinFactory.FileType.AUDIOS : BulletinFactory.FileType.UNKNOWNS, count, themeDelegate).show();
                        }
                    });
                } else if (id == chat_enc_timer) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    showDialog(AlertsCreator.createTTLAlert(getParentActivity(), currentEncryptedChat, themeDelegate).create());
                } else if (id == clear_history || id == delete_chat || id == auto_delete_timer) {
                    if (getParentActivity() == null) {
                        return;
                    }

                    if (id == clear_history && ChatObject.isMonoForum(currentChat)) {
                        if (getThreadId() != 0) {
                            final TLRPC.User user = getMessagesController().getUser(getThreadId());
                            if (user != null) {
                                AlertsCreator.createClearDaysDialogAlert(ChatActivity.this, -1, user, currentChat, true, revoke -> {
                                    if (user.id != getThreadId()) {
                                        return;
                                    }
                                    performHistoryClear(false, true);
                                }, getResourceProvider());
                            }
                        }
                        return;
                    }

                    boolean canDeleteHistory = chatInfo != null && chatInfo.can_delete_channel;
                    if (id == auto_delete_timer || id == clear_history && currentEncryptedChat == null && ((currentUser != null && !UserObject.isUserSelf(currentUser) && !UserObject.isDeleted(currentUser)) || (chatInfo != null && chatInfo.can_delete_channel))) {
                        AlertsCreator.createClearDaysDialogAlert(ChatActivity.this, -1, currentUser, currentChat, canDeleteHistory, new MessagesStorage.BooleanCallback() {
                            @Override
                            public void run(boolean revoke) {
                                if (revoke && (currentUser != null || canDeleteHistory)) {
                                    getMessagesStorage().getMessagesCount(dialog_id, (count) -> {
                                        if (count >= 50) {
                                            AlertsCreator.createClearOrDeleteDialogAlert(ChatActivity.this, true, currentChat, currentUser, false, false, false, canDeleteHistory, (param) -> performHistoryClear(true, canDeleteHistory));
                                        } else {
                                            performHistoryClear(true, canDeleteHistory);
                                        }
                                    });
                                } else {
                                    performHistoryClear(revoke, canDeleteHistory);
                                }
                            }
                        }, getResourceProvider());
                        return;
                    }
                    AlertsCreator.createClearOrDeleteDialogAlert(ChatActivity.this, id == clear_history, currentChat, currentUser, currentEncryptedChat != null, true, false, canDeleteHistory, (param) -> {
                        if (id == clear_history && ChatObject.isChannel(currentChat) && (!currentChat.megagroup || ChatObject.isPublic(currentChat))) {
                            getMessagesController().deleteDialog(dialog_id, 2, param);
                        } else {
                            if (id != clear_history) {
                                getNotificationCenter().removeObserver(ChatActivity.this, NotificationCenter.closeChats);
                                getNotificationCenter().postNotificationName(NotificationCenter.closeChats);
                                finishFragment();
                                getNotificationCenter().postNotificationName(NotificationCenter.needDeleteDialog, dialog_id, currentUser, currentChat, param);
                            } else {
                                performHistoryClear(param, canDeleteHistory);
                            }
                        }
                    });
                } else if (id == share_contact) {
                    if (currentUser == null || getParentActivity() == null) {
                        return;
                    }
                    if (addToContactsButton != null && addToContactsButton.getTag() != null) {
                        shareMyContact((Integer) addToContactsButton.getTag(), null);
                    } else {
                        Bundle args = new Bundle();
                        args.putLong("user_id", currentUser.id);
                        args.putBoolean("addContact", true);
                        presentFragment(new ContactAddActivity(args));
                    }
                } else if (id == mute) {
                    toggleMute(false);
                } else if (id == add_shortcut) {
                    try {
                        getMediaDataController().installShortcut(currentUser.id, MediaDataController.SHORTCUT_TYPE_USER_OR_CHAT);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                } else if (id == boost_group) {
                    if (ChatObject.hasAdminRights(currentChat)) {
                        BoostsActivity boostsActivity = new BoostsActivity(dialog_id);
                        boostsActivity.setBoostsStatus(boostsStatus);
                        presentFragment(boostsActivity);
                    } else {
                        getNotificationCenter().postNotificationName(NotificationCenter.openBoostForUsersDialog, dialog_id);
                    }
                } else if (id == report) {
                    ReportBottomSheet.openChat(ChatActivity.this);
                } else if (id == star) {
                    for (int a = 0; a < 2; a++) {
                        for (int b = 0; b < selectedMessagesCanStarIds[a].size(); b++) {
                            MessageObject msg = selectedMessagesCanStarIds[a].valueAt(b);
                            getMediaDataController().addRecentSticker(MediaDataController.TYPE_FAVE, msg, msg.getDocument(), (int) (System.currentTimeMillis() / 1000), !hasUnfavedSelected);
                        }
                    }
                    clearSelectionMode();
                } else if (id == edit) {
                    MessageObject messageObject = null;
                    for (int a = 1; a >= 0; a--) {
                        if (messageObject == null && selectedMessagesIds[a].size() == 1) {
                            ArrayList<Integer> ids = new ArrayList<>();
                            for (int b = 0; b < selectedMessagesIds[a].size(); b++) {
                                ids.add(selectedMessagesIds[a].keyAt(b));
                            }
                            messageObject = messagesDict[a].get(ids.get(0));
                        }
                        selectedMessagesIds[a].clear();
                        selectedMessagesCanCopyIds[a].clear();
                        selectedMessagesCanStarIds[a].clear();
                    }
                    if (messageObject != null && messageObject.isTodo()) {
                        selectedObject = messageObject;
                        processSelectedOption(OPTION_EDIT_TODO);
                    } else {
                        startEditingMessageObject(messageObject);
                    }
                    hideActionMode();
                    updatePinnedMessageView(true);
                    updateVisibleRows();
                } else if (id == edit_quick_reply) {
                    QuickRepliesController.QuickReply currentQuickReply = QuickRepliesController.getInstance(currentAccount).findReply(getQuickReplyId());
                    QuickRepliesActivity.openRenameReplyAlert(getContext(), currentAccount, quickReplyShortcut, currentQuickReply, getResourceProvider(), false, name -> {
                        if (currentQuickReply != null) {
                            QuickRepliesController.getInstance(currentAccount).renameReply(currentQuickReply.id, name);
                        }
                        quickReplyShortcut = name;
                        avatarContainer.setTitle(name);
                    });
                } else if (id == chat_menu_attach) {
                    ActionBarMenuSubItem attach = new ActionBarMenuSubItem(context, false, true, true, getResourceProvider());
                    attach.setTextAndIcon(LocaleController.getString(R.string.AttachMenu), R.drawable.input_attach);
                    attach.setOnClickListener(view -> {
                        headerItem.closeSubMenu();
                        if (chatAttachAlert != null) {
                            chatAttachAlert.setEditingMessageObject(0, null);
                        }
                        openAttachMenu();
                    });
                    headerItem.toggleSubMenu(attach, attachItem.createView());
                } else if (id == bot_help) {
                    getSendMessagesHelper().sendMessage(SendMessagesHelper.SendMessageParams.of("/help", dialog_id, null, null, null, false, null, null, null, true, 0, 0, null, false));
                } else if (id == bot_settings) {
                    getSendMessagesHelper().sendMessage(SendMessagesHelper.SendMessageParams.of("/settings", dialog_id, null, null, null, false, null, null, null, true, 0, 0, null, false));
                } else if (id == search) {
                    openSearchWithText(isSupportedTags() ? "" : null);
                } else if (id == translate) {
                    getMessagesController().getTranslateController().setHideTranslateDialog(getDialogId(), false, true);
                    if (!getMessagesController().getTranslateController().toggleTranslatingDialog(getDialogId(), true)) {
                        updateTopPanel(true);
                    }
                } else if (id == call || id == video_call) {
                    if (currentUser != null && getParentActivity() != null) {
                        VoIPHelper.startCall(currentUser, id == video_call, userInfo != null && userInfo.video_calls_available, getParentActivity(), getMessagesController().getUserFull(currentUser.id), getAccountInstance());
                    }
                } else if (id == text_bold) {
                    if (chatActivityEnterView != null && chatActivityEnterView.getEditField() != null) {
                        chatActivityEnterView.getEditField().setSelectionOverride(editTextStart, editTextEnd);
                        chatActivityEnterView.getEditField().makeSelectedBold();
                    }
                } else if (id == text_italic) {
                    if (chatActivityEnterView != null && chatActivityEnterView.getEditField() != null) {
                        chatActivityEnterView.getEditField().setSelectionOverride(editTextStart, editTextEnd);
                        chatActivityEnterView.getEditField().makeSelectedItalic();
                    }
                } else if (id == text_spoiler) {
                    if (chatActivityEnterView != null && chatActivityEnterView.getEditField() != null) {
                        chatActivityEnterView.getEditField().setSelectionOverride(editTextStart, editTextEnd);
                        chatActivityEnterView.getEditField().makeSelectedSpoiler();
                    }
                } else if (id == text_quote) {
                    if (chatActivityEnterView != null && chatActivityEnterView.getEditField() != null) {
                        chatActivityEnterView.getEditField().setSelectionOverride(editTextStart, editTextEnd);
                        chatActivityEnterView.getEditField().makeSelectedQuote();
                    }
                } else if (id == text_mono) {
                    if (chatActivityEnterView != null && chatActivityEnterView.getEditField() != null) {
                        chatActivityEnterView.getEditField().setSelectionOverride(editTextStart, editTextEnd);
                        chatActivityEnterView.getEditField().makeSelectedMono();
                    }
                } else if (id == text_strike) {
                    if (chatActivityEnterView != null && chatActivityEnterView.getEditField() != null) {
                        chatActivityEnterView.getEditField().setSelectionOverride(editTextStart, editTextEnd);
                        chatActivityEnterView.getEditField().makeSelectedStrike();
                    }
                } else if (id == text_underline) {
                    if (chatActivityEnterView != null && chatActivityEnterView.getEditField() != null) {
                        chatActivityEnterView.getEditField().setSelectionOverride(editTextStart, editTextEnd);
                        chatActivityEnterView.getEditField().makeSelectedUnderline();
                    }
                } else if (id == text_date) {
                    if (chatActivityEnterView != null && chatActivityEnterView.getEditField() != null) {
                        chatActivityEnterView.getEditField().setSelectionOverride(editTextStart, editTextEnd);
                        chatActivityEnterView.getEditField().makeSelectedDate();
                    }
                } else if (id == text_link) {
                    if (chatActivityEnterView != null && chatActivityEnterView.getEditField() != null) {
                        chatActivityEnterView.getEditField().setSelectionOverride(editTextStart, editTextEnd);
                        chatActivityEnterView.getEditField().makeSelectedUrl();
                    }
                } else if (id == text_regular) {
                    if (chatActivityEnterView != null && chatActivityEnterView.getEditField() != null) {
                        chatActivityEnterView.getEditField().setSelectionOverride(editTextStart, editTextEnd);
                        chatActivityEnterView.getEditField().makeSelectedRegular();
                    }
                } else if (id == change_colors) {
                    showChatThemeBottomSheet();
                } else if (id == topic_close) {
                    if (forumTopic == null)
                        return;
                    getMessagesController().getTopicsController().toggleCloseTopic(currentChat.id, forumTopic.id, forumTopic.closed = true);
                    updateTopicButtons();
                    updateBottomOverlay();
                    updateTopPanel(true);
                } else if (id == open_forum) {
                    TopicsFragment.prepareToSwitchAnimation(ChatActivity.this);
//                    Bundle bundle = new Bundle();
//                    bundle.putLong("chat_id", -dialog_id);
//                    presentFragment(new TopicsFragment(bundle));
                } else if (id == copy_business_link) {
                    AndroidUtilities.addToClipboard(businessLink.link);
                    BulletinFactory.of(LaunchActivity.getLastFragment()).createCopyLinkBulletin().show();
                } else if (id == share_business_link) {
                    Runnable shareTask = () -> {
                        Intent intent = new Intent(getContext(), LaunchActivity.class);
                        intent.setAction(Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        intent.putExtra(Intent.EXTRA_TEXT, businessLink.link);
                        startActivityForResult(intent, 500);
                    };
                    if (chatActivityEnterView.businessLinkHasChanges()) {
                        showBusinessLinksDiscardAlert(shareTask);
                    } else {
                        shareTask.run();
                    }
                } else if (id == rename_business_link) {
                    BusinessLinksActivity.openRenameAlert(getContext(), currentAccount, businessLink, resourceProvider, false);
                } else if (id == delete_business_link) {
                    AlertDialog dialog = new AlertDialog.Builder(getContext(), getResourceProvider())
                            .setTitle(getString(R.string.BusinessLinksDeleteTitle))
                            .setMessage(getString(R.string.BusinessLinksDeleteMessage))
                            .setPositiveButton(getString(R.string.Remove), (di, w) -> {
                                finishFragment();
                                getNotificationCenter().postNotificationName(NotificationCenter.needDeleteBusinessLink, businessLink);
                            })
                            .setNegativeButton(getString(R.string.Cancel), null)
                            .create();
                    showDialog(dialog);
                    TextView button = (TextView) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    if (button != null) {
                        button.setTextColor(getThemedColor(Theme.key_text_RedBold));
                    }
                } else if (id == chat_menu_topic_create) {
                    presentFragment(TopicCreateFragment.create(-dialog_id, 0).setOpenInChatActivity(ChatActivity.this));
                } else if (id == 888) {
                    dumpCanvas();
                } else if (id == 889) {
                    sendDebugRichMessage();
                }
            }
        });
        View backButton = actionBar.getBackButton();
        backButton.setOnTouchListener(new LongPressListenerWithMovingGesture() {
            @Override
            public void onLongPress() {
                scrimPopupWindow = BackButtonMenu.show(ChatActivity.this, backButton, dialog_id, getTopicId(), themeDelegate);
                if (scrimPopupWindow != null) {
                    setSubmenu(scrimPopupWindow);
                    scrimPopupWindow.setOnDismissListener(() -> {
                        setSubmenu(null);
                        scrimPopupWindow = null;
                        menuDeleteItem = null;
                        scrimPopupWindowItems = null;
                        chatLayoutManager.setCanScrollVertically(true);
                        if (scrimPopupWindowHideDimOnDismiss) {
                            dimBehindView(false);
                        } else {
                            scrimPopupWindowHideDimOnDismiss = true;
                        }
                        if (chatActivityEnterView != null && chatActivityEnterView.getEditField() != null) {
                            chatActivityEnterView.getEditField().setAllowDrawCursor(true);
                        }
                    });
                    chatListView.stopScroll();
                    chatLayoutManager.setCanScrollVertically(false);
                    dimBehindView(backButton, 0.3f);
                    hideHints(false);
                    if (topUndoView != null) {
                        topUndoView.hide(true, 1);
                    }
                    if (undoView != null) {
                        undoView.hide(true, 1);
                    }
                    if (chatActivityEnterView != null && chatActivityEnterView.getEditField() != null) {
                        chatActivityEnterView.getEditField().setAllowDrawCursor(false);
                    }
                }
            }
        });
        actionBar.setInterceptTouchEventListener((view, motionEvent) -> {
            if (chatThemeBottomSheet != null) {
                chatThemeBottomSheet.close();
                return true;
            }
            return false;
        });

        topPanelLayout = new ChatActivityTopPanelLayout(context);
        topPanelLayout.setOnAnimatedHeightChangedListener(() -> {
            invalidateChatListViewTopPadding();
            invalidateMessagesVisiblePart();
            checkUi_messagesSearchListPadding();
            checkUi_topFade();
        });
        if (avatarContainer != null) {
            avatarContainer.onDestroy();
        }
        avatarContainer = new ChatAvatarContainer(context, this, currentEncryptedChat != null, themeDelegate) {
            @Override
            protected boolean onAvatarClick() {
                if (currentUser != null && currentUser.linked_community_id != 0) {
                    showDialog(new CommunitySheet(ChatActivity.this, currentUser.linked_community_id));
                    return true;
                } else if (currentChat != null && currentChat.linked_community_id != 0) {
                    showDialog(new CommunitySheet(ChatActivity.this, currentChat.linked_community_id));
                    return true;
                }
                return false;
            }

            @Override
            protected boolean useAnimatedSubtitle() {
                return chatMode == MODE_SAVED;
            }

            @Override
            protected boolean canSearch() {
                return !isInsideContainer && !isInPreviewMode() && !inBubbleMode && searchItem != null && !searching && (!isThreadChat() || isTopic);
            }

            @Override
            protected void openSearch() {
                openSearchWithText(isSupportedTags() ? "" : null);
            }
        };
        avatarContainer.setGlassMode();
        avatarContainer.allowShorterStatus = true;
        avatarContainer.premiumIconHiddable = true;
        avatarContainer.allowDrawStories = dialog_id < 0 && !isTopic;
        avatarContainer.setClipChildren(false);
        updateTopicTitleIcon();
        if (inPreviewMode || inBubbleMode || isInsideContainer) {
            avatarContainer.setOccupyStatusBar(false);
        }
        if (isReport()) {
            actionBar.setTitle(reportTitle);
            actionBar.setSubtitle(getString(R.string.ReportSelectMessages));
        } else if (startLoadFromDate != 0) {
            final int date = startLoadFromDate;
            actionBar.setOnClickListener((v) -> {
                jumpToDate(date);
            });
            actionBar.setTitle(LocaleController.formatDateChat(startLoadFromDate, false));
            actionBar.setSubtitle(getString(R.string.Loading));

            TLRPC.TL_messages_getHistory gh1 = new TLRPC.TL_messages_getHistory();
            gh1.peer = getMessagesController().getInputPeer(dialog_id);
            gh1.offset_date = startLoadFromDate;
            gh1.limit = 1;
            gh1.add_offset = -1;

            int req = getConnectionsManager().sendRequest(gh1, (response, error) -> {
                if (response instanceof TLRPC.messages_Messages) {
                    List<TLRPC.Message> l = ((TLRPC.messages_Messages) response).messages;
                    if (!l.isEmpty()) {

                        TLRPC.TL_messages_getHistory gh2 = new TLRPC.TL_messages_getHistory();
                        gh2.peer = getMessagesController().getInputPeer(dialog_id);
                        gh2.offset_date = startLoadFromDate + 60 * 60 * 24;
                        gh2.limit = 1;

                        getConnectionsManager().sendRequest(gh2, (response1, error1) -> {
                            if (response1 instanceof TLRPC.messages_Messages) {
                                List<TLRPC.Message> l2 = ((TLRPC.messages_Messages) response1).messages;
                                int count = 0;
                                if (!l2.isEmpty()) {
                                    count = ((TLRPC.messages_Messages) response).offset_id_offset - ((TLRPC.messages_Messages) response1).offset_id_offset;
                                } else {
                                    count = ((TLRPC.messages_Messages) response).offset_id_offset;
                                }
                                int finalCount = count;
                                AndroidUtilities.runOnUIThread(() -> {
                                    if (finalCount != 0) {
                                        AndroidUtilities.runOnUIThread(() -> actionBar.setSubtitle(LocaleController.formatPluralString("messages", finalCount)));
                                    } else {
                                        actionBar.setSubtitle(getString(R.string.NoMessagesForThisDay));
                                    }
                                });
                            }
                        });
                    } else {
                        actionBar.setSubtitle(getString(R.string.NoMessagesForThisDay));
                    }
                }
            });
            getConnectionsManager().bindRequestToGuid(req, classGuid);
        } else {
            actionBar.addView(avatarContainer, 0, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT, !inPreviewMode ? 52 : 0, 0, 52, 0));
            actionBar.createMenu().bringToFront();
        }
        actionBar.setOnActionModeFactorChangeListener(() -> {
            checkUi_avatarContainerVisibility();
        });

        ActionBarMenu menu = actionBar.createMenu();

        if (chatMode == MODE_QUICK_REPLIES && !QuickRepliesController.isSpecial(quickReplyShortcut)) {
            menu.addItem(edit_quick_reply, R.drawable.group_edit).setContentDescription(LocaleController.getString(R.string.Edit));
        }

        if (UserObject.isBotForumWithEditableTopics(currentUser) && chatMode == 0) {
            topicCreateItem = menu.addItem(chat_menu_topic_create, R.drawable.menu_topic_add_30);
        }

        if (currentEncryptedChat == null && (chatMode == 0 || chatMode == MODE_SAVED || chatMode == MODE_SUGGESTIONS) && !isReport()) {
            searchIconItem = menu.addItem(search, isSupportedTags() ? R.drawable.navbar_search_tag : R.drawable.outline_header_search);
            searchIconItem.setContentDescription(LocaleController.getString(R.string.Search));
            searchItem = menu.addItem(chat_menu_search, R.drawable.outline_header_search, themeDelegate);
            searchItem.setSearchPaddingStart(7);
            searchItem.setIsSearchField(true);
            searchItem.setActionBarMenuItemSearchListener(getSearchItemListener());
            searchItem.setSearchFieldHint(isSupportedTags() ? LocaleController.getString(R.string.SavedTagSearchHint) : LocaleController.getString(R.string.Search));
            if (chatMode == MODE_SAVED || chatMode == MODE_SUGGESTIONS || threadMessageId == 0 && !UserObject.isReplyUser(currentUser) || threadMessageObject != null && threadMessageObject.getRepliesCount() < 10) {
                searchItem.setVisibility(View.GONE);
            } else {
                searchItem.setVisibility(View.VISIBLE);
            }
            searchItemVisible = false;
        }

        if (chatMode == 0 && (threadMessageId == 0 || isTopic) && !UserObject.isReplyUser(currentUser) && !isReport()) {
            TLRPC.UserFull userFull = null;
            if (currentUser != null) {
                audioCallIconItem = menu.lazilyAddItem(call, R.drawable.call, themeDelegate);
                audioCallIconItem.setContentDescription(LocaleController.getString(R.string.Call));
                userFull = getMessagesController().getUserFull(currentUser.id);
                if (userFull != null && userFull.phone_calls_available) {
                    showAudioCallAsIcon = !inPreviewMode;
                    audioCallIconItem.setVisibility(View.VISIBLE);
                } else {
                    showAudioCallAsIcon = false;
                    audioCallIconItem.setVisibility(View.GONE);
                }
            }
        }
        /*
        Choreographer60FpsContent.getInstance().addFrameCallback(justForTest = () -> {
            if (audioCallIconItem != null) {
                showAudioCallAsIcon = !showAudioCallAsIcon;
                audioCallIconItem.setVisibility(!showAudioCallAsIcon ? View.GONE : View.VISIBLE);
            }
        }, 1);
        */

        editTextItem = menu.lazilyAddItem(chat_menu_edit_text_options, R.drawable.ic_ab_other, themeDelegate);
        editTextItem.setContentDescription(LocaleController.getString(R.string.AccDescrMoreOptions));
        editTextItem.setTag(null);
        editTextItem.setVisibility(View.GONE);

        otherIcon = new ComposeDrawable(
            context.getResources().getDrawable(R.drawable.ic_ab_other).mutate(),
            context.getResources().getDrawable(R.drawable.mini_attach).mutate()
        );
        otherIcon.setIconTranslate(-dp(6), dp(6.66f));

        if (((chatMode == 0 && (threadMessageId == 0 || isTopic)) || chatMode == MODE_SUGGESTIONS) && !UserObject.isReplyUser(currentUser) && !isReport()) {
            TLRPC.UserFull userFull = null;
            if (currentUser != null) {
                userFull = getMessagesController().getUserFull(currentUser.id);
            }
            headerItem = menu.addItem(chat_menu_options, otherIcon);
            headerItem.setSubMenuDelegate(new ActionBarMenuItem.ActionBarSubMenuItemDelegate() {
                @Override
                public void onShowSubMenu() {
                    updateScrimSourceBitmap();
                }

                @Override
                public void onHideSubMenu() {

                }
            });
            otherIcon.addView(headerItem.getIconView());
            headerItem.setContentDescription(LocaleController.getString(R.string.AccDescrMoreOptions));

            if (currentUser != null && currentUser.self && chatMode != MODE_SAVED) {
                savedChatsItem = headerItem.lazilyAddSubItem(view_as_topics, R.drawable.msg_topics, LocaleController.getString(R.string.SavedViewAsChats));
                savedChatsGap = headerItem.lazilyAddColoredGap();
                savedChatsItem.setVisibility(getMessagesController().getSavedMessagesController().hasDialogs() ? View.VISIBLE : View.GONE);
                savedChatsGap.setVisibility(getMessagesController().getSavedMessagesController().hasDialogs() ? View.VISIBLE : View.GONE);
            } else if (chatMode != MODE_SAVED && (currentUser == null || !currentUser.self)) {
                chatNotificationsPopupWrapper = new ChatNotificationsPopupWrapper(context, currentAccount, headerItem.getPopupLayout().getSwipeBack(), false, false, new ChatNotificationsPopupWrapper.Callback() {
                    @Override
                    public void dismiss() {
                        headerItem.toggleSubMenu();
                    }

                    @Override
                    public void toggleSound() {
                        SharedPreferences preferences = MessagesController.getNotificationsSettings(currentAccount);
                        boolean enabled = !preferences.getBoolean("sound_enabled_" + NotificationsController.getSharedPrefKey(dialog_id, getTopicId()), true);
                        preferences.edit().putBoolean("sound_enabled_" + NotificationsController.getSharedPrefKey(dialog_id, getTopicId()), enabled).apply();
                        if (BulletinFactory.canShowBulletin(ChatActivity.this)) {
                            BulletinFactory.createSoundEnabledBulletin(ChatActivity.this, enabled ? NotificationsController.SETTING_SOUND_ON : NotificationsController.SETTING_SOUND_OFF, getResourceProvider()).show();
                        }
                        updateTitleIcons();
                    }

                    @Override
                    public void muteFor(int timeInSeconds) {
                        if (timeInSeconds == 0) {
                            if (getMessagesController().isDialogMuted(dialog_id, getTopicId())) {
                                ChatActivity.this.toggleMute(true);
                            }
                            if (BulletinFactory.canShowBulletin(ChatActivity.this)) {
                                BulletinFactory.createMuteBulletin(ChatActivity.this, NotificationsController.SETTING_MUTE_UNMUTE, timeInSeconds, getResourceProvider()).show();
                            }
                        } else {
                            getNotificationsController().muteUntil(dialog_id, getTopicId(), timeInSeconds);
                            if (BulletinFactory.canShowBulletin(ChatActivity.this)) {
                                BulletinFactory.createMuteBulletin(ChatActivity.this, NotificationsController.SETTING_MUTE_CUSTOM, timeInSeconds, getResourceProvider()).show();
                            }
                        }
                    }

                    @Override
                    public void showCustomize() {
                        if (dialog_id != 0 && chatMode != MODE_SAVED) {
                            if (currentUser != null) {
                                getMessagesController().putUser(currentUser, true);
                            }
                            Bundle args = new Bundle();
                            args.putLong("dialog_id", dialog_id);
                            if (getTopicId() != 0) {
                                args.putLong("topic_id", getTopicId());
                            }
                            presentFragment(new ProfileNotificationsActivity(args, themeDelegate));
                        }
                    }

                    @Override
                    public void toggleMute() {
                        ChatActivity.this.toggleMute(true);
                        BulletinFactory.createMuteBulletin(ChatActivity.this, getMessagesController().isDialogMuted(dialog_id, getTopicId()), themeDelegate).show();
                    }
                }, getResourceProvider());
                muteItem = headerItem.lazilyAddSwipeBackItem(R.drawable.msg_mute, null, null, chatNotificationsPopupWrapper.windowLayout);
                muteItem.setOnClickListener(view -> {
                    boolean muted = MessagesController.getInstance(currentAccount).isDialogMuted(dialog_id, getTopicId());
                    if (muted) {
                        updateTitleIcons(true);
                        AndroidUtilities.runOnUIThread(() -> {
                            ChatActivity.this.toggleMute(true);
                        }, 150);
                        headerItem.toggleSubMenu();
                        if (ChatActivity.this.getParentActivity() != null) {
                            BulletinFactory.createMuteBulletin(ChatActivity.this, false, themeDelegate).show();
                        }
                    } else {
                        muteItem.openSwipeBack();
                    }
                });
                muteItemGap = headerItem.lazilyAddColoredGap();
            }
            if (currentChat != null) {
                headerItem.lazilyAddSubItem(open_direct, R.drawable.msg_markunread, getString(R.string.ChannelOpenDirect));
                headerItem.setSubItemShown(open_direct, ChatObject.isChannel(currentChat) && !ChatObject.isMonoForum(currentChat) && currentChat.linked_monoforum_id != 0 && ChatObject.canManageMonoForum(currentAccount, -currentChat.linked_monoforum_id));
            }
            if (currentUser != null && chatMode != MODE_SAVED) {
                headerItem.lazilyAddSubItem(call, R.drawable.msg_callback, LocaleController.getString(R.string.Call));
                headerItem.lazilyAddSubItem(video_call, R.drawable.msg_videocall, LocaleController.getString(R.string.VideoCall));
                if (userFull != null && userFull.phone_calls_available) {
                    headerItem.showSubItem(call);
                    if (userFull.video_calls_available) {
                        headerItem.showSubItem(video_call);
                    } else {
                        headerItem.hideSubItem(video_call);
                    }
                } else {
                    headerItem.hideSubItem(call);
                    headerItem.hideSubItem(video_call);
                }
            }

            if (searchItem != null) {
                headerItem.lazilyAddSubItem(search, R.drawable.msg_search, LocaleController.getString(R.string.Search));
            }
            if (ChatObject.isBoostSupported(currentChat) && (getUserConfig().isPremium() || ChatObject.isBoosted(chatInfo) || ChatObject.hasAdminRights(currentChat))) {
                RLottieDrawable drawable = new RLottieDrawable(R.raw.boosts, "" + R.raw.boosts, dp(24), dp(24));
                headerItem.lazilyAddSubItem(boost_group, drawable, LocaleController.getString(ChatObject.isChannelAndNotMegaGroup(currentChat) ? R.string.BoostingBoostChannelMenu : R.string.BoostingBoostGroupMenu));
            }
            translateItem = headerItem.lazilyAddSubItem(translate, R.drawable.msg_translate, LocaleController.getString(R.string.TranslateMessage));
            updateTranslateItemVisibility();
            if (currentChat != null && !currentChat.creator && !ChatObject.hasAdminRights(currentChat)) {
                headerItem.lazilyAddSubItem(report, R.drawable.msg_report, LocaleController.getString(R.string.ReportChat));
            }
            if (currentUser != null && currentUser.id != UserObject.VERIFY && currentUser.id != UserObject.REPLY_BOT) {
                addContactItem = headerItem.lazilyAddSubItem(share_contact, R.drawable.msg_addcontact, LocaleController.getString(R.string.AddToContacts));
            }
            if (currentEncryptedChat != null) {
                timeItem2 = headerItem.lazilyAddSubItem(chat_enc_timer, R.drawable.msg_autodelete, LocaleController.getString(R.string.SetTimer));
            }
            if (currentChat != null && !isTopic) {
                viewAsTopics = headerItem.lazilyAddSubItem(view_as_topics, R.drawable.msg_topics, LocaleController.getString(R.string.TopicViewAsTopics));
            }
            if (themeDelegate.isThemeChangeAvailable(true)) {
                headerItem.lazilyAddSubItem(change_colors, R.drawable.msg_background, LocaleController.getString(R.string.SetWallpapers));
            }
            if (currentUser != null && currentUser.self && getDialogId() != UserObject.VERIFY) {
                headerItem.lazilyAddSubItem(add_shortcut, R.drawable.msg_home, LocaleController.getString(R.string.AddShortcut));
            }
            if (!isTopic && !ChatObject.isMonoForum(currentChat)) {
                clearHistoryItem = headerItem.lazilyAddSubItem(clear_history, R.drawable.msg_clear,
                    LocaleController.getString(UserObject.isBotForum(currentUser) ? R.string.ClearAllHistory : R.string.ClearHistory));
            }
            boolean addedSettings = false;
            if (!isTopic) {
                if (ChatObject.isChannel(currentChat) && !currentChat.creator) {
                    if (!ChatObject.isNotInChat(currentChat)) {
                        if (currentChat.monoforum) {
                            headerItem.lazilyAddSubItem(delete_chat, R.drawable.msg_leave, LocaleController.getString(R.string.LeaveConversationMenu));
                        } else if (currentChat.megagroup) {
                            headerItem.lazilyAddSubItem(delete_chat, R.drawable.msg_leave, LocaleController.getString(R.string.LeaveMegaMenu));
                        } else {
                            headerItem.lazilyAddSubItem(delete_chat, R.drawable.msg_leave, LocaleController.getString(R.string.LeaveChannelMenu));
                        }
                    }
                } else if (!ChatObject.isChannel(currentChat) && getDialogId() != UserObject.VERIFY) {
                    if (currentChat != null) {
                        headerItem.lazilyAddSubItem(delete_chat, R.drawable.msg_leave, LocaleController.getString(R.string.DeleteAndExit));
                    } else if (currentUser != null && currentUser.bot) {
                        headerItem.lazilyAddSubItem(bot_settings, R.drawable.msg_settings_old, LocaleController.getString(R.string.BotSettings));
                        addedSettings = true;
                        headerItem.lazilyAddSubItem(bot_help, R.drawable.msg_help, LocaleController.getString(R.string.BotHelp));
                        if (!MessagesController.isSupportUser(currentUser)) {
                            headerItem.lazilyAddSubItem(report, R.drawable.msg_report, LocaleController.getString(R.string.ReportBot)).setColors(getThemedColor(Theme.key_text_RedRegular), getThemedColor(Theme.key_text_RedRegular));
                        }
                        headerItem.lazilyAddSubItem(delete_chat, R.drawable.msg_block2, LocaleController.getString(R.string.DeleteAndBlock)).setColors(getThemedColor(Theme.key_text_RedRegular), getThemedColor(Theme.key_text_RedRegular));
                        updateBotButtons();
                    } else {
                        headerItem.lazilyAddSubItem(delete_chat, R.drawable.msg_delete, LocaleController.getString(R.string.DeleteChatUser));
                    }
                }
            }
            if (ChatObject.isMonoForum(currentChat) && ChatObject.canManageMonoForum(currentAccount, currentChat)) {
                headerItem.lazilyAddSubItem(remove_fee, R.drawable.menu_paid_off, getString(R.string.DirectRemoveFee));
                headerItem.lazilyAddSubItem(charge_fee, R.drawable.menu_feature_paid, getString(R.string.DirectChargeFee));
                headerItem.setSubItemShown(remove_fee, false);
                headerItem.setSubItemShown(charge_fee, false);

                feeItemGap = headerItem.lazilyAddColoredGap();
                feeItemText = headerItem.lazilyAddText("", 13);
                feeItemGap.setVisibility(View.GONE);
                feeItemText.setVisibility(View.GONE);
            }
        } else if (chatMode == MODE_EDIT_BUSINESS_LINK) {
            headerItem = menu.addItem(chat_menu_options, otherIcon);
            otherIcon.addView(headerItem.getIconView());
            headerItem.setContentDescription(LocaleController.getString(R.string.AccDescrMoreOptions));

            headerItem.lazilyAddSubItem(copy_business_link, R.drawable.msg_copy, getString(R.string.Copy));
            headerItem.lazilyAddSubItem(share_business_link, R.drawable.msg_share, getString(R.string.LinkActionShare));
            headerItem.lazilyAddSubItem(rename_business_link, R.drawable.msg_edit, getString(R.string.Rename));
            headerItem.lazilyAddSubItem(delete_business_link, R.drawable.msg_delete, getString(R.string.Delete)).setColors(Theme.getColor(Theme.key_text_RedRegular), Theme.getColor(Theme.key_text_RedRegular));
        }
        if (ChatObject.isForum(currentChat) && isTopic && getParentLayout() != null && getParentLayout().getFragmentStack() != null && chatMode == MODE_DEFAULT) {
            boolean hasMyForum = false;
            for (int i = 0; i < getParentLayout().getFragmentStack().size(); ++i) {
                BaseFragment fragment = getParentLayout().getFragmentStack().get(i);
                if (fragment instanceof TopicsFragment && ((TopicsFragment) fragment).getDialogId() == dialog_id) {
                    hasMyForum = true;
                    break;
                }
            }

            if (!hasMyForum) {
                openForumItem = headerItem.lazilyAddSubItem(open_forum, R.drawable.msg_discussion, LocaleController.getString(R.string.OpenAllTopics));
            }
        }
        if (currentChat != null && forumTopic != null && chatMode == 0) {
            closeTopicItem = headerItem.lazilyAddSubItem(topic_close, R.drawable.msg_topic_close, LocaleController.getString(R.string.CloseTopic));
            closeTopicItem.setVisibility(currentChat != null && ChatObject.canManageTopic(currentAccount, currentChat, forumTopic) && forumTopic != null && !forumTopic.closed ? View.VISIBLE : View.GONE);
        }
        menu.setVisibility(inMenuMode ? View.GONE : View.VISIBLE);

        updateTitle(false);
        avatarContainer.updateOnlineCount();
        avatarContainer.updateSubtitle();
        updateTitleIcons();

        if (chatMode == 0 && (!isThreadChat() || isTopic) && !isReport()) {
            attachItem = menu.lazilyAddItem(chat_menu_attach, otherIcon, themeDelegate);
            attachItem.onView(cell -> otherIcon.addView(cell.getIconView()));
            attachItem.setOverrideMenuClick(true);
            attachItem.setAllowCloseAnimation(false);
            attachItem.setContentDescription(LocaleController.getString(R.string.AccDescrMoreOptions));
            attachItem.setVisibility(View.GONE);
        }

        if (inPreviewMode) {
            if (headerItem != null) {
                headerItem.setAlpha(0.0f);
            }
            if (attachItem != null) {
                attachItem.setAlpha(0.0f);
            }
        }

        if (BuildConfig.DEBUG_PRIVATE_VERSION && headerItem != null) {
            headerItem.lazilyAddSubItem(888, R.drawable.menu_download_round, "Dump Canvas");
        }

        actionModeViews.clear();
        selectedMessagesCountTextView = null;
        checkActionBarMenu(false);

        scrimPaint = new Paint();

        if (chatListThanosEffect != null) {
            AndroidUtilities.removeFromParent(chatListThanosEffect);
            chatListThanosEffect = null;
        }
        removingFromParent = false;
        fragmentView = contentView = new ChatActivityFragmentView(context, parentLayout);
        invalidateBlurredSourcesView = new OnPostDrawView(context, true, this::invalidateMergedVisibleBlurredPositionsAndSourcesImpl);
        contentView.addView(invalidateBlurredSourcesView);

        viewPositionWatcher = new ViewPositionWatcher(contentView);

        final ViewGroup parentView = parentChatActivity != null ? parentChatActivity.contentView : contentView;
        glassBackgroundDrawableFactory.setSourceRootView(viewPositionWatcher, parentView);
        glassBackgroundDrawableFactoryFrosted.setSourceRootView(viewPositionWatcher, parentView);
        navbarContentDrawableFactory.setSourceRootView(viewPositionWatcher, parentView);
        scrimBlur3Factory.setSourceRootView(viewPositionWatcher, parentView);

        if (headerItem != null) {
            headerItem.setBlurredBackgroundFactory(scrimBlur3Factory, BlurredBackgroundProviderImpl.messageMenuBackground(resourceProvider));
        }

        contentView.setOccupyStatusBar(!inBubbleMode && !isInsideContainer && !inPreviewMode);

        actionBar.setupGlass(
            glassBackgroundDrawableFactory,
            BlurredBackgroundProviderImpl.topPanelChatActivity(themeDelegate),
            ChatObject.isForum(currentChat));

        if (chatMode == MODE_PINNED) {
            actionBar.setChatAvatarContainer(avatarContainer);
            avatarContainer.setActionBar(actionBar);
        } else if (chatMode == MODE_WELCOME_MESSAGES) {
            actionBar.setChatAvatarContainer(avatarContainer);
            actionBar.setForcedMenuWidth(dp(46));
            actionBar.doNotDrawGlassMenu = true;
            avatarContainer.setActionBar(actionBar);
        } else if (isComments) {
            actionBar.setChatAvatarContainer(avatarContainer);
            actionBar.setForcedMenuMinWidth(dp(46));
            avatarContainer.setActionBar(actionBar);
        }

        chatInputViewsContainer = new ChatInputViewsContainer(context);
        chatInputViewsContainer.setClipChildren(false);
        chatInputViewsContainer.setWindowInsetsProvider(windowInsetsStateHolder);
        chatInputViewsContainer.setInputIslandBubbleDrawable(
            glassBackgroundDrawableFactory.create(chatInputViewsContainer, blurredBackgroundColorProvider));
        chatInputViewsContainer.setUnderKeyboardBackgroundDrawable(
            glassBackgroundDrawableFactoryFrosted.create(chatInputViewsContainer, blurredBackgroundColorProvider));


        chatInputBubbleContainer = chatInputViewsContainer.getInputIslandBubbleContainer();
        chatInputBubbleContainer.setClipChildren(false);

        chatInputInAppContainer = chatInputViewsContainer.getInAppKeyboardBubbleContainer();

        updateBackground();

        emptyViewContainer = null;

        CharSequence oldMessage;
        if (chatActivityEnterView != null) {
            chatActivityEnterView.onDestroy();
            if (!chatActivityEnterView.isEditingMessage()) {
                oldMessage = chatActivityEnterView.getFieldText();
            } else {
                oldMessage = null;
            }
        } else {
            oldMessage = null;
        }
        if (mentionContainer != null && mentionContainer.getAdapter() != null) {
            mentionContainer.getAdapter().onDestroy();
        }

        chatListView = new ChatListRecyclerView(context, themeDelegate) {
            private int lastWidth;

            private final ArrayList<ChatMessageCell> drawTimeAfter = new ArrayList<>();
            private final ArrayList<ChatMessageCell> drawNamesAfter = new ArrayList<>();
            private final ArrayList<ChatMessageCell> drawCaptionAfter = new ArrayList<>();
            private final ArrayList<ChatMessageCell> drawReactionsAfter = new ArrayList<>();
            private final ArrayList<MessageObject.GroupedMessages> drawingGroups = new ArrayList<>(10);

            private int startedTrackingX;
            private int startedTrackingY;
            private int startedTrackingPointerId;
            private long lastTrackingAnimationTime;
            private float trackAnimationProgress;
            private float endTrackingX;
            private boolean wasTrackingVibrate;

            private float springMultiplier = 2000f;

            private Paint outlineActionBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private Paint outlineActionBackgroundDarkenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

            private FloatValueHolder slidingDrawableVisibilityProgress = new FloatValueHolder(0);
            private SpringAnimation slidingDrawableVisibilitySpring = new SpringAnimation(slidingDrawableVisibilityProgress)
                    .setMinValue(0f)
                    .setMaxValue(springMultiplier)
                    .setSpring(new SpringForce(0)
                            .setStiffness(SpringForce.STIFFNESS_MEDIUM)
                            .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
                    .addUpdateListener((animation, value, velocity) -> invalidate());
            private FloatValueHolder slidingFillProgress = new FloatValueHolder(0);
            private SpringAnimation slidingFillProgressSpring = new SpringAnimation(slidingFillProgress)
                    .setMinValue(0f)
                    .setSpring(new SpringForce(0)
                            .setStiffness(400f)
                            .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY))
                    .addUpdateListener((animation, value, velocity) -> invalidate());
            private FloatValueHolder slidingOuterRingProgress = new FloatValueHolder(0);
            private SpringAnimation slidingOuterRingSpring = new SpringAnimation(slidingOuterRingProgress)
                    .setMinValue(0f)
                    .setSpring(new SpringForce(0)
                            .setStiffness(200f)
                            .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
                    .addUpdateListener((animation, value, velocity) -> invalidate());
            private boolean slidingBeyondMax;
            private Path path = new Path();

            private boolean ignoreLayout;
            private boolean invalidated;

            int lastH = 0;

            {
                outlineActionBackgroundPaint.setStyle(Paint.Style.STROKE);
                outlineActionBackgroundPaint.setStrokeCap(Paint.Cap.ROUND);
                outlineActionBackgroundPaint.setStrokeWidth(AndroidUtilities.dp(2));
                outlineActionBackgroundDarkenPaint.setStyle(Paint.Style.STROKE);
                outlineActionBackgroundDarkenPaint.setStrokeCap(Paint.Cap.ROUND);
                outlineActionBackgroundDarkenPaint.setStrokeWidth(AndroidUtilities.dp(2));
            }

            @Override
            public void requestLayout() {
                if (ignoreLayout) {
                    return;
                }
                botDraftHeightController.onRequestLayout();
                super.requestLayout();
            }

            @Override
            public void setTranslationY(float translationY) {
                if (translationY != getTranslationY()) {
                    super.setTranslationY(translationY);
                    invalidateChatListViewTopPadding();
                    invalidateMessagesVisiblePart();
                }
            }

            @Override
            protected boolean allowSelectChildAtPosition(View child) {
                if (child != null && (child.getVisibility() == View.INVISIBLE || child.getVisibility() == View.GONE)) return false;
                return super.allowSelectChildAtPosition(child);
            }

            @Override
            protected void onMeasure(int widthSpec, int heightSpec) {
//                saveScrollPosition();
                super.onMeasure(widthSpec, heightSpec);
            }

            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                super.onLayout(changed, l, t, r, b);

                if (lastWidth != r - l) {
                    if (lastWidth != 0) {
                        hideHints(false);
                    }
                    lastWidth = r - l;
                }

                int height = getMeasuredHeight();
                if (lastH != height) {
                    ignoreLayout = true;
                    if (chatListItemAnimator != null) {
                        chatListItemAnimator.endAnimations();
                    }
                    chatScrollHelper.cancel();
                    ignoreLayout = false;
                    lastH = height;
                }

                forceScrollToTop = false;
                if (textSelectionHelper != null && textSelectionHelper.isInSelectionMode()) {
                    textSelectionHelper.invalidate();
                }
                invalidateClipRectForBackgroundAndChatList();
                isSkeletonVisible();
            }

            private void setGroupTranslationX(ChatMessageCell view, float dx) {
                MessageObject.GroupedMessages group = view.getCurrentMessagesGroup();
                if (group == null) {
                    return;
                }
                int count = getChildCount();
                for (int a = 0; a < count; a++) {
                    View child = getChildAt(a);
                    if (child == view || !(child instanceof ChatMessageCell)) {
                        continue;
                    }
                    ChatMessageCell cell = (ChatMessageCell) child;
                    if (cell.getCurrentMessagesGroup() == group) {
                        cell.setSlidingOffset(dx);
                        cell.invalidate();
                    }
                }
                invalidate();
            }

            @Override
            public boolean requestChildRectangleOnScreen(View child, Rect rect, boolean immediate) {
                if (scrimPopupWindow != null) {
                    return false;
                }
                return super.requestChildRectangleOnScreen(child, rect, immediate);
            }

            @Override
            public boolean onInterceptTouchEvent(MotionEvent e) {
                textSelectionHelper.checkSelectionCancel(e);
                if (isFastScrollAnimationRunning()) {
                    return false;
                }
                if (quickShareSelectorOverlay != null && quickShareSelectorOverlay.isActive()) {
                    return false;
                }
                boolean result = super.onInterceptTouchEvent(e);
                if (actionBar.isActionModeShowed() || isReport()) {
                    return result;
                }
                processTouchEvent(e);
                return result;
            }

            @Override
            public void setItemAnimator(ItemAnimator animator) {
                if (isFastScrollAnimationRunning()) {
                    return;
                }
                super.setItemAnimator(animator);
            }

            private void drawReplyButton(Canvas canvas) {
                if (slidingView == null || Thread.currentThread() != Looper.getMainLooper().getThread()) {
                    return;
                }
                Paint chatActionBackgroundPaint = getThemedPaint(Theme.key_paint_chatActionBackground);
                Paint chatActionBackgroundDarkenPaint = Theme.chat_actionBackgroundGradientDarkenPaint;
                if (outlineActionBackgroundPaint.getColor() != chatActionBackgroundPaint.getColor()) {
                    outlineActionBackgroundPaint.setColor(chatActionBackgroundPaint.getColor());
                }
                if (outlineActionBackgroundDarkenPaint.getColor() != chatActionBackgroundDarkenPaint.getColor()) {
                    outlineActionBackgroundDarkenPaint.setColor(chatActionBackgroundDarkenPaint.getColor());
                }
                if (outlineActionBackgroundPaint.getShader() != chatActionBackgroundPaint.getShader()) {
                    outlineActionBackgroundPaint.setShader(chatActionBackgroundPaint.getShader());
                }
                if (outlineActionBackgroundDarkenPaint.getShader() != chatActionBackgroundDarkenPaint.getShader()) {
                    outlineActionBackgroundDarkenPaint.setShader(chatActionBackgroundDarkenPaint.getShader());
                }

                float fillProgress = slidingFillProgress.getValue() / springMultiplier;
                int wasDarkenColor = outlineActionBackgroundDarkenPaint.getColor();

                if (fillProgress > 1) {
                    slidingBeyondMax = true;
                }

                float translationX = getSlidingNonAnimationTranslationX(false);
                if (slidingDrawableVisibilityProgress.getValue() == 0) {
                    slidingFillProgressSpring.cancel();
                    slidingFillProgressSpring.getSpring().setFinalPosition(0);
                    slidingFillProgress.setValue(0f);
                    slidingOuterRingSpring.cancel();
                    slidingOuterRingSpring.getSpring().setFinalPosition(0);
                    slidingOuterRingProgress.setValue(0f);
                    slidingBeyondMax = false;
                }
                float progress;
                if (slidingFillProgressSpring.getSpring().getFinalPosition() != springMultiplier) {
                    progress = androidx.core.math.MathUtils.clamp((-translationX - AndroidUtilities.dp(20)) / AndroidUtilities.dp(30), 0, 1);
                } else {
                    progress = 1f;
                }

                if (progress == 1f && slidingFillProgressSpring.getSpring().getFinalPosition() != springMultiplier) {
                    slidingFillProgressSpring.getSpring().setFinalPosition(springMultiplier);
                    slidingFillProgressSpring.start();

                    slidingOuterRingSpring.getSpring().setFinalPosition(springMultiplier);
                    slidingOuterRingSpring.start();
                }

                boolean visible = translationX <= -AndroidUtilities.dp(20);
                float endVisibleValue = visible ? springMultiplier : 0;
                if (endVisibleValue != slidingDrawableVisibilitySpring.getSpring().getFinalPosition()) {
                    slidingDrawableVisibilitySpring.getSpring().setFinalPosition(endVisibleValue);
                    if (!slidingDrawableVisibilitySpring.isRunning()) {
                        slidingDrawableVisibilitySpring.start();
                    }
                }

                float iconProgress = slidingDrawableVisibilityProgress.getValue() / springMultiplier;
                MessageObject slidingMsg = getSlidingMessageObject();
                float x = getMeasuredWidth() + translationX * (slidingMsg != null && slidingMsg.isOut() ? 0.5f : 1f);
                float y = slidingView.getTop() + slidingView.getMeasuredHeight() / 2f;
                float scale = slidingBeyondMax ? fillProgress : iconProgress;

                float clearScale = slidingBeyondMax ? 0f : 1f - fillProgress;

                boolean isDark = ColorUtils.calculateLuminance(getThemedColor(Theme.key_windowBackgroundWhite)) <= 0.5f;
                if (iconProgress != 0) {
                    AndroidUtilities.rectTmp.set((int) (x - AndroidUtilities.dp(16) * scale + outlineActionBackgroundPaint.getStrokeWidth() / 2f), (int) (y - AndroidUtilities.dp(16) * scale + outlineActionBackgroundPaint.getStrokeWidth() / 2f), (int) (x + AndroidUtilities.dp(16) * scale - outlineActionBackgroundPaint.getStrokeWidth() / 2f), (int) (y + AndroidUtilities.dp(16) * scale - outlineActionBackgroundPaint.getStrokeWidth() / 2f));
                    Theme.applyServiceShaderMatrix(getMeasuredWidth(), AndroidUtilities.displaySize.y, 0, getY() + AndroidUtilities.rectTmp.top);
                    if (fillProgress == 0) {
                        int outlineAlpha = outlineActionBackgroundPaint.getAlpha();
                        outlineActionBackgroundPaint.setAlpha((int) (outlineAlpha * iconProgress));
                        canvas.drawArc(AndroidUtilities.rectTmp, -90, 360 * progress, false, outlineActionBackgroundPaint);
                        outlineActionBackgroundPaint.setAlpha(outlineAlpha);

                        if (themeDelegate.hasGradientService()) {
                            outlineAlpha = outlineActionBackgroundDarkenPaint.getAlpha();
                            if (isDark) {
                                outlineActionBackgroundDarkenPaint.setColor(Color.WHITE);
                            }
                            outlineActionBackgroundDarkenPaint.setAlpha((int) (outlineAlpha * iconProgress));
                            canvas.drawArc(AndroidUtilities.rectTmp, -90, 360 * progress, false, outlineActionBackgroundDarkenPaint);
                        }
                    }
                }
                AndroidUtilities.rectTmp.set((int) (x - AndroidUtilities.dp(16) * scale), (int) (y - AndroidUtilities.dp(16) * scale), (int) (x + AndroidUtilities.dp(16) * scale), (int) (y + AndroidUtilities.dp(16) * scale));
                Theme.applyServiceShaderMatrix(getMeasuredWidth(), AndroidUtilities.displaySize.y, 0, getY() + AndroidUtilities.rectTmp.top);
                path.rewind();
                path.addRoundRect(AndroidUtilities.rectTmp, AndroidUtilities.dp(16) * scale, AndroidUtilities.dp(16) * scale, Path.Direction.CW);

                int wasAlpha = chatActionBackgroundPaint.getAlpha();
                chatActionBackgroundPaint.setAlpha((int) (iconProgress * 0.6f * progress * wasAlpha));
                canvas.drawPath(path, chatActionBackgroundPaint);
                chatActionBackgroundPaint.setAlpha(wasAlpha);

                if (themeDelegate.hasGradientService()) {
                    wasAlpha = Theme.chat_actionBackgroundGradientDarkenPaint.getAlpha();
                    if (isDark) {
                        Theme.chat_actionBackgroundGradientDarkenPaint.setColor(Color.WHITE);
                    }
                    Theme.chat_actionBackgroundGradientDarkenPaint.setAlpha((int) (iconProgress * 0.6f * progress * wasAlpha));
                    canvas.drawPath(path, Theme.chat_actionBackgroundGradientDarkenPaint);
                    Theme.chat_actionBackgroundGradientDarkenPaint.setAlpha(wasAlpha);
                }

                if (clearScale != 0f) {
                    AndroidUtilities.rectTmp.set((int) (x - AndroidUtilities.dp(16) * clearScale), (int) (y - AndroidUtilities.dp(16) * clearScale), (int) (x + AndroidUtilities.dp(16) * clearScale), (int) (y + AndroidUtilities.dp(16) * clearScale));
                    path.rewind();
                    path.addRoundRect(AndroidUtilities.rectTmp, AndroidUtilities.dp(16), AndroidUtilities.dp(16), Path.Direction.CW);

                    canvas.save();
                    canvas.clipPath(path, Region.Op.DIFFERENCE);
                }

                AndroidUtilities.rectTmp.set((int) (x - AndroidUtilities.dp(16) * scale), (int) (y - AndroidUtilities.dp(16) * scale), (int) (x + AndroidUtilities.dp(16) * scale), (int) (y + AndroidUtilities.dp(16) * scale));
                Theme.applyServiceShaderMatrix(getMeasuredWidth(), AndroidUtilities.displaySize.y, 0, getY() + AndroidUtilities.rectTmp.top);
                path.rewind();
                path.addRoundRect(AndroidUtilities.rectTmp, AndroidUtilities.dp(16) * scale, AndroidUtilities.dp(16) * scale, Path.Direction.CW);

                wasAlpha = chatActionBackgroundPaint.getAlpha();
                chatActionBackgroundPaint.setAlpha((int) (iconProgress * 0.4f * wasAlpha));
                canvas.drawPath(path, chatActionBackgroundPaint);
                chatActionBackgroundPaint.setAlpha(wasAlpha);

                if (themeDelegate.hasGradientService()) {
                    wasAlpha = Theme.chat_actionBackgroundGradientDarkenPaint.getAlpha();
                    if (isDark) {
                        Theme.chat_actionBackgroundGradientDarkenPaint.setColor(Color.WHITE);
                    }
                    Theme.chat_actionBackgroundGradientDarkenPaint.setAlpha((int) (iconProgress * 0.4f * wasAlpha));
                    canvas.drawPath(path, Theme.chat_actionBackgroundGradientDarkenPaint);
                    Theme.chat_actionBackgroundGradientDarkenPaint.setAlpha(wasAlpha);
                }
                if (clearScale != 0f) {
                    canvas.restore();
                }

                float outerRingProgress = slidingOuterRingProgress.getValue() / springMultiplier;
                if (outerRingProgress != 0 && outerRingProgress != 1) {
                    float outScale = 1f + outerRingProgress;

                    float wasWidth = outlineActionBackgroundPaint.getStrokeWidth();
                    float width = (1f - outerRingProgress) * wasWidth;
                    if (width != 0f) {
                        AndroidUtilities.rectTmp.set((int) (x - AndroidUtilities.dp(16) * outScale + width), (int) (y - AndroidUtilities.dp(16) * outScale + width), (int) (x + AndroidUtilities.dp(16) * outScale - width), (int) (y + AndroidUtilities.dp(16) * outScale - width));
                        Theme.applyServiceShaderMatrix(getMeasuredWidth(), AndroidUtilities.displaySize.y, 0, getY() + AndroidUtilities.rectTmp.top);

                        wasAlpha = outlineActionBackgroundPaint.getAlpha();
                        outlineActionBackgroundPaint.setAlpha((int) (wasAlpha * iconProgress));

                        outlineActionBackgroundPaint.setStrokeWidth(width);
                        canvas.drawRoundRect(AndroidUtilities.rectTmp, AndroidUtilities.dp(16) * outScale, AndroidUtilities.dp(16) * outScale, outlineActionBackgroundPaint);
                        outlineActionBackgroundPaint.setStrokeWidth(wasWidth);

                        outlineActionBackgroundPaint.setAlpha(wasAlpha);

                        if (themeDelegate.hasGradientService()) {
                            wasAlpha = outlineActionBackgroundDarkenPaint.getAlpha();
                            if (isDark) {
                                outlineActionBackgroundDarkenPaint.setColor(Color.WHITE);
                            }
                            outlineActionBackgroundDarkenPaint.setAlpha((int) (wasAlpha * iconProgress));

                            outlineActionBackgroundDarkenPaint.setStrokeWidth(width);
                            canvas.drawRoundRect(AndroidUtilities.rectTmp, AndroidUtilities.dp(16) * outScale, AndroidUtilities.dp(16) * outScale, outlineActionBackgroundDarkenPaint);
                            outlineActionBackgroundDarkenPaint.setStrokeWidth(wasWidth);
                        }
                    }
                }

                int alpha = (int) (iconProgress * 0xFF);
                Drawable replyIconDrawable = getThemedDrawable(Theme.key_drawable_replyIcon);
                replyIconDrawable.setAlpha(alpha);
                replyIconDrawable.setBounds((int) (x - replyIconDrawable.getIntrinsicWidth() / 2 * scale), (int) (y - replyIconDrawable.getIntrinsicHeight() / 2 * scale), (int) (x + replyIconDrawable.getIntrinsicWidth() / 2 * scale), (int) (y + replyIconDrawable.getIntrinsicHeight() / 2 * scale));
                replyIconDrawable.draw(canvas);
                replyIconDrawable.setAlpha(255);

                outlineActionBackgroundDarkenPaint.setColor(wasDarkenColor);
                chatActionBackgroundDarkenPaint.setColor(wasDarkenColor);
            }

            private void processTouchEvent(MotionEvent e) {
                if (e != null) {
                    wasManualScroll = true;
                }
                if (e != null && e.getAction() == MotionEvent.ACTION_DOWN && !startedTrackingSlidingView && !maybeStartTrackingSlidingView && slidingView == null && !inPreviewMode) {
                    View view = getPressedChildView();
                    if (view instanceof ChatMessageCell) {
                        if (slidingView != null) {
                            slidingViewSetOffset(0);
                        }
                        slidingView = view;
                        MessageObject message = getSlidingMessageObject();
                        boolean allowReplyOnOpenTopic = canSendMessageToTopic(message);
                        if (
                            chatMode != 0 && chatMode != MODE_QUICK_REPLIES && chatMode != MODE_SUGGESTIONS && (chatMode != MODE_SAVED || threadMessageId != getUserConfig().getClientUserId()) ||
                            threadMessageObjects != null && threadMessageObjects.contains(message) ||
                            getMessageType(message) == 1 && (message.getDialogId() == mergeDialogId || message.needDrawBluredPreview()) ||
                            currentEncryptedChat == null && message.getId() < 0 ||
                            currentChat != null && ChatObject.isForum(currentChat) && !allowReplyOnOpenTopic ||
                            hasTextSelection() ||
                            message.isEphemeral() && message.isOut()
                        ) {
                            slidingViewSetOffset(0);
                            slidingView = null;
                            return;
                        }
                        startedTrackingPointerId = e.getPointerId(0);
                        maybeStartTrackingSlidingView = true;
                        startedTrackingX = (int) e.getX();
                        startedTrackingY = (int) e.getY();
                    }
                } else if (slidingView != null && e != null && e.getAction() == MotionEvent.ACTION_MOVE && e.getPointerId(0) == startedTrackingPointerId) {
                    int dx = Math.max(AndroidUtilities.dp(-80), Math.min(0, (int) (e.getX() - startedTrackingX)));
                    int dy = Math.abs((int) e.getY() - startedTrackingY);
                    if (getScrollState() == SCROLL_STATE_IDLE && maybeStartTrackingSlidingView && !startedTrackingSlidingView && dx <= -AndroidUtilities.getPixelsInCM(0.4f, true) && Math.abs(dx) / 3 > dy) {
                        MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0, 0, 0);
                        slidingView.onTouchEvent(event);
                        super.onInterceptTouchEvent(event);
                        event.recycle();
                        chatLayoutManager.setCanScrollVertically(false);
                        maybeStartTrackingSlidingView = false;
                        startedTrackingSlidingView = true;
                        startedTrackingX = (int) e.getX();
                        if (getParent() != null) {
                            getParent().requestDisallowInterceptTouchEvent(true);
                        }
                    } else if (startedTrackingSlidingView) {
                        if (Math.abs(dx) >= AndroidUtilities.dp(50)) {
                            if (!wasTrackingVibrate) {
                                try {
                                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                                } catch (Exception ignore) {}
                                wasTrackingVibrate = true;
                            }
                        } else {
                            wasTrackingVibrate = false;
                        }
                        slidingViewSetOffset(dx);
                        MessageObject messageObject = getSlidingMessageObject();
                        if (messageObject != null && (messageObject.isRoundVideo() || messageObject.isVideo())) {
                            updateTextureViewPosition(false, false);
                        }
                        if (slidingView instanceof ChatMessageCell) {
                            setGroupTranslationX((ChatMessageCell) slidingView, dx);
                        }
                        invalidate();
                    }
                } else if (slidingView != null && (e == null || e.getPointerId(0) == startedTrackingPointerId && (e.getAction() == MotionEvent.ACTION_CANCEL || e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_POINTER_UP))) {
                    if (e != null && e.getAction() != MotionEvent.ACTION_CANCEL && Math.abs(getSlidingNonAnimationTranslationX(false)) >= AndroidUtilities.dp(50)) {
                        MessageObject message = getSlidingMessageObject();
                        final boolean allowReplyOnOpenTopic = canSendMessageToTopic(message);
                        if (
                            bottomChannelButtonsLayout != null && bottomChannelButtonsLayout.getVisibility() == View.VISIBLE && !(bottomOverlayChatWaitsReply && allowReplyOnOpenTopic || message.wasJustSent) ||
                            currentChat != null && (
                                ChatObject.isNotInChat(currentChat) && !isThreadChat() ||
                                ChatObject.isChannel(currentChat) && !ChatObject.canPost(currentChat) && !currentChat.megagroup ||
                                !ChatObject.canSendMessages(currentChat)
                            )
                        ) {
                            if (message.getGroupId() != 0) {
                                MessageObject.GroupedMessages group = getGroup(message.getGroupId());
                                if (group != null && group.captionMessage != null) {
                                    message = group.captionMessage;
                                }
                            }
                            replyingMessageObject = message;
                            Bundle args = new Bundle();
                            args.putBoolean("onlySelect", true);
                            args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_FORWARD);
                            args.putBoolean("quote", true);
                            args.putBoolean("reply_to", true);
                            final long author = DialogObject.getPeerDialogId(message.getFromPeer());
                            if (author != 0 && author != getDialogId() && author != getUserConfig().getClientUserId() && author > 0) {
                                args.putLong("reply_to_author", author);
                            }
                            args.putInt("messagesCount", 1);
                            args.putBoolean("canSelectTopics", true);
                            final DialogsActivity fragment = new DialogsActivity(args);
                            fragment.setDelegate(ChatActivity.this);
                            presentFragment(fragment);
                        } else {
                            showFieldPanelForReply(getSlidingMessageObject());
                        }
                    }
                    endTrackingX = slidingViewGetOffsetX();
                    if (endTrackingX == 0) {
                        slidingView = null;
                    }
                    lastTrackingAnimationTime = System.currentTimeMillis();
                    trackAnimationProgress = 0.0f;
                    invalidate();
                    maybeStartTrackingSlidingView = false;
                    startedTrackingSlidingView = false;
                    chatLayoutManager.setCanScrollVertically(true);
                }
            }

            @Override
            public boolean onTouchEvent(MotionEvent e) {
                textSelectionHelper.checkSelectionCancel(e);
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    scrollByTouch = true;
                }
                if (pullingDownOffset != 0 && (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL)) {
                    float progress = Math.min(1f, pullingDownOffset / AndroidUtilities.dp(110));
                    if (e.getAction() == MotionEvent.ACTION_UP && progress == 1 && pullingDownDrawable != null && !pullingDownDrawable.emptyStub) {
                        if (pullingDownDrawable.animationIsRunning()) {
                            ValueAnimator animator = ValueAnimator.ofFloat(pullingDownOffset, pullingDownOffset + AndroidUtilities.dp(8));
                            pullingDownBackAnimator = animator;
                            animator.addUpdateListener(valueAnimator -> {
                                pullingDownOffset = (float) valueAnimator.getAnimatedValue();
                                chatListView.invalidate();
                            });
                            animator.setDuration(200);
                            animator.setInterpolator(CubicBezierInterpolator.DEFAULT);
                            animator.start();
                            pullingDownDrawable.runOnAnimationFinish(() -> {
                                animateToNextChat();
                            });
                        } else {
                            animateToNextChat();
                        }
                    } else {
                        if (pullingDownDrawable != null && pullingDownDrawable.emptyStub && (System.currentTimeMillis() - pullingDownDrawable.lastShowingReleaseTime) < 500 && pullingDownDrawable.animateSwipeToRelease) {
                            AnimatorSet animatorSet = new AnimatorSet();
                            pullingDownBackAnimator = animatorSet;
                            if (pullingDownDrawable != null) {
                                animatorPullingDownContainerVisibility.setValue(false, true);
                            }
                            ValueAnimator animator = ValueAnimator.ofFloat(pullingDownOffset, AndroidUtilities.dp(111));
                            animator.addUpdateListener(valueAnimator -> {
                                pullingDownOffset = (float) valueAnimator.getAnimatedValue();
                                chatListView.invalidate();
                            });
                            animator.setDuration(400);
                            animator.setInterpolator(CubicBezierInterpolator.DEFAULT);

                            ValueAnimator animator2 = ValueAnimator.ofFloat(AndroidUtilities.dp(111), 0);
                            animator2.addUpdateListener(valueAnimator -> {
                                pullingDownOffset = (float) valueAnimator.getAnimatedValue();
                                chatListView.invalidate();
                            });
                            animator2.setStartDelay(600);
                            animator2.setDuration(ChatListItemAnimator.DEFAULT_DURATION);
                            animator2.setInterpolator(ChatListItemAnimator.DEFAULT_INTERPOLATOR);

                            animatorSet.playSequentially(animator, animator2);
                            animatorSet.start();
                        } else {
                            ValueAnimator animator = ValueAnimator.ofFloat(pullingDownOffset, 0);
                            pullingDownBackAnimator = animator;
                            if (pullingDownDrawable != null) {
                                animatorPullingDownContainerVisibility.setValue(false, true);
                            }
                            animator.addUpdateListener(valueAnimator -> {
                                pullingDownOffset = (float) valueAnimator.getAnimatedValue();
                                chatListView.invalidate();
                            });
                            animator.setDuration(ChatListItemAnimator.DEFAULT_DURATION);
                            animator.setInterpolator(ChatListItemAnimator.DEFAULT_INTERPOLATOR);
                            animator.start();
                        }
                    }
                }
                if (isFastScrollAnimationRunning()) {
                    return false;
                }
                boolean result = super.onTouchEvent(e);
                if (actionBar.isActionModeShowed() || isReport()) {
                    return result;
                }
                processTouchEvent(e);
                return startedTrackingSlidingView || result;
            }

            @Override
            public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
                super.requestDisallowInterceptTouchEvent(disallowIntercept);
                if (slidingView != null) {
                    processTouchEvent(null);
                }
            }

            @Override
            protected void onChildPressed(View child, float x, float y, boolean pressed) {
                super.onChildPressed(child, x, y, pressed);
                if (child instanceof ChatMessageCell) {
                    ChatMessageCell chatMessageCell = (ChatMessageCell) child;
                    MessageObject object = chatMessageCell.getMessageObject();
                    if (object.isMusic() || object.isDocument()) {
                        return;
                    }
                    MessageObject.GroupedMessages groupedMessages = chatMessageCell.getCurrentMessagesGroup();
                    if (groupedMessages != null) {
                        int count = getChildCount();
                        for (int a = 0; a < count; a++) {
                            View item = getChildAt(a);
                            if (item == child || !(item instanceof ChatMessageCell)) {
                                continue;
                            }
                            ChatMessageCell cell = (ChatMessageCell) item;
                            if (cell.getCurrentMessagesGroup() == groupedMessages) {
                                cell.setPressed(pressed);
                            }
                        }
                    }
                }
            }

            @Override
            public void onDraw(Canvas c) {
                super.onDraw(c);
                if (slidingView != null) {
                    float translationX = slidingViewGetOffsetX();
                    if (!maybeStartTrackingSlidingView && !startedTrackingSlidingView && endTrackingX != 0 && translationX != 0) {
                        long newTime = System.currentTimeMillis();
                        long dt = newTime - lastTrackingAnimationTime;
                        trackAnimationProgress += dt / 180.0f;
                        if (trackAnimationProgress > 1.0f) {
                            trackAnimationProgress = 1.0f;
                        }
                        lastTrackingAnimationTime = newTime;
                        translationX = endTrackingX * (1.0f - AndroidUtilities.decelerateInterpolator.getInterpolation(trackAnimationProgress));
                        if (translationX == 0) {
                            endTrackingX = 0;
                        }
                        if (slidingView instanceof ChatMessageCell) {
                            setGroupTranslationX((ChatMessageCell) slidingView, translationX);
                        }
                        slidingViewSetOffset(translationX);
                        MessageObject messageObject = getSlidingMessageObject();
                        if (messageObject != null && (messageObject.isRoundVideo() || messageObject.isVideo())) {
                            updateTextureViewPosition(false, false);
                        }

                        if (trackAnimationProgress == 1f || trackAnimationProgress == 0f) {
                            slidingViewSetOffset(0);
                            slidingView = null;
                        }
                        invalidate();
                    }
                    drawReplyButton(c);
                }

                if (pullingDownOffset != 0 && !isInPreviewMode() && !isInsideContainer && chatMode != MODE_SAVED && chatMode != MODE_SCHEDULED) {
                    c.save();
                    float transitionOffset = 0;
                    if (pullingDownAnimateProgress != 0) {
                        transitionOffset = (
                            chatListView.getMeasuredHeight()
                                - pullingDownOffset
                                + (pullingDownAnimateToActivity == null ? 0 : pullingDownAnimateToActivity.pullingBottomOffset)
                        ) * pullingDownAnimateProgress;
                    }

                    c.translate(0, getMeasuredHeight() - blurredViewBottomOffset - transitionOffset);
                    if (pullingDownDrawable == null) {
                        pullingDownDrawable = new ChatPullingDownDrawable(currentAccount, fragmentView, dialog_id, dialogFolderId, dialogFilterId, getTopicId(), themeDelegate);
                        pullingDownDrawable.progressToBottomPanel = animatorPullingDownContainerVisibility.getFloatValue();
                        if (nextChannels != null && !nextChannels.isEmpty()) {
                            pullingDownDrawable.updateDialog(nextChannels.get(0));
                        } else if (isTopic) {
                            pullingDownDrawable.updateTopic();
                        } else {
                            pullingDownDrawable.updateDialog();
                        }
                        pullingDownDrawable.onAttach();
                    }
                    pullingDownDrawable.setWidth(getMeasuredWidth() - (isSideMenued() ? dp(SIDE_MENU_WIDTH) : 0));
                    float progress = Math.min(1f, pullingDownOffset / AndroidUtilities.dp(110));
                    c.translate(isSideMenued() ? lerp(dp(32), dp(SIDE_MENU_WIDTH), getSideMenuAlpha()) : 0,
                        -(windowInsetsStateHolder.getAnimatedMaxBottomInset() + dp(10) +
                        chatInputViewsContainer.getInputBubbleHeight() + getTopicTabsSideSize(TopicsTabsView.Position.BOTTOM)));
                    pullingDownDrawable.draw(c, chatListView, progress, 1f - pullingDownAnimateProgress);

                    c.restore();

                    if (pullingDownAnimateToActivity != null) {
                        c.saveLayerAlpha(0, 0, pullingDownAnimateToActivity.chatListView.getMeasuredWidth(), pullingDownAnimateToActivity.chatListView.getMeasuredHeight(), (int) (255 * pullingDownAnimateProgress), Canvas.ALL_SAVE_FLAG);
                        c.translate(0, getMeasuredHeight() - pullingDownOffset - transitionOffset);
                        pullingDownAnimateToActivity.chatListView.draw(c);
                        c.restore();
                    }
                } else if (pullingDownDrawable != null) {
                    pullingDownDrawable.reset();
                }
            }

            @Override
            public void draw(Canvas canvas) {
                if ((startMessageAppearTransitionMs == 0 || System.currentTimeMillis() - startMessageAppearTransitionMs <= SKELETON_DISAPPEAR_MS) && !AndroidUtilities.isTablet() && !isComments && currentUser == null) {
                    boolean noAvatar = (currentChat == null || ChatObject.isChannelAndNotMegaGroup(currentChat)) && chatMode != MODE_SEARCH;
                    if (pullingDownOffset != 0) {
                        canvas.save();
                        canvas.translate(0, -pullingDownOffset);
                    }
                    updateSkeletonColors();
                    updateSkeletonGradient();

                    int lastTop = getHeight() - blurredViewBottomOffset - (int) (windowInsetsStateHolder.getAnimatedMaxBottomInset() + getTopicTabsSideSize(TopicsTabsView.Position.BOTTOM)) - dp(44 + 7 + 9 - 3);
                    int j = 0;

                    int childMaxTop = Integer.MAX_VALUE;
                    for (int i = 0; i < getChildCount(); i++) {
                        int top = getChildAt(i).getTop();
                        if (top < childMaxTop) {
                            childMaxTop = top;
                        }
                    }
                    if (startMessageAppearTransitionMs == 0 && childMaxTop <= 0) {
                        checkDispatchHideSkeletons(fragmentBeginToShow);
                    }

                    Paint servicePaint = getThemedPaint(Theme.key_paint_chatActionBackground);
                    if (skeletonServicePaint.getColor() != servicePaint.getColor()) {
                        skeletonServicePaint.setColor(servicePaint.getColor());
                    }
                    if (skeletonServicePaint.getShader() != servicePaint.getShader()) {
                        skeletonServicePaint.setShader(servicePaint.getShader());
                        skeletonColorMatrix.setSaturation(SKELETON_SATURATION);
                        skeletonServicePaint.setColorFilter(new ColorMatrixColorFilter(skeletonColorMatrix));
                    }

                    for (int i = 0; i < getChildCount(); i++) {
                        View v = getChildAt(i);
//                        if (v instanceof ChatMessageCell) {
//                            ChatMessageCell cell = (ChatMessageCell) v;
//                            if ((cell.getCurrentMessagesGroup() == null || cell.getCurrentMessagesGroup().findPrimaryMessageObject() == cell.getMessageObject())) {
//                                if (cell.shouldDrawAlphaLayer() || System.currentTimeMillis() - startMessageAppearTransitionMs >= SKELETON_DISAPPEAR_MS) {
//                                    float progress = cell.getAlpha();
//
//                                    MessageSkeleton skeleton;
//                                    if (j >= messageSkeletons.size()) {
//                                        skeleton = getNewSkeleton(noAvatar);
//                                        messageSkeletons.add(skeleton);
//                                    } else {
//                                        skeleton = messageSkeletons.get(j);
//                                    }
//
//                                    Rect bounds = cell.getCurrentBackgroundDrawable(true).getBounds();
//                                    MessageObject.GroupedMessages group = cell.getCurrentMessagesGroup();
//
//                                    int alpha = skeletonPaint.getAlpha();
//                                    int wasServiceAlpha = servicePaint.getAlpha();
//                                    servicePaint.setAlpha((int) (wasServiceAlpha * 0.4f * (1f - progress)));
//                                    skeletonPaint.setAlpha((int) (alpha * (1f - progress)));
//                                    int bottom = (int) AndroidUtilities.lerp(Math.min(skeleton.lastBottom, lastTop - AndroidUtilities.dp(3f)), v.getBottom() + (group != null ? group.transitionParams.top + group.transitionParams.offsetTop : 0), progress);
//                                    int left = noAvatar ? AndroidUtilities.dp(3f) : AndroidUtilities.dp(51);
//                                    int top = (int) AndroidUtilities.lerp(bottom - skeleton.height, bounds.top + v.getTop() + (group != null ? group.transitionParams.top + group.transitionParams.offsetTop : 0), progress);
//                                    int right = skeleton.width;
//
//                                    boolean lerp = cell.getMessageObject() == null || !cell.getMessageObject().isOut();
//                                    skeletonBackgroundDrawable.setBounds(lerp ? AndroidUtilities.lerp(left, cell.getBackgroundDrawableLeft(), progress) : left, top,
//                                            lerp ? AndroidUtilities.lerp(right, cell.getBackgroundDrawableRight(), progress) : right, bottom);
//                                    Theme.applyServiceShaderMatrix(getMeasuredWidth(), AndroidUtilities.displaySize.y, 0, getY() + skeletonBackgroundDrawable.getBounds().top);
//                                    skeletonBackgroundDrawable.drawCached(canvas, skeletonBackgroundCacheParams, servicePaint);
//                                    skeletonBackgroundDrawable.drawCached(canvas, skeletonBackgroundCacheParams, skeletonPaint);
//                                    if (!noAvatar) {
//                                        Theme.applyServiceShaderMatrix(getMeasuredWidth(), AndroidUtilities.displaySize.y, 0, getY() + bottom - AndroidUtilities.dp(42));
//                                        canvas.drawCircle(AndroidUtilities.dp(48 - 21), bottom - AndroidUtilities.dp(21), AndroidUtilities.dp(21), servicePaint);
//                                        canvas.drawCircle(AndroidUtilities.dp(48 - 21), bottom - AndroidUtilities.dp(21), AndroidUtilities.dp(21), skeletonPaint);
//                                    }
//                                    servicePaint.setAlpha(wasServiceAlpha);
//                                    skeletonPaint.setAlpha(alpha);
//                                    j++;
//
//                                    if (top < lastTop) {
//                                        lastTop = top;
//                                    }
//
//                                    continue;
//                                }
//                                j++;
//                            }
//                        }
                        if (v instanceof ChatMessageCell) {
                            MessageObject.GroupedMessages group = ((ChatMessageCell) v).getCurrentMessagesGroup();
                            Rect bounds = ((ChatMessageCell) v).getCurrentBackgroundDrawable(true).getBounds();
                            int newTop = (int) (v.getTop() + bounds.top + (group != null ? group.transitionParams.top + group.transitionParams.offsetTop : 0));
                            int top = startMessageAppearTransitionMs == 0 && isSkeletonVisible() ? lerp(lastTop, newTop, v.getAlpha()) : v.getAlpha() == 1f ? newTop : lastTop;
                            if (top < lastTop) {
                                lastTop = top;
                            }
                        } else if (v instanceof ChatActionCell) {
                            int top = startMessageAppearTransitionMs == 0 && isSkeletonVisible() ? lerp(lastTop, v.getTop(), v.getAlpha()) : v.getAlpha() == 1f ? v.getTop() : lastTop;
                            if (top < lastTop) {
                                lastTop = top;
                            }
                        }
                    }

                    if (isSkeletonVisible()) {
                        boolean drawService = SharedConfig.getDevicePerformanceClass() != SharedConfig.PERFORMANCE_CLASS_LOW && Theme.hasGradientService();
                        boolean darkOverlay = ColorUtils.calculateLuminance(getThemedColor(Theme.key_windowBackgroundWhite)) <= 0.7f && Theme.hasGradientService();
                        boolean blackOverlay = ColorUtils.calculateLuminance(getThemedColor(Theme.key_windowBackgroundWhite)) <= 0.01f && Theme.hasGradientService();
                        if (drawService) {
                            Theme.applyServiceShaderMatrix(getMeasuredWidth(), AndroidUtilities.displaySize.y, 0, getY() - contentPanTranslation);
                        }
                        int wasDarkenAlpha = Theme.chat_actionBackgroundGradientDarkenPaint.getAlpha();
                        if (blackOverlay) {
                            Theme.chat_actionBackgroundGradientDarkenPaint.setAlpha((int) (wasDarkenAlpha * 4f));
                        }

                        float topSkeletonAlpha = startMessageAppearTransitionMs != 0 ? 1f - (System.currentTimeMillis() - startMessageAppearTransitionMs) / (float) SKELETON_DISAPPEAR_MS : 1f;
                        int alpha = skeletonPaint.getAlpha();
                        int wasServiceAlpha = skeletonServicePaint.getAlpha();
                        int wasOutlineAlpha = skeletonOutlinePaint.getAlpha();
                        float adaptDark = 1f;
                        if (themeDelegate != null && themeDelegate.isDark && skeletonServicePaint.getShader() != null) {
                            adaptDark *= .3f;
                        }
                        skeletonServicePaint.setAlpha((int) (0xFF * topSkeletonAlpha * adaptDark));
                        skeletonPaint.setAlpha((int) (topSkeletonAlpha * adaptDark * alpha));
                        skeletonOutlinePaint.setAlpha((int) (topSkeletonAlpha * alpha));
                        while (lastTop > blurredViewTopOffset) {
                            lastTop -= AndroidUtilities.dp(3f);

                            MessageSkeleton skeleton;
                            if (j >= messageSkeletons.size()) {
                                skeleton = getNewSkeleton(noAvatar);
                                messageSkeletons.add(skeleton);
                            } else {
                                skeleton = messageSkeletons.get(j);
                            }
                            skeleton.lastBottom = startMessageAppearTransitionMs != 0 ? messages.size() <= 2 ? Math.min(skeleton.lastBottom, lastTop) : skeleton.lastBottom : lastTop;

                            lastTop -= skeleton.height;

                            j++;
                        }

                        lastTop = messageSkeletons.isEmpty() ? getHeight() - blurredViewBottomOffset : messageSkeletons.get(0).lastBottom + AndroidUtilities.dp(3f);
                        int left = dp(noAvatar ? 3 : 51);
                        if (isSideMenued()) {
                            left = lerp(left, dp(SIDE_MENU_WIDTH), getSideMenuAlpha());
                        }
                        for (int i = 0; i < messageSkeletons.size() && lastTop > blurredViewTopOffset; i++) {
                            lastTop -= dp(3f);

                            MessageSkeleton skeleton = messageSkeletons.get(i);

                            int bottom = skeleton.lastBottom;
                            skeletonBackgroundDrawable.setBounds(left, bottom - skeleton.height, skeleton.width, bottom);
                            if (drawService) {
                                skeletonBackgroundDrawable.drawCached(canvas, skeletonBackgroundCacheParams, skeletonServicePaint);
                            }
                            skeletonBackgroundDrawable.drawCached(canvas, skeletonBackgroundCacheParams, skeletonPaint);
                            if (darkOverlay) {
                                skeletonBackgroundDrawable.drawCached(canvas, skeletonBackgroundCacheParams, Theme.chat_actionBackgroundGradientDarkenPaint);
                            }
                            skeletonBackgroundDrawable.drawCached(canvas, skeletonBackgroundCacheParams, skeletonOutlinePaint);

                            if (!noAvatar) {
                                if (drawService) {
                                    canvas.drawCircle(dp(48 - 21), bottom - dp(21), dp(21), skeletonServicePaint);
                                }
                                canvas.drawCircle(dp(48 - 21), bottom - dp(21), dp(21), skeletonPaint);
                                if (darkOverlay) {
                                    canvas.drawCircle(dp(48 - 21), bottom - dp(21), dp(21), Theme.chat_actionBackgroundGradientDarkenPaint);
                                }
                                canvas.drawCircle(dp(48 - 21), bottom - dp(21), dp(21), skeletonOutlinePaint);
                            }

                            lastTop -= skeleton.height;
                        }

                        skeletonServicePaint.setAlpha(wasServiceAlpha);
                        skeletonPaint.setAlpha(alpha);
                        skeletonOutlinePaint.setAlpha(wasOutlineAlpha);
                        Theme.chat_actionBackgroundGradientDarkenPaint.setAlpha(wasDarkenAlpha);
                        invalidated = false;
                        invalidate();
                    } else if (System.currentTimeMillis() - startMessageAppearTransitionMs > SKELETON_DISAPPEAR_MS) {
                        messageSkeletons.clear();
                    }
                    lastSkeletonCount = messageSkeletons.size();
                    lastSkeletonMessageCount = messages.size();
                    if (pullingDownOffset != 0) {
                        canvas.restore();
                    }
                }
                super.draw(canvas);
            }

            private void updateSkeletonColors() {
                boolean dark = ColorUtils.calculateLuminance(getThemedColor(Theme.key_windowBackgroundWhite)) <= 0.7f;
                int color0 = ColorUtils.blendARGB(getThemedColor(Theme.key_listSelector), Color.argb(dark ? 0x21 : 0x03, 0xFF, 0xFF, 0xFF), dark ? 0.9f : 0.5f);
                int color1 = ColorUtils.setAlphaComponent(getThemedColor(Theme.key_listSelector), dark ? 24 : SKELETON_LIGHT_OVERLAY_ALPHA);
                if (skeletonColor1 != color1 || skeletonColor0 != color0) {
                    skeletonColor0 = color0;
                    skeletonColor1 = color1;
                    skeletonGradient = new LinearGradient(0, 0, skeletonGradientWidth = AndroidUtilities.dp(200), 0, new int[]{color1, color0, color0, color1}, new float[]{0.0f, 0.4f, 0.6f, 1f}, Shader.TileMode.CLAMP);
                    skeletonTotalTranslation = -skeletonGradientWidth * 2;
                    skeletonPaint.setShader(skeletonGradient);

                    int outlineColor = Color.argb(dark ? 0x2B : 0x60, 0xFF, 0xFF, 0xFF);
                    skeletonOutlineGradient = new LinearGradient(0, 0, skeletonGradientWidth, 0, new int[]{Color.TRANSPARENT, outlineColor, outlineColor, Color.TRANSPARENT}, new float[]{0.0f, 0.4f, 0.6f, 1f}, Shader.TileMode.CLAMP);
                    skeletonOutlinePaint.setShader(skeletonOutlineGradient);
                }
            }

            private void updateSkeletonGradient() {
                long newUpdateTime = SystemClock.elapsedRealtime();
                long dt = Math.abs(skeletonLastUpdateTime - newUpdateTime);
                if (dt > 17) {
                    dt = 16;
                }
                if (dt < 4) {
                    dt = 0;
                }
                int width = getWidth();
                skeletonLastUpdateTime = newUpdateTime;
                skeletonTotalTranslation += dt * width / 400.0f;
                if (skeletonTotalTranslation >= width * 2) {
                    skeletonTotalTranslation = -skeletonGradientWidth * 2;
                }
                skeletonMatrix.setTranslate(skeletonTotalTranslation, 0);
                if (skeletonGradient != null) {
                    skeletonGradient.setLocalMatrix(skeletonMatrix);
                }
                skeletonOutlineMatrix.setTranslate(skeletonTotalTranslation, 0);
                if (skeletonOutlineGradient != null) {
                    skeletonOutlineGradient.setLocalMatrix(skeletonOutlineMatrix);
                }
            }


            @Override
            protected void dispatchDraw(Canvas canvas) {
                drawLaterRoundProgressCell = null;
                invalidated = false;

                canvas.save();
                if ((fragmentTransition == null || (fromPullingDownTransition && !toPullingDownTransition)) && !isInsideContainer) {
                    // canvas.clipRect(0, chatListViewPaddingTop - chatListViewPaddingVisibleOffset - AndroidUtilities.dp(4), getMeasuredWidth(), getMeasuredHeight() - blurredViewBottomOffset);
                }
                selectorRect.setEmpty();
                if (pullingDownOffset != 0) {
                    int restoreToCount = canvas.save();
                    float transitionOffset = 0;
                    if (pullingDownAnimateProgress != 0) {
                        transitionOffset = (chatListView.getMeasuredHeight() - pullingDownOffset) * pullingDownAnimateProgress;
                    }
                    canvas.translate(0, drawingChatListViewYoffset = -pullingDownOffset - transitionOffset);
                    drawChatBackgroundElements(canvas);
                    super.dispatchDraw(canvas);
                    drawChatForegroundElements(canvas);
                    canvas.restoreToCount(restoreToCount);
                } else {
                    drawChatBackgroundElements(canvas);
                    super.dispatchDraw(canvas);
                    drawChatForegroundElements(canvas);
                }
                canvas.restore();
            }

            protected void drawChatForegroundElements(Canvas canvas, RectF position) {
                int size = drawTimeAfter.size();
                if (size > 0) {
                    for (int a = 0; a < size; a++) {
                        ChatMessageCell cell = drawTimeAfter.get(a);
                        if (quickRejectChild(cell, position)) {
                            continue;
                        }
                        canvas.save();
                        canvas.translate(cell.getLeft() + cell.getNonAnimationTranslationX(false), cell.getY() + cell.getPaddingTop());
                        cell.drawTime(canvas, cell.shouldDrawAlphaLayer() ? cell.getAlpha() : 1f, true);
                        canvas.restore();
                    }
                    drawTimeAfter.clear();
                }
                size = drawNamesAfter.size();
                if (size > 0) {
                    for (int a = 0; a < size; a++) {
                        ChatMessageCell cell = drawNamesAfter.get(a);
                        if (quickRejectChild(cell, position)) {
                            continue;
                        }
                        float canvasOffsetX = cell.getLeft() + cell.getNonAnimationTranslationX(false);
                        float canvasOffsetY = cell.getY() + cell.getPaddingTop();
                        float alpha = cell.shouldDrawAlphaLayer() ? cell.getAlpha() : 1f;

                        canvas.save();
                        canvas.translate(canvasOffsetX, canvasOffsetY);
                        cell.setInvalidatesParent(true);
                        cell.drawNamesLayout(canvas, alpha);
                        cell.setInvalidatesParent(false);
                        canvas.restore();
                    }
                    drawNamesAfter.clear();
                }
                size = drawCaptionAfter.size();
                if (size > 0) {
                    for (int a = 0; a < size; a++) {
                        ChatMessageCell cell = drawCaptionAfter.get(a);
                        if (quickRejectChild(cell, position)) {
                            continue;
                        }
                        boolean selectionOnly = false;
                        if (cell.getCurrentPosition() != null) {
                            selectionOnly = (cell.getCurrentPosition().flags & MessageObject.POSITION_FLAG_LEFT) == 0;
                        }
                        float alpha = cell.shouldDrawAlphaLayer() ? cell.getAlpha() : 1f;
                        float canvasOffsetX = cell.getLeft() + cell.getNonAnimationTranslationX(false);
                        float canvasOffsetY = cell.getY() + cell.getPaddingTop();
                        canvas.save();
                        MessageObject.GroupedMessages groupedMessages = cell.getCurrentMessagesGroup();
                        if (groupedMessages != null && groupedMessages.transitionParams.backgroundChangeBounds) {
                            float x = cell.getNonAnimationTranslationX(true);
                            float l = (groupedMessages.transitionParams.left + x + groupedMessages.transitionParams.offsetLeft);
                            float t = (groupedMessages.transitionParams.top + groupedMessages.transitionParams.offsetTop);
                            float r = (groupedMessages.transitionParams.right + x + groupedMessages.transitionParams.offsetRight);
                            float b = (groupedMessages.transitionParams.bottom + groupedMessages.transitionParams.offsetBottom);

                            if (!groupedMessages.transitionParams.backgroundChangeBounds) {
                                t += cell.getTranslationY();
                                b += cell.getTranslationY();
                            }
                            canvas.clipRect(
                                    l + AndroidUtilities.dp(8), t + AndroidUtilities.dp(8),
                                    r - AndroidUtilities.dp(8), b - AndroidUtilities.dp(8)
                            );
                        }
                        if (cell.getTransitionParams().wasDraw) {
                            canvas.translate(canvasOffsetX, canvasOffsetY);
                            cell.setInvalidatesParent(true);
                            cell.drawCaptionLayout(canvas, selectionOnly, alpha);
                            cell.setInvalidatesParent(false);
                        }
                        canvas.restore();
                    }
                    drawCaptionAfter.clear();
                }
                size = drawReactionsAfter.size();
                if (size > 0) {
                    for (int a = 0; a < size; a++) {
                        ChatMessageCell cell = drawReactionsAfter.get(a);
                        if (quickRejectChild(cell, position)) {
                            continue;
                        }
                        boolean selectionOnly = false;
                        if (cell.getCurrentPosition() != null) {
                            selectionOnly = (cell.getCurrentPosition().flags & MessageObject.POSITION_FLAG_LEFT) == 0;
                        }
                        float alpha = cell.shouldDrawAlphaLayer() ? cell.getAlpha() : 1f;
                        float canvasOffsetX = cell.getLeft() + cell.getNonAnimationTranslationX(false);
                        float canvasOffsetY = cell.getY() + cell.getPaddingTop();
                        canvas.save();
                        MessageObject.GroupedMessages groupedMessages = cell.getCurrentMessagesGroup();
                        if (groupedMessages != null && groupedMessages.transitionParams.backgroundChangeBounds) {
                            float x = cell.getNonAnimationTranslationX(true);
                            float l = (groupedMessages.transitionParams.left + x + groupedMessages.transitionParams.offsetLeft);
                            float t = (groupedMessages.transitionParams.top + groupedMessages.transitionParams.offsetTop);
                            float r = (groupedMessages.transitionParams.right + x + groupedMessages.transitionParams.offsetRight);
                            float b = (groupedMessages.transitionParams.bottom + groupedMessages.transitionParams.offsetBottom);

                            if (!groupedMessages.transitionParams.backgroundChangeBounds) {
                                t += cell.getTranslationY();
                                b += cell.getTranslationY();
                            }
                            canvas.clipRect(
                                    l + AndroidUtilities.dp(8), t + AndroidUtilities.dp(8),
                                    r - AndroidUtilities.dp(8), b - AndroidUtilities.dp(8)
                            );
                        }
                        if (!selectionOnly && cell.getTransitionParams().wasDraw) {
                            canvas.translate(canvasOffsetX, canvasOffsetY);
                            cell.setInvalidatesParent(true);
                            cell.drawReactionsLayout(canvas, alpha, null);
                            cell.drawCommentLayout(canvas, alpha);
                            cell.setInvalidatesParent(false);
                        }
                        canvas.restore();
                    }
                    drawReactionsAfter.clear();
                }
            }

            protected void drawChatBackgroundElements(Canvas canvas, RectF positionF) {
                final int count = getChildCount();
                MessageObject.GroupedMessages lastDrawnGroup = null;

                for (int a = 0; a < count; a++) {
                    View child = getChildAt(a);
                    if (child.getVisibility() == View.INVISIBLE || child.getVisibility() == View.GONE || quickRejectChild(child, positionF)) {
                        continue;
                    }
                    if (child instanceof ChatMessageUnsupportedCell) {
                        ChatMessageUnsupportedCell unsupportedCell = (ChatMessageUnsupportedCell) child;
                        canvas.save();
                        canvas.translate(child.getX(), child.getY());
                        unsupportedCell.drawBackground(canvas);
                        canvas.restore();
                    } else if (chatAdapter.isBot && child instanceof BotHelpCell) {
                        BotHelpCell botCell = (BotHelpCell) child;
                        float top = (getMeasuredHeight() - chatListViewPaddingTop - blurredViewBottomOffset) / 2 - child.getMeasuredHeight() / 2 + chatListViewPaddingTop;
                        if (!botCell.animating() && !chatListView.fastScrollAnimationRunning) {
                            if (child.getTop() > top) {
                                child.setTranslationY(top - child.getTop());
                            } else {
                                child.setTranslationY(0);
                            }
                        }
                        break;
                    } else if (child instanceof UserInfoCell) {
                        UserInfoCell cell = (UserInfoCell) child;
                        float top = (getMeasuredHeight() - chatListViewPaddingTop - blurredViewBottomOffset) / 2 - child.getMeasuredHeight() / 2 + chatListViewPaddingTop;
                        if (!cell.animating() && !chatListView.fastScrollAnimationRunning) {
                            if (child.getTop() > top) {
                                child.setTranslationY(top - child.getTop());
                            } else {
                                child.setTranslationY(0);
                            }
                        }
                    } else if (child instanceof ChatMessageCell) {
                        ChatMessageCell cell = (ChatMessageCell) child;
                        MessageObject.GroupedMessages group = cell.getCurrentMessagesGroup();
                        if (group == null || group != lastDrawnGroup) {
                            lastDrawnGroup = group;
                            MessageObject.GroupedMessagePosition position = cell.getCurrentPosition();
                            MessageBackgroundDrawable backgroundDrawable = cell.getBackgroundDrawable();
                            if ((backgroundDrawable.isAnimationInProgress() || cell.isDrawingSelectionBackground()) && (position == null || (position.flags & MessageObject.POSITION_FLAG_RIGHT) != 0)) {
                                if (cell.isHighlighted() || cell.isHighlightedAnimated()) {
                                    if (position == null) {
                                        Paint backgroundPaint = getThemedPaint(Theme.key_paint_chatMessageBackgroundSelected);
                                        if (themeDelegate != null && themeDelegate.isDark || backgroundPaint == null) {
                                            backgroundPaint = Theme.chat_replyLinePaint;
                                            backgroundPaint.setColor(getThemedColor(Theme.key_chat_selectedBackground));
                                        } else {
                                            float viewTop = (isKeyboardVisible() ? chatListView.getTop() : actionBar.getMeasuredHeight()) - contentView.getBackgroundTranslationY();
                                            int backgroundHeight = contentView.getBackgroundSizeY();
                                            if (themeDelegate != null) {
                                                themeDelegate.applyServiceShaderMatrix(getMeasuredWidth(), backgroundHeight, cell.getX(), viewTop);
                                            } else {
                                                Theme.applyServiceShaderMatrix(getMeasuredWidth(), backgroundHeight, cell.getX(), viewTop);
                                            }
                                        }
                                        canvas.save();
                                        canvas.translate(0, cell.getTranslationY());
                                        int wasAlpha = backgroundPaint.getAlpha();
                                        backgroundPaint.setAlpha((int) (wasAlpha * cell.getHighlightAlpha() * cell.getAlpha()));
                                        canvas.drawRect(0, cell.getTop(), getMeasuredWidth(), cell.getBottom(), backgroundPaint);
                                        backgroundPaint.setAlpha(wasAlpha);
                                        canvas.restore();
                                    }
                                } else {
                                    int y = (int) cell.getY();
                                    int height;
                                    canvas.save();
                                    if (position == null) {
                                        height = cell.getMeasuredHeight();
                                    } else {
                                        height = y + cell.getMeasuredHeight();
                                        long time = 0;
                                        float touchX = 0;
                                        float touchY = 0;
                                        for (int i = 0; i < count; i++) {
                                            View inner = getChildAt(i);
                                            if (inner instanceof ChatMessageCell) {
                                                ChatMessageCell innerCell = (ChatMessageCell) inner;
                                                MessageObject.GroupedMessages innerGroup = innerCell.getCurrentMessagesGroup();
                                                if (innerGroup == group) {
                                                    MessageBackgroundDrawable drawable = innerCell.getBackgroundDrawable();
                                                    y = Math.min(y, (int) innerCell.getY());
                                                    height = Math.max(height, (int) innerCell.getY() + innerCell.getMeasuredHeight());
                                                    long touchTime = drawable.getLastTouchTime();
                                                    if (touchTime > time) {
                                                        touchX = drawable.getTouchX() + innerCell.getX();
                                                        touchY = drawable.getTouchY() + innerCell.getY();
                                                        time = touchTime;
                                                    }
                                                }
                                            }
                                        }
                                        backgroundDrawable.setTouchCoordsOverride(touchX, touchY - y);
                                        height -= y;
                                    }
                                    canvas.clipRect(0, y, getMeasuredWidth(), y + height);
                                    Paint selectedBackgroundPaint = getThemedPaint(Theme.key_paint_chatMessageBackgroundSelected);
                                    if (themeDelegate != null && !themeDelegate.isDark && selectedBackgroundPaint != null) {
                                        backgroundDrawable.setCustomPaint(selectedBackgroundPaint);
                                        float viewTop = (isKeyboardVisible() ? chatListView.getTop() : actionBar.getMeasuredHeight()) - contentView.getBackgroundTranslationY();
                                        int backgroundHeight = contentView.getBackgroundSizeY();
                                        if (themeDelegate != null) {
                                            themeDelegate.applyServiceShaderMatrix(getMeasuredWidth(), backgroundHeight, cell.getX(), viewTop);
                                        } else {
                                            Theme.applyServiceShaderMatrix(getMeasuredWidth(), backgroundHeight, cell.getX(), viewTop);
                                        }
                                    } else {
                                        backgroundDrawable.setCustomPaint(null);
                                        backgroundDrawable.setColor(getThemedColor(Theme.key_chat_selectedBackground));
                                    }
                                    backgroundDrawable.setBounds(0, y, getMeasuredWidth(), y + height);
                                    backgroundDrawable.draw(canvas);
                                    canvas.restore();
                                }
                            }
                        }
                        if ((scrimView != cell || scrimViewTask != null) && group == null && cell.drawBackgroundInParent()) {
                            canvas.save();
                            canvas.translate(cell.getX(), cell.getY() + cell.getPaddingTop());
                            if (cell.getScaleX() != 1f) {
                                canvas.scale(
                                    cell.getScaleX(), cell.getScaleY(),
                                    cell.getPivotX(), (cell.getHeight() >> 1)
                                );
                            }
                            cell.drawBackgroundInternal(canvas, true);
                            canvas.restore();
                        }
                    } else if (child instanceof ChatActionCell) {
                        ChatActionCell cell = (ChatActionCell) child;
                        if (cell.hasGradientService()) {
                            canvas.save();
                            canvas.translate(cell.getX(), cell.getY() + cell.getPaddingTop());
                            canvas.scale(cell.getScaleX(), cell.getScaleY(), cell.getMeasuredWidth() / 2f, cell.getMeasuredHeight() / 2f);
                            canvas.translate(getSideMenuWidth() / 2f, 0);
                            cell.drawBackground(canvas, true);
                            cell.drawReactions(canvas, true, null);
                            canvas.restore();
                        }
                    }
                }
                MessageObject.GroupedMessages scrimGroup = null;
                if (scrimView instanceof ChatMessageCell) {
                    scrimGroup = ((ChatMessageCell) scrimView).getCurrentMessagesGroup();
                }
                for (int k = 0; k < 3; k++) {
                    drawingGroups.clear();
                    if (k == 2 && !chatListView.isFastScrollAnimationRunning()) {
                        continue;
                    }
                    for (int i = 0; i < count; i++) {
                        View child = chatListView.getChildAt(i);
                        if (child instanceof ChatMessageCell) {
                            ChatMessageCell cell = (ChatMessageCell) child;
                            if (child.getY() > chatListView.getHeight() || child.getY() + child.getHeight() < 0 || cell.getVisibility() == View.GONE) {
                                continue;
                            }
                            MessageObject.GroupedMessages group = cell.getCurrentMessagesGroup();
                            if (group == null || (k == 0 && group.messages.size() == 1) || (k == 1 && !group.transitionParams.drawBackgroundForDeletedItems)) {
                                continue;
                            }
                            if ((k == 0 && cell.getMessageObject().deleted) || (k == 1 && !cell.getMessageObject().deleted)) {
                                continue;
                            }
                            if ((k == 2 && !cell.willRemovedAfterAnimation()) || (k != 2 && cell.willRemovedAfterAnimation())) {
                                continue;
                            }

                            if (!drawingGroups.contains(group)) {
                                group.transitionParams.left = 0;
                                group.transitionParams.top = 0;
                                group.transitionParams.right = 0;
                                group.transitionParams.bottom = 0;

                                group.transitionParams.pinnedBotton = false;
                                group.transitionParams.pinnedTop = false;
                                group.transitionParams.cell = cell;
                                drawingGroups.add(group);
                            }

                            group.transitionParams.pinnedTop = cell.isPinnedTop();
                            group.transitionParams.pinnedBotton = cell.isPinnedBottom();

                            int left = (cell.getLeft() + cell.getBackgroundDrawableLeft());
                            int right = (cell.getLeft() + cell.getBackgroundDrawableRight());
                            int top = (cell.getTop() + cell.getPaddingTop() + cell.getBackgroundDrawableTop());
                            int bottom = (cell.getTop() + cell.getPaddingTop() + cell.getBackgroundDrawableBottom());

                            if ((cell.getCurrentPosition().flags & MessageObject.POSITION_FLAG_TOP) == 0) {
                                top -= AndroidUtilities.dp(10);
                            }

                            if ((cell.getCurrentPosition().flags & MessageObject.POSITION_FLAG_BOTTOM) == 0) {
                                bottom += AndroidUtilities.dp(10);
                            }

                            if (cell.willRemovedAfterAnimation()) {
                                group.transitionParams.cell = cell;
                            }

                            if (group.transitionParams.top == 0 || top < group.transitionParams.top) {
                                group.transitionParams.top = top;
                            }
                            if (group.transitionParams.bottom == 0 || bottom > group.transitionParams.bottom) {
                                group.transitionParams.bottom = bottom;
                            }
                            if (group.transitionParams.left == 0 || left < group.transitionParams.left) {
                                group.transitionParams.left = left;
                            }
                            if (group.transitionParams.right == 0 || right > group.transitionParams.right) {
                                group.transitionParams.right = right;
                            }
                        }
                    }

                    for (int i = 0; i < drawingGroups.size(); i++) {
                        final MessageObject.GroupedMessages group = drawingGroups.get(i);
                        if (group == scrimGroup) {
                             // continue;
                        }
                        float x = group.transitionParams.cell.getNonAnimationTranslationX(true);
                        float l = (group.transitionParams.left + x + group.transitionParams.offsetLeft);
                        float t = (group.transitionParams.top + group.transitionParams.offsetTop);
                        float r = (group.transitionParams.right + x + group.transitionParams.offsetRight);
                        float b = (group.transitionParams.bottom + group.transitionParams.offsetBottom);

                        if (!group.transitionParams.backgroundChangeBounds) {
                            t += group.transitionParams.cell.getTranslationY();
                            b += group.transitionParams.cell.getTranslationY();
                        }

                        /*
                        if (t < chatListViewPaddingTop - chatListViewPaddingVisibleOffset - dp(20)) {
                            t = chatListViewPaddingTop - chatListViewPaddingVisibleOffset - dp(20);
                        }
                        */

                        if (b > chatListView.getMeasuredHeight() + dp(20)) {
                            b = chatListView.getMeasuredHeight() + dp(20);
                        }

                        boolean useScale = group.transitionParams.cell.getScaleX() != 1f || group.transitionParams.cell.getScaleY() != 1f;
                        if (useScale) {
                            canvas.save();
                            canvas.scale(group.transitionParams.cell.getScaleX(), group.transitionParams.cell.getScaleY(), l + (r - l) / 2, t + (b - t) / 2);
                        }
                        boolean selected = true;
                        for (int a = 0, N = group.messages.size(); a < N; a++) {
                            MessageObject object = group.messages.get(a);
                            int index = object.getDialogId() == dialog_id ? 0 : 1;
                            if (selectedMessagesIds[index].indexOfKey(object.getId()) < 0) {
                                selected = false;
                                break;
                            }
                        }
                        group.transitionParams.cell.drawBackground(canvas, (int) l, (int) t, (int) r, (int) b, group.transitionParams.pinnedTop, group.transitionParams.pinnedBotton, selected, 0);
                        if (group != scrimGroup) {
                            group.transitionParams.cell = null;
                        }
                        group.transitionParams.drawCaptionLayout = group.hasCaption;
                        if (useScale) {
                            canvas.restore();
                            for (int ii = 0; ii < count; ii++) {
                                View child = chatListView.getChildAt(ii);
                                if (child instanceof ChatMessageCell && ((ChatMessageCell) child).getCurrentMessagesGroup() == group) {
                                    ChatMessageCell cell = ((ChatMessageCell) child);
                                    int left = cell.getLeft();
                                    int top = cell.getTop();
                                    child.setPivotX(l - left + (r - l) / 2);
                                    child.setPivotY(t - top + (b - t) / 2);
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public boolean drawChild(Canvas canvas, View child, long drawingTime) {
                if (isSkeletonVisible()) {
                    invalidated = false;
                    invalidate();
                }

                int clipLeft = 0;
                int clipBottom = 0;
                boolean skipDraw = child == scrimView && scrimViewTask == null;
                IMessageCell mcell = null;
                ChatMessageCell cell;
                ChatActionCell actionCell = null;
                float cilpTop = 0;
                boolean isAnimatingBounds = false;
                if (child instanceof ChatMessageCell) {
                    cell = (ChatMessageCell) child;
                    mcell = cell;
                    isAnimatingBounds = cell.transitionParams.animateBackgroundBoundsInner;
                }
                if (!SizeNotifierFrameLayout.drawingBlur && (child.getY() > getMeasuredHeight() || child.getY() + child.getMeasuredHeight() < cilpTop) && !isAnimatingBounds || child.getVisibility() == View.INVISIBLE || child.getVisibility() == View.GONE) {
                    skipDraw = true;
                }

                MessageObject.GroupedMessages group = null;

                if (child instanceof ChatMessageCell) {
                    cell = (ChatMessageCell) child;
                    if (animateSendingViews.contains(cell)) {
                        skipDraw = true;
                    }
                    MessageObject.GroupedMessagePosition position = cell.getCurrentPosition();
                    group = cell.getCurrentMessagesGroup();
                    if (position != null) {
                        if (position.pw != position.spanSize && position.spanSize == 1000 && position.siblingHeights == null && group.hasSibling) {
                            clipLeft = cell.getBackgroundDrawableLeft();
                        } else if (position.siblingHeights != null) {
                            clipBottom = child.getBottom() - AndroidUtilities.dp(1 + (cell.isPinnedBottom() ? 1 : 0));
                        }
                    }
                    if (cell.needDelayRoundProgressDraw()) {
                        drawLaterRoundProgressCell = cell;
                    }
                    if (!skipDraw && scrimView instanceof ChatMessageCell && scrimViewTask == null) {
                        ChatMessageCell cell2 = (ChatMessageCell) scrimView;
                        if (cell2.getCurrentMessagesGroup() != null && cell2.getCurrentMessagesGroup() == group) {
                            skipDraw = true;
                        }
                    }
                    if (skipDraw) {
                        cell.getPhotoImage().skipDraw();
                    }
                } else if (child instanceof ChatActionCell) {
                    actionCell = (ChatActionCell) child;
                    cell = null;
                } else {
                    cell = null;
                }
                if (clipLeft != 0) {
                    canvas.save();
                } else if (clipBottom != 0) {
                    canvas.save();
                }

                if (skipDraw) {
                    /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (DownscaleScrollableNoiseSuppressor.isRecordingCanvas(canvas)) {
                            skipDraw = false;
                        }
                    }*/
                    skipDraw = false;
                }

                boolean result;
                if (!skipDraw) {
                    boolean clipToGroupBounds = (cell != null && !cell.transitionParams.needsStopClipping) && (group != null && group.transitionParams.backgroundChangeBounds);
                    if (clipToGroupBounds) {
                        canvas.save();
                        float x = cell.getNonAnimationTranslationX(true);
                        float l = (group.transitionParams.left + x + group.transitionParams.offsetLeft);
                        float t = (group.transitionParams.top + group.transitionParams.offsetTop);
                        float r = (group.transitionParams.right + x + group.transitionParams.offsetRight);
                        float b = (group.transitionParams.bottom + group.transitionParams.offsetBottom);

                        canvas.clipRect(
                                l + AndroidUtilities.dp(4),
                                t + AndroidUtilities.dp(4),
                                r - AndroidUtilities.dp(4),
                                b - AndroidUtilities.dp(4)
                        );
                    }
                    if (cell != null && cell.transitionParams.needsStopClipping) {
                        canvas.save();
                        canvas.translate(cell.getX(), cell.getY());
                        cell.drawInternal(canvas);
                        canvas.restore();
                        result = cell.transitionParams.animateChange;
                    } else if (cell != null && clipToGroupBounds) {
                        cell.clipToGroupBounds = true;
                        result = super.drawChild(canvas, child, drawingTime);
                        cell.clipToGroupBounds = false;
                    } else {
                        result = super.drawChild(canvas, child, drawingTime);
                    }
                    if (clipToGroupBounds) {
                        canvas.restore();
                    }
                    if (cell != null && cell.hasOutboundsContent()) {
                        canvas.save();
                        canvas.translate(cell.getX(), cell.getY() + cell.getPaddingTopAnimated());
                        cell.drawOutboundsContent(canvas);
                        canvas.restore();
                    } else if (actionCell != null) {
                        canvas.save();
                        canvas.translate(actionCell.getX(), actionCell.getY());
                        actionCell.drawOutboundsContent(canvas);
                        canvas.restore();
                    }
                } else {
                    result = false;
                }
                if (clipLeft != 0 || clipBottom != 0) {
                    canvas.restore();
                }

                if (child.getTranslationY() != 0) {
                    canvas.save();
                    canvas.translate(0, child.getTranslationY());
                }

                if (cell != null) {
                    cell.drawCheckBox(canvas);
                }

                if (child.getTranslationY() != 0) {
                    canvas.restore();
                }

                if (child.getTranslationY() != 0) {
                    canvas.save();
                    canvas.translate(0, child.getTranslationY());
                }

                if (cell != null) {
                    final MessageObject message = cell.getMessageObject();
                    final MessageObject.GroupedMessagePosition position = cell.getCurrentPosition();
                    if (!skipDraw) {
                        if (position != null || cell.getTransitionParams().animateBackgroundBoundsInner) {
                            if (position == null || (position.last || position.minX == 0 && position.minY == 0)) {
                                if (position == null || position.last) {
                                    drawTimeAfter.add(cell);
                                }
                                if ((position == null || (position.minX == 0 && position.minY == 0)) && cell.hasNameLayout()) {
                                    drawNamesAfter.add(cell);
                                }
                            }
                            if (position != null || cell.getTransitionParams().transformGroupToSingleMessage || cell.getTransitionParams().animateBackgroundBoundsInner) {
                                if (position == null || (position.flags & cell.captionFlag()) != 0) {
                                    drawCaptionAfter.add(cell);
                                }
                                if (position == null || (position.flags & MessageObject.POSITION_FLAG_BOTTOM) != 0 && (position.flags & MessageObject.POSITION_FLAG_LEFT) != 0) {
                                    drawReactionsAfter.add(cell);
                                }
                            }
                        }

                        if (videoPlayerContainer != null && (message.isRoundVideo() || message.isVideo()) && !message.isVoiceTranscriptionOpen() && MediaController.getInstance().isPlayingMessage(message)) {
                            ImageReceiver imageReceiver = cell.getPhotoImage();
                            float newX = imageReceiver.getImageX() + cell.getX();
                            float newY = cell.getY() + cell.getPaddingTop() + imageReceiver.getImageY() + chatListView.getY() - videoPlayerContainer.getTop();
                            if (videoPlayerContainer.getTranslationX() != newX || videoPlayerContainer.getTranslationY() != newY) {
                                videoPlayerContainer.setTranslationX(newX);
                                videoPlayerContainer.setTranslationY(newY);
                                fragmentView.invalidate();
                                videoPlayerContainer.invalidate();
                            }
                        }
                    }
                }
                if (mcell != null) {
                    final MessageObject message = mcell.getMessageObject();
                    final MessageObject.GroupedMessagePosition position = mcell.getCurrentPosition();
                    final ImageReceiver imageReceiver = mcell.getAvatarImage();
                    if (imageReceiver != null && getSideMenuAlpha() < 1.f) {
                        final MessageObject.GroupedMessages groupedMessages = getValidGroupedMessage(message);
                        boolean updateVisibility = !mcell.getMessageObject().deleted && chatListView.getChildAdapterPosition(child) != RecyclerView.NO_POSITION;

                        boolean replaceAnimation = chatListView.isFastScrollAnimationRunning() || (groupedMessages != null && groupedMessages.transitionParams.backgroundChangeBounds);
                        int top = (replaceAnimation ? child.getTop() : (int) child.getY()) + child.getPaddingTop();
                        if (mcell.drawPinnedBottom()) {
                            int p;
                            if (mcell.willRemovedAfterAnimation()) {
                                p = chatScrollHelper.positionToOldView.indexOfValue(child);
                                if (p >= 0) {
                                    p = chatScrollHelper.positionToOldView.keyAt(p);
                                }
                            } else {
                                ViewHolder holder = chatListView.getChildViewHolder(child);
                                p = holder.getAdapterPosition();
                            }

                            if (p >= 0) {
                                int nextPosition;
                                if (groupedMessages != null && position != null) {
                                    int idx = groupedMessages.posArray.indexOf(position);
                                    int size = groupedMessages.posArray.size();
                                    if ((position.flags & MessageObject.POSITION_FLAG_BOTTOM) != 0) {
                                        nextPosition = p - size + idx;
                                    } else {
                                        nextPosition = p - 1;
                                        for (int a = idx + 1; a < size; a++) {
                                            if (groupedMessages.posArray.get(a).minY > position.maxY) {
                                                break;
                                            } else {
                                                nextPosition--;
                                            }
                                        }
                                    }
                                } else {
                                    nextPosition = p - 1;
                                }
                                if (mcell.willRemovedAfterAnimation()) {
                                     View view = chatScrollHelper.positionToOldView.get(nextPosition);
                                     if (view != null) {
                                         if (child.getTranslationY() != 0) {
                                             canvas.restore();
                                         }
                                         imageReceiver.setVisible(false, false);
                                         return result;
                                     }
                                } else {
                                    ViewHolder holder = chatListView.findViewHolderForAdapterPosition(nextPosition);
                                    if (holder != null) {
                                        if (child.getTranslationY() != 0) {
                                            canvas.restore();
                                        }
                                        imageReceiver.setVisible(false, false);
                                        return result;
                                    }
                                }
                            }
                        }
                        float tx = mcell.getSlidingOffsetX() + mcell.getCheckBoxTranslation();
                        int y = 0;
                        y += replaceAnimation ? child.getTop() : child.getY();
                        if (mcell instanceof ChatMessageCell) {
                            y += ((ChatMessageCell) mcell).getPaddingTopAnimated();
                        } else {
                            y += child.getPaddingTop();
                        }
                        if (mcell instanceof ChatMessageCell) {
                            y += cell.getLayoutHeight() + cell.getTransitionParams().deltaBottom;
                        } else {
                            y += child.getHeight();
                        }
                        int maxY = chatListView.getMeasuredHeight() - chatListView.getPaddingBottom();
                        boolean canUpdateTx = false;
                        if (mcell instanceof ChatMessageCell) {
                            final ChatMessageCell cmcell = (ChatMessageCell) mcell;
                            canUpdateTx = cmcell.isCheckBoxVisible() && tx == 0;
                            if (cmcell.isPlayingRound() || cmcell.getTransitionParams().animatePlayingRound) {
                                if (cmcell.getTransitionParams().animatePlayingRound) {
                                    float progressLocal = cmcell.getTransitionParams().animateChangeProgress;
                                    if (!cmcell.isPlayingRound()) {
                                        progressLocal = 1f - progressLocal;
                                    }
                                    int fromY = y;
                                    int toY = Math.min(y, maxY);
                                    y = (int) (fromY * progressLocal + toY * (1f - progressLocal));
                                }
                            } else {
                                if (y > maxY) {
                                    y = maxY;
                                }
                            }
                        } else {
                            if (y > maxY) {
                                y = maxY;
                            }
                        }

                        if (!replaceAnimation && child.getTranslationY() != 0) {
                            canvas.restore();
                        }
                        if (mcell.drawPinnedTop()) {
                            int p;
                            if (mcell.willRemovedAfterAnimation()) {
                                p = chatScrollHelper.positionToOldView.indexOfValue(child);
                                if (p >= 0) {
                                    p = chatScrollHelper.positionToOldView.keyAt(p);
                                }
                            } else {
                                ViewHolder holder = chatListView.getChildViewHolder(child);
                                p = holder.getAdapterPosition();
                            }
                            if (p >= 0) {
                                int tries = 0;
                                while (true) {
                                    if (tries >= 20) {
                                        break;
                                    }
                                    tries++;

                                    int prevPosition;
                                    if (groupedMessages != null && position != null) {
                                        int idx = groupedMessages.posArray.indexOf(position);
                                        if (idx < 0) {
                                            break;
                                        }
                                        int size = groupedMessages.posArray.size();
                                        if ((position.flags & MessageObject.POSITION_FLAG_TOP) != 0) {
                                            prevPosition = p + idx + 1;
                                        } else {
                                            prevPosition = p + 1;
                                            for (int a = idx - 1; a >= 0; a--) {
                                                if (groupedMessages.posArray.get(a).maxY < position.minY) {
                                                    break;
                                                } else {
                                                    prevPosition++;
                                                }
                                            }
                                        }
                                    } else {
                                        prevPosition = p + 1;
                                    }
                                    if (mcell.willRemovedAfterAnimation()) {
                                        final View view = chatScrollHelper.positionToOldView.get(prevPosition);
                                        if (view != null) {
                                            top = view.getTop() + view.getPaddingTop();
                                            if (view instanceof IMessageCell) {
                                                mcell = (IMessageCell) view;
                                                float newTx = mcell.getSlidingOffsetX() + mcell.getCheckBoxTranslation();
                                                if (canUpdateTx && newTx > 0) {
                                                    tx = newTx;
                                                }
                                                if (!mcell.drawPinnedTop()) {
                                                    break;
                                                } else {
                                                    p = prevPosition;
                                                }
                                            } else {
                                                break;
                                            }
                                        } else {
                                            break;
                                        }
                                    } else {
                                        final ViewHolder holder = chatListView.findViewHolderForAdapterPosition(prevPosition);
                                        if (holder != null) {
                                            top = holder.itemView.getTop() + holder.itemView.getPaddingTop();
                                            if (holder.itemView instanceof ChatMessageCell) {
                                                mcell = (IMessageCell) holder.itemView;
                                                float newTx = mcell.getSlidingOffsetX() + mcell.getCheckBoxTranslation();
                                                if (canUpdateTx && newTx > 0) {
                                                    tx = newTx;
                                                }
                                                if (!mcell.drawPinnedTop()) {
                                                    break;
                                                } else {
                                                    p = prevPosition;
                                                }
                                            } else {
                                                break;
                                            }
                                        } else {
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        if (y - dp(48) < top) {
                            y = top + dp(48);
                        }
                        if (!mcell.drawPinnedBottom()) {
                            int cellBottom;
                            if (replaceAnimation) {
                                cellBottom = child.getBottom();
                            } else {
                                cellBottom = (int) (mcell.getY() + mcell.getMeasuredHeight() + mcell.getDeltaBottom());
                            }
                            if (y > cellBottom) {
                                y = cellBottom;
                            }
                        }
                        canvas.save();
                        if (tx != 0) {
                            canvas.translate(tx, 0);
                        }
                        if (mcell instanceof ChatMessageCell) {
                            final ChatMessageCell chatMessageCell = (ChatMessageCell) mcell;
                            if (chatMessageCell.getCurrentMessagesGroup() != null) {
                                if (chatMessageCell.getCurrentMessagesGroup().transitionParams.backgroundChangeBounds) {
                                    y -= chatMessageCell.getTranslationY();
                                }
                            }
                        }
                        if (updateVisibility) {
                            imageReceiver.setImageY(y - dp(44));
                        }
                        if (mcell.shouldDrawAlphaLayer()) {
                            imageReceiver.setAlpha((1f - getSideMenuAlpha()) * mcell.getAlpha());
                            canvas.scale(
                                mcell.getScaleX(), mcell.getScaleY(),
                                mcell.getX() + mcell.getPivotX(),
                                mcell.getY() + (mcell.getHeight() >> 1)
                            );
                        } else {
                            imageReceiver.setAlpha(1f - getSideMenuAlpha());
                        }
                        if (updateVisibility) {
                            imageReceiver.setVisible(true, false);
                        }
                        if (getSideMenuAlpha() > 0f) {
                            canvas.scale(1f - getSideMenuAlpha(), 1f - getSideMenuAlpha(), imageReceiver.getImageX2(), imageReceiver.getImageY2());
                            canvas.translate(dp(24) * getSideMenuAlpha(), 0f);
                        }
                        imageReceiver.draw(canvas);
                        canvas.restore();

                        if (!replaceAnimation && child.getTranslationY() != 0) {
                            canvas.save();
                        }
                    }
                }

                if (child.getTranslationY() != 0) {
                    canvas.restore();
                }
                return result;
            }

            @Override
            public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
                if (currentEncryptedChat != null) {
                    return;
                }
                super.onInitializeAccessibilityNodeInfo(info);
                AccessibilityNodeInfo.CollectionInfo collection = info.getCollectionInfo();
                if (collection != null) {
                    info.setCollectionInfo(AccessibilityNodeInfo.CollectionInfo.obtain(collection.getRowCount(), 1, false));
                }
            }

            @Override
            public AccessibilityNodeInfo createAccessibilityNodeInfo() {
                if (currentEncryptedChat != null) {
                    return null;
                }
                return super.createAccessibilityNodeInfo();
            }
        };
        chatListView.addEdgeEffectListener(() -> invalidateMergedVisibleBlurredPositionsAndSources(BLUR_INVALIDATE_FLAG_SCROLL | BLUR_INVALIDATE_FLAG_CLIP));
        if (currentEncryptedChat != null) {
            chatListView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        }
        chatListView.setHideIfEmpty(false);
        chatListView.setAccessibilityEnabled(false);
        chatListView.setNestedScrollingEnabled(false);
        chatListView.setInstantClick(true);
        chatListView.setDisableHighlightState(true);
        chatListView.setTag(1);
        chatListView.setVerticalScrollBarEnabled(!SharedConfig.chatBlurEnabled());
        chatListView.setAdapter(chatAdapter = new ChatActivityAdapter(context));
        chatListView.setClipToPadding(false);
        if (ChatObject.isMonoForum(currentChat) || ChatObject.areTabsEnabled(currentChat)) {
            chatListView.setClipChildren(false);
        }
        chatListView.setAnimateEmptyView(true, RecyclerListView.EMPTY_VIEW_ANIMATION_TYPE_ALPHA_SCALE);
        chatListView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        chatListViewPaddingsAnimator = new ChatListViewPaddingsAnimator(chatListView);
        chatListViewPaddingTop = 0;
        paddingTopHeight = 0;
        botDraftHeightController.setRecyclerView(chatListView);
        invalidateChatListViewTopPadding();
        if (MessagesController.getGlobalMainSettings().getBoolean("view_animations", true)) {
            chatListItemAnimator = new ChatListItemAnimator(this, chatListView, themeDelegate) {

                Runnable finishRunnable;

                @Override
                public void checkIsRunning() {
                    if (scrollAnimationIndex == -1) {
                        scrollAnimationIndex = getNotificationCenter().setAnimationInProgress(scrollAnimationIndex, allowedNotificationsDuringChatListAnimations, false);
                    }
                }

                @Override
                public void onAnimationStart() {
                    scrollAnimationIndex = getNotificationCenter().setAnimationInProgress(scrollAnimationIndex, allowedNotificationsDuringChatListAnimations, false);
                    if (finishRunnable != null) {
                        AndroidUtilities.cancelRunOnUIThread(finishRunnable);
                        finishRunnable = null;
                    }
                    if (BuildVars.LOGS_ENABLED) {
                        FileLog.d("chatItemAnimator disable notifications");
                    }
                    chatActivityEnterView.getAdjustPanLayoutHelper().runDelayedAnimation();
                    chatActivityEnterView.runEmojiPanelAnimation();
                }

                @Override
                protected void onAllAnimationsDone() {
                    super.onAllAnimationsDone();
                    if (finishRunnable != null) {
                        AndroidUtilities.cancelRunOnUIThread(finishRunnable);
                        finishRunnable = null;
                    }
                    AndroidUtilities.runOnUIThread(finishRunnable = () -> {
                        finishRunnable = null;
                        if (scrollAnimationIndex != -1) {
                            getNotificationCenter().onAnimationFinish(scrollAnimationIndex);
                            scrollAnimationIndex = -1;
                        }
                        if (BuildVars.LOGS_ENABLED) {
                            FileLog.d("chatItemAnimator enable notifications");
                        }
                    });
                }


                @Override
                public void endAnimations() {
                    super.endAnimations();
                    if (finishRunnable != null) {
                        AndroidUtilities.cancelRunOnUIThread(finishRunnable);
                    }
                    AndroidUtilities.runOnUIThread(finishRunnable = () -> {
                        finishRunnable = null;
                        if (scrollAnimationIndex != -1) {
                            getNotificationCenter().onAnimationFinish(scrollAnimationIndex);
                            scrollAnimationIndex = -1;
                        }
                        if (BuildVars.LOGS_ENABLED) {
                            FileLog.d("chatItemAnimator enable notifications");
                        }
                    });
                }
            };
            chatListItemAnimator.setOnSnapMessage(this::supportsThanosEffect, this::getChatThanosEffect);
        }

        chatLayoutManager = new GridLayoutManagerFixed(context, 1000, LinearLayoutManager.VERTICAL, !reversed) {

            boolean computingScroll;

            @Override
            public int getStartForFixGap() {
                int padding = (int) chatListViewPaddingTop;
                return padding;
            }

            @Override
            protected int getParentStart() {
                if (computingScroll) {
                    return (int) chatListViewPaddingTop;
                }
                return 0;
            }

            @Override
            public int getStartAfterPadding() {
                if (computingScroll) {
                    return (int) chatListViewPaddingTop;
                }
                return super.getStartAfterPadding();
            }

            @Override
            public int getTotalSpace() {
                if (computingScroll) {
                    return (int) (getHeight() - chatListViewPaddingTop - getPaddingBottom());
                }
                return super.getTotalSpace();
            }

            @Override
            public int computeVerticalScrollExtent(RecyclerView.State state) {
                computingScroll = true;
                int r = super.computeVerticalScrollExtent(state);
                computingScroll = false;
                return r;
            }

            @Override
            public int computeVerticalScrollOffset(RecyclerView.State state) {
                computingScroll = true;
                int r = super.computeVerticalScrollOffset(state);
                computingScroll = false;
                return r;
            }

            @Override
            public int computeVerticalScrollRange(RecyclerView.State state) {
                computingScroll = true;
                int r = super.computeVerticalScrollRange(state);
                computingScroll = false;
                return r;
            }

            @Override
            public void scrollToPositionWithOffset(int position, int offset, boolean bottom) {
                if (!bottom) {
                    offset = (int) (offset - getPaddingTop() + chatListViewPaddingTop);
                }
                super.scrollToPositionWithOffset(position, offset, bottom);
            }

            @Override
            public boolean supportsPredictiveItemAnimations() {
                return true;
            }

            @Override
            public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
                scrollByTouch = false;
                LinearSmoothScrollerCustom linearSmoothScroller = new LinearSmoothScrollerCustom(recyclerView.getContext(), LinearSmoothScrollerCustom.POSITION_MIDDLE);
                linearSmoothScroller.setTargetPosition(position);
                startSmoothScroll(linearSmoothScroller);
            }

            @Override
            public boolean shouldLayoutChildFromOpositeSide(View child) {
                if (child instanceof ChatMessageCell) {
                    return !((ChatMessageCell) child).getMessageObject().isOutOwner();
                }
                return false;
            }


            @Override
            protected boolean hasSiblingChild(int position) {
                if (position >= chatAdapter.messagesStartRow && position < chatAdapter.messagesEndRow) {
                    int index = position - chatAdapter.messagesStartRow;
                    if (index >= 0 && index < chatAdapter.getMessages().size()) {
                        MessageObject message = chatAdapter.getMessages().get(index);
                        MessageObject.GroupedMessages group = getValidGroupedMessage(message);
                        if (group != null) {
                            MessageObject.GroupedMessagePosition pos = group.getPosition(message);
                            if (pos.minX == pos.maxX || pos.minY != pos.maxY || pos.minY == 0) {
                                return false;
                            }
                            int count = group.posArray.size();
                            for (int a = 0; a < count; a++) {
                                MessageObject.GroupedMessagePosition p = group.posArray.get(a);
                                if (p == pos) {
                                    continue;
                                }
                                if (p.minY <= pos.minY && p.maxY >= pos.minY) {
                                    return true;
                                }
                            }
                        }
                    }
                }
                return false;
            }

            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                if (BuildVars.DEBUG_PRIVATE_VERSION) {
                    super.onLayoutChildren(recycler, state);
                } else {
                    try {
                        super.onLayoutChildren(recycler, state);
                    } catch (Exception e) {
                        FileLog.e(e);
                        AndroidUtilities.runOnUIThread(() -> chatAdapter.notifyDataSetChanged(false));
                    }
                }
            }

            /*
            @Override
            public boolean canScrollVertically() {
                return !isInPollAddOptionMode() && super.canScrollVertically();
            }
            */

            @Override
            public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
                if (dy < 0 && pullingDownOffset != 0) {
                    pullingDownOffset += dy;
                    if (pullingDownOffset < 0) {
                        dy = (int) pullingDownOffset;
                        pullingDownOffset = 0;
                        chatListView.invalidate();
                    } else {
                        dy = 0;
                    }
                }

                int n = chatListView.getChildCount();
                int scrolled = 0;
                boolean foundTopView = false;
                for (int i = 0; i < n; i++) {
                    View child = chatListView.getChildAt(i);
                    float padding = chatListViewPaddingTop;
                    if (chatListView.getChildAdapterPosition(child) == (reversed ? 0 : chatAdapter.getItemCount() - 1)) {
                        int dyLocal = dy;
                        if (child.getTop() - dy > padding) {
                            dyLocal = (int) (child.getTop() - padding);
                        }
                        scrolled = super.scrollVerticallyBy(dyLocal, recycler, state);
                        foundTopView = true;
                        break;
                    }
                }
                if (!foundTopView) {
                    scrolled = super.scrollVerticallyBy(dy, recycler, state);
                }
                final boolean allowPullingDownScroll = !isInPollAddOptionMode() && !hasSelectedMessages();
                if (allowPullingDownScroll && dy > 0 && scrolled == 0 && (ChatObject.isChannel(currentChat) && !currentChat.megagroup || isTopic && !UserObject.isBotForum(currentUser)) && chatMode != MODE_SAVED && chatMode != MODE_WELCOME_MESSAGES && chatMode != MODE_SCHEDULED && chatListView.getScrollState() == RecyclerView.SCROLL_STATE_DRAGGING && !chatListView.isFastScrollAnimationRunning() && !chatListView.isMultiselect() && !isReport()) {
                    if (pullingDownOffset == 0 && pullingDownDrawable != null) {
                        if (nextChannels != null && !nextChannels.isEmpty()) {
                            pullingDownDrawable.updateDialog(nextChannels.get(0));
                        } else if (isTopic) {
                            pullingDownDrawable.updateTopic();
                        } else {
                            pullingDownDrawable.updateDialog();
                        }
                    }
                    if (pullingDownBackAnimator != null) {
                        pullingDownBackAnimator.removeAllListeners();
                        pullingDownBackAnimator.cancel();
                    }

                    float k;
                    if (pullingDownOffset < AndroidUtilities.dp(110)) {
                        float progress = pullingDownOffset / AndroidUtilities.dp(110);
                        k = 0.65f * (1f - progress) + 0.45f * progress;
                    } else if (pullingDownOffset < AndroidUtilities.dp(160)) {
                        float progress = (pullingDownOffset - AndroidUtilities.dp(110)) / AndroidUtilities.dp(50);
                        k = 0.45f * (1f - progress) + 0.05f * progress;
                    } else {
                        k = 0.05f;
                    }

                    pullingDownOffset += dy * k;
                    ReactionsEffectOverlay.onScrolled((int) (dy * k));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && scrollableViewNoiseSuppressor != null) {
                        scrollableViewNoiseSuppressor.onScrolled(0, (dy * k));
                    }
                    chatListView.invalidate();
                }
                if (pullingDownOffset == 0) {
                    chatListView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
                } else {
                    chatListView.setOverScrollMode(View.OVER_SCROLL_NEVER);
                }
                if (pullingDownDrawable != null) {
                    animatorPullingDownContainerVisibility.setValue(pullingDownOffset > 0 && chatListView.getScrollState() == RecyclerView.SCROLL_STATE_DRAGGING, true);
                }
                return scrolled;
            }
        };
        chatLayoutManager.setSpanSizeLookup(new GridLayoutManagerFixed.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position >= chatAdapter.messagesStartRow && position < chatAdapter.messagesEndRow) {
                    int idx = position - chatAdapter.messagesStartRow;
                    if (idx >= 0 && idx < chatAdapter.getMessages().size()) {
                        MessageObject message = chatAdapter.getMessages().get(idx);
                        MessageObject.GroupedMessages groupedMessages = getValidGroupedMessage(message);
                        if (groupedMessages != null) {
                            return groupedMessages.getPosition(message).spanSize;
                        }
                    }
                }
                return 1000;
            }
        });
        chatListView.setLayoutManager(chatLayoutManager);
        chatListView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                outRect.bottom = 0;
                if (view instanceof ChatMessageCell) {
                    ChatMessageCell cell = (ChatMessageCell) view;
                    MessageObject.GroupedMessages group = cell.getCurrentMessagesGroup();
                    if (group != null) {
                        MessageObject.GroupedMessagePosition position = cell.getCurrentPosition();
                        if (position != null && position.siblingHeights != null) {
                            float maxHeight = Math.max(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) * 0.5f;
                            int h = cell.getExtraInsetHeight();
                            for (int a = 0; a < position.siblingHeights.length; a++) {
                                h += (int) Math.ceil(maxHeight * position.siblingHeights[a]);
                            }
                            h += (position.maxY - position.minY) * Math.round(7 * AndroidUtilities.density);
                            int count = group.posArray.size();
                            for (int a = 0; a < count; a++) {
                                MessageObject.GroupedMessagePosition pos = group.posArray.get(a);
                                if (pos.minY != position.minY || pos.minX == position.minX && pos.maxX == position.maxX && pos.minY == position.minY && pos.maxY == position.maxY) {
                                    continue;
                                }
                                if (pos.minY == position.minY) {
                                    h -= (int) Math.ceil(maxHeight * pos.ph) - AndroidUtilities.dp(4);
                                    break;
                                }
                            }
                            outRect.bottom = -h;
                        }
                    }
                }
            }
        });
        chatListView.setOnItemLongClickListener(onItemLongClickListener);
        chatListView.setOnItemClickListener(onItemClickListener);
        chatListView.setOnScrollListener(new RecyclerView.OnScrollListener() {

            private float totalDy = 0;
            private boolean scrollUp;
            private final int scrollValue = AndroidUtilities.dp(100);

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (pollHintCell != null) {
                        pollHintView.showForMessageCell(pollHintCell, -1, pollHintX, pollHintY, true);
                        pollHintCell = null;
                    }
                    scrollingFloatingDate = false;
                    scrollingFloatingTopic = false;
                    scrollingChatListView = false;
                    checkTextureViewPosition = false;
                    hideFloatingDateView(true);
                    hideFloatingTopicView(true);
                    if (SharedConfig.getDevicePerformanceClass() == SharedConfig.PERFORMANCE_CLASS_LOW) {
                        scrolling = true;
                        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.startAllHeavyOperations, 512);
                    }
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.startSpoilers);
                    chatListView.setOverScrollMode(RecyclerView.OVER_SCROLL_ALWAYS);
                    textSelectionHelper.stopScrolling();
                    updateVisibleRows();
                    invalidateMergedVisibleBlurredPositionsAndSources(BLUR_INVALIDATE_FLAG_SCROLL);
                    scrollByTouch = false;
                } else {
                    if (groupEmojiPackHint != null && groupEmojiPackHint.shown()) {
                        groupEmojiPackHint.hide();
                    }
                    if (searchOtherButton != null && searchOtherButton.getVisibility() == View.VISIBLE && isKeyboardVisible()) {
                        AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
                    }
                    if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
                        wasManualScroll = true;
                        scrollingChatListView = true;
                    } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        pollHintCell = null;
                        wasManualScroll = true;
                        scrollingFloatingDate = true;
                        scrollingFloatingTopic = true;
                        checkTextureViewPosition = true;
                        scrollingChatListView = true;
                    }
                    if (SharedConfig.getDevicePerformanceClass() == SharedConfig.PERFORMANCE_CLASS_LOW) {
                        scrolling = false;
                        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.stopAllHeavyOperations, 512);
                    }
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.stopSpoilers);

                    if (selectionReactionsOverlay != null && selectionReactionsOverlay.isVisible()) {
                        selectionReactionsOverlay.setHiddenByScroll(true);
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                final ChatActivity chatToUpdate = parentChatActivity != null ? parentChatActivity : ChatActivity.this;

                chatListView.invalidate();
                if (contentView != null) {
                    contentView.updateBlurContent();
                }
                if (chatListThanosEffect != null) {
                    chatListThanosEffect.scroll(dx, dy);
                }
                scrollUp = dy < 0;
                int firstVisibleItem = chatLayoutManager.findFirstVisibleItemPosition();
                if (dy != 0 && (scrollByTouch && recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_SETTLING) || recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_DRAGGING) {
                    if (forceNextPinnedMessageId != 0) {
                        if ((!scrollUp || forceScrollToFirst)) {
                            forceNextPinnedMessageId = 0;
                        } else if (!chatListView.isFastScrollAnimationRunning() && firstVisibleItem != RecyclerView.NO_POSITION) {
                            int lastVisibleItem = chatLayoutManager.findLastVisibleItemPosition();
                            MessageObject messageObject = null;
                            boolean foundForceNextPinnedView = false;
                            for (int i = lastVisibleItem; i >= firstVisibleItem; i--) {
                                View view = chatLayoutManager.findViewByPosition(i);
                                if (view instanceof ChatMessageCell) {
                                    messageObject = ((ChatMessageCell) view).getMessageObject();
                                } else if (view instanceof ChatActionCell) {
                                    messageObject = ((ChatActionCell) view).getMessageObject();
                                }
                                if (messageObject != null) {
                                    if (forceNextPinnedMessageId == messageObject.getId()) {
                                        foundForceNextPinnedView = true;
                                        break;
                                    }
                                }
                            }
                            if (!foundForceNextPinnedView && messageObject != null && messageObject.getId() < forceNextPinnedMessageId) {
                                forceNextPinnedMessageId = 0;
                            }
                        }
                    }
                }
                if (recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_DRAGGING) {
                    forceScrollToFirst = false;
                    if (!wasManualScroll && dy != 0) {
                        wasManualScroll = true;
                    }
                }
                if (dy != 0) {
                    invalidateMergedVisibleBlurredPositionsAndSources(BLUR_INVALIDATE_FLAG_SCROLL);
                    contentView.invalidateBlur();
                    hideHints(true);
                }
                if (dy != 0 && scrollingFloatingDate && !currentFloatingTopIsNotMessage) {
                    if (highlightMessageId != Integer.MAX_VALUE) {
                        removeSelectedMessageHighlight();
                        updateVisibleRows();
                    }
                    showFloatingDateView(true);
                }
                if (isAllChats() && dy != 0 && scrollingFloatingTopic && !currentFloatingTopIsNotMessage) {
                    if (highlightMessageId != Integer.MAX_VALUE) {
                        removeSelectedMessageHighlight();
                        updateVisibleRows();
                    }
                    showFloatingTopicView(true);
                }
                checkScrollForLoad(true);
                if (firstVisibleItem != RecyclerView.NO_POSITION) {
                    int totalItemCount = chatAdapter.getItemCount();
                    if (firstVisibleItem == 0 && forwardEndReached[0]) {
                        if (dy >= 0) {
                            canShowPagedownButton = false;
                            updatePagedownButtonVisibility(true);
                        }
                    } else {
                        final boolean isPageDownButtonVisible = sideControlsButtonsLayout.isButtonVisible(
                            ChatActivitySideControlsButtonsLayout.BUTTON_PAGE_DOWN);

                        if (dy > 0) {
                            if (!isPageDownButtonVisible) {
                                totalDy += dy;
                                if (totalDy > scrollValue) {
                                    totalDy = 0;
                                    canShowPagedownButton = true;
                                    updatePagedownButtonVisibility(true);
                                    pagedownButtonShowedByScroll = true;
                                }
                            }
                        } else {
                            if (pagedownButtonShowedByScroll && isPageDownButtonVisible) {
                                totalDy += dy;
                                if (totalDy < -scrollValue) {
                                    canShowPagedownButton = false;
                                    updatePagedownButtonVisibility(true);
                                    totalDy = 0;
                                }
                            }
                        }
                    }
                }
                invalidateMessagesVisiblePart();
                textSelectionHelper.onParentScrolled();
                emojiAnimationsOverlay.onScrolled(dy);
                ReactionsEffectOverlay.onScrolled(dy);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && chatToUpdate.scrollableViewNoiseSuppressor != null) {
                    chatToUpdate.scrollableViewNoiseSuppressor.onScrolled(dx, dy);
                }

                checkTranslation(false);

                if (savedMessagesTagHint != null) {
                    if (savedMessagesTagHint.shown()) {
                        savedMessagesTagHint.hide();
                    } else if (!savedMessagesTagHintShown) {
                        lastScrollTime = System.currentTimeMillis();
                        AndroidUtilities.cancelRunOnUIThread(ChatActivity.this::checkSavedMessagesTagHint);
                        AndroidUtilities.runOnUIThread(ChatActivity.this::checkSavedMessagesTagHint, 2000);
                    }
                }
                if (videoConversionTimeHint != null && videoConversionTimeHint.shown()) {
                    videoConversionTimeHint.hide();
                }
                if (botMessageHint != null && botMessageHint.shown()) {
                    botMessageHint.hide();
                } else {
                    AndroidUtilities.cancelRunOnUIThread(ChatActivity.this::checkBotMessageHint);
                    AndroidUtilities.runOnUIThread(ChatActivity.this::checkBotMessageHint, 2000);
                }
                if (factCheckHint != null) {
                    factCheckHint.hide();
                }
                if (chatActivityEnterView != null) {
                    chatActivityEnterView.hideHints();
                }
                if (starReactionsOverlay != null) {
                    starReactionsOverlay.invalidate();
                }
                if (botDraftHeightController != null) {
                    botDraftHeightController.onScroll();
                }
            }
        });

        contentView.addView(chatListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        chatActivityFadeView = new ChatActivityFadeView(context);
        chatActivityFadeView.setup(navbarContentDrawableFactory);
        chatActivityFadeView.setFadeHeightTop(dp(48));
        chatActivityFadeView.setFadeHeightBottom(dp(48));
        contentView.addView(chatActivityFadeView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        if (getDialogId() != getUserConfig().getClientUserId()) {
            selectionReactionsOverlay = new ChatSelectionReactionMenuOverlay(this, context);
            contentView.addView(selectionReactionsOverlay, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        }

        animatingImageView = new ClippingImageView(context);
        animatingImageView.setVisibility(View.GONE);
        contentView.addView(animatingImageView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        progressView = new FrameLayout(context);
        progressView.setVisibility(View.INVISIBLE);
        contentView.addView(progressView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));

        progressView2 = new View(context) {
            private final RectF rect = new RectF();
            @Override
            protected void dispatchDraw(Canvas canvas) {
                rect.set(0, 0, getWidth(), getHeight());
                applyServiceShaderMatrix();
                canvas.drawRoundRect(rect, dp(18), dp(18), getThemedPaint(Theme.key_paint_chatActionBackground));
                if (themeDelegate != null ? themeDelegate.hasGradientService() : Theme.hasGradientService()) {
                    canvas.drawRoundRect(rect, dp(18), dp(18), getThemedPaint(Theme.key_paint_chatActionBackgroundDarken));
                }
                super.dispatchDraw(canvas);
            }
            public void applyServiceShaderMatrix() {
                applyServiceShaderMatrix(getMeasuredWidth(), getServiceHeight(this), getX(), getServiceTop(this));
            }
            private void applyServiceShaderMatrix(int measuredWidth, int backgroundHeight, float x, float viewTop) {
                if (themeDelegate != null) {
                    themeDelegate.applyServiceShaderMatrix(measuredWidth, backgroundHeight, x, viewTop);
                } else {
                    Theme.applyServiceShaderMatrix(measuredWidth, backgroundHeight, x, viewTop);
                }
            }
        };
        progressView.addView(progressView2, LayoutHelper.createFrame(36, 36, Gravity.CENTER));

        progressBar = new RadialProgressView(context, themeDelegate);
        progressBar.setSize(AndroidUtilities.dp(28));
        progressBar.setProgressColor(getThemedColor(Theme.key_chat_serviceText));
        progressView.addView(progressBar, LayoutHelper.createFrame(32, 32, Gravity.CENTER));

        floatingTopicSeparator = new TopicSeparator.Cell(context, currentAccount, themeDelegate) {
            @Override
            public void setTranslationY(float translationY) {
                if (getTranslationY() != translationY) {
                    invalidate();
                }
                super.setTranslationY(translationY);
            }

            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (getAlpha() == 0 || actionBar.isActionModeShowed() || isReport()) {
                    return false;
                }
                return super.onTouchEvent(event);
            }
        };
        floatingTopicSeparator.setOnTopicClickListener(topicId -> {
            if (topicsTabs != null) {
                topicsTabs.selectTopic(topicId, true);
            }
        });
        floatingTopicSeparator.setVisibility(View.INVISIBLE);
        contentView.addView(floatingTopicSeparator, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 4, 0, 0));

        floatingDateView = new ChatActionCell(context, false, themeDelegate) {
            @Override
            public boolean isFloating() {
                return true;
            }

            @Override
            public void setTranslationY(float translationY) {
                if (getTranslationY() != translationY) {
                    invalidate();
                }
                super.setTranslationY(translationY);
            }

            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                if (getAlpha() == 0 || actionBar.isActionModeShowed() || isReport()) {
                    return false;
                }
                return super.onInterceptTouchEvent(ev);
            }

            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (getAlpha() == 0 || actionBar.isActionModeShowed() || isReport()) {
                    return false;
                }
                return super.onTouchEvent(event);
            }

            @Override
            protected void onDraw(Canvas canvas) {
                if (scrimBlurBitmap != null) return;
                float clipTop = chatListView.getY() + chatListViewPaddingTop - getY();
                clipTop -= AndroidUtilities.dp(4);
                if (clipTop > 0) {
                    if (clipTop < getMeasuredHeight()) {
                        canvas.save();
                        canvas.clipRect(0, clipTop, getMeasuredWidth(), getMeasuredHeight());
                        super.onDraw(canvas);
                        canvas.restore();
                    }
                } else {
                    super.onDraw(canvas);
                }
            }

            @Override
            public void setAlpha(float alpha) {
                super.setAlpha(alpha);
                setVisibility(alpha > 0 ? View.VISIBLE : INVISIBLE);
            }
        };
        floatingDateView.setCustomDate((int) (System.currentTimeMillis() / 1000), false, false);
        floatingDateView.setAlpha(0.0f);
        floatingDateView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        floatingDateView.setInvalidateColors(true);
        contentView.addView(floatingDateView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 4, 0, 0));
        floatingDateView.setOnActionClickListener(view -> {
            if (floatingDateView.getAlpha() == 0 || actionBar.isActionModeShowed() || isReport()) {
                return;
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis((long) floatingDateView.getCustomDate() * 1000);
            int year = calendar.get(Calendar.YEAR);
            int monthOfYear = calendar.get(Calendar.MONTH);
            int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

            calendar.clear();
            calendar.set(year, monthOfYear, dayOfMonth);
            jumpToDate((int) (calendar.getTime().getTime() / 1000));
        });

        if (currentChat != null && chatMode != MODE_WELCOME_MESSAGES) {
            // todo: only for default mode ??
            pendingRequestsDelegate = new ChatActivityMemberRequestsDelegate(this, currentChat);
            topPanelLayout.addView(pendingRequestsDelegate.getView(), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 40));
            topPanelLayout.setPriority(pendingRequestsDelegate.getView(), 3);
            topPanelLayout.setDebugName(pendingRequestsDelegate.getView(), "pendingRequestsDelegate");
            pendingRequestsDelegate.setDelegate((v, a) -> topPanelLayout.setViewVisible(pendingRequestsDelegate.getView(), v, a));
            pendingRequestsDelegate.setChatInfo(chatInfo, false);
        }

        pinnedMessageView = null;

        undoView = null;
        topUndoView = null;
        topChatPanelView = null;
        reportSpamButton = null;
        emojiStatusSpamHint = null;
        addToContactsButton = null;
        restartTopicButton = null;
        closeReportSpam = null;
        translateButton = null;
        addProfilePictureButton = null;
        topicsTabs = null;
        botAdView = null;
        bizBotButton = null;

        // topButtonsLayout = new ChatActivitySideControlsButtonsLayout(context, resourceProvider, blurredBackgroundColorProvider, glassBackgroundDrawableFactory);
        // topButtonsLayout.setOnClickListener(this::onSideControlButtonOnClick);
        // topButtonsLayout.setOnLongClickListener(this::onSideControlButtonOnLongClick);
        // topButtonsLayout.setGravity(Gravity.TOP | Gravity.RIGHT);
        // contentView.addView(topButtonsLayout, LayoutHelper.createFrame(57, 300, Gravity.RIGHT | Gravity.TOP));

        sideControlsButtonsLayout = new ChatActivitySideControlsButtonsLayout(context, resourceProvider, blurredBackgroundColorProvider, glassBackgroundDrawableFactory);
        sideControlsButtonsLayout.setOnClickListener(this::onSideControlButtonOnClick);
        sideControlsButtonsLayout.setOnLongClickListener(this::onSideControlButtonOnLongClick);
        {
            final int indexToAdd = chatActivityFadeView != null ? contentView.indexOfChild(chatActivityFadeView) : -1;
            contentView.addView(sideControlsButtonsLayout, indexToAdd, LayoutHelper.createFrame(57, 300, Gravity.RIGHT | Gravity.BOTTOM));
        }

        updateMessageListAccessibilityVisibility();
        mentionContainer = new MentionsContainerView(context, dialog_id, threadMessageId, ChatActivity.this, themeDelegate) {

            @Override
            protected boolean canOpen() {
                return bottomOverlay.getVisibility() != View.VISIBLE || searchingForUser;
            }

            @Override
            protected void onOpen() {
                if (allowStickersPanel && (!getAdapter().isBotContext() || (allowContextBotPanel || allowContextBotPanelSecond))) {
                    if (currentEncryptedChat != null && getAdapter().isBotContext()) {
                        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
                        if (!preferences.getBoolean("secretbot", false)) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), themeDelegate);
                            builder.setTitle(LocaleController.getString(R.string.AppName));
                            builder.setMessage(LocaleController.getString(R.string.SecretChatContextBotAlert));
                            builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
                            showDialog(builder.create());
                            preferences.edit().putBoolean("secretbot", true).commit();
                        }
                    }
                }
                updateMessageListAccessibilityVisibility();
            }

            @Override
            protected void onClose() {
                updateMessageListAccessibilityVisibility();
            }

            @Override
            protected void onContextSearch(boolean searching) {
                if (chatActivityEnterView != null) {
                    chatActivityEnterView.setCaption(getAdapter().getBotCaption());
                    chatActivityEnterView.showContextProgress(searching);
                }
            }

            @Override
            protected void onContextClick(TLRPC.BotInlineResult result) {
                if (getParentActivity() == null || result.content == null) {
                    return;
                }
                if (result.type.equals("video") || result.type.equals("web_player_video")) {
                    int[] size = MessageObject.getInlineResultWidthAndHeight(result);
                    EmbedBottomSheet.show(ChatActivity.this, null, botContextProvider, result.title != null ? result.title : "", result.description, result.content.url, result.content.url, size[0], size[1], isKeyboardVisible());
                } else {
                    processExternalUrl(0, result.content.url, null, null, false, false);
                }
            }

            private boolean wasAtTop = true;
            @Override
            protected void onScrolled(boolean atTop, boolean atBottom) {
                if (wasAtTop != atTop) {
                    AndroidUtilities.updateViewShow(suggestEmojiPanel, !isInPreviewMode() && atTop, false, true);
                    wasAtTop = atTop;
                }
            }

            @Override
            public boolean dispatchTouchEvent(MotionEvent ev) {
                if (getAlpha() <= 0f) return false;
                return super.dispatchTouchEvent(ev);
            }
        };
        if (isInPreviewMode()) {
            mentionContainer.setAlpha(0f);
        }
        mentionContainer.setDialogId(dialog_id);
        mentionContainer.setBackgroundDrawable(glassBackgroundDrawableFactoryFrosted.create(mentionContainer, blurredBackgroundColorProviderWhite));
        {
            final int indexToAdd = chatActivityFadeView != null ? contentView.indexOfChild(chatActivityFadeView) : -1;
            contentView.addView(mentionContainer, indexToAdd, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 110, Gravity.LEFT | Gravity.BOTTOM));
        }
        contentPreviewViewerDelegate = new ContentPreviewViewer.ContentPreviewViewerDelegate() {
            @Override
            public void sendSticker(TLRPC.Document sticker, String query, Object parent, boolean notify, int scheduleDate, int scheduleRepeatPeriod) {
                chatActivityEnterView.onStickerSelected(sticker, query, parent, null, true, notify, scheduleDate, scheduleRepeatPeriod);
            }

            @Override
            public boolean needSend(int contentType) {
                return true;
            }

            @Override
            public boolean canSchedule() {
                return ChatActivity.this.canScheduleMessage();
            }

            @Override
            public boolean isInScheduleMode() {
                return chatMode == MODE_SCHEDULED;
            }

            @Override
            public void openSet(TLRPC.InputStickerSet set, boolean clearsInputField) {
                if (set == null || getParentActivity() == null) {
                    return;
                }
                TLRPC.TL_inputStickerSetID inputStickerSet = new TLRPC.TL_inputStickerSetID();
                inputStickerSet.access_hash = set.access_hash;
                inputStickerSet.id = set.id;
                StickersAlert alert = new StickersAlert(getParentActivity(), ChatActivity.this, inputStickerSet, null, chatActivityEnterView, themeDelegate, false);
                alert.setCalcMandatoryInsets(isKeyboardVisible());
                alert.setClearsInputField(clearsInputField);
                showDialog(alert);
            }

            @Override
            public long getDialogId() {
                return dialog_id;
            }
        };
        mentionContainer.getListView().setOnTouchListener((v, event) -> ContentPreviewViewer.getInstance().onTouch(event, mentionContainer.getListView(), 0, mentionsOnItemClickListener, mentionContainer.getAdapter().isStickers() ? contentPreviewViewerDelegate : null, themeDelegate));
        if (!ChatObject.isChannel(currentChat) || currentChat.megagroup) {
            mentionContainer.getAdapter().setBotInfo(botInfo);
        }
        mentionContainer.getAdapter().setParentFragment(this);
        mentionContainer.getAdapter().setChatInfo(chatInfo);
        mentionContainer.getAdapter().setNeedUsernames(currentChat != null);
        mentionContainer.getAdapter().setNeedBotContext(true);
        mentionContainer.getAdapter().setBotsCount(currentChat != null ? botsCount : 1);
        mentionContainer.getListView().setOnItemClickListener(mentionsOnItemClickListener = (view, position) -> {
            if (position == 0 || mentionContainer.getAdapter().isBannedInline()) {
                return;
            }
            position--;
            Object object = mentionContainer.getAdapter().getItem(position);
            int start = mentionContainer.getAdapter().getResultStartPosition();
            int len = mentionContainer.getAdapter().getResultLength();
            if (mentionContainer.getAdapter().isLocalHashtagHint(position)) {
                chatActivityEnterView.replaceWithText(start, len, mentionContainer.getAdapter().getHashtagHint() + "@" + ChatObject.getPublicUsername(currentChat) + " ", false);
                return;
            } else if (mentionContainer.getAdapter().isGlobalHashtagHint(position)) {
                chatActivityEnterView.replaceWithText(start, len, mentionContainer.getAdapter().getHashtagHint() + " ", false);
                return;
            }
            if (object instanceof QuickRepliesController.QuickReply) {
                if (!getUserConfig().isPremium()) {
                    showDialog(new PremiumFeatureBottomSheet(this, getContext(), currentAccount, true, PremiumPreviewFragment.PREMIUM_FEATURE_BUSINESS_QUICK_REPLIES, false, null));
                    return;
                }
                AlertsCreator.ensurePaidMessageConfirmation(currentAccount, dialog_id, Math.max(1, ((QuickRepliesController.QuickReply) object).getMessagesCount()), payStars -> {
                    TLRPC.TL_messages_sendQuickReplyMessages req = new TLRPC.TL_messages_sendQuickReplyMessages();
                    req.peer = getMessagesController().getInputPeer(dialog_id);
                    req.shortcut_id = ((QuickRepliesController.QuickReply) object).id;
                    getConnectionsManager().sendRequest(req, null);
                    if (chatActivityEnterView != null) {
                        chatActivityEnterView.setFieldText(null);
                    }
                });
            } else if (object instanceof TLRPC.TL_document) {
                if (chatMode == 0 && checkSlowMode(view)) {
                    return;
                }
                MessageObject.SendAnimationData sendAnimationData;
                if (view instanceof StickerCell) {
                    sendAnimationData = ((StickerCell) view).getSendAnimationData();
                } else {
                    sendAnimationData = null;
                }
                TLRPC.TL_document document = (TLRPC.TL_document) object;
                Object parent = mentionContainer.getAdapter().getItemParent(position);
                String query = MessageObject.findAnimatedEmojiEmoticon(document);
                AlertsCreator.ensurePaidMessageConfirmation(currentAccount, getDialogId(), 1, price -> {
                    if (chatMode == MODE_SCHEDULED) {
                        AlertsCreator.createScheduleDatePickerDialog(getParentActivity(), dialog_id, (notify, scheduleDate, scheduleRepeatPeriod) -> SendMessagesHelper.getInstance(currentAccount).sendSticker(document, query, dialog_id, replyingMessageObject, getThreadMessage(), null, replyingQuote, null, notify, scheduleDate, 0, false, parent, getMessageChatSendParams(), 0, getSendMonoForumPeerId(), getSendMessageSuggestionParams()), themeDelegate);
                    } else {
                        getSendMessagesHelper().sendSticker(document, query, dialog_id, replyingMessageObject, getThreadMessage(), null, replyingQuote, sendAnimationData, true, 0, 0, false, parent, getMessageChatSendParams(), price, getSendMonoForumPeerId(), getSendMessageSuggestionParams());
                    }
                    hideFieldPanel(false);
                    chatActivityEnterView.addStickerToRecent(document);
                    chatActivityEnterView.setFieldText("");
                });
            } else if (object instanceof TLRPC.Chat) {
                TLRPC.Chat chat = (TLRPC.Chat) object;
                if (searchingForUser && searchContainer != null && searchContainer.getVisibility() == View.VISIBLE) {
                    searchUserMessages(null, chat);
                } else {
                    String username = ChatObject.getPublicUsername(chat);
                    if (username != null) {
                        chatActivityEnterView.replaceWithText(start, len, "@" + username + " ", false);
                    }
                }
            } else if (object instanceof TLRPC.User) {
                TLRPC.User user = (TLRPC.User) object;
                if (searchingForUser && searchContainer != null && searchContainer.getVisibility() == View.VISIBLE) {
                    searchUserMessages(user, null);
                } else {
                    if (UserObject.getPublicUsername(user) != null) {
                        chatActivityEnterView.replaceWithText(start, len, "@" + UserObject.getPublicUsername(user) + " ", false);
                    } else {
                        String name = UserObject.getFirstName(user, false);
                        Spannable spannable = new SpannableString(name + " ");
                        spannable.setSpan(new URLSpanUserMention("" + user.id, 3), 0, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        chatActivityEnterView.replaceWithText(start, len, spannable, false);
                    }
                }
            } else if (object instanceof MentionsAdapter.EphemeralCommand) {
                if (mentionContainer.getAdapter().isBotCommands()) {
                    if (chatMode == MODE_SCHEDULED) {
                        // nothing ??
                    } else {
                        final MentionsAdapter.EphemeralCommand ephemeralCommand = (MentionsAdapter.EphemeralCommand) object;
                        final SendMessagesHelper.SendMessageParams params = SendMessagesHelper.SendMessageParams.of(ephemeralCommand.command, dialog_id, replyingMessageObject, getThreadMessage(), null, false, null, null, null, true, 0, 0, null, false);
                        params.sendMessageChatArguments = getMessageChatSendParams();
                        params.ephemeralReceiverBotId = ephemeralCommand.botUserId;
                        params.monoForumPeer = getSendMonoForumPeerId();
                        params.suggestionParams = messageSuggestionParams;
                        getSendMessagesHelper().sendMessage(params);
                        chatActivityEnterView.setFieldText("");
                        hideFieldPanel(false);
                    }
                }
            } else if (object instanceof String) {
                if (mentionContainer.getAdapter().isBotCommands()) {
                    if (chatMode == MODE_SCHEDULED) {
                        AlertsCreator.createScheduleDatePickerDialog(getParentActivity(), dialog_id, (notify, scheduleDate, scheduleRepeatPeriod) -> {
                            getSendMessagesHelper().sendMessage(SendMessagesHelper.SendMessageParams.of((String) object, dialog_id, replyingMessageObject, getThreadMessage(), null, false, null, null, null, notify, scheduleDate, 0, null, false));
                            chatActivityEnterView.setFieldText("");
                            hideFieldPanel(false);
                        }, themeDelegate);
                    } else {
                        if (checkSlowMode(view)) {
                            return;
                        }
                        AlertsCreator.ensurePaidMessageConfirmation(currentAccount, dialog_id, 1, payStars -> {
                            final SendMessagesHelper.SendMessageParams params = SendMessagesHelper.SendMessageParams.of((String) object, dialog_id, replyingMessageObject, getThreadMessage(), null, false, null, null, null, true, 0, 0, null, false);
                            params.sendMessageChatArguments = getMessageChatSendParams();
                            params.payStars = payStars;
                            params.monoForumPeer = getSendMonoForumPeerId();
                            params.suggestionParams = messageSuggestionParams;
                            getSendMessagesHelper().sendMessage(params);
                            chatActivityEnterView.setFieldText("");
                            hideFieldPanel(false);
                        });
                    }
                } else {
                    chatActivityEnterView.replaceWithText(start, len, object + " ", false);
                }
            } else if (object instanceof TLRPC.BotInlineResult) {
                if (chatActivityEnterView.getFieldText() == null || chatMode != MODE_SCHEDULED && checkSlowMode(view)) {
                    return;
                }
                TLRPC.BotInlineResult result = (TLRPC.BotInlineResult) object;
                if (currentEncryptedChat != null) {
                    int error = 0;
                    if (result.send_message instanceof TLRPC.TL_botInlineMessageMediaAuto && "game".equals(result.type)) {
                        error = 1;
                    } else if (result.send_message instanceof TLRPC.TL_botInlineMessageMediaInvoice) {
                        error = 2;
                    }
                    if (error != 0) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), themeDelegate);
                        builder.setTitle(LocaleController.getString(R.string.SendMessageTitle));
                        if (error == 1) {
                            builder.setMessage(LocaleController.getString(R.string.GameCantSendSecretChat));
                        } else {
                            builder.setMessage(LocaleController.getString(R.string.InvoiceCantSendSecretChat));
                        }
                        builder.setNegativeButton(LocaleController.getString(R.string.OK), null);
                        showDialog(builder.create());
                        return;
                    }
                }
                if ((result.type.equals("photo") && (result.photo != null || result.content != null) ||
                        result.type.equals("gif") && (result.document != null || result.content != null) ||
                        result.type.equals("video") && (result.document != null/* || result.content_url != null*/))) {
                    ArrayList<Object> arrayList = botContextResults = new ArrayList<>(mentionContainer.getAdapter().getSearchResultBotContext());
                    PhotoViewer.getInstance().setParentActivity(ChatActivity.this, themeDelegate);
                    PhotoViewer.getInstance().openPhotoForSelect(arrayList, mentionContainer.getAdapter().getItemPosition(position), 3, false, botContextProvider, ChatActivity.this);
                } else {
                    AlertsCreator.ensurePaidMessageConfirmation(currentAccount, getDialogId(), 1, price -> {
                        if (chatMode == MODE_SCHEDULED) {
                            AlertsCreator.createScheduleDatePickerDialog(getParentActivity(), dialog_id, (notify, scheduleDate, scheduleRepeatPeriod) -> sendBotInlineResult(result, notify, scheduleDate, price), themeDelegate);
                        } else {
                            sendBotInlineResult(result, true, 0, price);
                        }
                    });
                }
            } else if (object instanceof TLRPC.TL_inlineBotWebView) {
                processInlineBotWebView((TLRPC.TL_inlineBotWebView) object);
            } else if (object instanceof TLRPC.TL_inlineBotSwitchPM) {
                processInlineBotContextPM((TLRPC.TL_inlineBotSwitchPM) object);
            } else if (object instanceof MediaDataController.KeywordResult) {
                String code = ((MediaDataController.KeywordResult) object).emoji;
                chatActivityEnterView.addEmojiToRecent(code);
                if (code != null && code.startsWith("animated_")) {
                    try {
                        Paint.FontMetricsInt fontMetrics = null;
                        try {
                            fontMetrics = chatActivityEnterView.getEditField().getPaint().getFontMetricsInt();
                        } catch (Exception e) {
                            FileLog.e(e, false);
                        }
                        long documentId = Long.parseLong(code.substring(9));
                        TLRPC.Document document = AnimatedEmojiDrawable.findDocument(currentAccount, documentId);
                        SpannableString emoji = new SpannableString(MessageObject.findAnimatedEmojiEmoticon(document));
                        AnimatedEmojiSpan span;
                        if (document != null) {
                            span = new AnimatedEmojiSpan(document, fontMetrics);
                        } else {
                            span = new AnimatedEmojiSpan(documentId, fontMetrics);
                        }
                        emoji.setSpan(span, 0, emoji.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        chatActivityEnterView.replaceWithText(start, len, emoji, false);
                    } catch (Exception ignore) {
                        chatActivityEnterView.replaceWithText(start, len, code, true);
                    }
                } else {
                    chatActivityEnterView.replaceWithText(start, len, code, true);
                }
                mentionContainer.updateVisibility(false);
            }
        });
        mentionContainer.getListView().setOnItemLongClickListener((view, position) -> {
            if (getParentActivity() == null || !mentionContainer.getAdapter().isLongClickEnabled()) {
                return false;
            }
            if (position == 0 || mentionContainer.getAdapter().isBannedInline()) {
                return false;
            }
            position--;
            Object object = mentionContainer.getAdapter().getItem(position);
            if (object instanceof MentionsAdapter.EphemeralCommand) {
                MentionsAdapter.EphemeralCommand ephemeralCommand = (MentionsAdapter.EphemeralCommand) object;
                if (mentionContainer.getAdapter().isBotCommands()) {
                    if (URLSpanBotCommand.enabled) {
                        chatActivityEnterView.setFieldText("");
                        chatActivityEnterView.setCommand(null, ephemeralCommand.command, true, currentChat != null && currentChat.megagroup);
                        return true;
                    }
                    return false;
                }
            } else if (object instanceof String) {
                if (mentionContainer.getAdapter().isBotCommands()) {
                    if (URLSpanBotCommand.enabled) {
                        chatActivityEnterView.setFieldText("");
                        chatActivityEnterView.setCommand(null, (String) object, true, currentChat != null && currentChat.megagroup);
                        return true;
                    }
                    return false;
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), themeDelegate);
                    builder.setTitle(LocaleController.getString(R.string.AppName));
                    builder.setMessage(LocaleController.getString(R.string.ClearSearch));
                    builder.setPositiveButton(LocaleController.getString(R.string.ClearButton), (dialogInterface, i) -> mentionContainer.getAdapter().clearRecentHashtags());
                    builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                    showDialog(builder.create());
                    return true;
                }
            }
            return false;
        });

        if (!isInsideContainer) {
            fragmentLocationContextViewWrapper = new FrameLayout(context);
            topPanelLayout.addView(fragmentLocationContextViewWrapper);
            topPanelLayout.setPriority(fragmentLocationContextViewWrapper, 6);
            topPanelLayout.setDebugName(fragmentLocationContextViewWrapper, "fragment location");
            topPanelLayout.setViewVisible(fragmentLocationContextViewWrapper, true, false);
            fragmentLocationContextView = new FragmentContextView(context, this, null, true, themeDelegate) {
                @Override
                public void setVisibility(int visibility) {
                    topPanelLayout.setViewVisible(fragmentLocationContextViewWrapper, visibility == VISIBLE);
                }
            };
            fragmentContextViewWrapper = new FrameLayout(context);
            topPanelLayout.addView(fragmentContextViewWrapper);
            topPanelLayout.setPriority(fragmentContextViewWrapper, 5);
            topPanelLayout.setDebugName(fragmentContextViewWrapper, "fragment context");
            topPanelLayout.setViewVisible(fragmentContextViewWrapper, true, false);
            fragmentContextView = new FragmentContextView(context, this, null, false, themeDelegate) {
                @Override
                public void setVisibility(int visibility) {
                    topPanelLayout.setViewVisible(fragmentContextViewWrapper, visibility == VISIBLE);
                }
            };
            topPanelLayout.setCallFragmentContextView(fragmentContextView);
            fragmentContextViewWrapper.addView(fragmentContextView);
            fragmentLocationContextViewWrapper.addView(fragmentLocationContextView);
            fragmentContextView.setEnabled(!inPreviewMode);
            fragmentLocationContextView.setEnabled(!inPreviewMode);

            if (chatMode != 0) {
                fragmentContextView.setSupportsCalls(false);
            }
        }

        messagesSearchListContainer = new ChatActivitySearchContainer(context);
        messagesSearchListContainer.setup(navbarContentDrawableFactory, BlurredBackgroundProviderImpl.topPanelChatActivitySearchListBg(themeDelegate));
        messagesSearchListContainer.setVisibility(View.GONE);
        messagesSearchListContainer.setBackground(navbarContentDrawableFactory
            .create(messagesSearchListContainer)
            .setColorProvider(BlurredBackgroundProviderImpl.topPanelChatActivitySearchListBg(themeDelegate)));
        contentView.addView(messagesSearchListContainer, LayoutHelper.createFrameMatchParent());

        messagesSearchListView = new RecyclerListView(context, themeDelegate) {
            @Override
            protected void onAttachedToWindow() {
                super.onAttachedToWindow();
                if (messagesSearchAdapter != null) {
                    messagesSearchAdapter.attach();
                }
            }

            @Override
            protected void onDetachedFromWindow() {
                super.onDetachedFromWindow();
                if (messagesSearchAdapter != null) {
                    messagesSearchAdapter.detach();
                }
            }
        };
        LinearLayoutManager messagesSearchLayoutManager = new LinearLayoutManager(context);
        messagesSearchLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        messagesSearchListView.setLayoutManager(messagesSearchLayoutManager);
        messagesSearchListView.setAdapter(messagesSearchAdapter = new MessagesSearchAdapter(context, this, themeDelegate, searchType, dialog_id == getUserConfig().getClientUserId()));
        messagesSearchListView.setClipToPadding(false);
        checkHashtagStories(true);
        DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setSupportsChangeAnimations(false);
        itemAnimator.setDelayAnimations(false);
        itemAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        itemAnimator.setDurations(350);
        messagesSearchListView.setItemAnimator(itemAnimator);
        messagesSearchListContainer.addView(messagesSearchListView, LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT);
        messagesSearchListView.setOnItemClickListener((view, position) -> {
            if (chatMode == MODE_SEARCH) {
                Object obj = messagesSearchAdapter.getItem(position);
                if (position == 0 && messagesSearchAdapter.containsStories && messagesSearchAdapter.storiesList != null) {
                    Bundle args = new Bundle();
                    args.putInt("type", MediaActivity.TYPE_STORIES_SEARCH);
                    args.putString("hashtag", messagesSearchAdapter.storiesList.query);
                    if (messagesSearchAdapter.storiesList.username != null) {
                        args.putString("username", messagesSearchAdapter.storiesList.username);
                    }
                    args.putInt("storiesCount", messagesSearchAdapter.storiesList.getCount());
                    presentFragment(new MediaActivity(args, null));
                } else if (obj instanceof MessageObject) {
                    openMessageInOriginalDialog((MessageObject) obj);
                }
            } else if (searchingReaction != null) {
                if (position < 0 || position >= getMediaDataController().searchResultMessages.size())
                    return;
                MessageObject msg = getMediaDataController().searchResultMessages.get(position);
                setFilterMessages(searchingFiltered = false, true, false);
                getMediaDataController().setSearchedPosition(position);
                updateSearchButtons(getMediaDataController().getMask(), getMediaDataController().getSearchPosition(), getMediaDataController().getSearchCount());
                AndroidUtilities.runOnUIThread(() -> {
                    scrollToMessageId(msg.getId(), 0, true, 0, true, 0, null, () -> {
                        progressDialog = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER, themeDelegate);
                        progressDialog.setOnShowListener(dialogInterface -> showPinnedProgress(false));
                        progressDialog.setOnCancelListener(postponedScrollCancelListener);
                        progressDialog.showDelayed(500);
                    });
                    if (waitingForLoad.isEmpty()) {
                        showMessagesSearchListView(false);
                    }
                });
            } else {
                getMediaDataController().jumpToSearchedMessage(classGuid, position);
                showMessagesSearchListView(false);
            }
        });
        messagesSearchListView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                final ChatActivity chatToUpdate = parentChatActivity != null ? parentChatActivity : ChatActivity.this;
                if (dy != 0) {
                    invalidateMergedVisibleBlurredPositionsAndSources(BLUR_INVALIDATE_FLAG_SCROLL);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && chatToUpdate.scrollableViewNoiseSuppressor != null) {
                    chatToUpdate.scrollableViewNoiseSuppressor.onScrolled(dx, dy);
                }

                if (recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_DRAGGING) {
                    AndroidUtilities.hideKeyboard(contentView);
                }
                int lastVisibleItem = messagesSearchLayoutManager.findLastVisibleItemPosition();
                int visibleItemCount = lastVisibleItem == RecyclerView.NO_POSITION ? 0 : lastVisibleItem;
                if (visibleItemCount > 0 && lastVisibleItem > messagesSearchAdapter.loadedCount - 5) {
                    if (chatMode == MODE_SEARCH) {
                        if (!loading && !endReached[0]) {
                            loading = true;
                            waitingForLoad.add(lastLoadIndex);
                            HashtagSearchController.getInstance(currentAccount).searchHashtag(searchingHashtag, classGuid, searchType, lastLoadIndex++);
                        }
                    } else {
                        getMediaDataController().loadMoreSearchMessages(true);
                    }
                }
            }
        });
        messagesSearchListView.addEdgeEffectListener(() -> invalidateMergedVisibleBlurredPositionsAndSources(BLUR_INVALIDATE_FLAG_SCROLL | BLUR_INVALIDATE_FLAG_CLIP));

        if (parentThemeDelegate == null && !isInsideContainer) {
            searchViewPager = new ViewPagerFixed(context, resourceProvider) {
                @Override
                public boolean onTouchEvent(MotionEvent ev) {
                    return false;
                }

                @Override
                protected boolean canScroll(MotionEvent e) {
                    return hashtagSearchTabs != null && hashtagSearchTabs.shown();
                }

                @Override
                public void onTabAnimationUpdate(boolean manual) {
                    super.onTabAnimationUpdate(manual);
                    contentView.invalidateBlur();
                    contentView.updateBlurContent();
                    checkUi_backgroundViewVisible();
                    invalidateMergedVisibleBlurredPositionsAndSources(BLUR_INVALIDATE_FLAG_SCROLL);
                }

                @Override
                protected void onTabScrollEnd(int position) {
                    super.onTabScrollEnd(position);
                    if (position == 0 && requestClearSearchPages) {
                        requestClearSearchPages = false;
                        searchViewPager.clearViews();
                    }
                }
            };
            searchViewPager.setAdapter(new ViewPagerFixed.Adapter() {
                @Override
                public int getItemCount() {
                    return 3;
                }

                @Override
                public int getItemViewType(int position) {
                    return position;
                }

                @Override
                public View createView(int viewType) {
                    if (viewType == SEARCH_THIS_CHAT) {
                        return new FirstViewPage(context);
                    } else {
                        Bundle args = new Bundle();
                        args.putInt("chatMode", ChatActivity.MODE_SEARCH);
                        args.putInt("searchType", viewType);
                        args.putString("searchHashtag", searchingHashtag);
                        ChatActivityContainer container = new ChatActivityContainer(context, getParentLayout(), args) {
                            boolean activityCreated = false;

                            @Override
                            protected void initChatActivity() {
                                if (!activityCreated) {
                                    activityCreated = true;
                                    super.initChatActivity();
                                }
                            }
                        };
                        container.chatActivity.navbarContentSourceWallpaper.setSource(navbarContentSourceWallpaper);
                        container.chatActivity.parentThemeDelegate = themeDelegate;
                        container.chatActivity.parentChatActivity = ChatActivity.this;
                        container.chatActivity.chatActivityDelegate = new ChatActivityDelegate() {
                            @Override
                            public void openHashtagSearch(String hashtag) {
                                ChatActivity.this.openHashtagSearch(hashtag);
                            }
                        };
                        return container;
                    }
                }

                @Override
                public void bindView(View view, int position, int viewType) {
                    if (view instanceof ChatActivityContainer) {
                        ((ChatActivityContainer) view).chatActivity.updateSearchingHashtag(searchingHashtag);
                    } else if (view instanceof PublicStoriesList) {
                        ((PublicStoriesList) view).setTabs(parentChatActivity != null ? parentChatActivity.hashtagSearchTabs.isShown() : hashtagSearchTabs.isShown());
                        ((PublicStoriesList) view).setQuery("", searchingHashtag);
                    }
                    ViewCompat.requestApplyInsets(view);
                }

                @Override
                public String getItemTitle(int position) {
                    switch (position) {
                        case SEARCH_MY_MESSAGES:
                            return LocaleController.getString(R.string.SearchMyMessages);
                        case SEARCH_PUBLIC_POSTS:
                            return LocaleController.getString(R.string.SearchPublicPosts);
                        default:
                        case SEARCH_THIS_CHAT:
                            return LocaleController.getString(R.string.SearchThisChat);
                    }
                }
            });
            searchViewPager.setAllowDisallowInterceptTouch(false);
            contentView.addView(searchViewPager, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.TOP));

            hashtagSearchTabs = new ChatSearchTabs(context) {
                @Override
                protected void onShownUpdate(boolean finish) {
                    checkUi_topFade();
                    checkUi_messagesSearchListPadding();
                    checkUi_topPanelLayoutWidth();
                    checkUi_topPanelPositions();
                    if (tagSelector != null) {
                        tagSelector.setTranslationY(contentPanTranslation + getCurrentHeight());
                    }
                    if (finish) {
                        invalidateChatListViewTopPadding = true;
                        updateChatListViewTopPadding();
                    } else {
                        invalidateChatListViewTopPadding();
                    }
                }
            };
            hashtagSearchTabs.setVisibility(View.GONE);
            hashtagSearchTabs.setTabs(searchViewPager.createTabsView(true, ViewPagerFixed.SELECTOR_TYPE_BUBBLE_STYLE));
            hashtagSearchTabs.setPadding(0, dp(7.66f), 0, dp(7.66f));
            hashtagSearchTabs.setBackground(glassBackgroundDrawableFactory.create(hashtagSearchTabs)
                .setColorProvider(BlurredBackgroundProviderImpl.topPanelChatActivity(resourceProvider))
                .setRadius(dp(18)).setPadding(dp(7f)));

            contentView.addView(hashtagSearchTabs, LayoutHelper.createFrameMarginPx(LayoutHelper.MATCH_PARENT, 50, Gravity.FILL_HORIZONTAL | Gravity.TOP, 0, -dp(5), 0, 0));
        }
        contentView.addView(topPanelLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        contentView.addView(actionBar);

        overlayView = new View(context);
        overlayView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                checkRecordLocked(true, false);
            }
            overlayView.getParent().requestDisallowInterceptTouchEvent(true);
            return true;
        });
        contentView.addView(overlayView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.TOP));
        overlayView.setVisibility(View.GONE);
        contentView.setClipChildren(false);

        instantCameraView = null;

        chatActivityEnterView = new ChatActivityEnterView(getParentActivity(), contentView, this, chatMode != MODE_EDIT_BUSINESS_LINK, themeDelegate) {

            int lastContentViewHeight;
            int messageEditTextPredrawHeigth;
            int messageEditTextPredrawScrollY;

            @Override
            protected void onChangedIslandTotalHeight(float h) {
                checkUi_inputIslandHeight();
            }

            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                if (getAlpha() != 1.0f) {
                    return false;
                }
                return super.onInterceptTouchEvent(ev);
            }

            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (getAlpha() != 1.0f) {
                    return false;
                }
                return super.onTouchEvent(event);
            }

            @Override
            public boolean dispatchTouchEvent(MotionEvent ev) {
                if (getAlpha() != 1.0f) {
                    return false;
                }
                return super.dispatchTouchEvent(ev);
            }

            @Override
            protected boolean pannelAnimationEnabled() {
                if (!openAnimationEnded) {
                    return false;
                }
                return true;
            }

            @Override
            public void openKeyboard() {
                if (forwardingPreviewView != null) {
                    return;
                }
                super.openKeyboard();
            }

            @Override
            public void setVisibility(int visibility) {
                super.setVisibility(visibility);
                bottomViewsVisibilityController.setViewVisible(MESSAGE_INPUT_CONTAINER, visibility == VISIBLE, getMeasuredWidth() > 0 && !restoringFirstViewPageVisibility);
            }

            @Override
            public void checkAnimation() {
                if (actionBar.isActionModeShowed() || isReport()) {
                    if (messageEditTextAnimator != null) {
                        messageEditTextAnimator.cancel();
                    }
                    if (changeBoundAnimator != null) {
                        changeBoundAnimator.cancel();
                    }

                    chatActivityEnterViewAnimateFromTop = 0;
                    shouldAnimateEditTextWithBounds = false;
                } else {
                    int t = getBackgroundTop();
                    if (chatActivityEnterViewAnimateFromTop != 0 && t != chatActivityEnterViewAnimateFromTop && lastContentViewHeight == contentView.getMeasuredHeight()) {
                        int dy = animatedTop + chatActivityEnterViewAnimateFromTop - t;
                        setAnimatedTop(dy);
                        messageEditTextContainer.invalidate();
                        if (changeBoundAnimator != null) {
                            changeBoundAnimator.removeAllListeners();
                            changeBoundAnimator.cancel();
                        }

                        // chatListView.setTranslationY(dy);
                        if (topView != null && topView.getVisibility() == View.VISIBLE) {
                            topView.setTranslationY(animatedTop + (1f - getTopViewEnterProgress()) * topView.getLayoutParams().height);
                        }

                        changeBoundAnimator = ValueAnimator.ofFloat(dy, 0);
                        changeBoundAnimator.addUpdateListener(a -> {
                            float top = (float) a.getAnimatedValue();
                            setAnimatedTop((int) top);
                            if (topView != null && topView.getVisibility() == View.VISIBLE) {
                                topView.setTranslationY(top + (1f - getTopViewEnterProgress()) * topView.getLayoutParams().height);
                            } else {
                                invalidateChatListViewTopPadding();
                                invalidateMessagesVisiblePart();
                            }
                            messageEditTextContainer.invalidate();
                            invalidate();
                        });
                        changeBoundAnimator.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                setAnimatedTop(0);
                                if (topView != null && topView.getVisibility() == View.VISIBLE) {
                                    topView.setTranslationY(animatedTop + (1f - getTopViewEnterProgress()) * topView.getLayoutParams().height);
                                }
                                changeBoundAnimator = null;
                            }
                        });
                        changeBoundAnimator.setDuration(ChatListItemAnimator.DEFAULT_DURATION);
                        changeBoundAnimator.setInterpolator(ChatListItemAnimator.DEFAULT_INTERPOLATOR);
                        if (!waitingForSendingMessageLoad) {
                            changeBoundAnimator.start();
                        }
                        invalidateChatListViewTopPadding();
                        invalidateMessagesVisiblePart();
                        chatActivityEnterViewAnimateFromTop = 0;
                    } else if (lastContentViewHeight != contentView.getMeasuredHeight()) {
                        chatActivityEnterViewAnimateFromTop = 0;
                    }
                    if (shouldAnimateEditTextWithBounds) {
                        float dy = (messageEditTextPredrawHeigth - messageEditText.getMeasuredHeight()) + (messageEditTextPredrawScrollY - messageEditText.getScrollY());
                        messageEditText.setOffsetY(messageEditText.getOffsetY() - dy);
                        ValueAnimator a = ValueAnimator.ofFloat(messageEditText.getOffsetY(), 0);
                        a.addUpdateListener(animation -> messageEditText.setOffsetY((float) animation.getAnimatedValue()));
                        if (messageEditTextAnimator != null) {
                            messageEditTextAnimator.cancel();
                        }
                        messageEditTextAnimator = a;
                        a.setDuration(ChatListItemAnimator.DEFAULT_DURATION);
                       // a.setStartDelay(chatActivityEnterViewAnimateBeforeSending ? 20 : 0);
                        a.setInterpolator(ChatListItemAnimator.DEFAULT_INTERPOLATOR);
                        a.start();
                        shouldAnimateEditTextWithBounds = false;
                    }
                    lastContentViewHeight = contentView.getMeasuredHeight();

                    chatActivityEnterViewAnimateBeforeSending = false;
                }
            }

            @Override
            protected void onLineCountChanged(int oldLineCount, int newLineCount) {
                if (chatActivityEnterView != null) {
                    if (chatListView != null && (searchExpandProgress > 0 || actionBar != null && actionBar.isActionModeShowed())) {
                        return;
                    }
                    shouldAnimateEditTextWithBounds = true;
                    messageEditTextPredrawHeigth = messageEditText.getMeasuredHeight();
                    messageEditTextPredrawScrollY = messageEditText.getScrollY();
                    contentView.invalidate();
                    chatActivityEnterViewAnimateFromTop = chatActivityEnterView.getBackgroundTop();
                }
            }

            @Override
            public void hideTopView(boolean animated) {
                super.hideTopView(animated);
                if (onHideFieldPanelRunnable != null) {
                    AndroidUtilities.runOnUIThread(onHideFieldPanelRunnable);
                    onHideFieldPanelRunnable = null;
                }
            }
        };
        chatActivityEnterView.setVisibility(View.VISIBLE);
        chatActivityEnterView.getEditField().adaptiveCreateLinkDialog = true;
        if (chatMode == MODE_EDIT_BUSINESS_LINK) {
            chatActivityEnterView.setDelegate(new ChatActivityEnterView.ChatActivityEnterViewDelegate() {
                @Override
                public void onMessageSend(CharSequence message, boolean notify, int scheduleDate, int scheduleRepeatPeriod, long payStars) {}

                @Override
                public void needSendTyping() {}

                @Override
                public void onTextChanged(CharSequence text, boolean bigChange, boolean fromDraft) {}
                 @Override
                public void onTextSelectionChanged(int start, int end) {}

                @Override
                public void onTextSpansChanged(CharSequence text) {}

                @Override
                public void onAttachButtonHidden() {}

                @Override
                public void onAttachButtonShow() {}

                @Override
                public void onWindowSizeChanged(int size) {}

                @Override
                public void onStickersTab(boolean opened) {}

                @Override
                public void onMessageEditEnd(boolean loading) {}

                @Override
                public void didPressAttachButton() {}

                @Override
                public void needStartRecordVideo(int state, boolean notify, int scheduleDate, int scheduleRepeatPeriod, int ttl, long effectId, long stars) {}

                @Override
                public void toggleVideoRecordingPause() {}

                @Override
                public boolean isVideoRecordingPaused() {
                    return false;
                }

                @Override
                public void needChangeVideoPreviewState(int state, float seekProgress) {}

                @Override
                public void onSwitchRecordMode(boolean video) {}

                @Override
                public void onPreAudioVideoRecord() {}

                @Override
                public void needStartRecordAudio(int state) {}

                @Override
                public void needShowMediaBanHint() {}

                @Override
                public void onStickersExpandedChange() {}

                @Override
                public void onUpdateSlowModeButton(View button, boolean show, CharSequence time) {}

                @Override
                public void onSendLongClick() {}

                @Override
                public void onAudioVideoInterfaceUpdated() {}
            });
        } else {
            chatActivityEnterView.setDelegate(new ChatActivityEnterViewDelegate());
        }
        if (chatMode == MODE_SCHEDULED || isComments) {
            chatActivityEnterView.setSideButtonsForAttach(sideControlsButtonsLayout);
        }
        chatActivityEnterView.setInAppInsetsController(windowInsetsStateHolder);
        chatActivityEnterView.setDialogId(dialog_id, currentAccount);
        if (chatInfo != null) {
            chatActivityEnterView.setChatInfo(chatInfo);
        }
        chatActivityEnterView.setId(id_chat_compose_panel);
        chatActivityEnterView.setBotsCount(botsCount, hasBotsCommands, hasQuickReplies, false);
        chatActivityEnterView.updateBotWebView(false);
        chatActivityEnterView.setMinimumHeight(AndroidUtilities.dp(51));
        chatActivityEnterView.setAllowStickersAndGifs(true, true, currentEncryptedChat == null || AndroidUtilities.getPeerLayerVersion(currentEncryptedChat.layer) >= 46);
        chatActivityEnterView.shouldDrawBackground = false;
        if (textToSet != null) {
            chatActivityEnterView.setFieldText(textToSet);
            textToSet = null;
        }
        if (inPreviewMode || isInsideContainer) {
            chatActivityEnterView.setVisibility(View.INVISIBLE);
        }
        if (!ChatObject.isChannel(currentChat) || currentChat.megagroup) {
            chatActivityEnterView.setBotInfo(botInfo, false);
        }
        // contentView.addView(chatActivityEnterView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM));

        chatActivityEnterView.setViewParentForEmoji(chatInputInAppContainer);
        checkSendButtonBlockedByTyping(false);

        chatInputBubbleContainer.addView(chatActivityEnterView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM, 7, 0, 7, 0));

        int chatListIndex = contentView.indexOfChild(chatListView);
        chatListIndex = chatListIndex < 0 ? contentView.getChildCount() : (chatListIndex + 1);

        roundVideoRecordBackground = new View(context);
        roundVideoRecordBackground.setVisibility(View.GONE);
        BlurredBackgroundDrawable d = navbarContentDrawableFactory.create(roundVideoRecordBackground);
        d.setAlpha(232);
        roundVideoRecordBackground.setBackground(d);

        contentView.addView(roundVideoRecordBackground, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        contentView.addView(chatInputViewsContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        if (chatMode != MODE_EDIT_BUSINESS_LINK) {
            chatActivityEnterView.checkChannelRights();
        }

        actionsButtonsLayout = new ChatActivityActionsButtonsLayout(context, resourceProvider, blurredBackgroundColorProvider, glassBackgroundDrawableFactory);
        actionsButtonsLayout.setForwardButtonOnClickListener(v -> openForward(false));
        actionsButtonsLayout.setReplyButtonOnClickListener(v -> {
            MessageObject messageObject = null;
            for (int a = 1; a >= 0; a--) {
                if (messageObject == null && selectedMessagesIds[a].size() != 0) {
                    messageObject = messagesDict[a].get(selectedMessagesIds[a].keyAt(0));
                }
                selectedMessagesIds[a].clear();
                selectedMessagesCanCopyIds[a].clear();
                selectedMessagesCanStarIds[a].clear();
            }
            hideActionMode();
            if (messageObject != null && (messageObject.messageOwner.id > 0 || messageObject.messageOwner.id < 0 && currentEncryptedChat != null)) {
                showFieldPanelForReply(messageObject);
            }
            updatePinnedMessageView(true);
            updateVisibleRows();
            updateSelectedMessageReactions();
        });
        bottomViewsVisibilityController.setViewVisible(MESSAGE_ACTION_CONTAINER, false, false);
        actionsButtonsLayout.setPadding(0, dp(56), 0, 0);
        actionsButtonsLayout.setClipToPadding(false);
        chatInputBubbleContainer.addView(actionsButtonsLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 106, Gravity.BOTTOM));
        chatActivityEnterView.setSuggestionButtonVisible(ChatObject.isMonoForum(currentChat), false);

        chatActivityEnterTopView = new ChatActivityEnterTopView(context) {
            @Override
            public void setTranslationY(float translationY) {
                super.setTranslationY(translationY);
                if (chatActivityEnterView != null) {
                    chatActivityEnterView.invalidate();
                }
                if (getVisibility() != GONE) {
                    hideHints(true);
                    if (progressView != null) {
                        progressView.setTranslationY(translationY);
                    }
                    invalidateChatListViewTopPadding();
                    invalidateMessagesVisiblePart();
                    if (fragmentView != null) {
                        fragmentView.invalidate();
                    }
                }
            }

            @Override
            public boolean hasOverlappingRendering() {
                return false;
            }

            @Override
            public void setVisibility(int visibility) {
                super.setVisibility(visibility);
                if (visibility == GONE) {
                    if (progressView != null) {
                        progressView.setTranslationY(0);
                    }
                }
            }
        };
        chatActivityEnterView.addTopView(chatActivityEnterTopView, 48);

        if (chatMode == MODE_EDIT_BUSINESS_LINK) {
            chatActivityEnterView.setEditingBusinessLink(businessLink);
        }

        replyLayout = new ChatReplyContainer(context, themeDelegate);
        chatActivityEnterTopView.addReplyView(replyLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.NO_GRAVITY, 0, 0, 52, 0));

        boolean[] byButtonPress = new boolean[1];
        replyLayout.setOnClickListener(v -> {
            if (replyingMessageObject != null && replyingMessageObject.isEphemeral()) {
                return;
            }

            final boolean isButtonPress = byButtonPress[0];
            if (isButtonPress) {
                byButtonPress[0] = false;
            }

            if (fieldPanelShown == 5) {
                new MessageSuggestionOfferSheet(context, currentAccount, dialog_id, messageSuggestionParams != null ? messageSuggestionParams: MessageSuggestionParams.empty(), this, getResourceProvider(), MessageSuggestionOfferSheet.MODE_INPUT, this::showFieldPanelForSuggestionParams).show();
            } else if (fieldPanelShown == 1 && editingMessageObject != null) {
                if (editingMessageObject.needResendWhenEdit() && !isButtonPress) {
                    showSuggestionOfferForEditMessage(messageSuggestionParams != null ? messageSuggestionParams: MessageSuggestionParams.empty());
                } else if (editingMessageObject.canEditMedia() && editingMessageObjectReqId == 0) {
                    Utilities.Callback<Integer> open = type -> {
                        if (chatAttachAlert == null) {
                            createChatAttachView();
                        }
                        chatAttachAlert.setEditingMessageObject(type, editingMessageObject);
                        openAttachMenu();
                    };
                    open.run(ChatAttachAlert.EDITMEDIA_TYPE_ANY);
                } else {
                    scrollToMessageId(editingMessageObject.getId(), 0, true, 0, true, 0);
                }
            } else if (messagePreviewParams != null) {
                forbidForwardingWithDismiss = false;
                if (fieldPanelShown == 2) {
                    if (DialogObject.isEncryptedDialog(dialog_id) || messagePreviewParams.hasSecretMessages || chatMode == MODE_QUICK_REPLIES) {
                        if (replyingMessageObject != null) {
                            scrollToMessageId(replyingMessageObject.getId(), 0, true, 0, true, 0);
                        }
                    } else {
                        forbidForwardingWithDismiss = messagePreviewParams.quote == null;
                        SharedConfig.replyingOptionsHintHintShowed();
                        openForwardingPreview(MessagePreviewView.TAB_REPLY);
                    }
                } else if (fieldPanelShown == 3) {
                    SharedConfig.forwardingOptionsHintHintShowed();
                    openForwardingPreview(MessagePreviewView.TAB_FORWARD);
                } else if (fieldPanelShown == 4) {
                    openForwardingPreview(MessagePreviewView.TAB_LINK);
                }
            }
        });
        replyLayout.setOnLongClickListener(v -> {
            if (fieldPanelShown == 1 && editingMessageObject != null) {
                scrollToMessageId(editingMessageObject.getId(), 0, true, 0, true, 0);
                return true;
            } else if (messagePreviewParams != null) {
                if (fieldPanelShown == 2) {
                    if (replyingMessageObject != null) {
                        scrollToMessageId(replyingMessageObject.getId(), 0, true, 0, true, 0);
                        return true;
                    }
                }
            }
            return false;
        });

        replyCloseImageView = new ImageView(context);
        replyCloseImageView.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_glass_defaultIcon), PorterDuff.Mode.MULTIPLY));
        replyCloseImageView.setImageResource(R.drawable.input_clear);
        replyCloseImageView.setScaleType(ImageView.ScaleType.CENTER);
        replyCloseImageView.setBackgroundDrawable(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 1, AndroidUtilities.dp(19)));
        chatActivityEnterTopView.addView(replyCloseImageView, LayoutHelper.createFrame(52, 46, Gravity.RIGHT | Gravity.TOP, 0, 0.5f, 0, 0));
        replyCloseImageView.setOnClickListener(v -> {
            messageSuggestionParams = null;
            if (fieldPanelShown == 2) {
                replyingQuote = null;
                replyingMessageObject = null;
                if (messagePreviewParams != null) {
                    messagePreviewParams.updateReply(null, null, dialog_id, null);
                }
                fallbackFieldPanel();
            } else if (fieldPanelShown == 3) {
                openAnotherForward();
            } else if (fieldPanelShown == 4) {
                foundWebPage = null;
                if (messagePreviewParams != null) {
                    messagePreviewParams.updateLink(currentAccount, null, null, replyingMessageObject == threadMessageObject ? null : replyingMessageObject, replyingQuote, editingMessageObject);
                }
                chatActivityEnterView.setWebPage(null, false);
                editResetMediaManual();
                fallbackFieldPanel();
            } else {
                if (ChatObject.isForum(currentChat) && !isTopic && replyingMessageObject != null) {
                    long topicId = MessageObject.getTopicId(currentAccount, replyingMessageObject.messageOwner, true);
                    if (topicId != 0) {
                        getMediaDataController().cleanDraft(dialog_id, topicId, false);
                    }
                }
                showFieldPanel(false, null, null, null, null, true, 0, null, true, 0, true);
            }
        });

        contentView.addView(
            suggestEmojiPanel = new SuggestEmojiView(context, currentAccount, chatActivityEnterView, themeDelegate),
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 160, Gravity.LEFT | Gravity.BOTTOM, 7, 0, 7, 0)
        );
        suggestEmojiPanel.setVisibility(allowStickersPanel && !isInPreviewMode() && (chatActivityEnterView == null || !chatActivityEnterView.isStickersExpanded()) ? View.VISIBLE : View.GONE);

        final ChatActivityEnterTopView.EditView editView = new ChatActivityEnterTopView.EditView(context);
        editView.setMotionEventSplittingEnabled(false);
        editView.setOrientation(LinearLayout.HORIZONTAL);
        editView.setOnClickListener(v -> {
            if (editingMessageObject != null) {
                scrollToMessageId(editingMessageObject.getId(), 0, true, 0, true, 0);
            }
        });
        chatActivityEnterTopView.addEditView(editView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.NO_GRAVITY, 0, 0, 48, 0));

        for (int i = 0; i < 2; i++) {
            final boolean firstButton = i == 0;

            final ChatActivityEnterTopView.EditViewButton button = new ChatActivityEnterTopView.EditViewButton(context) {
                @Override
                public void setEditButton(boolean editButton) {
                    super.setEditButton(editButton);
                    if (firstButton) {
                        getTextView().setMaxWidth(editButton ? AndroidUtilities.dp(116) : Integer.MAX_VALUE);
                    }
                }

                @Override
                public void updateColors() {
                    final int leftInset = firstButton ? dp(4) : 0;
                    setBackground(Theme.createInsetRoundRectDrawable(getThemedColor(Theme.key_chat_replyPanelName) & 0x19ffffff, dp(19), leftInset, dp(3), 0, dp(3)));
                    getImageView().setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_chat_replyPanelName), PorterDuff.Mode.MULTIPLY));
                    getTextView().setTextColor(getThemedColor(Theme.key_chat_replyPanelName));
                }
            };
            button.setOrientation(LinearLayout.HORIZONTAL);
            ViewHelper.setPadding(button, 12, 0, 12, 0);
            editView.addButton(button, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT));

            final ImageView imageView = new ImageView(context);
            imageView.setScaleType(ImageView.ScaleType.CENTER);
            imageView.setImageResource(firstButton ? R.drawable.msg_photoeditor : R.drawable.msg_replace);
            button.addImageView(imageView, LayoutHelper.createLinear(24, LayoutHelper.MATCH_PARENT));

            button.addSpaceView(new Space(context), LayoutHelper.createLinear(10, LayoutHelper.MATCH_PARENT));

            final TextView textView = new TextView(context);
            textView.setMaxLines(1);
            textView.setSingleLine(true);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            textView.setTypeface(AndroidUtilities.bold());
            textView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            textView.setEllipsize(TextUtils.TruncateAt.END);
            button.addTextView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT));

            button.updateColors();
            button.setOnClickListener(v -> {
                if (editingMessageObject == null || !editingMessageObject.canEditMedia() || editingMessageObjectReqId != 0) {
                    return;
                }
                if (button.isEditButton()) {
                    openEditingMessageInPhotoEditor();
                } else {
                    byButtonPress[0] = true;
                    replyLayout.callOnClick();
                }
            });
        }
        searchContainer = null;

        bottomOverlay = new FrameLayout(context) {
            @Override
            public void setVisibility(int visibility) {
                super.setVisibility(visibility);
                bottomViewsVisibilityController.setViewVisible(BOTTOM_OVERLAY_TEXT_CONTAINER, visibility == VISIBLE, getMeasuredWidth() > 0);
            }
        };
        bottomOverlay.setVisibility(View.INVISIBLE);
        bottomOverlay.setFocusable(true);
        bottomOverlay.setFocusableInTouchMode(true);
        bottomOverlay.setClickable(true);
        chatInputBubbleContainer.addView(bottomOverlay, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 44, Gravity.BOTTOM, 7, 0, 7, 0));

        bottomOverlayText = new TextView(context);
        bottomOverlayText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        bottomOverlayText.setGravity(Gravity.CENTER);
        bottomOverlayText.setMaxLines(2);
        bottomOverlayText.setEllipsize(TextUtils.TruncateAt.END);
        bottomOverlayText.setLineSpacing(AndroidUtilities.dp(2), 1);
        bottomOverlayText.setTextColor(getThemedColor(Theme.key_chat_secretChatStatusText));
        bottomOverlayText.setPadding(dp(24), 0, dp(24), 0);
        bottomOverlay.addView(bottomOverlayText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
        
        
        bottomChannelButtonsLayout = new ChatActivityChannelButtonsLayout(context, resourceProvider, blurredBackgroundColorProvider, glassBackgroundDrawableFactory) {
            @Override
            public void setVisibility(int visibility) {
                super.setVisibility(visibility);
                bottomViewsVisibilityController.setViewVisible(BOTTOM_OVERLAY_CHAT_CONTAINER, visibility == VISIBLE, getMeasuredWidth() > 0);
            }
        };
        bottomChannelButtonsLayout.setVisibility(View.INVISIBLE);
        bottomChannelButtonsLayout.setClipChildren(false);
        bottomChannelButtonsLayout.setAccentColor(getThemedColor(Theme.key_featuredStickers_addButton));
        bottomChannelButtonsLayout.setButtonOnClickListener(ChatActivityChannelButtonsLayout.BUTTON_SEARCH, v -> {
            openSearchWithText(isSupportedTags() ? "" : null);
        });
        bottomChannelButtonsLayout.setButtonOnClickListener(ChatActivityChannelButtonsLayout.BUTTON_GIGA_GROUP_INFO, v -> {
            createUndoView();
            undoView.showWithAction(dialog_id, UndoView.ACTION_TEXT_INFO, LocaleController.getString(R.string.BroadcastGroupInfo));
        });
        bottomChannelButtonsLayout.setButtonOnClickListener(ChatActivityChannelButtonsLayout.BUTTON_GIFT, v -> {
            HintsController.Hint.ChannelGiftHint.doNotShowAgain();
            showDialog(new GiftSheet(getContext(), currentAccount, getDialogId(), null, null));
        });
        bottomChannelButtonsLayout.setButtonOnClickListener(ChatActivityChannelButtonsLayout.BUTTON_DIRECT, v -> {
            HintsController.Hint.ChannelSuggestHint.doNotShowAgain();
            if (currentChat != null && currentChat.linked_monoforum_id != 0) {
                getMessagesController().putMonoForumLinkedChat(currentChat.id, currentChat.linked_monoforum_id);
                Bundle bundle = new Bundle();
                bundle.putLong("chat_id", currentChat.linked_monoforum_id);
                bundle.putInt("chatMode", MODE_SUGGESTIONS);
                bundle.putBoolean("isSubscriberSuggestions", true);
                presentFragment(new ChatActivity(bundle));
            }
        });
        bottomChannelButtonsLayout.setButtonOnFullyVisibleListener(ChatActivityChannelButtonsLayout.BUTTON_GIFT, (v, id, firstTime) -> {
            if (bottomGiftHintView == null && firstTime && (bottomSuggestHintView == null || !bottomSuggestHintView.shown()) && HintsController.Hint.ChannelGiftHint.show()) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (getContext() == null) return;
                    final float offset = windowInsetsStateHolder.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom / AndroidUtilities.density;
                    final float translate = (contentView.getWidth() - (v.getX() + v.getWidth()) + v.getWidth() / 2f) / AndroidUtilities.density;

                    bottomGiftHintView = new HintView2(getContext(), HintView2.DIRECTION_BOTTOM);
                    bottomGiftHintView.setPadding(dp(7.33f), 0, dp(7.33f), 0);
                    bottomGiftHintView.setMultilineText(false);
                    bottomGiftHintView.setText(getString(R.string.Gift2ChannelSendHint));
                    bottomGiftHintView.setJoint(1, -translate + 7.33f);
                    contentView.addView(bottomGiftHintView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 100, Gravity.BOTTOM | Gravity.FILL_HORIZONTAL, 0, 0, 0, offset + 50));
                    bottomGiftHintView.setOnHiddenListener(() -> AndroidUtilities.removeFromParent(bottomGiftHintView));
                    bottomGiftHintView.show();
                    HintsController.Hint.ChannelGiftHint.increment();
                }, 400);
            }
        });
        bottomChannelButtonsLayout.setButtonOnFullyVisibleListener(ChatActivityChannelButtonsLayout.BUTTON_DIRECT, (v, id, firstTime) -> {
            if (bottomSuggestHintView == null && firstTime && HintsController.Hint.ChannelSuggestHint.show()) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (getContext() == null) return;
                    final float offset = windowInsetsStateHolder.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom / AndroidUtilities.density;
                    final float translate = (contentView.getWidth() - (v.getX() + v.getWidth()) + v.getWidth() / 2f) / AndroidUtilities.density;

                    bottomSuggestHintView = new HintView2(getContext(), HintView2.DIRECTION_BOTTOM);
                    bottomSuggestHintView.setPadding(dp(7.33f), 0, dp(7.33f), 0);
                    bottomSuggestHintView.setMultilineText(false);
                    bottomSuggestHintView.setText(getString(R.string.Suggest2ChannelSendHint), true);
                    bottomSuggestHintView.setJoint(1, -translate + 7.33f);
                    contentView.addView(bottomSuggestHintView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 100, Gravity.BOTTOM | Gravity.FILL_HORIZONTAL, 0, 0, 0, offset + 50));
                    bottomSuggestHintView.setOnHiddenListener(() -> AndroidUtilities.removeFromParent(bottomSuggestHintView));
                    bottomSuggestHintView.show();
                    HintsController.Hint.ChannelSuggestHint.increment();
                }, 400);
            }
        });
        bottomChannelButtonsLayout.setOnButtonsTotalWidthChanged((l, r) -> {
            chatInputViewsContainer.setInputBubbleOffsets(l, r);
        });

        chatInputBubbleContainer.addView(bottomChannelButtonsLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 56, Gravity.BOTTOM, 0, 0, 0, (44 - 56) / 2));

        bottomOverlayStartButton = new TextView(context) {
            CellFlickerDrawable cellFlickerDrawable;

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                if (cellFlickerDrawable == null) {
                    cellFlickerDrawable = new CellFlickerDrawable();
                    cellFlickerDrawable.drawFrame = false;
                    cellFlickerDrawable.repeatProgress = 2f;
                }
                cellFlickerDrawable.setParentWidth(getMeasuredWidth());
                AndroidUtilities.rectTmp.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
                cellFlickerDrawable.draw(canvas, AndroidUtilities.rectTmp, AndroidUtilities.dp(22), null);
                invalidate();
            }
        };
        bottomOverlayStartButton.setBackground(Theme.AdaptiveRipple.filledRect(getThemedColor(Theme.key_featuredStickers_addButton), 22));
        bottomOverlayStartButton.setTextColor(getThemedColor(Theme.key_featuredStickers_buttonText));
        bottomOverlayStartButton.setText(LocaleController.getString(R.string.BotStart2));
        bottomOverlayStartButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        bottomOverlayStartButton.setGravity(Gravity.CENTER);
        bottomOverlayStartButton.setTypeface(AndroidUtilities.bold());
        bottomOverlayStartButton.setVisibility(View.INVISIBLE);
        bottomOverlayStartButton.setOnClickListener(v -> bottomOverlayChatText.callOnClick());
        bottomOverlayStartButton.setPadding(dp(31), 0, dp(31), 0);
        ScaleStateListAnimator.apply(bottomOverlayStartButton, 0.02f, 1.2f);
        bottomChannelButtonsLayout.getContainer().addView(bottomOverlayStartButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 38, Gravity.CENTER, 3, 3, 3, 3));
        bottomChannelButtonsLayout.makeViewWrapContent(bottomOverlayStartButton);

        if (currentUser != null && currentUser.bot && currentUser.id != UserObject.VERIFY && !UserObject.isDeleted(currentUser) && !UserObject.isReplyUser(currentUser) && !isInScheduleMode() && chatMode != MODE_PINNED && chatMode != MODE_SAVED && !isReport()) {
            bottomOverlayStartButton.setVisibility(View.VISIBLE);
            bottomChannelButtonsLayout.setVisibility(View.VISIBLE);
        }

        bottomOverlayLinksText = new LinkSpanDrawable.LinksTextView(context, themeDelegate);
        bottomOverlayLinksText.setVisibility(View.GONE);
        bottomOverlayLinksText.setGravity(Gravity.CENTER);
        bottomOverlayLinksText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        bottomOverlayLinksText.setTextColor(getThemedColor(Theme.key_graySectionText));
        bottomOverlayLinksText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        bottomChannelButtonsLayout.getContainer().addView(bottomOverlayLinksText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
        bottomOverlayLinksText.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
        bottomOverlayLinksText.setOnClickListener(v -> {
            if (chatMode == MODE_DEFAULT && getMessagesController().freezeUntilDate > getConnectionsManager().getCurrentTime() && !AccountFrozenAlert.isSpamBot(currentAccount, currentUser)) {
                AccountFrozenAlert.show(getContext(), currentAccount, getResourceProvider());
            }
        });

        bottomOverlayChatText = new UnreadCounterTextView(context) {
            @Override
            protected void updateCounter() {
                if (ChatObject.isChannel(currentChat) && !currentChat.megagroup && chatInfo != null && chatInfo.linked_chat_id != 0) {
                    TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(-chatInfo.linked_chat_id);
                    if (dialog != null) {
                        setCounter(dialog.unread_count);
                        return;
                    }
                }
                setCounter(0);
            }

            @Override
            protected Theme.ResourcesProvider getResourceProvider() {
                return themeDelegate;
            }
        };
        bottomChannelButtonsLayout.getContainer().addView(bottomOverlayChatText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, 0));
        bottomOverlayChatText.setOnClickListener(view -> {
            if (getParentActivity() == null || pullingDownOffset != 0) {
                return;
            }
            if (chatMode == MODE_SAVED) {
                Bundle args = new Bundle();
                long dialogId = getSavedDialogId();
                if (dialogId >= 0) {
                    args.putLong("user_id", dialogId);
                } else {
                    args.putLong("chat_id", -dialogId);
                }
                presentFragment(new ChatActivity(args));
            } else if (isReport()) {
                ArrayList<Integer> ids = new ArrayList<>();
                for (int b = 0; b < selectedMessagesIds[0].size(); b++) {
                    ids.add(selectedMessagesIds[0].keyAt(b));
                }
                showBottomOverlayProgress(true, true);
                ReportBottomSheet.continueReport(this, reportOption, reportMessage, ids, status -> {
                    showBottomOverlayProgress(true, false);
                    if (status) {
                        finishFragment();
                    }
                });
            } else if (chatMode == MODE_PINNED) {
                finishFragment();
                chatActivityDelegate.onUnpin(true, bottomOverlayChatText.getTag() == null);
            } else if (currentUser != null && currentUser.id == UserObject.VERIFY) {
                toggleMute(true);
            } else if (currentUser != null && userBlocked) {
                if (currentUser.bot) {
                    String botUserLast = botUser;
                    botUser = null;
                    getMessagesController().unblockPeer(currentUser.id, () -> {
                        if (botUserLast != null && botUserLast.length() != 0) {
                            getMessagesController().sendBotStart(currentUser, botUserLast);
                        } else {
                            getSendMessagesHelper().sendMessage(SendMessagesHelper.SendMessageParams.of("/start", dialog_id, null, null, null, false, null, null, null, true, 0, 0, null, false));
                        }
                    });
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), themeDelegate);
                    builder.setMessage(LocaleController.getString(R.string.AreYouSureUnblockContact));
                    builder.setPositiveButton(LocaleController.getString(R.string.OK), (dialogInterface, i) -> getMessagesController().unblockPeer(currentUser.id));
                    builder.setTitle(LocaleController.getString(R.string.AppName));
                    builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                    showDialog(builder.create());
                }
            } else if (UserObject.isReplyUser(currentUser)) {
                toggleMute(true);
            } else if (currentUser != null && currentUser.bot && botUser != null) {
                if (botUser.length() != 0) {
                    getMessagesController().sendBotStart(currentUser, botUser);
                } else {
                    getSendMessagesHelper().sendMessage(SendMessagesHelper.SendMessageParams.of("/start", dialog_id, null, null, null, false, null, null, null, true, 0, 0, null, false));
                }
                botUser = null;
                updateBottomOverlay();
            } else {
                if (ChatObject.isChannel(currentChat) && !(currentChat instanceof TLRPC.TL_channelForbidden)) {
                    if (ChatObject.isNotInChat(currentChat)) {
                        if (currentChat.join_request) {
//                            showDialog(new JoinGroupAlert(context, currentChat, null, this));
                            showBottomOverlayProgress(true, true);
                            MessagesController.getInstance(currentAccount).addUserToChat(
                                currentChat.id,
                                UserConfig.getInstance(currentAccount).getCurrentUser(),
                                0,
                                null,
                                null,
                                true,
                                () -> {
                                    showBottomOverlayProgress(false, true);
                                },
                                err -> {
                                    SharedPreferences preferences = MessagesController.getNotificationsSettings(currentAccount);
                                    preferences.edit().putLong("dialog_join_requested_time_" + dialog_id, System.currentTimeMillis()).commit();
                                    if (err != null && "INVITE_REQUEST_SENT".equals(err.text)) {
                                        JoinGroupAlert.showBulletin(context, this, ChatObject.isChannel(currentChat) && !currentChat.megagroup);
                                    }
                                    showBottomOverlayProgress(false, true);
                                    return false;
                                }
                            );
                        } else {
                            if (chatInviteRunnable != null) {
                                AndroidUtilities.cancelRunOnUIThread(chatInviteRunnable);
                                chatInviteRunnable = null;
                            }
                            showBottomOverlayProgress(true, true);
                            getMessagesController().addUserToChat(currentChat.id, getUserConfig().getCurrentUser(), 0, null, ChatActivity.this, null);
                            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.closeSearchByActiveAction);

                            if (hasReportSpam() && reportSpamButton.getTag(R.id.object_tag) != null) {
                                SharedPreferences preferences = MessagesController.getNotificationsSettings(currentAccount);
                                preferences.edit().putInt("dialog_bar_vis3" + dialog_id, 3).commit();
                                getNotificationCenter().postNotificationName(NotificationCenter.peerSettingsDidLoad, dialog_id);
                            }
                        }
                    } else {
                        toggleMute(true);
                    }
                } else {
                    boolean canDeleteHistory = chatInfo != null && chatInfo.can_delete_channel;
                    AlertsCreator.createClearOrDeleteDialogAlert(ChatActivity.this, false, currentChat, currentUser, currentEncryptedChat != null, true, false, canDeleteHistory, (param) -> {
                        getNotificationCenter().removeObserver(ChatActivity.this, NotificationCenter.closeChats);
                        getNotificationCenter().postNotificationName(NotificationCenter.closeChats);
                        finishFragment();
                        getNotificationCenter().postNotificationName(NotificationCenter.needDeleteDialog, dialog_id, currentUser, currentChat, param);
                    });
                }
            }
        });

        bottomOverlayProgress = new RadialProgressView(context, themeDelegate);
        bottomOverlayProgress.setSize(AndroidUtilities.dp(22));
        bottomOverlayProgress.setProgressColor(getThemedColor(Theme.key_featuredStickers_buttonText));
        bottomOverlayProgress.setVisibility(View.INVISIBLE);
        bottomOverlayProgress.setScaleX(0.1f);
        bottomOverlayProgress.setScaleY(0.1f);
        bottomOverlayProgress.setAlpha(1.0f);
        bottomChannelButtonsLayout.getContainer().addView(bottomOverlayProgress, LayoutHelper.createFrame(30, 30, Gravity.CENTER));

        contentView.addView(messageEnterTransitionContainer = new MessageEnterTransitionContainer(contentView, currentAccount));

        if (currentChat != null) {
            slowModeHint = new HintView(getParentActivity(), 2, themeDelegate);
            slowModeHint.setAlpha(0.0f);
            slowModeHint.setVisibility(View.INVISIBLE);
            contentView.addView(slowModeHint, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 19, 0, 19, 0));
        }

        chatAdapter.updateRowsSafe();

        if (loading && messages.isEmpty()) {
            showProgressView(chatAdapter.botInfoRow < 0);
            chatListView.setEmptyView(null);
        } else {
            showProgressView(false);
            createEmptyView(false);
            chatListView.setEmptyView(emptyViewContainer);
        }

        checkBotKeyboard();
        updateBottomOverlay();
        updateSecretStatus();
        updateTopPanel(false);
        updatePinnedMessageView(false);
        updateInfoTopView(false);

        chatScrollHelper = new RecyclerAnimationScrollHelper(chatListView, chatLayoutManager) {
            @Override
            public void setScrollDirection(int scrollDirection) {
                if (reversed) {
                    if (scrollDirection == RecyclerAnimationScrollHelper.SCROLL_DIRECTION_DOWN) {
                        scrollDirection = RecyclerAnimationScrollHelper.SCROLL_DIRECTION_UP;
                    } else if (scrollDirection == RecyclerAnimationScrollHelper.SCROLL_DIRECTION_UP) {
                        scrollDirection = RecyclerAnimationScrollHelper.SCROLL_DIRECTION_DOWN;
                    }
                }
                super.setScrollDirection(scrollDirection);
            }
        };
        chatScrollHelper.setScrollListener(() -> {
            invalidateMergedVisibleBlurredPositionsAndSources(BLUR_INVALIDATE_FLAG_SCROLL);
            this.invalidateMessagesVisiblePart();
        });
        chatScrollHelper.setAnimationCallback(chatScrollHelperCallback);

        flagSecure = new FlagSecureReason(getParentActivity().getWindow(), () ->
            currentEncryptedChat != null ||
            isPeerNoForwards()
        );

        if (oldMessage != null) {
            chatActivityEnterView.setFieldText(oldMessage);
        }

        fixLayoutInternal();

        textSelectionHelper.setCallback(new TextSelectionHelper.Callback() {
            @Override
            public void onStateChanged(boolean isSelected) {
                swipeBackEnabled = !isSelected;
                if (isSelected) {
                    if (slidingView != null) {
                        slidingViewSetOffset(0);
                        slidingView = null;
                    }
                    maybeStartTrackingSlidingView = false;
                    startedTrackingSlidingView = false;
                    if (textSelectionHint != null) {
                        textSelectionHint.hide();
                    }
                }
                updatePagedownButtonVisibility(true);
            }

            @Override
            public void onTextCopied() {
                if (actionBar != null && actionBar.isActionModeShowed()) {
                    clearSelectionMode();
                }
                createUndoView();
                undoView.showWithAction(0, UndoView.ACTION_TEXT_COPIED, null);
            }
        });

        View overlay = textSelectionHelper.getOverlayView(context);
        if (overlay != null) {
            if (overlay.getParent() instanceof ViewGroup) {
                ((ViewGroup) overlay.getParent()).removeView(overlay);
            }
            contentView.addView(overlay);
        }
        textSelectionHelper.setParentView(chatListView);

        if (!TextUtils.isEmpty(searchingHashtag)) {
            createSearchHashtagViewsIfNeeded();
        }

        contentView.addView(fireworksOverlay = new FireworksOverlay(context), LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        if (getDialogId() < 0 && chatMode == MODE_DEFAULT && !isInsideContainer && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            messageMetricsView = new ChatActivityMessageMetricsView(context);
            messageMetricsView.init(currentAccount, getDialogId(), contentView, chatListView);
            messageMetricsView.setIsUserActive();
            contentView.addView(messageMetricsView);
        }

        checkInstantSearch();
        if (replyingMessageObject != null) {
            chatActivityEnterView.setReplyingMessageObject(replyingMessageObject, replyingQuote);
        }

        ViewGroup decorView = (ViewGroup) getParentActivity().getWindow().getDecorView();
        pinchToZoomHelper = new PinchToZoomHelper(decorView, contentView) {
            @Override
            protected void drawOverlays(Canvas canvas, float alpha, float parentOffsetX, float parentOffsetY, float clipTop, float clipBottom) {
                if (alpha > 0) {
                    View view = getChild();
                    if (view instanceof ChatMessageCell) {
                        ChatMessageCell cell = (ChatMessageCell) view;

                        int top = (int) Math.max(clipTop, parentOffsetY);
                        int bottom = (int) Math.min(clipBottom, parentOffsetY + cell.getMeasuredHeight());
                        AndroidUtilities.rectTmp.set(parentOffsetX, top, parentOffsetX + cell.getMeasuredWidth(), bottom);
                        canvas.saveLayerAlpha(AndroidUtilities.rectTmp, (int) (255 * alpha), Canvas.ALL_SAVE_FLAG);
                        canvas.translate(parentOffsetX, parentOffsetY + cell.getPaddingTop());
                        cell.drawFromPinchToZoom = true;
                        cell.drawOverlays(canvas);
                        if (cell.shouldDrawTimeOnMedia() && cell.getCurrentMessagesGroup() == null) {
                            cell.drawTime(canvas, 1f, false);
                        }
                        cell.drawFromPinchToZoom = false;
                        canvas.restore();
                    }
                }
            }
        };
        pinchToZoomHelper.setCallback(new PinchToZoomHelper.Callback() {

            @Override
            public TextureView getCurrentTextureView() {
                return videoTextureView;
            }

            @Override
            public void onZoomStarted(MessageObject messageObject) {
                chatListView.cancelClickRunnables(true);
                chatListView.stopScroll();
                if (MediaController.getInstance().isPlayingMessage(messageObject)) {
                    contentView.removeView(videoPlayerContainer);
                    videoPlayerContainer = null;
                    videoTextureView = null;
                    aspectRatioFrameLayout = null;
                }

                for (int i = 0; i < chatListView.getChildCount(); i++) {
                    if (chatListView.getChildAt(i) instanceof ChatMessageCell) {
                        ChatMessageCell cell = (ChatMessageCell) chatListView.getChildAt(i);
                        if (cell.getMessageObject() != null && cell.getMessageObject().getId() == messageObject.getId()) {
                            cell.getPhotoImage().setVisible(false, true);
                        }
                    }
                }
            }

            @Override
            public void onZoomFinished(MessageObject messageObject) {
                if (messageObject == null) {
                    return;
                }
                if (MediaController.getInstance().isPlayingMessage(messageObject)) {
                    for (int i = 0; i < chatListView.getChildCount(); i++) {
                        if (chatListView.getChildAt(i) instanceof ChatMessageCell) {
                            ChatMessageCell cell = (ChatMessageCell) chatListView.getChildAt(i);
                            if (cell.getMessageObject() != null && cell.getMessageObject().getId() == messageObject.getId()) {
                                AnimatedFileDrawable animation = cell.getPhotoImage().getAnimation();
                                if (animation.isRunning()) {
                                    animation.stop();
                                }
                                if (animation != null) {
                                    Bitmap bitmap = animation.getAnimatedBitmap();
                                    if (bitmap != null) {
                                        try {
                                            Bitmap src = pinchToZoomHelper.getVideoBitmap(bitmap.getWidth(), bitmap.getHeight());
                                            Canvas canvas = new Canvas(bitmap);
                                            canvas.drawBitmap(src, 0, 0, null);
                                            src.recycle();
                                        } catch (Throwable e) {
                                            FileLog.e(e);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    createTextureView(true);
                    MediaController.getInstance().setTextureView(videoTextureView, aspectRatioFrameLayout, videoPlayerContainer, true);
                }
                chatListView.invalidate();
            }

        });
        pinchToZoomHelper.setClipBoundsListener(topBottom -> {
            topBottom[1] = chatListView.getBottom() - blurredViewBottomOffset;
            topBottom[0] = chatListView.getTop() + chatListViewPaddingTop - AndroidUtilities.dp(4);
        });
        emojiAnimationsOverlay = new EmojiAnimationsOverlay(ChatActivity.this, contentView, chatListView, currentAccount, dialog_id, threadMessageId) {
            @Override
            public void onAllEffectsEnd() {
                updateMessagesVisiblePart(false);
            }
        };

        if (isTopic) {
            reactionsMentionCount = forumTopic.unread_reactions_count;
            pollVotesMentionCount = forumTopic.unread_poll_votes_count;
            updateReactionsMentionButton(false);
            updatePollVotesMentionButton(false);
        } else {
            TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(dialog_id);
            if (dialog != null) {
                reactionsMentionCount = dialog.unread_reactions_count;
                pollVotesMentionCount = dialog.unread_poll_votes_count;
                updateReactionsMentionButton(false);
                updatePollVotesMentionButton(false);
            }
        }

        if (getDialogId() == getUserConfig().getClientUserId() && chatMode != MODE_SAVED) {
            savedMessagesHint = new HintView2(context, HintView2.DIRECTION_TOP);
            savedMessagesHint.setMultilineText(true);
            savedMessagesHint.setTextAlign(Layout.Alignment.ALIGN_CENTER);
            savedMessagesHint.setText(AndroidUtilities.replaceTags(LocaleController.getString(R.string.SavedMessagesHint)));
            savedMessagesHint.setMaxWidthPx(HintView2.cutInFancyHalf(savedMessagesHint.getText(), savedMessagesHint.getTextPaint()));
            if (AndroidUtilities.isTablet()) {
                savedMessagesHint.setJoint(0, 77);
            } else {
                savedMessagesHint.setJoint(0.5f, 0);
            }
            savedMessagesHint.setCloseButton(true);
			savedMessagesHint.setOnHiddenListener(() -> {
                if (searchContainer == null || searchContainer.getVisibility() != View.VISIBLE) {
                    if (savedMessagesSearchHint != null) {
                        savedMessagesSearchHint.show();
                    }
                }
			});
			savedMessagesHint.setDuration(-1);
            savedMessagesHint.setPadding(dp(8), 0, dp(8), 0);
            contentView.addView(savedMessagesHint, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 120, Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, -8, 0, 0));

            savedMessagesSearchHint = new HintView2(context, HintView2.DIRECTION_TOP)
                    .setMultilineText(true)
                    .setTextAlign(Layout.Alignment.ALIGN_CENTER)
                    .setDuration(-1)
                    .setHideByTouch(true)
                    .useScale(true)
                    .setCloseButton(true)
                    .setJointPx(1, -dp(56))
                    .setRounding(8);
			savedMessagesSearchHint.setText(LocaleController.getString(R.string.SavedTagSearchTooltipHint));
			contentView.addView(savedMessagesSearchHint, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 120, Gravity.TOP | Gravity.FILL_HORIZONTAL, 16, -8, 16, 0));

            if (getUserConfig().isPremium()) {
                savedMessagesTagHint = new HintView2(context, HintView2.DIRECTION_BOTTOM)
                        .setMultilineText(true)
                        .setTextAlign(Layout.Alignment.ALIGN_CENTER)
                        .setDuration(-1)
                        .setHideByTouch(true)
                        .useScale(true)
                        .setCloseButton(true)
                        .setRounding(8);
                savedMessagesTagHint.setText(LocaleController.getString(R.string.SavedTagLongpressHint));
                contentView.addView(savedMessagesTagHint, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 120, Gravity.TOP | Gravity.FILL_HORIZONTAL, 16, 0, 16, 0));
            }
        }

        if (getDialogId() == getUserConfig().getClientUserId()) {
            actionBarSearchTags = new SearchTagsList(context, ChatActivity.this, currentAccount, getSavedDialogId(), themeDelegate) {
                @Override
                protected boolean setFilter(ReactionsLayoutInBubble.VisibleReaction reaction) {
                    searchingReaction = reaction;
                    searchingFiltered = reaction != null;
                    if (reaction == null) {
                        getMediaDataController().clearFoundMessageObjects();
                        setFilterMessages(false);
                        updateSearchButtons(0, 0, -1);
                    }
                    updateSearchUpDownButtonVisibility(true);
                    updatePagedownButtonVisibility(true);
                    searchingQuery = searchItem.getSearchField().getText().toString();
                    getMediaDataController().searchMessagesInChat(searchingQuery, dialog_id, mergeDialogId, classGuid, 0, threadMessageId, false, searchingUserMessages, searchingChatMessages, !TextUtils.isEmpty(searchingQuery) || searchingReaction != null, searchingReaction);
                    AndroidUtilities.hideKeyboard(searchItem.getSearchField());
                    return true;
                }

                @Override
                public void updateTags(boolean notify) {
                    super.updateTags(notify);
                    show(searchItem != null && searchItem.isSearchFieldVisible() && hasFilters() && searchingHashtag == null);
                }

                @Override
                protected void onShownUpdate(boolean finish) {
                    checkUi_topFade();
                    checkUi_messagesSearchListPadding();
                    if (tagSelector != null) {
                        tagSelector.setTranslationY(contentPanTranslation + getCurrentHeight());
                    }
                    if (finish) {
                        invalidateChatListViewTopPadding = true;
                        updateChatListViewTopPadding();
                    }
                }
            };
            actionBarSearchTags.setVisibility(View.GONE);
            actionBarSearchTags.setBlurredFactory(
                glassBackgroundDrawableFactory,
                BlurredBackgroundProviderImpl.topPanelChatActivityTags(resourceProvider)
            );
            contentView.addView(actionBarSearchTags, LayoutHelper.createFrameMarginPx(LayoutHelper.MATCH_PARENT, 38, Gravity.FILL_HORIZONTAL | Gravity.TOP, 0, -dp(3), 0, 0));
        }

        checkUi_topPanelLayoutWidth();
        topPanelLayout.setBlurredBackground(glassBackgroundDrawableFactory.create(topPanelLayout)
            .setColorProvider(BlurredBackgroundProviderImpl.topPanelChatActivity(themeDelegate))
            .setRadius(dp(18))
            .setPadding(dp(7)));

        if (chatMode == MODE_SEARCH) {
            animatorSearchResultAsListVisibility.setValue(true, false);
            searchExpandList.setText(LocaleController.getString(R.string.SearchAsChat), false);
            updateSearchListEmptyView();
        }

        final boolean useTabsView = (!isSubscriberSuggestions && ChatObject.isMonoForum(currentChat)
            || ChatObject.isForum(currentChat) && ChatObject.areTabsEnabled(currentChat)
            || UserObject.isBotForum(currentUser)) && chatMode != MODE_PINNED;

        if (useTabsView) {
            createTopicsTabs();
        }

        if (context instanceof LaunchActivity) {
            windowInsetsStateHolder.setupAnimatedInsetsProvider(((LaunchActivity) context).getRootAnimatedInsetsListener(), fragmentView);
        }

        onBottomItemsVisibilityChanged();
        ViewCompat.setOnApplyWindowInsetsListener(fragmentView, this::onApplyWindowInsets);
        Timer.finish(t);

        return fragmentView;
    }

    private void createSearchHashtagViewsIfNeeded() {
        if (hashtagSearchEmptyView != null || hashtagHistoryView != null) {
            return;
        }

        hashtagLoadingView = new FlickerLoadingView(getContext(), themeDelegate);
        hashtagLoadingView.setViewType(FlickerLoadingView.DIALOG_CELL_TYPE);

        hashtagSearchEmptyView = new StickerEmptyView(getContext(), hashtagLoadingView, StickerEmptyView.STICKER_TYPE_SEARCH);
        hashtagSearchEmptyView.setClickable(true);
        hashtagSearchEmptyView.title.setText(LocaleController.getString(R.string.NoResult));
        hashtagSearchEmptyView.setVisibility(View.GONE);
        hashtagSearchEmptyView.addView(hashtagLoadingView, 0);
        hashtagSearchEmptyView.showProgress(true, false);
        messagesSearchListContainer.addView(hashtagSearchEmptyView, new FrameLayout.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));

        hashtagHistoryView = new HashtagHistoryView(getContext(), resourceProvider, currentAccount);
        hashtagHistoryView.setOnHashtagClickListener(this::openHashtagSearch);
        hashtagHistoryView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    AndroidUtilities.hideKeyboard(contentView);
                }
            }
        });
        hashtagHistoryView.setVisibility(View.GONE);
        messagesSearchListContainer.addView(hashtagHistoryView, LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT);

        checkUi_messagesSearchListPadding();
        checkUi_hashtagSearchHistoryVisibility();
    }

    private boolean lastImeVisible;
    private int insetSystemLeft;
    private int insetSystemRight;

    @NonNull
    private WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
        final Insets systemInsets = AndroidUtilities.getDefaultWindowInsets(insets, false);

        final int insetsLeft = systemInsets.left;
        final int insetsRight = systemInsets.right;
        if (insetSystemLeft != insetsLeft || insetSystemRight != insetsRight) {
            insetSystemLeft = insetsLeft;
            insetSystemRight = insetsRight;
            contentView.requestLayout();
        }

        windowInsetsStateHolder.setInsets(insets);
        
        if (messagesSearchListContainer != null) {
            messagesSearchListContainer.setPadding(insetsLeft, 0, insetsRight, 0);
        }
        
        checkUi_chatListViewPaddings();
        checkUi_messagesSearchListPadding();
        invalidateClipRectForBackgroundAndChatList();

        final boolean keyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
        if (lastImeVisible != keyboardVisible) {
            lastImeVisible = keyboardVisible;
            contentView.notifyHeightChanged();
        }

        if (searchViewPager != null) {
            ViewCompat.dispatchApplyWindowInsets(searchViewPager, insets);
        }

        return WindowInsetsCompat.CONSUMED;
    }

    private boolean lastInAppInputVisible;
    private void checkInsets() {
        chatInputViewsContainer.checkInsets();
        updatePagedownButtonsPosition();
        updateBotforumTabsBottomMargin();
        checkUi_botMenuPosition();
        checkUi_BlurHeight();
        checkUi_emptyContainerPosition();
        checkUi_chatListViewPaddings();
        checkUi_messagesSearchListPadding();
        invalidateClipRectForBackgroundAndChatList();

        final boolean inAppInputVisible = windowInsetsStateHolder.inAppViewIsVisible();
        if (lastInAppInputVisible != inAppInputVisible) {
            lastInAppInputVisible = inAppInputVisible;
            checkSystemBarColors();
        }
    }

    private void checkBotMessageHint() {
        if (botMessageHint != null) {
            return;
        }
        ChatMessageCell cell = null;
        for (int i = chatListView.getChildCount() - 1; i >= 0; --i) {
            View child = chatListView.getChildAt(i);
            if (child instanceof ChatMessageCell) {
                ChatMessageCell messageCell = ((ChatMessageCell) child);
                MessageObject msg = messageCell.getPrimaryMessageObject();
                if (msg != null && msg.messageOwner != null && msg.messageOwner.via_business_bot_id != 0) {
                    cell = messageCell;
                }
            }
        }
        showBotMessageHint(cell, false);
    }

    private boolean bizbothint;
    private void showBotMessageHint(ChatMessageCell cell, boolean byClick) {
        if (
            getContext() == null || cell == null || cell.timeLayout == null ||
            cell.getPrimaryMessageObject() == null || cell.getPrimaryMessageObject().messageOwner == null ||
            cell.getPrimaryMessageObject().messageOwner.via_business_bot_id == 0) {
            return;
        }
        if (!byClick) {
            if (getMessagesController().getMainSettings().getBoolean("bizbothint", false)) {
                return;
            }
            getMessagesController().getMainSettings().edit().putBoolean("bizbothint", true).apply();
            if (bizbothint) return;
            bizbothint = true;
        }
        if (botMessageHint != null) {
            if (byClick) {
                HintView2 hint = botMessageHint;
                hint.setOnHiddenListener(() -> contentView.removeView(hint));
                hint.hide();
                botMessageHint = null;
            } else {
                return;
            }
        }
        botMessageHint = new HintView2(getContext(), HintView2.DIRECTION_BOTTOM)
            .setMultilineText(true)
            .setTextAlign(Layout.Alignment.ALIGN_NORMAL)
            .setDuration(-1)
            .setHideByTouch(true)
            .useScale(true)
            .setCloseButton(true)
            .setRounding(8);
        botMessageHint.setText(AndroidUtilities.replaceTags(LocaleController.getString(R.string.MessageBizBot)));
        botMessageHint.setMaxWidthPx(HintView2.cutInFancyHalf(botMessageHint.getText(), botMessageHint.getTextPaint()));
        contentView.addView(botMessageHint, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 120, Gravity.TOP | Gravity.FILL_HORIZONTAL, 16, 0, 16, 0));
        contentView.post(() -> {
            int[] loc = new int[2];
            cell.getLocationInWindow(loc);
            botMessageHint.setTranslationY(loc[1] - botMessageHint.getTop() - dp(120) + cell.getTimeY());
            botMessageHint.setJointPx(0, -dp(16) + loc[0] + cell.timeX + cell.timeWidth - cell.signWidth / 2f);
            botMessageHint.show();
        });
    }

    private void hideHints() {
        if (savedMessagesTagHint != null && savedMessagesTagHint.shown()) {
            savedMessagesTagHint.hide();
        }
        if (videoConversionTimeHint != null && videoConversionTimeHint.shown()) {
            videoConversionTimeHint.hide();
        }
        if (chatActivityEnterView != null) {
            chatActivityEnterView.hideHints();
        }
    }

    public void setTagFilter(ReactionsLayoutInBubble.VisibleReaction reaction) {
        if (actionBarSearchTags != null) {
            actionBarSearchTags.setChosen(reaction, true);
        }
        searchItemVisible = searching = !TextUtils.isEmpty(searchingQuery) || searchingReaction != null;
        updateBottomOverlay();
        updateSearchUpDownButtonVisibility(true);
    }

    public void setSearchQuery(String text) {
        if (searchItem != null) {
            searchItem.setSearchFieldText(searchingQuery = text, false);
        }
    }

    public void hitSearch() {
        searchWas = true;
        updateSearchButtons(0, 0, -1);
        getMediaDataController().searchMessagesInChat(searchingQuery, dialog_id, mergeDialogId, classGuid, 0, threadMessageId, searchingUserMessages, searchingChatMessages, searchingReaction);
        searchItemVisible = searching = !TextUtils.isEmpty(searchingQuery) || searchingReaction != null;
        updateBottomOverlay();
        updateSearchUpDownButtonVisibility(true);
    }

    public void clearSearch() {
        if (searchItemListener != null) {
            searchItemListener.onSearchCollapse();
        }
        searching = false;
        searchItemVisible = false;
        updateBottomOverlay();
        updateSearchUpDownButtonVisibility(true);
    }

    protected void onSearchLoadingUpdate(boolean loading) {

    }

    private void setFilterMessages(boolean filter) {
        setFilterMessages(filter, false, true);
    }
    private void setFilterMessages(boolean filter, boolean ignoreMessageNotFound, boolean animated) {
        if (chatAdapter == null || chatAdapter.isFiltered == filter) return;
        chatAdapter.isFiltered = filter;
        createEmptyView(true);
        if (filter) {
            updateFilteredMessages(false);
        }
        if (!saveScrollOnFilterToggle(animated, ignoreMessageNotFound)) {
            chatAdapter.isFiltered = !filter;
        }
        if (searchOtherButton != null) {
            searchOtherButton.setText(LocaleController.getString(chatAdapter.isFiltered ? R.string.SavedTagShowOtherMessages : R.string.SavedTagHideOtherMessages));
        }
        updateSearchUpDownButtonVisibility(true);
        showProgressView(!chatAdapter.isFiltered ? loading && messages.isEmpty() && chatAdapter.botInfoRow < 0 : getMediaDataController().isSearchLoading() && chatAdapter.filteredMessages.isEmpty());
        if (chatListView != null) {
            createEmptyView(false);
            if (!(!chatAdapter.isFiltered ? !loading && messages.isEmpty() && chatAdapter.botInfoRow < 0 : !getMediaDataController().isSearchLoading() && chatAdapter.filteredMessages.isEmpty())) {
                emptyViewContainer.setVisibility(View.GONE);
                chatListView.setEmptyView(null);
            } else {
                chatListView.setEmptyView(emptyViewContainer);
                chatListView.checkIfEmpty();
            }
        }
    }

    private boolean saveScrollOnFilterToggle(boolean animated, boolean ignoreMessageNotFound) {
        final ArrayList<MessageObject> newMessagesArray = chatAdapter.getMessages();

        int index = -1;
        int centerId = 0;
        int centerStableId = 0;
        int offset = 0;
        int cy = chatListView.getMeasuredHeight() / 2;
        ArrayList<View> views = new ArrayList<>();
        HashMap<View, Integer> distances = new HashMap<>();
        for (int i = 0; i < chatListView.getChildCount(); ++i) {
            View child = chatListView.getChildAt(i);
//            int ccy = (child.getTop() + child.getBottom()) / 2;
//            views.put(Math.abs(ccy - cy), child); // base on center
            int dist = (int) (chatListView.getMeasuredHeight() * .97f) - dp(42) - child.getBottom();
            if (dist < 0) continue;
            distances.put(child, dist);
            views.add(child);
        }
        Collections.sort(views, Comparator.comparingInt(distances::get));
        HashSet<Integer> ids = new HashSet<Integer>();
        HashSet<Integer> stableIds = new HashSet<Integer>();
        for (int i = 0; i < newMessagesArray.size(); ++i) {
            MessageObject m = newMessagesArray.get(i);
            if (!m.isDateObject) {
                ids.add(m.getId());
            }
            stableIds.add(m.stableId);
        }
        for (int i = 0; i < views.size(); ++i) {
            View child = views.get(i);
            MessageObject msg;
            if (child instanceof ChatMessageCell) {
                msg = ((ChatMessageCell) child).getMessageObject();
            } else if (child instanceof ChatActionCell) {
                msg = ((ChatActionCell) child).getMessageObject();
            } else {
                continue;
            }
            if (msg == null) continue;
            int id = msg.getId();
            if (ids.contains(id)) {
                centerId = id;
                offset = getScrollingOffsetForView(child);
                break;
            } else if (stableIds.contains(msg.stableId)) {
                centerStableId = msg.stableId;
                offset = getScrollingOffsetForView(child);
                break;
            }
        }
        if (centerId == 0 && centerStableId == 0) {
            if (!chatAdapter.isFiltered && !ignoreMessageNotFound) {
                for (int j = 0; j < views.size(); ++j) {
                    View centerView = views.get(j);
                    MessageObject msg;
                    if (centerView instanceof ChatMessageCell) {
                        msg = ((ChatMessageCell) centerView).getMessageObject();
                    } else {
                        continue;
                    }
                    if (msg == null) {
                        continue;
                    }
                    int id = msg.getId();

                    waitingForLoad.clear();
                    removeSelectedMessageHighlight();
                    scrollToMessagePosition = -10000;
                    startLoadFromMessageId = id;
                    showScrollToMessageError = false;
                    createUnreadMessageAfterIdLoading = false;
                    postponedScrollIsCanceled = false;
                    waitingForLoad.add(lastLoadIndex);
                    postponedScrollToLastMessageQueryIndex = lastLoadIndex;
                    fakePostponedScroll = true;
                    postponedScrollMinMessageId = minMessageId[0];
                    postponedScrollMessageId = id;
                    getMessagesController().loadMessages(dialog_id, 0, false, 50, startLoadFromMessageId, 0, true, 0, classGuid, 3, 0, chatMode, threadMessageId, replyMaxReadId, lastLoadIndex++, isTopic);
                    return false;
                }
            }
            for (int j = 0; j < views.size(); ++j) {
                View centerView = views.get(j);
                MessageObject msg;
                if (centerView instanceof ChatMessageCell) {
                    msg = ((ChatMessageCell) centerView).getMessageObject();
                } else {
                    continue;
                }
                if (msg == null) {
                    continue;
                }
                int id = msg.getId();
                int mid = -1;
                int leastDist = Integer.MAX_VALUE;
                for (int i = 0; i < newMessagesArray.size(); ++i) {
                    MessageObject msg2 = newMessagesArray.get(i);
                    if (msg2.getId() == 0) continue;
                    final int dist = Math.abs(msg2.getId() - id);
                    if (dist < leastDist) {
                        leastDist = dist;
                        mid = msg2.getId();

                        index = i;
                        offset = 0;
                    }
                }
                if (mid != -1) {
                    break;
                }
            }
        }
        if (centerStableId != 0) {
            for (int i = 0; i < newMessagesArray.size(); ++i) {
                if (newMessagesArray.get(i).stableId == centerStableId) {
                    index = i;
                    break;
                }
            }
        } else if (centerId != 0) {
            for (int i = 0; i < newMessagesArray.size(); ++i) {
                if (newMessagesArray.get(i).getId() == centerId) {
                    index = i;
                    break;
                }
            }
        }
        chatAdapter.updateRowsSafe();
        if (index >= 0) {
            int newPosition = chatAdapter.messagesStartRow + index;
            chatLayoutManager.scrollToPositionWithOffset(newPosition, offset);
        }
        chatAdapter.notifyDataSetChanged(animated);
        return true;
    }

    private void checkSendButtonBlockedByTyping(boolean animated) {
        if (chatActivityEnterView != null) {
            chatActivityEnterView.setBlockedByStreaming(BotForumHelper.getInstance(currentAccount)
                .getStreamingSendButtonState(dialog_id, (int) getTopicId()), animated);
        }
    }

    private LongSparseArray<ArrayList<MessageObject>> filteredMessagesByDays;
    private LongSparseArray<MessageObject> filteredMessagesDict;

    private void putFilteredDate(int index, MessageObject baseMsg) {
        TLRPC.Message dateMsg = new TLRPC.TL_message();
        dateMsg.message = LocaleController.formatDateChat(baseMsg.messageOwner.date);
        dateMsg.id = 0;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(((long) baseMsg.messageOwner.date) * 1000);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        dateMsg.date = (int) (calendar.getTimeInMillis() / 1000);
        MessageObject dateObj = new MessageObject(currentAccount, dateMsg, false, false);
        dateObj.type = 10;
        dateObj.contentType = 1;
        dateObj.isDateObject = true;
        dateObj.stableId = getStableIdForDateObject(baseMsg.dateKeyInt);
        chatAdapter.filteredMessages.add(index, dateObj);
    }

    private void createHint2MessageObject() {
        if (hint2MessageObject != null) return;
        TLRPC.Message dateMsg = new TLRPC.TL_message();
        if (chatMode == MODE_SAVED) {
            dateMsg.message = LocaleController.getString(R.string.SavedMessagesProfileHint);
        } else if (chatMode == MODE_WELCOME_MESSAGES) {
            dateMsg.message = LocaleController.getString(R.string.WelcomeMessageHint2);
        } else {
            dateMsg.message = LocaleController.getString(R.string.BusinessRepliesHint);
        }
        dateMsg.id = 0;
        hint2MessageObject = new MessageObject(currentAccount, dateMsg, false, false);
        hint2MessageObject.type = 10;
        hint2MessageObject.contentType = 1;
    }

    private void createHint3MessageObject() {
        if (hint3MessageObject != null) return;
        TLRPC.Message dateMsg = new TLRPC.TL_message();
        dateMsg.message = LocaleController.getString(R.string.WelcomeMessageHint);
        dateMsg.id = 0;
        hint3MessageObject = new MessageObject(currentAccount, dateMsg, false, false);
        hint3MessageObject.type = 10;
        hint3MessageObject.contentType = 1;
    }

    private void updateFilteredMessages(boolean notify) {
        ArrayList<MessageObject> results = new ArrayList<>(MediaDataController.getInstance(currentAccount).getFoundMessageObjects());
        if (filteredMessagesDict == null) {
            filteredMessagesDict = new LongSparseArray<>();
        }
        if (filteredMessagesByDays == null) {
            filteredMessagesByDays = new LongSparseArray<>();
        } else {
            filteredMessagesByDays.clear();
        }
        LongSparseArray<MessageObject.GroupedMessages> newGroups = null;
        LongSparseArray<MessageObject.GroupedMessages> changedGroups = null;
        chatAdapter.filteredMessages.clear();
        filteredMessagesDict.clear();
        for (int i = 0; i < results.size(); ++i) {
            MessageObject msg = results.get(i);
            MessageObject from = null;
            if (from == null) {
                for (int j = 0; j < messages.size(); ++j) {
                    MessageObject m = messages.get(j);
                    if (m.getDialogId() == msg.getDialogId() && m.getId() == msg.getId()) {
                        from = m;
                        break;
                    }
                }
            }
            if (msg.stableId == 0) {
                if (from == null) {
                    msg.checkMediaExistance();
                } else {
                    msg.mediaExists = from.mediaExists;
                    msg.attachPathExists = from.attachPathExists;
                }
            }
            if (from != null) {
                msg.isSaved = from.isSaved;
                if (chatAdapter.isFiltered && msg.stableId != 0) {
                    from.copyStableParams(msg);
                } else {
                    msg.copyStableParams(from);
                }
            } else if (msg.stableId == 0) {
                msg.stableId = lastStableId++;
            }
            msg.isOutOwnerCached = null;
            if (msg.messageOwner != null) {
                msg.messageOwner.out = true;
            }
            msg.isOutOwner();

            if (msg.hasValidGroupId()) {
                MessageObject.GroupedMessages groupedMessages = groupedMessagesMap.get(msg.getGroupIdForUse());
                if (groupedMessages == null) {
                    groupedMessages = new MessageObject.GroupedMessages();
                    groupedMessages.reversed = reversed;
                    groupedMessages.groupId = msg.getGroupId();
                    groupedMessagesMap.put(groupedMessages.groupId, groupedMessages);
                } else if (newGroups == null || newGroups.indexOfKey(msg.getGroupId()) < 0) {
                    if (changedGroups == null) {
                        changedGroups = new LongSparseArray<>();
                    }
                    changedGroups.put(msg.getGroupId(), groupedMessages);
                }
                if (newGroups == null) {
                    newGroups = new LongSparseArray<>();
                }
                newGroups.put(groupedMessages.groupId, groupedMessages);
                if (groupedMessages.getPosition(msg) == null) {
                    boolean found = false;
                    for (int j = 0; j < groupedMessages.messages.size(); ++j) {
                        if (groupedMessages.messages.get(j).getId() == msg.getId()) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        groupedMessages.messages.add(msg);
                    }
                }
            } else if (msg.getGroupIdForUse() != 0) {
                msg.messageOwner.grouped_id = 0;
                msg.localSentGroupId = 0;
            }

            chatAdapter.filteredMessages.add(msg);
            filteredMessagesDict.put(msg.getId(), msg);
        }
        if (newGroups != null) {
            for (int i = 0; i < newGroups.size(); ++i) {
                MessageObject.GroupedMessages group = newGroups.valueAt(i);
                Collections.sort(group.messages, (a, b) -> a.getId() - b.getId());
                group.calculate();
            }
        }
        ArrayList<MessageObject> messagesResults = new ArrayList<>();
        if (searchingReaction != null && TextUtils.isEmpty(searchingQuery)) {
            for (int i = 0; i < messages.size(); ++i) {
                MessageObject msg = messages.get(i);
                if (msg != null && msg.messageOwner != null && msg.messageOwner.reactions != null && msg.messageOwner.reactions.reactions_as_tags) {
                    for (int j = 0; j < msg.messageOwner.reactions.results.size(); ++j) {
                        if (searchingReaction.isSame(msg.messageOwner.reactions.results.get(j).reaction)) {
                            messagesResults.add(msg);
                            break;
                        }
                    }
                }
            }
        }
        for (int i = 0; i < messagesResults.size(); ++i) {
            MessageObject msg = messagesResults.get(i);
            if (filteredMessagesDict.containsKey(msg.getId())) {
                continue;
            }
            msg.isOutOwnerCached = null;
            if (msg.messageOwner != null) {
                msg.messageOwner.out = true;
            }
            chatAdapter.filteredMessages.add(msg);
            filteredMessagesDict.put(msg.getId(), msg);
        }
        for (int i = 0; i < chatAdapter.filteredMessages.size(); ++i) {
            MessageObject obj = chatAdapter.filteredMessages.get(i);
            if (!obj.hasValidGroupId()) {
                continue;
            }
            MessageObject.GroupedMessages group = groupedMessagesMap.get(obj.getGroupId());
            if (group != null) {
                for (int j = group.messages.size() - 1; j >= 0; --j) {
                    MessageObject groupmsg = group.messages.get(j);
                    if (groupmsg == obj || filteredMessagesDict.containsKey(groupmsg.getId()))
                        continue;
                    chatAdapter.filteredMessages.add(i, groupmsg);
                    filteredMessagesDict.put(groupmsg.getId(), groupmsg);
                    i++;
                }
            } else {

            }
        }
        Collections.sort(chatAdapter.filteredMessages, (a, b) -> b.getId() - a.getId());
        MessageObject lastFilteredMessage = null;
        for (int i = 0; i < chatAdapter.filteredMessages.size(); ++i) {
            MessageObject msg = chatAdapter.filteredMessages.get(i);
            if (reversed && msg != null && i == 0) {
                putFilteredDate(i++, msg);
            }
            if (!reversed && lastFilteredMessage != null && msg.dateKeyInt != lastFilteredMessage.dateKeyInt) {
                putFilteredDate(i++, lastFilteredMessage);
            }
            ArrayList<MessageObject> dayArray = filteredMessagesByDays.get(msg.dateKeyInt);
            if (dayArray == null) {
                filteredMessagesByDays.put(msg.dateKeyInt, dayArray = new ArrayList<>());
            }
            dayArray.add(msg);
            if (reversed && lastFilteredMessage != null && msg.dateKeyInt != lastFilteredMessage.dateKeyInt) {
                putFilteredDate(i++, msg);
            }
            lastFilteredMessage = msg;
            if (!reversed && lastFilteredMessage != null && i >= chatAdapter.filteredMessages.size() - 1) {
                putFilteredDate(chatAdapter.filteredMessages.size(), lastFilteredMessage);
                i++;
            }
        }
        chatAdapter.filteredEndReached = MediaDataController.getInstance(currentAccount).searchEndReached();
        if (notify) {
            chatAdapter.updateRowsSafe();
            chatAdapter.notifyDataSetChanged(true);
            showProgressView(!chatAdapter.isFiltered ? loading && messages.isEmpty() && chatAdapter.botInfoRow < 0 : getMediaDataController().isSearchLoading() && chatAdapter.filteredMessages.isEmpty());
            if (chatListView != null) {
                createEmptyView(false);
                if (!(!chatAdapter.isFiltered ? !loading && messages.isEmpty() && chatAdapter.botInfoRow < 0 : !getMediaDataController().isSearchLoading() && chatAdapter.filteredMessages.isEmpty())) {
                    emptyViewContainer.setVisibility(View.GONE);
                    chatListView.setEmptyView(null);
                } else {
                    chatListView.setEmptyView(emptyViewContainer);
                    chatListView.checkIfEmpty();
                }
            }
        }
    }

    private void createBottomMessagesActionButtons() {

    }

    private void checkInstantSearch() {
        final long searchFromUserId = getArguments().getInt("search_from_user_id", 0);
        if (searchFromUserId != 0) {
            TLRPC.User user = getMessagesController().getUser(searchFromUserId);
            if (user != null) {
                openSearchWithText("");
                if (searchUserButton != null) {
                    searchUserButton.callOnClick();
                }
                searchUserMessages(user, null);
            }
        } else {
            final long searchFromChatId = getArguments().getInt("search_from_chat_id", 0);
            if (searchFromChatId != 0) {
                TLRPC.Chat chat = getMessagesController().getChat(searchFromChatId);
                if (chat != null) {
                    openSearchWithText("");
                    if (searchUserButton != null) {
                        searchUserButton.callOnClick();
                    }
                    searchUserMessages(null, chat);
                }
            }
        }
    }

    private void createTopPanel() {
        if (contentView == null || topChatPanelView != null || getContext() == null) {
            return;
        }

        topChatPanelView = new FrameLayout(getContext()) {

            private boolean ignoreLayout;

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int width = MeasureSpec.getSize(widthMeasureSpec);
                int leftMargin = 0;
                if (isSideMenued()) {
                    width -= dp(SIDE_MENU_WIDTH);
                    leftMargin += dp(32);
                }
                if (addToContactsButton != null && addToContactsButton.getVisibility() == VISIBLE && reportSpamButton != null && reportSpamButton.getVisibility() == VISIBLE) {
                    width = (width - dp(31)) / 2;
                }
                ignoreLayout = true;
                if (reportSpamButton != null && reportSpamButton.getVisibility() == VISIBLE) {
                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) reportSpamButton.getLayoutParams();
                    layoutParams.width = width;
                    if (addToContactsButton != null && addToContactsButton.getVisibility() == VISIBLE) {
                        reportSpamButton.setPadding(dp(4), 0, dp(4), 0);
                        layoutParams.leftMargin = leftMargin + width;
                        layoutParams.width -= dp(15);
                    } else {
                        reportSpamButton.setPadding(dp(48), 0, dp(48), 0);
                        layoutParams.leftMargin = leftMargin;
                    }
                }
                if (addToContactsButton != null && addToContactsButton.getVisibility() == VISIBLE) {
                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) addToContactsButton.getLayoutParams();
                    layoutParams.width = width;
                    if (reportSpamButton != null && reportSpamButton.getVisibility() == VISIBLE) {
                        addToContactsButton.setPadding(dp(11), 0, dp(4), 0);
                    } else {
                        addToContactsButton.setPadding(dp(48), 0, dp(48), 0);
                    }
                    layoutParams.leftMargin = leftMargin;
                }
                ignoreLayout = false;
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }


            @Override
            public void requestLayout() {
                if (ignoreLayout) {
                    return;
                }
                super.requestLayout();
            }
        };
        invalidateChatListViewTopPadding();
        topChatPanelView.setClickable(true);
        topPanelLayout.addView(topChatPanelView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 44));
        topPanelLayout.setPriority(topChatPanelView, 2);
        topPanelLayout.setDebugName(topChatPanelView, "top chat panel view");

        reportSpamButton = new TextView(getContext());
        reportSpamButton.setTextColor(getThemedColor(Theme.key_text_RedBold));
        reportSpamButton.setBackground(Theme.createInsetRoundRectDrawable(getThemedColor(Theme.key_text_RedBold) & 0x19ffffff, dp(18), dp(4)));
        reportSpamButton.setTag(Theme.key_text_RedBold);
        reportSpamButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        reportSpamButton.setTypeface(AndroidUtilities.bold());
        reportSpamButton.setSingleLine(true);
        reportSpamButton.setMaxLines(1);
        reportSpamButton.setGravity(Gravity.CENTER);
        topChatPanelView.addView(reportSpamButton, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.TOP));
        reportSpamButton.setOnClickListener(v2 -> AlertsCreator.showBlockReportSpamAlert(ChatActivity.this, dialog_id, currentUser, currentChat, currentEncryptedChat, reportSpamButton.getTag(R.id.object_tag) != null, chatInfo, param -> {
            if (param == 0) {
                updateTopPanel(true);
            } else {
                finishFragment();
            }
        }, themeDelegate));

        emojiStatusSpamHint = new LinkSpanDrawable.LinksTextView(getContext(), themeDelegate);
        emojiStatusSpamHint.setTextColor(getThemedColor(Theme.key_chat_topPanelMessage));
        emojiStatusSpamHint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.3f);
        emojiStatusSpamHint.setDisablePaddingsOffset(true);
        emojiStatusSpamHint.setLinkTextColor(getThemedColor(Theme.key_telegram_color_text));
        emojiStatusSpamHint.setGravity(Gravity.CENTER);
        emojiStatusSpamHint.setPadding(0, dp(9), 0, dp(9));
        topPanelLayout.addView(emojiStatusSpamHint, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 25, 0, 25, 0));
        topPanelLayout.setPriority(emojiStatusSpamHint, 8);
        topPanelLayout.setDebugName(emojiStatusSpamHint, "emoji status spam hint");

        addToContactsButton = new TextView(getContext());
        addToContactsButton.setTextColor(getThemedColor(Theme.key_chat_addContact));
        addToContactsButton.setVisibility(View.GONE);
        addToContactsButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        addToContactsButton.setTypeface(AndroidUtilities.bold());
        addToContactsButton.setSingleLine(true);
        addToContactsButton.setMaxLines(1);
        addToContactsButton.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
        addToContactsButton.setGravity(Gravity.CENTER);
        addToContactsButton.setBackground(Theme.createInsetRoundRectDrawable(getThemedColor(Theme.key_chat_addContact) & 0x19ffffff, dp(18), dp(4)));
        topChatPanelView.addView(addToContactsButton, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.TOP));
        addToContactsButton.setOnClickListener(v -> {
            if (addToContactsButtonArchive) {
                getMessagesController().addDialogToFolder(dialog_id, 0, 0, 0);
                createUndoView();
                undoView.showWithAction(dialog_id, UndoView.ACTION_CHAT_UNARCHIVED, null);
                SharedPreferences preferences = MessagesController.getNotificationsSettings(currentAccount);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("dialog_bar_archived" + dialog_id, false);
                editor.putBoolean("dialog_bar_block" + dialog_id, false);
                editor.putBoolean("dialog_bar_report" + dialog_id, false);
                editor.commit();
                updateTopPanel(false);
                getNotificationsController().clearDialogNotificationsSettings(dialog_id, getTopicId());
            } else if (addToContactsButton.getTag() != null && (Integer) addToContactsButton.getTag() == 4) {
                if (chatInfo != null && chatInfo.participants != null) {
                    LongSparseArray<TLObject> users = new LongSparseArray<>();
                    for (int a = 0; a < chatInfo.participants.participants.size(); a++) {
                        users.put(chatInfo.participants.participants.get(a).user_id, null);
                    }
                    long chatId = chatInfo.id;
                    InviteMembersBottomSheet bottomSheet = new InviteMembersBottomSheet(getContext(), currentAccount, users, chatInfo.id, ChatActivity.this, themeDelegate);
                    bottomSheet.setDelegate((users1, fwdCount) -> {
                        getMessagesController().addUsersToChat(currentChat, ChatActivity.this, users1, fwdCount, null, null, null);
                        getMessagesController().hidePeerSettingsBar(dialog_id, currentUser, currentChat);
                        updateTopPanel(true);
                        updateInfoTopView(true);
                    });
                    bottomSheet.show();
                }
            } else if (addToContactsButton.getTag() != null) {
                shareMyContact(1, null);
            } else {
                Bundle args = new Bundle();
                args.putLong("user_id", currentUser.id);
                args.putBoolean("addContact", true);
                ContactAddActivity activity = new ContactAddActivity(args);
                activity.setDelegate(() -> {
                    if (undoView != null || getContext() == null) {
                        return;
                    }
                    createUndoView();
                    undoView.showWithAction(dialog_id, UndoView.ACTION_CONTACT_ADDED, currentUser);
                });
                presentFragment(activity);
            }
        });

        restartTopicButton = new TextView(getContext());
        restartTopicButton.setTextColor(getThemedColor(Theme.key_chat_addContact));
        restartTopicButton.setVisibility(View.GONE);
        restartTopicButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        restartTopicButton.setTypeface(AndroidUtilities.bold());
        restartTopicButton.setSingleLine(true);
        restartTopicButton.setMaxLines(1);
        restartTopicButton.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
        restartTopicButton.setGravity(Gravity.CENTER);
        restartTopicButton.setText(LocaleController.getString(R.string.RestartTopic));
        restartTopicButton.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_chat_addContact) & 0x19ffffff, 3));
        topPanelLayout.addView(restartTopicButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
        topPanelLayout.setPriority(restartTopicButton, 4);
        topPanelLayout.setDebugName(restartTopicButton, "restart topic button");
        restartTopicButton.setOnClickListener(v -> {
            if (forumTopic != null) {
                getMessagesController().getTopicsController().toggleCloseTopic(currentChat.id, forumTopic.id, forumTopic.closed = false);
            }
            updateTopicButtons();
            updateBottomOverlay();
            updateTopPanel(true);
        });

        closeReportSpam = new ImageView(getContext());
        closeReportSpam.setImageResource(R.drawable.miniplayer_close);
        closeReportSpam.setContentDescription(LocaleController.getString(R.string.Close));
        closeReportSpam.setBackground(Theme.createCircleSelectorDrawable(getThemedColor(Theme.key_listSelector), 0, 0));
        closeReportSpam.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_chat_topPanelClose), PorterDuff.Mode.MULTIPLY));
        closeReportSpam.setScaleType(ImageView.ScaleType.CENTER);
        topChatPanelView.addView(closeReportSpam, LayoutHelper.createFrame(34, 34, Gravity.RIGHT | Gravity.TOP, 0, 5, 5, 0));
        closeReportSpam.setOnClickListener(v -> {
            long did = dialog_id;
            if (currentEncryptedChat != null) {
                did = currentUser.id;
            }
            shownBotVerification = false;
            getMessagesController().hidePeerSettingsBar(did, currentUser, currentChat);
            updateTopPanel(true);
            updateInfoTopView(true);
        });
    }

    private void createTranslateButton() {
        if (translateButton != null || getContext() == null) {
            return;
        }

        createTopPanel();
        if (topPanelLayout == null) {
            return;
        }
        translateButton = new TranslateButton(getContext(), this, themeDelegate) {
            @Override
            protected void onButtonClick() {
                if (getUserConfig().isPremium() || currentChat != null && currentChat.autotranslation) {
                    getMessagesController().getTranslateController().toggleTranslatingDialog(getDialogId());
                } else {
                    MessagesController.getNotificationsSettings(currentAccount).edit().putInt("dialog_show_translate_count" + getDialogId(), 14).commit();
                    showDialog(new PremiumFeatureBottomSheet(ChatActivity.this, PremiumPreviewFragment.PREMIUM_FEATURE_TRANSLATIONS, false));
                }
                updateTopPanel(true);
            }

            @Override
            protected void onCloseClick() {
                MessagesController.getNotificationsSettings(currentAccount).edit().putInt("dialog_show_translate_count" + getDialogId(), 140).commit();
                updateTopPanel(true);
            }
        };
        topPanelLayout.addView(translateButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36, Gravity.LEFT | Gravity.BOTTOM));
        topPanelLayout.setPriority(translateButton, 12);
        topPanelLayout.setDebugName(translateButton, "translate button");
    }
    private void createAddProfilePictureButton() {
        if (addProfilePictureButton != null || getContext() == null) return;

        createTopPanel();
        if (topPanelLayout == null) return;

        addProfilePictureButton = new TextView(getContext());
        addProfilePictureButton.setGravity(Gravity.CENTER);
        addProfilePictureButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        addProfilePictureButton.setTypeface(AndroidUtilities.bold());
        addProfilePictureButton.setTextColor(getThemedColor(Theme.key_featuredStickers_addButton));
        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append("icon");
        sb.setSpan(new ColoredImageSpan(R.drawable.outline_attach_camera_24), 0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append(" ");
        sb.append(getString(R.string.SetProfilePhoto));
        addProfilePictureButton.setText(sb);
        addProfilePictureButton.setBackground(Theme.createInsetRoundRectDrawable(Theme.multAlpha(getThemedColor(Theme.key_featuredStickers_addButton), .10f), dp(15), dp(3)));
        addProfilePictureButton.setOnClickListener(v -> {
            final Bundle args = new Bundle();
            args.putLong("user_id", getDialogId());
            presentFragment(new ChatEditActivity(args) {
                private boolean shownAlert;
                @Override
                public void onBecomeFullyVisible() {
                    super.onBecomeFullyVisible();
                    if (!shownAlert) {
                        openSetPhotoAlert();
                        shownAlert = true;
                    }
                }
            });
        });
        ScaleStateListAnimator.apply(addProfilePictureButton, 0.04f, 1.5f);
        topPanelLayout.addView(addProfilePictureButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36, Gravity.LEFT | Gravity.BOTTOM));
        topPanelLayout.setPriority(addProfilePictureButton, 12);
        topPanelLayout.setDebugName(addProfilePictureButton, "add profile picture button");
    }

    private boolean isAllChats() {
        return topicsTabs != null && threadMessageId == 0;
    }
    private boolean isSideMenued() {
        return topicsTabs != null || !isSubscriberSuggestions && ChatObject.isMonoForum(currentChat) || ChatObject.isForum(currentChat) && ChatObject.areTabsEnabled(currentChat);
    }
    private boolean isSideMenuEnabled() {
        return topicsTabs != null && topicsTabs.isSideMenuEnabled();
    }
    private float getSideMenuAlpha() {
        return topicsTabs != null ? topicsTabs.getSideMenuT() : 0f;
    }
    private int getSideMenuWidth() {
        return (int) (dp(SIDE_MENU_WIDTH) * getSideMenuAlpha());
    }

    public static final int SIDE_MENU_WIDTH = 64 + 7;

    private long lastSwitchTopicTime;

    private void createTopicsTabs() {
        if (topicsTabs != null || getContext() == null) {
            return;
        }
        if (pinnedMessageView == null) {
            createPinnedMessageView();
        }
        topicsTabs = new TopicsTabsView(getContext(), this, currentAccount, getDialogId(), getResourceProvider());
        topicsTabs.doOnUpdateSideMenuPosition(() -> {
            invalidateChatListViewTopPadding();
            AndroidUtilities.forEachViews(chatListView, view -> {
                if (view instanceof ChatMessageCell) {
                    final ChatMessageCell cell = (ChatMessageCell) view;
                    cell.isAllChats = isAllChats();
                    cell.isSideMenued = isSideMenued();
                    final boolean newSideMenuEnabled = isSideMenuEnabled();
                    if (cell.isSideMenuEnabled != newSideMenuEnabled) {
                        cell.isSideMenuEnabled = newSideMenuEnabled;
                        final int position = chatListView.getChildAdapterPosition(view);
                        cell.relayout();
                        if (position >= 0) {
                            chatAdapter.notifyItemChanged(position);
                        }
                    }
                    cell.sideMenuAlpha = getSideMenuAlpha();
                    final int newSideMenuWidth = getSideMenuWidth();
                    if (cell.sideMenuWidth != newSideMenuWidth) {
                        cell.sideMenuWidth = newSideMenuWidth;
                        cell.updateTranslation();
                        cell.invalidate();
                    }
                } else if (view instanceof ChatActionCell) {
                    final ChatActionCell cell = (ChatActionCell) view;
                    cell.isAllChats = isAllChats();
                    cell.isSideMenued = isSideMenued();
                    cell.isSideMenuEnabled = isSideMenuEnabled();
                    cell.sideMenuAlpha = getSideMenuAlpha();
                    final int newSideMenuWidth = getSideMenuWidth();
                    if (cell.sideMenuWidth != newSideMenuWidth) {
                        cell.sideMenuWidth = newSideMenuWidth;
                        cell.invalidate();
                    }
                } else if (view instanceof ChatUnreadCell) {
                    ((ChatUnreadCell) view).getTextView().setTranslationX(getSideMenuWidth() / 2f);
                } else if (view instanceof BotAskCell) {
                    view.invalidate();
                } else if (view instanceof BotHelpCell) {
                    view.invalidate();
                }
            });
            checkUi_topPanelLayoutWidth();
            if (floatingDateView != null) {
                floatingDateView.setTranslationX(getSideMenuWidth() / 2f);
            }
            if (floatingTopicSeparator != null) {
                floatingTopicSeparator.setTranslationX(getSideMenuWidth() / 2f);
            }
            if (emptyViewContainer != null) {
                emptyViewContainer.setTranslationX(getSideMenuWidth() / 2.f);
            }
            checkInsets();
            checkUi_topFade();
        });

        topicsTabs.setSideMenuBackgroundDrawable(glassBackgroundDrawableFactory.create(topicsTabs, BlurredBackgroundProviderImpl.topPanelChatActivity(themeDelegate)));
        topicsTabs.setTopMenuBackgroundDrawable(glassBackgroundDrawableFactory.create(topicsTabs, BlurredBackgroundProviderImpl.topPanelChatActivity(themeDelegate)));

        int index = 8;
        topicsTabs.setCurrentTopic(getTopicId());
        topicsTabs.setOnNewTopicSelected(() -> {
            presentFragment(TopicCreateFragment.create(-dialog_id, 0).setOpenInChatActivity(this));
        });
        topicsTabs.setOnTopicSelected((topicId, fromMessage) -> {
            if (topicId == getTopicId()) return;
            hasSendingMessagesInBotForum = false;
            if (updateStreamingTopic != null) {
                AndroidUtilities.cancelRunOnUIThread(updateStreamingTopic);
                updateStreamingTopic.run();
                updateStreamingTopic = null;
            }

            if (botDraftHeightController != null && topicId == 0) {
                botDraftHeightController.setMessageIdToOverride(0, 0);
            }

            TLRPC.TL_forumTopic topic = topicsTabs.getTopic(topicId);
            TLRPC.Message message = topic == null ? null : topic.topicStartMessage;
            if (message == null && topic != null) {
                TLRPC.TL_forumTopic topicLocal = getMessagesController().getTopicsController().findTopic(-getDialogId(), topic.id);
                if (topicLocal != null) {
                    topic = topicLocal;
                    message = topic.topicStartMessage;
                }
            }
            if (message == null && topicId != 0) {
                return;
            }

            lastSwitchTopicTime = SystemClock.uptimeMillis();
            topicChangedFromMessage = fromMessage;
            if (fromMessage || true) {
                if (topicId == 0) {
                    savePositionForTopicChange(getTopicId());
                } else if (getTopicId() == 0) {
                    savePositionForTopicChange(topicId);
                } else {
                    clearOnLoadAndScrollMessageId = -1;
                }
            } else {
                clearOnLoadAndScrollMessageId = -1;
            }
            getConnectionsManager().cancelRequestsForGuid(classGuid);
            getMessagesStorage().cancelTasksForGuid(classGuid);
            classGuid = ConnectionsManager.generateClassGuid();

            saveDraft();
            messagePreviewParams = null;
            startLoadFromMessageId = 0;//clearOnLoadAndScrollMessageId >= 0 ? clearOnLoadAndScrollMessageId : 0;
            firstMessagesLoaded = false;
            clearOnLoad = true;
            waitingForLoad.clear();

            justCreatedTopic = false;
            if (message != null && topicId != 0) {
                ArrayList<MessageObject> messageObjects = new ArrayList<>();
                messageObjects.add(new MessageObject(getCurrentAccount(), message, false, false));
                setThreadMessages(messageObjects, currentChat, topic.id, topic.read_inbox_max_id, topic.read_outbox_max_id, topic);
            } else {
                this.forumTopic = null;
                threadMessageObjects = null;
                replyingMessageObject = threadMessageObject = null;
                threadMaxInboxReadId = 0;
                threadMaxOutboxReadId = 0;
                replyMaxReadId = 0;
                threadMessageId = 0;
                replyOriginalMessageId = 0;
                replyOriginalChat = null;
                isTopic = false;
                isComments = false;
         xxœ 	@ö¿œì½mwÛFÎ0ü}“×‘E±“tÛ«©Ó[¶åDÏúm%¥ÙÜ{zth‰²ÙH¢–”’øz¶ÿı0/œIÉN·{åÙm,rƒÁ`0ˆ¢è·?ı)2tµ&·ñº;Wë$FËÍ|¾ıÿV1¦hç:[Ÿfùf1\Çùzt›'ñt}^FûÑıWôÈ,»L’é‘,ß_Î2(ØÚãÁÇª˜­ÓÙ],É"û”L °÷Êû[”Ì‹¤i~=xàô—E’¯¡û‚ò^™P6«i¼N ÑbÏ’–à7ë×,Í‹õYOÏ“¢ˆo’‹[%´Qº'­u¾Ipñ§èqœ-×qºÔ7×ëP¤ë:[¥“*ş‘­b1jº?É–^OÄ8ù•]œ&·Éäcw9}G@»ôµš,„_1Š¯‹N‘¬7y,×„Më&ô§0¦¯8Re««x™Ì¹¾‹Àëlqù)ÉçñWÚPã½È³ù<Œbª³¸ĞH<…Â'i<Ïn£vT_Ìa¤¯Òå2±ÆÛ,s›N“S(¶N—7'€ëÏiò™C‘Ê¥É|ìk¼ZÍïNòx¶>ï®+´£ CÿNÖé§t}×[c»uæÑ;˜$—×¿&“u'-ÔŒkMÄpáÇêYç¶×ÁN]e«Íª5‹µØ©×’h"ĞÛt¹æèƒÏ³gØ²«QVpZ{ªë$ë»‰Å½)y~^eEºNqş Ô‘.Fˆª£ËÑèòœ£×³gø_˜?y–Nß­Ó9TH
"Ö_’»ë,Î§\,N³É¦ğØQÀş­b6¡É¢&ÿv¬æ)ÖØÀèLişŒÓi[P±“N=ñàÌ¤x‚T+Î¡wğ/‰#Ùè'	m³ÄÅb¬+'Tü‡hßndHşœ­“­ b¥ñ'¬+×å£¡%Ëè¢Â•ƒMm¢«(å‰É"‰óÉ-®ngi±N–¥ì+°;Šsàñ!•¤ÙósZ¤×°°3Ù‡ØÉ–¢òU<ë'qM•¨GÌv“#ÄxÀ: &c‹ÇPÊ=9{×YÿvˆPğµ“Í<ÖëO÷X!Y"ı›ñÅ^§.—b!&sè
t^BïğY-${ÑÓ×Nwªltxh/"ÀíëM¾tô‚9¬AÃÏézr+WéE¬:¼ƒqXÏ³ÉGkxyÎç©Ç„øñm¼¼I¦§%j Á@Ôg!ãcôÏ
:³;û¡e ˆ?%JÂ J¬ªÖy…RI4KîÚšÂ6ÜR`›·_.Qƒ=œ Ja×£§ukXü¶ m°(ü3P4¤Ì‰—PçÆ$^N’ù ùÇ&)ÖPáÍ&=¸©À¿ÂŠœ<Ä(.>ÖÖ×ï_X@d€ä:VÅ<í‡Œt—-3Ÿ`…µ/^‘fvÁ·HDƒÍ‰~û¯=«¦1í~ªOÜ“J¯¨†•lÄ¶K˜Kö÷ÏqŠZ Ğ‹t¨¸O d=M…ä	ññèlpuÜg(Y‰ñåªv¸åú=K—S¡ˆ»Šo iK<’ûÍZ¥bMû»óøKy}µsJc&V^ZwSü0^Ä_@vûsL×¿Ü¬ 2ú„ƒ‚r tÕóx}ÛÒ­ƒvwpğyXõH\®•¨ô%I®A`…2qáyöC©´èl¬ªé*:$ĞÀi·°^ˆ	¼lUÄáğÁñoI§n±Šô@*EO£Fxøha½hÎ>ûÍ–\c·ÆÖRL‘ı©+uã—yz“.ck©Èìw55aI\•ğ§_#-|vQš+}œ- Ì!³°ÌĞQ,ôµÈÙçéùØTP·¨ÖM!,6sPÎ‹µ:$Îz £äÇîÀ·.+Çé²Ô |I¿s•j>™Y¶’\BâFØ±p„vÉœ™ƒ)öÍ³fšˆ/"øQ¾ğv"¡²Awvï7ŸĞLQo
‡ÁYÚDóéXWVNÀĞTÒÏW)Dc®1eœ­ô¿À¹‹}°Â"P<şèÖ@ÓÒGev7õídÁÛÕ÷õnÿ-i•ÆØÿ˜¶şwš¶•ëëoæ0ÏPÈG0KàÿÓäËs‡	 12½¼œß¦ó©5­Oa/‰%ŒFIs@^3–úàe‘'‘a30Ö³õx:‚HÏ®¶¨ŞÎâ;Ø4½Mæ+ÚÀ"0áaßœÇ9¬[W_ZVóîèøíøª;è]Œœºö§7yŒ½ëŒ.¯¢ê_g½Sø¶ßNW­o÷èÏ}sâRB.90aİÛ”RÂ¦ÓB±¾\wBÂÑ-×¹ÎIDŸæğÆ*F[Òq•§°p%Ñ'-€ÛÍÍ<éİ9-æÀ³Fbø¯³6ôKX®U!'£FÙ'°@ëÅ“[§‚8yµ#´†ğöEú’.‹5šo²Y„ÍÈuí8á—©S0šà£– [à•ëÖé]Å[ñ
GÄÀ¦·Œa2NI²Û½2‡¹N3&(3£†ÓUôZ­€'sbp®kïH—dUı€”å¬ñ0«óÃÑœD¾É›cu!@‰ê
¹a¢-ÏHo í³$–ÿùO˜²–ÕğRØ-×É—µ0ærTÙÃKDõŸ%L¨
ÿ-ñ2ÃÊ{¢â$™'7€¼-Tlµ0Ô`Y!xR4Ö¦Òïı {5>¾¼Á§½pÛ ^^å)ìÂA1Ú?Ø¯¬r’\on.@(›uÃßô…Xâqı`¦ÿt“‹²;×ÆÇ8Ñ²ÖÖÑıD¥obtË†y«oZ³ŒÙ!É5›†´(J*¸Ü“ÛóÑÎlôòû¦übµõ]S±jÓÀ«z~y·œfìÜßÈ÷çõ—†(J·Ì©“áaR İé”T†x>Êãe1ñÇ‡–·ğ¢*r`§9)Àa…i'QQ*JbƒãéJß“ÿÕ\"‡ìÿàæ6‡ÕLàæz:ÿêŞPG%/Å“bğ‡ka ©óÊ`1Hõp:Gİã¿Ô±X·Î³iâ1Q!OSË=8lEF0ÒËYbiíª­.ürã€X¿•»eC¹D 6"–‚MşS0­[Âveašš›œÛzñı~°P÷à°U‹×™%ìN÷êª×ô/Ş´£ãÍ5l¢’ÿI“Ü¬Óéu‡½ñå»Ñø¯ïú°"íÖÎñÛîÅ›ŞøwmîwëÕIø{uìášqTÂU›ØÇ€¦œ;qp²ò$¹¼.’ÄIçr	ûà“<ş¬wÜ™ûÆÙ§Ã~—ÌøÁúbË'õH9EB‚qS1ÉÚ„ê›<Û¬H:ÓN¼$õ§Mˆ”åGw« ƒD—œ
p'âõ0$fÔÃTˆZŒÿ‡ùÖŠå§xNIğò{|¤æOğù]
7´o¹ãÛÚCò¹åõ¨™“ ä“'•¦èz>H–Óß—$âOŸòûÚz©0À6„œ<©w|8äBæKÓßÉ@$™·•ô4WÄê\¬‰İeº >Ÿª×-›0¦n§-§ŸVeCˆ0ş=ÄãbĞã,]º¾*0È,$¾NxÍmã~#4³9ˆ<@õt*~ÒßÉİXRË8ö7ó5zboÕÚår”m&·šsZŸÚQò	”V’CÛ’°7Ÿ§+<r?ºísš&¾­¿Æ ½¹…½èÜËŠ-FW€™c²ÛRï¤È8~ŸN×·ğÅ¾e)4xIéè•­TíÎö+M™ıèßß’&ş­c°´'Ï¥T—õ¸è!ÿá‡Rğ›óˆ8†è-'ùİ
ğG…¼ŒqíŒä'¤:,Ê£*éuˆÎ–0Ùfé<ö˜§Ò“
ˆ#{£P‰QèŞm¾8¿<é‡İŸ{'î™Ÿî<Y-q Zö¸ ­ı}º¾£ˆ_ãuÖ©ğÎ¢¸O³ÏKôïlG0Î°†áÈ&ñ<)OæíáM¶­A§ ?:ØõQvÛúÉ{x³-f	l=´ÄËmPêAáˆ`·rl‹ò:¾KG	sØäŒñãöèw'““¤˜ä£Xà{ùí~ *ŸGæPfK<AÎNãOhbŞwğ'ÙêÎÃJ¼Ü«c¨Áş#wšÂŒ³ç)(†»ôÏ½AÿôÃCğÈ,Ë?ÇùÔ'¹z¿MÿNE¥‡f‰Û8O<é-Ò-%Ö:Mç>Ûn‹Õ–’µ–z½R'TÇÂˆuÊù#‰¬ÿÌëºyıc¨€>‚8Ğ‰8êµw’e&ñİÒ’p™?@¹äêPıiñ÷ı_”?æöûşNP~¢Yççş°tÖ‹~?ß\^ô‚z“ƒ§#›è	{‘IÁS]ÅÓòâ%P¢F±ßGÂü¾ø
ögZDç¿ˆ©b˜b/òµ`oÔ¢KqNEØï¦›tëH-–·g„F ĞdÄÅ,÷1Ëo¥¶íÚ’…¾­a´ÿ€<˜ÂC‰‘	'2 <«ˆ½Uw@t¦i±H‹â}ºœâgçÌÌ)ÄÖ¸©–×UƒòmK©ò.ÛR'û´:ÍW·qkÏ²|/Ğèû_p-zà:û³½ÎÚ<ÜA¿’ƒç{ åèßZïfòïâïÏ ‘{Ë©Ø·8³yú#5ÂuÆÆÆÄ¸®[s†™Ée‹‹Ûìs;{eıÎöy*¦«?Üió(4m†¶
õºúĞ}¸êGİ7CuögÛŒä®ö,(ÊÜ#A mp”¤L—ë¿ÿÍ³‰D?ÿå_øçx¾I„úñ)~Å–R›qát+¬Ó­5šÒMusšÀålÔš¡ßhô¥­J†Ÿ†,ÙŸâ:Kä§X^ì
ÙšD3Ó¡jóQRø@O¬^}Ùöö¡dQ¾“ÍÈ§¶uM€¹q e¯AÿÇ¥%f¹¨6Ñ»ˆ‹ñØƒQXàÔQÆLÂŒuÕdªµğÿFÜ=KÚú°È‡§%ÓËç\àUĞÄÙ¬ûy¶&Gğº:Dn)Æˆ;ªÂ^?Ofki¹ÏVâm‰âÏkräf97+òß”°5L`I8†ßµ{»àhù„ËB×T¦nTbL-‰©LV"¬[3¥nz)^g•2EÿâGY
}V¿€†
EÅ¦%PÑìì‚ÇB C3
Œl+z=ŸÁkXÁO£o¢–·J‹Áè”È§ :pwá%ùª!šâÃd@÷)ÊãG†dÍè*‰V	şy3ø<U­'pu$xÄ®Öäq$­øœ§ÌKXy¹÷ÏCBïm·_µŠ·ª‡ÿA×sºş2[*p #M>&Ó–q¬¨[-÷—b;ÒÙ\}†ÒÖïr%ŸgË›z˜‹;Ú:É„Îløõ½ÑÅ¬°›ñ¿ZäYF:Á¸Éå›ámœ}lø·w„î&Ë«İ§y|ƒÙ«Aï¼ÿî||ÚëŞä)ƒRúğªƒjoĞKoO)±ºà‚OˆtşäRƒ¤.ÀÌaåµYsMÕ1§¶¯'öö.’L@Òf ±Ük-‡ë˜Î¿‹Ûa²şñØğutƒèÅ1ŞÏ"§hKUâ5Gj\‡¶İ€»î66maÙ˜¾Ã??ræ¢Î<YŞ¬o_EOÄ!öÒ >
PyP•Á¡}¬ïöİCëj34€ı‰´Ğuëc•¶G—Ìû£·qñ3*€ä¨Ğ¯plgqëP5ƒih¸Ä^‚Õßí¶+ğT¸Jp†‡É%WÑëÎèÃŞ^%\|°bº¹«¨Çn¦v;Õµİau1zÁUÚy~gQ²Ê¿FQÃ¨„l}gø™‹¡&§7Y±M1ëë¸Dğ¨„½6.j8MÊëäÒ_şµ>·©›ê¿ûô?oí’¶ö?€nÿ5ë>–`q}‰Ü'Lõ-ÆÃ´ÙT3ï×éá&x3P`Z(±ß¤÷¶X‚=†ºS\Æ_(NÒÉ-È,åšQXšĞ­+øúõNPONGßctcJŠÕ÷&<Øôµ¼ŒóˆqŒ€×ş@ÍÕÚÛıêa‡½zÜ«Y8Ü“Gaiãu²v-[bgµ,nÜ¡«î#„_Ö\×v+nš.k¸-S+©P6RP6pŠSCİH›N›˜7‚šÁp–§f4>Á`iBüJà#i›ùVuh}±Ñh¢ÕSa{<›£Y¢!üå-áqvİ‡ ãÊb—@q–Q×êTjdï!U‚êª<>ş[ŞÌã¢(_ºÅcŞÌÙßşq—ïºöæu8¤œ¬u’M6¸G¦ò€1Œî|ZºSGÕ¨	”W¶5:ÇŸât›ÖÿÍaÄL#š'ò¾i«ÜÇwÔFìY'vçñJ˜<ÜaFşšNØm¤Ùe¹f*´	WOxŠ(ª¢uÕm½ƒÿàéÊXœOÀ«uF+ÊäßC’(UÚn(Ş¨ ·§Q\4ò9¬ÄªÊU4q ØñœÆhq šgOÚÀÕÅ7 ñTM{vµ5Å&Cšq»‡&Nê…p”­\hN‰ãyº¢hyâ¡`Ê2eö¬)l8#˜Îá
:¥Â±§­'-z‹ETù)²ü_§0
º ªÙR¸–è‡eÑ×İ4lzÒ¾çøßÏ;ßÎÊëÇğ²7¿½ôÿ/é7G—WjWêÅ‡°I«tDs;[Í#Î[ Xz(=^ÎjÊü­¢Lé· fÚ?Ás`p|$ğÈn×3AÿHO†Ô°]Ÿ5£šd«xi¼D¥³»P”€İ
#
óÕˆÁó6Á³:`ğÊß, ´İŸ¹^j}À+í.da>%î—‘fB£óÒE›<!é…„"K!N$&øVœ®(¤Ÿ‹*ÃU2ß>ã[X¬¢‚È^Ù[z…ÃY' Q¤–ê¾¼XZ†Ô¹®Àˆtğ}X{’0a>,§qî!Ğ;uû»¸X¥1ù»;XÅºh ôä0zÀˆ×?J<qÅáX~û½ñkµDMÁƒ"rîà:©5^Wv¬±áNçÊ4qqâ[É²!v5X5È¦‹Úø°ç™Î$•Küä#İ0r—w¦ğût>¿ÈèR’W\8Óo9ÀÆº^ˆ*Õyo8ì¾é‡½îàø--‡İşEo`V«ÑáÕ¯†œ“¡Ëw[Ü¸³àQ(¥T_åŒ4rI«ó×sX Ÿ¿¿Å¥f®-Ğ­¥²»(ÕhYWÿ¾İ«)¼Íe?¶©šë}h¡‹ªƒd5T©ñ´Ì˜¼P^t³ í¦½Ø÷´±Ÿ{ƒQÿ¸{&Bt€Òö]çÅ‹™£ƒa?ûËÕFºK…ÑÓ7tw	™á 2Á—Ñ¾#\¿“(:”ë}YÅË)º€m3)ø£bhZ¥ô¶.úsm8Sâ;JØåëæ„]ÚeÀAÿÍÛ:Œ¶H>‚53é3¹aÈ4“.§ïoÓur4ß$X÷y“ªö[iqnôDœ56ĞÚ`	{_A÷1ğßcÅJdb4Ò‚çıs˜×g¾Å™'"äY?k:)üW´Ûã'ßéQÄ±¸$H,G‹ßê˜_:a3BÚ.Ø|éµH	e7× :!±»±ó<íŸi)È”Ëõ­Öşf™5 róQæºZ[ÏH·~ÃÅm†6eP]~uc “0âtLN:9õK QQÛ?±5E%³,¢ïo'W`”D‰kÜ``ï¶¬0îÕWöÑ«Jt£Ğ[Øcp}ÀXƒ†·Ùg«P-‚ÄFP&Ä´‰?p
[)kœv]£›seˆ­#§CòÇ¨ÿ a¾x‰ƒÆ,F  ZgŒMeë¾ÑQéEü{³4]á0~Ãì&¦£Å=•?‘<™©Kü´¡Ccôy¶Ì¬üLÜİ¬z`!ûeOö9û4oƒ¬T”]-¢£ßù¢/†*’"Ûä –(**5€D³W°'¹^eºÿd3›™_o 0ªGÒ¡Ó!‡ğówg£şÕÙ‡&4bÄ'	ëe_8øˆ–Oíè€G)4aJT+æËË— ¿l[qí\cïËïYkoˆ(bããªÙÕ4D°z¼PÁİ$ù„|4a°õğÖˆz"¬ÉPycÈÙE4ŠŸÿ._gëó@ücõ0öÎdV/F«h¾UU‘á uA,eXó—‘ti#>q]i¼tQ’½Œbß(’ Õ9O×0ìœB°‰cpŠcÕ¸ô{àšğt&<CÓ!ín,X	£´<5'<IjÇ ÖHNÒ	 Å
I›wW…İ‰$€¬™]y<1«¿*9æã­W&®ì¼:ù *W¨‰,ŞÖbòÑúC¯S6º÷\«wşüçYÍråÓg»%‹Ë¥Qsòßürƒ[¡n5¬N¬±b*i2Ë]¤Æº8ÆÑÈÔ¨(:^á•™#pÄ‹ï¾}qğíó}|ÎÚ4ıµ™G¦íƒ^Ïç×ÀªŞ!õğtê1oå›eK­ÅìEõüºY¬Fæ}n‰²Mv~s/5Kªx´åe6';v’ÛÿŸî€s„;£ÅA¬ 7˜úê'~3‰Õh·ç»XøÕÜè ØlŒfx¢Üèâ*bƒnD€ïj"èä¬"å(“Ü+îUÉ£}cí
>ŸdŸõ5“®ŸcLÈ¹‰çœ§†áR¨]ç”$9@Í/ƒÍr)âò•UeHù%J—Çq‘`²)à•@(Ô&”ä±Bf|)c•‹€åw4V;z7]^Àîù^¾¿ØØŒ3AÒ(dds#ÏWw¶Æ›ppÅÓ,ì‘èO+ Ğj O„(eØ¥¾È²"°ÛW>*ÍL|K×u¤Ì1Æx,úzÕ¨UÑü½ñâÇTYÊGÀ{fäpÄZÁzüİ9òñ_Q²=İá¢ô–b³m˜MœâÚà…Š^Àt¸²[€
^›èJéŞ®ÿ- +Å$¶®ÓJ0Şc;áuq&,û­şvYÍã;åÒ£Ï§Zxşt»±…ÂØõ˜™É™f"†æqÖ`DÍ´%ËI”3Û9 y‰µšg±á3Ø,Œ¿¹D6ØÈ+ğÀ•E1à”öhÍK¿[x§L§î™‘km¼œf!ôı‹=Ÿ]sçº	úLğúŒsÙø6.lèÿTéÎuzÃò».İ›Í Yi½ş˜Ì=]!Vòo¡ç6¼MyjGgº“º3L¼s´`óx]^¼w/úçİQÿòØ|¹áÛËÁ¨,ÈèX!Bàº\N”àÆÇ¦EœÔ »xåÆòş7$9ÇİáŒ@l6 ¸ü“É	t/Î.AÆ6 ÿ‡¯ÿ°|mŒR[×iº8JnAîÓ¾Û ”Û„ÔEs(í*¢´T¡dáğ2WÛÜõ|“W5N<]è¶^ÚÀE]ÊıÎsÚM´%ğO2\ZP»İ)…nï†.UÕ„¼V›„b¡~z±ô0›°şJ¢\Q|ÂZ(`œ`Jn•r­åÉPšô¹VqœğœY˜ªóCÚ±ëLz”äxUÕİÆØ½¦¢ÇLlbc—ióVÓ¹ÌLß«ŠuïÆSq›i}/’<fsP¥n	ÓÃûQùOMéGw>Ï>'2æİır
ùmKc³‡um~1I­ƒÕ8²]J¶JŒ˜3¸d]Å›€W·ğØu 	 FEµ‰eL¶šz•~-»¬,%d7¡0i sÊÅºíz”­¿›ÃV‘òÍp¹™ÇEá›¨åu'!ÏódZ– «ôU}‚aÈ­ãz•àµ.¿+yH’›°¶J;»æÇp‚W>òÂ&§O>^¦ªv¯o;‹t)x´«@™½ãnşx$®M{O	ßè^u!Ä<ÆêR(6p-&œb€-â€œ/öè…vÅÆµqÒUfµ áAÍì…”ê–¾d¢éKjõä	Íşášlë´Ki°°Ù<Ï:4S×Šhú©ÃdZêİròÎ8tu–â%öªø10Ëyx˜~}ôÆÊÚ›çñ6ü£*ğZ‹ÄBUÕe¬hJv0Qjö
Ä‚pSRï]+«àçE\ÎëøKË©Û–Ú¢M_~à5cQM¼˜3ch‡@øİ`Ä2?Ö“ÅıöUgI|@xÒß|œ>'i¨½«o¶ÓcA±…DÆİW¤Üs®ST"\ÇHE¾¤Ó”†çcòf]Çó#(~”®ñªhµ®é£Bô—,^‘4ŒP/a Ê
zä”§NK~¤¿[ôßN÷bÔwÏúİáøô¬û&pÅ£#ñ[´?¶œÏâ­Ê²j¼r¶#ñ¾3Jç	2ŸuÏ¯¯C4P˜yúE¶+~„â/è:/†t/ğ!]şjÙÃÂ¡úxZtHM•]%€¢yrhñ¡^ÅjŞ<ê&v)@Iâ-?&´Ğy2÷Ğßû½7Õ=CÙÃHn62öùûMGOÎíûç%F\.0s4åô–cø—	û[/,PĞ³·WˆM³\Eî@şÒ<HEö=5šª4Tái´°ú‚,úwÏ®Şv•ñwæµVâ­øx
2Ê`ün)ô³ÄÍ<@vkÜ;ğ2±Ìõj…^æ0¤6ØD±ÚT(@5Û/ÎCÆÈJª¾©—Úmq‡8ªz;Ùƒ—ã:,GèŠñ£°¦ªÓY„ğ	.U¬ß` –^L·©zêän2¯Ìòé/òá&Â÷’‰oĞ‚v‚“ó+“idŒ`V—°Ğ(.>VvH—4ÂÛ„K×ï;¬Òõ{¾»a9êÙ$éÚ·­Nÿxˆ®&ş}ˆ å*$X•¿ÈÍ‘u*-ı“/@¥iéLœ%.r–2µZÒJ%ˆìßê'[QK}tTcì^º¼™S.EÎ¡sŠÂ§¢uà8¿¿é¤”-ìŒĞ»Fùf9A]hİé]¸Ù›]dš‰pÅS¹ï-Æ×D÷:“è]@"É˜~JéjEÁ€æsÕ2YoÑ"H_Axô[û3Óª¾6…®¦Bõè__õş6~wÑOú 5¼dj6¿	&kÔŞ8“åª‚‚¼Ãßx/_9ñ2È( -Ó"½ÂxÍ²o§å‰ ’ ÚÅ¿[Ú¢æ•Do9(5è3¢¼1ôJÔN°ZçKôM´ßyùílo¯í£ÑÏ/‡£½¶‡±×g«Dİy]™°™# !Ã¡[	e@°4yß:ï–÷Œó° €&Æ»b¯ø32Ù‚	W"İÛ¬”l Xx¸¹É ø¸Xe°åÌ&…÷œuÁÊœê©Ó\«ÿØdhúoÒæ_±(¿é®bá€'êmÒ9ª6…õKìù¢\?@jX—Md Ø¤B*ÉC/BÒ‡¤„CDÇB3™v†Wİ‹qïoÇgï†ıŸ{å_u£‰m8ĞC˜ßŸ0ı5è“‡ Í!µÏ ÑâYôÎz={ü•‰&ZúİÈ†w šúÑ9¿¼¸„®÷¾.U€f˜&u9›QÔqÃŒ.8°ßMòŸAÀ]!¤3Ç{ÑëÃè`ÿÀ[Ëv8ğ÷ÇÄÕ
HıXßÍ‰öık°Y¢Ó»l5\ÈÕ²¡Ng6Ç``ÿ<tª¡r<}8ëÁı¿ô^…{ks‡	¦-<?y¢ Zù\ñ5ÇçİJÎACv‡èëÃ»‹“Şà¬ñGŠ"KxşV=Á¡òÇ†Ù_U¢øŞÊ¿EÚv‰iÜT8ÍrØBü%Œ`yr³™Çœ(l~UÄòè…;”[cy…ÕÖ·­ÁQœŠ]ÇEb&ÄôUOE„†ş8´®‰·1 aÊ…M§ğ)§R-?µÅÓ(p€/BíêÈG*f!à†B”¦“Q|]à-±›Ã7¾"U_ÑCÆ2âÀÀşé8€Jÿâêİh|ôîè&­€=‰¼-aÕÅ+ª£GÍğÅÌbss“k
‹K—ÿeã´5}¾séãveKºTO€2Ü²¸ğ-Í-Uù·¼ıvSRÈ(ÄÑ¡vLUØè8Øä©kíc(¬„ñB\¿}÷æMoˆ^¨CSƒsØmöY˜¶¹Ô3èu©B¹]’ªQÃÊHx¸ìÚ8K/•
ñ’ögØ¿ı1Få`óGı«KÁt9êıK†ßl¥Xpü3™¼dJaIªxK¼I0â#ÈQlÎ ³„Ú€[1¿‘0£ØEÉõ•‘
¦¶IY^^ñ2Aø—z¬{l«ù¨dÊhÜØ kQÀºš€³Ó€[İ´€ÃJ}(°¾5nøÉÔZPæÛäÑ“C&SâÙ`ä&²¾wfYş9ÎkóœxØŠzrôš€Ö)IxìÑæ¢m-6¨PJT88µ7C¼)ŒÙ@à•¦ĞòBûû¸W?ó'z³J:Æ’xd™¦ÃPìÓÜbÃJ0s¡U¹T;Î³CŠÛ'å“•­·‘nË-÷z‹=ñ'†píá1¹ò0¥áI×g~²ó,úIPç¾–3ñëùçOª'SğÂ(cŒ.ã×ê¬LnÿMR—ÅñáÉ5Ë“äÒA…«”¦eİr¨}FEGalZ¨0:º¥á‚ÀrŞ¡7ÁøĞrı‡]·vÏq‰—1ú¶†~i›‘Ëjò¹·õs€çô¡«úTéWÖ 6ŠÁ.¼LipgêÓ´X¤E0€“µÀ†µñ[)ÌE·ZŠA!ÿB©Pùˆ U«Q8RÖv²ÃLÓğ/”	aBpš›ë}´+/	mff²ù=ój7}ä~}mÜ£OÎèúÖŒÜ(ğ4ÙoJ±dÆéT&¨aæğWä¥…oæ8å|ôj\AÔ!ÄïF‚ly
-ŸHü•6‰—C#¾YÈMìÁÄT¼^Ç“Û+/#àˆwö“o–—ËwıI66öŒ×Ò.ë‚z¶Ó2BP¹áÚÑÁ·Ü¨vašŞ*;Í¹?¸À¥Bm« •Kµ,ÀòÂ?\M"\JÃê²€J_ˆëÁéd‡Áè^¶Cü:C³Ãlj˜¬ğ&×‰JdH¨í* ‚,SFmw’B‡P©àÁğÚÅe´‘Q&Ø¨V­Ã	íùz¤ÃW’rˆ%š‚K–ÓJ`½å´)(±2Vùê
Ï&µ"§RÊÃçµi»Î´å>	]—á°¼Ÿfµû:*ìÄ`Õ×Ù8úÜÇbå>LTFº<mÜéÅÖôÃ§^ñ	Ã`vY~NKËŞÆ—µœA¬°cvûÖûßYA#š¹‹úñ­fK=¬i$ Ÿ˜<˜>¹’l’bİşº)Öƒ„\‘ÙØ“äıfã©Ğ»,µ‚{»Ëo® a¤eümµ©t÷Y7U´«Ëİ,³<9ÉãY8˜š [xãâJ„)Üç«êÛÛqÑ_¥&ƒ[Õå]Ä‘N·ÃöÛÑEhÕ	YĞuÖğ8ú1º€<©›<öÆxál“·kcÓÅSÍ¡—Î
0Ê¦YX%5Ÿrø^T7ãèÖ@³Ö°¶jñQƒ%ØÇÒk›NïÑOÑs¼wVİêÌ®]•Ü˜¶%Ç×g¯FÊY‹ş´øûş/Õfí )÷#m›TÛËkö­¸ÿv´YNAFÇùº9/Ş„V,ˆ˜	QĞzœ-çwbùÌq+ö—ëÖcaï(Ğ£j‰Â…vÒï]¾G®zãÓËÁûîÀ½fD‡tÎÇ"ú%IL¾è¼’g¤ëA÷U¥àî“
:g9”È£¶t)Ê4Â•{\µiØojk¼ÎKÍ5Ğo½ï¨µDœy¶¼‰âÍú–.‹1*ùİhÅ;àĞ-z/BtÒáF¸Iğ~cB+qÀà°H²ò' Ñ(ìyŸÊ¬à"„Şñ<•gNñŠ$ô.¹Ï€&%­Ç \üñ@Óš)R\Bò¯¦¡”X¢®üÑdvªÁ"_‹ÇÍªµyG\ŞeenÚ3Ô¼IàW¾j±#xÙ‘0úB·yÎç6‚'kÒÅby.Æ¤iç+¯ò¤€º§†¾şULyÓt:„Íù^‰Oª¬ÜnÁ­ÍÜÁóg+İö:ØÎÉ üçËUcÖ¬e A7QºDÓdFÿJÒYf¤ÃV¯:S‘İ`Ã3Ç…wyå†df^N»¾-@4Z+‚ı©³Â?ÂgÜ%–l=)5ñçzÀ’C¤DQÀö‘*­}\ªÕ-*ù@·¯Í,ã¯\¡e5§Š¹Ñ½\v‰{Ğyúx¾*WRÔOû’X{ĞQv›CôÜn¼µ‚HÈm­ÌV^áÅ(İ =¼U‹[Q×½eˆ`zRm‡xz
­ ÊE£`Úè(ïja]†¤˜NGÙÕØ…¸>¿õwÆI£¨È-Ë7Cb	T!¸Öa¢øó”œ™ûåoÊÊÒçTB©_‹êô`QÌQfÇ3jÚÌ£j#Õ,a]6Ü·¬^AWDäğÂi®gÖ¬ÒP†:ëF•Y¾
¦`¢ÿ>5F¨š¡èîŠÀ»f¹¡„­%,ò÷*Î¼ã‰Ìaet‹ßÓ¹¶Ùt]HÅrçè,9ÍËÃ)Oõ'?q˜Iš'b‰çP¥O7aáø±ù<+¹VèÜ”2)8	já<Ø\ı
ótË9:¼…¶YâqÚwá½9Ô™wìG°n¥³tB÷(Ša²FÂqrïg¸À;èÛ*ğ@LñæÓãèI-cíuâ¹«ØMTIdó{‹•8Ñ$—c‚NË‰×|/7°ëóîuâã
—£jË=‰±án®ñuë Íµ· Ä5ÛX¦5—g[É2ÖO·ìºĞ¯±É×Vg +J®CBÆD~6GÄÌC]¤BU
ŠÌĞå¥~í˜ü1L²ˆ÷êÁ&Aëáš²]©ø´-g‹ˆñMÄ6	UÓ#Ò¥§n÷Úşœk•-y\íHëèÙ*ÇŒ8¶-7Z£3¨ Ğò­…>Fö£©^Ob3½rĞ/%ÿ7±Ö®Mı”²¯h2Ììå †«mú#Õ‚L]²×.¬íS5(b­Àİ\	“œî=XÏÆBßlÌly9üœ®EBR1àeÚ6çƒ‡Š[ Í	Î«àlÎG8B h&*[PŸ@Y‘êwÈr·¯^¹¡†u-c]Vï”!-´‘Tå:êNŸŸ†·"Îª½„Ô¢Ì¸b/G®ù¹ñR/E.V|å—rRã"4›FÒGË#ÎIl|``Z¡ØÆ·şâ*z´Œ	‡²‹@`¬°:™±˜:¦úÛ-¨VÊ cÊJ†åØÓ¹Ê6æ1ÛDMV¤IÊÎ:]ÏY3)TsƒÖÔAP!õv+*ú¥ˆ€’ö=ŞÇ‡IüìÏËAFÁ·Ìl€Qá4¢BÿÅÚhQKO¢ÇîhDY£ÊZ±, ÷ÄH @ïë3Õªü´›k-ŒØƒM—ô¥üe‹;Ã¨M8­á•êR’+ñ„Ä¹ÌßÆÅí:¾‘á:) Ÿ¹£Ê±í¸U7K?Mù§`=‰×qÉQ}f½$c¤f¸¿n’÷[ô"Éo’Ò¨ í¢"!¢yÉ?³ºÍOJãµºá_{\…HFRQ/‘Ynû¹mUwò—–é½
‹º7eÄƒÄúú½ ãÛt:MhF”‡Ÿtœ¹hA}U9¡M¸µ©Vİ=¢Š5›.g¨—SÇÕL¿,\{5ß8™Øaóì$«‡³™èV€OavıcƒP)É«ç!LTtÒQG™óÛ­dÄ»!UÅĞ¤ÄAlåƒyTÍÜ¿‰À°æP@•Ò’uçãOiñ¶1•÷iGñüUê YeäYìÀÖ&-~Ne8Ï9³£y6ùØş5m Ô¤Æ_0:j—j‘UARl–BÓ‹µ¬!
ø¨g8ÉOÈÚ—IÚILî ‘ğÈxÑY€ëê
&u“…_Dz$‡	B6p²=;BxƒÇeÏ,¯»0pQò,ºÎˆ…ÚÑãÍò V¤W ˜?™ZŸjœm·õ…ÂÇöA|„	¸n´ÕãZå(WDÍÒyb»#„\Ÿk]Bèˆå.wlÊ…w§…G¨"#ô«Ø™)Ş(‰ÓTÕ…«˜eJ9Û*.U+•ËÊşÎÌÛ²J•µ)Ö°ÙF2RsM…2é¶A/§š³x´~çÎÍØ¼RhÓTRïİ«ññåÅÈO%e2rG™¤DhãñÛËAÿÿBÉî©‚òÕFrrc´‚ [âštĞ¦;)P\•™æ`r¬³GÛuÃiŠRÔòÖ~0qî4IdµSrj:4!ñ•c.ı†JP-EØ°ØMÇ¡*×¦å0ŸµÀñr6+(·S‹ÀîEß°Áª_„îâéÙb„—"¨ĞØu9*–# p
HÙoÚÎIÌ‡wç2Ÿ{'w0w4¶|î3gÈ ÍÁ›'ÜÚsÅA fhÿ0vÁå³á„ß™ØÛ8”çŞ Ş-§rÙw7¾å·íà˜•ÅR«Ûa“pRä!{uİ:ÓÉ4~‹ïò9ºœäñ&B%SOGT‘j&ÖÄ¢z½/ìt“È´åÃõV¡m´ä˜<ı&‹v$¯“ ;©«‡Ş«çíh°‘V6QOı4>ÅIıdİ=Ñî4%Óy<·â+*ß49Hn†æ3%uÏÑƒ_¶}ÇŞ\mÚ,Ü•Ñ;qGÁÍ™!Õ&$ÑPÑÏ$œE1—P}‚³§üÀ)L“ƒ6ğİäßl¢4a6Îvô½úß~U.Nƒ•a„ëlä\#ƒïÊ…`~¼_ö^á”³`•Ä<gÉl}ûœty²¢ûgÑ³gQJQ[ñøü—Š†ı“Şø¼wñnü¾2z‹<S‚÷ñBˆFCg™h{!‰„ÆƒÇ^1hÁøàÜe'Ùåwƒ¶zş)¬má`xó&ÅµsŒ¸ŠäFßL2–JPÜÚã¶Z ²uË80İ«¼Ô$¦êâ^ ıQ ÈÊĞH=èpkñ^AN1úŞÏÏ¦×\ëŒÌËQ¶™Üö>áşú<#Å	ÿü/G(ê¦´À(«>pŠàS%¤h¢É`´ÑLÇï®Bs!-n¥Cµ6pz‰ö¯a«ç—?÷‚íSOÑf/ÇõiÕ¸¢£—‹ä´ê–@f²*›Ovjr³[2Ê0c¶à]8è+dÀ¡.;Yp|P>ñĞ¦ZŒµüoÁñ¶îÏŠ[°Bd¢–ŞRÈs±Úë±ä×êWÿ{üKÓ›é|í°^®m®R©Tu)İşµ·(‘3ÍãÏ"ùqz|Êü#Òb’¡BIĞæGé‚UÙÄı(3\PsuÑÅ‹P`:jªSÄŸÂ™ÙD‘É<]QÖ,a}!6ñİô=ò%ªêœ>÷ò{Îé@y»²ØÌ™x"Lßäš¡§ı}ÿ¹²ĞÁ/JŠı¶OìÆíû=³°VfCÒå­¸v¥yD1‡ä“%*‘ÆäáYÎ'Ô«Š?òpˆìÀÍğ§>0<ş±:½§%r›÷5î²—É\†¥Vº¿W—Õş‘ q^¥şÛÈiEVf9ş™+Ğ¤‰×››Ü}0U‹wêb]„kùã½Ò¸ffFıçñrÏ‡“\Üì÷u9y¿ÈÕaWğ)Ø(Óî-‘ËÍsØW¦„òwÉ/5'©#[jàÀ”Ğ†õÒª‚.],.vy¼æ“×8·E6Æ…úXÙ%+.šÕ,Y‰`ğ¡÷R:Ã?Å¿ÕÁ7Bİ`ÛÙ'w›ğNğ†r„OÑ)¬:dI]4¥
ülÂ<İ«­vIşá"F/ô)À>Šú^¨Í§Íõ;äÏK‹ÂÜ|5ÇÈ}á¢¶Ò²DQ¹Ó{>Q,+Çzdõ™¶aà¹‘ÁÔ
ÖjÍÖô²êxWY®®¸HìL{5”¶ˆ+ë¥gå}¬ìI«°E½ÀE§m™“ëÒè™¾`OáX’À’°"Æ]ŒS…‚$¹Î+p±Y\—/m{ŒÛ¨A\Á
­&¥uW+Ikeñš¼µtM-»#ágØró„°uƒí@ßÍşxğ}µ¡ñ9ğÂwÀ2/ñBc¡“<RÚó˜½åsv)7}€ŸVßaÇ(“¨å0Ó(2èÜÃt±š'uŒşÚ*÷bjèÛå®c§…¯È
³-¸ÁÃİêªÑeÖ³ğ©suÂ>Ô ×ÚxZ+å)ƒSßªŞÿà
_¹ƒE—³u4²”ˆL1^0MôêLmå¨Å¢²·Ñ³èy ÇøÔªcš«:_njáqL5³gKÉ®µpP±/·œÖ~ã_of?ÿöS[Üğ*'6—Ø¾Vv×ÒB·²›Òó %ı7oG¤ôàÿ_Öö·¿ˆ–]Fıp³Òj…™:¬“©¬{³öÖ/u•ÂxÇYI¨«x}­ğ?rb4Z…Ë“düû/QOÓTÖï¾ÿ…ÙÈVˆ_/D5(Ä¶Í³ZæÊ²(Ó6>2Ô †©i`Uª²öáÃ„¬G‹NñÒª- ğ·a×Ëé¥$(ºe!/’I’~~±Ü=€Ø›¢
3\1åŸòOxWWëyYë…ªuP[ëeYë[Uëym­?—µ¾Sµ^pŒ£dM 3¦›¬ê? È…<«¶h¾MœŞ9Iñ5^¿¯ o˜zi‚`s5C$áÑİÿí¼ÛõÂ-X“˜6J´>ĞÅÕ6åmvœ-VÙÏ¨DíHX·DÙù¢ÉOè&·ßyñüÛ™C ²í#¤bQËÑÍàMëg©AâÃ® ®roÚ5¨sÕzoŠi¢ˆî|}N¦ÜQte]sòr‡ Ïw\ÔÌvª¶îÏÛş¿r).ÜBZò™Eõ”vÜª7–¨ıyˆ[UØ¥ú^P
w,›2é<‹N!3[Ê…3°ò»[ú²9}‹%¢È6 Ú·4¿èZâ¢¸‹
s¨P…äƒ¸ı..d9üy²™ÍÌ/Í´UŠˆÒ¡„ÑAûXçüİÙ¨uö¡®GC¼‚;Ü–&HG¿“^ßµİ!«“™‰û”Ín™L|“:t›pWOzkw,í =ş%_6*ù¡IÉÒ©b4…Àğ¬’Á‘O—ñju5ï’\Œ{ô_Ñş—ƒÿÑ³=ªD°J†ıÙ¶?’Úí ÅBõk´ÉÀI|Ãì;¬OsÍ`£JõäM—éŠ¨:¦*aÿª™ë ±å¤uj»sÈ¸Êvy§Ù-zçÛ—¯òìİxåâÏÕË­­ëªbƒnùÈ”À©öYÙªış˜Åì¹?ùÍ¢êïmh|Ùf.«6f&?w7Ó?üNÂ¬×şÙÁy¹×Œ8ŞK™ a¹o™¢×[¡Ö¯QQt|E’¹|ÌÆü” ì[ª°7€w:‹{UµeßUÍ–Åü‰}óÏ:`Îã%€8‡"ŒÒËpi‚şæY­3€í]u—PŞĞ5ƒ‡TDu»±tÀØÒkÇ€ØÌu-¬såN–çºÚ©-ãt6)Æ]Š®å¿2s‘_‚™[wÏR‚%K,»4Z=Ş-WšçöÄÆ/ËïÖ@l»è“²nÛĞ©%‰á1 &µ£´â^su
Kè‰·Âƒwr©8q ×l²©'édÙ0¶Æ‚?5ØÄ³‘ä¼æªÇîYs»±;¦K'{Áü¦x³EL–jH,*mÆ[[º}Õ˜nc+?J¨
”Š¢Ï	ÉpÚëL²Å"emŞ2˜w½¥:¯©‡˜Ê…å`Âİ«Á†L	Åem’ii_æ	•¥ZhX^çYØş¢†—|ÆPòI^ø‹¹1£?†—w=Û¬ ö§ß#¡+ı$u·Ùé€¡Ñu	®cØñùÛ¢¤Qp©ÇAÉ`T~¬Íæ	5˜wÄ»¹ô1l&Sı!’ÅÜ-~ˆ «æ®ÑÎ<…‹.¨¨¡aò¿lİEAcØD@¸f^2®·æ Ó.bµì<ËOWBz=nGÎrdo‡€^Í7y<W Uï²ù´aN/ÁK;¢ h dx‘Ó2RÊˆ(â»”ıÌ%‘Õn0ÕÃFnÈO+ÆmR³øZã†ˆMÄFıGÌ¤BôSôøqô½A÷ØUÃW¥™ƒ<
ë3ŠA©XøõÊ>×c…ı®½åØAe·†^4VÛe)u•U;*ö§,m¶IKosyuVKõºŸ2¹jJÒ\’±hLZÔßNİ6­s‹d÷u˜¬sæŞÀXÁ$`p\ı½,n(ù¢´ñMûÇ^«3¥–zµ'aSŒ3A¥»££w£ÑåÅøêrØÙ‘*EÈ]ª«ìé›ĞŠ:$S”%{Ş¤©ËŒ=^i”+Ağ`kß¨EwÒhÔ¡÷H®3X2Ê»´HòÒsoµWnÂZq³,fáD¦=Ìš‚ìÚçÂ1B‘•÷Â8ò+ÓNqÀ#¨|~yÒ_õ/.zæd°’8ÉÔªS5èüiî@(åĞH¯à©#-FR=W. YÈÆìD7eª68-o@ÕKƒ×bÜM„½=Éj­2 æ <S¼ÊÈ Y­½Fc%¡÷ªõŠ…Ge[ïrQÂ5ßú›YS¤Cï¯ºû©Îª)ûUm%Q¶V4ad!c’®nÚèºÛ¯8ë ·MbïiXó,&S[®Ò• ¼FúÆL›u¶ç<y't½Ğm±¢Põàí¡CŒORÎòSK›ÙKó9SJgô+“ûJ)¤#Íû²JgsÜ>
®u#İ)	“EÊ©óïO-Òf··?„XÉhÒ"ÔsKn‡).!ƒš—³«ãƒ`bnØ’LÚ-ãÀ©]æ‡š¡Ã—8lR.×)†_ƒŞrQááYóşµœïµ•˜ø„±ñŠ §¹¶z¬ßÕ~k"¶™Ù|+°ÊåY«Œ½êivãàk›56Á/[§wÍËãd)Ç¯²Âú@¿$¦w=ƒ•Á–ÖVv˜(Lô3uZ]…{§º:_Hƒ[.FñN‡æ>¡øàşêÊh°ê*1JffPæÉÕ*ÅQ|.{Q¹àùë¤^in’5³(ÒŠúT'}´4sùu”‘eäƒ˜§lÿàÁ`²²èªş<Ñ5ãNîu	‘î\]x)t
&	$CË6ÉüvåÜnÕ„nÖÃÕ@šÏ¿D¶Zô}`Ii>;H#ói$tÕó;I)¡É5ØS,[¹zf“>7â0 ‡‘-?š²œ=£ìÿ»¡õÛÃÍGÛLŠ¤ÁUŞn§â¶pª#QyÒMïºÓêhTÜ2ış á|c!|h#½§ˆ5“MPõÔ3•ÚfúUuxãˆç`·v¡ıÓYTë‚T9$ÿñ´ÙÆÓ&dò°í§Œ•'iB·PÖ|¥ğ8-VfºgÕMË  û“½&Ú²nµ*­“›¥e™¹8¼–™ÛdB•¬"·qAúc"º†Š¿dB‘Fgî‰˜+ Ø`O).µyàcåW¹#—ME8Òxo¼öCö{QZé`Ã @â¿ÄM «˜°ÒÏ7ò…QÕ¬™øÁøT¡Z=Õ!Ü(â‰`¿2J¦ŠMP¥^‘ÇÄ¿i4F‹õ—óğ	]Ó¿?|Ô>éŸeÔ6jş¡Iæ½ñApÁ¨€@Ôæ§€÷!c–C=nŠÉbMeNXUĞÀ’!)‘ à‘4 ¶ÚA?å/0º”{'—8©!ií²…)¼Ä~‹³sÃ$t	ñO0Ü[	¯ä7
/ìú½:M±wŞÜrr]£Dë2%Î£@N¾¶¸ú|0sàl™Z£YÄ˜Six"ÃXÎ&Fº†&êçF¤†ç[£…‚g9å¨e{e4Ş*şPá3­2b2'yºÒE¹£t½ˆW-_ó¢}§RÌ5ÛÌÌé¡õ¯À•./¾ÖÂ"İp f"Kz(VŞßãc3DdœÄõ6kXC~h’P$8ÈF³ôU:ëtñÇäÍ<»ç¨Š‰ÒE«uMİ´#ñ—òíñe^\`İfƒä
*š¶Ø]¢z")ç¢Š t¯óô]~lñØŠ¡º¬¢l£%eaï©ëÄX®s”A[[jII…+,Z…W‘ÜdÒcƒÁ—.8u'7JâÈûº”‘!N³ÜÙÉ©óµŠ°—ê¾§ÈŸ]•”—–qe•ïT8?+”»ßi‹XÎ†Ê¼ú9ÊœT£2Edˆ&H¦c–[•PĞĞyÈ(äÚ„’îv™HÁÃã3…R¿:=ë¾ßvGUôğÑø³‘ÎAÌ°æ¾ÒíÈÊ(¸[úê“ş°ZËøjpùfĞÇ?÷{ïi[Y•ÍB„ı=Î8‘
Ê]ZZ Ì¬æŞH¸Ø±Q­¡·¯¶è‡šÕ—«ï„CÓâT v„³ô¦£•=z&z—è³Xíæ>Ü¼ƒ
ĞÁ¹9Ø8¯¬ÜÇ°TXZh@W±`4VbCµk²Ş¹Åu4…[Õøªá+VVıíÕ†
†F+z¡ÿü ô\+½¯.w`_ñ}7ÑRa.x¸ßCiÇ§¹à^yğê˜¹*Â†ÏQ¡q«646Ÿã(“ìá3us	ësvNXgÅØ"¹k³Ä®¿…À›š?›Ğ³¾Ù
•šZX7Ì¤Ù47èÁÌ÷ØÚ!ûæCdİ|€l›»dw,ë¨ƒ&3|wÏi8]ûÄıÒn~eÛ€K~&Î?‘K'³Ef8<äæL•Ãr”L[
à@ëw{6qµW}eD¶™:xhœÁ‡J›67³Õ¨œtÍ•Çz|ÂÏpÚ€r—U'm”p”I4Z·M9G?ª›¤!c”8æI¿$Ó¿$w×YœËÄ5x%nA½/«xiDÙÁ®[dø#9&=±í·â.ŞHF¦âå&Õ;DßííYĞŸÀQ›Fá}Š·ÅôÚğSuZÏ¿‡rß¡pcÊRâÈåH$—ta£ìû³ªŒ;_áç
:ÊZì“,Ñ£o8	êZÄ•o¡ƒ%9øïP“(á>Y oOpª_rXM:ºİDM'„6È4ŠEDopË%”qVÑå•sA%6wêÖ®àĞŞ”·l¬»Ièù“ÃÈjĞMœÄqR”a@ƒ7BŞV_®Gœd·¢ÇhiÄ¯öøÂœ3|‘²™W˜y†6ÁU{èåêAlºQk8.OmìtX”¿æ&wD‰³\ºœÂj-JïÙËê”Ã:åÏŒ6nYQoœ4ãï¬æO#¡1Uimî±ª×V²4\İ!± T?¥ ŠBn¹mÉô”có
Ú¹í™D|éß"ÄGŞB•I‰½ˆ1ã03,¦%Üş² ©§9IÍ²…>’¶Ğa¯;8~+Œ—´PzY··ï~=E/wkIu&¨8Oñ©roö
4Ì¿¢O.Z%I=£?Ô¸×8ÊÖëláŞåû]:f†­ğ
™Êd½Ø–!t|ÍŞ<_ÆŸÒ›Ø²ú¢7ëªõßğ…œÿnW=v$1aÔùT»|Ñàƒ
Çä™vÕ%:6»r•)†ÎXîšÓ™Ÿ=#¯ólÅï=³]ÇœBÚo‹×lŸ=ãü)]TÍ³ô 7%…ÜbØŠ`tÑÿ‘ÃBQò°
¨¼°×Ïft£Hvã8á7¢&>¡~·Z˜O{ÒÛQä¶6%lA|¼SNàõqâÛÓŒ–®Ö§ÊÚLè“²€qŠéõ]zÜ·ë&”?TÏ™÷Œû°?j?l+vš¢CpJÿŠAö¹Æ3Öô!Ùçz ÎÊ%…Ç£’£¨×?%­¼ £ÚR
8nxõ[f¿t/ºòÚlãàQkHı­Í2f…üE=?‚_u¥ q/GıÊ7}R=²Õ›y&µ™İ±ÒPëÕ%¥V¸K]ö-·mBû¶PşÇeHoğpyYéÃN2W&RİJî*$cÁJj(‚Pìb"wk…¯±öĞš ¶¿N|´ˆ,Â&ĞxûÌ!DwÄ¦Z|­ÀÕ™ªlŞ£iyt‡¹4ÛYpŸj¸;Y±R“¯T™pZzüÄ©nÎUx·Y‰QmñªÕSFm}¤3?ÉP,æGçSív“†:Òs“å85+\SJİ¼ÎæÅÏÑW¥İMúÀ«Éa4Z§3¨ø.àÁzŒGã˜oq+¤§[“Lh¦¶‚U´ËN 2´‰Öo ËúÚ; Ã[¹wønÏc2\1ñ²/0ëRnàäp2E·²™]F—ç\‹˜Î&ûÔLÖ¸t¯“· 2ÎhÎã/¢TªÅÛ7ÊViúCİ¦G¤‹4õ>U–ñÊ§°¶/7en‘ïel`?‚Yuñœ¥+sæLnbáıvßĞsœ¯Ï
½‡*³gÛæˆvôÈ!³¸º¬> l–KÖ6Y¤Zu¬sè|•aÄ/)X“Ï«,W(¶<ØŠ0SËQÏ"oLÈ/fšÀJ=¥z áê¿â~–œHOCÉªÇKør¬ş<pÛÃ	ßúQ+½ù¬â†wÕnÂöN¯÷yÔà™e³v•kæTi&aVÕ†ú.³àJŞ°tN;¨J!‹Ïi„-¢SXz}´¹¥\Ñv·¼¤TË©Jpàí<ªÖ`¦’Ac™,ô.ª·ø[ç&şÁ”ç¦ÆK¹É7İû)€È:'jr¾4ßš¡[ªÖ¯`5†%k<ëÒú=ä’8”«zåñbÕAŸOı­Ö ü)zšv²ÜŸŒâË2[ËSÿLã )6óu·ÂCéÖ³YRzy0+ß“°pwé/R„Xw™10J1<¡Ö~øwjÕ¿$a¼ÂM.:ÄE'ãMyÑr:uW%.|`ÀÚ6¶á]}W¸5r¹|×û´Ö4)Öyvg`…
::*Ê' T&S—h}<€_–®5Ò=/Ñ-.‚â¿UJåfµ=(ôëU-µèÚÔf=‡¿¯òKç!:›rŞïN³İHK˜ÒS¼'_E™ø7D¶¾™:ÔN$Š[ëóVNqƒN:íÃ„ª0êØP›ä ~€Ü¦-ş2ˆ§ü!ˆz¸Tò/Ùp‹î£¡ëİ‹xC¹Bÿÿ²“%GW™íí".’"¾:6Ôù5İJjKweàr	óB¢áGVF<‘óaİ“Ö ’Åi¡Qraß½fs—X{‚î5›¡Çót5ÊÔbbÕ„*¾Oçó‹lM	…Õ-}[+V0|?†ÄT”Ø¬8}Õ ªáJ„ÓåUª²ê*ßâß .3YÉ4pÕÆ}BØ\˜UÍ…¹”.bõUüĞ€¸+Ê+2¹Ğ¾âé’ÓkÜ2Ùqeİ3}wÌÒPç\–ömt®_&×¬CLnHó1
h(Ü}j®eJÉæè-ñFÎ„‹ÿêBÍ¨a<ËÃb 4> ÍğÎãc©nº¼ŞÎúº¥g|©Ò*Ñ¡	·¿°~… u"m1` @Y¨p:i\ˆ„e42‹Îê¦¢S¦?-0¤¼j‰µ@o®ĞYˆm“‹L_)tì`¥u~çgğ"d:ódy³¾}=y’²†˜_˜_y0OÑñº<ù5¤¯9q’Šò3d¡QğÔîºõk…6‹ 	?ÕyúågÇgÈıÖYjzÖ_Ğ¯ˆ)Y¦§ÿŠ&ñzrµz_&	]QÒ›e–ã~À»õiœVq‚ ÀKŠp”µ¥¹¥] vıZhÙOŸrw	„ésåR¬ùøQÀABàaŠEĞ© ¼É]—B±VÙfu'0erwâ–=—ÇÙênË*èªb¯’\›ÍŸHåNZüœ¥“D&£´ßøåîà%ûbò¨l²)óprZ¤ÁQå¢©úãĞa÷ú4'Ñ,¥¤ø7FdT–]á×ä¹ËÆúv”u)şzK5¦b3ÊyŒ”jm5]ËïeÏ‘€ãr}{„/:	…Ç¥‹¢37`G=<Œ¶j– Ëji¼OKn‰Ë¬	â]KüÓéú—ãaïâ„}rj"Óö1/ÓD»´~bæçÇ´6>[¬^>æŒ,U»RGşÖØÌF%#§‘~:¸©¬Û»!ûÄ³_WÉ×Y#­UÇ(5YççŞ`CÑüeÜ¿¡¼|ş2xõË[TW›uïË:ÕX÷ş6tÇÃÑ ×=o7*#%ËÓSÊğ9Òº«Õ\FR-g«ñ¾EO¢Ç•ú¸MZåØRÒôtù™$¢«`üfĞ½×“ñ»A|Õœ÷‡H¡à †W¼ûQ
(Ó™åÙ‚hSÕ%fÀ+™úÁ÷f†PÜS¿a¬Å€jVìRo³³Ì	Ü˜ü_l¶$hé”PkGßîûéü¿ğú¯p6ËC	‹bÒ/5}V¡ykğŒÔMÚ¼Ô×1E¼«ÊfA¼Èd¥BˆÈjMÚeÈgÏ¢÷	ŞÃŞÌÕÍ<Êœ€PJ?Øô,o’i4§Áˆ>§ë[4'åÉSÄÏ¬)wZ,‡Ö.31ÀPù‹¢|(¬ƒ•J’Ëc¨h‡KBÇpjt€ÛÌI…÷ÁÑa×g#CwZÀ%¦ss|Û)¾“›øFXE¾Ò‹mµû6+,=µÇ!îÛüQæçç0‰ˆ'¹cú<-!ãmÄ4lùQZVDpï$7M¬3ËÜjìÑû:ÃÛ¨j‡ø!T|‡ÑÓJ?Õí²ñ¤I¨RÑQ~õş®2½ÂŒ1jrMµ%± ]­™óı {5>¾¼ù6Nû“2£ûRôOıstyÕÓ	ıZ|9|+c
ÖUÜ&Rã‹ö´¼çÜ¹Ù*"¡°Ç›wZÍõS9H« º{ìp‡ckº³P¸êüg"F&¢I­sñ¿w…e;ÿ6±Dyû¹h×}ÈéhCn:#ÍZŞ¤,d‚è25¤\5eBÍÀŒlhöFÖ¥ò
=ğ­SmÃvÛ_~BËï}Ît?gOºuÉkQò:`Kûë5^^£> SYa¯C¦†zË¦Ö‡’^/*J*VjÕ«Gv}Q;ú)zı„%“†ÃI4Ÿínx„{%¹£²c%ñ&è±¹iŸ&v!CĞãZmÛĞ%fßÙWs…È`E[C¿¿áJ 
Ş¦Ói²<ºnß^ ı—LÌç7*?›Ÿ–?âæ^†„l=Î–ó;±-|ì	U˜’ˆ‰\4Ú™ ¤Øx:½ÑI¿{vùf8}¸ê1éƒ-HÊN§0Ck¬¶·Vv¨Pu9 PQşUÕ½’Dyù£ª
 #"Ü÷|²8}w3À:Ÿ[Ø€Q[g¥Âc?•tÑKeT%c`ğt$Léò.¸H¯°Bb¶İ¢Ã JÌÁòÂ‘‘í¹a•"nZ0õí‡`d¶t(ì1:ìÀ÷OÊÊ€t lóÙ2ìUâ9¾„q0"|ïØ¢*úeÍæá+t\Á¯Qz›0î¾ê’ŞgÁDŞGm§-.Ó»g½ñß@—ëÌÇß{ƒşğ5@wÏ®Şvğş}«QrÉqğ•@ø %9psBOw4ƒXŒïZÏ}£mh«H.Wd<´acàé¦¢ÿÇæ';¦ÿ‚IõàÔĞ¨?<è{M‚5#vcLÒÎ,¾ö&8ÇÑ¿5YwşÀ!ë×VoE/û•üc»2–cer\ë Î›¹ñºU©–Æ§I*¹ÖÈ½1lº•òM[óÆ±P€şˆìÑP«£‘õ«‘¢Wê2TÉmÉ1ZVŞ2‚q%X+\é  ¯0èÊúm |¸ aP³ë0és¸)Ä“¤q“Ó¦"Xré‰híÑÃ6göÍP¿ÖHfõÂË÷ !`ß_	ß¯Á{atq¤…®6„-7î6õh½í/ñ¦‡t{¾kÎ[Øã?å~}‰Gwm2À˜Úw3O0Ê`;šgt¬ç…»—G‹Jÿº{vá-8ÁŞ¤S²2ÚÀèäG)0F:AìÈÔ”xßñ<^ı(sÛÒ ãØ ã»:¨…^›5E	4Ô´§ÓÇmI”N:úÇ&ÉïÆTô1æ‘–åÕë@-YUaSUh¼ŒhRÚ‚”ûÙÔ‡À Êâ%=Vy‚—ğ‹e «KöIÄé˜µ_â^[3‰@ÔÊ@çÆ ¡ìqcS;(·EUá¯›¹Jñ›Íkûm#ç.ÎAÄZŞ` HÄ…T„ú™-Ñ£c³@çä	+¼Nı¯Òd>¥ƒÇ¦;úéĞ'ºë¹ËRĞ{À2¶2§K<‡*9·(Æf«åàíy†Å"^Nm'øwƒ³á*^–Ÿ;‰H‘âŸvP,#Ñ‹™¨|İÆrçcU#°’6¦pĞğM‹OjlØáÅ™å,3ö	Ñ3:£h=§\üTæÄFÂ«—ët’¦ëÂş¡ÎdwE³È>We­È€ CeÕ´‚Õ\bPhN^K‡Á<ÑèmˆÄ3Àv°Š-+Ìİ8#¹	a“¡»×ÎZÂ>öGİƒ¸®d9=(1è0æuZ¬ÈË»äfÊıä3¹íùZ$Å¾ÅVr+*ÏæpÆk6öû{Â¼$àCu~»;ãù…ª“óçıïsŞÀG|ËAdl¼.<Ó‰Şù¦c*ªÛ	?F¨¹¸åĞ@üÒÔ2“»lWG!‡õ_*Åš]=x‚ñëf±e¸ÎÄ™:¹‡q(Jşk®ÊØl2KóBÉ3f¦Hùö*Pk+¹ÔÇÈ€6™MÚ÷ähùúºŒCk4ÃüQ„¡­.h@LğwŒKùß÷©òL?*/ÍøDZğâ$µ9·*ñ7¦Kpî©Ç	w
åÚ¤ÆÈ[Æ'Ã¯ôqÏoõJìì‹cA?ízv—ÛDSzaïŞråT¼oiÍÎÏÄ¶Tñ“OÒ\(éÉän27uŠƒd©r—ggã“ş '®]¼»öF¯<¸À6*j¥åu–.§g±ŒŞ2OĞá‹‹!h1W„tŠƒ…÷÷`,Qr®BV©/‹Y•¤êâ–éÁÔ'ÅÚ©æ|€µSÆ1ÔüÅÜaW¾Áå}¨+F>û¼,‘’b‚&OØş(ü®‡	èÇk>Û ûXm=*mkã4`k@ÿi[ö=¹|ós[¦¿
c~¯x¦æ/äBÅ¥Öo9Äpƒ8˜é…8¨â»dgš‹´`›¿Ì»x8!]Å¸"ò¤ˆ.ågàÎNY Ó=ëFÂidH9[ Vo“E¢<#*[”‰ÀÉ ­WVY±^e€¼ ¬ıµÜmö™Î“ië`Ÿíâ6èQvV®¹E»B_:‹âZŒ÷‘è·#ãÉ•Çß.âó9Nqƒ
›f„„çG-¬Û¦^ğüx3NÉ~!HÄm[ñ	í—æY\Ş7ŒÖb©×Ö"\Ë”,rálGèQñfƒÕ^Š2yra™è+#ÆyüïÈão‹O´#t½[¥wÍa²~=>Ù!|½™›¤£+³Ö!ØWÌŒb³z!Ñ¤¢„2(°fEíÏ³ÂënÉô•§WÃû	TX@`ƒ÷>¹&Tô¶0u¾D™ˆoÆ
¤€­*¤İNÂ0L¶jQÒØ‚Ã±‰õğTQ†3PV"8Hş±IŠõU­
ìŞ
÷$Ì·N6snï¶#Î"ó“e’ı`m^‘U¡ô¯ MGË›|ŞJòo“dİò­~uÖ¿ïQ¦àñÑ»ÑèòB™ÑX™=ÖebñCü×m‰.).ÏÇı‹³ş´ú¾?:~«ërR—”³x³œÜj4e00÷˜"8^Â×Œ0ÀHvö}ïmêuÖùİ A‚ß-Íà5o|ê“^‘÷Íré5Ì7FK éÄ“zğéŸ"_âÕ]”°ÏL;~‹ñ¡‰SäeW{Y¦^Åf«)š¸¾°§TÏ´
Kƒr-–aZ—ùToª²_Ã6•|)™®8œ}6HoV»H«´®ãTc@9Ê0vL?LÀÂsoædñ¤m.7E¸zI2-TûkLnGª	åÉ…HSrÛómX‹pŠU{øôùµıÚé ´n˜ÄE(Œ/“Üob›¨¯ÉA}­F£P¦ù8ÔáÃD©x‘É{T®íîB¡ÙÉ7ËZ@¹»Z¤(_)(k$ö„Õ§|¦^…FĞtÍáC8™XP¡©PY$†WçŒÒ2üœ®'·WçŒÖByPäUÒ_ùş§¢şÃG‚tàà˜”]BPms††-i¥›ÛuÇ·(‰sh²Ş–‡ŠÍöòˆ›læ©”V—È…fLG–u|Ûäv>ÊŸÿ,[Ş´ËœÇmİA¾´¼]üXôwL§ÄkĞô[“µÅ0=6Î`¹U?D4:s„eWi2Æ`íÈ1¬FP¯¹×	’&lq_¡‘ƒŠÎ •iQ`Ã±qá€ŒhÊœİ®Ltû$Ê‚áùÜb‘îyù–·XnªĞjkÛ*vvİ”†z‘ƒ¾Ê:†ºë¦Y,?æ…A¥)AA•=Ï(·îô×MJ.Ê,aôş½qì|Dpd¯Ï<ˆlÙ›¦äŞ#l
Çó¬*Bğöø=Ü$OV¥ÕPøøm?UwÇ‰òÑM6ªÉAıSt¬‹è"yi‘GÔòp¯£ˆ:Øš¾ü[sx%Ó†¸2 7Ã2úŒÊ:¯Ú«®ÏçÙç³ôSru›­³‚Uß¸¶ÔE5Ffvì@½8œ^¡Y6Q÷”éö/ıi\ZËošxÄ™oÉ
¬«$O³©ô•Kf3XàûÓTº…Qùr–Á¶òDw£ª«ønèzÚ™OƒÅ¯áÒe>;D ˆî^1\§“IN9‚¹U~ç›I¦d™v³uº¤LpK†ä’‹…lLî;@¯Š‚µ@:‚Æäú^rÄ¶¢L0-ë÷äª~gÿ|)âù¤i*ƒqÓæA…pó½ÚBî..bõZ§¾û¾I*AGu¦TÈ%L"|{*ŸOå›(ş}­/åkqµÂh‰Æ»yn¼ÅúG¾ÙË|J;»íÛ¦š•‰èFŒU§Ùˆ–Ø“·Æüz³×¦uî4J0IÒyË/½9EÁº&„®ÑV0f®×M 1‘Ø‰é²u°ßxÔâ9ş7€_Uœ>õ”£Ç8ÔJOZš¼è¶ö:Z)N¢ÃJ]÷uÕ(yä0] !¤hĞÄ˜©îEO„;Ãn’MaâƒñlÒeÈĞ}Â3Ò|ˆp†y™0½/¿„Ë…î¹ƒl¿5{,Ü']¼L¶¡§Á¨Gè¨(‡¼Añ&¬€pÄ›•õĞ IùzP…ÖÌ`å‡¦¸•ë=ÅCuáÑË†¨	çCk@ßmø”é+sî\	Awj‹ŠÍ^¦œğ1å 3¤M´®ğ_¾'›:bîÛôæö¯›xÎæ?ú   ÿÿì}ıw·±èï÷¯ tÎ»¥jš‘&íul§´DÛ¼ÑWE:i^_ÏŠ\I[“\†KÚQïÍÿş0`3X,E;N“mO,îâc0ƒùˆ‚4_e7°»\˜‹Üèc"D`2-÷Äaß°‚3:[îçËhtG’t³¾İÌ¯|6P¾nÀ&¹7ı¦Ê×›òJõ¾{Ò«®ÿ€ºBLœ$«)ËßŞZkÊâ°á²B¤r²ZS%¶é`”‰CÂ:™/ßLF‰&s”,•Ù›9Aêee–àì®s¥¶>P‘Fô
— 
±»Õo44OŠ·N+…dMMZY¯gÎÂY7ñ†Ó¦Œêª|ßhV ÉX§½ë5
ÿşÍoU1€zô±‚HdÖN¿tÛ§‚&‹å6)PÈ.óLiÎ´A€`ù¡A“èæåN¿xÕ›£\Ü ¶jÏ“Ò\\'RîG;:è¯I-Ù+Ü›°Úó©ß5´pFÓC¤L#ÏÖïîsÍf¥JF¤=’TÔ^V²ùá¦­NË~­ÙC$òèáÙMeb0…­ÔI‹4…ğŠbM~w›¢7^„Ò–67b)¥;dåŞ:g
”l>Z¶áZzÂpõª¯BÖJ§c0œçZGV¤¶6í·*ÉÁòW¥Àlà®jªÕş÷]İ-ê•	é×Jk‹+ËT‚buƒ!ØJÓli˜µ9d(É'†óÄD:ùP³v_/ãˆi£	ã×2Ì’½÷şš€Œ¢y‚ ¬nkjIª3æ(è¹EU²ãCÖGPÖW?”‰5<”@«\uö€6¦¨Q7‰·â×É„Ì bğ¢Èv¾çõµº iBŞ×ÍÄ¢RwîM•­—Ö¡«¤JlCÛÜóã½8ÚŠ¾K“™g±b>Êg•½ñ–àéPÊ;4C˜fS‰0~wÌØçÒÄŠ@h
AÜFp ¸¨Àöß¨Ïhe¸ˆ£Í	Œ›KÓJ~û&Ì’ ğÔü63†=ıÃwhL<hí·8†E·¶‹ß¤wW9d\Ù­}É‘€h•€iäİd0CC5#‚]ƒU^ ©»—™^-¸(Êòˆñ~¾€]_éa²ÒÙã<Ë¸nÙÆÊO<_hıª1Eõğ$Çm@¤K¼U3¤œXõµ¡7Yñ]Ò5u"íÙü…p›¿ïÏóf”‡‘I×çÉ¾ ,š§x¦ ‚<óğ¡2LgFùY.ğ¾`ş(ÄXe*Ek}Z•RÄæ`â+b~Ôò¢ÌnôÄàù]‚]vàKÏN8„'w|Û¢Íâ|ñf …Ş¶ô‹Šá3–ñ ñ@ŒNBÀE67pƒ–š½+µJgQ;êûXT±ÆßqÌ@¦úßR@oĞ†ã”ñ¹ÂòÕüuV¬óÕİ&¾¬Hç]ş6­H©Ô
ªÒ¾ÏšIÇFtÌ,Fv,ªS]«Svëu×h(ærˆĞÍC¤ƒó–©W±Â	ÏAVT"—œªv¤æLÆ^—‘ïıˆ÷åŸ¢0Gğ¡)§š´V~³˜æ.oD³{õ¾¹²®‰¼„`™Çô‰ÕêÌ–G'ıŞe‡ô™ÄÛ½¥™iRMB0ÕXÌ]§ÁB€ïòïgŒ—˜¹h„Ä½†ƒuá®Û4šFÅzû Ì/ ƒäeÜˆÔút'BÌÈHÁÁ«Bf¨õJJz”V#»°›§àÛ,0j×ù:™E—L&ÏÏ¾ó¿•i®T2x¹æŒbbË^i×ks5J¾:µ>C±Qİ£‘ãŠn?tiu³Œ€Î9IL‘+“weT”jİ"|a¢&ĞÈ¸€p†F¶$‹²ŞæùRòLqˆ[	‰w(öÑvƒaGfÊcÆÓœL’»]¦­äÅmÎ™¥9šV),0ÕìC®]7ãu€ÓsnÍşöDnxó¡Úö³ÌT»º³­S"Ò£0¼º)®e„’‘zNk8Ä0-~“åñ3½¿ü[áq{ä¸á'#Z±%+ ìHQ€êÜâxËılÖ+Â²úË|6•Zb®]¢¶¥l¶®iI• ¥Å¾õ Ë€ÁF±“0§Zœ•_­Öşz&(k³m-¬•f´ÉËgª7-h…¦ãÔŒ>Bl…'	áà6ˆšùj=\Òƒä.ÃV.Ò™ëm#¾á{•kı‚65é{­p›Ä­|UB!UÀT ÕèµƒÂå«ó³>{(dd§ÁBœÃÒvuS¾€ËŒÌö`›TŸ©Ï¡Æµ7«ÒCbÖ4//±éLJ†LlìG*û1Ärj»G]ôxiŞï•®hÍ… )âY)˜Úìq×ô¨ô/kŒ¡pW¾T”J'|š	€VÉ!NÌ'IñÄÕ`ÒìE¾u8.'½p-†¢IšõğÎ,_ëÇî$”Vù’‚³Mz°ÛÀÚiÅáŠ5¼Ò‡[T<c#Ç+A3¦œçN"óÅQÄ·Îonf©¨‰r &·`Ozõ–aíÊb•=ƒg8ŸzÕ°õ½¬ ¾èdy5ÀW¦ÎF|ş”)µ °Œ°İ+ÊJÈ]»2Îì&u… +;?03ßZ7+Á„ªT†_QşÕC¡{©ãQY*“¿™ä|z~Üû½Ë£×.H'b¨ÉÊŠÙrb}{ÖjUÜ:
÷€« +*#öµ¹ÄQ6¢åAÁØ¬q+[Bƒ
‡Ï'Ç®„µvüö°ÓƒÈk;é˜·ÏÒqš9ª¾ñ²„,Àê+—‹§?/ÇÒ²ã‡²¥VBÏ&ˆ˜®áÑ£Ø&Ä)ÛúIÙx +ÙøÜP„µíğÙ¸4×È©dé$«QP]}1¦ŒĞsYú4#;/¿7€@Ï º0Ó¿şÑ½‰Hò!‘e—r¦sB,³¡m±3ê5:Ky°Ç5ô	]Ø¡¯Úbãç>he´®ÇèÓ ½:>£;WÛ‡GğÏéò<$e˜mšÚ´c|¤WxbÓtéî#&Ë Âº¹2|ïdªüÅÂÎÕu`¢ˆâáyr@ùô¦©d“!ĞÀ >*«ŠSŞZ‚›YwÖª€è²ß½QœÁ¨^§ÙÍíZ^BÊ¿_æúäicÄlNo6Ÿk<Ô˜Â˜ì½{-PÈ¡ô÷8Ğ]Óè¨¤KO»f%˜Ãè(0ëµïÄyNf‰¦ÿÓ2Y`döÌã”Ãh®`9r’C,ûFN9ó„1_·.»ì*¡«ÀdOü÷PQLØ×WV\N¼S<pV}-¦Ñu”m"¶Š÷Z¦Í­<ƒÄ¨ÈIŞ,à4ú-äÜ.ísĞ–¡k áÍ$_¡ro3Í¤w—‚5m¹W¾[y1·~Õd<€·­ 
4É0™î§K‰[±Zôe¢µPø–¹XöÜ2çÆ—ÁuŞ6­'dRKc‹1¹3A[Ï·]hÀa(Z¶úZ){éf­ÂŠá(7–ıÓlOµ½‚_l²Ù´ûmÿr88?ë¿ÎF0Õ?ÜQ[˜5ÎÄy‘>\Ãtv}‘®À>ÄpÁÆ²ëTğeù®{Ù?:¿<÷ŞÎ±“‹dò®kË»è_† æøÕeïlÔ?&³(›œªÿã÷H>óş§šŸ;­Ïï‹kUØÃ)
\8]{j‹SĞ%í;«Ÿc[FÙw•Ï5™©ª4¶ı£\l¶.»Z"–•bßĞÓ˜ï¥î¡¨‚ìá¼bĞ¾h³ïğJşÃE<4‹•iBÌäKn²¹Òd#]LdtDª;Z¢i3î'yâû‘‘‡[=±„Û©â9ÚaºÉİÌ¬VK‰ÊR+uôºüæD¬'Ÿ!
 š•EÅ’¯¡øKôaÑ—³Œª&^H²ó›Ùg4·ğ˜¯¥)¥ÑPó Ù3>FÑ¦!•A_²\Îî¤Ñ†î¡Sš)+.~ÿ&½ç2ë2TúıÀW¼¼%G¡›íš¼FŒ†,F[–¡‚ *_oH(	Óg."gèÕ¼tæGƒ
­“)]éÛ3‡Iæ[:ûM`]–/WÜ8 Õ$ÛÕï”º[pºÓ;TVOÖˆÉõİ2í„Ä!s0fşÜ…!1€ú×K!¢Kp9éT!ª9”¶³µà˜q»›a\ßç›‹Û|‘mæWé
+ĞìÖÌ@‰5y05]Æ ÚC0‡›U:´¦<Ü_ˆÜÙçîàˆî	³^!ÆMÒQrSø°UiR†ÎX-jò‚gá%vàÈ•²×öş°5TYEõëìF…W®R(´#HEuPxàcZVs²ğØù]íMNŞcéÑâ¡¸nÛù<ÓÕèHß¥Ê=)z¨>ÆÔ¥béŠÖieŒ½*,…H½C•qRÍÀ8™LÒåZu$$ Õ²¯)IéÇEeÔĞÔÚüò¢êGF"^È¼KEyi„F *“H[ô×Akù¥("0"|¾"°bbËÄøÕ5w†âF­üèd ‰¢­}ÈäÏƒV?‹4–‡0Ñ´´öÏq)­D‰àª¶üã"ïo\ q§÷©,ÏEn¹«ôB.,Bs}ƒıÖˆáÓsTÅÛ¬Ëw­¹€~Àx“ÈSÅÆö)¬ÆpÁ®ô3½ıÁÖÜŒÉtT}fç’f­º©E(ï¹ËyT3€Z«¼‡¡UG>ŒÓ0n¹éÀd¾‡–AğN÷	Ê2·P9{Ê,'ìoÉ–@/ÔRù¥œoµj¶a}ÖÒìÛÁpğâ¤/Š‹‹UúN¼†5ÉÂ
XfÑä63Ìş•J—…t%dÖyªNà×ÆßÏ0¢šGß×nJ1oTéâ»óë£[AP*a²Ø- ~Qù>·Ñ²)N×^…Ÿ7îğ
-GHy÷¿ÂÙñÌ%Ó)¶ãuÔQxx á-YZGÂå„Øm[¾»ì]ŒÎÏFı³Q§øôj•àÙä¤ÿrÔúßòçèü¢Óz$SÀá?´ø˜…õã¢ÔCJ¹7`áÂª™»hå	jdF}ìw­5øÈ4Z_·bXÚë|6ÉEšÌø*¸8ê”$<{:_Ìîì©ù@‚³
©lîbé7èk©Bå’µà2y‡f±ÕøÅ.¼‚téÊÖÙœw¸…¡«¦ P›—x+l×teÜHŞáJRI DE’Ù[^)¿LPññÇ8‘—™;<Øğì-€›à˜†vq<5§Ï<ŠšïÕ\2Plq¤:ÿFË1D‹*óA7ÕŠG8]Q"Ou‰ggv%œ=ÿµşTH	]Ìr$Sö=<Ê°)ç«³|×Zµ5švİKDãö–uı~ª³ÿ¥SiôêŒ)îuèd”/£{Pekš_TˆÙJ;Åê`ÎWï“Um£N±šF—>¹¦E³LMs°àWuíY…jD]Ôµh—ªiò
‰äUv½®kÖ/Õ´r)ˆkİ)\ÓÁèM•×µí–cšåøµ%_Væ¯kÆu"9ÎÏtQ-ä±– R
CöÍeÇ
vt°-­»íÍÁ(am5GÇÇD«Y-Ø~4IÀ¿ô¦Ô‚ÏŠ+1&ÔÔi>Ó4‹7iòDq·É¬fóF¦	§PÄ)öÏtö´¯øfƒI»½’İá«Á™&ºšŠ*—Ôñf…QÚ:ô²
†òˆmµÓù¨Gêÿ’GêÿbÔúBQ€èÜb)Ê€@Ëã%FGl=o=şSë­/ñ?ÔD‹ùe×ÅL`m¦—ØqrWìwZT'ŸµÚV/Œ‰Ş³]€ö:ß¬°m	Ö} :Í›uÊÃôåa IoÃTĞğ”é¤!oĞªç6p³Îe„aŠ½X×îGÍn¿â:5Øj"vÅde¥W¾‰'´RÁÛHÍM˜â5Í0ŸÚüÁšÕŠi?À²ËºQ~[ˆk¿ˆx.Ô)w¼ƒº72bİ¸~S‡îÎcÂŒj[.)¨”Ÿaˆ72VŒ¼Xézm@ÌŸóêâ¬°3z5Ñöz;öõü—V´,ov.ı n5·Ø&:-ƒŸ×’†…Ğ8ùè$b]%h_»şñ`4~ñf88ë‡ã“ÁÙ7êĞK¦XB/Wù®Š†i
®Ã»B¬©® 8Íf³¬@ËXšl2ÍôÖ\ï»ë”êKæ7£~oíTË WO[ùˆëğğÄªSøSXiåèî¹Ü¼vÌ˜v¼ëì4K^$iÑ×4F|V­(04%ß”<WŒÄÊ^gKsa:Ÿ¼cÍ3çX=I÷¸ÏÚÑ]VÍ=Và:²9*3œ£79&/Ótz•LŞ¶íŸ ¸_İoúß¿8ï]G=!Ñr¥^ô^¯ÎÎ/ûãW'ç/z'ãa4œ½²d£I²Ü¶ÚıŸÀn¼‘¼»¤—Ù,=Éoºi›	)'=‚l‚`>^±û_á¹ôFg ÌGÄÅŸÓ’uí÷IÜúY/—ë¿„»x÷4ÖşcA\WS¨f>v¡,à¸ó`—ZòÊÛtÊ‘¯¨†÷îøŞäÎtÔ±Üˆ²¶ıYe—vÀœNöì¨o “.%nt8¢ á`ŠWj8’Å%ÓpƒÑÔ‹»y^€úm»[qşÖÁ8Õ<7ûÆX1î·Fsì²°„1à9OEköÏÇ—çoÎ£M+?,,ßúµ6ˆ^´@—Rî´/îÓëd3kÔaär‹¹{5Ë¯’™Ê4Zá‚!îEÍã
®¢5”S!ê÷>ØPP,…à†oÇh¼Ï"”Ù:-x<<+$Î^æ«ô]eÜé÷İˆYDÙåRøQ`Ü›CÔ©U¨¾-Õ
U Ã©† ‡€\ÀÁã¯»Y¤ã14dø\íç(^cè¬õ"h‘PŞÙ˜w X;©ÌFÂmìKœ&A/†ZÉj
BGÍ"_bÓ¨p>€ŒÇ&4Æ'ÏXÈíÂªH<*p®ªÖ¢[œÊB*`æ‰~ÙŞ¡xaŸ„Ó©>¢–5ŒsÓ–—Ì]3&îı2èö’Ş÷²ÆæËíé·a[è:âXRb|ôıE|v>„m2|TqçŠ¸@ytèß Pg§©_ì¬Â\§0£eo¤ˆ²1wR&ñùÒİ8
HOè‡oò¸f©É‘.&Ğ×AöYßkËZ™2bÏ¯Q:Ñ&ñ©X\d|=25¹M¶N;Ú®ÿ~&…&³	¾İ`”C>Ô6¢:©~ÄXúÆU¸ç¸OÆ| I /ÓIš		¢•Y¿<°1©Öpaîa7!Šd³ôx•¼Ç|	2¸›ŒcµŒû˜şHß¨éF£ËdRå“q±ÊoVÈÓwËÏaÍo‰	‚ië=Ñ˜û¶µ/òqñµ¶"Ş;{—\‹ˆ/ãÚ~Q;°<—†=”"“öÖ‘éöª¸Ew"÷‡¢½Öÿîèü´9¾î{­QÏ‚“è“'èÑ\Ú‹-‡ÍN#beŠK´M•¼±ì¨î)/8ıü6ÄwĞÊïÚ|ÙÌûj—·;úÔ’>*‚{¼_ô÷-şßy‹?ß¬1 ˜Ïã}b”ûl{.Am»ëq»ŒLçPm2;ÅÛ]v¶*ğµ]¯;µ­IêÛ£Œİ¤á†àZ0ÿ6voÔÈGÍ÷§›OñÔçš¦‡v¢lÜ–À6à³€IêÄÀâÖèeÑLšD·Œ³á;Ça²2ëœåjİN|3+>Œ4…±Ù¯uínÖ42Æ•ê.r…M_ıËßÉÛ¦ÓƒU!#!ÚP¡×.ëí(]hD’å‹Y.Ø»	ı^¸(Ÿók-;'G?² J‡»{Np°‡ŒúEÚ&RĞN’¥Ôá»{f§5şå\)|øZŸ}†Uº³tq³¾Å±=><¤æÜëÍÕˆ®İ•¤,„Z¡¨÷õ»¤¨§2d”o&·ß·¸2LZ7yG¹é²–¨Ñô Æ8kˆÙ8l‘zLC“]gA·kî”><æq¾m{¤È(»Ê×˜CZg=Ü¬dà§÷Ùt}«^—é¤ƒ;ğ-F#5^s‘Šl0U“~s~S¾`J Ş,®;hĞµ’Å»¤€e"ş©ÖPEw³éèà6ˆU[¨Œ-¥îÑ9-ãÈ> WÌ£/¹\nĞbº˜†®“ôR4Íü‰®éöá
o•,Š*È¾oC_[øÏ9”ƒÖ[íG×â-ü*…˜Z{Öö.×$.­_Î”rFp×Öv²Ô_VBÓ‹óÑèüÔ›@Šê´ş‚b“øï_h×Ëã•vx.Ó“ê¦p”ŒP‹ +1–õÁİ.İ¦¼Z?2{™kÓd«._¥)ŒıVü²9nÛú&ú0³pbH™u{ß*³Â¬Øu>×	Y¯ œ8%f›9F—E
@yÿ^ÉEİÑÙg*§¨#Ul©}L5A–±:‚ÚÅ0’ö’ÙÃKÂòx‘Ë\_Ò'ƒ¶µÚU&#È9ÿ*»†ü|™9ì:]»~æ«ü áIˆ“êAZ¦—(V<ÃŠ5¡§ÉäíÍJYå›•X+—İ©º¸é
3ÉV´Å®Z¡ÎÑvËqÇèGJZÃã?—ÊRñÿÇ†áü‡5kMÜÓøUfÈVîjØú˜hŞæ*.äşÓ§ıÓóÿ<¾oH±U¸Lõ'ë¥hFÉòµ`1¸É˜v3æû€èp™HÛvÙ¢`©9_™,Ñ® t?¤©>êuĞÀ€´qÉ=M”İ·W{ÿ'WáWŞbµò×³–ëCY."Áò§.iÒo1Ïfi1^'WcùçAw¾Yc¸.»S«/\°$0¶³ø?%â=><`?Ô5~”Ïò•Ì‰Á°/ò•øóxs}m~Á r‚P§ø²wß¦wcŒc|“]CæaMş˜ª•.¨vOßœŒ'ß“à ¥„˜ìorñ—j§úĞí^å
=@¤Èfôá¶#gV0•áEïlÜÿûÑÉ›áàÛ~õ—ëø
GceİÔ	fáR›Ñ:~¢b®<'kz!XP5Gaæ›S®ü—´Ôé¦-ëœÀ¸,'5+ÿ:I9ÏBÇ™şB°›Œ·W&k[[«È¹Õs™¯1,ÿ(~+èÜ¤~F—ïæ×/gy²¦ƒ¸í\¼î	@»‡×b?hÙ"Ó½@5¨òÓ…Zú›~©2xlãyQš3ôÓ¶n¸2Y Ä¥šës*ƒ²~0¶§¿~ˆ8Ÿ[M:Ó@˜ÏöÄà‡;‚'¸mÉÃ|x‰~îM6æCÓAD¼Wı,+Âë‡zL®&oühÆÑ)âyéVİ‡&`ş‹iV?&Qt”&…Ÿ{…dÌØKéSĞ#ÑÜNÙmùñfè!åS¿*æª\ÍÊ"¨È0&6ä¨/fªiÄé·¦¤èS¶â¥ÌŞ DB¥˜ar›ÂKk>ˆæqîUÇ.7r*WoüFÕºRSå]¯™jC·ñ±fîZ¸§ZÃi®‘Ÿ‘RIÀlÔ`Êéæ÷c‹”T=´0‡–N£sA‘¿‚`×q—Ì¿ÿ¨–°~k H‚z¹Åáç÷sÇ/tîğ¦oç§2W–öm]l‰©Ë1ºğ¬ì®b#iGè8·ú%zZ‚Ü°gyá¦)&ªÛV™3Ö ¹ıcNéz³dLÊ¤ü‚¿…¤&]ˆİ¶GwK<F# bR}×?3ãñi8ì½êùµƒ“@Ä)H°Ûé«D¤;üàÈèD¢çÇ şóc?&/…ØÉ,/ÒoÒ»«<YMÛ$39ÅÊìZ¥ÄÙ«hsK’Ğ˜„°éÉOÒ Cœ„%îùË0˜ÖÑr›½é<[\¢g¸WÄô!/#™¥xçäÿâà=ä Ş¢±‡ÄèYB”P«p2èOµÙÂ¶; ¨úHœØÕYQ¦Ô•Ñ>8ƒywdå@»2çèÈ<{À]¢ù»ªläÏqÚ!Mr®+Øú0pV[g“·¢R«l00*•°ª)¶-hnS@DiÕ˜“ğ§†¯ÇÛE6åz¿J&»şİ4ND=Ğ@bõ—+1£/“Iz” {rİ%é%£&—Œ3¡M—M#òÆlBrË“TÎ‡^wìäa Líâ©&ƒÑ™fÅß63…T±ÎWß¥³I>/Ã?©HRU¯J*òö¿½}3¾ì_œúÃ»-ë¥pÚ‡|Ü°0ÂI#®â¦”5Äf!¨İî*O†¸•Ê³MèHBVPaÃŠD¾BÂ÷Èu!Aª=¼Ò€™‡‚G_xÈ:Ö‘—9Õºƒ±µ¨Ùˆêë>gØ€uó.Î¬–c{7ıq#¸!M/¤s]ít²÷cş)Ô1í›y÷|éhß™Œ.‹wÉ,«k½ÕÙà‚8­BNèTpØäN	|ÅkX\™…å%‰
MPÇM<n@•Xi¶~¾Š]=]`B‚½ÒsĞ,bÕ¯GùrPœåÚ}É›bn5;û˜†Upşâ´UÛ,z¾Şış!±ğùşî«À
+¯v¦¸Ú#Nuğ²œQKùl¢	…?àÊm[ë8¾JÅ1Ûj³å;§7Ü`Í>"©ÅÍ:sşä‰_Ä±DS¼¬'›4¯°ü<D¦?”ewÚHÖÂª„-»F#iË³Fær@{Öú6™mR–	–3…Ä¡ä˜–#å2§’à—21sÉ5“Åœ¾p¡!„È	øå@p¾yÖ'â8©å$]°|Òt‘=‚_¥¸g#JŞó0ÄÁóøH<×ãøƒˆ–.õ~º²%‹ÇFÌóÀã‚~R9òı”PC9¨À¾_OÑïg­Ç&Sˆjèû¶†ÅúfeD-š<<¿¾­·¶¦ËöŸ *˜¿[xò®+¶E®FN€Y2_QUgéjé³BÙ[§õß)´o¤¯!Ä5şƒ"¹	3Kìö¨†ŒkÃpkN£õµËúI‹Ñ`b² »IÄÓa÷‹k9Ôx ‡ĞÀßÛØN|…ï­
uç/_Ø¨;€q;à†UjÎaXæ1z£¿Çqì² !)ò­78²m/x­ÜG.€§Á:šìPF€çS86<L˜³r”àÙBnğzü¸'QŸò·8ŠÖ	$DÌY4ş0è>î4‰kş8I±"û=Ï­z¿@ß`ä«|./àdE{ÚëNæƒ¨j8×nƒ^L’ph7inL‹pe'Æ˜•	“ü˜o~¼76”Ñ5ÄP·Y àï)s\FÜ^‡[UwÅ›•T©¾ÈŒ2¿¸ş¯{»ç¤¨+ê2Md²˜I .S-QÖÈÅá¨ˆ˜ÈµâPÕ°Ó	§»´Œ±*x‰O€¸d…GcÜ,òU*/óÎÉ¦HK‰él’–¸ä0a1»¹(H;Œ½H|x™¯ÔÅiAİÊÌË¢æb‘Î¤AU¡L€k'P0£rd	¡ÌÏSÂ`ç€ùš±JzbLÉ}FÏ]|z©®ªV}A^Z>­Ú]t2¦ÌsÎ¯˜¼1pã%_1‹'y2­Qñá!'ï4Yˆ¥di÷–@WeÔ}%	†$¨ú_)&u|Ÿ`b8Ñ×+ˆ¨v­üFhüïÅdB
O ,@Ó¿9SPòe:¹lUPÏÎÇçÃ$ùÊªa­¶­û®ªw¤¢Ÿ{Râ4“!N3q
ÚÊüÈ§P©Â±±O%bË(D\€ÔÌ½ø)èUã{á‘SŠ)òÃí ]è>ŸzsÇÊ5şëVb€Ô›é€>wé„ƒÓ'§&`ºõà'ıì­Bcy¡ß¿vtK‚%X|o–—ù{À üÕC´şªéU
•ú,™©JˆBäK¬ÈŒĞÖ©Ø¼†i²S.©¨k»Â‚`‰HÅJ^â€®>…âº®ûŞæ!ë|ÌLbâ„'ıÉ±„¬8´Ûh}Ö72kˆ®Q#Ìy5Ì
n¸qÇrgØï]½¦hĞòCëŒ>}æÂ
Eb›Úƒ´pB
»M§ÿ8ü]½²24¤~ª-  e rø1 G%æŒõ:)n×É¤ZF>v£KXXÕlË_¢kõ¢Óšˆ‹W›lÚiÉ`ÊÜiYàˆ-¥ó81î&ß[8×üf'Ü8¢/î ‰¼MºGÄº1.&-r>Íï¦hÀ7†¹Ÿ§«›T‡£UÇ¦8‹/ÉOªÂ °€îl{@JßÄ(˜-àlT%½
õÒÏ@˜ç²ÉN¯Ü&?‰¶¦ğÛ!½dñxÏàˆU€ì‡ŸŞàıeYÙ¯[AŠA¢7ˆúÑ/AÔQóáLÂ!O’’|dÌÊ£7+.[4&¹{z®æF¡]EŒ¦d©’="×`ŞU è~ÌôÿrS›-¼©•Š#1éÔ\>úÀ+Ì™n=˜Yg}Dó›Ç‡Dt{7~H!üğÓD8w¨	h&–«|"àÔ> Ru%# ßf“[W//AM°àx	>”0J»3ˆ3ÍcŒ6Jølt¥’$]_¤«yVpæ>MÙuZ¬!a¸z×=êö/{¸ß_$“·ÅJGÒ½è_†ĞãøÕeïlÔ?f@^ß+é^¬«¼‚’îöÿøáx8~†´ Ä¼Õ§PğS¤Ã3@åSk¼M/²	hpÕi*´ñ7\‹óšN÷:8í½êz£7—”#,dü’Ù½DKÀylrYvy‘€ƒßFœÀ6jîKÂ$ğ§˜æ¢œôZ¯sÔı¿.{ãó7£‹7£îb•ƒŞEÿ7«ì%êKéh½år–Mğ:–¡ÊtV½„ô­­ıîR5ºß‘¸ãâFÓ€Öõr	ÄÕoÌP”9şîr0êß\ÆÑîªíË~ï8®éZAq›ÉÈï^¯ò9¢?ˆ5úVQ×RÔyép–®Š|¶Y³$ê·‰w„zöY\¦¤Oö†áİ‹DA‚ßµû?MRÌFĞòt¨ğÀHOò›nÚæU%Ænè2Ñé6æ8 X£o%úgó5ìòûüsNJÒ]D³b¤4Ì1-ùÏp{®Íõ³+¾íİiÕûvpÜ?ŒşER¤FWQôooúÃÑø4b1ô/Ïz'ãáèüR4Ï.=E[âŸËªòöH ‡^\@gÛ¯è,°ï–¼!ÿZ¨Àü×2ÇÖ¯Ò~ÂjA$ ŸXÙäÕaĞ3ÛÎÌnF`S€CZl{Kæ(aó3âHD1ûY/”Vô_íÍ®6ó¼¡,9§i¦D(®\›ûĞöOúG£1æ=îœt*°ôÁÀ¼²ìBBM‚!7v×‡GÃë«o`óãûëG ì;ëç?ÜDÁsÍ9Êa~İzÔz‚‘©RÚŞ7DPfçÚ™¸¢ .÷¡¬½½}û4›ÊÑ«±÷V«ä.ØÂ¥¼>àªp\C™.E´Áâ:ŞÂƒš WÍ,¹îNfµ)à@¾™¡™C§Y†h½`	»á¡¿ïW'!4o‘¦ÎF1Ú«‰–3wË´½Ÿ>û#•ß0X»‡(Z
W‡±Wı‘†Ö¨=^¸şo|28¨Ó:¶÷_öŞœŒÆ§½¿_NúXÎŒæÂ ÉG¡=š‹ÁÑ7axmUHE©=ŒTÕıä6ÏÕ’Ë€Û^Oj›Õl…^õ[¢vp6Ä^>ÀùÒ`…!;x“ôs½ÌYÃ£Ór½ô{ŞĞî([¬“$œzËUZ€¶â|mÍéh0îAqõ+Óñgçz¢(Ñòü¢6Vƒ¢tª¼DIñm÷Í´Rhö§“ÂsSN
‰&¤“ÂC)ÙÂ‡ÕH£şDôQr±ÕèŒL°w«ŠlyÇº¨fÓ`i¢‚ø¢Ocñ=6Ü©ÎHµ½sµ—1¦Nëñ‡TzY÷¶-&¡@P…s”ç¦÷‹X™ÈÉz	¹a—±².òÙCd¥m»»õã&û—	ƒÈĞ¶Br‚EÇ0.¡ó6vÇ@Ëï—˜xëü›ßóÖ5¼ïè„K*}«àmeÁÑÉåÅQWUú‹µ ËçºşÌ ~UŞ:±‰öµÙGı)Æ~s™.ºÅÎ›åp÷–CÎ£ëkÑ&\Å•¦Á1pi§Ê,“;pP±¨CŒqÖ×çIQB`É´Ë1Ê¤4£‚Bâ|øò¢T²ÊíËÅ”İ‘>FÌ‘P½Rñ—òŸgQ¥»ùu5@ãÕt,ÿ•
oZı"Òä€öIĞ„Wªv* ²^İl@*-¤.ÜøpK€)&©Z“S=ÃÂÕB5ólM`xªşdËÎóä·ŞÌ!Õµ„Ñi¾Lğn$Èg\ŒH¿	b.Ã¦peñ¶ì KÍ%é¸dï;z¨ m»ct¤®ßÕ?>@`Fù‹Ò‹¯¶»ÿèZÊœ«J…rœO$©à¦ĞV9Şt4ÓSÿHXËuÂ,cØhà*§p#Ó† z’uu…¿mr`s”n«S²E›%ÒìP6Ë¯¢Á+-ùf#¹^§:—=Ôó#ÄïRVMV»Á4İ<dÇ°VÛ‡İ’‚ì×©"$-µŒ=1Ä«G»C‚”+W-ãûSgeSccBº:jW¤ó•¶cH¹2¬Ú1õ†(WZWc%UÍBå„*óp*¹<\yÓ[ÆçÑL­Ï4Š¥9­İ„Â·®
g‡Õ5¤$ƒå’o¹.¥¥ö‡Wb›ˆU+%ªf‘öb„7¾	ìmRœåôXéÒ”H—¦¤õÔ™ŒVâû0E L`æt+ÆâIœ™•Id®s!Í²E*O‰¥·™=ñ#CwÁÜC“¼²Å'm§v%ÉÛğ‘±Ä}Õ-“|Ó›#3<û>ÜTs'’í;Ò6ÉFƒõî¤é]Š™?Âë`oûuP»3¿ÈuÂ\­š¹Íä~DlÕhê•ÄæN·p–°jvVn#¥:r¤ß·7ğ$ÆqˆíyûoA³ÎSMÄ±Ã({h¥Ãt—µÂ²"B=°>Æ®áı‹ŸmîŠù›oÿš˜¾íœœ7VÙ±û¤¹HàÆùşm3£'.“›³1zpZ^?1Z&œfÜJ/]B…¦—\€ˆO0-C×»š`±KÙ1.ş1¼MSéá§œ Äş!YO~İE¾K¯ÀU‹¡ÊQFÌ !T }e]JÏÑÚ}d;Í‘ç€“lñÖWş‹]î­ü~)ï[•Ï—WSÎ/RDİûÂİ1ö êR­1I>(	`š‚b”Û´èc“Ì<€ƒ†H—Ô÷®'ó~‘®îJÍ%—ä5®zwCñ#ìA°aÂ¤É8*Ó¶4RÜ~5ä
¼á•?lõ‡™Ø¨ìş—Ä]ƒ*İ÷’¤-3:râC•5'oCF'cñí.D@~9h‘D¡½ù©b*ÁŠ“IˆiÛüNh-x+š-ZoÑr$¸6ÄthZ¯zşßƒ!÷’€~'±ÀQ5760_zl›T×±šìnV3ò–¤Ş{E,“¦&Ü\T¯­RØÂÃ¦gU=¶«»”¨ûà:êàà ±¾Q¡ñÖY¯ÔGd-bÄ[ ËƒN½şZéN(PG!Šæø~MÁ7gWÉä-“ó‘ÖX¦ZÛ`/í8Tœ–'ªKÍF1Yİ-Å)Õµüå¼$‹t²BÒZVsqÈ­m‹iì‘«?+T{b¿ä8Äv¼òB¦*ò"Œ¡UŒ«şüJìmv€+ŸÅğ-$”_Ÿ¨'kxbIlœR°°¤{CÂo²q¸»A¥fnnCİ(3s×í2Äİ©¨€î¾°¬ıã"<z=ÏÒEÿGR$3ÇHøÍYñâc\3ô
[ßè#`9fŒQö²‘ê5™šk´+ªŞ`\ígÏ,¶X~ôZáßéu¯èk»Qø±½b¡: _ƒXÛ² ©›‡¢m‹[<bÀ´Üá`½]v.¶…
¨g­?ˆÿ¹¯:ş«®ÿjÏõÙØø»ÛÊBğ4•‡LşŠä"z2áÙÆGm)7ì“ò(®¬Ä¨İöf5+ø¯¡Ít«tûMÔqªİ<ë6N7|š/­i
4_=ã»ªáÊ°ZÊœÛ¤›´»Ì‹õåfş\d ©*Ú/êNz‹»÷h•Ø¢š¨nàÙúF?Tïäë¦Î·°º¨6Åóç­G¦–w<£üÄ»ml
zG˜gÊ*ş»ş‹ñ›ËqøÇBœœ¤Ÿ÷·`cßw5MıÛsêæ:š6„Ş©‹€NíU[poúÃ_ÿP¢­f.<±±Ç5hrNê¹;5_Nïsnï¤ûƒ60B…bs¥ÿ®ÒiÍ»x[A]ˆÑÛ¦Ù¨y.¹b¹á	2ÿ)™¤sôdjâ¸İ6ê[äƒ™Ë¡T[^ÿørE‡hº‹‘páÚdÇÆ‰_¨6cˆ-vFõŒÄÏ*{ıP÷Ñ&ìäu4’ÖüGò:„\£a˜ê¼¹à)ñeà»ÚQ——
'p[–Qï¸aéı ™¯‚±Ğ(ìÖ"í?ª3ˆÊ´€-Èü´|S›pACÍ°x´òoq¡b³p°6–4œEwDÓ)	pBŸcVÎÑ'=@ê´eá(å˜y'}ÂcìºÖaöŸy¶hï·ö;ˆ·{¿À£¤[€ åœĞÖ¹ü(äœu~’¿OWGIAfZñ¶ÙòÔ÷´õès8îìA]ØA±;´÷o×ëå“Ï>Û—.ÄÇ¿ş¶	ÁDiP”Gñ[Í$†«M-63;2¤ÊY£<•ŒêğÀ˜tÇw8Feë‹û¬¶ÖKÏ=¦¯•åİo'…Tz¥şU{¬_‚vq´R²3SªšÆÛül-$'9d´²Ãú*L^
¹
şèö–Ë³dÎÆ˜2Ú”‘Üß¥29BTãçßĞÕ}wG‡Ä®‚èÙj=âW‡’à^Íò«dv*–ó0]Ã¾LÎ“âŸåf=X¬ÛûV_÷;şâè•ÏçY0„Bøtn>æÕ†«8\‘‡x¸ånÌãĞP³y<Â#åA0^€Ñƒ6«Šizˆˆ„a*,â’àÈò½ÊåÒÖıÉ(’Uâ)ŠQ@îš"a	5ÁôM,ÁG'ãD*Ö`j³Ñ²—âJ¡bT¬4s‡0ÏY8ÃôÕñúê
NTº {Ğg5¾vCF}kÏ°ñâBO]ƒÀÆa´]æ)#ôØ)ª·¼!¯âôÏïPÙòàkdŒddcÉaLØ¶ø ¸m±™@HRÁneud¸[nb¥Â‡°ğr’P¼lç[	0›cÃ–‹Ô¨¸3smóÌœMş¨mõ¸@Á%¶¬¾l­9ªí°9xxq-"àáõ1"×Çâİ?îdÛ^`ÀóonÔQ@Ì»^=$o@E¿r`³.m8ïdì¢.lEKóitœÒİ‡&àùĞ<<m ö£Ï§|[/~£'<67Ğ“yÇD‡(§ÉRtè(n§ ’©šŒ=[šQ_6³ì_'¾uŒ[ª#5ÊÇOU˜ i¾ğìŸh 9W…HçôõœŒ ^úÍ1M¢"ÀàùEËÊò6r¡„q‚¼£x“,ìGä²+ùs¯hÉI•¦®÷0ËÒÏ`b’æ2dÈÛô®àq&¾Š£‰8dª·Õ%´ö gÈp j¦a¥õ~Şú"Ä±¹¶Å¯|,¤ü˜İ1 ‰“—I'Õ,Å£%¡xsìˆğQ–ï}ØõK8TpÒW»Ür]°ø…¨£…{ˆxòÄ:Etp!.EC°£¬VùŠXQWÌåÂPÓ(ıË«ĞqGhÓAĞ Ğ=¿·gbYjtk>õpz!A€o
Hï©énà/Ú©ºj„„@;èË§ŠJQŒ†À'o·,ÑÜW¶ú¬İE½é
ºbX…(‚ı5(D…5.{»Û×Çt«ZÑ³<ÕåÁ½µà­WpĞ-¶Æ‰ø-ÿ‰”İD·%?RI9¹öÊû¿²:™4î©¢…À†d##F ¶ğÕ®k¯´° 
á
ï>¼œÚô–Ò[ı”–›‰òÚ¬‰´Úì>“ ÉXÕÛ.®Ì}A… dÍÎNA×8¸ôLJªFÄˆôvƒÔ¶ENSc¸<C†Ñü^ê­ñš}®3ü2ŸpXfxNòÅÍp™¬Š­.rQÜ·ø¢Dl·VĞNC­Jw*õ¾öçºæ´v´je?®FÆ–Ñ­ËI«f0\€7ïšÙ¶ÀµĞ êY-zÇùRê4Ãq…ìTàÁ	EÁUy‰JW
¸LÓ•Î°Õ®ÖôR¼>è \5MsÂ…"çŒ}]Q>xùë6ÀÃ_h#ÔBâ«¡|ıØÚ‰ò´W¯•3k –*{4„–/1?|.ŠM&è~3ëÆìdğ”pzÉ‹ézĞÃZ=¸•-C¾aı4‰Ï¾g&[ˆ¥j7”hÌ‡’nšô×Dâ1ë¼Tî.¸±HÙ>‚XÄœUìg§Ò•ÛC»VÂšIÜcî±zSÕ„ÍxÜé'îÎÊ•ZJy…½
=!Y†{ê×x³’õ¥êK4Å%¤Öbîg+}cœ€KX†Õ$ó¡ j€ËÑ\®^ %3Aù]EHövòÈò§n¹Ê*uKH¥­öKœÓúgÔ:©L¡zËn³ÂP¢?Tñà Ö÷éXUïn³iz¤ã÷Ô‡udƒ7–{ø	
a•ıWùòŠ/‡|”ˆy#@$²Ç½î¿9é{T·äe¤"D7D2'Í|K?›©Ànµ»³Ş5²˜Ìë <_äkşs™¿/ ß*¦‘øé†ºqH²ûÂÅb‚ÏDcŸ5èÆ¢—P(,/ ÓQ‹ÄP`´ğ|Õ…;KYSÀ.éd¥bÉĞaœ-¢yæö[ÚVÔPcbø5.®¨‰®‰'©±@Çf6
¤æËıN* U¬ˆ¤.¿~E´¤<ğ×çe)«ª²µ (®…T-èù›ôî*ìË‹C{¤lä*_K{6Û¸´HW/fùä­ŸlËçåŸvœ  4pxW@®mh·»¤>OOÅ))+Ğs.&†ï3±ÕaJâ‘øÚzŞúóá¡±ÀˆF'ºEè,+^‚]x†Üµ«áØ"ŞÛçÉêífé‡i¼MVéôb•^§+°,@;[şıŒ1d³LØœÈƒ6ei³l0V—[_WŒ 2lŒ÷ÅauÃGL»ñ¤µïÊR>4€”Q×íıdQ¼ï¦ØŞú-ä…µg JNå5U›½×tsÃ^*õ(,:Æ‰)iÌ Å_¤¬;¤ªaLoÇ¥/Ö9Ï3¹p)…gø”$#™…7ãTèûmP‡2ú´ÁÑ¬_;E.Å Åm-şAi«…C@+	cˆª äRäÁíé+·„XÏ`I•B1
6ğNKìäçuR¼µfæwëô?V; Y@q±|³+¾À:ŸæìÇe3ç~5Ü4¼š5i+tìGr°ùõµ˜ÎNKy‰•EÜ²êğ+ Ç³-àÈkO–¼=d)-ÉYÌÁ¨¨šé¶1¥Ö_ÏòÅ¬r2;æìÁŸbİÕ ÒEW6ATR)KTZTÒ²Ë`G¢şK|½CL°Õ>Ì0-XÈÏ’ÄZeÀ1¥–L¶ÇNñ§úşˆ{øˆÅùI!Í—ßœ•ôã!Î]ûŸ ö€oğØ+!EV@‹_œ$mkÚşÍ`]ƒçÂ¦ùB…õÁVÕF•ª0_•–.oüQPÉåu‰DJÔrƒÇ;"	OŒízè:
I
A¾ø&ûyJ»!GÂ«‹	™ŞX^k6´B|+oG:áÂşgÅ²%ôp¥‡ Uxç´`x; ·½ğœÆX†Œòã<àÏğ)Î¦Âç}'ä„sÂ:8…Šõ~Ò³cùmÎbhî~QÿÙÀá9ÄxØ'·İyVR­sÄ/£¤Û ºi~ <M~êD¬
gõA¸dX¬f§î×EôÕÉ/†î•Ì¨fZ>âQCPj%7¦áÊ›¿ÂÚ“Ò¸$+°&•ÆšNéÏz
Q<dÎiÑ ÿ¢$ ]L#©S²rÄÒE¶¾@e0§èq‘¯%x\^o)1$ßl=«ÆÌâ™ˆÓT¥£a½¿ŞAšD/”²Ñ[RhëÆ¶Â.22˜ƒ]8«”£Õúƒ¦öG„‘ÏÅÙé©B!Æ«øù<¸«(¢¸¬×¸ÂS–ûf£1ÈÆ&>ƒºÉ
¼Ş%3q€kƒVËÒûü„2øC@ğ^Ä)ªl;wî;… ,R$	A xM–Ì†úD…»mò„yÂ1óÃªjOÿùŸÚ,™¬Qãÿİ­ Ûb	Ù)4t:XYSì­.Txûx6/ÅB§ÄCµyÁï#MVi6¨áÌƒEÜl58„k}¨yÆ²éÕØ„ >q…Á)wEgdQÍdÈƒjTrLôğµ–´ÎlG—ëb ¥¸üÖ†ˆ`ÃëvŠîKÁ’üÍ½~hûöP“‘&_„rYbã›²‡:“.´-ğîv3¸q(ÄF÷h:XLf›"{—ÚQPtµ.Ûú‰ÌË%Xw+"Ê_#	Wş¿Beà¨)[y‘ÏØu©Ÿ²KÆØ¸j)è*ZÉ+±ÀÖb‹šì<ÙÖ|³˜¦+HM¶Ëæv&l£ow£lk× .s±¬v¡llÇ mŠu>ïÏóÖZ²Æi4XgÈŞö\¹˜Ê^ºS•ôJ*è£j§~ÕÓ¶ ÕNæäÃR!©_
’ö¦ğ°²\¹l¤¯®úûJd“%ıËã¤º){)ÏU™ç¥ti•|^'¤í‘%=èaÛ"¹g`ET(\Yb²P¹Ÿ›˜+&ö”…^©"£°Våw‰µz”‹ZNtSTøLïU‰ù •ˆ%Øä M)Â>ûLU(„8Ÿ®!Ş3ª/[	¦r“Æm­«-@®B²ŒÂªSgĞºÈOQ­Ö-an¢`Óæôı¿¯kûDõµùÅ´[àY©%Är5:>V
câ6Öø+ooU'äA#Æ
 ò.ß<¹ƒÅ•ÏÓŠY‰cÿâ&î„Å6'`Öe3:»
Œ”ÀHÙÌs&~ìøKÛÁÇ41éÆıÒúú/áÛ6Üa1ƒĞ 9~—r\Qä_gœAo³tÿzH-0•–˜ÿC5h›™ù­hßÂ¢'ïD¸+İèÑ÷(CÅ5¹jC)ŠC­°u‰íQ“ûaœÙ@q_­VÍ¾eêY6‡TŞ¡#"ñš¯,=¤Ø1âî/kn.MĞ˜VëÑ©P²Èæ@´æ€l»[Óé‡ú¯tB;tÒ‹ßeÓ_5„)ÒÙ¦òÎ¹ÿ h¿kDU·õcƒÅ™&ößµPF+e5ƒ–”lÅMÙ’37²A|aXÇÀÿR:Y9ğ³®VÖp1ÊUõFCaÚ¨%»†#“6å‹ùQE°ëà~¥Fë… \ÛKª°­|ñÚZ2úÃW‘ã”—ûõƒí˜–Èjv?ü­„óÍ'16=å#	¨¹ı†Æàùr©¸ï[…›İ¾n:sk·‡fci2Š4j'«9©å¨üÎâÒ»Ë…ï³÷HœĞX ÇÍ¯½_)È5Ä°r²™}K1ÀÉu	4xÉ-¾ú}Úİi] a©uğkà»ÇzU0Jç o³"ƒØ={0YêR[§±ÍŠÒïÔÊbk>è–EÓG2]ïÎë–éV¹ö¢«íº7›åâÀ˜MŞB9	œkc()£D‰‡*°x>›¾´	cé8¤­ë™$ü:CE»söD›oñµÖOy©	«N .É²Eì‘Âg€Ûxå[H¥¦Z·üriõáÛAd,m*.b
øÍØœ# ¼HÀ€Bºùãiš$Õ4UÉ,/R£Ù9<ìbÀj/óÉ¦HÉÛÖ{ä<©Í‰Â‚Ÿ‚±Dé€ƒqå1­Ìî´º±{ìb¥ÓœÑAµ>ûÌ/IöşÁ×ún[BéÊ‘VT‘Êlƒ’í^áaug]¯H‘¿¶‘rÁ«õäw*Cv1ç®Á›ùQ¾ÔÎ»ØÛ·˜“ˆú$¹Ë7k%:ÊÊµZ¦y)>¤ö‡ÓŞèèõø¢wÙ?uZO¯V	@Ğ=;¿ºì};}b¢øÿÁ9Ù!§ÔÀÑ~i ÿô—&@èj…˜$Ó©k‚­8g	.Ì+µK#¾º ‘=Ú%6Kù +’şä´ÌgÄ¤*§ª[`t ù£ µˆÀv>˜‹1+ƒ‹ö“|Q½zfu¢Äˆş,Jù}È_$“·›¥Ûl}“sRA®¬-³ùr–‚&³jbƒ”o¸V¢P“FÕä×5›_ı³y«°©Ç´åˆ»ßë|u•MGÍ79ï8+æYQğ¡=¶M@@ÕQI´¢JxÊ†Ù+I½yt6â¿0‘Ÿh›C+[Á E?¼õS%BvkŠÀÃD‚ r$0òB×ÊKnd`Ög¥¸K
c’˜i–3ú"§ ùù»t5KîâLFµX‘9Î”dN¦Ğrq‡F–¦„~nÍj Ğ	ïHèúpˆ+‹PãcÑÚ'`¤6E„wM¯Vùf™–ÁŒ(U[lh58Du
3T¡‹TÊ6+¼h¢ÄÑ·Ş2ô­*³})íNåŞ  Á)VpÂã´˜¬2é5•qp2Á*2©b‡±¦`]¤éÀ/¦ßİ¦6ovÌKŒâ¢›_“Z–Z°ÿšÄ¥uª¦étñ‚[Oš±Jƒb@ 1g‰±ºJ…e&iE™ğÜEÕXÉ„ÔU%º47+Yqq›¯s~*àñ Š¡9dy‹)şÄn‚Á¨q4ïFåæõd\ÓØø6›¦ØÍGÁF¨§Zl¼Ê®Ã¸Øœ¢«m¡<İ`Úıqàìm¦Y>?`PÕ(ïŠÜ[¸s>ÜJĞşñCëªŒşy&¾©91€©&ÿqøƒŞ­Õ¶è¬¨=O?Ê´¢OªC9š¡Ğ­	®ï|xâOGT—åV*ûÄŸbcË7b¿ç;¿ìNWÉ{¸åìÎ‹›ñ˜ÎWç£‚«šGˆFT=‚¶ïïgëÛÁpğâ¤/zÃŸ¯ÎÏúLhGHOI€¶°èy‰Å6M¿Q
&[p]ú'rV-QÌšËEòn¸‚DÉäÖË/ñ3ë®Fõ}OñL‰Á\øú2µˆ”O È^„|RBkjÊ©mÈÃ8ĞâÎp÷‚@…8e!àUãMdk8b€4fèKOéTŸM$é=iQè”0m·{ªwÃ ¯­ÄaCFàGFÚkl‹=¿XM2’¹.õ$$·ó¢¯A(hš¬=ú‚u¡Ö:Øñê!³Ôæûã-úU“şEägéû“l‘m ƒá2QÆ4ílP¾’$¥³Ò£Cn^úAÛ’R1…}†J2²)(õ4­M
îy‘iñ8Y'•'SŒÔ9\ßÍÒËÍ¢ Ïä“ĞpkÈ×:ßÕº°çHuo¬°ÓS—ÄK“‚´k ½R·AçVôâ/ü@pã¸Há¯‡¡š·€ó>ş&Z˜p3ô1{´±ïBî8º'Ùt¬NòãÇ
íÁ[ï)yaÜ¯ç××éêÍéGÙz–²ÉímU(Hô´ÉÌe‰›I¦¤ÒuOğ¿‡¯¤uò‘zßŸÑèPç„|–¯Ò)bÈG
ñOoµRÌÆ+ğè‡àH•¶B‚W¦´ï×uÃMŠãt"h¦SÇ?iíî3 ¼Äk¯Ö!¾¤˜æ[|I3ª×ĞÎ‹ÌF6WkM-T‡şîÁ¬[OG‰ÀÑ0h `k)[sEü!3Á#P™¿ğ£!¹‡ˆë|%˜S4.´‘²I0b:ì~ùåuGãıÉ_z»E† (¹“úîõCÈûÿosü—ÏÅ¾Dç*O½Šmx›¦k6œAJ<ØNºÀã[í€â©€_ÿ’á‚’lBícÄ‡ª´<§	øMz×AşHÛ;†S*ßïğj¶,N¬	@Çu{ÒGİ ‡jhÀ›k[ûtL9éşé/­?¶(›Ø¡lcß¡; ûrğêõ(hÇQRF÷úµLÈGŸŒ{LÄ×ÛÜ' ‡…§J“×vÅµoëz¹»O·¢§İ¬ÓZFŸFª[ğÍBôm"múZ“â/®†›	+œ‰x#NRÚÈ¶aÖ¨&}í
föËßlë‡õ(“Íæ¯­ã¡¾ø{Vx¶µğÑOÀ@¾{ùH•Ÿ[Dò“g×BšÎˆ“RàºVÇ6néV[i-j‹k•6uth85†bî›ã^ÕjSCeÌk$Uz¥?û²7µ²5ÆgKZ“Dtûş6]”ó×ÊğÄpRhúˆ-ÊkÎîuÙòeœ”¦Ñãb°€îz…ªXH1ÕóĞğ½3¸¹GŸ•µLHä©ìŒ\EL6_ÛjÛd~lª="İ)•5Ì|Ê@F×0(™X	ëÊ´ln²^,h¿•ÁmÅk3]0ÒS°Ô(Ge¯¦ƒ6¼èâ1Œ‚%v½±TP›K™éØÉ­Ì†í½«Ì!ï••¢"®1\)#é+4RS†%wÁ<hC¬zû9$UÆŒË09Dz-ÚEÍHÖ—nl!¨r­5sôßĞè‘ö®SNläÌİÛ"Ğ4†e-ñvj\<Šómª–šõI¯¸şb²º[®Ó)°Š*YjV@ÊÅ3íR€Œá"×Ä´XJÿíë=ê3 =ÊH®Ö`9F<AãMCGL||\ÿ¤[ìêÏàÊs`ÙF[­…Íía”*dŒ°0²mJ‘Ğw™xüI»Løğÿî1ñ+ò˜(ÍPA‡—iRàm–/şX1aÜX0¨¼rZ!÷7³fYg¼ÂJØJÛ2n€™
Çä P
Ş™ªL˜(.Œ]¿Ÿ!Gşn‹2¡§Í=!5Ğ;ì°B›Kâ,ÛªDƒó€-D°<¥6×ÆhÏŠóÍÖ×+èßéªÚ˜à`·­şËè:»‘'y‹¯Y;‚Xœ1½Nª.â5nÕ	X6^ãJ+Ö˜v5ŠûÑ´$ˆ`¿”¢êˆÃÎ/°‡ğ‘ŠmFÍ¹Möw&é»èùĞ^¼•%!Iè{‹éY.0r“H
`=‚–ÍÔP¡]ÌìXPS~;è&Ë¥à²íıÖ~ù7´Ù•—ß[R±œF !ÌF$µøi„ëçPvû‘æY­›,Sv¡ğlÖŸ¾,ôOÑu.AC%2äˆDb*zsUÂ}zı…én€³ŸÕs‚zMu•­xšM¥Ú™¤@~Ã8‡Vùcâäá4¸@¢ÂsG.Üš"©—õ1Å.ãàµC¸·xAëß’Uï’…Õ‹å´E0ÌÅ®r_t¹ £ğlgˆµÆ?2*xÌ]–©÷\¹}zú
éì)!Vœ ÎÒ?.qh-b«J`G2ã²İßèËñ+XÏåÅM]JEî()ŞN`.UÚ„3JBµ„ç\u© öB+»¥{ÉÚ¾ŒAm³«3ÎEÀ…çß›À/#w¡$ªF¸ìá8æækNà0ç]ÁÑBåS¤qDÜ.h|¯ââÆÏ_‚æDp~ænÕŞÿ?Å~§	Î¤RºÓ²Ù_·ö÷ÁS]¼kêWwsa¸ÙÊîÛ¤¦“U%·Éˆ Ûd“· «ñùê»t6Éç©}/\k§?/Ùdœ\Éi¹—=ğ–” 
‚;Ï•@àxÓ_‚½é*™…±H[ò·&îÚ ßî<[dãT÷:¾Í¦Ót1~ÄyTÀãög/{FÓ®~h¸UWa‰9dT\•²î[\ZA§¤³ì"MİÕÚíˆezÀ¯S³;ˆ~	Sá"-uäpÓiwxÑ;÷ÿ~tòf8ø¶_ıhŞ`Q¢§û3üÃçš±8„9â¤`€MÅë¯áM2äÇ\à“>Fº7®Ò®ƒõ-<µÉÈh1Ø/igË\E5ò¨%Kü!,¸È4{CAéTêÒ»ú’WU‚şƒW›FpUÚî„`öÖ¹Ô¹ÍcÕcV%¹ÿË×¤0ÒRmµç†¯³.¡+;~¸"cmVl“•Úã¹…×(1Ùé@œ¶¨³ ëÅÂ‹Y†U+ ŞwX2±IWæúPã
İŞØàèü2ÕÂ»{ƒÀ®ÏuÏ}±İ¦{9pa:(wgÌñx·L¯ÁC^x{!˜ü©şÇÑ÷ı—½£şøòüÅùè||Ú?¼9F½“Á¥zÓUz±o-4uÏ€=Áe:  š†äU®İğõ}ğjêİùëÈK #í%ëV*éaîTê”hù¥¼Éi‰Èsß¾ÙgğœOäVòŒMÂVŒÖø¼º4HÖQÇ ˜4Zíamõ‰ÓÌæG[½Š®ÅDc´Œ¶Ò™ñû6YËR1Äïc8¶õq¦º…HÇ¥Q¥W†G‘n5$=Ñ9á-ğÖJÄ·T®™C	itµyàíK\×uÃã×yİ(	©:¥î+8Fã€‰ƒH÷Ft¤Ã]ó>‹Ñ4hkD×Dü‘e#CşPá~âBLWÜé‚ä.`Ğ,W„Xâbƒ?;.q`òù*•ä{CÎãñæ/Ìv Rb­èàC4CSMİ'èjâDÚnŸªsÂF#¶55Ü!N?¾÷Êv ÕŞIé*(œ»µcÏwFƒoWaª ÖBµT"f *’T…ÔªñN«:ÃùÇ¶FJzj¦¢‚-yRÁî"/íÔ5ÑÈ>˜’@µúˆ9œ¹LŒçÂç´ÂèwÏ…ß=îï¹@\zş   ÿÿì}kw7²à÷ı´?ì¡bš‘•IfÆ±œ¥$Úæ½F¤ìx²9:m²)õˆbó²IÉº;ùï‹*<İ¢œÌ½îs‹İx
@¡PÏ¯îZ­šîZ2Q…‡<„sWüa}¨‚îØvá?ˆ÷a-nÁÓ©M8Ìç—¯[ël"W¸•Ê®ÙL.VfH“ş(Ê°Ã_Z&3	İš‹.'=…µó—uÕ¦.xNJ",0zóñ(’ı‡5Ì4&Ë•9Vé‚¾Õ«Îª]:ì^jÚŞEDõ½:( À³)ÊzÛoÃ®Œ×«õSµíÔæ“¨Û¼ñy¸Ãh4Åç™¯Z«û˜+™w­-T¨oÙLüÄ8¿—ò-Ô'	j«Í'˜AŒıóÊ»¡Šì¿X«¬Ì³g¾™ˆÚ”‰hÈÏĞê_O5v<kt
ˆØJ—ÀŞQ_pW•X¬»­:—(¿§.¾êêwü¾OpWy.Û¢Í~Åuı-û
oÜ¯ÀÃ¿!]xN¤ÿ£û'¤lÿ¯…³[·k4ãØvÛ3ËT)Ó¬²,3hÍ6§5ˆÁÂr(h«‰UBtä1½² „×a¡H—À’‰‰rãÔdUê¾>’œÄøşÀÉæ¾ˆ3şÊ İ¼õ¡à‹T=«*ÍCş5›ğÔ2:«ô
Â¦ƒË¨ÜÏ×
ÉàJ ”t}·ÂùW"ÙªV±èI<ííFÃAÑêL´GM°›Ô²X] adU3ä ìFüãÁ1ÕóØÖ,y8<eQà4èæipê°íãı¦Ş5á÷˜<ŸÎÖK„ıio>9Y]¥K¶Aô]ûœ‘¿˜¾‚#„çÓ2M®›ó ²¸şKÑ'œ»²{·aCÍ¹âÅŸ>¥¯Şœ_†”üeª“ÀòëbÔÿyTQ¤w6ìö+JôFUEzû£ÁÉñÅé»“ÑIEÑşÑÉ†¾­»
ú0™«@ÈàßWålrŸv*naa‰C |âæ¡ÇÔ‚M –Ë"\±’5 wëË†1ûvğ¾ßûĞûX¹„Hl/g73öÇÛì6MîÊb©Lgıáùá¨ö²€í,-Ö³%™Œ±ÿËª€ òñöÑûÁÁc I)ÿgï£ı“ã£Ç›Ç-NÆH[ßŸöû›‡k>Ôg'çÇµÏ ˜ôãÁ~t>ì?ÂYySê!ãZşÖ?«âIGŒs9Å7?Ìá*_#úXÌÆÉááÀReÍ¤I‰4Á“%İÖĞà=8ÄÙl
ëãñ†ûvğ¦bNß½"’z·şwÓàyTú^kÁa0LeñìJUü•9”ë÷U½½ qÓ£¼éá9œy9Ğğ50?\åËÆnçu°ei±ATÓoÒMuÅMçÈ«.ikáfZ(é3†­*ã­¹kû‡¾Cu­#M{ „şõµÑûıãÜÜæcIT˜.RãA¡iÁ`4ŞÃó#s}İU.³Vh kÚkÃÓÈfÊ„±_ÌğZ=iÊ[…ƒú)`}†ÒŞ94M|oüVëö€è.cğÚt43ÿ"¹cá	H­ãT"B?ZqNj)Õ ã|>NVeSÖSÆ¡vª¦©YÊÛ@£Jéñ´´&¡:oˆÏR²üà#Ú5m-¡„±VÈ„©]mAˆiã™ºxz¦ÀötmÆÿä±…ûj3şÕfüÁ6ãÍì[gÙœ
ã÷Õ }èdş43Ü¿Mµ?¬=¼NŒ)¯\ñí”]°À˜:D*gZËÃa¾óiî[€¡C}Ááƒ&†i²_ñ>š\ ÅˆÙMj•¢‰B´¯<‰ §¹0O£C€¶›éıAÁ}6»0'buÖóvl
Ï"©½ïI)QØZ“ñp$ëÕUu£‰·ö¶dV°»ÓıÅzÊ„w’ü{rıy¹ÎÂÊhÂ¥ «vtˆªg½r¨E“]>÷û¯|îW>÷1}#¹ô$:›nd×'ct“ŠZZ53ì÷Ç#:Û¼kßŠçìş˜Úz–Ò6èf”âEı•Ã©kù8–ûJÚœÔ(jµ½¡KÕ"É&‚Ê^ìP±hÆ‘šŞ÷‘½t–#_âÏ7â§<Ÿ¥É¼ªÚ9Aı»É
Ô4µˆ§ˆØã{`é{øŠ‹ÕöÅûîèäØGì ¾¿üÚ*Ø?½åÒå÷—¿"ˆÏìÆÁ?ªGö“w`Iqå•­­—­§ÛO=ÄÇi$"ÎÀÃtDÎ|×Ÿ8Ö”;®2vx°?A
)sAÂ»Ì%İ‘ÂeüÀ§³$›·q¥u¼¬WdLbï ±·9ù‹°ûÃÓ\TµUg¿çÈİÀúáÁ÷æ“›Işná!t4Oÿïúà/ß°ÿïïÿĞzÚz&E4%Ëx2¦ËáUšÊxzº¦—ÖV3•rÁ$ôqÓJoSNï€šñª¿lÿZÅ*”%ãb‚ÃÁ@Ú]šü»	ßFÙÍM³šg37Ïb>"{i2««õÍ'Ô˜šªXùÔRó4ì	²föç¡|_U¤œ'×8âLœa¬~8ÿA4ÜĞTMòëo;FèmºN³Ö>}ÃÈZ*ÿGÜƒgÚtÅWr‹Ó®±o¨QîDtê’àwû]t·†úèÚÜAŒC'ã«tÄıh-7Zø6!îe„‹xÑïaê/jvíÒ¸2N+ªˆÕ¢Z¬4ó¬6µv]ÒJ·“æ©ˆ=Wf#&·ºG° ;Ì“	
#+…5`|à_oÿØÔ>;Öw;”™‰3íÓ‰+8Y´ÿ´M±î$GôÀßx²˜kˆ¬TçŸBöªÿ9+VEHbú8“Fà˜9ÙT¥™R	U¤‹ØéªšƒÕ5Ì¨ş>²èîô—~•o ¤şê¯æõ_ÎÆ0‘àå—Â¤º¢I¶|I©
ÕW$ÌFùîLÊ–¨ŠSœ6^à|Ü&ÙK‚5¤‹>æşoÙ|r2N <‚Œ¿¼Lfè*Š~H?-Øïƒ|¼¾A~Š“*îĞ¾4oJîµ—­n’EÛ#–Ü¤4® TÂú¾oã«ÁñûÁp°wHå0Ø¶î,®’åeğÌ•ÿÓwæ'jƒ}¿CG·¤Ùqÿ¢×º¢UH•t`­³d’­7™ƒîÅŸ›‰Eju²´lXljšÒ—Åö5Qñ‹‘PíãPy¤iZ%“ìÔZ¿5õÉD¥hRÕ-rFD–ºË
á[Q’TnÑ—]£ùòGÌÃmc\†#¢x´]$tìÑouôşÔzúıÅ÷ŸĞ7èûí‹ï·ŸvZÌY¨İ8û[ê=
¬¯–NÇ‡é UóQß™îüõ›î¿Huƒq+3 ´ÚòˆF½õmX·şwV}ªë*¿Ò32_¤ó¿¥÷Ÿr_X6AÒ7Wimİ.õì@¤=“ÏDÿnêË÷!;)LJv¼Ç£tÜèjÙŠQò)hWK¥OFvY,=úe÷PNÇ»t¶`L-F@§(×¢«ÛWVVäâ&Y^¯ä9ÈíÙ ¦)h Ò¢µĞşV¶ğÖåú(ÉæCĞÓ¾`»È*†Õ5kO¤La„Fm=k=½ é¹L]ñ(şÀ;UŠ¸o–€va=0B½X¯@$ş”§ÅJ'ØìêÚc ˆbˆ}Ùç77Ğ@Õ„ÅªÍ£TæR—»°5rÕšš‘;ÂcŠ\ûí·v5¯İ#Ïñ[q±/CÒ3÷ZF‹qÃ6QdÈ'¦TH¦3vªŞµøkİ{­ÜÖ:4d´g^ ğ>OÉÁ8€|•MïÀÿÉz–‚K9/?ÿyŠO
*cıã³Ö¶³õWŒ!IîQ)·E-Ÿª›0})Ûÿ-æ¥#0‹D|‰pÚrÆ'w	`kˆ®Š¹á6İ?L‘<Â=½Eãaö4›µ¥‰°£ilCSm?m;yS¯uK÷Ûºe+RChÚ4e:öâÂk4í4ô.[­EŠÀr3Ï¬ç'óóÁç"ªñ„{í³?{#»˜–hh2Ù^°FŸ„KĞ€{.§Ùm¾úÙëiğ/«¨)ÎºÆÈ‚Z1ü%³ÅUÒŞnu0î`@tÿúùë£ú•®ÖKÜ>mğ¶íìÈp8¶˜ÑÚ=¿({~Qvû¢vŸ,¦˜:Ø,ê}Y÷H#:­¶·©åùù¥„Âsyı72ªˆ¤ĞnÕÁ>¿RœÕœJ÷CÜ«x+5ëÉá†ØTŒ×Ø6A‹$›âCÍÓğC·äõN“lÒv:gïb™İ×{ËßºÉoÓáîĞ£ü0)dàã¶”a×Ùb¸`È#N	9Üjt€QÎ!9ÓìÉ"›¯Fù?òüFIsÁ%ĞF*`'°È‹L02„4Ç¡|TõgÚY•-…ëyÅ-Šûâ(™³:KFŒ8ÎNEãpï<™NÙt·e–½ÿu|1z•/8æ+gJ­Ú‚Í°T!ğg°GÑ/:J»é8¿³§ĞŠ•áÉÎÈæéd-NOì/+€`Æ
ù=Øò{Z¸øj5#íZ¹ ËÚÂæ®"Æ)¡Ÿ°02Ü0ğÛãşP$I°‚•-íê-‰Dá*$+^…Û]„…Wûù|¢yH!¯eÎ	<š}wÁ/µË|AÓAÊ'>ô
\®JxU±LæÌ}Ğ¡Äªß¢¡Øñƒ!äoò.ÈzÇŒè|­TÈã©eIÁ*Jú._¥¿ÃJıb«5¾££l6Ë
Ñİ7­ŒC;Œî–W¦:o²]j-ÖMlïR¬Í†¶dn¦&Ç DşÁæCÇàï|ê9›³æyÆw.¦ñY%Åõ`HRÁ b¸â(à	ã©G¬Rå¤iÓc~Ÿ‚,±ÿy‘1É‰¨â†9B¤á¸=˜CK Kp…ö÷÷y6NùwO~Á{q‘/JaZsqP¬RˆÍê8½åÔ„Ü7'“œ˜>“ZºÂğ¼PÌû˜K 4ûJ±ìS‹ä(f‹·İy.öî’ûÊïÃ|É›[ªƒ˜Øıù’Jvû(Y¸e	io2Á•&´7îei’0ëõ8‘Ğé2¿\²ÖP­Õé~ÊWËá,¿k½j…ïdöÍ†ºŠ•Ü|ºÂ›vhÉ¢µLdêÙ!óóHÔdãÕ/É¯.Æ¤ÔÁÒ4$ŸšÖ0ƒcÀ®‘p¥9êı|ñ¾wxŞ'×lî«48¦*yOš@÷tKİÓ0›ç
ët:Qİ±®Ê²Öm“:ìü-ÿL¤ÅD3”~u9¡ ²Kš´Õ\’ö¶Æf }
å
¿À~ä>”nYõ·R‡ià7bï£ªFS¹µP@Ÿ B™šOÍ°w¥8Bé/—ùÒm3µˆ]r‘YÍ¬çn [âÃÊs½doÊH„’¿\‰^¶yšN¸nÕªUĞCÑJ[Aµ²âM6[¥„€C/Åµ¡p1SÕü2´mU–ÉY­?Í²1?LŠX)P)SXåª«&«a¨ê6G§ä³ù~R¤brB"+@+ë„¯7I|/X;<V'É¤yÈú8=N?¯N3v˜xÖöÂQškÛ.‚É2oÄæ¶ÖŒ€24vôğßŒCÃ;Lû,ß³c©eTîŸ^Îú<ÈëÁÉ‡ã-sUºjûW4}Â„M|á¶“ÊvÏæ&„7hğ9{Tpqe!£|¤+şBLfd”ÒÎ¡·ÚŞùh¡j{oû8¡¿%ã°¨5`Hóƒ=Atïç *_¥³{¡ä¬Ò)ã$ù's`îÀ@f’ßÍ9|4é‡‡ó˜fiÍôĞ?f™¢´Ö4Úx—]^ÍØ¤êƒ÷#†Âxçúê=§uºÂ[êÃdJ^¿í5¹ŸÌfŸ’ñµ“zÍÂò]9c•2^xî®2¶û•øµL¸*ó@Ueg+ÅÖ—šw.{²ïê¬QğøcÊşBù§¡ó/*ºy:´4TZ_Â{7U"¼³â IÁì×[^Æ§¿s+[’&>¥h¿õ¢ş|k­‘€A«&p¿ÛäÇÎkä„úÆ¡íÄ£duÕeÜ°¦Î°Ğ‹iÕªd:ğ”‚Güë´ìD¶íVª+-üáâg+×-]J†ŞT˜n¸'¨¥Ìi{é™† c,?	ÌQ+´-PèÚË€á…*?½¼±Äpõxš<¿ÑTYòı½ó·ìÿ‡½°Üö«Š>…Aıî# ªúĞtÃ³LÁœDx~yÉcløºõ[§õçïi€¬5Ü°ÏºlâŒ…pn–„Px`)ë*?	‰çmwÖí		ö*zTN¾‡õ¦ÓB2öxÅß´1’	· (@×¿ç¼&ˆ°µİŞaÿltÁœûgàf’Ş œõ2!eÛªÀ'l¼`á 8LA>Ê¨ÉŠqì²ˆëÁüÑ$Ûü€äñ%/(kã§Õ™qÁúû:]Şæ“ô3ú¨pÉş¶¤
Éuzj¶CÓ«3Ïu‡(9(8Râ¶OØ*_cP¢ÒŸ´¤'J|lHw——)Ÿ:ûŠ;ÊwÛ¨"Ù–ôN	lñvuø86À<¢cÊI¡´;9J>ƒb~ã|ö¬#]ìµÑiÔû'$f­—„`”¸¨s² –.ë%:P©ãSÛ —s,\^ÙQD‚3nßĞ§ËäÜ[u“itÚMÖ…Ëüû/åŠw uƒ¥@!Œˆ"I»!&À¼ßÙlâŠPâ\léâ€n¹¥Ù~ÏM½Á*š2\ŠÅ öSš²ZE$ç†3ŞÈiºpÉM@¡VòòĞ@·Ü
Â˜Ú£%½××S XKL˜ÍM¡—£JÒf)ıœò¥õ*’FËd®‚Ú,Ò9¿Î âÎ›n| diom)Œ|ÜŒ 8N³[6®Ìøå ¹#¦‡¹ °…ÁË`H3”ëüÜ6zÁaÀ‹ŸÙ(ŸÙ=şìì0e4kÅj÷£ïGªÉSF[š‰Á¯40¼¦½ä?â‚#w¹z Z>¶õ±xÚ‰Šãƒ>”Gß¡åºkzQ@¤½lÒ] ;s±J.½‘Fu8ôwï²Éê
v¥{.xæÎÀÀYÂ¹£t¾æ¶Q¯RP=¤Éª‹71€]†æ ø1OR,ØÁ‚Ó—ú°Àu­{t¡³şpğşÅÑÉäUôä›{¯ßm =¿@‚S4¹†-Ğx)<óà[²0s¸ÓÚÙj}Ë~ètëfH£o‡óqH²¢^`5µ¾ÍÃå;Ñ9¿£ˆÍ‚¼j7MfóÛd–MĞÆ>¼Ât,²š!ù–k…oGeÕª
¤ÚølØĞÇºil¨MÑ´êÇ`ÕJ×Z§ˆq´7:Nhæ@n/ÿ™A×{—r¥GÕ^Øa><¬Ø7ä!ÔFï¶àØk´ªN±P³5„s$°ìŠ¨zıõ<^Éõ¨YCÚé¿eÒÈàñh¼â?ãšìÜ­áVº_Ù%|»ßà¬4=úÚçšZºÿ‚÷MoÙyÆ²©'$‡âï¯Òñ5!£p!Ğ.“~ŠFH£æëŸ€v¥ü¾9Öo:ÏófÏÁ:Ä5Õçª.5Á;|˜g“5{š-Êä%³œœ:7Ã³\(³pÑÚR•á-Ì/- ó=…m7baY^o+Áb)Q¬ª&Y÷¦Ğ¦‡Ñ™]³Ä$ºğ•¿×á10[¹±M+¤ÊU¥ıµÏBX[v"EÓÕ§è5«Ë`C%IûoCbëÙf´„´ùæš mˆÀ™íÒÂ[|}İJä«wpI–lŞğï_~eÿ™¿%fV5üZqaÛ½mƒ„‹„á¤lÌÎ)ÍİdšÖë](í:82àÀ¸Šˆ&İ³: 0Ì¹Ñ5×Ê—e)Ğæ–Ò^ïUxX7DÆŒk¾ÿ	tZ7¶¥¦0ìÈZ¯Zÿtl ±0ûö¾}ÛÚ±Ô<Ô YJ„ ?)­ŠTÃ¼¡¢:Q5çÃÆ&…LYöÚŸ­C[[;Ô =†¯±~ĞÆ´;»4ˆ¨È.&è7™èé™İS0œæÁÖ«šØŠÁTsDı“ê„¼	x€@øö†W›G*ÓĞœ_ƒ^àšÂRäÅ«ïêkã †ºÛUÒ¤`LãÄ½Cµ›l.YÉwùl‚êš€‹UTC½lzX¾˜ˆxáĞ€(s¬0e	l:\$üUj«{	–¬­³•8+_#]?^ÆiZ¯MüÊ\ná`c}°f'ù,í…'aëÖ­ê…}(øÑ­tòĞÎFIä`p”|¸èæ…Ø6~µ¬,6A–“Æ—Znf¯‹”i° í|ê ê{}ìĞ°¢¸à†éóOùç†]¤{Êãâ07rïZ>_NgÑƒ"YD±0ø½$UëCT:«"O%H>óÂÑ®²¥ğ€ÙƒöN	Ç«’Q'';NÛŒ†ñ@u <Ra<¤Y?:qq2Xı_Pô`hTøjs³`[Œò}¸ñ»Şo\¡3enª·Vê¤=å³q¨„÷\´wş²eí“qé
Îêï‹UzÓ‡üÃÅ´3zÍìõù‡¸Ó…:÷i¶^öÑÔ–“Ê>Í.»0š½ò³aEÊú ¼á
Ï2ü)ÌJ/é(v4†ß õî®š ç®ãQS¨©âAäT[':ë°³±hz'Ğ‰¶<¢ ‰-â›l}óf–ç“	{v^ ¬J×’¢²æÇ·
m‰ô5 Œ³´èŞÌbhvD\¥ìÖ^¶NYj;€DØ$°¼¹å©MeuV%W÷ÁøŠÚ #±Gƒ0R{º&Œ¿¹!·lc0¶Î{ÅµÏì6,_"SØ¡üé#×1İ–ö:?zŠ+ëõ’Ï[/Ï]9Í2í|^¬ô90s³J·ÖÖoÓêÍi›6€³QâÅÅˆœw]í¬±²ËàŞ?Ñ‡#±	ºïí%K
}xjš‡3d¼Dq¿¡ÀCúÅ´,šÌµ£­Ö7ÃÎ6éíÔ
kY…é”[lëÛ8z£UcÕ9ØqMdÅ?u SXÿ¯Ô©Çxd±3^Ëw(n—oŸi"B<ôÖ¸ğ´> ¯vÍƒ²Ô°¾_Û§´WŞ‡VÕkÉÅDD¹¶væ	øº¦©
w¼ı 1}EÊ`;>=‘Kœí­ln«ˆ7²4½ “ùœ»O‹©~îÌ©;4¥‰³¨Ğ¤ê€ae¸·î'k¶FífÂº@ŞÃ¬±Ç°‘24V*á||_È¥ÒIÍYá|1{ŒQ´_MQ“Ó`‡:ĞKår‡l¸Ô=w\ğdñ=Éø9 £š ô{’ré‘1Zç5^!¸uh×â>‘xÖyVf³ğAiµğŠ¼Øîl£e¹/ÇÇi¾X/À_½”€†Ò%½D9îæÒÍ‰^OîØÈF/!<MşÏLœôQŒ ¼+İr2Ê÷0Ü$Ü’ö’"}#Ä»˜‡rIíÕUV ÚºlH	?Jk!*p¬“ãœ-•Ëılõºˆ&ßb¥€<ıL'cuÚéû’ÖŸBmzû‹óİ¿lOÙÒ1İç»I!	ìñk¬Âİ‚‹®†éòœ¿GÜ¯JZÔ+9-%Ÿ)#$ÚÑPáÅ~˜.’e²ÒÃ¨<ğ|yˆ£`—NQåe
ÇFG(ãùERI\×D]*K6E“K¿\öÆØ]:KÍ´•ùñ‰äˆ«Nvî°¸k£h±èšXëjã½#húy¸$-z¤Ënû;ûowÉòL 0mv3£ki÷?:*Kê7ºğ
%}\j@Ü©fÓNx(Ÿˆ :Ù]r‚«›ˆ}HVŒoZBR¥Åz•~ÜÏsˆ¬ÒÁœï%vtÌúL>S2ÓÒ‚˜ĞÃb-ÎRàÙ-‘;İ+{îˆ{d€cëû»øÁø†I~‡Åp¥TPµ'’¬%Ÿù,a)lBf¯‚<b`ı‡†€#•Z;—(êîŒF'GP-›CÒ*¶”åÉ#G¹4	‰a[nJ…¢N‰­€f¾,º¢%ËkÕŸˆ–j:ëÅ3ÄäJ|BêŞÊ“IJÑ½^Œp¤YeQcÂEîöç·•gŸÓœ
©Õ‘ç1_xbÒ;Ò‡[Æ‘6f¦%«µ=6¢¦cÜGgO•ÇëD¹ÓŞÂ•Í­67&oVÈÜPıƒøwòEÄO”?*¾¾Éõœ½š8-ÄF"Â€pw-(HÙ›!H€@rb¤°KÓ‰Šx›O…œS=h&Vÿ!4‡´[ŠC:ZÙ8à¶j–çQÿÒôã¤ùWúGº÷'™Ç3×æ%ù–rª+n4ğ<0øÚkÆÛüCêW$°$RÈè"i´ÈÓ¦µrOª"qI¡CbJDß±½Òû§Óú“O^ì›zÓb l¿šuÖÏ[wÇzÕŞêÏ'ü­2èƒØ‡ü3ÜZ5)U 6L9ÓâD¸°ÍSNvßç«Ô#Ï‚g!‹1hdhé÷ü93ÍhÈÀÀVY¯úÌ¡!nèÙ]«£Û¸&!%3–x@v"íÛàjñctPkyÀù!õ)ïå£Ï!İ&+5Ú…‡9ÒÑÊjJÄµòAûwÜm¼³Z´…Î§ZPĞú–ÊRö²‰«@~ŒKBú kƒ»©KCZµaG‡g§û]IúÒt)ÁSsT)³…Wg˜IÄ\BUnªƒHG;…8nU>`Ûï»dve´ZÙÜ­õ{7ºjwÊÒÑ¡‚¬+ÿ¨pˆSÍ÷§S†‘ŸÃ­‰» ï¹NáÅƒwYğwZ¦-YGÍL÷SvÙúÉ×ûáÉñÛ‹Şñà¨!›[/}å†ïNÎFeÁ†CFDÑªfloo”M‘íÂCï©FgÇ™ÕTİ³¨Z~7Gõ±‘¾…Ón¦Y¢h[[–?Ejˆ¥X2™Íò;¸ò	¦Ô üKé!‹&*ÖEIşÂt67À)Sf½…»ó|ÁõÕÜ Æ¾/I&ê(‡«l|.‡ìp›‰1¥J<j¥¤mG´dæ³!în%~±ç˜á…?$(¼‰PúğÈ¥AÍftãRø ö!ÉBV&rJ<E¸m‚T¡šuZò‰¸|>J0ù&`M÷Ù‘¸Æ Räú/9Ùb€_Ò^ÒêP±%t”])VÈ˜²,ÑkvtzEÚRÒVëƒƒğ/£(æ¡¯“Ïmı}<¯§‡”¿µ°ô…oã¯WŒ¯WŒª+†gÿ½[4º[Œ¿^*Ä3Şômât%v8¥ıXÇgf›òùy Fh!í@Sg‘0s!ÜP}·ÛN!4\{Ôƒ3dûóe–x|ˆÂî:í¶ş}SĞ¿¸aEo6ƒéğÈà? l¬ìn]6æ'ËøÒPÁ¾6å4;ã¸ WfÈ‚¼>K[â•âb·`‹MÇ+Ö~…gŠ	†ã”Rß…Ä5}
_yÂU ¾ÀZÃáÃ»ù¼éıK´‰¸ô5ÄÂÊç«y÷Òœë3ö:~{¸»3†çoÓLÿ]6›aº±	706ôNQÌØ
ŠËŞlq•ğá Í_Q›ºÂ“‚Bù+OŒ˜0mŞèQ›EkÅÈ^lğB¸ml'ÄQ¯¯o´÷İCZæü€+ î¿ëœö¼6ƒt`«U+«aı]“#x’‰à*°
¸Ğvñ	Orê¿VæN‡{ÖE(×3.Ç@)°„š¡ç¤mÎº3¼Pòyş&_®oÇ“-ÕÓ¶·OõÁŒiâ€á[ÈÆå¸ûÖL“Í³æq‡÷À’›ß£Œ\U’»˜{3/0-…nŠKÆÅarRŠª]Y2¬6OÌËş»[èğŠZ€_úCÇWÑxöç€«qPLĞW|XÛ#v"Ì‹Ê»G|Şìv YåìÊÜ‰:ÏU¸×Ê8^H¢æ»Ñ\×Şù¡ÁÙlºMõÆ@q?Ì}ÍÔ”¢õÕı">Ô¤˜aíôäğ0$Ñ÷.#d$ıª¢‘•Ñ6„ç«æĞ>÷%ÉŠOƒÎN™ĞW†½â«Ôú<Ä•ê^®ÓbÄÅm–\À	ãvµ@F”YÉ·XgğNLwáw÷­¨p
±ZÇBˆÜn˜‹OşÜI<ô’~^è]½ï–¹ª…Š<ĞXUaCƒ¤ŒfÇ+Œ$÷Öã~·l‘ü¹ûİwSOXÿUAÓ©FÑ@Z}ñCƒÚGë hÜbøvª¬.©«% rÖ-ğ.+¶ŸpV	(ä(ÏYg‹ª$:Tÿ‘³ààø×&ƒ4lğÛRLÅåSÏI?8ÂP©­ñM^5½:›wø×Ù\×/ˆ´c¶6W)æhz£ıw§½³şñ¨ñÚ;­·Ë5|'´ş¥^@ŠŠ‹w'gƒœz‡M°}2—±»ü\¥¨äÛÅİS(€¿ğ³«ß9p¥(*”Í2AÔ0€vÓ°1R|D?,ó¨á–ph‹£ÍCÏnÅ¬r©™¥–\Ó·H-õ±ÜGÒ»Œ¾F.ŒZqG*«?It©š Bh&kr£›úÅ4¦EO¥«ÒÓï)ºk,†ßğ
Ö;EOW›^t·§Õ‰°ô€5šòşôj.CÿF’ÉxÁáÕ­ÕÖ£ø>†ø™
/MÇ¾5Õ£è$»0Kh©æq¥êŞ®ìšf5e%‡²a—CÄ4õQYT5ƒ²¶ÕQœİBĞ ,€„ÈNx67^:1{šašŞ¨Çc”‹?—rA,„?™¶=±/<D	YŒ!1¡0D™g?^ó ™ÏŸ‡³íè„$·bppôØ2m óèë£|¬IÉ+$¦úãÉè$ŸØ)a™Iù8ˆf‹¦Ó:ÖÑ%D`XÒc2$©ı<tä@êO<˜x*&Çã{©§ÌÄŞóš³‹E'[?ù¿¿l™‹dÿDşs$^ÀåÌ×¨q ~aÆ-5Œº0ø•İŸ£×•0/”DY“ÀP†w!ƒT¯]:ÈW.£;4o‚–f“,agdb˜ÏÁ0Ì1Ù³ˆ¾ˆû‘AyJS©Ï¼]G9ø¡_§ëTN—fi0KŞ¶8JWÉÄ27ÌTcÅ``<æl‡ˆšÛ!¢Ôvô@×V¦˜:éåê&l³4–=ñFÒÑI°¢•g²Â×$r8Æ/™DîA™àÍozjA Ï?›ubTE®á¹ä[$›JÎæËRi_ëç¢t*»ÍFa‚e!Ì¬¶*¼è¥;ÓåõûrñÑ\@ZÿA	¶Àá¾)Å‚ûëb•ßÀ›_ØµHÊpa×6\ éX`å¸†WZ>nÃ±†œ^{!5éğK­&Ó4ÖTP;F¼ÑÖ§ŒkìF8²
c÷uJ
ŒIŠBÄÓ¶â…åŒ¾bÚTıh¯óR²ãA¤tæ¹„Q¨ÏˆÀNv«2À‘
B±gƒ#ö'®3›ÇÕªÄqaa¨ÔÉ)+ô~Pi§ñ¿Ê€£¦åw8rÉxƒeÁî/<²±uSÆÌqÁ˜ÖÍĞQ%4Z$:ÁV\J”ÅU±;†U;†";ôù®ù§NÈ=pÓQUüÚxjQoˆ©¾b¼Ğö“ŠÕ\õ^gÛµ·İvì¶#¨Mè²İ–ËXÔzæ¼%¼¡réšhúµ»¨†ÀRFìªğM‹Lù‡$@q‡Aä2úJ®ªtr¤:vS*àûóØ•ÍÖ[nåÚ¢ì…ÃÃvˆ(p—£¿¯³ñõYº˜eiq²üÎÆùJ7
÷úB·Êİíz¾mİh56Y‰õV!ÍQ3+pË—ªê\_ºTbÜø®#é‡Ds€áC<(‚¥Ö2y‚wÓ˜õNßZ7ÏÎ—Í:ü<Ïgø¦^ƒ¹¶„ÿ**î 1ş!ºú2N„¿Jà2^ 1Zo_æò	ÜØ$nx"77™ô„Jô"ŠF‰Y³KbuÙœ'qÅ+œI=ùˆ"ö^„í}‘‹Ñ—#]11¾,íòBô8w9÷zæ¾6\ÀŸGTÄuÎZÇ±—ºˆj¸Ú}9ªŞèn÷õøzDŸQwMEÅİËfÜÛÜ•ÓåßÿÎYo±5Zh5YøîçÎy­Ë'½>ã® ìÊƒn€%¯¸×ŸæÀí)İ~¢Gqë¦“ùìŞL´Nä@ÃcvdùZ´)ÊÊŒ,lÍP™Z¨©x€_”—­›?!¾Œ8s80âvjlÔÃûb•ŞìÏòñu7%v¥ÉlÅÊÀõu·å©ÿ¬õâ{R>æWRsŒG;m‚ç3öØ;öhü#·}¤›÷˜ÉæÂ»Å1²<ÈÆ«_¶­×½‘ã €’¤Å›4ş[zïƒ¤ÊJ•nw±ö-húOo¹Lîy{%ŒÃ_·²I!ü
Ë¯ƒöF“İE£±+Ÿ†f‡TL3ëåO-íg7ÓÎíÀ6İô\+TIX¬/‘GµNšHCÕÒN4Ö"şašPªøÇ"+ØL€#ì)[ø ÑËïx²>eùeØ©]Š [@*ïù	‡ïß†ƒ½Ã>wjø‚Ô¯Ù¾ÍH°` ÎQ2ÊÁ†˜&U¾¾v–şF³t½ˆ¥óºÆ`¨^)G‰«k×ß¡–ÿ—8KåBí˜ Åƒ%V5?›ÀÉ‡ÉÂ†@w-s>–ÌäR¿‹Q¢%Š#ğMµŞ<"6£l=Ò&ÍÖøXvRİ¢gKiaE+Ò{¶¾míØºŠf>:‚+?é3OÈmöÁ£ÙÀ(Ò}wKpºL³nÏ¤Ì{÷µYQ"í4·Ï–…
ÉŒd]ˆl`ÙühDz'}ÊHhÉ+h›¦†çoßö‡'`Èİ}v÷Ùå<_¦Cpxx‡ïfdRÅI¯·]’LÅ5áø©¡eD/ÛûYø´ùè6FvM—©ğo¦i¬á´Í}\ ¿ëOØ?Êó§nÌ)‰ÎXlV9‹a›^í’cÜèú†)kdÔ)Ê]Åµ¢²Tb4¬JËq7\J ¸O*c-?{æçhá‰Ê–	"£ƒÊØ~ğ«½ÊCĞ6ğ§‹Û‚ÕìUl—ÜÚˆg•ˆÍğú˜ó„òå]²Ôü¿ÀmKøö:šv5÷–ôC·¤QìnÇkI¡°?hsp1Óí<ßuPé§Yt·àğÓ“@OK6ç¡C|]W°gqüåËÄs<ìIÙ\0à¬^™Z¯d"ò„† ß<(¿õõEÑ2#\¡·#Dfè"iM4ÙöƒÖpÇ$¾N	â-ù±Nˆ357s»|Éì÷çãåı‚ñ³À~…Ä<Jë…ëå(™'@ÿÀ-õ4.}HÙıUé¿¶‚—¿š+:fµÂS{Åî4õ’3ñF“'ß6ZÁö"q_$ŸaÍâßÎ²İær êµ¥]^1XåÉ2»„¸e¸P"´\dFöõ+*üeÛé #6Şjò½pü>3²æ–y­	ŠÙ`nm*ÜUÉ4lû<ÁzšCê 2 ^1{ùÊˆT
cšBWZ±q2çÛŞ)­RHáP©Åa‘¡‹åYÎîz@|°ŠÖ‰6!ı|®˜½q[­³@yçú~2GÒ¡Éí¾VæGYµvk³	y¼½İ­rÇI½™61ØóU6ÍÆ	7[€ÓÒ5ç…ñá«¹%»ãü}¤ÏÙ5F^Òı‹KBóË%kúf×(äq£æÍ
²µ€hh«TXñ€¼)]ŞfcHÑFÑÕ­¥ æ‘Uh/.?jóŒ&{	©á'Bå•¹(,YÔóVûÅ´,šÌµ#0 eU?ƒZš±Cäåˆ4¡Ayö‡æ¤Á¯®u;ò®`¬Ó=4›Í¸–¾(@»»şú¦pÅÉ-c¡lÖ\ƒ]Šã‡©8$9óöd—Ş¯îÒtªzjš;¤È&©à—
.)—àÆË_`-#-ÒĞ[iï|4:9¾8í½í_œ|8öR79ZÕrğ·ó?‘äÚ²U~y9KÖŒLÉÍÄ-ÀÈO7k®¦óŠlÎLC{19ĞğøÀF)«
ùXgŠÅÔ³9>Bl‚Û¡8¼JÓU+a]Àä÷àßbB‚æ24(Œß·yb0çWì"{ÎàfJªØ	úô'³1c'ĞÉı`ÎŞìšğ·ôşS,'2ÖÇìœ°µzfF/eõ*ByçtÑÖt•Íß°WCªüÒÖÚX$ƒ“Úç¹µä!…ÁC¸z…±Ùòì MˆóÄß€
K«½ãÍtZO ¸	Ü™K}û+:ƒâ¢ìß×9à…Ù~M©ã$~`øŸÿ-²Q¹£‚B[åÅÏ=3Y´wØD;ú­ø<’ÀiĞº¯ç­»l>ÉïøŠgg©ŒÅˆ–¿ü–Á˜:ŞÂ~@/ö'Æı~Xü¶µC¼\D ·júa9ÇN"ÜÿÂ†=ĞÚd}ss¯Ç*\#ì²À%/€b
ŠıdL-İñÖÕøiC šì²X++»¹´LÿX~gçÜr˜şç:ÓÖÊ°m(L®	pÌ!Ú;×XM{Q˜ÇI1±ì(S2³“@büD5K7g5}},ÖËëEwÌË«ÀabEõĞ…õÀÅå.0z‘¹åÄ:c­Æo¶µâf%våO·‚¾CıÃîú×®.=vÉJ.%BØÏ_à™Xê›¬(ïY%9õ÷µB£Ï±Âê!Êúv8q(Œ£*@QÅ<¡FN» \›‰Ş§ü–´–ƒyF`I«Ï6vŞQgóÛÇ'Úˆ¥MéBCz`éNOIùÓ@WPh-Û²Sœ€’n¼ÆcïİU.ò'0¦‡<ä=¯EÆóâİÈ ÀÊl$ãf#kÁÆŒ2yö,yÖ¹Ê¸4øÿ®Û"ˆ62k5O–ÅÜÍäbÚ±ë şDéß—üÚEaÍîFãX{-ºa¼ã’İÒe!“B¸;‰'ºâòµ{ZzÉ}îi”õVÂÛ¥óK4¿0Ïùy¤Cn`AHÛ%b>òv’<^j‘L¸ıÿyEå‹Å…R—™P4ê¾œšCH@’/ÚæÆ(‹ÆÂ(¤n§´nü¾Ed±˜dÅ‚uRÀî}ë›uÿmıÔêşyÚzÙê~?d§×.6¬¯ç›èkLù—Y’¾¬:7JêÒQ›»lu¥ShûƒŞ \¥Ä¥O~_!Š6éMù˜ßª“D8X„dn
u¡aeKÔ«E¾^ÓÂ/`)v%´ò_y´]¶’~ÓÙÔPòıj|˜ÜJ•?aÙ{ß?¨®.Îı`+hğT°;±"b(Ù`Óì]¢ó/ºJïS¾
6tÄ°-ïMÕîeã¨Ä¹‘•ƒ]ÈœØ>]KF
…‹µôÈ¸QõúóäÓÌª,Şé-ˆİácw„8DnÏKó¾y”,ğ˜¥n™wû5˜A´z>/Ø€x’Ö_‹%7jÔFuè#÷c<3ì;ìÆh»d»GvÅ Ím
õ£åßÁ:Èt¡…íê/ÿJ–'¯¯ªä()®ô@ˆ'~‚³YUÒe&ÁÇ×ò¡ áçqezBBMŒ+ŒÅQv–ß6—EÌ“Å–kòš°İİ”‡=Ö
v¾ßüÛ*V5µÂyV«!¶­6Ğ;Ù´³ÖÃ2ºnÏõÉ˜,Ïf±³Îıö»l¹¸ITÕİpu6[·Éª6Xõæ²n®ÊPÚ{şóG‡õš3™ËÓG¹åİF#’ÒØjë²0_Ï¡æÀÓ/T5õROƒ›¯À•÷Şî´4Á§g'oÏúÃáÅYÿôP»äW–?ÿ­ôøª,>8zÇ£ÒÇ«²ÆŞÉè‚«EY¥ï"+½¼>şYü´78¸8êz¬Ò÷‘•Şœœ}è°?ÄÖ8?<¼èû‡}VíÏ?ºü¿¹H{«ÀBôİ/¬Å±/ïkÃÕ=#f=vÑ».’¹YEÜäÍ¢ŒäÚêóåÌ,¾·dG#(§¢¼UQ(tÌ:‚ÖCZN îˆdĞyd‡<x‘gÒ¢ â¬ƒ×?şŠ¾ÅˆU)U,Â^@" ©Å€T–úïßü{_R—áşÙ	[œı½ó·ìÿ‡½&½§íqäÌ
ˆüÂ€æ…Õ¹¤;üÉ²ò£I8okS_.›tp½Our[ŞùQÈ‹Ì$	-?cNE3qÔ×ëÍê\vªuhtDuÒÁµÀÿONÌï‹ƒdZ+<|;å4fóı¤HÅºüB¸’@pœ™ü; ïÓı*ıåWÌ$•Œ+y—dàÜÉNñ·)c^¦Ót	ÚÔzüh6Qš„Å¥aÄ”¸€‰2ÌÌå®åé^jÈ’ †ÍtL‰ûH^.ğ„e­‡aúÎ*h3o¬F¡qqtQÅÅìÛäè<@Ár`ÊuVYœHfF²|v35'd’Ÿ°e;Kƒ>ãÜÿŠšP¢GxÜİJ¶ÓñÍ`Ç™¡5{Î‹øX#¾uï3Lıáni]5Dç›Ûå6<É§¢Mm?šMHèÕZO¯õİ%ÅQ2_'3^ÕqÜ ÄÓ-30_V¼ÉfìÃ×Š¿¥lbëhRê3WQ±sú¥ 6/ÍÈ<j§ı*K–ğIBşŸëtyïŞbKó ƒl™Ed®³t|?f—Ç24ùÀ%·48ëïƒŸûÅùñ°?¢Ú„+É•®uZ™UP¼ünn­Ö+‡äqG£!øÒ­xì+2ºšŞä“òWhuºXÒZù©.Ê€YeÓXÑ§Úî(*ôÅ¦™®Bö,nûM$»¤ƒ„ñsÓçEÇ=yøš9µÊûÎ'bO¹Ş‚‡‰ÇYĞ%J†‚¹®ï!ê£wö—?vâ²ƒ¡9ÕlõReÀ£9'ãöÉÇ$97Æ/³_s1 mV‰>È*Râ©\ª’Õ2¿²m¸`7µ|™Fø›ƒ{D6§<âäãwÙwiMÕ(ı½D‡µK|llU"wUD©Jş8ÄÊA³?/náêO˜lšBZ+´-œØfú„ ÂütFYj¿wë¨ÅÜcÍgù	`Ú]Œ Mê±X²¹DÆÀj©á)ÿjTñe4•-UØ_–yù*ÛV3jòS:™GÜ0!×åM<y—Ï°Ô¦*„Ñ¾İ¦Wæ=şÎ[ïaÛ„ xª”VôŠá×IRIİâºª.‰¨(m)Rtq5lç•¾…·j~W“)™[ÅÖ2Ñkeìî«Ûé©R
O)âY#{å‰î­Ù¹ÓW=J2UvsîâÇ´8QØR0:”ò²ôõ×„…ÂÒ¬j³0àv²Å17ïqƒƒCşÓ¯‚³Šª¤™ö ÌF¼)35ÍLe_ÅœÈEBG-ĞŸ[%n>éÇ¬oÀ²	Ûî«îèãiÿâı ÿáâÍÉşù°Pa¨\Š¤W‹1Üú9ŒË;µJâbØ›%5¢7ğu‰<Æñ•òR„:l‹Ñ§{e˜â÷:“^ëºgºãD5Î×qé– ZRù+|^d÷ñ5ÕşMòYÙ¦šÂ€ë}º\ec)æ:Kæ—©Ó9]VXÉÆî/øÊ¥j\Ë
w”©$<ş‹SÉù}dL›ÕhÜî#z¡W|($U²ä²ó½ûQ¾_ùsı1¦«¸ÉóÕÕP´ÒŞîÈáT…|3IW'·é’·‚Ù$çªŠœ¼ïŸ]õ¸Ï~ÔŞÍğ<à	bÚú±Û¼bè¸Ñùíıd6û”Œ¯»Rø¯üpjÖŸ%ÅŠ»‡†g:Ø È·ÔïC+5ØŒ IŠ~…‚hT5ÜìBB·$P¬$xŞAh<¹ü³ãrna,Pô“1CÁ¨QãdF§lIMò»9·r¦ÂºÀ¨ Ê)çºJmAí¤tE+9zô(7RTxˆ¡a_øF&Á1­ZB’³$8ZÜd…së²Ø0j5­CÚ^= ?ºáÀUzB£ Ø²:À•¼¯_Ûæ9‡,D‰Ì¥õ!íì èöûg£äô†§ƒããşYu	épöà&â“ÎÓ¥ß ¶	Ëi2NA‹Kác9D¶¿öªªˆñ´ÈYk|ÿ›_#š„(é,¹O'í?m».ÆÏÒ0f±ËÕdiOb=AŒ¥‚>-éàs°R¶(´½‡®mC[(£²ÔÄıå’İõw[O¼zşÒjƒÇÅ0¢¾ô¦l
é}ï/-7Euš9kŸÇÔ#±&ÒvÀ9¨Tî–­ÖG9h¬”!p
O„Á„ŞˆÙÆ4¹†éÑÛ¡a³:;ÒB:¶œ¸ áªöÅÇE´TÍJ[RÿTÆ‹AıõòRDYÁ$ÅÙ&2Û¿şåš³`U„W®ŸZßıp¯ärÕâV²?ÆåÅÛ5˜H}Ç_§£}ä¸äŒÙzö¬Ó’@:*ZsÑ¢øjŒ¹èüò+*ùÆXÏç‹şE2ß!¿(¾òg=%˜Ê4ìKÛ¨.ãëS¥ı~¦qˆÇÌH­°š§l="9³©nŒDº
¹”^Ozl›Ùïäûg(6E¸é$ui7h'—ğqî«*w—Àp:àÓR?/VCN&Î‡$ĞxĞ©bŞ\G&ÿly=”ª¦ÉÎ¬‚é·39‹ÜgHö‰µ!ƒGßK@ÈÈûĞî«¤ çr¾‰ygx=Öì»ûÀ¡!	ÌŠ³tœ/áÜî­'Yşœ«CÀrûäYí
Æ1JÈ0ÎM4élX¼ÔYZ¬gêèÒ2AıET"'èØ“:IÚB„S/½
g^¢–½–Eª2äÂ˜˜˜t²wïãÈJ‘r‹!Œ¸ı[”í?ƒOl„¦VÿĞD§e¥Òño !®†óÅÁk=i¸|a=–Û öŠú¥,ÉT5ıŞÙş»²6÷Ùmñ÷§ç{‡ƒı‹Ó“áh¨gÑzóíh8¿ŒóSÏü>j¯u¥A&u•™ Ç°<¡@«ØÑº5«Öóëc¶XFËÊ=eğ['Ş#ì«G\Gıc0­àKwCDÀsïÒi6Aö5¯g“wÉmz7}ˆ_³.ö’å€Í¥ˆéTQå8¹Í.jYTãõ¸‹‰
Ÿ
N¸"–¸²7~šû#fóŞbrP”¡J :’.#Ò/GÆÜš,¯éÌ]ÁÎàŠÑZİ`'ücáßríÛ8üOİ^İÚåí®Óú?Çäy{R¸ìîUr—Ò7[¿šïñâŒ`bïå¶Ğ‡ò¬¶ºBşŒÑGä¯Æ¯ŸE7ñSd‹3‹ë”Ô¾íµ dÀö ¹V˜1'ˆ/<¼¤·ôÍúFË“ÙJA£8)ZÕ†{x¤KVà&Äãÿ•ÃêMµ.¹y¦æÒCâT/Ó…0­$~‰tXÛ@a 0Ô˜ÿÛiÊ} À] ˜”NKR¬ŒP•¢&k–Lş¹ÍŒh•ë%¤\–ú†¡g·tdÉçÿ€BoÉÈ#%Ì\¡œ‘Ÿ¹–Í”“»äÚ2Ô&CxÈ¸Zõ§ÊûB”%¼}«ˆI.B_Gò¹B–=6gLæxbU“ğ~x–w›˜¿ı¶eWc°6€†1¡{¬!6«lŒU¥ÙÍ½R¦pïÓ¨i:¾~—Í£2ÃxÏÑ¦!V|>qh‡>¨/±2†?ÈÊkÃê8…I‰ 8ÑÕ)’ı#pCÉgØ¥'º+…2º½¾/(¸Ì[xfò
 -vDD@©l*Yo‹h…Ö«ÒØñS¢Ìº‹ææöGÆ`‰è]'º\£Ê‘U>6'Æ<dPÊ{ÔëdÊ´Bçğ”ò2µ]µ»6ìÍÖË}ş¶şÀ•¸Z%ã+÷ÈÿŸº™=†q]@ìûùq~›j¨hß7MZVfg±3ûš”C[%—\(˜GÑx´*n"brEèùc¸Ôg”\š¿%ñU‹Â®¢¼¿lmS*b…¦àHˆn'Å6¼ŸØ£OõZ¯`cĞ6&šİ^ĞEÊ#IlçZ'$<nM,-p=ĞôlLµ›Õ‰TáÉ˜ÔæÖ\U»X$¼Øg®eK$±²]±ë9ÕšO–˜!*­Jw²Lîàú¾YÀÀ|PÜ	ÍV…mÚk¤8÷×•»Vğ§µ/
†îšN¦?â0©N$ãğû^åäDv4^Éæ‘ş ütÅšóÃ°—0X9M¢¹=&ë9c0Ä·Ğö’"•»uÃÆ ~ì¶Úm«¢WôOîÉÓX1–9)Ô–©¶×:ÖÆÁMMŠ˜ÀÓnÛô†ACoŞú$áëeHóT`XÊb·<Q­.ÑŞØMŞµx`s-Ë›©ë†+	LÌ½À­ĞÍk—ŸzºÊòı‹íqIC- ¾Ø[b`Éß­ÌÖ³UÆåòPÆ«NO³ÏéDFén³ğĞ"ëğÄÈ1r{W(\@}~”ÍfY®(‰X± 3½IQ·ş
B°>0mGB˜±åR3ØËv!F Ù÷\¥İyˆ¸ãŒ§Éºàúà'nBxİôÎùX˜2
(Š@Øº †h¸ÊÆ×é²àÉë¶R7ö–İtÀ¢/9t#Š„Nf VZ±+'¹Ğ¦pŠcîçÍ1ê¾Ú%ó¶ìloùÑÎg¾X/ê Ø;ı›üŸYCœÂ}Ìr U@î„è!úàJò P€µ÷Æ4`:Åş‘B;Æ‹ÂÇx»ŒnÖió÷’áx™İàÏR¢Ç®¼:–¢‘çayõ;·¶ü£%ï?¤ªhAU4QˆIdãov”sJf·CM»í/­ºöì#³gl§ËŞlöOÎ\¼Ç¯Zv^L¹Ğâ™Ø†¸Kf³EÂÆ¿—­n’#®·ÙDyÿóÒ`©Z¶ ²
3e´ÜB*î÷óZÌû:4£l›ÃH¶Ì—–j{o	[yÎ…±K…ßæiÊZËn2Té¶	ws]¹1c§|õ`Œ>È…Ôv²	V€2T°ÄîşyçÅôÇPS„	Ä®wXØä_§p¬ô§D
—ùâû ÑŒ®tøŠF¦}´¾I&i¿M•\ı]*L{?g³5‹¨;²§J|·ª#oÁ–€30\ÈãøhAÈ9K¹$Œ2N‚-Ù›,I~×tAU!âß~ü_Î´¥ĞŞ—I"­¯‘º‹Ş-R•¯Š¿Öa¬hÑoFdëxxCg)çeC0©¶Äq¨È+ş61^~Äéû1x&:÷¶r½@(˜ÆË¸—6’KÓí@tÛ8"dp³˜É3ó}qîûƒàá4o–Ï/±!pËàétã5@ªn?„› !«»J¸X#íO.ÓştÊú÷èçL~6ßëeÚÖa·w*|Z#£A\DìEyVög)\‹Š˜>9DÆ¸`šPä>é(÷;]œjÇ$LXƒ©aTiıağa,8ÄÍ¢Ú”Èî‚>'$bÓŠ®„h%‘ZŠ¥ƒí……~Ykµ’Ø½%%ÕaKjŒŞ‹ğà–Å®áÙ8-¥ñUá(ØdkÑØßq4vá_åËà`±jµCliXõ:ìbhô«BŠ‹Hadêd½ú)”şüKÌ¦Œ®Ã—³ø^Íˆ9“Ôe/_`¸°QØh£ŠÍ¹éµñ»PªÎküœVÚZl1QRÈÓúAıÓšŒÚiKF ¯¦Àˆq1…Y,Øö$Ò_Yû–vîÇ®néóR×‘®ÔhÁq$u/o4iÙ¶ÿ¹°,áÎ;½Â,Ëımß'³µ³2Ê+µœŠŞlq•”¡¢v¾ÿòx¾˜¶`±W·&nPuÈ†MLf -€¶7pyáÕ–ûİ@À–høj¢¾Nï/¸N¢<7?\e«t«š”oØ-ú/nbS‹±)Ğw<A¹`zÏRP"İ¦‚© ñşË—:cßiQ› cl!i±İ1g“Båaí£×äJ˜é&}pûJËX'ŒÏrqğ˜@—fNëSM¦I²Ó:Fq§Yó. á&ƒ¡&µ+8ék´AÌIIwVû­©*—nx¬şÒÎp£HU<ÂÊå¥ºè=4µÜ0Eæ®*Ûï”eyÆ!)HgL>jÉuÇ¤¸SK]®¦cØJn
ØFìÊÇ8BÛ%<€hÕÇ§|µŸßÜ$ŒML®Ú»Fí(ïMä­×'d'§?+/·nY°–´xÂÃõa
Wªç×<Lˆöæ¥D£ÀW¤”Áß‘¯¥ßuö<e[¾F`û*yyr‚ÃÑö€Ö¤¤n~†²=‡!wÚ>èº3ãl1zÃ#\ŠşùyíBêmØ0U¬Ò©l~NÇÂ?”¯ñ°tœ®…#°q"ĞÚc=†Ÿ‰7Ezu+d')ÖŒù«UPp®™ÁÁƒÖ€CÕyH¯Ğša(Ù³Y§vÿBã*õ£ür®NïR±êtU73‚rõƒEÈ.#ÙÁêâ²<DıAátoC}ÓèïÑ:"M=ršHhÚš÷æ–Ù<2F¬šJ_ÌßAÚ@Ã%§ÁUQ»œ‚ö«$·š{Æˆe4#gä,¹g—ı`0è` h+­KVœqÅ5½B$vŸ¶ÜèÑ)Dl‘ÉØÒ2ægkUH0:SEhà`ó~N”¯a<”ô7Ü±ÓJŠd‹†İ«­¬NÎ p—.Õ&"å¬D`H†8ˆ¦•ßÍÕùŠé÷./³³¬X”‰V4†\A"êQ¥ƒ”·Vt‰*úcÔVâ5©E¹TÒÍ{Ê“Qœ"Ñ^Š8Z¾¸_JÊõq²GÏQ8#QÑ¼~£ç]Å6À“±½š_œü†:Å˜‚˜Ğı®’İBe˜Ö¦š¡"&Y±HVã+IÍÃ»sëü»•ŞR A1ÒÕ2Ub·4†Aq^¤K®ä¬²ûA¥ŸŒİüè.Ï‹XxUÚÑÒgŸè åã-¥Å?Ş´ÛVLqƒá¥¿ôG¾ËS¤ncŞ jsvc3ØÆËÒsğ’óõ",7omhõÉa°ÙÇq ò•No%’^ÉRÓä7Û´–oà²D©ÆÍ*FØ3A
wb—‹P†Ú‹İÖŠ¨{™Jº€Ó¢'nïîğ7 ¦ú0çéNÛÏí>”„üâjDĞ# 
¿zÇ×?†_©	?£­•`”!0;D8WïbváÅCHÉ¦UMˆNæ£dá«Î.:Iìöxş¦ıŞñ~ÿ0Üµ$²K: \@æêÍx%ƒ†½KŠ«U¢ÂI·¾ı†Íèù.Ö=éròÄ”S~ó­OR¹$$©úPp8"1Xú%«v3…u¼@HJÚÿå<8oBŠ;@vš¦+£ÛA8ù¸£Ş–~Rœ‡&àÇ¬a]èÙ°mB:÷_0Ë&¢,ôÚ{=÷­øMS$‰‘G!54Ì$ò‘!±­&˜$±2B‹¬ÂmÀüåòàÙúÿGßh‹DÅÏÓˆNİÚ6‚+Ih1œÉRø¯°­‘9XÕ…kÕ3oˆa0‚h#`”hîSÂulË9§!¾íİï3øÚGc£·¯oQ\y:9™U)cîe¶	U¢'³¼Q²ùê—_!_‘0¿„ß;¿Òe/Ë1˜‹[«éSaË‹¼°ZöStoÀO@"¥ÜwAŸª©”!Ÿ°S¿—ˆØåÁšğ4éø’wğ"¢ƒÚˆ€…/7ßòYŸ¯Ó¡gåœL<1ğ…³–$şQ5Xiñ{éæŞ3;äµòËö¯<û›ıå…|ñB¼ĞJ´µ&‹ö÷?l9Eù—^TÙ‚ÉáûºÊBøûsº€g‰şâc…_ş(BuJ6ùò*éßx0üµvz¶)|-gz«J—"W°°ü£Üür"`:J“b½L'*œ¢»Ôµˆ‹ú¾ÄúsÏ™ğr«ëÿÄ·°C€}V×òŸCV‚÷şÛ»3DÂîŞ	ì]Ñ„’¢aä)máyl°à±˜/SŒ^À{o›`52^g¼Ò¨QÇ-e5@w®)¦¢XªóÓP°³PµÍ" Xë„İ¹’Õ·¼aWÎÛ‚@·£ïÙr·2~‡ò»ıÙÕpPúçÚ\¼âWy6¾iv¹^"õÄ›`²€ï} ˜5ÓJÿå†A˜Éèº&0|B©,HSÓWÉø:8òá]unÖ“rÒ?]æÓl–*›£|Ú.õV¡-Sµôá1ô›©~“ˆ	yâß•q ¨;µî ø² Tõº°EÜ°-¹RM¥Ù2¹3ı·)Uî—Ë6X|ì¥WYiÓÑõŞ^œõ>ß^ô†{½ı¿½=;9?>Ø
Ñ*L¢+zoÔë®¦&ÂfÁ{=4Ö”½»?àFæü&¬”ci6¥vdm;ûÈ	Sº(e.o¹İë†{¶›5©„f£Ô£"¢ŒŞ¡y«bÕ ¬lü×]=Ü÷ÔJàD~Ä„MÃLÜÀØÙ(’-Ó»|y-u™ÆGtY^1J$£+R¥.³©Hÿ†
Sï×5ã¹—«|q.Ş‡¤K•¹NJG½'†âÓ«|…?ÁPVÆ¯§fp5"ùi©`.çİ1&“rÒvWÜe+Öƒk8Û…ÑU:l½ıü   ÿÿì}ks7²è÷ó+(Ø¢bš–d7›í¢%Úæ‰,iE:‰o*Å‘#qÖ$‡ËZÑ½{şûE7ƒGƒ!);ÉÉT*gğh4@£Ÿ|:Aúvlxß;¢\İ&¥%Äâ³Mç¸35†£Şhp<>>?õÏF¾3L‚…ÊT{Šx©¶Œ,úâòùÙëexvÚãY²\¦scç©;l%^$L^=O0
™‹v/'Ø}a1äGÎ³31rÆhbİ+N_JcˆHfİO`‚V%©ÌËh¡tURòš®…­Ï ™¤ÑYÈĞ-héæ*WÕ”Rvd¤ypØq‘¢”ê	å±·§¸¥R*#}ÓÅ´mM-d‚›aÕ¯wØú¢å!f™^Ôœ~Xß6È=`$”"É•A«]Ì<Œ;Éø‡ÁIÿ<$-àh€´ñZÑcŸÀHë–¶;„T4|ÓD¢w5·èe‰’,™X]KÈòˆ¼æıZ¢¼Ëjì½·±`á8/\Yl’0f‹Z(Ç¾¢#XÂIJíÁ˜0¼$£›áÓ¯¿Ş¾‰—è,ä2¿¶"dVbl“'mP o?0U:¤ª¢ø†µ|Ín9ä)y%óÌ~Û³ç˜ƒC
ìDø^]ß&M<ƒ•ÚÅ«Ó>ĞU>|ŸÎçS3ñ@ÕÇWÀAV°#w?¢¦	’q‡fj?¸
fx[vi£ÀˆŞ³ÃCY^0ŒsÛÇ#³­ÛÔœüHúk†]5}å´]KàÄmlÄÀ-8Ûä»ÌzÎÖƒº³•û¹º;½ßØs˜L¹»‡SnKâcKÏÅuÑ$¢á¨9÷ÅQL÷áò ÙGƒ´ïEış$CH2ˆ×È¼Ë±c˜’˜èğ—
=ş^7.Âmè:fsaÖ…¯ÃêšR§E	À€¡Öô·õ8BPÙ?¢ï¸F¢&5#’ËØÁé©wñw¯>öPtØ\azá\éaTä§¦ğìÂœÕ×É!„ÃC4èg•j±Êúÿ¨üù0ãúQ1?·îÇ‰ˆ»¡ˆŸCÇ'j¶etÈ@i<Ä œ¶†b	¡ƒqZd:îÀÇ»âø>şõî4½.ùÖUívuí¼'Úyo4bşÁÒp“¤ÚNDxf©ÂÂÄEr
Çkí…jM¬Ö·-;«@Æd­03=ÖN…~»À%?œİïÉİ8Ô51£~‚²/¾ä·U>ª·É¯¼Âæ0é€RKÑ‰(Ê>±«ÃßíÃÅ9ØŒÙï˜“h×Eä @ç¿ƒìOîæ…ˆãîQ¨²’7š›"N4í†Y	ˆe»O‚íbœS‘*Áh¾¾á§Á†EäS«iI|ÿ"¬‰o¾í-Õ½Cf²¿XÌóÅùp€ÚòW§½×ãÓş«Ş/¢‡ò¥?ò‡Ê­¬×—ó;¹È>ÙHÜÃ£Bt¾XTé.Bs()îÀ‹÷‚ ø!évIuÑáÌdÅ…ÄùW¿?œÇ#‘R.e›­NÃXöòúôLü†X¾<Ù•¯ ñ¸uÔ}jÇ¿6æ4’±—SÑô%øjâuLP­Ÿ'Ùš#¬‘lG¬ÉhwØíë±&•0Z§åE!o¾·÷ I±¡şÓŒ²3!å<Š˜Îjmí[PÃŞ¤St]¸åìİŒÿÃX€[õ×Ìçš¯·ÁŠ³º¼Ö1G¢‡òOEŸ‡Š›”ëìW·ó5…ù7^®£’-áBª*2V}ı4[~ Â®7Á–´åª7ÀòDã+Ï~›x‡¸¬z<ùa ›Ê¥ƒ1ËtÑñ m/fû»a,D:»õ
Wùè¨¼Áÿ-¼†:z„·Õ¯VÛÓÊj»:³\|[a3O§çë³|·b=)÷ §JÔeåñÔğy "Ìod3^¨Ó†êr'6 Â6Òc'DîXmi§
^ªø=
xMÃW_8zvZ”qééXQE]èÔÀî»†ŞééxØû¡œ‹W]¬ÃQ÷›kvóö Ü8ÁhêêI8º¨!A.Õkr\ø%8c¦*mÈÏãÇ5Ùï€Ê¨,wYqœÌçÃò2f{ü¸Â”¼@Ç˜ÆÔÆŸ|ÑÂˆ³Vÿ­›‚–ö–1‹ÁïÚ_PŸA‚ª
’rEo;åS—Ò^Ï7¿:ıË#­ŸPxàùè,±@×µxÖ$¿SE­>¯÷×¹½ıRU#wR‰)D¸¾_£İ¨_êtãacy9
©:Ë¥³Âfm*,.Jyz;dÆº˜ÔFÄõ¤l dHÆÃ5o§PEñb'I™´¦ğ¿ê2lÙ“1d[è{,¶²¡òVÇ?úZVğò—m“|…­øƒÍ¯Î³ÏöĞ˜ã!_éM9ßB™ÉD_¸[ÊŒ%2#aÅo}
V[0«3ë[+%%æmâ©¬s5n‘ƒ£&Ö<¤[·2ãWEuh8E}”!K:~¥V9Å/V>[* .¤³÷Tæ2Uûç'_|Q÷Q½E_¾{§5¬Šî*|×Í=ô*óü[)B¾ º¯¾	èokÆ	ŠP×jzX¨,R¾şÔiÍÓõŠ¿»ë(èÍ™E"NahAf¢a[C–-àŠí:¸]!§ª÷…wuÑ´şêŞ&à‘¹<t‹óÍ†+d3˜ğY1„¹”?Cë²Ş6m7õ%-ÿÔÒ WBs2Õ
Z¹ó”ˆ‰v­ôZÉ:\TÃÃ	…O°êªÔÆ?éKM·ÚZ=¶$«¼(‡ê&UWIHfÉ”Ç€…­tÎ[j[-Ç5…Ø«Ì9w{ôë«Wº0ù¢fõù¦E¿ßÔ¤´T	-äIvÔàªLõ¾§HTJÆD“!…'¼İ^Ø¢y¸ïÖ"ôs ±!ò¬E±ê©Ø†ò£0Î"CÛ†’Wú»­à¦o<^¿Â(½ŸÙïˆû·ş(Jˆz\‚â¹jŒ~¼ÄÏg ƒJ¡WaTKŞ¬¡Ï1		{Â–; U¨Ft+ã%ÚVXï.4Ù‘Ül¹9iÑ¢:ËÚP*æ"r1È4|°Ü£8£î{AÂyRíéGñdò«Tyzd<(Yô¯	³±;_c†œ2®5¸AÀ‰î"ùµÍàì@ûq«ó8[OØá/+uZ<İÒß®õ›œ™·µ±TClÊáImİÄ7An}òMˆ0Óüö]ÚÛÊª‹œø î‘Ê Š¼Kz€7EI˜c'Jå;¾ŒÙ*¶Ë›Zõ¬µ›¹Tµ¨[p	±ŞÖ™ ®İhæªò­4A¹á8Ó¨%è›V)°5‘ú¢P®]š†¤Dr¹Yğ}ğÇw¼EüÔWEk¤çe­VU˜pnü4SWY*ñTF+3”(5\ZKÆ™¨?ıUvÍ	Ï¶)yá©`¨|n)[öMQ‹E>ë!c*qÁzª.âÇT_r‹&«5Ù]AúLçk)l¸/¿ÕxEWÓ¶iúd	Cø[Vqr¨<âõë ˜¥O\ÄT0?v=èŞ=l14u£S »o»•kOëÖÕ[¨wÅS3HŞ-Ğ"`éLŞ'Ù'MÂû“TtEt…k9Ô×œñ±ì ÷ÕP1™®YÂÇª®Û’q1=2."ªÃu]‡kTGÄ¯HQ_Õu|%õõ½ò+XİÄr£ÍPUæ54ã~Ê1,%(b(3ÖßU>W÷Ñ°'‚¨§ÒŠk¾ŞôéQ”SÍ*í©ëQrEDq';ŒüŠàÒâ{Øi€¦¡u:
ƒKæ‘­hUfÅT˜Ûd%—ÑU¨|
tûQÉ;Y;y]’-Æ´6¢Y•×¢bÓâgìê—®Hú}z×®:LyøÉ TW´™	Ü7ìgOŠÃlFœu5<|ë-Úñ<[ñmû)äĞt	=jxÛxE6ìóŠ¬ŞD‘eÛ`­¢ÀÓ´ö'ÔùK‘#Z	Ôœz„©€J<lŞ3÷Ë…niÑŠ5µ5ä@¹U7óø…§öx‘PÅÃ*ï.€šË?JùÇZşqÕ	¶½Ê–KÈ)·Š)†YvÔ2æĞäD¸7ÀOÔ˜__,6Ÿ2ûèf…b·µÀá}-ÒÉ9 #6öà°½Ş˜i®Ñ—Ô‰å[	Y^¢k¡-ˆmFK5İ÷¾n¹÷=jq…C6c(¨ƒ±u¸U1¼BÄHéB÷åùhtş¶ÜFCŠ.ÕWsˆLı<<
½ıÕŞ¬U‡•øŸ¿êöÍº¯n´ËßzšV™„¶—Pl•<Î›o»ÃÃmÖïîÈĞ·0êâú[²–î£gÚ˜4«¶UMğs`Üã´ãƒí®Âd´ÚÚù½t°ô„Î£@Û£B³şŞ=s+v-€Xõš{¨ÓéşŞì¤ş¨·÷\ŞM¶l"¤ÿ´™H0,òÕàĞI8<b
Šˆ|ƒ1nÎ!PœzV:H³J€9JŠÑ‚;e,/^ŠÒói>€wfÃ÷Gft(…|.Â¹Â0Şûì× "ö°ZXj87SßY@×2gz°7)s> L¨‡¶¹0ÉÏ˜0kõÚh44Å6XÍ,+îbÖXêÅj¡¯63…È&!$B˜îÛñU²ĞÕ¨Ò˜4GUk§Ñ˜Íì8¦F—æmi·éı7êÜÚÃ9Jİì”¼ï™èÁRL[”P³Á"QrTÅ
ÑÃŞÄ¿ºH•*e ±ë¨Ñ]¼†¶pšâb‚Âã½…Ï’â|S^!3ˆNV¿£)q B…Í^¦b/;ÚöÃ‰“«Æ„ÂƒÙß“HoÁÜ=¡`ôçŞ•Ø„N•˜qlËŸ)ó-1ŠWÜÖôZÃàUö ò]R¶/û“
¯¿}ïYµU§±ú«wÍ¶ô.Û†wóŞBp¶c‘¡oÑg*˜yƒì”* ×GW¿†Ç-ã&áà&<°×«y‚âŞº¨®úØDL°F£Ûì¡˜OB(‰cA3FµyÄ¨&xPµö„‰° /è‘QÏÆì)Û8iéO#‡-ı¹Wç­„Q\d»;ušİƒ—ş|:O-ùD_Ò·0/·Ë?Íø£F]Ïæí"Úç~cË©^ÌX¤¶8Én8ÎÎ"‰ò7ĞŸ{•F5ˆB=Ñn$f£ªçs>‘”Cëê$úÏmo£Ÿ,º¡…Ø«VeëKûoáŸ.0¢ı¡Òv‹ğ}¤x«æÜ?y©?y©?y©?y)ÕËŸ¼TlòRŸóh·ârTŸí¿c¾ŠüâÙÌ+¶/š2aáv@Wş
*ÕÄU1]¸Ô‹òbpr—È9Pù2L€¹CØ2Ø4³­É<MÖÍòÔhHÔdÏ¿,jK4>ÙZc»áÑsvLzBR:€Æ¸ÓHm#…A3 ¸/ËFxê\á	ŞõäÅs…|ÂÔdàv7z²ô¿eŠ²@ı“¦š
% öÔÛ¸p[†sv0‘òÂ‡­ŸDè°“Àá„÷*hø´B†¨ë×½>…`¡ÉÊüsïş£îİ¾HKáì6äˆ[P8Æªm±>Z×.{ã}ÿŞ÷ü­÷û=ïõ÷¶ÏÂğ©QQ¶şx1ŞşËüeü$ã‚Õ‘ˆL€NkÕ>W¹ªé9‹<áI×Ägzó–àVÿÀÎABO›/á´7j§5¼+Êtq<Ï'º›„˜~›ÍÙÖHJ«\tbâXä8›mõ¶«¡ıŠ˜,æ¾¡ØîuèÛ:LWìü²2UÑ%ö6³ÙıŒ)[§·ùúC!’sl¢ÕÎ^€û÷&›|`›ğ:¢ÿ|¾ŞJoƒ{÷&»†tU#+-QS ­föZºÈÿ•í8§¡½€ÇØ½ÜŞä»úLKÍF Ûİàe¾zGÀ®½Ş7øZÓ{èATø¶K9iÛˆïûÕÇ^÷‘h9;ÒØ"/ (kÃŸ§È¾‡èéf/£¼ÊåÎœùeßc2[ßËP¶J­ËÙ4¹{qe‰Ğÿ‘:?·WÍŸ¼(C-H¶ô¾AK~O¿ˆú÷‘úÃa¯³_Óé÷éİU¬'R˜ÁæÛïš„NãquçùšLÍØ]}z/Úøw—5>æqQ´8³¬¤<‚pé˜±¶ëlŞâ­%#Oü»MnŸ9Ğ+)²S¸fñ!ÛÉvb q¯TTÔ¹G-b"Ü¼®‚¡Ìİ”1+†{àÚóÛ%D1J®æ©¾ø‰ÏİešNá·£¸H–éÜs»bóU‚ çJDê°”[€ß]a ¬ŸÂ¸qáë—›+Ö9ªëİ†®d„€&mÉ&„ öH¶ÖÖ‘$ÒgŒr¹æÕu‘G±íŠ¼cn &En€) 5#Sk64ÎE"Ÿ"¡&Äá;°é{¶6‘!" Š¢í˜‚Scä2oØáÍÚÚš‘{WA»ä]ÍêÁA¼•÷¹ÇÓ©»ÙÈ¼5çµsVù,ğÌ5ŞĞggˆFŒêŠJŒ·÷™Ä·êhBˆ™ ãçã/`^nÀ@é‡şåpp~Ö|?œ€Œããó“ş°;Bç|>‡ ˆ÷,ÏŠt¸Y­`Ìy0HT°"ı$½ÚÜHõBAcâ¾x¬~k#Z­³Œ8TÀÓ¬xÅÀ€Heƒ›%CH[œ½E–S7Á\§åf½l©ØŞò\Ñ¾ÙŞ[xĞŠ3FÓ‹j9†õ÷f–DI0ã¬×Á6éşQç_%Ó´"X•ç¶ã5uXõ4”7IKŸ/Å¶Šº«|İàb/‡«tÒÁCz†‹I{mOCCğ%Çá0ß¬')4wµFA[qËrp}Æ¸ƒtêœªZÖÒù¡€x>U]“ôÓ6¥EqF[‚©¥sæëúr-¢"ô¯:^³0Ì“ñK}°İZƒ°@\$Ríw[õß1ŠõêNß;‡„šµ
d	â4Y&¯’µˆ˜ÀgîGV~•¬8OÄ_1æS‹Ú/æ¹âéy!®¾ 6“v»¦J,t¤BàÁy¼œX[\ƒ>÷Ïw>·l7Q­Pº™çWÉœoV\(ï\µúÌ™wút`&ª…{6Wæ¾+RCÀ­½f8˜{õ‰Ãl±bü±6ªLíŒ4Ø>Ì¯¸àQVVe½Êq`ßY90½UË†-®aİ-4ØÖ¿ã‹C<Òİ×İyº¼	Ä€,U‹’7ş÷¿²ü×…¬}¤tàî'áĞàÅ,¿å»p¯L0—Æ¼ëm¦YÉìùëmUvnëØÒT¤Ç_((8ehØò–±ãá»×¯ûCpzJ. t´Zvš“5FÄ#û´ëC.Íğ(^xÿg»İ¼Ê×›ÅY9ëO³2áwAÆŞµµâatÂ0—á6æ®¾â¥ÁDŒR¾lMü6YßdK½ÃúNy
^—Ê»Ì¨Ø{ÆÇ°»ç×OÙåóï>ÛÉ†)µş0èúºB¢è5D`Õ½!İ«,O«´ÔÀ¬ã{hjÀˆ(c£œƒšDÙ&7±u€g)[ÄÂQ•îÎ æğæ
şnsğöGw[…SµP²ïû˜œ×çgñ3Caa%rß©`Aß¦öe°+s×ÄC©Á6¢ĞÈé1ÆOûèôòâ¸Û=\([öÇ`ysùôquÀË İŸ;¼-È’x£ÈhNË
H]Û*ŞuW³|™¡¡bÌv½oä[=´´Co±\¯%»(*	ŞI¶H— qkËk”Î£[ iw˜G8º[q•¹M¯+€ìkˆNÚmó0ÑªQI™¯üÇ»•›‚->y¿²;Ó/8w‹w”§©Â³ÏÁT€r™ü"İæÉóåuv³Y'Üš³ËîÂŒ˜ieh|î_úg£FE:í{}¶Ÿ|Ã “¦ÆœiÁ°£ôX*>ú¢’äuœkrñÎ}Ú½ráµ\6£.ç†ìÎ–õşÃ™f£¸£@õëNMr²À^Ô¦šìs9Zª3®.ìƒL—‹‹XÍ†¥ÀKV•i“âÛi_Tî!y¡­hb 
;bÖMİRÔXÃ‹4°	·©Äæ¾ó”LSÈzFiÌ¬1ù˜ ux0Ñjá¬¸ÈW›ÕíÚh“È–«W=Šy*â,qSÈ¨fPH¬9§·7lÏJØû2ıÜ&Øì?`8¹æ5µÄ!Óÿ6f‰°ÁœK!ƒñMùbÜ±VxtÕ(zË&À8•Ë­§iÎbÏ8xf¿¥(Q9(dÜA!³ÒÊ9ip[™ßq¡Ç²ÙfÖj—¯o‰Æé^Í¬fãWßšsÜRXIèÎ^˜ØÚ7ÈÀl."OÙ¢Ò[óù˜&+¶u(G®KñFhåU-Úv‹lÓÛd	Z’.×rÙ)ğ\§İ^)­œã»O<xsjú]eé´â6Gî™¶íé…»Ï  [ÌßzÊ˜“ŠŒ_VÈ„ `¤[O;­èP¿p$ğí.ó2»¾ƒ­Œ§Êœ¶ëÒÂ“Lÿµ)ƒ+d*oÒ9Ú“¥V¥‹	ÅŞQëÂ‘¯y€‚ğÂ<ê 0«áËëÍRËËtîÅ¤İ=˜P©½WÜS½)É}¬9	P36İAŠ©ƒøÏZYÁ6,á“€b7Ú¬gµ)Q ¡Øû£X~Ë­êåÂ·ÇÚVÁö±O8pù6§  Ö§üÎÕ(A¿Õ_ÂnÊ÷ûƒ8tqß•z=4ÙLûïµ_£±±N'ù‚]Î§);0¦¸i'¨‘O/O?ºš´b.i¶E./“b0(OBâ_)Ü¥c
c_.AhåÕéWö±z	uÍô^¼Ş’´,,d*ÁOv¯­5QÃŠ’¡wÆ%R>W’§h¥€®é¯Ó=JÛÆ†]ò©ÛdÛmÇ‡’A¶4mš6Ñ’q( >ŒØœıëk`!UËEÉz¹0zg®ñNØŒ4œ½¬|ıÌt›ïñûÜJâŠë[F3â	ØÏïËU\×ËÁÃßi­„	€³ï¥«QÀ§Z»
Õ={ÇÃ¹‡ş#›ó øoKêŠ™p})¹áBà“·0¢…¿*UÉ‹ÖWß`k‡¿úà{Hyœ,Òuò!’í7x<íO($b{LÅúP\Vd¬#Æzãu,MØ?_z3÷İaäüÀø£‘„w*öšb÷Qx^õÔ|ÿfvˆt±*‘‚Wkv~ëô [î}n<¤õh#WÏwÏjo´ª5%C÷Kéøõœ;ªÀ#ÅzgÇ±ºR·ÃÇïCG?÷ĞÖ¼ÖBgÒ‘¬Vt¨¢x•ÃSAÜ!Oõ/¿>‚º×ã-#Ã™¤²—u@|¢i;ĞF½Ñ ¡>,ßnæeÆ7É{%	ÂŞÒfÉş¤šßÕø±úy†£¤,¦ÁEwëaF•vĞµ½Z«Œa]ÀÖ\ÿÁÖ¯à!Æ«w§ê
©?j‹^&Ëe:,çìc8İäç¤UO;½Ñøíùp´5½:È+€f”T—ÉVŸÂ®ÚN*#kê†A„Ş<Ú:ÍÏç™AI÷µ&KB4Æèı¼ºß´¹ıS‰¾{pîÆİÎÚñHÅü+İ°0yî³i&aäV	‡f¼ÆA>³ÖÃg†¹Ÿ ©ù)*CTkê-m»®cúñEå¦ëº›@ı>ÖŒæêÖÀB	,D[¿½[»!ôañˆ"Ëäcv“ëˆ!6¼~şÛ ÜµĞÃd”\‘nÒ
«ÊÈ±òÁ#Ëî`Ø(Ñ×£F•jˆ¿!RÜA`ğ9Éƒ¿Î½ØŞ”Ö3×Ÿñ:ûU\ÁØÉÀÏhgFÚ~£òõ=Ö4bì˜S¶ˆ¡:2¦”¸³sŒ$Gl¿+™aCÍ$ù,T<İn6a³¦fgJ¼Ñ½=ı¶Öùò2¬Sp¼?xÖzô„´T.Ê²6Ï äiÈ×5„Ú°òï£«0mƒQ×ófcòSGÁÁ?TPPhÆ4Ù¬Ğ›}3Ÿ§¥t=³	u2K'¸(ÚqFĞÌ®É?ì¦¿¹šgî£¿Nÿ½I‹RB`;ŞCC‡:¨Yå‘BôÂƒŠYı5ƒˆ†°ÿ&€&UËÁºG‰·¸ my³Èb€Ò6xÂ­çx¼ƒ9ÿ§äÿ¬ù?Wö0ªu2‰1ÜBM¾§½”áˆ¼áÀ•[:V»›Ó?êjÁÚaÃãÄ	ËW5² Ş
'h€Ã!
™áØ5š[áìÃš†Ãéô¬ÂâüŠPZaôÔqÁ©5ëj*«rµˆ³W˜ÂdSG³øÌNpOí›uÂ#Y1„tÅzUIßÙOÕÜkşWwt~ÑúúÙÛ©É †qàù|S¦¯UÓ²“¿¨fßœ_şÏùÙ¨w:~}Ùûa0z?~Û~Â7ÛDÊl’ÌCÍşĞ¿FİÃ#1ßµ@ÔDÆoÊºÇı³Qÿr\5ñmX–„ÿ™³<WryÄ	M•Y1FsVA¿ÁoÍÛÚ/Gûã¹¼~3Šƒgk»Ô[ñiß•Ò NÓëd3/·µQë@’–E®Q„Ã–^ #ÂÇ ª„f'¬ØQÑ™Ô´ÊÅ»ıU× ğáV^Oß—XJkOnQèl·¯Mì x ÁÎ„ZhŞ‘Šy¼¾}‘ñËóÑèüíN0* ÷`Üré2j±ìf@«/]À_Îï‘g)¤‚I
cS vÛ$•pIZ¥BZÓ¦ÕpÌbWœB	b%~È{4‰Õz~'tßÍzFßp¢„u­á	—ù·´hgö'æèßiQ¢¡İkÍ‚ºoyxU_ ¦tØ5@U1]É.ÏA} ì¬¸L'ùzzœ­'óô|-ÓyDÌšĞ£g[Ø½q»Ä¿…düú÷ÊQğè¥’ÿ+ã©H.S'ã}CŞl:Bİ5ÎJ:Åè¯uŸƒù{$zí„ú¦ş1Ï&©÷ëœK_&ËQ³%½2¾9)"b†jÅ`K/	ˆœig£¡>Z‰^ô"Ùò:ÖÜ÷{ZDYë6mÛä<j£}DûºõÍNj÷z×$­ÿ(_Êö4óıÈ>Ä}È»ûGs:€Çmº?Ê_¢LôÚÔì´M×0î"º¼,x?,V¼ÒYë£y”
)kå ÏÏÀ*ô'åömZ®³Iá i•Ïç½éô“÷â†ä—rG ŠPƒÂ]Úê\ì¨v:ÚaùË:ÙÊ[PPŸ¬Š£ÒÉ«ØNUaR)A¸u^ÇÍÔxTx\{§=îeäÔZU®îF­ÇQ†(¹Ê½Ó<{šl–“™Š2F/ÍIyØ"Ú8Ä€ÕéšÓNeŠ•Ìçù-DÊ‘gíyxü¸P¦?¶PÍ}ôù´H/}{ì¨Kz—Ùe"C{ô2h6ê0œåË3¶·ŒD¨uD0&qt.LB‡-İe´ïÛwÖ ªíĞ.I*“yÑ²VÌi¶ºIñ¸q£ä¦P¬œvª¾j—ºN ÓV­BTOlÄÛ«KQã©R‹œšÎu»e‡•JpjT©†]¸÷æÚtæ©¶° ZÆkÅÜm,6ò–“]W{¨¾~(×*ÓZhíı1c2Å¾RŸ}Áe!}¹Np¾WGÀ;„ ‚Hô°÷µb¸•bf‘­:e¸%Òi˜µ…¶ßTEëÕÅÍ¢jsG¾ÊHƒz_nL„º_!í$÷T-&š„m¼Y¾æœ©ÔnO³b•”“Ù÷é]ÿ#£ü£•~$Î<ÌO„Âı¹xàÅd½î÷ı÷dbü²wü=ÂªxoR¥š~‚8óõm²†¡j,¨‘¢“* ‘Le4O@+¢ç"+
Ÿõ·°%píšLú4LÔqŒygid r%1Œœ¥·ò§c
¡ÊÉ$y³NØYyÃˆ 3œÈ0øU3¤ÄÔNĞô¢zõmeAaÀS3I@fµ·¹˜c—lLào%ä¡4^¶…ÑQÄly:†XèB§¸º²İö»Å®b&Âp0ôø>J»˜{ä¬}=ÖD¡½Ù-İÕ£pw-áç›g¬µ'áŞ‡[ö#ÆÿvÜ`°y;C°+‡xÿ5)=B¡‚ñ¹mzUÑ¯\^1X6¹¦}F'p©2¹—6öYSü|yÌæîœ?)ãÓì:tŞ ŒÏİ†ÀdÉ<¿Lùî®àïŸ½{şÎ‰8DÂ‚ù0»Ln-¢áKHÓ¾ìøG··)gùúM6¦Ë“|£ÀæòNâ kNV€®+DyTùî®¦sJRD6Ï[mi”¿PDÜpªd¹¶æNÚ‚Ã@ğ&öCô ë²Ìz¡º')^s67Œm)«HhÁk]¸ËöIz<Ï‹@Æ2]ÚÄ¯°è{ü¼äÉ&h˜ªp}…^åĞS¶·¼ÃpnYíE÷&»I`®0ç”ŞLVÀM“íé€\K«¿HE}‡Ç±FÖ:ññzı‘75Î´ÚÂŒ–Ó±Ì(Erøb7Ú,!aìË»—y^ õ 1®‹¼@~ô2]ä‘©½La½"ßWÈZÆ°İ…ÁSLëİø¤y‡$üÀóÅ*A¦Sñ3º“X§uY]T®3¶ÉLÇól‘1¼@‡İÅ48>I¶lSôk	²ÙÈ;şt§×ir¯é°d;nº.ÆŒÓç*3Ÿ“œ6vØÓ‘é$â_Ê=µ„Wl½Oª&öÕ.‡C§Uˆõòè¯‡ò_ŸæĞjÙñ‹¾YQğR-o
¶‡ÓmWŒ¸¡6?40şã3¯Ä¿|Ät¡öƒiëAëa+æBâD{öï(×·ßğ0*è¿€¼Ê×©–ßoE?H²™!©S´;¼èû?Ÿ¾~èW5 vt·bÈ$Å&à*ŸCÔÒ ét` NÙÓ©¾u{§ƒ×gì:ìŸØ‰ˆ`=‰‚ƒ¤†×â³Ä½¤<,%«"ju1@xé™¸šâ9;reõC/!y™'®&õlğ#8aœÂvx™&“Y:åZëá,MËn¾J—|[f„Ë7z}“g·¼³C	Îw —eoµš‹íœ-_dÇÙTJû›zMíÆ;¾F¯m½Ê¤Óúäçá(Üó–4ƒÍ8¥—”[ì6(r8GöíÃm+Æ˜VÅ²÷pÌ†Àë¸û,oıö—°À}8ìnK;OÑÃ'(øóà¯öÇ¾XBYÜšûR3A†VĞ˜ëşr²¾[12Åûƒæ{r•İ¨FBÒ«Zºœùóoåµ)«7:›%O¢èÍ÷ç"<[§Bz_»=ÂSïOæÅ²nÿ‰ÉšF§ãT/qÉ}Ğhû²İvP	[!ë©}yC[n'ğĞSÒ2j×˜{nˆäyÀóf(v];ÓÕœ±Š “­;>z"Äz5úÙá~í´|wõu=Ç)£Ó¢/“ÄÙxğİcV‚d şøx’“Lïs•\¦ÿJ}<ÓçF±·4ÂÁ>x@¹Sƒ­cO¢¥ãhv' ş`üû5»æWÜk‰nËâ˜LıyîÊïå rS»adäQƒ%tùp(ïQÔûÅ6@{™Ëå§Ë"_;¡´°@OZ@²‡¨Zt’åØ±H6Û×©ÃÑ$Ó§8«ş^&†²•
°YÎî­ÃD5¦™Ù¼Ê' ÿğ3c\ÂoÕ¦îH×Á‚‡ûEºU-\'Ù]a3W&¨^è xfwyÜ”~ş¥µªê ^½ºY'Ë’7PØÊ™ ó7à °ùÈÂæ×uæ.Î7pÜ¸ûVøæ—I‘j})…İeÿŸïúÃ¦ª÷õ/Ïz§ãáèü²÷ÚñûS³Y–ÉdÖc»uĞtÊ*Š±Ugy™Ë ]Ü†e¾†<Kµ2ĞŠn4‰ÍƒÛß»Á­«¿í´#g«™éF½ã7ìï3öï.FV+_òÈd‘ÃµGu/äèò“¿#xòÍ.# ç…Ûeµu…T¦9Ô?ı|ô r‘L>€´ˆ‡rÑ¿|;b>—×—½³;kí#nKÑ0dáâ©ãµï;fj:êV• }5Q8cŸI¤Ä :_\›İ¥5D	
Ó–„b’+É6¦€S|¯VgŒ‚H¦»jO&Š‹i±Z_g9º<¡Y>H®Á´´¦£3\ö1åJˆ†ı¯ÒåPÖ0|	ÎºÓºe“†¢Èr}p a0)†^A…7öH8—Ó]­sğYweï]¶ƒaô..NÇ<3îIÔœÇÃşh48{=¹²có€¸8´ß­³î*YiûÁŠÓà·pWx6A!ßiy˜»IõFè&q¥ò:0Ím¯ Š-c×„uõ›æu›œ$ ÿë$Å+]+èÚù*›§§ŒèÓvƒ4ÑaòáÆÿÈçü{F*„ÈÒhy–ßF|ö~ÿw{+©Û~vÙ²-T¯¸Ô‰ë~Ò)ß¾Û	ş3^ÁîµOGİçı³ñqïmÿ²÷[1:lEİGO¨Ãûèé¡fş ""56ªô—–­„ï˜—¶ÉÂi,Á’ºÑ«ĞH	”´¾£ğÉ>Ğ’ìƒğçdûC\O¸<}¡.š`€Tª}ãtçÂ›`6\Èà¯°Ó³Æußäÿ¹Äÿ5˜ÑcŠ{òœ6¬¹ş­•1U•†µ©’2*c‡ ØNkÃ¾–×¹¾å».®‰1Û#çÅ8ù˜dsenâªJQ[’,öz‡0:4İlz?î8u¢†Š;?I—YJŠä¬ih‰“)¦™„tG­XÔ±™dè•¸éc[Ëäşó÷JG¥µuÚlP–pQ½ç<…áĞø»nYÎÇ×Yş‘G!Œ Ğ¬6³øV6Dt5Z9­(ÖÀSlƒG·•Ù"…$«OƒI²P¤òÓİ±ØbËÒ‘®PYEQ86 ´U5RFîg ´„t¯c^ªÁX…2Ó^>ª^73½Œ5DÏRÑMØ)»Y´^´tl´¾mQT`‚‰`°³Xl–>ÀˆtÕ u-?¤ÓñDVg|M;3P­2{‰êûµ¯9ÛcÓÀ½(|×óä¦xÚúKëéÑWß Xñ¨ÓòĞ@İFEkp^¤d;°wx;×˜Cû mÒ/±OÅt©väm†êîÜş.=´Ê©õ›¬(ó5¥-Á&ÏÆ"¡ù³ê,™$Ëññ„[,iî`ÊlTgÅÊÿÁöĞ¬BBİpà<ÂrÊ­z85ò¾hñ"n­8†Áş±-şæƒ!
ø/›³0>†@Œ°ÃÖüÎ\ê 5zeXîOQNÛ|½#y›Í1d ›ÇwBğfwv¾ñ—PYX½Î×~{ÔCÚõPI‹F8á"tà`kŒµÕŸúê7»x0í¶äO‚N °'ö
4ØñcCÅ{‰qÛ.±£Çñvôş¢?>=ïÎ^×tFt‹ŞNz#Ì¬]áõàÕh<zÓÛ¿»h\[È/ŞœÎU¾{ıD,Ík"ÀÃQïrèVË
åå)=hCk…^­ü2PKéÔŒá¬…¦Ù?Ë±PfÅÁ*j-¬õHn2ÜX¾}HáV.U(¿æaøDQ~˜–<–öûYË³H]XT»Eê¦i•x?zÃ^…&0áå¼blH@JP×6WóçÌûU›Í-p1œåëµ$Ÿ%ô†fgŸ5uäïµÄ9N/U4¦6ŠL­%$İãŠÑäÈYPÁšXÔ6ºë²òÉfÁ½‚5i¬sÀo
Æ’+VÀD¶ŠİŸ,Á=5,ñ„Ñva`–üœ¹à‹©/’rV;í 8i]Uüˆl7L"8ª›şšO]éUØ ó·èÏ6ˆHÄ,=ÀÓıÆ†³õhhJ‡‡ÔcFÌua­»8R§LÕaŠŒá£EÁ-n¯ÇÌ•—İ2?ÍoÓõqÆú]Æì Wn?`DÊñQ£–d7éõGçØğ´Şÿº˜oÕõ×{vfû,½}]ómÖk±úêßò¬«åıá_+ï‡ôf«!ş5rˆµ¥Â%Â_,_ùa¡ëos¹öKœç79Ï†×Ô/·`¾©]S94u+o,/¨m5â¢J©vVÿº¹F­0bkA„_#. „^—D?O¶¾ÿÆ	rô[&Å²	Jk@`¬·_‰JÍ]4J’âŠEı—İø¢êğ¾o©÷y+ûÔ7²úÛ˜ç¤i4ù¨ûU£»Õ½İ«ö§jvŸŠ½|Ä’BVÄC[ä%"Ì¼2ÙúaÉqÈm
ËÙ(—f¡~TıæñÈËXİö~Ù²/ZÄ L@ão'q·’m¯¾õT–sÔÿDî®Íùø½¨j8{?W¿#Gïçæ}¾ÒÇû_æ_”S2BĞn:©âWt“µsç›\øtøéÖ¬ú¢^§¥Ñ¼Ğª-ÕĞªB®Ìuúñ8Ç3Œôy,rlNªW•åä¢¬*;*YÏ^ÀãUjöW¼¸*ÒıÁHàl©…¹ë$OsêÛĞı'x±BËöiú+¹%Aø”ŸjëãÊ¿µ™d¢À¶«Æe¡Õ$¶uàYé*DaR¼[bºıì€¹B±ùŠ:Z7Öïgö›·É
Àrwv¨Ç‰Õn¹fûÇ¿ŒèÎ6&/Ú¸×èGî˜E·€ĞU‡^C_?r>\<Tí6’š»°Vá`ZüŒôóKÿ9¿ş>Uì˜¼Ç²ÑÔîíö¬×Ë`+¬&Í„ß÷4ÈŠ&ßß¼ÁÜí…{úLNZ.¢V“†©<5ÓÛzÔòå‰áp†oŞ›Jw’ˆ £5CAsI#u„4Oƒ=°CÛMÉÊî¶{Î@£kæiŞ® -Ú*_BÔ H48êÿ4
	T¤DÆ)"¦¼:0ĞÜÏ›şÙ^æy™‹åt0VÃsœ¯î¶G.M7
íŸ@`m¼¥›x“Cêp¯nYM‘V®÷…,›N–`a'/×º1cf³gU=ÚqŞX£Ür_Aã„n>~Ó?ywÚ?é´@Z­n¬ä+ò‡ÇÅEš®ÏrÁ¶;¡:ô‘úG‚f©Ì« §QmíÕŒ—ºHx\›Ò†fÛÙö‰ñ`gŠÈO°bÌ ¨^5YJãeİİ1ŠÇ·Ägí uºı"Ù‡äé÷D}Ú/OææR›%0+–Ó-P%i¤ÇùCvÅ‡}”<kµåËÃhÿcs|˜®?‚D~øağò²7:¿û—?}áZ¹ÛÇUm3ùÈ²]şGÚ~zDe¼u'M>ãC—ğ)«qöÀÑĞ±ì»ÿäYvãYv@´Ÿş7ğ/÷†¸{ãe>üñ2Í€ığ2ŞoÉË4Ÿí{àejÁËØŸQs{~Ğäw´Ğ¾ïÀí2#./Ô|>­€ıl/Ü¨eÁşËı€ñÆP‰'³‚{AÀò<`1Sˆ×}g0Hå+•;±u®$İjŞ#)¿á
zjƒà—/2t-ºFvH[æ¥õ Üá >ƒTÿĞ“»Má±®ÀP~lÄ$ñ·=& AÛP>h8$› ÍÊG5œ²Ó¬IÃP>ªa±=4i[T‰jûk6i×ˆj¼Lnš´ÌŠK›•¸Éœ%ëf$¨t‘ç3œ¢³DÅŸ9S¥Õ9K³û´şQøCú\¿¬}PW;äÀ%ÀæA”Œ@ò®ç!ƒ®ŞzÜA”÷ïdáçÂ9_²Uæyˆ±ÑÉ;âö§ÏŞ>&*™¶ƒX°x¿0]¡›L§m~<ÉvóëWY¸Ã½†{§ozğL´^´t®!±p÷ëko&‰«¬–8è#…H´mŠ…7Æ7ûÑ8¡~q`PÉPôTLtzµê«å~ÂV©Œ(Ö×¡hMÍ‰‘›
—NÁÃ#ÆLUãKëKqr6l+â†A¯şî
¢{çÕ3vwUKÁÓ²§h·YóìÇO¼rOe¶èªœ‚ÚF$_Vájıhv3MÚAXõŞg4(;hïâè™ØHŒúã!º†¸.­,¾ª:_ÚßÒk5Ş›¯fIMÓæVÖ£Ç]¯ş ›Ü—ìMÎ'Ÿ‡û¯\òÂR3YÎ
Òn;â°¼«xZ-¢¢Æü5ŸC¤+Éİ97u¹
""˜ø†%’Ùj‰P=ƒ{•Ù*r¬zÈ ‰¯r’ß.W5%à¶p1‡»Ş*øŒuïväv˜û*±oCºÍÊ«›i™PÊZ€	2$½.A€&W‡äZ±Jët°E9x¦g…ÊÛyİ
5fLÖŠC®*m"×ŸÈ#Yö¦R¦?Ê_%óuV¦…Ğñ5³ ‹©YÀÆ”m3%Ã-‘C-76'š€ fVª’vX6Z„¾Å>æ’Ê]CFiy+÷”RÖOƒö}ªÆ¯qÅj¦2Qò¾ÖU#£Áâ†0šLæ›´W¶¯j8hK÷¯÷,+¢fµh¡¼PoÛé‰+Ÿ¦‹E0Úƒ¿óhf¢ÁV%—¤¶% eK@Ë.Ìf`ŸÙJ°Şn°ZÔª¸Î–ÉOK¶!€¢­ZÜîİQ¾¡Û§~¬hoıK”>#·aµ Éù­°Ëj‰	:ãétDX9²K•¿”½1ÂáÂ"2röÈŒg >˜ßQSõÇ6Ñkw–—ƒ%šù;9ÂM' `'ëãé¹yÈ/ò‚h›ºgÅìe<îÄTÿ<–¶‚†«òHÎ"”¤<ìeX«#…¬a“Ÿ»ƒ|ğÃ&"¢ºŒºk±q?‚Šƒpíˆë´Îêt]ü<«=âàñÙÆ{:ˆ9ãàÜ«àDu%èÛ İQAc¥ú1Êc®áÜéOÍ	OSO»ZÍîv$0Ï—7h·/<cün,ğ4ç¢œš5¤–lCjğ4!·H–ÊÀÑÂæH¡Ü‰êQŞEƒ©’rèx—ûõNVQÂÈ>6ÈË´OıÃSÆ¤LŒm<(4§.=÷±¶½ÇZ_!=é;,Øı]nıQ”÷¹½ïgniié,¿E¾Rd¸	N®Wùã¹Ë+#…f\–³îñÎ]"¾çJUy <´lqˆ¼£h’0ÿÅ Z¨³Mªóç9í>L¢O»Œ×ÌÙuÚ=|˜mqÌ‘ÂëˆË"O'lLÎ,©lYw“×ªZöm`#‹/5£¸mZõYo œŒau²	ÊÙAú­Z;ÖrD0ŠÏÈÅ)Ítj–¦(æ
N!½Àq¾¼ÎnP¬t±NãK¸€Ô‹g7‚âjç©»•¿`;Á·pïzèï€Ğsæ;wcë•ğÆ(•-ûèÀÖ9 fÈ#jßªWÆ]hòÜ½ É’˜!š<Òéí:°ÏìA-Ø.ÈÀSú«]dÕ”½*·İ´Ü˜/SÁxò áB]M=şÅ5&İ4C=Ø‹r3Ï3‘)„;ïš{@eÍe>ÏM_‚Û/ê<%Î.•ìÌsĞªf>ğf>Ä®|;É«Š_µGë‡€u›iı=KŠ’y6­PÄjèè-œÈªIõ=Ú}<Ô	Oµ˜/[+ù‡¸|©åõ‚ÿÆpQ¢ŒÆäÛK´TíWáÇäË®Ï…zêù€&ç­IÒhX‡ÏP²;-ê‰Z¡4ß)ÎbiÕLòTÃ=‚DUƒåd¾)2Û™²Ä±à:Ğ›¾!šÆ3^D4r[³ C{ÿ€=ÀÑ;(B4û€é(¹QUÒÇéEõH]Y¸­ørÒ:®”ëÍò|ùnÀ¥íív lƒy¢Ğ"ÒÕÉÔÊÑz¨NëÉSÛ¤Î+´gUıP…*õ‡"–j-ªñÍ¸	å=€G²©‹*•m&uU
5âúÓÒµÉO+†òŠís>)¤›‹êT¨Œ|0S'ª"§æ,bÎ`,`7’_ı«ğ
[<'²vO…êêàÌhal7H¬P]6ƒd€íÉç†Í;ƒú.«Ta*@Ï?,‚ x6‘¼ö2¿åí`B>ÌfGD3»[óV™o&³ŸŒ_ïõáúdlæ™+ëé1BAX,j§.]ZŸ[øß3.}6ªA»&>+( R·J…Å¡´i1Ü²c~<ÎsğµCƒÖ\©Ç¨Añ}…SX»m—„–ƒ°jWg GîŠm'úªö.ù²g×u½†ï^g×„ší§nn5Àw­§˜øSø…?=Ò^â¾|C†bZ‚hç¿Ïgı“ññ›ŞÙYÿTk(.e7ŠbÅÍ`Ô=¡eÅpÅv¾FO@xvÈ­ÀêvÌuXJúIVUùõ­:GY97œ‡xaÅÅvŒˆË Óšu¹;Âç°Ãä–Á‰pF\
c~"tñ½¥‹‰9e€ÉÙß.§Ê ER^Ì7Œ&Ëú@6!güAg7çĞÃ:ƒÃ.ª¬ú=x¦b«Ş–! Í>Âğ—¡„å,?NV¤õ¯·'î"à±ı÷Ö’Vç”!¹—‰n8ZN,á#İñQw\CiŠÜÄ>R ™±©9ÜYà/±²"X_¹!i^5|?â»P]&m;«n0ã¥o³wí~ø978?ZIuCñ[Ù¹°¹ƒø«t=ÜÜ0”#H-29+Œ5òÕøƒ|™ã[^Àµm†¡0~u‹I²0ß\'Ró›ºì:K§*ä!Oˆ^ŸÖ0#P Ö†ïp´¯ƒ©w«P³ã¦4^ä¨ÅÙ,TâÀÕ™‡³ÇÉ›`ÓP¦zÛ†ê±Æ€vb$K]jMúk{©Â•5`p¹ËÑ©ïÓI–0B,³IğtKhNŠ‰FMÄ€oSg3f<r	—YeX°é»Ÿ!jVt×µ l†WöR­›üŸÜ¬Ñ
—Ó/fæ†tÑaú…bv(ïg* €*^üÂÌ\ ù!Ì
ü!¦ÿäó€V€?W\(~EO@›ïâ¹ıÂÿ}/úÈ¸vèeïpÚ$Oñcÿôøümü¶?öwa/z/ŠøÇt>ÉÒæ–n8üóİàøûñeÿâtàv5ş¹É&Àx(ÓC÷õe¿?œ½î¦ÿŞ0n–ìê‹öş-+Üa®É†>õ½C‹á÷_n
V©(^¯ÓÔÄkÃõ ßû±÷şóŞ»Mî<p7è‘€x*ìŸFã—ï†ƒ3F†ãÓÁÙ÷äeŞK}%FsÊø!¹ØšÀOÔß%M&ºõKnCö~è;©Ñšw*Tâ`Êcf?îîê4İƒÉ-ƒŞt²OÛÉ!Èf¢#<E–¾vqhlBÆı`.Î&ì›Ä®0âTëİA˜zË|y·È7Å=Á¤Ú.ì±pÁa:¿¾'°ŞŞåeêT4hÂ*´ÏÁVú¡îì÷­Î}í¼Í=x³oYn^`äjĞÍË¼4®èï(ò¹?¹¹pÁ|~“B<_G«Ãsœ/0$A” ²p‹ÕoÜOß”4LBõ CåÓâR<Ä†û¯6¼bì´ü$täVeÎ}İp$0°-ŠHúôlø!j¼—=¿~¿¿7B[~‘^ˆÔr¥‡Ğö€ÚÄ^ûN‰=w7œÌÒéf^é¶ãã.gg.
w[(FØL¾Ø¢s6÷ƒ¹NtHë¥ËØë³‹†wöÂ*ÆĞ0¿Ş;u£1˜—Ô´œ«.'šáÍ
´5ÎŞò›“LJ“{ˆ—'ß]1fS7
•œQX*Ï_¥¬€¸ülÔò«'^ƒçN¨Ã¹šåK¿Ï”wF. Ú+\ZFFÑC±ŞÚ>h=l¹=uŒw¥¾«äŠú›ŠR}×åzàLŸÙÒ~d‘Ÿ”Q6„ß	ò>§hñ7‰Æ†[Î/Ş…YuŒDSıU@N_®z,³YyÇ±l£ÿF _šFpé ê»õÍTÓñ‚¡¥cŸâªƒáÖ¡Ê;Ö)õwêmi‹#[6€¾,ûĞòb2\fÿl±3“Œßq³üj_¤\}µ1ZÔD"Ft˜Ï2:Ka«àÂIŠé©ÓªÃíXÔÿÁ”
Â>İ$F¸hÒBÌY||—6~‘ıömRÎº‹ä×6ÄK§D-0,“9ñ¥Ö
‡"œr:Ìó‚—™"›¥ìhXt@3âÊÛd‰®ûà¸0Ğ±(|ÛÊ:¨^w¶iœ+“ß˜•£Š-T¢f(`àÆß•[‡Î$º¯ûËÉúnŞ¾:n€Ò7Â«Üññ›‡hÎoÒùÊõ»Ğ
Ó²d¼áœ!â–×y(è-Ì¬æõœYÉ¢û’gëıï™ªò§<ª :c)‹d9JdÊå×¬÷c^©%*Ct£­PòRÊ
İ2?ÍoÓ5jm…"§ı`Ææ"œ²¼š0¿›­~ûˆê¶3\ÛµF
î½HĞ[`ËOŒå"’ùœ<½Vc7WèzÁ(m³â^î¼Õ!9E¨ºDIˆä„í ÕDİ	 ±5ZGÎGİ–0ª5¹Ù°%;a›ûÍÍ<}»‰·ŠÃ­RÊET í…müÖ|Ÿ„¬:Ü÷ÄÏàrWR,×V9Ã‘#æG™ Sğ;ŞñbÕëø“‚QFJn­³›ú÷(5›Ñ›aÛö&›NÓå`	ûÚf©¿¤^-º:)ªÓwxd¤¹¶È7-ÿ©Â@ëHÙ2§Ò$}<CXÆO¾:ì.6%¤¾³¹NÙñ;Ï×¯²y)¢©_°;Fº>Ù\_ë_¼A§o³å4¿­bWÿ8ËÊ|éà*À¸¾ª¹.P_wxy<œyDd¦*ƒ–£t„ˆv§ôì» 4ó)„Vc’ˆ3ôáŠ”eÈ]K‘Û<½6©M'¶ëâh«†¢Àœç“>0Éµ	ú?à½–'vT @ë¿ñu^İ¢­rxE0ÍWmãê°xĞUrEÉ¿ï}0â¶îFıåYğM^Â×©èÒÚìı}_6á(»\)GÒ?<²: s)ûµ¯€Hzİh;şwK¨Fé¸ôæ¯VS¬©yˆ}³•H«;ÈÎòR‘A1„½Nš¿û7#·ßz°ƒÉÍ’ÀeŸŸÌ"›§Ë&nó®y¿1Iùj³úq¬V~)I°R—sf¾	æfô^†Q“mÑ² ‹›óhäk|êÕ)Í‹-ÅÄ?œfSø}0(˜İI4nø×¶‡ÖnvÿäğLß†w,Œƒ«`s·”æÕI½¤˜¯Ò%F±‚ Œ•ã¸õ^]$å¬Œ-;]ÿ½I—“´%òş‘bL)±?Å·n™•‹dÕ*g›H=ôHÁpûL¥¿?‚6‚RË$›·5°0:ùœv±PW•*ºogƒñ÷ƒ³k2/fy™ÃŞÏ7+Míäˆ¹Ûå,+üÒÈj$•£½t¢Ÿ$Àxc_õÎø8mÅj}öó®•âÿyşbí£N‹ÿ§!çHJSÅG«SlYåp~&gÕ,¤ıá±±:üHÍ
áŠJ[õø+¢°?·…z™ô¿‹œ[ qüú
²Ó¡½66‰#é	w­)Tyj/.Ö9à™N'åO#%–JÀ/ÓIš±‚]¾ŞäsÖgí3 ùÈÑ…:b—c·±NaœÆkíG%ã˜ÒÙÇu™8Œ×U¨ô£s±áj±€OÁ–<—_¬ÙØß¤@†ybGJ§(×ühşîh™ÙY}Ç‡[‹‘¶TÍ7ìêÈ6“Æ¸çÓª.²Ó2e7?îLlñµÛş5xh,uv\ %°& G’ıáYûN h°I÷æAá	l¹cÿÇl‡Ù¬Ó·l»F$[ƒaü‚:«ó±•1‡y`ÎáY9;Ndş¢_0DBCâTZ­Ó;'àk‡ıëT›±óÕ£ÿ_ã × Ö@ÈŒÛf…'ÙËªÂ?7y©\7¥r„F;ú1 ï¥²bÅsãbç`²( ?^ƒ91DHoOÈnÊÙ]ù…×¯|_e¶FôºL×²pºœÖñ' ´fÿè1ä|ğ¹HF?²´xµÎ|ÑV•‰É³ˆ%a…>o­ E˜´´èhÂÍaÊšš³:vÈæ
B@§Ü0—@)j48µ@uNÈ»ˆ1ó¦\o¤7Kils¸b%¼®
ª½.a‹fÌ
^Ã«ªØféÍ>~ìiØ1÷@.µœ™My®ÿ¼šÈ¦Œ=óëf8!ÀG3ãİ1÷vGÎ¤6Ç
\ËÕnÜi²`Çt°ZÍÅÅ,¼Øt'Õ!FîŞ¨<n KÄ»ğHZsµ(’ï|SVp\ç’œíOmÀB ©«»2ıù—ÖÕæúš{Ë°6ğİ7­/ZO~õ‹¿.,úñ’m™şr·3˜‰v»*‹šÊ.Ænã=3V5à›+6Ìî-ä>µp§¬Z­	›µ¯ç›bŠşïÌ F9Tº¨-$
LZi\1zØ”œ½®ŞŒš&³V»ÿë$å÷‡4„,Î¢ŞtÓvhî'ÁHŒñªLxbV/İŸu¡H²õw¶Pn`zÅÏ[ùÊ„­ÔƒìôyU¡¹¿kß
y¬îu¶.ØŞQ­a¸ˆ1n¤ºÂ}W‡P«MÜğ@¥{ä´ñ°êòØê£.œ&úÅÙH^=5fT°‹¢´©T5¹Pu,„¨cŞà~¸ £÷¬ÀƒÉ¾MUDŠ¿HKm.ÛE5êL³MÅQ*¡|/¡âäcmçˆ+¶-2^½-ª“ö+Ãi$5°™416æÈ`aü¯–)±{ÿè’#è¤K…ò92ğÓ5 ªO¬€Ÿ³_ê8Ù·eflh”= n‚¸[bxgIë>!†‰¡ıL>Î[Ó Å1ÇGƒ™¯¡Qld¿b¬ A­DM5”%xÜÏ/ù1~İ¯ÈÅKvb+=¢EÁõ¢mœ•iÿd_Ù 4:Âcv›Ú=×í¾óg¿–9ıV˜¢¨¡ƒ°S`"ŠSw…¥;ÉİbşNÄ‹é6¢ÄÏìª^å¸d¯îjåt®Ã$R£&Šß/ôêênqË¶£†zã©‘§Œsê*‰RõD¾v©ĞêàoOéiVL’õ”êêãk{S%°“óëkv2¨6¨uíŒ2Æş”Ébõ®pÛÑ>úğ¬î2æİ¦Jr`¼ï–¹0ğ'7ÀvY-¼Hª†åOùER|¨
<5³¯pYÎ+B*} Xi‰zåÅÉ’çëîÁv¬¡Fí©;ËnfÿÜ$"¡š$7Õ[Š2ø®†WDh„(Áblcê„6—\èÀİp½NW_æk9OMuüŠGëx{UXª&®ª	ŸzJÚ
ÛCédÍ¯§ (‰R+T§¹_»° Õ
N~$.a¿!ï„ìK×zaR<DU€ûå«,OÑø)Ã8Êul’îz!ÓçÄ.ÈÜ½¾òvV+J=]ë¡}]1Û³,ë‚ l}0µ.ç~óx¸J÷íÔ€_­… Ææ~(M–‘å…*ĞUéÉa=¡0Uì2ı÷€6¬¢úüÿ   ÿÿ ĞD{Bxœì}kwÛÈ‘è÷ı”?ì¡bš#{&¯Ës)‰¶uG¯ˆ’gssæğ@$!& ek3ùï·«ú~T7¤<™d'‹@?«««ëİ«a^_Ü—«²¿ÛûÏÿìídi¾Ê‹»Ó¬®“»ìüæ¯ÙlÅŠ|ÈÓŒÙíıı?zÚSe«uU|§ŞıCıu›É¼g4Ó+ù?û=ª“¦‘·ù<cõÙö{Åz>o¾ä·½şÎUöyu½Êç5Öx±\=öy»Ã…hîS‘UÃdµJf÷ÉêŞ´l:û„]µUÿÎ¨ŒC€†Ùç¼^Õ.Lô.ŒÑ› ú‡1+^×ğŒÆzR&)İ]¶:.êURÌ²şl]UY±ÍfåºXíÂ7öU) KMoĞ»Mæu6è­ªu¶K-ŸÓâFpÚ_b”0¼–A†5šgÕê(Oæåİğ`ÏÙ°z7â_D‰>ÜaY¬õwY÷÷Ù";ÊæÙ]²Ê,lmëlu•¯.±ª“UÅp»ÿl´\%‹ìÙ w9¬ñİP¼Úõ·rQÖlk<dëÕª,ôæÎĞ[:ÿ‘Åjª,çYRô’y•%éãQù©˜³Õ`UØ|ÅşÓ–´´ö7¼ó¬ (j/d^Ÿğöõ­ÄêÁO˜kß,,š;:ji`‘XÒÀCAâ‚Í·ÎØÛ,YÈí>ÿÑËºoÙ…·qãW}_~âèÔ—ÏØĞV. ü´éñ‡<ûd-Á.bHÂáÏ°$g´ouŸ×~åTxTUÉã	Û!¯9©}Ó[BµÜêó›¾S÷”ÑêvCUÎÙVâØÆìçc/Ãÿò6üÅú{ƒÿîT6ŸÑM]Î×« l0öÉÆCYeOv7œ%ËU^¬ÛÙ}¢ÀÀzÊ*€G½l^áîµk§ë*Õû9Cß^ƒ°Gâ“[iV>dNHí!¨€C=„oz@o§ÚMØír|iÏå)YZçbƒ |ù†Išò¾´şı¸S.³¿¾-«	C•ÙªÏÛAH¿Rg,§Ş
±Ú‹‹ª|ÈLÚÓù?çl"ûd¼]®oæùÌhñbÌ2ÙŒ˜.ğš³#&ë°ĞzW'—‡CN©f|Eoµƒ[\öÿ4û<P$±È²ô¢ÊØš—³9#¸4ùáû³w¨¡ÙvÛĞj)Fk<İİYG_Íxà=”yÚ«³"åÇk¶®³´¯M‘qÌØª,=.nËŞƒù[ƒB¹Êo9€êÙ}–®çÙ#æ›ËlÉhÖEVåeÚÔ½-«#ƒ³õ"ƒ½ã‚‘Ÿàí`p Qµ`ç|ûİnœ¼>¬Êå2K{?ÿÜ“¯.6óCFø;.¾)€9’6ŞåÀ…«„§	K v:°LJ¦&ç;–à¡É[ZÙØ€pŸjxcLSû")¥]WÙiYeˆæµ³áá»iÇæŞ°-óòÓ„Ío²¾a [Á^MeÛzLàn¨×÷£õ½üã[Rè¸ÿK\FİE*îØögÿ2Bş·uÆè{Oœ‹vå~Û®¤q°ÖXÑ/Iâø4·œ4ã™çbæÑSŞràív’d?d7eR¥$ÒÀ:Ó9ò¼–m|ÈëüfÑ¦’Ö8ÜGà/L=†æ†¶›ú¸Ğ¼
.«üÄ_ëé_óÇã[hÌĞ 
8‚ÏîX²—‡÷Œ­?„ İ†].+ä){	«³÷ûç5o‚ıùü¹½†Ç6ŸËl–1Y¯êåÆ/Jo€hƒ÷h´ê'„¤…UrÁ–·ÈÍØfô¹Õöé:$­6{3øc°¾`04>’ËSeõ`ŠE¦Ñ®ï(ñ
¹aØ˜ŒêÓ“|Ó³š.™„ƒ<Ái²\‚`­ï^@ºŞ† Í¯×=_Íaÿw`«Î-ì@Á‚ˆäëã0léS‚|H‡Qğı_Ü·Ö™ÁGçÃf €ìLô ³YÂ@M½"™[c%‘¾Åî¼ë¬J[¡7¶Õ¾¾İ—¶Y:@S»EÑ«ûõâ¦™¹¢¶7œÚŞèÛ¬iJì0öİ%ÁöÃOœî„ÕâB6şµO6[ë& *›¹jg8'æï~Å?æSÆü°¥ÔÁ¦>´Í{Şj9åsSeÉÇp1zåÃ_ ,Ö ]ŒĞUÁÆ˜úú™±ˆb|;¢à7tªx§šÊbÑº92¢TïEÆ¸.M)áÓ^ÎhúPÎ™lKáióåWà]1·õ 2¥Ét
Ü(€îA0xfdı¹9a¤êğâÏ¯ØÇ×Ox‘¤ ?_•ËŞ‹Ş¨H+&HÁŒI€Òeÿ/ú	Fßå?éù3ü—Ÿ—[V©T&Ã«W?¹•å\äZ?2v¡üÔçµ‰¥j“”5ÑVX…)Tš\Øşà8ş²GŒ]+ùç¦äËŸx÷ØÂÈ©5pt·Dï—“÷ÖH
¶ò ¼ãz‹šô
I5ºµ5Œß4—¢‰G>Ü¯àô³…éä«E²œ$·*)­½*IóuM5xÉ¤§ô?÷=Ú…v¦ö‘°YÁ‡6ŞB*EÎ1£P½Zíav˜Ùl/2ŒRĞ½h6‡¾w³98¿½­³•?{qqP®VåÂÇÍ¦–Bo¼ï…Ìóëè»oŸp/¬v=Y1‰ÿ}9Ä¢o–&Ÿy'Xªïk‰Õ`°ÈgWÉM=aN>¾©áÊ¼ÜHÊÎ‹ƒó««óS_Sy±\¯ëyR¤ï³üî~uÈW’ – {¶ÅÅ\Şæ/‡LZê¥_XfÅ¨Zå³9W¡Ú"±<ÊáoPkLó‡áû}ÀÂ½URÁ¶‚¿İDÃôc’uúùg½†şúÎ±î6¾´n' i8Ã69Èj´Q™RŠÁíyKUùìş$y,×«¶	pë%é%«%ÆØ«´¿mIÉ`¨ àT¼1gd4AÂFcaµÖøMo)ÿl±Æúà0œÁu¶B©Ÿ7ÔoÚtÌ¹J3ÆzkŠñÕùm_G9sÒR—±çªvs¦Oü½ï·I6]h<Fn­°€^G7ù@Æ°â© ¶¢š÷(M–«¬ÒWt e`B}7Ü*a!Õ‚Ö‹2@¬¤ª[,¤ŞœXO¥ü„0‰!ƒM&¸ÂĞDÑïÜÚá)zPÅ–q$”~…@Í‘›ºo8ñéõûá¦Ó@í%'Êøİf`„¼AulÊƒİûÆÂÑ/|BSå&ıö©b¹ğTµ¦şùS…½«ÊõÒ—úĞ»Ãÿ²Éhe]­˜R»ä vô€™Àšbœ•xİ+Ø?´âÅìfs£U?ß n‡ezc™jvFSÑ£ñámYñ5Ñ6}‘'f»ŒÛì¹PY¥İlßy.îIñÙÄQ˜Ÿ9µˆİe:Ù™’hìİğÙ-H>İæ•ÁÔÚâÿö0ıõQs{WY½{ÚºÏI#w¸ĞÈûñ>v¬MÎ_o/8²5›'udY˜´¼œgàZR›º§`5Ù‰5^œ®Yšf9ğdª}u2„Æ63zƒàp¡m,B‡®%Ìù«¯7’;·g«JxÄ#Û¯]ÓºÂ› vlâU¶¨Å&èı]¢€²iÄ´gcÜ(d ·éÆ\×<½ õÏçîÈãÚ”Â³tÕ‡ë*X¥X²¢²¾€òIˆğˆ/&qÿ1»ÉÈ>
w˜v»-ïÕIãğÀıÊlv®F°}rjO3;Ü¾ß¿‰áHfrÜ)\N]Û½STË¼€AÀéÖ,·k"çZ(¨¶š5’:ùdÃ¦³ş!øşÔ+eÃü1_İ£­ß7ÚÃÚ»hF.•¢´¢LøBD°æ0F<£ÀÄ{ëmÃÑf&À(Ú…¹ıL¯XLÉkõÍu“Ì>€‰Bâ®k¦}æº`”Üuq{Ø0 ·käK#×Ÿ›7Zo9[Õi›Ë,+÷—½ŸØØdì×ï²£*ìƒr©©şâeĞwM¶N(äCî,áö½'µ9C›%z©üC-zS9rİ¥é%rÑeÜdÃşëW{—?úªÌ*¢³ğbo;¢?’nÑñD#ÿ2˜‡M·âNŒ´ÕN±P3¸^¦Œ«ÌÙw/‘³o3vÂJ¥^Ğ‹ğ/ÒÜ\%{ Ü^…t­ÔPa8}gh~9© šÃ¨-£óšô,¿·ŞËÙıéû¤J?±~/æÉ#O¨Óˆs™´VÊ•ë\¥q/û¼ÊŠ´ŠÁñJw›Èkü´c’Çò²è“¡ôuMŠ_^+fm~¥Å¸"é.¼º¥k;XêI¢‚¯.äÑj™„«e¢ [üœáÙÄw®>ÒÀø<7|gfRûQnäÍl:©•‘¤ÙßÖåím‹‡êØ´Ñítk(Ê«£ĞWÀõª“_”şxU£Éjñøj
ÂAÿ¼Yˆù/ô²bc'}€<Í¼tšù³jæe ™Í<ÌàñúNî²mX¬ò‚
éø…}©8·úSà|BÇ©nPÛù7=…7Óÿúı[úi)s‹Å#òX_Æ;s-
ˆKãª*Ípk A¯¿½SvV:cDãm2[•Õã°¼åÎ"iö,ËôğÎ3-ÊŸÈ•p]Ôëå²¬ØÊ5p²ha‚}Ú'À¸®¹ŠÆd§ˆ¡ @Ó3˜8qÈI\=¾``Ä{Š¾ÀÈn`ô$¦‘ŠNà?•dÇ|	D×ş®kÕ±˜*$ÚjcntT?o›zÜÀŸ=ÛfÌ'(mğÁ@Êáİ)¡Dœ¶µ€KÈ_4ñƒtÛŞlIZÜ'TÂë*ÕRQÕg¿zë*×‡ábdæ=µÃã§*˜‚&ÄŞÃ¬V÷-+ôùl5*FûXço«rÁ Å®JAÖìÄFá!ğ[	4úÏfì×]YŞÍ³aÂÚa²\ÖC‘¨
²).õÌYHH¤â.­âm^Õ«1ïšŸÃÖ8ê%;ÓûÏ¾zùÕ³]’…Ò½ş¬Ö”ëß³¯F‡W×£“g} ¯¿Ú1¿eÔî¬^ßğs²Â„ÂÅÔëŒ.5š¼¾<9boKHñß¾ÕÉ ÷ìúêí‹?P±¥Ç`,eõí>ÚƒØe^Íî{ıñçYÆ³Ñ8öx¸án˜õİør÷/1c`†!1"©§‡œ9°¿³+–U~ò¨,ÛiÊÉm;oT_#,“(c˜ëÍğì|:9<¿M'Wç—£wc{–zSf,¹F©›Ñ[Ô¸oÿÁ£ÇN84+— ğ«ò1§BÏ@yóLÒ–³lÎbÂ|<`û‡X®·òy¡ŠZBØà3“ y¯ßgó%Á²Ê@Hé3]^šgëjo¥şŸÿa¥¿àáÓ<°Ù-çöù2@vû’x©,$²®¬ğ§u	™P¨ãÉÍš‚d]¶ĞhH@éÓa|n²¨¡DX—#xZØHä¸i‚è_Pê¯{v!Gs‘Ù¼¯\€©lµÒqé=ŞVÉN˜
xòzuÈh2?´¹Wÿ}Œ'`?±ï¦ìF•kér<¹>¹šÿ@)Qµ¾Ğ|:Sëİ+ÒïÅÔ`ãÎ¦åd3U
“¶©©¬âÊªÊg€Ë<p ¡2çä3ÈÁCä¡4×TÁ©Fg¡l¨³´—0EÏœø¤¹7ò·r¶NDVSçRi)Pöûâ±½°Ù‰UlïÊÍÎ€ò1Kûˆ‘TÜ&’‚rĞº®Ô‡ß˜]ıë—ÏåyâøÆKãD •‘ı"‹Š'hxE»AIHl»˜KF““IÑŒ¼vù{ù^Kyw§|h¡@>z‘œ¢H&V>~fVµè5chCm×ôwØş—ÉºÎ‚aä—«•ÊšÇ`¨éƒWKˆ@Rí±1«öÈ<­ş);jÓXÉ}*èÔéùÑ˜ñğïÇG×'ã£<pëÕ‡p<–2eéDËi‡{­™M	Õ£Áõé¬xTvÁİŞ‹7-vŸÎŒ3õ´ò„Ü:b–‘MŞŒã³x=—‘äÿİ‹e9<åJ‡8ÁPÎ–pncU¬ó·^Ÿíµª®­–#Æğ—Ü²£O4M:ñ¦.†]ùÅ™’§'drJÇ™E©#¯/›uIóz‘×u÷LIğü»“`Wåk®MæZmÛèã<>LÆfš&'p8Ï—ÿ‹p¿6„“«Ò›É?î¾Y±pf¬H“ÍE¤N­Öew#Ø-bHü`Å6Üşe‡çŸ·m¾çl}+lÚìPi#vafksÚ²-Î?5¾wÅõMs~)´úšÁ Ñ~R¶ïÁ,Ä›¿,.Êù¼‰òáç„Oãó‹7Ï~•s<°yÏoGÕ]İ?Xé<ë%ìo[‹L¨>=Ó‚ÚÃåZİ¬²deŸµ¨\#[1ğ—UëxI5-«ñ¨áÃ¢]d‡ôÉGF”We¡’E[Pùëšáì):CNR{‡)±Ø+1 ëZKµ¯=®Ê;“{Æ
¤‡eq›ßÉeeQ‹„²B!pÕÌW°ğĞïö(Û©yÕ@fpænXHŒ‹ä38Àí£uá‰§£ÿš~\›r$Az4TMÅs¿3§c¼œã+Â†°ÂÁ†kéãĞ`Éª¾&Ã1$(XÁv[$:ó¯÷=É]d³Gy½½á{p±¸W÷o…%ã »Ë‹«rÂ(.ï'÷A$9ÜïõwM»ÌÀf›‚S.¦|ªî²#™mG$SÖË½üIKedÕdå}Gà1Í§øÆ—‘é»­ô¾ïq”Ç×;¸kª]•½o<€YÖÚ‡&w#ƒC•Üßp$7|È hğ+Íä¦îW~Õkt»ÂÇ1çÃñäøàdì¯¥c5ƒÁ^çíŞ Ú”Pe*N¢Ñ’±óU3ÅÓZDQ=Îè£0È°PÙC>Ë˜0Á6÷A‡XÃ$£ìÅøòíùåéèìp<=<M&Ó“óù]fU¹¸`@Gkğ'm±à+56kf“«ÜVQÜPæ=ª’O@9{Ÿ’ù|™,Ñ]Ùà¹aÖ?Ê²8E'„«QÈ½#­
ú9'³ £HÉ,ªÿpºlÚÒLD§%Ì¶iK™Œ¯loQJÇcsªwÚRÓ©
×˜iÆ8­E^0p«ËqŠ/rHë´.Ÿƒ0Û¬èYöyõÜÅ³ÂBs¼¢¡2©‘cc¡ÛôÔ,³,€¯¨Î¨àËáooÑ{é‹æMñğDd¨1©A@uh²õôMnLvb´lLyÔLk&—È«î®òEvÊÈÌ‹¶6Øq:ùa|2¾:?›OFãÑåôtBòq<ï›ŸFªkqøqd3wd¼}·¦§¯V]†3T·u€)´lĞÃË 5rùa‚BêLm€DlZ^<$ó<Å{üÂìµÄU¶(²ë,ó¤ÈÔÊÖ3ğ_»*ÇEj¯îºĞá;è–äÃNÊOI•fkŸxıò'Ç™Oµ–S>.LßmŸcğ°“x%óf’…Ì%ãZÇk}®#x‰šş²'êK2É@ßº#€k­+~ÛZ)¦y*"Ò3Päå"İJ%)÷•J¸ek ‚ö„÷pHÊº<uºŞÖaëÒQ¼>Ê$6"x2µªõaPj@8 mÎÔÖõ4ü*ØR â×›VüfÓŠ¿İ´âï6­øû0˜½Ä$z7Â.È?µÆmÛBõ.úËK=’ã #HÔşnífí„Ë°^ş¶ÎªGè±pL·ûÓÙ%¹LK6åKÜÏªª9*«Ù×îHµA’<g„/Ì7m{‚î',n‘•¸¼âˆ$øötá1¿L‚şd"‰ÓÿÓßÛ	È€"2p0Âå‡¡2˜aïñ××ÈÉÑ…¸õ¨>cœçe6÷5%mÊE@6I2Ôx!	h j²ı›r%¯KÍˆ¹j›õ2/
~1d¨$ ¹n%”_ıÆ7.­¥ß|õB)äªš¬<ş
à³à‚_9¬••o×
ëöİqY«ÖÅyq}Ìİ3ú}´úÆ¤uú6/òúŞ;œİAï{v’/÷éëOˆ#swf[ÁÔÌi¡:Ÿ4Y½-+À ©cR!­ÍyÉX M•‹<VØß\!Òßv UÂq•*büAŒóC- ô%JM^–õjY cŸ
éÍ7Wå	ãÅÿL‘cæ.“ÙÄ8¶¾;’ÛäcváŒ3‘Ÿšú¾t5:{3FNŞ`$iÂ#şôªDÙG°–.#ÖÈÈè"A³. Z½NæˆµîÌŞ'õıi²|-°ÃJµù¦WÎ•v˜e”½X7ÕyëÈ•¯º{ÏÑøíèúä
U»†ÛÏõ»wãÉÕñùÙÄ‘J9?Ó`äA¹bÛe| Ò‘?ÁanúXH¹Áš;6Åôí–ĞÄ`åû¨ïzß*µÏÇJ©{ÛŞˆ(†fŞ;JšqKoe wP–À£à{nòëÁãQòUf‚ÁßtIdš“œÁ’.·ÒôFišy–ÈHYó’Ñ¼á—o¾xÈYÅ¸›Ùê/ÉOtßğhÅq1«—ÒVÖâ+·H>‹A§¬ƒ°eÊ¨˜¾ŠÇg¾ŠAÓw`(ş[†âŸƒ«©dıƒ—Jt×¬Û¦<¡W¨ñÊúwÓ:¸„vùÃ¤8,—T›¬’ª­šEÜ Ä–ùÌ‡,šA+ñ+]‚‹l7All#Bä‚¥­ñL‘î#ÍìüEù¥M©æü×r×ba0p‹È£âaîœVåİİ<;®C‡b‘b 9›¨ç}*êYêiU°É÷eGğN´iFr}—à™,æ+)G,F. 5Å#Ê§qQ®›(ZÆ‚&?ô5÷>Ã(«²H:\|ÙVæ§¥÷}ïë=vBÎIş¿ÆQZj½W–ç´Aø¥#÷iò™aO
¿y=>`ç%ßd!´u49‚”¸íe±àËŠN5Ë¼îÚ•E¦EÆ™‰|nJ¯í\>ÉïŠëe_Ö¹Ã›]¨¬fÙûO±'M2Kñ›µj|·Õ£¶8¨˜Å‘š‰½o­Â0Û¿“‚Ÿ§¸‘¶s²wÙ¦Ò¿œ/³ÂYdØ”äl÷1è½ôXÔìúME×ÄÙ„3¾/³Ù#£æ•*:>½¸úóôÃñøÇéèìøtüöôêÏãéèäâıˆÑ¯ÑÉØoÚ‹ƒGÇù¼ïÄí1k¼èûĞ‚çá|#’â.K}™¸‚{B¼2Zæ“Ç,•A¼^[e ß¤¸* ÙÛ³ì!Ñ‹¤úÈ)ñ¨²h’i4RAÌk#å3èjĞok”Î0€ù¥¡Î‘»§1æ±ïè,f‹RŞ‚  ²aNÈ¤ç}~ƒÄ©¿äçòè;“ù-y“ä!Kå!ÇÉs«úÄ¡Çv×îéï|ıÓõñáÓËñÅÉñxâåAçAu›U¸Íçß(“Ì°nß9^şEí=G{(„š‡šÛ”È_,«ƒ0Pİ&³ŒÚ}Ñw˜õò ë©€ÔŒR%|Õl“ŞCOY}ÙÁÂZ/EŸÍò5u'˜­µ:¶ÖGS€dKj^ü]tÇßVC,…¤JÿN8~YÅÌBËóËZæK4InË›¿ZDÖ)0ßüU©cV¡/L’,)t&\S£¹tÿ¥êşˆæ¤÷œ±Š6(Bƒq ñÊ4Ù’¨UÉH ' }Uòõ°ƒÇÓü~O†¼ğÄ­ <-WÛo— uÇá¨ôM“P{ÀVi¹Â@Ò4½ŸoõÌvõ¬ÖÔ… îSşÔ„¹ÓDãÀá'“ORÏV2ãÕ×
2hèûö@}S´ô ³Chø=2Æ©|Æ=€¯ğ‹€„vÊ‡9Æ·g¥õÛ¦ÔŸ à§şóç–Ù)å>'%´0ş7æš<å,Šqí}ß]ÁK³†L@n£¬†oÊ_á»û¤ñ]J¹Ë®Uã÷Ä€§«Ç%Qô¤9lŒ¤Ç2†ı‘h5K§‹äó”t³xå)©<™Ğ;Õ³²ÿ˜0 òûrğÙM}­Ûx ké¶	ºàoÜ«1îĞ¢Šr˜ –ğqh¸6T
Ø½ÑX	åœ‰ˆ¥Ùï}ã¤¥óíöVV8ƒï°Ô4/„m!`ZØ„ï,Nä5·snÅƒx'nûl:»áãÔs8ú}nÕi±}:¸Ç9vnMv•dâ~jÒÑfTCmø+R 
·,.`7Ò¶¦!İûBRxèÔ…áÅrv±Ş“ÉÃ:}.…^C,¤ç1Kú‚+m¹.7*
–Â®‡:J4<(×JOH ı1Øfìh%·k8k¤§I®“™¡äkŸº}ˆÙdåN}a¼¬ŞDŠtğ(òú‘‹u	±îc8œXjÕ%!û¸«SJ¨Úd_\bS	Ë²Û¼¸áéJûñ¦7üî´Àßµ	N^‹wdb
xx·à“À‘ş³¬˜1Ê×DÖíó´-¦ëál>`KûÏ€ˆ#ê4îıË Ñè#úc¡	äÆÓuãİá—@‹&%`Í¡–½ÏûÜõŞ§F©'¿r@LóÜf`G/èğ`>šóĞ.ÆùMewx6—ßeÏ›Êğğ<‰uÊqôÛ	æöº
çğl* ÃÓ]H‡'’'—–8Ì¸¶ªÒk=£Êâµ~÷åzJƒ"hña üv‡+ÒûWX5]•Ó9÷dĞCùA0,x4±¶£»ÔÒ¹AG–,ü[++¾ËjLî×>#Œ.‡yŸÏSy¡L02´mìòq dMáw¡;»T¼˜ªOÁÒ~‚Ø+ò4Ü´NîJm‘gA¶¶År¦ôz-Ğ3²JPÚ‚\x"æ´ºÀî_çkó†¯}#Å½äóqqS
/†÷q!Fwæàia{áñ Ñjôpú&{.4nztñ1p“Íç¥¼šw‘¡¥“©ÜóAF[TÎ/Køv4u](Ÿ]Š#ùïÿû½½=™6CõãäNoHø!­”µ¤~ñÂ••õ¶„‡¿şÊ–QeEEµ±\kq×ôn°±Ç—#Å”¾`”|?$SØÁ)‚ë •·º9/ˆ XÆ¶_–Ÿôr©Â6wz;‚Vb¸;‚£
á0 ¬…‘ÂiÍò"Sæk¤3©¹~æ~zéì§¯UÆVâà™í97ƒ¿m¯¸x÷¹­Üdßì=ÂS:Ş[Îã+ãzS`LÂŸÖùì#xäY}^ı˜ÍgåB†T×Üe÷Ã~2ôw,üÆ™V€-;¼U—à×ahš¾!|z^¼8Ms¸-Q$€ÿ!{lt¾».Yu°Í¡Û£‡H
!|O-Jû¦÷Š4¹v?×	&¯ßZNŸÚO‹ß×¾,ˆB„b;ü>ú1¡,n¹P8<±!-ğÄ†µÀCùÁ¼
º¾DG·Hoá‚må‚•7‰t§U¹´qÄKË°Âsòäèéı"†Œ€	¹{„­í †à±ß·H}fú"\QÙ^¡3’¯{3ÌPú~õ¬eü
ÊôlëÜdl1ÙÉ…'n,Ì¬I}•ôì>©AòŠ	>ÛôÎ\WHÜµgŞ]KúÇ!PW±»¬_šCá’óú†õÛ·¿µ){â‘¼]¼‰ëA‚]?JTã¡=.W“¬Ø‚	ÅÚRº9xJ^U`$éËXe/Î%¾bÔºEå$aíéÙ§Û€Âï×PjÆfÂè5ícáÃsÕÖ¶{&¿yÅ×™½¶p®Çgg®‚7óH­Ü’é¥@ópğ›*ô«,ü¹´šrdŸózÅ)?1à”˜dEÒÑPm¶Ü|Š )ÙL 7O+vÃš/;­
Öb‹²dlFÄßëe„Õ>åp£)h0ïÈE	K–ÆnßØa#n_!î­mzJ#yYíô.	åÌchĞ¡mÛRÂ™«=s¹­–Mˆ9ÂZ¤"—ëZM”ÍËÇhpaƒ_\	KŠB<Ÿ„}+·?pDÂ!IÓÑ|Şo¦1 ¶Ñ µ©¼²Œü¼Ö1ÿİ!¤4Qb1ÍÄñ@{®ÇUÇµC	Œ‹í1Å_şşêDjİÈüÑú¤š80¡Ò‘Y‚}f.5–ğ ÓYàÊ×wåÙÈbL÷g¥j³#3¸I_b²í‰¯ãî2¦j¶®ë¼Ä,LÆòRĞ>eİ‚÷âÄ¡Ö±å¨©´€Ÿ÷¬rV5éömIÀH&Œc]+ÇDÊñÍ×øP8NÛ¼³·¼ÈB|%ªEÖÊk™Õ|\¶Ñ[“Iš7sáÌHÔDü´™nšõ}m‘Ù
.ÂÊDîë È%±¾µ]…•çû¶„Aª—¢İ¨Å÷!`5æd ^²*dÙ·¸ñ€J©{è#ê^Ó<'ÇÉVpı¶Ta|rŒÆíDÿì¼ĞMó±µn1Í*ÉrY•ŸÑçg~Ÿ ”gù/8"PÁiä0’øt+uàà„æ¾3õ–o€à;7sSÇ†f<¶ÚÓ˜uq;ƒ€ùÛ½Ú$uá_ÓqòÜ!½´*—xY ømwnFk¦l½\Õ,Â‡¸‡Ö 1íÅÙØa·°´Uâz1¸›ƒÛúÜ’Û‰ŠmQ	Àóx “’P°M$H!DÁ8rø—çH_$êjùám…wä‚_Mâœ«Fht»»*J†¢º.ZeÔ´w5\/…˜Gõç¢%2JÃ±a4lÛºÛJE‰¢Ÿpá”ÓğTœŸ2ønHHF·Ô¤ ¿l;u*Ø­»Y\ßBS{ÃGÎøØï8¥%ua¥˜ ‡óÌëó5Ïv£<uØåªÓû–öàñævcMBj~Ø!–S…¡ÖLŒs7m™ìü1Šœ–EI3í¢³¤8M
6c§´bõÚ¤kD®@¸Â¥hæ÷=i®xe§g›	÷dr§	5|Ù–]—€´Úë·ª"ŸGoßü#Gk¾½Í~ûRßô;H¸xTp¼·ı[+ZB#Côá(1eèá·ú§¼h¾£ZÔERmİ£‚b²^7›§y½œ'Fû† d§K¶ö—ÿnØENN(ÉÆ$qK–Pèÿu6¿íwã,ekÔÑôÎÕ`Œä¦\ÉC‰ æå_Cn)2rÈ>­rài¬­¼LtÎ
³y‚L’+`[-/Âyëäù¦ƒ:t®#3¾MgÙÚUşVÆ©ÁÀ¦ü|‘œ]^s.Ò¯CÚ½L0<ÏH'>½o²VÜÚK@#íí9‰ó*¿ƒT°±!BŸ¬Ê
•fÃFp®FFTºÕãò­â:¯1 '£‰ÑÊFõ"Q5õEbm½Âû?dU~›g~_i¯ôş 5EA:áÍiô8t2›ãgâU	—QÖœ×¸rÕå$cÁÊD‡P^HcÅE¬íÖÃÁ’cÙf¼`çTs¿96ÒnÊƒÃ¥`»=¸ÙhP(ì<&ë4/eÈ*Xª°]óuDMOeö%Tk­«D<FÆÇ6ÀšşrÑ{ÿOè¶à‰3	+±Ü[ªÆH“+ÍëñÂÔ*zhÇ]»BÜè'ÃÊèi²º.ò¢igà+í»ôKw”"ÚN>mÓ¥ÃW„m‡Ú6ekXO:eœñSîÈ±p€O>•.gÉ;_]Š›miYD:§é­‡oz˜¯{nïâÓÍ¶Ñİw¶f¶ÙB;šÙNÑ
Ë=Îi¥•|õš‘:D¨—7ö®
¬Óü®%¼"X>^ÂÇLØ<„.÷¢j®¬P8é®„‰Úê4­#	Å}»}êo(:D3îâp°ŸÆ¡kŞt0KÄ}#Ò	_D‰Š—*öX‡õ]Ï ĞìcKˆÄ4~1Ó™™gÓ0ô×÷
c9ÏoçÓ¬Ìø…®“qÊòs’/rîŞ´»7’Ç'‡ç§ãééx2½Ûb0™<Àíxˆä³ĞwÄ±WÂ®±¥ŒP’òÌ³_X£ÅÒe\á!Å6•(hi•Uº5².î­Hƒ‡<ëfføk„w£#ª2[“»Zyuw\­ıútÒ*!ò,ÄtŒÖ|3XXõ:ŒÅ7¼NÄeíl¿¾›gÒä¿i qrqàøş!{´÷„I×Ò6Æ6[v¢b¾ûê«0rq³°ÚÚ¤lÿ§öøi	ó¨ÖbCN¬AshÕ¨ø|ì–éGÍŸFûé½ùƒÆÚ²ÚĞ,#0”Ÿ‹O«‹œvG1i•‹øš¼óób[ç5‰^WŞ:¦óbà«
‹‹J€B±Ô9ÓIëô¢§_´ÊKïÓwÏ,<íy ·D49á|ÕTòÌ¤‡hEšT½™üc_½óù9Ø¬
Š\¸aù¸w,C´+ÓÑ°ïı¦÷rÏ¹
Ï×p_êıùõåôüíôhôg×ìÚ^ıôøìúj¼IÍÉøğüìh³>ONcª[ËËcŒú3mA ïö¾
ÁÆc“iJ9ur½£ºßé39—òà¾e Å[FW¸3Y7ègnu¼³+hşvH—ù¯·eÕ´cv“'ì¸¦?Š±vxo ‡³©›¹Ñ\K[>ãŠ
Òd
¥5Ù¡=”´kİ‰Ëg¢`K<èÄ¯¯j[˜©¿•H×JÕ ÇÑ*ÆJ¨  ‰î“úÜ×>hÇƒ˜¹I,76;®ĞÖôÕÑCèë:óFÕØ­Æ÷ë&@;ˆğ–9¡ÊòrM¨úõ.ŒíäJ¼Ã“óÑÑôíùå£Ë kìQÈÛMô'Š¿ò5ì¢`_x«¦ ÙïX"Å÷L²ÿ¶÷ª;—$%ÇA,:€ÏƒÁ&ªá90W¢¨<Nğ@¿FÅıÙ Úò‰Æ|²ı“F!7øzW7*®&^EdGúbAv°fÈçé0D>_SäN‡‰%btÂC@A»„:)Òr1,²Ï<…gä]8¸ú°îS‹ÖSOi“é|‰è3êÀlIOAİæ³í“Ş«©¡";û=ùg\Å;µ*&%ˆìö¨‚<ÍìòaÛªæå/ŒŠª—ºë•=ØİGÄu¿ÿv''P€-•k@jÚpNä[£I¥AÓ§/ĞùæfDRÄÎÉí³Y•-—?^b²;1ÄÚ˜¤RÁöö(³
yé“Ô{:Š12ƒ¤¬€„tÂd‘†˜ÚéL™#$ÑIu­~Q&MÌÛXDBİ¡–`ùÈ%º‹¤JµV56Õ|iªB2¢9ÒÛ5\8õ£a¨·.ùIxrÑŠ”¸=Ò¿-»#—ÿ"R6MÏ6à£·¥F}bV‘úõ­t`|ÈÓ¬œ.«rÆŞæÅİT‘£¹Øé{‡T(`L7ó×2ˆ©¯šVÉ²Z¢ · 1K6Çu0”ù„ä€¨‰c>ôÑòÅ4ğ‹8Í{ã¶Õ¸mûšöÍ´ì›iØ·Ğ®/D«îÒ­ÍµémìğlªEÖ wÒ7…­İYÏ«q·[
p
ğÄ³ñÊöàíxT=?•R:o°‘ú©‹å “‚<F»š&Ü73…$Š©ÔÄ).Iù{	1ÅA¯"Ğ±?]æ¬k²J¬ãÏo/]?¬ÈÜi²ÓM«b¼v|màjÕ&Z‡G`½Æ9Dn«r1U.ª‰ôÛXıÓæ^R#öku:&hok²ƒQkÃ%cHï3Gh.“ú®æö2…å£W®ÔvqÒEï_çY/6È2-ÇFG¾‰ i¡[¶€´SÙ6‚}¸Ù¦©ĞïVµıSxgÁã:í<{Ö^:änÏ/ãÎ GfÆÀ,y|ö®½	“/{Õ^¡#³ Ï¶g2<kF(.OÔóW«Íû¦zûTC²nÛ¥o(±Ì´Û_^ ×ïA5"1ŠßÅd#Ÿ
÷z êºT	x’¿&(§¨ÿÖ„7v#Î‹×ş[`jRWcQ!+èf+2Q÷VIı±ûQÊ9PË+|ãÂN¸ç>®²¿üÔ[²ÆÏ—°|fåí½ÛÏãÉï¯Rºà÷²®/i±ï6¬8ˆ{ÓËn›µÀ½,*z€e”¾0µÂˆÖĞy/“³Şn@øõÜÀçN;’Ş U¼ëá	İMkvæÏ¢MŒÓ—¨œJKé#ƒ’ú† Ÿ$Ég<·#¸8òØÑ‚·ZYrrà­¹àú;:MšìµÖä¬µ¼ŞÀö-ûæì}Pqo¤‘lùNcUœ:½
2Ñ?Š>ûÔd»_Š×Ş–;ãJÛ¸Ñ/Ï…nÁ}Fsp›êâ‚Üç–›@ÃÎ›nÜ½ŒğlÁw¡äDW]	2<aí˜ûš1Ñ@$7èy	E5˜ÛºÏÕ:Áé›ÅÈ<øæ]c„DÛ\¤g±Ò.ã¨«’vLo½Ò½Ï¾-+~p8ÃòÆœkzGŞÇH÷Ùzf±ïÁ[‘$T]Ÿ¤öÑk½]×ÄùºÈAaÌšß>â1Ë0JIHÈòVÄ¶ø$©6·à^¢“öõ¯âVÓ£òSÁ¶ÂšöXä²rÆ.‚c‡9CšÄU¹Œ9b-¢§H7­H4nG…KP!º¸TøC}Fûğ>Ÿ§"ï8+áÏÇz¬!²…\fêÇŠ=FÓğ	fa×4ß|û}§‘4ß›\G@ÇÃBvÁ0èüö–Q‰·eë?ê®bÕ´ÎœE.‚­¦¬·ñ+™1±µHJFR†HáÑCÀÉğ¸¨3ˆÓæJßˆ>cºÖ’H4ÆjL’Û`ˆ-½(	ÎğyQY¿hƒ›T¹—'İz`5ôñFIˆô‰Ÿ$åzÅS‰²CP4!Y§óÕ=G	ƒÀÊábæ|¬ŞssF˜>³{°k'Y^]ÍOÑ{³Å;ë7‡X7AØæ«¯7ßî_ ’‚ëEÚ¸7CÌ"«î²#Ì“®qòšñ¡uí–:‹»´©ÍİRpù»Öˆ¥¥©@´•Û7[şİ€}.ÚIØ‰Ô=bh'xıZĞ“(?¬Ö™Ej›ÎbncÕ#ºıÛ*¹ƒ>²»¼¸*'÷È+93Ñw#s{ÏIôûl¾ŒH¼Ú‰ãÛÆ8N‚€iĞùd4È›ËåyÁÄƒõ"|ı,›4ß¼ü¶UF 1§Ÿ´ \ÌùâŒæq‚9ÁŸº8NÁÅ;XíWŒœD“[•ºÀÖÜ¯îkSU&\=´²¸$*ˆÌÒÄu…İCÄb$i:aËY—Zb+õÛxCÇ:;˜ùàÄD†–rIÜgà+|S®Vå"HÕ´‰õæc8—±¶qb>°“-¿p„ŠK­x[°T åNwâj„"d'øcÀL ¾<ap›ğQŒ¹âÊEßöXZ€¡Öº7"'4Š¸³xá:z,û/}»O>Í 4S3£±wøÆóœÊ\¾ìó‡£cáéáXĞyV¡+c­7ê™ÿo÷˜¨ÿÕWŒà0^p™ìRß>ÀÙŸ×ìß:¿™gAr±Ã=;^È9*ÿÓº\uÁïpDx¬0iÔ‰ç'õ§»iwL¨°®— ]½Œ®'yDß9çl[´Æ[‡—ZÏdMÔ‹ŞX¸Ô§>˜2[,”bŸRnâw7šÍ†TÉÿÅ8Wóâ‚g¶†°4êDĞyèMÏÈYR€rÁê¦å§â`Í TDš|Ë™? µ"÷âyõI
uùªœF.Ÿ<•¾5ï¾æˆÃ&Ú•™qìüØãJä}:g}—<Ş1B	Áõ¾R¯øÀ`7ñV:¸¾º:?›^ŒŞ§Gç?üw˜€0f(É6zŒÙ~‰Í9 pâÚŒÈcğä»p3ù–¨vCÖ.ŞŠtG“Ï–’:–‰‘$µÔVl&|ÊÛë³ï<œÎäîNpm:åUMÀïyn½¡£w[+úuÚ§ã³«ãó³Ak«lÇğHkqt+´¢Õ¹6ZºîÔ°¥µ-zĞr&Ÿm,hòyRKš|6¶¨éğÚÔ²&Ÿ/ga“Ï6v'ùìOòÙŞò’-,p¿4D6‘£ ÇÑ¢†¼®@N›åù)¬É2„³ı“¿ZÆuNU$ó6–”©¼BWË%Iöp\½µXÛ F€%³öuaİùè8à×F´g˜h­VMƒm VO>ÑÒ=©ÿâ“‹²D«I=‘ºµõ›Ú©±Ñ/a«–£íl¯†çŸf³†§s*-íæœ6åm›”×U›×,™¶ayÛ‹iC*j:ûRµQ1{Šk¸ °¸-õR.¦š=ÑÁ+¢¼Êèáhª¾§ƒ6¼ ¼-4nÃ4ŞOk³óxÛ,6›Vƒ«nş)^64ˆiİ\wÕYôÑšÛ’
Í±f‡Æë(Ë«uq^\ó€Ò> ß›6äã±; °E^®‡äPw(},‰”²Chq¡ˆ5€pşm_[M#™ü„Ñ$§oêí<4._f³ÇÙ<«TÑñéÅÕŸ§Ç?NGgÇ§#p§èë?:¹x?šNG'ã§ähÎcC·Tï,ºìßön÷}ësŸ1Ø›Œ|P÷¬7ù÷7­Ãc‡´'’n~HæëläŞÇ;o–Ìgë¹G‡nŒú‰×Á1Zˆ*ZRFÓGK­¨%Šl¦ó1{ÄÉø/ëuÀ%ÏY%ìzS‚ùÈû±ÕßåË”ä,%:§°ê¡yéë ;˜NzÊ(—x¨º<máù|.Ïéö”ò	h<’…WØ„Ÿ“r¸doì“öŠlJ´}lldîGE»÷ñ!~ß9;çáO¦ÌVrz¸eú‚ÍŞŞ…®w¾árºº3UB´^	¤ äl^ÖÙQ÷âGùí-­±¤´Œ€ƒŠE#M=ìÓÜ¼J o¢Lnj,/zve#
“'T{½Y !@Ş¿ÂÚ.k1A’<”º8–ÇÌ±U]œ/ä
l	ŠùˆÄ@ÌH"GñÚÄ‹Vºd!‘l'Ì˜x05°,Û%‹¶1 D­ŸRÄ·úÈœw¤Ğî®_ÀÓEy
v¿Ô„¨Î¹à4Kê5cx¾Lä÷Ša05ëWß„fÅÃÑn)mAæâûum2EÚëáM‰Šıƒ¥´Ê;ğâ\Ş×| éªõ¢¯UvÎÕŠ×ÌUÉÜ±ğœ1dóäQ?C¬ƒÎ	°>8”»¾¥º‰}ß1®w&!ON—åî"Üú%âšv÷øtÅ PgnièRóÍÀQŸ}88Rœ†s”ä6^yï9)\Ç,^ÌÚÙ3c›™öœ_D˜¢¿+Ì£ù¼ü”¥z‰šÉ¶IÁ·»ò|Û‚ã]úËOwÛÂb
ê(ÇĞáA(ƒ¼Şa2Ÿ_c÷t¡gXŸeÃ°¹¯)•TArV²I’´Qã
h]“íKbšÛªL|¹)“*“Ê¹PîÈØV
RÚ@N”•@ƒ¯~ã—ÖÒo¾ú‡h:^•Æ Ywàã[¶ p5,òj…ô‚èXïAqùWGŸ^ç–F>á»jÛ˜aUˆñ&ñÅ8DÆ3l¢uiÕR¾®¡şÌ’n‘×äÜ±DÍYRÍî%1%WÑTçF_beì®„K"0t X|9ÒıÙt<¬‰Àôã&™}TÜ‰×Â[bÉÂ‘Áº è*ïgB
V*±±~Ê‰olµ7êpg}o·ËÆmkoà]©¦(dDd>7^İŸ©·)¥Â¦!µîÍÈ>lñh°Ê(³aWu›ÿŞ¶Œ_šÜ\qâWş¥ÕŞñR7–ËŠÔÕ#¡ıºÍ‹ô‚ööà‰·©vR‹)ê=…€¶Å
ƒ¼ü‘RÕ•m:ºí:Ö6äuÒ|!]É	ÀÇ™Pø ÅPû4 z2½\ƒ:ş;äÈÒ¼ÊfáèŠ
çEÓaŒ;1´AŒUd4µá°š‡&Po%ÔÇ÷Ê4£XXƒrN/ÏON¦GÇ—ãC´Ñ€[pïÛ®µ®/È”Ó2Ù7ÿ“õïø¨Ë¯5»ëÛÈ¿‚)†õàæ¹,İãäœújvvä²ş #êÉZÙ¶eº§Èl&¤'1a~A¢JˆbáÊ@†­`3×ØÏÕµ†³~Ï×øf¼^‹îc+¨BÚBa¯ïèBÿÒS—i‰°Ô‚Ÿ%9Z[eC×ˆ
ÍÆLÚN³ô­pqP„¤Omd§§xòÎ/ş¤·Ä•R³°ï$ì#€æ/ìKºàSø°âb.¦¶Çæİı:©4G«gD2vO¨ŸGgÚ¿Ñu—ˆ‰"tJ+ä+ì¦Ã ?TÉI…)×_eŸW°Í …~•ç>®Ám¥•zÂKŸ0jTğÉ¾[ÆXåTVÖuÀ©ô¥ŠÃ”	E6wâ3lıä»ó3ÊÁD¤—?.d°÷
	å,qcÀ©€µ{£u¡w¢=àÚª¾ĞZù«&:n ZGEÒY¦”"ç†ñÓ|eki45™²á^°C6_/æåìc–ömõÓFQ[uúzÊ,8˜ÑCéÕ4ªìĞf³y÷®ñèòğ==d®Pº^E„1Z:G<”Ù¹OÉ–{f‡5t{\7 7D(š b@LD¥eÍÓ‹„m,†<ØÔ¢Tiú˜yõ\€ş4-1E›(¹½-Éy§ ;ˆlíšOªåéÙÓ[hg°-B5ìšö,ˆrxş<ßÌ	È2‚ûå}³¢ëç{nÈŞ2Wr+OŞ€5Xµ…È.$¢}%eó¯–a¨2Àã¹ÔåXö8 WœÚÈ—¨c–>úÿKË* ˆÆ1%e¢`Æ¶Áı¼âø—p¡™á™¯Xøá°—TwÆÅ\Ü•P°½h.?%¼>`,2èäÜth[B›.à§ËÂ¹”ãº'em©Ìw,¢A°	ÿ}yj½Mò¹ë7nˆ¬8»¸,ì(€gLæªß­9OŠï_ı¤GÒ©¼âÜLàÏJ.KôğÚA»–h˜æ®½½<ÿck½z~4=>û0:9>z6Ìş¶fp?®'êİåòú^J>q]ä [•ÿ#ÆS®ø½fŠrOå*[­+B+èİM«6[×Œo™¬ÛÃBôwMfrÊë‰¸$u!9œgÅİêÓj}ßëğbÍ²~ës»CZ·Z%³{ëMûÄøBpFG›•×t§—¶´(‹]3Ù±ÍÜ:#s¸ß¼¾(—ëeû š÷ŒÇºqƒ5şRm“—ô6‘‡Ó´æKË˜ÿ¦™¨2=.¢÷ûá†DÏ úU7/–ëUÓøñQÏzc_àT ıœÍBCFõÙ§÷I}™ŒÌ7íÕ1u²›¸ûFì€ŠñÉŒÚßÀ¸1vÀ7Ú 	Èq$ÆÚ*ŠÑÕˆÓ>‚cêeŸÙÙÂÆ±´Ú0úòóRF3ÃfÉõà+3¬ãË9Aâ³ŒÁE5ÕEÔS™¹–¯‚gĞQ9[‰ocËÌÒ=1*õ[NM{ŞÓ)<Ü¯Õp¿şÉÄ¢ŠĞ¥Q56v”Ï’ùÕızqs‘¬î96ñ¶D7›Úà!n2y&Y“hğÊu5cr{	vWêz¢Eù×\]OdĞsî+b’Ê"‘W5›øûº¾Hnñ/(À™gy&“z–OÑQšBâ8Yr€H½ÊWóléãÑºâúF9ŞáÑõ%+:9?{'.FÚ(ñ<>¦€®E˜
L:’údÙøÒÇÄˆœIáGÜşªçÂR›¾èCĞº$Êïšp’­Î1m0ÒÃ>Weú#ã¾Äæşû©7õnh?æ9ùßB*&ù9·Æ2€/Ï×üÏZq ¾oëÆ(%ÂCÅë½òè¡ş=Ü„„*ûûKòS›/Š¯^KR»¶[ØDò£H)Ş,:"å¸»±BÁò*ŒĞ#X5º÷¼.Ò²­/Y&”Té“à¨^€FÀj”óôp]Õ 5¯¤ªÏ‘ÚØ¡ª}A˜ª‘§Y9/!R@ÖÁL\ ?á³O™ ê0"ÕŞû|+NÕp  ±±7¸dÿû+Y£ÓRSM¼ìÜéË¸Ncèmš§Ü{mĞ‚û‰LV³f_d@«¹ßkŠÖRqŠ_qÖ÷…2³<Y­ö¥%CåóbÎèª”…W¹=¶“’¨ q~*]­ÃcŸŠ•iBNŒøó5È};û¢j(vÖ\Ùñ°Ä‰}lÀ¦Zñ%1æä!Y%U½µŠ
ÉS"µ)éJ“õrÛaoë(»MÖóUs¹‚=ö¹¹ˆóˆEt}:SRšVJ3#ŒUË‰¸ £6í¸×Ë$%Ô–	m3áó¸Hr‡úo²œ˜Pì6™e5E(x™Ó¤şè¢7µš}­Âö\ÿ…áõÅÑèj<=M~˜NÇÂ}™mÑNUß®¶©?>=ÿ¿ÇÓÉÕèêz²ëˆn.³ÑC×|‡)—¹°äé×‡“&«¾I–ÆYD÷ˆ;:ùÃQDËZÌ§ëx2–\óxÃÀ”¡˜ŞxxÊëˆîµ)‹pG¨µ™	BWH*[UâR#k’X¤ÿê¾Lİ‘şt|z0¾œlˆ÷Æw@yTÄ]ğlt|ù–Dƒ®ë-å2g`2ú0º]nCˆ¶j¡¡bôaÍ>
ÁÚ¦àx"N ¢>Û9ØN£¼Œ/§—ÇgWş±~ù¥h7/‘¤YîÓÛOéAÆ$vYœËY)nT##ß¾ 9W¤|?¼wâÌ¬ğÄ| Âû‡„ ÀPÁÈ‡aÚêZôÌWÃë–f¥“„s÷9Êğæ¾b)½Vó6ËávÜM2V¹5iTSrKŒ‡Î¢Ó—±ñBƒâÿ£ì¼æ–ìGQ†÷çgãŞÏ‘¤¶„\•Kô§‹î=CAÆ‘¯²Eh‰šRZ±¾?A‘_ô!?ÿ”Wps‚r³úÆæ.¿F©Ó²(¿QUN'ìÜûéÆYá[(<Í›ğ{­¹YRpQÜiU]ÿ¢¥YÛÔØY Üœ4'W¢oóyvÃıgºı¥GVæÏ¬Q¡˜ˆîàAATUÉ#¨ú^¾zoXià÷ú¾ïœpa¦.å®:_T2–é‰Búû€·$:ÊÚ‚eá¹ÓLª.ĞÅV÷5ß‘î¶£ã#úËõ»wã	&mI#Íz‡ïÇG×'¼Õ…áÚ«aÇÁ&İ„'Éº˜İ+³Dı$·ÜĞ¸'A3`¯Óü$™Pı€Á-U²¨ÛI9à†	fä ¼ —Í½]år0d`b'„è»môöõ¼H³L‚	ªœ.«ÜCòânºäş?Opã <İö%Çúoqælİ–Šv{ÏÊ'?3ÕzˆÒ±Ş°˜qäW'€‹ì.Áxe‰ô¨Ô–BìáJk'H&üöliÓ®3è¡³MÕd`døtæµóÀv,–ØA%\„Çñ)fÀàåAWÜOI¾â×‹øár½²RH“o0FfF	åG»ç°ø±š¦e‘=ë¨wN¹¢Y»
ÖUWš7ÅÚa:±Ó‘Ø]‘)K6è¯É:C)á1I‘á'I!PäY,{ˆ9áiwHÌ÷
:qqLü€6¡JY¢cˆyiı_f²Jf!œJ&n|¹Å,B±É*¨öbqy·KıV^´'äfr³i¡Xi™M=¨7ób6_ÃQ§8Ô4])F+Õ²¶ßib×}í˜-~ï,•Å.ìvá[€äµ°‡[LÇ¿øb—^¹‰}æ89ãÌh´ºÏ2™íï7ú’‡.8	[Áàqr¸x‚³^-
ÔÑË~ï“Z>!{÷"ùp;&ÆêsjÏ(/K¥<<:A¶±ÙO¢‰ö[ÒrS#j»ïÉïq€6ÚJpxåíq@ÁØÃW´%sh	·¦ç“Ÿ~h<İ¼qîƒŞo÷|éè;Ä)Åİ‹»Ll
Ô¹û­µÿãÈµšm[Uµç1øûÈG yx·Ï÷y“`Ì¥H\cllo™µYã ˜.nŒ®Ø`vHk:hÓ|¿e0³µß!‡w%_Íù* ¤kvÊVMXçŒa³ö›æ €§¶$¦İ- Øl_[ ô¨;ƒ£;åüë7LàùB7¢E•	7}¶”ƒXâØáÒ¢¾i­ï½!ñi	®qa;rg`ô_H¯^ƒ$Ğí²YÃoÅrJËeíŞMØ!Ÿ	{nG @Ak¼…úÇ =
Šv·$>ÿÃ™¯ª‰2ˆ6ÅcÔvÚ¯|ÖvØóûoÊÏí‡e"YÖÖ #ü°Ù7r°ÉçcxÁ¹?´Ìr`7„_î^²5f@¤_õœû«¸ê?vPŸ™'~Xsfå  ÊLy‡d(•×çk¡Khx#“&«”qûÑ $zà—6·ÇÓ·Ğò0µ®Ùtq´egŒÌM«?zŠ€ÆÊyY~‚ø~35$Ê(µÛŒ¥bhh}o"ğZ/˜‚?ÿÍ0{3Än¡ŞÃ¿#f«ëVI¬ÈeŞÏqüÿ/ş“Šâë/Šb8Té|?Ê(~×ÎW0Onv6Â‘=Y&U>«hömJ÷›‡M"iQo+6‹`ºş¥Û}p€TÔå¦¡.ò”U”åFPö—Ÿ° Ûö1{Tµù5X7Åâ:=á•€±Š¡Ü¶Ù#g/[”ÿğ@^^Ä_th@äiè-<Ó\9g›AĞh¯¤¤ú«×2ccT–«“XJ
ÏV2<ÑTUrÊ
Ï“ñíSjö¤ÿŠMùÙ§kıâUÔ‘½xñ4¶x.»óö/æI‘/Ãô,eÿ#fë3†IÓtR|¦æ¤cMUÎk#Ñ`k(æÏ&ÙŠ€w½³O)„uD3Vü+ífÄÜ‹Y·¹M¼•®¯®ÎÏ¦£wcÌ+<ğ€ú‰2ŸJ€Š³­ûÄ+n|‰êá#Èæh9ÍUÿ÷ ¢æÜùŠ>şÍN‚.„º#éÑIİç5$$8„w.uî¤ÒK[‘<N­¤lTQŠ;F¬êqªeæml…Ò˜ØB"£$_æ@iaQ¸0›§O£9f0ŠÖÃkU®’ytiıvQ\İàì8:u—LÌ©­Ce<„·•¤PÔ·±¾µè†–£n ãZàï7‘¼Nù^Jën>5Naæ †zC±c¶ñOÔ§í&„Øø o*Ûª§˜Vû­CÍoJ4hQp[Uvr£òs¸"³1 ‚·?¯_vš<Ğ5[åàñ(y¬åe7xyèäÁô×n+/%çè?'šÃL>Ö›ö£¦ëNÊŠmw¢…conE9à×^EÌúÃãÅx6S`ÁÂûŠup•ğd2®ŒDgà9±_>­·&Ú]Äf¦‘‘¡¦í‚I?ÈT÷«Dƒ¸›|VG$f45ß¼„7âäşkúatr= ³v;ŒhNc6w|j®ÕÛ§ÓĞÃ}uzô2±ÑÁU†j¨ø£Ë(Ùx´ÄlÀc¦n<»ÁÑmípCÈ‰¼çÌƒe>·Ø2ÊY’3.Uzî¯™ğÃÿ·3ƒ7Ö·à@›Ì É¡+kHğÀ6’0“åã©t˜ôŒ	=>èå5ŞéU`&",­ç	bÒÙRÈñQW¥i¥ÛüÅ6h”ÿ–>‚>Ïg÷Tö5æ/u‰q\=7ëOd()=¿’d–`6¿†“©º„€æÒ…`”ô_W ¿;³f57ôı7Ü¸CO–~y¼>á£áË<£ü0$SwËzS8w“C}¢*Îf°6‘C»dÜ°±‚tƒ|S}ÉY¨ #ÔP–õîAŸÊ°-ÉÉêûJÜy‚“-OşH¤q´püC '”ú†7›ú-dÎU:ÉßBvaëÀ"à	£ÒÀèÒN7õäïvíZ~àÿÎØJSQC7;_²˜o~9×è[¿¯\ÿĞğª–0ÖV¯x´Ğ ÁN„¦Ü»!¯ÜÊß“ ”“oJÓ´L–K9–_égs‹bQ£‡Ù·ô>iWQø¯bá-ûô¼Û}ÇË+#ÒSó0¾>c¬¬˜7Ôşæ mñ³ Ö_ŒBÖ¯ÿ45°Ão%ó0¿ÿ	¥/ºi³JHRs&Óê«ÅõƒÄe*ÂÉT¯uß¬(µZä¹à x¶3Zjç™³ëkµOÎ`Ğ$=ŒÏ¶‰u3‘Mß¬˜o#/²uV ñ‰
lßÏØÔ/3à·uY!Ô¾D:¸Ú=sBwAÛî&4É2,¾o›×àŞş(rëpít>øÂ}:èXqSZ3 o>Ñ^ÉæÓĞÈÍÃİä Œ	(ÒJıM$g¶cóš{mš!ìzÉ–HÜ<ÉËy¦27_«äÁl5ÅhdŒW™šUÊå@x&•Eê+”Q9,ZZVM'xJ€hxa¤â/Ø‚ ²Â1W›á‚IRg5Ô=–¨1ˆÔnÂTjÙÍxtLÑfÕÊ:û¢E´ÆÄCëOÜÎÓÿHäe[j¿÷=İ“×ö5´YUcoœ^V`·š„Aúš%EŠ66é†v«–#ık¯Ûpc·0.$O‰G„ÙoG*¯"¢”Cáêÿôèå'\ş›v»¥GQ@&¡ùÓõñáÓËñÅÉñxâÏ×c
ÚP›ÁPİ¨ùˆ!Yù‹èºq{®F¯ƒÚEú<N²êÁó<hW¿Ø’ô/ ~‘•L Âl€†Œİ–$yLñÌî>ÒD>j5i!HO9g<­‹¯1ßä°JCÔi>-²O§4¸¨ØYx|ã‚ì0²µAÏ{{«“Y\ëMayÿ©ßç4U’®MŒ6¯çé%’!d;ä·¥*PíÁ,TĞ·tC+?ÜyÃ³Ù‚ SV‘7ûqìĞ"2:ç`9¿ı!{TKµë½‡79IO;ìùø|,U`j²ÃSvcÓÓ]VÄ|e;w9²<`sé"†¡´	-VŸ.¾!òñûˆÄw.ÀÁ)ıø¯Î>› MÂ‹7r#‘piwÊˆu§«³<Û8Œ¸õ7t‘ƒÿ—[ü'ÕŠ½Ûº}@>'şØLw<ù‚
8­>G¥o|j¹ÖØCí:xIÿÙ—]C×·O³È~}µ®¼8MàqÊ»	»gĞ„ÊN|Û~/~Íğ¾L˜¥ÓœHÚâ˜¦ö{ßø@Õ´cYü¾!–:(‡-íÄÌDY½2&t_‡œMõÒÂ,r@ûË‹PÑ-ÛtU=Éé’»´‹ß™LFftxœ‚è´^êN	jXf	)RÿüsoÇiKüÌWÓ„ø:i- [ÓùùçVâêŒfÉ³†ÒÓğ"JÏÌà³¿­³ê‘¡È³İ˜^õ9²	{sJìXµ1íxúôw%éF´‚^ZÀ„ÙÙ}¯?ş<Ë–x=™×Z>2sdÖ’GJl@ï`w¡ÎN¦_n¦§í<_ù˜tê¯¨Ôå<ÅWƒÇütš,•/¬>š¶ÈÙ`ä!IôÚˆ{æTj´Mx`"x»#“/åt¨|>!:«·å©Ô@ÒÛ=Fãô}û)}™À!Õ½ø‹+û8½­Ê…_†4¾û
ù%£¦ÃŠ|É.—£A´ÆäVÂ¦E² s"zA«:Üo`^Œ<ÂÊ‡èş´éÅàXÏZ€4ïp(yQº^®ˆ{\b&}/hmê¸ü
¼`µÀ*Y²ãä,ûK°©¯™ŸyJ„T‹,©×UvŒ×¤(ŸÃ¸»R:ŠìvqGDÙqm,Â)kE”¯³"ÊûçLz:L >r2>;Â›jğÏ+ºQ´K_KŸ)ıTvË'iŠüã©6É!¤•³ø¾ß…–‘QkIî½'wußRıö§¨…d¾*/Ê9?XùıÅDÅ “—[|Œà…ÑŸƒ^ô(çĞ½‡Ó¯5àe}Í€Alè|Äƒ~¿‡r4ŞS÷=7ÎÈ Ò»µƒH
PÃ:Ì fğŸUTnT9õÊ™„n`F½|Ê+’zÏŸç!R…~Å3¨ÑÛ§[­úyk¾¨oYAõy)C¨ör—w‹†Pÿ¸
P©MçĞò4Ö7l”H†~Û%õKRäq_^Üá}³šûŞï—íÇ6nïWpF¹œéRˆØ}F#ÃÛ¼Hßæàú­Àu'e£‰t·UæGC 0+Ÿ4®$Áà™ØÓí6|ærS!Ù­f€?úŸ¡¢n"3]™–óä‘10€mpˆ;TE¨ÀQ•Ü®Şgùİıªi´m©|õ†e"•V~ì°â¿—šğò-iÇßÆ6ëXG³öÌ¾Áîÿ  ÿÿì}ksÇÑî÷ü
PNAK²T9¦\¼J|#R4IÙ'§ê-ÖX’kXdÄœäüö3İ3³;—Û.(9yµ•X °3Ósëéîé~Úua¸ZzéBªŠwY}´\#ü0^‡ãµªÛ€ªJ¶|+ºâ¿zŸ:[ÄªÚÂz^xTU¥uåüŸ¶V¯..Ş^Øƒ˜|/f\QÍ=Èæów4¸KZ–ä´I‚2KÈ×3ğ¢	¿’¯0Àî•§:`Am"§}0Íš]dÌo˜¡qp3İÃ#š'ª®ÖWÅMäÀ~y_³cğ röóy¶ª	|¾fï` Gñ'ƒçß‡™-<HI3ÿUËP`ÍøñŸŒ·ï ãMä/9À9
JxÏ%×6à¹v0ŞòÏÈÍ¼?>Î‰9€
¥­Á˜Y–™“oÊ«r…‘¾…#ŒşuÇ”Ë„…×—\äàÊÉó¼¿[òŒÕû÷ûeYÓd’ƒé€Eáø¾í  ‘fg°ãå¼üŒDeyÍê³"Ü÷8ïán™fc2ƒö´×l½±È	dp¹»r‡ÙÌ‹ùl¡¹"ì>µ8öì÷AQççr3Ÿ1ÌALŒ–h«ŞL®¶ÙÜ¥¨h‘pKÖü<ÑÑëŠ‰Š®¶[ÖÅjÄ‡iÔôà1wÈ4gw4øV[ù‘épsrvf.Ô6Ÿ+øóãºÃÍ­ü©CòQRf>Ğ‘ğ]>QxN-ÊßŠólúŞçÕĞJ6EôÈq@etæL‹IfrãØíC.I9"ØÍ®ÀÖº˜«l¹
À†G‡äâÈ[d…úPW8U <JgN³õİx‘}¢ÉÖÿàø]è²1jêx ¯‰–D!Ù ¡Ù¤ò£f,&xçi1Ÿß÷›ÁógÏ¶ÄA¨å³ÁÙ‚×I$ÁâÏ•ÁˆËlïî§'H\?†!2F€ÇQY+&¨ßw7z ‡{Uˆ-Ç#)Ö{lüÈı¢ ¾Á4…Rè²RşÔ’©g4´£re_…‡£ì¶ıÒm¹>ï¹x‘Æ-ğ`¹Ü¬Vè¹Ô-Oî7´lÇJªÒ?~ãµÓÚmèoáè×@/&Í×›ÚÏÂMAÊÛøğ„"]Ü{CÁô&5zİu¦Î¦†;½%ş"¶³kˆF€™°÷ø6ŒR©}‹›HıÂe2ŒÒªzë +Fk98:ÏÎÿ8ÖÌ†)©İÏo‹åU	XeK)Ü¥¤dHĞŒ(:BUù|†‚Vº¾ƒm.Å9‘nØ:vv]\?„«kV‘I
ÅÜ)^ô…2@YMx|WU˜&œÍlµ†«Otí/	WvİuFn\ó–‡MU¢å1]Å²ĞP>Lêx¤k¡¸(CO 4Prã¥{¨İîgÕi¾Ü%-Î6KÁsÃDéæMŸãŠm!òĞàá9Ûá§uŞJur•&qÏ8KËPæ3¹ÈŞ.8LÒ¿J504Œß`Ç	Ëø|#â-;2ôíádêËõ)KeØyPYõít£ˆ8ÌŞ¢ôH,|¹™ !&Ş³©íõ6Êë
ü¢¥Nø«HXÕÕÆüµkòòÒFæó¾Aòº’èÔ©¢­À¨³©ğ©»Ëip*îŠšÃØ³öYzî2¦“p¿XdËYívé†gRò~;˜ÅK>hdxŞ]¼¹\ö°ísÌ¬êñ(‡ÇeÍ±4ïNÖœö W¥&—OYuÜk7ƒv›:”s^*Õ£Ş–6âŞF@í…¤ß¼„“c4ÿä‰ÿE‚Æ˜Ùì¥¦ÉŸúÃËöùbÇÁD¥ği{4“Ğˆ´e‹Ug<Ù"}QuÌC	Y·ÏS|s§™V^gF8ÿp#>‡9šİ˜`/éË¬!Ì ®’±X˜@IPT‚"‚U€‰âaÎô!êv9ÒêÉ»F:¸ãSw»{§ËîÊĞGÁCÁJå43ıÎ6§lıæ!`¾|09[>Ay[òa1BNa·'u)BŞö´»}ÉÑ†o™;¿øY_qT`ˆ_LC#z—¤EË(Y´6DÙ{5ì¦hÃšÅ-xùñğÍ¢$¡"D!£E!«§¢“î¨…'í +é†İÕÊIO”$B9â¿Ÿ¨|ú;- ôŞîÊ„?—šó-ÑÅ®îòE~´`£3-—o«_Y«l•“Bi¹OùEş÷M^¯è®xÓá(Fš6¶<váM}ÈÅ&†ø^ŞH’¦ûP.infç¢ğÇài
Šk	)YH¨‘à-Œ=#J—dÅÜ^º¬Oçec¶Ö¡M]Ó¬7Æ®tSÌ!’p"ş>öµ€ÅÏ–¦\)ÄrÕbìZ‰bœŒöV«3B´ë;ÍV?ò×Gƒ_kZÎËª†Ã(›æ×zñ:«ï  ¬dB1ğÊÏ!|!n»Ù¼7ÄÏ…îš[|®ÊU‹Hê©şÅªWÇµ\56má(Åéuö!+æ 
ølÖàXUVã³£_¯Îşv}x²÷æí«ë«·ç×'oÏ®/OşÏQãæDàÈîn<Í'õ Ä·•ÇœnnbŸ~ZV“b6Ë—Ş\]–­`oÀ’Ş2^ºAìÃlQ,¹‡šGTZ”2}ÛtGÊPûôşœ{?|WEC/œ½ô2Ú%õÜq³*Ï2Š>’’K	_ÑÔ|AÍ¶GÇ‚0µçòb	­ÉúXiûé$ªWp@?vÆ„"êáÄúšÇùnÓ:wy?àµèÕ¼ÉÖxj»Ğè­¯Úl7Û[tš«mû3<Dd‘@/®¼ü
‹Fçho$)„M,áî-íÚ,­{¹Díòá n-†Äoa1Y¯Êô8Ğ—µW]Ìd<8Vf¯…ğ„µoÅÜ×›Û[¦ìßiVeh[Æ7E%àâÃ@·k´Ù›ÏË—L;|ŸWõŞröª¸á`ò2Â£C{®é/@¶CTËø}9ÛÎYÏğ á†;tÇŠ:ÿ“Løœè–»r‘5KÚ
a*
Ûº1±ŒQûŞl@!¶âÒWäE„¶Š×vXåMX¥O0ßQrM{­.şÔÚíà¹«Eı<Û•¿	O¬¿íI‹÷Äq{:xélgèıîn1:ö {eÁîf…NÛF†±«‹£*\!8±™­• Á}È°)8.æŒ†ÆsÃ'Õ`‚^Õó†ÃñÅÚ©÷ı—¡ğÜ1V|l–s,,Ü9Úê©ùÙìÊ|¾ù†æ âdYçè¾ïKĞåÖû‹”]nˆ­ƒ÷u?oòM®¯š·NlŸöwİ¯yÊwÓ>üü¿GR`»zsÍEœZZ5‹›6uË)“Je¿¼Ã¥mášÉTù’ékÖêĞz+VÜ¸ÃÖëeSâ0_çú§²Á˜6'<Ğ|^\‚¼rLšY%wà‹æ¢¡€Ğ{Fy<3¤ÿi9Ÿı’Í7pRÂñ>'†:Û=¾;¼/è¯"r«©/ËMó¦†œ×–ˆÌwÙgIÜóü,ÿxp·Y¾ß“VG“]xXhí²…/"†§_ÿÈƒÁJDl¹yWŞ iG…F„„%Õ/&¬é‰/a6à@gT¯«LûBÆœ•Ó½CêClEèvÃ‹	›A¤Yf±¢ïêb:Z®ó[c<ÕÅUV[œGÈÍ%hœĞÀÏvoºpê‰ËUVÕ9b£ıhgôx9Xè¥2¼Èê9åÇW §¢Í6DíAEŞç÷°~ámöÑñEd5É8¨z!|jß“å”Ÿ+ãÙÔ€¾<ãfåõ_òórw:}ó¸"«Ö!wè›]× L¾¼çñW0Şná¨åîî¿v^Q+Eş–gD¿ÏÙ1Ã”lvXc”Å†2:Ÿ‰&ıµ˜›ËšéqşuÄ#ò‡Ù qÓÓ©„ÁÊfz+EÆ"Æ †É+Šóö¿‹Ÿ«1„ª1Éü~Ü\ãúm.¨¸[•0«lÉ¤À1–áfÁ­r£­nC?7sÜh–úM…NoCnW¿ÁA²zÅš¿€ev\e‰5`²˜ö¼µ®zm]’•.ó|Æ½;üÚ“·ô••’ä9•Ä¬Ñg½ *Z4µ‚-¥Ã‹æÏ&æ_¼öT¼  ß‡´Am4»¸bÒß
Ko»ÌAnÓl›õpñå­¿ÑT°CœH)è.
Q©şB>¥ğw´‹Ìƒ†:i¢¼`aeOÏi'ji”D5y…	j…T}€ÿ¸N].§!vÃ›ö±oÒ Ôœp£7Ş)ĞG×Ï+òœ¼À¬ ^…5RLzcĞi$ççİÀPŞ·a,©ÄÑ*êğãƒÜâ¹m&ÛĞ8)e..­E÷E±Jå·µ8;äı…ÑFˆYãY l#¡Ö^•˜¢äõ5‘Ø.<{"p2/VÙG0ˆè^Îkåê;¿+×åÉ¯gğ§qo
€àÈ¶•FZÑåÓäqÏ1íù‰ü¿Ë2ÛÌŠR^¿cËåcTÙT×b¡Èºbú‚ĞŞ"„Šx0‡U!¼«š¦…±
=0àŞV ËkÌn±æğ÷pß@Ä_+Ğ‡·:7r
‚8ûG½ueµ¾\|f¡‡åò‘l—©¬“2«f'RÄáiîÖŠNñWñ
(æáö
Şl5|Ê,Ÿ›y™­k¸éŸ	‡uÔÅ·WåŠI«CAÌy¶Díd{8²!˜Úòæ†í˜Ææ5š|Ê³)´İ{ª’öt0™ƒum¯[+ÖPY/@XŞL4ãXy\ö:Yîo&8˜Ó³Áß«ãu¹ÎæœˆP¿¦öÚ_ß¯r;CÖÕßÎ®/Ş¾;;¼şåäğèmì*GàS7Ç‹áBğDg¶	Ú«õ'6¿è?šˆ`çÿ%c’É£\…à%}{‰òôëö«C½¡Ç¸H"‡%ê-\¼†»Ìë-y\óÚ°Ÿ‘Â0›'†¾%”q²å-#Û)g-$‚ƒ0¤Ào§›º˜:~‹=*úb>Ú%¶uxù€3Ån„T?xºøûHvø‘Ú¡¿–°¾O7UcŒ¤Ä£CÊQ³´Hî´¨G\óó=ãõV4«qkƒòÙ¦>¨öĞ¨IY´)c«ğîÜ6+ÚêM˜n[—šCÌ¾7Úò†ŠËN–3mo^'½4q¯ù®Ù²§¬GÄ R¯9³ÛàËŠËù"mÒ"_wÊ.£,U’êÀï°[®²Û0ÛÂHÓ‡ÎÆ4x¶ÃSÍirìCµ+ìo“Ïga…–ß3şD}ş,'ûÅz‘­şÏ®bIiÈÈgü¥X ARy}‰ÄÀãOëéB]Mı&B¸SN>§	¾øµ˜­ï ²ıJ*ˆ‘}”ÏA¶üÕ€t
ÿğğEşh.±>^ÑxÆ‹ šu1ªÍÔÃJ3¶?½Ÿº¾¨§IÊzuW•|Ùz½e©'œ¨•ly‹ú”bÌó÷W¥qI«™ìNëÑ`ç½MspTC§¯¢~Ã£n¸´b;	ÏŒ¨1d+ÚW ±™¿’Ï/ĞBšĞáÁWşAî€vpèĞ:µ:Ù¤{GË,.6KD!rºR qËæ”áKhzr|½Yûw½Yë}±ö`÷j¶ÿK¼­£÷mUPg3e
w2L•¦Tv>–ôìhn˜ô+Ò«S‘ëx†"K‰Iaš{„HyN—Ÿ€°VT9/>åó!ˆH>ú¯.öÎ.Ï÷.Î®¢o	âç>f®ûWnĞ
JJ¢íßïèûJÏ2ì÷*I¾êmgM³¤zl6­ÉæK˜IU¸‡kşÌo‹×ïAFúğúcgİ1ñÆ}[—_íé_íéÍóyìéÛ³!9­é~Ær%–Œûßİ”şş_ü'&æ÷ÓĞS…q€ÉœMUvˆ‰	ÿªƒ}Áöà-Ù‚õ-¼â3Øeó¢'Š+[V|¥iî¤+nªğÅ/@Ü6^`ÉG´ £wXÍˆÛ[Våloy,ã2Ïßïg„Öê#{¯û¹íŠé_‡u	²¶Ë|ê¨ı’Z'Ör¸©¤¤¤U«ıé^'ä	mZR|ô0xÉpüƒ,]–ÆE¿Ëj¬î¦…†Ä{şÁÓG˜&?‡ñ‘ïÑî"¯7s;…-Ï Ç~†¶ñqÚNÃ(9ş¬V–ãuPCíëŞC5Xk‹å#¾õ`yğ÷”^*ñoS‰6$"Ü®Y=4æ½¤U\·KMâcTß*™ÿuoç"_]4´Ê Qü÷{úñ6œñ C>úm|";ôÎ¼ˆ&E½¶*_Ô¢18â¨ä HÁQ®™k!gà1 ³6ÀP^m9˜¯<–mØ!ÀîcYT$D‹%	ÿxG9AğÚøïìs„²ßÔ†%h‹ùš	Ï®¢ŠeÈ)Éä‘kQ,”m¥w8.t—&²0U•DÊe¸c¯Cÿ@[;%vÕh[VÂ¦fˆAğï˜ò8G–à'.ÈGúåÙáñVï–³’«­vMĞM„‹|"a6æÈvö•LKådÊ44`i2¢U×ë2«×ˆôj*+ü×b>GQãñhğâ;OÓTòm2-À˜b±E!AMR–› ]ˆÖdËÅ^a
cÊ+±·¬?æ‰W?ˆ~-ÂÑ„²Ä]VÜ±å±ƒûP§˜`FcùoüY&ŸV
à…i«j¦Dã?v¡Ä"r(yã)v.FDUG¢oN‹ Tjä7eUÁÁıÏŒoÔ—¢éVç<0ƒ~Âı!-Íğ„hŠs†‘{a¼Ê+@ğyÍbzœç3¼dÑÿdJ'lJ6û=úÛşÛ½‹Ãë«½óÑÀõÖñ›½W×'¯ÎŞ^]¿zóvïÍõåÑÕÕÉÙ«¥Hú}šæt>(n—eNUx=4ó3© ôø±¬Ş×G'ÒbÉ0t3§ÇŒ»fïs×	c>ˆàÊŠ^–óMsSE˜.Br‹ú æÔuCŞ[™MW5€‡B{¾¬·I£ğ}ZçËäÛc²[GÜ¹Yùvy2›çC¢2}£wG}KA<RJY|ÉLİI(#6ô\ š@›)$	ÓÄƒgÂåé\ãu£r´J0?@D§´ñ~éQ(ífcoMõR¿r1´ }y5ftD‚'"™5kğBÆw‘.KõÉòÀı6óRÎĞrF‰ä­e½Å¡O••ÛÏ#šªÑ»â%)I«Ş¢Cª\r¾†î}1²•nešİp|òéî0 —lb»)dö7¿?æô%¦hœ)ij¢øTPG"›ª³˜:i½ÊnkŞQŠWµ3½ƒ ‡€`È„¾›âV\áÎkèÂÕ‹äckH‡ÌYĞÜí&ß” Êß ëBßüHaŠ‚,Óreëå(›Şñô~êÅØˆß–‘µÈõ¸#÷."}-ö8ù†KVÕ_Oboj¹¤O´hù2<ÈfålBUB#ö¶¥e¹Çä®øºÚ>Ã>ø=-å“É+Öš\Ë2«I§CÕxÙH#}ä¥cíS¶4Y]¤MkGYeM5.K™¾<óOŒhı+/Âì˜„²„‘&j
Ì¼]BWşj>#8Mò_I„ñ½‰êêZOpÏ•‰MK“ş¬/_šYB“òH“„kQµ¢p2û/¬ôEZPÒ| ›tÜD†ªÉÖkv®œgë;¥®öË¨
osVÀçİmŠ ²ô|¢W>©WàeËÛPó=úßlo{à6Õ²HK«/n¿wÍoNyX@¨kmj6‹•9ûdSÓdRiğ.mºB —¬Ş÷y oC™ÌİI‹/§;9–ºÄÂHXìã!Š IŒÛ¿Dêy1Ígø­ˆAõ¾e‹n«d²ûv”ã™!?¦UpË× +¯ÄâeËY¹/ÁQ—Éµ©45ã˜ÍfL†6—°2Î›	ˆ83OÏG¡‰‰ál$	w±œ°‰&‰“ÒÇ$|wçí$lïÉãñ’Û½j“â0íğ8‡€#DÈ±ÚJ4+Á¬ £*AûHíPDÛæuœgjpÿ¥­<6ÓÍ¼Ì1.ÔDÜÌº™osêD¬;bfıbª¶uwÖ)bR}Ì5àX1¼ø–|üŠÁ!‘ØÑ} £›Ê’s5(ÓÑ°†ğĞl rJÅ“WúD{UöÁæ™¨?Róteívm	²`ÅuÚz=¿®ói¹œÕ­ğ¨¿‚Ò ™ÛÁªv˜ùA¨‚òG,ëÜãÿ™J›¹<xZ“”¾ÛÆ9~¹3Qˆ[ÛUÊPH Òq·1ı]ˆVvJ¹ÜÃù¶Ïóå-SÔ^c2U~“Z$6Mê­&ui•Ô9å«h{¹ğp §Ÿ·dAAËğÈªü‡ÛgG±`©Ån°Ğ©³eÑ¥eusµgÜ·:[Öz dâƒãØ=X^NYkçNs¨My aMØv_lTô“%?ybk\¤ËŸÏ©ı¤™3¨?òE‚‘œ Ï1w‹ğGPÒ*û¤AÓ²ü‰*ÓŞ©9J*—n‘ÖqËDåiø Á%Æ|ÆÙRñ6Òâ?Î§@Ç”CöªìÚ¬0ö¨”}Ñ·Ó·Ívú6¸’É…Ä26l¡™å-•äï’¿Û>ÉÇxBX4ç¤™&º¯ÜŞ¤{Ñ³½Øü+d–Sğ0b„n÷#%»½u¤P·úì¨æˆUâETn5o$-;Š…]Û§PÒ+K%c£‡×ã¾ÅƒÄ¾ëÄbÔe§T«ÂÁH­Ëv‰Õx˜å
µbpE£¸Çœ ò»l~ÃQöIxít»¾İI-/lÆÎ‹ÕÂà«ôşq0şî&ÿ®#’7¸ÀX-ÿéÆ>qÜ)½?É±5ky<ø&øq<P8<qH{uáÊÒõuòë ê›ËãA>SCmYÍÌ„ÛØÑ¢ü­0ƒˆKH§Ğı¬šf²<ÕY#úÔÃàé¨‹³“e»³N¦´U{Šn†u•Y¢d–íúTùjMfÂ‡ÒåçrñŠÛD¢İFÕ%[°¾İM&°Õ(Q”n§tåçKF} Jº¤+Ï}‚gÑÙyB«6~`‹ŞYR×­g~¸zğfı¢¶¬âçM¹viÓ
q”¾£Yab•¥*æ’$QËy•@®ÊQğPe¯qF4=e#½GüZ\j?Q?ş0îEĞúQŒZ7y£©NƒÄÄŠ9„Üù|v-ó¹ïšÈ#i¥çNr>šcòÌè4»ùY¡§ñæ™Şñ»E^İæ‡«MJØ×¸ÒœIL'â€s…8èu=cu.ºjô|Ô6LKÛ©ş•Kå«ú2_C<³pî·FG$`-ÖóÂÇ­hÄ‡F•€:Xu"Ñ<ü†8càüß‚Á:‡oäkğ_SşÖ€WÛ¯Ç¼_J}¿]œÿšWMÆsqñxÍ~™­qƒÊø°˜İ1xP®Ùİ@êyç=G^ÓáŒ†TSÒÓééõê^¾¨ã€f•Zö6½2ÏåŒ«w»F‹]™Ï°]f
`;ùà™·AÈyz Ö î&'ŸÃ¤ëçoÖ’ê5Iƒº}‹nôC6ßä”Æ†ş© ÿ[p´ikn(Ÿåß²ÃNQ˜Kj’ĞiÀf¡»e1b_Ø@à­uö"Ú)+›Í.ÌÁê0A“ÇŸ?ˆàm·Â ?fœÇeuÁ·…ôz‘Ná3½FÚ/˜Vv›¡¯ò{öÍÉò¦TÏ Á,ŸÊßÆóbù>Ÿ]ÃßŞ‰ÒÄ¯&­î# u’^ç˜ÁÁˆÛ‰Œd€â¼`İo¸Ìg;/DîËA13ù‹ç`0$«‚¸¡xuÔFQÔÕÎë¬¾;ÍV’èÑÀ¨Vx\jùô9k¨!§)úp DàÚƒ©³OT0õ÷"˜ø“¯&é¤l”ş“£´œÎ|‰!wèkÌêŸeCƒSÎÅ
îÂòı—Ã•ºğiA^(c”#ôº9Ï¨¢o0±Ö+¨²ğ)ˆğMÓzr2cŠ
¸*ä‚Gõ?\ÒÍÜ•ù—•ëÀ\îê9QGêBhWEŠÁO®)Î1=h~ğDãÊïo§ƒ³Ût~Ë™v†ÖŒ³Ëmš^İf<¦ó¬®_màkO
$ºÇ” Æ¸K¢Ä¥ÃüBùC‚ï…ñiÄkÄ’Î1Íï‡ 3îì5qÉÇqm"KææF¨†cğ0#2pÍì˜,ïÒÛ4·Pdö&¶)Øh8H'‰_E'¬pµ*~ˆ¥¹+ËƒOc¹‘7q#İÊ‡RµL!7NxÄPwÉÿÏ,ASs=ÛJ$~£Y´³ÚäAVjúYkQ¨W±mZUß·¢¾ü°qWÀràvpË¶*ÊÅ„r/)$MOaÉf¡i)q8ø–pÀ´¼ˆ¡°ŠÊ‘„Åº€ªLqÍ7eÔ´İâ-<é{O+"ôğ‹ŠÁ¤,ŸıôıJ#BbÂ«ÄZ\Ñ%û-
×‚xú´ÏXÄ.w-ÜdGÁÖX…"5;D(ÀCoİ„Œg›(2¾Ëê_ ©½½KŠÜê:Çì!,ÓPH0E~‡a«Ÿ·AïÀ­Dé$=ÆI>±±NáHÓ/ ÌÄæÁ`ÉOiXÕ.åö6ŞĞWiæk0" ì‡BÌÛÃ£ëó“³³£CT#>0¥×N7Z\F1Ï‚›»áõÍ•! ìš­Ïç›*›ÜçGK}„ÀĞÄşÈf÷À@óºi\—ÕzhrìãmÏ™àó/LËìĞ^¬²*¿*Ù÷¾6-İØtÜê]1Ëµn"Êj ƒQÔúïæˆÕ–'§4*Ãê!×½X«0´x#­ôáUUNÙxå2İbÖé~/3Y¢åÃ|¯„ƒÖeøFã'ÅàPª»ÍpbbÄll+Ë…|H+KÅÁËéü'©·§«cšN¥W6’¤÷RYIt#¤äwÙí_Ö
OG/æĞK·©w³§‹Mz®7GG³ÊÇ§exŠyôRÉ§Ošs–z\ q_s¹VÔ¡KN›÷§H›§4õk>YQw¦Nä7Çecô[Ô}JğÒ4d¸0ãT.‰İ~¡nvÖ×A·)$,6àiÓ_MW ê·3*AjoÓd`P''±Õxnl˜¤ºÇùr6qÎä÷QÕğd.ÒbB=&ì+Ò²”8?	sÇŸ(ı2Ş‰ÙğÍ¸éÆgU„Ğ(¼5¡\×ô7=á+?õ†¸<Z;ƒâßİ¦Š[:ãğRÂ1R›±ßG\)Lâ‚ˆß8,H0@R™’X€³>îÜğx\º!_¯u 9â&	örëPÆ¢ó4„Fóì¤#=ÛT¦ı¿³ø.e8xló¯Í—zßŸİJUz\ê|-ùŸ>Œ1f?ÕiNëQšœ±ÅJşº"¿®È(ª!ÁM»™^%‰õ’šs7µ•ø‹˜V
\l}ˆö=İMI2.
@”|ÖqŸğmtüÇmXDĞ‡Ç+,¾§ÈÃŞ»£Ş/Û4bœàjğbwyÚ%¬/¿™{	&¼%GêİBƒæûƒ#®T¬B^]&\!R>/„Æñ¨›ià
¢Ôaùqig¿ê{ja–Èb&-õ5o¡æÁÿ)ÓY	Ì?ÿ‚ÛñÕĞßKg¡ıwWWoÏ®OÎ®NŞ¬!öG ·TQL¹6»5yı¤¯øÏÖ;)Ñ›İLLævSD$]Â‘&=h‹ÃŒø›ÜHLÊgÔã}NkıQÅµïñûbØ–Óê~Å“{½ÙĞOšƒ+E¶T¯k08 .„ ¨9|ã¿ça»õ†-XŞZÇË=¤xSçÕµU#<-îZ­ÃnK!H¬4n«•°‰í¡2bP=iÙ1ÀéQô5¬æj™yE}¼"íş(†šl<êî¾k?Y²1ƒœJtÍa–®)TşØ•È¼N˜Év¶JT÷¡“
é#—¸é"xA;ŠÌÇÕƒŒÂˆ}x¼YWğ=GÿÄ5ÑıáãØSÚ[	Ò| ‡b@½£éíJ/íË©y¥ÿm/z}´
·…‰ü42 9?ÆÀüâçM1}/¢Ä‚27“!ÏgÕv-~V­İÓ‰„h‡öÔË	cÚ¥Hı•+«ı	›ìš- Xè>8óV»Ç¤‰‹­ô¹k‚§©Oˆı¬Á¬wÍï'eVÍR‘æËuYf‚·Z¬ÏòZ "ë%üÔj´`Mï›¬YlôMïãæ%‘_.åÖ'*u‹$m¡k{(ûÀZÚŸ—Ó÷n-lÒÌ	•ËÁ2G‰V¢\°0¸r¿+OŞ÷Úœã^<!ˆC#¶Ş£7ÔìÒÖ²³´ê‰0ƒÃİ¯b÷i‘˜ŸØ’íhµè¹Ì³jzwÁÓKï}ÈŠy6™ÓàÖªâb(:ÏşÛ}Å[x·jÕq¼“°¶{×XÊkŞß6‹ÕUÙaŒhH*’RmT«Aí‹ÿ™øÎşê{*¿(„F”SuZ6^7±éxáqXø\pªhŒÀ®²} ó€ªÛ¦ù1Ì¬Z;èÇêÙVœój°¬Ï[Wˆp­ôÎi÷C;-;±š»HÈÅø1k“&èV;Pn²ì~„lÒ(U+†&A"N6î3mOÑ¥ˆÛÅ]Wù1¹á¼)ˆüM¾Ér·“>˜ÑTÑ•KÎQŞpO2~ïíÎ­Ø.6çîôÙ\Ò7aúì0Æ=%Ğv8@1Cû&œ—wåG>çUy[1Š"[Ê†¥ùWiW!Ñi6£	I›Û(lS”ò.q÷—,Q18ÑD+Ã:»åıDŸ*¼iÉkşª¦½KÿäáŸ|y´wqğÚÕ'º±1Xî'·§¸#6óv„-¯î¸I†ioM®Íü¼É«{4§¸¹KÌÌÓLÍ±Ë›(Ùeéá›´œ•hD,6åø'ì.4eaY7‚Vó¾CGŒl…íßLOKhÈ”$} _š2+l(/›¶Å!”0ºêŒ”8Œ·Ò‚?]@”›§I¤7fî%hÖø­ó×Ö¢qš"Z*+ ;?´tø®	¿¸øI_~?˜ë„Ş§ZU«çQb~¥n wÁ­x}DSÇËuHë’i d Sã…ßç÷ 42pÂÊoB¤àõ&ÉKFĞV»ÁVî/àcØô"¶UÙò$
?G¾İ4÷£‰€«y| ˜©ë‰‹Ñ†'Ó¢‰[Ñ°Ûî”¬…‘£r#6æïl`nÚs%rl‚‹Jo­S¤ó–¢œuJã·ë,–‡<]Ò¥µ£‰ÉİæQ6Ü4‡—ÒŒŸV4óFùÂÓ1ÌÈ¸em™$E0oÊÑöã7D0Oçƒ¢Xs.ëuf)s±|€ğûÙô}+y‘G9åÍªŞ~HLö"„—ë87)ÊõO-î¶Uv´—kF&\Ç6È ±a’ûYËZ7òC¸¥$°¦ºpù-jæ;#ù¼-uõ]í.äi½"ÙTX4Pë/s&î¢:G—›ú2[äZ»ÒGAÑ®ã4oSí5óNNû›xù@˜š·Û/b8‚¹†Ã~ƒğD$µÙ'ø*+ı&…b+ÿßF¯	-Š?ö—¦Û
Údf;º‚ëÍ8®t.oË:åo×«u4Å›Z	½¦lv½È>ÁvŒ­©-¡Ö”T	ùsá6áÖQ‡>VÉÙÑXDŠ¤1±DÀ˜ZÉ¨G`²ë‰“ã—İàÉ®µxã(±ÆZMSv|]ó
¼ô)?Ù#@Ç“»£7(ÀœÓ˜Æ"úlqĞu*ØÒß›Ï‡ÏFÎ‘‹¤ğã]1ÏcÖ¶cf Ïãç™‘ìº|ú=&Ò˜}"+~IòôØÑ&ÏƒÔãà­‡ûjœĞ»òÕ8±ã<$¤n µ­3Â®ánòX¦|ïVÆèüÁæ³%îÖÃè£Xa¬AÓ¯”D‚ZUà éå®–Ñ.hÒ¶q¿Ã–v{	´åE­4…À³Ğ[ÂÃŞÖ6xtŞÓe½=$Rhº•Ìƒv‘¸’u®±ì-ÎÁ—Œ`U§ˆb©§KÛ–„›Ás£–ãäAÑ°{±–úßÉ3‚CôÍ[jŒÃt§›×£|bB[~ø¡ÛÅÙâg"ïT¾éIm,•]¦o- ”Z7?ˆ·şg}åo›ÇHo"Æõe•ç•L¸ıxIY IPEÖÂrÅêwVÙ¦Î)ó˜ØËj$Lß3€y¼ÌnHô¨>HŒyVÙÍZ(mq*¨Îe+¶ °ÁÓì~Bzt%®&Ä¥ó„@?|~,V0=UVUúu¢|„D´R‚DæªzAÅ$x-D/tª“ˆ&3Æh‘}²ñğğƒ—èeı
ÜUD y%V\ø	@#ºÕ6Ì»Ç“S¦(ÜGüÖÓ$¦‚áS©·]Š´‹ }MxSnÃãIĞî—B‘ª+	ƒ¯Ø`([BøÂİ‹œQôGÜxRø®„(n£Y0ömGê5ëÎ®AHÒˆšI1"/ÌdVzÜµn%5…BÖğ§·ˆän†;,¤$´ŠuÃöYrNQ¥¬ÍLç~ b’óíËÓ|¹ÁSÔwî?ö«{¥Zf”kÁz‡Foa¨w°=ëÑ—½0²3FĞ‚ha˜¢Õ`%fE‹ç‡x.(ic ¡Ì1òa§”Å¥èWUƒâñ¦wï[Oà|Å›|¹ºËùÑ‚&cÔo«_ÙŒ®²UNÂMwS¦Cw±}uqQÜŞ­×°¾ÿWÅM = ´ãÁ2¸Ì—³ó¬h,˜ê¡ŠCüE*/ğRR @ºğ\2Î©ÇãI6‡ï› b¿])¥föÛ>¯<>bÏä„ºuTtÎ]ùqo3+Ê¶˜öê¶°À*!¶ÄxuW.óë)û­¾Îd¹¿ÛR J¤©Eë‰·¦Pˆ.A]TY
ÄŒ‚úà) ĞîÀªh<­r4v0İ<ÒşV®òåÚ£óCØU±È›ÔÔĞÌ¼)òîš_N.OößEßˆ «½ùê.>»‰ÍÅ„ÉœmªK?¿AÜ—ÃM…„Ÿÿé~@I«r­Ëjx°™ÓıüLrûıøhïòèúä`—sàkßCğgÌØ…mşöìÔÚ8jCØÙÙ-ær¼İoÛw¹™Àç!¬ö0Ÿw4ûçC1ËË”ıã¡«­k;@J3  a7³ı°hG“ÆúÚ‹±D-ÕWoÏ|ë´»…ï=ŒšÜÏ*[¯İ·Œ;‘¶ÈdwÇcY`?·¾v½¨o¹Ç‡ÃŞ°}˜¦à„ax4Ãx#¬ñy Å'îÁEJŒà$¿y•øn<ÙÔìıº¾.`hBË“nÖ¦8Ü‡ŠÇkÈä5rµ;åõ´*VĞSÇ »›¾dâí{6¥®ºkş{”ğ!€slºò¦˜çB¾Ó}œ¯ÙÆ©BFY4~k¯äóù˜|İ”KOƒãø·º¨1šÕún–İïÕlğë–rAõ¾øùèæ3úŸ²ÙòÆ²çHFtNU¨¹'ñDgßÌ!¨µÄnßTÀV úú2_ŸåÅ&_QD§G”iaB%dº:Ü’.É`ñ•ˆ‡º^ºmç&ÒkÀš“ÆK63)n-L'˜òn§Æn“r($òğZĞßïÕ(O]”¯²
$ŞØ¦õRÉ€n‡A=FL÷Îy²wil£•Z¿l)–°w™D·ÃøéØĞÿ‹çÙºù–ıG‡ıGy‹1ˆ8Z+Ÿõjø—¼½†€ñMQÕk^à¤>Ìo²Í|İÕjoGAùzÌ1åy»‡GÇ{o®®¯^]ï]]Ÿ×páÑº¼Ëª|ÆÎå›œñÙi^nó¹1´Æ†Wór’ÍOÙá*ïÈÍu"°YeMh…à&Úá#ì×]©7ÎJ'ÚÕú!§×VºäúÂ\Xj¢ŒÛ›aFDbÛ¢lŞÈqıX»TïÍuİ­²Ô´ $“zà±.ï b¹X¯÷Î^J»3¸±[æÁ‡í²p{G ğn³!Ú%¯ø­PŠ`ÍªjYl6Ëz­¾z–-ò!éÃQy˜İŸÉµD5¡ª|HômåV„Oãc£¼,@i¿,¯jœ˜ó‹âSÊíÌw”7’“îıù’îİ–xX
-„>‘ês8{Ş.ñWñ&©IS8s=Í–ŒMzOl	/w^ÖõvÉúlD ái5±Ãb9;†C¨z ßË¢Ô¹Î*»Ÿ2şÜœä¯ËùŒQtÇÿaÍñÆc] €váÿñ¸¬„‹OÓ¶»)QP²‹jßŞÜ0ŞIªêYÍdïÙë*z*ğ6Æ`t”oñÛüÕ¬àœ‡¼É7]B¼ùôùvµ€©z;òößå÷Kˆ	>Å¬òÊ‰Ã†‡î¸,0q@ç©¸âvTrÔíÚŸJêªûí¾u‘u~™/ë²ò8
^Â4éx½qZJÔß$õÈk¶^7š€KÉDâp Îş«M|wİøQ¨¯Á¢UŒhˆÉ·‚Ò=øĞ§(¯ø‹F`¦È$9/yi×Dùqğ}÷áœ†'-O—ô;]Sï„‘(G™r¶‹ÀŒpÆ}ËjvU–—wee-wvŠ%â›Œ›5şİÄvht]pEÂÄN¶Å7<F.@X¿”Å4]H±zdª'ÏM¿Æ´aÄ«
bìQºİ“ÈÊYêÅYYW¬¶û@â„‘à_ù@à.aÙwÅ|Æ—)±´(dÎ|Ø‡)Tìj0–tgÈKˆ$Bk›¬÷ßïG_Íõ¦ğ`¢¶ËûE×Ì¥ƒÃrºA<•™ü°‹µ¡Äš	4‡glß—@­-‘½¢C¡c¶ˆ`yŠ"ëu6½;«!àñ8ÿû†í¡\rş¡‡Šó¹®{•Ûú¡¶ËÇjğÎÚÌÛK!‡¤é'.M7U¹8 Ï/xâ5h§½3Nl'à kõÆáÜçqÒš¬ n2¦Ìj^GËyÇh„fJuä­ß|Cµ÷1Ã„ ûû_Ã'wÄ¡º<ˆàº/Ôr7
õsé*( Ä5oÁøG>x}tøîw4,¨
s9œfŸ İ!ü­Ñÿä	Ó¢Å”mûbš‹›5W˜Œufô²‡u;!ÒO‡Î'CÏS!N´ïI€3#ø½æĞÊVp`D/4,y6¹º/#î@œ^ê"ƒsˆ¶®ÑĞ{ûQg}C24"+úŒæråÌq¨B`'ÀÜL7˜–7lKø4Q1S¼œ÷AÅ²¨ïÚ÷íÜ³ü2Ÿß³sC µmw*)£eÍ .pâ/oË¶O8•L	+ãúûå]íàTØ›ÕèÏaVqwZNì"öxê	,ş–ƒÊ6Sên~mHÅ>Jßy°Dáš§CÂ7>«Cİ+¼@	nz˜Ç[)æ~+…¬swğTÏ×Üà'BŠPÇ%mVl…W†'aà³àĞ-Ú'†ö¶\ˆªú^¤B·êwK8öÏ¿”k!Vå•CY³ÌBÛğ¶V3CöP…¬4r–Ë’ÉX›…•ÕĞœpÔ‚PIwHNÙJÊ©L:el""%eĞ¸3µ‡Ä
c±û½ñ–TŒ‹?	G4Î¡éf¡²$F¸i¨¾è#€Uå¸PPÄ5©NDV¢ä<{\ñåC…®)»_¤Øc[Áxºø¶ÙŠ{<Ñ¸!0ÒVí¨Í€Ûğ7ßDe-ñw—+,«yv/Ùr£8EÇ}óÍ¿˜òÌ›Ñ¨“rŠAädú,±®mî0Åº¸7L¡9w³F/Ó˜MÌŞä
ë¹A›;&¥óoÙ¯Ì¿}*9(_™ÿ4ó§ç9‚ù“û2ÿf+n“ùoõÃÓñ?0Û§§ÑÇö]¼Û7g­7Û¿0hÛÛ­âWÈ“\ÖL™Ú¿‡øQÊ¸¦YmUæı‚Ì³5új~	Î=®k¶ºa©±«*_›Åµú–çÂfš-ÑÁ‰¬…ñ	îëùŠ×àú¦ãtI< ¤Õ‘¾«r­å8Â4èr#\Z`—á û­³ÊŠ¢p­]ãuqép¹™`´C„…E·İ]t÷<ãîòü—¨>â(‹«M<}1½Â!æÈåDKµ9ù÷Ëµvğã:ö{T`NS¢Ïr¢P)  vrĞ‘#>–às¯‘åU•-ë9¸mDq»	R,¾öôƒşÊ²ñ†~íÏğX»n£7¥0Œ•‘'fÈliğÔÙ‡üUÎ&(›ót‘ÔÌ´¢˜’_rPºğRÂª#p2)	Î{Ù1xò¤èuÇíéøî2<âÈ6.«áñãå8.1ÜÕéÓîouôFhÔbÙŠ½UH LdC
÷ò¾«H!ÔBçD@)£®N4“ O×1¬×’Í s§a[×O{jãå¦Po2L—GBS"Lˆc ‰U#KàË$JN`l4¿Ééâ “·‹	Ş¿uË˜à¸¸©rçñ”À5O‚eÇ¶á×¿q~ıã×¼Kï~òä·tÀX¬ËÜàF0,¿¥ ¢“;¯2­_;35ùÌ-ôzŸ4hÚ|‚'‡>•©*-<	Ü¿R`¤Òx¤^¨å.vŞK©FªÓ~ 3Œ]“¢û†º‘pPxiO81‚$ùıN“Î„©GK'òOš ¡ö¾F1ûw¶­=è€-”rˆf},ÇæïâÉ—î.úöÑê5b¨¸¬—;Ï&0âïEÍ^{b|Íôå;Œÿ9Y‚´¬|N!üKå£ö¤vîEe'Pt+ˆ<â6ÄpH²5pDœÿZÌçSšYô]Ağ$Å¿Åg£ ÏÚäE×ƒ)%à±Á»ñ³{¡ıÓ_àÑ{ë&ÊŒÖTP4ÔCë¿'
ßtŞzK±VñM4÷Şî„Àh°ª@¤y”=¼„ï©ÂPåc™4¦ß”DÍˆ¸üU»Ô ¬Èì¾4’Ñ<‘õ:ô¡ÆÈPè­}9Æ(¸ZS›ÕOnô[`ôé°òŠ¤í¥Ş,Yu/œ1Çó|y»¾¼<ÇËã‰Şao´”B«1Öió©>>Ê5š<÷EOë¤Ş³DÏè;š˜Efò5fÃpôv·0ÁŸgZ\å˜TA9Œzù™oÍÛÙüÚÇôŠåš§°–Ë‰Büw¹EØ*3-—Ë@Üƒ7£Ä3>"Æ;j€*/²@´a-¯`T%!ns7âÓ¼QcWzñq­ìğN¼%3oÈøß'qÚz{·xû|Ši¾f™à¥®˜»-ñØ Ê³[ ‡¿jâUè9:0œ…¯-¯ñ0 ¿”]>fõÑ§UhòúÉÓPÑ¼B¨ÕÄyU¨–ˆqÒh‚ASz¥½9Â†º¨ÄX¼È-K„¦Šå«üïL±‘31¥øŞ…íµÚ¤scJgä]¬;%5£?ÿ–¬²¿¿e¥©®»ÓğîûDœe˜°;Öb˜'"Ö9ÖPC;(¦&¸²~¸+?¶_â;Fñ¸#€õ–£®³íİâ¡ğ²&­É¨]ª¯#¥òui§¯Î'›Ã.Àº¨Uàh8şÚ_^e^Ó¯^…!íË‘ ‡öã]&Gó?©ˆéƒ>Tjè/G
­pj¹Ï.÷~9:ä9ê“¥DXásÎãAÁÏ}wSÜŠp±y!¼ß¤n#)W4¼’îÒQyuÍëÚU“Ú¶?b4m7ı}SLß_ğLäŠÌ¯GÎ³ùøYyo°ëÌƒ¨†îóÔ˜Jê•I‰èàjU‘é0¢ÀĞ
ëoÌÙ>|+	€F¯ªŒ‡i'òÓF€¿X0q«™CÒpF½¡´¹àÖoŠå{—& ZÔW÷ª[	’‰µ@a¯5˜Ùsöò#‚û‘•yxÏÕ›kíÎÇ²4¿ÚZ¾@3Œ–}µ7‘ó7Şğ>I’ÃÜ‚Í…ğ…•êiP„>Äb<Y{ì6á¨}</ËÙ¯LÑw¥ÆlõbÉ6à¹‘1#^hC}‘±¹ğÛ4Âq¾Êûì
?âï‹…Ë÷ªk¢!Ğ)¶BÛngWĞü˜´+Ùùhõ­[UvgwÀ^ƒ…
!õì°{Ê‚ğºzı#ªA9p]€ ‘6AŒOğ6ÅlnĞæõ‹ÁÿÚıls
£íñ›½W×Ï_ø»&Öãk1It,•|¢îá\”şsw›„Â?ı…Rxø"ŠNë ^',sÿJÜ¤ì<¹[>}VMï\Æ·ÏŠfµ:ƒÊ›œuŒ¢03Æ«eõûpAmt.ó9[69GÉ±Ë~G´òÛf±º*•ÛL³Ğ÷fîA‰=	
 ½!Ğ:òĞ2P¹`.Ìöw†	RÚ¼i–Ô‡ÄS tmL5‘“/<?dÑ¥øÈ‰7UEZƒ¼åBqLDe°¢±‰ÑJæš%Rê’}"ı(‚*ƒ6»Qùè¨VŸ‹Ì$¬všäp“M×è Iµü£:>ŠsMÖ¦Ì^¿jòNñä<‘,^Wgš ŸX>Y.ïåÆA˜v	'ğ ,øË…3y€R1d£˜Á}6oh#w~Å”ı½rÓ5#Ÿ»[[0éN—XjYë7À`â½‡ÜŠr|z¹Kîú8Ïç^ÒdÅ©¸¹„¬‘M`áyŞ*úÏ„IÀù‚³æØûK=tÍx-vÅ‹<‘±Û
)ík¾,§KQÚ¯ˆ8Ä0ÀÃbvÀÑÉÉµ £_çó+º¯ååÔ`¡pi$¥TìúboÛ«(ÏBÓrR?k_µŞå1²WÙÄ‹Ğ¾Åv'H!X?x¯Œ
…ix.1ù¬z»„S…‰9'5$}A0q$WwE=`ÿË n~/ÒhÌ8ãN“|Xe7kkvÍS˜”²)³ì.™2Ó¬Ü?Õ¢X“~œœw°¡òèD)ü¼°ğ›cg²ãT\rH‰§‘ƒÄÏTñ6ˆQ;Q^2*eŠ25ÎÑt„coa|ôI;!Œ÷Àm=›æZ5+U)r«`2Öøí@#ZÂ@İ¿ÄÑºş50¸|%ÄS»a
ìt~¡!8P;îAH\Ê´-ÌË×g-ƒ5sRë-ùñ,¨!o×,-ô&Ó†>ğ<]¬´l>İ€³ÿæİ’ØÛÍµñ¡\‘Ä˜°¥ûS2µ ÒDƒ"5„ƒ¢îUUNÙ{g€e.œ_Øâñ2<9šuUÂIX³Ü^éÒçu4 rµñË(&Jpmr^B°ÓşıÕı
¬´—r Ín¼”—íÂKí’^š€î‰c;Ÿ•<ÎŠAOÀ›ª‹œ<w¿äEãöÜo%#döŠÊ­‘jC&½ŸŠ¢º™·u	%gK÷OÎfŒô(øİu…@!rÀ©Õ)ë(Ñ‚«×"OYÙPà²Ø¹ÁM§÷WÙ­†˜EÍï>#diĞÅËêòf"L,y¨ˆdã—Åb5ÏeÉáÅ¸Ê>òKÄéú'9¾¸ù¹`ú|ÿ—2q¡öf3Fıcw2µ÷;ÖáYàUã—ÙMS,şÄ«Ln‰|XÃG³b¾N˜¤ZµJF3·¨X4#òò¶½|„¢<.‡Ÿe,u‘×å¦šæçU	)*glÖ¿s¤’ûÑeˆûBSŸJşôOF0´K™à˜>²‡ª´ªnLµAøèÈäPÑŞœpĞ(í; ¯-A9Ö‘¦>Şó|ÀûI2/´ó‰Äadn­ıûÆ.ibˆ£-\’üŠç´²ù
’æCnyÿÛ®GPëã(ĞAX Æ,p@²¹HÎeÏ®œöp¡úÅ³#¦uK-Ó¥WÏ"·ê„m¾÷¾cÖ6ªyB‚µ)çè¢¾}Õ,d·˜i/¹FEäÖUYÓÎ®ÜÑäiÂœfë»1ã¡CÆ}?ˆ}‹°-“æİìÓpÒ¾«¤xóAIJj÷·%ar³'O§ıMÜÛ5Ô1‘mÂÈ‘*†ÓQÚÏÿ.×ìßÔş•Wşà•tŒìÒUáVY¦.ü5ÛÕViüvH±©Û–±>kpXøß-5¿w^‹İsÙÔ"±£?¼Oæ7-ßjkşL|oWBßü}S6UÜŒ¿òr«Íd^Lyš[¶^j<‘£>—voªAí	'©šií¨‚"úæó¢bºòAÊ@Ì´EÖ1÷Zü2ëñà‡Á£G-A®şCM;’ãıM)\Vª´1 sw³Ã4‹¹î hj1âr§Kµ	z‡6/·E3å²¶»)«EæmOÔ;³ò˜hšŸ˜„G}T@Ï%›)®=Ù¾)Ï³*[ÔÚÅ9ñû§/dö†¬\Çeõ1«€+ˆ
dÀ°øƒ«½ıë‹£ó7ûBÚ#sYMÒFæ Íï±¹ckê¸Èç3çµ&AÌÛåaQ/
p­× 9œÑµ{Ö¿e;²ÚmšfAœ¥Øh”†ÄÈ½`öïáÀn¬ïÊlš¨ï[$n%¼¦SXM×PèÔ8{ß°Ş~à.'Íğ5ç¨mQ|Â9œ™êÙ"KRÇ¯ì¯ŞìKb*]ÛÑ˜DhŒ6B‘«Ck7¸M‰ƒ£RTÜcje<7}¶ š™gCƒQ[e³¡şÙŒ6›	îr£Ö(¤hö‚}Ïí4X¿ÁXIN%ãüÁ#µ¿<åíx6|ä,†‚Öî£Á“ÁöÿGìÀ®7óuÍÚüßGO¬ r HËzşøÉ£á;¼
l:Ø
P6Á
8©Z“ğãJm„¬Ó$£k!†m¿«$ÕA`	˜b9jaLp2M1ÔKà6BW4Go‡öB;·ûÍ€0¶£û1¨#J¸ñtã˜Lí³BÇ~1¨ØxhX/F_:Q[Ê§ÇöcÒ|Šå,¤twüìÿ|	Å,¯ï=(­“€-ƒ…O)ÖåšIHWºMÒ‰q …VMÆ:ÒšÛ:Ît iRî¦òbÏ>^Æ6IÙNã©£l£‰ —h”äHUÂİe–¯ÀŠĞâŒJ#Ï©Ò1UéÜM´l»‰HÜ M¤ÂxÛ{uX¬züáXÔà!Ç·4¾=€q»oXü³ŠGn—”8½dÀp*ÒŞcn,p‰Nz?xè¥T¤8HQÕùQùà‰jè3®Á·è¼§æóœŒÉô|ÆÃ2
|+çgØ«/6ÛÖõ­9ØºQ‹£M6Ãd+–ÖûP´uT–#zq¬¹E£QÖ­ú~wHëIë}‘Õ·ƒ¨îÿÚ.‚zät7¥} Îƒ›àáñÉéõ·ÄP–ı!Âöiêî¹'€\£7¾Œê2‰3X~5ãt3ãô²²÷Ál¢[mÅ­HÆJ¢}D`oÊ”d3ĞN(³5Yö!Éügˆlÿó‘‡?´ìx‚äé„xsÖeea¡†şí5t‚° ò%Ä¹6ïå.ïkÀ8‘WÅ"?eBSQ£HMµ		=³\Õ°rk”;Œ˜HP“töë‘Ôÿâ®§Šªa$üiğlğÃàù÷ÏšPuüäÛÖ6?ìZ¡;äíÜãúmü)(×Èt­ñS,ÑİOH#ãÓ½ÿ}ıËŞ›wG£Á"û¤ÿtrÆú‹9qÿÆ¸şÛ †m@al+F¡´­r!¶.Û.%¶	¡g¡À­\À#UãØƒ_Œ6ê‰UR¬¹Æ·¿•t'•ŒJ´¬5-ñ2ª
L·AI0 ¯% sÒ~Üå­Q£AŞ ×½S0Ø	­½š1MÙÁuê‘ş|rd“l;Ú¨ş^S§jãğ<ıvCí±QWbb6skêµÅ±*ùxœµéüŸ–”Õ<:™¸•ÌoÂ§ƒ?£ªX´ûqğdğç¨Ê¿}uöO4yç7<½<¥]oé5‹YyûI
é<q/yå•æÚ{ıÑ™6&ÒÆ±Wú•Yè_!<»àÂ´jús\Vàñ®ø%ş   ÿÿì}{wGrïWõG® XÒf7¹¶)$!	Y>°dÅ7ñÁCrV ‹$1gıİoWõcúQı”6{=ÇÇ"fº«ßÕÕÕU¿"9\Ğ±»¹_¡1¿¹³Œö'oû—`8e ‘úè®ü¼„·£»l˜àÏ£¢bÙJØöò«xñ{¦eØ)üTp¤î¿eéYÛhçÉ=ø9ø6åÔOaè8¤AáŸ>:d|«Ê@ä…„?€_‘÷Œ@yHjGÛ3RÜˆğ$Ÿ€~‚Ü!§W¤l‚ÈÊ§İ‘oÚ¯ÓvÓ€)ø¨»|‘ Ë-h“H•.KÀ6W§^Ø}•R5dx“Ï\2¼5¬s°š«S/M¯L$½D	DÅ»sĞê5Z©¢RĞƒADÿÊµZŒC®×ºRöCBÏP.¼¾DÕ
Â%È»„µ*—U¹®ÅçaYmªã|ó9Ï—¦²IÙ{ÛYz³™TcÉ´ÃlÃª©¥}¿‹wfBs©]—›‘EPx1QÆNåÎDg’µ–3ã, Û©5û
€â‡RæÄs&@BQ©KµAÂ{œš7Ü%¬o 9³6ºJQ9–½Bh#çÛwd#N‹
ÔSp-ÀèpoU®íAn	]§Sy¼0"óÜßlGRõÊd?Õ1$l,M5€:ŸêÕ©©p·¦ğÙÔ BOM™bŒ™»å
 ú‚R®ïCfî²ŠÙ)ÃCf“k•_«b·ŸÜm6«êûï¾Ûü÷³~ø®ıßÿıùéaûõ÷ğ×ìéáák;î…;8Fgg6÷×L¬æÿQõèŠ¯î
í³<³ ‰î,ü‚!>Á2[ ¥È'#/Í(ŒGKÛØPöã>ù(*FáBì‚“ÉËÖk¥Ê]eë*g?ÚfM_¢³q ¿¶¦¡Õî™æa¬•¿d}ã?8Ê@­2¶½!û[ã-ìÊ@ÇIÀá@×²q„B5‰o|v5<ér~îMeÜÖs0bŒgJÔdÁœÕ§üL–Ì³¤–1sñz(2ÁÎ3ìG€ˆÓòt¾Ï3ù‰¦3?ëˆÜ±ÁÅÃ¤f¤¯¥Ï`ŞÎıö\ñë6ÏÎoŸ¸ Ë‚[º]MBXá›Gl³ßkø¬¯y‚ïİ=ÈFïcQ“,(ë‰}µš\‹w58‚ûÍa<ĞËr3ÜVLÒq„‚ĞfçÏÕEHåp¤·Tï½˜C«†PÁ„°òSqjTEhUƒGÉ LÌŠ´‰ãzÃÇáÆ÷ Ş9“ßîIà£‰{f™¸^|pdWÇÎ6µ	Õş'Ù|è}õİ8r¾şdn-ê
 ÒëË•‘›æÀÜßeÕú § €~ø©˜å%aAê‡7Ü,‘q·>OµEí"¸®™¼Ö;MÇÆZ-1+bgß-j¢Ğrô!*†À!
Øõºü˜/;­`
-ïYlÈ¬ı±ºB0~Áı<*¾ßÔyDLêrú©n/"ˆAú‰Pâa•ípw5;Ò&ƒĞÄğ¿õf±šT:İ¡†©£pªàw†Ó	åB³Öï´ÂfÎWÃ8Î»„>¨0ê*p µh>Û)Äé‹D9òw3amYÌªqy…kßiÜmP6ÆŒ&
»}_ø
.=ñ]å"ÊÖ9gÅ‚zĞNë¢uÔòõ ×’^Rso)åùÓ%….è„ªTˆ®aCC•Ñ³áÃ£5ñËcÂX­&wQn#?æ÷ØD:ø@¶Ô°èãT\Ù}|6é8`Û°Xªİ˜qDo§YqÑ>Ï&Àufê]Š(&IòĞ»øçÃ¾ÀÔ°ÜÍë(cO@LóÅ"§]‹zó
ë¥¦æÍ4é­E`Ô„Ûe_^EãéQë…ÿÌÙ…¾ğãÃoª¨1>Ÿ^0æ2&5-qXc§]?í=P|
–•àé0Œì%/Ã×©SÖEşs]3¸)0¸—ìß¸34;°€ÒÊÅËÈÃC÷B¥¦…+*Å.!¹P!~úUè½OšFoTU`¯l0‰`Rúƒ<µ”‡]„dŒu˜·‹ìË!eâ]¿¿•Ú¹cÄ¤Oö/;tK^ætdfìÒÖíV“Híª@JŞ$ÓYAÀÉŸ ÆlmbéÉË&í·×ÆI4:fÙ¤W×¦İÀ†øµÕÌøÇ¾˜ aËn¤a?aƒ£˜îŠÍh«±Òô¥c÷gBÑŠU^k¬Ò¦ïÜ=\ÖxgòñÒ„¿>l¼a¥õ]€!if™qeû[cp¥ZÛá=/o`ò&ÌWÈü* ÌØé¹½ÙµDş¸”k?ñé©lZ÷4w*RlÓ¤‰‰’üË>,x†:éRü<{.;¯N
–‚sPa2Å,ëÎ˜ÚàE°cÕ}Sş_—!‚×)6³ÎÕf¹D¸Yë™<¯aEag›æYşs'ğJ`?è‹œ‚æìW•¦?®7,œ]'¬ô‡åe„Wœ³;Øñ`³Ùbáè@)éM§æ£ÎcÒg$İy²ßtš|j|Cj—@»ÿñ×r}¯T³l¯?¯‹MŞ©Ïò¼üj"˜§ÅÍëvà—å¿Z´ŞØ*E•Zûˆ¼üFºg—oG“şEïø¬Ju•†c‰•oİñÚ·®ïëŠóÀopWÆ8"@ê/$r¡ôø½ş9@#H²)Wz§…"P&¨”º‡#:-M¡Òze×‡Ä“Gİæû%¸a‰´½›œ¾÷§a/:ş¦ ­I±¼.¿L¸üni_Cúû"Ä±šNêÙ2,-ŒòÍó´ü¼„È ?Ò
½ªË*_³åä\ê™+ˆ1}£{O„Á¦·û¶˜zÂM2mRçLZ,Êe•Je!ÒSäP{±Î?ò¯!'zpD×Ù§¬Ø¨ˆkaÁVq;š˜téÍ.«ü?%êÚó‘7Óñûñøòb2ì½íON/?\t<}ßiw;)ÊO¢¯<]åRp–,ÁŒÍ5^qÍˆ:ºNÈ6æğ4òÚv¿“Ê[Ùâ?Q=;£÷À{™'Æ;ªàÙ	î£DêSÕñÜ¹Û1yW=²ëxĞŒÇ¥Îúà‰¡Ñ’8ï_Œ—lAØıìíì†j„¯¢B2":ºd³6e#â¿lRRccPŸA€¬û]æ<¾?Íî‰üæ÷Q	¼ŞMEètœ4›;}{v+>°8w½€óÕ+nÿì™=›-öm]¡9¤ô—Óõ=Û¼ffìHwŠ²İºŞæ³_(ewtÙÀÌD».{—W xšR¤xºÎæüce2ÆœT+ªNkñ'o\³z˜UŞ¡±ÓdlÅ¯îfƒ/KİF™øİGëiB Ëº¶&Òu)8“÷X­¥ .÷•8æØø8&m0¯¸dÕæ·õáˆî–Öj]Ş®YkCe‹jéHÄg´øò§Ñ}eX½ëÔ¹üK³¥]¯5Q)ÚxJ•à(1×ös† ]¿†ÂğJ ö1õÙˆ­ŸYMÏ³{wB›Æ^ò¤),ÇFŸ‹ÍôNìGì°ù1¿¿.ÙIÅrÎú˜ß¯rv€o[×øÚ>iò,WäãHøğ¤İ*[äxİïVhì!öÿ±ï…1ó_·9;ƒ²ÎùwŒ7 u¯2s†#%|5Á?+^[u9ŞczB›¡yÆ˜< 63WÄ› V˜†	ÜœcÄl£Æ+	Kcm‘³Ä.§ †yöSŒAN?BA\ïõªõ‚4“Êª\æ@	_ı8jÅ(ƒ%±ôg­—‹£L"´ ,­¶q:4êzØ­mØëê…â-–Eu'	Ö!1øx»œÍaíà?\ÏÎß…<é88«²ÍU‡¿7&Bğò—C`Nÿ„TXËŸtZ}6GÌ¬<bo}SbUVâî5…sTû	,d^WƒD°*éÔaÑrêÏRÈ“o³Ùl\Á.PKu~ÏÄß¸®Àsf©æ=üŒ¹Ûæ•t=Œéj –êSEŠíÚo‹ÛLx`,Ù&²áşµ6k§¬1Vš™¶­hAÎí×‹\”Ã½Ú—ÛBû¢L<&œ1øIµ½eÂ¿j ‰''—?õ¯Æ“·ƒ·½·W—ï‡OĞºID0ú<;KXÉ±ü­äÃ=¼‡ë\è/*˜êï#Ê š±‰™
î’]1©	ÊGUiîLÀVü¿.
vºD´e …}1™òY9Ù°„OÜÍL§	IP hÕZíÓşñû·“áÕà§Ş¸?a}5b‡úÖëÖ‹—€õÇç­ßñÿ½üö¿õÓÖĞ­¸‰8ëe‡„JÀıÁâ!aˆ«£š8»V=†„Ÿİ‚¾P¢M1@rÀw0€Ö}L˜4ĞAró ‡FkÉ!¸Ô-
¯Ÿ¶|€ÿˆİÁtûæÃ$k"……!~BOïpkÄ¿6 æX§Ü£ÿ;ˆàkvtŒ¦dÂ÷O¦œ	ƒîê“ÍsCÏAAŒÇ¸„¦rX¬L·Ş&¬¬Û<ü˜‡úáæŠÓsÁ#ïSf%óqpôr·"oS»ı±Õe)OÜìAd-Ñà°À¹’,Ödã»½Ğ:Öü|2zrÒ‚Îf»Wí×$ÛGû˜<÷w›+#MæFZÇï4Nz'ı³FSÁÇ+¸iÉH	µ#S§EI5ú•æÑñ!h>àìÔN°wzäşU:%mw4…êpàSŸ.É‚¤0°‡Á”Ôr,¯ÿÒ‘zâV9Ÿˆ
°æ$Ü8## 5°Æ J×Yi·›–7š’ãç‡©=¦U³c:¢0ß„eÈòÓÚGÙ@§5hØ\è İ…‹ÂßÁ ÏhŠ©ÛÄ1") ³°Jã™2Ejf³¥ÃbfcÉ»ÀÎ9JQGRQ+Ô>×>«ÈZ¹	Ã89Ú6¤°]t„÷³ÒÕq3;™Äİ®`(vv´¢œ^€ i^XûZÅ«Ì^ñ1]¿ßÖ‘ÑÍÕk!²¼b1=ŠlFk_uØ½9J½Çï|w÷èw2±$”iÒje„…(a·:Î¦áÌ´œ®³Ï€»ã(†ü¬æ‹LÒm}ÎæóU¶Â‹ƒ&ÌÉòc]sSş¿v%ôÚÀ!Ez<¶×-,ĞóÂÇk*–ŞÙí«`Ú¾<*ÖK5ØŒVÊ7„vµÚmJ­2ŞÿŒË‹üK*OÖHéLş“1Œj=‘ğ'¸>Nd²É¢º½Ün¬¡UDô¦ñùík‘¯=­~8­«Õn;¤dyØışeà²H‹x€q˜)£=kö.¹rj»€FC¿jª˜Z(“õnjğ¨W0Š\6A`Ox	gkk€ÈÊRºî_\`G€²ÚèhàÏ¯Lõ*iÆ­n9xyÈHÊ{)´bßè×şf‰ˆ£ÂæXZåofsaÃGzÿôO-ö"ù	·|Ô—
oxKÿÚµÉ´hèPï-*˜ÎZƒW«Ød“PÆX€¦„t12Øglàx»}”~A†wâ«Åñ25éõW–—ŸB…	ôŞ]VÉÔXPewÄæéDÅDÍd4›¹™?Uaig7b!×å«ÄShªù*ß 4k¹­Ä(ğ$m¢S_·¦Ù|º€ZşæırSÌUu‚Î9€‚Cˆ0¡z)›£q)õEvç¨`°!ç>ÃPEı8[ÚÒBkloôóB¥Ğ¹¬<ÉÅˆĞÓ°‰|äœˆ±TÏáf±f¬¬&àÈˆo¬/Gwåv>ãor´5¨[›àu¤¥ŒÂµXT’áX-£`¢DQ+Jã¬ÿ%3üÒÂİ­\4eDşjF)•&ÖÒrÖVÆö‡l­‚B›®•T‚ˆn¯7ÅÆ•¯¬’åéõ!>àÁÓZÈé{{G4r#+ÄÙ¼áEQ€âªş+&3@7ãèr¤Mû£ÆƒÀnB•,Œx´ß/‚w÷©vÔ¸ï,NV„5[R³k¯Y^¢ËMùÎù‚>·¹Ggy¡uYÁÄª<[Oï‚\…o3 *Éøå«•Ôöæ1.*ğéØÎsî…-r{œ¸"¡ŞèCŠ„		Wm¹]·˜„/.¬®
;èIÙ€Bz9 nJ*8—u¦º1‹:8+—¶ÀŸ§0Ôi0ëÑû·oû£ñdøîrŒhêÉ9…øCïìlØö¯Èå•’„€{U-…ëq¬Q±(‘X°ªøoÍEÔwMÄ	1	[ûdœÙÔêm›¨NÉM2ofŸ–«{&ò°“Õ0[g‹Ê®ce¢É°Gn(ø¶Àõ‡ âq¿œÏ>hZ»|ÔUGúğı‚¶-óÏÄ&¤­á'6ÈÕeÇ¼ïQıü ¤¿æ)}Æ ôX†+|dĞÓuùFRg¤\B5ì­#>8‘«"ïWÍn/•·Å1¼+7¥fZÅ7#|‹šµj°<DG=2wW@¦“ĞI˜p'–š«B5Òù†êC:Æ†½Üm§Jû|•„üö\’Úò…
wÏ^D5ì¯¥h	×nåÙIBÖŸ_²ÉÙû©Š‚î0ä¹¾’Tê‚_‡q[Ì‘œ"ˆq QDö…½¸.¿\±àCêı`©^{¥d“8›eƒ
rÄ±ä\Ù%æƒ@Ãğ›Í¥;IXÂ‘AÊx°Çê,Æv‹,‚ŒÄh
eu[ê	«MÎÏ*ö'áíœ#“ÄÆs`G€5¬=QÒ!‡Ø{[ÜlDƒîtUUÓÌ'l>ï–sÌXf$g›ÙÊÏYÅnÉã1¹äJ;’+íä]ÿôı_m&mJ¥p&P1ş”ßÓ2Ú¡—ç¡"Œ(V¡Šug ıÇ£ˆ³uè\•Šää²E„m€HHsl·”š„ŒYMSTÂ©Óvæl9+İeş…Â{¶íÌpQBRûÍñÃœoÕà
BÍm’ESì ¿‚‹¦u6§±ÉğxiĞ½×ö9Ş¯ÕĞ­£OrPÏá<—×¬q
ºqÅõ¤Ù"oy§ó²Êy±Ç÷h­)˜@ i½UàGk<˜äB ÅÆ@&(]@+%I&ÈÄFˆ$Y—°v R™Æôòe5/?/K›äxY6£a#ôì‘â™)jH˜‚õHz
áu·KPQÀ»«À¡¹59Û¾K6/0ä“ ’è•ˆnØß0ÅÓÚ¿@õI•OËå¬
%d9û4‡·{kø*™h²å¨Ób`}ˆC5İ¦°[bi;ul\½'İÚÁ¡‰ÁêİĞØõ7ÔpÒ+qÄêê¾¶9±êqËÈEöô°!À¤y³ès‡¡wd ²Á_hì@»19ë?¶Ä@Ã‹Év(ä•£',*q¥#¯vfıEù—‚J
®z77¹˜LÛÙ÷*ù'&-ó5;,`ç9‘‘_`iÙ¹ôÍåÕ9¡NNÎz£Ñäìò"ûŸ`4ËÑ‘ÇßJ­øë–Êüæ¬÷vÒ»œ÷ÆıÓIÿüò?““w½1;ByÒŒÆƒ“?õ¯F˜ì0ñL…A‡óì÷w£N<^Å¶«$mP§Š›§MîŞÎÖD©9„g$¤yóù·¿%*8w®jo6kVOÍ*Õ¨ëá#Wv˜uØ¶Éº>
ÿh‚^ªòÆgëó|¹%‘ô'%@Õ¿ÁÕ«ş4¿†ÕŸíÙk5§áÀŸ "¢fšXNÙú#/©ù‡	8RÂ9¦
ßõúóµÜ7G­v¨œÄjÓ=lb*rìD§ŒîÚ€WÃûê˜4`	PEr\O9rÑ?!LwÈÎ2v9Öƒ†Ô‡!ÕUàG@]µ8${X6j¬á¼>bDd¥jË%HÖæ9:MJJ£ø-‡§6¤P
i„¦š `>¾|sìRGã·\9824Ê—~£ô€}h4/iÀ¬˜G”ZM·I¦ÁÎTi}›ËWi’Ù4Coèwi^Lñ”­Xv8û0¾XUïŠÛ»9­	 f ìÓ(îC,ÑÂ²¤tÕmÎ¨sğ)‘ñdj¼?Éjü6[ä£i¹nXaÈf×øÑë:ÌîP&§ñİ[gt»Ùë·ñXu?o:9Bµ#7î©òÂ£4ŸA„ÅŞŠqÿO™wŸ Q;¥Êüz[®É"»Y¼(Š¹f&~_Cû¶ÚMI´ˆtª4è¸H^K¸°¬R6)èĞ]&\~ıó{v*Ÿ\õ‡gƒşˆ<”‹k£ÂÊŸÖÆh³°Q(RxXtsK#@Ce2f>ëUV¿ç¨ˆî(já-ûğAfå	¥úÖP¸§Cç>ÊÜğaÖñ;u^‚='<Í—3.?ÏË)¼ã{&A‚A«¾ˆú:.EÇğ£-k¡ÓjË8Ûëã€º5'·‹Ö§l9EÉÂ›†Ü6í1¹ı¥aI[LîUb3Ÿı¤lx;®¾Y•X}ÕjZ.§Õ¥¼&Á¸KTù»)ñJaßUÛë&|ĞÑŒäfBğå*¢Z2öœ´)#ï¸Yw¹ŠnBËh%Ÿk ¶O‘lzG¶%ÌwàñÔÔA•Ï¯Öïÿğœ2øKàÊ;gœ`/o8Ë*)'®2×:‡Û…s…ÛE:Füe[m¼(Ÿ‘QÀ~sN›N¤õH©.ãÎ9,/É»®³ƒ	xM•¹İ¹XwÄ˜“¯ÌÃbO ëü@†'ûó¶˜~DIN:Œ¸/ÁB‰	¶ÜF)ÿë–Í-7;Ê§|3º+×›) ƒ1¥Ë-eipİá¤TÄ%‚eC¼[m~±ô@j3š–)n‚_¨GÑz1D—ºy¹áîPea·wÿÍõ7‰²WôØ1Šé€ãÎ¸‹T5ÅKåÈ>Ûå®Ë’Ó€&!W–¾ŠÉÆ‡şÙÉåyrŞzoa±±¶ÖØôÂ“I4ê}-å	‹4ô­9@ƒ³ĞA¿©Ìb’/Aîd¼'æäg•kgã·}25Da~æØ³ğª™^ VÛ£gHıÎ]¤wö™æ¯ğpÃ¹®ph	Í½•¡÷Ïí•Ö¦U­ö=®
QYEU¦nn}¡ˆÌr‘ÆhçHİ;Çg¾.µ/×X÷¢‘]zw³™×ö)IH³ªûÜŸ· Ö—^5LpÁo6ñ}ø\úæâŞÚ1XµEŞz³i¿Û½~àoK™)ç5ÜSS,ÎJw6Ü>›Û´Ì,(©Ã4_l©uÓÙ…¨ f#ã·½udôv=‡÷¢g~xk†>?r/×¸Å2È!Ïÿnšl–Àn›„'<$œNDgÈæT?l<×ùq¦éTïÏŠzûşHâÇÖKïhÚu*½$¸(öUGêm	 }İWÁEUû3ë ©Î®÷j…}´m|‡+«Ÿé   ÿÿ %@Ú¿B¨Ã’{K)‰úÅ%¢Ê”ñ¶…‹A¨ÜñƒôGcÿ>}º»¼ËÁŞsÂsš÷ò: —<ÎÕvçà^ _ˆ`İ‹LÇ¹ÉëÀØª]À\•m}êÔÂ}øö($75­F±lXZäµGÎ@¬ŸĞ‰ÓéqÀ[óeuÂAÄ¸[Mø˜eG{ÈzÄ‹ÂÜøzT‰‘mĞKıBÜ—9èjÀªì¦Ê¥£BÊsóNæöÎ‡D9„ç›EuÜ!úœ/ªâîƒğ8­oÀ¯a˜>‹çğ	şâ‡pÁ8¥²İÇÌ•e…Æ~UGÂ&åï²êî<[ıÈÃ"u¼(o¯ZŸóëP„˜âs®¡ƒíxOóâã’à 'å?
A­XŠ;“ñ+#.;Ù”s`‹P§c˜ˆ)hÛ¹S˜pV°7{ÏÄü5­Ò®ó4£
˜¯|™— ¶áV»É¡’ÒıpÀN`obıoZ÷ß´înns­»q°>/—¥lÆÖ,™Ó#*©¼¹%[Œè§kÄx˜6¾q»f©ˆDlÂ×OöÃ€Bs@/´¯6Ğ‡İCV¤h0~ù_y1â*¿&¥yú^®7!nØ¯°ƒ,ÛCáèùì±Ó„nv¨Ö&a<@½fn•Ü˜KC ¬¡Éã"<v„Ê›‡¹á'\ç f´SGg—t ®†7ädé¿ûÎ—Ãì"¨‚P¶Õ-«‚ö[R†¾Ñ·üà8dµnğgƒ‡(]´«Id%x·¾ŒÿrPÑâ•Ú­›áù5ğİï†à-e÷ì›6ODm}€àˆÁÖK`Æp*V¿F\¹D¯¨ıX´Æ0^G—†ˆNSx#ÉéH!BzÍìÉÊÓÔëDVZ¶ÔP‰U4xV´¬ƒm™rİZø>´]û±(uíîŠO9Ùxí@›îOdœ‚S1Xô'ì`®È×ğ&Z[Ü‰õa'(­†‘&ä[ı¨¾î 1]_Wÿ¡ìBBi|Y®ÏJiâ¹Ş3ZøÍháF¿]÷şvİû€|³ëŞÈoêİço×ÂßâZØIg†º˜fKa]SÛÖ<Ü˜‡lnŒvC»ª3ÅZUÿ‚­ã@ÈY ©¤2ÕY±(¸Ûî¡†Ã[Ûîsç
åôíC>Ÿ–êN€É½_D¥<
{8¼›¦ªJ…ã¢[úä›°æ?&Äû/’ÉÇ±JcGŒˆ`ÓŒK±n5œàyvg„„z[H_ç d@½ü3=s2i÷A2èÄ=h9½ñ¹içó]ş^ÏGŞ£d`…üÄÄ"Š<Ö"vöÈ65àIî2êZ<‹n½¹¡/Œ_Gœò+Æë^û©+|PÑ­gLsâIÚ-7O8ˆZÊcG³i|¨áºY¨ªƒ×€æµ%uWÌä€ËAõŸ´V¥iÄ¬¾xîimhâÂÃåmR%M"ÌIÀö@(:O]¬{¯^‹#ÆtañÀƒ ´uv~Ş’J9ıƒ1 ò1
x.Kk¼Å
ï›š¯"•ÄYCed3&Ü2"ÛLœµÂ7ô„(ûmLÈHd}›ô‚jù¢~ÁÄWj"8¥^y»LítD|vøª/øSó¦úY3õ¤…—r›}W-­Z¾¾o¥‹ë^'V³i¿Ä6ºàT‘366—AP­}ösNVœØ®wîvŒÎxûªé½Œob[ãC•İ= *;*€ÛTÇùæs/Cû–Um¶×Îz³!¼é<Œ\BPÙ½ûğÄàìšºôBb1îÃ/!pÏ~Œ5äxÛ3Ø;¾–aãº˜Â°?ŸYHw	>ä×CÛ¼ºÂü“¤ô™ç~¿ íÚ‘Æ§e{Ê&4^mxÅÿå}»H¥àğ÷Mw»¦<:dÃ5êñëUi°NœZÃq;^ëN]^£T¹‚
,ûM‚‹Ë·t5u%	§Zê U+<!Oá©3}ÊæÛ¼ç¦®Šµ»ßœa=xìÅ¼¦aå¨>çSç²:©¾ÕÄh2Éú%ËğW.ãrV”‹ëHpk [Ø„Vo‘šÆYõ±âUs¨İ¬ËÅDiUüã¶ÜÑ„‰9Ë 3`)@ÀSğc”©@Ë¾)@öõyôÕÂÀœË«]í’7´Í©T1Å"û¢hóé¢|Ù*dû§îÕLzÙƒ™ÊÇõæ¦jU7Ïê®´æ%p†÷¥´«®Ş¦;YJá›ˆ:
U¦7¤ˆÙ_vOïR$•at’’ mIhNŸ¬õ‚gåp!Ğ“Âq-oò	§éÁb rµÏÒ‰œÈo>aç€<Düè+A³V2¢÷1Nô–³‹r#Ê–™|ZÑ5ÙÕ
"¶¾±Ï(sÖö—‰m ŒÕİê’õ·\¢Bœ¿¡qn˜‡R1Ç•èzàˆá7¹'ûÚS8Ù¾za†k¬-àÄc‰µÎåD¬ó¢˜”]üZDà¡#™º³)òêrm_ó—³ˆVÄ0ÊÃØLÜ@úÂ¿±H¯¸È9\PRóÿüË¿Şà“¤îM¤XSÀŞr×ÖèÀ…Ş¡í«n…ÈIÁîırSÌ/Sàø“5©	Ã¸DÑº].;¾¬§ÂµœîLa&¸»	wJS“›®ëNª3Y6ZXyîá9aõYÎ²uk*ÿ8RïØÏ¦M¦fšƒåy1ŸU»İ8ÓCß…ÕïZ/“øá¶ªÕ»Ë÷W“Ë7“ÓŞÏ.äu<ûùàâı¸¿KÎQÿäòât·2ÏÎ)Ùå°‰ès-¹=ÕÄèàÃÖw±>4¯qî%º;ö5S¨‹@WŠ¸
Ú‚…8íûñüS¡1ædˆmİÎPT§ü¯Pd/;›Ö×ó\‚ìÄ/¶-×´¬m(Å'Teº\Ò‘å%ì–\±÷pàiHl”mªuqÆ*rØÁcQÆõşy´{š4qà:—·y9\—_À®ñ^àñ
ÏYËµ ª;!M7c¾Ø«l[åÍn'T÷H,µq¹z¿Ç…Köÿj»Àk‚íÒk]›p\İc2‚\d‚Qˆ]=‡o IŠiMÚ—\+<y’–#¶‘Ãóõx½¬™Æïÿ˜–Úäî/Ó2iÌ.Ï%·öÙfËÇ`ËÏSx1<Éáy÷íõğ„/ğé…%ú%L¸2£'°l±F$†eUŸ§g ÅDf·ŸeÅöüô­Äy%Or
qM}}KÏKé“¨Ä2‡À¤ê~7ôu	»©D´’VRq8VSK`o9¶ßY¶/“w7[._%"Ò§ªõ9¾plj^µ¼V¢ŠšeÀ›f¶+ze_¦ôÄŒ¬î^<˜ÿ<†hT6c¦È¦ãu¶¬àêÒôµ S~şİ$ 6ÓsÊ–è¹º**¥—õÏÑpÀ‡&wŠûm*lXTğ[h¬·¾şüÂ»º©ïl¢şr¿Gzi$®Š÷¨Gº/ÿ7@Öó`_Ç¤¤ù§:wWà{¿ÓÊ·„Ã§êó hPãÀGZıú5¡BõÚìz7¥àCPµq°Á’ÆE'«{]n0×»ÂÖ[«¾£øuoŠ9[œÁåìÍ[©­~0—ğ{ÍÖ7ˆ¼M",Év¤ôŒe]Iî…Õkšwo3 «Æth§±cKÜÑ©´ìıÛ§—Ø	,ë‡Èø9F™¾ì¹Å–°>­Ú
Î³ãX2vÔŞ­X’ñ…ºéíy°7áI8ÄØÖÎ–q…Ò‰m`µ*ç
ØvÕ[•?n†v¦¨!Êªø BEçsÏê6©†ãA´:7'LX¨æÀ:Î‰uT÷îıÔfm H<\—·kÖl”¼(¡Û–ôŠNM©T§!t¡0€jx”dAœí%÷*ğÈ€DÑlø<[f·¸¡-goŠµJ½/µF>kƒæQë*ŸŞOçBd½¸œ/G7B{z]%B%åvÀô®˜Ï¼€$Ç÷ªêZ!T+XÏÌŠ›º}¼ ÈƒìX––uŠËC6„+Í³jË•\" ¤‚V_¼¤¤ŠŠÄø!wÁB~<rƒ]ÎVí?ÒaXAòT¶¢ášÀ*Û±!701áÆNV»ÒÑÉñËœıqê’µŸdó#X.+%‰:<Z¯ƒÃÊ5e›±Æ<Rt »ˆ=D
ĞŸ‡FĞŸ¿·ú³‹#á4[`÷cÍ?ëÿyˆ-ÿŞl†,7›ûù›|ø!Ó¤œ¸Í}CÓ¼Ğü4ÏAîÅJ™o ÚñdÄäáië–Õn;Ù±€Ãn-×‹KÃCrªjÂÍcÇšé¹›VÌ+ËÂ>ÔªM@&6¶cd+í˜”éŠ2¿>›»Ë›°'Šh1’;hÖ‚oØşÄ	¼)QâuöIÉ©:¤ıü€çï%èÖ¥Yà·›¼ ]â®ÿR’¶¥§ı7½÷gc»[ñl²š…ñë{ğñó…À1“êê'ãdĞ×$8Õø Ùî'‹lıq»"õ6˜`€6ªç˜*fÿ’XXE!N>¶}ÃŒySäóÙ0[æs¶Ô8zŒQRdWŒøèïz€¬ÿ
Ï#1å Á?‡¦UªÄÖÖ˜fÿ“ òÜmoæÃ½×O p,L½A ò é˜zxÉ'qn
\&_…hø=ò´ï¢-ê"¤rI™¥ªÇõíJçB¯Âw7)uìhg[•!.ÙöBEj²|­-:ÒñÚ¦­vl9èvÊĞmkôtä¤×Ûååòı€_æ¶AQğÊÉÀ=´·Ï••Øs¾ÂÁÂ„Îœôy¶“3—ßù’…ŒyvoÜ2J ¼ç`Ú²³„-^uçùòvsçÁù‘ù(á½Q½ê›.ºyãb3§9 ^Ò³}éz¡ì/Çò§¶ó)õ!ÓÖBtWĞ}ÆÅÌ°d£ºL¬İu‘-FV¯oŸúÍEVª iŸô©K³Â8˜QãÏ¶.ÌçMnwÔj]NÙ§.yŠ€aˆ»jµAY~£,8®óE±]ÏËéÇ÷E£éD\’ÇÙí»bihÔw€Œı¼t—™.öıçûZ¨°à`×À“ÄW	_òp=®K9CíâÍ/¾R­TáÂn²)›ælëe9·(BĞSo¡Ô{X@¹BÓÖHğ„”·ZŸÀ‰êëup@M³‡rÂ¬¶
S™›_Ápªgº…òìÄXÄ,ÍÈ‚Àd ¬{‘C]°¯?¤5»`Zã-°hÕö©•ØêÜ÷×ÒĞ‘JÛ©>±=×8—)¤¼ò²m7zƒZ^Æ¿¯q—2µßª¶Õ•7*êA»U1;$tCDK‘ßı˜µ+qİe•€2[Ò>üİwä¹GR^Çh·' úŞÚ şÀR½ÂKââÙ3J
ÄºòQê±iDˆ>5pç'­ãh
\XTúä¹Öª	áÈn…‰wP¢I›«üÄƒø‰àÓb§àæ‡)Šò¦0\ğh}¦šMççóÖ¯é¢zjšßé“¸¦êtº8®æüûmV’L®ªOé4’$¥44¶yx˜ÉÂlfs81?8İdkéWŠ£>‹¼˜[ı!–ÆR¸V-YØÙW2ëÚ…m¥-¨]±Pv_û_·š_«WÊaÀKñb®hëè3›v9;ÑI£6æÙ„9šÒaÜ ØûğĞ”uéÏäBH,cÇO VÇF¨±;ËMÎôU4Ú=;a§úhRÍ U$£T¹ycí)ÈCU#„½À¯_¹õ¯KP'x¢¬/ÑbP¦Dv^†¬Y•šİŠãœ€9/`‡–eÌ¥+³­ËcëĞÜc@C#ï5õí‡Íûhì(j±¨^UÖ8‹|}›ËÏ^po½/¼¬Ô ş¬ù!øœ$èæ¡ğ5IÕƒjO`«GUÚj~o»WèçrÒÖîd~÷Ô­U¥w”)ÿ¼-7´YÁM6Ÿ_gÓõ½•©±PÊÄ„Ó‰C$hAûÊAbTË&¤–Ê2b|İÍ§GI »Û6nµš4bÉ§dÓ<	áB&–XÕø2y–Ş 7GjÓ:1#QÁT¨np÷Ì`Ñ‡ïåM3êNë‚ı£çƒ)úfİ‚oËhÃªÏxSm…ËÍ®/¼vÖÇY•ËÜ­ùG¼Ø²)ëjTÓI2R¾¨ ¥ê!³ï´¦§1< À4Å$ ‹ß›Éô]¸	äÎtÃuş©(·Õ([äFyÒŒVfQŒØdı\]õ˜ëGGæo›Ì-wà<uı"tŒ·§iØ 0p‘½Ûu5ìåúX®ks ,şy¼bö	–"¯¢InAÍ2sa•­ PToĞ/‡ÕùuëFü)ÀH©R„ <&’¤Úò~QiÜSS%’»à±ŠG-+÷È2nŒomojêóQz 0jZMçe•ŸçË­}ëïz|`(I8aWç™¶ùy]ôLŠUKÆÏí Ö…&„Ê,‡Å*µ,©VY^mÖå½–&Şšäºğ¤W0IÆÜja#™~x[2³p4v§™ÔÖÑĞ”|6Ôó©ÃC-ä—²)7Ùœ óì™ŸûòølMbeùnçW.vÍ¶&°ôeL¯‚ûFÄº4˜Rpİ»Z4ªø]…ˆsé/SxfİC(çMn›µ)©æÑÃ”rë²=…*…iÕ"™Šr£XNæŞì¡Ë©;÷˜)[Ó©¾µO–	›x¹.ná\Oæ”€Q–<Âò×Á«ırMÔ¦ÆfŞ2y8íêr˜ıõ¯{Ê™BÚëFC¬çÛuhm:ŸØ6VqNêÄãtèÎì
qù”H™äFØ?[©eU‰Gü,ÒÄÓ<ŸÆ‚ˆÖæ+L´£wzärÍ”¯™ 
ç!BU9_í¤Êj-(Å‚ëØ¹ùñ\xCb1á	;ˆDIâ <îY=À¼”R²RıÍ*}¥ë‘ä‹HùºRºZ"|äÂXÓ#«üNÇ£¿hchi:ÔÈ•Âæx¨8ıÚsRÈ)ÂTT›zÆ‡ÈÑ¼ç²™EkH
¶ÔB.Ûg\Sx¨‹?ëfã‡ÖÓ§EŠS™fğÎ¶Å¡.f¡&ì¥?ªE¸òyäàsğˆ(/©İğØ3eš*iµ@³ÉY‘f›ÌCĞp1Á²<?æPşâªQ#Ø/ë3ih½ÄZn™î#”eŸ‡ûûI,²´è‰QR«T¥æÆè&<»FKTvãy¶Ş5àMğ™x4’C‹ tÁNÀâæõ"·%ñ‡¥[~k†Ç9Ñ¤›)¡¼Ë7Cæ+ˆr8ò}ï²Rfv<Ü¨D¯k2ê_œNFãŞÿkö™xA÷Œ¦ÌÑ¶Àä$Éò“íz/Ùfö<á¬FÜ7îcX´Á–Âöñı˜ÓQë¬Øä 5ëé%+ÎÚêÕ›³ŞÛÉÉ»Şx2~×»¸íCÎ[¦‡ŞEh.Å&Uv
0ezøx=«vC–ÎYkµü³s™Œ/èQm»P
ï“5`¸Ú˜.·¼{;öH$lEfåIÄAıp§åd›TÚF¨y˜|á£Ä‚Ó¦ßú4Ñ}Ú”/jÓfš­ıÖ%R?9”·"iF§v)IòG^‘²-ü^ß:±y¦
N<<Ë',LÌj™˜#©àÏdbùØm–²}ºè`·>‚§¹’TÙÆ$äÛHL·Ö±Ñà©Ã8ÙóÌ@œqGıİÉÉ¥1ºøx¬9ùXí¬Ë‰4•Î/‚C¹TÂ9°òµlì^6<».nxâß=-rxvZè²û_ìğ<Ò‚‡gWCS¹Õ>¤òI²•)¾A“°ùGx9¸l¤¡¶`ÏF†êé{)Õíµ—Ç—Î.ŒOgïÂ~°Y¬úã¬'U“$]B/Ã^I‘ùøW°‚é<•Q2Û š³Ã8Àóˆ¼ø¨|X[¦\¨ù<Ì»ZÚ5Öï=NÓ´Ê$†‘q/­kp?ùĞ­ÓŒ»€ rHû0UÙ"o»)»Ì\´	oªòuÇ-İëéËòNë™Õ
knh®ü£M¹³©Ãnİ]²Üv³B¡3Ğã?P:9~òş‰tÌ î¨¨ÕD_vñ*‰Û.â2õWbÂpC¢9°‚·öQàá7UQ¥ïŠ‘C¨®œ]@w¢¦Ø8;HôC1Ÿcˆ~É†±XŞÎ¥¢İ.vW|“péRXÁ¯È®övYTô 4bKò)¯&ê“ÆÑ–Ügù<¡ÙsÊ¥í4À¿Š±®¦˜Ei—‚Ä­uù?ùr_ĞÑlGmø[õÒh>ø´û—q›ìÙÈÍóåL„Eü¯ç¿`OÎ9\®Ïà¯¡`›†M·ÓOƒCç×*ß`³±LÂØ1P$¿åé/§ë{ÖÈŠ=ô"û¢ö]èÇ#óÍx#LÚ»ç½ÿœüÔ;{ï	³ºPŠ”şÆ$5¸ğ‘
²šFÕõ—Ñ¸ºş–»CÁj((ªzø#µf¬Zfñ3¸uè@ŠFù@Î¤B˜9!è%Åµh
†;’~ÿ¥3ößÁ–`__‹­Ö”Õ¨z»NÏëh–GˆƒçÙˆ¿¿Æ<}ÚQÁrÜv‰µOƒ]úf”@úÈ±Uª¥Œ£è1ÃÁB©!†>b5Ä-¬;ãĞU²ka55Š™/,iËŒnùË‡ãL˜‹6Gd¦q[é3y<ñ¸ÀšH™ÃÁÅëÉ3„^;áqË+9¢gO´øá|»Îæ£Íš-¦öÂá	ÂÀ§ÙXíC*Bà‹uş	P±@…aÊvwü-qx1³½¤ódeFÍÿ~å:×>E	À<ò’ıD`=ÁáLF\*[ók˜ú
ùéèŸ]B”3§¥|RgÔ ß&Së+´±ù\4áñÈÅœ `Kt<&-Ê]š¥‰Ocì;È1-°ãôÙç‘Z¿÷5”7ŒƒÜçÙ2€½ê1=§˜Ø™PF0¦q
œ¸¿w}±ƒı÷Ä2øU¬ã–ıò›nsoUƒõ|­J	çëğ¹Ú¿4½v:¡’‹"pG¥ğ¬w‚µ­©Å.Ö‚
ié(.!Í„óPaæAÆ—sd‚)ËºÉP ½Ö¯”[¢aW+‰@âVãè¸å¦„O‰s#ŸÇˆõ}ë›lêÄªùŸµ	>«QGjşóûÁÉŸ&WıáÙ ?ÒH½qÕ’3§õ)F‚PqqEF.~—ÏWlÍ‹¨1\=;Ã²œ« ™Ökn™DBv‰Yºï€™DDø--Ô§7(¯C2!51¨¥5e,%iÉ2[2”¬L63†jd%¾ªõ‘¢Ë±Éi¤8	óàL!^=fêßÅí²\ç§ÅO?çL~Î¯œVîö†º=2«ºB
kë-:R:Pàø—ĞD@›M£­´KDGpZG–‚à·T)³MÁ{>³eå\?Aƒl$6‹L'£`15SJo1®¡Ã?“û=¯¦Ånt A 7r°ºr)#OÀI«c’ë:!PôÁ\;4Àb¬>õtµ¤Æù—MwS
-%,¡Óf<R!‰NLKˆMc'µ'Ášß„¬£¦Ïı0õúµÑKLz+ê^ËnºpMp|êZ5´‰ Èıvz¨IjÜ–`èc“qRì¦5u5Wq$²Ã›?ÉêÑ„±âÃ7×„’TOŒSiaªœN3Af¤1Œ¢»3“è=%RÔöÂè%ÃÑà}ù®Î±Ïüº`ØÄL=ˆNUM¡$’€…v0&(Œë¶Ò•¶äcmõÂˆ€ÒÅe¶G¼eÔ€&6CÆ_-¡­]El‡Ğëbz‡½{]~éOïJ7(Øæ~E8Üö'½«ñàä¬/h™ğt»‡3si”¬¢ˆË>ÿğC6y,¦Ø¦şİ%Löy	â?Ñ§¾Õ¦†F‡ê1PD¼×Îf§¹ùEç9ü›h 3S!{Şf&.°¥ÚymVÂ
`K²×Ò‹ïâ»¼Ö ì¡ÎÃìà#şZk™íÊ'Å€ÚÂÑ”whE(°ê^1ÚŞ²g“Ï˜¨¹é­VëòS¼ë¥#È0‚’†Ş¦ŞùÂš*ªùõ*CM—éi}U·[î,\¤2jì „RZZºÉ;rg@dÆ‚ºŸyeu&şd“ÏóÛu¶˜T›r}ÿ¤›ÿu›á>£e@¶”¯YÍ!?€]+ê[•ú+$zÃC#lè5È6ìts½İ4CÜàİ*ú±'I´€J¾8.‡¿º²W‘5Ó?[%bŸ¤˜¢ë]ÖnÇèAù‡]•'\ax°?vJ¨kèÍ\ö!kÃÏ¡"ãJMEY›ÎÆºÕ¾*(eRÊÁØÿÄ4FŒ¤Âçn7$iêÆÛ"ÛsĞVÄëén6Pññ'&IöH‡*â¨Õ`uÁwò€ç‘1xvGsÃÎæÛ^Ÿc¨…/Å!)àBó½*ëıÃZ–u’à „$‹Í&›Ş³Í%á×_ııj¯sô¿Õ¦Tí×©äp71(iovÔ:™%`ØÆsE”ƒ×î.áø¯  ãšpì÷5ÁG]ºíÍ}0KÖ+)—>ı†mÌws
gµõ{J«:¾ô€‰¥»z*2ÒQÒÛR·«dI¢kÓ0Y‚—$éx,v™Qt
ğKÈÙ!ÜQzù@(•Ì×••uÂfCô©pSD›¯ÄÔ ñçÛª˜òãóí´œná´È?[õ\¡%Êİvq]éš>o"í9+o'èxö¿ßÍ<G6§*×ûÂŸ?Æàlxí´ş	/üj^Ló™€—Çş¦OéKx4²l# ôD•6'":˜ÑĞÂggËY¹è.ó/ô]ê¦ú5›ÍzóyÀj{{	ÔÓÖ‹8àPbeÔ)äšŸB®!†UAyü¸öˆ&7ùõ!¿bz«:–èî´6ÀÃHı¶7jxÌ%IT¿£·)±hßŞõx£:1¶)ÄÍhQl•¿L³Ÿß·Ì -ëÀ|v%d»JSà$}dpˆ¢Ô#„UÙH›Á*¹B@Gï”İ&©İâÀÍ^³²Òğ%ş¿ÜºëPîğz?Ñ—ğØRÜ(û”ù Klp±å¹Îù=@%ì[ãGb´¸1]Ñ‚:­4t[gãruÏ#u³u¶¨<ÂäiÜô,…«íÆğá åçŞFo} b¨ß„\/cÇqøTıó2­ƒZ;ÛXspğ=ô½k¿÷7¯jQãûÒÑÓ6Â&ƒ†•|¨˜‡Ğn`6±:„Qää¦6÷¿—Ï£Ím½nµÚ¥	×N^Témó9³èOšä²ƒ§Õ·ªfS§¤oYÏFK_³¢ûÃl¶Áê<ôå×ü*²ıù-ÈÏšJs¯éŞ×®A{ØÎ¡ÜÓîa“ÜË‚DwßEàyôDÖq·İDæ~Ğog
w'M7µ‡İå[Uù!;Í·¬óÎ»Î×¬ô×½é§¼e‰ø‘I—À»"óiŠm™Ï£ÍæŞ{M¯,]|¾kÀ?hÑW_:®F¤ğŞÜ§ê[„÷Õ`áebWdH»)ûÂX”Ïãqú/İ_½ß°Sóµ¨¢œÜ”ïé¢¸àqy‘F@s®—f¨GÜ´hö>ô:ßl×KŠ½ñY=/—·ĞÛ¬Âs†–0€îŠ4“B»-¾ÏY•¿QêÑŸ_ı8jéUD«)ñ‰uËô£‚CÀKá×ÑÄ°Š	âÅñ÷–•®¾‚ctäÂÖ=¢»›»¢Ò[Ù§âİçÄ(ğÎ:ÎÖÊ	D/ÈræÌ€_ˆ´®Ólõ*†“×åM1ÏÔZ¨¨.‚)”ë ¼¬Ğ †ÕšpÇ°}ŒirSO‘„B|˜?ˆ~í1šÛˆ¹»]Îæyëšÿ#ññ‡×Q¿Â^Æ?NÅÅ“N½Š<9í†f³™lè¸äMµ}ğÛ¼4&®ø dóÅö<Êç7PNôœwSöõœ3èÎİ–­o«¤Nƒ„ØeV­ÛPÜ×ë300.ı_Öivµ¿A¯Áq¦Šu›“°µ5~µÚ-Y¤_;0È@€kú=¾_E ÿÍTıÂ¸To}‹ÖeA¢]Çó¦­Ñ!±Á¥% ÆCî“¨“@o9[3¹G³sÚ./—ïR­›Ê+Ó¡oû#0ğİW %Ïöl¯£™uŸ÷Áå5ëÖOì¸*L¯s‡ªét^V §o(9ÖŸ·Ëæèßã"%åCü İ²‰àBÁÉjƒÕeQTwò§Eê’&wŒ÷Í9›¶â
ˆ‹îŒÖ‹çÏŸšµsˆè¸ %úJÀóÀŠeU‡€ûÄßzËñøÁSQÖØ^£ôL~ É*È…z¤D˜¾kÉıgG&L‰ºpÒ°†&2¼ØNM h¹¯R!Ğ²¸&“¤&á5ü²•Ôğ”Ö`ySPV; éÀêXò¿nsÆ[›üË†c¡û£~˜„BÛu&ìÕÎ³Í]w‘}iÿ›fñ³X¶Û@AF¿fç–ï‘hw/o7w°i~×úÃóÖïZ/şa–>×µÇ,_¾)–oØú,×÷İò¦G*~.ÉGÅb5Ïe¢öUw}&±©&kî¦XuD#ş{ÁãTÔº-«o/—ïŠY^µäVÒ£º¡pÄí…Âj¿…xÇÅ|& p	>J /!¥ *ÖçüÏS'$‡l×A3[Â–˜('9kdÄ¢‚ Š-(Xô[SøŸÌôr±U4arC°|†¢@õºó¬.²ƒôåx_ƒ\¸? °ä`¹	Ìg`ÃœwøbJ6 
0§Z.
á!VÄF_¡2Œ€L:F¸¬¾'Á(ŒMÊù×Yİp'IH/š·Öü_[s%^ëNàßÛe­œ‰º•(Û%¿n‹CGµ*şÏQË&Uç©7^yÑ¾>[›ûW-@û†ı› 1‘ßÌ]ÑÔI{áE6¤f‘«’ GÂ©4rĞû;eRó±BÕ›ÍIvm›kDëÙè¹}¯AÌ8»9B2s 7:”&F+&&ŸSÇ[ÆÙÂ;ÃZ÷"[Qş¥è
,AüÑæÛÖø._ä¸£MÕí±(0Ì
~||SÂêd”§Õ 6„C”b^‰Ò¸®<Ÿé¥Êæ6/ß*Ø,‘IA}Ay\":…]œ†Œ/ÿo·‡<Šò.şÚ=\3CÌ²8/-$ƒØ<[OïdôYõ‘KaËí¢SË6‹¬˜#N•ÈoB9ËÓ—¡ÑÇ‰<3”Ú#o¶ã÷ãñåÅdÔï]¼›¼vsc•oığoáëôºõ’	/¹S&	·òÈõ:½üpA×ì«ÙK_Í´aÆ!‚hp'RÆC	H  Ë`äêÏú@#©G?÷¿0f2Ã£Šo­tĞG'óbú:¨Í…ĞW-ûŞÉ•q6ÀÎ’Ù|u—Õ¡[nX¿<ïşá6²µQUÿ™ÃÓJ{Òšß}­”@¨¢špÒ<pRÕÕøÌ=bÙÃõ#}•jÕ_@Ÿ<é¨bi×oª
Ú:TŠÓÆzœåU—óµîEÉ…¤Ãô
A°Ÿ€ÁSrm¨Xãìö¶¾Je½dõAR5]¯\¤ ß
•O"~¨ADsZÛú Öğ2d‰¼£eü«a–‡âÅvq;½üŸ[ÿ|í·Ç.—7‡¡‡fú8ÑËüß%¦)_ôÛkÆnÔ
]æùì4Ÿg÷—«|©°Buï‡"4HÚ-(w;—ò©ó1xMì¸·Š`"U”ÛJ»€N"æ!êñ¬e[·J-£tÏ$:{İ¡Ssˆáñ§üşºÌÖ³à*ág 0´_ı—L¨üÂdï‡è\@ş_.s' zÉ{U#}Tú'Ç7«ÚÊ0MÅÚ*™Øu"•Ó<ôØ›ÍÎ¥5¤ãŸÛS3¹×šë£èÑY%
÷Edj½‹ÚU²#‡C ,³9¥· sß¬óü¸hë>hªÜÆ¹fw tGÖfƒïûïÍed“ì´şøü¹w@¸÷ÀòŒKÆÀ¸û“ùÂy9¡Ÿ¨4¤; OãèŒBPÉK›`¬Œe3çŸì(¼†HHÉv€C¢6ºjSÖ¦{úşª70Ñwxuyvyñ–Îg)?µÁ©‹¨Z0!ÅŞ£ÈZ&R6”ûFwğĞÿÁ•&CÕq…Ê m,,Ö¯µiQõ	L²>Ëkku*Ûv¹ «.l­’²şû¯_Zór*Ôğûå/u*[zÃöâÙ	WƒzĞø‹ˆ:™Õh-øCëÙ3ùÒrÅ6Äçp' &˜\²Š‰»lÊ%•¤¯tNâU”¥¤ïâj¬Öå”½eKs²â.ºŒ YÏ¶¤z"´¥gë¡®ÆLhŸP÷fİVìkjZ†—£®û7g½·“ãKvü=?TáäÈµ}RT—Ûv«âë`±gı7cvp%¹¼}7n—©øtì²n C½K±FÙb;üÁ†_“7ó2Û´À„%…(©O1`?‡n	îaYÍVí—ÿò­äî!T›³ÏÍŠjÅ„_ˆÜ½gë2ü¦“r©,³5"ºTĞÙÔÙoñ€Ökb jpÔºtŸ"Är©}Öˆ3KøøÒË¶Îr;õûîéàª‚3INo·ïLáTØ"Ş°é˜Ï¤¬*dx¹;M¹½°Ğ’ßÈkĞrÅÿXÃpñ?¯1–7 „b}E—ÓDz‚– Cı„b›uÖÏíg<V²|6Å^¼dsò©¯On`x#ŒbÁÀl¶.9’0nîd:øÜ›·Ë¶Ğ·á´„èŞ^LNúãşUDhùı< ’€tr|?.·Ó;_-¶U>‚Cq –çÙ—Åls×~ñO9W0ÉAöú·Ã¤¹ÚHyã|‹TÅ¶ÆîRs>ÇA_’¼§øN‹w·ˆÇÂÅQv\ämãÃyo|òn2ì]±Qè´ØTé´Ş®34a_[S¿ŞÎÎ&ï.¯ÿ÷òbÜ;ciÿˆ&$ø]QÅœ8¡Öy±A“dÈ¡‘0Ö‡7åCN ôÿ(ÙÃ/mÖ]Ï€î,4ì¹Ù°ËóŸÎœ˜­ïZ/oRKq;v+âl9SŞæWÆQÌ1Wˆş?„‹X?„x§õò9qöóœ R>s?©£+}'‡(¡À¢K7Y‹}4ğår¶H¼v¤ëlDòQQ|¦aÀŠt2ÆÇí’íO+ôlšµæ4Àˆu>—'W­—”†Ä«8€b]²ˆÿ8/¯³ùyV,Gùî/ná =0Mx¬©õ#úç”ê¬oJ¿‚ 5Îgë¯°feN‹Ş!€(—X&×?°dƒH´ülµš»Cµ»}4¸b~_sÆ®hÊÌ©kğÍæÏW²àÈPjL›UŠ  h°GGR° å¡×`?H`â¢¬t>®*g«‹¨©Ú	="Õ÷?W,y6ŠdÛ¤”0®ÁÛ2H1º¯6ù¢+œ» Ï‹ù¼¨P2;®Mşíùó‡¯³Mv\jiš2xÒµeHwo3xvÒšÉÛEsF5¸©öL•2f¶ÎÙœ©Â1¾Røtü¬0Xo¯Ù\Qï…E"Å.™zü}Z_ïıº¿E¶+YYO$Â®¾ÆUâÀãgf¾ÍìÊ3LWÆLj]o€D}îI€pÕJÁL4=€úçœÅ$|F§&ÏñVQ`C¾Ú¤%}eÁ5?ëíîgß©Ò_¸oG`©wƒÆ’Æƒ²”@ôÈgYŠºsÅÆÑBPâú»b6ËM;
ˆxÎ
äNˆåÚ¾÷	W<bü›<cG^©Èñ¡# šÕN×ÅuŞÛÎŠr¼.²ù‡<ÿ8¿çF/ÀcÃÑY1²Òª±¢$MùÊííØšXfğBdoŠ[¼náíØcql#Š–EáSdÙ©Wšİ²?ôÚo×¸“Ëµº5¯I0l ¾ü©, b	†À¨rDÕ l°uî÷KœszŞ®T_˜’
2ö„»}±Y|Q²¸Íò¥­9óûcØc‚Œàòæ¦~•ğGø‹Ã\8Æ–Ìå÷ŒsƒXeøÂ:Ucs(ß`—ÉO+d‚`odğ.naSŞÂ¤ãfâ¾Ë7{ÈÄ‚ÓØœŠ&ì¸_Ôµ›µQÎF#ïÎr¸[6êJ»Èo˜Ûèì8«4‘Wx<,vLAÛ]akŞ_r•‡1Q´¦M¶å xo€®ß–#d6ƒZ®Zn¼&í_@5ûË¶Ú³¥®ÿ6¯Ršf–±([Å »Âğ4\—9°­Ræ³ÌïQõ-MS®ó«ojpgÁÆ“ƒ*ñá«Ş(CSŒ`<^Úá\tğÄx­–r1ã,¤¬¥eç¯Û¼Rf~Qö‡»¿O‹º4£>¾(! ,eJ‰ø@ê¢²@¾×Q’ëlúñ3ë½a’¹x‰)"2³f…ÇkÊºÕ¯?ĞÄ<É3ø9Ÿéä•3„ˆfƒÅRêâÙÿ€6]6Sx'Ü
]
$bûB£ãöqP“Ûgu8rÒc—n¼Pküâ?7÷3Ü\ª‹<Ÿ]åàãè4!~]*†tZÌÎ|Ô…=o¾ÉùV×¨)?‰Ü…“(şÿ£wj´cŒ_¥nYİg³qF‡êÆ•D1’²ÔşÉµ~ƒn>ÏVôU6‡«Ê€j—=¨HôJL… "SøMÊ¥ßtÌÇ¶Iœ…D/VÕÔÕâtô¾ì™¤p[ QhL‘Ï‰À)˜çÛµmêçŸ^<&Ş÷™ËG©CÍSjn¸Ã²%µ¯ıHv×ğ.ZºÒÌuX,¥µ/ˆ·+ígDÄÑ’z¯µ°uV)$«1g„o¦ü=@â²£pÌO	$‹v%ßĞfĞÄ’ñÎ~«\‚¥(=İ`)#¦¦Ñ—¹x)ß}§—}ƒÍİŸúW£ÁåEwtú§Éàbº÷—xÏcfÀnC%Ô£¡u579¤u»5'Ù|šQÄr‚'ğ	Ñ¥_ÉV[Ê{§)°=›¤¡G9/—å›r½]ª<;jéØt;Ï–L¶q’÷¦Sîõeh^ˆE¨WŒfÄ°ôFùr6ÚŞ2!
Æ„¾\uwıš¶÷¼|—UWù´\KõSàt\TP®	úÕ\Sh£üÜyAİCÑ°°EJíå–b¹Èo3„~¥¸q¬„[)èj]ß7®wÜV§®Ík²¶TŞ.êúà/‘÷eëû2¿$´óĞ—wµşŞ×‡˜¤X²eHÁRÉh çšğÈ÷Í‹Ág˜"yQ~ï¢7HÑ›+%Åâ§ƒØjª%Jw ø
ß¸Q¶ÈcÈ&àG1M‡ŒA˜Âãƒ1M¯ö(R¬à>ú`WlSxC}Àåµs*V¢¬ N£›U	©}ÂÄ4vØ®ØÉø\1ÅğÄG(WÖ¿‰Ä'ÂvN¥Ö^ØÉ7(\õÁ +b¿²³Œ­‚I'_ÿŠŞ7„NÔŸ¯qƒŒ›¸Dmçùìr}Q$Krn%‹î£”$%SGUÁm£Û]¬â3Œ5`úpÅ­=¤µ¤¹‘Ãæ&5Dî>RT8›ĞUÇvVïŸÆ“ã÷£ÁE4šœ.şäfÏÏ–}‘ÏgoÊé¶Êg¾qû‹ÆÕ’²«}÷spÕV.Já «–›ÀÁğîËŸç<×ŠìXAGÙÀ_
#Oš;Æ1áñ&_RONĞêï£à%µq:¬o«é‚c?æí&Ë^]_NÆ?ûàzùŸ?Ç°û#8—³Æ–›Ë5¢cjMí´°>ır/€¹uÕÆ"##±.Kñ\x_1–ÍŠ­X×•kôZğÔı?   ÿÿì}ıwÛ6²èïûW0>ç¾•[U±“´¯·İ´O±eG¯şĞµä&9{÷èĞeóF&uIÊ‰÷¶ÿûÃàƒÄÇ )ÙIö…ç´±H` ƒ™Á`Æ»¸a¿†3®ÊSÔs
Æ}J8í­òpâAÉ#VÌ½õ=‘¦púŠQTG´ñm°3¥„;âPu+s¶C:ó3İ!-ğ¶j¶è­I·æ•†¨5ñF‹B™¸;çHÀğhdfoÂ¼=QÚ“vX§<‰ìHâ°Ü%ĞRõM§±EŞ&y9TB,ì¯ÆË*.Ö›¥··±UQÏ¶Ùx¶ÌÜ¨p1ºû"ÃùMÇã¦‰_Î(v‚Dİ½0â¾ÄÉû\(“Js5$ò¥Úí›¸¸éS¯7p+?õÙUĞÉàíd:<;:×éRºgÿwí]­ädxJ7x *íèòä„†"›¾êü6=ºèŸÎ&6Y§	ÁÈPxş´ÊîìR}«¼4oğˆgMÖ[§¶¥Ü$u<€ÀI:2ş4Öm•¡0mM€«46¼¹Öa_úá±.˜oB.ÂÆË3î:?°T‡³Úññ,‹*ï#¼WşgëèVú¿ÍñË®=qr.ã9OE¦œ ›|Hmß§Q²~•“”jÅúÑãZ”á~'X=uüH	Ä#­AiY§1ƒFdÚeë2ÒÏZEó#º6Š”×iTå.­pËlÇe.gYo
€ƒUŠéö™w ”¿gä6O
ÎÚ%¤xDs½é·{Ô¹€‡n\Ä³÷pB^ şx<ıFt®GßæQÅÚıæi`|ı9xú4ø©zŸ3°ğÅwÕNÓî›'–4†·o£1H)_²–‚	£|Ôà{†QºÄ~‡¯Ní0K"jU7i™ªvtDP¯âf}{u˜ÎhršA ¹G?Mçü›’MôÖ¨mq6b439™
G›i^Ñô'_!à[¡¾D(*Â{u?œ—Dañy–`ËÎ•ÕëŸÇ"W(v¤§ãËœoÄ‹ÁçvWo#ò²Á`Œ³m<”:È,PlšGR„z¯JHXŸj¾QB|`•‘:öuiÑ¢á¬He`„ÛÆ­àìÌwtWo©.4i&%t6}«ÑYëş5C0ÍcFêA´iÔ¥Y‘/£ËºÍ{ãQÿl:x{pr9ş>¨şòÃGñæ¼¾ŠmÑu*tŒ‚Ğaá,¢/ãæUºœ‹a* j‡9<Ã‡© +\tÛÙ	œ3ÉKÉí{Ó±9$Y\$ßÙù7ˆŠëc3Å®J úÙªeeò7£Š1Ìà"Ö‘}ï_‡Ë"›	Š´×Ò«_GÊ >ßé¹’ğò£pN—ó|Õù‘…ø«Y¼›s9ŠÃÙû‡¨)ám,W`¶lFGùôæ¢?šœöŸD8Æ‡íñu­u¹ê1áè4Ì®c»cİÍÃ|EÈeñ`¡„LôwI£Heí+1v¼nÊú‚–.=ZÇL+ñíŠrÎ·¸	WwŸaŞXKˆ§ÛŸİà¹#ìŒßØ„Y…OU!Qe1íBEÉb£-¡˜8   gÒ7Î¸„¢¨˜<d0ëñÏ*™ç%¢âr‹+Yàœ ß“z_
»@Øá¬È§¤‰ty‰Â¤Éÿæ›YmYy¬¤c0^ÊÍWe˜Ø•DìÂ-sıbÎªÉœQ:N—fªƒ²¨DY™s	Gªe[ßıò?Æœ
@.³½uœóQDvvåÇË ã.‘LY“¸ĞÿD.L1æ!Ü³&¡4'K‹£C±İñ-C)àŒ dˆ¿§ÌÜATŸdí²û™:\ö’M%ñB#"kM—5^/Kûo;¤.ÍSª!ª±ZSŠdh@sŠÇ¤Q't™zõşJ¤¼Ïiyß—˜=˜JØ²-·Ä¸bÊÊÉ>6öÚùF^Hke÷êtÎ(ŸÌ¼c´Êƒı–­ßxÕ±Bºl£B‰ ò,=AŒÔe†l.êJUÚ©¢ËRÊ­î`şj~úIzE3XÚ/*c}”.’(·D«×
ë`îd¡¾4=Ü&ä§&1á –KÜG İ8Ùu¶
±à"ä¸ëôĞ×9½FcÜ<ùRpóxy¢!æó5».µá°e‡kİ±İ/Àg‡ÚÇ­#æËÇïtSÌx•Â¬¢84¿½Få;qÑmî¬Y·õS(8Sëª,Şt<HooÃdî¿ÛJÔ¢v;YÛ2^r›ÕŠ¬òó&ºê¯V‡q>[†ñm”õÉìs5V5Ç×IšE¸ıÃö˜n‘^_/aÏJD*­¢1ZÑ—Şà©ÚxIÙÑ|Ä’ËY¯+x€øÅE4—ôfbc@HñU$ùı—ßŸùÉïÍh|åê²g¯$şé¼¤ç)\°İ`®{èñ%™ëô¦îÁd\¾á±"‡”MÈaˆÏóĞ¬K~üX;-I˜
B‰œ¼–áõ0¹KßGıEA¤¨?jÊ…ñò<aWõèŞ–ûîEÎ[eyÏPñ”XÂÌOÒÕï,¶ôƒ§ˆÇ
V)À„¹Òz	Jä&èI\ºL³KFz­ş|İOi~RF	Ã$6n×
øö€/™×´Çœ0IÍªFÛ6KÌ©Û¦+,¤üô‹ô6½ù£¾æZÎ‹ôÙw»¡{(»‘Ğ‡`WÊaRìFKZĞ9åÈXj…oÖc(ìÓ&pçU¹S°EÙmœCLön •–Rİ@ŠëÀã¶ÁQå\Dƒo>*voûNH{Ê¼ıù|’QvÅ¯­tƒ7ñì¦ÅFÏç$‰çGH7Ú
I(]Xª95·èç'@‰g;x>•X&?[®Z÷ä1¥,ñøK[eÏCêªºÓ˜Ÿ'q36~@ƒwì–2·íW%d¨mÊÊò´5Y?ã!ãè4%8âUúQ×û‚ü“"ºÁ÷Ô`~åé:›Ám;Èû’y“ğĞˆÙ’+Å^‰y½êüÈÜ+ğO-š«äÃJ˜i³ğÍBÎo¸5‡^ºšanf> œÎ:RR•L™0ËlyNc>«“}ºÁ¿·Óœ}è‰§9~EçbEÿäšøFĞ~´ÛÇ|s|uĞÜôe®Ñ»æ[å•¬
@x
Ì³cÜŒÁ‰‰êl+M^ãôg6a5W•ÏİÁ:':èç‹4üC«W‡ª%€­ÚˆË<­Ïn¨û 6öÒ å¡ áé­Bı¼‰¸~Üs%WA©W–h)jËT1Óë±»ge$ª‰Ú\@´bËUyßÔN•19“+,¹/‘éÆ‡qÚ`‡ıâW7Ãj®mù|…U QWOÈmŞÂƒŠKõä˜:o¢7¦,ãÔ]M+}\|®Ñ5ºë¶¿+‚·`bÊ£\·Ó4áñŒ@RF,YĞkT¢	ï ş5Õ¼èRuÎ#»=QåØıPÑ…*êúq½~y“ôê(áM7a^]ƒ9–?Êº
‚5ş•U­XŠE¤g'ÜË¹¼† $Î‡EtÛ	Ù†Û» ŠIo–®î=®& J¸G`N«£å§ëjù×"^.«‰¤<Ú.Ãª’Yv¿"	˜†µ´]ÛhPåìdr
Áà‘æ 1@Î¿JÚérËÒd©2ÇJ¿âd¶\Cnõä½²Ä­£Ó 2h2d¨~ÂAo!ñr¼JcÂ°s}ªÔD©ˆÚ!à‚À®İÖ•(Ë•ğ¹ˆ¶ÎxIÍ9›õ_Oqš1{ñæÖ¶DäyiˆÔ‘Æ!™70¹ŠOÿ½&›m—Áÿö[¿ÿuvñ¾BKû<4ï_Î 4ì!o[ícùçæ‘	üÌÅKåCßò‹
£ÕEfè‡·îmÑ¶Œº!íÇ6JxÎÎ‚4š?ÍØ({E±ØùThoj+­'`vF&ôíâ6¡‰F˜?z§çgç_ƒ­¡¶9zoé¾P‹\•m)Û‘|í—Ex‰ÃR«µ{KøDã[ìï‰·<iğû½añ‚İd\Ü/éìôÊ_ë$ÈÖ‰8D²ÒdR§Yeòà—Zµ£“şñt<yw2 ÿ¿ş6ğ¦L‡´ğ	)&§Hô ™˜ÀË„”Ã£>‡7Q—g‡ƒ‹“áÙ9Wk¯ÆK\•›µJ|ßP
9 Î•ĞÚ.bråå9›	L¤a?¨šô¤«2S.º]s\eS#´³¤&ùPX\è‹«
O6ö±ú<`§½LëUãu7CE! JøØ)›»ô?[îäYt½^†MåÓVK7ÊñOXÀ›2êTf*Æ”6i~#ÜLß 1®•«mó³µ²=‹O6a2 ¯èõR°¡ıP¹¯ˆ}—-„zA¬ÃÄ•\Vp1=âŠcu|ğzpxy28ôØ`×Æß„qz—÷æA·R˜²§ªŒTˆw6ƒ¤H¤_<Û)•\¤Ù,²¦ÁSJ¥·öb`ï=µÒØ½,ˆ¼WQš®³,iLÉáà¨y2±yÈŸgÇñu¨8ÆÛºÎèmkÇÖº=–íQ–ş“¦Ë/çxì³”Ç9ÙoÁ+Í¶ÍKcP0õà½w\Ó´Õaç.e¼ºĞ‡g"ØÍHFF¦M%ßšbb=bİ:>?Ó{d6a«ŒÈ^şe.‰°GÁ¹ò\÷P¼Î&™‘’ZiÁ†¬ÏT=šÿ.×Qïàütt2xKd°ádz8uƒıç@‚Êf¦¸r‹œº°z%¢O ûB¬ŒLhèˆ,!’\;é{+ò€N/¢9³‡0±ñª½¬¨u¥íd³FE ÿLôHM1<^_’İdÔÉ¯¼Ëìšá+Á>Ş®—E¹º	ís¸`é{ç<pS>%òã*—½½ïäVúb8"Ô==í›öONüÇHÏêi¯ãp~ÖXU-35q-K@ÎÎóhüş'4t•’øšvŒ³hä/ ®°|°'Şõøm÷)øÅY4å	ë2ëœü+Kİ.K­Y\6·1YÃKpŞñöİI–÷|RÂ„ºÏDp¥><Š  ÖPé„Ï}{¡»Ã„21›¿'LıqE4Ô|À–wd|C>h7¸tIDÁA:uÁ Í‚Æ¼’6YÑÉ˜ŒSÍÏ }4Vü\~›^F'ÃÁXÎí-¢µ‰w3¦Ğ’Å<{O¥wQê$¾æ4üuñ~–‹×ås7Z®³pYŞOXçqBæ•§ğ S»ÓİŒ&>ÙJg‹!ZÏ(]­W5Yl7Y o'„ˆØ0ûÇm×h´
%Óú&ZBjsQö4üøuşK¬Smb=×i=]ü¼VMXÿ÷Á!Çá]4o(!cviNŠúgçgïNÏ/Ç–´iŠ
qê±¹}Y—.ˆt–Kö:@Äó.èã×êzÅ øvÌ³ô \åÁOZBH¸6P¯„j– €ØwƒÜ[“.JQö4dÌˆ[y	Q[CÇSmÏÎ‡&—<´Gq‚,-¿Ä€šóq&ä5aWGÍü˜&éÒsO¿ùãL“ß<õukËduÔvEa¥­†Ét'BÓ0ÉÂyÃY‚Ø0O¯è»€PkÈÔ×.æJÚzáJG±ıåRL¸X/Ÿ#·);¹³	
“h^Óíí™Tê#=±ÈW‡ÿ}p1<2ò÷ĞX,vË:Û/N×E4—=®¯¥¬‹–Ü°›"•3ThšcÓ~»ÆÚš8ÉD,Z-h¨a×/“[¹ó–K&­úŞŒ®°ô@˜¬ŠÊóÒEi.æM¯FğÛxÑ|Ó¸šøI5˜¹¿Ì’EqŠû¯”0È²iAZ 9Ï$‚4BK'Oëå¼¿BOıÄğË1¬à‹cÈÓZ‘íå*Ï‘Lh{gpaœFÂ3Z”Vo±ü”‹š¬è¨€C²Y“­[ÒË,ä={•6NŠá"Âİh{öGg>IéKfÁÜ>w]Ì´ÓoıºÚø÷dZª»-ªÒNıÍwéR^¿°¤ã{òæ¶Çû_Nãå2ÎéµÆß‚ı½½½à›àøŸ59“ü´fX›¾¼8Ì:¿<&ÅLñ—BAKÙY¢÷ ‹ô¹ué)æ3Ag=*q!£='q|¨_¦uüÔÓá˜¿	Øò×PÎô0ñ®w ¯Ié|Ş…ñZ©¯{ŒÈLó*KÃù,Ì‹*Ø‰àQIÌï6NŞ“-ºTÄ¦,Èè»ıÆte§©VZ´¬$	±İà0ÎWàÖñ!^E“ô$ZA˜Z ‡	Ûy­[ÛÉ>ŞFä\=@,^-í)ßa0L¨`£şÖ<[Â¸&´¸ä’Pa¼P¸uUÆ®QËWYÅÙÊ˜Ú­Æªı¸î§à¶Òª¢%Y–{iÕÛ½mMWfY£•]Vµ®ÔˆsÅ	÷¢YÎŞ³tfåÁyO
’³Ÿ¥7ÍEoÎÿìİæ×Ó[¢LÄ³İ!S±ÑÖ(,cóÅKHO	şM£4#®ù‹õÄçCœÌ	M–ş?onâ":ÎÂ{zşÓ•ÀõÀ¸Ú;½<™G'ï\RD=‡Îû9>SìÓL`óW÷ı9ÁQ«–}$ï–KÖº1`æ›M7æ6›rë|’Í¸Õ6Û„7ßNid‡ÉMœóĞŞa'û|üy)^eëaÀ†á ëÊ&V5={#ãš%!yMØÚ•‚Ì™8Py<ß†ÚNä¶&ÓœPÑ+ÏŒ›/‚ÕËä
PdWBbà6jhûhËş«×Ï	Cà—€á)d=æY”ÔSV“Öôè_®÷†«šÿ–2ë²7	©Sgé3à8F%ğF8Š£å|‚M¶Nç°kÕ¸³8:³ŞHÓXÃ¶m<Šñ›Æ3uİØ€çµÁÇÕxù6ÒàÌikÃÚ¥É½J}ÒÔ¨ LH˜{QÀclM6‡M<Øàiãm$p¡¬Cƒá(-ıİ_¥a6Ç öª¤NGél£Ñ,‘Ô tµ–†Jì-Ë7§œò*	UxmãÈ	 âÕ*Ï£èa¥\jpv„À*ÿt¤«‘×ñuHïÈÒUSùÿè¾¢ğ‹y&êîŠ25÷õÛà£,º#„İÃ]CÌójHøåAE„.î¸:ú¯i~ÖşğlpÁù+Îf¯ì¹éˆƒÖÍ‘n½t9XÔ,kMŸ¦Unª»Ñşñ‡rkÇ®ïÈ»aïsz‚2Éä]½µA\©‚b@aœ˜—ï*øe‘ÍR¿o,Ñfz—×ìTÄÅ®µ¢%C‚˜´¸Ç ¥µÁÇU˜ÌYšó4«oR-ÏCkõ—Kî5Ç¬mhÕmê–Î
y:º¿ğí&aIÔ™ºl;]-Ó°@Aw°çÂùü’:W–ÑnÙ¥x¢’Û£Ş¢ãytĞ¥İ „ ›‰È?O»o3dÆÉ]¸Œ¡°-@W€‰d+âc»rƒ!–ƒ£i3øñ²?WÍñˆVæ¨?J Ö¤/†>HæròJ„8İIp¤î/ìŠÏFœL¶³©ÈO›Y…Q¶|çZNëşì{,ø³­“±J—ğ»#úl¼,Ä¯B¨¾ÁÅèü¤?9¿ğn $9ï˜£[™×íÍgÓyÔ8`39Ã`xõ8çyi¬¡Ná)X™*ù9,¨&ZŠÈ„ã06ûµú¬Æ ‰ß¨ »ï¿¤ Şgi@g(ò·-{#R÷Ò¬–Ó¿Ü‚ê­[[ö0±¥Í.òUÔùE‡·ÕWQ¢F”0/ØGeåÔfrôÉ*ŒûzÛ¦X´ÜÀN(W;Wj:ÍÍ®¸å×šÎøÚPÍóÓÂ¸“VmËÂä!.*ÚÏr}…¹FÆX¿ú^×ôš÷ÔÇeàú+óèÜ[VDGÍ@nÜÈ“/-Œ×Wù,‹¯¢Œ[áˆ›İzØæ-
hLõi¢©]&)²V¡mÛİ	ËU‹š»œt}Ü²ôî-İ¯ E¿÷üêúNí©´.ñÃOŸÓ>+çµh6%
‚İ´…,ŠyúÔ<7an¡òrâØµj§vºRn×Àà§UZ*{%GÄ[€”8å‰Òn®½´;İœEnaò$eÌ9wíÏ
j!lgâ)—©æÅî5‘3~<¸Mÿ+¦î>F¥İ±©?bÑå<ar¤OU²‰|„6š7QIšëÚ,RÁ–ü+”g?wGÑ8qQoAaÊe7èYÏd ÍV%7ø\îİŞ.§‚êŞc‹»ÓxÄÚ-GëÏ6™ÛVxÆYqë=i{êb[|™!;‚bÔ´;ò`fLMEÇr"1ÃQå¿°Ù.²Ø7Ä²İ',eÎÌWì_ÙØø—¿ÈÑ”şÔ­lCãjØ¯#*æ-CL’Ê]/Ã<ŸÎ£E¸^<ÍÖ"ZŸöBŠê©w«[ÅÀşÙ2-ŞdájE$T±4ƒA]°Xhği.Ğ²¯.'“ó3n¡íšqŸ™c­¥k…&hè ™0š¾EÇ‡ƒƒIñ1ùÜ{~<<âı–üg>ÿN÷§Çç—£éğìè¼‹ú	moå(f—õ"Œóh%yZÙâµ ıjt‡zVb<M\Gİ˜V>L ‹ı¼ŒOYw™w>ææ+EÔ«‹¸÷„(êdL"BÌ$=;r¸dj{•wÛ=^«­6Ê½é°¬ªàry|<O†çgFÌ/4wŸº3ñV¬–NÌÊ¡6R†Í,ı¿=Î pÓUßBN9ÅüqÀ¢Nt3=ÉÉW·“ğáôø¿çHyF9mÛã¸Ág‚öÃ¯&ùÃyN±J'›OªÛ#¯nx„ƒÁæõZÄ§Wúâœ¹Û"ßJÏ¸ÅgİlØyRÈ6CK,4“a!_+³æ‘C°¾¦ê_™$»I÷õ( Z”nDµÍ2D†dæÂ¬:$TP#ïöpÈ²E1Ñ§Q<løô3·ËÜi™É(Ğô¿T'‘;dŞ$A>C6[2×®Ij( "6ûçñStRÜ# ×qi¨bmèaçİ=ìïš4A¥m:ÈF£n„D¢¶8´©‡™±îTÁÃ©ğ”æpÇúg¥	¯ş¢®åõ4_Ğl@^×–ÏÒ¯óPà|8Ín76hH¨³uy
©*äpbÓƒO§JXö¤ÚbTx?À›ß˜T“)ÃCzæØÀõÓ‹ôÃ›¸¸QfßJt6$5G•™aKáÚ>áàŞæy™>ë’•ÕXl2¤ÎÑZ`³µh#t0Ì›Œæ¢F!c(âK9²ÛàxAT'¦3É™±Úå¾*ÿ¬Zc©j²ğ6b­(y¤¡«}:R7`_^GËÁæLäÖ‹Â¬£|9íO^OGı‹ÁÑ|_üèh„LÉ(‹ÓTD©¡ı}g•Ãèj}M³7Huvèß\'Ù‘UJú
Ãê‘0"~ÚĞaÔk–û…œšĞá°§bìÆö†C‰vmixjê³\0¥úez´ø`¹ŒW46<¼…ÆòŞ$['3XEopvXá4ü­åJ0ÙrNU”ë¢¤HI\¥Ä7ıuG¤”èøtœ…`éMÎGÁå¯“˜n~ìßÓÿïítÕ†¦¶AO›ĞgİöÁxÂfdĞ–>Õô?{^Í¿Äge=ìc”÷w¸$•Şt…X…˜dŒíâç¿X¶‹—>Û4'ÿ.wÿÆ;‡ÉneW_‰áú°z	úVGÀz$,òĞÂÄ$^vşúŸÉ_»Á_ƒ¿îv5 ôBjœğğ@GdåFó³|˜Ğ\;L$ÙU… pK/ñ±f™Ül²„ÁQ™ïï…âî€BD…‹¿	¼kŒÇˆŸ†ï)îs*iÙŠ-º§¥I"=ŞÄ9Õ>UÚ“$1C²±)¢Ş¤å¨Bl7x¾WúX¬67z”nºîêR˜®ôòÈUd1ËÂãGLZ³ÜÉp`Ó Uz§ ‡ÖÄCdd¸ [!=­$ÿüwïŒ¬ºÒg™_Ã&ß¿ıÖFZHµ¿‡ÿ¨#¼–ÅçŞ]É+ÃuH¢)…\Fä,&ğŞâ—ŸMR°ÄÉ÷³èÙ’æÔŒšmuDYØàüZ7l~ T·|:š3$»^ù ®4€Í-4‚À"¯Á: y6Ì˜A¢s®ûèr¦ZRN™Îá¼ÊìÓ1(ßY×ÁGLê¡ xWùëc5Âš•WÜ©(¹00ÖX†&j–!Ğl*‘ê(JXÄ8R÷	EP…‚QÙC?;ÍĞC³äÑÒøCÀ®®E´ªš˜Ì'¼¯@ÁÆñeËà5¤N/q€Z¬X&ŠÏ½&³´ƒM" 1öˆ\„ÀÖ¸ü†äææí7sæQ‚ıYğ&™º7ŒÚ"Sé]¹$ûg™õY:XÌ#/?äÛÎ^ïvõËQç­‰œr¤ÛQã¨Pô{Åœ×ORÂ|n¢¬ƒöfX4.½éÄÔeı“Ñë~Wæ(¿û½½EğS°GşÙín©ñAÿd0}kkéÅ¶[z·Í–¤Õa¢sùÑf6¡`Ìld;ãPµFÔ5£âë‰Âã¥¸"ío@iZC%ÖÌ¦^<HSïÜM-y®uã&ßú*½ŠşC
¬ê}oĞ¦ç—È{6ñÚº”Ë…û?²”^¿İÚZÇ¾*+CŞ—È{9æª]B·r…UmÖ†eCÕH´M~×Y1zkB	¶©[­Ù­>ßä­üÍÀ» l‹–0ª°ƒîŞ~@$¡ûe(ï6…²­Ea“µuàƒ,Dd´Aq"7„Òluc×j•_Õ‚‚«Ò<„ÄıÇ&QÔ­«%FÔ‘bÖUM¢íÄi÷ãîHV‹Š]7LJ£Tg¯ì[bƒY/İ$E°Jó˜Æxx€£M"š÷®â$Ìî™3¹juÎóÒ›l¤~è·«0ã8ÈVšGçõ p,.i6ûÌwÓß•]íÚì8+¨ú—f
›œLßó»q=qIn˜,IÏ¸ùŠµÇc^~¹ˆ#-æŒZ÷’ÜOoÃìız…f§Ë˜G4tö”–ÒÉeAfré(˜o^üÚ¢¦QP/K?ä2ğ‚à")e‘¥$ ŞÛõ Ï
ö®Ô Ô[ ”îşÚïüOšÇ°'WŸ5Šæ'”„‹/!p¤Í*mNÀXuİyi‡µ$OKõ>DW+µ}şN-£ë,¼ÂwzÑ¯	ç^'i„yÔ©İ+îW‘ä¢ûèzÀ™³‹
ÓuÖj®™ş»”ió_“ÎD/\Æ›É,»'ÊÎ\	Ô ßuÄ<cyØ,hÿ¦†%#&é›0¹Æò*ù)Ü›½SÏpÍ½PñW´DEÀ¢6Peê¥=Å¯×w?¦G>´wqÇ´í—U¿áoWwwöÜù}qUƒ/Ú†=\> gæa/cDûÍ*ò–qM¼aM0N€İşµönˆ×‡ñ¬øûŞ?ü{ğ§“"q™nŸüË¿j%¢23@9\:Bº!]Û—`¬b‹rW´äUW$&÷¤òZ¤%Z’»sf äG‹ˆ |å„)Wo”îXåh¨_ºäØS²†Fä#“.š•Éè¥JÚLÂÚ‚‘Û×–áŒ _¹¬AªKc‡·àe³Iìµ¼1{,²œü
©Şğ*TCÆ”oIúôoØx`â øˆR”`Ú™Q…­DÜ™ùƒ{~  º\p/`®]fƒ¾Ê–å¨½VciÕÙú€•6X­­ãŒ—[âgáÈãWQóº@mÑ3G<=t˜½e¢ñÙÁßlÉ{Gà¥<­¼xìYo0Ä¯ºõÀCJÍŞÎı&7ãøvµ¬|ÉÕ±œ÷ZzùwEgr3íıÃlJƒ.İûP8Uß7kcdnA7k‡r·b4£‚ÔŒ|wbK`E<½ßƒ³­@çÎ“ƒe<{_	áì¤dÈ·æ]IqU·¼ˆ¾‘!oYJ»gÄÏãÇ«ùoòkH%È^Áb_`à íÉ‰á¸›4‘6ˆê[è*òù;–D=(új}ß›A´S„0/4·ÊNA[Æ6E©ŞEÍ[¼1Lˆª<CÕ*ñ”ƒYà'œ¥"0‚p8¦_:¬ 7/V`#NÍì[W_~Ìx'Ì•F=ªg¶>ufœÓòŞ˜Sûvw@_M‘=00<¶,E’Û>Òf ÓŸ'›SşøC\Ìn˜êS-Çhù€0É+²E=n«Çd“{Ü/³åã6¨0³Ga]Ü<r£Daeé"^Šù¬KEˆéòÓ4à<Í:‡”ÄiåX\ö_áKÚO_öf•Øî\Ã­Ä9IİTŠÖø¯—1W×ìÎ^«5`Gÿ'ç¦Ú¸íç$úó•lø+WÄ<¾şØy#ş••ã¡_Ã‘t)‘ŸZH¡VW]†½u¶ùsÉVë’ÃÂSd÷5}Gc²ıãu¸*âÙQÍa‘tÔŸ)•è‹¼wr~v<]Æãn`+stÒ?ÏÎ/Óß‡ƒ7Óñ`2×ôşÏ`ŞtgÑŠº2Äô Ø>-ğ wvğÖ/õ”íWrv‡flŠkHpŠßÓx‡Wÿ7Ìı‡ÖPÄg›æekòßKø«·"úU„YÛñÊüH _®A·"`˜ÖTÜŒ#zù'gÊSÿR9ˆ8¢áí€‘o¢ˆEÎSîr—ş,e´¤½“.íQs*§²q8U|òMÔO¥2’«Vu…ù:‹Ø=†Söc¼Šf½Ûğ}$ıîü;}¸G±\°?™'»È'Æ|Õyö#Vpğ¶09y‡Ñ§äE#ëÓL!RPxKæ_¶@	:Ó½Ó0»¹¤m9u:îZµØíeñõMÁêÂ¡¬ãH6 é`Çm#ˆ{ñALÃGãœÚ/	&¾…Âû/È¿?îñ­½{ÁÓo:q>çd’5=‰¡-‡‡ƒééàìrúfx8yÍîí~ó…ä}ÑF6¥>şü¹[÷Ÿ¼ÏdîšM]ĞdîŒWRl™Œ¶`)ıØ¬ÙJîb†svJ®ÎƒÙ¯ÆŸ5n|{?÷iœÒŠ2;¶lü…OãúqÂ1ÿ}›öqä›L#)ˆD7»‰@ÄÅBØøñ¤XÌv<ºI‹‚°«ò¯—ÁüORJÅB¶§y”eQˆŠEã¶`¶j
fr³¾½ÊŸuÍPd>¶‡mdz
€1z¬N½°ôIøb‰f\®5r3ìÍ3|¿–PíVr,§ÏÌ±nğ‘7V–Ïö§A›êÀŒÜS½ãô[Ö£’lS5Ä)m>¢ì¤oïù-‚&k@ÁO3ò÷~”õ ÙMEGŒOŒxªĞ¬Ã[Òuœk®²è®w”¼·Œ\Â×6‡œßXæÔ©|‰ã•_©œ¤”ï-SæK…V\Pº`.“ğ.Œ—4†Méÿ¤NUœ÷“ûßâd~¾àé:»ö²ãh–‰óI<%Z—^ôÄé±€”¯q±Ôj½óWËuf÷]s¢Qcá:Í„ë×-÷O¹ îrÂïWqq®\
i-œ¦IApö`#MjŸÙ?=·zaÿô½ú	ôÖçMµ|ÛŞçdâw"¼§ö°¾3Ç
ä"œÇëÜŒjÄVN\»[lÒî³f JîÚnÂœ.Ğñ*%¬ Ïã«äœÓ¾£Je$Q³;½×µ¼ƒ×V¢;ˆTSeõÍy‰¶y¹ Xæ”şî(Ğ™£şÚAšïêíîvƒï÷¦ßïít(u´v(Ó«aËÎiÁrVº}8©Á2ì6a<, 8ívLrv8ä˜®E­¹(‚ß…çsç¯? ù-Zº%!×ôIeXV?/İ$à‚b; ëö.(ØbUÒ]€^xª×÷˜ê­„†bÃ³Ë j²ÜûŒ¬– ÈÏ—DTd”g’—z½O%÷¸ƒ3upØä3¯©­ñà“àÙNÉ=ğeUÅ’R¯áÏjŒ°ìò}ÇU`xÌ{
åÕé•5CÎ9˜ÎË}˜³`ŞÁâÃ|ºÈÒ[üÚ’zON”u5@3öÁõ§tÌÊÒ«làmâ,˜,{)Ççïz9$ô£NWQäÂ…ÁÔë÷ ÷p£“€(RÕ˜d–V·şl–£ÏœóOTh4)ú\{¸Z#2¶è{u=±›FªUk3æÓd’0â&# >"pO£ë²Æ­ØµšÁ)åëï½éî‡Õâ_zİ5ê4K¾Õyæˆ~˜N¥<\¿=éğq…Û™€í˜%E§É¡’ë7{/
¯é/qâ©Ÿ/ïqÕÓ–‡æ\ÌÛ—ƒ.›j•æİiÀßdXEÃuî'òÂ?øêĞ¼3³"72CÑÄ% º·ˆ³¼˜²LôÅ2ä¿ks"…ZëÎAe#÷)--™ü*NÇé·YwãÇzO|¤Æiİ*ŞAÑÔ
Óõj£µ*LR“ş9`cj·Û“ÑK«‡G"ÇmÌí†3µÅ9¯‰)#?µñeš5©Åš‘íqIõâß«u¼E<¿Â/òïÁÎn/\­¢d9ˆ2ó¸Eº·Yhêa]_ÕşÇš×¿‘æYø:Ôä¹ˆ˜gyişuœg´ô-M@ÅnÀBÑáGóŞxÔ?›Şœ\‡¿ª¿š.ĞüÊÈ‚¢?Ö'É<úø÷½xk$5vù±ZÏÖ·WdºOÃâ¦w~ìì[Â³ñ¾IQÒ*Æ‹8*©	/¶$>Ãt‘á’£¡ëöx3TÄI‡5õP‰ŒñÚ‰¦Á“Û%DaĞmİÒ}¡ÔÎY-‘‹ˆ#vk/YÏêh]ƒ>Ù•wk×¬¶ïLİÅé:oÚ©¯,æ+‹ù¢XŒOœ%†ÁªŒËQaL•¥x¢Î*”‡UÚ¶i)=Ô/ƒ8ñ·“w£Áô”nwŞa¶Î¹×Ùù·œÌé¿AÆ_¬E8^Yçñîµ¥Ü+q•bYK‰•[lt~rÒÜçÒ¹ë"1ŒR·Îd­¬àr|Q¬_ít+eç'	³GÿGè1§n	jæ\MP—GÑaÙ¾¯eµ¼9Z3__±måıŞ–ïûr•š¹òê¼³8ùBíZ%oKD1IÓÆD•ÜDÁÀ> Qé<ejöWŠÀŞ>À½f/¾‹ÄXñÔ¸¯áê4Ö.v}«U<÷¥M¬c³^EµÚÜ5z´C dÊeM‡CQCêlA™8U¶¡HOjô£ø¾½iè¬	päkûªwêâÃÃ òÁrß|ÓA75×¬ù	÷¨q³Ä%`_Éü+™?*™;ÔâÖJKû^iuá…n†‡aJV…pNa‹ûet±Nrl~ºAG‚/ã©¬
ßY,…ñäİÉ`:OÁ"“‹áoÍôõi¶e"i;³ešĞs	a¨ÖƒN‰¿¯,[rïx2;ßúûê÷³,¼‡{ù…áê}šåLYæ—V`ùÃAf¨™ßs{WF1wãAº–Œ3ò¤G‡+Î¢ŸO$³H §‘¦F7('V}Ç™ÓMkJMWyº,9P]hã'ı³ñI2<?ƒ”r{´åïĞëv»u±Z×°ÔÊòÓò¤ÇÄy^Û´ö¼1Ö‚«õëÏ¶(‰Íçş' $Ñ@ãQ­ÔfİD_kY8à¸İÎõÌ“yğà®¸;J6cLx(kÌVÀÉ#u–íÎ7×dÊ`ÛPMŠÑÆİ†Ûµ’F–U÷-Ÿ¿/‚Ÿ‚ï(½ÖtLg¸ÃÆ"ÍfÑxà$=¿! ûyÚ/Á÷Ş;Ë³í‰½ÿî™“ã{İ|¶³f·ÇmLÖè–ê=‚ï`ö½¶ë)ºÜé¡|`¿D‡Öô¸—±¨ıúÎzÌ{«D<¹ÄáÒ;ŸúÖ4ÔêÚI0ÄıÆCtòš­M6ç‰Õ–¯™où>ŒÔ­øT-à¿#Ø–6dòïÉ&ÚlË²2ÏìAŸl÷ÃÑ÷%l€­{şìV2ú,¶AA“îmĞº¤kx³ë²¯GÄìmX²«}_8~ì—|š0`3<ÊyZoîkÚ‹¶Ä|UÀÃ{ËÁoÈyËœmŸŒ÷¢ØûXoÛ>œW'¢Ï‚ïVéæ¼¶åÜ„ñV,Ä—í"{Ûr]¥ñyî«pö~½*c\ZÁÃÜø@Añ‘¥{ú$E0Qkdx€…Xc†<y)Åsq	¹›Îüğ¸Rz¹Át/Ü¯à»ñÎäaSH¿ÔûÚZÂ´ÈÄ¶¿Ec.ÒĞ#Ø[ôOÓ·U»ÿû1Û}çÛîöökXG°«Û[-§ñèí¾óm·å9N‹4âqĞo;ÅÇÍÅç(ãŒWûnËSçNÖdø¶}ÌvV£‚¾Å‹¾õí!«FL š	p¹Å³ÛÏ„;´ßh,‡í«
Õ€JaÀŠ0wÔ8ºñåŸ·ÓAàñ¾¾ÒHF20ğÀ@ç Ë<÷Ğe(È7Tk`8Ï·¥SzÎc#{7î§«’‚HKâe®
÷p¸ğüŸó»(ËˆF`çµë«e<î2ƒ4){pÀRÿŠFùğÁÚKlÆ¼®j)ßÉöUË)ŠlƒdŞ•ü¾¹uÇOmÚ*;äƒtë’±³úÉˆÔÓ¦ú†îr~ğh”RVé vÖWñìUôÏ8Êä÷½A<˜_N¦ÿq9<›lÜæá:c·ÎöÜ¾	°0ØãtŞÛ÷wŞÓm*}çªùÍNÖ}¸»xÜ\õ	İú ¼NKt\LXŞşÈ|Œ‘:°V,Ã|r+5º-ì«¾H¨ßXŒÎms£ÑŸ¸‹x6u<Ğ¿)ÏœD<°aOë14çâÙÌ^=­ÑšCˆG†óø#Ù@.mŞ¨-±±_§z«ex?I¯£â†ğ¡ŠÁn(aæ%¼y«¶öÆ¼ÑêFgãØ‰»Hl{/-ráÓ§ˆ°œl[BÆ;îJ×àIyÏšàuÏ¥Á5½ílÓŸ@ßÛÎ­„
/7ê2²(]ü¢‚©ø$;öæV5W3<œ1İÌJ=‘}ÖøH¶¶Ÿí\6½9êÔa”a˜ñUnğ¬ÍÁ<›J®ôè ¡ğ¹i›nÊjÓ£-ˆ¿şò—o7—gİ½ò=Xó£R·ßtã…Óê$«Ê‚™½­÷E¨ò®5½­ŒgoãÙk?iªõ,‰šÚ1İ*L²xÙã¶ °c_s¨åyps`t˜êoÓ£MÆ¸µ{-U÷4.X úŠMZ5P•¤7­ l†²z>ïy^õÎÂe†ş9	½à=Ìˆug¼+íwë³mk5;ÈF«P®Qí÷×Ê‹nnîùY;6pu²;üû¸İs7{çl,Jƒ%M&{Ö­6V±Öáõ5•Å7!•·sk£õnÁ"áà¢Bá&ÁœP6±k´@OmÒ¢ÏÒì³äxóu'àğ45æºÛl 5>}êit0-5í­Ø6£Iø,tHtHF	¹_.Ø8pP›Kréuº&ºıÛ÷òÚ,àQã;ÛdÈN`ïñº­IÎ-y;
ãÓ§Ş?ïAyBôàFº¢’2´æi0-èo%_«3S5œ£Úh>xÑ2M·u%l‹Ê×ÆJ—¬Qâ†q3ÜÆ&¸ÍoN ^gO	fŸìöÈm 
o¬o¬oîJëb3Î'4ÇM÷¶ë“Ü˜õí$N"±ÁuXâŸšhÍ¥+)‚UšÇ0²ş öæ~ä½«8	³ûqf³$æ¹%ãÅ¼K@Ü®ÂŒ*ÃYteytÍi¾E;PÆ£7ÖË!!Ò.œ„’ÿDwµñU¹*†å7­•Q¤>¸D«nD¶õÊj¤ëítÌ-jÛ’ÙÓe¹e8)ºrc(ey¨^[HxÍÉ÷²‹+‡Áw´B¯ù”ÄÉµŠ“^C>_ü‰pçÓÛü²/³›pP«u¡ÂpfÉ©œ¥Áìte¿ñ¼A8UR˜:šª½ÆËÒôèF`ŞÎn¢¢CŠ-SÖ{`eefD“Täîay<¤Ÿ½xNˆ…°°¸ŠeèÚªı‹ùÏ	S­ãvÍŞZ`¾GdôdrA
×ÿVY|Gè!˜-Ã<hzCRJ~ãøvµ¬ÎFÈ¿QB¯½®çî14&Ü>˜±õ®çëÁ¸ø(÷¸j„÷W)Ê„ŒåÍp½CÔ_'Wr7ŠÚwármœT7qŞSàÒbxwXŞµÒŠ8‹Šu–èğQ¸¦3Ò*KBddi‹Æfd'-¢z¯½;Û §7*JgQ¾^ß Hî)•YE•iÎ×
	d«¾V¸:5*<á¸=„¹ÁÏÊrBƒ|à<i»“¢ì<ı3ã|c±a†ÛR•æ›a¾fı‰i£nrQK3Œºì•èvD_şl¯û*-~²x³í‚—ddJ äæ|$j^]O'ç£é¨68«GòâY™ÒÕ(&§ÅşQ‘Èf{šÎ©ğüâôüp0_ÆàÔ?†İ¶àUÚŒÍ«::áß„ÙÏ³he}*‡åßeÊ$-õğYZ”ØÊÇQQP~ÉYv6&^Fvõv™&×Á<†}·Ü~V±Á ’Yv¿"Ø:päúØÒ´ÙĞnãÒG¿¤ëòs“-âkÿ`“*ğv8'ÔLğ
/ÌÅ¤pœŸ°=Z|…,ál¡£ågüåaÌY‰îA[=^bR	ÃŠZTc5„wİÅ9‹Ø!æ€¨ş;ŸWa6%…ï ³€W×Exôİ0‚÷’Ğdøüïçèµ9jäPMÚv1±™WlÖãR°ÔFÎ2¥QÒ6›{ÅŠ(MæP´l“©Iî&å†.¢Uš~-e´lë¦^-ÓÙ{¿–® hë†úó¹_3DXjßÑã;Ï9
YaŞZ)K6mó8JıÚ[råA]•§kı&.núÀbLĞ<I‹™æÿ@ªL[¢¹—Êf´“Rr‘›á2³ß@ªæxxëˆ€©(íÂVY»`«`¯C™FZ§¸1ç“ô8K×+¿aÄÉ]\è‹´jeÏbY4È–½Îi[Òïr•JlUÎŞÒ‘–2¡¦rµ2rH»ÁU–E¼$Yáé¼ŸæôÃ.Q[Tæ>9™^¥EŞ»R%Öšú®ìoÅ¡­=ä¿tAŞûpŠ´ ’s‰Yª`Á¨;kØ“E*£é	Œ²TŞàÇ8Z.dĞıPT…‘Nïä>	X¿:ÊüDÉPoZ¼sÁ´—ù‰‹ú”P1„ùäu*WdEW=M“ô(ÍÖ·ª¶Jú&›…Éi˜¡É(Í%£®¬Ü;l™nU©è
RùÿK¦oëahAõ-‘Yçôuç;‰aBYjPQÇ¸ÔBUÚš<çôM/IWa<ééòiôq­Ä½ónŒ„SY™Ï‰F<Uäë=ùÓTnH§	ÅÑ­K“©JQ[îú
şÃêèddqŞEÓEih(ÅíÆC§té1:ÍT…%”œ]7ëìÃõÕœÒ/èëq¸rMFUJ3pâ#òí°mj–Íãó3İ¬ií8èàèy«…ĞÃ-\êŸ”$JÆÂ³€MB¢vâÖ}VÄd“¼ŸéØÌGË0NÌ´Ü,I¯™û0Î;ºÉ®×ˆºê	‚ÏÙ§à¨Së]÷æ‡0Æy!¦éÎÒ$jĞU/ã´Ö¯OJäVµ‹3bM8I5ÑtŞ|M+îÄ¢1Rxã#Âä„rsÏ¹yÀù±Ç¹Œ
÷R‘Õ šoÊ_ÂFÚ.İm*å¶JÛµQY‹«-U²IÉN[´oDû”TÚÅL',G«‹–¨åR"®hCÆÁÕÚGâd²|/\­–÷ú±„9@CÊÅÂ›~Ó¤ "e“+r$•²-°Ç›d~W-"•1Œr1ÙŸFYtGø3Ù1¶$ßJ¬¡z‰óhN÷]éíM<ŸG‰Mæfò®CŞîJ-XÔÒnÊ—‚cİtçdT·1™™]éèS6Sê?\iQ™Óƒ_âtÉø¢cšI’ígâ+¤º„­‡½¥ÅaÀå{ë5E½û'cÖ¦V[GÑ³(§DÍ98Œ0)SµhJ'µdßïÒ}¨ìD3#×(K‰>âôˆL¸¾ğÍVVöAÉ$¤7n§[İ¤Ejšwk^üOÒB¥©†î—¹èÎDº«§E<—Í©Tñ78¦E_³E#¤-¯„ #\2~ó-yO>Wi’§Yy(, ılì¾2Á]¢q+U—´0œ0Ôó_¦™ÈqÊ +9Z·±Ä}aÕQF!Œ8êØaåÚ?âañ»&ÅWwEñg†”îÇçô`´<äÂNEÓĞ4ıÎÜíš—~X¥âÔœ[ÖQ¼Oìó!Åúu%}wiƒr9ê€„j(°Äğ“ÕÔ‚nUN	Ê–ª	ÙbÊM‹Å’(”R_vÅÃ¾ä,Ô">Û'§P‹¸æG+ÚãÇ® "6è²±à]7Š™C@¨©…HáõõÀ(T|tÿ6ÌŞ—ÌµOxX8ï üQgKğ”ªÁÍ¢‹qên Îu¿8îeaì½\äØ’ÏEt%ë„SBÒıyªAI‘Şo¢¨øû?"ÿr¦Úâ6ÿd
„ù×ÁèQb "KÑáçºË¯<¬4²0dïï‚¿BºÎf¡‹»xeİ µ£kc”9 ôrñBQÈ	w&$M=&Ãe£j—ÙR1Õf·©Iª³sS«ü§§OßànC$^÷,è-ãä=ø,Äkãº‚iæœçò †±½ÒŞmø>ˆWx“wê.¥ö©»ë«0n£¥~èÑr)ë2ñ0J¡
”I05w»Dkô–)ı:Š¯oŠÎÌÿÇVoB¯fÎ‡³”Šğº.^^ôæYøtˆ¸ş…YFdo ãÛwSbüÚåşt¦­.Î/&'àü„g0Ú[a7°V´TbÎ‚®Æ:jÎ“ƒeÌ< i<¬»}X½)Ó@%B©‹ F(2Z:vŸôŸúæ¼–’YœDaÖQ¾œö'¯‰0~18›h•Ş\ôGÓƒó³	ù´[G®J/Ú¥«õêMœÌÓ½ãpE¨4jeËŞÿXÛËÊºñKs§rÜQÕ=à:V¨³)#£šAH+¯Ó¼è¯‹›4Sn|‚¨‡ãağ”N|…øƒ!ª\kòDzÜ6-dÿ>ò÷AºL³Îä&º¨G”ôó}tÏÎø¹jG¦ÿı0Aˆ¥E³cP'÷«hş;8ŒöÎOG'ƒ·ÓË³ádz8uƒı:§ƒüãû?Z2ìïÙ>8j4ìØiø‘ùK¢©ö¼à]fq°&ÿ½„¿z«0Ë£zÚl>-W„íƒRÏO’`1À­Éi»ˆÃÃ³i‘^&ñŒzä“X78å¡rLÃ¶aºÎÈ™sÚcç"œ#¸v’f‡|wb‡+¤È\'U¦Î0™\ÔbIèeéRğì'M¢ï(‘[”ŸECùI{ó8¿óÜ7p:A]Ù2B„ª¥ ã¸‰ ?‚lÒU”²Ô¶ô¬ÉRü@ø?Õ%— ‡#JA¸Zõ—Ëôƒ_ Dx|âÒªd¶€‡&•ºø':Hƒ?:II/WWi˜¡ô NP“èÅ¯HG£"NBX¨÷½tÑ¯zâ.³€Ø®îsÈ&ÄYÆAºº‡i @%Zï2ğC,n'8³ë• =šR½#¥6îÃfÒ‘¬ñ}’H(¤ÙÃh®—à4sKt.ĞŠ¾Š$ŸB$1d‡Zjû*´âô±eŠ÷Ï„/—<ÙĞì»c	«8šïn›…®·Æ}åÍ_yóÃòf•î>+.ıÃ—Äk5<~åºj‰OÃuiFıYƒ˜Eƒ¿UÍ0_…Ÿƒo¿}f‹rV^œ¥¸à{ÆõğÉ‘-£,¼å7Ik<zçíÒ·¦ìœºˆk8ı<æç\m¸:<Ì†Ìºİ;‘+öÏËv–gï$.´•Ş=H!í{ªÇ²q½D]—ƒó±Ô‡ÚÄŒE2Ùş!Bû^Òîu´“"~¹Œ^VĞ¹‰Î*ùoæ™vÅsJ­CÕ‘KL¨»³´;ÇáÊÅ¢ìBC/O>CÇÂ:İŸïºÏÄ+ğ8æ(ÀÏ¨K‘O¼ñ…Âz'ŸeS|
üwlÔºWÀJ?$[ğ5xp8ğêø†“	Í{Šbö5áŸ+^ŠÃÏ+5f¨óLã¤…ğ4º"ÂL¹éÖc{zÂá…‘U4o$CôçÌat·Aƒ¾¢†ÍÃ”L'Zû É;·t‹ù¼ÒÕ‘âÇ*­6îAÃ¿s—g²A_5¨è.§ÃËÓéÑ ?¹¼Lû‡cqİ²-;v¯?²£g!¬õÿ·w1<~=q‹tÌ?ë=€Z.6Ë¢Ùrh½ükÉ~3r¯%óODŞH<Oo½æˆkw Å.‚<ş%Û…t“Ö]×ÁñÒ5>Ğ,ìÄxŞº})õrÚ…6wb¯KkZP.ğ²¹([¶õM+†vMö±&rù~û»V‹1Á ìAôÔ1iqy,CRK±Eè¨–™´j8l:º¦/¶5”¡¶æ0ÛU}À[•½¤»ªÛ‚¾©İê*BKP!î²ÛÅ¯ Hæ…òrYÓk
Æí)bE¬‚oáîíøEûí,f¾âÁˆØà+¦UcèU¸Zi¹ıñ«ë¦”4Bé‘ÄõP‰¡¢u°³ÖnHN\q>¸] „.1dÇ¼¤@!£Âo)*½.í÷¦-ÙùQ˜¸x08›.¦¿.&Ãƒ¾~Ø	ÊiXÁÌº¨ç»¸÷¦µ%‹éÙßî¼ŒóB²7?ÃP¦1a A;ÅÜVÛXl^üèÑ6 7‹™c¢¥ı÷‚r]­¯éÅw˜x@lŸ€Æ*¢&ö ¤¾&äqtôµ«™|s“ÍiI~CåF4¼ÁA‘¦á!k¨Cô–@~ ³T¸8.ƒÀì-ÀÉºjK]4¼S×Æ¤ãAe9<"Ñ?jØo ½] ÃwÇUØc¤Â.…¸ã\ñ1¿²A÷Ñy
~„ÚĞôQÙô•Fãƒ´vOşŞ´ƒM„f[ÔdKö¬é¤|w°‚™†´Ø †"ó©7QOá!­-•ñÀ!ˆÏéW D_{6ğáµ–ÕÑÆ­;õ»&Õye½¬ „Òö:k÷ZÙ/wyp!Ÿ¶ƒÿì}Üÿ÷}PÏOl% >ì0QR”e=,"…rG›(oË{*o®±°¢‚Š:ÇŠöÔ$ditO(«áA©(K_äÑr¿ø:D%?çê#m²Ù¤áîÄ¦u¦š7±Ê?ãY×{öåyÀ$¸„@›dÖi@•ewİ”]&<ö¥ÍæàDÏ›FYÂ¬E½a¬·5ø~I7@ÔgéA¸Bi v`ğ<}J„àyjıÎ’Ë_…y$Â˜[8è(Œˆos…ù:‹šÄ¢ªtDe5("lï¸Î!²iÓÒ¨å2^Qw‰Ë’hDUÊf»5˜ÁT»8'rÈ=Hæ½Áwš¿CõÀ^Ğü–rç&Ù:™E¾è	å50-û²Gº½&k1#³Âşÿyşf|yƒMƒ¢÷ô^T·¸lÊx+£›4‰ì¼ÉAh˜»Ê6¨‚LÑC¥ÆQvÏØ
§;C8y"bÑª3S'IÕ±èaSB†Çu
?9İ„¨ñú[¦KŞˆj°¬¿{i69õÈğ’páùmC[ğ=û‚U`|§¶:›‚U…³ƒšPbs ÇNŸßf°%ôˆ¼LviS$/[ê¥”_M‹ğw÷³õr3Uu“›L×v¡ÉGß¦3ìÚ,<¢Ï¯Bíÿ  ÿÿ OÂ5õxœì}ıwÛ6–èïûWĞ9ïíPEµÓvv'mÒ•e9ÑméIrÒ¾n-Ò67©!)'mÿ÷‡{ø$AÙiÓ7åéI-ÀÅÅıÆ]Ôë}ó/á	Âp™²´Öeq¼+Ë,Q¹nü/»UyñUœÄå½ÿ6>ŞN“ã³±¥‘<Úfy¹Ø¡ÓècéŸeë ‰ ù<K’(ÜDå¢ÌãôÆŸ
üc0¯*_d£`[˜÷‹%Eäı±÷øÚóÃğ^zé.Iz–jFÿzzaú/VÔIqç‘&r˜†gQpÙ'ÔºşfŸ  q8È®ş;Z—«2¸éÓFºaÔ(K²Ü'ƒ_ŞF›(¤?ñïÁûè~U’"«ygIh6-MÚ[D	%ËOòàCp³i=“Îƒõû›<Û¥¡ßë{n½{ÿê~<úë5>}¯ÌwQ—±’ù²4lØÿ"ÿ’~²İÁ|RF›¦õ“K:î…º¡oè·Ì¶³ ’³à>Û•¶~åR´Ïèö›Dşú6(ßÅåí0ÜÄ)  |ì{q1?ô½ 7A‰ó£@³+¢ü8ÉÖï£ĞûùgÏ7—F(`"7Ò„ô x=+Ş¿ş«a·-špËŞ\O¨â6ûà½ô®B­£„B£¬(½WŞ!ô	¿Ç›ì¿ãE”»‚	¾ 1P{Zç™Ë%[_Yo¶´° ø@æÃ4!U•»<µá<Q ÌÎ›8-yD0!§ûÌ ™ì¢ò’ ¯MÓ­~Õ&ü€M8?ßÈ„hà?lIzxLtê¶€Ø0ì§Ë2NŠA\Œ7[Ò´İz=©	ÓÚ®“¬ˆê“§3Å€§™ pƒ’Sã·a“Ûù£_qpÎ£¢n4f«¡õEüÈ_Şo£ğmì¢Áhz>;¿º¼˜,W'“Yß;úrğåµ[ƒgq‘ßk8ãÃ­DN#¥&ùœ¦ptQVàx'!Yà2+ƒà!Ä"%[Ó\Ì´¡ö"ğ\ÇixË³ùl4ğ
ú¿—f2¤e´ùÂëeÛ(]ğ&’?{eá9fy´‰wò¿;‚:ÇÙ"›Åm•¤cø—N„µ”_ŞÆEß[ïò<JËázMÎz‚e°]ğ¼ŸGE¶Ë×Ñ,Ïîbœ;2yF@’,½ñÂl½Û†Vqh>òq®é¸â”ü‘®£ìšMßòl%`D·h*ïáL™[¢=š!‚G VlĞ:Æ–»fÌëº„}İu`BUû¥ö}¸ğ :,p‘V'Š„Æ2Àdî¶“!9x Æk8o•O@¿à4 ÁÀÆåi @ë¿Ä=Ç9ÜÁâC°5~ñBşí±cu€í{@¦şrH•¹úh8z3^-˜WÃ³ñ|¹šÍÇo'ãw«³áüµMÀ‚–âè«ş8¸M•È©9M‡%94oa©"Â¯¤7ÑY\”QJ¶0Ï§¦BMXúÓ»(Ï	9°–Øî®’xíİeÃ²:£=Eä8§aöÁÿ‹,½ âŠk~×Ô<|Üƒ Ri¸ø˜%Ó}?‰(à§y¶y èa´è¿XjT,ñ…MÜ§œ²RÓ|OM<_"caî†–ÏØÔ-ã2‰&t‹¶ÙêdNâ Én|¬nŞôW¶c½õD'rN–8º¸Œ£‚ğĞÛ$XGR&‰@¦ì¦G¸$çŸ0=À™<'»»>•MÔG²7f¸ı'ŸZ8cg(Òeà<ô"päĞ‰¢BÆ!@zÂ°`úìL‡¡P²½*ØqHØšCÒòŞØG›¬Î‡B»kÑ«HDfÒ+àÁ†´Í¾‘»$™ä¿l8È‚¬‹Ij?Q ³Á5ìwMìÕ+ Tê{‡ŒA¥7å-(Q¢p°˜/VãïGg—‹ÉÛqı—ÓÔ©¨–Äé{¦¹ ş2J¢›<Ø06Ñ4ĞC58ègDèä{ìŞØ–®™âÊÔ[T‰+ù§‰¾Ö;rïRiıÅLİÛmC‚cpãYY	Âb”6r¬ÅnK¦^m&4mş„Ì÷eJè™üµ¡œ®›©½AÄ•{şÁğ€”ÃŞ€œ†ÑÇéµÿäı¯ÂDÏ`‡ÇŞ«rƒM2bíÇ}ÒÍSï+¶iÜˆH#\Ï.˜K'°*Itl	Š†Öj˜çÙ‡Â‡ş¸pO˜É¿ö¾ğˆ¼L9ËÁ_ş¯Õ­)÷¬ªÁL£C9meÿC$ôFÖŸİy%+šçbì¨ˆ‹ó,ÍN³|·á§|E¡Pl¤çAJ[­t%ƒŠµmË$ŒUy„äX²ìi(ãÖ—“Jç8u¼s›0ÅÅ.ëşÍÓX#ß®¶E„GC±'ÿ•şWjBwÛxP	…(` Yøñ'ñ`Õ¾ı¤7­A[§i‰lx—÷/_8µë,'ºzD1Ôj±±©9!wÑi-n	×Ö×8|}l"Ÿ­ÙBÚaH;»Øm®É·NßûSÿO=R¼Iñ‚0ÈÇê„é Tî1¶#œGwQº«àé×ÈÑ'2~³÷Ç‘Dkİ³IÓ,>CbYŒ@»å(¾…çè6Z¿¿Ê>ÂßÇA‚h”gDúm%uV­&Y<'Zš±áÁ–BFBôg†ı¢p%Ş‹Ö’.X×k_y0[|çÑáÌ’]$tP£l³	ü'*óèz—†O’’ã¹çù@mz¼±£óò…½óM+ÈĞ7àj—]P ~9ÙŠÆ} ³AÚ³"¶%«*­…mü“‰r"0øëué¢‰-şâüôãáO¨
Ïƒ´Hg÷U\*˜Jï?5—É6°Ã¯ól·œùMœR›Á, l{á%[P6éÙä!±zp&[r]—´583·şó¯	'äÇÅ‚Lñ9¬9m	Ú~ùœ`Ü¡:R?on÷o ÁäPŸ–¢áSĞËwœ•o£<¾×Ag©¬Õÿ­MØñş°ã}Nv<Áâüy™	÷Ö
êJ6ëD-†®=²ÒUõï¡VB1ü¨³è]Š¼½g
ûj‡9ŒŠuoáRÿóD=OŒGIãFG•ÅK‡ÿÅÏ«Š#àş+Çñ?Èºş–'•¤;4‚Ú­yâTpû8xiÒª–Ö ®€™

ÎÈÁB6|…h¬ÇmßÁ,¢îÎRAŒìàCPœépİkàeùÜ¥ O`d²ø6æTdÛì(ÆİVZ(C+†ƒÊ6…Ôuü‘Ã=a›Ğj°¶®óàô¸ò-ºu±,‘«ş¾#,íÂ¤EQîª¿L›@ÁÉçÀWŞ0µóMº4¸K§éå„j,=Ú(lå-æ—ô.HâÚKKUˆüØ‚iÒê:\òaxOˆ¤6Ë¿hÂŠÓó .¢E”Y®ã3œÔ\3&Âz." ·± á¢e¼~åÅø#9­B ¦ê2GaXd¾lía’d áÎËy´ÎòPS[lº š‹ĞÈºB2Êğm×B¥Q+ø¨€·Û„¦gY@ø‰Á†œñ˜ªë€ˆ™áECô|¼B¦5 ÜmÁ·iÍªÚK¹°Â¼0Y$Á½Ş0ûàÖVùúò8•9jåSk{_ÅCØHKaiÅÍ$[~DKO›¸(FÍ˜	Û¸¹Ğµh"âŞÑ­M+Â¼*eµÓSşŒ¬ämVfœP>›(Œæµ6 ÄÓ4Ğ¸ƒÖşnÉ¬°î®Û*y®I3µp™§ï·¹g—”ù¨gaDP>K¢ õî@Zgv¦•§å¾‘×ª6Q‹ëìÂ8I®MÎáÔÛ
ªŸBÕÿ-œ ×˜IE ^\2Çª˜Ç}³±=6şn°½ÍÒhµ&P«à.ˆ“Àâ?†
]>¢aÁÜ3bqvôƒI›÷ k\Š‹ÙÜ	€¿{q2É
P“5Ë’*Å Óéz½ÛŞSŞı8Èı•,õ$i‡şx%€œ”->ÍA,}x7ÎV£éÅr|±ì{Ò§óárôf5ÎñÓë<@ûÉr:ó~®~OÉ7yi‰¼ó5Ê;(i~ı¼/K>¿èÄËÆÉ“Æ%È«DçŠÀ âN.tRdŠİÙATı€€­ G@ Ê¾Ğ–€“ ĞÁ©g¹pötaŠÌãU°U*0Ú„hEPµÜ€	B„¸Éö60Ä'+.d2	°€®{”†’¶Y¦!´»Š×ÇÑ?â(ßNÆ§ÃË³%hñ®UÙ‚k€œl!ëæã kğï¤¬PåàXa÷ÿ¬¾ĞÓY6¤ô6.­-Ğ§.EÙ˜ÎªO0±Ã>K{›î–:"¶ÆU¥öìN‰rYbG“0Š6²¼<œL£Û8	GğQå®³mI^€†~ò¿oi+äÏ§OMc«<tlıK?0D²DÍæF7ìDp±öŸë"»p[7íûZt-Ô‡8Ç(õÖR	!bt8ÄÀ0No[àØ«ûÿôC*ó>¦ïäá°WTYÑP	SÚFŠ’¯TfC™Cx;‡€Â·„ÌtáR|8§ˆ±ÏÙÜĞ±ZŞû¶ÁÃÇÖ‰ÙËE|nƒ‚H¼º4úÏòX#Vë$ûRºç3ı`C˜$<qÀUÀºSN´L µø‚îü‘p|,¬•/—ËéÅêœ0#“éE_›©v ­_l¬ó&Èß³>Ø÷a1­Gû0U²ô3¡Zµó?	óMù£Êí§nŞYÁÒrB¡è
½'İ‚ÇJ±Toâ›Ûá‘.çîÙ7„¸Ñ¦„G:€Å çí°Êñ]×¤î‘Ç¢:C‡~^D„³.)§î‹ŠaQóÉüSšICQ´‚¡ş/ôk¶ñ Ø'¾µ³2?¼M 	œsÃPåÁàlc07±¥oB+(u­±vÑ‚î–F¢.b”»bB¯H¿(Ö‡KßŠÙ´<Ç¤ÿ¨ŒÓÁ	ø7ƒòôŠ½á/¾iSI€!­Øm"_×>ÔŸêîQ/» ÃM²Ëµ­0’Kë-ãp‹û˜CF«àåyœ$±d;ƒ¹OçÉÈ€ëN”V®4T~¤ºZ(NoŞ˜å÷~Mòl‹RU¡ê¬ ÿ¥¦Uù“–NêÕCÇ´=<IX‚­˜O¯*?£Æ	V=}C&»Š5¢ šÕów5L»‡ÇJ5¢¦”¢Zp,Êga‰š‚Õaøß»Â+Á$.´Ó¢èD'èı—Ù8È¹ò¦^ÅBÙ: èÄÌúË6İ¤	ãD šÄa w"àHÄ¾ÚÚArlc±¬êç¡„*1ĞbÌá Œ€`	1èát}!Wğ®nL8ˆFØ ÀGöt! K5q›†xVø<(o4!ÇF#È•†¼ÉÀë0†æçÁG
0–ò{F0¢ë4¸-ƒ+4£S7xSÀ+\ªYVÄp>§„+<·5…¨5I·;\àB2#àkJİßDÀux
&ü¿’ÿı[Owë¼:æö¥gr|Ô=’~Q(	pÃJg’-wkdïøXú¦û <h V=‚b—G¡0|é;ßï¡ßú4€_gæ!¸I¢‘”¢<`Â¸fuf”ß§ …1”‡Í™AÛ¥Sv¢ÂùèóíXmWS·ü[íWB=#B:³œæ&	Š¢şÍUF§¤¹¹7âê_á¼¼Í.)cº—¼ë„lÙ«h¦½ªsîÃïÅ…—f%!KqB†q]1¶æË,Ì^€ñş;c	¦!öMƒd¢•¹Š%I–W1ùÀl¬ªÇÏ¿Ã(ğö}ùh¶7>Â˜ğÖÓl(f×TinÕÇíÌe)–’{1à°¨¨À¯Í9t„"œ‡‚ “ß°i_ê€ºhåLÏ¢Ğ÷ôwÌm£ï=ùúpõõá“¾çÔäòv·¹êĞîêê	ç¦ê²@Åß#G':ö’`¨Í¹Ä$X'[êh|Ë¼Ë:<©.{÷Òô¶€©ârúLêQ•ÈÍğ0ßéÖu5¶ï^vZY¡¾²´í-k+¦‹+¼V×0}]¦èMP "4Î^Ù3EÁé’×KƒæŒLÅEVV†²æ¸zÂşF!å'a-vô+T7Fô´éƒĞÂV”oE£ºÜ´ñùôd¼ZŒŞŒO.ÏÆ'šz
MèÜµÌ€S`Ì§¦=—‹]¦ úá…1ûx.k^ml ‹Æ¾*O™{ÊÂ˜Õ0üÜ¦>jGqTÎÄÁd=ûëáa£r”ÁAİÔ)¾!ûµ§¶ß †ª nˆ ½÷ ?:t†ü™ÆÜÁ©¦fÚN ,ãè¢GDüÁFLæŞ(oÜİß‹"[lKTÖÓE½b{óTwNT°Ïaï³™h—‘m“áJÖ<lb3&Ñ[nÒ¾ûŒËbØÓÍùÆU-N1ÜRï.­ú~š
ÒQ-vFö
8yŸAAÈT\®>˜‡QIğt›hŠˆ)Ğîû“ cöÍÚc£í‘°±º[h°%üa¶Á¥ûşJ_ZüN%Ó÷lºhIe*<èe:ÔÖ¢V\,ĞËí4’;!÷¸W$'ü8ç£7Xÿ8(ÀŸyşÁmP`-¦|qö›À.O³5,¯æù¦+iæ§á °Ñ½£LMc“–¼&ceõ¥ĞÊK´ÃØÊé·øıÚü{A)WµåJgÖ\¬
L&ãô/Í|”zæ?ĞÀ¨Y}_O’©Ú\¦èQ)e0±F¥mÍÌïM«§µfj©K+U¯èĞaE$ ÇY‰¡¯iXèV Öèèg_Ç7HÁ[ÂHŞEyIõÖª¡hK¦›Ì&ØaÙYmJŠ£—ÒŒÃÒkOoK(Ù:b+m/#IUÁ}mQ†²ƒ«UD§y–Üt¸39UÈ\'Y€Š³™:PIÄ´?Úl|ch²vO—5ÜŠ­nÄÒ\Ki1Ç¤û	bÂ1vöJä”[ğÆ7š+Ö ,z½“LÀF.¦¹62÷.ÄÙ²[œ®ã4.nù9c°;©4'ŞÌ²ínK“•YqG)»™IÄ3{ƒG2jtZĞ‹:õ’ Y„ÇQ˜Çë[l?F:rØ½”	ó/`Ó5¦ş».mÌ ¬Ì@M‹­(Ê¨ëü»èjKKÉ²ºÆÄ7i–GÔ¨2MqÂ­#uc}‡A2ªÕêñM2V(“©íĞ”Û@öÌ9`^]RìDhIKÊ¥Î±GX}Rpj¬Å-ÚfÚ,¢Æ?‰niEúø?tPğT„é!Í€Ün˜çÁ=ßRWrV‚ K@~å·Lw(ã xtÕ£|ÌŠqO˜*?Î8]ç÷„Á1„Fà$L+0‹¢œ ™¥(/È÷M(Ñƒp´£Ã£:™ Íô
ƒå	w`²ÍvWÒ)©òğˆX$4@çA\f/úH&œÒæ© ÔDĞI°Hı¬â»PŞM.üıRé´¶ÙHïPgÅ±á…²õ­pÖdÏhŸ£Ø‡?õ+4è{Ò¬I1Qâ‚â¨R;'§'/Ë5æ¼şXÒ(X¸Bt¡¨qœ¥¤š’çàQ%M¦åÿì²2ª _P¯l‚L,v¸GK‰A™-¿JQÒBü¾³lqØ××„R‘úvHÄ¡
½½gbYŸ9^Õ9¤tÈã¼º‡±6¦CSÖwßOÌ $rÕº5ÉuÎœ©ë€a—ŠRVãkm¨×óÙ‚»5k–UñÄ¼âœ´šhÌŠùŠ—¡>UvÅ‰	6Wy+ÄOBÕä’„lƒÑ#‰}$"ÔBSS%ıöÜ"<¸'¨qëä4yŒR» õ½È>ˆ—µÜÁÁ&ÛšıkxÊEh­…2³'ªğt6ğ1Í,ócœÙÊá%ÇÌQ’—L¯}bfz1é;7Ì¯M™¦í.{aUÓgFñÁÁË6‰VÍ¯ä»e„ŞJDMkeÔFÛbñ:»‚iğésTíàoÍ'!s…&æ2»¡<ã(‰™šö—Ô¨Ex´z#õÌ^¾~=^€üBIaiK0§ÑÎZ şFİèÆ·Z”,ï· %‰.ë}•<>t²Œf2q KpçE	Æ8M@—	LÂÃæ‘fxÈ£:D„ŠÂY]G9ˆQÅ€jd½ˆşï¥§ÏÆ@%†‹¨,1‡š\1'_¨"Œ&¶¦¤HfQï‰*áÚ,¹•N`›ÁRYéOÁ?§Ù¼XbÅ4+†@¬¦0./6Çgi0ÄgÅ½†ø\f=~ÀŞ–)	¬-|šã}à±ÍW{xÍAÅ÷ö"–ÔúÆ·,›39,°×„:…ıÀGĞƒsk¬-‚‚ey‰¹ßæ7<0·i’æÑú~M¶Fµ ÔíÓ»¥ÿ{éù–ÊºÁàú‘1f'¬†Áam0–cÔ%í×á?>³[*añ1ÆVEæHA3hƒ˜,ÎÛıqø£Â`ØJ‡na}â#Hi-À·¶Ã.¶ñpĞ[—CÂazD˜*ÏÙ=w€	švˆ„ç“í0ï©vÁU·‘ğÃ†2mÍ§K5z!H¸e/J‘ÁĞÏ¼#~õRò2PNH‘gÏ\fÚOwày¦›lo‚AI[ßRæş|%dæ¢) à.5NÛNxš	ªö\ÌN’ê¤àCUÛ©uÀò¾iœ1ş±‡)O âbºcì£òE
=¾Î³Íª€›xv	¸ÌÔ!Ê2AÑ>.1ªÙ}É«}ÔÎuğ§…ÇÜo®èş|å=w¼…rï{~úô¡äUfµ³ExÈ™¸&{œíCT $±´Õm¶™ºÂ·:·’a”³|PÅ-ŒÆ½«A)nøÃÆ<«oƒ¼œM_/Vã‹áñ™.T6=§qe7ƒĞbr=„'äLd?’pÁQı¤öaÔüÅmOû:×óçQ7ºı««¸Ò˜b6ŞõDïÁv)`ü'aÊÀ[4Á=ãÿİ[¼û­Ëdî<{¤Ş)îØsy˜'Äìİi?Ø¨‚õq'°yÖ”gÖœR0 ÖÙf£+\7p;8Â£:º:ÂC=Â&)½:Œ°Ú‹uEiq›•'Q‰—ë°Š¿ ëİefµhqÕ(bBƒ&ÇIµì JYª4˜4*ğ¨1ínêC,Ç”‡ö†‡˜ÃŒÏ“ò·ÏFZ[A4_+¥l2A›+½O6¹£ç8ûË³U@uxpï5~;ŞqJv/Sx¹RKÌµ º¬È¯L–_„ªÁIÙÉaI8ÖEïáC–À_ËÅ¦:,U:ºÔuéİ>•HX°KİÈ”ÔJñÆ{:ª*ªoÙAİaY[Ó•ïÂåKÔÀ,OÒ r‰± ½à2c­šDÃ²T>~¸·'`ßM÷ønÄ)§±§˜„Îv®¶V¤yÈékéF_{O<ó¡ ]½ŒŒ‰Ğ¾|»o¼üZÇêŞJS#vÕ&«ÕÎ²òæÍWå‚Ò„C¢Yêë1vÎÄãºßêU)VM¯·ˆÊW@ÌŠ#4Zhî?mûaQ5Sø¦êxQğø|úŸ“Ùpô·EŸ‹š™&8Lï¶U7‘|ëyTVK_Hbœô¥º|ÃIÂ€®p7Ea­[v–Øé]¢Xi@uz²tH8v6#¼éq”½áqI(xÓat  ¸°"C·Çf¬wÎî#BºŸï·O£ É—^ş»Ñm«iP»¬UUcÙekhIßbôõ£ï/Úì›«û×_sÁ£î¾®¯¼š:»ZÅ ùL\N}h@îi†Ù.£Ağı¶ö@,¯é»³9¬ê‹ŒïF¶¥„]æ2ŒªÉòÔÔ{|râéo¨W¸½Š‹M«„{‹ƒ®ù;6Âl:úáÊêjÓÄ£øœ­‰«Bò.…@ÙØ+³p¥%"Æi}:’™#ú4ÜĞœĞïŠXïÇ	9@G1Ä\¼-`Íÿ°yM5Êã¬²ÿÌ%*×|¨·ğwCbÛ,¯ÔîE·ğ¾²šXßƒÃ·
ié¢q˜p§õªı%î†îlÚüt{´Ëna`ÈÂ†LËşqqšGòô´¬Q±İdhœ!5C$á¥	^®KDÿŸåàÂ~€—GÅ»½xAx)ÿ	å–QQ(…6ÖÑk#“Òr€JOQ•¡eu’ÖsufÆ	cêòìóû®­÷!ÊZEª!A¹xºø ‘Kÿ=şôÒïOà£¯ï]gé:(á"ÅÊ6eVó7;tãUÕlH!UmtÍšÛ4ĞB®»]éˆü‘‹ÃSËò2²Ë½x„¾;pUbÕL}ÛŒ~¥T&dn*ö@¹1ß†ìoíëĞË›´Õîå}`òòî,l·R o*³Ş9NÀU\·©g¶ìQN&š:˜j­TPÉÑ¼ƒÁrÙı‰àÒF0jid>8Ia‰‡<Ü¶`	¢À1È¿…&û@^y[ò¹%æsšFÔ×\‰©TH;úØBkgv=*(9@·vÌ+³ƒE‹ˆ5
­™š+Ít
kµ8Ğ»[\"¬”¬
 g6=_³¼_›â`Õ°*…UÃ.ğ
 0ëˆ÷îÁÖÕM9ä•“ets9İ8^²åCU™µ¨şXm
4j!uû®}µy»áÁ°³^şF_s-ıë8qVÁ­÷#ÄÆc¼>äzFÂÍÉ–kŒ^cocS'ï[y¦ÁÀo
VĞª-–ñ«ªºĞ’9B
Ùoãlêıi£y©F§8V(YŸFKHM¥Ùêw60¹&P/…–]©ÑÊ†ÚªÈ°\Í]H¿^ZŠğNƒéµ4¯7é(²¼N`ˆ¥1ÓGæ‰`YÚm!ñÇ`¡g†[ª–ÁÓ×tG’¸:¦iÔÍI²ëS.‚Ş"íjÖªµ	“hn†;
Wm*Ê‡ ï"Ò2 d½sK«Ş€Õ8ÄÙCĞ‘}¢`b"¨çzP9N²ßTŒ"È-íËZº}¹ô…ÉWÍQÙËÓd/Ó#U|˜W±(hÜüä]ÎÏào8ØûOÀ/Ímßû²×÷¤!)?Õö)İŠÂÁb6¼X¿].&oÇõ_ûV86œ=Kà|í°egínÔp/Ê{BxÉHÕ¯ù.õò]Ê­ÖBmVÒÆ RšŞÏ/•fNÏ†¯W‹ågãÕùôbÚÜ‰ªaˆ­-3äF4@}Òyç•°ÇõÒ­.kxœ%­Ê¸ÏbQ§g'ÿ,‹2)ƒ$^ÿ.–e²MFÿ,ìàûß[,ç“¿ÿYæ2%'qúûX›Ë‹“ñülrñO³<¨ÕÎ[-R­¼hE‚u„¾–l;ÒìçÍ&-¶YœDùï3³éäl<ÿgÁËã$[¿ÿ;¤‡k[Ì!‡³E„5ü±ÄDË˜X³Û ÙO°Ê[H®ş¸<z»÷7Ğ6_ŞuıÓÉa«ÌïÌÎb«`¢k“†×A¹¾õüñÇu´Å`p'qGûE~¤gˆ2vÔÉ@[!/zçzşHîê€¹^AşqĞT¦ş.Ğ6£QZÇB“}qéİrK<Zß(Swè~£
 ù5£îµ‰Y½£§AİŠIjr>pW·™«Õ† YáîÀü†:¦PÊpLM¾ş“Û  ,Ç¾qmHlgq‡ÓP«Işô_éŸz	1ö¼@¤í"ÑSb`¼ÕÁÔZ§k:ÍXÓUİƒ;şİ_eAş€œ)–íÑ÷şíğĞĞ¨^Ü:Ò*‘«¬#Ç·m96q˜‡‚fb²„8ì™'O\óüUÀ«“ 1¥Øà,€ËÉµÛ$³İf;AË§[çìıón†Ë‚6…«mVXkÜ£PËÛ›)¦¹5Q÷mö¡=„ä*Íù ìsdLµ+Z)%iıÙœ¢×`NÔMÅ¢ƒ¯oj^(`ø,f\ÑÄ¡‘ÂX†‰²œ’I‹S»è[“2ûm·°<É6L5}ãæLDúWï«^c&)µ4*`ŞóÊ÷-Öx@Vpís¿¬£¿ô¸±¾©-Æk¼ğ™%¡‹Öï/¢|æ§)øoëÍÓ"‚Óih9C´VOšm¾¤ö»zEšûkÌİ<K4nŸİ¨Lo/±¤'ÅÛ”ÇØpõ+1‰J[î”/¾@›Gï_½€®=şh‹Z…J‚¸®ÓY—4ŸzÓş¢7(Ko!º_‹îaÁïÃ‚¥˜B»ú ÿÅ´X^	ğìOZ,>
±BfÊ`O!17ò@uDJDLSò/ô_üw¿¬ïÄ±ĞîWI;W½*=ıŞSúPîi_92<}ªé/äæ7Åš,68Ûa1‰9U+&…æÅæÀÑ—–œ‡šj	ÎSÒÇ#Pa©ğ —”´îD¡C}Ùµù£°3w=ù¢…&ÃTo@»kØÊA!‚Cšáã‡^˜[¬"3Xz5C{Ã™d6x9I®‚õ{ÇvšrC€ËvM3£úD	pœ³ğ¿ê¸Oğ}åİÑT-œ†ö4ÇNéÅ”>¸&¨‚ä•:Ë].¹³N6æ4Öjšœµ°¥F¯,„ç½Ğ­i‹mîW2ÁÈÚsÏ¢Gâş‰fÕäËëã»ô4³Ğ‡]ˆ¯ …úÎi\í!j™&èd2½f)ìØ[†…4ic¤a¶á.±MƒW° ~c‹Îã°Õğ´\!]PùİªÉÇûò™}9™Uå³À|kS_½9{š‘;_D„Q7ã·˜É£IšÀëRı$ïe&¤Ó_9+ëÄO#
#NY7¢-}áÙ?M¨ñH‹SÃ7gÀ’ K*ŒòWñ×ÒZQ‡[úïÆÄªØuS¦S¤ŠáWéN eÒ ‘ÎŒ7Aq{lY¸„|ŠâZ¥
/^eÆwº1©;¡íF`‘°®¡¡”•P§…lœïFùmLÍİöt.(×bÓ‡+Mx¬|B=2ÁDv9Âú¹â¤[}ª1yq½®íL„dU8iµŒ¦~ª¶ ^næ, c :˜ÊÊ5êÇë–¶dƒ[Zè«ÓØ³¤¨‚§™DY'DëGv9é³ÆÜ(L…Eyˆ›+$(ZÜ¸¡‰W™—ÅÜ×ôÒnÈ 8‡»GìÚnhêic’lmfÕ1·˜¤-Mh­˜ÄĞ·Ü™ÌŸır—¯Ç”…ªÉ'.–à'`86,Å"*ğ:°=‡¡ASeÂ&àpQ¿#'úˆDÒîòuÄŒa4Úe—ô‡A–CÄK@¯‰xéIŸÓùd|±B¤àj6/çÃÉÒ-R]KN¦ù˜È3ôî"}?àuõ‚àŒš²Gh~‹·Ù²û•åºø}˜`Üú=^óÁ›KaØ”4|W³º¹TgÛ–T?²Âğ°Pn%~]Fõ6I~Õ­	wfÆ¬.1¶I>J1zUôaI„Çéa5îPcFXŞi:Ã+?TwlÃò¨ez1bÉWÈ‰‹úåIÆ›36Óq¬İÇM“ÙêcR.*³/²²˜şªU×’¾”ÏÔ£gIpÏØN¼ Ş˜'´†e~;bÊ‡İ–tYëTx¥•7ç©†Î§c¤¬ÜªÈ¤ËŸÈŞf#}Ø0d1GãY¹·	ø	qrÄëTIsÑÈi¶€{sŞÄò…c0å*æƒnZ‹nhF(eT‰Du¾Ó&™lHåy´b²¼XúUï³Û¬Ì°¨	‹“#5-TmL®ÖHËÎÖV±“¢ŞH¹Ì¡„ÊËŒpá„©ÑàÇÁÿğ³@tÏ]ß¬¨W¦ Åm`?åUwÅÕ^²ÊA»„eõİ±^õ¡Ó[u›l„¿M:+Jo¸™§uÂ¯:éñ6+G1UãP´m	c)Wí <<5æÎŞgn×¤Ô˜]Ï[Á Ñ¼3D‚¾ºmCw­™j)ÖL¼Œä>x)j?Ş¯“,(Áìˆ»‘ ±¹2j°±zM)µcZëÛéŒ’5Án7‚vÚ)ãcá¥B)e“4€dëÈ’VMïE;^$Üv:+4ĞK«Çƒ&’Më>§³Ñ£x_ßEë}«“o×uqš.Çã¬á:Ùf>2.d6RiØbn5q„:ìàÜTarmÔ¡mSÏœúçQ±KÊV&X\Ë¸LçT‹. ®kYKÈ¢û”ï˜¯‰¹Ü9w“`É=ì^Õ­éĞ…£2€^Â²ö3µâj·Ù»Ÿ©²Á.Œ³YİŞ´È{PĞ³òRì«±ÕsÇvÏ›Z®+ÂÅŞ¶‹Ç3‘¨(?IgñÖïá‚ã/Ş
HFÁ{¡Œ›¹ººÎşQÙD2ô-ëp	½¤ìâzÉ€êŞìfg¸—c µ0}!Î¡ `ùÆDÈ¨ƒ‡ÈÒBZ•&S¤ß,FlúqÆÅ&.,×­HÔK½tÔ<VcsW/N£áL»ƒœ·)¢ÔmPÔKÇŒ‚oåf
û	£zè3Ã#“şJq £Ø´iøcŸ‡Âœ¨9ªu/¸U;ÚD'QİVLáK -fæGz6Âvšå0Xò‰~Iü‹^³ï[ó<®áPÛmYóuGåV§¦TgÖ>;€_ë~y¿*×Øê¶–Ş¡¹È&Êo"î‡e/&&ì¢¥¶5p„¢ƒà’·…˜8®¸¸Ğ†Ø.ŠÅšWÙ	)ŒÊ×A˜egmë¦PX½Úvo’ öo"nAÂlô[jØUÍ¤ 0V¥(¹Ì”›coÉ/EBŸÍsÑà`‰,lã²â±†œÒ§7"ÉTßÑ˜¤(äno†‚šµYªˆÅùkÂØ¡ÔXcÏßvœuşOØ'ñfx]ÊqØ˜ï]ïPaDşôïh?şÄ-:O=Ó÷£Ÿª;{MW×6øÏÁ¼#˜1¸´Î:sô09êPÿ³×Èv _éËä¯otÃÀÌ ÊÂD*ë\ÜÅç«B¯ô¦Y8å-Dİg1G2`?jaI}iu	Í¦]…ñæ8ºe7ZûŠˆb:@M¨«B¨ZÓ¸©²Ib=¨šqQ7ÆÄ—‘´I¥ò:^ş‡|ğ{÷bŠ9j¸â<Õùx±¾¯†#dâFSÂÎM.ÆsõH»¨Á¶¯“À”i)#êH¿«›%_s‰’°&‚<l.IrÏwE¼¶æÜŒ­lÂŞIçXI„âb®üvt×õ
j;MŞª6˜©%“#
µDãû·“ÅäøÌÈÍÃ£ôM˜‚¿ïˆXˆAnÒ ÚÁÌa’d@J$ã"ËM"¼Õ·ªÈÜæ«&E‘75vh,ŒWSh¹IÃı—rıØ<¯ÄíÕ|±¦3
š×~›GwdL Ï \@b³}j ½ƒPc Su-yĞÁ-o¿S·¬‹Ù¯İT|éÑ‡R.u‰®ó‚°¤ITÿN˜¶²ÏÖù÷B™,½Ù‚öE„©„®H/¼õºUÒŞ=—`êvŒ‡†”ÀT°¸RÜ÷ŒG-“Ô9Ê ¼Ş‰Òì=£(ÙS  ©ı©!ïƒ-òÇŸ †E¾üAw½mö?Mé5îÒD­ \„Ü ó<

¤º57J:²%ÊÊy®¶©T‘İ½ëÂ«K+¢ƒ:Ã?w@Ò€¶y-hmDÃhï[æAZ¬óWD+f›ãï¯"êiJ#şŠÌ©â2hMs7Ö»´Ó8e	šÏ9c‡¬­ÄÁ=1KÚëê,—‹Ë6ó¾wÁ<U/FjN¿0šÒmj+¥!S^aÛæw(f}–øSOE‰ô ,Dd.¡QæÏ¾½‰S},BS¨ ÌÇúV‘M~4IÍVÍóÿXì¶øõR)=‘à}€~aÑBóÈÆ">éıö"”£)€M¶Ê`Fé
Ì¤q1¶YnĞ·9ƒ=ÿUkãÌ©"ŒûşËg²"› ç¶ÅúĞ!L¡&/†àƒ»^«ûŠGS33©¤Şš )Ô@WÔ~±ä>àkÓn´IjÕtº¸±|3yûz["'AwœáË„s*[#s't³ô5ª©¦c“‰F²Q¶ÙìRLQ—dS:V~ãmpùz÷ŠYº{-%(hµ½¶ú5™D7ÉoR!*»©:öºU@.ôŸ€é‚àFºóè/!Õ·rœ‘7fsŒÌünQ3Ô¯Ğîñä‰èhN¨0,´nˆi –äïUáU)‚©3P;­½ï¢àCp?G“~Qï
ƒ3…È¡p²)“!_a©3#[c‡H¿í—ÇoF$‹á–,r8L‹ætŸb'(X@â›â†ßıduErÊ!Æ…¸@hëª¶tIÅ¤ äA€GÄ6)ÑlLûÎ;¢¦2¦[<d:`k»úÍ¶)mX¡Ş ÀBƒù:&ñvÍsL*Eeœ ­€¤}Ô«Ÿ©âÇyå¼Œ–­ƒ$’ÅJ*‡úóÍ²ÇïÖ¸ÈÊSp\é©ºç¤ éäWñ«`2Uåş‹Éğ<›…ÿÀ²}°,N9uFİL¹¿¸·7~ı“`SÍôTŸèC³R÷u¥Zº<xªJ[Z®XaVaü]š·%øïêeéöÒ¦I Øµjú
(½¢ÜkÌÌ…ªûY$xëHÓ˜3LĞĞ”ªôzY#õJÃ9µåø„>$*¶¤ÁÀ7Ãr—Ò¼D´IãºU“º•fyø&G,0é©Üc QÓHÑ³£útä%^Wm.5Õ‚ïN=99°å¾?6·œ½í³ ãRÓ aw’<ªƒ,YíÓ,ßT^C¾¹a‚-†ÚéLæLGœC™Ó$¸9âdš.0&IŒÉCé·Ùşóèš­Èx¯SEfû6%ã¶tÔ ¶>+û¡°Š»Æ.Áe¢ÏÀwBV—z˜ØÊ½Q@C²ÔÒ$%S-³Çk±ji±¾Â]bF–gZqËìµEìØã
pzµ7o³É¶Ü>Z Û;×å<>â‰-}·y¨*­´hÙõ4éÎ´ÁÉÕ0ãÂ[$ì®w[S¼L¹³Í¸° gÁ.]ßòom*ª…®6å|_mÇĞmßc{®ŒıîWY:`ÇN[ú	—D÷tPN”ò˜ôiªúÊ1­z_Ğ¤æOî²x»…µš8Z[êO8ñÇ2ü'òŸôÛ°#áháÖ4†±¶4ÈëßœîÙ<Ú&™bDM\^V)ág½ßíÃ40×ëIny"n(ıõÍ¨gğş®g³â}Òè4_˜'Ô‡2vƒBj º2=ä¥O\24|ÂÜ	‹»îóVô®UGô[›pĞ{Ô¢UÜù7ŸNÔİp½A‚á!€tBÍøÙmÓe÷0ÔÂ—.¶·%s&‰0x5î¢¯a\bA•	Ü<´OÂã-èú.AK`š|U€¦*¦n'†´Ù’Y“@ÈÉrn÷5¤·—¢™¾WYÄ*‹_§"/b±¶ÜÌ§o¡¸—+‰ü™f¨5x˜€W¬”T¥¥„¹!¹3z‹ÕÈÅ­å|W^Ñ”Ïi“ x‹‚t”mïÁ>ø‰ù]ª7.èU@4êRÅV¶Fİyù‹/ø·s³¾-ŸbB«=ÔÓÓ ‚ÛTÄ¡ÜƒÁ8g<MôöTtõ'7áà‰[[Ï‡¥‚d¢äÁq'8tÎ§'ãÕbøv|Bõ¹ÿg¯ßƒÂ4Šiş.JÖÙ¦r·¦K¡eb|T…«S´>GoÆ'—g´__Ş©|óB\Âİµ§èÚ1ë|T/J>Xje¸|°` Ê˜z“¥fõ÷¾ø3¯é|:'E0½öUhúÏ_˜]0Œ™—mùeHy>©ü©Ù(sHGûÔ+ĞÀ½mA„Æxq˜ŒÕ6Nù1el¿3XÍÛ¯¤M#MZà_Å,âÙUG0’©j¼Ï'§?tÀä1”©ş¤yîğŠæƒhä„{bĞûµ'æØ—ØÛ˜á0e°ÜúìË”©ñò‡\v;ÿ°:_@x‹N½pËÀÙ..@‘~‘±0“BÊ#D¨GŠh†m]šzæ³ 1ò³s‰-K#|iál89!<™-Äù2İv$A"ø[ñT™!H©ª 
NZC¶Y‡|²P¬µÛÆ,©‡½
œf4²Ì°>’§ª¤â‹w_P!Ä2[Di(£lû"¸"·cÙÙôìL>£ëê9€­ak7Œ²„¦È(x¬Ïö®M®ÏğHÃé–.×–[,ÀğK@UƒÛ½ÔwS¶ÊrÍl³Ô–Y.³8ÿwƒie>u6é4XóÅİ /ü·ŞóFf“énæÒI2ûùgmva‘Ö#ûöÔLóíï)&’`¢6uĞÄ~™Îkš
Jk†Æ‡‚g%4¬£`r…y¶—j‹¤÷Ë]‰·¤ï‚¸¤<P¤^ñò5™t	¡Ã*Ë*·!Í{_ä~ãBby™Zæ€í~¥•AñŸ»‚º	˜ÅY&Ò˜*Y
ÒE£İEVNRªS©œÆ¼×å%Ä¯7CF¨A€É
ÃÈ$Á%º	hÛ'\	c£Tõp˜Æ‡ÓB$Eó\sAÚÃ„èš–¬¾ç¬A7QşÊ‹×t-×àÔŸÄ'R½Œ6Õ©K±¡{ê€éĞXišz˜i,´¹3ä5’¢Uhzy¹nÆ)´(Œ˜H7Z™pƒ#»Æ¥Ü ¤NVş†^"Gó'a²˜ÉÁ”İ«ÎÅQcb´:Ã*à­2..[Î7Ë³ë81:y°IÆæ¦3Ì0/V³ùôtrfºbqËÏ!Ëu‰nM8XªÕÒb¶µu;Pçƒ_4&€éflƒ--udDÅq’­ß«kjö·@&Ô—G_zöŸÅÿGªÔ©’šŒ»|–C	Êûr¥aYë[xwl"³?S>°©]l£($È°ÉQa1O Î§]0¡€~Lyœ`å¥Ä}0ØÕfQ·‘‘¹Aš®›Tv6ŸNççÃ‹Ñx5:.«³é;;\ñY·Q²]1¤Á¡{Ò–úë$(mùq¿3¡›é…åg¾ôªzx‡]uo÷5Åe· Qü¦ØZ*ÌæãóÉåùêt<\^ÎÇ«“é»‹³éğ„®— L“.İÁ¦Š×…Àı øgGvk é¢F±/šÆ—zŠVb_l¿îÌ¶ÚÔ^ç8 g9ızº[o<§ã£c‹›]à±d1Ÿ®÷#p›
Bq SÜ´#5JH$DúÌ±ƒ.{}i‘úbë³ùd:Ÿ,X½™¼~¾Ç½*:‰™ahäÇÅry†2FWkPiV[°X8E™O„ê$†›8e¾1’V(ÌÒçœŒÏÆËñŠ%ÕYôt†á®Ì¨>|–mwÛwy°…d%ù5c–Œ}–²Ê¨^;2ºµ*ú\á$œ³_º˜B³JJÙâ¹ÿ
AÀ¬ñzEñQë™²`ª+8Tp“ªæóa^A±ÑsxAê½hÔQÃ˜l¼MÁµ¬a‚É5îö±‹GÕ*Â5..¹ÓùCİf.ÉD£É³Ízee]]Là±åì;Ù„‡÷Šïâò–íQaÆ¸»«”M›£ïjëPY&+²9â%ùÄ	o¥àG ‰Ü‹ã9æŠW®Ñ“ÃL&ØU¸p·U¡cA‡ÚC&ˆYúÿ€;“êˆ¬F;”ƒ9æBñ³#…\ª:"¨$bâô‘øº'Ü•îd—„Gs¹0šõ’ÜiÂjÕç‘µöó!È¡á}Ê7÷)ßç³Û.2Ù•fŠ+.ŞÄaš£ uÇòihÅõ]l3rìçÅ<º‹‹£³‰¿ãâBÃ=Üõ„ğãŒ«ÅyíÃmÜÕôˆËõ­çÓ²ÍŠo‚`\f!"Ój9]½ç?¼Ø§Òónµ8ÿ½XMç«óËÅdäXÿÍp>v+J$ŸwÃù‰[áùxvÖ0pxøüÓ«¬KÀ*nº^wiùÙ³æŒw;©o"s¾ªÓè`Ÿ5k:ôê³Q nP~+;´Áê»¿4è¯›ìÄo¦Ëi·*o''ã©ıvÏN*˜ñéõu”³~lNŠ&-ÄåkÂLãŸã“É’³×69×¦•€|ƒÆĞÄ*Wª‘ÍòxíªdSÇDä­‘»¶œ*8œÕ»4!Ú~C€Ëƒ÷ÁrrŞI]P¥a?o?ùxï’ƒ€Ä•ì§-×©CEJ¶¯~„¢LpdÖ«ÖÕş&\Án,í<¦€/³M¼>fn‚"Ï‚ôæ„€ÅÃØP@ó‚‹Î#‚'`øv¾KQ£şÊËR©İ“,¤¶«:ü¾U©õ9Áœ¹¿’:s–•G*„†‚Jöî‚8¾çåñ_yò«‚‡ÍjÊ=êuÎl¼zÍ›íuR“ñ¼”ó ©yFsÿª`[‹U÷˜e‹€4 ö†¼[}n1“½]òfœ¤<\%%®¹c}Dk±Ì“‚«J‹ßÓlfk—FC^'ë¥²pŠ§¤Ê9ÄèÔWDÑ
2‹ÑKåj|¶œ¥ZošØêÔZ­–9ûŒŞf«ƒcNÌ%X4©eº?`j!5~-ù7†óâş3Á2qš£ùßt7<Ÿƒºõ–øóè#,:µ¤l+7I7‰ƒûqKê6k*®!¢>‚–ş¬Û£©–¯*}ğe#Ÿ}PÕT]®ª-fÔÀB…j<„èÛa‚GĞE¶AÖ­×ôÜŞ‚ë½ÎÛÏm†2ĞåA¨ÒMd—H‰8ÚÊwD©O‹NC%4 (ÔK?²LG^3DÂQËæ¨©¸Ãùh¬Ş°B‚QCpÅ6j˜†Yò› ÕZÚ¹n4„8Ù@ã‹“Õ|L-šï4—Ş¢¾KfEÀØ™-²²P­‘jù©wÃtW"!û 2’³ZU¤¶¥¸æRI1´/#jÊ¹L1ƒŠúU|íòo´X¬ÙMs ŞAn{Šï[+«À¶gxäï©_çŒjŒuß°sÎ	jxå°Å¸#„Jk„Ÿ—ñ:Ş•…Şbü­ÃÁ^ÄÿˆÀU`ZROÅYàÿÌâ4
ïY”?Ğ­¢v¾3ÇÜÄº
ûg¸+o³ÜaYvÃ´(xüüşGPû™(P$qú>
WUd‹z±°©mS3£‹‘Â˜èm‹AB¤ÓâA}I@ğc%×RÈ”^¬j.7L³ô~“í
½œğbp••z]û.Ö„<tãWÆ&áÄ9¨ÌM@µV@XŠËİ$¬şA„Ûˆğv´îŒÔHğ' eİ¶hÛR¾I%Hóˆxí†dô^qÿó˜½Óá4o°Tû$¢îàóšÍJMñ›M(‹ì`*¶)ì`OCFvš‚V]/lå†!P¬ŒÀïZã2—R-©¶\±M»Q€G„ÿä¶,·Å‹/¾€œĞ6šÇì,®ã*·Ş®;µà«Y&ëÑßv7…C†^q¶ƒfI‘Ø+aıVlììç—Şß ú™^²—Ó³áëÕå‚°qï&³ññpô·Fõ{Çæ½-¼a3­y·Ìy{ö°"L¸© RŒ|¥dîìÓÉQSá×€oÒyœÆ›İæ]ÌÒïÃ­ÿüPËQŠº}p3q gA'r¡húÅ'hÈÒÛÀ˜ÂÆ”Jğš)èO^Áoo°Ù•HM#!uŠÎ¾d_DólWåGYBXe‚)g?ñïÁûè~ğu=‰®ƒ]R.vW`>ªë«s*`>Z¿6Êoc°õZ€ÁiÓu—›`«ÙY¬£ Æ±ãd—ÉóÉ²KBv_ ¶õ )wåÚzU·Ï¿L6Û„çÒ<øZÖBsGó ŒÉ	Fòè¹¥_TRæß{=uªPà0|lWœÁ#&Ûl4^öAÖ‚ÃM^NéH2=·ç‚Ê`Kvß"Â²å®ï^(RÖ¢ìªìv“˜Ÿ2”Ğ*\h@Ú8úÒbr—HÅ›(¾¹-a–¿|n3øë•…Å9úŠô‹‹„ÿ«Z±—´Bs¶H@ş;øÌoòÃ©;#|FûÒ—óárôf5ÎÇK¥Ò»ùp†We’O] ±’v‚eXÀîÎ+çYŞsÆi3ù?4¿6ÌÄÒQø#­/sú†“$$÷J™†«ŠÕ‰®ÎŠa¹/¼ºáW0m‚º`J¦2¶ÖÌ'M„º›CÜÅyr2Pº¸Ş¢å…9¼4Ğ:÷^8šAsûâCGõô¥G0şlwğe½WŞÊä÷mVD>yüiœ³P|ºU˜ßÊŒì:H©sF¥ÎÇ0™Bi ñvôÓ‡Ò´&ŸæäXV­.3˜¦œ:4Z“&j#µ'è´Äôƒ,˜.¾İ}Ø¦ä×‚“Êf0qDŒ;c7¤F4µA”Ÿá?TFHĞKƒse+ÅµL´Ø×@œŠêo‡zdm§9„»ˆ_gâ··ãùr2¹ôOÚ¡1 Øx’¿"ö]úhPúzòzòå/ŞŸÍ“8YÏWoÆ“×o–«“ÜÃTÏÂwŞW*úœ¼ıwB¤¿úª‡5ˆ%ğ‡mz+n ¶ aüÑ[$¸®L…2f‰Æğ‹»7™k8Îz‚™…fÙÖ#i/ƒÌäd¹İ|¼,•‚`çÙ‡´ÛÖåí’Æ¤ˆl¢Ö9a]–gdêÉÔ›Öú+@‚CÂCÙ*Z*ÑÛ8l¡pâÜLS¼?(%öİÜ‡ ²„0AâmtŒXt&<ÍòˆI:NÛ‘³•¼çOÁÚÚX£†…;a @ç¿a”€­ğíè'wÇÙDùe½†c¯ÚaM§à2(qT¯(§H'"n?ÍDšFŸ«B`{Åo]é/<¼¹F:úf:Ÿü_²nVJ
´2S…·Å)|ÕÛ}€{“åñ?à IxAã^®
á'íÆT²©Gk—²ckÁî-]V}İZ† ª‡IRqR¤E~æ\i=Àá¡Ó	èõR€Ú?!ºWC›NáÓ¼/Ò0CL4»½ùõó>hÀïQÍ5"Êl8Çl·4GÖA×ßÚ/…ŸÛ;ªÀÂõ©E¯ —Ìı†¦Uó8G¢:h»­ãÖ%B)cÒ¾F2óQæ²L¤'"F‰åÆQf E¦eØdšgÀ¾ÄÆzUØhke3â_âûÆo½šÎ¢Slœ„ÃÒÇ^ÛAÕ‰ÌOÜV›NP!O9S¨¶·AoP¾^|RµD~‘×À_ß#“ˆEÈâ’×ß£Ú×ËS?|¤Ùï÷…÷üºÌd6$Ò\Pâ‰ ¥7ƒìúÀõ	;uDÃw9=Ó¾n"…ü	a…±ÍhÑí®­şƒßAı~p2>^-Ú$üÔ%jÉ+d`°W:á¼°€føgÏ«Á\0ø#
q>\6<ÊúõzÂ¹Ğ#=‘5%ç,í3À‘J®Û(E1İç»˜ËYİ "ûG×Ğ”c·Æ&\jÿâ¶Ú`Ñl›­¶¦ªíÍùæÛ;#ÇŒå<"øßEÉ½oå•Ú2Wı:0}ÿˆüÏW\ÌíÃ)Ğßy¡òÄ_èM«e§é:f¨\‘yŞ+SdOCcµşœÎ[CM£X"7·§p2uµ7vüY¨ÙYÄ["»
a”3Q£ ìb›¹ØyaµoMB‘pğÔ€¿¹­U¬£%\Ó'TË²–¨f@n‹ijÌÄ¼êœñ‘/+A·ùqŸNåSãTºès,Z–Ìr¥5fñiÈìÃY-'·E×^1Ã0Ø/H¯ò#/ù‹¶ùiÎbRõC³™ x7Œñlm}ˆ’GúxPÖ®'o1V¶¡zu¹¹;¤+Ì'£–S÷‘Ü1«mÖš÷SŒ‰ÁK= Ê˜eíÀÑk®·¥ùd¶Y·%@àNİ'˜°·"(@T-¹ñŞ¬Ç´1ü© ¯è1mÇ‘•`‹Cë8ğN‚—Jù¸O•vê ¤ºÁo4	J¿ÚnÇDgâUÃeÈê£öeIc˜Fİà]ƒyÃ¯­Ñ`e¹³/¬V›ıWlòâQ„ÁÂ,Ş(tUâ†L-™{W”Ùf¼Éş;^0ÏJ(ñMÓ#h]§$";¸ƒ1ØYtáOKNZvãA5ãµç‹KN'õ¡6O„z¬ßx–‡NZlñ•o¦Û±ÌÖÌÓÓmnàq£ö¦§kÒ´¦§Øçé	­é	ãÍqtKv1î¨6Áö´ïn©t·P9$ëó A®¿Ÿ¤äMáÇpÄUä!æ$Oì—Ÿµ6Â§b¯i¨³AìSNšùÍGx:Tÿ¥+iaÉ€u²B¹)ŒÜ+2	7¢TÖüî¤äx—†IäyåòIßtÅ×*·]÷Ğì³e Î²ôÆÂ3?áãí³v·
£P<ÛŒn˜Îú&b›ÿ$¶Y^Va³Ü÷B£ØÄøM*7–4Tµ?Ì]ùp=‘ı¹ºtÃĞ™~_MÊĞMùìT{P¯Xç=ìM!:ínºi©Ğá’7ú–%ã¤ß÷Ø ºLÍyäJ´®›ï¾gÍªà*Y¤û!j7£“¹º bbe˜fşŠ¥úÇÃŸôÁ9+Úùã¢¹ìlh‚u©³<
c@ÚˆJèµ&ÁYªyQÆ¤V_ï^¼ùV'ş½µˆ.÷İ94,Š¨2ö¹Ôæ’ü¯'¼SÖ-*Ê<»wÛ÷PPÔSI“èálúÔHÅZÙÛi²¡&İ¬dõ“@ş¤Ô¦"6’§ÀÀXäQT8ÓvCµÈQèËóJM-ü÷ôúš µ<÷ôİ,şHk'Û2ÜL`°å¨ë”Pfús¦²ò}Kòt©®A]ª™%™ÎÎIg*>f“èú¶£I´-££ø¬oM†¡X< ÀlMòšy/¨¡¤" %îyÿ›zW|§—v1òÉ²tu<5qqÉØÜL¨´`G*Üy<mTtÊº¬üÜFæpòÀƒ¾ßÓ$úxQØ÷n	õÅ§İÌJ­¬hdE«Œ…®²ÛÀ¹eO*m.gÕD}ns~~µÑ2shpÇ½ÙÇaÉø,×ƒş³çhXŒªAh‹À4yVJ>:×ÀÎ2&wû]Uûh7l\3O“Çá˜åO5¸'¼Éä!œÊ¢ÄÜng<æ7×Q†f1šOÏÎV‹åp9^MNÎÆ®”ÃâúóXHhıÒÕÜİmğÓ±ú“´*C~‡Fz›Sr“-ÂØö°6z½@'[ÄãÙ ~CÛÃ¯bsøÙºÛÃ¶ğ ›ÂÃl	Ü [ƒŸÈfğ@[Á~6‚=l6_ÑğPÀşºÿGÑùwÖõ?ßQH ……oX‘p>W¡-!İg^KÂ'¶"¸Íò§³1<Ğ¶ĞÉ¦Ğ•*À”ZÊ ¤¹~îB9F ^ê­Û ÜµJï:è7­>]˜G†zi®tÇ+²øÅ¦ës\°º¼’AŞM±êòŒéµaê§9MrØ”gü²*çw¢„êÌğ_±ë½fB:Ğ‡ÌÖNQ=º®ÓÃR±cÆöºÕò¶:ÀÖÒ¶PÊ¦Ni*eh©XQn•7G÷ïRö¢®§3iqÀšğªKd
9x’
nÊõêŞ…0gÍİ ¥Í¥*[(ùQ)’†,û\Ï
)ˆş®¿x‘ïÒiz9¡×°÷=ÈTÔ÷ßÑ"‹¹6"§t!–Q˜“FîJ.x'uá®$slæš¶Ïí‹ãÇÃŸÈBCƒÂÛO¡6‡Í@>¶ã¬$ÒÛâ6‚.”ùÄì{ø|TD·o 2}iØİ›ßDùMD8‘©‚å³m¼†ó®{“<}1Ï(àS4g"Œ¸R9ööI˜˜G÷˜À«=Ãğ=Vn<”óì±±…Ëó’ğF÷I‰SpSÅ.Bfˆqß%RÛoÛfêà+mmhCÍ—P¨Öd ,¶Ñz°	ŞGÂo\OÃôõšãï‡£åÙ·C+ ‡†®./³ñhr:Ÿt’u »¾¤	ÎÀÆñ{[@Ğ_oæezs– =W§ñ[‚ÃzˆWöp_j¢ÊM>C&Å¯³‘Ñ`jñSØ'àÙŸ¾Æ‹ÎPÿ6‰áét·¾q0rGK§¸Ìê](×&§#Lğ4~yi4™Ç¨lœ®9ªDf@j-‡µĞ .Î² 4ßŞ+>t,ÛF©‚H_†îÜ!7¦0…û†
[wE•ÇÕrç˜9¦)}¹éÆ)eĞ.QjÊn·,Í2;á±V Ó†¹mü†&í0™g}tÁdÿÜÆ!ÜÑ„©V»”œ<+dÎé•zU1|ª/a•²Æ|L¦eï€Ğ%×LÆœã]œ$g¤bnÍÁfí³%òæÍ†¦ u‘š–ØşüÔœ€İ#Åî
A_â³ÊÒä¾çµPÖÅÏ/=X`gŠÁÛér¼šËùd´Ÿ¬.¦ËÕâòx1šOÉOÈ·Ş•Öè¢…='œÓ*.²çRö|[¡úÖ´6:ê2ªãÈÙxy±œÿĞ:¢Æ!±¾‚6‰¼›/@_Eˆ—Ã–òD	–¾Í@çûÈí2
L«Ç©]Ÿ¬-Õ Ô£íòiÃLICXİü>ûıÍŸ‡§¾µœlÊõ	¶õ2ä•'ÿ*Ùç;¤p®P¬],dõÁr
eºf,Óš¬B³=AÌ£òÈjFäóÔ—/Õ±ğ¹Æœ¢ë_%ŸèºC.Ñõ¯›Gtı¹åmÅ{LëÉ#Aºá¼±m)E¥!‡ğCÒTªiz?İå¶÷M0<Ö÷ër$3q+a<‡ôğ†}XÑwæúgiWZ½úˆ‹ğúóÖI©]y+]*~¶‡›</a™ÇÔMlE»»œìHŒ-×IdÚ8–cÂ8Feœ¢„Ty5ŸŞ]²ıî#X›…€=3ó*'Ëïe]™ki×ô%êşE¨;§‹¿G{ÊÑhX&,:F¯˜‹â5Š¼è·DÎ6DZbiI§QV4ÆĞFÿ(Ä´ñvÎ¯Éaíu3~¹:Ù´›zâ(À¯iuè%zn£±ù"ú¾Ø‹î89°1ÏÄ&é½ªû@Xû-ri[İ l-ñi\ëàV×ÉíFZ	Ã‚HíÀÇÚ@ ÅpïÒˆ]97N(‡6÷cææ@ï[#ÛùÜmûãÍ”ë{~ì‘Åàò«¿²k¿:ùŒÃ ;$¶k<ã"ğ
V^Y¿¯!¶I°–ÁMá$tj™eIo¹_›‹†À>z)?íW‡‡.T¯©5G
	)èímçÔp@g^›\V,1Yô…bBB#TÇš¼Ã›:bofe|}”Á"â®`Ã~=ƒ‡‘E¯ù«İÛPO+òw ¥Ë¬ºSõ…¸ÄF»Ug†Á!R±;¿ĞF„í‡PMë[Œçû0e‘ax$Fáûš<.CĞ‘°ÊUŸÊ&mÒKkÊÉ¥N6õÚf­ÑE¿¦ºIaÌŠ°
êı¤êkI¬|TÔşæBƒå³ñj1_ºe¿…f9íù_ÜÔ«§€ºç~iMŞjÒVß˜W®kş¬Vs|2Yş±š{®¦p…ügµ¨§Óù»áüäum]Wñ*¢9é|™)(¸¶4Ş©Ëõo³xÑûê•o6‡5r*.fy´‰wÚŠÁŞÛĞğàú—àï‹zÁ}KÓ0WaKó†jÌn}Ø½â”ˆ†#æ2®à0´Í³«à*¹Ÿ©µÌR¶HÌ(:N×ù=‘wBPÕÌ?©ïÀ|ÒÓ¥0–OÚËt¸Ï%ój¢IÑ (Xw }¿ i2^4úO³(ÂÊ¬Èî@U¬;ëÆ››]Ññ;Œ|¥¸›‚d™q4**5Rá$[ïãíµê~.Ó0Cj¿ãĞ Jö«MÀÆğl^³C|¯ƒ
‘wqyËnÒ­Bxûdüİùäâõrz1^ONÆ'}šc—âõ&Î¹ªK€a Å¼S­nùD£F¾K;åaÀÌû¶®ñ_]óo»I1€œnlUş‚“öİ"*K‚¿EM‹-r»-ş¨«{<n|É£º¡ÒZ6Tøİ86µ»"wå­İ”†*IEEÎUx#1­õû‡:*=ÌÊ™g%²ÌĞ¹¥1~’¥7<EÛí7®Ê*xöµx=(c†¨Ã%ß…ºqxø-O6÷«-U=qLBÖôÕDŒ¦îš`sŠÅ¿µw[½)~%ÿ¶O©æ²K_¿wÚh7]´ñYÍÑj**Ø=»QoMÔi¦à’Z®Ö}4‡4KËñ“ÊŠWÍÛ:c__î‰)µ*£5"¢ğî‚ÜQÒ+ù,qw ì¸"¼. µ8~ÀmYû¾»ËèuÜ7¹|‹}°}å"ÆÖ{²U8lÏú&pÅË::õûÈ$¹¾‰r9ÑHÉç}êƒ
+o’‰H?ş„àÎĞhWa2àŸmÎduõ$À½ÖPÏXñ„ÉßÄÍíO6äÜ@Œğ.·¬ùêõ>´âr+»­2L¤³?Šó5Ï’–åJ;îRöxW±œ×êÕ“n‚/Ø2C?æQ‘íò5‘õñgŠ¢»€­vÛŞ`³ƒL§6®¯ªƒ7P“1œÆIuó`–“?Ov××â—®~¼ p5}İà rKóÑjrá0 Í(ÏŠâ:£jÄ¼ ÕÀ
ír¢³ãU1“éèùuOOâ‚__Ã§½GÙaˆ¤Ú Z«(NœDİp"KD¢Á*yºÜv'Pšòôå—„ûR»ú¶¦¸ŞÏ21zö!E_şU G&ˆäí~’}HcÃC;ÿßmyrb¦lúÏhÓs,ë¼íyÅÇŞøĞî~[j>ææwÚû\»‹°Aìs‹İ„Æt€ÿÖ'óªûhU‰‚Š’¾1$]ÜÖ?5ex“*ÑÎšn©f‹šeh]‡Y³tµ¸/ĞC,[¿DI°-¢pIoZõHÚÖõasû¾ö½çù2¯	 |Ç¼Õi`›²ømì¢ao 4İKîÒ¹ôn]ŸÂu-şá5¦iqK¼`K\¢3f%>n9¤‘ğVW¡øx[LÏĞÓ¡ŠB±upmúm€Vt]=úºÍs•Õ˜Àı®Û,	òG»«x}ı#rñıàd|:¼<[º´Xy£Çç/65×A±©ÔUwaœ™ò6VhÒÂ‰‡šÁ'°m®¼0^¯!f©°Éìí4§²…ŞJ‘bn5‚7wp“eĞ¨¶=íµ²)ÒDŠ]ò$jG÷“¹˜yÑ	_jîã`àbş–}A6
ù?¹ä¿ıÖèN ¢Ğ9–Õ´fDÔŞ‹Ãh™ İá^šJ—CN†0-Å>XÄ›mUñó¡Ÿ4»ä*N¯³2Şº%‘×3H/ÀÔ-›á/}ıh„hŒï¼ó ¼l‚îËÃC¸+©ñÄ„{l”–z½–›«šh|Ëú‰[œmVâo£¾µğiÔÛC?ÇÛ¶Ô ¼›AUd”HôVy”®ºèÿ  ÿÿì}k{Ç²î÷ó+°>œ¶1‘e¯ËöŠƒ’ÙF‚È‰ON1ÒÄÀ°­½’ÿ~ºª/Ó÷éÉq²4ÏZ±˜éû¥ºªºê-úHÿ¾ëİÂÓªÚğ¯êV$øÇÑ›†__áV#ÜÈn´&\šÅëL³ÄfÎsu	åÿú«^¾H×šÍĞo"Z„æ¸Ê>s:9eĞr0Á©Ül6ëüÅ7ßÔ;˜E²ú0ÈâyòÙ{‡[	oÀb”æ!}»8ÿ»é¨ÍˆİÔ¿f<¼¶Pì=ÏÂĞPªıš°4BÆÈnyWÅK?tA*à1ÄsúBş]Î–|½ô­êEh,`ş¾b|„vGr¸'„§ôµåƒ§âM*:j1¹€LÕ‡®Sèª`ˆš‰æiÿbĞëü0¹ºì'íî€°—>J¥É	…Uûw—–öÈõÁ“£B£.¢Ïn›ãgn›cş\eIm›AØ^ò`Öç±o5Vœú	!Í9„F§è×i¾YÚ•TÙ¨ñİöåd“^­’i:ÃO°—!)ú`Ó0?áõ:nF†Ñì.®EYæñ]í¯(*UhE‰€?ğw® c'8ÌSj‚«µM°¡ñøZAÄl¡yñ„­]ËaÿĞĞ…J Cı¥=Ê»«h½n-é§°n”)>ÕE ¦Œ¿ëB(“+E·‚Zd'BLÇ)éà¡ ¼óÚîÀŒ#Ìùpš®oaÒ…Z'<¦Dùø°ñ.×Ñ•­ Î` ¾œ¼6 ;q%²0÷õó{Ğ†ÂlÔ¾…ÁøVÑÃ±îËÏ>´Gø+!‚p©¾qW¯“x¨í†§$"õ×A:í±"ú@D½c¸¾rêôm°5åw ŒÚ°=ĞHúõËÑÈyšaÈ·Z‚ şäŸo‹â)Tä?j'e3ƒÄeDV 6%~_IhlfÙm!‰¬‰¦9¦»BuJ(±ñŒT0ƒå¸@Ö{y{„D)¥54oğr€Tä±ãU,©ÁŠÜË‡ÎQÅt‹Ìc¹7‰sg"ãŞˆ<wºÎşxTKİEÍO#fAøò-å°ô7Mˆ}/J›wM,d¬§Oäa¨
² ºİ· 2Š°ešªh‚öŞÃpGË’?Œã¤{è(KÙ:÷MŸrP	~ç½İC·Úi¥'¡°{_³û/+h–x|©¶”å‹”ÁR3©Şãû`os©rä6œ1à)|HÀtN$ÕæP”ƒ‘Ô‚(VÅ5Å;f
ZÖ÷É¸¸ê»ÄL¥…øwKÑıP¿ø¤M:§.¢üPïGÕÅñù‘áxÀ-?j¿ÖÜ	<Ï°{şzL–,ÿİëœ½wÖB“ -gq–J5	%‡EÆŒ%±bÁOQ
øÂ²µ\å¥×!ÇPP9®*ÜåÀµ¢^…rÂÖxrÑ¢ü­ú˜Ûq‹!©¨r¶ÛÏ‡#<`³M˜Z:g#sA"•üğ†›Q|Á£uãÛiœ,êjmTõ‘áWÃ†ç£O`M>=Ìîµ Ë½xù˜õæq­ø4¤ äš-ï]\íä[ØõÅb7º¹Èwe¹ØğğİÎtS0IyıY	áätt¤2aY;‹E²Fq¾ ágÛÕ”PÇÖ¦Ù¹lt*J½–=Õ“ÚH{ğà{¹½YA:!]Œ¡Ê­°Jv7ƒ{¶‹Ü³ 38Ï¡­ŞíÌõ|ÚñÀ•<µCÜ{Û¤µÇ•˜Ğ?›ğx’ö}9º‰ãMód›,fqV{ÏşeĞPfŠªÕüa#»ÍI©Bv´m)£Ä­HTÁ1´êîŸXğ5ßèÁ‘PVó¨¤~L S[<¼‡”„ê\’ä,ÿ ¾­]’?öã 3á5Y‚)Ç«¡Psäè2"Ø³/kı¢B¶;½Î¸ƒæù€$/¦é’»ZcÂRóP¢ª:ÀËÃœ&+—‰CÕxw:ÀºwÄş½„ ¿/ˆ*b:3D¤˜ótèÂ±NÕ÷a H.Pvï°VaÂİd9¤m3]vP[TşÕÇ…`zåÜKèiğ®şô2åÕÑ.ğF¤íÜ÷õ2c7[vÁuWÌÈ“ù."øè!Ç˜»9H¨v2ß·kÀgğ+èIjgK¢„‹İ¥`¦â’‹Z¸µXßDUzÛ°	'ÕŞ‰²Y]’¼Ñ½Mã	ÅÅu³YLÈa“¤³ ÀÒ  ñÙÄHÕ^â ùdêóZdâè>:şT›k/V˜Wo*‡ºĞ}»Q2m¯¯cÂÎiNûTóR®Ê¡gE÷$'É¢Ä)ª”t;dMiğ'p\È•·®cr ©'Ëøj•lš£áÓÛ#Gm™“¥'¬À22%'^}>3Â+è¿Eª¡ùÛà²î±[šŒ’Ä2^ï‹-
7VVëÇè'ï^±nûaç¢ÿ@®G¾eöçU]]ğ\ñ×¸ Ÿ‹üìË.@ëvkoâ’Ú«‚C¥¬îıË¬³t
Nâ¼5”{•ùãÄ9ÎUçñ°u9êµÆ_Wè sĞôcâ’ò™æ‚3K±ò!ÉÙ²k…ùJgj›´­®™b
l-,Êœ.‹mÅ0‡)„3MH_A[Äe¬T$ÚÚ"µl0.İ8)ˆFäeÔ¾;§¢$),ïOµ‰èÙÁ§…¤srze#@Ó }U¯±ë©@éƒÔ€:ÃµÈó†µ™¾"Åi´X€»ãı·WÃŞh­5†Ûüª–®zÔg] ¾Ä,™aÖBğWœÂ¿D÷…ù(6`	–=y¿«ÕµW‡$ÃÊIùÏ› ÇÏ„QPúBZÉÿò…t±°$d‘“¥aú}³p/&¨ˆ=±ä¶ns"ŸŞÄ³-ìnò¼áŞoyúíÎäŸWİÓ7äŒôºr@}G›û"îFw şÍ7®áRi‘ê·‹™X'·l}ÿ±®lw{“äcs˜LoØ¨Ö²âo•ŠãlˆäGoë(ïIßbˆ¦(Z{YÃ¡ç;/;-å§lƒy$9š]¼2¤Æ(ö<K—Œfûš˜²5œ„‡ñ›úhŞ-Æ(o¾íº'½N ·zç‰ÎäÔÎğÜ£r˜¢æĞVøDIë•¿k’cPø:ÍDF=ø_°ßåÓÎíZÿXÎsPâŸ£¼›$7ãD2n?9<ú©!ºÔ»ßplæFm•¹èS”Íò>JOø§!Ÿ<xÚ,YÄ7„½G¶™ÉMå‚€¸p<©Š?ÒÏQ¾vÁú@"”'W96kD˜º:Úì ÙÔØ36üè1Ô7ë/ƒv#ÇÚİ""Ó“'a7ôûôŸ¬€„%Kn‡a-=lFëõâ6ÔŠ‚*qÆéz­âEHèxv7w*d<Çš+†\úzÖ|Š’ÍYšQb´¡VÎ°›‚6Ñ>¢J§ÉF‚ë\D˜œ*oŒ,c=…El^]¾¹ì9éµ.Ï¯ZçC€WŞ^x	à<†1¨Ÿ` ù`1éY ËQ e=Õm""5ÔyÃ¡A-©KÑ””Ş7o¢|"¼(8R&§=´±”ÊŠ˜šM ™Åb…ê¶øhË¨d[óÃaißà¸•¹rÚâÏóşeÇ›ùßúšù D‡Ê¢<ãjCåU-†‰ _EšØA0Õ”	üùİÙ7]ÂÛ.—QF–×ÌÉÙéš˜†Ü?wW–Ã#Áó‡É²Êl=ã³4«UKĞPK2f‹<ÈRêÈX2<Â.¬˜N}«Ç²Ê‘,í:jV}íUî_¡©¼jSµúK±êëÖPµ¼~±]êÒÖñª”ø#6HÕNòe’çâòn¼
%›[/833]*Oÿ ŠéÏƒ(fîDãäƒŠ<ilíh»ªĞ»pÊûüø“,ıü(GmKõÜhI÷²ZÀĞ=á‚–ËVÚ!ÒÜ¤ì®ü°Qš¹Î8ÅÒá öëdCKáŒAŒ!<0År^«8H>ø$Bã³M(”İµ\Ï½Ë†S$P<„çkY_¬R"B“õ2òıÔèü	ôrÙGQÃ¼6_)e´Ó^©V÷h‚ÇYXˆQTYAĞ»€ËLşDĞ TÏÓ˜Ä(ÕY²ˆ{)á8ëËÅ‡dó¢6Ø±›´F)-’/ÔÌ%+Î"öìaîË€¹Põ8Ÿ=o¼´#ÖsÀ>è{¾B}¦û“ª|şœ*zûo ÕÙŸ²<èv¼%?èvt;İ<È¤­Ó|ÓÑmñëN9cGA­ÕlÄYÚPş¸d8µcoDIß¥+Ê:ó§çwÿĞŒÛÁv5;à,›[cÈõÿVœZ &ŸælXı0ô@ÿªX¦‚#Ü‘	¼KVçŞÙœ>çÏyàsè7z¦‡Ş¯˜BÍ7÷nİ´:0?22û“u¼P×BüCùÊÀŒlw@Æ»Fr+Gqûn¿#zÛ^ÈmQÛî±íşÑÚÊ5È_J›¿Áw‹Îæ ¬ÏV"Û_]n°»¡±İ	Ûı£°í€Àæ‚®»ô5yÍ‚áò*rr’™C(z|‹c°_5üûwúOñÓQÌB=åî/«³n¯çj‹íØ]p¨q=±º+4‘¼bØâÎÖYO$ù.\1WEÓsÑZç™¹7ºÒWËf-¥Ab¢5Yò¯ÓÅŒ…Š¡e‚ÈK†õÛÓ&¡õ%/Æ"÷ÇvT[a‚EC ”cÛ
³¶ZØ}Ãók‹,XF%ã”×Ä<(tîüBù1on7^eº9´5ZÜÏãY’¯£ÍôæM|ÛùHJ¯ó?j1ü×ñ@KH–S`¼Ñâù›o:ïNa£´NßÀæÉ‡ñšLGÓ~Y‚ÌSˆĞ•p7˜;;=µNÒ¾ÙĞş#9N·ÓZÍE
Û‡§«—¼€÷pS­´W*Šdwœ8¨OFC‡Tª¹IxDZê‰Dîıı÷ûRš®&pnªûÁşÍì\üúè9;Yò¬)öÄ:å€ğûø'=è”Vtíc£¦Îrè¦¡³uåĞÓŞ=g¥X(r*jßÀ½Ÿqh¢.GAü4‹ãdhÅb¤—ÜÆ¨ğJ6ÈÑÒêd’JA*¦¸4 )ªMáß§ìß£Ÿˆ$ ÕÁ¸ñåîywBvâ”	8{[#(©ä$æ÷¨M{÷”" •nBşx í›·¸ÿ©¶ûWãQ·íEÑÙr8ÈjÔCçc†1•!rı<Íø‡â|5Œs
À ×·GşwÓ)ÎuHé‚K?§ÁE×9Õ$\åqvŠpgŞg‘™€·]6AŠ¦Ò4KçI÷\cUÇš¿s~§àãÖù¢àxµ;g­«aŸ€‘¶ÇìÂËÉãêWaö;™ÒnªAXØ5JOÛì{Ó¨Á/²Ó‹´¼–îr½àJFXÈ¢³R ÃhÇÒ0¼^%ƒêÚ-–†w\×ç>ÉY¼L¶Ë:³zGãÙ8º-‡rmhÜ,Ê:…ê$YÂ@ÃõòYs_‚GGo¤¼ì*>ÜY5âŒp•„„KPÏµÿK×¿3Uİ²ZYbòÏG²`‰|½j8v.ºW“³Nk|5ìLF­·6îÎÊÓHlJµ›Q‘¥¿zMÖšàwf!¨Ræõ˜GUŞâôo¶=`¿Hr±rZ£d±µĞ‰8[¸0–ÖÊôå'‹CÅ»TP™®#€o;>v©[È
Ğ$ÏíI,[URÌ<¤Ê§êšçüol„ÆÄ¤ıN\xçÙ‡çÊĞ8´®÷—Kúuß!Ò4?ˆ|{Ş$Pæ›]#ğjĞ…L?çÁµîvW'Û÷€ÃŠ÷]‹˜®}T7Äñ8¦kj Á_a|Ğa<õpôü™/ÒhSûLÃoËõú'QNÛt6™2Q¯ëDxYFÙí…Š—WJcàAœ~(ÕÂ‚
ü=_n)ııÒ‚5W˜[”íš2şÆäª½ß¬
 ?¨¥™92"ß ç­k³x¯5V´ğúØÄÏdï“âÅ¿ôşâ›Úñ<ìnù6°[V>ÿ—ÅÓ«¨ä*º!lËŒJÕWY‘¯²5&zoK¬¨ök]a-¼‹PÍ}®¯Rê¦¹¬ïĞ>ºce!c,è? ?fy#é5on“FM%éë)İ€£ì39AÆAÅĞ@¥cJ:8¦´ï¾ñà’
ù·ó.ÕTÈ„º¦5²'¬¬q1Y÷™*†ê9&Hç¹wwİŸÏs46şÆ)^åDV!y×È]*!‡P¸úËşóäØe\®ÑÈôµé¤ìÕäjË\/z—"!nûéM²˜eñÊ»$ P~x~ŸlnD£Š35 „æ¹
(\C€aª¸y-’™ú½ÎJö(ïi;ÃÑvÌßr ÚŞªÊ8ƒı×:¢ÔŞÀkU¸Ñ¬ih±hıšÀËA–^£‘¨k©Â ¹Ã‡Œœ'¸0Yíı•xÁÛ¢Ü9àIX1¾LÖûùáF’wVDH¦G7S`èLE\2ÚX·u†4˜³IyjşfÍ~‡ÆÔ,JÀë=¼ub}åwlÇ#FØ`1­YêxºVòwÖÆ»,½ì.¡té)téNz³Ğ+Oçµ'Å8v6†Üªy_íb0ºwÛ³¯Sµç`&T^FÂÑşt™1Ãu’œ*ŠwÔ±„ãÆ+¥…Å;ŒP­4(deSzİ|ŸnĞ¶ÄZcN_Yy2Â”ÆxõÛ¢xÙ™%¼˜³š«ŒN_wÚW½NÛşôÌ±¸€ÏĞPvr½í»gïÌQ.g$úé±§Æ4êc¼¸İÓÌÔ5«®ğ»ÚSwÛ™â™/2·—©ØMÆ©H=w(úÕ%b}¼d„xâ3x$}0Ò\]ÓîY·Ó>´|®Z„cÓ¿W«$q·…½V€[p=Ë¨/Ëø:Bî¤Ä*yóQ·G®šqóñ"²…ÜƒÙâÜF‰Êc’(ñ/òMæ&ÒÈI*›?À)2má^‘pp"'¿æI%J9Y“>Mâ0Â¢_;b-4‚6š­-àÏnÒls	v‰RCÎD2@¶x)ö(N|
X$èÔtO#ÒUø³M,6.aÏÁ\ŞVJGw-×v¥}}—nÃ:ùeZµ»tÒÊÒTï»ZIkyÂe	Q,¬lOA—Lëdv‰;®ó:©Í2©lX¤:vŸ¹ÂU’6i­ÂöZ“Ò4ÓÂÛŒ³ŸÙ[áigÑ'd	ò›ˆœèüç1œeô@9M—ë+ã=¦¸ÃæŒ%jbK'óäs<› KàóÃær»ÁË.‡.Cm‚ˆ0y–,6BÌÌÈŸíí|.©z
Wî¤ÉE‘M`›W½qwĞ{ç2ÙïhÎcºÊ>GæùBµÊ¨kãàÎ&¬y?†š­ğ"çøô©üWëËãùâ®í‹+rÁOş¾\Ò=ğw„R+	ÒÍ0[Y8)gÖÉ²4«;¥$ù@´JQ1ä¿Œ?™‰A—Ÿûøš?—ıoÄ +Œì†z ÎE¤·ÓhãÁÄ‰ïÂ‰$Ìt¸ù|qî,¶Y´`‡×ub§Z[±4G@´Ÿp…¼{¶‹ôS<+Öá¡ãô vàşxÏ9èŒ²¡(˜¥‹¤ÓèMõp²?œìf¶‡“ı~²³•2f‘öœ§1ÕÑÇ³Î2ı9X™?şT‹ôwùS)ŒçR
î)Ù•@ºU<ƒè–uö·Aİée(NdK‚æ"^]S? £qÍé"‚Œ£rŠM#lV
yÙutŸ,6h@éxıeÅÒ©ƒÃ^î; `A©´£–Ô­- 5Á2Ò’‚5-$­.)à˜àÖÂYZ[¢(ÓaF1Òçhö‡d1l^Õrñ7÷1*²¼*‰›[›'Y¾¹›QsWdƒ0"?ÀX&µşóË”:ó­¥?&?ÙçíÇ„0EîŸÊMi­‚ü†ûÁÕ,Ê‚´g@Å’Õ~†>tA´Óé¬Ãk3şÇKÚœâ·˜Beøøa‰—÷¼”ºî¡ÕÁ†T^ù;`]³Ò’­é¡Ry,Û"u×`y*nëssÁ¯,æÀ„]jŠmñ3İ?“%,mfDÛâçĞ›o9;`ş|ØLf0Åò"øÊ˜õ%ì¶Gp)îßï¶,LI]á­¤ÚÃ„Ë{bÎ Px¼œªì[PÒÁ?³¿ü Êá=5}¥çç^˜+#'fàªS£ÀÖ7ä‰ox*hv.úÿİ€ûXÉ‚ -Ş‚’^=aİ"·ä¨`jßlAHGW×Cwz˜qd#nYÅôCx™Óœa¤jnEò`8R^Àî;Ó8ëÁ£¯»"oòz’¿‰oß§äøgN¥N¼J‘_"šØ
ĞËÎ:Ö[FK¾ÛÈİ%÷zák	!¦S2=[ ×ãŞÔUï½Êæ~t¶Ù\=-ó…Âhíšû”ÇÈĞê"‚)VœÂ¸$"ÿ2L§™da9YÜ5xµîÉŞO$ˆÑ—á8Ğ_‘ó!ıÚWÔtBf5&Ì'fŒnğWÔ×š©Fq¼ ¤Äô–—.-l_Ã-z-¹›`gïÛ7Šm¾îş°-6g ‹„³nƒÂß›T/2äŞ`ÙF&!ï¤¥ö«Ñ˜µ·„f&„¸,n}°š¶ş³„Â8Yß¬„r¥÷êeÍğòöîE;•ø4â”ñ¸ ‘ –8%ñbf8Rµa]Pİïé6ËÓÌ7;AöœÕÈpØdÂuù©İ«È~|Œ›5#¯™eõ—ÿğôÚæ±‚”k›[N¶<…•u{›QkëcÃ¡Ä–½¿İädò&ï^«§u²ºfô¶l(}Zõâ¿şj_ú¡GNÏåÖîh™¦›\â…g‚] ´5\ÔhsKº†2äß¦8“‰ˆ$’„ÂƒÔ£Ï…Ÿ…€ÎÒé6/Ÿ}Œ‚/ÓmyOl·è†5ä‚}—bÖ*º.âÍM:CäX«ÜŞ½\‘";ã×ıöä²?\v:íi+~”ÎYP8}Ï5Ù—¬9êŸ'´Vû¿¯Fc¨çu÷ò¼¬I™â.Å©ÂÊ-³-óì-Ùàñ\·9òÀ—>ÏoÎ£uqóG¯/qz—¹Ü.˜sÃN–ÚQóè¯sC¸PÛäôDà3Kï½?×x–²åşùqí}CÑòæ‚ê¸™>ş»…(±&}kÍñW7V$ôÂÅJ'¤ª^Qn)É]hWæÿõ°â yÛş…Úà#t0&F%I>†MàÀG¤¨p†@¦AÃé‰çşÅ	\ÆËq!TşP{üRTöãÑOŞÎ¬Àk-¨ì#¬ªÜì2u ­à´y–—C–¿m†‹œÔğiX¬mc‡Ä%ÑN2WZ)¯ì›ìÈ:qòĞAU‹*WœŒwf·–Éê:¡S®¾¢ßá!°~¨`æè3d–ö„OÅŠÓkûş­œÍ¹ÍJÛö‘ùã[‡£J’wWÀ•‰%áÎ	ßğ|¬º)l=ÛÑùn]Jl} ªVË¦šÎ¦#	PŠŒ¬´–¹è_îËjNòõ"ºÓ¼æí!_b•X0
Z/’åˆÚ¤kû‡÷hÜáÚXÏí;Kš”Çö#	rúZí^½E1ÓE´\Óì]‡ºsÊ‹"†E~pônÜhs¾‰6Ûœ°åä^Ğyò×€o)…_”šÿ£4ç;%ç;S#=^_´‹è3[R¸$…¥Ø¢A·«³¯#+aÈ`›:àÚ¬ê:Iğ’OåÂŞMŠ›åDçªh$´…­pƒîÙÕƒæª5Yyè%(BƒU£®Ú¯âç¸?hÈKIşaDàBG;ÃÕ.móX¢Šv¢vèkÌ±1=P¶«şêª;¾!Ê^Ï¶P¨BĞX‹	˜¿Æµ¦‘Â‚ú3®¼ÍÌºr*<ùô®ÎÖq†º­)Æ£¡1v"³òî’Ğ+ôûbÈª­ÓÓÎhÔ=éöºãw“³şéÕˆFkÛ¥y¼š)µQğhóÅë|Ûí|O«4äY÷ø­Q{nh~3Yk„òkCî
øÍ)P°Ö„¥·TTû¢ä99K©YÏ¬‘¶)ŠUUòG+€hú?ÓlC±ÖWdšË@j¥dôú *kÔ–ÊNÛ€Â·;•|ïŠê]Ô¶¡V›Ïp‰Uõ¼¤ªê=€=ÆÓ4f­µ%)†¡Ç?CÏziGQşMi-»2RÚÇd7Ì˜WµC"ñßıîe§=9}İº¼ìôdô%ÈFp“ÄŸôVšÁO§êueª„>yH¤ eäSÓWL‘Õ}„LJĞfx‰Å4ˆo»„÷:FŞœš©ğÃó=¸_ŒÓ³,])¢-å¸¡¤¼¨AòáÀĞkñ'n™À‚LØ ¯-2ÊCJ L0T
°ˆu¤Qï˜ŞÅºòùŞ®ù‹¼{}cè\èâÊ”'Ş–*c©-ˆÂ*R½ª²n€Åì7ß`B´³¶ÃB1á;Î«~fê·ªİ€§h‘^Z)IÌ·××„Ñ`¦Fà;,³?úÇıgçm]±¥á-]“,‚,ş—V±IqÚD`Âñ~±¹úÃ­+–õo¼…`FÀ_É¾½mWÓşÍÖ÷z]Oc“ğc{Ñ¾èís÷=Î$u¥SÌk]%mWZ…ÕÂş«¬çR°rvÕ+“ş«”^<[¸‚]ÍhÌÏAì¥>N§WŠöÑ‘ô7#‚Å¹Qmä	^‰Ì%ôSX8GÔÂ9Ã1zx\	+çèñc})º2ı¡gN>Ò–œqÎLÉlgÜÌ½7?šT[t	å?xw›éüpÈ0;¯G§­^gò.‰Î	+Ú<šZçYæ&háÆ)iùMœÕEò™/%†kpqí­DUWR4]—çq9GÌá‡›sF«Q[0‘C9ŞËh‹>1v:±Î0¨^9–%ëškD¿áì¨Ö›É as`-œWi‘£ôîŒÊ´Ô†û±õûSşıĞÄ@ıMÛ<ı”ñ7Õ”“O—Ñj¦ÔP4Ää‰Ù´ğhEh×B¹±Îr½¹¥6Ì¾%‹é¹AÊ…§´ò;x6`İe“m—‡†®)ÓV¥6GA¶”¶(¶²¯©SÛ,Á
kxéÉ+›ˆm/F…Ü½4… œoé‹›Ùš%6jÏîÜŸ•+OÉ¯Îğ°ì”´t3‹—éÇ˜°H´í‹Ò>:ãBÜøÃZ2à_~¯XÏV6‹Öh@ıœMs,QŸ%
±ËíXä©„Ss™ò6pšQZîø	JzÈU6u%µ– .À¸Nò8û˜L©£«¯<É'›–@×áˆfîğö[ùFQ–âë´qƒbÕ[Ãï	'ŠÀ¥	?uÿh“AGSKKŞ§Àçúòòv» ëÜº~ìxïÌğÌÙ†b§¡¡S8ÅÔgOdD'! ×ÿÜ&ÓÃx½ ègj ÓœŠ8úfüEÊÒÑ6æ?mß´` Š²Q›¡cÃ$™?Ü°Š»ä…¨å±İ¦ÛÀ0OÖÖí¹È!‹µàû\ñæŠq4!ì¹ƒ•0Ôƒ”Z;~'öoßÃıÂûØÕBeàìÍğÜıØLVÁ2ìKT —¾dÀ¥6!«'W£îeg4šôº—oôú~›C¼ë¼—¬>+ôÄúÑˆW[r9a‹Ï^ó]¬>{É_vù¹ÚğÅÖŸlĞ–ÅD¾›Å³ó,†ëœyà(> Eæ±	®‚Ñ@Å-²ÊqÑˆ1§›	\&Y<aş¢Ü§Ô´Mj^ÍÁÔÍ&LÌ'9Ã³B•“é2¥—æ‰-=‰øé°-ñ¥¾e®ù(Úxoği<—è½f34Ÿs¢tçÕj.DYºAÉÃ–3Ñ€Ë=d©¯®crŒ€&cAÿÙĞ2ú3¤-5ù%Š’H)¤’û½BE¸@À¦VÊ*ÂzÒ0Øtÿ3Å;ú+•^ªk§TRå£×=ºVÒ>¹@•
yˆ9 :.ã©ú‚Á!òæüÔøLÚpøcŠ3lŸûV),v¶2Šr©6NbLU aİ’–ıs›n„^êø(ä3S'Óî
ÎÁ
™¬CL‹3Bš® sw»ø]F‹a>TTK¡Xv]“jÈ¥îIØãhx
ì·½äû>(h9àI@Æ÷ÈåPª1S§¼íÇİÓV/ ŒjòÏÈ5×è}Q¦*DJ`ÿ‚x\ÅââLÁ$ÃÈwSáŞ,ffİQhs“ld»>Ïbà>×NwµœÜ¸Êå4ÑwÇ#Q_(¢î`b)±ë¬¦ÙíšœZ@|\*0*—Óu2U€û]¸Y§¸0}ÁëNm/Ùinù¤è2åPOis|=k×ÖŠ/,ìjñˆŒ,Jš0ZÜw¤^äÇ‹9rk2½%1¢Úã8¤26L|BÚ/Sr0]Gˆó¥@äÛÍß'×Æ¼’Ô'ÒkßœÚÚüQB³s1¿£&_h×q>ì_j/Êbp‰½–ÜÁßaÉğh(ß(·˜R°$.áÑJâ²ÀÒÇ@
ŸG3.sÛ¢7ÛÊ€ôqì;ãô¯f´È·cœ.˜î6€]ôXj-	ÓÒŠ‚6ˆenÖ;UÊáv5†ˆÎâšZ8šŞÄ³íBÒ^[Ö‰q.ù‘®È—¿ıíoGGGöoÏÿËóíùsöÑ„Qûíízf˜C‰Üp‡ƒrnZÿÜRn½d4Œ9WççØ[¨i4\’]º~.èï¨Õ˜P50ÍÃ]öñ2õÍ·u[«µøIÛ×©àO@4ÊûÓğgg­(àŞ´üqàæØ3”kø³¿v?»høóo­msihÊ'é£}àÏJ¤å£s¯’iyõ»I¨b9¸—İµ8®bvÑæøÊ
Òê8øª´;ÖV~UZşx´bQù8ë]ì4Œ¼Â^CfQB3íÌ¢ºQ†³{gXËİƒP?;ºg+û@T³Ö°–á³Úø¯ŠF®ô†Í†Ñ’¯Çvƒ?:¶¡øË¶;÷T•k{:§ÃÎØ¯îqiAÉˆ.“Õ„"—Ş±–©Â4½K`\?Ñ (enÈâŒ3Ô³ô·›ë”¤hÈ1Q¯ø&ËÍhƒµãmqw5M—»·8Øà÷ÓÎıæ´µ‰,šÙ«Kå,"·cU2)[Á¬«JZm–)„:j—sªì‡Å†qÁïSš¹OX»2Y1dš6VÔÁhaÄìi1<Ôƒn‘®®kNÓƒ—ŠbÛ•Ì$X–à¤È;Û ŒdíhYè#ÄQí;+Y·o©BÈ¢“X|Ãhô	&Àfo¸"ˆ^n—ïAÂt4²QûÆ¾Ø©™W«Eå†n!3$¿wåµ—o¡(Ú"vHµëC°Á_x5jL8ãøÆİÚiå¿§¦2¤Ó{ÚØ¸¥W^Å·Îqõ	«@m-»_]ÆñŒ¬ˆ2RÚ‰nµ^k8Ğ©‚:û†İ·­qgÂT}+Q1ªbfv/^PºM£*oİ6Tû«Côár) Juı¾cu¶!Ë´ñ*i…É£…ò•MêW×vˆ·^u»5Ûı³W¿â0m«|å/úå3ˆ³°ìòñ Éª±äX½,#¥ıRãâ™¨×–Ê·à….ñğĞvH¸I"+›E/í:‰rĞ b€Î{Å«¯¢|ÃSú ´E!!°µ,-2!&EùÅö‚)ÑÄ§¶‚Âcÿ:öRéZõ\»³@¥ÅW±B‘w‚¯¹%;¡ŒQ²³Ê‚.å$Šå\Æñ”c}8?RisÈüÂÃ±şp¬ÿÉõKmo©ªÈ,¡¡KíR-Å[É/R¸Q¹Ê­)¸´±{ùëØüTEÙüîõ†âÛË€Àg¹Š>ºB@X}’u´6Kx	Íƒõ‘¥™JµFï}‡§=GáyÉ/N­°bÌS =•ŞÛléKu:…ï<xÉÍµÕG³ÙnxqéÙé²šPçPÙa-¢Âƒİaƒl½G=b¡,cQdG\Ù×È¾Ù|/rM6H2>ÀÓ¿Xañ-™8\€Zu‰Œ^ßÈ›Š¹â‹JRvñuQJØá>V/â‘•‘ûb5GRLk;ÉÃVG‘Ó0xëõ°—":ÎòHCH	èvÄ©;„Ã,GK$úôšæ4^,´#%Ø=Øünvó»¿ŠñÓd„Â%×•)jÔšGÇˆ8r¬SS%-Pƒ»•Â‚I‚“p)ÏEyêÉhÄšƒaç¢{u19ë´ÆWÃğªdhbŸsdµººjmı‘¾W¬£ ã,œ÷/uˆrÂË,¹
6áŠ1CY*{Øñ‘j7ˆ7ä ĞV-^©øMMwÊ#é9øõÈëôw˜|aıxƒ$Í.ªÒew5‹?ƒÎ˜™ÀÏş¼~ğŸÿy ²AŠß‹ltn®é8l¸Ìªåù{FÙì	ê´x”¥¨ß"ß‹‰d-¯e:w`·Ås"lŒ (÷qMSÏzäè±y»ÃÓ;4ŞV²¼Ö6òrÍb±•Ç“?ä¨˜'ôŒÊW7GÑgüZ¶/mİ=ªEÇ,õó*TÛ
üoóû×İ±-¨œG;rôùi|„OYFÈƒ¡ÏÉÿÄT@ûódÊ€M_9š¢}6ğâ—LÒïîÔğ‹—t( @ä¿-I1<®½9´.'N{„b¿í¹eIñ—AO·99.ğú™´K*Á<œ[ i/¢Ï´û D´ô¸9İ’ÎŸ9äöu´˜³ùnÔêKÃM¨YŒ¶½Õ	˜¾ YF<b¹È—\çF¨'z^º‹:dR›jm©œ§¥•×+m¸ÇjñF9»êY\ÌŸôÇãşn§t‡9oé6<•O8º8mÓ &óv±IÖ‘0õé2­Sÿ¶ìk5œò.Ã÷orØ.ö¹öQıMQq±@”¤³ñ£2)3*Y`x"³E@ä¨u?´JvĞê•?yD–rp-¹ön@UOõ‚”¾@¼ OwløîØõæ§õ[
Eït¢e{“ˆÃØ‹Q3ß èğ,‰ÚÑ&
WäŠ^^Dk*ÓíŒõtjµÕ…nYâ×>„à#¨‰z-ÍiqhY êQ*kTÑ•ŸÅÍ9aRøDv"îéój—`ˆŠŠ}ÑoÍQQKâqIÏHÒì¶™ÎÍ¸Ú‡My‡ÁYüUë4ğF5¬_C. vûn9TÊ<TT<"2kûjØBÔæÁ°ßë_›*g§Š–"ò:eâ§ànõ\F·–m¸ƒ{hQ<(6&òÔ˜µqµÏüÛ=¨,W
@áí”t*Gëâ5‘‘É+Ş æ0¢4ù)ÙÜj‹`ˆ…† EÜíV}UÇ#·P.ÙuŸœägYú?±%Ø#ÛBôs‹Æ	‡™ÖÈQ‰$«4B?C$NGß§Iwö¤µ–—.ŸXVtaüiÜÕKêè"åE¿Ë¡aÒ>ïÖ k¼M%zİMvë‹AKJàÑ5^Gk"çÅñ¢ÕÕŸdZáÀØäMØ–d{vF#çuÔĞ®jõÎçiŒ·Kµäz•R°Â²È«ğx»ô¬cƒ‡³ø¼ıÃì˜Â)ËZZÚ”0gÿ“íéIÖ¤¡lô¥¦•a§&<SÈq(]Ü‚H |Cí‰~q+Àâš@`´¡9lÎ’™Ë±0Ï<ƒa™ò?^Ö\[ô©,U¶Èu7ÁË’Í4ùË&êÅ°¼¦ª‹aU¡XéK¡ëqÇ+Ò;‡+ï”»CğXÎbùø%Ëµÿ|Ø$^Yä“Ü°•X-Ú;ãÔ‹s¨`°ÒYƒ·„` ?©Ò«ö¢vp à	” {¢K‡3iU,¦ér™€Ô;g·¥~:cjïPoK X€|[¾Û}l'ù0ŒSÃàøkH;Aòn.[¼â~á¨ñ»…²‡tš”6±8`€²»‹uÜã—3œ²ÕSÙµ¢ûèQ‚~V;~ŞtŞô[ÃödÜ4j®Tg½Öù¤{~Ù²@L£ÎxlÆüÅş¹+ã´²µ»cÊ{oTwwŠë6şºCúù§¦å»J_6ƒxÎ\2QÌkkú²¢aV¾ÔqÕiš…¦<~*±FLC‹5Hv¿ T
*&—@¹Ü‹ú"úÈ¬Ÿj)ûW”-…x±L:Í¬mpĞDÖ®·…âYY¾Z$TD¬¤a^•¯xs\ôYòk{ğUO{9l7<Ë&ZCˆî÷[=‰Ş>oiĞX®O/JûÅX“½Q¯kïØ¨Óİ:ÈÀ3]3gE²}ŠK÷R‘¨;_5Îq’+5‰—tAÏ!.HÜBX¦D‘	±#ÔÌ”ÙİYóíb£ğÃú>ô_‰[HCàı8ÂZ‘#úœß[÷KÉ„>ÒºyåªòVù)²ì#‹Jñª€bZi´‚…ÒNYäI)iCNÇâ:—íÉ°C,c¿Ë™OH³g“cjš¡Ÿ_„	BÙ%'¼”îj|ƒ=Ş8,I=!
ìÌç¤wŒ@²»œS:Fˆ¤"l¯Kéná[I%Dc¢ás´r%%X<3‹Ò¦²MhÓu›vA¿äœÛYšRİÕÖ/ŞÈ**@¥.¢5êÏÍ¬¸Ü¨W‰ƒ…F3ìx³‹7év1u1ßÜŒÔHƒƒş¨‹K¹bzUûÕ›î¾Ø¿œa÷üõX]kª6v©ü"4»úø5%Û÷Éæælş.ğ_›Ë¨R¡Gz±, %¯9ÿtçØˆ_}á,S¼Ò\jqŠJ»®ÑZ…õ\õ\R•¹±iw€æø>R7•e`é’åÄ1Ôğ´……ğ+·îI_+Gâç…ÌìC¥gë²{·`dO×¿ì½›\ôßvŠÄ‡îÔ£×ıáXJYn’ –„E«íSxóû:×‚7Ä§eô!VÃ-¯Vd§1Şƒ
T¤€6àú¼K·Ø’xûÓ¼6·‰VK€Bx¿òJ¯ $›,ZVİ½yDÉF 
ÃÖªS	Ì)}éu7Ø©ö1»/TVq|)—@–c7"`cÁÖõ¥`7i•oÚ0ú³ãJÂw%?áºDô\³÷¤Dùîî¿õÖ€WVJÀø3Kû«îŒ¡/2;öî.?\˜İÿpußzâ¸»¡åtaÒñhiÕöƒ'AR1 0;‚˜øÚ¨NÊ÷]=»*-A+ç5N©7ŠÁdúûl-«»j'ÈÅq§ø	aí,\×mÄ‰cc…+Á†µšz5`›ƒpgM·ïÔ
„	ôşú~3 ä‡CV7u©k"{ïçj·¾uŞÊ›Y$éPã£¢‚}ÙAe/ıø¶—ıÔiâ¡é2]xTÖš’ÓÃ^§FtË™ÿxÂYŒ!Š6õƒËt“ÌoÁLl„ªzp(9 ;¿)}T¬Ä °KQ[gJ«[Ÿ½ˆmO¨êóÀÓ¥Fk»»–—EüX½¥…Óš¨üw<ã2¯r	ªì®
ïT˜wõK›4oQ§8$¬ÿÀÑw7ñ2Ä¡Ô–¾‰µ|Ÿ,liµ^]´şœĞàÏIí[ÚMN»‹ØÏ‰ûW®š$9Aj‹ÃH2,÷Û³,Ê+'
ë¯³&ßı§teÙ	úm˜•—q°0C³xé=„‡†2Ú<mĞ 9ÂÚ—†m5Çb®WEV_·ª°CßY?[GL GéÀåÌ<täfë%¿íƒÃĞGï‰ìö¶Šµ‚¦Í–oÇuÏ;BÇæ•'”ƒ¹'€Îğ!åK.77ÃôSí1úrÇ%È³·ÔgJÙYyú:ºggW˜úAH¿ºØ‹Ÿ§&“"‰ü‹Îw–C“{¯ûÏ<m§­Sé?aÙNñ¨-J3ãÇ 9!VÖÓ€ŞjPƒeÈâ*ªÄÛf/âQ©Û¬@ 
÷%®‡^
-½í–ŠÅ;V¥5†~\"aiHá™Ş9Oµf€«|²Ò)€óš@:Õ; XD‰•¨K†É–Å¨L%Z‘OÉzœ®µ%ÀîËáÃÛ¦ íÏçy¼!Ilà½.0g1Má.~^ÄQ¾%”;è‘%ÌˆönR®WÆš52?©½_€2İ¯Ü¶Ø'©[÷¡2•Ô›"›‚@ÛËõíFí*=¢„=¡"tßDH¬‘ÀwJœpŠï’D=P$(d_2¤x‚¯Õ™%àÓª®;ÏÖT€oœ(Y˜Á–ûe—–HÎMyİ«SMë–`n¢Ô+˜_9Ğ…_]!BÍG@¦[%J\€3‘ñØJ7}Ü™ÈŠ{Ìfñêäv4…+UCxÖšeÓà½å6Ñ2O"Œ†)Î’x1s6§H*V)‹N¨¦ÒMNôVSŸ¥ÓmÏ<˜`Ì”Ë¶Îª™%ÓQäRûèx›Æ^ÄëPR¬Ø4«Vwhİû 5ér&e›D­Só”üGÉâBïàPw F2¼†Ş t!¡rÜB·ÚrNs9\øábD«xAèƒ˜VûVä±ÀêÖT\¤€67~€zÆÉzèÚ-¾2XÛô¸Qè7mıVz’Óäñl²NA[š&ç0·5“ş<™çl™‰¬j)«ä ÀÖnD8ĞˆMÆ%ÒOšço´Uûšó ‰–oIşÏ-i:D~¼Õ¢_ò(ßwz§ı”9²÷
Ğ±‚…‚‰#Ù ¿ğ˜9ÉMKÇ_šëCòyœº«õvAÆ$ W³Åb[NÂaüzŒPï¤UÌî!.Aa4òDc¬“º5òß|M’é³,¥âv4*ğ5èÈ®âµ—9ò3CìĞ£XR»I<ö«Aô¦6ßM‡ôıË,~Ì¶æ•»cÁÃŠFxğş
HÛZ¯Êu[,•É¦#¨TXŞ|¥ãj(/~ædÖ?Æ'òTKÿÍ¡Õùƒ?¶¯ˆÚ.×­qà*q †@h;)§•×”£êGş hiq~ùœ Ë˜ûß#6›²ÖÑ2–H´Ò`^Ã5cİîá@Øl×ì y›¼§à
„FŒ0–î¬1Õä[+oRsHqÊ²´“‚8Bn¡!eä?8(¤?'êÙ_go¥b’\Áè –öWİş¸HÃBT,À-j¶ğJ²,QÔ×ƒjê'¢`Â,Ò”tnrÖR†yd»eµDùUŒçà&İ¤˜T?0†=Â%±;L)Œ:ı@R§İBğì-Fñ!œhğˆpØê¤\‹à‹lRìû†gÃD¢¹BÚ®>eÑš–€I=º
ĞBw2A¯c5¥l>úñ:Êo.¢õ·p÷~ÁÙ¯jëh×ñìÔàé^ù¬XX¼ú´Q{šô™?<«œö/ÿ^îß†—ûôøïüüÈ_²Œ'¨Î€–Pg%ùíÛôIùøæwŸÔgÏıƒt÷ı>MçÉêwï÷ñóğ•ôì¯÷·6œûß<ŒcÁ=l¢Eˆ_œ’sÒğ˜`ÜJÇ?oÕ#Qr’ƒpRxrPÊÀŞ˜ÒH;„Çw-ÇÿºÅ–X$¥vø›»A×şßö¬stvĞ@‡e÷„¡z	k
h0<Ø"%©qdI/š73ßnÊf­Ø±¶rÍ“»Œ+gš2:é<€ğ×Ç%áâª¼æı¼ æ	­*L,ìßºc~K&C:l}ùÂÍ!tôTAW¬=téW¶#pÅÜxÓm~%_[®@\•šìÂŞp!Û“«-a½Xx°ˆŞjU¥œ‘/§af©ñ0Ò´oË*q@	ÃC÷'\‚!u¡t!-¨Íµà(BY¬b7‹•[2Ş ™ƒ$GşsÃQ§¡Àæ@¨'0æ4æzs±ß‡‡¼˜ƒÿo¥ã.««NySğ,l0¿-LdßÁDü'¥£)Cufç±ëÈxw.<LtÊ˜G6½Ê>ç~ã2ÔËôŞ_‰ê%W <ñœ1ÓF›GM1–fbCbŞSO£µì¿è)Şš¯LßâÌÏ~!€²í,d·®$¿ûªs»Z'+®¹¼gõ\íW¹¨$st çÕW‘¤	*.®P2İà)Ü#•¼ª¥øo‰%KÎ“ì5w>E^!­$³’‚I,Ôªi×¼öÓÅlœn¢½A³0ò,^ÛM‰Pær#€[ùX3% 9õÒh¦Ö¡ ‹‘^ˆûY3‰XõöæÕÔˆP¿ÆêCzËÄO^/<	ªŠ½’¨ “@o<ƒù|È¾Ğ ÑÛ.¶”¤!²Ì«wd›Àéûù[åÑ°Ê`-PĞA¤@ûïº|Ñ·W´öÃÅL
µt-]ƒ—ó:Íè¹‹|ÚŒë4øqÆ£ßnâ"Y¨¿&ûÈoÉ¥¤FDJÎ…hÓ‰sÍcW4N ‚jj<mlNˆàC˜ò(»æ‡¾Ñ‹TÂ÷%9b¾ĞK>¦))ô!=2Nõà·È’>hˆ!+;¢ÔüÀĞüOlü/=»¶9ÈpÂğjê€Åt–òñÔ'dºÒò4 É‘êÇ©½¶ÂÄJ6 Z”¥$Z•Ã/áÅT å–'Ó“ÊË‘Ò³iæ_Ü§t}äşWãô<‚İzzZ³õ¼67†Œ¢0WÑ†0å7’®”§‚ÂŒEt–Õ¹‰—k6Rğ›¦4eŸG®&‡¥EÆşÓöšäÁ+Q¥•sR2ü$½O†dœrÂèAK?ç²Ğ<¤Ãs^c™D]ÒoÓğhŠ‡Ì?·Â¸í“”d ÔÅßf›Å@Š¹ö•³Í’¢c_ù@ıBÛ<NGdÛ¹XÓ×Q·YİS¥rékõÑı}G¬›¯[Ùô’1ª²Ì¶Ê°œBÑÑëÒ6"^èœjn};’‘•Ñq¦B
!¬%Ú<ÓıÅn\DZ2³zÄ\u˜ÿÚyõñºÜœšh­¨Pv>o½k;m¨ãRÃîêVÔ¬€
Uj8›¬"—c÷˜­0½(-g¨¨ÃÚ
íN¢	€8PE6Ãa§Ñ‹ÉUv‹öÖn­Rßço¡‰ˆæöp2e©Ñcz½ÎR mqY:ôaÜ¦‡P%õ;7¨æ:edæ²)ì™"yş)A f³…ğL‰¼PëSnØß½pbÉª]Ê4Ô>š³jÉìteWËÂTfhu–Ÿ‚üfÍxN€Á[èççIÆîtxÅ´QSàÉZ°ú˜“õŠ‚èİbk©G½üˆ­[„ó:Úâ(rg¤ˆzÄGnøÈî¯7õ‘Q‹¬·ìVúR·¸¹G°,ï½0Šîj«Ôö¸QÜ_p×±Jd#YW¼'ùÌMê 0NA*æıßñò´]ˆs†ÍA*;‡Tt«U²/Wó<»<UgÙrC`©á=!\G	<2ákwzqÇFùv4¸Õé¾ÇIM:NÁJñ”©ìÃó3êN	§&v²DµŸ ÿĞ+9ÒeíÈkØÚéä–*MÚYø}kØöÌšU‰èšÄŸ‚'ñç=óäùe³ÂÖ7äx×óœW9ËC´†ü*6ztÖÒÕâ–²bÎ%Å3uRˆêûòñíôk”È…>´İmõúç£Éøİ ÃWWY‰K±ÄÈ’8°[´)n¢|@–"IjYšãtF™Òg„é°%€¼˜ nıŠ*g‘æ8—C„·KP
d÷EºJÏÒl»TQºÉ!ª¢tS/
#µ`±ôÜÒïæ"Y}ˆg“%É<‡Ìj\à<cTÍ-ë2Ä”N!D+ Œƒ·¤† ±ÌX\d¢º+ÂôOcÇ\±¯V¤:£42nt¥Óu2ÍİëU[”º~Wû¬«xùÃsa@vIA}åÌ´š`şT¢Ï§ıG˜÷&ÉÇ&ørR3M×·òo‚Ø¿ó¥RÀ<† Ff·M§ÖuDèA{ºHÖïS²NôâPê˜ÆÜÌË²%—ymJd	¦¿¹Ù,ŞCÀ¨ÎÉÏÑÇ¨¹%}jr«VØwi®@OğªöË6İÄ­íæ†E‰#ëÒÌê5ˆ-î¢OéôáK_;¬òÂUTø–Ã—M¼¸U28ú5”ö”_>‚²¤%¼-V¨í5ù“l§Œl@³ ¼;Å™ ˆ7ÊGÂV¸yÑúaò¶Õ»ê·4»nn`ÓfÑ²¹MšbŸ€Öƒ/1ØÙúH4× ¸„ğYÚ{®ôÕìbŒğM^äOªG»nÆõj&¾Š­İzáå«¶‹`3kèâ`Éhê®œùš?§É*¡IŠ³E±æwfa1Ôyâ#ÿ´C%‹Ñ»bË¨kµßr~Ï*v1z»×¹¾#2n/ÜKÍ}‡C—ĞQt—[Šz‚şy2ß\­²A0K=$åaHõ‡‡ümÙI#ªïv@šï¬68j5ãÅËûi¡}Y)¤c*ä§l OO^½MxA70åh¢YÀ2¿†9ELMdÉ¾ˆÁæ.îıĞ¿o‹ÊUm'k„jíÍ7ŒÑkÆì;lYÙB7mı´µl_¡— }¢ğYú-ï¦£¥)¾éßÑşnz6K‡À!ÜmÙ÷»ÏKDu,Yª‰ô‰7†‡gb t;mçO%¢^MÆıÉy«×ëØ/* ßh ß|ÛH¦æ¨ıfÒ½ğâñ3¤öïß’ï‡‹üü6Êòæe2"íï´'£qHzƒâ¬EØD¬"ßÍq¶Lò…ˆÉÉ<Î7€“½k~?ì;“ÎãÎğ²Õ+Š%{M?€XM!
šƒÎğ¢;‚NÎ‡­Ëq§íÄ µ´'£ĞEkò:ß
²Â?ı+¼m¿5j.©5]å«]5cÕÂ^ØZUBbh¨¡Eœ·–ÌøÕRˆq#c-JÀ1-hoÀ¯€ğo'ı—B¶!©i¥×@û¬²nÑ ·(aµS-ÀÜ™‹ñøßº=X’3Ëon6v–ÜìÎ·Š\ÂŒ*5Û_r@‰ˆ¸Ï.Ñß\ğb,fÆ¼ªæàuÜyÏ-V€}ë{ÛmwJêÛ¯‚‹N»Ûª2%ıÚé§Õ"fÂ›7¢!ï¯ånp­+Üi–/?®¥ Ş¦jÿ–dÛD¤\çÄôºo;¸Úˆ,kRNø—Éì_Ë,Œs*ìKÂ|S‰›h½w“^ÿ´Õëş_j3é'ã×ëE Zë,Ò)³rkËeT‡Y«SJ5Mt¯@¿AŠîÎwŸ4#çù¹è(È7Â «n±rõÀ××ë2¢H
EÃæ&í¥Ÿâì”¬Xğ>_Ír`¸ëdzaÓx»@åFüow5Oé>Ã¿^²oÑz½¸Å?qA±úuÁLö™¦íâvR%l OE¥¾îúE‚A!,®úÇJö¹Ú—QğäÌf¢7¥«şjSjJFêÉÓrK‰Á¸Dıá<´;[9?íÎÎ[ËKŒ?ş]_[Œ?÷1f)›“©Ò»«iše0Ğ
•ÜâB,@­"Y:è­>2ş‘röWdŸ·“R¸ú‰Wöv³dyß$+®{õ'üñƒ§¹ríaÉeÿƒÜ^´ºŞ’…¡Ò\Í®d2É¦wÌÅîØ—>>8_šW‘Äaâäh¶J÷Rº»–²F¯[C«<¥úı…	4v‹.	áws¯®ÁÂIê(WVîÈ§‡ }ĞìoDÿ$Ïµ>]Ö„½¤)"ˆ°½dFÕÅ[Ğ“6Kô]şÃ/FK›qÍˆÄëvm´Lpj.“e<Ùd–rL™Ù1±şƒç®¡õ›Ó°^¬·›ÎçMñ®w~[“ÑxØi]4°Uƒ,_$aWYr–fØVë!@hô‚yıJÒKFñ¸vĞ\³BÁnÍF&Æúl]ç¼•g½Ö9½t˜†¶'WÃî¤¸pŞœv"Éõ*Í¼Æ"AÃD†¥	¸480Îş8mœzü»¨Ù¬Õ½.l÷44¨+ûM&çÛ…Ø)”ŠÓ èY6¹Q9ıÜš}xQJmæŸbÂÆ7YJñ´Š	»óS@½¹;~8l	B„@¿W¥Wû¸”ï¨lw ÖFÈéü(@âÑ›}İƒV„eıszìûÒy\öypé»Ã}÷ÿ°cääoØ^Ì¾ºË½ı¿²1~0ÿx0ÿZ(O‡}lKü¸üÙ_Âw<{&øãšğÊ…^¤	ş¸\.à	BÇ4‚?_‰ÂháˆüùÈüñ»Rï‰T¡t¨
b…¿u˜ğªHü¹DşØÁá|æ#*z |ŞV²“€Ç"›¹GÎ|³¼†@c¿p€€¼Z²*_³’€;ìzNƒTí¤t3ÊV7²&º9‚o¢ÓàkŸƒKn…äe)¿VH/SB¢ÔPw}èlú(Éi|-,!Ñ•œ§ÔïÓ[ş4ZÒÜÒÙStI …ë "­æ–1”Rœ4¤Š9¥ûˆ3À+‘YBÍÏMÖh€`…é©©äŒF8GŠ$ÂŒğ/|^ğT!şàcq«©ÒíUUñA‡g'?t9ãİù¢[›ƒ®†a-ypø&›ÔŸr¨W¡+#/ÏÎ†˜zvm~° :¡ÿ|¢:+–<†^?øã`‰Òˆô–ãt‘0XX-¹G(õ:çC3¡™ÉÑ?*ÙW°ÎR°9<wãIOorx*x”û­lõ°§4úe©í¾–¡í6(«Gãîé›Îpf¡­Ñ›‘]kÍâ‰ÂU~-Âÿ²°ò×µ¾…Ñáñ1YşQ¼)¬îŞc4WvÒÛğœ9$^+qdñ¸ƒÉâI-Ÿà’€b}"ˆrôAÑ[ğ7
”9+·ÄQH²?£Åô"ZÍ Çë–H61%’üM|‹ÎTEde³ˆk{QM›ß©;ïéEÒşJ¿–§8Õ×òÒU<yWOíş÷—½~«M×âÕ¨{úàü„Ïƒö«xÂùü"0äÅ6O¦æuû`‹DŞvbÉDÏ-…éÇ(hMˆjŒc¤ËüÚSh'İ1Å]¨ğŠü¯—5ÃY$;Äš›˜Ø†•6×ğAáIKÂ3”Œ¶ªhx;Î§YÂ×
¼¡ê  	Ùg2,ËÕp¤ì^¶;?LÚÑé°‹içvu>¯dsßi»:?z­Ëk—G¤²Œ/m~ÊM|@•°˜BB¬to9~k}m¥ÕèÂ:,5@&"J:fü9‹‹tß¤´D*lÍ¾‹+W6 †- °+…uË×P>A•Q¶_u­9û8kU8|U‹?¯	wÏäÕ/¯8ù½mEJß÷Z‘¶r¾èŠ”ğÅW¤J&œ•}+Ò:k®é¡ËÚi†/_òŸö]*`ÀN\
Å˜U¨±G£TÇ·0WpmáÓÅ1ó[ŸÃÊEË ş”™vûoD°áexªºk*óãñ®l]µ»ı‘Ï½òêòÍ%‘F UÅåÕ? 6”UÉñX,ğ_%ŞĞ	¼Æ¹5D;øîÃSÊyùüŞwÙ•İ3óú£)z á÷%»›@Ü¹à´ë.zXõÎİÛª/‡%øb«ØŞ1N>Ñ$À¦üğÂŒˆ°l¹½'<÷u2·å=Oæ®œÌÚ0ÀÓË0,á'öKàu½ğ\İ˜ö¼J¯Ä!µËrgÄÛTé°ÛÃ üÿ  ÿÿì}ksÛH’à÷û´î.‚:ÓÙ³31×İv/%Q6£%QCRöÎõ:	I“ mkgû¿_eÖõÈ*(Úİ½kÄL[ê™•••™•b^;•ÃÓÔ°\vo\O­¹V~‘¶¡•œší×58—£İÁşTƒ\­êøí©5ÙŸMõ	˜7²³6 ù«ÚZÿFa¿‹åØ‹"ş¡ğ#Uˆ$7@¯Ç-qÄÃåGñW_zTp19rd$êÆ`Ì¸mÌ©bó‘ÚGÿ5ªyÿŒÚGï¯gÍú~|—B*Ûsv…Ö$äÖ3­`À&­),Û÷¹÷†ÑØñî(}¹_€le¨W‘(±zo2¤Cô=xİá±79ˆ/>ˆó†´à ‰x”ÎØ| :h4³^¸~ıº?Æ?û§ƒ‰Œ	û·”Š3¢5oÅk ^¶PŞ65*ó5Ã)Oc«¦Èá<nÎ›H‹…&Ö
»X*4Â œäÕhpB† ²R6î è»}L!7Wkõ—×@–8
–ÂFÀî3·€.){OØÀ¼å"›¥ãû4E›1Œ"ı-ÀÔ Ğk¼œ&"œ¥`ÛøHÙ*è“°kO"›uå©ãªQµñ¾Kï‡À»ú`5¿GDd4—¤LšÁVÙZ»š,”šçÄciîÕàÒ¿U Zğ2›Ó‡\àF…p¹°"×&TaİÆD>B	 Ò¨RíšrŞ|Å`M;hºÊ¤¹<+îjiÆ0ê-Ö÷I»û§ÛğP›Ç,ò ¡Paşü¾…†ƒÄª—ùÛí˜ø™¢8ƒÒJ9=æÊ€°ã‡](˜¤u·xW¬‡áb®¯—7lI,±ûôî¹ınå)_újÉş)üç¿²ô	¬›ÒJÃ°Õ5¬¬*’e*²¿ÜjíUõ†Ú€UÆ	Lê8ÿ|’2š6KÑÎZÔßÓ{úy|6h–ë8™} w—Õü´H0ÀJ›‡^Å”¯@§òB}©ô%[Í–‹·9ä;HpÀ¿·(Á½ê ÓÒQB¼í´´åAõzQòğ®ÆuP{¢¯xÄø®˜$ƒpeÌ×î°³r49‡Ün}×ú÷"lTñ/àğÏP104ƒ@ÎA"Ãí´øKsWˆaˆ\mãÃEoròfzÕõ/'Ö¿ü¥Óz]$È´M†W­ÿT¿Îûg“˜%®NÙìƒ"¢ÃÚ)xÄ}œ©şö°õÑß3öúççïYÍ'êGMè	õ¡|:o«zõ^Å!÷ÔŠ¦lËM¾„%ŞŞBF³?ÇÕã'Mµ°M£B9É/}^…^‡A7ä“ÚVCÇØ“µy6iŒUuŒ9e8ãô«cğ„ÀÏ‰^r£®³YªïÑçå`…ıÔÎ6J¸ŸÓ»yGx<ËQ…!ò¬<ß`o«ôL/âe¾ÉnÄAÊÿíı¯qéçèÑçèÑ—;Gÿï×9G¿Id»KdÀìS(óĞtâÆæQ‘•«‰0zÑæ^ì°²¸MflY@ùè»–Y«&uNI©Í@é Æ(iä‰Æˆª½ä¥¼GaÓ¸WrœfU3³B‘Äœ<²õàšç}D½ªƒŠ8L=µ Bw—à{<0òÑ®1§­šdkÆÇ7 (¥éü†˜mó'['ĞoÊîOı¿{£Óé¤wÕiùJağØÁëËá¨?}}><gÊşd2¸|ä¥<Ácxì:­?ı‰ŒkVc]Â6CøP/ü1~šÅßn¤Í½¾ôèsÑFh}š!4H;Ø›w’­Àãš0÷ÃX ^}nÃ '^»à½+š›&²£ƒã†TÍM§N+8…»Èç)ìbxÚÍıeÿ”ûé¡Ä‹€ÁtVŞ«HM¦_úJ:ÖÍW×«5Ø„5<AXlWí®Ëğ·Û“áå¤wB^Å4‰âbøØ²óešÍó#÷.¯+ÊÚ”Z¸õ}¾J£ÚÄ’ÓÕvyCÚ7:Q@ó+	…›¡’„ˆJ½ùÜúñX*u2¼úÛôêÍğ²?½¼¾8î¨åi˜j»9˜š¹w~NÒrF¶‡È=<vø:)Ê´}°Iß´6EŸ5ïŒAÏÚ#x;˜ümzÙÇéñO6„"—QÇk‚ˆW·7à;·—¼ë¦m’Ê<~=§½×=ú´lx¡ô6\	™¡r’,mƒ¥Mş±ã€FèyÏ€¦Ëw]4—›2®cQN“I¶ õƒÇ4ÒÇ/‹PGg[s\]F—hÇ“ı|Ô›ô½»H cFì´	DÁñdŠ¿‚Í  ›Xˆñ¤p¶ˆx?`ŸvÖ{;„ˆ ¤‘šŒSpšlcY”•ŸÓ&Šuy@¯ŞÛ¾Eaã×æú=lµÇå&]vÅ²O²ez‘-YÉø„?´±mê?Êä´Şgk,¡r6^üŞáâ9+›vçƒËŸüö‹“g\¼/§éçu^È]|­>´Šôâøˆ(íËínÛĞ¼ôÄ¿c”'c´c	À
ˆüGÍ'°a‘İÁE½RÇĞgTã®B;–EŒm‘w FãxÒ×TÃŒµòØi˜úU¿ÉF¨D§YŒIÅ˜yÈJq]Cw¹©Ğ*EBVŠHp"§+¶o0–Ş?¦3nÍŠ°ã"-Š¼@-€ÃWÛÕpu=à14ƒª@$ÙdŒëƒÂW¦JœD´¦ŞéÖsÄ÷Ã–ìİ¯¨We$ (Ì'¤XJÉÖL¾€ñÔ•>$Òì$ÕaÓ…j$8ã´ø¦qòÃÉùàŠ«IÆıÑÛÁ	e›©?Ğ?PB% üİeâjÁ„s¼o8`\Bº`Ü>Åî@Y£€–³D_‘-“âzjÃ‡šº<ª˜¥/KVcv°+5Ù‰&_vÑª9&3 aí6$³àäë˜­êÒË(8JÏÑ>øÃì‡õ¢ğøoÖšp½ò	s¿á>‰è¿<Zhõ¯†£ÉôäMÏkrmñ ™ƒ7QV<„ÿ·Áµİ~šO!µRÙi’¢ÙÌòP&çÌçñ"Ÿ}`ƒg˜1^'KœgF\s9'.>D.uÃš’SÃÔQSdÉ­¸ˆB
º«ˆoB%Ÿí1ÆŠDkÔn¾N•–¦vÚ_B:»<éŸcÊ·Áåë8Ğôqy~ :_!N%ÿêxõ	cá0já-på™Ë+ÎÕ½¢uÚ#µaÜƒÚ‘Öl‚Û¼@.·öèGß³~«5¹Ş|ú4´±A Q ”x<~b½ôß1	¥7€˜BJ%"¢De¼ûó{FQò;FTJşÂµïıùı?­7Œ|šIpï¼ÏÈ)J1cTåì{#ân®2ËA+s÷j¼aüFÉä¥RßÇà T}qâQ‚’feN³ÍhĞ¿)ı\b˜3AùóÑûîœÛ{Êås—ßÛqmú>ÏˆGî~Ì7i9½+’õ=[%®MÉo#<åßå¯¡DŸóäa> Šç©åœdú„l¹^¨ËÊwÙæ~ÀX1ìşQ·H>u7L.¦›<)¹Ós—+0­|ÇdåsÖb¹ÿ1€jwÇXØ}Ş]`ùVX‡+®G_cÔ¥!—ä®Eß„&;"°ùK;»;—ÛÔ ñ×,ü‘D²	øÄT~Zh‚*‡X}D<íŞÔ‹oå¼ŠèÇ#×—o‡“€Ùoôø°ßš„·Œ”9†Üñ(„ ¿{êÿmkÿ÷ŞÚèB:ınÇÂ)yd“'vv‰¬¼*Òe¶]ú-,ß@QşŒÜÛ"ÕÏ3B"…Ù?`©#oq»W£şÅàúbzÖïM®G}²T÷7U§RŞ³y¬MCÕ€ŒHÅíUYUÇiYo¨! fùbŞ[•ŸÒ‚§Æğór„u|ü‡&¯F¸ïNòÓ`È
= 5Qµµô:ó–ÃÃóïkşB·»ÉçywÁ6­ë<„µÆlÑmú(–;‚¸1T«¤*ŸÈÚ}õRqnæ
Åëí¡İ€I­@ÆšV¹MÚkŒ>¾NŠdÉ†ºB;nÆ[Bˆ¤í"=…ó¸ò‹ÔytÊM>3ê2ÇCÜ³,ò¡Q³œ&ë5;å'Y€ÀS·L¢õ\ü?ºëİ`÷Ñx‡‚y}rBøŒèbYŞMñv'¸ÕQ°ƒóâõ?vÃ‡¡¿ña0á!c/R¡“q…NÖúÁ!}­§O³˜;£fË:Ø¤ËVÿ1	˜»e5 •’ë½h«—l¾KBÙŸO!áá  CÁ_@è”Ä ùShbÇ„mˆúZõÖÓÖó¸ÔÃØ[87z
ıı×@™û¬¢ö¶·qVİY:dta/‡6³wH‡z+@ø«,/#ˆMôÕrMúÒ(g·FÄTá_~ÉƒËc=üÚøğ…W›ÿYÊU%7$ÿğuÃàz¡™Iÿe™§ºüØş%Áñdx5½ŸC­¿^ş_½a}´<ôÃ‡4ğÑ_¢ÅŸıìQQ>ìÆ4Z•İfÿáw
„2Şäkh6Åj¾‰£–ÙAú™ôˆŞu^˜ÆäKÎ«êÀ7¯rìà,6vûMë\áqE*ÄE/{–wéÉâ#‘f ÃUİ­såŸí)â›ÔìRÕ×1•C,;‘ïêj!7Ã~Yuå—º.v¸ˆuõg‹œ1ÇA”tkô¦"7ß]´[Šl/Z¶†Â·àZ ©ŠÿùùÎ½|x„£Ÿ—{¤
z#Bx_fÈ§ù¥†ªüÚìŠÛŒf„wìıƒ×[·Ègìı5F,%Ç,~V6õ±¢¤Š¨å7Í¾Ä ñ>Ş±‘@tZ„B’!góH>ˆ»8úõ/ş+y£¥Ó¨u>ècàÉ›Q¿wíL;Âz#ªss.Æ‡Í…±°§ö°õ]­DÄSoòÎklnÀ:‘É³-æâ]¡Y¯}	Üÿ¹\*š­,˜:­gÏñÏÂìÃÌlp9e$yy‘¯îZ0øçú2ûjbÂ?Õlé›×[ +»
]]+¦ë6/¶Kb˜«à™å·€øÚY1Ÿ«¢iT^eE.'xŸ¥&‡á3pûZ¶ˆa3DÊW,2rRrŠemúXÚv·ÓËá;j\È³^¤«mû‰¦X zmW¨Fh?Hc	Ñ1^äŸÀË½M‡+A‚ıÑd£ò–Ï@Ï¬¼Z$~cİ(>Çnt—ÚÛ5´Ì~WA³v
ÅåÊ Ä±¸MT
» äHÖq¦û"äì˜y/eÈ¬O9›éİˆ°ÉaOÈ$‚õ)¢{×.×ŒäıXÆò<†lĞÚŠ•úÂê/S"¥ÑU½"Ÿ(†ºIW°ÈXì³G¯‚1V‰6$b‚Ş‰áGG šìVGÀo/2á|Èk¡6.cãgöL½Fu\7<š®rìkc7v4 ãnéÌAp'üÌ›Ûãóá;—ò®7˜L¢üÌÔç n@=ûhŠÏˆÒp²nÉN88Ç¸xä+	ğn
	¡Êöøeñƒû¢:èMÏ†£ãÁéiÿò70eÉ
ÏÌQ
ï€¨1ş-ıè[á™ıœIÔØ!Õ_äî~TÄs¡4Ço9|û“Ãé2Dd1“BPP¶ÂÓ5÷ıñZ6
>äRŠ»á;.ÍXŸıb(“o‹YzUäĞ/£İ~wƒfW(<G¾bh‚$¸G	¶ßä…è/R‚ìpmâöä…_¤tZŸ"H)œø@ƒ™İÂã~´‚$ĞzÀ;Ã§½
çÔ	LwÄã“7ıÓëóşt2¸ ıâ´.îàLUò©š@p'²Ü¬çZQHá!¤Y,^ağĞå˜Æ8H‹A'æèaC~’$aZ¤kÖ1£ E–³vÚ”ídõk„¥¯°p@yçŠ»k×@"ÈÁé÷uwTì8ËV‚8‡ø÷ˆé{Nˆºƒò3sùòsp÷¤lÓ¼o%kïb—àmèT–öZCDÙW4! o§pÁ<¼GıS•XÒ{Fö{sRªoÍèxx=™úoû—×¬ë7½Ñàòõ´wJj¦FŒm[mÓñ}$œuaªt§S ÖÜc^Íª›ê\'PF Saö{
[@«¾iÇ÷1H–L4Æã¿6yO³Á^ßö}€ß×j¯ú­®FÃÒ}å+¸¡È¤™|(~y³Ñ´†WıK˜ÕÙàœÜ,\ƒ ª«F’§øÁÜ1†€4Í’d¶1tƒˆòÈ£UÿØ2Ş}ÇŠ Bv)íuUpCkÕã¬wÑ:ú'dà)‹ı ’à}ñğhÆøæ‘Ùå ğª8uäQÂB§…Yèöâì]ÿüdxÑGJ9š|ç4æÓ5¡¤»y—.fù2í¯d*`V}¬bŒ´™à:5<;ëüÎkvJÈGgKüâˆ¢ã·w¸¬0’6­Òøõ‡§1|ì'-Şşè|êª¦¦ „c£â†Î`ô{^…É[ÃÛÛ´ˆÉã§1ÌN#Wè<şÆ´ĞÂh«»ƒé¢âV¹ãæÔ™ÑQpf•Ç¨U{İM>ñÚhá¥nä¢°oN?êDßKæs„Ä$WM*6‚İWß¯8ú¨l·>IñÛ>¤ö!¿-„Ô¢—i:ç{²íb­…”x«mÛ’BB ©ş¾aAÌe#=Ú§ôÊŠõ9­İ®jÓÃ¼° †/»üo‡yA°†4¯b‘-Ç aÓüÂÄ/L àñ…asŞx©¸‰´‚4LO!hî§ªh¹$?¬ÒşVNOC6KVüğ"_åhfáØ¡˜ÎšM)aõWMjß˜”¾MRùDÔ¼õöf‘ÍZsÆ‘ã­y°2xô5ˆg_S"ÿKÇN¼º!òxû.HmÍf·½q!jœå2—W×éí’¶¥N/EU( 	ÙŒ¢9Fs°ŒgY
Se¢–XAa£Au¥á³uØ8BDÊö¥P{µÅrÃ5{VÀ%®ŒŒ°bí cãYAb»{BÖA# ”İ.~©?ÕxPSıÏ³w†)%RÒ‘•RÎ[á)ù9fåçYÃµß/d"ïX Ag¶Ÿ¤¡ãä6m—nÇÉBÄGï&åe²ÊP~°;“_(p¸«¨ÆË©æD¼ï'½ÑØwÈ! ²Ã+|”%(;9MgŒW„õ~mr}ç<T7“3ßÂq&Ô™ş-Ã>8¾å9Ä¯öj’££2Iª‹ÍƒÊË¸kwëM—g2¼-û¼ÿ¥‘vxhø'eî:ß½@ˆBhg¶FS‹F‚ö¦¼á;²'#¸Û.°99…s¬õHxñ²6¤~1Î†"ûò JekQòÑv (dŸŞœ«¹Ó¹Š…e´1“ÍçéÊ{7B`Õg%,@‚°‰d3Ï?¦gE¾¬4ïA¯ z3ŸÉ=£¸¥¿¹–_AÏİ;»Êm[şuø1-
†%:_!y6çyGù0Ë¶øW…Ï¹w´H«rÆ[àÖ¢Jàâk^âm]1Ó>¶$[VñÈFi¬ˆgÁÓiõ7ºXÛiqƒp	nk­~ÚA^)
HJ®°’úÚOä$Ñ=8g;‹ñ;tòÅF³N-ÖVM-Vâ6/>%…ÆVŠD/	áÁ¼D•tëÈWê¹QŠfó¹†šböµåÕº•wËÒœF·kNwbé™,\k´«FRuo±ğ4Qüı>îl2ÚuõŸVTƒ†5ñs°&~Å­ŠŸ=£æTAÜïÒâU‹m 0¨®n¸áòf¸¬#Fò^™,ßøM–Y·†=°ÕÂ‡ô¡·ißÄ¹gŸ Mæ†À]Fj7mÖxä`£Ğj¡i&Òßx´'¤Œ“)Ïš0Fİ	9‘˜íDè‰L<[z„Á0dñábÅ7I şõï¨sªw¡Êõ{˜5.ÃÆùèõl.şw8y9óKH>3-=¹§‰ Sœä³ ÚÂ!­\tO /Dõv’s'.ÌåZuÅƒ,–»ê$äßaŠİ30q‹ÕTG;\k"™Ğô[š ‹ÉïRÇX[Ã™Ö«Ös8îÔšUk‡M½A8«l¹İaô|í°ç÷Õ¨eàJiÏöÄ„nº*#u•djÑ/¶‹M†C)–hÎ.y729çUltk=mµÁ€’‹`Ò~K:æìÏ™¸„ÚMÏUp¬ Taü¼ñôei=äŸ×sî0öüU}ºœHÀ—Ûsb{cÜã|6;&åcÓê“d9XÉ÷¨"t@Tñq×ê1õh­=Šlx@¢š§.w1ò£9YÀå‹Wy›•…Fù'Ò‡@šCö¬,Œ¾RuzmÖ(Ç*ã$
8Pqé=XçxÆ+Ú“T´Çƒ¦X’¨ò
z[¾S_ªkŠ6|>l³­Ì?bÛó˜?ê¨çÊ÷,Ò^	 ¯%¬cJƒ`.-]7¹¦ÑFvNÅÊª8Ö£/+ÌÀÖšñ²0
´ÅfÄ+¾ÀÌø”BN´ºNPË#WR[È#¹€µ,¥~ïJ¹"xõ×áØrîõ‚çâÁßT(&º¼õáİ5"+1ÍªãN`O­1f€»wU:^0w¬ÁÚ°‹LªNÍú”Ó\Qu½šçHD=Nì[ñ½n—CYEb“^ç(ü$.·Ş$ñ³¶R§¡*¹kS<˜ËÃÔºcÉibgĞ6?<ÍK ÉlÂgï”Åğ˜è¼ñv5¹o´GM‡ÖÑ &î*€Õéÿ³eñ,YvÌ¯úhÎ†=‚{ûÕ”õr®åjÈnÕ²Z›åÓ™rå%üñR—°Ì¶~HVò0C‚<¦U¸LºÒÂ~ÂGÕŒŞÁÊ€h0š†×î†î”Bˆ]ÒÛ«ŒñåÔ
“œh¸‚ÌŞKÌLï¾õj®¨®³­fÅÃšáˆĞ·3çCb9–;*ÒÕŒA‡um4È ¥Z"Í †-6tˆ‚T1F}œÀ®¶„±÷R±]=sÌ&…8kòY MïÌøôDh°Àö÷p®PßİVêà¤…¤ïWGQeC´Ñ‰]‡×`•É‘³‹â0FîË/	wKer9^ÈiÃƒ“I²»ûMÙ`¸úÕ†ËÊñö†í¯ì&-*~«<‰¶Ócd€Ãaò$d‡KÁ|"D2ŸOò+`sÄÎ-/¹¿¥hˆí–½EPı;¢\(T‘Nö¤öÉ¼Hªc-+HÉû bié(*À‡ÛÈ.áÑKpG9fÀÂ‹ÍM2#ssÃYˆÇw3CYÁkÆ ©‹¿‹LyG†èÑ-6õİ`é:¸¾%ĞÍ¡âó³4ÂUm„“Úí……ç‘â Ç5hÇ ÑL•Ó½zeeò²3ÄŠAÿ¤ûéÊÏ;Ç¶Ñ§À¶x¤‚58~­™
ªÕË(¨ò”ïûƒ©1&¢ñ]@gt”¯ŞÚCeŞ%ŸZ’‡„Ş—  °é×iXëçë‰Ïì€Â±Å4ĞˆÕ4wÌ½·+„Ğ<»%%@7f^9qe¤½ ¿y'ROVN 16a×ùÇÁ+ŠxÕR¤^å'e*Ûz¥:¡®üU4„óä!ßnøÅš¬*fêáoÔ•€ MOÔk2«_B‰õtXlun	Cš]A¿#Û¬æéçÖKmˆ¼ŞªaFpO¼‘WáûYãÎÙ¨)îŸ-°ïŸõQd:ìL Ö%si²ÃØêOtè¢]˜GÕISÅ^Ó‰ƒ2ÿyR*§Ÿ¸°W.óàpD˜İ©-Í®ï@5¿Èİ£;º¨›H˜Â‡ôá&g0x—”¢´ÍÖGòôÁ>İ?‰N0ÙÃ*ñºŒÑ-Üiúi™ÿ=ÓâyĞ-Óyé3Û¨·æqt!¦sˆ4¡DuË(åÅü<Ÿ}Hçmù%[}Ì?èŒúÍÒğQb|c6TÛ^²‘ÌUA)+8ˆ$’‡Ô|Û	6¾tÎïTÙL+ŒvÅwÈ^SÅ˜óæ…IÒH¬Btsš‚îÖ¾Nö9†<›¥_aªZ7uSuŞ<.¡1Na«,3Ö§ûlv_ã»³h#î™B;›zâyŒ(ş‚ŞM2î>Ğæ³g9âe°¿X†‚ò}\<}ö5[)÷¢ıøˆúæxIÅ¶Cëµ³"Æ~?_3Îí
’¸d__N_å}2’øã¥zĞg–òâ°L¼U-í4¸ÑªKQEO–§š?Ï7ŒGáE@·=Jn$Üğ¤h·cš¤†uˆR¾*$‘D!QÊû–èe3ËpãQ† ¤<x·ÓúÈÎŸ1C±UÍêÖó¤†ñŒ°Ò"İ'¼CîŠîİ›EÁ†İğK\xµ €ØlèF =o©®‰êrD‘Õ0NxÀ0aë$ÊrƒK±j€ÑÂorÀ%¼)ÈªK~­\d@h?][¢éFU3à¬T7niRÌîA—K ªT]¿]mŒX^åëíú]¶šçŞcÔÅîVÊeÁë-Âÿ5ÅS¶+á`,÷CíÆd@³*4œßoÇ‰AÔK†U•ÜøUËäãÔp 0@Ï¿(q3¬;Ê"¸òq»À¯ÆFê÷Áş°¿‘³U?Î7ïÒn¥æ/x“oNòå2YÍK@ØAµåıuØ:ª¶uZŠğ©fñ%WÆ?ì×è¸jğkaR	tA`h}­>Fbç#É1¹Á`MÇ×ãÁe<.‚ÑoËl•B°ÑÕå ÉG3JWÉ2åş%»ŸŒU|İJBËlÃuõCøÆ/Ğ'ÉM©ƒÑ6;rô³ÆZWmt93ÀïÔÄE:Çê.ƒÖ’vÑkî¿^N~’™¬pŠí¿n3 çëE¦ht_úıÉàòµˆ‰?À”z'I™¶ÿ!+<0Œ.6³íı^<íôŞõşÕB\}ëê|°VĞ/è¸/ÎQı!«h„J`6†"´2 ¡ò›¤äkZF°Æb.?XÏ~÷)Òîıäœ	êpãØšª2çœ«új)L–;Ÿ×Õ‰ì~m—GAd×ÿƒlÒíí†%“YÈ|e=³Ö.¶ÀRÖ™äü"‚n±cÖs¯Ãô;B¨£‡IZÌ–~s’@XÍ:µŒSX(AHN¶‘ŞWüÜÌÚ¨4}…Ññ^³#4SÊ]lŒÜ—¡©Bl\tó;(ıx³1é"nIéQí?ÙRT©/‡aüò“[-z¡mÔğJX ï;ÇnÇ¹5®8söun{G=	ïÜS‹ãMDñÕQï{Š0qÑ¾$‹döG€é¹Ä«CQf‡ïÔLåŞ=¾L†—Ó«áx0¼í;ZLlÒCÀùWD/ÖáI¾à1 'p4ç?ñoˆ‰0¤Òùq¾˜{°¡Ü™X ñY?3â”Zàr»{N‘Ì?§k†ã½@šøaìà)ÒĞC¤:/ı—Ú‘Ùîuuxv½±9n¥¹„ôÁ±Ü6é²·Ê–àÌ>É€…ÑïbW“E¦õ.µ’W‰É®+uWˆ/‘F\?¢šöÇ˜ÒcT²İ|T®è‡RV/éw¤³(yŞ˜V•dØBZ#R¬C©¶ì6<QH£4VZK-Q•õR˜ôÅE—ØB‡nóÎdD»Æ”Ø*h’îŸéÇ¦R`§O¹Ä½L>V7ùg†˜spèï†Ûö’ÊÍ[ıI ÕhÜ`ß»ÅõşxyıMUmlXyUò"ÙÜwYñöóÕË¡Ó¶ÔÚDªµ¶—x¤*[Äª†LÎ A«´¸t:=Á‚@ Ø±sáb/Ê
Ñ~òü3&®8ô#sa½!›ŒĞ2eGï’ÙÜ`ø@ùæB"ÜÁ—¬ìj¥ô•ñ¬Ÿ|å\-^XHâÚ¢š¡ßrK;ò)Fş^°˜‘Lëju64×k[øà©qÂÃéè¤G¥»Ìrê¥@m+l_i«}¹ô<YÊ§ùsªhš™ÕT”µˆX}$T™rE‹ƒÂ^ŒëŠÂI¶ª¼ïÿ×¢µıê»Õı®¶‘\Z İZç©äÎ1ÌÓYüÎ]_…x¥1~ÊŠÿØ"ûƒfU`bm•®šœ¨†bŞBŞòÄ©à2+Cì²ú_âÌéDöZQÁ4ı}ã–í=BØ!fòMvw¿ ŸFµhÜ Û`İîBë-tBªv¹	/o³Óâ’*ãÄrUÿœæ··¬RtŸTìè,+0>¸¹ëıE'Ù2Å˜8á²¬³¦Øg!‹I™Böe%Q÷bŒÓ^”?c¼èÜ D´B¤ÀÀì·ÊxhÔ-’Oİ”‡:‹Q!`¯—ù;–¡”5I¶~e[<t4ÖG+µ¨~'©p³ÈµC«â¤jØ8ìÊê”«¡Í†¥´Öû•tµ[£¡>@^îHû‹æ©`‹-eùR.C»Q:{`2<¿ º
íÌğò{££0æŞ@‡–O	ä”gÔŒû
 h·«hß7hTHú¬x,Ãl1?‰ËÖDP	W^O„ÒóuÒŸWûÆAõ4ƒ9À8+)jÛÖËCŞú™’$ŸlBâÒGrŞ•'Õª;'9/¢ÑW5¡]œÁÛÃy²Ö3ÒãñÃ›ñ"<×0¢£Aœš2AµDÈt´‰*‘”Ä-L]ùÀ: ğ§&êÓI?ÂŒ«ò:)÷‰öÚ.O…Š¸2L†èvZİÀ«./:'¸p-Û¢"Æ!HÈXq8‘HŸ¼{.!b’bÿv£ğÓŞ‘3øÏË–»)¡mwñÍ5YÚ+ÄjÛ˜ÈGUÅìÆPçJéë÷0Q”_((ı%ÛoÒÔ,ğL„ˆ¾÷¢ ƒ_Ú„[ª­Œc©jL’òâkDY¸wç—è55p5Ë”g<Î?K+Z»QCµ°BK»Bj( ë-“8K+p0”g‡?Û:ï(\·İ*–ƒê§şKê\öÅ.g£z/IøOéCÛúí× v~µ/ÀÄd¯¬pË\¥ #0Ş¦Ú9ˆ4µ>×+NCL¯.¾¢U©(¯”ÃÂ·ÀCzrÁ‰=fR¾mR?£@á	äEÒöçlz€ú7<VOÿÍ¥ñH^›šÙ€WÉ¨{:x©Q˜Ç±ğœåª¹—yCñ=+¹Â‰+©œ×“\/Š¤~ÀƒEUoÁ#˜¿„Óİ»ƒ€œùh?Z<ĞĞÔ-Zİ!öIóÙ'ö~ê¸;L£|Q£’)•ë‡ĞN°1‹Xøİ‹Ş¿MßöÎ¯û˜äĞ‡Ù$iÚî6î‘W"Gáõ¹î]T½^q˜Ä…ğ“ÅğB‡
î7š¨‹]EÕ[åÀFJ&vˆÄ@Èx\dL*ğ©EÑ”X)ŒíCIåhr<9ÅõY¿ôCëÅÑBÆÖÿPG÷ÓäT¡£V¨ßZæÍ¹ÉHveB	]™Ïu[³ƒ­CIÁgÖ9¯hÄ*¶­fü»Qc1øØæ#‚êm²±šqÙ¶Âã~otò†SE°ˆe$ûMRŞo’;“WßşºM‹‡] Ñl%<P^•Î8à­¢ÀKbQ–c¬òi²IìØ~¼Î™Ë“JRÜ©ãÁ—iq—Vv¡şĞó±Üp›cœ<7¿ßˆ:iê^îëÔ›ÿÀ×ÀxglXåXp_ÿmÕ ×ôŒ7hûĞ0Ô’ã“Ñğü|:@ÁÁéyßê^CJÀ¥æ¡*ahôŠ´§¦Û«´ìMh¥ÃoØ”šŒÁVÚíjºe*8@4ç‘5‹®8Ëí0sŒ´Íb»¼<óšK…UğU„–“d± KôjÏMÛNKDĞ|ÕbşÇóM3ÿM3ÿM3ÿM3ÿM3ÿÖÌ#µMíM½¬ok™¼FpáG{Pİø»TÑhä‚ŒNô6òÔu8'¼BU–²> ãï•mÀ°»Aê-œ„¾4ÁhÌÔà|3ªc2‘÷
2ŸŒãLà[`ş“!~¾á£]½áÛøFEš&4ZQş&ÜÎªŞ	Eáw(:d²O~ów“EšoìÛğüĞ	¸õV¶¸¦Oõ­?·şOëÏp£â¿L>KÄ`‡ëŸxñgÜD4¹)…ÕŠ+K¤"_¥ÍB›Vsx†Õ×ÜŒ”†Y$6T<Mñå‡—b(}# )j\"Fú–‰´:€±Nr˜ğA§¥¬ƒª·–øP4 nÓ§Û"áÙláÆîqZa”{¸ºp{"áQlîÊNëùÑ‘ƒl}ád3ıÏkHAîúáîd¢mrâ£ßUIĞ#¯q97ôÒC•}h´ß¤,º¢WJ*4«Âv-xÂy–Îƒq–íá4¹fYÛÖÑ¡¹}—šÕŸ[Øµ¢æ0PP*d¼+%I2EÇMŞß£Å¥çúŞ`Îß]Ù°×æõ	ŸªE~•š(­P£V$€†¥éò¨7D¸¶ØIyä.znú‚ä•†p	H5R{ËòCÕˆÖAY’ã¶2ò¥ªO#PŠ°yü†‘¬<Ä¨Säwl¡úŞ~ânBªcãŠ—ò4=ÄàjQLn\öãèÈ¨fÛß{Y«ÔùÀ"@FL¾Û&±Şæ¹ˆÈ¢Í9®¯Ôt§ ½õú2Yñ8sø“5Q£İÍÃš_ø3ùÛUúvpÚº®¸;E½Ìy@<Ã‰eëÎë˜Ìóö2“@ª+CÕ~äRß•ni]>Ûr£âî’ÏS€{,ÃSo’ÂøuR”éER|˜çŸV½òa5ûŠïóecËï@Iì"<èúè÷G“)¢É“‘üAöƒ—ôè‘2¢7É·³{F¥Êlî†çƒGíÏï[<VÇÛòWOê ÷0\ñ>€ıHİƒ+¾jàç£÷B'Gëš¦ÎÛÏÿt¤ëĞñ”3¡TZîz÷.½¹bÈWš¶JcmäŸ0JÃm’-¶m·)è-¼Y‰DWğ‚»†ÈW*/¶}K2K6³ûV»êŸÔre‹ôœ-pÚ¦jÈVŞótàËg‚ß]É	ğ™|OÔRQT? k%ú«ÎqÇyáÁ—Î]!A³“‚Z†±¡è<Ö¢:ÛQ/™ğ2PHJzÅ&›-RÂRìŒœ.W¸£Ã´éı`^@B¿RÏ«º¯?ÃiŠtûŸg)^Z·¼ÉØá©p3d˜Gğô–PÃ¨ãuô#à°‹|r(Œ ë–_½b†X…·D¦Ğç§Œ‡â¸‚^~ƒŸ¾”êŞd+ÓnÓşˆô‹ˆJé¤‚èÊaø<%ñğYGU‰›V|×b»“(Â@,®Xwlµ@H-ö¤i|²Dœ°ËÂpàT{›,¶©ÈIAX ¢›	c°4;	j/U¥°YÕI5¯‡—ş_ÙQ²Ù$¦AÕQUjçrFKŠÁÌU­Ù¿†İjmpK]7½ÜÎjSáÉ¯ãŞÛş)ıEKfiÂ„†_•¢ÿv0îúåøE+È9ÛÜy{%|ŒAÔŞı Z=\ÙûÆÑ“üCqÚØy4<ş%Øuk`İ©K×ì‰º•’!p ,—KˆÕy¹á©¢¤ÓºÙ{¥÷ãPù	ÚJ9šWx£ß28”³"Ú2¡@„mTÉ3º„²ñ©úh»İEÈÔwjnQí$P'‘ËMU}xÁx³ÊµƒßZV|/[md­[­ïZâôS.&Sÿ×iÍIY¾ŞÂk"‡r PÁÊh¯µ‹aöú	Lê*r‡ª ƒ95‹)åaÕ„£X_Á©İó—ú¹eE¡ÚH@u½X™€®°ª­eÍh³í'ò52¥%ğ-íƒÿyÀwõí¸JA——Q
rÎ•ñæ ¬åàö2MçæmÉ7nÖÛ¯ÎÍ’øÚŸuŒ_J¼ıŞ-#É‰]ÉÅûE|/«V+?ºƒ=ĞF‰w~rBŒUfó¶}q¾±ŞbJßXïo¬75†o¬÷ï–õVd7=šID¯>û(_Qg×R,°pÇÁX­v¹X[6EÖÉÀv¢ÆÁO#)Â	/ÎÎÄËœ±‘w	7§³3$uxSNŸß‹=Åyz›l‚EGåöËÖ‹šë,ªÎR ¡x)¾zâú;ïLOŞô./ûç`47ÛÃ6¸óºNeG2ƒ^vE([¨1JK6²MvU3û=u]ƒœ€[ Ú"Œ›ªïÚº¦Cf]¤ÙÀG©«@$I-YRgf¸åšüf;Ë›'	êâ)WŸ°ŒJ
DµU?ºC6ÄdüéËÙÇˆÂ_[Úµ+í&ÚÂs¯o1”¹­£ìWâfË®ÈïEpÖFz:±œ‚üxybvò4¬†¡¦PG…Š MGLßŒÈİü§²ŞT&ĞhåĞ÷z¨`{Òh™½i;mtˆf)`ìB^×ÛÍ$Ã mJ.^‰’ˆS|©ôuÑİl"[AÌ^ÍË\'pI0-lX¶ÛbĞÚaW7õìêxUM04cÃúR™GÆ’bÃâÃ,ŒÊL~+ÓÛÒœÚ7Š=¶_]ƒb-®Ø·û@³£oJ‰]'ôMğM°‚géÈÒÕ<©ß«šå‰ãõzƒªãFº³Ó_Gtó‹ìû‘ç*°Ès"Ç®7êŒÃË£ L¤¥'˜¡räò»àQpñpÅ¾ñ(fGßx”o<Ê7åBùÛ]&ÓáDCÅeRÚA\MÌU*à×_øøt«Û›xvE:+Eö²¹³l±I‹t>­vİ¥í9½$u·0š
<ôĞëîkbZ&®|pŠ¼ÿ¯@'
ÆJq4Ğ,©æÙœ‡ï@£î•sª‘¸ÃäB|ãiEä/¸(«­Vù&»}à%dèƒSôW@ÿáuò qsJÆŒĞ(v4]¬Ùêj¯®’"Y–àœÿ¼Œ*©:ªÁj÷dV#t·0òµ…gŸú¯˜”9!äÃcë¡F‚zÔân+³>UÁà™µí¶"„Ş<üO§ÌRfÀƒ”n¼yƒşÚtÀ–CT9ì¯$HN­/UUÙ¸v¼öQ/Û¼y‹è†¿7¤%=V‘-…oËu´ƒf%Â*µôùğ¤–$Â‡®‰&';T›¾LŞLÏ!¥ªÍ ßV 0ş‹”u²-İĞêj"_Wp»ôåÑòek1”àö¤¶ ¾YoâS$b~
ÄZŠJ3NÙ±ñe¥	¹‡ÛFT£MBÍ’ÕXlnE,;Ñ¬Ôó‹wéb–/U`™ŸN//œMÉp'Y©vmÛˆà±'_Oû{¡zĞóÂí³7A1®’¬’æÙİáVêº¡”æÍØ¾AÎXà…à¥yP=X¿ÌA;\q2,`%dªLökñM#±[æ«Î/¼1Õ†±ó:kÄÇ´ò;yÓ?½>ïŸR-ÃpÇ)H]ƒu;†¤Z×}íÎ²°áAõ`:”»,ƒÛ‘Ì"ªa=.Õ.ÉÅÄ·Ì°3¢ñªÕ²~KÜÅVyrÙÏIhş¯U²7³edtóZÀÚY–Íf«E¶JyüQª©LûNµ£]*W×íxcéß³ğ94&’Q³Nãp«Î‡€ñ#)<–SZ%ZÖkÛd—³ç8ÎNVàje9‘Y”ú¨ñÔw2¥$ZaLe\ÛAËS7dz¬S[Xó£Óï6ÊÍúØFdŸ¢èÏ2pÆˆŠ--XB‚ı™Ú.A¹ƒÚ†xVÛ°†Û¬õ†Ú:ÇH jÏD´YÅ5€`…Tëç*B©¾“»<óv3œ,ù'dk¯îóMN‚ô‰G4ğÃÁd0*àïc”0¸Ï hæòA0VßQß|gÀPC‹øQÇ‡àv¾“¥@öò%#¤ìf8¥rWõ~ÔÚğ/\uvX3‡W¾™oÅ7ªA‘úu5GE¹?):*ÉË@ª§ÿØ¦l"Œå\s}H½ªFÃó4a´¨Èò¹Pâ¤··¬°ÌT\×êcZpÛĞ€¢‡kÒÙ‡1CdX”<İû¯Ûl±PXÌšÄ8a:«fA(ÉÕĞšÑÍ¤@!µ†`“±ˆPG£²å;ì(à‰ôèÁâÍwB-ô(Å©R¢Nn€B©£­±2jQL¸Õ«3<hˆö³my­üÒˆôe4‹|Š_C«ˆ0ß‹fQk‰¢^¿T@äk1]¢î÷¥¾4dñ ÖR+×\s©ƒaí%<;i0÷±7JJWÛäî“Reën@›¿ÊÆA³×]\“~øeöšŒ¶İŒ=„ÿå–¾÷üş”¿ñMÚß.k´Ó´Â±»M«R»ã´²»í:4;î<xääéÌ¦.„Ìëk·¬1ÆZ;Áqÿ}=<vÎY›
úº|CÙ¯²;'¿4?\ÈÓs“)Ád©Ézğ¥µÎ—hö
ğ‹Ÿ”Â½JfZK~rT‡•ÈqÉyØş›mz«òY¸é T®B”³p4s½d5Ï—İ;ŸÎYÌ3•ı"ã@ÚÇ²X†şj“m^µRø—uFÖåáñ€"%:ü~/qO5?şS4÷‰ÕHByÉú¢XDĞ“OÕ€‹(üóÑûî&ÖÊkHTÄ7í'Ÿ¡Y¦-0Œ£¦XÖ6ššiÇÂ¹¦bnZ¸3ú8.ŞPeû(wHJ“[¤ã%mÁÚ—:Éç9MG&LPnmØç¯¸ãœ½/¾~By¸I¾±0÷¢p…ö]`oâŞ‹X¹ìÅ£³_A2{±/¤ç:Qü¯fQ†JUFE‹‡ÖZıÙiazĞF¥sL&õÑüı8ušJ*¦:Uªg/U#‹S­ş”vå_âP²L‹„2/7“Œ£Øé¾g•¦óQ
ğwŸbr`¿ÙÙvØl‚ß½Åº]5 ¯ÄwÎ!ëé¨QÄ]¯—9y¹¡lzŸà6Ö—Iè«|{wß[bz6h‚!Nğ+)¾£¸íq¿ƒ¿–Ì‡æg)Œ«öŸ…Ü+ÙÜNÖXŸº…ÊÑĞc¶ÈË”Ÿúß=Ä|`×^¶|±ªÃµ\&§¾]ø«9œT§õG+Ÿú+¤ó®6}WqñÚ;É6iíâ)[¢Åz^°å¶H5[†8·Y!iãŠ±—:­çŸâÂm¯Ä€BËÇoZ§Ix¼yÏ×î×$áÇ„BoùÔ2º<EŠ‡Ó­ºY'›û{péh•3Ñı
‹9oá·Á!y¯#âøds—›MSE¥Öª5xŸ”"{¯9;¹•‚,º6X¥sáÛ½Ô2;ŞÆ%ò±›ûºDÖQØ‚+Y-ªSsUî·ËşÖa™¿0B°m8û€ª^Æ½› ÏQ~%DÑÅB‹õWÆˆõ7døM ƒKKLöÂGYşKœ:UÏD;4òş¡:£¬ÿÔ—Æ:¢D£Õï•Hí€S^ıÎ²oxó[À›ºc/^)ØJ5Ó¶N÷†­èmŸ ½X³ÕšäÚ©İRW¼¿ù[­5³~¿l9Áàpf1=bbv¥ò§ÊXW}Öké%»Qs’³nîYÓÎqd•¥™ø|¶ÈÁêÕR‡{œŒOzçıé¿1ììşéö°³Æş¶ŸÆzçWozĞÔQƒ¦ÈÅ0|o`;ìÒş`åiı*XŸJPâêU‘3"‰Ú±«Şàr2•]=rÛM:€˜ûˆóPÍû0'™ÏUAÛäËŞœÑ!ËÃÓ}Y>FBˆ•šŸí}Ù°Ğ|gtH	z#×œI.l àH)(Ï¤HV"†e(M›ÛÄ¹á?í@¡µÛàƒK:8<Ş”³¾%«KÍ†FÉX:÷—ùß3qç&™V
/ë/Kl‹Õš§ä& Õ»©´c$×-û;ŠÙj‰aZÆÅZZ×âÆø|Û7˜¼öÉ¶ÜäKœoçA´R[\o›×ìÎåR½ä‹å- ¬ƒ L7›;ÅòÛ[†¦İŒø´HWwŒİSZ[ñÂ-ÈœÌ«i_¿Œq–²˜xë'®Ë*>/âvØg•¸Ó®}ÚÕùØ*ŠÜçŸ AäkFøm¿´Ü(Ø‡ü§cŞÌQ)Ùe±¡·Å-E”KLù¡£[m3Ã×Ş6ûÓáæ8éğ0¤†ç-*l¢_!Å5<ûJs¸Àá·bŸ\S&„Mà¼ƒZlbÒ¢Ù‰Âb#Œ°åfºÂµÅ‹Dü¶¯Â|JÑ×ÁAÈn®qkã$!HBËÎf…¡ìÖ†ÙÒƒ·9E[ºâ6™-€çöÑİ5|‡X»An Am¶£<.†Ã /e<êEÉ{,K‡
¤x1¬ùk° c™´¶0F»È˜;Ë—Ëlã\rú¡x	XŞŠ<ã´›h]‚,œ*Ü‹i’œ0§ù„ÃFEzv->óàÄî¾yâÜCgåRË†wé„ÍV~Òåv½Î‹xM_xò !°$•—şY‘Üágöì'VµeÊì?ÀÿúYë9f4ßÜg¤µ3<ë.bUÈ’[CoK¼ÓIo¸N¨‡­+ Çvt‰~W„`Â¿Ù’^˜N*&ã(ˆõ\ìªÁê¸È?!‘“Y:g»K aÚPS¬ÌDŒY×D+³”ÀÃ¾‚µÍĞkAZ„ï}eäN);cÓRÙsZ«/)úì‚òtFaÆ¨`–Ã#Ûl”HŒ)ïmÌ’6?[NçY9Û"ƒà¡COí×T;ª9a^Wâ§ç‚%.ó˜2Õ¾'4Lë"Ÿ±na¾ÄpÛ•€T}ëDÍßy£K[bÜ/îeXåeòy„¾ÿ‹7c”qÃŒêÁšØc+Òtt¿ë¼Èî€kƒ¼Ëûìî~Áş¿¹(ïÜ~eyñV§dœûƒ&€ÆĞøÉ|s%åPğ1C(JØ@¡ç@Œ7è`ì.F0ŒâNÉ.Ólu“²’ kAÊ:­ˆhZğÈáSæJšúı‰(§Ğ@Ë}Y7ú'îˆİV¨±Â¯&™;cÓòw%F©—sI VAšnr"·íoêö¿5 —îG§§Üw"ËdGpå'\F ğrøäÂ |ÖNönæ„‹AD‡ {CE©åmR”İóáëñ´Ù;>ïŸR½e‹ôœÉ9óöA˜¶µ€ÏØÈ‘…FBwbĞƒW­D~«M0tıpöÑ ô'\—'&Á_¶‚‹ÉWë{Vú‡Ö%ûçéÓ&›£K‰™l€
b 5B°S}È‰B³•şÔExävq°.Ôàİ ªğ¨•C¬±´m`ºÑ>ñ‰‘’qÒ4Q)j´Ğ×Ñ`kÎ¼3<A%ªÚÓ”Õ=İE®æÈ½¨àî°l«iuHS\M=ŞÒ$Şæ©¼†¸*Àh¶ú˜,2õêde“Ïñv5_¤lî¤2’¿¡Ê"ë&„[ ÇÕÔ8ù6-²),¸+AÈDÿ­)£œLBx&ÛÔK±š¼Ö[‚.;-ÛÏ;1'*%\oWX±î€jR/hL‰Ğµ®Št´‘5j Ÿ\p·)=ĞVKO¿T¹‘Uò
ôKy|éÁºJKƒi ®É—1®­ËÏ´(°z1U/å™)ç¿>ç‰‡Ïq&UÕÓ8>³±Z†aæMVS?,¬O¦íoÁãB:!6å‰`pÁ|ó„jëåncÿ~y˜ñÔµ“n:ÙìËÈşc&Ê!RıüÇ»t.¶bõéŸ\EÖ"¼9GÛÕ
TD˜_Bò’Çvôz&H0W=ş|ô‚;†$'^‘j>ÉJˆIô cÑcØJF‹²ò]@C¦8”¿‡lrùªûÎ+x¢Rsø‡æ›1éÄ-òHµÂR3jr¶"l¨Ô!ò±•p:Æûn1(­:¯²‰ ”fQÇy*‹WcH\`‰kªıGtÙú¹CéÈÒ2¾0f3©‚
‡†!áÜ¨a¶§Qyu–³“È·¸‘øKìaMS`ß^[Mëv•ĞP—Ê±ÉH¡Z>†PB·S‰ Œ¼‚¡´’gÏê Fl·:ñ	%ö\•Ğ0o½Ò"€¹òcêÆ†3çp­wÏ#%ÂÅhäğ±|äå#^Jğ7İq¥ÅšÉ3AæÄµq0ì’ß|Î›Óo|NRØ0]÷•¼|lå7Lòú¨®0UvLuô‡6ÛÑ+Á(¥ªR«ş»³9W&R §·‹,w¾ ÁÄ¨Ää$Ÿfs80Ä²ˆqˆ­ÿüÎ®—îÅ ©}ÚÃ¹¢Nòyäy¨?y¡Ì ¬ ë'ì‡kòkúC =dP¤ `(°²¼N'bájÒxgÓÍ5œK2Ÿ«‰È}öˆÉTèÀú»KÿºM·i\%wY‹>;Cƒ§$DØw‚çKG)Ğ¼åìÿ¶P÷
­ÖËïªSà)óng³u*‚*#{yûø#köÅ¡ÌÔRT¿t•*,£t|ƒ8’MˆÈî^¦©KE,iˆ¼şlÄ7a}›²¼@m­û¢ZÙ–b(6#±g­FvqY!®vc8Ğcö=İd«3&&ÂE~‹dèPØô‹"/d™8s‡ÿŠmU8:7éÜ±â)İm/Ñ2r0;Úy5¿‘…Ø»uWpâ¢P¢—ˆY±Pñsç*±şšÏ^iÇ”K%Î›r©l†1ş®ÒcEIWĞ×
4fk=…6Â¬Ö[ìİ¶²wUº¶Š›¥•q•w)£]Èğs>Íş:h©Şáƒ¬@ıEdbˆ}iïÜZ97ë–Ô½çûëVÌ€®Nà|Ô!–ZQ/|ĞÍ³çv'€Á9£\7Å¢ fÏĞJTö¶éš“şBeŸcö4c‚áÖy:W×°ºb7á<æ&`-üPBf?ÁºM²ez‘-Yiß'îMä_• xÇ¸œçl<OŸú´yT«øÒ»d`Ø+1ØË5+æÊp„¼¢RSò¿zX´£s‰G>5"¡bÖÏ úá¹˜Ò!(¯ÿ&”Ïû.ğ9àB7«„m^êçD,#•£¡y/‹¿ESç²M÷Ş‘‰C~+¦ÈŞ¦a[š²qÔ@ë‘	eŒÇãz5úò{ÖËNwíN¼Î¨@>J—áQÊşù¡®Ieağôiâe}Š¼ºæARÊBõ>ìäã·;_|‡•D“‚¥WL  ¯´°9!IÊäãƒ*V÷Y2U-ŞRä–ki8QQÄ'+T%$Kà˜+¾TjRé<2Dã6×êÕÁju¸ĞÎD¿R\+¿È–œÑèÏ]@İÜÁd1wÆDu†sSåóôÌôZ/<˜¢v!ê'!7Z{®«Oˆ“«ù Ÿ¶{ Mo@“{y¡s/ãH.F¼<rgSÍAcp^çE:,JÍ:Ï‡«Á<B¹dŠ^hLŠİqè¾U>~ºO ´§cÏCA3öÖKŞj{Œ°*.Æ³MÔë¶‹>âƒ“7½ËËşùôj4xÛ›ôºlÊŒëhslènÒÏtDê!$yĞÑœ$+¼ÔæŞ:\=İ@$«Ã¯ùñCo¾ÌV¾ö³Ó=·şìrçm`‡ûoû‰ÁkxÂ¸#ûûâ­1ÒwxÑ‡êXCëÛ¢£Qík[²Ú-±lµ™m`˜¼a5vdÀc€üûŸ¥V54;-¤m8¥iû·›Ln“/{]mËLİA†µ½’B(íºğ›3Õ¯Kı—Nc,C59€²Éì$ˆ€@ı¤É½“YÛcÚ¨e×û/~p›/3²Íc†yÜ°DÉ¸a‰Â¤:Êokk6Œ\W²À°Ëß;µ•ı+„{òİéºH?fù¶œ–àTŠ°IÄ­Ì«%™ÆÀòì…cUşĞoÇÌM‹÷5 ´?üA›mnÅ÷Cé”AbÍ–6·­q\¤œ+İF¼‘ËíìŞEna€£‘÷~ŒÓÅ­‘÷Xs£¡0çöÓ|z[äK‘jşÄ|£
CÁ<¢jpO‘™ïËÂª;iì !¿´UW½âk5üQ:ö@â¼ĞÙ?Í‹èª1µ	µ¹OªX×Ïş¾gºY×Ï£i$â	Ê¸Gre…œÊu± lÙûíæ~”–Û…äjZÂ~“r--ªæXµÖ†›W…1bƒØ2hÏçËGÿØ)ò¬5VĞWHÀ†ÇÃ`#lĞNDO»ªéÕ<ıÒ"6ØÅŸÃÛöÁÿÖ=úÆë„s½¼e	§ÒøÅ¡Emó_"ŒG›ÿƒñ/­=ÀÇóŠØãFo Lè
QúztCs>#ñÄ¸(*0Q§^w|Õ»œöÿíäüz<xÛ¯ş"÷=¢Ä½Ú¢(UÀ¸l7}@{.›-ÚoTÕì	ĞãüóIºX€‹ûG‡Æ§ï«¡œg«4)x$
¸i«~ğŠúwjh³ÒkÃÔ·ÃŠrËD½™îÛşh28é;Öë§dlÍµŠ÷S9
ãŠAÑwÓ¯“;l¶Å‘FåÓOE¶I»
ÒmëÇÖ‹Öw­ç‡¤W'‚îçä==šü)H¶`9fbîfÇ8-’Oˆ¨ŠK>“y¡¾pÌ	´vÁˆo§"Îé'ÉcÜ’PM&±¶»+8366™¥¬å26‚ MCŒTú¼#Ï‹î<_&ÙªS¥H¢†©H½¬¢QZIø´'‚öó´‡ä¹Kô“-z$ù:8p©—Ùù®„L>®šÌÀ	{òç…3ßÔ5¯ßlÑnvï¸ò¢cù9@¬0»É™üvX;ï¹Jæ Ju‡—•£É9#Î¤æëöó?ƒ9õå/Ü¶Qkñ5Æºqå!ƒà²ÁcLO9#Ö3~±ã¸Í#'ÃmãËEoròfzÕõ/'V¥w£ŞÕôdx9aŸBDh¸Â³N…ıè75yò³&Ïı^1ñÖ%ÂÈŸ…K¸§P6ÿG€æQµ8tVÄæ‰ö:+åÛCo&F˜ö*Â†ğêÏß×úÙUèJC¶#–àÄ­½oÑÛ“0s†5IÈÜ¤ÖêµC´Öqæ¤v‰òÆ(Ltœ7Ä^>ù£ÚeÁª0|£a4Ó€O#:ËE0MBalŞºÈï˜ÜWò®óóûZoèÑèrOïœ±hÓÉß®úÓñÕàò²?²ùÂ³Â’•à×ZÉ}!]£¤ÏŒG	]¿ºÍVˆb/[F5S¡ğí‚P­ÿ|Ùú—†,P™÷Á/ú«şÔ[¯Ê
£ªÅ_ù««ª¼ˆ˜qQK˜\ñ‹c?û‚·(·XäŸĞ»ZmÏÄvH“H÷b¢<lÇyVBXH²áÖ,ÙÌî[ŒËf›­!³»U^ ]_Ä0œÎ¼QyxŒ£}™t>Ây¬ÖØÑĞİôp×¥Ak†pM°0ì=í‡Má‰¢›¬™®—vVË®´ñ 8MoöOĞ«äQ”]#z™P†Uj:GMØ1iŸrŒÂğ»!©UÖİ%”„õH„%öY=,šG3pig`Q©¢8XÎÀ€ãLé‹êV—Šk$-è©3Što¦Íõ›^BêG}<U2cŞê·PÑwRš6QÙ›J™Ú¬E©ü^R~~½­ê˜7BØö‹ìFĞ;í}âd,Ø.¹ÓKñˆßƒMºaws½me…ş£ü“ÛíáxëÀ!Mèä>Yåå7 ï
tG^ñ‚(öÛ“¤˜·­Èâêb}Ÿ¯RõëãŒ•U¿t•“xU)œ¬›ŠPHîØ8]¾+®­¸š¥K«;{ S°İäÓ™Pª²¬*„£œ6+ŠÿÆ”"Ø:|Q5*`‚Ñ€úUWAUõ(§.Â®Šü6[¤Ş»0x‰~\n<õZ·®F•ˆªÌaId·”bDÕû&}š¨~ 8ÛÛîòÃœü&Ù¾í´8ğºg·vèFx·2:¿Ö½h}âÿHôÔ¿µe£âç-uÔñú\hSx¤ÂÄÍNr¯ LGG5~i'!®;`åM ¦º½'OßÀ)€HåĞáÅÏøí¨	ÖıÏÀ¾^n—7àvÃq¼c……6;e\†)„70c‘tıLÛ~z¦Ş ºa²¿4`-V7ôqÂ¯Å'¨)?ÀßÜÃ7›Öî`E’}œFÃİ×¬·uj ugıFè-˜—š%	7¼áµª{H¨ŠP”×+f’¶€¥î…¬^ˆhŸt˜Oô!½ÉÊƒyù3ƒÙ{yÿ”>Ø³°ãëHs—C6ïí=5¿µ¦'N]„Ø'FÔ0Üµm<‡i œÖ®²(ušw‚(ZIİWí{ªw*¤õ3½³A^Îg« 3ì„L‹ñæu”’Ä!_êKÀ?ñ
¸v™·ÄŞP3ÜI‰Qd‹˜Îï2¼µyÕÆšŠ'u*…lüë7Œ¨6@€ãûbéÂ8Å5Í‚ˆ±ªœGëö†«\ÛÏ©ó‚’â\”E="Ç9r—ãû4İx,;ˆ´ø.á5²1†-ÏØÂn’å:àwj¯¦€ü‘^2xô6+¦`Ù¯ì¢º®š
İU:+GÂoèN·¿ÎWÍJ-GSstÃ¬…íÖkÔÑ²™ÑDÎ¯
¬ÿ
Ïu‘1d·Lö‡d[ï»ÆçÁ¦T‰;Í7y¹á‰FŠ¬£>N/ï}½Êfù?İ‡¢íC:!ŠşÄpö£qaİvàıbÍõzt~šÂŠîÿ5à&AĞ[,ÚÿşïOK~ğ¿_ÜÀÅöÁõäìÙ_(fs—	Ò¬QıŒÌXu»šX¬ñ¤HKß¥›/â(‘ƒ¿&Ú¦VÛÇm§JúÀFú‰qÎpğf%8w^BJV|Õ>„Ÿ¦¼OÄÒ'ŒãJu«Ş‰©jğa½lkcıQ†5.Æ [ÍR¨}÷Kë;¢øú‘>\ùúáğuï×i}ºÏf÷5êI,ãµ•ÑK6%÷òa)¢‚o å²®®µ8aƒ$ÂáM~šñœ‰wÈ¥–E·ê¼Àã²Uòü¯‚![`uQsßì‰ìV)ĞÕÊ8š-ı	­Œ³M–¹OdÀZçÏ û÷¼ÒŞñaÀJš1Ô`(kWîfå[˜§
ö§pÛïoóÒlSŸ.¶e6«óDaBÌ¡*˜3ã‚mŒÿfhDãá¡†½í øfçu!ñè±µÈ–êÛ*F¶ÜxGd§¶™ısÚ*½	ücÛ´mê›ôuS¿ Ü9Ï@¾Ë0"=YKi×_ßÏÂ×6Î~@Šîwàÿ  ÿÿì}isG’èçİ_j7àŠ{vmÉ’ „0JvÌs š@“è€Æv¤9óß_eÖÑuW5 Rò:ÂÑ]•ueeeåÏı\ èeÄÜ6up[Ğ}+ÈÜÙ™|Pä«ˆ™`9ˆµBf‘ßM'o8QâÎ—-CÌ”‡ Â™òPê_pH#òäÇ‹EDF·^L–ËEùış>ğ—û˜‚’|Ùx±¯Äªƒ7?-ßÀ;ÇõS°ĞM•£b ‡b£˜)ië¯=š®o´ôu³]Ú]MõO´ØAzAÇ?›‡ø0æ 9a¦ö{´)³jÃ.V
÷ŸZ\ÏÇ9j‚M­X¹ØùÍ­»_5ä¤òÃ;ˆ†5P«’ëæë½c«}<è^^¯û«‹öygx|ÙëvN"¤:‚	Ğ;öo/×_ÿûVûû¾İ?h¿«Ùİ­5Ö½ø9ºíH-†$Ÿ¸œ“õ	µ”l{‰Z"$æğÔšÇw>.-2Öv-Ï³Kı±õjÏ n&Û 6K˜ç%É¦¿/AË#J³cüT‚pşgk™Ÿåiqœ˜êlZâÙ&IIn:y÷µ' "EÎzQEgó®Qƒ-ªù’û„ô²Êæq5­
>2[
¿İˆ|”“äJêÀB#~Ó¸‰¨‡º»\­a0G0œy¢Š|‹åkYó)í1Ã½Ó¼_smšh§óûZ‚Àï5“—áÆaÊD¿ÎX~ó#†›vçãÎêóË“Î°ü¾sr}Ö9@ÆWt°êwÚWÇïcòƒqu®Ìû¤œ,“»~š£‰‡AQ:»Õ&<·˜1K	­‹‚#º5J/Ú,’Øğ„æW”ËWÅĞÄ“Em¬Š¸ÈŒ#©+gºm@¦R'€}@Ë‘Æ=³§—"—Éf0·Jõ(ü¿ßk|'•?Ê—Óøş!»¡JÍÎí-PÙv¯7ì\]]^µîñsQOê†‘ËÊê,…‚½§7r¨Ãe,M2ÓÒ$<¦±]ÕqıCïi›Ù]Ø½¥?Kï4“~:sºj%²v;Íl£\$sİŒB5³fŞã®cÏZ˜YzÛvàº¡ŒÀ:É/?H+a8ÚïVu\1›ì@¤‹Ú=©”“wÑ±4jŠªCÍ•…¶Fv){F)™º¼´&¥RÛm+	CmbXGùÁãX·ŞÕå»«N¿7“Pe@Ã>®â™E“XES4lÿµæ,¤óq“«~˜İªtBŒ¯ <¦¢2·’2ù0ûá›<Ş4”9¥Á+×Hô·×8üÖœÙ™På‡uHÓ·[©ÏOh¾6wŸ‘°œ^^}l_|%ÿÄ‚°”ÌõÜÎ¬TQ®¾xÒ!YZ~eQ¤©üŒ”äèr0<º./B R>ôvÿJ~ş×“Ÿ^’Ñç+·ò•ÆÄ•×iL¯İ=wNºíš4Æaô•ÈüÑˆNó”tâ”¬èU6šp­š"oá]Cqô‰qï¿”f´JÉğX=½²P §“¹ıƒ‹_Áúæÿî“/×ggÃ6„F<ë¸æÏk¤è²Q”óˆTA:i…,æ*8iÄÜÎ†J%|R¢ ™ŸI9Õ7ğÁ›oó‹:ãıí7è9.Şü¦¯•qØóæ>óá"Mud5ãF,!¹	$xbÔãêÀsûG87àá£iå,zŠuX$©î"ºØbyÃdÁ8]D;F&3µ²<}ó÷–±Òm#Q©ÍˆxO°Æ4è“Oß¯ TT¢O/™ãÌ³Ä?Wb¬°},;€JÜ×¹Êeo]>’Œ:j8)¤#T0e&ÆrçÇæÅ„ú$˜ô$“O¹[àLl~Ó6\asEw¤…”§7ñ/İİÜ–û™(Å¬UNó0|Á,—tPj%;İYÓ7¶àb$áI‹é£Æ£¢ç×ôÑnˆt«š›ùò«û`Û%{^Go+Šêo…Ş€·…Rd÷üyÜş4x#ƒsU‡"U¼pÏ…QF5_Ü¸CF>Ÿ¥š£zæ¯È"Ğ% A¼‘€ä5¶5 37¡Îj:‹†ÙŒ¢A» xôc˜‘€—¿é¶ÌZäåÒˆ!\]^€ÁÈÊğa…Içfaù¨x„À{ÔM¡B¡+ÓA–éÍ‚|êA¢j§{€r®Z	éÎ	†«.+S‚ÕƒœÏEÜh˜@Q³CPÏAjvµd÷¤×ĞuşãĞzúû¢l€…Ïnµ¼Ğ‹=D¾šû“ü¥ËÒ¢q˜åèvÍ2m"-1V-ÁÜÓ¼h/¥;ıŸ09j€üŠ2¼`ëÛlšïw©MÆRN_â&¸ZÍ	Ÿ'íàf­š,é7ñj¶NÏÚï†ıÁ¯gá óË`H î:ÙExÜ3áÊ+­¿©G‘b%’š±¥E°q{‡|Fƒ8Eş8’­G«›ëÖÃ½7êô¿jİ÷ şÚÑ“«H4ˆ9”ÀÖœéµ:—Í!€õÃ°·ÜS+Ñøzê8‹¥;ºÍØÛËPqR8e‡tÄjT’ó+5LÀœL•æÙ•¢#fPowD3Œõ†!`ÙD^Q5t)X‘ÌÇÔ+T—û•¼¦,˜Á¸ÒÂ_œü«~ªæñ£*½Xrå°ñZ–°¶pK–K^4ˆ‘Hkzì­$ú‚tğ!›Gıy>ÏmÈ4
:A•ü¦Fò>øĞ¹êşj#3âø§­ !ŒàâQv.³°kÜ=¨Q¹„aÂ#™â@¸¦ÖpLB†#à˜º³
´İià‰ô¼1Q÷ÃBÌ&#Ô4}V="tNÕbNaR±ÈJK¸ÖPíê¬idİ	EB…§¦ûlÎ2â]Ì}äv¿p"˜§_CxÖ÷lrcÀ)htBHn_3†Ç(PÅxoŞ4š8F¦ÙëK)/|3 ·xÆf`ùmß–ôyşt‘êÀéVaºš¶
Ê~0‡U%ú®»â,Óé÷v~†bpRŠÔbï[kú«ºø]czÕ)Ö¦Ùyê£ßw;"±©>Ê—õ°Ac0ñLŒG‘ğÏÀ ’´¾¾éĞEâ<Ñ¤×ä8¾K0¥Ç'iGqk§0•U:cÚ,'?"?“lœb•^2O§åŞáı²1Ã4Ñ(M;Î°˜8”ù²Q¦é'P'v"="?¦7=`Ôè¿Á¹veø5_V7)°äš.Ò`Ü™ôÖ¿YæÑ•N×c)&ÓÆc°€¬3,xt;PÎÉ	õÜ·Å¾'x­ÀNèb<–#‹¿i“·h$g1É—È…¦wõÛã=m•A‡:¢òWKEXıDºÏ¤Ğ‡¡åÕC6^Nô—“R£ï‰BîüåÏé#2ê²2ƒ¼“µ½‘Üé›ƒ‡ÛRX0µB~ó_R„“l´ü[15l;õO×ô7T›Å ÂÎn};µ†r`œ<k8¸`›ïşDÅvĞ›#=­m_ŞïT›ÓdÌÁ¹ŠÓïıFóvš'Ë]‹‘Cë˜tQ¯öáT¹HÅ¢|’#+Y³ÜÁĞ¬yHYwÑ·æ@®/ggÙÉ´Uºçc|^ID=ÙìjS¼DÛìêšKErÒov-W^ºd Z]‚½iòHxæ*‚G­xQ¶WØ4‚Qëª]ÀéÕuşZ˜¢¸yesLl0½„ÜïÙ1õÊ2ö¹ˆæ“}ò©é×&ÃùÜ>oÑdh QWƒZÖ6b#wZPG/ñ–äÉ³aï”',d'qßº…´ó9¯ÜßÙº+œpaš/®|WµÚ»ø1Øš¤6E•ñÃ)ş–$Añ†¨$Ç”cï¶#è¨ ¹¥ÒZ{/‹5"ñfäø9.Ì&ÔÍƒ7›+Ô_Ìo+àË‡ª¡¥@…V’ÎŠ"³$›.sÄ’:ft™|`WÁÜs5ıôQÔã{	×ÜÎêıdw üÍkïU?O¡²(“†Ğ…lıSÚ2e²bÈ£pYI,~õ¯Ó>¯Ó%n{(×÷M3š¸„û#E®@x.„Ã"¸“Ç6ƒvZ¼)‚Ğ´"™µòânÿ†lq´‰kò³r@,& Gå ø‡¢6 3»‡½›Ğ,!F«@84ìÃÂÀÎ’Õ|4âK28ŠÀ~’d)ƒä¦TmêÔk-‹Ç«ö;ù­ñMuBœnŞÓ±˜~íb™¦)½¥2A.ê¸¢˜ÖØşÕ¹™ÿQ(¤çäÛ„t\Œ—ùò}r¥n3P9Ç<#ºë0ª³&‰ÖJ+¾A•%Ù‡Öˆ—%2WÙ5,“ı~{š‘ 9¸Q÷
ŞyRì-#X”ÊJZ²®!gn5Ôè  «¡6äNsğ_XO:²´Ép-µ'ğ½lÇwƒemNÄé¯Èj‹Ìw¡È¢ÕÁ İ»ÛacOhÛØ²·I©¸z_Á¨ÆÂlWàÅª9Ì°á‘PWQóƒQ‘nšÃŒV†*b/¨ôCˆ®41ŠÖ˜)â»Ÿ©ÌÕ£§Òˆëƒ{ÒPÑöÅÉ˜ÙQ-™ÛV ~¼î6Ux .ÈG+¡ĞN1ĞH©km–'ú9úà’É’çmtåŒ‹‹Ö‘Á¹kó2.éTDš$ÔásU¤²ùøF_…Ãáä®<©²å,Y4nè?oª!T}MÇ´‹!ƒN³úlA8ÛëRYŒHô#«B»CÛ„AOË[½zZ¯Šãd~Ÿ”ı‡ÚÒw| >­Ø`½ @é¿"İF›™×Á ÍğÒäZ;z™ŒåGXß&äŒF\«“Ğ¬¾_c4éƒÇO/FpI^-Øî¬<MÄıÚ¿÷l„0]¸tƒKn@-nö«ŠÎ¿ö:ÃŞûËÁ¥±ã#(¯Ì¤šúKmÚE‘<Âı£Ò…·„`¸X|ëçö’Ò¡ÊÅJX‡'Û ñ72 šlÕ!?×f5<ŸT£…`ú`Z8«i:è^Áê5E#{R{ZÎwB:„Â·.Ï|‘€÷Ş³èšMÓ²X„©:&|·¶›Î,øq‹A¥úƒ)[ŠÌÒâ.åŠ$w1yt´T`ˆâ/0yO®4ìfüD Ewïr²Í9ˆ¬¬ƒÍüy…nj¬8,]{7…ìDY¨oÇ¿š[nóârœOnse…—†Œ}³E^A-Á=EîIsF:ØàT^£~#ûÜK˜™s9*²Y/_¬	ZæÊÙ®+{2}è¬¾KVûÕ=ï]^Ú˜7lwúıîQ÷¬;øuxq9|ß%Ôñ¤Ó?î\œB}²âÁjík²kİ«2KŠÊŞ¾]"ÿj˜  @]”Ü«ft/jDr‹ŸL {îv¤'~Å´lsƒ¶Õ6D†¦ËÁ=Æì­á£`«àsQÈ=¾	ëø°zVËeÛrJô0—Å(ÔèÌ#…KÄ	Wt”Şeóş”0w¦~rÿõº{üóğªÓ;ëvúòé]’ÅîÌ¸»ÁÅ=ûÙhÔS(ÂbKi¢•(“›4À÷UZ®¦ËvI÷9ßĞ-šz}eÑ¹…š`ßË‡l‘BÊîµ¶t1bÉA.•¥6	²|d«ŸäóËÛ[B~pÈJ_`¶ş{î/Â†ĞLáy‹C¸*EOà,E&»T™-Å¬1Ù†Úøšç9¿uEz^Œ_W¯|“yŠd¨3»ÇĞéU\!k	…â?ÛÒsjÏW{’”àw\]úË.Ø“Sp5ûAñJM“²TXšö8!pÑ —8¢l\ÑûN!nÂıP"êËÜ¢oaÚx½ÿ©¢…ğn1UvcÆşøÁ(Q¡)é¦ùøfr“Bñ›ıë„üï*¯šWßyKzŠÜ€?ëmî‡Ã
!ºûK‚§FŞ”ŠE®QÅ€¸„[Eì2ÂE!ˆZÒ5jÄ>h-iÌ[ŠTR€±+¢öğRùËèœ7/él4â:åÈÊÓ"ÿ{j¸‡ÀÃÉàPœju)›Ä¶ÛßsX¬ˆš³¶©1™ÙáH4LŸ©jÑÿ=•ê<Øo²ÖØÈö„¾€%NQÁY£kÚ´ÆuÎBÍšœè¿úRrÊ’@FÁƒt	>V9ˆ,®ğºu“ku	#ğ>}@ˆdwläT7—yv2Q£OWé,¿O9å&;¤T÷Ÿˆ÷•Ü‚)¡×úÀ7IæwÈ*[´<èuR9?â`±E9ç’Ó_ÅÙ6|§Qûû(º¬hô[‡Cış>Nûj®£Ëìö2·Ó¹K ,ÒZµš,{ßö÷í9‘„~¢;Ç<Eãy,Ãúà1s÷ßÒˆCfIÈÑ“Õ·õ‘^õ'¹êÔÀÎãˆ~r›št|6©éı;ù!HÁÈäó»Zg{ákåt–Îj{ñ÷‚Á`Œ„·ØaUîĞ^ğL=öe.À[¡b TÀ^é\;…%™¡y0Û*ÒƒYªÆOj×”j¬‡Î¸+ªŒˆÆš8×ÛÎ”Ø÷ˆ
$Œ¿4Şœ„;£®¡ÜŒsä»±cŒeßQ×ÃÔVzG_jlc±wÌÕæmhë»£/°«¾v~sˆó«O‡ü›¼×v”ÍæjÇ@¥—|uelÚ1ĞÉUÓP;.ŒR€Øˆ$%ß'ä†ÔÇ<Z@L­>ŒvZ©İ¬ç‡ıÎk=ÎÁØ°3ÎA‰r©ğ,îU%k~Ã.ï	Îä$\RğÚÎÚ˜RbuëéÃ¨h¾°‘Zî:YF;çºÌ*s+×ŞzW	Bıë²©uk ·ŒL}™S!ä¶¢ºTóåxù2²ûöû¸ÙKÇ­çm÷İšõÜyóî«µqååSÍÛ9Á:çØllÇ”»9#ıVˆŠ"(.¶ª®cµ­²2j‡o}³øÁoÔ9=³õ¯.nÖ_¸F‹#ö™úz!ÕœéoŒƒĞÅLÂ#¨ÏKSIë„,X¥
®uEwÈd´Y-Öæ˜ËÕ^p³8†xâ @8r>-É´*'B+S+Ú³&:WP¯Yû`P“Îiûúl ßRõœ–‰RĞW¹]üğ†wñË~]âaŞy­x%é%h8f±<q_(¥PkóY|„ú1ĞDÈÄ¤o¾§×ÔˆgLÕ¸êëÁ^@ƒ™P_ãáiÖÀm]môª>âñn­³Àö©´¾UçZ.1ô²åD	á¶K·v£	—Â¶xGî8ô|ìœ_w†ç~¿ı®ÓwŠÙ‚ÜšVò0ØÁ¨şZÁ¿‚Ö/Óg~‘‡Aõ`¸Àı	Y‰ÑÊí¶7*Ç\J¼ÏO„•1$Ñï	C”FrB©É5OºıöÑYg(2>|èv>â'Ã‰++pïZR]1aË :A½RÃ]an`¥¸ähSFÊ¦x²c€Ç9/)Šk³ğh¯#ø­×_-Á²¯úZ”ù‹&Ì“aû/‰¤˜¹W$E'XÎ-Ós1Ô/>e1GTB¦i®xï³r™ë\ü+2¼ú±Î°‹Lã­ölH5œlS0i[(öíd¡#«‘?Y§â˜9T&€¤r$Fh²H#-—…şÀÃ¤l1‡&+ëÌÑ"•1o¤1Xˆi²vÇhd¼ÈËÌ³&¶q0€CÚÜøƒ„gFıõU¨çê™:	¾*o-ò}¸›òï?jg¥Ç€A±$ ^ğwÁoÙÎI´ WÔC';*9Z’y°Ã:À·â÷§(pŒ‡õÀù6
tv{`ı9–|	ô üh€Òe×ï/±¨cÓs»Áşß:ka!Ï.†TWFs+:´„ÿ½Ï§cr†çsÌbV¯šğç;ˆµÙX «Ê²``ù´ô~£Y"Í]b	¿£ç5	xmp¦R½@›Ü.F	eÁ‰¹OP	VŠäZTæ«b”z\ùcxîj¿m¨Ğ?’VÌıE?¸SD3ÔFÚ8ÛğìYÜx»´¥#¼EŒãcOëcõuÓA‹ä¶rA,{y>¥Úíóƒ	ÍéWˆfP{:ÍÚe™‹ÈÒ–"g"­i&4¤ÑË5d¾hÊŠ:F`O–ÈÙ%Ÿwç„6$SÂ+"„uZÛ´¾%[òÖˆ1/?Ôz'á¸enûû0Ét1IXz
2ş9™.ÄÍAÎÌ7¯Ì)ª‰„ïÎÙ$4²¥¯RòSš.‰ÜÁÆC^|‚Ø £œ Ğh9}tK%ñN‘.?Èr¡<wE³`³©"%èÁ×)3ói^u Ä.41®¥ş¬öëÆ¸6ÎÆàGÔ+ÒY¶š½Ën—ZYá²¨J^Ò;ØøSdî*§«»*¡µ˜Ç( gé  rltH	]Q“q¢çxÀm–˜™Éˆ¸a„‘¥#±fxô:ÛFè´xkj‡¤ØBÙê'Ğôê2	~Ì ’Î‚•¯)&¯Î6á¯4ï(àAj¬.rS_ô( —«åĞñĞ•>+I13Ù2ÿ=¾ùfÁØa rEÿ{ÏVáe¨Î¶`¿Ô,+?FºüÿñèCzóŠftŠö¢jQj‰´¡ùßÚäÈ)­óR‘ƒÑ$/¨ÇFˆø3šë5ØŠˆIÌmnë€$¶'jijœV¯¢QÈ‚˜/Üb^¨º@F^¸™^\{—ı.äVñOvNUdwğ³J2×@EXm|mM’²G±èºöXçüACáQ±úÜ‹ÿäi}Kóµ¾z9ŠŞD<mléY.Šõ÷Ò¤Û!RÓ~1üûğÈŒJòr{ÿlîC¥ò‘wlpš–kªØÑÂKxÇ±=ñƒÇÓ‹?ÒšˆiŒƒO÷$ÄD¤…è#Iîø˜ )fõàñD ’ùò¡/™ÌÆMçgØŞî¯î/ŠG?0#Ì6Úë½ÉêÓYIŠ´ÒåÎ!?&5,Ğ çd l\é±Ï¬Âgá`4v€!‰‰õÈŞ˜_‹=‡p5˜qÜ“”éi‘ÜñhLõùsÇ*ÆÀ~[æ(hPåÊC9­•[# Sße–³TÆÉ>¿~Iù‰şó=^5xà?<46?ÎŠ¾J¸‹k°ÍĞ»™@$1LÍÀ©$©!l.ŞuOuÁ”åó'­àk0·Œ¸`>A–S—	AFh6‰?õ˜0Ây° q‹¶»òƒ&Åu*ÀcØÂ«ùåüºK¥úMà7ßÖ`lÌ{5­,0]4ãOô;Á^¸ñkÍŸì5¿{QxíCÿ‰n§ „ Û[2,vïşÎCş5¸ÚŸÀ°Uıìï),ùp.åÿ•URq5Î3ñ#	é2íEÕlÕÌÍa
0|ùÑŠœ·V¡Æ3\Ó7¼ Ç·.Ç×ôDc™I?¶ÏÎzí^çŠ^|}")HM-éÜå>&Óé"Y`äXÂßLBåÙñ,ª…/ÔØ–Ò›Æ˜;§WïZG«Uzj:‡»JS%U»a¡< ¸@n¬æY>J¦©Á‹²Í«ÍĞ¢.Ãb<X±FC<ìÑM±ª5£üû}Ê6F|›dB›nSd ²×Èl¬±¨pC
¸TIŒ¥ìñÓrbázÓrŒ<+“/G¶D#t‡N&IŠîò àÄ EĞvôºüÕncŒaAéˆ˜-\(o	OÑ:º- "µ@3èuO[¨!!¡5p/‘>¡	¶(bÃ˜şÄ¿[ŸÒÇ!ìÚá8wMÇ¡ø¤u/Ø[;NhÄÚš§ÉÖÎ†:‘-
–’?ÌC`!şzÓ¨‚£mš—i¹EA( ÿŠÃ«&«ÙiÿÏßú¸,''¼:è5‘=9OŠO«E°.?ém'•ç$í.¯~w.à@mú² æ'}°d‘ùiĞKrfZ•a’$–Æx’xıLÎL„fÁ´DFoJÂ2‹Å²ç¸“­³7Xa”d3Š(ç·	~¤ÆºfÏZ«³Vg“¬? ÷®R‹Ô«fö ê«Ö‡PEÂ²×n]‚Ç‰
Y®Ä|…ÏÒ 7xˆØ˜Ú›fºË8IJ¾x1s€‡S>‹ı¨àØtqcŠR„Ã-	îGV~¯}ºÁ³ÙF÷¡%ëÒİ!Ø®—Š‚¬VM¿¿´üÈdŞz¸ºn.Q=Ùk¼~ºİ«'†ÚNõ­^Ëk	2×=®ß½ëô4®´²\ò5.&Í¯WR÷™ö}²L¬Aôg;Û\^‰œ&r J"¯Ús¸~}ÑÉ®§
kôÄF¤<qÓMò¹->â“#›$páé
 Vlµ*üµ‘Àãü”ç" ;XHÑõ´à“•,'ƒ¼½\&£Ix#A îÉèì!›MxÑTwùIV`ĞGùıyç¤Ût¯†Çíã÷H”A>Bq0ÀfQ@.ÖH'KWÆZP;[«<l=°r´æ6ôşşàòä’â$ÛiŒó‡9‹
ı˜ºÍå'f«`Ç6ŞLzĞuDÿÎrl¤øŠîbÍ×<Y_ğöM™OWËdK¯EV4tÂ XÂ-$K3â1b|:P‚‘¥P¶áNŒ®…’Ì0€v-ÎòºÿC İ&#×ëwÎ:Çƒ!½>|hÚWbÑZP*ŠÜƒô¢àE`lœšgŞñå–{ ráÍ±;;Mî3J)õXeq™:¥#”#ñÔÏ S-ì)!§§Z½ˆœÁ±ûŸyLX’´Œ°.ï=Ã^)½	ğ' ;[kaPøñÛ)ÛÏ²œ6¥	Dô‡`[`-q›ÓmPı–fC„ÑÉ/Á»|5MO3 ¾¹JäŞK‹,WuÑØkOb—ÃÊIE+.áA†ˆns8ÔöÈN
¨c±¤sKÀ¬NJÚGÚ¦5G]¢ojj÷ş+ô­s®ÁS™#ÕRK²u`kàTÙ28×Ñ„Bvƒl¬ƒÎæ‹ÕrMX\ø’9RÎy S¿…!¹ƒO¢šÊ×nfsX¤·i‘BÏ˜æÔ*ÓL~¢ÓFÿcŞ	B2ËÜ[Ö¹ÃÛ7eÍ™Ëp1Êõ€ÃcÔP~€ÁŞïYüšòÇ°`;ÜÈÊ¦ÜPüËøÓÇ>FôÛY¡ó/‚:°1±çfèÊ˜ïA7¤…±=h)%ˆN¬s=×YHŞÎJ
S°.x´ƒ!Y,¦”	cÎFÁ]¸‡“½şªñG_‹RõF¶	çs½Y …rd˜Ğ ZKĞÄ3Iµë$we”¦±ë@ÏMˆşÊôË[êe¹º	u´O:1…î®ÓÛİõ¸ı9ZÍÇK²¸ã2ú¦Îi{  P ³œŒá Ö0¿ØkTĞVoD$äüY,a	J¨±Z=èí&+ËDÙ#B\RÂÁ&((iå·–„~LÃôœWh®ĞãµU".^¤Yt#ïÑ²'P‘©Ô©ìš=³³_¿F½Ò[ãc™àMokÿˆÊÓ)]Pâo!^-v¼ÑMuGNrrÍ£oKi³íŞ!¼õíàÁË•\-Š:äßHÄ_”Ú‹léV©T¤ê„S%`¼ë~ç*0 Ñ—õRˆÛ"Ÿ	·×&ìƒB¾±¾u­fKô"ì	Kîµ1†6€"odÊ¯7Ï#»IÂ)škM+ÙÜ¹PnLÛ³İÖ—K`bĞ¥²¤ybt=ºˆæ¾¢ËÆ?ê¤ª•H—ú¿•F½•Ø¾AìîÔ0ÜŸ<weñÚ¾	9ôV¥h¨ß«–\û]ä¢éq6†«JÆí¶ÿu\ñ18Ûœ‰ş!ªahÙS?šÔ§·†ót¾b®L-Äş¯åïÆù×
<ŸŞ[€«)%Ry·¶¿ T­ã+°Ä¤$ƒä¦¬W&F„|<Ïç¹ó¾•ÕUÓ„¼Â­ç†€Ãæ0«4ã!ƒï(zéoµFcOƒ} oV4L¡Wş&Ò UæuşÊµ:+'¼Å8!‹é#÷X°ÒTÎy»}µšcØu*as„„hË‘0àáNÍÂ›²	~j fÊİŒ<€5õ"UcZÿÔ8 ‰äi]Ÿ‘€ÃIÔ‚bhù£g*ãCš¥ğaùék¸RÔt÷ñ-*rBğ¾E{â 8ƒ¤ü«>Ä¦ÿ.IÉîW$·!ù›œ é+Ê(”s¡€MËğSúx“'Å¸õ3ûƒÛd€Z‹ºú„¸¤;ÃMeWöÎ¿Ai½B/i
L>.R‘ÓZ4Äg·ß=:ë4¾ùÆ;Õ;ÍÁï6Ë¨•àßÎ¼šÔ¢~šÍ™7”ë?dËÑ¤‹o[˜ÌÉ?m£ÇÉtz“Œ>=[ƒï’Ùóîº˜>[[G«ÇçW{µœ<_{ødmŞˆÖ˜EdXx¾}/ÄòÅ]H×¹ŠÚM29Ä¬sŒRònk4Rùù„WşQ2?)’Xåâ‘²Ï=İ-TÏi4kñ¬š½ÃÙ©šõø#:ÚúS -²F€a¡˜Ê[‹mËSôI1^|ÌÆèş¾´Hƒ½LfÁfSš[8ÖU1uó¢hQBˆs	ş%¸İ4_üçïÎsiià‚zôÈÅR¨«*1mìZm]|•„zïşíf¤Ğ_ÿ»¿Ó”ƒ-yó[.”¤¸¤}n¸Y^æ¹Ÿ&ÅhÂ$Ä¤WjºÖŸƒ©?	ûşÑÚÉÖä³Y27©˜˜@U¥oÙSŒ!{(60Ó%Të%¾+Ôüš9çE†‰ßil5r\ƒ¹?›NıÄ¬M2¿ ™,NÊ(­§nÁkĞ?Z–3-ESĞ¨–ÿcİxäüşV¿É¿„O‹vùéŸù°pÅ·”"i^ÏKšC*‰™Ú@+yôÃ3Ú0À?[d £ø_õbĞ½d’«i2+ÑîJI#¡|u~i·Çï‡½öUçb°ç†ĞúxÕî//¤œŞw¶ârD<V–¶óªVLR¦á8Êæc)ÿ…+UÆÿÙk„òm-ÿŒÄ«4&ätñT†öª•-ÓÙkŠÈüiÎü‡¬a­„‹âäĞŒAa¬6„”6ZDj10â#'ØúĞ¹êşú}ÿÙícD×íMÃİ0_–­#ºÌÿC¬<Oƒ†ùòe&ƒ°‹N3!ª2ãW 2PtÕnVíUb¤*÷Bç¥äÜg4üƒå·€¯–ÆZJ¤NÌ)LyÈSÉ+ÏßAF¹áŒú³*#¦–tnHîÃtœYòi[‘Áy¼ DÔÍ/š©2Éò½&‹¤.›‚X›Qİ6“lÙ7†c$³–ûŞ·ş±½r— Hú»HTÂt¼
él†	ø¦°6LõP’h)Õqs4qsRë%‰ªx*Î4«Wd\œü\ã hâzÈß%ó¸˜U…8ò‡ˆ®R’„zÚ1W†¼iX3UiûÜbfçÔç½­åc½…jòF÷¢“³ó‘ó¨%“ÕNöP&8íÃËËé§¤Ïó-hIqHY>œ&|.ijŠ·ƒxL	$S£«ÌbbI“Ù(ù^#Ueş}¸ŒË©bØe†¦¤Lª?ÙôÊØƒ³[U§Ş6å2Ÿáùap·y1KÌpx¤ ûöµ'ÃäZ
â*bĞ½ÎáÛÜğ*{eX$Ì™Pº²Å	T6HÊ÷uİ7Y÷õ{ª…Ç#z•÷¤ˆÜî´R‹6p€:jëÕ%P‹Oü17÷Û:'®+CüªÛL]“û*%+S=¡«WàâãÍhÿ$==Z•ÙœY-‘Ú:üØ9;¾<ïÏ;ı~ûİõóc:å3b±ÆÚÏ ê´!]ó õú6ˆeıENV­À\à,ª‚ı…æ ,ß’İß‰AÿøêòìlØ´a÷äL!ºF^Ğ§ÚÚxş)´·¼ú¶Åÿc-õ6r&¯•­PÆjxxí@Öjm|aC$®7{µ9)6‹5Â4UÌ®´H0µÙŠœ×ØŠÖÁÂõ³:Q$ĞóÙÎ¤¿ãsÙsÑÂ¼OÕ
5î´ßÈã}€ŒjÉ@üK©{ğ“Á6$3i9Œ‹"3‚ıtz«
ôRvÑ&³°öÕñûˆ~RJ1/ó
cùÕŠÚÎùTaê€˜¸ ñÆˆ)ã×n?~ p^,Ù<òŸ.­‡Z™¾V)
ó‹ğ"—•İy™‘ê'„ÿ("ç¬ÂŸpø&wHşTxçé]‚ØÌÈ‡XI¶Ú—Ú@ÄŒWi‰/µÒŠéÕinÿ¯ƒ³;D÷î]JV!™†;I—àOÅc1-RùÃ’æ	ã[*Õ%;Õ6‡Uã»rO}rÂ˜$åIVVeY…›C\İá¸zü¾sr}FñÕøªó¼P¨6‘>§ÙüÍ2±.rõ2ÒĞXÛn(o.ğ“BÏËÖˆî¹òçôQ„+aÖÍH u/‡¸Õ¾‰3¶Šf‹,L¨Y'8e_‹¼L.Ayü0”zuÇÛYÆÓôÈ"IöŠµ×ê%•F:ÒuÚ¯›xÎ@%Z¿p]2}?<ÿ5xƒ2_R¬éGIuJ«ì²À4Y£ığYf7T±á¹ëhéu/.âªöt
@J<'ùÈ“’™àv“Ö«ŞA·{¨dUÚ4»l1Aï<5` q„
Ãk/ÁÕ‹t‹Óİş(¶GïØ¹]q×-n4âÜ©U$hÁŞùcÊãü|BÁè´)MsØÇ	ÊoÆa‹&„ÉêysŠRRI2âşò²qøƒ»æœÜÊğÁSÁ~LD§ùĞ.¢›é¬Ÿw©èRD`}vÏÎ'o‡àfYÆetÍ´˜|ÂûzÃs^ŞBGwI9ã$?òÂÈ¼r7ÂL"»áïBt„L+Làµn§É]ÙøF[x¤zxzÖ~7\ö¨éC¤ÅúhÈì§EŸ×’=Ólò§v4ÎgA5ìY°T­¾Ët<>Â«…œ×¨¬NÕ«ƒ×ôêü	»~]—çŸÅ]cû¨R‰?3)ı~
\Q],×™£øúÚtnÑ7Œ¿R{ëÕÂš†ín¬óà¹›Ş:*Ù;Ç9+–3®¸LŒ^r×ÈË¹:¹Ÿ½ ’“Ü±‡;…˜˜ÃMQb‹æŠ¨§(K#—âjr„¼\ïœ—)WEø íÜ.ÆW° J”ÏmKm¨É@ßTá˜å÷^üyÍy²œ´’›²)CVæ4ùUŸÍo»ß4¾küŸÆŸQ„ÖÕ˜hiÀ1\´«ß²É9¶E’‡ìÍüvÊÆÛ¸33ş6ÊIaU­TwFMñOóâ!)@ I¦P]d£HlÂ_+Ú¤¬Ù
o)ŒD·ã!„ôb“RHB«:ü‡%ªàC0T’±Äß'£Jİ˜ÎhÑ›¾Î,^#8«„“‘ı¡&Ğ5¦¬ª°á„©/³æêã;¾k§« D¤şÌn4âºhŸèò·v§–ÕìH€å4\@dMpÆ¢©1àGw¬íjğ·,0tÆÒnÎ7@´:ÙkCD–ÉÆé•	küfÍı¨ø7¨º¡(…Fì6âåÜš¿şqÛ+²{r‚yÎt½ÄÏÙ»LCè~J.¼EsÈòa›[Òa7QŸ?«Eö)sóğœn–’U0ƒA¾VwPv…*ÍÙúÄA]á}–àÙ…b /0ëX =[\÷Æí1H©¡²¢\vç@¼ÜÚå’mŞÉÀX)a¨\e4ÇäEñ7F í¸1Z]¢$À»&½m¡@Ìv2B²Rõ36Ü˜Ş—ƒè)v”­5Íö»'ØŞæúá‚_$ÌÁÜ½ÊøL^&ÅØeG!¹¡@¶!ª¯öÅ-ùsèÕ-7M8,°
•…VoL£S€§’6mC$ó$LÖ*ìÈ[m+2)unSÂãìég‘ğù«“úØ!*Qgë©D%[h…·$V#,!Kâ\²m‰JÂ|>Q	İa5zø,r’Èş<»œ„Î–‹]îõEH4Î±¶÷”’Ú=zBe÷)ÑØšud#lt!Áˆl§ë¢é;¢^K¸"É
ôõ³‰U¶ˆ|¨Ê¨˜qtw\Y»äŠqAüyˆè±¤\¬”Nù¢r^
uaP&Îë¡l¤ŠE†®¦¢s°bß‘Yı—j+cpíjŞ&£Ç"²PKxiTµŒ›Œ
şAE~*ÙLäWo?¯{lhú&ìós@kKúœ¬Ó³Húœ­’¾h¼Ü˜ÏÈwRËoJæÄuáG&Šò²¸ñ,=d¯¥Mì†ÉÖÿPŒvƒÍJìâWÇ½2.íQ\ã±Bàºëoe¹l«\­úÖ‘Óê˜ñNĞèOƒ"™—SÌÄæªº¦åûm^@Ú¨e##}}ıùçGÃŒQ“¨şĞxù2-®×‘*Û­98:[ñoiö°Yêš“OMÂVÙcf.S}H˜;ùß1’«ĞÎÕ†Ütı•³¾Şßß(”Çş¾>8·ÄZ.a»¿ïß°Â{’?ĞUŒ%Z…«¨Òÿi¸¹§tv¯æ,¥ßÓ¤ìÑşãOiÂÑîûìn2%ÿ‘KusÂÿf¦î|™Ş:|Şşeø¡}vİÑhÎyšµ=¤RîEVÊ@Æ@‡õ×U¾Œ’SJåæ9Xè?–ËtÖUáÎ³é4súõù§ã’X;Ç³äX?^ŞŞ8§€:h|@:üÊS:O‡Ã×¾ô90¹ná0âŞÆß@×áQ<÷Ñ†5fc˜A)W<ËL`45ˆá¨ò±=ì‘]|‰ñ27í'@jZá†{ôBÎæwï“r²LîdM|ûë*-7œfØM*ÀZnÀ6\wĞ,_1 «~=§IãÃ‚cG€7Ákâw‚íÉ<›%ŠM9sJá£Â¦k)ü«_Ç9’,ÓÇ6…‹®öFŠxÒ_]^_œ?tO:—(LÄËÑò8™¥E‚1l$´0>¢ÄŒ¬ïªHá'=°b;c0agDçi^¦]¹Œ²pÃw÷Áé•’ÇÌÓV„¨Å×Ó³¿¨BŞm˜ù+})Y×2-î18`2Ÿd`/ ¾Fn×Ë¶Ìb1Sèè/?Zr\xJïú£å§—-® ‘æGœ\oŞ˜¥ÔÄÏñâWøÊ@H«5ÕFR<ú¸Vıq0ÄúÆ‰"å÷©¹Ş/+²/˜Qp¤iv’å×½ãkÄN?ÍÀ«©€Æ•„ŞàúCj//§ãFAsCZ‰ıu‘%Ríb	AëànÖØGø­èY$Í—ûM²ÉÁlğòğ·8 Ú4ãıa±»­ìn)ïH¨â4Äk{½ˆúÀNnƒŒâÂØ.çäf”¦s†-/ş·×¿5^ºğêŒªµ,6eì‰ÅÑâ§Å_Ñ—Uß0„jÜ5mı‘ä8¿Fc+Ş”ºê³7ğ7}+Bq5FÚoïÎÅb;¤AÇ8ÕÙ}Nf·Š¸€üZ]Ş‚3vşM÷£ôµér‚Œè–B/›
*ÄBUú9òŸ¥½£1¥Ñ¶Óäq“õœşÕ²ı L$o¾•ßelj“°×`r‹öYgøË%œ»îœ [jé×ghipÕ¾èŸµÑ×Ÿ´'oûWôPxÜNóÖı§¤…ÌÄø¡}Ö{ßŞkà6Ø°mr»²·Ü#“€-
Ù+òEZ 'İkw/CŞ'ÿùj™âî¢Ëø£š^cËÀ¶†Dn‹|
¯šÇ«›lt”ş=ƒäÕûV§İï/¯ÆùbÙ:È³6Rş²§œ~)ìI=Ğ“ÎiûúlKĞd"¦ÒŸ|Oh
k)+\õOR¹ypè£ù€œ¬
ª2ùJâ¢oÎt-sPãa-¸iOÅğÕ»FÄ”R¦°#Êİ‘á/Yöñ¨#â/üQ“t	¢Ó™›bŸ$âj^Ó‚ÑÊñ:¢FÇÂ‰áÉkZKÂÃ¨ìãZ0ß½„·$~
[ Òf,æ‚ò¶ù"¾½¨;ƒğÔ_TşÈ‹[b$áUªG+nä,0½{İC™uz	¥^N(Àz‹OäÅ\~6(\CÛá˜¤uç…%ÖË+C81=kÍL­Ò‘ä•?µ8÷:€·Ãó§Ö¹½>ãhn&
ÖÊA4{ÄŸ&R>K¼Z­H
?‚IˆÉmjJæ“0ƒò'W|Ó˜fâÔãÏvåÖOAşl1 Ç24!•úAÒdú³·=z„Ç&•¬\|èö»GNÍ|è©G:ë×pEu=ÊÆºQtòX‹E¯ÓvºÅ	BV²>ë)[X¡öüñçl>¾¼í/³Ñ'¤ªKS:îÌòÿÊX¡’š-óBe:‹-’,›=Q½€XLş'RÈEµÍ}*Œ4èJktŒœ9°ÔˆJ?AoK¾%…0Ao¨²Dlb´r¡)é>nü„¶{»jUÛx©~=O“rU¤ã÷)(­ñ{SèÓğß;²Š1nù*%øp¾›HlBuö¼ğÊøæ”ÍÙ¼¢•ÀO¦j”ì³åª$£UiEgässö)Ï>šfj)¦¯ÄËÆBH½iõ!KaZ’»Kÿ®%e•?Á„ğ™ˆPåFbfkœ•³¬,	#’«lıÔÙ-¶§OõCøüªexêñI^s€¸oA
j„Ñ7H»IìÍªjÉ¸k’ßøŞĞÔŠF‹ë±|µÎYşÄó/”AÜ'VeTòf«±O¬Ë†g«úì
 ª66\â:Ìşpí÷ÁmU^Ç¿˜7döõíı†ÇPÑèAñë†zxNKoÎµäİùGÂkæuµäğ(
,c¬£}†‡[!š'fò¿x­h?<[_¾±äeX4¿ıK­ëJtÑzZÙh°B:8$„ÌwÈt-ô€}Û|±8¨-^O¨a•ü†ÎÆ-I‚™˜“Ù‹÷•ApõnÇ5EÀÍT%ú›¥u&FËóSw^¢KÆ^osnssşÓm_ÖÛğ éWnd
•+\6¿‡¬“è&óä
Š†]şË°ÃŸD³³=­CÅbp§{ìXfFHLáR_­ßŞÖ\‘ßcÚZZ`xÖWpÀó9ÉØ®à3±>ùãÏ¶É ¶HùS_ù@k}>TyrÉ•lry65Ï^ã5¡$ñzÛxouæ¢u‡{–KúïªÚË€‚IjQÉq
wÆ_hFĞx²of¶È'5´µ&´èßş9Ú"Í€óTÊíÍÙÜ'QjÛ$†TŒ»¦Ğ?_0oâUşœ¬2<Û²©Óa=‘]]NvíİË•ÕOw×­güæ×3üÃŞv—ÜÜíëu÷ùuW¬ôŞw7½ğníÆ{ølW^É‚±®…áºC[ß´P#»|·	 §0PTÛ¨kƒU['ò	¢ñõÔŒX|sàc è¼à#•Ó®ŒÿÃPØWr(	Étq-X`ÈÔ`!óÄÚÁÙÄˆ\Ö­¬ìÌhåôÍ7Ë÷5K.N…m!¼ \Á<³n¨2Û,÷X¹ÖĞXûÈ3ÕV·•Ïßi
ıcVvÒâÑüŠ–(Ñ5–ioqUd†v–|J™ò³}»¤ï¼\L¸v˜ÖØsİ€Õo¾jÄ68ÊùxşŠ”ËªW2˜D€šñ˜m—|74Çœ¬‘k†uÌfA›‡EÍÖÙh›e›¹WÊçÍÀ±vö•/%ÅM5€ÃÈéµ–Ûzj{Ÿ+­e|–L7ŞÀ¶·*Y`Ö¤øçV¦@SìÀ«z`€=BMõW?\TjöÉé}ÎWéëV?kÕîÌBÃUì]œ’Õà·Wˆ!(¿ªÍÑT^5e4ìœTVbãK%"p¤ó|[¡‰u!’³‰TBó÷JÄq;<í¤É‹Q*¬™±±É–	Eªõjº£Zb#–>B>§Š¼©Uy°—^âzQ½|¼„Zª±ªşd¤Eá&-U=ô2…èw`7M›„AR¥FY6³yEnğG‹çAH]¼±œ…^£á°©ÊBï—¦¹…ûC×
#»M /S¥ÈVÒX{ŞPÌªo\&yWßT¿$q”)jbb$ğñĞs“aº{n©õİÊc²—aòßDÌ‹Gûa¯pä½ÉÉæ¼Íñ'¸c^ØµûODuŒÓÖØ„ØHa`¾>´5Î>ø!¶™·fR8…SªX¢Î|ì1¥í¢HaW«òå·€9t!N‹üï©SÁÃkÓt3¤à¹œDÔlJĞœZ†ËŠ!!K×üx´Üƒ7÷ [<;iañÂ!o	Âkn¹zTØëXF;Ò:@­JĞt†`ı9ÖE2KÁëÜ‚ïğ-ğÙwO{ÿ;xğãDÒ€~°ñm'VæÛjÑ,¦õÄ¬=Ëe2š@”if¾­œ~ğ¿÷(Ÿ`imÔŒ%hÌÔ?9R ™Ü@iÿÅ»ÒEğíÂÄ=‹hÒ$<ñ=uS}H’´ŸÖ
­Ms+R°ê}y¦_éî7&45HpôıB­Vëra¯Zóf¡H†Ö¸VØê×¹S(õ	›	‚$çªIãnãuqæÚO8Õ5”.¸ÆG-G¡óªïéëûGj²IµvÛáì>Pæ±‘B©†šˆJ6Y}7·G«FHSù—¸Wê¨ÑM%şW×,HØÒT¥gşÆôXÉ(m¾ø«ÓÎëÓ{/BÒ`ÚR¤ø»3Î¥/ «º2ËîU‰İ£SºGÇÅr{m.ûõ¯º+KG::ïÉbş¿|‘0YŒW¥=)óë+F§Ô?VwVóã™’)Á*÷÷rWê¦}§¥¡é„m°{CP0›$sY?…KDâ–­`¡ñŠ‹3h8t<(9^rœ•p|ô±‚79*ô„S<!mø7¸™ƒ!e‰Ù0í[lƒk¦ÖÍ*Ìrà¸˜Ú³RøäĞÚ†<¤£OGùïÜ­m9‘îÅL	)Ëbwl„&NœNn-
Î6 äøwõ0ÉÆ0¨ÁAr[jgyàŞøîéË¿è¿ñøõšz°‹9ôŞ¤05dŠê%£O€şó±•jíÁxüƒ¦PêzÒ¦‡¤ÒÚ‚{·­Ÿ¡ÎØ0!k\‚ˆ}/[6}¬1—ÔMÆ¸ñø[% Áœ¼1îèÓ¶gN$æº d³Î{¶Ú8j§Ï®¹´l¿ÙèS 8ù„TáœØø(ÜTÆƒ7Ê;˜Sƒ=7š¥Å]Ê_ízÛ ÇfÂ;ĞH&8ôp†‰ÀLÖj«.£h»mÃj+Şí¨¡Yà PÙ*%÷Lıé¨“ª©1WÚØ¢KwEªğ¦’’4“~6ÎÓ ½BuœZX¹®_|
+D“ Ò}y¬ôï"ÇÏm²á‚6 RyJ`j3Àl5.kÎ×ÌZ_3kÕaÏ¾ˆUšV©šÖD½•´5Úè"(j]Ïöb}»‹mØ\lho±­EüJú5ÜMC…­ÛİŠFYcfd,,kDoÔÏ¬—Ûg¿*(u¸ŸIAi¼
¨HÂ*K³DfÉ.H=&œìß¸¤Ó3¸	©M…§Ğ†â^%³ÎâYòk£W,"3q„@}È3Dk1B«\ba‹Ä9éísyâíW¯Ü,ï€^s°Ç˜>r5ù™$@ëyE4‰€<«y’?ÌUELD„ßè@d@©Ù±T²­ i¤/pÈMyGõ•®ƒË‹áyç’sì3×ù°ÚÃ¢…™%Å'ÖûŞ.¯ Ç¡qkWÌíÀ |Nc»ÆOég+C,Ğ½J¶è?od5Ôr¡K{²E{›õW@©ÌŸºje·ïŒBBÊsYi¢bÚ«·£f°ÿ’ÎÙ ‚ª:ÈÁĞ^
f@dZµ: å n©ódN†SÈ²t~R³¿=ç+µÙğÚ*^@’o&œÛ²†f—xq9ì]ö»°¡0ê.1ŞíkM¬ˆäş!)IçWÉ”š=±_Í%€ïr G¥ÛQO²é˜’%ËZÚtWsòÏË—.ÔÄ³y@]µAcå Ğºk/™;«íÚdM`dÎ•÷„%VªtY–^Á³ğ´¼6ÀşımÒƒÀcâ•Áá¾Ü´{ğPöµÙüJwNsD`º:k‰VnHW>­y9U~Íóevû³@˜ÆVÏ<˜k}Òt+ai7Úæß ­'esÂó1[NèüèmíÁL:-›¤­²“"ë	°UT¶rÒvl1ö*~Ç‹İĞİİPÈŸöµÛ‡ñMh§ûì®e‚0r¶Ø€‡Ò8„ÓoŞø'WÁ”DùÚÛşlr*âˆS±ÚìUÉÂ§Óü¡;ï™Mõ–ÙİØNU¹‚ë¼©ğÔÇBxÖÆD> u±m`¤èG$Vâ…oÄ„bãY6§¤ŠÓÄà­oä3âèÁ;ã²ÙÍ€”×Ìşgd5üE[üÈ³Ÿ…ÊOÇÂ³–($Š”şDK~¢¥>N¦iEn—¹¸¤™‡¡w¬øì4İÕ¦=i*ì XÒÙe•ìr8©XÀÉİg?_¾4ñ‰ò ÿí§b{Y®WÎÙµİX,Ü›‹±š«áà%¬½Ô]ßl„ıhEHÔ‡¤([g—ïúÃÎEûè¬sb›Q‚“éY~×7_ĞÖcš`	!i.)¡o^aª¯‚Æß¼«@î´ªßøË.ıTlsöw‘KZ©A³
™¡8ñÄÛ”Å@+°¬Ä¸!„né‡ë²x´´Z®„³µ¢‹à£d9š4šßG)ê½ÖÆñ"m[DùÅ±BZô?¥Ó„_vı¤rô…šM6^5àfú–ŞP_½²õ)J
m»–ªæÖaŸt°ÈæV[+/l—ÚZ EõC2·28»ê·gCVjØĞïò½íäôPkÁbÒ=
Íc}©.¦ÃîÌxc¹šÄ ™ÎÇW)z¾@|H:=’uÊÎa R¹ƒßvÑpTé˜ıº—>dåü½\ZDé:Jï22§`ê»Û‰¿ı®<ö=§õÌZH·w‹ö²İıN› ”GĞ~G’	s¡w,3":ŒİWFG”ÇÚ”´bµ¶wzlzrlŸ¼ËëîZŠ-Òø5°ò
:gCMä“ö(›	£€ûà“ k1qlH‹] ¿_|o«^¬ƒË_‘–=qH« …®…ø¼˜Ü—i±|›±–6§°_±’=qX)–øË¤¥çù=C?™Íú¸ÇTM½§DIá6¦—½ªeF:É?Rë_ÑÔ9üm¡)ÅÜ+ğœÿVÒùÌ€•¨~å  ‰eË=:Áäw w;1Á·v)W¸÷*ƒA}©ûÍ^©í0TÊÄ˜Â£ÉtQƒ4:Ì›õ°&T”ø4 •b	w °O	ı€/Â+çĞxáW«°Xˆ¬¼\-Qà êË¯|Ú
÷å¶®Åóº|¨qìÙŒèT}>”FnıÊ†>ûåˆ-ğór¡ñÈV©“—dûæ†ìíÉòÇĞ&?‘ÙrÂºĞHoo	}ÑøŞHâ+’jÃgsÆl”]–åïQ¼©?
%ó¢HI‘°?ê”İf`îÌÃæ…î/ vÇÿâ=Y‹Ë¶mÕgf²Ù¦ıÊbçˆŒ_(oÃO’¬ä¾WõŸ1º¦a¤}`íW!NÀ±ûcÂìEvZ´á±Ëõú†Yãy°éïäßqÙhó&àø%ZÆZ[ÔüŸ%óã|:MeJ¡
£Ji	  6À‚VVüèÎ©s¬ì·3„¹û|q&ôíÊ ¹)}VÊ²õ‰ì7 ŞYTLöŸÚ‚ÑÚ=ªç¥=„=´ä¬lš¨÷šız(Ã°¸ABªÿ.…°uÍ¦ˆÔänk$•±)Q­Ş±´XE_(+ä¦Q·¢Ş4&(:
û¾‘\uli-áÕrU€€Gãô6YMYœ3—ZÚ—ô’7MÖüZÃ³(iz­±L%ñ…V©ÕiU)C–´“Ô¤Cê:NaÜ /0“Ó­øSŸz†Ù!JµL‰“4Å1âÎK—,Å[#hÇ½RĞE0F`*ÓŒàòõ¢òQ“B­X¶­H9¬fü8'YÖ·#à3ÎÏ"ã#·ÛóÔïüz“$Å·I¦äxN
ZË·Ö¶òñyO\‡#ˆÄbÛ®ÊnŞ.YrÂ8õV£HtòS"üˆŒ­ª7ód–^ìtG>“ËÒ?ü®”z7ì¡vßè™†«” di1~‰#á˜Û‰ºPZÁ 	Ğu6¼Ô4ó•ËÙ\	Æs^ÙÎš[[””,Á%9ÅıË¿üK)¸Íj(§Y:câœÃäçÆÑ¸+›àr@¾ÿÿ  ÿÿ #ÑË]xœì}kwã¶µèçs~í{W|¢aíÉ£=IfrdY«µ-U’g2'7K‹)›ŠTHjfÜ6÷·_ì )ÉNÒŞáj3‰Ç°±±±ŸÁû(œwÓ(È÷qZy_{—Ù"H¢~––y–$QîßEå´Ìãô®3ñüÃ§5¾ùwOz
|;,£•_:øë<’°¬Ë8K;é&I´:A¯‚2Ë¥Ò¯ã"¾“¸|€f^É&ê,ƒ¤ˆº^™o"gAq_wqA^=l×R¼ô:Q—³èc	ƒğ^x ±÷»ßyò{˜2k#^àÈû»Òoì>
Â(—›2•„§*‰€
°;¯ãèƒÿjt=Ğ`…çgc§¦ØºUFeèøõp:<½4õÏâ>Z¼HM\Eé¦ÓĞ ,Š-M`V%÷¬¼'ó¼ÈÒ¦>EAèşÅn†?­ûc[‚Ô×1ª¸Ï>PœíğİˆÚÂŞlÂ8ëIbƒ©Ç¸Áªµ³7de¶ı<
Ê¨RhÅwîşg/"Š,îƒ²·(ã÷¤…AZF9¶r€àd»“‚¿nÊ8)ü¸¬Ö¤'s% ™Iöá*£Y¼ŠòÎVï,6y¥eŸTò^°‰ÿÇ?<ø=ºık´(ıEN£4'AœÊÅşeÉÌvüHdÃNÛŠr{wŸÇ?	áxÚ=¹-–ş¢«´-p¿PÛBğKìªmaú§§¤¿ILıU­=@<î¢¼‘y§7	Š³à¶ğKø9ØûôPgEŒ·¼[÷·¹b·«YFŞt»¤º™ÇÎ£Ÿ6QQöR6ÀŞ¼À&ÁF”àÑfÀ_@«ğ³hæÚëó	Œğ7ÀÇ\Îóiïõà¸™ú—›W¯ÓÙpt=…ïå=ÙÔáUTŒaÅaÜQÎØ ¸˜DëäŞp.ş>ªÕ§åå­løËíÅQÑÏ6)0sßz'Æ5S/±-p]™ª:ÃW²Tˆ±j%ÏÙÌÁYPÕU»sD×äœÀ¨ ¶BFÏÖ8ÓÓAoÒ¿˜÷/z××ƒËùx4MM¼R½æÓ¢ÒEÄg¼·XÀl1¨hITl±;mflÈøH 8&hE§°`„=Ö¥{ã+øiVÆËXªi¼xz…îÎóh•½¦QB–.â+yßİ'äÿ¥^w³Éwš•e¶½ò$x0Çi*š|4ñ”´(Ã¾IF7·R!=&7ñŞ+¿^¨_ılydAÉp¾&Doœgw9ƒP®¥Ö¿Ò„áte”W¾ŸxÏ^zBXTk¾ÓYBïGj£'€(ôGR©Î‘.‹R¡€#îfë,ßşæ6^œF‹É#½÷½é`>º™Íÿr3¼55y¶ÉÚÎçÏuú­•-ƒ¼¬-ƒ*¹¢Hİ+`¶“[Ñ§­@m¼—"vè=ÒEŒÓ»I, rn{{7ë³ìCzº!¨™J´Ğ
,aËÈƒ §|gÏ‚»Âµ?Å)a0`¦ÂÀæ9ˆ:ñ¨Ü.Àê§|ËÈúÇ	AA¶«‹ZŸ«5üo yFâÍzs›Äï}‡^–Òî×AJ”¦X|#ó°ıòí†&0³8`€`t–¢g€–ƒu½'o&Q¶Ø|¼‰Ë{`šMëV/eR×‘£ª÷çèá6òĞÈKï#ş³‹§Ñ2Ë£Y¤œ}¬c.ôpä,$kçc§Àû˜Ùº^ædıA,EZŒ
Ÿ1½ğ¯›‚ÌDÿ-êŠ9èJeU£®·H‚¢xµ‰ÃVì{½ŸM:Jo†3\ÍA8BË]œç› 0sCj1.c—T„ ?âÀ1bƒ:Ï#»j„6!_€£u+÷Çø.+øs×ûòØÄÔß“ùİ+ s1ğ1OwZŸ|:­ÛŸÖr‘G:¬ğT¨om ]¦âäX¡§CAÅÏ‚†3šËİTİ¤šÒ…ÉzÒ?ºïÁa´SİUÛ–µ;q­®¡i•–Æ¿Îã÷ñ<ÄLÏ²/d6I>Q¨NÛ[³Ÿæ³¶V‹ì×µèR_¡eÜ­È|çæB+¹œ§wcØ±öÉÖÄ $W®Ûb'ó0)Y·±´IJî
ut½g':gz›e„K9fXÎ	^Šğ	ä"”\8ŒÿË&ÊH>
İß‰W¨ÒE%_fLÙºpµúšÔõAjªõQßQS ÃÑ9üß‡(Òp”ø_‡fÕ4UfYĞE§Š^…=Ãåu…õ+,”Ë‚©¢søß‡F@Êœ<g¸ÙI	³>Ã¡¶Û%§k‘mòEDvÇ{‚’¹ı.7yºˆ×@"¬‰\Ş¹êüyÚS™?¿ÓÙÍ£œÒÖ¦§µ¥éÔ–cËÓ›?¿Ş)ÎŸ:×ßì€Åh¾Á%qd	¿lSQî"¯[oÊYÆìpj›ÒÄk–;äp£ˆiMñŒ|ÿyQ“jWœ	—èµw—î:»ÉOq©]ı§‘£b·ÆIÃYËÙÍ8yo¼¦v:–âĞ*KúèË¬C…5mĞ¨¸S<¦V8SQ7¼ £–Me®ù1å™¥´Öu5ìäºˆ/ÈV2ªÚøÜrªæÏŒ7yÁxE[“¦ë1$‚[!•Æ¿6J›t~×t†ü+Şç”ŸV•
¾õ†)™³I*&Ùİ<»Ş*Êï¢3ü=%!^M4Ñd·šh2òN¤×Ğ›á5—°ï&È…kW45ŞÂ¶åz„ªY^-_*·+'	DçYN5·&u[
s$ÚA^”*İÄºBİ4XE£œŸäâ}æ¾ºáR®Äª ı5ªN$ã?ã:=yÇÉ+^/ ÕémWŞuÕ=1úiC&¹S¿»…åõy­Ì·BŸ4™ŒÒ>A®wÍ
SU1Ê!â4HÄ•JºcûI”Ş‘Ûë*÷-,T!´´à¾ãš0ë%”7êbçw¹×çŠ?ÛŸtFD¬/B+Cşè<0§¼‚–ÛîPæaµ™ÈíØw-àõÊÛ	¸„è(HÑ˜†»$èC¤Âëm'jÎúÂ~£ºbn{’ª(Ned)’B¢ldË-0iGa«æ²šlsfÀ³Û¹ƒÂƒşaW¨«#­Ài­¼R½_¼Z»úùßÿ<Öê #êÄÅt³^gy‰; Gø®İq<®‹O5Ğ>i![B>
Úíizi9ï-—-Ëáo(½YZfù"š’Ú§ÖT‰!ävÔH>ªûÔÌì1È`õ_®k™
Tæ‚JisğWß4U:Zù­¨dÑ+W´°¾¦’È©M½è¬ÍI}(d	ş‡Ou}ÛşlvÖ ,"Ù†Ë˜ğ¬xKbfÎ0)ïD^ò«
à=ˆ" r‚ù‚İnü¸I4¢wã<ZÅ›Œüß2ñÁH(ß’ä,J¢;x¿0¿ÿ¦Uå;´¤3}ª-±¥Û:[‹ãb[;u¬¸s ÚdÍ.'ã¾?»œ³Û‡·ÈÖú–µáe«âN†ºV‡|gàêŸd8I)Ÿğ/°=ò—úeI®óê3ûi(Cpµ(‹y°óXµ¼úI­»¢\jŸıTË€ıK8×J*/Õò!]§Œÿ­~>®ã<šË…¤WjYaŞ–U¿Ô|®YöS›Ÿ„ê×2ñÃğı¹Rà¹Şr*Õ¼Šz¹0æ›„UÑêZ:Û”¼ùSı¦¶aª€uŒ˜Dö[-õô}›•sà¡xIùZ:“¬ù*ÈßmÖ¼´üNo›Ü¸ªFÉm^³üC‡ÕÔ³ßõ>¥a°ŸÚXÉ}TÅşBÃÔ˜p3bRé/ßÉVÈNş6ì¤bq…›DÛDâ­ZhÈâ—¤¿°ƒ½;¼Ğ C«`şÒ ûÎûm^ñj«Vo%7a½‹ÂTEıdÂ”2S±¤Ìê³=6å=ªS3í•Zö.Ï6kBR* ª7zß”H8Ã^èå€	Æ/sR¤¨(Iı‹Z³,BÚò8°ToÔ’?mÈ}iNGO8²¼\läy´|Ö0d¹ßøK-‘fú^ªŞh'IJØÆrDGœ)Ò;-—	YcA…èOëÈÜ¢’Q ¡í=r˜¥´I«7u<&ÿÊÇšx£áO+1fúK[ã •R¼Ğö)Ü•¤bü·vrqñ²8¾ø‹~Hˆ¡µ¸•=€¿°h¥ze-{'I\jĞÚh‚‡Hì(ü¡¯ÅOó8­Ö~ÕKHçû©–ù—÷óAÜE–‹±(/4áJ=åwzéŸ†ÕªÁÚ.OäğKCI6W”¿&Ûr)ñ<Úkm3Y5 ¶ˆúZ£“Y¼ †¢‹<^ËIıKSÍÑ:rÔ†¯M-LÀÄÃŞ~njã…¾Ö6ğscp×u´Ÿ›Ú:¢¯öš^mHßÅğ¾:/_5j—Çw0²Ë ½ÛH‹¯¿×ö: •À¬Î2½¦é›µ6(êõÈ[Ã.Keá›æ³ÇuğTµÙˆ4Ş~P—f½óÅ«u\+âÛªT—¤F±óåÓ(YÚ-=•bğÛ”Kí&z·^NÈLï³2ëhãöˆ&É}/~ïƒ(ÿ”7ï¼[ø¯eD²Q…_ŞÇ…_ë„6JÙA€Æ!Z„‚xêŠĞn#Ğ:tu°’,ÏÉ}}e-B¶ãT.‚t–İİ%‘Ô^˜‹Ûìc+Q´ ¥8/	—&•-A(\W­Ôå2N;ÂeŞƒê7Ë{I’} ½Á> Pmê3»R~½ÀYò+ñ}oRDh«ùS>ğ;õèƒ$•s—ÂQ1È#Í¼Ú]EAAæ ¥*(a˜KJİ&NC•®	:Ãj‹!~ÙmËn·9º’m5)…]o²IÓ ˜Á£*/Gé Ï³šJFßE6-“ÒH5‹Êkp(2iLÀ¿>vÇÅ”\S¨¥8Èeõ¯°œôë/¹1~®Q./ÜÓ‹‹(Y£ˆ¤2mWÓm;6®«5õÙÖç*ÒÌÈO¾xåÃºfÜ„QK˜è‰iºåŒh§NP%¿©p¥îyĞ¢]s¨MÍÖŞØÌ$a‹~ÚDäôòJÊ£:‹‡ã"àfUé2›ƒÏV›g­> ŠuxÍp
¨
ï5Vk¥Zœ®J:i“§›8	A¦übº'cQ:†zWjL‹ /ãô]Á]Ã•ök¿¢22¡ËD™™„³ÌÇâ6Ô¥u¸KB‹:°Ñ@¼tÜõ®É?X${½<|ôT=ú†”øÖ»&ÿ|ö™ËÅ5nøâ‘¶ëıáu‹ÒŸ©CJ3Àï´®Æ£é‚™ÌÏ/{¯æ—ƒó™Û2Õ°·pqCµF¸8l°GÙ<M]Ã£S§•»#ìŒ,ò£€„rrD{‚h,šJsKbõoaõ¿!ÿ|K[&º—Y~d»#<½²sÛ0åF“vé|°ÓRqjrÅ2=fS(ı©Wdäú!FMèÛZ%»Ì qa8¨¶=rŞâ±Fï.áşz›GÁ;; æÊ­ülÈä%4G¡¿]ïÄmGdZƒ–¨E‹â£ëigŞ–›$<ËƒT6)<Üœ¡èK_óÇ?6ŒLSWNjú ’K~âÑ´Òïˆ¬niµJ«ZiÅËY/Ÿq—‚.ÙŸøà=ĞV V»Ì¸H÷Z”00K’…RHX¸×çİ‚¨¤¯éCVkìÊZi  „Öëœô"B¥½—ŞÉññ±k¸µ¾ö`ı©t·•X(Ze…ódÁÌQı,k0úaÒ´æû	<Êµ€#&A;I~·Ş#ÚÚy°(ñjÿÜì×›ÅGöïƒé’³ä­ 9rZ}Ë}@¾æ=`²ëMƒê¾¨®§õ›ÇSŞèÍ=
ƒ1ĞjÈÆtAßbúMıJ5«Ñ‹—>ò? ­wD/+«~ƒÊõ%Íâƒ^uĞş´Ö3ÅÅš`xŸ¾ê˜®FBHy$‘xÖ°n!ú³·ÊÅ½×|\DTçeô:“è2»ó£éjW¡´S¤ 1 øößóŒwİê½6œúÈĞŸf³ÑÕ‘‘= ƒÓ+rŒAÓ6½¸á­e¡H/‰ïÒÎeğmJ 1ô{—ÃW×óëÑäªwio@¸@?;±º ;ğôa–m÷.x6E4ÅårÃ<Àƒp>y®-
•°‰9ó€ä±IV&]CPV
Ú¥Ö0J…‘¥-`°Ñ<¾ÀC½
X[–;h‘õ>ªãŠÑğW³^×gE¡†lÑ;†€Ld‡-"4­í/r3"g.}te–a.ùÕ·›<YA2/.	ª®GÑÊé|êlp«¨£|¸êÍúóqo2¸u½/Ÿ“ú«<Àãm6{ÿ¿Î‡——ó‹Ñdø?£ëYï’0º_¡U=şãx¬‹^?„â6l"-øÑK²#ğûùšÃX!Ô#§oâ4Ì>tHUÒÕ×)HÕ·Pç‡“	¤o¯lœQ¸îY>ò>ƒ#¹EóÊÈãàÄúŒÔ=ù
ªB7Ç?B›Û ?TZ?
ë±“¾mŸœş,xÌƒ_=ÙÅÌÑ`zh.pĞPÄÙ5ĞàÆx-(€ÎÖ^ÍY>¬¹0ë…7‰B5Sy=šsqT}ø2û)_¤ğ=nş« $3î¬~… cªë˜¢cYå"ld¹¨'n‘Mpò)õ%Bñ³g¶½NÅ>™Ol‘ùĞS`y/M¢¹ê?$†öŞá¡W5ö-HıÜr¬à19d,(ˆ4ˆ{ìWí6î‚ğ4«ì"i.ÅTØÛ¡xvD‚E£åâ^f|Ú:Óğ89’ïnmz{h¡Ù4»¿a4±Ê/ê)É–œ‡íÁp;Ñu”É›ûÉ/{bR_ò NÉë˜¯"İ¯"dÑUıva¡ö…&ì±A»û,§H¡Bn…Ö’K×Whğ®3!­xBH©Pò1àİÈê-]x¸è¬wšã®Ò‚q 	.Øº´Ój ÿ\R{†İŸ˜›OÌ^÷ÿoæÆ²-”2ÿ"ĞCİŠIÂh]†ÿˆl	ºÛÉ8¶â^ZíZ¼Uwä\ĞA!—ÜÂ8‘f&§ÌXîÑ8/ÓR©ku—f9æw‡%ÁıüTİŸ	6vª)à¶7¶Ãé‘G ŒÿWˆì5ÍÛÌŞ_Àå`zäê-¸ü‘ÿaTcíd¿­ßj­şTÁ‡]²œi]m;*¡ò/¶ïtß)Ê
£‚¤.÷µv¹›ØñI’×ô?êüYaB7…] êXs•½·±émÆ¾RnÃ1®µ1 Ô RQ°K	Aóîp-UÆ=HÃ'ö~;Æ>3‹$+ø­¨Lˆ!˜Y5(Š–+s3:%ĞE„Ás´Ò–şî1Æà/êÕ´…Yº}ºĞĞZÉ ÅÈº¶iª’,½iÛ¬ˆ,ÂZlwZ€;±˜<¶è G®{eI3B˜@ÖÄóæjêZÕ?:†üÔÙå7Z„ãô­2É/½€0êëvt\dÑŞÆfÚÂ˜JúıBs¬Ñ~ÉŞ³ËBWj¹)á£4v­ª0¤jfôt ¤VY3äi'çdµ>/Iøg8ÕyqLjøÂ¸Pbç¾†WarCéuÁ6“a¹
âcÚ¥VŒÃÏ=iübpj3Â3‡Yjáï„æ´êWÓîoh·î¿átE“¨lG…QŞ†JD5Ô‹PkÏNğ÷ÉÒŒÜ7«qDãu5Ÿ’éæ †±ëÑqßˆÕ*å é[¡…0ğS‡Öh©eJ™b ~R˜°U_›[a¥Zâ¬»å¼ï@›AŠÊĞÑ¸QÈ?«Ô¸Ñ-“ÓõNØÒ˜ëÁõó>ZEa?K²¼ƒûï¢‡ù&³ùm°xd#-İº«Çé2Ã–Ú||şÙÍ¤‡v;ãÉèrtıŠ¯Ï¶ÒŠ~Î±“‚O¯€»åi²ÁP‚ 2_À6€2çG²¼E€s™•eÑ"p?æ1€‹9\êu>Ô>¸-Oó(· ½QÌÆ\èÑ¤m©\‘£ğã÷9æ„¥¿ÅŞ7ÚUŒªÎàEÇbçÇd ñ‚ÒKtÅv£³U##È³¿Ea0£´RŠ1İ(9$ş îİM©Œ(Û-² 9xdoYK <^_Îš@æ^ÿx5œHñ?êü…Õ!$3.îE{´g{òbÃ'ÌÔPExÖ’FŒoN/‡}3Zx´ G.ÊÂ/PÚ‘m|êh6ŒNqØš"Ä¶»gô© ¼¸øƒüoÁu\^V€4gq±Ø¡¹œN‚QôV=±Àt>‹¹ãé©²À,Û•³®iUhH#­š]ˆkÆèmĞÖ>¥ÂrÄ‚ĞK£“Õ–@³7ÌÛC„ª¯Óî‘„w¼ÑÁ³ã­_ãfÇAŞòvÏn7¼ú*˜ß´¾íé ´¸ñ)Uo}fø´Ë_ºH6aÄCçØx‚&Iàİ‹EcÅ:°Ì÷‚ÊY!b84¬ÛLè
ÂzuQ%=¨.`”6UNó‚P÷)SBI¨ÅÕk›<¼D	Œ9?–më÷w¥*Èá®„×”üå‹ÒµÎËÊ«¸(„Â…àO“W¬İ.Í5xì¬üæ5¨âX#§®³nPu+é Ùù-e[õ·våOÃåY‰‘D’jr ï2¬¦kŠü`ÑI´ÈÈİ5÷§˜+ÕŸ6‚Í¡J“îeİ`MŠpx”Şl0aHOlšìtj}d³êÛ¾4È†bR´§”ğl{45œ€Şw;^Ş×-Vp‡sunàHè‘µ±j®Õqñ,öŞ)½h¿İÀ,*øTECYFg¦\M÷NşĞ¨WÎ˜ğ¬€ÏpK¬|sÀåŞ ¦†Êùù½ùC#²Û}nägtâõÙ£>r„¾Îâ¥k‡‡öÑëƒ¨÷ÛæğòCİáLÅ^\tŞy:\ØrË= ?ò\Àß~	?Ú÷ß.:A»Rº¸2[vj<ÍbN1H `Nü<ø@SĞ.Ê9ê‡º¦£½ò¯j¿„µÈ1bÎ¼ï¶šì6ád(oñÇy¶Œ¡“¯÷ëÄì8VõÃ@Ö]·B	³ÿŸş=†Lîgz¯Ô¦9FØªV‡oÇ‘$U€Ò†„ÃÓ‚ëxŞÈvğ§û!lÎâ¢Wl¾‡%bf­ãl¨°=“"ær÷•’ mZ0ç×†C(×Û|2Y#ø&V@wå‹]n¿Õ¶[ù#qÒQv.³ônJNè"Bvï[zôĞå|	çrÑ­âôt%v³£|œÊÕ»Jt+mN”vCğÓ–ë“?€üVN¶8hÕ<6_Îj6]}Ã°ĞQÖ8mÆ:44Dñ©l±9#«İqƒ¯iä@+DN)C÷ÌÃ·%à“}@“ÔÚ×Ø*_@¿G=ÜÏßœÍ¯ÓiïÕ`Úå8Ãğ£’ì¼g[‘_üÇ-Á•¼%Ğ­ş˜Í:	*"Eˆ0Ë®³r$m/Mhwß-«TE!Ä³æ´Ël,æPúT4cµ)ë!aêâÒ¸xñ¸G,Â3#ĞÏö„t„ö”ØÎ˜Æ«—´µ‡“ jèì‘tà1GËÀ%Ş„qÆ½EùÇ¯#<òz‡ú‡CLÒP½aB;dÿ0(Î4ÊßH;üCïæl8šO“×Ã¾)MN¡å„—‹0——;÷§³É w5¿º™ûöºï³dƒä0”4ú?wh.7ÖLC\;¥“ ·…¡Ÿ.ƒª«ãçM/Foæ7Ã†¦^u´)QÒÌ<msı®HÒí#$×÷ú%Ùe¼€¿íRµŠ0#H9J§hÁ˜jGÖAş¸‘™?­)[=,µ‚'³gJ§oA*M{ßd¦\ß®„z"Wj„	’ñ‹cı½ïĞ”ÂÆˆÙ¸C”áœ39Ô¾@êˆ ‰.ågÍ×Zw‹ÿk³d50F	«”€Fbì Ä¯Éá‘™w'üÂhm´–mŞéÑQ‘»Á‚MªÀÄ$qÁíºÈzĞk.dôNàkoõ‰asL{h=ÉW›"^8£³7TÀ*êö#YZÀHè³ªf1?kÆÅyœ?¢Ğlëöïwõ~y(Aßr0wº7ä·D¿ÎüaØÄ}¬¡ £VvPîë‹éÊ²›i”ùæuvx5Mfóáõù¨¶<[OòôŞe[ÓI(ÂÊ.÷»-q¨‡@	.ß+¿ŞJNÎ»É=˜Pş¹¯yµ”; yé‚1pv»w¡éI–—æFîÎ2ğ	˜&ÜH^ÄÒd££ãÚÙë@Ì÷3ÖıÉ­ä.H[Ñ|MÅnF«QãÆlÛÅ
/îSšâÎW…¼Í‹&ƒñå[nfc•$ÇÚH¥Â#]Ñ0MK(¯Ğ´’Vd+ Avì"ğvÑœÔ_Ğ:ŠÖéRSÔæh]¡ì›‹,cmåÒg2WŒ‹áŠ¦`å-1Y!uI]3¾Jâ$º{8Z!4£ò>²ÑJ42(ñ½òë­3–ì#Åsôy€~UÍ9{;ÌÇ£ëÁ¼ß»¼´]6†Y,‡¨$ÒÕ³±2/ë,]’³–éğç¨ßmÓ‚'’—vÚuØ8óc!ı‚â~•ß‚˜ñ¥· qP¼R¡dæ¬*f¹ªCRd^ qèğ€Âxh‡ø5ºØ3—vº±ë 8iô´¥ØV[»8]o¨jæ˜’[$ç@=õ›§eñuÔ´Í®ZÅé&±ß¸àGÛ":p\bL]ö“™T4$é‹ß»î·×íz'înÉ°×÷Y	{ œ˜<úILá»m6H5A'V{ñ$^Å.íBÙMÓøoÑ%ÔpíêLC³Ò‰J#jéÏ$ê43¡"=reù©iYŠ®GH-*¶÷Ô~«ëç·P®ÑÄ0…JÎøÌóYl46ÒV´ZNJ­êíå63BşØ–…,.°E'‡à¥ya¿µµlÎ.hø„–­áœù
yqÆ×ÍÂ•"²Óû(Òmı’÷XªÎ×ÙpÌÜ¼şšÅÒñÑÆŠ¤«m4§-3#ßx'ìz9nÕ}u	°d×Èüîá«ÉèfÇ:¹E½î]ÏYƒ!Ê´úAAß™†?O·Jæ<¼’1x˜Ì	Zß^ûaƒ\£-h$ÔÇØåÂ<(Œ<ÙHt.?`ÈQúÖ²GXêÂÕùêXÕ¯M¬ä…Ğ4ÁÒ>( Ğ4R}µãåcô<êz@† ÷ƒŒ‰üEù9à{1ŞqRã®)¡`×e*`@’D,ŸÜš°Û‘•èhwy*¹ŠÒrQf¦Àœ•§L|}Z˜ÄËLDwSUœgKH v–-6è¾ÓæÂÕ6ó›PLGÿùºİmÃ”«¯mRN-®ZÂwã€ûnô/g7—ƒ3Séƒ¸ †ö:c÷;rì‹9èt•¼yx½:0_¯zCPÏ{{Ü,å%¡fÂ–¨„Y>°²^ÈşhîBOBÀkÓ
7OÙœÁ„¢[ æ:„bãõWÁ»H»fî½ÔºÀ«¤¢‚«A˜}H!Ñ*u©ÖR ı,;cE
²)›Y˜FÕ3tdèÂÓ“~§‹<^±†Bšù˜!’bB­ÿ­–KXÓÚï.¶Äl?şëÁd:]ûÓ³?†d1Æn°æïß’ï@ğóë / Ôá´?ÎæÓÙhÒ{50îiÃ¸XĞŒ(Y£. }&gq¼$‡¯¿ïü7“ál0|?L®{—¢ØÚã`ñÌ5WË“«á ¿šô®gfÏM,Ì†¡‚¤ ®-ˆ?üø÷öpıÜõ¾h-øj<¨e×~Gš&°v*T¢oŒ=wÃ*³œHJD¤Vfá{íg˜×›Á(«3ÍÅQ°ÀJ‰‚© á’¡Zx‚¯l„:D©SI‰Ìµ˜/fUG9ÄØWsU&˜¶Te_-°Ò˜¦¦zğÉ\©Zİò¼*A&Á²~T¡ÌÉŸe6°–’ğ/¼Ï]¥*
'ˆexs$fkE3^ô¦úé$6Óìà¼Ûöt>]Í‡g-ö¶ÕI†µ…Œ(Š§(V7¿~	f£P‚œèü·®æ…sÒÜ’O\a¾s›¸ûê.˜é›=uıü’_0~ÒE¶:kº2µº‹Z/Ì¨†sÂ®Fo&	Ü“ı›ë?_Ş\K–‰Š¢½#›¥º=}Ëö8Öä8Ëò(DO™V—h;ÄYï…Å^zòéfµ
r0‘t‡¯¾}@÷ëV·ªöjÊšš°àà„£u”b.w‘oàp˜Aï‡	9¶j
Óˆ–R“–t;#p<]}%fS”ÙŠÅ&ªé^»5¸ìè×Z¤J¨15éÀï¤
é¿÷Ìœ-PD;†tK :¢ê¼¼L£Êû&‰2£
~¬WKæZv/Ëi²¹káºëMâ î$:5µzšg@8„:ö<Ñ$”‡÷e¹.¾şıï!Í¶á’8}Gæk$…Ÿ.KZœw»‹Ş™´·Š7+÷Ã>£•\ùl_èam­i¼kÇ3]Eõ´Ğ¸ÌµÈ²<äì)”j”’;n‘J±¦Ö¨uLTÀˆNŞpø/QÎë`Qäü^§›m›{klîí®Íaf8©Iü½3pßŸHM]FËÚéĞz”rCìz·v˜K7´TC	‚I–ãUF²—Á½„ï;€X¦G¬Ú·ŞÌÖH6Ğ‚Å‚1@uª<4ÆAC2Ä<%«;ä1X~`½şhkTC9Æ¥½*wì²@Å;ûó…©¨+0`U·N*šû®º&=K`ˆëyõR,	Ÿy»CyÛ¹¯z7t(ú;é˜{2g9åIp«MyÆ¤sŠ¥˜³$rìo5ĞÁ=€µ¼ÈvègÚøÿùÂ;öÿëË¥ÙÔLÜ$›n
Áâİföó¸áâš° Õ>5ÙŒÃÃİ%š±Â¥úä«ã9ù¿Å«Ûæ$ÛÃ+0ÿålKR[YK¹÷wLcDşkYŞoV·â×Oˆ¯ù ¥gPHLŸuÈÉUÅ:ü€ÎŸ§"Vâ›ûôÁÃpÁôıçKËì0»™û¬ÌÀ^ÂCà 9U+ ï)E)Šu*ü[‡é¿L:6 ‡¨Úúı‡èvÅµ¬U«xÍ1U¹K«©,SY.Ì¢›åBú!äºv…¦´l·ŸşAX˜9°/¬qœ…ŞÍl4¾ì½Ÿ/gƒ‰Û:B]Õ–ÊZ¥’Ÿ1îôM–÷½4¼ˆâ»û²óåÉó®÷e-§ªüXt´M*~+Ü@gTM\pú1-ãÅ»¨>ã®xÊ¿Ü˜áù%Æffœf²#Açq	Î×›Uç¶Ã
¢wÅ$cr©™ú„ëÎÖp^¬qG™‡®<Œ®{Np‰$¹œ|A(èÇ‡]uı¤Bv cÜUğxÄ£¨Zgº62¬Gr·1˜Äæg›åRşÒî—NîVå¿¶G5RãÓd5&óWéƒªÖŸNúóáµËúÃy
“ŸmÃiTzºv™®õ¦¬Š4ay#¥øe¡1¾µƒÀi†§RÎğÅf.bknÎ!)CeÌKV‡®n7ÍŒ¬İì#šè…!¤‰¡ÊŞ-âú®I%R—ÖC×L•²ƒY·pê€8Ç
8EÍu‡™?È±%·ëîX Ü–ûAd:£„Ÿm&€Ãt¬ÏÅ§L6OÕøò‘h+ãC—³ìúH¾`zBWyà^yø6<ù/16V«*làp·ØrB)6â³€ƒïéû  òOWÏÀfuSxìé2^Dlõa CÌOÖ¡pïeD$ˆÈ¹¶äã¿Š‚b“KÂTŞ­ìl;à6£õèºUå÷R¶pÛ’„”{»jı38YáÎÓ/Ëu…C‹"€®İ®6TÀQ2rLubº5Ä±Üİ‰®š”cŸ+!9é¬S³oôf²ä¾i¤çd0àÃñè¸eB$‚>Ø:;£\E5æÌTÔ7½"ãg	+«ÉµCS2ÿFº4£P’ı…½T-°æX³êEËùS5,%Íq¶e“©ƒb f.sŒİVŸ"
„j¡ 4†{ÅÀûÕoİh›o…W[Vô5`¨öÃäòAÿÊu[ÑçäŞQWù;ïNº£ê‹6¼ÀuÀ€ƒÌ™#H?Ÿ}ö#„Ë°ÀäÆÌq>¼ìÓÎtp-Ò˜åeÊÀm÷¡ö^®ëéoá1]6Mîş@°á<O`d3&nÔŒ¤ÿp†K6Ğfü\ïO‡ôŒüôBòóšˆ~„:Ğ‚a u°| ”’7Ö­&Ğqs>Ğ#ìÏ	İ9¯Ì­à†š2…q\³{3ºî:9z=- ƒ`ìaôâ8R,^4-aGÙõÈeåv¹*>Ä@J6æÈ¤Ê´¢9Ô#M_€OzD4O¦Ç‘5WF¦€í ‘Ù˜G„Q´f‚õ3:!%}l(4’ÖnF(]9:&ÀxlŞ9æì>bl¸vóŞöæü1ÙÃì¬y Qƒéù¨Â'avîŞB[`àÏÛÁŠ$!f+ù*Ù n³uà.U&¢ëiXñ÷è9ãÑò­¶X[°Uî£=ÍFb^!ëÓ~)¶Û¥®Qµñr³´¸÷rp>sÔ@s-*W¤†hö¢%7ƒ ‰Ï@ğGƒ[F‹#p¯›‚8®¶‚§µ´4¶82[e»š®š‘¢úRûlqk»Üîó)x(6Ái‹mÌÉ€Òªê¨ÍL%6ĞÀP·<pšVlÃÁ<bÌ@aŞ+éßI”Ş¡¨ñ„9%ÿ‡÷.^¤¸õé:t½ÃÇa
›†Ôš÷#°‰á0ÃeòŠVşØb¬Õ8¡P˜ °±!c’nÓ`@Â•ö‘Ó‚¢‰F\kñß‡Güo^ûÈ½PÎ%i<3¶2¶·mb š¢˜Š\%ŸëzÏk½FYo#-²«O´ä-ù­ÑÕ–P£%†ä·AKØXŒã8óÖØÕßBC¬¨Wˆ¿ŒkİÁ®>óìH% BµÑlàfr	„œ0‚ùu½Ï0û^U›#A—‚…ştÜ»¾ï_ŞL‡¯Õ_6@Ä¬‹F[áÇ?ÕİJÉğ„¡ŸZç`0¶o>şQêşI[ñÒVÈA¬ELiŠä¦©Çâ‚ıYÓÀ=¥vät 2ÀuİÈ?‰Ö6¥4ÖjœöÛ<ÂEP@ _wÿı‹Şõõà’Ô]cm§'ù¤¥‘´4€ú¨$Yğ?“ZÑH5tµŞ4ºÑû~ºzÚğÆt{(AX?Ÿ” õgOµƒåÊ­?O¬vxd(öT;Ğp·RE<İĞT…En|-Bïq»Ø{-†ğIsb|¶ØDÍ"}é€ïÖOnm,,g:9ª[©\ÍIƒf½>ÇÖ¤÷HÎo1»`àoUE³÷zşÖT4m(Ú'ü<ŠF&BÕºY£ÃI¿m'•Õjôq¤!zª™®ş›†à¼0R¤V¯“áùÛ#«ˆ\Ô\CßòuQzkŠ ¾©ÂÚ#Ø(D°qDÒğÀıÈœÑ§~šnÒ0‰¼ ¿ã·JúÆ„6P¢Ü‚t¦sĞÍãğ°ËÅa§t:‡”¤¢¼$õÊlèüôÌ¥ğåW>w /ÓÕâ)Px5ôSCrWî)	anª%ç«ÏoÄ-„i—ÆmGôøœ¦Ã‡µµ£¾W¬SıßgCJâ¹–²)3ñî˜-SÌµß+÷Â\}±¬èºÓê13VF×»Pº§%	,t¢9L:¡ééˆsTĞ|×³ğÀs|ë=Ö´"r³ÆåP—¤ı!è;ÈªÛ¥Â1H¹œñÅMi;Ëhğ˜ş NUÒGáq¦åÓjûò†·ñ½r@e·Ã…U%µ T™eBÜáSgsÏ`¹ÿÈ±ˆÿcŒø¬®u-şsMÚ+‰yUÁoİßâ'`Ôı
òÀµ¬á!Ù‡, mšVn%aâq‚Æ,Æ¶¶"E>£ï<¸Øò2ä-OQC=»l±6”`t¨VÒ!7¦hÂØ¢ÕÉˆ]A.æ¥¾Û\‹Ï#%sğ3gİ:§Q
îjü9;Ï¢UÉ¤p†räµ„5¿ïj.oúãNZ-!¹¸e©"§.²YâÚdOÓ×Ò"ı‘ñ¡¡º;	ù.@ãòÿÓÓš ?v½/qøkcˆÓØ¨?ÃfZ¸¨o;2ƒÁI{¦€c¥]³lv•î—15%Kù®‚ªL7°—&t×óÓ¬t8œÿ3ÇL|n;@+sÜ\oño³jş/›xñBIê7ÊßDÉ"[	:E]´«K¸R_¤”}c¤„°.´sz“'¬{şÂÈr Y„í@¾vhÁn‹6}¼ÕÂ{5ÄßBÿBU½0â±68-ÆÂÚØ	G³ÅĞ§¨Åâ[6GOÖ?g¼£WÁêéGsºyxò>
ô6åıÓ÷SÉ°½¯
¡'<&<¨6Ÿ×²µXİBqšÍ©8	ÂŠˆr¬Ü«õƒÆjWM²Ø¼
-¨BÜ(ïıM0¶|¯CÆ²m:ÈwJ-øêø´8­m9Ú¦fÙ>åãöÑVG†!¯Å×·§M±Bµ'<ĞB&%pO·?½yõj0Å\Âgƒşåğz`êsö˜Í*ËYN¥4:İÜ‘C¹ŒÂ1¹.H+íVæN«Æ—xÒ[‘¥öëíb“GĞ€Œ•q'rÔ²T)²ñ.D!1]‘ÍÑ3÷X„^¿?sëÊ¡ò>¸9\ÓšÄ)ª[\œÅ9™ƒ^¸ŠSÕWa¤4oÇU–fJx	Wf-Íl´Ú %Ë?µÂÄª ÀAr–1~H*Éra;àQÂc°üØãÑtväL)9Ù0‹â,…&äl¸bÓz6+ÿìĞùSTFÁu) á‡ƒE4îŠjK¤@3 Y\uçCm9‹Ö%„İî6^ÆZ˜/
Y·ÚÔğXÖAúáÿIÿOzè*Ü[ÁÚS“[ú·Ğ^¾ùÙr,#ËŞñÉnáÒŞWY:1ÏL‚¶ç´;wuÖ³rÇ}öŞŸ®½ïvlÈ&ßù„N(d¶Ÿ­X¾›1ƒf¿sÔlCÓĞt-ZuåXz‡“6n“ÜÂµt€=×AšuØ„ş:ò¼ß{'ÇÇÇŞŠUéê‹âéß‚)Åâ>
7I4§I]äx;Í<C»§©è¥e¼ŠúçÍt¶œ^A8…x$„.@Ò®²ªi±èğåµ[?ğçèÑrœş&W ÎÅGY€G˜HûÆj3Åÿ
»âŞ ¤X.£œÆ©¤ŸA2‚&­ˆ×§M¨<-üÑvß¯³ŞOºç[qufÿGCÁ§Zì‹ü¹{q¶Lh`eªzä~c”å¬àM—şÅèf2µ/“›y¦,*O„õÃŞ-ûS¨›Ù—ƒsÚãßAXµ$“îÒ3š§k&×$öÒÁ5[hÒ«0ëÿøÇ¶¡Ğá‘ªùÇªI¡å@ÑÆÉC<™?ša{_IŸ`­-ÑZ0ÈÉ4^­u—q¾ÂÏA‰s|5ä7œQ1ã uaíÜ~k¸ÅmGÀzêÍ—óØĞ©²¸wÖæI¥íÃnccü¨7xĞtèÚëàöhVİªF3bé¤‰1ÏOEsjb§ã®×I³2^>ì`C9Cşk­I»äîga‹©’§Œ6Üf>øc»’ƒ!ÏŞ××îlçOa…)»µnÕ›£A'Ê“İºİ8Ûü K 1ş$¢IIÇdncLu5œbRÉùYo6˜‡ı?&sÌ(Îd”ÌìÜŒÅâ˜x,~ğÉ°ã—ÃŒ¬8næÒv7Éhsn5¯d{©v[‹É¦³á~@æ"^˜’‰òW>ÿã¦a@7+5OHôUìoæáádr‘tâçÁ?¹ª1œ‘ß„ûd¾ŠE0?Â Ñí€ 7•MNÚ¿:ƒ5MËŠ°_©åÇâgYzš/†rE!Í°Gåè]³$¶Aƒ¶ƒ1-WRv½U€°…çz™ÁÙĞ¨•q§¢©ë‚¢œal
Y™+Ş¾­,Wå”À¢[B/K yv1ôŒØKˆ®PÌ‚[pf€xìnA£4w^¼¨Lm÷
‚Êã'ûÀR6“éÜİÙaÁcè6­e°0ŞË)6Ä'ÖÀ.zm4±áA€yªæKğŞÊ	Òä«Ùú†Ğ%Ì^fáTq©ƒái¯Kj)xÌ1¥aÅ'®8›®†7WóÙ¤w=íO†cxÛEPÜ®yÒR°µlH¿i^7Ÿp>èÍn&ƒùëÑ°Ox4Ÿ¾Ÿ1äs:¾ÙÀ3à.òø–|ÃCŞ•ùƒeÜòô®Éí9ËWÁºŒç‹Ğ6VıI`ÂD…ÿçÁÛÓQor6ŸõÆ]ÏV
s›_]È\¼ºö.çÓÁl6¼~e4Á_`:Îà#6 $¾K!yÁ#+iä¸{bŒjÆS¥„ “ó×"‰b)Ûì—bâ{›0Îf9a>úY–ƒ:½¶tS”´{¡ùUŒ¹‹ jœ9a'ÿ!G.œxìR—b¿‡]OG=‘«ß1P7©6\œû£#Ëù©àŞd“P±Ux£U­mÑ ë}uädmì{VcñÏ¾iŸƒ1)ÿñùorï±ôÆå«áƒõ@²Ùbp¡³İÔà	6Õ ·›m¦÷M½K®7«[rS03ûJœ¾ÆaUHc—×Ù†>%ÃJ`êã7Ü~ŒÃ?İ<´tğÿÍpüÙ›«àÏÏ6:îê¥ù\û…°Şá¡£õVÖlad\Ü7A\r²ÄüÀú´Òæk&W_™_ÿ\ókÜç™¿7Ñ-œm¯ã[*/,—h7Ï“Éhâ¿Ç/5ÕÊ÷89šã'ïVŞ!­ 3t	"ü¹?H’qû7uâßŒËJmàÎ,ïËòT]i,OÕäÇòT]T^,OÕƒêÃòd½<Xö4ÁxëĞz+QpIYÅ#¤Úªä?sKTîv¹ &€0²!ëï€_¾ŒÓw*ô©¹Œp‘%‡eóP±…	°_Ë°©í®c—£ëW ³šNÛ]Æ^ov¹ŠÕob;”¸Ú“ˆæĞt†à…ğ~ŒÛ×'w#áA?ò?jº×å× ¡=W½H­“v¶¢š=Ï²³¬7À¨HY˜A¨<¯Šw&F áˆ’Õ=Í5¦0JõTï…§ \òÈ%g\ÓwdĞRs\cy“¡KĞYed€dâÁ8îë:4–".€lUšaÚÆ=kœÇ« pzi¼ª® _½7Ç´õ¦¬âø]ÕW´wı²£æİë°nMÁ»Vx€ÜG®Öäqêz|t{ól
\^Lx†úœ(+¡LBø©å1D¢÷‚í¨æ4`¯–YMÉï5³£ñCk ìÄÌÎ–P]$42J­^ ÇşÚî¢ü%&‚aµÅç—¦ë[õY–<¿ÄÀ¶-›`¶eu02õ³m+I"6™MeùºtH4Ş.ù‡µì˜ÎÍšï{èQc›†x+_ÍÒ8O9Î`QGïuû—p †ºæĞ1õ¢*ÖêeMš
 E.`¢¨±~ç9á·Y¨~¬ÆÙz³á“x\u©¬é43íd{ğÚ‰[¬!ñà=hAù^ÿµ)18 ‚Jªôñ:d’×¹wl€N{ı?Ã1#ŠSS1ª8j´…[$YA±¹u0iŠ°÷µAÒ±™t¡ÛÌd•ˆHUøÌëZßqXŞ³Óu´ q½î1‘»ôÚ:¿P¯€ÜfVqÚ‘ê ğ4ş[Ô©·Ö­KìÂu™¶(„kn&ù=ÎWSï?=c!ø8¿_]Ìægc›å7 Ã}ãBÂpºmĞƒâEµdõå’'ì‚¤ß{õso6¿ïÑvÅãÀP’?&ì­á?,†e ÉbêµFCëŸ¶ÓÒ÷A®µäÛâĞ¯>0™¶= úyô!¶Â"ANÈ¾'À©‘µı#ÁHöAÚ70É˜©zw8,¿$ÿ|UıóáSr\øı7–îÙ I¼Æ)pl	uš€óÄ&ÈÙ4¹“”ä^×œêgµ€•ºÃ!Gÿ"=Xúƒ‰$¨rI¿úáÍ¤7÷G×³Áõ¬ëY?éØH8NƒAao¦—ìèYÃåÚ²sDÓc«I©éÉ>oËĞ&FQgêøñÇt?!ËÁÃXm¥pu‘”Š.äZuæ`™åúÓ¾¥<¹_U${
ÈôÑ7ŞgŸÅötAæî‘ûÖÛOØb
´`V^œÍµívy\3Æ["à»b@#UÓ©QmI(¯d™+0»S@é¶Á­ à³…ó[“/ŸyÆ.f·mÎ?Ê1  Æt ÍÊ¼„=¥R33W™šÖ¬ê©8ï !Á3H£Á(Ä$)Kåg,š:´‹EÂ~–d9+ş.z˜'äèä¥É–û
v}Ğãf=\‘	A³XüÅ¼…ÔïMÃÕa4ô˜ÂxSÀAqòS ‹…?å”Gbè¦H™J	è\¸:dÍ›€<ÏriáI…®Ö˜{Ñ8mjäZ&ñŸÑõàÿr„xïâgŸPôÁdşz0™û½Ë®wò9ÒÒc÷
BÂ1\°\{fÔ
/İìCÓš‰z0ğ/Å(+‚ÑÍO‚­šD^¤Åáë ÙD~t5¾|?¿¹¾n8&ƒıª©AB~¤¶UµY¾Id‚{¥?¸>kjª´/É1cÓ’bæ)B(j²CXeëh;jvCø…12:r[€›îx¹F@ú†İüÑâW¢#Z‰´g>êÀÀŸøºU(XÃ†m:6Ä.<Oµ}Î‡——ó‹Ñdø?äKïRÚI³A¨/ÿ‰ßIÛ§ØÜîºƒäª[o"ôÃó¶­¶ØGŸ·lÊê[ÛË!°¯ÍêCâa¢ÌÃä™º“s³u ù”Ë÷¿úji´ÿ3 ë¯‰;ÏÿàBõÜQ™b½·±Pw Ø^ÊL·ñƒÕëÀå$gÛ‰LtSy->=á	Ş§¥´Ä¡‹5¬fÇ¸‚@ƒ\]Â«Ş¬1÷&¸N_Öv„A­Úíôw¿óÔ˜¨µPV}š³>ï¸N
kÍ–fº3½
ÖHUî‚µëjÅŠí(‡‡t€;: K¬8ô—q9GIÇ¼îÛ®ò†U#Mí¸\´Ag»›éåêØ*¦ä‚ÓÍ-êÿ@ñJh}J¥2æ+)£H6í3o—ÓFB‡‹,meÓØgujÉxëîì*NãÕfE¥&'_™"!ËÅktPì7Jj4íËàUÌ£ë,ç—0\Ì<ú‰¸Í¥í™~ÂË±æWÉRØë,DvW[D‡õ¦4{“4k-·Ó´(±³"†Wék-IAJ…aÀ—£c-ÈÜ0âi‡ôÒõÈ‹5)Bğ:‚˜à¨9Ù3`< o¶mt±ht­„×ÖôNÖh¾ƒÊ”Bà6É´›Œè#ªM
¡x³Œl¥5Mcm Â‡ÍÉÃ¿YËÈÿÍ….m½Ø·1pçDD˜¸·³J…Çíâm°]i"ÿœp—ÙušÒ)ºbÁßîÃXpbØh¡REÂ˜“‘@…p%ál}0‘÷Òìûô‰3øe9ƒv¬AÁÿt3Òl6pU“:_`rm Øs&*Ô™ÚXc/-µÂo•%0óV‰º¾ù¬òtÑ”&WÇ•éòëMÚDêò¬@ÕvÒuşè½äÑ*{9;ÂÎ=³°Ì-'sõ„hì±Ôb˜íÕü©wÒvJ‘àë•ãS'š'Ì}/“à®ğ~÷âÿz'»zÏc-±#F7cö_¦Ú@¥0Ã+åk)}†\?ÒQ¨™Qã^÷eªIßY–g³yz $ÑdJ{œüÕs@-íİ
=Ê˜Ô'ä™WMæ…BÙPÜ„´ñŸÏ!6=+`3HFdõKı–)‘|Â|ŒÂy t¾8òW›Òà]£õŠ6n ,$X	 À` ¸Óül³\Ê_¬²Å€Ÿ|gÑ2Ø$%9üÀ©R.Î]4éƒ›Œus9/ßêÇµ§´SŸäÖÚ]UÕ¥õaòA‘1Îğû8	¹uÑ·£—5¯ªçAÉÙ}”¬Sñ`wÊ°P©‰¶ÕÔF6j%·G*şb	~±]cİ$ZÊæ°Õ¬ğé §ëZÚ‚´üáG/É«á÷s-šŸ+’-ĞÙkÈàtH5­9‘C«JIlÑ¢×X;Û±®¾S@k·uØÅ[ËPVÅfVÈmE\qÂKœã|±Eä€Çb¶d˜c«#ìë3…óµ›Djí¹åªµ(èz0)Ò¦ bBÆ×„¨Å‹ I\Q„LóvAVì,^	ãO×ü…ñê4º'Õğp%WoŒÖ†0S0¶n1¯‹¤³ø…lM¸’ÇQ6çÀ„§M;˜{8I(5íoò‚ÕiLrf#èì Ù•ƒM]Cx¿˜šÂÔ¡©&›v‘“Y„±zşÜn'TUmÊ‚¬ †LÂ#¹e· öY“ÓŠ§bkYM€9-Hgpï#ÿú‚ÀáíiÑÜy¶ØÍ`ëçŠÙ2âÒ»ÌMV˜#ŸıµºÜ¶Ñğé1šm1(d½ŠÊû,D§]£Ddx=¾!Mf£³ùõh6¿ÎºÒßÔü4[². qú“4Ù:ÏŸÎgsÚÏto®§ãAx>lî‡‹h ºÅ*SÄSìŸ-¸~¢{eV	µ• jË—l(kŒ ³7&5ìXƒFO¸0¡½wÂ]Ò Ã»?+eLÙ”´V^‚ö—lgs±j¼Ÿ½Ğúw+ÿRf»ø=È’c0aï #õt÷…q¢(µ´>v˜á©°¼Æ“›K³¥X³~ÿ¶šdV­ËzÖá1Š¤c·–D–¬Á‚ã¸µ¢…íƒG	dcaMt   ÿÿ )@Ö¿Jš+²¼ĞÇ?ê‹a\‹·õA2ÔüV^j£$²ÂÀ±¾¼Óã-Y€Ï?ûÖ&-.†)b]v÷íç+m›G^$fÒq»~ÛfÎû½÷|‰¶û™áº1ËÖì®Av©êÄã;òmÇ+˜|î:Z.	-#yÆáÂıpb´Z‡âCµ7[üŒQ2»MÔ¶àïr{k¶	·A¹„º8=İÜŠ g†÷wì}]WîePn
rNš‰¦üKeÄÄK‰Ôp Fj0Úø¶•
Gı|¾Ï>ôÄŞRH±ÕTtª–€¶SÂ%¿}Ëß¾ÕO[…Âe¶¦7²´¾ÿ˜®%µé1^µD©YP€b”‰š¢Œfšµ&ê•‰úEÚ‰‚;/øõÆQèñ'×9ä!úÓx ºš©•›ovjùÉ¯f»^ÉLS­ñ8F'Ã]ıì¤QgL§Ÿ=ë¥Å‡(÷üç“¯½Ê'_ûß¤¯=`î®¾öP·Á×›7ûÚoãOßÚ—ú3ùÑËïw°hçCO·¾Ÿqçm¥‰İ½è÷ô ßß{ş<ç=¯ùí=æ[zË·õ”×õtŠ‡üãœ\J<œ×YÉÂ1–3«ZjıôzÉÂÔ#›šš`‚Xøù‘şS‹ˆã ¥;ä“~”LÒ1FùX¢sÔN¿@*@İ_€m'ó4Q‹pÀÆå‡¸ˆhíû €ò­‡®ÔÁëÑl0Ÿ¦³É°?P±Ûôæb€Ÿ’ŸD‰°å­Ë?ÿâ¢¹ÎéÛyts=›¼Å²Æsï¸ÁÑü>HÃùó®k6gÄäËf´«¯Æ‘İN¯el0œŞKÖ	õ(îºí8±ıÄS­k’ Ò&ô¼Zi1ÿiV–ó¯şìíx0./ç€	ä’ŠlPjİFƒ)½7Ê`¯ïƒÎ±¼lYAºíÒ8×üªkÑcÉÚaI0ŠïFKTHã}åÌğ©¨QvÕkò…÷ÌhjÈŸíµˆ2Œ•/u5]6šÏ¼“'s²1]ÚOşñŸf‘
Ÿ¦Ú–@®_ìœoíQ)®C>DüJÌó±ƒ6F5¾U-Ì”ø¤»_.í2'a«<&ÿ5Ê&DóÂJ+ c¹&ÿ|ö™ìCaÄCklµ‡•¿$f%¸ÁR‹<xÏŒa=¾ ©,La† ³lmïŒc!mXû€†FÍÒçşç¶-7õ¶jÊêW0œ{S€eb^6§nk^rş|d‹cv}Š¾uµ¸ïóg#3eèM¡zÑÙ*ËÊ{*;}€Ä!T¤Ê[k±tÈÏQ´³ŞüÕß¨ÄŸó,—ö“‘Il"„7t¥İ©ÏÍÖ¼6€©¨8ÁšZ,çpbnØÖP&»jŠÂ^“¾x»°“’€hç ŒUÈÉÕè&A‘…c&DœLÿ‡ !®=dF…€”ï-bBXz"¯ÎIPÖh%ßŠ‹4b‹µ\E±ÈÂhd‹wsŒmeˆÙ$§°ÄS—<mÉŒİÆa¥lêºµ	As5ãF—MßÑ‘%M€3`¸C@W¹¨Ø¦~¤vÇ§…Şv¡3+Ì`®Ú;Ü¾,Ö¾yMŸfª!oò|Mû…K[m–Ù7¼¬Y¼3ØàCC•æL0y,WêS%$˜ÎFÜÅf¤õ$k[5îC8¯æpÁ’ß4;Üzl0Ø†•ÿT×£—HwhÅv4–Ù$˜ êÀzuÄ¦‡=l¬G§#0cõ×–r¨>ˆ€ãŒÔ7Âc	‘T“œ%PùFV6Í6×1
²4Ûÿ½˜‰Z¶Ñº–9Qõ2 ™QÏJ(CyµIÊ˜J2k¶<x5ï…ÁºŒòqVÄ"´õ¶Y 9ÇCærLÃ‘S3µºe1ÃCa‡/k‡Ä[BIi á‡Ì‚ÀaôÎò’ÿL‚ûÂŒÉ6ˆ±geİä‰/  ÌÑ¸ÒÃpòrLslD‡™7Î…>°¢¨à '¸ş{b¡‚Ï®eTª%*.9NÁ»…ÿš8fü äÖ€~)ê»¾¡¿BKµ«S*ÉsmÒ^8:"$œ´Ú¦Ô2lyÀ%1¡ ö©C»Ñ-/À%7Ia<za¾/â»ûÌ\0.|aÄá˜{&QH“ ½ÛàU‹µ%°É$Jï¨e‚…ştÜ»¾ï_ŞL‡¯Õ_ºòªÁƒZ×ªlëş-8vA'ÿbÂÆÔ%Ğ[cóU–G,ÄÁ$Z`¤‚0¨[M§“EoÇ%¦!D{Ë…g}cŒíF
ùëM	GLç†§™Ç5Müí¨5LËÎ!sà•I­)!6 ş'µBÏz§óÉ ?ºº\ŸÎæı‹Şõõàrj²4äšÅ¦„h:æ1ä„×°ê—Ì9©Š¶­„áiòåiŒµY†Ú˜!¬êâ]Ã›¶$'N‚œ¡ÇGÚ»œcaòJ—¤’ÕlÁºo‘ø° à‰ #‡vN¼öÚ¬ÿÙ‡ß4íC«`†q˜}¾Å‰É6jÍ>hBù
vìHü‹üŞ›[‰C0ïõæŒ4m©ê¤ ÎÖQñTÚ¬!¥%0´yT÷î“nÁæ^mAÕ^!–Ú+­çîu—yDI._|\¬­&K¶‘±„&MÙ¶=Ì°¦²mÙ!ô°dşât½µÆ#Ş²Oš4hz1zs=?ŸŒ®æ§£Ùlte>&øğTg×†°€Í®»ºµ…2ùr@ÌÜ$‚=j¸,©é]"fªêÏë2nî¶E¨­Æö±2LTĞ¥Q¨cô‘0™¡~Ü´wÅkÀ+®Ö•Ù9şšÅO…RÓ» ‹Hø§ª•:Š‘ßf&G­ï¢ÕØCÁŒ‹û–Œ!U™»·%Ö<\s¢˜)S•‰AïKE]ˆ	òcÉI³µ2ä/Í
å
™ë%-½_v› ôQFhâÒá1k%m¬!Y{8>Éåâ5ã¤Æ!xåş§•RÂk•ŞÂÚ&nros#0Ç uç¨×ëÚ²më{ám¶} ˜à.;‘fĞÄ¾«×'»m¡‘öŠ‰ĞÈ3¢XÆ3
#Ç[’Iù)Ïj Vó™ˆ>àö´zjk\¯`õŞÄùd‹Rû]yFXÈZ\î”ÜkŒjW§fº|ÔG¿×lIP¸P§V‹@oÁ¾ï#ê2okĞví"/î¶’ƒ“â!$?}­\“±ÙŸFÃëêún"‡Ø ê,ò@t›»Áx6VäÃAaAÂj6ŒM”ÏiDGÆÓÚv•^û4 Î$cø,·O€tÌû™p‡£Á*ûkl3Ç•Ë€xÍ+È,WP[.mI¿V °eâä:
mÉ×SøÂ²p¹1°T[ÿ9E¶ØÀ~E=4v ÄK—«/%Úà-T¿ÑV&J¤, ­…¼•š–·‚ìˆ|¥áúÑ»lë–‹Ó’Ğ×(ŸF`î¨ş6¸~hUD×†\km;à*‡Œb^T­,B¥C]gAè\éõ·Sñ>7zg|DQ1V¡ÙüıïõQIıW¸)^ú…<Ğ¿kõ«!²íöÃÕÓ%èÚ§ÿÃ¶Ú=¸ãR55$ŠºƒË~2Ö¾* İš¥/~ïr0™Í‘NÇCB@'µÃ£ö€üÆY”d£>?>nU(©^µcœYs¥«#˜ó˜61c?ÿÈH³±½'Ä%,,9ò’u*¤©@®²Ó›ËëNÚ{ÉviTš`NŒ3`ğZÀÜé#²ÖBŠ5ï
D¥+¬_í›É$Âx›T¬½»`V °SŠDÉâ
 `ÉÉ:’J¸à!*XØ9427$Yw`›Æ‚?×Ş¹>İ;iÇÅÚf‰êİÿÂËc\ÆpSFÁrì°‚[­ÛÏÚo~ónù/¶ÔÎ¡§ÀA‰‹\·ÖÂ÷†×Óùàjô§á|x=¿L§½W'ux‚®¢PaÙÙ¿ú®~”İŒÆ\ Äë˜æ¥„²§^D±=ÒñÑZ°*²å­Èÿ
I.Y2Ñ% †t[œÑOÂ¸IçrMFOV«2™ ûr­öq	i?
£;Tß‡¯eû7>18~>´Çr:LNRiZA¶Kû4¹ëšÀ¨¦‘¥>h/o~„#Ù›$¡É¶Ê¶…’­¥©…™Gié¶Ş=”Pl¬+3LGy|.#7–-´£uY5ƒ[©É|PHÙ™§ƒŞ¤.SQ/îgL‚BßÏ¯ŞòóiºÇ”>–¨¯çöÀXÕÖsìËçÄ}˜&q)NLúğ\.ääH -bµûºeI‰êgıCO#pÖÒ7Ñ-ÊG4¶Ëí©Ğ6ıÒZñ+Œ Âz}‹wÃ±¨…É¦Yvi}VÌY0«Û(”,”éÁe !Êõ}Vbˆ•(çÒ.‡SPE@»'Ğ52n"Åì>ÏhÈec.
{î‰=ì*È"ÁèÕåâÂÊx¢ÛuméÄ–FwRh{Ã;ù×M“ ˜;ùË_yA£†³KíÓöÃ@€(œBº·ÌQgÀ°åÁú^ØR2Ãtc•¥YZïI½Ê3µŒë04V`6Ìnÿ'kmÆê(b3‘$uøÏ×Ô …MÒ˜5nŒ&Ü.qœÆ¥5±
|‡•ÃûÅãÈÒ =¼ñdôjBˆ?¹¤Lg½ëY›6 ÿ
Êw±ƒÖ8}$qhzNWmÇ	$ô¾Ãw#3E³æ@Lo*OCâ$CØPò”cuj² ì»Şó/MÊf:j÷›ŸëÛ]ìÔƒj«Bø“©pä€¹D˜ˆ·¢7Ù,Çs Î,²Ã­7:Üùô •ÿ‹`tÍÛ«[ÑL—û¾İ¸‘,£ ibJ 
“µY@QäqR…Ç;LĞˆ»Q˜OÖT3½µÍLïÀàe`‹q`mI.Õàw€iÉ¸ñ¿k£àuR‘¢””4Ãb Ä"€•#nrªvùùÒu²}a!t&Ù‡)óÁE î‰<2¨”r üI¨[ÉVV	íâgÔ@)!0K
‡f	‹ÊDÁ89ß¦ =½h.ÀT0ÚÅk+0Çñj(ÎÇ>Ë¦ áé°ä[Ö'@·°¨‹[¨wckD±„œäzöçX<ÑXúÖƒbo;ìh~›­nOV s(ü:+Ï©ñŸ5ÚŒ3Dª4Ø)©'Æ\Ë™h™>ó>Íø<ql“oSgó2óÑ6¶©Ã•Äm‘õ±Ö ã…>7ŸÒ™“\?ÖH¼)¡%RG´'$2¸ŸÍ­ÈFyŸ"Ô£7.ÂÄÁÉ§{RÃö•áW4À@Ùám(µì®óÓ&C3k§–¨b2%±Ølè´9Ç¤œæÖnÊè‡Yl4WA÷W,Dš‘K.Ş
YV(ïùñ<r
«·iR‡Í!>Ü\ø*-Ô“½Õ6$tiN—§Ix)/â–Mÿ6Çhn-ß%­šÃ¦¶Ë‡·ÂÃ“ãç_¸ñP€(¶i{@3¬²ÛåjJƒx ü³[Ÿ;¦‚´ó4‚h1ÎÍ[,dŸ#\\s›yãÌáÔ‚ĞBÎO£›îÔº)*|³r‘òCfïóYÊéÛ*ÛóíÀ#QÇ©òl5kàÎã_õ¾Ÿ¿î]Ş¶½¦´¢i:Àà²Ø¶p{|øOKÔæ=lƒŞJ{!x5„32hhîH…¼ëâû«ÿÄ9h» ? ©h|¬ş¦î{fça„À¬¾CÅ‹ÏBãªšÃ†4©ºÍ@rÒ!è	-¯~PT›c”*!ï·àNå¢iøñL?ìéÆë—ñV§ß\ÓøòĞP©<ãvüá¾¥ İA’Äkƒˆ“w`)5‰ŞGAb,C§`OÖÑl¡÷8©FSWMÇµÊfXÆ{õèÒ.—Œ¦ÄuÃ¸Ìw¥ç¿&Ÿ,C¨oDòUtÀ"­ÛĞu2	d5„lä/Ğó8ß“	ÇÀÕÔ¹¢ÁÊVd t/î	k« {u´çAô+¬Jş~ıÕ’ÁxÒÕ’;Úyµ¬_ö€çYî”A’q´q(ÛCNŞ~kç'9[X¯ø…5X?©ëÆ1½×ƒ3ƒ((—T<ª°ši*‹Ó“à¶ğÂ!ÁÙµ!0#—Á&]Ü‹ÕäqÛ›åÖwùqjß8`›îİ®(6c,óàKöX€<]TµáIdÑ
ò¸mÔøòc×H+Ãn«Ş·Ï•ªêßBœİ¢I]Ù?Œ/â×[k¥ö—Ÿ& òc0Ÿíb3ùim2 ?¿”ù€üØ·ûëÏ]3~ue™a—³»•h·½ICux×.ú™iqÿbpvs98s]êø4r¤ŠBŞ
(õ×w9ê;{N%ænAMïyÄ²*ÿînÑˆóB€Šv¦PØ¦3H%‡İ°Ê<ÁÕáÿ  ÿÿì}[wÛH’æûş
–v¨±Š¶«{÷t»ÛöPmsK·¥hWÏTÕò@$$aLl ”K;]ÿ}3"3¼g‚¤D¸×8uÊ"—È[dddÄ;::TmbC‹ğ»$/Jï©Ì˜uG{kù•º]XHö"@´:Ï0cŸù\4@Nø}ş
~æxH/àğG³9:^S?N˜!„»LN²j›ëo±0"jµ£}¤©×0”ùmÌ_uŞv^v^ÊÍûâHì#½~Ôxv¼Iïfƒ66à{û“èFNÄédAşpïn¾“qıY—Z“Šgç÷4.++ñ¡š’+,†Öàäd×Lª¡;#cÇb›YÓĞÒ*`× ÔˆòoõŒ€Ç³Ó¹|Ôù“ÕÊÏwh¤%@± ş›ùW÷Àe¤S	ó5@Z°øØ—\KV˜5ëZJÎ¾¶¼Æ®üÜ½ÈÿK£NR`hokùŒuÈÀÁ˜[õ ïÆGî£ŠãœÎtÀ(˜.s3–Q>*„³CÑ;ÿ8+)Ó°¡	õƒp|1™œL.Îƒ‡Bì”G9(_‘
Ï³’ºëXxZ \gˆV!7#èÑ HÕ­m·<á3ˆR†8W2pV“/]“Õ'€÷1ôº¥³¹‹JæàK£gÊ5ú1e'Ûwu`O{*-:øk9:8¢QcnhÎ"zĞ‹e|%mîÍ"¡y™ŞU¸Ğ¶ÈOÆÒ{(!Å‹9‚aüÛAçY]Ë³ÎAçÀpU—GXî=µC,Ùã,+JvÈjá˜Û¢ÄA;°ÒwŞÖ3&% 5o½~0ÎŸ V2™õq´4{Ü™– ûDD_Ìn	§ó«x–W\`²ËjÕ¼à48Fôg8Œè
ÈZaÆ9s§»Œòr+”o¼t-#«Ë~Ç³8¹ØXñ×‘Zrö¤±“ı!v_ÕÚä‰j‡»U5ÂÕş
¼èsÌß“}%´P›~u(¦ÊrRÌØÂ‚8#»¿“E=Ô“Sñêi‹ÅıÕâÑñ’zÇÜ¡ù<şb=ªÇ¹ÂñshCTñNF¡àøÊ51ú W¶D²Æ&088·C³Ÿ‚;SgUıe^U`lNÅë¬Á;¤	ÿ†ÏZIªZaWñ&PHşùkİ^ÌVh^Ÿ|ö,1M"ÇÊ'ñ±–Á'Ë®ò‡.g/Êƒ)S'¶FwW
çôØõ(¼¨×”ë9/âØØğåıÜ@½§’Ëá!ò¬â„ÁX‡ÒŞ‹Ê2šİ]FåÛ,¦¸’’Zµì^Å²gÆA/:ÃŞj$dò›&]	9ù4`-1,K1ušİĞ`{…S1«lh˜Û±Å+ïeœ5h°'t£ã¢½šF€åC«ñ;{hŒ”NÊ¿Çî±¥`ÓœEJy/kx i;DĞ'ÿÇp¼r9…Ú‹q¿ò¾+XØbTòÕÊ`Ôö¾1øÔã2ÛHN ©ÑcÁ©Nß‰ğE¢rJRT·N‹»õÍÍ" ˆWÍ¼ü›5§NøaÀ÷¡]$Õi•ßl¶â‡jÓ÷ît¤3‹t3ùÄPjG{a•€)¹²ªè1‡i—ø¦bWĞÙ&eW~öĞw›1Y ªPZ-^úÑÍÙé‰¦OŠ³hUà:CƒáQóğóÚ+0L‚“e"}ÃÔ-Œ·ªß»°Uê¶ òn‰^ËèmBüeœ/“¢€Ã-h‘œ&ÖÀƒuµ7zBŞB`©Z´Š\yaàıº´P® ÇÔÏF¬c¶G-À,Œ:s™üx2î’â´³Æ¨¬¿ ×Î{2;q¾´4§272âgÂümMGvU~ê£bûÍÕEo•÷¯:]ÊÈE…}ƒ±Ò„ŒÂ[úéUçÀ¤ªÏ”ÁRÊÜó‚J%ém ÒÉR´İÀ‰Ydæ¯Ë)v$T.H‚š6ãÇK:RN€!ÉÛIRôÓ‡ÉfqÃ0/r
*I‡¡‚–4-Kô¡M9e ÔbÀ14´+¬h.é°Øu†,¦—U®9™ ¹ *‹ö[WA¨fèÉGµÃoœ	ƒG’Š(†g†)(ù˜ R ¬ñzŸ0LÇúÍ8^ÅYèy’¡ÎßÂ7oTENÀÊéÏ¶3Ú‡Ã5¥øØ¦wIÙ,J¯Xo„lL@^prŒñ$ÙÍ±vO'NRïPÙ?6hUï’ÿç&A•»Ï ¶¹kÛ ¥wÃÃú¥Ú¬ìŞÑ‰Z]Úİ´.³%q/šÊô.*àÄ\ÈoÂ‹Á]½À?ì™Xzp¬ôÅGÈpL´@k/M4YŸõÉ–°Ãæ¢”yÙÕ&ªÛüKA¬5%Ûf™áM¬l7åe§m9çd¤@lŞœXûñ¦ù7„z5ï~!œöíôWH¨´¸û¹ kYªY„“Ñ ì‚\Ü!H¥`ó)sEğ¸ A1³”2K<yş°ğVä}²HêWeT®·{Ì$9oûUìğzÆ½a§!ˆóâÃr¡²‘ÎHGâÍ»ŸUó¸# ½ÙL®G„ùôÁçÏÁ8“ÌQtõóSG9ÕS£ùØpq>³0/O“ş”W“ÑàÇáØL;ÊYÇHÕ<uh ©Kíç¸³u‘ÌªÈ6§7“"“éÌ/Ñtù‹üb¹e¥*C>Ó¢ô»
K
¹,Eİ‰–ZEè!Gººšáo«E”¢¦«VvU0‘Á’Ç›~óÍ>÷Fç'Ã¿M‡»<íŸ÷'£‹s·Û™¯íŒÈËh]Ä¯jwQ¤Q•»†£6µ®õTäoQ»ÎP\ôøï¾¾æš7¼¯¾<w^Òm›yJ‘¾ìW©°Ú#íjÂ\Áî¼ù#¯ªr¹RY‚®ÖÚğÚM-Û¹4Õh°0.ıTRAáÙš‹m¾®áø“®WP:‘ ü½k>=»ë((?_	"ÜªsÔ1Ùæ±ğêë*tšˆz¡?Z¹64Ë°æ ®Ä›ê/:Q«/ú$•›¤J!+ÔÎdôukÙ+zIë¹Û€u’Ô‰çÙ—İN×aY´«»Âî‡·R"MÃßÈ\° äšIBKÂİQôó»9¿Íèo.í`M~a]gK_9¬Ô§’òôHÅ%
Óºt¼ğX`’,L“vÁ|åze7 ’ÙÀIÓóAQœ?hıVf	bTò4‹ÎoL	t4%‘ı­ÉDğšÊ3ÃÑºzäšzy"ÃcÙ]Ãd">Ú]>tì»ªu×†„ôòi[«`Ãßf1Eõ¢Ù¨*LT›Ğ?(NıNpÉLR‚í º5Wc2oŞ7ÔÔ)T*Ş‘p­•åUÉÊ6nŞç³H6˜Åõ.å,Úg˜I¯û…Cv¬vÂ¥[®‹Uöè»rj`J;ºu(<MÍ„xt«HwÚ¥×èdx5.Q9a¯<ØÊrF8#-HÀ&LÀã@üpŒjY)KÂÄÛj¤Nÿ¶ŠÀX8{`5×Èãb½( …–¦Wd‹5bìy™«øÆÄ¬ıúµ21QÃê#”~¼³Å]Oá‚æOüÁêÏWÉ`ıÎ+×Â3YP‘Y´˜­D°§·†WôLÿ!*îtao…Ğd…Ô†‚Pw‘°à#Os”lñ²’^5ç4TÛãÊ†)OXYYºÔ§¾B^Y²ï)Øä_ÅRûÜ©°ÛU+Uıçëª ›çT]‚yªs¹¸Èìï'ÂôİkÁÿi¶ °èKRûáØÚ¡ÉµFjšp¼ÇuB*mo^[¡Û˜¼‹-¯Œíù„MCÍèŞ"¶Ø7c¦.|œ½èŸE[hz¾çˆûM³2‹ë›ú—	T3F²âNWgÑj†Ê¯9±:-¬Ÿ|r5¹û«¹œüíño?·¸ËÛô÷M£ÆQ}êkÕwŒÙ¨\ŒÿİßÍàVİ«ã˜‰—va9ÂÂÙwò°ĞjN:ÌKsãÈj¦º6¨¶Y(µÌÉ÷J-dve£Û¡†ºé†$ÁØ7ü5F¶)–CrÈ”Úîö½æ9VkR/¬€Ş1‡|ÖÛoÕ™7-Z÷`®ûL ©îßØwüü,N×ÌqRôY¢±?í×ÁƒÇìÍ$„a	ŒWPTtW>lV8FP*ÕÕíÃQ øŸ:ŒúêxênÎ(”f;E²¢î2Ä‚ÕîµÈ
ù@>õ×åYÓ„v=’:ïşï€_ ¤©¼x ÁÏé¬¡v
;<Ò"g;íê6Æco	İ<Éœ¥u"2:‚…îp(õGL÷Ü(rº¹¬FPê`ÔêÍ:~èôÆ°éO™Ş4Ì†åleU®_–Ì“Ş*Ê‹ù¶|'ÑËÍt‡m8õîu’À1ÆÎ°+D«2‹Õí<+¯Èùş3D¶˜ÅØççÏ·‰SrØ ©¯j0ùÑ¹fŸÌÓœò1ãË¶N ık–Æ¾š?<5†BØR4zz÷…Å1Í—èEEüNÉğ¶Ã[Õë@å;=†„ó¼» LØøíübz5<?Á4ÒîÃF]*ï6ÅWŒgŸ¯ÙPqP§>oôİ'¤'¼"‹&Zàpÿ¼JÓÇÖ–y1ˆ@îª57È)oá
œCeµ	.ìÿ–ø)©oç Ä5ù8çó[T\|vÒúö$—§o­½¹Œ‰Á¸@@ü-£Ï1p;'atsš%xf‹ù³Ò›¬İG	úàùçd•Ô;¡dXWwqŒ¡â¾à»	p5“FH™dŸ¢Å:Ï‘%‰Ú_R<qˆØMŞùAåy6YTU›ş]\üy¼E¦Ô0'1¥#ûšpXö¬Uşxb½8ñâ—™·w<½(##qÑ¬r‡	?gJ²k¯ùşYM†×_äOúlñß ‹õj•åe¡ÕI.„ü¯LfY*­#~W_µ{(¶ê`­*ßŒ<^&ë%Çï°h
0açi,€¤–ædé$ZÊ‹)FL*qóÙRÔ‚±cè€œ>s9±0"Ğ	€< VçñbÇs¦ı07µ’AAâ¦²B8Ázmœ¤†h]dEl,ğ:ªõu˜”Ê„âvŠ@ã+—†FF;å#¡7ÄÎhĞ#;°šf<Lx9+¡<Ø²:Ô#fC¥Ü7N".CŸ«ôx¶!İV»ì'¸%5Àe(ßL"IˆQvÕ ;€¿ğ V–¹¡×œ»k@\èh |¯Ì7^¼îthBøJ€ªñ˜
)Œ%xz›±ë_q))Ìî~/Ã[İ]ˆ˜ãïòÃÅä"0ídø·‰æ™TöU×‹MŞ7ß'7ö[áÈ²"œå.f9–«lÑxçëÖI7V5á+Xò~qW$£àe}Ì½ßbş­BÂ³©
š?nÉuÿ*iş„Ù%"Í›DûÔ»CUµ§‚TÖæ2G…§a$Px<Ñ@áÙlÀ6

ÏF‘AáÙGtPxÜkÏŸÂaÖ
A.Ü¹×:pÛB)nS&UÅÃ$-Fi´Z!.f€³šõ‹K1Üô2Ÿ?[î='P<]çnÄ»`”p0 
9M“d	çÊåªNXm¿0È`?IÏvWku1z/K6&1×æ4cëİdù,¾ŠãÏÀrí¢åÒa§[×ğ¼cllĞ•IeÂEö.ã+¸eÓ!Ğ0öYæÓèdx±›3ğØı<Áƒ²sãdÎVs*Á¼Óg4P'­C'—¬kÖß`>İj£	…„~Ó‹=ˆ œ [rk­àŠIıU”R³ñLêFˆÍ+¢Æ®¦ïºô~mói4üÉqgu¼NóŞ§áøŠ$î]üHvñIçÍëÎt›3—ö÷İ"º-xïNûï§ïÇD˜‡ı“éÇñhz9Ÿ®®Ü.,uy^“ìk [t¡+c)ğÈ²p¤'µşjµ`‘£h×£ápı]¬Ÿuz+Vèa‰$ßüÈ/W4á'ã ø´æÀ^u“gK$9°&ÛE‡ÚÜjêøoÒ/côÎéRJ:ÿCCÓ¢	÷ŸöûM7ò—öjXœî½™Ï=¼©æzw>Mîcoç±çƒu \n‘t x¾©^sİB°.rªùiÔêz§ôü¡àn§£OC÷±¿+ˆi½«ı™å£‰}şÀˆÿcæZ²›Å0å«’Xâ˜¼íü¾ÖÚb]¾Ñ8ŠŸEPyb,ÀÃ^™fD|Dü"S½ S‰îÙÔAu`]l(²,²şÛFÁ¥–©¶“^xÂ¤xê^¹i|	gíÂªP¿
¨jù.Å¤tÃfdÿ6TÅŠ½fßĞ0ÿÄgÈvÒüï´R\]Sñ§gib×eÏ«JuaVœdí¤Ğpå“ŒÇY¿HÇ1µ·²DÁR¶ç`X#qõÀŒ¬“è	 ‹şya‹|Efqë,Ê?ĞÚ%(r”òè‹j+´Æª„<u¿xHgİs3}XJn¥§ÁPjŠŸ†Ô7É·†·ğR#ç½hÚIïÜ4-º%·Ò—cáò¿êóäØ“«Êº(5/™$ú­İÒ9YpiŒÇqûmÓi´NgwÕ:ç~f>®ÿü_­;Ç7ö|s!øæBÀŸ¯Ô…à_Ÿ›Ïºf¦A¡H+ÖÕ¥7À|ëÖ'²¿6öa}‰tº#Åš¹˜ûn§.íÃÇşOÃzï§Ç§f=ùæå_öÿŞ¿¼4U°ùDµ’m?fwi¼É0˜’ØÁfq6ñ<Àá/ÿlÛI6âû›ğ|}ï—×‡‡ÈğòøGáï»âíòõ@¾uü‘¦|¼JşŒş©qœes Òşûä>î‰hôf3%1ëM¶Ìçn‘>ÕC¼ÀÄ9»øÁèºİ@`¶”«ğÀ›¼Óã“	W7÷ON¦ÙÏ@bˆÌçxb·†AN],Ìn( ]÷ôÓÅdx5½üx|:LOGWF•%C­ÑïƒÔ0–”ınş` Õ¨4Ô!%%¡Í'ÛšÖä™š‡0|DNõ…æt{-PµMD!óøÍ(ÑÁf]µgÓ|=rüÇ°ÌÈo5¾JÀÇ©è2¾ÕæsHìçe2[ÄX‚Öy ûTÑëÙpCŸş`BïvÆènëD@ë°O‰u‘zuJ©é4z#@ë¾ƒ~“¤.—¶œ ç8+’”l°ñiåó.ÔwäoûêØ§ézy”ü~FÊHw“äE9M£ex±´»a|j ç‘3úï‡6E× -ëDù-7¤§oL2$pSRq÷€ypÔ1®IÃ-.Æ.°6é2²}[L2%’+’™·KÉ~½aÇayë/œ—>X‹6UŞ^™Qr¬B¯×ÄL+tåŞ;)Nâ%üKdŠeT*ºÿ|Ù=xvĞyÖJvH¨!z¼bÍ;Eòù1ÌİC‰ºõ–ş	ëšFİÓ¥#yØñuNp‰ÕƒuÀ"–—›:ã^ôÎ±?>¦ŸÓì‹êı¤Ğøta|İ„¡Âceªx½b‹1Ës±`§9{šç9¦'¾îA4GÜ©hV8/7œœf$Ç'?×»oCÏ¢’Mqj‰&Y1”"d´{›ñC0úTÂ…Ó&ÅÛ¶ŒŸâëKÒ¿°]G•&äDĞäÜàÛjâBO ¡;Ú$2zìU±¢|0—Q	gÜÎŠıûš¿éÍÈñnğşÏ]Y®Š·¿üòê—_“ÿÊ_~!´’?ÀÕïí+ÕÌEë¼ïöş‹ÉøïîåHÏº«°·¤ïºÕ‡ì„o
½o
=ÓóM¡÷x
=³š†­Ú‚",{hs9 X7—ÂÛîK¯êIÈ¤5pÌ&Ye5·A…‰.¦Eå
ÅğÀ¶ ò†Kÿµ‡ëñš&1‚FIX×¦Ét­ÚGIo¥D$™‚g2'eP%‰ìä{u—åh E=£ÕŠl©]UaĞ$æô<àØÃ.Ó«¿÷øÖ‚ãŠ…¸êÀ~AvFF>Ñ3¿8‹RÒ-î %ãøïë¸(»¤ˆ£ùqÔ!¼A<ëÓ‡y WñÏ0ÕHFt×¾BM×‡ìŞuWŠH3˜üˆ»ª¨ù×Eob´1}îN:ÊÏd„¸a*™zÑlFºjz€Ğƒhf'fk^'êd‚«2©eLƒ…_¨ç#áMØ[Ï1(”¾,£x¦g ¦Ç\`z‚U	ğpÄÙİbS(cË-ˆhÃ/ŠO‹z}7ê-@%Ç½<úÒ‹Áò§8SĞCıÛN¥êÏ)®ÍyV¾#{Ø×ŠßøˆóÏ‡‡x¹¸Âï®7o»YÊ‚+Ó»¡°@Ï‚¸0š3%Ï¦vä‡Ğ†ø©â‰š{GKãÛÁ¨Zçœ~‘„âğíæ†¢Ã»C*—+o
ÿ±5ôÇ6°ö)¾óQx… kóñ¨»†÷ØÚ£!¬‡Ò£ù l
å±ŒÇ> <<Û¥}ıÙ‹nÙaà>­î0l6.Â¦h—[Š"‘)Î V–2Ä)˜ôŞÔñğìB%oîKKóvkx#T ş“yr½.ã"¤á¦ |zI!±øÜ´° |F¥Kİç‰qâ„ğw	4ªœy}5YÉ;c=zü¤P¨dRDó9ù*Z
{ª¶zÃÁ²,Å²İÔâÓ¨>M‚)noÈô¸ øl¼¯4\Ú¦È*‚<oŒ°"dƒ8+¼ÒoR¯åù&õšÊû&õº^©w:{Svæ[
Ñ—èÈëß¶;E<¿Ì
“¬mıVÇÍŞÔpªìF6Îl[ş‡(™+§ZJ
„Î(*022^{½¤Â–ĞyfsÚ“ŸìAßWÚ-=dC*i‰¹@(¡)–Nt•QVYIÊVY©7ˆybH%ô³ƒÂcis|á³¤ªh›É‚Ó$‘æÇ—„P0=Ô±fƒ!w¢ó\a-¡Cpº9öŞƒ=C\Xu(HU½Å3FÈ¿cru4:NşE›W´h1!¾áC"Öç	Kƒ"ç‚!Q÷ _¬ÊPNeqÆºŒ~<Ç‘r%‰SfAäŠN¥~¶âÁæ¸ÇU¶øŠ¬ŒlßS’ÚhÆ.Òæ+ˆ±#­{¯
$šc”Ê_¸:8ØIRÌÖEîfl2“¬"¾Z/©½9Åújêª¨4Ì„ÁUAÁ‹ş|™¤]îW`ÜZØ8qª¸qIÁ€é‰$v‘	xÑe%ƒÈëİÌ¢ŠÛTnœ<µŸt¿¹ÇÍÁÊ›uN öv%ïé:†&É8J?;;á[x'Ãd¾^rz1^²£ÙúG9 <v÷³Öæ¤•Rˆ&y¤Îğ'KõõYG@ÒæâÎô5Â)‰S¿EQ&ëñ¯]ı3÷ ‚“=ø‘³¿!QÍZ½éÊ:‘kôCü!°lÔheöôB™òŒf“¹ù,29eR9…•É,!§rÍ
L<&ˆ~VŠ‚>P
õZÖ"^0p;øIzå&¹¥sÏQŠÉ”éù®¢›XRûš­ÖÕe&hh‹\&ï¹ùZ4ZšÉ¾³(Î“ÒÔoÂº5}V¶½#Îæx™øZo†0Ôú5ëÒ>ÔqÕstVõ9ÀT@€™§¬Œ—™V_26kÚ1V¤2š†^¹ÒˆİiGË<hBéÇwC½…(ù+@x^fe<Ÿ^?À¤7-F"gÒµhÖ:Ï¼5á¢6¡ÜÖñ:ë¥;İª§/q”\ë€W¹»åĞõ–+²à>Y¾-•oK¥•Kö:Ï	]n_Ã„mÖŒ½Ì[…ÙèäH¬&Iï“²’ÿi§x·k™®RgX—yR›DBïyE÷ÎõÛSzMRÍ0ƒ«%©íRaX÷áMëMM°—+®Ù`‘ĞuûOS)^±§íJ.6p9§~6ê‘àÕ+éªLuÄqßaçaF0ŒJcCIa‘P7YåÁ Ğò`!–Çš#OCùÜHÏ‰Jq»öÙr¥5ËäT“è8ú„ƒm8às#Ê‘b"eu¾×Ôˆr¼àÈs¼Â®‘ÀÄ*/ê;<”ÿ“§MÓÕÁŠD–iö|ıÓDÍJ¦ÜzAøÔŠH.<J´ñîEL¦Öù.Z ßz¸\DqåïĞ›ÑÛCp1 GÏ±½-	¤ƒ7 ÑLaQÖĞY!”àÉU¼ Ÿ†kÇÌ°ßY]­Ì°-õ¤›ì]TĞrê›8[…’n4/~~ñ+³Šì<3~Y}#^¾m–BôöwIšwà)¡P†ÁŞ%yü%Ë?İ†Ct™¤³»IöY¶¬A{ië•šp³øÌ¦FÅÆÄ³Uk…¬hŒLÈä~Vâäö®Äş‡}İŒ Íe43ˆé¥1ÄùªûÃs_i×+§Q:/fÑ*¤ë,
ÂşS2Çx(ol	8áLŠÚ^
¾k„‰ªI7#²-Bxù‹uyşYt?Iuk¶íï³ öx·²(Ñ4Š:š&G,ºz÷ƒ±*ŒV6‹DOÆ^,í<›c`$np:ü»Z~L6	p°ÒWÓb-Ók¼ãw§\·Ó:Æ@VÂm ”'\fD`—?üìXsöåøıœ\À’+´µ¢1µƒ ë…ô½X¬¬2•hZÏ9p`ˆ¾B˜‘8 *:¨…ûÆû©4ÏÈvHöÅ/ÌbìŒ‹uG,n-s¸K>IşÅ-Æ©áv×hG*\‡—§ÿ>=¾@°$´7îÏ£U);)¯‰`ô.Y?b¸cbßp^ôg'ÃéÕ°?|ØFä$‹•È÷qº¯î"¸qèÏõİ¥½še(Nö6|û½˜kT5j`9
˜¥(ÛSÇ_…m
2¾Áû—Y	îR`R£ÅUœBŒû˜³¼	Åp«{i¡VªQ™Àüä¡7è´S,%¹_|@9	_zıÓáxÂ½.GççÃ±Ñ„J,I=‰Q,$¦ÉÚÔ!³¦]ÅeI†º \üÃ~^WjŸ'Å2)Šéì±Íõ×İÄW}ÇóâSœ'7,ÜˆC}1CMX²ôocŒ¥+æDû&\›ô>[™÷PÀ)Ìj`´í¶0ÍØ
ì|Ww—À>ø;PšĞù3%¹PmR=I¡µŞ\3«óç_Éô%zàüõË_ÍÙÈ~±ŒÙÆs#üÍ‚¹ÕoäC¦E1H‰F®k£¤‰Û©Tô¹t¡½ö„8Î~£‹~\g¿±–‹ß”%ø2•\|xá€1pÍ>ƒÑ]:ïÒPj`b€ÇŒ,‰,º®¬MK„™Ü5*°É|eàN}ìxãzpT‚_a%VËÄñ®f3‘ãÉ)„èZu_şO©EşøÓ!Ú‹¹Òş‰%%y¬!(áæ)`€`X)N©?°Ó)…8ÀiÜ•>œõ'ƒÓËşxx>9êüñOG÷y„»Ïäâ²óê×éğİ)gÿ…ÉEŠBœŒc°–º÷ãÉÓ‘üï%èÁÅ·‡{·*O\OßU?Üy°&¿€šÈ¶ LŸCO#!SÛûzÄ„ˆS"Tˆá»²şÌ‹ÏÙF\%II£-7X&h*`åUü_@ÎğÖGÚ9XdE,aj„ä
 ‹°€h°.ˆ´Ÿ.nnÈïîŸıÎ
¡ÙJï®Ñ´@±éÁãĞÒ”Ïêªİ›C{¡±M¡šf"DuçÉQçK .ÿµÊb@Š×d3ø+Ã’}Cd]OaÕG°Ø½%ò†Gá>©&·`Ñ“Å¼;²ìÌ_g°øàÀÏ Ğo›Jî‡9àŠü®KK®p¡HÛpºHR›azTÁ)üıº£i³šQ.ir\Ô½İ-)\…Ï@@XõËFı"=	QDìcáö<OÕ\ÊáEÓÔ#›/‹ĞÁâ¾ßÖš”j¶éx|şüWŒÕ®ê
©ŸºşñÎNåoñAQ¸Ò T'.ÏUà*ß¡0F)¶í;š±é2
‚ÕjT"<p¸jŒ-rºç]”,â9U¤÷FŠÊ«ß€'Ô÷?a
«ZF×XÀ‰ÏîÜ8Ø'qI†ƒ9šL“ô&+“•n6ÑcÊı\v»ÚçU/“õÒv&TÅŞ3“,*JfÅõ±†â®ÔŒò
Ÿ|©îê¸5öî’Û»ÜŸT$a’5œe¿r€¶fÃ
N'>T\ï&9o®>¨²6Ù6›S·#ğA¡âûÜ_“ã¥Š¶ÈVš±cM%û5İ
"[
2^‚&çŞÒ½3#‹{4‹´¢„ {?ÛMB\nÒÂTcêyÉËñ
«ÉMn\<öÏ_õĞ?üµ|¬/nLİH¥
hØ=ãÀx·³ù	³”F a¨á½ùø~mpj¶†‰İ8öóŠ:`êÉ]mE]9˜Ã?ú“BÙ†{{$ûM"[Ñ1ÿ´Ä,r”×8¶ÓKü:˜áAÄ»¯a:ËV%uåxÖdÁA²§;VêÕV¸¤âêoç°â¾Š\|ÖVå3ry<ªoÑÏ’&KkE/Oé–Vj÷|ÛyÙy¥©šÕ™Éë™¬i5î~ğ´`CË‡éŠ¶ØlÈ´¨L½ÊTiú¶!³ìÊ‚CG©bŞnë|ñÔZGš.…šö‘·9‘ãüª| }S¬¢”Bª€kf‚$Î5vWo¨²®‹–M“
Ğ°NGç?š7¢§j•öŠƒşL”pÙ.ÛLF‰‚%Xh;¤ÍÆ‚ÿ5^§)Ü0Á©}½â1s
¸%úæBãªà^kn$‘û¬.ÓaÂ…„%uŸ:³]W¢\E°VĞD9©Âêw@“<zq×`ãDE³â§¤¼ë”·¯Øœ[eEyp(¨šğùs)©:¬õÄœ,ÁA­*†µÊ:´Ô ÑóhÙËòÛƒC)¹\¥9ƒÜ-ó¤Cú#"ÔXP”&uÛI+ÓTYá|
™È’ëBA°%ÿïuœ?\Fp%AVT÷€u€\†5¤ˆ¼ÔV"Ë§Š¡ëÑù )4—^àš	q"«-•™,ZŞ‘=´%uãT¹â_†c{PØ-^(^d~ÿÒ„ßQOrÇY­]f 	N2VëÔTnÇ(Ú5”q½fæPh´b6ÊëŸcû£¨…–_üÆ­êT×¥1ÚÛzÍã|‡ÜBnøh^W®íEàìEeÊô‚„‰÷nÕzÏ(†ü`†DnÕ"YğÛÎ´eş`sÄÂÎŠÛù‚GœóÀâ³èÁéºœ°§Ã·€İû¼ûó‹ïÿ}ÿûßÿÇô×g‡ô7üñ–ÿõÖ™rá2ÙªÆ_~yÛÅ’`†>;4U¦TıûÚÔü*Tçñ–“KÖ#{ŞıFè_ı_	£}t´aëGú¨ÊHãqıph=µÙv ¡Hì®Ù¬XğŒ"v5-™e³ÍöÛ2*>æä)İJ$Ic›k ZŞ’¦å84‘íWÕÜkUeš]ÔHƒvıPÆ?ÿÚ¡ÏXô%áuı²Díjš "tÂK£tô$h	¶NÑ68j«qtmyğ˜%\©uŞèÆ¢tBø4 º¨§J±¼Dş|õÈ
'É&C=Jr‘ği€EyÊSÖ]e)rk²_ìŒ*gíj`ÍûLÂs¢[átœbªø™Ğ_ş«rÿtrS·‘4Ä›µDÏî¤"xÂ$#ì…™Cı—r°'šÇÙ$!d”ZN£u:»«Ôƒ0Jü+¤Çm	8H€Ã¬­’7æa&R<ƒ.ÍçO­+Ü}°”HÙ÷QÅ‰IŒvê6ñı”ª–“+è/îcßñ¶Jæ	ê>âòR‚·îÄæŞ?âªt3zèæÙ2JÈ¾Ø+³ÓìKœ¢Â9cÇ'h±Ü¯MSv ‡ [ÂaØa[¬a™À$„…n±5À³Ûíğ-"¸ğ<õaº"0d İ»wpleW_Ôç@Ã¡4äù8ÎŸ½ù³É‰‘?ŒµVçlk­ÒØ»Ø!ßŸ*me5\hœ×*·)áÅ)Â%€ıïÿÊ’`óŒ y	Oc@ÎFµÓş6‹Qäè8íöŞ%‹ø4»íÅİûk´×¤©¶‚uTTçİLIeÌã×Íj½T ‚*Hgd f·J#í¬ëéM;êxA1¯10‘gÓãäNÔğüÙ”=šœU^Ğ›”™¼E?’Ïkğ4P®aÃš*ØàÙFÉ?‘²­j¹*Ü
ê’ı»õN£quNAÈâù»úù“Èo-âëš¥ušÈÎßK'¤¡ëïq¾6µn1Tø}ÓĞBğl¬	ƒçÛ‘çi4CHìOì«şÒy3ø‘A!t­Qz§k˜nU°°ûh±–è)Öà*,'¦‰dë
sY·•*3J†Ò¨ƒÆ”G$›nRUÒy‹nt½÷çÃÎ+ú÷§ÑÕèøth5Ù’[w™Ç2*ì/†¦y’ÍaÅ¤ïG5xV‚ññT$6£Êñ>ct`YQ"•K«i$ŒiŠ$©ÅşbkfÄ$,·d®l*¶°rq³ETxº¥Ì‰»]vÈ0NçEgÏf„ÙWF„4s¯ŞVùşKGğ—høÿ‹–ƒ‰¡×)‚X¼ĞSğa¸F uŠeè«k.¾“v—F“úôÚ«‚t¯Å^óõÿK8ÆÙ(€k»Lå—–L^î…4bU²Q:£;ùyVV>Mƒ,ùèºÒVñ]¥Qà¦x.Xœ¬AZåwUÕ*Ï•*t‘dF)tÒ"z@Îacôltãv4¥XsÃtn¼Ja³ÕéÂ €­W`7Î¾ˆ=i:h
“ï²íb!<Ş?)«óŒËTE/ñ¸¸©³eµ\w?èˆŒï¦¶ÇKçÙáª‚y¿«%¡Çôa]%MGh–Fç{ÉÛ™½%’DPªV˜ßİ&H4‚Fí]Uí¤,åˆq§-w5mLç.Tr’mÇØõ,ĞäŠçÏ57ñÆ‚+ôŒÃçÊñ\
ü‹BÑ¬âíl	ßş>Ir
'I @â¿A®Üœşj0¾8=ŒÆCŠ%wrñÓ9³ÿşå¡dJÇ ·ïEWMá	Jcm,,K+")R¤“{5Å@EŞÓŞ@DôÚ¿7 'ÀJƒ÷.×{+Î•ÉzÚèX5&1ú:Å%è»kEûÔ•SË åxş¼ŸçÑÌ¢¿*Ä¼¡öÕü€§qÆ»„Dô=‚IÉ.Éê?¯Î6r¡vvK–D§c©0\U7hÅˆ¥R5òÑîùs]bş\+@vĞñÍ#ŒËr™.¥»‰€8`{·
LT”RĞ ËZæÒ2×«ÒKfõë¿™_ÿwóë1¿†	“U:Y¹)Ô[ø®tß$ÇÊóhY½ìZ·Ñ§ˆYÙp=Ù *S6¯&»şOÿa¨ è{&“àÁ9“ú²Ğ|Q?I
Æñ<¾h™,.
jX,±Ø¤¸ŒÖE|‘baÚRcM¦DfJO•*É=õç@pâU}zmtw€Ö\/`T„<ƒl‘åœI¹ä.wN&à;ô¦ŸŒŸî’2ŞœÌî!H£HÔÚaãÃ™ï“CÕ»Ë@5+BMy««“2§Çõp1ÅU	U¸¯i¼{¾Ér,ŸˆbùD€å‰ÉŸÏÙDá €›m˜ŠÇ¡ëZ@u9.‚
„gÃ“ŒôP@Ú0rÄ´ŒÔùp÷6âkÚğ@å¼«På³»¦óÄœûIfŒ§ê¦s‡ˆ÷“˜]ööÒxL€İ“Cßj½ú‰œA³/pÔs†1u§±Ş"NoË;g/3şıJaLpò-™²^ù9~˜VúÅ“ø&Z/Ê«õõ2N×PEƒiœm4ËRÛ=‹R¾€iUÖš©¿0ÅPãYš8{Û†Ì9Zx&¡“YÄFÖÚ#@İR™ËpäŒ%LOO»-=^ç7u‚üİ¿­ ÌúœHÛ±·È)7ÙPÅMy’­Ä
+”lCèjˆÎÂdÛ&[¿±„†¤ß‘ƒxœÃüuU]§‹÷—ÎÇ€•oÉ6GEğpT¦Â4raı¸ÆÌµ€’ğV«Ê‘«†Íõ’Wf«Ë(~@}9eÃ^(Ö·d#¤ñ)°WMZâİŒ-†PÆ’ÖÃ„u¯)gÆF¬V'†ÃQš–,MÇ€>Ø}}A·“Â?êÖLMOT¨&eÁ‚k·çjP=ŠÇê‘Œ‹Í€½…‹/nPÂ¿YÌ›´»šıÅâ½X…¶ TB—¢¡T&í• ÄÓÒ£~ª¾T¸RS“aıÿõ¨8_¢Åb­bI&yÊz§·y4¨ši™½l?´´ø“Eß%Å8^e¹!ÜB"+©ìHW½;í¿Ÿö§£Éğìjpqz1iƒ*ç™Äéx5<&ãi´Éá;¦s2šB7%’q?nO~<ŞH"Õ—*f3åVÌQªÿ€;ê¯a¶jÔ¶{Şî¥s7À^jßd¤ùlxşñ¸?øñıøâãù‰‰b»úİ³í‡ìÇi¬Ä7 Õ9OBzç–d£³ş{û´ÚºuêlkĞBÙ*ÀLıéèjòşôâ§ûá†r¨íğöÖ|+â½â+‘,­M“áß&»ÛMÛÑ&÷,Ûd÷}¬vñºš—u‰>?Núï«æ†©°üù×ÿ¢F®××~à!¼Ç³,Ÿã×ßr6M®ã~Í¶Á\Ù®ö%ïœFˆN¸qpôè¤]öÃ§'Ã»¨¼„€»}•?Åı³G;?ìR·˜NéãR9¹¸Ü¡`Hú¨´>ò¬ÁjÌm¼ÍæûÅnÅ ëÌİ›@¤jÈHE\u+ïÿËuµüëïz3¥tû”ò‚µÈfŸCÅÓm:Tš	1¢ƒ2›Ğ£X¨ôĞD¡„6¾Rª¦&˜–`ešF4Zf=%éº:œãMOÂOÛEÑƒÚJŞ§$[ÄíYğÑÚÍAú1¨<Dí%îÜûÚJÜe’~nq`Î<âèÃíã(y­d*…­ä+…md--ä.}-d0}œÇT²÷•mÁ²¸U˜½+	kƒÃÊŒ·®rÎ^M±¡ŒÕ ÆêÂ²|^±ÃÇêäÅµsÇuKeº	¸X—;©””ã­hwCÊóV¹ÓÊkë®k·ë£2’£Îq†±ÓM¼E>ÏIüáÈ°m§º$=^__o¬¸ß†;Z Û‘c¥úZT–j¿"óRàş“ôD
ªêï¢yö¥:ãv}Äô[\moK¼4É¾Â\¸	õÙºlù€£¿?µ†GgÂÀ[j[şU«Äö Ùk	*J
Ì8ÀˆÉıû(Yà@£gî‡<B'¼g†|/÷3C±E¢áWß¢?È+°í2nÇ6ËÛNqCş§kD‘Ğ<¢axû¢ôDí\YuD%ë¢}‚ è®à÷tVåÂğä¶³+5_ˆóûdOv¥f77âÙí³y§ê‘|/SU¸§¹ÎÊA”Ó5èøRÜE9½‹
Ì€ÈM2ºFé"Iã°s$ÏfM’Òû¤&ç6kÒZÙÉ@×m|M¢§½/;êXV‡K%’»µµæ^i~òıÅy>·Î¼¹Z£5U)­Ò^˜û{™›¥iZÒ †Ô“‘Ø!ù;ÚŒˆ¨t]‰îoV[§ÀÃqgòÂ6Mh ò ğˆ•Ág @¦,Ñ×ÖªŠS†´®5b{ÓVãhº­Ól¢ÅMø°cù_c§T#ºYç4š=R}mÚë8‘`ÖÔZÂZÙsIÚÎèjÏ’·LT-à²¨Oq†ÕJÚ[¿¾mˆjnÎ‡XÖM³-$ëlèdß<@	&ñ/Æw¶Ü“<å’YùlÜkØxgl:À$KóÁ%™<›qJ¾¶®ÛL `]¸YfÚ•!¢ˆLá¾»Vf÷£©³í£”õ‘=íÿx;&–±õº"ŞÒş„ò¬ì•–ÎœÓSxfj4I–U…{ï‰F–9éÚµüñûWÖ¶&¥ÛwûÔuÄùW_+†ÆDz8iñ(àÊˆ%O¸÷4X&#„3-]+„'Ùw«ÔÆè2rlv½äúÖÏóÚºÇ>3ÛÒú&
ªh±è¯çIÖL„lRşk˜›tÇfrÕ-!»ÉŞºG]@u=ûÛ6·MÔêR©›ê–nĞ”+ô4	0zÁt¥…­8É¾ƒkó¾I‹éâNåË¨­MŠ$İËHÈcÄI†Šâ¼°OKLÚ²Gš|]¿3¸¤ÔnaXOŒ^5éæÇ|cÌ$ÿüXÑh"',ÃSë7šo£eLïûhowÛ®»›…m:­#ì!û¥Pœ¢d­”1YÂÈf9Emn]·ôá|s2Àó8+)ö²Ûóš'ka`Ôê¿Q2´i²$é»,ÿås"	µ‘‰Ñn	—-¢VRìµ¥¤Ö="œ •Äá}Qk©+ø}tk)LhğwˆjÒ*º2zİØ>ÂŠÚV }Ä±ÑlåLã#ÚJâÄQm%ldÏjƒïV‘Ç·­ôUİ‡7'í¤±îÃièH®m­¦m-±"jïà3@ë6¾Z;Ñ:Ò’ô*)ãVn*`¹ÜVÚ’Q3gí›IÏµ™¼ªó.ï²´íäµ–%×ƒÜÎnTèkm?¢Zkk[EY’â}}+i#£Û^âXÇñ)×Jy¶šÈ}{[º†¸•d‘Qm%]´»ZËƒÉ‚/“'¿ 	¥«µıL¤•Ç	ksÏµ}Eàöç7Y¾ŒóÖØÚ¬$•Öv¥Fakû’z$¢UÄñnl'u¬ëöeÜ{­%PéÀÖ®µ#[KhuJŠ?_Gû°¢9µ6¹ÛZ	© t_+i”»ñ]²Ûo/yƒhv·q*CÜºşãôµ¶“ôS–ÌZIXµ•6¹ÛZÉY”îk%r7¶qõ¶š¾$%$Å­T™‚•i[i“»­•Cé¾VÒH»±•W«¬ûZIí¶Qz“µ4Öm­¤­î¶Ö$…îk-´[ê:Á:°¥Ô©]×J¦¬ua+©$r_œ®ÛÉh@èk-qBÇµ–ÅˆØZ"ñJCLé@ÖùÂï¹HR¼¹w§c2°CHŞ—[ˆ@*D«¾Š£|vGû.	q•n±›ÍiÍãv©@2ÔeŸ®ûîîjåÖ'v[+	LÒN¶$mç\ãÕÒ‘|œ™¦~¦Áûƒß/>Ÿ¼N†Íb6Ä6$‘µ­>¾øÕÚÂìl÷z|ØeCv6Û÷9`/­í¬-£¿iI+Ô/àÛÙÉF]ĞºàMÙS/‡7Â9UöÜ
×\¹Ì‹A–çd¯è§Å—–	à?Öjiş”gém©cİ·_ò¬khEHû¤ad+`˜Â›âd-oKA”° QŠ÷rí Ş9õùOßxI[WP…µ#N’h‘İZ!4£û¨Œ*Ğ½Bo+M€ÚqœŒXJÉ?DÀZ§¥W-SL×iGó¤–yÖw2$Br«YÆ[’rh;²>ÔVÖß^jß
¤äœ§øİİPRk›?LgùÃªd(_–~P¹:DNêéŠw¾ÿşÑøooñ aòf¨ra›šQÌ¢¥
£Í¿İDŸcoçytÓŞÃÊ©ç¡kz±ÄÓò.!zd\´§ç!¨štõí²´š&¹3PU¢kÿÄD%9ÃßmOÎnvsuªæd“ô6`°#¼ğØg;ä”IĞ“Ai+ã›D+ÏX‹×ºf@ôH¿Œ’GÛ
%iíı}£ÀÜ„ü‹z4Âšra9 ˆ²å*+`ˆ2îŸÆ‹­õbÊı*F$ûê.šû£OˆDÓ{&x©x‡]MÅÃ¯pØ	oç¹ÎÊ2[^ÜÇù"zøzºÙFötò¦J>Ù«ï“òa‡ÛšÓ½í#¿SÛBµšŞ‘M†›X"ĞkeÔû0IyUqí–$=`„ç	*÷Â¬“êÆn©nêàãøêb¼çÆÖy±U(‹àæ~O~to¡é<¾‰Ö‹mu½ÁMõß·l'‘öæ>^mâÿ  ÿÿì}ksÛF²è÷ó+`İsOQk–”d7ëÄö¡¶UÑ«D*ÙTNŠ „5	pÒ²ÎYWİ_sØı%wºçyàCR’EUb˜éééééééî©Ÿ­Éº%ŒÌ«şQu¤}xÙû©·rüÓQšèûƒŞàhı³÷õ£ô‘Å„Ş”qS@+n^ñ8ó·1Ì»fBömÔéfİâl…Íòy:¾Âc[I*ç€Ö{­4²d3/&IC¹öÈò`t¢l*’QD`øãaş ËÔãÊïvô$§75_>an~ÔnÓ;OrxËÅÍMRşki^G÷èüòå“âˆæ²hªj[ù‰ò‰^’º)¿k~m£Œ=Iy„#Ù'i–@¹•w†/_>e=*Î³„×ñ”PĞšÚMÚÛ–µ^¤#nwkMª†`¥°‚õ4Š¦Iı¾hÓÂ¥ÈÀ¤>'¹zbøĞİ»ÂOpú‚S‘j=_?NœßıªK7¶‡x7)Šñ&×æ–ßMÛ8ø>“UÜŞğP‡Euè€¾ñ¸`wDÙ(Y%ÔSFds*Ô±İojD5‡°şqçşÀ*Õ²¿ä6¡HâÃœºy?Óã
„Ì“`,°OPwˆÛŒI2Íÿ"9gÉİ  
)ò¤4¸æn*ƒ]L¢û‹hQ&kõá½F¼‚·õ{x“Ì OáGg;xÅ¨Ğ:AYáÕ(n†Ÿ:ÕTÅ]1½Çoê£él~¿nWƒ§Nôº-ˆ¿z×ãá7GwŸ^ÈyAêlBñyêäCárşPëxÁÅ‘p‰?ùûè×ûG“ì4äD°,Òû4óCzs;!ÿı¡´ËªÆµV%8İKœ>N³hpÂš({“¤˜sëÛ×îŞ±‚éhAO€¢²EçØö&;¤»ÈË5:zôÕ—ÄByÉ­İÙSİ¢¨Ìôm‹›»÷\
ªçEĞ!;÷ "#´óùçû`üóü¹ŞŸWYœ#K|"%IiRœ°ü‚¿}Ìó/³t¼Ú§VìZó&à6¼¶îNÛ£¥MWN©Ã M7>@<©uî Ÿä«d%X
QØÏ=u<S–]µ1PáÁ±,×…¨}ÚœŸı`1³o€ŒGâÈ>ÒÈôøxL1c÷.<8ší^j¨˜$ãy;‰f¥b¹x^ßÒi31_çó’ûë<*˜ÇØ
&«•Û^Ù>ºF$çe0Ñü6;ovl|8:øaĞ{¯2à;[™äqğ"AcšœD÷ù¢v>=u6»|R¬<M¨wÍ®æÄÍ²ÍNè$Ú”“‡‘&Ü66®s~Ëh#Q¿Šñæ·M¹¸ŞöC}‘ÌE6È¢É*á¬3™Gˆ§n;› ÖGFË#ª
éeèŸ¥Y–Ä'ÌxåD>C²­E;Ê
G}kDé.%ªÕ]Uæ§Ût¾Z,Ÿ~M‚×›©!¼ãéÜÜæĞš~IPòKôë³š,›ÒF1µdlú²—D`“ûõ:µ-z›CÍÒ‹mÆ È/pÈ­1ípËÄ
"‰Ç8=1ô°ã.“Y^Ìû³húÄ°#ÈjE“´—şşkÏy8½$v…è¶†¨5Ñjqu`xI–¯|²ìşêq·tú’ÈOî‘©„•d=\»®`6¡•¿!“:ï±©+d°"²;‹£bíˆ‚'¼Ë¦‘nƒ¥~>ÑÚ©·–2š‡q?*z4!càÈ+:«P2¯Êä‰D=†OeVÎ6úÌúVš	œ`ûóh¾(WØ5+XDŠi#ÛäºÒ­‚öÇM~˜ße”•Øîg½Éâ–Ajs©ëø½Ï±'„GŠ$ægÍÃkÅ‚¾:é:®WE'âàS:ZåNædãH-¥š³ñ"RÆ…×ÅåùûË£~¿÷ ˜œº‹ ¬!.ª	¦ÇMÃ¯dœUìGÕ¹}3#ÒH“ûŸò¤·ó¨X÷uPí…(M›ª¹HDoßïåÉnWKà¼¡e×Eß:&´¿ç"®tùãñÄ·m'ÕÊLV+‰Å&B|ÍX*şiz¢a—ßW’tàbÅ'Bg»åTØÏçı»tN6kš	×à §AŠúJ÷˜Ô2`nZ3ÉdXG¯Õª½?!¿[4/¯L÷û‚n ¾Ú İz€=Î9I³­îDJ‘//“’hôpS<§Íøˆ!ãÎ¯ü;Wº^¹ìÊš¬³cÚêŞow€°áy¸)ÔB†¬wÆå8Køví·ƒ1ûm`§kJsJ[Õ›t÷òÕ=-ã¸L­é¸}J •u·†¸¶ßîJH
-})ãtŞò Î»^mZ—…¢ÍÑãÚ7ÖÔnÇø -7Bî`—¥juŸ‘q^ÜEÅï‡ı6JÏ£Ö$àîªÃhîó§ÙÍp­¯,ÎÑ9?Ÿ=<ñ«5¼úĞSè«aÒä½~”JÑxM&Iq¿I'ñßj×hyÆ FO¤cÌcâIÃi3ñ¿Õâö•'5RO¤oàûõ‹ÓlF[FkzİMURQT¹Bİæóü	¬åô–ÉÇçi0õ=.. ı ‹½ÇE£?*Èäz?Y:ìšğ€®XÁ{]‚n«º	yÑå‡Ó–KqH7!~ápœÍówi2Ù„ì°JÑ| |ÕD mÛO³Gm^i¼ºoÃMÊÓŒúl˜SG¤÷E’d»›§ÙŞğ„amÃû“EòC»J/÷ªø!VZ~š|å†i”7¡áÔr5&é!mÇi’ÍOËg'\/!X,®²éSè†Æƒv¶¸ß‹§iÆñxìö½¾z,N8•y
8<ÀHè
îÃˆF®Ô^&, áq¤ÙCàÓ¼½ÿ€mÛÈ^1aïÓF¡…Š(EEo2»Ú%8şã?	
¯óùA>FY\&Ùbş§í'Ëùhï«ğ°êç 7®x4Î'ŞE#Zb%?O»'pìŒ&©¼Lş±HÊyÉyŞÅyâá8Lôî(;Æ xDêµ‰5 ó£JÃL*I™/ŠQR^9zb¯9 Š¯š#Äl‰fg‹ëI:
 (¡÷)šGE5YoıUGF’Ô"6àDëˆƒœtugå<Êæ“{Ï¿re€¸b!ÑÄ?ï'7i6Èû·ù³Ì ˆ²2ÅN¦#^çsÓH³OÑ$I×ÉISåe2ïLg’ø,"Â“7q4)Y\ç¤poÈï2¥y,i¢ÈÁ£Èíì%XZgB)„8Á³¨Ÿ;;²b¸„ŸIg©E“äoşv¼u^®®Fc‰ÿ„»‚ğ°Ì ½,Fór`m+çù´êIú5Å½|’DY–À)İàr‘ep4'ı$3HZªq‘OíãD>.¢‚›"¼)-qÆG ˜'õéÏI«@oúß„“‚7Á®>÷£2á¥ƒY‘|JóE)^¼n¼ê´ÁãE°§­(õÖSœ£$SÀ[Zà»¨<²E4¡'ÆüãG/IÚˆ	L®/æÚ —aÁœ–Ûf}ûŒÛ±”l4óbKİºIÛËâ‚HÀ«y:!¸'eÏ:ßêë»fÙÈh
AÁïEcİ¢ÎÎ?F“EÂ'wğIùõZıæãwà&Ö!Ê,ÊXë„Ûbl­úĞ4‰ÊE‘tğÇ)ıÑŸ%£p}L¤ß–áKKH8Ø'S-üÜ GëN~Ş¶|ÒaÿKÁŞ`xzŞØá‹µg”ğåºQ
ÿ®z‚µÑ¢ )I¯, lvÍoIsŸÌóZ¤¢ß!j™38C)şÀ!øÕ«=®‚Q;ØRİBMÊÖqE6¡¢¾ÂfH28”¤óñ˜ˆBØ‹DçuÀlPÁû«hnä€Vª|ß^¼q°=ú¡<¼$|³­6°‹R$1Ê(=¯¾ ®fyâÁ"¢Ö‡âE:<°KâàOÑ£HOÕÀ©[ˆîÖk¶j¸ %…ÂIhĞ£Ë’ã²m-uîğÛqğ<Ø	÷Æéë€ıóf`SÍ¢ØRıkí«×ŞŞKlïvêg©¬‰{$zÍÏË—M$½ÁÜ6¡\ËAæZk¥¥•b£A]fÖ…{3äEÚRÜ`5ÁŞS½S˜İûÅ¶.pá;’O ÑlX¶UÊn!Ü%í¸›ÕƒeW,¸2ş²G³93Ù»7œqòÙB<ªeÀèiÙ”T™0ÙEGl*"şŞ7håbF&²¤ªk"JÁŸtáY>OÇ)õ>H`ª`8fÛéÓhz£¤J¢òC?Î„Z€Ğ¨å³ç.ÕUGYÜ²£jÌr®â>3«Çl§?†jQ,²óìêxpYP:`ıp)€üi9|R'¾K³´¼¥#çá“/İà«=ÛæÁƒ0:M8°Şˆé«ÑØ¨©?œúÓXG¶™3šv ÏT‚§Öè=fY.óõrË­—¼j¤ùú ¯bp^	ÖÏëÕ`´WQm7¸†ø•”UŸ„ZƒvdàçÖ|²´…Â¤¿³è1¤ÑÃEAÏ¾Ú±ñ»½Ò1,³|Ëgç`qö“ÿN“B~½ë]šÓÙ 'ct«Û0,õkV=<?é{V*ÙñYQ-Zõ³zÎZbš¥wE>%h:*ÿ@Ç(Ï¼)´;ø—mÛü`]oª9*Sû­çìhë­ËŒ>F;zğÊñ}—|ßÑ§¨èwi<¿%ğuånb’„¼ù	Jwt9B™ƒh\TV7ùÎ˜ ·;Vq¨3öÁŞC%‰ÿ¡´õ¼[qv(ÍfGŠZåÙO?·:
0Í•?•“Œ½¤A«ªì.ò÷úD¾(äÁë‹Z¢Ç¼ÙÚ^i™­ïº·½¾-okVl»Õ]r›û[\Ëö¶u·´ÛÖ¶ØÒ¶İÎ¶ÜÊVî	^[…u¢zö\vm©ÉôäO»æŠ»ñîÄù.ÜJeÍ¾Î òï½r÷]ãZEıósÊZÃ/AB8ËÒŠ`'^ß@Ç¬*Zw"+ÒOàÁ‡ß·ùW\Od[¸Üm³ÊŒÁ–ÜA.*©n·ÂBH<vÈèoSÒf1‡‡šdúµ±"KfW?@j8ïß&°øiÎRl²¡³c5†bğıüúïÉh¦åira@\ÛªôjUo£ã.áğ¸¬­M©JJR†+¡¸¿‡¶ñÆTÁdFbôğqÜÙÆ…é<ëÍf“4‰	Ñi¨~ê»¦„FĞ(ÊÈ¯ì­óÇÙ8_š& “±{7F•eÚ&åÈÂ8L²ò”JWuTu±2Òƒ[éQ2›òÅèV¨]É'Øb‘eDó¿*á‚†t“N>B‰Ë
e7Ğmš0hvldutXŒ£Qb®\®Ş0ùÆ´Y£:²,^hb(Àôîy5Ëèh’DÅOdœEDGÜ.¦×h!*eQóÅ'SLQ‰ÿ„Vnšì£.i ˜¼g†º8“õi"> dúÙ@Iåá½¶Ôò«òRzU^|_Õ†nAÌñBnğüyj›œ
M ½y'İîJt6³|bû™KÀD"ŞWÃ­£ö‰1ÇÉÖôç°n“ÑGd”£)Q¼H;ç(ë¡ĞTÍÈ–?Î	+Ç“ÄªØ•ôÆ¥äwq(lAoş*DÑ¼âÛƒ—/ehĞS	ƒ"|‘XÈ‰ĞVkÂSÕhsCşEoïKsÎ×i§Y4	'—¡øJŸÎúŠ4dıóŸÁRå³cv‹k¬şCr|L@›á?Ã|L–¸Ä"3*ù&½{İVº|g.RÅ’¯cø¡CEî0Ù:QIm³×+öõàd(Ú¸ÊÒ,’÷éx®w—Ù‚¾:Ÿ%IL&’Ş=ğñı{ŠÅJŠİ.ã&KoPrH·[”w^×má œˆ—8àèÎÇÉ[£@V_	7m ºAÇß;6R+øD«Lç¨{”¶}+D”\¸Ä+ëäÊ¡ÿf ±60¥heÚ‰´	Mqï¸œêb(‰jÕ¤Sœ-;VSƒ9_æ‚¹"ªŸ‘ßD³ÅüQCq³A£‡Ä<ˆ]Ò*ä^·oı@k®±¾«<”K@ş_ ØàNüåär:u«%CL_3|Ñ7¸â«h¦[µŞœ<_H	Oğ¹ó7ŠÄì7‘ò ‹Šü»k…ıÀaÉ@ª.`[Y¡ZúÄ«WÌ>g¢g‚ôÑyª# àONá~häÑòøÅF­/*$ÑŠ=ô÷+·ÇÑ°BQcÂè6ù†®¦\	Lf˜ÙÈ,6Èg6È–û¶éìBzmkgl³5Bé½4âòHCü%gkÅ¼§Â‘MyF§!n-¿SÊ`x%ÚdC:MÀ¿t¡Ø!:ìZ@7`×1áC4#Ùº¹vDu Qy{Í¾§Ø³é÷òæ/J¶ñ
oäÅÆ¹K Ä_- FÌgfe?heúC9ÌâUûD;+²‡ëEtÏ›¦+«­±µ¯a¶êì†‚1ËËê‡”f1›Gél²íZq ‚WfaõÌkÍÉ ·±`÷ºçÌzûé|ÍÀ5+ÕG.3Ëª2:›
KDÙ§¨ä`é/WãıÛ¦,+Kµ)ëB—oÉ6‡q–¢UÜ Òò0*>šßåÀI6BèQQ¨Âæµ`Á~Ú‡ï×8MÈò_US7!\TÇ°”¾9Á£O°%T=Øéw]õäAÔ	4XN@¨™\’–½OQ:Á%•î)¬G‹b·ÖzŸSéCÚVÆ¢5‹9¹J3~µKî†ŠªgÕ³Â¡Æu‡÷±û¨KTê®¦Å1%;İj®‚ÇgD'ŠÑÇéV;N	;óÈ%ÑWÛ¾Me°"}ú–õ8nU %+bN[o£S.ûYÖûI~M*ŒÃY^*¶¸ºc©§1™ÖgÉİ@ÚS´òs“øÓ<OeÚS±KĞ\ö11ñø_]¦|Ì#»	óhÕB|>&ê e“ÌÛ¢•ß=ßÅ›<é€ŸÍ§uÜ&‰7y¬m!·h×™Ôhà¥NQô—ÔwWŒ˜¾Ğ6á€Ø˜‚şP™‹ĞÏ§zí@M®èB°–j	ÈªÑ¼Ì²YzG¹Íü’sM3Ö¤^­ú!†Ôt}C’)ş&Ë‹¤çr°Î¾¦¢¸bÊ{Chü!z”½DC[’Ö>½©ÄïÜGNãqT£áÕŠË—>²³ÅœÎA
|•Å)ˆf³É}_ŞA°'º]R:n»Ì22¯¼òÿfy÷³V÷dıOÙjY>°-gzû!5g9i$ôİv5ªzjb@§¶±rñ3å;'F–­g×¶Áë*¶‚nĞ’/5mÑÔ\jkP!å•Ç±¶YX³2"ÊGØícsù«¶ÈpºÁv‘ÆANI=ÆË´õĞ€—Îèaë„V+O÷v‚ã~•<)c¥úQÈü=ïP[#íĞ•mê­éêÏt`uÈRñ@õ|õ ¯–è`a¯µ‹KˆZwÏM˜š"óª	Ú[üó(Ê`ìBš¢p”Šû¬‘˜r©âÚ1ó;ÏÄàåŸï/¨§x{1Ï$²&ûUpızo¸~iuü^Qz¹MúÖ4ÊH¨ÈÿôõU)17œ×Iï!|\çÕœe±`HƒŞX~æ3˜BËÌuËGéæ İ&±hé,Ïö'ùè£%ŞFD¥¥€vš£“kãÙó–nb~P¾QnòğP[¾ÕBŞ5,âYr÷Se—âEL›ìûìÍ8'Ë=XuÒôÖ¥¶TØLË¶…{|
«BøÊ¦MÎ¤(©%U*¶ğYHLJ!öˆ[eÁ‰0´L;x8„ücnV$Êü'Ş½¢ğ˜=ıåËVFhá]CÚ;Rı‹tï„ »3s5PÀæ“X«.ÁV?4i@ãÒÎO’AáYN8#<ÃîÃEä™×ÀÜÑ>GÉ?ämG"ª+wÜ6Õ¥Ä` ‹‰qd«ˆn'g0Uv—ÉÙŞöDÜÕø¤H¨T˜h‘;´Ë©ëø ¤)+W#ÃT	cZæ<ùâòÊølUB{<nƒEÌb¡Ê5"™Ãvµ¬Â^ÄêÔ¨zG&¢«ÄµğŞhDşk_ù4éÏ¸¬Ñ#Í¨ÿ“ÂÉÒ’.‹¸%ˆåÔR6V6[ëø*%S¹6L€I%'3º:í{ÎJ•YÊ•jv8Æ*À\0‚¢_İ0K7@¯­8¾v>Ù£pîºèOë­µşfñø570kš¥	Ğ¡a¸AqİÍâ­ŒVó0¿­»aµçY=•å~eÆ Óqƒ0›ÆCFZ×šhEa¸LĞ(¦ò<î•·á„PıèZL8EpƒrNÚ¨¥„£"!"ˆÑatD‚1—ùtvÀ#ğMsP5„q¦\²óùà`Ÿ¦Á%ã×\+pÄàút/™F>/˜ˆ—ß~ç¬ÔRnéæ"·à²Ğ® ºšµ@k[n-½ÖØvKÖ¬åcTÎXËƒ!É"«`5§¿üJ-è¥-Áå(šŒ8§Rk9ÅdøšÁÿeçWûÒ,¬J6é¨å¦&¢ÜĞ²¨Ñ–ë~—á«|ìl» ÅÂÙRm<O
t–•sı÷â8‰…3!Yñ¸n××á×±¼`‚çåKšá®Z¶¬#Ö<u <ö€fš˜6‡Uè»îƒ$ypø)ğ³¹‚.‡=‹8SµÍsÃ‘1÷E­İè=µ=.3²Rçe9bG lÅó¬-0Êaí¸ÖwK®!³ 9è×ÉX>Ñn0Zz—6éohR{µmqŒ¯nxNË›ó…mák®ÈZ !!›”h
i¥i©o*wµ§°Áª4"°e^Z„ÓŠd/ÓïŒWB¿$:w¸şcZ¦ÂàïK èq¶ÄTæ®vKO­¦üêÎŞ±G4€WÍzgYwwZó¨iıé=Yo±ª6Y¨MÁ¹”ŞbËÑ¡‚6ÁæÑŸò™”!Ìtg5kÙ/ZJËºz1ËüAŠï}³£–VÄ S¨PBû²c€³ÆpÁğ(Á ¾R
ú»ï‹íÀµY#lx!Eqq{Á]:¿3µzM7ëgÉˆ.qB8¸U×´ãÊnİ³ÕêMÊ	Ô5€bşÂ©„w2£ICÊáá!˜<ó¨5FL,ŒÌ÷³?‹lqX|°-úQnQÁ ‘íémà±*)àl§I~ËÅÅmT&,Íœ†›]f|Ä5¨Š6ôËnÕâÇ§Ûâ¸²@ôÊÃd-&DİX\osCôE!ZÛt
ã M×ç>sÙÃ[¡Ñ\?T}0„§Xå‡á,)ÆëN?Ğ^Ş:c‚[Ò0
Ïj4„iU(w	m´ªÙïèxùÒhÅ’N?UŠªÃH{Õ[÷Ğ¿]ı"µàÅiéÇÖ¿%²‚ÂÇ	á =Æ¿0[p8ÉÁC	-<æ†[âlÒát¶ĞåŠHÈqz³ÕD~™ÓóÃ£áÅåñ½Á‘CõäŞI5¦@¬:8¶ˆ…:[“¨œ²’¤-¸,~ËX $5²-_91f¾­·†>E¬é^Uê†LA6éˆâ£è	<ÒlÙjcJ‡<ómÔ'-’mºF	©qzk'FxVÕ&3“¾2!v$LD´›Åë´³y7 M&iİÎ€£~
¤PğA7ıeÜã×8tå–M¯ÈÓAVZ£Ş–£[1Ù.è0)}Ç"`9¯<'±„b¹jî$	×¥p¶ELW´RaP1«	'A°èdhé(¬Ò‚êî±³¡}bâ	ØŸ8,Vœ¥K-±öæòÔíì~ÃÊ¶ˆßº.³Õ/ÍQ2i¶ƒÇ’·tmZjvûŒ Ï¹7ğ-Ë\:iÍ†-?uY©#ŞÂ‹7¶]è•RwtÔĞyaÉ íºÊŠMãÅ.ˆğøOø³¶Ûeø³ôU(£úşö^0 ]7À/ÇAÎ kÏ¹/0¶MÂV×¸qvø¬°‡‚§ÆÂÀ¯p´‰ÀV(î>s6a¶w"/u%í˜İIëÚF:4§ã_B¢zVÍ»ıáe…‘-Z~6)+¬°­y)Œf
8²*¯_Äi+»ooeÛ<ËÙ?°|: <OÒ‚ˆmÊbŒÌ¦í!ğ´°‰à°ÕuËæl#îÙ°}ö6W=Œ­„âàËÚL„Úƒ(Qz3¾lğ4÷g£ˆÔø´ñBv¿6¹Äò¾Zğ´=Šny©Ñ©‹#ÏYDStVÙOşh\ ¹£Éûğ(›÷è	¼g÷ÄË€Bü€ûYoCìÔséMA²Ïã4fÁé™İ—å8{å6$KÇñNõƒĞıgÂÁÏGÃÁÑßbnñ$DdŞ-A®É”²¸|×"ŞO&Éˆh·$€¥OÚşè²^äO{ÔıÔi#c ’±ÁA8_Ì7Á=ãMv?8¿mı«´a›Ãsìÿ İ¿yö_Ï ÄÔw¡†b5ñŞ+@˜lAÀT®˜sHE.ÿz-`Ó (é›6s-“ 5¼=T¢D©’=¾NuÔîÚ´ÖüWP»5dñÕÔXuxş"-›¢­°º~à7UĞg };qùa~2UªÑq²ºUÚ@ºÅ¤¡’Ş´O÷Hâ¤vYWŒóš4T®d{ÔS².ŠÂÒ6ÅT|i¨•2 ²J*@Ôè£4/J>¡ùÂÉ	Z2ƒìÛä»[Š (ßÔ|¦h¹†ŒƒdòT”wMg^ï®&…ûpW+o;“¿t±¥á~–Àe?*şÒz‘’O”hÄ8	±s¹‚Ä»It\õJØ;{'Ç½şğİIï=¦rÎg™D
È¤™•Ï' nsédH$LáR­É˜8gÈ¯aSÈ¦>Éx
 ­¡&ı@eV#áeê>g“*¼©&¶Ğ£Ö(ÊïœED¸£»ˆ—è.$İEÜŸ©nT‰V’'õ6g¿SI÷F˜raKt£«E«*%ŠÑc«Kò9(‹›”ğ—r®¼±ä}2Şj&»z‹½d-¾ÌÔàÄ.YÁÉì~™&ÒmMOFÅR0%ƒš/¸´â*¥¥š ×+Z¡Wøî˜¥•áøø¨Ôhjš5[ëwÿºD<–Ia ¬ª³§ÓŞÜl lÇ$Êl÷•±‘(7Óé¸ê9RòÊ¶¥Ğê7Ó¾	¾°y©m,Ğâr‡æÜpRM*+ÈzGvğ ‘O ™‹¤ šŞªÀÕ4« RöâèòİùåiïìàhxpÒë÷‡'ç?iÊâ:’5XúK•±öès‹;¾¥”ëvĞvİh_ïù
„†|ä#½^ëìˆÍĞzë>•¤-¢ó]ÏoIw¾	ªw8ñØÀíÒ_ïØ[€çÈA=ZxÜ/³á?Ù^ò0ó¶©;©[)€‚Ã´ÕYµ–¾Öšóƒæ?e_Ø”é]¾ß~KGkÊU!Z¹?ˆZºeg§—S$v›QGdHrÛ*@ıK\…@LSÂ3¤ğœ·Å•!ç5íğTJÇõdQpjnÚ¯c@—tü?uh…ìÎà…pOgyF„DÒXìàÓå¶óùİ;ÂGb)rº¿hqLå{\tÄéÛpN’Ó<NB"ZO/¯ıMÊÙ„]‰išp•™†¸–­xÄ€@Û_¤“8üñè²|~öŸ`Ç¦|œõÃÁñeïô¸å½Ê^¦	÷¥éd) ã¤#S¾;>]Ñ«áäøì¨wé1/Yzj-°íBHË<m*ÅÚ>¹ÚD’ı™pı·;KÊ2	WÊåä%r7¹Q¬'´¤‡ÜYœZi“Ê	‡ŸÂz“÷‚ĞºË5ßz“µçÁ×ÕùÛÀuENz„¥ì_‹ÏouñQ÷¾®¥H_†ìG^ÿùJiRİ[¶’ÖR&±–,XÚœ¡–÷H³s{¼ÈßÄØ¬Œ2ß/Õ¯lmiZŞ,Ù“QÖvŒÍ¹½«<†.Z FK<GÇL~ßÜšï8 Phtà'Ùå›·ŞÕ(©Nlì‡%®•´R34ëkh!A³;KGÒßLÁ¨Şø¢ª6‚†È”_ºœÊ)n­s$¨Úir -uN?šs·ûİğÏuş†Á}º˜ÌÓÙä~¿ (#sMît©-)È3üë_ cø×½ÖÍEñßå¼McÏÃİ=híE¸ã¥®‘?„¯ïj£$ÖÓw_c×}û =·óm£[Î¶4®‰i?~İª¥¥:Gì¹§Š®u-z=ªó€Îšˆ/UÆ:š€”?I¤¸D¼³	U–Á¶(2l¯dKÅê‚ ¨r‡½{çr6÷.rwØë;¥âi'£<÷œ|±
£{„kmwmKŒ¦Væ¢kl€4…±H[ï–f3Í½‘¯›F-ªú‡q8õ¨Øûòóî7üÉÜ§é»5_ïş N5±l¦D;·]>Ş“ØJıUO­—Úuß½fÊï²"k¯ì…ßØdËòâŠÀÿb…éîÆŠÑşüuëŠí%ÜMÄùi%Ûü ê„š7ƒÓ£Ä³Yğ{¡,å~‚x.3N.Vu»4k4¢–ºõ~±îÀ0–išåšµ^D¬H#qIàr—KıË&ÔÆ&dúxÕ^!ÕyØ» ¤†ö*©á‡¾	BjzóAèó’ÏY0’í½œt>s=I²/;†£iW©Ö­DƒÓt,óYm¢p×¨Ş¤-™µjÛ…»Fõ&m©ÜTÛšT¼kÑ¤E™‰jÛ…»Fug[¦¾çµ9£3xCƒ¨3¢xsZ: Ù~ÂESæµnß&¥xB8£Å"a¹Ëz³‚â±Úw:R†¨±N$ŞÖùÕì±¦H'æ›e¼@÷ó9åg¦Äçşaq3É£ö€Àş<}LŠƒÛdôqÛ”Z«+7Iq7ßÌ‡h2~ Š.Ò,#ÆÆÛ¹Lf²ıÚ|CÑÑÌÚ¹ÎçÇ™É¶)]×Bi ïÇÛ x……×ş8û”Î7Ö=£|
iÛÙXoª•›/Üô‚L…ûM6P‚/º»cuRú¥5Şæ/à»DÌĞ>ˆy`İMœ
ø£9a©^RÖÏOÎËÀ²¡ÖLµA1ª½o¾	şTi~ÿrf]9³˜>ù³”ãZDyÎú–:ªhå­TcÜn@k{{cc½§Mı¬àù#2·GN<o×zò®ÿ0îéğyK4[$5.A5‰«×¥òí.:Í6×'Ìí¬÷š³õü‘sñ×ë@Â†Ø!$Q¶ãÌ§æ„÷äx‡½+MN?¶ÏóÎRŠSPĞõ&~’?vjAÒzæĞ‰›áËêŠx9U½%³»èÍ®Ú	]a)u¹;w¿”Z…#P]¾ä-.¬^ø+Ü?éüĞø`ˆğåuãÃ*Y¾Å=‘@ºaV)¬¼ÛĞWÎóİ&P÷ÚBµ¹•P¿jõ+›Š3vªß1…ù‘¥m§´SWo¶0Ù'†>)BÁaÂU°Q[+°SšBYÂS/³-¢uY52Ñ 1_Wã!í÷öû+XùŞ–>«½ÁÅ4íY>¦æ5[Rú[áé¨©A-¼Suë$ªk¡ ëY{Úh¢.]¸ït¤‰Rï…Yü±òÇ¾¸Or"©Œ!%,i(§EiÉÛ…^s—z,_BÆŞØ­/iÖI»dÑğkíV;‘|å8Ö™ê]7ÚÃcŸê¤»öE*Y”›Èpûït¬Ãk–ôÄEÄc¹tqˆp¹nzy\õ=‰øûËTr¯°§ç‘ønœ‹²~N*Ã¤y<oªI!
xhôÎªË(‹ói?IâêRê]+¨ê<¤wï”õÂ’FäÂËÉLÎëjnSpr—ÿb?ËR·L•ñ	õW+¸ñóv 5a;¾Y°ù­ï°€ğİ5m—åú;I5W;ÁwğK¥±eã'oAéú²•ãB%tùÄÀ[6ÖŠÅCI¾¿Xªß…,åf÷fq²¡¥#ÃáÏe¡%*GZ$\¬—Ã.‡A-T‰lÃİ1-€¥%©	Uİ~à¾TçH±ÒD¬ÊDI¦¥€DwŠn^ËùüC2™$“ÉaÀéE@V3¥SU¥w^DY9x}„·UƒòÀ#…ÕkÙY˜•f(Àğ§UUÁK-Ø…â3kCî1d&;&„uN¢ìf	i“y2@öıÅl–Î¡;¸Š~òÜ}à¯±#xrE}¸Œ&büƒ¿îPâVß­-²hã-óœİª°İ&¤´]Œ3+É<€•“|MG×bg2AÒWGÈ!6/Í¸Î3xÃ³Ò¶·a"È¯·Knm£öì2)	`ºãM•Ô–'®é XX
ô€İ£”è ÛòF€Ì†óÙT¬	í5çbÀ‹‹|¶˜ı”Uã.´½¤W”ñvS[»Ê4»"r¬77­åmD*óŸ{è†„Õ!?š+™½dØİà2äb œÃqú9‰‡dÀ‹ù×MRkMÛ¥/È$IŠÃÅx,ášx¬ÛQ¸É¹àfËÅõ4É’iWb$üéÕÉàøâäg_oÑQ%aGCß-Ë‚¸ÈP*üòk§å4-yb
ña×qİ„ÂE§„"BØñ<™êòOç©¨6xU"lÙtï•¬DpèÜ±‰&N.Ãÿ…ØdYå›iy3´CÄ˜„{õ*¨@¦<ºGMÜğ§Ş9dãgŞ¡	²¡Ë„`7 7¥‚°iÑéÏá0ê‚rÀ/;¿6İóÁSÕ
‹EV§#/wOcv¢ëÃò¯/l>6·‰¥àşã–fÚ`7 V~ÂK?]ö.†çgƒ£³ç“‡©‡É~cØƒâ²/¾îc%q[±(“3¢ÕŒÙQ¥/ÌK­{Hw¦U»‡=ßî¡p¾˜—iœòÙŒã‘@Ó¦Éì˜ÍÈäêJãŠÙşü43™üâ€±9Òkpí[ğ]>Z”m§OE>;Ìï²¬º]¡ığ{/2ßM8uşºCf°íÃ‹İ?“/ï‹uıóÁàü4ø§xqrôÎÅM¶Ùâ¿–˜i
(¢œI1ŒºgëIáØK7ƒ)£jE±Œ¢®«·ÎŠ|‡£B'j~wQ$%‘”ïŠè\¸”Û>K3vT‹îwmš¸%Ÿ~Hî¯ó¨ˆÏ³>ác§8¿-’(fKÌùõß!İ°bÓ3>S]h^Tšp–”†¼§¬Ğ(ÊúIÆkŠóE(ah‰ŒPÍŸé‹ŞôEM¾N¡;óÊÔ~pÃ¢$ÂÔè´$´Y÷TWm]ù§\,
ãTñ‚ü­.2S˜Ü<I6!zÖ¹;1*…ü´ìœët6u.+ÀY¬PÓcIŞnµ/vi”F|½·…ÇÙCòî‡¬ÑXROS¢â†m|Á›`'üËØe.È¸ìï|²ñ]9Û†Q[	^XçáÌgDğ.&ñ‡èS¢²({¥ÂŒÕõ¨Å³DL`|ÕA»~[¤ÂZå”zÍtÒXÜfÀÊõ?’mh»¤ìYrÇvøôÈò)rƒzÕ’ÿ¡Ü!#@i›{iŞËrE’¤¯o£,K&då!ÊÂirQâ}"ƒ#Ş¢Ÿ-Á8Y·v÷¾İKù?ÇQ9§ë0#«ñ1‘^¶zşz»n1h† ´†öÿÍhôÖr‘¾ö4šß†d:àÁ/¨õÜ*VeÕİ	¿“U¿ÃB¬‚WVœ¿ŞÛv¨ä¡¦³™M<TQK9mõ«oÆğ‡1‰H.ã…õnÉ ºeRåsš0 ßPœmA•,tÖ¹B·œ%™Ã¤™d³6æ€´f@ve æ;åå
õÜ¾¹Ÿ“ÀÔÚ,mé2İ&IÁ{ìààC:–¬§f Î&	(eĞŸçøt1‰ªµ'D•6Á]‰„;ëIW“¦³Òƒ=›j9VŒÀÆ…Åº#jªe­6tóvfù&fjï:€çá*Kò§Ş(¼c›œ2Û]]QêùÌ¡ıê±él÷Wvš'sïi•‹"‰E~è1õXst¼±ˆ­Í¯Ú{LË”hÂ^L×ÕÒ4şK¨(†xˆu¢We»
¼­31#?ùç{ƒúƒÛt3Íï»àùóÔ¦•`Û#(ié?„Ğ#‹•Ë½ƒS:«R"øqfÛ.–(§leìÃ±Ñe~g7Rµ[hÇ¢t›À†ßø"^|ğFÂ’ŞÙãØÔ¨êõ´K°¨
Ë6éqM…ZÒ`’ŸòÙÁ»¼ ¬vëÏÒN/&¨¨Û¦#„GĞ
ŠĞ˜×ÄÕÆO“şßdêÄr$Dgbë?µ’ì|›€Û(Úì¤3Œ'*%¼Ã¤”ıŞG‡ÛÆ©@xV 'Ô¬ƒ0OĞƒÌè6¬––DZ¤# "@İ¿WT‡M‡Q”´­K/¨»b^¸ÆU Aè¼ñ*C:¹É)‘¸¨+ÖÙÈ¤>šDÿNî{qlé)Ã9ìZ6t‹&6èw`\õex{(„ÙÈÖâÜt¢PÚÌ±¶qS,šµoŒ »Yç>§óšk$
®ÊjAÂuªz^çÎ„’«u!«{-©ÚÇ8×mÉ3ÒyšJÏğ2‰/“²[…¼ÆEıİ¶ş® ßÚtpNó;0š-ª¥¦¤ç¼å€L‹¼<ñ+S›—¿‡¼½æ$Ó”ÍiÉm¹âd“|è†ƒ½³ó¾¶mfşlÜX [—œÑ­š„uì™‡}BM8¬¹D0]IÁZ[2õïa§ºù}ÛV@ùlG†äBZnE÷=±¸¤¸³âX+›Ù¬ôz$¡¡€É4ÿ„!èc…è5PÑ­¢\œìLàLz9¤\J?®40^ÚÁ=¥±+*H'™¿¥0 :š¸qò¯#.ÇTsÆ+”$œK“&0+^LÛ>¶§­:=¬µÍ˜ôÕóRbåí·Vn2C¸}Äá£TºìÏ•Ri'ŸÏÇ¸R6ŒÛÚ‰!°AKGùpÚ|^ô.ÍSFù“İÅ-1B­¶ØÄ]&Ôˆ[‚`İcÜ²şò;ÅDd«…ËšùŞå6è®ùa/‹SÖÚß†áé™v™vÕI²UËÖ›ÊxÚ@‹PgzØğò¥LÎI´ÈF·¢Q¾ã•åeUÁE“hÑ-W(Ë|©íYÙkœ÷-~6 ÔSĞs5B¨»a½Áúémƒ¥Mnwsb’ÚŠlrVº·9E6Xé¾÷§Ô³“M”-MæMx] ™ôµzç¢yèœÿNs,ğ3“ó"NÌÀgEB@• ¶ŒI#Ğ¦D¦Á‹İê#ÁHKlÃ0¤»çl†5Íxdµ¥ÕZt¢AÊUX€¥ˆ(Çïò‚èîÆa$ìàµ½–`¸ìRWpÓ˜ß¥Á¹×Ş»®5™ “¹&×›ç¤	¨İmûSéŸj‹öM
ÈÂÚó½ÒÅohEéIô½…ß¸<_Ä Gt€#°œ>0¬‘o«4’¡#˜J©£Û];x 9:=ÂÎ´•¦ˆÎuƒkÔ¯Ñ5F;uÄÿv´5•PíM&v]•–ZŸA=ŸO©]Tx™ıpiåêªĞEµ™XË°µŒjÔØVfW_ë*™ëbÚöÌº
“ªƒ¾û¸Èiö)š¤p:rAğHS4Ò$fDŞäMòäò×¨4ƒŞNÇ\¯')³;!û[Tït˜
ˆ´dH4©,¿ä­µlıBçêÑ|X$ÿX¤E2œQğõÀE'‹´ĞFçFØùKÍUŸÑ­ñTz¬ŠÒğ7îlAŒ´¤•ÒR—„¾_®ª}×óƒ(»DöãÎé ëöï÷ó¼œ«c¼š¸\±½^ä%­Q`ğÉ
ö+T@óq 0kuHÙs©#TS®D+ŞÿèøgHà“tšÎAã¹Mbz°Ø¿M’y˜Ï’ŒbHVw³?ÌíFºƒ§~*¤Ç£¬7ƒôßø¶Ä8Ã46“"Z%Ş6R8AÁ%‘÷»IÇ?Eé|ŸğQ2O³†}5ƒ[3f•š]aû÷%˜8Ù,¤Óä4LÒR÷;Ò/À!?c+ài”®,B8…×/28ÎÛİÙÙ9	şÔ6fh3 ıY’Ä‹º&Ü_$Eš
”;²©¾¯¡KTª+qäEXT sZ’Ë¤Ğ»Ğ'¥¸õdjÃJã5Ñeãx²ö¡I¶q¡Àkœ{ÖÔ8áj­‰¿ú³ˆF.°¡ë|Ÿq›ú­s˜/ÀZ/"Ğß‡FRd„[wwÀ©ww'ÜÙ!^4J`aİú¯ÿ
wş}«lmÉ‚Âä<¤ŒÜÏ’1©„/©å;à´Óeu'IvC³6!¢Iö/zgÃ£¿œ\õ<ªş’=Şñ(7*ÁEå6ƒ˜…Âà.ŞEŒuæcÜ—l³mp]„üPí2,¢»°„®N@RvUáé>ßà,¼ÄQ}ƒ‰b7‰ƒWÕW>²ò÷í®_ü³¡‘Ô:[ÿ;Ş²øZ±†ñj+ãÍ¹ı*ÊÄeJi¾ÊeD1eÊâ“„ÙCˆÛŠYëì•¡.n3F«4m%~ø["¥·Ñ×]v‹·¬=qŸ•|¬KÌ@tP%”’]}-ÿêb<!áÅ¤@Ÿş $ßåQ}¤7Íµ
ÜÎg(ÂƒœıûZ~N£	ûÛ¶4S¤4ç
»?*Rœ.Ò\dH_Œ»ëCF±p‹úê‹óxGy«6z°,J¢ˆ¢k óé¨]ş”±îgùÛ-ê
±_äw Ö¦%w1»*Òù/Ä´UèO#
HÓ@Qap¨åÓ¡Ğş”\³bçD;:†jßx$“¡´B0×u"W~ˆÊÛyƒ¢wÄÿÚÂ¥Vıï[nR‰
N, ¦äõ<µ¥K×g´†¾pòq†M›D%H…ğ[åX8ÉÃ»äº$rÅ†r`ÚfšuƒJy¡2‘CGa lXDSÖ·{²P‚5Y.îr™Wˆ ÀÉÏxäª˜tèfagÚ\Ç€i‹ÑBöX°SêB€s|n£Ş¶€l¬…3Îë@æmF:/’CT±Iç2²]5”[’L¸ Ğ¤=†h±úTBÙ`×‡éØ}?}}Ó¦³k™TãDºà|XÃw-uAYú}%My÷F®f\sô™ÊÊz®qm0Š³< i¸ªBiŠPO³eeyaˆT!²]ÌÔ0N‹)\µ…{v°¬{ãMYzZ©Îm 3°ş¢ğ¸ÂB–1ëj–…gİãéoÕrç„’;[qÔÙG}å©©/ûX^Z1›NSi•ñLT}Éxğ©JGá)HøMLU&Ò/sµnÊ`·˜¬+¨¿Ñ–sµZ0i¦!Ö=§	QÛÉzÿ­…–a'’ª* ËhUÉ‚œ§R¤Ó/`#‰&äwH&ï^Ènéò‡p„3h¨‚}-çc¹HÔZ‹b‚}jÌ9Ÿ ¡ÈÏ‡°õH,Äü\|bDé-Kp~—a`t–B³¿¬½ŸšÕ±u¡Ï)¸ÆÍƒ&Õ±ûn@¥Ê¨’½ÊÍÔ¶^8/î/˜Bä·ÆzMœ•Ûg@46ÒÌbÔ+æéh’0‹8·ÂŞczTĞQ>»—9Ñº-: …øëW|Ë§}Çw¯Ôwì:û6Jë)—W©LYã-&vYí±™ìVGšô…ÙµÒ—	-µé´ü¦ÖiıÓ%>=ßÌSÈ¥dûtº(Ó‘+‹Ò31=ñnkˆ¼‹‡ıbØä¨OAbvP¦ÃÜ•S4‰™'Gm{=fëšĞôŞÔ«^dîª2m|ñœ¼k',£O	ıs8Küòü‘zÎX"/<ó]¦i_¦Jww×‚	’:2ïJ:{#°0²“X“ëìœÎrWZÛºÀx`ÌSÒÂ<šÎ4.Qf	S“S2ä³tüšéÑƒišÎb9.ˆMì'ToÃ 9<h™¤~uöB´hïq&%ì@0{¶t¬,ƒÖø+öY	m²ô¤4`c&­¶nçóYùêåKP±^nÏ+ Ïƒ-|Q±!yóvşŞç1ã¼˜FxØŒCÜƒİ<Æ·¯££† j×n?\P»Óz´ –s´ œg˜ZØÖo{|¬3‘¢¹®˜Ë=îñ ‡vê­Ù³¯Ğsq›	´	\e‹kª=VÇâ×8€sñjƒ†¼®_T#¿Êâœ¦fPË"O±oõB¾vlòû?í¢›7ˆÖ(I££!Ï3Ç3ìÏÏ†Wı£Ë³ŞéÑğàüâøèĞºãW¬› ­ˆ×‡^ÿÃ "ËjÑZ©™“ã³¼mTÛ#ı`J?NÓ¬‹’ÙQïùÍ+³{®|1‡;(‡T‰ÚûÚj ÃQœFÅÇ^{Œ Ê¾šâÕ¨IÏp(ÅÖˆìFsWŞEUÇ‰?©ü†Yé¼¸ÃèÕl6åVB×!¡Ş{ºãIép:Q¥Æ_t!Q
z>·{µó‘¡U„ãA6Ë\ÇN3<&««Ëhô’zYL-AŒ`ºXÌ“øª€ÓÕNÇV ÓuõòDTóâŞÂ0ö·ğ\i°(Àñ¿:ê–‘p{”Ëxr»$s#ù@´`ºJiW|<><Îó«,å1~Â«ÔHQôàqÚœGÑ|tt„-50Ò¹ğç]:INÈH%Ï9¢¹ôi´N=L EH·Œ¨Ê4'¦çàÂô¿÷®·[Wƒw/¾İ2Ä[3$ä½ë°—‚çëàM°·óµ´F R»šEd’`}¢Êü¿ÿó·|84¦FzÔ)ú?+k\)e»"k’²ñiæéó÷È!$Q¶»üø¼zò"½GŒMy¤YÂ²y:¿WÚ|§{œÓ·ş6ë*@İ@<6)¥¹š¶Å0ë+•Z#â7ˆø#bÖ´W¡GÔœ¼ãq2Sái¿_»2ß¹[µè™:TÏ–E¦U«&r95!¥_$Höæ”nõGpˆ¢r,†aÊş¢ñ”s@ñsOj^À7œ¿Ğ
Y&“ñae*ª¥øN¢/Å8,ÙñiËœc³KÎkğNxz•GBí½L&¡/9ÄtâÙÊ/ÕDÏu[@eDµºß^3šÛ ‚ÂÍ·† R6Mâmæ*³Ã^#<—ÅŞ\Wßu= ¸wÀê8:‘·8¬zR)£Ì„8)	ó`-SZ=«Ly<*íFÖ´î$%ÈîuƒCnµpnlk±a¿ Ø
IPê°ªkBaJ]V»Ç„¼ù¥#ASTÁİíà­Ñ´RZUıvQóÃpk;x¥¹’±9Áxîè¸X[=¨T!Î
vvNº”®Ü^7xÆJ‚4‚¹·ßì%™GÅßÚu@‹lh8Õ29|£éÖOæ½Ëd
1å«L6Õu†°NhêïÅdÚ¦*ºén^^ñ”~¾3H¿¥¶úEv—¤Mäô; @\»/9£6õ÷™…ËªxG]áuzdÇPNJÇïˆ»øcÇëñb?,²óìêx€éÄ;.£è4äò?¸æ¼6Ö–!/îÈê“Äò’Q¡³şX Jì®ŞQ»õÀœ™¾ÀØ6¼–.TYTšszÁ]=$Ş1ááÕeíy—ç'çgïùhW7p8ayóø…è¸?Y€Ÿ–¡¡?_ºÁ×;®ËüjVW9Zj“&ÄÖ;kM•Y}[İÚŞ×tïíÙ1{·Ì º5Ú)OğÒlÖs#RÍ€á¶¨‹"¿àp/ıãu Z7O&-ú—Óv´Ã±íE÷˜Ûxˆ—¼eU2 Ù·şƒgÅª++s©f‹é5³ÒŞ5›Ãg¾£1c1;T¤^BôV9ï²]şÅ«'P¤-!÷xï„I;tÄÍìf*økİÑÀ³Ä®F®Öfgç3SA¿^ ÔímİÍjëÜÖT”8758Ÿ-&ÂCVN1)®¥aS0ü50qM§j¡ÛBÉ Û„¨É+€?&K!®2E¼á¢˜”.ÕÇÃ—!ŒYB0‹»ù@›äçA*wÇÔ5ÍàHé’FºÃ-'~ãzw^>Z–MT¢¢îôízà}4så¡‘Jaø(ƒJ4—¯U³ÎYqëSU
5]¦Ô²òRE—‹ÂèZ±hoÊj	.´.—ğ»äŒûÀ„£bAW³ÓW¯ èù=\–É(DrĞK¡)3¹zs´´,@ë4ÂÇ4ÍRa©­1eWŒök3w¤ØØúÌ¬ÊÔ?YØ¥]Ä,„çº	ä*€•Ô%ÛÜ	hÉ{ßóFºÁ>u}ƒái ˆòë¿£jNĞºi»¨Jù&¨·Ûöğ„>Ò–“™¿²-Ì+36%2ÁÑ”ÈW– :ob<Œ”K Û¡Í	P`Læ4ÔXÛ§‰É]í¿àĞbªir•‹Õ’÷ş3ÆJÅtç§)üS—Nã@E†Ëe5XoõX^æSiù’¦G‹²D[‘ìÂ.Cb:ø<HpØkÉVáÑÊàÑó
(ÃB´)B§uÉòá.£KÉ7N'¨¤qÉ»Q…rë?e—·«–œ¢ÖŠ¥²~.³xâ@à”Ëg!úø<ãC_Ã(~•M8ò”“á(Ÿ¾ä½û²¦«s“E÷j1‹ÑõKÉä]›U¬0™Uµ3¯*ª|E‘Ê¾m·œ†¬Ú23Ñe–×•S_œåœ-Ş‘Â=İšëÑSÕãoX<ÀÓo‘Nâ£¢ö¯Ş/.ì†?]öÏÏŒëO†5Ü†#É»L{ÂÒa/£3$T	%7_ş§Æ¶ÜŞ±×Üà±G-{hòØ[Öæšì‚	‚À•Û¶„ßåÅÕåÁ‡^ÿhØû±w|ÒÛ?9Úâ„P)AàˆíQ´TœŒ6öºjH³î¢ÏhÎÍY’á½ÂrK•†j™3õá¤Y– °Âí“O‰ÌºõËUÖµSKX-¼ò ¶1ùñªôjç±qı×•Ñ]Ğå±œèC L]à²¿vP(VGV+Šü¸vY³î²ÊN#[6‹‡ö@m AâC	šßJ•HA$bÔŠ°&|
¾´¾q,­úCÇ¼fŠë˜¿]iÍ¿¦ÖyPMmóÎ¯î/B){c—¡Êña#¦k²ÄM èÜ³cèÄÌ*ª]«Š]ÿ³.ÖK¨]ËÂÆ—„ÆËÁã.–ÆKÀZÅÿÆEÿúÄşEşÚÄ}Q¿1ß^Ä¯E¼7½'TDºÓİŞ&k7*È½BÜ½—yöŒv¼ù‘û]/…œê7Ud2]³©À³”°ûGq‡EA…]á NM:Ú–Bÿ~º˜ÌÓæIKJİÕA*ÛÑóeÊÙ?ù5ø×‘æ684°­f7¸»MG·¡Xg`²ìˆ–6)Qw×£Ïìå©Y©KÏïŠ)í¥yÑö„«¶e].nó,¡G¤KœŠÌ ö†DØ^š€ø¿|±D©¯¾ìß#qèÍQgìêm¡„—J‡÷øäûê$A›`È®ÁEUÁw°†Bäœ—çÛå·uòjãÑN7Ö¿²åŠø[éõ5Ò2"â‹NS6ìàm_äœNQV…¸â1 ´5òİ*‚Í:SqĞú´ß(°Â¾ä¾/É¿dØç,TÔ±øQ€(;ïg‰	ë2ºãïÂƒó³ÁÑÙ`8øùBC‚Çk<±!Nÿá¡¬Çgı£ËÁğürxtx<X]×ãÁÑ©akÿËÓË? ˜}&×XÊğè[“3,”âŒö‡ã´(çCê+/ Ñ<ş®÷€§Š%9`±¯Ñ„èo‚¼Eİ›£T
Ìï‹¤üŞºWÉï GÌÎ €ğôøôÆAë:hÓiA©Ò,.C*p›Œ ×MˆüÁJUÂö™õL”S‚rŸ»ó‡H9 ¦÷àsê«©nqÙõ ´ú4š}O1îlŒ¤îû	\$¤){ûæM A˜JÅF€Ãp^wäv…g1'ÜiwÒı‡éjÊ·¤d0	ºI4eoá6
æ&~’Ã›0ªŞ0¡İqI6O‹b\?{%™®ğ7&øÚBKy"xşy¾eä9°/[¹5Œ(HÔ ãOÉ
áP åKRj6´5¢ÿEÅÈ –„Àygë;=ºU¼CI¯ñ)îßğzt6£<u\cÌdÈ,€û/{–<}uG'¬k»üÓWÃäÆõKTõ§çóFXØŸ¨ƒ1$X( ÍX€ÈZYÆZëjbtiË!mQÀ¦ùÀÕ.n½m…‡MƒÑ$7ï•µ[„˜ÃãÔöãÃ“ÆpvÈ§„Õy‡?lâOïgLªSé:„;<ÊíY_ÃíYo^»äß/|4qwóÅ5¶2£«ƒ yú¿/ë|LÚ¨1»ğŠò%ìô¿“ìM½uD‰*’”Ô8Û9,@fge@ıœª €©- ;4…¡}uİq¯®ò#/á 1»+Í0:YïƒçrW<¯>ûÛĞø×tóÒŸk2?®Çè„ú‹‚ ä|¦–†0/©%d§Øaÿğ¢O`
¸ÊÎúá©op(ó°•=Í?ª›(n’İìØğb¥~­á¥*˜¾ŸŸ $$ş…)zÏ]GñV²í°–½ R:e]w\æ5¿šÍˆICœÃ«>ìÙÈÜ˜Œ£ÅdÎ”¼úuyc3d•Ùá®Yñÿ  ÿÿì=kwÛ¸±ß÷W(ù$o­ã}´M6é•%ÙV#Kª$'›ÛÓ£CK´ÍFU’râŞî¿˜@âMP–7ÙÖ<{6‰Ç`0óğ[>áº¬G³È?¸8?îEÏUNæ&+9 ğì|xÜë›p„Âı™€`²hPÎ„O	­Gišw¸·!éw³³Yœ°ŠvF¢—`¼•Ë~ÇÍNkÚj ¼†öM9îóßU,ÿµE„‡¹Â(”E“OÑ&%‹ºhŠª•Z4Ÿ—èg6P#¼“—/Q¾É»qöb0³ƒ‰äÕeœ™ı[P3:¹nÄaÄ•ûÁÚ-‹v•y-ÅÿSYÓX¢NÕn¢ÂS2õÖQKQ'‹bı®»ò2=t`â}óÔ˜ágB«xÎ5 }õ»¬„Š@I	‡·,,Ò¿ÁÔ6WâÕŞˆ™;Ëô»\õiŠöômx—ëJLÙiÀJÇN>Ù‡ÍAÉÇË„³q±\
4›Nn»G=j’ˆ“*ƒ¢Ü	K˜e,à¾q`=(Nc•'Â³Æ¨ğ¸Õ’ŠÕ¥p|×ë¾o`ô¯+šA>KëOÓUJX3[/4µªc¿WY>©=»ŒwOÊ¾’`¡°_¸z-eà˜oò2NŞ¨ıhõèı-Î€&,wä%ÜÀ_o—Ab¤8&
üwQĞ’$rúÂSb¤Lèû%È?‹ĞQu.¿ÈRŸHxÊîm€âXİÙ¥ûCGÃÂ úr¨{ Èâçc{ŒøÄÙ0ÏûæÇ;˜7«ódö§p:K”¹E»‡E-÷¥öå“pI«ùø×JéğVfĞù.îXä)ĞB—_Óç7‘J”ax×Ä€Ül·A„ÒkÃÄ²Š†ğ3c„9´1@™•àaÌx‚¯{Åİğõ¡IgÌLø‘	×Œ0”’Ë%÷€)Á^¹'<ş-9K-ï=\OòÈâ•¢×ÂS%‚-Z§X"Ør(ä†¸@¬4ÿô©^Ùî4wå@Ï°"­Û’_tÕ¼/Ş+wõÁàÕÓSŞ?Ü+<Eljöt¿¯ğ¸ü¤TÂ¶‡c«æl%#©‚ë?¦[®aõ<e¤¬A4«ÕÓ™øØpä&œ†l
V|	`A¾tˆßu@Y5şøë6ÜÂÈ„2ŞRúpM+?’G.uá1lşxYqKã?ªæÇkÄÕ‚šê—=¬à± Áe®É¯ ½ŒïRøbFñ¥ñ:Õ›íá÷fÿ vğû±ß“ıû^lß}íŞ=Y–İúÜçÅÉ®À§ª+Î7ôÿßĞRÑ-ºä—ş¤ádd—<Ó6IÇï¢4º„%u÷J²*Æû¼šT´FÎp“VÃ$ÿWÌ9ïI¢åŞÔ ådj2ÕxU¥i||¤Ğ.CskEstğõüò”ş«’Rºİ0Î.…ÿª®·ˆn	­CH¹-¸9'X×¬Ğè'8$eñÖÒ(X‡KÌ¸i++YZ(×:‹ĞY˜Çå ç7Ğv›ªİ5šë¬ù½²»àÕ2à>¡ÃĞãr‚9K§ä ÎtJı!Ò~poÕÛ1¹}4ÃI*œLH4O!A§ÊÏh™$\Å·˜u¡LeÎlÉPÉá1^×Uµ@9ÿ3¼%ÛJ¢HJ`C›ët‰7÷¿P’…_¼2kS­'ÕG)Q«ü	ÍupEÃ0k/éhMœ81–)1ã»ÅŒ9şÀQd™¹‘˜Ä™‡pAñ¯[Y†.„Óø}´^ÀÉÜˆ8SAUÜqóJ£Ñ‰“ã2¡Œ£«hi¾ëMzÇÚı÷¯:eJ=˜ÙÇx#°jˆï„Ÿ'I¼*A½©¨7±îq^\Ò‚0x&ÍI	å«Sy:˜–ft«óB:+ö_ûcÊ¡¸ô°ˆÒÜOMãíü††Ø>a5ãßµğÖ “¡W‡–^)		£v°ÜWpì"b~=GF“S«iDRÜÃÀØÒ¤Û·Ïf£‹ã~¯='Ó‰~ƒ)$á`[`kl2ñjV­b,Ş¤}s@é/‚6€Fò+iÀ¯”ÉÓL›i¼‘ÖCœ¸óI~¨ƒê¤^ç|ÁF4Š{qÑ^ÇKÔ-ú·wâOk¡1Ïbã·ƒ–l¬·æ[-n—` &€1"ÄIê€kÏM¯®È2 E´SÛbSÿl»Ë±B-,LˆáàˆÉK>×p–úˆ»\Âİò@¡`0´è£ÁuŒ‡,¸i¨«Ô>g$STö®¹	¢9'T2)®3Ôeé8"HcŞ‚-]2ŞM² ñ½R¿–ğ¾Åª5™UZÄÆKzñbUøcî2Îf„ÿå±÷mİ™ñ£Ô®z´
ìÔÔ£Ú˜áç:ºÊ.°y4Áä>‰Ó©at@Ø$w%	Èâˆí¥š«Ù
–îS/¦Óá`vÚ;™jFAJô¢í5¡5qxæ¶Âëæe‹9ø\qŞ;ÃàéDæPJ‚4\ÌVñ:&;ı–§r>üÊğÔé»mShSÌ=¤À±eL~‰_G!d»îLqä-æòN¯ìòˆâ4—äA°Ã`1¼ãùÄ6²RÁ’PçËO¢õ(	Av9ÏØ—sx¶KÂykrUjPÄjM<)‚cYÀç>Y8á>¾D3ÖWÚâÌ¯¶n˜ÄAE	*@8Me¨îğ^p=5mq’»6çµT<¥zªûã_ĞØ¹Æ¹z#Ÿ)Ğ­Á•èà¥ŸùZ¦5‡yĞ›xùëŸMì SŠò-e\ŸºWL¾iøN1¤ïÆ²7„\u=ïïğÉ]ƒ	ä9V!°±a+n‡Ã‘N!<ğÄtÈŠ“¾
‰šØ®®zõ ÕĞÍ;‰VArgÍy'Dô0AAsÛú!–Ïa(ë¼
YYòó™Äy“+¨”aªƒA.Ò UÈy™È!ô–ÒOOÔ£”,ØU´]éÔ‚Ö|9š&A[ÉêŸ„ùŠæÃ~V˜qSÎ‹›£q÷¼wq>;é¶¦ãîlÒz×íÌ¦­Ó	ÓÜhÎA§ş(£iò?Œ÷ü#ˆ‹*60a9êÃUì¤ÍšŸ„áiåŸµx<J›heF†6™(€
^Ú5£.äqò†B'©ñÒFÏµÈ}ZË©Ùí¥r48s 
`ø4ÕÌâÍŒ×qiwªHÄóxµŠ$ù]¥‚"Æ4Æ±˜ó?JCö`àŠç*0ç<ó<í*9›ú5w8"­ä'İ¾Şğ]±NRSKÓKo"ô5LyŠ$°ùL5„á;!g=Iå¦w‡î€ÌŒ?µi•kI935 U±¦¥˜éjÈî@àË|±®ŸsAdåª>sÒî¯~$¼tq4^£[úï“™&º
™¨©ŞV£x³İäã`_(
eMâ“‰	è4şX4ioÃ;ªGã˜•hâ,a  R|Ãè¾Æë7ßv?€/áì¸Õ~˜Ï‹Óìj(JÒ*Î›TtåärVweã[&S=¯6H:6“F³
&U½óy¤d3EwãOÑ"»a/&›pŞ@«ƒ›0º¾É„×H"µ2ıçAvÓ\EëºPKÀÓWo­aTq!©‡¼rÏµQLyŒq4&µokÆBdã¬Û;=›Î:#›U)ÃMéDÂpº*äÁõÿ|ÊôéNkÂïúü¹5'ST-
{´­{`Öô/²ÖƒUZ§?˜a1K£LAşğ~ÜÍX|”FÍúÉ¢É÷9Z±·>Ş^Â S-äÌ”óÇ!Øó¬…Ûò´oà²2JlÍñ÷T™Q»¤ÿ˜-K9ØÊ{]J«x&‚ÇÒ¶ë„ä<ùP-5(G±SfÈlÄVıĞå3JıöEi:ùÀÀy_.	Z¯©ìÎ¥K/Q†4±‚Iæ¬Íê’ş	*c”w½ÕfÙ¼¯´Èe·cx1†“.áª<À8ü/4—gçÇyÆË:–ƒìßËù;ëÖ˜¨áq¹/ó1Ó»ˆ¢7idØlù4	Ä˜»‹h›ÖÙÈE¶(’ÌEÎ‚”—âb­Ì0QµEÕ²¢½«=c”š6¿£?ªlz^e†?V|ıíïµe<g¢#üVÃşğ$Îà_ ­÷Öìæ›TS )B´ÔóñZÓ+ñâY€‘’vÓƒ<q '¢>Nú¸]“-}'DHĞ—<ÀcZèzKĞÉé2¾–ç„°'aÆC¤*\€qØöˆüøƒô,A'ç‰ìi£öâ ƒ^Ü-;)w<\9ãMÓK&úoàt¨Ø‚9¨*ÍË6VV~±¸TĞ=icÅ¤ĞÛKp³¨ÉÑMŒPÄÖe!ÊšF3r0'­74zF¿2ª#í,çD1Í/Û.Ã%÷¥şmB`,òêXM¨‰œõ-yo”±Gëhµ]½Ùµşâ'›/¯Pc¸&pş$hH§G¨œm¥Ç/xøUÃ5ÊæèÈ•“y—hM4œ*
ê‰áVX±À>Zg¸ğv>1Ü¤ƒŞAW1ˆ±ªè°9Põœc¼r†âºşâÑ¨Z­¶Ş®¢e&Çw´æ„æ+¬;m-œÍXVíÈ
Ï¡Ôz U±@HC?æ§®xå•OQ±¤|ãª¢àuC@]×ï}N³¦á%lU…Å)L‹×5’=5Œ} ¢Ï¾¹y#b_„¨¬w2—ÊW#pÔ¼†…æ^¬ùĞ,+˜fZ‚ i}
œFÎ"¿ÙÏáİl¿Éæ· ¸ñ(dèÎDT9&<ëQˆ×Z´¥å«A¯«œ]/||ËƒûÆ,ÃÙ"S’%¿˜°º®È"<©ßAZÌ‡œ=”|©°·«hİb”RÂ4ÄÂ,²J§ÛïN»³óîdÒ:íNô³<°æÜåƒÚ›ÚÜW‹×ÈÓLR6õ‹ªrkØŞ¦äTÓ]Åÿˆl–µH¹M‹,¼ÀtŸ…$¡ÃC‘
g®Ó_Š?(¿K¢’¡±:®íNT&±è7íµ[ı²68© &5«™Y*ò™*Ñ™nºyHLÖˆmÆ°Ši¡4ÃEÎ¾è+`¾  Š:áwD#KK"6ìl“â
EÂ1
æSkşŸN¸òÍtç‡uó€İ‚‚ıFC|d!æ¬ï±3¿qq¹÷‹Ï"Z‡DZf2€G4GÂ-{ED?nCÁr~¬è´ 2^–Ö#ñ…Ñ¸!+ÒRŞT‡ƒ^
v!0–•<¨±ë¡Ç±xX´}áÜÒ…‚±Cù*÷GÎæKVËñv½X†<6oÊÂ_Ò}"ÓBÓ fœ3õ§Ì©ù)‡Ù%ºò˜[‡cmıyyóÎñ=©p)òÃçbâ<Å7¨‚JBPwåû·L¥ã1^Ò—,+7jóp0˜åRÄSÁÔa'¤)=r#zFUÊg¸íèGÂ[óZ‘Ö&-&ÀŠyAİg=	âã}(Ê±%öÚGï
Ò”.á•‘_Ùgø£úÒJ–yoé%\ùfğ¦u…±jŠŞ¤ÌòÇ‘
g bR;`o˜ x©Û6-W/eê ygojÜ`-!»v´K‘ªÃÖÀÑ£ğ:³ÀÂ”ÔEKû;˜-«€YK»âú«Y–hítFeŞ„1„¹¨k› ¬NUûèË!VJšğán©<	¶f=âN€Ù‚ŸP¯^¾”âM¨Ñ)ÜË˜ÇğÔ+1hK9åW!‹mI›,¬iS	ìPÖ¦H¸;ü;^¡¥âôí*XÂJ9&‡[º+€K¡xWÃ°¢ÒpÊ›[…ÉuÈïÒ˜õà °<Wr4Ò+m‚ëy´:ÇäW{h1şØô|ØjùV«˜«Ğ›fzœ•m\Î[Sp©l+Ú¸ÀCÓÀúîz”y!Öå5j¡5Q=Ôj»ÌZËÍM`×eåÊX+~²½(UcÍFí°yøÓ•UU{ˆéêï4Ø £Æ5û×qKÈŠÒˆ…§¢gF5ş°r]^!ÆqÅ¤f`ì8á´	±ˆ,ØNy/!´Y =PØÒHo®TrÆØvÓ¼Õi,ÓAc{†{`zğ½1¼’kr‹±ZT0Â\¹ºT„İ$µZ;úá°4‚¦u¯ª=WyQµ"…@2¶­¥£“6À’gqU^ş’ `Ør‡ß€‘D<ßÂ‘æ$Ìæ7F1¹3í¢¥º sÁªšÏj€WÑÀW!
ê5¡  eMø5µuAZ@éW…¥47Æc±İEÌ¬ rËB½h]Ux:™
°áh¬9éNûİÙ¸Ëå–´vğä ïu¾ªÓ_¥éÄQ¹”á!IXĞVÊºrÄîoñG62§r*¯œ{Ñ=~5±T½»ûve5“¤‚ˆCFq™îdå¼‹W„—¶ÛKÃ¨êèŸĞ4=®—k©YLÙ86Æ„„P*Ãàe¹«¨Ú.æÅvÖÀh8<ªÛ×“9îŞ…p˜	–K£8Lx;#3Ö‰Vy6_şüoJõÅe`8s}ÙÓu#© ´ tQv…KĞàyŞ}Új›=ˆím’©Ş1;eQ	­ì‚æ·Û4ÄYŞBËÅn«ÉĞKmB%£#ÕÁT}¸ÍÀCaÜÏnÁš{CÎ]İºğ­–ƒ‰™Íñ4AşmæHXæNˆ—–ƒ­ríóoÁ©w¹´˜Œ“_’Z«Ëı]ŸöÑ¬6P2<³›xêŠºñĞÛŒ.H“İéÙ°3§³A·Ûév<šŸÄW¬hœ¾ç,MôËiN†'Óíg2mM»³‹ÁdÔm÷NzåıpiˆÇéRÌg¹·F:ÅşÙ„K­Z"‹³`IO”˜·Q:Ú³“¦,'÷A#5ìTƒª9œ˜…½ìÆÔ£»·RSè¥Cd]°’?4^‰xöZÈ)— ühÚóîPQVG›{sŒ¨#MíW¦zÁg#¥ÿDÈ<÷àScAqS+®K×?Á¯™bí4l*Ä€l­ƒ"LiÀlÊ;Ğ0DvxÔ(X&ï³°IæÒòÿj¿ú }ÿ,’‡ÑŞ	=-À¸í¯1ºw•ø`‹‘¯™;:ıq	ËKÃ†G^”cÒtAc6v6a®ö]íè
¥l÷™Ÿ³ËcO„“U¸R‡Ià:6¥ìuí9‡ï9wlrX–~(Öu%ÀŸ3öh:A•ÑŒ`> ”ùŸv`+÷|ÁÎAPh¡şçÚaí¥ÎºÒ,È¶)Ù‘ÍÜXü%‹ü'¿|RüÒ{TJîM¸‰?µò(1ğFí4	PµÑïLkÿÎN‡£†Ğl”»‰o?ğ·Ô}]bC _£g'•ıxŸ´L =Ó ıh<Ği|Ñr'Æ™¢~ÿ%µ¹NÓÔŠ¯øG?ÊæÀg‡	ãù«“~ëtÖôÎ‰Ö™uÏ‡éÍŞv?[cU“ˆÔº™^wDk²Ñ–R#*Ñ¹,Ş\rOqfa%/F#7Ñ.ÕK~CšFÆ·;µüà'Ñ]O &Tk™&ôˆ:rl]j¬:F|8¾„ß†™ù’´ [æğ¹Q»ú1†ÿG›Úw¸9Wl9ßğ·Êï½†0x¼æxMÕr¦hX§P–bC$Éÿ¤ÖP§²!Ú–[İyş¦hkÈåXæ(¥m¡‰L'B?Kö¦ˆÕ…‰‡Éé°hµd9‰ˆ­	,\xí5 şØê	.)~ñÅ<eS4¢5cÒ¶É¹bHR^›Ø”í*òú´BòD4‚r²Õ€ÔöjÍ¡p‰ôZEå«²JÍÓ$ŞnŠ±Õ®•ßhÃø.XF¹dôfØ²Ôêäbüz8G(O“~V{ç” ù(şâaíÙ3«sŒŒkŠkƒ°*"›y
ğEt‚,,Ê¨kBÆÒ4Èizs,+ÏCTõ~íİêBš96¼c‚ÌÑá× q¿'Go}j¡´åÎ-«H3Mÿ/íò@`ĞZŒ Ç¡t“¦ÅBòmr€yS›c"†Öf“ÄŸñ¼+UÃX…¼h
›­í#Çx–—' …Ÿ‡WuúQ¢<q‡È«Ã9íùÓÓ‰Ò:²`‘~p§rUí­ÑÜhíß Œš\Ú„B&4ò3¼¿¡+±·àİóR=@„B>lÔb%fáÃJù¤™ÓÁÆØ*Ó(ª„¦©Íä‡†%7–	­GédC¶¸8	™Q}g‹œôlI=ÊKÁSà¯ jcàg@‡øñÎüQ ,—2§'€²;Moq¤}rüì™Şï¨}o”™€`sÎg8÷dİÕõ8ğäÕĞ)@­¡ƒá›EbğO	»ö¼V/¸xİÖş\{éj?|põàŞE°º=Ÿãü%³âK35ı´ sè`a¾ÁúüùÃ¬EÛê2ÕÜ°?7MŞó¯‚°¼aóc1VÄ°o‘÷7¼‹Ö`¯ w7‰¸Ï5jĞÿˆ•—¼d.yÉKSÉœÊşAÙş?Û×Ö0şX®jÑâ3©*ÔBQ½q<AaB¾¨#¬clîçè0×‰ä›Õ­†M¾›¥IÄrL[×X*ÎÁÌY§­¼ùÒ€ÿ™Y IpæşÒü¥ğ—;¯ÓªVAœ¼.‰ğH˜bÉ›SNÔ\®Åi–‡Ù§0\×>IĞQT‹I½x‡zp¡S‡û’›ƒãİr”n–ÁDSmŞÚPÆ¥a—»ãrÇa\Â0î< AolX‹Ê·´;YSä:Ï¦‚Ä÷uï/¿'ÁåqúyÜGş£÷‘×»n$•+>ğNRË]²¿½¤ÂfÂJòåmHAt-¹º¯Şòªò¾°‘î62h>"Â¨¯É¢DûîÚş¹×ó³fS±¼¡İÆÒoTDF¹>?e±TînÚ\¹5,oÒzeyRô_b®/Ó4=y2Ë´»gŒ$X»nSx«+‰UAÁy#/Ú4æ¤ÅáoƒŠy“R}×IºmµTp)wÌW»¨‘IÿºæÇáfIh|˜¼—óx•{ğÓ¼X0Z¥vŞ§úV¼KÓ@hŸu;}
F=J©«6FàIÑ3WãÌ~İéIëíğƒí>y-ûTrˆ/R;WüŠîò¸É<óŞå	ïjß}KÆ+7CTÎI‘(¢9O‰Mûå®’–é®çÉİ†¬ªÁ~+‡(QJ]¼øfÜí¨W ÁØtŠJc¼8†eÛDy°cû•ÁRÑåMNÒ’ëåŞÔ‹0/r9‚z¡Æ»î¸wò¡%c(“ÿY¹ã×$9‰¿—.­ASŒÅLG¬å>ø@a]İÏ›ˆl²_÷NJØÕÌXº_™§ƒßl2?ÌÎ»pëÓ™.™OA‚êõ(…èX"øŠIâ¦õ¨Ï°¨Móâêy˜@š&¼À¿Ô²4Â×ÆZ½`§×²ğê‹õ'÷‰²+ø³$‘,ê¤±u(IŠÖÛğ®®"Ñr½I£Û®y`hœëÒn7–‡98n²²àÌ¥($LJğ+^•Q?1›a‘Ã_k»ˆâi<	×r:Iñ%~Ï²£a¿/oéÅÀI÷Ó Í*®QÛ!Íä3?RôÂjàºmæÌ€œ¡äŸgÏÊ­:T+'a–“h­=Ñ#•Ël¼Öó,9Nc@i›Öq›ˆğÜ
x¤™1iTà)õ0Ó7¶:ä‰ÿ¹vdÛè¬<İh#(JqÙ¦‘˜‰é» şab8iID¢¢ál÷…}i¾V—¦¥íöÁIHXIø.XnÁš`©‹ìHùä’ÖLÛûÏHíR÷Î¬Z¿–-Ç‘5²Ì,q1@ù>ˆ2*şP»#qõˆ×e.&ÄÙQ¥[¹i*¤(¹Q*IÇÈ¿‘ùÒ¬vR+Ÿ‚ô/Û4ƒiÑõˆ†Ó”&Éç's
ìºİ Îzk<¿h;ŒTN“ó‹rò)è7‚ÛïK…Î:qj™tÆ	¯dç¸ìÌÆØı!0s’d³Îü,)¥lz¼ŒçóFyw¹¶ª¬ÇbRÛÄA!ƒÚoŸÀêà”İÒ‚3•¶2aL³póÑp0»„Z½˜óZbpöôzv	ƒ9:¨Í‡wë’°¸1ä1Ü†“› Şµ©ZÇÃ‹élÜ}×\„œµÆ½Áé¬ÕQTZQAƒj©• §ğš·qw4O	€•§J¿ô©J¹Æ[…| ÆqÅŸa"¬ƒ;¾ë°ïáví¥Ã’UQÖı#4&¢3È¾4Ë $/ od±[ÙfÈÚ'§ÃÉöš Œ¼9âi/uüHí‚e½’á€~ŸAB[£Ç±S6³l9Û„I/pK¬ÒuÊó*Ì‚mk=ïĞ•p‘íù“NJ ã\=íş25lû-÷NÄ½R”JQ¥¨pé.Ã¢(1%D±nØÉAˆ*ã ~ZğÂÍ¬rP5¯®Ó±{9úğv¼¹ó\šíáèC¥E	M;ob«AŠköòËƒM¶[ƒv·?›tÂÏ}y¥–nDXŒœÔ´¥V•Õg‰†s#+œU„;ö ht—-ÈrS ŞzÈeÊ)™¿(ïó}Q­³ÇØibëà%–ä,#Ûñ¸+àEëš)/PvÉC—´|'¥À’?‘•,ß»ÔøyÀ]+Ÿ¢'lŠŞwûíá¹8Äæc_UY™•@G}G°*ly	öâ{‰ÿD¿&Ò°Qh$@/)Op”Pf ıTğŒ5k:×êY]…*{Æ?ê![z–[B>lRï`çiCí’¦‡o,Gx…Iz×ë¾Ç™êu'³áx6=w5ÑÑ=k„½@T÷„‚à=uvMCªñ,¦^pÈó'yì
ÓJ¯ã„›Æ=ƒ$=Ëâªå)@ä¸ôÜË2‚H9x‹ZFzƒÙt8êµw!"Bc—¾¤QÜƒìgØØ[-Æ}1õUÇ»E°#-ö~?³ŸÁvÚ³«âÚ~¢‰Ñ‚Ñ„aa°IS.[á>jR¯+õ‹RtO“`.!³r¿Ğà™DŒÀ©ç c¡N4ÙÅY…?~ì‰ÇDó®Œvá;ãxX˜­)«Ó_ÙêT8·“~K“äJi1…7Ez^ıyŞ¼o>Fêh+ ´ÛéM«br¼z#ÒSs­/ÇÜ&7_Ç†3°áÊÃÉ¯,3 ¼4R®Œ™mâÔx©âö7‰Ã««0i-Œy-“9¹8%tDµÎlxrÒW¨avgDbœ›Ëwß™šã³kcÁZ|»õ«ì2+Ï{wÀ³”êÏ&#9½çÌ»û°,¨ÙhÜkwKkÛçàŠ*JQ‘çlÆ¼æJx§¾u!Šp}^nğ i^½¢M‘Şš)Ò 21˜eaGf¾áuó2Îö´#Qe9%«*§UP›·ÏZ•ùg®àwÏ¦ÕZÍ ÉU­¸µy{0-ƒó¾§V›Éã$[n{Éit•]¬£nC*&û”<ğéşà ©ìÆû™l›zÙ2×³}ªÍjf}¢ùDXØÔÉÔs&· ñ‹Ì¬=iûıÍ¤áÒa”±§V¡EvÍíÁ¶M;ö‡*aSşïp½h­ãì&DØj/kT/f*%¦qC´ã¼†ØÀÉ<\œDIš dËìOt§½“j;ğ—k€¹œ½¨ô'JRâ,‡±¼lN=±LVWâ6àgvE÷‚ú¾Äp_ÂBíT“Hì~õåo-,Ôd¼¹iÉv{!“Œá.ãèk¾yRIİózó?şóËSêŞï×¬j^¾(9T²¬$TIîGZaS1agaØyªæ»ˆªÿ¾ª 	.wT¼îÆ€G7ñ:lv¯¤Z"éJ9ÀlÍ;ö	’òùªC'4/µiÃ»éDéœp™1= &Hwª¼M5GI”ñ3UX.ƒùGj|Ë·®ÉŞ\ETnõû³Öi«ºs	ĞÙ­ÛßÅ½Ïö¬Çd§qJœíU-:rWz¢'Ë ¹r¥å6íVfĞá¨fEÆUp»kj,ÍÌ»¢ìg˜[è¶Ö‹Aœ‡×*Ouc\…*Æ–:#©vº'­‹şÔ¡)¸¿â¿P×“Ë—2y£·XÿÖp
hå†Ä»Ï×¦»¨l
c±ˆ°[ÈˆüìcfËhı¦Àa:ó ¶3W*‹QºÜiz8³›ÒÄiÕ8(Š¬~ıÖ?ªkféVå¦åÊş+Õî.¾ûc&}²š*2”Y¿7x[uº`Õ>Zi=Ziİ×J+W;¹ø´}ÙB-;GÔÏ²®Ûf0É Š0
	®ı zäcÍR±bÅ¼û5—Eü¬@æ8Wğ¡Ğö'ozU»óSÊ *S¸ñ/Ãñ[èË¹Á¶`fÃ¡ŒüÕ[wBH\Ã£¡áV–aX9Ú9C(Ë!kXJöî/äp1i{HĞ%h-‡¯ûy³ÖÁ^áëş2ê·-_)€rìĞêÇŸÖË8XœDKt–± SÇŒ%òåÒ5•L®QúœÌb½^ÒÒAéhàÑEÿğó&X/ˆà/ [†[üb™Pâ#3µä3SÈ«”ùñY‹X×‡»—Ï´:ÆómÍ½gOÔüN‚Ûpc}QıK_ó!]îLI—ÙzGŸÃYgø~Ğ¶:(<_L¬†2«ƒƒÎ£³”[şõ×½o;….xGò!9/üyÜwJA‡ZDĞÂ$ò£òãD3ƒ<z©ùDÁùÌMI5£3È%wñø@„› $÷Xd(ĞA–ÜŞdÚk›^[ïŸeå9?ù™ıb}gáC!•¶~<^Ş­&Ælv»~ã¾l	ûÀxº9ÏÈµ,^pJ×¬â“òSpaŠ§hPƒ‰¸¿Ã¶Ué#©‘æpÑ'*`Áb­g	„$NmA±L­\RR»i»i*
TşuıËŸŒ±~%RôáKÌ¥Š£ûƒƒ|ÙœÅr&ÅİÄ„¿^ôş÷¾‹„!Höy)ALè³W‚üäC >ÕY¦ÅºáÚG¢•ótØŞwf ´%KÚ,¾êãÇ$¶6›p½˜ÆÓxï²Ó˜"‚,Ğõ>[4Xå1xˆ#¯ÀúÅ^kØvÔ`îMÖH] zGóp¸‡6Ï¨q¼]/h²)«ÈºŠNÙôíóørŸc‹ÿù)ïw7Ş§É¯f|/óuûºï«ëËlrü6÷áåîâJ‚Aøé4ºÊé§´¯æ'êçQ(ög°A)â´wâ$;%5]§ë¬_GWş6ú´şàš<ÿµˆW·!ÁÒ¢û9A%FOín›Øj·<Ö«ÁJn†ˆ^t¼Ü&áb”„p(öÛ0wšì ¾[ïÁÅÇÁ N[ı~W3)5Âh¥ˆGÕ†y$ÓÑë jâYkìsö´Û€x[&Üßÿê¶–ßÕî¾Ã })kGªºEÙi.ÃaÅéØ¿Wú¹Rã‡–m} ÇÿJ6;şì¼éåÀUßüøã>0üW};ôë_Â
ãı„×‘ß[ƒpùã~LBZ›ÍòFÿÂËMiš˜ÍhÔÿ0ëÛ­~ïñúšš{uÏ+ÇŞXëëmpmS…Wš÷·K<Û»ÿz ŞµßÚ•gy-”šóÚnƒÇ•2ËÂ/·D2èşq}<®=®Ÿªi«k#*_¥zˆå"ÆÑ.s[.[<®¼ß÷ÊûCån”éÇ}Ò7ÜRÅçe÷Töó%Ø¼í)!´&owÒ°¦ÔŸç·ñoÀs¢¯vĞğPe=W3ÔrËE5ı¤¬Œp€’ÇÜ'œºdA(L+ÁÒ[Ÿ·qB°šÖ½Ôø¼mkÓpÁºX°æ§qÑüœqzóî¡<a“ó}Òz7÷¦İ{iR¾ü)Õ²îSJ½õÁ;Ô[VWşY'ãá¹7ºÆP%ÈºÇ‰ù6’[ÆëëÚ2):ı2è5$œ‰YÄ‡®[ŒòåjT‹i—×¡SSñañ°¢òÔöØŸ’ê8`ÒÈmdÛdÍ˜ä)5Eì–"ò‹AÊÃKpÛgrr9{Ù‰(çªoy„£’˜ˆXU^À@˜FYe© ˜@{8˜¶Ú¶ =‘Ãâ‚µ2êËãÏH#
ÌÖÛÕ¥%™ªÔÃN®,ûó`åÍñlt6tgƒ‹óckHÊ¨Šî­ü±3Ûêq*RÖ®áJb'xñ¿?¹ÃnìGÚx(Iã¤Œ‡”0ö']ì*YX6ËıJÕ¥‰=J»I¦ğÚ+Í¿ŠÙRCîW—C±±GêRê‘ıÜVÛîëh«á­pÑİ®n‚TRì·9ÖÁiéG·!ĞÆd°Ö39ÄXBcã¦ ÌÓ
ƒ/¹RÓ<ïÆ@¾?¤Ôo6wÏ{ç{jmOµ‡ççƒŞô„¢œÚigğfãî_ºíéŞeDP]ë¬W˜fñ(i:ô'ä÷dßİ›0¡v¼0ö—SeÙ*x„—r–ß£ÄèdÚOtÈÜ{ÛÏM	oÌÿŠäd8~ßWÁV¼·cùcúÇôˆT÷Kÿa5ô˜ä4øûHRä>ÌŞñ˜½ã?#{‡i÷8ÕÄ’%o6u­ü<Ûîr=İHO—T>Ù´Wİš \/‚Ät=¬Ï€Äæk¤¾¦Ÿ¡K…ÓıŸM_›ä`KNb¸1úµ1Cå
fa:—VzÆdu´oÂùG[®ÖUÖTWÈ†s¨±kJ{øÈ{Œ¼óáÔsÕ´ƒ¬Ö¼„ƒ?T‘~[mÈÒmW‹¸…ù_8½±WÊwL4Qê«ËóBF.åZÀxC"Eïß›V3—³+ˆªqeKÆØiBÌºnªµgİ_yBœ‡£51Ñs%|]9îÅ·¾|¬wÇ\[3ÈSìÊN O¢3çÑcV#ëdüGGşİ„~ü(ø1PğcšğÇ ´Ş4ò%Ğş°/ÓxSéŠ>s»úó1£÷¯Âæık3ÿÒ.óâ.ÿè.P:º0Ì=]ä– ò¿±Ïü£ÏÉ×NDÿ[Á£‹´Çøÿ<_+¹Uíß[èşB;x	=ú6<ú6<ú6˜zxômø¯ğmĞí¿ŸK’Mİ\²ãó&ş$ßX€z"É†T¹-+µÅåqkjÖ#ı/¿wßuÇ*ÿ1w—á 	v1;úA¨DÎMFsdan³_2xÅšBü%""	³m²¶ÖBºQúP\µÖóê,ğÊÿmLv£ífAş>3ÔÀMƒË”üÅ«ó ¹ÖuB”@»•B1ÛnCÇñÊD)ÔÕáŠ°}²#ŠÖä¼Dvà0K!İ@x/t5ñ”óà3
K˜iô‹K®·Şl3Pü¥ùıİİÉëãí%™ä³B­CµÅ¦ş§ƒÚsø÷G §¼ÉbˆMÒÛ„‰!$»UşzA1CªÓa8ğ‹&Ñì,LÖÛQœFÔ¼[@Û¾ğ"aå0ÔÂP`“è_a}šÕäğ4‡Óéğ\mb7´’~’PÊ­Aàô6Êîºë,L EI?m* ¦íxµ
Ö‹ğW\|>1“_µV`†¹å'ÁÂ‡úsiB‹¥ÏG
R°µœÖ‘V Bn‚iWÇG²$|Ş£µÿ’:	™\JyÛËe4ÏS”N¶0_è.®Éş_7°œ,Ù†¯J-Ú`"—"t`ü`êÇX°yrÑï—÷Î‡ÜZ·Ñ5¢ò8HL]äà’·‹ÿ~÷míÛïä%J×ÙÇç[°ÃE’î‘9Z/Ió~32af[´!'4±.œ6‚ì¦¹
>×ËIT0¦3“0¬8l}gÁ’ÇÀzY;l Åüğƒ´ìDáU°]f
€´Î+÷ ÀĞ,N„Bøù6J£ËhI`T¿@frrx'÷—ğ(İà.‘·!ã)Ú’å­6=¯’ÜïòÕÄ­|™Ä¬7]Léa¤7è	’_µu(KÅˆöÓù!éætóBîFCé-@'#ú­µN?2x	èí]°Ü†ÈĞÊ¸°Y¼¯[wàÀØ‹+²›Vëåğ`/¨9²cø1
ÌdÀ`;ƒoÙå[fy¦F€ßÔóõŒ¿Kªafu>jß
ÈË×n£öœüõò¯†‰ƒ’	ÁÜ`%0]î‘Ê4ÙºxåSvŠ¼€1ÆëiQ.aè¥MìŞ„K0Ójj2/³:E.]]èµAFHÛ±ïàö%Ì¹qîF`c÷›0—š„9ÜPôì aıáÆãxK–ş:-$\µ ıñJ Ñ".ëEà6›Ï˜ş5­O>DW;€•~”"RFäLH©åÄùÇ€@q—Ã!l¾Œ6Çp*Hñ'¡B$c´re§9t‹HèÏ÷éûèqç™•\ıöq²Pè8'Á"Äò)Ã/Âÿ×!;[$R‚€éH±Ë X¤Ôs.HÁØ!ÈóšÖÜ÷?‰4!´H?!“ö¹9'ghÂõ	ëşOÕŠóe°ÚÔóâÀJ@0 ‘Œ½~èjã}´Ènêd_È+š€“fìr	kwÆTñfxuEÊÈ%ÄE`êØ«°€l¡G:›´Sç4ä}x§mË[aİX€(=°é%õ³áaóÇ+²?Öq-i*ˆg>«´·
í«”¶Rm…J”©¬$fñVœ†´MÀíÄø"µ!M/ÙÔ7k‚Ç]‘9‹£vlä4g¢(áˆQÔy†ç—?šúÉZ€rªQ°Ù‘é}”İ0._GÖ&n²²‚Ë2hS¬p:Y5ùø'aÌo¦Áµ'dG$tóİ­4A?¹šúÕúş'Ş×YŞdÁ5›¨WaâŒƒ½x,lÿÂ\†ÏÖƒ©£;‡
6B(„j[t“€MLqHMÖı6%3·–HØm~ÁÈŞÉí‘¦£ô b­¦ƒ¨zÉ‹j@uh%J)E×C Òèñ2ìâ¸vÒıB÷?*ËVŸGQ©¢kçL¥z9RÉVL&·d
³uİŞĞ±Ÿ‘FâäÎÕ«^·˜xCÈ»Æ~K:Õà¡‘´VáËÖ«k6Š~ ´ãJj[ªœÿÒØ…·Ë·ç’Î”ÒM¸Ñ	’~po3­wè>×YÔ¾«]1aä¨t'Í©ö…É´aô—¯Ä³D>J†«‘Ukøø²–İD©¼Ä&›ƒâ 8À²ƒ³ Hn ,µÅ…©—´œÉË­

!ÛièŠk±PÉÄUBgd¿åè?¾C7›·áİe$‹îçM VÑvõ22B­åæ&¨Ó®œ€`oPÙ‡÷®7é÷»dbğçépĞ­Œ&uëwoâLÚ„DXÊ®­j(¡ğK,Ö®&t™VB,]EcÌKŞJ)´)Üì­•ofôJle¸Wğ5jLE&şSUî¥òÚ—!<?)S¾…´¦^pGÒôC×&ñ2¥ÓUz™ãO&0Oe®L[÷¢RêÄa0¥•qƒø(ğ‡yÇÿÎœMY&ÍYçX‹´3æÂ‡?åËè>S„˜r=ü>„$ı›+Ü{pÈ÷%XÒˆdôÊ³¦0©ş>W¶C8´J™z?ëñù­õ¢õ™×BU%µ`‰^±ó36½k’ßCßÒ;b¦ö³xi;Áäİ™G×°>ö/¹ıŒm ÕÎô¥„´?ŒIı¢£7Ô§××ªo¤ô	Zã[$—›ila¯Œ¤›QûÛ“œ…·íoxy$>L)i©5Îô"—EåÕêÀ52½LU#Wëí©’%EíI.>ÎÜ*i”ƒù-ïÈ^'3VR¯›úñªA`j˜·)ch!õ©c3¥?ø–vìû†Òû~6HHdñšÎ-Äº&«´ZÑ¢!á?ÿ•PòhÔh›d¸³]†¡Jà:ñºÖôÎ[ÓáxÖëÌFı>D·´Âc†Céõ{Ó*¥ÆÌ.±µ‚V7"0÷;^:<|È¬İ:ï[^¡Æµa<çq²(ì%m«
{­’Ù/¯\…Tî"S¾û®äîÙ‰ÓÑ°ßG—ˆÖ`ò~O“Ïáì•6]Ì¾„š(#»qkà±ÕÁ©b»›€õªg=êX=ëuÀıo4µİşìøÃ¬{>üKoö¶ûáxØwfİ_È—N·£Û‰–©¢Ìh6*<I`ÒmÛg³qwrÑŸÎú½ÉÔAEs3´š`i.fÎÌ…ÒÃ}iR^.-+()Ì:_ßËh1]“kF›Ëğ˜^Mç=´Ö‹I¼MæaZ?î_‰¼kõ{Ö´;;é·Ng“öx(¹›øÌñI¯ÛïxÌ®CVÑm.l¸Vi!‘ÀnKÅ	*©ç:cÃ:kMÎ¦'g=1¼|€eÚË)Õw>‰ÖQz£ïÃ ßœ°Íx×Í×›aC}¡G´ı,e  &vÂ”¬m®5ãÄh…ã®$2Iïğ•{%¢ È¯Z³ã”4tT¡!o¾OÚı¾B»NMÚú¡z[*+ ­üX½ûÊ#íıôJ¹œÂ–É.§ú|7&l#Z_C ×albÍz%êjàuT£ÖŞ^Fóãğ_$àÉÈÉ"^BcÍnkÒ/¦³¿^ôÓFíû#~“ë;Èã¦A|3ÈKè¸*Ü?VÛ"•"ÛºXZË2È½Wçƒ’‹KMQ6'ø V4 àª¬æ7 Ôvà	±­í»(.hlÕæRìa–ŠÔ„u6|×÷[0”¤Tı¨RuE+UÿŞ]ƒİj3ÿî¢æ~5²Åš?·Qózìvå(sõ¨½–d †
­×aò_¾t…^Ù´€Eë‚¬‰7àèÑ ÖÃ…¿N#Wvb8Wµ#UV¼uÜ ‰šµ ô•]o¸ÊoêİÛ¹ÿ]}õÃºÔÔ­tË…ã6_Ê+#&Iò0¢øtëôt5Ÿ1ÍÅªhŞºáÖ¢©Vrø£Äö¸ƒ+S÷€Ùi›úäûä†•s4jÌ5¤¼a£3bÃÆJ5ÕqÅŞ2½+É©¶aåV ¥+ÅõWáäÂU §ÀuÜiíÊ›CÑ®ñS:îÇíMb@H÷tT‰Ç6–›ˆß{ğKv_-BQSX†®ù›(ÛÆ|Me©e,‹>˜>¤ ØzûïtÕõs=WÁ±û@ƒG÷Ê±u¬»W—ödëİ^c€°Uæî›ºWw¯×\—»ÒÑíÚ¯Õ~ü1'~ Uñí×r¥q~ó°3l\\ÚÆXù8™5ªÖüşÄ"»ñ¦hµÃ¢Æø_‹<½t†Ëæ&‰¯“0M§1·&'sĞqk!ƒ:Ò»ŞŠ.î\%ÁõŠ›/
w—FSŞaÍ¸öKş¢ÖryºÒ´•eäVBÒ’Å%Á´ÂÛ™v.xY»ÖZS±vkiÆ%ÜMŠ;ºt†ë69¬}Dõ%¾B=iğï ROO«áñY8ƒÙvnĞÒé—‹`™‚¤PX/…wî™%Uû…x¨ı Í.Ö4 ÖZ½ÚGßã.V´ŒŸ‚ô<Xoƒådì†tô„?¶4Räı üœC*]pĞQ°Œ¯gpçÀ}À ğC£–°‚é9ñ²]ñ“[Ë‘S]íùK:‘¼Œõc…^_}¨÷õX©DXWDk„¬‚äcŞZ+Cş6L$WìØ]AMqZ§qX­À$úğ A#äò“ÛI•¹ï†S=Ëh	ïâ,LK‰s£”Ü?q{(#Nu ÷#Î¼µı§qdÏŸ›áãF:*~.‰Tú0#ÃÑíu¾ş5Ç	#İ×ñ4†u3L ôUdóLKÊˆ÷‡i;vA¢j$öJk:mµÏLØxzEá§EèÍi¤…C $$$²¤œèñe%vUÅ›pMÛç?¡Ê…´<(#¾½ØhÁ\´Ğœ89ê0ÏD³Ï`ÁÿØ®6ùr¨ÏAš<İÂÒ”l &,mo½?ƒw‡”Evzşâ Á ‡<‡úüÿŸ¾36Ğ	²@b©ˆ–´·ÆLI0šÀ5V`ÓÁß°l °ê	‹LGş>‚€Và,¾¸jtSŞ!ˆeÊa^P†×\ P(\’Ï~úZ±EĞZ¶!3‡rş`µS«-í‚OÈÙ	³¸a@i©cûN$ÁÊ¾ÓÏ®Ø "äò¸xêS*¢;	1Ÿ$m[@P÷·%˜Ú
¢öûµq6äÎiÔñi?·¸›©“•ÉûŸìbÓ£´Ú©ÜÜ„†Ü!Ï¡’gDŠ¤ahg­¢¬¸¸äZ2ÑŒNFs[R3‹73ˆ…ŒÚ©jJY¶úœ@½«%Dø·Öå¯lÁb1‹i¸ÄÇ«Ğ8çë‰üÁ<'­ãğŸÛ0Í¯ùg8Nº!Eá„I:²:´ É–œ°/z,¯Zİ"ÏÒéËçO?´X>ƒ×4Á|Í„`‰¹dIñ¼•<¹3ñ àØzù£¡õ5ö…;‘Eø+ËÙahÒ"³›åHeÍ5	<@åB‹vf§öm9A:˜%ò´™êg,ñ¡;Öèä¡ÍêŒ}9dâR$;?éŠª]t.:ÀÂãÍóqtÕvj’É(3_Àïğ iËı GËé?¯sâ‚¤;ü;¶bÍÉ R"œœX»-ÓÙÚĞ[,EÛvZlK-0Û"vK©ÎÜ ‚ºk›W¤¾G»ÍÊ„+.x5ı˜ÈzV­'ùŸî~6Ş®×¨a‡@ô@-B"ËbÙ¾µ–4éI xœThŠXVm"£Bûˆe÷Aµ3Œ @&Kşù9'Úf
¤ÈËgÏlÔæ)/¬\$ª†ˆ-9l¼…ì Hª_¾Më”i¸³xÉUSšÓ„ÔíQ†?§ÑŞøíW.m
¯^Y™ReKÙe+Á»ßy­Fñf»yQ’Ê8Z¾¹ˆÒU”jR¨Ì_#%|ÙbÊ+îºš"ºš"ÓjŠì«IZ/‘Y¿]ŠDx*èåwQt?Œ>şwMkv­z±5¿jSÕº^ÔVAÑ¾‹Š÷aÔĞ_šÚäô\´üOT3EÇirœTğşx¸¿[4:l/)%k±U‹;ë3V½†ÜÛ)º¨’ø–¬‰¢”*šK-¢ÕqxQ/¾ºõ¨¡m;‚É6=\w(–AÅ‘Àx”6àGOÕFd‰-ÍR&Æ"j;hi,‰Ş‡8¦)@™"XS]İ»0É"pğº3‘µŒUeÏ=•\S%8î)R­êó’úÔ˜vËŒö6IãÄ4"Á®Bø’%wªáô°	Èû~lNÂpnŸuù'!Ğngi³?œÎFãîdÒ¨ÙÊ dït0wg§ıáq«?›t§ÓŞàTRÛÍƒl~S«w?ÏC´ö«E×kÈKDSÀ–ŒDq’ÂÉ˜“6Ãh`èåYBÁò3µë&ÿ6j˜G²É—NÊ×Gå7Øt’	èšÖó°µM™™Læ¥^ğü_t<Ôäş‚ µâèN’`2û/ÔmÁÌ´.1V j:…|¸"ïª ÙGëhµ]1SÈlğÇg!ŞAM†@ÿ)~Šlp„h‹ÖW1üà*Yö>‡RY²O)6	ÙÈàB†ì°ØÃ¸Ç¿böb²	ç784áµi•+Š`TcH0¡6Ió”[ÓÙùp2e) ù.Q¶I²\²(bUËÏ®ç3‰:Ë<d?Z‡TWEgÏ"×)w”'ĞÀ¦Æ·ó0ZÖåÖ©Ww¤™HòÇ¬õP\“Ñ»
>†ÂïúuN€ığŠ˜¥Ğ>«ŸÆÔ,­!µÔı…œWú¦”Ã:`é–pßfA\:aéDeeõ‚Îƒ­ \wÁg@ZZÿşÀ\à4	Paÿ6ûİ“©¥hw¹Œ6(Ì™4§Év=»»¬Ùt,áßv¼$»åV¨Ü/~~ïf¹ƒz‡¦)™l/AB€-ßÀFÑXÛDìZ ©d³=<õ»¿Ì.½é¬Ó5
†gE¥1í˜Ï]×İŒ0XP)ƒWk°z.(ÇGÖZ—>œ·¦í³Ù¨5î‚S›ôéı¸5B#Xü¤çŸŒÒñ´_ûsÏï¸wz6­½¬Ió]ûwş»MÚ·ÒîxÚk·úe{<(ySsÁÕ >aYQMIZØ…9œğŠ ¶ªÄl<tD)TZš:¨½ôl.¡…6 onLyÙ"¤ØÂ ˜œÑ4ÿI$T¹Ïı˜C‚„y(3vCáÃëâ*É,–
0“‘Ú™¨KÚjxaæÖ„ÁØjÚŒEÎxbĞr¼ÚlA™1Ïx¶AèòaF¿©#¦+1 4æiâÀ²±Ğ£Ío¡¯SÖÆ‘%m“ª³†‡å|#½óhä¶f›Ôhı•µn‘…§4ıˆš4ƒí,,×	l;ÖLAÏilx“
œªøïò»Üº0°ç¤°Ä¿ùæÕÇ­O¶Pâ§c(d!2{yælË¯N½‚¯™"ùsFMnåã±êˆ2ÚÛ¼VŒÂÉ»áUû&"g.q‚ÇBZÿå²Ql•³~Û–gµö]á[LZl$Û­[5(vêÚÄ¨7íRŠ2§ß/Dép›áí‚~c@kS±%…•ô¢ÁÔn
%¬³á‚LàŒŒh­Bne…Çƒ—–BSy7¢uEërÀh®ü/W.‚+TvV	ávÙ8Ç³š‚G€bªRå7‹fíh×1½¤~åÚ¾<¯¯—!–óm´F€K[ƒíelrûLïq¼\PµÊ¨t/·÷ÇË:ö†êêÈlzºÛFT› Ñ¨…·²MWÜà¸‚ş`Ñ¿Ônu†Èú şNºïBGÍ·İmš´ºçWì½ˆŒI%¼0s,³X(Œı7—áúcß{QBL›N¹šFI_PB1U	|`Ã5 'Nèˆ$¤Óƒ\kÛ°^ø³Ò»ÚÈ ÍŞyîiÛĞ”6³¿’¸}€H+Í‹õ^¦NĞ±L„<PáZ![·œ×­¢OŠïå?ÌJÕ˜i|8IãÈ§S€r+29”í*³›	ÆÀğ™fIâ‰4è&şÄjinrRœ“SWrÏ·Æk¼XBèŒe{¡ÀòFíÅ¡hSò+‘¿)cô)OxKTÕ·É¦×ÅÎõte©ª’¬+ù§­?":àudÅÀlS~Îäàà‹õŒ·îÙ)İ\”ÖvÌ>£ãw“<ˆÌx]¥Œ,†£92zWı}G§b±„¼H2g >‡ÿ'Ã|yºÊ¨[€Úw	9çA‚ŠËè‰vÃ4ñ¥dò *ĞŒÚ¢½>J¬Å¢‹\BÏz=HÀÍP!}xcÕ‘ãÓUtİÄ C¢5ÎƒÏxY¯­QÃz>†ÎS—m±9>	ó w);¬¹¼PÈ¹o”YI<o>È77ü
ˆuÌY=Pìâ¸&¥ù-ZCÀ¹÷¦aæD»Ä=¶šTÀÖ}–xgû‰â`‰âÿıù¢Q(Rµ*0¶<ˆ©šË¡"ªEåE®”Î•äÉÏ5#‚½Ox°å›¬¯•¬¿k:s±'‚GûCæ¦ t>‰’ğSœ|Lµ‰ºâ_x°a#Q¿¹UØ¡ÁìÓ¡rU[Aˆ¤llšÉ‚V§šùBjrÚ=¨ƒ‘£‰ÙırÃ¦h@ÏVû³˜÷úáÉ—ádÌC)/k™çJƒ”F¨‡„$ÓTZìzÇº`!³Y˜lªˆjê!í‘´=Bk†“š‡BµŸŞ­•v¿7â™®l*GB•‹k?ÁÇóA{…Dñí
°ƒş\{Í©[
ƒõät¼–‹æ»îxB0ĞœtŞôLk?×¤÷3P8MšÌÌƒ‡nğ ÄQN¶›´‰wIIåZb(q*'@rl%I€ºŸá–ìä5Ã£î$Ä.?Šâo8“Ü­=:å­AXšhS—²=]€ì¤÷å'€ˆ²O¤jğp¤'R:»;;Ñ6Â3íe´ÁŠ“âr€`¤Í”G–,Ó;R°
ÍÙ1³ÜÕ1ƒu'q©>Ú®YqvK²^ÀEü",iºá„ÜŒºï¾»òjÿví]jå4Y¤²±}ĞàÔ"B)øåË9IC½_fù.´÷ü¾Y9õ°™"? ’ò¤«µ€É¥*.©*Œ´¥›%e?d1äSêÖIZ¡;Iˆ .î¤ÖÂ>`­˜ Ã'^6]óÌ²Œ'dõe+^ÜZ¸ø˜ê´O6:;©ÈHŒŠ ŒL±Ğ˜…À£I‘o›X’6x-İ4X…ßf‚Ï¿ØM;´”3f§>}FÏ>æóƒË¡BÕ«iôåã_›Ëğ*Ãø‚¨úÜœ“ÃIsV•`ZI¹à|¬6u©
z€Â(míB~tÁhS5aù@uGEa[;	p¥Š bR/¹	2Z›€¬=Ó¬oT†Ú¤ÒáwßÂú~Ü	/·×¦u]¯·²PÉ2Á•ß„Â0¡Âù‹ŞöwºÇ§³Óq·;˜M¦ãá[-íØ·ß™4;iÊ±Òw^ãv©ìÍk£XöW]ÃÜ-éx{±Z/ÉøZ,à:»H=ng¸™·ú£³–~K†®gy5rüñGÛ}¨¸>Á8ìÊ|ÃIõ^|Í–|ÀÙÊ‹“ãzÛfò:öfæUj€7%ó“«76÷ñ56bÜijò†Â6+·i¼1.ÃÖáÑ£5ëú9xüh+Liì™›Ä2°¼¸§dP,¢bõlŠ]eN7ÍÓÁĞû=Å-7à»@\0·Ü•ãŠ¸œéEa„µh:u×æä|DM;§«@öêùT9p–«Ô;ß‚lwÎL-ú Ìëi‚¬4:¹#4³ÌO æ¸Åkô YbÙ7a<²†À×hğeZBğ—€ùP¾ªùD}ç°IFğt/ôºˆÛZõV!Ë6€+‚F{Ô’îByúj•ú'-`»=¡±U¯.Rg6-,§Ôô¶@¡z?âÌ½ÜÎÁªK:ï{Ê2Ä¹±JÓøLÈYUŒ¼Úp‘$æmQ#~‚À »‹ë°{uEÖhŠ¶¨´~YóÇ£š˜Üh[^(G¥+\bt,€ŞºµÙpê/dÅßt¡™Wš•–|6é
Ttd SÒx"dé9÷&Å6D Cq{E aºŞ‡—Óº–&àëÕ`ªw!¸]@õv¼‰È†^4ÂjÈJ¬^Ü|‘ŸÍ9²g!g¦¿cò.Ì"–Şâ,,´=`Õ]ˆÆËÔŸâZBÀà&öd& ²§Ôƒ¼Ô&€ÿe0	Ø$!¨äá‘·äë´ÃÄ<7PœÏU-±‚ƒæå2L•Jj¡$[ÊŠè Êğ6Ë;5f¨æ@?cÓ©ğ²èEømyˆ®ˆŸx®>Ûø%Ñ(c‡ye³®>¸L3HÙMÃ`Ö¸4A$;‚Lz5~&Kg‘ÖøKÎú07°¢ÿ+Ö(»ƒ5µcñî7ùk %iù>À™°p=–Ë%\Np X
‘©»!ĞOÊN®µ9;ÀÊİ9êÑ
Z˜W[Ïd>Â]z6Ô+ï9ŸÍJƒoÔşg°¥ª[E#ôbnºlt¥MÿúÍ¯ßü?   ÿÿ f3X   ÿÿ cş