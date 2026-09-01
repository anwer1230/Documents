/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.dpf2;
import static org.telegram.messenger.AndroidUtilities.lerp;
import static org.telegram.messenger.LocaleController.formatPluralString;
import static org.telegram.messenger.LocaleController.formatPluralStringComma;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;
import static org.telegram.ui.Components.AlertsCreator.createClearOrDeleteDialogsAlert;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.LongSparseArray;
import android.util.Property;
import android.util.StateSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.math.MathUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import org.telegram.ui.recyclerview.LinearSmoothScrollerCustom;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.AnimationNotificationsLocker;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BirthdayController;
import org.telegram.messenger.BotWebViewVibrationEffect;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.FilesMigrationService;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.VideoEditedInfo;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.XiaomiUtilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.messenger.utils.FBool;
import org.telegram.messenger.utils.GradientProtectionDrawable;
import org.telegram.messenger.utils.SearchTextWatcher;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.tgnet.tl.TL_chatlists;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.tgnet.tl.TL_stories;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ActionBar.MenuDrawable;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Adapters.DialogsAdapter;
import org.telegram.ui.Adapters.DialogsSearchAdapter;
import org.telegram.ui.Adapters.FiltersView;
import org.telegram.ui.Cells.ActiveGiftAuctionsHintCell;
import org.telegram.ui.Cells.AnimatedStatusView;
import org.telegram.ui.Cells.ArchiveHintInnerCell;
import org.telegram.ui.Cells.DialogCell;
import org.telegram.ui.Cells.DialogsEmptyCell;
import org.telegram.ui.Cells.DialogsHintCell;
import org.telegram.ui.Cells.GraySectionCell;
import org.telegram.ui.Cells.HashtagSearchCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.HintDialogCell;
import org.telegram.ui.Cells.LoadingCell;
import org.telegram.ui.Cells.ProfileSearchCell;
import org.telegram.ui.Cells.RequestPeerRequirementsCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.UnconfirmedAuthHintCell;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.AnimationProperties;
import org.telegram.ui.Components.ArchiveHelp;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BlurredRecyclerView;
import org.telegram.ui.Components.ColoredImageSpan;
import org.telegram.ui.Components.DialogsActivityStatusLayout;
import org.telegram.ui.Components.DialogsActivityTopBubblesFadeView;
import org.telegram.ui.Components.DialogsActivityTopPanelLayout;
import org.telegram.ui.Components.FragmentFloatingButton;
import org.telegram.ui.Components.FragmentSearchField;
import org.telegram.ui.Components.IconBackgroundColors;
import org.telegram.ui.Components.ImageUpdater;
import org.telegram.ui.Components.PermissionRequest;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.BlurredBackgroundWithFadeDrawable;
import org.telegram.ui.Components.blur3.DownscaleScrollableNoiseSuppressor;
import org.telegram.ui.Components.blur3.RenderNodeWithHash;
import org.telegram.ui.Components.blur3.capture.IBlur3Capture;
import org.telegram.ui.Components.blur3.capture.IBlur3Hash;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.drawable.color.impl.BlurredBackgroundProviderImpl;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSource;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceColor;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceRenderNode;
import org.telegram.ui.Components.blur3.utils.Blur3Utils;
import org.telegram.ui.Components.chat.ChatInputViewsContainer;
import org.telegram.ui.Components.chat.ViewPositionWatcher;
import org.telegram.ui.Components.chat.layouts.ChatActivityFadeView;
import org.telegram.ui.Components.inset.WindowInsetsStateHolder;
import org.telegram.ui.Gifts.GiftSheet;
import org.telegram.ui.Stars.StarGiftSheet;
import org.telegram.ui.Stars.StarsController;
import org.telegram.ui.Stars.StarsIntroActivity;
import org.telegram.ui.Stories.StealthModeAlert;
import org.telegram.ui.Stories.recorder.ButtonWithCounterView;
import org.telegram.ui.bots.BotWebViewSheet;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.ChatAvatarContainer;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.DialogsItemAnimator;
import org.telegram.ui.Components.FilterTabsView;
import org.telegram.ui.Components.FiltersListBottomSheet;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.FloatingDebug.FloatingDebugController;
import org.telegram.ui.Components.FloatingDebug.FloatingDebugProvider;
import org.telegram.ui.Components.FolderBottomSheet;
import org.telegram.ui.Components.FolderDrawable;
import org.telegram.ui.Components.ForegroundColorSpanThemable;
import org.telegram.ui.Components.Forum.ForumUtilities;
import org.telegram.ui.Components.FragmentContextView;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.JoinGroupAlert;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.MediaActivity;
import org.telegram.ui.Components.NumberTextView;
import org.telegram.ui.Components.PacmanAnimation;
import org.telegram.ui.Components.Premium.LimitReachedBottomSheet;
import org.telegram.ui.Components.Premium.PremiumFeatureBottomSheet;
import org.telegram.ui.Components.Premium.boosts.UserSelectorBottomSheet;
import org.telegram.ui.Components.ProxyDrawable;
import org.telegram.ui.Components.PullForegroundDrawable;
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.Reactions.ReactionsLayoutInBubble;
import org.telegram.ui.Components.RecyclerAnimationScrollHelper;
import org.telegram.ui.Components.RecyclerItemsEnterAnimator;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SearchViewPager;
import org.telegram.ui.Components.ShareTopView;
import org.telegram.ui.Components.SharedMediaLayout;
import org.telegram.ui.Components.SimpleThemeDescription;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Components.StickersAlert;
import org.telegram.ui.Components.SwipeGestureSettingsView;
import org.telegram.ui.Components.UndoView;
import org.telegram.ui.Components.ViewPagerFixed;
import org.telegram.ui.Stories.DialogStoriesCell;
import org.telegram.ui.Stories.StoriesController;
import org.telegram.ui.Stories.StoriesListPlaceProvider;
import org.telegram.ui.Stories.UserListPoller;
import org.telegram.ui.Stories.recorder.HintView2;
import org.telegram.ui.Stories.recorder.StoryRecorder;
import org.telegram.ui.community.CommunityChatType;
import org.telegram.ui.community.CommunityEditActivity;
import org.telegram.ui.community.CommunityPendingRequestsActivity;
import org.telegram.ui.community.CommunityUtils;
import org.telegram.ui.community.cells.CommunityRequestsCell;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import me.vkryl.android.animator.BoolAnimator;
import me.vkryl.android.animator.FactorAnimator;
import me.vkryl.android.util.ClickHelper;

public class DialogsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate, FloatingDebugProvider, FactorAnimator.Target, MainTabsActivity.TabFragmentDelegate {
    private final int ADDITIONAL_LIST_HEIGHT_DP = Build.VERSION.SDK_INT >= 31 ? 48 : 0;

    private static final boolean TMP_DISABLE_TOPICS_TWO_COLUMNS = false;

    public static final int MAIN_TABS_HEIGHT = 56;
    public static final int MAIN_TABS_MARGIN = 8;
    public static final int MAIN_TABS_HEIGHT_WITH_MARGINS = MAIN_TABS_HEIGHT + MAIN_TABS_MARGIN * 2;
    public static final int FILTER_TABS_HEIGHT = 36;
    public static final int SEARCH_TABS_HEIGHT = 36 + 7 + 7;
    public static final int SEARCH_FIELD_HEIGHT = 48;

    private static final int ANIMATOR_ID_SEARCH_VISIBLE = 1;
    private static final int ANIMATOR_ID_DONE_BUTTON_VISIBLE = 2;
    private static final int ANIMATOR_ID_SPEED_BUTTON_VISIBLE = 3;
    private static final int ANIMATOR_ID_SHADOW_VISIBLE = 4;
    private static final int ANIMATOR_ID_SEARCH_BUTTON_VISIBLE = 5;
    private static final int ANIMATOR_ID_ACTION_MODE_VISIBLE = 6;
    private static final int ANIMATOR_ID_FORWARD_BUTTON_VISIBLE = 7;
    private static final int ANIMATOR_ID_FILTER_TABS_VISIBLE = 8;
    private static final int ANIMATOR_ID_SEARCH_FILTER_TABS_VISIBLE = 9;

    private final BoolAnimator animatorSearchVisible = new BoolAnimator(ANIMATOR_ID_SEARCH_VISIBLE,
            this, CubicBezierInterpolator.EASE_OUT_QUINT, 350);
    private final BoolAnimator animatorDoneButtonVisible = new BoolAnimator(ANIMATOR_ID_DONE_BUTTON_VISIBLE,
            this, CubicBezierInterpolator.EASE_OUT_QUINT, 350);
    private final BoolAnimator animatorSpeedButtonVisible = new BoolAnimator(ANIMATOR_ID_SPEED_BUTTON_VISIBLE,
            this, CubicBezierInterpolator.EASE_OUT_QUINT, 350);
    private final BoolAnimator animatorShadowVisible = new BoolAnimator(ANIMATOR_ID_SHADOW_VISIBLE,
            this, CubicBezierInterpolator.EASE_OUT_QUINT, 350);
    private final BoolAnimator animatorSearchButtonVisible = new BoolAnimator(ANIMATOR_ID_SEARCH_BUTTON_VISIBLE,
            this, CubicBezierInterpolator.EASE_OUT_QUINT, 350);
    private final BoolAnimator animatorActionModeVisible = new BoolAnimator(ANIMATOR_ID_ACTION_MODE_VISIBLE,
            this, CubicBezierInterpolator.EASE_OUT_QUINT, 350);
    private final BoolAnimator animatorForwardButtonVisible = new BoolAnimator(ANIMATOR_ID_FORWARD_BUTTON_VISIBLE,
            this, CubicBezierInterpolator.EASE_OUT_QUINT, 350);
    private final BoolAnimator animatorFilterTabsVisible = new BoolAnimator(ANIMATOR_ID_FILTER_TABS_VISIBLE,
            this, CubicBezierInterpolator.EASE_OUT_QUINT, 350);
    private final BoolAnimator animatorSearchFilterTabsVisible = new BoolAnimator(ANIMATOR_ID_SEARCH_FILTER_TABS_VISIBLE,
            this, CubicBezierInterpolator.EASE_OUT_QUINT, 350);


    private final WindowInsetsStateHolder windowInsetsStateHolder = new WindowInsetsStateHolder(this::checkInsets);

    private boolean canShowFilterTabsView;
    private int initialSearchType = -1;

    private final String ACTION_MODE_SEARCH_DIALOGS_TAG = "search_dialogs_action_mode";
    private boolean rightFragmentTransitionInProgress;
    private boolean rightFragmentTransitionIsOpen;
    private boolean allowGlobalSearch = true;

    private TLRPC.RequestPeerType requestPeerType;
    private long requestPeerBotId;
    ValueAnimator storiesVisibilityAnimator;
    ValueAnimator storiesVisibilityAnimator2;
    public float progressToShowStories;
    public boolean hasStories = false;
    public boolean hasOnlySlefStories = false;
    private boolean animateToHasStories = false;
    private float scrollYOffset;
    private boolean actionModeFullyShowed;
    private int actionModeAdditionalHeight;
    private boolean invalidateScrollY = true;
    private boolean fixScrollYAfterArchiveOpened;
    private Bulletin storiesBulletin;
    private float storiesOverscroll;
    private boolean storiesOverscrollCalled;
    private boolean wasDrawn;
    public boolean hasMainTabs;

    public MessagesStorage.TopicKey getOpenedDialogId() {
        return openedDialogId;
    }

    public class ViewPage extends FrameLayout {
        public int pageAdditionalOffset;
        public DialogsRecyclerView listView;
        public RecyclerListViewScroller scroller;
        private LinearLayoutManager layoutManager;
        private DialogsAdapter dialogsAdapter;
        private ItemTouchHelper itemTouchhelper;
        private SwipeController swipeController;
        private int selectedType;
        private PullForegroundDrawable pullForegroundDrawable;
        private RecyclerAnimationScrollHelper scrollHelper;
        private int dialogsType;
        private int archivePullViewState;
        private FlickerLoadingView progressView;
        private int lastItemsCount;
        private DialogsItemAnimator dialogsItemAnimator;
        private RecyclerItemsEnterAnimator recyclerItemsEnterAnimator;

        private boolean isLocked;
        public boolean animateStoriesView;

        private RecyclerListView animationSupportListView;
        private DialogsAdapter animationSupportDialogsAdapter;

        public ViewPage(Context context) {
            super(context);
        }

        public boolean isDefaultDialogType() {
            return dialogsType == DIALOGS_TYPE_DEFAULT || dialogsType == 7 || dialogsType == 8;
        }

        boolean updating;

        Runnable saveScrollPositionRunnable = () -> {
            if (listView != null && listView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE && listView.getChildCount() > 0 && listView.getLayoutManager() != null) {
                boolean hasHiddenArchive = dialogsType == DIALOGS_TYPE_DEFAULT && hasHiddenArchive() && archivePullViewState == ARCHIVE_ITEM_STATE_HIDDEN;
                float tabsTranslation = scrollYOffset;
                LinearLayoutManager layoutManager = ((LinearLayoutManager) listView.getLayoutManager());
                View view = null;
                int position = -1;
                int top = Integer.MAX_VALUE;
                for (int i = 0; i < listView.getChildCount(); i++) {
                    int childPosition = listView.getChildAdapterPosition(listView.getChildAt(i));
                    View child = listView.getChildAt(i);
                    if (childPosition != RecyclerListView.NO_POSITION && child != null && child.getTop() < top) {
                        view = child;
                        position = childPosition;
                        top = child.getTop();
                    }
                }
                if (view != null) {
                    float offset = view.getTop() - listView.getPaddingTop();
                    if (!hasStories) {
                        //  offset += tabsTranslation;
                    } else {
                        tabsTranslation = 0;
                    }
                    if (listView.getScrollState() != RecyclerView.SCROLL_STATE_DRAGGING) {
                        if (hasHiddenArchive && position == 0 && listView.getPaddingTop() - view.getTop() - view.getMeasuredHeight() + tabsTranslation < 0) {
                            position = 1;
                            offset = tabsTranslation;
                        }
                        layoutManager.scrollToPositionWithOffset(position, (int) offset);
                    }
                }
            }
        };

        Runnable updateListRunnable = () -> {
            dialogsAdapter.updateList(saveScrollPositionRunnable);
            invalidateScrollY = true;
            listView.updateDialogsOnNextDraw = true;
            updating = false;
            listView.invalidate();
        };

        @Override
        public void setTranslationY(float translationY) {
            if (getTranslationY() != translationY) {
                blur3_InvalidateBlur();
            }
            super.setTranslationY(translationY);
        }

        @Override
        public void setTranslationX(float translationX) {
            if (getTranslationX() != translationX) {
                super.setTranslationX(translationX);
                if (tabsAnimationInProgress) {
                    if (viewPages[0] == this) {
                        float scrollProgress = Math.abs(viewPages[0].getTranslationX()) / (float) viewPages[0].getMeasuredWidth();
                        filterTabsView.selectTabWithId(viewPages[1].selectedType, scrollProgress);
                    }
                }
                blur3_InvalidateBlur();
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            FrameLayout.LayoutParams lp = (LayoutParams) listView.getLayoutParams();
            if (animateStoriesView) {
                lp.bottomMargin = -dp(85);
            } else {
                lp.bottomMargin = 0;
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        public void updateList(boolean animated) {
            if (isPaused) {
                return;
            }
            if (animated) {
                AndroidUtilities.cancelRunOnUIThread(updateListRunnable);
                listView.setItemAnimator(dialogsItemAnimator);
                updateListRunnable.run();
                return;
            }
            if (updating) {
                return;
            }
            updating = true;
            if (!dialogsItemAnimator.isRunning()) {
                listView.setItemAnimator(null);
            }
            AndroidUtilities.runOnUIThread(updateListRunnable, 36);
        }
    }

    private FragmentSearchField fragmentSearchField;
    private SearchTextWatcher fragmentSearchFieldWatcher;

    private SearchTabsAndFiltersLayout searchTabsAndFiltersLayout;
    private ViewPagerFixed.TabsView searchTabsView;
    private FiltersView filtersView;

    private float contactsAlpha = 1f;
    private ValueAnimator contactsAlphaAnimator;
    private ViewPage[] viewPages;
    private ActionBarMenuItem passcodeItem;
    private ActionBarMenuItem downloadsItem;
    private DownloadProgressIcon downloadProgressIcon;
    private boolean downloadsItemVisible;
    public ActionBarMenuItem searchItem;
    private ActionBarMenuItem optionsItem;
    private ActionBarMenuItem speedItem;
    public static boolean switchingTheme;
    private ActionBarMenuItem doneItem;
    private ProxyDrawable proxyDrawable;
    private ActionBarMenuSubItem proxyMenuSubItem;
    private HintView2 storyHint;
    private HintView2 storyPremiumHint;
    private boolean canShowStoryHint;
    private boolean storyHintShown;
    private FragmentFloatingButton floatingButton3;
    private FragmentFloatingButton floatingButtonStories;
    private ButtonWithCounterView addChatsToCommunityButton;
    private ChatActivityFadeView communityBottomFadeView;
    private ChatAvatarContainer avatarContainer;
    private int undoViewIndex;
    private UndoView[] undoView = new UndoView[2];
    private FilterTabsView filterTabsView;
    private boolean askingForPermissions;
    private int searchViewPagerIndex;
    @Nullable
    private SearchViewPager searchViewPager;
    private SharedMediaLayout.SharedMediaPreloader sharedMediaPreloader;
    public DialogStoriesCell dialogStoriesCell;
    private DialogsActivityStatusLayout dialogsActivityStatusLayout;
    public boolean dialogStoriesCellVisible;
    public float progressToDialogStoriesCell;
    float searchViewPagerTranslationY;
    float panTranslationY;

    private View blurredView;

    private ItemOptions filterOptions;

    private SelectAnimatedEmojiDialog.SelectAnimatedEmojiDialogWindow selectAnimatedEmojiDialog;

    public boolean isReplyTo, isQuote;
    public long replyMessageAuthor;
    public long forwardOriginalChannel;
    private int initialDialogsType;

    private boolean checkingImportDialog;

    private int messagesCount;
    private int hasPoll;
    private boolean hasInvoice;

    private PacmanAnimation pacmanAnimation;

    private DialogCell slidingView;
    private DialogCell movingView;
    private boolean allowMoving;
    private boolean movingWas;
    private ArrayList<MessagesController.DialogFilter> movingDialogFilters = new ArrayList<>();
    private boolean waitingForScrollFinished;
    private boolean allowSwipeDuringCurrentTouch;
    private boolean updatePullAfterScroll;

    private BackDrawable backDrawable;

    private final Paint actionBarDefaultPaint = new Paint();

    private @Nullable ImageView actionModeCloseView;
    private NumberTextView selectedDialogsCountTextView;
    private final ArrayList<View> actionModeViews = new ArrayList<>();
    @Nullable
    private ActionBarMenuItem deleteItem;
    @Nullable
    private ActionBarMenuItem pinItem;
    @Nullable
    private ActionBarMenuItem muteItem;
    @Nullable
    private ActionBarMenuItem archive2Item;
    @Nullable
    private ActionBarMenuSubItem pin2Item;
    @Nullable
    private ActionBarMenuSubItem addToFolderItem;
    @Nullable
    private ActionBarMenuSubItem removeFromFolderItem;
    @Nullable
    private ActionBarMenuSubItem archiveItem;
    @Nullable
    private ActionBarMenuSubItem clearItem;
    @Nullable
    private ActionBarMenuSubItem readItem;
    @Nullable
    private ActionBarMenuSubItem blockItem;

    private float additionalFloatingTranslation;
    private float floatingButtonPanOffset;

    private AnimatorSet searchAnimator;
    private float searchAnimationProgress;

    private ChatInputViewsContainer chatInputViewsContainer;
    private FrameLayout chatInputBubbleContainer;
    private FrameLayout chatInputInAppContainer;
    private ChatActivityEnterView commentView;
    private ChatActivityEnterView.SendButton writeButton;
    private ActionBarMenuItem switchItem;

    private RectF rect = new RectF();
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private FragmentContextView fragmentLocationContextView;
    private FrameLayout fragmentLocationContextViewWrapper;
    private FragmentContextView fragmentContextView;
    private FrameLayout fragmentContextViewWrapper;
    private DialogsActivityTopPanelLayout topPanelLayout;
    private DialogsActivityTopBubblesFadeView topBubblesFadeView;
    private ActiveGiftAuctionsHintCell activeGiftAuctionsHintCell;
    private DialogsHintCell dialogsHintCell;
    private UnconfirmedAuthHintCell authHintCell;
    private Long cacheSize, deviceSize;
    private CommunityRequestsCell communityPendingRequests;

    private ArrayList<TLRPC.Dialog> frozenDialogsList;
    private boolean dialogsListFrozen;

    private AlertDialog permissionDialog;
    private boolean askAboutContacts = true;

    private boolean closeSearchFieldOnHide;
    private long searchDialogId;
    private TLObject searchObject;

    private int prevPosition;
    private int prevTop;
    private boolean scrollUpdated;
    private boolean floatingForceVisible;

    private boolean checkPermission = true;

    private int currentConnectionState;

    private boolean disableActionBarScrolling;

    private String selectAlertString;
    private String selectAlertStringGroup;
    private String addToGroupAlertString;
    public boolean resetDelegate = true;

    public static boolean[] dialogsLoaded = new boolean[UserConfig.MAX_ACCOUNT_COUNT];
    private boolean searching;
    private boolean searchWas;
    private boolean onlySelect;
    private boolean canSelectTopics;
    private String searchString;
    private String initialSearchString;
    private MessagesStorage.TopicKey openedDialogId = new MessagesStorage.TopicKey();
    private boolean cantSendToChannels;
    private boolean allowSwitchAccount;
    private boolean checkCanWrite;
    private boolean afterSignup;
    private boolean showSetPasswordConfirm;
    private int otherwiseReloginDays;
    public boolean allowGroups, allowMegagroups, allowLegacyGroups;
    public boolean allowChannels;
    public boolean allowUsers;
    public boolean allowBots;
    private boolean closeFragment;

    private DialogsActivityDelegate delegate;

    private ArrayList<MediaController.PhotoEntry> sharedMediaEntries;
    private String sharedLink;
    private CharSequence sharedTextSeed;
    private ShareTopView shareTopView;
    private Runnable shareLinkSearchRunnable;

    private ArrayList<Long> selectedDialogs = new ArrayList<>();
    public boolean notify = true;
    public int scheduleDate;
    public int scheduleRepeatPeriod;

    private int canReadCount;
    private int canPinCount;
    private int canMuteCount;
    private int canUnmuteCount;
    private int canClearCacheCount;
    private int canReportSpamCount;
    private int canUnarchiveCount;
    private int forumCount;
    private boolean canDeletePsaSelected;

    private int folderId;

    private DialogsActivity parentForwardDialogFragment;
    private long communityId;
    private TLRPC.Chat community;
    private TLRPC.ChatFull communityFull;
    private BackupImageView communityAvatarImage;
    private AvatarDrawable communityAvatarDrawable;

    private final static int pin = 100;
    private final static int read = 101;
    private final static int delete = 102;
    private final static int clear = 103;
    private final static int mute = 104;
    private final static int archive = 105;
    private final static int block = 106;
    private final static int archive2 = 107;
    private final static int pin2 = 108;
    private final static int add_to_folder = 109;
    private final static int remove_from_folder = 110;
    private final static int community_ungroup = 111;

    private final static int ARCHIVE_ITEM_STATE_PINNED = 0;
    private final static int ARCHIVE_ITEM_STATE_SHOWED = 1;
    private final static int ARCHIVE_ITEM_STATE_HIDDEN = 2;

    private long startArchivePullingTime;
    private boolean scrollingManually;
    private boolean canShowHiddenArchive;

    private AnimatorSet tabsAnimation;
    private boolean tabsAnimationInProgress;
    private boolean animatingForward;
    private float additionalOffset;
    private boolean backAnimation;
    private int maximumVelocity;
    private boolean startedTracking;
    private boolean maybeStartTracking;
    private static final Interpolator interpolator = t -> {
        --t;
        return t * t * t * t * t + 1.0F;
    };

    private Bulletin topBulletin;

    private AnimationNotificationsLocker notificationsLocker = new AnimationNotificationsLocker();
    private boolean searchIsShowed;
    private boolean searchWasFullyShowed;
    public boolean whiteActionBar;
    private boolean searchFiltersWasShowed;
    private float progressToActionMode;
    private ValueAnimator actionBarColorAnimator;

    //
    private float storiesYOffset;
    private float tabsYOffset;
    private float scrollAdditionalOffset;

    private int debugLastUpdateAction = -1;
    private boolean slowedReloadAfterDialogClick;

    private boolean isPremiumHintUpgrade;

    private Long statusDrawableGiftId;
    private Drawable logoDrawable;
    private AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable statusDrawable;
    private AnimatedStatusView animatedStatusView;
    public RightSlidingDialogContainer rightSlidingDialogContainer;

    public final Property<DialogsActivity, Float> SCROLL_Y = new AnimationProperties.FloatProperty<DialogsActivity>("animationValue") {
        @Override
        public void setValue(DialogsActivity object, float value) {
            object.setScrollY(value);
        }

        @Override
        public Float get(DialogsActivity object) {
            return scrollYOffset;
        }
    };

    public final Property<View, Float> SEARCH_TRANSLATION_Y = new AnimationProperties.FloatProperty<View>("viewPagerTranslation") {
        @Override
        public void setValue(View object, float value) {
            searchViewPagerTranslationY = value;
            object.setTranslationY(panTranslationY + searchViewPagerTranslationY);
            checkUi_searchFiltersVisibility();
        }

        @Override
        public Float get(View object) {
            return searchViewPagerTranslationY;
        }
    };

    private class ContentView extends SizeNotifierFrameLayout {

        private Paint actionBarSearchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public ContentView(Context context) {
            super(context);
        }

        private int startedTrackingPointerId;
        private int startedTrackingX;
        private int startedTrackingY;
        private VelocityTracker velocityTracker;
        private boolean globalIgnoreLayout;
        private int[] pos = new int[2];

        private boolean prepareForMoving(MotionEvent ev, boolean forward) {
            int id = filterTabsView.getNextPageId(forward);
            if (id < 0) {
                return false;
            }
            getParent().requestDisallowInterceptTouchEvent(true);
            maybeStartTracking = false;
            startedTracking = true;
            startedTrackingX = (int) (ev.getX() + additionalOffset);
            actionBar.setEnabled(false);
            filterTabsView.setEnabled(false);
            viewPages[1].selectedType = id;
            viewPages[1].setVisibility(View.VISIBLE);
            animatingForward = forward;
            showScrollbars(false);
            switchToCurrentSelectedMode(true);
            if (forward) {
                viewPages[1].setTranslationX(viewPages[0].getMeasuredWidth());
            } else {
                viewPages[1].setTranslationX(-viewPages[0].getMeasuredWidth());
            }
            return true;
        }

        @Override
        public void invalidateBlur() {
            super.invalidateBlur();
            blur3_InvalidateBlur();
        }

        public boolean checkTabsAnimationInProgress() {
            if (tabsAnimationInProgress) {
                boolean cancel = false;
                if (backAnimation) {
                    if (Math.abs(viewPages[0].getTranslationX()) < 1) {
                        viewPages[0].setTranslationX(0);
                        viewPages[1].setTranslationX(viewPages[0].getMeasuredWidth() * (animatingForward ? 1 : -1));
                        cancel = true;
                    }
                } else if (Math.abs(viewPages[1].getTranslationX()) < 1) {
                    viewPages[0].setTranslationX(viewPages[0].getMeasuredWidth() * (animatingForward ? -1 : 1));
                    viewPages[1].setTranslationX(0);
                    cancel = true;
                }
                if (cancel) {
                    showScrollbars(true);
                    if (tabsAnimation != null) {
                        tabsAnimation.cancel();
                        tabsAnimation = null;
                    }
                    tabsAnimationInProgress = false;
                }
                return tabsAnimationInProgress;
            }
            return false;
        }

        public int getActionBarFullHeight() {
            float h = actionBar.getHeight();
            float rightSlidingProgress = 0f;
            if (rightSlidingDialogContainer != null && rightSlidingDialogContainer.hasFragment()) {
                rightSlidingProgress = rightSlidingDialogContainer.openedProgress;
            }
            if (hasStories) {
                int storiesHeight = dp(DialogStoriesCell.HEIGHT_IN_DP);
                h += storiesHeight * (1f - searchAnimationProgress) * (1f - rightSlidingProgress) * (1f - progressToActionMode);
            }
            h += storiesOverscroll;
            h += dp(SEARCH_FIELD_HEIGHT) * (1f - progressToActionMode) * (1f - searchAnimationProgress) * (1f - rightSlidingProgress);

            return (int) h;
        }

        public int getActionBarTop() {
            float scrollY = scrollYOffset;
            float rightSlidingProgress = 0f;
            if (rightSlidingDialogContainer != null && rightSlidingDialogContainer.hasFragment()) {
                rightSlidingProgress = rightSlidingDialogContainer.openedProgress;
            }
            scrollY *= (1f - progressToActionMode) * (1f - rightSlidingProgress);
            return (int) (-getY() + scrollY * (1f - searchAnimationProgress));
        }

        private Rect blurBounds = new Rect();

        @Override
        protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
            if (child == blurredView) {
                return true;
            }
            if (SizeNotifierFrameLayout.drawingBlur) {
                return super.drawChild(canvas, child, drawingTime);
            }
            boolean result;
            if (child == viewPages[0] || (viewPages.length > 1 && child == viewPages[1]) || child == topPanelLayout || child == filterTabsView) {
                canvas.save();

                final boolean doNotClip = child == topPanelLayout || child == filterTabsView;
                if (!doNotClip) {
                    canvas.clipRect(0, -getY() + getActionBarTop() + getActionBarFullHeight(), getMeasuredWidth(), getMeasuredHeight());
                }
                if (slideFragmentProgress != 1f) {
                    if (slideFragmentLite) {
                        canvas.translate((-1) * dp(slideAmplitudeDp) * (1f - slideFragmentProgress), 0);
                    } else {
                        final float s = 1f - 0.05f * (1f - slideFragmentProgress);
                        canvas.translate((-dp(4)) * (1f - slideFragmentProgress), 0);
                        canvas.scale(s, s, 0, -getY() + scrollYOffset + getActionBarFullHeight());
                    }
                }
                result = super.drawChild(canvas, child, drawingTime);
                canvas.restore();
            } else if (child == actionBar && slideFragmentProgress != 1f) {
                canvas.save();
                if (slideFragmentLite) {
                    canvas.translate((-1) * dp(slideAmplitudeDp) * (1f - slideFragmentProgress), 0);
                } else {
                    float s = 1f - 0.05f * (1f - slideFragmentProgress);
                    canvas.translate((-dp(4)) * (1f - slideFragmentProgress), 0);
                    canvas.scale(s, s, 0, (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight() / 2f);
                }
                result = super.drawChild(canvas, child, drawingTime);
                canvas.restore();
            } else {
                result = super.drawChild(canvas, child, drawingTime);
            }
            return result;
        }

        @Override
        public void drawBlurRect(Canvas canvas, float y, Rect rectTmp, Paint blurScrimPaint, boolean top) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || !SharedConfig.chatBlurEnabled() || iBlur3SourceGlassFrosted == null || !BlurredBackgroundProviderImpl.checkBlurEnabled(currentAccount, resourceProvider)) {
                canvas.drawRect(rectTmp, blurScrimPaint);
                return;
            }

            final boolean isThemeLight = resourceProvider != null ? !resourceProvider.isDark() : !Theme.isCurrentThemeDark();
            int blurAlpha = isThemeLight ? 216 : ChatActivity.ACTION_BAR_BLUR_ALPHA;
            canvas.save();
            canvas.translate(0, -y);
            iBlur3SourceGlassFrosted.draw(canvas, rectTmp.left, rectTmp.top + y, rectTmp.right, rectTmp.bottom + y);
            canvas.restore();

            final int oldScrimAlpha = blurScrimPaint.getAlpha();
            blurScrimPaint.setAlpha(blurAlpha);
            canvas.drawRect(rectTmp, blurScrimPaint);
            blurScrimPaint.setAlpha(oldScrimAlpha);
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            if (Build.VERSION.SDK_INT >= 31 && scrollableViewNoiseSuppressor != null) {
                blur3_InvalidateBlur();
            }

            if (invalidateScrollY && (rightSlidingDialogContainer == null || !rightSlidingDialogContainer.hasFragment()) && progressToActionMode == 0) {
                invalidateScrollY = false;
                int firstItemPosition = hasHiddenArchive() && viewPages[0].dialogsType == DIALOGS_TYPE_DEFAULT ? 1 : 0;
                DialogsRecyclerView recyclerView = viewPages[0].listView;
                if (fixScrollYAfterArchiveOpened) {
                    if (waitingForScrollFinished) {
                        firstItemPosition = 0;
                    } else {
                        if (firstItemPosition == 0) {
                            fixScrollYAfterArchiveOpened = false;
                        }
                        if (fixScrollYAfterArchiveOpened) {
                            RecyclerView.ViewHolder archiveHolder = recyclerView.findViewHolderForLayoutPosition(0);
                            if (archiveHolder == null) {
                                fixScrollYAfterArchiveOpened = false;
                            } else if (archiveHolder.itemView.getBottom() <= recyclerView.getPaddingTop() - dp(DialogStoriesCell.HEIGHT_IN_DP)) {
                                fixScrollYAfterArchiveOpened = false;
                            } else if (archiveHolder.itemView.getTop() >= recyclerView.getPaddingTop()) {
                                fixScrollYAfterArchiveOpened = false;
                            }
                            if (fixScrollYAfterArchiveOpened && firstItemPosition == 1) {
                                firstItemPosition = 0;
                            }
                        }
                    }
                }

                RecyclerView.ViewHolder holder = recyclerView.findViewHolderForLayoutPosition(firstItemPosition);
                if (holder != null) {
                    float visiblePartAfterScroll = recyclerView.getPaddingTop() - holder.itemView.getY();
                    if (visiblePartAfterScroll >= 0) {
                        int maxScrollYOffset = getMaxScrollYOffset();
                        float newTranslation = -visiblePartAfterScroll;
                        if (newTranslation < -maxScrollYOffset) {
                            newTranslation = -maxScrollYOffset;
                        } else if (newTranslation > 0) {
                            newTranslation = 0;
                        }
                        DialogsActivity.this.setScrollY(newTranslation);
                    } else {
                        DialogsActivity.this.setScrollY(0);
                    }
                } else {
                    DialogsActivity.this.setScrollY(-getMaxScrollYOffset());
                }
            }
            final int actionBarHeight = getActionBarFullHeight();
            final int top;
            if (inPreviewMode) {
                top = AndroidUtilities.statusBarHeight;
            } else {
                top = getActionBarTop();
            }
            rightSlidingDialogContainer.setCurrentTop(top + actionBarHeight);
            float storiesAlpha = 1f;
            if (whiteActionBar) {
                if (searchAnimationProgress == 1f) {
                    actionBarSearchPaint.setColor(getThemedColor(Theme.key_windowBackgroundWhite));
                } else if (searchAnimationProgress == 0) {
                    if (fragmentSearchField != null) {
                        fragmentSearchField.setTranslationY(scrollYOffset + getSearchFieldAdditionOffset());
                    }
                }
                blurBounds.set(0, top, getMeasuredWidth(), top + actionBarHeight - dp(2 * searchAnimationProgress));
                if (searchAnimationProgress < 0) {
                    drawBlurRect(canvas, 0, blurBounds, searchAnimationProgress == 1f ? actionBarSearchPaint : actionBarDefaultPaint, true);
                }
                if (searchAnimationProgress > 0 && searchAnimationProgress < 1f) {
                    actionBarSearchPaint.setColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    if (searchIsShowed || !searchWasFullyShowed) {
                    } else {
                        blurBounds.set(0, top, getMeasuredWidth(), top + actionBarHeight - dp(2 * searchAnimationProgress));
                        drawBlurRect(canvas, 0, blurBounds, actionBarSearchPaint, true);
                    }
                    if (fragmentSearchField != null) {
                        fragmentSearchField.setTranslationY(top + actionBarHeight - (actionBar.getHeight() + (filterTabsView != null ? filterTabsView.getMeasuredHeight() : 0)) + getSearchFieldAdditionOffset());
                    }
                }
            } else if (!inPreviewMode) {
                if (progressToActionMode > 0) {
                    actionBarSearchPaint.setColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    blurBounds.set(0, Math.max(0, top), getMeasuredWidth(), top + actionBarHeight - dp(2 * searchAnimationProgress));
                    drawBlurRect(canvas, 0, blurBounds, actionBarSearchPaint, true);
                } else {
                    blurBounds.set(0, Math.max(0, top), getMeasuredWidth(), top + actionBarHeight - dp(2 * searchAnimationProgress));
                    drawBlurRect(canvas, 0, blurBounds, actionBarDefaultPaint, true);
                }
            }
            tabsYOffset = 0;
            storiesYOffset = 0;
            tabsYOffset -= Math.min(
                dp(hasStories ? DialogStoriesCell.HEIGHT_IN_DP : 0) + dp(SEARCH_FIELD_HEIGHT) + scrollYOffset,
                progressToActionMode * (dp(hasStories ? DialogStoriesCell.HEIGHT_IN_DP : 0) + dp(SEARCH_FIELD_HEIGHT))
            );
            storiesYOffset = tabsYOffset;
            if ((rightSlidingDialogContainer != null && rightSlidingDialogContainer.hasFragment())) {
                final float rightSlidingProgress = rightSlidingDialogContainer.openedProgress;

                tabsYOffset -= rightSlidingProgress * (getMaxScrollYOffset() + scrollYOffset);
                storiesYOffset = tabsYOffset;
                if (dialogStoriesCellVisible) {
                    storiesAlpha = 1f - Utilities.clamp(rightSlidingProgress / 0.5f, 1f, 0f);
                }
                if (filterTabsView != null && filterTabsView.getVisibility() == View.VISIBLE) {
                    tabsYOffset -= (1f - animatorFilterTabsVisible.getFloatValue()) * filterTabsView.getMeasuredHeight();
                }
                if (fragmentSearchField != null) {
                    fragmentSearchField.setTranslationY(lerp(scrollYOffset + tabsYOffset, -dp(hasStories ? DialogStoriesCell.HEIGHT_IN_DP : 0), rightSlidingProgress) + getSearchFieldAdditionOffset());
                }
                float rightFragmentOffset = 0;
                if (rightFragmentTransitionInProgress) {
                    float scrollOffset = rightFragmentTransitionIsOpen ? 0 : scrollYOffset;
                    rightFragmentOffset = -AndroidUtilities.lerp(-scrollOffset + (dp(!rightFragmentTransitionIsOpen && canShowFilterTabsView ? 50 : 0)), scrollOffset, rightSlidingDialogContainer.openedProgress);
                }
                float addH = 0;
                if (hasStories) {
                    addH += dp(DialogStoriesCell.HEIGHT_IN_DP);
                }
                addH += dp(SEARCH_FIELD_HEIGHT);
                addH *= rightSlidingDialogContainer.openedProgress;

                viewPages[0].setTranslationY(rightFragmentOffset - addH);
            } else {
                if (fragmentSearchField != null) {
                    fragmentSearchField.setTranslationY(lerp(
                        scrollYOffset + tabsYOffset + storiesOverscroll - dp(4),
                        -dp(SEARCH_FIELD_HEIGHT + (hasStories ? DialogStoriesCell.HEIGHT_IN_DP : 0)),
                        searchAnimationProgress
                    ));
                }
            }
            updateContextViewPosition();
            updateStoriesViewAlpha(storiesAlpha);
            super.dispatchDraw(canvas);
            drawHeaderShadow(canvas, top + actionBarHeight);

            /*if (fragmentContextView != null && fragmentContextView.isCallStyle()) {
                canvas.save();
                canvas.translate(fragmentContextView.getX(), fragmentContextView.getY());
                if (slideFragmentProgress != 1f) {
                    if (slideFragmentLite) {
                        canvas.translate((-1) * dp(slideAmplitudeDp) * (1f - slideFragmentProgress), 0);
                    } else {
                        final float s = 1f - 0.05f * (1f - slideFragmentProgress);
                        canvas.translate((-dp(4)) * (1f - slideFragmentProgress), 0);
                        canvas.scale(s, 1f, 0, fragmentContextView.getY());
                    }
                }
                fragmentContextView.setDrawOverlay(true);
                fragmentContextView.draw(canvas);
                fragmentContextView.setDrawOverlay(false);
                canvas.restore();
            }*/
            if (blurredView != null && blurredView.getVisibility() == View.VISIBLE) {
                if (blurredView.getAlpha() != 1f) {
                    if (blurredView.getAlpha() != 0) {
                        canvas.saveLayerAlpha(blurredView.getLeft(), blurredView.getTop(), blurredView.getRight(), blurredView.getBottom(), (int) (255 * blurredView.getAlpha()), Canvas.ALL_SAVE_FLAG);
                        canvas.translate(blurredView.getLeft(), blurredView.getTop());
                        blurredView.draw(canvas);
                        canvas.restore();
                    }
                } else {
                    blurredView.draw(canvas);
                }
            }
            if (!hasMainTabs && communityId == 0) {
                AndroidUtilities.drawNavigationBarProtection(canvas, this, getThemedColor(Theme.key_windowBackgroundWhite), navigationBarHeight);
            }
            wasDrawn = true;
        }

        @Override
        protected boolean invalidateOptimized() {
            return true;
        }

        private boolean wasPortrait;

        @Override
        protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
            final int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
            int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
            boolean portrait = heightSize > widthSize;

            setMeasuredDimension(widthSize, heightSize);

            if (doneItem != null) {
                LayoutParams layoutParams = (LayoutParams) doneItem.getLayoutParams();
                layoutParams.topMargin = actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0;
                layoutParams.height = ActionBar.getCurrentActionBarHeight();
            }

            measureChildWithMargins(actionBar, widthMeasureSpec, 0, heightMeasureSpec, 0);

            int keyboardSize = measureKeyboardHeight();
            int childCount = getChildCount();

            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                if (child == null || child.getVisibility() == GONE || child == actionBar) {
                    continue;
                }
                if (child instanceof DatabaseMigrationHint) {
                    int contentWidthSpec = View.MeasureSpec.makeMeasureSpec(widthSize, View.MeasureSpec.EXACTLY);
                    int h = View.MeasureSpec.getSize(heightMeasureSpec);
                    int contentHeightSpec = View.MeasureSpec.makeMeasureSpec(Math.max(dp(10), h + dp(2) - actionBar.getMeasuredHeight()), View.MeasureSpec.EXACTLY);
                    child.measure(contentWidthSpec, contentHeightSpec);
                } else if (child instanceof ViewPage) {
                    int contentWidthSpec = View.MeasureSpec.makeMeasureSpec(widthSize, View.MeasureSpec.EXACTLY);
                    int h = heightSize + dp(2);
                    if (rightSlidingDialogContainer.hasFragment()) {
                        if (canShowFilterTabsView) {
                            h += dp(50);
                        }
                        if (hasStories) {
                            h += dp(DialogStoriesCell.HEIGHT_IN_DP);
                        }
                        h += dp(SEARCH_FIELD_HEIGHT);
                    }
                    h += actionModeAdditionalHeight;
                    if (actionBarColorAnimator == null) {
                        child.setTranslationY(0);
                    }
                    int transitionPadding = ((isSlideBackTransition) ? (int) (h * 0.05f) : 0);
                    h += transitionPadding;
                    child.setPadding(child.getPaddingLeft(), child.getPaddingTop(), child.getPaddingRight(), transitionPadding);
                    child.measure(contentWidthSpec, View.MeasureSpec.makeMeasureSpec(Math.max(dp(10), h), View.MeasureSpec.EXACTLY));
                    child.setPivotX(child.getMeasuredWidth() / 2f);
                } else if (child == searchViewPager) {
                    searchViewPager.setTranslationY(searchViewPagerTranslationY);
                    searchViewPager.postsSearchContainer.setKeyboardHeight(keyboardSize);
                    final int contentWidthSpec = MeasureSpec.makeMeasureSpec(widthSize, View.MeasureSpec.EXACTLY);
                    final int h = MeasureSpec.getSize(heightMeasureSpec) + dp(ADDITIONAL_LIST_HEIGHT_DP);
                    final int contentHeightSpec = MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY);
                    checkUi_searchPagesPaddings(true);
                    child.measure(contentWidthSpec, contentHeightSpec);
                    child.setPivotX(child.getMeasuredWidth() / 2f);
                    AndroidUtilities.rectTmp2.set(0, actionBar.getMeasuredHeight() + dp(ADDITIONAL_LIST_HEIGHT_DP) - dp(2), child.getMeasuredWidth(), child.getMeasuredHeight());
                } else if (commentView != null && commentView.isPopupView(child)) {
                    if (AndroidUtilities.isInMultiwindow) {
                        if (AndroidUtilities.isTablet()) {
                            child.measure(View.MeasureSpec.makeMeasureSpec(widthSize, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(Math.min(dp(320), heightSize - AndroidUtilities.statusBarHeight + getPaddingTop()), View.MeasureSpec.EXACTLY));
                        } else {
                            child.measure(View.MeasureSpec.makeMeasureSpec(widthSize, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(heightSize - AndroidUtilities.statusBarHeight + getPaddingTop(), View.MeasureSpec.EXACTLY));
                        }
                    } else {
                        child.measure(View.MeasureSpec.makeMeasureSpec(widthSize, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(child.getLayoutParams().height, View.MeasureSpec.EXACTLY));
                    }
                } else if (child == rightSlidingDialogContainer) {
                    int h = View.MeasureSpec.getSize(heightMeasureSpec);
                    int transitionPadding = ((isSlideBackTransition) ? (int) (h * 0.05f) : 0);
                    h += transitionPadding;
                    rightSlidingDialogContainer.setTransitionPaddingBottom(transitionPadding);
                    child.measure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(Math.max(dp(10), h), View.MeasureSpec.EXACTLY));
                } else {
                    measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                }
            }

            if (portrait != wasPortrait) {
                post(() -> {
                    if (selectAnimatedEmojiDialog != null) {
                        selectAnimatedEmojiDialog.dismiss();
                        selectAnimatedEmojiDialog = null;
                    }
                });
                wasPortrait = portrait;
            }
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            final int count = getChildCount();

            int paddingBottom = 0;
            int keyboardSize = measureKeyboardHeight();
            setBottomClip(paddingBottom);

            final int W = getMeasuredWidth();
            final int H = getMeasuredHeight();

            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                if (child == null || child.getVisibility() == GONE) {
                    continue;
                }
                final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();

                final int width = child.getMeasuredWidth();
                final int height = child.getMeasuredHeight();

                int childLeft;
                int childTop;

                int gravity = lp.gravity;
                if (gravity == -1) {
                    gravity = Gravity.TOP | Gravity.LEFT;
                }

                final int absoluteGravity = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

                switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                    case Gravity.CENTER_HORIZONTAL:
                        childLeft = (W - width) / 2 + lp.leftMargin - lp.rightMargin;
                        break;
                    case Gravity.RIGHT:
                        childLeft = W - width - lp.rightMargin;
                        break;
                    case Gravity.LEFT:
                    default:
                        childLeft = lp.leftMargin;
                }

                switch (verticalGravity) {
                    case Gravity.TOP:
                        childTop = lp.topMargin + getPaddingTop();
                        break;
                    case Gravity.CENTER_VERTICAL:
                        childTop = ((H - paddingBottom) - height) / 2 + lp.topMargin - lp.bottomMargin;
                        break;
                    case Gravity.BOTTOM:
                        childTop = ((H - paddingBottom)) - height - lp.bottomMargin;
                        break;
                    default:
                        childTop = lp.topMargin;
                }

                if (child == fragmentSearchField || child == searchTabsAndFiltersLayout || child == dialogStoriesCell) {
                    childTop = actionBar.getMeasuredHeight();
                    if (child != fragmentSearchField && child != dialogStoriesCell && child != searchTabsAndFiltersLayout) {
                        childTop += dp(SEARCH_FIELD_HEIGHT);
                    }
                    //if (rightSlidingDialogContainer != null && rightSlidingDialogContainer.hasFragment() && (child == searchTabsView || child == filtersView)) {
                    //    childTop -= dp(SEARCH_FIELD_HEIGHT);
                    //}
                    if (hasStories && child == fragmentSearchField) {
                        childTop += dp(DialogStoriesCell.HEIGHT_IN_DP);
                    }
                    if (child == dialogStoriesCell && dialogStoriesCell.getPremiumHint() != null) {
                        dialogStoriesCell.getPremiumHint().layout(childLeft, childTop - dp(24 + 8 + 22) + height, childLeft + width, childTop - dp(24 + 8 + 22) + height + dialogStoriesCell.getPremiumHint().getMeasuredHeight());
                    }
                    if (child == searchTabsAndFiltersLayout) {
                        // childTop -= dp(4);
                    }
                    if (child == fragmentSearchField) {
                        childTop += dp(2);
                    }
                } else if (child == searchViewPager) {
                    childTop = -dp(ADDITIONAL_LIST_HEIGHT_DP);
                } else if (child instanceof DatabaseMigrationHint) {
                    childTop = actionBar.getMeasuredHeight();
                } else if (child instanceof ViewPage) {
                    childTop = 0;
                } else if (child == topPanelLayout || child == topBubblesFadeView || child == filterTabsView) {
                    childTop += actionBar.getMeasuredHeight();
                    childTop += dp(SEARCH_FIELD_HEIGHT);
                } else if (dialogStoriesCell != null && dialogStoriesCell.getPremiumHint() == child) {
                    continue;
                }
                child.layout(childLeft, childTop, childLeft + width, childTop + height);
            }

            if (searchViewPager != null) {
                searchViewPager.setKeyboardHeight(keyboardSize);
            }
            notifyHeightChanged();
            updateFloatingButtonOffset();
            updateContextViewPosition();
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            int action = ev.getActionMasked();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (actionBar.isActionModeShowed()) {
                    allowMoving = true;
                }
            }
            return checkTabsAnimationInProgress() || filterTabsView != null && filterTabsView.isAnimatingIndicator() || onTouchEvent(ev);
        }

        @Override
        public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            if (maybeStartTracking && !startedTracking) {
                onTouchEvent(null);
            }
            super.requestDisallowInterceptTouchEvent(disallowIntercept);
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (
                    parentLayout != null &&
                            filterTabsView != null && !filterTabsView.isEditing() &&
                            !searching &&
                            !rightSlidingDialogContainer.hasFragment() &&
                            !parentLayout.checkTransitionAnimation() && !parentLayout.isInPreviewMode() && !parentLayout.isPreviewOpenAnimationInProgress() &&
                            (
                                    ev == null ||
                                            startedTracking ||
                                            ev.getY() > getActionBarTop() + getActionBarFullHeight() && (chatInputViewsContainer == null || chatInputViewsContainer.getVisibility() != VISIBLE || ev.getY() < chatInputViewsContainer.getY())
                            ) && (
                            initialDialogsType == DIALOGS_TYPE_FORWARD ||
                                    SharedConfig.getChatSwipeAction(currentAccount) == SwipeGestureSettingsView.SWIPE_GESTURE_FOLDERS ||
                                    SharedConfig.getChatSwipeAction(currentAccount) == SwipeGestureSettingsView.SWIPE_GESTURE_ARCHIVE &&
                                            viewPages[0] != null && (viewPages[0].dialogsAdapter.getDialogsType() == 7 || viewPages[0].dialogsAdapter.getDialogsType() == 8))
            ) {
                if (ev != null) {
                    if (velocityTracker == null) {
                        velocityTracker = VelocityTracker.obtain();
                    }
                    velocityTracker.addMovement(ev);
                }
                if (ev != null && ev.getAction() == MotionEvent.ACTION_DOWN && checkTabsAnimationInProgress()) {
                    startedTracking = true;
                    startedTrackingPointerId = ev.getPointerId(0);
                    startedTrackingX = (int) ev.getX();
                    if (animatingForward) {
                        if (startedTrackingX < viewPages[0].getMeasuredWidth() + viewPages[0].getTranslationX()) {
                            additionalOffset = viewPages[0].getTranslationX();
                        } else {
                            ViewPage page = viewPages[0];
                            viewPages[0] = viewPages[1];
                            viewPages[1] = page;
                            animatingForward = false;
                            additionalOffset = viewPages[0].getTranslationX();
                            filterTabsView.selectTabWithId(viewPages[0].selectedType, 1f);
                            filterTabsView.selectTabWithId(viewPages[1].selectedType, additionalOffset / viewPages[0].getMeasuredWidth());
                            switchToCurrentSelectedMode(true);
                            viewPages[0].dialogsAdapter.resume();
                            viewPages[1].dialogsAdapter.pause();
                        }
                    } else {
                        if (startedTrackingX < viewPages[1].getMeasuredWidth() + viewPages[1].getTranslationX()) {
                            ViewPage page = viewPages[0];
                            viewPages[0] = viewPages[1];
                            viewPages[1] = page;
                            animatingForward = true;
                            additionalOffset = viewPages[0].getTranslationX();
                            filterTabsView.selectTabWithId(viewPages[0].selectedType, 1f);
                            filterTabsView.selectTabWithId(viewPages[1].selectedType, -additionalOffset / viewPages[0].getMeasuredWidth());
                            switchToCurrentSelectedMode(true);
                            viewPages[0].dialogsAdapter.resume();
                            viewPages[1].dialogsAdapter.pause();
                        } else {
                            additionalOffset = viewPages[0].getTranslationX();
                        }
                    }
                    tabsAnimation.removeAllListeners();
                    tabsAnimation.cancel();
                    tabsAnimationInProgress = false;
                } else if (ev != null && ev.getAction() == MotionEvent.ACTION_DOWN) {
                    additionalOffset = 0;
                }
                if (ev != null && ev.getAction() == MotionEvent.ACTION_DOWN && !startedTracking && !maybeStartTracking && filterTabsView.getVisibility() == VISIBLE) {
                    startedTrackingPointerId = ev.getPointerId(0);
                    maybeStartTracking = true;
                    startedTrackingX = (int) ev.getX();
                    startedTrackingY = (int) ev.getY();
                    velocityTracker.clear();
                } else if (ev != null && ev.getAction() == MotionEvent.ACTION_MOVE && ev.getPointerId(0) == startedTrackingPointerId) {
                    int dx = (int) (ev.getX() - startedTrackingX + additionalOffset);
                    int dy = Math.abs((int) ev.getY() - startedTrackingY);
                    if (startedTracking && (animatingForward && dx > 0 || !animatingForward && dx < 0)) {
                        if (!prepareForMoving(ev, dx < 0)) {
                            maybeStartTracking = true;
                            startedTracking = false;
                            viewPages[0].setTranslationX(0);
                            viewPages[1].setTranslationX(animatingForward ? viewPages[0].getMeasuredWidth() : -viewPages[0].getMeasuredWidth());
                            filterTabsView.selectTabWithId(viewPages[1].selectedType, 0);
                        }
                    }
                    if (maybeStartTracking && !startedTracking) {
                        float touchSlop = AndroidUtilities.getPixelsInCM(0.3f, true);
                        int dxLocal = (int) (ev.getX() - startedTrackingX);
                        if (Math.abs(dxLocal) >= touchSlop && Math.abs(dxLocal) > dy) {
                            prepareForMoving(ev, dx < 0);
                        }
                    } else if (startedTracking) {
                        viewPages[0].setTranslationX(dx);
                        if (animatingForward) {
                            viewPages[1].setTranslationX(viewPages[0].getMeasuredWidth() + dx);
                        } else {
                            viewPages[1].setTranslationX(dx - viewPages[0].getMeasuredWidth());
                        }
                        float scrollProgress = Math.abs(dx) / (float) viewPages[0].getMeasuredWidth();
                        if (viewPages[1].isLocked && scrollProgress > 0.3f) {
                            dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0, 0, 0));
                            filterTabsView.shakeLock(viewPages[1].selectedType);
                            AndroidUtilities.runOnUIThread(() -> {
                                showDialog(new LimitReachedBottomSheet(DialogsActivity.this, getContext(), LimitReachedBottomSheet.TYPE_FOLDERS, currentAccount, null));
                            }, 200);
                            return false;
                        } else {
                            filterTabsView.selectTabWithId(viewPages[1].selectedType, scrollProgress);
                        }
                    }
                } else if (ev == null || ev.getPointerId(0) == startedTrackingPointerId && (ev.getAction() == MotionEvent.ACTION_CANCEL || ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_POINTER_UP)) {
                    velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
                    float velX;
                    float velY;
                    if (ev != null && ev.getAction() != MotionEvent.ACTION_CANCEL) {
                        velX = velocityTracker.getXVelocity();
                        velY = velocityTracker.getYVelocity();
                        if (!startedTracking) {
                            if (Math.abs(velX) >= 3000 && Math.abs(velX) > Math.abs(velY)) {
                                prepareForMoving(ev, velX < 0);
                            }
                        }
                    } else {
                        velX = 0;
                        velY = 0;
                    }
                    if (startedTracking) {
                        float x = viewPages[0].getX();
                        tabsAnimation = new AnimatorSet();
                        if (viewPages[1].isLocked) {
                            backAnimation = true;
                        } else {
                            if (additionalOffset != 0) {
                                if (Math.abs(velX) > 1500) {
                                    backAnimation = animatingForward ? velX > 0 : velX < 0;
                                } else {
                                    if (animatingForward) {
                                        backAnimation = (viewPages[1].getX() > (viewPages[0].getMeasuredWidth() >> 1));
                                    } else {
                                        backAnimation = (viewPages[0].getX() < (viewPages[0].getMeasuredWidth() >> 1));
                                    }
                                }
                            } else {
                                backAnimation = Math.abs(x) < viewPages[0].getMeasuredWidth() / 3.0f && (Math.abs(velX) < 3500 || Math.abs(velX) < Math.abs(velY));
                            }
                        }
                        float dx;
                        if (backAnimation) {
                            dx = Math.abs(x);
                            if (animatingForward) {
                                tabsAnimation.playTogether(
                                        ObjectAnimator.ofFloat(viewPages[0], View.TRANSLATION_X, 0),
                                        ObjectAnimator.ofFloat(viewPages[1], View.TRANSLATION_X, viewPages[1].getMeasuredWidth())
                                );
                            } else {
                                tabsAnimation.playTogether(
                                        ObjectAnimator.ofFloat(viewPages[0], View.TRANSLATION_X, 0),
                                        ObjectAnimator.ofFloat(viewPages[1], View.TRANSLATION_X, -viewPages[1].getMeasuredWidth())
                                );
                            }
                        } else {
                            dx = viewPages[0].getMeasuredWidth() - Math.abs(x);
                            if (animatingForward) {
                                tabsAnimation.playTogether(
                                        ObjectAnimator.ofFloat(viewPages[0], View.TRANSLATION_X, -viewPages[0].getMeasuredWidth()),
                                        ObjectAnimator.ofFloat(viewPages[1], View.TRANSLATION_X, 0)
                                );
                            } else {
                                tabsAnimation.playTogether(
                                        ObjectAnimator.ofFloat(viewPages[0], View.TRANSLATION_X, viewPages[0].getMeasuredWidth()),
                                        ObjectAnimator.ofFloat(viewPages[1], View.TRANSLATION_X, 0)
                                );
                            }
                        }
                        tabsAnimation.setInterpolator(interpolator);

                        int width = getMeasuredWidth();
                        int halfWidth = width / 2;
                        float distanceRatio = Math.min(1.0f, 1.0f * dx / (float) width);
                        float distance = (float) halfWidth + (float) halfWidth * AndroidUtilities.distanceInfluenceForSnapDuration(distanceRatio);
                        velX = Math.abs(velX);
                        int duration;
                        if (velX > 0) {
                            duration = 4 * Math.round(1000.0f * Math.abs(distance / velX));
                        } else {
                            float pageDelta = dx / getMeasuredWidth();
                            duration = (int) ((pageDelta + 1.0f) * 100.0f);
                        }
                        duration = Math.max(150, Math.min(duration, 600));

                        tabsAnimation.setDuration(duration);
                        tabsAnimation.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animator) {
                                tabsAnimation = null;
                                if (!backAnimation) {
                                    ViewPage tempPage = viewPages[0];
                                    viewPages[0] = viewPages[1];
                                    viewPages[1] = tempPage;
                                    filterTabsView.selectTabWithId(viewPages[0].selectedType, 1.0f);
                                    updateCounters(false);
                                    viewPages[0].dialogsAdapter.resume();
                                    viewPages[1].dialogsAdapter.pause();
                                }
                                viewPages[1].setVisibility(View.GONE);
                                showScrollbars(true);
                                tabsAnimationInProgress = false;
                                maybeStartTracking = false;
                                actionBar.setEnabled(true);
                                filterTabsView.setEnabled(true);
                                checkListLoad(viewPages[0]);
                            }
                        });
                        tabsAnimation.start();
                        tabsAnimationInProgress = true;
                        startedTracking = false;
                    } else {
                        maybeStartTracking = false;
                        actionBar.setEnabled(true);
                        filterTabsView.setEnabled(true);
                    }
                    if (velocityTracker != null) {
                        velocityTracker.recycle();
                        velocityTracker = null;
                    }
                }
                return startedTracking;
            }
            return false;
        }

        private ClickHelper clickHelper = new ClickHelper(new ClickHelper.Delegate() {
            @Override
            public boolean needClickAt(View view, float x, float y) {
                return isInPreviewMode();
            }

            @Override
            public void onClickAt(View view, float x, float y) {
                parentLayout.expandPreviewFragment();
            }
        });

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            return clickHelper.onTouchEvent(this, ev) || super.dispatchTouchEvent(ev);
        }

        @Override
        public boolean hasOverlappingRendering() {
            return false;
        }

        @Override
        protected void drawList(Canvas blurCanvas, boolean top, ArrayList<IViewWithInvalidateCallback> views) {
            if (searchIsShowed) {
                if (searchViewPager != null && searchViewPager.getVisibility() == View.VISIBLE) {
                    searchViewPager.drawForBlur(blurCanvas);
                }
            }
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            if (statusDrawable != null) {
                statusDrawable.attach();
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if (statusDrawable != null) {
                statusDrawable.detach();
            }
        }
    }

    private float getSearchFieldAdditionOffset() {
        return -lerp(dp(4), dp(SEARCH_FIELD_HEIGHT), animatorSearchVisible.getFloatValue());
    }

    private void updateStoriesViewAlpha(float alpha) {
        final float factorSearch = Utilities.clamp(searchAnimationProgress * 2, 1f, 0f);
        dialogStoriesCell.setAlpha((1f - progressToActionMode) * alpha * progressToDialogStoriesCell * (1f - factorSearch));
        float containersAlpha;

        if (hasStories || animateToHasStories) {
            float p = Utilities.clamp(-scrollYOffset / dp(DialogStoriesCell.HEIGHT_IN_DP), 1f, 0f);
            if (progressToActionMode == 1f) {
                p = 1f;
            }
            float pHalf = Utilities.clamp(p / 0.5f, 1f, 0f);
            dialogStoriesCell.setClipTop(0);
            if (!hasStories && animateToHasStories) {
                dialogStoriesCell.setTranslationY(-dp(DialogStoriesCell.HEIGHT_IN_DP) - dp(8));
                dialogStoriesCell.setProgressToCollapse(1f);
                containersAlpha = 1f - progressToDialogStoriesCell;
            } else {
                dialogStoriesCell.setTranslationY(Math.max(scrollYOffset, -getMaxScrollYOffsetWithoutSearch()) + storiesYOffset + storiesOverscroll / 2f - dp(8));
                dialogStoriesCell.setProgressToCollapse(p, !rightSlidingDialogContainer.hasFragment());
                if (!animateToHasStories) {
                    containersAlpha = 1f - progressToDialogStoriesCell;
                } else {
                    containersAlpha = (1f - pHalf);
                }
            }
            actionBar.setTranslationY(0);
        } else {
            if (hasOnlySlefStories) {
                dialogStoriesCell.setTranslationY(-dp(DialogStoriesCell.HEIGHT_IN_DP) + Math.max(scrollYOffset, -getMaxScrollYOffsetWithoutSearch()) - dp(8));
                dialogStoriesCell.setProgressToCollapse(1f);
                dialogStoriesCell.setClipTop((int) (AndroidUtilities.statusBarHeight - dialogStoriesCell.getY()));
            }
            containersAlpha = 1f - progressToDialogStoriesCell;

            actionBar.setTranslationY(0);
        }
        containersAlpha *= (1f - factorSearch);
        if (containersAlpha != 1f) {
            actionBar.getTitlesContainer().setPivotY(AndroidUtilities.statusBarHeight);
            actionBar.getTitlesContainer().setPivotX(dp(20));
            float s = 0.4f + 0.6f * containersAlpha;
            actionBar.getTitlesContainer().setScaleY(s);
            actionBar.getTitlesContainer().setScaleX(s);

            actionBar.getAdditionalSubTitleOverlayContainer().setPivotX(0);
            actionBar.getAdditionalSubTitleOverlayContainer().setPivotY(-dp(30));
            actionBar.getAdditionalSubTitleOverlayContainer().setScaleY(s);
            actionBar.getAdditionalSubTitleOverlayContainer().setScaleX(s);

            final float titleAlpha = containersAlpha * (1f - progressToActionMode);
            actionBar.getTitlesContainer().setAlpha(titleAlpha);
            actionBar.getTitlesContainer().setVisibility(titleAlpha > 0 ? View.VISIBLE : View.INVISIBLE);
            actionBar.getAdditionalSubTitleOverlayContainer().setAlpha(titleAlpha);
            actionBar.getAdditionalSubTitleOverlayContainer().setVisibility(titleAlpha > 0 ? View.VISIBLE : View.INVISIBLE);
        } else {
            actionBar.getTitlesContainer().setScaleY(1f);
            actionBar.getTitlesContainer().setScaleX(1f);


            actionBar.getAdditionalSubTitleOverlayContainer().setScaleY(1f);
            actionBar.getAdditionalSubTitleOverlayContainer().setScaleX(1f);

            final float titleAlpha = 1f - progressToActionMode;
            actionBar.getTitlesContainer().setAlpha(titleAlpha);
            actionBar.getTitlesContainer().setVisibility(titleAlpha > 0 ? View.VISIBLE : View.INVISIBLE);
            actionBar.getAdditionalSubTitleOverlayContainer().setAlpha(titleAlpha);
            actionBar.getAdditionalSubTitleOverlayContainer().setVisibility(titleAlpha > 0 ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public static float viewOffset = 0.0f;

    public class DialogsRecyclerView extends BlurredRecyclerView implements StoriesListPlaceProvider.ClippedView {

        public boolean updateDialogsOnNextDraw;
        private boolean firstLayout = true;
        private boolean ignoreLayout;
        private final ViewPage parentPage;
        private int appliedPaddingTop;
        private int lastTop;
        private int lastListPadding;
        private float rightFragmentOpenedProgress;

        Paint paint = new Paint();
        RectF rectF = new RectF();
        private RecyclerListView animationSupportListView;
        LongSparseArray<View> animationSupportViewsByDialogId;
        private Paint selectorPaint;
        float lastDrawSelectorY;
        float selectorPositionProgress = 1f;
        float animateFromSelectorPosition;
        boolean animateSwitchingSelector;
        UserListPoller poller;
        public int additionalPadding;

        public DialogsRecyclerView(Context context, ViewPage page) {
            super(context);
            parentPage = page;
            additionalClipBottom = dp(200);
        }

        public void prepareSelectorForAnimation() {
            selectorPositionProgress = 0;
            animateFromSelectorPosition = lastDrawSelectorY;
            animateSwitchingSelector = rightFragmentOpenedProgress != 0;
        }

        @Override
        protected boolean updateEmptyViewAnimated() {
            return true;
        }

        public void setViewsOffset(float viewOffset) {
            DialogsActivity.viewOffset = viewOffset;
            int n = getChildCount();
            for (int i = 0; i < n; i++) {
                getChildAt(i).setTranslationY(viewOffset);
            }

            if (selectorPosition != NO_POSITION) {
                View v = getLayoutManager().findViewByPosition(selectorPosition);
                if (v != null) {
                    selectorRect.set(v.getLeft(), (int) (v.getTop() + viewOffset), v.getRight(), (int) (v.getBottom() + viewOffset));
                    selectorDrawable.setBounds(selectorRect);
                }
            }
            invalidate();
        }

        public float getViewOffset() {
            return viewOffset;
        }

        @Override
        protected int measureBlurTopPadding() {
            return dp(48);
        }

        @Override
        public void addView(View child, int index, ViewGroup.LayoutParams params) {
            super.addView(child, index, params);
            child.setTranslationY(viewOffset);
            child.setTranslationX(0);
            child.setAlpha(1f);
        }

        @Override
        public void removeView(View view) {
            super.removeView(view);
            view.setTranslationY(0);
            view.setTranslationX(0);
            view.setAlpha(1f);
        }

        @Override
        public void onDraw(Canvas canvas) {
            if (parentPage.pullForegroundDrawable != null && viewOffset != 0) {
                int pTop = getPaddingTop();
                if (pTop != 0) {
                    canvas.save();
                    canvas.translate(0, pTop);
                }
                parentPage.pullForegroundDrawable.drawOverScroll(canvas);
                if (pTop != 0) {
                    canvas.restore();
                }
            }
            super.onDraw(canvas);
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            canvas.save();
            if (rightFragmentOpenedProgress > 0) {
                canvas.clipRect(0, 0, AndroidUtilities.lerp(getMeasuredWidth(), dp(RightSlidingDialogContainer.getRightPaddingSize()), rightFragmentOpenedProgress), getMeasuredHeight());
                paint.setColor(getThemedColor(Theme.key_chats_pinnedOverlay));
                paint.setAlpha((int) (paint.getAlpha() * rightFragmentOpenedProgress));
                canvas.drawRect(0, 0, dp(RightSlidingDialogContainer.getRightPaddingSize()), getMeasuredHeight(), paint);


                int alpha = Theme.dividerPaint.getAlpha();
                Theme.dividerPaint.setAlpha((int) (rightFragmentOpenedProgress * alpha));
                canvas.drawRect(dp(RightSlidingDialogContainer.getRightPaddingSize()), 0, dp(RightSlidingDialogContainer.getRightPaddingSize()) - 1, getMeasuredHeight(), Theme.dividerPaint);
                Theme.dividerPaint.setAlpha(alpha);
            }

            int maxSupportedViewsPosition = Integer.MIN_VALUE;
            int minSupportedViewsPosition = Integer.MAX_VALUE;

            if (animationSupportListView != null) {
                if (animationSupportViewsByDialogId == null) {
                    animationSupportViewsByDialogId = new LongSparseArray<>();
                }

                for (int i = 0; i < animationSupportListView.getChildCount(); i++) {
                    View child = animationSupportListView.getChildAt(i);
                    if (child instanceof DialogCell && child.getBottom() > 0) {
                        animationSupportViewsByDialogId.put(((DialogCell) child).getDialogId(), child);
                    }
                }
            }

            float maxTop = Integer.MAX_VALUE;
            float maxBottom = Integer.MIN_VALUE;
            DialogCell selectedCell = null;

            float scrollOffset = rightFragmentTransitionIsOpen ? 0 : scrollYOffset;

            for (int i = 0; i < getChildCount(); i++) {
                View view = getChildAt(i);
                DialogCell dialogCell = null;
                if (view instanceof DialogCell) {
                    dialogCell = (DialogCell) view;
                    dialogCell.setRightFragmentOpenedProgress(rightFragmentOpenedProgress);
                    if (AndroidUtilities.isTablet()) {
                        dialogCell.setDialogSelected(dialogCell.getDialogId() == openedDialogId.dialogId);
                    }
                    if (animationSupportViewsByDialogId != null && animationSupportListView != null) {
                        View animateToView = animationSupportViewsByDialogId.get(dialogCell.getDialogId());

                        animationSupportViewsByDialogId.delete(dialogCell.getDialogId());
                        if (animateToView != null) {
                            int supportViewPosition = animationSupportListView.getChildLayoutPosition(animateToView);
                            if (supportViewPosition > maxSupportedViewsPosition) {
                                maxSupportedViewsPosition = supportViewPosition;
                            }
                            if (supportViewPosition < minSupportedViewsPosition) {
                                minSupportedViewsPosition = supportViewPosition;
                            }
                            dialogCell.collapseOffset = (animateToView.getTop() - dialogCell.getTop()) * rightFragmentOpenedProgress;

                            if (dialogCell.getTop() + dialogCell.collapseOffset < maxTop) {
                                maxTop = dialogCell.getTop() + dialogCell.collapseOffset - scrollOffset;
                            }
                            float bottom = dialogCell.getTop() + AndroidUtilities.lerp(dialogCell.getMeasuredHeight(), animateToView.getMeasuredHeight(), rightFragmentOpenedProgress);
                            if (bottom + dialogCell.collapseOffset > maxBottom) {
                                maxBottom = bottom + dialogCell.collapseOffset - scrollOffset;
                            }
                        }
                    }
                    if (updateDialogsOnNextDraw) {
                        if (dialogCell.update(0, true)) {
                            int p = getChildAdapterPosition(dialogCell);
                            if (p >= 0) {
                                getAdapter().notifyItemChanged(p);
                            }
                        }
                    }
                    if (dialogCell.getDialogId() == rightSlidingDialogContainer.getCurrentFragmetDialogId()) {
                        selectedCell = dialogCell;
                    }
                }
                if (animationSupportListView != null) {
                    int restoreCount = canvas.save();

                    canvas.translate(view.getX(), view.getY());
                    if (dialogCell != null) {
                        dialogCell.rightFragmentOffset = -scrollOffset;
                    } else {
                        canvas.saveLayerAlpha(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight(), (int) (255 * (1f - rightFragmentOpenedProgress)), Canvas.ALL_SAVE_FLAG);
                    }
                    view.draw(canvas);

                    if (dialogCell != null && dialogCell != selectedCell) {
                        dialogCell.collapseOffset = 0;
                        dialogCell.rightFragmentOffset = 0;
                    }
                    canvas.restoreToCount(restoreCount);
                }
            }


            if (selectedCell != null) {
                canvas.save();
                lastDrawSelectorY = selectedCell.getY() + selectedCell.collapseOffset + selectedCell.avatarImage.getImageY();
                selectedCell.collapseOffset = 0;
                selectedCell.rightFragmentOffset = 0;
                if (selectorPositionProgress != 1f) {
                    selectorPositionProgress += 16 / 200f;
                    selectorPositionProgress = Utilities.clamp(selectorPositionProgress, 1f, 0f);
                    invalidate();
                }
                float selectorPositionProgress = CubicBezierInterpolator.DEFAULT.getInterpolation(this.selectorPositionProgress);
                boolean animateInOut = false;
                if (selectorPositionProgress != 1f && animateFromSelectorPosition != Integer.MIN_VALUE) {
                    if (Math.abs(animateFromSelectorPosition - lastDrawSelectorY) < getMeasuredHeight() * 0.4f) {
                        lastDrawSelectorY = AndroidUtilities.lerp(animateFromSelectorPosition, lastDrawSelectorY, selectorPositionProgress);
                    } else {
                        animateInOut = true;
                    }
                }

                float hideProgrss = animateSwitchingSelector && (animateInOut || animateFromSelectorPosition == Integer.MIN_VALUE) ? (1f - selectorPositionProgress) : (1f - rightFragmentOpenedProgress);
                if (hideProgrss == 1f) {
                    lastDrawSelectorY = Integer.MIN_VALUE;
                }
                float xOffset = -dp(5) * hideProgrss;
                AndroidUtilities.rectTmp.set(-dp(4) + xOffset, lastDrawSelectorY - dp(1), dp(4) + xOffset, lastDrawSelectorY + selectedCell.avatarImage.getImageHeight() + dp(1));
                if (selectorPaint == null) {
                    selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                }
                selectorPaint.setColor(getThemedColor(Theme.key_featuredStickers_addButton));
                canvas.drawRoundRect(AndroidUtilities.rectTmp, dp(4), dp(4), selectorPaint);
                canvas.restore();
            } else {
                lastDrawSelectorY = Integer.MIN_VALUE;
            }

            //undrawing views
            if (animationSupportViewsByDialogId != null) {
                float maxUndrawTop = Integer.MIN_VALUE;
                float maxUndrawBottom = Integer.MAX_VALUE;
                for (int i = 0; i < animationSupportViewsByDialogId.size(); i++) {
                    View view = animationSupportViewsByDialogId.valueAt(i);
                    int position = animationSupportListView.getChildLayoutPosition(view);
                    if (position < minSupportedViewsPosition && view.getTop() > maxUndrawTop) {
                        maxUndrawTop = view.getTop();
                    }
                    if (position > maxSupportedViewsPosition && view.getBottom() < maxUndrawBottom) {
                        maxUndrawBottom = view.getBottom();
                    }
                }
                for (int i = 0; i < animationSupportViewsByDialogId.size(); i++) {
                    View view = animationSupportViewsByDialogId.valueAt(i);
                    if (view instanceof DialogCell) {
                        int position = animationSupportListView.getChildLayoutPosition(view);
                        DialogCell dialogCell = (DialogCell) view;
                        dialogCell.isTransitionSupport = false;
                        dialogCell.buildLayout();
                        dialogCell.isTransitionSupport = true;
                        dialogCell.setRightFragmentOpenedProgress(rightFragmentOpenedProgress);

                        int restoreCount = canvas.save();
                        if (position > maxSupportedViewsPosition) {
                            canvas.translate(view.getX(), maxBottom + view.getBottom() - maxUndrawBottom);
                        } else {
                            canvas.translate(view.getX(), maxBottom + view.getTop() - maxUndrawTop);
                        }
                        view.draw(canvas);

                        canvas.restoreToCount(restoreCount);
                    }
                }
                animationSupportViewsByDialogId.clear();
            }

            updateDialogsOnNextDraw = false;
            if (animationSupportListView != null) {
                invalidate();
            }

            if (animationSupportListView == null) {
                super.dispatchDraw(canvas);
            }

            if (drawMovingViewsOverlayed()) {
                paint.setColor(getThemedColor(Theme.key_windowBackgroundWhite));
                for (int i = 0; i < getChildCount(); i++) {
                    View view = getChildAt(i);

                    if ((view instanceof DialogCell && ((DialogCell) view).isMoving()) || (view instanceof DialogsAdapter.LastEmptyView && ((DialogsAdapter.LastEmptyView) view).moving)) {
                        if (view.getAlpha() != 1f) {
                            rectF.set(view.getX(), view.getY(), view.getX() + view.getMeasuredWidth(), view.getY() + view.getMeasuredHeight());
                            canvas.saveLayerAlpha(rectF, (int) (255 * view.getAlpha()), Canvas.ALL_SAVE_FLAG);
                        } else {
                            canvas.save();
                        }
                        canvas.translate(view.getX(), view.getY());
                        canvas.drawRect(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight(), paint);
                        view.draw(canvas);
                        canvas.restore();
                    }
                }
                invalidate();
            }
            if (slidingView != null && pacmanAnimation != null) {
                pacmanAnimation.draw(canvas, slidingView.getTop() + slidingView.getMeasuredHeight() / 2);
            }
            if (poller == null) {
                poller = UserListPoller.getInstance(currentAccount);
            }
            poller.checkList( this);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            if (ev.getAction() == MotionEvent.ACTION_DOWN && ev.getY() < (getPaddingTop() + scrollYOffset)) {
                return false;
            }

            return super.dispatchTouchEvent(ev);
        }

        private boolean drawMovingViewsOverlayed() {
            return getItemAnimator() != null && getItemAnimator().isRunning();
        }

        @Override
        public boolean drawChild(Canvas canvas, View child, long drawingTime) {
            if (drawMovingViewsOverlayed() && child instanceof DialogCell && ((DialogCell) child).isMoving()) {
                return true;
            }
            return super.drawChild(canvas, child, drawingTime);
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
        }

        @Override
        public void setAdapter(RecyclerView.Adapter adapter) {
            super.setAdapter(adapter);
            firstLayout = true;
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            int t = 0;
            int pos = parentPage.layoutManager.findFirstVisibleItemPosition();
            if (pos != RecyclerView.NO_POSITION && parentPage.itemTouchhelper.isIdle() && !parentPage.layoutManager.hasPendingScrollPosition() && parentPage.listView.getScrollState() != RecyclerView.SCROLL_STATE_DRAGGING) {
                RecyclerView.ViewHolder holder = parentPage.listView.findViewHolderForAdapterPosition(pos);
                if (holder != null) {
                    int top = holder.itemView.getTop();
                    if (parentPage.dialogsType == DIALOGS_TYPE_DEFAULT && hasHiddenArchive() && parentPage.archivePullViewState == ARCHIVE_ITEM_STATE_HIDDEN) {
                        pos = Math.max(1, pos);
                    }
                    ignoreLayout = true;
                    parentPage.layoutManager.scrollToPositionWithOffset(pos, (int) (top - lastListPadding + scrollAdditionalOffset + parentPage.pageAdditionalOffset));
                    ignoreLayout = false;
                }
            } else if (pos == RecyclerView.NO_POSITION && firstLayout) {
                parentPage.layoutManager.scrollToPositionWithOffset(parentPage.dialogsType == DIALOGS_TYPE_DEFAULT && hasHiddenArchive() ? 1 : 0, (int) scrollYOffset);
            }

            ignoreLayout = true;
            t = ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0);
            if (hasStories && !actionModeFullyShowed) {
                t += dp(DialogStoriesCell.HEIGHT_IN_DP);
            }
            if (!actionModeFullyShowed) {
                t += dp(SEARCH_FIELD_HEIGHT);
            }
            additionalPadding = 0;

            final float filterTabsVisibility = getFilterTabsVisibilityFactor(false);
            final float topPanelsVisibility = topPanelLayout != null ? topPanelLayout.getMetadata().getTotalVisibility() : 0f;

            t += (int) (dp(36 + 14) * filterTabsVisibility);
            additionalPadding += (int) (dp(36 + 14) * filterTabsVisibility);

            if (topPanelLayout != null) {
                final int h = (int) topPanelLayout.getAnimatedHeightWithPadding(lerp((float) dp(14), dp(7), filterTabsVisibility));
                t += h;
                additionalPadding += h;
            }

            t -= dp(5 * Math.max(filterTabsVisibility, topPanelsVisibility));
            additionalPadding -= dp(5 * Math.max(filterTabsVisibility, topPanelsVisibility));

            final int b = calculateListViewPaddingBottom();
            if (t != topPadding || b != getPaddingBottom()) {
                setTopGlowOffset(t);
                setPadding(0, t, 0, b);
                if (hasStories) {
                    parentPage.progressView.setPaddingTop(t - dp(DialogStoriesCell.HEIGHT_IN_DP));
                } else {
                    parentPage.progressView.setPaddingTop(t);
                }
                for (int i = 0; i < getChildCount(); i++) {
                    if (getChildAt(i) instanceof DialogsAdapter.LastEmptyView) {
                        getChildAt(i).requestLayout();
                    }
                }
            }
            ignoreLayout = false;

            if (firstLayout && getMessagesController().dialogsLoaded) {
                if (parentPage.dialogsType == DIALOGS_TYPE_DEFAULT && hasHiddenArchive()) {
                    ignoreLayout = true;
                    LinearLayoutManager layoutManager = (LinearLayoutManager) getLayoutManager();
                    layoutManager.scrollToPositionWithOffset(1, (int) scrollYOffset);
                    ignoreLayout = false;
                }
                firstLayout = false;
            }
            super.onMeasure(widthSpec, heightSpec);
            if (!onlySelect) {
                if (appliedPaddingTop != t && viewPages != null && viewPages.length > 1 && !startedTracking && (tabsAnimation == null || !tabsAnimation.isRunning()) && !tabsAnimationInProgress && (filterTabsView == null || !filterTabsView.isAnimatingIndicator())) {
//                    viewPages[1].setTranslationX(viewPages[0].getMeasuredWidth());
                }
            }
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);
            lastListPadding = getPaddingTop();
            lastTop = t;
            scrollAdditionalOffset = 0;
            parentPage.pageAdditionalOffset = 0;
        }

        @Override
        public void requestLayout() {
            if (ignoreLayout) {
                return;
            }
            super.requestLayout();
        }

        private void toggleArchiveHidden(boolean action, DialogCell dialogCell) {
            SharedConfig.toggleArchiveHidden();
            final UndoView undoView = getUndoView();
            if (SharedConfig.archiveHidden) {
                if (dialogCell != null) {
                    disableActionBarScrolling = true;
                    waitingForScrollFinished = true;
                    int offset = (dialogCell.getMeasuredHeight() + (dialogCell.getTop() - getPaddingTop()));
                    if (hasStories && !dialogStoriesCell.isExpanded()) {
                        fixScrollYAfterArchiveOpened = true;
                        offset += dp(DialogStoriesCell.HEIGHT_IN_DP);
                    }
                    smoothScrollBy(0, offset, CubicBezierInterpolator.EASE_OUT);
                    if (action) {
                        updatePullAfterScroll = true;
                    } else {
                        updatePullState();
                    }
                }
                undoView.showWithAction(0, UndoView.ACTION_ARCHIVE_HIDDEN, null, null);
            } else {
                undoView.showWithAction(0, UndoView.ACTION_ARCHIVE_PINNED, null, null);
                updatePullState();
                if (action && dialogCell != null) {
                    dialogCell.resetPinnedArchiveState();
                    dialogCell.invalidate();
                }
            }
        }

        private void updatePullState() {
            parentPage.archivePullViewState = SharedConfig.archiveHidden ? ARCHIVE_ITEM_STATE_HIDDEN : ARCHIVE_ITEM_STATE_PINNED;
            if (parentPage.pullForegroundDrawable != null) {
                parentPage.pullForegroundDrawable.setWillDraw(parentPage.archivePullViewState != ARCHIVE_ITEM_STATE_PINNED);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (fastScrollAnimationRunning || waitingForScrollFinished || rightFragmentTransitionInProgress) {
                return false;
            }
            int action = e.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                setOverScrollMode(View.OVER_SCROLL_ALWAYS);
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (!parentPage.itemTouchhelper.isIdle() && parentPage.swipeController.swipingFolder) {
                    parentPage.swipeController.swipeFolderBack = true;
                    if (parentPage.itemTouchhelper.checkHorizontalSwipe(null, ItemTouchHelper.LEFT) != 0) {
                        if (parentPage.swipeController.currentItemViewHolder != null) {
                            ViewHolder viewHolder = parentPage.swipeController.currentItemViewHolder;
                            if (viewHolder.itemView instanceof DialogCell) {
                                DialogCell dialogCell = (DialogCell) viewHolder.itemView;
                                long dialogId = dialogCell.getDialogId();
                                if (DialogObject.isFolderDialogId(dialogId)) {
                                    toggleArchiveHidden(false, dialogCell);
                                } else {
                                    TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(dialogId);
                                    if (dialog != null) {
                                        TLRPC.Chat chat = getMessagesController().getChat(-dialogId);
                                        if (ChatObject.isCommunity(chat)) {
                                            ArrayList<Long> selectedDialogs = new ArrayList<>();
                                            selectedDialogs.add(dialogId);
                                            performSelectedDialogsAction(selectedDialogs, community_ungroup, true, false);
                                        } else if (SharedConfig.getChatSwipeAction(currentAccount) == SwipeGestureSettingsView.SWIPE_GESTURE_READ) {
                                            ArrayList<Long> selectedDialogs = new ArrayList<>();
                                            selectedDialogs.add(dialogId);
                                            canReadCount = dialog.unread_count > 0 || dialog.unread_mark ? 1 : 0;
                                            performSelectedDialogsAction(selectedDialogs, read, true, false);
                                        } else if (SharedConfig.getChatSwipeAction(currentAccount) == SwipeGestureSettingsView.SWIPE_GESTURE_MUTE) {
                                            if (!getMessagesController().isDialogMuted(dialogId, 0)) {
                                                NotificationsController.getInstance(UserConfig.selectedAccount).setDialogNotificationsSettings(dialogId, 0, NotificationsController.SETTING_MUTE_FOREVER);
                                                if (BulletinFactory.canShowBulletin(DialogsActivity.this)) {
                                                    BulletinFactory.createMuteBulletin(DialogsActivity.this, NotificationsController.SETTING_MUTE_FOREVER).show();
                                                }
                                            } else {
                                                ArrayList<Long> selectedDialogs = new ArrayList<>();
                                                selectedDialogs.add(dialogId);
                                                canMuteCount = MessagesController.getInstance(currentAccount).isDialogMuted(dialogId, 0) ? 0 : 1;
                                                canUnmuteCount = canMuteCount > 0 ? 0 : 1;
                                                performSelectedDialogsAction(selectedDialogs, mute, true, false);
                                            }
                                        } else if (SharedConfig.getChatSwipeAction(currentAccount) == SwipeGestureSettingsView.SWIPE_GESTURE_PIN) {
                                            ArrayList<Long> selectedDialogs = new ArrayList<>();
                                            selectedDialogs.add(dialogId);
                                            boolean pinned = isDialogPinned(dialog);
                                            canPinCount = pinned ? 0 : 1;
                                            performSelectedDialogsAction(selectedDialogs, pin, true, false);
                                        } else if (SharedConfig.getChatSwipeAction(currentAccount) == SwipeGestureSettingsView.SWIPE_GESTURE_DELETE) {
                                            ArrayList<Long> selectedDialogs = new ArrayList<>();
                                            selectedDialogs.add(dialogId);
                                            performSelectedDialogsAction(selectedDialogs, delete, true, false);
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            }
            boolean result = super.onTouchEvent(e);
            if (parentPage.dialogsType == DIALOGS_TYPE_DEFAULT && (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) && parentPage.archivePullViewState == ARCHIVE_ITEM_STATE_HIDDEN && hasHiddenArchive()) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) getLayoutManager();
                int currentPosition = layoutManager.findFirstVisibleItemPosition();
                if (currentPosition == 0) {
                    int pTop = getPaddingTop();
                    DialogCell view = findArchiveDialogCell(parentPage);
                    if (view != null) {
                        int height = (int) (dp(SharedConfig.useThreeLinesLayout ? 76 : 70) * PullForegroundDrawable.SNAP_HEIGHT);
                        int diff = (view.getTop() - pTop) + view.getMeasuredHeight();

                        long pullingTime = System.currentTimeMillis() - startArchivePullingTime;
                        if (diff < height || pullingTime < PullForegroundDrawable.minPullingTime) {
                            disableActionBarScrolling = true;
                            smoothScrollBy(0, diff, CubicBezierInterpolator.EASE_OUT_QUINT);
                            parentPage.archivePullViewState = ARCHIVE_ITEM_STATE_HIDDEN;
                        } else {
                            if (parentPage.archivePullViewState != ARCHIVE_ITEM_STATE_SHOWED) {
                                if (getViewOffset() == 0) {
                                    disableActionBarScrolling = true;
                                    smoothScrollBy(0, (view.getTop() - pTop), CubicBezierInterpolator.EASE_OUT_QUINT);
                                }
                                if (!canShowHiddenArchive) {
                                    canShowHiddenArchive = true;
                                    try {
                                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                                    } catch (Exception ignored) {}
                                    if (parentPage.pullForegroundDrawable != null) {
                                        parentPage.pullForegroundDrawable.colorize(true);
                                    }
                                }
                                ((DialogCell) view).startOutAnimation();
                                parentPage.archivePullViewState = ARCHIVE_ITEM_STATE_SHOWED;
                                if (AndroidUtilities.isAccessibilityScreenReaderEnabled()) {
                                    AndroidUtilities.makeAccessibilityAnnouncement(LocaleController.getString(R.string.AccDescrArchivedChatsShown));
                                }
                            }
                        }

                        if (getViewOffset() != 0) {
                            ValueAnimator valueAnimator = ValueAnimator.ofFloat(getViewOffset(), 0f);
                            valueAnimator.addUpdateListener(animation -> setViewsOffset((float) animation.getAnimatedValue()));

                            valueAnimator.setDuration(Math.max(100, (long) (350f - 120f * (getViewOffset() / PullForegroundDrawable.getMaxOverscroll()))));
                            valueAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
                            setScrollEnabled(false);
                            valueAnimator.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    setScrollEnabled(true);
                                }
                            });
                            valueAnimator.start();
                        }
                    }
                }
            }
            return result;
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent e) {
            if (fastScrollAnimationRunning || waitingForScrollFinished || parentPage.dialogsItemAnimator.isRunning()) {
                return false;
            }
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                allowSwipeDuringCurrentTouch = !actionBar.isActionModeShowed();
            }
            return super.onInterceptTouchEvent(e);
        }

        @Override
        protected boolean allowSelectChildAtPosition(View child) {
            if (child instanceof HeaderCell && !child.isClickable()) {
                return false;
            }
            return true;
        }

        public void setOpenRightFragmentProgress(float progress) {
            rightFragmentOpenedProgress = progress;
            invalidate();
        }

        public void setAnimationSupportView(RecyclerListView animationSupportListView, float scrollOffset, boolean opened, boolean backward) {
            RecyclerListView anchorListView = animationSupportListView == null ? this.animationSupportListView : this;
            DialogCell anchorView = null;
            DialogCell selectedDialogView = null;
            if (anchorListView == null) {
                this.animationSupportListView = animationSupportListView;
                return;
            }
            int maxTop = Integer.MAX_VALUE;
            int padding = 0;//getPaddingTop();
//            if (hasStories) {
//                padding -= AndroidUtilities.dp(DialogStoriesCell.HEIGHT_IN_DP);
//            }
            for (int i = 0; i < anchorListView.getChildCount(); i++) {
                View child = anchorListView.getChildAt(i);
                if (child instanceof DialogCell) {
                    DialogCell dialogCell = (DialogCell) child;
                    if (dialogCell.getDialogId() == rightSlidingDialogContainer.getCurrentFragmetDialogId()) {
                        selectedDialogView = dialogCell;
                    }
                    if (child.getTop() >= padding && dialogCell.getDialogId() != 0 && child.getTop() < maxTop) {
                        anchorView = (DialogCell) child;
                        maxTop = anchorView.getTop();
                    }
                }
            }
            if (selectedDialogView != null && getAdapter().getItemCount() * dp(70) > getMeasuredHeight() && (anchorView.getTop() - getPaddingTop()) > (getMeasuredHeight() - getPaddingTop()) / 2f) {
                anchorView = selectedDialogView;
            }
            this.animationSupportListView = animationSupportListView;

            if (anchorView != null) {
                if (animationSupportListView != null) {
                    int topPadding = this.topPadding;
                    animationSupportListView.setPadding(getPaddingLeft(), topPadding, getPaddingLeft(), getPaddingBottom());
                    if (anchorView != null) {
                        DialogsAdapter adapter = (DialogsAdapter) animationSupportListView.getAdapter();
                        int p = adapter.findDialogPosition(anchorView.getDialogId());
                        int offset = (int) (anchorView.getTop() - anchorListView.getPaddingTop() + scrollOffset);
                        if (p >= 0) {
                            boolean hasArchive = parentPage.dialogsType == DIALOGS_TYPE_DEFAULT && parentPage.archivePullViewState == ARCHIVE_ITEM_STATE_HIDDEN && hasHiddenArchive();
                            int fixedOffset = adapter.fixScrollGap(this, p, offset, hasArchive, hasStories, canShowFilterTabsView, opened);
                            ((LinearLayoutManager) animationSupportListView.getLayoutManager()).scrollToPositionWithOffset(p, fixedOffset);
                        }
                    }
                }
               // if (!backward) {
                    DialogsAdapter adapter = (DialogsAdapter) getAdapter();
                    int p = adapter.findDialogPosition(anchorView.getDialogId());
                    int offset = (int) (anchorView.getTop() - getPaddingTop());
                    if (backward && hasStories) {
                        offset += dp(DialogStoriesCell.HEIGHT_IN_DP);
                    }
                    if (backward) {
                        offset += dp(SEARCH_FIELD_HEIGHT);
                        // offset += canShowFilterTabsView ? dp(50) : 0;
                    }
                    if (p >= 0) {
                        ((LinearLayoutManager) getLayoutManager()).scrollToPositionWithOffset(p, offset);
                    }
               // }
            }
        }

        @Override
        public void updateClip(int[] clip) {
            int y = (int) (getPaddingTop() + scrollYOffset);
            clip[0] = y;
            clip[1] = y + getMeasuredHeight();
        }
    }

    private StoriesController getStoriesController() {
        return getMessagesController().getStoriesController();
    }

    private class SwipeController extends ItemTouchHelper.Callback {

        private RectF buttonInstance;
        private RecyclerView.ViewHolder currentItemViewHolder;
        private boolean swipingFolder;
        private boolean swipeFolderBack;
        private ViewPage parentPage;

        public SwipeController(ViewPage page) {
            parentPage = page;
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            if (waitingForDialogsAnimationEnd(parentPage) || parentLayout != null && parentLayout.isInPreviewMode() || rightSlidingDialogContainer.hasFragment() || communityId != 0) {
                return 0;
            }
            if (swipingFolder && swipeFolderBack) {
                if (viewHolder.itemView instanceof DialogCell) {
                    ((DialogCell) viewHolder.itemView).swipeCanceled = true;
                }
                swipingFolder = false;
                return 0;
            }
            if (!onlySelect && parentPage.isDefaultDialogType() && slidingView == null && viewHolder.itemView instanceof DialogCell) {
                DialogCell dialogCell = (DialogCell) viewHolder.itemView;
                long dialogId = dialogCell.getDialogId();
                if (actionBar.isActionModeShowed(null)) {
                    TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(dialogId);
                    if (!allowMoving || dialog == null || !isDialogPinned(dialog) || DialogObject.isFolderDialogId(dialogId)) {
                        return 0;
                    }
                    movingView = (DialogCell) viewHolder.itemView;
                    movingView.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    swipeFolderBack = false;
                    return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
                } else {
                    int currentDialogsType = initialDialogsType;
                    try {
                        currentDialogsType = parentPage.dialogsAdapter.getDialogsType();
                    } catch (Exception ignore) {
                    }
                    if ((filterTabsView != null && filterTabsView.getVisibility() == View.VISIBLE && SharedConfig.getChatSwipeAction(currentAccount) == SwipeGestureSettingsView.SWIPE_GESTURE_FOLDERS) || !allowSwipeDuringCurrentTouch || ((dialogId == getUserConfig().clientUserId || dialogId == 777000 || currentDialogsType == 7 || currentDialogsType == 8) && SharedConfig.getChatSwipeAction(currentAccount) == SwipeGestureSettingsView.SWIPE_GESTURE_ARCHIVE) || getMessagesController().isPromoDialog(dialogId, false) && getMessagesController().promoDialogType != MessagesController.PROMO_TYPE_PSA) {
                        return 0;
                    }
                    boolean canSwipeBack = folderId == 0 && (ChatObject.isCommunity(currentAccount, dialogId) || SharedConfig.getChatSwipeAction(currentAccount) == SwipeGestureSettingsView.SWIPE_GESTURE_MUTE || SharedConfig.getChatSwipeAction(currentAccount) == SwipeGestureSettingsView.SWIPE_GESTURE_READ || SharedConfig.getChatSwipeAction(currentAccount) == SwipeGestureSettingsView.SWIPE_GESTURE_PIN || SharedConfig.getChatSwipeAction(currentAccount) == SwipeGestureSettingsView.SWIPE_GESTURE_DELETE) && !rightSlidingDialogContainer.hasFragment();
                    if (SharedConfig.getChatSwipeAction(currentAccount) == SwipeGestureSettingsView.SWIPE_GESTURE_READ) {
                        MessagesController.DialogFilter filter = null;
                        if (viewPages[0].dialogsType == 7 || viewPages[0].dialogsType == 8) {
                            filter = getMessagesController().selectedDialogFilter[viewPages[0].dialogsType == 8 ? 1 : 0];
                        }
                        if (filter != null && (filter.flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_READ) != 0) {
                            TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(dialogId);
                            if (dialog != null && !filter.alwaysShow(currentAccount, dialog) && (dialog.unread_count > 0 || dialog.unread_mark)) {
                                canSwipeBack = false;
                            }
                        }
                    }
                    swipeFolderBack = false;
                    swipingFolder = (canSwipeBack && !DialogObject.isFolderDialogId(dialogCell.getDialogId())) || (SharedConfig.archiveHidden && DialogObject.isFolderDialogId(dialogCell.getDialogId()));
                    dialogCell.setSliding(true);
                    return makeMovementFlags(0, ItemTouchHelper.LEFT);
                }
            }
            return 0;
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
            if (!(target.itemView instanceof DialogCell)) {
                return false;
            }
            DialogCell dialogCell = (DialogCell) target.itemView;
            long dialogId = dialogCell.getDialogId();
            TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(dialogId);
            if (dialog == null || !isDialogPinned(dialog) || DialogObject.isFolderDialogId(dialogId)) {
                return false;
            }
            int fromIndex = source.getAdapterPosition();
            int toIndex = target.getAdapterPosition();
            if (parentPage.listView.getItemAnimator() == null) {
                parentPage.listView.setItemAnimator(parentPage.dialogsItemAnimator);
            }
            parentPage.dialogsAdapter.moveDialogs(parentPage.listView, fromIndex, toIndex);

            if (viewPages[0].dialogsType == 7 || viewPages[0].dialogsType == 8) {
                MessagesController.DialogFilter filter = getMessagesController().selectedDialogFilter[viewPages[0].dialogsType == 8 ? 1 : 0];
                if (!movingDialogFilters.contains(filter)) {
                    movingDialogFilters.add(filter);
                }
            } else {
                movingWas = true;
            }
            return true;
        }

        @Override
        public int convertToAbsoluteDirection(int flags, int layoutDirection) {
            if (swipeFolderBack) {
                return 0;
            }
            return super.convertToAbsoluteDirection(flags, layoutDirection);
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            if (viewHolder != null) {
                DialogCell dialogCell = (DialogCell) viewHolder.itemView;
                long dialogId = dialogCell.getDialogId();
                if (DialogObject.isFolderDialogId(dialogId)) {
                    parentPage.listView.toggleArchiveHidden(false, dialogCell);
                    return;
                }
                TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(dialogId);
                if (dialog == null) {
                    return;
                }

                if (!getMessagesController().isPromoDialog(dialogId, false) && folderId == 0 && SharedConfig.getChatSwipeAction(currentAccount) == SwipeGestureSettingsView.SWIPE_GESTURE_READ) {
                    ArrayList<Long> selectedDialogs = new ArrayList<>();
                    selectedDialogs.add(dialogId);
                    canReadCount = dialog.unread_count > 0 || dialog.unread_mark ? 1 : 0;
                    performSelectedDialogsAction(selectedDialogs, read, true, false);
                    return;
                }

                TLRPC.Chat chat = getMessagesController().getChat(-dialogId);
                if (ChatObject.isCommunity(chat)) {
                    ArrayList<Long> selectedDialogs = new ArrayList<>();
                    selectedDialogs.add(dialogId);
                    performSelectedDialogsAction(selectedDialogs, community_ungroup, true, false);
                    return;
                }

                slidingView = dialogCell;
                int position = viewHolder.getAdapterPosition();
                int count = parentPage.dialogsAdapter.getItemCount();
                Runnable finishRunnable = () -> {
                    if (frozenDialogsList == null) {
                        return;
                    }
                    frozenDialogsList.remove(dialog);
                    int pinnedNum = dialog.pinnedNum;
                    slidingView = null;
                    parentPage.listView.invalidate();
                    int lastItemPosition = parentPage.layoutManager.findLastVisibleItemPosition();
                    if (lastItemPosition == count - 1) {
                        parentPage.layoutManager.findViewByPosition(lastItemPosition).requestLayout();
                    }
                    if (getMessagesController().isPromoDialog(dialog.id, false)) {
                        getMessagesController().hidePromoDialog();
                        parentPage.dialogsItemAnimator.prepareForRemove();
                        parentPage.updateList(true);
                    } else {
                        int added = getMessagesController().addDialogToFolder(dialog.id, folderId == 0 ? 1 : 0, -1, 0);
                        if (added != 2 || position != 0) {
                            parentPage.dialogsItemAnimator.prepareForRemove();
                            parentPage.updateList(true);
                        }
                        if (folderId == 0) {
                            if (added == 2) {
                                if (SharedConfig.archiveHidden) {
                                    SharedConfig.toggleArchiveHidden();
                                }
                                parentPage.dialogsItemAnimator.prepareForRemove();
                                if (position == 0) {
                                    setDialogsListFrozen(true);
                                    parentPage.updateList(true);
                                    checkAnimationFinished();
                                } else {
                                    parentPage.updateList(true);
                                    if (!SharedConfig.archiveHidden && parentPage.layoutManager.findFirstVisibleItemPosition() == 0) {
                                        disableActionBarScrolling = true;
                                        parentPage.listView.smoothScrollBy(0, -dp(SharedConfig.useThreeLinesLayout ? 76 : 70));
                                    }
                                }
                                ArrayList<TLRPC.Dialog> dialogs = getDialogsArray(currentAccount, parentPage.dialogsType, folderId, false);
                                frozenDialogsList.add(0, dialogs.get(0));
                                parentPage.updateList(true);
                                AndroidUtilities.runOnUIThread(() -> setDialogsListFrozen(false), 300);
                            } else if (added == 1) {
                                RecyclerView.ViewHolder holder = parentPage.listView.findViewHolderForAdapterPosition(0);
                                if (holder != null && holder.itemView instanceof DialogCell) {
                                    DialogCell cell = (DialogCell) holder.itemView;
                                    cell.checkCurrentDialogIndex(true);
                                    cell.animateArchiveAvatar();
                                }
                                AndroidUtilities.runOnUIThread(() -> setDialogsListFrozen(false), 300);
                            }
                            SharedPreferences preferences = MessagesController.getGlobalMainSettings();
                            boolean hintShowed = preferences.getBoolean("archivehint_l", false) || SharedConfig.archiveHidden;
                            if (!hintShowed) {
                                preferences.edit().putBoolean("archivehint_l", true).commit();
                            }
                            final UndoView undoView = getUndoView();
                            if (undoView != null) {
                                undoView.showWithAction(dialog.id, hintShowed ? UndoView.ACTION_ARCHIVE : UndoView.ACTION_ARCHIVE_HINT, null, () -> {
                                    dialogsListFrozen = true;
                                    getMessagesController().addDialogToFolder(dialog.id, 0, pinnedNum, 0);
                                    dialogsListFrozen = false;
                                    ArrayList<TLRPC.Dialog> dialogs = getMessagesController().getDialogs(0);
                                    int index = dialogs.indexOf(dialog);
                                    if (index >= 0) {
                                        ArrayList<TLRPC.Dialog> archivedDialogs = getMessagesController().getDialogs(1);
                                        if (!archivedDialogs.isEmpty() || index != 1) {
                                            setDialogsListFrozen(true);
                                            parentPage.dialogsItemAnimator.prepareForRemove();
                                            parentPage.updateList(true);
                                            checkAnimationFinished();
                                        }
                                        if (archivedDialogs.isEmpty()) {
                                            dialogs.remove(0);
                                            if (index == 1) {
                                                setDialogsListFrozen(true);
                                                parentPage.updateList(true);
                                                checkAnimationFinished();
                                            } else {
                                                if (!frozenDialogsList.isEmpty()) {
                                                    frozenDialogsList.remove(0);
                                                }
                                                parentPage.dialogsItemAnimator.prepareForRemove();
                                                parentPage.updateList(true);
                                            }
                                        }
                                    } else {
                                        parentPage.updateList(false);
                                    }
                                });
                            }
                        }
                        if (folderId != 0 && frozenDialogsList.isEmpty()) {
                            parentPage.listView.setEmptyView(null);
                            parentPage.progressView.setVisibility(View.INVISIBLE);
                        }
                    }
                };
                setDialogsListFrozen(true);
                if (Utilities.random.nextInt(1000) == 1) {
                    if (pacmanAnimation == null) {
                        pacmanAnimation = new PacmanAnimation(parentPage.listView);
                    }
                    pacmanAnimation.setFinishRunnable(finishRunnable);
                    pacmanAnimation.start();
                } else {
                    finishRunnable.run();
                }
            } else {
                slidingView = null;
            }
        }

        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            if (viewHolder != null) {
                parentPage.listView.hideSelector(false);
            }
            currentItemViewHolder = viewHolder;
            if (viewHolder != null && viewHolder.itemView instanceof DialogCell) {
                ((DialogCell) viewHolder.itemView).swipeCanceled = false;
            }
            super.onSelectedChanged(viewHolder, actionState);
        }

        @Override
        public long getAnimationDuration(@NonNull RecyclerView recyclerView, int animationType, float animateDx, float animateDy) {
            if (animationType == ItemTouchHelper.ANIMATION_TYPE_SWIPE_CANCEL) {
                return 200;
            } else if (animationType == ItemTouchHelper.ANIMATION_TYPE_DRAG) {
                if (movingView != null) {
                    View view = movingView;
                    AndroidUtilities.runOnUIThread(() -> view.setBackgroundDrawable(null), parentPage.dialogsItemAnimator.getMoveDuration());
                    movingView = null;
                }
            }
            return super.getAnimationDuration(recyclerView, animationType, animateDx, animateDy);
        }

        @Override
        public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
            return 0.45f;
        }

        @Override
        public float getSwipeEscapeVelocity(float defaultValue) {
            return 3500;
        }

        @Override
        public float getSwipeVelocityThreshold(float defaultValue) {
            return Float.MAX_VALUE;
        }
    }

    public interface DialogsActivityDelegate {
        boolean didSelectDialogs(DialogsActivity fragment, ArrayList<MessagesStorage.TopicKey> dids, CharSequence message, boolean param, boolean notify, int scheduleDate, int scheduleRepeatPeriod, TopicsFragment topicsFragment);

        default boolean canSelectStories() { return false; }
        default boolean didSelectStories(DialogsActivity fragment) { return false; }
    }

    public DialogsActivity(Bundle args) {
        super(args);

        iBlur3SourceColor = new BlurredBackgroundSourceColor();
        iBlur3SourceColor.setColor(getThemedColor(Theme.key_windowBackgroundWhite));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            scrollableViewNoiseSuppressor = new DownscaleScrollableNoiseSuppressor();
            iBlur3SourceGlassFrosted = new BlurredBackgroundSourceRenderNode(null);
            iBlur3SourceGlassFrosted.setupRenderer(new RenderNodeWithHash.Renderer() {
                @Override
                public void renderNodeCalculateHash(IBlur3Hash hash) {
                    hash.add(getThemedColor(Theme.key_windowBackgroundWhite));
                    hash.add(SharedConfig.chatBlurEnabled());

                    if (SharedConfig.chatBlurEnabled()) {
                        TopicsFragment topicsFragment = null;
                        if (rightSlidingDialogContainer != null && rightSlidingDialogContainer.getFragment() instanceof TopicsFragment) {
                            topicsFragment = (TopicsFragment) rightSlidingDialogContainer.getFragment();
                        }

                        if (topicsFragment != null && topicsFragment.getFragmentView() != null && !searching) {
                            hash.unsupported();
                            return;
                        }
                    }
                }

                @Override
                public void renderNodeUpdateDisplayList(Canvas canvas) {
                    final int width = fragmentView.getMeasuredWidth();
                    final int height = fragmentView.getMeasuredHeight();

                    canvas.drawColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    if (SharedConfig.chatBlurEnabled()) {
                        TopicsFragment topicsFragment = null;
                        if (rightSlidingDialogContainer != null && rightSlidingDialogContainer.getFragment() instanceof TopicsFragment) {
                            topicsFragment = (TopicsFragment) rightSlidingDialogContainer.getFragment();
                        }

                        if (topicsFragment != null && topicsFragment.getFragmentView() != null && !searching) {
                            BlurredBackgroundSource source = topicsFragment.getFrostedGlassSource();
                            if (source != null) {
                                canvas.save();
                                canvas.translate(
                                        topicsFragment.getFragmentView().getTranslationX(),
                                        topicsFragment.getFragmentView().getTranslationY());
                                source.draw(canvas, 0, 0, width, height);
                                canvas.restore();
                            }
                        }
                        scrollableViewNoiseSuppressor.draw(canvas, DownscaleScrollableNoiseSuppressor.DRAW_FROSTED_GLASS);
                    }
                }
            });

            iBlur3SourceGlass = new BlurredBackgroundSourceRenderNode(null);
            iBlur3SourceGlass.setupRenderer(new RenderNodeWithHash.Renderer() {
                @Override
                public void renderNodeCalculateHash(IBlur3Hash hash) {
                    hash.add(getThemedColor(Theme.key_windowBackgroundWhite));
                    hash.add(SharedConfig.chatBlurEnabled());

                    if (SharedConfig.chatBlurEnabled()) {
                        TopicsFragment topicsFragment = null;
                        if (rightSlidingDialogContainer != null && rightSlidingDialogContainer.getFragment() instanceof TopicsFragment) {
                            topicsFragment = (TopicsFragment) rightSlidingDialogContainer.getFragment();
                        }

                        if (topicsFragment != null && topicsFragment.getFragmentView() != null && !searching) {
                            hash.unsupported();
                            return;
                        }
                    }
                }

                @Override
                public void renderNodeUpdateDisplayList(Canvas canvas) {
                    final int width = fragmentView.getMeasuredWidth();
                    final int height = fragmentView.getMeasuredHeight();

                    canvas.drawColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    if (SharedConfig.chatBlurEnabled()) {
                        TopicsFragment topicsFragment = null;
                        if (rightSlidingDialogContainer != null && rightSlidingDialogContainer.getFragment() instanceof TopicsFragment) {
                            topicsFragment = (TopicsFragment) rightSlidingDialogContainer.getFragment();
                        }

                        if (topicsFragment != null && topicsFragment.getFragmentView() != null && !searching) {
                            BlurredBackgroundSource source = topicsFragment.getGlassSource();
                            if (source != null) {
                                canvas.save();
                                canvas.translate(
                                        topicsFragment.getFragmentView().getTranslationX(),
                                        topicsFragment.getFragmentView().getTranslationY());
                                source.draw(canvas, 0, 0, width, height);
                                canvas.restore();
                            }
                        }

                        scrollableViewNoiseSuppressor.draw(canvas, DownscaleScrollableNoiseSuppressor.DRAW_GLASS);
                    }
                }
            });

            iBlur3FactoryFrostedLiquidGlass = new BlurredBackgroundDrawableViewFactory(iBlur3SourceGlassFrosted);
            iBlur3FactoryFrostedLiquidGlass.setLiquidGlassEffectAllowed(LiteMode.isEnabled(LiteMode.FLAG_LIQUID_GLASS));
            iBlur3FactoryLiquidGlass = new BlurredBackgroundDrawableViewFactory(iBlur3SourceGlass);
            iBlur3FactoryLiquidGlass.setLiquidGlassEffectAllowed(LiteMode.isEnabled(LiteMode.FLAG_LIQUID_GLASS));
            iBlur3FactoryBlur = new BlurredBackgroundDrawableViewFactory(iBlur3SourceGlassFrosted);
        } else {
            scrollableViewNoiseSuppressor = null;
            iBlur3SourceGlassFrosted = null;
            iBlur3SourceGlass = null;
            iBlur3FactoryFrostedLiquidGlass = new BlurredBackgroundDrawableViewFactory(iBlur3SourceColor);
            iBlur3FactoryLiquidGlass = new BlurredBackgroundDrawableViewFactory(iBlur3SourceColor);
            iBlur3FactoryBlur = new BlurredBackgroundDrawableViewFactory(iBlur3SourceColor);
        }
        iBlur3FactoryFade = new BlurredBackgroundDrawableViewFactory(iBlur3SourceColor);
    }

    private MainTabsActivityController mainTabsActivityController;

    public void setMainTabsActivityController(MainTabsActivityController controller) {
        mainTabsActivityController = controller;
    }


    private NotificationCenter.ObserversGroup observersGroup;
    private NotificationCenter.ObserversGroup globalObserversGroup;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        if (arguments != null) {
            onlySelect = arguments.getBoolean("onlySelect", false);
            canSelectTopics = arguments.getBoolean("canSelectTopics", false);
            cantSendToChannels = arguments.getBoolean("cantSendToChannels", false);
            initialDialogsType = arguments.getInt("dialogsType", DIALOGS_TYPE_DEFAULT);
            isQuote = arguments.getBoolean("quote", false);
            isReplyTo = arguments.getBoolean("reply_to", false);
            replyMessageAuthor = arguments.getLong("reply_to_author", 0L);
            forwardOriginalChannel = arguments.getLong("forward_into_channel", 0L);
            selectAlertString = arguments.getString("selectAlertString");
            selectAlertStringGroup = arguments.getString("selectAlertStringGroup");
            addToGroupAlertString = arguments.getString("addToGroupAlertString");
            allowSwitchAccount = arguments.getBoolean("allowSwitchAccount");
            checkCanWrite = arguments.getBoolean("checkCanWrite", true);
            afterSignup = arguments.getBoolean("afterSignup", false);
            folderId = arguments.getInt("folderId", 0);
            communityId = arguments.getLong("community_id", 0);
            if (communityId != 0) {
                community = getMessagesController().getChat(communityId);
                communityFull = getMessagesController().getChatFull(communityId);
            }
            resetDelegate = arguments.getBoolean("resetDelegate", true);
            messagesCount = arguments.getInt("messagesCount", 0);
            hasPoll = arguments.getInt("hasPoll", 0);
            hasInvoice = arguments.getBoolean("hasInvoice", false);
            showSetPasswordConfirm = arguments.getBoolean("showSetPasswordConfirm", showSetPasswordConfirm);
            otherwiseReloginDays = arguments.getInt("otherwiseRelogin");
            allowGroups = arguments.getBoolean("allowGroups", true);
            allowMegagroups = arguments.getBoolean("allowMegagroups", true);
            allowLegacyGroups = arguments.getBoolean("allowLegacyGroups", true);
            allowChannels = arguments.getBoolean("allowChannels", true);
            allowUsers = arguments.getBoolean("allowUsers", true);
            allowBots = arguments.getBoolean("allowBots", true);
            closeFragment = arguments.getBoolean("closeFragment", true);
            allowGlobalSearch = arguments.getBoolean("allowGlobalSearch", true);
            hasMainTabs = arguments.getBoolean("hasMainTabs", false);

            byte[] requestPeerTypeBytes = arguments.getByteArray("requestPeerType");
            if (requestPeerTypeBytes != null) {
                try {
                    SerializedData buffer = new SerializedData(requestPeerTypeBytes);
                    requestPeerType = TLRPC.RequestPeerType.TLdeserialize(buffer, buffer.readInt32(true), true);
                    buffer.cleanup();
                } catch (Exception e) {
                }
            }
            requestPeerBotId = arguments.getLong("requestPeerBotId", 0);
        }

        if (initialDialogsType == DIALOGS_TYPE_DEFAULT) {
            askAboutContacts = MessagesController.getGlobalNotificationsSettings().getBoolean("askAboutContacts", true);
            SharedConfig.loadProxyList();
        }

        observersGroup = getNotificationCenter().createObserversGroup(this);
        globalObserversGroup = NotificationCenter.getGlobalInstance().createObserversGroup(this);

        if (searchString == null) {
            currentConnectionState = getConnectionsManager().getConnectionState();

            globalObserversGroup.add(NotificationCenter.emojiLoaded);
            if (!onlySelect) {
                globalObserversGroup.add(NotificationCenter.closeSearchByActiveAction);
                globalObserversGroup.add(NotificationCenter.proxySettingsChanged);
                observersGroup.add(NotificationCenter.filterSettingsUpdated);
                observersGroup.add(NotificationCenter.dialogsUnreadCounterChanged);
            }
            observersGroup
                .add(NotificationCenter.dialogsNeedReload)
                .add(NotificationCenter.dialogFiltersUpdated)
                .add(NotificationCenter.updateInterfaces)
                .add(NotificationCenter.encryptedChatUpdated)
                .add(NotificationCenter.contactsDidLoad)
                .add(NotificationCenter.appDidLogout)
                .add(NotificationCenter.openedChatChanged)
                .add(NotificationCenter.notificationsSettingsUpdated)
                .add(NotificationCenter.messageReceivedByAck)
                .add(NotificationCenter.messageReceivedByServer)
                .add(NotificationCenter.messageSendError)
                .add(NotificationCenter.needReloadRecentDialogsSearch)
                .add(NotificationCenter.replyMessagesDidLoad)
                .add(NotificationCenter.topicsDidLoaded)
                .add(NotificationCenter.reloadHints)
                .add(NotificationCenter.didUpdateConnectionState)
                .add(NotificationCenter.onDownloadingFilesChanged)
                .add(NotificationCenter.needDeleteDialog)
                .add(NotificationCenter.folderBecomeEmpty)
                .add(NotificationCenter.newSuggestionsAvailable)
                .add(NotificationCenter.dialogsUnreadReactionsCounterChanged)
                .add(NotificationCenter.dialogsUnreadPollVotesCounterChanged)
                .add(NotificationCenter.forceImportContactsStart)
                .add(NotificationCenter.userEmojiStatusUpdated)
                .add(NotificationCenter.currentUserPremiumStatusChanged);

            globalObserversGroup.add(NotificationCenter.didSetPasscode);
        }
        observersGroup
            .add(NotificationCenter.messagesDeleted)
            .add(NotificationCenter.onDatabaseMigration)
            .add(NotificationCenter.onDatabaseOpened)
            .add(NotificationCenter.chatInfoDidLoad)
            .add(NotificationCenter.didClearDatabase)
            .add(NotificationCenter.onDatabaseReset)
            .add(NotificationCenter.storiesUpdated)
            .add(NotificationCenter.storiesEnabledUpdate)
            .add(NotificationCenter.unconfirmedAuthUpdate)
            .add(NotificationCenter.premiumPromoUpdated)
            .add(NotificationCenter.starBalanceUpdated)
            .add(NotificationCenter.starSubscriptionsLoaded)
            .add(NotificationCenter.communityPendingRequestsUpdate)
            .add(NotificationCenter.communitySwitchedCollapsed)
            .add(NotificationCenter.appConfigUpdated)
            .add(NotificationCenter.activeAuctionsUpdated);

        if (initialDialogsType == DIALOGS_TYPE_DEFAULT) {
            observersGroup.add(NotificationCenter.chatlistFolderUpdate);
            observersGroup.add(NotificationCenter.dialogTranslate);
        }

        loadDialogs(getAccountInstance());
        getMessagesController().getStoriesController().loadAllStories();
        getMessagesController().loadPinnedDialogs(folderId, 0, null);
        if (databaseMigrationHint != null && !getMessagesStorage().isDatabaseMigrationInProgress()) {
            View localView = databaseMigrationHint;
            if (localView.getParent() != null) {
                ((ViewGroup) localView.getParent()).removeView(localView);
            }
            databaseMigrationHint = null;
        }
        if (isArchive()) {
            getMessagesController().getStoriesController().loadHiddenStories();
        } else {
            getMessagesController().getStoriesController().loadStories();
        }

        getContactsController().loadGlobalPrivacySetting();

        if (getMessagesController().savedViewAsChats) {
            getMessagesController().getSavedMessagesController().preloadDialogs(true);
        }

        if (communityId != 0) {
            getMessagesController().loadFullChat(communityId, 0, true);
        }

        BirthdayController.getInstance(currentAccount).check();
        additionNavigationBarHeight = hasMainTabs ? dp(MAIN_TABS_HEIGHT_WITH_MARGINS) : 0;
        additionFloatingButtonOffset = hasMainTabs ? dp(DialogsActivity.MAIN_TABS_HEIGHT + DialogsActivity.MAIN_TABS_MARGIN) : 0;

        return true;
    }

    public static void loadDialogs(AccountInstance accountInstance) {
        int currentAccount = accountInstance.getCurrentAccount();
        if (!dialogsLoaded[currentAccount]) {
            MessagesController messagesController = accountInstance.getMessagesController();
            messagesController.loadGlobalNotificationsSettings();
            messagesController.loadDialogs(0, 0, 100, true);
            messagesController.loadHintDialogs();
            messagesController.loadUserInfo(accountInstance.getUserConfig().getCurrentUser(), false, 0);
            accountInstance.getContactsController().checkInviteText();
            accountInstance.getMediaDataController().checkAllMedia(false);
            AndroidUtilities.runOnUIThread(() -> accountInstance.getDownloadController().loadDownloadingFiles(), 200);
            for (String emoji : messagesController.diceEmojies) {
                accountInstance.getMediaDataController().loadStickersByEmojiOrName(emoji, true, true);
            }
            dialogsLoaded[currentAccount] = true;
        }
    }

    private Drawable premiumStar;

    public void updateStatus(TLRPC.User user, boolean animated) {
        if (dialogStoriesCell != null) {
            dialogStoriesCell.updateStatus(user, animated);
        }
        if (statusDrawable == null || actionBar == null) {
            return;
        }
        Long emojiStatusId = UserObject.getEmojiStatusDocumentId(user);
        statusDrawableGiftId = null;
        if (emojiStatusId != null) {
            final boolean isCollectible = user.emoji_status instanceof TLRPC.TL_emojiStatusCollectible;
            statusDrawable.set(emojiStatusId, animated);
            statusDrawable.setParticles(isCollectible, animated);
            if (isCollectible) {
                statusDrawableGiftId = ((TLRPC.TL_emojiStatusCollectible) user.emoji_status).collectible_id;
            }
            actionBar.setRightDrawableOnClick(e -> {
                if (dialogStoriesCellVisible && dialogStoriesCell != null && !dialogStoriesCell.isExpanded()) {
                    scrollToTop(true, true);
                    return;
                }
                showSelectStatusDialog();
            });
            SelectAnimatedEmojiDialog.preload(currentAccount);
        } else if (user != null && MessagesController.getInstance(currentAccount).isPremiumUser(user)) {
            if (premiumStar == null) {
                premiumStar = getContext().getResources().getDrawable(R.drawable.msg_premium_liststar).mutate();
                premiumStar = new AnimatedEmojiDrawable.WrapSizeDrawable(premiumStar, dp(18), dp(18)) {
                    @Override
                    public void draw(@NonNull Canvas canvas) {
                        canvas.save();
                        canvas.translate(dp(-2), dp(1));
                        super.draw(canvas);
                        canvas.restore();
                    }
                };
            }
            premiumStar.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_profile_verifiedBackground), PorterDuff.Mode.MULTIPLY));
            statusDrawable.set(premiumStar, animated);
            statusDrawable.setParticles(false, animated);
            actionBar.setRightDrawableOnClick(e -> {
                if (dialogStoriesCellVisible && dialogStoriesCell != null && !dialogStoriesCell.isExpanded()) {
                    scrollToTop(true, true);
                    return;
                }
                showSelectStatusDialog();
            });
            SelectAnimatedEmojiDialog.preload(currentAccount);
        } else {
            statusDrawable.set((Drawable) null, animated);
            statusDrawable.setParticles(false, animated);
            actionBar.setRightDrawableOnClick(null);
        }
        statusDrawable.setColor(getThemedColor(Theme.key_profile_verifiedBackground));
        if (animatedStatusView != null) {
            animatedStatusView.setColor(getThemedColor(Theme.key_profile_verifiedBackground));
        }
        if (selectAnimatedEmojiDialog != null && selectAnimatedEmojiDialog.getContentView() instanceof SelectAnimatedEmojiDialog) {
            SimpleTextView textView = actionBar.getTitleTextView();
            ((SelectAnimatedEmojiDialog) selectAnimatedEmojiDialog.getContentView()).setScrimDrawable(textView != null && textView.getRightDrawable() == statusDrawable ? statusDrawable : null, textView);
        }
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        if (observersGroup != null) {
            observersGroup.removeAllObservers();
            observersGroup = null;
        }
        if (globalObserversGroup != null) {
            globalObserversGroup.removeAllObservers();
            globalObserversGroup = null;
        }

        if (commentView != null) {
            commentView.onDestroy();
        }
        if (shareTopView != null) {
            shareTopView.stopHintRotation();
        }
        if (shareLinkSearchRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(shareLinkSearchRunnable);
            shareLinkSearchRunnable = null;
        }
        if (undoView[0] != null) {
            undoView[0].hide(true, 0);
        }
        notificationsLocker.unlock();
        delegate = null;
        SuggestClearDatabaseBottomSheet.dismissDialog();
    }

    @Override
    public boolean dismissDialogOnPause(Dialog dialog) {
        return !(dialog instanceof BotWebViewSheet) && super.dismissDialogOnPause(dialog);
    }

    @Override
    public ActionBar createActionBar(Context context) {
        ActionBar actionBar = new ActionBar(context, resourceProvider) {

            @Override
            public void setTranslationY(float translationY) {
                if (translationY != getTranslationY() && fragmentView != null) {
                    fragmentView.invalidate();
                }
                super.setTranslationY(translationY);
            }

            @Override
            protected boolean shouldClipChild(View child) {
                return super.shouldClipChild(child) || child == doneItem;
            }

            @Override
            protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
                if (inPreviewMode && avatarContainer != null && child != avatarContainer) {
                    return false;
                }
                return super.drawChild(canvas, child, drawingTime);
            }

            @Override
            public boolean dispatchTouchEvent(MotionEvent ev) {
                if (fragmentSearchField != null && fragmentSearchField.getAlpha() > 0 && animatorSearchVisible.getValue()) {
                    return false;
                }

                return super.dispatchTouchEvent(ev);
            }

            @Override
            public void closeSearchField(boolean closeKeyboard) {
                fragmentSearchField.editText.getText().clear();
                if (closeKeyboard && fragmentSearchField.editText.isFocused()) {
                    AndroidUtilities.hideKeyboard(fragmentSearchField.editText);
                }
                fragmentSearchField.editText.clearFocus();
                fragmentSearchFieldWatcher.toggleSearch(false);
            }

            @Override
            protected boolean onSearchChangedIgnoreTitles() {
                return rightSlidingDialogContainer != null && rightSlidingDialogContainer.hasFragment();
            }

            @Override
            public void onSearchFieldVisibilityChanged(boolean visible) {
                if (rightSlidingDialogContainer != null && rightSlidingDialogContainer.hasFragment()) {
                    if (getBackButton() != null) {
                        getBackButton().animate().alpha(visible ? 1f : 0f).start();
                    }
                }
                super.onSearchFieldVisibilityChanged(visible);
            }

            @Override
            public void showActionMode(boolean animated, View extraView, View showingView, View[] hidingViews, boolean[] hideView, View translationView, int translation) {
                super.showActionMode(animated, extraView, showingView, hidingViews, hideView, translationView, translation);
                animatorActionModeVisible.setValue(true, animated);
            }

            @Override
            public void hideActionMode() {
                super.hideActionMode();
                animatorActionModeVisible.setValue(false, true);
            }
        };
        actionBar.setAllowOverlayTitle(true);
        actionBar.setUseContainerForTitles();
        actionBar.setItemsBackgroundColor(getThemedColor(Theme.key_actionBarDefaultSelector), false);
        actionBar.setItemsBackgroundColor(getThemedColor(Theme.key_actionBarActionModeDefaultSelector), true);
        actionBar.setItemsColor(getThemedColor(Theme.key_actionBarDefaultIcon), false);
        actionBar.setItemsColor(getThemedColor(Theme.key_actionBarActionModeDefaultIcon), true);
        actionBar.createAdditionalSubTitleOverlayContainer();
        actionBar.getAdditionalSubTitleOverlayContainer().setTranslationX(dp(4));
        actionBar.getAdditionalSubTitleOverlayContainer().setTranslationY(-dp(3));

        if (inPreviewMode || AndroidUtilities.isTablet() && folderId != 0 && !isArchive()) {
            actionBar.setOccupyStatusBar(false);
        }
        return actionBar;
    }

    @Override
    public void setTitleOverlayText(String title, int titleId, Runnable action) {
        super.setTitleOverlayText(title, titleId, action);
        if (actionBar != null && selectAnimatedEmojiDialog != null && selectAnimatedEmojiDialog.getContentView() instanceof SelectAnimatedEmojiDialog) {
            SimpleTextView textView = actionBar.getTitleTextView();
            ((SelectAnimatedEmojiDialog) selectAnimatedEmojiDialog.getContentView()).setScrimDrawable(textView != null && textView.getRightDrawable() == statusDrawable ? statusDrawable : null, textView);
        }
        if (dialogStoriesCell != null) {
            dialogStoriesCell.setTitleOverlayText(title, titleId);
        }
    }

    @Override
    public View createView(final Context context) {
        searching = false;
        searchWas = false;
        wasDrawn = false;
        pacmanAnimation = null;
        filterTabsView = null;
        selectedDialogs.clear();

        maximumVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();

        AndroidUtilities.runOnUIThread(() -> Theme.createChatResources(context, false));

        authHintCell = null;
        activeGiftAuctionsHintCell = null;
        dialogsHintCell = null;
        communityPendingRequests = null;
        topPanelLayout = null;

        ActionBarMenu menu = actionBar.createMenu();
        menu.setTranslationX(-dp(5));
        searchItem = menu.addItem(0, R.drawable.outline_header_search).setIsSearchField(true, false);
        searchItem.setOnClickListener(v -> {
            showSearch(true, false, true);
            fragmentSearchFieldWatcher.toggleSearch(true);
            AndroidUtilities.runOnUIThread(() -> {
                fragmentSearchField.editText.requestFocus();
                AndroidUtilities.showKeyboard(fragmentSearchField.editText);
            }, 100);

            /*
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(ObjectAnimator.ofFloat(this, SCROLL_Y, hasStories ? -dp(DialogStoriesCell.HEIGHT_IN_DP) : 0));
            animatorSet.setInterpolator(CubicBezierInterpolator.DEFAULT);
            animatorSet.setDuration(250);
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    showSearch(true, false, true);
                    fragmentSearchFieldWatcher.toggleSearch(true);
                    fragmentSearchField.editText.requestFocus();
                    AndroidUtilities.showKeyboard(fragmentSearchField.editText);
                }
            });
            animatorSet.start();
            */
        });
        if (initialDialogsType == DIALOGS_TYPE_ADD_USERS_TO || isArchive() && getDialogsArray(currentAccount, initialDialogsType, folderId, false).isEmpty()) {
            searchItem.setVisibility(View.GONE);
        }
        searchItem.setVisibility(View.GONE);

        if (!onlySelect && searchString == null && folderId == 0 && communityId == 0) {
            doneItem = new ActionBarMenuItem(context, null, getThemedColor(Theme.key_actionBarDefaultSelector), getThemedColor(Theme.key_actionBarDefaultIcon), true);
            doneItem.setText(LocaleController.getString(R.string.Done).toUpperCase());
            actionBar.addView(doneItem, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.RIGHT, 0, 0, 10, 0));
            doneItem.setOnClickListener(v -> {
                filterTabsView.setIsEditing(false);
                showDoneItem(false);
            });
            doneItem.setAlpha(0.0f);
            doneItem.setVisibility(View.GONE);
            proxyDrawable = new ProxyDrawable(context);
            proxyMenuSubItem = new ActionBarMenuSubItem(context, false, true, resourceProvider);
            proxyMenuSubItem.setItemHeight(56);
            proxyMenuSubItem.setTextAndIcon(getString(R.string.MenuProxyTitle), 0, proxyDrawable);
            proxyMenuSubItem.setContentDescription(getString(R.string.ProxySettings));

            passcodeItem = menu.addItem(1, R.drawable.outline_header_lock_24);
            passcodeItem.setContentDescription(getString(R.string.AccDescrPasscodeLock));

            downloadsItem = menu.addItem(3, new ColorDrawable(Color.TRANSPARENT));
            downloadsItem.addView(downloadProgressIcon = new DownloadProgressIcon(currentAccount, context));
            downloadsItem.setContentDescription(getString(R.string.DownloadsTabs));
            downloadsItem.setVisibility(View.GONE);

            updateProxyButton(false, false);
        }

        fragmentSearchField = new FragmentSearchField(context, resourceProvider) {
            @Override
            public boolean dispatchTouchEvent(MotionEvent ev) {
                if (ev.getAction() == MotionEvent.ACTION_DOWN && getAlpha() < 0.25f) {
                    return false;
                }
                return super.dispatchTouchEvent(ev);
            }
        };
        fragmentSearchField.setPadding(dp(4), dp(4), dp(4), dp(4));
        fragmentSearchField.setPivotX(0);
        fragmentSearchField.setPivotY(0);
        if (initialDialogsType == DIALOGS_TYPE_DEFAULT) {
            speedItem = menu.addItem(-47, R.drawable.avd_speed);
            AndroidUtilities.removeFromParent(speedItem);
            speedItem.setOnClickListener(v -> showDialog(new PremiumFeatureBottomSheet(DialogsActivity.this, PremiumPreviewFragment.PREMIUM_FEATURE_DOWNLOAD_SPEED, true)));

            fragmentSearchField.addAdditionalIcon(speedItem);
            fragmentSearchField.updateColors();
        }

        fragmentSearchField.setCloseButtonOnClickListener(() -> {
            if (searchViewPager != null && searchViewPager.actionModeShowing()) {
                searchViewPager.hideActionMode();
                return;
            }

            fragmentSearchField.editText.getText().clear();
            AndroidUtilities.hideKeyboard(fragmentSearchField.editText);
            fragmentSearchField.editText.clearFocus();
            fragmentSearchFieldWatcher.toggleSearch(false);
        });
        fragmentSearchField.editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                fragmentSearchFieldWatcher.toggleSearch(true);
            }
        });
        fragmentSearchField.editText.addTextChangedListener(fragmentSearchFieldWatcher = new SearchTextWatcher(fragmentSearchField.editText, new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
            @Override
            public void onSearchExpand() {
                searching = true;
                if (switchItem != null) {
                    switchItem.setVisibility(View.GONE);
                }
                createSearchViewPager();
                if (viewPages[0] != null) {
                    if (searchString != null) {
                        viewPages[0].listView.hide();
                        if (searchViewPager != null) {
                            searchViewPager.searchListView.show();
                        }
                    }
                    if (!onlySelect) {
                        if (storyHint != null) {
                            storyHint.hide();
                        }
                        if (storyPremiumHint != null) {
                            storyPremiumHint.hide();
                        }
                    }
                }
                if (dialogStoriesCell != null && dialogStoriesCell.getPremiumHint() != null) {
                    dialogStoriesCell.getPremiumHint().hide();
                }
                if (!hasStories) {
                    setScrollY(0);
                }
                updateProxyButton(false, false);
                actionBar.setBackButtonContentDescription(getString(R.string.AccDescrGoBack));
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needCheckSystemBarColors);
                blur3_InvalidateBlur();
                if (searchViewPager != null) {
                    searchViewPager.onShown();
                }
                if ((searchViewPager != null && searchViewPager.dialogsSearchAdapter != null && searchViewPager.dialogsSearchAdapter.hasRecentSearch()) || getMessagesController().getTotalDialogsCount() > 10 || searchFiltersWasShowed || hasStories) {
                    searchWas = true;
                    if (!searchIsShowed) {
                        showSearch(true, false, true);
                    }
                }
                fragmentSearchField.setCloseButtonVisible(true);
                updateFloatingButtonVisibility(true);
                checkUi_mainTabsVisible();
            }

            @Override
            public boolean canCollapseSearch() {
                if (switchItem != null) {
                    switchItem.setVisibility(View.VISIBLE);
                }
                if (searchString != null) {
                    finishFragment();
                    return false;
                }
                return true;
            }

            @Override
            public void onSearchCollapse() {
                if (fragmentSearchField != null) {
                    fragmentSearchField.clearSearchFiltersWithCallback();
                }

                searching = false;
                searchWas = false;
                if (viewPages[0] != null) {
                    viewPages[0].listView.setEmptyView(folderId == 0 ? viewPages[0].progressView : null);
                    showSearch(false, false, true);
                }
                updateProxyButton(false, false);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needCheckSystemBarColors, true);
                fragmentSearchField.setCloseButtonVisible(false);
                updateFloatingButtonVisibility(true);
                checkUi_mainTabsVisible();
                blur3_InvalidateBlur();
            }

            @Override
            public void onTextChanged(EditText editText) {
                String text = editText.getText().toString();
                if (!text.isEmpty() || (searchViewPager != null && searchViewPager.dialogsSearchAdapter != null && searchViewPager.dialogsSearchAdapter.hasRecentSearch()) || searchFiltersWasShowed || hasStories) {
                    searchWas = true;
                    if (!searchIsShowed) {
                        showSearch(true, false, true);
                    }
                }
                if (searchViewPager != null) {
                    searchViewPager.onTextChanged(text);
                }
            }

            @Override
            public boolean canToggleSearch() {
                return !actionBar.isActionModeShowed() && databaseMigrationHint == null;// && !rightSlidingDialogContainer.hasFragment();
            }
        }));
        fragmentSearchField.setSearchFiltersListener(new FragmentSearchField.SearchFiltersListener() {
            @Override
            public void onSearchFilterCleared(FiltersView.MediaFilterData filterData) {
                if (!searchIsShowed) {
                    return;
                }
                if (searchViewPager != null) {
                    searchViewPager.removeSearchFilter(filterData);
                    searchViewPager.onTextChanged(searchItem.getSearchField().getText().toString());
                }

                updateFiltersView(true, null, null, false, true);
                fragmentSearchFieldWatcher.listener.onTextChanged(fragmentSearchField.editText);
            }

            @Override
            public void hideActionMode() {
                if (searchViewPager != null) {
                    searchViewPager.hideActionMode();
                }
            }
        });
        fragmentSearchFieldWatcher.setDoNotCloseAfterFieldEmpty();

        if (initialDialogsType == DIALOGS_TYPE_DEFAULT) {
            optionsItem = menu.addItem(4, R.drawable.ic_ab_other);
            optionsItem.setContentDescription(LocaleController.getString(R.string.AccDescrMoreOptions));
            optionsItem.setOnClickListener(v -> {
                getContactsController().loadGlobalPrivacySetting();
                showItemOptions();
            });
            optionsItem.setOnLongClickListener(v -> {
                getContactsController().loadGlobalPrivacySetting();
                showItemOptions();
                return true;
            });
        }

        searchItem.setSearchFieldHint(getString(R.string.Search));
        searchItem.setContentDescription(getString(R.string.Search));
        if (onlySelect) {
            actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            if (initialDialogsType == DIALOGS_TYPE_BOT_SELECT_VERIFY) {
                actionBar.setTitle(getString(R.string.BotChooseChatToVerify));
            } else if (isReplyTo) {
                actionBar.setTitle(LocaleController.getString(R.string.ReplyToDialog));
            } else if (isQuote) {
                actionBar.setTitle(getString(R.string.QuoteTo));
            } else if (initialDialogsType == DIALOGS_TYPE_FORWARD && selectAlertString == null) {
                actionBar.setTitle(getString(R.string.ForwardTo));
            } else if (initialDialogsType == DIALOGS_TYPE_WIDGET) {
                actionBar.setTitle(getString(R.string.SelectChats));
            } else if (initialDialogsType == DIALOGS_TYPE_START_ATTACH_BOT) {
                if (allowBots && !allowUsers && !allowGroups && !allowChannels) {
                    actionBar.setTitle(getString(R.string.ChooseBot));
                } else if (allowUsers && !allowBots && !allowGroups && !allowChannels) {
                    actionBar.setTitle(getString(R.string.ChooseUser));
                } else if (allowGroups && !allowUsers && !allowBots && !allowChannels) {
                    actionBar.setTitle(getString(R.string.ChooseGroup));
                } else if (allowChannels && !allowUsers && !allowBots && !allowGroups) {
                    actionBar.setTitle(getString(R.string.ChooseChannel));
                } else {
                    actionBar.setTitle(getString(R.string.SelectChat));
                }
            } else if (requestPeerType instanceof TLRPC.TL_requestPeerTypeUser) {
                if (((TLRPC.TL_requestPeerTypeUser) requestPeerType).bot != null) {
                    if (((TLRPC.TL_requestPeerTypeUser) requestPeerType).bot) {
                        actionBar.setTitle(getString(R.string.ChooseBot));
                    } else {
                        actionBar.setTitle(getString(R.string.ChooseUser));
                    }
                } else {
                    actionBar.setTitle(getString(R.string.ChooseUser));
                }
            } else if (requestPeerType instanceof TLRPC.TL_requestPeerTypeBroadcast) {
                actionBar.setTitle(getString(R.string.ChooseChannel));
            } else if (requestPeerType instanceof TLRPC.TL_requestPeerTypeChat) {
                actionBar.setTitle(getString(R.string.ChooseGroup));
            } else {
                actionBar.setTitle(getString(R.string.SelectChat));
            }
            actionBar.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
        } else {
            if (searchString != null || folderId != 0 || communityId != 0) {
                actionBar.setBackButtonDrawable(backDrawable = new BackDrawable(false));
            }
            if (folderId != 0) {
                actionBar.setTitle(getString(R.string.ArchivedChats));
            } else if (communityId != 0) {
                actionBar.setTitle(DialogObject.getName(community));
                actionBar.setAdditionalTextLeft(dp(28));
                communityAvatarDrawable = new AvatarDrawable(community);
                communityAvatarImage = new BackupImageView(getContext());
                communityAvatarImage.setRoundRadius(dp(11));
                communityAvatarImage.setForUserOrChat(community, communityAvatarDrawable);
                actionBar.addView(communityAvatarImage, LayoutHelper.createFrame(32, 32, Gravity.BOTTOM | Gravity.LEFT, 58, 0, 0, 12f));
            } else {
                statusDrawable = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable(null, dp(26));
                statusDrawable.center = true;
                logoDrawable = context.getResources().getDrawable(R.drawable.telegram_logo_2).mutate();
                logoDrawable.setBounds(0, dp(2), logoDrawable.getIntrinsicWidth(), dp(2) + logoDrawable.getIntrinsicHeight());
                logoDrawable.setColorFilter(getThemedColor(Theme.key_telegram_color_dialogsLogo), PorterDuff.Mode.MULTIPLY);
                SpannableStringBuilder ssb = new SpannableStringBuilder(getString(R.string.AppName));
                ssb.setSpan(new ImageSpan(logoDrawable), 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                actionBar.setTitle(ssb, statusDrawable);
                updateStatus(UserConfig.getInstance(currentAccount).getCurrentUser(), false);
            }
            if (folderId == 0) {
                actionBar.setSupportsHolidayImage(true);
            }
        }
        //if (!onlySelect || initialDialogsType == DIALOGS_TYPE_FORWARD) {
            actionBar.setAddToContainer(false);
            actionBar.setCastShadows(false);
            actionBar.setClipContent(true);
        //}
        actionBar.setTitleActionRunnable(() -> {
            if (initialDialogsType != DIALOGS_TYPE_WIDGET) {
                hideFloatingButton(false);
            }
            if (hasOnlySlefStories && getStoriesController().hasOnlySelfStories()) {
                dialogStoriesCell.openSelfStories();
            } else {
                scrollToTop(true, true);
            }
        });

        if (
            (initialDialogsType == DIALOGS_TYPE_DEFAULT && !onlySelect || initialDialogsType == DIALOGS_TYPE_FORWARD) &&
            folderId == 0 && communityId == 0 && TextUtils.isEmpty(searchString)
        ) {
            filterTabsView = new FilterTabsView(context, resourceProvider) {
                @Override
                public boolean onInterceptTouchEvent(MotionEvent ev) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                    maybeStartTracking = false;
                    return super.onInterceptTouchEvent(ev);
                }

                @Override
                protected void onDefaultTabMoved() {
                    if (!getMessagesController().premiumFeaturesBlocked()) {
                        try {
                            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                        } catch (Exception ignore) {}
                        topBulletin = BulletinFactory.of(DialogsActivity.this).createSimpleBulletin(R.raw.filter_reorder, AndroidUtilities.replaceTags(LocaleController.formatString(R.string.LimitReachedReorderFolder, LocaleController.getString(R.string.FilterAllChats))), LocaleController.getString(R.string.PremiumMore), Bulletin.DURATION_PROLONG, () -> {
                            showDialog(new PremiumFeatureBottomSheet(DialogsActivity.this, PremiumPreviewFragment.PREMIUM_FEATURE_ADVANCED_CHAT_MANAGEMENT, true));
                            filterTabsView.setIsEditing(false);
                            showDoneItem(false);
                        }).show(true);
                    }
                }
            };
            filterTabsView.setVisibility(View.GONE);
            canShowFilterTabsView = false;
            animatorFilterTabsVisible.setValue(false, false);
            filterTabsView.setDelegate(new FilterTabsView.FilterTabsViewDelegate() {

                private void showDeleteAlert(MessagesController.DialogFilter dialogFilter) {
                    if (dialogFilter.isChatlist()) {
                        FolderBottomSheet.showForDeletion(DialogsActivity.this, dialogFilter.id, null);
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setTitle(LocaleController.getString(R.string.FilterDelete));
                        builder.setMessage(LocaleController.getString(R.string.FilterDeleteAlert));
                        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                        builder.setPositiveButton(LocaleController.getString(R.string.Delete), (dialog2, which2) -> {
                            TLRPC.TL_messages_updateDialogFilter req = new TLRPC.TL_messages_updateDialogFilter();
                            req.id = dialogFilter.id;
                            getConnectionsManager().sendRequest(req, null);
                            getMessagesController().removeFilter(dialogFilter);
                            getMessagesStorage().deleteDialogFilter(dialogFilter);
                        });
                        AlertDialog alertDialog = builder.create();
                        showDialog(alertDialog);
                        TextView button = (TextView) alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                        if (button != null) {
                            button.setTextColor(getThemedColor(Theme.key_text_RedBold));
                        }
                    }
                }

                @Override
                public void onSamePageSelected() {
                    scrollToTop(true, false);
                }

                @Override
                public void onPageReorder(int fromId, int toId) {
                    for (int a = 0; a < viewPages.length; a++) {
                        if (viewPages[a].selectedType == fromId) {
                            viewPages[a].selectedType = toId;
                        } else if (viewPages[a].selectedType == toId) {
                            viewPages[a].selectedType = fromId;
                        }
                    }
                }

                @Override
                public void onPageSelected(FilterTabsView.Tab tab, boolean forward) {
                    if (viewPages[0].selectedType == tab.id) {
                        return;
                    }
                    if (tab.isLocked) {
                        filterTabsView.shakeLock(tab.id);
                        showDialog(new LimitReachedBottomSheet(DialogsActivity.this, context, LimitReachedBottomSheet.TYPE_FOLDERS, currentAccount, null));
                        return;
                    }

                    ArrayList<MessagesController.DialogFilter> dialogFilters = getMessagesController().getDialogFilters();
                    if (!tab.isDefault && (tab.id < 0 || tab.id >= dialogFilters.size())) {
                        return;
                    }
                    viewPages[1].selectedType = tab.id;
                    viewPages[1].setVisibility(View.VISIBLE);
                    viewPages[1].setTranslationX(viewPages[0].getMeasuredWidth());
                    showScrollbars(false);
                    switchToCurrentSelectedMode(true);
                    animatingForward = forward;
                }

                @Override
                public boolean canPerformActions() {
                    return !searching;
                }

                @Override
                public void onPageScrolled(float progress) {
                    if (progress == 1 && viewPages[1].getVisibility() != View.VISIBLE && !searching) {
                        return;
                    }
                    if (animatingForward) {
                        viewPages[0].setTranslationX(-progress * viewPages[0].getMeasuredWidth());
                        viewPages[1].setTranslationX(viewPages[0].getMeasuredWidth() - progress * viewPages[0].getMeasuredWidth());
                    } else {
                        viewPages[0].setTranslationX(progress * viewPages[0].getMeasuredWidth());
                        viewPages[1].setTranslationX(progress * viewPages[0].getMeasuredWidth() - viewPages[0].getMeasuredWidth());
                    }
                    if (progress == 1) {
                        ViewPage tempPage = viewPages[0];
                        viewPages[0] = viewPages[1];
                        viewPages[1] = tempPage;
                        viewPages[1].setVisibility(View.GONE);
                        showScrollbars(true);
                        updateCounters(false);
                        filterTabsView.stopAnimatingIndicator();
                        checkListLoad(viewPages[0]);
                        viewPages[0].dialogsAdapter.resume();
                        viewPages[1].dialogsAdapter.pause();
                    }
                }

                @Override
                public int getTabCounter(int tabId) {
                    if (initialDialogsType == DIALOGS_TYPE_FORWARD) {
                        return 0;
                    }
                    if (tabId == filterTabsView.getDefaultTabId()) {
                        return getMessagesStorage().getMainUnreadCount();
                    }
                    ArrayList<MessagesController.DialogFilter> dialogFilters = getMessagesController().getDialogFilters();
                    if (tabId < 0 || tabId >= dialogFilters.size()) {
                        return 0;
                    }
                    return getMessagesController().getDialogFilters().get(tabId).unreadCount;
                }

                @Override
                public boolean didSelectTab(FilterTabsView.TabView tabView, boolean selected) {
                    if (initialDialogsType != DIALOGS_TYPE_DEFAULT) {
                        return false;
                    }
                    if (actionBar.isActionModeShowed() || storiesOverscroll != 0) {
                        return false;
                    }
                    if (filterOptions != null && filterOptions.isShown()) {
                        filterOptions.dismiss();
                        filterOptions = null;
                        return false;
                    }

                    final MessagesController.DialogFilter dialogFilter;
                    if (tabView.getId() == filterTabsView.getDefaultTabId()) {
                        dialogFilter = null;
                    } else {
                        ArrayList<MessagesController.DialogFilter> dialogFilters = getMessagesController().getDialogFilters();
                        final int index = tabView.getId();
                        if (dialogFilters != null && index >= 0 && index < dialogFilters.size()) {
                            dialogFilter = dialogFilters.get(tabView.getId());
                        } else {
                            dialogFilter = null;
                        }
                    }

                    boolean defaultTab = dialogFilter == null;
                    boolean hasUnread = false, hasShare = false;
                    boolean muteAll = false;
                    boolean[] shareEmpty = new boolean[1];
                    shareEmpty[0] = true;

                    ArrayList<TLRPC.Dialog> dialogs = new ArrayList<>(defaultTab ? getMessagesController().getDialogs(folderId) : getMessagesController().getAllDialogs());
                    MessagesController.DialogFilter filter = null;
                    if (dialogFilter != null) {
                        filter = getMessagesController().getDialogFilters().get(tabView.getId());
                        if (filter != null) {
                            for (int i = 0; i < dialogs.size(); i++) {
                                if (!filter.includesDialog(getAccountInstance(), dialogs.get(i).id)) {
                                    dialogs.remove(i);
                                    i--;
                                }
                            }
                            hasShare = filter.isChatlist() || filter.neverShow.isEmpty() && (filter.flags & ~(MessagesController.DIALOG_FILTER_FLAG_CHATLIST | MessagesController.DIALOG_FILTER_FLAG_CHATLIST_ADMIN)) == 0;
                            if (hasShare) {
                                for (int i = 0; i < filter.alwaysShow.size(); ++i) {
                                    long did = filter.alwaysShow.get(i);
                                    if (did < 0) {
                                        TLRPC.Chat chat = getMessagesController().getChat(-did);
                                        if (chat != null && FilterCreateActivity.canAddToFolder(chat)) {
                                            shareEmpty[0] = false;
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        if (!dialogs.isEmpty()) {
                            boolean allAreMuted = true;
                            for (int i = 0; i < dialogs.size(); ++i) {
                                TLRPC.Dialog dialog = dialogs.get(i);
                                if (!getMessagesController().isDialogMuted(dialog.id, 0)) {
                                    allAreMuted = false;
                                    break;
                                }
                            }
                            muteAll = !allAreMuted;
                        }
                    }
                    final boolean finalMuteAll = muteAll;

                    final MessagesController.DialogFilter finalFilter = filter;
                    for (int i = 0; i < dialogs.size(); i++) {
                        if (dialogs.get(i).unread_mark || dialogs.get(i).unread_count > 0) {
                            hasUnread = true;
                        }
                    }

                    filterOptions = ItemOptions.makeOptions(DialogsActivity.this, tabView)
                            .setScrimViewBackground(new Drawable() {
                                private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                                private RectF bound = new RectF();

                                {
                                    paint.setColor(getThemedColor(Theme.key_actionBarDefaultSubmenuBackground));
                                }

                                @Override
                                public void draw(@NonNull Canvas canvas) {
                                    bound.set(getBounds());
                                    float insetY = (bound.height() - dp(28)) / 2f;
                                    bound.inset(0, insetY);
                                    canvas.drawRoundRect(bound, dp(14), dp(14), paint);
                                }

                                @Override
                                public void setAlpha(int alpha) {
                                    paint.setAlpha(alpha);
                                }

                                @Override
                                public void setColorFilter(@Nullable ColorFilter colorFilter) {

                                }

                                @Override
                                public int getOpacity() {
                                    return PixelFormat.TRANSPARENT;
                                }
                            })
                            .addIf(getMessagesController().getDialogFilters().size() > 1, R.drawable.tabs_reorder, LocaleController.getString(R.string.FilterReorder), () -> {
                                filterTabsView.setIsEditing(true);
                                showDoneItem(true);
                            })
                            .add(R.drawable.msg_edit, defaultTab ? LocaleController.getString(R.string.FilterEditAll) : LocaleController.getString(R.string.FilterEdit), () -> {
                                presentFragment(defaultTab ? new FiltersSetupActivity() : new FilterCreateActivity(dialogFilter));
                            })
                            .addIf(dialogFilter != null && !dialogs.isEmpty(), muteAll ? R.drawable.msg_mute : R.drawable.msg_unmute, muteAll ? LocaleController.getString(R.string.FilterMuteAll) : LocaleController.getString(R.string.FilterUnmuteAll), () -> {
                                int count = 0;
                                for (int i = 0; i < dialogs.size(); ++i) {
                                    TLRPC.Dialog dialog = dialogs.get(i);
                                    if (dialog != null) {
                                        getNotificationsController().setDialogNotificationsSettings(dialog.id, 0, finalMuteAll ? NotificationsController.SETTING_MUTE_FOREVER : NotificationsController.SETTING_MUTE_UNMUTE);
                                        count++;
                                    }
                                }
                                BulletinFactory.createMuteBulletin(DialogsActivity.this, finalMuteAll, count, null).show();
                            })
                            .addIf(hasUnread, R.drawable.msg_markread, LocaleController.getString(R.string.MarkAllAsRead), () -> {
                                markDialogsAsRead(dialogs);
                            })
                            .addIf(hasShare, R.drawable.msg_share, FilterCreateActivity.withNew(filter != null && filter.isMyChatlist() ? -1 : 0, LocaleController.getString(R.string.LinkActionShare), true), () -> {
                                if (shareEmpty[0]) {
                                    presentFragment(new FilterChatlistActivity(finalFilter, null));
                                } else {
                                    FilterCreateActivity.FilterInvitesBottomSheet.show(DialogsActivity.this, finalFilter, null);
                                }
                            })
                            .addIf(!defaultTab, R.drawable.msg_delete, LocaleController.getString(R.string.FilterDeleteItem), true, () -> {
                                showDeleteAlert(dialogFilter);
                            })
                            .setDimAlpha(0x60)
                            .setGravity(Gravity.LEFT)
                            .translate(dp(-12), dp(-4))
                            .show();

                    return true;
                }

                @Override
                public boolean isTabMenuVisible() {
                    return filterOptions != null && filterOptions.isShown();
                }

                @Override
                public void onDeletePressed(int id) {
                    showDeleteAlert(getMessagesController().getDialogFilters().get(id));
                }
            });
        }

        if (allowSwitchAccount && UserConfig.getActivatedAccountsCount() > 1) {
            switchItem = menu.addItemWithWidth(11, 0, dp(56));
            AvatarDrawable avatarDrawable = new AvatarDrawable();
            avatarDrawable.setTextSize(dp(12));

            BackupImageView imageView = new BackupImageView(context);
            imageView.setRoundRadius(dp(18));
            switchItem.addView(imageView, LayoutHelper.createFrame(36, 36, Gravity.CENTER));
            switchItem.setOnClickListener(this::openAccountSelector);
            switchItem.setOnLongClickListener(this::openAccountSelector);

            TLRPC.User user = getUserConfig().getCurrentUser();
            avatarDrawable.setInfo(currentAccount, user);
            imageView.getImageReceiver().setCurrentAccount(currentAccount);
            Drawable thumb = user != null && user.photo != null && user.photo.strippedBitmap != null ? user.photo.strippedBitmap : avatarDrawable;
            imageView.setImage(ImageLocation.getForUserOrChat(currentAccount, user, ImageLocation.TYPE_SMALL), "50_50", ImageLocation.getForUserOrChat(user, ImageLocation.TYPE_STRIPPED), "50_50", thumb, user);
        }

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if ((id == SearchViewPager.forwardItemId || id == SearchViewPager.gotoItemId || id == SearchViewPager.deleteItemId || id == SearchViewPager.speedItemId) && searchViewPager != null) {
                    searchViewPager.onActionBarItemClick(id);
                    return;
                }
                if (id == -1) {
                    if (rightSlidingDialogContainer != null && rightSlidingDialogContainer.hasFragment()) {
                        if (actionBar.isActionModeShowed()) {
                            if (searchViewPager != null && searchViewPager.getVisibility() == View.VISIBLE && searchViewPager.actionModeShowing()) {
                                searchViewPager.hideActionMode();
                            } else {
                                hideActionMode(true);
                            }
                        } else {
                            rightSlidingDialogContainer.finishPreview();
                            if (searchViewPager != null) {
                                searchViewPager.updateTabs();
                            }
                            return;
                        }
                    } else if (filterTabsView != null && filterTabsView.isEditing()) {
                        filterTabsView.setIsEditing(false);
                        showDoneItem(false);
                    } else if (actionBar.isActionModeShowed()) {
                        if (searchViewPager != null && searchViewPager.getVisibility() == View.VISIBLE && searchViewPager.actionModeShowing()) {
                            searchViewPager.hideActionMode();
                        } else {
                            hideActionMode(true);
                        }
                    } else if (onlySelect || folderId != 0 || communityId != 0) {
                        finishFragment();
                    }
                } else if (id == 1) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    SharedConfig.appLocked = true;
                    SharedConfig.saveConfig();
                    int[] position = new int[2];
                    passcodeItem.getLocationInWindow(position);
                    ((LaunchActivity) getParentActivity()).showPasscodeActivity(false, true, position[0] + passcodeItem.getMeasuredWidth() / 2, position[1] + passcodeItem.getMeasuredHeight() / 2, () -> passcodeItem.setAlpha(1.0f), () -> passcodeItem.setAlpha(0.0f));
                    getNotificationsController().showNotifications();
                    checkUi_itemPasscodeVisibility();
                } else if (id == 3) {
                    showSearch(true, true, true);
                    fragmentSearchFieldWatcher.toggleSearch(true);
                } else if (id == 11) {
                    openAccountSelector(switchItem);
                } else if (id == add_to_folder) {
                    FiltersListBottomSheet sheet = new FiltersListBottomSheet(DialogsActivity.this, selectedDialogs);
                    sheet.setDelegate((filter, checked) -> {
                        ArrayList<Long> alwaysShow = FiltersListBottomSheet.getDialogsCount(DialogsActivity.this, filter, selectedDialogs, true, false);
                        if (!checked) {
                            int currentCount;
                            if (filter != null) {
                                currentCount = filter.alwaysShow.size();
                            } else {
                                currentCount = 0;
                            }
                            int totalCount = currentCount + alwaysShow.size();
                            if ((totalCount > getMessagesController().dialogFiltersChatsLimitDefault && !getUserConfig().isPremium()) || totalCount > getMessagesController().dialogFiltersChatsLimitPremium) {
                                showDialog(new LimitReachedBottomSheet(DialogsActivity.this, fragmentView.getContext(), LimitReachedBottomSheet.TYPE_CHATS_IN_FOLDER, currentAccount, null));
                                return;
                            }
                        }
                        if (filter != null) {
                            if (checked) {
                                for (int a = 0; a < selectedDialogs.size(); a++) {
                                    filter.neverShow.add(selectedDialogs.get(a));
                                    filter.alwaysShow.remove(selectedDialogs.get(a));
                                }
                                FilterCreateActivity.saveFilterToServer(filter, filter.flags, filter.name, filter.entities, filter.title_noanimate, filter.color, filter.alwaysShow, filter.neverShow, filter.pinnedDialogs, false, false, true, true, false, DialogsActivity.this, null);
                                long did;
                                if (selectedDialogs.size() == 1) {
                                    did = selectedDialogs.get(0);
                                } else {
                                    did = 0;
                                }
                                final UndoView undoView = getUndoView();
                                if (undoView != null) {
                                    undoView.showWithAction(did, UndoView.ACTION_REMOVED_FROM_FOLDER, selectedDialogs.size(), filter, null, null);
                                }
                            } else {
                                if (!alwaysShow.isEmpty()) {
                                    for (int a = 0; a < alwaysShow.size(); a++) {
                                        filter.neverShow.remove(alwaysShow.get(a));
                                    }
                                    filter.alwaysShow.addAll(alwaysShow);
                                    FilterCreateActivity.saveFilterToServer(filter, filter.flags, filter.name, filter.entities, filter.title_noanimate, filter.color, filter.alwaysShow, filter.neverShow, filter.pinnedDialogs, false, false, true, true, false, DialogsActivity.this, null);
                                }
                                long did;
                                if (alwaysShow.size() == 1) {
                                    did = alwaysShow.get(0);
                                } else {
                                    did = 0;
                                }
                                final UndoView undoView = getUndoView();
                                if (undoView != null) {
                                    undoView.showWithAction(did, UndoView.ACTION_ADDED_TO_FOLDER, alwaysShow.size(), filter, null, null);
                                }
                            }
                        } else {
                            presentFragment(new FilterCreateActivity(null, alwaysShow));
                        }
                        hideActionMode(true);
                    });
                    showDialog(sheet);
                } else if (id == remove_from_folder) {
                    MessagesController.DialogFilter filter = getMessagesController().getDialogFilters().get(viewPages[0].selectedType);
                    ArrayList<Long> neverShow = FiltersListBottomSheet.getDialogsCount(DialogsActivity.this, filter, selectedDialogs, false, false);

                    int currentCount;
                    if (filter != null) {
                        currentCount = filter.neverShow.size();
                    } else {
                        currentCount = 0;
                    }
                    if (currentCount + neverShow.size() > 100) {
                        showDialog(AlertsCreator.createSimpleAlert(getParentActivity(), LocaleController.getString(R.string.FilterAddToAlertFullTitle), LocaleController.getString(R.string.FilterAddToAlertFullText)).create());
                        return;
                    }
                    if (!neverShow.isEmpty()) {
                        filter.neverShow.addAll(neverShow);
                        for (int a = 0; a < neverShow.size(); a++) {
                            Long did = neverShow.get(a);
                            filter.alwaysShow.remove(did);
                            filter.pinnedDialogs.delete(did);
                        }
                        if (filter.isChatlist()) {
                            filter.neverShow.clear();
                        }
                        FilterCreateActivity.saveFilterToServer(filter, filter.flags, filter.name, filter.entities, filter.title_noanimate, filter.color, filter.alwaysShow, filter.neverShow, filter.pinnedDialogs, false, false, true, false, false, DialogsActivity.this, null);
                    }
                    long did;
                    if (neverShow.size() == 1) {
                        did = neverShow.get(0);
                    } else {
                        did = 0;
                    }
                    final UndoView undoView = getUndoView();
                    if (undoView != null) {
                        undoView.showWithAction(did, UndoView.ACTION_REMOVED_FROM_FOLDER, neverShow.size(), filter, null, null);
                    }
                    hideActionMode(false);
                } else if (id == pin || id == read || id == delete || id == clear || id == mute || id == archive || id == block || id == archive2 || id == pin2) {
                    performSelectedDialogsAction(selectedDialogs, id, true, false);
                }
            }
        });

        ContentView contentView = new ContentView(context);
        fragmentView = contentView;

        viewPositionWatcher = new ViewPositionWatcher(contentView);
        iBlur3FactoryFrostedLiquidGlass.setSourceRootView(viewPositionWatcher, contentView);
        iBlur3FactoryLiquidGlass.setSourceRootView(viewPositionWatcher, contentView);
        iBlur3FactoryFade.setSourceRootView(viewPositionWatcher, contentView);
        iBlur3FactoryBlur.setSourceRootView(viewPositionWatcher, contentView);

        final PointF tmpPoint = new PointF();
        iBlur3Capture = (canvas, position) -> {
            final int searchViewAlpha = searchViewPager != null ? (int) (searchViewPager.getAlpha() * 255) : 0;

            for (ViewPage viewPage : viewPages) {
                if (viewPage != null && viewPage.getVisibility() == View.VISIBLE && viewPage.getAlpha() > 0f) {
                    float rp = getRightSlidingProgress();
                    if (viewPage.animationSupportListView != null && rp > 0) {
                        if (!ViewPositionWatcher.computeCoordinatesInParent(viewPage.listView, contentView, tmpPoint)) {
                            return;
                        }

                        canvas.save();
                        canvas.clipRect(position);
                        canvas.translate(tmpPoint.x, tmpPoint.y);
                        viewPage.listView.dispatchDraw(canvas);
                        canvas.restore();
                    } else {
                        Blur3Utils.captureRelativeParent(viewPage.listView, canvas, position, viewPage.listView, contentView, 255 - searchViewAlpha);
                    }
                }
            }
            if (searchViewPager != null && searchViewPager.getVisibility() == View.VISIBLE && searchViewPager.getAlpha() > 0f) {
                Blur3Utils.captureRelativeParent(searchViewPager, canvas, position, searchViewPager, contentView, searchViewAlpha);
            }
        };

        int pagesCount = folderId == 0 && communityId == 0 && (initialDialogsType == DIALOGS_TYPE_DEFAULT && !onlySelect || initialDialogsType == DIALOGS_TYPE_FORWARD) ? 2 : 1;
        viewPages = new ViewPage[pagesCount];
        for (int a = 0; a < pagesCount; a++) {
            final ViewPage viewPage = new ViewPage(context);
            contentView.addView(viewPage, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            viewPage.dialogsType = initialDialogsType;
            viewPages[a] = viewPage;

            viewPage.progressView = new FlickerLoadingView(context);
            viewPage.progressView.setViewType(FlickerLoadingView.DIALOG_CELL_TYPE);
            viewPage.progressView.setVisibility(View.GONE);
            viewPage.addView(viewPage.progressView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

            viewPage.listView = new DialogsRecyclerView(context, viewPage);
            viewPage.listView.addEdgeEffectListener(() -> viewPage.listView.postOnAnimation(this::blur3_InvalidateBlur));
            viewPage.scroller = new RecyclerListViewScroller(viewPage.listView);
            viewPage.listView.setAllowStopHeaveOperations(true);
            viewPage.listView.setAccessibilityEnabled(false);
            viewPage.listView.setAnimateEmptyView(true, RecyclerListView.EMPTY_VIEW_ANIMATION_TYPE_ALPHA);
            viewPage.listView.setClipToPadding(false);
            viewPage.listView.setPivotY(0);
            if (initialDialogsType == DIALOGS_TYPE_BOT_REQUEST_PEER) {
                viewPage.listView.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));
            }
            viewPage.dialogsItemAnimator = new DialogsItemAnimator(viewPage.listView) {
                @Override
                public void onRemoveStarting(RecyclerView.ViewHolder item) {
                    super.onRemoveStarting(item);
                    if (viewPage.layoutManager.findFirstVisibleItemPosition() == 0) {
                        View v = viewPage.layoutManager.findViewByPosition(0);
                        if (v != null) {
                            v.invalidate();
                        }
                        if (viewPage.archivePullViewState == ARCHIVE_ITEM_STATE_HIDDEN) {
                            viewPage.archivePullViewState = ARCHIVE_ITEM_STATE_SHOWED;
                        }
                        if (viewPage.pullForegroundDrawable != null) {
                            viewPage.pullForegroundDrawable.doNotShow();
                        }
                    }
                }
            };
            viewPage.listView.setVerticalScrollBarEnabled(true);
            viewPage.listView.setInstantClick(true);
            viewPage.layoutManager = new LinearLayoutManager(context) {

                @Override
                protected int firstPosition() {
                    if (viewPage.dialogsType == DIALOGS_TYPE_DEFAULT && hasHiddenArchive() && viewPage.archivePullViewState == ARCHIVE_ITEM_STATE_HIDDEN) {
                        return 1;
                    }
                    return 0;
                }

                private boolean fixOffset;

                @Override
                public void scrollToPositionWithOffset(int position, int offset) {
                    if (fixOffset) {
                        offset -= viewPage.listView.getPaddingTop();
                    }
                    super.scrollToPositionWithOffset(position, offset);
                }

                @Override
                public void prepareForDrop(@NonNull View view, @NonNull View target, int x, int y) {
                    fixOffset = true;
                    super.prepareForDrop(view, target, x, y);
                    fixOffset = false;
                }

                @Override
                public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
                    if (hasHiddenArchive() && position == 1) {
                        super.smoothScrollToPosition(recyclerView, state, position);
                    } else {
                        LinearSmoothScrollerCustom linearSmoothScroller = new LinearSmoothScrollerCustom(recyclerView.getContext(), LinearSmoothScrollerCustom.POSITION_MIDDLE);
                        linearSmoothScroller.setTargetPosition(position);
                        startSmoothScroll(linearSmoothScroller);
                    }
                }

                boolean lastDragging;

                ValueAnimator storiesOverscrollAnimator;
                @Override
                public void onScrollStateChanged(int state) {
                    super.onScrollStateChanged(state);
                    if (storiesOverscrollAnimator != null) {
                        storiesOverscrollAnimator.removeAllListeners();
                        storiesOverscrollAnimator.cancel();
                    }
                    if (viewPage.listView.getScrollState() != RecyclerView.SCROLL_STATE_DRAGGING) {
                        storiesOverscrollAnimator = ValueAnimator.ofFloat(storiesOverscroll, 0);
                        storiesOverscrollAnimator.addUpdateListener(animation -> {
                            setStoriesOvercroll(viewPage, (float) animation.getAnimatedValue());
                        });
                        storiesOverscrollAnimator.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                setStoriesOvercroll(viewPage, 0);
                            }
                        });
                        storiesOverscrollAnimator.setDuration(200);
                        storiesOverscrollAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
                        storiesOverscrollAnimator.start();
                    }
                }

                @Override
                public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
                    if (viewPage.listView.fastScrollAnimationRunning) {
                        return 0;
                    }
                    boolean isDragging = viewPage.listView.getScrollState() == RecyclerView.SCROLL_STATE_DRAGGING;
                    if (isDragging != lastDragging) {
                        lastDragging = isDragging;
                        if (!isDragging) {
                            if (checkAutoscrollToStories(viewPage)) {
                                return 0;
                            }
                        }
                    }
                    int measuredDy = dy;
                    if (dy > 0 && storiesOverscroll != 0 && !(actionBar != null && actionBar.isActionModeShowed())) {
                        float newOverscroll = storiesOverscroll - dy;
                        if (newOverscroll < 0) {
                            measuredDy = (int) -newOverscroll;
                            newOverscroll = 0;
                        } else {
                            measuredDy = 0;
                        }
                        setStoriesOvercroll(viewPage, newOverscroll);
                        return super.scrollVerticallyBy(measuredDy, recycler, state);
                    }
                    final boolean hasStories = DialogsActivity.this.hasStories && communityId == 0 && !(actionBar != null && actionBar.isActionModeShowed());
                    int pTop = viewPage.listView.getPaddingTop();
                    int realTopPadding = pTop;
                    if (hasStories && !rightSlidingDialogContainer.hasFragment() && !fixScrollYAfterArchiveOpened) {
                        pTop -= dp(DialogStoriesCell.HEIGHT_IN_DP);
                    }
                    boolean hasHiddenArchive = !fixScrollYAfterArchiveOpened && viewPage.dialogsType == DIALOGS_TYPE_DEFAULT && !onlySelect && folderId == 0 && communityId == 0 && getMessagesController().hasHiddenArchive() && viewPage.archivePullViewState == ARCHIVE_ITEM_STATE_HIDDEN;
                    if ((hasHiddenArchive || (hasStories && !rightSlidingDialogContainer.hasFragment())) && dy < 0) {
                        viewPage.listView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
                        int currentPosition = viewPage.layoutManager.findFirstVisibleItemPosition();
                        if (currentPosition == 0) {
                            View view = viewPage.layoutManager.findViewByPosition(currentPosition);
                            if (view != null && (view.getBottom() - pTop) <= dp(1)) {
                                currentPosition = 1;
                            }
                        }
                        if (!isDragging) {
                            View view = viewPage.layoutManager.findViewByPosition(currentPosition);
                            if (view != null && currentPosition < 10) {
                                int viewsH = 0;
                                for (int i = hasHiddenArchive ? 1 : 0; i < currentPosition; i++) {
                                    viewsH += viewPage.dialogsAdapter.getItemHeight(i);
                                }
                                int canScrollDy = -(view.getTop() - pTop) + viewsH;
                                if (!rightSlidingDialogContainer.hasFragment() && !(actionBar != null && actionBar.isActionModeShowed())) {
                                    canScrollDy -= dp(SEARCH_FIELD_HEIGHT);
                                }
                                if (hasStories && (viewPage.scroller.isRunning() || dialogStoriesCell.isExpanded()) && !rightSlidingDialogContainer.hasFragment() && !fixScrollYAfterArchiveOpened) {
                                    canScrollDy += dp(DialogStoriesCell.HEIGHT_IN_DP);
                                }
                                if ((viewPage.scroller.isRunning() || dialogStoriesCell.isExpanded()) && !rightSlidingDialogContainer.hasFragment() && !fixScrollYAfterArchiveOpened && !(actionBar != null && actionBar.isActionModeShowed())) {
                                    canScrollDy += dp(SEARCH_FIELD_HEIGHT);
                                }
                                int positiveDy = Math.abs(dy);
                                if (canScrollDy < positiveDy) {
                                    measuredDy = -canScrollDy;
                                }
                            }
                        } else if (currentPosition == 0 && hasHiddenArchive) {
                            View v = viewPage.layoutManager.findViewByPosition(currentPosition);
                            float k = 1f + ((v.getTop() - realTopPadding) / (float) v.getMeasuredHeight());
                            if (k > 1f) {
                                k = 1f;
                            }
                            viewPage.listView.setOverScrollMode(View.OVER_SCROLL_NEVER);
                            measuredDy *= PullForegroundDrawable.startPullParallax - PullForegroundDrawable.endPullParallax * k;
                            if (measuredDy > -1) {
                                measuredDy = -1;
                            }
                            if (undoView[0] != null && undoView[0].getVisibility() == View.VISIBLE) {
                                undoView[0].hide(true, 1);
                            }
                        } else if (((currentPosition == 1 && hasHiddenArchive) || currentPosition == 0) && hasStories && isDragging && !rightSlidingDialogContainer.hasFragment()) {
                            if (scrollYOffset == 0) {
                                viewPage.listView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
                            } else {
                                viewPage.listView.setOverScrollMode(View.OVER_SCROLL_NEVER);
                            }
                            measuredDy *= 0.3f;
                            if (measuredDy > -1) {
                                measuredDy = -1;
                            }
                        }
                    }

                    if (viewPage.dialogsType == 0 && viewPage.listView.getViewOffset() != 0 && dy > 0 && isDragging) {
                        float ty = (int) viewPage.listView.getViewOffset();
                        ty -= dy;
                        if (ty < 0) {
                            measuredDy = (int) ty;
                            ty = 0;
                        } else {
                            measuredDy = 0;
                        }
                        viewPage.listView.setViewsOffset(ty);
                    }

                    if (viewPage.dialogsType == DIALOGS_TYPE_DEFAULT && viewPage.archivePullViewState != ARCHIVE_ITEM_STATE_PINNED && hasHiddenArchive() && !fixScrollYAfterArchiveOpened) {
                        int usedDy = super.scrollVerticallyBy(measuredDy, recycler, state);
                        if (viewPage.pullForegroundDrawable != null) {
                            viewPage.pullForegroundDrawable.scrollDy = usedDy;
                        }
                        int currentPosition = viewPage.layoutManager.findFirstVisibleItemPosition();
                        View firstView = null;
                        if (currentPosition == 0) {
                            firstView = viewPage.layoutManager.findViewByPosition(currentPosition);
                        }
                        if (currentPosition == 0 && firstView != null && (firstView.getBottom() - pTop) >= dp(4)) {
                            if (startArchivePullingTime == 0) {
                                startArchivePullingTime = System.currentTimeMillis();
                            }
                            if (viewPage.archivePullViewState == ARCHIVE_ITEM_STATE_HIDDEN) {
                                if (viewPage.pullForegroundDrawable != null) {
                                    viewPage.pullForegroundDrawable.showHidden();
                                }
                            }
                            if (hasStories && !rightSlidingDialogContainer.hasFragment() && !fixScrollYAfterArchiveOpened) {
                                pTop += dp(DialogStoriesCell.HEIGHT_IN_DP);
                            }
                            float k = 1f + ((firstView.getTop() - pTop) / (float) firstView.getMeasuredHeight());
                            if (k > 1f) {
                                k = 1f;
                            }
                            long pullingTime = System.currentTimeMillis() - startArchivePullingTime;
                            boolean canShowInternal = k > PullForegroundDrawable.SNAP_HEIGHT && pullingTime > PullForegroundDrawable.minPullingTime + 20;
                            if (canShowHiddenArchive != canShowInternal) {
                                canShowHiddenArchive = canShowInternal;
                                if (viewPage.archivePullViewState == ARCHIVE_ITEM_STATE_HIDDEN) {
                                    try {
                                        viewPage.listView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                                    } catch (Exception ignored) {}
                                    if (viewPage.pullForegroundDrawable != null) {
                                        viewPage.pullForegroundDrawable.colorize(canShowInternal);
                                    }
                                }
                            }
                            if (viewPage.archivePullViewState == ARCHIVE_ITEM_STATE_HIDDEN && measuredDy - usedDy != 0 && dy < 0 && isDragging) {
                                float ty;
                                float tk = (viewPage.listView.getViewOffset() / PullForegroundDrawable.getMaxOverscroll());
                                tk = 1f - tk;
                                ty = (viewPage.listView.getViewOffset() - dy * PullForegroundDrawable.startPullOverScroll * tk);
                                viewPage.listView.setViewsOffset(ty);
                            }
                            if (viewPage.pullForegroundDrawable != null) {
                                viewPage.pullForegroundDrawable.setPullProgress(k);
                                viewPage.pullForegroundDrawable.setListView(viewPage.listView);
                            }
                        } else {
                            startArchivePullingTime = 0;
                            canShowHiddenArchive = false;
                            boolean changed = viewPage.archivePullViewState != ARCHIVE_ITEM_STATE_HIDDEN;
                            viewPage.archivePullViewState = ARCHIVE_ITEM_STATE_HIDDEN;
                            if (changed && AndroidUtilities.isAccessibilityScreenReaderEnabled()) {
                                AndroidUtilities.makeAccessibilityAnnouncement(LocaleController.getString(R.string.AccDescrArchivedChatsHidden));
                            }
                            if (viewPage.pullForegroundDrawable != null) {
                                viewPage.pullForegroundDrawable.resetText();
                                viewPage.pullForegroundDrawable.setPullProgress(0f);
                                viewPage.pullForegroundDrawable.setListView(viewPage.listView);
                            }
                        }
                        if (firstView != null) {
                            firstView.invalidate();
                        }
                        if (viewPage.archivePullViewState == ARCHIVE_ITEM_STATE_SHOWED && usedDy == 0 && dy < 0 && isDragging && !rightSlidingDialogContainer.hasFragment() && hasStories && progressToActionMode == 0) {
                            float newOverScroll = storiesOverscroll - dy * AndroidUtilities.lerp( 0.2f, 0.5f, dialogStoriesCell.overscrollProgress());
                            setStoriesOvercroll(viewPage, newOverScroll);
                        }
                        return usedDy;
                    }

                    int scrolled = super.scrollVerticallyBy(measuredDy, recycler, state);
                    if (scrolled == 0 && dy < 0 && isDragging && !rightSlidingDialogContainer.hasFragment() && hasStories && progressToActionMode == 0) {
                        float newOverScroll = storiesOverscroll - dy * dialogStoriesCell.getOverScrollCoef();
                        setStoriesOvercroll(viewPage, newOverScroll);
                    }
                    return scrolled;
                }

                @Override
                public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                    if (BuildVars.DEBUG_PRIVATE_VERSION) {
                        try {
                            super.onLayoutChildren(recycler, state);
                        } catch (IndexOutOfBoundsException e) {
                            throw new RuntimeException("Inconsistency detected. " + "dialogsListIsFrozen=" + dialogsListFrozen + " lastUpdateAction=" + debugLastUpdateAction);
                        }
                    } else {
                        try {
                            super.onLayoutChildren(recycler, state);
                        } catch (IndexOutOfBoundsException e) {
                            FileLog.e(e);
                            AndroidUtilities.runOnUIThread(() -> viewPage.dialogsAdapter.notifyDataSetChanged());
                        }
                    }
                }
            };
            viewPage.layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            viewPage.listView.setLayoutManager(viewPage.layoutManager);
            viewPage.listView.setVerticalScrollbarPosition(LocaleController.isRTL ? RecyclerListView.SCROLLBAR_POSITION_LEFT : RecyclerListView.SCROLLBAR_POSITION_RIGHT);
            viewPage.addView(viewPage.listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            viewPage.listView.setOnItemClickListener((view, position, x, y) -> {
                if (view instanceof GraySectionCell)
                    return;
                if (view instanceof DialogCell && ((DialogCell) view).isBlocked()) {
                    showPremiumBlockedToast(view, ((DialogCell) view).getDialogId());
                    return;
                }
                if (clickSelectsDialog()) {
                    onItemLongClick(viewPage.listView, view, position, 0, 0, viewPage.dialogsType, viewPage.dialogsAdapter);
                    return;
                } else if (initialDialogsType == DIALOGS_TYPE_BOT_REQUEST_PEER && view instanceof TextCell) {
                    viewPage.dialogsAdapter.onCreateGroupForThisClick();
                    return;
                } else if ((initialDialogsType == DIALOGS_TYPE_IMPORT_HISTORY_GROUPS || initialDialogsType == DIALOGS_TYPE_IMPORT_HISTORY) && position == 1) {
                    Bundle args = new Bundle();
                    args.putBoolean("forImport", true);
                    long[] array = new long[]{getUserConfig().getClientUserId()};
                    args.putLongArray("result", array);
                    args.putInt("chatType", ChatObject.CHAT_TYPE_MEGAGROUP);
                    String title = arguments.getString("importTitle");
                    if (title != null) {
                        args.putString("title", title);
                    }
                    GroupCreateFinalActivity activity = new GroupCreateFinalActivity(args);
                    activity.setDelegate(new GroupCreateFinalActivity.GroupCreateFinalActivityDelegate() {
                        @Override
                        public void didStartChatCreation() {

                        }

                        @Override
                        public void didFinishChatCreation(GroupCreateFinalActivity fragment, long chatId) {
                            ArrayList<MessagesStorage.TopicKey> arrayList = new ArrayList<>();
                            arrayList.add(MessagesStorage.TopicKey.of(-chatId, 0));
                            DialogsActivityDelegate dialogsActivityDelegate = delegate;
                            if (closeFragment) {
                                removeSelfFromStack();
                            }
                            dialogsActivityDelegate.didSelectDialogs(DialogsActivity.this, arrayList, null, true, notify, scheduleDate, scheduleRepeatPeriod, null);
                        }

                        @Override
                        public void didFailChatCreation() {

                        }
                    });
                    presentFragment(activity);
                    return;
                } else if (view instanceof DialogsHintCell && (viewPage.dialogsType == 7 || viewPage.dialogsType == 8)) {
                    TL_chatlists.TL_chatlists_chatlistUpdates updates = viewPage.dialogsAdapter.getChatlistUpdate();
                    if (updates != null) {
                        MessagesController.DialogFilter filter = getMessagesController().selectedDialogFilter[viewPage.dialogsType - 7];
                        if (filter != null) {
                            showDialog(new FolderBottomSheet(DialogsActivity.this, filter.id, updates));
                        }
                        return;
                    }
                } else if (view instanceof DialogCell && !actionBar.isActionModeShowed() && !rightSlidingDialogContainer.hasFragment()) {
                    DialogCell dialogCell = (DialogCell) view;
                    AndroidUtilities.rectTmp.set(
                            dialogCell.avatarImage.getImageX(), dialogCell.avatarImage.getImageY(),
                            dialogCell.avatarImage.getImageX2(), dialogCell.avatarImage.getImageY2()
                    );
                }
                onItemClick(view, position, viewPage.dialogsAdapter, x, y);
            });
            viewPage.listView.setOnItemLongClickListener(new RecyclerListView.OnItemLongClickListenerExtended() {
                @Override
                public boolean onItemClick(View view, int position, float x, float y) {
                    if (view instanceof DialogCell && ((DialogCell) view).isBlocked()) {
                        showPremiumBlockedToast(view, ((DialogCell) view).getDialogId());
                        return true;
                    }
                    if (filterTabsView != null && filterTabsView.getVisibility() == View.VISIBLE && filterTabsView.isEditing()) {
                        return false;
                    }
                    return onItemLongClick(viewPage.listView, view, position, x, y, viewPage.dialogsType, viewPage.dialogsAdapter);
                }

                @Override
                public void onMove(float dx, float dy) {
                    if (AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y) {
                        movePreviewFragment(dy);
                    }
                }

                @Override
                public void onLongClickRelease() {
                    if (AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y) {
                        finishPreviewFragment();
                    }
                }
            });
            viewPage.swipeController = new SwipeController(viewPage);
            viewPage.recyclerItemsEnterAnimator = new RecyclerItemsEnterAnimator(viewPage.listView, false);

            viewPage.itemTouchhelper = new ItemTouchHelper(viewPage.swipeController);
            viewPage.itemTouchhelper.attachToRecyclerView(viewPage.listView);

            viewPage.listView.setOnScrollListener(new RecyclerView.OnScrollListener() {

                private boolean wasManualScroll;

                private boolean stoppedAllHeavyOperations;

                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        wasManualScroll = true;
                        scrollingManually = true;
                        viewPages[0].scroller.cancel();

                        if (fragmentSearchField.editText.getText().length() == 0 && fragmentSearchField.editText.hasFocus()) {
                            AndroidUtilities.hideKeyboard(fragmentSearchField.editText);
                            fragmentSearchField.editText.clearFocus();
                        }

                    } else {
                        scrollingManually = false;
                    }
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        wasManualScroll = false;
                        disableActionBarScrolling = false;
                        if (waitingForScrollFinished) {
                            waitingForScrollFinished = false;
                            if (updatePullAfterScroll) {
                                viewPage.listView.updatePullState();
                                updatePullAfterScroll = false;
                            }
                            viewPage.dialogsAdapter.notifyDataSetChanged();
                        }
                        checkAutoscrollToStories(viewPage);
                    }
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    if (contentView != null) {
                        contentView.updateBlurContent();
                    }
                    viewPage.dialogsItemAnimator.onListScroll(-dy);
                    int firstVisiblePosition = -1;
                    int lastVisiblePosition = -1;
                    for (int i = 0; i < recyclerView.getChildCount(); i++) {
                        int position = recyclerView.getChildAdapterPosition(recyclerView.getChildAt(i));
                        if (position >= 0) {
                            if (lastVisiblePosition == -1 || position > lastVisiblePosition) {
                                lastVisiblePosition = position;
                            }
                            if (firstVisiblePosition == -1 || position < firstVisiblePosition) {
                                firstVisiblePosition = position;
                            }
                        }
                    }
                    checkListLoad(viewPage);
                    invalidateScrollY = true;
                    if (fragmentView != null) {
                        fragmentView.invalidate();
                    }
                    if (initialDialogsType != DIALOGS_TYPE_WIDGET && wasManualScroll && recyclerView.getChildCount() > 0) {
                        if (firstVisiblePosition != RecyclerView.NO_POSITION) {
                            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(firstVisiblePosition);
                            if (!hasHiddenArchive() || holder != null && holder.getAdapterPosition() >= 0) {
                                int firstViewTop = 0;
                                if (holder != null) {
                                    firstViewTop = holder.itemView.getTop();
                                }
                                boolean goingDown;
                                boolean changed = true;
                                if (prevPosition == firstVisiblePosition) {
                                    final int topDelta = prevTop - firstViewTop;
                                    goingDown = firstViewTop < prevTop;
                                    changed = Math.abs(topDelta) > 1;
                                } else {
                                    goingDown = firstVisiblePosition > prevPosition;
                                }
                                if (changed && scrollUpdated && (goingDown || scrollingManually)) {
                                    hideFloatingButton(goingDown);
                                }
                                prevPosition = firstVisiblePosition;
                                prevTop = firstViewTop;
                                scrollUpdated = true;
                            }
                        }
                    }
                    if (!hasStories && recyclerView == viewPages[0].listView && !searching && actionBar != null && !actionBar.isActionModeShowed() && !disableActionBarScrolling && !rightSlidingDialogContainer.hasFragment()) {
                        if (dy > 0 && hasHiddenArchive() && viewPages[0].dialogsType == DIALOGS_TYPE_DEFAULT) {
                            View child = recyclerView.getChildAt(0);
                            if (child != null) {
                                RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(child);
                                if (holder.getAdapterPosition() == 0) {
                                    int visiblePartAfterScroll = child.getMeasuredHeight() + (child.getTop() - recyclerView.getPaddingTop());
                                    if (visiblePartAfterScroll + dy > 0) {
                                        if (visiblePartAfterScroll < 0) {
                                            dy = -visiblePartAfterScroll;
                                        } else {
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                        float currentTranslation = scrollYOffset;
                        float newTranslation = currentTranslation - dy;
                        boolean applyScrollY = true;
                        applyScrollY = false;
                        invalidateScrollY = true;
                        if (fragmentView != null) {
                            fragmentView.invalidate();
                        }
                        if (applyScrollY) {
                            int maxScrollYOffset = getMaxScrollYOffset();
                            if (!(filterTabsView != null && filterTabsView.getVisibility() == View.VISIBLE && animatorFilterTabsVisible.getValue())) {
                                maxScrollYOffset = dp(SEARCH_FIELD_HEIGHT);
                            }
                            if (newTranslation < -maxScrollYOffset) {
                                newTranslation = -maxScrollYOffset;
                            } else if (newTranslation > 0) {
                                newTranslation = 0;
                            }
                            if (newTranslation != currentTranslation) {
                                setScrollY(newTranslation);
                            }
                        }
                    }
                    if (fragmentView != null) {
                        blur3_InvalidateBlur();
                    }
                    if (rightSlidingDialogContainer != null && rightSlidingDialogContainer.hasFragment() && viewPage.listView != null) {
                        viewPage.listView.invalidate();
                    }
                    if (dialogStoriesCell != null && dialogStoriesCell.getPremiumHint() != null && dialogStoriesCell.getPremiumHint().shown()) {
                        dialogStoriesCell.getPremiumHint().hide();
                    }

                    final int topIndex = hasHiddenArchive() ? 1 : 0;
                    final View topChild = viewPage.listView.getChildAt(topIndex);
                    final int firstViewTop = topChild != null ? topChild.getTop() : 0;
                    final boolean shadowVisible = !(firstVisiblePosition <= topIndex && (firstViewTop - scrollYOffset + dp(5)) >= viewPage.listView.getPaddingTop());
                    animatorShadowVisible.setValue(shadowVisible, true);
                    if (dy != 0) {
                        if (scrollableViewNoiseSuppressor != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            scrollableViewNoiseSuppressor.onScrolled(dx, dy);
                        }
                    }
                }
            });

            viewPage.archivePullViewState = SharedConfig.archiveHidden ? ARCHIVE_ITEM_STATE_HIDDEN : ARCHIVE_ITEM_STATE_PINNED;
            if (viewPage.pullForegroundDrawable == null && folderId == 0 && communityId == 0) {
                viewPage.pullForegroundDrawable = new PullForegroundDrawable(LocaleController.getString(R.string.AccSwipeForArchive), LocaleController.getString(R.string.AccReleaseForArchive)) {
                    @Override
                    protected float getViewOffset() {
                        return viewPage.listView.getViewOffset();
                    }
                };
                if (hasHiddenArchive()) {
                    viewPage.pullForegroundDrawable.showHidden();
                } else {
                    viewPage.pullForegroundDrawable.doNotShow();
                }
                viewPage.pullForegroundDrawable.setWillDraw(viewPage.archivePullViewState != ARCHIVE_ITEM_STATE_PINNED);
            }

            viewPage.dialogsAdapter = new DialogsAdapter(this, context, viewPage.dialogsType, folderId, onlySelect, selectedDialogs, currentAccount, requestPeerType) {
                @Override
                public void notifyDataSetChanged() {
                    viewPage.lastItemsCount = getItemCount();
                    try {
                        super.notifyDataSetChanged();
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                    if (initialDialogsType == DIALOGS_TYPE_BOT_REQUEST_PEER) {
                        searchItem.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                    }
                }

                @Override
                public void onButtonClicked(DialogCell dialogCell) {
                    if (dialogCell.getMessage() != null) {
                        TLRPC.TL_forumTopic topic = getMessagesController().getTopicsController().findTopic(-dialogCell.getDialogId(), MessageObject.getTopicId(currentAccount, dialogCell.getMessage().messageOwner, true));
                        if (topic != null) {
                            if (onlySelect) {
                                didSelectResult(dialogCell.getDialogId(), topic.id, false, false);
                            } else {
                                ForumUtilities.openTopic(DialogsActivity.this, -dialogCell.getDialogId(), topic, 0);
                            }
                        }
                    }
                }

                @Override
                public void onButtonLongPress(DialogCell dialogCell) {
                    onItemLongClick(viewPage.listView, dialogCell, viewPage.listView.getChildAdapterPosition(dialogCell), 0, 0, viewPage.dialogsType, viewPage.dialogsAdapter);
                }

                @Override
                public void onCreateGroupForThisClick() {
                    createGroupForThis();
                }

                @Override
                protected void onArchiveSettingsClick() {
                    presentFragment(new ArchiveSettingsActivity());
                }

                @Override
                protected boolean showOpenBotButton() {
                    return initialDialogsType == DIALOGS_TYPE_DEFAULT;
                }
                @Override
                protected void onOpenBot(TLRPC.User bot) {
                    MessagesController.getInstance(currentAccount).openApp(bot, 0);
                }
            };
            viewPage.dialogsAdapter.setRecyclerListView(viewPage.listView);
            viewPage.dialogsAdapter.setForceShowEmptyCell(afterSignup);
            if (viewPage.dialogsType == DIALOGS_TYPE_FORWARD) {
                viewPage.dialogsAdapter.setAllowForwardAsStories(getMessagesController().storiesEnabled() && delegate != null && delegate.canSelectStories());
            }

            if (AndroidUtilities.isTablet() && openedDialogId.dialogId != 0) {
                viewPage.dialogsAdapter.setOpenedDialogId(openedDialogId.dialogId);
            }
            viewPage.dialogsAdapter.setArchivedPullDrawable(viewPage.pullForegroundDrawable);
            viewPage.listView.setAdapter(viewPage.dialogsAdapter);

            viewPage.listView.setEmptyView(folderId == 0 && communityId == 0 ? viewPage.progressView : null);
            viewPage.scrollHelper = new RecyclerAnimationScrollHelper(viewPage.listView, viewPage.layoutManager);
            viewPage.scrollHelper.forceUseStableId = true;
            viewPage.scrollHelper.isDialogs = true;
            viewPage.scrollHelper.setScrollListener(() -> {
                invalidateScrollY = true;
                fragmentView.invalidate();
            });

            if (a != 0) {
                viewPages[a].setVisibility(View.GONE);
            }
        }

        topBubblesFadeView = new DialogsActivityTopBubblesFadeView(context);
        topBubblesFadeView.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        contentView.addView(topBubblesFadeView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 100, Gravity.TOP));

        searchViewPagerIndex = contentView.getChildCount();

        searchTabsAndFiltersLayout = new SearchTabsAndFiltersLayout(getContext());
        searchTabsAndFiltersLayout.setPadding(0, dp(7), 0, dp(7));
        contentView.addView(searchTabsAndFiltersLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, SEARCH_TABS_HEIGHT, Gravity.TOP, 4, 0, 4, 0));

        BlurredBackgroundDrawable searchTabsViewBackground = iBlur3FactoryLiquidGlass.create(searchTabsAndFiltersLayout, BlurredBackgroundProviderImpl.topPanel(resourceProvider));
        searchTabsViewBackground.setRadius(dp(18));
        searchTabsViewBackground.setPadding(dp(6.666f));
        searchTabsAndFiltersLayout.setPadding(0, dp(7), 0, dp(7));
        searchTabsAndFiltersLayout.setBlurredBackground(searchTabsViewBackground);

        filtersView = new FiltersView(getParentActivity(), null);
        filtersView.setPadding(0, dp(3), 0, dp(3));
        filtersView.drawDivider = false;
        filtersView.setOnItemClickListener((view, position) -> {
            filtersView.cancelClickRunnables(true);
            addSearchFilter(filtersView.getFilterAt(position));
        });
        searchTabsAndFiltersLayout.addView(filtersView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP));

        floatingButtonStories = new FragmentFloatingButton(context, resourceProvider, true);
        floatingButtonStories.setContentDescription(getString(R.string.StoryPrivacyButtonPost));
        floatingButtonStories.setImageResource(R.drawable.outline_fab_story_24);
        floatingButtonStories.setOnClickListener(v -> openStoriesRecorder());
        contentView.addView(floatingButtonStories, FragmentFloatingButton.createSubButtonLayoutParams());

        floatingButton3 = new FragmentFloatingButton(context, resourceProvider);
        contentView.addView(floatingButton3, FragmentFloatingButton.createDefaultLayoutParams());
        floatingButton3.setOnClickListener(v -> {
            if (parentLayout != null && parentLayout.isInPreviewMode()) {
                finishPreviewFragment();
                return;
            }
            if (initialDialogsType == DIALOGS_TYPE_WIDGET) {
                if (delegate == null || selectedDialogs.isEmpty()) {
                    return;
                }
                ArrayList<MessagesStorage.TopicKey> topicKeys = new ArrayList<>();
                for (int i = 0; i < selectedDialogs.size(); i++) {
                    topicKeys.add(MessagesStorage.TopicKey.of(selectedDialogs.get(i), 0));
                }
                delegate.didSelectDialogs(DialogsActivity.this, topicKeys, null, false, notify, scheduleDate, scheduleRepeatPeriod, null);
            } else {
                if (MessagesController.getInstance(currentAccount).isFrozen()) {
                    AccountFrozenAlert.show(currentAccount);
                    return;
                }
                openWriteContacts();
            }
        });

        if (!isArchive() && initialDialogsType == DIALOGS_TYPE_DEFAULT) {
            if (MessagesController.getInstance(currentAccount).getMainSettings().getBoolean("storyhint", true)) {
                storyHint = new HintView2(context, HintView2.DIRECTION_RIGHT)
                        .setRounding(8)
                        .setDuration(8_000)
                        .setCloseButton(true)
                        .setMaxWidth(165)
                        .setMultilineText(true)
                        .setText(AndroidUtilities.replaceCharSequence("%s", LocaleController.getString(R.string.StoryCameraHint), StoryRecorder.cameraBtnSpan(context)))
                        .setJoint(1, -40)
                        .setBgColor(getThemedColor(Theme.key_undo_background))
                        .setOnHiddenListener(() -> MessagesController.getInstance(currentAccount).getMainSettings().edit().putBoolean("storyhint", false).commit());
                contentView.addView(storyHint, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 160, Gravity.BOTTOM | Gravity.FILL_HORIZONTAL, 0, 0, 80, 0));
            }
        }

        updateStoriesPosting();

        searchTabsView = null;

        if (!onlySelect && initialDialogsType == 0) {
            topPanelLayout = new DialogsActivityTopPanelLayout(context);
            topPanelLayout.setOnAnimatedHeightChangedListener(() -> {
                viewPages[0].listView.requestLayout();

                TopicsFragment topicsFragment = null;
                if (rightSlidingDialogContainer != null && rightSlidingDialogContainer.getFragment() instanceof TopicsFragment) {
                    topicsFragment = (TopicsFragment) rightSlidingDialogContainer.getFragment();
                }
                if (topicsFragment != null) {
                    topicsFragment.checkUi_listViewPadding();
                }
                checkUi_searchPagesPaddings(false);
                updateContextViewPosition();
                if (searchViewPager != null) {
                    searchViewPager.invalidate();
                }
            });

            BlurredBackgroundDrawable topPanelLayoutBackground = iBlur3FactoryLiquidGlass.create(topPanelLayout)
                .setColorProvider(BlurredBackgroundProviderImpl.topPanel(resourceProvider))
                .setPadding(dp(7));

            topPanelLayout.setPadding(dp(11), dp(21), dp(11), dp(21));
            topPanelLayout.setBlurredBackground(topPanelLayoutBackground);
            topPanelLayout.setDefaultRadiusDp(communityId != 0 ? 18 : 24);

            fragmentLocationContextViewWrapper = new FrameLayout(context);
            topPanelLayout.addView(fragmentLocationContextViewWrapper);
            topPanelLayout.setPriority(fragmentLocationContextViewWrapper, 5);
            topPanelLayout.setDebugName(fragmentLocationContextViewWrapper, "fragment location");
            topPanelLayout.setViewVisible(fragmentLocationContextViewWrapper, true, false);

            fragmentContextViewWrapper = new FrameLayout(context);
            topPanelLayout.addView(fragmentContextViewWrapper);
            topPanelLayout.setPriority(fragmentContextViewWrapper, 4);
            topPanelLayout.setDebugName(fragmentContextViewWrapper, "fragment context");
            topPanelLayout.setViewVisible(fragmentContextViewWrapper, true, false);

            fragmentLocationContextView = new FragmentContextView(context, this, true) {
                @Override
                public void setVisibility(int visibility) {
                    topPanelLayout.setViewVisible(fragmentLocationContextViewWrapper, visibility == VISIBLE);
                }
            };
            fragmentLocationContextViewWrapper.addView(fragmentLocationContextView);

            fragmentContextView = new FragmentContextView(context, this, false) {
                @Override
                public void setVisibility(int visibility) {
                    topPanelLayout.setViewVisible(fragmentContextViewWrapper, visibility == VISIBLE);
                }
            };
            fragmentContextViewWrapper.addView(fragmentContextView);
            topPanelLayout.setCallFragmentContextView(fragmentContextView);

            dialogsHintCell = new DialogsHintCell(context);
            dialogsHintCell.setBackground(Theme.getSelectorDrawable(false));
            updateDialogsHint();
            CacheControlActivity.calculateTotalSize(size -> {
                cacheSize = size;
                updateDialogsHint();
            });
            CacheControlActivity.getDeviceTotalSize((totalSize, totalFreeSize) -> {
                deviceSize = totalSize;
                updateDialogsHint();
            });
            topPanelLayout.addView(dialogsHintCell);


            if (communityId != 0) {
                communityPendingRequests = new CommunityRequestsCell(context, resourceProvider, true);
                communityPendingRequests.set(IconBackgroundColors.BLUE_ALT.top, IconBackgroundColors.BLUE_ALT.bottom,
                    R.drawable.filled_requests_24, getString(R.string.CommunityPendingRequests), null, false);
                communityPendingRequests.setUnreadMode(true);
                communityPendingRequests.setBackground(Theme.getSelectorDrawable(false));
                communityPendingRequests.setOnClickListener(v -> {
                    Bundle args = new Bundle();
                    args.putLong("community_id", communityId);
                    presentFragment(new CommunityPendingRequestsActivity(args));
                });
                topPanelLayout.addView(communityPendingRequests);
                checkCommunityPendingRequestsVisible(false);
            }
        } else if (initialDialogsType == DIALOGS_TYPE_FORWARD || clickSelectsDialog()) {
            chatInputViewsContainer = new ChatInputViewsContainer(context);
            chatInputViewsContainer.setClipChildren(false);
            chatInputViewsContainer.setWindowInsetsProvider(windowInsetsStateHolder);
            chatInputViewsContainer.setInputIslandBubbleDrawable(
                iBlur3FactoryLiquidGlass.create(chatInputViewsContainer, BlurredBackgroundProviderImpl.inputFieldDialogActivity(resourceProvider)));
            chatInputViewsContainer.setUnderKeyboardBackgroundDrawable(
                iBlur3FactoryFrostedLiquidGlass.create(chatInputViewsContainer, BlurredBackgroundProviderImpl.inputFieldDialogActivity(resourceProvider)));

            BlurredBackgroundWithFadeDrawable fadeDrawable = new BlurredBackgroundWithFadeDrawable(
                    iBlur3FactoryFade.create(chatInputViewsContainer, null));
            if (!SharedConfig.chatBlurEnabled() || LiteMode.isEnabled(LiteMode.FLAG_LIQUID_GLASS)) {
                fadeDrawable.setFadeHeight(dp(72), true);
            }

            chatInputViewsContainer.setBackgroundWithFadeDrawable(fadeDrawable);

            chatInputBubbleContainer = chatInputViewsContainer.getInputIslandBubbleContainer();
            chatInputBubbleContainer.setClipChildren(false);

            chatInputInAppContainer = chatInputViewsContainer.getInAppKeyboardBubbleContainer();

            if (commentView != null) {
                commentView.onDestroy();
            }
            commentView = new ChatActivityEnterView(getParentActivity(), contentView, null, false) {
                @Override
                protected void onChangedIslandTotalHeight(float h) {
                    chatInputViewsContainer.setInputBubbleHeight(h);
                    checkUi_chatListViewPaddingsBottom();
                    blur3_InvalidateBlur();
                    checkUi_fadeView();
                }

                @Override
                public boolean dispatchTouchEvent(MotionEvent ev) {
                    if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                        AndroidUtilities.requestAdjustResize(getParentActivity(), classGuid);
                    }
                    return super.dispatchTouchEvent(ev);
                }

                @Override
                public long getStarsPrice() {
                    long price = 0;
                    if (selectedDialogs != null) {
                        for (final long did : selectedDialogs) {
                            long dialogPrice = getMessagesController().getSendPaidMessagesStars(did);
                            if (dialogPrice <= 0 && did > 0) {
                                dialogPrice = DialogObject.getMessagesStarsPrice(getMessagesController().isUserContactBlocked(did));
                            }
                            price += dialogPrice;
                        }
                    }
                    return price;
                }

                @Override
                public int getMessagesCount() {
                    return Math.max(1, DialogsActivity.this.messagesCount + (TextUtils.isEmpty(commentView == null ? "" : commentView.getFieldText()) ? 0 : 1));
                }
            };
            commentView.setInAppInsetsController(windowInsetsStateHolder);
            commentView.shouldDrawBackground = false;
            contentView.setClipChildren(false);
            contentView.setClipToPadding(false);
            commentView.allowBlur = false;
            commentView.forceSmoothKeyboard(true);
            commentView.setAllowStickersAndGifs(true, false, false);
            commentView.setForceShowSendButton(true, false);
            commentView.textFieldContainer.setPadding(0, dp(1), dp(20), 0);
            commentView.getSendButton().setAlpha(0);

            commentView.setViewParentForEmoji(chatInputInAppContainer);
            chatInputBubbleContainer.addView(commentView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM, 7, 0, 7, 0));
            contentView.addView(chatInputViewsContainer.getFadeView(), LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            contentView.addView(chatInputViewsContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            if (hasSharedMediaEntries() || sharedLink != null || sharedTextSeed != null) {
                attachShareTopView(pendingSharedCaption);
                pendingSharedCaption = null;
            }

            commentView.setDelegate(new ChatActivityEnterView.ChatActivityEnterViewDelegate() {
                @Override
                public void onMessageSend(CharSequence message, boolean notify, int scheduleDate, int scheduleRepeatPeriod, long payStars) {
                    if (delegate == null || selectedDialogs.isEmpty()) {
                        return;
                    }
                    ArrayList<MessagesStorage.TopicKey> topicKeys = new ArrayList<>();
                    for (int i = 0; i < selectedDialogs.size(); i++) {
                        topicKeys.add(MessagesStorage.TopicKey.of(selectedDialogs.get(i), 0));
                    }
                    delegate.didSelectDialogs(DialogsActivity.this, topicKeys, message, false, notify, scheduleDate, scheduleRepeatPeriod, null);
                }

                @Override
                public void onSwitchRecordMode(boolean video) {

                }

                @Override
                public void onTextSelectionChanged(int start, int end) {

                }

                @Override
                public void bottomPanelTranslationYChanged(float translation) {

                }

                @Override
                public void onStickersExpandedChange() {

                }

                @Override
                public void onPreAudioVideoRecord() {

                }

                @Override
                public void onTextChanged(final CharSequence text, boolean bigChange, boolean fromDraft) {
                    AndroidUtilities.runOnUIThread(DialogsActivity.this::updateSelectedCount, 100);
                    if (shareTopView != null) {
                        if (bigChange) {
                            shareTopView.onTextChanged(text, true);
                        } else {
                            if (shareLinkSearchRunnable != null) {
                                AndroidUtilities.cancelRunOnUIThread(shareLinkSearchRunnable);
                            }
                            shareLinkSearchRunnable = () -> {
                                shareLinkSearchRunnable = null;
                                if (shareTopView != null) shareTopView.onTextChanged(text, false);
                            };
                            AndroidUtilities.runOnUIThread(shareLinkSearchRunnable, 1000);
                        }
                    }
                }

                @Override
                public void onTextSpansChanged(CharSequence text) {

                }

                @Override
                public void needSendTyping() {

                }

                @Override
                public void onAttachButtonHidden() {

                }

                @Override
                public void onAttachButtonShow() {

                }

                @Override
                public void onMessageEditEnd(boolean loading) {

                }

                @Override
                public boolean isVideoRecordingPaused() {
                    return false;
                }

                @Override
                public void onWindowSizeChanged(int size) {

                }

                @Override
                public void onStickersTab(boolean opened) {

                }

                @Override
                public void didPressAttachButton() {

                }

                @Override
                public void needStartRecordVideo(int state, boolean notify, int scheduleDate, int scheduleRepeatPeriod, int ttl, long effectId, long stars) {

                }

                @Override
                public void toggleVideoRecordingPause() {

                }

                @Override
                public void needChangeVideoPreviewState(int state, float seekProgress) {

                }

                @Override
                public void needStartRecordAudio(int state) {

                }

                @Override
                public void needShowMediaBanHint() {

                }

                @Override
                public void onUpdateSlowModeButton(View button, boolean show, CharSequence time) {

                }

                @Override
                public void onSendLongClick() {

                }

                @Override
                public void onAudioVideoInterfaceUpdated() {

                }
            });

            writeButton = new ChatActivityEnterView.SendButton(context, R.drawable.send_plane_24, resourceProvider) {
                @Override
                public boolean isOpen() {
                    return true;
                }

                @Override
                public boolean isInScheduleMode() {
                    return super.isInScheduleMode();
                }

                @Override
                public boolean isInactive() {
                    return false;
                }

                @Override
                public boolean shouldDrawBackground() {
                    return true;
                }

                @Override
                public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
                    super.onInitializeAccessibilityNodeInfo(info);
                    info.setText(LocaleController.formatPluralString("AccDescrShareInChats", selectedDialogs.size()));
                    info.setClassName(Button.class.getName());
                    info.setLongClickable(true);
                    info.setClickable(true);
                }
            };
            writeButton.setCircleSize(dp(52), dp(38));
            writeButton.setCirclePadding(dp(7), dp(8));
            writeButton.newCounterPos = true;
            contentView.addView(writeButton, LayoutHelper.createFrame(110, 50, Gravity.RIGHT | Gravity.BOTTOM));
            writeButton.setScrimViewBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
            writeButton.setOnClickListener(v -> {
                if (delegate == null || selectedDialogs.isEmpty()) {
                    return;
                }
                ArrayList<MessagesStorage.TopicKey> topicKeys = new ArrayList<>();
                for (int i = 0; i < selectedDialogs.size(); i++) {
                    topicKeys.add(MessagesStorage.TopicKey.of(selectedDialogs.get(i), 0));
                }
                delegate.didSelectDialogs(DialogsActivity.this, topicKeys, commentView.getFieldText(), false, notify, scheduleDate, scheduleRepeatPeriod, null);
            });
            writeButton.setOnLongClickListener(this::onSendLongClick);
            writeButton.setVisibility(View.GONE);
            writeButton.setScaleX(.2f);
            writeButton.setScaleY(.2f);
            writeButton.setAlpha(0);

            textPaint.setTextSize(dp(12));
            textPaint.setTypeface(AndroidUtilities.bold());
        }

        if (filterTabsView != null) {
            BlurredBackgroundDrawable filterTabsViewBackground = iBlur3FactoryLiquidGlass.create(filterTabsView, BlurredBackgroundProviderImpl.topPanel(resourceProvider));
            filterTabsViewBackground.setRadius(dp(18));
            filterTabsViewBackground.setPadding(dp(6.666f));
            filterTabsView.setPadding(0, dp(7), 0, dp(7));
            filterTabsView.setBlurredBackground(filterTabsViewBackground);
            contentView.addView(filterTabsView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 36 + 7 + 7, Gravity.TOP, 4, 0, 4, 0));
        }

        if (fragmentSearchField != null) {
            fragmentSearchField.setupBlurredBackground(iBlur3FactoryLiquidGlass.create(fragmentSearchField, BlurredBackgroundProviderImpl.topPanel(resourceProvider)));
        }

        dialogStoriesCell = new DialogStoriesCell(context, this, currentAccount, isArchive() ? DialogStoriesCell.TYPE_ARCHIVE : DialogStoriesCell.TYPE_DIALOGS) {
            @Override
            public void onUserLongPressed(View view, long dialogId) {
                MediaDataController.getInstance(currentAccount).loadHints(true);
                filterOptions = ItemOptions.makeOptions(DialogsActivity.this, view)
                    .setViewAdditionalOffsets(0, dp(8), 0, 0)
                    .setScrimViewBackground(Theme.createRoundRectDrawable(
                        dp(12), dp(12),
                        getThemedColor(Theme.key_windowBackgroundWhite)
                    ))
                    .translate(0, dp(8));
                if (UserObject.isService(dialogId)) {
                    BotWebViewVibrationEffect.APP_ERROR.vibrate();
                    return;
                }
                try {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                } catch (Exception ignored) {}
                if (dialogId == UserConfig.getInstance(currentAccount).getClientUserId()) {
                    if (!storiesEnabled) {
                        if (dialogStoriesCell != null) {
                            dialogStoriesCell.showPremiumHint();
                        }
                        return;
                    }
                    filterOptions.add(R.drawable.msg_stories_add, LocaleController.getString(R.string.AddStory), Theme.key_actionBarDefaultSubmenuItemIcon, Theme.key_actionBarDefaultSubmenuItem, () -> {
                        dialogStoriesCell.openStoryRecorder();
                    });
                    filterOptions.add(R.drawable.msg_stories_archive, LocaleController.getString(R.string.ArchivedStories), Theme.key_actionBarDefaultSubmenuItemIcon, Theme.key_actionBarDefaultSubmenuItem, () -> {
                        Bundle args = new Bundle();
                        args.putLong("dialog_id", UserConfig.getInstance(currentAccount).getClientUserId());
                        args.putInt("type", MediaActivity.TYPE_STORIES);
                        args.putInt("start_from", SharedMediaLayout.TAB_ARCHIVED_STORIES);
                        MediaActivity mediaActivity = new MediaActivity(args, null);
                        presentFragment(mediaActivity);
                    });
                    filterOptions.add(R.drawable.msg_stories_saved, LocaleController.getString(R.string.SavedStories), Theme.key_actionBarDefaultSubmenuItemIcon, Theme.key_actionBarDefaultSubmenuItem, () -> {
                        Bundle args = new Bundle();
                        args.putLong("dialog_id", UserConfig.getInstance(currentAccount).getClientUserId());
                        args.putInt("type", MediaActivity.TYPE_STORIES);
                        presentFragment(new MediaActivity(args, null));
                    });
                } else {
                    TLRPC.User user = getMessagesController().getUser(dialogId);
                    TLRPC.Chat chat = getMessagesController().getChat(-dialogId);
                    final String key = NotificationsController.getSharedPrefKey(dialogId, 0);
                    boolean muted = !NotificationsCustomSettingsActivity.areStoriesNotMuted(currentAccount, dialogId);
                    boolean isPremiumBlocked = MessagesController.getInstance(currentAccount).premiumFeaturesBlocked();
                    boolean isPremium = UserConfig.getInstance(currentAccount).isPremium();
                    boolean isUnread = MessagesController.getInstance(currentAccount).getStoriesController().hasUnreadStories(dialogId);
                    boolean isLive = MessagesController.getInstance(currentAccount).getStoriesController().hasLiveStory(dialogId);
                    CombinedDrawable stealthModeLockedDrawable = null;
                    if (!isPremiumBlocked && dialogId > 0 && !isPremium) {
                        Drawable lockIcon = ContextCompat.getDrawable(getContext(), R.drawable.msg_gallery_locked2);
                        if (lockIcon != null) {
                            Drawable stealthDrawable = ContextCompat.getDrawable(getContext(), R.drawable.msg_stealth_locked);
                            if (stealthDrawable != null) {
                                stealthDrawable.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_actionBarDefaultSubmenuItemIcon), PorterDuff.Mode.MULTIPLY));
                            }

                            lockIcon.setColorFilter(new PorterDuffColorFilter(ColorUtils.blendARGB(Color.WHITE, Color.BLACK, 0.5f), PorterDuff.Mode.MULTIPLY));
                            stealthModeLockedDrawable = new CombinedDrawable(stealthDrawable, lockIcon);
                        }
                    }
                    if (dialogId < 0 && getStoriesController().canPostStories(dialogId)) {
                        filterOptions.add(R.drawable.msg_stories_add, LocaleController.getString(R.string.AddStory), Theme.key_actionBarDefaultSubmenuItemIcon, Theme.key_actionBarDefaultSubmenuItem, () -> {
                            dialogStoriesCell.openStoryRecorder(dialogId);
                        });
                    }
                    final boolean fromTopPeer = user != null && !user.contact && MediaDataController.getInstance(currentAccount).containsTopPeer(dialogId);
                    filterOptions
                            .addIf(dialogId > 0, R.drawable.msg_discussion, LocaleController.getString(R.string.SendMessage), () -> {
                                presentFragment(ChatActivity.of(dialogId));
                            })
                            .addIf(dialogId > 0, R.drawable.msg_openprofile, LocaleController.getString(R.string.OpenProfile), () -> {
                                presentFragment(ProfileActivity.of(dialogId));
                            })
                            .addIf(dialogId < 0, R.drawable.msg_channel, LocaleController.getString(ChatObject.isChannelAndNotMegaGroup(chat) ? R.string.OpenChannel2 : R.string.OpenGroup2), () -> {
                                presentFragment(ChatActivity.of(dialogId));
                            }).addIf(!muted && dialogId > 0, R.drawable.msg_mute, LocaleController.getString(R.string.NotificationsStoryMute2), () -> {
                                MessagesController.getNotificationsSettings(currentAccount).edit().putBoolean("stories_" + key, false).apply();
                                getNotificationsController().updateServerNotificationsSettings(dialogId, 0);
                                String name = user == null ? "" : user.first_name.trim();
                                int index = name.indexOf(" ");
                                if (index > 0) {
                                    name = name.substring(0, index);
                                }
                                BulletinFactory.of(DialogsActivity.this).createUsersBulletin(Arrays.asList(user), AndroidUtilities.replaceTags(LocaleController.formatString("NotificationsStoryMutedHint", R.string.NotificationsStoryMutedHint, name))).show();
                            }).makeMultiline(false).addIf(muted && dialogId > 0, R.drawable.msg_unmute, LocaleController.getString(R.string.NotificationsStoryUnmute2), () -> {
                                MessagesController.getNotificationsSettings(currentAccount).edit().putBoolean("stories_" + key, true).apply();
                                getNotificationsController().updateServerNotificationsSettings(dialogId, 0);
                                String name = user == null ? "" : user.first_name.trim();
                                int index = name.indexOf(" ");
                                if (index > 0) {
                                    name = name.substring(0, index);
                                }
                                BulletinFactory.of(DialogsActivity.this).createUsersBulletin(Arrays.asList(user), AndroidUtilities.replaceTags(LocaleController.formatString("NotificationsStoryUnmutedHint", R.string.NotificationsStoryUnmutedHint, name))).show();
                            }).makeMultiline(false).addIf(!isPremiumBlocked && dialogId > 0 && isPremium && isUnread && !isLive, R.drawable.msg_stories_stealth2, LocaleController.getString(R.string.ViewAnonymously), () -> {
                                TL_stories.TL_storiesStealthMode stealthMode = MessagesController.getInstance(UserConfig.selectedAccount).getStoriesController().getStealthMode();
                                if (stealthMode != null && ConnectionsManager.getInstance(currentAccount).getCurrentTime() < stealthMode.active_until_date) {
                                    if (view instanceof StoryCell) {
                                        dialogStoriesCell.openStoryForCell((StoryCell) view);
                                    }
                                } else {
                                    StealthModeAlert stealthModeAlert = new StealthModeAlert(getContext(), 0, StealthModeAlert.TYPE_FROM_DIALOGS, resourceProvider);
                                    stealthModeAlert.setListener(isStealthModeEnabled -> {
                                        if (view instanceof StoryCell) {
                                            dialogStoriesCell.openStoryForCell((StoryCell) view);
                                            if (isStealthModeEnabled) {
                                                AndroidUtilities.runOnUIThread(StealthModeAlert::showStealthModeEnabledBulletin, 500);
                                            }
                                        }
                                    });
                                    showDialog(stealthModeAlert);
                                }
                            }).makeMultiline(false).addIf(!isPremiumBlocked && dialogId > 0 && !isPremium && isUnread && !isLive, R.drawable.msg_stories_stealth2, stealthModeLockedDrawable, LocaleController.getString(R.string.ViewAnonymously), () -> {
                                StealthModeAlert stealthModeAlert = new StealthModeAlert(getContext(), 0, StealthModeAlert.TYPE_FROM_DIALOGS, resourceProvider);
                                stealthModeAlert.setListener(isStealthModeEnabled -> {
                                    if (view instanceof StoryCell) {
                                        dialogStoriesCell.openStoryForCell((StoryCell) view);
                                        if (isStealthModeEnabled) {
                                            AndroidUtilities.runOnUIThread(StealthModeAlert::showStealthModeEnabledBulletin, 500);
                                        }
                                    }
                                });
                                showDialog(stealthModeAlert);
                            }).makeMultiline(false).addIf(!fromTopPeer && !isArchive(), R.drawable.msg_archive, LocaleController.getString(R.string.ArchivePeerStories), () -> {
                                toggleArciveForStory(dialogId);
                            }).makeMultiline(false).addIf(!fromTopPeer && isArchive(), R.drawable.msg_unarchive, LocaleController.getString(R.string.UnarchiveStories), () -> {
                                toggleArciveForStory(dialogId);
                            }).makeMultiline(false).addIf(fromTopPeer, R.drawable.msg_delete, getString(R.string.StoriesRemoveFromRecent), () -> {
                                MediaDataController.getInstance(currentAccount).removePeer(dialogId);
                                getMessagesController().getStoriesController().toggleHidden(dialogId, true, false, true);
                            });
                }
                filterOptions.setGravity(Gravity.LEFT)
                        .translate(dp(-8), dp(-10))
                        .show();
            }

            @Override
            public void onMiniListClicked() {
                if (hasOnlySlefStories && getStoriesController().hasOnlySelfStories()) {
                    dialogStoriesCell.openSelfStories();
                } else {
                    scrollToTop(true, true);
                }
            }

            @Override
            public boolean dispatchTouchEvent(MotionEvent ev) {
                return !actionBar.isActionModeShowed() && super.dispatchTouchEvent(ev);
            }
        };
        dialogStoriesCell.setActionBar(actionBar);
        dialogStoriesCell.setMenuItemsOffset(isArchive() ? dp(68) : dpf2(16.66f));
        dialogStoriesCell.allowGlobalUpdates = false;
        dialogStoriesCell.setVisibility(View.GONE);
        animateToHasStories = false;
        hasOnlySlefStories = false;
        hasStories = false;

        if (onlySelect && initialDialogsType == DIALOGS_TYPE_FORWARD) {
            MessagesController.getInstance(currentAccount).getSavedReactionTags(0);
        }

        //if (!onlySelect || initialDialogsType == DIALOGS_TYPE_FORWARD) {
            final FrameLayout.LayoutParams layoutParams = LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT);
            contentView.addView(actionBar, layoutParams);
        //}
        if (!onlySelect) {
            animatedStatusView = new AnimatedStatusView(context, 20, 60);
            contentView.addView(animatedStatusView, LayoutHelper.createFrame(20, 20, Gravity.LEFT | Gravity.TOP));
        }
        if (fragmentSearchField != null) {
            contentView.addView(fragmentSearchField, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.TOP, 7, -2, 7, 0));
        }


        undoViewIndex = contentView.getChildCount();
        undoView[0] = null;
        undoView[1] = null;

        if (hasMainTabs) {
            actionBar.getTitlesContainer().setTranslationX(dp(4));
            actionBar.setTitleColor(getThemedColor(Theme.key_telegram_color_dialogsLogo));
        }

        if (folderId != 0 || communityId != 0) {
            viewPages[0].listView.setGlowColor(getThemedColor(Theme.key_windowBackgroundWhite));
            actionBar.setItemsColor(getThemedColor(Theme.key_actionBarDefaultArchivedIcon), false);
            actionBar.setItemsBackgroundColor(getThemedColor(Theme.key_actionBarDefaultArchivedSelector), false);
            actionBar.setSearchTextColor(getThemedColor(Theme.key_actionBarDefaultArchivedSearch), false);
            actionBar.setSearchTextColor(getThemedColor(Theme.key_actionBarDefaultArchivedSearchPlaceholder), true);
        }

        if (!onlySelect && initialDialogsType == DIALOGS_TYPE_DEFAULT) {
            blurredView = new View(context) {
                @Override
                public void setAlpha(float alpha) {
                    super.setAlpha(alpha);
                    if (fragmentView != null) {
                        fragmentView.invalidate();
                    }
                }
            };
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                blurredView.setForeground(new ColorDrawable(ColorUtils.setAlphaComponent(getThemedColor(Theme.key_windowBackgroundWhite), 100)));
            }
            blurredView.setFocusable(false);
            blurredView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            blurredView.setOnClickListener(e -> {
                finishPreviewFragment();
            });
            blurredView.setVisibility(View.GONE);
            contentView.addView(blurredView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        }

        actionBarDefaultPaint.setColor(getThemedColor(Theme.key_windowBackgroundWhite));
        /*
        if (inPreviewMode) {
            final TLRPC.User currentUser = getUserConfig().getCurrentUser();
            avatarContainer = new ChatAvatarContainer(actionBar.getContext(), null, false, resourceProvider);
            avatarContainer.setTitle(UserObject.getUserName(currentUser));
            avatarContainer.setSubtitle(LocaleController.formatUserStatus(currentAccount, currentUser));
            avatarContainer.setUserAvatar(currentUser, true);
            avatarContainer.setOccupyStatusBar(false);
            avatarContainer.setLeftPadding(dp(10));
            actionBar.addView(avatarContainer, 0, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT, 0, 0, 40, 0));
            floatingButton3.imageView.setVisibility(View.INVISIBLE);
            actionBar.setOccupyStatusBar(false);
            actionBar.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
            if (fragmentContextViewWrapper != null) {
                AndroidUtilities.removeFromParent(fragmentContextViewWrapper);
            }
            if (fragmentLocationContextViewWrapper != null) {
                AndroidUtilities.removeFromParent(fragmentLocationContextViewWrapper);
            }
        }
        */

        searchIsShowed = false;

        if (searchString != null) {
            showSearch(true, false, false);
            fragmentSearchField.editText.setText(searchString);
            fragmentSearchField.editText.setSelection(searchString.length());
        } else if (initialSearchString != null) {
            showSearch(true, false, false, true);
            fragmentSearchField.editText.setText(initialSearchString);
            fragmentSearchField.editText.setSelection(initialSearchString.length());
            initialSearchString = null;
            if (fragmentSearchField != null) {
                fragmentSearchField.setTranslationY(-dp(FILTER_TABS_HEIGHT) + getSearchFieldAdditionOffset());
            }
        } else {
            showSearch(false, false, false);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            FilesMigrationService.checkBottomSheet(this);
        }
        actionBar.setDrawBlurBackground(contentView);

        rightSlidingDialogContainer = new RightSlidingDialogContainer(context) {

            boolean anotherFragmentOpened;
            DialogsActivity.ViewPage transitionPage;

            float fromScrollYProperty;

            @Override
            boolean getOccupyStatusbar() {
                return actionBar != null && actionBar.getOccupyStatusBar();
            }

            @Override
            public void openAnimationStarted(boolean open) {
                rightFragmentTransitionInProgress = true;
                rightFragmentTransitionIsOpen = open;
                contentView.requestLayout();
                fromScrollYProperty = scrollYOffset;

                transitionPage = viewPages[0];
                if (transitionPage.animationSupportListView == null) {
                    transitionPage.animationSupportListView = new BlurredRecyclerView(context) {
                        @Override
                        protected int measureBlurTopPadding() {
                            return dp(48);
                        }

                        @Override
                        protected void dispatchDraw(Canvas canvas) {

                        }

                        @Override
                        public boolean dispatchTouchEvent(MotionEvent ev) {
                            return false;
                        }

                        @Override
                        public boolean onInterceptTouchEvent(MotionEvent e) {
                            return false;
                        }

                        @Override
                        public boolean onTouchEvent(MotionEvent e) {
                            return false;
                        }
                    };
                    ViewPage page = transitionPage;
                    LinearLayoutManager layoutManager = new LinearLayoutManager(context) {
                        @Override
                        protected int firstPosition() {
                            if (page.dialogsType == DIALOGS_TYPE_DEFAULT && hasHiddenArchive() && page.archivePullViewState == ARCHIVE_ITEM_STATE_HIDDEN) {
                                return 1;
                            }
                            return 0;
                        }
                    };
                    transitionPage.animationSupportListView.setLayoutManager(layoutManager);
                    transitionPage.animationSupportDialogsAdapter = new DialogsAdapter(DialogsActivity.this, context, transitionPage.dialogsType, folderId, onlySelect, selectedDialogs, currentAccount, requestPeerType);
                    transitionPage.animationSupportDialogsAdapter.setIsTransitionSupport();
                    transitionPage.animationSupportListView.setAdapter(transitionPage.animationSupportDialogsAdapter);
                    transitionPage.addView(transitionPage.animationSupportListView);
                }
                if (!open) {
                    invalidateScrollY = false;
                    DialogsActivity.this.setScrollY(-getMaxScrollYOffset());
                }

                transitionPage.listView.stopScroll();
                transitionPage.animationSupportDialogsAdapter.setDialogsType(transitionPage.dialogsType);
                transitionPage.dialogsAdapter.setCollapsedView(false, transitionPage.listView);
                transitionPage.dialogsAdapter.setDialogsListFrozen(true);
                transitionPage.animationSupportDialogsAdapter.setDialogsListFrozen(true);
                transitionPage.layoutManager.setNeedFixEndGap(false);
                setDialogsListFrozen(true);
                hideFloatingButton(anotherFragmentOpened);
                transitionPage.dialogsAdapter.notifyDataSetChanged();
                transitionPage.animationSupportDialogsAdapter.notifyDataSetChanged();
                float scrollOffset = !open ? scrollYOffset : -scrollYOffset;
                transitionPage.listView.setAnimationSupportView(transitionPage.animationSupportListView, scrollOffset, open, false);
                transitionPage.listView.setClipChildren(false);
                transitionPage.listView.stopScroll();
                checkUi_searchFieldHint();
                updateDialogsHint();
            }

            @Override
            public void openAnimationFinished(boolean backward) {
                transitionPage.layoutManager.setNeedFixGap(true);
                transitionPage.dialogsAdapter.setCollapsedView(hasFragment(), transitionPage.listView);
                transitionPage.dialogsAdapter.setDialogsListFrozen(false);
                transitionPage.animationSupportDialogsAdapter.setDialogsListFrozen(false);
                setDialogsListFrozen(false);
                transitionPage.listView.setClipChildren(true);
                transitionPage.listView.invalidate();
                transitionPage.dialogsAdapter.notifyDataSetChanged();
                transitionPage.animationSupportDialogsAdapter.notifyDataSetChanged();
                transitionPage.listView.setAnimationSupportView(null, 0, hasFragment(), backward);
                rightFragmentTransitionInProgress = false;
                contentView.requestLayout();
                if (!hasFragment()) {
                    invalidateScrollY = true;
                    fixScrollYAfterArchiveOpened = true;
                    if (fragmentView != null) {
                        fragmentView.invalidate();
                    }
                }
                if (searchViewPager != null) {
                    searchViewPager.updateTabs();
                }
                updateFilterTabs(false, true);
                checkUi_searchFieldHint();
                updateDialogsHint();
            }

            @Override
            void setOpenProgress(float progress) {
                boolean opened = progress > 0f;
                if (anotherFragmentOpened != opened) {
                    anotherFragmentOpened = opened;
                }
                if (fragmentView != null) {
                    fragmentView.invalidate();
                }

                if (actionBar.getTitleTextView() != null) {
                    actionBar.getTitleTextView().setAlpha(1f - progress);
                    if (actionBar.getTitleTextView().getAlpha() > 0) {
                        actionBar.getTitleTextView().setVisibility(View.VISIBLE);
                    }
                }
                if (actionBar.getBackButton() != null) {
                    actionBar.getBackButton().setAlpha(progress == 1f ? 0f : 1f);
                }

                if (folderId != 0 || communityId != 0) {
                    actionBarDefaultPaint.setColor(
                            ColorUtils.blendARGB(
                                    getThemedColor(Theme.key_windowBackgroundWhite),
                                    getThemedColor(Theme.key_windowBackgroundWhite),
                                    progress
                            )
                    );
                }

                if (transitionPage != null) {
                    transitionPage.listView.setOpenRightFragmentProgress(progress);
                }

                checkUi_menuItems();
                checkUi_topPanelVisible();
                checkUi_filterTabsVisible();
                checkUi_searchFieldVisibility();
                if (viewPages[0] != null && viewPages[0].listView != null) {
                    viewPages[0].listView.requestLayout();
                }
                if (fragmentView != null) {
                    fragmentView.invalidate();
                }
            }
        };
        updateFilterTabs(true, false);
        rightSlidingDialogContainer.setOpenProgress(0f);
        contentView.addView(dialogStoriesCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, DialogStoriesCell.HEIGHT_IN_DP));
        contentView.addView(rightSlidingDialogContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        dialogsActivityStatusLayout = new DialogsActivityStatusLayout(context);
        // contentView.addView(dialogsActivityStatusLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        if (topPanelLayout != null) {
            contentView.addView(topPanelLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 0, -14, 0, 0));
        }

        if (communityId != 0 && initialDialogsType != DIALOGS_TYPE_FORWARD) {
            if (ChatObject.canAddChatToCommunity(community)) {
                final AlertDialog[] progressDialog = new AlertDialog[1];
                final ColoredImageSpan span = new ColoredImageSpan(R.drawable.filled_add_album);
                final SpannableStringBuilder sb = new SpannableStringBuilder("+ ");
                sb.append(getString(R.string.CommunityAddAChatToCommunity));
                sb.setSpan(span, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                addChatsToCommunityButton = new ButtonWithCounterView(context, resourceProvider);
                addChatsToCommunityButton.setRound();
                addChatsToCommunityButton.setText(sb);
                addChatsToCommunityButton.setOnClickListener(v -> {
                    CommunityUtils.showChatsToAddToCommunity(progressDialog, this, currentAccount, community);
                });

                communityBottomFadeView = new ChatActivityFadeView(getContext());
                communityBottomFadeView.setupColorKey(Theme.key_windowBackgroundGray);
                communityBottomFadeView.setFadeZoneBottom(dp(72) + AndroidUtilities.navigationBarHeight);
                communityBottomFadeView.setFadeHeightBottom(dp(24));
                contentView.addView(communityBottomFadeView, LayoutHelper.createFrameMatchParent());
                contentView.addView(addChatsToCommunityButton,
                    LayoutHelper.createFrameMarginPx(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM,
                    dp(12), 0, dp(12),
                    AndroidUtilities.navigationBarHeight + dp(12)));
            }
        }


        updateStoriesVisibility(false);

        updateFloatingButtonVisibility(false);

        checkUi_searchFiltersVisibility();
        checkUi_menuItems();
        checkUi_searchFieldVisibility();
        checkUi_searchFieldHint();
        checkUi_mainTabsVisible();
        checkUi_forwardCommentFieldVisible();
        checkUi_searchFieldStyle();

        ViewCompat.setOnApplyWindowInsetsListener(fragmentView, this::onApplyWindowInsets);
        return fragmentView;
    }

    private void setStoriesOvercroll(ViewPage viewPage, float storiesOverscroll) {
        if (this.storiesOverscroll == storiesOverscroll) {
            return;
        }
        this.storiesOverscroll = storiesOverscroll;
        if (this.storiesOverscroll == 0) {
            storiesOverscrollCalled = false;
        }
        dialogStoriesCell.setOverscroll(storiesOverscroll);
        viewPage.listView.setViewsOffset(storiesOverscroll);
        viewPage.listView.setOverScrollMode(storiesOverscroll != 0 ? RecyclerView.OVER_SCROLL_NEVER : RecyclerView.OVER_SCROLL_ALWAYS);
        fragmentView.invalidate();
        if (storiesOverscroll > dp(90) && !storiesOverscrollCalled) {
            if (dialogStoriesCell.openOverscrollSelectedStory()) {
                storiesOverscrollCalled = true;
                getOrCreateStoryViewer().doOnAnimationReady(() -> {
                    fragmentView.dispatchTouchEvent(AndroidUtilities.emptyMotionEvent());
                });
            }
        }
    }

    private void toggleArciveForStory(long dialogId) {
        boolean hide = !isArchive();
        AndroidUtilities.runOnUIThread(() -> {
            getMessagesController().getStoriesController().toggleHidden(dialogId, hide, false, true);
            BulletinFactory.UndoObject undoObject = new BulletinFactory.UndoObject();
            undoObject.onUndo = () -> {
                getMessagesController().getStoriesController().toggleHidden(dialogId, !hide, false, true);
            };
            undoObject.onAction = () -> {
                getMessagesController().getStoriesController().toggleHidden(dialogId, hide, true, true);
            };
            CharSequence str;
            String name;
            TLObject object;
            if (dialogId >= 0) {
                TLRPC.User user = getMessagesController().getUser(dialogId);
                name = ContactsController.formatName(user.first_name, null, 15);
                object = user;
            } else {
                TLRPC.Chat chat = getMessagesController().getChat(-dialogId);
                name = chat.title;
                object = chat;
            }

            if (isArchive()) {
                str = AndroidUtilities.replaceTags(LocaleController.formatString("StoriesMovedToDialogs", R.string.StoriesMovedToDialogs, name));
            } else {
                str = AndroidUtilities.replaceTags(LocaleController.formatString("StoriesMovedToContacts", R.string.StoriesMovedToContacts, ContactsController.formatName(name, null, 15)));
            }
            storiesBulletin = BulletinFactory.global().createUsersBulletin(
                Collections.singletonList(object),
                str,
                null,
                undoObject
            ).show();
        }, 200);
    }

    private boolean checkAutoscrollToStories(ViewPage viewPage) {
        if (!rightSlidingDialogContainer.hasFragment()) {
            int scrollY = (int) -scrollYOffset;
            int actionBarHeight = getMaxScrollYOffset();
            int actionBarHeightNoSearch = getMaxScrollYOffsetWithoutSearch();
            if (scrollY != 0 && scrollY != actionBarHeight && scrollY != actionBarHeightNoSearch) {
                if (!viewPage.listView.canScrollVertically(-1)) {
                    return false;
                }

                if (actionBarHeightNoSearch < scrollY && scrollY < actionBarHeight) {
                    int h = dp(SEARCH_FIELD_HEIGHT);
                    int s = scrollY - actionBarHeightNoSearch;
                    if (s < h / 2) {
                        viewPage.scroller.smoothScrollBy(-s);
                        // viewPage.listView.smoothScrollBy(0, -s);
                    } else {
                        viewPage.scroller.smoothScrollBy(h - s);
                        // viewPage.listView.smoothScrollBy(0, h - s);
                    }
                    return true;
                }

                final float p = progressToActionMode == 1f ? 1 :
                    Utilities.clamp(-scrollYOffset / dp(DialogStoriesCell.HEIGHT_IN_DP), 1f, 0f);

                if (p < dialogStoriesCell.K) {
                    viewPage.scroller.smoothScrollBy(-scrollY);
                    // viewPage.listView.smoothScrollBy(0, -scrollY);
                } else {
                    viewPage.scroller.smoothScrollBy(actionBarHeightNoSearch - scrollY);
                    // viewPage.listView.smoothScrollBy(0, actionBarHeightNoSearch - scrollY);
                }
                return true;
            }
        }
        return false;
    }

    private int getMaxScrollYOffsetWithoutSearch() {
        if (hasStories) {
            return dp(DialogStoriesCell.HEIGHT_IN_DP);
        } else {
            return 0;
        }
    }

    private int getMaxScrollYOffset() {
        if (hasStories) {
            return dp(DialogStoriesCell.HEIGHT_IN_DP) + dp(SEARCH_FIELD_HEIGHT);
        } else {
            return dp(SEARCH_FIELD_HEIGHT);
        }
    }

    public boolean isStarsSubscriptionHintVisible() {
        if (folderId != 0 || communityId != 0) {
            return false;
        }

        if (MessagesController.getInstance(currentAccount).pendingSuggestions.contains("STARS_SUBSCRIPTION_LOW_BALANCE")) {
            StarsController c = StarsController.getInstance(currentAccount);
            if (!c.hasInsufficientSubscriptions()) {
                c.loadInsufficientSubscriptions();
                return false;
            } else {
                long starsNeeded = -c.balance.amount;
                for (int i = 0; i < c.insufficientSubscriptions.size(); ++i) {
                    final TL_stars.StarsSubscription sub = c.insufficientSubscriptions.get(i);
                    final long did = DialogObject.getPeerDialogId(sub.peer);
                    if (did >= 0) {
                        TLRPC.User user = getMessagesController().getUser(did);
                        if (user == null) continue;
                    } else {
                        TLRPC.Chat chat = getMessagesController().getChat(-did);
                        if (chat == null) continue;
                    }
                    starsNeeded += sub.pricing.amount;
                }
                return starsNeeded > 0;
            }
        }

        return false;
    }

    private boolean isCommunityPendingRequestsVisible() {
        return communityId != 0 && communityFull != null && communityFull.requests_pending > 0
            && !animatorSearchVisible.getValue();
    }

    private void checkCommunityPendingRequestsVisible(boolean animated) {
        if (topPanelLayout != null && communityPendingRequests != null && communityFull != null) {
            topPanelLayout.setViewVisible(communityPendingRequests, isCommunityPendingRequestsVisible(), animated);
            communityPendingRequests.setTitle(formatPluralString("CommunityPendingRequestsRow", communityFull.requests_pending));
        }
    }

    public boolean isPremiumRestoreHintVisible() {
        if (!MessagesController.getInstance(currentAccount).premiumFeaturesBlocked() && folderId == 0 && communityId == 0) {
            return MessagesController.getInstance(currentAccount).pendingSuggestions.contains("PREMIUM_RESTORE") && !getUserConfig().isPremium() && MediaDataController.getInstance(currentAccount).getPremiumHintAnnualDiscount(false) != null;
        }
        return false;
    }

    public boolean isPremiumChristmasHintVisible() {
        if (!MessagesController.getInstance(currentAccount).premiumFeaturesBlocked() && folderId == 0 && communityId == 0) {
            return MessagesController.getInstance(currentAccount).pendingSuggestions.contains("PREMIUM_CHRISTMAS");
        }
        return false;
    }

    public boolean isPremiumHintVisible() {
        if (!MessagesController.getInstance(currentAccount).premiumFeaturesBlocked() && folderId == 0 && communityId == 0) {
            if (MessagesController.getInstance(currentAccount).pendingSuggestions.contains("PREMIUM_UPGRADE") && getUserConfig().isPremium() || MessagesController.getInstance(currentAccount).pendingSuggestions.contains("PREMIUM_ANNUAL") && !getUserConfig().isPremium()) {
                if (UserConfig.getInstance(currentAccount).isPremium() ? !BuildVars.useInvoiceBilling() && MediaDataController.getInstance(currentAccount).getPremiumHintAnnualDiscount(true) != null : MediaDataController.getInstance(currentAccount).getPremiumHintAnnualDiscount(false) != null) {
                    isPremiumHintUpgrade = MessagesController.getInstance(currentAccount).pendingSuggestions.contains("PREMIUM_UPGRADE");
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isCacheHintVisible() {
        if (cacheSize == null || deviceSize == null) {
            return false;
        }
        if ((cacheSize / (float) deviceSize) < 0.30F) {
            clearCacheHintVisible();
            return false;
        }
        SharedPreferences prefs = MessagesController.getGlobalMainSettings();
        return System.currentTimeMillis() > prefs.getLong("cache_hint_showafter", 0L);
    }

    private void resetCacheHintVisible() {
        SharedPreferences prefs = MessagesController.getGlobalMainSettings();
        final long week = 1000L * 60L * 60L * 24L * 7L;
        final long month = 1000L * 60L * 60L * 24L * 30L;
        long period = prefs.getLong("cache_hint_period", week);
        if (period <= week) {
            period = month;
        }
        long showafter = System.currentTimeMillis() + period;
        prefs.edit().putLong("cache_hint_showafter", showafter).putLong("cache_hint_period", period).apply();
    }

    private void clearCacheHintVisible() {
        MessagesController.getGlobalMainSettings().edit().remove("cache_hint_showafter").remove("cache_hint_period").apply();
    }

    public void showSelectStatusDialog() {
        if (selectAnimatedEmojiDialog != null || SharedConfig.appLocked || (hasStories && !dialogStoriesCell.isExpanded())) {
            return;
        }
        final SelectAnimatedEmojiDialog.SelectAnimatedEmojiDialogWindow[] popup = new SelectAnimatedEmojiDialog.SelectAnimatedEmojiDialogWindow[1];
        TLRPC.User user = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser();
        int xoff = 0, yoff = 0;
        boolean hasEmoji = false;
        SimpleTextView actionBarTitle = actionBar.getTitleTextView();
        if (actionBarTitle != null && actionBarTitle.getRightDrawable() != null) {
            statusDrawable.play();
            hasEmoji = statusDrawable.getDrawable() instanceof AnimatedEmojiDrawable;
            AndroidUtilities.rectTmp2.set(actionBarTitle.getRightDrawable().getBounds());
            AndroidUtilities.rectTmp2.offset((int) actionBarTitle.getX(), (int) actionBarTitle.getY());
            yoff = -(actionBar.getHeight() - AndroidUtilities.rectTmp2.centerY()) - dp(16);
            xoff = AndroidUtilities.rectTmp2.centerX() - dp(16);
            xoff += dp(4);
            if (animatedStatusView != null) {
                animatedStatusView.translate(AndroidUtilities.rectTmp2.centerX(), AndroidUtilities.rectTmp2.centerY());
            }
        }
        SelectAnimatedEmojiDialog popupLayout = new SelectAnimatedEmojiDialog(this, getContext(), true, xoff, SelectAnimatedEmojiDialog.TYPE_EMOJI_STATUS, getResourceProvider()) {
            @Override
            protected boolean willApplyEmoji(View view, Long documentId, TLRPC.Document document, TL_stars.TL_starGiftUnique gift, Integer until) {
                if (gift != null) {
                    final TL_stars.SavedStarGift savedStarGift = StarsController.getInstance(currentAccount).findUserStarGift(gift.id);
                    return savedStarGift == null || MessagesController.getGlobalMainSettings().getInt("statusgiftpage", 0) >= 2;
                }
                return true;
            }

            @Override
            protected void onEmojiSelected(View emojiView, Long documentId, TLRPC.Document document, TL_stars.TL_starGiftUnique gift, Integer until) {
                final TLRPC.EmojiStatus emojiStatus;
                if (documentId == null) {
                    emojiStatus = new TLRPC.TL_emojiStatusEmpty();
                } else if (gift != null) {
                    final TL_stars.SavedStarGift savedStarGift = StarsController.getInstance(currentAccount).findUserStarGift(gift.id);
                    if (savedStarGift != null && MessagesController.getGlobalMainSettings().getInt("statusgiftpage", 0) < 2) {
                        MessagesController.getGlobalMainSettings().edit().putInt("statusgiftpage", MessagesController.getGlobalMainSettings().getInt("statusgiftpage", 0) + 1).apply();
                        new StarGiftSheet(getContext(), currentAccount, UserConfig.getInstance(currentAccount).getClientUserId(), resourceProvider)
                            .set(savedStarGift, null)
                            .setupWearPage()
                            .show();
                        if (popup[0] != null) {
                            selectAnimatedEmojiDialog = null;
                            popup[0].dismiss();
                        }
                        return;
                    }
                    final TLRPC.TL_inputEmojiStatusCollectible status = new TLRPC.TL_inputEmojiStatusCollectible();
                    status.collectible_id = gift.id;
                    if (until != null) {
                        status.flags |= 1;
                        status.until = until;
                    }
                    emojiStatus = status;
                } else {
                    final TLRPC.TL_emojiStatus status = new TLRPC.TL_emojiStatus();
                    status.document_id = documentId;
                    if (until != null) {
                        status.flags |= 1;
                        status.until = until;
                    }
                    emojiStatus = status;
                }
                getMessagesController().updateEmojiStatus(emojiStatus, gift);
                if (documentId != null) {
                    animatedStatusView.animateChange(ReactionsLayoutInBubble.VisibleReaction.fromCustomEmoji(documentId));
                }
                if (popup[0] != null) {
                    selectAnimatedEmojiDialog = null;
                    popup[0].dismiss();
                }
            }
        };
        if (user != null && DialogObject.getEmojiStatusUntil(user.emoji_status) > 0) {
            popupLayout.setExpireDateHint(DialogObject.getEmojiStatusUntil(user.emoji_status));
        }
        if (statusDrawableGiftId != null) {
            popupLayout.setSelected(statusDrawableGiftId);
        } else {
            popupLayout.setSelected(statusDrawable.getDrawable() instanceof AnimatedEmojiDrawable ? ((AnimatedEmojiDrawable) statusDrawable.getDrawable()).getDocumentId() : null);
        }
        popupLayout.setSaveState(1);
        popupLayout.setScrimDrawable(statusDrawable, actionBarTitle);
        popup[0] = selectAnimatedEmojiDialog = new SelectAnimatedEmojiDialog.SelectAnimatedEmojiDialogWindow(popupLayout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT) {
            @Override
            public void dismiss() {
                super.dismiss();
                selectAnimatedEmojiDialog = null;
            }
        };
        popup[0].showAsDropDown(actionBar, dp(16), yoff, Gravity.TOP);
        popup[0].dimBehind();
    }

    private int shiftDp = -4;
    private void showPremiumBlockedToast(View view, long dialogId) {
        AndroidUtilities.shakeViewSpring(view, shiftDp = -shiftDp);
        BotWebViewVibrationEffect.APP_ERROR.vibrate();
        String username = "";
        if (dialogId >= 0) {
            username = UserObject.getUserName(MessagesController.getInstance(currentAccount).getUser(dialogId));
        }
        Bulletin bulletin;
        if (getMessagesController().premiumFeaturesBlocked()) {
            bulletin = BulletinFactory.of(this).createSimpleBulletin(R.raw.star_premium_2, AndroidUtilities.replaceTags(LocaleController.formatString(R.string.UserBlockedNonPremium, username)));
        } else {
            bulletin = BulletinFactory.of(this)
                .createSimpleBulletin(R.raw.star_premium_2, AndroidUtilities.replaceTags(LocaleController.formatString(R.string.UserBlockedNonPremium, username)), LocaleController.getString(R.string.UserBlockedNonPremiumButton), () -> {
                    BaseFragment lastFragment = LaunchActivity.getLastFragment();
                    if (lastFragment != null) {
                        presentFragment(new PremiumPreviewFragment("noncontacts"));
                    }
                });
        }
        bulletin.show();
    }

    private void updateDialogsHint() {
        if (topPanelLayout == null || dialogsHintCell == null || fragmentView == null || getContext() == null) {
            return;
        }

        boolean dialogsHintCellVisible;

        if (dialogsHintCell != null) {
            try {
                ((RLottieDrawable) ((AvatarDrawable) dialogsHintCell.imageView.getImageReceiver().getStaticThumb()).getCustomIcon()).setMasterParent(null);
            } catch (Exception e) {}
            dialogsHintCell.clear();
        }
        if (isInPreviewMode()) {
            dialogsHintCellVisible = false;
        } else if (getMessagesController().isFrozen()) {
            dialogsHintCellVisible = true;
            dialogsHintCell.setOnClickListener(v -> {
                AccountFrozenAlert.show(getContext(), currentAccount, getResourceProvider());
            });
            dialogsHintCell.setText(
                getString(R.string.AccountFrozenAlertTitle),
                getString(R.string.AccountFrozenAlertSubtitle),
                false,
                true
            );
        } else if (folderId == 0 && communityId == 0 && getMessagesController().pendingSuggestions.contains("SETUP_PASSKEY")) {
            dialogsHintCellVisible = true;
            dialogsHintCell.setOnClickListener(v -> {
                PasskeysActivity.showLearnSheet(getContext(), currentAccount, resourceProvider, true);
            });
            dialogsHintCell.setText(Emoji.replaceWithRestrictedEmoji(getString(R.string.PasskeyPopupTitle), dialogsHintCell.titleView, this::updateDialogsHint), getString(R.string.PasskeyPopupText));
            dialogsHintCell.setOnCloseListener(v -> {
                MessagesController.getInstance(currentAccount).removeSuggestion(0, "SETUP_PASSKEY");
                updateDialogsHint();
            });
        } else if (folderId == 0 && communityId == 0 && getMessagesController().pendingSuggestions.contains("PREMIUM_GRACE")) {
            dialogsHintCellVisible = true;
            dialogsHintCell.setOnClickListener(v -> {
                Browser.openUrl(getContext(), getMessagesController().premiumManageSubscriptionUrl);
            });
            dialogsHintCell.setText(Emoji.replaceWithRestrictedEmoji(LocaleController.getString(R.string.GraceTitle), dialogsHintCell.titleView, this::updateDialogsHint), LocaleController.getString(R.string.GraceMessage));
            dialogsHintCell.setOnCloseListener(v -> {
                MessagesController.getInstance(currentAccount).removeSuggestion(0, "PREMIUM_GRACE");
                updateDialogsHint();
            });
        } else if (folderId == 0 && communityId == 0 && getMessagesController().customPendingSuggestion != null) {
            final TLRPC.TL_pendingSuggestion suggestion = getMessagesController().customPendingSuggestion;
            dialogsHintCellVisible = true;

            CharSequence title = new SpannableStringBuilder(suggestion.title.text);
            MessageObject.addEntitiesToText(title, suggestion.title.entities, false, false, true, true);
            title = Emoji.replaceEmoji(title, dialogsHintCell.titleView.getPaint().getFontMetricsInt(), false, null);
            title = MessageObject.replaceAnimatedEmoji(title, suggestion.title.entities, dialogsHintCell.titleView.getPaint().getFontMetricsInt());

            CharSequence subtitle = new SpannableStringBuilder(suggestion.description.text);
            MessageObject.addEntitiesToText(subtitle, suggestion.description.entities, false, false, true, true);
            subtitle = Emoji.replaceEmoji(subtitle, dialogsHintCell.messageView.getPaint().getFontMetricsInt(), false, null);
            subtitle = MessageObject.replaceAnimatedEmoji(subtitle, suggestion.description.entities, dialogsHintCell.messageView.getPaint().getFontMetricsInt());

            dialogsHintCell.setText(title, subtitle);
            dialogsHintCell.setOnClickListener(v -> {
                Browser.openUrl(getContext(), suggestion.url);
            });
            dialogsHintCell.setOnCloseListener(v -> {
                MessagesController.getInstance(currentAccount).removeSuggestion(0, suggestion.suggestion);
                updateDialogsHint();
            });
        } else if (isStarsSubscriptionHintVisible()) {
            StarsController c = StarsController.getInstance(currentAccount);
            dialogsHintCellVisible = true;
            StringBuilder s = new StringBuilder();
            long starsNeeded = 0;
            long _firstDialogId = 0;
            if (c.hasInsufficientSubscriptions()) {
                for (int i = 0; i < c.insufficientSubscriptions.size(); ++i) {
                    final TL_stars.StarsSubscription sub = c.insufficientSubscriptions.get(i);
                    final long did = DialogObject.getPeerDialogId(sub.peer);
                    if (_firstDialogId == 0) _firstDialogId = did;
                    if (did >= 0) {
                        TLRPC.User user = getMessagesController().getUser(did);
                        if (user == null) continue;
                        if (s.length() > 0) s.append(", ");
                        s.append(UserObject.getUserName(user));
                    } else {
                        TLRPC.Chat chat = getMessagesController().getChat(-did);
                        if (chat == null) continue;
                        if (s.length() > 0) s.append(", ");
                        s.append(chat.title);
                    }
                    starsNeeded += sub.pricing.amount;
                }
            }
            final String starsNeededName = s.toString();
            final long starsNeededFinal = starsNeeded;
            final long firstDialogId = _firstDialogId;
            dialogsHintCell.setOnClickListener(v -> {
                new StarsIntroActivity.StarsNeededSheet(getContext(), getResourceProvider(), starsNeededFinal, StarsIntroActivity.StarsNeededSheet.TYPE_SUBSCRIPTION_KEEP, starsNeededName, () -> {
                    updateDialogsHint();
                }, firstDialogId).show();
            });
            dialogsHintCell.setText(StarsIntroActivity.replaceStarsWithPlain(formatPluralStringComma("StarsSubscriptionExpiredHintTitle2", (int) (starsNeeded - c.balance.amount <= 0 ? starsNeeded : starsNeeded - c.balance.amount), starsNeededName), .72f), LocaleController.getString(R.string.StarsSubscriptionExpiredHintText));
            dialogsHintCell.setOnCloseListener(v -> {
                MessagesController.getInstance(currentAccount).removeSuggestion(0, "STARS_SUBSCRIPTION_LOW_BALANCE");
                updateDialogsHint();
            });
        } else if (folderId == 0 && communityId == 0 && !getMessagesController().premiumPurchaseBlocked() && BirthdayController.getInstance(currentAccount).contains() && !getMessagesController().dismissedSuggestions.contains("BIRTHDAY_CONTACTS_TODAY")) {
            BirthdayController.BirthdayState state = BirthdayController.getInstance(currentAccount).getState();
            ArrayList<TLRPC.User> users = state.today;
            dialogsHintCellVisible = true;
            dialogsHintCell.setOnClickListener(v -> {
                if (state != null && state.today.size() == 1) {
                    showDialog(new GiftSheet(getContext(), currentAccount, state.today.get(0).id, null, null).setBirthday());
                    return;
                }
                UserSelectorBottomSheet.open(0, state);
            });
            dialogsHintCell.setAvatars(currentAccount, users);
            dialogsHintCell.setText(Emoji.replaceWithRestrictedEmoji(AndroidUtilities.replaceSingleTag(
                users.size() == 1 ?
                    LocaleController.formatString(R.string.BirthdayTodaySingleTitle, UserObject.getForcedFirstName(users.get(0))) :
                    LocaleController.formatPluralString("BirthdayTodayMultipleTitle", users.size()),
                Theme.key_windowBackgroundWhiteValueText,
                AndroidUtilities.REPLACING_TAG_TYPE_LINKBOLD,
                null
            ), dialogsHintCell.titleView, this::updateDialogsHint),
                LocaleController.formatString(users.size() == 1 ? R.string.BirthdayTodaySingleMessage2 : R.string.BirthdayTodayMultipleMessage2)
            );
            dialogsHintCell.setOnCloseListener(v -> {
                BirthdayController.getInstance(currentAccount).hide();
                MessagesController.getInstance(currentAccount).removeSuggestion(0, "BIRTHDAY_CONTACTS_TODAY");
                updateDialogsHint();
                BulletinFactory.of(this)
                        .createSimpleBulletin(R.raw.gift, LocaleController.getString(R.string.BoostingPremiumChristmasToast), 4)
                        .setDuration(Bulletin.DURATION_PROLONG)
                        .show();
            });
            StarsController.getInstance(currentAccount).loadStarGifts();
        } else if (
            folderId == 0 && communityId == 0 &&
            MessagesController.getInstance(currentAccount).pendingSuggestions.contains("BIRTHDAY_SETUP") &&
            getMessagesController().getUserFull(getUserConfig().getClientUserId()) != null &&
            getMessagesController().getUserFull(getUserConfig().getClientUserId()).birthday == null
        ) {
            ContactsController.getInstance(currentAccount).loadPrivacySettings();
            dialogsHintCellVisible = true;
            dialogsHintCell.setOnClickListener(v -> {
                showDialog(AlertsCreator.createBirthdayPickerDialog(getContext(), getString(R.string.EditProfileBirthdayTitle), getString(R.string.EditProfileBirthdayButton), null, birthday -> {
                    TL_account.updateBirthday req = new TL_account.updateBirthday();
                    req.flags |= 1;
                    req.birthday = birthday;
                    TLRPC.UserFull userFull = getMessagesController().getUserFull(getUserConfig().getClientUserId());
                    TL_account.TL_birthday oldBirthday = userFull != null ? userFull.birthday : null;
                    if (userFull != null) {
                        userFull.flags2 |= 32;
                        userFull.birthday = birthday;
                    }
                    getMessagesController().invalidateContentSettings();
                    getConnectionsManager().sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                        if (res instanceof TLRPC.TL_boolTrue) {
                            BulletinFactory.of(DialogsActivity.this)
                                .createSimpleBulletin(R.raw.gift, getString(R.string.PrivacyBirthdaySetDone), getString(R.string.PrivacyBirthdaySetDoneInfo))
                                .setDuration(Bulletin.DURATION_PROLONG).show();
                        } else {
                            if (userFull != null) {
                                if (oldBirthday == null) {
                                    userFull.flags2 &=~ 32;
                                } else {
                                    userFull.flags2 |= 32;
                                }
                                userFull.birthday = oldBirthday;
                                getMessagesStorage().updateUserInfo(userFull, false);
                            }
                            if (err != null && err.text != null && err.text.startsWith("FLOOD_WAIT_")) {
                                if (getContext() != null) {
                                    showDialog(
                                        new AlertDialog.Builder(getContext(), resourceProvider)
                                            .setTitle(getString(R.string.PrivacyBirthdayTooOftenTitle))
                                            .setMessage(getString(R.string.PrivacyBirthdayTooOftenMessage))
                                            .setPositiveButton(getString(R.string.OK), null)
                                            .create()
                                    );
                                }
                            } else {
                                BulletinFactory.of(DialogsActivity.this)
                                    .createSimpleBulletin(R.raw.error, LocaleController.getString(R.string.UnknownError))
                                    .show();
                            }
                        }
                    }), ConnectionsManager.RequestFlagDoNotWaitFloodWait);

                    MessagesController.getInstance(currentAccount).removeSuggestion(0, "BIRTHDAY_SETUP");

                    updateDialogsHint();
                }, () -> {
                    BaseFragment.BottomSheetParams params = new BaseFragment.BottomSheetParams();
                    params.transitionFromLeft = true;
                    params.allowNestedScroll = false;
                    showAsSheet(new PrivacyControlActivity(PrivacyControlActivity.PRIVACY_RULES_TYPE_BIRTHDAY), params);
                }, false, false, getResourceProvider()).create());
            });
            dialogsHintCell.setText(Emoji.replaceWithRestrictedEmoji(LocaleController.getString(R.string.BirthdaySetupTitle), dialogsHintCell.titleView, this::updateDialogsHint), LocaleController.formatString(R.string.BirthdaySetupMessage));
            dialogsHintCell.setOnCloseListener(v -> {
                MessagesController.getInstance(currentAccount).removeSuggestion(0, "BIRTHDAY_SETUP");
                updateDialogsHint();

                BulletinFactory.of(this)
                    .createSimpleBulletin(R.raw.chats_infotip, LocaleController.getString(R.string.BirthdaySetupLater), LocaleController.getString(R.string.Settings), () -> {
                        presentFragment(new UserInfoActivity());
                    })
                    .setDuration(Bulletin.DURATION_PROLONG)
                    .show();
            });
        } else if (isPremiumChristmasHintVisible()) {
            dialogsHintCellVisible = true;
            dialogsHintCell.setOnClickListener(v -> UserSelectorBottomSheet.open());
            dialogsHintCell.setText(Emoji.replaceEmoji(AndroidUtilities.replaceSingleTag(
                    LocaleController.getString(R.string.GiftPremiumEventAdsTitle),
                    Theme.key_windowBackgroundWhiteValueText,
                    AndroidUtilities.REPLACING_TAG_TYPE_LINKBOLD,
                    null
            ), null, false), LocaleController.formatString(R.string.BoostingPremiumChristmasSubTitle));
            dialogsHintCell.setOnCloseListener(v -> {
                MessagesController.getInstance(currentAccount).removeSuggestion(0, "PREMIUM_CHRISTMAS");
                updateDialogsHint();
                BulletinFactory.of(this)
                        .createSimpleBulletin(R.raw.gift, LocaleController.getString(R.string.BoostingPremiumChristmasToast), 4)
                        .setDuration(Bulletin.DURATION_PROLONG)
                        .show();
            });
        } else if (isPremiumRestoreHintVisible()) {
            dialogsHintCellVisible = true;
            dialogsHintCell.setOnClickListener(v -> {
                presentFragment(new PremiumPreviewFragment("dialogs_hint").setSelectAnnualByDefault());
                AndroidUtilities.runOnUIThread(() -> {
                    MessagesController.getInstance(currentAccount).removeSuggestion(0, "PREMIUM_RESTORE");
                    updateDialogsHint();
                }, 250);
            });
            dialogsHintCell.setText(
                    AndroidUtilities.replaceSingleTag(
                            LocaleController.formatString(R.string.RestorePremiumHintTitle, MediaDataController.getInstance(currentAccount).getPremiumHintAnnualDiscount(false)),
                            Theme.key_windowBackgroundWhiteValueText,
                            AndroidUtilities.REPLACING_TAG_TYPE_LINKBOLD,
                            null
                    ),
                    LocaleController.getString(R.string.RestorePremiumHintMessage)
            );
        } else if (isPremiumHintVisible()) {
            dialogsHintCellVisible = true;
            dialogsHintCell.setOnClickListener(v -> {
                presentFragment(new PremiumPreviewFragment("dialogs_hint").setSelectAnnualByDefault());
                AndroidUtilities.runOnUIThread(() -> {
                    MessagesController.getInstance(currentAccount).removeSuggestion(0, isPremiumHintUpgrade ? "PREMIUM_UPGRADE" : "PREMIUM_ANNUAL");
                    updateDialogsHint();
                }, 250);
            });
            dialogsHintCell.setText(
                    AndroidUtilities.replaceSingleTag(
                            LocaleController.formatString(isPremiumHintUpgrade ? R.string.SaveOnAnnualPremiumTitle : R.string.UpgradePremiumTitle, MediaDataController.getInstance(currentAccount).getPremiumHintAnnualDiscount(false)),
                            Theme.key_windowBackgroundWhiteValueText,
                            AndroidUtilities.REPLACING_TAG_TYPE_LINKBOLD,
                            null
                    ),
                    LocaleController.getString(isPremiumHintUpgrade ? R.string.UpgradePremiumMessage : R.string.SaveOnAnnualPremiumMessage)
            );
        } else if (isCacheHintVisible()) {
            dialogsHintCellVisible = true;
            dialogsHintCell.setOnClickListener(v -> {
                presentFragment(new CacheControlActivity());
                AndroidUtilities.runOnUIThread(() -> {
                    resetCacheHintVisible();
                    updateDialogsHint();
                }, 250);
            });
            dialogsHintCell.setText(
                    AndroidUtilities.replaceSingleTag(
                            LocaleController.formatString(R.string.ClearStorageHintTitle, AndroidUtilities.formatFileSize(cacheSize)),
                            Theme.key_windowBackgroundWhiteValueText,
                            AndroidUtilities.REPLACING_TAG_TYPE_LINKBOLD,
                            null
                    ),
                    LocaleController.getString(R.string.ClearStorageHintMessage)
            );
        } else if (folderId == 0 && communityId == 0 && getUserConfig().getCurrentUser() != null && (getUserConfig().getCurrentUser().photo == null || getUserConfig().getCurrentUser().photo instanceof TLRPC.TL_userProfilePhotoEmpty) && MessagesController.getInstance(currentAccount).pendingSuggestions.contains("USERPIC_SETUP")) {
            dialogsHintCellVisible = true;
            dialogsHintCell.setOnClickListener(v -> {
                openSetAvatar();
            });
            dialogsHintCell.showImage();
            final AvatarDrawable avatarDrawable = new AvatarDrawable();
            avatarDrawable.setBounds(0, 0, dp(36), dp(36));
            avatarDrawable.setInfo(getUserConfig().getClientUserId());
            avatarDrawable.setCustomIcon(getContext().getResources().getDrawable(R.drawable.filled_profile_photo_20));
            dialogsHintCell.imageView.setImageDrawable(avatarDrawable);
            dialogsHintCell.setText(
                Emoji.replaceWithRestrictedEmoji(LocaleController.getString(R.string.HintAddYourPhoto), dialogsHintCell.titleView, this::updateDialogsHint),
                getString(R.string.HintAddYourPhotoText)
            );
            dialogsHintCell.setOnCloseListener(v -> {
                MessagesController.getInstance(currentAccount).removeSuggestion(0, "USERPIC_SETUP");
                updateDialogsHint();
            });
        } else if (folderId == 0 && communityId == 0 && ApplicationLoader.applicationLoaderInstance != null) {
            boolean found = false;
            String foundSuggestion = null;
            CharSequence[] output = new CharSequence[2];
            boolean[] closeable = new boolean[1];
            if (ApplicationLoader.applicationLoaderInstance.onSuggestionFill(null, output, closeable)) {
                found = true;
                foundSuggestion = null;
            } else {
                for (String suggestion : MessagesController.getInstance(currentAccount).pendingSuggestions) {
                    if (ApplicationLoader.applicationLoaderInstance.onSuggestionFill(suggestion, output, closeable)) {
                        found = true;
                        foundSuggestion = suggestion;
                        break;
                    }
                }
            }
            if (found) {
                final String finalSuggestion = foundSuggestion;
                dialogsHintCellVisible = true;
                dialogsHintCell.setOnClickListener(v -> {
                    if (ApplicationLoader.applicationLoaderInstance != null) {
                        ApplicationLoader.applicationLoaderInstance.onSuggestionClick(finalSuggestion);
                    }
                });
                dialogsHintCell.setText(
                    output[0] instanceof String ? AndroidUtilities.replaceSingleTag(
                        output[0].toString(),
                        Theme.key_windowBackgroundWhiteValueText,
                        AndroidUtilities.REPLACING_TAG_TYPE_LINKBOLD,
                        null
                    ) : output[0],
                    output[1] instanceof String ? AndroidUtilities.replaceTags(output[1].toString()) : output[1]
                );
                if (closeable[0] && finalSuggestion != null) {
                    dialogsHintCell.setOnCloseListener(v -> {
                        AndroidUtilities.runOnUIThread(() -> {
                            MessagesController.getInstance(currentAccount).removeSuggestion(0, finalSuggestion);
                            updateDialogsHint();
                        }, 250);
                    });
                }
            } else {
                dialogsHintCellVisible = false;
            }
        } else {
            dialogsHintCellVisible = false;
        }

        if (rightSlidingDialogContainer != null && rightSlidingDialogContainer.hasFragment() || animatorSearchVisible.getValue()) {
            dialogsHintCellVisible = false;
        }

        topPanelLayout.setViewVisible(dialogsHintCell, dialogsHintCellVisible);

        checkCommunityPendingRequestsVisible(true);
        checkUnconfirmedAuthHintCellVisibility();
        checkActiveGiftAuctionsHintCellVisibility();
    }

    private void checkUnconfirmedAuthHintCellVisibility() {
        if (fragmentView == null || topPanelLayout == null) {
            return;
        }

        final boolean isVisible = !isInPreviewMode()
            && folderId == 0 && communityId == 0 && initialDialogsType == DIALOGS_TYPE_DEFAULT
            && !getMessagesController().getUnconfirmedAuthController().auths.isEmpty()
            && (rightSlidingDialogContainer == null || !rightSlidingDialogContainer.hasFragment())
            && !animatorSearchVisible.getValue();

        if (isVisible) {
            if (authHintCell == null) {
                authHintCell = new UnconfirmedAuthHintCell(getContext());
                topPanelLayout.addView(authHintCell);
            }
            authHintCell.set(DialogsActivity.this, currentAccount);
        }
        if (authHintCell != null) {
            topPanelLayout.setViewVisible(authHintCell, isVisible);
        }
    }

    private void checkActiveGiftAuctionsHintCellVisibility() {
        if (fragmentView == null || topPanelLayout == null) {
            return;
        }

        final boolean isVisible = !isInPreviewMode()
            && folderId == 0 && communityId == 0 && initialDialogsType == DIALOGS_TYPE_DEFAULT
            && getGiftAuctionsController().hasActiveAuctions()
            && (rightSlidingDialogContainer == null || !rightSlidingDialogContainer.hasFragment())
            && !animatorSearchVisible.getValue();

        if (isVisible && activeGiftAuctionsHintCell == null) {
            activeGiftAuctionsHintCell = new ActiveGiftAuctionsHintCell(getContext(), currentAccount);
            topPanelLayout.addView(activeGiftAuctionsHintCell);
        }
        if (activeGiftAuctionsHintCell != null) {
            topPanelLayout.setViewVisible(activeGiftAuctionsHintCell, isVisible);
        }
    }

    private void createGroupForThis() {
        AlertDialog progress = new AlertDialog(getContext(), AlertDialog.ALERT_TYPE_SPINNER);
        if (requestPeerType instanceof TLRPC.TL_requestPeerTypeBroadcast) {
            Bundle args = new Bundle();
            args.putInt("step", 0);
            if (requestPeerType.has_username != null) {
                args.putBoolean("forcePublic", requestPeerType.has_username);
            }
            ChannelCreateActivity fragment = new ChannelCreateActivity(args);
            fragment.setOnFinishListener((fragment2, chatId) -> {
                Utilities.doCallbacks(
                        next -> {
                            TLRPC.Chat chat = getMessagesController().getChat(chatId);
                            showSendToBotAlert(chat, next, () -> {
                                DialogsActivity.this.removeSelfFromStack();
                                fragment.removeSelfFromStack();
                                fragment2.finishFragment();
                            });
                        },
                        next -> {
                            progress.showDelayed(150);
                            if (requestPeerType.bot_participant != null && requestPeerType.bot_participant) {
                                TLRPC.User bot = getMessagesController().getUser(requestPeerBotId);
                                getMessagesController().addUserToChat(chatId, bot, 0, null, DialogsActivity.this, false, next, err -> {
                                    next.run();
                                    return true;
                                });
                            } else {
                                next.run();
                            }
                        },
                        next -> {
                            if (requestPeerType.bot_admin_rights != null) {
                                TLRPC.User bot = getMessagesController().getUser(requestPeerBotId);
                                getMessagesController().setUserAdminRole(chatId, bot, requestPeerType.bot_admin_rights, null, false, DialogsActivity.this, !(requestPeerType.bot_participant != null && requestPeerType.bot_participant), true, null, next, err -> {
                                    next.run();
                                    return true;
                                });
                            } else {
                                next.run();
                            }
                        },
                        next -> {
                            if (requestPeerType.user_admin_rights != null) {
                                TLRPC.Chat chat = getMessagesController().getChat(chatId);
                                getMessagesController().setUserAdminRole(chatId, getAccountInstance().getUserConfig().getCurrentUser(), ChatRightsEditActivity.rightsOR(chat.admin_rights, requestPeerType.user_admin_rights), null, true, DialogsActivity.this, false, true, null, next, err -> {
                                    next.run();
                                    return true;
                                });
                            } else {
                                next.run();
                            }
                        },
                        next -> {
                            progress.dismiss();
                            getMessagesController().loadChannelParticipants(chatId);
                            DialogsActivityDelegate delegate = DialogsActivity.this.delegate;
                            DialogsActivity.this.removeSelfFromStack();
                            fragment.removeSelfFromStack();
                            fragment2.finishFragment();
                            if (delegate != null) {
                                ArrayList<MessagesStorage.TopicKey> keys = new ArrayList<>();
                                keys.add(MessagesStorage.TopicKey.of(-chatId, 0));
                                delegate.didSelectDialogs(DialogsActivity.this, keys, null, false, notify, scheduleDate, scheduleRepeatPeriod, null);
                            }
                        }
                );
            });
            presentFragment(fragment);
        } else if (requestPeerType instanceof TLRPC.TL_requestPeerTypeChat) {
            Bundle args = new Bundle();
            long[] array;
            if (requestPeerType.bot_participant != null && requestPeerType.bot_participant) {
                array = new long[]{getUserConfig().getClientUserId(), requestPeerBotId};
            } else {
                array = new long[]{getUserConfig().getClientUserId()};
            }
            args.putLongArray("result", array);
            args.putInt("chatType", requestPeerType.forum != null && requestPeerType.forum ? ChatObject.CHAT_TYPE_FORUM : ChatObject.CHAT_TYPE_MEGAGROUP);
            args.putBoolean("canToggleTopics", false);
            GroupCreateFinalActivity activity = new GroupCreateFinalActivity(args);
            activity.setDelegate(new GroupCreateFinalActivity.GroupCreateFinalActivityDelegate() {
                @Override
                public void didStartChatCreation() {
                }

                @Override
                public void didFailChatCreation() {
                }

                @Override
                public void didFinishChatCreation(GroupCreateFinalActivity fragment, long chatId) {
                    BaseFragment[] lastFragments = new BaseFragment[]{fragment, null};
                    Utilities.doCallbacks(
                            next -> {
                                if (requestPeerType.has_username != null && requestPeerType.has_username) {
                                    Bundle args = new Bundle();
                                    args.putInt("step", 1);
                                    args.putLong("chat_id", chatId);
                                    args.putBoolean("forcePublic", requestPeerType.has_username);
                                    ChannelCreateActivity fragment2 = new ChannelCreateActivity(args);
                                    fragment2.setOnFinishListener((_fragment, _chatId) -> next.run());
                                    presentFragment(fragment2);
                                    lastFragments[1] = fragment2;
                                } else {
                                    next.run();
                                }
                            },
                            next -> {
                                TLRPC.Chat chat = getMessagesController().getChat(chatId);
                                showSendToBotAlert(chat, next, () -> {
                                    DialogsActivity.this.removeSelfFromStack();
                                    if (lastFragments[1] != null) {
                                        lastFragments[0].removeSelfFromStack();
                                        lastFragments[1].finishFragment();
                                    } else {
                                        lastFragments[0].finishFragment();
                                    }
                                });
                            },
                            next -> {
                                progress.showDelayed(150);
                                if (requestPeerType.bot_participant != null && requestPeerType.bot_participant) {
                                    TLRPC.User bot = getMessagesController().getUser(requestPeerBotId);
                                    getMessagesController().addUserToChat(chatId, bot, 0, null, DialogsActivity.this, false, next, err -> {
                                        next.run();
                                        return true;
                                    });
                                } else {
                                    next.run();
                                }
                            },
                            next -> {
                                if (requestPeerType.bot_admin_rights != null) {
                                    TLRPC.User bot = getMessagesController().getUser(requestPeerBotId);
                                    getMessagesController().setUserAdminRole(chatId, bot, requestPeerType.bot_admin_rights, null, false, DialogsActivity.this, !(requestPeerType.bot_participant != null && requestPeerType.bot_participant), true, null, next, err -> {
                                        next.run();
                                        return true;
                                    });
                                } else {
                                    next.run();
                                }
                            },
                            next -> {
                                if (requestPeerType.user_admin_rights != null) {
                                    TLRPC.Chat chat = getMessagesController().getChat(chatId);
                                    getMessagesController().setUserAdminRole(chatId, getAccountInstance().getUserConfig().getCurrentUser(), ChatRightsEditActivity.rightsOR(chat.admin_rights, requestPeerType.user_admin_rights), null, false, DialogsActivity.this, false, true, null, next, err -> {
                                        next.run();
                                        return true;
                                    });
                                } else {
                                    next.run();
                                }
                            },
                            next -> {
                                progress.dismiss();
                                getMessagesController().loadChannelParticipants(chatId);
                                DialogsActivityDelegate delegate = DialogsActivity.this.delegate;
                                DialogsActivity.this.removeSelfFromStack();
                                if (lastFragments[1] != null) {
                                    lastFragments[0].removeSelfFromStack();
                                    lastFragments[1].finishFragment();
                                } else {
                                    lastFragments[0].finishFragment();
                                }
                                if (delegate != null) {
                                    ArrayList<MessagesStorage.TopicKey> keys = new ArrayList<>();
                                    keys.add(MessagesStorage.TopicKey.of(-chatId, 0));
                                    delegate.didSelectDialogs(DialogsActivity.this, keys, null, false, notify, scheduleDate, scheduleRepeatPeriod, null);
                                }
                            }
                    );
                }
            });
            presentFragment(activity);
        }
    }

    private void updateContextViewPosition() {
        float searchTabsHeight = 0;
        if (searchTabsAndFiltersLayout != null && searchTabsAndFiltersLayout.getVisibility() != View.GONE) {
            searchTabsHeight = searchTabsAndFiltersLayout.getMeasuredHeight();
        }
        float storiesHeight = 0;
        if (hasStories) {
            storiesHeight = dp(DialogStoriesCell.HEIGHT_IN_DP);
        }
        float totalOffset;
        if (hasStories) {
            totalOffset = scrollYOffset /* * (1f - searchAnimationProgress) */ +
                    storiesHeight * (1f - searchAnimationProgress) +
                    searchTabsHeight * searchAnimationProgress + tabsYOffset;
        } else {
            totalOffset = scrollYOffset +
                    searchTabsHeight * searchAnimationProgress + tabsYOffset;
        }
        totalOffset += storiesOverscroll;

        float searchVisibility = 0;
        if (fragmentSearchField != null && fragmentSearchField.getVisibility() == View.VISIBLE) {
            searchVisibility = fragmentSearchField.getAlpha();
        }
        float searchOffset = dp(4) * searchVisibility;


        float filtersTabHeight = 0;
        float filtersTabVisibility = 0;

        float topPanelsHeight = 0;
        float topPanelsVisibility = 0;
        float fadeViewT = totalOffset;

        if (filterTabsView != null) {
            filterTabsView.setTranslationY(totalOffset - searchOffset);
            filtersTabVisibility = filterTabsView.getAlpha();
            filtersTabHeight = dp(36 + 7) * filtersTabVisibility;
            totalOffset += filtersTabHeight;
        }

        if (topPanelLayout != null) {
            topPanelLayout.setTranslationY(lerp(
                totalOffset - searchOffset,
                -dp(3) - (searchTabsView == null ? dp(44) : 0),
                animatorSearchVisible.getFloatValue()));
            topPanelsVisibility = topPanelLayout.getMetadata().getTotalVisibility();
            topPanelsHeight = topPanelLayout.getAnimatedHeightWithPadding(0);
        }

        if (topBubblesFadeView != null) {
            topBubblesFadeView.setTranslationY(fadeViewT - searchOffset);
            final float s = lerp(dp(7), dp(50), Math.min(topPanelsVisibility, filtersTabVisibility));
            topBubblesFadeView.setPosition(s, Math.min(dp(40), topPanelsHeight + filtersTabHeight - s));
            topBubblesFadeView.setAlpha(Math.max(filtersTabVisibility, topPanelsVisibility));
        }
    }

    private void updateFiltersView(boolean showMediaFilters, ArrayList<Object> users, ArrayList<FiltersView.DateData> dates, boolean archive, boolean animated) {
        if (!searchIsShowed || onlySelect || searchViewPager == null) {
            return;
        }
        boolean hasMediaFilter = false;
        boolean hasUserFilter = false;
        boolean hasDateFilter = false;
        boolean hasArchiveFilter = false;

        ArrayList<FiltersView.MediaFilterData> currentSearchFilters = searchViewPager.getCurrentSearchFilters();
        for (int i = 0; i < currentSearchFilters.size(); i++) {
            if (currentSearchFilters.get(i).isMedia()) {
                hasMediaFilter = true;
            } else if (currentSearchFilters.get(i).filterType == FiltersView.FILTER_TYPE_CHAT) {
                hasUserFilter = true;
            } else if (currentSearchFilters.get(i).filterType == FiltersView.FILTER_TYPE_DATE) {
                hasDateFilter = true;
            } else if (currentSearchFilters.get(i).filterType == FiltersView.FILTER_TYPE_ARCHIVE) {
                hasArchiveFilter = true;
            }
        }

        if (hasArchiveFilter) {
            archive = false;
        }

        boolean visible = false;
        boolean hasUsersOrDates = (users != null && !users.isEmpty()) || (dates != null && !dates.isEmpty() || archive);
        if (!hasMediaFilter && !hasUsersOrDates && showMediaFilters) {

        } else if (hasUsersOrDates) {
            ArrayList<Object> finalUsers = (users != null && !users.isEmpty() && !hasUserFilter) ? users : null;
            ArrayList<FiltersView.DateData> finalDates = (dates != null && !dates.isEmpty() && !hasDateFilter) ? dates : null;
            if (finalUsers != null || finalDates != null || archive) {
                visible = true;
                filtersView.setUsersAndDates(finalUsers, finalDates, archive);
            }
        }

        if (!visible) {
            filtersView.setUsersAndDates(null, null, false);
        }
        if (!animated) {
            filtersView.getAdapter().notifyDataSetChanged();
        }
        if (searchTabsView != null) {
            searchTabsView.hide(visible, true);
        }
        filtersView.setEnabled(visible);

        animatorSearchFilterTabsVisible.setValue(visible, true);
    }

    private void addSearchFilter(FiltersView.MediaFilterData filter) {
        if (!searchIsShowed || searchViewPager == null) {
            return;
        }

        if (!searchViewPager.addSearchFilter(filter)) {
            return;
        }

        fragmentSearchField.addSearchFilter(filter);
        fragmentSearchField.editText.getText().clear();

        // actionBar.setSearchFilter(filter);
        // actionBar.setSearchFieldText("");
        updateFiltersView(true, null, null, false, true);
    }

    public void updateSpeedItem(boolean visibleByPosition) {
        if (speedItem == null) {
            return;
        }

        boolean visibleByDownload = false;
        for (MessageObject obj : getDownloadController().downloadingFiles) {
            if (obj.getDocument() != null && obj.getDocument().size >= 150 * 1024 * 1024) {
                visibleByDownload = true;
                break;
            }
        }
        for (MessageObject obj : getDownloadController().recentDownloadingFiles) {
            if (obj.getDocument() != null && obj.getDocument().size >= 150 * 1024 * 1024) {
                visibleByDownload = true;
                break;
            }
        }
        boolean visible = !getUserConfig().isPremium() && !getMessagesController().premiumFeaturesBlocked() && visibleByDownload && visibleByPosition;
        animatorSpeedButtonVisible.setValue(visible, true);
    }

    private void createActionMode(String tag) {
        if (actionBar.actionModeIsExist(tag)) {
            return;
        }
        final ActionBarMenu actionMode = actionBar.createActionMode(false, tag);
        // actionMode.setBackgroundColor(Color.TRANSPARENT);
        // actionMode.drawBlur = false;

        if (hasMainTabs) {
            actionModeCloseView = new ImageView(getContext());
            actionModeCloseView.setScaleType(ImageView.ScaleType.CENTER);
            actionModeCloseView.setImageDrawable(new BackDrawable(true));
            actionModeCloseView.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_actionBarActionModeDefaultIcon), PorterDuff.Mode.MULTIPLY));
            actionModeCloseView.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_actionBarActionModeDefaultSelector)));
            actionModeCloseView.setOnClickListener(v -> hideActionMode(true));
            actionMode.addView(actionModeCloseView, LayoutHelper.createLinear(54, 54, Gravity.CENTER_VERTICAL));
            actionModeViews.add(actionModeCloseView);
        }

        selectedDialogsCountTextView = new NumberTextView(actionMode.getContext());
        selectedDialogsCountTextView.setTextSize(18);
        selectedDialogsCountTextView.setTypeface(AndroidUtilities.bold());
        selectedDialogsCountTextView.setTextColor(getThemedColor(Theme.key_actionBarActionModeDefaultIcon));
        actionMode.addView(selectedDialogsCountTextView, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, hasMainTabs ? 18 : 72, 0, 0, 0));
        selectedDialogsCountTextView.setOnTouchListener((v, event) -> true);

        pinItem = actionMode.addItemWithWidth(pin, R.drawable.msg_pin, dp(48));
        muteItem = actionMode.addItemWithWidth(mute, R.drawable.msg_mute, dp(48));
        archive2Item = actionMode.addItemWithWidth(archive2, R.drawable.msg_archive, dp(48));
        deleteItem = actionMode.addItemWithWidth(delete, R.drawable.msg_delete, dp(48), LocaleController.getString(R.string.Delete));

        ActionBarMenuItem otherItem = actionMode.addItemWithWidth(0, R.drawable.ic_ab_other, dp(48), LocaleController.getString(R.string.AccDescrMoreOptions));
        actionMode.addView(new View(getContext()), LayoutHelper.createLinear(5, LayoutHelper.MATCH_PARENT));
        archiveItem = otherItem.addSubItem(archive, R.drawable.msg_archive, LocaleController.getString(R.string.Archive));
        pin2Item = otherItem.addSubItem(pin2, R.drawable.msg_pin, LocaleController.getString(R.string.DialogPin));
        addToFolderItem = otherItem.addSubItem(add_to_folder, R.drawable.msg_addfolder, LocaleController.getString(R.string.FilterAddTo));
        removeFromFolderItem = otherItem.addSubItem(remove_from_folder, R.drawable.msg_removefolder, LocaleController.getString(R.string.FilterRemoveFrom));
        readItem = otherItem.addSubItem(read, R.drawable.msg_markread, LocaleController.getString(R.string.MarkAsRead));
        clearItem = otherItem.addSubItem(clear, R.drawable.msg_clear, LocaleController.getString(R.string.ClearHistory));
        blockItem = otherItem.addSubItem(block, R.drawable.msg_block, LocaleController.getString(R.string.BlockUser));

        muteItem.setOnLongClickListener(e -> {
            performSelectedDialogsAction(selectedDialogs, mute, true, true);
            return true;
        });

        actionModeViews.add(pinItem);
        actionModeViews.add(archive2Item);
        actionModeViews.add(muteItem);
        actionModeViews.add(deleteItem);
        actionModeViews.add(otherItem);

        updateCounters(false);
    }

    public void closeSearching() {
        if (actionBar != null && actionBar.isSearchFieldVisible()) {
            actionBar.closeSearchField();
            searchIsShowed = false;
            updateFilterTabs(true, true);
        }
    }

    public void scrollToFolder(int fid) {
        if (filterTabsView == null) {
            updateFilterTabs(true, true);
            if (filterTabsView == null) {
                return;
            }
        }
        int index = filterTabsView.getTabsCount() - 1;
        ArrayList<MessagesController.DialogFilter> filters = getMessagesController().getDialogFilters();
        for (int i = 0; i < filters.size(); ++i) {
            if (filters.get(i).id == fid) {
                index = i;
                break;
            }
        }

        FilterTabsView.Tab tab = filterTabsView.getTab(index);
        if (tab != null) {
            if (viewPages != null && viewPages.length > 0 && viewPages[0].selectedType == tab.id) {
                return;
            }
            filterTabsView.scrollToTab(tab, index);
        } else {
            filterTabsView.selectLastTab();
        }
    }

    public void switchToCurrentSelectedMode(boolean animated) {
        for (int a = 0; a < viewPages.length; a++) {
            viewPages[a].listView.stopScroll();
        }
        int a = animated && viewPages.length > 1 ? 1 : 0;
        if (viewPages[a].selectedType < 0 || viewPages[a].selectedType >= getMessagesController().getDialogFilters().size()) {
            return;
        }
        MessagesController.DialogFilter filter = getMessagesController().getDialogFilters().get(viewPages[a].selectedType);
        if (filter.isDefault()) {
            viewPages[a].dialogsType = initialDialogsType;
            viewPages[a].listView.updatePullState();
        } else {
            if (viewPages[a == 0 ? 1 : 0].dialogsType == 7) {
                viewPages[a].dialogsType = 8;
            } else {
                viewPages[a].dialogsType = 7;
            }
            viewPages[a].listView.setScrollEnabled(true);
            getMessagesController().selectDialogFilter(filter, viewPages[a].dialogsType == 8 ? 1 : 0);
        }

        if (viewPages.length > 1) {
            viewPages[1].isLocked = filter.locked;
        }

        viewPages[a].dialogsAdapter.setDialogsType(viewPages[a].dialogsType);
        viewPages[a].layoutManager.scrollToPositionWithOffset(viewPages[a].dialogsType == DIALOGS_TYPE_DEFAULT && hasHiddenArchive() && viewPages[a].archivePullViewState == ARCHIVE_ITEM_STATE_HIDDEN ? 1 : 0, (int) scrollYOffset);
        checkListLoad(viewPages[a]);
    }

    private boolean scrollBarVisible = true;

    private void showScrollbars(boolean show) {
        if (viewPages == null || scrollBarVisible == show) {
            return;
        }
        scrollBarVisible = show;
        for (int a = 0; a < viewPages.length; a++) {
            if (show) {
                viewPages[a].listView.setScrollbarFadingEnabled(false);
            }
            viewPages[a].listView.setVerticalScrollBarEnabled(show);
            if (show) {
                viewPages[a].listView.setScrollbarFadingEnabled(true);
            }
        }
    }

    private void updateFilterTabs(boolean force, boolean animated) {
        if (filterTabsView == null || inPreviewMode || searchIsShowed || (rightSlidingDialogContainer != null && rightSlidingDialogContainer.hasFragment())) {
            return;
        }
        if (filterOptions != null) {
            filterOptions.dismiss();
            filterOptions = null;
        }
        final ArrayList<MessagesController.DialogFilter> filters = getMessagesController().getDialogFilters();
        if (filters.size() > 1) {
            if (force || filterTabsView.getVisibility() != View.VISIBLE) {
                boolean animatedUpdateItems = animated;
                if (filterTabsView.getVisibility() != View.VISIBLE) {
                    animatedUpdateItems = false;
                }
                canShowFilterTabsView = true;
                boolean updateCurrentTab = filterTabsView.isEmpty();
                updateFilterTabsVisibility(animated);
                int id = filterTabsView.getCurrentTabId();
                int stableId = filterTabsView.getCurrentTabStableId();
                boolean selectWithStableId = false;
                if (id != filterTabsView.getDefaultTabId() && id >= filters.size()) {
                    filterTabsView.resetTabId();
                    selectWithStableId = true;
                }
                filterTabsView.removeTabs();
                for (int a = 0, N = filters.size(); a < N; a++) {
                    if (filters.get(a).isDefault()) {
                        filterTabsView.addTab(a, 0, LocaleController.getString(R.string.FilterAllChats), null, false, true, filters.get(a).locked);
                    } else {
                        final MessagesController.DialogFilter filter = filters.get(a);
                        filterTabsView.addTab(a, filter.localId, filter.name, filter.entities, filter.title_noanimate, false, filters.get(a).locked);
                    }
                }
                if (stableId >= 0) {
                    if (selectWithStableId) {
                        if (!filterTabsView.selectTabWithStableId(stableId)) {
                            while (id >= 0 && !filterTabsView.selectTabWithStableId(filterTabsView.getStableId(id))) {
                                id--;
                            }
                            if (id < 0) {
                                id = 0;
                            }
                        }
                    }
                    if (filterTabsView.getStableId(viewPages[0].selectedType) != stableId) {
                        updateCurrentTab = true;
                        viewPages[0].selectedType = id;
                    }
                }
                for (int a = 0; a < viewPages.length; a++) {
                    if (viewPages[a].selectedType >= filters.size()) {
                        viewPages[a].selectedType = filters.size() - 1;
                    }
                    viewPages[a].listView.setScrollingTouchSlop(RecyclerView.TOUCH_SLOP_PAGING);
                }
                filterTabsView.finishAddingTabs(animatedUpdateItems);
                if (updateCurrentTab) {
                    switchToCurrentSelectedMode(false);
                }
                if (filterTabsView.isLocked(filterTabsView.getCurrentTabId())) {
                    filterTabsView.selectFirstTab();
                }
            }
        } else {
            if (filterTabsView.getVisibility() != View.GONE) {
                filterTabsView.setIsEditing(false);
                showDoneItem(false);

                maybeStartTracking = false;
                if (startedTracking) {
                    startedTracking = false;
                    viewPages[0].setTranslationX(0);
                    viewPages[1].setTranslationX(viewPages[0].getMeasuredWidth());
                }
                if (viewPages[0].selectedType != filterTabsView.getDefaultTabId()) {
                    viewPages[0].selectedType = filterTabsView.getDefaultTabId();
                    viewPages[0].dialogsAdapter.setDialogsType(0);
                    viewPages[0].dialogsType = initialDialogsType;
                    viewPages[0].dialogsAdapter.notifyDataSetChanged();
                }
                viewPages[1].setVisibility(View.GONE);
                viewPages[1].selectedType = 0;
                viewPages[1].dialogsAdapter.setDialogsType(0);
                viewPages[1].dialogsType = initialDialogsType;
                viewPages[1].dialogsAdapter.notifyDataSetChanged();
                canShowFilterTabsView = false;
                updateFilterTabsVisibility(animated);
                for (int a = 0; a < viewPages.length; a++) {
                    if (viewPages[a].dialogsType == DIALOGS_TYPE_DEFAULT && viewPages[a].archivePullViewState == ARCHIVE_ITEM_STATE_HIDDEN && hasHiddenArchive()) {
                        int p = viewPages[a].layoutManager.findFirstVisibleItemPosition();
                        if (p == 0 || p == 1) {
                            viewPages[a].layoutManager.scrollToPositionWithOffset(1, (int) scrollYOffset);
                        }
                    }
                    viewPages[a].listView.setScrollingTouchSlop(RecyclerView.TOUCH_SLOP_DEFAULT);
                    viewPages[a].listView.requestLayout();
                    viewPages[a].requestLayout();
                }

                filterTabsView.resetTabId();
            }
        }
        updateCounters(false);

        final int currentDialogsType = viewPages[0].dialogsType;
        if (currentDialogsType == 7 || currentDialogsType == 8) {
            MessagesController.DialogFilter currentFilter = getMessagesController().selectedDialogFilter[currentDialogsType - 7];
            if (currentFilter != null) {
                boolean found = false;
                for (int i = 0; i < filters.size(); ++i) {
                    MessagesController.DialogFilter f = filters.get(i);
                    if (f != null && f.id == currentFilter.id) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    switchToCurrentSelectedMode(false);
                }
            }
        }
    }

    @Override
    protected void onPanTranslationUpdate(float y) {
        if (viewPages == null) {
            return;
        }
        panTranslationY = y;
        if (commentView != null && commentView.isPopupShowing()) {
            fragmentView.setTranslationY(y);
            for (int a = 0; a < viewPages.length; a++) {
                viewPages[a].setTranslationY(0);
            }
            if (!onlySelect) {
                actionBar.setTranslationY(0);
                if (topBulletin != null) {
                    topBulletin.updatePosition();
                }
            }
            if (searchViewPager != null) {
                searchViewPager.setTranslationY(searchViewPagerTranslationY);
            }
        } else {
            for (int a = 0; a < viewPages.length; a++) {
                viewPages[a].setTranslationY(y);
            }
            if (!onlySelect) {
                actionBar.setTranslationY(y);
                if (topBulletin != null) {
                    topBulletin.updatePosition();
                }
            }
            if (searchViewPager != null) {
                searchViewPager.setTranslationY(panTranslationY + searchViewPagerTranslationY);
            }
        }
    }

    @Override
    public void finishFragment() {
        super.finishFragment();
        if (filterOptions != null) {
            filterOptions.dismiss();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dialogStoriesCell != null) {
            dialogStoriesCell.onResume();
        }
        if (rightSlidingDialogContainer != null) {
            rightSlidingDialogContainer.onResume();
        }
        if (!parentLayout.isInPreviewMode() && blurredView != null && blurredView.getVisibility() == View.VISIBLE) {
            blurredView.setVisibility(View.GONE);
            blurredView.setBackground(null);
        }
        if (viewPages != null) {
            for (int a = 0; a < viewPages.length; a++) {
                viewPages[a].dialogsAdapter.notifyDataSetChanged();
            }
        }
        if (commentView != null) {
            commentView.onResume();
        }
        if (!onlySelect && folderId == 0 && communityId == 0) {
            getMediaDataController().checkStickers(MediaDataController.TYPE_EMOJI);
        }
        if (searchViewPager != null) {
            searchViewPager.onResume();
        }
        final boolean tosAccepted;
        if (!afterSignup) {
            tosAccepted = getUserConfig().unacceptedTermsOfService == null;
        } else {
            tosAccepted = true;
        }
        final NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (tosAccepted && folderId == 0 && communityId == 0 && checkPermission && !onlySelect && Build.VERSION.SDK_INT >= 23) {
            Activity activity = getParentActivity();
            if (activity != null) {
                checkPermission = false;
                boolean hasNotContactsPermission = activity.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED;
                boolean hasNotStoragePermission = (Build.VERSION.SDK_INT <= 28 || BuildVars.NO_SCOPED_STORAGE) && activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
                boolean hasNotNotificationsPermission = Build.VERSION.SDK_INT >= 33 && activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED;
                AndroidUtilities.runOnUIThread(() -> {
                    if (getParentActivity() == null) {
                        return;
                    }
                    afterSignup = false;
                    if (hasNotNotificationsPermission || hasNotContactsPermission || hasNotStoragePermission) {
                        askingForPermissions = true;
                        if (hasNotNotificationsPermission && NotificationPermissionDialog.shouldAsk(activity)) {
                            PermissionRequest.requestPermission(Manifest.permission.POST_NOTIFICATIONS, granted -> {
                                if (!granted) {
                                    showDialog(new NotificationPermissionDialog(activity, !PermissionRequest.canAskPermission(Manifest.permission.POST_NOTIFICATIONS), granted2 -> {
                                        if (!granted2) return;
                                        if (!PermissionRequest.canAskPermission(Manifest.permission.POST_NOTIFICATIONS)) {
                                            PermissionRequest.showPermissionSettings(Manifest.permission.POST_NOTIFICATIONS);
                                        } else {
                                            activity.requestPermissions(new String[] { Manifest.permission.POST_NOTIFICATIONS }, 1);
                                        }
                                    }));
                                }
                            });
                        } else if (hasNotContactsPermission && askAboutContacts && getUserConfig().syncContacts && activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                            AlertDialog.Builder builder = AlertsCreator.createContactsPermissionDialog(activity, param -> {
                                askAboutContacts = param != 0;
                                MessagesController.getGlobalNotificationsSettings().edit().putBoolean("askAboutContacts", askAboutContacts).apply();
                                askForPermissons(false);
                            });
                            showDialog(permissionDialog = builder.create());
                        } else if (hasNotStoragePermission && activity.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            if (activity instanceof BasePermissionsActivity) {
                                BasePermissionsActivity basePermissionsActivity = (BasePermissionsActivity) activity;
                                showDialog(permissionDialog = basePermissionsActivity.createPermissionErrorAlert(R.raw.permission_request_folder, getString(R.string.PermissionStorageWithHint)));
                            }
                        } else {
                            askForPermissons(true);
                        }
                    }
                }, afterSignup && (hasNotContactsPermission || hasNotNotificationsPermission) ? 4000 : 0);
            }
        } else if (!onlySelect && folderId == 0 && communityId == 0 && XiaomiUtilities.isMIUI() && !XiaomiUtilities.isCustomPermissionGranted(XiaomiUtilities.OP_SHOW_WHEN_LOCKED)) {
            if (getParentActivity() == null) {
                return;
            }
            if (!MessagesController.getGlobalNotificationsSettings().getBoolean("askedAboutMiuiLockscreen", false)) {
                showDialog(new AlertDialog.Builder(getParentActivity())
                    .setTopAnimation(R.raw.permission_request_apk, AlertsCreator.PERMISSIONS_REQUEST_TOP_ICON_SIZE, false, getThemedColor(Theme.key_dialogTopBackground))
                    .setMessage(getString(R.string.PermissionXiaomiLockscreen))
                    .setPositiveButton(getString(R.string.PermissionOpenSettings), (dialog, which) -> {
                        Intent intent = XiaomiUtilities.getPermissionManagerIntent();
                        if (intent != null) {
                            try {
                                getParentActivity().startActivity(intent);
                            } catch (Exception x) {
                                try {
                                    intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    intent.setData(Uri.parse("package:" + ApplicationLoader.applicationContext.getPackageName()));
                                    getParentActivity().startActivity(intent);
                                } catch (Exception xx) {
                                    FileLog.e(xx);
                                }
                            }
                        }
                    })
                    .setNegativeButton(getString(R.string.ContactsPermissionAlertNotNow), (dialog, which) -> MessagesController.getGlobalNotificationsSettings().edit().putBoolean("askedAboutMiuiLockscreen", true).commit())
                    .create());
            }
        } else if (folderId == 0 && communityId == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !notificationManager.canUseFullScreenIntent()) {
            if (getParentActivity() == null) {
                return;
            }
            if (!MessagesController.getGlobalNotificationsSettings().getBoolean("askedAboutFSILockscreen", false)) {
                showDialog(new AlertDialog.Builder(getParentActivity())
                    .setTopAnimation(R.raw.permission_request_apk, AlertsCreator.PERMISSIONS_REQUEST_TOP_ICON_SIZE, false, getThemedColor(Theme.key_dialogTopBackground))
                    .setMessage(getString(R.string.PermissionFSILockscreen))
                    .setPositiveButton(getString(R.string.PermissionOpenSettings), (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT);
                        intent.setData(Uri.parse("package:" + ApplicationLoader.applicationContext.getPackageName()));
                        if (intent != null) {
                            try {
                                getParentActivity().startActivity(intent);
                            } catch (Exception x) {
                                FileLog.e(x);
                            }
                        }
                    })
                    .setNegativeButton(getString(R.string.ContactsPermissionAlertNotNow), (dialog, which) -> MessagesController.getGlobalNotificationsSettings().edit().putBoolean("askedAboutFSILockscreen", true).commit())
                    .create());
            }
        }
        showFiltersHint();
        if (viewPages != null) {
            for (int a = 0; a < viewPages.length; a++) {
                if (viewPages[a].dialogsType == 0 && viewPages[a].archivePullViewState == ARCHIVE_ITEM_STATE_HIDDEN && viewPages[a].layoutManager.findFirstVisibleItemPosition() == 0 && hasHiddenArchive()) {
                    viewPages[a].layoutManager.scrollToPositionWithOffset(1, (int) scrollYOffset);
                }
                if (a == 0) {
                    viewPages[a].dialogsAdapter.resume();
                } else {
                    viewPages[a].dialogsAdapter.pause();
                }
            }
        }
        showNextSupportedSuggestion();
        Bulletin.addDelegate(this, new Bulletin.Delegate() {
            @Override
            public void onBottomOffsetChange(float offset) {
                if (undoView[0] != null && undoView[0].getVisibility() == View.VISIBLE) {
                    return;
                }
                additionalFloatingTranslation = Math.max(0, offset - navigationBarHeight - additionFloatingButtonOffset);
                updateFloatingButtonOffset();
            }

            @Override
            public void onShow(Bulletin bulletin) {
                if (undoView[0] != null && undoView[0].getVisibility() == View.VISIBLE) {
                    undoView[0].hide(true, 2);
                }
            }

            @Override
            public int getTopOffset(int tag) {
                return (
                    (actionBar != null ? actionBar.getMeasuredHeight() : 0) +
                    (filterTabsView != null && filterTabsView.getVisibility() == View.VISIBLE ? filterTabsView.getMeasuredHeight() : 0) +
                    (topPanelLayout != null ? topPanelLayout.getHeight() : 0) +
                    (dialogStoriesCell != null && dialogStoriesCellVisible ? (int) ((1f - dialogStoriesCell.getCollapsedProgress()) * dp(DialogStoriesCell.HEIGHT_IN_DP)) : 0) +
                    (dp(SEARCH_FIELD_HEIGHT))
                );
            }

            @Override
            public int getBottomOffset(int tag) {
                if (communityId != 0) {
                    return navigationBarHeight + dp(12 + 48);
                }
                return calculateListViewPaddingBottom();
            }
        });
        if (searchIsShowed) {
            AndroidUtilities.requestAdjustResize(getParentActivity(), classGuid);
        }
        updateVisibleRows(0, false);
        updateProxyButton(false, true);
        updateStoriesVisibility(false);
        checkSuggestClearDatabase();
        checkUi_mainTabsVisible();
        if (filterTabsView != null && viewPages[0] != null && viewPages[0].dialogsAdapter != null) {
            int dialogsType = viewPages[0].dialogsAdapter.getDialogsType();
            if (dialogsType == DIALOGS_TYPE_FOLDER1 || dialogsType == DIALOGS_TYPE_FOLDER2) {
                MessagesController.DialogFilter dialogFilter = getMessagesController().selectedDialogFilter[dialogsType == DIALOGS_TYPE_FOLDER1 ? 0 : 1];
                if (dialogFilter != null) {
                    filterTabsView.selectTabWithStableId(dialogFilter.localId);
                }
            }
        }
    }

    @Override
    public boolean presentFragment(BaseFragment fragment) {
        boolean b = super.presentFragment(fragment);
        if (b) {
            if (viewPages != null) {
                for (int a = 0; a < viewPages.length; a++) {
                    viewPages[a].dialogsAdapter.pause();
                }
            }
        }
        if (storyHint != null) {
            storyHint.hide();
        }
        if (storyPremiumHint != null) {
            storyPremiumHint.hide();
        }
        Bulletin.hideVisible();
        return b;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (storiesBulletin != null) {
            storiesBulletin.hide();
            storiesBulletin = null;
        }
        if (rightSlidingDialogContainer != null) {
            rightSlidingDialogContainer.onPause();
        }
        if (filterOptions != null) {
            filterOptions.dismiss();
        }
        if (commentView != null) {
            commentView.onPause();
        }
        if (undoView[0] != null) {
            undoView[0].hide(true, 0);
        }
        Bulletin.removeDelegate(this);

        if (viewPages != null) {
            for (int a = 0; a < viewPages.length; a++) {
                viewPages[a].dialogsAdapter.pause();
            }
        }
    }

    @Override
    public boolean onBackPressed(boolean invoked) {
        if (hasShownSheet()) {
            if (invoked) closeSheet();
            return false;
        } else if (rightSlidingDialogContainer.hasFragment() && rightSlidingDialogContainer.getFragment().onBackPressed(invoked)) {
            if (invoked) {
                rightSlidingDialogContainer.finishPreview();
                if (searchViewPager != null) {
                    searchViewPager.updateTabs();
                }
            }
            return false;
        } else if (filterOptions != null) {
            if (invoked) {
                filterOptions.dismiss();
                filterOptions = null;
            }
            return false;
        } else if (filterTabsView != null && filterTabsView.isEditing()) {
            if (invoked) {
                filterTabsView.setIsEditing(false);
                showDoneItem(false);
            }
            return false;
        } else if (actionBar != null && actionBar.isActionModeShowed()) {
            if (invoked) {
                if (searchViewPager != null && searchViewPager.getVisibility() == View.VISIBLE) {
                    searchViewPager.hideActionMode();
                }
                hideActionMode(true);
            }
            return false;
        } else if (animatorSearchVisible.getValue()) {
            if (invoked) {
                fragmentSearchField.editText.getText().clear();
                fragmentSearchFieldWatcher.toggleSearch(false);
                fragmentSearchField.editText.clearFocus();
            }
            return false;
        } else if (filterTabsView != null && filterTabsView.getVisibility() == View.VISIBLE && !tabsAnimationInProgress && !filterTabsView.isAnimatingIndicator() && !startedTracking && !filterTabsView.isFirstTabSelected()) {
            if (invoked) filterTabsView.selectFirstTab();
            return false;
        } else if (commentView != null && commentView.isPopupShowing()) {
            if (invoked) commentView.hidePopup(true);
            return false;
        } else if (dialogStoriesCell.isFullExpanded() && dialogStoriesCell.scrollToFirst()) {
            return false;
        }
        return super.onBackPressed(invoked);
    }

    @Override
    public void onBecomeFullyHidden() {
        if (closeSearchFieldOnHide) {
            if (actionBar != null) {
                actionBar.closeSearchField();
            }
            if (searchObject != null) {
                if (searchViewPager != null) {
                    searchViewPager.dialogsSearchAdapter.putRecentSearch(searchDialogId, searchObject);
                }
                searchObject = null;
            }
            closeSearchFieldOnHide = false;
        }
        if (!hasStories && filterTabsView != null && filterTabsView.getVisibility() == View.VISIBLE && animatorFilterTabsVisible.getValue()) {
            int scrollY = (int) -scrollYOffset;
            int actionBarHeight = ActionBar.getCurrentActionBarHeight();
            if (scrollY != 0 && scrollY != actionBarHeight) {
                if (scrollY < actionBarHeight / 2) {
            //        setScrollY(0);
                } else if (viewPages[0].listView.canScrollVertically(1)) {
            //        setScrollY(-actionBarHeight);
                }
            }
        }
        if (undoView[0] != null) {
            undoView[0].hide(true, 0);
        }
        if (!isInPreviewMode() && blurredView != null && blurredView.getVisibility() == View.VISIBLE) {
            blurredView.setVisibility(View.GONE);
            blurredView.setBackground(null);
        }
        super.onBecomeFullyHidden();
        checkUi_mainTabsVisible();
        canShowStoryHint = true;
    }

    @Override
    public void onBecomeFullyVisible() {
        super.onBecomeFullyVisible();
        if (isArchive()) {
            SharedPreferences preferences = MessagesController.getGlobalMainSettings();
            boolean showArchiveHint = preferences.getBoolean("archivehint", true);
            final boolean isEmpty = getDialogsArray(currentAccount, initialDialogsType, folderId, false).isEmpty();
            if (showArchiveHint && isEmpty) {
                showArchiveHint = false;
                MessagesController.getGlobalMainSettings().edit().putBoolean("archivehint", false).commit();
            }
            if (showArchiveHint) {
                preferences.edit().putBoolean("archivehint", false).commit();
                showArchiveHelp();
            }
        }
        if (canShowStoryHint && !storyHintShown && storyHint != null && storiesEnabled) {
            storyHintShown = true;
            canShowStoryHint = false;
            storyHint.show();
        }
        AndroidUtilities.runOnUIThread(this::createSearchViewPager, 200);
    }

    private void showArchiveHelp() {
        getContactsController().loadGlobalPrivacySetting();
        BottomSheet[] bottomSheet = new BottomSheet[1];
        ArchiveHelp archiveHelp = new ArchiveHelp(getContext(), currentAccount, getResourceProvider(), () -> {
            if (bottomSheet[0] != null) {
                bottomSheet[0].dismiss();
                bottomSheet[0] = null;
            }
            AndroidUtilities.runOnUIThread(() -> presentFragment(new ArchiveSettingsActivity()), 300);
        }, () -> {
            if (bottomSheet[0] != null) {
                bottomSheet[0].dismiss();
                bottomSheet[0] = null;
            }
        });
        bottomSheet[0] = new BottomSheet.Builder(getContext(), false, getResourceProvider())
            .setCustomView(archiveHelp, Gravity.TOP | Gravity.CENTER_HORIZONTAL)
            .show();
        bottomSheet[0].fixNavigationBar(getThemedColor(Theme.key_dialogBackground));
    }

    @Override
    public void setInPreviewMode(boolean isInPreviewMode) {
        super.setInPreviewMode(isInPreviewMode);
        if (!isInPreviewMode && avatarContainer != null) {
            actionBar.setBackground(null);
            ((ViewGroup.MarginLayoutParams) actionBar.getLayoutParams()).topMargin = 0;
            actionBar.removeView(avatarContainer);
            avatarContainer = null;
            updateFilterTabs(false, false);
            floatingButton3.imageView.setVisibility(View.VISIBLE);
            if (topPanelLayout != null) {
                if (fragmentContextViewWrapper != null) {
                    topPanelLayout.addView(fragmentContextViewWrapper);
                }
                if (fragmentLocationContextViewWrapper != null) {
                    topPanelLayout.addView(fragmentLocationContextViewWrapper);
                }
            }
        }
        if (dialogStoriesCell != null) {
            if (dialogStoriesCellVisible && !isInPreviewMode) {
                dialogStoriesCell.setVisibility(View.VISIBLE);
            } else {
                dialogStoriesCell.setVisibility(View.GONE);
            }
        }

        updateFloatingButtonVisibility(true);
        updateDialogsHint();
    }

    public boolean addOrRemoveSelectedDialog(long did, View cell) {
        if (onlySelect && getMessagesController().isForum(did)) {
            return false;
        }
        if (selectedDialogs.contains(did)) {
            selectedDialogs.remove(did);
            if (cell instanceof DialogCell) {
                ((DialogCell) cell).setChecked(false, true);
            } else if (cell instanceof ProfileSearchCell) {
                ((ProfileSearchCell) cell).setChecked(false, true);
            }
            return false;
        } else {
            selectedDialogs.add(did);
            if (cell instanceof DialogCell) {
                ((DialogCell) cell).setChecked(true, true);
            } else if (cell instanceof ProfileSearchCell) {
                ((ProfileSearchCell) cell).setChecked(true, true);
            }
            return true;
        }
    }

    public void search(String query, boolean animated) {
        showSearch(true, false, animated);
        if (fragmentSearchField != null) {
            fragmentSearchField.editText.setText(query);
            fragmentSearchField.editText.setSelection(query.length());
        }
    }

    private void showSearch(boolean show, boolean startFromDownloads, boolean animated) {
        showSearch(show, startFromDownloads, animated, false);
    }

    private void showSearch(boolean show, boolean startFromDownloads, boolean animated, boolean forceNotOnlyDialogs) {
        animatorSearchVisible.setValue(show, animated);

        if (!show) {
            updateSpeedItem(false);
        } else {
            createSearchViewPager();
        }
        if (initialDialogsType != 0 && initialDialogsType != 3) {
            animated = false;
        }
        if (searchAnimator != null) {
            searchAnimator.cancel();
            searchAnimator = null;
        }
        searchIsShowed = show;
        blur3_InvalidateBlur();
        if (show) {
            boolean onlyDialogsAdapter;
            if (searchFiltersWasShowed || forceNotOnlyDialogs) {
                onlyDialogsAdapter = false;
            } else {
                onlyDialogsAdapter = onlyDialogsAdapter();
            }
            if (searchViewPager != null) {
                searchViewPager.showOnlyDialogsAdapter(onlyDialogsAdapter);
            }
            whiteActionBar = !onlyDialogsAdapter || hasStories;
            if (whiteActionBar) {
                searchFiltersWasShowed = true;
            }
            if (searchTabsView == null && searchViewPager != null && !onlyDialogsAdapter && communityId == 0) {
                searchTabsView = searchViewPager.createTabsView(false, ViewPagerFixed.SELECTOR_TYPE_BUBBLE_STYLE);
                searchTabsAndFiltersLayout.addView(searchTabsView, 0, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL));
            } else if (searchTabsAndFiltersLayout != null && onlyDialogsAdapter && communityId == 0) {
                AndroidUtilities.removeFromParent(searchTabsView);
                searchTabsView = null;
            }
            if (searchViewPager != null) {
                checkUi_searchPagesPaddings(false);
                searchViewPager.setKeyboardHeight(((ContentView) fragmentView).getKeyboardHeight());
                searchViewPager.clear();
            }
            if (community != null) {
                FiltersView.MediaFilterData filterData = new FiltersView.MediaFilterData(R.drawable.search_users_filled, DialogObject.getShortName(community), null, FiltersView.FILTER_TYPE_CHAT);
                filterData.setUser(community);
                addSearchFilter(filterData);
            } else if (folderId != 0 && (rightSlidingDialogContainer == null || !rightSlidingDialogContainer.hasFragment())) {
                FiltersView.MediaFilterData filterData = new FiltersView.MediaFilterData(R.drawable.chats_archive, R.string.ArchiveSearchFilter, null, FiltersView.FILTER_TYPE_ARCHIVE);
                addSearchFilter(filterData);
            }
        }

        if (animated && searchViewPager != null && searchViewPager.dialogsSearchAdapter.hasRecentSearch()) {
            AndroidUtilities.setAdjustResizeToNothing(getParentActivity(), classGuid);
        } else {
            AndroidUtilities.requestAdjustResize(getParentActivity(), classGuid);
        }
        if (!show && dialogStoriesCell != null && dialogStoriesCellVisible) {
            dialogStoriesCell.setVisibility(View.VISIBLE);
        }
        final boolean budget = SharedConfig.getDevicePerformanceClass() == SharedConfig.PERFORMANCE_CLASS_LOW || !LiteMode.isEnabled(LiteMode.FLAG_CHAT_SCALE);
        if (animated) {
            if (show) {
                if (searchViewPager != null) {
                    searchViewPager.setVisibility(View.VISIBLE);
                    searchViewPager.reset();
                }

                updateFiltersView(true, null, null, false, false);
                if (searchTabsView != null) {
                    searchTabsView.hide(false, false);
                }
            } else {
                viewPages[0].listView.setVisibility(View.VISIBLE);
                viewPages[0].setVisibility(View.VISIBLE);
            }

            setDialogsListFrozen(true);
            viewPages[0].listView.setVerticalScrollBarEnabled(false);
            if (searchViewPager != null) {
                searchViewPager.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
            }
            searchAnimator = new AnimatorSet();
            ArrayList<Animator> animators = new ArrayList<>();
            animators.add(ObjectAnimator.ofFloat(viewPages[0], View.ALPHA, show ? 0.0f : 1.0f));
            if (!budget) {
                animators.add(ObjectAnimator.ofFloat(viewPages[0], View.SCALE_X, show ? 0.95f : 1.0f));
                animators.add(ObjectAnimator.ofFloat(viewPages[0], View.SCALE_Y, show ? 0.95f : 1.0f));
            } else {
                viewPages[0].setScaleX(1);
                viewPages[0].setScaleY(1);
            }
            if (rightSlidingDialogContainer != null) {
                rightSlidingDialogContainer.setVisibility(View.VISIBLE);
                animators.add(ObjectAnimator.ofFloat(rightSlidingDialogContainer, View.ALPHA, show ? 0.0f : 1.0f));
            }
            if (searchViewPager != null) {
                animators.add(ObjectAnimator.ofFloat(searchViewPager, View.ALPHA, show ? 1.0f : 0.0f));
                if (hasStories) {
                    float translationY = dp(DialogStoriesCell.HEIGHT_IN_DP) + scrollYOffset + dp(SEARCH_FIELD_HEIGHT);
                    animators.add(ObjectAnimator.ofFloat(searchViewPager, SEARCH_TRANSLATION_Y, show ? translationY : 0, show ? 0 : translationY));
                }
                if (!budget) {
                    animators.add(ObjectAnimator.ofFloat(searchViewPager, View.SCALE_X, show ? 1.0f : 1.05f));
                    animators.add(ObjectAnimator.ofFloat(searchViewPager, View.SCALE_Y, show ? 1.0f : 1.05f));
                } else {
                    searchViewPager.setScaleX(1);
                    searchViewPager.setScaleY(1);
                }
            }

            if (downloadsItem != null) {
                updateProxyButton(false, false);
            }

            ValueAnimator valueAnimator = ValueAnimator.ofFloat(searchAnimationProgress, show ? 1f : 0);
            valueAnimator.addUpdateListener(valueAnimator1 -> setSearchAnimationProgress((float) valueAnimator1.getAnimatedValue(), false));

            animators.add(valueAnimator);
            searchAnimator.playTogether(animators);
            searchAnimator.setDuration(show ? 200 : 180);
            searchAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT);

            if (!show) {
                searchAnimator.setStartDelay(20);
            }
            searchAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    notificationsLocker.unlock();
                    if (searchAnimator != animation) {
                        return;
                    }
                    setDialogsListFrozen(false);
                    if (show) {
                        viewPages[0].listView.hide();
                        if (dialogStoriesCell != null) {
                            dialogStoriesCell.setVisibility(View.GONE);
                        }
                        searchWasFullyShowed = true;
                        AndroidUtilities.requestAdjustResize(getParentActivity(), classGuid);
                        searchItem.setVisibility(View.GONE);
                        if (rightSlidingDialogContainer != null) {
                            rightSlidingDialogContainer.setVisibility(View.GONE);
                        }
                    } else {
                        // searchItem.collapseSearchFilters();
                        whiteActionBar = false;
                        if (searchViewPager != null) {
                            searchViewPager.setVisibility(View.GONE);
                        }
                        if (fragmentSearchField != null){
                            fragmentSearchField.clearSearchFilters();
                        }
                        //searchItem.clearSearchFilters();
                        if (searchViewPager != null) {
                            searchViewPager.clear();
                        }
                        viewPages[0].listView.show();
                        searchWasFullyShowed = false;
                        if (rightSlidingDialogContainer != null) {
                            rightSlidingDialogContainer.setVisibility(View.VISIBLE);
                        }
                    }

                    if (fragmentView != null) {
                        fragmentView.requestLayout();
                    }

                    setSearchAnimationProgress(show ? 1f : 0, false);

                    viewPages[0].listView.setVerticalScrollBarEnabled(true);
                    if (searchViewPager != null) {
                        searchViewPager.setBackground(null);
                    }
                    searchAnimator = null;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    notificationsLocker.unlock();
                    if (searchAnimator == animation) {
                        if (show) {
                            viewPages[0].listView.hide();
                        } else {
                            viewPages[0].listView.show();
                        }
                        searchAnimator = null;
                    }
                }
            });
            notificationsLocker.lock();
            searchAnimator.start();
        } else {
            setDialogsListFrozen(false);
            if (show) {
                viewPages[0].listView.hide();
            } else {
                viewPages[0].listView.show();
            }
            viewPages[0].setAlpha(show ? 0.0f : 1.0f);
            if (!budget) {
                viewPages[0].setScaleX(show ? 0.95f : 1.0f);
                viewPages[0].setScaleY(show ? 0.95f : 1.0f);
            } else {
                viewPages[0].setScaleX(1);
                viewPages[0].setScaleY(1);
            }
            if (searchViewPager != null) {
                searchViewPager.setAlpha(show ? 1.0f : 0.0f);
                if (!budget) {
                    searchViewPager.setScaleX(show ? 1.0f : 1.1f);
                    searchViewPager.setScaleY(show ? 1.0f : 1.1f);
                } else {
                    searchViewPager.setScaleX(1);
                    searchViewPager.setScaleY(1);
                }
                searchViewPager.setVisibility(show ? View.VISIBLE : View.GONE);
            }
            if (fragmentSearchField != null) {
                fragmentSearchField.setTranslationY((show ? -dp(FILTER_TABS_HEIGHT) : 0) + getSearchFieldAdditionOffset());
            }
            if (dialogStoriesCell != null) {
                if (dialogStoriesCellVisible && !isInPreviewMode() && !show) {
                    dialogStoriesCell.setVisibility(View.VISIBLE);
                } else {
                    dialogStoriesCell.setVisibility(View.GONE);
                }
            }
            setSearchAnimationProgress(show ? 1f : 0, false);
            fragmentView.invalidate();
        }
        if (initialSearchType >= 0 && searchViewPager != null) {
            searchViewPager.setPosition(searchViewPager.getPositionForType(initialSearchType));
        }
        if (!show) {
            initialSearchType = -1;
        }
        if (show && startFromDownloads && searchViewPager != null) {
            searchViewPager.showDownloads();
            updateSpeedItem(true);
        }

        checkUi_searchFiltersVisibility();
        updateDialogsHint();
    }

    public boolean onlyDialogsAdapter() {
        int dialogsCount = getMessagesController().getTotalDialogsCount();
        return onlySelect || /*searchViewPager != null && !searchViewPager.dialogsSearchAdapter.hasRecentSearch() ||*/ dialogsCount <= 10 && !hasStories;
    }

    private void updateFilterTabsVisibility(boolean animated) {
        if (fragmentView == null) {
            return;
        }
        if (isPaused || databaseMigrationHint != null) {
            animated = false;
        }
        if (searchIsShowed) {
            return;
        }
        animatorFilterTabsVisible.setValue(canShowFilterTabsView, animated);
    }

    private void setSearchAnimationProgress(float progress, boolean full) {
        searchAnimationProgress = progress;
        if (whiteActionBar && actionBar != null) {
            int color1 = (folderId != 0 || communityId != 0) ? getThemedColor(Theme.key_actionBarDefaultArchivedIcon) : getThemedColor(Theme.key_actionBarDefaultIcon);
            actionBar.setItemsColor(ColorUtils.blendARGB(color1, getThemedColor(Theme.key_actionBarActionModeDefaultIcon), searchAnimationProgress), false);
            actionBar.setItemsColor(ColorUtils.blendARGB(getThemedColor(Theme.key_actionBarActionModeDefaultIcon), getThemedColor(Theme.key_actionBarActionModeDefaultIcon), searchAnimationProgress), true);

            color1 = (folderId != 0 || communityId != 0) ? getThemedColor(Theme.key_actionBarDefaultArchivedSelector) : getThemedColor(Theme.key_actionBarDefaultSelector);
            int color2 = getThemedColor(Theme.key_actionBarActionModeDefaultSelector);
            actionBar.setItemsBackgroundColor(ColorUtils.blendARGB(color1, color2, searchAnimationProgress), false);
        }
        if (fragmentView != null) {
            fragmentView.invalidate();
        }

        final boolean budget = SharedConfig.getDevicePerformanceClass() == SharedConfig.PERFORMANCE_CLASS_LOW || !LiteMode.isEnabled(LiteMode.FLAG_CHAT_SCALE);
        if (full) {
            if (viewPages[0] != null) {
                if (progress < 1f) {
                    viewPages[0].setVisibility(View.VISIBLE);
                }
                viewPages[0].setAlpha(1f - progress);
                if (!budget) {
                    viewPages[0].setScaleX(.9f + .1f * progress);
                    viewPages[0].setScaleY(.9f + .1f * progress);
                }
            }
            if (rightSlidingDialogContainer != null) {
                if (progress >= 1f) {
                    rightSlidingDialogContainer.setVisibility(View.GONE);
                } else {
                    rightSlidingDialogContainer.setVisibility(View.VISIBLE);
                    rightSlidingDialogContainer.setAlpha(1f - progress);
                }
            }
            if (searchViewPager != null) {
                searchViewPager.setAlpha(progress);
                if (!budget) {
                    searchViewPager.setScaleX(1f + .05f * (1f - progress));
                    searchViewPager.setScaleY(1f + .05f * (1f - progress));
                }
            }
        }
        updateContextViewPosition();
    }

    private void findAndUpdateCheckBox(long dialogId, boolean checked) {
        if (viewPages == null) {
            return;
        }
        for (int b = 0; b < viewPages.length; b++) {
            int count = viewPages[b].listView.getChildCount();
            for (int a = 0; a < count; a++) {
                View child = viewPages[b].listView.getChildAt(a);
                if (child instanceof DialogCell) {
                    DialogCell dialogCell = (DialogCell) child;
                    if (dialogCell.getDialogId() == dialogId) {
                        dialogCell.setChecked(checked, true);
                        break;
                    }
                }
            }
        }
    }

    private void checkListLoad(ViewPage viewPage) {
        checkListLoad(viewPage, viewPage.layoutManager.findFirstVisibleItemPosition(), viewPage.layoutManager.findLastVisibleItemPosition());
    }

    private void checkListLoad(ViewPage viewPage, int firstVisibleItem, int lastVisibleItem) {
        if (tabsAnimationInProgress || startedTracking || filterTabsView != null && filterTabsView.getVisibility() == View.VISIBLE && filterTabsView.isAnimatingIndicator()) {
            return;
        }
        int visibleItemCount = Math.abs(lastVisibleItem - firstVisibleItem) + 1;
        if (lastVisibleItem != RecyclerView.NO_POSITION) {
            RecyclerView.ViewHolder holder = viewPage.listView.findViewHolderForAdapterPosition(lastVisibleItem);
            if (floatingForceVisible = holder != null && holder.getItemViewType() == 11) {
                hideFloatingButton(false);
            }
        } else {
            floatingForceVisible = false;
        }
        boolean loadArchived = false;
        boolean loadArchivedFromCache = false;
        boolean load = false;
        boolean loadFromCache = false;
        if (viewPage.dialogsType == DIALOGS_TYPE_FOLDER1 || viewPage.dialogsType == DIALOGS_TYPE_FOLDER2) {
            ArrayList<MessagesController.DialogFilter> dialogFilters = getMessagesController().getDialogFilters();
            if (viewPage.selectedType >= 0 && viewPage.selectedType < dialogFilters.size()) {
                MessagesController.DialogFilter filter = dialogFilters.get(viewPage.selectedType);
                if ((filter.flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) == 0) {
                    if (visibleItemCount > 0 && lastVisibleItem >= getDialogsArray(currentAccount, viewPage.dialogsType, 1, dialogsListFrozen).size() - 10 ||
                            visibleItemCount == 0 && !getMessagesController().isDialogsEndReached(1)) {
                        loadArchivedFromCache = !getMessagesController().isDialogsEndReached(1);
                        if (loadArchivedFromCache || !getMessagesController().isServerDialogsEndReached(1)) {
                            loadArchived = true;
                        }
                    }
                }
            }
        }
        if (visibleItemCount > 0 && lastVisibleItem >= getDialogsArray(currentAccount, viewPage.dialogsType, folderId, dialogsListFrozen).size() - 10 ||
                visibleItemCount == 0 && (viewPage.dialogsType == 7 || viewPage.dialogsType == 8) && !getMessagesController().isDialogsEndReached(folderId)) {
            loadFromCache = !getMessagesController().isDialogsEndReached(folderId);
            if (loadFromCache || !getMessagesController().isServerDialogsEndReached(folderId)) {
                load = true;
            }
        }
        if (load || loadArchived) {
            boolean loadFinal = load;
            boolean loadFromCacheFinal = loadFromCache;
            boolean loadArchivedFinal = loadArchived;
            boolean loadArchivedFromCacheFinal = loadArchivedFromCache;
            AndroidUtilities.runOnUIThread(() -> {
                if (loadFinal) {
                    getMessagesController().loadDialogs(folderId, -1, 100, loadFromCacheFinal);
                }
                if (loadArchivedFinal) {
                    getMessagesController().loadDialogs(1, -1, 100, loadArchivedFromCacheFinal);
                }
            });
        }
    }

    private void onItemClick(View view, int position, RecyclerListView.Adapter adapter, float x, float y) {
        if (getParentActivity() == null) {
            return;
        }
        long dialogId = 0;
        long topicId = 0;
        int message_id = 0;
        MessageObject msg = null;
        boolean isGlobalSearch = false;
        int folderId = 0;
        int filterId = 0;
        if (adapter instanceof DialogsAdapter) {
            DialogsAdapter dialogsAdapter = (DialogsAdapter) adapter;
            int dialogsType = dialogsAdapter.getDialogsType();
            if (dialogsType == DIALOGS_TYPE_FOLDER1 || dialogsType == DIALOGS_TYPE_FOLDER2) {
                MessagesController.DialogFilter dialogFilter = getMessagesController().selectedDialogFilter[dialogsType == DIALOGS_TYPE_FOLDER1 ? 0 : 1];
                filterId = dialogFilter == null ? 0 : dialogFilter.id;
            }
            Object object = dialogsAdapter.getItem(position);
            if (delegate != null && dialogsAdapter.isAllowForwardAsStories() && adapter.getItemViewType(position) == DialogsAdapter.VIEW_TYPE_FORWARD_TO_STORIES_CELL) {
                delegate.didSelectStories(this);
                return;
            }
            if (object instanceof TLRPC.User) {
                dialogId = ((TLRPC.User) object).id;
            } else if (object instanceof TLRPC.Chat) {
                dialogId = -((TLRPC.Chat) object).id;
            } else if (object instanceof TLRPC.Dialog) {
                TLRPC.Dialog dialog = (TLRPC.Dialog) object;
                folderId = dialog.folder_id;
                if (dialog instanceof TLRPC.TL_dialogFolder) {
                    if (actionBar.isActionModeShowed(null)) {
                        return;
                    }
                    TLRPC.TL_dialogFolder dialogFolder = (TLRPC.TL_dialogFolder) dialog;
                    Bundle args = new Bundle();
                    args.putInt("folderId", dialogFolder.folder.id);
                    presentFragment(new DialogsActivity(args));
                    return;
                }
                dialogId = dialog.id;
                if (actionBar.isActionModeShowed(null)) {
                    showOrUpdateActionMode(dialogId, view);
                    return;
                }
            } else if (object instanceof TLRPC.TL_recentMeUrlChat) {
                dialogId = -((TLRPC.TL_recentMeUrlChat) object).chat_id;
            } else if (object instanceof TLRPC.TL_recentMeUrlUser) {
                dialogId = ((TLRPC.TL_recentMeUrlUser) object).user_id;
            } else if (object instanceof TLRPC.TL_recentMeUrlChatInvite) {
                TLRPC.TL_recentMeUrlChatInvite chatInvite = (TLRPC.TL_recentMeUrlChatInvite) object;
                TLRPC.ChatInvite invite = chatInvite.chat_invite;
                if (invite.chat == null && (!invite.channel || invite.megagroup) || invite.chat != null && (!ChatObject.isChannel(invite.chat) || invite.chat.megagroup)) {
                    String hash = chatInvite.url;
                    int index = hash.indexOf('/');
                    if (index > 0) {
                        hash = hash.substring(index + 1);
                    }
                    showDialog(new JoinGroupAlert(getParentActivity(), invite, hash, DialogsActivity.this, null));
                    return;
                } else {
                    if (invite.chat != null) {
                        dialogId = -invite.chat.id;
                    } else {
                        return;
                    }
                }
            } else if (object instanceof TLRPC.TL_recentMeUrlStickerSet) {
                TLRPC.StickerSet stickerSet = ((TLRPC.TL_recentMeUrlStickerSet) object).set.set;
                TLRPC.TL_inputStickerSetID set = new TLRPC.TL_inputStickerSetID();
                set.id = stickerSet.id;
                set.access_hash = stickerSet.access_hash;
                showDialog(new StickersAlert(getParentActivity(), DialogsActivity.this, set, null, null, false));
                return;
            } else if (object instanceof TLRPC.TL_recentMeUrlUnknown) {
                return;
            } else {
                return;
            }
        } else if (searchViewPager != null && adapter == searchViewPager.dialogsSearchAdapter) {
            Object obj = searchViewPager.dialogsSearchAdapter.getItem(position);
            isGlobalSearch = searchViewPager.dialogsSearchAdapter.isGlobalSearch(position);
            if (obj instanceof TLRPC.User) {
                dialogId = ((TLRPC.User) obj).id;
                if (!onlySelect) {
                    searchDialogId = dialogId;
                    searchObject = (TLRPC.User) obj;
                }
            } else if (obj instanceof TLRPC.Chat) {
                dialogId = -((TLRPC.Chat) obj).id;
                if (!onlySelect) {
                    searchDialogId = dialogId;
                    searchObject = (TLRPC.Chat) obj;
                }
            } else if (obj instanceof TLRPC.EncryptedChat) {
                dialogId = DialogObject.makeEncryptedDialogId(((TLRPC.EncryptedChat) obj).id);
                if (!onlySelect) {
                    searchDialogId = dialogId;
                    searchObject = (TLRPC.EncryptedChat) obj;
                }
            } else if (obj instanceof MessageObject) {
                MessageObject messageObject = msg = (MessageObject) obj;
                dialogId = messageObject.getDialogId();
                message_id = messageObject.getId();
                TLRPC.Chat chat = getMessagesController().getChat(-dialogId);
                if (ChatObject.isForum(chat)) {
                    topicId = MessageObject.getTopicId(messageObject.currentAccount, messageObject.messageOwner, true);
                }
                if (searchViewPager != null) {
                    searchViewPager.dialogsSearchAdapter.addHashtagsFromMessage(searchViewPager.dialogsSearchAdapter.getLastSearchString());
                }
            } else if (obj instanceof String) {
                String str = (String) obj;
                if (searchViewPager != null && searchViewPager.dialogsSearchAdapter.isHashtagSearch()) {
                    fragmentSearchField.editText.setText(str);
                    fragmentSearchField.editText.setSelection(str.length());
                } else if (!str.equals("section")) {
                    NewContactBottomSheet activity = new NewContactBottomSheet(DialogsActivity.this, getContext());
                    activity.setInitialPhoneNumber(str, true);
//                    presentFragment(activity);
                    activity.show();
                }
            } else if (obj instanceof ContactsController.Contact) {
                ContactsController.Contact contact = (ContactsController.Contact) obj;
                AlertsCreator.createContactInviteDialog(DialogsActivity.this, contact.first_name, contact.last_name, contact.phones.get(0));
            } else if (obj instanceof TLRPC.TL_forumTopic && rightSlidingDialogContainer != null && rightSlidingDialogContainer.getFragment() instanceof TopicsFragment) {
                dialogId = ((TopicsFragment) rightSlidingDialogContainer.getFragment()).getDialogId();
                topicId = ((TLRPC.TL_forumTopic) obj).id;
            }

            if (dialogId != 0 && actionBar.isActionModeShowed()) {
                if (actionBar.isActionModeShowed(ACTION_MODE_SEARCH_DIALOGS_TAG) && message_id == 0 && !isGlobalSearch) {
                    showOrUpdateActionMode(dialogId, view);
                }
                return;
            }
        }

        if (dialogId == 0) {
            return;
        }

        if (onlySelect) {
            if (!validateSlowModeDialog(dialogId)) {
                return;
            }


            if ((!getMessagesController().isForum(dialogId) && !getMessagesController().isCommunity(dialogId) || isBotForumWithEmptyTopics(dialogId)) && (!selectedDialogs.isEmpty() || (initialDialogsType == DIALOGS_TYPE_FORWARD && selectAlertString != null))) {
                if (!selectedDialogs.contains(dialogId) && !checkCanWrite(dialogId)) {
                    return;
                }
                boolean checked = addOrRemoveSelectedDialog(dialogId, view);
                if (searchViewPager != null && adapter == searchViewPager.dialogsSearchAdapter) {
                    actionBar.closeSearchField();
                    findAndUpdateCheckBox(dialogId, checked);
                }
                updateSelectedCount();
            } else {
                if (canSelectTopics && getMessagesController().isCommunity(dialogId)) {
                    Bundle bundle = new Bundle(arguments);
                    bundle.putLong("community_id", -dialogId);
                    DialogsActivity dialogsFragment = new DialogsActivity(bundle);
                    dialogsFragment.parentForwardDialogFragment = this;
                    dialogsFragment.setDelegate(delegate);
                    presentFragment(dialogsFragment);
                } else if (canSelectTopics && (getMessagesController().isForum(dialogId) && !isBotForumWithEmptyTopics(dialogId) || getMessagesController().isMonoForumWithManageRights(dialogId))) {
                    Bundle bundle = new Bundle();
                    bundle.putLong("chat_id", -dialogId);
                    bundle.putBoolean("for_select", true);
                    bundle.putBoolean("forward_to", true);
                    bundle.putBoolean("bot_share_to", initialDialogsType == DIALOGS_TYPE_BOT_SHARE);
                    bundle.putBoolean("quote", isQuote);
                    bundle.putBoolean("reply_to", isReplyTo);
                    TopicsFragment topicsFragment = new TopicsFragment(bundle);
                    topicsFragment.setForwardFromDialogFragment(DialogsActivity.this);
                    presentFragment(topicsFragment);
                } else {
                    didSelectResult(dialogId, 0, true, false);
                }
            }
        } else {
            Bundle args = new Bundle();
            if (DialogObject.isEncryptedDialog(dialogId)) {
                args.putInt("enc_id", DialogObject.getEncryptedChatId(dialogId));
            } else if (DialogObject.isUserDialog(dialogId)) {
                args.putLong("user_id", dialogId);
            } else {
                long did = dialogId;
                if (message_id != 0) {
                    TLRPC.Chat chat = getMessagesController().getChat(-did);
                    if (chat != null && chat.migrated_to != null) {
                        args.putLong("migrated_to", did);
                        did = -chat.migrated_to.channel_id;
                    }
                }
                args.putLong("chat_id", -did);
            }
            if (message_id != 0) {
                args.putInt("message_id", message_id);
            } else if (!isGlobalSearch) {
                closeSearch();
            } else {
                if (searchObject != null) {
                    if (searchViewPager != null) {
                        searchViewPager.dialogsSearchAdapter.putRecentSearch(searchDialogId, searchObject);
                    }
                    searchObject = null;
                }
            }
            boolean canOpenInRightSlidingView = !(LocaleController.isRTL || searching || (AndroidUtilities.isTablet() && folderId != 0)) && LiteMode.isEnabled(LiteMode.FLAG_CHAT_FORUM_TWOCOLUMN) && !TMP_DISABLE_TOPICS_TWO_COLUMNS && communityId == 0;
            args.putInt("dialog_folder_id", folderId);
            args.putInt("dialog_filter_id", filterId);
            if (AndroidUtilities.isTablet() && (!getMessagesController().isForum(dialogId) || !canOpenInRightSlidingView)) {
                if (openedDialogId.dialogId == dialogId && (searchViewPager == null || adapter != searchViewPager.dialogsSearchAdapter)) {
                    if (getParentActivity() instanceof LaunchActivity) {
                        LaunchActivity launchActivity = (LaunchActivity) getParentActivity();
                        List<BaseFragment> rightFragments = launchActivity.getRightActionBarLayout().getFragmentStack();
                        if (!rightFragments.isEmpty()) {
                            if (rightFragments.size() == 1 && rightFragments.get(rightFragments.size() - 1) instanceof ChatActivity) {
                                ((ChatActivity) rightFragments.get(rightFragments.size() - 1)).onPageDownClicked();
                            } else if (rightFragments.size() == 2) {
                                launchActivity.getRightActionBarLayout().closeLastFragment();
                            } else if (getParentActivity() instanceof LaunchActivity) {
                                BaseFragment first = rightFragments.get(0);
                                rightFragments.clear();
                                rightFragments.add(first);
                                launchActivity.getRightActionBarLayout().rebuildFragments(INavigationLayout.REBUILD_FLAG_REBUILD_LAST);
                            }
                        }
                    }
                    return;
                }
            }
            if (searchViewPager != null && searchViewPager.actionModeShowing()) {
                searchViewPager.hideActionMode();
            }
            if (dialogId == getUserConfig().getClientUserId() && getMessagesController().savedViewAsChats) {
                args = new Bundle();
                args.putLong("dialog_id", UserConfig.getInstance(currentAccount).getClientUserId());
                args.putInt("type", MediaActivity.TYPE_MEDIA);
                args.putInt("start_from", SharedMediaLayout.TAB_SAVED_DIALOGS);
                if (sharedMediaPreloader == null) {
                    sharedMediaPreloader = new SharedMediaLayout.SharedMediaPreloader(this);
                }
                MediaActivity mediaActivity = new MediaActivity(args, sharedMediaPreloader);
                presentFragment(mediaActivity);
            } else if (searchString != null) {
                if (getMessagesController().checkCanOpenChat(args, DialogsActivity.this)) {
                    getNotificationCenter().postNotificationName(NotificationCenter.closeChats);
                    presentFragment(highlightFoundQuote(new ChatActivity(args), msg));
                }
            } else {
                slowedReloadAfterDialogClick = true;
                if (getMessagesController().checkCanOpenChat(args, DialogsActivity.this)) {
                    final TLRPC.Chat chat = getMessagesController().getChat(-dialogId);
                    final TLRPC.Dialog dialog = getMessagesController().getDialog(dialogId);
                    final boolean isChannel = ChatObject.isChannelAndNotMegaGroup(chat);
                    boolean needOpenChatActivity = dialog != null && dialog.view_forum_as_messages;

                    CommunityChatType communityChatType = null;
                    if (communityId != 0 && chat != null) {
                        communityChatType = CommunityUtils.getCommunityChatType(currentAccount, -chat.id);
                    }

                    if (communityChatType == CommunityChatType.YouCanSendJoinRequest) {
                        showDialog(new JoinGroupAlert(getContext(), chat, null, this, resourceProvider));
                    } else if (communityChatType == CommunityChatType.HiddenUnavailable) {
                        BulletinFactory.of(this).createSimpleBulletin(R.raw.e_hand_2, getString(isChannel ?
                            R.string.CommunityHiddenChannelUnavailable :
                            R.string.CommunityHiddenGroupUnavailable
                        )).show();
                    } else if (chat != null && (chat.monoforum || chat.forum) && topicId == 0) {
                        if (chat.monoforum) {
                            args.putInt("chatMode", ChatActivity.MODE_SUGGESTIONS);
                            args.putBoolean("isSubscriberSuggestions", !ChatObject.canManageMonoForum(currentAccount, chat));

                            ChatActivity activity = new ChatActivity(args);
//                            ForumUtilities.applyTopic(activity, MessagesStorage.TopicKey.of(-chat.id, getMessagesController().getForumLastTopicId(chat.id)));
                            presentFragment(highlightFoundQuote(activity, msg));
                        } else if (ChatObject.areTabsEnabled(chat)) {
                            ChatActivity activity = new ChatActivity(args);
                            ForumUtilities.applyTopic(activity, MessagesStorage.TopicKey.of(-chat.id, getMessagesController().getForumLastTopicId(chat.id)));
                            presentFragment(activity);
                        } else if (!LiteMode.isEnabled(LiteMode.FLAG_CHAT_FORUM_TWOCOLUMN) || TMP_DISABLE_TOPICS_TWO_COLUMNS || communityId != 0) {
                            if (needOpenChatActivity) {
                                presentFragment(highlightFoundQuote(new ChatActivity(args), msg));
                            } else {
                                presentFragment(new TopicsFragment(args));
                            }
                        } else {
                            if (!canOpenInRightSlidingView) {
                                if (needOpenChatActivity) {
                                    presentFragment(highlightFoundQuote(new ChatActivity(args), msg));
                                } else {
                                    presentFragment(new TopicsFragment(args));
                                }
                            } else if (!searching) {
                                if (needOpenChatActivity) {
                                    presentFragment(highlightFoundQuote(new ChatActivity(args), msg));
                                } else {
                                    if (rightSlidingDialogContainer.currentFragment != null && ((TopicsFragment) rightSlidingDialogContainer.currentFragment).getDialogId() == dialogId) {
                                        rightSlidingDialogContainer.finishPreview();
                                    } else {
                                        viewPages[0].listView.prepareSelectorForAnimation();
                                        TopicsFragment topicsFragment = new TopicsFragment(args) {
                                            @Override
                                            public boolean isRightFragment() {
                                                return true;
                                            }
                                        };
                                        topicsFragment.setParentDialogsActivity(this);
                                        rightSlidingDialogContainer.presentFragment(getParentLayout(), topicsFragment);
                                    }
                                    if (searchViewPager != null) {
                                        searchViewPager.updateTabs();
                                    }
                                }
                            }
                        }
                    } else if (ChatObject.isCommunity(chat)) {
                        args = new Bundle();
                        args.putLong("community_id", chat.id);
                        DialogsActivity dialogsActivity = new DialogsActivity(args);
                        presentFragment(dialogsActivity);
                    } else {
                        ChatActivity chatActivity = new ChatActivity(args);
                        if (topicId != 0) {
                            ForumUtilities.applyTopic(chatActivity, MessagesStorage.TopicKey.of(dialogId, topicId));
                        }
                        if (adapter instanceof DialogsAdapter && DialogObject.isUserDialog(dialogId) && (getMessagesController().dialogs_dict.get(dialogId) == null)) {
                            TLRPC.Document sticker = getMediaDataController().getGreetingsSticker();
                            if (sticker != null) {
                                chatActivity.setPreloadedSticker(sticker, true);
                            }
                        }
                        if (AndroidUtilities.isTablet()) {
                            if (rightSlidingDialogContainer.currentFragment != null) {
                                rightSlidingDialogContainer.finishPreview();
                            }
                        }
                        presentFragment(highlightFoundQuote(chatActivity, msg));
                    }
                }
            }
        }
    }

    private boolean isBotForumWithEmptyTopics(long dialogId) {
        if (dialogId < 0) {
            return false;
        }
        final TLRPC.User user = getMessagesController().getUser(dialogId);
        if (!UserObject.isBotForum(user)) {
            return false;
        }

        final ArrayList<TLRPC.TL_forumTopic> topics = MessagesController.getInstance(currentAccount)
                .getTopicsController().getTopics(-user.id);

        return (topics == null || topics.isEmpty())
            && MessagesController.getInstance(currentAccount).getTopicsController().endIsReached(-user.id);
    }

    public static ChatActivity highlightFoundQuote(ChatActivity chatActivity, MessageObject message) {
        if (message != null && message.hasHighlightedWords()) {
            try {
                CharSequence text = null;
                if (!TextUtils.isEmpty(message.caption)) {
                    text = message.caption;
                } else {
                    text = message.messageText;
                }
                CharSequence highlighted = AndroidUtilities.highlightText(text, message.highlightedWords, null);
                if (highlighted instanceof SpannableStringBuilder) {
                    SpannableStringBuilder spannedHighlighted = (SpannableStringBuilder) highlighted;
                    ForegroundColorSpanThemable[] spans = spannedHighlighted.getSpans(0, spannedHighlighted.length(), ForegroundColorSpanThemable.class);
                    if (spans.length > 0) {
                        int start = spannedHighlighted.getSpanStart(spans[0]);
                        int end = spannedHighlighted.getSpanEnd(spans[0]);
                        for (int i = 1; i < spans.length; ++i) {
                            int sstart = spannedHighlighted.getSpanStart(spans[i]);
                            int send = spannedHighlighted.getSpanStart(spans[i]);
                            if (sstart == end) {
                                end = send;
                            } else if (sstart > end) {
                                boolean whitespace = true;
                                for (int j = end; j <= sstart; ++j) {
                                    if (!Character.isWhitespace(spannedHighlighted.charAt(j))) {
                                        whitespace = false;
                                        break;
                                    }
                                }
                                if (whitespace) {
                                    end = send;
                                }
                            }
                        }
                        chatActivity.setHighlightQuote(message.getRealId(), text.subSequence(start, end).toString(), start);
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        return chatActivity;
    }

    public void setOpenedDialogId(long dialogId, long topicId) {
        openedDialogId.dialogId = dialogId;
        openedDialogId.topicId = topicId;

        if (viewPages == null) {
            return;
        }
        for (ViewPage viewPage : viewPages) {
            if (viewPage.isDefaultDialogType() && AndroidUtilities.isTablet()) {
                viewPage.dialogsAdapter.setOpenedDialogId(openedDialogId.dialogId);
            }
        }
        updateVisibleRows(MessagesController.UPDATE_MASK_SELECT_DIALOG);
    }

    private boolean onItemLongClick(RecyclerListView listView, View view, int position, float x, float y, int dialogsType, RecyclerListView.Adapter adapter) {
        if (getParentActivity() == null || view instanceof DialogsHintCell) {
            return false;
        }
        if (adapter.getItemViewType(position) == DialogsAdapter.VIEW_TYPE_FORWARD_TO_STORIES_CELL) {
            return false;
        }

        if (!actionBar.isActionModeShowed() && !AndroidUtilities.isTablet() && !onlySelect && view instanceof DialogCell && !getMessagesController().isForum(((DialogCell) view).getDialogId()) && !rightSlidingDialogContainer.hasFragment()) {
            DialogCell cell = (DialogCell) view;
            if (cell.isPointInsideAvatar(x, y)) {
                return showChatPreview(cell);
            }
        }
        if (rightSlidingDialogContainer != null && rightSlidingDialogContainer.hasFragment()) {
            return false;
        }
        if (searchViewPager != null && adapter == searchViewPager.dialogsSearchAdapter) {
            Object item = searchViewPager.dialogsSearchAdapter.getItem(position);
            if (!searchViewPager.dialogsSearchAdapter.isSearchWas()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString(R.string.ClearSearchSingleAlertTitle));
                long did;
                if (item instanceof TLRPC.Chat) {
                    TLRPC.Chat chat = (TLRPC.Chat) item;
                    if (chat.monoforum) {
                        builder.setMessage(LocaleController.formatString("ClearSearchSingleChatAlertText", R.string.ClearSearchSingleChatAlertText, ForumUtilities.getMonoForumTitle(currentAccount, chat)));
                    } else {
                        builder.setMessage(LocaleController.formatString("ClearSearchSingleChatAlertText", R.string.ClearSearchSingleChatAlertText, chat.title));
                    }
                    did = -chat.id;
                } else if (item instanceof TLRPC.User) {
                    TLRPC.User user = (TLRPC.User) item;
                    if (user.id == getUserConfig().clientUserId) {
                        builder.setMessage(LocaleController.formatString("ClearSearchSingleChatAlertText", R.string.ClearSearchSingleChatAlertText, LocaleController.getString(R.string.SavedMessages)));
                    } else {
                        builder.setMessage(LocaleController.formatString("ClearSearchSingleUserAlertText", R.string.ClearSearchSingleUserAlertText, ContactsController.formatName(user.first_name, user.last_name)));
                    }
                    did = user.id;
                } else if (item instanceof TLRPC.EncryptedChat) {
                    TLRPC.EncryptedChat encryptedChat = (TLRPC.EncryptedChat) item;
                    TLRPC.User user = getMessagesController().getUser(encryptedChat.user_id);
                    builder.setMessage(LocaleController.formatString("ClearSearchSingleUserAlertText", R.string.ClearSearchSingleUserAlertText, ContactsController.formatName(user.first_name, user.last_name)));
                    did = DialogObject.makeEncryptedDialogId(encryptedChat.id);
                } else {
                    return false;
                }
                builder.setPositiveButton(LocaleController.getString(R.string.ClearSearchRemove), (dialogInterface, i) -> searchViewPager.dialogsSearchAdapter.removeRecentSearch(did));
                builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                AlertDialog alertDialog = builder.create();
                showDialog(alertDialog);
                TextView button = (TextView) alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                if (button != null) {
                    button.setTextColor(getThemedColor(Theme.key_text_RedBold));
                }
                return true;
            }
        }
        TLRPC.Dialog dialog;
        if (searchViewPager != null && adapter == searchViewPager.dialogsSearchAdapter) {
            if (onlySelect) {
                onItemClick(view, position, adapter, x, y);
                return false;
            }
            long dialogId = 0;
            if (view instanceof ProfileSearchCell && !searchViewPager.dialogsSearchAdapter.isGlobalSearch(position)) {
                dialogId = ((ProfileSearchCell) view).getDialogId();
            }
            if (dialogId != 0) {
                showOrUpdateActionMode(dialogId, view);
                return true;
            }
            return false;
        } else {
            DialogsAdapter dialogsAdapter = (DialogsAdapter) adapter;

            Object item = dialogsAdapter.getItem(position);
            if (item instanceof TLRPC.Dialog) {
                dialog = (TLRPC.Dialog) item;
            } else {
                return false;

                /*
                ArrayList<TLRPC.Dialog> dialogs = getDialogsArray(currentAccount, dialogsType, folderId, dialogsListFrozen);
                position = dialogsAdapter.fixPosition(position);
                if (position < 0 || position >= dialogs.size()) {
                    return false;
                }
                dialog = dialogs.get(position);
                */
            }


        }

        if (dialog == null) {
            return false;
        }

        if (onlySelect) {
            if (initialDialogsType != DIALOGS_TYPE_FORWARD && !clickSelectsDialog()) {
                return false;
            }
            if (!validateSlowModeDialog(dialog.id)) {
                return false;
            }
            if (initialDialogsType == DIALOGS_TYPE_BOT_SHARE && clickSelectsDialog() && canSelectTopics && getMessagesController().isForum(dialog.id)) {
                Bundle bundle = new Bundle();
                bundle.putLong("chat_id", -dialog.id);
                bundle.putBoolean("for_select", true);
                bundle.putBoolean("forward_to", true);
                bundle.putBoolean("bot_share_to", initialDialogsType == DIALOGS_TYPE_BOT_SHARE);
                bundle.putBoolean("quote", isQuote);
                bundle.putBoolean("reply_to", isReplyTo);
                TopicsFragment topicsFragment = new TopicsFragment(bundle);
                topicsFragment.setForwardFromDialogFragment(DialogsActivity.this);
                presentFragment(topicsFragment);
                return false;
            }
            addOrRemoveSelectedDialog(dialog.id, view);
            updateSelectedCount();
            return true;
        } else {
            if (dialog instanceof TLRPC.TL_dialogFolder) {
                onArchiveLongPress(view);
                return false;
            }
            if (actionBar.isActionModeShowed() && isDialogPinned(dialog)) {
                return false;
            }
            showOrUpdateActionMode(dialog.id, view);
            return true;
        }
    }

    private void onArchiveLongPress(View view) {
        try {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        } catch (Exception ignored) {}
        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
        final boolean hasUnread = getMessagesStorage().getArchiveUnreadCount() != 0;

        int[] icons = new int[]{
                hasUnread ? R.drawable.msg_markread : 0,
                SharedConfig.archiveHidden ? R.drawable.chats_pin : R.drawable.chats_unpin,
        };
        CharSequence[] items = new CharSequence[]{
                hasUnread ? LocaleController.getString(R.string.MarkAllAsRead) : null,
                SharedConfig.archiveHidden ? LocaleController.getString(R.string.PinInTheList) : LocaleController.getString(R.string.HideAboveTheList)
        };
        builder.setItems(items, icons, (d, which) -> {
            if (which == 0) {
                getMessagesStorage().readAllDialogs(1);
            } else if (which == 1 && viewPages != null) {
                for (int a = 0; a < viewPages.length; a++) {
                    if (viewPages[a].dialogsType != 0 || viewPages[a].getVisibility() != View.VISIBLE) {
                        continue;
                    }
                    DialogCell dialogCell = findArchiveDialogCell(viewPages[a]);
                    viewPages[a].listView.toggleArchiveHidden(true, dialogCell);
                }
            }
        });
        showDialog(builder.create());
    }

    private DialogCell findArchiveDialogCell(ViewPage page) {
        RecyclerListView listView = page.listView;
        for (int i = 0; i < listView.getChildCount(); i++) {
            View child = listView.getChildAt(i);
            if (child instanceof DialogCell && ((DialogCell) child).isFolderCell()) {
                return (DialogCell) child;
            }
        }
        return null;
    }

    public boolean showChatPreview(DialogCell cell) {
        final boolean isCommunityCell = cell.isDialogCommunity();
        if (isCommunityCell) {
        //    return false;
        }

        if (cell.isDialogFolder()) {
            if (cell.getCurrentDialogFolderId() == 1) {
                onArchiveLongPress(cell);
            }
            return false;
        }
        long dialogId = cell.getDialogId();
        Bundle args = new Bundle();
        int message_id = cell.getMessageId();
        if (DialogObject.isEncryptedDialog(dialogId)) {
            return false;
        } else {
            if (DialogObject.isUserDialog(dialogId)) {
                args.putLong("user_id", dialogId);
            } else {
                long did = dialogId;
                if (communityId != 0) {
                    CommunityChatType communityChatType = CommunityUtils.getCommunityChatType(currentAccount, dialogId);
                    if (communityChatType == CommunityChatType.HiddenUnavailable || communityChatType == CommunityChatType.YouCanSendJoinRequest) {
                        return false;
                    }
                }

                final TLRPC.Chat chat = getMessagesController().getChat(-did);
                if (message_id != 0) {
                    if (chat != null && chat.migrated_to != null) {
                        args.putLong("migrated_to", did);
                        did = -chat.migrated_to.channel_id;
                    }
                }
                args.putLong("chat_id", -did);
            }
        }
        if (message_id != 0) {
            args.putInt("message_id", message_id);
        }

        final ArrayList<Long> dialogIdArray = new ArrayList<>();
        dialogIdArray.add(dialogId);

        boolean hasFolders = communityId == 0 &&
            getMessagesController().filtersEnabled &&
            getMessagesController().dialogFiltersLoaded &&
            getMessagesController().dialogFilters != null &&
            getMessagesController().dialogFilters.size() > 0;
        final ActionBarPopupWindow.ActionBarPopupWindowLayout[] previewMenu = new ActionBarPopupWindow.ActionBarPopupWindowLayout[1];

        LinearLayout foldersMenuView = null;
        int[] foldersMenu = new int[1];
        if (hasFolders) {
            foldersMenuView = new LinearLayout(getParentActivity());
            foldersMenuView.setOrientation(LinearLayout.VERTICAL);

            ScrollView scrollView = new ScrollView(getParentActivity()) {
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(
                            (int) Math.min(
                                    MeasureSpec.getSize(heightMeasureSpec),
                                    Math.min(AndroidUtilities.displaySize.y * 0.35f, dp(400))
                            ),
                            MeasureSpec.getMode(heightMeasureSpec)
                    ));
                }
            };
            LinearLayout linearLayout = new LinearLayout(getParentActivity());
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            scrollView.addView(linearLayout);
            final boolean backButtonAtTop = true;

            final int foldersCount = getMessagesController().dialogFilters.size();
            ActionBarMenuSubItem lastItem = null;
            for (int i = 0; i < foldersCount; ++i) {
                MessagesController.DialogFilter folder = getMessagesController().dialogFilters.get(i);
                if (folder.isDefault()) {
                    continue;
                }
                final boolean contains = folder.includesDialog(AccountInstance.getInstance(currentAccount), dialogId);
                final ArrayList<Long> alwaysShow = FiltersListBottomSheet.getDialogsCount(DialogsActivity.this, folder, dialogIdArray, true, false);
                if (!contains) {
                    int currentCount = folder.alwaysShow.size();
                    if (currentCount + alwaysShow.size() > 100) {
                        continue;
                    }
                }
                ActionBarMenuSubItem folderItem = lastItem = new ActionBarMenuSubItem(getParentActivity(), 2, !backButtonAtTop && linearLayout.getChildCount() == 0, false, null);
                folderItem.setChecked(contains);
                CharSequence title = folder.name;
                title = Emoji.replaceEmoji(title, folderItem.getTextView().getPaint().getFontMetricsInt(), false);
                title = MessageObject.replaceAnimatedEmoji(title, folder.entities, folderItem.getTextView().getPaint().getFontMetricsInt());
                folderItem.setEmojiCacheType(folder.title_noanimate ? AnimatedEmojiDrawable.CACHE_TYPE_NOANIMATE_FOLDER : AnimatedEmojiDrawable.CACHE_TYPE_MESSAGES);
                folderItem.setTextAndIcon(title, 0, new FolderDrawable(getContext(), R.drawable.msg_folders, folder.color));
                folderItem.getTextView().setEmojiColor(getThemedColor(Theme.key_featuredStickers_addButton));
                folderItem.setMinimumWidth(160);
                folderItem.setOnClickListener(e -> {
                    if (!contains) {
                        if (!alwaysShow.isEmpty()) {
                            for (int a = 0; a < alwaysShow.size(); a++) {
                                folder.neverShow.remove(alwaysShow.get(a));
                            }
                            folder.alwaysShow.addAll(alwaysShow);
                            FilterCreateActivity.saveFilterToServer(folder, folder.flags, folder.name, folder.entities, folder.title_noanimate, folder.color, folder.alwaysShow, folder.neverShow, folder.pinnedDialogs, false, false, true, true, false, DialogsActivity.this, null);
                        }
                        getUndoView().showWithAction(dialogId, UndoView.ACTION_ADDED_TO_FOLDER, alwaysShow.size(), folder, null, null);
                    } else {
                        folder.alwaysShow.remove(dialogId);
                        folder.neverShow.add(dialogId);
                        FilterCreateActivity.saveFilterToServer(folder, folder.flags, folder.name, folder.entities, folder.title_noanimate, folder.color, folder.alwaysShow, folder.neverShow, folder.pinnedDialogs, false, false, true, true, false, DialogsActivity.this, null);
                        getUndoView().showWithAction(dialogId, UndoView.ACTION_REMOVED_FROM_FOLDER, alwaysShow.size(), folder, null, null);
                    }
                    hideActionMode(true);
                    finishPreviewFragment();
                });
                linearLayout.addView(folderItem);
            }
            if (lastItem != null && backButtonAtTop) {
                lastItem.updateSelectorBackground(false, true);
            }
            if (linearLayout.getChildCount() <= 0) {
                hasFolders = false;
            } else {
                ActionBarPopupWindow.GapView gap = new ActionBarPopupWindow.GapView(getParentActivity(), getResourceProvider(), Theme.key_actionBarDefaultSubmenuSeparator);
                gap.setTag(R.id.fit_width_tag, 1);
                ActionBarMenuSubItem backItem = new ActionBarMenuSubItem(getParentActivity(), backButtonAtTop, !backButtonAtTop);
                backItem.setTextAndIcon(LocaleController.getString(R.string.Back), R.drawable.ic_ab_back);
                backItem.setMinimumWidth(160);
                backItem.setOnClickListener(e -> {
                    if (previewMenu[0] != null) {
                        previewMenu[0].getSwipeBack().closeForeground();
                    }
                });
                if (backButtonAtTop) {
                    foldersMenuView.addView(backItem);
                    foldersMenuView.addView(gap, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 8));
                    foldersMenuView.addView(scrollView);
                } else {
                    foldersMenuView.addView(scrollView);
                    foldersMenuView.addView(gap, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 8));
                    foldersMenuView.addView(backItem);
                }
            }
        }

        int flags = ActionBarPopupWindow.ActionBarPopupWindowLayout.FLAG_SHOWN_FROM_BOTTOM;
        if (hasFolders) {
            flags |= ActionBarPopupWindow.ActionBarPopupWindowLayout.FLAG_USE_SWIPEBACK;
        }

        final BaseFragment[] previewActivity = new BaseFragment[1];
        previewMenu[0] = new ActionBarPopupWindow.ActionBarPopupWindowLayout(getParentActivity(), R.drawable.popup_fixed_alert4, getResourceProvider(), flags);

        if (hasFolders) {
            foldersMenu[0] = previewMenu[0].addViewToSwipeBack(foldersMenuView);
            ActionBarMenuSubItem addToFolderItem = new ActionBarMenuSubItem(getParentActivity(), true, false);
            addToFolderItem.setTextAndIcon(LocaleController.getString(R.string.FilterAddTo), R.drawable.msg_addfolder);
            addToFolderItem.setMinimumWidth(160);
            addToFolderItem.setOnClickListener(e ->
                    previewMenu[0].getSwipeBack().openForeground(foldersMenu[0])
            );
            previewMenu[0].addView(addToFolderItem);
            previewMenu[0].getSwipeBack().setOnHeightUpdateListener(height -> {
                if (previewActivity[0] == null || previewActivity[0].getFragmentView() == null || !previewActivity[0].isInPreviewMode()) {
                    return;
                }
                ViewGroup.LayoutParams lp = previewActivity[0].getFragmentView().getLayoutParams();
                if (lp instanceof ViewGroup.MarginLayoutParams) {
                    ((ViewGroup.MarginLayoutParams) lp).bottomMargin = dp(24 + 16 + 8) + height;
                    previewActivity[0].getFragmentView().setLayoutParams(lp);
                }
            });
        }

        if (!isCommunityCell) {
            ActionBarMenuSubItem markAsUnreadItem = new ActionBarMenuSubItem(getParentActivity(), true, false);
            if (cell.getHasUnread()) {
                markAsUnreadItem.setTextAndIcon(LocaleController.getString(R.string.MarkAsRead), R.drawable.msg_markread);
            } else {
                markAsUnreadItem.setTextAndIcon(LocaleController.getString(R.string.MarkAsUnread), R.drawable.msg_markunread);
            }
            markAsUnreadItem.setMinimumWidth(160);
            markAsUnreadItem.setOnClickListener(e -> {
                if (cell.getHasUnread()) {
                    markAsRead(dialogId);
                } else {
                    markAsUnread(dialogId);
                }
                finishPreviewFragment();
            });
            previewMenu[0].addView(markAsUnreadItem);
        }

        final boolean[] hasPinAction = new boolean[1];
        hasPinAction[0] = true;
        TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(dialogId);
        boolean containsFilter;
        final MessagesController.DialogFilter filter = (
                (containsFilter = (viewPages[0].dialogsType == 7 || viewPages[0].dialogsType == 8) && (!actionBar.isActionModeShowed() || actionBar.isActionModeShowed(null))) ?
                        getMessagesController().selectedDialogFilter[viewPages[0].dialogsType == 8 ? 1 : 0] : null
        );
        if (!isDialogPinned(dialog)) {
            int pinnedCount = 0;
            int pinnedSecretCount = 0;
            int newPinnedCount = 0;
            int newPinnedSecretCount = 0;
            ArrayList<TLRPC.Dialog> dialogs = getMessagesController().getDialogs(folderId);
            for (int a = 0, N = dialogs.size(); a < N; a++) {
                TLRPC.Dialog dialog1 = dialogs.get(a);
                if (dialog1 instanceof TLRPC.TL_dialogFolder) {
                    continue;
                }
                if (isDialogPinned(dialog1)) {
                    if (DialogObject.isEncryptedDialog(dialog1.id)) {
                        pinnedSecretCount++;
                    } else {
                        pinnedCount++;
                    }
                } else if (!getMessagesController().isPromoDialog(dialog1.id, false)) {
                    break;
                }
            }
            int alreadyAdded = 0;
            if (dialog != null && !isDialogPinned(dialog)) {
                if (DialogObject.isEncryptedDialog(dialogId)) {
                    newPinnedSecretCount++;
                } else {
                    newPinnedCount++;
                }
                if (filter != null && filter.alwaysShow.contains(dialogId)) {
                    alreadyAdded++;
                }
            }
            int maxPinnedCount;
            if (containsFilter && filter != null) {
                maxPinnedCount = 100 - filter.alwaysShow.size();
            } else if (folderId != 0 || filter != null) {
                if (getUserConfig().isPremium()) {
                    maxPinnedCount = getMessagesController().maxFolderPinnedDialogsCountPremium;
                } else {
                    maxPinnedCount = getMessagesController().maxFolderPinnedDialogsCountDefault;
                }
            } else {
                if (getUserConfig().isPremium()) {
                    maxPinnedCount = getMessagesController().maxPinnedDialogsCountPremium;
                } else {
                    maxPinnedCount = getMessagesController().maxPinnedDialogsCountDefault;
                }
            }
            hasPinAction[0] = !(newPinnedSecretCount + pinnedSecretCount > maxPinnedCount || newPinnedCount + pinnedCount - alreadyAdded > maxPinnedCount);
        }

        if (hasPinAction[0]) {
            ActionBarMenuSubItem unpinItem = new ActionBarMenuSubItem(getParentActivity(), false, false);
            if (isDialogPinned(dialog)) {
                unpinItem.setTextAndIcon(LocaleController.getString(R.string.UnpinMessage), R.drawable.msg_unpin);
            } else {
                unpinItem.setTextAndIcon(LocaleController.getString(R.string.PinMessage), R.drawable.msg_pin);
            }
            unpinItem.setMinimumWidth(160);
            unpinItem.setOnClickListener(e -> {
                finishPreviewFragment();
                AndroidUtilities.runOnUIThread(() -> {
                    int minPinnedNum = Integer.MAX_VALUE;
                    if (filter != null && isDialogPinned(dialog)) {
                        for (int c = 0, N = filter.pinnedDialogs.size(); c < N; c++) {
                            minPinnedNum = Math.min(minPinnedNum, filter.pinnedDialogs.valueAt(c));
                        }
                        minPinnedNum -= canPinCount;
                    }
                    TLRPC.EncryptedChat encryptedChat = null;
                    if (DialogObject.isEncryptedDialog(dialogId)) {
                        encryptedChat = getMessagesController().getEncryptedChat(DialogObject.getEncryptedChatId(dialogId));
                    }
                    UndoView undoView = getUndoView();
                    if (undoView == null) {
                        return;
                    }
                    if (!isDialogPinned(dialog)) {
                        pinDialog(dialogId, true, filter, minPinnedNum, true);
                        undoView.showWithAction(0, UndoView.ACTION_PIN_DIALOGS, 1, 1600, null, null);
                        if (filter != null) {
                            if (encryptedChat != null) {
                                if (!filter.alwaysShow.contains(encryptedChat.user_id)) {
                                    filter.alwaysShow.add(encryptedChat.user_id);
                                }
                            } else {
                                if (!filter.alwaysShow.contains(dialogId)) {
                                    filter.alwaysShow.add(dialogId);
                                }
                            }
                        }
                    } else {
                        pinDialog(dialogId, false, filter, minPinnedNum, true);
                        undoView.showWithAction(0, UndoView.ACTION_UNPIN_DIALOGS, 1, 1600, null, null);
                    }
                    if (filter != null) {
                        FilterCreateActivity.saveFilterToServer(filter, filter.flags, filter.name, filter.entities, filter.title_noanimate, filter.color, filter.alwaysShow, filter.neverShow, filter.pinnedDialogs, false, false, true, true, false, DialogsActivity.this, null);
                    }
                    getMessagesController().reorderPinnedDialogs(folderId, null, 0);
                    updateCounters(true);
                    if (viewPages != null) {
                        for (int a = 0; a < viewPages.length; a++) {
                            viewPages[a].dialogsAdapter.onReorderStateChanged(false);
                        }
                    }
                    updateVisibleRows(MessagesController.UPDATE_MASK_REORDER | MessagesController.UPDATE_MASK_CHECK);

                }, 100);
            });
            previewMenu[0].addView(unpinItem);
        }

        if (!DialogObject.isUserDialog(dialogId) || !UserObject.isUserSelf(getMessagesController().getUser(dialogId))) {
            ActionBarMenuSubItem muteItem = new ActionBarMenuSubItem(getParentActivity(), false, false);
            if (!getMessagesController().isDialogMuted(dialogId, 0)) {
                muteItem.setTextAndIcon(LocaleController.getString(R.string.Mute), R.drawable.msg_mute);
            } else {
                muteItem.setTextAndIcon(LocaleController.getString(R.string.Unmute), R.drawable.msg_unmute);
            }
            muteItem.setMinimumWidth(160);
            muteItem.setOnClickListener(e -> {
                boolean isMuted = getMessagesController().isDialogMuted(dialogId, 0);
                if (!isMuted) {
                    getNotificationsController().setDialogNotificationsSettings(dialogId, 0, NotificationsController.SETTING_MUTE_FOREVER);
                } else {
                    getNotificationsController().setDialogNotificationsSettings(dialogId, 0, NotificationsController.SETTING_MUTE_UNMUTE);
                }
                BulletinFactory.createMuteBulletin(this, !isMuted, null).show();
                finishPreviewFragment();
            });
            previewMenu[0].addView(muteItem);
        }

        if (!isCommunityCell) {
            ActionBarMenuSubItem deleteItem = new ActionBarMenuSubItem(getParentActivity(), false, true);
            deleteItem.setIconColor(getThemedColor(Theme.key_text_RedRegular));
            deleteItem.setTextColor(getThemedColor(Theme.key_text_RedBold));
            deleteItem.setSelectorColor(Theme.multAlpha(getThemedColor(Theme.key_text_RedBold), .12f));
            deleteItem.setTextAndIcon(LocaleController.getString(R.string.Delete), R.drawable.msg_delete);
            deleteItem.setMinimumWidth(160);
            deleteItem.setOnClickListener(e -> {
                performSelectedDialogsAction(dialogIdArray, delete, false, false);
                finishPreviewFragment();
            });
            previewMenu[0].addView(deleteItem);
        }


        if (isCommunityCell) {
            if (searchString != null) {
                getNotificationCenter().postNotificationName(NotificationCenter.closeChats);
            }
            prepareBlurBitmap();
            parentLayout.setHighlightActionButtons(true);

            args = new Bundle();
            args.putLong("community_id", -dialogId);
            DialogsActivity dialogsActivity = new DialogsActivity(args);
            if (AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y) {
                presentFragmentAsPreview(previewActivity[0] = dialogsActivity);
            } else {
                presentFragmentAsPreviewWithMenu(previewActivity[0] = dialogsActivity, previewMenu[0]);
            }
        } else if (getMessagesController().checkCanOpenChat(args, DialogsActivity.this)) {
            if (searchString != null) {
                getNotificationCenter().postNotificationName(NotificationCenter.closeChats);
            }
            prepareBlurBitmap();
            parentLayout.setHighlightActionButtons(true);
            final ChatActivity chatActivity1 = new ChatActivity(args);
            if (AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y) {
                presentFragmentAsPreview(previewActivity[0] = chatActivity1);
            } else {
                presentFragmentAsPreviewWithMenu(previewActivity[0] = chatActivity1, previewMenu[0]);
                if (chatActivity1 != null) {
                    chatActivity1.allowExpandPreviewByClick = true;
                    try {
                        chatActivity1.getAvatarContainer().getAvatarImageView().performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null);
                    } catch (Exception ignore) {
                    }
                }
            }
            return true;
        }
        return false;
    }

    private void updateFloatingButtonVisibility(boolean animated) {
        final boolean isVisible = !(onlySelect && initialDialogsType != 10 || folderId != 0 || communityId != 0 || inPreviewMode || (searching && !onlySelect) || floatingButtonHidden);

        if (floatingButton3 != null) {
            floatingButton3.setButtonVisible(isVisible, animated);
        }
        if (floatingButtonStories != null) {
            floatingButtonStories.setButtonVisible(isVisible, animated);
        }
    }

    private void updateFloatingButtonOffset() {
        final float top = -navigationBarHeight - additionFloatingButtonOffset - additionalFloatingTranslation;
        final float baseTranslationY = top
            - floatingButtonPanOffset;

        if (floatingButton3 != null) {
            floatingButton3.setTranslationY(baseTranslationY);
        }
        if (floatingButtonStories != null) {
            floatingButtonStories.setTranslationY(baseTranslationY - dp(52));
            if (storyHint != null) {
                storyHint.setTranslationY(baseTranslationY - dp(52));
            }
        }
    }

    public boolean storiesEnabled = true;
    private void updateStoriesPosting() {
        final boolean storiesEnabled = getMessagesController().storiesEnabled();
        if (this.storiesEnabled != storiesEnabled) {
            updateFloatingButtonOffset();
            if (!this.storiesEnabled && storiesEnabled && storyHint != null) {
                storyHint.show();
            }
            this.storiesEnabled = storiesEnabled;
        }

        if (floatingButton3 == null) {
            return;
        }

        if (initialDialogsType == DIALOGS_TYPE_WIDGET) {
            floatingButton3.setImageResource(R.drawable.floating_check);
            floatingButton3.setContentDescription(LocaleController.getString(R.string.Done));
        } else {
            floatingButton3.setImageResource(R.drawable.filled_fab_compose_32);
            floatingButton3.setContentDescription(LocaleController.getString(R.string.NewMessageTitle));
        }
    }

    public boolean hasHiddenArchive() {
        return !onlySelect && initialDialogsType == DIALOGS_TYPE_DEFAULT && communityId == 0 && folderId == 0 && getMessagesController().hasHiddenArchive();
    }

    private boolean waitingForDialogsAnimationEnd(ViewPage viewPage) {
        return viewPage.dialogsItemAnimator.isRunning();
    }

    private void checkAnimationFinished() {
        AndroidUtilities.runOnUIThread(() -> {
//            if (viewPages != null && folderId != 0 && (frozenDialogsList == null || frozenDialogsList.isEmpty())) {
//                for (int a = 0; a < viewPages.length; a++) {
//                    viewPages[a].listView.setEmptyView(null);
//                    viewPages[a].progressView.setVisibility(View.INVISIBLE);
//                }
//                finishFragment();
//            }
            setDialogsListFrozen(false);
            updateDialogIndices();
        }, 300);
    }

    private void setScrollY(float value) {
        if (viewPages != null) {
            int glowOffset = viewPages[0].listView.getPaddingTop() + (int) value;
            for (int a = 0; a < viewPages.length; a++) {
                viewPages[a].listView.setTopGlowOffset(glowOffset);
            }
        }
        if (fragmentView == null || value == scrollYOffset) {
            return;
        }
        scrollYOffset = value;
        if (topBulletin != null) {
            topBulletin.updatePosition();
        }
        if (animatedStatusView != null) {
            final float alphaToSet = 1f - -value / ActionBar.getCurrentActionBarHeight();

            animatedStatusView.translateY2((int) value);
            animatedStatusView.setAlpha(MathUtils.clamp(alphaToSet, 0f, 1f));
            animatedStatusView.setVisibility(alphaToSet > 0 ? View.VISIBLE : View.INVISIBLE);
        }
        checkUi_searchFieldVisibility();
        fragmentView.invalidate();
    }

    private void prepareBlurBitmap() {
        if (blurredView == null) {
            return;
        }
        int w = (int) (fragmentView.getMeasuredWidth() / 9.0f);
        int h = (int) (fragmentView.getMeasuredHeight() / 9.0f);
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(getThemedColor(Theme.key_windowBackgroundWhite));
        Canvas canvas = new Canvas(bitmap);
        canvas.scale(1.0f / 9.0f, 1.0f / 9.0f);
        fragmentView.draw(canvas);
        Utilities.stackBlurBitmap(bitmap, Math.max(9, Math.max(w, h) / 180));
        blurredView.setBackground(new BitmapDrawable(bitmap));
        blurredView.setAlpha(0.0f);
        blurredView.setVisibility(View.VISIBLE);
        checkUi_mainTabsVisible();
    }

    @Override
    public void onTransitionAnimationProgress(boolean isOpen, float progress) {
        if (rightSlidingDialogContainer != null && rightSlidingDialogContainer.hasFragment()) {
            rightSlidingDialogContainer.getFragment().onTransitionAnimationProgress(isOpen, progress);
        } else if (blurredView != null && blurredView.getVisibility() == View.VISIBLE) {
            if (isOpen) {
                blurredView.setAlpha(1.0f - progress);
            } else {
                blurredView.setAlpha(progress);
            }
        }
        checkUi_mainTabsVisible();
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (rightSlidingDialogContainer != null && rightSlidingDialogContainer.hasFragment()) {
            rightSlidingDialogContainer.getFragment().onTransitionAnimationEnd(isOpen, backward);
        } else {
            if (isOpen && blurredView != null && blurredView.getVisibility() == View.VISIBLE) {
                blurredView.setVisibility(View.GONE);
                blurredView.setBackground(null);
            }
            if (isOpen && afterSignup) {
                try {
                    fragmentView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                } catch (Exception ignored) {}
                if (getParentActivity() instanceof LaunchActivity) {
                    ((LaunchActivity) getParentActivity()).getFireworksOverlay().start();
                }
            }
        }

        if (!isOpen && parentForwardDialogFragment != null) {
            parentForwardDialogFragment.removeSelfFromStack();
        }

        checkUi_mainTabsVisible();
    }

    private void resetScroll() {
        if (scrollYOffset == 0 || hasStories /*&& !ALLOW_SCROLL_SEARCH*/) {
            return;
        }

        final float target = hasStories ? -getMaxScrollYOffsetWithoutSearch() : 0;


        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(ObjectAnimator.ofFloat(this, SCROLL_Y, target));
        animatorSet.setInterpolator(CubicBezierInterpolator.DEFAULT);
        animatorSet.setDuration(250);
        animatorSet.start();
    }

    private void hideActionMode(boolean animateCheck) {
        actionBar.hideActionMode();
        selectedDialogs.clear();
        if (backDrawable != null) {
            backDrawable.setRotation(0, true);
        }
        if (filterTabsView != null) {
            filterTabsView.animateColorsTo(Theme.key_actionBarTabLine, Theme.key_actionBarTabActiveText, Theme.key_actionBarTabUnactiveText, Theme.key_actionBarTabSelector, Theme.key_windowBackgroundWhite);
        }
        if (actionBarColorAnimator != null) {
            actionBarColorAnimator.cancel();
            actionBarColorAnimator = null;
        }
        if (progressToActionMode == 0) {
            return;
        }
        float translateListHeight = 0;
        setScrollY(-getMaxScrollYOffset());
        for (int i = 0; i < viewPages.length; i++) {
            if (viewPages[i] != null) {
                viewPages[i].listView.cancelClickRunnables(true);
            }
        }
        translateListHeight = Math.max(0, dp((hasStories ? DialogStoriesCell.HEIGHT_IN_DP : 0) + SEARCH_FIELD_HEIGHT) + scrollYOffset);
        float finalTranslateListHeight = translateListHeight;
        actionBarColorAnimator = ValueAnimator.ofFloat(progressToActionMode, 0);
        actionBarColorAnimator.addUpdateListener(valueAnimator -> {
            viewPages[0].setTranslationY(finalTranslateListHeight * (1f - progressToActionMode));
            progressToActionMode = (float) valueAnimator.getAnimatedValue();
            for (int i = 0; i < actionBar.getChildCount(); i++) {
                if (actionBar.getChildAt(i).getVisibility() == View.VISIBLE && actionBar.getChildAt(i) != actionBar.getActionMode() && actionBar.getChildAt(i) != actionBar.getBackButton()) {
                    actionBar.getChildAt(i).setAlpha(1f - progressToActionMode);
                }
            }
            checkUi_searchFieldVisibility();
            checkUi_itemBackButtonVisibility();
            if (fragmentView != null) {
                fragmentView.invalidate();
            }
        });
        actionBarColorAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                actionBarColorAnimator = null;
                actionModeFullyShowed = false;
                invalidateScrollY = true;
                fixScrollYAfterArchiveOpened = true;
                fragmentView.invalidate();
                scrollAdditionalOffset = -(dp((hasStories ? DialogStoriesCell.HEIGHT_IN_DP : 0) + SEARCH_FIELD_HEIGHT) - finalTranslateListHeight);
                viewPages[0].setTranslationY(0);
                for (int i = 0; i < viewPages.length; i++) {
                    if (viewPages[i] != null) {
                        viewPages[i].listView.requestLayout();
                    }
                }
                fragmentView.requestLayout();
                if (fragmentSearchField != null && animatorSearchVisible.getValue()) {
                    fragmentSearchField.editText.requestFocus();
                    AndroidUtilities.showKeyboard(fragmentSearchField.editText);
                }
            }
        });
        actionBarColorAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
        actionBarColorAnimator.setDuration(200);
        actionBarColorAnimator.start();
        allowMoving = false;
        if (!movingDialogFilters.isEmpty()) {
            for (int a = 0, N = movingDialogFilters.size(); a < N; a++) {
                MessagesController.DialogFilter filter = movingDialogFilters.get(a);
                FilterCreateActivity.saveFilterToServer(filter, filter.flags, filter.name, filter.entities, filter.title_noanimate, filter.color, filter.alwaysShow, filter.neverShow, filter.pinnedDialogs, false, false, true, true, false, DialogsActivity.this, null);
            }
            movingDialogFilters.clear();
        }
        if (movingWas) {
            getMessagesController().reorderPinnedDialogs(folderId, null, 0);
            movingWas = false;
        }
        updateCounters(true);
        if (viewPages != null) {
            for (int a = 0; a < viewPages.length; a++) {
                viewPages[a].dialogsAdapter.onReorderStateChanged(false);
            }
        }
        updateVisibleRows(MessagesController.UPDATE_MASK_REORDER | MessagesController.UPDATE_MASK_CHECK | (animateCheck ? MessagesController.UPDATE_MASK_CHAT : 0));
    }

    private int getPinnedCount() {
        int pinnedCount = 0;
        ArrayList<TLRPC.Dialog> dialogs;
        boolean containsFilter = (viewPages[0].dialogsType == 7 || viewPages[0].dialogsType == 8) && (!actionBar.isActionModeShowed() || actionBar.isActionModeShowed(null));
        if (containsFilter) {
            dialogs = getDialogsArray(currentAccount, viewPages[0].dialogsType, folderId, dialogsListFrozen);
        } else {
            dialogs = getMessagesController().getDialogs(folderId);
        }
        for (int a = 0, N = dialogs.size(); a < N; a++) {
            TLRPC.Dialog dialog = dialogs.get(a);
            if (dialog instanceof TLRPC.TL_dialogFolder) {
                continue;
            }
            if (isDialogPinned(dialog)) {
                pinnedCount++;
            } else if (!getMessagesController().isPromoDialog(dialog.id, false)) {
                break;
            }
        }
        return pinnedCount;
    }

    private boolean isDialogPinned(TLRPC.Dialog dialog) {
        if (dialog == null) {
            return false;
        }
        MessagesController.DialogFilter filter;
        boolean containsFilter = (viewPages[0].dialogsType == 7 || viewPages[0].dialogsType == 8) && (!actionBar.isActionModeShowed() || actionBar.isActionModeShowed(null));
        if (containsFilter) {
            filter = getMessagesController().selectedDialogFilter[viewPages[0].dialogsType == 8 ? 1 : 0];
        } else {
            filter = null;
        }
        if (filter != null) {
            return filter.pinnedDialogs.indexOfKey(dialog.id) >= 0;
        }
        return dialog.pinned;
    }

    private void performSelectedDialogsAction(ArrayList<Long> selectedDialogs, int action, boolean alert, boolean longPress) {
        performSelectedDialogsAction(selectedDialogs, action, alert, longPress, null);
    }

    private void performSelectedDialogsAction(ArrayList<Long> selectedDialogs, int action, boolean alert, boolean longPress, HashSet<Long> dialogsIdsToRevoke) {
        if (getParentActivity() == null) {
            return;
        }
        MessagesController.DialogFilter filter;
        boolean containsFilter = (viewPages[0].dialogsType == 7 || viewPages[0].dialogsType == 8) && (!actionBar.isActionModeShowed() || actionBar.isActionModeShowed(null));
        if (containsFilter) {
            filter = getMessagesController().selectedDialogFilter[viewPages[0].dialogsType == 8 ? 1 : 0];
        } else {
            filter = null;
        }
        int count = selectedDialogs.size();
        int pinnedActionCount = 0;
        if (action == archive || action == archive2) {
            ArrayList<Long> copy = new ArrayList<>(selectedDialogs);
            getMessagesController().addDialogToFolder(copy, canUnarchiveCount == 0 ? 1 : 0, -1, null, 0);
            if (canUnarchiveCount == 0) {
                SharedPreferences preferences = MessagesController.getGlobalMainSettings();
                boolean hintShowed = preferences.getBoolean("archivehint_l", false) || SharedConfig.archiveHidden;
                if (!hintShowed) {
                    preferences.edit().putBoolean("archivehint_l", true).commit();
                }
                int undoAction;
                if (hintShowed) {
                    undoAction = copy.size() > 1 ? UndoView.ACTION_ARCHIVE_FEW : UndoView.ACTION_ARCHIVE;
                } else {
                    undoAction = copy.size() > 1 ? UndoView.ACTION_ARCHIVE_FEW_HINT : UndoView.ACTION_ARCHIVE_HINT;
                }
                final UndoView undoView = getUndoView();
                if (undoView != null) {
                    undoView.showWithAction(0, undoAction, null, () -> getMessagesController().addDialogToFolder(copy, folderId == 0 && communityId == 0 ? 0 : 1, -1, null, 0));
                }
            } else {
                ArrayList<TLRPC.Dialog> dialogs = getMessagesController().getDialogs(folderId);
                if (viewPages != null && dialogs.isEmpty() && !hasStories) {
                    viewPages[0].listView.setEmptyView(null);
                    viewPages[0].progressView.setVisibility(View.INVISIBLE);
                    finishFragment();
                }
            }
            hideActionMode(false);
            return;
        } else if ((action == pin || action == pin2) && canPinCount != 0) {
            int pinnedCount = 0;
            int pinnedSecretCount = 0;
            int newPinnedCount = 0;
            int newPinnedSecretCount = 0;
            ArrayList<TLRPC.Dialog> dialogs = getMessagesController().getDialogs(folderId);
            for (int a = 0, N = dialogs.size(); a < N; a++) {
                TLRPC.Dialog dialog = dialogs.get(a);
                if (dialog instanceof TLRPC.TL_dialogFolder) {
                    continue;
                }
                if (isDialogPinned(dialog)) {
                    if (DialogObject.isEncryptedDialog(dialog.id)) {
                        pinnedSecretCount++;
                    } else {
                        pinnedCount++;
                    }
                } else if (!getMessagesController().isPromoDialog(dialog.id, false)) {
                    break;
                }
            }
            int alreadyAdded = 0;
            for (int a = 0; a < count; a++) {
                long selectedDialog = selectedDialogs.get(a);
                TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(selectedDialog);
                if (dialog == null || isDialogPinned(dialog)) {
                    continue;
                }
                if (DialogObject.isEncryptedDialog(selectedDialog)) {
                    newPinnedSecretCount++;
                } else {
                    newPinnedCount++;
                }
                if (filter != null && filter.alwaysShow.contains(selectedDialog)) {
                    alreadyAdded++;
                }
            }
            int maxPinnedCount;
            if (containsFilter) {
                maxPinnedCount = 100 - filter.alwaysShow.size();
            } else if (folderId != 0 || filter != null) {
                if (UserConfig.getInstance(currentAccount).isPremium()) {
                    maxPinnedCount = getMessagesController().maxFolderPinnedDialogsCountPremium;
                } else {
                    maxPinnedCount = getMessagesController().maxFolderPinnedDialogsCountDefault;
                }
            } else {
                maxPinnedCount = getUserConfig().isPremium() ? getMessagesController().dialogFiltersPinnedLimitPremium : getMessagesController().dialogFiltersPinnedLimitDefault;
            }
            if (newPinnedSecretCount + pinnedSecretCount > maxPinnedCount || newPinnedCount + pinnedCount - alreadyAdded > maxPinnedCount) {
                if (folderId != 0 || filter != null) {
                    AlertsCreator.showSimpleAlert(DialogsActivity.this, LocaleController.formatString("PinFolderLimitReached", R.string.PinFolderLimitReached, LocaleController.formatPluralString("Chats", maxPinnedCount)));
                } else {
                    LimitReachedBottomSheet limitReachedBottomSheet = new LimitReachedBottomSheet(this, getParentActivity(), LimitReachedBottomSheet.TYPE_PIN_DIALOGS, currentAccount, null);
                    showDialog(limitReachedBottomSheet);
                }
                return;
            }
        } else if (action == community_ungroup) {
            if (alert) {
                AlertsCreator.showSimpleConfirmAlert(this,
                    getString(R.string.CommunityUngroupChats),
                    getString(R.string.CommunityUngroupChatsText),
                    getString(R.string.CommunityUngroupChatsButton),
                    true, () -> performSelectedDialogsAction(selectedDialogs, action, false, longPress, dialogsIdsToRevoke));
                return;
            }

            for (Long selectedDialog : selectedDialogs) {
                getMessagesController().toggleCommunityCollapsedInDialogs(-selectedDialog, false);
            }
        } else if ((action == delete || action == clear) && count > 1 && alert) {
            boolean hasDialogsToRevoke = false;
            HashSet<Long> dialogsIdsPossibleToRevoke = new HashSet<>();
            boolean canRevokePmInbox = MessagesController.getInstance(currentAccount).canRevokePmInbox;
            long revokeTimePmLimit = MessagesController.getInstance(currentAccount).revokeTimePmLimit;
            if (action == delete && canRevokePmInbox && revokeTimePmLimit == 0x7fffffff) {
                for (Long selectedDialog : selectedDialogs) {
                    if (DialogObject.isUserDialog(selectedDialog) || DialogObject.isEncryptedDialog(selectedDialog)) {
                        TLRPC.User user = null;
                        if (DialogObject.isEncryptedDialog(selectedDialog)) {
                            TLRPC.EncryptedChat encryptedChat = getMessagesController().getEncryptedChat(DialogObject.getEncryptedChatId(selectedDialog));
                            if (encryptedChat != null) {
                                user = getMessagesController().getUser(encryptedChat.user_id);
                            }
                        } else {
                            user = getMessagesController().getUser(selectedDialog);
                        }
                        if (user != null) {
                            ArrayList<MessageObject> dialogMessages = MessagesController.getInstance(currentAccount).dialogMessage.get(user.id);
                            boolean lastMessageIsJoined = dialogMessages != null && dialogMessages.size() == 1 && dialogMessages.get(0) != null && dialogMessages.get(0).messageOwner != null && (dialogMessages.get(0).messageOwner.action instanceof TLRPC.TL_messageActionUserJoined || dialogMessages.get(0).messageOwner.action instanceof TLRPC.TL_messageActionContactSignUp);
                            boolean canRevokeInbox = !user.bot && !UserObject.isDeleted(user) && user.id != getUserConfig().getClientUserId() && !lastMessageIsJoined;
                            if (canRevokeInbox) {
                                hasDialogsToRevoke = true;
                                dialogsIdsPossibleToRevoke.add(selectedDialog);
                            }
                        }
                    }
                }
            }

            createClearOrDeleteDialogsAlert(this, action == clear, action == delete, canClearCacheCount, count, hasDialogsToRevoke, param -> {
                    if (selectedDialogs.isEmpty()) {
                        return;
                    }
                    ArrayList<Long> didsCopy = new ArrayList<>(selectedDialogs);
                    final UndoView undoView = getUndoView();
                    if (undoView != null) {
                        undoView.showWithAction(didsCopy, action == delete ? UndoView.ACTION_DELETE_FEW : UndoView.ACTION_CLEAR_FEW, null, null, () -> {
                            if (action == delete) {
                                getMessagesController().setDialogsInTransaction(true);
                                performSelectedDialogsAction(didsCopy, action, false, false, param ? dialogsIdsPossibleToRevoke : null);
                                getMessagesController().setDialogsInTransaction(false);
                                getMessagesController().checkIfFolderEmpty(folderId);
                                if (folderId != 0 && getDialogsArray(currentAccount, viewPages[0].dialogsType, folderId, false).size() == 0) {
                                    viewPages[0].listView.setEmptyView(null);
                                    viewPages[0].progressView.setVisibility(View.INVISIBLE);
                                    finishFragment();
                                }
                            } else {
                                performSelectedDialogsAction(didsCopy, action, false, false);
                            }
                        }, null);
                    }
                    hideActionMode(action == clear);
            }, resourceProvider);
            return;
        } else if (action == block && alert) {
            TLRPC.User user;
            if (count == 1) {
                long did = selectedDialogs.get(0);
                user = getMessagesController().getUser(did);
            } else {
                user = null;
            }
            AlertsCreator.createBlockDialogAlert(DialogsActivity.this, count, canReportSpamCount != 0, user, (report, delete) -> {
                for (int a = 0, N = selectedDialogs.size(); a < N; a++) {
                    long did = selectedDialogs.get(a);
                    if (report) {
                        TLRPC.User u = getMessagesController().getUser(did);
                        getMessagesController().reportSpam(did, u, null, null, false);
                    }
                    if (delete) {
                        getMessagesController().deleteDialog(did, 0, true);
                    }
                    getMessagesController().blockPeer(did);
                }
                hideActionMode(false);
            });
            return;
        }
        int minPinnedNum = Integer.MAX_VALUE;
        if (filter != null && (action == pin || action == pin2) && canPinCount != 0) {
            for (int c = 0, N = filter.pinnedDialogs.size(); c < N; c++) {
                minPinnedNum = Math.min(minPinnedNum, filter.pinnedDialogs.valueAt(c));
            }
            minPinnedNum -= canPinCount;
        }
        boolean scrollToTop = false;
        for (int a = 0; a < count; a++) {
            long selectedDialog = selectedDialogs.get(a);
            TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(selectedDialog);
            if (dialog == null) {
                continue;
            }
            TLRPC.Chat chat;
            TLRPC.User user = null;

            TLRPC.EncryptedChat encryptedChat = null;
            if (DialogObject.isEncryptedDialog(selectedDialog)) {
                encryptedChat = getMessagesController().getEncryptedChat(DialogObject.getEncryptedChatId(selectedDialog));
                chat = null;
                if (encryptedChat != null) {
                    user = getMessagesController().getUser(encryptedChat.user_id);
                } else {
                    user = new TLRPC.TL_userEmpty();
                }
            } else if (DialogObject.isUserDialog(selectedDialog)) {
                user = getMessagesController().getUser(selectedDialog);
                chat = null;
            } else {
                chat = getMessagesController().getChat(-selectedDialog);
            }
            if (chat == null && user == null) {
                continue;
            }
            boolean isBot = user != null && user.bot && !MessagesController.isSupportUser(user);
            if (action == pin || action == pin2) {
                if (canPinCount != 0) {
                    if (isDialogPinned(dialog)) {
                        continue;
                    }
                    pinnedActionCount++;
                    pinDialog(selectedDialog, true, filter, minPinnedNum, count == 1);
                    if (filter != null) {
                        minPinnedNum++;
                        if (encryptedChat != null) {
                            if (!filter.alwaysShow.contains(encryptedChat.user_id)) {
                                filter.alwaysShow.add(encryptedChat.user_id);
                            }
                        } else {
                            if (!filter.alwaysShow.contains(dialog.id)) {
                                filter.alwaysShow.add(dialog.id);
                            }
                        }
                    }
                } else {
                    if (!isDialogPinned(dialog)) {
                        continue;
                    }
                    pinnedActionCount++;
                    pinDialog(selectedDialog, false, filter, minPinnedNum, count == 1);
                }
            } else if (action == read) {
                if (canReadCount != 0) {
                    markAsRead(selectedDialog);
                } else {
                    markAsUnread(selectedDialog);
                }
            } else if (action == delete || action == clear) {
                if (count == 1) {
                    if (action == delete && canDeletePsaSelected) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setTitle(LocaleController.getString(R.string.PsaHideChatAlertTitle));
                        builder.setMessage(LocaleController.getString(R.string.PsaHideChatAlertText));
                        builder.setPositiveButton(LocaleController.getString(R.string.PsaHide), (dialog1, which) -> {
                            getMessagesController().hidePromoDialog();
                            hideActionMode(false);
                        });
                        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                        showDialog(builder.create());
                    } else {
                         AlertsCreator.createClearOrDeleteDialogAlert(DialogsActivity.this, action == clear, chat, user, DialogObject.isEncryptedDialog(dialog.id), action == delete, false, false, (param) -> {
                            hideActionMode(false);
                            if (action == clear && ChatObject.isChannel(chat) && (!chat.megagroup || ChatObject.isPublic(chat))) {
                                getMessagesController().deleteDialog(selectedDialog, 2, param);
                            } else {
                                if (action == delete && folderId != 0 && getDialogsArray(currentAccount, viewPages[0].dialogsType, folderId, false).size() == 1) {
                                    viewPages[0].progressView.setVisibility(View.INVISIBLE);
                                }

                                debugLastUpdateAction = 3;
                                int selectedDialogIndex = -1;
                                if (action == delete) {
                                    setDialogsListFrozen(true);
                                    if (frozenDialogsList != null) {
                                        for (int i = 0; i < frozenDialogsList.size(); i++) {
                                            if (frozenDialogsList.get(i).id == selectedDialog) {
                                                selectedDialogIndex = i;
                                                break;
                                            }
                                        }
                                    }
                                    checkAnimationFinished();
                                }

                                final UndoView undoView = getUndoView();
                                if (undoView != null) {
                                    undoView.showWithAction(selectedDialog, action == clear ? UndoView.ACTION_CLEAR : param ? UndoView.ACTION_DELETE : UndoView.ACTION_LEAVE, () -> performDeleteOrClearDialogAction(action, selectedDialog, chat, isBot, param));
                                }

                                ArrayList<TLRPC.Dialog> currentDialogs = new ArrayList<>(getDialogsArray(currentAccount, viewPages[0].dialogsType, folderId, false));
                                int currentDialogIndex = -1;
                                for (int i = 0; i < currentDialogs.size(); i++) {
                                    if (currentDialogs.get(i).id == selectedDialog) {
                                        currentDialogIndex = i;
                                        break;
                                    }
                                }

                                if (action == delete) {
                                    if (selectedDialogIndex >= 0 && currentDialogIndex < 0 && frozenDialogsList != null) {
                                        frozenDialogsList.remove(selectedDialogIndex);
                                        viewPages[0].dialogsItemAnimator.prepareForRemove();
                                        viewPages[0].updateList(true);
                                    } else {
                                        setDialogsListFrozen(false);
                                    }
                                }
                            }
                        });
                    }
                    return;
                } else {
                    if (getMessagesController().isPromoDialog(selectedDialog, true)) {
                        getMessagesController().hidePromoDialog();
                    } else {
                        if (action == clear && canClearCacheCount != 0) {
                            getMessagesController().deleteDialog(selectedDialog, 2, false);
                        } else {
                            boolean revoke = dialogsIdsToRevoke != null && dialogsIdsToRevoke.contains(selectedDialog);
                            performDeleteOrClearDialogAction(action, selectedDialog, chat, isBot, revoke);
                        }
                    }
                }
            } else if (action == mute) {
                if (count == 1 && canMuteCount == 1) {
                    showDialog(AlertsCreator.createMuteAlert(this, selectedDialog, 0, null), dialog12 -> hideActionMode(true));
                    return;
                } else {
                    if (canUnmuteCount != 0) {
                        if (!getMessagesController().isDialogMuted(selectedDialog, 0)) {
                            continue;
                        }
                        getNotificationsController().setDialogNotificationsSettings(selectedDialog, 0, NotificationsController.SETTING_MUTE_UNMUTE);
                    } else if (longPress) {
                        showDialog(AlertsCreator.createMuteAlert(this, selectedDialogs, 0, null), dialog12 -> hideActionMode(true));
                        return;
                    } else {
                        if (getMessagesController().isDialogMuted(selectedDialog, 0)) {
                            continue;
                        }
                        getNotificationsController().setDialogNotificationsSettings(selectedDialog, 0, NotificationsController.SETTING_MUTE_FOREVER);
                    }
                }
            }
        }
        if (action == mute && !(count == 1 && canMuteCount == 1)) {
            BulletinFactory.createMuteBulletin(this, canUnmuteCount == 0, null).show();
        }
        if (action == pin || action == pin2) {
            if (filter != null) {
                FilterCreateActivity.saveFilterToServer(filter, filter.flags, filter.name, filter.entities, filter.title_noanimate, filter.color, filter.alwaysShow, filter.neverShow, filter.pinnedDialogs, false, false, true, true, false, DialogsActivity.this, null);
            } else {
                getMessagesController().reorderPinnedDialogs(folderId, null, 0);
            }
            UndoView undoView = getUndoView();
            if (searchIsShowed && undoView != null) {
                undoView.showWithAction(0, canPinCount != 0 ? UndoView.ACTION_PIN_DIALOGS : UndoView.ACTION_UNPIN_DIALOGS, pinnedActionCount);
            }
        }
        if (scrollToTop) {
            if (initialDialogsType != 10) {
                hideFloatingButton(false);
            }
            scrollToTop(true, false);
        }
        hideActionMode(action != pin2 && action != pin && action != delete);
    }

    private void markAsRead(long did) {
        TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(did);
        if (dialog instanceof TLRPC.TL_dialogCommunity) {
            final ArrayList<TLRPC.Dialog> dialogs = getMessagesController().getDialogsByCommunity(dialog.community_id);
            if (dialogs != null) {
                for (TLRPC.Dialog d : dialogs) {
                    markAsRead(d.id);
                }
            }
            return;
        }


        MessagesController.DialogFilter filter;
        boolean containsFilter = (viewPages[0].dialogsType == 7 || viewPages[0].dialogsType == 8) && (!actionBar.isActionModeShowed() || actionBar.isActionModeShowed(null));
        if (containsFilter) {
            filter = getMessagesController().selectedDialogFilter[viewPages[0].dialogsType == 8 ? 1 : 0];
        } else {
            filter = null;
        }
        debugLastUpdateAction = 2;
        int selectedDialogIndex = -1;
        if (filter != null && (filter.flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_READ) != 0 && !filter.alwaysShow(currentAccount, dialog)) {
            setDialogsListFrozen(true);
            checkAnimationFinished();
            if (frozenDialogsList != null) {
                for (int i = 0; i < frozenDialogsList.size(); i++) {
                    if (frozenDialogsList.get(i).id == did) {
                        selectedDialogIndex = i;
                        break;
                    }
                }
                if (selectedDialogIndex < 0) {
                    setDialogsListFrozen(false, false);
                }
            }
        }
        if (getMessagesController().isForum(did) || getMessagesController().isMonoForumWithManageRights(did)) {
            getMessagesController().markAllTopicsAsRead(did);
        }

        getMessagesController().markMentionsAsRead(did, 0);
        getMessagesController().markDialogAsRead(did, dialog.top_message, dialog.top_message, dialog.last_message_date, false, 0, 0, true, 0);

        if (selectedDialogIndex >= 0) {
            frozenDialogsList.remove(selectedDialogIndex);
            viewPages[0].dialogsItemAnimator.prepareForRemove();
            viewPages[0].updateList(true);
        }
    }

    private void markAsUnread(long did) {
        getMessagesController().markDialogAsUnread(did, null, 0);
    }

    private void markDialogsAsRead(ArrayList<TLRPC.Dialog> dialogs) {
        debugLastUpdateAction = 2;
        int selectedDialogIndex = -1;

        setDialogsListFrozen(true);
        checkAnimationFinished();
        for (int i = 0; i < dialogs.size(); i++) {
            long did = dialogs.get(i).id;
            TLRPC.Dialog dialog = dialogs.get(i);
            if (getMessagesController().isForum(did) || getMessagesController().isMonoForumWithManageRights(did)) {
                getMessagesController().markAllTopicsAsRead(did);
            }
            getMessagesController().markMentionsAsRead(did, 0);
            getMessagesController().markDialogAsRead(did, dialog.top_message, dialog.top_message, dialog.last_message_date, false, 0, 0, true, 0);
        }
        if (selectedDialogIndex >= 0) {
            frozenDialogsList.remove(selectedDialogIndex);
            viewPages[0].dialogsItemAnimator.prepareForRemove();
            viewPages[0].updateList(true);
        }
    }

    private void performDeleteOrClearDialogAction(int action, long selectedDialog, TLRPC.Chat chat, boolean isBot, boolean revoke) {
        if (action == clear) {
            getMessagesController().deleteDialog(selectedDialog, 1, revoke);
        } else {
            if (chat != null) {
                if (ChatObject.isNotInChat(chat)) {
                    getMessagesController().deleteDialog(selectedDialog, 0, revoke);
                } else {
                    TLRPC.User currentUser = getMessagesController().getUser(getUserConfig().getClientUserId());
                    getMessagesController().deleteParticipantFromChat(-selectedDialog, currentUser, null, revoke, false);
                }
            } else {
                getMessagesController().deleteDialog(selectedDialog, 0, revoke);
                if (isBot && revoke) {
                    getMessagesController().blockPeer(selectedDialog);
                }
            }
            if (AndroidUtilities.isTablet()) {
                getNotificationCenter().postNotificationName(NotificationCenter.closeChats, selectedDialog);
            }
            getMessagesController().checkIfFolderEmpty(folderId);
        }
    }

    private void pinDialog(long selectedDialog, boolean pin, MessagesController.DialogFilter filter, int minPinnedNum, boolean animated) {

        int selectedDialogIndex = -1;
        int currentDialogIndex = -1;

        int scrollToPosition = viewPages[0].dialogsType == 0 && hasHiddenArchive() && viewPages[0].archivePullViewState == ARCHIVE_ITEM_STATE_HIDDEN ? 1 : 0;
        int currentPosition = viewPages[0].layoutManager.findFirstVisibleItemPosition();

        if (filter != null) {
            int index = filter.pinnedDialogs.get(selectedDialog, Integer.MIN_VALUE);
            if (!pin && index == Integer.MIN_VALUE) {
                return;
            }

        }

        debugLastUpdateAction = pin ? 4 : 5;
        boolean needScroll = false;
        if (currentPosition > scrollToPosition || !animated) {
            needScroll = true;
        } else {
            setDialogsListFrozen(true);
            checkAnimationFinished();
            if (frozenDialogsList != null) {
                for (int i = 0; i < frozenDialogsList.size(); i++) {
                    if (frozenDialogsList.get(i).id == selectedDialog) {
                        selectedDialogIndex = i;
                        break;
                    }
                }
            }
        }

        boolean updated;
        if (filter != null) {
            if (pin) {
                filter.pinnedDialogs.put(selectedDialog, minPinnedNum);
            } else {
                filter.pinnedDialogs.delete(selectedDialog);
            }

            if (animated) {
                getMessagesController().onFilterUpdate(filter);
            }
            updated = true;
        } else {
            updated = getMessagesController().pinDialog(selectedDialog, pin, null, -1);
        }


        if (updated) {
            if (needScroll) {
                if (initialDialogsType != 10) {
                    hideFloatingButton(false);
                }
                scrollToTop(true, false);
            } else {
                ArrayList<TLRPC.Dialog> currentDialogs = getDialogsArray(currentAccount, viewPages[0].dialogsType, folderId, false);
                for (int i = 0; i < currentDialogs.size(); i++) {
                    if (currentDialogs.get(i).id == selectedDialog) {
                        currentDialogIndex = i;
                        break;
                    }
                }
            }
        }

        if (!needScroll) {
            boolean animate = false;
            if (selectedDialogIndex >= 0) {
                if (frozenDialogsList != null && currentDialogIndex >= 0 && selectedDialogIndex != currentDialogIndex) {
                    frozenDialogsList.add(currentDialogIndex, frozenDialogsList.remove(selectedDialogIndex));
                    viewPages[0].dialogsItemAnimator.prepareForRemove();
                    viewPages[0].updateList(true);

                    viewPages[0].layoutManager.scrollToPositionWithOffset(viewPages[0].dialogsType == 0 && hasHiddenArchive() && viewPages[0].archivePullViewState == ARCHIVE_ITEM_STATE_HIDDEN ? 1 : 0, (int) scrollYOffset);

                    animate = true;
                } else if (currentDialogIndex >= 0 && selectedDialogIndex == currentDialogIndex) {
                    animate = true;
                    AndroidUtilities.runOnUIThread(() -> setDialogsListFrozen(false), 200);
                }
            }
            if (!animate) {
                setDialogsListFrozen(false);
            }
        }
    }

    public void scrollToTop(boolean animated, boolean expandStories) {
        if (rightSlidingDialogContainer != null && rightSlidingDialogContainer.hasFragment()) {
            return;
        }

        int position = viewPages[0].dialogsType == 0 && hasHiddenArchive() && viewPages[0].archivePullViewState == ARCHIVE_ITEM_STATE_HIDDEN ? 1 : 0;
        int offset = 0;
        if (hasStories && !expandStories && !dialogStoriesCell.isExpanded()) {
            offset = -dp(DialogStoriesCell.HEIGHT_IN_DP);
        }
        if (animated) {
            viewPages[0].scrollHelper.setScrollDirection(RecyclerAnimationScrollHelper.SCROLL_DIRECTION_UP);
            viewPages[0].scrollHelper.scrollToPosition(position, offset, false, true);
            resetScroll();
        } else {
            viewPages[0].layoutManager.scrollToPositionWithOffset(position, offset);
            resetScroll();
        }
    }

    private void updateCounters(boolean hide) {
        int canClearHistoryCount = 0;
        int canDeleteCount = 0;
        int canUnpinCount = 0;
        int canArchiveCount = 0;
        int communitiesCount = 0;
        canDeletePsaSelected = false;
        canUnarchiveCount = 0;
        canUnmuteCount = 0;
        canMuteCount = 0;
        canPinCount = 0;
        canReadCount = 0;
        forumCount = 0;
        canClearCacheCount = 0;
        int cantBlockCount = 0;
        canReportSpamCount = 0;
        if (hide) {
            return;
        }
        int count = selectedDialogs.size();
        long selfUserId = getUserConfig().getClientUserId();
        SharedPreferences preferences = getNotificationsSettings();
        for (int a = 0; a < count; a++) {
            TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(selectedDialogs.get(a));
            if (dialog == null) {
                continue;
            }

            long selectedDialog = dialog.id;
            boolean pinned = isDialogPinned(dialog);
            boolean hasUnread = dialog.unread_count != 0 || dialog.unread_mark;

            if (getMessagesController().isForum(selectedDialog)) {
                forumCount++;
            }
            if (getMessagesController().isDialogMuted(selectedDialog, 0)) {
                canUnmuteCount++;
            } else {
                canMuteCount++;
            }

            if (hasUnread) {
                canReadCount++;
            }

            if (folderId == 1 || dialog.folder_id == 1) {
                canUnarchiveCount++;
            } else if (selectedDialog != selfUserId && dialog.community_id == 0 && selectedDialog != 777000 && !getMessagesController().isPromoDialog(selectedDialog, false)) {
                canArchiveCount++;
            }
            if (dialog.community_id != 0) {
                communitiesCount++;
            }

            if (!DialogObject.isUserDialog(selectedDialog) || selectedDialog == selfUserId || selectedDialog == UserObject.VERIFY) {
                cantBlockCount++;
            } else {
                TLRPC.User user = getMessagesController().getUser(selectedDialog);
                if (MessagesController.isSupportUser(user)) {
                    cantBlockCount++;
                } else {
                    if (preferences.getBoolean("dialog_bar_report" + selectedDialog, true)) {
                        canReportSpamCount++;
                    }
                }
            }

            if (DialogObject.isChannel(dialog)) {
                final TLRPC.Chat chat = getMessagesController().getChat(-selectedDialog);
                CharSequence[] items;
                if (getMessagesController().isPromoDialog(dialog.id, true)) {
                    canClearCacheCount++;
                    if (getMessagesController().promoDialogType == MessagesController.PROMO_TYPE_PSA) {
                        canDeleteCount++;
                        canDeletePsaSelected = true;
                    }
                } else {
                    if (pinned) {
                        canUnpinCount++;
                    } else {
                        canPinCount++;
                    }
                    if (chat != null && chat.megagroup) {
                        if (!ChatObject.isPublic(chat)) {
                            canClearHistoryCount++;
                        } else {
                            canClearCacheCount++;
                        }
                    } else {
                        canClearCacheCount++;
                    }
                    canDeleteCount++;
                }
            } else {
                final boolean isChat = DialogObject.isChatDialog(dialog.id);
                TLRPC.User user;
                TLRPC.Chat chat = isChat ? getMessagesController().getChat(-dialog.id) : null;
                if (DialogObject.isEncryptedDialog(dialog.id)) {
                    TLRPC.EncryptedChat encryptedChat = getMessagesController().getEncryptedChat(DialogObject.getEncryptedChatId(dialog.id));
                    if (encryptedChat != null) {
                        user = getMessagesController().getUser(encryptedChat.user_id);
                    } else {
                        user = new TLRPC.TL_userEmpty();
                    }
                } else {
                    user = !isChat && DialogObject.isUserDialog(dialog.id) ? getMessagesController().getUser(dialog.id) : null;
                }
                final boolean isBot = user != null && user.bot && !MessagesController.isSupportUser(user);

                if (pinned) {
                    canUnpinCount++;
                } else {
                    canPinCount++;
                }
                canClearHistoryCount++;
                canDeleteCount++;
            }
        }
        if (deleteItem != null) {
            if (canDeleteCount != count || communitiesCount > 0) {
                deleteItem.setVisibility(View.GONE);
            } else {
                deleteItem.setVisibility(View.VISIBLE);
            }
        }
        if (clearItem != null) {
            if (canClearCacheCount != 0 && canClearCacheCount != count || canClearHistoryCount != 0 && canClearHistoryCount != count || communitiesCount > 0) {
                clearItem.setVisibility(View.GONE);
            } else {
                clearItem.setVisibility(View.VISIBLE);
                if (canClearCacheCount != 0) {
                    clearItem.setText(LocaleController.getString(R.string.ClearHistoryCache));
                } else {
                    clearItem.setText(LocaleController.getString(R.string.ClearHistory));
                }
            }
        }
        if (archiveItem != null && archive2Item != null) {
            if (canUnarchiveCount != 0 && communitiesCount == 0 && communityId == 0) {
                final String contentDescription = LocaleController.getString(R.string.Unarchive);
                archiveItem.setTextAndIcon(contentDescription, R.drawable.msg_unarchive);
                archive2Item.setIcon(R.drawable.msg_unarchive);
                archive2Item.setContentDescription(contentDescription);
                if (filterTabsView != null && filterTabsView.getVisibility() == View.VISIBLE) {
                    archive2Item.setVisibility(View.VISIBLE);
                    archiveItem.setVisibility(View.GONE);
                } else {
                    archiveItem.setVisibility(View.VISIBLE);
                    archive2Item.setVisibility(View.GONE);
                }
            } else if (canArchiveCount != 0 && communitiesCount == 0 && communityId == 0) {
                final String contentDescription = LocaleController.getString(R.string.Archive);
                archiveItem.setTextAndIcon(contentDescription, R.drawable.msg_archive);
                archive2Item.setIcon(R.drawable.msg_archive);
                archive2Item.setContentDescription(contentDescription);
                if (filterTabsView != null && filterTabsView.getVisibility() == View.VISIBLE) {
                    archive2Item.setVisibility(View.VISIBLE);
                    archiveItem.setVisibility(View.GONE);
                } else {
                    archiveItem.setVisibility(View.VISIBLE);
                    archive2Item.setVisibility(View.GONE);
                }
            } else {
                archiveItem.setVisibility(View.GONE);
                archive2Item.setVisibility(View.GONE);
            }
        }
        if (pinItem != null && pin2Item != null) {
            if ((canPinCount + canUnpinCount != count) || (communityId != 0)) {
                pinItem.setVisibility(View.GONE);
                pin2Item.setVisibility(View.GONE);
            } else {
                if (filterTabsView != null && filterTabsView.getVisibility() == View.VISIBLE) {
                    pin2Item.setVisibility(View.VISIBLE);
                    pinItem.setVisibility(View.GONE);
                } else {
                    pinItem.setVisibility(View.VISIBLE);
                    pin2Item.setVisibility(View.GONE);
                }
            }
        }
        if (blockItem != null) {
            if (cantBlockCount != 0) {
                blockItem.setVisibility(View.GONE);
            } else {
                blockItem.setVisibility(View.VISIBLE);
            }
        }
        if (removeFromFolderItem != null) {
            boolean cantRemoveFromFolder = filterTabsView == null || filterTabsView.getVisibility() != View.VISIBLE || filterTabsView.currentTabIsDefault();
            if (!cantRemoveFromFolder) {
                try {
                    final int dialogsCount = getDialogsArray(currentAccount, viewPages[0].dialogsAdapter.getDialogsType(), folderId, dialogsListFrozen).size();
                    cantRemoveFromFolder = count >= dialogsCount;
                } catch (Exception ignore) {
                }
            }
            if (cantRemoveFromFolder) {
                removeFromFolderItem.setVisibility(View.GONE);
            } else {
                removeFromFolderItem.setVisibility(View.VISIBLE);
            }
        }
        if (addToFolderItem != null) {
            if (folderId == 1 || filterTabsView != null && getFilterTabsVisibilityFactor(false) > 0.5f && filterTabsView.currentTabIsDefault() && !FiltersListBottomSheet.getCanAddDialogFilters(this, selectedDialogs).isEmpty()) {
                addToFolderItem.setVisibility(View.VISIBLE);
            } else {
                addToFolderItem.setVisibility(View.GONE);
            }
        }
        if (muteItem != null) {
            if (canUnmuteCount != 0) {
                muteItem.setIcon(R.drawable.msg_unmute);
                muteItem.setContentDescription(LocaleController.getString(R.string.ChatsUnmute));
            } else {
                muteItem.setIcon(R.drawable.msg_mute);
                muteItem.setContentDescription(LocaleController.getString(R.string.ChatsMute));
            }
        }
        if (readItem != null) {
            if (canReadCount != 0) {
                readItem.setTextAndIcon(LocaleController.getString(R.string.MarkAsRead), R.drawable.msg_markread);
                readItem.setVisibility(View.VISIBLE);
            } else {
                if (forumCount == 0 && communitiesCount == 0) {
                    readItem.setTextAndIcon(LocaleController.getString(R.string.MarkAsUnread), R.drawable.msg_markunread);
                    readItem.setVisibility(View.VISIBLE);
                } else {
                    readItem.setVisibility(View.GONE);
                }
            }
        }
        if (pinItem != null && pin2Item != null) {
            if (canPinCount != 0) {
                pinItem.setIcon(R.drawable.msg_pin);
                pinItem.setContentDescription(LocaleController.getString(R.string.PinToTop));
                pin2Item.setText(LocaleController.getString(R.string.DialogPin));
            } else {
                pinItem.setIcon(R.drawable.msg_unpin);
                pinItem.setContentDescription(LocaleController.getString(R.string.UnpinFromTop));
                pin2Item.setText(LocaleController.getString(R.string.DialogUnpin));
            }
        }
    }

    private boolean validateSlowModeDialog(long dialogId) {
        if (messagesCount <= 1 && (commentView == null || commentView.getVisibility() != View.VISIBLE || TextUtils.isEmpty(commentView.getFieldText()))) {
            return true;
        }
        if (!DialogObject.isChatDialog(dialogId)) {
            return true;
        }
        TLRPC.Chat chat = getMessagesController().getChat(-dialogId);
        if (chat != null && !ChatObject.hasAdminRights(chat) && chat.slowmode_enabled) {
            AlertsCreator.showSimpleAlert(DialogsActivity.this, LocaleController.getString(R.string.Slowmode), LocaleController.getString(R.string.SlowmodeSendError));
            return false;
        }
        return true;
    }

    private void showOrUpdateActionMode(long dialogId, View cell) {
        addOrRemoveSelectedDialog(dialogId, cell);
        boolean updateAnimated = false;
        if (actionBar.isActionModeShowed()) {
            if (selectedDialogs.isEmpty()) {
                hideActionMode(true);
                return;
            }
            updateAnimated = true;
        } else {
            if (searchIsShowed) {
                createActionMode(ACTION_MODE_SEARCH_DIALOGS_TAG);
                if (actionBar.getBackButton() != null && actionBar.getBackButton().getDrawable() instanceof MenuDrawable) {
                    actionBar.setBackButtonDrawable(new BackDrawable(false));
                }
            } else {
                createActionMode(null);
            }
            AndroidUtilities.hideKeyboard(fragmentView.findFocus());
            actionBar.setActionModeOverrideColor(getThemedColor(Theme.key_windowBackgroundWhite));
            actionBar.showActionMode();
            if (getPinnedCount() > 1) {
                if (viewPages != null) {
                    for (int a = 0; a < viewPages.length; a++) {
                        viewPages[a].dialogsAdapter.onReorderStateChanged(true);
                    }
                }
                updateVisibleRows(MessagesController.UPDATE_MASK_REORDER);
            }

            if (!searchIsShowed) {
                AnimatorSet animatorSet = new AnimatorSet();
                ArrayList<Animator> animators = new Arrxœ @à¿xœì}ksÛÆ’è÷ü
Ø¶¨k†‘íóÈu"{)Š²YÑƒKÒöIåºTJˆA€vtÎñ¿Ó=3À¼1 (ÉÎ.R‹À<zzzzzº{ºÃ›“¤\ÿü²³÷Ówò,ò"è$Ù:ƒƒ`ÿ'òÏÏA8_'yvšGñ»$ş\öÊäŸ1©„OìÿÒš€
ŸàZõ«xİ	]ÃUze¼'Ÿòõ¯>V=¨4ØEœ­«woâäêzİÙ~YšëgQ‘'ÑÛu’&ë$.{ó4‹£"ü^¦q?K–!4Ön-M„X(/Ê^EóËßãùºÏŞõòÅqš‡k¬ßÅA÷¦ƒşÉğâ×n°ß{ºèO{û‹=CÓ_´7¼£i¼î­Òğf–“1_ÇE§‚ÀĞŒX‰ íhSĞ=Ûßo*½‹µJ_¾“~&‹ rtò4/øÈƒGA¶ISÓü›kôæa6S½Çæª„†Ş…é&ÖĞ¾*ò«".ËYŞ¯hŒ |¡ô±€ÂÁº³2×1?¥$q¹('ÿ„’BÈ&w^êIãìj}MŞš	ĞUş-ùàB<bÙ^J B¢x¤Éüãd“e@¨eg]lâF*’™Ç{®¯{ËğÎ~7ˆVÎuXN	JÉÚ^GI˜æWì÷ NÓŞ›áèõ›ÙÅèìâh¼ö÷‚'ÁtØŸŞ\†'G´ ¼.çE¦¿/„X3&x‚öí$ÓÙ¢şÓj0?{¦á¸Y^-	Ï@Äñoâr}ŞäåKõb	évfDccï–E@xÈÛUÄêÅYİŸDÊ¾iÀ†‰Ìå^ µ Œ’şG¸hLL¾&¼ıÀ6øIÛ¿v¾·ûÿ!qm"Â*
%V~¤Ñ ßd0¶åÄ	G¯Ù_w’=øõ.)“Kàí7d88 øİh::<ÿñ†N±*Ğô©i§E½Ãpşñp³^»g‚DÊîûéê:ì<]ßûbW'WóÀ¸ šØ‘´X’ŒU”j" ½³ùu<ÿø6¹(Éş:¿>Nâ4'Go‚WHÖñ²F£½Î—=ßV-­ŒŒ™à/ûQ¸Z“o&$üçù§¸(’(ÖWàæ’ğãà‘%‚<«ä†auª¥ò·6ü–›ULö.¹z]Ë"~ØvD˜IWóÜoªsLÚ½™^çŸãùÜ&6× ì¾îò}Re—»Ê÷Vl­ŒÍ$Ás«=Ÿ?m÷~bQpnUâ£/DûÛH›´+C,WeÜ±u¦¥÷øoª:,«˜ 6ÊÈ2]å)üî6—Éü0şgâûŞÑğ¸ÿödæİ¨C>¶Uâb²&Ï,’”@2/Ë&æ*—ìÑ%c?ïÙu¼Œ{ã›²²ñÅ:¼œÆ)9hÄ„™eD¤u™Å¬-EŸhí¼?W8¨÷Ÿ£xnRUû¢aã’°p~®ráB,32É×tFvƒ[wTÿµA
¥‡¸(;*©–-”í”Xğğ-Ï6ËK²(ÅØ¡¶ËZç"k—‹VEò‰¼¥ûÀ<ÍËxŠÛ´› 6´sgRÎ`Äk]L$¯cUOè7Z·,Ğ]™^]}Ôeß1¾ä‰¢[*Ş£\¦¤à±İ¶·Ú¬'ñœğ†/Z‰¢~u>/qCi3'ˆ	(Ğ«(<ÏŞ$(TËŞyöó5’L@Ær3Oc*pñ©ÿ-ÑB¯7E&oKœÉÿäß6ÔØÃ¼ˆ	ENåI	‚`™U²=«XJ‘í¹F.V†hÿo³(×@bÚ°dà6B¢ Š8¯şäd‰ÿ±æìß@{)zªÊ´îÅ¤òzfÔnU£	? ÑìTgİo/VªÒŠ¬´€Wn¹R­/Õ4oäx*¿NJÀ™8Qä TOÜÓ¥Á¿ÿ¯µó™_vôrŸ…*‘•8Iv%À­h(H§qXnŠ8ª´}O@ÂüdC!â¬!GçÑs•Vm’•étÕfZ«Å|™çifÁ<Ì€RŒ§xL$¨‹ }+<²~à¬xDNX•<“” T"xpXáaœ÷×İH ¬Eó¡bK³3Ù$^æŸbº£Pé¥“æÙU0§Jãz«A,Ï]ç´zGÊç+¼×ÌFçäÄ2<Î†°šÊ\ß»pÅ—›«“°\SPŸ5<µ#DX:©ÀŸ‹üŸqfÓŠÃY`I¡ªïñ°Å%¥QÅø¾w é<¥PÎUÎQ )!Ùë
 ³ LµO«ğ¨ÃKÜ£ãÏ%Ùø>65/„ú«û(‰à½´(LÕgv2zìàG†¦#°Àe#wâ %şHc3\ÖËòu²¸9
×át]avkªq°Ó¸N•	‘€knG“½‹Mv½Í®ÉŒEÂƒJV#H·Y:ZCúØ~ö‘¾¼4M]2mú…G•|š7ÅŠ!˜ÈôÑtz$ÛTó&^×nª2Bûn:9˜#[v©Pša´AÅi¥C<N²¤¼¶¯°&EñE³q\„Ë˜jszôŸqH^•Aº"K‘¾y§ qRÑ+t¤§ıÙàÍÅ¸?Íºr÷“şøbp~6ÃO¯‹ğ‘{‡ç³ÙùiğïêÅÉğ˜|ş±ìãÿT—®z—9ã–§aq•dÁ²"IÅ«P²I‘ËšgúW¹ÅNT©µ'©¸ö€ªQÆÄÿnğä	ÿÉh<]	PJÇÿ<#ìÿâé‰Ë ãOêÉJ>îV ·¯›ì#*© ÊSĞü
-É¨{>–bp€€6åÏäEïšHv¸¢»š_˜zúcö‰’[j±]NÃı{ö
ÚŒK¥ëÄZW&}åbã"ÿã†™Œøé€)‰pÊßj«géT­tlÂ¡,Ê³„|‘ø»¶T Ÿ“ù_¶ò:ÿ|”ÎÈy**A¨Ï&éÀÀkÀìá ”½ˆ½$Ç‘ã$K—P í@¹Rë'åI]»ãİ“YkgÀ¿öŒÛª:TóÁÅ 	šÔ–0 +Œ×aù6ƒM/ªÉd‘É•a /:|<¥ÏüRZP*P›#–}
QzÓp UÄ^RƒÌ´KZj*´R#G´V£•5…€L¯Ã"ÆE¼ˆ‰Ô?K²Úê¿‚şjEØ,ò|J/½°~ÃtH€­Îãe˜d„,’«Çİ e¸'/Æ“Ñ»şl(0¾,puÑ]Ø§ Ši©Îc,vÓruC\¥7È³Œs«ãÍ€¾"ƒ˜®·9¬_•§a†J<¥Ô j‡ĞmÂ£*YH5À8 Ó8ÛL7—H_D"¢ŠĞ».@Ù açjë@¯‚I¯ÄB=hùcîûWònÏøı()±ÑíJâ bÕ…Z·Æx·bËf@¿ö¥ª˜-÷“ıÓuLËuD^*.:î ƒ8™ºs@•ÊT"1—jğVšj7yx©å,~]Zsª’¾îİTT°ÜOe=Ö¿hÖ{úã¾"'©›@¤Ö<FÚù[İ?ë%şj¿Ã×U‘QläÂî|X}PqÈíj¬ZƒU–B€`¥vğÿàéwv>{3:{m8 Õ~ß,W³œ¹XÒ<Lqı¢©3)Ÿ—AÈßñy¬ÊH§U1—_%Çó­ìŸŒßô»¸jß ×JğP<,5‘)y·®Û:‘˜õ•·vi\K"Á<2Q¼X@•*JÇJy}~6ÜöĞY“’se‹.ª
ûåôsXNeKîp¹Zßüd“¿yaæ|¦ˆØó|Ùä-ÅYôq^|‹ÈÂ¥©æå„ÂfƒƒÁ¢È,_A÷“x¬Kiâ
ÖÖÍî(/á.¬“5Jb'ù<LãZíÕ{u’‘3s˜²¶g7+”ƒ Hûí“ò¬pu¶q†™Y.îÎïƒëĞdß…!Y&:h5Zœ£ÊTV1qÇm2-»t<z;Ü/€¶Gç²<ÿë~7°ù¨ûÓáÅùÛÙÅ½©®*‚)ÂÜÌÂ+jbs-Y±®Çùj³¢Û“4İ=b}´ÿß\æd.mÚ%±BIDøÌ¥oå8d›'+®,RXïø©¡üGI8$‹!‰KêZâû“$ûØÔ’èS N'Ç½y¤¡ÉS‡õöBù&Á-» ™CÎØˆ“×Úp¹ıUX”ICíÊ“ÇÁZjv€M„DäFÉ±oº%aF”şæsA”Sá˜{eH~ÚÕXS¥™1<h;$ÛÇ<6ûLR
je\– ¨Æ>u×IÙ“_>	:@`‘¨™—B³5Å®ö*Ø'¼Ó4ñ¨]¡#ˆQg$et¶Éf5 Ì˜ìVAN'Y4“ˆê(;¤C=ŠÿL‡ÚâK·ÕJ†‰‚J„=@ ul`'åÛ2.àaİ‡i>ÿHX5€ÜjÒyr ÂåÃ¶*àÅ»2­˜|¢šàâJÆD˜
›¼‡½S[Ğ„
É.8NÉ±+e+ûq%°<¶®¶n¿»©Úw¹…Ë¿¬“cŸíğ	“lËFı“ó×Ó‹Ù¯ãáÅûÑÑëáLYdï‡üh3aÌÂ‡«¤s¸IÒ¨÷n8™‚«Ãàüh8íîéòvX~$’à8.–IYæYY«»A|ä*6œpüÓ8ä×ı>)÷¸‡-ÛVÛQH)¡¼Vî3+šqó4"äÈß`ÑnÈÜÜï$õ¦G¿\º£îs£ÏÀDÎT‘Aä/:=r~Û¤Q¿üXÑì3ªb‘?¨hÂ¦ğìêê«ê¡<ª¿M¨dŠEıl¨Y²€/«ê]o|>]œÏFÇ£Aœ_¦dM]!Y>Ñ3»¥…^lÏi£Æ¢»ƒÎµé½ 2ë·Óx‹¨ôíË²é4™•ù¬p¹ Ä)¥dûÛ‡à_ Á—®Qš@htffbÔ¦’«Ö‹5:­Q$«#Á*J·òEB¤ù^y“ÍÙ¾^â­òcÿ’œ¤wohM LwÑ@“aÿ­ÑıÁlŠ§«q8ÿHvh®ı'§£)2º×“şÙlxÔj)âA™-kd	q\²è×r fŞœ[Óùp+uöxûÊÒsÀj<²^şÑE)»^§ùe˜Šì£¬è¯G$v"ª‚cweÚP{&»®új¯Ü2±:^j{†í´jrİßJAAÃ;ÃsçÎh\¦©ŸÚV?Í†·¨Oöÿ‹ş`pşölf_\íÉv=¡ıº:ÙäbtÚ=ÜrmµÁ¸Ô›k
o; w££áù}‡v¶óáP"şc6œœõO.¦³ó	ÁÛÊÒ¥ÕÄPË½j%ÇËg?‚9?¿#G­ŞÙùÅtp>Õ#ÚfWØ%v|§»1»@°Ì„­GB¤­ò#øMTL¶ãí$C9İàÏ`ÕÇx+®Õ‘û“]’¯!pÀ®Õ:G!^˜$'uq£jöbv Q_²+Î¯ƒÎğy¼Bßíä*ËÉoG>DIæ!Í…nUGI	½v$ß]±I~X.®yXzŸU´½Rô"b»ç^Š|5œÑ¬V“¦İ…(É~F%BfğåîÅÒK8ÁÑ&´ë×5"ãì–¶±I…z›mÆ6Ño—›t}ÌçèWÅ¨s€±NªPÏO‰wÈ+<^ÑJÕĞ%4‚^@Ú˜R„.š®¤ /tÏÊÓíÆ‹Œ—æ›!ê›òs‚KO é·ğƒµål%~ç‘ÖC™:$¼¶å~)>’€-Kà£¬\ƒsEgÎ#ÍQqˆ§R©óŠ¸³S#AïI¸–¯j½X¿8.JX'F¢ïN¼®	±6ˆ3DQo•—Òô/ÔKöĞçn´\åEu<BŒMWÍvÊKºoC{·&‹û;rY¡é„ˆ ï”ÖÌ‚ÓıİhI*2WEqñïQÁmdãp}íÕ¶¨qi®¿ùO8([Ö_T(jõrúš*†LÓÎfØª¼·6˜şmÏMˆ8\Ç]Ëmç¯nŞ¥—6)õ'P=¦O“+*/MãâS2{éí!ŞÅ˜^Ç±órTË¦zK|Ÿ§Ñ1Ê±`uÊ=}Á
QÄàJÌo·3{G‡ÿ®îÕ®÷Dd\O•™xYãÅç­\êÉĞÀé‰›«N”[uuüC6ë;3p‘lBÿ:$Ë,Šâ¬_Ì¯“O,ÈUU¥
;£DæªêW%CZLf*TÃºgônxA8é)á }ÂPßŒ†g*. œIXP§¶V‚Túut…öÌĞJeTúÚíÑxE³|œ—x%è}²¾f·ÀŸvYL9[»/n\ó9JJõÖ#è'Ì­jú}"oÔı¿6=‰¦*fr#“›a—d‚d/z€ù Šëì#ÑüÕ´ü«&Ü×ù¬r‰©¾M=¬î¥<,2"ƒ9•¬ã§Oß9¾iŠSì‚ö (Ê!ˆ€µ£#Ùñueß¡mao6"â‹¹(NòÏeÇ Ö½Áª=íO¹8¾¿8N§ºê®hïa¦gçScr‘ÖõL:'#ÜUpxç¬Yióªn‚‡Bµ¸UT¢ª®zqGWÇÀƒÂ',6M+Ö§8åÊqLÆa&„ô`L–Æ3¹»_HN	¤VCî¦†¤9zW¬L7«àâ}Xdxy¼Éú8z¼× {‰’‘½-Ï5ô"[5ßİ€¹&õzAX\iªŒÀpàcä,I_ •˜ô­ÕÅgé‚Ÿz¹ØDÍÍÖ«[E¡Ngšà$p …uVeà5tqRùŸiÏœ.í¦h\¾ù;àÇUàGkÌŞ±Í™Lv’¡àşæì
.%Àôƒ8“òmjÿ)H‚–i {‹4¼*ƒÿ0b=€.G'ä{q|ÒM³ƒ“·GÃP£ìY¿èFB˜GJŒıé‡c#3e ÙPøÃda
ş:¼ä„Ê¤î`ç¤ œc×éM@oá–\©¼(òe@™]BçÁú§rP¥¯=Ş2¸ƒù4P`=s]%ø,ÚÚö€€8%â_Ë;¸ê£´†<~±x[Ä‡ìOÿfS)t{ä5Vï¡-‘(ç\(úµVÁiÏ¶¬óU2/’5>Ú
Óe¶ı½vÌ¯Ãõ([ä¬µ îc@À×`Îÿ Ç/ùÓÎ«€»İ‚	o£—FUšõÃ‹ëd‚s3àÅÇqW·™m¤d8qŠşõ&*Ğ#¯]IeÑ¸¶İ/ùÓ´Ú\÷8ZÜ¿ò $†7º°åÂN¸&	æ15D~ÊPuôXSòji¾?·@Ç˜ìÁïòu\ºñrûå,õ:‰©kô÷Jvìß3‡2®3ãœy…W~ò$±j°’´¸˜üô$­¨D¯©> vUÓãrŒ‰Øôe0
«y“?4¤	Ô"j-õí©QÄĞú-Qa…† îâÓ60‘?ÇçVá“ñ‚×%JÀa’ÑkÕ¸ „¦1k·¾…Ğ°‡7èÙÀBóİkä^H1r·¼9y€uÆrĞ§CÚ^X„óX3ÁĞá.ÃÄ¢ûm!töõª¢]í(x BØŒ)Q†hv²âª0k¶’[‰–ª 6Ã“'ôƒ7ıÆøUê¿ëÏú“ÖÕ ¯[Õ=ëŸ:pO…÷…5XETÚ|ŒõNê34ÙW-ÙòUş§ph¿ÅËŸy®ëç…PW­ÀCClqPj·ñn+Æ¶ 8°ù¼º§QöU5Æe^¬Ï³”ğôÊÁî¯İÃnÓ­Ãõ¦ìÔ$C{Èİkj[”4\Ù÷á•áj…G¯«|£İà'¬~“½{>lwŒ³yq³‚À„èh@Ù»8T²‰±*ïZ)}°Æ”âpÈÊåç®…"7'×bWJvzô4İËUj•z-¶ï:“`S°l«sã
ş÷¢^7F÷kƒù…ò¡y°©ZR¦s£Î`j”¯˜Õ.H4”Ø¾šr*Ï…XRKÎ¦„ÀåS¬Ñªy¦oÅã´€ÙIÉÎ‰tC@‹æótµí:ı4¥î	¥˜p‚Tûù xjå….>è@×©¬3áÏwÛş#Mª-™Æäf-rÃ>Œ&_ÅåeMƒÆjÀ,âOC-r‚ğ§
Ç¨™G¨ŸYÔGÂ=ÿÀpĞ9É!XšQ
—*¡Vr¤VQC{ŠcGĞš¢Ò°tjy”nFü#tOªº×J±/MG][ÛÖ«o–Š5õ,'á&¿J;Œšì}ZádùÊJğ8e±x}.uÕ±€İRxmí‹03vk)­d&Õ;“XpnFİÁG}ü+€K9={Ô°Ã¢È‹æa4â÷ìˆzlµVHFS0õ—åÒ*pˆñ<ysn?¯‰¬Læ4‹Ğ‘˜cÈ´3Ø²İ`«æ|E.aÑ+ßQ*Ou›Í°ˆWéŸG‹èİzâ™Vãb6üÇ¬åtPÓ:§}È÷‘ŸÚªüYÖV%ª6zÈ–A}"vœã§KûIS‹¹ÚQŞ£1âŞkäJğWš ƒÇû•ÖĞy¹®>¤ÏÜ§/	/;Râ2{k0í ´Ì_Æ´+Â^0,«J^D–ã±ˆ‰¨¦}®õÜ©TrÓ)Àèb"2ƒŸÑ¾&¦t½•à+È‰šÌ§Š‰Ô(ú`SR7İú•EN¬Éhì•mË–‹¼Eü)ÿ›¿]æ4¶“>÷šŸü†Tfü@v§{Ş‹.w?W`¶Š|U(İ RNÉ<1x!±T?IOj5
³—@9˜¦MJB¥£Œj2aÖ\¸í”	İáô†¹(‚¶“âîoëd¬ÂtNKÂ÷uï#=jıÔ $°ØÓ„éÑˆÜEDòA4¦İñ}Ç¨t­ø›ß6³{	Ç1ù[´Æ‡ó‰ãgëÅÉÑ‚*Zèmuî&«ò[ù§=‰F…–æ…¼è#iaçÅfé^UJŞ	5`ØÀ1åcäåeÑYî5°h5}0FôÊ–Íì…öádØ7ì*üe7´-·	|·ÿÙÜX»˜’Õa<Ï—1šIÆ[P­ŠÛ¼©^À‡J`²ä¯ìÖDÈèS]ùŞ¯
»iç<ùW6L&^maò vĞ†ƒ¶ãêŸŸ„ôyº¹"ü%æş§0ÁÌ:¦^Îâ?ÖàCPÓº’ŠQ:~v¤4İFDæ3\’øfĞ-¶‹¾&áÎßjáar0Î¤LE¼c‹b+Ó<.ó$šÇeùÈ)³Z9vCS;Ş÷[,Eû“Š÷F}6=_ŒJ@
åë–©C2¶ô2X†ÅÇ>ïï«À¸ô²å0M£I’Å©—jX=&)#ïTmuÍŞö¨> ’v“yêªéû³»Ô«J BÛÜàaüL«xy–…!a’bk3/sT˜bœˆ–Ÿ¦áªÔÉ–‡è`ŞU=ÕymX{PşR:©€bLeÕÛXø	Ş½sˆ¼‘†ª¡7FE9X|Oï”–uÀô!‚îÎ‰İZ@¸¹:½A©àŒB¶…ùÇ]3¹Å‘šîÏ·œPÙ1ª¡|<¼û–§Š‰­Œê~¹-h F1¯JéTiîDELÃÑAnßuøTA$ñ	Ao©ˆñl5< œ#Åã3«w%âŒİ eì6f‡c ¼’.;hóE GÉº!ìIyƒïïïñİk²…7§ûøNLA¢>f³š#¶`şNa'	1L—±Kß¬ğPjªÍ©Æ&íX¯ª
˜&C®³è@ÿ–TQ­Úi‘G}ìÙÄÅçÖÙzÔ=Î«aU1ÓüæE|:(z¿.òÍj/0¶¸'fº¬J4¸zÃÓœ+×Ê]Œ7xüÚşÂW#áÊêÛ—×çÓ¿6­E+ŒyIv{]¦Şu¨İh5‰om¥4P·á‚uktï™ç£®³ßÿ°ÕQ^ûG	Í%›%íÁiÃÙ•û¥Øb[Æy	Ç¿¶¬gjCÀÛyiÂ¦"~U|Ãd vñÈ¦î„fÆIVù[”•"	U½Ú-¿Û€pÙ„jPÍ¢Õlf†Š}Å(ıÜêJûJ\b8PŠg6ér´{Âx)\¿nc¸ŞWÕ·º1.Áñ}ğw‡S•~¼~KÜrË’?ÑVÄÇ<í..òæ#ˆh–tœ?{¾×È*§0ş.Àh½@ÿŞ(±ñ{döæúëÎï×Øi#	cRù‚%à\“hR—d†’)›Àöİ2Bí$HI¼n/or4§õ‘?B]ŒùnÏ0¦>ÀŸ»»ç³pJºİ5ìôXFp¶ú–pÕ$3î ·ñxcà°Ü¹æMeûô†fc.–qÔß¬¯]YÔø~·éPù2wbø}€{¦°ÎXNo?(>İ\–ó"¡Ñ…]ÁnV¸ZQì®ÆÒk”êç´U/ßÑÿã?R„?‡<-t€aÅ\byAşíĞXºRµ¾)›G‹ªfÉ°I_m¡&Í6fä÷–S¹„–˜$ÿÖv/[Rj«™LÁÖ mß³Ç;ÄÃW·óÂ*P­¨fP°øí&2ğºeã~¡ã@ Fgÿn?*;]æYİÒŞŒÓÖÙ¬l¨cšå-û]}è±™Ğ"î=lª–$½Êi ¢åqÿíìœÇ”Ÿßô'{ñoÈºrM¯;ßêôC{e«ä¹cìñ).ß$Q|Ÿ‘»Åª¦Ù4vôGñ*çñ,$Áíz&³¦y&
=ŸÅW!0hæ_éÓÇ Uy{ÚaTi™:F¶jùu>Ë¹iŸÉ…İàóuÎŞFï6ˆûgµÙ¦zD8¯îÇ['	GE9Ê[$ò‘d@ê3†aòIÍñªf'2©iˆº<—‘c(MQF \åª¹û,l@&Ö¤ù`(ëÏÌ™*¬ni¢Ÿ	Rœå f„G¬“Wîù÷G­U§Öæ eéLP´*Çá¦¬¡‘\ú*¼I6.b8K®48Ò`ª`Ru
™Ç%Ğcõ÷3Šúi˜Ôiå„¹F‹eİ
T¨Â©S¸¯LlI}&ü5„j—ÚF…H‡©tec‹ l-=á|=àlod¯SÚh|¼)„*ôO†Ü}“‚kâ Œ¦ÈôlVû°Û$»Z“«pºÊ‹¹ÀŸB$jôJQ×¦ßıjÔoZ;¢Q£MÓT‡¾ 7¤É¬VbIóîÈúÒ®5Fc7¨u›Œ–µ³…4’¤RÉ€V¿.m‚Lğ0Ò'Œ!I^¬è[PÖé§MtUéjÂ–dÀ•&b7W†tlÉ¹bvqj{Ù¿åöò²ÇÚ±â˜É` ‡¨…k.äŸy–eÀš1ä\ˆŒDN%»µÀ²ÕıÉÔù¿ßûGÃ	Í¿{ú+„49;Lå«£•_OÎß¡êÓvU«zÏ´zPô‹MÏ¾K8@-åš”SÉ]<êfà¾)¶fUÄRªâ£áqÿíÉ¬F“W­ÃóÙÅôM2DAüQªr‡ãÄFƒuNÓpŸÉn@€p"àŞü÷&.n ï<İDô€â1$®ÂeìCÿèèâí¶’Ù9¢À@I0xœŞpW9ö¡*ı[?>Ÿ¼ïOHÃÏ[à…Âs~vò+©ø—9¹òºmQ—R¯ù·5ÏOí>%µşŞºÖ3RëÇ6$sr>ø…Ôù¿-êĞ<Ú@cmˆst:>ŸÌ.ŞŒ …Œ¸˜)¥RJ B~z³u›8ÏĞ$¥:° ß®Eh«¡Mg}R¹?›õo`5Bı6ôx2ü¯·Ãéìb<N ~šC@¯„¿NFÇ8€¿)ª­Z,yùK`(ª3ÇjıçYñAÂÖ‚*ä |ª ï$ùíª•ŒcßÑjù-~š”ãT·éåMâ~	–ú+÷åU5™åh&KGŠÖ´XÆ¥ÃWúJÓ- Aíê•­ñZx6–’W‹?ˆÕÅd¸ÑNhkøÄmFÓsYÈ“‡ßÖ¥s;ˆƒ0ëGœ‹Ë^™ü´æO#:½°ıÍ¯4º+	eŸNzmz{Is"„išæßLÂ2k Ó¥¢¤$çH¿zÎ¤(ší§©äW/üÑ'~ÙñĞéuË3È¶¶$_ZïJ±]bÎÒ‘´iUeÅœ“•æˆI&§ìßÆV:W0¥Ÿ/¼„ãuÇmAi9 ‰H7Ù‡~¹-Nã«gˆ_†íA~…©¶hì‹?¢#Ôô€Â¦¨{DåÜ#”.°œöê8Cæ­ÉI`Ûõa_âÃIÇ‘Ê»#ı{'Ù^qÙ§*å·±sIëğ8/>‡EÔº?áDÓj‹¦[ƒ‰Ël«}Z:Aµî—¯Ù­º`íÄH¹-´tÅûÂúwPZ6–&Ÿ;Ñ²`f—F§;”Wdë|˜.É=4ÅÓh/
HÒE¯	=e•çêS@Gb]H¶šZ±jóüÏàÛ°%Ì‡ow•–¨aUè§É†
êñ×¤ãUN™.óCv.µÏ“Ú¸.é«¶Ø:+*ØàW¸ŠZáD_1ØrLÀş:Ì×õìÆz§R–ƒÈ¢ôàÊn™JŒJc^èHÙcS˜âc<Â Î\:á—W7è:2€ÑWJa–Û´
ˆÍ8)I})>Úô
	Fæx% ÷EP£}O‡~À5BÚŠSPRhˆ
;‘1*â`¢!DÇß'D$œßÔ#¾¬äÄro÷¤SoUnÚ£™i‡qZ'ÇG ÇØ!:-Ù„i,mó
³œ1{2›÷G 5KÖgšÏj=Óÿ;Ñ;èºUu=r6o˜õØ¨Typ¦RowNC¢~/T$‘6'¸ßÓf'Ì’ä½ædz¬.>y!hU±ÈìÖd•„ZH~¢yáöŠSNAà€Ú)o°„õ‹ìàÚ™ Æ"Áñ˜¨@)Cïîé3M `Ú’7ay}®~†0]â—Ì^äZËa9Ñv,»5…d«µGö•µŒãõ¤Æü™1¸ô”L4r]¡ß•êEÁZ=ÈtzŸÂ”€h¿ĞbHíÈ¾ˆÂ²ç4%RùK|#‰·Â‰ÂFé
©ˆ“öï¼”Û[Å•ûmUŞÑÛM5ñš+cÎÆäªáYn·‚Ï«÷²mQõ°ÈÃhšõœn. ½¾ÄıÆÎĞ“Á4-6ŞYæ›b.H·ÅÆ+/!Ï{oÅÆ¤1øK MÉˆ¾¿-Ç"{L7hŒ»yWl«êJ4^lK“Û°­ï¡¶ßòÃÌm™—•){†ÇÎúx3¶Æ™H™ CÃ²Ğ6µ´ÆË’5\k_ šxö÷Mì„iû™D¾SJX„BËmÙ*ò€HA.İíêøÉ6©Pyù“:†EÆ°"_V6M  ½ ³ò®¨jÑ¬Õ:0`Ô	^Îâ»:²sc+ìb§Ö’ğ[c¿÷ªæv2ÍÈ¨~*N»uj>j¤¿â`«Í¹’7œ˜V*Ñû-y!]â°•!ßpõ³ßî)U!ø½XÕ¡ªeIÛ”4êõ „øwˆ@ğİ¢šåôÚY.FÉn3„ë°¼ 
ÌÂe,ÂßTNÜšàú®½eØœsëC;ˆÛØJUÀ¹M(ã‡Ñ2É.Šäêz-]n°MÑ2„Ø„eªM°›&Sèn9 Lcÿ-áÂÚøÈ1šz0Ó!6V²°ğ±ÊÇEŠû&‰¢ØxGø:‰âc©lå•ŸTFoš&àcM¯*S/p)Q• 1ÍåF¾‚EÀ7’ƒÁ_-yêô{.ÕŸ¦‘“¡õº<‹,‚*°xÆûÉ„©İˆ1ÛŒ×’y¡4Óœ3Ë³ØK^meº0yó#Î‚+5ñŸa'›‹~24…TeLH;Á˜`*YI­VØ,c66DŸhÂÜşAğN<Uù	B†¦Ë –"–=û«MÃP	^µë¸Xå)üî6—Éü0şgâûwmjHÆo«h?.¯
T×ƒ´¬ß{u8;ÌgH'&ÂË·İ,£Ã²fZëÖ2ËRëóaM?ğ(±§]§]©âW–üğ&Ÿ>{lÄöûødÉënòßÃ;aâaY¯HoYU	f>í/aÊ%¬Ú…§Ÿ|<µF&®ãsÒYhòq³>ËS¼3}””¦Ú$"úB¦Å\#,èµ8–)HÓ%†¡Ö?í	óKcù
U†Gšéé`spÕ=ÈteMŒX2Ê¢dë!“T6„Ë š¼¤4yinu©Ó¢|Õğòƒ–ı‘’]—†å­ÔKjT²ÛvF,ã$Sİf¿|÷ÃâïfCŒÿ}ÒË†û¤?)}‹îÂR+Öõ®4`
ÎüY<p¹<<Æp]æ^	¯µ>ù´6ëÒ+ÁS—iÓenÁì¼ìÀÔ{à»\÷ªÄó²õWúÄec.OMuæ2*ıUı ÇÏ¢øÚ+ª‡¿Ïµ*Õ"­ú3uNß1tøJ@\uáuÒ®À©­É¼\¢=!8 š{¡‰ë‰Ã ‹'^äz)(a¼šë×x}uŠÉ‘Êj¹iX¸íŞ~ì'ÃóÉXŒ0/‹;b†=§ŒaNHã9ùGçĞFEf™¦è„z´]ä	 ó33Néíx[FÆWZ®ú»‚ê…¡GÁğå•HÁ07l^€šç5+4öËG›
wá`~¥6:›DdŞ´äÉµÁ`•²LA%ç† ’™l«6Aÿ ¿Q°¯Âd;œœ};ñ\TgÃ÷<Ñ(ÎÅ> „U,ÙL±^›0ìbcÍƒÓ¨W8.eé@,İ,¨}K’ÔL(Ú"f§)7¦“LiŒÃ˜oÕeyûÈ -­âŞ¶¬Zâ ôòlç1*[bü˜Ã|si,DòX"Q› Ö6"ŸÀ¥vòŸ¶H9Hˆ¼‚«¾7át	zpéu}÷ÛÁ›şŒõsK[f‹ÑÉÙÄ½FÉ{¶°d-ÂMÊ$28wtĞd­…FJ sl[Bº¸ĞJ›²Û-¹&<ÃêõÏÊ®¡ÖMe>ˆç¨AĞ(fB^%¡ùº-+WĞ½8¬-—<”Šwœbú¨«Eiªò‚0Lô9Ù±?øş±‰¿óÛmĞ¼âØk:ºVŞéÃÅØÙ¸ÈIÓMÇÕ«V°ÚtM8ö•Î;[Äİ‚{O«İ©­X¬MzÌ±u¤‡ ùAı‹Ì Ş’c+‘‚ÅHM¹…7ş˜o´=£ºM|jiÎŞ7‘ë.}b²?Q	v›f!?Z¶Ø´ùsëhæ=18\\¿ÇBëñ÷\£¾Çñ²¶Ò.{Øx“ªB.Dü-ãë|ÉÏ›—ALş!ÛyL®Ål³dVvss®LQlYQÈf¯ê•ê<–5dC^İp¶« Œ°nRJ$ÙG­yjl[6%6×ëp~ˆœå+Œá¨`C€Î$LğĞÎ	«ƒ+FlÄ¦2M'Œ†‡Şi«©"åÅi"?›¦Èˆ»ú3DÜÆ˜ß°ıì±¶	êŒY›õ ›ovbaĞi2!K]«ù…
’ÿùÍtP=£.bÓ(T†ş¼VßŸe_¹ë°¬çò¼À¥jpó¨,›D„Èt£u'"ÒŞ¡„nÑsYÿ\³Sÿ~{É•´µ‚ÎÔ©´ä¶Ù$™½44
‚«hõ[V“8¥ËoORÉÊ PÛĞûøuvì…u`Œ*Ee´ğ­·˜¹nÇŸø¤d¥Yg–š¤†…Ú8I©6d´\HS`Z7&¿*ÃmÁÑÄùkmX¦©tNS“¾È¢Ô·5_Ûº_ïFæ[•A÷k	O†ˆıèõ7‰éErÜû”@05X4¹%÷úXùÌ|2‡˜G/3_ÙC5Œø%JÀŸ§1*¼Ì9ê©&²‚‚€êü±¶Ä¾·Rƒ)q¯±QDkïôühxqv~æ0“Ôì\Èø|X´¢N%’ØDïÛ²s¿81*¶ÂåXÚ¤ˆènğ—•ZÇE¸ŒYşgú!®pYé
N¶Ï{:¿¾jÎ&«zF†ÅU’á…™×	À
©•ÍdË:JWšÕ–Ï²•^íË­§T·m˜C/ê[¹_¯(˜;…O®ÎT	Á¯CÜŞÌò­ø}%@×[Ë
×Ö=ª7X»lö¥.Ïf‹‰5lÂĞ« J¹Ñ*ïEıkh§’ (®Ş&zóœ•À¡ğÔİJôOu¡ Ÿ£&?şcãÌâ`v]€‡e:™Äód•à%¦œ5¤à³ÒÆ‘iw7¯ìØæOeÒ*‘
jD*+N–íÉ Y"¨¤p³,ÌÜÜÕ=y7Œçh«èzhÀÏH´%Ñ¶$‘ªTÎ2[Á@o®~Pcè€;†K¦:ëTó†«YÕŸØ<@I—{eïÈsÜ›E${‹„‰0Pó¢Ÿ¦8
Í[ß^nªl"¸ú,Eo8¼VĞº)FƒsO¢­±UıÛÊ½b~”¿7Ğ*øk`—%dûŸØŠf¶c Ö ~%8 F½¦ôİg\¬‹P‰­à^Ô§9½9; ‰-!™o†"W/¼†œ?P´Ã©ìóRüÅ¯³'iûìBøÑ­İê„l&1¦Èù„29Oó2ä®ºŒHçí0œÜ¬FKÒyå\íyzœ]o–—Lï®rN§¢O*G¶”­Î%åyqz„WÏÎ=ŸèA–"f”½'°äŸ;´ª)Ô¶ß4[ùåï¬Û†’&©ÜƒYø2@€CËª+ûµ.¦FOäÅhb,vX§Á\<Ù$ûZœ|Â+Èˆ¢‘øÒö¦”ÔĞZÁôHÉz®¦áÂÙª” #ñ§ğã¶¢E%qn?ü+ˆV¿A¾.ıŸà‹Ş
##Ò˜r
üÎÕJHéÑ¾Aïæ€ø®ìÃ.E†´OB[&êP§Ûì§ØÑ˜rZv®ĞÑÀGŠ½¶–‚hŒQõ»*—´…­â•a6…»¨›46‚g0G‹F=3œ4Å ¿°ÄÛ‘>—O-RİfóâfUUíö{çM9âiLã,‡ITga‹§±ÁİXƒê¾·H¹4lË·Ÿ&ë™ævüÎğ#eŠÁ·£åìÒAˆÑ  Í\íäÍ)ç0 ùÃÿ²;"Ué>	{î8yëí^¾ÒJ°Î×aj>7Ùbá£İŒV;°ß%cı;ôÖ£Æ;‚Ë\ÒD Rœ¾~¡¾Æ®Q)ˆ°“!ƒ”˜ã.‘¯QÚ®~[È­p"öb>Â=˜'O~¢Š«òÊgLè„Xé@`{úÉ`–!é7E˜²yxŒ ä£xî€ü0½–©Ñµ}W8|WW[µ:ZÇK[£·Y²ÓÍåÚ{ÕÒ»?/hN~ÈºÀjû‰¶h75±8Wšy¥[yæ›­”HT±=Â˜a>‹è}xr+Ë8êÇ=(˜âh”-rJøõo5»%•<K&×…ëX~3‰W1!ƒ¸Hr1wÜû?ÊçTMš.¼ßds®¯(òe¥ô4“ºIŠµÍÂÎtåö™“4šføÇbz¤ç8 8•<¢ØÃíAÀ:4›Çhh†dåF%Y½í’Š'ŠW³2ØD›7wcMiDÒ ]Â…h|ç2b^€O1™®dşK|ó’0ú—G¤ÏªqÃÅpClµ]çOÕ;Æ°²AÙËš°Hó¿t	övı„Ç¡Kó2¦G·ÁN%="¥QÕƒKõàëQ5c5Î®C—Z+¼#™€™’óÇfíÔ1sK¬´æ—È'Ğ´ñp`ĞS3BpvXë 6ŒÑ™x;<¸™³…eîáBÄ«ÑŠ5ë'’™İ†®w+”Á#êØü¾KJuvmMv"øÊ¶¡-ÆÖ‡,áÜáH¢æ¹îÁ"{„XMi¥6zü‚VjÖTóp¥øŠèS33¹‡°ı¢È¯p~/Á_¨s(Ë°Û+X’Òš£gf¡ùË}PA¨şÒö§Uî~dJªp!öëìÒ0‰áªùµQA¤ûš¸u®b†¡'™s·*ñµñ.k;•UÔ®µ¨›#/ÂğÚ9³=¶lQĞÅÁ1v[š°ĞmxÆ²vêNºˆÚ·!üWºD“Påô]ªû²L hL’ôfŠ3ŒË§°gÔa¾ĞÊN¯óÂ^¡EÈxÜ§l9üœMC¢D×[ç¬˜Ø·)µİ9~–Ÿ†ÙÍcÍ•¡ÑáÑ`;à5{IšUjş
·‡pe°î{’7‚ºûIñ¹è½86=ì
¥q¼¥P
àÆ?\Í¨O“©õDÿ$v…ç@C™Úÿ\|k‚¡Ş9NÃ„¥EÆ[¥Wàú2‘àê*õÚìr”}R>ùdôwæ‰/¸[oªMFİg´væüã(úrS°7‚ö™^IÖšĞ“ÍyÏ 6‰Êdò‡{¶hr,h\Ã•á6éA˜½/ˆ,Ñ¡K†m8ì"³² =råi)Æ!”‹_5)ï™y©;ãS¢Ï)FÜí“Å¾–©]âxMQ»K°ßÊÒÂ0bÚ ·Mo¼*îz:"Œ×»ä	‚0ÌÂ³uµkT™z”IlìU™=Û+T¿Ëq¢â™òû¹U@ìS¼õ¸ÈsÉşeŠ½„É«Ü¢k`m/F+ŸÈAPÃ¦+Ùg»õ(‡Ö´(³<ÊÉL¬Yî «R©ö°§á ¢¡‹¡°v£È#cVµÆMèúö²!°˜¹>œÿ²çÔqï+c¼#Œ™[)µ%T˜îF›>çÎ°ğ9mğø>ä÷(#;È<6.Ğ»\œw¶0}ånä4&ÓŠ|Úq4Ar2ÙvIÊ‹Ñ¤ÀhÈ5ìx1¶[ˆöE¨^˜J×r†A4«ñ“¸Ü¤k“äÔ¥?Q?lg›2ÆõÕUìL+¸M"]±Wú¨®Ú¬ÛÂÚºîên€¶“*¶8­^ıTeÈG²Ôi¬Ü÷Ø<DJcÒmOqÔ”Õ|«ª&)@h|´\UYúìª4ÉÂ£ä3ùÉğ±’JÒàpù9¤İVéâŒ.’íşÀ0#Èr³Ş„é‹LmÕ#E¾Í"TşUf±_6ó"Úfyè1¼,ŞƒyŸ¬¯i”2auò{ıÁlt~ÆÉáŒœoNßÎŞöOœ"†Y¬ñ3®Yw6aftïèÀ"3È¶¨¦UØSÏ"ä´B¯ZÂ;Ğb»s:}“Œ,§ºt::»»™–õÄÔş^KmäœŸ_oÈ¬ˆuæ¼¢Ü×?’ñĞ¤ëãÑÙÙp²gb.³“^ì¹Ú›“PÎÙ3 “†šçª¡Š:¤.ËA¥£lµ¡Õ-”J“'dÔ€PVAÉ{àOÃ	Ab‘.¤)W¤HÜâ¢€„8ß¿Ô#Ë›ì<{;š]Á#êt,·ƒAÓh&4yzQR.“Òè#ó%˜C®’ 3ücSU£¦¼àõ1¿êÅæë»ğ˜¶"‚gË…f[¤ÈiZH–Ù#‰”Rr­Æ`’¡P˜×Rr9 ùŒ˜h(¿³KYaq…~L¥ „>N°0Gc¾Œ¶HŠå~,•&À#ôb™{ş”Røv¼½!’¥7‘çZ8ÒÀ5_j.guwÁ¶Zz¥@ß”!Öâên\OÔkèøÎu"’Éƒ,Ä9AÇ–ºfìAĞèw°€ Æ%LB/tâc¸öVy)}@ƒ’^²w-Q8ã,(jWÎ‘?Q¦Ô¸G¨“gfE
Â“Uœ†7qÔy¾¯İ›Ğ9Qr•å²#ÛÉª>.óSª-¨ÍITÔ>ÕÿÊGÔêŠQiÌºŞ:İ–.u·¸!O³ğ}Wš6t…5~aüÑøíOØ3)àÇ€¯ZÉ®â®ªxêº¥òrÿê×Q$  wûtÕ¨#gÙ‹-B+mÉ½ù;¶á„À¶Rzé°Y65(Ú§d¯Jc}9uÅ'È~hÊi“òSN‹1hÂ¦Ãô=\Ö2G·tHØílì`F[uı jòØÕ„	“æãÚ§‰·AV×šüµ½ aQÂc[˜8¤o„$Z/Rxv6ï~Úƒİ7i3»b»²³#izÆ¦7 Ã.ÊÁ`ZN§ö¾:“`ÆM“©zÓª•Ş’Sz„¯;,Õr¥Èu,Ú•OTãjÜO‚Çä¿'´uÛøÍ¨T|9nYkü·Ì®ãºİ­9#ğİa»Zdd­ıØ¾:~Ä¶¨#ÇI¿4[âÚ Á¶¥+jâDf˜…ähº4•…fh¦¿ÊRU´[É%pb[»A‚'K)©«ÚGìÀla6`–M³éŒ©w"®åQmgú‰A0´EÌLáàÛQ}†td7­EÄ-¼‰Ôn'›CCâ­8P"9¢ô©·­¬î+;×½´Ô»ğ}aG÷n©yQ¨Õ¾Y±]ÜµY	ÑÃÍ6	x»“‹—’í5)¯ëôÆÌæ'3¡Êñ0_SÛa&ÓfÂjLGí¥*åéê=Dl¡CªU‚°5SÒf¨&'¬UpóÊ~Í©‰ñ3Ë54¼ÂEÿO‹ÃEõU“¾4‹@½îÒdZCTóÛv3võê$éÄú³Y”–H}óÈ|Qí³¿İ5¥‘Å[‡Øù ×
 •ÑëŠ8>¼[¼;şË¨ÉâkãÄŒƒÕ‹72bÏ#¢›ùÚÒRH
tòT4İšGç…4­¸8Â]°Óª1AÒ†íPñB?xËhÏÊÂ^ræ{!ı"ş5ßL7ôò0	Ä¶¡*áòA­I°&Úk ’KÖu´ÕyÍ	‹²ÖâS¿İ¨Pw5î”(§÷Pà»|Âóa(WäéÖãîµé8 ÷JOb¿ÕÙ@èÙ¾ÃƒÚ¬Õ*×ÖhÚÂáqäÌÚ«%U×…$ÿ¨ôVşüC–tk¡Lvš«úI£&?²jÎ€À¯B<SÇ˜o‘%Õ
¸?1ÿñd·ŞæñÇ$¡‰bÌ“U˜I	z›ŠÂ}m'åÜ´Öƒ’]G0šàßÿö†Õ³é˜ÔØğ^ğÊ»shÿ¿ìÿe„+:i£õØšÏ€•ŞÄvõ¹Åö£ÈM¡"-ï/Ì	;dúK¸y}ø3)btéÁÄíÿ3~¶ùl[1b_RŸ=¸CüøñÿÊß¾<Á·é<¼äÙ&(éĞ¨«JÅFÇùòAU¯î$½9½)ãt¡{İ"j!¿Û„Gzò$Q±-]ˆ0U½)ò‘®ğà†8V	ZÑšmõÚô¸ÆÏûòŠpªS‘Œ6çíx¶2sªš·Êº)ô’½ëû—k»ËsØÕ+ï:eãûá@ˆËE$ë-Ã1û›9G"¹K£eÒ‹Šğ3ˆÏ½Ün/¨ã"_,0Ê½Éü~Ôùf=Í7”YÙf¥àe?TíÖxåGĞ"tš¿•ùÎ‚œµpf¶$ŞMh³¯: @é¿òØ0G‹°r`§ªètY^]Àö–EañÌÓ	‡µÃo¢Ú‰Öäo<F5&ûGåşdô½WWë«ffJïŸvë_Õ¿¡gòÕ¶ÌQ€øc¼D­w¶&éÚ÷w–°±%¡Eñµ½å± ş¸Ü™à±‹6wyF_„ç#0ºQôU32;ø_üÒê©œà«·Ö;­Lä]¤yˆaKğä5MËÇİĞµ¨>…Pˆ¢o@s¦Ç…¤w°ƒbµµÀéÃU²¹@>
aí¯£EfsZÖĞÙ596Åå¼HP
zIÓ<È/e|¨_{ê‹*«6¤“?ªí-¦­Ä”÷ü¹5å¹–´=%˜e¥KgàÉâÆ@3¾³Ñ){vzy&Ä@ÄS†Ô[şæ±ninZ©Ñ¨ÎºVuX`4HkÁ_ıvù¡—rÚòb4dR´áÁåÖi±E›„`æ,®:´]å¹ÇèáN‘3¤taäO;Ga{µ£şº:üBh„EòËY·ş„Æ*‹Ì:Cqlr¯GÀié=*\Õd£ÁÀ8Jcçb¹Û÷Š]cŸu)Ï¨DşeZîÒÕymR³xI±ßBrò.\kOcK…úB_ÿ¦>èÎ¬®g®«µßÀÄõ¢Öm^;ğl·~à‘ÖµsÇzâƒÖ(R%úÑ4<Z^¦´ÖBV{ôbä&L«ÜÜ‰t©§ßÂ½éé8/bœ•EGìxè£ôh†¡o§ya¼Ê»ªB¼²|:×]Uˆê“W›dŒ¢ğ!@.Ì°Ÿøwïc|sQU=Šá&…<D2ÛÔõ÷´D{ö^19ÄvBÕZª¾¯Gd³İİÕü;¥G‰¼hèœ²7ªeçU|Pê¢&O_‡ëMéCÑ,o*–ïÔJ)Ş®NˆwÆî=kpö~Ğ #²ğ‘FÓï!êDøä*ğ˜Ö½±•èM9¡ KÙlµØmIí[“,AıvğŞ†j­~1¼Wßé4Ë6~ØŒ`r­RR3(öU=8¾h’bíÄ¬AB$L"tğÜ®-I,Ør‰FyÃL8ÇÉÊÀ a[26Nn@<ƒ¾‹º÷H-øœ…—eS'rÉ–¥•=ûØ¦‹¤ltòU¤Úm:òA˜\rënúYtLÑBóÙûu©ÖjÙıe
›HÔ4D(Š®K½wÃÉtt~Ö›ır1:›AÂ'éÃÅàüh8íZÃïÕbìæJfë€Á®Xş¢Î"¤h?]]‡ƒ|¹"2[ÛWàgd°5/zÁe»ÁÓıı½¶Â‘¤A‡Qáó>w’µ\´íÚ‘jƒ¶!ã‚±B+ÆpHfãùµ™X¡Ú>Û¢×`w]ç«q˜Åi35Ë%[bT>Ü\r)Ã(n"d½t=äjÏÚjÈ†ùdUæè×Èõâm	ˆµÀóØ¶ìØP­1UÓûY²Ä†*ui~o–Yk9„Q´Ÿ$"n‰6R9÷ÓÅrÕPæk_.kP1áòb˜W¦Kç©|´Y,z§y÷NßÌFã“_A¦RÔÁ:M3l(® ³gWàx­uúUó¢Ğãõ”¥¨‰”Ì˜«J•[ YËĞŞ‹Ò7šœ|”PÜ“$¿F"—$äßÌæÃªªø0è›ÒV5¶w˜‰^³ŸŸô__ö¿@,½³#~[Fÿ¿»¬Àœ.óOdërQ®ğu#wz=UÒ šCàM4àxt2N|†¡U…ã¢e$bNíÊ÷0£í9Ló@+ îiªä•µÕjØçİª‡	D#»d-Ï2 »x6¤ŸŒ¦³×'çïç'ç^´u7xo$şáÅh6<Záí¹®µ`:ïÀÙhv24Hä;ÕoşEac’ÀÅu&QxÃ¿á•]‡˜«yÁ+Ç€h˜‘—Ìq7§‰BÎ'ŞDäÃ v
_2x³`°ˆî¬ñI0|s~r´-oÇpUây¸ÄäM†òÿeƒb6ØD.,-Sô§cƒ|`vÈÇòÕ²ÅÀ¯=ÊÚØd«“ìıÓm×©ù0- ûÃ»ƒ±Èm‡Q„owĞÍÎÇ»p–¯îÆí×ºC!Ñ
PÅ5=efLgƒ}6üÇìHÓZÚø¾©G_	^w‡x(ĞdüßÏØ,Ï²ÁËÀhtŸ7·ÚË")k:›^çŸcKPY1V‚‰¬LiX–D9–-sxı¥+$&e—Šªññ÷jYQïÈ‹ux9e+ó$ÉŒY´Z CY’¿+ÇkğûhXáÁ¿-%o†ƒ_fı×Nœ?dÜ´9ğ¡ÿY#ˆ«*BëÀ&ı÷ıÃ“!İ´†GÓY6¼»ù×W«ãöWº˜+®DÚı²ŠÅ!ã-œøÈ"ş6lè\¢l%šÍk®µò§r÷Úkƒu¸r]Ëx›AâJqfÑî ŸÓy _ÌrZ?M;4 °FÔÚ©¾.éZªÒúœtN‰,)˜_×
k‘Ìæö/´Ğà¼]—”-¼¾uÎÓ§™êµ£Ñ®\Åq„Ç<åŸmC®ë7Œ¸*ˆ.½Df§÷'Â?.Fgmü ¾øÛ«ÖMÅÏW<î×!ZïÖmq¯®Í½¯-îÒµ½GÇº0İºîĞ…wè¬b˜y¼—™†;s¶vÚòrègû$´c<¹¶‚CÙ6¤M‚k³ñF°Ó.ÂÊìŒúÖ¦«ßçS¶2
Kø)\‡•$\„,€	Ö¶Ø†½`GÍ x»Á‰>ºãnp»E]—˜ãn¡{Moáé††ïL]Û‚iwï,ö¡4ŸÔŒCê`JÌˆ½¥ië>FQÎÃ%ÿ¨ÎÚ"ü70*ÂÅ]®F1¬’,‹#Û(ä¯ÏÔÏEœ„ù5“6s—3¹Í`Ÿõçó8[ VlŒwÉ3ıH—¾6œ,\ÆôËşujêoOµoTˆ9ã%¾¸y
´ôµ¾ŠVæBƒRÈ…¹hb(:»Côh{Ë°RÄ+|±¾.âô]Z®³ûsßÎû®¢ÿfò?½OL6+(ªmãnA0Oi‘ÀaàÊsL3pÏ³»N¼¦ü·ï¦Ã<šáº¸$Å8º%Ú@¼Ç}Q“îİ3KB,¢4¼¹?,‚D§Òµ”uy°¬İ=´Ò¦!ñÊ³õ J~ÀNÈaÇ&º^‡ébà="hék•çÁˆL”¼7êÆ¤ØTaªï‡E#‚ë…Fö+>b¾,’8ò£d©ôW>"ïÁÜÊ|´C¢ZnÖÍ'z(¤iÅ VBÚ¤ÉFpïKñæ2åÅò´vZes3£ö‰¯ƒLVyš6
P«M±JïRmO*¨ÛÔõ<æ¹16  Ëùa¥[½ëåã:¸P/ãqr¯DÕÍÃ“g)9’b~ÄkÄëÂÆ-MRè“rxşf@%¯$–Íï&o¤ûş{GãÀr—ù•Ğ±z8ÉCMë€oÅâ†…‡'­Iv'0Úå ™#æ‹,˜ö÷«À8Vœç»ZeŞgÛ|˜nâ
`	bÊ4Æ]„ökñ…©FÖEûøÖSe^¢§¼€–7ay½¯ìmß·²¼™!7\0½#HÑ&àw‹àaKw¼İpxrR6ÌÔîº=Øf‚€"FÙ"CLğùÍŸyŠv9Ô]ñ='LÒ®Âcålµ{s~ı—]—…oA_w5ÎÊŸmµöÓîvõR¹¬`™Ä÷q!Híõ¼3ÙfqO¿Kò4n{o¿.âø>n©ınÂ‡è¨ûº%GÊĞí4üô Ëèaï³êöÁ˜Æ³ãÏŠm<{ ¾ñìÇ³âÏŠuğ›şØõ›$ŠbY“7h&w?h‹ÿá]v®×áüZá½aõşhI›É¯ k‹×Ò]B±Ô/İ.o¼q«ÌğZ ÀxÃvJ»›°Cğ8S$}CÃøN{µkà-'eN³:NÌ1|çá66k¾ı¿ò±>óëÒÀ½Åe7½¸Ö†	/˜ÕŠm£;'^ù<9É×ë$¶š?}éú©û¸_9!–’Â·7Äg·âñ¦¯hzÿÔëwİö«êaş‡q &ßÔ¯} FräùêF²J2FCé=6Hÿ´¨ÉÕîëìäÍcùê³ÉşTscÎ7:;aë‰±{!}¥ãÑ˜ò7Agæ<ıIì:‰âqª‹%pŒåë¦1Û€TömèùŸh@ƒÍ*˜ÄÑ·Çc?¸ol2ÛfãÁÕ¾Á8¸€Ó‡÷«Õ·¹Ú†ò ;(ÏömQ‚ònv;7‹¿ß}‚&§ÅhRê lŠz§ÿÒÃåœº˜·‘årêİä>¼¯şr§SÑ0‚dIZØÍn=îÂíÒ‡'ç¯'Ãéô°ïe¶hğ
‡§-¶¢Æä°\Œóõ£pµf‘õ¸Î›æÑÅº¦€sÃÇµ):e™s%yOßØ1v¼§eÂy“í¾€¾& Ğ¸Kß*Ú™§İ,<ÚÁ|Ÿ¶H!âıx!pi”=Ê?[DÒ€†hÕ,ô¡G=@V#(2É’‹RÎY…¥\4Ç¸›@È'lcëì±Àˆ7ÿ  ÿÿì}m{Û6²è÷ıL>ìR­¢ØNÚíišteIvtW¶|$¹Ùœ½}ôĞmsK‰ZQJãó4ÿıb/Ä;IÙrÒŞÕÓÆ	``0Ì‹:õaÎW‹°öaê~/Cñ˜1ñ~/còˆø~CÂØIÅA‘JÿQ†ã±N‘¿Ûñ)\NÄŸüÑ„ÉˆeªÕcVaĞx3DŠ¯áĞU*}@³ÒûÔTÓOÎ¼ïöYUûUe~Ät-‰Ø€§ìÇGµê‚ù¼˜&Ëëz˜B…Ï€g¾½ú½ ºá»Æ—Œ$W|ñˆ¦ñõFYõ8eUëgeKv¾dÓíìĞ‚àCaÀÏBá/
¿ƒ/¿ÿú²ÑûîËFï¯_6zß~Ùè}óe£÷òËFïÅ—Ş—¾o|Áè?DiEôLo«‹rô8¼m$=ŞÅ> Áñ uhğ .«v8 ƒÑM2wªoôÒÿ×çè± ½Ü 8ìMGñüXi½¯î<Ö*8B ôâ± íq"P@›âØ5ÙFñÍ6ê¦ŞŞiÔ@ÿ¹÷A[®¶›“$Şÿ
* á½`´yHÏe;È8ş÷6Z;Œ\÷×ˆ˜¼—KŒœøØ#ÜMr°ÍÛ;ÔQ4O²Ç#§®ó8CK³¨?[FØ}@ÏÖYš¦jˆ¹½ĞLPê8ûø¨Àe¹ƒ!ÏS#?&¬Ç[t§Øärï19¦Ÿ¥ƒàŒZâX{xûxı£ğC†¡ö+şÉôËô}@:I³r!=Î.À¡=ÊbˆÑı.íü67Ów›ËÕ¾ád›[-KëÃBY¥Ñ]¼.r%ï·Ö!N’MºGrÜ^m"¤ÜŞãFÊÀÙo!÷
2oíÈê7Md'"'Gş–½Şÿ$Ù3ÇWÀ8LtgÙrw€öüæ¹R+n0Ôš’«,—æ¥EöÇeœŞ+ƒ†9öolIj"cL÷°’8
®;ÖVğÔßzc5uHßds×›QÏ…	Cµ7OĞµŠm¾Ò½í+t®s9GŸ¹{í:ßé«BßöÏ'_ ¹ şüùÃw±Ö>íİ³y¼œSa¸v×Æ±Ê!öî˜Qİw?ğ÷"øœ@JË~ ³ F…G/B¨º¢YF¦ù*Ò|nö´oÈ’f4£'ÆG>ÜŸ `¸Ç‹) Pó1;¸ç‹ÑŞ&[áRy<*µFDà#SqŸ÷ÂÆ,m§ñnTç	Ùvß¨Gş˜A~¹\|ÎÎ3ğÒ}„t|×/’%‡ÿ¹à~¶ïÑRÁNé3	‰Ï	ûè¥
—H“o²uçÓ:M1Uí²íZppŠU„;KÉ©ş„<YÎóÇïµıÑûşèİU§3FTORrøË+«›Úi*×—BÍ<ÔOër…íjNPDËÏ<TN†2Zä·L³hÎ¯èà€áÂÏVÖ	ÆY!Y~ˆÒ*©X)hUôŸ×1­ƒ$†Õ9qò°#.pãd±J¤‹8f 5öı÷ò•¡«Ÿ±KKŠpD.âGÛÍíÃôYnéów¸´àO®ã4µt*“O“ëM{Ks:?Ğ :ÛıÃ§øº7Ûõ²`{´+p×µAN8Ù5¢P¿q6¼¯iáëd¥ø8cÉ6êd³Çİ)ôò€ºÊ²4–jc‚·ú:ÉÇP ú6YGË<2Ğ"ØÁö2YD„ËÓvÔbücú1gMJá¯Õö*Mf¨O¸TgKö‹Â+	9&äĞÅÑl
ôàb6AŠ6ÙøÌÉ°kï$;#’•<õ`²*UŞéî;¥Ç­ìoÏC+9š„ãg”õ”e}2Î·kÄhüÍÁ+¥;dÜÑ]¶İt²å&J–JxæÿùÏí=l	gq”o×ñü]2ßÜ’*oÌ”&Xğ˜o—›„Ì$²¡ƒªv±
Z×ÁóÊ@¾ÒhÓHÍà»+kÙ1<íºDÎk/†|P¦ù³7dİhs”Ã	Cœ€@©ƒ!¼ğ{<G¤‰„R›B&áe•¥ğ;ìl¯’Ùqü¿I¼–Ÿ·zíqo:¼œTo¶Ëhr"W­º‰ÖYF©:«?y˜ 9	ÍƒÕ:^EkA»I†ƒ\²üã›d‰åôUşD*ËÀ^KÛ8™_›õ6V06Şd+%Ë›ŠÉ³×–¸Â?‘a¥¾ª„ÒuDÚ®€–k]³VYŸdj­Öh¯M	d%o#ä·N%wYİøC2‹/âõu¶^ À!Â~x(e/z£“áè¬}ŞéM;ƒöx<mÿÔµO{Áo¿O`Ÿ:Ëæq+É{K´<Å#–£¾=™;íA¯¡Ó›NX—(cé%¢ôÉìµİwğ³KØ\Öß .j&w«8ÄsÉ ı¾7šNŞ_ô¦oÛ£î»ö¨GÏVmMuÒdÕ¹MÒù:^ZIïª4É.»sM£VÊâ¶T‚©eeâR"Ş×øÆIºÇ }Òî2‹ÃÌ˜GuÉT-£xMtÔ?cqÍ’ïäŸA‚–¨È••)/×j­ãoãœmÖúÉô“Ÿ‹UJ²†yˆ„†ØI¬‰Ö{¢5øğ³+ÈTâc&?›ä¸aaZüåüä<»—ŸŞŒBm7ğÕ)V_•JÎÅç©lfÜùä 7|z‰:Ñë]œ:øšK³{X”êì´&«‰Q•rÄæ^ä†«X‘„QÛşöëÁğlO·d,~ŠÖy«Û;¾„€ºıŸÚ“Ş”lçãşğ¼Şöl—Œ¯ˆåÒ“9%"1<å"W;_Ú`ƒğäĞ¼¶¢i§ˆzR•†éçù^Ù”T%¼¯êYº
P–n5µ­>HF“¨¯¡se“(ÅÙ?Ïæ«PWâÀ1šÈìÁ3{ï4ò0üu"¡Ê8VfªÔƒİ@½²ƒYü~ğ×ÉÍí†VXLA®£åxO1˜Ä'ÕıŠ+QìlMœd_èÎà’Oj¡Qg`ÌÁ)‘öJ&åËz³Pnª*IÅƒÖÁ7×Ÿa2gQÿ#Ô÷—_½|šX¸H>d›„µÊ¿7ÊîÅèiÂC_“ÿß-xÏÈíŞ}rïÔŞîi'p¥²[[¾eT.‚÷Ï£Év˜Êó-I™¯õDFY{Êsƒ©S“œÅ×'/‰ôãë
ì—ñ<ø±|ĞßW(xÖ;¿T4·ò!æÍmö+]¢!µÕÈ)f}×Ä‹ÄPÇÒKi›fmIVJ”2Kr‚#»C¢m3swœräoˆĞõ]§¤¢òCk3Õ„b@`ß?#B|D+Ğ!&g2¬Ã€‹Œ2ª“ŒË:BİJğÆ“óŞ‚ZDW9[]x»·^sŒ®úsÊQW>-¿=êá9ÉÈ£pÕ$-İWR¾´‹ƒô W#¦:Ö¡{®—ì3;lGî­ˆ1Êí´Ì~¸®1÷™?Üd·îaÉı·yG„Õà1o‘Mg¶Mášm» ¢!9†ï![½.¿3‚û¹¸ûr7¾ÚbGaVâopí|dËS8ƒ²ß¹Ù¶á®=oE9´SŒ¡J…ƒ6Ré-ÊÃQ+Ç/´t—å÷aî/Fs;dÊ®±`7ÚDWQ7šƒgoK/mÑ¥#!“ü%Sef´bK?ŸIå›Ö"ú-àrğƒ¯nå!2±og3¢šÂ w~:y;¿&„½È†Ñ§½m*ø±Qkçq¼ÎÅ(KãY´Æ‡ê±
„ÇTå³Ÿ3#4 ‘„Ê|á6Ê‹zCñT$Ç¹$¿½XÇßkwÒ=5ì^ïÖ…"~tñª×´ŸôµoW¤MË0Ùd>ªpaŸ¦Ù¯§ivÅEãˆôB5-Å•ô"¦]k:T*q1>DE®×A·ßOÇTi~2½kº°1QëI¾õÊ[–úVÈOI\‰ÈéiŸÿÔ÷=¦j#I~’¬sØéíÛ‘At‹pJ';"K(ñ¡˜-†>è¦b)Qkª7'´ËOŠB°×+ÔEÀ4ÆIò6‘ñÉ“â’FÂ¯¸”IpkÉ– ¥•hCÙ2½£òe=•;Èİ(eë96pRv0¡æGù­æGÚ{ 2y}'Z2¦1'‘ióPàÆÉÉúuÍGª<‘OONU™“¡ªuå9Ç›'ûEAtU]°[²˜îú¨E1Äc¥ª¥+ôhA¯5Âe<qô†<~›Ìçñ…ãŞb±ä,c´¯§w -“&;Ñ@R¤ó‚i,5ª‘ŒËÅäHçÒ‚V:j,w6ğ®&dX
ÚDQfŠ:È¤°«iÃÏP÷QîFeY¾LšnTÉ¼tÛü}—Æ,Æ6F+¿wòÔ£²k|wÍMêb•­H8;×iI<”r€š«ÓM§\Ù•°
j±q™>ò;­IÖÕ±ñLAÿ}`S yFÙ4“¹ZÅ‹Ñ
º†P—ôzÅ-ìwiÛáßÎ³å9ì8ªMrä2óUZv¾2‹NÑ”ÅšÓmºQÇª@şT¸dÔ?æ²?ıT›Ğ
‰ùş%÷zXªö–óğ!‰X:ùı|R•[ëŸJ\átxnc	üc§+Gí÷=¹d;ä£ƒš,¨ªmt·wÒ¾Lj6®›9cïlò|Êè\o!³RŞ.ÈüôÍ“Ÿùu§UéäW¾ràšDúšŸ&Ù[!pV?ÂX*ƒÍuUa­\ ƒ‹#Aƒl£U‡‡×Í@·ÈÔœ¦ø§’ qƒŞº_¯³Âû®ğ©ıığúš´c+¿ÉXiS¹óÄ;Bğ•©ä}Ëğ*/,¹mªªsÉ}<wIK×g3÷xÌUzuz#‚‰N–Üñîít?RÁ]ğ¾O¬ŸİÅ¢j¨>ZŒX_V½âD T´—ó5ÄÂ×‰œTW¡D–f1-›•Â5àÙuÇ˜¢ğL™öZÚ¤áuí…öûˆüñ]x@Îú¥Àné¢¦dQz;Wguî,¥Ö^Rõ¤Sg2=øçÖ³YÙÊãª’*ÁbªL|´»¯¬êXaµV;š£àÂ²tˆbX¦=ŸãÕu”ÒM¤UhA­·½şéÛÉ´>í^¸pwí?¥Ÿí
Rëî3Ë.ù.ÙÜfÛ¿~¨5î÷vÁ€âğñÏÄëß$ä:×ñš¹—÷È=¿îÅ¥*aõpŒÊâñÖ»ÇaÇÓ®~Î±.×.¬Jêö#EUNVÆB©¡ß~³èƒ÷=MUãUåd#Ë>JØ†Á)Ô¹z/ÎP—#ìÈ	ÊÜ”ª®øz+½¢ôâ°’…!r¬^l—¼;rôf±XjEkz'HÄŠÏ‘qö5FHø¿ØJÊ&fÉ¢(™1j§  ¸2@1XÍ#ëå×÷Í$«wh;CëÃ÷šÆÕ]µ@ÙÜ&y[mæ589±géœpÔy3îöš¶†[Â–1ùÈ/í¢¥bH	üWj*Å³»9qØb	øªiìª;oÉÊœ{–[Ãrúô­‰â"û3G¦¹•#CÓ<É!í8ùß¸õ1xcªäN¡`³¾ˆ[që‰Eç5ûHíY¬Çƒ=öN±¢Ñ-S|½ß*N¸u¶!‹+ó£Ì$º‚IL×\<Çí`Å,AmÈ2‹ŠUÏÑ:*É»,ˆU˜¼êNSM —f„€ÃÍ7WìŠ]¤Á—2ëTgÁœp/V˜É’Æ1É®ƒI¶JfÂjÃièO-WÂP/^&Şñ‹Á©fNOª³Zt)-ÀÂ³‚o£™=˜Nöhğ\U­¥’~§07Z³	«ë£Zºè¥úÜXĞÉĞ 0²ÌiÂ„»	ï]ŞÅŒvÈZim²››”ÉÌGß`'‚$ËY
|-:ù×CÛ%•ÌqK˜ÚKGy+õ*ˆê 9¼¿µ÷ÖíõºJ-ûı¥/X­ÎBãRÁ²ÔrÁY³.÷Üòr°q\İFl¨"øî#Zš–3±¹J·ëÓ¾šÉoSrVO"èÈƒ¼^ĞéM:Ë³k7ì•x@E<wğ¢÷—İ¼“-VÑ¦5&ƒĞšõƒTmàG»C?N&Ã³Æ«z«©rD´™İŒB‰ß‰È8ç`CJş¸‰¦Tf…Ëæ³bäIÎÙe6nêx‚ qKq/¾¾şÊşéP¬µà4\°W)tØÛ¸RØ°Ö‡Ï× ­İíö'ıáy{0ôÇ“);B“ó³³Ş3¨wä{oø?’Ş­Œ™Wù©J¯Ô×òmí)hıx`‚à_	³¥Ö9V˜ú,Å ’wÆb›²…[Óy£‰n.¥£,÷p§°Á(±.®­î1 Æºä?3ni’ŒìmÕçŞùşÉr·¾Â!L@J
"<ÂD>rîwxĞáÿ¯ÊÃ¤¡eõÙlé }¬-Õ+ãQ6ôBT<»àÁõ]ìà2ÿØdWinN¨Uƒ*Öc$L¡Ñb3Zãîß§ıó	¸¿)/¦a·7nQ[ƒíÂ  ÷<ÏğWY­@tÍ¼®ˆ ¯2È™¥HÃ§Êö»+”[0²GèwË\]DËmdWK0"X*³J_)vDñ­(†¨uâ*:5HáîèÆ¨i°šºJ«¿œÇeÎ¯«YPevˆ¢wœxÅÀR˜ÏÌVÈöğ>j,¦„ˆ¼ì$â«"dc©Q°ÖÇ¬Øwq0áÉ—À<h˜äTñ`NœôÇ»(·)g†eó«h¶pì(!&é3œ";„·3ßO§f› ÅàfPœ®aš%v½­OƒåĞj‘ÅF†*ã÷Ú3~z{Ô³“	¦µ–:Oæ0Nñü8Í ½<]šxß÷'Mª#å\_R‘l¬>:H†´&TºçÉºÀo¸o¯v>”€Ln•¾\ê	ç´ã4Ãë:Ö:4ì›æú=†ÚA'´œé;ó+\d|°„ÃdwG.BØ÷p=ŠA7=VZ¼KÃ’#ÏœıéöÇSÇÓš„§2SÄ2¨lV»‹*t€”TVÎ›î²EO¥àGqN„[ŠøÕĞ™JÓæÊñ;&Ò,9†GëvqÌøìÊ(†Wÿ"øê^ædS«6£ LkµİÈ¼ŸnIÅi2j[@•7›ÄdyØä3O›Ö§í|7î PÎ§p!¬ª­d"Á/‰#zIY/‰#¿ù|ŠàègÎ¡%A"—ğÕÓŸ‡™ò“•ïÏÑÙ·,Ü6òtÁ0£çQökšŞØ­Ë‹.ÄÈ;kÿ>÷½ÎdJ½ak›:U¹Õë8=Ät¢%ŒÜé…0¡ø- Ğ‚+MïÔ†qm’ëd†ÛX'^¢5\k•åÊ‹óh‡fIÊ> 	W˜ şI›TªbLú)%ûÄN¸“î±-]l5ûêßCKË8Ó-
2f”n×Â¾@ô£áŠšÉ?®=ØìÊd0ºè´€ÀX™I¸Rä9í«¥<f˜p‹Îƒ)IãWì/İs,%lƒeSˆÓÒ¨Õ‡”öÕÂu©Ëá,¶‰:Ö.ÀFÓÜÖñ*fñ$ºÉM0 'Çá©6Ù®¼¸5¼!šmr£Yd%@®Ö5xôO—äw'@+ØïF£¤_4<Ñ‡˜¦ª4„ãMB„¨uNg?Äá`Û0²k2MˆNÅ#swbD™…k¬	Q9¨ØàÅğÌõ0ìàÁ¢á”¥9Ç¬âÈdä ©i‘M€“€và¥eÁƒM6ïüQƒÁŒYgºêÀµ/'“áùôb8îOú?Ù1¿
mºL×‚¥x°,ªuêH•¬9fICÎ ë¼ã}2
›ÒÃz–MØïwQNEºŠµFñL„ëR»ˆ	á¦Ì.Œ‹†³AÀ0¤ß`\åä] Î¿,ê"İ®£T0*;¬§†şÌ© #BO09;EUÀrn„¸z˜PuíŠµ/EH¦¯ó\g[5>¡êAfÁ½É_ÆéFËïD½‹ºæòäŸ‡¥óaW°o£üvÒG}™¸’…Ó¶â?ÔV¼Ş.[Ö¯Õ®ª™ßÓ×ï!»&%Óã&—ìDFã9]g9£Yy(êî®ßå—L/˜­·‹Â¼p¦d$CJñMjÅ¸H¸–KÂZé0¤D.Ã%^NZü"Ü™æÛÔ6ƒÍàÎ²ÛP-g]ıËÙU)I>´Va±«ö5÷¤A™&xx‡³Ÿu”´.}(!¤ƒ€o´¬ÇYÍÌä²©†1Û«Ñ-ã#XßãXûµs•Z.ãgøèÚ"¼İwÈB&QÊÙˆ 9ìQ¡V‹´i¢W‡z€iZ]9Ş À.•ºÑJ]ÑÔ©X¯Ô²s^’Í5|º Ã¦`êhé!æ£>An	K1”<óü÷6#ò…Ü4âäöïòq¡«ló˜¨ wî:AÛ,ÓGïb]'i,xvræB›àÌå?kï?kï1×Ş-=í´ü,ï*	üc.<–fá[yĞ\+£óø¡W€ÔöV¶ü\	R4SûT2â¶ÆìºŸÃ°Q±B5t’„yW	ÄƒÍF…fàTJ¨…W
bÂ`ÊãÉpÔï§ã^{Ôyëi‡+8Ù&­A¿Z¶Døœ÷|»ÜğŠèûJ•›åD‡®*È³kR<M?¸Ä«¦.Û«/o"\ã<°ØAÌ[M]Ô0ÇÜsQ 9)ÂdÛõR8~ '÷µİÛ ĞÌZôZíÕ*—¥w\õªÜºÚaw~OÿïÓ§Á×t”ÙÚ°‹[qç× ¯¡”ğ½µuÎ–wP÷Ê—…Íà×ånÉÍá»ø*Z­èİ§ÛÔÇ?¤NÅóR…Ÿ£÷–DÖ¨ùz(jÏ2Î÷qhp/>Î2áÅ\À“Á4_eË<[Çsàe‹Y/¬bya›ÍX“¼´InrAê+fip)s»4²©ŒüSGË›9œn×ÓuôpO>Ë=
xØ,*Ì¨0d"°ã‹aşüç Ç´ƒˆåÌ‚Õ~ùá·tõ´[&ŒUë]…¬	ïúİÓŞÄ†{¦nz†ªI]cÍÀ¶˜ˆhyXíŠs§>fÅ¢6Ör lÅß§˜bl™zLŒ–£pï#ù3Måu%å¼< ’ñµgOşÈ¿8Ãìy-à˜îk=ó¥²Ã+şY§ùÎLgRhAÓ?l“l)(3ŠÉ”ÏMOŸ=÷ôøxÊ†úÓ“MM¼>M!,”Ú˜Ó=µE#À@˜ü‰hYl¦¡,ñ«Mı·ª5ÉÂWøQ§^¢×şR,ùç!$Î­Pò}•’%1-5À/Œrá³Êı?O¢äy	(ñ‚ı‚ï¬={¶%”DèÅhÎnÍ-¹–*ï¿*(–¨½¿ ÈSŸ‹uH?^)ÅĞä|‹Ö—+©AÎ#6L-GBBöN˜ƒV«Xì8¹yeâ'
m9på±Zã˜œâM"nÓ¦ÙóWfX20¯“©†å”1Ş¬ï´%†£A¶!K›Ó¶AÑšÅÆ•Á*×áÀr_=ÄŒ@È`Æ½à ˜Ì&·ÛÅ»…ílóM¶èÏàÆÎO4!÷"V}0ƒ0AØû8‹W8Bz_Ëj%SØa!¬–Áã¸<1˜_œü¨u2ÑÉu9îD W£Ü§œøOÖähg·9¯#½SFÕš
r®ŞŠ„ëøA)~
Úò§_'|ªE¿¯ ]Â·‚"Ó9AÁä(½…ß”Ì~"g>0Çä­â;á¨ÚºÅ„jv±ÆSÏ×dz0÷xd®Øsö“+­…Ë4@v¬h ?	¼şl
ZÒ’ÍÙ’=N6‹h¥¬'<´Ò]›½uF£-³!«
şâ6Ûdğ‚) ˜(îè’‚eÑ@ª!ëS‹\ØY)4º\Î3ö\#ˆ|Â ’HıkÑ0AWh^¶–ƒ¶éïÖù÷ßs¦„Ä¤á º»Mì+±FÍ˜ÍZCffé[ÊÁ…?À{Òy†(¾;fğ½µRTjXy)ÿ¸×(ÿÊ„Ğ‚Xy¸uÚE8\Ô‰ì¤}MfÜ1Ù³Åø6ÆÈ­W¾– 6H´|ìÉç'é¬şK²‚ğ»^5o®,©f@.d±A›™Ît?u½6]×s M&î:¹QV?~ßÍà^ƒJ)D òã,sLw¦ğéânº¢]yêˆ¶[ÍÌÀsOâİÒ=Wf~à¿ÃYÓb·³<î/7»ÜØÓ&ëßâR¾µ…çdÄ¼e‘ÀËÆİ1Ò‰VRØ›¦k¹ùçÏÁ,ËÖs>máÑÑÏî*©¾A‡˜ Ş_ÒH`!mÑ˜v ë?`ZşŸ¸R÷EÃòT¼¤ñ€•x$¥5Y'•m]×„ıÒ67p€<ŠÅ®>®½¢«M+Êyˆm3]Œ¥/Úæ$Fp¶a©
Xäpâv;·{á4Çµ4|ì¢«h—]wa>"pËvµÊÖ›œ/Y'‹)Ñîxä€AÀ:8CáqeÊÃÂçwÍöR!•Š *,øªèvÕ²óğ“`«ì¬¿${(¨3‚<múcà·äñ<Û^ñ_xnœ$8Ú/VÍ€9Çã»‹hsËA8 >®’øk¾ÈQšÒWÚ“—ÅX6^dÿJÎ¢õ/Û•‹Ğ¦³ìv9\^ö'·Dtœ‡a9\"vK~ áQ5}&ÃiSæé,|1QøÇ'>ÚU(ö-Ÿnù*2<õo¶K–öÍsŞYe«õõß-€@°Àº~¢F©ô7%œ¼íã%z»#Q¹*Â´Òk:Ejb|T­<6=EÚt“sXê
¬	ùå}Çj‡ v˜.h¯åU—ìßŞ§/vµ0f@N*K6?‹–4ÙaßDx ¥B‚G3 ÿ¢ı a_„ƒ–æ™å"¥âih´Ë€Oıàì/=…ÕÏ`6”«„s°}
TTè	1­ˆÈ}‘Oî+üsMş¸ÈA¶\ì$`í¼dÀ®âç©¡ s®3|Umb«ì;İ&”²mü˜ĞµV¯özİÁ½ôšÌñ&ÈÉ¿9ßhXó-ú/¾ªÀ*ÊVéİP4§<ÔçQ®àoˆšÁá7¾´h>xD¦ªí;kF^7´Bøú ¾YÇmS ¥ˆÁüHe¦ïíXŠÆ–ş†É`TÆMõøŒRäxSGœ1[¤ÿNÑÌ‚r2¯Î-é¤©±‰:±áÓÿ¶Rå‚±ìSu¡ûqR)V)¶tò’kcºÛĞ‚mÀw@Ÿ2/<ÍÉÄ=¡rª:©}
M8M²öfÍn)^^e	
ùz¶;t:µÁ˜-Òn´ˆ'YÈÇ 6úì ˜¥ó¿ÇàÍ@‘i}ÈÒí\#ÀÄx
vÈìÌ”=ÿÛ7Óoî’p 
R]ĞZ{¡Àîú"5x',Ğ_veâK“áÚTĞİ9[£öC©”¦†*z%ê¬pÖ5Šêë¬Ø¤µ&tŸ}¹	ôšÁÓÅê¥ÿn ºò`7Ÿ¡èä#-$ÉGâÃk;Ş9?èpÄ>3o;Şƒ{Q¥R)ÏŒ^v¢,’ˆ(Ú<ì·W§XÜ [kE¬é˜ëB2°—7ÔäÁ%ŞÔ”rÀ¿Î1Bğìÿq ‚¼:©2dÆx0Ã?«EjİÍç–Ï–)='C©?+#èE*¨Ûb—âødJ¤Õ×•
ÔJ»·¼ÎÒu7”ò¥E¸
ÕcÖc?FQ³ZûU¥C%\|òfP›væı£µˆ’%§»t©UÕDböƒ­¹Ò&,½÷1têl5ŞŞ*hq@6m°Ò»èw¦ãŞäòâi¼)º…!cXÕ{^/ÉŸÏ{Õ$Ò‡°êfËØg ïı¶?´äÛ°Ã–b(]>¼j ­Ô°›ªÖV(‹İç´¾­Š‡V÷ınu™•xõîUu³05£V;,‰¬oa‰~Í~¼g?0CQ1my¯=îM‡—“é_öÏ'%Mú‹ofrßÇc€%Z»—£6d›^Œ†ƒá¹+æwUŒ¼vh®â’ñïŸ¼±©«(†Å¶*nc+jeä–]ñV¨éî”Ü MıÎâåË%i¾±ÕrŸ­„Óç£‹Ÿ _>¬ÃuÉŠ@t¶ƒÅ<dºBÎ*»	á6«³4ÊóÓ­â–\tÜ¼ª¹'ó;¢0ß+»‘aÖ€d¿úT¢~Ç#Õ”m¥©DqZ*…À¦'¬¤z–èV¨ôş‰\Ïæ÷¬ë­QÉşí–ûÄš4cøoFª×Bg~jòH™ İMRt(.vgÇG‘­r \à“ÜC~± y	sÙwëdóĞèŠáTYÃZ”ëìÍ©£”©™²f&`X³¸pg±RFñ,[Cp
{šF‡è-Á	Èˆ]‰Út0Íf¾©0–>™ØVM•—ey†ËX8|,ñavœ¬³ÍmÀöFO¯¢%v; )™ĞpL¦t
a÷}á'²¡– =êŞğ/ÿwù—fğ—à/dİXÉlÉ¦47òÌ¾ã2¨KøÄp9Ô˜£m·–Æ–t§O°æ©[Fûm OW|9öHsÍÏ[İş¨×AÉn¹`Íˆ \¡‰'Ğâ;û{!/~7…ëDk¼¡d'	\”ÖRgÛ&É2Æ3•§\ô3q^|,óÂÅ¶=
¾áŸo_Z¢­ˆCŒıõÿÉ€Û6ƒg/<¾)	
ŒsòJ¸ƒê$5—·–éú™-ùê3H´•PM…ñVƒâ™æ”@_9“-jH4*"¾Ó•æL¦ç"•gíIçíô¢=êOšÁÑK²ÚN×:šĞ¼ÎÁoâÁI0˜¾úÿ3<Ÿ´MB\ß~×Èà–‹ÅAÃ:fŒmøVµ(`,c©%~ûeÏ‚Êƒd‘lh;ôë.R¡Ñ6—œ¡&üI%É§xÚ‚ÀscÛÂqëÀÚ£®Ñæ’Súa5•Hà@Ÿ"Õd(¡ƒ·ùxˆˆıç{­2Š–ì:4u¹Ò,M“ìİ-¸ù2ö«¶¬Šp;ôæfŞ÷ğ
%ì³”hÖÍ`´e»äš}ñ™iÓj|‚ÄÕÜ4Ïë$›d«°šj?i]iFİ÷Î| ü#ó‘VQİëH"úYË cøo¿òƒò’ã·1Z->'fÜ;Æ¿†+ê¸Âù¦Hwğs+åÁ\nbü;YÇñğŠŒõ‡$‹KÂÁÙ\	£—m™ÅÊÆĞ=»å†)m¾nW©HjvıË>%¤|	¢AT…æìÂl7¡mê|^6û8²Ğ§îÆ"ˆÖg`*_£#/j‰Êô#¾t°\µÀ;_âºtrºàGËšƒD¡z]Àä{W²
]¥np´é]¨Ä)m‡@Ü—{‹(I9›’zOE8b(Q³=7¢EƒÒ©\¾f¡vâjsO¬bFï¬İLÇ—§§½1Êö'íÁØHıL‘d7ÉR„CM•_ô¡”@Dáº1uZ^Õ,¡ßÓ(är‘ú.şÅÅGáò;jÑÂ±šâ°N“åUöÑ]Ûs›£ÜÑàPP¢Ó;¿ñv6O#KÓ@ı9;I.Ù…*ôè–[4Q‚Zİ¶›í¦h¿át[6c–òË7‘á!¶åÛhµIf'q<‡sJ¨şÄĞ	Ñr“·şŞ{<lºÓIû‚&¥NíÓiÿô|8êMOÃãö K“¾õöÀ½$¹YBÈ=Œa¢.Z~”w-%«r•ZdĞOûçS\ OMc¹V[_“Ñeoz2uz]ıbT?í++¬¢Ü:4ÎĞÎÇTû£kZÙX©¬]%-©Ğ<‰®rÈ‡g´Úû AUâ…ë™¿Fë¹®Ş¢ñúˆD<ùíRÜF¹”~ÄªW6Br*X]ÑKr–y€)=¦Â…¸ãÃüÊEßP¯ñF†R¬¾+n8ª¦R¹wÚR8ôµù½Î$Û’eóšúötééÒgT”Ã­U	èçG½%bêÀƒÊ×¤Çà>­ËëÂİOĞ˜ï<ıüÁ‘½»› 0 ùkt2øjÊËAôY‘dàm8Â®QÄÇ¿&«˜’†`9¾%Ëu^ÜàtÔº.A%±e	K”-nL
JÔ¶É£¢¶¥¼Sf‚M
b‘Á«*éy€Ñ•ô ·B^‰î€ı…äîf¨j¤·JNEœXİø:Ú¦,*5êT0àv“3=øxğ¼0Ë@.VE…|eQ_è3bm—³ÛBBS~ª”K—]ju}¨­^>2 YçƒÑj¦)äfÙb±]’‚ä¼ÿÄ’— ÊÀgÑ€ÉÆĞ/’å8X^Ô¶ê-ÀPrÔš³l­E~3a5´Y¶ÅÌæ-Âmã8Ş “ÎüÃî¨®èŸ*a\hQ)‡J£¹HƒR#ÎŠ@ö1‹((k¤›¨
ÃÍÛ#rŞêƒöÅ¸×%bÎÅÅpdØ[÷)—*=lıÉ$òP)ÄÈõñ¼˜­—¢UÏ‰´?eA÷*P‚åÃ%&n/!9^ìùúÔ.ñn“İÜÀåk·Ã›í/™ô/¯—Rê‹jRŸØ&†2Ô%]¯:Öãx­áòì+Œøgğ2¥‡q-œvf=ÔÑ­F½š¯B¸×ªu'B?xÕFwá ¬1ƒwÚX–VVÊûlöÕ‚Èmœ®¬8¼Í~íf1‘	Şeë_À˜CÆÁ 2ˆpYeÎ=².trÉÄŠ.9Ñ©{·®êpé÷ÍvÈŞ£W&Gx£œm»¶µ1*÷˜¯ö@”F––Àşµ YşÓyt7]dóxzô­_Õ·KØİù{óh.ØZañÔö¿&›Ù-¾šdİèN¶³UŞã©ÿà“jXŞ}Ô·ˆÕ6¬—)T¶¿XÇ×1éÓ,ÎaÍˆï¯ƒöj•2ÛsæÀO˜®/õvÂ§ :ÃS‘/„élØíM/FıŸÚİ²•ùöB!Â`ışE£²aFJ]V’´ÿô8İÆºÕ$P\ºåÆò‰ËWNÌh4t)"^æÂ3 rG×¿ˆâœ«Ø]ˆÒá'ş¢Ş>¸KHùºNAâ¿`lÄ·×pªÄ)KåñnÅÿŞ’ıQÇİeÄ$À‰ÎáåŸ¥Ayep?uŞV˜8B¾—êm¶&ñò½`“©ü_NÆX°­¯ÅÈı=ÆíÒ‚²•Öé¥Í×xTVh9¤•D{M6ZM
ëğúº½İP>ZBšâÕŸ[~´‰Ø¸8”_­ÉÛŞY†±>G[(ÍÕ'õğo€LÑ%ÛnÀ°i
Ö?«¶8›sÎtìıª9ÔÖwl·ƒ<VSG{¿l] ·¥¹«cxÉÅî{vcÏO­çi# kx „oo¦O8r.y®F@Y}–dV‰L˜7GèÃ¯ãl“şåu`1ÒWR_Z]Ô¶Ò°®RÖTA’>\``y†¹éëmåMÅ¸ÆÌÖR|¥Ïúë{˜k‹R*áé ›9‘«§Pß¯!UÔüç[;LrpÀ¶=9şæ³4JdI,ãxnšëŸwñ™k]QÓÔÑÃzéFišı:—|WŒ®%hÀsæSzÎ&x÷—Å”!»à¦ˆ¦Ä‘ñW¬âØT´$ŞÉã4¢ÿƒ:ô¤½©3Q‡˜Z—QYªş
ÆñS$EÌkŞîÊÑ×
ØR¶#…íè!Ã°ÁÇ1á_WŸïşğâúGUkã´W¸˜ÆµL‹EmQ6"eŸ2–«“†ºZ¨œ–‡5©âàF²e20
Ÿ¤ÑMù!û…m~+){%ép9F[¬Ì‡2“«O|„V8¢\K»ÏB;»İ–0= †
zhËü} /pÆE|Ğí¿Ô+gÃœI‡ğŠ|]UÓæåLù6ÍÒ¹]Œ+¿±‰O¥GŠVU’«uö‘ê‰·W˜¼Û!2éåZùöJ·à)±ÅG!œZ³“¡ÀyµLQ‡9øÔÜ{N%8",’ÜÛ1ÿ#Æñ*ûP“ôĞ Yg»C­±JµJ6<©Öµ=Ÿƒ“º[ç‚¥¦É
Ô-º–EÕ¬bÉØiµö„{míÈOíZ}KÃh-BƒèÊp@@…Cï­¸p*wÍ|–Ht®[‰+šŸ-ÖwA¹÷_ñùQÈÂ¯Ì5i÷í™ã¾ñ
}Ú»ü¤l*ÒK•èUl•’|LC¬÷æ7ñ$ƒm‰Ï
ñèSy›À	¡!ÅeÆÖª$`hö`§AvîÍ6Ş6¯Œ‡³PÚ2N®÷ÿwáˆÿNÖu¸^_ l²üíœ´E¸ªR—¦b@<È¤\¬¢M-oÜÉoB^—†ŠøĞÙHğ‡<ª¿J… ËYü6ƒÌŞ4 `uÛ	ú:ÈïÓ]°¿BıDM4´i«æ’×îú€Ö&“‚'Ûœ¯´òWè&£Ûo˜4&õ(FL ÍñkïL+lÃhšªR“¼¥µLvLõ‰Îl•ô:O”íDq$N0WMVÇ8ÈE©˜£„Ã4õñG/°™ø,›¤e2+¼O¸µµÎ¢õOct­£E¤QW I¥pşnù—ïÅW›!Š(æñ¸)HM)Eñ€ó­eÚ|]¾¦åO
½C_ÿ"}ceò £YÉª–tÕRĞ&¯äß.Í‚^M­e­¢¹†¨q3Kg•ı—&8V°=Õû 8°uş¢`{l²;O»Í@°*Rlû°,êÎğ||yÖëŠMÿ>ÿ*ÖëyğÕsÏ¾ÈÒ~P58 5ÊìO÷<fÎsó^G4Ã ûµ¦ækÍ€Ö¡€ÈB˜‘C¬Èh€XíóşY{2Mûİé¸×uŞN²h_™à!dcÊñÔdø$‰Ó¹lk/LXïE´ŒS&È¸ŠÉ¦•Ş‚8KK[^Âu¼¹s7+ÌDXæ/šÍÆÖºhÒáîğ¼7=¾œL†çûf/&ã‹^¯[•„ 1^ÅñNpŞ¶»Ãw.4ec±íx9o5ÿÃOñ¢s½Æ °Ä¿µG M½îñÔôyi~2½gjfÖÄ`<XS]$~€ıÁ¤7šNÚÇãRh¸À«¾jU –r,Üü­ÉL¦Æ	ãì»rrËêFwqXÅ>3`¦qÍæ?Å '8¸^	¤{‘FEŞY¶DïA«•²D¬‡Èf¨øÇæ:rDéÖGèºœ!Ñ yİ¬¾Hfñõ…­lA‘ƒáÍÿ¢7"«æ¬}ŞéM;ƒöx<ß¹Ä$q½†kÓô‘-²@ùQ~-g‹Ö+µàÒ3½Ë†œ'Ø××€#­k¢“Œê{™BÆQábÔ;ë_MOzíÉå¨G¶²wçƒa›Í5BTO!u½‚/;®†å<şHæÉM„">ÆôväÏÊ0óôLÙé)ÈÀÉ×_WI9åkŠ#LÂ^Cw©»:íí[Rá–hG¿ø‹y¼İ#<±xC\†tÅˆÒ¹
C{›#ÇF@6š
‘šrë£şpÔŸ¼Ÿ¾íŸ¾-¢JUïœÍZùåº„€9Ş&é¼õSo4&ÛukÜıû´>	Ş€C§ôbÚ!ûø¸uæ4Dâüô¾V~ä½	)Øs_Kã’€ş{§Cs™Öps–i4®­%Üƒsî]ÈH”qãsoU©aƒîb¶²RÜQŠ‘./øê¼¯W‡:ûÔª"$°7ÁAğ£âƒ|ØRR«7@h–mòI&ÌØéşèßÂ)­@wFö–&Ll‡ÍÀÚ	'iõ‹Àò:ïëÕñ²·Ş#m§ ~sBX×[T¡»YëŞ¤÷-Z§0jõÛyº½ä¡4™s~…yCñ> ½§hGñyı18$]Ğ’Êš-Â=æPÖÚ Pœ¾âm»ëÈÓş;˜÷×ÒÄWj¹‡É3õ+T2ç¾¿’g¦ù+>Ÿ±Ú7I0²1éÃkN¦PùD?Íùé­Â.Æ!”–•†€¢´ã\óEU¯û÷ê¹Óßm­à}ûìc1¾ƒ™LÀã„¶aœäì#áX»GÚ–¥PÍ¦ü;¡TÄ³šm¥Ìå«•ò¬W­äƒ-PpG“5(š—ê½ÅË¶5w­{tÄ×2L
¶yÒ\Š;`g´±×-ØPYK#ÿü¹}´ğÈÒUñü¹eñÆHµ‘tk!’8–•X¡‡×¯°e¶‡Âw¾£iKŒw´ºÜP}ãUÛ.¡»YØ±–Íål|©# ±[Ñ<6
Ù¹0ÈäI,l²ÜO,†šeE,ğS§¸¯´]¦;­ÌFÈ¸×Ê*lw»}Ğk·ÓA<™¾í'Å´{aZÃ|íSb)ïF .Ø*üæ ‚~Yb³~íš±¤(Š:Õox()$Óä·ÖL¶A!YeFAzÕ]Ph²ÑnÚ¨[2qèJÂ'Š(s“êƒÅÜI–³t;)CñoÎ‡hŒ ‡Q©Î–`$ëğ ¸gz!Õe(a¾C»(8™øöâUéâ´\=”‰9~êhšwè ûbåÄEÁ]ülpò"ÇD¥5ÅÌjŒW3«•0~Ælv0fGA“1›ï{¾G%Š<”˜b±ˆGéÓIÛãVzsjØ,yy„%7^½Èò„‡ı+›`„éŸœ|Âh%	ÅŸP^7¤×JØ,É;U›u,"W‡O1>û~Zdá›%¬ñ·´½¨Ãñæ†DÅ¥ˆ‹6îhˆ(–6&;éZ´ÛVmwæ÷Òã¢tõ”%a‹]DiH^ØÈcÜšÆ“äïŞSã(¥ˆ‹Áâä?¾£i=ÎªÏ¢Í-5l¥Ñb†Ïr¹yRÂM#x[±¸KíºLh m×¡iØÇpAIà,›ÇÅ$$ZX£âN1ÉÚ¢XSì
m½ªGÁ¬p>Š³¢(îÙ@9&LV9L½˜}#f¯Ş£Ù¶Ô=òƒyÑŞMõH]sšÖ©ÁQpnã'q™FÄĞÔYO–uãV8
ï®Q'€:Ä9¿q¦mşğšr¸j‘b­FE2Ë¶÷ä˜²Î‚÷Ê]«q½R/šÒÅ¹“6™å”çØ'ŒtëC d×Î·43EaåÏ”£wPŸ>ÿ^NC•Ì–x G6×å¦– Ûlx«¦/)k‡Dªãİ“Cies! ¤4«‹¹,só-/ØÍ~]Â5k…¢{-¹ØE”ç3Î€ü:ìŸ|Ãäm-±»îœ¡pÙÇ™¦7/Ñİ”ì”SÔ2·èP­‹¸ÊÊ´QNî)Ã†vz(›–)^zZıC’°ô¤Z›DR€ÌšD±-~/Uô¥+ÖÂÛ(¿-œ•›? uA™ô&°Uø¹7©ùpï@këÖQJì9¯¹ô\QÆıukRWìšä5¶ûRîú‡¢ÌK¹CVÒ‡aÑâÛËi,ÌcëÒ×¾J×îÎ‡SßÑèw:?ö¼rY|“¬ğ©•ÔÉÌ{b÷t°*¯OZAùç½L6‹‚
VÀ€Q·ßOÇ44V»ÛBPòs¨NRjÑa0 Œˆ jƒ$X­àßç_Ahµµp‚â½õ£ïÀ]+^S=¬úì•vk‚4ÿ¸¨¢‰:ìw˜‡›ê6àÙy–ä1ŒÌ>2'rñ@jo_y[7Në4ÉÍ(^Îãõ9Ù¡ƒŠ¼ OÁ¸üdAVão×Ş sÖu´‡A”¦ğI½¦¸9?‹ÕÆšd¿XÉ¿·É|\KÀ÷nrÈ‚Â«?©“¶:Ñ
LñYqöË:½Ûëu„~Å³ÍÉV…¯b­(ö†óµ¬­UnGplËT¯~ÆôìZmùœ¯âJƒÉÚQ¹gi-Ù³“Â½Ä‹i_pR¨oè ìöß?XÍ¿ÇpÛáåJ‚	ÙÃMz~­‡	 JäôvşåwÆvH¯p¨¹%€âRg¹š'Û¯=)¡ç+:k÷Ï©;ÕY{tÚ?÷!0A+›’FÙí‚¡yÆ˜©ø½®apğµ¬í^Ã¬uål£&’U×Ø¸™Eé@÷Â4XøºF:™’T6ºYÁ ¥‡¹ÁÀ´”(ÁÀ¬0Ş.(ğáu;åÆR[²˜ØPåª½ÅYªW0&½òØŞVc2O^($–’%DJx¦.ç¦sbVdĞÃá­¾6$Mû5«Úz|­s¹\Ì$óŸ®]ãŒ mÀSIÁu×_+çÃP0Š
# 0İ¹Ğ}úĞ€œ®@{
Ú+ñ'0¯Ù ÿß—ıîôœÿ¸fëe±º¥> Z.¥Â+uFMkõe_ÓÂÓ;‚JI\>å¼ˆË³mJöÇ³t`M´S¡%‹Z)iÊ ~¹Ô*
{÷yîíªĞsM:É› ƒ#ĞJ÷·_œÿFq¾M7&(&1VX(¨¡,_×!Æ?#Íòhïî—û™/€Ôâ¦KÄÃtî_Ÿc0IGn[g+÷òñºW§_–8Ve\“ˆ-lÊ‘[Œnİ¡+ì“dhy}¤Ğ°TØ [äÁ_u7>>Ş^‘5"øVü+û{¤oV9­/ÿÃß¼‡´{xDş¾üüsxdB¨ÑV¥@@åaÊd´pªĞVİ]…P!zÍ`6–’ËÍËIÈ™–ÚKÈÁâÙ
šZ0}}’J×_Å1J?8‘éùÏŸƒUÆ§ğûèçâ½ÔVÊhÈÄş’ö	IM‰úäØ~ıZ¯§Æ;ĞVğ<8RÛ8¬Ş†˜õJ#çÙ&¹æá	eèb9M³«(~Úµ‘‹BÿĞRÂícÈôq.4eJ0OdÒTyX9¬%æªª5ÖÍv`P-°móÕSV¨d°" à‡+ÊşlQ]†äuLÈEóBé^‚¢C¿_§³x‘"¢“h’m"µm!ãŞÆ2¶„‡qBR@Ø6dá£NĞ6«œïVE=»~Ç´c¹NB÷v&Sƒ]î°ñÕsÉˆï‡’âÈÖÈçØÏk‹ozh#%L¹Ë=eÀĞ[‘ŒÈ®Q&:©‚›-6‰@ •ÆË›Í­3‰‚í?¯:|ä²…‘º€Ñ^–A«]£ßZ¯
š˜¿Ü´‚ocğ1¾`3íDd¡æ›šüiÒ «w:¹V(j0VçÓ™Ï¿‚KŠî§å
Œ°—¯ïhLv+!V©ÇQ®Ø+e
q(SšCZakKõL"2 /R^@S·Ø1vÊ½ö!°w'²,P)EŞ³`=NtŞ“Yiõ&ò»WÎ÷mánnÛ0¥€vŒX­ÚbĞ¹ŠÿX£ã RdIš'˜Šï‚p·äa®KqağÌNP¡¸‹Ö–éÂ€ïÂ[a¥ß5ÛÉ“<6Á×ÄQ÷IwF²yX´l5_=÷O|4ÿóŸkØòò=¨0Ò­"W¦&#âÑ7ß2š8Y:ZweZóä\SÜT;âÜ@©Ğ	‰Ö¬Ş„¢Õ¬³\üÌÅ“™æ&J;W›%;YÉ•t3²¸¶Aj¸†ŒM8ÇC¹ZëípÔÿŸáù¤=0+IÎItâĞØş£ˆœ\R%;' ìs¼tƒÅ"6oh47È¾@>*Ò?G2ªHÎBEƒbaV³I#HÄÓ‹ÔŸìòVy(³µ8¥}…€œ	êdMyœÊX±) ©„J(#	ƒ|÷‹TLú‹¶‡ßCÊ2Úç“ş´=è·ÇSPËj3\U1p²6ÊÄ¬ú[„#Wd—ğd›´òGÁ•wè¥Aâ÷Öx2ş]aà©¿Î~‰)Sƒ#iëÅ‹k»¶V.I`ÁõÍÇ›döK¼Î§{
!.÷í$ë]	’¢upÌXÖ)à3@ı[² Ke@ìÔvE{…dŒDNV®®lÒ)dBÚTmt‚B>_86âZ§¼"|ñ²Àÿ§ë/m;½sˆèùSo4éwÚƒfpxD×¶}y#Ù®XĞ32})dzEOÕ©\ÌÌ7áŠ6%×ØƒÖw0=^••|o–üô'{ùğCÂ“mR"lƒkœ±?£x'0E.xä'Ø™µRü$[{BÓrŸ¦ÆšŒªÅÙO¥3|õ’˜PşWIŒAaØ6¾Í|lîlË“±Ÿ.Bê‰eP£–qs”ä[áÙÅ ÷éåy2íö/ôo=ÕK–55ôƒ‚Ç)™h>D0Ißğê_dŠµX> Ôà!ãwÔ#ÇrÃ<<rè¥i²Â˜Er“Éz»œa.ªVï¼Û°¯PŞˆnÚËw£öÅ´CvtB?êzî^´/¨éK}Õ2%AÅª¹âK¶Š—l&ó-#êã	Ø”b
S©>™7ñú—Î·‹+Âní&UÅüV
·³Í“PĞóùóƒ,Fœµÿ1mw:ÃËóÉÿ%%L¥ °—ì¹ƒiêK´^•Œ=P®†)ØQò¼…ÑtÀ«šGPÍÖ‚=°všA˜‘u˜Y2G¡hzèœ²Cˆz“,'ÉBËîFëyê9ëbÚƒC"Ï§Gî\Ş>Nº3"uğ×ÕÃÒª"+$–•tNRbÏ bK¿[‹è—˜'ı„Tq£í6%i\[×É“£«"¦â]¹Šù:›ßk“ÆŸƒ•E;œ™)4Ğ‚¸éiA>{ïz27P[Çñƒª¥çâam7:kvwşQ’{9‡øÌÑš/Ba#e¿Œ2GE³?ëÆuL-Kçâûë`Î¾úZR3èZ†VrBU4æ¾ö>ˆ!¬¥¸·Ù&ëAB«7D]¯ïà{‚iÉrtÃ
ì¡s!Æ6È~ò‹h ~ø*yd=†Ğ„«cŒœB«ãsìªM€qY ¶íü(?EÍ¸ã¦—,PÆ.Õ2Ò›ĞL“ÁÄ=Zõ¹£'åaû—V
òooWzG­Xo|&†Ò¬tÔÆõ&Ï9Û¯üÂì·™q1¥HM…jôdû 1Ë*D&wâMí„NÕûà€“N4U‡*SSO`¨! .…]á©-Ï2!LVî“#^QšnÙñ64÷d,V±Ğ¨\İ%-SáA‰È3B¿¶#/©’Ş.¨Ó;°Bíôî6!ó^Q0¡Ú£©AzE6%È‚GULp.kpmçb›2êÁÇ““üR­Ão½AFc\Ä¿¶…5&µ‹Í1¿àmÿ«·1­“4$©Ï®n”Rv¡‰ÚZ{+Lh¹ğ>b&ÙR!–P—Úå¤}úôÿ   ÿÿ ÿz˜¯   ÿÿ ÓBT¼