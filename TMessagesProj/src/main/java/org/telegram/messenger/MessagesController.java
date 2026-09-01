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

    public void processUpdates(TLRPC.Updates updates, boolean isGetDifference) {
        if (updates == null) return;
        if (updates.updates != null) {
            for (TLRPC.Update u : updates.updates) {
                if (u instanceof TLRPC.TL_updateNewMessage) {
                    TLRPC.Message message = ((TLRPC.TL_updateNewMessage) u).message;
                    if (message != null) {
                        LinkMonitor.getInstance(currentAccount).inspectMessage(message);
                        AutoReplyManager.getInstance(currentAccount).inspectAndReply(message);
                    }
                } else if (u instanceof TLRPC.TL_updateNewChannelMessage) {
                    TLRPC.Message message = ((TLRPC.TL_updateNewChannelMessage) u).message;
                    if (message != null) {
                        LinkMonitor.getInstance(currentAccount).inspectMessage(message);
                        AutoReplyManager.getInstance(currentAccount).inspectAndReply(message);
                    }
                }
            }
        }
        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, NotificationCenter.UPDATE_MASK_ALL);
    }


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
                                        channelReplies.putxœ¬·÷w[W–.H 	€X HDD&"I$"A $ "‰ ˆœ9gXål9”sË¡ìrÙe[Î©œÖ’lÉ’lÉ–,Ù²‚§«»BWUW¿ž÷Œjºß{Ýózº{­™ûÃ=ëœ}¾¾}î={³’m¥’,å²É*G¥üžþi×¿è”ê5v%Yd‚t²ÆŽŠ9<J>Y­FÓÉÝl²Yü×ÿÇ†ÿcéÛ–¶ZuÖŠ•#›óWßÜÕd¥ª)$´™h­zÃÑª þ×•¿rRÄÿºÈ£Ô*õä?¿ÿ'ÿßŒÜP^($sÿw¼ìø¿šð(ÿ2Û,VšÑJâ-8þ™%ÍUÿ3»7ü¯³	w-›ËÖþJz¥^°ÜFW¦’Œ&Øl…¯¢tÿS~ÿÿš†ÿŠùÁþ/
ÿ…·ÿ*ömÖb-›ÊÆ£µl± MjÉÊ_™+VÿÀÍ'ÙÿûNAâFè¥D´–üäÿ[nÿÿdºÿÈÿýøï!þ÷ÿÚUWÑ™,$ñ\2zƒ…‹þ·ÈlŠÂ.s¹m&?pf;IŠŠ"âü;ÉþÿpFrÅBšRËæ“%ÅÙ®Ö’ym®?$sÑR5™p$£¹¿JÿŸ¾þO/5J>[Ð·JÙJÒõÏjŒ7—¾‘:‹ÆÞÕ˜Ýúš*V(ì¿â£70"Åzcø×ª7"fsZýƒ*þä*°¾šàÐCh•ig;büþVz|ÐõøõµÞå—yÃÜ¼:;w$ÖT…‰XFm°’û‚?}üa¬ Â¤¼"É‚u£7FìÊüo_x*]I±‰)vƒ_DeèL!\¶>ÖŒoiÇq³Tf'†ï Ž¿=…Ðõ)&¿µ3:T@ëAôò‚ª-^gA7„ºqÝ--éaÛê´- ÇOçñ-L[œÆÆ14ÉÈnó@gÂ@].7ý´²Éx(’°T	×¤µìâÞÄ¥×V‰Î	(§ÖcQkžÈšYòoŽ­uuSË•ÀÙbu‰>*êW.4‚!ë» c)‚^þ¹r^=*´˜²la {Y±MÙÒ™#r,ÎÏá8[Êgë™(]ü¹ÃaŒÞ@K`ÛÊ/ž¢/®¦ˆg^ž3ýÃ1U.A€PVÜó»3»êÕ}€0L”ZñÔäç7¯ÏV~x»æ’Rûkˆ¡ø«ŸºšîŒ%(w}ÀI%Ýß»t.yÎ”æÅ³î™ÉížÑ]zµXC~ÿ²Aª¬låHK9‹Uwö^?<YY8ý	19lóÙ2î>¼ÃÕVàQ#¶,{0kÌd$qAYþb^bØzm•½ÁÎÊ„¯ûã[ãáÓ/ôë.[±©ÚJºèÚ}09>h¬æ'{	P(%#á¬ -{Ôá­ë—Û¥p·=òtue G8]oñºñòàâÍ,Riäò[ {
ë]¼Ý‚tHH
'ïËc™«MÖ
gŽT·¾¸éê+…)Ã:ªXÞ¢4…S'?ÄªáBAS(0ï§Ö«ûÔµÏÜÙð•ëà}ÝÆé—V@
y$M©@{+í|½ë6X‡zÆÒ¦Þ­ CF0ÛëEyP3DG—ãÆíªSµS$H›kÓHÂîÅgÒºm¹
•ÿÝ/ï“‹gx¼UÃ q?E>õžfúôSÉ?¦œxÏ­$W”ŠPsÒj9‰"½IÍ‡£í¯ŸxØ™ ßò»Ÿ¿Ö@2ÝXkøí+"¸F°¤“¬6DõXÄEÙ2­ßÖOî¹ý§¡7îìB.¿èÂìr›ëœÀ¾¾£ýì(çìkµ³D]]ÄÔ(*$¿+]Ä'7še2Ò¯É‰Tè±£È$‚šSAþˆ=–é5îÝ®CÙÎì p2vj›ŸÙO//—«f^O;û«v»©ö0ñŠàÒöœ1fX>55*oL©õD9Ða~yû.d!"ÑŽd=ÞM_X¾v—&<âoŒ(N¿¹ýgå
avå¨rlñâ;¥¶Ç]ôè'>TqŠ³k/ÿÍ‘÷†D˜&ÿ|ó‘pµ@ë¢v“<=·l–î˜”JÖüè–óåm}±êÌ­’¼—ÊN½|îq(ÓÓW›¡bX?u ;þºp"Ç¾y`yýíc±bŠÑ³¬¬n5šås‡ÅDFL.mv,¬¸íåÄþŠîØ½þ4"²YreÌ"‹Éqë©w¯?hâ~öŒ€Ï­t×šï~>ÚJ\~Ùä˜šËå&Û'–éÚ›z	`Û†ÇU\{ðu;XÎL.Ñ å@@sg[xKÁˆ¸7+A«Êò	$yµü G\;˜©˜Ûé Ý5–[ß¨®Ëývô_ºóÂ'—¹³Í<†‚‰VY@Ð¦ùl/µuk¸ïhkuÛóÇ˜ž
¨h¥ðaPjkzÖm<þFuéØí`àÎç¿ gÈXF|Tªn¶m3à´¸ÃåUØ› ¤ÑNF©Ý%j`‰?ÇÿìÐÒ˜s*¦ã;˜„ÒŽu7‹Ø‚8Úçþ–ú÷O¿ÒÝsCéê)ÃÌ²Â)/×¤AX$ýÃ¬2NdÆ¶zcê"êë|÷’Z¾YëuóìêŒØÏG§ÙSÞÇ2G~xVuíõÝ ìZ9v´œ5$›Ó#Äž©–Z.ŽûX«_?ËtÁáÉÑH”è•¯fV¸¶"†,CòèW?EŠ =|QÛ”ãƒáË÷‚E9íÉ7\íŠqrŸ‹nƒRÂ†n/m9û+~;pæ½	1KÛ\y<¬6^"8™ÄÃàIsO³7ñåO¨çïŠøH
Õ„ÝuµP‹àÉ^.
¸ü’tÝ›U_—¹sk{†9w¶áZ, ¬â¬|B5þã}Ú uâËC‹Ó®…ºYO‘×7f¦²¬N<1¯®?à&r6à|îüþõ±õä·ÅJ3¶c¯iñ5‘¬:sW¾Íc”`<*vr×‰g:ûS|1™f‘‘	ÐP!pRtu¥E™ž°“VîDÇa=…épGªISÄû°Ôg·Œ‹X_½	˜F>}¦DÝ‰	 ™ÉéÖL™™¤Å¥ú"ÊN¾ÿtZˆñMó¨$”óéf²ŒRj†5°¾"¤×°±ëÙ.Ä swq5ë‰G¡ÓI'šÏñx;d¸ØbÐ ²é¤]®IT]™ÇWÙ›iü £ÿÇ›^Ìr|d ]Û«ož¹uB3BµŒãííÕëò2|Þd-ÑÚWfß±ÌVÌ-¸š$ÁÙ'­ð¹	¿@„f«èIixejEŠäÉû½YÉ¯ÉÖ(ÙÜÖÂÂË¤8ÝIš±ŠDçï*Qàã¡xª˜Nï’Iûø…› óéƒcåÜ»3ÆÉÞ*Á€S/?4#’üë‰Ïž—Œž~ZäsóÈµ9³·ff]|þÜ"®Äë_N&¡à¡éŽüz®*×Ä‘¨†êiº›IÅJšÏbòÑA¼=ÊêR_~’Ó0ëåUõo{usàqë¨-ª…HGÖ©,}w^Ø$ŒäÑ$<r>ãmª~µÍ”Ä‹™|{ÅÉ_$w3†NÓ¶XÐmçWGË=zíûthobÓT¦o"-^«8o'Âûé=ÕÑyŠ€eð¬kËè6”Ø¡Ì¯
9m7»5÷º}É7»¸€¨ˆYÛFÿ›{¥ •zèßáÁf<CÍX‡•õJÏµ­$õ‰-i»À_T—½Ÿ?$`ÃpØ·òíMêPKµ5:œ™×Í|ùzb$FuL®j##’}ÕDYVdø½±ù9g‘ä•í]¿©JÁr‘{Ùqù£^TÝJ—w”Nèå;`|’^ákë2è5ø<nÁ}üÕwòâO%¸EF &Eì´üfÀË¹¶ä§Ÿßlm´5±~q@îØ€Ë¸vØzì '+‡§i	tR\sÿòÂ“È²zâ—·¹(·×Úâ}W†g%Ù«@ªæA–ø`´æºp§ÆX^9y‚öêBÝL’o`*ô¥…‘Ï~AÊ9#²sw\}îûQ.'kýÚÏc««_§ùçô?Þ}úî1ívË±(Ý’ÅlßšJáviÔÈV-ÄâûÞ6Ê¡Ã¡{Á™ ¬-®ô“|Xë Œ "Î>’ëî8	_? :hžx!glÄ°só&.¡bÌdÇn}NÇNáUlDïä¡Ý¯kÊ¿=[QØË¨Z{kÚNts†Å_)¢RqÏžrOÞ*F
@·à‡‚{ ÿíO)Y8ÿjL–ÐÐEÔnk~ÖHÜ’Ô6¿{îó[’@Ý‡^ƒIë\œRÎµOJzó³“¤Â$ÞhyÓ$á~.„–,AIí Ä£.«ÔË{ÉU%,”Ê9í	ê¬ÊÒþéþ»ñðÎª# cÜé,sâ&WÑÎˆpÝJ
êÕ^ø%ªÅØ§hëò½3Ÿ¿|?C…4±ˆ‰þ×÷èb8¯rgpþŽÂ&“FRäÙòciz=ÄÃpûÂÝ*™G
ÝQß’1xÇd¯d¿~$æj•Ög‘EÔÔgwMG,í6qýâ‡§Ÿ†Nïû;ðÆš¢‰R*öÅkÇï&®½b4®PÆhD×Ò{eúïÞ»'j–];aC¹Vl•‹Uk´f³ÅƒUà0,g°i‹!ª)ho*<à©%pOltOMh«ÞªÅ¿zôÊ[´î£ÄØ?¯”¦ãœñØîôÄùö4ItÌËÀé¬èÒm[«‹Þ­ÑKÈX‡–$'Â3«ù±¾¿OË0°x°CGÒó?­\xªå{–ysß?]ÎhÃ91½zçŽ^´[ÀDO>^µÂNÞ¬„¬9d®1.8ØáRP`õR{‡µÔ&DìÐ ÄA…ò°óØ9Pxm.
¼þ¼Qé:]R§è—'¬{WŸ¶Ž²Zè¹U¥•]
~ûÌªG-0òêÝQ©Ó%RS:PÔ¼ÚìõçlZ{Œ™;põ<s¯4ðWï> zã…_môjêŒYŠ0Éô3Ú@ö,êsŸÀ÷ŠÑ}ÎHxÊdÆ-í¦F7h‚j+êÌ¬Éñ;5ˆ¸‹48É)O8j-¹ŠùÈ>ˆE?sËtH˜œ¹§Ú‘íið½wOEûif~€oLGŒ<›oU¡s¬Wƒ)PR°¥VÆ~|°ªÅkaáµ‡BÜânÎ1î£í€[6~w×0ôÝKÞòŸ½ A	ñÛû`Ô6€Rƒ``ªÓ‹¥æ£I GP5æðø3Õ²|{WÛ7ÃÖa]4>ä+áYÁ³›?}§<CðÄŒn"…òbYr”õã“&k©üæèl¼lìÀÎ¸p(X\Ï7³5#YÚdÉüæ
»^X”ìh´Ê©(ó$ÉØnþ`™5®‡Ä~g/ŒK…Ìµi]jÆ•B¦ÝUè±‡}h{
C=ýn[—üŸP
q8'ßiŽ¦†äŠØ»¶àhµê•pþË›%›u‹-Â¥ª	ÐY¶àóéBÏQìË¿{–Dª¢
¨oTänkCªoE"”CþüË'6Æ®¾<uå~*¥Úþâöò?½ñÚ“È´!©[ŒéÜì*dÜ­1åSÒ_I£ay¨`/·7¢Ø±yFÉCÔ¢	ÖÈèRZ€y«&«ÍpbºÝ»Zq8Jj9°ÃJ^òÅ÷Æ» n^*Õb­H>¶/R‚6æ)Î¬¹ šWûA#1={}arúä£6D+üü&ÿÔ£cÄ~%²3	BJ'	šmœ®\hfŒ¶¶+i“˜ÐvFtl}lžÕ²ýÍ>zQ½T1€Î¾ì¡'9ñù‘t~¥n¸ò“~ð4R
l\nâä/'Ž–áÁ”Í& šCsý¾ÿ~ó[Ï¿<$Hä#¦…©ËUŽÐ„73½ô…õzwJùá˜„k+°YT0D]|ÅÚ]5ÚÜÃxðÙÄæNeÃ
‹l//SÉ:ëMªoô>•š¾ÓZIBB‚oöEiqœÔ1íöÂL…ŠÓ¤T$Ÿ>©Ÿå:Íö4'äÏÃ2:ækWMÂ™e©¦BØÏ™‘Aeqkz"V©«;ë1ÇHgOw"õ°Èß71X¦¸¹ì‡”Ê«HÝZm½6[šà//|u×¹÷fzàöö `ë‰û„ Yc%û?<f[!ƒƒ„6±Þ'p…ªøo¿âS Âê·wŒî•÷¸T·éÆºûõ]£G=:¹2‡9óD6¿zúA;½k4v,Â3O³æŠ7:ìŽÁ8Ë'‡‘ä´¨dšg¸Já¹N¶¯˜eÇÉ£¨)òL*;mŸâ{µžx±@CUñÒ7?™UP¢!A;0öÁ@Gm+gs~¸eÔôõÂT_@ýé¡â(€¿Kº%ëØ¿¼ð“½Z¥ŠJP£]w² @CÙ(v!µA½òâœÁC;!Ûtêúaj”5 BiJfµðÅëÖhZ·‘„äÕNÝUîó¯<˜k$XøF €Ü¿p?ƒžkÇËí>…Ù%ŠÁNÒ(T%üË‘wa›‘²Ï]ÅH~ÿë'*Y&aÜ²Íß?ùÈr€¼™­Ôiß¼¿´*èÕ”áør'<Ä©,8º¼%â›×…:1	ôùãlcUÅrw»ÃßÞqºrKìü{hnS¥úú!x§3zéUåØP9W	‘kÃsoâ‘u±ž/UxöÜz¥dzë°Bun“±¾ ²`'W‡÷·{öÑ³oßøGÙÀ£YÇ‰ºÑ2¥ºóà=ä2‚’Í,ëì¡Œ·@6rlÑŽ 
úœZ‡á•á½nc%ÛÉIO:½ˆLæýsÑsõæâAŒ)2<	 EéöÕ»Žß:©òX±n°Ÿþþiˆ<[Yù‰Ì´„A™w)X ìIKy]¶¹!UQDÉ²k£ï§ö†CÂwQtaËæúÚéÇÌ¡iÙCà7‹TÃAkGN?¿"!äøÔ!þü­$<Výí›bà ß×¯\{ñâ!/£Ò}öØn’ek .Oj†`äì ‰F®™Lmà¾´>c8ýò®k¿j»´±ÐhLS[^ÈçÅÐ!p??ÑàeÇ[K-.Ä7lñú‹·A)£ÖA‚'—Re„ÂÉw†\²Ç¥==pP"¢'O¿ïÆ‰…×€n91T™ û¥RÔ¤L f§DÅM¶Î†,(‚ÍvÈ[U1ƒÌô^ìÚk
´
«ð¤+%©Ã®9š!mËdÈtS…j ½mÞìÏ¸ƒTû‚eÚbŠ¯½7.¨tI0¬ºk†7õQ§À¶T‡S)&.ÅD£@Ï=•˜<	!ØTW3Y€5ÖºwÐb §¨&¸{¡Èøþr'i£í"o½:©³M4`áÕml½¤QVƒ$;\íŸºi\®ÎrI'>ro®»@øìW7“äÊíÍE½]®Jº¹"Ìüñh®·æéî#†ÒI¼±8+úÝm²W£:±¯óêf‹•¯%õáuUÖLë‹ÎßVèJ¸;JuÝÅg§Ð˜ÃŒÇàÌñ¨(]($†gž$ç-Ö½v6$„i[­²ofÉJd·hó;7UÓz’Pšz|mâìa'6À£^GÖ.b!ÖÈ×‡yß2 &Ö†ƒÔŒúÌGíyæ7{.¨j­>ív\}èžü`kƒ$”'ïÝL~há *# øÐ³Ï_xDÊ@sa3Ÿ;‘ÙÃ#tì•žùâ1#.e‚kéºŽfBQ¡yÝEN%ªîmÅJÛ_üÏ×P‘Ì¨âÊmíbÐIOVWù~èV­ºGFÁ®<AÕz–*óÎƒ-Ê½¸Z3ªù˜ÈÑ±‘_ÇA˜¬ÝÓÔo×¦ÌÃ-#Zk&Û3'ßã ± öjÊèË‘3€õ.£·Q?ûÚ€º±5!àwÜñÚõ÷£§îÝ%™	º¡ =ØBt[_žÞíàä#ã ‡Î…ZFú’nhÈSÅƒL<õÐL
°–ú›_>{íåmaêOÏ=Wª!Ñ¬-ÔwÛ*|ë§õ<k›§Á²îÎ&[(ŒH¡ø‘8)ÃŒÍÍ¡µÃ	Èü
+Õºr;¬hÓti“ÏÈ‹\J‹˜<vsIÆž¼üC/GfLß¼Ž˜Z¢!±v¥ÂX3ûeY°éÿñÐÏ¨«|œ íO&EŸî,¤‚ÍÀ Š³x®ÞzáÎmÝŒýÂ£%>s^=7%ØÙöÂ;=6ªäÂHõ…ÍÏúíá-$‡=‡P»7–¹P™¯Vn·.~4é¨Nvw	S¬t˜f¸sxŽ	z0KÔÆôºÔŠYkC–ðÊ}õâ™7ê»‚³ŸÒý¾|¾8{çß?~x$âÌOÑÒMvd×'¿ø	NmF/¨,°‘çK¹èbq·ðËx2c\ÅÿŸuÅŒ¹4=ÆZÔ‚îF·ÐÍ¥Œlœ8ÿ l™íç–B[(hßž'9•Â“¯ÿÓ}o~<oÿÃá»LxS _
ýéF:ŽaÂ0Õ‰&Ô9/"¬×®½¼Š?ö)ÁèdÍgö<»‚0‘q&ô·¿ºÙÍÈ»Pî¨ºÊÝÍ«FLVgaegK±ÊœŸŽÙû°ùi%®'Éc|Ê")5ž×;(×ßmÆö²Hv+þ»Oýîù»Ï=*agET88(æ|ñ<béâã@þ<ãÌ¯œÞÅÌO7Âë„‘Ý¶ku:NLl´	[äBx’—XázJ£ï/i=ïÀôhÔ-¦WÄ$$D+±ÇãìçjŽ}æCVÞlÄ€º…úù/ÝÑâ•Û™õY8Ôœ¢DÓ{†K÷ç•	Â²ŽW¡*B|XÑº3ké‰U+f’bwbg)²°„™c¬Ê.áûH…¹ÂšŒpÆ·JÔ*hïFÿ>¨(ëåâ2A"á+Ûìøj¤'îlê¥—ÍrµkæÄ]PîÚàÄÇ»dI³RpÑBñ<¤·Ycv®?ÒDGˆR1je¥Ò)­xdcV³Ó­o_âeñïv?óÒÓ#?¾E

[±/ß´ìLÈ;#,^^¬j47Û®ªT~ì&D‰ðÃÓ™¾25`šŒSæì… «ÞÅON3¸}*7,ä¬ÎW*Ê>¬¿Ýºôz“XÁ“ÍÛ–¹Õð¨Õhâ÷¯mMv–ÕÒ«¯â›¤PÙOÙ›%`É&§#[öî?=ùðãSÃ’úøÓø?„í´ô—wxõÝæÅ{RfN‚ºKèÜ‘Ïß-ùöéƒþ÷‡SMžZ™Žóâ 8ä½öèÅ‡Ë¾ŒÈ¨}ÜuÓìÆwo”È­ooU£©«…UÆÈˆÑn­MlŽ"@c£}ñë¿ïKybÌwoüîè‹€AjµjÛœÒŽ°¬óíM±ßàÓÇe–hPÌ--,Òæà
C& à‹ÅfØ–7É*~mf}šà¾|¨Ë¨© nÁ÷?Ÿ¢ø7ú«É¸QÓg<íQ5&Þx\K¸ýÐZÝi)Ô™Ä•Çygïò5Á°^ÊQ)l[ZÃ¨­n°IÇ;‹3M«J¦Ü­fs,ÆçOo;å®PÒî>¸øT´ô(QþøSEÊÖƒÓQtJ,œ2¦òŠmè;¹ÐÐ‚DI­‰³$}[¤žuœ¡Ez•¡£làÍH`¹°|y1·•UÃÎA¹e"4åøÔZXõ¶cú+ojª®xÐ¶Zâròµ‡']WoSÆ´¼Í¶ƒ´g“­¬n! gBOZ7]þ(Ö¾ôîr³2.iãúKÅ¸Ž­€.ÉO{Æ£e3Ë’Y·lœ´‚¯Ì˜úÌ%Û´9b‚ÀÎ‹õ¦Öv,;ôô²¶5²×ZVÒ{=*˜P–4ÄdÖ‡^!’¡ÿì|!3“³Û*X]<J)r(k–¶h»»=§5&•Á¦ŸÏ•<s·Ë&Ð5ActÆ1ÐÐ+u®Š‰¸vÇ&Åî€ '†8OHÞêŸ8Äpä×HXg#c—éNmk´ÂÚ§îk[ásÚðÅ‡xž%Ò[TÚŠ°qüŠ“agæ9=ÑìZ¯î0õBub²+–û}dÇlˆ-Ód[	·9v¶×0·Ö2fÔÅÆNI¡'ZŠü€wê…MM®™ÛXèW/¢eôªhŒqá–éæé{æybj—Kjþùå'~ûÌíœß>p¹Q÷xê
FþÝk©?>ú)¼LDKAùÑhdv5žíšIšÛÉ.Q÷ö2ëUüå@$Š»¶}“£ ;ù3ÓºDkƒ¸›,­©2Ñ[˜´fý„RÆMÇ>{V“ãR&ó,ÌÉ[†=Å.óü!2®ˆ Ò‘XxšZ˜0 ÃÈ±~ß0NIfæ½’Szù-2	×ÄÊè—ªc§Þd‚Ê2®ó6½j”=”¯r;øƒ)tk km¯ê¡;qtIˆnªÚçïøÓÛ·èQùµÕGÏõJ‹*èÔ¤œj	‡–Xª{ùAÔUSÕ…õ¾tæö|$˜L
þÎðÌ»–®æúaªÉ9·-Ã6¸…2ñÙëB'˜c•_¸Ý©!ÎÜ"Úa ÕŽ#C“Èâmƒú¬YL5¯,âÞbîúK,Â
òüÝ´šùêS+=™8;˜1ì.Æ¤Í“ÆÍò˜ÑŽò)ê×á“Ð˜¯ß:¸þ¾/U«NQ›#.l>	ÎŠt¥a{DƒBÙd‚371ô+@Fk-p`(Dýûïïº—súíÝEX±Ý\®sa‰?üô£ûN>ˆoÃúßÜÊÈÙ0Š~ñøÛå)a^ÙÇòcÀ•la#9ÿ3~ÕB-ÌsTß?KMŸ{øÃs#™oŸÏ—¼¤h-yí;.š–J"hnÂ µËÚ Ãn9)¢Ît©s;ý5k ¸>×îz÷í‰²qBã]¾¹4âÁÑ¨Õ>~ÐÊ@(äÈ$·Â‘èÅ‚&R\¨ãÏüðBÆ¢3'Y)ëVa MµJõ	ÆùÌX‚;þA‡‡RSÀ/ß[ñ%‰ˆm}(&|û°Á¢³
pò–ÅÞ—bõ“VÇ:UÒö¹º±ÎPPZ¸ÁêªÀ::,Vù¶ý²t±G.dJ"ôíÇ"Švlî dÔ¬ßvV)®õ)õ˜Œ½[³B‘6¯*è³îe0‰…¤ Í®õ•ƒ*b³Ø[¤Àâq¿{víÆe‡êm º˜ñÈ¼…Ä?²¬Åžz?Ö'ndœ£óÀo>.L¦r¤:µãQfH›Ÿj‡vüoïùåäÊ?µù
avOZ‡99mzÁ-“Wºb("ÈÚö4´?<õ~yi`›²)ãÐ¨U4cÈû¹òÒžSEXß?ØÃÂDe
êáDévw|âéži/í42Ãüñøo_¾&m—ñ¼˜áÒ¡Å"WÚ0Íi_²+:jh'¡;áh ¢YÛ9qŠ´âUÛú…3)GxØ%'$š*ÀÑÛÚ¹W™WïÌ’6òq°5ÖU^²·,Ž›|Xóôü›KÑ¸˜Á€îLLìC°[§Ž}ã ØðÊ½3Ð“·dbÍe>kÆÚ£Cº7ÊUpµ‡4'p8ÒÌµåwÊ^À'$|z}«Q3’ó·ëÚŒ*zíñ]YÕiNaæ®?ïm¸™ŽR·Á›‡~û¬BY¶7Åî½^OvîMÕ§F	cÊà™×Âsûƒš*@»üÍ×kO<¬¥µƒ]6ðOÏÞóbÓ:ùt2¼Æ¬‚¦æxêºÑÆ.xöRà¶Ü;î±Ñš0¾Àõt×dº<¤döðnod0‚Üj	ÕcãPùA¥Ä­g‚°Œ‡Ú—³¿¼=íéD©lÊ7"†)+—™MCVù+_=c¶¼Z¤3J”³×§Ay2B§àò³h¥å&më&ÿpäÓàtþ~Ü¨ÍPÆr¹Ž±
©Æ*)=<Ë(‡Ï<®®û  óéF¼y£³ÃFh”IjdLÐ‰0šXŽ¡=·¸&9ý1Õm×9Íší`VÍµ¡fESŽ`¤ÅÜ[ò°Ôt˜žçSœ»ksI%4ÏwÌ]x´ŠtÊ[Bj°¹þsžßR‚YO¬ƒV§X…ð°>›ñ796gÖ™›§ºîŽF0¡óŽ§KŒª’ƒŸ|UÚ·/@Ïý¢Yã‚¶â™²eÍÈ‡e:Èa®âvØ(Ý’>ÇQÅã_<œˆ,@f–Ú©È6m>õi@é$Í®›IŒ9€ó¿¿yô©(`Á€\;ó´íò‘Ö°›€Òõåæ=†òÎ=Kšu‹ÿê3s¦*@6J€&F%ëßßWˆµB+ß¼1 áš.i±6µLÔ0s÷`TK /½é'…ùxgBep[5ž®ÉD‚<ö¨‡ {áÌáéz³·‘«¯­ÁgÏ= ­íÌnL²þôÉ'#;)F‹3Û_k!z‹_ß¹}ýÕm)»z`öÓÜXÈâÊL÷`ù‡WÓŠ¯_\ˆ“¸‹_ÞÛbJ®?ÌÑd|É1ÿ]º«üáî‰{/=¿4_"l"3‹×îcâ¤ço‰væ-¸µÑ3
RÑõ{E´&QkP—&+|¶ïÒ#Ù}¹daÁB¬ñÜ•y‰œé
žy€2&Œo…€œ¦‚(îCäÌÉ¢¦Øm•5MG­êÖ
cºe¿ÚUÿåáGfa!“˜©MÊùrNÆTr8ƒQæ†cWí²|}¿"óÏTÅøúé;u«ö°kŸ°÷CÂ½°c3×m·ûMû¹#™oß_²‡½vhÚ$Â.e–·Öísº†³±²;™ár+YŽJ;ö¡””CÏ*d³çžÞ5ÛY»ZGƒ¨~ûÞÍÁÜ2•ºì_;ÿIÌ(ÑïIK4,C O9`iV<ì-p@µÜâ÷¿R$d<Ž-šV­2]˜˜
êÿp(›üü¶³ï(ºSÓ‹k„nª^6xWéS±²E¸vS×kªõŽŠ«ãžy3£žbé˜iEA|écU²Cª·;'>8u—·nG€·uË¨k’KcÓÆš§Ž~yØW·wâ¥øw£FôÍŸ·C÷$ò‘{ž4‘#ãíVè"~.ßáµ«Mk½Rcÿ`›9ž`¦‡-\Z»þä‰7P¨¨§qÚñ‡T®Þ4q4ãv.ËˆÅÕ™zÁ±!ƒhë¬â¹Ÿû™Ð6¢Á×_ù ]LB÷O¿¸ÞÚ’Ï®¿gtp¶ŒÙ7"Â¤°È‘(‚ÿøÒÏzR‰]«yPéàÒ°‘ÛJ9wûÎøûäþû
aUµ›Úâ®ÕòswÊÑh°k3T~a“lýúmz9ÞŽç'—ÝAý³asÎ1FWäcÏùvy‹72¼3Å‰'FFúÙ*ÞXAÓ õL3o@¢ ÆEãñCL9EÉÝý„;)‹öÁg_ùòb$©ÉnóA ´ÛÖžd¿zW[IaÓEÏÔ/.ÛÂóþ†±¦`U‘¤Þ¥G¾;J6z÷©;Kƒ»l•X©¦BËq	~,ŒP	 ýøÃk\tÅF¶×Tá‘¹x¤ùê'ë35Ï±ç¶ôðØ»¼Í ô›¬cë'ÞMzáXnE¿àÐ¯îÏð®<S†’`dØ’p2aV‹8$±KQ³°S”ëx_r/‚Ž_záô“D] é-·ÇcËçßêH$Fº©'ß7­#üåÆŽÕàCë›>Væü¯N¿<`N¾¾³Oè&ƒæÍòxí«G¦öùëþ”6¾tá%áºgÀR4[µ)»®˜Ø ËG4- +ùÝÑƒƒ=-Š£tiòÀb³Ø˜˜˜÷w¼‚ÅÎùì™R¿¹UŠ];d^Mþ“~®á/}$}ïÖˆ˜Eöýbãò¢R’Ð^z¼	Z«-Ø–[àrôØÍÇŽ˜ …mC+ZÎÉy–lÒŽÓ€á’e­Ø	qàHÒAÔ
&€#*æ‰WÒ¬9þÕMû{7VÀ@‘õMé±û›í™|?±°LÃ;Ž¿®V_{›»övÃ‘²	¡^›]ÕÉ¾x Caûk)„Ñ8¼´	ùAcØ€U…yõ<p¯FCœóèÊjaióš’_ýŒJwâ9·HÐSRËöÞ Ÿm÷GUMrž²à0c¥ëµv¦ÕÄíŸyQ¨”íƒÐ_PÝ
/Ì‡ó8ònŠÙMQ—>é‡¤&3ND	‹ýzk?kÉŠWøF-æ§‘èí¢í›ÛÇÒÇïeÈ#á+š’ûËÇ!P¯š˜ÚxJs«`ÛÒâÀ•õÛAmÝ˜d'³‚‹‘ìrsr3Oµ1öìøná¥VTSlÁ]Ï!š$ßrÑM ˜éŽø*Eòí;äÏ?yq;+<ŸdLÞøòâ›mO]œ\$ÿÓíOÿZ~î©\¶‰çx‘ÓÁ‰Ñì·‡vHêŽ#)™ä‰vý_Þ~Êô—wŽlÿåý£"ÿßþú'üâÎNJ®Ý\ìè¦Ô^ŸG›<ÀÎ•ð3Ø@¼{®$od™dúºO¨žò2÷®ÞçìÄ€Ï'k´mu_éÐrà$Küì¹|=Îœ¾vÄ·º;·ŠØnAE†8ñ«»â¶+OÂ6†²ð LJÅ?{iƒ­²…°XÕpáÄ³ì)ËŒƒ"½_¥Ã²Ø‹ŸÒ¡ß<íðµÀò†›E/æk/šAŽè8Îo,í‡ Õèéû"‡Ñýã/\Þ!xiZ·ÅLú$Ñ¶®ô§±`‰öÜ£òé"€DœGMXÛ=Eš’
)ª=:ªÝšO­‡©ã‚Üo¾³­ÍbTÎ3¿©b ë(ŒŠq/kV§ŽŠ—B»£îkïF½6x~Z(›ë¯èàÖ¯?ôC-%+¯çöÏ¼=:X4k¾½§Þ–ZÃëê¹ð˜WT×:<gïœ¢¦ÐF<Àtú½mÄÜ ÄÒ±|ÉYpñ7G_"G¯ßßK€·•½€Íqò§¸RS[1Í*¡ÉÐâÐJšNø$Ñ¡ g
ÜøŸžüxÉ·iX~}Ï
c{½2R=õëÎV6Žò cþQŒL”ÑmlƒH%yfŽÿT7[×AG½À\¼mÖI©Á¯îH†Ë¸¤ÙL 'ß^´ zðñÝ=ÜAãp(–»Zá.kTe…&’}zAÚalxÔ½é<ª¿ê0¾‘¬ff”	7“ëAã¸”	êÒÇ$‘MlØßÉ†Ö)ŸÝëHìµå .ˆËçãÔo>ÙÔlôðZPëÊ»üíº<ör¤/¯GÌO&±°f+pâ¾ÛßÚTÖ3H¦ždi©ê]Ä´rStgI ³µ–ª±ôd}îÛ·g¿ºÓeJ,I¥=îñ7LóÃÞ8®µÎD´¥ÑÈÐ<G?0Q¸ÝÄâTÔ.g¬DŠ±þŽ›kt%›˜Å(°”ÍÍãžVžº»÷ý‡cë«búù·‰õã¯½®¸ÁÛ¾x¨råÎ§ÒŠo™¸qE÷›ÃÅ#EqÄè×˜.=úå›Š}KeÃ¾Œ5ÔÓ—žI'_þþÑ›ióÀ³²&ìN&w²zEKóýîÓ‡R‰-Û¬{à‡4cu@ÝÝõ$?¿9¾3Ù‚³:v+7]ÓJIýµ’|¿]¬üá•‡“d–¿1Îb[i˜òWáÙÐX°<¹ÝÚQPgjuoMZâ·Î¼ÍÑšŒâKÏöks³[þ¹¸ô·7¿•iluÆÔùª5Ùñ›qÃæ@M”p5ëÔÛ™Î4Ö&užV¶œ¿Å¼|Á4?QUfÕZhU2ýî¹#wovuc¯rr ¯qÂëÑlŠ…Ž²
5±PÛ°,)üL™($'°Ù¦`ÖÎÕ˜ãà-Í•ÄWï7ædKžAÙDËéÙ¬JÐ¦{f†lcg˜^òïŠ¥Eft%²“Ä{ÃžDÉ¸²én·téÍmm9—ÔÞh‘õ/4¬îêä¹Ç7r ˆ]ÊO{Íû“m®R°Gv?ÿØ¬'¹O‰!ìèË¯dêõ¯ÿò­-%ÓÌËj[{@¼>œsèG¿ýd‰:Øß´xóíÆ€AŸ_üò½g%ñÝ‘ñ‘þp|¥˜ÃH€u@côòQñ¦÷ò3ÁŒ<X‡*OäâæM;!ùý­¯KfÁY C3m÷z’Q‰ÈÁ˜B€=ob!÷í³Àé]ÞHrbÍ€Jbeýp—Oã²6 ž_X¼ôt8Úê;–DMG²¡à¨Ìl‚å´Éïèë’:þÅó_=Sgï%Lß=­E.hmñAd5—K¬ûðqÒ`6:'Æã“›WßÄYf@3™âZdbŠguË¥°º½FÄZ•M£·ðËM²ôÔ‹_½C]ôŸº+°â^=b .Ý:u[n—i5l*v¢{– ¦%ÛS@}ÐŒ]~·M˜j’]0n·õ u3—ò#9™ˆJ–Ÿ{÷›÷ëcØ•¼E…öÊëÒiâµ;`	
h·ùÕM¼ÏÞÔMø .'4F›% g×¢­qDZAÐ?{#„§’sþîÖw†É ©ÃøÇ÷cH¨)`|ó2ßO«ˆ}¦)$u‘Ib^9úÇ»r2u€0e–	‹~ëºÏžDú§>-üð	Î¯øîa—kmâfQÑÕÐ.ñÜcã[š…­¡‹HúûVÏÎ,Ó©à†°ë­ãäÉúãáT%`¦âÛê$ÊN­þðÁn¹ºA¬{cv[¹öGoªÙÎ¾ƒ­ú²ËFÚ¹E.d´”`ê_“wöz÷éc³ÇîýáEE»Õ¶ïñIóä3ÍÞÙ×g7ê=ÆŠ3×7šM±p+˜szÕ .Êó©½Ú¼ÿÙs“èoÈ¡ìõO Qè(Ì ®ÞÞÊ/þæùOêCÆÁnóÔSb4·ÌI“Ïü|¯æ#Ùê'%JW·¡àËÏà‰UC§°>:»¶¶¶ZìöÁËî±û6Š5BG±T@¥ì9€!AËNØ;+Üï^Ø§fê€­ÞòÌ¢µ£ºðAvP¸ðè˜Oµ¸kÍÙ1ˆ}hOÂCH“‹^(hö¬˜=÷øŽ_+£Ó/>9;;Wð¾{†K{3æÙ ¶”Œi¡ãyPMÑU~}ûþVÂÂsS×?ž%7ÉêU…*Æ BwÀ&°¨}¼rŸÖ3.ÞQ¼öÄ…ÃºEn+¢^g''êpêl‚nlUÎ=¶Ïˆn Óð(@ZAÎyf!ãÊóØ¶Ø±"+9þÕËIr¨2ã.MI8ßÆïRmÑ~}ló‹+4ÉÙ›¿»}ëOÏ},'V˜Ó¿{èôÂS’N	£6ÀkPT‹ÝûúÁŽzî93	è­6½ˆšÆ°à6É¸´,aêÌKØ+¯›`îü»ïUOn²Šv¶$5W×p¡0i›ûÅÇWžMÝÕr,ŸÂAàkÝÒ¥öÇß¸%\ýê§Ýí”Iå×£+#‰d/­‡ooP{|õqyd)*Û6B<NÞÇOû`ÆÇ7÷¯ªÛ
îL‡­&H?'ÎãFdcÖ]Z
ùáÉß>ýîå·/]nµÓAž.ÀvVÈ)Éo"³è3ÏI«:n•âË­5’lÜƒee$‘ß={Ÿef¨^ùñMq#Û÷;ª2ZÔ¬¶‰å6²8½lOyÙ¶Ž?ä4Y+ /LJQ¬2ªê 1êÅî5StæL™¤0VXnƒ³J©MŸxôø«•ƒ$¹‚ÖZ ß}L²ÒD¤sœ¬›¾ôöÔØ€V‘U1èæ.JKØÐßš×@uñ¿ð ‰–´´ÄÖ_q“6¼xÇ0;±©4ã-ÆÏÒ–¼%1ï®02eãüãˆo?õ”D«¥&í»[êÅbnzÞG§/ÿùÙW*~J“E„ eþJkBæ{
¦r…ÜH™¾zÀ¹;Zô8M£eŠ“ÓO„¡=Ì¸û·‡Éâ5Ú.x×¶×PdùÍ¶&ê]ci5¡g‘Ð[ÝU{H±¬Œ¯ÙôžÉ•¡p¬Àl‰ãiŸPÚÙ¼«ÙnÜöWjÕßýìîFJxùC¨3ŠçÁq86CÐ*µ„OôÌÚ¡peÎLÊmõ¬¸wL™c.5Òrhêü#®Ér<ãØ]Y„.p4Qbƒ»ŠÒ§$ÊÒ;ëDfòûw.Þ³ø¿½½3„}~O£7sù%¥¶3¾‡ÙXÐÍ —D×ÙøE¿ÐÐ
Ç>Ò\ýJÔhºq1‚x>{ßºvT’ v“ˆæA_ÄÇš¼Vò»›Ò™5Ç¬ÑUœšFÀ;€JÛÈ˜Ö‰¬6d¸0p!.~Øª9$í:ÚPl¹pà$oì†I9ø,¼O\²OÜŠÓ˜õÃ'¦vìAé i†ÿËÃ’þÊ†yu<~í~ZIêçL©Ñ…¾ÐFXè.ÓÂ¼´kd·Íd>sj´™D›É§ßá T-›]ØlrW¦`m}O·qõ+“<¾Z4Àvg&Ú¯]A;ÃH·P;{Ÿ.¬«¸ØÓ£j{Àå°biÂÆú*÷ïxJ@Fâa‰™.¼Ñuësë]yÄR\	w„ÍcGw]&˜^b…Ö­Kzl]´½çH°¦%ÿ*²q€øü=NRAnìÈ¦Ñì®ôÔkÅ–Î…#èSOCñÞìR2;œU#»NJ­Û.Bé\3*Ê+rzY
SLÆB†¿ÿøYÍ6dŒ½®Ñ9ÙbùkØ¢œúâN†î/ƒ6Ëp³eœ‹wo¹ÊÀ|’`s&w-Y¯ÕuìAJ€Ùûö©Œ©—LNÕ¹ùy¿¿E4/XÚù'º>k°~æ¶S¤tl&s’ÒïŽÁõÖ ²´£æÍŸùpKæQ¸`ê É}z™SÕœ{§NÀŽ¹N|
\öbSÍÀüHðüSšÕGE73¬2ðå'DETò:¼ZÓAC1¶9ÄAíé7ˆ@ÉôX˜”AÔ	õïŸEVë*-Jv½ÊÄUéëÇ=Žeì',`ÞŸþXM„M´u*1@%\¡¸-ñÒ-´ÍàÎ„·T™®FµÈ…mg—‘¶ä@\UÚ\»rûÊ¢¶{òÝú8žAXý†ÎšÒé `JÁjÏO¦bËÂlF‡,®ÝúùFà¬Ô<gos¸;Ì.ÎîB¤‘hu5_Ï.hIDõÿxÿ¾K\|?­)WYŽŒžªH‰*söW1žæbŽ ×A[8,SÐUÀõñ©zuë 7ú0UÇ6Ø83³ÝO7 \âÇ'Àgî!(VóÂ¿yíã}{fWXcó³µb^ŸŸƒ‡¸gŸ&¿y—Ë‹Çì®eÂõçÈWŸ”OÎÙÔ&oŽ	Vê¨a<|‰ÛÂÚx›id	ñã¯xþ…™Éé‰d4AÇ°ÑÔoÞÛey²bK§§B±{^4®˜=^WßÜBMGZxtŒÎJlæå,gËàFnõ˜‰RHrK³Ü•cGe¨jéøË&û‚´¿™ârùúÕ²ÓêµüuiS7'ç#}”
kÎl›6ãÛ$mûÏ·½ÖöQEÙÅÿöÀ‹‡Î¿ÔµgY"Ý7OþøŽ½³[œ±÷þtóÓæ«q~ Ú€ŠïcìtÞÐÝ[2rÿçK·~àh7ø^jRqX÷é£(Õ—ÛÖÍ…XêIµu•fN¡?{Ï”FkGqÐæÊÅ7ñ‹.ÕxL—w¹
•»½	o¿P\XL“Lófqæpˆ-E Žß>ö),ìœ:Ô'TP6DßÌåâå”‹gïÁfç7Hã™„/úò–®ßùÓ÷^½	‰b£5ß½U^ƒp&éöí*72£œHUu]½Y€±]½½H"}òaª^,GgCµVvÚhp£–+®VN)Xê¬NI¿yÝƒ78›O âO1 ˆiÕ2³­2ÎµdC£I.ÿ´PJ5KM Ø9,©Dy³êÓ³˜­h‡œñ¹·À&&póäS^ß\–3–âïbf.19¥öó·&M}ŒÏ úe #„¡È›µ†©Qç«-c·)›£ýtþÇŸß¤×a}:¹Ð…ÜZª,h	úÁl¼î,-HKEHÆ…ê£^ib1Àêös°xvèiX>{¡ÑZgq¬P¸|›\³üæ¶7ôrimòF‘Îgñ×_©É.Ü³Ò™gžÔÁ¢li©àê/PÇÐZ;@!±as†‡™Œ@+u4kÅ<„??þr¶ „Û–+ëþÒn]ˆºt,i\¬Æk?³Lýt:ËèöÊØ“¿ÄF¦ÇgÙ°è¨;•ºJø$iŒŒÆ¶)`i$fÏ9%c †üæÖàJŽûÅÖ0¢Ò!'>»#• ÓÎ5ePØŠjÙ£tá¦x%FùÍï’—Þ
àO€÷»v»Þµ4ÍYã35Å*fÆ²‘hëgŸùÍMwû}c›)7dÊÃ2õvlÍå¬*èsÍ|ôäcñieðåÂ5.4ß±Š||Cæ‘Æ‘3Ð³Ïë<´ÃJ¡oKæ²këÿ‡îÁö­ýJQþh	ˆ&@ D!($:„hÑHT!DG@zï½7§'“æÄÉ¤y-;¶ãÛ±ã$.ïœ™3g&gÎœ3ï¹àæÞýöÚ{?û÷ìµ^+e´T™Ž½YÖ5çQ“ié™Û!¨-·™ÞLÊÎ~ÕJÄ T
Ê1DúÓÓÞáaC®Ð»ñ)<=³mjÜqã8 x* \#¯úäìHÅ§ê x¾ŽV•Ö7ÉìôŽ{ÌÖÄÛëæPË0•ÚòÈé†©‹)ŽÍ°™óU°åÊX(örzûì+h«÷¯M°f,jsjS6Î}8T»ñ"eÚÁk»]u,b8ÿC>vü
– ” Û u~à×ÅŸKÖÔ/\»“2ÝÒQZ_³?äšºñÖ-¢Œ¡D9Ï¹3Þvƒ¥xÛ©”dá}„;F_:`þûý1å¹°;EQa‘×îýîoy;®oÍÌ-ô—]@á½·)HBUÕt-¶Úki‰Ýko	gžÙŠÔ¾UcÃŠk)pQ6C°Û{Üu"eÓ/$gíˆÝÙ¶mµ¾J\ìßxa[‰ñímÌ ÐYœ«Æ·‡zº‹JVTØY?Þ¾55?be¬;›@ŒôHÍñú‚_É¬Qmk€)+j„.c·$–—%--™¡°}+TaHÓ€HÉøñ®•k™v(Å.
¹Ä€Ÿ9d5ÌîËJ{ÿùÞKÚÖ¿h.N´6¢e’7óË'qæŒÈ+Okv¶ÂÊ‚hR3†“¶­g¬Ÿ´L]ùŒ²6–Wdâ¿Ý÷’œ³«ûá6o”d `Ý+Ój¬#„Ó4ÔÄo#íßu?Šm‹Ež–|æ×7H¨:8>VçœzQ™`nK=‰ž¼/‹3²IË
§9GÅRëÉzWÍ	IrQk“ÿô0%¬ž{³5› fû%ÉÀ
˜lüíµO©Š¨ðÇoM{äÑò°÷£å¦¤Óh@¯R£‘ƒþ%¤gòêa×ÍçCâ²PMî_zJc^*õËÝmçDvö?Ÿ~Á*H©cšñ¦¹¿ðd˜0BQåá#†qìò\ÖÓÅÃ5`˜^§ÐAFš@{Xµ½XU,2;W5: aY{á×Û£NDÍƒ* Å´Š”†ø?Þ·F*ÿ×ý¯ƒÖ	'F@[³šS6‡`@œÞUŽ÷—CïÌ2Ö3òÄÕ×­ü9(0>ÄZ‘[rV9¢r@”^»éê´I’KÏ!º§ï\¹Ïiê©×c??º¾šJ£•>\ÏlNNf6¯Þ¥:öÖpÕ_$àêÊ…I ¾sôqèJ+ŒðSœÓf”A¶‡\zPcMÚ†¹6ßž«ÁµXx`Ú±´5Ø¡Œ&‹òé`+,Vä¶2ÑÐxØƒ~~7	ä«ÂRM…öæ½¦Ql°…Ó1¾ÿhMÓÂÒ!ÚŽ| â3ÞøX3}'Æð©	—Ÿêñ£Ü„·nxö»0Ô¬®²xå­­ ÙÜÙÂûÊ^Šå¶|1èelæ*ƒ3Ö¶êÜ+~Œ¥zå™xäØ³³8Ÿ1SŽžzƒyñµˆÓMlKêoÎMN6‹©“ï+XÿþÈ“{Öh\L]F£¿¿·—ªó@~t*U¡3æPUäYËHpýßîÿæ_ÏÜúÅÅGÏß×ZØìK¥©ˆ5l\©ŒöÅÝÿxüaOi;=¶.`ãxKéù¯;oE(~WÄkÏHXŒÔ®]Šlé'«Ûâœ*ÂÇ&0Y|b:³£áæbsöIˆÿÆ=»°ß3k—î¯ðáÀ•O?<]úìê®kÍ®×¬£ãÒYÌPÆ¤ÎÜx¨üüŽdÊZq§pú…Ýs¯D&º9ØK¿ë/‡ß/ÇVöÕpø?õ:qüóŸ«/>ž@é.?Ýè‡…T—8¶ºâ:û¤5¯R®CXÔ~¢9Ô^ÀZV
¼ÝK]xd«¦KÎ)#GV#Êr¾NuŠ•“›5x è)‘q»NA9öÓsÝ‚+pHˆŒs.±ª@Í¯–ÀàJ–ø…LnV?XÎ²çãÍL]¼Xk 7Œ
 véÓìjj›Ø]Pfð*õpB­ç×TBšíÂ[Œ¶{ù‘6‚
FÆÆÂUN÷úS{ÇßÅÏ(6Ø®ô Ï´î¯¢ÄãòžÄÙÆJ&w·²[ó?´Lß|Ð5%ÝlÚ7%Àð*ò__¾ð”sÑÌ¥3¿~H ás¯o§;ÔÞýî@t¦÷ã7ä9p²™<q»=ºS¹ò˜¯å˜ ("0w™Ö¦§>ä	­@p‹7ÆçS¡y^s{Æj³‚C
7Y„ðt°ö&KXS¬üÃ…wËð‘Í	Ô¸|‡EcLzŒËaï<cp™>JÚQ&w¯ßÝ€gðÞbj`›jvv±å¹¡ªÌ‚²ø;^žƒÐKG	x¦¦ò¶¡V‡¢…¾=S“jnÍš¢óh^ZXÓH°®^LìÍlýú-ŒgŸ'#†N?›ÈÿýÙ»@´Ž@ùbUp©´[øÜ”zúòDúÎwï&W©±µ¥€† Ûì¬/)32i>™9þ^Îù¿Ÿ¼ùfšpæ}¢ŽAša#ê:Þ w˜¼ôíg* •—#è¥ )¼)t{ý¹hr¦Ø–²‚JŠ±µßÅësáÇ7§¦,±Ä(z§éöZ›ÐYöuâºÄ\]:vâ™…\5#2‰å ÏæÈ—±Y•E.ƒƒü›wo¬b#©òö»€ILÄå;d(;Þê÷zíÈÑ¯”›5ã\<ñ_;`cz±z‡ƒmÕ —},8Š"ƒ`ž«¯¯ôaY¯¦òÑþ*±3;-Ç‰¤¿DÔ$½«™cô»§Ã»O¸Så²‚S¼"`6 ÈÂ ó»·–Ï¨éçcL­gd ú+ø)BáÛî(FÿñÚ;ù².´ÃDAÌÜ8MÿË­.©³3
þ?_½0h[ªÌ7º»Rêw¯dF}8ÆÜE×V–Gº¥£ð ¹Ë<´°@&SÉšI•ÆN_{k:71QvÎ®óËÆÐ²@,@»Góõ£ÍH,ÙlÈ÷0¿¼¿Ñ*IPë¬ÆâåOô˜æ~v Ši) [ª«÷C:”Š·«"U~—t·Û&RÜ|šºîÙ×ÂÃ‘‘´wù9%‡—ªØÕÖ0_›¤ÈçÊ½’|nS¢1ò#o¤ëžè
–Ã:÷ÖZnjßçBnºQ#Ý©¹Ð—‘!©½”s!ì¹°r„/_þbt(¡°…àe¬¤7Û+˜{7žw¯DrtÐ0)©q‘,2E<»‹ãš´¡Ö€›¶mÊH£ØOoÆ´ìDF;fû&iGA³dSjØÔãµ³b»jöxš6àïqÁŸ>þåÎ)Ž1–ýùÓZpÉjQ²ÄøÒµ»ŠµJ·êœžnÄQæýÀ¸ï1}¿Ñ5ZP¥â|ºÈ\+gRCî:7rîa
¾Á<ûâ$kL*²àšÆß xÎ|äØ,Ó©ºkïEÖÎ½8Hdcý1û¥§s+»ô¶=1Hœ¹;±^þ&<ëÒûTî®½;½„ N½ÌAtLÇ¾¾ô¢½ð‘¼0øÿ¿kÇ…Ý¦>„N›r2†â¬û•Àt‰¾Ëò˜íä*dŒ©ˆÙ}|Vâdi¸wªÖð"s®“ïšO}ž9÷žµë4dn>E8»PÚŽW
Ez©’Q¤¤ŠJ=%Þ¯œ»m7Œ*ø÷CZ¶wkc€ Ø‚Mê ëþ²Éè1:¿>#º»”‘q•]K÷è<å¸˜JNxÊ?ßò(V&ècëQ&È°IËâÙÎ?ÀÓÔ2¡k¯0×½£“'Þ<ù´A ÜhGUªÎÈÍO?³¿g “‚½	NÌÊ´/nFaK€æòá.~{IY[ÊÙQ³Qä©wç™ºbh(K{Ü!r-UXâË+«ªB÷WvÊ¡]uSLï;RÍ9ÔE®žâ¯ô&g©å‚VŠXÅ72.ÀuáÝ} ÒAx’;²6/ gEEŠS%&¨gÚë©_Ÿw;ò”#³%®H™µ"V£"#Ì'.V¸æf·Î¾˜;s³òõœ¾·6ÊA·ipªHïLo…<MšflÚ±@XË•W@æ•Œ}f?ÿ(bƒê=}Ÿ©„ÄºSµ£/Ü|Kqàû®Êz†šø´Æâú¦ÞmŒ†h0ÁòR{}f³´î˜SLKfãeŽ™ Æ:sFCfŽËjŽÚ«)"QŽro)ª‘Zß0F[+º¸Û­!)ÒÉdÓo÷YbÕ¢ÎË  [UÝ40`­ì /=¢°g{k¾<Hÿð¬'“÷)Ç™;¾3ï­ºW‘#?}E>ÿ¹ÓÏÓÔÂ_Û<úÆ0Î„°ÆêÄd£"?úòäD_g%jH&DÉDZS •„…¾{=Ï· Òëá•¶Œ²GqfUˆ>¨ø'ÙY¨£}íÝ(uºå xËú
jpw‘aÝŽ’)³J½_v:“q¼ŽgÊåÍsÌ/	¦MèéÏ~~˜7 (-ª[À¨¨G(ö’™î¸Ôøý»ûRÕoOÒùãÉÚñ?¤6\ \±‘0ˆ½m‘<4Ä]PL’àÃ¶ï¹÷{\…@ã)wMëC"rW¾˜Q@tÕøºóFq z…¦ðLÍræžú#‰-(cÄÐõÖÌTÆJ°>Z žý`ÔHøi…©‘½¤P*“§odÿåÐÇvÉ%'+§ÇœÖ{Å0[BºþñöýÎB;Wxd(·˜ššLMŽ •Ü¾§Ï1Ú¨r€‘š"ÿ|6^Uä`ã•ÂZlr>îøíð“ÒàFnà·Ã/±aÄ_¶M,i—v:V™›5‚VG¦	AÇüR”ƒ¦ã*>I÷ïdS—ŸÀW¶£-?•}íöâùÏfÃ¢5iUðËm–„Khx` „(ƒ\UÉÞ|r×-Ü]_­Â*gßýòJÛ¤ì£GÖ†Ô¶i(Øóeš3Ú˜`QsyÍ£×FñeCY=»pòi]É¨{}#Ð!„
‡æÆë1&ôÚÓMrÁ+L&®úîF‹ÖÞÿ×»_YJ(@˜Ì‘¶æS7bÏ9ä² [¿Ý\ç‰~ý|†DÜ\[çw…b>4³X>yÏžÉP´òTND#‹:q“çîCïþnpÉ°†i–-!W³P²2l¼´š`.¢	Å›…=¼tâÙ¼éŸ^	 þ÷ñ?Ü]®v‹Õ^ŽG™×¸ô<ÍYw»µYÙçkg—‘¸àèõ;€,’b5RðÎ-ôT›a·:¾¶G&ßVcß¿«ÓúÜÆer™fÔwíSMiÄÝOÿ«k§^o²¡ Õ."¬î_î*…§¿ýþÀVzŽd2\@ÔYŠš·F­5MAîž=õÈ††EÔŠÙ¬]¡+¶X[ÁÒõóƒõâözD˜-Oj\'ï[õB³µýéå·Úö¹'§ìùüål¿hRw@Ãÿù¡"‹fÝk§×ˆ1Û•G[D‚!°Ë´÷)]³"†^7æ{p?=÷=Ñ	·—T€®²bóà"9ñÃ“…aäpþÀšóÆç³*s§{üìûç÷¼X• ¸»ÙŽ1VäqØê¤µ(!ØQEÈü^¾>ïøCÉ)ú>vo³¸$unÕñT¦J]Ì÷âF-M˜G"–ßÇ¨ýÃ+üÿùê‹—È¹¥µ¡¹$:y¤Å ËÙˆc±ìÇFùüaÑµ/dz=Ö»µ‡Y´ùTõ>É²á'ÜbQâ”c¨ò
x-Kœ>}Ù‹›æhÍ×qÎá–³Ñå’g«&RQ_°t¹ j» ;¬UBä®_€ççúŠöú7d1u6¬%ì:ü”ŸŸû:¢Í`ˆ….om¿û”Yøõ]2Á¿·Ìø–Cól+åòËw³ê­²¶6Ç>ÓBæÙi(ÝžÊfÔû¥‰ÁÑ~)“Ó M¥Gyýöm÷¥—¦\n¥[©í»×‹cÊ!(y©^qÁ<]Ý>§vÔ½ËûÏ7Ÿ¡
M®ã÷m>›°.V#%ëbñçÞF¤lL½ÛW¡)@0Ê‹mœÒÕ;èÑÝîžÀ¿uý. Ge:7«'-ôö¾òèçiŽ…9Ei-ß™U0i¹æ†ky®S•.<«ç¦ ë±@ ‚÷¸ätD’oÖãðÞþîîF%@ÝnOÅÆ½k%l£¸l Æ¿»uÄ&MÏSb±=È¤ŽPµ­ûú“4h4FëžxoÕTM`.JåŸ;G¿û¬¿ŠŒîAÌ	Ýx´Aª¹ìÄïoÙ, é¶‰ôèäOcþöÉáI¨cpy­rð\0V
6m#ãÁ`—L`'Œ (¸EaA°JÐÔ‡ 7oS\}¾n.’×ÉÛ¼mÛØeòÄk„aúÑJárê/¯}L¹xç±»—³kKÁvG Eˆãä=ÁzotLŸ VüÚ1X h^‚¥ìSäÙñ„Œn²Uª±2é„ô¾"DDs]û:vGRfaÔ*·oa~û÷Ï³G n"=‰ž»øIx¯ÒŸ}úìGòK¯‘€kÏç7"j|AkxWvÜ¢Z3iNo.D…]m·¿¨Ù×»wáFˆ¡Aç[ù’íŸ¿]
}÷J=»U¹þ(n	8ñ<ùüç{JGd82¯B€¦µ-:
4sõàéÇÂS"‰^”`{w‡™ýNèì×Ìòœ”¸ä4¬É
ø”A™¶à¦p+sâ¾ñÜãÑù¾
1ÚgíìzOßXùÞz.5üÛáÇ¼iîL°h_¬.ØöÏ«9"ÝèY¾òÇ
ª²Ó	--ÛD¬è÷ï¬îüõÅûsi¶þü[¡B±2@Û'Ü8dáQÇ¥­…#dé¤°ËªÆ{fýìûWîZG“q#XÑ…dÜVÐ Ç¦7K'Ÿó¸bZO-GùÀž«/¯*—’–œ%¦u“òfÊ¦nl™vtÛîÅZƒåga’ºPm£dÿõâ“äPr€¥æuÏ¸”.”›Á=¼
¬ÓÆ»U©¶žL°N@— &Ð+»²¡~n\Ž.ˆÅÞã/³J2Û(¯ƒ“lõÓH’H¿~§(ßaÑåAr÷‡— ø–8ØWy­‰®11ñ¬ØYŸvàä'>rÆûè±ÝYq¾ˆP/=ªf –,Sò¤mG«3uÐS3ÛÞC7]:þ	¦3iûþ}~<S”¬‡"o¾/J_~òÄÃ’Œª‰Š`·¦Á[Öý}¥$QÄWÃ°š`^Ù¶7@£Ü-®FBhùa2:yã¡~µ÷ï¯|´§¤Ð‡0-½"€À®õI=,›è±éøk™î°ƒ“w’‡—Å9äØdU³~ýy·W†¶R—WhËSÇÐúŽ»- —ÖÔÒh,è•»GÈÜðLNp›örX\V)É®`üÊ«©ÓOXu¥ÞÖ__¸uhÏ›A£À40zUOê“°äé®ìDo¼Ñ1d¹oêœ,ìžióæ«nÑ{ûìG6NF”,Í3èØ²÷ä#ÓXÄ¾×Æqˆµè_žo3ÙM»ŠZÕ‹SÓ²þ¢=©RÚ‚0?îØA$ÁõvCöØƒY¨@“Œè–I{•Á@4Çš	D¢s3Ú_>ü˜Ð›Ð§€ËwwŠ†W¾VCúpeŠU§¸};*)NwjSPial Ô¼!¥õø;ðª›VNd°úº²§Y® EIª¿
Q-zgã2£ºrâ.-`½ñy0!0LìT¦f,ä}¹¯Â°V7ïúë]/Õ5¶(š›,·]>ñŽ>þ±d$2ªaíÔè¤Bž?OAy¥‰­}{ËgE¨–k_>yÀ&Vê{ì½9”%DsKá²IKPüëÞÛœ¸ƒn0g!FÇ/);8¾ï¨KU?Édl‘ÙBß^èì›*ÙF¹S-/Œníí+H!eVÁnÈ…[ás_×Ìta%î÷/¿»PivIjgŽMc¹/ß«Rð["<Ök¬àÇFˆÃl”¥({Œq„)³ÎEûÌ­9š„åƒ[G¬‚©©5Õ´:…~2Ak¶v¥b)Îô{ÑH`@ªr5¨=«p®ËøJ 5>Ý/SÔÍÁ’Cmm¬ù‡†	jÄþ¹7Î=…Þ6Œ7‚3Â%O¥qìÑ'|ÂðÙçÏ¿e=óÊ€[®…"
wž3Ò¹5ÏO 8è®9âF®õã«Ín>!ÌbÏ½h¾pèØ+B÷õS¡Éj¤ pÙTb•:¶õåÉ²Ï>©io¨Úž€·BU35ÖFNÍìÙ÷»ôa62‡Óû'_^áj¸pj¶Jcˆ7ûsýöÒþžåËvÝ¨óhñÑéü‹«Òó·k®Å¶þýë;PÃñ `þó{xŽ>5ÄPdjæß^ÿ‚sêá_‘«ƒÂEH[ˆY§ã8›”ÊOOºvpxë\wÝGÏ?ëƒ½Ô±dW‰›ÏÁh»¶Ü&ð«ý˜Ê3ˆ7šÆ2^Ð±gê·§^A³KûM:²¸„íñÿôùjöâç+[?|iYÏâ†óŠ°®üîvŒ4sòƒ½éæ-§îõþÛ“_¯@]\Éc	’™]ôæ|³’¾øþ?¾¾ßåL‹ÛõÁÂ=‚€;?=0;CDìg¬¢È$~¦´±Ú±²ÕÛá–2¹ÌÝ©&ùë¸¶#ÙÜûñË<­$NÔöPšèîÀ´¢¨9Ý(Œ¦ºØ>tÄI‚ƒ3‚£¨:ÿ”Éwê	ÑA$lß çç}–|ÞçJ~ïØèÕ—Å2WDõíèêýêm&Åæøä\4bïµW~§ª~Ö-d¦póUáâ¨ÉØÍ m™Â¦$®©uµö¬+ë<þžÞ7J§‰Ó°ºsðÆîê/w²­p¸zkØižT´\?½6¡XóLkx‚\iè÷í½¤}ÈÂ\€ƒÍ„¢½66³NÆ›F™ùŽ\_ž$äÿxå]}rycKâ–Úa™•/	eÈs_­í2'b§²$¢è~mB¨ ¡|[mµ¯jÇ¾b‚gõÇïZX_ä±O?3»Ù‹vÏ½?”UA´=üÀùÆý)æ¯0‰øþÇM
ÌBd'ôËé(je½S•9ZøÖä™¿¼mÉ¢G6Ž½afšÒKSG¿P‡èNßZêû[dƒG#Ã”ï~,k½>Êª9‚6f–óçésÍŽÔR0pôíÅX‹¼ù"¾(-Æëš^¸A É¼—nÛXYµ!…å}¶.È{NkyÌ†Ó!s”ëo„›¾ùùî®rØŽS\ÉÄÍgiÁHÉ“a¸;ÀÙïtCµj ìÓ¸8¢L#<^[wC¿Eù;_À»éåY!96®kþÔâ6òL
™xäõ¢U±gƒLFvb-·‰ž¼Õ½tÛ–lÓ)í´;Žù2gPs½¯`þâËÇÈ˜#!Úm€Y¤æßÞWÍÂ/Ü5a-U¨þœ‹lUO‹¦˜H!M®,‰çS*ñnÞÕá°¢×žJ§iMõfÕMoA,Ÿ|¤#rb+vä©¢xIJîÈ…Šor‰Ñòm„WeùÑOðÅø¾c›}â@æÔs“X¯Ò–,jç“oÈìÕyŽ1æË-×Ô×žCËÆS+EÞª¾6Y, 3jcyy
Žo_½Ä‡ŽY—¢'ï‘ZÓ±?‰L¹y{íPµ®š‰$g¤Œ8R…‡·éùÎVÃm‡€·wC+8àØR`èæË¾ª—Àâõ7_¦Æ@Ð?øœwåiˆ­ç`n¯ið£Íž@Þô–Øp-L¥ÐUbzü¡õÆ©O^qŒ“J­†‚ŒR+åÙï—³þJá?žþè§ûü5ío_¾Þä¤«å6Š:ª6ë-†¹È±:e­Ïöý9Öÿ}çÖ€iãˆ4äÈîPóUtd_"F#žÞ†5VfÁ1¼vžäáNm¨
}mm©¶ þÓ»tŽàh¢®¨xVöÔÇî'ž}é§ç!:íõ[Ö²Ù¼§5½ì×¾S€Ð†-X<0Þ,Zu‹?=; 1sg‡‘Á=<AŽAˆ ¬þýÓÏ)uwnQZ:‹œ6œ–ÜT£iÅï_lx=e¤³ ém¶œC¹þÊÒõW®Þ;L[ÈWŽ=eælÊ]éá‰gŒ6±d‡ˆ)Ü«÷ ÷LŒW¬Bª¿}xØyãã_ß.,îbÚ¿Þ±ÞW.ˆ¡q ›È÷»ç?k†ëüÑºÛMƒÕ3˜Àº«Óˆˆ4-Èa¤ä°Œ**+7Ôæ 	%wCe×ZÄ½gZ*ˆÒÉãwN¨öB"uõÆmÿ8x>‹È#B“˜z/ãð÷tÓ!ù¾˜O¬!>}Åfê@f¾ÌÊ¶@¹iv76ry?f·¼'ô+{Ò0J6!má³Õ‚£Êù[òT¢ÛÑâä))™¬w/~@ùåƒŒs½¢Â+È9¯„(÷|¸¼Ö!i†²aÂ®ü_=qhPÛÔÖ9îœ7A‰yùç‡7ÿ÷ã'Ÿ®ªçjg¸ïj*cqbe»ÅŠÑz¡ðø›gS}agµ<f¢Î°zÀùGºsðšoÇ9Lá+‹>%
'ËÑ{t¥¶cSÏc”ÄßNó{?ÞÕaâÌßÝ²5a£gÁô’€M³áµÈÅÏÊ»ŒåÊXU¹e+vYhÙrEõm/é§w®¼H´¡ìK_ðæS@‰JôïŠ[ÓÄ4ˆ9Y^àÍÅWU~áµÏ÷j¡‚òNÂd¥o²]'OEÍ˜@E}æ£nÐ¶ßœqy÷$]´gõÌ»2åµç€	k}ÔO«¸Ì”Ö‘¯Ü.ÓÎ"Ü ‡;µÏ[J_xuhø?Ÿ{êœpr :~g Y¯VÚæløæûIðÆjÝ­Gnž<òi$ZaøçŽ~CµË;“‹ÔµáÊÙOá“‚ïßžÉ°ÜS«gîóíQæEx~wt~úÄÝt³ùÚ~ÑTŸ[4Ò³5Ý*2’Mv*j4SñË_/˜ÇLŽ™^øÇ;@­MûŒ/¦W8‚#ÛñcÏ.˜³Chm|jmËkØZ“¤}]°æÚ°åïß„»½lÙ]Ù3À­êVÌ>Ž‚4ƒkƒÚÈhÞ¤‚Cÿöé+Ûƒn äÛò—ùe¾MC˜&½|Wÿè=U
smÈ!ì |žvÅ»ø€«QM!5¨rè›G_œ&#&wuÐÂ+SXâù7Ï$°­M:ÕËõ˜ÚBKò6;]nXÀE’è*ÁjQ_ëðõQH7%™·ã:†aEBb-~ÿØVW/\˜
ÿòUjAï(¶tu³%ÈqÇ‹æàõy—?R”“¥œl8_¤ÄšH8 «‰çá¿JðI|,‚„BE`QÎ=0úþÉ&ÜI”0ÔR ‘!ZtZF¯²˜%ÈðRùèû|,‡Gj- ¾3îIîÎ.½êóZpS™$%3u5R weØ¹àÅÛ®Ü­¯£öôÐ™„ûwþÓý”jŸÔpˆ@¢·:2"îMg@cH³r(5ª6G®=4]v¬þí®OCèð©÷þüöèåd'-Ì*DéXÌí«'6zi€ûÝ;s¼áR5D©m®6XXÓµG…kmÁ
Þ®ÔþÏÛ wWŒ'Þq´W'e­EQóÜTiðcªƒj}Ù˜]z]¤tff=>,;ýGü!<¡´g";ao)¼OE–'3,+e¦™:òp„&ÄŽùvcóÂíFêŠî†{ëÀ"hB+uÚ:Ó`vR52ißÑ‰¬ØkçXg3at"ÈÀ»©ÉJ'¬T“lÎuñ0??qãm_ÞËýEy«IìÝ¼‹ß¹v×„ÍÒ£„udôž¤sR&lž!U+&¶§òã^Ê…‡¬êHôß‡nÿòesY=Z×›;±P#–¥Â×ÞÈ	¨#Ow³áHZÌÖ‘ý†4"$Z,ýQ]ÄÃÒ]=Ð·$®ä_º•ÇFËØn}de)îD:Òåô”-O´9’fÌ^øñ–þ¼Û!È™râ ßlÇgäâ!ÀÒ3BÉÜ¢u‚V¤¾û3ì)°•@7AG.wS­Šbe1ýÇ£wZc#¹]Àsq«41lêÜ'°ßny€Ášš¾ú„uWÉÈAÃ} Â òMÔ¶£ ™ŠWn×;®}3¹%¹¨u‹¶†ø~†Í[=s?€Ø!”a‰%ýˆj•E]vâT8#e«Öºñ¡ÿw¼Umy£;kóKß2×ÚžS_›jJ*wn;éØ‰IØžñ/âÑVNv“ì Ø…çîÊ®i$ž©ÑÍùÖ^þú‡×A˜ë‡cWï[ld»þÜ$wú¡lUYtÃj9Cé8ñ‡•(¶äê¬ùàÕ‡D‹C½ú¯ÁÙ'¾ÊœºuÉ–uÀÁŠ–ºFšgV9³ åøùÁBR9~úp„9	]õMñæ¤>™ÀR¦nke	›‘ :}{ÂGŽ2G5ÚÒa¸f¨’…;¤¼9ÇÄûÖ¡‘ówˆ‹üOÏðÐMn	¶†ùõ‹µÊ4æ§ÏÓhfÅ|õÍ±lKÍIÇk<›X¾ñ•…‚.©Ò¼ão,÷%	DuÅ½\:Ú†‰,Âå˜ëÞ0‚5Õ7¡„³³´	R§@+O<­öîÙ¤DF }â#P3ÏË¿ã:VÍ‡BÖë÷Ñp¡„’àÙWcøóµº¹72'kCÄ>°@8ý¦þøcqÐ }í?ßûâ‡7—°Ž–Aâ›Ó!©¹ñ¡¹Ì³LÑí|¦L)É‰þ€Ôž‘qb¼Þ hµu÷üî¥§Jc<wJÈnöÊ`Êöèî´äŽÇÁXá‹
}-ýôÆéƒÛË‹žÿ}àë[Û×?®ýp;Æ<ïÖîæ·´ž«Ïìõ¼Â@NM& Œv$gŒzüÄ#÷Ï?9NOJ‹½ÚŽ|†e4¥$+Ç!,\9óoïßÊ•îÏojÛ‚”qì©>n—IlBöñÎªßM1²r‘£DÜ´s·»™Ô‰ÞXVà¤G°6 ¦¯V Ô-á5Ü˜!}a·6g„ÕHÚÍâÂ‰4ß‹¬5EÌ–Sc=ú%àC¬ÛÌ¢{îýh6«vCNb¢Â0aE;=5®^eËD¬!Žh‘"s’¿>{›È‰¸e;Ö¯âdÁŒ™Ò\ð²m“iœa.Ê4{îjäìãÌáˆ$ZeàB®à“—înOÓÍn©†ÑwW/Þ>áª»†¬šÅ
›ž_\ß‚ÍN!æ'¬HíoÑäßaÿüx¬ïœXPôËÏOÃ¦É06cjÃb:Ÿ{gŸ¿ù˜~òÌí^¦éÅ«ò¿¼õ¶
Õ$|ÿÙv²N¬é2›ˆ}†å°9,);„ì £YŠ¯üá3éÜöfR‡/‘a2d‹ÃWiÆ*¡5ád¼j˜Ç©Ú’>ÝSVÓ;nÆŽ8‰°Íb£k«ÃËJñ¾waUCc¾y+Êäö~é¶gmûÈ³DØ~
6˜s&_~zy}š4mÜø5ñPLãÂ¿=ö!#=Ð¥ÏŒ¸‚Ù©/õh¿¾GÂŒ»äØOßŒ’ Õ‘·Ö¦&ÃëvÒî›qÚ7e``‘8mÂpþïOþÑÏ©†UõtgèøÛv†%HåÎÒ7àúêéÜµ'xj¿
‡ˆ't5!²­é{:éSƒ LÈâÝ¯ÅŠA˜4”€ráF€^‚ÖýŒò& ¦4d6P
N\ú$›ÛÿîÙi©Tn¯Q_žZÛFi+Y=—NÖ¾¿}rã“‰¢`¨?0&Ãoòn¼%]úëï@Åˆtžþ}—ê–æ©`Îà¶
»¤AFiß„ÀR;çZ¢€§Ï|6X\€)¦wV*£+²½!©½šßçþûG_UõúàD03pñ.;MTgiåë¯±:¯é÷ªEÝÕÓæ®Üš›Ä6Î¤òÄdÄa„ÙXØµÿqèA[w¼ŒÀÇ×wãuæ@Ü±2´%mï¸ó;N§\VZ»é…ižŒ9Sb€uÍëÏ¨j°‚*‚[ìø4N?•F•üû-Ÿ¸7¾©@6Ê“Cñ‰M¦“m#JWNÇŒ7
Þ	$d@ƒ(å%+#Í°C´0Mw×åQÑD&
!\½GàÂsJªìµPrÚæî^J—G‚#ç÷ý¼#ŠMRÝ¬ Ë›©yÀR;áé®sS@½aOÑ§æÿþÈò’i;º›*n¼AÇü‚MÄ:Ö	ÙàL²»äXSØØËžž`¿ìäb~ú,V8ù\UŽWJ•8ÎÚÅC˜ìŸXG§×ÑWž+›!ýVa<”EùVù}c"‹_»ßß_"-RÍ<»È5Ä	aÓ¥ëOÎëZ³-¡ÁI²D1Ö÷¯T³—ž¦åuø	@ªYDš¢¸ÊÖcMÊ~õÌ½	?u×=‚–T÷õDÞ!hÉãã,0~êÞÀX ^÷	´}âww2òvé¼f`e¿;«?÷,ðë-f?G°2ÅÇ¯GH¹mN`­ÊÍ’6©0ó¯¯vËôe‘nà»÷Eëœ1<\C
ØNÆÎn7ÂÊE§·èe„eÈÄ=ûì¤§ªÂ…MHXœ•Ä5¨
Œ{˜ÇÊèóÄqí¨·KÛQ;tŸ^†
E²¨BÖ/_7R¼LzüÁÑPvÈmÑlC¡B¹‹!€ŽÀš¿½ø“kë,.éÁääZ,³ÜâtÄOL{=ÂŒÂ˜Øõwûó˜ä÷ Õ3e5ºòûôÃS2ÇBCÑÆ˜«X‹<¼YÛýñe ²%þåî!‹†Cad†þÊ;ÿ|üm†õÔgR( Fç)å†¹á-t@±å-O/NNh²Ö€šµK1“m‰0¶7Nð…ÇGO|*£pR^¬‰ý¢64A›Ÿ›ôa9}€71Xô™ï:N<0zíî©üº©5W&ƒ²Ùz9²îêË²ï¾V\{%×B´~}Ë€fÿí™»@.Ü\ëêón/– ñ¸I	õ÷òlî“Ñä8¹«S&¶ý™œƒ°éaîoAh­Iýn“Wq-šCà‰CV?-A,øFâ+X†Û§b\¸w«ÞtŠe#gï«•„ÛF4Ã†õÿ|û7“+È¶yg‘=?’&ŽÖ›U:k"qþÝ|J;*4Ç»°æûÃ“: ·
SAëëþko7„Õ† ïÓ×mÇï÷º›À3jkŸýêÌ£Cç[ý/I¨ÛÛ\4Îäd\€ðÓ[òš·1ã\†˜GßºðõcÉC˜ùñÛ_Ÿ–Š†’~ú^0KÖ¯%\‡€hÜVÙUÜ²fN$2UF
ø¡W˜X¢¶½xF¯ê£ýûÃÅo­ŒÁ×mqÖ™”ÜÈ!ü× P”í¸¶Ž|Ü™™ª´ë‹°~¾¿Rå–&€Î2vãÊ!ðæ¢-”L-Ô§7º\OcT„å75ØÑÿùöãÇÿqÏ|£&&§Ña"d4pãÓ¬#·w¡üÜ&À’“èl%QÜ4sÎ¾Å¬ÔÐnãj5v\C¶ÅúH«Kö£²‰¿½ÿy‘šEþ|+ùï~,EK—?K˜<5­äŽì“@ÿ<øE*0ra™äÄ‹g>Žêêüž"À)+¦;RóDRÉÊ~zŒÎe—¾; ¿øp¢ˆÝž'™&O<6³2-UÞvº[`¨ºŠ›åuäõî2ÍÎ«LmÎ Kþõ¹syñü‡{-ÝÌæµg9.É÷Ž}IäSÕ%ÙÏp-ƒÑ“D±ˆù­k"pY
zg–T]‡«ñVr|·™_8þhàæóÓë-Á™;gî§]xŒÃþÍ¿ßý!8Ë±úÝl,¶Ò¿ñüÇ7Ê‘á´µv>÷ÃaßŽïçÇÅ‡nÜíµXÛ7oŸšâ­æ—O}%+	þþàí4 ¼|¢h}ÅCm¡c'ŸPÇ¶»móDDiR‰ÄØÀ±Û»4¿$×“gŒP»—hk¬ÝqçÝˆª;4wä~Arö¥^ÄÄ9{óê #„hgfÖ›ÄÌFtŒÃÆ›Ò.{0dALåã±ASC®­¤N°/i
7Î([£óÇÞžÎÑ–‰%N›ÜDcCXïK+Äo1ö:í%bìŠ$#TÿR\SëVä•û©4¢·–mRøQáñg·¡b¤ÏDG9!]æä{ùÚ‚?÷<MtâYÆ¹wG6¤a“ÉºÕ]u&}ûº1 ÀÂ{ˆ}Á„;
•>té#óÙ‡ë¿¯–Õ¬{_=èÐ50`AÉä’«qºõ//=Õ¤:ÝC’ÿyñà¡:ISÈÀéÃ8}ÿÜ;¦›Op$:ìÌ6Æ¥Yî%†ûÈA ÑòsO¼®MŠÞ†B»yU‡vîaY-€ÕlLdç/U_6UË 4T6ÃÑcO,%<:ï:Q¥ MÂþzø½4M+$˜&´º]×IùÐ©WÔz xã ¨l¢LxÇäó™<Cm¦/=uñu «`ÇúíJÕáÙŽ2ë‹HÕØÆWå–½¿ßýø–_2Öäp[5b-…Åx433ôšf¡ Wê#¥úÏOç?4×„rmSYË%~ùlrÃ¤–ólhÛZÆŒÄ.>'èû@Ð°¬xö9Ä‰wêkDÉÀ¼§óÿÜñè{‘¤Ô…NüøÄÖ&³šYÃ’-µÞùæ‘6!dÁ ¸2©:«K:2!âi,Q6ŒÈ¹ä”›OKWë¢ÊšcFa$¢ÖË¯øFƒÂ‡•H/?S¶^¸“Äæ»0¯áÐåNº¸ãó¥M‹¾Fxì‡W ]^üÛ#o{SA3!T´‰²Â­ù/‡ŸY©•:6ìexÎh‘ZìnŠ,©
µI¤ÙønM?÷l›'ZØó­§W–p#[û¢ïÜg[<Cè"9‘ùÓG·ë%~â­=.& ú[kh'GÅñù¡ÎôÕÃp ±8˜úõ½}knJ6mòÄÅìËo#è:h(k9E¥ŠG½øêÄ#¸|±>URHSX¹öÝ‡Üðv“˜•§‡r3q¶ÜQõN­²ç§5<nôô×Žï…ÿqçó2rA¸ì“Œg»H÷(ÞÏ­ÒÑýp\ÐXu«o=%Ú5D+0Ct«Ò—Òç êìÓÈõùq¥¬·WæŽ^¦sœ-ùxmBšS-PØCÔ1.ß.çHøÓwYw©ò
aÑÒ`qsœ©%¹S¦ÓÕxƒ‚­JišØ:~øÄgäkÏ,iÐÇ¿¼ô	6}éžé€T4$ó23Í“'1Ãtýó2aÀ_B¢qŽr61;5FHP¾ÄªñÚ×žYøx>éñã‡4b—¥VÀ³Ãéµñ@T½ 7Â¹ÿ>ô§»
éÂï^\èõ7%Ufp'·¥ŸziýôóSÌ©0¥»1!O„$1íêÃÅ;[˜o&v¸ÏÚ‚-X³þãÑ§\±·¹ÊÃD/½F™d2éÜû]  ³T´Vå+ƒxf3ÕugG`í‹_ÀùpWV˜ZÏ‰ÓXRÏ‹§—‡Ç9‚ên~Š™-™Â÷ßmq´õ4g²IAú5ÇæùþNfÈÕš&(–¾zóŸ”ÑÒ­d¶©6º³¾®_IqA m½Ön&,Äe‰0×²>vlšËóFî‡ƒ¥ã·ÌEÂ®w–ñþT·îßV!ê‰`au˜#K¤Ç³Öy9‘]IÀÐs[ýÑž†/ÖN>h¬Bëì‡BÀ£I´Ã7*õÐæXÈÇ$,Ý3§¿ùÛë·ˆ#E©`¥ ðýåµ;Bdó…ƒÐ£füšË÷n÷Ôì*–@]-¸IƒÏ# ¥aã+/ÜÏÁÐ¥+£ë?ò¼‰9º1ÎC\±ðåÃë¢(Ð&`ˆ]Èèê
Ñ§m“SÑ¨‚RœÖ]~ÁÃÙÃ¦›%R×Ç	ƒK:X >ú^“&¤65‡YfÀø°¿’êp*k¥G$«8NS¬™JVd@NÚÏ5ƒÒúošRk¶*Ç‹³>Ã/Ÿ6³3ü©ÄYQS”}éŽÄ=BŠ:'Däw/~âÇ\{X2³Â™oûécd–8goÐ8e›4Ñ‚éþñå‹‘Ý¶šÃÃö7Û^¥5µyrF1»¼2³G/†Ä8°y+›SF„M;·óã”€Ì5è×^*B†9˜–}`®ERçÈÒÎ¸¤›’†7¸DYrÕ#‰s3ÿýÕ§CGîjT«dL0&d(5„Õú±FM(5Am˜ª²	˜Ã•·iÚÖßý2•*§BÜñOMó*EsXsüu³åOw~A\p?ÞrýM‹áüÁe‰‘XG‘¤@m8s›C“@žát” ¹¶ël0Wvóy\-ð´gö51¡9­Òd$à–0&õÀìjnÄ»©ÚYIIÅ†œqiÉÊÝW"¿¼ƒ”zðzíÂñ·9­ÚòDŒ«9Láð° >QP™¦ÿÈ­$‚5Ø_o{.=vê£é?ÝñÀðrj[ìÒ_{›Ø(»V´!I¿X	Z·“’¸:èw Áe')æÔ–	nÔ_?~ª*a¥/=]œßçÄwúÄïTË½é×/N Í%Á€kdº¶Âž”‘Å–€+_o%v4®Þ6«cFÌæjôÜ#ÅzÔ? ÕÔ€?¶Ùv¥¥—¼ðú0V]ÀÇ7ƒÙÕgþÒ†Ê™Cìîœ:g>þ cðâ£z!1ï<ùÑJçäá–ÃTn*Î?.Ê»¨¸pË‡µ/ê—;œ1ÊfÇžmŽM$1K¤Tóïª‡Q þ>ó§ÏÞl„·y="èè—Œ¼Ä½ß„²ª€z B¼òÁúÎ¸i_Dnš@CÆÒ¥ÿyßÜýæÌÊàdæÌ jig]É)¤K{l s,7žUYuZÒXÒšì*œT7‘‰›¦Q•ÉJ£”ñ­‘a	Ã><±¬· ÙvS´xýÞÕÉ¼Zo–í¤3w~{þÞ3‡n»@ß1eÃÚè²4ä.ßÆáHtí<<œ	æ*ûo_üõ«…!ÑJÃ1 Õ“yû!lpæú­6¾¶¢BEL²ëÃö.=ˆUE	FŸ›#£Wdä	ÞÙås&º¬eR‘„h;VJF‡·ðY~/ýËã6`Ò\0ÿ¶,ÝÀªÄØQÇX…E¤”p°³À}ÊD³³-ÅwÒ$Š42,ŠA@ 4E‹‘Ž¾ÊjñçÍ¹•k·‘Ü…YoYRÛÕHûÜÍ=v¶	ç	„°žÅ;ÿ{%§÷Â;{‹óíõý·Ì«Fì×Î¯Ç[R3G¯¥jäMº—ëV„°Ãr¾~;èdNíÊÐ@d³€»ô€Œ>¦¿¼êRá÷Í¨ñý$³ú?Ü[•«r3æGëKø<õèíRÌ¦@ëó‹FZDÏÖ5íÏúù›•ñ†¹±.æshÄi)–áóaž6ÙáQ¡¦a\Ž…@iVpLÁl=$çÙîÿ£o•°»ŒÜ3–b¢Ð$&`äAz¾™>ù¨Ã¶¸â*:¸Ð´8iûå)óWáRMe›æ2r¤|ý½2uò}Q{äÄK63Yâ0£GÞ×¡G?üåIbtmU„ß› Faiš\ž.ñ9*N|ñ‘©Çdfù¤4CÓ6-ñGøÉ¸w'Õ‚<ô°çÊsmv*LÕå8³·EÏ…d3Z¹>æÁ)ý6S&âŠ™Þªhò/¾•’…YöŽÙ e´å?Ü‹ÜÂVòËÆNÔlP;“Ìêô†dÚen¥™W!6é“˜JUºùÆÕo´?ØÅ¶Ç%ÇïDNï0Ç¿íÄg»CKª¾·Øn4›ur7iFcz ,ê¡Á¤)#¨­Ð9¸}ƒ½±ßÃ-þøÁÏ÷K{þ8«GB/Ìá•]tåê¡H+!É«§±²0žW‘küÔD&aLÀ=µ<»ÄWaràKo4z/ÆŠ3§×u¿<ÝrPI­©¸×3DãóZQš7ø²\4Ð€ìã|ù)6Åo˜Jß¢4[s³@ÈfïÔXË®bžÈO/‡} / teÞ.Ææ¹o;3äá•%¤Ê	ütÿjô/}uäw‚¶ò¼	[¶ÓÕm‹fóÂW{ƒÜ|[ÛÉ¯u£;h—»Û'iÖ+—nµ…Ð"mo!F÷€09éµ¨T)3^š´{àtêdÍpí*O¸pîÃåvêªxNŽ|ÛÎÜª:¦¯ßÂ[ÚÞ,­ÌÑ'ó_‚´¢$a}3xOMÝVf”@¨žJ9Übrm’eÇåqPÓÂ~Ø‰X«+þë®çâ {€Câþ¿½ww¢‡µÆK‚]t Et’ Ñ$MÑDIôê:îv·8.qïã¸ÆÎØqÖšñŒ=3ÛãižêäœÔ““sî]÷\ŸÏð<{ïç·ÿØ›6rý™q+näfW~YKø¢/†’òã/Ú÷1Õ®¥"‡p%iÂiËÁüb3,îNn“ªùAÜ42ÜT%mgîß®†
z\g‹ýáC†„‹Wå¼ãP|xî¾Ú¬4$©Îsƒr=»¸†Ý¡žùc@¹Jr¤£´Ž
"jK€…•d!Ÿ~nóf)”±!ÓÔºðëKÏ‡pÏÈ^€M41Á>÷‡Vu¸_ÛPÔØ…ªýÔ/J‹pÑºCô€ÑK{ÙKŸ¾óø‡°üÊÃ)XÜÚ°®aøûˆÔŽ[85¿Ìo	*•°ÜÚ_gŽ @Ü"_G¨Ø‡+´ðÊ06~ãs„ÄÀHF0É'ÛÛ©´¥”l©‘ÿÖ'Û
€šgš’ªŒå½‹·	%†ŽqÏ†™$VY‹ƒÌ<òx§?ã]øàê$$I¨^Lc-ZC:ÿŒz1ÒÛT¸Tº\™*U~øCb3 èé¾é¶¿íuõ˜°¾1YûË+oK€ÃÅÇÉDµM vŒ£¾{w	Êðï!‡}j|d6Ï®€Ê7Ÿ_cþå×ÏÍäw¬:«ÙÁ/£Ý'.	°Ëmí>ué•ÃB¾…&Û‹ƒë¿Êò‹ ô×ÏŸ;ºL]—êT21Q…ºrnBvé×JTx¿]åíÏ×" ä^œ¹ç^ýõÿüò	I<™©0jîa`‹-Ü9¬LÏîÐß½«ö’={Ÿó¨ÅÙ2QÆ=\YpLe÷µ£fÃ,P/O¢¯½¨†¹šK]ñz&0•¨gc‚<@)Éˆ#Ôþ!Êè¼ö»ØÞ:qôË;EW?–Q´IDm'”µ&È ˜[Þ[01¦“[~IŒª”Ý-²§ÒYå÷Úe 1QÛpP¯¿»¾á%1‰²,²=spñSßpâÊƒªs¿¨2â}e‘BÖ/éþ}ïS›"$~ÔšR‚8®³oì†Ñ;ÙŽ¥#å¯¸úÍyIV9BˆÓQÞéTo…<	ÊâXÃÞÿÜö>[¸ÅmO˜ÑN:ÛÃ\”F—ŸceÃ,:=Ç¤([ß?MãÆKÝvùÑ	c×X
q/áÖ;ÒúšZ2!¹Œ1Ã„rFÝs3&¯OS’-yß0ÒÿÀ6§6–bá%ìp|îù@Øs@/3ûäd
‡#‡)ëÑ‰Òá¼}8+›.µ
Éß{‹ðô¿eä³K !¹Wˆé“p¬Í†xF9Oakþ¹½äíyØ"à£ 'C-·I•¡¹šÌÆrxd'65„¢§~í2ªó3“úÈN¡¹-½z»Q_9ÿD¬_ŽIhí×wÊ²òeV5ípÊŠ„u6nçøMŽMÞÙ—k½b¨^:„‡zÎ}ú>þŠ©D–é%û]'7ZŸŽ„ÔdÚ²r+Åž3ÁM;€YàÍgñ¥ôFºä³¯w‹4s ×Mê†ì‚Ým^<úÕ»gÛÃš/¨¶§ô{Ý…tFT^,ôõ‰aÃKèG2rÇñÛx©Î°‘o[ÒWïÔKË0Û—Ñ&=`ŸÔó»âLu€öM[>´N’‘›ÙR ØLÊ8Å¿Ÿù}±-@/˜­C[ÜÂ“#ê ðírQÑÍG˜{®CÁR~»\5pÛÕD²>R1£Ï¿³é]/‡†(S-nç™e|ŠÃÏn©Éã»\Ÿ*ÆÃJxÈÅ:~Ue®UÜFÍ-J˜pÛ@ EW†|A¨^¥
¸¡/ŸÚÔAn•–oû‘Š‘>ELyXl¯±À+½q_—ƒÚ©âW¶}³@À6›>œ,Ï¥ÉÛ*#[‚M…Ü™i5Ç“3N¿ƒGNá¥®†»í.i2¤UeK»pó.e`îË?8\®µò,~Ê9ùÛ“Ä¹ÓÇb’RI±0vXB¤¨D†SœÚ‡xZû¼ÑŒÍZŠYïÖNK¢œaò"i8HU;¶Û‰Ÿy »51Q¸ò›RÎGLµ6»Ý'¿8sta¢=VzôÃA©7ž¤n¾£ÉQÆ›’q×ªÆ}àp6Õš€y¥”ãœ¿o8346G§abú_/½gXòûôžÈôÃ3úiä:¨ÿÃ}Öè7¯TgÎ¿»¹Ã‡yˆuþæî€:þ¯Ç>k"uU`Ã	E”Ÿ~Ãœt¤ KØ)=%„W´fWiš8º×	~ùË¾*C8Q—îZY‘à‰…Vc>:¿ò…ŒÅ7ë›Ž¯ïÔ—¼æÜö…ÇÇÁjÌãÌx¯?ðÏ×_‰ú~x²”Nm.º2g?‚ƒj+_}zþ×
X¹úÛÂxzjeß=áBSÛ3Óyêé{¾ï_wÛ+)Çx²îv‡³‹YZƒ*J£¼x¿ã4×ÃqÝf˜ïÊÜ˜¯ßÝ±‚öìÐ09çY@•·îÖå|5Dël/&£¹ë³ÄˆxœÁóÉ=k¾}ÖGM#þ°Ùõ/4j8hðÜ[Ÿcdóø›HHãÇ»Î~„ò’ÿÊ;î –NÞØ	gÞZ¶ítAâ¥%¯Ø´•+]åìCE[‘(úR{µL[ºxíõ+Ï¬OwéüáÚñ÷ëŸF£ÝòÙÏ# €ðÓ½áý¥v~S-õÌ|÷zn/!	¾~×sì_º›E«ûìŽC0c¹8½3Ö,Ï}¨> ‰ê\M€ižæ†ºù5êfGÅú÷Ù–ØÂ{+fLKˆ´Ð
Xl(’'cžwkwMÅÛ¦òñ¡K€ñµKpÎfteä«7+\§«£¸ó±æ²‘5![%¸I¡K·×OÝaÿæÑÁòˆrq¹cŸo,ÇÏ¼„ôÿ>ýÌ<Sm4«ñl°Ôi[Ðà%úé×Õç>Þû×Oudàp¡¸â°äªpòÕ?4j«"2¤YðÒþþù§Ú¸Õ’íæäèµ™Müÿw÷S¿r&P#Ý¦ž‹/(!PŸ:š3RLÛŽf¬3Ã„uÝæ’)³Ðw„Iîz²@Y­ÓzàåBÎÍ¯P"‡3k’F©V»F±:%½²>ZK¹ÓÅ^Ò7¬¤Å¿¾ôÁÍwt3°€«–Õf;!fZòÌ.¥I×ÿëž_bp+‹Ðô<#<+óZ&ø]f£PŸ8XiY†ìËoiAs„¸ãLìI9SüÝÈà«'(š¸â"ƒ…]9»áe¬¢—ÖÊA°%Râa?><ÑÞTŸ8ÚÿéM[Z¥Ùëã£×Ÿ&¤N=”uaŒ)19„°æÏ½•?äï5dâUln¡\xVµ5+FñuæXf³»=¸ùRÿäck¡ò­™ÑÀûÜÑeÍUH.<XŸìbÎ~Jsn›˜~ ÃÆ§äyåPË÷¢%¿³^øl3©H®´e‘±o¼¸ÖÔæ—>P,ßºÇ/Yg„,«À$T*œdÀ¼8°Œºñ¡\¼õ«Q²*IÐQçàùóG]’—ÒN¿ÒA•Ù)ÅžbdîVtÁpõÓÁ©»o¾WF-õp…¨àIqÅyÍrqéšãý,zî`ùÜ½0WE%IóùOŸ¸1‹„›ìËœÄ°ÖX=&j–Z³ÕSÍN 2¢ö|YJÜß÷4ÍZ]Doíä—´zçÏ¶rþP—,˜¸‹ØéýÍé}!Yr ´6ÀZmûËW3ƒxÇàBjf(ÈF»òKÎî<¦Ù›C…é¦ð¾ž¿¾§èÜå÷‡¦/ÕÝKº9©ùf‡ôïçïá¦Hçß¦MÖô–e:C¯ŠíR.“ö*`¹Çrî%ÎwïWê¢½ñ
¿ÑÎ‹ý©åºÆM‰°Û3X¶ù°Òlz²ÐnYjûÕ¹9ý™O&Dh5t¾8²ÐR)¢a9º|d›‘¨x©L›Ú§¾|½¡™^ŽjŒåM0¢àä2–ÑþÂÒ·w$Â×,¥Ç^qúÃtV§-²«.½Ô >Ÿ*5=2¿–>õª(-Ìã¿{»7ß ð„Æ¾5ñÕKÑ~í$|}¯'w´·Ý¾\®;íÜ@Æg#ºe£I’ñ9K{!,·<t®º0³xtDËE†­…U	ÞÚ`öIÕLÊï¨G9ÔÕÈÍ#Ç_Jå²Âo¤W_sW@CÐV&àÛž[<ùz­¾=_Uá"t¦ŒÚ¯£`kº²wú3ªÐ›#Ó¸µ.1­Üá®µ÷šã³9	Mv0Å”Õ:FGÍ1f¢×”n)K>`Ø€L’ÙÎ13dƒeÔ¸7¦Öª8$î?}:ÅÁÁ®¼9"ÁÔ¿9’ßø¿Ûþð%Us_ø®~€JÁQWÒæÓþ43®$%RssÎÑ/Ÿø K°¦‘Jó´kwOXÃ—_¯¯Az€×à…d±Ðr¨ëŸßZÎWÃ¢‰f+1À–:k7“¯9Ò`fl6¸sˆÆ®2ú™¢47;be*‡ÂŸ~£€²0¡oßPT§ºsxn$ŠuÚÃ¬¤ãË]1øŸ>©¯èøl6‚9Ìþí·Ke\ÀdYù¯ÇXi9_¿òÕ“Q*”bó¹Á
¢;OÔ ÛKr}|8rëiì7G~øŠ–G*} Ñ¼x2M„ð$6;Š3ÿëùO³hí˜t{ƒØ±²Â¼gŽŠc[š¾˜.9<ãõÏ«T>Pwß<ÖáYn<moOR˜“jvO.ßò‡\ç?úþ>8¥'o‡zid[
»õÉ˜òú›ÇŸ+¢È5ð§0[j7õPqPÙ•—	žQ5®õ)ÆíÖ9/S'.ü®ælËD¸·)¹õâ«w‹Êî÷lŽ/˜66tÇ)/euÜJ÷€+ ÒG $†SUð–À"
±;W`V  nÖà¸ÜH™óœx'µƒ#œ>i¯=L]þa¾Ò€ó‘-Æ‚þÛ
ÙCã¯#8~Ô¿8©É+î‰Üx`ºåR¶ V×¹gª/_dœû%1GÚJN^xÔ5¨™(>ÐÙ›ÀêA¬ñ¾£3¿«v!‘Ë»ã¢}»ÒÜr›²&ÜLN—Û?óÎ.FOL„LÆVg'w±P'î È¦³ˆ[;1]µ…¦J"“>ô÷ç^œ"ž{fïì¯¹?ýÆÑöìTýP©oö$ò}¼H)9Ë¹!cqžJ9§åíÉ[ëÛÛ)g,†mØº]÷gÎ.÷Ã@*F8.ÞIGf+2Î¾ò»ÅiÚ®]ÚÐÿôÅk,—bk¯õÍÛ}O‘]¡‘×ìß¼±éæAôJšr#— ÛÝ®³~8ˆŒýéÍW§ÚšÉ–ûú[)KW¶ÚW‘èG­úí>H†0*`DÌïØ¯d5’UtËœ¿û‡×dÔ 9­â¸¾yËáþ™í7Â^ì¿èY¨²ÆÁEº›ïŠ;KŸˆûµ[^VW]Kæ-ü]Ñ¬aÓMÁFb+˜–<¥Íøó=/[ôÛ»M‘}é!mózß¾Î‹M9¬ÆHï.¢Ñ5lÏjøêÞÄøÌx3‡s9êúÙud3V|âåéSŸÃ›„)ò²²Gt 6ýˆ¸áM~s7ÆÙdºNOLY,HŸ˜
WoSÌ"ÕŸ~{§`:?ÇFõL¥¬Z»ù;sÎ‚¼t¤3š2ëd|¹æiÍÙRÛŒV³Ù˜î°ek„ì8|Æ`’|âaVçjç\™^s:'qy½j×SkºµùºúúhˆÍÂÚ˜ë3šVqþ»çÎ¿Åå™å£ ¸ZýÃ…­ïßÔ,ŠÓ'Ÿœ66ö)Jfî‚þqe%«X¾ùÀs×‡{›RÒ²9Ýö  |K rµVìšHªh,MËõËÙ^ÎLÏ}.ŠL úÅ³ÛÛWîgÊÍ27uF—ãÁÊ.&ÈA]þDxNFÙ¤[¨B³Ç×gªœ‚VæŒÊ	T"ØŒ¨þôûwÓaÚn¸ö‚VêV;º½¬py¡@Îí`Y{®§fÖÃ
Î"‰ÕÈ/¢ç8æa—Ë@ÚÇ¸Ù™%0ziSÛ£þççŸ:`i@.ˆ‹çNÝD„”[:¶ÊIWÃÛ„„É$ÄnÝ³€²+ƒyš)Ö\!’R÷¿ûÅÐ°ºå
1C3$°™Lk}RÍ'œx”ž4š™{ûÓ¹`'ÖÒ3míq`¤!Öób1á>2/7Ñõ¦v/Ý•µÁÜãœQ&¾}ÂwòíÍÒ‹@ÂØ™DrK²;úŽU ÜZœ+NÄxƒÍqê™4CŽå*ºxVà¯‚V‡®zî‹´°õ3ÜÝ|a«L4}$ ·-¶hÍòå||Ýwïnt2õŽdo,M¬3w­±êÄ÷/„¦”|¬M"rIÆN¼›˜v»ø.ÿzôäý[žž#ü}ÍX¾ø$áøçk¿
 ç^€ZcRþ‰»rµù  ßÀÒ
³½\`ysB¤µ1ë>Z`‡²\¹|Ôã9ìÈŒÅ±]	ªìÀÑóàèòÝRÊI Ù5QOtú,)©üç›ïmÄv/Ü¹7$¹˜:XR¼2kÝàÍ `yª¨¦’NdÃe]yü¯¿~±.%…ç#â}¡„ïÐ§"‚IŸ¹£¹Ï<€Æ—%à(>Pßƒiœ¦š‹ÛPµÉ0ë°sîÑö±tóI¿™ç°Æí 8
$’äÚC6K6ZÂÿt×ð³=£kuYÎß¥|ÿ.ß@ÂŠÑ?½óØø8G¹‡4mQxŽlµ*^ÊYæÁî¹Ç7…;6x,l±Ã$ÝÕµ–Ø—[š•ýéÀ¾cT!›O<@Ú¼¦}žî<û1h‰¼šŽ(Y*‹F”|^ó`W·'‰œmîŽ;“´8Nÿ¾ýð?þÊ˜\nåº=+9Ó‘‚Ù=;ÃF5ÌŒUã”›OLÔº}NC8® *RÙg=2qéCl‰Tª|uÌ#Ø&JW‡;ðîWÈÀÔ¹£“ˆáÆR·Þ:®wÒ®4Ï3¡b!`W’ÓÍ˜¤UT¿ªÖ¥ÑÚ ýË‡þø!jg¡ N€2Vø+õÓo”†^WÜúÙ\ªBA`_{âø»“%GÂeâ$ðYX„³²ËŸ˜ðþ÷CÏý¿_¼x_zi2awIó&û€‰AÑãµ÷+@Š  	â\3qY@:0æk2XÁkÑ
 ÅûnjÔ
!~|Ä?„ýèÉwY^:{ês¯0áj6*Ôs|õ©Ð&˜Ê¨ñÍDµíZeóI¤ð.^°pÊ›¿©õËªÓ¯¦ÖúKÕ+_~0)¦0Î?ºŽåÆV‘.¶H	fD¦é\MŽìì,é³ÞYšr-=Æ•I‡Üõîzb,Ùš¨yæ¯<?å–®/_y‚ÉO ô4eÚ*Ä&òs;¢ ` •€è»ìiA¯Ê3š#E4Wí´ü‰VÎuõ%³P×dDn<¼PÒˆ' æ8\Ú7âøÊË/ªâ©Z”§ÜºK`ÒgÍz9'ÕÚìàªRíŠûï,)tå#¾S¥Ôo†˜QÑ#ÙÛdúŒØïáÏîhMZŠ0Ü2²±œAoEmüðÁE;£YÌÞ–drœQ¹¶rÐk”ùdx~» Í¬ÌMóZ±¶`Ô¿}Ý&°/!fIÀj¬+^	·bXfž=?tYùÀßï~Í €¿¼ŸUÔªç7‚LÂîÖ.ØM¶V«;»	NQËê¦AàöÌÓø5šâ¿øÒöp‡ÞéXŒk£ßÀžx,üË3ïlÀÖÌÑCð ª“øÑc(qª5÷;¿ nþxïVËfÞ­ã£ÐÐVÅ£ÀWSWE~ýÃAšcE’Ì49Ø¨âÏ?5b¥°	VÁfnc1¥E€6¦\€ µ—·¥YÌ]S“AƒÐI#¢ìÖ
ê`LýûÇŸI¹¦mT¦"ý†uŠÑéz°Ö2¹¹õfruòÜ«ËÈD¯-
×á§·­ÜÎ8#ˆñaÖ9¦÷—Ãîº¥3f÷ëe
:)àkÆ‹ië”›è§C|U‚nXê¡™fXËÀA!>2G]zD3!ËÑû¨.c"@‘%Tžùõ{q‰³"ö“ŒÊùGFËj…µ­œ•ÆÊ4Åü¹JKò7ïyÔûö,Q¸xù¶|Qõåã<NÕä;ûd2Gv-ŠWÆVçá:>µ"ˆ„)Iqæ3f…žÅ{ö:sáÓÀ¡/X%©4‚uŸuê7ßÀ;†À&t†µ/
OaC0±„?‹†¨œ¢6>ŸF^|JÚÕj©gž¹òL—îÚL]„1ðâ˜Vº€®óŠ)œÇ	¬+&f¸Û	[tO”íïv3©¬öÔý5¹æâÂýÔïVÛtùÂ…Ç¸†ówÑÊEÆÎ_ŽÞyóNã`ÜD½|éz(€TÔK¨	†¶hPØ&ŽoG»½lE®UÖµôÚÕÇö§RØi/¦ÿöÎ¾¥èJÞèO¿SY±“Â˜”¼°R0çÂÎÏÉÕàtÌ;Z¬Ï”Å9KÚQæ2íoG_;w_þ³='g‰Jq2Ë­?3›ïŽàZ¦fºƒ­×ŽôÜv'J±®Rÿø\¸cJ»	(xWJd ü|{Q:ðì¹Ãžo:ù*Tgð…ì:0]Àf…³0ø.%`†Õïqeni?TÂOæ:›;¬ŒwåËgBû3#^¤ÛK¼üx¿%-ßzbº³‚¦5Ã¿ÿâØŽyþ·Ôýµ%ô­MÓrß8çâ’õnÿA@åØùµ¿¬!üñÞò|Ø œz¢šì(Æ­kë›øÜýï¿º—ß³‘üêªÁC/%¬«ËéˆŽJ`É`V€Ð›ÃƒÕv Ôüß;P4gÀçÿ>òIÕŸvš§œ“àµHâ{ºY¯zÓ;¿8·¹ D+²ê2]q ùÁ6kEßµn„G¹ÂËooy9®ïÞ^M„õÕNÛS×ð<Úâ¥'8Îb<Ó»|›ÁYèÅ	¦–a¤›à§-ÜvdÌVÁýëÁ·òWß·w|œòöƒžõ­%-;Øþè(MŸx¸8m!¬ýë¾_YÊiÜºÊ‡CY©Å ÞÕ¯_hSÒÖÃ?ødíðúï	†¨MÒÓ­Ž#¦ \ŸÃÔ¸Q¦T	'øœ¼BAJž^\ƒ ÷ÍÆ]ÛÙÜv¶¶ÖƒMG×Æå
æ2¯;nÿ<|ˆoÂÿç©gþüä[zB8	uI—âT¿r°CÜ‡7Ë-T‰dg/5eIkY·L?~ßäÂJòŸw|òõ3|ÇÂÈ¨Š°ø×ýŸ³µK¾ÓïÍŽúËBu¿¹·‹%"šÿ~òes™ÊôUãšZ&×þþÃ iƒæÒn%%Ö•’¿‘°ñ<Aãwy~xº¯^{—Á5úÞùÃå‚µuå‘¿ˆö‹Î<h
Þ›9ûÐ:z›¼’®ã±z;”CïQ†îQƒüûÈoÕ3ªË%†ýÅ½}Ml_~ñž1·ôÌûI0ºfVjI£X)|ƒÜl8.U5íø÷½O‹ue;gPSÓ–m  #Ôºè¬0ëÐ•0^ü·o¥[Tc…+¯¾â-úuÿÏ{÷•]xÒî¯;¶Ïßµ´s€A6	ºPK¼IÞk«yÁª·Êçî0Ä½øPH¡ eÎøµÛÅûf7ª™`mÆ¼žKúzñr·O‚rF–%E9ÝKÃCsw…ÏeqîÍ}½A
Ÿ9wÄºšïTÛŽuöj!qÒ"iËhâñ<¶3·’ÓìHË“Vrcn¸í†æà#¢V@ô‰4?Ñ?qw"Î”Jef'`¯ÑfùFñžaÖ¦ËÅŠh'mÛ.´Pˆe
Í`Oó”EHÊç&|IH•Í}ÿ4ÙŠØc.ZtKÝ¹`õû'sT 
,¦’1„(ø–¯Ýž›L¹‹k%k¹øéƒFŽ+>ñ®Ì2œø…|zêC­2_n\8’ŸOp{ êábÕ´¸“Ù„‘«À÷o¸&ÑR­-7ÏÈìŠqcùÇ‡#Í"8„k‘åkî„S½W !×~ùÕ+^qnäâ«>w¿( ïVoÝ¸üvýåÇºZÍÛ¡ô\´käŒóÂ5~ýßß¹ëPFÏýZÝ¹ù¨—bP¿}{”½Sí.=ÀUmÓÞÑvYbÞai”kÏÖ”¢,‰Ešç!™¦ŠiX<ŒÞú96‘Ø}¹Ubª°ýu¶¾„ŽÍÎî \<êCÃíò.+§ë5Æ…ÆH‚ÁKÆ’ß›-,2T¦fTºEª›ùhlY¤ÞNû]T}¥/±.L öÞ‚íúç7`IB”$ÞÚ$·!nXgryÆýÛ±cmÇÕ£‚z³+œ´ñ£Cð™c°C°]¹ut	Ôå é›£—Þ4{NÆ”ªª¸> ‚;<y4bç¸Q€ôªúLÝ!(»†2_ºùÒÿ¹¿…×^yF™3]írWÖ’„èÇÿxø®9¹ C–.|õÄF@ÔN_xyÕ0æúêwKÔbïcÛà‘ùð¿{¬É[¾úÎ×/EÈ“ÈáØgFŠÞ.<»å«ðÎ²mUEZÌÍlîoË¨KoñÀ*&·èß‡Ksu)XÉØ-rb§ºXÄÕ—Ùû_¾*fh¡™TØ#…RéÈw/`Dêù[I…A—;W_ñøu@Þö„Kx­¯¨i1”a|ÝmoL”I,±5´+`Îu'/¿³¬àÄNlìÇßªr#ÐàRž ž(öPD—½‹×’KÇÂ†Õ:wÔg£)à}(’‚B¯|¬7µ Œß[, ((5¢}2	²	Úu“ÝµHC,˜‹ˆžÆÚqõ{«Ó`¨eÊ4¡ìËxcÅ¦šœ_ZJ6;ßþ\&xHœ¥* µZIáÀ -Ó»3Ù‹Ï Ñ€oµƒ´
=ê--üëéÇÜ_ßvâˆ•/ácBº“‡MííÏ?+åªÍ"Òc‡ãbWqá³%etuÍyõ25“wÌËxºCžS›|yÜÆN$â…D1©Ýr,­FeãEèàä‹®Re³R I0hc!^—ðþÕWaô:KŸQŒ×‚
;ÙE.[=Lu&…%¶'^ŸÚ\(&á°þµÏ¶Ùáz÷®—ø£!5äÓŠjx²bÑWO>‡Mö5†óÏ€'ß:,Á¦Ê¨ŒNš"3‹òœ`ƒüÃïñ¸2k Q ³LÂk,…–XAìÊÔÊ7÷ð{Pìˆ#?bÚ¹ùFŒTL}ÿ)a,žîàg-13rñ¨røc‡˜"†ëî—û½¾M‰ Äæ!u]@põ¥VEGnƒêÜ+ÏÅÏ¼Iš].‹œCu17âÛ	Ë=ÑÙõo>¬ÿã™/ä<Óàê@Âwàw•%”=G:ûÑñ—çgTí†¡í²6	3Âçêû¡œ¬êD'lzµ0ÃÊ!q–9ÿÍ=lkC¥_e[J¢ÆÈWÈt²õ<ŠFÿõú—ŸÂ±Q{5î‰ÑÈž^ªïŸ}j´ÞKFw9{áääóû{è¬`ªT©ˆà‹'>òX‡> Ý¢Œ±iü2dÛcƒ*4»ÿsï‡·Þ3ŒPdOä,`ˆùa˜X÷Jº1†Zc%âã¢Ð™_ŒÉgKûN	ùí‹çîç^LQE©AýØðÏ/ÜQVå5°?ýþ“¢ZJçÖé…Ü•§%âp‚@m²™"ýæ7o‚uÛÌ¼×š!i›³Q`u½fZa¿zÛ»Ö¡UíÉïîˆŒµ1ØŒzo”ZÜ		µÆl*Û‚UÌÆ,ÄƒPW¾ü¢§ó”{î>îäbÆ1¨8úâ/µ›üŸ^±GOßÏe@lŠ&ß…íŒ®µíVc*Á#õÂ»Òr:­ŸN?ª(A1ß¼n² QÞ¢·eö×&VÑMƒÂÔeÇ6=“ÝÑ[÷Èw±Á™úw¯DçòMÝÍÏ"4§ª† áîê(…uÔ´aß>Þ›úê]V¶·Î6ö·°7îë‹MèÑ +¥+íËÔq÷Úé•¼}z—0á­- 4a.|ýžUmáêÓ×ïüúSpï$Of–å)åE7ÏwàŠ­å¸äøož‡CxéË_Ÿ½:%œœ©Ù¬vn±W<®áQ›Øô±½’‹lãº-Êâž¸jp”Ë/‹öS€1Ÿ‚7Ðìœ¶!/åÙb Å¦°ùÍ¬ü&dE@‰@óòÑµÇQ”6«±´’@Í‚÷AMjF7V’³þ÷ý7$P:‰ Q‰³ÀBˆ}â²!ŽbÃ‡Œæ‡W»'?…ð]†ÿxø‰¿½ñô©?65‘ìJ{©ffýãÛCëÜ—¯Å•L=©Ëm)¨çî¶DèFTIªðâŠ’–eZË¼kÏPº”®m3±±µƒ}ù7_;f½üÆ×ýªÚWÄO¾ElOÛlg^Sh\L-®_üée&¿ÕZÞ·˜.|á-!÷H—^TFøª¸ÎWáò¢ØèÏ  BöY&z²^î–pvYóÕ³eWh‘6‘FtÂº*,êCMêŽ|a€Tdã!)å€Æ®‡)£yn›¨Gç&ëeÃ7‰ËÉ„=ÍË‡FŠ2!Ò-ºGäB:ïÆSh Ã^MeÄÙ<ñ5¢8ÿ˜ïÚcóKéP?g÷ž<z Ù9óûÇ÷#”9zœÒI%H®"Ç›×ˆ€Íì?õØ"gåün„=„P¯ÅÊkvt‰#µVZ2faÔŒaÄV¢+ÃöÆváÃl0þÝÃ&‹Û²Öó@Ë&@ÚË–‰™ÈÆ!¿¿tþ¶ŒÅS˜3†xh•©@¹ääé7OÝœºÃG[öÄùÛÖ÷Gú)½¢œ{«õÏ·îtWàh ÊÙ"î+Ì>ò„{EŽ_ÇëZØD¼2#¯½¡Ø(„H“aA¶îœ¶YÁŠÏ-]zåça•3pœBÏ–
íº*Áz#bQËRã²AÓ‘SÛ‹™=!8«óg ¶«I‹+iôâeLN¤b‹…á¬épDo‘M¼h 9RQ¥ŠÒVÒ¾8¼òøp@])æ°8áA"Ò¤æ°uè {æˆî²uyD¦¥¡PÞQ\¦:æW.Ü±ÆÜY7›šÙ8DŒK}tÊœ5SšD‘Öåú™ d¤VÂ”x'š&üîõU˜Ž§)ªÀH“sëµm#>cžÓ;æ0ˆÝ‘nq|E7¦ðgQUf—™Ø0cÏþþ»gÕC–Qèó¸E•­0¾ºpíÈp=Ëð+ô0¸þÜ‡´Ísn×ë&MŸx?{/‚ô·Ï_Ýö±§RÊD<ri—Ž„ƒ¿æOj¡¥–„â;ý©{€‚¬ò—òç-#(ªWL›„ÓK¾¤5A¯•òxwÊ…_ß/¨ßÝ­íb'ãÅ=’MNãˆ*U¶2°üôZlïû‡”>òŸ>zþü¯°ì>üæoù»)„^Ü4÷6ÚDik%å÷ö¥1h/’×·)FC¹àT®°åÂtØ7^©¼…ŠÊ¿^,,:!ÞÁd)æÚ§õ'=MÓRSsóv¸Wó¨¨Fá×ßˆMm‚=í«•/ØØXo™Så"—vú@c{ÄIýúíŸãfG¶Ü(g¼ê3O:äÔ: çE¶æš	N¿…Ç•â˜­f0½À±.ðØ«÷csÓñC_?fÝ* §¥vu³êävwÀ=>£ £ÌA¤ª9óñ~S3-›Û•Ë·/Å##[Å®a>9´+Ç,ðÒWO/ˆM³TOÇ×a¡Ô”ÀG‰ûÈYfz¡y8XeºÚ“ããüU°•ÜÙbÐHüEò®60Æ\ÇÓgn>¼ê·Ïø[ÿ<òœŠ²½‚µÀåjƒáÿ^>qÁ›¹–ÛÇU¸¢©Éô\ÙBw2a¨Ï‰"œP$™É †‡ÝöÌ‰·¯>Í	G¬ÂËlÂ{â«¯íeFŠ‘?ÞJG&R×ßÞŠØÕá)7ýíZÅ¹±!4‡¾ÄGXè“	.IÆ8/|:¥ŽÙúäJ–ìõC8¥úÚKËk=ƒlbHs¢
÷O¿¤<)ÙµÛ¹§_¢Ô~x6ÞÇ¯(4kk‚f3–2¥'7â£ô:
o(r+"]Õ—ÛMP7Le5"?ý9"¯°ï‚i“ÅÌÌÖ©g![WºJ*0Z„ ³Oà#ðe³¹	±Î##$æ‰7Íæ6";£¨2WoÝ¼¦­_|¿Ä%Ú¹}ç(­3‰ûÇ‹OùF(Õ¡-½“YÍ$‚kw4½k“cmçîåN`^!fèÍG¨ã÷‡V³f1*ÌŸ¨’g_<àHZ±H‰GÐé8¶\”§,ž{0ä×ÑÄQm.qñU1’i‰To=¿‚¼å9 mv3TKDh\ó³¥ jWŒL]zŒ["ì‹¾ym{ØËÁPÕ'òåÔRNVö²&~f~ÒÃÜg‚€Þìj­ ô]•¶©GÚËN®,LºÜòñ»°–²°±m°Ê@\ÃÏJõÆw× Â`zÔ¬üëão\­DÎ=)&hø?½ÐüÇ±';^BŸJ3ÀDL$Wn[V#Ì·#7O¼ÈœZÍ¥÷h)ªùO/=OÞ±óšæ¿ýòƒ‰=ò×GiÄ-èò‹;k±¸&>vééÂY;9£áÜÛæZ ‚ hþâê€©!m:âÃÿ|ô}çT{%L¤n´í—?‘3òsÞ))Y&úTRAà…ñÕ‡6ù6%Õ¢Øº{æÉïïÔìähþ±Q¹áÆ?þ>£«—ÚºøáA¸vmÛnçŽU‚ü]eÓqåõd°0õHÃÓ¾hx½œtÙÖŒIQŠTÅä0ë„¥ÀÐÆ'Ëúw×ådja-.Oa$É¾w±õÃg#ë¶@‡v™§j4X[Â=õ$Íç£:þ`z6<-š¡sè‹{{ë#Öl×õ@#‹eÈšeÅ%°'AüÆ~†”š/Æ›_}÷P®Žé­ÿôsÓ•gÆåžw=ÌGÃVur":bK9Ækíª:âJ¦–\êz	QŸÝÛÌcXòodÿ°´)Ý¤ß~¦ºð¬„¨|˜Ì|*áìêh}v3iöâæ]¤yjY"Ÿ3âÇFýi†Y(‚ÅÕ¨©)¯ÝüDäF«£¦,Vqò¥ïâÇë–îøç¾Ãd5¦Þø=zìêâõ›·e’ì=‚zîÁZ<²l’›. ³º9X’"Õp¸Q1ã…é€­)ÛÁþ¹_º `ÈøAnAk0í™¼^¾­.1nwŒÅlMnéš—=ô_ð÷×K©¨C$[BaM[ó{¸tÁ3žz”¸žóÉ={íú*‰%›æ€¥Š„*zëHHOÈ2•ˆ\QbÇŸc.ŒlD:3÷&ÆÌ»Ì1 âß&g9ÄæFˆLæŠ€ÞDP™ˆ"ð×ŸN¡©ˆûÛwe5Œ[“îJ´é\gUVDS+µZíä³QÌ¿ú`LS@dôÏYg·Fñr)†7gÚc“ÈUZ3):qÏå£·îUøaüT%ÊÏÀ-Ftpýø[meÿëGwàFP¹òâèDöâ³v›¹…BÊ¨qÉ.^<WŠCìNk6%j{úzüTÍÎ¯Lû‚h"×¨ó²1kÅÚ•zåÌôA¤£½r¬›ÖqëvêÙß/ÍïZªjVJ™SÅ¹í°”Áèrë@IppóèŠËÄ)ØÆµÞàòKÎp©Ä­s¯ÝžVÿãÞOêQ‡õÏ÷Þi‚àJ,šã…2ERxÎœY*¬£Ï~‚˜>}d& áíÃbÿ·ž(ÙˆëÛ/`»±±CðõÈÜÑÒºQID¶•®€Î—Çbaõæ~éü}Ò Ñ	¦¸ŠsO!a„¢…¾ñªÖU¾þÀõÚ‡2Kp^û„ßl§;-—|…™_àñeí–{«UŠVèwr ¸Wß¿òáª¯°JßR'2Ä9ôæ~UÆåê#þÃ­=¶š.å839KvÁEî®å»=±ý¯÷½ÁaëM¤m‹^±ßMpÐÞ¦þ‡/X,µ´ªU«ˆb‘ðÆ¹Nm×?Œg”­1Uúñ©ŠÛe4sÿçM~pyQÐÂó¹Ì Rú÷‘{˜Q¶sÅ §vOýÂÑÑ‹ÿòì=½T?YÙêÍ5õ¢mv±3ÅësËEpþ°±«^ŒM[‡;6„²ëö©ÒËä¢ŸKfAAÍcëîRZœƒ³OÕËk{µ¹²-g×öZå”IPoô!*/ÔhÅ)`òô;vÄzziæ!“¸ë/N(jþÏŸ}*—h&nÁ$,KUÖŒ-þsjÛšÃµ”œ;Ç_CCD¥Œ*ÐÙÅðqeÒù÷ð×ß; c‘À!V‹ÌæÌüa
5Žæ·Rx7_Û®r&C}§UÆµÁÅšäÅ§™Nþ¶i«‘Û_=œ'Å ‘¡‘L%Òàj	sáÉqJÁ˜¶žzjˆ”ÑÓU•V
©M¸SÃ¯î5Çü™MYyr¸#*J,²¤‚IK_=§éŒƒ+
ËÐšfpÑµ9+F2Al¡¬ýñM§Ó
bqµ„BÔ¾É  j|Fn¾˜CîÊP—žúêyŸªÇjÅ­u…ôÝïgÚ3?ƒXÞ7ÈýÐF[‘ZÒpn<b¬'6l€uK0—ÃsR©-0´vŒ1gC©tÀ*©ÕKŠÿüú'˜ŸÄK/TÏ|¼11åI‡7€þõçáùbR§‰è«Í±v/½ºjfq$îü=×l ;g.àçÈŠ±{ë)ÚÌµ×¦e\=Ê•-ù,…Ñ-e§[dåä‰”¶eY] ûzW:!Î¢=^ø0—Ñ!%$mñÚÓ2ïÚ›&Ì$ÏË x‘°a¹eÌ…/<›Í›ß¾²D©#tÏb`ÊÙ˜™MÁ\Ž[ê”R&³cWï”É¼ÀVtÖ=ÏâQs²¡È¨…üðÞÄ?Ž¾Úœ†9/¾M(’=õÕÇG%æt«îH†Ý­ZŠ[MÉò+Jx¦6N¾ =\óg.>ç';¾|¼Ñ¡­«;#—Žv¶wD!1ÖÅ±é¯_J"‡S²	}}/dÃëÕñÑ´Ò­R§z;áçxzÒ_0*¯¾ÉïîäÖ¥ùŽoôr£äCÐÚjùÖët•aM~ùAù¶!îÈr.Ù”h:¤Ÿí `½Ø1g@[Uóh`ßÃ›_¾òŠ@Kõú¬RhÌÝ_çÆÄåj2að9[\a”ƒ¡…ïrÓ,P	`´GJ†²R/óÚ‡&ÉªÖ_ÖŒIŸDoúêýs@%Tìžýïwý~]¡Ò”S2ÀØeiª:cZº|27Æ¶äÚ	¿pKYÙÂ\VM9—]j¢ à†Â'O(‚s—ïî­ø6W·Á'2"®©Ho=
F¡´J$‚rÛ33 ^æm¤°Ä^ÁgO¼»ˆM.z
jU«9áo³§î_}Ünn
g]|5TFhÅQ®3Äò³ùcËl¸_ÌÓS!Ù"ÁßE«ûCS{;€Øúé£±)ÜÔ`„ÞzæwÙa^ŽS_[%«Åô¾¾öpÀ®Iˆ|Uð÷Ï’`ŒŽM#¥â[‚¡»G2Èa‹Š³Ç`sJmA›óÄÃb4|á ¥ÑN¿¸‘ÖÅ¦<ì}mž¦Vê,Ì¬S¿zÛd>}›y><=-!v±±‘iâŒBíf˜ÇÆ‡.þ`$6Œ‚YØjyalNY:}”N™j†%@EÈ¯½Õcèc¼U{pÆÎ6IŽ¦„ŒÄ—~ÆsÈ?îþYºù›i”Þ²|@]3×w"*=ýú³ùôáª#eÂÜ$º½-O®À‚ƒÒ~ÎÀ=yßœÉ¶WÓ†´¨w¶À*¨Xúu_å×Ö#™9?o*p‚|¨$M,M†’PÿÌŠÝöVMµT×kkå§Ú2¶Z¿4ÇM²0;f>H¤‰pzÌŸñìÔåƒ°ÆroÙ¯?°»yÖ4$5¬ÀÒ‰P6-ŒWQ°õç>]ã¢µ#üÚmXVm.¡Û•å×oÝ%ô °›ÇLkúÓŸói£kíËŠ—rb„•ï€"•u
OÿïÛÞëT&»–}gN‹¿õÅÆAÇ<­ÊN
}"Â2%š3JÎ¿öAAïîîò6¬Å1›¼ú…&UM,õ­îìÊ¼Çópœ”FÙƒñC}wéÜƒ•áÖ#Ç_v8™=þÄâš ÌŒŸ|^‡·‰8r`ÍdñI\´@®L^¹ÄŽWÏ¿:¾=“‚«’»ãŒy&óÒ³”MjÜÎV	ÍÍ¶F¨‘v¾þ6rísûÂdpGº”+bÉÁL 1=R!ŠA êÍž¼cƒ;Ä€Ë3{õ¡½æ_Ž¾J!ÊýòX(aK•á›–z¤}óà¨ÔA‘Y*ÁåYv7>ÝšŒø#­å¾SõöªË Ä¹
"¢.>Ùþß7_ç"Â.~*³\*pºY+mDÃÈ5c£ÛI_WS•ö,ˆA9Ö/jTQ˜³?{‡(´8Yêº}öÀp1ÐÝq³r}}Q­&×G5Ö™1o}*²èYƒH2}<ÓÓ•QâbŽ¹ 
Œ147}þiõ¥öüÌSÏ“±¼±žçZÍgß’Ð@ô­¨{þÙ1ÁñÛ¬ÂŠ/¨J;-z™ÕQó(…g©'ÂtÛ™³w ›ÿõÂš™kÏ¦Ýý–Rßi-´Õ´‘.m]Ô)y¤Ýe?GÖó’W´ç^›g¥ù\€¦¸ñÄŽ‹„C†Áµvˆ-ØLþ-iz ”’Y‡ÀB?-Ýœ†onÜì3K¸ü÷wyÉ»ÌbwÁd´ªcÓÅ¶ƒ¼"åýë¥_é¬ß<s˜Z°ªæNà€4V”§‡àI%Ê]½þ;Tj^;îèv4š3wW"±å¬vz«Ò"š'`öF›…nŸx„©ÜW‹ÿùÂ[K§¿hT[Zš·.“x¾=2ž0¸R…úÒë[zqFÐíZO~°ž[­'ãó¢eEåð 8æaó»û–ÚZ¢§W:õ¬‰Mð¢TžXßÈEñõ<ÊÝeG‹+»ˆ±•9ds8™ò±½¬ÜþéMW\|ýÚõÏEyripw[6í/C±k:<†cvN|ÿé	ØdJú÷O^kBÝväŠ·ß9ðÃLàÝ>(Êvl9pâ‘ù)iÐGíâ\×^îõ7žd´˜ñMoÁ%€½S÷ýõ…‡hD­Ú·7ÃI´'gô\WßÚ–O]‘Ù©p?N*òºd€6¼ß~\üî§éÛ7£”XÊCH’¾{ji ÚâÛ8/ã€ˆE4Ôpì,z=…»räÊcÎêú2[˜ºúÈÒöºš}Ø4•7~ï"i=ÀGÅ2Ûa–fË-ãÌÞzTsíÁ_µ–óào;üî~M£œoÓSÌ2ãÂC“UK˜LÿøâþÃ©‘êÚõfCxì<z·pŸpò~ÑJ V{|A"WÀÃ<Ý¶¦ÿÛíŸLf½ªY™Ì1èt[ŽÊ‡|L­Ž¡¨æ7ë’Àb³­¸6À*ñV¿¹1Ù6J/½ÞÛ9ùRTÚëæaÓD¬ëÆ§j0¢c%­°ëè=çwÏê*Eê°¶ ÕÐo<^w¡~YåU­Àé-Œ¶„v b°@>R7Y3L•²:ÛÀìŸz`zÔyíù‰Ú$`¼lHIF^!Á‘™»Ý™G§…¬ææüˆ·Y¢!uüø‹•ø?~ý²B¾²Ä™?L~JÅ¸;ù‘bøÜÛ3,§BÐ>„ÑÄ¹ðÕÛ‡CÚvf-²`$(SÑ¤Ó%#kQ~€ÚOv%‹À·GL® 88èTd–º¸Û¢UãöÝ©ñˆŠ@›¦· °F™µl;à¦¥¡Ÿ³Ø¶P†äöl¸
ñ,I_×Ú>÷ }71FhrÖWD?züwIh
7öô_=IvÙ‚nA·Vtiò8¨«»ñÀP“O¦(è”V1ÊÙ¼õðßîÿ„'†}ýÎfB«Þ[®VíœŒ¸Mì=½ÝI~69c¡ƒ´äÒ1«õå‹míL<×)µðéS¯nña8~{¬‹ªL.rQf¸ÂºÜxÉ©OL5ž6¸NIù´k/!|Wif“7%ƒS÷Íà½7^ET‚ZD¤¶ pÍ]ˆB‡&M ÀåOXL8o¸\¨éâŠîgà8ð 	“¶+R³y9¼.m«GÊSˆ…„ØK@ÃóôR=h³;½>¬nPk^F·tå6ô6	~áNïÂ\±S„¬"ÝþTÛ8.JVS«DHzþ3†AfG­{™)EÛ6d„YþæZéìÓ½©~¹§f´ó„µ5
Œ¢3,QÖÍ°Ç¦G¶Ô³‹À7Ïw9;ÔÂº_5^|ðÛg¨2“ï›­o# Óß~¨êŸ|xf°k2üŒ8y‹QõmqSfi3µªzlòÿçèL¿Û,¬u/Y²†WZ,k¶$KÖèAƒ%Yól4Y–,YÖhÉš-Ù–5K†B¥H HÚRJÊa(c¬•	HHBB2œöœÓrÚÞv{ïpÓûuÜkíg?¿ýáÙj,8ÆÞyìÛÝ	{,©hÌ…ru-£œ›ûúG@UIaÔ8"æ²Ç‚ùôº}ª¼þF(‘C§ÌŠ$Æ´$½ùš–Ï_úi%-M€ê,#@-üó±Ÿ%…Ø½VØÜÒÑöHr‰À²tºWO¡¶Å|ØrÎ–~sŠSæ¨BW_šèîÅ´úŽ©›©ÌMíÆ¢îÄb'Ýô|ö:Ü)Sƒ)—Ž—ÐÙ÷Í"

oú5àm|`Ê»é,A)¨›G¦þøòÃ±aÒh¤œBlÑm}bÔ¶AŠÔ
¾ÚH†1Ý()w¾üø@}öqÓHŽîÐé„ aoß¦½yÌ8€Ý¢&4îCˆzÕ¿ƒ£cOHVgiûrç§¥CÐÍŸF4ß¿ðïo¾óùÃN›´£Qj¹F„ÆÏ›`Ûöâ©éQ¯ ñ£¸Þb25áè®òÚó&Äõ_/ABRy#ªåm¼Ã Ò,è¤DÀmÔñ©t‹&OÊù‘#¼·ÂÙÏöyÉäå'=›>ñ+Ž –Ö¸ù~Ð­ÛÄ(Ž]yù™¿Ü¼}¿#>+‰
˜˜øžT}õávèÂ{áL¢0
I~ùP6mom»ÇµT—d’*C*Ö3O›áZ“ìÿó+
O¿ÍôsÁæ‡È×¡ÉK‰Lhÿ‹Ãm7³Èˆ¡ø	ñ3z%—:Ü¸ÒÌ›”fÿùòÓdšÅd] _~Ûa2n£Ë5^2?æm,ã×›ÜíæWLÉ{Å,“Çé{âQ­Zõ‰¾zà ?	[³r{#¾•õ
ºRò&§c¯‘“ü$'­“Mô™ÌôprÃ]Ë°YœŠ°Næ¤IBˆÝÂ|ýpQ|å¨èBíˆñ…g\Î% î#ÎOaäª"Ï Õþ¸Øk%rÞÖä¨Øˆ_À%§÷áÑœÇi¤JUo<e&a}Umc¨öŒ'9 U<,šÍ³¨²zÈ¶MZLwR).µðÞŒw±¿¡±í”˜uÉÁVúî#QÕD®9Cß¡ž=–à4U+Ûº[Ð)íB†qEu@1óPG”l¤QøÙÓ÷ïaÏ²“›kþà—A&ä©5Q(o8Pg=Üo~0³%Ÿq&r ¶:˜VG†¬‹g‚H\ï´bß>´^/Öïü:ÝKÌ%Éc!&\äÂu¿k3UwV[ûß‰êÄÆkjB¾	ØÇÿûý'¾8±5è»%[Ãæ 1zùoÉ(Ö&
æF8û^‘»[NQ;Ùú£FÔ¨®¢IÚÝ£‘Ed’õCm{”-‚îŽ¯‘¦â÷GTîê\-¹ä5àM>ÚÙ“éwÀ«úÂÒ³/q®ýžÞqiÂýô/,1Íâ™.¸"Ä"3Á,êú³l|Øˆ8`Êí®Ì•W½AÌ”z¶+¶áéÙÐÕçâXÉÆ sñÅÐ…#2Xv¾ßemRh-'žåö;ÅÂø Ðb3Æ‰ZÑŸqÃæp£9V
Qøæç¨t«4’³S=€–~ëePÚMsó=åˆ˜Y¡é¾ý`ego&6¨)³&~Yª}þÑÒÁ¬óÆjÎ¼ínËyøÉ’àžhÔT‰WkIÉµ¥¤´(0‰ÑaÕÔqc‹(²d9íÂ¡x1áÂŠvÖ_^@ÍãçáÙ	øÙü[¢4±@¹q\rÐËÈ&% 1/NE4ýRÇÆ"s”‰=¦œ…o.˜®¼k“iölèÀÞm¨¬[ÌDECÓ¬7çauð²€»{j«ÖI9/V‡÷ÆH#*ƒ¦êýû±SÁâwO÷ù"’Oëô
Cº•¯à¤Ãá½†8ÎìÕ¤+×P	ræÔ6m ±“	O>Ãe%¹pœ--ˆÉ´æóû×¾¾\l_û	Î^0ÇIÏ„Î±¯\=â·É0;ñá!_±8']ØŠZãz¦ll©JÝ\ýââZb}·£ÚýÇƒ‡G!òÿÒÃ¬Fìo¯¾—mD³ó(ÝwïúXÌð¸<ºhvÊ’:`µÌóÇ°g%˜+‚­áfžá5lÍqû»EÖååÄ°A‚ ZâÆq:V £ˆr™?rîƒ(§?±Pø<K5Ûì­:óg'=«¬¤Ü—æËüLoÙ`¯Øû÷Çƒê3áº¿Fn.V©CÛð’|çï'Þu +òï^·äé°lRF&ÐÕ÷–¶×¹r(¥nmZþøæ)ÿÕG5\ÿ:¸ØóºµÊ*šQÙ ¤­Û¿b[«Z}î‹#3õš#.Èç‡Œ·.Zeì [I‹ƒõú÷¯Iæ(ÊÞ2…–iØi¬mSñìbUGGîþn¢µ€M¹öùzª[ØEýýýFˆy¯Èµ0¶læàÖOà Õ÷ä<vÐâÑ´Æ/%ÂW`n¼)Ü…âTüó÷qR¯·Öaå•.¿ÔÔ­™ç`{R²Èð›[6\º3Mw\ß¿@¼qª	°À`­èæ©˜ªZÑä€eØúÔÒ^H@x e‚ÒkD˜³¿¿þ»Ó¯ˆQ´8ÜÞ«*Òº<€ˆ—´ÒSµûD|$”©ÎÝwåðŽðÎSíO$rÝ®º¾²Šy¼ëˆ¹5½AÚÅÙvtùùï~ÝäW/?råù\Ð.À®òl`ØÍÖñ«ÕŠ¯xŒ‘K?­˜©èXbØô ô³ÑÞ…†R
œ’²´9·9{ß„kÊtøUÎ!©žƒ‹ï$ÔåUŠ]%@üÄAÔ¤ä£<CVóù±„@•™¥±î>¦Ï…¯üÜäKåRcskî3qc¤.fó·÷ž	£¡ihÑ
è§†‡Ðü¹´wå‡ ~
>8ËœÌDÖaâW/~¤fkñ»v²ÝµìåÑO_fwóˆ˜Â³<
ûÛkÏ.7òºÐk©Mïæ&Ã÷¦DGŸÞd¤¯~L×ì!!´6•$}û6ƒ©Èäù…±8š5è\~Yyú$Â|ù>ß46úÂÉ¯¬Ý°uÉâà›÷6f[c(­=¶ožlÛ×tkÂ}³,G”%-6‹MHXöæRóxÄð?õpI9y¸úûí¼=&Î#TUÁô÷ZØ4.k/lEÏé?`k`lžj1`1Šˆƒg˜9J†^-ft”¬ikl!+Rô,e£/ÚXæ6‡¾|£ZÞnKóH‚Ë„àÙ¨âŒñ žqËLú™éÚ}5M×RþvòáÁ_NükWçˆÅ öÊ¦T	4ÆK”‘­¥•¹FqHÉ…ÙÀTShø¯ôájIb÷­"¡+Úêº8âè;þ¬2¤Hº´+Äv&<À¦ 4¬©/Ïx)ë h•@"šAIäÓ«àà-2¡ë .[a—¢Û	ÖÚö}(kÙþo?4´ªO'w;`,kEUÁ´lZLÔóÕkV¶Š¹GEã‰:ëÎËç?ÌXG¦‡:ävÄDæ;‰@º8yå	UÚ×ŒW•°FÏ–Í‰÷Ó¨i¼Å@ƒšÇ·Eºtt¥Ùñì`}KŽÓo¹5Ä‹ïGìöä"Å†B?ô/¾…wB«sNH¥¢1ã2õÝÃS¾nGå¾&–§oÛæM^7#Ôc.¿0yúµ•ÈP†–Ó%ý˜¿üòƒE_¤9ã®=h4†¾<F]?27héYÃLh,åmÔ°Ó£€8fqgÚ)â½Msú„vëP}Õ‰¥%€”4d*Ü©nßà;äMóÒF‡±tåˆ´Œs›“ËWÿMå_¼ý€™»ß…°¤ËÑµ–ËKÆzÐèØp§¡eN¿Ó/æH6º‚õé1llf¶E¨¦AÑMhçæ“Ÿ¿™˜=ó‘czºÑÒ®Ò1cžjq;=±)HÅ­·jô¤.þæ“Æ˜¶XJ£1ö³ç©a[†bÀyÂ7Ïïe…[cëÎ5$öÎ/°µ:…±Ì°u¨¬¨Vé"ñdGØ!eûÓ»mÚø›‡‚4…]æÓÙùC˜ƒ€XUû¯¯dçs°±X\µvæ7n@âäÔi¥ØX›G{q"6‘äÛÁ·á®	ÈRÃ(ˆ7 f ¿"
5Sy7)¬ÒP„½Gio¦K
“6)6SçßÈ&HE¡œOó&s¼j4ŠÚÅcy)¡ÔF«HïZšrîîÁÊ¸Å¯süßGÞxkOÕÂÛÌÅX-`]¸qŸ„UK¸áù>v¡–ë‹;õÉ(#4nŸµ‡óFŸd~´»¤Þ_šÊÔËcG\Lª}ˆ:£Ç ¨®Ñ’Ë!ûâW{=mv_¸7; YÏýüÔÆô¦oƒxå5«§tç·§Y;Ú¼À‘»û¢h_?YÆK]ûê_|8}æ×ñ<»É­FÙñ™g¡3¯Pç	Õ‚…³7ì õœR R‘´sm…VïØîÌÈèôÁ.‚ØdlèÌéÛ÷Ó´úsˆ’ŒFËÐíWÏ½
†¹½´E°ž+È¶°í¿!­Þ|ï¨MœÏ`®<ê+s ~Í­£eÉ×o‚ öå=î»×þù“gÒ“¥Ä<ºV×OÏ«N¨Þ^pP‹ÁHÎéí]z‘2ôíkÎÐÕg	Z½¡jw¢¿}êæ[Ò‚E³ºçÊñåÂ Ú!7³"—ëm®Ä¨fAA”œ#Í¶+µÝ€ªÞ¹úþºÀS¹Ô±]±—µ;cÊø¡^Þ~»Ø\#Èê‰4ß™3ð£¥‘;.©eÂuÜúN§‹¯®ÿðã×EH¢`
Fšÿóè}VÌ¼ˆêž¦þø³G>ûŽ¬ç{³Cà¢‹…_ËÄõörÉ€•2ªÂe¹äÊwÜÔY¨}b¥é*XbôyéÍ+Ûdm±‚¢–èÎ}_<§-´M	!Þ/CP¤Ê7¯Ð²±~P·[ÿþÉYŸÏ†(úß|”ØNì¯ÀÐ‹Á
ÙR	Ùç¬Iù³¡ýïŸ,ïµ%S9éÈ?òþÅßö¦ëH#•žØ­PÑÑ^!°&bV s+••ÿ
°žfœ?gE]¾@Cµ,&QløÂ¥wF‹EÞ W;PKÚ,¾ûÔò~%Éî8Ù9åpÚÆ@@Ñ ðAî›GÍåTF±\ÐdýËBZµÔ©Ú=÷[QMj ïTÖ—zBæþ¹§›äõÕ§ï;C.gÖný¢¦J÷"å™©¨Šæä%&èû_>-Qú=‰-cCyÊÌlu¡Nš5˜¿?>Åd—G´ïNÊ*õUs/WŠ_?_–[
$lté@j&Í#¢ï~‘ÀÌírÓsÙ¿ìÓ~\tíþ…UÙžñâ±
¢bd]#.âtqá°­ýÑÇ(ýy´I—ß)©Ò¡iZ†HøR.ÉB‰K:8ÚfUxí¡YÒåçÿãã£XfJ‡¿ó–]¼Œ`æe´½êÄ¦sñÁ)Ûr#·h¨ï.I/>Ÿñ®Bq‚ºñýO¤2<v5.n4Äåñ±•„'ãÞzbRyëá=9œ‚ë7Î½Ót^8æ©]}@¼»N³dÏÜÌ/ÚæÃ¤ËÏ0dŒ2*lñ,Ÿþ©°„æå‰@„Â ß½O)þ¯GÉôC«Ô+Çuƒ–Æ K¨á˜9{Q©9B•è˜•×õKgu!ä:gææ¡_<²´8	x!…F¹]èØû“ÀÚ¹Gï™¹®IØi,%”š¹â¸FEÍem|UÜ57"2m~Iæ*ñ4ÀšwŽ1á{ÔÚnÔ—€]8{ß`C@Œr
X~y”Ï•àÂqÏP6ÓíëÏÀv&†¼gÖYu@=Bðëc}² 1³ìùê°êÛG™k‹Q|óÎ‹üíJ|Ãwõ™‹¿@X µš¹ëÕ1›Ü\x!žÊ›åÄ0%7¤ˆu/|œòõÄ-B$Ÿ‚3WÊ‹|š¢B3ï
k5Ú gn	r{£ÈV7þïïüÛLzû`xØvúõ^Sö/Å)ÉuoÏOìpRê$Ý*…Nè¦@@¯_Õ5¢p³†¸CZï·;«³Â Y‘l¢#amjØÓš¥kF€—&¬•ÐY;„±”êÖgu´¡ÜœXÏÈ¸™Õs‚
Y¥ }žgU}y¢¿0Áœúgï<šVkt=¹Ê“¼xâ/<3S„þqß!êÙ£‚río³t?¹±äûÕŠ*£WWJkÉYxá^ë‹A¹Á»K—cäNýüýsùþ
<ƒÉÃvswƒäDwª°±1µg½T^A5³>O@˜Ïêndc®¾}bM§§–Uöªñ<–A[Üã.Yž¿Â•M_þ¹¾Ã¥©F*[‚ìÔh›[Ç1'M:{²¼ª€0táÌa'NÀ€­|FÑìeaæÏ¿KH`!ë8ü¾{aL¢_hÕh†Ž¼i—«ªSñˆ,œ£Ë°¨b’µ:
YvÓWæHs¹IæÂÙã£8\YíöÓoÑ%Ã6„™ðÍI/ÞßwRÑÃ^®Ðm"2Ä°i°¾;Ÿ­Ãzå»¿ë¤Šh’6-«k‘òì1;uvõÆaÄ™§ƒÿsòW†Q„ö‚ê<òÁnî@•-8¬ž-Ü6dGb×Ðy[£ß½õÕÉ™nJg° ´Ýr±´AJÐ]{†±&ª//á+3¿ 
†ãVÄvpKŠ‡§Û%ˆÛ„E¤ÄÓþYg*1Ñß	_Hc
…¦¢— ˜JÝYþbØÖäø“Žå]l	ÚÔZ‘u'€æ5ZÑéÖ0°ëHv&o¾kûdÎÅ×Ê08#ò¢í¬ÌP,O^{oKróåÕÆÝ×ÿ÷³Ç~D™ê—“J¿÷úûàÿsè•#1ânÑ›š†c©8¥[¡¥¹R< mìâƒºn}hMï¥W8L¶øÕýó©‹'Ä­Ê,[¯ÉîÅëÂk”rÈÝF)#ª­*iËe¤zõÙ»‰××™D¶µ·ÞVÏ˜7ËS…}00gƒmêçÇÆs]3-dñ†éžÐ>B¶2êü’1¥‹±Ö<œ=Ì¥œ;å'U["·lÿìÑ½úL©ÃÃ†Mº{=KàðŠwWÃ·,q®Ç”Ý>L"ªºŽ	fÐ¸7 Pí·ßÌYB3ùz^¸]BÐ£àº]ì1m®µo¿eã#›“¼–7ÁØ¨m¯rhÊhü×±“6yRÃžž?ØÕL$
Ê©Ï~Î`ÄçºéM?t&"íàÿçÉ>2´*óŸ¶˜™÷ã	 ¦ëµjÍááB‰M["o.
„ß½Å™¯åJ½dg{{ÔÝZ¶çwÅê’©
hÄœq5t_£](RˆõÌµû*
gVm\zrêî[YLß\`(·ëJF&ièÜ!.+ÍseôªZchª8íë_Ï¶ÄÀ=+±Þ8wRÃÒ„¶±^Ì>´ÌdN?¼ëwgCi/Ÿ[öïAÇ×¯-£K'ÉÍÀ~ñîrjpûŽp/²›ê“v·êŠl«k]G­æÂh§RìÜ=j˜ÝÂîšouf0L“zã£ÛïÍ¦ÑÔéo|ußjY.ÍÏÖ§Ë@Û%¸õ©PÎqrqy~E´®Á–§{/ÂòüHÆ€-	Â2&ˆb,ËIPº0'^uº$Å-ÌÖâÎÿ¦*…Úð†uâ"Ãš›™”
g¾9«Îù·¡îN¤Õ|&'™öÇúò>¿nWZÙ{4µÿÂÏöÛÿÆ6³•wîg9å§²h4Aá^ªô›ŸHGV ¤¹Ö:3Ð›¦ï¶RncdÃ¹}Pï®XG¥+Xb&³V¥¬®„0;Ï<»=5ïôÛÙ‚Çb>ýXo]žÎyê8¸q²u€LÂQÖ°æ\¢U¨3e¨l{÷¶»E»À7Ž0°ácÿÂƒJi&¶à•,uh¬Véîô¢H9*ÄÅ’d\LK­ô#Ñh›ÖÙ6vZž´WÖ®ÇƒmƒœÏÈšm;Ãß=ð&¶8lØ·ï:4þyé®°²Ô/4oü˜ål
X|ççÇÎ<)‡’K´˜UâÛ˜§dn¿€ºB(õ ðü‹ËTÛ\ÿ€7™®¡Ô‰À|¨³ŽÃ¸(êBµøå¢È††Nöt0Åœ]V‚2G‚iÐŒÉ.S©–+‡&Q‘+2Íc\oŸ#&©*üÌž¥JäŸ¿Ñ"TxÆëO-9Öe¢ÅVMÆÛnÿ:ÏÁX;SÚ¹.%$jgÜ%jÏbÔvÿ?îÿ’g_ÃÆ¦$•Ë‡aËRüFº§òƒxKÕ…?=z²T2vš‹ÑA‚ÃÂœ~7Òè¨|¶oÌBè£R°nÄbnþ“"ÌŽO²k›hîä$˜Š/k—žCà÷êOÎÙ—3"Lù‡Žì—ÖÅ¸yêXYcÆS‰±Ž¿²NW^;tígÅ/¥Û‘åÍ[§vV+$ðÄnulk°uå>Ë&`\è™¬ÿé³wÃeˆ<ž5¥¦Çõ¨Ô…—¦ú»Ó¹	z*b®"Ôôh¦<˜SªAðÁ ?ÊnêC½Óoyán„‹ÍIv"Ô?{M¶¶‹H./s»xÌ|üó§Ìk?üî½Ù>*ëMzCvá¶Y"Ðžµ1GoÿÂ.€1SDÍ–ên8˜7>XÙ'HhÛÉ¡UÚí·¹ÎØÝàå—áô™ùŠ”H}pç>3ÄºúzJâŠã|7Ö°“OY·å§×tÝŒ½9ß³Ç5ÅöV <Ì‘Ö:¨|³ÓêŒ–ÈkL`–ÜhaqíÓ9{}€$Fâsã rûÁl¥ß7Ûè80PéÛM5ÓóÿÉV9HùövV®=°¾ÝënÉœ¸j±¿äÝ‚og¹kçŽq„NõRT˜¢ßsîDÙþç7¬*Fd²ýqw«œï²„y÷#ËÛ ‹Ü„,ûøYîÝ··¿xË¼´¤ÚSúsÓ£ÊF¿¶p cCÎiJ‡dÇº?û=ìÌ³
‘nh·ß‘9@ö)þüC…	—Å¶ÈQ¦Ñ;/4nüÈ“%ƒñÈäèfk½ÞsÍ£¹$d—ßUw)Â;?+ó<ÊÐ^LˆìáSñ«Gàº )–óû bŽ„aŠú¥{`+ÜŠ„¯þF"RŸ=Ì$À™‡‡VYè¤³>Ísƒ3j	òÚû©0¥ºÆGýé•£ñ|·ŒLÉL’zÑ7áW…P,&ƒR$4`?ÃQ»ÐV	eØéaÉ-9Œ"W©K „%¬àìÏé]:b7#y%DmiQÂã„[˜Ò¬t5ÖÈZøýL0f•“ÆõÞÀe#M£–²ç9sÒ4œ‚Ajxòµ*E€ä­Y`É3¿Ô
#²¡ ‚uŠ'Á<›£Ag‡*VhºD_€ú"À°Ma’“†™cös¿—•KÐv\¼((CU!£è 9©s9ÎÜoC4_2oÜ…Èzd0mð"rˆE
gl6ïg¯º¡ã€°ÔöV1ºIö„íÂ¶Iœg§n=_¸ôRÙ..ö ùü+u<kÞ´âl=ôD±º?5½_ÝüYeôÆÏDe¥}í€xë„½KÖë9Ã[#©„=½:bMÞù$6b™¦ëôÝ1)gd{ï_Ê–ÿyíí7,wš°‡ÖA®¿@µ†°± ¡‘žÐ\xÞY
¶ûsì¨ÍwV6ö–ÝÖ÷áJ×_¡¡¤å#uý#’Žs¬VÓ§ÏˆÎ½té§ÑhõçÑbÕV`œD“Õm¾‰óŸ0.>ç«&fÖ8'lÌ\Û%“{ÌHE['ŒÔóE09"^=‰[6%~†¦hCüºñƒâ V-6¬´-‡ƒ'	)%‚Õ@<Ì6‰¿~›	ë];ªêp9íl{F§”£¼	ÎÙ§Ó)3ßˆ¯Öñ=A-x@…bÝþdù÷W~Ï5ÿýÐË`ÈÈéÃ¼{twåòkñt§.^{Ž]Þ©©e½-„‰~ýã:]NhÅ$…t8³ùéúöw÷çð–8,¼8´G©x QŽ–x W`!¼·â\#à®K[Ü9k3Væ\¼¯š§h ›Ÿw”” Ó¹óÔ"ÒãØ]öî<Z4uÛ‡™c;$4%‚©¡„dT Þ¦p´ý˜à¯<Íóo7Ç•|Á|ºièO×—Õv0áìƒŽÄ¬¼|L4þõã1f.Ü­®cE@‚¥’¤øù¥ —¼T^@yZ
–BFn½èý×ßBQ
î~ÀÃš/oCŒVcf	GaWÉ–i 8M*YÃ2m{†¡›#èó+d„¯ON°Ê>z±‰¸÷8<‚í_ø°Šã%]êJ9›¥Ž„PÿzÀ1¶Ó-Vx# ‡¬5’]:Ç¼þ¬c‘Tvrñ–ÌÙS¸¡>c°e‰ÄøJ²‹¿ñÉ­7yRxŒNÔþðÄ)ç®­.],m@2^ùÕ¯ÂÔÖÜõ_6CC¼KhL²h‹Ž†Ðo~3P•«·ê%Ût°Xß‰é£Ó¢Ä¨~`§µhÜ¾½EÇl©k EÊè!8E+üëc¿qæ›{+,5/bŽÆ\’+Ç-pK:óéñ(Â¶¹Ü15%Ýª®À¨BNH¦	1yNøó“S;¨?½ð£oß¾r4lj4–®Ú£-OLá"`Ñ¥æE{iãÌ¸†Ñ³Fó‚±~¨[hè\{ÊÃž6F…ß½X˜ßeP/¾)%E“,TU:Œ·Þ;÷x×Ô±1¥«ÓÉ]ƒY:b„Vn3Æà6rBjµ-éÿ<ô¦.1•¤h·ß+„#òìÕ“2”T±Ás¾gÛâøìÅ…Ä>”0^ÐFœÞ÷î_~Z«èQô\r>?“n;Ò¹??Ä>÷¦:²AÓêfn¿ÜðK‰väË7ÉÄ`]&g’=ÉfSÙ‚Î45›K Åa‘9Üno²”ûý/Œ‡2SŠüµOÆÜëË{=™rÆSçuî¾:Ü²¥W†õ@‰sùõOßÓ¨:¾}'€Mýpê¤—€µ å.×™M+{£ŒóÏŒùÇ¼¡/±Tº ÷ÝC‹ïû*Å5j0ëà]ü`—]u¸Kü‚n"‚f{ÒEy^=èÊ4†²U)t‡^q>»«Î£ÝºÚzKìÙÕ¶™9·ÙúÙ+a¤ñá½	ä>zš¶‚àîÜú%lt°‚¹›oÒÎÞ·â Ñú«4¡A`ˆCìÒï¨€4š•ÂFÖüÂ¸UjofüÓ´jšïaÄÃZ=øíKøƒÅëÏ¦ØÚ‰EÄùW£·Þœ¾zˆá­Ñï¾nºóŸ_ÝòÒ"Ò„0nâ5J·ÐõÍ*åË‡;XÈßý …Þ8wª<¶ÖqU%]ÁD®©õ×çÞ—Né—ƒÕô_öë;™dM˜O?Ž¼õ8g.\½ÿì‰Qf<¬…mClL«±²°Í:¨BþÙû{!âóíõ/ŸÁ«Tçär ›nw+òîŽ•Ø#rÏ¼¹^hlz?{úÒ3Wž™šOÚ(,Ž“3Œ¹.è6“†ÝüÁdœBÍ$Ï^AHA_mïÚ‘¯šÉC³óšÁtêŸ‡~›7zÃTÉµ÷|ÇˆÖ
©ñhàÀ ¶·0ãR‡L1íýý½çÚóþØ|âì/ÒÚxîÇ‹&.<­©õá»±’çp(Í™m"|óãª_îÉ±É_½¢¿ùöåÇºµ‚ÉÞkÍˆ‰Œ5Ò*ØÍÂÄí\Vòõá†Á³p(ù@‰ué¾Ì&Y+ÅÆÍ'P®“äs¬ã7~„w…ÉÂVmŸì	žþÐ¥µÆLÇ×+'öªSjÀéÈ„ÊÐ6²Ú“Æ’ë{QW¿ÔŒÓÆœÚ;à}sTåºxIydÓÿÏwŸ›‡PQÖIo¯.'S`âêõçÇ&kÚµ7–«´ûÍ]æ_8¤îW[N ÚmüíÉ‡< HõÜOÖÆŒnÖ0¥¼ßÉìóljoçÀ4}æY·¾¶JàÃ·'ˆVX%„ý[™…þXx
¾†\‘\´Æ™¼.’+Â4:]~ÅÀ[—;V¾½ßÊãÛœDIÏïšØBá<ogêdqJL±õÂ#•Š«†ž´`Ì~xäiPdvghÍGF÷£>´ÛoÃ‚ù A7_D—«þEË‚8„‹g¿9ÂÙ:H{Y1–û×]n—¼doùP‰V5/Z Ü0[òÒe¸5³Î…pˆ‚%×“žòs,‹LóûcÁk?ç2¯<f2`Ó;ó3‡âþó¹§ÏÝfêÍ–µ5¨2nøæA$¸|ñ	vÒ[lG.b*%ûõ/.X#PpO©AŽd_Y ’ˆ4Çª”O7¡À?ž=Bcgë&óîzáÔ€h4>Õ
¸þ‡À92‰m³íÚó/®lÒ•Ò~,)ÙÑB-0­Ë"lª¯=°à¸øÐžA!’3b×^\ƒn”kIÏV‘›<b³Ù½xÔ	´¯<_ŸYèB&äþîÁö~îußù7"q#ìÚ/{¹üÅg+1ø­™Hü?~ÿÖWG	E…j7>AZäP4ôÎŠƒ÷Š€7é­f„óîäí§hèù â\[»ø‚ãæ©o_.HÓ‹V-6Ã£,Lb­ÿÊ“S›ðô6FA'í@³0Y‚‡\lïêÑí-u±ÉÒreýT@ceš]%<D4Ì6„þ>N™(	ôþ¥ÃðqœDÛ\¯NOQØÃÉ‚•0kªw3Dºˆmð3Áƒmt83UX=b-7Ý¾ó2 4•j«ñéÃÊ¯µÍûùq“}ó³Ç•Æé9h²þWêìlâÿ  ÿÿ õê°