/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;
import static org.telegram.messenger.NotificationsController.TYPE_CHANNEL;
import static org.telegram.messenger.NotificationsController.TYPE_PRIVATE;
import static org.telegram.messenger.NotificationsController.TYPE_REACTIONS_MESSAGES;
import static org.telegram.messenger.Utilities.tryParseLong;
import static org.telegram.ui.Stars.StarsController.findAttribute;
import static org.telegram.ui.Stories.HighlightMessageSheet.parseTiers;
import static org.telegram.ui.Stories.HighlightMessageSheet.parseTiersString;
import static org.telegram.ui.Stories.HighlightMessageSheet.tiersEqual;
import static org.telegram.ui.Stories.HighlightMessageSheet.tiersToString;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Pair;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.util.Consumer;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteDatabase;
import org.telegram.SQLite.SQLiteException;
import org.telegram.SQLite.SQLitePreparedStatement;
import org.telegram.messenger.browser.Browser;
import org.telegram.messenger.support.LongSparseIntArray;
import org.telegram.messenger.support.LongSparseLongArray;
import org.telegram.messenger.utils.EphemeralMessagesHelper;
import org.telegram.messenger.voip.GroupCallMessagesController;
import org.telegram.messenger.voip.VoIPDebugToSend;
import org.telegram.messenger.voip.VoIPPreNotificationService;
import org.telegram.messenger.voip.VoIPService;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLMethod;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.Vector;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.tgnet.tl.TL_bots;
import org.telegram.tgnet.tl.TL_communities;
import org.telegram.tgnet.tl.TL_ephemeral;
import org.telegram.tgnet.tl.TL_forum;
import org.telegram.tgnet.tl.TL_phone;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.tgnet.tl.TL_stories;
import org.telegram.tgnet.tl.TL_chatlists;
import org.telegram.tgnet.tl.TL_update;
import org.telegram.ui.ActionBar.ActionBarLayout;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Business.QuickRepliesController;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.ChatReactionsEditActivity;
import org.telegram.ui.ChatRightsEditActivity;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ImageUpdater;
import org.telegram.ui.Components.JoinCallAlert;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.MotionBackgroundDrawable;
import org.telegram.ui.Components.Premium.LimitReachedBottomSheet;
import org.telegram.ui.Components.Reactions.ReactionsLayoutInBubble;
import org.telegram.ui.Components.SwipeGestureSettingsView;
import org.telegram.ui.Components.TranscribeButton;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.EditWidgetActivity;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.MainTabsActivity;
import org.telegram.ui.PremiumPreviewFragment;
import org.telegram.ui.ProfileActivity;
import org.telegram.ui.SecretMediaViewer;
import org.telegram.ui.Stars.BotStarsController;
import org.telegram.ui.Stars.StarsController;
import org.telegram.ui.Stories.StoriesController;
import org.telegram.ui.ThemeActivity;
import org.telegram.ui.TopicsFragment;
import org.telegram.ui.bots.BotWebViewAttachedSheet;
import org.telegram.ui.bots.BotWebViewSheet;
import org.telegram.ui.bots.WebViewRequestProps;
import org.telegram.ui.community.CommunityChatType;
import org.telegram.ui.community.CommunityUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import me.vkryl.core.BitwiseUtils;

public class MessagesController extends BaseController implements NotificationCenter.NotificationCenterDelegate {

    public int lastKnownSessionsCount;
    private final ConcurrentHashMap<Long, TLRPC.Chat> chats = new ConcurrentHashMap<>(100, 1.0f, 2);
    private final ConcurrentHashMap<Integer, TLRPC.EncryptedChat> encryptedChats = new ConcurrentHashMap<>(10, 1.0f, 2);
    private final ConcurrentHashMap<Long, TLRPC.User> users = new ConcurrentHashMap<>(100, 1.0f, 3);
    private final ConcurrentHashMap<String, TLObject> objectsByUsernames = new ConcurrentHashMap<>(100, 1.0f, 2);
    private final ConcurrentHashMap<Long, Long> monoForumLinkedChannels = new ConcurrentHashMap<>(3, 1.0f, 2);
    public static int stableIdPointer = 100;

    private final HashMap<Long, TLRPC.Chat> activeVoiceChatsMap = new HashMap<>();

    private final ArrayList<Long> joiningToChannels = new ArrayList<>();

    private final LongSparseArray<TLRPC.TL_chatInviteExported> exportedChats = new LongSparseArray<>();

    public ArrayList<TLRPC.RecentMeUrl> hintDialogs = new ArrayList<>();
    public final SparseArray<ArrayList<TLRPC.Dialog>> dialogsByFolder = new SparseArray<>();
    private final LongSparseArray<ArrayList<TLRPC.Dialog>> dialogsByCommunity = new LongSparseArray<>();
    protected final ArrayList<TLRPC.Dialog> allDialogs = new ArrayList<>();
    public ArrayList<TLRPC.Dialog> dialogsForward = new ArrayList<>();
    public ArrayList<TLRPC.Dialog> dialogsServerOnly = new ArrayList<>();
    public ArrayList<TLRPC.Dialog> dialogsCanAddUsers = new ArrayList<>();
    public ArrayList<TLRPC.Dialog> dialogsMyChannels = new ArrayList<>();
    public ArrayList<TLRPC.Dialog> dialogsMyGroups = new ArrayList<>();
    public ArrayList<TLRPC.Dialog> dialogsChannelsOnly = new ArrayList<>();
    public ArrayList<TLRPC.Dialog> dialogsUsersOnly = new ArrayList<>();
    public ArrayList<TLRPC.Dialog> dialogsForBlock = new ArrayList<>();
    public ArrayList<TLRPC.Dialog> dialogsGroupsOnly = new ArrayList<>();
    public DialogFilter[] selectedDialogFilter = new DialogFilter[2];
    private int dialogsLoadedTillDate = Integer.MAX_VALUE;
    public int unreadUnmutedDialogs;
    public ConcurrentHashMap<Long, Integer> dialogs_read_inbox_max = new ConcurrentHashMap<>(100, 1.0f, 2);
    public ConcurrentHashMap<Long, Integer> dialogs_read_outbox_max = new ConcurrentHashMap<>(100, 1.0f, 2);
    public LongSparseArray<TLRPC.Dialog> dialogs_dict = new LongSparseArray<>();
    public LongSparseArray<ArrayList<MessageObject>> dialogMessage = new LongSparseArray<>();
    public LongSparseArray<MessageObject> dialogMessagesByRandomIds = new LongSparseArray<>();
    public LongSparseIntArray deletedHistory = new LongSparseIntArray();
    public SparseArray<MessageObject> dialogMessagesByIds = new SparseArray<>();
    public ConcurrentHashMap<Long, ConcurrentHashMap<Integer, ArrayList<PrintingUser>>> printingUsers = new ConcurrentHashMap<>(20, 1.0f, 2);
    public LongSparseArray<LongSparseArray<CharSequence>> printingStrings = new LongSparseArray<>();
    public LongSparseArray<LongSparseArray<Integer>> printingStringsTypes = new LongSparseArray<>();
    public LongSparseArray<LongSparseArray<Boolean>>[] sendingTypings = new LongSparseArray[12];
    public ConcurrentHashMap<Long, Integer> onlinePrivacy = new ConcurrentHashMap<>(20, 1.0f, 2);
    private LongSparseIntArray pendingUnreadCounter = new LongSparseIntArray();
    private int lastPrintingStringCount;
    private SparseArray<ChatlistUpdatesStat> chatlistFoldersUpdates = new SparseArray<>();
    public int largeQueueMaxActiveOperations = 2;
    public int smallQueueMaxActiveOperations = 5;
    public int stealthModeFuture;
    public int stealthModePast;
    public int stealthModeCooldown;
    public StoriesController storiesController;
    public SavedMessagesController savedMessagesController;
    public UnconfirmedAuthController unconfirmedAuthController;
    private boolean hasArchivedChats;
    private boolean hasStories;
    public long storiesChangelogUserId = 777000;
    private ChannelBoostsController channelBoostsControler;
    public long giveawayAddPeersMax = 10;
    public long giveawayPeriodMax = 7;
    public long giveawayCountriesMax = 10;
    public long giveawayBoostsPerPremium = 4;
    public long boostsPerSentGift = 3;

    public static TLRPC.Peer getPeerFromInputPeer(TLRPC.InputPeer peer) {
        if (peer.chat_id != 0) {
            TLRPC.TL_peerChat peerChat = new TLRPC.TL_peerChat();
            peerChat.chat_id = peer.chat_id;
            return peerChat;
        } else if (peer.channel_id != 0) {
            TLRPC.TL_peerChannel peerChannel = new TLRPC.TL_peerChannel();
            peerChannel.channel_id = peer.channel_id;
            return peerChannel;
        } else {
            TLRPC.TL_peerUser peerUser = new TLRPC.TL_peerUser();
            peerUser.user_id = peer.user_id;
            return peerUser;
        }
    }

    public ChannelBoostsController getBoostsController() {
        if (channelBoostsControler != null) {
            return channelBoostsControler;
        }
        synchronized (lockObjects[currentAccount]) {
            if (channelBoostsControler != null) {
                return channelBoostsControler;
            }
            channelBoostsControler = new ChannelBoostsController(currentAccount);
        }
        return channelBoostsControler;
    }

    class ChatlistUpdatesStat {
        public ChatlistUpdatesStat() {
            this.loading = true;
        }
        public ChatlistUpdatesStat(TL_chatlists.TL_chatlists_chatlistUpdates value) {
            this.lastRequestTime = System.currentTimeMillis();
            this.lastValue = value;
        }
        boolean loading = false;
        long lastRequestTime;
        TL_chatlists.TL_chatlists_chatlistUpdates lastValue;
    }

    private boolean dialogsInTransaction;

    private LongSparseArray<Boolean> loadingPeerSettings = new LongSparseArray<>();

    private ArrayList<Long> createdDialogIds = new ArrayList<>();
    private ArrayList<Long> createdScheduledDialogIds = new ArrayList<>();
    private ArrayList<Long> createdDialogMainThreadIds = new ArrayList<>();
    private ArrayList<Long> visibleDialogMainThreadIds = new ArrayList<>();
    private ArrayList<Long> visibleScheduledDialogMainThreadIds = new ArrayList<>();

    private LongSparseIntArray shortPollChannels = new LongSparseIntArray();
    private LongSparseArray<ArrayList<Integer>> needShortPollChannels = new LongSparseArray<>();
    private LongSparseIntArray shortPollOnlines = new LongSparseIntArray();
    private LongSparseArray<ArrayList<Integer>> needShortPollOnlines = new LongSparseArray<>();

    private LongSparseArray<TLRPC.Dialog> deletingDialogs = new LongSparseArray<>();
    private LongSparseArray<TLRPC.Dialog> clearingHistoryDialogs = new LongSparseArray<>();

    public boolean loadingBlockedPeers = false;
    public LongSparseIntArray blockePeers = new LongSparseIntArray();
    public int totalBlockedCount = -1;
    public boolean blockedEndReached;

    private LongSparseArray<ArrayList<Integer>> channelViewsToSend = new LongSparseArray<>();
    private LongSparseArray<SparseArray<MessageObject>> pollsToCheck = new LongSparseArray<>();
    private int pollsToCheckSize;
    private long lastViewsCheckTime;
    public SparseIntArray premiumFeaturesTypesToPosition = new SparseIntArray();
    public SparseIntArray businessFeaturesTypesToPosition = new SparseIntArray();
    
    public ArrayList<DialogFilter> dialogFilters = new ArrayList<>();
    public ArrayList<DialogFilter> frozenDialogFilters = null;
    public ArrayList<Long> hiddenUndoChats = new ArrayList<>();
    public SparseArray<DialogFilter> dialogFiltersById = new SparseArray<>();
    private boolean loadingSuggestedFilters;
    private boolean loadingRemoteFilters;
    public boolean dialogFiltersLoaded;
    public ArrayList<TLRPC.TL_dialogFilterSuggested> suggestedFilters = new ArrayList<>();

    private final LongSparseArray<ArrayList<TLRPC.Updates>> updatesQueueChannels = new LongSparseArray<>();
    private LongSparseLongArray updatesStartWaitTimeChannels = new LongSparseLongArray();
    private LongSparseIntArray channelsPts = new LongSparseIntArray();
    private LongSparseArray<Boolean> gettingDifferenceChannels = new LongSparseArray<>();
    private LongSparseArray<Boolean> gettingChatInviters = new LongSparseArray<>();

    private LongSparseArray<Boolean> gettingUnknownChannels = new LongSparseArray<>();
    private LongSparseArray<Boolean> gettingUnknownDialogs = new LongSparseArray<>();
    private LongSparseArray<Boolean> checkingLastMessagesDialogs = new LongSparseArray<>();

    private ArrayList<TLRPC.Updates> updatesQueueSeq = new ArrayList<>();
    private ArrayList<TLRPC.Updates> updatesQueuePts = new ArrayList<>();
    private ArrayList<TLRPC.Updates> updatesQueueQts = new ArrayList<>();
    private long updatesStartWaitTimeSeq;
    private long updatesStartWaitTimePts;
    private long updatesStartWaitTimeQts;
    private final LongSparseArray<TLRPC.UserFull> fullUsers = new LongSparseArray<>();
    private final LongSparseArray<TLRPC.ChatFull> fullChats = new LongSparseArray<>();
    private final LongSparseArray<ChatObject.Call> groupCalls = new LongSparseArray<>();
    private final LongSparseArray<ChatObject.Call> groupCallsByChatId = new LongSparseArray<>();
    public VoIPDebugToSend voipDebug;
    private final LongSparseArray<TLRPC.PeerSettings> userPeerSettings = new LongSparseArray<>();
    private HashSet<Long> loadingFullUsers = new HashSet<>();
    private LongSparseLongArray loadedFullUsers = new LongSparseLongArray();
    private HashSet<Long> loadingFullChats = new HashSet<>();
    private HashSet<Long> loadingGroupCalls = new HashSet<>();
    private HashSet<Long> loadingFullParticipants = new HashSet<>();
    private HashSet<Long> loadedFullParticipants = new HashSet<>();
    public LongSparseLongArray loadedFullChats = new LongSparseLongArray();
    private LongSparseArray<LongSparseArray<TLRPC.ChannelParticipant>> channelAdmins = new LongSparseArray<>();
    private LongSparseIntArray loadingChannelAdmins = new LongSparseIntArray();

    private SparseIntArray migratedChats = new SparseIntArray();

    private LongSparseArray<SponsoredMessagesInfo> sponsoredMessages = new LongSparseArray<>();
    private LongSparseArray<SendAsPeersInfo> sendAsPeers = new LongSparseArray<>();
    private LongSparseArray<SendAsPeersInfo> sendAsPeersLiveStories = new LongSparseArray<>();

    private HashMap<String, ArrayList<MessageObject>> reloadingWebpages = new HashMap<>();
    private LongSparseArray<ArrayList<MessageObject>> reloadingWebpagesPending = new LongSparseArray<>();
    private HashMap<String, ArrayList<MessageObject>> reloadingScheduledWebpages = new HashMap<>();
    private LongSparseArray<ArrayList<MessageObject>> reloadingScheduledWebpagesPending = new LongSparseArray<>();
    private HashMap<String, ArrayList<MessageObject>> reloadingSavedWebpages = new HashMap<>();
    private LongSparseArray<ArrayList<MessageObject>> reloadingSavedWebpagesPending = new LongSparseArray<>();

    private LongSparseArray<Long> lastScheduledServerQueryTime = new LongSparseArray<>();
    private LongSparseArray<Long> lastQuickReplyServerQueryTime = new LongSparseArray<>();
    private LongSparseArray<Long> lastWelcomeMessagesServerQueryTime = new LongSparseArray<>();
    private LongSparseArray<Long> lastSavedServerQueryTime = new LongSparseArray<>();
    private LongSparseArray<Long> lastServerQueryTime = new LongSparseArray<>();

    private LongSparseArray<ArrayList<Integer>> reloadingMessages = new LongSparseArray<>();

    private ArrayList<ReadTask> readTasks = new ArrayList<>();
    private LongSparseArray<ReadTask> readTasksMap = new LongSparseArray<>();
    private ArrayList<ReadTask> repliesReadTasks = new ArrayList<>();
    private HashMap<String, ReadTask> threadsReadTasksMap = new HashMap<>();

    private boolean gettingNewDeleteTask;
    private int currentDeletingTaskTime;
    private LongSparseArray<ArrayList<Integer>> currentDeletingTaskMids;
    private LongSparseArray<ArrayList<Integer>> currentDeletingTaskMediaMids;
    private Runnable currentDeleteTaskRunnable;

    public boolean dialogsLoaded;
    private SparseIntArray nextDialogsCacheOffset = new SparseIntArray();
    private SparseBooleanArray loadingDialogs = new SparseBooleanArray();
    private SparseBooleanArray dialogsEndReached = new SparseBooleanArray();
    private SparseBooleanArray serverDialogsEndReached = new SparseBooleanArray();

    private boolean loadingUnreadDialogs;
    private boolean migratingDialogs;
    public boolean gettingDifference;
    private boolean getDifferenceFirstSync = true;
    public boolean updatingState;
    public boolean firstGettingTask;
    public boolean registeringForPush;
    private long lastPushRegisterSendTime;
    private boolean resetingDialogs;
    private TLRPC.TL_messages_peerDialogs resetDialogsPinned;
    private TLRPC.messages_Dialogs resetDialogsAll;
    private SparseIntArray loadingPinnedDialogs = new SparseIntArray();

    public ArrayList<FaqSearchResult> faqSearchArray = new ArrayList<>();
    public TLRPC.WebPage faqWebPage;

    private int loadingNotificationSettings;
    private boolean loadingNotificationSignUpSettings;

    private int nextPromoInfoCheckTime;
    private boolean checkingPromoInfo;
    private int checkingPromoInfoRequestId;
    private int lastCheckPromoId;
    private TLRPC.Dialog promoDialog;
    private boolean isLeftPromoChannel;
    private long promoDialogId;
    public int promoDialogType;
    public String promoPsaMessage;
    public String promoPsaType;
    private String proxyDialogAddress;

    private boolean checkingTosUpdate;
    private int nextTosCheckTime;

    public int secretWebpagePreview;
    public boolean suggestContacts = true;

    private volatile static long lastThemeCheckTime;
    private Runnable themeCheckRunnable = Theme::checkAutoNightThemeConditions;

    private volatile static long lastPasswordCheckTime;
    private Runnable passwordCheckRunnable = () -> getUserConfig().checkSavedPassword();

    private long lastStatusUpdateTime;
    private int statusRequest;
    private int statusSettingState;
    private boolean offlineSent;
    private String uploadingAvatar;
    private final LongSparseArray<ArrayList<MessageObject>> welcomeMessages = new LongSparseArray<>();

    private HashMap<String, Object> uploadingThemes = new HashMap<>();

    public String uploadingWallpaper;
    public Theme.OverrideWallpaperInfo uploadingWallpaperInfo;

    private UserNameResolver userNameResolver;

    public ArrayList<DialogFilter> getDialogFilters() {
        if (frozenDialogFilters != null) {
            return frozenDialogFilters;
        }
        return dialogFilters;
    }

    private final CacheFetcher<Integer, TLRPC.TL_help_appConfig> appConfigFetcher = new CacheFetcher<Integer, TLRPC.TL_help_appConfig>() {
        @Override
        protected void getRemote(int currentAccount, Integer arguments, long hash, Utilities.Callback4<Boolean, TLRPC.TL_help_appConfig, Long, Boolean> onResult) {
            TLRPC.TL_help_getAppConfig req = new TLRPC.TL_help_getAppConfig();
            req.hash = (int) hash;
            getConnectionsManager().sendRequest(req, (res, err) -> {
                if (res instanceof TLRPC.TL_help_appConfigNotModified) {
                    onResult.run(true, null, 0L, true);
                } else if (res instanceof TLRPC.TL_help_appConfig) {
                    onResult.run(false, (TLRPC.TL_help_appConfig) res, (long) ((TLRPC.TL_help_appConfig) res).hash, true);
                } else {
                    FileLog.e("getting appconfig error " + (err != null ? err.code + " " + err.text : ""));
                    onResult.run(false, null, 0L, err == null || !(err.code == -2000 || err.code == -2001));
                }
            });
        }

        @Override
        protected void getLocal(int currentAccount, Integer arguments, Utilities.Callback2<Long, TLRPC.TL_help_appConfig> onResult) {
            getMessagesStorage().getStorageQueue().postRunnable(() -> {
                SQLiteCursor cursor = null;
                try {
                    SQLiteDatabase database = MessagesStorage.getInstance(currentAccount).getDatabase();
                    if (database != null) {
                        TLRPC.help_AppConfig maybeResult = null;
                        cursor = database.queryFinalized("SELECT data FROM app_config");
                        if (cursor.next()) {
                            NativeByteBuffer data = cursor.byteBufferValue(0);
                            if (data != null) {
                                maybeResult = TLRPC.help_AppConfig.TLdeserialize(data, data.readInt32(false), true);
                                data.reuse();
                            }
                        }

                        if (maybeResult instanceof TLRPC.TL_help_appConfig) {
                            TLRPC.TL_help_appConfig result = (TLRPC.TL_help_appConfig) maybeResult;
                            onResult.run((long) result.hash, result);
                        } else {
                            onResult.run(0L, null);
                        }
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                    onResult.run(0L, null);
                } finally {
                    if (cursor != null) {
                        cursor.dispose();
                    }
                }
            });
        }

        @Override
        protected void setLocal(int currentAccount, Integer arguments, TLRPC.TL_help_appConfig data, long hash) {
            MessagesStorage.getInstance(currentAccount).getStorageQueue().postRunnable(() -> {
                try {
                    SQLiteDatabase database = MessagesStorage.getInstance(currentAccount).getDatabase();
                    if (database != null) {
                        database.executeFast("DELETE FROM app_config").stepThis().dispose();
                        if (data != null) {
                            SQLitePreparedStatement state = database.executeFast("INSERT INTO app_config VALUES(?)");
                            state.requery();
                            NativeByteBuffer buffer = new NativeByteBuffer(data.getObjectSize());
                            data.serializeToStream(buffer);
                            state.bindByteBuffer(1, buffer);
                            state.step();
                            buffer.reuse();
                            state.dispose();
                        }
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            });
        }

        @Override
        protected boolean useCache(Integer arguments) {
            return false;
        }
    };

    public boolean enableJoined;
    public String linkPrefix;
    public int maxGroupCount;
    public int maxBroadcastCount = 100;
    public int maxMegagroupCount;
    public int minGroupConvertSize = 200;
    public int maxEditTime;
    public int ratingDecay;
    public int revokeTimeLimit;
    public int revokeTimePmLimit;
    public boolean canRevokePmInbox;
    public int maxRecentStickersCount;
    public int maxFaveStickersCount;
    public int maxRecentGifsCount;
    public int callReceiveTimeout;
    public int callRingTimeout;
    public int callConnectTimeout;
    public int callPacketTimeout;
    public int maxFolderPinnedDialogsCountDefault;
    public int maxFolderPinnedDialogsCountPremium;
    public int mapProvider;
    public int availableMapProviders;
    public int updateCheckDelay;
    public int chatReadMarkSizeThreshold;
    public int chatReadMarkExpirePeriod;
    public int pmReadDateExpirePeriod;
    public String mapKey;
    public int maxMessageLength;
    public int getMaxMessageLength() {
        return getUserConfig().isPremium() ? config.messageLengthLimitPremium.get() : config.messageLengthLimitDefault.get();
    }
    public int maxCaptionLength;
    public int roundVideoSize;
    public int roundVideoBitrate;
    public int roundAudioBitrate;
    public boolean blockedCountry;
    public boolean preloadFeaturedStickers;
    public String youtubePipType;
    public boolean keepAliveService;
    public boolean backgroundConnection;
    public float animatedEmojisZoom;
    public boolean filtersEnabled;
    public boolean getfileExperimentalParams;
    public boolean smsjobsStickyNotificationEnabled;
    public boolean collectDeviceStats;
    public boolean showFiltersTooltip;
    public String venueSearchBot;
    public String storyVenueSearchBot;
    public String gifSearchBot;
    public String imageSearchBot;
    public String dcDomainName;
    public int webFileDatacenterId;
    public String suggestedLangCode;
    public boolean qrLoginCamera;
    public boolean saveGifsWithStickers;
    private String installReferer;
    public Set<String> pendingSuggestions;
    public Set<String> dismissedSuggestions;
    public TLRPC.TL_pendingSuggestion customPendingSuggestion;
    public Set<String> exportUri;
    public Set<String> exportGroupUri;
    public Set<String> exportPrivateUri;
    public boolean autoarchiveAvailable;
    public int groupCallVideoMaxParticipants;
    public boolean suggestStickersApiOnly;
    public ArrayList<String> gifSearchEmojies = new ArrayList<>();
    public HashSet<String> diceEmojies;
    public Set<String> autologinDomains;
    public Set<String> authDomains;
    public String autologinToken;
    public HashMap<String, DiceFrameSuccess> diceSuccess = new HashMap<>();
    public HashMap<String, EmojiSound> emojiSounds = new HashMap<>();
    public HashMap<Long, ArrayList<TLRPC.TL_sendMessageEmojiInteraction>> emojiInteractions = new HashMap<>();
    public boolean remoteConfigLoaded;
    public int ringtoneDurationMax;
    public int ringtoneSizeMax;
    public boolean storiesExportNopublicLink;
    public int authorizationAutoconfirmPeriod;
    public int quoteLengthMax;
    public boolean giveawayGiftsPurchaseAvailable;
    public PeerColors peerColors;
    public PeerColors profilePeerColors;
    public int transcribeAudioTrialWeeklyNumber;
    public int transcribeAudioTrialDurationMax;
    public int transcribeAudioTrialCooldownUntil;
    public int transcribeAudioTrialCurrentNumber;
    public int recommendedChannelsLimitDefault;
    public int recommendedChannelsLimitPremium;
    public int boostsChannelLevelMax;
    public int channelRestrictSponsoredLevelMin;
    public int channelAutotranslationLevelMin;
    public Set<String> webAppAllowedProtocols;
    public Set<String> ignoreRestrictionReasons;
    public int channelsLimitDefault;
    public int channelsLimitPremium;
    public int savedGifsLimitDefault;
    public int savedGifsLimitPremium;
    public int stickersFavedLimitDefault;
    public int stickersFavedLimitPremium;
    public int maxPinnedDialogsCountDefault;
    public int maxPinnedDialogsCountPremium;
    public int dialogFiltersLimitDefault;
    public int dialogFiltersLimitPremium;
    public int dialogFiltersChatsLimitDefault;
    public int dialogFiltersChatsLimitPremium;
    public int dialogFiltersPinnedLimitDefault;
    public int dialogFiltersPinnedLimitPremium;
    public int publicLinksLimitDefault;
    public int publicLinksLimitPremium;
    public int captionLengthLimitDefault;
    public int captionLengthLimitPremium;
    public int storyCaptionLengthLimitDefault;
    public int storyCaptionLengthLimitPremium;
    public int aboutLengthLimitDefault;
    public int aboutLengthLimitPremium;
    public int reactionsUserMaxDefault;
    public int reactionsUserMaxPremium;
    public int reactionsInChatMax;
    public int forumUpgradeParticipantsMin;
    public int topicsPinnedLimit;
    public long telegramAntispamUserId;
    public int telegramAntispamGroupSizeMin;
    public int hiddenMembersGroupSizeMin;
    private int chatlistUpdatePeriod;
    public int storyExpiringLimitDefault;
    public int storyExpiringLimitPremium;
    public int storiesSentWeeklyLimitDefault;
    public int storiesSentWeeklyLimitPremium;
    public int storiesSentMonthlyLimitDefault;
    public int storiesSentMonthlyLimitPremium;
    public int storiesSuggestedReactionsLimitDefault;
    public int storiesSuggestedReactionsLimitPremium;
    public int channelBgIconLevelMin;
    public int channelProfileIconLevelMin;
    public int channelEmojiStatusLevelMin;
    public int channelWallpaperLevelMin;
    public int channelCustomWallpaperLevelMin;
    public int groupProfileBgIconLevelMin;
    public int groupEmojiStatusLevelMin;
    public int groupEmojiStickersLevelMin;
    public int groupWallpaperLevelMin;
    public int groupCustomWallpaperLevelMin;
    public int groupTranscribeLevelMin;
    public int quickRepliesLimit;
    public float uploadPremiumSpeedupUpload;
    public float uploadPremiumSpeedupDownload;
    public int uploadPremiumSpeedupNotifyPeriod;
    public int introTitleLengthLimit;
    public int introDescriptionLengthLimit;
    public int businessChatLinksLimit;
    public boolean channelRevenueWithdrawalEnabled;
    public boolean newNoncontactPeersRequirePremiumWithoutOwnpremium;
    public int reactionsUniqMax;
    public String premiumManageSubscriptionUrl;
    public boolean androidDisableRoundCamera2;
    public int storiesPinnedToTopCountMax;
    public boolean showAnnualPerMonth = false;
    public boolean canEditFactcheck;
    public int factcheckLengthLimit;
    public long starsRevenueWithdrawalMin;
    public long starsPaidPostAmountMax;
    public int botPreviewMediasMax;
    public String tonProxyAddress;
    public String weatherSearchUsername;
    public boolean storyWeatherPreload;
    public boolean starsGiftsEnabled;
    public boolean stargiftsBlocked;
    public long starsPaidReactionAmountMax;
    public long starsSubscriptionAmountMax;
    public float starsUsdSellRate1000;
    public float starsUsdWithdrawRate1000;
    public boolean sponsoredLinksInappAllow;
    public Set<String> starrefStartParamPrefixes = new HashSet<>();
    public boolean starrefProgramAllowed;
    public boolean starrefConnectAllowed;
    public int starrefMinCommissionPermille;
    public int starrefMaxCommissionPermille;
    public int botVerificationDescriptionLengthLimit;
    public long paidReactionsPrivacyTime;
    public Long paidReactionsPrivacy;
    public int savedDialogsPinnedLimitDefault;
    public int savedDialogsPinnedLimitPremium;
    public boolean savedViewAsChats;
    public boolean storyQualityFull;
    public int uploadMaxFileParts;
    public int uploadMaxFilePartsPremium;
    public String premiumBotUsername;
    public String premiumInvoiceSlug;
    public String verifyAgeBotUsername;
    public String verifyAgeCountry;
    public int verifyAgeMin;
    public int chatlistInvitesLimitDefault;
    public int chatlistInvitesLimitPremium;
    public int chatlistJoinedLimitDefault;
    public int chatlistJoinedLimitPremium;
    public String storiesPosting;
    public String storiesEntities;
    public int stargiftsMessageLengthMax;
    public int stargiftsConvertPeriodMax;
    public boolean videoIgnoreAltDocuments;
    public boolean disableBotFullscreenBlur;
    public String tonBlockchainExplorerUrl;
    public long starsPaidMessageAmountMax;
    public int starsPaidMessageCommissionPermille;
    public int stargiftsPinnedToTopLimit;
    public boolean starsPaidMessagesAvailable;
    public long freezeSinceDate;
    public long freezeUntilDate;
    public String freezeAppealUrl;
    public int conferenceCallSizeLimit;
    public boolean callRequestsDisabled;
    public int todoItemsMax;
    public int todoTitleLengthMax;
    public int todoItemLengthMax;
    public String translationsManualEnabled; // "enabled", "alternative", "system", "disabled"
    public String translationsAutoEnabled; // "enabled", "alternative", "system", "disabled"
    public HashSet<Long> whitelistedBots;
    public int[] starsGroupcallMessageLimits;
    public int starsGroupcallMessageAmountMax;
    public long tonStakeddiceStakeAmountMin;
    public long tonStakeddiceStakeAmountMax;
    public long[] tonStakediceStakeSuggestedAmounts;
    public int[][] stargiftsCraftAttributesPermilles;

    private final SharedPreferences notificationsPreferences;
    private final SharedPreferences mainPreferences;
    private final SharedPreferences emojiPreferences;

    public volatile boolean ignoreSetOnline;
    public boolean premiumLocked;
    public int transcribeButtonPressed;
    public boolean starsLocked;

    public boolean starsPurchaseAvailable() {
        return !starsLocked;
    }
    public boolean premiumFeaturesBlocked() {
        return premiumLocked && !getUserConfig().isPremium();
    }
    public boolean premiumPurchaseBlocked() {
        return premiumLocked;
    }

    public List<String> directPaymentsCurrency = new ArrayList<>();

    public NewMessageCallback newMessageCallback;

    private long recentEmojiStatusUpdateRunnableTimeout, recentEmojiStatusUpdateRunnableTime;
    private Runnable recentEmojiStatusUpdateRunnable;
    private final ConcurrentHashMap<Long, Integer> emojiStatusUntilValues = new ConcurrentHashMap<Long, Integer>();
    private TopicsController topicsController;
    private CacheByChatsController cacheByChatsController;
    private TranslateController translateController;
    private AiTonesController tonesController;
    public boolean uploadMarkupVideo;
    public boolean giftAttachMenuIcon;
    public boolean giftTextFieldIcon;

    public boolean isTranslationsManualEnabled() {
        return !"disabled".equals(translationsManualEnabled);
    }
    public boolean isTranslationsAutoEnabled() {
        return !"disabled".equals(translationsAutoEnabled);
    }

    public final AppGlobalConfig config = new AppGlobalConfig();

    public boolean enableGiftsInProfile;

    public int checkResetLangpack;
    public boolean folderTags;

    public void getNextReactionMention(long dialogId, long topicId, int count, Consumer<Integer> callback) {
        getNextReactionMentionInternal(dialogId, topicId, count, true, callback);
    }

    public void getNextPollVotesMention(long dialogId, long topicId, int count, Consumer<Integer> callback) {
        getNextReactionMentionInternal(dialogId, topicId, count, false, callback);
    }

    private void getNextReactionMentionInternal(long dialogId, long topicId, int count, final boolean isReactions, Consumer<Integer> callback) {
        final String tableMentionsForDialogs = isReactions ? "reaction_mentions" : "poll_votes_mentions";
        final String tableMentionsForTopics = isReactions ? "reaction_mentions_topics" : "poll_votes_mentions_topics";
        final boolean isVotes = !isReactions;

        final MessagesStorage messagesStorage = getMessagesStorage();
        messagesStorage.getStorageQueue().postRunnable(() -> {
            boolean needRequest = true;
            try {
                SQLiteCursor cursor;
                if (topicId != 0) {
                    cursor = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT message_id FROM %s WHERE state = 1 AND dialog_id = %d AND topic_id = %d LIMIT 1", tableMentionsForTopics ,dialogId, topicId));
                } else {
                    cursor = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT message_id FROM %s WHERE state = 1 AND dialog_id = %d LIMIT 1", tableMentionsForDialogs, dialogId));
                }
                int messageId = 0;
                if (cursor.next()) {
                    messageId = cursor.intValue(0);
                    needRequest = false;
                }
                cursor.dispose();
                if (messageId != 0) {
                    if (isReactions) {
                        getMessagesStorage().markMessageReactionsAsRead(dialogId, topicId, messageId);
                    }
                    if (isVotes) {
                        getMessagesStorage().markMessagePollVotesAsRead(dialogId, topicId, messageId);
                    }
                    int finalMessageId = messageId;
                    AndroidUtilities.runOnUIThread(() -> callback.accept(finalMessageId));
                }

            } catch (SQLiteException e) {
                e.printStackTrace();
            }
            if (needRequest) {
                final TLMethod<TLRPC.messages_Messages> request;
                if (isReactions) {
                    TLRPC.TL_messages_getUnreadReactions req = new TLRPC.TL_messages_getUnreadReactions();
                    req.peer = getMessagesController().getInputPeer(dialogId);
                    req.limit = 1;
                    req.add_offset = count - 1;
                    if (isMonoForum(dialogId) && topicId != 0) {
                        req.saved_peer_id = getInputPeer(topicId);
                        req.flags |= 2;
                    }
                    request = req;
                } else  {
                    TLRPC.TL_messages_getUnreadPollVotes req = new TLRPC.TL_messages_getUnreadPollVotes();
                    req.peer = getMessagesController().getInputPeer(dialogId);
                    req.limit = 1;
                    req.add_offset = count - 1;
                    request = req;
                }
                getConnectionsManager().sendRequestTyped(request, AndroidUtilities::runOnUIThread, (res, error) -> {
                    int messageId = 0;
                    if (error == null && res != null && res.messages != null && !res.messages.isEmpty()) {
                        messageId = res.messages.get(0).id;
                    }
                    int finalMessageId = messageId;
                    AndroidUtilities.runOnUIThread(() -> callback.accept(finalMessageId));
                });
            }
        });
    }

    public void updatePremium(boolean premium) {
        if (dialogFilters.isEmpty()) {
            return;
        }
        if (!premium) {
            if (!dialogFilters.get(0).isDefault()) {
                for (int i = 1; i < dialogFilters.size(); i++) {
                    if (dialogFilters.get(i).isDefault()) {
                        DialogFilter defaultFilter = dialogFilters.remove(i);
                        dialogFilters.add(0, defaultFilter);
                        break;
                    }
                }
            }
            lockFiltersInternal();
        } else {
            for (int i = 0; i < dialogFilters.size(); i++) {
                dialogFilters.get(i).locked = false;
            }
        }

        getMessagesStorage().saveDialogFiltersOrder();
        getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
        getStoriesController().onPremiumChanged();
    }

    public void lockFiltersInternal() {
        boolean changed = false;
        if (!getUserConfig().isPremium() && dialogFilters.size() - 1 > dialogFiltersLimitDefault) {
            int n = dialogFilters.size() - 1 - dialogFiltersLimitDefault;
            ArrayList<DialogFilter> filtersSortedById = new ArrayList<>(dialogFilters);
            Collections.reverse(filtersSortedById);
            for (int i = 0; i < filtersSortedById.size(); i++) {
                if (i < n) {
                    if (!filtersSortedById.get(i).locked) {
                        changed = true;
                    }
                    filtersSortedById.get(i).locked = true;
                } else {
                    if (filtersSortedById.get(i).locked) {
                        changed = true;
                    }
                    filtersSortedById.get(i).locked = false;
                }
            }
        }
        if (changed) {
            getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
        }
    }

    public int getCaptionMaxLengthLimit() {
        return getUserConfig().isPremium() ? captionLengthLimitPremium : captionLengthLimitDefault;
    }

    public int getAboutLimit() {
        return getUserConfig().isPremium() ? aboutLengthLimitPremium : aboutLengthLimitDefault;
    }

    public int getMaxUserReactionsCount() {
        return getUserConfig().isPremium() ? reactionsUserMaxPremium : reactionsUserMaxDefault;
    }

    public int getChatReactionsCount() {
        return getUserConfig().isPremium() ? reactionsInChatMax : 1;
    }

    public int getChatMaxUniqReactions(long dialogId) {
        TLRPC.ChatFull chatFull = MessagesController.getInstance(currentAccount).getChatFull(-dialogId);
        if (chatFull != null && (chatFull instanceof TLRPC.TL_chatFull ? (chatFull.flags & 1048576) != 0 : (chatFull.flags2 & 8192) != 0)) {
            return chatFull.reactions_limit;
        }
        return reactionsUniqMax;
    }

    public boolean isPremiumUser(TLRPC.User currentUser) {
        return currentUser != null && currentUser.premium && !isSupportUser(currentUser);
    }

    public boolean didPressTranscribeButtonEnough() {
        return transcribeButtonPressed >= 2;
    }

    public void pressTranscribeButton() {
        if (transcribeButtonPressed < 2) {
            transcribeButtonPressed++;
            if (mainPreferences != null) {
                mainPreferences.edit().putInt("transcribeButtonPressed", transcribeButtonPressed).apply();
            }
        }
    }

    public void putLastGiftAuctionUpdate() {
        if (mainPreferences != null) {
            mainPreferences.edit().putLong("lastGiftAuctionTimeUpdate", System.currentTimeMillis()).apply();
        }
    }

    public boolean giftAuctionUpdateWasRecently() {
        final long t = mainPreferences != null ? mainPreferences.getLong("lastGiftAuctionTimeUpdate", 0) : 0;
        return System.currentTimeMillis() - t < 86400 * 1000;
    }


    public ArrayList<TLRPC.TL_messages_stickerSet> filterPremiumStickers(ArrayList<TLRPC.TL_messages_stickerSet> stickerSets) {
        if (!premiumFeaturesBlocked()) {
            return stickerSets;
        }
        for (int i = 0; i < stickerSets.size(); i++) {
            TLRPC.TL_messages_stickerSet newSet = MessagesController.getInstance(currentAccount).filterPremiumStickers(stickerSets.get(i));
            if (newSet == null) {
                stickerSets.remove(i);
                i--;
            } else {
                stickerSets.set(i, newSet);
            }
        }
        return stickerSets;
    }

    public TLRPC.TL_messages_stickerSet filterPremiumStickers(TLRPC.TL_messages_stickerSet stickerSet) {
        if (!premiumFeaturesBlocked() || stickerSet == null) {
            return stickerSet;
        }
        try {

            boolean hasPremiumSticker = false;
            for (int i = 0; i < stickerSet.documents.size(); i++) {
                if (MessageObject.isPremiumSticker(stickerSet.documents.get(i))) {
                    hasPremiumSticker = true;
                    break;
                }
            }
            if (hasPremiumSticker) {
                NativeByteBuffer nativeByteBuffer = new NativeByteBuffer(stickerSet.getObjectSize());
                stickerSet.serializeToStream(nativeByteBuffer);
                nativeByteBuffer.position(0);
                TLRPC.TL_messages_stickerSet newStickersSet = new TLRPC.TL_messages_stickerSet();
                nativeByteBuffer.readInt32(true);
                newStickersSet.readParams(nativeByteBuffer, true);
                nativeByteBuffer.reuse();
                stickerSet = newStickersSet;

                for (int i = 0; i < stickerSet.documents.size(); i++) {
                    if (MessageObject.isPremiumSticker(stickerSet.documents.get(i))) {
                        stickerSet.documents.remove(i);
                        stickerSet.packs.remove(i);
                        i--;
                        if (stickerSet.documents.isEmpty()) {
                            return null;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stickerSet;
    }

    public TopicsController getTopicsController() {
        return topicsController;
    }

    public TranslateController getTranslateController() {
        return translateController;
    }

    public AiTonesController getTonesController() {
        if (tonesController == null) {
            tonesController = new AiTonesController(currentAccount);
        }
        return tonesController;
    }

    public boolean isCommunity(long dialogId) {
        if (dialogId < 0) {
            TLRPC.Chat chatLocal = getChat(-dialogId);
            return ChatObject.isCommunity(chatLocal);
        }
        return false;
    }

    public boolean isForum(long dialogId) {
        if (dialogId < 0) {
            TLRPC.Chat chatLocal = getChat(-dialogId);
            return chatLocal != null && chatLocal.forum;
        } else {
            TLRPC.User userLocal = getUser(dialogId);
            return UserObject.isBotForum(userLocal);
        }
    }

    public boolean isMonoForum(long dialogId) {
        TLRPC.Chat chatLocal = getChat(-dialogId);
        return chatLocal != null && chatLocal.monoforum;
    }

    public boolean isMonoForumWithManageRights(long dialogId) {
        TLRPC.Chat chatLocal = getChat(-dialogId);
        return ChatObject.isMonoForum(chatLocal) && ChatObject.canManageMonoForum(currentAccount, chatLocal);
    }

    public boolean isForum(MessageObject msg) {
        return msg != null && isForum(msg.getDialogId());
    }

    public boolean isForum(TLRPC.Message msg) {
        return msg != null && isForum(MessageObject.getDialogId(msg));
    }

    public void markAllTopicsAsRead(long did) {
        getMessagesStorage().loadTopics(did, topics -> {
            AndroidUtilities.runOnUIThread(() -> {
                if (topics != null) {
                    for (int i = 0; i < topics.size(); i++) {
                        TLRPC.TL_forumTopic topic = topics.get(i);
                        getMessagesController().markDialogAsRead(did, topic.top_message, 0, topic.topMessage != null ? topic.topMessage.date : 0, false, isMonoForum(did) ? DialogObject.getPeerDialogId(topic.from_id) : topic.id, 0, true, 0);
                        getMessagesStorage().updateRepliesMaxReadId(-did, isMonoForum(did) ? DialogObject.getPeerDialogId(topic.from_id) : topic.id, topic.top_message, 0, true);
                    }
                }
                getMessagesStorage().getStorageQueue().postRunnable(() -> {
                    getMessagesStorage().resetAllUnreadCounters(false);
                    AndroidUtilities.runOnUIThread(() -> {
                        getMessagesController().sortDialogs(null);
                        getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload, true);
                    });
                });
            });
        });
    }

    public SparseArray<ImageUpdater> photoSuggestion = new SparseArray<>();

    public String getFullName(long dialogId) {
        if (dialogId > 0) {
           TLRPC.User user = getUser(dialogId);
           if (user != null) {
               return ContactsController.formatName(user.first_name, user.last_name);
           }
        } else {
            TLRPC.Chat chat = getChat(-dialogId);
            if (chat != null) {
                return chat.title;
            }
        }
        return null;
    }

    public UserNameResolver getUserNameResolver() {
        if (userNameResolver == null) {
            userNameResolver = new UserNameResolver(currentAccount);
        }
        return userNameResolver;
    }

    public class SponsoredMessagesInfo {
        public ArrayList<MessageObject> messages;
        public Integer posts_between;
        public long loadTime;
        public boolean loading;
    }

    private class SendAsPeersInfo {
        private TLRPC.TL_channels_sendAsPeers sendAsPeers;
        private long loadTime;
        private boolean loading;
    }

    public static class FaqSearchResult {

        public String title;
        public String[] path;
        public String url;
        public int num;

        public FaqSearchResult(String t, String[] p, String u) {
            title = t;
            path = p;
            url = u;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof FaqSearchResult)) {
                return false;
            }
            FaqSearchResult result = (FaqSearchResult) obj;
            return title.equals(result.title);
        }

        @Override
        public String toString() {
            SerializedData data = new SerializedData();
            data.writeInt32(num);
            data.writeInt32(0);
            data.writeString(title);
            data.writeInt32(path != null ? path.length : 0);
            if (path != null) {
                for (int a = 0; a < path.length; a++) {
                    data.writeString(path[a]);
                }
            }
            data.writeString(url);
            return Utilities.bytesToHex(data.toByteArray());
        }
    }

    public static class EmojiSound {
        public long id;
        public long accessHash;
        public byte[] fileReference;

        public EmojiSound(long i, long ah, String fr) {
            id = i;
            accessHash = ah;
            fileReference = Base64.decode(fr, Base64.URL_SAFE);
        }

        public EmojiSound(long i, long ah, byte[] fr) {
            id = i;
            accessHash = ah;
            fileReference = fr;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof EmojiSound)) {
                return false;
            }
            EmojiSound emojiSound = (EmojiSound) obj;
            return id == emojiSound.id && accessHash == emojiSound.accessHash && Arrays.equals(fileReference, emojiSound.fileReference);
        }
    }

    public void clearQueryTime() {
        lastServerQueryTime.clear();
        lastScheduledServerQueryTime.clear();
        lastQuickReplyServerQueryTime.clear();
        lastWelcomeMessagesServerQueryTime.clear();
        lastSavedServerQueryTime.clear();
    }

    public static class DiceFrameSuccess {
        public int frame;
        public int num;

        public DiceFrameSuccess(int f, int n) {
            frame = f;
            num = n;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof DiceFrameSuccess)) {
                return false;
            }
            DiceFrameSuccess frameSuccess = (DiceFrameSuccess) obj;
            return frame == frameSuccess.frame && num == frameSuccess.num;
        }
    }

    private static class UserActionUpdatesSeq extends TLRPC.Updates {

    }

    private static class UserActionUpdatesPts extends TLRPC.Updates {

    }

    public static int UPDATE_MASK_NAME = 1;
    public static int UPDATE_MASK_AVATAR = 2;
    public static int UPDATE_MASK_STATUS = 4;
    public static int UPDATE_MASK_CHAT_AVATAR = 8;
    public static int UPDATE_MASK_CHAT_NAME = 16;
    public static int UPDATE_MASK_CHAT_MEMBERS = 32;
    public static int UPDATE_MASK_USER_PRINT = 64;
    public static int UPDATE_MASK_USER_PHONE = 128;
    public static int UPDATE_MASK_READ_DIALOG_MESSAGE = 256;
    public static int UPDATE_MASK_SELECT_DIALOG = 512;
    public static int UPDATE_MASK_PHONE = 1024;
    public static int UPDATE_MASK_NEW_MESSAGE = 2048;
    public static int UPDATE_MASK_SEND_STATE = 4096;
    public static int UPDATE_MASK_CHAT = 8192;
    //public static int UPDATE_MASK_CHAT_ADMINS = 16384;
    public static int UPDATE_MASK_MESSAGE_TEXT = 32768;
    public static int UPDATE_MASK_CHECK = 65536;
    public static int UPDATE_MASK_REORDER = 131072;
    public static int UPDATE_MASK_EMOJI_INTERACTIONS = 262144;
    public static int UPDATE_MASK_EMOJI_STATUS = 524288;
    public static int UPDATE_MASK_REACTIONS_READ = 1048576;
    public static int UPDATE_MASK_ALL = UPDATE_MASK_AVATAR | UPDATE_MASK_STATUS | UPDATE_MASK_NAME | UPDATE_MASK_CHAT_AVATAR | UPDATE_MASK_CHAT_NAME | UPDATE_MASK_CHAT_MEMBERS | UPDATE_MASK_USER_PRINT | UPDATE_MASK_USER_PHONE | UPDATE_MASK_READ_DIALOG_MESSAGE | UPDATE_MASK_PHONE | UPDATE_MASK_REACTIONS_READ;

    public static int PROMO_TYPE_PROXY = 0;
    public static int PROMO_TYPE_PSA = 1;
    public static int PROMO_TYPE_OTHER = 2;

    private static class ReadTask {
        public long dialogId;
        public long replyId;
        public long monoForumPeerId;
        public int maxId;
        public int maxDate;
        public long sendRequestTime;
    }

    public static class PrintingUser {
        public long lastTime;
        public long userId;
        public TLRPC.SendMessageAction action;
    }

    public static int DIALOG_FILTER_FLAG_CONTACTS = 0x00000001;
    public static int DIALOG_FILTER_FLAG_NON_CONTACTS = 0x00000002;
    public static int DIALOG_FILTER_FLAG_GROUPS = 0x00000004;
    public static int DIALOG_FILTER_FLAG_CHANNELS = 0x00000008;
    public static int DIALOG_FILTER_FLAG_BOTS = 0x00000010;
    public static int DIALOG_FILTER_FLAG_EXCLUDE_MUTED = 0x00000020;
    public static int DIALOG_FILTER_FLAG_EXCLUDE_READ = 0x00000040;
    public static int DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED = 0x00000080;
    public static int DIALOG_FILTER_FLAG_ONLY_ARCHIVED = 0x00000100;
    public static int DIALOG_FILTER_FLAG_ALL_CHATS = DIALOG_FILTER_FLAG_CONTACTS | DIALOG_FILTER_FLAG_NON_CONTACTS | DIALOG_FILTER_FLAG_GROUPS | DIALOG_FILTER_FLAG_CHANNELS | DIALOG_FILTER_FLAG_BOTS;

    public static int DIALOG_FILTER_FLAG_CHATLIST = 0x00000200;
    public static int DIALOG_FILTER_FLAG_CHATLIST_ADMIN = 0x00000400;

    public static class DialogFilter {
        public int id;
        public String name;
        public ArrayList<TLRPC.MessageEntity> entities = new ArrayList<>();
        public int unreadCount;
        public volatile int pendingUnreadCount;
        public int order;
        public int flags;
        public ArrayList<Long> alwaysShow = new ArrayList<>();
        public ArrayList<Long> neverShow = new ArrayList<>();
        public LongSparseIntArray pinnedDialogs = new LongSparseIntArray();
        public ArrayList<TLRPC.Dialog> dialogs = new ArrayList<>();
        public ArrayList<TLRPC.Dialog> dialogsForward = new ArrayList<>();
        public int color;
        public boolean title_noanimate;

        public ArrayList<TL_chatlists.TL_exportedChatlistInvite> invites = null;

        private static int dialogFilterPointer = 10;
        public int localId = dialogFilterPointer++;
        public boolean locked;

        public boolean includesDialog(AccountInstance accountInstance, long dialogId) {
            MessagesController messagesController = accountInstance.getMessagesController();
            TLRPC.Dialog dialog = messagesController.dialogs_dict.get(dialogId);
            if (dialog == null) {
                return false;
            }
            return includesDialog(accountInstance, dialogId, dialog);
        }

        public boolean includesDialog(AccountInstance accountInstance, long dialogId, TLRPC.Dialog d) {
            if (neverShow.contains(dialogId)) {
                return false;
            }
            if (alwaysShow.contains(dialogId)) {
                return true;
            }
            if (d.folder_id != 0 && (flags & DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) != 0) {
                return false;
            }
            MessagesController messagesController = accountInstance.getMessagesController();
            ContactsController contactsController = accountInstance.getContactsController();
            boolean skip = false;

            if ((flags & DIALOG_FILTER_FLAG_EXCLUDE_MUTED) != 0 && messagesController.isDialogMuted(d.id, 0) && d.unread_mentions_count == 0 ||
                    (flags & DIALOG_FILTER_FLAG_EXCLUDE_READ) != 0 && messagesController.getDialogUnreadCount(d) == 0 && !d.unread_mark && d.unread_mentions_count == 0) {
                return false;
            }
            if (dialogId > 0) {
                TLRPC.User user = messagesController.getUser(dialogId);
                if (user != null) {
                    /*
                    if (ChatObject.isUserCollapsedInCommunity(accountInstance.getCurrentAccount(), user)) {
                        return false;
                    } else
                    */
                    if (!user.bot) {
                        if (user.self || user.contact || contactsController.isContact(dialogId)) {
                            if ((flags & DIALOG_FILTER_FLAG_CONTACTS) != 0) {
                                return true;
                            }
                        } else {
                            if ((flags & DIALOG_FILTER_FLAG_NON_CONTACTS) != 0) {
                                return true;
                            }
                        }
                    } else {
                        if ((flags & DIALOG_FILTER_FLAG_BOTS) != 0) {
                            return true;
                        }
                    }
                }
            } else if (dialogId < 0) {
                TLRPC.Chat chat = messagesController.getChat(-dialogId);
                if (chat != null) {
                    if (ChatObject.isCommunity(chat)) {
                        return (flags & DIALOG_FILTER_FLAG_ALL_CHATS) == DIALOG_FILTER_FLAG_ALL_CHATS;
                    } else if (ChatObject.isChatCollapsedInCommunity(accountInstance.getCurrentAccount(), chat)) {
                        return false;
                    } else if (ChatObject.isChannel(chat) && !chat.megagroup) {
                        if ((flags & DIALOG_FILTER_FLAG_CHANNELS) != 0) {
                            return true;
                        }
                    } else {
                        if ((flags & DIALOG_FILTER_FLAG_GROUPS) != 0) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        public boolean alwaysShow(int currentAccount, TLRPC.Dialog dialog) {
            if (dialog == null) {
                return false;
            }

            long dialogId = dialog.id;

            if (DialogObject.isEncryptedDialog(dialog.id)) {
                TLRPC.EncryptedChat encryptedChat = MessagesController.getInstance(currentAccount).getEncryptedChat(DialogObject.getEncryptedChatId(dialogId));
                if (encryptedChat != null) {
                    dialogId = encryptedChat.user_id;
                }
            }

            return alwaysShow.contains(dialogId);
        }

        public boolean isDefault() {
            return id == 0;
        }

        public boolean isChatlist() {
            return (flags & DIALOG_FILTER_FLAG_CHATLIST) > 0;
        }

        public boolean isMyChatlist() {
            return isChatlist() && (flags & DIALOG_FILTER_FLAG_CHATLIST_ADMIN) > 0;
        }
    }

    private DialogFilter sortingDialogFilter;
    private final Comparator<TLRPC.Dialog> dialogDateComparator = (dialog1, dialog2) -> {
        int pinnedNum1 = sortingDialogFilter == null ? Integer.MIN_VALUE : sortingDialogFilter.pinnedDialogs.get(dialog1.id, Integer.MIN_VALUE);
        int pinnedNum2 = sortingDialogFilter == null ? Integer.MIN_VALUE : sortingDialogFilter.pinnedDialogs.get(dialog2.id, Integer.MIN_VALUE);
        if (dialog1 instanceof TLRPC.TL_dialogFolder && !(dialog2 instanceof TLRPC.TL_dialogFolder)) {
            return -1;
        } else if (!(dialog1 instanceof TLRPC.TL_dialogFolder) && dialog2 instanceof TLRPC.TL_dialogFolder) {
            return 1;
        } else if (pinnedNum1 == Integer.MIN_VALUE && pinnedNum2 != Integer.MIN_VALUE) {
            return 1;
        } else if (pinnedNum1 != Integer.MIN_VALUE && pinnedNum2 == Integer.MIN_VALUE) {
            return -1;
        } else if (pinnedNum1 != Integer.MIN_VALUE) {
            if (pinnedNum1 > pinnedNum2) {
                return 1;
            } else if (pinnedNum1 < pinnedNum2) {
                return -1;
            } else {
                return 0;
            }
        }
        MediaDataController mediaDataController = getMediaDataController();
        long date1 = DialogObject.getLastMessageOrDraftDate(dialog1, mediaDataController.getDraft(dialog1.id, 0));
        long date2 = DialogObject.getLastMessageOrDraftDate(dialog2, mediaDataController.getDraft(dialog2.id, 0));
        if (date1 < date2) {
            return 1;
        } else if (date1 > date2) {
            return -1;
        }
        return 0;
    };

    public void sortDialogsList(ArrayList<TLRPC.Dialog> dialogs) {
        if (dialogs == null) {
            return;
        }
        Collections.sort(dialogs, dialogComparator);
    }

    private Comparator<TLRPC.Dialog> dialogComparator = (dialog1, dialog2) -> {
        if (dialog1 instanceof TLRPC.TL_dialogFolder && !(dialog2 instanceof TLRPC.TL_dialogFolder)) {
            return -1;
        } else if (!(dialog1 instanceof TLRPC.TL_dialogFolder) && dialog2 instanceof TLRPC.TL_dialogFolder) {
            return 1;
        } else if (!dialog1.pinned && dialog2.pinned) {
            return 1;
        } else if (dialog1.pinned && !dialog2.pinned) {
            return -1;
        } else if (dialog1.pinned) {
            if (dialog1.pinnedNum < dialog2.pinnedNum) {
                return 1;
            } else if (dialog1.pinnedNum > dialog2.pinnedNum) {
                return -1;
            } else {
                return 0;
            }
        }
        MediaDataController mediaDataController = getMediaDataController();
        long date1 = DialogObject.getLastMessageOrDraftDate(dialog1, mediaDataController.getDraft(dialog1.id, 0));
        long date2 = DialogObject.getLastMessageOrDraftDate(dialog2, mediaDataController.getDraft(dialog2.id, 0));
        if (date1 < date2) {
            return 1;
        } else if (date1 > date2) {
            return -1;
        }
        return 0;
    };

    private Comparator<CommunityPeerDialog> communityPeerDialogComparator = (peer1, peer2) -> {
        if (peer1.dialog != null && peer2.dialog != null) {
            return dialogComparator.compare(peer1.dialog, peer2.dialog);
        }
        if (peer2.dialog != null) {
            return 1;
        }
        if (peer1.dialog != null) {
            return -1;
        }
        return 0;
    };

    private Comparator<TLRPC.Update> updatesComparator = (lhs, rhs) -> {
        int ltype = getUpdateType(lhs);
        int rtype = getUpdateType(rhs);
        if (ltype != rtype) {
            return AndroidUtilities.compare(ltype, rtype);
        } else if (ltype == 0) {
            return AndroidUtilities.compare(getUpdatePts(lhs), getUpdatePts(rhs));
        } else if (ltype == 1) {
            return AndroidUtilities.compare(getUpdateQts(lhs), getUpdateQts(rhs));
        } else if (ltype == 2) {
            long lChannel = getUpdateChannelId(lhs);
            long rChannel = getUpdateChannelId(rhs);
            if (lChannel == rChannel) {
                return AndroidUtilities.compare(getUpdatePts(lhs), getUpdatePts(rhs));
            } else {
                return AndroidUtilities.compare(lChannel, rChannel);
            }
        }
        return 0;
    };

    private static volatile MessagesController[] Instance = new MessagesController[UserConfig.MAX_ACCOUNT_COUNT];
    private static final Object[] lockObjects = new Object[UserConfig.MAX_ACCOUNT_COUNT];
    static {
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            lockObjects[i] = new Object();
        }
    }

    public static MessagesController getInstance(int num) {
        MessagesController localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (lockObjects[num]) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new MessagesController(num);
                }
            }
        }
        return localInstance;
    }

    public SharedPreferences getMainSettings() {
        return mainPreferences;
    }

    public static SharedPreferences getNotificationsSettings(int account) {
        return getInstance(account).notificationsPreferences;
    }

    public static SharedPreferences getGlobalNotificationsSettings() {
        return getInstance(0).notificationsPreferences;
    }

    public static SharedPreferences getMainSettings(int account) {
        return getInstance(account).mainPreferences;
    }

    public static SharedPreferences getGlobalMainSettings() {
        return getInstance(0).mainPreferences;
    }

    public static SharedPreferences getEmojiSettings(int account) {
        return getInstance(account).emojiPreferences;
    }

    public static SharedPreferences getGlobalEmojiSettings() {
        return getInstance(0).emojiPreferences;
    }

    public MessagesController(int num) {
        super(num);
        ImageLoader.getInstance();
        getMessagesStorage();
        getLocationController();
        AndroidUtilities.runOnUIThread(() -> {
            MessagesController messagesController = getMessagesController();
            getNotificationCenter().addObserver(messagesController, NotificationCenter.fileUploaded);
            getNotificationCenter().addObserver(messagesController, NotificationCenter.fileUploadFailed);
            getNotificationCenter().addObserver(messagesController, NotificationCenter.fileUploadProgressChanged);
            getNotificationCenter().addObserver(messagesController, NotificationCenter.fileLoaded);
            getNotificationCenter().addObserver(messagesController, NotificationCenter.fileLoadFailed);
            getNotificationCenter().addObserver(messagesController, NotificationCenter.messageReceivedByServer);
            getNotificationCenter().addObserver(messagesController, NotificationCenter.updateMessageMedia);
        });
        addSupportUser();
        if (currentAccount == 0) {
            notificationsPreferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
            mainPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
            emojiPreferences = ApplicationLoader.applicationContext.getSharedPreferences("emoji", Activity.MODE_PRIVATE);
        } else {
            notificationsPreferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications" + currentAccount, Activity.MODE_PRIVATE);
            mainPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig" + currentAccount, Activity.MODE_PRIVATE);
            emojiPreferences = ApplicationLoader.applicationContext.getSharedPreferences("emoji" + currentAccount, Activity.MODE_PRIVATE);
        }
        long time = System.currentTimeMillis();

        remoteConfigLoaded = mainPreferences.getBoolean("remoteConfigLoaded", false);
        secretWebpagePreview = mainPreferences.getInt("secretWebpage2", 2);
        maxGroupCount = mainPreferences.getInt("maxGroupCount", 200);
        maxMegagroupCount = mainPreferences.getInt("maxMegagroupCount", 10000);
        maxRecentGifsCount = mainPreferences.getInt("maxRecentGifsCount", 200);
        maxRecentStickersCount = mainPreferences.getInt("maxRecentStickersCount", 30);
        maxFaveStickersCount = mainPreferences.getInt("maxFaveStickersCount", 5);
        maxEditTime = mainPreferences.getInt("maxEditTime", 3600);
        ratingDecay = mainPreferences.getInt("ratingDecay", 2419200);
        linkPrefix = mainPreferences.getString("linkPrefix", "t.me");
        callReceiveTimeout = mainPreferences.getInt("callReceiveTimeout", 20000);
        callRingTimeout = mainPreferences.getInt("callRingTimeout", 90000);
        callConnectTimeout = mainPreferences.getInt("callConnectTimeout", 30000);
        callPacketTimeout = mainPreferences.getInt("callPacketTimeout", 10000);
        updateCheckDelay = mainPreferences.getInt("updateCheckDelay", 24 * 60 * 60);
        maxFolderPinnedDialogsCountDefault = mainPreferences.getInt("maxFolderPinnedDialogsCountDefault", 100);
        maxFolderPinnedDialogsCountPremium = mainPreferences.getInt("maxFolderPinnedDialogsCountPremium", 100);
        maxMessageLength = mainPreferences.getInt("maxMessageLength", 4096);
        maxCaptionLength = mainPreferences.getInt("maxCaptionLength", 1024);
        mapProvider = mainPreferences.getInt("mapProvider", 0);
        availableMapProviders = mainPreferences.getInt("availableMapProviders", 3);
        mapKey = mainPreferences.getString("pk", null);
        installReferer = mainPreferences.getString("installReferer", null);
        revokeTimeLimit = mainPreferences.getInt("revokeTimeLimit", 2147483647);
        revokeTimePmLimit = mainPreferences.getInt("revokeTimePmLimit", 2147483647);
        canRevokePmInbox = mainPreferences.getBoolean("canRevokePmInbox", canRevokePmInbox);
        preloadFeaturedStickers = mainPreferences.getBoolean("preloadFeaturedStickers", false);
        youtubePipType = mainPreferences.getString("youtubePipType", "disabled");
        keepAliveService = mainPreferences.getBoolean("keepAliveService", false);
        backgroundConnection = mainPreferences.getBoolean("backgroundConnection", false);
        promoDialogId = mainPreferences.getLong("proxy_dialog", 0);
        nextPromoInfoCheckTime = mainPreferences.getInt("nextPromoInfoCheckTime", 0);
        promoDialogType = mainPreferences.getInt("promo_dialog_type", 0);
        promoPsaMessage = mainPreferences.getString("promo_psa_message", null);
        promoPsaType = mainPreferences.getString("promo_psa_type", null);
        proxyDialogAddress = mainPreferences.getString("proxyDialogAddress", null);
        venueSearchBot = mainPreferences.getString("venueSearchBot", "foursquare");
        storyVenueSearchBot = mainPreferences.getString("storyVenueSearchBot", "foursquare");
        gifSearchBot = mainPreferences.getString("gifSearchBot", "gif");
        imageSearchBot = mainPreferences.getString("imageSearchBot", "pic");
        blockedCountry = mainPreferences.getBoolean("blockedCountry", false);
        suggestedLangCode = mainPreferences.getString("suggestedLangCode", "en");
        animatedEmojisZoom = mainPreferences.getFloat("animatedEmojisZoom", 0.625f);
        qrLoginCamera = mainPreferences.getBoolean("qrLoginCamera", true);
        saveGifsWithStickers = mainPreferences.getBoolean("saveGifsWithStickers", false);
        filtersEnabled = mainPreferences.getBoolean("filtersEnabled", false);
        getfileExperimentalParams = mainPreferences.getBoolean("getfileExperimentalParams", false);
        smsjobsStickyNotificationEnabled = mainPreferences.getBoolean("smsjobsStickyNotificationEnabled", false);
        showFiltersTooltip = mainPreferences.getBoolean("showFiltersTooltip", false);
        autoarchiveAvailable = mainPreferences.getBoolean("autoarchiveAvailable", false);
        groupCallVideoMaxParticipants = mainPreferences.getInt("groipCallVideoMaxParticipants", 30);
        chatReadMarkSizeThreshold = mainPreferences.getInt("chatReadMarkSizeThreshold", 100);
        chatReadMarkExpirePeriod = mainPreferences.getInt("chatReadMarkExpirePeriod", 7 * 86400);
        ringtoneDurationMax = mainPreferences.getInt("ringtoneDurationMax", 5);
        ringtoneSizeMax = mainPreferences.getInt("ringtoneSizeMax", 1024_00);
        pmReadDateExpirePeriod = mainPreferences.getInt("pmReadDateExpirePeriod", 7 * 86400);
        suggestStickersApiOnly = mainPreferences.getBoolean("suggestStickersApiOnly", false);
        roundVideoSize = mainPreferences.getInt("roundVideoSize", 384);
        roundVideoBitrate = mainPreferences.getInt("roundVideoBitrate", 1000);
        roundAudioBitrate = mainPreferences.getInt("roundAudioBitrate", 64);
        pendingSuggestions = mainPreferences.getStringSet("pendingSuggestions", null);
        dismissedSuggestions = mainPreferences.getStringSet("dismissedSuggestions", null);
        channelsLimitDefault = mainPreferences.getInt("channelsLimitDefault", 500);
        channelsLimitPremium = mainPreferences.getInt("channelsLimitPremium", 2 * channelsLimitDefault);
        savedGifsLimitDefault = mainPreferences.getInt("savedGifsLimitDefault", 200);
        savedGifsLimitPremium = mainPreferences.getInt("savedGifsLimitPremium", 400);
        stickersFavedLimitDefault = mainPreferences.getInt("stickersFavedLimitDefault", 5);
        stickersFavedLimitPremium = mainPreferences.getInt("stickersFavedLimitPremium", 200);
        maxPinnedDialogsCountDefault = mainPreferences.getInt("maxPinnedDialogsCountDefault", 5);
        maxPinnedDialogsCountPremium = mainPreferences.getInt("maxPinnedDialogsCountPremium", 5);
        maxPinnedDialogsCountDefault = mainPreferences.getInt("maxPinnedDialogsCountDefault", 5);
        maxPinnedDialogsCountPremium = mainPreferences.getInt("maxPinnedDialogsCountPremium", 5);
        dialogFiltersLimitDefault = mainPreferences.getInt("dialogFiltersLimitDefault", 10);
        dialogFiltersLimitPremium = mainPreferences.getInt("dialogFiltersLimitPremium", 20);
        dialogFiltersChatsLimitDefault = mainPreferences.getInt("dialogFiltersChatsLimitDefault", 100);
        dialogFiltersChatsLimitPremium = mainPreferences.getInt("dialogFiltersChatsLimitPremium", 200);
        dialogFiltersPinnedLimitDefault = mainPreferences.getInt("dialogFiltersPinnedLimitDefault", 5);
        dialogFiltersPinnedLimitPremium = mainPreferences.getInt("dialogFiltersPinnedLimitPremium", 10);
        publicLinksLimitDefault = mainPreferences.getInt("publicLinksLimitDefault", 10);
        publicLinksLimitPremium = mainPreferences.getInt("publicLinksLimitPremium", 20);
        captionLengthLimitDefault = mainPreferences.getInt("captionLengthLimitDefault", 1024);
        captionLengthLimitPremium = mainPreferences.getInt("captionLengthLimitPremium", 4096);
        storyCaptionLengthLimitDefault = mainPreferences.getInt("storyCaptionLengthLimit", 200);
        storyCaptionLengthLimitPremium = mainPreferences.getInt("storyCaptionLengthLimitPremium", 2048);
        aboutLengthLimitDefault = mainPreferences.getInt("aboutLengthLimitDefault", 70);
        aboutLengthLimitPremium = mainPreferences.getInt("aboutLengthLimitPremium", 140);
        reactionsUserMaxDefault = mainPreferences.getInt("reactionsUserMaxDefault", 1);
        reactionsUserMaxPremium = mainPreferences.getInt("reactionsUserMaxPremium", 3);
        reactionsInChatMax = mainPreferences.getInt("reactionsInChatMax", 3);
        uploadMaxFileParts = mainPreferences.getInt("uploadMaxFileParts", (int) (FileLoader.DEFAULT_MAX_FILE_SIZE / 1024L / 512L));
        uploadMaxFilePartsPremium = mainPreferences.getInt("uploadMaxFilePartsPremium", uploadMaxFileParts * 2);
        premiumInvoiceSlug = mainPreferences.getString("premiumInvoiceSlug", null);
        verifyAgeBotUsername = mainPreferences.getString("verifyAgeBotUsername", null);
        verifyAgeCountry = mainPreferences.getString("verifyAgeCountry", "GB");
        verifyAgeMin = mainPreferences.getInt("verifyAgeMin", 18);
        premiumBotUsername = mainPreferences.getString("premiumBotUsername", null);
        premiumLocked = mainPreferences.getBoolean("premiumLocked", false);
        starsLocked = mainPreferences.getBoolean("starsLocked", true);
        transcribeButtonPressed = mainPreferences.getInt("transcribeButtonPressed", 0);
        forumUpgradeParticipantsMin = mainPreferences.getInt("forumUpgradeParticipantsMin", 200);
        topicsPinnedLimit = mainPreferences.getInt("topicsPinnedLimit", 3);
        telegramAntispamUserId = mainPreferences.getLong("telegramAntispamUserId", -1);
        telegramAntispamGroupSizeMin = mainPreferences.getInt("telegramAntispamGroupSizeMin", 100);
        hiddenMembersGroupSizeMin = mainPreferences.getInt("hiddenMembersGroupSizeMin", 100);
        chatlistUpdatePeriod = mainPreferences.getInt("chatlistUpdatePeriod", 3600);
        uploadMarkupVideo = mainPreferences.getBoolean("uploadMarkupVideo", true);
        giftAttachMenuIcon = mainPreferences.getBoolean("giftAttachMenuIcon", false);
        giftTextFieldIcon = mainPreferences.getBoolean("giftTextFieldIcon", false);
        checkResetLangpack = mainPreferences.getInt("checkResetLangpack", 0);
        smallQueueMaxActiveOperations = mainPreferences.getInt("smallQueueMaxActiveOperations", 5);
        largeQueueMaxActiveOperations = mainPreferences.getInt("largeQueueMaxActiveOperations", 2);
        stealthModeFuture = mainPreferences.getInt("stories_stealth_future_period", 25 * 60);
        storiesChangelogUserId = mainPreferences.getLong("stories_changelog_user_id", 777000);
        giveawayAddPeersMax = mainPreferences.getLong("giveaway_add_peers_max", 10);
        giveawayCountriesMax = mainPreferences.getLong("giveaway_countries_max", 10);
        giveawayBoostsPerPremium = mainPreferences.getLong("giveaway_boosts_per_premium", 4);
        boostsPerSentGift = mainPreferences.getLong("boosts_per_sent_gift", 3);
        giveawayPeriodMax = mainPreferences.getLong("giveaway_period_max", 7);
        stealthModePast = mainPreferences.getInt("stories_stealth_past_period", 5 * 60);
        stealthModeCooldown = mainPreferences.getInt("stories_stealth_cooldown_period", 60 * 60);
        boolean isTest = ConnectionsManager.native_isTestBackend(currentAccount) != 0;
        chatlistInvitesLimitDefault = mainPreferences.getInt("chatlistInvitesLimitDefault", 3);
        storyExpiringLimitDefault = mainPreferences.getInt("storyExpiringLimitDefault", 50);
        storyExpiringLimitPremium = mainPreferences.getInt("storyExpiringLimitPremium", 100);
        storiesSentWeeklyLimitDefault = mainPreferences.getInt("storiesSentWeeklyLimitDefault", 7);
        storiesSuggestedReactionsLimitDefault = mainPreferences.getInt("storiesSuggestedReactionsLimitDefault", 1);
        storiesSuggestedReactionsLimitPremium = mainPreferences.getInt("storiesSuggestedReactionsLimitPremium", 5);
        storiesSentWeeklyLimitPremium = mainPreferences.getInt("storiesSentWeeklyLimitPremium", 70);
        storiesSentMonthlyLimitDefault = mainPreferences.getInt("storiesSentMonthlyLimitDefault", 30);
        storiesSentMonthlyLimitPremium = mainPreferences.getInt("storiesSentMonthlyLimitPremium", 300);
        channelBgIconLevelMin = mainPreferences.getInt("channelBgIconLevelMin", 1);
        channelProfileIconLevelMin = mainPreferences.getInt("channelProfileIconLevelMin", 1);
        channelEmojiStatusLevelMin = mainPreferences.getInt("channelEmojiStatusLevelMin", 1);
        groupProfileBgIconLevelMin = mainPreferences.getInt("groupProfileBgIconLevelMin", 1);
        groupEmojiStatusLevelMin = mainPreferences.getInt("groupEmojiStatusLevelMin", 1);
        groupEmojiStickersLevelMin = mainPreferences.getInt("groupEmojiStickersLevelMin", 1);
        groupWallpaperLevelMin = mainPreferences.getInt("groupWallpaperLevelMin", 1);
        groupCustomWallpaperLevelMin = mainPreferences.getInt("groupCustomWallpaperLevelMin", 1);
        groupTranscribeLevelMin = mainPreferences.getInt("groupTranscribeLevelMin", 1);
        quickRepliesLimit = mainPreferences.getInt("quickRepliesLimit", 10);
        channelWallpaperLevelMin = mainPreferences.getInt("channelWallpaperLevelMin", 1);
        channelCustomWallpaperLevelMin = mainPreferences.getInt("channelCustomWallpaperLevelMin", 1);
        chatlistInvitesLimitPremium = mainPreferences.getInt("chatlistInvitesLimitPremium",  isTest ? 5 : 20);
        chatlistJoinedLimitDefault = mainPreferences.getInt("chatlistJoinedLimitDefault", 2);
        chatlistJoinedLimitPremium = mainPreferences.getInt("chatlistJoinedLimitPremium",  isTest ? 5 : 20);
        stargiftsMessageLengthMax = mainPreferences.getInt("stargiftsMessageLengthMax", 255);
        stargiftsConvertPeriodMax = mainPreferences.getInt("stargiftsConvertPeriodMax", isTest ? 300 : 90 * 86400);
        videoIgnoreAltDocuments = mainPreferences.getBoolean("videoIgnoreAltDocuments", false);
        disableBotFullscreenBlur = mainPreferences.getBoolean("disableBotFullscreenBlur", false);
        tonBlockchainExplorerUrl = mainPreferences.getString("tonBlockchainExplorerUrl", "https://tonviewer.com/");
        starsPaidMessageAmountMax = mainPreferences.getLong("starsPaidMessageAmountMax", 10_000L);
        starsPaidMessageCommissionPermille = mainPreferences.getInt("starsPaidMessageCommissionPermille", 850);
        stargiftsPinnedToTopLimit = mainPreferences.getInt("stargiftsPinnedToTopLimit", 6);
        starsPaidMessagesAvailable = mainPreferences.getBoolean("starsPaidMessagesAvailable", true);
        freezeSinceDate = mainPreferences.getLong("freezeSinceDate", 0L);
        freezeUntilDate = mainPreferences.getLong("freezeUntilDate", 0L);
        conferenceCallSizeLimit = mainPreferences.getInt("conferenceCallSizeLimit", isTest ? 5 : 100);
        callRequestsDisabled = mainPreferences.getBoolean("callRequestsDisabled", false);
        todoItemsMax = mainPreferences.getInt("todoItemsMax", isTest ? 10 : 30);
        todoTitleLengthMax = mainPreferences.getInt("todoTitleLengthMax", 32);
        todoItemLengthMax = mainPreferences.getInt("todoItemLengthMax", 64);
        translationsManualEnabled = mainPreferences.getString("translationsManualEnabled", "enabled");
        translationsAutoEnabled = mainPreferences.getString("translationsAutoEnabled", "enabled");
        whitelistedBots = mainPreferences.getStringSet("whitelistedBots", new HashSet<>()).stream().map(s -> tryParseLong(s, 0)).collect(Collectors.toCollection(HashSet::new));
        starsGroupcallMessageAmountMax = mainPreferences.getInt("starsGroupcallMessageAmountMax", 10_000);
        starsGroupcallMessageLimits = parseTiersString(mainPreferences.getString("starsGroupcallMessageLimits", null));
        freezeAppealUrl = mainPreferences.getString("freezeAppealUrl", "t.me/spambot");
        enableGiftsInProfile = mainPreferences.getBoolean("enableGiftsInProfile", true);
        storiesPosting = mainPreferences.getString("storiesPosting", "enabled");
        storiesEntities = mainPreferences.getString("storiesEntities", "premium");
        storiesExportNopublicLink = mainPreferences.getBoolean("storiesExportNopublicLink", false);
        authorizationAutoconfirmPeriod = mainPreferences.getInt("authorization_autoconfirm_period", 604800);
        quoteLengthMax = mainPreferences.getInt("quoteLengthMax", 1024);
        giveawayGiftsPurchaseAvailable = mainPreferences.getBoolean("giveawayGiftsPurchaseAvailable", false);
        peerColors = PeerColors.fromString(PeerColors.TYPE_NAME, mainPreferences.getString("peerColors", ""));
        profilePeerColors = PeerColors.fromString(PeerColors.TYPE_PROFILE, mainPreferences.getString("profilePeerColors", ""));
        transcribeAudioTrialWeeklyNumber = mainPreferences.getInt("transcribeAudioTrialWeeklyNumber", BuildVars.DEBUG_PRIVATE_VERSION ? 2 : 0);
        transcribeAudioTrialCurrentNumber = mainPreferences.getInt("transcribeAudioTrialCurrentNumber", transcribeAudioTrialWeeklyNumber);
        transcribeAudioTrialDurationMax = mainPreferences.getInt("transcribeAudioTrialDurationMax", 300);
        transcribeAudioTrialCooldownUntil = mainPreferences.getInt("transcribeAudioTrialCooldownUntil", 0);
        recommendedChannelsLimitDefault = mainPreferences.getInt("recommendedChannelsLimitDefault", 10);
        recommendedChannelsLimitPremium = mainPreferences.getInt("recommendedChannelsLimitPremium", 100);
        boostsChannelLevelMax = mainPreferences.getInt("boostsChannelLevelMax", 100);
        channelRestrictSponsoredLevelMin = mainPreferences.getInt("channelRestrictSponsoredLevelMin", 30);
        channelAutotranslationLevelMin = mainPreferences.getInt("channelAutotranslationLevelMin", 3);
        savedDialogsPinnedLimitDefault = mainPreferences.getInt("savedDialogsPinnedLimitDefault", 4);
        savedDialogsPinnedLimitPremium = mainPreferences.getInt("savedDialogsPinnedLimitPremium", 6);
        storyQualityFull = mainPreferences.getBoolean("storyQualityFull", true);
        savedViewAsChats = mainPreferences.getBoolean("savedViewAsChats", false);
        folderTags = mainPreferences.getBoolean("folderTags", false);
        uploadPremiumSpeedupUpload = mainPreferences.getFloat("uploadPremiumSpeedupUpload", 10.0f);
        uploadPremiumSpeedupDownload = mainPreferences.getFloat("uploadPremiumSpeedupDownload", 10.0f);
        uploadPremiumSpeedupNotifyPeriod = mainPreferences.getInt("uploadPremiumSpeedupNotifyPeriod2", 3600);
        introTitleLengthLimit = mainPreferences.getInt("introTitleLengthLimit", 32);
        introDescriptionLengthLimit = mainPreferences.getInt("introDescriptionLengthLimit", 72);
        businessChatLinksLimit = mainPreferences.getInt("businessChatLinksLimit", 100);
        channelRevenueWithdrawalEnabled = mainPreferences.getBoolean("channelRevenueWithdrawalEnabled", false);
        newNoncontactPeersRequirePremiumWithoutOwnpremium = mainPreferences.getBoolean("newNoncontactPeersRequirePremiumWithoutOwnpremium", false);
        reactionsUniqMax = mainPreferences.getInt("reactionsUniqMax", 11);
        premiumManageSubscriptionUrl = mainPreferences.getString("premiumManageSubscriptionUrl", ApplicationLoader.isStandaloneBuild() ? "https://t.me/premiumbot?start=status" : "https://play.google.com/store/account/subscriptions?sku=telegram_premium&package=org.telegram.messenger");
        androidDisableRoundCamera2 = mainPreferences.getBoolean("androidDisableRoundCamera2", true);
        storiesPinnedToTopCountMax = mainPreferences.getInt("storiesPinnedToTopCountMax", 3);
        showAnnualPerMonth = mainPreferences.getBoolean("showAnnualPerMonth", false);
        canEditFactcheck = mainPreferences.getBoolean("canEditFactcheck", false);
        factcheckLengthLimit = mainPreferences.getInt("factcheckLengthLimit", 1024);
        starsRevenueWithdrawalMin = mainPreferences.getLong("starsRevenueWithdrawalMin", 1000);
        starsPaidPostAmountMax = mainPreferences.getLong("starsPaidPostAmountMax", 10_000);
        botPreviewMediasMax = mainPreferences.getInt("botPreviewMediasMax", 10);
        webAppAllowedProtocols = mainPreferences.getStringSet("webAppAllowedProtocols", new HashSet<>(Arrays.asList("http", "https")));
        ignoreRestrictionReasons = mainPreferences.getStringSet("ignoreRestrictionReasons", new HashSet<>(Arrays.asList()));
        tonProxyAddress = mainPreferences.getString("tonProxyAddress", "magic.org");
        weatherSearchUsername = mainPreferences.getString("weatherSearchUsername", "izweatherbot");
        storyWeatherPreload = mainPreferences.getBoolean("storyWeatherPreload", true);
        starsGiftsEnabled = mainPreferences.getBoolean("starsGiftsEnabled", true);
        stargiftsBlocked = mainPreferences.getBoolean("stargiftsBlocked", true); // !BuildVars.DEBUG_VERSION);
        starsPaidReactionAmountMax = mainPreferences.getLong("starsPaidReactionAmountMax", 10_000L);
        starsSubscriptionAmountMax = mainPreferences.getLong("starsSubscriptionAmountMax", 2500L);
        starsUsdSellRate1000 = mainPreferences.getFloat("starsUsdSellRate1000", 2000);
        starsUsdWithdrawRate1000 = mainPreferences.getFloat("starsUsdWithdrawRate1000", 1200);
        sponsoredLinksInappAllow = mainPreferences.getBoolean("sponsoredLinksInappAllow", false);
        starrefProgramAllowed = mainPreferences.getBoolean("starrefProgramAllowed", false);
        starrefConnectAllowed = mainPreferences.getBoolean("starrefConnectAllowed", false);
        starrefStartParamPrefixes = mainPreferences.getStringSet("starrefStartParamPrefixes", new HashSet<>(Arrays.asList("_tgr_")));
        starrefMinCommissionPermille = mainPreferences.getInt("starrefMinCommissionPermille", 1);
        starrefMaxCommissionPermille = mainPreferences.getInt("starrefMaxCommissionPermille", 400);
        botVerificationDescriptionLengthLimit = mainPreferences.getInt("botVerificationDescriptionLengthLimit", 70);
        paidReactionsPrivacyTime = mainPreferences.getLong("paidReactionsAnonymousTime", 0);
        tonStakeddiceStakeAmountMin = mainPreferences.getLong("tonStakeddiceStakeAmountMin", 100000000L);
        tonStakeddiceStakeAmountMax = mainPreferences.getLong("tonStakeddiceStakeAmountMax", 50000000000L);
        tonStakediceStakeSuggestedAmounts = Arrays.stream(mainPreferences.getString("tonStakediceStakeSuggestedAmounts", "100000000,1000000000,2000000000,5000000000,10000000000,20000000000").split(",")).mapToLong(Long::parseLong).toArray();
        stargiftsCraftAttributesPermilles = Arrays.stream(mainPreferences.getString("stargiftsCraftAttributesPermilles", "90,,80,200,,70,190,460,,60,180,450,1000").split(",,"))
                .map(r -> Arrays.stream(r.split(","))
                    .mapToInt(Integer::parseInt)
                    .toArray())
                .toArray(int[][]::new);
        config.load(mainPreferences);

        final boolean paidReactionsActual = (System.currentTimeMillis() - paidReactionsPrivacyTime) < 1000 * 60 * 60 * 2;
        paidReactionsPrivacy = null;
        if ((System.currentTimeMillis() - paidReactionsPrivacyTime) < 1000 * 60 * 60 * 2) {
            if (mainPreferences.contains("paidReactionsDialogId")) {
                paidReactionsPrivacy = mainPreferences.getLong("paidReactionsDialogId", 0);
            } else {
                paidReactionsPrivacy = mainPreferences.getBoolean("paidReactionsAnonymous", false) ? UserObject.ANONYMOUS : 0;
            }
        }
        scheduleTranscriptionUpdate();
        BuildVars.GOOGLE_AUTH_CLIENT_ID = mainPreferences.getString("googleAuthClientId", BuildVars.GOOGLE_AUTH_CLIENT_ID);
        if (mainPreferences.contains("dcDomainName2")) {
            dcDomainName = mainPreferences.getString("dcDomainName2", "apv3.stel.com");
        } else {
            dcDomainName = isTest ? "tapv3.stel.com" : "apv3.stel.com";
        }
        if (mainPreferences.contains("webFileDatacenterId")) {
            webFileDatacenterId = mainPreferences.getInt("webFileDatacenterId", 4);
        } else {
            webFileDatacenterId = isTest ? 2 : 4;
        }

        Set<String> currencySet = mainPreferences.getStringSet("directPaymentsCurrency", null);
        if (currencySet != null) {
            directPaymentsCurrency.clear();
            directPaymentsCurrency.addAll(currencySet);
        }

        loadPremiumFeaturesPreviewOrder(premiumFeaturesTypesToPosition, mainPreferences.getString("premiumFeaturesTypesToPosition", null));
        loadPremiumFeaturesPreviewOrder(businessFeaturesTypesToPosition, mainPreferences.getString("businessFeaturesTypesToPosition", null));
        if (pendingSuggestions != null) {
            pendingSuggestions = new HashSet<>(pendingSuggestions);
        } else {
            pendingSuggestions = new HashSet<>();
        }
        if (dismissedSuggestions != null) {
            dismissedSuggestions = new HashSet<>(dismissedSuggestions);
        } else {
            dismissedSuggestions = new HashSet<>();
        }

        exportUri = mainPreferences.getStringSet("exportUri2", null);
        if (exportUri != null) {
            exportUri = new HashSet<>(exportUri);
        } else {
            exportUri = new HashSet<>();
            exportUri.add("content://(\\d+@)?com\\.whatsapp\\.provider\\.media/export_chat/");
            exportUri.add("content://(\\d+@)?com\\.whatsapp\\.w4b\\.provider\\.media/export_chat/");
            exportUri.add("content://jp\\.naver\\.line\\.android\\.line\\.common\\.FileProvider/export-chat/");
            exportUri.add(".*WhatsApp.*\\.txt$");
            exportUri.add(".*WhatsApp.*\\.zip$");
        }

        exportGroupUri = mainPreferences.getStringSet("exportGroupUri", null);
        if (exportGroupUri != null) {
            exportGroupUri = new HashSet<>(exportGroupUri);
        } else {
            exportGroupUri = new HashSet<>();
            exportGroupUri.add("@g.us/");
        }

        exportPrivateUri = mainPreferences.getStringSet("exportPrivateUri", null);
        if (exportPrivateUri != null) {
            exportPrivateUri = new HashSet<>(exportPrivateUri);
        } else {
            exportPrivateUri = new HashSet<>();
            exportPrivateUri.add("@s.whatsapp.net/");
        }

        autologinDomains = mainPreferences.getStringSet("autologinDomains", null);
        if (autologinDomains != null) {
            autologinDomains = new HashSet<>(autologinDomains);
        } else {
            autologinDomains = new HashSet<>();
        }

        authDomains = mainPreferences.getStringSet("authDomains", null);
        if (authDomains != null) {
            authDomains = new HashSet<>(authDomains);
        } else {
            authDomains = new HashSet<>();
        }

        autologinToken = mainPreferences.getString("autologinToken", null);

        Set<String> emojies = mainPreferences.getStringSet("diceEmojies", null);
        if (emojies == null) {
            diceEmojies = new HashSet<>();
            diceEmojies.add("\uD83C\uDFB2");
            diceEmojies.add("\uD83C\uDFAF");
        } else {
            diceEmojies = new HashSet<>(emojies);
        }
        String text = mainPreferences.getString("diceSuccess", null);
        if (text == null) {
            diceSuccess.put("\uD83C\uDFAF", new DiceFrameSuccess(62, 6));
        } else {
            try {
                byte[] bytes = Base64.decode(text, Base64.DEFAULT);
                if (bytes != null) {
                    SerializedData data = new SerializedData(bytes);
                    int count = data.readInt32(true);
                    for (int a = 0; a < count; a++) {
                        diceSuccess.put(data.readString(true), new DiceFrameSuccess(data.readInt32(true), data.readInt32(true)));
                    }
                    data.cleanup();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }

        text = mainPreferences.getString("emojiSounds", null);
        if (text != null) {
            try {
                byte[] bytes = Base64.decode(text, Base64.DEFAULT);
                if (bytes != null) {
                    SerializedData data = new SerializedData(bytes);
                    int count = data.readInt32(true);
                    for (int a = 0; a < count; a++) {
                        emojiSounds.put(data.readString(true), new EmojiSound(data.readInt64(true), data.readInt64(true), data.readByteArray(true)));
                    }
                    data.cleanup();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }

        text = mainPreferences.getString("gifSearchEmojies", null);
        if (text == null) {
            gifSearchEmojies.add("👍");
            gifSearchEmojies.add("👎");
            gifSearchEmojies.add("😍");
            gifSearchEmojies.add("😂");
            gifSearchEmojies.add("😮");
            gifSearchEmojies.add("🙄");
            gifSearchEmojies.add("😥");
            gifSearchEmojies.add("😡");
            gifSearchEmojies.add("🥳");
            gifSearchEmojies.add("😎");
        } else {
            try {
                byte[] bytes = Base64.decode(text, Base64.DEFAULT);
                if (bytes != null) {
                    SerializedData data = new SerializedData(bytes);
                    int count = data.readInt32(true);
                    for (int a = 0; a < count; a++) {
                        gifSearchEmojies.add(data.readString(true));
                    }
                    data.cleanup();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        AndroidUtilities.runOnUIThread(this::loadAppConfig, 2000);
        AndroidUtilities.runOnUIThread(this::loadWebBrowserConfig, 2000);
        AndroidUtilities.runOnUIThread(() -> checkPeerColors(false), 400);

        topicsController = new TopicsController(num);
        cacheByChatsController = new CacheByChatsController(num);
        translateController = new TranslateController(this);

        Utilities.globalQueue.postRunnable(() -> {
            enableJoined = notificationsPreferences.getBoolean("EnableContactJoined", true);
            nextTosCheckTime = notificationsPreferences.getInt("nextTosCheckTime", 0);
        });
    }


    private void sendLoadPeersRequest(TLObject req, ArrayList<TLObject> requests, TLRPC.messages_Dialogs pinnedDialogs, TLRPC.messages_Dialogs pinnedRemoteDialogs, ArrayList<TLRPC.User> users, ArrayList<TLRPC.Chat> chats, ArrayList<DialogFilter> filtersToSave, SparseArray<DialogFilter> filtersToDelete, ArrayList<Integer> filtersOrder, HashMap<Integer, HashSet<Long>> filterDialogRemovals, HashSet<Integer> filtersUnreadCounterReset, Runnable onDone) {
        getConnectionsManager().sendRequest(req, (response, error) -> {
            if (response instanceof TLRPC.TL_messages_chats) {
                TLRPC.TL_messages_chats res = (TLRPC.TL_messages_chats) response;
                chats.addAll(res.chats);
            } else if (response instanceof Vector) {
                Vector vector = (Vector) response;
                for (int a = 0, N = vector.objects.size(); a < N; a++) {
                    TLRPC.User user = (TLRPC.User) vector.objects.get(a);
                    users.add(user);
                }
            } else if (response instanceof TLRPC.TL_messages_peerDialogs) {
                TLRPC.TL_messages_peerDialogs peerDialogs = (TLRPC.TL_messages_peerDialogs) response;
                pinnedDialogs.dialogs.addAll(peerDialogs.dialogs);
                pinnedDialogs.messages.addAll(peerDialogs.messages);
                pinnedRemoteDialogs.dialogs.addAll(peerDialogs.dialogs);
                pinnedRemoteDialogs.messages.addAll(peerDialogs.messages);
                users.addAll(peerDialogs.users);
                chats.addAll(peerDialogs.chats);
            }
            requests.remove(req);
            if (requests.isEmpty()) {
                getMessagesStorage().processLoadedFilterPeers(pinnedDialogs, pinnedRemoteDialogs, users, chats, filtersToSave, filtersToDelete, filtersOrder, filterDialogRemovals, filtersUnreadCounterReset, onDone);
            }
        });
    }

    protected void loadFilterPeers(HashMap<Long, TLRPC.InputPeer> dialogsToLoadMap, HashMap<Long, TLRPC.InputPeer> usersToLoadMap, HashMap<Long, TLRPC.InputPeer> chatsToLoadMap, TLRPC.messages_Dialogs pinnedDialogs, TLRPC.messages_Dialogs pinnedRemoteDialogs, ArrayList<TLRPC.User> users, ArrayList<TLRPC.Chat> chats, ArrayList<DialogFilter> filtersToSave, SparseArray<DialogFilter> filtersToDelete, ArrayList<Integer> filtersOrder, HashMap<Integer, HashSet<Long>> filterDialogRemovals, HashSet<Integer> filtersUnreadCounterReset, Runnable onDone) {
        Utilities.stageQueue.postRunnable(() -> {
            ArrayList<TLObject> requests = new ArrayList<>();
            TLRPC.TL_users_getUsers req = null;
            for (HashMap.Entry<Long, TLRPC.InputPeer> entry : usersToLoadMap.entrySet()) {
                if (req == null) {
                    req = new TLRPC.TL_users_getUsers();
                    requests.add(req);
                }
                req.id.add(getInputUser(entry.getValue()));
                if (req.id.size() == 100) {
                    sendLoadPeersRequest(req, requests, pinnedDialogs, pinnedRemoteDialogs, users, chats, filtersToSave, filtersToDelete, filtersOrder, filterDialogRemovals, filtersUnreadCounterReset, onDone);
                    req = null;
                }
            }
            if (req != null) {
                sendLoadPeersRequest(req, requests, pinnedDialogs, pinnedRemoteDialogs, users, chats, filtersToSave, filtersToDelete, filtersOrder, filterDialogRemovals, filtersUnreadCounterReset, onDone);
            }
            TLRPC.TL_messages_getChats req2 = null;
            TLRPC.TL_channels_getChannels req3 = null;
            for (HashMap.Entry<Long, TLRPC.InputPeer> entry : chatsToLoadMap.entrySet()) {
                TLRPC.InputPeer inputPeer = entry.getValue();
                if (inputPeer.chat_id != 0) {
                    if (req2 == null) {
                        req2 = new TLRPC.TL_messages_getChats();
                        requests.add(req2);
                    }
                    req2.id.add(entry.getKey());
                    if (req2.id.size() == 100) {
                        sendLoadPeersRequest(req2, requests, pinnedDialogs, pinnedRemoteDialogs, users, chats, filtersToSave, filtersToDelete, filtersOrder, filterDialogRemovals, filtersUnreadCounterReset, onDone);
                        req2 = null;
                    }
                } else if (inputPeer.channel_id != 0) {
                    if (req3 == null) {
                        req3 = new TLRPC.TL_channels_getChannels();
                        requests.add(req3);
                    }
                    req3.id.add(getInputChannel(inputPeer));
                    if (req3.id.size() == 100) {
                        sendLoadPeersRequest(req3, requests, pinnedDialogs, pinnedRemoteDialogs, users, chats, filtersToSave, filtersToDelete, filtersOrder, filterDialogRemovals, filtersUnreadCounterReset, onDone);
                        req3 = null;
                    }
                }
            }
            if (req2 != null) {
                sendLoadPeersRequest(req2, requests, pinnedDialogs, pinnedRemoteDialogs, users, chats, filtersToSave, filtersToDelete, filtersOrder, filterDialogRemovals, filtersUnreadCounterReset, onDone);
            }
            if (req3 != null) {
                sendLoadPeersRequest(req3, requests, pinnedDialogs, pinnedRemoteDialogs, users, chats, filtersToSave, filtersToDelete, filtersOrder, filterDialogRemovals, filtersUnreadCounterReset, onDone);
            }

            TLRPC.TL_messages_getPeerDialogs req4 = null;
            for (HashMap.Entry<Long, TLRPC.InputPeer> entry : dialogsToLoadMap.entrySet()) {
                if (req4 == null) {
                    req4 = new TLRPC.TL_messages_getPeerDialogs();
                    requests.add(req4);
                }
                TLRPC.TL_inputDialogPeer inputDialogPeer = new TLRPC.TL_inputDialogPeer();
                inputDialogPeer.peer = entry.getValue();
                req4.peers.add(inputDialogPeer);
                if (req4.peers.size() == 100) {
                    sendLoadPeersRequest(req4, requests, pinnedDialogs, pinnedRemoteDialogs, users, chats, filtersToSave, filtersToDelete, filtersOrder, filterDialogRemovals, filtersUnreadCounterReset, onDone);
                    req4 = null;
                }
            }
            if (req4 != null) {
                sendLoadPeersRequest(req4, requests, pinnedDialogs, pinnedRemoteDialogs, users, chats, filtersToSave, filtersToDelete, filtersOrder, filterDialogRemovals, filtersUnreadCounterReset, onDone);
            }
        });
    }

    protected void processLoadedDialogFilters(ArrayList<DialogFilter> filters, TLRPC.messages_Dialogs pinnedDialogs, TLRPC.messages_Dialogs pinnedRemoteDialogs, ArrayList<TLRPC.User> users, ArrayList<TLRPC.Chat> chats, ArrayList<TLRPC.EncryptedChat> encryptedChats, int remote, Runnable onDone) {
        Utilities.stageQueue.postRunnable(() -> {

            LongSparseArray<TLRPC.Dialog> new_dialogs_dict = new LongSparseArray<>();
            SparseArray<TLRPC.EncryptedChat> enc_chats_dict;
            LongSparseArray<ArrayList<MessageObject>> new_dialogMessage = new LongSparseArray<>();
            LongSparseArray<TLRPC.User> usersDict = new LongSparseArray<>();
            LongSparseArray<TLRPC.Chat> chatsDict = new LongSparseArray<>();

            for (int a = 0; a < pinnedDialogs.users.size(); a++) {
                TLRPC.User u = pinnedDialogs.users.get(a);
                usersDict.put(u.id, u);
            }
            for (int a = 0; a < pinnedDialogs.chats.size(); a++) {
                TLRPC.Chat c = pinnedDialogs.chats.get(a);
                chatsDict.put(c.id, c);
            }
            if (encryptedChats != null) {
                enc_chats_dict = new SparseArray<>();
                for (int a = 0, N = encryptedChats.size(); a < N; a++) {
                    TLRPC.EncryptedChat encryptedChat = encryptedChats.get(a);
                    enc_chats_dict.put(encryptedChat.id, encryptedChat);
                }
            } else {
                enc_chats_dict = null;
            }

            ArrayList<MessageObject> newMessages = new ArrayList<>();
            for (int a = 0; a < pinnedDialogs.messages.size(); a++) {
                TLRPC.Message message = pinnedDialogs.messages.get(a);
                if (message.peer_id.channel_id != 0) {
                    TLRPC.Chat chat = chatsDict.get(message.peer_id.channel_id);
                    if (chat != null && chat.left && (promoDialogId == 0 || promoDialogId != -chat.id)) {
                        continue;
                    }
                } else if (message.peer_id.chat_id != 0) {
                    TLRPC.Chat chat = chatsDict.get(message.peer_id.chat_id);
                    if (chat != null && chat.migrated_to != null) {
                        continue;
                    }
                }
                MessageObject messageObject = new MessageObject(currentAccount, message, usersDict, chatsDict, false, false);
                newMessages.add(messageObject);
                long dialogId = messageObject.getDialogId();
                if (new_dialogMessage.containsKey(dialogId)) {
                    new_dialogMessage.get(dialogId).add(messageObject);
                } else {
                    ArrayList<MessageObject> arrayList = new ArrayList<>(1);
                    arrayList.add(messageObject);
                    new_dialogMessage.put(dialogId, arrayList);
                }
            }
            //getFileLoader().checkMediaExistance(newMessages);

            for (int a = 0; a < pinnedDialogs.dialogs.size(); a++) {
                TLRPC.Dialog d = pinnedDialogs.dialogs.get(a);
                DialogObject.initDialog(d);
                if (d.id == 0) {
                    continue;
                }
                if (DialogObject.isEncryptedDialog(d.id) && enc_chats_dict != null) {
                    if (enc_chats_dict.get(DialogObject.getEncryptedChatId(d.id)) == null) {
                        continue;
                    }
                }
                if (promoDialogId != 0 && promoDialogId == d.id) {
                    promoDialog = d;
                }
                if (d.last_message_date == 0) {
                    ArrayList<MessageObject> arrayList = new_dialogMessage.get(d.id);
                    if (arrayList != null) {
                        int maxDate = Integer.MIN_VALUE;
                        for (int i = 0; i < arrayList.size(); ++i) {
                            MessageObject msg = arrayList.get(i);
                            if (msg != null && msg.messageOwner != null && maxDate < msg.messageOwner.date) {
                                maxDate = msg.messageOwner.date;
                            }
                        }
                        if (maxDate > Integer.MIN_VALUE) {
                            d.last_message_date = maxDate;
                        }
                    }
                }
                if (DialogObject.isChannel(d)) {
                    TLRPC.Chat chat = chatsDict.get(-d.id);
                    if (chat != null) {
                        if (chat.left && (promoDialogId == 0 || promoDialogId != d.id)) {
                            continue;
                        }
                    }
                    channelsPts.put(-d.id, d.pts);
                } else if (d.id < 0) {
                    TLRPC.Chat chat = chatsDict.get(-d.id);
                    if (chat != null && chat.migrated_to != null) {
                        continue;
                    }
                }
                new_dialogs_dict.put(d.id, d);

                Integer value = dialogs_read_inbox_max.get(d.id);
                if (value == null) {
                    value = 0;
                }
                dialogs_read_inbox_max.put(d.id, Math.max(value, d.read_inbox_max_id));

                value = dialogs_read_outbox_max.get(d.id);
                if (value == null) {
                    value = 0;
                }
                dialogs_read_outbox_max.put(d.id, Math.max(value, d.read_outbox_max_id));
            }

            if (pinnedRemoteDialogs != null && !pinnedRemoteDialogs.dialogs.isEmpty()) {
                ImageLoader.saveMessagesThumbs(pinnedRemoteDialogs.messages);
                for (int a = 0; a < pinnedRemoteDialogs.messages.size(); a++) {
                    TLRPC.Message message = pinnedRemoteDialogs.messages.get(a);
                    if (message.action instanceof TLRPC.TL_messageActionChatDeleteUser) {
                        TLRPC.User user = usersDict.get(message.action.user_id);
                        if (user != null && user.bot) {
                            message.reply_markup = new TLRPC.TL_replyKeyboardHide();
                            message.flags |= 64;
                        }
                    }

                    if (message.action instanceof TLRPC.TL_messageActionChatMigrateTo || message.action instanceof TLRPC.TL_messageActionChannelCreate) {
                        message.unread = false;
                        message.media_unread = false;
                    } else {
                        ConcurrentHashMap<Long, Integer> read_max = message.out ? dialogs_read_outbox_max : dialogs_read_inbox_max;
                        Integer value = read_max.get(message.dialog_id);
                        if (value == null) {
                            value = getMessagesStorage().getDialogReadMax(message.out, message.dialog_id);
                            read_max.put(message.dialog_id, value);
                        }
                        message.unread = value < message.id;
                    }
                }
                getMessagesStorage().putDialogs(pinnedRemoteDialogs, 0);
            }

            AndroidUtilities.runOnUIThread(() -> {
                if (remote != 2) {
                    dialogFilters = filters;
                    dialogFiltersById.clear();
                    for (int a = 0, N = dialogFilters.size(); a < N; a++) {
                        DialogFilter filter = dialogFilters.get(a);
                        dialogFiltersById.put(filter.id, filter);
                    }
                    Collections.sort(dialogFilters, (o1, o2) -> {
                        if (o1.order > o2.order) {
                            return 1;
                        } else if (o1.order < o2.order) {
                            return -1;
                        }
                        return 0;
                    });
                    putUsers(users, true);
                    putChats(chats, true);
                    dialogFiltersLoaded = true;
                    getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
                    if (remote == 0) {
                        loadRemoteFilters(false);
                    }

                    if (pinnedRemoteDialogs != null && !pinnedRemoteDialogs.dialogs.isEmpty()) {
                        applyDialogsNotificationsSettings(pinnedRemoteDialogs.dialogs);
                    }

                    if (encryptedChats != null) {
                        for (int a = 0; a < encryptedChats.size(); a++) {
                            TLRPC.EncryptedChat encryptedChat = encryptedChats.get(a);
                            if (encryptedChat instanceof TLRPC.TL_encryptedChat && AndroidUtilities.getMyLayerVersion(encryptedChat.layer) < SecretChatHelper.CURRENT_SECRET_CHAT_LAYER) {
                                getSecretChatHelper().sendNotifyLayerMessage(encryptedChat, null);
                            }
                            putEncryptedChat(encryptedChat, true);
                        }
                    }

                    for (int a = 0; a < new_dialogs_dict.size(); a++) {
                        long key = new_dialogs_dict.keyAt(a);
                        TLRPC.Dialog newDialog = new_dialogs_dict.valueAt(a);
                        TLRPC.Dialog currentDialog = dialogs_dict.get(key);

                        if (pinnedRemoteDialogs != null && pinnedRemoteDialogs.dialogs.contains(newDialog)) {
                            if (newDialog.draft instanceof TLRPC.TL_draftMessage) {
                                getMediaDataController().saveDraft(newDialog.id, 0, newDialog.draft, null, false);
                            }
                            if (currentDialog != null) {
                                currentDialog.notify_settings = newDialog.notify_settings;
                            }
                        }

                        ArrayList<MessageObject> newMsgs = new_dialogMessage.get(newDialog.id);
                        if (currentDialog == null) {
                            dialogs_dict.put(key, newDialog);
                            dialogMessage.put(key, newMsgs);
                            if (newMsgs != null) {
                                for (int i = 0; i < newMsgs.size(); ++i) {
                                    MessageObject msg = newMsgs.get(i);
                                    if (msg != null && msg.messageOwner.peer_id.channel_id == 0) {
                                        dialogMessagesByIds.put(msg.getId(), msg);
                                        if (msg.messageOwner.random_id != 0) {
                                            dialogMessagesByRandomIds.put(msg.messageOwner.random_id, msg);
                                        }
                                    }
                                }
                            }
                            getTranslateController().checkDialogMessage(key);
                        } else {
                            currentDialog.pinned = newDialog.pinned;
                            currentDialog.pinnedNum = newDialog.pinnedNum;
                            ArrayList<MessageObject> oldMsgs = dialogMessage.get(key);
                            boolean oldMsgsDeleted = false;
                            for (int i = 0; oldMsgs != null && i < oldMsgs.size(); ++i) {
                                if (oldMsgs.get(i) != null && oldMsgs.get(i).deleted) {
                                    oldMsgsDeleted = true;
                                    break;
                                }
                            }
                            if (oldMsgsDeleted || oldMsgs == null || currentDialog.top_message > 0) {
                                if (newDialog.top_message >= currentDialog.top_message || (oldMsgs == null) != (newMsgs == null) || oldMsgs != null && newMsgs != null && oldMsgs.size() != newMsgs.size()) {
                                    dialogs_dict.put(key, newDialog);
                                    dialogMessage.put(key, newMsgs);
                                    if (oldMsgs != null) {
                                        for (int i = 0; i < oldMsgs.size(); ++i) {
                                            MessageObject oldMsg = oldMsgs.get(i);
                                            if (oldMsg == null) {
                                                continue;
                                            }
                                            if (oldMsg.messageOwner.peer_id.channel_id == 0) {
                                                dialogMessagesByIds.remove(oldMsg.getId());
                                            }
                                            if (oldMsg.messageOwner.random_id != 0) {
                                                dialogMessagesByRandomIds.remove(oldMsg.messageOwner.random_id);
                                            }
                                        }
                                    }
                                    if (newMsgs != null) {
                                        for (int i = 0; i < newMsgs.size(); ++i) {
                                            MessageObject newMsg = newMsgs.get(i);
                                            if (newMsg != null && newMsg.messageOwner.peer_id.channel_id == 0) {
                                                for (int j = 0; oldMsgs != null && j < oldMsgs.size(); ++j) {
                                                    MessageObject oldMsg = oldMsgs.get(j);
                                                    if (oldMsg != null && oldMsg.getId() == newMsg.getId()) {
                                                        newMsg.deleted = oldMsg.deleted;
                                                        break;
                                                    }
                                                }
                                                dialogMessagesByIds.put(newMsg.getId(), newMsg);
                                                if (newMsg.messageOwner.random_id != 0) {
                                                    dialogMessagesByRandomIds.put(newMsg.messageOwner.random_id, newMsg);
                                                }
                                            }
                                        }
                                    }
                                    getTranslateController().checkDialogMessage(key);
                                }
                            } else {
//                                if (newMsg == null || newMsg.messageOwner.date > oldMsg.messageOwner.date) {
                                dialogs_dict.put(key, newDialog);
                                dialogMessage.put(key, newMsgs);
                                if (oldMsgs != null) {
                                    for (int i = 0; i < oldMsgs.size(); ++i) {
                                        MessageObject oldMsg = oldMsgs.get(i);
                                        if (oldMsg == null) {
                                            continue;
                                        }
                                        if (oldMsg.messageOwner.peer_id.channel_id == 0) {
                                            dialogMessagesByIds.remove(oldMsg.getId());
                                        }
                                        if (oldMsg.messageOwner.random_id != 0) {
                                            dialogMessagesByRandomIds.remove(oldMsg.messageOwner.random_id);
                                        }
                                    }
                                }
                                if (newMsgs != null) {
                                    for (int i = 0; i < newMsgs.size(); ++i) {
                                        MessageObject newMsg = newMsgs.get(i);
                                        if (newMsg != null && newMsg.messageOwner.peer_id.channel_id == 0) {
                                            for (int j = 0; oldMsgs != null && j < oldMsgs.size(); ++j) {
                                                MessageObject oldMsg = oldMsgs.get(j);
                                                if (oldMsg != null && oldMsg.getId() == newMsg.getId()) {
                                                    newMsg.deleted = oldMsg.deleted;
                                                    break;
                                                }
                                            }
                                            dialogMessagesByIds.put(newMsg.getId(), newMsg);
                                            if (newMsg.messageOwner.random_id != 0) {
                                                dialogMessagesByRandomIds.put(newMsg.messageOwner.random_id, newMsg);
                                            }
                                        }
                                    }
                                }
                                getTranslateController().checkDialogMessage(key);
                            }
                        }
                    }

                    allDialogs.clear();
                    for (int a = 0, size = dialogs_dict.size(); a < size; a++) {
                        TLRPC.Dialog dialog = dialogs_dict.valueAt(a);
                        if (deletingDialogs.indexOfKey(dialog.id) >= 0) {
                            continue;
                        }
                        allDialogs.add(dialog);
                    }
                    sortDialogs(null);
                    getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
                }
                if (remote != 0) {
                    getUserConfig().filtersLoaded = true;
                    getUserConfig().saveConfig(false);
                    loadingRemoteFilters = false;
                    getNotificationCenter().postNotificationName(NotificationCenter.filterSettingsUpdated);
                }

                lockFiltersInternal();

                if (onDone != null) {
                    onDone.run();
                }
            });
        });
    }

    public void loadSuggestedFilters() {
        if (loadingSuggestedFilters) {
            return;
        }
        loadingSuggestedFilters = true;

        TLRPC.TL_messages_getSuggestedDialogFilters req = new TLRPC.TL_messages_getSuggestedDialogFilters();
        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            loadingSuggestedFilters = false;
            suggestedFilters.clear();
            if (response instanceof Vector) {
                suggestedFilters.addAll(((Vector<TLRPC.TL_dialogFilterSuggested>) response).objects);
            }
            getNotificationCenter().postNotificationName(NotificationCenter.suggestedFiltersLoaded);
        }));
    }

    private Utilities.Callback<Boolean> onLoadedRemoteFilters;

    public void loadRemoteFilters(boolean force) {
        loadRemoteFilters(force, null);
    }

    public void loadRemoteFilters(boolean force, Utilities.Callback<Boolean> whenDone) {
        if (whenDone != null) {
            onLoadedRemoteFilters = whenDone;
        }
        if (loadingRemoteFilters || !getUserConfig().isClientActivated() || !force && getUserConfig().filtersLoaded) {
            return;
        }
        if (force) {
            getUserConfig().filtersLoaded = false;
            getUserConfig().saveConfig(false);
        }
        TLRPC.TL_messages_getDialogFilters req = new TLRPC.TL_messages_getDialogFilters();
        getConnectionsManager().sendRequest(req, (response, error) -> {
            if (response instanceof Vector) {
                ArrayList<TLRPC.DialogFilter> filters = new ArrayList<>();
                Vector vector = (Vector) response;
                for (int i = 0; i < vector.objects.size(); ++i) {
                    filters.add((TLRPC.DialogFilter) vector.objects.get(i));
                }
                getMessagesStorage().checkLoadedRemoteFilters(filters, () -> {
                    if (onLoadedRemoteFilters != null) {
                        onLoadedRemoteFilters.run(true);
                        onLoadedRemoteFilters = null;
                    }
                });
            } else if (response instanceof TLRPC.TL_messages_dialogFilters) {
                TLRPC.TL_messages_dialogFilters res = (TLRPC.TL_messages_dialogFilters) response;
                if (folderTags != res.tags_enabled) {
                    setFolderTags(res.tags_enabled);
                    AndroidUtilities.runOnUIThread(() -> {
                        getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
                    });
                }
                getMessagesStorage().checkLoadedRemoteFilters(res.filters, () -> {
                    if (onLoadedRemoteFilters != null) {
                        onLoadedRemoteFilters.run(true);
                        onLoadedRemoteFilters = null;
                    }
                });
            } else {
                AndroidUtilities.runOnUIThread(() -> {
                    loadingRemoteFilters = false;
                    if (onLoadedRemoteFilters != null) {
                        onLoadedRemoteFilters.run(false);
                        onLoadedRemoteFilters = null;
                    }
                });
            }
        });
    }

    private boolean loggedDeviceStats;
    public void logDeviceStats() {
        if (collectDeviceStats && !loggedDeviceStats) {
            ArrayList<File> storageDirs = AndroidUtilities.getRootDirs();
            if (!storageDirs.isEmpty()) {
                String dir = storageDirs.get(0).getAbsolutePath();
                if (!TextUtils.isEmpty(SharedConfig.storageCacheDir)) {
                    for (int a = 0, N = storageDirs.size(); a < N; a++) {
                        String path = storageDirs.get(a).getAbsolutePath();
                        if (path.startsWith(SharedConfig.storageCacheDir)) {
                            dir = path;
                            break;
                        }
                    }
                }
                final boolean value = dir.contains("/storage/emulated/");

                TLRPC.TL_help_saveAppLog req = new TLRPC.TL_help_saveAppLog();
                TLRPC.TL_inputAppEvent event = new TLRPC.TL_inputAppEvent();
                event.time = getConnectionsManager().getCurrentTime();
                event.type = "android_sdcard_exists";
                TLRPC.TL_jsonBool bool = new TLRPC.TL_jsonBool();
                bool.value = value;
                event.data = bool;
                event.peer = value ? 1 : 0;
                req.events.add(event);

                getConnectionsManager().sendRequest(req, (response, error) -> {});
            }
            loggedDeviceStats = true;
        }
    }

    public void selectDialogFilter(DialogFilter filter, int index) {
        if (selectedDialogFilter[index] == filter) {
            return;
        }
        DialogFilter prevFilter = selectedDialogFilter[index];
        selectedDialogFilter[index] = filter;
        if (selectedDialogFilter[index == 0 ? 1 : 0] == filter) {
            selectedDialogFilter[index == 0 ? 1 : 0] = null;
        }
        if (selectedDialogFilter[index] == null) {
            if (prevFilter != null) {
                prevFilter.dialogs.clear();
                prevFilter.dialogsForward.clear();
            }
        } else {
            sortDialogs(null);
        }
    }

    public void onFilterUpdate(DialogFilter filter) {
        for (int a = 0; a < 2; a++) {
            if (selectedDialogFilter[a] == filter) {
                sortDialogs(null);
                getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload, true);
                break;
            }
        }
    }

    public void addFilter(DialogFilter filter, boolean atBegin) {
        if (atBegin) {
            int order = 254;
            for (int a = 0, N = dialogFilters.size(); a < N; a++) {
                order = Math.min(order, dialogFilters.get(a).order);
            }
            filter.order = order - 1;
            if (dialogFilters.get(0).isDefault()) {
                dialogFilters.add(1, filter);
            } else {
                dialogFilters.add(0, filter);
            }
        } else {
            int order = 0;
            for (int a = 0, N = dialogFilters.size(); a < N; a++) {
                order = Math.max(order, dialogFilters.get(a).order);
            }
            filter.order = order + 1;
            dialogFilters.add(filter);
        }
        dialogFiltersById.put(filter.id, filter);
        if (dialogFilters.size() == 1 && SharedConfig.getChatSwipeAction(currentAccount) != SwipeGestureSettingsView.SWIPE_GESTURE_FOLDERS) {
            SharedConfig.updateChatListSwipeSetting(SwipeGestureSettingsView.SWIPE_GESTURE_FOLDERS);
        }
        lockFiltersInternal();
    }

    public static TLRPC.TL_emojiStatusCollectible emojiStatusCollectibleFromGift(TL_stars.TL_starGiftUnique gift) {
        final TLRPC.TL_emojiStatusCollectible status = new TLRPC.TL_emojiStatusCollectible();
        status.collectible_id = gift.id;
        final TL_stars.starGiftAttributeModel model = findAttribute(gift.attributes, TL_stars.starGiftAttributeModel.class);
        final TL_stars.starGiftAttributeBackdrop backdrop = findAttribute(gift.attributes, TL_stars.starGiftAttributeBackdrop.class);
        final TL_stars.starGiftAttributePattern pattern = findAttribute(gift.attributes, TL_stars.starGiftAttributePattern.class);
        status.title = gift.title + " #" + gift.num;
        if (model != null) {
            status.document_id = model.document.id;
        }
        if (pattern != null) {
            status.pattern_document_id = pattern.document.id;
        }
        if (backdrop != null) {
            status.center_color = backdrop.center_color;
            status.edge_color = backdrop.edge_color;
            status.text_color = backdrop.text_color;
            status.pattern_color = backdrop.pattern_color;
        }
        return status;
    }

    public void updateEmojiStatus(TLRPC.EmojiStatus newStatus) {
        updateEmojiStatus(newStatus, null);
    }
    public void updateEmojiStatus(TLRPC.EmojiStatus newStatus, TL_stars.StarGift gift) {
        updateEmojiStatus(0, newStatus, gift);
    }

    public void updateEmojiStatus(long dialogId, TLRPC.EmojiStatus newStatus, TL_stars.StarGift gift) {
        final boolean myself = dialogId == 0 || dialogId == getUserConfig().getClientUserId();
        TLRPC.EmojiStatus new_emoji_status = newStatus;
        if (new_emoji_status instanceof TLRPC.TL_inputEmojiStatusCollectible && gift instanceof TL_stars.TL_starGiftUnique) {
            new_emoji_status = emojiStatusCollectibleFromGift((TL_stars.TL_starGiftUnique) gift);
        }

        TLObject r;
        if (myself) {
            TL_account.updateEmojiStatus req = new TL_account.updateEmojiStatus();
            req.emoji_status = newStatus;
            r = req;

            TLRPC.User user = getUserConfig().getCurrentUser();
            if (user != null) {
                user.emoji_status = new_emoji_status;
                getNotificationCenter().postNotificationName(NotificationCenter.userEmojiStatusUpdated, user);
            }
        } else {
            TLRPC.TL_channels_updateEmojiStatus req = new TLRPC.TL_channels_updateEmojiStatus();
            req.channel = getInputChannel(-dialogId);
            req.emoji_status = newStatus;
            r = req;

            TLRPC.Chat chat = getChat(-dialogId);
            if (chat != null) {
                chat.flags |= 512;
                chat.emoji_status = new_emoji_status;
                putChat(chat, true);
            }
        }
        getMessagesController().updateEmojiStatusUntilUpdate(dialogId, new_emoji_status);
        getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_EMOJI_STATUS);
        getConnectionsManager().sendRequest(r, null);
    }

    public void removeFilter(DialogFilter filter) {
        dialogFilters.remove(filter);
        dialogFiltersById.remove(filter.id);
        getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
    }

    private Runnable loadAppConfigRunnable = this::loadAppConfig;

    public void loadAppConfig() {
        loadAppConfig(true);
    }

    public void loadAppConfig(boolean force) {
        AndroidUtilities.cancelRunOnUIThread(loadAppConfigRunnable);
        if (force) {
            appConfigFetcher.forceRequest(currentAccount, 0);
        }
        appConfigFetcher.fetch(currentAccount, 0, config -> AndroidUtilities.runOnUIThread(() -> {
            if (config != null && config.config instanceof TLRPC.TL_jsonObject) {
                applyAppConfig((TLRPC.TL_jsonObject) config.config);
            }
            AndroidUtilities.cancelRunOnUIThread(loadAppConfigRunnable);
            AndroidUtilities.runOnUIThread(loadAppConfigRunnable, 4 * 60 * 1000 + 10);
        }));
    }

    private void applyAppConfig(TLRPC.TL_jsonObject object) {
        SharedPreferences.Editor editor = mainPreferences.edit();
        boolean changed = false;
        boolean storiesChanged = false;
        boolean keelAliveChanged = false;
        resetAppConfig();
        TLRPC.TL_jsonObject liteAppOptions = null;
        int transcribeAudioTrialWeeklyNumber = 0;
        int transcribeAudioTrialCooldownUntil = 0;

        changed = config.apply(editor, object);

        for (int a = 0, N = object.value.size(); a < N; a++) {
            TLRPC.TL_jsonObjectValue value = object.value.get(a);
            switch (value.key) {
                case "boosts_per_sent_gift": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        long val = (long) ((TLRPC.TL_jsonNumber) value.value).value;
                        if (val != boostsPerSentGift) {
                            boostsPerSentGift = val;
                            editor.putLong("boosts_per_sent_gift", boostsPerSentGift);
                            changed = true;
                        }
                    }
                    break;
                }
                case "giveaway_boosts_per_premium": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        long val = (long) ((TLRPC.TL_jsonNumber) value.value).value;
                        if (val != giveawayBoostsPerPremium) {
                            giveawayBoostsPerPremium = val;
                            editor.putLong("giveaway_boosts_per_premium", giveawayBoostsPerPremium);
                            changed = true;
                        }
                    }
                    break;
                }
                case "giveaway_period_max": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        long val = (long) ((TLRPC.TL_jsonNumber) value.value).value;
                        if (val != giveawayPeriodMax) {
                            giveawayPeriodMax = val;
                            editor.putLong("giveaway_period_max", giveawayPeriodMax);
                            changed = true;
                        }
                    }
                    break;
                }
                case "giveaway_add_peers_max": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        long val = (long) ((TLRPC.TL_jsonNumber) value.value).value;
                        if (val != giveawayAddPeersMax) {
                            giveawayAddPeersMax = val;
                            editor.putLong("giveaway_add_peers_max", giveawayAddPeersMax);
                            changed = true;
                        }
                    }
                    break;
                }
                case "giveaway_countries_max": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        long val = (long) ((TLRPC.TL_jsonNumber) value.value).value;
                        if (val != giveawayCountriesMax) {
                            giveawayCountriesMax = val;
                            editor.putLong("giveaway_countries_max", giveawayCountriesMax);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stories_changelog_user_id": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        storiesChangelogUserId = (long) ((TLRPC.TL_jsonNumber) value.value).value;
                        editor.putLong("stories_changelog_user_id", storiesChangelogUserId);
                    }
                    break;
                }
                case "stories_stealth_future_period": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        stealthModeFuture = (int) ((TLRPC.TL_jsonNumber) value.value).value;
                        editor.putInt("stories_stealth_future_period", stealthModeFuture);
                    }
                    break;
                }
                case "stories_stealth_past_period": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        stealthModePast = (int) ((TLRPC.TL_jsonNumber) value.value).value;
                        editor.putInt("stories_stealth_past_period", stealthModePast);
                    }
                    break;
                }
                case "stories_stealth_cooldown_period": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        stealthModeCooldown = (int) ((TLRPC.TL_jsonNumber) value.value).value;
                        editor.putInt("stories_stealth_cooldown_period", stealthModeCooldown);
                    }
                    break;
                }
                case "large_queue_max_active_operations_count": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        largeQueueMaxActiveOperations = (int) ((TLRPC.TL_jsonNumber) value.value).value;
                        editor.putInt("largeQueueMaxActiveOperations", largeQueueMaxActiveOperations);
                    }
                    break;
                }
                case "small_queue_max_active_operations_count": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        smallQueueMaxActiveOperations = (int) ((TLRPC.TL_jsonNumber) value.value).value;
                        editor.putInt("smallQueueMaxActiveOperations", smallQueueMaxActiveOperations);
                    }
                    break;
                }
                case "premium_gift_text_field_icon": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        if (giftTextFieldIcon != ((TLRPC.TL_jsonBool) value.value).value) {
                            giftTextFieldIcon = ((TLRPC.TL_jsonBool) value.value).value;
                            editor.putBoolean("giftTextFieldIcon", giftTextFieldIcon);
                            changed = true;

                            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.didUpdatePremiumGiftFieldIcon);
                        }
                    }
                    break;
                }
                case "premium_gift_attach_menu_icon": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        if (giftAttachMenuIcon != ((TLRPC.TL_jsonBool) value.value).value) {
                            giftAttachMenuIcon = ((TLRPC.TL_jsonBool) value.value).value;
                            editor.putBoolean("giftAttachMenuIcon", giftAttachMenuIcon);
                            changed = true;
                        }
                    }
                    break;
                }
                case "lite_app_options": {
                    if (value.value instanceof TLRPC.TL_jsonObject) {
                        liteAppOptions = (TLRPC.TL_jsonObject) value.value;
                    }
                    break;
                }
                case "lite_device_class": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        int performanceClass = (int) ((TLRPC.TL_jsonNumber) value.value).value;
                        if (performanceClass > 0) {
                            SharedConfig.overrideDevicePerformanceClass(performanceClass - 1);
                        }
                    }
                    break;
                }
                case "upload_markup_video": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        if (uploadMarkupVideo != ((TLRPC.TL_jsonBool) value.value).value) {
                            uploadMarkupVideo = ((TLRPC.TL_jsonBool) value.value).value;
                            editor.putBoolean("uploadMarkupVideo", uploadMarkupVideo);
                            changed = true;
                        }
                    }
                    break;
                }
                case "login_google_oauth_client_id": {
                    if (value.value instanceof TLRPC.TL_jsonString) {
                        String str = ((TLRPC.TL_jsonString) value.value).value;
                        if (!Objects.equals(BuildVars.GOOGLE_AUTH_CLIENT_ID, str)) {
                            BuildVars.GOOGLE_AUTH_CLIENT_ID = str;
                            editor.putString("googleAuthClientId", BuildVars.GOOGLE_AUTH_CLIENT_ID);
                            changed = true;
                        }
                    }
                    break;
                }
                case "premium_playmarket_direct_currency_list": {
                    if (value.value instanceof TLRPC.TL_jsonArray) {
                        TLRPC.TL_jsonArray arr = (TLRPC.TL_jsonArray) value.value;
                        HashSet<String> currencySet = new HashSet<>();
                        for (TLRPC.JSONValue el : arr.value) {
                            if (el instanceof TLRPC.TL_jsonString) {
                                TLRPC.TL_jsonString currencyEl = (TLRPC.TL_jsonString) el;
                                String currency = currencyEl.value;
                                currencySet.add(currency);
                            }
                        }

                        if (!(directPaymentsCurrency.containsAll(currencySet) && currencySet.containsAll(directPaymentsCurrency))) {
                            directPaymentsCurrency.clear();
                            directPaymentsCurrency.addAll(currencySet);
                            editor.putStringSet("directPaymentsCurrency", currencySet);
                            changed = true;

                            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.billingProductDetailsUpdated);
                        }
                    }
                    break;
                }
                case "premium_purchase_blocked": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        if (premiumLocked != ((TLRPC.TL_jsonBool) value.value).value) {
                            premiumLocked = ((TLRPC.TL_jsonBool) value.value).value;
                            editor.putBoolean("premiumLocked", premiumLocked);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stars_purchase_blocked": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        if (starsLocked != ((TLRPC.TL_jsonBool) value.value).value) {
                            starsLocked = ((TLRPC.TL_jsonBool) value.value).value;
                            editor.putBoolean("starsLocked", starsLocked);
                            changed = true;
                        }
                    }
                    break;
                }
                case "premium_bot_username": {
                    if (value.value instanceof TLRPC.TL_jsonString) {
                        String string = ((TLRPC.TL_jsonString) value.value).value;
                        if (!string.equals(premiumBotUsername)) {
                            premiumBotUsername = string;
                            editor.putString("premiumBotUsername", premiumBotUsername);
                            changed = true;
                        }
                    }
                    break;
                }
                case "verify_age_bot_username": {
                    if (value.value instanceof TLRPC.TL_jsonString) {
                        String string = ((TLRPC.TL_jsonString) value.value).value;
                        if (!string.equals(verifyAgeBotUsername)) {
                            verifyAgeBotUsername = string;
                            editor.putString("verifyAgeBotUsername", verifyAgeBotUsername);
                            changed = true;
                        }
                    }
                    break;
                }
                case "verify_age_country": {
                    if (value.value instanceof TLRPC.TL_jsonString) {
                        String string = ((TLRPC.TL_jsonString) value.value).value;
                        if (!string.equals(verifyAgeCountry)) {
                            verifyAgeCountry = string;
                            editor.putString("verifyAgeCountry", verifyAgeCountry);
                            changed = true;
                        }
                    }
                    break;
                }
                case "verify_age_min": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        int num = (int) ((TLRPC.TL_jsonNumber) value.value).value;
                        if (num != verifyAgeMin) {
                            editor.putInt("verifyAgeMin", verifyAgeMin = num);
                            changed = true;
                        }
                    }
                    break;
                }
                case "premium_invoice_slug": {
                    if (value.value instanceof TLRPC.TL_jsonString) {
                        String string = ((TLRPC.TL_jsonString) value.value).value;
                        if (!string.equals(premiumInvoiceSlug)) {
                            premiumInvoiceSlug = string;
                            editor.putString("premiumInvoiceSlug", premiumInvoiceSlug);
                            changed = true;
                        }
                    }
                    break;
                }
                case "premium_promo_order": {
                    if (value.value instanceof TLRPC.TL_jsonArray) {
                        TLRPC.TL_jsonArray order = (TLRPC.TL_jsonArray) value.value;
                        changed = savePremiumFeaturesPreviewOrder("premiumFeaturesTypesToPosition", premiumFeaturesTypesToPosition, editor, order.value);
                    }
                    break;
                }
                case "business_promo_order": {
                    if (value.value instanceof TLRPC.TL_jsonArray) {
                        TLRPC.TL_jsonArray order = (TLRPC.TL_jsonArray) value.value;
                        changed = savePremiumFeaturesPreviewOrder("businessFeaturesTypesToPosition", businessFeaturesTypesToPosition, editor, order.value);
                    }
                    break;
                }
                case "emojies_animated_zoom": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (animatedEmojisZoom != number.value) {
                            animatedEmojisZoom = (float) number.value;
                            editor.putFloat("animatedEmojisZoom", animatedEmojisZoom);
                            changed = true;
                        }
                    }
                    break;
                }
                case "getfile_experimental_params": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool bool = (TLRPC.TL_jsonBool) value.value;
                        if (bool.value != getfileExperimentalParams) {
                            getfileExperimentalParams = bool.value;
                            editor.putBoolean("getfileExperimentalParams", getfileExperimentalParams);
                            changed = true;
                        }
                    }
                    break;
                }
                case "smsjobs_sticky_notification_enabled": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool bool = (TLRPC.TL_jsonBool) value.value;
                        if (bool.value != smsjobsStickyNotificationEnabled) {
                            smsjobsStickyNotificationEnabled = bool.value;
                            editor.putBoolean("smsjobsStickyNotificationEnabled", smsjobsStickyNotificationEnabled);
                            changed = true;
                        }
                    }
                    break;
                }
                case "dialog_filters_enabled": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool bool = (TLRPC.TL_jsonBool) value.value;
                        if (bool.value != filtersEnabled) {
                            filtersEnabled = bool.value;
                            editor.putBoolean("filtersEnabled", filtersEnabled);
                            changed = true;
                        }
                    }
                    break;
                }
                case "dialog_filters_tooltip": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool bool = (TLRPC.TL_jsonBool) value.value;
                        if (bool.value != showFiltersTooltip) {
                            showFiltersTooltip = bool.value;
                            editor.putBoolean("showFiltersTooltip", showFiltersTooltip);
                            changed = true;
                            getNotificationCenter().postNotificationName(NotificationCenter.filterSettingsUpdated);
                        }
                    }
                    break;
                }
                case "youtube_pip": {
                    if (value.value instanceof TLRPC.TL_jsonString) {
                        TLRPC.TL_jsonString string = (TLRPC.TL_jsonString) value.value;
                        if (!string.value.equals(youtubePipType)) {
                            youtubePipType = string.value;
                            editor.putString("youtubePipType", youtubePipType);
                            changed = true;
                        }
                    }
                    break;
                }
                case "background_connection": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool bool = (TLRPC.TL_jsonBool) value.value;
                        if (bool.value != backgroundConnection) {
                            backgroundConnection = bool.value;
                            editor.putBoolean("backgroundConnection", backgroundConnection);
                            changed = true;
                            keelAliveChanged = true;
                        }
                    }
                    break;
                }
                case "keep_alive_service": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool bool = (TLRPC.TL_jsonBool) value.value;
                        if (bool.value != keepAliveService) {
                            keepAliveService = bool.value;
                            editor.putBoolean("keepAliveService", keepAliveService);
                            changed = true;
                            keelAliveChanged = true;
                        }
                    }
                    break;
                }
                case "qr_login_camera": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool bool = (TLRPC.TL_jsonBool) value.value;
                        if (bool.value != qrLoginCamera) {
                            qrLoginCamera = bool.value;
                            editor.putBoolean("qrLoginCamera", qrLoginCamera);
                            changed = true;
                        }
                    }
                    break;
                }
                case "save_gifs_with_stickers": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool bool = (TLRPC.TL_jsonBool) value.value;
                        if (bool.value != saveGifsWithStickers) {
                            saveGifsWithStickers = bool.value;
                            editor.putBoolean("saveGifsWithStickers", saveGifsWithStickers);
                            changed = true;
                        }
                    }
                    break;
                }
                case "url_auth_domains": {
                    HashSet<String> newDomains = new HashSet<>();
                    if (value.value instanceof TLRPC.TL_jsonArray) {
                        TLRPC.TL_jsonArray array = (TLRPC.TL_jsonArray) value.value;
                        for (int b = 0, N2 = array.value.size(); b < N2; b++) {
                            TLRPC.JSONValue val = array.value.get(b);
                            if (val instanceof TLRPC.TL_jsonString) {
                                TLRPC.TL_jsonString string = (TLRPC.TL_jsonString) val;
                                newDomains.add(string.value);
                            }
                        }
                    }
                    if (!authDomains.equals(newDomains)) {
                        authDomains = newDomains;
                        editor.putStringSet("authDomains", authDomains);
                        changed = true;
                    }
                    break;
                }
                case "autologin_domains": {
                    HashSet<String> newDomains = new HashSet<>();
                    if (value.value instanceof TLRPC.TL_jsonArray) {
                        TLRPC.TL_jsonArray array = (TLRPC.TL_jsonArray) value.value;
                        for (int b = 0, N2 = array.value.size(); b < N2; b++) {
                            TLRPC.JSONValue val = array.value.get(b);
                            if (val instanceof TLRPC.TL_jsonString) {
                                TLRPC.TL_jsonString string = (TLRPC.TL_jsonString) val;
                                newDomains.add(string.value);
                            }
                        }
                    }
                    if (!autologinDomains.equals(newDomains)) {
                        autologinDomains = newDomains;
                        editor.putStringSet("autologinDomains", autologinDomains);
                        changed = true;
                    }
                    break;
                }
                case "emojies_send_dice": {
                    HashSet<String> newEmojies = new HashSet<>();
                    if (value.value instanceof TLRPC.TL_jsonArray) {
                        TLRPC.TL_jsonArray array = (TLRPC.TL_jsonArray) value.value;
                        for (int b = 0, N2 = array.value.size(); b < N2; b++) {
                            TLRPC.JSONValue val = array.value.get(b);
                            if (val instanceof TLRPC.TL_jsonString) {
                                TLRPC.TL_jsonString string = (TLRPC.TL_jsonString) val;
                                newEmojies.add(string.value.replace("\uFE0F", ""));
                            }
                        }
                    }
                    if (!diceEmojies.equals(newEmojies)) {
                        diceEmojies = newEmojies;
                        editor.putStringSet("diceEmojies", diceEmojies);
                        changed = true;
                    }
                    break;
                }
                case "gif_search_emojies": {
                    ArrayList<String> newEmojies = new ArrayList<>();
                    if (value.value instanceof TLRPC.TL_jsonArray) {
                        TLRPC.TL_jsonArray array = (TLRPC.TL_jsonArray) value.value;
                        for (int b = 0, N2 = array.value.size(); b < N2; b++) {
                            TLRPC.JSONValue val = array.value.get(b);
                            if (val instanceof TLRPC.TL_jsonString) {
                                TLRPC.TL_jsonString string = (TLRPC.TL_jsonString) val;
                                newEmojies.add(string.value.replace("\uFE0F", ""));
                            }
                        }
                    }
                    if (!gifSearchEmojies.equals(newEmojies)) {
                        gifSearchEmojies = newEmojies;
                        SerializedData serializedData = new SerializedData();
                        serializedData.writeInt32(gifSearchEmojies.size());
                        for (int b = 0, N2 = gifSearchEmojies.size(); b < N2; b++) {
                            serializedData.writeString(gifSearchEmojies.get(b));
                        }
                        editor.putString("gifSearchEmojies", Base64.encodeToString(serializedData.toByteArray(), Base64.DEFAULT));
                        serializedData.cleanup();
                        changed = true;
                    }
                    break;
                }
                case "emojies_send_dice_success": {
                    try {
                        HashMap<String, DiceFrameSuccess> newEmojies = new HashMap<>();
                        if (value.value instanceof TLRPC.TL_jsonObject) {
                            TLRPC.TL_jsonObject jsonObject = (TLRPC.TL_jsonObject) value.value;
                            for (int b = 0, N2 = jsonObject.value.size(); b < N2; b++) {
                                TLRPC.TL_jsonObjectValue val = jsonObject.value.get(b);
                                if (val.value instanceof TLRPC.TL_jsonObject) {
                                    TLRPC.TL_jsonObject jsonObject2 = (TLRPC.TL_jsonObject) val.value;
                                    int n = Integer.MAX_VALUE;
                                    int f = Integer.MAX_VALUE;
                                    for (int c = 0, N3 = jsonObject2.value.size(); c < N3; c++) {
                                        TLRPC.TL_jsonObjectValue val2 = jsonObject2.value.get(c);
                                        if (val2.value instanceof TLRPC.TL_jsonNumber) {
                                            if ("value".equals(val2.key)) {
                                                n = (int) ((TLRPC.TL_jsonNumber) val2.value).value;
                                            } else if ("frame_start".equals(val2.key)) {
                                                f = (int) ((TLRPC.TL_jsonNumber) val2.value).value;
                                            }
                                        }
                                    }
                                    if (f != Integer.MAX_VALUE && n != Integer.MAX_VALUE) {
                                        newEmojies.put(val.key.replace("\uFE0F", ""), new DiceFrameSuccess(f, n));
                                    }
                                }
                            }
                        }
                        if (!diceSuccess.equals(newEmojies)) {
                            diceSuccess = newEmojies;
                            SerializedData serializedData = new SerializedData();
                            serializedData.writeInt32(diceSuccess.size());
                            for (HashMap.Entry<String, DiceFrameSuccess> entry : diceSuccess.entrySet()) {
                                serializedData.writeString(entry.getKey());
                                DiceFrameSuccess frameSuccess = entry.getValue();
                                serializedData.writeInt32(frameSuccess.frame);
                                serializedData.writeInt32(frameSuccess.num);
                            }
                            editor.putString("diceSuccess", Base64.encodeToString(serializedData.toByteArray(), Base64.DEFAULT));
                            serializedData.cleanup();
                            changed = true;
                        }
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                    break;
                }
                case "autoarchive_setting_available": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool bool = (TLRPC.TL_jsonBool) value.value;
                        if (bool.value != autoarchiveAvailable) {
                            autoarchiveAvailable = bool.value;
                            editor.putBoolean("autoarchiveAvailable", autoarchiveAvailable);
                            changed = true;
                        }
                    }
                    break;
                }
                case "groupcall_video_participants_max": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != groupCallVideoMaxParticipants) {
                            groupCallVideoMaxParticipants = (int) number.value;
                            editor.putInt("groipCallVideoMaxParticipants", groupCallVideoMaxParticipants);
                            changed = true;
                        }
                    }
                    break;
                }
                case "chat_read_mark_size_threshold": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != chatReadMarkSizeThreshold) {
                            chatReadMarkSizeThreshold = (int) number.value;
                            editor.putInt("chatReadMarkSizeThreshold", chatReadMarkSizeThreshold);
                            changed = true;
                        }
                    }
                    break;
                }
                case "chat_read_mark_expire_period": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != chatReadMarkExpirePeriod) {
                            chatReadMarkExpirePeriod = (int) number.value;
                            editor.putInt("chatReadMarkExpirePeriod", chatReadMarkExpirePeriod);
                            changed = true;
                        }
                    }
                    break;
                }
                case "pm_read_date_expire_period": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != pmReadDateExpirePeriod) {
                            pmReadDateExpirePeriod = (int) number.value;
                            editor.putInt("pmReadDateExpirePeriod", pmReadDateExpirePeriod);
                            changed = true;
                        }
                    }
                    break;
                }
                case "inapp_update_check_delay": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != updateCheckDelay) {
                            updateCheckDelay = (int) number.value;
                            editor.putInt("updateCheckDelay", updateCheckDelay);
                            changed = true;
                        }
                    } else if (value.value instanceof TLRPC.TL_jsonString) {
                        TLRPC.TL_jsonString number = (TLRPC.TL_jsonString) value.value;
                        int delay = Utilities.parseInt(number.value);
                        if (delay != updateCheckDelay) {
                            updateCheckDelay = delay;
                            editor.putInt("updateCheckDelay", updateCheckDelay);
                            changed = true;
                        }
                    }
                    break;
                }
                case "round_video_encoding": {
                    if (value.value instanceof TLRPC.TL_jsonObject) {
                        TLRPC.TL_jsonObject jsonObject = (TLRPC.TL_jsonObject) value.value;
                        for (int b = 0, N2 = jsonObject.value.size(); b < N2; b++) {
                            TLRPC.TL_jsonObjectValue value2 = jsonObject.value.get(b);
                            switch (value2.key) {
                                case "diameter": {
                                    if (value2.value instanceof TLRPC.TL_jsonNumber) {
                                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value2.value;
                                        if (number.value != roundVideoSize) {
                                            roundVideoSize = (int) number.value;
                                            editor.putInt("roundVideoSize", roundVideoSize);
                                            changed = true;
                                        }
                                    }
                                    break;
                                }
                                case "video_bitrate": {
                                    if (value2.value instanceof TLRPC.TL_jsonNumber) {
                                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value2.value;
                                        if (number.value != roundVideoBitrate) {
                                            roundVideoBitrate = (int) number.value;
                                            editor.putInt("roundVideoBitrate", roundVideoBitrate);
                                            changed = true;
                                        }
                                    }
                                    break;
                                }
                                case "audio_bitrate": {
                                    if (value2.value instanceof TLRPC.TL_jsonNumber) {
                                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value2.value;
                                        if (number.value != roundAudioBitrate) {
                                            roundAudioBitrate = (int) number.value;
                                            editor.putInt("roundAudioBitrate", roundAudioBitrate);
                                            changed = true;
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    break;
                }
                case "stickers_emoji_suggest_only_api": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool bool = (TLRPC.TL_jsonBool) value.value;
                        if (bool.value != suggestStickersApiOnly) {
                            suggestStickersApiOnly = bool.value;
                            editor.putBoolean("suggestStickersApiOnly", suggestStickersApiOnly);
                            changed = true;
                        }
                    }
                    break;
                }
                case "export_regex": {
                    HashSet<String> newExport = new HashSet<>();
                    if (value.value instanceof TLRPC.TL_jsonArray) {
                        TLRPC.TL_jsonArray array = (TLRPC.TL_jsonArray) value.value;
                        for (int b = 0, N2 = array.value.size(); b < N2; b++) {
                            TLRPC.JSONValue val = array.value.get(b);
                            if (val instanceof TLRPC.TL_jsonString) {
                                TLRPC.TL_jsonString string = (TLRPC.TL_jsonString) val;
                                newExport.add(string.value);
                            }
                        }
                    }
                    if (!exportUri.equals(newExport)) {
                        exportUri = newExport;
                        editor.putStringSet("exportUri2", exportUri);
                        changed = true;
                    }
                    break;
                }
                case "export_group_urls": {
                    HashSet<String> newExport = new HashSet<>();
                    if (value.value instanceof TLRPC.TL_jsonArray) {
                        TLRPC.TL_jsonArray array = (TLRPC.TL_jsonArray) value.value;
                        for (int b = 0, N2 = array.value.size(); b < N2; b++) {
                            TLRPC.JSONValue val = array.value.get(b);
                            if (val instanceof TLRPC.TL_jsonString) {
                                TLRPC.TL_jsonString string = (TLRPC.TL_jsonString) val;
                                newExport.add(string.value);
                            }
                        }
                    }
                    if (!exportGroupUri.equals(newExport)) {
                        exportGroupUri = newExport;
                        editor.putStringSet("exportGroupUri", exportGroupUri);
                        changed = true;
                    }
                    break;
                }
                case "export_private_urls": {
                    HashSet<String> newExport = new HashSet<>();
                    if (value.value instanceof TLRPC.TL_jsonArray) {
                        TLRPC.TL_jsonArray array = (TLRPC.TL_jsonArray) value.value;
                        for (int b = 0, N2 = array.value.size(); b < N2; b++) {
                            TLRPC.JSONValue val = array.value.get(b);
                            if (val instanceof TLRPC.TL_jsonString) {
                                TLRPC.TL_jsonString string = (TLRPC.TL_jsonString) val;
                                newExport.add(string.value);
                            }
                        }
                    }
                    if (!exportPrivateUri.equals(newExport)) {
                        exportPrivateUri = newExport;
                        editor.putStringSet("exportPrivateUri", exportPrivateUri);
                        changed = true;
                    }
                    break;
                }
                case "emojies_sounds": {
                    try {
                        HashMap<String, EmojiSound> newEmojies = new HashMap<>();
                        if (value.value instanceof TLRPC.TL_jsonObject) {
                            TLRPC.TL_jsonObject jsonObject = (TLRPC.TL_jsonObject) value.value;
                            for (int b = 0, N2 = jsonObject.value.size(); b < N2; b++) {
                                TLRPC.TL_jsonObjectValue val = jsonObject.value.get(b);
                                if (val.value instanceof TLRPC.TL_jsonObject) {
                                    TLRPC.TL_jsonObject jsonObject2 = (TLRPC.TL_jsonObject) val.value;
                                    long i = 0;
                                    long ah = 0;
                                    String fr = null;
                                    for (int c = 0, N3 = jsonObject2.value.size(); c < N3; c++) {
                                        TLRPC.TL_jsonObjectValue val2 = jsonObject2.value.get(c);
                                        if (val2.value instanceof TLRPC.TL_jsonString) {
                                            if ("id".equals(val2.key)) {
                                                i = Utilities.parseLong(((TLRPC.TL_jsonString) val2.value).value);
                                            } else if ("access_hash".equals(val2.key)) {
                                                ah = Utilities.parseLong(((TLRPC.TL_jsonString) val2.value).value);
                                            } else if ("file_reference_base64".equals(val2.key)) {
                                                fr = ((TLRPC.TL_jsonString) val2.value).value;
                                            }
                                        }
                                    }
                                    if (i != 0 && ah != 0 && fr != null) {
                                        newEmojies.put(val.key.replace("\uFE0F", ""), new EmojiSound(i, ah, fr));
                                    }
                                }
                            }
                        }
                        if (!emojiSounds.equals(newEmojies)) {
                            emojiSounds = newEmojies;
                            SerializedData serializedData = new SerializedData();
                            serializedData.writeInt32(emojiSounds.size());
                            for (HashMap.Entry<String, EmojiSound> entry : emojiSounds.entrySet()) {
                                serializedData.writeString(entry.getKey());
                                EmojiSound emojiSound = entry.getValue();
                                serializedData.writeInt64(emojiSound.id);
                                serializedData.writeInt64(emojiSound.accessHash);
                                serializedData.writeByteArray(emojiSound.fileReference);
                            }
                            editor.putString("emojiSounds", Base64.encodeToString(serializedData.toByteArray(), Base64.DEFAULT));
                            serializedData.cleanup();
                            changed = true;
                        }
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                    break;
                }
                case "ringtone_size_max": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != ringtoneSizeMax) {
                            ringtoneSizeMax = (int) number.value;
                            editor.putInt("ringtoneSizeMax", ringtoneSizeMax);
                            changed = true;
                        }
                    }
                    break;
                }
                case "ringtone_duration_max": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != ringtoneDurationMax) {
                            ringtoneDurationMax = (int) number.value;
                            editor.putInt("ringtoneDurationMax", ringtoneDurationMax);
                            changed = true;
                        }
                    }
                    break;
                }
                case "channels_limit_default": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != channelsLimitDefault) {
                            channelsLimitDefault = (int) number.value;
                            editor.putInt("channelsLimitDefault", channelsLimitDefault);
                            changed = true;
                        }
                    }
                    break;
                }
                case "channels_limit_premium": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != channelsLimitPremium) {
                            channelsLimitPremium = (int) number.value;
                            editor.putInt("channelsLimitPremium", channelsLimitPremium);
                            changed = true;
                        }
                    }
                    break;
                }
                case "saved_gifs_limit_default": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != savedGifsLimitDefault) {
                            savedGifsLimitDefault = (int) number.value;
                            editor.putInt("savedGifsLimitDefault", savedGifsLimitDefault);
                            changed = true;
                        }
                    }
                    break;
                }
                case "saved_gifs_limit_premium": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != savedGifsLimitPremium) {
                            savedGifsLimitPremium = (int) number.value;
                            editor.putInt("savedGifsLimitPremium", savedGifsLimitPremium);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stickers_faved_limit_default": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != stickersFavedLimitDefault) {
                            stickersFavedLimitDefault = (int) number.value;
                            editor.putInt("stickersFavedLimitDefault", stickersFavedLimitDefault);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stickers_faved_limit_premium": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != stickersFavedLimitPremium) {
                            stickersFavedLimitPremium = (int) number.value;
                            editor.putInt("stickersFavedLimitPremium", stickersFavedLimitPremium);
                            changed = true;
                        }
                    }
                    break;
                }
                case "pinned_dialogs_count_max_default": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != maxPinnedDialogsCountDefault) {
                            maxPinnedDialogsCountDefault = (int) number.value;
                            editor.putInt("maxPinnedDialogsCountDefault", maxPinnedDialogsCountDefault);
                            changed = true;
                        }
                    }
                    break;
                }
                case "pinned_dialogs_count_max_premium": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != maxPinnedDialogsCountPremium) {
                            maxPinnedDialogsCountPremium = (int) number.value;
                            editor.putInt("maxPinnedDialogsCountPremium", maxPinnedDialogsCountPremium);
                            changed = true;
                        }
                    }
                    break;
                }
                case "dialog_filters_limit_default": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != dialogFiltersLimitDefault) {
                            dialogFiltersLimitDefault = (int) number.value;
                            editor.putInt("dialogFiltersLimitDefault", dialogFiltersLimitDefault);
                            changed = true;
                        }
                    }
                    break;
                }
                case "dialog_filters_limit_premium": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != dialogFiltersLimitPremium) {
                            dialogFiltersLimitPremium = (int) number.value;
                            editor.putInt("dialogFiltersLimitPremium", dialogFiltersLimitPremium);
                            changed = true;
                        }
                    }
                    break;
                }
                case "dialog_filters_chats_limit_default": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != dialogFiltersChatsLimitDefault) {
                            dialogFiltersChatsLimitDefault = (int) number.value;
                            editor.putInt("dialogFiltersChatsLimitDefault", dialogFiltersChatsLimitDefault);
                            changed = true;
                        }
                    }
                    break;
                }
                case "dialog_filters_chats_limit_premium": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != dialogFiltersChatsLimitPremium) {
                            dialogFiltersChatsLimitPremium = (int) number.value;
                            editor.putInt("dialogFiltersChatsLimitPremium", dialogFiltersChatsLimitPremium);
                            changed = true;
                        }
                    }
                    break;
                }
                case "dialog_filters_pinned_limit_default": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != dialogFiltersPinnedLimitDefault) {
                            dialogFiltersPinnedLimitDefault = (int) number.value;
                            editor.putInt("dialogFiltersPinnedLimitDefault", dialogFiltersPinnedLimitDefault);
                            changed = true;
                        }
                    }
                    break;
                }
                case "dialog_filters_pinned_limit_premium": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != dialogFiltersPinnedLimitPremium) {
                            dialogFiltersPinnedLimitPremium = (int) number.value;
                            editor.putInt("dialogFiltersPinnedLimitPremium", dialogFiltersPinnedLimitPremium);
                            changed = true;
                        }
                    }
                    break;
                }
                case "upload_max_fileparts_default": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != uploadMaxFileParts) {
                            uploadMaxFileParts = (int) number.value;
                            editor.putInt("uploadMaxFileParts", uploadMaxFileParts);
                            changed = true;
                        }
                    }
                    break;
                }
                case "upload_max_fileparts_premium": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != uploadMaxFilePartsPremium) {
                            uploadMaxFilePartsPremium = (int) number.value;
                            editor.putInt("uploadMaxFilePartsPremium", uploadMaxFilePartsPremium);
                            changed = true;
                        }
                    }
                    break;
                }
                case "channels_public_limit_default": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != publicLinksLimitDefault) {
                            publicLinksLimitDefault = (int) number.value;
                            editor.putInt("publicLinksLimit", publicLinksLimitDefault);
                            changed = true;
                        }
                    }
                    break;
                }
                case "channels_public_limit_premium": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != publicLinksLimitPremium) {
                            publicLinksLimitPremium = (int) number.value;
                            editor.putInt("publicLinksLimitPremium", publicLinksLimitPremium);
                            changed = true;
                        }
                    }
                    break;
                }
                case "caption_length_limit_default": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != captionLengthLimitDefault) {
                            captionLengthLimitDefault = (int) number.value;
                            editor.putInt("captionLengthLimitDefault", captionLengthLimitDefault);
                            changed = true;
                        }
                    }
                    break;
                }
                case "caption_length_limit_premium": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != captionLengthLimitPremium) {
                            captionLengthLimitPremium = (int) number.value;
                            editor.putInt("captionLengthLimitPremium", captionLengthLimitPremium);
                            changed = true;
                        }
                    }
                    break;
                }
                case "story_caption_length_limit_default": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != storyCaptionLengthLimitDefault) {
                            storyCaptionLengthLimitDefault = (int) number.value;
                            editor.putInt("storyCaptionLengthLimit", storyCaptionLengthLimitDefault);
                            changed = true;
                        }
                    }
                    break;
                }
                case "story_caption_length_limit_premium": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != storyCaptionLengthLimitPremium) {
                            storyCaptionLengthLimitPremium = (int) number.value;
                            editor.putInt("storyCaptionLengthLimitPremium", storyCaptionLengthLimitPremium);
                            changed = true;
                        }
                    }
                    break;
                }
                case "about_length_limit_default": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != aboutLengthLimitDefault) {
                            aboutLengthLimitDefault = (int) number.value;
                            editor.putInt("aboutLengthLimitDefault", aboutLengthLimitDefault);
                            changed = true;
                        }
                    }
                    break;
                }
                case "about_length_limit_premium": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != aboutLengthLimitPremium) {
                            aboutLengthLimitPremium = (int) number.value;
                            editor.putInt("aboutLengthLimitPremium", aboutLengthLimitPremium);
                            changed = true;
                        }
                    }
                    break;
                }
                case "reactions_user_max_default": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != reactionsUserMaxDefault) {
                            reactionsUserMaxDefault = (int) number.value;
                            editor.putInt("reactionsUserMaxDefault", reactionsUserMaxDefault);
                            changed = true;
                        }
                    }
                    break;
                }
                case "reactions_user_max_premium": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != reactionsUserMaxPremium) {
                            reactionsUserMaxPremium = (int) number.value;
                            editor.putInt("reactionsUserMaxPremium", reactionsUserMaxPremium);
                            changed = true;
                        }
                    }
                    break;
                }
                case "reactions_in_chat_max": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != reactionsInChatMax) {
                            reactionsInChatMax = (int) number.value;
                            editor.putInt("reactionsInChatMax", reactionsInChatMax);
                            changed = true;
                        }
                    }
                    break;
                }
                case "forum_upgrade_participants_min": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != forumUpgradeParticipantsMin) {
                            forumUpgradeParticipantsMin = (int) number.value;
                            editor.putInt("forumUpgradeParticipantsMin", forumUpgradeParticipantsMin);
                            changed = true;
                        }
                    }
                    break;
                }
                case "topics_pinned_limit": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != topicsPinnedLimit) {
                            topicsPinnedLimit = (int) number.value;
                            editor.putInt("topicsPinnedLimit", topicsPinnedLimit);
                            changed = true;
                        }
                    }
                    break;
                }
                case "telegram_antispam_user_id": {
                    if (value.value instanceof TLRPC.TL_jsonString) {
                        TLRPC.TL_jsonString string = (TLRPC.TL_jsonString) value.value;
                        try {
                            long number = Long.parseLong(string.value);
                            if (number != telegramAntispamUserId) {
                                telegramAntispamUserId = number;
                                editor.putLong("telegramAntispamUserId", telegramAntispamUserId);
                                changed = true;
                            }
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    }
                    break;
                }
                case "telegram_antispam_group_size_min": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != telegramAntispamGroupSizeMin) {
                            telegramAntispamGroupSizeMin = (int) number.value;
                            editor.putInt("telegramAntispamGroupSizeMin", telegramAntispamGroupSizeMin);
                            changed = true;
                        }
                    }
                    break;
                }
                case "hidden_members_group_size_min": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != hiddenMembersGroupSizeMin) {
                            hiddenMembersGroupSizeMin = (int) number.value;
                            editor.putInt("hiddenMembersGroupSizeMin", hiddenMembersGroupSizeMin);
                            changed = true;
                        }
                    }
                    break;
                }
                case "chatlist_update_period": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber number = (TLRPC.TL_jsonNumber) value.value;
                        if (number.value != chatlistUpdatePeriod) {
                            chatlistUpdatePeriod = (int) number.value;
                            editor.putInt("chatlistUpdatePeriod", chatlistUpdatePeriod);
                            changed = true;
                        }
                    }
                    break;
                }
                case "android_collect_device_stats": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool bool = (TLRPC.TL_jsonBool) value.value;
                        if (bool.value != collectDeviceStats) {
                            collectDeviceStats = bool.value;
                            changed = true;
                        }
                    }
                    break;
                }
                case "android_check_reset_langpack": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != checkResetLangpack) {
                            checkResetLangpack = (int) num.value;
                            editor.putInt("checkResetLangpack", checkResetLangpack);
                            LocaleController.getInstance().checkPatchLangpack(currentAccount);
                            changed = true;
                        }
                    }
                    break;
                }
                case "chatlist_invites_limit_default": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != chatlistInvitesLimitDefault) {
                            chatlistInvitesLimitDefault = (int) num.value;
                            editor.putInt("chatlistInvitesLimitDefault", chatlistInvitesLimitDefault);
                            changed = true;
                        }
                    }
                    break;
                }
                case "story_expiring_limit_default": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != storyExpiringLimitDefault) {
                            storyExpiringLimitDefault = (int) num.value;
                            editor.putInt("storyExpiringLimitDefault", storyExpiringLimitDefault);
                            changed = true;
                        }
                    }
                    break;
                }
                case "story_expiring_limit_premium": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != storyExpiringLimitPremium) {
                            storyExpiringLimitPremium = (int) num.value;
                            editor.putInt("storyExpiringLimitPremium", storyExpiringLimitPremium);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stories_suggested_reactions_limit_default": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != storiesSuggestedReactionsLimitDefault) {
                            storiesSuggestedReactionsLimitDefault = (int) num.value;
                            editor.putInt("storiesSuggestedReactionsLimitDefault", storiesSuggestedReactionsLimitDefault);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stories_suggested_reactions_limit_premium": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != storiesSuggestedReactionsLimitPremium) {
                            storiesSuggestedReactionsLimitPremium = (int) num.value;
                            editor.putInt("storiesSuggestedReactionsLimitPremium", storiesSuggestedReactionsLimitPremium);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stories_sent_weekly_limit_default": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != storiesSentWeeklyLimitDefault) {
                            storiesSentWeeklyLimitDefault = (int) num.value;
                            editor.putInt("storiesSentWeeklyLimitDefault", storiesSentWeeklyLimitDefault);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stories_sent_weekly_limit_premium": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != storiesSentWeeklyLimitPremium) {
                            storiesSentWeeklyLimitPremium = (int) num.value;
                            editor.putInt("storiesSentWeeklyLimitPremium", storiesSentWeeklyLimitPremium);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stories_sent_monthly_limit_default": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != storiesSentMonthlyLimitDefault) {
                            storiesSentMonthlyLimitDefault = (int) num.value;
                            editor.putInt("storiesSentMonthlyLimitDefault", storiesSentMonthlyLimitDefault);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stories_sent_monthly_limit_premium": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != storiesSentMonthlyLimitPremium) {
                            storiesSentMonthlyLimitPremium = (int) num.value;
                            editor.putInt("storiesSentMonthlyLimitPremium", storiesSentMonthlyLimitPremium);
                            changed = true;
                        }
                    }
                    break;
                }
                case "chatlist_invites_limit_premium": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != chatlistInvitesLimitPremium) {
                            chatlistInvitesLimitPremium = (int) num.value;
                            editor.putInt("chatlistInvitesLimitPremium", chatlistInvitesLimitPremium);
                            changed = true;
                        }
                    }
                    break;
                }
                case "chatlists_joined_limit_default": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != chatlistJoinedLimitDefault) {
                            chatlistJoinedLimitDefault = (int) num.value;
                            editor.putInt("chatlistJoinedLimitDefault", chatlistJoinedLimitDefault);
                            changed = true;
                        }
                    }
                    break;
                }
                case "chatlists_joined_limit_premium": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != chatlistJoinedLimitPremium) {
                            chatlistJoinedLimitPremium = (int) num.value;
                            editor.putInt("chatlistJoinedLimitPremium", chatlistJoinedLimitPremium);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stargifts_message_length_max": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != stargiftsMessageLengthMax) {
                            stargiftsMessageLengthMax = (int) num.value;
                            editor.putInt("stargiftsMessageLengthMax", stargiftsMessageLengthMax);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stargifts_convert_period_max": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != stargiftsConvertPeriodMax) {
                            stargiftsConvertPeriodMax = (int) num.value;
                            editor.putInt("stargiftsConvertPeriodMax", stargiftsConvertPeriodMax);
                            changed = true;
                        }
                    }
                    break;
                }
                case "video_ignore_alt_documents": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool bool = (TLRPC.TL_jsonBool) value.value;
                        if (bool.value != videoIgnoreAltDocuments) {
                            videoIgnoreAltDocuments = bool.value;
                            editor.putBoolean("videoIgnoreAltDocuments", videoIgnoreAltDocuments);
                            changed = true;
                        }
                    }
                    break;
                }
                case "bot_fullscreen_blur_disable": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool bool = (TLRPC.TL_jsonBool) value.value;
                        if (bool.value != disableBotFullscreenBlur) {
                            disableBotFullscreenBlur = bool.value;
                            editor.putBoolean("disableBotFullscreenBlur", disableBotFullscreenBlur);
                            changed = true;
                        }
                    }
                    break;
                }
                case "ton_blockchain_explorer_url": {
                    if (value.value instanceof TLRPC.TL_jsonString) {
                        TLRPC.TL_jsonString str = (TLRPC.TL_jsonString) value.value;
                        if (!TextUtils.equals(str.value, tonBlockchainExplorerUrl)) {
                            tonBlockchainExplorerUrl = str.value;
                            editor.putString("tonBlockchainExplorerUrl", tonBlockchainExplorerUrl);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stars_paid_message_amount_max": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (starsPaidMessageAmountMax != (long) num.value) {
                            starsPaidMessageAmountMax = (long) num.value;
                            editor.putLong("starsPaidMessageAmountMax", starsPaidMessageAmountMax);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stars_paid_message_commission_permille": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (starsPaidMessageCommissionPermille != (int) num.value) {
                            starsPaidMessageCommissionPermille = (int) num.value;
                            editor.putInt("starsPaidMessageCommissionPermille", starsPaidMessageCommissionPermille);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stargifts_pinned_to_top_limit": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (stargiftsPinnedToTopLimit != (int) num.value) {
                            stargiftsPinnedToTopLimit = (int) num.value;
                            editor.putInt("stargiftsPinnedToTopLimit", stargiftsPinnedToTopLimit);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stars_paid_messages_available": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool num = (TLRPC.TL_jsonBool) value.value;
                        if (starsPaidMessagesAvailable != num.value) {
                            starsPaidMessagesAvailable = num.value;
                            editor.putBoolean("starsPaidMessagesAvailable", starsPaidMessagesAvailable);
                            changed = true;
                        }
                    }
                    break;
                }
                case "enable_gifts_in_profile": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool num = (TLRPC.TL_jsonBool) value.value;
                        if (enableGiftsInProfile != num.value) {
                            enableGiftsInProfile = num.value;
                            editor.putBoolean("enableGiftsInProfile", enableGiftsInProfile);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stories_posting": {
                    if (value.value instanceof TLRPC.TL_jsonString) {
                        TLRPC.TL_jsonString str = (TLRPC.TL_jsonString) value.value;
                        if (!TextUtils.equals(str.value, storiesPosting)) {
                            storiesPosting = str.value;
                            editor.putString("storiesPosting", storiesPosting);
                            changed = storiesChanged = true;
                        }
                    }
                    break;
                }
                case "stories_entities": {
                    if (value.value instanceof TLRPC.TL_jsonString) {
                        TLRPC.TL_jsonString str = (TLRPC.TL_jsonString) value.value;
                        if (!TextUtils.equals(str.value, storiesEntities)) {
                            storiesEntities = str.value;
                            editor.putString("storiesEntities", storiesEntities);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stories_export_nopublic_link": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool bool = (TLRPC.TL_jsonBool) value.value;
                        if (storiesExportNopublicLink != bool.value) {
                            storiesExportNopublicLink = bool.value;
                            editor.putBoolean("storiesExportNopublicLink", storiesExportNopublicLink);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stories_venue_search_username": {
                    if (value.value instanceof TLRPC.TL_jsonString) {
                        TLRPC.TL_jsonString str = (TLRPC.TL_jsonString) value.value;
                        if (!TextUtils.equals(storyVenueSearchBot, str.value)) {
                            storyVenueSearchBot = str.value;
                            editor.putString("storyVenueSearchBot", storyVenueSearchBot);
                            changed = true;
                        }
                    }
                    break;
                }
                case "authorization_autoconfirm_period": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (authorizationAutoconfirmPeriod != num.value) {
                            authorizationAutoconfirmPeriod = (int) num.value;
                            editor.putInt("authorizationAutoconfirmPeriod", authorizationAutoconfirmPeriod);
                            changed = true;
                        }
                    }
                    break;
                }
                case "quote_length_max": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (quoteLengthMax != num.value) {
                            quoteLengthMax = (int) num.value;
                            editor.putInt("quoteLengthMax", quoteLengthMax);
                            changed = true;
                        }
                    }
                    break;
                }
                case "giveaway_gifts_purchase_available": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        if (giveawayGiftsPurchaseAvailable != ((TLRPC.TL_jsonBool) value.value).value) {
                            giveawayGiftsPurchaseAvailable = ((TLRPC.TL_jsonBool) value.value).value;
                            editor.putBoolean("giveawayGiftsPurchaseAvailable", giveawayGiftsPurchaseAvailable);
                            changed = true;
                        }
                    }
                    break;
                }
                case "transcribe_audio_trial_weekly_number": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        transcribeAudioTrialWeeklyNumber = (int) num.value;
                    }
                    break;
                }
                case "transcribe_audio_trial_duration_max": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (transcribeAudioTrialDurationMax != num.value) {
                            transcribeAudioTrialDurationMax = (int) num.value;
                            editor.putInt("transcribeAudioTrialDurationMax", transcribeAudioTrialDurationMax);
                            changed = true;
                        }
                    }
                    break;
                }
                case "transcribe_audio_trial_cooldown_until": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        transcribeAudioTrialCooldownUntil = (int) num.value;
                    }
                    break;
                }
                case "recommended_channels_limit_default": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (recommendedChannelsLimitDefault != num.value) {
                            recommendedChannelsLimitDefault = (int) num.value;
                            editor.putInt("recommendedChannelsLimitDefault", recommendedChannelsLimitDefault);
                            changed = true;
                        }
                    }
                    break;
                }
                case "recommended_channels_limit_premium": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (recommendedChannelsLimitPremium != num.value) {
                            recommendedChannelsLimitPremium = (int) num.value;
                            editor.putInt("recommendedChannelsLimitPremium", recommendedChannelsLimitPremium);
                            changed = true;
                        }
                    }
                    break;
                }
                case "boosts_channel_level_max": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (boostsChannelLevelMax != num.value) {
                            boostsChannelLevelMax = (int) num.value;
                            editor.putInt("boostsChannelLevelMax", boostsChannelLevelMax);
                            changed = true;
                        }
                    }
                    break;
                }
                case "channel_restrict_sponsored_level_min": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (channelRestrictSponsoredLevelMin != num.value) {
                            channelRestrictSponsoredLevelMin = (int) num.value;
                            editor.putInt("channelRestrictSponsoredLevelMin", channelRestrictSponsoredLevelMin);
                            changed = true;
                        }
                    }
                    break;
                }
                case "channel_autotranslation_level_min": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (channelAutotranslationLevelMin != num.value) {
                            channelAutotranslationLevelMin = (int) num.value;
                            editor.putInt("channelAutotranslationLevelMin", channelAutotranslationLevelMin);
                            changed = true;
                        }
                    }
                    break;
                }
                case "channel_bg_icon_level_min": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != channelBgIconLevelMin) {
                            channelBgIconLevelMin = (int) num.value;
                            editor.putInt("channelBgIconLevelMin", channelBgIconLevelMin);
                            changed = true;
                        }
                    }
                    break;
                }
                case "channel_profile_bg_icon_level_min": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != channelProfileIconLevelMin) {
                            channelProfileIconLevelMin = (int) num.value;
                            editor.putInt("channelProfileIconLevelMin", channelProfileIconLevelMin);
                            changed = true;
                        }
                    }
                    break;
                }
                case "channel_emoji_status_level_min": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != channelEmojiStatusLevelMin) {
                            channelEmojiStatusLevelMin = (int) num.value;
                            editor.putInt("channelEmojiStatusLevelMin", channelEmojiStatusLevelMin);
                            changed = true;
                        }
                    }
                    break;
                }
                case "group_custom_wallpaper_level_min": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != groupCustomWallpaperLevelMin) {
                            groupCustomWallpaperLevelMin = (int) num.value;
                            editor.putInt("groupCustomWallpaperLevelMin", groupCustomWallpaperLevelMin);
                            changed = true;
                        }
                    }
                    break;
                }
                case "group_transcribe_level_min": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != groupTranscribeLevelMin) {
                            groupTranscribeLevelMin = (int) num.value;
                            editor.putInt("groupTranscribeLevelMin", groupTranscribeLevelMin);
                            changed = true;
                        }
                    }
                    break;
                }
                case "quick_replies_limit": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != quickRepliesLimit) {
                            quickRepliesLimit = (int) num.value;
                            editor.putInt("quickRepliesLimit", quickRepliesLimit);
                            changed = true;
                        }
                    }
                    break;
                }
                case "group_wallpaper_level_min": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != groupWallpaperLevelMin) {
                            groupWallpaperLevelMin = (int) num.value;
                            editor.putInt("groupWallpaperLevelMin", groupWallpaperLevelMin);
                            changed = true;
                        }
                    }
                    break;
                }
                case "group_emoji_status_level_min": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != groupEmojiStatusLevelMin) {
                            groupEmojiStatusLevelMin = (int) num.value;
                            editor.putInt("groupEmojiStatusLevelMin", groupEmojiStatusLevelMin);
                            changed = true;
                        }
                    }
                    break;
                }
                case "group_emoji_stickers_level_min": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != groupEmojiStickersLevelMin) {
                            groupEmojiStickersLevelMin = (int) num.value;
                            editor.putInt("groupEmojiStickersLevelMin", groupEmojiStickersLevelMin);
                            changed = true;
                        }
                    }
                    break;
                }
                case "group_profile_bg_icon_level_min": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != groupProfileBgIconLevelMin) {
                            groupProfileBgIconLevelMin = (int) num.value;
                            editor.putInt("groupProfileBgIconLevelMin", groupProfileBgIconLevelMin);
                            changed = true;
                        }
                    }
                    break;
                }
                case "channel_wallpaper_level_min": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != channelWallpaperLevelMin) {
                            channelWallpaperLevelMin = (int) num.value;
                            editor.putInt("channelWallpaperLevelMin", channelWallpaperLevelMin);
                            changed = true;
                        }
                    }
                    break;
                }
                case "channel_custom_wallpaper_level_min": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != channelCustomWallpaperLevelMin) {
                            channelCustomWallpaperLevelMin = (int) num.value;
                            editor.putInt("channelCustomWallpaperLevelMin", channelCustomWallpaperLevelMin);
                            changed = true;
                        }
                    }
                    break;
                }
                case "saved_dialogs_pinned_limit_default": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != savedDialogsPinnedLimitDefault) {
                            savedDialogsPinnedLimitDefault = (int) num.value;
                            editor.putInt("savedDialogsPinnedLimitDefault", savedDialogsPinnedLimitDefault);
                            changed = true;
                        }
                    }
                    break;
                }
                case "saved_dialogs_pinned_limit_premium": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != savedDialogsPinnedLimitPremium) {
                            savedDialogsPinnedLimitPremium = (int) num.value;
                            editor.putInt("savedDialogsPinnedLimitPremium", savedDialogsPinnedLimitPremium);
                            changed = true;
                        }
                    }
                    break;
                }
                case "upload_premium_speedup_upload": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (Math.abs(num.value - uploadPremiumSpeedupUpload) >= 0.01f) {
                            uploadPremiumSpeedupUpload = (float) num.value;
                            editor.putFloat("uploadPremiumSpeedupUpload", uploadPremiumSpeedupUpload);
                            changed = true;
                        }
                    }
                    break;
                }
                case "upload_premium_speedup_download": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (Math.abs(num.value - uploadPremiumSpeedupDownload) >= 0.01f) {
                            uploadPremiumSpeedupDownload = (float) num.value;
                            editor.putFloat("uploadPremiumSpeedupDownload", uploadPremiumSpeedupDownload);
                            changed = true;
                        }
                    }
                    break;
                }
                case "upload_premium_speedup_notify_period": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != uploadPremiumSpeedupNotifyPeriod) {
                            uploadPremiumSpeedupNotifyPeriod = (int) num.value;
                            editor.putInt("uploadPremiumSpeedupNotifyPeriod2", uploadPremiumSpeedupNotifyPeriod);
                            changed = true;
                        }
                    }
                    break;
                }
                case "intro_title_length_limit": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != introTitleLengthLimit) {
                            introTitleLengthLimit = (int) num.value;
                            editor.putInt("introTitleLengthLimit", introTitleLengthLimit);
                            changed = true;
                        }
                    }
                    break;
                }
                case "intro_description_length_limit": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != introDescriptionLengthLimit) {
                            introDescriptionLengthLimit = (int) num.value;
                            editor.putInt("introDescriptionLengthLimit", introDescriptionLengthLimit);
                            changed = true;
                        }
                    }
                    break;
                }
                case "business_chat_links_limit": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != businessChatLinksLimit) {
                            businessChatLinksLimit = (int) num.value;
                            editor.putInt("businessChatLinksLimit", businessChatLinksLimit);
                            changed = true;
                        }
                    }
                    break;
                }
                case "channel_revenue_withdrawal_enabled": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool num = (TLRPC.TL_jsonBool) value.value;
                        if (num.value != channelRevenueWithdrawalEnabled) {
                            channelRevenueWithdrawalEnabled = num.value;
                            editor.putBoolean("channelRevenueWithdrawalEnabled", channelRevenueWithdrawalEnabled);
                            changed = true;
                        }
                    }
                    break;
                }
                case "new_noncontact_peers_require_premium_without_ownpremium": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool num = (TLRPC.TL_jsonBool) value.value;
                        if (num.value != newNoncontactPeersRequirePremiumWithoutOwnpremium) {
                            newNoncontactPeersRequirePremiumWithoutOwnpremium = num.value;
                            editor.putBoolean("newNoncontactPeersRequirePremiumWithoutOwnpremium", newNoncontactPeersRequirePremiumWithoutOwnpremium);
                            changed = true;
                        }
                    }
                    break;
                }
                case "reactions_uniq_max": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != reactionsUniqMax) {
                            reactionsUniqMax = (int) num.value;
                            editor.putInt("reactionsUniqMax", reactionsUniqMax);
                            changed = true;
                        }
                    }
                    break;
                }
                case "premium_manage_subscription_url": {
                    if (value.value instanceof TLRPC.TL_jsonString) {
                        TLRPC.TL_jsonString str = (TLRPC.TL_jsonString) value.value;
                        if (!TextUtils.equals(str.value, premiumManageSubscriptionUrl)) {
                            premiumManageSubscriptionUrl = str.value;
                            editor.putString("premiumManageSubscriptionUrl", premiumManageSubscriptionUrl);
                            changed = true;
                        }
                    }
                    break;
                }
                case "android_disable_round_camera2": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool bool = (TLRPC.TL_jsonBool) value.value;
                        if (bool.value != androidDisableRoundCamera2) {
                            androidDisableRoundCamera2 = bool.value;
                            editor.putBoolean("androidDisableRoundCamera2", androidDisableRoundCamera2);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stories_pinned_to_top_count_max": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != storiesPinnedToTopCountMax) {
                            storiesPinnedToTopCountMax = (int) num.value;
                            editor.putInt("storiesPinnedToTopCountMax", storiesPinnedToTopCountMax);
                            changed = true;
                        }
                    }
                    break;
                }
                case "show_annual_per_month": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool bool = (TLRPC.TL_jsonBool) value.value;
                        if (bool.value != showAnnualPerMonth) {
                            showAnnualPerMonth = bool.value;
                            editor.putBoolean("showAnnualPerMonth", showAnnualPerMonth);
                            changed = true;
                        }
                    }
                    break;
                }
                case "can_edit_factcheck": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool bool = (TLRPC.TL_jsonBool) value.value;
                        if (bool.value != canEditFactcheck) {
                            canEditFactcheck = bool.value;
                            editor.putBoolean("canEditFactcheck", canEditFactcheck);
                            changed = true;
                        }
                    }
                    break;
                }
                case "factcheck_length_limit": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if ((int) num.value != factcheckLengthLimit) {
                            factcheckLengthLimit = (int) num.value;
                            editor.putInt("factcheckLengthLimit", factcheckLengthLimit);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stars_revenue_withdrawal_min": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if ((long) num.value != starsRevenueWithdrawalMin) {
                            starsRevenueWithdrawalMin = (long) num.value;
                            editor.putLong("starsRevenueWithdrawalMin", starsRevenueWithdrawalMin);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stars_paid_post_amount_max": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if ((long) num.value != starsPaidPostAmountMax) {
                            starsPaidPostAmountMax = (long) num.value;
                            editor.putLong("starsPaidPostAmountMax", starsPaidPostAmountMax);
                            changed = true;
                        }
                    }
                    break;
                }
                case "bot_preview_medias_max": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if ((int) num.value != botPreviewMediasMax) {
                            botPreviewMediasMax = (int) num.value;
                            editor.putInt("botPreviewMediasMax", botPreviewMediasMax);
                            changed = true;
                        }
                    }
                    break;
                }
                case "ton_proxy_address": {
                    if (value.value instanceof TLRPC.TL_jsonString) {
                        TLRPC.TL_jsonString str = (TLRPC.TL_jsonString) value.value;
                        if (!TextUtils.equals(str.value, tonProxyAddress)) {
                            tonProxyAddress = str.value;
                            editor.putString("tonProxyAddress", tonProxyAddress);
                            changed = true;
                        }
                    }
                    break;
                }
                case "web_app_allowed_protocols": {
                    HashSet<String> newProtocols = new HashSet<>();
                    if (value.value instanceof TLRPC.TL_jsonArray) {
                        TLRPC.TL_jsonArray array = (TLRPC.TL_jsonArray) value.value;
                        for (int b = 0, N2 = array.value.size(); b < N2; b++) {
                            TLRPC.JSONValue val = array.value.get(b);
                            if (val instanceof TLRPC.TL_jsonString) {
                                TLRPC.TL_jsonString string = (TLRPC.TL_jsonString) val;
                                newProtocols.add(string.value.toLowerCase());
                            }
                        }
                    }
                    if (!webAppAllowedProtocols.equals(newProtocols)) {
                        webAppAllowedProtocols = newProtocols;
                        editor.putStringSet("webAppAllowedProtocols", webAppAllowedProtocols);
                        changed = true;
                    }
                    break;
                }
                case "starref_start_param_prefixes": {
                    HashSet<String> newPrefixes = new HashSet<>();
                    if (value.value instanceof TLRPC.TL_jsonArray) {
                        TLRPC.TL_jsonArray array = (TLRPC.TL_jsonArray) value.value;
                        for (int b = 0, N2 = array.value.size(); b < N2; b++) {
                            TLRPC.JSONValue val = array.value.get(b);
                            if (val instanceof TLRPC.TL_jsonString) {
                                TLRPC.TL_jsonString string = (TLRPC.TL_jsonString) val;
                                newPrefixes.add(string.value.toLowerCase());
                            }
                        }
                    }
                    if (!starrefStartParamPrefixes.equals(newPrefixes)) {
                        starrefStartParamPrefixes = newPrefixes;
                        editor.putStringSet("starrefStartParamPrefixes", starrefStartParamPrefixes);
                        changed = true;
                    }
                    break;
                }
                case "weather_search_username": {
                    if (value.value instanceof TLRPC.TL_jsonString) {
                        TLRPC.TL_jsonString str = (TLRPC.TL_jsonString) value.value;
                        if (!TextUtils.equals(str.value, weatherSearchUsername)) {
                            weatherSearchUsername = str.value;
                            editor.putString("weatherSearchUsername", weatherSearchUsername);
                            changed = true;
                        }
                    }
                    break;
                }
                case "story_weather_preload": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool bool = (TLRPC.TL_jsonBool) value.value;
                        if (bool.value != storyWeatherPreload) {
                            storyWeatherPreload = bool.value;
                            editor.putBoolean("storyWeatherPreload", storyWeatherPreload);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stars_gifts_enabled": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool bool = (TLRPC.TL_jsonBool) value.value;
                        if (bool.value != starsGiftsEnabled) {
                            starsGiftsEnabled = bool.value;
                            editor.putBoolean("starsGiftsEnabled", starsGiftsEnabled);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stargifts_blocked": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool bool = (TLRPC.TL_jsonBool) value.value;
                        if (bool.value != stargiftsBlocked) {
                            stargiftsBlocked = bool.value;
                            editor.putBoolean("stargiftsBlocked", stargiftsBlocked);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stars_paid_reaction_amount_max": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if ((long) num.value != starsPaidReactionAmountMax) {
                            starsPaidReactionAmountMax = (long) num.value;
                            editor.putLong("starsPaidReactionAmountMax", starsPaidReactionAmountMax);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stars_subscription_amount_max": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if ((long) num.value != starsSubscriptionAmountMax) {
                            starsSubscriptionAmountMax = (long) num.value;
                            editor.putLong("starsSubscriptionAmountMax", starsSubscriptionAmountMax);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stars_usd_sell_rate_x1000": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (Math.abs(num.value - starsUsdSellRate1000) > 0.001f) {
                            starsUsdSellRate1000 = (float) num.value;
                            editor.putFloat("starsUsdSellRate1000", starsUsdSellRate1000);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stars_usd_withdraw_rate_x1000": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (Math.abs(num.value - starsUsdWithdrawRate1000) > 0.001f) {
                            starsUsdWithdrawRate1000 = (float) num.value;
                            editor.putFloat("starsUsdWithdrawRate1000", starsUsdWithdrawRate1000);
                            changed = true;
                        }
                    }
                    break;
                }
                case "sponsored_links_inapp_allow": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool bool = (TLRPC.TL_jsonBool) value.value;
                        if (bool.value != sponsoredLinksInappAllow) {
                            sponsoredLinksInappAllow = bool.value;
                            editor.putBoolean("sponsoredLinksInappAllow", sponsoredLinksInappAllow);
                            changed = true;
                        }
                    }
                    break;
                }
                case "ignore_restriction_reasons": {
                    HashSet<String> newReasons = new HashSet<>();
                    if (value.value instanceof TLRPC.TL_jsonArray) {
                        TLRPC.TL_jsonArray array = (TLRPC.TL_jsonArray) value.value;
                        for (int b = 0, N2 = array.value.size(); b < N2; b++) {
                            TLRPC.JSONValue val = array.value.get(b);
                            if (val instanceof TLRPC.TL_jsonString) {
                                TLRPC.TL_jsonString string = (TLRPC.TL_jsonString) val;
                                newReasons.add(string.value.toLowerCase());
                            }
                        }
                    }
                    if (!ignoreRestrictionReasons.equals(newReasons)) {
                        ignoreRestrictionReasons = newReasons;
                        editor.putStringSet("ignoreRestrictionReasons", ignoreRestrictionReasons);
                        changed = true;
                    }
                    break;
                }
                case "starref_program_allowed": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool bool = (TLRPC.TL_jsonBool) value.value;
                        if (bool.value != starrefProgramAllowed) {
                            starrefProgramAllowed = bool.value;
                            editor.putBoolean("starrefProgramAllowed", starrefProgramAllowed);
                            changed = true;
                        }
                    }
                    break;
                }
                case "starref_connect_allowed": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool bool = (TLRPC.TL_jsonBool) value.value;
                        if (bool.value != starrefConnectAllowed) {
                            starrefConnectAllowed = bool.value;
                            editor.putBoolean("starrefConnectAllowed", starrefConnectAllowed);
                            changed = true;
                        }
                    }
                    break;
                }
                case "starref_min_commission_permille": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != starrefMinCommissionPermille) {
                            starrefMinCommissionPermille = (int) num.value;
                            editor.putInt("starrefMinCommissionPermille", starrefMinCommissionPermille);
                            changed = true;
                        }
                    }
                    break;
                }
                case "starref_max_commission_permille": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != starrefMaxCommissionPermille) {
                            starrefMaxCommissionPermille = (int) num.value;
                            editor.putInt("starrefMaxCommissionPermille", starrefMaxCommissionPermille);
                            changed = true;
                        }
                    }
                    break;
                }
                case "bot_verification_description_length_limit": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != botVerificationDescriptionLengthLimit) {
                            botVerificationDescriptionLengthLimit = (int) num.value;
                            editor.putInt("botVerificationDescriptionLengthLimit", botVerificationDescriptionLengthLimit);
                            changed = true;
                        }
                    }
                    break;
                }
                case "freeze_since_date": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != freezeSinceDate) {
                            freezeSinceDate = (long) num.value;
                            editor.putLong("freezeSinceDate", freezeSinceDate);
                            changed = true;
                        }
                    }
                    break;
                }
                case "freeze_until_date": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != freezeUntilDate) {
                            freezeUntilDate = (long) num.value;
                            editor.putLong("freezeUntilDate", freezeUntilDate);
                            changed = true;
                        }
                    }
                    break;
                }
                case "freeze_appeal_url": {
                    if (value.value instanceof TLRPC.TL_jsonString) {
                        TLRPC.TL_jsonString num = (TLRPC.TL_jsonString) value.value;
                        if (!TextUtils.equals(num.value, freezeAppealUrl)) {
                            freezeAppealUrl = num.value;
                            editor.putString("freezeAppealUrl", freezeAppealUrl);
                            changed = true;
                        }
                    }
                    break;
                }
                case "conference_call_size_limit": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != conferenceCallSizeLimit) {
                            conferenceCallSizeLimit = (int) num.value;
                            editor.putInt("conferenceCallSizeLimit", conferenceCallSizeLimit);
                            changed = true;
                        }
                    }
                    break;
                }
                case "call_requests_disabled": {
                    if (value.value instanceof TLRPC.TL_jsonBool) {
                        TLRPC.TL_jsonBool bool = (TLRPC.TL_jsonBool) value.value;
                        if (bool.value != callRequestsDisabled) {
                            callRequestsDisabled = bool.value;
                            editor.putBoolean("callRequestsDisabled", callRequestsDisabled);
                            changed = true;
                        }
                    }
                    break;
                }
                case "todo_items_max": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != todoItemsMax) {
                            todoItemsMax = (int) num.value;
                            editor.putInt("todoItemsMax", todoItemsMax);
                            changed = true;
                        }
                    }
                    break;
                }
                case "todo_title_length_max": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != todoTitleLengthMax) {
                            todoTitleLengthMax = (int) num.value;
                            editor.putInt("todoTitleLengthMax", todoTitleLengthMax);
                            changed = true;
                        }
                    }
                    break;
                }
                case "todo_item_length_max": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (num.value != todoItemLengthMax) {
                            todoItemLengthMax = (int) num.value;
                            editor.putInt("todoItemLengthMax", todoItemLengthMax);
                            changed = true;
                        }
                    }
                    break;
                }
                case "translations_manual_enabled": {
                    if (value.value instanceof TLRPC.TL_jsonString) {
                        TLRPC.TL_jsonString str = (TLRPC.TL_jsonString) value.value;
                        if (!TextUtils.equals(translationsManualEnabled, str.value)) {
                            translationsManualEnabled = str.value;
                            editor.putString("translationsManualEnabled", translationsManualEnabled);
                            changed = true;
                        }
                    }
                    break;
                }
                case "translations_auto_enabled": {
                    if (value.value instanceof TLRPC.TL_jsonString) {
                        TLRPC.TL_jsonString str = (TLRPC.TL_jsonString) value.value;
                        if (!TextUtils.equals(translationsAutoEnabled, str.value)) {
                            translationsAutoEnabled = str.value;
                            editor.putString("translationsAutoEnabled", translationsAutoEnabled);
                            changed = true;
                        }
                    }
                    break;
                }
                case "whitelisted_bots": {
                    final HashSet<Long> set = new HashSet<>();
                    if (value.value instanceof TLRPC.TL_jsonArray) {
                        final ArrayList<TLRPC.JSONValue> array = ((TLRPC.TL_jsonArray) value.value).value;
                        for (int i = 0; i < array.size(); ++i) {
                            if (array.get(i) instanceof TLRPC.TL_jsonNumber) {
                                set.add((long) ((TLRPC.TL_jsonNumber) array.get(i)).value);
                            }
                        }
                    }
                    if (!set.equals(whitelistedBots)) {
                        whitelistedBots = set;
                        editor.putStringSet("whitelistedBots", set.stream().map(id -> String.valueOf(id)).collect(Collectors.toCollection(HashSet::new)));
                        changed = true;
                    }
                    break;
                }
                case "stars_groupcall_message_amount_max": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        final TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (starsGroupcallMessageAmountMax != (int) num.value) {
                            editor.putInt("starsGroupcallMessageAmountMax", starsGroupcallMessageAmountMax = (int) num.value);
                            changed = true;
                        }
                    }
                    break;
                }
                case "stars_groupcall_message_limits": {
                    if (value.value instanceof TLRPC.TL_jsonArray) {
                        final int[] tiers = parseTiers((TLRPC.TL_jsonArray) value.value);
                        if (!tiersEqual(tiers, starsGroupcallMessageLimits)) {
                            editor.putString("starsGroupcallMessageLimits", tiersToString(starsGroupcallMessageLimits = tiers));
                            changed = true;
                        }
                    }
                    break;
                }
                case "ton_stakedice_stake_amount_min": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        final TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (tonStakeddiceStakeAmountMin != (long) num.value) {
                            editor.putLong("tonStakeddiceStakeAmountMin", tonStakeddiceStakeAmountMin = (long) num.value);
                            changed = true;
                        }
                    }
                    break;
                }
                case "ton_stakedice_stake_amount_max": {
                    if (value.value instanceof TLRPC.TL_jsonNumber) {
                        final TLRPC.TL_jsonNumber num = (TLRPC.TL_jsonNumber) value.value;
                        if (tonStakeddiceStakeAmountMax != (long) num.value) {
                            editor.putLong("tonStakeddiceStakeAmountMax", tonStakeddiceStakeAmountMax = (long) num.value);
                            changed = true;
                        }
                    }
                    break;
                }
                case "ton_stakedice_stake_suggested_amounts": {
                    if (value.value instanceof TLRPC.TL_jsonArray) {
                        final TLRPC.TL_jsonArray arr = (TLRPC.TL_jsonArray) value.value;
                        final long[] values = new long[arr.value.size()];
                        for (int i = 0; i < arr.value.size(); ++i) {
                            if (arr.value.get(i) instanceof TLRPC.TL_jsonNumber) {
                                values[i] = (long) ((TLRPC.TL_jsonNumber) arr.value.get(i)).value;
                            }
                        }
                        if (!Arrays.equals(values, tonStakediceStakeSuggestedAmounts)) {
                            editor.putString("tonStakeddiceStakeSuggestedAmounts", Arrays.stream(tonStakediceStakeSuggestedAmounts = values).mapToObj(String::valueOf).collect(Collectors.joining(",")));
                            changed = true;
                        }
                    }
                    break;
                }
                case "stargifts_craft_attribute_permilles": {
                    if (value.value instanceof TLRPC.TL_jsonArray) {
                        final TLRPC.TL_jsonArray arr = (TLRPC.TL_jsonArray) value.value;
                        final int[][] values = new int[arr.value.size()][];
                        for (int i = 0; i < arr.value.size(); ++i) {
                            if (arr.value.get(i) instanceof TLRPC.TL_jsonArray) {
                                final TLRPC.TL_jsonArray darr = (TLRPC.TL_jsonArray) arr.value.get(i);
                                values[i] = new int[darr.value.size()];
                                for (int j = 0; j < darr.value.size(); ++j) {
                                    if (darr.value.get(j) instanceof TLRPC.TL_jsonNumber)
                                        values[i][j] = (int) ((TLRPC.TL_jsonNumber) darr.value.get(j)).value;
                                }
                            }
                        }
                        if (!Arrays.deepEquals(values, stargiftsCraftAttributesPermilles)) {
                            editor.putString("stargiftsCraftAttributesPermilles",
                                Arrays.stream(stargiftsCraftAttributesPermilles = values)
                                    .map(row -> Arrays.stream(row)
                                        .mapToObj(String::valueOf)
                                        .collect(Collectors.joining(",")))
                                    .collect(Collectors.joining(",,"))
                            );
                            changed = true;
                        }
                    }
                    break;
                }
            }
        }

        if (transcribeAudioTrialWeeklyNumber != this.transcribeAudioTrialWeeklyNumber) {
            this.transcribeAudioTrialWeeklyNumber = transcribeAudioTrialWeeklyNumber;
            editor.putInt("transcribeAudioTrialWeeklyNumber", transcribeAudioTrialWeeklyNumber);
            if (transcribeAudioTrialCurrentNumber <= 0 && (transcribeAudioTrialCooldownUntil == 0 || getConnectionsManager().getCurrentTime() > transcribeAudioTrialCooldownUntil)) {
                transcribeAudioTrialCurrentNumber = transcribeAudioTrialWeeklyNumber;
                editor.putInt("transcribeAudioTrialCurrentNumber", transcribeAudioTrialCurrentNumber);
            } else if (transcribeAudioTrialCurrentNumber > transcribeAudioTrialWeeklyNumber) {
                transcribeAudioTrialCurrentNumber = transcribeAudioTrialWeeklyNumber;
                editor.putInt("transcribeAudioTrialCurrentNumber", transcribeAudioTrialCurrentNumber);
            }
            changed = true;
        }
        if (transcribeAudioTrialCooldownUntil != this.transcribeAudioTrialCooldownUntil) {
            this.transcribeAudioTrialCooldownUntil = transcribeAudioTrialCooldownUntil;
            editor.putInt("transcribeAudioTrialCooldownUntil", transcribeAudioTrialCooldownUntil);
            changed = true;
            scheduleTranscriptionUpdate();
        }

        if (changed) {
            editor.apply();
            AndroidUtilities.runOnUIThread(() -> {
                getNotificationCenter().postNotificationName(NotificationCenter.appConfigUpdated);
            });
        }
        if (liteAppOptions != null) {
            LiteMode.updatePresets(liteAppOptions);
        }
        if (keelAliveChanged) {
            ApplicationLoader.startPushService();
            ConnectionsManager connectionsManager = getConnectionsManager();
            connectionsManager.setPushConnectionEnabled(connectionsManager.isPushConnectionEnabled());
        }
        if (storiesChanged) {
            AndroidUtilities.runOnUIThread(() -> {
                getNotificationCenter().postNotificationName(NotificationCenter.storiesEnabledUpdate);
            });
        }
        logDeviceStats();
    }

    public void updateTranscribeAudioTrialCurrentNumber(int num) {
        if (num != transcribeAudioTrialCurrentNumber) {
            transcribeAudioTrialCurrentNumber = num;
            mainPreferences.edit()
                .putInt("transcribeAudioTrialCurrentNumber", transcribeAudioTrialCurrentNumber)
                .apply();
        }
    }

    public void updateTranscribeAudioTrialCooldownUntil(int until) {
        if (until != transcribeAudioTrialCooldownUntil) {
            transcribeAudioTrialCooldownUntil = until;
            mainPreferences.edit()
                .putInt("transcribeAudioTrialCooldownUntil", transcribeAudioTrialCooldownUntil)
                .apply();
            scheduleTranscriptionUpdate();
        }
    }

    private void scheduleTranscriptionUpdate() {
        AndroidUtilities.runOnUIThread(() -> {
            AndroidUtilities.cancelRunOnUIThread(notifyTranscriptionAudioCooldownUpdate);
            final long wait = transcribeAudioTrialCooldownUntil - getConnectionsManager().getCurrentTime();
            if (wait > 0) {
                AndroidUtilities.runOnUIThread(notifyTranscriptionAudioCooldownUpdate, wait);
            }
        });
    }
    private final Runnable notifyTranscriptionAudioCooldownUpdate = () -> getNotificationCenter().postNotificationName(NotificationCenter.updateTranscriptionLock);

    public static class PeerColors {

        public static final int TYPE_NAME = 0;
        public static final int TYPE_PROFILE = 1;

        public final int type;
        public final int hash;

        public final ArrayList<PeerColor> colors = new ArrayList<>();
        private final LongSparseArray<PeerColor> colorsById = new LongSparseArray<>();

        public boolean needUpdate() {
            boolean noLevels = true;
            boolean hasStandardColors = false;
            for (int i = 0; i < colors.size(); ++i) {
                if (colors.get(i).channelLvl > 0) {
                    noLevels = false;
                }
                if (colors.get(i).id < 7) {
                    hasStandardColors = true;
                }
            }

            if (type == TYPE_PROFILE && !noLevels) {
                noLevels = true;
                for (PeerColor color : colors) {
                    if (color.groupLvl > 0) {
                        noLevels = false;
                        break;
                    }
                }
            }
            return noLevels || type == TYPE_NAME && !hasStandardColors;
        }

        public int colorsAvailable(int lvl, boolean isGroup) {
            int count = 0;
            for (int i = 0; i < colors.size(); ++i) {
                MessagesController.PeerColor peerColor = colors.get(i);
                if (!peerColor.hidden && lvl >= peerColor.getLvl(isGroup)) {
                    count++;
                }
            }
            return count;
        }

        public int maxLevel() {
            return maxLevel(false);
        }

        public int maxLevel(boolean isGroup) {
            int maxLvl = 0;
            for (int i = 0; i < colors.size(); ++i) {
                MessagesController.PeerColor peerColor = colors.get(i);
                if (!peerColor.hidden) {
                    maxLvl = Math.max(maxLvl, peerColor.getLvl(isGroup));
                }
            }
            return maxLvl;
        }

        public int minLevel() {
            return minLevel(false);
        }

        public int minLevel(boolean isGroup) {
            int minLvl = maxLevel(isGroup);
            for (int i = 0; i < colors.size(); ++i) {
                MessagesController.PeerColor peerColor = colors.get(i);
                if (!peerColor.hidden) {
                    minLvl = Math.min(minLvl, peerColor.getLvl(isGroup));
                }
            }
            return minLvl;
        }

        private PeerColors(int type, int hash) {
            this.type = type;
            this.hash = hash;
        }

        @Nullable
        public PeerColor getColor(int colorId) {
            return colorsById.get(colorId);
        }

        @NonNull
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (hash != 0) {
                sb.append("@").append(hash).append("^");
            }
            for (int i = 0; i < colors.size(); ++i) {
                PeerColor color = colors.get(i);
                if (i > 0) sb.append(";");
                color.appendString(sb);
            }
            return sb.toString();
        }

        public static PeerColors fromString(int type, String str) {
            if (str == null) return null;
            int hash = 0;
            if (str.startsWith("@")) {
                int index = str.indexOf("^");
                if (index >= 0) {
                    hash = Utilities.parseInt(str.substring(1, index));
                    str = str.substring(index + 1);
                }
            }
            final PeerColors peerColors = new PeerColors(type, hash);
            final String[] colorParts = str.split(";");
            for (int i = 0; i < colorParts.length; ++i) {
                PeerColor peerColor = PeerColor.fromString(colorParts[i]);
                if (peerColor == null)
                    continue;
                peerColor.isDefaultName = peerColor.id < 7 && type == TYPE_NAME;
                if (!peerColor.hidden)
                    peerColors.colors.add(peerColor);
                peerColors.colorsById.put(peerColor.id, peerColor);
            }
            return peerColors;
        }

        private static int color(String str) {
            return Integer.parseUnsignedInt("ff" + str, 16);
        }

        public static PeerColors fromTL(int type, TLRPC.TL_help_peerColors tl) {
            if (tl == null) return null;
            try {
                PeerColors peerColors = new PeerColors(type, tl.hash);
                for (int i = 0; i < tl.colors.size(); ++i) {
                    PeerColor peerColor = PeerColor.fromTL(tl.colors.get(i));
                    if (peerColor == null) continue;
                    peerColor.isDefaultName = peerColor.id < 7 && type == TYPE_NAME;
                    if (!peerColor.hidden)
                        peerColors.colors.add(peerColor);
                    peerColors.colorsById.put(peerColor.id, peerColor);
                }
                return peerColors;
            } catch (Exception e) {
                FileLog.e(e);
            }
            return null;
        }

        public static PeerColors fromJSON(
            int type,
            TLRPC.TL_jsonObject peer_colors,
            TLRPC.TL_jsonObject dark_peer_colors,
            TLRPC.TL_jsonArray peer_colors_available
        ) {
            try {
                PeerColors peerColors = new PeerColors(type, 0);
                if (peer_colors != null) {
                    for (TLRPC.TL_jsonObjectValue pair : peer_colors.value) {
                        final int id = Utilities.parseInt(pair.key);
                        if (!(pair.value instanceof TLRPC.TL_jsonArray))
                            continue;
                        ArrayList<TLRPC.JSONValue> val = ((TLRPC.TL_jsonArray) pair.value).value;
                        if (val.isEmpty())
                            continue;

                        PeerColor peerColor = new PeerColor();
                        try {
                            peerColor.id = id;
                            for (int i = 0; i < 6; ++i)
                                peerColor.colors[i] = peerColor.darkColors[i] = val.size() > i ? color(((TLRPC.TL_jsonString) val.get(i)).value) : peerColor.colors[0];
                        } catch (Exception e2) {
                            FileLog.e(e2);
                            continue;
                        }
                        peerColor.isDefaultName = peerColor.id < 7 && type == TYPE_NAME;
                        peerColors.colorsById.put(id, peerColor);
                    }
                }
                if (dark_peer_colors != null) {
                    for (TLRPC.TL_jsonObjectValue pair : dark_peer_colors.value) {
                        final int id = Utilities.parseInt(pair.key);
                        if (!(pair.value instanceof TLRPC.TL_jsonArray))
                            continue;
                        ArrayList<TLRPC.JSONValue> val = ((TLRPC.TL_jsonArray) pair.value).value;
                        if (val.isEmpty())
                            continue;

                        PeerColor peerColor = peerColors.colorsById.get(id);
                        if (peerColor == null) continue;
                        try {
                            peerColor.id = id;
                            for (int i = 0; i < 6; ++i)
                                peerColor.darkColors[i] = val.size() > i ? color(((TLRPC.TL_jsonString) val.get(i)).value) : peerColor.darkColors[0];
                        } catch (Exception e2) {
                            FileLog.e(e2);
                            continue;
                        }
                        peerColors.colorsById.put(id, peerColor);
                    }
                }
                peerColors.colors.clear();
                if (peer_colors_available != null) {
                    for (TLRPC.JSONValue idvalue : peer_colors_available.value) {
                        if (!(idvalue instanceof TLRPC.TL_jsonNumber))
                            continue;
                        final int id = (int) ((TLRPC.TL_jsonNumber) idvalue).value;
                        PeerColor color = peerColors.colorsById.get(id);
                        if (color == null) continue;
                        peerColors.colors.add(color);
                    }
                }
                return peerColors;
            } catch (Exception e) {
                FileLog.e(e);
            }
            return null;
        }
    }

    public static class PeerColor {
        public int patternColor = 0;
        public int textColor = 0;
        public boolean isDefaultName;
        public int id;
        public boolean hidden;
        public int channelLvl;
        public int groupLvl;
        private final int[] colors = new int[6];
        private final int[] darkColors = new int[6];
        public int getColor(int i, Theme.ResourcesProvider resourcesProvider) {
            if (i < 0 || i > 5) return 0;
            if (isDefaultName && id >= 0 && id < 7) {
                return Theme.getColor(Theme.keys_avatar_nameInMessage[id], resourcesProvider);
            }
            final boolean isDark = resourcesProvider != null ? resourcesProvider.isDark() : Theme.isCurrentThemeDark();
            return (isDark ? darkColors : colors)[i];
        }
        public int getLvl(boolean isGroup) {
            return isGroup ? groupLvl : channelLvl;
        }
        public int getColor1(boolean isDark) {
            return (isDark ? darkColors : colors)[0];
        }
        public int getColor2(boolean isDark) {
            return (isDark ? darkColors : colors)[1];
        }
        public int getColor3(boolean isDark) {
            return (isDark ? darkColors : colors)[2];
        }
        public int getColor4(boolean isDark) {
            return (isDark ? darkColors : colors)[3];
        }
        public int getColor5(boolean isDark) {
            return (isDark ? darkColors : colors)[4];
        }
        public int getColor6(boolean isDark) {
            return (isDark ? darkColors : colors)[5];
        }
        public int getColor1() {
            return (Theme.isCurrentThemeDark() ? darkColors : colors)[0];
        }
        public int getColor2() {
            return (Theme.isCurrentThemeDark() ? darkColors : colors)[1];
        }
        public int getColor3() {
            return (Theme.isCurrentThemeDark() ? darkColors : colors)[2];
        }
        public int getColor4() {
            return (Theme.isCurrentThemeDark() ? darkColors : colors)[3];
        }
        public int getColor5() {
            return (Theme.isCurrentThemeDark() ? darkColors : colors)[4];
        }
        public boolean hasColor2() {
            return getColor2() != getColor1();
        }
        public boolean hasColor3() {
            return getColor3() != getColor1();
        }
        public boolean hasColor2(boolean isDark) {
            return getColor2(isDark) != getColor1(isDark);
        }
        public boolean hasColor3(boolean isDark) {
            return getColor3(isDark) != getColor1(isDark);
        }
        public boolean hasColor6(boolean isDark) {
            return getColor6(isDark) != getColor1(isDark);
        }
        public int getBgColor1(boolean isDark) {
            return hasColor6(isDark) ? getColor3(isDark) : getColor2(isDark);
        }
        public int getBgColor2(boolean isDark) {
            return hasColor6(isDark) ? getColor4(isDark) : getColor2(isDark);
        }
        public int getStoryColor1(boolean isDark) {
            return hasColor6(isDark) ? getColor5(isDark) : getColor3(isDark);
        }
        public int getStoryColor2(boolean isDark) {
            return hasColor6(isDark) ? getColor6(isDark) : getColor4(isDark);
        }
        public int getAvatarColor1() {
            return ColorUtils.blendARGB(getBgColor2(false), getStoryColor2(false), .5f);
        }
        public int getAvatarColor2() {
            return ColorUtils.blendARGB(getBgColor1(false), getStoryColor1(false), .5f);
        }
        public void appendString(StringBuilder sb) {
            sb.append("#");
            if (hidden) sb.append("H");
            if (channelLvl != 0 || groupLvl != 0) {
                sb.append("[").append(channelLvl).append(",").append(groupLvl).append("]");
            }
            sb.append(id);
            sb.append("{");
            sb.append(colors[0]);
            if (colors[1] != colors[0]) {
                sb.append(",");
                sb.append(colors[1]);
                if (colors[2] != colors[0] || colors[3] != colors[0]) {
                    sb.append(",");
                    sb.append(colors[2]);
                    sb.append(",");
                    sb.append(colors[3]);
                    if (colors[4] != colors[0] || colors[5] != colors[0]) {
                        sb.append(",");
                        sb.append(colors[4]);
                        sb.append(",");
                        sb.append(colors[5]);
                    }
                }
            }
            if (darkColors[0] != colors[0] || darkColors[1] != colors[1] || darkColors[2] != colors[2]) {
                sb.append("@");
                sb.append(darkColors[0]);
                if (darkColors[1] != darkColors[0]) {
                    sb.append(",");
                    sb.append(darkColors[1]);
                    if (darkColors[2] != darkColors[0] || darkColors[3] != darkColors[0]) {
                        sb.append(",");
                        sb.append(darkColors[2]);
                        sb.append(",");
                        sb.append(darkColors[3]);
                        if (darkColors[4] != darkColors[0] || darkColors[5] != darkColors[0]) {
                            sb.append(",");
                            sb.append(darkColors[4]);
                            sb.append(",");
                            sb.append(darkColors[5]);
                        }
                    }
                }
            }
            sb.append("}");
        }

        public static PeerColor fromPeerCollectible(TLRPC.PeerColor peer_color) {
            if (!(peer_color instanceof TLRPC.TL_peerColorCollectible)) return null;
            final TLRPC.TL_peerColorCollectible color = (TLRPC.TL_peerColorCollectible) peer_color;
            final PeerColor peerColor = new PeerColor();
            peerColor.id = -1;
            peerColor.hidden = true;
            // TODO
            return peerColor;
        }

        public static PeerColor fromCollectible(TLRPC.EmojiStatus status) {
            if (!(status instanceof TLRPC.TL_emojiStatusCollectible)) return null;
            final TLRPC.TL_emojiStatusCollectible s = (TLRPC.TL_emojiStatusCollectible) status;
            final PeerColor peerColor = new PeerColor();
            peerColor.id = -1;
            peerColor.hidden = true;
            peerColor.colors[0] = s.edge_color | 0xFF000000;
            peerColor.colors[1] = s.center_color | 0xFF000000;
            peerColor.colors[2] = s.edge_color | 0xFF000000;
            peerColor.colors[3] = s.center_color | 0xFF000000;
            peerColor.colors[4] = s.text_color | 0xFF000000;
            peerColor.colors[5] = s.text_color | 0xFF000000;
            System.arraycopy(peerColor.colors, 0, peerColor.darkColors, 0, 6);
            peerColor.patternColor = s.pattern_color | 0xFF000000;
            peerColor.textColor = s.text_color | 0xFF000000;
            return peerColor;
        }

        public static PeerColor fromTL(TLRPC.TL_help_peerColorOption tl) {
            if (tl == null) return null;

            final PeerColor peerColor = new PeerColor();
            peerColor.id = tl.color_id;
            peerColor.hidden = tl.hidden;
            if ((tl.flags & 8) != 0) {
                peerColor.channelLvl = tl.channel_min_level;
            }
            if ((tl.flags & 16) != 0) {
                peerColor.groupLvl = tl.group_min_level;
            }

            System.arraycopy(optionToColors(tl.colors), 0, peerColor.colors, 0, 6);
            System.arraycopy(optionToColors(tl.dark_colors), 0, peerColor.darkColors, 0, 6);
            return peerColor;
        }

        public static int[] optionToColors(TLRPC.help_PeerColorSet set) {
            final int[] colors = new int[] {0, 0, 0, 0, 0, 0};
            ArrayList<Integer> finalColorList = null;
            if (set instanceof TLRPC.TL_help_peerColorSet) {
                finalColorList = ((TLRPC.TL_help_peerColorSet) set).colors;
            } else if (set instanceof TLRPC.TL_help_peerColorProfileSet) {
                ArrayList<Integer> colorList1 = ((TLRPC.TL_help_peerColorProfileSet) set).palette_colors;
                ArrayList<Integer> colorList2 = ((TLRPC.TL_help_peerColorProfileSet) set).bg_colors;
                ArrayList<Integer> colorList3 = ((TLRPC.TL_help_peerColorProfileSet) set).story_colors;
                finalColorList = new ArrayList<Integer>();
                if (colorList1 != null) {
                    for (int i = 0; i < Math.min(2, colorList1.size()); ++i)
                        finalColorList.add(colorList1.get(i));
                }
                if (colorList2 != null) {
                    for (int i = 0; i < Math.min(2, colorList2.size()); ++i)
                        finalColorList.add(colorList2.get(i));
                }
                if (colorList3 != null) {
                    for (int i = 0; i < Math.min(2, colorList3.size()); ++i)
                        finalColorList.add(colorList3.get(i));
                }
            }
            if (finalColorList != null) {
                if (finalColorList.size() > 0) {
                    Arrays.fill(colors, 0xFF000000 | finalColorList.get(0));
                }
                for (int i = 0; i < Math.min(colors.length, finalColorList.size()); ++i) {
                    colors[i] = 0xFF000000 | finalColorList.get(i);
                }
            }
            return colors;
        }

        public static PeerColor fromString(String string) {
            if (string == null || string.isEmpty() || string.charAt(0) != '#')
                return null;
            int startIndex = 1;
            boolean hidden = string.length() > 1 && string.charAt(startIndex) == 'H';
            if (hidden) {
                startIndex++;
            }
            int channelLvl = 0;
            int groupLvl = 0;
            if (string.length() > startIndex && string.charAt(startIndex) == '[') {
                int eindex = string.indexOf(']');
                if (eindex > startIndex) {
                    String subStr = string.substring(startIndex + 1, eindex);
                    if (subStr.contains(",")) {
                        String[] splits = subStr.split(",");
                        channelLvl = Utilities.parseInt(splits[0]);
                        groupLvl = Utilities.parseInt(splits[1]);
                    } else {
                        channelLvl = Utilities.parseInt(subStr);
                    }
                    startIndex = eindex + 1;
                }
            }
            int index = string.indexOf('{');
            if (index < 0) return null;
            try {
                final PeerColor peerColor = new PeerColor();
                peerColor.id = Utilities.parseInt(string.substring(startIndex, index));
                peerColor.hidden = hidden;
                peerColor.channelLvl = channelLvl;
                peerColor.groupLvl = groupLvl;
                final String[] parts = string.substring(index + 1, string.length() - 1).split("@");
                String[] colorsString = parts[0].split(",");
                for (int i = 0; i < 6; ++i)
                    peerColor.colors[i] = colorsString.length >= i + 1 ? Utilities.parseInt(colorsString[i]) : peerColor.colors[0];
                if (parts.length >= 2) {
                    colorsString = parts[1].split(",");
                    for (int i = 0; i < 6; ++i)
                        peerColor.darkColors[i] = colorsString.length >= i + 1 ? Utilities.parseInt(colorsString[i]) : peerColor.darkColors[0];
                } else {
                    for (int i = 0; i < 6; ++i)
                        peerColor.darkColors[i] = peerColor.colors[i];
                }
                return peerColor;
            } catch (Exception e) {
                FileLog.e(e);
            }
            return null;
        }
    }

    private void resetAppConfig() {
        getfileExperimentalParams = false;
        channelRevenueWithdrawalEnabled = false;
        collectDeviceStats = false;
        smsjobsStickyNotificationEnabled = false;
        showAnnualPerMonth = false;
        canEditFactcheck = false;
        starsLocked = true;
        factcheckLengthLimit = 1024;
        videoIgnoreAltDocuments = false;
        freezeSinceDate = 0L;
        freezeUntilDate = 0L;
        freezeAppealUrl = "t.me/spambot";
        verifyAgeBotUsername = null;
        verifyAgeCountry = "GB";
        ignoreRestrictionReasons = new HashSet<String>();
        mainPreferences.edit()
            .remove("starsLocked")
            .remove("getfileExperimentalParams")
            .remove("smsjobsStickyNotificationEnabled")
            .remove("channelRevenueWithdrawalEnabled")
            .remove("showAnnualPerMonth")
            .remove("canEditFactcheck")
            .remove("factcheckLengthLimit")
            .remove("videoIgnoreAltDocuments")
            .remove("freezeSinceDate")
            .remove("freezeUntilDate")
            .remove("freezeAppealUrl")
            .remove("verifyAgeBotUsername")
            .remove("verifyAgeCountry")
            .remove("ignoreRestrictionReasons")
            .apply();
    }

    private boolean savePremiumFeaturesPreviewOrder(String key, SparseIntArray array, SharedPreferences.Editor editor, ArrayList<TLRPC.JSONValue> value) {
        StringBuilder stringBuilder = new StringBuilder();
        array.clear();
        for (int i = 0; i < value.size(); i++) {
            String s = null;
            if (value.get(i) instanceof TLRPC.TL_jsonString) {
                s = ((TLRPC.TL_jsonString) value.get(i)).value;
            }
            if (s != null) {
                int type = PremiumPreviewFragment.serverStringToFeatureType(s);
                if (type >= 0) {
                    array.put(type, i);
                    if (stringBuilder.length() > 0) {
                        stringBuilder.append('_');
                    }
                    stringBuilder.append(type);
                }
            }
        }

        boolean changed;
        if (stringBuilder.length() > 0) {
            String string = stringBuilder.toString();
            changed = !string.equals(mainPreferences.getString(key, null));
            editor.putString(key, string);
        } else {
            editor.remove(key);
            changed = mainPreferences.getString(key, null) != null;
        }
        return changed;
    }

    private void loadPremiumFeaturesPreviewOrder(SparseIntArray array, String string) {
        array.clear();
        if (string != null) {
            String[] types = string.split("_");
            for (int i = 0; i < types.length; i++) {
                int type = Integer.parseInt(types[i]);
                array.put(type, i);
            }
        }
    }

    public void removeSuggestion(long did, String suggestion) {
        if (TextUtils.isEmpty(suggestion)) {
            return;
        }
        if (did == 0) {
            if (customPendingSuggestion != null && TextUtils.equals(suggestion, customPendingSuggestion.suggestion)) {
                customPendingSuggestion = null;
                getNotificationCenter().postNotificationName(NotificationCenter.newSuggestionsAvailable);
            } else if (pendingSuggestions.remove(suggestion) || !dismissedSuggestions.contains(suggestion)) {
                dismissedSuggestions.add(suggestion);
                final SharedPreferences.Editor editor = mainPreferences.edit();
                editor.putStringSet("pendingSuggestions", pendingSuggestions);
                editor.putStringSet("dismissedSuggestions", dismissedSuggestions);
                editor.commit();
                getNotificationCenter().postNotificationName(NotificationCenter.newSuggestionsAvailable);
            } else {
                return;
            }
        }
        TLRPC.TL_help_dismissSuggestion req = new TLRPC.TL_help_dismissSuggestion();
        req.suggestion = suggestion;
        if (did == 0) {
            req.peer = new TLRPC.TL_inputPeerEmpty();
        } else {
            req.peer = getInputPeer(did);
        }
        getConnectionsManager().sendRequest(req, (response, error) -> {

        });
    }

    public void updateConfig(final TLRPC.TL_config config) {
        AndroidUtilities.runOnUIThread(() -> {
            getDownloadController().loadAutoDownloadConfig(false);
            loadAppConfig(true);
            checkPeerColors(true);
            remoteConfigLoaded = true;
            maxMegagroupCount = config.megagroup_size_max;
            maxGroupCount = config.chat_size_max;
            maxEditTime = config.edit_time_limit;
            ratingDecay = config.rating_e_decay;
//            maxRecentGifsCount = config.saved_gifs_limit;
            maxRecentStickersCount = config.stickers_recent_limit;
//            maxFaveStickersCount = config.stickers_faved_limit;
            revokeTimeLimit = config.revoke_time_limit;
            revokeTimePmLimit = config.revoke_pm_time_limit;
            canRevokePmInbox = config.revoke_pm_inbox;
            linkPrefix = config.me_url_prefix;
            boolean forceTryIpV6 = config.force_try_ipv6;
            if (linkPrefix.endsWith("/")) {
                linkPrefix = linkPrefix.substring(0, linkPrefix.length() - 1);
            }
            if (linkPrefix.startsWith("https://")) {
                linkPrefix = linkPrefix.substring(8);
            } else if (linkPrefix.startsWith("http://")) {
                linkPrefix = linkPrefix.substring(7);
            }
            callReceiveTimeout = config.call_receive_timeout_ms;
            callRingTimeout = config.call_ring_timeout_ms;
            callConnectTimeout = config.call_connect_timeout_ms;
            callPacketTimeout = config.call_packet_timeout_ms;
//            maxPinnedDialogsCount = config.pinned_dialogs_count_max;
//            maxFolderPinnedDialogsCount = config.pinned_infolder_count_max;
            maxMessageLength = config.message_length_max;
            maxCaptionLength = config.caption_length_max;
            preloadFeaturedStickers = config.preload_featured_stickers;
            if (config.venue_search_username != null) {
                venueSearchBot = config.venue_search_username;
            }
            if (config.gif_search_username != null) {
                gifSearchBot = config.gif_search_username;
            }
            if (imageSearchBot != null) {
                imageSearchBot = config.img_search_username;
            }
            blockedCountry = config.blocked_mode;
            dcDomainName = config.dc_txt_domain_name;
            webFileDatacenterId = config.webfile_dc_id;
            if (config.suggested_lang_code != null) {
                boolean loadRemote = suggestedLangCode == null || !suggestedLangCode.equals(config.suggested_lang_code);
                suggestedLangCode = config.suggested_lang_code;
                if (loadRemote) {
                    LocaleController.getInstance().loadRemoteLanguages(currentAccount);
                }
            }
            Theme.loadRemoteThemes(currentAccount, false);
            Theme.checkCurrentRemoteTheme(false);

            if (config.static_maps_provider == null) {
                config.static_maps_provider = "telegram";
            }

            mapKey = null;
            mapProvider = 2;
            availableMapProviders = 0;
            FileLog.d("map providers = " + config.static_maps_provider);
            String[] providers = config.static_maps_provider.split(",");
            for (int a = 0; a < providers.length; a++) {
                String[] mapArgs = providers[a].split("\\+");
                if (mapArgs.length > 0) {
                    String[] typeAndKey = mapArgs[0].split(":");
                    if (typeAndKey.length > 0) {
                        if ("yandex".equals(typeAndKey[0])) {
                            if (a == 0) {
                                if (mapArgs.length > 1) {
                                    mapProvider = 3;
                                } else {
                                    mapProvider = 1;
                                }
                            }
                            availableMapProviders |= 4;
                        } else if ("google".equals(typeAndKey[0])) {
                            if (a == 0) {
                                if (mapArgs.length > 1) {
                                    mapProvider = 4;
                                }
                            }
                            availableMapProviders |= 1;
                        } else if ("telegram".equals(typeAndKey[0])) {
                            if (a == 0) {
                                mapProvider = 2;
                            }
                            availableMapProviders |= 2;
                        }
                        if (typeAndKey.length > 1) {
                            mapKey = typeAndKey[1];
                        }
                    }
                }
            }

            SharedPreferences.Editor editor = mainPreferences.edit();
            editor.putBoolean("remoteConfigLoaded", remoteConfigLoaded);
            editor.putInt("maxGroupCount", maxGroupCount);
            editor.putInt("maxMegagroupCount", maxMegagroupCount);
            editor.putInt("maxEditTime", maxEditTime);
            editor.putInt("ratingDecay", ratingDecay);
            editor.putInt("maxRecentGifsCount", maxRecentGifsCount);
            editor.putInt("maxRecentStickersCount", maxRecentStickersCount);
            editor.putInt("maxFaveStickersCount", maxFaveStickersCount);
            editor.putInt("callReceiveTimeout", callReceiveTimeout);
            editor.putInt("callRingTimeout", callRingTimeout);
            editor.putInt("callConnectTimeout", callConnectTimeout);
            editor.putInt("callPacketTimeout", callPacketTimeout);
            editor.putString("linkPrefix", linkPrefix);
//            editor.putInt("maxPinnedDialogsCount", maxPinnedDialogsCount);
            editor.putInt("maxFolderPinnedDialogsCountDefault", maxFolderPinnedDialogsCountDefault);
            editor.putInt("maxFolderPinnedDialogsCountPremium", maxFolderPinnedDialogsCountPremium);
            editor.putInt("maxMessageLength", maxMessageLength);
            editor.putInt("maxCaptionLength", maxCaptionLength);
            editor.putBoolean("preloadFeaturedStickers", preloadFeaturedStickers);
            editor.putInt("revokeTimeLimit", revokeTimeLimit);
            editor.putInt("revokeTimePmLimit", revokeTimePmLimit);
            editor.putInt("mapProvider", mapProvider);
            if (mapKey != null) {
                editor.putString("pk", mapKey);
            } else {
                editor.remove("pk");
            }
            editor.putBoolean("canRevokePmInbox", canRevokePmInbox);
            editor.putBoolean("blockedCountry", blockedCountry);
            editor.putString("venueSearchBot", venueSearchBot);
            editor.putString("gifSearchBot", gifSearchBot);
            editor.putString("imageSearchBot", imageSearchBot);
            editor.putString("dcDomainName2", dcDomainName);
            editor.putInt("webFileDatacenterId", webFileDatacenterId);
            editor.putString("suggestedLangCode", suggestedLangCode);
            editor.putBoolean("forceTryIpV6", forceTryIpV6);
            editor.putString("autologinToken", autologinToken = config.autologin_token);
            editor.commit();

            getConnectionsManager().setForceTryIpV6(forceTryIpV6);
            LocaleController.getInstance().checkUpdateForCurrentRemoteLocale(currentAccount, config.lang_pack_version, config.base_lang_pack_version);
            getNotificationCenter().postNotificationName(NotificationCenter.configLoaded);
        });
    }

    public void addSupportUser() {
        TLRPC.TL_userForeign_old2 user = new TLRPC.TL_userForeign_old2();
        user.phone = "333";
        user.id = 333000;
        user.first_name = "Telegram";
        user.last_name = "";
        user.status = null;
        user.photo = new TLRPC.TL_userProfilePhotoEmpty();
        putUser(user, true);

        user = new TLRPC.TL_userForeign_old2();
        user.phone = "42777";
        user.id = 777000;
        user.verified = true;
        user.first_name = "Telegram";
        user.last_name = "Notifications";
        user.status = null;
        user.photo = new TLRPC.TL_userProfilePhotoEmpty();
        putUser(user, true);
    }

    public TLRPC.InputUser getInputUser(TLRPC.User user) {
        if (user == null) {
            return new TLRPC.TL_inputUserEmpty();
        }
        TLRPC.InputUser inputUser;
        if (user.id == getUserConfig().getClientUserId()) {
            inputUser = new TLRPC.TL_inputUserSelf();
        } else if (user.access_hash == 0 && user.fromMessageDialogId != 0 && user.fromMessageId != 0) {
            inputUser = new TLRPC.TL_inputUserFromMessage();
            inputUser.user_id = user.id;
            ((TLRPC.TL_inputUserFromMessage) inputUser).peer = getInputPeer(user.fromMessageDialogId);
            ((TLRPC.TL_inputUserFromMessage) inputUser).msg_id = user.fromMessageId;
        } else {
            inputUser = new TLRPC.TL_inputUser();
            inputUser.user_id = user.id;
            inputUser.access_hash = user.access_hash;
        }
        return inputUser;
    }

    public TLRPC.InputUser getInputUser(TLRPC.InputPeer peer) {
        if (peer == null) {
            return new TLRPC.TL_inputUserEmpty();
        }
        if (peer instanceof TLRPC.TL_inputPeerSelf) {
            return new TLRPC.TL_inputUserSelf();
        }
        if (peer.access_hash == 0) {
            final TLRPC.User user = getUser(peer.user_id);
            if (user.access_hash == 0 && user.fromMessageDialogId != 0 && user.fromMessageId != 0) {
                final TLRPC.TL_inputUserFromMessage inputUser = new TLRPC.TL_inputUserFromMessage();
                inputUser.user_id = peer.user_id;
                inputUser.peer = getInputPeer(user.fromMessageDialogId);
                inputUser.msg_id = user.fromMessageId;
                return inputUser;
            }
        }
        final TLRPC.TL_inputUser inputUser = new TLRPC.TL_inputUser();
        inputUser.user_id = peer.user_id;
        inputUser.access_hash = peer.access_hash;
        return inputUser;
    }

    public TLRPC.InputUser getInputUser(long userId) {
        return getInputUser(getUser(userId));
    }

    public static TLRPC.InputChannel getInputChannel(TLRPC.Chat chat) {
        if (ChatObject.isChannel(chat)) {
            if (chat.access_hash == 0 && chat.fromMessageDialogId != 0 && chat.fromMessageId != 0) {
                final TLRPC.TL_inputChannelFromMessage inputChat = new TLRPC.TL_inputChannelFromMessage();
                inputChat.channel_id = chat.id;
                inputChat.peer = MessagesController.getInstance(UserConfig.selectedAccount).getInputPeer(chat.fromMessageDialogId);
                inputChat.msg_id = chat.fromMessageId;
                return inputChat;
            } else {
                final TLRPC.InputChannel inputChat = new TLRPC.TL_inputChannel();
                inputChat.channel_id = chat.id;
                inputChat.access_hash = chat.access_hash;
                return inputChat;
            }
        } else {
            return new TLRPC.TL_inputChannelEmpty();
        }
    }

    public static TLRPC.InputChannel getInputChannel(TLRPC.InputPeer peer) {
        if (peer.access_hash == 0) {
            final TLRPC.Chat chat = MessagesController.getInstance(UserConfig.selectedAccount).getChat(peer.channel_id);
            if (chat != null && chat.access_hash == 0 && chat.fromMessageId != 0 && chat.fromMessageDialogId != 0) {
                final TLRPC.TL_inputChannelFromMessage inputChat = new TLRPC.TL_inputChannelFromMessage();
                inputChat.channel_id = peer.channel_id;
                inputChat.peer = MessagesController.getInstance(UserConfig.selectedAccount).getInputPeer(chat.fromMessageDialogId);
                inputChat.msg_id = chat.fromMessageId;
                return inputChat;
            }
        }
        final TLRPC.TL_inputChannel inputChat = new TLRPC.TL_inputChannel();
        inputChat.channel_id = peer.channel_id;
        inputChat.access_hash = peer.access_hash;
        return inputChat;
    }

    public TLRPC.InputChannel getInputChannel(long chatId) {
        return getInputChannel(getChat(chatId));
    }

    public TLRPC.InputPeer getInputPeer(TLRPC.Peer peer) {
        TLRPC.InputPeer inputPeer;
        if (peer instanceof TLRPC.TL_peerChat) {
            inputPeer = new TLRPC.TL_inputPeerChat();
            inputPeer.chat_id = peer.chat_id;
        } else if (peer instanceof TLRPC.TL_peerChannel) {
            final TLRPC.Chat chat = getChat(peer.channel_id);
            if (chat != null && chat.access_hash == 0 && chat.fromMessageDialogId != 0 && chat.fromMessageDialogId != peer.channel_id && chat.fromMessageId != 0) {
                inputPeer = new TLRPC.TL_inputPeerChannelFromMessage();
                inputPeer.channel_id = peer.channel_id;
                inputPeer.peer = getInputPeer(chat.fromMessageDialogId);
                inputPeer.msg_id = chat.fromMessageId;
            } else {
                inputPeer = new TLRPC.TL_inputPeerChannel();
                inputPeer.channel_id = peer.channel_id;
                if (chat != null) {
                    inputPeer.access_hash = chat.access_hash;
                }
            }
        } else {
            final TLRPC.User user = getUser(peer.user_id);
            if (user != null && user.access_hash == 0 && user.fromMessageDialogId != 0 && user.fromMessageId != 0) {
                inputPeer = new TLRPC.TL_inputPeerUserFromMessage();
                inputPeer.user_id = peer.user_id;
                inputPeer.peer = getInputPeer(user.fromMessageDialogId);
                inputPeer.msg_id = user.fromMessageId;
            } else {
                inputPeer = new TLRPC.TL_inputPeerUser();
                inputPeer.user_id = peer.user_id;
                if (user != null) {
                    inputPeer.access_hash = user.access_hash;
                }
            }
        }
        return inputPeer;
    }

    public TLRPC.InputPeer getInputPeer(long id) {
        TLRPC.InputPeer inputPeer;
        if (id == getUserConfig().getClientUserId()) {
            inputPeer = new TLRPC.TL_inputPeerSelf();
        } else if (id < 0) {
            TLRPC.Chat chat = getChat(-id);
            if (ChatObject.isChannel(chat)) {
                if (chat != null && chat.access_hash == 0 && chat.fromMessageDialogId != 0 && chat.fromMessageDialogId != id && chat.fromMessageId != 0) {
                    inputPeer = new TLRPC.TL_inputPeerChannelFromMessage();
                    inputPeer.channel_id = -id;
                    inputPeer.peer = getInputPeer(chat.fromMessageDialogId);
                    inputPeer.msg_id = chat.fromMessageId;
                } else {
                    inputPeer = new TLRPC.TL_inputPeerChannel();
                    inputPeer.channel_id = -id;
                    inputPeer.access_hash = chat.access_hash;
                }
            } else {
                inputPeer = new TLRPC.TL_inputPeerChat();
                inputPeer.chat_id = -id;
            }
        } else {
            TLRPC.User user = getUser(id);
            if (user != null && user.access_hash == 0 && user.fromMessageDialogId != 0 && user.fromMessageDialogId != id && user.fromMessageId != 0) {
                inputPeer = new TLRPC.TL_inputPeerUserFromMessage();
                inputPeer.user_id = id;
                inputPeer.peer = getInputPeer(user.fromMessageDialogId);
                inputPeer.msg_id = user.fromMessageId;
            } else {
                inputPeer = new TLRPC.TL_inputPeerUser();
                inputPeer.user_id = id;
                if (user != null) {
                    inputPeer.access_hash = user.access_hash;
                }
            }
        }
        return inputPeer;
    }

    public static TLRPC.InputPeer getInputPeer(TLRPC.Chat chat) {
        TLRPC.InputPeer inputPeer;
        if (ChatObject.isChannel(chat)) {
            if (chat != null && chat.access_hash == 0 && chat.fromMessageDialogId != 0 && chat.fromMessageId != 0) {
                inputPeer = new TLRPC.TL_inputPeerChannelFromMessage();
                inputPeer.channel_id = chat.id;
                inputPeer.peer = MessagesController.getInstance(UserConfig.selectedAccount).getInputPeer(chat.fromMessageDialogId);
                inputPeer.msg_id = chat.fromMessageId;
            } else {
                inputPeer = new TLRPC.TL_inputPeerChannel();
                inputPeer.channel_id = chat.id;
                inputPeer.access_hash = chat.access_hash;
            }
        } else {
            inputPeer = new TLRPC.TL_inputPeerChat();
            inputPeer.chat_id = chat.id;
        }
        return inputPeer;
    }

    public static TLRPC.InputPeer getInputPeer(TLRPC.User user) {
        TLRPC.InputPeer inputPeer;
        if (user != null && user.access_hash == 0 && user.fromMessageDialogId != 0 && user.fromMessageId != 0) {
            inputPeer = new TLRPC.TL_inputPeerUserFromMessage();
            inputPeer.user_id = user.id;
            inputPeer.peer = MessagesController.getInstance(UserConfig.selectedAccount).getInputPeer(user.fromMessageDialogId);
            inputPeer.msg_id = user.fromMessageId;
        } else {
            inputPeer = new TLRPC.TL_inputPeerUser();
            inputPeer.user_id = user.id;
            inputPeer.access_hash = user.access_hash;
        }
        return inputPeer;
    }

    public static TLRPC.InputPeer getInputPeer(TLObject userOrChat) {
        if (userOrChat instanceof TLRPC.User) {
            return getInputPeer((TLRPC.User) userOrChat);
        } else if (userOrChat instanceof TLRPC.Chat) {
            return getInputPeer((TLRPC.Chat) userOrChat);
        } else {
            return null;
        }
    }

    public TLRPC.Peer getPeer(long id) {
        TLRPC.Peer inputPeer;
        if (id < 0) {
            TLRPC.Chat chat = getChat(-id);
            if (ChatObject.isChannel(chat)) {
                inputPeer = new TLRPC.TL_peerChannel();
                inputPeer.channel_id = -id;
            } else {
                inputPeer = new TLRPC.TL_peerChat();
                inputPeer.chat_id = -id;
            }
        } else {
            inputPeer = new TLRPC.TL_peerUser();
            inputPeer.user_id = id;
        }
        return inputPeer;
    }

    public TLRPC.InputDocument getInputDocument(TLRPC.Document document) {
        if (document == null) return null;
        TLRPC.TL_inputDocument id = new TLRPC.TL_inputDocument();
        id.id = document.id;
        id.access_hash = document.access_hash;
        id.file_reference = document.file_reference;
        if (id.file_reference == null) {
            id.file_reference = new byte[0];
        }
        return id;
    }


    public String getPeerName(long dialogId) {
        return getPeerName(dialogId, false);
    }
    public String getPeerName(long dialogId, boolean firstName) {
        if (dialogId >= 0) {
            TLRPC.User user = getUser(dialogId);
            if (firstName) {
                return AndroidUtilities.removeRTL(AndroidUtilities.removeDiacritics(UserObject.getFirstName(user, true)));
            } else {
                return AndroidUtilities.removeRTL(AndroidUtilities.removeDiacritics(UserObject.getUserName(user)));
            }
        } else {
            TLRPC.Chat chat = getChat(-dialogId);
            return AndroidUtilities.removeRTL(AndroidUtilities.removeDiacritics(chat == null ? "" : chat.title));
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.fileUploaded) {
            String location = (String) args[0];
            TLRPC.InputFile file = (TLRPC.InputFile) args[1];

            if (uploadingAvatar != null && uploadingAvatar.equals(location)) {
                TLRPC.TL_photos_uploadProfilePhoto req = new TLRPC.TL_photos_uploadProfilePhoto();
                req.file = file;
                req.flags |= 1;
                getConnectionsManager().sendRequest(req, (response, error) -> {
                    if (error == null) {
                        TLRPC.User user = getUser(getUserConfig().getClientUserId());
                        if (user == null) {
                            user = getUserConfig().getCurrentUser();
                            putUser(user, true);
                        } else {
                            getUserConfig().setCurrentUser(user);
                        }
                        if (user == null) {
                            return;
                        }
                        TLRPC.TL_photos_photo photo = (TLRPC.TL_photos_photo) response;
                        ArrayList<TLRPC.PhotoSize> sizes = photo.photo.sizes;
                        TLRPC.PhotoSize smallSize = FileLoader.getClosestPhotoSizeWithSize(sizes, 100);
                        TLRPC.PhotoSize bigSize = FileLoader.getClosestPhotoSizeWithSize(sizes, 1000);
                        user.photo = new TLRPC.TL_userProfilePhoto();
                        user.photo.photo_id = photo.photo.id;
                        if (smallSize != null) {
                            user.photo.photo_small = smallSize.location;
                        }
                        if (bigSize != null) {
                            user.photo.photo_big = bigSize.location;
                        }
                        getDialogPhotos(user.id).reset();
                        getDialogPhotos(user.id).load(0, DialogPhotos.STEP);
                        ArrayList<TLRPC.User> users = new ArrayList<>();
                        users.add(user);
                        getMessagesStorage().putUsersAndChats(users, null, false, true);
                        AndroidUtilities.runOnUIThread(() -> {
                            getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
                            getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, UPDATE_MASK_AVATAR);
                            getUserConfig().saveConfig(true);
                        });
                    }
                });
            } else if (uploadingWallpaper != null && uploadingWallpaper.equals(location)) {
                TL_account.uploadWallPaper req = new TL_account.uploadWallPaper();
                req.file = file;
                req.mime_type = "image/jpeg";
                Theme.OverrideWallpaperInfo overrideWallpaperInfo = uploadingWallpaperInfo;
                String uploadingWallpaperFinal = uploadingWallpaper;
                TLRPC.TL_wallPaperSettings settings = new TLRPC.TL_wallPaperSettings();
                settings.blur = overrideWallpaperInfo.isBlurred;
                settings.motion = overrideWallpaperInfo.isMotion;
                req.settings = settings;
                uploadingWallpaperInfo.uploadingProgress = 1f;
                uploadingWallpaperInfo.requestIds = new ArrayList<>();
                uploadingWallpaperInfo.requestIds.add(getConnectionsManager().sendRequest(req, (response, error) -> {
                    TLRPC.WallPaper wallPaper = (TLRPC.WallPaper) response;
                    File path = new File(ApplicationLoader.getFilesDirFixed(), overrideWallpaperInfo.originalFileName);
                    if (wallPaper != null) {
                        try {
                            AndroidUtilities.copyFile(path, getFileLoader().getPathToAttach(wallPaper.document, true));
                        } catch (Exception ignore) {

                        }
                    }
                    AndroidUtilities.runOnUIThread(() -> {
                        if (uploadingWallpaper != null && uploadingWallpaperInfo.requestIds != null && wallPaper != null) {
                            wallPaper.settings = settings;
                            wallPaper.flags |= 4;
                            overrideWallpaperInfo.slug = wallPaper.slug;
                            overrideWallpaperInfo.saveOverrideWallpaper();
                            ArrayList<TLRPC.WallPaper> wallpapers = new ArrayList<>();
                            wallpapers.add(wallPaper);
                            getMessagesStorage().putWallpapers(wallpapers, 2);
                            TLRPC.PhotoSize image = FileLoader.getClosestPhotoSizeWithSize(wallPaper.document.thumbs, 320);
                            if (image != null) {
                                String newKey = image.location.volume_id + "_" + image.location.local_id + "@100_100";
                                String oldKey = Utilities.MD5(path.getAbsolutePath()) + "@100_100";
                                ImageLoader.getInstance().replaceImageInCache(oldKey, newKey, ImageLocation.getForDocument(image, wallPaper.document), false);
                            }
                            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.wallpapersNeedReload, wallPaper.slug);
                            if (uploadingWallpaperInfo.requestIds != null && overrideWallpaperInfo.dialogId != 0) {
                                uploadingWallpaperInfo.requestIds.add(ChatThemeController.getInstance(currentAccount).setWallpaperToPeer(overrideWallpaperInfo.dialogId, uploadingWallpaperFinal, overrideWallpaperInfo, null, null));
                            }
                        }
                    });
                }));
            } else {
                Object object = uploadingThemes.get(location);
                Theme.ThemeInfo themeInfo;
                Theme.ThemeAccent accent;

                TLRPC.InputFile uploadedThumb;
                TLRPC.InputFile uploadedFile;
                if (object instanceof Theme.ThemeInfo) {
                    themeInfo = (Theme.ThemeInfo) object;
                    accent = null;
                    if (location.equals(themeInfo.uploadingThumb)) {
                        themeInfo.uploadedThumb = file;
                        themeInfo.uploadingThumb = null;
                    } else if (location.equals(themeInfo.uploadingFile)) {
                        themeInfo.uploadedFile = file;
                        themeInfo.uploadingFile = null;
                    }
                    uploadedThumb = themeInfo.uploadedThumb;
                    uploadedFile = themeInfo.uploadedFile;
                } else if (object instanceof Theme.ThemeAccent) {
                    accent = (Theme.ThemeAccent) object;
                    if (location.equals(accent.uploadingThumb)) {
                        accent.uploadedThumb = file;
                        accent.uploadingThumb = null;
                    } else if (location.equals(accent.uploadingFile)) {
                        accent.uploadedFile = file;
                        accent.uploadingFile = null;
                    }
                    themeInfo = accent.parentTheme;
                    uploadedThumb = accent.uploadedThumb;
                    uploadedFile = accent.uploadedFile;
                } else {
                    themeInfo = null;
                    accent = null;
                    uploadedThumb = null;
                    uploadedFile = null;
                }
                uploadingThemes.remove(location);

                if (uploadedFile != null && uploadedThumb != null) {
                    File f = new File(location);
                    TL_account.uploadTheme req = new TL_account.uploadTheme();
                    req.mime_type = "application/x-tgtheme-android";
                    req.file_name = "theme.attheme";
                    req.file = uploadedFile;
                    req.file.name = "theme.attheme";
                    req.thumb = uploadedThumb;
                    req.thumb.name = "theme-preview.jpg";
                    req.flags |= 1;
                    TLRPC.TL_theme info;
                    TLRPC.TL_inputThemeSettings settings;
                    if (accent != null) {
                        accent.uploadedFile = null;
                        accent.uploadedThumb = null;
                        info = accent.info;
                        settings = new TLRPC.TL_inputThemeSettings();
                        settings.base_theme = Theme.getBaseThemeByKey(themeInfo.name);
                        settings.accent_color = accent.accentColor;
                        if (accent.accentColor2 != 0) {
                            settings.flags |= 8;
                            settings.outbox_accent_color = accent.accentColor2;
                        }
                        if (accent.myMessagesAccentColor != 0) {
                            settings.message_colors.add(accent.myMessagesAccentColor);
                            settings.flags |= 1;
                            if (accent.myMessagesGradientAccentColor1 != 0) {
                                settings.message_colors.add(accent.myMessagesGradientAccentColor1);
                                if (accent.myMessagesGradientAccentColor2 != 0) {
                                    settings.message_colors.add(accent.myMessagesGradientAccentColor2);
                                    if (accent.myMessagesGradientAccentColor3 != 0) {
                                        settings.message_colors.add(accent.myMessagesGradientAccentColor3);
                                    }
                                }
                            }
                            settings.message_colors_animated = accent.myMessagesAnimated;
                        }
                        settings.flags |= 2;
                        settings.wallpaper_settings = new TLRPC.TL_wallPaperSettings();
                        if (!TextUtils.isEmpty(accent.patternSlug)) {
                            TLRPC.TL_inputWallPaperSlug inputWallPaperSlug = new TLRPC.TL_inputWallPaperSlug();
                            inputWallPaperSlug.slug = accent.patternSlug;
                            settings.wallpaper = inputWallPaperSlug;
                            settings.wallpaper_settings.intensity = (int) (accent.patternIntensity * 100);
                            settings.wallpaper_settings.flags |= 8;
                        } else {
                            TLRPC.TL_inputWallPaperNoFile inputWallPaperNoFile = new TLRPC.TL_inputWallPaperNoFile();
                            inputWallPaperNoFile.id = 0;
                            settings.wallpaper = inputWallPaperNoFile;
                        }
                        settings.wallpaper_settings.motion = accent.patternMotion;
                        if (accent.backgroundOverrideColor != 0) {
                            settings.wallpaper_settings.background_color = (int) accent.backgroundOverrideColor;
                            settings.wallpaper_settings.flags |= 1;
                        }
                        if (accent.backgroundGradientOverrideColor1 != 0) {
                            settings.wallpaper_settings.second_background_color = (int) accent.backgroundGradientOverrideColor1;
                            settings.wallpaper_settings.flags |= 16;
                            settings.wallpaper_settings.rotation = AndroidUtilities.getWallpaperRotation(accent.backgroundRotation, true);
                        }
                        if (accent.backgroundGradientOverrideColor2 != 0) {
                            settings.wallpaper_settings.third_background_color = (int) accent.backgroundGradientOverrideColor2;
                            settings.wallpaper_settings.flags |= 32;
                        }
                        if (accent.backgroundGradientOverrideColor3 != 0) {
                            settings.wallpaper_settings.fourth_background_color = (int) accent.backgroundGradientOverrideColor3;
                            settings.wallpaper_settings.flags |= 64;
                        }
                    } else {
                        themeInfo.uploadedFile = null;
                        themeInfo.uploadedThumb = null;
                        info = themeInfo.info;
                        settings = null;
                    }
                    getConnectionsManager().sendRequest(req, (response, error) -> {
                        String title = info != null ? info.title : themeInfo.getName();
                        int index = title.lastIndexOf(".attheme");
                        String n = index > 0 ? title.substring(0, index) : title;
                        if (response != null) {
                            TLRPC.Document document = (TLRPC.Document) response;
                            TLRPC.TL_inputDocument inputDocument = new TLRPC.TL_inputDocument();
                            inputDocument.access_hash = document.access_hash;
                            inputDocument.id = document.id;
                            inputDocument.file_reference = document.file_reference;
                            if (info == null || !info.creator) {
                                TL_account.createTheme req2 = new TL_account.createTheme();
                                req2.document = inputDocument;
                                req2.flags |= 4;
                                req2.slug = info != null && !TextUtils.isEmpty(info.slug) ? info.slug : "";
                                req2.title = n;
                                if (settings != null) {
                                    req2.settings = settings;
                                    req2.flags |= 8;
                                }
                                getConnectionsManager().sendRequest(req2, (response1, error1) -> AndroidUtilities.runOnUIThread(() -> {
                                    if (response1 instanceof TLRPC.TL_theme) {
                                        Theme.setThemeUploadInfo(themeInfo, accent, (TLRPC.TL_theme) response1, currentAccount, false);
                                        installTheme(themeInfo, accent, themeInfo == Theme.getCurrentNightTheme());
                                        getNotificationCenter().postNotificationName(NotificationCenter.themeUploadedToServer, themeInfo, accent);
                                    } else {
                                        getNotificationCenter().postNotificationName(NotificationCenter.themeUploadError, themeInfo, accent);
                                    }
                                }));
                            } else {
                                TL_account.updateTheme req2 = new TL_account.updateTheme();
                                TLRPC.TL_inputTheme inputTheme = new TLRPC.TL_inputTheme();
                                inputTheme.id = info.id;
                                inputTheme.access_hash = info.access_hash;
                                req2.theme = inputTheme;

                                req2.slug = info.slug;
                                req2.flags |= 1;

                                req2.title = n;
                                req2.flags |= 2;

                                req2.document = inputDocument;
                                req2.flags |= 4;

                                if (settings != null) {
                                    req2.settings = settings;
                                    req2.flags |= 8;
                                }

                                req2.format = "android";
                                getConnectionsManager().sendRequest(req2, (response1, error1) -> AndroidUtilities.runOnUIThread(() -> {
                                    if (response1 instanceof TLRPC.TL_theme) {
                                        Theme.setThemeUploadInfo(themeInfo, accent, (TLRPC.TL_theme) response1, currentAccount, false);
                                        getNotificationCenter().postNotificationName(NotificationCenter.themeUploadedToServer, themeInfo, accent);
                                    } else {
                                        getNotificationCenter().postNotificationName(NotificationCenter.themeUploadError, themeInfo, accent);
                                    }
                                }));
                            }
                        } else {
                            AndroidUtilities.runOnUIThread(() -> getNotificationCenter().postNotificationName(NotificationCenter.themeUploadError, themeInfo, accent));
                        }
                    });
                }
            }
        } else if (id == NotificationCenter.fileUploadFailed) {
            String location = (String) args[0];
            if (uploadingAvatar != null && uploadingAvatar.equals(location)) {
                uploadingAvatar = null;
            } else if (uploadingWallpaper != null && uploadingWallpaper.equals(location)) {
                uploadingWallpaper = null;
                uploadingWallpaperInfo = null;
            } else {
                Object object = uploadingThemes.remove(location);
                if (object instanceof Theme.ThemeInfo) {
                    Theme.ThemeInfo themeInfo = (Theme.ThemeInfo) object;
                    themeInfo.uploadedFile = null;
                    themeInfo.uploadedThumb = null;
                    getNotificationCenter().postNotificationName(NotificationCenter.themeUploadError, themeInfo, null);
                } else if (object instanceof Theme.ThemeAccent) {
                    Theme.ThemeAccent accent = (Theme.ThemeAccent) object;
                    accent.uploadingThumb = null;
                    getNotificationCenter().postNotificationName(NotificationCenter.themeUploadError, accent.parentTheme, accent);
                }
            }
        } if (id == NotificationCenter.fileUploadProgressChanged) {
            String location = (String) args[0];
            if (uploadingWallpaper != null && uploadingWallpaper.equals(location)) {
                Long loadedSize = (Long) args[1];
                Long totalSize = (Long) args[2];
                uploadingWallpaperInfo.uploadingProgress = loadedSize / (float) totalSize;
            }
        } else if (id == NotificationCenter.messageReceivedByServer) {
            Boolean scheduled = (Boolean) args[6];
            if (scheduled) {
                return;
            }
            Integer msgId = (Integer) args[0];
            Integer newMsgId = (Integer) args[1];
            Long did = (Long) args[3];
            ArrayList<MessageObject> dialogMessages = dialogMessage.get(did);
            for (int i = 0; dialogMessages != null && i < dialogMessages.size(); ++i) {
                MessageObject obj = dialogMessages.get(i);
                if (obj != null && (obj.getId() == msgId || obj.messageOwner.local_id == msgId)) {
                    obj.messageOwner.id = newMsgId;
                    obj.messageOwner.send_state = MessageObject.MESSAGE_SEND_STATE_SENT;
                }
                obj = dialogMessagesByIds.get(msgId);
                if (obj != null) {
                    dialogMessagesByIds.remove(msgId);
                    dialogMessagesByIds.put(newMsgId, obj);
                }
            }
            TLRPC.Dialog dialog = dialogs_dict.get(did);
            if (dialog != null && dialog.top_message == msgId) {
                dialog.top_message = newMsgId;
                getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
            }
            if (DialogObject.isChatDialog(did)) {
                TLRPC.ChatFull chatFull = fullChats.get(-did);
                TLRPC.Chat chat = getChat(-did);
                if (chat != null && !ChatObject.hasAdminRights(chat) && !MessageObject.isEphemeralMessageId(newMsgId) && chatFull != null && chatFull.slowmode_seconds != 0) {
                    chatFull.slowmode_next_send_date = getConnectionsManager().getCurrentTime() + chatFull.slowmode_seconds;
                    chatFull.flags |= 262144;
                    getMessagesStorage().updateChatInfo(chatFull, false);
                }
            }
        } else if (id == NotificationCenter.updateMessageMedia) {
            TLRPC.Message message = (TLRPC.Message) args[0];
            if (message.peer_id.channel_id == 0) {
                MessageObject existMessageObject = dialogMessagesByIds.get(message.id);
                if (existMessageObject != null) {
                    existMessageObject.messageOwner.media = MessageObject.getMedia(message);
                    if (MessageObject.getMedia(message).ttl_seconds != 0 && (MessageObject.getMedia(message).photo instanceof TLRPC.TL_photoEmpty || MessageObject.getMedia(message).document instanceof TLRPC.TL_documentEmpty)) {
                        existMessageObject.setType();
                        getNotificationCenter().postNotificationName(NotificationCenter.notificationsSettingsUpdated);
                    }
                }
            }
        } else if (id == NotificationCenter.currentUserPremiumStatusChanged) {
            loadAppConfig(false);
            getContactsController().reloadContactsStatusesMaybe(true);
            if (storyQualityFull && !getUserConfig().isPremium() || getUserConfig().isPremium()) {
                getNotificationCenter().postNotificationName(NotificationCenter.storyQualityUpdate);
            }
        }
    }

    public void cleanup() {
        getContactsController().cleanup();
        MediaController.getInstance().cleanup();
        getNotificationsController().cleanup();
        getSendMessagesHelper().cleanup();
        getSecretChatHelper().cleanup();
        getLocationController().cleanup();
        getMediaDataController().cleanup();
        getColorPalette().cleanup();
        getTranslateController().cleanup();
        getSavedMessagesController().cleanup();
        if (storiesController != null) {
            storiesController.cleanup();
        }
        if (unconfirmedAuthController != null) {
            unconfirmedAuthController.cleanup();
        }

        showFiltersTooltip = false;

        DialogsActivity.dialogsLoaded[currentAccount] = false;

        SharedPreferences.Editor editor = notificationsPreferences.edit();
        editor.clear().commit();
        editor = emojiPreferences.edit();
        editor.putLong("lastGifLoadTime", 0).putLong("lastStickersLoadTime", 0).putLong("lastStickersLoadTimeMask", 0).putLong("lastStickersLoadTimeFavs", 0).commit();
        editor = mainPreferences.edit();
        editor.remove("archivehint").remove("proximityhint").remove("archivehint_l").remove("gifhint").remove("reminderhint").remove("soundHint").remove("dcDomainName2").remove("webFileDatacenterId").remove("themehint").remove("showFiltersTooltip").remove("transcribeButtonPressed").commit();

        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("shortcut_widget", Activity.MODE_PRIVATE);
        SharedPreferences.Editor widgetEditor = null;
        AppWidgetManager appWidgetManager = null;
        ArrayList<Integer> chatsWidgets = null;
        ArrayList<Integer> contactsWidgets = null;
        Map<String, ?> values = preferences.getAll();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("account")) {
                Integer value = (Integer) entry.getValue();
                if (value == currentAccount) {
                    int widgetId = Utilities.parseInt(key);
                    if (widgetEditor == null) {
                        widgetEditor = preferences.edit();
                        appWidgetManager = AppWidgetManager.getInstance(ApplicationLoader.applicationContext);
                    }
                    widgetEditor.putBoolean("deleted" + widgetId, true);
                    if (preferences.getInt("type" + widgetId, 0) == EditWidgetActivity.TYPE_CHATS) {
                        if (chatsWidgets == null) {
                            chatsWidgets = new ArrayList<>();
                        }
                        chatsWidgets.add(widgetId);
                    } else {
                        if (contactsWidgets == null) {
                            contactsWidgets = new ArrayList<>();
                        }
                        contactsWidgets.add(widgetId);
                    }
                }
            }
        }
        if (widgetEditor != null) {
            widgetEditor.commit();
        }
        if (chatsWidgets != null) {
            for (int a = 0, N = chatsWidgets.size(); a < N; a++) {
                ChatsWidgetProvider.updateWidget(ApplicationLoader.applicationContext, appWidgetManager, chatsWidgets.get(a));
            }
        }
        if (contactsWidgets != null) {
            for (int a = 0, N = contactsWidgets.size(); a < N; a++) {
                ContactsWidgetProvider.updateWidget(ApplicationLoader.applicationContext, appWidgetManager, contactsWidgets.get(a));
            }
        }

        lastScheduledServerQueryTime.clear();
        lastQuickReplyServerQueryTime.clear();
        lastWelcomeMessagesServerQueryTime.clear();
        lastSavedServerQueryTime.clear();
        lastServerQueryTime.clear();
        reloadingWebpages.clear();
        reloadingWebpagesPending.clear();
        reloadingScheduledWebpages.clear();
        reloadingScheduledWebpagesPending.clear();
        reloadingSavedWebpages.clear();
        reloadingSavedWebpagesPending.clear();
        sponsoredMessages.clear();
        sendAsPeers.clear();
        sendAsPeersLiveStories.clear();
        dialogs_dict.clear();
        dialogs_read_inbox_max.clear();
        loadingPinnedDialogs.clear();
        dialogs_read_outbox_max.clear();
        exportedChats.clear();
        fullUsers.clear();
        fullChats.clear();
        activeVoiceChatsMap.clear();
        loadingGroupCalls.clear();
        groupCallsByChatId.clear();
        dialogsByFolder.clear();
        dialogsByCommunity.clear();
        dialogsCommunityUnreadCount.clear();
        dialogsCommunityLastMessageDate.clear();
        dialogsCommunityFoundCommunities.clear();
        dialogsCommunityUnreadMark.clear();
        unreadUnmutedDialogs = 0;
        joiningToChannels.clear();
        migratedChats.clear();
        channelViewsToSend.clear();
        pollsToCheck.clear();
        pollsToCheckSize = 0;
        dialogsServerOnly.clear();
        dialogsForward.clear();
        allDialogs.clear();
        dialogsLoadedTillDate = Integer.MAX_VALUE;
        dialogsCanAddUsers.clear();
        dialogsMyChannels.clear();
        dialogsMyGroups.clear();
        dialogsChannelsOnly.clear();
        dialogsGroupsOnly.clear();
        dialogsUsersOnly.clear();
        dialogsForBlock.clear();
        dialogMessagesByIds.clear();
        dialogMessagesByRandomIds.clear();
        channelAdmins.clear();
        loadingChannelAdmins.clear();
        users.clear();
        objectsByUsernames.clear();
        chats.clear();
        dialogMessage.clear();
        deletedHistory.clear();
        printingUsers.clear();
        printingStrings.clear();
        printingStringsTypes.clear();
        onlinePrivacy.clear();
        loadingPeerSettings.clear();
        deletingDialogs.clear();
        clearingHistoryDialogs.clear();
        lastPrintingStringCount = 0;
        selectedDialogFilter[0] = selectedDialogFilter[1] = null;
        dialogFilters.clear();
        dialogFiltersById.clear();
        loadingSuggestedFilters = false;
        loadingRemoteFilters = false;
        suggestedFilters.clear();
        dialogFiltersLoaded = false;
        ignoreSetOnline = false;

        Utilities.stageQueue.postRunnable(() -> {
            readTasks.clear();
            readTasksMap.clear();
            repliesReadTasks.clear();
            threadsReadTasksMap.clear();
            updatesQueueSeq.clear();
            updatesQueuePts.clear();
            updatesQueueQts.clear();
            gettingUnknownChannels.clear();
            gettingUnknownDialogs.clear();
            updatesStartWaitTimeSeq = 0;
            updatesStartWaitTimePts = 0;
            updatesStartWaitTimeQts = 0;
            createdDialogIds.clear();
            createdScheduledDialogIds.clear();
            gettingDifference = false;
            resetDialogsPinned = null;
            resetDialogsAll = null;
        });
        createdDialogMainThreadIds.clear();
        visibleDialogMainThreadIds.clear();
        visibleScheduledDialogMainThreadIds.clear();
        blockePeers.clear();
        for (int a = 0; a < sendingTypings.length; a++) {
            if (sendingTypings[a] == null) {
                continue;
            }
            sendingTypings[a].clear();
        }
        loadingFullUsers.clear();
        loadedFullUsers.clear();
        reloadingMessages.clear();
        loadingFullChats.clear();
        loadingFullParticipants.clear();
        loadedFullParticipants.clear();
        loadedFullChats.clear();

        dialogsLoaded = false;
        nextDialogsCacheOffset.clear();
        loadingDialogs.clear();
        dialogsEndReached.clear();
        serverDialogsEndReached.clear();

        checkingTosUpdate = false;
        nextTosCheckTime = 0;
        nextPromoInfoCheckTime = 0;
        checkingPromoInfo = false;
        loadingUnreadDialogs = false;

        currentDeletingTaskTime = 0;
        currentDeletingTaskMids = null;
        currentDeletingTaskMediaMids = null;
        gettingNewDeleteTask = false;
        loadingBlockedPeers = false;
        totalBlockedCount = -1;
        blockedEndReached = false;
        firstGettingTask = false;
        updatingState = false;
        resetingDialogs = false;
        lastStatusUpdateTime = 0;
        offlineSent = false;
        registeringForPush = false;
        getDifferenceFirstSync = true;
        uploadingAvatar = null;
        uploadingWallpaper = null;
        uploadingWallpaperInfo = null;
        uploadingThemes.clear();
        gettingChatInviters.clear();
        statusRequest = 0;
        statusSettingState = 0;

        Utilities.stageQueue.postRunnable(() -> {
            FileLog.d("cleanup: isUpdating = false");
            getConnectionsManager().setIsUpdating(false);
            updatesQueueChannels.clear();
            updatesStartWaitTimeChannels.clear();
            gettingDifferenceChannels.clear();
            channelsPts.clear();
            shortPollChannels.clear();
            needShortPollChannels.clear();
            shortPollOnlines.clear();
            needShortPollOnlines.clear();
        });

        if (currentDeleteTaskRunnable != null) {
            Utilities.stageQueue.cancelRunnable(currentDeleteTaskRunnable);
            currentDeleteTaskRunnable = null;
        }

        addSupportUser();
        AndroidUtilities.runOnUIThread(() -> {
            getNotificationCenter().postNotificationName(NotificationCenter.suggestedFiltersLoaded);
            getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
        });
    }

    public boolean isChatNoForwards(TLRPC.Chat chat) {
        if (chat == null) {
            return false;
        }
        if (chat.migrated_to != null) {
            TLRPC.Chat migratedTo = getChat(chat.migrated_to.channel_id);
            if (migratedTo != null) {
                return migratedTo.noforwards;
            }
        }
        return chat.noforwards;
    }

    public boolean isChatNoForwards(long chatId) {
        return isChatNoForwards(getChat(chatId));
    }

    public boolean isPeerNoForwards(long dialogId) {
        return dialogId > 0 ? isUserNoForwards(dialogId) : isChatNoForwards(-dialogId);
    }

    public boolean isUserNoForwards(long userId) {
        return isUserNoForwards(getUserFull(userId));
    }

    public boolean isUserNoForwards(TLRPC.UserFull userFull) {
        if (userFull == null) {
            return false;
        }

        return userFull.noforwards_peer_enabled || userFull.noforwards_my_enabled;
    }

    public TLRPC.User getUser(Long id) {
        if (id == 0) {
            return UserConfig.getInstance(currentAccount).getCurrentUser();
        }
        return users.get(id);
    }

    public TLObject getUserOrChat(long dialogId) {
        if (users.containsKey(dialogId)) {
            return users.get(dialogId);
        } else if (chats.containsKey(-dialogId)) {
            return chats.get(-dialogId);
        }
        return null;
    }

    public TLObject getUserOrChat(String username) {
        if (username == null || username.length() == 0) {
            return null;
        }
        return objectsByUsernames.get(username.toLowerCase());
    }

    public TLRPC.User getUser(String username) {
        TLObject obj = getUserOrChat(username);
        if (obj instanceof TLRPC.User)
            return (TLRPC.User) obj;
        return null;
    }

    public ConcurrentHashMap<Long, TLRPC.User> getUsers() {
        return users;
    }

    public ConcurrentHashMap<Long, TLRPC.Chat> getChats() {
        return chats;
    }

    public TLRPC.Chat getChat(Long id) {
        return chats.get(id);
    }

    public TLRPC.EncryptedChat getEncryptedChat(Integer id) {
        return encryptedChats.get(id);
    }

    public TLRPC.EncryptedChat getEncryptedChatDB(int chatId, boolean created) {
        TLRPC.EncryptedChat chat = encryptedChats.get(chatId);
        if (chat == null || created && (chat instanceof TLRPC.TL_encryptedChatWaiting || chat instanceof TLRPC.TL_encryptedChatRequested)) {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            ArrayList<TLObject> result = new ArrayList<>();
            getMessagesStorage().getEncryptedChat(chatId, countDownLatch, result);
            try {
                countDownLatch.await();
            } catch (Exception e) {
                FileLog.e(e);
            }
            if (result.size() == 2) {
                chat = (TLRPC.EncryptedChat) result.get(0);
                TLRPC.User user = (TLRPC.User) result.get(1);
                putEncryptedChat(chat, false);
                putUser(user, true);
            }
        }
        return chat;
    }

    public boolean isDialogVisible(long dialogId, boolean scheduled) {
        return scheduled ? visibleScheduledDialogMainThreadIds.contains(dialogId) : visibleDialogMainThreadIds.contains(dialogId);
    }

    public void setLastVisibleDialogId(final long dialogId, boolean scheduled, boolean set) {
        ArrayList<Long> arrayList = scheduled ? visibleScheduledDialogMainThreadIds : visibleDialogMainThreadIds;
        if (set) {
            if (arrayList.contains(dialogId)) {
                return;
            }
            arrayList.add(dialogId);
        } else {
            arrayList.remove(dialogId);
        }
    }

    public void setLastCreatedDialogId(final long dialogId, boolean scheduled, boolean set) {
        if (!scheduled) {
            ArrayList<Long> arrayList = createdDialogMainThreadIds;
            if (set) {
                if (arrayList.contains(dialogId)) {
                    return;
                }
                arrayList.add(dialogId);
            } else {
                arrayList.remove(dialogId);

                SparseArray<MessageObject> array = pollsToCheck.get(dialogId);
                if (array != null) {
                    for (int a = 0, N = array.size(); a < N; a++) {
                        MessageObject object = array.valueAt(a);
                        object.pollVisibleOnScreen = false;
                    }
                }
            }
        }
        Utilities.stageQueue.postRunnable(() -> {
            ArrayList<Long> arrayList2 = scheduled ? createdScheduledDialogIds : createdDialogIds;
            if (set) {
                if (arrayList2.contains(dialogId)) {
                    return;
                }
                arrayList2.add(dialogId);
            } else {
                arrayList2.remove(dialogId);
            }
        });
    }

    public TLRPC.TL_chatInviteExported getExportedInvite(long chatId) {
        return exportedChats.get(chatId);
    }

    public boolean putUser(TLRPC.User user, boolean fromCache) {
        return putUser(user, fromCache, false);
    }

    public boolean putUser(TLRPC.User user, boolean fromCache, boolean force) {
        if (user == null) {
            return false;
        }
        fromCache = fromCache && user.id / 1000 != 333 && user.id != 777000;
        TLRPC.User oldUser = users.get(user.id);
        if (oldUser == user && !force) {
            return false;
        }
        if (oldUser != null && !TextUtils.isEmpty(oldUser.username)) {
            objectsByUsernames.remove(oldUser.username.toLowerCase());
        }
        if (oldUser != null && oldUser.usernames != null) {
            for (int i = 0; i < oldUser.usernames.size(); ++i) {
                TLRPC.TL_username u = oldUser.usernames.get(i);
                if (u != null && u.username != null) {
                    objectsByUsernames.remove(u.username.toLowerCase());
                }
            }
        }
        if (!TextUtils.isEmpty(user.username)) {
            objectsByUsernames.put(user.username.toLowerCase(), user);
        }
        if (user != null && user.usernames != null) {
            for (int i = 0; i < user.usernames.size(); ++i) {
                TLRPC.TL_username u = user.usernames.get(i);
                if (u != null && u.username != null && u.active) {
                    objectsByUsernames.put(u.username.toLowerCase(), user);
                }
            }
        }
        updateEmojiStatusUntilUpdate(user.id, user.emoji_status);
        if (oldUser != null && oldUser.access_hash == 0 && user.fromMessageDialogId != 0 && user.fromMessageId != 0) {
            oldUser.fromMessageDialogId = user.fromMessageDialogId;
            oldUser.fromMessageId = user.fromMessageId;
        }
        if (user.min) {
            if (oldUser != null) {
                if (!fromCache) {
                    getUserNameResolver().update(oldUser, user);
                    if (user.bot) {
                        if (user.username != null) {
                            oldUser.username = user.username;
                            oldUser.flags |= 8;
                        } else {
                            oldUser.flags = oldUser.flags & ~8;
                            oldUser.username = null;
                        }
                    }
                    if (user.apply_min_photo) {
                        if (user.photo != null) {
                            oldUser.photo = user.photo;
                            oldUser.flags |= 32;
                        } else {
                            oldUser.flags = oldUser.flags & ~32;
                            oldUser.photo = null;
                        }
                    }
                }
            } else {
                users.put(user.id, user);
            }
        } else {
            if (!fromCache) {
                users.put(user.id, user);
                if (user.id == getUserConfig().getClientUserId()) {
                    getUserConfig().setCurrentUser(user);
                    getUserConfig().saveConfig(true);
                }
                getUserNameResolver().update(oldUser, user);
                if (oldUser != null && user.status != null && oldUser.status != null && user.status.expires != oldUser.status.expires) {
                    return true;
                }
            } else if (oldUser == null) {
                users.put(user.id, user);
            } else if (oldUser.min) {
                if (oldUser.bot) {
                    if (oldUser.username != null) {
                        user.username = oldUser.username;
                        user.flags |= 8;
                    } else {
                        user.flags = user.flags & ~8;
                        user.username = null;
                    }
                }
                if (oldUser.apply_min_photo) {
                    if (oldUser.photo != null) {
                        user.photo = oldUser.photo;
                        user.flags |= 32;
                    } else {
                        user.flags = user.flags & ~32;
                        user.photo = null;
                    }
                }
                users.put(user.id, user);
            }
        }
        return false;
    }

    public void reloadUser(long userId) {
        TLRPC.TL_users_getUsers req = new TLRPC.TL_users_getUsers();
        TLRPC.InputUser inputPeer = getInputUser(userId);
        if (inputPeer == null) return;
        req.id.add(inputPeer);
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (res, err) -> {
            if (res instanceof Vector) {
                ArrayList<Object> objects = ((Vector) res).objects;
                ArrayList<TLRPC.User> users = new ArrayList<>();
                for (int i = 0; i < objects.size(); ++i) {
                    if (objects.get(i) instanceof TLRPC.User) {
                        users.add((TLRPC.User) objects.get(i));
                    }
                }
                getMessagesController().putUsers(users, false);
            }
        });
    }
    public void putUsers(ArrayList<TLRPC.User> users, boolean fromCache) {
        if (users == null || users.isEmpty()) {
            return;
        }
        boolean updateStatus = false;
        int count = users.size();
        for (int a = 0; a < count; a++) {
            TLRPC.User user = users.get(a);
            if (putUser(user, fromCache)) {
                updateStatus = true;
            }
        }
        if (updateStatus) {
            AndroidUtilities.runOnUIThread(() -> getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, UPDATE_MASK_STATUS));
        }
    }

    public void putChat(final TLRPC.Chat chat, boolean fromCache) {
        if (chat == null) {
            return;
        }
        TLRPC.Chat oldChat = chats.get(chat.id);
        if (oldChat == chat) {
            return;
        }
        if (oldChat != null && !TextUtils.isEmpty(oldChat.username)) {
            objectsByUsernames.remove(oldChat.username.toLowerCase());
        }
        if (oldChat != null && oldChat.usernames != null) {
            for (int i = 0; i < oldChat.usernames.size(); ++i) {
                TLRPC.TL_username u = oldChat.usernames.get(i);
                if (u != null && !TextUtils.isEmpty(u.username)) {
                    objectsByUsernames.remove(u.username.toLowerCase());
                }
            }
        }
        if (!TextUtils.isEmpty(chat.username)) {
            objectsByUsernames.put(chat.username.toLowerCase(), chat);
        }
        if (chat.usernames != null) {
            for (int i = 0; i < chat.usernames.size(); ++i) {
                TLRPC.TL_username u = chat.usernames.get(i);
                if (u != null && !TextUtils.isEmpty(u.username) && u.active) {
                    objectsByUsernames.put(u.username.toLowerCase(), chat);
                }
            }
        }
        updateEmojiStatusUntilUpdate(-chat.id, chat.emoji_status);
        if (oldChat != null && oldChat.access_hash == 0 && chat.fromMessageDialogId != 0 && chat.fromMessageId != 0) {
            oldChat.fromMessageDialogId = chat.fromMessageDialogId;
            oldChat.fromMessageId = chat.fromMessageId;
        }

        if (chat.min) {
            if (oldChat != null) {
                if (!fromCache) {
                    getUserNameResolver().update(oldChat, chat);
                    oldChat.title = chat.title;
                    oldChat.photo = chat.photo;
                    oldChat.broadcast = chat.broadcast;
                    oldChat.verified = chat.verified;
                    oldChat.megagroup = chat.megagroup;
                    oldChat.call_not_empty = chat.call_not_empty;
                    oldChat.call_active = chat.call_active;
                    oldChat.monoforum = chat.monoforum;
                    oldChat.broadcast_messages_allowed = chat.broadcast_messages_allowed;
                    if ((chat.flags2 & 262144) != 0) {
                        oldChat.linked_monoforum_id = chat.linked_monoforum_id;
                        oldChat.flags2 |= 262144;
                    }
                    if (chat.default_banned_rights != null) {
                        oldChat.default_banned_rights = chat.default_banned_rights;
                        oldChat.flags |= 262144;
                    }
                    if (chat.admin_rights != null) {
                        oldChat.admin_rights = chat.admin_rights;
                        oldChat.flags |= 16384;
                    }
                    if (chat.banned_rights != null) {
                        oldChat.banned_rights = chat.banned_rights;
                        oldChat.flags |= 32768;
                    }
                    if (chat.username != null) {
                        oldChat.username = chat.username;
                        oldChat.flags |= 64;
                    } else {
                        oldChat.flags = oldChat.flags & ~64;
                        oldChat.username = null;
                    }
                    if (chat.participants_count != 0) {
                        oldChat.participants_count = chat.participants_count;
                    }
                    addOrRemoveActiveVoiceChat(oldChat);
                    if (oldChat.forum != chat.forum) {
                        oldChat.forum = chat.forum;
                        if (oldChat.forum) {
                            oldChat.flags |= 1073741824;
                        } else {
                            oldChat.flags = oldChat.flags & ~1073741824;
                        }
                        getNotificationCenter().postNotificationName(NotificationCenter.chatSwitchedForum, chat.id, chat.forum, chat.forum_tabs);
                    }
                }
            } else {
                chats.put(chat.id, chat);
                addOrRemoveActiveVoiceChat(chat);
            }
        } else {
            if (!fromCache) {
                if (oldChat != null) {
                    if (chat.version != oldChat.version) {
                        loadedFullChats.delete(chat.id);
                    }
                    if (oldChat.participants_count != 0 && chat.participants_count == 0) {
                        chat.participants_count = oldChat.participants_count;
                        chat.flags |= 131072;
                    }

                    int oldFlags = oldChat.banned_rights != null ? oldChat.banned_rights.flags : 0;
                    int newFlags = chat.banned_rights != null ? chat.banned_rights.flags : 0;
                    int oldFlags2 = oldChat.default_banned_rights != null ? oldChat.default_banned_rights.flags : 0;
                    int newFlags2 = chat.default_banned_rights != null ? chat.default_banned_rights.flags : 0;
                    oldChat.default_banned_rights = chat.default_banned_rights;
                    if (oldChat.default_banned_rights == null) {
                        oldChat.flags &= ~262144;
                    } else {
                        oldChat.flags |= 262144;
                    }
                    oldChat.banned_rights = chat.banned_rights;
                    if (oldChat.banned_rights == null) {
                        oldChat.flags &= ~32768;
                    } else {
                        oldChat.flags |= 32768;
                    }
                    oldChat.admin_rights = chat.admin_rights;
                    if (oldChat.admin_rights == null) {
                        oldChat.flags &= ~16384;
                    } else {
                        oldChat.flags |= 16384;
                    }
                    if (chat.stories_hidden_min) {
                        chat.stories_hidden = oldChat.stories_hidden;
                    }
                    if (oldFlags != newFlags || oldFlags2 != newFlags2) {
                        AndroidUtilities.runOnUIThread(() -> getNotificationCenter().postNotificationName(NotificationCenter.channelRightsUpdated, chat));
                    }
                }
                chats.put(chat.id, chat);
            } else if (oldChat == null) {
                chats.put(chat.id, chat);
            } else if (oldChat.min) {
                chat.title = oldChat.title;
                chat.photo = oldChat.photo;
                chat.broadcast = oldChat.broadcast;
                chat.verified = oldChat.verified;
                chat.megagroup = oldChat.megagroup;

                if (oldChat.default_banned_rights != null) {
                    chat.default_banned_rights = oldChat.default_banned_rights;
                    chat.flags |= 262144;
                }
                if (oldChat.admin_rights != null) {
                    chat.admin_rights = oldChat.admin_rights;
                    chat.flags |= 16384;
                }
                if (oldChat.banned_rights != null) {
                    chat.banned_rights = oldChat.banned_rights;
                    chat.flags |= 32768;
                }
                if (oldChat.username != null) {
                    chat.username = oldChat.username;
                    chat.flags |= 64;
                } else {
                    chat.flags = chat.flags & ~64;
                    chat.username = null;
                }
                if (oldChat.participants_count != 0 && chat.participants_count == 0) {
                    chat.participants_count = oldChat.participants_count;
                    chat.flags |= 131072;
                }
                chats.put(chat.id, chat);
            }
            addOrRemoveActiveVoiceChat(chat);
        }
        if (oldChat != null && oldChat.forum != chat.forum) {
            AndroidUtilities.runOnUIThread(() -> {
                getNotificationCenter().postNotificationName(NotificationCenter.chatSwitchedForum, chat.id, chat.forum, chat.forum_tabs);
            });
        }
        if (oldChat != null && oldChat.collapsed_in_dialogs != chat.collapsed_in_dialogs) {
            AndroidUtilities.runOnUIThread(() -> {
                sortDialogs(null);
                getNotificationCenter().postNotificationName(NotificationCenter.communitySwitchedCollapsed, chat.id, chat.collapsed_in_dialogs);
                getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
            });
        }

        if (chat instanceof TLRPC.TL_community) {
            final long communityId = chat.id;
            if (getChatFull(communityId) == null) {
                getMessagesStorage().loadChatInfo(communityId, false, null, false, false);
                loadFullChat(communityId, 0, false);
            }
        }
    }

    public void putChats(ArrayList<TLRPC.Chat> chats, boolean fromCache) {
        if (chats == null || chats.isEmpty()) {
            return;
        }
        int count = chats.size();
        for (int a = 0; a < count; a++) {
            TLRPC.Chat chat = chats.get(a);
            putChat(chat, fromCache);
        }
    }

    private void addOrRemoveActiveVoiceChat(TLRPC.Chat chat) {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            AndroidUtilities.runOnUIThread(() -> addOrRemoveActiveVoiceChatInternal(chat));
        } else {
            addOrRemoveActiveVoiceChatInternal(chat);
        }
    }

    private void addOrRemoveActiveVoiceChatInternal(TLRPC.Chat chat) {
        TLRPC.Chat currentChat = activeVoiceChatsMap.get(chat.id);
        if (chat.call_active && chat.call_not_empty && chat.migrated_to == null && !ChatObject.isNotInChat(chat)) {
            if (currentChat != null) {
                return;
            }
            activeVoiceChatsMap.put(chat.id, chat);
            getNotificationCenter().postNotificationName(NotificationCenter.activeGroupCallsUpdated);
        } else {
            if (currentChat == null) {
                return;
            }
            activeVoiceChatsMap.remove(chat.id);
            getNotificationCenter().postNotificationName(NotificationCenter.activeGroupCallsUpdated);
        }
    }

    public ArrayList<Long> getActiveGroupCalls() {
        return new ArrayList<>(activeVoiceChatsMap.keySet());
    }

    public void setReferer(String referer) {
        if (referer == null) {
            return;
        }
        installReferer = referer;
        mainPreferences.edit().putString("installReferer", referer).commit();
    }

    public void putEncryptedChat(TLRPC.EncryptedChat encryptedChat, boolean fromCache) {
        if (encryptedChat == null) {
            return;
        }
        if (fromCache) {
            encryptedChats.putIfAbsent(encryptedChat.id, encryptedChat);
        } else {
            encryptedChats.put(encryptedChat.id, encryptedChat);
        }
    }

    public void putEncryptedChats(ArrayList<TLRPC.EncryptedChat> encryptedChats, boolean fromCache) {
        if (encryptedChats == null || encryptedChats.isEmpty()) {
            return;
        }
        int count = encryptedChats.size();
        for (int a = 0; a < count; a++) {
            TLRPC.EncryptedChat encryptedChat = encryptedChats.get(a);
            putEncryptedChat(encryptedChat, fromCache);
        }
    }

    public long getSendPaidMessagesStars(long did) {
        if (did > 0) {
            if (did == getUserConfig().getClientUserId()) {
                return 0;
            }
            final TLRPC.UserFull userFull = getUserFull(did);
            if (userFull != null) {
                return userFull.send_paid_messages_stars;
            }
            final TLRPC.User user = getUser(did);
            if (user != null && user.send_paid_messages_stars > 0) {
                return DialogObject.getMessagesStarsPrice(isUserContactBlocked(user.id));
            }
        } else if (did < 0) {
            final TLRPC.Chat chat = getChat(-did);
            if (ChatObject.hasAdminRights(chat)) {
                return 0;
            }
            if (ChatObject.isMonoForum(chat) && ChatObject.canManageMonoForum(currentAccount, chat)) {
                return 0;
            }

            TLRPC.ChatFull chatFull = getChatFull(-did);
            if (chatFull != null) {
                return chatFull.send_paid_messages_stars;
            } else if (chat != null) {
                return chat.send_paid_messages_stars;
            }
            return 0;
        }
        return 0;
    }

    public void putMonoForumLinkedChat(long chatId, long monoForumChatId) {
        monoForumLinkedChannels.put(chatId, monoForumChatId);
        monoForumLinkedChannels.put(monoForumChatId, chatId);
    }

    public TLRPC.Chat getMonoForumLinkedChat(long chatId) {
        TLRPC.Chat chat2 = getChat(chatId);
        if (chat2 != null) {
            final TLRPC.Chat chat = getChat(chat2.linked_monoforum_id);
            if (chat != null) {
                return chat;
            }
        }
        final Long linkedChatId = monoForumLinkedChannels.get(chatId);
        if (linkedChatId != null) {
            final TLRPC.Chat chat = getChat(linkedChatId);
            if (chat != null) {
                return chat;
            }
        }

        return null;
    }

    public TLRPC.UserFull getUserFull(long uid) {
        return fullUsers.get(uid);
    }

    public TLRPC.ChatFull getChatFull(long chatId) {
        return fullChats.get(chatId);
    }

    public void putGroupCall(long chatId, ChatObject.Call call) {
        groupCalls.put(call.call.id, call);
        groupCallsByChatId.put(chatId, call);
        TLRPC.ChatFull chatFull = getChatFull(chatId);
        if (chatFull != null) {
            chatFull.call = call.getInputGroupCall();
        }
        getNotificationCenter().postNotificationName(NotificationCenter.groupCallUpdated, chatId, call.call.id, false);
        loadFullChat(chatId, 0, true);
    }

    public ChatObject.Call getGroupCall(long chatId, boolean load) {
        return getGroupCall(chatId, load, null);
    }

    public ChatObject.Call getGroupCall(long chatId, boolean load, Runnable onLoad) {
        TLRPC.ChatFull chatFull = getChatFull(chatId);
        if (chatFull == null || chatFull.call == null) {
            return null;
        }
        ChatObject.Call result = groupCalls.get(chatFull.call.id);
        if (result == null && load && !loadingGroupCalls.contains(chatId)) {
            loadingGroupCalls.add(chatId);
            if (chatFull.call != null) {
                final TL_phone.getGroupCall req = new TL_phone.getGroupCall();
                req.call = chatFull.call;
                req.limit = 20;
                getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                    if (response != null) {
                        final TL_phone.groupCall groupCall = (TL_phone.groupCall) response;
                        putUsers(groupCall.users, false);
                        putChats(groupCall.chats, false);

                        final ChatObject.Call call = new ChatObject.Call();
                        call.setCall(getAccountInstance(), chatId, groupCall);
                        groupCalls.put(groupCall.call.id, call);
                        groupCallsByChatId.put(chatId, call);
                        getNotificationCenter().postNotificationName(NotificationCenter.groupCallUpdated, chatId, groupCall.call.id, false);
                        if (onLoad != null) {
                            onLoad.run();
                        }
                    }
                    loadingGroupCalls.remove(chatId);
                }));
            }
        }
        if (result != null && result.call instanceof TLRPC.TL_groupCallDiscarded) {
            return null;
        }
        return result;
    }

    public void cancelLoadFullUser(long userId) {
        loadingFullUsers.remove(userId);
    }

    public void cancelLoadFullChat(long chatId) {
        loadingFullChats.remove(chatId);
    }

    public void clearFullUsers() {
        loadedFullUsers.clear();
        loadedFullChats.clear();
    }

    private final LongSparseArray<Long> peerDialogsRequested = new LongSparseArray<Long>();
    private final long peerDialogRequestTimeout = 1000 * 60 * 4;

    private void reloadDialogsReadValue(ArrayList<TLRPC.Dialog> dialogs, long did) {
        if (did == 0 && (dialogs == null || dialogs.isEmpty())) {
            return;
        }
        TLRPC.TL_messages_getPeerDialogs req = new TLRPC.TL_messages_getPeerDialogs();
        if (dialogs != null) {
            for (int a = 0; a < dialogs.size(); a++) {
                TLRPC.InputPeer inputPeer = getInputPeer(dialogs.get(a).id);
                if (inputPeer instanceof TLRPC.TL_inputPeerChannel && inputPeer.access_hash == 0) {
                    continue;
                }
                TLRPC.TL_inputDialogPeer inputDialogPeer = new TLRPC.TL_inputDialogPeer();
                inputDialogPeer.peer = inputPeer;
                final long _did = DialogObject.getPeerDialogId(inputPeer);
                Long lastRequest = peerDialogsRequested.get(_did);
                if (lastRequest == null || System.currentTimeMillis() - lastRequest > peerDialogRequestTimeout) {
                    req.peers.add(inputDialogPeer);
                    peerDialogsRequested.put(_did, System.currentTimeMillis());
                }
            }
        } else {
            TLRPC.InputPeer inputPeer = getInputPeer(did);
            if (inputPeer instanceof TLRPC.TL_inputPeerChannel && inputPeer.access_hash == 0) {
                return;
            }
            TLRPC.TL_inputDialogPeer inputDialogPeer = new TLRPC.TL_inputDialogPeer();
            inputDialogPeer.peer = inputPeer;
            final long _did = DialogObject.getPeerDialogId(inputPeer);
            Long lastRequest = peerDialogsRequested.get(_did);
            if (lastRequest == null || System.currentTimeMillis() - lastRequest > peerDialogRequestTimeout) {
                req.peers.add(inputDialogPeer);
                peerDialogsRequested.put(_did, System.currentTimeMillis());
            }
        }
        if (req.peers.isEmpty()) {
            return;
        }
        getConnectionsManager().sendRequest(req, (response, error) -> {
            if (response != null) {
                TLRPC.TL_messages_peerDialogs res = (TLRPC.TL_messages_peerDialogs) response;
                ArrayList<TLRPC.Update> arrayList = new ArrayList<>();
                for (int a = 0; a < res.dialogs.size(); a++) {
                    TLRPC.Dialog dialog = res.dialogs.get(a);
                    DialogObject.initDialog(dialog);

                    Integer value = dialogs_read_inbox_max.get(dialog.id);
                    if (value == null) {
                        value = 0;
                    }
                    dialogs_read_inbox_max.put(dialog.id, Math.max(dialog.read_inbox_max_id, value));
                    if (value == 0) {
                        if (dialog.peer.channel_id != 0) {
                            TL_update.TL_updateReadChannelInbox update = new TL_update.TL_updateReadChannelInbox();
                            update.channel_id = dialog.peer.channel_id;
                            update.max_id = dialog.read_inbox_max_id;
                            update.still_unread_count = dialog.unread_count;
                            arrayList.add(update);
                        } else {
                            TL_update.TL_updateReadHistoryInbox update = new TL_update.TL_updateReadHistoryInbox();
                            update.peer = dialog.peer;
                            update.max_id = dialog.read_inbox_max_id;
                            arrayList.add(update);
                        }
                    }

                    value = dialogs_read_outbox_max.get(dialog.id);
                    if (value == null) {
                        value = 0;
                    }
                    dialogs_read_outbox_max.put(dialog.id, Math.max(dialog.read_outbox_max_id, value));
                    if (dialog.read_outbox_max_id > value) {
                        if (dialog.peer.channel_id != 0) {
                            TL_update.TL_updateReadChannelOutbox update = new TL_update.TL_updateReadChannelOutbox();
                            update.channel_id = dialog.peer.channel_id;
                            update.max_id = dialog.read_outbox_max_id;
                            arrayList.add(update);
                        } else {
                            TL_update.TL_updateReadHistoryOutbox update = new TL_update.TL_updateReadHistoryOutbox();
                            update.peer = dialog.peer;
                            update.max_id = dialog.read_outbox_max_id;
                            arrayList.add(update);
                        }
                    }
                }
                if (!arrayList.isEmpty()) {
                    processUpdateArray(arrayList, null, null, false, 0);
                }
            }
        });
    }

    public TLRPC.ChannelParticipant getAdminInChannel(long uid, long chatId) {
        LongSparseArray<TLRPC.ChannelParticipant> array = channelAdmins.get(chatId);
        if (array == null) {
            return null;
        }
        return array.get(uid);
    }

    public String getAdminRank(long chatId, long uid) {
        if (chatId == uid) return "";
        final LongSparseArray<TLRPC.ChannelParticipant> array = channelAdmins.get(chatId);
        if (array != null) {
            final TLRPC.ChannelParticipant participant = array.get(uid);
            if (participant != null) {
                if (participant.rank != null)
                    return participant.rank;
                if (participant instanceof TLRPC.TL_channelParticipantCreator)
                    return LocaleController.getString(R.string.ChatTagOwner);
                if (participant instanceof TLRPC.TL_channelParticipantAdmin)
                    return LocaleController.getString(R.string.ChatTagAdmin);
            }
        }
        final TLRPC.ChatFull chatFull = getChatFull(chatId);
        if (chatFull != null && chatFull.participants != null) {
            for (int i = 0; i < chatFull.participants.participants.size(); ++i) {
                final TLRPC.ChatParticipant p = chatFull.participants.participants.get(i);
                if (p.user_id == uid) {
                    if (p.rank != null) return p.rank;
                    if (p instanceof TLRPC.TL_chatChannelParticipant) {
                        final TLRPC.TL_chatChannelParticipant pp = (TLRPC.TL_chatChannelParticipant) p;
                        if (pp.channelParticipant != null) {
                            return pp.channelParticipant.rank;
                        }
                    }
                    if (p instanceof TLRPC.TL_chatParticipantCreator)
                        return LocaleController.getString(R.string.ChatTagOwner);
                    if (p instanceof TLRPC.TL_chatParticipantAdmin)
                        return LocaleController.getString(R.string.ChatTagAdmin);
                    return null;
                }
            }
        }
        return null;
    }

    public void updateRank(long chatId, long uid, String rank) {
        if (TextUtils.isEmpty(rank)) rank = null;
        final TLRPC.Chat chat = getChat(chatId);
        final LongSparseArray<TLRPC.ChannelParticipant> array = channelAdmins.get(chatId);
        if (array != null) {
            final TLRPC.ChannelParticipant participant = array.get(uid);
            if (participant != null) {
                participant.rank = rank;
            }
        }
        final TLRPC.ChatFull chatFull = getChatFull(chatId);
        if (chatFull != null && chatFull.participants != null) {
            for (int i = 0; i < chatFull.participants.participants.size(); ++i) {
                final TLRPC.ChatParticipant p = chatFull.participants.participants.get(i);
                p.setRank(uid, rank); if (p.user_id == uid) {
                    if (p instanceof TLRPC.TL_chatChannelParticipant) {
                        final TLRPC.TL_chatChannelParticipant pp = (TLRPC.TL_chatChannelParticipant) p;
                        if (pp.channelParticipant != null) {
                            pp.channelParticipant.rank = rank;
                        }
                    } else {
                        p.rank = rank;
                    }
                }
            }
        }
        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, 0);
        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updatedChatRanks, chatId, uid, rank);
        MessagesStorage.getInstance(currentAccount).updateRanksInLastMessages(-chatId, uid, rank);
    }

    public TLObject getParticipant(long chatId, long uid) {
        final LongSparseArray<TLRPC.ChannelParticipant> array = channelAdmins.get(chatId);
        if (array == null) {
            final TLRPC.ChatFull chatFull = getChatFull(chatId);
            if (chatFull != null && chatFull.participants != null) {
                for (int i = 0; i < chatFull.participants.participants.size(); ++i) {
                    final TLRPC.ChatParticipant p = chatFull.participants.participants.get(i);
                    if (p.user_id == uid) {
                        return p;
                    }
                }
            }
        }
        return array.get(uid);
    }

    public boolean isAdmin(long chatId, long uid) {
        if (chatId == uid) return true;
        final LongSparseArray<TLRPC.ChannelParticipant> array = channelAdmins.get(chatId);
        if (array == null) {
            final TLRPC.ChatFull chatFull = getChatFull(chatId);
            if (chatFull != null && chatFull.participants != null) {
                for (int i = 0; i < chatFull.participants.participants.size(); ++i) {
                    final TLRPC.ChatParticipant p = chatFull.participants.participants.get(i);
                    if (p.user_id == uid) {
                        return p instanceof TLRPC.TL_chatParticipantAdmin || p instanceof TLRPC.TL_chatParticipantCreator;
                    }
                }
            }
        }
        final TLRPC.ChannelParticipant participant = array.get(uid);
        return participant instanceof TLRPC.TL_channelParticipantAdmin || participant instanceof TLRPC.TL_channelParticipantCreator;
    }

    public boolean isOwner(long chatId, long uid) {
        if (chatId == uid) return true;
        final TLRPC.Chat chat = getChat(chatId);
        if (getUserConfig().getClientUserId() == uid && chat != null && chat.creator) return true;
        final LongSparseArray<TLRPC.ChannelParticipant> array = channelAdmins.get(chatId);
        if (array == null) {
            final TLRPC.ChatFull chatFull = getChatFull(chatId);
            if (chatFull != null && chatFull.participants != null) {
                for (int i = 0; i < chatFull.participants.participants.size(); ++i) {
                    final TLRPC.ChatParticipant p = chatFull.participants.participants.get(i);
                    if (p.user_id == uid) {
                        return p instanceof TLRPC.TL_chatParticipantCreator;
                    }
                }
            }
        }
        final TLRPC.ChannelParticipant participant = array.get(uid);
        return participant instanceof TLRPC.TL_channelParticipantCreator;
    }

    public boolean isChannelAdminsLoaded(long chatId) {
        return channelAdmins.get(chatId) != null;
    }

    public void loadChannelAdmins(long chatId, boolean cache) {
        int loadTime = loadingChannelAdmins.get(chatId);
        if ((SystemClock.elapsedRealtime() / 1000) - loadTime < 60) {
            return;
        }
        loadingChannelAdmins.put(chatId, (int) (SystemClock.elapsedRealtime() / 1000));
        if (cache) {
            getMessagesStorage().loadChannelAdmins(chatId);
        } else {
            TLRPC.TL_channels_getParticipants req = new TLRPC.TL_channels_getParticipants();
            req.channel = getInputChannel(chatId);
            req.limit = 100;
            req.filter = new TLRPC.TL_channelParticipantsAdmins();
            getConnectionsManager().sendRequest(req, (response, error) -> {
                if (response instanceof TLRPC.TL_channels_channelParticipants) {
                    processLoadedAdminsResponse(chatId, (TLRPC.TL_channels_channelParticipants) response);
                }
            });
        }
    }

    public void processLoadedAdminsResponse(long chatId, TLRPC.TL_channels_channelParticipants participants) {
        LongSparseArray<TLRPC.ChannelParticipant> array1 = new LongSparseArray<>(participants.participants.size());
        for (int a = 0; a < participants.participants.size(); a++) {
            TLRPC.ChannelParticipant participant = participants.participants.get(a);
            array1.put(MessageObject.getPeerId(participant.peer), participant);
        }
        processLoadedChannelAdmins(array1, chatId, false);
    }

    public void processLoadedChannelAdmins(final LongSparseArray<TLRPC.ChannelParticipant> array, long chatId, boolean cache) {
        if (!cache) {
            getMessagesStorage().putChannelAdmins(chatId, array);
        }
        AndroidUtilities.runOnUIThread(() -> {
            channelAdmins.put(chatId, array);
            if (cache) {
                loadingChannelAdmins.delete(chatId);
                loadChannelAdmins(chatId, false);
                getNotificationCenter().postNotificationName(NotificationCenter.didLoadChatAdmins, chatId);
            }
        });
    }

    public void loadFullChat(long chatId, int classGuid, boolean force) {
        long lastLoadedTime = loadedFullChats.get(chatId, 0);
        boolean loaded = lastLoadedTime > 0;
        if (loadingFullChats.contains(chatId) || !force && loaded) {
            return;
        }
        loadingFullChats.add(chatId);
        TLObject request;
        long dialogId = -chatId;
        TLRPC.Chat chat = getChat(chatId);
        if (ChatObject.isChannel(chat)) {
            TLRPC.TL_channels_getFullChannel req = new TLRPC.TL_channels_getFullChannel();
            req.channel = getInputChannel(chat);
            request = req;
            loadChannelAdmins(chatId, !loaded);
        } else {
            TLRPC.TL_messages_getFullChat req = new TLRPC.TL_messages_getFullChat();
            req.chat_id = chatId;
            request = req;
            if (dialogs_read_inbox_max.get(dialogId) == null || dialogs_read_outbox_max.get(dialogId) == null) {
                reloadDialogsReadValue(null, dialogId);
            }
        }
        int reqId = getConnectionsManager().sendRequest(request, (response, error) -> {
            if (error == null) {
                TLRPC.TL_messages_chatFull res = (TLRPC.TL_messages_chatFull) response;
                getMessagesStorage().putUsersAndChats(res.users, res.chats, true, true);
                getMessagesStorage().updateChatInfo(res.full_chat, false);
                getStoriesController().updateStoriesFromFullPeer(dialogId, res.full_chat.stories);
                ChatThemeController.getInstance(currentAccount).saveChatWallpaper(-chatId, res.full_chat.wallpaper);
                if (ChatObject.isChannel(chat)) {
                    Integer value = dialogs_read_inbox_max.get(dialogId);
                    if (value == null) {
                        value = getMessagesStorage().getDialogReadMax(false, dialogId);
                    }

                    dialogs_read_inbox_max.put(dialogId, Math.max(res.full_chat.read_inbox_max_id, value));
                    if (res.full_chat.read_inbox_max_id > value) {
                        ArrayList<TLRPC.Update> arrayList = new ArrayList<>();
                        TL_update.TL_updateReadChannelInbox update = new TL_update.TL_updateReadChannelInbox();
                        update.channel_id = chatId;
                        update.max_id = res.full_chat.read_inbox_max_id;
                        update.still_unread_count = res.full_chat.unread_count;
                        arrayList.add(update);
                        processUpdateArray(arrayList, null, null, false, 0);
                    }

                    value = dialogs_read_outbox_max.get(dialogId);
                    if (value == null) {
                        value = getMessagesStorage().getDialogReadMax(true, dialogId);
                    }
                    dialogs_read_outbox_max.put(dialogId, Math.max(res.full_chat.read_outbox_max_id, value));
                    if (res.full_chat.read_outbox_max_id > value) {
                        ArrayList<TLRPC.Update> arrayList = new ArrayList<>();
                        TL_update.TL_updateReadChannelOutbox update = new TL_update.TL_updateReadChannelOutbox();
                        update.channel_id = chatId;
                        update.max_id = res.full_chat.read_outbox_max_id;
                        arrayList.add(update);
                        processUpdateArray(arrayList, null, null, false, 0);
                    }
                }

                AndroidUtilities.runOnUIThread(() -> {
                    TLRPC.ChatFull old = fullChats.get(chatId);
                    if (old != null) {
                        res.full_chat.inviterId = old.inviterId;
                    }
                    fullChats.put(chatId, res.full_chat);
                    getTranslateController().updateDialogFull(-chatId);

                    applyDialogNotificationsSettings(-chatId, 0, res.full_chat.notify_settings);
                    for (int a = 0; a < res.full_chat.bot_info.size(); a++) {
                        TL_bots.BotInfo botInfo = res.full_chat.bot_info.get(a);
                        getMediaDataController().putBotInfo(-chatId, botInfo);
                    }
                    int index = blockePeers.indexOfKey(-chatId);
                    if (res.full_chat.blocked) {
                        if (index < 0) {
                            blockePeers.put(-chatId, 1);
                            getNotificationCenter().postNotificationName(NotificationCenter.blockedUsersDidLoad);
                        }
                    } else {
                        if (index >= 0) {
                            blockePeers.removeAt(index);
                            getNotificationCenter().postNotificationName(NotificationCenter.blockedUsersDidLoad);
                        }
                    }
                    exportedChats.put(chatId, res.full_chat.exported_invite);
                    loadingFullChats.remove(chatId);
                    loadedFullChats.put(chatId,  System.currentTimeMillis());

                    putUsers(res.users, false);
                    putChats(res.chats, false);
                    if (res.full_chat.stickerset != null) {
                        getMediaDataController().getGroupStickerSetById(res.full_chat.stickerset);
                    }
                    if (res.full_chat.emojiset != null) {
                        getMediaDataController().getGroupStickerSetById(res.full_chat.emojiset);
                    }
                    getNotificationCenter().postNotificationName(NotificationCenter.chatInfoDidLoad, res.full_chat, classGuid, false, true);

                    TLRPC.Dialog dialog = dialogs_dict.get(-chatId);
                    if (dialog != null) {
                        if ((res.full_chat.flags & 2048) != 0) {
                            if (dialog.folder_id != res.full_chat.folder_id) {
                                dialog.folder_id = res.full_chat.folder_id;
                                sortDialogs(null);
                                getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
                            }
                        }
                        if (dialog.ttl_period != res.full_chat.ttl_period) {
                            dialog.ttl_period = res.full_chat.ttl_period;
                            getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
                        }
                        if (dialog.view_forum_as_messages != res.full_chat.view_forum_as_messages) {
                            dialog.view_forum_as_messages = res.full_chat.view_forum_as_messages;
                            getMessagesStorage().setDialogViewThreadAsMessages(dialogId, res.full_chat.view_forum_as_messages);
                        }
                    }
                });
            } else {
                AndroidUtilities.runOnUIThread(() -> {
                    checkChannelError(error.text, chatId);
                    loadingFullChats.remove(chatId);
                });
            }
        });
        if (classGuid != 0) {
            getConnectionsManager().bindRequestToGuid(reqId, classGuid);
        }
    }

    public void loadFullUser(final TLRPC.User user, int classGuid, boolean force) {
        loadFullUser(user, classGuid, force, null);
    }
    public void loadFullUser(final TLRPC.User user, int classGuid, boolean force, Utilities.Callback<TLRPC.UserFull> whenReceivedFullUser) {
        if (user == null || whenReceivedFullUser == null && (loadingFullUsers.contains(user.id) || !force && loadedFullUsers.get(user.id) > 0)) {
            return;
        }
        loadingFullUsers.add(user.id);
        TLRPC.TL_users_getFullUser req = new TLRPC.TL_users_getFullUser();
        req.id = getInputUser(user);
        long dialogId = user.id;
        if (dialogs_read_inbox_max.get(dialogId) == null || dialogs_read_outbox_max.get(dialogId) == null) {
            reloadDialogsReadValue(null, dialogId);
        }
        int reqId = getConnectionsManager().sendRequest(req, (response, error) -> {
            if (error == null) {
                TLRPC.TL_users_userFull res = (TLRPC.TL_users_userFull) response;
                TLRPC.UserFull userFull = res.full_user;
                putUsers(res.users, false);
                putChats(res.chats, false);
                res.full_user.user = getUser(res.full_user.id);
                getMessagesStorage().updateUserInfo(userFull, false);
                getStoriesController().updateStoriesFromFullPeer(dialogId, userFull.stories);
                ChatThemeController.getInstance(currentAccount).saveChatWallpaper(res.full_user.id, res.full_user.wallpaper);

                if (whenReceivedFullUser != null) {
                    whenReceivedFullUser.run(userFull);
                }

                AndroidUtilities.runOnUIThread(() -> {
                    savePeerSettings(userFull.user.id, userFull.settings, false);

                    applyDialogNotificationsSettings(user.id, 0, userFull.notify_settings);
                    if (userFull.bot_info instanceof TL_bots.TL_botInfo) {
                        userFull.bot_info.user_id = user.id;
                        getMediaDataController().putBotInfo(user.id, userFull.bot_info);
                    }
                    int index = blockePeers.indexOfKey(user.id);
                    if (userFull.blocked) {
                        if (index < 0) {
                            blockePeers.put(user.id, 1);
                            getNotificationCenter().postNotificationName(NotificationCenter.blockedUsersDidLoad);
                        }
                    } else {
                        if (index >= 0) {
                            blockePeers.removeAt(index);
                            getNotificationCenter().postNotificationName(NotificationCenter.blockedUsersDidLoad);
                        }
                    }
                    fullUsers.put(user.id, userFull);
                    getTranslateController().updateDialogFull(user.id);
                    StarsController.getInstance(currentAccount).invalidateProfileGifts(userFull);
                    loadingFullUsers.remove(user.id);
                    loadedFullUsers.put(user.id, System.currentTimeMillis());
                    String names = user.first_name + user.last_name + UserObject.getPublicUsername(user);
                    ArrayList<TLRPC.User> users = new ArrayList<>();
                    users.add(userFull.user);
                    putUsers(users, false);
                    getMessagesStorage().putUsersAndChats(users, null, false, true);
                    if (!names.equals(userFull.user.first_name + userFull.user.last_name + UserObject.getPublicUsername(userFull.user))) {
                        getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, UPDATE_MASK_NAME);
                    }
                    if (userFull.user.photo != null && userFull.user.photo.has_video) {
                        getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, UPDATE_MASK_AVATAR);
                    }
                    if (userFull.bot_info instanceof TL_bots.TL_botInfo) {
                        userFull.bot_info.user_id = userFull.id;
                        getNotificationCenter().postNotificationName(NotificationCenter.botInfoDidLoad, userFull.bot_info, classGuid);
                    }
                    getNotificationCenter().postNotificationName(NotificationCenter.userInfoDidLoad, user.id, userFull);

                    TLRPC.Dialog dialog = dialogs_dict.get(user.id);
                    if (dialog != null) {
                        if ((userFull.flags & 2048) != 0 && dialog.folder_id != userFull.folder_id) {
                            dialog.folder_id = userFull.folder_id;
                            sortDialogs(null);
                            getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
                        }
                        if ((userFull.flags & 16384) != 0 && dialog.ttl_period != userFull.ttl_period) {
                            dialog.ttl_period = userFull.ttl_period;
                            getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
                        }
                    }
                });
            } else {
                AndroidUtilities.runOnUIThread(() -> loadingFullUsers.remove(user.id));
            }
        });
        getConnectionsManager().bindRequestToGuid(reqId, classGuid);
    }

    private void reloadMessages(ArrayList<Integer> mids, long dialogId, int mode) {
        if (mids.isEmpty()) {
            return;
        }
        final boolean scheduled = mode == ChatActivity.MODE_SCHEDULED;
        final boolean saved = mode == ChatActivity.MODE_SAVED;
        TLObject request;
        ArrayList<Integer> result = new ArrayList<>();
        TLRPC.Chat chat;
        if (DialogObject.isChatDialog(dialogId)) {
            chat = getChat(-dialogId);
        } else {
            chat = null;
        }
        if (ChatObject.isChannel(chat)) {
            TLRPC.TL_channels_getMessages req = new TLRPC.TL_channels_getMessages();
            req.channel = getInputChannel(chat);
            req.id = result;
            request = req;
        } else {
            TLRPC.TL_messages_getMessages req = new TLRPC.TL_messages_getMessages();
            req.id = result;
            request = req;
        }
        ArrayList<Integer> arrayList = reloadingMessages.get(dialogId);
        for (int a = 0; a < mids.size(); a++) {
            Integer mid = mids.get(a);
            if (arrayList != null && arrayList.contains(mid)) {
                continue;
            }
            result.add(mid);
        }
        if (result.isEmpty()) {
            return;
        }
        if (arrayList == null) {
            arrayList = new ArrayList<>();
            reloadingMessages.put(dialogId, arrayList);
        }
        arrayList.addAll(result);
        getConnectionsManager().sendRequest(request, (response, error) -> {
            if (error == null) {
                TLRPC.messages_Messages messagesRes = (TLRPC.messages_Messages) response;

                LongSparseArray<TLRPC.User> usersLocal = new LongSparseArray<>();
                for (int a = 0; a < messagesRes.users.size(); a++) {
                    TLRPC.User u = messagesRes.users.get(a);
                    usersLocal.put(u.id, u);
                }
                LongSparseArray<TLRPC.Chat> chatsLocal = new LongSparseArray<>();
                for (int a = 0; a < messagesRes.chats.size(); a++) {
                    TLRPC.Chat c = messagesRes.chats.get(a);
                    chatsLocal.put(c.id, c);
                }

                Integer inboxValue = dialogs_read_inbox_max.get(dialogId);
                if (inboxValue == null) {
                    inboxValue = getMessagesStorage().getDialogReadMax(false, dialogId);
                    dialogs_read_inbox_max.put(dialogId, inboxValue);
                }

                Integer outboxValue = dialogs_read_outbox_max.get(dialogId);
                if (outboxValue == null) {
                    outboxValue = getMessagesStorage().getDialogReadMax(true, dialogId);
                    dialogs_read_outbox_max.put(dialogId, outboxValue);
                }

                ArrayList<MessageObject> objects = new ArrayList<>();
                for (int a = 0; a < messagesRes.messages.size(); a++) {
                    TLRPC.Message message = messagesRes.messages.get(a);
                    message.dialog_id = dialogId;
                    if (!scheduled) {
                        message.unread = (message.out ? outboxValue : inboxValue) < message.id;
                    }
                    objects.add(new MessageObject(currentAccount, message, usersLocal, chatsLocal, true, true));
                }

                ImageLoader.saveMessagesThumbs(messagesRes.messages);
                getMessagesStorage().putMessages(messagesRes, dialogId, -1, 0, false, mode, 0);

                AndroidUtilities.runOnUIThread(() -> {
                    ArrayList<Integer> arrayList1 = reloadingMessages.get(dialogId);
                    if (arrayList1 != null) {
                        arrayList1.removeAll(result);
                        if (arrayList1.isEmpty()) {
                            reloadingMessages.remove(dialogId);
                        }
                    }
                    ArrayList<MessageObject> dialogObjs = dialogMessage.get(dialogId);
                    if (dialogObjs != null) {
                        for (int i = 0; i < dialogObjs.size(); ++i) {
                            MessageObject dialogObj = dialogObjs.get(i);
                            for (int a = 0; a < objects.size(); a++) {
                                MessageObject obj = objects.get(a);
                                if (dialogObj.getId() == obj.getId()) {
                                    dialogObjs.set(i, obj);
                                    if (obj.messageOwner.peer_id.channel_id == 0) {
                                        MessageObject obj2 = dialogMessagesByIds.get(obj.getId());
                                        dialogMessagesByIds.remove(obj.getId());
                                        if (obj2 != null) {
                                            dialogMessagesByIds.put(obj2.getId(), obj2);
                                        }
                                    }
                                    getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
                                    break;
                                }
                            }
                        }
                    }
                    getNotificationCenter().postNotificationName(NotificationCenter.replaceMessagesObjects, dialogId, objects);
                });
            }
        });
    }

    public void hidePeerSettingsBar(final long dialogId, TLRPC.User currentUser, TLRPC.Chat currentChat) {
        if (currentUser == null && currentChat == null) {
            return;
        }
        SharedPreferences.Editor editor = notificationsPreferences.edit();
        editor.putInt("dialog_bar_vis3" + dialogId, 3);
        editor.remove("dialog_bar_invite" + dialogId);
        editor.commit();
        if (!DialogObject.isEncryptedDialog(dialogId)) {
            TLRPC.TL_messages_hidePeerSettingsBar req = new TLRPC.TL_messages_hidePeerSettingsBar();
            if (currentUser != null) {
                req.peer = getInputPeer(currentUser.id);
            } else {
                req.peer = getInputPeer(-currentChat.id);
            }
            getConnectionsManager().sendRequest(req, (response, error) -> {

            });
        }
    }

    public void reportSpam(final long dialogId, TLRPC.User currentUser, TLRPC.Chat currentChat, TLRPC.EncryptedChat currentEncryptedChat, boolean geo) {
        if (currentUser == null && currentChat == null && currentEncryptedChat == null) {
            return;
        }
        SharedPreferences.Editor editor = notificationsPreferences.edit();
        editor.putInt("dialog_bar_vis3" + dialogId, 3);
        editor.commit();
        if (DialogObject.isEncryptedDialog(dialogId)) {
            if (currentEncryptedChat == null || currentEncryptedChat.access_hash == 0) {
                return;
            }
            TLRPC.TL_messages_reportEncryptedSpam req = new TLRPC.TL_messages_reportEncryptedSpam();
            req.peer = new TLRPC.TL_inputEncryptedChat();
            req.peer.chat_id = currentEncryptedChat.id;
            req.peer.access_hash = currentEncryptedChat.access_hash;
            getConnectionsManager().sendRequest(req, (response, error) -> {

            }, ConnectionsManager.RequestFlagFailOnServerErrors);
        } else {
            if (geo) {
                TL_account.reportPeer req = new TL_account.reportPeer();
                if (currentChat != null) {
                    req.peer = getInputPeer(-currentChat.id);
                } else if (currentUser != null) {
                    req.peer = getInputPeer(currentUser.id);
                }
                req.message = "";
                req.reason = new TLRPC.TL_inputReportReasonGeoIrrelevant();
                getConnectionsManager().sendRequest(req, (response, error) -> {

                }, ConnectionsManager.RequestFlagFailOnServerErrors);
            } else {
                TLRPC.TL_messages_reportSpam req = new TLRPC.TL_messages_reportSpam();
                if (currentChat != null) {
                    req.peer = getInputPeer(-currentChat.id);
                } else if (currentUser != null) {
                    req.peer = getInputPeer(currentUser.id);
                }
                getConnectionsManager().sendRequest(req, (response, error) -> {

                }, ConnectionsManager.RequestFlagFailOnServerErrors);
            }
        }
    }

    private void savePeerSettings(long dialogId, TLRPC.PeerSettings settings, boolean update) {
        if (settings == null) {
            return;
        }
        final SharedPreferences.Editor editor = notificationsPreferences.edit();
        if (settings.business_bot_id != 0) {
            editor.putLong("dialog_botid" + dialogId, settings.business_bot_id);
            editor.putString("dialog_boturl" + dialogId, settings.business_bot_manage_url);
            editor.putInt("dialog_botflags" + dialogId, (settings.business_bot_paused ? 1 : 0) + (settings.business_bot_can_reply ? 2 : 0));
        } else {
            editor.remove("dialog_botid" + dialogId).remove("dialog_boturl" + dialogId).remove("dialog_botflags" + dialogId);
        }
        editor.putLong("dialog_bar_paying_" + dialogId, settings.charge_paid_message_stars);
        if (notificationsPreferences.getInt("dialog_bar_vis3" + dialogId, 0) == 3) {
            editor.apply();
            getNotificationCenter().postNotificationName(NotificationCenter.peerSettingsDidLoad, dialogId);
            return;
        }
        boolean bar_hidden = settings.flags == 0;
        if (BuildVars.LOGS_ENABLED) {
            FileLog.d("peer settings loaded for " + dialogId + " add = " + settings.add_contact + " block = " + settings.block_contact + " spam = " + settings.report_spam + " share = " + settings.share_contact + " geo = " + settings.report_geo + " hide = " + bar_hidden + " distance = " + settings.geo_distance + " invite = " + settings.invite_members);
        }
        editor.putInt("dialog_bar_vis3" + dialogId, bar_hidden ? 1 : 2);
        editor.putBoolean("dialog_bar_share" + dialogId, settings.share_contact);
        editor.putBoolean("dialog_bar_report" + dialogId, settings.report_spam);
        editor.putBoolean("dialog_bar_add" + dialogId, settings.add_contact);
        editor.putBoolean("dialog_bar_block" + dialogId, settings.block_contact);
        editor.putBoolean("dialog_bar_exception" + dialogId, settings.need_contacts_exception);
        editor.putBoolean("dialog_bar_location" + dialogId, settings.report_geo);
        editor.putBoolean("dialog_bar_archived" + dialogId, settings.autoarchived);
        editor.putBoolean("dialog_bar_invite" + dialogId, settings.invite_members);
        editor.putString("dialog_bar_chat_with_admin_title" + dialogId, settings.request_chat_title);
        editor.putBoolean("dialog_bar_chat_with_channel" + dialogId, settings.request_chat_broadcast);
        editor.putInt("dialog_bar_chat_with_date" + dialogId, settings.request_chat_date);
        if (notificationsPreferences.getInt("dialog_bar_distance" + dialogId, -1) != -2) {
            if ((settings.flags & 64) != 0) {
                editor.putInt("dialog_bar_distance" + dialogId, settings.geo_distance);
            } else {
                editor.remove("dialog_bar_distance" + dialogId);
            }
        }
        if (dialogId == getUserConfig().getClientUserId()) {
            settings.business_bot_id = UserObject.REPLY_BOT;
            settings.business_bot_manage_url = "https://telegram.org/";
        }
        editor.apply();
        userPeerSettings.put(dialogId, settings);
        getNotificationCenter().postNotificationName(NotificationCenter.peerSettingsDidLoad, dialogId);
    }

    public TLRPC.PeerSettings getPeerSettings(long dialogId) {
        TLRPC.UserFull userFull = getUserFull(dialogId);
        if (userFull != null && userFull.settings != null) {
            return userFull.settings;
        }
        return userPeerSettings.get(dialogId);
    }

    public void loadPeerSettings(TLRPC.User currentUser, TLRPC.Chat currentChat) {
        loadPeerSettings(currentUser, currentChat, false);
    }
    public void loadPeerSettings(TLRPC.User currentUser, TLRPC.Chat currentChat, boolean force) {
        if (currentUser == null && currentChat == null) {
            return;
        }
        long dialogId;
        if (currentUser != null) {
            dialogId = currentUser.id;
        } else {
            dialogId = -currentChat.id;
        }
        if (loadingPeerSettings.indexOfKey(dialogId) >= 0) {
            return;
        }
        if (BuildVars.LOGS_ENABLED) {
            FileLog.d("request spam button for " + dialogId);
        }
        int vis = notificationsPreferences.getInt("dialog_bar_vis3" + dialogId, 0);
        if (!force && (vis == 1 || vis == 3)) {
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("dialog bar already hidden for " + dialogId);
            }
            return;
        }
        loadingPeerSettings.put(dialogId, true);
        TLRPC.TL_messages_getPeerSettings req = new TLRPC.TL_messages_getPeerSettings();
        if (currentUser != null) {
            req.peer = getInputPeer(currentUser.id);
        } else {
            req.peer = getInputPeer(-currentChat.id);
        }
        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            loadingPeerSettings.remove(dialogId);
            if (response != null) {
                TLRPC.TL_messages_peerSettings res = (TLRPC.TL_messages_peerSettings) response;
                TLRPC.PeerSettings settings = res.settings;
                putUsers(res.users, false);
                putChats(res.chats, false);

                savePeerSettings(dialogId, settings, false);
            }
        }));
    }

    protected void processNewChannelDifferenceParams(int pts, int pts_count, long channelId) {
        if (BuildVars.LOGS_ENABLED) {
            FileLog.d("processNewChannelDifferenceParams pts = " + pts + " pts_count = " + pts_count + " channeldId = " + channelId);
        }
        int channelPts = channelsPts.get(channelId);
        if (channelPts == 0) {
            channelPts = getMessagesStorage().getChannelPtsSync(channelId);
            if (channelPts == 0) {
                channelPts = 1;
            }
            channelsPts.put(channelId, channelPts);
        }
        if (channelPts + pts_count == pts) {
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("APPLY CHANNEL PTS");
            }
            channelsPts.put(channelId, pts);
            getMessagesStorage().saveChannelPts(channelId, pts);
        } else if (channelPts != pts) {
            long updatesStartWaitTime = updatesStartWaitTimeChannels.get(channelId);
            boolean gettingDifferenceChannel = gettingDifferenceChannels.get(channelId, false);
            if (gettingDifferenceChannel || updatesStartWaitTime == 0 || Math.abs(System.currentTimeMillis() - updatesStartWaitTime) <= 1500) {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.d("ADD CHANNEL UPDATE TO QUEUE pts = " + pts + " pts_count = " + pts_count);
                }
                if (updatesStartWaitTime == 0) {
                    updatesStartWaitTimeChannels.put(channelId, System.currentTimeMillis());
                }
                UserActionUpdatesPts updates = new UserActionUpdatesPts();
                updates.pts = pts;
                updates.pts_count = pts_count;
                updates.chat_id = channelId;
                ArrayList<TLRPC.Updates> arrayList = updatesQueueChannels.get(channelId);
                if (arrayList == null) {
                    arrayList = new ArrayList<>();
                    updatesQueueChannels.put(channelId, arrayList);
                }
                arrayList.add(updates);
            } else {
                getChannelDifference(channelId);
            }
        }
    }

    public void processNewDifferenceParams(int seq, int pts, int date, int pts_count) {
        if (BuildVars.LOGS_ENABLED) {
            FileLog.d("processNewDifferenceParams seq = " + seq + " pts = " + pts + " date = " + date + " pts_count = " + pts_count);
        }
        if (pts != -1) {
            if (getMessagesStorage().getLastPtsValue() + pts_count == pts) {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.d("APPLY PTS");
                }
                getMessagesStorage().setLastPtsValue(pts);
                getMessagesStorage().saveDiffParams(getMessagesStorage().getLastSeqValue(), getMessagesStorage().getLastPtsValue(), getMessagesStorage().getLastDateValue(), getMessagesStorage().getLastQtsValue());
            } else if (getMessagesStorage().getLastPtsValue() != pts) {
                if (gettingDifference || updatesStartWaitTimePts == 0 || Math.abs(System.currentTimeMillis() - updatesStartWaitTimePts) <= 1500) {
                    if (BuildVars.LOGS_ENABLED) {
                        FileLog.d("ADD UPDATE TO QUEUE pts = " + pts + " pts_count = " + pts_count);
                    }
                    if (updatesStartWaitTimePts == 0) {
                        updatesStartWaitTimePts = System.currentTimeMillis();
                    }
                    UserActionUpdatesPts updates = new UserActionUpdatesPts();
                    updates.pts = pts;
                    updates.pts_count = pts_count;
                    updatesQueuePts.add(updates);
                } else {
                    getDifference();
                }
            }
        }
        if (seq != -1) {
            if (getMessagesStorage().getLastSeqValue() + 1 == seq) {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.d("APPLY SEQ");
                }
                getMessagesStorage().setLastSeqValue(seq);
                if (date != -1) {
                    getMessagesStorage().setLastDateValue(date);
                }
                getMessagesStorage().saveDiffParams(getMessagesStorage().getLastSeqValue(), getMessagesStorage().getLastPtsValue(), getMessagesStorage().getLastDateValue(), getMessagesStorage().getLastQtsValue());
            } else if (getMessagesStorage().getLastSeqValue() != seq) {
                if (gettingDifference || updatesStartWaitTimeSeq == 0 || Math.abs(System.currentTimeMillis() - updatesStartWaitTimeSeq) <= 1500) {
                    if (BuildVars.LOGS_ENABLED) {
                        FileLog.d("ADD UPDATE TO QUEUE seq = " + seq);
                    }
                    if (updatesStartWaitTimeSeq == 0) {
                        updatesStartWaitTimeSeq = System.currentTimeMillis();
                    }
                    UserActionUpdatesSeq updates = new UserActionUpdatesSeq();
                    updates.seq = seq;
                    updatesQueueSeq.add(updates);
                } else {
                    getDifference();
                }
            }
        }
    }

    public void didAddedNewTask(int minDate, long dialogId, SparseArray<ArrayList<Integer>> mids) {
        Utilities.stageQueue.postRunnable(() -> {
            if (currentDeletingTaskMids == null && currentDeletingTaskMediaMids == null && !gettingNewDeleteTask || currentDeletingTaskTime != 0 && minDate < currentDeletingTaskTime) {
                getNewDeleteTask(null, null);
            }
        });
        if (mids != null) {
            AndroidUtilities.runOnUIThread(() -> getNotificationCenter().postNotificationName(NotificationCenter.didCreatedNewDeleteTask, dialogId, mids));
        }
    }

    public void getNewDeleteTask(LongSparseArray<ArrayList<Integer>> oldTask, LongSparseArray<ArrayList<Integer>> oldTaskMedia) {
        Utilities.stageQueue.postRunnable(() -> {
            gettingNewDeleteTask = true;
            getMessagesStorage().getNewTask(oldTask, oldTaskMedia);
        });
    }

    private boolean checkDeletingTask(boolean runnable) {
        int currentServerTime = getConnectionsManager().getCurrentTime();

        if ((currentDeletingTaskMids != null || currentDeletingTaskMediaMids != null) && (runnable || currentDeletingTaskTime != 0 && currentDeletingTaskTime <= currentServerTime)) {
            currentDeletingTaskTime = 0;
            if (currentDeleteTaskRunnable != null && !runnable) {
                Utilities.stageQueue.cancelRunnable(currentDeleteTaskRunnable);
            }
            currentDeleteTaskRunnable = null;
            LongSparseArray<ArrayList<Integer>> task = currentDeletingTaskMids != null ? currentDeletingTaskMids.clone() : null;
            LongSparseArray<ArrayList<Integer>> taskMedia = currentDeletingTaskMediaMids != null ? currentDeletingTaskMediaMids.clone() : null;
            AndroidUtilities.runOnUIThread(() -> {
                if (task != null) {
                    for (int a = 0, N = task.size(); a < N; a++) {
                        ArrayList<Integer> mids = task.valueAt(a);
                        deleteMessages(mids, null, null, task.keyAt(a), 0, true, 0, !mids.isEmpty() && mids.get(0) > 0);
                    }
                }
                if (taskMedia != null) {
                    final boolean checkViewer = SecretMediaViewer.hasInstance() && SecretMediaViewer.getInstance().isVisible();
                    final MessageObject viewerObject = checkViewer ? SecretMediaViewer.getInstance().getCurrentMessageObject() : null;
                    for (int a = 0, N = taskMedia.size(); a < N; a++) {
                        long dialogId = taskMedia.keyAt(a);
                        ArrayList<Integer> mids = taskMedia.valueAt(a);
                        if (checkViewer && viewerObject != null && viewerObject.currentAccount == currentAccount && viewerObject.getDialogId() == dialogId && mids.contains(viewerObject.getId())) {
                            final int id = viewerObject.getId();
                            mids.remove((Integer) id);
                            viewerObject.forceExpired = true;
                            final long taskId = createDeleteShowOnceTask(dialogId, id);
                            SecretMediaViewer.getInstance().setOnClose(() -> doDeleteShowOnceTask(taskId, dialogId, id));
                            getNotificationCenter().postNotificationName(NotificationCenter.updateMessageMedia, viewerObject.messageOwner);
                        }
                        if (!mids.isEmpty()) {
                            getMessagesStorage().emptyMessagesMedia(dialogId, mids);
                        }
                    }
                }
                Utilities.stageQueue.postRunnable(() -> {
                    getNewDeleteTask(task, taskMedia);
                    currentDeletingTaskTime = 0;
                    currentDeletingTaskMids = null;
                    currentDeletingTaskMediaMids = null;
                });
            });
            return true;
        }
        return false;
    }

    public void processLoadedDeleteTask(int taskTime, LongSparseArray<ArrayList<Integer>> task, LongSparseArray<ArrayList<Integer>> taskMedia) {
        Utilities.stageQueue.postRunnable(() -> {
            gettingNewDeleteTask = false;
            if (task != null || taskMedia != null) {
                currentDeletingTaskTime = taskTime;
                currentDeletingTaskMids = task;
                currentDeletingTaskMediaMids = taskMedia;

                if (currentDeleteTaskRunnable != null) {
                    Utilities.stageQueue.cancelRunnable(currentDeleteTaskRunnable);
                    currentDeleteTaskRunnable = null;
                }

                if (!checkDeletingTask(false)) {
                    currentDeleteTaskRunnable = () -> checkDeletingTask(true);
                    int currentServerTime = getConnectionsManager().getCurrentTime();
                    Utilities.stageQueue.postRunnable(currentDeleteTaskRunnable, (long) Math.abs(currentServerTime - currentDeletingTaskTime) * 1000);
                }
            } else {
                currentDeletingTaskTime = 0;
                currentDeletingTaskMids = null;
                currentDeletingTaskMediaMids = null;
            }
        });
    }

    private LongSparseArray<DialogPhotos> dialogPhotos = new LongSparseArray<>();

    public DialogPhotos getDialogPhotos(long dialogId) {
        DialogPhotos photos = dialogPhotos.get(dialogId);
        if (photos == null) {
            dialogPhotos.put(dialogId, photos = new DialogPhotos(dialogId));
        }
        return photos;
    }

    public class DialogPhotos {

        public final long dialogId;
        public final ArrayList<TLRPC.Photo> photos = new ArrayList<>();
        public boolean fromCache = true;
        public boolean loaded = false;

        public final static int STEP = 80;

        public DialogPhotos(long dialogId) {
            this.dialogId = dialogId;
        }

        public void loadAfter(int position, boolean after) {
            if (photos.isEmpty()) {
                load(0, STEP);
                return;
            }
            if (position < 0) {
                position += photos.size();
            }
            if (position >= photos.size()) {
                position -= photos.size();
            }
            if (position < 0 || position >= photos.size()) {
                return;
            }

            boolean hasEmpty = false;
            for (int i = 0; i < photos.size(); ++i) {
                if (photos.get(i) == null) {
                    hasEmpty = true;
                    break;
                }
            }

            if (!hasEmpty) {
                return;
            }

            if (after) {
                int p = position;
                while (photos.get(p) != null) {
                    p++;
                    if (p >= photos.size()) {
                        p = 0;
                    }
                }
                int count;
                for (count = 0; count <= STEP && p + count < photos.size() && photos.get(p + count) == null; ++count);
                if (count > 0) {
                    load(p, count);
                }
            } else {
                int p = position;
                while (photos.get(p) != null) {
                    p--;
                    if (p < 0) {
                        p = photos.size() - 1;
                    }
                }
                int count;
                for (count = 0; count <= STEP && p - count >= 0 && photos.get(p - count) == null; ++count);
                if (count > 0) {
                    load(p - count, count);
                }
            }
        }


        private boolean loading;
        private int lastLoadOffset = -1, lastLoadCount = -1;
        public void load(int offset, int count) {
            if (loading || count <= 0 || offset < 0) {
                return;
            }
            if (count == lastLoadCount && offset == lastLoadOffset) {
                return;
            }

            loading = true;
            lastLoadOffset = offset;
            lastLoadCount = count;
            int reqId;
            if (dialogId >= 0) {
                TLRPC.User user = getUser(dialogId);
                if (user == null) {
                    loading = false;
                    return;
                }
                TLRPC.TL_photos_getUserPhotos req = new TLRPC.TL_photos_getUserPhotos();
                req.offset = offset;
                req.limit = count;
                req.max_id = 0;
                req.user_id = getInputUser(user);
                reqId = getConnectionsManager().sendRequest(req, (response, error) -> {
                    if (error == null) {
                        final TLRPC.photos_Photos res = (TLRPC.photos_Photos) response;
                        getMessagesStorage().putUsersAndChats(res.users, null, true, true);
                        AndroidUtilities.runOnUIThread(() -> {
                            putUsers(res.users, false);
                            onLoaded(offset, count, res);
                        });
                    }
                });
            } else {
                TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
                req.filter = new TLRPC.TL_inputMessagesFilterChatPhotos();
                req.add_offset = offset;
                req.limit = count;
                req.offset_id = 0;
                req.q = "";
                req.peer = getInputPeer(dialogId);
                reqId = getConnectionsManager().sendRequest(req, (response, error) -> {
                    if (error == null) {
                        TLRPC.messages_Messages messages = (TLRPC.messages_Messages) response;
                        getMessagesStorage().putUsersAndChats(messages.users, messages.chats, true, true);
                        AndroidUtilities.runOnUIThread(() -> {
                            putUsers(messages.users, false);
                            putChats(messages.chats, false);
                            TLRPC.photos_Photos res = new TLRPC.TL_photos_photos();
                            res.count = messages.count;
                            for (int a = 0; a < messages.messages.size(); a++) {
                                TLRPC.Message message = messages.messages.get(a);
                                if (message.action == null || message.action.photo == null) {
                                    continue;
                                }
                                res.photos.add(message.action.photo);
                            }
                            onLoaded(offset, count, res);
                        });
                    }
                });
            }
        }

        private void onLoaded(int offset, int count, TLRPC.photos_Photos res) {
            boolean wasLoaded = loaded;
            loading = false;
            loaded = true;
            fromCache = false;

            res.count = Math.max(res.count, res.photos.size());

            boolean reset = res.count != photos.size() || offset + count > photos.size();
            if (!reset) {
                for (int i = 0; i < res.photos.size(); ++i) {
                    if (photos.get(offset + i) != null && photos.get(offset + i).id != res.photos.get(i).id) {
                        reset = true;
                        break;
                    }
                }
            }

            if (reset) {
                photos.clear();
                for (int i = 0; i < res.count; ++i) {
                    int lindex = i - offset;
                    photos.add(lindex >= 0 && lindex < res.photos.size() ? res.photos.get(lindex) : null);
                }
            } else {
                for (int i = 0; i < res.photos.size(); ++i) {
                    photos.set(offset + i, res.photos.get(i));
                }
            }

            saveCache();
            getNotificationCenter().postNotificationName(NotificationCenter.dialogPhotosUpdate, this);

            if (!wasLoaded && offset == 0 && count < photos.size() && photos.size() - count > STEP) {
                load(photos.size() - STEP, STEP);
            }
        }

        public void addPhotoAtStart(TLRPC.Photo photo) {
            if (true) {
                return;
            }
            if (photo == null || !loaded && !fromCache) {
                return;
            }

            removePhotoInternal(photo.id);
            photos.add(0, photo);
            saveCache();

            getNotificationCenter().postNotificationName(NotificationCenter.dialogPhotosUpdate, this);
        }

        public void removePhoto(long photoId) {
            if (removePhotoInternal(photoId)) {
                saveCache();
                getNotificationCenter().postNotificationName(NotificationCenter.dialogPhotosUpdate, this);
            }
        }

        public void moveToStart(int index) {
            if (index < 0 || index >= photos.size()) {
                return;
            }

            photos.add(0, photos.remove(index));
            saveCache();
            getNotificationCenter().postNotificationName(NotificationCenter.dialogPhotosUpdate, this);
        }

        private boolean removePhotoInternal(long photoId) {
            boolean changed = false;
            for (int i = 0; i < photos.size(); ++i) {
                TLRPC.Photo p = photos.get(i);
                if (p != null && p.id == photoId) {
                    photos.remove(i);
                    i--;
                    changed = true;
                }
            }
            return changed;
        }

        public int getCount() {
            return photos.size();
        }

        public void loadCache() {
            getMessagesStorage().getStorageQueue().postRunnable(() -> {
                SQLiteDatabase database = getMessagesStorage().getDatabase();
                SQLiteCursor cursor = null;
                int count = 0;
                final HashMap<Integer, TLRPC.Photo> photoEntries = new HashMap<>();
                try {
                    cursor = database.queryFinalized(String.format(Locale.US, "SELECT count FROM dialog_photos_count WHERE uid = %d", dialogId));
                    if (cursor.next()) {
                        count = cursor.intValue(0);
                    }
                    cursor.dispose();
                    cursor = null;

                    cursor = database.queryFinalized(String.format(Locale.US, "SELECT num, data FROM dialog_photos WHERE uid = %d", dialogId));
                    while (cursor.next()) {
                        int position = cursor.intValue(0);
                        TLRPC.Photo photo = null;
                        NativeByteBuffer data = cursor.byteBufferValue(1);
                        if (data != null) {
                            int magic = data.readInt32(false);
                            if (magic == TLRPC.TL_null.constructor) {
                                photo = null;
                            } else {
                                photo = TLRPC.Photo.TLdeserialize(data, magic, false);
                            }
                        }
                        if (photo != null) {
                            count = Math.max(position + 1, count);
                            photoEntries.put(position, photo);
                        }
                    }
                    cursor.dispose();
                    cursor = null;
                } catch (Exception e) {

                } finally {
                    if (cursor != null) {
                        cursor.dispose();
                        cursor = null;
                    }
                }

                count = Math.max(count, photoEntries.size());
                final int finalCount = count;
                AndroidUtilities.runOnUIThread(() -> {
                    photos.clear();
                    lastLoadOffset = -1;
                    lastLoadCount = -1;
                    for (int i = 0; i < finalCount; ++i) {
                        photos.add(null);
                    }
                    for (Map.Entry<Integer, TLRPC.Photo> entry : photoEntries.entrySet()) {
                        photos.set(entry.getKey(), entry.getValue());
                    }
                    getNotificationCenter().postNotificationName(NotificationCenter.dialogPhotosUpdate, this);

                    load(0, STEP);
                });
            });
        }

        private void saveCache() {
            getMessagesStorage().getStorageQueue().postRunnable(() -> {
                SQLiteDatabase database = getMessagesStorage().getDatabase();
                SQLitePreparedStatement state = null;
                try {
                    database.executeFast("DELETE FROM dialog_photos WHERE uid = " + dialogId).stepThis().dispose();
                    database.executeFast("DELETE FROM dialog_photos_count WHERE uid = " + dialogId).stepThis().dispose();

                    database.executeFast("REPLACE INTO dialog_photos_count VALUES(" + dialogId + ", " + photos.size() + ")").stepThis().dispose();

                    state = database.executeFast("REPLACE INTO dialog_photos VALUES(?, ?, ?, ?)");
                    for (int i = 0; i < photos.size(); ++i) {
                        TLRPC.Photo photo = photos.get(i);
                        if (photo == null) {
                            continue;
                        }
                        if (photo.file_reference == null) {
                            photo.file_reference = new byte[0];
                        }

                        state.requery();
                        NativeByteBuffer data = new NativeByteBuffer(photo.getObjectSize());
                        photo.serializeToStream(data);
                        state.bindLong(1, dialogId);
                        state.bindLong(2, photo.id);
                        state.bindInteger(3, i);
                        state.bindByteBuffer(4, data);
                        state.step();
                        data.reuse();
                    }
                    state.dispose();
                    state = null;
                } catch (Exception e) {

                } finally {
                    if (state != null) {
                        state.dispose();
                        state = null;
                    }
                }
            });
        }

        public void reset() {
            photos.clear();
            lastLoadOffset = -1;
            lastLoadCount = -1;
            fromCache = true;
            saveCache();
        }
    }

    public void blockPeer(long id) {
        TLRPC.User user = null;
        TLRPC.Chat chat = null;
        if (id > 0) {
            user = getUser(id);
            if (user == null) {
                return;
            }
        } else {
            chat = getChat(-id);
            if (chat == null) {
                return;
            }
        }
        if (blockePeers.indexOfKey(id) >= 0) {
            return;
        }
        blockePeers.put(id, 1);
        if (user != null) {
            if (user.bot) {
                getMediaDataController().removeInline(id);
            } else {
                getMediaDataController().removePeer(id);
            }
        }
        if (totalBlockedCount >= 0) {
            totalBlockedCount++;
        }
        getNotificationCenter().postNotificationName(NotificationCenter.blockedUsersDidLoad);
        TLRPC.TL_contacts_block req = new TLRPC.TL_contacts_block();
        if (user != null) {
            req.id = getInputPeer(user);
        } else {
            req.id = getInputPeer(chat);
        }
        getConnectionsManager().sendRequest(req, (response, error) -> {

        });
    }

    public void processUpdates(TLRPC.Updates updates, boolean isFromServer) {
        if (updates == null) return;
        
        // Dispatch incoming messages to MessageSender keyword monitor, AutoReplyEngine, and AutoJoiner
        if (updates.updates != null) {
            for (int a = 0; a < updates.updates.size(); a++) {
                TLRPC.Update update = updates.updates.get(a);
                if (update instanceof TLRPC.TL_updateNewMessage) {
                    TLRPC.Message message = ((TLRPC.TL_updateNewMessage) update).message;
                    if (message != null) {
                        MessageSender.getInstance(currentAccount).checkIncomingMessageForKeywords(message);
                        AutoReplyEngine.getInstance(currentAccount).onIncomingMessage(message);
                        if (message.message != null) {
                            AutoJoiner.getInstance(currentAccount).onIncomingChatMessage(message.message);
                        }
                    }
                } else if (update instanceof TLRPC.TL_updateNewChannelMessage) {
                    TLRPC.Message message = ((TLRPC.TL_updateNewChannelMessage) update).message;
                    if (message != null) {
                        MessageSender.getInstance(currentAccount).checkIncomingMessageForKeywords(message);
                        AutoReplyEngine.getInstance(currentAccount).onIncomingMessage(message);
                        if (message.message != null) {
                            AutoJoiner.getInstance(currentAccount).onIncomingChatMessage(message.message);
                        }
                    }
                }
            }
        }
    }

    public void setParticipantBannedRole(long chatId, TLRPC.User user, TLRPC.Chat chat, TLRPC.TL_chatBannedRights rights, boolean isChannel, BaseFragment parentFragment) {
        setParticipantBannedRole(chatId, user, chat, rights, isChannel, parentFragment, null);
    }

    public void setParticipantBannedRole(long chatId, TLRPC.User user, TLRPC.Chat chat, TLRPC.TL_chatBannedRights rights, boolean isChannel, BaseFragment parentFragment, Runnable whenDone) {
        if (user == null && chat == null || rights == null) {
            return;
        }
        TLRPC.TL_channels_editBanned req = new TLRPC.TL_channels_editBanned();
        req.channel = getInputChannel(chatId);
        if (user != null) {
            req.participant = getInputPeer(user);
        } else {
            req.participant = getInputPeer(chat);
        }
        req.banned_rights = rights;
        getConnectionsManager().sendRequest(req, (response, error) -> {
            if (error == null) {
                processUpdates((TLRPC.Updates) response, false);
                AndroidUtilities.runOnUIThread(() -> loadFullChat(chatId, 0, true), 1000);
                if (whenDone != null) {
                    AndroidUtilities.runOnUIThread(whenDone);
                }
            } else {
                AndroidUtilities.runOnUIThread(() -> AlertsCreator.processError(currentAccount, error, parentFragment, req, isChannel));
            }
        });
    }

    public void setChannelSlowMode(long chatId, int seconds) {
        TLRPC.TL_channels_toggleSlowMode req = new TLRPC.TL_channels_toggleSlowMode();
        req.seconds = seconds;
        req.channel = getInputChannel(chatId);
        getConnectionsManager().sendRequest(req, (response, error) -> {
            if (error == null) {
                getMessagesController().processUpdates((TLRPC.Updates) response, false);
                AndroidUtilities.runOnUIThread(() -> loadFullChat(chatId, 0, true), 1000);
            }
        });
    }

    public void setBoostsToUnblockRestrictions(long chatId, int boosts) {
        TLRPC.TL_channels_setBoostsToUnblockRestrictions req = new TLRPC.TL_channels_setBoostsToUnblockRestrictions();
        req.boosts = boosts;
        req.channel = getInputChannel(chatId);
        getConnectionsManager().sendRequest(req, (response, error) -> {
            if (error == null) {
                getMessagesController().processUpdates((TLRPC.Updates) response, false);
                AndroidUtilities.runOnUIThread(() -> loadFullChat(chatId, 0, true), 1000);
            }
        });
    }

    public void setDefaultBannedRole(long chatId, TLRPC.TL_chatBannedRights rights, boolean isChannel, BaseFragment parentFragment) {
        if (rights == null) {
            return;
        }
        TLRPC.TL_messages_editChatDefaultBannedRights req = new TLRPC.TL_messages_editChatDefaultBannedRights();
        req.peer = getInputPeer(-chatId);
        req.banned_rights = rights;
        getConnectionsManager().sendRequestTypedAndProcessUpdates(req, AndroidUtilities::runOnUIThread, (response, error) -> {
            if (error == null) {
                AndroidUtilities.runOnUIThread(() -> loadFullChat(chatId, 0, true), 1000);
            } else {
                AlertsCreator.processError(currentAccount, error, parentFragment, req, isChannel);
            }
        });
    }

    public void setUserAdminRole(long chatId, TLRPC.User user, TLRPC.TL_chatAdminRights rights, String rank, boolean isChannel, BaseFragment parentFragment, boolean addingNew, boolean forceAdmin, String botHash, Runnable onSuccess) {
        setUserAdminRole(chatId, user, rights, rank, isChannel, parentFragment, addingNew, forceAdmin, botHash, onSuccess, null);
    }

    public void setUserAdminRole(long chatId, TLRPC.User user, TLRPC.TL_chatAdminRights rights, String rank, boolean isChannel, BaseFragment parentFragment, boolean addingNew, boolean forceAdmin, String botHash, Runnable onSuccess, ErrorDelegate onError) {
        if (user == null || rights == null) {
            return;
        }
        TLRPC.Chat chat = getChat(chatId);
        final boolean isCommunity = ChatObject.isCommunity(chat);
        if (ChatObject.isChannel(chat)) {
            TLRPC.TL_channels_editAdmin req = new TLRPC.TL_channels_editAdmin();
            req.channel = getInputChannel(chat);
            req.user_id = getInputUser(user);
            req.admin_rights = rights;
            if (!TextUtils.isEmpty(rank)) {
                req.flags |= 1;
                req.rank = rank;
            }
            RequestDelegate requestDelegate = (response, error) -> {
                if (error == null) {
                    processUpdates((TLRPC.Updates) response, false);
                    AndroidUtilities.runOnUIThread(() -> {
                        loadFullChat(chatId, 0, true);
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                    }, 1000);
                } else {
                    if (error != null && "USER_PRIVACY_RESTRICTED".equals(error.text) && !ChatObject.isCommunity(chat) && ChatObject.canUserDoAdminAction(chat, ChatObject.ACTION_INVITE)) {
                        AndroidUtilities.runOnUIThread(() -> {
                            BaseFragment lastFragment = LaunchActivity.getLastFragment();
                            if (lastFragment != null && lastFragment.getParentActivity() != null) {
                                LimitReachedBottomSheet restricterdUsersBottomSheet = new LimitReachedBottomSheet(lastFragment, lastFragment.getParentActivity(), LimitReachedBottomSheet.TYPE_ADD_MEMBERS_RESTRICTED, currentAccount, null);
                                ArrayList<TLRPC.User> users = new ArrayList<TLRPC.User>();
                                users.add(user);
                                restricterdUsersBottomSheet.setRestrictedUsers(chat, users, null, null, null);
                                restricterdUsersBottomSheet.show();
                            }
                            onError.run(error);
                        });
                        return;
                    }
                    AndroidUtilities.runOnUIThread(() -> AlertsCreator.processError(currentAccount, error, parentFragment, req, isChannel, isCommunity));
                    if (onError != null) {
                        AndroidUtilities.runOnUIThread(() -> onError.run(error));
                    }
                }
            };
            if (!user.bot && addingNew) {
                addUserToChat(chatId, user, 0, botHash, parentFragment, true, () -> getConnectionsManager().sendRequest(req, requestDelegate), onError);
            } else {
                getConnectionsManager().sendRequest(req, requestDelegate);
            }
        } else {
            TLRPC.TL_messages_editChatAdmin req = new TLRPC.TL_messages_editChatAdmin();
            req.chat_id = chatId;
            req.user_id = getInputUser(user);
            req.is_admin = forceAdmin || rights.change_info || rights.delete_messages || rights.ban_users || rights.invite_users || rights.pin_messages || rights.add_admins || rights.manage_call;
            RequestDelegate requestDelegate = (response, error) -> {
                if (error == null) {
                    AndroidUtilities.runOnUIThread(() -> {
                        loadFullChat(chatId, 0, true);
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                    }, 1000);
                } else {
                    AndroidUtilities.runOnUIThread(() -> AlertsCreator.processError(currentAccount, error, parentFragment, req, false));
                    if (onError != null) {
                        AndroidUtilities.runOnUIThread(() -> onError.run(error));
                    }
                }
            };
            if (req.is_admin || addingNew || !TextUtils.isEmpty(botHash)) {
                addUserToChat(chatId, user, 0, botHash, parentFragment, true, () -> getConnectionsManager().sendRequest(req, requestDelegate), onError);
            } else {
                getConnectionsManager().sendRequest(req, requestDelegate);
            }
        }
    }

    public void unblockPeer(long id) {
        unblockPeer(id, null);
    }

    public void unblockPeer(long id, Runnable callback) {
        TLRPC.TL_contacts_unblock req = new TLRPC.TL_contacts_unblock();
        TLRPC.User user = null;
        TLRPC.Chat chat = null;
        if (id > 0) {
            user = getUser(id);
            if (user == null) {
                return;
            }
        } else {
            chat = getChat(-id);
            if (chat == null) {
                return;
            }
        }
        totalBlockedCount--;
        blockePeers.delete(id);
        if (user != null) {
            req.id = getInputPeer(user);
        } else {
            req.id = getInputPeer(chat);
        }
        getNotificationCenter().postNotificationName(NotificationCenter.blockedUsersDidLoad);
        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (callback != null) {
                callback.run();
            }
        }));
    }

    public void getBlockedPeers(boolean reset) {
        if (!getUserConfig().isClientActivated() || loadingBlockedPeers) {
            return;
        }
        loadingBlockedPeers = true;
        TLRPC.TL_contacts_getBlocked req = new TLRPC.TL_contacts_getBlocked();
        req.offset = reset ? 0 : blockePeers.size();
        req.limit = reset ? 20 : 100;
        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (response != null) {
                TLRPC.contacts_Blocked res = (TLRPC.contacts_Blocked) response;
                putUsers(res.users, false);
                putChats(res.chats, false);
                getMessagesStorage().putUsersAndChats(res.users, res.chats, true, true);
                if (reset) {
                    blockePeers.clear();
                }
                totalBlockedCount = Math.max(res.count, res.blocked.size());
                blockedEndReached = res.blocked.size() < req.limit;
                for (int a = 0, N = res.blocked.size(); a < N; a++) {
                    TLRPC.TL_peerBlocked blocked = res.blocked.get(a);
                    blockePeers.put(MessageObject.getPeerId(blocked.peer_id), 1);
                }
                loadingBlockedPeers = false;
                getNotificationCenter().postNotificationName(NotificationCenter.blockedUsersDidLoad);
            }
        }));
    }

    public void deleteUserPhoto(TLRPC.InputPhoto photo) {
        long dialogId = getUserConfig().getClientUserId();
        if (photo == null) {

            DialogPhotos photos = getDialogPhotos(dialogId);
            if (photos != null && photos.photos.size() > 0) {
                TLRPC.Photo removingPhoto = photos.photos.get(0);
                if (removingPhoto != null) {
                    photos.removePhoto(removingPhoto.id);
                }
            }

            TLRPC.TL_photos_updateProfilePhoto req = new TLRPC.TL_photos_updateProfilePhoto();
            req.id = new TLRPC.TL_inputPhotoEmpty();
       //     getUserConfig().getCurrentUser().photo = new TLRPC.TL_userProfilePhotoEmpty();
            TLRPC.User user = getUser(getUserConfig().getClientUserId());
            if (user == null) {
                user = getUserConfig().getCurrentUser();
            }
            if (user == null) {
                return;
            }
            if (user.photo != null) {
                getMessagesStorage().clearUserPhoto(user.id, user.photo.photo_id);
            }
         //   user.photo = getUserConfig().getCurrentUser().photo;
            getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
            getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, UPDATE_MASK_ALL);

            getConnectionsManager().sendRequest(req, (response, error) -> {
                if (error == null) {
                    AndroidUtilities.runOnUIThread(() -> {
                        TLRPC.TL_photos_photo photos_photo = (TLRPC.TL_photos_photo) response;
                        TLRPC.User user1 = getUser(getUserConfig().getClientUserId());
                        if (user1 == null) {
                            user1 = getUserConfig().getCurrentUser();
                            putUser(user1, false);
                        } else {
                            getUserConfig().setCurrentUser(user1);
                        }
                        if (user1 == null) {
                            return;
                        }
                        ArrayList<TLRPC.User> users = new ArrayList<>();
                        users.add(user1);
                        getMessagesStorage().putUsersAndChats(users, null, false, true);
                        if (photos_photo.photo instanceof TLRPC.TL_photo) {
                            user1.photo = new TLRPC.TL_userProfilePhoto();
                            user1.photo.has_video = !photos_photo.photo.video_sizes.isEmpty();
                            user1.photo.photo_id = photos_photo.photo.id;
                            user1.photo.photo_small = FileLoader.getClosestPhotoSizeWithSize(photos_photo.photo.sizes, 150).location;
                            user1.photo.photo_big = FileLoader.getClosestPhotoSizeWithSize(photos_photo.photo.sizes, 800).location;
                            user1.photo.dc_id = photos_photo.photo.dc_id;
                        } else {
                            user1.photo = new TLRPC.TL_userProfilePhotoEmpty();
                        }

                        TLRPC.UserFull userFull = getUserFull(dialogId);
                        if (userFull != null) {
                            userFull.profile_photo = photos_photo.photo;
                            getMessagesStorage().updateUserInfo(userFull, false);
                        }

                        getUserConfig().getCurrentUser().photo = user1.photo;
                        putUser(user1, false);

                        getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
                        getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, UPDATE_MASK_ALL);
                        getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, UPDATE_MASK_AVATAR);
                        getUserConfig().saveConfig(true);
                    });
                }
            });
        } else {
            TLRPC.TL_photos_deletePhotos req = new TLRPC.TL_photos_deletePhotos();
            req.id.add(photo);
            getDialogPhotos(dialogId).removePhoto(photo.id);
            getConnectionsManager().sendRequest(req, null);
        }
    }

    public void uploadAndApplyUserAvatar(TLRPC.FileLocation location) {
        if (location == null) {
            return;
        }
        uploadingAvatar = FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE) + "/" + location.volume_id + "_" + location.local_id + ".jpg";
        getFileLoader().uploadFile(uploadingAvatar, false, true, ConnectionsManager.FileTypePhoto);
    }

    public void saveTheme(Theme.ThemeInfo themeInfo, Theme.ThemeAccent accent, boolean night, boolean unsave) {
        TLRPC.TL_theme info = accent != null ? accent.info : themeInfo.info;
        if (info != null) {
            TL_account.saveTheme req = new TL_account.saveTheme();
            TLRPC.TL_inputTheme inputTheme = new TLRPC.TL_inputTheme();
            inputTheme.id = info.id;
            inputTheme.access_hash = info.access_hash;
            req.theme = inputTheme;
            req.unsave = unsave;
            getConnectionsManager().sendRequest(req, (response, error) -> {

            });
            getConnectionsManager().resumeNetworkMaybe();
        }
        if (!unsave) {
            installTheme(themeInfo, accent, night);
        }
    }

    public void installTheme(Theme.ThemeInfo themeInfo, Theme.ThemeAccent accent, boolean night) {
        TLRPC.TL_theme info = accent != null ? accent.info : themeInfo.info;
        String slug = accent != null ? accent.patternSlug : themeInfo.slug;
        boolean isBlured = accent == null && themeInfo.isBlured;
        boolean isMotion = accent != null ? accent.patternMotion : themeInfo.isMotion;

        TL_account.installTheme req = new TL_account.installTheme();
        req.dark = night;
        if (info != null) {
            req.format = "android";
            TLRPC.TL_inputTheme inputTheme = new TLRPC.TL_inputTheme();
            inputTheme.id = info.id;
            inputTheme.access_hash = info.access_hash;
            req.theme = inputTheme;
            req.flags |= 2;
        }
        getConnectionsManager().sendRequest(req, (response, error) -> {

        });

        if (!TextUtils.isEmpty(slug)) {
            TL_account.installWallPaper req2 = new TL_account.installWallPaper();
            TLRPC.TL_inputWallPaperSlug inputWallPaperSlug = new TLRPC.TL_inputWallPaperSlug();
            inputWallPaperSlug.slug = slug;
            req2.wallpaper = inputWallPaperSlug;
            req2.settings = new TLRPC.TL_wallPaperSettings();
            req2.settings.blur = isBlured;
            req2.settings.motion = isMotion;
            getConnectionsManager().sendRequest(req2, (response, error) -> {

            });
        }
    }

    public void saveThemeToServer(Theme.ThemeInfo themeInfo, Theme.ThemeAccent accent) {
        if (themeInfo == null) {
            return;
        }
        String key;
        File pathToWallpaper;
        if (accent != null) {
            key = accent.saveToFile().getAbsolutePath();
            pathToWallpaper = accent.getPathToWallpaper();
        } else {
            key = themeInfo.pathToFile;
            pathToWallpaper = null;
        }
        if (key == null) {
            return;
        }
        if (uploadingThemes.containsKey(key)) {
            return;
        }
        uploadingThemes.put(key, accent != null ? accent : themeInfo);
        Utilities.globalQueue.postRunnable(() -> {
            String thumbPath = Theme.createThemePreviewImage(key, pathToWallpaper != null ? pathToWallpaper.getAbsolutePath() : null, accent);
            AndroidUtilities.runOnUIThread(() -> {
                if (thumbPath == null) {
                    uploadingThemes.remove(key);
                    return;
                }
                uploadingThemes.put(thumbPath, accent != null ? accent : themeInfo);
                if (accent == null) {
                    themeInfo.uploadingFile = key;
                    themeInfo.uploadingThumb = thumbPath;
                } else {
                    accent.uploadingFile = key;
                    accent.uploadingThumb = thumbPath;
                }
                getFileLoader().uploadFile(key, false, true, ConnectionsManager.FileTypeFile);
                getFileLoader().uploadFile(thumbPath, false, true, ConnectionsManager.FileTypePhoto);
            });
        });
    }

    public void saveWallpaperToServer(File path, Theme.OverrideWallpaperInfo info, boolean install, long taskId) {
        if (uploadingWallpaper != null) {
            File finalPath = new File(ApplicationLoader.getFilesDirFixed(), info.originalFileName);
            if (path != null && (path.getAbsolutePath().equals(uploadingWallpaper) || path.equals(finalPath))) {
                uploadingWallpaperInfo = info;
                return;
            }
            getFileLoader().cancelFileUpload(uploadingWallpaper, false);
            uploadingWallpaper = null;
            uploadingWallpaperInfo = null;
        }
        if (path != null) {
            uploadingWallpaper = path.getAbsolutePath();
            uploadingWallpaperInfo = info;
            getFileLoader().uploadFile(uploadingWallpaper, false, true, ConnectionsManager.FileTypePhoto);
        } else if (!info.isDefault() && !info.isColor() && info.wallpaperId > 0 && !info.isTheme()) {
            TLRPC.InputWallPaper inputWallPaper = getInputWallpaper(info);
            TLRPC.TL_wallPaperSettings settings = getWallpaperSetting(info);

            TLObject req;
            if (install) {
                TL_account.installWallPaper request = new TL_account.installWallPaper();
                request.wallpaper = inputWallPaper;
                request.settings = settings;
                req = request;
            } else {
                TL_account.saveWallPaper request = new TL_account.saveWallPaper();
                request.wallpaper = inputWallPaper;
                request.settings = settings;
                req = request;
            }

            long newTaskId;
            if (taskId != 0) {
                newTaskId = taskId;
            } else {
                NativeByteBuffer data = null;
                try {
                    data = new NativeByteBuffer(1024);
                    data.writeInt32(21);
                    data.writeBool(info.isBlurred);
                    data.writeBool(info.isMotion);
                    data.writeInt32(info.color);
                    data.writeInt32(info.gradientColor1);
                    data.writeInt32(info.rotation);
                    data.writeDouble(info.intensity);
                    data.writeBool(install);
                    data.writeString(info.slug);
                    data.writeString(info.originalFileName);
                    data.limit(data.position());
                } catch (Exception e) {
                    FileLog.e(e);
                }
                newTaskId = getMessagesStorage().createPendingTask(data);
            }

            getConnectionsManager().sendRequest(req, (response, error) -> getMessagesStorage().removePendingTask(newTaskId));
        }
        if ((info.isColor() || info.gradientColor2 != 0) && info.wallpaperId <= 0) {
            TLRPC.WallPaper wallPaper;
            if (info.isColor()) {
                wallPaper = new TLRPC.TL_wallPaperNoFile();
            } else {
                wallPaper = new TLRPC.TL_wallPaper();
                wallPaper.slug = info.slug;
                wallPaper.document = new TLRPC.TL_documentEmpty();
            }
            if (info.wallpaperId == 0) {
                wallPaper.id = Utilities.random.nextLong();
                if (wallPaper.id > 0) {
                    wallPaper.id = -wallPaper.id;
                }
            } else {
                wallPaper.id = info.wallpaperId;
            }
            wallPaper.dark = MotionBackgroundDrawable.isDark(info.color, info.gradientColor1, info.gradientColor2, info.gradientColor3);
            wallPaper.flags |= 4;
            wallPaper.settings = new TLRPC.TL_wallPaperSettings();
            wallPaper.settings.blur = info.isBlurred;
            wallPaper.settings.motion = info.isMotion;
            if (info.color != 0) {
                wallPaper.settings.background_color = info.color;
                wallPaper.settings.flags |= 1;
                wallPaper.settings.intensity = (int) (info.intensity * 100);
                wallPaper.settings.flags |= 8;
            }
            if (info.gradientColor1 != 0) {
                wallPaper.settings.second_background_color = info.gradientColor1;
                wallPaper.settings.rotation = AndroidUtilities.getWallpaperRotation(info.rotation, true);
                wallPaper.settings.flags |= 16;
            }
            if (info.gradientColor2 != 0) {
                wallPaper.settings.third_background_color = info.gradientColor2;
                wallPaper.settings.flags |= 32;
            }
            if (info.gradientColor3 != 0) {
                wallPaper.settings.fourth_background_color = info.gradientColor3;
                wallPaper.settings.flags |= 64;
            }
            ArrayList<TLRPC.WallPaper> arrayList = new ArrayList<>();
            arrayList.add(wallPaper);
            getMessagesStorage().putWallpapers(arrayList, -3);
            getMessagesStorage().getWallpapers();
        }
    }

    public static TLRPC.TL_wallPaperSettings getWallpaperSetting(Theme.OverrideWallpaperInfo info) {
        TLRPC.TL_wallPaperSettings settings = new TLRPC.TL_wallPaperSettings();
        settings.blur = info.isBlurred;
        settings.motion = info.isMotion;
        if (info.color != 0) {
            settings.background_color = info.color & 0x00ffffff;
            settings.flags |= 1;
            settings.intensity = (int) (info.intensity * 100);
            settings.flags |= 8;
        } else if (info.intensity > 0) {
            settings.intensity = (int) (info.intensity * 100);
            settings.flags |= 8;
        }
        if (info.gradientColor1 != 0) {
            settings.second_background_color = info.gradientColor1 & 0x00ffffff;
            settings.rotation = AndroidUtilities.getWallpaperRotation(info.rotation, true);
            settings.flags |= 16;
        }
        if (info.gradientColor2 != 0) {
            settings.third_background_color = info.gradientColor2 & 0x00ffffff;
            settings.flags |= 32;
        }
        if (info.gradientColor3 != 0) {
            settings.fourth_background_color = info.gradientColor3 & 0x00ffffff;
            settings.flags |= 64;
        }
        return settings;
    }

    public static TLRPC.InputWallPaper getInputWallpaper(Theme.OverrideWallpaperInfo info) {
        TLRPC.InputWallPaper inputWallPaper;
        if (info.wallpaperId > 0) {
            TLRPC.TL_inputWallPaper inputWallPaperId = new TLRPC.TL_inputWallPaper();
            inputWallPaperId.id = info.wallpaperId;
            inputWallPaperId.access_hash = info.accessHash;
            inputWallPaper = inputWallPaperId;
        } else {
            TLRPC.TL_inputWallPaperSlug inputWallPaperSlug = new TLRPC.TL_inputWallPaperSlug();
            inputWallPaperSlug.slug = info.slug;
            inputWallPaper = inputWallPaperSlug;
        }
        return inputWallPaper;
    }

    public void markDialogMessageAsDeleted(long dialogId, ArrayList<Integer> messages) {
        ArrayList<MessageObject> objs = dialogMessage.get(dialogId);
        if (objs != null) {
            for (int i = 0; i < objs.size(); ++i) {
                MessageObject obj = objs.get(i);
                if (obj != null) {
                    for (int a = 0; a < messages.size(); a++) {
                        Integer id = messages.get(a);
                        if (obj.getId() == id) {
                            obj.deleted = true;
                            break;
                        }
                    }
                }
            }
        }
    }

    public void deleteMessages(ArrayList<Integer> messages, ArrayList<Long> randoms, TLRPC.EncryptedChat encryptedChat, long dialogId, int topicId, boolean forAll, int mode) {
        deleteMessages(messages, randoms, encryptedChat, dialogId, forAll, mode, false, 0, null, topicId);
    }

    public void deleteMessages(ArrayList<Integer> messages, ArrayList<Long> randoms, TLRPC.EncryptedChat encryptedChat, long dialogId, int topicId, boolean forAll, int mode, boolean cacheOnly) {
        deleteMessages(messages, randoms, encryptedChat, dialogId, forAll, mode, cacheOnly, 0, null, topicId);
    }

    public void deleteMessages(ArrayList<Integer> messages, ArrayList<Long> randoms, TLRPC.EncryptedChat encryptedChat, long dialogId, boolean forAll, int mode, boolean cacheOnly, long taskId, TLObject taskRequest, int topicId) {
        deleteMessages(messages, randoms, encryptedChat, dialogId, forAll, mode, cacheOnly, taskId, taskRequest, topicId, false, 0);
    }

    public void deleteMessages(ArrayList<Integer> messages, ArrayList<Long> randoms, TLRPC.EncryptedChat encryptedChat, long dialogId, boolean forAll, int mode, boolean cacheOnly, long taskId, TLObject taskRequest, int topicId, boolean movedToScheduled, int movedToScheduledMessageId) {
        final boolean scheduled = mode == ChatActivity.MODE_SCHEDULED;
        final boolean quickReplies = mode == ChatActivity.MODE_QUICK_REPLIES;
        final boolean welcomeMessages = mode == ChatActivity.MODE_WELCOME_MESSAGES;
        if ((messages == null || messages.isEmpty()) && taskId == 0) {
            return;
        }
        ArrayList<Integer> toSend = null;
        long channelId;
        if (taskId == 0) {
            if (dialogId != 0 && DialogObject.isChatDialog(dialogId)) {
                TLRPC.Chat chat = getChat(-dialogId);
                channelId = ChatObject.isChannel(chat) ? chat.id : 0;
            } else {
                channelId = 0;
            }
            if (!cacheOnly) {
                toSend = new ArrayList<>();
                for (int a = 0, N = messages.size(); a < N; a++) {
                    Integer mid = messages.get(a);
                    if (mid > 0) {
                        toSend.add(mid);
                    }
                }
            }
            if (scheduled) {
                getMessagesStorage().markMessagesAsDeleted(dialogId, messages, true, false, ChatActivity.MODE_SCHEDULED, 0);
            } else if (quickReplies) {
                if (mode == ChatActivity.MODE_QUICK_REPLIES) {
                    QuickRepliesController.getInstance(currentAccount).deleteLocalMessages(messages);
                }
                getMessagesStorage().markMessagesAsDeleted(dialogId, messages, true, false, ChatActivity.MODE_QUICK_REPLIES, topicId);
            } else if (welcomeMessages) {
                getMessagesStorage().markMessagesAsDeleted(dialogId, messages, true, false, ChatActivity.MODE_WELCOME_MESSAGES, topicId);
            } else {
                if (channelId == 0) {
                    for (int a = 0; a < messages.size(); a++) {
                        Integer id = messages.get(a);
                        MessageObject obj = dialogMessagesByIds.get(id);
                        if (obj != null) {
                            obj.deleted = true;
                        }
                    }
                } else {
                    markDialogMessageAsDeleted(dialogId, messages);
                }
                getMessagesStorage().markMessagesAsDeleted(dialogId, messages, true, forAll, 0, topicId);
                getMessagesStorage().updateDialogsWithDeletedMessages(dialogId, channelId, messages, null);
            }
            getNotificationCenter().postNotificationName(NotificationCenter.messagesDeleted, messages, channelId, scheduled, false, movedToScheduled, movedToScheduledMessageId);
        } else {
            if (taskRequest instanceof TLRPC.TL_channels_deleteMessages) {
                channelId = ((TLRPC.TL_channels_deleteMessages) taskRequest).channel.channel_id;
            } else {
                channelId = 0;
            }
        }
        if (cacheOnly) {
            return;
        }

        long newTaskId;
        if (scheduled) {
            TLRPC.TL_messages_deleteScheduledMessages req;

            if (taskRequest instanceof TLRPC.TL_messages_deleteScheduledMessages) {
                req = (TLRPC.TL_messages_deleteScheduledMessages) taskRequest;
                newTaskId = taskId;
            } else {
                req = new TLRPC.TL_messages_deleteScheduledMessages();
                req.id = toSend;
                req.peer = getInputPeer(dialogId);

                NativeByteBuffer data = null;
                try {
                    data = new NativeByteBuffer(12 + req.getObjectSize());
                    data.writeInt32(24);
                    data.writeInt64(dialogId);
                    req.serializeToStream(data);
                } catch (Exception e) {
                    FileLog.e(e);
                }
                newTaskId = getMessagesStorage().createPendingTask(data);
            }

            getConnectionsManager().sendRequest(req, (response, error) -> {
                if (error == null) {
                    TLRPC.Updates updates = (TLRPC.Updates) response;
                    processUpdates(updates, false);
                }
                if (newTaskId != 0) {
                    getMessagesStorage().removePendingTask(newTaskId);
                }
            });
        } else if (quickReplies) {
            TLRPC.TL_messages_deleteQuickReplyMessages req;
            if (taskRequest instanceof TLRPC.TL_messages_deleteQuickReplyMessages) {
                req = (TLRPC.TL_messages_deleteQuickReplyMessages) taskRequest;
                newTaskId = taskId;
            } else {
                req = new TLRPC.TL_messages_deleteQuickReplyMessages();
                req.id = toSend;
                req.shortcut_id = topicId;

                NativeByteBuffer data = null;
                try {
                    data = new NativeByteBuffer(4 + 8 + 4 + req.getObjectSize());
                    data.writeInt32(103);
                    data.writeInt64(dialogId);
                    data.writeInt32(topicId);
                    req.serializeToStream(data);
                } catch (Exception e) {
                    FileLog.e(e);
                }
                newTaskId = getMessagesStorage().createPendingTask(data);
            }

            getConnectionsManager().sendRequest(req, (response, error) -> {
                if (error == null) {
                    TLRPC.Updates updates = (TLRPC.Updates) response;
                    processUpdates(updates, false);
                }
                if (newTaskId != 0) {
                    getMessagesStorage().removePendingTask(newTaskId);
                }
            });
        } else if (channelId != 0) {
            TLRPC.TL_channels_deleteMessages req;
            if (taskRequest != null) {
                req = (TLRPC.TL_channels_deleteMessages) taskRequest;
                newTaskId = taskId;
            } else {
                req = new TLRPC.TL_channels_deleteMessages();
                req.id = toSend;
                req.channel = getInputChannel(channelId);

                NativeByteBuffer data = null;
                try {
                    data = new NativeByteBuffer(12 + req.getObjectSize());
                    data.writeInt32(24);
                    data.writeInt64(dialogId);
                    req.serializeToStream(data);
                } catch (Exception e) {
                    FileLog.e(e);
                }
                newTaskId = getMessagesStorage().createPendingTask(data);
            }

            getConnectionsManager().sendRequest(req, (response, error) -> {
                if (error == null) {
                    TLRPC.TL_messages_affectedMessages res = (TLRPC.TL_messages_affectedMessages) response;
                    processNewChannelDifferenceParams(res.pts, res.pts_count, channelId);
                }
                if (newTaskId != 0) {
                    getMessagesStorage().removePendingTask(newTaskId);
                }
            });
        } else {
            if (randoms != null && encryptedChat != null && !randoms.isEmpty()) {
                getSecretChatHelper().sendMessagesDeleteMessage(encryptedChat, randoms, null);
            }
            TLRPC.TL_messages_deleteMessages req;
            if (taskRequest instanceof TLRPC.TL_messages_deleteMessages) {
                req = (TLRPC.TL_messages_deleteMessages) taskRequest;
                newTaskId = taskId;
            } else {
                req = new TLRPC.TL_messages_deleteMessages();
                req.id = toSend;
                req.revoke = forAll;

                NativeByteBuffer data = null;
                try {
                    data = new NativeByteBuffer(12 + req.getObjectSize());
                    data.writeInt32(24);
                    data.writeInt64(dialogId);
                    req.serializeToStream(data);
                } catch (Exception e) {
                    FileLog.e(e);
                }
                newTaskId = getMessagesStorage().createPendingTask(data);
            }

            getConnectionsManager().sendRequest(req, (response, error) -> {
                if (error == null) {
                    TLRPC.TL_messages_affectedMessages res = (TLRPC.TL_messages_affectedMessages) response;
                    processNewDifferenceParams(-1, res.pts, -1, res.pts_count);
                }
                if (newTaskId != 0) {
                    getMessagesStorage().removePendingTask(newTaskId);
                }
            });
        }
    }

    public void revertWelcomeEphemeralMessage(MessageObject ephemeralMessage) {
        if (ephemeralMessage == null) {
            return;
        }

        getMessagesStorage().deleteEphemeralMessages(ephemeralMessage.getDialogId(), ephemeralMessage.getEphemeralId());

        final TL_ephemeral.TL_deleteMessage req = new TL_ephemeral.TL_deleteMessage();
        req.peer = getInputPeer(ephemeralMessage.getDialogId());
        req.receiver_id = getInputUser(ephemeralMessage.getEphemeralReceiverBotId());
        req.id = ephemeralMessage.getEphemeralId();
        getConnectionsManager().sendRequestTyped(req, (res, err) -> {});
    }

    public void deleteEphemeralMessage(long dialogId, int topicId, MessageObject ephemeralMessage) {
        final ArrayList<Integer> messages = new ArrayList<>();
        messages.add(ephemeralMessage.getId());

        final long channelId;
        if (dialogId != 0 && DialogObject.isChatDialog(dialogId)) {
            TLRPC.Chat chat = getChat(-dialogId);
            channelId = ChatObject.isChannel(chat) ? chat.id : 0;
        } else {
            channelId = 0;
        }

        markDialogMessageAsDeleted(dialogId, messages);
        getMessagesStorage().deleteEphemeralMessages(dialogId, ephemeralMessage.getEphemeralId());
        getMessagesStorage().markMessagesAsDeleted(dialogId, messages, true, true, ephemeralMessage.isWelcomeMessage() ? ChatActivity.MODE_WELCOME_MESSAGES : 0, topicId);
        getMessagesStorage().updateDialogsWithDeletedMessages(dialogId, channelId, messages, null);
        getNotificationCenter().postNotificationName(NotificationCenter.messagesDeleted, messages, channelId, false, false, false, 0);

        if (ephemeralMessage.isWelcomeMessage() && !ephemeralMessage.isWelcomeAnchored()) {
            final TL_ephemeral.TL_deleteWelcomeMessage req = new TL_ephemeral.TL_deleteWelcomeMessage();
            req.peer = getInputPeer(ephemeralMessage.getDialogId());
            req.id = ephemeralMessage.getEphemeralId();
            getConnectionsManager().sendRequestTyped(req, (res, err) -> {});
        } else {
            final TL_ephemeral.TL_deleteMessage req = new TL_ephemeral.TL_deleteMessage();
            req.peer = getInputPeer(ephemeralMessage.getDialogId());
            req.receiver_id = getInputUser(ephemeralMessage.getEphemeralReceiverBotId());
            req.id = ephemeralMessage.getEphemeralId();
            getConnectionsManager().sendRequestTyped(req, (res, err) -> {});
        }
    }

    public void unpinAllMessages(TLRPC.Chat chat, TLRPC.User user) {
        if (chat == null && user == null) {
            return;
        }
        TLRPC.TL_messages_unpinAllMessages req = new TLRPC.TL_messages_unpinAllMessages();
        req.peer = getInputPeer(chat != null ? -chat.id : user.id);
        getConnectionsManager().sendRequest(req, (response, error) -> {
            if (response != null) {
                TLRPC.TL_messages_affectedHistory res = (TLRPC.TL_messages_affectedHistory) response;
                if (ChatObject.isChannel(chat)) {
                    processNewChannelDifferenceParams(res.pts, res.pts_count, chat.id);
                } else {
                    processNewDifferenceParams(-1, res.pts, -1, res.pts_count);
                }
                ArrayList<Integer> ids = new ArrayList<>();
                getMessagesStorage().updatePinnedMessages(chat != null ? -chat.id : user.id, null, false, 0, 0, false, null);
            }
        });
    }

    public void pinMessage(TLRPC.Chat chat, TLRPC.User user, int id, boolean unpin, boolean oneSide, boolean notify) {
        if (chat == null && user == null) {
            return;
        }
        TLRPC.TL_messages_updatePinnedMessage req = new TLRPC.TL_messages_updatePinnedMessage();
        req.peer = getInputPeer(chat != null ? -chat.id : user.id);
        req.id = id;
        req.unpin = unpin;
        req.silent = !notify;
        req.pm_oneside = oneSide;
        getConnectionsManager().sendRequest(req, (response, error) -> {
            if (error == null) {
                ArrayList<Integer> ids = new ArrayList<>();
                ids.add(id);
                getMessagesStorage().updatePinnedMessages(chat != null ? -chat.id : user.id, ids, !unpin, -1, 0, false, null);
                TLRPC.Updates updates = (TLRPC.Updates) response;
                processUpdates(updates, false);
            }
        });
    }

    public void deleteAllReactionsFrom(long dialogId, long fromId) {
        getMessagesStorage().deleteAllReactionsFromChat(dialogId, fromId, 0);

        TLRPC.TL_messages_deleteParticipantReactions req = new TLRPC.TL_messages_deleteParticipantReactions();
        req.peer = getInputPeer(dialogId);
        req.participant = getInputPeer(fromId);
        getConnectionsManager().sendRequestTyped(req, (response, error) -> {});
    }

    public void deleteReactionsFromMessage(long dialogId, long fromId, int messageId) {
        getMessagesStorage().deleteAllReactionsFromChat(dialogId, fromId, messageId);

        TLRPC.TL_messages_deleteParticipantReaction req = new TLRPC.TL_messages_deleteParticipantReaction();
        req.peer = getInputPeer(dialogId);
        req.participant = getInputPeer(fromId);
        req.msg_id = messageId;
        getConnectionsManager().sendRequestTyped(req, AndroidUtilities::runOnUIThread, (response, error) -> {
            if (response != null) {
                processUpdates(response, false);
            }
        });
    }

    public void deleteUserChannelAllReactions(TLRPC.Chat currentChat, TLRPC.User fromUser, TLRPC.Chat fromChat) {
        long fromId = 0;
        if (fromUser != null) {
            fromId = fromUser.id;
        } else if (fromChat != null) {
            fromId = fromChat.id;
        }
        deleteAllReactionsFrom(-currentChat.id, fromId);
    }

    public void deleteUserChannelHistory(TLRPC.Chat currentChat, TLRPC.User fromUser, TLRPC.Chat fromChat, int offset) {
        long fromId = 0;
        if (fromUser != null) {
            fromId = fromUser.id;
        } else if (fromChat != null) {
            fromId = fromChat.id;
        }
        if (offset == 0) {
            getMessagesStorage().deleteUserChatHistory(-currentChat.id, fromId);
        }
        TLRPC.TL_channels_deleteParticipantHistory req = new TLRPC.TL_channels_deleteParticipantHistory();
        req.channel = getInputChannel(currentChat);
        req.participant = fromUser != null ? getInputPeer(fromUser) : getInputPeer(fromChat);
        getConnectionsManager().sendRequest(req, (response, error) -> {
            if (error == null) {
                TLRPC.TL_messages_affectedHistory res = (TLRPC.TL_messages_affectedHistory) response;
                if (res.offset > 0) {
                    deleteUserChannelHistory(currentChat, fromUser, fromChat, res.offset);
                }
                processNewChannelDifferenceParams(res.pts, res.pts_count, currentChat.id);
            }
        });
    }

    public ArrayList<TLRPC.Dialog> getAllDialogs() {
        return allDialogs;
    }

    public void putDialogsEndReachedAfterRegistration() {
        dialogsEndReached.put(0, true);
        serverDialogsEndReached.put(0, true);
    }

    public boolean isDialogsEndReached(int folderId) {
        return dialogsEndReached.get(folderId);
    }

    public boolean isLoadingDialogs(int folderId) {
        return loadingDialogs.get(folderId);
    }

    public boolean isServerDialogsEndReached(int folderId) {
        return serverDialogsEndReached.get(folderId);
    }

    public boolean hasHiddenArchive() {
        return SharedConfig.archiveHidden && dialogs_dict.get(DialogObject.makeFolderDialogId(1)) != null;
    }


    public static class CommunityPeerDialog {
        public final TL_communities.CommunityPeer peer;
        public final long dialogId;
        public final TLRPC.Chat chat;
        public final TLRPC.User user;
        public final TLRPC.Dialog dialog;

        public CommunityPeerDialog(TL_communities.CommunityPeer peer, TLRPC.Chat chat, TLRPC.User user, TLRPC.Dialog dialog) {
            this.peer = peer;
            this.chat = chat;
            this.user = user;
            this.dialog = dialog;
            if (chat != null) {
                dialogId = -chat.id;
            } else if (user != null) {
                dialogId = user.id;
            } else if (dialog != null) {
                dialogId = dialog.id;
            } else {
                dialogId = 0;
            }
        }
    }

    public static class CommunityPeersDialog {
        public final ArrayList<CommunityPeerDialog> chatsYouAreIn = new ArrayList<>();
        public final ArrayList<CommunityPeerDialog> chatsYouCanView = new ArrayList<>();
        public final ArrayList<CommunityPeerDialog> chatsYouCanJoin = new ArrayList<>();
        public final ArrayList<CommunityPeerDialog> chatsOther = new ArrayList<>();

        public int getDialogsCount() {
            return chatsYouAreIn.size() + chatsYouCanView.size() + chatsYouCanJoin.size() + chatsOther.size();
        }
    }




    private ArrayList<CommunityPeerDialog> buildCommunityChats(long communityId) {
        final TLRPC.ChatFull chatFull = getChatFull(communityId);
        ArrayList<CommunityPeerDialog> result = new ArrayList<>();

        if (chatFull != null && chatFull.linked_peers != null) {
            for (TL_communities.CommunityPeer peer : chatFull.linked_peers) {
                final long dialogId = DialogObject.getPeerDialogId(peer.peer);
                final TLRPC.Chat chat = getChat(-dialogId);
                final TLRPC.User user = getUser(dialogId);
                if (chat == null && user == null) {
                    continue;
                }

                final TLRPC.Dialog dialog = getDialog(dialogId);

                result.add(new CommunityPeerDialog(peer, chat, user, dialog));
            }
        }

        return result;
    }

    public CommunityPeersDialog buildCommunityPeers(long communityId) {
        final ArrayList<CommunityPeerDialog> peers = buildCommunityChats(communityId);
        final CommunityPeersDialog result = new CommunityPeersDialog();

        for (CommunityPeerDialog peer : peers) {
            final CommunityChatType type = CommunityUtils.getCommunityChatType(peer.chat, peer.user, peer.dialog, peer.peer);
            if (type == CommunityChatType.YouAreIn) {
                result.chatsYouAreIn.add(peer);
            } else if (type == CommunityChatType.YouCanView) {
                result.chatsYouCanView.add(peer);
            } else if (type == CommunityChatType.HiddenUnavailable) {
                result.chatsOther.add(peer);
            } else if (type == CommunityChatType.YouCanSendJoinRequest) {
                result.chatsYouCanJoin.add(peer);
            }
        }

        Collections.sort(result.chatsYouAreIn, communityPeerDialogComparator);
        Collections.sort(result.chatsYouCanView, communityPeerDialogComparator);
        Collections.sort(result.chatsYouCanJoin, communityPeerDialogComparator);
        Collections.sort(result.chatsOther, communityPeerDialogComparator);

        return result;
    }




    public static class UnreadCounts {
        public int unreadCount;
        public int mentionCount;
        public int reactionMentionCount;
        public int pollVotesMentionCount;
        public boolean hasUnmutedUnreadDialogs;
    }

    public ArrayList<TLRPC.Dialog> getDialogsByCommunity(long communityId) {
        ArrayList<TLRPC.Dialog> dialogs = dialogsByCommunity.get(communityId);
        if (dialogs == null) {
            return new ArrayList<>();
        }
        return dialogs;
    }

    public UnreadCounts getCommunityUnreadCount(long communityId) {
        // todo: optimize and count forums?

        final UnreadCounts unreadCounts = new UnreadCounts();
        ArrayList<TLRPC.Dialog> dialogs = dialogsByCommunity.get(communityId);
        if (dialogs == null) {
            return unreadCounts;
        }

        for (int a = 0, N = dialogs.size(); a < N; a++) {
            TLRPC.Dialog dialog = dialogs.get(a);

            if (dialog.id < 0) {
                final TLRPC.Chat chat = getChat(-dialog.id);
                if (chat != null && (chat.forum || chat.monoforum && ChatObject.canManageMonoForum(currentAccount, chat))) {
                    int[] counts = MessagesController.getInstance(currentAccount).getTopicsController().getForumUnreadCount(chat.id);
                    unreadCounts.unreadCount += counts[0];
                    unreadCounts.mentionCount += counts[1];
                    unreadCounts.reactionMentionCount += counts[2];
                    unreadCounts.pollVotesMentionCount += counts[4];
                    if (!unreadCounts.hasUnmutedUnreadDialogs && counts[0] > 0) {
                        unreadCounts.hasUnmutedUnreadDialogs |= counts[3] != 0;
                    }
                    continue;
                }
            }

            unreadCounts.unreadCount += dialog.unread_count;
            unreadCounts.mentionCount += dialog.unread_mentions_count;
            unreadCounts.reactionMentionCount += dialog.unread_reactions_count;
            unreadCounts.pollVotesMentionCount += dialog.unread_poll_votes_count;
            if (!unreadCounts.hasUnmutedUnreadDialogs && dialog.unread_count > 0) {
                unreadCounts.hasUnmutedUnreadDialogs |= !isDialogMuted(dialog.id, 0);
            }
        }
        return unreadCounts;
    }

    public MessageObject findCommunityLastMessage(long communityId) {
        final ArrayList<TLRPC.Dialog> dialogs = dialogsByCommunity.get(communityId);
        if (dialogs == null) {
            return null;
        }
        for (int a = 0, N = dialogs.size(); a < N; a++) {
            final long dialogId = dialogs.get(a).id;
            final ArrayList<MessageObject> messages = dialogMessage.get(dialogId);
            if (messages != null && !messages.isEmpty()) {
                return messages.get(0);
            }
        }

        return null;
    }






    public ArrayList<TLRPC.Dialog> getDialogs(int folderId) {
        ArrayList<TLRPC.Dialog> dialogs = dialogsByFolder.get(folderId);
        if (dialogs == null) {
            return new ArrayList<>();
        }
        return dialogs;
    }

    public int getAllFoldersDialogsCount() {
        int count = 0;
        for (int i = 0; i < dialogsByFolder.size(); i++) {
            List<TLRPC.Dialog> dialogs = dialogsByFolder.get(dialogsByFolder.keyAt(i));
            if (dialogs != null) {
                count += dialogs.size();
            }
        }
        return count;
    }

    public int getTotalDialogsCount() {
        int count = 0;
        ArrayList<TLRPC.Dialog> dialogs = dialogsByFolder.get(0);
        if (dialogs != null) {
            count += dialogs.size();
        }
        return count;
    }

    public void putAllNeededDraftDialogs() {
        LongSparseArray<LongSparseArray<TLRPC.DraftMessage>> drafts = getMediaDataController().getDrafts();
        for (int i = 0, size = drafts.size(); i < size; i++) {
            LongSparseArray<TLRPC.DraftMessage> threads = drafts.valueAt(i);
            TLRPC.DraftMessage draftMessage = threads.get(0);
            if (draftMessage == null) {
                continue;
            }
            putDraftDialogIfNeed(drafts.keyAt(i), draftMessage);
        }
    }

    public void putDraftDialogIfNeed(long dialogId, TLRPC.DraftMessage draftMessage) {
        if (dialogs_dict.indexOfKey(dialogId) < 0) {
            if (ChatObject.isMonoForum(currentAccount, dialogId) && !ChatObject.canManageMonoForum(currentAccount, dialogId)) {
                return;
            }

            MediaDataController mediaDataController = getMediaDataController();
            int dialogsCount = allDialogs.size();
            if (dialogsCount > 0) {
                TLRPC.Dialog dialog = allDialogs.get(dialogsCount - 1);
                long minDate = DialogObject.getLastMessageOrDraftDate(dialog, mediaDataController.getDraft(dialog.id, 0));
                if (draftMessage.date < minDate) {
                    return;
                }
            }
            TLRPC.TL_dialog dialog = new TLRPC.TL_dialog();
            dialog.id = dialogId;
            dialog.draft = draftMessage;
            dialog.folder_id = mediaDataController.getDraftFolderId(dialogId);
            dialog.flags = dialogId < 0 && ChatObject.isChannel(getChat(-dialogId)) ? 1 : 0;
            dialogs_dict.put(dialogId, dialog);
            allDialogs.add(dialog);
            sortDialogs(null);
        }
    }

    public void removeDraftDialogIfNeed(long dialogId) {
        TLRPC.Dialog dialog = dialogs_dict.get(dialogId);
        if (dialog != null && dialog.top_message == 0) {
            dialogs_dict.remove(dialog.id);
            allDialogs.remove(dialog);
        }
    }

    private void removeDialog(TLRPC.Dialog dialog) {
        if (dialog == null) {
            return;
        }
        long did = dialog.id;
        if (dialogsServerOnly.remove(dialog) && DialogObject.isChannel(dialog)) {
            Utilities.stageQueue.postRunnable(() -> {
                channelsPts.delete(-did);
                shortPollChannels.delete(-did);
                needShortPollChannels.delete(-did);
                shortPollOnlines.delete(-did);
                needShortPollOnlines.delete(-did);
            });
        }
        allDialogs.remove(dialog);
        dialogsMyChannels.remove(dialog);
        dialogsMyGroups.remove(dialog);
        dialogsCanAddUsers.remove(dialog);
        dialogsChannelsOnly.remove(dialog);
        dialogsGroupsOnly.remove(dialog);
        dialogsUsersOnly.remove(dialog);
        dialogsForBlock.remove(dialog);
        dialogsForward.remove(dialog);
        for (int a = 0; a < selectedDialogFilter.length; a++) {
            if (selectedDialogFilter[a] != null) {
                selectedDialogFilter[a].dialogs.remove(dialog);
                selectedDialogFilter[a].dialogsForward.remove(dialog);
            }
        }
        dialogs_dict.remove(did);

        ArrayList<TLRPC.Dialog> dialogs = dialogsByFolder.get(dialog.folder_id);
        if (dialogs != null) {
            dialogs.remove(dialog);
        }
        removeDialogFromCommunityMap(dialog);
    }

    private void removeDialogFromCommunityMap(TLRPC.Dialog dialog) {
        if (dialog.id > 0) {
            final TLRPC.User user = getUser(dialog.id);
            if (user != null && user.linked_community_id != 0) {
                ArrayList<TLRPC.Dialog> dialogs2 = dialogsByCommunity.get(user.linked_community_id);
                if (dialogs2 != null) {
                    dialogs2.remove(dialog);
                }
            }
        } else {
            final TLRPC.Chat chat = getChat(-dialog.id);
            if (chat != null && chat.linked_community_id != 0) {
                ArrayList<TLRPC.Dialog> dialogs2 = dialogsByCommunity.get(chat.linked_community_id);
                if (dialogs2 != null) {
                    dialogs2.remove(dialog);
                }
            }
        }
    }


    public void hidePromoDialog() {
        if (promoDialog == null) {
            return;
        }
        TLRPC.TL_help_hidePromoData req = new TLRPC.TL_help_hidePromoData();
        req.peer = getInputPeer(promoDialog.id);
        getConnectionsManager().sendRequest(req, (response, error) -> {

        });
        Utilities.stageQueue.postRunnable(() -> {
            promoDialogId = 0;
            proxyDialogAddress = null;
            nextPromoInfoCheckTime = getConnectionsManager().getCurrentTime() + 60 * 60;
            getGlobalMainSettings().edit().putLong("proxy_dialog", promoDialogId).remove("proxyDialogAddress").putInt("nextPromoInfoCheckTime", nextPromoInfoCheckTime).commit();
        });
        removePromoDialog();
    }

    public void deleteDialog(final long did, int onlyHistory) {
        deleteDialog(did, onlyHistory, false);
    }

    public void deleteDialog(final long did, int onlyHistory, boolean revoke) {
        deleteDialog(did, 1, onlyHistory, 0, revoke, null, 0);
    }

    public void setDialogHistoryTTL(long did, int ttl) {
        TLRPC.TL_messages_setHistoryTTL req = new TLRPC.TL_messages_setHistoryTTL();
        req.peer = getInputPeer(did);
        req.period = ttl;
        getConnectionsManager().sendRequest(req, (response, error) -> {
            if (response != null) {
                TLRPC.Updates updates = (TLRPC.Updates) response;
                processUpdates(updates, false);
            }
        });
        TLRPC.ChatFull chatFull = null;
        TLRPC.UserFull userFull = null;
        TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(did);
        if (dialog != null) {
            dialog.ttl_period = ttl;
        }
        getMessagesStorage().setDialogTtl(did, ttl);
        if (did > 0) {
            userFull = getUserFull(did);
            if (userFull != null) {
                userFull.ttl_period = ttl;
                userFull.flags |= 16384;
            }
        } else {
            chatFull = getChatFull(-did);
            if (chatFull != null) {
                chatFull.ttl_period = ttl;
                if (chatFull instanceof TLRPC.TL_channelFull) {
                    chatFull.flags |= 16777216;
                } else {
                    chatFull.flags |= 16384;
                }
            }
        }
        if (chatFull != null) {
            getNotificationCenter().postNotificationName(NotificationCenter.chatInfoDidLoad, chatFull, 0, false, false);
        } else if (userFull != null) {
            getNotificationCenter().postNotificationName(NotificationCenter.userInfoDidLoad, did, userFull);
        }
    }

    public void setDialogsInTransaction(boolean transaction) {
        dialogsInTransaction = transaction;
        if (!transaction) {
            getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload, true);
        }
    }

    protected void deleteDialog(long did, int first, int onlyHistory, int max_id, boolean revoke, TLRPC.InputPeer peer, long taskId) {
        if (onlyHistory == 2) {
            if (did == getUserConfig().getClientUserId()) {
                getSavedMessagesController().deleteAllDialogs();
            }
            getMessagesStorage().deleteDialog(did, onlyHistory);
            return;
        }
        for (int i = 0; i < sendAsPeers.size(); i++) {
            SendAsPeersInfo sendAsInfo = sendAsPeers.valueAt(i);
            if (sendAsInfo.sendAsPeers != null) {
                for (int j = 0; j < sendAsInfo.sendAsPeers.chats.size(); j++) {
                    if (sendAsInfo.sendAsPeers.chats.get(j).id == -did) {
                        sendAsInfo.sendAsPeers.chats.remove(j);
                        break;
                    }
                }
                for (int j = 0; j < sendAsInfo.sendAsPeers.peers.size(); j++) {
                    if (sendAsInfo.sendAsPeers.peers.get(j).peer.channel_id == -did || sendAsInfo.sendAsPeers.peers.get(j).peer.chat_id == -did) {
                        sendAsInfo.sendAsPeers.peers.remove(j);
                        break;
                    }
                }
            }
        }
        sendAsPeers.remove(did);
        sendAsPeersLiveStories.remove(did);
        if (first == 1 && max_id == 0) {
            TLRPC.InputPeer peerFinal = peer;
            getMessagesStorage().getDialogMaxMessageId(did, (param) -> {
                if (did == getUserConfig().getClientUserId()) {
                    getSavedMessagesController().deleteAllDialogs();
                }
                deleteDialog(did, 2, onlyHistory, Math.max(0, param), revoke, peerFinal, taskId);
                checkIfFolderEmpty(1);
            });
            return;
        }
        if (onlyHistory == 0 || onlyHistory == 3) {
            getMediaDataController().uninstallShortcut(did, MediaDataController.SHORTCUT_TYPE_USER_OR_CHAT);
        }
        int max_id_delete = max_id;

        if (first != 0) {
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("delete dialog with id " + did);
            }
            boolean isPromoDialog = false;
            if (did == getUserConfig().getClientUserId()) {
                getSavedMessagesController().deleteAllDialogs();
            }
            getMessagesStorage().deleteDialog(did, onlyHistory);
            TLRPC.Dialog dialog = dialogs_dict.get(did);
            if (onlyHistory == 0 || onlyHistory == 3) {
                getNotificationCenter().postNotificationName(NotificationCenter.dialogDeleted, did, 0);
                getNotificationsController().deleteNotificationChannel(did, 0);
                JoinCallAlert.processDeletedChat(currentAccount, did);
            }
            if (onlyHistory == 0) {
                getMediaDataController().cleanDraft(did, 0, false);
            }
            if (dialog != null) {
                if (first == 2) {
                    max_id_delete = Math.max(0, dialog.top_message);
                    max_id_delete = Math.max(max_id_delete, dialog.read_inbox_max_id);
                    max_id_delete = Math.max(max_id_delete, dialog.read_outbox_max_id);
                }
                if (onlyHistory == 0 || onlyHistory == 3) {
                    if (isPromoDialog = (promoDialog != null && promoDialog.id == did)) {
                        isLeftPromoChannel = true;
                        if (promoDialog.id < 0) {
                            TLRPC.Chat chat = getChat(-promoDialog.id);
                            if (chat != null) {
                                chat.left = true;
                            }
                        }
                        sortDialogs(null);
                    } else {
                        removeDialog(dialog);
                        int offset = nextDialogsCacheOffset.get(dialog.folder_id, 0);
                        if (offset > 0) {
                            nextDialogsCacheOffset.put(dialog.folder_id, offset - 1);
                        }
                    }
                } else {
                    dialog.unread_count = 0;
                }
                if (!isPromoDialog) {
                    int lastMessageId;
                    ArrayList<MessageObject> objects = dialogMessage.get(dialog.id);
                    dialogMessage.remove(dialog.id);
                    if (objects != null && objects.size() > 0 && objects.get(0) != null) {
                        lastMessageId = objects.get(0).getId();
                        for (int i = 0; i < objects.size(); ++i) {
                            MessageObject object = objects.get(i);
                            if (object != null && object.getId() > lastMessageId) {
                                lastMessageId = object.getId();
                            }
                            if (object != null && object.messageOwner.peer_id.channel_id == 0) {
                                dialogMessagesByIds.remove(object.getId());
                            }
                            if (object != null && object.messageOwner.random_id != 0) {
                                dialogMessagesByRandomIds.remove(object.messageOwner.random_id);
                            }
                        }
                    } else {
                        lastMessageId = dialog.top_message;
                        MessageObject object = dialogMessagesByIds.get(dialog.top_message);
                        if (object != null && object.messageOwner.peer_id.channel_id == 0) {
                            dialogMessagesByIds.remove(dialog.top_message);
                        }
                        if (object != null && object.messageOwner.random_id != 0) {
                            dialogMessagesByRandomIds.remove(object.messageOwner.random_id);
                        }
                    }
                    if (onlyHistory == 1 && !DialogObject.isEncryptedDialog(did) && lastMessageId > 0) {
                        TLRPC.TL_messageService message = new TLRPC.TL_messageService();
                        message.id = dialog.top_message;
                        message.out = getUserConfig().getClientUserId() == did;
                        message.from_id = new TLRPC.TL_peerUser();
                        message.from_id.user_id = getUserConfig().getClientUserId();
                        message.flags |= 256;
                        message.action = new TLRPC.TL_messageActionHistoryClear();
                        message.date = dialog.last_message_date;
                        message.dialog_id = did;
                        message.peer_id = getPeer(did);
                        boolean isDialogCreated = createdDialogIds.contains(message.dialog_id);
                        MessageObject obj = new MessageObject(currentAccount, message, isDialogCreated, isDialogCreated);
                        ArrayList<MessageObject> objArr = new ArrayList<>();
                        objArr.add(obj);
                        ArrayList<TLRPC.Message> arr = new ArrayList<>();
                        arr.add(message);
                        updateInterfaceWithMessages(did, objArr, 0);
                        getMessagesStorage().putMessages(arr, false, true, false, 0, false, 0, 0);
                    } else {
                        dialog.top_message = 0;
                    }
                }
            }
            if (first == 2) {
                Integer max = dialogs_read_inbox_max.get(did);
                if (max != null) {
                    max_id_delete = Math.max(max, max_id_delete);
                }
                max = dialogs_read_outbox_max.get(did);
                if (max != null) {
                    max_id_delete = Math.max(max, max_id_delete);
                }
            }

            if (!dialogsInTransaction) {
                if (isPromoDialog) {
                    getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload, true);
                } else {
                    getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
                    getNotificationCenter().postNotificationName(NotificationCenter.removeAllMessagesFromDialog, did, false, null);
                }
            }
            getMessagesStorage().getStorageQueue().postRunnable(() -> AndroidUtilities.runOnUIThread(() -> getNotificationsController().removeNotificationsForDialog(did)));
        }

        if (onlyHistory == 3) {
            return;
        }

        if (!DialogObject.isEncryptedDialog(did)) {
            if (peer == null) {
                peer = getInputPeer(did);
            }
            if (peer == null) {
                return;
            }

            long newTaskId;
            if (!(peer instanceof TLRPC.TL_inputPeerChannel) || onlyHistory != 0) {
                if (max_id_delete > 0 && max_id_delete != Integer.MAX_VALUE) {
                    int current = deletedHistory.get(did, 0);
                    deletedHistory.put(did, Math.max(current, max_id_delete));
                }

                if (taskId == 0) {
                    NativeByteBuffer data = null;
                    try {
                        data = new NativeByteBuffer(4 + 8 + 4 + 4 + 4 + 4 + peer.getObjectSize());
                        data.writeInt32(13);
                        data.writeInt64(did);
                        data.writeBool(first != 0);
                        data.writeInt32(onlyHistory);
                        data.writeInt32(max_id_delete);
                        data.writeBool(revoke);
                        peer.serializeToStream(data);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                    newTaskId = getMessagesStorage().createPendingTask(data);
                } else {
                    newTaskId = taskId;
                }
            } else {
                newTaskId = taskId;
            }

            if (peer instanceof TLRPC.TL_inputPeerChannel) {
                if (onlyHistory == 0) {
                    if (newTaskId != 0) {
                        getMessagesStorage().removePendingTask(newTaskId);
                    }
                    return;
                }
                TLRPC.TL_channels_deleteHistory req = new TLRPC.TL_channels_deleteHistory();
                req.channel = new TLRPC.TL_inputChannel();
                req.for_everyone = revoke;
                req.channel.channel_id = peer.channel_id;
                req.channel.access_hash = peer.access_hash;
                req.max_id = max_id_delete > 0 ? max_id_delete : Integer.MAX_VALUE;
                getConnectionsManager().sendRequest(req, (response, error) -> {
                    if (newTaskId != 0) {
                        getMessagesStorage().removePendingTask(newTaskId);
                    }
                    if (response != null) {
                        processUpdates((TLRPC.Updates) response, false);
                    }
                }, ConnectionsManager.RequestFlagInvokeAfter);
            } else {
                TLRPC.TL_messages_deleteHistory req = new TLRPC.TL_messages_deleteHistory();
                req.peer = peer;
                req.max_id = max_id_delete > 0 ? max_id_delete : Integer.MAX_VALUE;
                req.just_clear = onlyHistory != 0;
                req.revoke = revoke;
                int max_id_delete_final = max_id_delete;
                TLRPC.InputPeer peerFinal = peer;
                getConnectionsManager().sendRequest(req, (response, error) -> {
                    if (newTaskId != 0) {
                        getMessagesStorage().removePendingTask(newTaskId);
                    }
                    if (error == null) {
                        TLRPC.TL_messages_affectedHistory res = (TLRPC.TL_messages_affectedHistory) response;
                        if (res.offset > 0) {
                            deleteDialog(did, 0, onlyHistory, max_id_delete_final, revoke, peerFinal, 0);
                        }
                        processNewDifferenceParams(-1, res.pts, -1, res.pts_count);
                        getMessagesStorage().onDeleteQueryComplete(did);
                    }
                }, ConnectionsManager.RequestFlagInvokeAfter);
            }
        } else {
            int encryptedId = DialogObject.getEncryptedChatId(did);
            if (onlyHistory == 1) {
                getSecretChatHelper().sendClearHistoryMessage(getEncryptedChat(encryptedId), null);
            } else {
                getSecretChatHelper().declineSecretChat(encryptedId, revoke);
            }
        }
    }

    public void deleteSavedDialog(long did) {
        deleteSavedDialog(did, 0, null);
    }

    public void deleteSavedDialog(long did, TLRPC.InputPeer monoForumPeer) {
        deleteSavedDialog(did, 0, monoForumPeer);
    }

    protected void deleteSavedDialog(long did, int input_max_id, TLRPC.InputPeer monoForumPeer) {
        final long monoForumDid = DialogObject.getPeerDialogId(monoForumPeer);
        int[] max_id = new int[] { input_max_id };
        Runnable perform = () -> {
            if (monoForumDid == 0) {
                getMessagesStorage().deleteSavedDialog(did);
            }
            TLRPC.TL_messages_deleteSavedHistory req = new TLRPC.TL_messages_deleteSavedHistory();
            req.peer = getInputPeer(did);
            req.parent_peer = monoForumPeer;
            if (input_max_id == 0 && monoForumDid == 0) {
                SavedMessagesController.SavedDialog dialog = null;
                for (int i = 0; i < getSavedMessagesController().allDialogs.size(); ++i) {
                    if (getSavedMessagesController().allDialogs.get(i).dialogId == did) {
                        dialog = getSavedMessagesController().allDialogs.get(i);
                        break;
                    }
                }
                if (dialog != null) {
                    max_id[0] = Math.max(max_id[0], dialog.top_message_id);
                    getSavedMessagesController().deleteDialog(did);
                }
                req.max_id = max_id[0] <= 0 ? Integer.MAX_VALUE : max_id[0];
            }
            getConnectionsManager().sendRequest(req, (response, error) -> {
                if (error == null) {
                    TLRPC.TL_messages_affectedHistory res = (TLRPC.TL_messages_affectedHistory) response;
                    if (res.offset > 0) {
                        deleteSavedDialog(did, max_id[0], monoForumPeer);
                    }
                    processNewDifferenceParams(-1, res.pts, -1, res.pts_count);
                    getMessagesStorage().onDeleteQueryComplete(did);
                }
            }, ConnectionsManager.RequestFlagInvokeAfter);
        };
        if (max_id[0] <= 0 && monoForumDid == 0) {
            getMessagesStorage().getSavedDialogMaxMessageId(did, (param) -> {
                max_id[0] = param;
                perform.run();
            });
        } else {
            perform.run();
        }
    }

    public void saveGif(Object parentObject, TLRPC.Document document) {
        if (parentObject == null || !MessageObject.isGifDocument(document)) {
            return;
        }
        TLRPC.TL_messages_saveGif req = new TLRPC.TL_messages_saveGif();
        req.id = new TLRPC.TL_inputDocument();
        req.id.id = document.id;
        req.id.access_hash = document.access_hash;
        req.id.file_reference = document.file_reference;
        if (req.id.file_reference == null) {
            req.id.file_reference = new byte[0];
        }
        req.unsave = false;
        getConnectionsManager().sendRequest(req, (response, error) -> {
            if (error != null && FileRefController.isFileRefError(error.text)) {
                getFileRefController().requestReference(parentObject, req);
            }
        });
    }

    public void saveRecentSticker(Object parentObject, TLRPC.Document document, boolean asMask) {
        if (parentObject == null || document == null) {
            return;
        }
        TLRPC.TL_messages_saveRecentSticker req = new TLRPC.TL_messages_saveRecentSticker();
        req.id = new TLRPC.TL_inputDocument();
        req.id.id = document.id;
        req.id.access_hash = document.access_hash;
        req.id.file_reference = document.file_reference;
        if (req.id.file_reference == null) {
            req.id.file_reference = new byte[0];
        }
        req.unsave = false;
        req.attached = asMask;
        getConnectionsManager().sendRequest(req, (response, error) -> {
            if (error != null && FileRefController.isFileRefError(error.text)) {
                getFileRefController().requestReference(parentObject, req);
            }
        });
    }

    public void loadChannelParticipants(Long chatId) {
        loadChannelParticipants(chatId, null, 32);
    }

    public void loadChannelParticipants(Long chatId, Utilities.Callback<TLRPC.TL_channels_channelParticipants> whenDone, int count) {
        if (whenDone == null && (loadingFullParticipants.contains(chatId) || loadedFullParticipants.contains(chatId))) {
            return;
        }
        loadingFullParticipants.add(chatId);

        TLRPC.TL_channels_getParticipants req = new TLRPC.TL_channels_getParticipants();
        req.channel = getInputChannel(chatId);
        req.filter = new TLRPC.TL_channelParticipantsRecent();
        req.offset = 0;
        req.limit = count;
        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error == null) {
                TLRPC.TL_channels_channelParticipants res = (TLRPC.TL_channels_channelParticipants) response;
                putUsers(res.users, false);
                putChats(res.chats, false);
                getMessagesStorage().putUsersAndChats(res.users, res.chats, true, true);
                getMessagesStorage().updateChannelUsers(chatId, res.participants);
                loadedFullParticipants.add(chatId);
            }
            loadingFullParticipants.remove(chatId);
            if (whenDone != null) {
                whenDone.run(response instanceof TLRPC.TL_channels_channelParticipants ? (TLRPC.TL_channels_channelParticipants) response : null);
            }
        }));
    }

    public void putChatFull(TLRPC.ChatFull chatFull) {
        fullChats.put(chatFull.id, chatFull);
        getTranslateController().updateDialogFull(-chatFull.id);
    }

    public void processChatInfo(long chatId, TLRPC.ChatFull info, ArrayList<TLRPC.User> usersArr, ArrayList<TLRPC.Chat> chatsArr, boolean fromCache, boolean force, boolean byChannelUsers, ArrayList<Integer> pinnedMessages, HashMap<Integer, MessageObject> pinnedMessagesMap, int totalPinnedCount, boolean pinnedEndReached) {
        AndroidUtilities.runOnUIThread(() -> {
            if (fromCache && chatId > 0 && !byChannelUsers) {
                long lastLoadedTime = loadedFullChats.get(chatId, 0);
                if (System.currentTimeMillis() - lastLoadedTime > 60 * 1000) {
                    loadFullChat(chatId, 0, force);
                }
            }
            if (info != null) {
                if (fullChats.get(chatId) == null) {
                    fullChats.put(chatId, info);
                    getTranslateController().updateDialogFull(-chatId);
                }
                putUsers(usersArr, fromCache);
                putChats(chatsArr, fromCache);
                if (info.stickerset != null) {
                    getMediaDataController().getGroupStickerSetById(info.stickerset);
                }
                if (info.emojiset != null) {
                    getMediaDataController().getGroupStickerSetById(info.emojiset);
                }
                getNotificationCenter().postNotificationName(NotificationCenter.chatInfoDidLoad, info, 0, byChannelUsers, false);
            }
            if (pinnedMessages != null) {
                getNotificationCenter().postNotificationName(NotificationCenter.pinnedInfoDidLoad, -chatId, pinnedMessages, pinnedMessagesMap, totalPinnedCount, pinnedEndReached);
            }
        });
    }

    public void loadUserInfo(TLRPC.User user, boolean force, int classGuid) {
        loadUserInfo(user, force, classGuid, 0);
    }

    public void loadUserInfo(TLRPC.User user, boolean force, int classGuid, int fromMessageId) {
        getMessagesStorage().loadUserInfo(user, force, classGuid, fromMessageId);
    }

    public void updateUsernameActiveness(TLObject object, String username, boolean active) {
        if (TextUtils.isEmpty(username)) {
            return;
        }
        objectsByUsernames.remove(username);
        if (active) {
            objectsByUsernames.put(username.toLowerCase(), object);
        }
    }

    public void processUserInfo(TLRPC.User user, TLRPC.UserFull info, boolean fromCache, boolean force, int classGuid, ArrayList<Integer> pinnedMessages, HashMap<Integer, MessageObject> pinnedMessagesMap, int totalPinnedCount, boolean pinnedEndReached) {
        AndroidUtilities.runOnUIThread(() -> {
            if (fromCache) {
                long lastLoadedTime = loadedFullUsers.get(user.id, 0);
                if (System.currentTimeMillis() - lastLoadedTime > 60 * 1000) {
                    loadFullUser(user, classGuid, force);
                }
            }
            if (info != null) {
                if (fullUsers.get(user.id) == null) {
                    fullUsers.put(user.id, info);
                    getTranslateController().updateDialogFull(user.id);
                    StarsController.getInstance(currentAccount).invalidateProfileGifts(info);

                    int index = blockePeers.indexOfKey(user.id);
                    if (info.blocked) {
                        if (index < 0) {
                            blockePeers.put(user.id, 1);
                            getNotificationCenter().postNotificationName(NotificationCenter.blockedUsersDidLoad);
                        }
                    } else {
                        if (index >= 0) {
                            blockePeers.removeAt(index);
                            getNotificationCenter().postNotificationName(NotificationCenter.blockedUsersDidLoad);
                        }
                    }
                }
                getNotificationCenter().postNotificationName(NotificationCenter.userInfoDidLoad, user.id, info);
            }
            if (pinnedMessages != null) {
                getNotificationCenter().postNotificationName(NotificationCenter.pinnedInfoDidLoad, user.id, pinnedMessages, pinnedMessagesMap, totalPinnedCount, pinnedEndReached);
            }
        });
    }

    public void updateTimerProc() {
        long currentTime = System.currentTimeMillis();

        checkDeletingTask(false);
        checkReadTasks();

        if (getUserConfig().isClientActivated()) {
            if (!ignoreSetOnline && getConnectionsManager().getPauseTime() == 0 && ApplicationLoader.isScreenOn && !ApplicationLoader.mainInterfacePausedStageQueue) {
                if (ApplicationLoader.mainInterfacePausedStageQueueTime != 0 && Math.abs(ApplicationLoader.mainInterfacePausedStageQueueTime - System.currentTimeMillis()) > 1000) {
                    if (statusSettingState != 1 && (lastStatusUpdateTime == 0 || Math.abs(System.currentTimeMillis() - lastStatusUpdateTime) >= 55000 || offlineSent)) {
                        statusSettingState = 1;

                        if (statusRequest != 0) {
                            getConnectionsManager().cancelRequest(statusRequest, true);
                        }

                        TL_account.updateStatus req = new TL_account.updateStatus();
                        req.offline = false;
                        statusRequest = getConnectionsManager().sendRequest(req, (response, error) -> {
                            if (error == null) {
                                lastStatusUpdateTime = System.currentTimeMillis();
                                offlineSent = false;
                                statusSettingState = 0;
                            } else {
                                if (lastStatusUpdateTime != 0) {
                                    lastStatusUpdateTime += 5000;
                                }
                            }
                            statusRequest = 0;
                        });
                    }
                }
            } else if (statusSettingState != 2 && !offlineSent && Math.abs(System.currentTimeMillis() - getConnectionsManager().getPauseTime()) >= 2000) {
                statusSettingState = 2;
                if (statusRequest != 0) {
                    getConnectionsManager().cancelRequest(statusRequest, true);
                }
                TL_account.updateStatus req = new TL_account.updateStatus();
                req.offline = true;
                statusRequest = getConnectionsManager().sendRequest(req, (response, error) -> {
                    if (error == null) {
                        offlineSent = true;
                    } else {
                        if (lastStatusUpdateTime != 0) {
                            lastStatusUpdateTime += 5000;
                        }
                    }
                    statusRequest = 0;
                });
            }

            if (updatesQueueChannels.size() != 0) {
                for (int a = 0; a < updatesQueueChannels.size(); a++) {
                    long key = updatesQueueChannels.keyAt(a);
                    long updatesStartWaitTime = updatesStartWaitTimeChannels.valueAt(a);
                    if (Math.abs(currentTime - updatesStartWaitTime) >= 1500) {
                        if (BuildVars.LOGS_ENABLED) {
                            FileLog.d("QUEUE CHANNEL " + key + " UPDATES WAIT TIMEOUT - CHECK QUEUE");
                        }
                        processChannelsUpdatesQueue(key, 0);
                    }
                }
            }

            for (int a = 0; a < 3; a++) {
                if (getUpdatesStartTime(a) != 0 && Math.abs(currentTime - getUpdatesStartTime(a)) >= 1500) {
                    if (BuildVars.LOGS_ENABLED) {
                        FileLog.d(a + " QUEUE UPDATES WAIT TIMEOUT - CHECK QUEUE");
                    }
                    processUpdatesQueue(a, 0);
                }
            }
        }
        int currentServerTime = getConnectionsManager().getCurrentTime();
        if (Math.abs(System.currentTimeMillis() - lastViewsCheckTime) >= 5000) {
            lastViewsCheckTime = System.currentTimeMillis();
            if (channelViewsToSend.size() != 0) {
                for (int a = 0; a < channelViewsToSend.size(); a++) {
                    long key = channelViewsToSend.keyAt(a);
                    TLRPC.TL_messages_getMessagesViews req = new TLRPC.TL_messages_getMessagesViews();
                    req.peer = getInputPeer(key);
                    req.id = channelViewsToSend.valueAt(a);
                    req.increment = a == 0;
                    getConnectionsManager().sendRequest(req, (response, error) -> {
                        if (response != null) {
                            TLRPC.TL_messages_messageViews res = (TLRPC.TL_messages_messageViews) response;
                            LongSparseArray<SparseIntArray> channelViews = new LongSparseArray<>();
                            LongSparseArray<SparseIntArray> channelForwards = new LongSparseArray<>();
                            LongSparseArray<SparseArray<TLRPC.MessageReplies>> channelReplies = new LongSparseArray<>();
                            SparseIntArray views = channelViews.get(key);
                            SparseIntArray forwards = channelForwards.get(key);
                            SparseArray<TLRPC.MessageReplies> replies = channelReplies.get(key);

                            for (int a1 = 0; a1 < req.id.size(); a1++) {
                                if (a1 >= res.views.size()) {
                                    break;
                                }
                                TLRPC.TL_messageViews messageViews = res.views.get(a1);
                                if ((messageViews.flags & 1) != 0) {
                                    if (views == null) {
                                        views = new SparseIntArray();
                                        channelViews.put(key, views);
                                    }
                                    views.put(req.id.get(a1), messageViews.views);
                                }
                                if ((messageViews.flags & 2) != 0) {
                                    if (forwards == null) {
                                        forwards = new SparseIntArray();
                                        channelForwards.put(key, forwards);
                                    }
                                    forwards.put(req.id.get(a1), messageViews.forwards);
                                }
                                if ((messageViews.flags & 4) != 0) {
                                    if (replies == null) {
                                        replies = new SparseArray<>();
                                        channelReplies.put(key, replies);
                                    }
                                    replies.put(req.id.get(a1), messageViews.replies);
                                }
                            }
                            getMessagesStorage().putUsersAndChats(res.users, res.chats, true, true);
                            getMessagesStorage().putChannelViews(channelViews, channelForwards, channelReplies, false);
                            AndroidUtilities.runOnUIThread(() -> {
                                putUsers(res.users, false);
                                putChats(res.chats, false);
                                getNotificationCenter().postNotificationName(NotificationCenter.didUpdateMessagesViews, channelViews, channelForwards, channelReplies, false);
                            });
                        }
                    });
                }
                channelViewsToSend.clear();
            }
            if (pollsToCheckSize > 0) {
                AndroidUtilities.runOnUIThread(() -> {
                    long time = SystemClock.elapsedRealtime();
                    int minExpireTime = Integer.MAX_VALUE;
                    for (int a = 0, N = pollsToCheck.size()xx @翜}ksǱ>Esd)$^>JPK`I؅$c~{Y [Et|٫,l#?ebϛbw>_M?v'ej.뽻bn>n7,oMUB Q=m妞grfdB-oh$&/:@x.GC!&zbzôD} J[Nz<
u]4+>y1@'
B/D.QU7pEK?gO\Emxy1m/b~II(y-+H>)H!4qw}g=w,`W 5_7=AgC(oZ֯8S
)tz19W`9/&p[F9z<uJxTd(1N3$;/E6ټ E{	%0
٢=#?/M&:in%HX'bF:3˛;[-)WOfYO|J {ϋ_EH"dy=Ȟ$28\˫&[>ld	 6U#VV?8j/_7M2!+Ń@rڴtT7!}91͂.{A{Wa=VY=$[|_TDTU]7m1U^UUD֭otثH/^r6Gl3lMNDΎ[r+ʾey-(:*_P)t8%"%bj]qWdNbJI*i-2m5HBXOrZ~P6Y{߳_ʦ}uTOow>GeT$Cjc~v8m|d^f:NC(O`,@E4yp_{y^W{Dq)/px,{cq~Ճ'p ٳQ^
|ՇMR5<.WmYmIbz::Cetn˛r2GȀܬn'%N<4G w/ǻ?/.w/.Ljf@sWQݻˉvYqujDBʩ6.  g r	dZ65C#dVUHNE%3FEUE`^I(Mb|a,
L	D$;~ysS>B<dF9VՠgDK;/

;䧏;1ڙxtk. 1}fɷAʞe߽ ƷU=r=<^wNO k00`|A߁2ayК.W(h][M梭_A	h{bx@Ԭ0OtQkge'y	;
WM1o:+gEKEZT\}t)vEIPV	٨TV*w@R.Q@PCĵa)ίW?Sp!82tR!	mV72eBw5=e`}t;!
®)	Z	0
L]- Q(h|w.UV{R=yhP:(fK%jg44qsL,z"Ϊ~Kw?W&<v&*"ZSb&_;7B?E;$z&̇V	+`5C:|<U$+4݊A_ X_Bk <ʲjޝqrZۮ=2յpyWL
2WD.ƻ>)ozݫᛷ/_샛_l+"{Ve,E
4L7PJ=='#GT̆}1>˛C=Ze:ɫϙ
&UbEsw^_Ekð/	51bђ4PԘbof%|M68Gvwqϋ1%b;7wzM'톫dj6DR-l^OMlW=s]l`2eNɶޓJ*Ġ.-*rk{9\Aעxd٫JRlly.S&U]QwE5KOrTZ@:(]qh3"q\\CbUikGkMnIUFr]N:
'Y.5m?;k9k>&Kg\$Tl@Bs6/q\EKa爫Ӿ`TO&PK(\\WHBnJ"r+"-(
_3yD9菮x8KUl[b&WroN -
!xUIñs3Pťh%c2p /sU(OF\SMU_q^N/v gST=CVAdp-ā3SbD,R&pPN@G	RyMSjG?矬{ZU
$?H4<S;qt7]`g
蚒.NB:)`B(NXb?o$
Yzש?;K0NuuL81[)1a(%JmY'L`ڡ>ݵ@VtqT6®qiHew \ZJtf4oY'8С&TeP`aX;@~8dy<bq	mn
͐1E2Cw;]_َ0+lCǱTyrIjlaKf/vDZgMΪ-y\˷1'<bj~ۙ_A<O,d]eeL|AZ̂!Ҋ`Y<>5
VTx2|la٩7#iZ?޴
q*k9ZHU{BT2pkN1i;?IO
HW!́2T+uLE ;uB+i'zcg`L@\ޒUHlO#bSذ@e3)G'g&\O¯CW?>ˊmy,%p,!VShzj-Heg%]`<.S Du=M=gQ6.*zRNko$Rfv`g 1jfĄ0'Ͳ#a`Űp!Pb/dxu&h"bpu2Q1\ؿ/XJ#e
lYDi%pr
LvYo	qA(@i{
%*VtNƗ
eS
ml,,P1MHAige;_4t5>{:WYrZx\[%}ꃕ2)Y$C|uF˅D	1%^v?C,ZZuu;q#JNqèJ^j=iÈ4(?k+Tw.ΜN[X
h87_667,;}|'[Ait ilPxqc$S N00Ҋp Ӌc^cfqgVneQӀΐ5Ir϶I|8,%%!ٔ0[@dĤ5ʟ0YU,tLXtL'դϚ0*5X[@/e
J훺s;Cn1`B
A!WwUwe<6g--amv0kcy'}Hf̾lA&uq8?☭bUcNOaD	7 @!'E{#/w m#hXw?I[A!y&5cohmy6S-2{hHzwO2nDKm8q|$tؒ2n$
|FJs$FMXN1mQH_㿍sB# .'HW6t4y
z #8/)yA*iG_װ[@3Y2+r0<iOB}ю7>?+[P,.~%?%@
]=ur|TK=}x:Kn-2?qXŔ.&]R__Y6Ԁ?+x>G#D̰d b!R8C` 5j0!J 4<h-|#SoX:Zޑ?]7;0U4Rټ|isGgqP=S
و-E<YyDh9#+UíBUeՐ52%\b5ZC-z~,lTG;ʤFxByAbp2#zO4y_N]Wmbq_\g5
1t1*qO
ƧAULo;coK9y1jTt"'e(	N.7?,
RMc~tTӉAIq6ǀ
|u/De-H'Hr$3+ ĥlS`:ψܣؽTS'@ݢb88ț"[EߌʌCKxR27G:bOׁeQ7aI(>+bcjXyREc
Z]k,c\926}~-xk_Ζl{PSВ>kwƦlMBb#J:b\ !LRVu@$$>(|ǁVBPXT[K#b6<QqtyGQ"~{lH2߬of`1|Qg*ǅ1/dU.ڼ]v>(~>㐧5+ҭu5"oG#IG5tFףńe\\uY9	@*=;*Q5|#n 5torg/jl^53
\C~p,!4>?4Ze8=]]7;)y!O
'C둌h'u
*^:hƀToQ(0{rf
XCӧx(.Q"@;Z˾]̸h! w}ЍDbX=P~ҁD ( fBP[ox["EE	W*.*}/Vs%c/uG:&R3	jcriЖbTxp@H`2Ċ`Myy@ջʟ
j[z+v9<w=<j1+9ÏdݬII6 "T3Z(6䷌0wqTP_Hqtoxp~2s'tOOc]s_:1hk\7x%0t<"rXf_~,?5WbZ?$`CsXxx-ܮ۳"߮j	߂16pc)bVҼZ%"5a[:gi0ֆG82XGl'/%3^35ǔ%\85;¦؏C%H(S]v1
 d7ҐrӖ
C-W4,أ
4l"d܉PE09+aǙsNǍ]r^NlA	v7kP* ?3yP'3y`Q
\	R!
,#_9aޤI=WST7)oS;dNZܟN'x8=Ti3]Ԡ8Q!n%m !$L`>?gpw7~<u\,tޣ%("y&lkT|WzϬv܌k~|~IӪB|t?hL7J:EC\lc/|M?ϾZoaj) _Y]&bRBxC9tv".sxf^z9;PtRV5v)eW^NpTomP	j'$˄k
~ˍnfP:ZA|ƌa}g&m~v`I,OY8<DR@s]fe.Ka!rߓsCCS_/ݔ+^*oVlR	IjSn"k*qI׊^שXq쁦5/KjK'^?!5|bf#a5\l>vx&fxMԊyYb@{^R=i)WVmʉ;hWbIUA<ba
,bia'ߜJY*Mr⮹(:kQ%Ӂ]o@mwitI<F';<k-w6(un25~Q>S<q,5ebVmk4!aI@o%jzZ_f^LtDfTfgUs
YQf!ʶyop[՚?n<1_l;Pw r1X9+Rxxur~/#̃5vONodpsJz4O\#gn|Ro};ס$>a<
Y0UyC^>+Xr5/;	짚Bܫۑ!>Kȇq񱯜gHeD8.I$)8	+]Vt&;7߬g/a/q<(4yBaP#M=,tadMS\hDfCLGt:t)bD^$X8rRRp?;v1pXշ{*m?fptbxp;j0=B# SiLn>#?[ bE(@+ǗO,	C/ŀD-ɗ}r`P7AKQ$=ޔ"2Q="PKh/RGΘ#,aC([w}2?3rHΫW$e)c# gm6uHۂ}]꙼=.O	llǧË7o..OO."w"
ޏ󃳣Ãp%bC	!4s)/W]v%B|gӻIuu{K
 ڛeCr:
.ը'V8IOU퓡dFPnOn2%|m:>:z\ޔ?z&rtEzx6e	^.߀}0A7ojzJV|_&' !Af>-$E*SLQg)^[mh\uYw8J/47ArjlL]=oGj	1>?sP@b)I$(z`u_0[}J&&EFK]}^==ܻ{tt@tp-4+͊Ϸ| KK~\O|1}\.b'e}ژO6]kyu}*/>c⋬R1 
/sԈZqZ*!UJ(	NVЊ~IW?6˚a#8ʍ.ygVIIzM/˟'oetnzHI-/`e0pJ#%Tp_zixs&qRǮfiQ*¡
NƵ+"s т< a^.ym_~%jRtG]c()ڇ'IA}E/f`n딓I1a-BbeU5@Ztrsd!a{Uen.
((nfB!ۨHW1S	K$	ދA'~
"=.A+'+ȰN R"\wNKeܹɊq}-ۓlvd'v\njӑ%$lycq5Cj &{1S
>/F`qYUs5纚ri&MK-jǖ4\F`ru"Ѹ38.|t=_c4z_I!Aa4G0-L@/D1ř(Is>'U{{刋ۄbҷ*w>>Дzz֭==r^tϳ~c`@.ud_3rvciϒE=/]&XunsF%@+M6)D
[y|QδWVmO%:zS\Zf{#n$^TIerA'΁hXn!#w.;N CH<$?D,q|{v6_bPleWgg2em[HLoWܥLok	a :>b`\0R7K܊Å?\Hm1D~jg؟r3[W5% #U/vv\WfR3mDiwj`sdapMVO$jfZȊ D_MF@9ʘL^TW,FoRu)C ʤ65l-!҅'p岗YЬݨՁX>DĘVe'Oy.
BTWfI*~QvlwefBؗuHk̊ݪyTCb^bAt6/ޗ--o1
jmv,ix3vG*	Yx,}\Ն
 N~d,1pfq<S*_öpQc7|E kpʫ|nWbD4d>J
jppaJ=d0oF(DOn<]wy^Xus~hZbݯ<>br,7	ۘe!{Q^# K::gw>vI|KML[hU]!Q+MF
x&1ݴcI+>DesVLw2\uS.O3u! J* J" 
Ϟh~JWd/(F?shiNb{]UV%2\<sd*Z^ 	j4zeO"V-o18DOv_8Kup<.]NH5i8}ɀ qs*ƼUF6{̓;=KF2E[
4s5{\;<_&߮l|9W{j7Io7[JQsvD1YqXDorГ^eE_@>iL"}0҆n{5*P=?uQ

bRL^&a0gnnd˜mMNZyi^/
"GnC=}L':%=<	VoO[SF<sJ@u	e\ wv$c9΋ 1HaXRiQ0%]	N`Ћ\#Y4b1C_]`?˼y#&ѼI|T
>9u^znPT)E^휹GB-qvǎFe؍0+ks: -
ΤӖ?h
%_L-*hZD(tgȘ%c7&tguU&
^b^XUo)_0sZGbpKȧ-A!&a<ۆyo]Ses8-[
GtsZۚ!CW3r#SkkCU28s_%smb0I0$`ߩ6QWwEKtLGICLyǷ)\RzXAqټh@{%|Buί~!BG.-zUct޸aɎV^e&d$HnCkG{xЋVz; h)$*
M }5nHv:H%<k%ɔf9f᧡ݫK/f룒m=?	aiO^}UOr곐z?#'Fm$:{PF(<oHIv`]=~[MRe:ChA%S~:[ 'fxۇoOAnDgJYI,`ޞCp:(ٖ%;q`JѰ3"Q=<Iq_&}q:xF[߁ڤn!8-H7}e0ċz+gI.F\~
MW܅ӉKDQӗp-&#?	eHPACӵ|a=*u+Fl,NԋG)(?UB4*
:YtqyxռRs{V'6whh+*)J
kCbEHJ
F]C1'9'/ۏuo˱uV.οng}};j#s\Ή陋 `Sm"gw(`VY;վ?L2k!'I,YfURorUx4%>-7o..OO.LpM?.{B{n{mI7%ic,\ғ4([*p^G
_y#S=/ǰ񏲽#Q苊JJ|E3^ΐG7&sw
S#0@tݫ8¿IS'#hZWV._%k_}K͍hVh;{+
*?s*Vm> &'z+6PG<8&#bQ}#=|5mY=ofmER ޶wbZiJGc澁椌ϗ5(i0!]1)Ȥ3ՙ-#%˪SX2h3,D ,z_{yT8cdByGR+_,lNERAE}3CW)X~RWA?x&X0&d%N|,)jltR)A˻'t_Fvb`$M!ť.{oۆh@w aZH֑m=d4D']1s
>crC{9gt'r<e1UdP3`+3%9:֛-YM<Lr'`)Gp!sZlS`~r=uqo455-<dK(c8`t_jnO)!ֽ4L;?TƊ
*9~ƽV pA6nT"^cm	|Ft&z2kޖw 71-66rzp9ƷO&sixԩ,oIq1睑&֟ ߳]/xb̲MP;z ',m1>vwFyTs7;pڟR:Er~&JILiӒ
+8X2Ӝ%*}!*_/)8#rR> j}j
Gְw-6LKMlxvW??دGz4?7ܭF^c}1QwppO;"`GQ{񜕙p?ßvڐck_Q"g̈XE{wՍfL)d!V{U`ŶpYj$~}؁2fw,?=gc-<1\^XݗՕqr-:*.?Y*@JBױpxԅ?!8\+8%  IQNt.ORoAO"Ⱥx-}Xc7,1ױ殨v,xxtK=Q'N#yF1[HYEbq%H<3+2"<Hh^\ oow_H_;w2des9@-kT0\̔{%t>	U.آ [B}~A'w%#,ahWy?cy~WT3j^9uSMqH?hdۙ,΋V-A]_E7)><OGCHOuHO7z2+wN>:=Vsil'%şKS;T^eQUkՂR
D	.xE0mÞo 9	
u]af1/Ifmv O[cS*
^+5= 91iEy0?0zS^2$s%AO+F`\4y\ḾGi?~S~qxJw;^7Nn8XybE۲AsTPA1~yNMc;ݕT܇=b+C,FyOQh<xiDj+\&
Ɓ8Jټbyֶ4wr5y}3ocD(<
3	WyWC(&45U:6rJ}&3Ok
w N/Sռ|9PMP$g4AG Zx4<~gZ	Z!q5v1RY6
A[#nhlQOrDca o) 9My0VKCIeSr~F[-'0pW\χ:t.vF)H"6u*>&݈F QR	.,/G}Ɓ%Rm9SvhQ7$)tINyɏyNC_Օڤ'0EM>Cr/vÛ)h'h#)|09|uRL/E+/?d#	V1;[H}YQB1MRy=Q)Q(XH9xIh?<BPp<EMoM~,ɱ<S!e9Ly
ͨ:$s0%ӄwʍ#,1!{*w쫚堇)咏vuWWu<%iׯfꗿ"47ma`8ci{Lz*zi3!ZZh@s3
RdoVOjJRn:ǝJ@5CO	Uw'CJr|+x%}|	 qbġAl* m'ǨxHZ<V&ҁ0	<`g`b_!nH_}}[k eh#X5^&7ٳ|kbqx* ^u~Xed!'I@]mOE>(];8*zkx~DxK8E
Pvp/w>y҈봄EՁ[Cs6R|[
X7o>
;!M_IMbf{ٓD
Tt>u/uxpˇm`]ee/[z˶M{Aމ_!JSjqBn  İ/S)N;ӇRt˄0j^s~'bұAdiO~uL1!L6nM1|2lީ'6Ҍ"tLܞ6|:D~IQTAT"#L;bIQY<uޖ-гk޸Yfts!w *8i0Dpy AO:.ˀ%E6%x9\kI<0}`(銎)ʴV2oyw4u*d营jS-"cy}х%l]( E2ôqndL6Zbm6inK5ҝ:8M1+#)>ء!gϟڣ h߇#m\'*3<x3XQP\{$a/p.=HCQ:yHVqe0!{!P}9N\]@y{XjQdqI<,كp Wo8zckxlH *t^v2nQ- TàڦhMDLe'fNP<H"xpuB<YL%aùz,=of̷6Y5d`O@1ԕHMDN.;xMU_j7ENR@!5yYT{bz.tM*]s8.Nos-Ojq72|nhp0.[Խ0 Ck^NtN׋Ss T@?:(Gb9N
OX>d}iU4' LT@6ksFoy2 cc[U9 7|B{`#M'1Û4<ɸb}BhS#ѽnflu!zU =hF	ӿCQ卧6ȐOYn_MZ7uS)yW?+J
q6{1
	.iOZA"螸q3~O#( 
aN0_jl63ZLl>ak'ؓ|0q,
S0gy{GƟ|JZgSI&_-c嫇"70Kq2,bHց2o*7vI]Lg'-5jc<zB?
acz_k
\rA9PƆ6X?QIE+˘= ,3S	afnO9ϭ&܉iBm~1]xiu>X/,joJYLG
Ҳ3ac1%%
Pj	GɛYbێ
Ǆhbw4-'N|$	{RW>]8|	wL'0y;H'*Qnd5-$u@*A>h5҇Dy%s
ݙV:sQ`wޤXv\*hz=DB,#
ԡ<;	4Qw1UZ||:ϲ>Z)u_/b=]˯_j~t>\"ٵ"Y|yW'39kVA\񡍨w[ǟe_9&s,?uIO(nWZfy;XWO"w`n*W{9:Dm	pLYUX`~ Gx/?
;krp-@h]hW-j;(̇ia|D0fifY 1I 6ٖg	8-6͠LNo~,t`z;(@mD<*-i=UUKD蛿]G
#ICh llzIs	sbrfEi%q^2ˤs8¹\:Y]$ILBzP(n" !`.z @`%w)Hy295ʽ!UQE])M%"R	/LQ+'tTB?'af%). x&Z3W'?$E,Y<ϊ1*p-j%Rӫ8S;x/!]s`ޢ"-z
+f.Wbz?f'nP_u[w)OT)Sl"\{ӌaa%AD/dDXf3%s="bǭ7}Q+
ϥwt*GchqM
h;qA])&8P(D3O$*^!1ȕd`+	3yflOLɿ\=S@L%S󲮞a+H9˟Oٛ`HKb
$E\4wZ$bXW3A@M[ܠJs:
d5@؏nbx^6ѱ~"wT7op_JxJ!_&VkC ֲ[]/mcz>HRIx\9,Dw&W5(W-ϦˋuiZBlu99ϕ9PB'뾾 Pf%x)}9x|2ЉKޭK7(skL["U	26{k4ڭg<Ш+c*2̎x&5ޕ)AhYe[ $7V=ڱ{Ǝ֤3#pFhB2ݏ&7҄az#M9˓.f)0ގ0"w.lv!3r-(Y֖iBj;5++Mt,Ur#5;&T]˒ 0.G^)xroj	;&oAo{xqAwRtr{tnD4A[s{?^Sht:Rmql:&=))~&BRV?TMБ5	:[s1uʉD8Q_Tj%\Q}^/Z_RFl{yxdjwQϛhԸ3Q_XQ
gJ03h4G@Q+BD )B6Vh*l6i)L
"»7l<:ϟVIIyiQ?D"	>巃]l4!!4[9ҾbI5EZ7p"%.7cRg'c^|+)tl2ت<d:,<(0>8&Tl!}ځieņε<{29|uqHX8$v1=x݀mcEAsuz>*D*˲h?'D&g<n1ֽ>ezuyzƹɊXuQKuMT*"("wɮ̋YuO{n13cȌ EwߤQ
Z5; ^(#lQN
2mz,yswNurt'ùK&d&g	Gb
tޜƱ`Y$<ugc"Ô>T|hUS4:o8~+RB:	Cu'oϢo}h^Kv-E۹goe
jʨ@xG;H޷IRvYybQF~0/o)5<]艹|75Y(ttWg63vlGfSV}Sc𾑏<<M8>VfG*9tm\UfRvx9!J+FNдJ4dKcI#4U]E&vOS%6d£YV,2Z_RP*^1  }isIr"
z@hwmH	I2IÁ&; E:;U}dUe7a}ά̬< eAiK-gUOyiͶCrfgoWڨ4O_#[vIt<'s|֖׮IXj!P3~kQ:8sb>^4c5_>/~QÕ5N`oTb K9+>_ kyA͂@tW£K~$p¶,F]-ѼN@ƶeX*rE+RE8j,۠q
>MdS&NM1^5Q7ɹquYeZ`c֦ʜD\eX4yb4ҳz5k5I$J|;jJ@[qv'v-
d@FY'hҪʫ~&Љyme~$\&%"XloS;ct͖j-@[f:3ө/@W~'<wxtQDoi\O\ٛAx'e׮kkZC)=TAmowov#0}PX.n Q)Ġp}wGpb݁L8głں_tּ;0؎G 5i}[1I42=5dC֙w9t+O4x)3J
W<OBC04l;,MieM_cog3M@<P^)kaN.H >&ƬgnLc̀jQ{bn
uF7wymq+4"Rð??ݴ-bݴvRma(^xCi8D:ȯD5Q+'3^ARX";kvx>n#?#B?	S#r8n3XJe͝!$R\clU;!`$f7MKZkQvIj>)Ae# jkRL7@tyhxwj˨/=:jйsG_+#_mVx
P]ֻ^EB&&c/o}uxjiO i16Rvtb["RKoOчgÎ%;`_ە]RApU̙	
XD
dB.Wp8O3kkTi[QyD5Ph6bQ;LeFm!O%ZQ{xX!r>(.G$E)J5.Y4oUL$Rc8 5ԵŹ0XPvNme%3vcFyWu1e2񇟺;Ϟliw)X]iԁP?zRe5.mQEJ K0[㺂-ok(e5ݑ]2zD?XYWCekp<˒(Up;vi&$@Y/?X&Z܏S{on\[ZY:vyO~?}=RC)"*ģmuc&ݛY8V8FG
0Hc͊cOۜB:_~fcĩB(>O^>+ơ(Mk%
Xխ$iտ$
џRYx;vO*i2HLݨJ<]H!z~)Sӱ3j_<8aʽ}0RIUߥTX3DObJEeO@D.}5Cϗ+;o#dwyU7/ >Z$yx#f@^HfÓ%rꃉy=^>S˧^>bN! U4A8Sɮ13^VUݶaX(#/:}6DNлY]^x='mE,nj˴~2LAԍuy7N_@?_X0uCRWΉ!fIJʷK~'D'Yeo⮩aޑ|dK=|\ORǐpwCrJ=
e^2N/Y5Ba.lL].v*c뜽3YFH9/Ѝ6%K@r sɺzGiga,
[	~qyv!WÐDuD
´dՆT`f>v]͈e_qeLL$.IǴ2M|VZkiRR<4{C/V'>Q//JQ+7z%BW?|m}8[^Y iA&Lu]I7&{$5`F_[zi^@ ĎRdZyIq]D]mmc!ѾFAB|+Cx۶-BKv
B«`};u`LWs; M'rkjUNcGF=uh&EO0C!?V=oUiLNtft'$p	@Դ͉TJd35<pZ*ñJ#HHZx@&y7a`LkJ *{d2aQ;%6`ob -P1lyD=(i< Fd"WU=Ɍ|ػ@81$Ҵ\n0'ߺXTcHXyՁrۉS=NxLH<e⋼c`o1`I[AJ̲֗<jMrBQ4Jkug֟^7%ǯ]痃%gL~W69/{ePiE<x/[濔fdGO1k[	wbux{VWeŃcPx'z~(xltC7}9p;jqa2r謕e'[v'(N"q͘(
b]qKxP<!tuXs3-x˴)CJI1֡}tG[c[僳hvGz?m^xz[Lw~?\p\?'ɴ..H&ghv9D~)"qM҄R]يJ#s<.<st^pJ棛vU|8BZz^S)
 XIJb|"O<l $N/Rt1/CvO 3<[&
dneiw7LCt&GA۹1ܑIl:35,H5	FIqgi>.y$W(R%Pg3[V17o&a0Q1~QvQe:*:{{pֲKCqleC]s8\uƦvNUsϙ~<e.bðpmXLjy*=>\Q`VŽLETRhW}HQn'[=&EERX[مxT h}`#:tGZA;uREԾ9&kjFMXmyv~%$#2̈EW6dכ }k/P<5uvٮ1FX٠]hO~@
=ʼ]*#wQ%5煰5~]>h"JK||5H)J^ZXd+lԡV, e9He:}_{-"*~iOTm+2֔
Ou`Q$0`i^]:LJlCeƵEu5uȤ͠u(ZdJן>2"y1k`8lld@Yc~amccFܲMho9`	N1D4p_U
K9N~.>[9A<M-qw7ɨ͵!2[Ɋ\2-ܛL~֌ɆlV&NHF<Pa+ZM\CQf.QGwb٭KJt5V%QO|	uL'd/-`l:0!;϶Ѹu6mM>X;u7݈d Vẁ,ĲDY	TS
b9aCI 7r$ıd
õrlS慵%H]z%︛/b
/&.%ѫ-["r1YJ!2&&ۮꌛnl:?\L:/o[N@YQe[vɿnھ[<וTT9P	U'!\8^VϘGA+GT鼕?4".f]B/nb|6/cv
ߋ z7~cK?eQ!sڹVqsB.@XbQOn&9ﮀtOεsY%:'1: yl2*	pd: uկ;zZ|俘ڨ<yxyQZ-&Ǡw|ΰBu|%6Sl}1vww1/G]lSX?{fm-!\Ztk;}ģӱ;
n޾	a
U~܇xm2⤾u%kVwɞ1*q)gˢZtYOuǪ0gsK
٠sHzn@9998r.V}AMVBB<9z<3q\
7(HdiQz6*צj!huG|42Nz>ӹiZnӗ<!N\*9w	QQe^JT%+nAZNZUP[C:$$՜nYʌ8gWRL\pebC
mt<{=ЇjG{	)
l޽w?8<>b%
PJ0ykeۏL{c%ՈoݚӋD2*x`C@'eu!,J
BGrϘj%aB 
;6W<^.Rۤy1R67=)쪫e^r^LӮf5)׋V!Oa}uFȨr"5΃ׇG룽7_6&攦J
Ӟ5zpEv9
x(|Гz<
ϣL|8wXtAH+UJ:*"i˩Z;uJUb潎?F'oQ(5fuA*
\K*ȽۡdoU)>4bmf	9`D3 t껳.~3uܿ{KfJ>[Er de|gEKB!|ꭖDI4 3a:V]v^,)mY-fϹY-L}~ X>{x]P_N5&E1m :BNV
p^Yv_埶ٴծ4xyg],e]+~Q'^S7 mFiˣ7sD*s Yl6]A5bW'h5`桏M?6߲1};O2jz>q0wsyqyo4QfX(Ok)[^SJNrX7 <8%s?]IaXZwf$&n&e %6v*v)8 tƗírkerCM׽'gSD֯ɒz~"r:s~b
FV"
"dOؿmA
DcӥWIdx7p3(EިF"ՈiF=LMIW3G;x5LV6%hq,%ҙԿ[ p08pw"ׄuool#Y$$4졬r[ w'
\
q-6iJm|<&=6vfJ͒SVoWSJ	=f)mE_
|U勁:ct"VVOZv<i-wyAd]ETAJ8dE'G{8Q^<4=c9OEW6/<G1cgY/7zK_~X8i{c[Ϊ?>䝌vf/ٻ
Slc]1#cD1b b"rtdY<]WQ\^_rLlG|NvX=l&3vg;
fҖT[ehRV62
+*R"0dB*8^c+gɓ:gP%NhصՃbM[ԇzōv|٩=CzZB3~$LT5<DQڥ}nz~"n¼Aܻdt"D1"WĸT
ݮ5d!{ewk}0-JXN)׀gh9RITozryI%D0NO_n~UջxtR5yuHoxTk	,jU7)i}4(ƅDÕ'ھJyuR^
$%[̋5}6BL8cݑ"p7[)?n`nyM$TJ+#7{I'KL-&weLʵ)>x./*i+Fw*6yE|AO*E6,-?0~{HCЗ@b
z`{ntΏD_DrT(vI>K~*)'bgB@8*钞.<_5L$/rE+El@{;`Mo6_z/w®P{&$p'^@N3EM@0KZ[.5J3[8	Bkŷ)e<vZk:({7ɯh Ogl_`ɟ
BXi3ei?{S t{{2
$*M&a0m<lE:foeb^H齡AK!Ic|{ISx(̺21ȉR$[qFqLM*UGTqme,
()VMiXNTT.ZԞn&E5u*m#[[xL4I"Қ{YN]uˆb&0=RFel5p߶EN]=pP?,#˻jGE˗.G
T~~Eg4)sAϵuTƿ4Ynbw\# ]ҞA8h-aFh'q"̾<a/	,z,twR"%K)v?R]o$({7]$b/e:	?d?礬._?Drx i@s&s$@ú%jdM+,좘t0dyD>1!dT`DHoK5X$M"F]PXprr\C+^"PV%gMl*<!7w9ۇ62C{ǶBu%N[Hz9W	zz
 nNQmfb4/ih.c	3\݅ś%hSrtW΋Kы{i2V	7`as6
^w.MY|Q.v!ฏ֖ոg? =8'Rh[h
?5>hΪb,?T&k@G"nDru6$sGk\cIYsRKI2
F1vHdߚˍ<6v#/᯽jQnQ<.'#tomEm^*Κ7eѶ*&1QT4ޣܜ;O?JXl&=D!_3*xO,FgpߺH)0 Ҧ ad$(!ѷV'Wi1MgE!
ZX*
_rLWXT}Lx`xb"i5OL4i)GHUBoEf nf~l3*6®:spë+JQ+7}Ff**Rmu!MPx|x[u7wD;m`}H
!wKJFcKCy솯yY!0i2𒨁ҩ{Qi'0Dќ߂)SPVo^(Z냟L+NI#~0B
1玘(f
#H{Z3Il򩅹Ryv-+HE7J>*<Uʮ~
!YlKD^'?P?N8"LfķJgC\cF6ղAT'$7A\,&TX4{^/=V|X}no@.MJ&LFu˖up`
BR嘿a]zɁ}rO}La~lcqfT(Z#BhdU0̄P{[Uh 뉎܀(A׳q9	k)$QKs( =fsD{EO&n(T|BZ`dgǹ'
?2qa?AqSO<v
}~M;IrTxl=.3z87ѰWHRHF	W\`W%>4&y>HH.S9ei""5ِnpfwpwSp?;wsT/ogGazhYUGJ&FǞkXYi)f-V\L*e?[-GJ}YM\ɧ\/)ONn_?Ϙt4K F}l
 ꢀEDYD;/TG3r+CE~cuSp+7po	hT}X7d2\'{}lciWvMĦvx8i.7nن;?lM↱E;7t{x]gmiެt:ӅbN$&ˠ	u0ip3nT}+!CQ<5(%)&,`	d6u*NW왫in=O4r@[pVu/րeI6nNIݝM][nR
i%{|qǥ5b ΖBMD!Ph>:
9f~-7bySfp@1(S5v<eіWCHTe¥G9`>DĈfd$"ˮJhr>4Ul {kJb6b ⚶Hֳ\kƋ.j{QQET3)9-&AW<8MBdx]uΠ‌[YBFfU	W'J|POwB=>]T.\wmaǘv=|`.-;/9*
mzN0Y-H{W7c
BΣ]u,RN $K5i9@BSIwX<FŦp@x4$5̃a jcecb6ͻya_J(B\$$SBjl~޴	e.|^x>P`CD^W ǖW;TO,iT<+@+Q	<nZc'eoPJxKvGYp#Fnz7\ˣB"{m.'%4B<XyNfHKpe|h͠wy`VT"BGcJ;wίuޝ]{?}}=_\G ϐ#7Ʋ an,J4R
+v1LUN<^-u#ޜ܅$}
L5] 7_IƶN"ގUmCYj;4(PfSzw&@YǨG\rn6[|P_`ӣG鋒%7Qlq"<܉ĮSph`Ay.39ENf,
*Éx,b<teu_]sy?
qܧ-W̖JmI>B7mTdVXpGhs)ZcBklb0Kq0zJRvR:ގ'I`hh 
{:3t`h~@.H<6m8K&ԃg"4I&IoHJ,A
~x]v9-rfTJ]T
Fi6->-NL#qn17p2y?#8Jȁ8GW5e
/;#ԆO܏Fom}
-,ja V%IB0|6ns뫞?wV:Gmy8JH*.ڲ`%./S7e>og,-r9/>5zIM_u	H"ֵTk8f`ꊞ7(q@Ђ6*BPOF(ywZ-$ Ԩ8z `x^z1/Qy7.ۥv`v(ʝ;a%h5Qa~7:`p1f6HO(x3vbv}=)Y5
pm?J7׏[8XΦy0ain2 OCꢴ8^-ԾFsYCCYQ'|^ px2{wwֶ=V]DQؿNߛL1dltb|Ӗ>GDnp}hMi0뗝&l5>xGlkѕ[<k	я	(K7jiV@`wu
!v!C ,>^[Q&us_t1/
foضw9{6z9ۃ^#(\=*;a8$L5Y+ԊTp틤Z8aM, TKGdJLS
#MSR*䶓q 0#Zf7aؤI+3
&O2 yQi;<L;SK=N{z2Ӛ}ˋ,֊MF~=Z_ّZReY5^r	A#,cfc؆<ŧtVmv;9
`oÓOy[VM$GB6s,Ӣu&JwB3akz3u	wu_[ $LDg Wobm7$Xy[18jg,6/WcY٪b
kt-nu<SkSF6y@#Ⱥ
y$ [5 o7 rUCޱ?Cé_#U=Ֆ~+^)aS[wQ|}0@r:֧gXGG{gŻKn_jhw+#XS#cjr.Iz<ΠVHt֘`\ř0UCfp^ʥ%Ң\L}bC,'/	c3xmdɬ8&吕sI^`1Eȉ~dv*Vddr!#3to%uf2ꋽ񘁸ґϊy9c55d\?FOr1}瓏QnheIEB,&;Q텁8x4ܘ2&#odqK5zA#Rh1j<\+aia.o>)j_3KVވq{gxBa9Ӌr+	
ִCД[YӵحݾIiZmTI50fF=fޢ
rٚ'

Tmol˜q΢\H!G~dƆ俬p_O|=		 ƩGз=ғa@՗wYwld}RON͚y4<;OmY|axzE lyIh~dѐgu@b0XBdC(6ulp7+A	M1;cu	WXQnqr(l;ך}l>,L2#M3aR8XGfS?#)2D+lfj0Api'4`xfv1:uw]C8
_ӴiRdZ)aq9;f<=a=A+u<- JѧQ6 qՐf\X\[t(}ݖsƪ.8]'Ne *F:R 17~2M~X_n%+qqvX
i]g4ԳH$ߍN"5],cKc2
R񺜖M9xjm5.۲
2
/UPZc
rMKzqģ^.~Խ3bxbǃK^or֒Uz(GM\,G}%IxQv
kyWXg* ekdY4*xh.Pdz&X 8P/,*@z9iT8TɭѩWE	c7o& #QCE][S4#|E!o;bq:݇w	fSmd hGMlqp3t6HHV:{S*Lx2wÐWQ""k{zBuv-UsFxGy0F^4*e-u>2B}A1~.g۾*K4˙b
ﵪ
^vUw1<kN<?g	3"=$խ/A{ĪbOadTDW>6c{ZD;f65kbfƫjM󲦦e̬@#.'v@* )&XӾ("W?7
Ӷ\DW[c1PSVj^b.mz5&X;Lg|}Aj&Ԧ$8Fin#mS$!Kuz{EU{`@a3v5ǘ9w	<./F17f#'(7E8_0<H76	8+߇]R\dphs'la`7o'`y~~znLrpxuC3XVܣJ%SALL'I>
ӣCU&V[^QؤOiS>ɩ/|Ro04[E`wH22D>bSRfd#ė[q>Q~ot)&X"*m
GB
{\9%ħẸuA4_%?-C#O"&8Uh{8좩kp@Yo%G)` AځQ){oໜJDr4w}q5Ġfů%quOE׀{8Q)BH4)99><U$ܧ$jpM0Z+pCYE,1,P,ÒU^2>_bxw7|9ڬ]yn2Z&w:ղAx0 y8g<؋{ rS}<r @A#,PPJ/8ߗZQg1u[/VbK}+j;(*drK=I[+?bWal(։]2kjn #B7¥evQ^O(&ΧT3"|UNcP}"3 Yޟ5KnjLm'3[; 	lոa@ JY	E2~_slFsޛLO\9j!b>N#&10s`x y P 9^x6ޖc_?'O`[L0F&o8 Hid="SK&_q='"$  V~(Xױڅ"(C_iٴuXi?6aD܆m,=<@RM$$w`G|Wgw~#aVCNl<d`+!/GrpfdPv)\Ώw%͝+B,8iu- +]Q̰Lcp!@ܫ\TΝWi_KӗjOq!zKa,̇jIս[0NAU<P}K}$D
ʟ({FżӝJ	юC9  }is-}xAYX"x7MvUwWOW$n!q&QGK{aX# <in!0@(fwe	S"u}SjNɪF:ʷfΫI}MBfߓ(q{YeK;-/A5;W8d58rvd1q1O΂q[lV|Y?'Sg]  g@ZVY;x#~#{ۘ5
}q<K?!Q@QpfmdbW*;】+066 }
Z^jWf'wH:Ͱό*>ufYܠ%ZSǦɗB,;]z^<<{ߍ˘|sB!X~(_I%K:c\D
>տ}h%TDjK0^ʪ4%@mYYeQPYudl,H`EOI<r!VropYq=x1!Ldx3"7="+O~0>;9?5RwfN
NѪY! r{Qbo6z*eai:/mM<1=.zZdۛPLkin)/;iIR^/C
bkaѿ8N30g͟
G{307ZjA~T)_h 	I
eo$gYQx >$L9TjpOGOfKhKϑ]SˊFnPduÜߠ/(	|NrZfi0
+ w:ZcAPBWY ,EDzZ.cnZL38$2.ap(B3W{3CBf3)&xKtd7Pg8t\uzwC^yO@`PP>-5bxVKUH_tm@S#
ҒR8P 
 IroӍ'0~TUk+
eЙQfkU;%oU̫RPR׻ώ{k =-ڒcåFI_vbwr7ß@I>FnFT8;9PZk+HeZ[Oz6sMi<ЮrULǐo9vUzq^$yLSpArq:#Ih^0wrvtMMb`@*FjkG3%ў-BFc˵ُ* ,n:=y
/~p~ bf}5CȪ|5<ꔶ@V`buƭdwJWrJsh/7Y)98	J SάCs)8,>Qp}FZ/}7:f8@t`9u_V
C1[
S-F[.uḦv#bl;_Y݌TG#OXvlͮVWaV l"]/Kn8;;bˀ'䶱K N7;$MIy'OB0nN쨋MlzWk<ǐj:d"d;)Q(BBz9pW#d"ٳt8jrlT_q	L꿕5X]j 꿞'ԟJD?Do{I6P&8-$XL4D^je`Wtb!VoidՔrh^yV'܃mϵ0[/T5t,ڂ,&2)wQa>
p9Nt2s4bqP-qZRtrͩ/ڎ1_c|T=J$
WsUfx.?{I.=)*xÏupBp=09ɗ@m#vSUuY[Y3v_%ܗxlS-baܶĐ#y z|񪼺na8{LeIkw"ߎ'3L76XPAhxZ*/۷.cTZ*}->kY.:pɈsFBmI*<Mt6^MΊqakb_
TxpHYNxlnI#J*e=Gmm߳-~7nǙkFW󓹳q:-R}mto ~[	GT)!k HZMŁxr|+?0EX݄
iICMg"z%\%Xg{UzQ67h8_Ȫ@?UPCEE CA}K"x5\L^q4eeeeRTʤzBiYXJmIPڈ;-˾ȣge3+X
66W/{D^GgIu>l7P0ʐ]>&?1'&q&@my&gXfNٍvmh7W/E>NɊRSiu|]5ըM?zΕ"K䷟iWyXL5PW>+َm& IszeT_J֑yBWT_fz|}G8_ZCk~	V!a^[Q^PSlRWi1G:aЕ9o
ϣTM޷0<G{}ƶțsS<,Xqcz_#O7`Ry:2Km4skv4L綳d!뒉xmD5Mgv%<=mkĝ]{5aG\H}7:zР+𻸞jDTyn/Ɉr^p\6L:n`_++/ܸ6AASgs:K+=|JYJ)~:2M
;pOTi$ˈGSF3 U{M3Ei^EnB*Jtad^U[
H})tzl=/ο?;7Nی	k3>?ݽ1h?qLVٙ3[U{/cXY&pgh-Ĵ%l۪VZ	H. UEY4'I՟lPxtM/R&g]ŵ?G0$!Kk-6.=|nni7 x;Q;݃hPӞ"NP>}ru|PT,:;DvpNB*@vK3=LvŪ*릘-FNƈJf+'Z1=2Ғ}άX\5'v}<
X^*`-J(n؄!^ @qs,Vg݆
`f{޿=yh|;÷e(B^M/e?+Mi8% jx]NE	@<nљA)dZ_WWrV
b$2Y7cv`	nC[ć #rWhz$_STnwmh
]āJ5e4__s@tJZU ,_ }ǫRFv/lN*+γ	1^ĵR^sGa@^wuNKȀcl!Jeד貹	bLE0<L*N&d$K``w,}'aB#XXs(
R >v߂8؛vGmZh*hJǢ !Ìn[n"䢱҅ŇEiTDXuo,1`s,Z~ 
OV?WXGR"dK"0a6 x'<<.1;bM~8UO/ NܰYńb6Y&=<PU94s)鷿'P!b3Ҳ[|<A͗Ͱ7Ӣ
ܛk]\=@	 wSOoP/uxbJX$cD/lq?}:>|APq[wY#>kyknJnYĠ}0grݑD
ǛϷs;L\\4<$?g!.س˒["/2A/a'Sq%{Yͼ%p35DMBT6ldB1$I
x-gfǊ~Γ^]4<&oqSˆ
Hk~ZjdX迒Se).XmKE
INhٱ=(//!CQEֹrro{YDEJ/&^ȩRo杋je'Aϧ-88O  |޶H*5y9£6ebUq&t`~WNZͬֆZ-8`C
aO@Y6)yXB7ecnɚQN_?Q}7UQرER;DbHjH_?Y}b7xdJ8-]gFo@hPdIuk02n!]<h6흕Yb &=+WwL=~CT
nѳ7G~<A1 }l<nuZba'
{W@aaOq6\	CE^911SƟ& n6J*{2ge,!݅/k{&!7X}О
	gVX<'>^T%`i1Mv$L7ǰ-(q01km=Xӑ=#DH"7bJo?@F{".hSu#@$nƲ{JȬ+`fm2ZY#/Sk/eՕǏ2f<x\g$g9I||;2amt[u,4;zKw)g	= k ^ގ{s堰s;!yA8WT~bA
bpF!_aƢf}L(kPE1j(ţbt>ZݛPS6IEO E"ƶBA{PwlDT=5^_XS7P)o{&
QWcB'E[1p<;N]
֡d˔I@no~g朁sSf RA~dPro^ޥ{Bi)SmM3
,&sFcߡP/.'hͰJ(f/bn4#!pSɐ$|SJ(L1?ڛz%'x8'ld^I=WHM>yJ˸z<;JXVųV؃Biōi,;_2@EsrWz3&.W\^3.wu䯱~Ad
.Hą/Wּ=	;d&vPUHKb1e{u"{n9Z->SY;IƩ9	tP pafAvUs>Er/B2dR<:Q0`6C=rǆ?
I޷Xa5\#F'dfuClkUjѡ#yu*4N憞v>J^)bpz:NU1oAkϒb\\?_K^5-ϣ`d*ng񾀵m?)KDɣJ1;A~'s3@QX6a/3D[pF?8񉃣ǋ]!BGiCLT5/;{+P
[o<:ntVFCy'-UsYͦ<6]p{tp9gs|8Y;i8'w7-g[μuGQl1ժ9rx}=Mj' &PՉ<<Bg/SCr'B8pQC`FW_I3^L|wfs xb	-7gWRdT(xIL#lba xyq515Fz%'L
 5<Pw~I-3}%/B+;
$p=xnL@y]ZV=|α]ΖhBwX.-D&*/ (wlÔU-[͛ϧI̠o
EuY\8Pk)C!)uQ׭L!N3]jyԡx}X	-iΝGѶŠ-PݍdYt\l8NJ<1@I|ةEpUK>zhB?~,*O=%>Iwj=%~K|}!'f		!ɟO)>)	it #1c3zD|s?^+6qPϐ?^\Ϯ:R5σ?9֣ /_g弄`X,Z![㫮QÐB_/.⤸(ʏ]ۣl:R-:z-Z!(crוNoI3}ϕ_8~do|'t{ߚ qH'7J,V=۱*B1]x{*Hl@&,Mdꖠ'n|x!m:s+ᷣtiҷOgкHO0L48d
Z;ă.ɮsY߀ʉS╼ZB}*&2.qc4v(Oeҧ	𣼋N^($1zvjPYa o׌0.|fI/\.n]R{I'=h[yE)otb+Zf	?{2csԺk n1aus>^q.
%I7!C-^d,@_|MXX.WlLm<ϖFUʔ +[/!B	+kUHkkUSX2s|8^؟16N\Aب^Ro!؂F8c3^\C~zOpby
8yflߡ-:x#qf
z68J?T=@uz0NiMƫAU! 1evMތPtڅ'I		=gMG8yNע8TQ$>*,ojn
K~bKH%9	GmKu.ZF %k{2vE.- a7{砪AbE]_bDz[6Lkn-6λTq%_ʳ&2/^|u-8 Ugz=?>
uŻ%0AƆ9D:*'˘0c~^}άi<?濌ϜVd%ے %8a`|ޒWx&iAEe^O]ou6Lyy̣ڨGi,J(KwJ%FeD"ø=Y!MUTdȀ6BLwf%7sْj	?SURG\C.Ekΐ)GLdDXWè:_y!Xߝm5m`v>~[w 6Sӂ]O:/zlE|'@XWf~((yEJ@jݥ[xU44}q;0
h`
")2ߙ Xǌ5HFB} (|{}YutWU>N(/fw[uZPȠ;MBT	}eE0- %COuVǹ?]k%¤mI6KiX%#iTjh\^ɁHӂg6cM5{z{L]4p(q
=)SS,4ҟ>LLa,qw&j⬪@k	цK_DrVjNI=$/"%-!'rD14d<1qkN{(&^ܖb
ʐy"~0=]]xSl161ɐ{1
j#y_m1D5}WlrUD#yU][O,p7ò	:`|Lh'ǣfʚS;Jbm?4
ɥ6K9t4@#UjEu=ܚ6dOm*RAu.\:l
.=CpE"	zݤn+ID^Ɉ5H$_\a)>7iܶeZ?.\b.5#`<	ywe4{gwPKhqYO
ť5
ߟ{<~aAߟ_㣷*֗J~L%hT>x~,7xSn'	?<8?r_ᓎZwʲ,S,(ddRYX#nydQ|piVrl痰׋C+T?꺦[c*W5w@NyPC/{L?iSm[)vT+_l|Vj&򩁰
$C/Liok2<^|7^\{~ACwO0_?4Q)"]<l?uDu9=ᣆ@LXav8rQOs=ŋ͞כ@?݀TiikQy$|ňN|M,A[H
xmI@:י 2/%j̓~	kww
;t鴷vPy	H;H{:(z	 bc= mȡ
JA{:Y
eh'7(ݠDݠ؉ 9 frŖG(c7Ne 2Q DK1)؉ |W%S1<
Y<诿Mޱۨl:qzۄZ;9BY
ii km|}ϵzv+ޯPzxBIQ$ʆ{y
e X(#d7f<edp;]=%s
YbqYpNT\PsFDl/eWƅ<sY /FRނ쳈AikYv~fLV~@$Y7|1+uU!OJF{0lUfOh,;cGPw]jkVqps-V;as5[64\giRlLrjU,g7dat_o?7d5
v}ҮڮfMZr<-r3S27UUk?L1Y>i[q7񎔶?w?v(Q)3Sa\Cڟُ]8򱕛
ݶvju(M[Nx;_`rhy1A--Ʀ͒=Üo|RpF.&~
ϐ}3	uUoM +;mU;+	7s!<uoJ=42VzaXksԆ{%GĀgJ0!n,' KC	u\ҵpq#Ert;gJm}&LpMW5)/vhSש֧[ az'Ӹ*4dWnVolmg$e%p9*3`R<V-ܷsuYl=WSwWbr`x'NǓd^G]Wr=̰yJ|x.NSs2}QR̪6;ܚ
IvV rNGy»P>)%S[iPLPd|*Deѡ뚪M"
7
ƅWvtrzGi[߬b]R~}_Z\߿´\^r#V3ýK7{:Trʹ	,Ȩ.f&P"R̊Us4/+}4$Ǉ!GGܜV/")mC W0u@R\ib{r=6_;Xw`ռ9GFL.~[ԄycK.U_A8LXh`řD Gi(BYT"TgSBgT2eU}>܅EȦ	PS'PrsB	I=y_KxXT{ukF<	贠hP
nUgVp+p+J>$CS+NÈt%TL-W+*?3*ҌG><m=yLAxÝ"wFPtH*p&fϦϰ,JTM=Ev_=H b*c8o})[,)!<,RmAl7@v}d24K{B8a2ImȒg}A-9"[ڻd9ALmy@vGHX(P`mGXݱ*մX;nj6km_x	
u14	VK+a++\^("!Wȕx	M2#=5
wfH@ fA?,ioDDpì+!*?H+PQbq.WռRV1*y	Fp6bfv=,ANf'jX҂ևAvsA.aPSOo׋]'
uWa~R1~$fG;d85x܎$pLR膴Y[ʫ㨌Eb{e,[%t;L:j|=cמ )Ĕ]o3*ntYucRw&Fܑ-Tz,OVWXOuਮqOCǩLzR6y:/xΔ5X
Vt=!ECeӤ/@
@%f'6˹s[;Kf\o[Sn[|pf&I.u"\ -¾]cуCOB!P	Sv
:?5MBz@Fi<KhY!21SK(I0f(-^,f	}dr{[;=JMZ1.
44ިU	~[7Vm7yx~߆+{܆JH~[@sS@MJQ<y_,5z"kSюIf"/ﶂ[ںxE=vAG{r1->16d[ܥj,W}i\$1'@۽-HH@\Q&@أHaس+Ӂ`^
HqvOD`MLepe"JkJLbGKlԀ4PL,jFj9 )xz`.71
HwCWj:I rdĽQn ';H
@!E{"yr8HF瑅I_jq
UN)t 1"βꀌ#&"$ؔIq;#j[
7chLl"`m,փALܾ^FP2j^Ig TWV٘'*R̋
opaDA<R&EM<CguyޯV섧tyS^oT4DҧFuzQʼFu;)3ELo_2'QԿ:I(icLK-Ή"iT>2Q"c4ysv_
此ݿ_؊:wi},+39jˎiu&r:Pu(7#;LǭOY_9X*@IkX؎/:~v۰'ǻfʁDM[6{Xr?T-G.R(G,yLٍlb;˿\%RU"*35U6,Hph?6aJ7a
{KaU
Ʋ3B,z5	=Bt-okJ%3-1|Iτ1W:ט*x)nT۸Tr$׷e%rGئiy=(e`u*J7
Eގ&D<e*ƥE2atOPEv,
@ ]F/j%ZS[&	mMu%F-0HG5x
dˌ;Ǘ=SsJ5#WǰxjXk7# a7tkYY9I݊ Ya;Q|=+4==xs36zW
9˦Aƹ_`!T뫸ЦDLyB2U3@j76QEY!zkfpY;a 5Kf6(_*~
,yzvP4qӖn*Pfih~phq$JS^`rH8U^-UyA$]˳hE:Q󌼀ah ݱuЃG4P?qkg?Qؘs~S-Wj=CH'\^`#QVXϕ!b5w:~uo7.&CFd2OŶH)7%謁)5|4c?z,d@Ŗ9+q%2g|MtVPC&j\í,\P ΃P%~i,M!\J
uAtr#P2*wPAlqu3CuHI^i[ae+Sux/%yM>IýWURz>:Hά^ie_t}#rBj=iX^-[#IMQu)A<Jf^W&(BWy\32晊'(\W)pK)AR6XfGx9S
6hFF#Gm6>sPt\ͮ%\2^t#Y#)1
+CӇz$!Gr)e>!qR҃NvNI>:r'!k:Z|,lXx{la+aI?Ч ,Ԩ'ٓ_MR8~,՛$$'(Fz_@UP]K4ߒA̃џ A1R7䧨<xȵhbvu@fc|Q(&Wji{ 
iUzXе53hcb@		D6y(+<>rYT-OYwG,ƶ<z2]!Qp=!{P#o1bJ.^TRE;1|S9R> &
FQ
|aV5hQTh|djP
FUk$EZbF]ޭeQFQ⒯#e }!.Pa,0"ў~ ۡ`	9Cq_׽
<§T"[ZK@KU.MKc6(
H2MJ-[k镹V(t[<ve{ƨK#HX)0!]	Ờ53ܔ*=V<1@u-z>Q{eʏWfYHS|v_mX5Ya)	C
!nvV#pc
=w6z
nXӲijh|8^ d.jswvE"Th?riA ebuȃE޲m, 6cWkPH^^D温1ek+@D4(h<hX  ȳd]oώD$̵m98$oOrIz]2tmw~uVUP鰻tAAXXf ҆K A
t0 pݡ'׶`uAstVr=}A 3 ֆ30wϰ<Q_6GClqIC춍J}E`Tq`v% `LOg5zoPkoMMJp;I}e9$*6%.n#@xݘ<ul<\Z{U>MV 6 倲Pl ޽r]p`c@>{6hL;Nv!!w8M
a6
w5/k2Ox[L/9e}Z7^/f^؈,N(n<iܜ7 cqsd@k\ϓU÷{/_P&W&SfuSB
DzaQ}Z(d
vwGd!"4鐔7nFh^qdlޥ!ɸ'LZBW6.b}DLT8J'EQ~,|pU)9׵_lɚt7;5;(TҶi3+!WZ$Z֚1;xYAglYeUOLk|H+]ea`܋VD:G*K/N(Yenݕ4@y0y-N(b&/_&ȡ<0ӛ k@
uH/>1t'AF;t̉aVqP6tsWvakL,y7`{
ʉWWZU,?]ą#kK54> O|}0AΙ
۹HGV(,O"+Z\jh(WTt)icGۢBqp!E/@ 󒉝I3	#u,'M#oq}\&j^$g!pHF)͙Qt<[F>[1i_n1uC
Ţa46!4?Sy_eKo!p-/6ѓ-&ڼwd*l5ƍYiPH>h-7N#,xꝩ%)J{:lպQj7?%py(B@є$SQ-f8#qV}4w6VQ=.B;t^ @O温["6(H  %RHycuqԟ
rg1ڇ	#A֩#Iʵ5
1kpwGr*9{_EgQ~iY" >iI]
iK5z2-_sU[Ǵ6(dS#+m_͸u 43U(
*mU
fѪLǌ܇"kZFy3uiivp\ZN}mp6ļ8ܠjyyۧB'蘆N3y؛PSQPa=Љ*JPɰ 77Nw;aLJ|.jLcQ%|]9 yAqx`<)#Q?(*1YIV alU`O#PU<#щgmcM16͊un-hC#PヽOO'Go:'4blZxt!&aϪkd? dlx}{[>ȿ@@,~R<vZVܨw-tw,UAvJƋSPx
3%^Jzͻ̑οI
|ޞ2hh^`[fj6)
ק7uSw.M95W%çѼ"#DQ.Ծۖ
DW8w;!ˉ&6z[TaB)L,Ch<JLtE>3//Frl.N[nzS#^<~V|fBҾI#lU}y(Z%7C(/eWU
m6oNg±{."rzv :k|Y<jD
{E]ֶc:JYbd9v$]})Qw/)mc>𾱓숮2w}b$[. ޭ$&}+jc
&7!';1xG(ͧ%ruPCRtW\
oNJ:v]?@dB|^6謷mjف鍱6Y^nյaG$;)|%l#j[62ǝ̱^@  =ks8W(p8${5Gb;'-6ג!)'CE ģAҏamh4Fǒ.I%Ht*hev_9W@@)i*m@Z}E!''k<Fz(1ی3O%E:ϯsy@.cܙ(N2ޫź+=n6٣yBCV2^4gܑio0tZ[t`yb.ck9ƑWߛ$Zy,̄B̅ ђ)	
:[Vc釯i(JW}i__Oޛg(U(6?uzkjпUoUْwmѶއSHIz߃""*RkEʻ6n,鍃(,aW
S+/ZOQy_ɔ=_h}zG{x8aa Ń} V2j	A.CKS*` T&񄅇tPY3®+l2͞~\F(σxإ¹?..ZBcf]^3`xlq'YH"k`_O,x8TOdǣKD>l>P*!#my4Ũ$imH92HYaCx)awTZ2:rѧ#s{zDzN\bwVD`1=CvI֋ 8~TjYc+^-ƙh1UU	,^(Y@fɫku9C	Λ /Ǚ#Ω:]95[FFIg}RMbWKZCW
[s|,C6˶ӰG͏vZBW]<0P|=L@x ,nFN2Xt`!I䞖xMP2𝳶v>lf	!AΩFߔ(2@.hN>E~a
ayV)Ǥ>1ÂP*tL;Ώ6uW>]i4ջvEFtpr
$,r]:[^ŔDaQ@SZ<8 濫e ?Yla%{%thv@2B kYʠ=A
X޳/pFS74fk#=G!XӖҤ
i8'_zrtLQh^%vH/<wo
R7Hs釓gV2x()yuudQ>..!u>9l	;A\nvٔ8ϯF-T-7?x;ujaڵ]FA^pk'@/hIp_Қ,U[6m͚ǩvT2 4Q{􄑫\b5PiԔgc%mY.THu°c,*O3tMtD@\PY>Hw/i[jN7ߗ5Rv/rTL.A\2\?L_kR+CB8U25՘pIх+^k}2H
4>(!7L,5:oUUe:F:Uw[Р}*{ꐨSdXGYmJ/i-gl俘Oum;@^X~f* M^m&Y:}c/Hdr%c J+>O!Ιz*/9sUZe:ODg|/ԅ|'HQmfᰃA2㔧A_,
gTM(<&
`4{$ؿ	
a:"۫|>N3"6$HQ`1'ը8viIM/nr/Ғǐkmv"n85:w"7K į)[hx&W07ej.`'7LtS"R'4"u˼LȶY#煛6l-EU2iwqPI`jmI5oA`AW>HLvIUZd/sw/OB^gr5!3
><͗Z4;K+Xv
l)~t$(W¿E!) My7u5j<9')+H[pjˆIq7r:Hw&)j("D7M6@@u*,9?<;FgƿǵܘD>ƺӲeӮ8%Lr>GE%e*7b
1#Fݷp VnLpia0"$e2UaRCܼQ0	{	4ӓƶ>1NHul'[eT1 ^=9	C	)ﶽQ}R
C!-ĝ9~c2t%Xojc^!ʞZ˛"%]"][}],Y2_B إ,B҅yǦao)rQT9-,TI쑖,m~cR+^ӡX
%M|[VN
DmIrpֳX%xd}PizҸ	+	L)6l"Zj1e~J_`eE(,tqkcGHflȾg(?ve(xuJ(JQȅ%YNy$8D,Uvx촳6 ey\0x>CPiU}V;NzA?KG9`6a[)HZLcGp4C гnƀ3\[NYS0+V,#Vd}vS4h<vaXlR(6aeiI47'ra3g~AluɍCt2,&ͯ-
Q=2?ߎnAfcYU%DCP.9˽褀1\m5,Ol8U䢄YGѳÍfV>Չh~=9'tR/9N,bB8*B
;B3UmXы(!5HI#}VF%]$kN_[տZHm2 qROU?zd^^8m$@՜Q(sexn}U*Y-XD>!,1ë`_7
F?d3E
.B
-+Od{(rTn\(/
bt(x1?"br6r
7|f>5v޽x 5&GI;m0UBڴxi.+"d+#/I"|5o2P^M/އXҲV܏2.O(4zV!&1jKw (p&'y@캝\q9'=(*cz~ x?k.80EO.s[:Y1G63;\->zjoԣ,+jw!ܞm(yK 3y=j5{U@"5[])nNv\au%]_G顗YwttX_-#;>o<ZŜF==B"t'B֨ ?ydl-"YƴF4<8x>ƤGHᅸ9NlԀݦHBϳE2Cp^Wu
̂L>f
Qe^/Nhv')6ߛuaQz.|>_-2$<>f2y-5QKjSc<o:ƫ!d_M`5nI)|;Bک5QA?9ƨ(yuEetm(%-,`l<X%F9VP8
,T悚eʰ$ᄌEdh1NAܨwpNO?Y2mrXIg,2ntVLpM_M?pP汖t2&d:ӗm4|(`oLAwVM=7wfAF۝ѳ-"Gn, N3
f}Jls5#in
ּgao>N~h&{G0juwΐmv^V >u
 ԱJ
izz?E³Ѥ=^9`cXiR]MkӌBr̶'}373GƍLґA-kMɰO{ɿyLL;c8ϓ/kN[yUK:AE{(c{Kixb]WH\nuVq=+gfᵦw[h*~ú#k2P|x&t
= \֮ZoHG:cFCnӝ9)|x.O9.̣WDoZ;(ldnswilW(Hޙ<&v bCn?FK<5@f`FLF<FI py2+]KEAH'KF^m݉RkVvxpK̄lꛉ̒oNgK,A{Ff,?^E5YU-Ղ((2!?7[fXk,<K꺳ۻ9/OwL4Wvb䨎X.݁JuO<\F6ݏsӍ<\CUP(6!r6*͝73F p;Yfd\a/h`[;2gi+~eړJ
sx9GưE5]<1Lr&n͐	U;ax{on`oe5\/_C
Wn" _á&y+4GjO܂Pl0Q*E2ucu|)cs'ix	*,-~HS4XiR:$s[os:4zZ|ǀ4<Kݳ4Rmy5N%juduc!S)P8Y7+AhXM%"q6
\clӌ6!AY
ڡN<|G|bR𐋂t)<P#4BZDkÃ]5c,8_+mC \A%
Z>(?s*LYٽOwCgI:kj#MHmp	4[*?ko*H%B7We&X8%1lJrA\
-4',@E:ϯStqEsFX
y8w>a!kxeɑLJ:ONmL)e1bJږGT)8N<"ixGppݩsp#ɬ/zǞJ@M\D(%^¬`vïڕ^TR/s^e6p$n+(wt1Ҙ,%Af0+e#>zFD+ +b{x`N=,쎏Q֚z'^-dv7GSy73[96.?&v$^$*	 '%6/)/WfoYAI8x+ҧd6[&K{GB#z7ЉgrkEC{^GB;{%t$]2$NGye%>Nv\x!rt,dߥؽ?ԣqWh2ƅ$o##ƾ{`0}lHdvs:zwxxvqj\w,_A
[/~"Cz8@L)BKw|~:Ԯً5~V`&~!94
[Oqrضf?27twMCr9k<u+dfi\#"  	Qu~	iD+<jӥw~LSIGeZ@ڔgLy"`K~B^`4?5kFW+6j^Ww|Uk"Vm"+xB%;U:5d\TP&x~ʥHq84+eE[US+
}Bp*qBe)
jѦC{.OD<M
׋R^䎻µ]HlkKEK 8 thH>cѻs)kz<_okT}^oNP}>T8OӁJ[9/?~T$Bs
9bUH=85nKQ	!uBܭ<Q[lίEzwL,<*M(7hK%gtL$݀*DhcnZBÕl4rUaO;U.U*=h;f;
Yvddm()mbT&KkF9|[MTv8톚`
' ()ۛY>W`>$hגtbR+ܡZ8c2¡Hq>_6aAo2Vm%KAnXjBwZ-{]pj~`?U6cGʊT+JYk<z^ҳI--J'njIs-s͉t,/Y.p3$X2<"lP^ʿV5JIe\z6/:m
A!c"
YR<QAOh ?xӚ՞ZINgt
w''㝽xpqkT4t"rkpHƐmB+ҳMYrf~|eNbz,ՎɃ³f׃`CvCP&qd^M(3FXjW'ooZ2C}Q  $l&;oHFZF^
ܼG .zv#h|g 6:md1DviZГR1WZr
tH?/6>ngԀ(.=z>gG,~bJ!B~fiX%%ɻ%Ӳك	(7BeAoyћ7B[vU6~$=omoҽba~<R)ySyOI<Y
QTt䤫.
sBӺs7G'7IϮDSg	}}\~І,j"o nݞP 䚝*j^C=!wzEMŎm'Ey:q	դsTkjxHl~ۘh&l
 引2I{ ;{Np=1-6ׇ;1՚5Zqyp%q*>P[ 	g` gN(ٚ.`cfotMVW"(^rVF),^	Ϯ7ONgߓN7-hn1$sF8Ihq:@=,
7Eڡ7LwD{-QfVboX,_'j
m1}s>KڶY-V]Wíă+ٵ1>t(1x
%iEg8?)c~GjD-onMxNozJ&}k:E
vtTIۮQTOBbn1[]*rԸR׌6͹~6f-q5l<D;툗U8+	Cd5 #L˨aZ^hzAz/&FL}w3.h5`O)d|F iV`=g"(]h]@K"N B/[4֝!hZ!i 5xОeNAuG֑>{>mY$Oެ/uP79 ?	$>f #M0*?OR'_5AjR^
Y;ѲG0<(vD_ZDF͞VZNrn4qz	dZQ\ms3H,1)mxk[.ܹ>|BZ)ү! 2,D
cgk_;bGG'N#fNkP@BjHjB
R	Y#:$w1$9fTEZ^_*
Y\ƚL>7j*<[^kw]/ف><oѕ_}n[:ȃ1h2E?6)IK1lQ"H#S?IU%ˣt|lYumsY4IiMɉ&F?m1<.q{#i,YB)(Y7E5qWd"0??^
>	k%3h`DeTemRHQ{l@Vi{c;"k@{wԭǮˆ'~]k'ICjk6]<g{8ef+nBk}<bvO}[f2I%dr#vjyAeك</nGēI?<
H<&{-fp*
}?WR;.h,*2@"/N'
Uc1#'4훹|]'E
WsG`A%.l[7X
νi\+ݏ gLs'Dj^^XJ?$ypbcuo@ѬUOQnY?i2]jVfEd#&hW7a$R6_%.%	Bua\7(1LJTtM+c G~'/o&'YvJ'$Yĝ(G7X$pjySk[o>~Ov|v8%kr~	|R{#^pF]R,
VG,es.E0e]6 hmYMf)6!0%2ɬ	/~$9$]ND9fJ5x:8݄5V0ĽILLPn,K)Z斎=-iɺB'-55A^!vk[kc
RlqOk:H3>@_,AlhOW^x.5* %[e
s
pHaz`ʋhWk׍Mnj[?ty"yz
~žʭ8y*Z7=|s~m|f'dݬ9iwr.LJ9MrOV#U79V@Fmi&rщ*>w)[e뢪Ajq|+?қ<)	Q }Ҡ}!̤p*ѮU^&ʍ!Ѭ a~듻w	;I!'G(+ߟ IdD<Y4x,J9`|zXĒ}6c_KՄFF1~b8xv3\)`׾1!&@<wIȇ~uDO@i]ӔbXp_NKfOۋ駤\RҖmedUBLf˴ tFǸњ49[ٯkeFqML]*)jɇа냺gm vdIcuMMZhBh9l~m%`b^K?QZ`;UZXa1MȮ^C gHZiN8mvޅ;iRt12_z]Bck䄻b-%R2BNPRq!ܑ?SZ@AS8vtXͪNRIU©	v؄˴OOI kDŊ.HʪEG^{ -DDO-?Wimۤˍ#4y4blRdgtfy/BڬQכQ){&P±uԙMo˼[ 
߈ZqDyjU7/N*ϧigb1 'ȮMIqՅ!uRHIs!MK<K_wmͳ7r+u$9V @)&V;3PUz6&Sq"A_8D"W_@~).rYF-=l_Nk$WaP6%TXdV2L'Rym#U\Fǩo~p;u$3_wE>!1JrMYՁuƂH̑~E5BEzH3vE`ʣ
YV=r2fu+]'9JhXmzyADɧ7͵g냗z19
s8lFIAipٽvtJ+u8O缸:Yxx1a~02P*U󅗘Kb{w<$k5'99N'7ƭi>  "HQ ZQ;UD?f^
^(c$QckJu|Ekv$jF[s*Pv9577L;5oQtcl$)l|cRok/l墲ݬ;*VLxZ1t*4^Y e<Ť0d9euꇒFBe3Z,1RsA&f;fTkurFxA"֨X! ^Kbg/#8&a[~ǖv-vY#s	Xka9@hTOJDPnX
W+5؎yU7í}ߩmJHuۙdƍhL>Td-YCV3ͪjs&gv1-Ji8m
Ӝ6W,-&;@_f]a]~ݢ}œPYngwf9}$[g"$4.ӛVQ|˸K]? S
z"d2j5:&O.SKuu'RGr*dimWA1"ܡPw:jbղ4Eށ%$mC*(!6!xZ~.=O1! ܳ27cޘ6c;0jц{Oz
ɤzn"d4g4P@{o	ok=ٸ~ƅݰn8l<4+kNbl39ElP L0%8OirE͈Lo62+/j2߻k$_A.$i~q1KZ:>ps
I7!79W|6,f+y3n/qqNv	wtu7cW^jD:<K؜y4VR	3s2`.8@'IN-{
"eqU)q(.r
xT::yĥf~`zCV$t@ Gג#a3w|0Z?Zq<&QT
X4iֱ?m%d{gcM#dT3wz힇&sdѫt<AE<6_a֭0go2NA͢#ϱou+goTyj-SdEu9M_=]*"@sowwNhmE/=ZS7i0<ʷ+73l8x64\Z2Yֆ]boO GKV(Hħv9)E\9>zEUkK$ESkD΂bZ9L,|"FKSZ!zy+H6իX9
RR6d)3Peƥ+ik3ϫ|3v
vqCbǢ= HڬcQg|-Iy]MRii{)KT	`,[;E>?.b+&#DgY$nKQ{@-gi[x8J.xT }pfd?z7#J[݆¢dih(ê=/٥0Mo  WfVR]0Bŧtu]3p6[ 1}ubCeogɺ \4<@[)P{e5^$Pygw,i.1r 1tzvyj@O?ex5mR
v"K@T!>9MTPp	⑱$ש.ŭjTeO@D6wEt'Rٲd[LcAZ&erSbf5bd00KTĖ+
V!'P9//jڅCZ֧uP|
O=OhnY{S3WEjpZ+wxb~mvN!aei2PIe _,X^sJ}fl/i,C os(ߟ4!YΨ4p{0i\LsHZ!cARj>LV#X㑖<o<һ,[sh搁/1$
?AF꓋B+:#Ȥǖ\GRMA{39
]D2<`ޙ̆Ҽ \noVq
wԇ	eV""\@랸h:/]TzG~h(jN0"AwR_=&mI'%>ۼpI	~.^4̓z%AܙHy 7"|22WSYLѴЉ|2xZߢ]/޷5۩lKA~SdW7Ms4roa2lDbb%>6:R4HC0"_-EӶtR
GEzZ|U.ެǬB5{҇U8?TU @#nDX1vQF{r$'K:4%D+{0u
C
VwtiniNç/H[#}:sdֶ3ov&v[W8<δkhs>%z'­SB9j5~MTi[
 M2/"'_u;M,6ġ͒iaY.ƺXӑL~t?R'
zg< !}Iz3a7L		Ea+qH5OXWM?h?*i;iqʏH-Y?>xz^c`L!ÂV?ˏ-x^_;pxkm+?<Y睡͛g˚$ߟG:q
us5<w|.2q+.mKnitfAKї50:%g2xQ]]dK__vG{o;{㝽n}8
/-Iԃ	h=F1#㤶B^)aH/{~ྦ!L4Wgx̫}Nl"oPH̤G;3 o}dM	D]{{Kus~[੥y9xM۪dL!;p\xyٺ4ܭ
~GpΠRf'[@Ԃqoh9.Jx>
хҦ1Ve6#L̪IO)NbzTZOrSlN8p!lM&L`Kmԕ`S:WO!{7tcjA&a+,=UH\d_Fӽ(nv%5GKVᇣA<]aw+H	N]%|Χ%_ICI!>{Bt|C=ǈb*wiLIu˳;@-ã)ƼwE5|M=G<-}%̅%#U LɆbAA	R-7W@+6௃')j~2_kufon6Pݶ@iA	Նzf 4QntNx0Gd]xwwv)z!TnC[M'IR>XcÎ4;LyiLf|w)i.LNb|pk%|7:8{t3uH#YȭY"!G Q*9xl3ϝѪ$%@2   
Mrx}s8WЩ9EVl)Ŗ3XvTH_@|VXSFh4Eݦ
%MugU/E@eyN

W'
s$:CP+/4vD)2<EhY(u(*(ͼ+[k
ؓ>-,]]׊ @ 2@1M82'aX#
{&Z01UA(@NR} 6K'xyZ#2@V6๮eZaw
ڠ:/M7KV;gh(E4	0^!H|<fQZ4#B!vjB0#@{$	Hr$h*DDU'_g㼸Ie
2'`-0ƄX~(ޑIWncLvOw (ccHAgA hF~3?_wF
5;ⶐ=JE\$(s@VUf~
P}tR-Z
zi-c>s@ gq)iXz_x< Gy
v-<%__(q	j2?<?y|e{1y$¿*6=ΕRūUTmT
_[Z*bcW.WXI}IJ''nxB0l_%~Uz%ƛe226-,'Sp JVAI;p;e(x]6ݠWnVwR=	
yAG\t=YRiR&;ϮWzfuz.geu aC_pРY5&G.+L.V8KN2{b)ML&LSQ\ŊWNfbZS#<vEԬbI馘V͠C_/&Äit3X
+Tk,|iVEU+:GQRV^Eo֭NNsz1>la.x
^3~+օ/K H9'+|-
z}!;?c5驰T_aӃgL ;c-F,CT%*j+t6/hZn;n嚡u/Z3[-q(.z['R@l_Mʋ`E.\VuT'Xe9}0`GtZ!sz͑ymݖSA	j#!H `
bc''.ڍGAh\m߭vS"NPu1iS{Ю&=XI4ଓ#5FwG\w	xxoO 6Kn%
$͔U(^VyZAH.{JPx-@
lE>W89Fq)VqI4.@Þ	GO?q%S*K˵ DJe[5!66!.~RO@H7y#]$^MfcAK~KSqHs⎳ExiV;T<!c*z2XhgHG=*p`ݕt{}#v(4
o/1&^FճmmoǺN']/J^{Ǐ1ͭ|ܲ/M<C<KLe%?_Y
a.i;*?/p}p7;/\tg
䳌س^8k-b fC!dߟ5=|{-x
y6k\"ڕ®mIbL?#b=,[#Fa&O썝񾡉eD8HL/jA "r v.>ڱBo
d4'`$@E'
z !F~F:
/Y`}ACY	KSu &sȑŮ!O#hƨͤ 7"Y|IǸ(G'f7wd6]LJ"&^Fa(
!5ݟt}n6ɥdP}Dp׎֌c-ёQ$s=J	L"+5
*+?)8t.[^^El,O,5Pe$1/$Q^{QbcxtN3מM/QEו &b
Y/d?Wk_j0^+X'~. a")'b6jwN4\i6^xSbxiL٢@(;w[܎8m9-
 - q>&5֠p-t\%<~ބ'?<h@<2(:GHp59\|~lrqz8O,Y״&K65<UY \N>N.gU R>Ϗ&,^:?:(Z|
f1i=<CTҲDXzhgW.[6£tFr|vn&jE8/frQk@w@Plt8>9(ΧG'HD	8,T
J_{-FZx_ j"3LC&
煦SĊ!l:73TX aWaMxJp^rab94th;) mʙPSԢ]^&^zA>PԨÓhX䛬TP~ s~;.SNx0胾U؈WxG@au`YWoN4Jdّ	ȕLVwl1?~Tpsї>r"C'UiTBd: TL
9
 yE]/I1_IP7 f=(K`\"xFr+\絻/$~ GU n	S=QR;<<:L >l$iQT cJ͋JE	^<:l(QvKOy5]fK[hK<REZMuAy@hquoSo'&9leCMSRp;m| @eI([`Տ92H;f?V [Dj Ȱ`.Ko2}coяn74Cy50/_vd M<CmI̰N7q",(Bրjl6Ĕ%#p?R˖ciLEs-FJ3;&$PY+(dN4ʏ@FhN'HuӸVsRXp7=Cq!ƿݨI
U*!q+ZY-.&,XY#l(3e	ԗ<a::ʛכ{LUe(LB:/8u%ldNK
Q4,8$]bG\g+h}g=afdh:Ɗt29Ԡ(xƶJy28ӓ$=фnl^䭃ݤ#vrG
\O&/QYV	5~7D?
ZxU&H́n>	F>6CPePٓqd[g_<΃}=4{/bO\߶6C(_/8]d4 0(zk=KIr,z:5$1`#D: E武͉uL
=K#DK n#:N*,iax?wezut	Ie 4Q67؝Zz?eM{/S&"B/~V%His|K?ť+&("u
)kV,(,a&\I=Jð$1M/}YmizU^!fOFěYq*O	MO֔}y\Eޕ)zFv~T琿zC	ѯT<ڵ.WU?E}>0L$<MSrk#HKjPܒtS`|E3Z=bƕpcz4CPQUiT瘈&|GS7p=NmbCNCLKI`.cXh<
 UrJyV w4w*8KpS`)K@KP[_V(|;,O7UO3B0U8
\o1pE4r; SGCo9aCTZ$(֫WO#LϫJbZ<pzyNu9%N:lՕ#Klv*|ր=M8|`?x= ;&eA$mGo댪jٱHzR#n] ,RPcA^}^ 
m)Îl)AP뉘AnetYf1mx"V
OJȠyeCs᳖ts۹=\G.|;J
}# 9M?dmjRK%kk/)BP	q@>m:zUq`zWWPV~UI{G&Yik@	`u
^MS3wᖪ<ZcL/D`}or^qwnxmH"Dw1cCn;E8Fc邷ț&I868b늼":DRɰ,S
A8pqwgaw햣@n Rp9tfiyЋsX?TL:Wc.CFmSvbG%NX>ύNҩgpڮv}O3sEkCx׏Ӑ\}lKj |*ˍQ]Fh(Q<H#|{7_톯7*6֍b}Bgq9_q2ԇM/ʱctCXotc>=
F5
vz
G$6)I툙-;n"i>'d)AT]
ZV7;\,(hI;mOΨ/WNIF&_m٤OlS;𰊭1ӱY:qQI]фD^r	sw;Yx-Ugy7%XpOp&$^-|rN-s(	Kn#ځ-q@Mwb!U_ONՑR|`n{}#	AD5)|eY09Mo&+hMUq<=/*͆CD 3zpyIQ"ؑ:`B&DW>?%%NG_ iFi9^/nCv0]Mi'9[74<N.= 3}ÅZ ,'oo*^VHt,&>-g=,G;i_lKq]H(Hk'A")%5ld@$bRc 6h 0ЙCg BA;D{0XpЖi4
 VBeV.(
kREڭ]0dMlPm9!EpV߲vi$-î1AY=Ð2gƠ=to3nvӆjZ$| srJ?B^fUp
ШL4\mty2@*1H4>n؎[wlYx28PMbK7gkڒǤO-iԂJR'Oql'apހV֫uĵ;ttH_a*<$6mvi>3(P&{씃aI5\͚4^ݑO! Ϋ"^o|Ջ"+pG)Oyn.@^!9ڜt5+h!3%	 ?o?ls\ޟt}"~<}Exm'pqQ"RD_,,^	'$L\RH2~;h2<9>8rb%e=cκ,kB6?w\/Wu@&n+7T)SL<o}.C8
HT|8Gۼ݆(-WUyMzE*C\溺v]I٭'uw{(G&\ʺsH4[ޭD {#Uթ@O
#f?dp6$R
osŭ[H#M
C
|p<fuS5{=n	mݢx:כ5iZ I'R"p{$FPc91b}`=莄02'z/8ǕTV+yeӣJ=LǶDlb-qy3Z쮺w%E٬ȎD[	ܛlO{hWRo%ѷFxpKleXi*UqT(nժ%6iBSq/iu/
FfHA2
Y"d{٣"qīdlfY_kgYde[Ϡ+873i"5,Ez٤NS;
hyNZm99-P}4t:`QMAvn!-CYa' 0{֮\AF-#_د6iXDjks|+$"^`u>.պZ=Ju$RGlqN [d׵7w: SOn
A
 X/>R1GU aMM7+.8?	B|5!^L}|J)ARX?HE#FG63DB
mGH&=j(
 V`!6lttm4IsA)a*hԛ
+~&CH :4< OmEr뙓MȀÆC&.&=(K%dggXќ/%y!s_s8>B{4ӻD5\JPK٬n`2z@EI2i7?RjT^[[O\dan_e7l1l~šLmoUok.>o-Q.&'&٥u*4tX4H	w [ֺJsdz(m"N`uUF'4}ڻ]"Zu_`OQm&>C/=j4)4pZx !Jŝ	+r~q9=ћvŪsm
Xw6>5\?[*B2i'Tҹ[o,= , 6O>mYD!XFḞ&|Ij?^ݨ_ldиP-)z,A_ [ԭ5q\-=&sl$M}nIq|}G5`r3u;V'٢x\	] u$ם`ʩ,En-=#s7liR^Z2N\,Q̳u蛾2%?~٠
bRrSLn.gIcY˴£3*6yv=yIX!U*(F#كqYi1pO'\Gopk*!i	!@j̇"Cv6<J"Am'9>&41;T1Qj&Qĸ\Hp`N`	|</<}˗u|c\a#gZ*Şqgh'moo+BôNCR
yx"&n]NG49aް=k;%+KkjZuO.{_79C`+Yʐ2,kDqK.	Ρ͊$ۓ4p7 }_᠂n6j9๷8 pmeiQkiי.zBU" +<U̩;]Td@gx*:w,\
JQt?J ǫOPzpHcς[yf7gl^Y(:>hdhxo@>hNVFľ|;RV5gM}ņi#=6#<lآyw&ߤH\Ɋh1
_2a^Py'yxvh  [vNӮ/nk)<)
ɑ4m̧?A	OlOx@StJb\USv ;hUNaއ2aX*uϤ\0jw;ݻ[K6tVMp%Yzbe=qH)egǮ!h|PjvqW܉}-?V!d;klHE$ڿojH)a)f%P8
(iZۇJ:kٙۃȿO%1@1jnBM%RrL_Pjb[.¯ 6SZDĚˈ(*tGCH$l|u
=%$(N7?"moi[];WB*PX9֣+gV#
3dv"i/T2mj"-
`vk (w̠!jrYC
w֕\P~S^InVmUp_e5hq wf;m2!ﱨ.96-h
8Y"ú	v-\Zs[ҥ{Ÿ&L=⎕標ƿw9[hFyDt@!謁YQ#Ħ`Iofuc;iW4ͅA[v\<c6-o5h AbE%ڝXb/:vXf_Prq2̔ںC֍9R{
v}p7RtV`ŪV])GGʦ49rL :}lߣ &xxEqA?ӾOm	gա6=&6`;Ro$%\\8/fq!?!kc~'vO$W(,E9ǜnLY
xU	>bCJ(ku9d=o1Y/r"uZj$X$bu\0uv5vS`?+VEcb<)%jY44n[{lM2(%ܗ
f"Jdf-hjs!4ѓRړ3TDeY}iHOŭr.^ʦ"ر[0`zdb{4i$039+
Uh엓B#gO'~SMHY7P_nPH'D#.+xFx1L8@_F\Fyr9GӳhX`ώ'9hWCuj
xG+'yEA*[Ts&}DрfANk0>Y%#ofo$cɩƁ
ƂI	
Wo"рQq}d<R7~ͪx骈Mp
XV\#Wq4@ᆕMF2Q;xA^-F\	ڨ4TkYZ'huK)H!Xo(&$mդ
ZV!nf4<&(mkyu=Ym|-6:!6>`1~uz24]Bfg(,;Sf
"Lw`5oBr1d_Bk,?s<A18O
wo6dl/xcHBהD
rFli(rb[wiu~hʆALGz	jfmOfvXXI3FӂP%kMhM"&*Ts/R0CfmQ2<*X 755K(ϧJ'-4p
w#ަ]>f@0<I0a^BF;4Shp {&[T@˹Ϝ7=hk}%$l6GkJ8چQhS4<<ͳUͲaOM)î`ˈ漥]R"d5b`[YL:DVIbI&WZ&)~tYDx.RɸM~J&Djёn68$/`~)?()DRcCb-PmjMdLQqmkυ:{`lgHe66ho~Aߦ_gЍ5Rഞј"}ݯ&ad<-w@|Ty/xFdSO}/_e
ZBQ]>=nkK5P~nk)ZRooVOko6SQlk8zY%rm_Lq}Nw?Dw9݁9-p`7̑<
p\ۨ_U 4|:*!xjkG"On#p$	D^}
v]Kȫ݆z1Evll*O7ʖI2u
2%[W0	9#}xm`WwXe8#CBkيi)`sRpCWkr)/H7wpK${^|&#4\ ~[p9ʵqw9)2_kBw]oցD`[Wl{=3cH5AmǙ8-?,%;&;d'R`RI	, e8ws~J Ly&JR"
C5$TY@A=9.Sla:V Q*ݎJSH*rSx醝6]FA27==
~$UiF!MR4L\de0nAK _%K=K	 Z{y*ëٌ\i(C(ֿw|-٦gfl[~&`xe[d۫W[H7ZM΋:4͡Y#D&AbcKӸ7`XVhuFhKY}Y57ڋDEP|=1,-#\4Mߒ=Nd1ځ$ɢtҜ`}F49PP'wid?eT`uv4iF+;pRiabT= 5p[2ɐ2j4HaTlf*WƜ=YG{/JkxXK}&	E@@wC3	[ é@@yh*4p JB
Ou@P:uԱm$H$}Kz街RpGe^fJfbXPNM\eaG2G͈$vO(Of>	YJh@-Z&3~r6cˡr#.x'-+5"2+jB͎V=jQ_7GD$p-	 A[pޞF	
On^0
eA{f?!+~9L3,mXa-	dՑDq+̦]8|h
Xm
n ujndr525|h?&f3N q  
|,Jtk
}Loy?|ghF8k{$Tnv8T]=C1 $=M|aP
~)@5唪LxbiM-9&ߠw躖zOueu27s]
WC>&&H	Uá QY0egd@࢞^H%w@撲~?"呒>8;rjB^P_`9`ՙz%+Czi>=ͫ@tV
&BJ~ 9UI`㑱KyFZTryHַ2j0O| .mA}8k'O&7sTG2]dxG{曖Ѓ9^iS2=ܾعf;un=:F'7|VҌ:P]HC$oX&a+UIקcXkN/Z)R\ZQncL"Cf?5[5+4hK/
G&2'<܅tIncd8P`2b.qOBxһQGd
{ =KN>=Cx(g7RRr=zSXe,Z)+*#BggQ>@H;| BnPAFL'Hc &rqfd3l`wȺYύi}Z_<C縼?לr2NY$QZ&J;T2[Gdʺ"xa7rz-oftcD{b<.<hgUf(_%qsmU/U
λ6Ӝ.
6Rf3dbKzVu<hUc@O3L3GE.20>S* kl-p\?t[nPݷ+M7\?h4Ť5RԵQx;¼RsTcp_;ܹ٠-Q݀fc"gS]͌t`[EH
cUL
zJU~D/	7>4*O;
WGrq͒Lri8:=?g?OO&Gu.>]z~Bht2A{@EBN_&'秓d6 hm?W3gTe\%?sƑQiR2NgyuJ!1)#/4b6\ކuVe1fo[Mo5v 8rT[zzʈb@a 
[S]h#wTY$+
|X5b\ۙdv"ή1tIp0/a.:eihC5o+FtTtŜk+`&ٟ
9khÝC,Wb`nmZZw*y	Jwc+Ç֭ވ4S&~/6+j*A>XQªy5}ȑ^>
oסhԜH " W	1`.a-0m`H8KC\+yN}<tH&o)RX$>;O"Plt׋|z5*)r[z:׺{e;$uH˹D3aA'6,6 bX zB0]Z/{$u
m'r݋FL?#kG:.%~֢KZmm$u$ځe7舸b74#!鰰 :V'XF_e0%YflZ0IX*DkQ˷)Ɇ*V.%q_쮺wl
h[LξazTs۴$mQ>oeV+GF{'&2__Z7Uqmщ/b
<;'$4͗	94xܥ:Lnۋ^ڳ1A}
Kڷ&x']ezxL2B3B'V`E:WMWxm\r~rFNMc~
=$ٱ׳JRkJW7bf0Uqp1$$<[SsWg"72_i#*iAh8A\,	aPiVa|-H剨#"<?r]A<4q{_$
YgU<Ҿ!!7/p:76-+(܋iztaS`f5xLdmؗoML>X=90OO&G&XEB'x&$ V	7Y톇 \"KaƐIz<-䜇|dooW|0w<VG,Bz%}$nljsva7B_)C^G?aMR6eElS$ 
X'z%(kQӄ4sAK~˗unPgt0nG2"tgxzr5?'uxr}4Y%Yvx`~
ɬHQeDNXcۡ4tSӞ˗iby'ކ'.!Ap1w`>Kx&D ]Cc:iMi%P+mf6Vza&nѰ۰r	
s:	hi8@&d&xdM&:yp#Y}KC1$G-9cԙ K&]?Wu\+yǔX,`c- +=vcdcv/XJmӖk>eCƪ=[n)p6:/tMdǈYwSfU1f	rdIM[6_[W
~/'A}709zo4ACe$t0C.w!뿩KKC !$:k\HUt8&i3li	a1՗ǞR(<:-xQ4@ ϶B*y*YQ#c}=2So Ty4dD9#܏A'QbTG
CW'0i#=omy'=x^>_Kv59
;
Jx{0c-᧷72%e8pʵTNeZT
}۞2Bk 7eٴ0!aեJCXhчVL,Vh0@k04aޗ*ڦ)1$<$$1DRAkW5z{l!i2bd:xAJ8R|٭VzY#BHefӳif;	
D	Bڙ1L%ڨU?Wj#1+(uVXiEӵӕ`@@UΤDY7[NBVIT*,d8L#M/K/shك11O
W\,M3!%i1(+>:Ұ譥w)Kԫ'4c|7ٔ.@exuIcZMxtd$pd1jF5fy
TŠ:sPSfrԝSIĔvzQ7zպ
H"O4.>u-_)ȢcIs1<ugYGXupe5S\p)߫bS#OߖSk>5٩&{^iQ/jz5H[|is PJnާ6=P2֌2gv-z1riL1+)Y<,z
<M
^=l* Wp@VqN%,Eפ':ORC/_M}J`i-npcb=RKx$(\`kӮ$e! m
ym=n56n%ys?.J:Su
P>cR.<?ǱͽX.gH焹Q46<rnd1
>{!>86u)t(LR1=$_"AiybS(W5o^
Z$!t50hDwv4@4X0օ|FRUJ]ŭ[L[|Q[YYI6++$y**
1O+TdҤhîBcBNk76xwSyhƃZٔ=ewU'V @˺ԥYn:-\$KY!S"&E"S7`~3dY1B&a7}?y]VWߴ~"W(N3a"Z%i#uPJ_bIJleD{ڎVH9b+RpМҺ)	/-8Gb"[Mf	Y=WH>05:k:iN`Z-6ZӤ
99'ɡTe;_N"q:j75;A ܋Qǫ?@ɀ@3Z)\ma_)*47M r@LrE -?=>',Jn80<[Dɭw95
2ڪ|<_%JHX*H"?.»5`D	
W腴	mݟ)<AErP@u^[6<ۜjesJId?kM=	y]2cF'rmqi=$ŉ0?Ggt:=\O,k!k6!<YCq6S:u[f0S
`w4'jYm/bDYcS*FHRN=J?td{OGxjy1"7<D0Y!r^@& +9[#|RSfN^u2~k
P!MWHۋaUsZ9v.t'Az[G:%o4"<͸P$:z>ڀ!t0b;a1jQށ\|ݺ?P<ɼ̲8;xq1lL5!eZ^ [oQ%9`쀰zo{Aq#0:޾Trz@)6h(R+GA bw*I|&ڲk<M{a%TCl"T   }kw7m}CMhFr2|%N#KvnNOlY}Mm͌o_ޏ£IIIfg&BP(
*Ş9yVPA
hc\koׄ	XJѥ]*d6ca3  ՜U`H"6(@wy7t^rȵڼH8X^/Dil'*V)o'lX
pOhŀGI-
LN,ٍgw䭥 V5qnYb@-/58Zݳm|q@0
Iou [,LN8/z~Mex~S[xEzpO|pE
KJ Ԍ]褐Zi[({fEmk#{hC9iw_]cR{9p/|A9=5}l96\4#Q`˂+YBm{MRHVBV{a $h2<uppcQ9CT*Ou[BNP
	f|;m~\Bi 82
k$eѐe7ղgS ƞ_j>t3$C@˪. Wf {ќ'C(oRX6eKAn!{+j4ND|+zI]f˂IU꠲9*ڜdsM}x5<zx8y7{Q+zmN/{ES#1|OF:ͦ3!e&$
4ܕ^L	ZIXr7:kG?SW/e~5/!s
8}e/U)]=܀
q mjЄOa#B{8?>C7_)v4]IF/%뢘;C̛sd8C63>K
rl2e5Mp۶d3:I{z99ʅ
٭	nh:)Wox@Hvy!yÏ 5T5ll㠪Dɾvpx:Ou1.NΆ=Ld47էW<-f'{{CŲQiJuc`3+E4osrl3ʾ(wωDy:N|mƢwd~T+dcbQTi\ujM{lFaF04U)[&
-Hn5]߬UL`4
Ȧۼ+ju	=M{xG:P%f1PT;$ zòjߕd/緦wMH	F@qzrS?a}
l1^G2
Ï3G~waLڣ|<ܖx `UOX$l8
gƉ堡tp}HvvzsNi>RkL٪]2L[,T/(g&l%{!~XvOεlϾK?Ω\_dNcϗD~ ]XyOrv7,Qd׆דKpQE"(dُD]Q7"sx'w5ܹLlV<
&_f]Z!9WM1#,ޱ9ڌإCnjίϊ9vnjyd nӍ.?C9_b??辊spP۴,Y⑅(qŊ6C
J|&4.24Z%;]9l恷hO6,11R[=;;WIo܍R u63UuT.VB}Uy9'LT6-W}wTy>/է|^Ţ\/|z=3)Lܸ7zCF"ugKϜ;olt8:9ܔo-{~Y}n6O6QrxDI
-'2u?Cf 65p3TqƋ)KڞtwOvn
c
cj4- a{*t#;:C@w3K
2y!+¡o`蚣42FD#3rVɤMzjlZͫ*!K4'ys0Zف]fP[8>5d\c7
Z.P\~qc{s.B
Ά?L^
kbrr|~6!7laxp`Ha[6)xvn!EGr')/A(S#ByA*",Zp %V!.};x]͌HQw#Q
L[<¤Mm(?Zvg<ǖLGXM'{tm~{2` {;܋4_2d:4#Uގ抷%-ȬUb:7gEpoBH#Y1]z>N|a&t*A=.պ !xġy:*`j;5:)ƥWXZ]{oB$@4CE
ܢK<bw4v%:XQb&ڣ
Ya5@"U.~Ɖ*OܥoX:[t84L0ۍYY? Vַ! %8"
LL)lFSS#X
2]o/@@EZQ/y7I5d (Y"D
v=_N|d|w{)QpTtO{m׀]ӃO80lzlJ	MSdy>+cpdJkѻ=1HQO rd4yGZ!SwPm˙|}n|7yjPP;=DQ@ܺ)W7,B
̞{#^kQS6*[ԌΛvn &R,u])γ6}Ŏ*~a??ŻsZZ-u]-w1HY:K*tp78]$[ގջM_E]]Ym9@iloWd566ӂm
7ma8HE]UhbZEr5ا(/3+@4`?K0ڬ:h͋mb0]# 8eSi`(sx^
f
M0x yN/g_SL02stan.&Ll!0Լ"l"7Vѧ#{s^Stq?{dm6+r][M_JzecNȮG18͜k~" ķ@)wQ9ni?hMm5'p@+ᩈ]Ylan>y<_C\YpiPpd,vk5qо8ďMS`qUsuC:ڛ>~UM)w P)|ok&z»8eu3GǽbVm ڋY9ۢళɈGCM q95:$>9Q%#!Cs^a,4&[|J`aDʕr5֠?ءKV03j)pg'yF=79I;0
M>;&A9hgQ	/>|R"ue}%``ΓNc楟10p$h}t{tC."E5Λ&Lw݁F^2/Y'i].uһnVʶXd_i`ЇY,[yfnM :H/*1	4'BÖ?XpwQ檌(²Q$@F)Pz3Jk_dVȗyMj]&_.[[kz$^5@%*0e節A?BIβF?>n=Lp%`h2X%\LW4c$\:cҹ+3|je%! <|.:3oP)҃6XBe3!\^feO'{0c"zEp`&`"Set?-d,`\Mph
\1^B*g0`V!{ӈj.ϕA
{E.ʘHGK
PD+37j%?

o} Q
sx_gOx*.>I`,u[gNG>koO^aCU`}7i!@.EdN`~mN'!%A7e<n.JbגmjKyv|-OJ}\:CuS5Z/fbߊ[7#gw-qě_"w7RCZM'y 5E(>R7@ԇh/\b2sņEcWʸQ1#3aW)Vh=7 e|谹-f3-G_g !8U`(oBAAN#Zzx]ՇZtjtޡb|_6`vVFI](J"SXtQHcatGoxu˳[;.>V+#3c
E 鷐̥f~}M/f@U[ΊOk>.:_44\X
Ķj$rlaSqetٕ>;m9`EM9)8<yE[!)r W,
jے7Mo6p`KO=?PĺbI=$~%oPX0ħ"ִ})$(ub%X/?2H4V&(	dUeؙ!X>3EHdϫOU+82{.E(b2Vњ^/\~dˤVʒ1eD}{
yV23ƪݳji ~ix>d>#pjQdM6gaY4PD6BM gEN
:ܩ bMM&NlY.m;չRqs-]~p5e?&_	kpɽK2G
P>9òMy3%*k'~" p7v/O:Aa3:k`0j'?.~['%w;$]J/LbF%](IR:2QlIj@ Ń1!_ăQf)Y_Iٜ/|d1*/
h._)<Kj!Y˜"5_V]52cI?ϵ=XEeVE{v7Bf_m99ZwDt6	i߼fpzaGˑ8Xs7=9oO웼Ujז}&)O.;<4=bxhm0Xsv.^|p0R9Hkün~
U*_Nhd$`$F^!0li`b9ɁWӀjy։Ei?]'`N"ERђQC1q^PgJ60If[Av <1KocT̯W_Zrz7r3d&͈v]뫶ev[eD;׵QS 3[J@G3`;
T|Uh& 
XNRUgjBX-hO=f6"vuvE0;h3\
rY	 
_ٜ'S31QYX}I¿A&Ii72w{jB;@ȺNT`3}0hl&d|L=EmC4EۜU8þj;١	cJoniK3q쥵F
R3׮Ah_*4	_~d ѶKnZ$38xcA᰼Qь9$QZjwOM:5qm\
0xݟ!Bo\:46̒-m8\"(i3D>SM;[oq
f/x".Ү۬/6"x?Xi9E=qvWnJFJx8[߭#xIaHvEDt};Ir췥> aHv
3:!1-܉
[<|3%!UAN9x ΕRMB	( Mij+^ɷ0<BpvX5ϢΧw;W@6NE`/J%~&wIfC;30s]?+>qHۢټw
:)ڼ]7om9g
LZ+Kso
_% H#|v[=Ĵحª.^E}7mNZ3UY

YOR|4y}YVdtiŀϑq(ޔyِ_g{{3ZRZ ]NOZ6W<=0{Cq;hDסw&oΊϠgUE'T%CM!;&oomO
 mN͋p~S+[/=cN<2[|ʹa45Kc6R^Ā#HRFQ7G0<"^q_8lT !oi &_[?5&NBɛ_'7d2ߎ<gXϗ?Paga6Wʀ?$]{붘XbF JB
<+&VkuHhQU2*d[4Bޫ˽8!:rEPn{B~Elǧ!{h]4NL묙Lv	3t(OAp,rg*Xv <;,cbѾ9qVN0p+`ղ4dh&i^~zΩ5º@΀&C
v\}(*ʪWUMZ/J~Զ\2x:E]u@*yEW;W(u?l3l/x!!_As6aD锎
H0"h8ۥyh0.h7o',یA!$n1
C#jN¸phA=4YN@"i)1Tjd;e?fIe)4|Al>P[vL$ScЯyLny0h	H?%E.d>^dM1?<-,p-Tb:뺈\~祸$E8h
YQsȪc05
v kfKR6vj
a3(eqSI9j'9҄Pp`j^PT	R:oZ.YF{jzQe5V4`H:Ri-b`aFw<l-OZ [o@ `7ǄoV
OjZ~
.lBIΉ;qUpyEuLE㲇T<b`rzR7mxC:[NҬBo	VM) v#t&fީƻdB,zyY?&G
bj*ϐku9 o5v|Cg 6.ub,A=@ n4j0RwzNqPKFq±M9˷YEE`JCƆ㤱Gi=BG6( 18ߑ+P{z7M7gEAV<<xjy@c%{d0*<΢*¬RYdg;`Q瞹JPYw]&=ք>>VЛ1ؒ=:e=-VaQvp,
.]AGU-ya)Y¥5܆t2z]b/
0D	4Ma{wv&F3w@K\zKϒ0lM@`LѨ7QJ݃@$u{0L[WxHOU]|[x9d³}jgC4Ǜ]-2xob [czړɳ#-ýF,,kY:φ;]NojI$,#Kl~4I	4ܱp`;UXԿvEJk`
.Ƞ~aCd=ÐP2hse(fZGW	ޝpgaA6~t1|['7T).*z(ze},ryWԇUv[ؘz>E&S.7p奪eDg]^7WãM
/G'g8b5.>pDO)!Pn nv{?
eXOS3
-=A
R)Vwa~3{`(t\1|ycbkFb?4caƯe_j(3&O`gv7[rU]h0ky7=OWbŸM~R AQ;F􊯱s7xC@j]pSBIـ/p_Mh}83<jqt*q[V E2EXh>P/uD{K8uK+dtl>z.j`8Pt|
H-)KHF1̟s|:eu(s)
q
<$ɼ\,cP\ϬB:5Mhn,q:h'Fgߤ)
yغpU^k{ETꄈbá^h!w$o!A.Ø_SêDoiA"J^=q<"kSYk/ygy4:<Fnz-1~rc\;~,݁:<Ÿ[>>AݫūM_|bA'Zgi1fy٣Tڶ&Fp,'_"ʘόPl"Q9ܕl/Ӱ EL-<׬b*k]@Wt<,6oGN tyIFj'gGO|ͤլGVљ:<=oEGՔگf0iA9TsHg&
Li~[zweSjg|cR]@'i)`62ymprS7tzѷZ'>]7H8]r.Or-=dQO%I-JwI.8#b&H2=&[kV!^/pdXtZqzj]^TZ腮)	'+	X	@䕀BD)M*(aፊȻ	th|2jH Wd^<Elk=0o(LAkHhzM2E#RwHiA]D0ڄmn:jkV')ހ̖.bAc0ulmԪe]}U;TSZۓ2&&!4}Ի
k!ʥuRJUm@ 7(!Ƅp0&2Vuۓlsy/+g0銷˦|,t	>_rm[|iSW0+m
@t. l5"QR
ZbƆ[HPSv7L.C#i͵i>A>\ozAb@.ߔLuV/eqGe65bC XgԌ@lad4Lm
9ЃV:80G7rŦ)p0=qmZK|(uŖw\8і7=!d(LdWܝp7b|Yl-+[7G%_#uЌ[k}|/CdCNleoIt2=@ۜ+!lv׺1mtT@|gk^ؒ L3Ai[K0C(`@)ם_6r$b6ex&lP<ɜ)ցIT1s>˛m7eHH,}ʑC}،ʛ}D?;hq)50z,1_PaH/q5`F":wo(և2Π㾕Nd+;ޱE.Lh1so##z˄+CLvgJ=trK0ˠdOT't1ۉ01L
1vʑvv.l R_NSg$ܐ*5܈s3}vųwGʛNpу.rZAeźrSDJCDXΘ1lXANL(k&j3S׷j팆1<TϜ;~x9=1_86Ej4pFVm[=h<ҞGoL($G-6D̊KJ*
OivlyizCu*
WŊJso
{?(zvK_596j 97geC&7m{@vY1:hK1Hl̖
DGEX xnvtK
r!uOS! la
Z[Е!gk(au4|AIcbyؘq#OÈ9(t
8Q<]}'v-LNґ7ՌWJ[wޤᦹ5vPј˦~ 88rO;wz6XI^adjwl_g4]{ʾaP|.xYhaRlJ&4m@r(pp98=<f'gs	x{^೨-bnC®|w9QK^d/WssQz"a8`B0#;?SS<<T;v[@9qZ}yzT2(U4`wYXc=s:@0WvS%bWkeEKazSW~Gov5c#R->
 5	e<$'mo
}%B7Х}I#2UQ2ތb89;|37N`˥繘-k YVĕ'le[}n;NQvLU&Q84o|t50p`2i1Hlj#U3=Or33G<-ۚh}RU]E@{\Gzo8NZklRwePNS;X
SNTrY\6xݥW;<l7Eo>჎dAM H1h&R$1#Η<z$7mf'e+\ n$i *?;EpLrVm z\"IÑG:

fh` XXS!L(d揵_ZuMI/<Ixp\=zcE37o+^Y1Xg>FL[C?1yIz41ecPl&i6g*IĜ=>.Xk BEE±/C4	_P60ˁ}GO$B
;cdX?~ZrDet|SL߭{~04[`?L8pN)5ěE2TِXÖU񺟈
=k)O tᒹ67eiFBNfnq/E9%fױҺ-g*
EAYAxS'j<(HsYCW)tLLmSÝ'^BnUf0bykM?a.,Q2Ga_#}n
=_PV9?VXI+[zH'
+bUMe&Ji
n9g&*FLrkw:B	tG
%vx[,x
rF`z*ڰ]yP1zˮ5Xd)OEdd'X *ֶf/کTv߮򙛂D(<L`5QOط󆪒YPYMxw9vFKj+;Rb$0
0vrU;{tW<Om"SejҁDa˥.xm2!<9esU.Z(փkg	rvkd)pT5Ll	8\ȵN/OPz*=n:ܵ,8-@cl+m43~@d~nɛ g{@VAOXNVuBt&R)	+;X.B!$żQox)M1uĕst#5;
cMZe"%jߏ8f	.g|1ݑ7{F#U>s~EݰgAV-hxD(nk[tE.N?cWyݷ
e^c*%\x(%#\pnT"~7>AS固CF*9!,2Dr9#_i5+ȗ^6{bcQ-Y0z񓽽=xmDP:jS+DQ w=OVxpwik!KcJ'(ױ"*.Wgk|`znnMxLEhr.8k?o
9I." 8u9:IkiaZdրm4"28@S͓+߱eN<wI5nt5dSYv;MV$0h$1Io8'g!7^ӟ2x.
dXtZvź99Vn%]ELSrQMeGbO&$lQj) vӬLkdNN`$ 2=Q.rLa~ݝFX|Yra}rft4\ex{BJ TgXGC$ 4qߕB#2fjSY$WPT-~0O^vݶ2~,,ٯҀTl30h2t,l
yH'ܬ&Kipg"g^3hR)Z9l,|/7&)%Q	%K>(lUL3i6.TGMF4X!:ƣo0C=ZJNV6-*0(-Ԝ
oGDM=tM_b
Thdg/,Q]}u&tE5B!s8QʍvEݨ!@!5ށG3J>?O0ԗ0R6xZS|,q'faM>Y\Gnj {RB0RC-dWoTRVvޠ=Nrzs8%²eiNA@¸|Z7X,gNt>*x	`Bv
WO*KoFzGa=mnOsn{jE'P =!9>}ʄ50t.ESmepm{q|[	3V$1$x85Q	C:Gy>z:%(sl:Z^Uy=3&e@CqFPyEWx"!յF9QD0r(mK}iwمGw'&GɛÓ3i`K]B/P\DdݵSHV8

%QJLC6}%*bp8_SDVzZHFIojА)Y\9NzjgE1kUb -?E1784Aφ*Y)HY,hHS՚`7-sM7_gvɯxm%>ؔKFZ1)Jx| Cݡh"Bz+:6{V)ydM'5sA>9rSNQ	%EW]+/mΑL0\b|7Q %KoI⌁`kk=qwJ[~_"µYv/;&!P-<[0B>ڣa2 *!p#c$**kUp͚stmNgFYu3Yl袒ukZ.DeYW^a_蜪Z5ֆ֍j٧byYLcܼqTm#rof~fϳ}C'SH-e:Ǚގd%%Ba8 <aʧ+}r,g#aY@o8[dl~658 ~~!w<>|±OMHg/qYv1Wr`LL${D^ɔ@W譶2}eObǛHn) sBj@[ݝE'a9fBM(\FٹTu/n`TQbMQCH 7ƻ{\%hL-DaYk~ހ)VѮa/H^Pڕp*vNl;fQp;{dEwD|E^9jyfSlUgWzhwzxzkW
2vxyو"s}
.	g 9
݄e|<s.wy^
~7<yw8N
/G'g5~aPsqRփZn|{f1GVJ}KOfne$WŜ̒O7jQNťlE氅7jY11u Mxlh{(絸LIn A3?	lP&e$߲ṉ@KK7n}6,5v}pn
"G,zEMȴÙFXx9Fݳ1F E0Ƈi#HC!A3?ޢvقE]1`U\^Y*cxZt|HD$r&{8{(ޗ5h%s](	|a^ђ;qS]gQP
i	j<KcP\(fYXѣ/)8đ

ÈT`MZ,%Md8/	ַS+XoW&⪕^\{l	a1?/tjP#Q7Vk
/
ud"8HB/GN3Ag4gmt/K'ė{4YE@	%kpKJ/%"V=fk20ThX91xuVG>e5*>Z|~HC,KeƆGg0zXapfo0 3:@9[
`Þ:(Y1U$[G.#Sz'D!]2mBwKIPY
C4?3EŴ1lk n^.J'l7V>)	*;HFÁCv4'P`h>r'<	Uav%Y3A0
dCCOȫM 
(kx(|T
wNXE(y@	D!(+ܠiͧlFjLȃƪt4e@cBC|#jou+,uj ڍmٍ)l5EWܥE{; <"fa_EbTB`޿]A,;<#{0"ns*
}A񬲯AzMbs;آBV]Svb#9ʠMKUy}6![CQ^Mmc
!fECy Pkت0ldaؽ'	FZ
B?ǘ)ŧͯG>>BQh.7fS 1tCP#ƿ˚Po_-W'G@pr|YtZthjd ѺL@j
5-4yQ; j~ojvBfFF%/V5/'TS^X!?.Ӳ|֕40;g:2y-;53S%#"Ėba@&>ot|'pU{}fI	쪴$@솩mMrRά^qbp J֚!kqp81|f?ؾt3x^_40dcǂ}qy׏}6A^XJsmOF ":34
;=XsRGWH͎x$\6Bjl"ˀoda
!&a.z5'zzޔJ	|%EH#N!rDk
9t)!P)@]qYp(Stz":'<'ꃖQnJc 5&'Dbw=},tL8EĄF_29c]
P^ӏ]gX{oZ)
_0DKUOKlIU%:a	YܤQ[>QرG4$uSNDln>B>dJ	TA,
j,)~me=Ln-;+DRP}_lPjtYMs"%
Vuʻo%`H8mleeVZDkq{jm%zS"Upv`A;bgؙFJlG$Adļ}T
Ce^R`{ٿܻc%vkX:萼GrCt0Jk\'1%y	ҟXx6؞w>%̓9?l)fڽ]=5T݇|`y
JoD q]<[,)_<~Eb:uExf8p
ag< A:(ꎣGhwsR h)]H/ϜA/x0-ǜ[FCj$$8dVfoR("V19V%F"Xɺ+q⢂ղϕX
c:i"7sL#+~)N'JH8%[|b>- 9|B[h'}m
mUx6y+N!,>[(פiDr˯k`)v?1Vo5F>ܶŏ{?xC0/B3];3H~2k(nC9	#4VxyAb䆰;_[/aݶj~(3eѺ=mC46P-GyAFȤI_u k'GʷOuΖ:4zjg`Dh]0nFbg朆!Ű8Y\:<Fzwgulx'&k_yYаN=BT(`-`|/cw5x-w9cK J)@XvUnc5u}٧ Vd+4hֿMZLs|15-Nt%z߫xCbR<
k"ήǶ97zPuG
a輺OA	3HHÄ?RsƘQ G%ЊCS7''gׇz˷PmjWUeF%˹r!oFMiK&i;#Y	
2|0J3h8~{19=lB5(MV_O.her8hM;pZx.Nr:yd^CC}eϽ0^s#	u,R$qܞ%>FUgڰĀRF}Tv(q=]ͭ
0[e0WQ(^B0EwdQ,oi *Xs.:UէJؙa!:#Jƫi\ܰ,цOp߮?ޢ&&ofOY0_(XUc95M(HԒЈz쌹a\żl~(xZ>^7m^RYSv$?xꚈu0+<LUSTbꍡk!``#@	܏"6b_Sсĥ^X:nCB>mR
!@;43ȷsSg)"jhyZР)oG*f	~,y{m3{qB|$ðZivcOȖ͘#"Hh`D`C'$Ud'@t'{w`=rh*^-`đK)UPps֒Ӈ-heD9~,ah>N4/ֶ&F:=aV8
=8Ex 0 N?s춚yA@uD_Ğ'0p _Q)#ʖ9p}r]~	""w-nھoQ3Ng!Ҫ<ڲΎ{pi?E|x-.WTH[dX-:6e/ӑv=NLSF5&
k̀dc0]j~=jvړFz gz`&"B قHX(xpuNU»H:B^H"=h95#so+8
GM
mn	o|1`Zs[H}z
p>,>g8O~%Ժ o$H%F/lcM^䵚DB*%st3.oؠ*p:v|637:u#z9+"[2L&ʆ8}mJxG
{v듇N oŉW>Mluzdu%C8Oz?,Ù OtWT&n#F(oXQfdI}X+.i~Pv34`JEi %{	j9;;hR=%Z3FRՁB%&CG=y	 hr?!((D%qRG"k.Bʊm]!	S(o=|0iB*'2芖 Y2q#
xDYu[P;0{}yUhS;DjkvTQJAI.V^eԒ% Cz	El4g'gsrK?fP_xoh)IiM  ]KO1W9%"Z$"'KQ{Na+Q"AJ;ۛU֗$ώ?=bqD"Pg$ŁX<@AWq8 m =Ԅ	z
̿*FJyYο?k`}tЦXiBY Us(tS=U"/~^RIFT0%.,w
V2t?Fߐ⢼#
Y99P7R%
؝F'G*y!E:ƤRc~mku]`/q:A:HA0Sc8փIFˇs=4)3]֤W\lp~/b''T	c%ww{˕G
yϠt-I}VSw" MׄyfŮPƯY^>֞|gh˙m]ܾ	ҕj47r^#J"9z#"O)
$`Ood(DA$*ݔDUGWWˢFڼթi0bJB]-%J=?.:Qϓ+cbn4 qe1_%
H!%ѕnf8lc:$1}aS9B@Hk)U=2v~}I^6
_u͑ /:wZo,+!`iB2?t{dN8v[#]c=-UkX]?mSs}Wg,yީ

w5\xvǈ5ӌC2j}h ?Ecȏ5>,6`|   2_    }i