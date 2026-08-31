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
            gifSearchEmojies.add("ðŸ‘");
            gifSearchEmojies.add("ðŸ‘Ž");
            gifSearchEmojies.add("ðŸ˜");
            gifSearchEmojies.add("ðŸ˜‚");
            gifSearchEmojies.add("ðŸ˜®");
            gifSearchEmojies.add("ðŸ™„");
            gifSearchEmojies.add("ðŸ˜¥");
            gifSearchEmojies.add("ðŸ˜¡");
            gifSearchEmojies.add("ðŸ¥³");
            gifSearchEmojies.add("ðŸ˜Ž");
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
                    for (int a = 0, N = pollsToCheck.size()xœì}ksÇ±è÷üŠ•>¤À‚Eú‘sd‘)š¤$^ó>œ“J¥PK`I®µÀÂØ…$Þcßß~§{ÞïY ”¨Ø[‰EìÎôôôôôt÷ôô|ŸåÙ«ìäû,öl#ûß?ežçb–Ï›bw>Ïï_M“ß§×?£v'Ëáe¶Íêªj.ë½»bônð>¯ÅnÛË7¾÷Â,o²«¼MUB žQ=mËé¢ðƒüÍûå¦žg½rÚf×Õýìd‹ü‹­šòÿ½ïÉB‰-òo˜ðh$È&Ú/–“à:@x.ÎÏö—GCæ¸—ù!&ËÿÚÎzÞbzûþëÃ´˜D¸} J[NŠz¸ý‚<á
×u]ù4+>ÎÊy1Ž@'ƒÌ
B/D—À.ƒQU7Åpœ·Eö„Köç?gO\EÆ±ámùxµóy1m/Šùûb~IúœIŸÍ(yàù-+ª¦H>)§H!Àˆ4qœ·wò²§}ègþ®=wô,É`‰ðW 5â™_7= ÁAgCÀñ(oZ””Ö¯8Só‰ò§²)¯«âtz1šÅ9†ñWê`Òé9/&õû¢§·p[´‡ãÞFñà9Ùzþ<­äuJÁÈx¤²TdƒÁ(ÄÑ1N3$Ôö;/šEÕ6Ù¼ø… ›¢E{	%Ð³¢˜0
ÓÙ¢=#?íÚ/óª¾M&€:in‡%HçX'bF:3¼Ë›;èé[ò-)¾WO§¤fYO›ã|J Í{ƒ¦˜ŽÏ‹_EÓöHƒýŒü·™‘"dâóy=ßÈžï$28Ê\¨“º°ªË«–&[°ÅÄ>ld½´	 Ðê6Uù#VïVïïÉ?¯8jþ/_ÆËøòë7ƒMÚ2!Ã×+Åƒú@ßÐrÚ´ùtTÔ7¤Í!}91Í‚.ìÝ{A{‚ÊÙWêaè«î=æ½VšÃY=ð$[ëõ|µ_²çðTDØýTUâ]Ü7m1°U^—UUÁDÖ­o’–t×–Ø«×H/^r6¯G„êl3¦lúÙMN–—DÎŽ·ö[’‚ÿr“¦+½Ê¾±Š“„†eí‡y¥÷-(:*Ù_P)t8Ü%Õý"Ö%ÍÐbjŒöî]q¦WdNbJI*àî”û­ŠÝé¨i-2ÑmÕ5¹HB—¡žXO«rZœÍË÷ùè~P6“Y{ß³Œ´_Ê¦}uTOow²¶>GòeT$éášCÖjÆ°Àç³ÁÁ´ßcý~v8m²|ïd¼Ì^f:øö‚¬Në—C(‹ÊO`,ö@E¶4yÂp_{y€È^ÄW{µÇDq“Ùñ)/îåpùx,{ñcqïÔÊ~óŒ¢Õƒ'þàp Ù³Qæ^
|½Õ‡‚MR5Š<»Óñ¼.ÇWmY•mIÖýùbz:½:¼¼›ù¸×CeŒtø¤nË›r”ƒ2·GÈ€ªÜ¬n´'ù¤èÙ%™N<4¿ÉG ƒ¯Îöw/†Ç»?/.w/¯.Ljúf@sWÏQÝ»Ë‰vYquÍj“DB›Ê©6.  ¯g¨‚¡ ’†€°ër	dÓZ÷6Ø5CŽ#dV÷UHNE%²‡3ìFÇEU´E`í™^I(M‹b|aÁ,§ãâãéL	€›íD$;´î~ysS>BÙ<ë›dF9ÅùäâVÕ öªšˆë¢ÊgDK;/ò
¾
‚;ä§ËôÖ;1¯Ú™ÇxÅtk.¢õ¼… 1}¡fÉ·AÊžeß½ ªÆ·¾U=ê°š‰r=<^Îw¯NO k0æ0`Å|‘ÏAðÜß‡2¶aìyÐšå¤Ìè.W(Òh¾]å[ª…MúÈìµæ¢­çä_ÒAº´ì	h{õbÊxŠ@Ô¬‰0äO²Žt÷ËñQû™…kgýÚe‚¤ÉÈ'³y	;·WM1o¤:™ýú+ÚgìóEKþ¸E¼ôZT²™\Á}çt€À)…v—Ž©©¦ŽÑãEäIãPÛôV	Ù¨Âé‘Å×TÃV‡„*w@ÜRµ.Qœ@Pª‚CÄµÃa)Î¯˜²ÜW°?Spß!ê82tRï´!šô	mV72eÛBw§5=Ôe`}tµè;¢ôÁâ!à
úŽÂ®)†’¬	¾Z	È0
L]Áß- Q(h|w.U¼¹V{R=¶À›Šy¶hPÚÀ:´—(°fKÿ%³j€gŽ44Èq•ÐÝsL,ÃzÁ„ì"ÇÎªü~KwÙ?º”¦WÂ&<v‘Œ&¾ýï¤ÒÉ‚ï’*"ZSÂbí²Ÿ&Å_;íçª°7ãB‰®ª ƒ?Ëî„E;Ü$‡•ð÷z&Ì‡ÏVÖá”	+«¡´`5CžÑ:|<öU$+4àÝŠÒA_ X_Bk <Ê²ÄjøöÞûq¦r ý¤«¯ü¨ªZÎÆÛ®ž=·ÂÕÅÁùðìüðä2ÕµpyWL
¢2WD.Æ»‹¶>)oïÚËûzœèçÝ«ËÓáÉá›·—ÃËž/öÞì_ìƒ›_l+’"{ŽªÖV¶ˆ‰­ôe,EÈÖ
ç‹é4¿®L7¶ÝP¶­J­ï=='#üGT™›òÌ†ü}1>Ë›æC=ƒZ£îe:É«¬ØÏ™
&ÐU«¹´ÞbÕEsw^Ü¢˜_EkóÈÑÃ°/€õÙ	»ý51bÑ’Åÿ¸ü4PÔ˜bŠ´oçÄf%|M6€8Gv¿¬©´wq—Ï‹1%b;7wÀšýÌzM'í†«ÿd¤j6‹D‹ÂŽR-Òõl^OêÃéM³lšW=s÷‹]Öl›…`2eNÉ¶ÈÞ“Á¶J*Ä .†-ù*ÇrÇkÕÂ{9ÌÔ\Aè¤×¢xùÄdì²Ù«JR”ÀllæyÑ.æSíì&ÌU]èQwE5Kÿ²˜OšÓ¿rT°Zï@¨¼:(ë´ó]ÝqØhšÀ¥3"î­q\ù\«CbUËikÉïG°˜kMnºIßËUF—ï÷r]Nïíº:
Ï'Y¨™÷ðÃ.‘5m?û†ú;°kÃú†Ø9k™öŽ>&KŠg\$ûTl@éBs6/˜“šq\¶ÐïEKäaï©‰ÈÓ¾…ÛÆ`TO&PK‘(†\\WåH‘‘BèönJ"r…+…­Ð"Êä¨î¼-þ¾(
_3ÙøyD9…ç–Øè®x8•¨K—Ul·[ðbÎ&WÑÂrË‘¨oN «€-©¿–€ã!ª­º¶xô¯€UIêÃ±s3Â‡ÙPÅ¥¼h%c2p /†sªU(ÜOF\þSîMU_çÕq^N/Š¶åv ¢g‘Çèì’S‰Tÿè=Íë÷CVî©¡Aõ™‹íŽÇdp-Ä•3çéS‹bD†,£ÁR&p¼P£‹ñ‰N@ð™Gï	­RyËMŠ³óÓãSjG?ÿçŸ¬ØÇ{ZŒ÷U$²?ÈH‚õ4ò<S;êÐqtü7]`Ìþñ³gŠèš’º.„NB¥:)`B(NXb?oó¶$
Yz«Ó×©?‰©Í;øÄî¡K0……NuuæL»¬¢˜Í8µ1½¼[)1a†Õ‡ø(%J¿m¯Y'õL`Ú¡ã‘>«Ýµ@VtÁqT€áË6öÂ®æqiHewßÖ \„õëZJt€f•4ø°o•ÿ¹Y'²… 8Ð¡&TŠî“ó‡ìûePõŽ‚ˆ¹ÐéÄÍÛ`­…aóXŠêïÊÑ;²®¡‚@~8dùy<bæäqó	»m¯‰ªþn¾ÍÞ1ÂðE2Cüw;ÎÖ]_ÙŽ0Ð+üÞlCÇ±Tœ ÐyrIÖ°j¤ã›lòaKÀœè¡f/vÝDÃZgMÎª¨-y±\¢ýÓË·ç1'<ƒbj~Û™_«³Aò±<¥O,úd]´eˆeL¦|AÿñZÌ‚¶!ÒŠ`YÃ<×Àé>ÑÀ5V×Tx˜Œñ2|lša‚¼¡£ãíÙ©7‡#²ÁììiZ?Þ´ê
ýq*k„í9ZHŠU{‡¿B×T’2pkëÂNê‘1èîiÁü;ö?Iï“ˆOøòàãHñÏùW!·§ƒÃÌ·Š¿2T+uLõEåë ¾ÞÒá;“uB§Ìü+¥ŠàiÈü¥'Øzcg`´¨Lì@Øõ\ÜÞ’UHÃlOØ#bìÕSŒ±Ø°‘å@Çe3)›¦ÇÀŠ‚‰€G‹¦­'g&Î\OÂ¯CW?ÀÕ½”>î¾ÏËŠmÝy›Œ,%›p‘,!¶ÖßVSŠhzj-Hëeg¸®Ñ%]¯`ä<ó.šS îDu=M=gQ›6.¾*±ùzRÀNk…’o¦ú$ÅR¡fv`g 1j¬áf¿Ä„0'Í²Þ#aà`¶ì Å°pŸ!µPb/“ƒÎd·³“x˜u&h…¡Û"ÕûÙbép¡þu2õþQƒ±“µÈ1\Ø¿ö/Xô›ªJœÌ#ãÄe³
œ›Áãå²lŒYDý‚Æi%¨ž±pr­£Lv–Yo	qå©æü«øÉA›(ªž@i{Ï%Î*VtNÆ—e‡S
ml,é,Pª1MHA´–ige;_®³4¬t5Î>´è{:®WüYûºµrZx\[”%}Âêƒ•¼2¤¬)Y$CŠßÙ|ÔuŠ†FË…D¹	1%^š…÷v•?²CùŽ“óÜ,’Z¼Zu¸—u½;qõ#J¨NÃÈq°ýä©Ã¨J^¹¾jÃÈ=ïiÃˆ°Ö4Œ(¤×?Œk+ðTw.æÞÎœNïõ÷¤ÿŒ[X‰Ñhð8ü7_Ã6ô¦²½É6¢7Óã,»;íî}|‚'¾ ¥†¡ø[Ažišt³åÊ §•’û·›il‹PÕxÁüôƒüqc$S ìNÇ0óšž0ÍúÒŠ¡¦pÔ Ó‹áÍc^Ãcœfqð‡“¥¢g‹þèVûÌëãnõe·ÔQêƒÓ€á¿“5Içør¥”µÒÏ¶I»¢Ø|8«©,Ö%%ƒ!Ù”ñæ0[ë@dËÄ¤“5·‚‰ê³„©ÊŸ‡0YU,ØtåÏLXþtL'ÕÕ¤åÏš0î’Î*×5‹X[„@/åe
€Âø÷ŽJí›ºs;¯Cn1`B
A°!WwUªwe<¦’6g-Í-aÌmãv0kcyÔ'}HùáfÌ¾¡lA´&Úýuýq8É?â˜­äbð—“U¹c–NOaDÌ¡	Êò´7ú éÕÁÎè@!'õëEû{#¿Òå¥è/ëw mçš°#½ŸhºXw½›?I[AÜ!ßy»&­5cohm­y6S¶–Û-2{÷h«ãö‘ùHzwØO2Ÿnª—DKm8™½µÕqÊ|$“tØ’2Ÿn$
ç|FÕJðsÀ»£$ˆéëFÓMúÊX÷•N1m½“QË™ÄHÔ_ã¿þìsBü‡ëá# .'îá‘H™©W6t4ôy
´z …¿¥#¤º8ô/ƒ»¼ù)¯Êñ›y½˜A*iGø£Ÿ_×°[@3­Y¤2+r0ªŠ<iOB}ºÑŽ7>?­+[ÕÙP,†üü¨.ÓàÕã~š%?%·ªë‰@]¿Ð=ur·‘€³Ñó|ÚTíK=¸ŽÅ}•¾§x:Kn-2ô?qXŒÖÅ”ÍÈ.&óš]Rø_þð¿Ðç“û_àY6Ô€?+Èx>Gè#DÖÌ°d b¨ñ¥ó!†R8C¬š` 5j¿0!J 4Ü<±ãh-|#ñ¤…So§ØX:ZÞ‘?]7;0U4¹RÙ¼|i¡ûsG©gÂq›P=Sí
Ùˆšóø-ŒE¿<†YyDéhº’9#é+´UÃ­°„ÓBÄU•eÕ5©‰‹±2%\b5Š„ZŠËìC-¡z†~,¿lT¾àG¾ûâ;Ê¤£âFËxByAë bp›2¸#zO4µ²¿°y¶_âNßÂ’]°ôWÐmbq_ÞÏàì•\gò5¸1töô©§1*î¦œÛqšO
µ‹Æ§AULoÛ;êð´cÏoÎK£9y1jÌöTž’ñt"«§'ñe(	˜N.7Èè’?ž,Œ
R¦³ØüMøc~‹çtTìì€Ó‰AIq6Ç€óœÂŽì¡
°è|uÝ/²Dež-ú€ÝH¢'ÒôßHr$3í+ Ä¥ºl“Sþ`:šßÏˆ¤Ü£’ØØ½Ÿ™T°Sµ¯–'Ú@•Ý¢bÄ88È›Ú§"Ï[¾ÑEÏßŒÊŒÆCKxR27G:ébO×‰ÓeÅÇê°QÆ7aËI(¯>°Áº+bc¡jXÀyÐREÓcéZ¥]ÖýËkƒ,…‰îc\‹ô9Á2ÿÊû6}~-÷õö’xæk_ƒÎ–¶Þl{PÎSáéÐ’¦>ákwÝÆ¦ž¢­lÐÁMðB½b½”ò#J:“–œû¼Õóñîb\ÖñüÜ ™!ÍÆLÇR„VŠŸu@$Ë$‡·Ú>ù(Åý|ÐàÇù­ïVBPóXõTŸ[™ÉK ¥#µÔÁb6…<Q£Š¾ÑqÏëÅtüyGQðŒ"~{l£ˆH­2Šß¬o¯f`1|¾Q¼ g˜*Ç…1Õ/dU”.Ú¼]„v¿>ý(~>‰Êã§ê—Ç5Š+ËÒ­uòÕ³ðÓÎÂ5Ž"ðŸoÅãGýÛ#I©G5–tFî×£Å„e\ÿ\“òuYÎ9	É@*=ªñ;»«ÛÏ*Qçèá—Ç5|ˆÒ#¿ôœnð çØÁ‡Ç5torg/Ñjlä¾^ãÈ5¿¤ã3\C~p,Ì!4>?š4ðZe8=‘ùËçÞ]]7ˆ;)ÿy”!áOöõ±'Cë‘ŒæÁ¤þ¹Ähù‹¢'u²¹*Ãå^:hÙÆ€ToËQ(ƒÛ0ÏÁôçúž{Ór’óìf
÷XŸCìÓ§x(.ÒQ"î@;ŠZŒË¾]·Ì¸h!õðü³ÈÄ w}ÐDbX=ÜP~Ò ÄDú (É ÐfBP[oƒ¨ãÝx[±"Eý‡E	çW³*¿.*¶“§}ñ/À­Vsë%ìcÅš/³…½ê˜ÂˆÞuG:&R3	Ñjùòcöà”•rüiÐ–bTxp@á’îH`2ÊÄÄŠ`MyÖyÃ‡@ÃÕ»…ÊŸõ
Šj[³z+‹v9¬<w¥=<ü¦žj1Ï+ä9Ã¼»dÝ¬ç…ÞI¥áçIÉ6 "Ž–¤T§Í3Z÷(¾6—Àä·Œ0£»¬wðqTÌP_H¾q˜áöôètoxp~þ2s'ƒðtOïOcÏ]sèê_:1h¸k\ˆ¬7x%0t<"§ÏrX§fø_~½,?5ÍW”Äb˜Z?¾$ð†`C˜¡³sXxÐxû-Ü®Û³"ß®ëj	ƒãøß‚1žõ6¿ñÁpc)´•óbVäðÒ¼ÏZ%"â5a·ãâ[:Þg—±ãi0Ö†G8õ±Ýäº2XGàÉlÿ'÷/%3^3«5°Ç”% ˆ\–µ–8¤5›;Â¦ØïC%•H(üS]vˆÚ1 d¨7‰…ÔÒrö“Òñ¸¹ÕÓ–á
C-ªW4,‰ýÚÙÎêØ£4l†"ú‡ÑÿîÅ‹’dÜ‰PE »íï½09+°aÇ™sóNÇäé]r^ÇN©îlA	÷²´Ž§v©¹è7kðP* ã?ô3•yP÷'3¦y³Ðï`Q·
\	RƒÆ!åú
,#¯Š­êÑ_ŠÓ9ÎñañÞ¤I=WˆST7æ)oSö;dNŒžòZÜŸNÓÐó'xéò8=ƒ‘Ti3]ñÔ 8ŽQÇ!Àn%ôÚm Éæ!$ôL…`Ð>?gñšp€ò•w7Ž~<u\š“ß,tÌÞ£˜%("ßéyó&lkT|ªƒ¿WzÏ¬vÜŒ¶Âíék~·|~¾ù‚ÆIÖÓªœBÐûû|t?€hL²–7JÌ:âEC\»Þlâ¤c/Ð|µMá?Ï¾Zoaj¹)æ× ø_±ÐÈYã]&bÀRBŸýx•C 9tñÕv"¬.s€‘¦‚xf^Š¬z9; È—œP³tRVÇ5©v)“ëe±W^ßNŒ®ÙpÒÜÒTo ®m¨«šÍëPç¦Ê	ûüjÍþö'³$Ë„Œk
Ë~¢ŠËnfPößýé:ZÇA‡ó‹|ÆŒa±}‚gê&Åm~òÿv¨ö©`I,¿èOYˆ8<¦DøR†@s¼ˆÎ]Êfe.ºKa²˜²ù!›rºµ’š²ß“Úsº’ÚÛC¥CS_/Ý”+^*©ÉoVlR	ñIjïÛÛSÂû’Úûnéöô˜¤Æþº"k*qàIíý×ŠÄìÚÞ¯Ø^×©·¹¼Xqì¦5é/©î Kj¿ýºª»K¢ØÆ' ¹áç^?‰!¡Î5é|bf§#a½Ëþ—5\žälÒ>Üváx&ÔÀfÀðxMÔŠ×yYbÌßó@Ã{ÅšÒ^ÍÆ×ëëRôú²=¤“í°Áiâ)êW²ÒVmÊ‰úÙ;©Ãhïõ»æWbõIUÓA<¼baÑÞá,›bÆi¸³a'ÛäßœJY¡¶œŸªÚ*ðMÓrâ®¹¨(›öµ:ñµ•kQØ%Ó²]o®@m›ö×wit—IŽãù<Ñ¿F';<k›ððÄ-w6©å(ŠÃþu‹n25ÝÈ~Qä¿>àõ¼ž …ÚS<q’Šò,5ebVm‡§ék4ï!a€Iþ‘µ@où¦¿%jzàZ±˜_f·^×ëLtºµŒ«ýìDfTò­f¯²gÚU­s¼¢
Â•YQfé!·Ê¶ÖyopÌ[íÊÕš?n¿<1_šl ;ŒéP‚w‰ü ªîîØÝûñ»çûrã1Xüõé9+½™Rúüôxxur~°»/#Ìƒ5vÏO¯Nö‡Ç»odpsJ¥ýÝËz4O­‚\ú#gn§›|RÌo‹}ñŽ;×¡$êËÀž>çèaÉ<êõÍY0U¥¬yC¦Ð^>º+Xùr¼ß5/;ý	à…Ñì§šÿ’—™ÔãB÷èÓÜ«Û‘÷¯!ÉÕ>KÈ‡Óqñ±¯œ¿¬gåHeD8’.I$)8	´î+Ý]Vº«tÕê&í¢Õ;¥Æ7”¶Ùß¬¾g/a²Óÿ±¿ä/Þqï¦à<(”„Ÿ4yÍBaú÷Pé¶£#Mªù=,tadMS\ÕöhÕDfC†LèGÍt:™t)ä±ÈbD^ $X‘ïó½8r§R²¸RÐëópçô?Œ;Ñé…“ïˆvú1§¸ÅÊýöpæX·˜Õ·˜â{*µ¹mŠ±œ?åófptúæbxp²ûÃÑÁ¾©ÀÑ;j0î=¸B# ÈS³æiöLn¨>#?[À¬ øbŠE(@ãøà+ýÞÑÇ—ìO,	CŒ/Å€ÓÒD-¥É—±}ür»`P¤Þ¯à7ù¿éAKï°QÂ$´=øÞ”À"2¾¿Qä="¯ðPÙKh/RØGÎ˜÷Ñà#,a¾Cô([àw}…Ñ2?3€íªr“ðë“÷H—Î«W ö$ðeï)cƒ#µ gm‹ºÛ6u‘HÛ‚}¼]ê™¼ÈÒ=”.äOÞ	°lÁ—ö¾lïÇ§ûÃ‹«7o..OO."w"„
ùûÕáÞÃóƒ³£Ãƒ´íp%óbCú‰«øÙáÉ	¦žœ!¤ä½4sðœ÷´)/W]…¤v¯ñ%Ï‚Bø©|g§¼Ó»Iuu{»K˜ Ú›ñ’e²íªCêró‹„:Îó
ü.¤áÕ¨ž¼£š“'V8ââIÙOäU˜Æí“¡dìèFP¸±ô¥¨nO½n2%ÀŸÕ|m:µ>è¤:©Ûãz\Þ”ñ„â±?·–©ü¦zç&rt›EÕz½x“ÀÀŠ6À£ õö€ÿeú	^âõ™.ˆ»ß€›¼»î}0A7o‹jÀzJ–ŸV|¾¬_çïŠýâ&' ¸ó!AïýÂf>®Ò¢Èõ-$©Eõ¯ò*¨´SËL‘QºÊg)‘^Õ[‘mh\ÄÅÔu¿äYwØ8J®Ïñ/· ×Å4ï7Aðï‹rôîìj§lLªáÍ]=oG‹ÖÏÁ‡j	1¹>?sP@bé)I‰$(¤‚zžÏòŠ`ÿìuÊ_Ÿ0[«èõ}ñJ¾&ÿ&EåFöK]}Ÿ^¼==¿Ü»ºžü´{t¸ÿt@¸št…‚àØp»-¶4¢+íñ‹ÍŠÏ·¸|¤¾ ÝƒKKæ~\Oë×õ|1¡ÉÜ}æ\.ò÷bÇ'¶–¨e}Ú˜OÕ6ú¶]¦k¢åÂy‡u}*/>Òcâ‹¬R1 
âÎ/‚ÀsîÔˆçÐZq†ŒÅZù*!UÖJð(©¯•ç	éNµV¶ÐŠ§~­ØIW?¿6Ëša›Ž¯®ñ#¿“8Êþ.yègVªÊIIzîM/ËŸ®'Ýoe‹ÛtµðÏn¶z –HI-Ë–Ê/Ïô`eÛè¨0ÂópJ#%Tp«_®êšá†ÉziêxÙs&q®RíÇ®f›i¾Q» òî*Â¡±N·þÒÆµú+à"‘sÎ óÑ‚< ša‡^.ûüyê±m_˜ƒ…~ê%²jRtG‘]®”cƒ(Â)ÒÚ‡'’IA}â÷‡EŽ¤/¡f¯`ýnë”“þI1a«ÞÑ½-§·BbæeUŒ5‚ãú@…î¦‚§ÆZÃÅàñÙ÷îƒtrsd!ßÎa{UºÒenä….úþ(Û(nfBÀ!üÛ¨¹HW±ˆÜ°˜1 àS¸	KïƒÄ$°	Þ‹Aé'Ú~÷ˆ
"=.A+Ï'+¯È°NÛ R"ºð››çÜ\Èwå¼ü”ÎÔNK–eÜ¹ÉŠ­qËÇ}ž-ìÿì ä¦Û“lÉÎvd'²³ýØÉv\ÕnôjÓ‘±%œ$lycq5C±³‘˜j &{ÁœŒ1SÁè>¢‡µ/î›¶˜Fò`öqYU¥sŽÿîö–5çºšr¼iÉ&‹MKÏ-­jÇ–4è\öáF`ìù½ruÑ"Ñ¸3Á»ä8.|t=_æÀcÿ4z€_…­Iþ!A‚éÁaÿ4ÅG0-¿Lß@Ô/°Dö1óù¤ƒÀòøÅ™Ð(Is>'U½Ø{{°åˆ‹†ÇÕÛ„£»b¼¨Ò·òŸ*‚ïw§>>¢Ð”zäz·Ö­ý=ˆ°ÖÅ=ßrÚ^âÿ²téîýÏ³~¹c`ž„Â@¸æ.áºïÈudõ×_3»Œrvci—¹º–Á¢´Ï’¤E–=¥¨/˜†Ö«]&òXunÑsÐF%@å·+‚üìM‘§ãµÀ6)DƒªŽ[«áy|Qê¨Î´ÑWVmO¡Ôá%áÉì:ízSØ\¡ØZ¢f×{á#¹‰­¤nŽ$»^ÜTãIerA÷œ‡'‹ùðÎÄhX‡ñïnÕ!÷#¯Žw«.;¥¼N CHâ<Ô$¦?ÓDˆ¤,ôqù|{v6_bPlúŽ¾e§Wè¼ggœ2öÝûem[ññ©ûHLoìöÚöòWÜ¥L´o×kÖþ±…ø	·a§ì :ß>øb`—ý«¯\þðý0R¬7ºôKÜŠüÃ…ô‡©ó—?\HŸmè1D†~jgØŸô¿‰°æôëÅõr¢3à[²Úç³Wô5% #U/vv²Åõà\ÖçWfRç3mDÜiÂwj`Ç÷¾÷sdapÎMVOïÕ$ŸÉjfZµ¾ÈŠ ïD_MñFš@9Ê˜Lì^ýƒTW„,FoRÕu)C Ê¤6®–õ5l-±!Ò…€ëÚÁÁ´ß'på²—ûðÃYÐ¬õ›Ý¨³˜ÃÕX>¸DÄ˜ƒ¾Veâ'¥þOyµ°.ñBÈùTWf¸ò„I*•’~µQv§lwÇefB‹–´Ø—u½HkÌŠÝªêyTCbëæô^ŸæbAát6/Þ—--o‹·˜‰1Æá
……à¬þÙjm‚Çv“,ÓiíÁµx3vGß*î	Á²üóÂïYå¸xÇ,}\Õ† N~dÛ,1«·ÂÒÓÒpfø q‚<ØÀS¨*ô_´”¼°Ã¶™pñQÓcÀ¨³7|E ©k—¬ƒþåp¼¡‘§äìÊ«Ä©Ù|¬í€Ön²ÚãWÖb˜õD¿4§¯d™ê×Îñ«>Jj°pépúaJ”=ÄdÀÖ“Å0ËÎoF(³DOön<]wŒyô^¸X‚us¨¥Æ~‡h»žZb”´ßñÝ¯<¸«>‡“œ§Ù€úÄäõåÝbrÝ,7œü	Û˜ëe!{QÛÃõ^#ò ì‰K:ë:çÿgw£>ßv”°¯°ªIý|KML[ŸÛhUë‘]!ËQ¢+½†MŠF
xû&1Ý´cI³ÑÂ+>¶D‰Áes†™VÖLóåw2¶\uËÏî¶S.O3u! J* J" °ŠÏž•æhË~à‰JWd/(˜åF?sh—iÄÂõ·± üNÆÖéÎÄb{É]ˆ…UV%²2±\ôº<¢üŽãsŠ¥—d*ZÙ^ 	“j4©zëeO­"ÚV-oŽ‹¾1€·8Òèþ¼ýÿDO¯vß_8Këu¼p·«•¢Û<.Í]¿‘ÔçNÂH5iú8ÏÇ}†—ÒÉ€æ §qs›·*”›Æ¼UFÆ6Ž{öîÍƒð«;=K„—F2E©[î›±
4îs5{\ËÞ;<¤_&Äß®ël|9W{j•«7úøï‘IæoóÖ7[¥ÂŒJQøó÷svÁ½DŠ1¸YqíîXD˜óŒôorÐ“í^’eE_@Ô>ÇÜiL±"}0òÒ†n{5ê*—P=Ñ?ùuQÉ
 b¼RööLÇ^íóÈ&‰ƒa0Êg nndËœmMÉNZyi^/
"GÇnCù¨µ“=}êL'Î:%=<Ž	÷V™oO[SñéF<ßsJ¸@¡ëu	e£\– w€¢v$c9ç•Î‹‚÷ ·1Hä¦ÝïaXæR…iQŒÏè0%]ž°ò	NžÈ`îÐ‹\æ#›Y4þ§¼b1ÉC_£ðå]š`ßþàÈ?ƒË¼y—¡#œþ&ŠÑ¼íé©IÜ|ñT>Æ9€uÃ^ª©öªzônPTù¬)ÆçE^µÆåíœ¹G„B-¤¶q‹¢ªvœÇŽF±‹ŸeØÎõˆ0+ƒkòs÷:®·Ã ×-Î¤ Ó–Ã?Ã“ýhÎØ
%÷_L-*ühZ¾ëöD¡‹ûé(ØtÇæ°gàÂàûšÈ˜µ¡%cèÝ7&ª‚t¼€ü×guU±’Õ&èéìÊ^b^“XþUÜo)©º_ÞÜó‚0¢ÚsêüZGb®pKëÈ§Ô-áAï!¸&a<ÎÛ†yo]‹†Séñes8-[ÂGtsZ³‘­Ûš!¤C¢ûÛæW3ÎÀrËí—#¾Sk–ÕökCàÇU28‡s_%¡îÇsøøm·Œb0I®0²$`ß©é6º±îQôWwÅEúKÍt£¿Ò·LGIýCLåö×yÑÇ·)ƒ÷¡\R‰…z„X¡šÂßAq€Ù¼ôh‚@¿{‘ý%Û|ñBuÎ¯~!BG”å¥.œ-õzUÔc·ÜtÄÞ¸ÐæaÉŽ‘V^„¹e&d$HnC˜þúk–ÆG{´ÔxÐ‹«è‚‰VzÌ; ‚å…ÌÑhïñ)ü$õÆ*
M ¦}5Çn…œÁÙÂHv:H¶¹%<¯•k¯%­É”ë¬fð“Ò9¥Åøºfá§¡ÞþÁëÝ«£K/fë£’mù=ÜËæ?	Üaíði„î³æOì^ëóÔÐ}ó®ëˆÅUOÛrê³Üz?#'Fíçm®$‡åÕ‚:{PF(<oúÛî HIvð`]=À~[šÇ®MôïR³e:ªãö¯ûûæChA%S~:[çíÝ 'öfxÛ‡õÎoÂÖOúA‚nØDgJæYI,ù`Þžô†ÓC„Âƒìpž:­ü(œÙ–%ç;º¶q×ò`ÿð¦Jìð¤Ñ°3"ëQ­ç÷=½×<IÖÉqë_¶¢þ»ô&}‘qéî:x”Føç[ßÚ¤˜ún‰!øÑ8¾-HÜ¦¹›ñ7}åe0¯Ä‹Çz+½¶gçIõ.FÆ\~ŒŽÄMWÜ…Ó‰KDîå…ÅQšÓ—pÃ-&#º®?â±	Ñ‘€øeHÆÐÀ›PAøCÓµ†|Û”Ïa¤ó=Æ*¼„u+F¶l™,N’Ô‹ÖGú)…(?Uô¶ÒÈBù4•*
Ê:Y”¦ÃtqyxÕ¼ÞR‹sÍ{ç»VˆÉú'…6w±hôh¾+*Òîˆ)ýâJŒ
kóCïbÈÑEHJFÐ]×ÑCü¼1ˆ”¾'ã9·˜™'/ðÛÅýuÏÇoË±uVÎ”.Î¿ngß}Ó};j­#s\ÞÎ‰¸¾¬é™‹Î `Sm·"Ãgw(`¶VY;Õ¾òÜ?L©¸†2¶k!'Iæ,YfU¡ñRorëÅUx¬Æý¹Ý4%¨£™>„-ø«7o..OO.LýpÍMñ?.{ÂBÑ{¸²¤n£{”Ÿ çmI7%¿i”c,õ\Ò“Å4(Û[©ô*p²^×Gý
¤_ªÞÄyÉ#¥«Sá=Þ/Ç°¡ñ²½#³Qè‹ŠJÐÐúJ|E3”^“˜ÎGÿ¼7´ÁŸóË&¦Ésüôw
˜åS#0è@tÝ«‘8£»àÂ¿‚IS'¹#ŽhƒZÿ¬€WèšV¢Ç._ÙñÌ%ôk_ª}¹KÍåhVh¾;{Âæœ+½„‘šŠ±ž«Ó*»Œ·¹Á?s*VÅm>ºÇ þ&¿'zÑ+6PG»ÿ<8÷­‘&#ªˆ¹Êb¬Q}€‚›#¯¾=|5m³Y=oý¿„f×÷m¡ER§Õ „›Þ¶wbáìZišJªô¯ÿÖGc™æ¾æ¤ŒÆÏ—5‘(i½Ý0ø!–Êí‰•×Ù]1)æ¹È¤Ï3Õ™·-¥#%òËªçÕSX2áh3Ë,’ÂDü ,z‰_ù¾Á{yïT8Øcãdé’Bã«yÅÈêGRÒ+_,l©çN¦¡ERÖõé˜AE}¬®€3£CWü)X´‘ô~ÑR³WA?¬x˜&žø©X0&™d%½ÃN|,)ÇôØjløùtRŒò)µAïšãË»Åõ¤±'—ët_íùæFévòðbô×`$›£íM®Í!õÅ¥.{Êo¯Û†ùh@‘ØÇø©„ wšž ´a‰»µZH‘Ö‘åmÈ=°´¨d4D·àƒÜ'“¬Ÿ]£1s¤>crþ·C{Âûô9«Ãgtó'rÇ<ì¨Øe1™Ud”ñPš×3`ž+3¼Ž÷¬±Ò%9:Ö›¤ñ-YˆÛM½Ó<L¹Þr¼'`½)„G§“¢ê«pŠ!µôsZlS‡`£†°¬×~‹rõ=ÓuqoŠ4ò55¬-<dªK(›¬Øc8ÈÙ`t›_õ žójnÅùOÄÔ)ÛÖâ!½ÔÖ½4ˆŽþL;ú³³£?ÇT¡ÆŠ”
ïòòÒ*9~Æ½V p­öAŒÓö6ínT›"´À^ëc°mÀø¡	|F€€t&Òz2kÞ–·wùûºžã 7·1-©66©Îã³rzÌÕíp—9Æ·O&—¬‰‡siŽùÈÉxÔ©·,ÖoòIq1ªç‘†Š&ÖŸ ß³üö]/ŠxÚb‹Ì²ªMîPú¯Âÿ¸;£„z Ëé'èÀÅâ–,Ëm1>«›vwF”©÷yTs7;Â€p‚ÚŸ•R:Er¯~&JüILŽëiÑÓ’
©+·–8Xù2ÓœÑêï%ÜÖ*–æ}!*¦_/…)8¾ýèÂ#rô©®R–øÈ> ÀjÐ}j–ýGÖ°w-”Òù´œ6LêŸKÕM¦l¸xvW­·?Ï?ä×žØ¯G˜z¯‹–4?7Ü­F^¬ñcÞ}1Ž®Qwp‰pO«;"Õ`GQ{÷ñœ•™p¼û?ÃŸv®ÜÚÑcÞÕk_ßåQ˜"gÂÌˆ•Xª»µEÏñŽöÔ{wÕfL)¢d!VÔÎû{•¯¤‰Uç`Å¶páYÍÙèjÖÁ$~}Øµú2fÂøw,Ñ?=âgˆœ³cÕ-<1º\^áîæXÝ—Õ•q·rñ›-Á:*Â.?Y*@•JB÷×±‘pÉxÔ…ãÛðÂ?Ïý¾!ú8ÿÓ\+Øø8¢%É … I“Q‰Ntþ….OR×ÊoA©O"ËÈºx-}»X¦cé¶7,¸1×±æ®¨Åv,ÅŠxxtÉK=Œ°QÊÃ'Œ˜N#y†F1[HYÀEb–ÄÅ÷óùqÑ×%­—H<¬·Ü3+›2"óÄÚ<Hh^\ù ³ooˆw_•H_;µw2des×ó°÷Ô9@-kTœ0\Ì”’®{%–tª>À	ÕUŽ.®ïØ¢÷Ò è[BÁ}~ù¥AÄ'wò£ä%Ð#í,ahWÕy‰?æÃùcy„õ~WT3¸Òî¼¡ßàj^9ï¹u•SMqHñ?§hdÛ™Ž—,µÎ‹¹€Vø-´A¸Â]‰_ýEï…7)><“¼œžÑþOÉò‹GCH¿ØÕOuHO7£z2ßÿå+w„«Nø¹>:ú=´V‰äÄøsil'%ÅŸKS;T^eàQUäÞk†Õ‚ìR
D	úà¿.x¥E…¯Ÿ0mñ´½Ãžðo 9¦	
üu]a›Óf1/èÚIÜfmÐv½Á× OØÙÐý›½£[«cS*ð
Îø^+£ŸŽ5=ð£ ‚Ûš91Éßi¨ŽE‹ªïÊyï0?0æ‘ëzSÆ^2«æò$ªäs%AÐºƒO+±FÎ`êß\4ìy°¡¿\ÔMÆÌ•¾ÉéÏGi?ù¬~S§›ú~€q…úèxJÎw‡Ã;^ð7NÌnÂ8ÝX¨y‘bEÛ²­ÀAsTòªPÎÌA˜1Í~yN¬Møc°;Ý•ïšTÜ‡=b+†CŽ,ÍFy†ŸOÝQæ¼h¡ª¸<xŸôiDj+Õ\&Ô
Æ“À8¯ôJÒÙ¼žÔbîyÖ¶¤4²ð¸wÿôr‰ð¬5y×ã“×}­3oŠcD(<ü3µ§	×âW¹yœ½W¨C(Ž&45Æ‘ÏU:‚¸6 r”J}&ö3Ok
w N/¼™S¿êËÕ¼|›øìî9PM˜ÀP—$•g4½ÿA×G Œ²Zx4<¿~«ÁgZ˜£	…´Z!¥Äðq5v1RY6‰
ˆ–A[¡Ÿƒ#˜¢nÉhù‡lQOérûÂÏDca oÂ)¿¦˜ Ž9ÓêéMy«ž0”¥ŸÈÑVàýK‚”CëóáøßI¾eSrº~ïòF[-ü'°0pŽ•µW\¤­Ï‡£Í:t.v‚Fá)ÕH"6åu*>»&‰Ýˆ‘F QR	£¥Ü.ì,›Ûì/¤G}¢ž—Æ%Röm9SöÑvôhQ7$Àˆ)tÕïÿIÿ›N¯y±Éy¹N„öéù€¬ãÏCÛ_žÀÕ•Ú¤'Ì0EÁM>‚Cr/œvœä–Ã›)˜h—¹Ö'§áhÚ#³)¾¨|Ž™0¸¬¿9|uRLÀ/E+£ÉÉóæ/Ï?d#Ý	†V«ì­Ç1;Ö[¦¡ØH‹š}YQÄB1M§…ÙŽR¡‹y=QË)QÙ(XH´“9ÔxIh˜?Í<BP‘‚púØ<E©°MoðMéš~‰˜éË,ÅÖÉ±<S!eá9ƒL“yÕ¬Í¨:–ü½ª$s–0×%Ó„wàÊ#†,1!Ž©²Êú¾½{×æ*ãwì«šå ‡)å’žòÇvéÙuWWu<ò%i°­×¯¿fê—¿þõ¯"ç™4‡7õô­Ñåmµ«a›`¼8·»cÎi{L£z*‚ÒzÝñÇi3¨†ï!Z’ÚÄâïŽZ–Ùh@§s´ú¢3
Ù»RÔdŠê³ùoãØVOýôójJRüë®×Ôn®:ÇJ@Ãí¥æ5CO	ùUw'CJrˆ|+xæðü%}î|	 q²•˜ÓbÄ¡Al* àm'Ö®Ç¨xHžZ¸ú<ÛVÛ&Ò¼0ò	š<`¸‚ ñg`²bì´_Íû!´n«H_–´î}ñÃ}[ü°€k ežÿžh¸ÑÝ#X5Â^&ÈÞ7Ù³ŒÿñŒ€|˜—¨Ò½ÕÛükbÁÀŒq·x‹* ^æ·u„”œ~ðXÕÙe³d!'ãI@]ÂmOE>éêé‘(áÑ]Ö;ø8*ðzªÌkÎÆ½x~Dx„ÆéK8EPvŠºp÷Ê/¼w>yÒˆë´„EþÕ¨ð[Cs6R|[ƒãûXç¦7o>ºñ;¦!ˆM_½IÊMbÿf®{Ù“D
¥TtŽ>uŒ¨£/€uòõxð®žæpÑË‡m`]“æÔe²¿e/²—†¢[zË¶ÈûM¿{AóÞ‰£_º!JSj£q»Bˆ­n ‡Â ˆšÚá¡Ä°Ÿ/¦S‘Ïê)N½¼ª®óÑ;Ó‡RÑìýê˜t·Ë„0jÑÊé^°Üs~'bÒ±µÔAdiÑÑÄøO~u½ìLøÝñ 1ý!ïLÛ6nM£÷£1€‚•|2ÎÉlÞ©'ý6ÒŒ"tÎL‹¼Üžì€6èàŸ“|«:ØðD¡ó¬Úû~I—ÆQÌTAŽTå¤Ú"†öçâ#¦¯L;Íb÷IQY<uÞ–-‘Ð³íÔùkÞ¸áýYfó†tý¹sâ!wƒøÃ À*œ8õi0Dpy AÉŠ­úñOð¸ÒÆÂã:.þË€%áE6%xÃæ°øà¹9\»×kIàû¤²<0™}ð`½(éŠŽ)¹ÉÊ´ÀŽVù2¿Çoyûw4¹u*òdè¥jÀîšS“–-Óõ"cy»‚¦þÎõ‘Æ}ŠÑ…%l]‚(ó EÖ2ÔÁÃ´Žqn—d÷üÆþ¤L6ãZó®ìbmÎÿ6i…nKü€ÛÔÜ5¢Òï:±8MÉ1š+‡#Éü)à>Ø¡!gÏŸ‡ïð¸Ú£— hÎß‡¸â#m¯\'*ûÕÖ3®¢…<ëðx3XQP\{$a/”í‡p®.=œàHCQâ:yHVõqe0Ñ!Å™„{!¾P}9N\]ã@ÔäƒyØ{åX¬Èj£ÒQdqIé<ú,Ùƒpò ÿWÿ´oÔº8zckxl¥HÉÍ †¢Ý*t^Üv2ˆ¡·ÀnÝQõ‰ØÒ-àâÍ ‡ÑT”Ž˜íÃ Ú¦â™Øh´M¦°DÚLe'×fNP¤ÂÃØðýÎð<H"xpuB<ÎèŸÍYLñî%aÃ¹zæÈ,=o¤fáúÌ·6Yª5¤‘ù€d`£úO@1ŽªÔ•íŠHïàMþDN’™.ÎäÍ¯;x€ðMU_ç•jß7ENRû@!ó5¨…yYáT{bz¦.ÐtM*]Üås8ð.N¿‘—os-£ÑOj¸q72§|nhp0.[ÂÚþ³éÔ½0 C©k^N›ÞÓtN¿™×‹ÙSçÐsµ¿À‚ —T@¤?Ð:(¯ßGb9³N
ºOXË>¾dÁ}iU4¶¶'Ì áÃLóT@6ks°ü¤Foy2 cc·[Uë9 ä7¡|ìÂB´Û{Ÿ¥ô`Ý#…ÔM'Þõ€1Ã›ñ4<Õ©•É¸Îb}¸Bh˜S#Ñ½€­nø‹ùflu!zUÚ =hë÷ŠF¼¡û	ÐÓ¿ÝCô£€˜“Qå§6ÉÈÑüOYn—_MZáè7uS)Öy€WŸ?÷+©ÚÉJƒqý½6’å{â1žÈê	£ª.Éöiö°Oƒ§ñŠZA˜"Ëèž¸¿q3~O#(Ú aN0_øjl6ÕÜÕ†3ZÆLl>ak'Ø“­´ž|õ©0q,«Ä–’
ìèëSì÷0gy{Gðô¤ÆŸ®|JZgSÍIÖ&_-Œc­üå«‡"ù7Ë0Kq2˜,ÚbHÖ2šÃÔo*7v±™â•àI]ãL¡ÚÏgš'ß-5j¶þÆc‘Ž<±zB?acˆz²¹õ_kè
\¬ÇúrAÖ9²P»Æ†õèŽ6Xì?Q’¡‘ûIE™Ñâ—+Ë˜=ð ’,Šú3£“¸äS	afnÿÖóO9Ï­&¿Ü‰®¸iBmñ~1]xÚiŸïu>ÊáÞXšŽÉ/,jûœÚoÃJõYçÑLþG“
ÆÒ²3‚ac1û%æ%PjÒ‡	¯ÉßìG¬ÛÉ›æYÃ“bÛŽˆ…Ã
¦Ç„Úéh¢¯b w4¶™-¯'œøâµÆN|ýû$	•Ýæ{R«W÷È>ýî]—Ä8Š|¼	Ãw‚ØˆÿöL'0y—Ž;H'ò*¹QnÑÄd5ç-$ïü¤u@*A>h5Ò‡ ¬Dy%s†‡ð®¦Ý™V¶¤:öà‹£sQ¼üçÙÁðü`w¯±÷…×ÉØéÞ¤Xvµ\×*¹Äêhî´z=DöBËÛéÕ,¼#
Ô¡å<;´	»¼4Q°w1UZ||Ëô:×ÜÏ²Þ>ÀZû)×Ùu­±_ÈúêÛÐüëêšÖÔ/b=]†ÎË¯£_Èj®Ÿ±~t>±ã\"Ùµ"þäY|yóÄW'39šk±×VA×ú¬à\Ûõõ‰ñ¡¨w‡Þ[ÃÇŸe_9…&žs±á,ý?uIO(ŠÍnWZf¦yíÆÌ;ÿÙXÐÈäWÛóO"w`²n*™W{ì¨9:ôDßm	çpLYUX`®~ ¬úÞ´GxÿÀ/?åÕï
•;k›¤rpú-©àß@ÿ¹h]‹ªhÁW¯É-ê‘j®;Ñ(Ì‡«é»iýaÊÎ|ÐÃæ”íDÌ0fiðfYÃ 1Iéš 6˜Ù–ôÚÞgŸ±	ã8÷-å6­Í „ûLNo~,î•t`®Ûzƒ™€õ;(Œ@µ¥Ž—ãòm…ŠßDí<*-i=U­U§¹KDàè›¿]¡Gò³
ÒøÄ#—àŸï•™ICšhÞ £ŠlÉlzéIsÜ	sbÉrfŽÌEái%ÌqÝ^…°’2Ë¤æñçŽÑÉÕsÆ8·Â¹\:YÏ]®$I‰³§ÆL›Bz¸—«P(à‹¥§éßnÜ" Ïš·!`³.œªzá`%wù)Hy295»úðÆù¿Ê½!ìU¤¢¸ËQ©ÉßEªâ])ÚM%ð"R	/LQ+Œ»Ô'tTB?'af%¤) ûÙþá.® x&Z3W'?žœþã$õE,ÑY<ýÏŠ©¬1Žõ*®ìpæ-jÎû¸Ÿ•%šRÓ«8¥£Sí®;ã—äÌx²/÷!]sŠ¹®ƒ`…»Þ¢"Ü-’z
”+f.ëWœbz?fìÎÔ'ûÉn…PÏÿâÁ•_uµÀ–¤Õþ[w)¨O—T¢‚)®Sl±µ®¥½¶Ø"ÿú“Àù®¦Õà‹\{ÓŒaa%•AÑÎýÊD/dDXÙÀ’“àfî3ÛÛêõ³¡Ý%s="’bŸÏÇ­7¡­}Q•+€
Ï¥ÔÒðwt*†š¤Gchq÷Mž
hó;q•ÁA¥Õ]ž)þ˜&—8ÁŽ˜P(éD3™O$é*Ú^!1‰È•–d`ä+	ƒ3yÄfò×læÑOãLã¯É¿á\Žü¡=ƒSÀÑ@ÂL%ž³‡š±ÆÕçÔSó²®žaÁ+¯âHÁ9”ËŸ¥OÙ›É`¢¼HKb¼àÙŠ•$E°\ø4wZ$b×XŠW3AÍ@M[³äÜ òJs:‘d—¹5¬¹ÁúµêÄ@Ønbx¿Ø^6Ñ±~æ³Ý"§œ¢wT7oöp_J¥xJù!Î_à&Všk²Cþ üÖ²[è]Ãð×/mcz±²ë> HRIx\9Ð,ãDw&W5(¨W²¸¬-Ï¦ˆË‹¦¶u¹iÎZ·Bl’u99§öÏ•¸¼Û×9P»ÛBË'ë¾¾ ÈPf%xÆ)ÿ}9±”ÉÁŒxœ|2Ð‰ðKÂÞ­™K7–(sk¹L™["Uææ‹Ž¯	™2±ÜÒÙ6Ù{kýé4ÔÁÚ­ªå“g<Ð¨+º”c*ù2ÌŽx&5Þ•)‚AhîY£ªe[ Ø$òÕ7¦­V=êÚ±{é’ÆŽÖ¤çÎ3ç#pþFÇýhB2Ý&—7Ò„az#MÜ9©Ë“.fù¼)0¯š‘ÞŽ0"wÚâ.lŠš•vÌ!3ør¶©-(YÖ–iBj˜;¨É5û+à+M¹´—¢Àt†,UrÀ#«”ª5;&TŸ]Ë’ î0.àG^ý±)xëroj	;ð–&±oAoÄ{„x›æqšAw¦ÅRö¥tÓré{êtã¹á…nªD4Aë[ÌÌsŽÌ{˜?î•–^Sht:ÁR³mql:&€³=)’)€€~°ï&BRV?ôƒTÅMÐ‘Ð5	¦²ö:Œ¦“[¢ãí½ž”·sˆ1¶uÊ‰€ÎD°Þ8³Q²_Tªj%Œ\Q}^£/…Z_RFl{y’ü°x±dj‡wÜQÏ›hÔ¸3Q_¿XžQßgJÝ0áý3êØh·4úG@¿¿Q£ƒ£¾Ý+¥BD± É)B6ÑVÚh*ŸlÕ6ªÊi)®ÿóíL"ùÂ»Þà7€lå–Ëí€ïå<¾:øÏŸÛV‘IIyiQíÏ?îÓD¤"ûÔá	Í>å·ƒ]Él­õ4!­!4š[9ÓÒ¾bI5Eü‘ŸZ†ÌÐ7º†îp"¤¸%½œÀÂè.›ƒ–7ºcRg'cò^|Ú+)útløöÿ2½Øªø<ÂõŸdåÇ:,<’ï(0±>8†Ü&¼Tlš!}û¤”ùäÚieÒÅ†’Îµ¹ÉØ<{­2õ9|ËéuýqHX8$¡ï¬v¸ƒ¼‰¤û1=xÈÞçíÝ€¼¢mcèEA•suØÙÑzÑ>Žž*ˆD»*Ë²¾êíh?'D&¡¿gŽ¡Ü<ðèòn1¹Ö½>ª‰ez«µÇuyzšÆ¹ÕÉŠìŽXìu»±QàKu£MúT‚¶*¶Îü"¡("wÉ®ë ìäÌ‹YuO{þn13½»øíÇâþºÎçcÈŒÚÝä E Äwß¤‹›µQÿ˜
ÆËZ5´; €µ^(ç#º˜ÂlñQËN
2‡±Ámè½zÊ,®·yswœÏðNu‘rt'Ã¹K&­ÔÀd&gó	‚ì¥Gº»b
tÞœÆ±`˜Y“$<¼¥ðugçc"Ã”>÷³T|àÁhUìS4:o·8„öä•~Ý+°R¸B:•­	C¼õu'•oÏ¢o­ë}Ûhé«^§¾¶Kðv‹-EÛ¹goÎèeŒjíÜÊ¨žÌ@¾²êxèG;ºH¡Þ··IR÷µûðvùîõ¾ßYªÎyŽˆµÛâï‹bQà±F~ñ£ó0èÝ/oØ)5·<ò]ï©|¢ä²7ý¼5åY(tt¦Wg£ì6”¼3–ŸÉã‚vl€GfãÁSVÊ}žSòŸ²cåð¾‘„<¹<à•Mï˜8´>¸VfGýÁ¨*ò9™tûóü¦m¼Ù\UñfR¬vxØ9Û!JÓ+ÝFíÆñNªÐ´ËJ4‘d«ŽKcÌI¯Ò#4U]E„«&vOSì%6dÂ£YV,2Z«ì_¾RüP¶*¸^1‚Øÿ  ÿÿì}isIrè÷ý"žz@šähwmÍH	Iôð2IÍî†Ã€€&Ù; ÀE:Â;ÿýUÖ}dUe7¤¤a} Î¬¬¬Ì¬< ÕÑeAiK-Õg×UOy¡®išÍ¶C‰rfgoªWŸÇÚ¨þÖì4Oâ”_#”´ù[vIóát<»­“Œ'¶’sÞ²|¼Ö–×®ýIÌÁèX j!P3þ~òkñÙQ:Ëæìë½8sè b¹ƒ>ø‰^4c¿5†÷Œ_>Æ/ØÝº~÷ÈQ¤Ã•5¤N`“¶„oT Öb K³ý9š+>Æ_ Ô’®kö«Ÿ‘ÛyþAÍ‚@³tWö²Â£€÷’èõK~$pº±Ÿœ÷Â¶¨,F]-Ñ¼Nõö†@Æ¶êeÉX¡Ö*¹rÚE+ôR‹E8j,Û âq
Â®>×M­¡°ý½dS¬&¶Nä…ÑM1úõÀ^5œQô•7ƒÉ¹qŒu÷áôYÊe÷Z`‡þ¦³»cÅëÖ¦ñÜñÓÊœD\à®eXÈ»4y·± ¯ãb4Ò³z5›k5I$Œ”„ƒ—J|;j®J@¾[ðqv³“‡'v™-†
d@F”Y'’hþÒªÊ«¡~å«&¯ÇúäÐ‰þØy¶½meŒ«ŸØ~…Æä$ö\&–¸%"XíloS»¡¬;¡¼ctœðÍ–Šj-€@Âì[¨Àf:‘»3Ó©²/@÷¿€«W~'<wxtÏQDŒoi\þùOÙ\Ù›A²Žx— 'e×®käÌíkZÛC“Œ)=TAmñý°âoôwùðovâ€µ#Ð0}¾PûÞùX.n Q)Ä ‘pÿŽ}àƒÂwG³ÑpbÝœõ‘L¾8ð¯góÛá‚áäÅ‚çÐÚºâ_t»°óÖ¼;ÿ0ØŽG 5ûŽîi˜ü}[1ƒI42=5²ºÇüd‘„îÆCÖ™…w‚‚9t+O4æ«èÏÒx)šÁÑù­3ÔÅØÂƒÖÂ›J
W<OBCŠ04l;,MÖieMù_cogò3·MòÕ@<P’^©¾)¬kaN¬û.H½È¾³ î™>&Æ¬gÚnLýcÍ€jóŸŒQ{bÝæ¼åøn‘ï¬Ø÷uF7úÂ‘wymù¾qŒìÃ+ù4"íô«RÃ°??Ý´-b³Ý´vRmaµ(îÆÁ^xëêC•°©i8D:È¯¬ëD¥ë¼ðž˜5åQ+'ÎìÓì®3^ŒèA¨RX›"ÙÌ;µŸkvÙ´x©>ðå±nÙ#?#B½©?	¹S#õ´Óür8n©3àÚìXÏÖúÛãìÕJØe¯ÍÔ!þ$R\ñ³ÄclUØ;ç‡!`$f7±†´MˆèKZkQ½í’áˆ®vIjí>)œAÔe#¦š³‹ ÀãjkRL¯7@t©ŠyÖhxwÇñîÓÞjôËîË¨/Ó=:»j†¦Ð¹¥ÇçsñßGå¢Ø_Î+¶ë#ñ_Âöm¸¾Vð‰‡ØxPÆ]Úêî·îÖ»‹^çéEÿ¨¿ÙáB&&ÖëóÓcò/oûç}î‡uxÒéþ¿jãiO i1“éâ6R÷íÇvùtºbö[ÓâÓ"ôRËóK¶ƒo„±OŠÑ‡gÃŽî%;`_‹ö˜Û•]RÇAípÜUÌ™‰	
XÃDÀ
¥d–ÁÚëÔ÷—´BÝ.ùWp„8±úO3kkTi[¼Q¶Ây·DãòÁæŒéÛ…¾5Ph6ØbåQ×;LeF·‹mï!ÞO%ÆZQøê{xý‡†X!Ær‚²>‹(.™ÐG$E)âJÊ5.«»Y•4³oU‚»‚L$¯ªRÇc¯¬8äõô ¹å5ºÏÔµÅÅ¹0ÝX°P‰vöNømòòeçûÝÎÉé%¿ÉþøýŸþãÏÛÿ¹³ï3þþÏÏvþc÷ÙFŠyàžWuî1e2¬ñ‡Ÿº;Ïžílÿiwž„ü»)«X]µi„ÔPõª¹Ôù?ÁÞzRe5Ô.m‹£©¾Q©ŽÌEî´žÐJî¶® Kî¸®0[§ãº‚-¹o’kÚõ…(üúe5Ý‘]2z¤¹°Dààì?ûêXYÓWCekp¿<Ë’€ò(­ËöUp;vi&¹ƒ¯$’@Y/?è¿X&ŠêZÜðSÝ{oõûn÷\«÷[Ë÷Zë÷Yë÷Ø:î¯vî­ü‰©yO­~?}=÷Rûá„C)‘ü"ïÕ*úÄ£muc…‘&¥Ý›Y8Vô„ã™×8ˆF¾“ŒG
Óç—0šHÐc©ÍŠcOÛœ¦·B:_~œûfcÄ©BÃÅÄ(«¡º>OØ^Û¨>ÖÆ+£ºÆ¡Ä(MÕkœ—ª“âÆú%
ôXÕ­$„iÕ¿Ð$ÑŸRYßxØ;ñÛvOó¼*iµÚ2ãHLÝ¨J<…£]H!«øzž’~Ìú¨)S›Ó±3j‹_<ß8„aÊ½ý½ý·}0µÿRùíÞÉIÿˆUß¥T—éüXõï3£¨DObüJEeO@D¹.¦£}Á¯‡ò5CÏ—+öï;oÁ¼Žªú#þd¥ËÏwyUÎþ¢éÙÕ7ò¤›/ ¿ý>ÈZ«‡$á¡®Êyµx#‚f@^ÂHfÃ“â£°%rêƒ‰ùÐy=^>ÙS¡Ë§^>ßb¬áÍNý!…¡¤ú ¿ð¨õèæžÎUý²Ô4½A8S æÉ®‘Ø13^VˆUÌÝ¶aX(‰#/:µ}6šºDëôåNÐ»Yê]Ó^x='mE¡,nÊjË´Ò~ýŽ2LAÔuy7ÖN_@?½_ãßêXŽ¢0uCŒRWÎ‰Áè!fŠçIJÊ·’ÖK­žŸá‹~†”'°Ÿ¾ÞD'YãeÛÒôoßâ®©šaÝÞ‘|dÔõK=å|\ORŽ°ÇpÁ—wûCrJ=ê
e^2²ÖNºÚ/…Y5BËÞaâ.l’ûL]ž.v*cëœ½3–ÉYFHãì‚Ã9/ÐõÉê6%K»«à@rúá s¾ÉºzGig±Àa,Ü
[†	™”Ð~qy€vþ!WÊéšÃD…uüD
Â´dÕ†T`öÔf>‚v]Íˆîe_q†øÐeâ÷íLÙLè$åî·ðÔ.ÁI£¾Ç´2M€|“ÕVZ™ki‘RR•Ç<4{ÉCã/õ‰V'¦>Q/û«†±/Jôä­ì‹Qž¨¾‘é+Ê7zä%Bâ¨W?ó|m}8Á[¤^ñúY iA´™¤³&ÄLÄûu…ÃÒ]ª‚ŒIôŽí7Í&£{Œ$5µ¿¨…`ÑF”ç¶_›[ziö^™ã@¦Ç ÄŽRýÇdÂëZíyíØIÄq¦‘]D]Âm÷mc!Ñ¾áëFAªýB|ïÙ+´C‰xÛ¶-BKvžBÂ«¡`};ƒu `LÄW†sÁ; …M'rkÅñjUNÛcG¶½Fƒœ=uüØÔhµ‡&E“²O0¶’ÒíðC!?àV=o¢ºœ‰§Ž•UiLNý‰tñÅfýt'$pà	Û@ñÔ´Í‰T—ÛJd€€3Ø5<pZ*äÃ±J#HH´ºZ¦èÅx¼Õ@‡&€yŒ7•aç©`L¶è©“­åkJ¶Úø *{ªád2û¸aÅQ;%µ6`ÝobÖäé -žP1×Ãëùly—¶³äD=(é‡åi˜œÌ‡< ŸûF‡d"WUò=êØÉŒ¾ö|´ÝØ»@8µ1í¾¾$ôÒ´Œµ\ãn0ÒÓ'¬ßºXÞTcHþXyÕrÛ‰ƒ’S=æNÄÀ§åxLÅû»HÅë<©eÉâ‹¼‰c`o1`I[AJÖ—Ã×Ì²¥<¾jºMrùBQƒµ–Ï×î4›ÓJìÖkugÖŸß×^7%Ç¯]Ÿ–ç—ƒ%gLùÐù~¡´’óWí69ï/”{Êýe•ü¿Pâœi€E”<Àx—¨ö/„[æ¿”fd”GËOïŽø1k[Ÿ	wšœ½bâu‹úx­{V®WeÅƒ˜cPÜx'®z™~(Á®x¤lû¸tCÓ7¹Ì}9p;jèúà½qˆa2r™¥è¬•eÌ'–[Œªév'(N"qÍ˜(³ôb]ÅÒï¢éqóKx³P¬<Ã!Ùt›uXËs¦¯™¦3-xË´á)CJüIµ¨†‚1Ö¡}t¦­G[“·c¬—[ƒ±²åƒ³ôùhø¹˜ÿÂv‡GÏÎz?m°Õ^£¹xz[LîñØw~Þ?¹\ô÷Ïû—pé\ŽöþÖ?'¸©ûÉ´.œ.ˆÙHÚçÎ&êghv9öÃDò~Ç)§"q´MæÒ„R]÷ÙŠ‰›J#¯„þs<.<³s›t‚”^pJæ£›òƒv¿Uæ|˜ù8¿BZz^S)ª ŸXIËJæ”b|’Ÿ"ÜO<ÞÙììl šäçØ$¾•ÀN/Rt1/CvO 3<[&ßŽïÛÅdÙn°e‡iwõ7Ô‡LÊCõt&÷ýG¯AÛ¹1‰Ü‘™âêIš£lÐ:35›,ÓH¸ô´5	F¾û®ÎàI÷•êºq¶giØ>.y$WÔ(R%Pg3[›V¶17o&—™a0ˆQ1~QvQçeŠû:*:{Â{£¼º‹°µpâ›Ö²KCqÀle˜–C]«ÔÒâs8›Ø\›¥uÆ¦é¦vNŽUsîëÏ™~<Œòüeõº.–óbïÃ°œððpmXLjÒèÖyâ*ŒÏ=«>\Q`VÅ½L§œETRhÖWª÷}ÙH§Qý‹ñn±Æ'Ë[¯=û&ÞE”º³»ER÷²‹‹ö©X[Ù…xT h“}¢¨¦`Ñ# “òë:t’GZ—ÍA´;uÙ‹ùRˆE°Âô½¤Ô¾9&k•jFMXm¤yvñÆ~Š%$#2ÌˆÓðE¢W6d×›ß }kê/­ù·P…í™<ž¼ð®Ï¶5Ù·uvÙ®ºÁ¡1ö¡Á±°‹ËFˆÎXçîÙ ]hîâ¨O~©—@
=Ê¼™]«¬*‹#wËQ%—³–ú±Å5ç…°5~È]>ä„³Ï×hÀ·«Ò"ÿ®JK||¸¸5óóHþ½½ÿŽ’©¿×)„J®þ^ZªXd+¸lÔ¡åÍVš, Šìe¬Ù9ü¢þô¡ÔHe:}©_{-"Ÿ*í‹~±i¯OT…Öû¥’m‹Šª´+2ÒÖ”
üø÷Ou`QÖÐ$â¥0‚¤`i^Ž] ü:¹œæLìJlCæµeÆµE¦u5†uÙ×È¤¶Í ®¶ u(éZdJ×Ÿ>§Ú2Ú"óy¯Œçý1k`8ïÙlÑlÀd¶ÍÛ@Yc¹¦ò~ÊûaóÔmÌcûŒcþï¡FÜ²MŠáœh”o9`ƒ„¿	NÊ1µDÈÙ4p_U –¡K9ŸN¯~.>[9ãAï›<Mžõ-¨qwþ7É¨¦šÍµ!°2[øÉŠ©ú\Ù2-ÙÜ›LÀ~±ÖŒª›˜ËÉ†ˆl•”ò¨V&N«ÄHFì<íPåÜa+ZƒM\ÅC‹¥ÅQ‹¤—ÂfýíÇÎ.ÿQG§ðøÚw²¡¡ÄbÈïÉÙ­KJt¬5„ÎV%›¨¦ïQOÐ|	uçøˆL'ÿÁú×d/†¸»-`l:0!¾¢;Ï¶·Ñ¸…»u6mM>ÑXí;ÛÛuÌ7×¡ÝˆñÏd VŸÿ‰ãwÍ€,ªÄ²ØDY	TS£ób¤˜«‹®ºÞ·…ÝÓ9¼a®­CI†µÝÜ á7r¿Õ¹„´„$³¤Ä±¤d°Ô¯“¶ò­ôýÃµ¹rlSæ…µ%àH÷ðÂ]¯Ÿz%„à‡ºï¸››Å/b/¥&.%Ñ«-ë["r1Y‘J!‹2þ&°&»ÍæËÛ®êŒ›n”Õñl:ã?ü¥\Ü§Lþ:/¯o´[Næ´@YÌîÊQe¥«½[vÉ¿ïnêŽÚ¾²í„‘´[<×•åT…ûT9Pî	 óUÁ'!Å\8^¨VÏ˜Ì“GA+GâõT¾Ž†àé¼•¯™ªž¥?4".f™¸]öB/n†ób|6/¤«cµÕ—v
ñß‹ z7~öÆ´°cÂK?åe¦Q‡!sÚ¹‚ÓVíqsBÌý.@Xb°æˆQOün&9ï®€tOÎµ™sÔYÖü%:Ø×õÈÊ'÷1Û:± ¶yl2*¾€Çá”	pd: u¢†Õ¯®í;ÿzZ|¼ä¿˜ÂŽˆÚ¨<yÂÀó¡xõyQ¼Z‚-ä»¢™óÏÈ&¨êÅÇ «î³Îw|Î°Bu|%6SÝl}œ—‹‚1³ßïvww1/Gè‹]•lSX?—³‹»{fmý-!æ\ŒçZt©£†kÌ;}Ä£œÓ±Ì;çÏ
åËìnÞ¾™	øaª°üÞU˜Ãé~Ü‡xm›2°†â¤¾ààu˜”·%ÈkVŽwðÉž1ìä¡*qÉ)gºóâË¢ZtYÓOçuÇª0žª˜ÏgsÄÃÙKŽ¨ 
‰Ù ÝÈÏÕsHzæé n@Œø9Ã9•98Îr™.¥âV}ðè²AMVÜëÂ“BÂB<ä9ÔzÒÝäÇÆ<3q\Ÿõû7(HdðiŸÝQœz6¤“*×¦¹j!h†•…ÁžuG|ìò4Ù2Nz>Ó¹iZÆnÓ—<ë¨Áð•!ËN‚«Ì\*9˜wÜÝÄ	Q“‹Qe^’JTî%ê¨¨º+nƒÎAZNZUP¹Ž™×[¢‡šCŠ:$˜$Õœì’Šº‹n™Y‡ÊŒ8ÐÈË½gWRL\íÀâ¹pebC×
mtÓ<{Ð=á‰Ð‡jÐG¾‹{	)¥Ñºùlú¯Þ½œþ²wÙüÒ?¿8<>¹¨b%¹§ÿºèˆPJ0yke¢ÛLâåì‡{c%¶ÕˆoÞÝšùÓ‹ËD°2œ*‚xì`Cí©@'ŠeÂúu«»!,ƒ§ J¼BGrÏ˜j%íaBˆ¯¨ˆ Æ
ÈÊ;6ÂW¥•ò<^.Š±…RÛ¤y1æ»éíR‡‘®67›=)ìª«eõ^ð÷ðr^LÓ®f÷š÷¼5)¦×‹V!OÌí‰a}ýÏûÿu„FêÈ¨rÿ"5Îƒ×‡G—ýóÁë£½7ƒþ_÷Þôçý½ƒÚ6&¾€æ”¦JÓž5z®šp¯E‘ÈþÅvý—9
x®é(Ä|îó Ð“z<
¿Ï£ÐL™|‘¤¡8àw³‚Xtõ âAíH+ŠüÈUøÌJ°:*Ó"ôiòÄË©ÁZŒªØ•;uJïUßbÿãæ½Ž?F'üoQ(5f€ÑÂuÑA*\…¿»ìK*È½©Û¡§doÇU‰)‘>4‰¤b•mf	Ž¶ûøäÛ9Ö`D3¿Ž tê»³Ž÷.~æ3uÜ¿¸Ø{ãÇK¬©fJ>[ErÞ deß|¶àgEèû¸ð‘K¥BÞ!ï|ê­–øúDI«£é4Â 3aâü‘‰:ªV]ÃØv^,–ó)ùÑämY-fóÏ¹×Y-ñL¢}ß~ê úX>ÝçÞ{Šx]P½àè_Nä5&E£1ámÝéÍ ·ýéŸì½:BëNÊV¡èÃï£pƒ^Yvá_àåŸ¶Ù´Õ®4x¦Üy–«ô§g],±e]+~½Q˜'¡^S7ï ¹mF©iÄË£7Ås¤D*sž YýÕl6Ñ]ó˜A5¦¢bÕW–Ä'h‰û5` âæ¡Mù?ïêúÏ6ß²åã1¯•ºýÛ}ó;O­2æ‘jðz>»åqö0wsyøqÙÂyŒÎÚÒo»4¸Q¸‚ùfX©(úOk)ôÃ[^²SJNrò÷XÊ7 <8©é¬%s?]IÌÌÞa¾XZœwf$&›‡n&e %€‹¹6v´ªÖ*¢v¦)–8Íê žîÍtÆ—Ã­®rý°kÁ´erõÑçÂCMÐ×½úÀ—Ôö¸òÿ™Ø'ûg”êÔûØëS˜DëÖ¯ëÉ’¥z~"õå¥ãr:s~Ëb“
FèÜô´V"
¡Ï"dO®´Ø¿¸£mõAãDcÓ¥ÝW¬Iád–çáx¼7™p3(þE„Þ¨F"‡Õˆ‘i¤þ÷F=×LMI­±W™3G‰©;x5LâVÒ6%hq,%‹úÒ™ªÃÔ¿¼[î p0û8pw"×„uo¹˜åoÇÀýl¨¶Û#ý”ÍYÃ$$ª4»ì¡¬rá³[ wéó…'§\°çq«-6i›”Jm|<&´=áÚ6vãf›¤JÓÍ’SVæ°oWìèS»¦Jþ	=fê¥òí°ºœÝ)m–µ•¥E _Õë|Ÿ³ÊÜUå‹:cåtæ¼Ðþ"­VVOZv<ði­¸ØÑ-ºwÅyÖøöAd¡Ð]ETƒÚAJ8ÿd¬E¥¥¨'·¸âÏGæ{Â8ÉQ^ê<4=cè‰9O›EW6Ô/õ<«çËé”GÑÀ®1c¤ÊgáùâøY/¤ìå7zéK_~…X8i{„c[Îª?„É>ùÒäŒvfÀ/Ù»
Slâcˆ]1Ñ#cŒ¬ÜDÑ1b–ÄÁ ªb„"“rtédŠ¼ÈÛY<]WÀ¥Î…½Q›\Ÿ ^_rµLÖÌlG”|Nv½X=l¶&Ç3ÌvÄg;
fëÒ–TÂ¥¬[eªhÛRVØ62Â
+*R"ð0‰dBí¥*Ë8^öc·÷“â+gÉ“ËÓï:gPá%ðNæèÎåhšÝØµÕƒÞbM [Ô‡ÛzŸ¯Õ»Åßvë|‡Ù©áù“êôª=CÿzºZ‰ÉéàB¿3~$¼‡LèT5±<ÑD‹QÚ¥}n¢ézðÄ~"Œn›–Â¼A¢³éîÜ»ŒŸdáªt"Dã•1¢"äW¡Ä¸ÌÜÿT
Ý®µ5ïd¥‹!Ñ{e‡wÑÇè¼k}0-J˜XíNà)ã×€gô ¼´ÒÆh9RµIToü…ôzr¦yë€Æ×I¦¹%ö¦’¸ú¢DïÖáÖ0š—NO¦Û_n‡ŸèÉ~UÁœÍîÊÕ»àxtR‡5Ìyuñ»±Ho½x”©ßTk	ŠÚÃÀí,£™j¨Uƒ¾7)i}ì“®ã4É(ù•Æ…ÔDÃ•'íÚ¾JÐÖÏy¯†u¡R§†œì^$„ð•ì%[“­Ì‹Œ…5}á6¥ÎBL›8®cÝ‘"p7[)’˜?‰ùô°n`Œn¬y¼ÿM$»T×J+#Ì7{ðI¤'KLä—-‚&ŸweãLÊµîñ€»)ûÉ÷>x.Þ/*çi+ŽèF©w*6†‹yE|A‘OÊ*žë¬Eø6,ú-Ÿ?0~{Ž†éHCÐ—˜@bþÁzé`­¤{ntþÎD_DrüT(ãvI>ÅK~*Ö)”¼'bóg¾B‚Ò@é8Ë*‹˜éé’ž.ñ¶<_ž5‘¯þ²œL$ƒ/®òrÚE+ôÐõEl¢Ó@{Ìÿë–û;°`î°â‰MÒÔo6_ñz/wÂ®PÓ{ö¢â&¦Åõí$pÓÏ'^·ž“@N3EòÌM@0KZ‡[.”¯Æ5JÓ3ŒŽì[êú8àýÞ	BkàÔéÅ·äÛ)Âøœ«‘e<évZkë¼Ù÷:§Ôö(°{ö7É¯Ðh—ëêä ÷¸Oglè_`äöÉŸþ
µBÞöX—iÛ3eiÛ×Ã¨èªÃš?{ç§ÎSÎ tž{{2úÓø²ãúÇ$Í*¹M&ð…a›0m<Ôí¸lE:fo¡eb^ÙHÈé½¡AK!ôìâIc‚ê|¿£Ð{ISx(Ìºû21‘ðæÅ—¯¹àžêÈ‰öRÚçÑ$[qéFqÉÙLMë*UÖGÊTq•me,Ó¶©(€‚)VMiÿ˜¨žXã›NTÿ˜Â“T¾Õž÷š.ÊZÔžnç÷®þÙ&E5»u¨*m#[[„xéL¡´ŸÒ4¿¦¼êØIð†÷ó"¥æÒš{YN•]u†ó‚¤Ë†²bŒÓ&¼¾šç±0=RÌÁFçeð“lü5pß¶¬Ô›³—¥ENÝ]Ü=pëPÖÁ±?²¼,ï#Ë»jÍG–÷‘åEË—Êòþ.åGT²ü~º±~ð••Eg4)†sÌAÏµùíuàø¶©ÚäT³¿“Æ¿É•4«YnbwªÉè\# ­]ÒžA8h-¨›Ÿäø„aFÆh'šq"…Ì¾ÊŽ<a/ë	å,¦z,ùtÜw‹RÏ"¯%ÏK)ç£Äv¯£¸?R]o$Ëû(ç{„å7‘Š]ï$bÃéËÙ/eñ±â:º	?d?š´ç¤¬._?Dr³x Üi@Æs&»s$@’Ãº%j½dM+áÚÌ,‡Éåì¢˜Štê0d÷y›D>Ë1!dT`ãDHoÖKÒ5X$…±M¼"F]P¨Xp“rr\C‰+^"éèéþPV%ÛgñÑM¨l*Á<Â!˜7Œw9Û‡Ë6–2C{Ç¶Bu‘ŒæâŒæ%²ƒ‰ê’ÚN£ó[H ä¥zÂþã½9Wé	zz¥
 šÇnNQmf÷‹„ýôb4/Ših.c¦	3\”·…ˆô‹ÆÝ…ïÅ›ý%«hSërÚÿtWÎ‹KÑ‹¦Ù{õi¶2æ¹V¢	7`ªÂéa°sñ©6ýà
^w±¤Ù.MY|¾ãQ„®·.ÿvÖœô‰×Ä!â·à¸¦Ö–Õ¸g? =È8'ãR…h[ý—hŒÜÉÍ?ÒÝ5Ç>ÆhÎªb,‚†„?T&”k@µúñGÖ»"nD•Ôrû­uÎ½ÄÚ6Å$sì¢GÎk\cIúÀYŸÔs¡RÔKÌI‚2
F1 ¤ vHÉÀ˜˜«·“þdÁå™ßšœË²<í6ÎvþØù#©/½éá¯½ÎÅçjQÜnQ<.'“²–¦Óý#ûÇt£óoímûEé·Ømñ‰åîËÁ^*Îš7eÑ¶*&±Ž—“Ð1žÕá’âQTœÛ4Þ£Üœ;œ‡OÀ‚·?JXl&†µ“=üD!_3Ø*ÕxèäÙÂO,FâgpßºHáñ)0· íªÒ¦…Î· adà$(ü!Ñ·V'W•€”¿i1MgE! ÿÐÇZX½ÀôÍ*ë
_r÷LWXª›T}Lx€²`Þx¾é¦Öb"îõÎi5«OL4°Ñi³Á)GH•UB„³òo¡ØEf nfª~l3ï*Ø6Â®ó:sŠp±Ã«+žÕJQ+7}Fªf*“†*Rmu±É!MPÁxè³á|x[u7wD;m`}Hú!úÓw½°‘KJÈFc™šK£Cõyì†¯ªyÿY½ñ!Ü0i2ð’¨Ò©{¡Qiú¤'ˆ0ªDºƒÑœß‚)¨SP¹V©Êo¹æ^(ÉZ×ëƒŸL+î“N´I#~‹ï0BÄ1çŽ˜Â(êâföñ”Ï#H{Z3IlˆÑò©…¹ÔRyÔvþä-+H”¶½›ª¡³¤ÇÔïÈE7J>*<U Ê®Ÿ~
!ÒãYlKDª^'¿?ÄPá?N8"LÝfÄ·JgëÂC¹ Ô×\cÉ·F‹œ–6ÅÕ²AT'í¤$áì7óAÔ\,&ÑT”ÈXÎ4{^¼/µ=V|£X†‰}nŸoØî@ô.MÌJÖ&ïÄÅîL¼FuùË–½€ùuëpä`óçBïØRÜå˜¿¾À“»aÒ]¨z¶ûÉœŸ}²ór‡—…O}LaÝî~Ïïlcqf‰T(¼è±Z#BƒhdU0¤Ì„P¼{ô[ÈUh ²Êë‰ŽÜ€(‰Aõ×³ùq9ö	é‚k)å¿$Q’Ks(ð ƒ=­fsó‰D¢{EO&ânÿô(T|ãB…Zò—€`dîÊg®øÇ¹´'Ô”Ï©
š?2ºq›a‚¿ƒ?A€êqSO<v
Ï}~šMä˜;IrTxl¶=Î÷.3çÿz8¶†”7Ñ°ö”WHÖòRïHFÔÐÕø	WŸ\¡ã¢`W%ú¶˜Üé®ñÜ>÷žé4ˆ&Ây>¶¹HH•.S9ÓÌei""‘û·å¶5õèÙ¶nüpŒf·wpwøáSp¦Á?ð;°w“Ïò¤sT‡/ogÓ›ßòòÕGøaœz”ÕhYUÀGå½©ŠÑøÛJ&ªÕåFÇžkXYi)¡f-Vá\¡LÀ*Ã‹e?ù½¯[-G¡îJð}ÐëŒÊYM€\É§\¶/¯Æ)²ONÎn_?ã·ì¼Ï˜t4K¢‡Ý òF}lÝá ê¢€ÓE¬­·äDYD’•;/TôÍG3Àr+CÓý«äEé~àcuSp+Ó7©po	hT¬ƒ}ÄX£7dëü2˜\'{}ÂlciWïvMÄ¦vx8ïª•iœÏ.7ˆ†nÙ†;?lMŠéõâ†±E;ê7tÃ{xú] gmÈäiôÄÞ¬å‘t:âçÓ…b¢µN$&òË 	±uÝá0ip3¬nT}ë+¼!Ð™CQ<5ÆúÍ(%˜™)&,ÊÌ´`…	Úþd6úu«˜ï*ðNWñì™«þin=Oë4™Ír@[pãVu/£¶›ÃÖ€Óæ¶Ïeª‰I§6nºÂñðNÕIÝÃÍM÷€‘ý]ÜÞ[ƒúnR
i%ˆ{½|q€÷æÿ‚Ç¥5õbÛ õíÎ–BšíMD£!èºPhœÌ>â:9®¸f~-â7b¶‹y©Sfp@1(S¤5ªþüv<â¦eÕôÑ–ÑW€CÞHèý€£T¾eÉÂ¥G9Ú`ç>Dœ÷—ÖÚðÄˆfdÅ$"Ë®©Jh³»räž>ä…4ªÒóUŒlŸ {»á¯Êk©Jb6åßbÑ âš¶ˆ¶‚¯­H×Ö³Œ\‹k™ÓÆ‹.j{QÑQæET3÷)9-”&AW<8§§MBíÎÕdx]uþùÂÎ â€ŒÆ[YêBåFßù‡ŸÎfU	¾ÖW'ÅõÐûJ„|POûw³»åB=>]Tå²ˆ.¦ò\Üâwma¿ïÇ˜vÍÔ³Ô=|ä`è.¸-;/ÜÕ9*òÐß
¥mzN0Y-ð½H{W7³¯†cðBÎ£±®]uà®,R„N ë$KÓð5§ó’ªi„ö³9@BSª¡ÄIŠàò•ÈóðŒwX<ìFÅ¦p@x4„×èÆ$5Ìƒ…a ³©jcecb6Í»yña_®‡‘ ãJÎ(B¢Ÿ¼\œ©û$$óS™ŽªB¼á½jlÄ~Þ´	e.|^xà±×>P…`C½Dà^WÎá Ç–óW™ï;½¶TèOŠ,ÊØþiTÒÕ<„+²Î@‹Ð+èQ¯”ƒŽ˜	ÍÏ<nà‘Zc×¶'ò¼èeoÄPƒŠJxÎËøÇKvGYÔp›#FŒùnz»Ô7\åË£ôÕBÑúƒ÷" Å{Ð®m¼.'à%4…¬B<þŠ¿X¬Ÿÿyÿ¿NfæHÉ×ýKçàpïèôÍàõáÑeÿ|ðúhïÍ ÿ×ý£wýÁyï`£VT"B”¬ÜGä¬c´µÞJ;ùw€®Î¯†ŒèuÞì]öÇ{?ó}È}=î_\ì½éG ‚ÏŸ#Î7§öŠÆ² a»n,JŽ®€„ˆøÀ–Îíü4ÄR²
+v1óLU¼N<^-uç­#ÞœÜ…¾†ˆ$œ}ØØL5]ë ªŒ7_IšãÝÆ¶ÚN"ÞŽèÌUøÿõÖm¥ÊïC®Yù¨j½;³‚4ã(ûíPfSÁzðw&@Y‹Ç¨G\€rÛn6€[|•ù¾›P±_å§`òÓ£üä–Gùé‹’ŸÖÁ%7QÖÅãl°¶q÷­¡"†áº<¶óÅÜ‰ÒÄ®Sª½¸æâph`A®y.39ÎENf,
˜¥¼*§Ã‰x‹ò,bÝ<t¾eu¬ÚÄ_]ƒsýè•y?
Úq·Ü§-W†ð§ÐÌ–JìmºI>°©B7ŸmTdÿØV§XÅÕpG¢š¦á€Ühs‚È)ñZ«c¸Bk¨lb0äKµq0zJRÝøvR³:¶ÕÜÞŽœ'’ÏÂIˆŠª`hhõ {þ:3—t`h~@ô.HÒÐ<6ûú“m8KªÞ&Ôƒø¸g"4ÇI‘&IÓoHÂJ,‰A~xü]‹ÑÍþìöv9-Ÿ¥rå¿fåT–J˜]ŒTØFi¾6->-N¯®˜LÓ#q†n17Âþp2y?ýºû#8JÈà8ßGåôW5•že¶¸/;#ÙÔ†¡¸O½®ÜFoýÎœšm}³-,’¤×jÆaÉç  ëV˜”·%ü¾„IÿB0·¹ü|Çä6nsãë«ž?wVÂà™Û:Gm“y8ÄJHÀº¸¦*.Ú²º`%ê.á/S7¨Ìe>og,¼-ær­ÊÉã9÷/¢>ó5âzIÞM_u	½H"ÖµTš²kÁ8¾f`êŠž7(qÖ@Ð‚6šé”*ù„ž”BPOŠFÖ(ywZ-$ ˆ×ÚÔ¨8z—Ú ™ú`ŒÊxæ^z1œ/ÊQy7œ.¬®Û¥è‚v`£Ñ´Çv(Êé;a%h5Qa˜~7Å:ù‚`p1f6HO•(áxø3ÿv÷´ÆÏçbv}=)Y5øó
pmœ?¦J7ûž×[8¯XÍöÎ¦˜y0aò­iŸçÐn±œ2 ²ºO¿ôCê¢´8^‡Ù-Ô¾§FÉáµÛsËYÓÙäCáÜCúYQØ'“ñ|^ —pïxî2 {wwóÙÖ¶é=”ìµëÞÍV]DQîÅØ¿Nìß›LÂ€1d‹òlšt¬§b|¼Ó–>ÀäGDnŽÈp}hMi0ë—¿&Úl5Ç>xGlkþËïËñÑ•[ˆ<£k	ûÏÑèŒÌ	¾(×K7jiVÔ†®£ó@`ñƒwŒuù‡
Ï÷!v!C Ý,…ÿ>^[Q&uûÎs_ð¶µt1 ½/ÐfoØ¶w9˜{ÒÂ6Œz9ÛƒÌ^šä#(·‹ã\=*î;aëÙß8Ïø´$½LŒ5òYñ+ÒÔŠT„Ïpí‹¤²Z8ÑaM—,ñŠÆ éTŽK¿’GdJ©LSã
#¾çMSRïþ*åä¶“qÂ 0#íZfž7aØ¤½I+ã™3²–ñÄþŠèŸòÖçÓÖÁ©ï&ŒO2 ÂyQ†ñ¦‹i;£<ÑÎL¸Í;SöK“=Né{zºÛØ2—Óš}¨…ÁéÖË‹,ËÊÖŠ÷MÞF‘ö~=ÛZ_ô­«Ù‘ÍZRèÐeY5þ^Îÿr•©	AÓ#Àéþ,¾c¿ªøfc¿Ø†<—Å§tVm±¡vŸî¿Ý;9éöŽÀ`êoƒ£Ã“ŸûOyŸ[V¥¨þM$ØGB6sá,Ó¢­ö³u˜à&ƒ‹±ÅJ€™w˜üB3­÷Þak€Åz‚3u	wuÄÞ_[ $L¦Dg  W¯Òüobàm7$Xy[Õ18j¤g,ð6ã/¬¾·ûä•»¬›WåâcYÙªbñšýÐköt¬Å-nu¹»Óë<S kÖSF6¦ŠÑ÷éëyŸ€»@ö#Èº
yŸ$ [5 o7 rê ÔUC´öÞ±?›C Ã©²¸_‡#U¹Ù=–´æÕ–š~+·^)ÄaS[wQœ|ã}æ·0Ô@ƒr:Ö§¬g½X¥û§GG{gýƒÁÅ»³³ÓóKn_jæhw+å#¶Xâ‘S#cj½ª…±†ÆÅÇr.IzÃ<¾Î V»HÙtÿÖ˜·`\Å™0¶ÁUCfƒp^…‘©¹Ê¥%Ò¢\LŠèÍ}ó©b’äC,ªû'•/	ðcæ3àx‹mdöøÉ¬¦8&¸šå•sŽâI±^`1ÑÍEÈ‰‹ƒ~dvå*VöÃ¨ddýáûÙr!#è3t½šÍoï%êuŽfâø2ÌèÖê‹½ñ˜¸Ò‘øÏŠy9c€5¬Š×óá5dáí\É?‚åâü£©FÂø÷ËOr1}¢ç“Qn ’ŒheÏÛËæëI«EB”æ,&ƒ;Qí…†8x4«Ü˜2¦£·¹&à#îodqèK—5zA#Rhœ1÷Žj§ð<­Ü\†+¦aáƒ¯ýiø‰©¤¤aàä‹¼‚.o>)æ‹j_¨3•ÿKV¬ïÞˆ»ËÉéöôq‚Æÿ {¡×gxB°ñëa9áÓ‹r×+	©
Ö´ØÓåôCÉÐ”[YÑÓµØ­âñ¦Ý¾IiZÌmTIÿ­ÄÃ”‘Å50ú†f‰¶F=¯ÚfÞ¢¼r›Ùš˜'Á¯–²†ÁýÒž
‹¸ÞîT€˜mol¡ÙËœÝqÎ¢\±Hã!G~ÿdÆ†ä¿¬êÐp_ç˜Oˆ|Â=	º×	©ï–¤» ÇÃè§Óî´Æ©›“óÄäGÐ·=ÏÒ“a@Õ—­wÜ³÷æüôÝY¶æëÓówÇùldš}àRO‰NÍš¼üÌy4ö³øß<çŠÏÏ;OŸ†màY²|ax¹¡zE ª‰Ý×Ãëùly§ŸIŠhú~ÎdÅÑgu@šb0XÞBÝìdC¸(Î6uíÃ×Ålp7+§A¦	’þM1;ƒcÑèuë­	WX¨Qn±¿Ëqr(l;à§×š}•l>,»ÕL2ñîôÙ#öM3aR8·XGÞf†S’?¤#)Ê2D+lšf„j0AšªÁüpi'Êô4`xÚfvê1:uÃw­]C„ô…¼Õ8
_Ó´iRdZµ)ëaq9;fÔ¸ º<=å§—aÙÿ=ÛA+¢Ûèu<çø- ÄJÑ§QÞæ6† ãëqÕf¯£\ë‡X\¯£þÄ[t(}Ý–×sŠòÆª¼.8Ù]ìç'Nße *F¥®ç:ÆR 1ü7~2MÌ~X_níõÏ%+qqvÈXÑói]g½4Ô³H$ß¤¬N"Š•³¶5]ŸÜ,«ÛÝcKc2ÒçRˆñºœ–ÕM9½Î‚xjm¿¸û·5.«Û²
2Éú…š
Ú/ñÔÐÁ­UPZ¿•c×rMK´©åÅÙzÐãq¥Ä£^˜‹Ý.˜~Ô½3ÊÑÈä‹bxÏbéÇƒµKŸ^oŠ‚rÖ’¡Uz(„GMÁô\,¶G}·%Iñx°˜Qv
¿kyÝWÎÎXg*ûà ËÏek›dŸY4Ò*ªxóhŒ.Pdzæý&XÅ †8PÖ/Šµ,*ž@ñ‡z9Ði›çùÑT8T‡ÏÉ­Ñ©ÌWEº	îŸc‹ê7o&² ªú#QCE][SÑ4Î#á|üûEÜ!ÃÁo;ªbq:Ý‡w 	¼fSmd hã¥G¼ÅMëøÑÞlqñçp3ûèã²ÿåõt6çHHV:{S*éLx2šwÃå´WâQŸ"‡"êk{zBu›ÔvÕ-’UØsFx”×åµGyí«‘×0F¡^4*³õe¬-u‡>2BéòÈ}ŒÐA1~.ÆÝgÛ¾Ð*K4œË™bõ´ïµªÅ^väÉò×Uw1<kë³èN<?gÈ	3"=ÿ$ËäÕ­Ç/…A{ÄªÄbÌOëadTŒD¤W>¿6c{åî—ZD;æfë65kbfÖÆ«jMó²¦¦eí­®­°ØìÄÌ¬ò¤¡@#¡¸.'‹v@ö*— )&X½‡ÒÓ¾ÇÆ("øWð?µ7
Ó¶œ\¶DWù¤è[c1»œßÁPæSåVøj^îªÑÀÝb—¸.mz5³&€ïX;Áí¸ÂLÍg|}¸ˆòAŸ¬jøÁ&ù˜Ô¦$8Fi¯³¦n¸#mSã$!K¹uz{EÁU{`Ñ@a«ã3·©v5¶Ç˜û9w	<.ð/¶F³17Üf#'(7˜ÁžœE£8³_‹½«…Þ0„<HŸ76æ	†ø8œ+ß‡]¹R\dpÔ÷hs'û™lä¿ìÛa½`ÿ™¨7—§oÞõ'§`Õö—½óƒ‹ÁyÿâÝÑå ~~znç×ïàôçŽöL¨ßú¬rpxò†u±ûC3XòÚVÜ£Jæ%¢¹S…ALÉL'I>üÊÓ£CU¹&V[þå^˜óµòÅQØ¤OçµêiS>×ÖÉ© þªçØ/|£êR¬ÉÓo04[EÍ`wHž¬ð22·‰¸Dˆ>¸bSR‘fd#‡°Ä—[ú¯“âãq>‰•˜Œ¬ØQ©~oØÍt)¾ÙØÊ&³âX"*mGB
‹³¶{¼†Á\¹¥9ž°%òÄ§EûÌ££uA4_Ò%?œ-¬èôçÕCÕ#¸Oó"¾&Éü¯µ8±ÊöU…„h{ª8Öì¢©kp˜@Yo¦%°§ÈG)` AÚQ†ê){oìà»œ›ìJDÁ­¬µ«³éÅr4âî¡Öw}q5¤žÄ fÀ¤Å¯¼¢%q¯uOE¼—×€{8Q)BãHŽ†èŠ4‹‰”Œñä)÷99½Ÿ¾>ì<U„„ü$¦Ü§¸$Èjµ¦pM0Z+­pCY·E,ð1,P÷,Ã’Éÿ¬îU¯«¬ƒ¸^2>ãÕ_˜úbxw7ù|9Ú¬Êûþì¡]¢yn2‘¨¦ZßÚÈ&w:‚Õ²A¥æx0®¸ã y8èïg<Ø‹Þ‘{ô§ írS}£ó<¨ºrA#ù,ÐPPúJ/…8ß—Ü»øZQõ‡«gŠª™ì1u[¢¡ªŸ/åVbK}ßóñ+ój©;Ô(*drÓKº=I[+ÇÈ?bÊWöéaÊñÑl(ÃÖ‰á·]2ÛÂkjnÆß÷ ¯Õ#—B˜î7Â¥ÀevQ^O™Ê(&Î§Tú÷¾±3"|UNôøcP}ýÉÅ"3³ YÞŸÒ‰5KnjïLØm†'3[å; 	l‡õÕ¸‹a@ âJY	«E2ßîå~ÿ_ãslïFsÞ›LÔÛë¸O\9j!§õÅbø>NÂ#Á»&1…µòê0sž`ðx ¾¨ÑÖy äP¡ 9^™çxÎ6Ó‰èÞ–c_?'Oå`œÍÙÕ[L0Fü&Áoþ8 Hid¦=—ã"¢ÑSK&ì¤_‰‹q¹Ð='Ÿˆš”÷áÐè"ˆôÀÆ$ œ ˆV~îˆ(X×±õÚ…"(C_iÙ´uáX™Œ¶ií¢?6ÐañDÜ†°³±”m,=ä<Œð@RMŠ¹À$¦¬Ã$w«¶`ËG|¡WóÐgçŸÿì’þÓwýó“½ã~#a…£VCNµ‹l<d`+!¼/Gr¼²põfdõP´‹v)\´ËÎ·í£ýÝw%Íì+B,¥´Õ8i—Ÿþ¥½uË- ð¾+]¬€…QÌ°²Lcp¬¬!@Ü«å‰\TÎÅÃËöÝWˆ®»“ƒi¼_’¹×KÓ—jŸöOÁqÎ!zK„Êa¥,¹ÁÌ‡‚„j‰IÕ½ý‚½ìº[¦”¿”0—N·¤…ÖAU<ÝÙP}Kß}Ý$•D
ÊŸ({F´…Å¼°œÓªœJ	ï¹ÑŽCá—â9÷ÿ  ÿÿì}is¹‘è÷ý-}xAY‡ºÆöX’—"©îèàÔx7Mv‘¬UwWOWµ$nØÿý!q&€ÄQGK{aX#‘ ™‰<èÅin!¤æ…ÒÇ0ì@(fwûe	îS"°uÛø}S¡Ìæ·ò²jN›ÉªÙÂFñ:Ê·fèÎ«æ‡I}MùB…Èf†ß“±¥(qƒ{YÉeìäK®ù;-/A5;àWÃ8ãåd58 ræv•Õd1­æâq1OüëÎ‚q[ãlËV·|Y?ª´'Sg] íÿ¢»²Ã ¶g¼@ZVY»±Õ;à®Êx‚Úæ#Ãî~¼©#¢{Û˜›5
ãÎ}q–íŠ€„á<K…?°!Q@ˆ‘íQõpfmdb÷‰ÎW*ú;âÄã€‘©+066 }óìâZ^žj‰WœÌfÕ'ö½wæH:Í°ÏŒ*ê>œuåf…–Y¼Ü ©õ©%ZèôSâÏÑ¾Ç¦å¬É—ô“B,;]õz^¬¬š<<í{¾ßË˜µÔûÕ|ÉsB!œûX~(_I%Køõþþ‹:c\D¾>üÅÔÕ¿ÿ}÷ãh%áØTÍDjKå—ÕÄÍ0˜^ÊªÝã4%@mÑYYeÙÂQ·¨PYudl,òþÑH¦¢`EOÞ‚I²÷•<r!ñVÜßrë¤opY‘qÍàÂìÞ=ªx1Ï!Ldx³3Ó˜º­€€"Éð7="×+ŒOŽ~ÞÛÿ0>;9Ú?³5R¯ðwfÖNNÑª•˜¢¿Y!Ì £ßr{Qbo6³zõ*eaŸÉû÷iîÎ:·/ž‹mM<œ1ƒ¬À=£·ð.ºzÎZdÛ›PL†kïÂàiÁÔn)/Ø;×i‡™I‚R×^‚/¡ˆæ¶Cæ£b¸k—aÑ¿8Œ‹þN30úgÍŸñð
G—{3Ø07‡ŸÙõZ›jA~ÇT±™)Áð_ã‰h 	IŽž
eüo¹$ægß×ñYœQx¡¨ >àÞ§$ÀL9ËÀTjp¯O¶ŒGO‹øfåK°h“KÏ‘ôŸüí]SËŠFnÚ×PÏdu§Ãœ¹­ß ¦/È(	|™N ár˜Zfiƒå„0+ w:ÖÄZcÞAðP»BWY –,ÿEDÿzZÌ.c×nZºL©ž3õ8ª$ö2.ap(ïB¸•ÿ3W{3CõB÷fËÇ3§)÷&xÿ¢«øK¤t£d¯Ž7Pg8t \uzwC^¦y­OèÙØ@`øéŠéÛP½P>˜-5×bÂØxVÎK™UH_ tm@èS#ˆ¬ÂÒ’R8Pù¨ 
È ´Ir—oÌœ“Óï‡òË'°ŽÐ0±˜~¦¤„žTUk­+•©õý§eÐ™£Q…ÿfkU­;¬ç”%¨ùÄoUÌ«…¾R¬PR•îþ×»£·ãïßïŒÏŽÞ¾{–k =“ø-Ú’cÃ¥÷F•ËIç_ïv»˜¸†b˜¤—w•ræøäðÍÑû7ã“ÃŸÞä@I¨Ë>FànŠFT¡8;¢9ºPZ¿†ú¶k¡+H¾eµZ²³[®çãOÕz6s»ÄMôi<ÐÐ®rULÇ”o9vUzq^Ã$€…ÛyÐËLSŽÀpAãr¡Úèé„Ùq™:Ðâô#½‚Ihœ÷^Ÿî0¢¶wrv´t¼÷–¼«øM©MbÒ`ªŸ@´÷*ØFjkGï3%Ñž‡ØØç-¦³BäFc´ËµøÙ* ,nž:ø=£y
/¬~âp~ Õbf˜œ}ô„5óCòÈª|5³¤<ê”¶¥@V­`ý bu‘Æ­d”wªJWærJ›sh/â7ÔëYóî)98¼™	©ßJŽ¡ S¦öÎ¬‘Csë)ü­8ÿ¹,>Qóp¢}FZ€è/}7:f8ï@ñtÍ`9u_V¯ðC1[²ÃÁ•
‹Á¸·S-‹…ŠÅÀFØ[.uHÌˆvÃ#bl;_ÕYÝŒÉT¢ÿ¼GÄ#OXævÈ¸ìlè¡•Í®§äšÓÐVWýaV®ˆ ³l"ìß]/ŠÏKnÿ8º;º¯;bÿ¼Ë€²ŸÔî'ä¶±¨éK –Ný7‘ŽŠ;ùÜë$”®MÜIy‰'ÂOB¡0“nN´Éì¨‹ÜÃMl¦ØÂzWkÜØ<âÇ´°þj“˜:dÖ"˜®îÆ¿‘d×Ù;)ôQ(B’õBzÇ9ÃpãW#d"¸Ù³¶Ýøt”¬8Üj†›ÇÓñr…lT_q	™Lê¿•Í5ØX°] jš âê¿ž'£ÔŸJÝD?Do{Š¬åÞÃIˆ ¾6€P«&8-¥$XL4D‰ÐÁ^jÛe`úÚWt… bÊ!V›¨ÃoØèiÊ÷êñdÕ”år²h^­ªyÒVã'ÜƒmÏµ0ñÄì[¢/Tú5t´,Ú‚¶,´ÙÞ&2)ÿwQa>®
pÄé9îNt“2sÛô4ü§ëbqP-¬™qZRtórÍ©Ì/ÚŽªú´¯Ø1_c|‹ˆÎTÄî˜=ßøŒJÐ$Ý
WsUÍfüx³ïÜ.‚?œËç¢{I«.=)ÇÂ*xÿÃuàã‰êpçBp=09´¦É—@m#vSŽåŒU³›Ý…§uÔ¦Y[›Y3Ã¿ÎÄòäv_É%àçÜ—ðïxlS-béÓa‚Ü¶Ä#y ézç|üñª¼ºn¼·a8çÀþ{L«“eñIkw‚"£ßŽ'ä™3¤LÚ7ê6XÍPôAhxZµ»*/Û·‘.ë­•‹cTZ´*æç˜}->´kY.:ãpÉˆs›FB¢ãžômšÁI¸*Æ<ŠMt°6^MÚÎŠqaâkØÑŽ§bŠ_¾’Tû¸Áœ©ÖÝxpH¯YŽ„NâxlnI#J»*e=Gƒm¹åmß³-µ—~7nÇ™·ñµˆkFî£ ½úWˆíó“¹³q:-ÉøR}mÂto ‡è~é—[ë	­ÙG÷ÅT)ŠŸï!øk‘ Hƒ‰ZÐM¬ÅÚxÖr›ëí¢|+ºÆ?0ò—–EþâXÝ„
i€IöC“MÊg›Œ"z£ßå%\þ½ä%Xg{“UóùzQ67h8šû_Èª@ïì?UÄPC‰°¤EE ®C¿Aã}KÀ"ˆ‰…x5óö’\ÉLºžÜ^q4­ßeÒßeÒßeÒßeRTúÊ¤ÿzBiÌYXÕJmÁàýI¦Çú‰´PÚˆµ‰ú;-¹óË¾ÃÈ£‚öÂøge3+ÈXáü‚çè6Ò6ðWê™/{Dô^Gg°Iu>Ãl¹7ëP0ÊÔ]>š¥ãÐ&?1'š&µqº†&@mà¿yä&ÏgX…fN‰®ÙÔv˜Åm§h½”7W‰/Eö>NšÉŠRS©i¨u|]5Õ¨šMù?¬×z°Î•"Kä·ŸËi¡ãœ–ÿWŒŠyõ¿å›ÉêXšL«5P…üWðã>+ÙŽm&ó¥¦ ü·ãIs­º¶ÁÂzeTÏö _ò×óòJüÖ‘þ y”Ø²‰•BWT_fºz®ñë|}ÄêG8ÑÈÅ_ZCƒûkü~	÷V!a^[Qþû^—ÐPS‡ÂlRWi1ØÆG ¤:ýãùèaòî÷Ð•‡ï9o›Ï£TMÞ·ˆã0æÂ<±ù³G{’š}Æ¶È›Žç¢ís¼Á²ÁûS<÷,XäèŽqcz³ò_#™O7Ã`˜Ry€:2K‰…m’õà4‚s£kvã4úLç¶³ýd!ë’‰xmŠÎD5…MÚgÇvÙ½%<=´»ÈèÙèmÐkÄ]{Ï5Ãa½G\ÊH}®½7:z•Ð +ð»¸žjºÁDT»y¨nø/Éˆrý©^Ã’ÈÂp\6Lî:n`_¹+ç+½/Ü¸6ÅÜAAˆóýœSgs:»ç¹Kèþ¬ªÖÕÀ¹þ+Æà„§Þ=|JYíJõ¿žì)~:2ÞM£¿
Æù;¢§ÐpÜü„O¹Ti$ÇËˆËGSÜËËF3½ ìœU{M3¹¸£E“iè^ëÕEn‡B*JtÊúÛaÌÏd^œU[
äH})°±ëéÇt«z¤Õl=/àÎ¿?º;7NóÛŒ	k3ùÓ>Ý?Ý½›‚1hÞ?qL…VƒÍÙ™3[U{/ßcXYÎ&¯p´Øgh-¶Ä´·%lÛªµV¥Z	–H.©  ÑUˆEY4'‹I¹ÏÕŸÈláPxtMÖ/ÚæR˜ÞÄ&g]¹ÅµØ?àG¢0$!Kk-6.=âöèî|ùänÜÏni7Š ºxúà;Q;ªÝƒhPüÓžÌû"NƒßõP>±}«ru|PT½¼,:;ñ½Dvp’½Ÿ÷ÎöNØúB²*@vÇKíª3±ü=LvÈÅª¸*ë¦˜¯ëë-×FNºÆˆJœfóÊ+ž'ZÀ1=½ž°2ÏÒ’}ÌÎ¬X\5Ð'±v}¡<ú€Ãê øX^Ø*ø`-J(nØ„!«^ ¢@ƒqs³,¨Vgì»Ý†
`´ófï¿Ç{ûûïÞ¿=óÿŠy¦·h”ÿ|Ž;Ã·e(BÀŽ“­^áÙM/e½?+ù¯Mù‘íói8% ¤j®‹Õx]NE¨¹µŽè	@<‚nÚÑ™AßÚÿé)¢ˆ½½dºZÍ_WWrVÚÝâbñ‰$ƒîÑø‹³¥2Y7×cv`ßÑ	€ÑÏnC[Ä‡ú» #Íõr«WhzË$º¨é_S†T€nwÈmhâ]®›õªÄÏJ¸·†ëŸ5¨e4¶Ã_ó•á_s@Œ‘tJ‹˜ZUµ…Œ ,­ô_¦ }Ç«âRáFv/‰Šl…ýÑNš×*ó+Î³	1^ôÄµçRÙ^ŒªsýGa@º^Ù÷wªu­N–Kž•È€c€lºì!J±§eªô¬¤Ú–×“õââšè²¹	íb¿‘L»µE0ÿ …<LÈ*›N©›ÐÙ&d$†KÕ`ôà¡ç`w,²}§¶¹¡¸'aBÐ#XXs(¶
RŒ >vÙÍß‚8¤âÃØ›vGmçZh*h“J±œñÇ¢‚½ó !ÃŒ„n[õÝn"Æä¢±Ò…µÛûÅ‡Eõi±·TDX£„àuo,Ùò­ÿ„ÿ‚1`s,ÙZ~«Í —©Ë ŸàŒæOVûÇ?ô°¬±ØW÷ÒñXGR"ûØdK"0a6 x'<<.1;÷b›Mê~8‘UOï/Ð NÜ°óýYÅ„Ùb6YÖãê&³¦´=Ãä<PUáÊ9ú4…ësæóü¦)þþèé·¿Ø'ˆØÿò¤þP‡Î´ª!bð3Ò²[Ž|–î<AÍ—Í°ðÇ7ýÓ¢Ï×ÏÜèÜ›ðk]\°=@	€¦ú wSOÙoP¹Û/­uxô¬ç“bJX$cD¸/×ålúó„q?¯ß}:>|»÷òõáAèŽPq§[wY·#>Š„´Ñk¡yæk¡nJøî±¨n¿Y÷Ä ™}£¢0Îg r°ŒÝ‘D
Ç›¡œ…°Ï·ùÿsñø;ÞL°\\‚4‡<$Ú?ÄÁg!Š.ÁâØ³Ë’ŒÉ["/Ï2¿ºAç·/–é´a'ÕSÂq%ëÿš{YÍ¼%p35Œ¢DMB‰áÖÙT6ldBÈ1$IÃx-gfÇŠµ~Î“Œ^]4¯ÙíÀÚþ<™­˜ß&o×ÁqS›öË†
Hk~Züjš×dûXóŸðè¿’£S—Þã¨Ôe¿)ü´.Xÿ“mKE¡
INhÙ±=(//!CQ½EÖ¹rro›îëÊÁ{¢žYàDEJ/&^È©Roæ‹jÊe§'»AÏ§-®8‡ü¬8OÀ…  |Þ¶H»*’5y©Éßö«ù9ÄÂ£©•6ebÛUq&t`~³úWNŒÖZÍ¬Ö†¡Z¼-8`ÆC
aO@Y£Îþ6)y‡§œXB7ÞâÊe¢¯cnÉšìëQN_?Q}7ÇÂUQÙØ±×ÈER;ìDúbHj‡H_?Y}ý‡Ób7xdJ8çŒ-à]ÒgF¤´o…@hPóídIuýk02nî!Œ]Ù<§‘h6ÎÄéí•ÈáYb ×&=œ+WwLÆ=ÇÔè~CTnàÑ³ç™7G~ <™®AÒ1 µŠ}l½<ÙÈnuÂZbï§Íaï'
{¶ýíW@aaO–‚q’†³6¥\à°ªçôÂ	ÌC‹E–^¨9ó®Œ11ÿSÆŸ&ô ŽŠÊnß6JÀ*{·œ2âge‹,!ÃÝ…/k{‚&!£7Xþ}Ðž
	g§®V×öÈXÍÈ<âÂ'>^Tó%`ñi1Møv$¢ëL7Ç°-(õ±Âq01’—k„äm=ÒXÓ‘=Ìä#¹DûH"„¡¾7¾§åbã‰JœÙõo?‡â˜í@»æèžFž{".hSu#¿ö@¹$Än•ôÆ²ª{JÈ¬+`äfm2ZY#ôŽ/žSk/ôìeÕ•Ç2f™<x\gº¤€$ô˜göšõ9Ió||;ý2amt®[uŒ,½™4×;“ózK¨ÿw¤š¼)g³²¥	Ò=ØÆŸîîß Ûk¡ Íàï^ŽŽÞŽöØ{ûöðµÐªÉså °s;ýôþðý!ôƒyô‰A8šW«‚T~¾bA˜†bpF—É!_aäæ¿Æ¢f}L‹(kPô›×E1j‡ë(Å£bt¯£>ZÝ›ªPSˆ6ò§IE«O¾ E"®Æ¶ØêºBùûíAµÃÂ{Pì½ã“wû‡‡l¾ûñ®ÇDØT‹=5š•^Œ©í_—£¡XÚS7PÑÀ)¡o“‚û£{˜&µÐQó²´˜WcÚB'EÍá§[1‡°pƒó<Ñâ‡;ÙN]æÖ¡d³Ë”ËIô°¨’õ”@noŠ~gìæœµÖs×ÁáÚ÷ÿSfÿ¾»¥³ Å†À©ÛRAîž~³Í ´ dPro^Þ¥{ûBi)¨™Sêm¡™íÅÃ»M3ò
²ö,&‚ÍìÁ¶sÄF¡Ìcß¡P/.‚'hÍ°‡ÁJæ‰Û(f¹/–bn»á4Œ¥#!ëôpSÉ…$Ì|SJ(ÁLæ1¼÷?ÛÚ›žz»%'xä8'lƒdþ•^IÍ=WH¿ªM>¤y§ŠJ‚“¾ìË¸æðƒz¨œÔŽ<ÓÌ;JXåV±µÅ³VØƒBˆÒiñùÝåÅêi,;_´2ÏÌ®@Eü•srWÎøz¶3’&«ß.WÕ\^ìÂ3ŽÉ.wÂuä¯±ë~”øAâä•éÊÛdªª
.£ã¯HÄ…ª/éðì¡é‡×WöÖ¼=Ý¥	á;d‚&v‰PUH¶K€ãb1e{îŒu"{ê®øn«9ØZ£ö->úŸS‡”Y;¥IÆ©›9	 tP pafÀÈ–ÑøAvUŒ§s>Eérª/ú›B°ùÙ2d¶R»ž <:QÎ0`6ê´C=rÊÇ†?ã›ÇÑ
ˆ­IíÞ·“¦üX€aóË5\#F¡'dèžfuClkUÍÛíjë‰Ñ¡¿#yu*4Nþæ†žv>­Jî^üø)µ©ÞêbÅpÀz:«N›U1™oAkÏ’èbÒ\\¶?_K^…´5©¿ãŽ¹£À›-Ï£`d*’Öàngñ¾€µmÄÅ?)KëD’É£J1;A~'s3@Q½X‚6a¥Ò/Ç3D[pŠžFò?8ñ‰ƒ£Ç‹…]!BGËi«ÿCLûT5¸ú/;Ô{³Ù‚+ÑP„[ªo‰¦<¸:n·ŽätVÄ FüC ‘”‡y‰©¢§²'•-Uü¿ø¸sYÍ¦<œ6]üp{tp´Ç9Ò×ïöÆgÿs|8–Y;£i8'wŒ7†-gŽ[è¦Î¼¥uGQ¢¸ÕlõõŽå1æö§×Õª9®Ørx™}¸â°©=M›j‰'éô š‰&PÕ‰À<Ìø<øB½ž«g/ÅçìðSèCýârë'ƒôªB8®©p¬¤ƒ¶Q·C`FW_ðIˆ3^Lõ|ÝwÐfÇsâ ºªx²†ï©ßñŸbý	-¨½„ø7gÂWR‚æ–ìÿœÅdšT(’x§IÚL#ÐlbÎa xýyq5¹‚´Ñ15Fz†—%ªž'L
 „5<PwþÔ~I¢†-3¥ª·ÜÑ}%/B+õ;½$ßpæž÷˜½îˆ=xn¡L¦ç@‘y¥]¯ZíûÚØVð=Ë|âÎ±‰]Î–ìhBwX.‚-›­Dì&*/ (¡wl± ù½Žî«Ã”UÁ-Ã[ÌÍ›Ï§ÅIÌ oœŸªEuY­Öó\’—8¸PÜk)ëC!)’uŠQ×­éæL!óN¶3›Ø]»³jyÔ¡x}X˜	‰-üôiçÎ‘GÑ¶„Å Ç-PøÁÝdä€ÇYö“×ðt½\²Éól8NJ<¬ò1@Iù³|Ø©EpUò¿K®µøãÿ¸»»Ë>zhB•?~,*áO=Ù%¿>IwÈj=%Û~K|}¸›Ó!Ëã‡äÇ'Ÿfôøø		!…‰ÇÎèïÉŸ‰–O)°Ÿ>Êèî)	ùõiÎt¿¥ ù#ù1c¶ü3µ’zD|üsÎò’ÝýùÔÇ?åì^+ú6’qâ…PÆè»ÇÏ?—^­\ûú…ÄüÏ®:ìRª5ƒª†Ïƒ?9Ö£¾ /_ŸÉÆíŒçgå¼„ì`ýÅúÁX,ÛÃZØ!ŸÈ[Ûã«®QÃ¾¡ÇB„_ÁÑ/ÿì.Õâ¤¸(ÊÅÔÛ]Û£Ðlù:ÁRê-ä:Ýz-ZÔ!(ôºcõr©Ù×•NoI3éŒ}†¦¼ùÏ•_»ÞÞÄÊûú8È~õd¸oÛæØ|¾ö'tŽ{ìßš qH'ÐÑ7J,µV=Ž¬ÌÛ±ÃÔ*B1]…Îx“{º›”°*Hl@&î,Mdê– 'n›|áÎxÝîó²Ý!ŒˆÀm:îs+·ðÑöá·£ûÖÖêðtûèiªÒ·O¢gÈíÐºŽH›Oï¿Ïã0åL·Ã48d†
Zã;ÖÄƒÜøÀÎ.…—É®sYÎß€âÂÊ‰S¿â•¼äZÜBó}¿*&’ÿ×2.‹qÃÃÈÜcû4¶v(áOe´£Ò§²¹	ˆ‘›‰ð£¼‹¼NžÛ^($1ÿzvjPûY«aÛ oˆ×Œ0ž.º®•|fôI/¸Œ\”Ü.n]R{I™¤ë'ð”=º™´Öhñ°[ªyÈŠ†–E)ßoˆtÑöÂÚãb+ÇÌÄZfŽâ	?{Ž˜©2ÍcsÔºk¡¹ µn1“–ausÏ±>ú^q›.%±ù°û•—éI™¼Íë«÷Ú7!Cƒ®-^d,@é_‘|­«ÝMÆú„¸ÁXX.²ÝWl±ÕL m<Ï–FUÊ”Ü ’+ï©[¨/ö!B	+kUHkkUòSXå±Ç2s|Í8^ØŸµ1ÉÙ6çN\ÙAù ‡Ø¨†ÇÃÆ^¸¤¼Ro!Ø‚F8÷ˆc’ªäÂ3ò^Ú\C~œÐÖÉzOÇpßÿåÅè¢bÂÁ…y
Ç8y™¨Âfl³ª„„ß¡-ž:ª‘x—#qfÂÝzØ68šJ?°–ÛT=@¬Ÿ°»’u¤Á•´¥zð0îN¡°iM¡ŠÆ«¸úÌÈá‡AUÂ!ÄéÂ š1e›˜vM•ÞŒ¡PtÚ…'IŸ­		Í=gMGŠ›½8yÆÅÚN¦“àâ×¢8§Œ¥T›Q$>€Ó*,þØoj£n
îÁ€øK~ï½bŽKH%«¿¼9å	¶G–mKuàã.û¸ûZ€Ôëþ€÷ÑF Ÿ%þk{Ÿ2úv³½ÃE½.- a7{ç ªAbEÌ]Š_b¡•D‰zåÙ[6â÷Lêkn-¢6Î»¤Tq%Ã_Ê³&ô2/^¨|u‘È-8û „Ugýìz=?×>ž
uÅ»%0AüÆ†9ÓüD®:›*'§çèªË˜©0íc‹~^}æÎÎ¬i¦<’?æ¿Œç“ÏœVÃd€%”«¦ÛüÛ’·Æ …ø%¡§8a`¾™|Þ’Wx¡&÷´êiA“Ee²Ñ^­›ÞÅO]ouÜó6Ly¨œyÌ£éÚ¨Gàƒûi§,J(K²×wJ¤Ä%FŽeDÎ"Ã¸€=Y¹ˆÝ!¸ÄïMUT¦dÈ€6B¤ýL…¢Á÷àäßwfÅ%7sÒ Ù’þjíÃïð	?SUµäž÷RGÃðû\C½¡ó.±÷EkÎŠ)GLd­×òDXWÃ¨:ÿ_y!Xßœm5ûm£`Ûvô>´Ø~[w ì¶æ©è¸å6ûSŽùîÓ‚]‘àO:®/®‹ézÞþlE“÷|'äÝðªàûë@XW™Ðf~((yE›«JðŸ¬@jñ°Ý¥[x´„U44}¨ˆq;0ªh¹`
")ë2ß™ ¶XÇŒ5HF…BÝ}î (|{}àYutWíU>ÉÐNû(/fÝw[ùŠàçuòZÈP­ÎÜÖÈ Í;M«BTÉÛ	}eEÉ0¼- %CÈÙOu†VÇ¹…Ë?]k%û¨’µïÛÂ¤ŠâmñI¯6KiX%#ÍiÑÐ€Tþjh\^ÉHþØÏêÓ‚g6ÀÓcß÷ÖM5•¿½§å{Âz{€’ L]Ÿ4p±‚(q
°åõ¸=Š)ÇS¯S,Ž4ÒŸ>LLÅa,qwá&jÁ©µ†â¬ª@£k	ÑÑ†’ »KÏ_DrVjN­IË=$å/"%-!'ð’Œ»rD1‰¸µ4¬d<1ïqköN{‰(˜ü&ÅÛ^ôœÜ–ÕÇbÅÊy¤"Å~’«0=]Á]äxSlÑ16»1è·ÉÑÃ{Š1
jî#˜yÇÁ_Â–m™1š¡D¬èÍ5}WàÉålrUþD#ÍyUô]‡±Å[O,pâ7àÃ²	:`|ˆLh'ÕÍÇ£fÔÊšÐð­ÃSçÐ;ÔàJîbm?4ÚÕÉ¥Í6ªKŒŒ9t4¡@å#U©jœ¥Eu°=š‚Üš6æâdÊOmˆ¤*RÖAÑu‹.\:l
.=C¨ØpEÁ"	Îzï¿Ý¤ºõ³n†+ID^Éˆ5H$_\ääa™ø¯)>7iÜ¶e¡ÛëZË?œÃ.ŽèþúÉ\µbÃ.é5#á`”õ<Ì	Ûywe4¨ññÉÑÏ{g‡w¿£PÛKßöhqYíO¼†£Å¥5„…ªëû—¯öÇßŸ¼{<~»÷¥a¦A˜ßŸžŒ_Üã£·*øÖ—™J~ê€L%hT>ÑØÁx€~ÉË,7xºSÛn'	?¦<8?ü®rñ¯_á“Ž±ÅÚZwžÊ²,S,ûë…ÜÈ(íûdçdà£ü”RYX#nÜÀ®òydéQË|p¯iVrçlç—°×‹àC—®+ÊT„¤‡ÿØ?üÊëÿêº¦[c¼*W5w@ôçÿ«”NþáyPC/{ËåL?iàSÖÒm¤˜¾[)vT+_•¡lƒ|Vã¦j&³±ò©°$“ÛÁC¿ë/¾Lioƒk2<³^ˆÿ|7^\{œî~A®CØwþOàí0£™_õ?ÿ4Åêÿ»Q)´Ú"]<l?uDuÀ™¨9Ò=¸á£†ö@ÊLX®aÊïˆv8rûÌQ²Osµë=Å‹žùŠÍžÛç§ã×›Ü@?Ý€ÍTiik©ŸQyœ$|¥’Åˆ»ˆN‚®Š|M,ûAà[¬—ÁHûš
–¿x§îmI@È:×™ Ç2/¦%ëjÌ“Ò~¨	üøkÏÃwˆw
›è…Ð;té´·§”®ÞvPÚyÜ	œ·™H¶;™H{—:(z	ì bŽc¿=š mÈ¡JŸàAÛ¶{:·©Yèàeh'7(Ý DÝ Ø‰¤ð 9 fîÓr¹Å–öG(cû¾ú¸•7ÛN‰‘ƒåÅe¡ŠÃ ñ€2ÀËQº ¶ŽD•´•Kº1)Ø‰­ Æ|W%³S¥×1€’<
éYÆ<Öè¯¿MÞ±­Û¨l“í:ÊqzŽÛ„ÍZßÜÔô;9ªBÔY
iùi kméö|}ŽûÏµzÒv··ç+”Þ¯PzxÀBIQœ$ÓÉÊ†­{yÅòÉàe ïX(›õ•#ü¦¼d7óf<e¡dp;›ð]ïï=%ás
Yúçbq±ºY²Ýþ›pNÕTä\P‘sä¿FÌD‘•ó¶žl‡ª/eÀW¸žÇÆ…î<s§¹Y¾å /FÓÂRÞ‚§ì³ˆA¨i‹¬©èŠkYÃ§vö~ífËïíL™VÈë~@É$Yå7èö|1+uUÔìßâ!OJŸ¨F«ãõ»{è°î¡Ö0ÆÑlUæúŒfOúŽh,;cGPwñ]ÛÚjkáV§¡µqpsÀ-VÞ;aþsŠ¯å5º[Ø64\þgûâi¹É¸Rø¼òlLrŠjU,g7ãùdõa½t_ˆùo?7çÕd5ý¡œ³
ç¤v¿}Ò®“¼Ú®f×Mò¦¼Zú°êï¹ÜrÍ<ø„-r¬þ”3îÚS27UU—k?LØå1Y>þi[¹‡¼q7‰ùä³ñŽ”¶ö?ŠÑw?–v(Qî)Ê3Sa\CˆÚŸÙ]Â8àò±•›
ÂÝ¶ïvÑáøj„€èïu(M[ö›N¡x;_`är´h±y¼1Až-Ï-Æ¦Í’÷=ãÃœo|üRÁŠpÉôàïŠ¡ëF.&ƒ~ÄÌ
Ï½}þåâ3´	²ÐuUoMÈ‚Þú ¿ûË+ïô;­­mþU½ë;+À	··7sÃ!<uâoàJ=˜4­€Ë’Ô2½ªVzÅØÀÆaXå¡ks”ôÔ†{ÅÀ%GÄ€Íg‘J—¯0—œà!ºn,¿'Ù K½žCŒ	u¨¤\Òµåpq¡—åß#ÊErùt;gJ”m}&ËLËpÌMWâ5¤)Ø/¿vhS»ž¦×©ˆƒÖ§[í azõÈéô'¿Ó¸­*”Ÿì4dÎWên¨Vá¹olÔmø¸Õýg$§e’ŽÏ%ñ žp¥…9±*ù»3`R<äV¥-š‡Ü·´ésŠ„uYl=ÿ¡WSªü…ðŒÆéÇwWbr›ô×`x'ØNÇ“Õd^G]å°ÅWr=Ì°yîŸJ|Ïx.N™Ss¼ 2}¡QÍýöRÌª„‚6„â;ÜšÀñ­ÌIŸvõ½V´ £ýÈ±¥rÈNØGyÙÂ»P>î)… %S[i‘¶PËL¥Pæd|*DeÑ¡ìëšªM"
7¾Æ…âW©ÛæŽ¤vÙt¤ªr­¬ÎôzGi[ß¬›bŠÕ]R~}¼_Ì×Z³\ß¿Â´\ì^r«#V3¹çýñÁÞÙáøÍÞéã“Ã½ƒñÁÑ£Kã7‡§§{ß:ëºTðr‘Í´	,ÊÈ¨.fÜ&Pô"RÐíÌŠÅUs4/‚¥ Úýýü+å} Ž4$§ûêèõÙáÉøÕë½ïÇ‡ÿ½ÿúýÁ!GGÜœ³®VÚÇ/" )mC W0¼„u@R†ú\ÑibÍ{r=6_ìÊÁ;XÙw`ÿÕ¼9ÑGÈFLá.~×Ù[Ô„ùù‰…ycŠK.U_Aæ8–ÛîL áXhÿí`ÏùÙÅ™óóŽD üÇG®üÑiã(ˆBYT–"™T¡ägS…BgT…2eU}>Ü…E“È¦ªúÂ	PÿœSïÛ'©‚¬ü¨Prs¤B	çIå=y_úçKx”XÞT{uæÃåkFèñ<û	îè´ hê†ÀðôPƒnU‚gV×Öp+p+Jïá>–þ˜$ŸCSå+ûNÃˆt%ãTL–ú-‡W+Ó*¢ß?3*ÒŒî’GÛ¿>Šÿ<m©áÈ¶À½=yLAxÃŠ“"ôûwFšPtH*½åþpÏ&få½Ï¦¨Ï°,JTMñ=µEvè_=¸H± b*c8o}¡)[Å,‡›)ëè!<,R–¹mAÖl7úƒ@Éá»v}ñþƒ¿d2ú4K¶Ô{Bó8a2ïImé…È’óÂãgÊ}A‘Ï-9" [Ú»Á¦¿dÜ9AL¹my@vñGHíðµåX(›‘P³`mGXÝ±*ªÕ´X—ìØé;n…Ëj6k»mÏåÈæ¹_Œx	¾u©1£×4	¤VKÀ+a+ØÝ+åñ´\¬°^("!¸W«È•xô	M¼Ó2™#=5ê
w¢fHÁ@ œfø˜˜Ÿ´Añ’ã?ÝÔå,ÞÚiñoƒÌD”þ™DpÃ¬Â+¾!*öôð©×?H+PêQbq÷.WÕ¼RV™1ŠÛ*ÉyÛ	ÀFà¦p6©bf¸šøòv=,AžNf'â„âjXµÒ‚Ö‡„ãÂûAv‹àšsAµ­©.a†PSOo¿šÏ×‹’]'‹uÔºWŸa~ˆ«R1~$ÐfçÁG;Š’ßÄãñÉåd›ïÐà‘85»ïéxÜŽÆ$pLRÜè†´YÌ[Ê«ã¨ŒEb{Üe¢,[%Ît×âÒî’ñ;’L:jº|=­c‹é×žŒîÔÿ )Ä”]•ÚÃo3*ntªªYÃà“ucºÍRw&ÓFÜ‘-TzŽú,OÁVîÙýõWê‘ÇXO—uËÿû—à¨®®qOCÇ©Lë÷zêöRï˜Ê„6yå:/˜¨Â×xÎ” 5´X
øý‹VªÑî¸t=!±ECeÓ¤/ïî@@Û%fÀÍ'Ÿóäóé”ŒÇ6çðË¹¬òä’sä[Å;KõfÒ\ï°o[SÃþn[•¢ôÏåœÁ|pÇf&IÑÃ.u"»Â¤\ -Â¾â]cï¼ÑƒÑCÙOžBÔÅ!PÉ	¸Sñvú¸ÐÖ
:Ñ?5ŸM¾B÷z@Fê¶i<K¬Éhª««Y!2¶1­‡S—âÖøK(I…0˜©—f(-^›íòêöê,fÃ	}ÀdÈrƒœ{ö[´€;ó=JìMZá1ñ.¥ÝÛ4”ðû4”èõ·íÞ¨U˜	~’[7ýVm7yÉx~ß†+·{Ü†ÒæJü‘›÷H~æ±[@sSÞ@ŠMôñJ»”Q<‚‹¹y_Â,5¾z"‚­–kSÑŽIÂf"/ï¶‚[¼ìÚºxE=ô°®v—ËAG{«ÿr1->¿»ü±¸1—6d[Ü¥žóËÚj,WÂ—}òiß\Í$1'@¾Ùç¿¸•Û½-ê—ýH´H@¾Ì\Q&ƒ@¥Ø£HòaØ³‘ê+ù¹ÓÐ`Œ^
©áH¤qvóŠÞåODÉ`MµLepe§þ‚ª"›JËkJL›bGKlÔ€ˆ4PL,jF‘jä9Ã )Çüxz`’.Æ71é²ëžH w„š¬CWj”›:ÀI rdñÄ½à÷‹Qn† ';€H@Â!ŽË÷ÂøüçE{"yrï„É8œHFç‘…I’ûð_jq´¿ë„UïÍNÜðÄÈ)ã‹t º€1…±"€Î²ê€Œ¦#&æ"$Ø”IÀ¹q•;#†üj¹™[î˜à7þòcÁh£ìÑLl"¸` m,ÖƒøALÜ¾^FP2ªŒjŸ^Ig° TùWV¯Ù˜ô'*RÌ‹à¢³¬à›o†„paDA ƒ˜<ôÅáçR&E‹Môª”õ—áì¨<CÐ—g³uyÞ¯°Vì„§t¾yS^ïùoTž®š„4DÒ§Fß¶€³uz‹²QÊ¼ÈFuô€ê;)3ELãoä_û2ð'­Q³ÏØèÔ¿:õ³IÝ(icLˆK†Î-Î‰"¡iTé>2Q"Ÿ›„c­4áÚysôvüóÞë÷‡áÁ è_
æ­¤Ý¿_æØŠ:wi}åÆ,+3ü³9jËŽ–ýi…u‹ý&r:¾PÉu(7ø#;LƒÝÇ­œOYðÂ_Àœ9›XÍ*ý@IkÜX ê¡ØŽ/š:­‚~ª¡vÛ°¸ø­Ïå'ýþÇ»ßf¨µ«òÊðDääMØÕ[6{Xrú¦®œ¿?¥Tî-¯GÐ.Ré(ªG,y L–ËÙlb…Æ;Þîõ–Ë¿„ü\%RâU‘ˆ"ª*3Ž5U•6,Hpùíì‚ãâçh›?ýëèáè»Ð6ìaé J7a{KaUºò
ŒÞÆ²¸3³ÏB,zÎÅá5Á	Ù= òBöt-oˆè¯à”kJß%Ì3Ç-ºƒ«1|I¥Ï„ƒ†1ÎóWÔ:×˜*x)nãÍTÛ¸¿Tr¯$¨Š×·eâ%rÇGäùØ¦øi÷¨™æy‘Ð=´ö(Áe`u„*ô…ÒJ7¡
±E·ÞŽŒ&ÞŸŠD<Ëe€*Æ¥E2aÍtOÂP°Ev»,Õ@ ÊÅ]Fû/·´ójÁ%ûZS¬[&ð	m£œMæu%¥Fô-0HéG5Äöx¹¾dËŒŠÅ;Ç—=Ss¡Jü5À#W£œ¸¥Ç°Ôø–‚xÎj“©Xkë—7# Åa7ðtà¬k‹Yš¡¤åÈY9›IÝŠà ÊÅYa;†„ÌˆQ|Û=êü ­+ÛÓ4=á=Òx¥‡î‡æ¡sû‘ä™íŸ36½z¹½±W9ÀË¦A‰ Æ¹Œ_×`¿¦ü!Tâë«¸ÄÀßÐ¦Dð·LØy‘œB2U3”@øëjü—7‘6QãEÂY!¢zÖkfËpóµü¼¹YÝÒ;a é5üKfì6(_*~,yzÖvPö4ª„qíÛâÓ–Åén*¹Pf…ihýêÅ~p¬ëÆh¸q$þéªJ³S^ì–`ô¿ªr¡ˆ¥HúÈ8ìÕU^-ªUñÚyA$]¡¹Ë³hïøEˆ:ÆQëŽíóŒ¼€ahÛ ‚§Ý±ê³íu´ÐƒÝÑG4˜P?ýqkg?ÌQØ˜ÔsÆ~S-ªWÕj=§±ýžüCH'\^`³#ÊQÖÂÄVÂˆXÏ•“ˆè!b5þ°w:~uòîÍøèÀo7«.&‚ŸCé¸Fd2O“‰Å¶ìÐH)7%Çè¬‹)5´|4âËc?z‘‚Ó,d@ßÅ–9+çöþéãq%2g€|¨Mt¬¥V™PC&j\µýÃ­,¾\žP¿² ±¢ÎƒP% ×£~”‰éÍi’,îM§!\¡šâîJ¢
u’AütÝrß#«å½P2*wÚP›¡Alq¯u†3”CuH†I^iêã‚[ae+ÚõSux/‹%°y ˆ½óÉM>²I¶Ã½WUÃÃäŠR¼çÉë˜Áéz>:HÎÀ«‹Î¬­„ïþ^i£¶ãëe_tª}#rñBj=˜®žiXï«ä^-ª[°ƒ#IâøÂMQºu)A<Jf^W&œ(BW‰ÌéÔy¥Ô\3“2³æ™Š'È(¶\W)“pKŠ)ƒ”AR»“6XfG‡ßxëŒ9S
6hÛF­F­ï#Gm6ì>sP t\°ÓÍ®Ë%Ûì\2^ò tµ#êYÏõ#) ˜í1º
+C Ó‡ÿz$êÖ!ÇGˆr¨¾î)êe¡ï>™‹¼‹!”ßqRÒƒNvNÌ‘I>ž†:r'!‰k·€ý:Z|,l“âÚXøxÊá{ˆlaíž+aI?Ð§ ø,¥Š‚Ô¨'Ù“÷_MÏÂRÑ8Íì~,Õ›$÷Š‘‹àö®$'«¦¼(—Fz_Ã@U×ÓP‰]ÆÇÞ¢êK4¨ã–ß’AÌƒÑŸ±ü‡ ÛA1R7ä§¨’<¡‡x§Æ‰Èµh€Óbvu±¤›@f‡ËcÿÞ|¼Qƒü(ý¯&W«j½„‰‘Ši­{Ì ˆ
iŸUzXÐµ53hcb@¥œ		D6Óy(+¢<Ú>räÐÀúYT‡-»šœóOYŠŽÑw§ˆG,Æ¶<z2úÃèÛ]ñ œ!Qp=©!{£àPô#îo1€b—®J.ª^T·RE¡;1§|…S¤9R>¹ ”â&
¹î¦Šô×FQ|÷aŽV5hQ•TháÂ™‡ï|dðÄóâþjP
×Fú†ŒðõýUk¸$ÍEZÚÿbúòF]Þ­eÑùQF™¸´Qâ’¯#Äe }!.P©öûaÂÀ™,¹0£"èÑžò~› ´½½Û¡`÷šû	æ9¹CqÝÙ_Â×½†<Â§•¬¸ôT¸"[Z÷ŠK@KUÅÎÒ.MïKc6ìá(íH¾2M¦¹Jë-[£ké•¹V(tü[<ývÓe{Æ¨©K#ÈHX¹)œ0!Ñ]	Œá»œä5»3Ü”ä*=V<î€1‘¡Ú@íï€þu-zŽ>…÷„øíQÖê{àe¿ò¯ÊÀ¬í²ÂWãìfYH±S|«ä†v_m‰Xæ5Y‘Íaæ)«É	äC’Ëôƒ!§nÕøv™V«›#p‘c=ô´íèw6ËáÀzÊnö·âüX ©À‡Ó²i‹jáhº|8…½Šš^ d.Žjœ¾swŒvE"T“h?ri›A¤ ÓebuíÆùÈƒEòî¶Þ²³mÚ,Ÿ Á6²ÚcWkêPHµ^^ØDæ¸©£1öÌek+£¥Ï¸·Ú@D4(œµŠh<¦º¡hXÜ ÊÙ«Ù €È³ÜÝdè]ÂoÏŽ›D¶$›ÌµÈm98$ùoOr¼ÛÈÝIÕz°]å2tmw–ß~ÈÝuVU Pé°»tËAÀAüXX¬fƒ Ò†K A
t0 pÝ¡Ê'É×¶`uöAst»ù‘¢ñVr=}AÛ ï3 Ö†ã€ú30Ôœ¸¡wÏ°<Q_À6ÂõêðGClóqICì¶òJ}˜EéÎ`T†q`v% Ô`LËOêgò5z¡ÛÎo®PñkoÎM˜MJp;I„}e…9$*6Â%…Í.n#Ü´@øäxÝ˜<§±‚uØl<ü\ŸZ‘²ùæà{U­>MVÓ š6 å€²ú€Põ½·£Ýl Þ½“r¯û]î¥p€´`c@>{¨6ÍhL“;‰NvÛ!!œw8«–å…™°ù·¾ÎM
æØa6w¦5/k›œ2O¶‰‰x[ ×Lê/9´âe}±®ë²ZôÙ7^/ÚÛfœ^œ‡ßÍ¨ØˆÝ,ËN(n<üiÜœ7 þ€ûâçc¯îqs’½d@k°¿\—³éÏ“U½óúÝ÷§ãÃ·{/_P&W&SàÝfuSB’å
DÑzñaQ}Z(ód±v¡wG÷d!£"4é”õÍ7£ùºnFçÅhµ^ŒÀªqdlÓÞ¥é!É¸'›´’—ÊL‡¥±ZBW¿6è÷†Ñàú.Šbú}¡D”ú ¼¼ôL÷T‡²®¬ã8ŠáJ'ÅEQ~,¦|ôpUñ)9×µ_ËlÉšt7’­¯«•;Ã5;(â¹TÒ¶Ëi3+ÇÞ!WÝZÞ$ZÖšêÿ¥1¥êÇéÅ;xÉY‚Aœg¦“lY¹„¿®e¦îüUOL¹k|§¿H+]e¾a´´àÝ`Ü‹VD”:•G*íK/Né(ªYõe˜nÝ•4@yèÔ0yƒ-N(˜ùbÖ&×/_&°âžûéÈ¡<ÆÈ0¯Ó›Å k@äª²Îu¿H—/>‡É1èÅtàÕ'îšAÇF;çòÓtÌ‰aÄV–ª¯÷µq•¿Pð6t®¶sÄÞWv•aèkÆL,y7`Ü{Ê‰‹WŽöò©WþšZU,?]îÄ…¹£#k£ûK¯Ž54¹> O|}’0A‘Î™
¶Û¹HÁüGíV(’„îë,O"+ÞZ›\ú²jòh(øçWÑT‚êŽt)ži¹c§–õžGÛ¢ÃBÁ”ìqp!üE°ñæ/@ Ýó’‰‹«ƒI3	¢¹#»u,—Ã'àM#oq}£ÈÔ\øœ¨&jš²‰^$gÂ!·p„µHáãF)Œ‹¦Í™Qt<›[ÂF>[1i_n1¼½uCžç
Å¢¢a4å·èõÐ6Ýë!4¼â?ÏýSy_eKê¼Ìño ¢ðŠ!°p×-/ý6Ñ“þ-‹ø³æë&Ú¼w¤“dÌ*ÜÌló5Æ«â  YiPŸH>h—-7à’N«æ#,ûçÅxê©%)þ¤»J†{òé:lÕºQÄÄñj¡7¡?ïŸ—%ÄpyÆ(B×@–æ‚Ñ”Ç$SQ-få¢8#Š‹Çq™V}¨âè’÷4w6›€ÆVQ=†.Bè;tì^Oêæ¸©†€Ù[÷´"°6(°ÑÇÜH ‰ ©à%RÝì„Hœy…£ÛcuqÔŸrgÑ1Ú‡•	Áø‰ïœÌ#áAÖ©î“¨ŠÄ#I¡Êµ5É‹
1k“p£ÇÂwGr*9{Ù•’_EgQ~i‡ÓY­ãŒ"Í £>ÅiI]Î
iK5âàz2-Ø_ñêsÁŸUú[ª©Ç´6Ÿã(¶•àdS#+må_âÍ¸þø¹uÆ ñÝ43ãU(½æüïôŠ
µ*–³ÈmºUŸâ›fÆÎÑª¬ðLÌÇŒ…Ü‡"k»ƒZFˆÑÁ†yÚÔùä3Šu»ø¯¡¬£ï¹iéi¸ùvÕp\ZòN}äžmp6Ä¼ˆ8›Ü jòyÍyÛ§B¢§'‚’è˜†ÛŒN±ô¡3y†Ø›PâSQPûÏa=ôÐ‰”‡*”ÙJPÉ°¼ é¡ô77Š™Nºw”¢;aŸ‡ÐãLø—J|É.žjÑLc—½Q%|]9 y’A¥¬qxæ`<)ª#Q?…(Â*ë1Y•IV ¦alµòËU¹–Ü`O¶–#P‡ÉU<º#„ŸÁÑ‰œgÝm‰¸cŽ†M1æ6§ÍŠ§un-hCéð§#ðPú†¼ÐñêíÑûãƒ½³Ãñ›½ÓÇïOOÆÇ'GoÏ:'4‚âbl³ZÙxt¡!&œaˆÏªkd?ú dlx—ÿ}Ç{åÖìøÎ[>´È¿‘Í¼@î@,ü²~Çø´ßR<…ü¿ãvZVàúÜ¨w-t„w²ôí,UAvJ îäÆ‹SPx
3%Í^±¿ã¶JÙzÍ»ÈÌ‘Î¿I§û›Ð|¶Þž2¬hhÆ^`[¶Óf²jþ6)¹
úòä‚öšý®µ×§7uSÌw.Œ¦úM9›•5ÖW»½Ü%øÃ§»Ñ¼„ˆõ"¡È#èDÛQä.Ô¾Û–‚¶ãDÁêõW8âw;!§Ë‰›&6˜ìz[§T÷a BÏ)­L,šŸChß<ÛJïLƒžátÉE˜ƒ>3/ë/Fór¡lÉ´Ø.ÜNÉ×[ÿßnz«ÍÉS#^<ž~üÁV|fBÒ¾ŸI#žl–¿š–è…U}ÌyÈÄ(‚ä”ûƒZ%¨¢­ÔÉ7C(/ºeWêÐUâmÙ6o´ÖNgÂ±{.‚"rzvÎ :k„|§Y<Áˆ±jìD{¦E†]Ö¶c:JYbd9v$]²}¸”)¸Q‘wô×/›)mc>Óð¾±“ìˆ®ó2w¿}¡·óâbÂ$ð[§¶.†Ë ØçÔäÞ­ª$Ì&Âƒ}ó+j·cÖú&÷7œ!'å;µˆ¾ÒÖ1xG(Í§½%r³—ußPCRtW\
oòN»J:‡vÛ]¥—Í?ÿðÍ@dù‚B|^6¬è¬·mÊÉjã°¢­°©ÙáŽûé±™ì6äY–ý^nÕµaGÂ–$;)¹|²–Ýî³žü%Æ˜Æl#jäµ[62Ç˜Ì±^ç@óÿ  ÿÿì=ksÛ8’ßïW(ùpåÜ8š$³µ{5³ñ”âGâ¿b;Éí'-Ñ6×’¨!)'¾Êü÷CãE Ä£AÒÄamíÄÑh4F£ÍæÂÇÇ’Ë.I«™˜%û’ÕHtÍ*¢h¢ev_9¦×W¶@Œð•@¯­)iÌö*m‚@œöŠ±Z}E!'ó'ÙékŠ<FÐzù(„¢€1ôÛŒ3ÒO¯ùÐí%ÒE:Ï¯Óà¼Âsöüy@.cÜ™(¤N2Þ«–ÅºÜ+òµñ£¿=±Än6Ù£–þöÍÀÜy·úBC‹„àï¢VíËîÀ2ýó^4gÆÜ‘iÍÈŒÀìÙÇñ‚où0‹tæˆZ[ÍÂÚêìt`yÀ–bÅ.cñôk9Æ‘Wàß›$ÂZ”ñèy,Ì„ÍBÌ… Ñ’)ÖÀ¤	»:ýò[V§ßcÔé‡¯ÿi(ÁïñJ°W}iÕ__ÂOÞ›êúgŒêú(UÏ÷õ(6äç?Ùæ‹ñu¼ºzkšjÐ¿Á©U½o¥UÙ’Âø…íw¡mý‰Ñ¶Þ‡´­»S¤Þ÷¢H½ïI‘zßƒ"õþ‡"¥*Rï¿kEÊ»6n’,éƒŠŒð(,¯²åaW¸S+/ZO¹‘ÿQñ‹yŒ_É”=_h}z®¹ùGô²{ŸÐx¤8aæí‰ÓÏaÖ Åƒñ}˜ V’2®jÈã	êÛA.ð€Cß–KŽS—ç*¹ªÆ`¸›ñ T&ñ„…‡ÿ´tPYžßÚ3Â®+lÚÞ2óÍžž~\F(Ïƒ¿Œx¥¤þØ¥Â¹‡?..¾çÓã“ZBcÎÊfÙ×]†Ää^ï3Ý`xl÷Â×Áq¿¡¸Ž¼Žö‡€'¼ŠñYó¤âýâÄãH·Š"k`_ëÄO,€ëx¾8—®TöôOdô‚†Ç£KD›Åã«>Ï×l§>P*”!#¢mðôyµÙ4ÔÅ¨Ü$i¢­õmûH9žõïÞÄ2HYŒµ¾¢½–ÞaØCšxÄ)±awÙTZ2:ãrÑ§ÕÎ#ús˜{z°õDzžN\b¤“wÆ¹àVïûD¯û`¬1ª=CvI£šÙõ¢ –£Ö‹ Œ8~ÛT±j¹YÂcÁ+²«^é-¡Æ™ßh1›ãUå¶U•¶§	Ì,öë^†(YÕ@f¬ìÉ«Ük¥uãê9‘¹Cíæ	šÚÎ›ÿ ¶/Ç™Õå#›ØÎ©–€ä:]9§5[F†©¹F®õâ±üÂÕÓIúg}õôRMbW¦ŽKØZCWŸ
[s|,ÒòŠÊCÿšŽñß6Ë¶ÙÓ°GÍvZïÿBW]<0âP|…ú=L@†ï¶x‹ ÑÕ,”nãëFN•2Xt`²—!ÛIäž–²xœŸMPë2ôð³¶v>Ÿléúf	³™!°îÛAÎ©Fß”Ÿ(2¥ýé‰@éœ.°hœNÛ>ÔàôšE~–aÙý¬“¡ëŽ
 íÊayV)ïÇ¤»>1üÃ‚¬œâ‡Pº*tL;ÿÈÎš¶6uW¹>]£iœ4ÚÕ»¶Ývˆ±EF€t²pÇÜr
Çþ‡ò$,rþ]:[ª‰^ŽÒÅ”ÈÕíÅD¦aQ@ÒS¡µZ<ž˜8ý Œ£æ¿«ôÆeØ ?ÜYl§a%ªÕ{¦%§ÙÚ£Þthƒvš³@àƒ2B çÈkãYÐÊ =A•
ÒXÞ³Íç/ÞpF½øS7¢¿4fk¸#š=›™G!ù§ê½XÐÍÓ–£ÔøÒ¤
i8œ'_Æz½rt·ÀßLQhÍß^’%vœ’²¬ÖH/<wÒo
°ÀR¯7ªçµHïÔsþ¯“ÓÑé‡“gVô2xñ()’yéu­µÉudŒÿ»úŒø°žQ>.®ˆ.!u>9l	;A²\ÎnvòÙ”þ8Ï¯©öÚF‚ö-Tµ-7Èò‚?x;ujaÚµ]FA^ÃÒpk«'@/ßhI„ª¯p¢äÁ_©×Òš,ø·Uø[‡6ë mÍšëÇ©vT2 4ý·âþÂQ{åô„‘«ÿ\b5Pþiïò…¡Ô”gc%mY.T–ÜH‘¬u­î…¦Â°€c¸±,*’O3tMtèØ÷DÃ@‰Ÿò†ë\éPY˜>¥ŒH¾wõÇÛ/i½[¿¦öéÁjN7ß—5Rvœì/“rTL.AðÓ\ÞÁä2\ñÏ?‡Lú_õÚköâRî+”¬CÉBú¢Ñ8U25²âÕ˜pIÁ‘Ñ…+^kµ}2«—«H«´4>(!7µ‰ÑLæï,×5’ÑßÕ:¸oUUe:‹™F:UÍw–[ÅÐ ©ÿ}ý*{õåê¨SdªX«Gï»æY¨èmJåÿÜÌ/i-glçä¿˜Oum;@´¼¬ ±^¨‹X~f–*¾ ÝæM^m&³Y:}c/Hd¦›r%‹ÞÉcšÙð J²§+‚Ì>ãÔOéÙÍ!ý™ý£ÙÎ™ãz¹*/½9sU‹±òôØZ£íåe:O‹Dg‡·¡|/Ô…Ô|'œH¿¿Q½m¥³”f¡á°ƒûA¾Ã2ã”§ùÅÅA²_ƒ¶,
ÍógTM(<‚£ñ&‚ÐŽ°Ï`é4{$š½Ø¿	êôÏaŽ€:ë­ê"Û«|Îñ>N—3"ï6$HþÂQ`1˜'ÅÕ¨¤å8v¡¦iIíìMÖ/Únr/¤ÝÒ’¶°“ÀÇÞ’Ükm¶˜vú"n‰8Á5:w"½½˜7K¦ Ä¯Ñ)[’h®óÀx¿Ê&W0Ý7 ÉeµÕj.`'©—7L€tS"»R‘¯–'Ë4¹"uäËæË¼¨âÚLÈ¶Y¼#ç…›ž6µˆ‹«l’-“EU2iwqžŸæò¢ÕÂPÍûI¶`j‹·mÄI5ŽäoA`úA•”W>HL v¹IUîZdšÂ/sw/OŠBŽ^®ægr÷¡5!ˆº3†
><Í—Ùä°ÄÉZ4¼¢¨;„K+Xvù
l)Ó~°’ t¤$(W¹ž†Â¿EŽ«¿!›)ç ½™My7uÙ·ï5»jø<9Ì¢'œ¥¥)+H[Ãpåj“è…Ë†I qËä7ý·Ër:éHºw& è)–€j( „á" D7˜M6@@uÜôß*½,ˆ9?¶<;Fœ‘gÆ¿žÙÇµ€ÒÜ˜ËDú>ÆºòÓ²eÓ®8©ùä%ØLr>ùÅGªE¬%e*7Á•b
1#FÝ·ªpý£€ñ ”»VnLpi¨a0"$eÉ2Ua–RCºé¯Ü¼²Q0	æûù{	ÿÅ4Ó“ùÆ¶>1NHîul¤¶Û'[Œ×eT§1– ê^=9Œ•›ç¾	„C·	)ï¶½š¹žQ}ŠéìRC!²-˜Ä9š~Î®cÉê2tñ%XÔÒojÎc^ñÔæÔ!ÊžZ¾¯Ë›"ä%¦î]¸"]ø’çÙ[ãì¨}],äàY2¹ÒïàÍ_‡ùBš Ø¥¬,ÀæBÉÒ…«æyÇ¦þao)rQT”9-×,TIì‘–Ô,Û†öùmÊ~cR©+²ÌÒÌëêÓ^ÝÔÓ¡X¾%í±ÇMè|[âVµó¸åNŒDm†I³r†³·pêÖ³¿X†%xd}ŒPi¬zÒ¸Ý	Õ+	L§ú)6l¦¿¸¼"Zîj1eç¡~’ÂãJ±_èã`¸œeE¶Â¿(›¥¨©,tqkc›æõ‚GºHÌfùçýlÈ¾¡g(?vöe(£xµ·ÿîÜÚuÝJ›(J–—Á‚÷âQÈ…Ë%ƒŽYN‚y—ÄÓ$¦8D,Uvxò¢£ì´³É6°ûÍ eÿyÝ\Ôì0x>ø“©CPˆƒóiUô‹}V;N†„ ¾zÆA?‹ K¤¼€G9È`6ÂëÎaõ’[)ƒ„HÜÂZ×ÛÉöŠLcˆÚGðp4¸ªC œ…ïÐ³nÆ€3«\…Á[ÃNY‰­±S0´+¿æŽ¹äV,Š#ªžVÜdÜ}Úv¹ËS4h<v®ÂÔÂíöaX¶ƒlRç´–(©ÿ¨Ê6Êëaúe™iIã4á7'ãra…÷µßšý3g~AåÈ³lÕu¯“É´ŒCt·±2,&Í¯¯-®ªý
Q›òë=2?ßŽÁnA÷f·ž° åcœYïUì%¾DC¸P.9Ë½ú‹è¤€Ëã1\m¯–¦Ï5ýíôæ,OŠé»lêÔ8U€ç³ä¢„YüûßGæÑ³Ãf÷V>Õ‰h~é=”9­'tR/9á®N½è,•bõ†š£ÊðB8îú*ÜÇB;³B3ÆU®mXÑ‹á(Ö!§5HIË#}VÂ›FÓË%¦¢]$k…ìNï_[²ÌÇÕ¿ZüHmˆè2‚  ®àq¸‡RO‚Uä?Èÿ–Âz¤œœßýêùd^^8‚m“$@ÕœQ(sä³exn}ÙÂUø¯¾É*”YÊê-ô¸ÍìXDªç¾Ì>’!Ø,ª1Ã«`ù_¬›²7
±Fô¿?ýd3Úí„E
.ÐB
Ö-+O•dÔ{(rÉïTn\Î(š/
™bõ»‹t‘ ´(Ÿx¤³1?"brÌ6èr±7|f·>¥¥œ¡¾‰°5vÞ½°×x ×î5&£ËÛGI;m0UBÚ´x„i.+Ù"Ød+¶#ö/IÔ"±£¬©ƒüìß|´Úû5îo2šP£¢”ëµ»^›ë×MŒ/¸Þ‡’XÒ²†ŸãVÜ2áô.O(ÿ4zV!ÿ&1ìj¥‡Kw £(’p&'“y«@ìº÷·\çq«Ôå9'=öÀˆœ¶¾(*œý—côƒœzÒï~ÕÉüû x?ƒk„¤úë.šÃ8€0­EO.s[êÔÍ:ù²¦ŒY1ðGÄÁ63;\-¸Ÿí>èzjˆÚÀoðÔ£œòùÉ,+òjw!ïÜžÁ÷m¬ûÙ(§yK  ï3yÖ=jºÅ‡5{U¥´@ª"5©[]•©æ)Ãï—ÑnëýNv\˜aØu«›%]úºâô_GÛãÑæéîáÁøèÝáé¡—YÂ†½Íw£Óñèãètt´©X½ô_-#;>ío·<ZÅœïF¡==B"t'êƒÍÚà“B‰Ö¨ †?yï‡Ÿdlˆ-"ÌYÆ´Fß4<8žx>ØÆ¤GH€êá…¸ƒœ’ØÇ9N… lØÔ€Ý¦ÌHBßÀÏ³E2CÂp^W´®ŽuÔîÌ‚Lè>f
Qø»e‘^³—ï/Nhv€')6¬ÿ¦Êß›¡Çua¹ˆQz.ÒÍ|>_-2·$<>f2Ày©Ñ-–•Ì5Q´¦îœ—äK§ÉjSšc<o:Æ«ä»Ý!Êd_M`€¹5n“IÐ)|¼Ù;¤ùBÚ©ä5QAú?³9ùÆ¨Àö(yÜÒuEØ‹õe»žàtm¢(%¬º-ÝÛøâ¿Û,`ë±à‰•l<X±%‹è¥F°9Vóë…‹œP”†8íò
¹ÌÃÆÀ®­,Tì°»æ‚še×é‰ÓÊ°$‡‡á„Œ”´Ed°h1ÕìN¼AÜ¨wÀpN¼³Oßü?êY’´2ÃmóÔr¥XIg,ï2nâ¦Àt§ÍVñ–LpÛÆM_¥ä˜M?p…éPÔŒ¼éŽæ±–tæ2°®&dÓ:Ó—m4öÎ|Ë(”±÷`oL•AwŽÿVëMî€=ˆ7w»fžƒAFÃÛýÑ³¸-…"¶õ„Gêñn,à NÔ3—f}JõùÑlÆs5‰ó³#õiÓn
·„Ö¼ëàgao×>N~ˆÌh&²{ÎGë0jÉÀìuwËÇ™ÂÎîûmv^Vô É>ã¢u Ô±¸Jì¥”ìizz?õEÂ³Ñ¤¥=^ôºýÒ9Ï`cXÍiR‹˜]MkˆØÓŒŽBrÀÌ¶á'}37¡º3GƒæÆšLÂÒ‘üA“-»¾æçkMÉ°îO{É¿§¸yLŸíL¨Ž®Î;Àcù8Ï“/kµ†Nš[éy²šUÆ­K:A†E‘É{(îc{ŒK¥¢i³Öxüb]¡W›H“ƒ\nuVïq½±Ñ=ÀÇû±â+Žg‹ÝÚÔfáµ¦wâ[hš*éÖ~­Ãº¯#± ¦ùŠäkø2P|ÄÀ¤¨àx&t=· “\ËÖ®ªZôoH¤““G:cˆFÑCnÓÈîŠ„9) ü‰â|‰x.OÇÍ9 Ÿ.¤Ì£ø˜´îWDo¼Z;´(Àlº­dnsö¥û¼wi l©W(HÞ™<€§«&Ãv› bCÔn?¾‘îÜÁ¸FK<£Æ5@f·`FLF¯<®FïI Ãpy¬àŽ2»+ò]KùE±ÙAèðÓHìŸ'KFá^mÝ‰RÚèËkçVÚù v€´šxpK·„„«üúöÌ„lê›‰š£¸Ì’çÁo¶N‘œgK,âA{FêÛæÆf¯,·?Õ^æE5YUö-êŽÕ‚¢²Ó((Þ2‹!?‚7[f¤XÜÊk´Æ,<K—êº³Û»œ9Ø†/OîöŽwL4ÒW÷çv£Úbä¨ŽX¶.¤Ýº·½J‰¸ÜÞ¥uˆO„<€\éFí¬—£6õçÝ†säíïÓ½É¯<\„C¸UP(Ÿöºã6!r6¼ò*ÖÍ73¸Fã† ÿµ•p¤;½YfdÕÇ\a‚/hû¦`š®[;‹Š²ä2ƒËßg‘„i+º¯Ø~eÇÚ“ºJsî¸œxÛø9øGÆ°·Eù5¨]ù<1ÆL£r&–Ôìí¬n–Í	UµžŸ;ax{oÙn¹¡`­oeÖÆí‹Šô5çßû\È/×_CW†Œn" ‚_±ŒÃ¡&›yÙ+††¾”Àñ4†GájOÜ‚§–éï¤Pl¾0Q*E2çucÖu»|Å)¥cs'ù–ixß	ÉÈ*†ý,«Ø-~HS4äXiÚR:¾ð$‰øŽ—ês[os­:´4zZ|îÔÇ€ü4<„KÝ³ßÿàæò4ýRmÉy5òN%jßäudáucÝá!S) P¬˜8’Y7¯+Ah¾Á¸çXM%Œ"Ôq6¡ß½\cê‘lÂã‚óÓŒúÍ6ŠÓ!AY
Ú¡ïNé <|ÁÑðîGÍèÈ|bÛ³ˆ‚ôR»ƒð‹‚táŸ)¾÷<éPÄ#4á¼áîB†£øçZÀDæÅŸÛkâÃƒ]5c,8’ÀÞ_ÍÚ+æîmC \·A%Á
áZ®>«(?sõ±â*Š•LYÙ½ÍÕÇO¤wCgŒöI»‰ÐÌ:Äíkjï#¨M†H«þmp™	4[*?ðko*Hž%BÀ—ú¿‹7²ÔÏ¿ÒWãeï&ü†Xˆª¿Œ¸ù8ªç%1…ólJ¼ÁrÅAºêè¹³\…Î-4',ð@—E:Ï¯St¯ð•ÅqEÄsFXà
÷y8wø>åaºù¾åô‘¬‹Ÿ!ºkxôeÉ‘ð¯LõéJ:ONmL)eø1b¼–JÈÚ–¶G§œT)8ìN¢<"iŽx¢“çˆGp ÖpÝ©sp#É¬Ò/ìz‘ÇžJ@M\­ˆøD™ýÛË(%^Â¬†³¤¤`Ÿ«vÃ¯Ú•È^—‰TR¾Ô/£s^Æð”e6p¤$n+(à¡wšt1äÒ˜Œ,%A†úfõðÖÃÚÊæ0+¸¶e#Ñ>zFD’+ +èb¸ª{¡x`NïÔ“ó=þ—·‘ŸÄ,ìŽQÖš…Óz'^-dv»ýíý7ÛÇGSyÕÊÓû73[9û6.?&v$â^$*	âÀì æ'%´6/¬)¡ Þ/û¸ÎW±fæoYó€ìÕAI„ü®¬ÌÕÁ¡ø‘8x+Ò§d6[&K{¹•GBà#z7Ð‰™¶÷ÿg÷±³’ÕrkEC{^ÑGB;ã{%ÙtûË$]º¬2„$ÀNG—yeÍ%äû>ìNÁÁvÔ\x­!rt,dß¥Ø½Ýá?Ô£œÆq°ªW¶h2Æ…Ú‹´$o##Æ¾{®‹`¸0¯}löH¦•dÑvs:zwxðxv§†qj\ùw,_“Aó[/þ~Ð·"CƒÛåzï8â@L)ÏBKwŠ|~’×ä×:Ô®Ù‹5¤´­²~’V`ÄÁž¥Õ&~!¥9Ñ4ý“ªÜ[§µOÛq·¦ì­rØ¶f¤?27twMCr9‚×k´<„u+df‹iúåð\®ö#©"  	®ÏQuÃÕ~ù	ißÙæÿD…+‹<ØÆ‹¤jÁÓ¥ëíŒwž~LSI˜¦üÛGeZÛ@¿ÃíÚ”èégLÚy­"äÆ`šêKÇ~B^§`4¯˜?Ùù—ü»5kFW+6j^Ww|Uk"£–VmüŠø"+xBÝ%;¡ë÷ËUü:5Õdž\¥¼TP&˜›¼xŠû~ýÎÊ¥¬ÚHq8©4+ÚeE[US+èë
¶²«}‹Bp*q£ŠÁÁ¬Be)
çjÑ¦„”ß×Cãå{.O¸D<ßMœî×‹R€Â^äŽ»ÂµôŽ¬]ùÎHlÑkÍ©öÝKµ¶ÞEK† 8Š‚ ™th£HÜ>¤cá˜Ñ»s™Œô„êäÕÊó)´kz¼<É_ÄÀokÇT}£^Äo—NP}º>T8OÓJ[9µÝ/?ñ~Tíü§$­ºµBåsÓ9ð“bUÂH=85Ž¬nKþ†¿Q×	Àû!¡ý†‘uBîÜ­È<—Q[lìÎ¯¶Eîýzw˜šÌL,à“<*M(7¶Éíh¦æK²%gtL$Ý€“ò*¸ýòÚÙDhcºnZÍBÃ•l4ÅrU³a”ÏíÈÇOÖ;¾ø„¹¦U.ÒòU¶¸Î*=ýšh;àfÐ;YŽ—vddm£()º»mb¾TéÙñ¶&‡Kk•†Fé9|õ¬[‚èMÜ÷˜´ƒTv8«Øí†šÓ`À' (ü)åÞÓÛ›Y>¹²Wäö·`…×>„îØ$ühå×’ä÷«t•ò¸÷ãÕb‘œÍR+Ü¡þZ8c™2Â¡Hqï>º¨_Ÿ¹©ª6aŸAoò2åô²Vm%ù©¶‰¤âKAïÔnXjBwZ-{]ópj~`¬?“ £ÇU6cGÊŠT+JY¦k<¿—ó´ö§¯z^ËîÒ³Iª¹--ýû†µ«J'n¯òåjIs-sÍ‰›tÕ,ÌÚ/ÃYº¸¨.ÉÚÛp3ñ$ÀX¤éôä2ÿ<"lP­^­˜Ê¿¡V¼5®ßJ™Ie–\”ƒÿ¼zæÔ6ëƒ¾¸/íâÚé‹á:mÕA«!c"¦YR§<QßA¦Ohú—×àéÀ ?€xçòÓšÕžÿáZIN«¿gtŸ•¯Ù¼wÛ''£·Ûã½ÑÛñ»ÑÉxçøp¼»åqkTƒ4Òtù„"î•ÎùrkÀpHÆ˜mB+Ò³ÁÚÉMY¥ó¡r¶ßÏf³¢Ì~¼|ñÂeN³Ëb•z,ÕŽÉƒÂ³Á„f£×ƒüã£`C¾›vèšCPº&qÂd^Má(¬ÃÖÂ3FéÆX³žÅj’›²ûW'¿ïooíŽZ2C}Q§‹È © $‚l&;oâHñòÕ»F¼ZF^
¶º´Ü¼¬Gä ÓæÐÆÑðí.ëz¾üvîý#h|gŽ 6œâ¯:Â÷²mîdÛÝÇÚ1DÞÃv·¼îä³iZÐ“R1WZørtH?§³/‚Ô6>ngœÔ€(.‡=œz>¥gG×,÷×~bJ!B~fiX×%%º€¢š¡É»%Ó²åÙƒ¿êÇ	Ùã(¬7BeþAo§yÇÑ›7BÑ[v¢÷›U6›~$„î¾=oŒÞìmo¹¨¾“ÍÒ½üb¨úa~<ð¬žôRò)yS›‡y²OÜI<Y“£
¦–ÿQ’¿Ttµä¤«.Œ
ÇsBÓºs7åG'7‹IÏ®‡D¦¢Sg•	ó‘ê‘}ð}„›\¾~¥Ð†ó,˜Àj"o —¨nÝžPý äš*j^ÄC=!ÀwzEMõ·ÅŽm–'Ó‹«EþyÁ¹Œ£ã:£»q	ÚÔÕ¤ßsÊÕT³kËëjx¾÷Hl¶±—~Û˜h&l¤‡ï Áëå¼•Ÿ§ä”2I{àñ ñ;õ{Np=Ì1Š-«6×‡Ô;ñ©1£Õš5†›ôùæˆZqyp%qË*›Í>P[« ½	gû`í g»N(ùªÙš¡†î’•.äì`è×cfo³tÂMý—…§V¼žºW"(^rñ€VF),íãŠ^Ö	ýžôÏ®7•ONôg°Ýß“NÎÏé7ðæ-ð¼hn¹1$üsF†8¥Ihýq¢:ô¬Ô@=â,
7EÖÚ¡7LÁà‡wD±Êæ{-QÜfÓôVÿÅËÏÅbo³X,_'­j
m1Ý}só>KÚ¶Y…-ÊàVÑ]WÃ­×Äƒ+ŠËÙÙµ¥Ç1¯>Ê®t«ã(1Ôxã
%iÖE²g8³»?â)òÿc–~ŽÈGj¶D-½«on»áMØxÃN•oïz£ÑÕJ&}kó:E
½» vtT†IÛ®‚ßQT³¦ê¼O¬ïB‘Ÿ“bÚn‘ˆÆ1ë¤îð[]*rÔ¸ÕRþ°ŒÀëþ×ŒÄä6–Í¹à·~6f-žÇq5‚l<D;»íˆ—ÚU8+Ç	Åò÷ÁËÁ¯·C£áÓód5«Þ #L³‹Ë¨üá–ÖaªZ»¦Ÿ›øêõ±^h¾£zAz/€&FñL£­…ë}w3.hö”5Í`ÅêªO¬Ç)ûd|F¿ô£ Ïèäû®ÕÒíiVéû`”†Í=¹¬¹gì´î€±Ó"(]hí]@–Kö"äN B/„[4Ö!hZ€Çð!ˆîËÍßi 5xÐž‘eˆüÏŠ‹¬ê¯NøAñuGøÌÖ‘‚>â{ê¦>Žm•¯üY$°O€Þ¬/uPƒ79ª Ç?÷…õ	€ñ$>äf #„³MÏ0*™ƒé’þ?OR'_¯5¢ÍAûjûRß^
YñŽ¦;Ñ²G0<(vD_Z²ðD—­«ÕýF™ïœÒÍžæ¸ÅVZÂN”ùÜr¤n©Ÿ4ïq®Âzªš	¿ÊdZâúQ\¸m€s3HÙ,ð1)mxk[.Ü¹ÚÍ>|ÈBÃîZà€)Ò¯¬­! 2,ž…³³ÔDðäà•÷cg­k_;šbáï™GGáú'Ní#ëfNkÜP@ªBjHjB
Rþ£â	Y#:…$wÐ1Ò$9fTEZ­Š¦^_’¨ñ*¼Y\ÆšéËÀL´>7jô*<[^kÂûw×]/Ùì><áëoÑ•¦_û‚}néâ[§ÌÇ:’Èƒ1¨Ñh¡€óÍ2õE†ý?å6)ØIKÅîÊ1šlœëðQð"¦ÉÕü€¿H#ÐòéS?®IU%“Ë£¤ºt|lYums¦ÃñY4þI¬”iMò…ØçÉ‰&ÕÔÌF?mâ1<ß.qÞ{#¡Ñi‘,ÊYBÕÔ)(²Y7ðÄïù…E®5 ˜¶qWd"÷0°¯?Âéíë?Ñø¼‹•^˜À >§	kÌ%3hÇ`D¤eT”emR·HQ{l¥•Ž@ÅVi{¨×c;"kÛ@¥{wßÔ­•Ç®Ë†'~]kÞ'™ICjkî’6]<gù±¸{8e½¾f¹ÕôÆÌ+nBñôk¨}<—’bvO}[ä«åf2›ÕIÂ€¬áî%‹‹£dr…œ#ñ¹vj ¾yAeÑÙƒ</Ên³²ËGÄ“´§óœôI?<©
H’¶“<&{-fp*Í
ë}?íÃWR;.¦Òh…£,*ª2@»"…/N'•ˆUƒc1#'ù4í›¹¹|]'Ùò£E
Ws”Gƒ¥Ó`A%¨Øö.ƒl[7XÅÞæÎ½­i\+›Ýü gÃLs'ðD¶j^^ÐXJõå?ï$‚Ìãy¶ðäp¶bcuo@Ñ¬U‘O†ÂQnŸY?ÿi ±éÌë2]¤åjV•¶¬fåEèÛd#Ì&hW7¿a§$æ®R6_©¾%ê.%¶	Bua\7(1¼LJTtMäš+†”æÑc¨ G½~'/ÆúoƒŸ&Ç'Yðµv´JÃõ'Ö$Y°Äõ§¦(­›þŽ©€G7¸éX$püjÁÎyûSk±[Ûo>¼ï~„òˆ·Ov|v8%Êkr™’ý~	ü|úRË{#À^pF]¤Rè,îVÂG˜,e­së.ÀE0²¹eÆ]6 òú…ßhÚmÁé÷YMæòf)Î6!ã0%Ë2§É¬	¼áóï/Ü~$9$]·™Š¢NŠD9fëÀJ­ª’¿Í5óxý:8Ý„ã5Ví0›¡Ä½ÒIL±LœPnÁ,K¬•)Zæ–Ž”=Šý‡-i¨Ðî¼ÉºÏíB§'Ù-¾55ƒŽÙAù^!‰vk[…kc·
°ÛþR‘íl„‚qOk:Hµ¿¼²Ïè3¤ó>Ñ@Å_,Al¸h áOW^x.­øË5ï*ÓÑáÆ ý%[e¾„s
ùŒÝö£¶p–H¯aÁz`õåÊ‹hW¼ëžÖk×Môn‚j[?ty™ÎÓ"ñyz²Ý
×~àþÅ¾Ê­8„y*Z°Â7=ïé|™så~¸m|ŽÄf'Ãd¹œÝ¬9‡iíwr.½é‰®LJþ9Mr·O˜V#U79V@Fmià&´rÑ‰*>Íw’«”»Œ»î)[ãÔeë¢ªÂAjˆq²|„+Ê?Ò›³<)¦§ù	Q†š }Ò }!¿Ì¤pÉø·’*Ñ®U‰^ú&¯Ê!Ñ¬ ä×a~®ÙâÖë“»w˜Ç	®Ð;I!€ç'G(+ßŸ ²³Idü°D‘Ã²Èó<YÄ‰4x,J9`|¿zóšXÐùþÄ’}¨6‘c_’K‡½×Õ„FF1~bŠ8úx›vñ3²ä\)õ`ß×¾Î1!¿&¼@‚<³wäIÈ‡¤~u§÷€¢’ÞDOÒâàƒ¬—@á·i¾—]Ó”öbX„þp_NKö¨¢´¤õfÄOÛ‹é§¤š\²‚žÝÑRÒ–meådUBLfË´™ t¾´FÇ¸ëËû²«Ñš˜4ÅŒ•½²ñ9íÙ[–ºÙ¯keÐá¯ßÀFëqçªMºLäé]*)ÃjÇÂÉ‡…Ð°ŒÖëƒºé‹gêm õÑvd‰¼ÿI¿cªuM´MZhBh±±‰ë9Æål~ÿm®%µ`÷Žb–ëø»^K?ÿ¬Qò­ê¿ÀãZ`üÔÈú;©²ÉUZœ¤ÔXíÊa1M‹ È®^Cë g«HÎÅñ®˜ýüZÝiN8¸àòðmvÞ…ÖÀé;iR­ŠtÊ1ëîè2_¤àóz’]“B¶¸€ck€ä„»™ÏçÉbÚ-éŠÛ¥¶çù¿³È%ñR2ÎôBþNãœPçRq!Ü‘? SZ@­ýÈÈA‘êçé´”S8vtôX´“ÍªNRIÓUÂ©°º	˜vÎØ„ËË´´OéœÂ˜OäIì kDãÒöÓÅŠƒ.H‘æÊªªòEG^äþ®§§{ -¦DDþOž-ŽÓ?WiÙmÛ¤ËÀ#£ë4y4blRdgét´šfy/ ºBÚ¬óüQ×›Q){&€ÆPÂ±uÔ™þƒÛMo­ªË¼Èþ•[î 
êßˆZôq€DyjU7£/N*ð—¼ÜÏ§ÝÅigb1 Ý'ð¨È®“ÉMçñšIqÕ…ß!uR‘£HÙIsÝ®ªŠÎ!M°ˆÃK³<èãK³ù_–wmÍÍ³Õ7r³ƒ¨ÿ¥•+u¸$™9V •Ê@)&ª¦V;3«PÿUz£6&Žì™Sœqµ"÷€„A_8 ÔóD–"øW_@ä~ù)«.ëràåÚÆYF×Ô-¡=l©»º÷_NŠk$Wa™ýéØòP6•%TXdV2¨žLø¤ï'åÕ½Rymû©#âØU\FøÇ©o~ÓŸp²;u²$3ã_éw‹E>!ä¿1”J¢rMí®Y¶Õ‚“uÔÆ‚Hê»ßÌ‘´¼~E5BEzØH3vE`¥–Ê£äYV=ï™›råä2®fu¤+Ü]¹'±9ôåJ¾´øhXÁmzþyAƒÕ’÷DÉ§ü7àÍµgëƒ—zœ™1„9
s8l”Fê‚IAŽip¤Ù½v«tÎJ+­–ËŸžu8˜ùO¤Õç¼¸:½Y¦€œxxú¯£í1¯a~²®ù02¡øÁP*Uçýó…—˜Kb—ü§{w<$k5©¨'99…ÊN'«7™âëÆ­ái>šêý  Þ"HàÓQ ZQ»é®Ñè;ÒÈUD?Õûf^Ð
ý^Ï(ÙcßâìüÉ$QcìkJu|ÈEkv$jF[s™ø*Pv´›Ì9Ý5Ê77L;›«©5¨³oƒ®ªQÁìtêcl$›)lû|cR€üokÍ/‡ÓlÊå¢²Ý¬£º†Ç;*¡ßõ®Võ¤Lô±…xZ§1tù*Ù4ôõÁß^˜ÈüåYÉ eü<Å¤0d¡9eñ‹uê‡’…éýFBe3³ùÀƒZõ,1RsíÓAà¿Þ&få‹Ñ;f£ãâ÷®TŠk÷¥Úu‰érFôxA¦Ï"Ö¨øÅé‡ÓX•Ñ! Ö^Kbgõ/#ƒ8„æö&aã¸[~ÞÇ–v-ñûv™Y#s·Æ	æ®XÙka9@hŠTOJ¿ÛÈéžDPn¥Xœ¢ö
WÎùàù+5ÛØŽÈyU7ÃýÃ­íñ§í½ÍÃ}õ°æß©ÛÍŽmJHuŸàÛ™ÝdéóÆ±hLíÙ>øT¸d-ë¸YCVÕ3Íªj´–¼s&«gvö1ƒ-Jùi—8–Žàîm
¢Óœ6Wÿ,-Ø&;³@µ™•_Õf]´a]~åÝ¢”¡Ê}›Å“êPYºnñÉáç…Õg™wÃfÁ9}Á$[g¥ñ"$ŽðÙ4.…‘Ó›Åò–íœVQ´|šË¸ÉK©»]? ÿò˜Sµ
z"çd¾2j…5:&úOæ–.S®«K­÷u³Ïu£'‹ÌRGržƒ™*d´ßimñWAÖ1–"ðªÜ¡Pw§:jbÕ²—4ÅEÉÌÞ%¤ÌÜí­$²žÌmC«*(€©ï¢!‰Ù6€‰×!xìßZ~Î.ÿ=ð·îÐOéÙº1øÌþ!Áˆ¿Ý Ü³27¦cÞ˜¬‘¶¡áÈ6ÞcÐ±;Î0˜ô¦jÑ†{ÑOã‡z­®É¤zn°"d‹óü4g4P@{oƒ	¸o”k=Ù¸~åÆ…ÁÝ°Ýn8–ŸlðÔ<âœ4‡¸€­¾+kNbÛl¸ß39E‹Ölè¢P «½ÄL0%ßÆàÜ8O–irE„Íˆ™ûLÛo6Ð2†+›/ó¢j2ß»§…kƒ™$ù_¬ÕA.$Îi~q1KZ:>ps…þ¼ÖÀI7!Û79W|àÅ6,¯•Ûéªìf+y3n¿/§¤Ò÷qqNvÄ	wÏá·Ùötüu7šcåÎÜWÉ^jÄD:<KØœy²Æ4VR	±„Ó3êï€s2ñ‚`.œ8@'—I‘NŠ”çÙ-‡ {"eÿqU)q(.ràõxT£::yàûªúÄ¥Âf~`þzC®V$Øt¨¤»@š G›×’Áû#½a3†wòÕ|‰0Z?ˆZÇq<&ûQTþX4›iÖ±Í?ìmŸŒé%ûÞèäôd{ûÀg«cMÐ#dÅT¯3wzº»ÜîÁÇÝÓíž‡&ýöïsd›£½½“ÛØÑ«£ûtßó¨Šü<›A´E•ßëÈÞžö<6_aò»×Îáñ§ÑñÖ­0ãÑÓâžgí o2šNAÍ¢#¼Ï±¶¶¶·Æoþu+ƒü˜go¢ù»åÇÃÝÍÚÞ÷TžyjŽÞÅèÞìö-SÞdEu9M¼ÕíêøôÝÖè_=¨]ÅÛì¼*Á¥"@îsŒowwN½ìh¿mE/†=ZÑS£7i0<Ê·Žâ¦+73l¬í8x³6ö4\Z2òYÁãÖ†]§×éb•²ó…oÀä¸O GKÐ÷ÔìùV†(Hèš·¡ÿÄ§v€9ô)E\9Å>”zEUÖ¯kKÒ$E•Sk˜D…€Î‚bZƒ9L¿,³‚’ž¿|ñ"Fº¢ÚKÊêSšZ«!‘zy+Hí6¿ì€Õ«X9RÂåƒR6„†Òd˜)3Pe…ÕÆ¥°+ik3…Ï«|ëŒ3¹vßØù‹vqC’­bÇ¢™Öè=µ HÚ¬cQg|-Iyž]°ûÛM¥RiØiÙ{)KT	`ÐÃÅ,[¤;E>?¬.Ób+½&êóš•#ïDgYóò£$›n™¤KQÍÕ{˜@-gç¡³¯i»¸«[»x8òJ.ïçñ…üðx“T ëü¸Ç}pþ”ÌfËdé?‡Úì¯z7˜¹³#ÍÑJ‹[Ý†Â¢dih¡µ(­Ãª‹ÖÕ=è/÷¹Ù¥0ö±¹M¨oý°  ÄWfVR]0«£BÅ§tuâ]é3Éæpº®Ž6˜[³žûßâ»Ì ßî1}ÛuÓÕbC³ðÊeÀogÉº µ\±4<@[)ÜÞ½PÔå{že5^$óúÖPyçgwÖ,i€’¯Ú.1rµ 1ÀtzvÙy›ÑjòŸ¬@åìO?eØx5m©R
È¼vô±"ÐK@TŠ!>9M¿TàPÊpñ™ì	Åâ‘±$ž×©.uÿÌ†¿ðÿj›Te¶šO–Ÿ@ÀD6wòE˜tíº'ë™RÙ²Ñd[LÅc™AZ&Œÿ³›ÁÓerŒS¶b­f5Ø¼Œ•bÊdÙÝ00KËTÄ–+—
×Vä!'ã—P9ÍÊ//j®üÚ…CZ°Ö§uP|“
OÛ=Oh·ÑÉæ†nY{S3WEjpZÒ+¯§w‚Ñäxb~§˜•‰mvN–üŽÊ!aei2PþI‡eª à_„Ó,X^ †ë«sJ¤±}fl/iŸ,³C ¦o¥s(ÜßŸ¿ÄØ÷4!ÜªYÎ¨«¬4îp{è0iž\§üLsHZ¢!cAƒÞRj>LÉVÆ#›Xã‘–<Îo<Ò»Á,È[›ÇshúñæÔ/1$‚Ôö?AFûê“‹‡BŽ+:¨#È¤Ç–\âGRïMÜá±ÅîAè…ÔÙ{åŒÙ3Ÿ°¬9
]D2<`³Þ™Ì†Ò¼›øÖ¤ —\nÛ·òo„ôVq†¯å†wÔ‡	eò„VËÚ""Ú÷ÿ\……@ëž¸ù¿·óh:Áß/]¥T›zÍG~h(ì¥Èñj‡îN0›"´›ž­AwR_»°=ƒÿ&‚ê¹mI‡'%>ýóÛ¼pŽIÄ	ž~.º^4Ì“Œz%AÜ™H™y ­ó7"Û|ä22›ûWS³³ˆ³YLÑ´Ð‰´š†|¬2¤xZß¢œ«³]/þÞ· Ø5Û©ÂÁ·lØKA~SÍdÆW7MëŠÍs³4ro­a2léD¢bb%¥Ÿð>6:R©¨4ä¢HC0ˆ˜"_·-EØÓ¶öt›†÷ËRGEz¥ŸŸòòÐZ—íö’›|Uí.Þ¬Î¸áÇ¬Ì°ƒBüã5¶Éð¼Èç§{Ò‡U¬8?TUº €@¢»­›î#nDX1vQÝêFÌÔë{r$˜'³†¯“ÿ­ÊK:º¸×Š4%Dß+{0uÏÖCŠVùêùwÙtšê¦iÝšnñiNôÃ§þµµ/õHžö[#}¡ŒÌ:Œs¸d¿Ö¶ “3oµÙv´‡´·¤…Ç×Ñ&vô[WÝÎ8<´Î´Þåµôkÿ²éhsõÙà>š%Ìz'éÂ­ÇS¬BÄ9j”´5~«MTi½[
éþŽ ÔÇíM2ƒ/"Õ'µ©_uÒ;Á¨M,­¦ã®è6Ä¡žñÍ’¤‹¡øÇi¾ÍaYöáÜÅàÈ.Æºç£XÓ‘ñL~t?ÙâŠR'¼Œ
ßz½ö½¶gÏÇÎð‰ó®›ªÆÆ<œ‚† !}IÊÍÇz3ÏaÄÞÈ7•L		ëEaë+qH5O©X«WM?hˆ³?è*üiëô¾ˆ;­ióŽqÊÝ®H-ð¸ÓÐYß?>xz°^Àc³`L‡!£Ã‚V¤Ã?Ë×ÀÁ-xþè^_;”pñ­xkm¡¸+Ð?ý<Yç¡“óôÍ›gËšçŠ×$üÌßŸæGŠ:q
u»s5<wËÙð|›.2qì+¾ÚØ.µm±–K½º›ßnit™ƒfA›KÑ—50:%g2ôxàQ]]´dK__¶vG{‡oÇ;»{§ÛÇã½ÑÛñöÿnî}ØÚï8ÝÞ
/-‘Â¿‹×IÔƒšš	¾hï=F1#Êã¤¶B¾ìÅ^)†òîaÓH/{¿~à¾¦!L4WgÄÍxÈÌ«}N«lï"oã†íé¬PHù§êÌ¤G;3©Ø× †oÆå}‚dôM¹	§ý¥¹Dí¹æ­]¢{{¾ÏKuçºòsšºþ~ð[µïšßà©¥¥Òy9ž—¨®ÂÝxM•®Ûªd’L!;p²\ÎxŽyûµ– Ùº‹ô•4ÔÜ­
~G€pÎ Rê÷fàë'¦[@Ô‚q…Çoîh9ø§.JËËüóxÉ>Ñ…Ò¦—1¾ŠãÏÿåV‡e6#šLÌª®IÆíùO)‘NòÕbz”T—„ZOrúçSlÜNìúæ8ðp!l¯ÿõóMØßâ&LÊ`›®KmÔ•`S:òWO!{7táÀc¾ªÒñjAÎØí&ÚÖa¸+Ü,è=ÉUHæµø\dØ_‚FöÐÓ½†(«n¡ËÙÖñv–Ÿ%³5GKVÖòíñá‡£©A<]ãÖàéaw +HîŽ	N]Ç%|¼Î§Ž%_„I‚ó‡ÞC¨Iàî!£ÙìÇ¢>–„®¨{ÝBîtá|C=Çˆ¨bÜ¾*w¿ iãÉÂLI¸ñ„uó­ÛÄË³;‹@é-Ã£)Æ¼ÊwÙ•E5€|¢õM=G<-}ß%Ì…²%û…Îß#U §LÕØÉ†ó—˜bŽAA	™ÑÛR®-7¤WÿÝù¬¼ä@€’—ä+ò6à¯ƒ§'¥„)j„™˜Œ~Í2Ýâç¯_k¶ufonýäë×6²P¹Ý¶üÒ@˜àiA³	ÄüÕ†zf¨µë²Ùü 4Q–×²žnãtNxî÷0GdÛ]åxwwv£)z«ô!åTn¹«C[ôçM‘'ÓIR>Xc§éÃŽú4;œL÷yÚi ðˆLf|ìw)i•.ïLÚNäb|pÒÖk%Ú|7:8ØÞ{t¦3uðH#YÈ­®Y"’!¶G Q*’9xl‚“3ù¯Ï¡×ØÀÑªº$šçÿ%¡Ø@2Žÿ  ÿÿ Æ
Mrxœì}ýsÛ8’èï÷WÐ©º9ùEVœìÝÞÕlœ)Å–3ºõ×Xvö¶®®T´ÛÜH¤†¤âøÞÎûÛ_@|‘¢ÌVXS‹F£ÑÝh4®³EžÝ¦Å
%ãMu˜gU‘/—¨ìŽÖE¾@ey½Nâ
W'óùs$þ:CP+/Òÿ«4Ïv£›¸D´ÂîŸþ)2<¿EhY¢(½uÙ(ÍÊ*Î(¿ÌÍ¼Ï+Œ[†´™ÿk„Ïª®·í“ÖØ“ôéð>Æ-,]]±×Šè¯è 2õ@€®‘1ãM¹¼8¥ñ2¿‹úÏû£œ'é¢a’öX#
{ž&ZÔ01U´€ÿÀ¨ÀÏA(@NR}ç Ê6K'©xyÞZ#úáŠ€2€¦ÃÊì@¡ÑÝV¾6à¹®ÒeZ¥¨a wè—Ú Ñ:/«ËM–Å7KÌV»ÑÞ;Öghã(½½EÂ4	0Œ^£ý!í¡ƒðH|Ô<¿ùfÑQZžåÕ4#ô…ŽìB—!vjB0ë¯ò#•@ÿ{$½¥	ÔHøÝr$h‰*DDUÜ'_g¼ƒéºã¼¸I“eÐ2„ŸÒÅ'”„`-Ñï0Æ„XÀÿŸ~(òÍÞ‘öIWncLvOwÒ (cùc˜Ã‹HÿA£g•A ªðhÕF·Ëø®Œþ~€3ÒŸŒ?Ì_wƒ¶F¨ ½Àö5;â¶»ÎØàŽ=JËE\$(éÚs€Ì@ø€ÿÜV¼€Uf~—÷
Pé}tR‹-Z¢¶zi-ö˜ôc>½˜¡âsº@ Ýg÷q’)›iXzµä_x< Gy¦¡Ÿèø
vÂ-<ü%ì_í_(º§qù	æÈõÅÑøj2?Ïþ<?üy|eÇ{™ÇÉ1¦¦y$Â¿*6=ëÎ•ÌRÅ«UTmT
÷ò_µ[ûÉæZ*ëbàcâÞW‚.«ÀWXú“þýIþ§“ú}Iü§“öýJú'òÏ'á»ÊnxBí™0¦”lŠÔê_%~¶U©zÂ%éÝÆ›eõ–¾ä2½»¯Ê«”Šá26²–-óìŽ,'S‡p J°VAöI«;pÍ¾e(xŒ½†½]6Ý WnÐVwÚR=™£	ºù»yAG\tÇøÙ=ÍÆYRäiRÛñÅ&;Ï®§W÷Š“ÚzÇfuz›.ˆgëeu aC_þp¯Ð Y’å5&‡¤G.+ßLö.Žø°V«8KÚN2©¦{b)M„L&LÓS„åáQ\ÅŠWNÀ›f·ùà¯€ñbZS#¦‰<Åv‡µEÔ¬…±˜¡ÖbIªé¦˜ÒÄVñÍ •C‘ÑÕ_/&óééøÃ„i­½‹t©3çX+ºT¦k,„|iVEÕãÚU¢Ý+¼†:G€†Q´RV^¯À–E«üoéÖ­NNÏÿsz1>üólËÅÂ×a¨.«†•‘”Ãx
^†‘Ñ3ïâ¬~Â+·…Ö…Ë/ÑôK“Í çH9'+…ƒ—ž«|- zž’}!;‹?c5é©°T _a«Óƒg£ü ýôÚàL¸å ;òñøc-ŸF,C“˜Tå%*‘Ój+«tä6/hºZç…øn³°;næÕ¹åš¡Ôu/Z3[-¶q’(à¸ð.¿àzžÒ['Rãû¢@l_©M’´Ê‹‘Þ`¢•E.Ÿ\®Vu´ÞT'Øê¼XÆe…9ÁŒ}•®Ð0`G¬ñÙátZ!sïÙzÍ‘yê®ñmÅÔÝ–SA®êž	j#!H ê¾`¦bÊc''.‹’Ú ÊGÌAh\ämØî½ß­ÈÓvS¢"ÔòNˆP®¤»u1òiS{ýÚöÐ®&=¼ŠX¿²§ I4à¬“Äÿ“#Ê5ªéFßÕœw÷GÞ\•¯ç«òŽô¡½Œ „w	x°xo†ÂO ßè6ûÖKn‰%ø$Í”¼˜U(^V÷§yâ›Í¢²ñêZžæAH.{¬ªJPx-@lØàE¼™¯ Á>W…89Fqµ)¶ÔËVqñI4.ü@ÖÃžõ	¬GO…?°qÿèŸæŸ%Së*ÇKËµÉ Á½D›ÜJe[5!6µ6!¨.~RO@½HÁ7yœ›Æ#]´$§^ÝMËfcAK~šÅK¾ŠSqá‡HsâŽ³Eüx’–ÕÛiV¡;T¼ƒí<êñ!®c‚¤*zÀ2ÙX«hgHéÁGÌÑ=*pÀ`ÀÝ•™táß{æ}#Íû–vÑ(¶À4oÊé/1&^¯—Fš»–Õ³ÆmëmÔoÇºNÒ'ã] /J·Çˆ^ï©{íÇµ“³1´ÒÍ­|ôƒÜ²/¨¡¿M‰‹û<C°<Kïð˜¥Ùî¶Le%ð?_Yša.i×;*?/pž}p7;ˆÀÝ/\t”g
Êèä³Œ”Ø³¿ˆ¼^¦ë8k½-b„á– –fCÊÇÃ!–dßŸ5Èâ=|{•-¢xäðyŠ6k·ë\ãõÑÑ"’ÃÚ•çåÂ®œÈm»Iµb°L?#b=,[í#“FôÀšaøê&Oìñ¾¡‰eŽ‹òDÕ8HL’/jA Ý"r ÑÐÄv.>£Ú±Bºo
d4í'ÔØ`$¾¶@¬Eª'
žz âˆ†ü!F~F:¤¶
/YÄ`}ºAý°£žØÁ‡CY	£ÚKüÁSüéëõu– &sÛÈ‘­”Å®ú!Œ§O#hž¯¡Æ¨†Í¤ 7"Ÿæç´íY|¿I—ÉÇ¸(G'çfóÉÙøýÉäÈ7èÇéäw£dð6]°LJ"&‹ïÿ½ˆ^Faš(
!5ÝâßÍÝŸót}„n6ÊÉ¥¦¸d£ÁÿP¢}Dp×ŽÖŒcœ-¸ÇÑ‘Q‚«Ö$þs=ÊJæ	¼ïL"+5¢—è×*+?)8¸™tô.±©[^ù¦Š^E¯÷÷÷£·l«˜ˆ,Oã,¾Ó5Pe$1»³/$ÞÃÆQ^¤°{‘çQ¾¤ìübëcxØtN3×ž—ÎMž/QœE™¤×•˜à” ˆ&bø—¼Y/dÔ?ÌWk¬_Üùj0^¯—ì+ìX‚'ª~. ô¥Úaù"Ã)'ä°b6j¶¸wåN4‘\Åiþ¹â6^ ‹xSbˆxÉÞi–LËÙ¢@(;wýÕ[ÜŽËà‘8mÊ9-Íù
þ ñ- q•>&5Ö ‘°p-ñt\…ð%<~Þ„'Œ?ýð<áôh‰@°<2¶‹ªÚëÝ(„É:GHÁ¾ÿp59™\ü|~ö×ùlrùqz8ñŠO,ƒY…¥×´œ&Kÿ65<UñˆY \N>N.gÓó³ÑìèÏóéÙUôî R>ÌÏ&³Ñ,”³^½º:?:Ç(Z|
fæ1ØiõÈæ=©<CËÛT¬Ò²ÄDÃXz‹—¬¸óš£ËÉøhôŸÌgWã«É.¨Ðñâ[6Â£‹ÉåétFúûár|vÕn&jE–þþ8/f››rQ¤k@w@PÐÙlt8>9¡(Î§G'ŽHDù	8,âTîJ_¸{æ-F¤ZÜã¹x_ä j"ç®3¸LCƒî&Š
ç…¦Sÿî¨ƒ é¾ÄŠ!l:»Ï7Ëäçø3ºØTXîç û³Š”•aWaM€®xJïpÏ^½ra¯ùb¹š9‚Ì4th®;)Ü ámœ•¨¸Ê™†ÙúP€œÊS»ýíÔ¢ß]ÖÄ^&^zƒ‡Aª³£“>PÔ¨´Ã“h¼Xä›¬¢†ì‚ê–ìTêºõÓP~ ûˆs~ýÓ;¦­.SÞNx0•¡“°èƒ¾U÷Øˆ‹WxG@ªa„uü©`ÅáYWoNÍÞ4êñ¦Ê÷´À½JŽdÙ‘	È•L‡ýVw–ló1?æì~Tpês†Ñ—˜ßúµ>Ûør"Cà'U•ó¥i¶ÞTB–µd:Úê¨ TL9“
Õð ”y¦÷E·]/I©÷›ò1¤_’„—IÜP7 Çf=–(K˜ø`€Ãÿ¿\ã"xF¡¢Èr+\ßçµ»¤/ $£~ ¾GU¾¦¾ éËnÄ	S=àQR•;<­<:L ¤>lÚí¤$iðšQT cJæ»Í‹†¼ÙJE	^Î<:Òl(QÕv K•Oy5˜Éô] f¯ ´ŒK[hKù<ùRñàEZÎóMu—ãAy˜ë@ÁÂhquºoSoÎåè'&9leC¥M ÚâSðR”Š¶p;Îm|Öè ñ@§–¨«ÁÁeˆþáÁIÐ(°[Á`±ÕÄ9®¼Ï•¢2™ðH›;f?V [™êç¡Dòj” È°¢`û.K¸oƒö2¼}cÔoÑn74CÕy†5Ò0¿/_ÓvíéÀd µM<CƒŒäm¾€IÌ°ÎýŠN»7üq",”Á¯(‹B•°°ÉÑÖ€þjîl6ÚÄ”%#ôp?RË–©ci•LEë×s-¶¨FJ3Ð;&$PþÂY+(dNþ 4Ê¤@ÞìF”ÈÐâßhµN'äH´Éu†Ó¸øÔö”œVÝsR®ÑXpŠçÑ7=ÇúCÃq!ÀÆ¿Ý¨I
“U*!àßq¬+ŽZªƒ‡†Y -.&,Ø·–XìYÀô´ÆÙñžñ#íl§(ÐÖ÷»³3äœe	ç¿ð¿ôÔ—<èa:Æ:òÊ›÷×›{ËçÕL¹Uç³e(ÌáL”°Åó’ªBƒ:/8ªuá%lŸdNKÚõÓèQ4,8$¢‰]èbG«\g+h–Ú}ù²g³ÃÔÈÞÞÓî=³³a†Ôfd¿òh:ÆŠÐüt2›9“’Ô (ÞxÆ¶JyÌó28ÇÓ“«Éåœ$=œü×áÉõÑ„´ó¡nl^ä­ƒÝ¤šž#vrGë¶
\ÆüO›€&/Q¹YVõ	»5~7—Dü?
Z¸ÏxˆU¶õ&©ªçô’ÒHÍÆÓn§>Àð …	ÁF>¸6§ßCPñe ÕPÙ“qñd[­Ðýgï­_à”¹<ÎƒÆù­’}é=¯4{’/bO\ß¶Ó6C(_/ÑŠ‹›Çñç8]‚ƒdø4× 0±”²( z”k=KIr,z:´5ÊÌ$1ÑÞ`#D:© E°æ­¦Í‰ÊuŠ¦ÒL
=KÆ#D©¦²¾ÄK ÿnÕ#÷º­Ò:Nµ×*,ia†x?wezut	Ie¨â€ƒÞ†Ì4¡Q—6’7ÑØõúØZz?ËðeMæÄ{Ÿè“/”SûÝ&»"BÊ/ç¼×Ú~V%HiÛs|Ké?Å¥¾ä¦¾å¿æ+ÒÃþïê­Ú&("uÜò˜Ï)kV,ý¿ìê¨(,Öùå€Ÿa©&\¯áI=JÓÃ°$1ðMß/ì}Y—m…iz¹U^!f¿ô•OFÄ›ÏóY±q*OŽ	MOÆò¸ÄÖ”}y\¶’E³¹Þ•®¿ ›)z FÙÆí¦v£~TÔš§º¡Éç¿z³C	ÕÑ¯T<Úµ¤.¤WU¼¸?EÙæ}î>0ïL$©‚<M¦ÈÿÌSrk›#´HKÏíjáPÜ’ßÚtS`’|ØÄEò3Z®=êòb™—ˆÆ•pcãõzÐþè4çÍC±ÅPÜàQUi“¡ÞT•—üŽš‘é¥¿³½ç˜ˆ††‡&|GS‰ÒÚ7ŠpŸ²=NÄmåbCNCL³›üKËI`äž.žcXhš<¾ Ø£æUrJ£yÓV wõ—¹ï‚‹í’é4w*8×Ë•§ñ—K’ï·pS`)ºˆK™™¤—@K†áP[_¯V(˜|;”Ÿå,¶‘OÏ7UO3…Bê0U8
ßø\Ùo1p¿EÇç4Ïr’;«« SøGCoð9aÅÛôCûT¤í…ZÉô$(ú„Ö«WO#³úïLÏ«»àÑÎÂJƒÐbZ<§pzâyÑN€uÛÜø9%¹Ž®®N:lÑÕ•#ãKû–ÜlðvÀš¸ü½›*|ßÖ€=M8¤|õ`÷ßð?x=Ï éÉ;¿&ÁeAü$m¸GÔoûÈëŒªj‰Ù±HózçR#n]Â• ££€,RPcÊìAôÿ^ÿñÿñ¯}^¨Íüý 
må)ÃŽl)œ‚™AÏâšPë‰˜A ãºnºe¼®Ý¦tYf²ÿ÷óú1žmxÍÐ"á·VöËþOJÈ yÔeCsá³–´ötsÛ¹´“=\‘G.|;JÐ
ª}# 9»„ˆM?dmjØÞœR¨KË%kk/œ“)BåéP¬Ý	qÍ@‹>m“‹Ô:zUq­`è“ÎÛ“zWœöW¸²PÌþV™~œ§âˆUé¶I{ÌÀGÃ&ÀYÏÎi·žk@ðÑš¾	`uƒ
^MÙSÉ3˜¥àwÑî—›á–ª<óîZ®ÄcµáL/D¶„`¸}or^q¹wnÐx“¤¹mé‹H"¸Dwñâ1²cCn;E´Â‰ã€êž8…Fc¡é‚·ÎÓÈ›&I‘8–£û6‘8bëŠ¼"‰:ÒDþRÉ°æ,—œ‚S
A®8—pqöwg añÃÑ†wí–£´œ¬ÖÕã@nÊ ¯„œR p¸´¶9t›fiy¯Ð‹…sXº?Tº¢L:–Wöþcž.ŠCûíFm‡…žS´vbG%NX>Ž­–£ÏNÒ©Àgp‘Ú®ãÁ¬·«vÕ}O3»í¶ì×sœE›‘kêðÚÏCéÚÞx«×„Óþ´\}Ûl¶KØj ò|Ž*ËQ]ÕF¤h‚(Q›‹<ø°ßËçÁ€‡Hâ#—|·¿ÞÈ{½ðè7_í†¯7à*ö6Öb‹Æ}™ßBgæq9_q2Ô‡²M/ÚÊ±cªÃtðµÆCÁ…XîoÀtÿcß>=½ðF5²Í
ÕÁv·zG¨ä$ò¦6)üIÔíˆ™-Ç;nžš›"½šÒi¾>'‰dû)â¶áAT]‘ïZV7Ô;ò\Ìô,(÷hI“;ŸˆmÈO‚ÛÎ¨¤/¼W’ñ³óNIF&áï_mÙ¤‹OlS;ð°Š­1®ìÑÓ±ýY:ÅqQ¤Ÿ±IÚÖ]Ñ„àºDÀÔ^Èr	sšw;Y¿¾Ÿx-ŸUgyö¸Ê7%»¥XùÈp‚Op&‡$^-ê|í§ér™–ƒÝN-sÕÑÒ(¿þ	áKÒnºÛ#—¤¹òÂÚ-‚q¡Œ@Mwbõ­î!ÂËUš½_æ‹ONÕ‘ž“Rî|`ÿn{}#çâ	ADšÙàÆ5)íÙ|eY09äùMo&ÍÜëþÄ+hÒMU—qæ<=/òŠÀñ*öÍ†CD 3zpyIûQ"ÿØ‘Ü:ð`÷¨žB&ñ¼DW>?%€%NGéˆ_ ‡iÕFiö9^¦„»ž…/þ‚nÞùCvú›0]…MËi†Ü'9[74ùÙú<N‡.= ·é‚3»}Ã…Z·´ §©úù,'o›o ‡°*ç^V§¥Ht,âæ&·>”-g=ä,G;iÒ_›ýñl–ƒä†üKqù]ÿßÐH(H±“k…¦¢¤'šA“Õý¢Ûâ")¯ò»»%5¢lã‰dÙÉ@$b†Rôc Û6ÖÒýhÞ 0ÖàˆÇÐ™C‚gŠƒ BA;D{0XpÐ–iç4Ž óVšBeV.ºº•ñ‘ì(kREÚ­]í0d†Ml¼Pm¼9!EpV–ß²viÐ$³-Ã®ø1AY=†Ã¤2g¢Æ =tÀo3Èn÷vÐÓ†ËÔýjñýÅZø$‹|ü ÙïsrïJÞ?B«^©˜fUÇä÷‡Ñp–Ð¨Lÿ”4\ømt†ÿyù2@*Þ1H4—³>ÄnØŽ¨[wÆÓlYçxÃ28PÛô®†Mb’K7gk£’Ú’Ç¤O-‰iÔ‚˜„ŒôJR'ÄOèql' ÷ÊÖ÷ÄœapÞ€ÚæºVùªÖ«Çu­¤êÄµµ¥;ttH_ÒÁa´…*×ìÛ<$¥é6mÒviæ>3(÷èÎP&œ{ìì”ƒaÑI5\þ‚¬ÿÍš4»^Ý‘¢O!¢ ÀÎ«Ì"^o|½Õ‹"¿+pGÌã)±Oyð€n.€@^Öè!9 ÁæØÚœ²­t¬5+hº!3%	 ñ?o£?àìl÷s\ÞŸÆë·tû}‹"~<Á¤}«¨œïÞE«xm'pÎÖqQ"Rù­D_,,„‘^å	²'®$L\õÚÅRH2~;Úèôüh2Ÿþ<9º>™8r÷áâªb¾ÄÙâ%›e=öš¤cÎº,ÔÌkB“¾éÖÃñÇ6½‹?wë™\/¤WŽž¨u@&ón‚˜+7Tˆƒ¨Æ)SLÈï<‘àoü—}.ÁCÄ8ÕHT|ß8”GÛ¼¡Ý†(ù›¬Ð -W UþyŠÛÕMzñ¨©õÚEè*ŒC£Ó\æºº­v]±ÐI×Ù­'u‘w¾{Šô(G&\ÞÞ“ýÊº›sÅH4[ÈÞ­®D {½á#U”òÕ©@èO‡ªÕ#fõž?dp6$‡RÆþòosŠÅ­[H#ÒÀMÙ
Cžé
ãÌ|ƒp<‘ñÍÕýfuSœè5{=n	ÑmÝ¢ï†ŠÀx:ù×›Šó5¹iZ ·ÕýIÔ'R"p³{”$FPìÉcˆª‡‘91ò‡ä½bõñ°©ò„}ƒ„À`=ÀâèŽ„—0Ú2'Óz/8Ç•TV+yô¶§·eÓ£µöJ=†LÇ¶Dªlb-qyÈ3Z¢ì®ºwÊ%ºEÙ¬ûßñÿÈŽD[‘ÄÖ	ºÜ›«ÈlO¯{é¦ÀhWûRùÉo%–Ñ·ìF¥xðŽ×ÐàpKlê¸‚¦eÿ¥´ÓXiü–*UqT(nÕªÞ%¡¬6×iBSqæ/iu/
FfHAû2®
ÚY"d{ˆÙ£"ðÈÎÛq¾Ä«“¸éï–üd¿lfY_ˆkgâYÒde[Ï +8°ì¹7û 3i“"Ö5¨çîæ,¦¾EŸÃzÙ¤NS¨–ÜÄ;²–©hÔyNZÀm99-P}³4t†:ìõÞØÅæ`ûQMA˜v¡n‰!º-CêYa' È0±{êüÖ®\AF¥-Ÿíöö‹‚#_Ø¯¦Õà6iÙÒXÅD§j‹ŒÍÁksä|¦çêµ¢ß+$"^ÛÔ`u»ø>.¯ÕºZ=ÅJÙu$á´RG¦¹l€ëÚæqN Á[ÌõÝêdŸõ„×µÉ7áÅw:Ø °¥SOnðA
 X²/>éR›1æGý¼ñU aM€Mâ7+Á.¢8Ô?	BÀš|ã5!¬^¾L}| J)ƒ•àARÏX?¥HEË#õÒFŽòG¡63DB
¸üm¨GH&=j(Á
ôÍ VŠ°`!6²lò•Žttmñõóýã4¡â¹IžÝsA…)µ¦Çaë*hÔ›
+Ô~»&CHßð ÔÕ:Éõ‹4ã<¬ OÌã©mEr•ë™“M˜È€Ã†C&.&=(Â›èK%å£d §ggXÑœ©±/%Â—Çy!»©¨“sûð_ÆsÊ8ùŒ>B{4Ó»D¬5\JPÄKÙ¬n¾`2z@ËE¾¸ÊÇI2Êi7Þ?RŽjãÐßæø™˜TåÐÊ^[ô‰[ËO\danî¦ýû—ÉÉáùé„_e7òßl1²l~Å¡µL•mÉoìUoƒk„ÞÝ.Þ>oë-Q…’®.&'¼ž&Ù¥áêuÑ*4·„tˆƒX‚ö4ÑH†¤	w È[ÖºJïsª¬Êdzý(m"¹Nã`u¶UåF'ß4¹ˆŸ²­}ëÚ»]"Zuü†€ð_Ë`çµOQmž&‹€>C/= j4Î)4pZx !»ÃJÆÅ	+Š¶éÓëÙär~q9=»ÒÛÑ›vÅªs‰m¯X°·w6>5\“êÃïçó³‰?–[à*§B´«ì2Âi'³êTÒ¹¼[·¹µo,²=£‚ÜÈ é°,êì êðÂ6O>ÊâæmßÙÁìçˆY¤”ãD!X£F†FÌ‡&|IŒjÛî?Ñ^Ý¨_ldÂÐ¸×PÈ-ò)z,A«_å‡ñâ™ [Éõ›ìÔ­ã5q§èŽ\-=&éÁù’ôý”slŸÿÔ$M}n‡I¿›€êq¥|­}G €5‡ý`r3Æu;VÄ'Ù¢x\µëÀô©º	] u$µ×`Ê©,ðE‘Çn-‰=À#s7lßûiR^ùZ‚2N®\,QÌ³uêè›¾ÕÀŒ2Ÿ%½Â?~Ù bRår“‘SLƒÁn´÷.gI‘cY¥Ë´Â£3*6Ùyv=¥yIX!UÀ*(‰¶F#ÙƒqÍYi1pOÄ'\¸ÖGop¡kú*!i	!@ ¹jÌ‡"Cv6Æ<J"A±m'—ìÉ9>&ô41;TŒ1–Qj&ñ˜êæµá­Âš€ýQÎÄ¸á‰\Hüp`N`´šÑ	|Ç<é/ÜÚÃ<Ïï¢}Ë—·u¿|®cû¦ØÍß\Ûa#gZ*Þ¤Åž¦qgÃh³'Æmoìæo°+°BöÃ´NÈC±ƒR
áŽyx œ"Ö&´Øn]NÆGó£éøäü÷Îõµ‘ó49ÜaÞîˆ³Þ°=žÞÑÅëk;%é§+KkÀ¼ÀjZ¡ä‰uO.{ÝÜ_¹Í7ï‚9©Cçž`ñ+ÁìYþÊûÀ2¼¯,kDßq™K.Ê	ŠüÎ¡°ÜíÍŠ‘$²ÝìÛ“4pÏÓïÐ7¼ }_žá ‚Õönï6´jç9¸ºØÕà¹·8 €p·ðmeôi‰‰¦‡Q€ki×™.zB˜ô°ùÀU" î+<õªUÌ©;]ÐTd¤@©g’x*¤¶²Ï:«w,\
JQž¦t?J ‰ÎÇ«OPzþpH­…cÏ‚ñ[yf7ÁgØý¨lë^Y(:à>§hdhx£o@>ÞöÃh³ÝN½âVôFˆßÄ¾|ï;ÖR VÀ¾5ïgM}ÿÅ†iŽ¾©#=6#<lØ¢´yêÚw&ß¤H›â\ÉŠh´1
º_•ÃéÊ2â€a^PÌyø'ÜyÃxÙv‘üøõh Î Å[v¼©¤ƒâã±ï¼êNÓ®/³nk“®÷)ø÷<)¨É‘Èú4mž¶Ì§?A¿ü	OÿÖlOxœ›@ÔS©ïtùJ©b\íÍë‚US°ïÜvÖ¬ ;hU¾N²aÞ‡2a‡þ¡Xü*uÏ¤\0jwÐ;Ý»ó[K6ÁÛþt²êVäýMp¡%æ YzbÔÍýŸüe…ã=úÑqHø)µeÅg”–Ç®Ü!hýðƒœ˜’|‡P°»Ãj°¥vqWáÜ‰}-?ûØV!d—;klH‡ E¬ÎÇÑï€$Ú¿€ojH©)až†‡î)f‡€%°P’’8á¶ù
ú­ó(i ZÛ‡J»•:kÙ™àêãÛƒÈ¿O%1@1üºjænÌBM%ÍÊú¿»øRñ—rL_ÎPjãb[.–Â¯ ¤6SZ°Dê„Äš™ËˆÝý³Ñ(*«t¹¤GÖùçCH$lë|Ïè÷ŽŸu€”ó=%$(ÍN7´£?´"ŠmÄšâoi[]ë;WB*ŸšÔPX§9åÖ£+¨ûg¼àVü#¬·3dÙv"·iâ/T2ØãmŒj‡"¬-»¢ñ³†`©à¥v‡ÑÞkÏ Ù(¤òwÌ ½!„jr¤YC¤wÄÖ•\P~S¥^InVïm¨Up_e¤5¶h¬ïqÖ wf;m”½2·!ï±¨•.µ§96-h
“Ó8ÃõY"ÉÃº¤¾	Ùv-\½ÙZs¨ö[ØÒ¥ì{Å¸’&ð’L=™âŽ•æ¨™»ßÆ¿šwÍ9¿½[hÆF‡ÑyDt@­Ç!è¬Yñ¯Qƒ#ïÄ¦`Iç…oéfÚuc¼•;œöíiWÃ4Í…A[ÛÓÛ¨›üv\Õ<cå¯ú6“-oé5¾hÚ AbE¥%³Ú´ÝX—žb¶û²/:vøXáÝfö…_®§‡Prq2Ì”ÅÚºCÖË9R{
v}p¹Í7ØRˆÚÀt’¦V`ÅªâVëœ]æ)ÎGGÖÊ¦4´“É9ÑrŒL :}lœ¾Åß£ˆ £&ŠxxªE­q©A?Ó¾ŠËOmù‡Ô	gÃÕ¡Â6ç=…¯¾&—6`;Ro$%\ê…Í\ñ8/fÿ¤–›q! ?!×³ôÇkc~'™vâOÜÀ¦È$W(û¶ÞÜ,ÓEô9ÇœnL•¨ÄY•õx¾U¦°	>ñbýCJÉ( k †u£­²9¤dî†¾¨¿=ú”ºoÎñ‡ÛÂ1Yé/Ør†"…uZ‡jô¢$ÌÑX$bu\0•uv5©vôSô¢`?æ+VøEôcôbû<ÿ®ßÿ)¬%jY44§n[{ü³ÞlM2(¸¡©%ÉëÜ—çf"ÇJdÌfÏË-hjs!ƒº€­êÕ4¤çÑ“ÞRÚ“ð3TD¥ò‹e÷•ßYµ}i›HOßËÅ­år.ý˜–^®Ê¦í"¢üÉØ±[0í™`Õñz²dðb™âš{4ÆÚõi$—ïØ039+—œí•ò°ÜÊUñhèéì—“´B‡›¢Ä#´ ÿØÒgO©'~S€±MHY7Pã_¿nPñH¶žðè'ÊD#Ì.+¼´ä‹x‰F×Øàx1›œL¯8•®¬@_žŸFÿ\Fùyr9‘¾GÓ³hðÏX`ÏŽ˜¤'Üþ9Á£hWCuàjÄxGž+èð›'yE†ÓA*[­T©Ãs‚&ÅÃ}ºDÑ€’f”AîNk0Ÿ>ÝY%üú#¨Ñ›o…ÅfŽo×$¾ðµ’cÒÉ©Æ
ÍÆ‚èÁÐIÒ¯	WëoÑ"®÷Ñ€ÎQq}d<R7¾ã~ÍªxñéªˆMp‡°XèV\#©ÆWÚÖq4@á†•Mø»F2ÍôåüöÏèQÏÑ;‡xóA­^…-F\	Ú¨¼4TÇÚÍk¬ÄìYžZ'hu‰‹K¢)êHÈ!œXÁÝo(&$‰ŸmÕ¤ZV´‹­!nfâ4<&(mk›yuä=YõàámÈ|ô-6:ŽËÊ!ðÁá6>œ`1~uòýãøäz2ü4Œà¿]ûBfËg(°ˆ,;®Sf´àž"Lw`óÑ5öŠoB’±±ër²¸1d_¥²Bkç,²—?ÞsÏ<â–A§Ëô÷1•þÕ8¬Öé‡O¢´Íêwo6d¸ºÛlªóÇå/xcòHÔBÈ×”¡¶D›rÛFÆliŠÂòœ(r¿b[wË·¿¥iÔu~hÐÊ†A¼LGz	„ÈÎjüò­fúmå²„„O‹fvúŠšXXÔI3¬FÓ‚P%ÒkMhŸM µ"ä&*œ§ð…TsÜ/R…ê0Cùfü„’£’mÿÉÝQ‰2<*Xã 755K•ë¨(òÂáÏ§J'-—Û˜µ4€‰pÎœw#Þ¦]>’Ðf@0¸<I0³£aÁ¿¿^”åýÝÕèçB­žˆöF;¤4ShÛôòøp á‡ê¼{&Î[ˆT@¸ÎÜÊõç»Ë¹Ïœ±”7æÁóÚ=hžk}%âÏ$ïélò6ÓG–kJ¨8äâÚ†œ¹Qh®S4<âš´<Í³«‚›UÍ²ÁÂÜaÏæüûÇé‘OÂëåM²„)ÏÃ®`‡Ëˆà„æ¼¥èþ]‰âR"‹d5‹b¹`[YLá:D±VàëIbI&èWZ»¢&)~tšYåD˜xü.•ÏÖRÉ¸M~J—&DjÑ‘nŸ 6ñÃ8$/`‰«~ª)¯³ã?ž(º­)é”DR¹Ž‚ˆýc•CìŸïbè»’ž-ÄPmÑ×j³´Máñd³ÿLßQ­qmk½Ï…:”{Ï`˜lgHèƒÇe•˜­·òòÀ„†66hoÝþ~ŸA´ß¦_ÝgÿþÐ‰ƒ5ÒR³à´žÑ˜€"}“çÝ¯&aœd—<-wÆ@|Ty/¹Ôx¢¥¤íü¢’FàåÚÔd£¶íòS·æøOÇ}¬/_‡e
´ZèBQ]ä‡>=nkƒKý»Ûú»ÛúÛ5P~nk)˜ô»ßZRë˜êoÉo­VOŽkÑÙoÇ6SQú–l³ïŽkþü®„ñ³8®÷ÿ½zµ¥ûúY%rm_LÍçq}Nâýwÿõ?’DúÆý×ßåÐw9ôÝýí9°-–É×p`7Ì‘í<Øp­\ØÛ¨_¶U 4|Ø:*–!xõjkG"¨›÷ÑOÏãÏn#pÜ$	—D¯^}—vÀâòµ]Úþ©…KóÑïÈ«Ý†»zµ1EúvlÓãûõ¡l*ÖO7®ÂáÊ–IË2¨u½–2%¸[W²«Î0™’	9ÞÝ#ë}ÇxàÙÙÞÁ‹àÏm`WwX²Á’³çâŽe8áÖèã®#CBkÙŠi)`s RüûpC˜WkrËÑÕ)ž‰/HÇ7ÛwpïµÄK${^Æ|Õ&#4\ –~[åp¦ˆ9³—Êµqw9½)à2_øk´Bwñ]‘oÖö¨ÖDó`[óÜWÛl¹{=3cHÇ5AmÇ™°8-?,ó¼È%øô©›–;«&;Ì£Œú½ód'RÇµÿ`R¿I	, Óe8w“âÛs¸~šJ LÑyžìÑÁÍŸ&J³ÂR"
CÚÈ5$öTYîÄ@Aé³=¶³9Â.Slõa:ýäV Q£Ëôî¾*ÝŽñJS¿H*§ñr©SxÏÒé†©6Ñ¼éè]ÝFA2»7==
ç~€É$UiF§™‘!MRµÕ4L\çdæe0nA˜€·Ëø®„K ßè_éÉ%K¡¸=¼öK›Ø	õÖ ¥ôZ—²{Úyí*ÍÃ«éùÙŒ\ªiÏ(“C(Ö¿wÑæ÷|-Ù¦“¸gÙf¿¥ló[›~Ù&`xe[íäúdÛ«W[H7ZùM¾ý¤ÚÐÎ‹:‘á4»Í¡ãªœ“YÀ#…ÿD¥þÑ&Å·AéªòbcKšÓ¸ºÅ7å`öXVhu¸ÌŸFh¯K’Y}Y57Ú‹DEP|É=1ÿýŸèûø¯÷÷÷,Ì-#\Í4õ•ËMß’½¡÷=NÒdù1Ú$É¢ìû¼°tÊÒœ²`¤š}F”¼49˜þ²PP¨¨'wid?ªeT–`Ù›Þu ÑÐ§¡ñv4¸±ÌëiF«Û+¶;™pÕÍRóèó‹iab—óT= ”5—p±[š2Éß2‚j4Ha¦±×ëTlûf*ÃWÔÆœ=Y­«G{Ò/Jk’xúXKº}Û¯&	•–E@Ü@wèñC´ÿåµ3 	[¨ïá ôÃ©@@yÝÌûh*¿åî4žp ÌJB¿üåßOÆu@PÒ:àuòÔ±m´Ò$ž†æH$½}Kz„Ž°þÌè¡—µRÅpG¼e^ÐfØJfbš„XP¯NMì¾\·îe´aÜGëÛ2óG„ÈúÍˆ$ÞÉváO(ÒêOføÓú>üÝ	þ‚ÿÂŠ¿YJh©ëö@-€ÇÂZ&é3†¯~r6é³Àžc¤Ë¡Ær¬·ã#.êx'-««+5ÒŸ¤¥"2Â+îù³Òj´BñÍŽ©Vâßü‡½†=j˜ÃQ_à”‰7ÿöGéD$ëp-	 A[pðÞž¿FÍ	ò³On^0ÔÂä
½e¼A¶{ýf›±£û‚?Ø!+Ê‡Æ~Ñ9¢”ÐL3‘,mXÑa-ø…	žd¥Õ‘èDþq–ä+Ì¦­á]¥ÕÒ8Ü|hîºXš€m
‹nâ u‰ùj…•ndîrý¹5è‹û¼2ž£5|h?&f3–˜«ÖN Ðq’¤  â¥|,Jtkáý¦ªòì
}©LÐoÈ×y…?·†|g—hFÀ‹8›äk{¸ù’$TnÂ„­Áv8T]=®Cä1« $=‰óMõ|–Üëa°îP†
¸â~³º)Þ@5Ðùå”ªLx€b¨ÓÍiÄùMµ-‚9&îõ§éß wèº–zOìóuÿî¦ÓeuÍ2ü7‹s]ª†WC³‚>&&êö¡HÉ	îUÃ¡ ‡QäýY0eg¼âöÑd¦@à¢ž^HÆ%¨wå@æ’²~?"å‘’>ž¤Ÿ8ü;•rjƒBµ^P_`9—`—ª„‡Õ™Êz¥—%”+Czi¾>ò»Ä=—´«Í«@tëVË»¨
ó—&úÉBJ¸~´þ 9U¨ÜÑI`ó¾ã‘±KªyÇFåšÒZ ³T¿rÓyH‹Ö·2Ëj¼0ËãOÂ|Ñ .©möA«£ì}¦Ãêö8Âìk–‰'OÁü&7s³TGß2Ù]Â¡ÂýêdxG´{æ›–ˆÐƒ9^ÈÊiS¡2=Ü¾­÷Ø¹f‹º…ÍÅð;un¯´=³:F'ü7§‚É|íV«úÒŒê›:ÑP]‰ŸHC¢—$µ²o†–X&a+„œUI©×§cXkü±NÁ/ãZÅ×)R\Z­ù»Q‚ncL"Cæ±õf?Ö5[5ÓÚ+äÈâ4hK/³G&·ð2¯ØÇ'Ž<åžÜ…tIncd8PÓè`2bÍ¯.qOBöx¼„Ò»ÌQGd’Ê
ôÕ‘ø{ =ýKìN>ô•=àCÞx(ùâ¨îg7RRrø=‚ôû›Òòz„¾¬SXe,‡œZ)+ºö*Ó÷#ÌýµBgg¸‚¨ääQÛ>¼¥@H;| ößBnðñP¸øAFL'ƒHßc¥ €ÿ&ÈÀrqÞf‡²dõ…è³3ô †¬l`wÈºYÏi}†àZ¦Ý_<¯™Cøç¸¼?×œ€r2NïˆY$QõZìÐá&ÓJ;T2[G›dÊº"»ëxè°ažé7Írz‘öýÞ-²oft¹cûÏD{™b«£<.ò•Èä<…‚Äh‘gUœf¥(Ïý_%·qžs¹mU/U˜’ÍÎ»6ÿÓÖüÓœ™.
º6Rž¶f3dùbµ®òKÂzVuœ<ëãh’UÅc@O”ƒ3÷Lß3ä¸GŽE.20>¤ÎSØÌ°©* Ðkälá†-p\?tó[n˜¯¢PÝ·Á+Mÿ7\?±ÄhÊ4Å¤‡«5RÔµ¹ÈQxÁ;Â¼Rƒ™ÂsTÁcàÅp_;Ü¹¥ÍÙ -QÝ€‡Üâf‡âóàÃc"²g¿Öé S ]°ŸÍŒÝt`”á[EŸHÝ
cº«¦¹UßÅL–ÄðôzÀJU~Dà/	»7ê¨>4Ãã*O®Õ;µWÑî×ìƒÚGõÞårq’Í’LÀÚr¼¨ÒÏiõ8:=?šÌg‡?OŽ®O&Gº·ƒùu“.>]¢õ’zÝì~¹žBhôÅÉt2³A{@ËE¾B§õN‹à_&'‡ç§“ùéd6 ˜hm†‰ã©?W3ºÄgÝòªT•¨e\òÃ%˜Œ?sÆ‘QŠiRùç2®ŒûNÒíœgyu¾‘šÒJÒ!1„ œ)#/4b6\Þ†u´Ve1fÍò ®o[Mo…†5ƒvôáÂàº 8r³´T[¼…ÚzåzÊˆb@Žaš ÜÐÞ[Sé]¸¢¥’h#ÇÆw›¼TYË$+é
|X5b\þÛ™dþÈ³›ðv"Î®1tÄI‰pû0¶/£a.²ÀŽ:²e˜œiâåhC“5o+FtTtàÅœk+¸`&ÙŸ
ì9kîhÃ†„’ÌC,Wñ»b`nmî½ÕZÑZw*øéô÷Çyñ	Jwc+ÀÏÐÃ‡ôÖ­Þˆµ4þì‰áS&†~”/6+jŽ«*A>«°ü†­ãX’ð™QÂªy5}ÀÈ‘^>ãÞÇo×¡ûâæ¥»hÔœH " W	1‚`•.aëà-0m`ÔH8´KðúÓC\þç¦½+yšÅN¨}<Ž³t…›H&«üo)åR‰éØX”¦®$>;ÇßO"ÇPltõ×‹É|zŠ5*)ær[z:·×ºÏ{eÚ;$Ž¦u…HË¹D3aAƒæõÈ'–6Â,¨ÕýÚ6 ­–ÈbX ÄzB•0]Zïô/’¨¦¼°{$™ìÉuÉ¬m'r·Ý‹FL?#Ék¬ÐG¼:.%š~ŽÖ¢KŠZmm$¡u$Úðöe7èˆ¸†b7Æÿ4½#!‰¹é°°ÿ :V'ŽXF_eØ0%Y·f¨øŒŠólùZã0ÎÆI¢Xú¾*§DŸkQž÷®ÁË·éÅ)¨†É†*ÙV.%øqºŽ_¢ì®ºwºlÈ
h¨ûßñÿ„Äú[ªŠLÎ¾®ÂazT¸ÉÎs§Û´$Œ‰ômQ>oeV+…GF{€—'™Òì&ÿ2_Å_Z×Ì7U‹ªÀqùím‰èÑ‰/ìb
<±Ü;'$‰4ºÍ—	±š94ÌxšãÜ¥¥™:„Ln†Û‹^Ú³1A}
ÁK€Ú·&§ïx']eÁþzÿxL°2’ÃBÄ3éB'V³»´›`E¾:ÌW«M†W÷Óxm‡£ú\r~rF¡ä½NäMcš§~´=…$Ù±ô×³àJRkÂJW7bf0UqpÇéûÐ1¼$µ$< [°µÜSsâWg"7ŠòË2Å_iž#Û*iõµAÔh8âŠA\,î­	aøPiŽVÐa|Ñ-ðH¡³àå‰¨#ú"<?rÚ›]A<4qíñ{‹ô€_Öî$
Y»‰»gÅU<Ò¾!! ™ØÍ7ê/›ÆÛp:76®-–+ÂæÎØé(Ü‹æÀiæz„‰ÊÃÑt¿™âaS’`¦f5µxLÉÌdmØ—oòïûMºL>ÆX—=9ÿ0›OÎÆïO&G&¨XEB'x&$ý V	7YÂí†‡´º° ‚\„"ÉKüÔž¸þÈÔ–ÊaÆ‹Iz<-äœ‡|do‰ÇoW|‚0w”¸<úVßÔ²G,Bzî²½%}×$¡Á³nlÊñjsvž×éa7B¡ç_)CŠ^G?ê˜aMŠR6eE£ ¦lÊS¢$ÿâ¿¼ “ßX'Áúzåé%(kîQ»Ó„4sAÊK~éÌË—¦uÏnÝP³èÆgÝt0‹nþG‘–2"¯ÈÑtŒgòüxzr5¹œŸŒ?Ì'ÿuxr}4¡Yš¼%–YävxÞ`~
ãŽÉ¬Hõà†Qe”DNËX·¨cþ¡Û¡4†ÁÀ§tàSÓžâË—iÀžby§ï'¦Þ†¢'È.!™Ap1±­w`”>ÄéŠKÝÇåÇx™&Då ´]CcŸ½Ã:¶iMi%¬PŠ+mf6£ÁVúzÍaªÎŒ&nÙÑ°¢¦Û°¨õrŽ‹¸µŽ	Ós¥:	Åhi8@&Œd¹Ä“¢Ø§ë‘Ñ&Äõx¥ÆúdM&:—yÁÕøæp–Ðí#þY}ç¨KC1$ßäGÌè¯-9cÔ™ ñK&ó‰]?•W·u\Ä+ûÑúyÇ”®ÜX,`úÐc-ÂÅ +=ÔvøÛücòdcÓv/XJÜmÓ–©¶kãÛä¾Î>eùCÆª™š=[–n¨)ïp˜6:¡/§tßMâüdðà…Çˆ»©Yw‹ýS‹fU1—f	úr®dI§M‰[6øÕ_šºû[òîW¾»¾~§®/¯Á'ÕÜÒèƒÇæÛAû}709´·Ñzáé¤ùÂó¬ýÂª‡o4µÓÝàéAãCeåß$tÈ0¨C™ü.²¢®¼»w!¨†«ë¿©îKKœ¸CùÒ È!$:k\HŠU—t•Ë8…º&i„¡3li	Óáa¦1Õ—Çž›Rš(Žº<š:’Ì-x¢Q4ö@ ®Ï¶B*–¬y*YQâ#c}=2ÂíSÖo T³y4°€‡d‰ÁDþ9…#ÜÜÿA'»ÉQbTøˆGCñW‡'0´ÂÉi#=¡‚omy'Ò=àõÏx½^>þ®Æ_ŒKÒvè59
;JxÂê{ó0…Úc-“±á§·Ý7Æ2âû%’e˜8“p½ÅÊµTËNìöe®ZýT}ÛžÝüÁ2§àŸBk²˜” ö¾†ÃÞ7³e©Ù´0ƒ‰!aÄá­Õ¥àð§JŒCXÁhŽœÀÔÍÅàÑ‡VàÝÀƒ¨L,ÌV‹ûh0ù²@kâÖ0êøž4ùÓa¸ÆÞ—ëœ*“ÔÚ¦³)ê1$ÌÌ<$$™1DRA”kŠW5z{‹¨l’!Êi2Àbd:xA“J8Õä û³RÏ|Ù­äVêz’Y±#ÞBºHšüežf¬—ÆæüÓ³ifÛ;	
ÞÖÇD	£žBÚ™1L%¥€Ú¨þUò?ÚWâj#1+(uù‰øíèÈVX—›•µ¯­‘iEÓµÓ•`@@èUÎ¤ÓÀíéÓDY7[NåBÒVÓßÝI¶‘®T§–*Áôµ,ñdý°8‡LÓÉ#óMŽâ/K/¾shÙƒ·¥»1§ã£1O¡«ïîW‡\,M3!%¹¥­iùšÝ1(÷È+>:õÒ°è­¥w)«K…Ô«û¸'«4c×|µ¹7´Ù”.ô@ÍexuIc›ºZMxtdÞ$pd1jF²5•fy
T€½Å :¬sµ¾PSé¬fÙrôÔSŒIÓÉÄ”Àû¤vzQ7§z£¯ÈÕºÖHá"Oü4.>u†-_ï¡)È¢™cªâ¿èIs÷½¼1š<u‚g˜ü–YGæÏøXup½ý…õŽ…‚Š×e5S\¾p‡)ìß«übSç#ìOß–S€†kÝ>5Ù©×Ò&é¡{^ŽiÊâ·Q/jæz5¨ÄH[|i…¸Þs° PJnÞ§6=P¶Ì2ÖŒ2g¶v-Ýz”ÃÞ1riLÝ1Ð­+)Y<,ùçüÉzÚ<M¡^=l* ¡ÛW¢p@¶Vqà±N%¢Š,Óì‚ÈE×¤‚Ç':ãORC¯â/_M}J`ÖiîÚÌ-—€n²¤£pcb¾¬=ðR¹ÓðKx$(\Š`™kÓ®$e!÷› myÂêm¼»=ën56n%y¸¾së?.·J:¹SuñP¨>cR.< ?¡Ç±Í½X.ê¾âgH†ç„¹Q4ü6<ár‚ndÕÞ1¼>{!ô>í“8Ò6‹Âu¢¦)t(LR1=$_"£Ai”êybSŽŒÑ(††W”¶5¡›µÌÚÁo^ò
Zé$®÷ãöÜôÔ!tÔÒ50­h®×íDw´×vñ4@4Xê0áÖ…ãÍ|ìF²RU¸J]ÅäÅ­[ƒ—L[Ç|Q[Y§Y†øÊIÕÏ6++üÍ$ƒ­ËÇy‰*ØÚ* ´¡î1O+TådÒ¤’úðhÃ®BcBµNkî76éxªwS•y…hÆƒZÒÙ”=e½wUÕ'™VÔËºŽÔ¥ÒYnæö:Ð-\$ÆK±Y!Sä¢Õ"&E"S7Í`~3ÁdøY¦¬1Bß&—a£7†Í}øý?yÛ]ŽVºWß´ÄÏ~ˆÆëõ’"ÀýW(N3‘aó"ÆZ½%‹i#‚uPJ_bõ¨IùJleD‚{‘‘ÄÚŽœ‚ÄVHÊ9b+R§Èp„Ðœ­ãüÒº¬)ç	ƒ/²-8ÒGÚb"ä [œMf	Y=WþHÚèºÊ>¶­›‚0½õ´†ø5:k:ëiN`Z¢å-ž6¦ÄÄZ‚ïÓ¤öˆ9À9'ÝÉ¡ÜTe;â¹_ñ„¤N¦"¯­Éqú:„jŽ¡±75;AŒ Ü‹QÇ«?@É€@3Z)\½mæ€Úaâ_¬)°Š*þ47ëù³Mð’ r@ÌÁL’ÁÜr³E ê´é¢Öî-ä‰?=„>ñ',Jn80<[†DÉ­öÅwˆì952Úª¦ßí|<_%†JîHëX*þH¼Õ"¦Š?.Â»Ð5‰ÍÞ¯ä†Ú`©D	
Äò÷Wè…´í	m‰ÝŸ)’ž´<A·ÕE‘¯r¦éƒžÖP@u·€”^[6<Ûœjes‚J•Id?ãk´—M=	yö€]ß2cê ïFú³Î'¥rmqiµ=ò‡$Å‰¿0ç?»Ggt:=›Ÿ\O,kµý„!k6äœ!<ÚYCq6Sƒæ:u¨õ[f0S
`ßwž4ŸÅ'¢jÚYm/bDYcïšåÂS®*¬FâÝH®RÌ×Ýô¯ýÛÝNã=ïJ?ßt²¸d{«˜OùñGxØjš„y1"ž7<›D®0œY‰!r^@È& ù+9[#|ƒ¦RSfN^”u‚2~kæP!áMÉWHÛ‹aUásZ¡9¹ìv·.¬tÄÙ'ŽAz…—öö[â¢°”œ¶¦G÷ò…è:ð%Äo4"<…ÁÍ¸•®P$þÎí”:·zÛ>Ú€¬äõª!t0ùb;ê£a1jüQýÞ\Ý|Ýºõˆ?¹áæPÐ<“É¼ÊÛÌ²ð8¥–ï;x¬±qò³½1lûòËL‹5¤¾æ¤!ïeñÛZ^·° [‰ÅoQµ“%úÄÞ9`Îì€°§zoÆ×¢{Aq¡‘‹#ê0:àÞ¾Tîãärzü×@é)6‚žÆäæýhú(òRö+GÎAèƒ„–½ûÍ bù‘w*ìI½|ÛÂÒ&Ú²k<M{a%„TC¿lã—ìî"Túÿ   ÿÿì}kw7²à÷ým}ØCMhFr2ë‡|%™N´#K‘vönNO‹lY}M²™î¦mÍŒïo_ÞÂ£IIIfÒg&»B¡P(
…*ÅžÇ9‘Å¿yÓÌVPÓAéhšÌÔÅÉc´\koÈ×„	XJÑ¥]*“üŽdø6ca†¢ƒ3Š  ãê¤ÕœäñíâUŸàâÜ`šH"6(ÖÁ@wy7tŒ^²r½ÇûÈµÚ¼Héá8X^/D’i lÍ'¾*V¨)“oá'¶lÐX
p—OhÅ€‰GIˆ-ÏLN,îÙÙãgwä­¥Á‚Â ŒV5qnÇØYÈbž@â¯-½/ æ5“‰8ZàÞËÝ³m„|Äq@0°ËIúƒøoø½¿u [“,Íó£ÕLN8¿¯/z~·ëéMù‘‡ùªìó¼exÄ~S[xÐEzßpO|ópE‰
äKôòJà …õ¸ÔŒ]ï© ZiüÚ[ì(èÎ{¿fEmÒk#{ƒh²Šæ•ÿò€žîÇC9’äiØw_]®cR{9pÂ/|Aî9£š=5‚®}l96\4µµ#Q`Ë‚+YÚB³ãm{MR›HŒV›åBV{a Þ$Œ¾h2<ôÔuÆppcQ9CÛT*Oœu[BèNP¾
‘	äÀÂÖf|;m½ò~\Bã¶i 8³»„2æókµ‡$èeÑ¿©ƒÀe‘7Õ²gó˜Sâ «é¿ÆžÍ_û×j>›ÍôñáÃt3õ$ÝÁ–CÐ@ùŽÕË÷Ëª. †ÓWf {Ñœð'Â€ýC(€oR¡½¦X6eK”AñóšÈþ•nÚ!{+Îjž·„¨4âõN¾œÕD|ú+Ýö‘ë§ézI]ÂfË‚¦âI§Uê ²9*ÚœÔdsM}x5<zûÝäâòäÝáx8y7¼œŸ±{ê–Q+°ºózmñ¹õN/{ÉñEâSŽƒ#1|ÛOÝF•:‹¼¡ŽþÍ¦Ñï3!e&$
4Ü•É^ÎLÑ	ZI¯X¨©r”7Åë:©³³kþG?SÑW/×Ëe~5/²!ªös
‹³•Ù8}Þe/¬U‡)]=ÏÜ€¨
¾q ö’mj…Ð„O §öò§Ça#‡ìB{8?>º‘ŠàC7_)ø˜v4]ÌIF/%ð·‡ïë¢˜©‰;CÌ›´sd8C63ø>¨×KÿµÅr¶€šÈ÷l2eì•ó5é±×öý­¸ÀM‡pÈæÞÛ¶œ³Ùd3:IÉ{§ùz9½‘9Ê…
Ù­	nh:¢)WoxÏ@¶¡ÓÀHvÙÃy!îÀù•yÃ è5T5ÂlôŸ§lã ªöDÉ¾þvpx:¼OÆÿu1œŒ.NÎÎ†—­=Ld–47Õ§WÅ<¿-f½'{{èþC°Å²•öQiJu’ç¤c‚`3+›EÙ4o—sòÎÂÇår›—÷ã¼l3Ê¾Ë(ûwÀÏ‰ÍDy:ÝN|íåmµ÷±âÆ¢w‹Åd~òòëâT+ådƒc«›bQÀ’T­ëiÑ\ÔÕÇÎujçÍ»M{Ÿl×FaF04…×Uü)ËçŸò[±—¯÷Âð&-H÷n«5] åß¬®öULç`4È¦Û¼+ju›	Œëò=M´{ø¾xG:Pé%¨fÉ©1¼ª´áPð–T;ªè¸$è zÃ²jß•÷d×/ç·¦Šw¹¨MH­	ËF@qzár²S?…a€û}Ø¤¦
l€¦ÛÚ1¨^GÕçã2
Ã«ê3Gý›È~¶ßwÙøÀaLÚ£|ú<Ü–³ãx˜ ô’`U¿ªóO ¼õX¾â$l8gÆ‰Þå ¡¸ëöˆt©‹p±}Hé¹ÛÏvvúŠ¸þüÈzsù²ŠöN«i>×Rœ€kÊø”L¾Ùª·ÿ§]2ýÈL[ý,Tö/¼(©ƒgåÒ&l€%{!š~ø¾˜¯XvOžÎµžñáÍáøøûÉÅáåðlÜÏ¾ýK?û®Î©\Ÿ_dÿ”¿N‡¯Çcþ¿íÏ—ÇD¬~ ]ºXyòOçrùêžþv7ûè¶Îþä¼,…¨QÈd×†×“½íKpQµE"(œdÙÈD]Q¡»7"¯sx'ì¢îw5Ü¹äàL¿lÉéÆùü€VÝÅÁŠì<
°&_f]Z!½9žWM1#,Þ±ž9ÊýÚŒØ¥C½nÚjŸÎ¯¯ÉïÞà•ÏŠ÷9´v´nÛjy§d õnÓç.?òCñ9_¾búœ?³Ò?è¾ŠsÌpPå›Û´¤é¬,¶Yâ‘…(q¹ðÅŠ°6•”£™Cî
³J„|„&ýŒ4¤‹«.½¦šúú2˜¾4ïZÀš%•;©]¨9lûï¬òæ·â‰h–O6Ò,1’1„Rü[í=;;WIñÈoÜR ƒ¦¡Ùï¾×ð‘²›æuÅí º6á¹3Ò÷ðUuT.VóB€}U´y9'œLT•õ6Ÿ”Ëëª-W}w³T«y>…Ð/Õ§¦ç|^ÕÅ¢\/|zŒ=3)ÇéLÀÜ¸7òêzCF±"uÒgKÏœ;ãï‡o†lt8:9ÞÜ”ïoæàÌ-{~Y}êÅn6ÛOø6ˆQr—«xÌ©¼ÇD“ê±éâI±ùš
-éˆ'»°ÿÚç2ŸÅ™žu?Cf ì6µµå5™ùp3T¨åq¥Æ‹)å°íÉûõKÚžt÷¢ÇwåôOvän¸¦c”¨cj4-áŒÔ êa{¾*–t#à;Þàçíü˜Å:ÅôC@ÝÚw‘3„KÝÝ2y!›˜+þ¯Â¡o`èš£42ò†ÔFÇÙD#3¡rVµ‡É¤‚òM¾zÎjôÅåƒlZÍ«šÞ*!K4£'íÚ÷ys¿§0õZÙ¥Çì]åfÇP[8>·Ü5d\­”ýc7þ“»€¯µZ.Ë•P\¥~’Âqc{sÌ.BÎ†?L^Ïþkòêäðôü»Éøübrr|~6ü¿!7lŠaßíxé“æ¬pÖ`­¬µýHaº—[6¨Øó)©xˆŽÓvn!ê±EGr·'¡)—/A(ÐSÃ#B™yA‡ú*"ô¹,“¡Zp %V´îÝ!.};ïx]¾‡ÍŒHQ»ªw#Q
L[²°<‡ˆ‹öÂ¤Mm(£å?¹ÿþ“Z þv¸àg<ò°Ç–ì¬L®GÒXùÒM¹'Áæá­{tm~§Çìë†{2ô`þ ‘{;Ü‹Ç4_2d©Ü:4Ø#—U‹ˆëÞŽˆæŠ·%Âý˜î“Â-ùÏÈ¬Ub¥ÌõÞ:7gEpoBÞÕH#Y1³Ë]€ñz>êNÖâ–ò†|aè&Ít·*÷éA=.Õº õ¸!x‰Ä¡y:à*`j;€5:ÓÄ)Æ¥WðÈÈXÀ¾Zî™˜]¼™Üñ„º{oB$ô@Þ4CE
Ü¢áK<—ËbÇø®†­wÇ4vÇ%À”:XQb&Ú£¸Ya5Ùë@"ã¼ŸòU¡.~Æ‰æñ*OÜ¥oX:±[¡ìt84®L0ÛÔY‹šýY? ´VÖ·!ã %Û8"¬Lí¯L)ˆŽl‘FSƒS#…XÙ¦2’]ôˆöoŒÑ/@@—£–”‹EÔZ¢Q/y¤¸7ù’´I´‰Þ5»d ‰(þ¹¹Y‘"D¢v–ˆ=_”NÙó»æÖõò|ùöd|w˜{)æQpT±tOìÁ½{¶·òØØmŠ×€Û]ÓƒÁêO80lzûlJ	¤MSðdéy>ƒ+cºpÛdJÒðÀ£ôkã÷°êÑ»´=1H«çQO‘ ¼¯€rd4ø ÷y½õG®˜õZË!SÉw£PmË™|µ±ÁÓ}ªÙn|Ô7yj‡«PPø;=ÕDÜQŠ@™àÜº)¯æW7,ûB
ÝüÀÌž{ª#^ŽkQƒŠS6*[µÔŒ«“Î›”ŸÚv°Ón &·ƒèºÃR,›u]©Á)öøÎ³¯6Ø}ª®˜ÅŽóùü*Ÿ~èaóì?Ï?’Å»œÎÝs¿ZZ-«Êu]-Žóéw1HY†:K*tp7îÚ””8Ûã—]$¶[õÞŽÕ»ÊM_çõE]]“•ù‡²½éY†¤¾m9òšÆ@iloWd5’66°µ‚Ó‚mý
Ù7Òmaÿêæ8ÿHE«]Uh¸bZðE¶ïr“×Ùõ5‰ù™Ø§½¢(/·à3+@4ƒ`ïé ?ªµøK0Ú¬:«˜—hÍ‹»é¾Ýmýb0]#À ûŠ¾ÉËå8¿’eS¢ÝŸiïŽ`(s«x^ËëÞìf
M0x ¨µ¶y˜N/g_îÕSL0Ê2ÍãËsåtò…aŠn.&¥ôLlÁ¬é¢!çê¹ÀÄ0Ô¼®êõ"l¦ñ"7®Vå´Ñ§#{s^S«‡tŠq?{dò˜ßm6¤+¦¯r][Mãƒ_”‚JzÁe³c÷NÈ®å¨G18¢ÏÀÍœ¯©k~§"ª èÄ·@)ªÓ¼–wQ‡ƒ9ónÔi®Ø?òœ‰ó•hMµmò ¼5'p@µ‘çôô¬¾+á©ˆ]”èY‹á”lôaý²¡ónöô>y<_C”\†®šêYÐpå™iÌPÞåpÛd,vkˆ5ÁôqÕÐ¾·•8”ÚÄ›ÙM²Á£¥üøSô`†q¹ÓÃUsuéC:Ú›·>~ú¥¨UM³Ø)w P)ð|š¤okö§&ýzÂ»¢¶¿­º«8e·uý3GðÇ½ŸbVmÄ èÚ‹Y ‡™‘9ÆÛ¢÷à°³ÉˆGé¥CM ûˆ‡q9Þ5:$…ð>æ9Q%#!ÒCs^åºa,™ÿò±‘ÆÛ®²Á4ù&[—¨|J`ýøáa©­DºÊ•r¨Ñë™5¤çÖ ¹É?ŸþØ¡KøÀñVÍ0ç3j·’Ï)¾pg•¸'ûšyF=’79÷I;óÙ0Õ²M‘>;·&øÆA9hªõòõg˜îQ	Å/>|RŒ"Š¾ue}ø˜%‰`ð`Î“NcØæ¥Ÿý1ÛÃÈÀ0¨‘Üp$ë›þh}©œt“ý¡{tC¤Òì¢.®"¦E“5Î›™«&ÀL¤¡wùÝF^œ2/”Y–'Íi±]è.uÒ»nVÎÊ¶Xìd_iÀ`ÐÐ‡ÚY…,[yf”›Îó¦ùnM½–ÜÃ Ò:áH/*Š1	4'B³Ã–?ÈXéÊÄpw¶ùÒQæªŒ(Â²‚Qá¦ïµØØÀßÏÌ$ä¯@­Ûúù”FÜ)P¶ ƒñÏzû3››®Jk_dVuÅÌ‘•yMù«j]&‘¿_ çÛ.«[°èŸ[kz$^5­@%*0eï©–’„îŽAÅ?BâÙÒI¥Î²FÄæª?>þãn=¶›Lp%`h2‘ÊX%\LÒW‚4¤c$\:cÔâÒ¹ƒ+Œ¨3ç|›je…%!ø <ü|.Ä:3oP)ÎÑÒƒÇï‰ÃâÞí¢¸6—X½°ˆBe3†©!\^fßìeO³'{š0ÕÅc"ïÈÒá¾zE´÷p`&þ`"ªSeçt?Ö-dé¼,¦ÄÿÖá²`Â\ÈÅMp¾hÌ\ì1^³”âBü¼*g0¡`V­!{øÓˆƒüjÓ.…Ï•A¹
{÷ŸEˆò¶´.–ÐÊ˜H‡GKƒúP‰¿âŸD+—3·Ú7žj%?šÓõÍo}Õ Q
íÕsÎx_gOx*Òþ.Ë>ôI`,u[ÐígN†GÈ>²™škÒoO^šâŒa‹CU`¯¯°Ÿ}Ó7i!¢µ@.ºEÕdN`â~û‡mÒ÷N·•´÷ÕÅ'ÓÅÍ!–ŠÎæ%µäAî€ó«†æùìµ7eÓï<ƒènÛ.ÙJˆñb×’ím²ŒjK¨µì‹yv»|-„·OJ¢}Ý\:þCþÃÝu¨S5¥ÙÈÉZ/øºžÏf²ßb‰ßŠÞ[7ä#²gwŽ-qÊÄ›_„²ïÎ"±w7R•‡÷ƒô“»CZ€¡M'y¤‚³ 5E(¹€>ƒRÂ7ÅØ@Ô‡°hª§Ù/û¯‹¼\b–˜©¬2ÂsÅ†E¡£cÓWÊ¸Ž˜Q1#¦3‰aW˜‚)ƒïV‡hœ·=7 eÝ|ñáè°¹„-²fš›”3ç²-ýGïÊ×_g !8U¤÷‘`ü(À¬oåÖõBÙÖAAN¸ž#­…åò•ZÞzåóx]Õ‡ÀªZtj×tèÞ¡b¸|_6`ÝçvðÛVFI](“J"S—¸XtQHc­a¶€¢×óœútõö³çÏÁGéŸüÏo¬‚xuÆóË³®[ßóÏò;£†ù½.>Và+#Í3c¸³«E „é·…Ì¥f~}Mš/fŠòÚ’@ÁÐU’[ƒÎŠO¯Êk>Ï.ò:_44—\ÞXÁÕíÇÄ¶øj$r¯†laSqÑetŒÙ•¼>ƒ›ê¨ù;m9`³EM9)8ø<ˆñö°yEù[·!)år W,š¦·¡õ
ø”»j‚ÂÀÛ’7÷ŒÕMo6pÿ`K³ŸO=Õ?P¿§Äºb¢­I®Ø=$~%oPÑðŠXÖ0ûÁÄ§" Ö´Í})$ïÊ(uÕÍb%ØìîãX/†µ?2Hª4V&(	dUe²ÕÕØ™½‘!•¥‰X­>¿Ê3EïHd¯›Ï«O“U+µê¾82{.E(•Êâb…¶2VËÑš^‡/\á‘á~d§‚Ë¤·VÊ’ùØ1ôëþ¼e‡D}’Îã£áéð˜{ŸyÒV2”¶åçÊ3‘ËÆªÝ³jiœ¢ ~ÇixžžÞ>d>#è˜pñš£jQdMµÐ6Ô·g‹a®YÉø4PÃD6†ÐùÂ€BM ÿµ¡gýóEöÄÅêN
:ÀÜ© bMÀöM&–ÈNæå¢lY.mÖÛá;Õ¹R®qs…è-]~›žp5e?•&å_¤Ô	ÝkpÉ½†ÿÈKýðÊ2G»P>Á9‹–Ã²½My3¡%*kƒý§'ûß~ÛÅó"† ¬¦ÝpØßûó7þvÿ/O:áAaà“É3Í:k`0j'ËëŠö×?ê.~[úÐÈ'Åþ%w;$ã]ßJ§›¢›/LµbFÌðýŠ£%]ÎÓ(I‹R:2Q’lIj@–Î Åƒ˜­1!ÎÏ_ÄƒQfƒ™)ê²Yù¦å_öÿãIâ™µÙœþö/üóŸ|d1*¥/
¢§ñÍh€._ª)<K™‹jŠŒ­!®íYšöËœ"5­÷_Vý]íü5¨2cIÝÎÈ?Ïµ=‚ÈÂXºé®ˆÃEõßeVÀE{v7Œ¢ØBfÔ_m9ù9Zw§Dªtèž6âÀ	Ïïšéïšiß¼fúpzaGË‘8¼X€sÑÎåòÂó7=Ë9²oßOì›¼¨UÍjú“×–}Ü&Ê)Ò Âÿ”ÓOûÎÌ×ÜÃÊË.;<4ðè=bxÒhm”0Xs´v´à.ê­^|p 0àR9HkÔð†Ã¼n€~ÀÌØÛ´UÇü*_Nhd«$°²´`¹$úFÑ¦^!0lå²ã¨iü`Éb9ÉWÓ€ªâËjy»¨Ö‰Ei?À]Ž'`²N©•÷­Ú"ERÀÑ’QìæåòC1›€Öq¤^´‘–ÆèžÕPgä¨öJ6¯0‹If[ðÚAvô <1ÊÿKo±‹ËðcTÌ¯éW_Zrz7›rî„‘Í3d&ïèÍˆÆv]ë«¶e­‰v[®òeÛD ¥;’Š‡×µÒQSü 3[J@G3`;
£žØT|õUéÏh­¡£×& XØN€åšRUô°gêõ€‡jBŠX-Ïh˜Oë=fŠ§Ž6øþõ"…vu”vE0ÖûðÍà;hÈ3\
ršYÔ	 ¦
ã_ÙœÐ¿'S‚¶»3Ú1QšYìXº¿}ã¥ÛýIÙÂ¿”€A·&Ii¤±72–±¦wÂÎÉ{jîB;@ýÈºNÁT`Ã3ñ}ù‚0hl»&ãdë|€ØL=EmCé4EÛœ´ì¨Uç8Ã¾jí;Ù¡¨	Êc€J¡´o¼­niëK3q——ì¥µFù
R3×®A §žh_èËÛŽ*4Ë	_~d Ñ¶¾¥KÑë²nZ$ö¤3‡ž8xcûA¦‹á°¼ÅÜQÑŒŠ÷9$QZºÆjwŠíOMò:5ƒì×q´‘¡¹mé\
ô¿0xÝŸ!BoÍ\:­4‘…6áÌ’-ªúmÆ8\±ú"ë(®œiô3åD’>S¾Mè;[oíÇqf/x®ê"ÿ.ÖùÒ®ŸÛ¬Ù/6øöð"ˆºx?XiÃ9ÁöÆø‘EƒßÑ=‰qvW€ëšn¦÷ÄùJFJx8±[ôß­#xîIaHvÓEDt}ž;ÑIàérì·¥›À“>Ï…ô Ôa÷H—vóâº3:å¥!1õ-ÒÜ‰ƒ¬óå
…”[<éü|3%!UAØN9x ÅàÎ•‚Í‚RîMØB	ˆ( ›M•i’¦ójþ+^É·0¡Î<¶ÇBp°vXÎ5Ï¢¾Î§…w;©¢›W@Â6¡NÂE`/õJ%~&âÛwõ”ãä‰If¡”“C;30s]„Û?+>qÐHÛ¢Ù¼ˆ’Â×w‘
…ƒúíÌƒ:)Ú¼]7o—m9gÎöµLZ+Kso¯_%¬× ˆH# ‘|v[=Ä´ö²ÚØ­îÂªÿ.Ÿ¯š^»E«§Äã}ä7­–mNô«¿·Zà3UóY
‚ÆYOR|4÷y}ÐYVŸdt—Þè¶i‹Å€Ï‘q¹(Þ”óyÙº_gû{{†›3ýÏZR¦Z»ê ]NOZ6WÕ<=0õ{õâCq;‚hDƒ’×¡žwíà&oÎŠÏ êgŽUEã'Tº¦¶ƒ%…€¨C¬úMâ!;÷&ooàömO¾’
 ômðæðÿNÞž¾ÙÍ‹p~ËSè–áÉ+†[Á/=cN„<2[|þÊ¹ô•að¿Ê45‘Kc6R^÷Ä€#H…„RFQÇ7‹G0<´"àÑ^q´_À8lT•± §’!âoi &_À¿[?5&NÄB»·¯ÇÃÉ›ÃÑ_'Ã7çÿçd2ŽßŽ<ªgXúèÏ—¾ä¤?Paágaå©£Ž÷6WÊ€?¶Òï$µ]ñŽÙÓ{¸‘ë¶˜ñøÀXêÃbF¸ JúB
–<¦+«­†&VkðÀâ²u©HÅhÐ·´øâQU­°Ç2ª¾†*dÜ[ä4¢‹BçÀŸÞ«…Ë½ÚÉ8€ž!:™ÉrµÈEPæ£”n¯«ú{B˜~¦ElöêûÀÇ§¼Õ!¿{h]4€´NLÓÙøÛåÂë¬™L“ýv	3„Þtáº(OAàpñ,rô¾g·*Xv­š <;°ß,˜ƒªcbÑ¾9‰qVNÛû©·0¶ë…p+`¯Õ²¢¯4±dÞh&éiæ^§÷~ÂzÎ©¢5µÂºüá@ÄÎ€ç&C
æÓv\}(¸¦*ÊªËâŸWUMZ/Jö~àœ‚Ô¶\çÐÊ2x…þ¶ç:EŒ]·óu@Ú*ÁòyEõWŸ;œ§W(±u?­ël3Êlô/x!!î_€AÞsÂ6¤aD—±®é”ŽHÆÑ0"h»8Û¥§îyh®0.hçÁ‡7Þo'€,ÛŒÑA!ÛÞ$ƒånÎæ1
C¢á#jNÂ¸…ïpãühÞA=¼†4ÚYãNôÚè@"ùi)ôùŽ1ÿ“T›jd;Úe?f°ëIe‡)4é˜Å|A¨l>º…•P[”¤Ôv¾ôLù¤$©SÒcÔÐ¯Ëy¹LŽny0 ñíª †ø™úíh	êH?ƒ°Þý%E²¶.d>Ò^dM1§¦ô¯?êí<Îþ¬-Ë,p-«Tbôí…:ˆëºˆ§\~Ìç¥¸$E8‡hóYQsËÀÈªc05
‹›vÂà kfëKR6výÌjÈçíš
ô¦èa3Š(‹eùqå¶üS´ÍIÛ9jÖä'9ÁÒ„Pp`Õjø^ˆP–T™Œ	R:oZ.ÓéÞö€YãÈF{j zQÔe5ƒV4š`îH:R˜°æ½i-b`ÛaÀ«FÀw<l-ÄèO¿ŒZ æ™[o¤ü@ `7ýÇ„o´VO„ôj¯ZÄ®ä~
ô.lB°Œªô¨IÎ‰;qUØpyEuÝL¶Eã²‡ÇËÀíT<b`r«¨z¦ÇáR²7áØm¤xCª:ô[¸NÒ¬ñÍBÞ–³œÿý‹oý	§VM›¡Ž) v#¦†t&Ñf«Þ©Æ»dÞÛBÐñêÿ,×z¾­ŸyY?&GàbjÀ*Ï¢kçu9¿í©“ Åoö5v|Cg à­6.uõ÷b©,¸AÕ=àÊÿ®‘ó¸@ †n4˜j0Rßw‹ázÀNé±qôPèK˜¯FùøqÂ±ÍM9›Ë·ËYEEÌ`J”CÔ³“×Æ†Àã¤±ÂøÇÈGÕi=B°G6(Ï»• 18€ß‘·º+P²{z7M7gEAV<<xÔjyÞ@c%{Îd0†¥²*<»Î¢Œ*è“Â¬ðÌRY•¶ÙÕd÷—gÂ;Ž`Q‡çž¹JPÎÇYw‡Î]&õ´„=’×Ö„>>VûÐ›Ú1Ø’‰®‹=:½û”òÁèeó=¥ìÑ-ÐVÃaQåý¶vÎøp’Þ,.¾]AGÈçóU¾²¬-ÔyaÅ)YÂ¥„¢§á5Ü†t2z]bÒ/
0D	4Ma{Âwv&F3w@¼K\ªz“©…ÄK—šÏ’0‚ÂlÈ¤ðM@`LÄÑ¨7õQJÝƒ@Ú$u{‹ú¯Åô0L¼Žµà[WxÂö”«Hðˆ²ƒO¢‚†§éU]|ôä[x9Æd—Š†¡Â³ô}˜jŽg¼éÁC©4ðÇ›]ê-2x¤ìobè [cèúzÚ“ŽÉ³òÃ#¡±…Ü-êÃ½Fªº,,k½óÒY:§š×Ï†ï;­òØ£¹]NoêjI$þ,#Kæôóïl~4‡ËIôÞ	£4¬Ü±p`;U—XÞÔ¿ÈvEÀ¢ü£Jk`þÉ.ˆÈ áµî~èºa×Cd=ñÁô´Ã†P2öí’hs×e½(f‡ëöÆZïGWó‚	¯·Þpg»aé²¿A6Ì~â¦t1|[À'ü…‰7†²ùT¶Ó)È.*z(ëze}‡,„‹r½Øyê£WÁŽÃ÷Ô‡ï‚U±·v[ï‡çØ˜×ùzÞ>E ÍÊ&‚S¬.§7på¥ªe´Dg­Ëùì]^7ƒWÃ£·ßMÞ/G'çg»8öbß5.>·pDÑˆ®Oê)Ä!Pn ­óÉÌ„nvƒ{?eˆœìçXÿîOS3—-=A
RÎ)VôwÂa„‹Ä~¢‘3¬{ñÜ`(t\1Ì|‰õy·c…bËkýFbÈ?4caÆ¯e_Ój±(–3æ&«ÑÆáO¹`Îgv7€[§ùérº²ëÀ ÜU]h†0ky7Î=OWêbêáòÅ¸¥M~³£Rý AQ²;FôŠ¯±ªs7x¹‘ÅC@¹j]pS©BIÙ€ÔÝ/—p_ÚïóææM¾¢h}ŸÇ8ôë3ë<££Üjqt½äÒü*qÈ[V† ÓEÒ2EXÎhþ¾>P/ÃuèDã{ÐK8u“K’²±…—+ÿdtl±>z®.º·jà`»ã¸é8Pºãït ¤|‘ìH-ÿ)ªøKHFð‰1é ÌŸ®õs|:¹ªØeû£ªu(«üsü¥Ÿœ)«Åq’ˆ•<ç$É¼ôÝ\,¡‚Œ‘âcœP\Ï¬B:5M×hn,õÕ£åqÀ:Éh'ëFégß¤±©)ÿ
ò»yƒØºßþpþU^ÿk™ÖÄ{ô†E£Tê„ˆýbÃ¡±^h!w$¤o”ååôð!áA.Ã˜_SßÁÃªòD‰oñiÈA"J^=q­<"kS‘Yk”/yg˜y€÷4¶:<Fnz-è1~ræ÷²¢Õë¨cöÎúâòä\þ”;ì”~üÇ‚ƒó¦Ë,¢à‘žÝÃÒ:‹ÁÐÏ<²Å¸ŒŠ°À[ˆ>>± àAÆÝ«‰Å«Ó¦ßÐùÄýMÁ_ÿ|bAú»Í'Zgûi¤ƒ1fýðûìy¨Ù£TÚ¶&‡ì‹F…p,ä'_"‹Ê˜èÏŒâPl´"QÐ9÷Ü•¥Ôl/Ó° •EŒÁÃL-<åÏ×¬·ï‰˜bí*øk]ãµ›@¹Wœt¢<Í,6o’GîN tŠyIF„j'¨¢¢–gG»Oà|ÕÒÍåÍ¤¥º„üÕ¬ç›G»V·Ñ™:§–ê<=Ž‘½ö´oE¦GÕ”Ú¯f®Ï0æ–âíiÈA9T€sÓHg&Ê¢“ýLòØi~[­‰öz´¾ºšƒweSj™€ég|ÚócÂÜRä‡]ëø½@'iÐ)`6¯2yûúmŒprçåÌS7tÈzÑ·Z³'“>]7åHÑÂû8»äŒ]rÂ.O€r»-=ÖdQœö²—äÿO%IÜÍ-¶«‡¬JwI°ÿ.—ö¨¥²ŒÎß†8£Ü#b&¸H´Ê2=ß&Ž‘[kV!^©/pòdXtúZ‰ñ¦qzÑj]^TZè…®‰Âà)‚	èò€þ'âå+	ÒXó	@Àä•€B¿‚öD)Á‚M*(aáŠÈ»äÊ	štŠh|2j…HÐùÙÒÃÔû¯ËÏ ÂÝWûd^<Þ÷ÝEálÉk=0o—“(âÁLAƒ³÷kHhêÏû”‚zŠMÙ2ƒE#žRwˆH§iA•]ÆD0‘ñÚ„m¹n—Áö:ÇjòkÐVÿá«'˜±)Þ€Ì–äü.b„A»´cùõ0úu‡Þl¯×ï¶ßÍmÉÔªÅeì]}Uõ¨;Tø™ºÝSZ´Û“2&&Ø!‰4}Á¾Ô»ý
kö!¶¼Ê¥uR¤èºJU¬mÖ@  7(!Æ„Çp0&2àVuÛ“ól¿s¢Äy¬“¹˜³/²+ög0þÒæø‚ìéŠ·Ë¦|¿,èþ€t	>‘ÍÀ•®_rm[|iSWÒ0Û+m™²Ú™@tÁî.’í” lµÄ5"QR
ZÌäb¤Æ†…[ÌþHPSvé7¯þØÓL.C#‘¹ŠiÌÉú¦˜•ù«¼Íµó×i>Ÿ‚ØêA™>ãºÑ\oz¯A³¾bÁ¨@ÿØÿÓ.ýß”—L“ãuÓV/eÒqGeÀ6É5«¦ëÙûšÒÅåbCžÐ XìŒågûÔŒ@ßÃë¶lýaóîd4Lþmî¦ÑÿÚ9ÐƒV:‘80øG•é7rÅ¦)pï0ä…=ÝqmÚZK|Áÿ(®¤…ßu¿½Å–Úw–¿Õú¹\8Ñ–7¸ž=“!dÃ(áLdW¢Ü¬½ù²üñp7b|Yl·-í+[Œ7Gïþ•%·õ_á€#âÀ€ïu˜ÐŒÀ¿Ï[k}|/­¦Cðð•dé»CöNleoIÆ×ÞÁtà2=@Ûœ+˜!l¬ÍÍÞv»×ºë1ÜmtTð@Ê|gk^Ø’ •ÿ»ð´¤Lã3Ai[KôÛþ¸Žùâ0CŒ(Ä`ù@…­¼)×_‚î³ÙÃ÷6r$ b6exÌ&˜lP–µ®<ÓÉœ¶‰)¢ÖIÂTúâàã1ÑÅÌs>Ë›“m³³¹Â7eH±H,ÔÖ}“ÚºÊ‘ï©C’ì‹Æ}ØŒÊ›}D‡Ó?¬;÷hüq)¢50Ñz,1ž_ÁÉPšaH/½qžû5Áó`F":àwo(êÖ‡»2ÑÎ ã¾•èNd+;Þ±íìE.L¿ÝíhÏ1Áso†##çzË„Š+CLvÜg†öç†öJú=ÖÔá†tôrKà0ºË dOTî'Ít•1Úéî¤Û‰•0Š1‚ŠLß
Ù1’vÂÊ‘vv§.l†ü¬ô ÷‚R_ÛNÖSg³ø$ê÷ÜÏ*¶“ê5ÜˆsÀÛ3ÂÜÏîƒÜ²»}‰vÅ³wG‹Ê›Np¶ì¹é„Ñƒêÿ.rZžˆ½Aþe¾€üÇßÖÅºàrSDJC¯DX÷œáÎ˜§º1ýí´lXA®àNèLüñÂ‹(k«üÒñº&j3ÜS„°Ûúóè¢¨×·¯áÞ„jèíŒ†§Ãã1ýž½¾<ÃTÈÏœ;ø~x9Ô=1_î86÷±¦Ej4pFV¨ÅÑm[­¯¯‹šáñ‚ã=¸’h<Òž»GoÖL($G-6¾DÌŠ¦¨KJ*
¿OñÅiv²l¿yÂÒizCÁu¤ÉÈ*WŸ‡Ÿ§ÅŠJsÔo…
{?(zvK_Ø5À96þj ¢97ÅgeC&†7‰m€‘®™{@vY1:«h±K1Hl´Ì–¶
‹DGÙáEX x…nðv»ˆtóŠ¤K
rËƒ«!uO¡›S!ü ÛlaZ[Ð•ø!ëg¡àkÐ®²Ï(€auçíÄì4×|A£ØIæƒcb¿yØ˜î‚ÑËqé#O—ÃˆÄ9åäÎ(t¸
8—þQ<]}ô'v«-LNÒ‘7ÕŒô¥ £†ÌWJ°š[wÞ¤á¦¹°5ŸvP´Ñ˜Ë¦~«ë ¢Õ8Ž8ºïÞÜrO;¹±wz´6XIÃ^›adîjwàÑðŸl¬â_Ô¬g €Ý4]‚Ÿ{áÊ¾ÄaP|.¦ë¶xY½óŠhøãaåRlJ&4m±ß@®˜r(põàp9¼8=<f'gãs	šÛxÔ{ÙÏ^îîøà³¨»õ-†€³“¸bÿ°¹n™CÂ®’|w9¡”ÜŒ«QK^ôd/’Wå’söƒsQÖÐzÒÏ"Ða8œÜÍ÷¾‡`ÃÙB0#;?SS<<¶T;Ÿ˜•v’[@Ù9®æÜq¯êêšôZ}y¦›zT2(U 4Û`wYX–c·»ß=Ós:@½0Wê§vS½%›ëbÆWÑkœeÑEŽûKazSÌW~³”—Gôo·vÀÉ5c­#ÂÆõRï-öÔ>
Ê 5	e<$ðâ'mo†²
¬}%šBÞ7‘¶Ð¥}ãíI„#Ó2UQÜ2ÕÞŒÿëb89;|3Ô7£N¿`Ñõ·¶ÈË¥žç¹˜•-¬¹k™ YÁÚéë£ÝV¼Äîî€†Ú÷‰ûÄ•¾óë'øl´e›”Î[}nÚ;NQvêLåUÓ&¬Q84o|´éët50‹½p`2iöûœŽ1Hlj#Uâ3üâòüõÉé=Or3˜ë3Üë”G<-¶¸ýÛšh}í­R¬U]E@{»\˜GzoÇµÓÏè8N†­÷“Zklò¥RÔùweñéPéNS;X”ŒãÍ
‹äSNÁTrY°€\6xÝ¥W¨;<ƒl7Eo>ÂáƒŽd‚×AMý H1h&R¾$1#‹Î—<z$–7mÆf'¬e+\¦‹ Ön$ì¥Âi òÝÊ˜Á¢*?;Ep¶LrVm ‚½¬éz\ñœÅ"æIÃ‘—G¥œ:
þfÃh`™ XçXS¥!L(ï‚dæµ_—Z…•¶»ïuÌMIÛ/é<I´¡xp¢\¹=Özí¿ŸîcˆìE€3èé7oü™Ñ+^Yì‡1¼X‘g>F€åLæÁ[°C?¢1Œy‚õIÍz4á1eŒcPl&«¼œi±6g*êŠ¦I¾Äœ=>„œ.Xk BEïÁÆEÂ±ÀÁ/C¼4å	_šP60©©ËÓ}Gµã¤O$ŒB 
;cdÝX?û“~ZžrÇDet°|ž¢SžÆL†ÐÔß­Ý{óœ~0ˆ4êœò[á”ë`?Øå¬øL8pNã„)¨ôýù5Ä›E’ÕÃ2TÙXÃ–UñºŸˆ
=kâö)O tá’¹ù†67eóôi ¬¹Fñ«BÞÒNf„¯ËøÚnÐq”/E¬9›%f×±ÒºŠ†§ˆ-g‘å*¶²ôÑæÝEAŠ›¬”YAx¾S¼'j˜ü<(™H‘å±sëYƒ‹“CW)tLÑL°m¼SççÃ'^B¶éïnU‘ßf0¿byk¨¤¨Mã?òîa.û,˜Q¹ì•2Ga_€–±#}nü
=“_PVð9î‹þ?ê‰VXI+[zH'Ú¹ú+œbšUÍÎÔÃMe&–žøJin¬Ê9´Óô“âg®åõ³Úë¾&*ÖFLrÉkw:ãBÐá	t‹G¯
öžà%¹„vàx[,þ˜’íx¿¤
rÁFº©þ`z*èÎÚ°Ó]yP1ñzúË®„½å‰5XÏÁdª)OE”¸džž½dˆî'¶X *Ö¶f/Ú©ÆTvß®ò™›‚‰D‹(Ñ<ûËáL™`5ÔQOãäèÖØ·ó†ª’YP›Y­«º˜Mxæw9íÆÅvFKjí+°;«R¶Õû÷ób$0á
£§0vráíïU§;ŒæÐ{t¯W<óOï—m»×"™™ìSÖejµáõµÒDaË¥×.œåxmý¶…¿2!„ýÎÙÅ<ò é9esÚÀ„UÁ¾½.Z²(Öƒkø·gç	Úë‹rë«vkdá)püÄãT‘5üëÖLl	8\–ÈµN/ô¼ªóOPŸz*ñ=¼÷n:Üµ,Ò8-Ëë@éÎcðl+Èm4œ3ó~¾@d~nÌçÎê¸à¸É› g{@VA‡öœOìäXžNVuöÛB›¶t&šR)•	±“·+‘;ÏX»².ÄÂB›‘î œç!´$¾ñŽØØÕÅ¼Q´ox‚)þËM1u§Ä•st#òªÚ5Ü;Ú·©cMZÆeØó"%Èjß­8–èçÚÈf°Œ	ýŸç‹º.g…|³ª«–ˆ1Ý‘ÚÅäš”Ž7šåõ{F­¾Š#Ð×î­çóùU>ýðís~E“Ý°îg¼üAV-ùå‘hxD(Änk„„[éÝtØE.N?ˆc‡ÄÙæWÍyÝ·º
‚e‹^c*%\¶öîùºxÔÛ(%áÁãÿ#÷\p”nT"í¢¹…Ð»~7>ØASå›ºCFòÜ*9!•,2D¸rü©9#‘_ƒi5+È—ú^´Åç6{šíìøb†cÔQÃ-èY0z²òöñ“½½=xm¿D”‡ÜP:‰‡ÓjšÏS¥ƒ+žðD‘Qè wå=OêÍVxp‡w»ik!KÅéóÎ¸cJ¡'(×±Í"¿½*.ýWˆõgƒk¼|Ê`¾ôz’néŠçŽnëÚM¡¦xLªEhœrƒ—™.¢†8ñðÚk?ŸˆÇo
õ˜9á¡I´.ÞÉ" ž8u9:Iäk˜†iaZ¾ªÔê¢dŸÿØÖ€m4"Ü2ã8@SÍ“‰÷+àñß±èŠeèN<éw»áI»ß5Ín¸t5—®˜ðdSYª¿vÇ;ŠþMV¯ßàŠ$ïõ0¹h$Þú²1I•ä±o8¦'g£áå˜Ý!¡¸7†^ÓŸè2ýéx™Œ.
ñÛdöXt¹ZævÅº9¶¸9æVÆn‘Ù—¶Ø%]ý‚çáEîÖLãä‹SÂˆãrQðM¦eê‡GÄ¶bO&µ$âlQj·) íŽÉvÃÓ¬í˜L·àkÏçËdN N`ƒ$ ‘½Îæ2=Q.¥Ûráàì÷¢ŽâLÁa~ÝF´X”|Yr– Äa¥ÿ}r¡æÁft4¼\–‹eÎx{å˜ýBJ TÇÍgX¨G½«ôú¾C$ ¹4Öq›ß•þB#2fÄ¸¸©ÚjSìYå$ÔWPTâ-~Ìù0ÔO^ó“vÝ¶2~÷,³ï,Ù¯Ò€TÎl3šñÝ0¾Ïh·2t,ÀÉl
yH'Ü¬&Ki¯âpg"g¦^Ãüâœ3–hR)—ZŒ×9©èl†,à¿ö| /7ž&)¢ÌÏ%¹QÎ	Ê%K¯ÄêÑ>áÉ(l£UL£º3¬ßàê°Ðiž6.èTGˆMËÅF„ê4ÁúX¸Â!:¬ÊÆ£Àªo0ñCÍ=¥ZËÃÕJËNVª6àÑ-*0»(-ËþÔœžµìo‹GDŸM¶=tªM®ù_bT˜ðhœdg/,¼úÙQ]}‚‹uõ¾&t†Eþ¡¬5B!s8QÛÊûËvEÝ¨!@Ž!Œõ´5ÞG3­‹Â‰·J>ºî?²ñãO» 0—·”Ô—0€R£¸ù6xâû ZSØ|ß,úqï'îfaM>Y³\–­G—“n ¤Íà¨jÁýˆ {ÅþRþBÆ÷ýŸ0éRCæ-dWoŒªTRV°vÞ Þ=ÍNóõrzs8%›Â²½ei©¯éNA@ÃÂ¸«|îZ©´7¨X,gNt¢>*x»Œâœ	`òÖBv©WOÏ*åûKúøoà–Fz±Gêa=mnªO„ÅÚs˜‹ó¢n{jþE›†'œ§P =!ÿ9>}Ê„ì50tèæ.ESm¨e¯¢pm‰„{ª‚q|»[	¼×3‹€ÌV$’ ‡1$øòx85ÒQ	’†æ³ÏÁÆÒÙôïõC:°GyÍ>zçÕÌíõœ:þ¦˜%ð¦(¼Óï€s™á†l:ÿZÜ^Uy=3º&àÂeÔ@ôCqßøFšPyEW…x"!ßÕµã‘ÆF©Ï9Q¬Õì¿D0r(‡mK}½iwÙ…ÿ†G“w'Ã&GçãÉ›Ã“3êÝæiã`”ÿàK]åöúÖB¥/Pø\°D¦dÝµÉSèHúÑVŠû8¿§©ëÚúö²€%üîQJïÆL˜˜øÀC‘›6™}Ò°%Ë*bp8_ÓS¹¾Á´DèVëzZøH­FIojÐ)Y\çë9°Nz»ìjgE1k¶¯¯›Ub ¹-?E1ç7Ù8‰4AÏ†*†Y)ÜHYÊ,ÁÕ¢˜hì£ÞHSëÉÕšðŒ`©ìß7¤Ð-ãsžúM7_g³«vÉ¯ùxˆÒmó×%>…Ø”ÿ¶KêFÂþZ×1)J…çðìíäèíx|þ»ý–¡©òÐ¢‰‚÷· CÝ¡•h®ˆ"ÞãBÔèz+Ì:6»ü™{¿›V)¦ydM'5sAâÛ>ê§9ë™ÙrSNQî£é	½¾%EWÏ]+/®àmÎ‘íÙò†‹ÍL0›\¯ÿb|7çÐQ Å%K¼oâI»ÛâŒÝ`—kké=qwŸJ[ë¾~_"¾ÂµYûv/÷»ø;Ã&!Pî-ÑÔ¿<‘[Ø0B¹>’˜£Ú£„a2ÜÙ *!p#c$**„–­kUpÍšÆstmNÙý“gFYu3ÝõãÔãY°lè¢’uk‹Z.DŠæšeýÍÙžYW¼õÄê¬^a—_èœªZÂ5Ö†ÖjÙ§›byYL‹òcáÜ¼°q×T¥ÑmÓ‹×#roÊùüf²Ç~êfÏ³}ðþCö'öìðSÇH‰ý-e:«Ç™ÞŽÝê™d%‰ý%Ba8üøûàÑÆ <aÊ§Œ+}ñròŠŸ,g†#±˜Ç¶ÄaYÕðÃÅÂ@oø8[dl~6ã58 ÐÌ~Þ~˜!³«wä<‘>³²|¿¬êÂ±OMèH»¦g/qYv×šòïŽ¬€¹£1åÑWŸ‡rƒ`þÁ†ÛLL›ˆÅá$ííø°Úé{DãŒ^ç÷É”@Wè­¶Ž2}ÊeO½bÇ›‰H±n) ŸœsBðÄj ·œ@·[ÝE'³®Í¨¨a¡õÜ9fŸBâM(\ÐìF¥·Ù¹ŽÄTâ…×uÔÉ/Èn`ÂTàQžbÇMQ¡CH€õÆ»¨{¥Ÿ\Ôý%ü¹øhL”¨ˆÄ-Daø€¢ïªY¸ü‰k±û~Þ€)VÑ®ãa³àˆ¼/HÇ^çPë–Ú•†p™*v³šâNlˆ‡×Ýñ­‹;‚öfQ«›ÁpñÐ;{d¸EwþD¢|ÔÅE^ªô9‡Ëjy»¨ÖfÀŽSî¤lUg¾ùWz©hwz«§xzkÛÃÃWæ‘2¼÷•õvxÀ Œ†ˆÑÆëy„›×óÙˆðô"üŽsêôþ}éõü†Œ±’ì.	g 9§¬ò©¼þÝ„eŸ|<s´.ç³wyÝ^Þ~7¹¸<yw8NÞ/G'çg5~ÊÂa…PsqRòÖƒZÇn|•í{âf£1šGúVJÍ}KOf„Œ«ªne$¡WÅœÌ’úÖ´…O£ö7ÒÏÛjQNÅ¥úƒlE¸æ°…¶7jYº11Æuá ®MxÀlŠ»h{¿¸(çµ¸ôLˆI•ónä ïæ§A’Ø3?	ålÆP&ÿeÁ$Àß²nÚÁÌ±@³KKê’7¶nˆ}§ƒé‹6€,©ƒž5v}p¢n
"‰§‡G²,Åõz‚E¡¦ÀãMÈ´Ã™FXx9‡FÂÝ³ 1F E0Æ‡iƒ¸â#HCåº!A´à˜3ê´…?·Þ¢v‹Ù‚E’]1ª`U„\^­Y*cŒ¬x•Z¢t|œÜHD¼$Ëèér³Š&{8Ÿ÷ {(Þ—õÆ5ðÀhÏÜ%£Æ‰‡¸áÊs‰](	¡“|×a^Ñ’‘‘«“ï;ÍqÒS­Ê­]…gQP
i	†ì«Ój<K›cP\“(¤fŸYXõ©Ñ£¤ÙÕù/)ú8Ä‘øÃˆöŸÆTØÐåì`Màì•ä¸Z,ª%MÙ†d8¿/ø¬	Ö·S+XðoËW&âª•^\±Ú{Ãö´l	„ ­a1°áÌá?/tjPÙ#Q7™—•VŠµ¬k
/Þud"â8ÉHBê/ÀÃ‚ß¢G°N—ÿ¡ØÚà÷Öø3¼ Aìg4»gØmt/ÀÀK'ˆÄ—­Ûï{4›YŸEã@	%kàp¯K’J/%"ÃïÒV=fkû2×Ï0‘ïThX91xu†ÄVG ÏËÝ>eâõ5*òµŠöíñ>Z²­Ú|~ÌHãC,K÷³eïì…Æ†Gg0‘zXaú»øƒpÏf»o0“‘Ø ë¿æ¡3:@¸9[¶ÿø`Ãž”Ò:…Ó(õY1„ÖUÉ$[G.ÁÀ#Szî'ÒDÙ!]2ñéöm’ÇB¡éë¢®wKÉI±„PY
C4°È?3ø²—ÙšÈE‡ýÅ´ûì1lÃík n^.J '£ëËlÿÈ7VŠ>)Ñü	*ˆ;HðF¿²Ãžð«Cïvœ4÷“Àž'P`h>rñôû'<	ÓUa€Œ“œvŒ%Y3Ç«A0ÿ»
dCCO…áéÈ«„MÙ ­(Ÿ©kx(|êTwãNâÁ½£XE¾’(y@¨µ„Ð	¨Œ×D!ˆ(+ÈÜ  iÍ§™lFìŸÑÆ¡jLÈƒÆªæt4äe@Åc¶»„BŽƒCœ|©Ô#jo×u+„­,uj ÚmÙ¯)l5EW•ÝÜ¥E¹ä{;½ <"þÂfáàè¯ëêïÅ³a_×Eñ÷bTB¼âÙÅ÷`çÂÞ¿]A,ß;‰ÿ<#{0ƒ"ûŠçã¬øÜ¦n„s*}AÐñ¬²¯AzôôïMÆbÛÙs;“çãØ¢üýï®BÓVô]SÀŠÃvbµ#9Ê M²K¾áUyè}Ç6¨÷![ƒC¥QÞ^MÙÁmc
!•ÖèfECy PïkØª0ld÷àaÿØ½ñ'¿	ÎÔ¡FZÄðB?Ç˜Óà)Å§Í¯€GÕ>>Bè»áQhŽšÂ.ý7fS” ¼Êœ1ÏtæÔCP#¨×Æ¿ËšßPo­_-W'G°óª@pžr×¯|üø¸Yåt®×§¬Z•ÓžÍthjú¤ÊØ½“±ød× ‚“Ñº L@ÀìÃj5ë-„¢4y¼ù‘Ù÷ûî‘Q;‡ ãj´~ÿžoãjvÿËBf‹FF–%/ˆVŸ5ú/'TSó^÷X!?Å.òüÓ²ð¼|Ö•40ð;g:ç2yŸ-;ö5Æ3SÞ%¸™¾Ü#"üÄ–ãûb¾’Ça‚@&>íý otê|'êpUÊë”·½Ø¤½¾Ãÿ{}‡fIð	ìª´$@ì§ì†©èñ¾›á¨ñÂ‘¢ÄÛm‚üMàrRÎ¬^qbpž ¸ÔJ¯Öšœ!k˜qµª‰p81|f?Ø¾®çt¡3‡x^_40§ùd±šcÇ‚} qy×}6A^X¡±JîsmÆOF û"’:“¯ž34´ü;¨´›=¤XåsRGóWòHÜÍŽ†•x$\6¬Bájël"Ë€od´Æaä !&±³a.z5øš¡Þê…¦É'zzÞ”¡íÆòŠJŒ	|%³EH#N¼!¹r€D·kã9Ýt)!P)Œ½¦ó@]ËqÀY¨‰þp(×ÊÔSætîzá˜Ó":'<†'‘©†êƒ–¸‰©QnÂJ½žc §5®çùû&û'DÄçbýõéáw“=},tL8EÀ‹Ä„F_2©9c]ÁŸPö^Ó¡]ëgX{ÝoZÁ¿ÌÈ)ï
_˜¹öŽé0âýõ×DKœUOéùKlâ”IU%:‰a	…YÁÜ¤Q[>§QØ±ìºþGú4$ð›uSNäDßln³ý»>B÷˜™É>d½J	ÝT¡ÇöA©,j,Ñ)~meèÇ=ÙLÐnïÄ-;¾+·š›©DRè–µP}_lêPÍ†jtáæYŽMºs"Û%ÿÙVu¸Ê»Ññ«o©°Á%í¾`³H¬8m‚‘leeVý‹âZ›Dãkßq{jÒm%zS"¤×Upúv`ø¹ääA¯;›õbgØ™FºóJÌÿåî¼lG„Ð$Aä©dÄ¼Œ˜ïÁ}ºT××ßCe^ØÁ§îR`{ÜíÙ¿ÏÜ»¹¹Ácýõ%vkäX¤Ó²:ÓÓõè¼GôÔr¨Ct„0œJk‰\'íˆ1%y	ÒŸXêx6ôÑØžúw>%Íƒà9?ÿÔæl)ÑÓf­Ú½]=¯Öë‘5TÊÝ‡|´¾Å`yäáõÀJoúDý± q]<–[,)_<~EËéýêÿb:ˆuÒÁExðÝ f8pè¼ñ
aˆãý˜g<÷ä²º¼ð Aœ:(êŽ£GhwsïR ·‘µh)çˆçÐž]H›/Ïœ–¬ÂA/x0-Çœ[ÑÍŸF¤Cj¸$$8”dV‹¸Èf…·o¶èR("„œV19²V®¨’ó%F"XÉºìÖÄþ+qÝáçÁˆ¡ÑèäÕâ¢‚»½Õ²Ï•XñûÞÆcó¶:i‹…Îß"ñêÐ€Þ7Œs«LŸ#á+«êËö~)Nƒ'J”üº¥ÖH…8ˆ„½ì%[|Ÿb´¦>¹-ÎÁ Ö9|BÃ[âöŠÂàhÊó'Ž}mÃmUxª6y·+N!,¤>í[(š¾Ü×¤çiá×êDrÔíË¯ãÏÂk`)v‚Ù?¸£1V×o÷ó5F“>Ü¶Å{?…x•æŸC0/B3ÒýÄ]¬á;…3éH~»2kß(nŽ‹¬ç€C9	#4VúxyAÅbä†°ðŽŸ;†_î[/aáÚÝ¶ûjç~(º¹3eÑº–å=«•¡´»mC4õ˜6ðP¦¨-Gú°óîøyAFÈ¤óëIö¿_üuÈó¹ k±Ãû'GÊ·O²ÛuÎ–:4«zjãg—ô¨Œƒ`Dh]Ñ0nF„bÊêëgæœ†‘!Å°8Í÷´ßY±\”:ì<Fózwg’u¢¬lx'³&kô_æy¼YÐ°þ‘NØ’=­BT(™`-¢`§‘|/c¡®w5xÈ-w9cÂKŠð àJ‘)Ð@XvU³«ncÛ5u¾}Ù§ Vêd+4«ŸhÖ¿ÉMZ´Ls˜|15-Ntµ%¾zß«xCËbÔRÂè‰Ûìùý±<k"¡Î®Ç¶9“7–z½Pu°G‚Ýaè¼ºOAÇ	3HÑH¸°Ã„?÷’©¦“Rs¢Æ˜ý Q¯Ç ÙèøÖG¨%üÐŠ–¬C¥™’SÃ7‡'§“ÑÛï¾ŽÆ'çg“×‡§£¡âúäzãË·Pmƒj“×ç—ÇÃWUáeF%ÌÍË¹röû!oFÚMiK&‘i;‚#ï¡Y	»
´2|0J3ŠÙh8~{19=ÿîälBñÞÙ5(M¤V·ú“³óÑ_O.ì hÚer8h¡Mý;pZx.Náã‹r:•ÈyÓd§^ŠCCÏ}•íïeÏ½0ð^³s#	uÇÅ…³,Š™R$ÓqÜž%>«FÊUgðÚ°´Ä€§RF›}Tv¨(¹q=]äµÍ­‡Œ¦õ0¬±š[êe0½ŠÆWÁQ›(^•ÓB¥–0ÌïÃEõßåwdQ ù,oi©¬ ±à¿*˜Xsø‘ô.:ª¬ÄâUÕ§ÍJØ™a!:#J¡ž˜Æ«i\£Ü°²,¦—ÖéÔÆÑ†ƒÇ×›OpÎß®Š™?Þ¢Á&à&oëfOŸÊYø0_(XëUcÃ9´5M(¾HÔ’µ°ÓñÐˆìz¸éí™½ìŒ¹aÐ\û°Å¼l~(®xÊZ²>^7mµà™£œåü‘^úRæYÈSvÔÜû$?ªxÐêšˆûu0+›Õ<¿LçUSðTbèê¡k!à`‹`£š#è@Â	Ü«É"6¹b_SÑÄ¥¥÷Ò^×óÎX‘:’nCŽBì>mR·!ò­@“À;43‹È·ÌsçéSþ•Ãg…žàÌÝÒ)÷•ï"jhøyZÐÐ Í)¸ñãoŽGŒŒ*f¯	~ô,áÎåyë{Ïm3{‰q²äšB|Š$Ã°Zi¥vµcýžOÈ–Í˜È×#"Hh`‚D`ÙßÛC'¹$ˆøUÌd'@œãt'Ûè{¤w`=rhï¤÷*^û-`Ä‘Kƒ)UõÔPp’‚ÿ¡sØÖ’Ó‡È-ùºhÉ¬ÙeD²”9~ž,ïaï†¦hÞ>ÚN4çÖ¿/Ö¶ÙÜÖÞ&FðØ:èé€=ÔaÒÓâV8ƒüè
Àƒ=ã8¥Œ´E°x 0ˆè Nµõå?sì¶šyA@uD_Äž'ÿ¡ÁèÁ0 p _QÝ‰®)#Ê–†ê9úp}ýõ§¸Ïr]~Ž	ÏÀ—""§šŒw-Én÷æ„²¹ÂÍþÚ¾«o–šQ˜¤ 3N„í¾‡‹gëö¦ª!§Òª<°Ú²¥ÊÓÎŽ{pèiÖ?E|xÚÀ-».´Wñà¼òº®TH[dûªˆXœó¢-:6e/Ó‘v¼–ÜîìØòÆ=ìNLSF5ºò&ƒì»¡kÃ¾¶Í€Údc0]àj~¢§=ÑjvÚ“ø½FzŸ ãgÒz`Ÿ&ª"B Ù‚½H ­Xó(°xp÷¤u×NUÂ»H°“ð:ñûBð›^H"=„hÿíË9„á5#žsÞþo+¡ô8
ø GíM’Ýmˆ‚ÏÅÄn©	o|1Âõàç`ZÒïs[ƒH}zæp>Ç,>Îùgº8Oµ ¢~%‰ÖàÔºˆ Ãoÿ$H%¼âFÂÈ/lùéˆcM¼^ÇäµšD¹“¯ÇB*ìÐ%stþ3¯«.oØ ƒ•™Ïøçï*p:‰µvà—|Ä6Ú37ÿ:uþóü#¡z9+ä"¾[2¬ÅL&Ê†8}m†JxGõ{vë“‡N ±ÔÏÜ¯oÅ‰Wå>M¥‚luÍzî½d¤û‡¹Ãìu%C8ÂïOæz?à,¸Ã™ OtÙãWT&n#F(ùýšüoÀ¸Ã®Xó”èØQ²ž½©fdIÃ}¶”XÌ+¶ÜîÆÂ¾.çÅiõ~PôvÞ34`úJE€iÉ ð‹Ü%{	Ójþ9;ô;¼hÉR“=%Z¸ïÂ3FRÕB%&ƒCÑG=Ùyûø	ý h€õr?!((¾ðD%‡ÔqRG”‰"’kòÅã.®ààBÊŠým]¬®!	S©÷(¡­o=|0úÛiÙB°¥ü*'Ì2¼èŠ– á»YÌ2Ðqè‘#xDY¢ÅÓu[¼†P›;¯†§Ãñ0{}yþøUhSÁ·;D€µÅj©kv©òTùQÒÑJAIÑë¢.V^eÔ’%“Þ× ¯Cz³	Eûäl4¼g'gãsíìÝáéÛá¨÷rK„®?´øäfPÃ_•ËÙxÎohî÷)ŠI’iMÄýÿ  ÿÿì]KO1¾÷W¤9%"Z$Ž”"'”KQ{Na+Q"±AˆJùïØ¿Æ;Û›ÝU«Ö—$ŽÖÏŽ?=ÏbqãD"„™Pìïg·í$ïš–ÅXŽý©<”@AWq8» m ­=ÅÔ„	à„zýúÒ
Ì¿ƒ*€F–¿JðyYˆÎ¿¶ï?k`}tÐ¦XÞØi®æáµÔäšßBYš µ¾Uÿs(ÅtS=‹U•¾"Ó/˜~ýè^Ï÷ïòRI©Íòàüüöæ¾FT£0%ž.,w î™’Vû2t?Fßâ¢¼ð—º#­ÂŽY9Ù9P7R%
ÌØF'GÓ*°·yŠ!…“ñœé«E:±Æ¤RêôÀc~mkµu]`¸/qŸ:A:HA­0éScÿÒ8ÖƒIFË‡s=4)Ø3”]Ö¤W—¹Ô\lp~•/¢þb¶''T¼	Ûc%”Á“«w¨wù{¶Ë•GyÜï¥Ï t–-•I}VÆÂSˆw‘°Žÿ"ÒÂ M×„yÀfæÿ¤ìÅ®PÆ¯ËÍîY^>“ÞÖž|ØghË™®ím]‚Äœ¨ éùé©Ü¾âºñ	î´±ƒÒ•¦¡·“jˆ47é¶r^#ãû¯¿JÇ"ÌÏòÁù9zÃÇ#ÀçÞ"ãO)™™$`Ood(DA$*Ý”æèï›ÝÛöð»ÚïDUý²¶G¬W¨WË¢¯FÚ¼Õ©âi0bá’JÃB·]á±-% šJÞ=ó£Ù?.æ•:QÃÏ“+£¹£¬cbnñ4Â Úq£¡e1ˆ_%«³ƒHû!%Ñ•nf”8¾Ã×ølícÏ:¦$1}½€a¢SÖÓÑ9B’œÂš@àHk)U=ªí2ÆvÚ~}I¯ÄÞ^½óß6
ß_uÍ‘ Ÿ/:wZÙo„,è+¸ŽŒ!²`¢iB2”?×t{¼dN·8ïv[#²Þ]c¦=¨-òU³kÚ¡X]?mÌSÊósú}’ë¾×W¶g³,ÃyÀéÞ©¨åwä5“\xvÇˆš”5ÓŒÞC2jÔ}„h ÷ø?‡EcºÈ5Ê>,¥6Å`|øô  ÿÿ ÔÛ2_