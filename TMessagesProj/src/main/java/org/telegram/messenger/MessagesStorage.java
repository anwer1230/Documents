/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import static org.telegram.messenger.MessagesController.LOAD_AROUND_DATE;
import static org.telegram.messenger.MessagesController.LOAD_AROUND_MESSAGE;
import static org.telegram.messenger.MessagesController.LOAD_BACKWARD;
import static org.telegram.messenger.MessagesController.LOAD_FORWARD;
import static org.telegram.messenger.MessagesController.LOAD_FROM_UNREAD;

import android.appwidget.AppWidgetManager;
import android.content.SharedPreferences;
import android.os.Looper;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.util.SparseArray;
import android.util.SparseIntArray;

import androidx.annotation.UiThread;
import androidx.collection.LongSparseArray;

import org.telegram.PhoneFormat.PhoneFormat;
import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteDatabase;
import org.telegram.SQLite.SQLiteException;
import org.telegram.SQLite.SQLitePreparedStatement;
import org.telegram.messenger.support.LongSparseIntArray;
import org.telegram.messenger.utils.EphemeralMessagesHelper;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.Vector;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.tgnet.tl.TL_bots;
import org.telegram.tgnet.tl.TL_communities;
import org.telegram.tgnet.tl.TL_ephemeral;
import org.telegram.tgnet.tl.TL_stories;
import org.telegram.tgnet.tl.TL_update;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Adapters.DialogsSearchAdapter;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.Forum.ForumUtilities;
import org.telegram.ui.Components.Reactions.ReactionsLayoutInBubble;
import org.telegram.ui.Components.Reactions.ReactionsUtils;
import org.telegram.ui.Components.VideoPlayer;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.EditWidgetActivity;
import org.telegram.ui.Stories.StoriesController;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import me.vkryl.core.BitwiseUtils;

public class MessagesStorage extends BaseController {

    private DispatchQueue storageQueue;
    private SQLiteDatabase database;
    private File cacheFile;
    private File walCacheFile;
    private File shmCacheFile;
    private final AtomicLong lastTaskId = new AtomicLong(System.currentTimeMillis());
    private final SparseArray<ArrayList<Runnable>> tasks = new SparseArray<>();

    private int lastDateValue = 0;
    private int lastPtsValue = 0;
    private int lastQtsValue = 0;
    private int lastSeqValue = 0;
    private int lastSecretVersion = 0;
    private byte[] secretPBytes = null;
    private int secretG = 0;

    private int lastSavedSeq = 0;
    private int lastSavedPts = 0;
    private int lastSavedDate = 0;
    private int lastSavedQts = 0;

    private final ArrayList<MessagesController.DialogFilter> dialogFilters = new ArrayList<>();
    private final SparseArray<MessagesController.DialogFilter> dialogFiltersMap = new SparseArray<>();
    private final LongSparseArray<Boolean> unknownDialogsIds = new LongSparseArray<>();
    private int mainUnreadCount;
    private int archiveUnreadCount;
    private volatile int pendingMainUnreadCount;
    private volatile int pendingArchiveUnreadCount;
    private boolean databaseCreated;

    private final CountDownLatch openSync = new CountDownLatch(1);

    private static volatile MessagesStorage[] Instance = new MessagesStorage[UserConfig.MAX_ACCOUNT_COUNT];
    private static final Object[] lockObjects = new Object[UserConfig.MAX_ACCOUNT_COUNT];
    static {
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            lockObjects[i] = new Object();
        }
    }

    public final static int LAST_DB_VERSION = 177;
    private boolean databaseMigrationInProgress;
    public boolean showClearDatabaseAlert;

    public static final int FORUM_TYPE_CHAT = 1;
    public static final int FORUM_TYPE_CHAT_TABS = 1 << 1;
    public static final int FORUM_TYPE_DIRECT = 1 << 2;
    public static final int FORUM_TYPE_BOT = 1 << 3;

    private final LongSparseIntArray dialogIsForumTyped = new LongSparseIntArray();


    public static MessagesStorage getInstance(int num) {
        MessagesStorage localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (lockObjects[num]) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new MessagesStorage(num);
                }
            }
        }
        return localInstance;
    }

    private void ensureOpened() {
        try {
            openSync.await();
        } catch (Throwable ignore) {

        }
    }

    public int getLastDateValue() {
        ensureOpened();
        return lastDateValue;
    }

    public void setLastDateValue(int value) {
        ensureOpened();
        lastDateValue = value;
    }

    public int getLastPtsValue() {
        ensureOpened();
        return lastPtsValue;
    }

    public int getMainUnreadCount() {
        return mainUnreadCount;
    }

    public int getArchiveUnreadCount() {
        return archiveUnreadCount;
    }

    public void setLastPtsValue(int value) {
        ensureOpened();
        lastPtsValue = value;
    }

    public int getLastQtsValue() {
        ensureOpened();
        return lastQtsValue;
    }

    public void setLastQtsValue(int value) {
        ensureOpened();
        lastQtsValue = value;
    }

    public int getLastSeqValue() {
        ensureOpened();
        return lastSeqValue;
    }

    public void setLastSeqValue(int value) {
        ensureOpened();
        lastSeqValue = value;
    }

    public int getLastSecretVersion() {
        ensureOpened();
        return lastSecretVersion;
    }

    public void setLastSecretVersion(int value) {
        ensureOpened();
        lastSecretVersion = value;
    }

    public byte[] getSecretPBytes() {
        ensureOpened();
        return secretPBytes;
    }

    public void setSecretPBytes(byte[] value) {
        ensureOpened();
        secretPBytes = value;
    }

    public int getSecretG() {
        ensureOpened();
        return secretG;
    }

    public void setSecretG(int value) {
        ensureOpened();
        secretG = value;
    }

    public MessagesStorage(int instance) {
        super(instance);
        storageQueue = new DispatchQueue("storageQueue_" + instance);
        storageQueue.setPriority(8);
        storageQueue.postRunnable(() -> openDatabase(1));
    }

    public SQLiteDatabase getDatabase() {
        return database;
    }

    public DispatchQueue getStorageQueue() {
        return storageQueue;
    }

    @UiThread
    public void bindTaskToGuid(Runnable task, int guid) {
        ArrayList<Runnable> arrayList = tasks.get(guid);
        if (arrayList == null) {
            arrayList = new ArrayList<>();
            tasks.put(guid, arrayList);
        }
        arrayList.add(task);
    }

    @UiThread
    public void cancelTasksForGuid(int guid) {
        ArrayList<Runnable> arrayList = tasks.get(guid);
        if (arrayList == null) {
            return;
        }
        for (int a = 0, N = arrayList.size(); a < N; a++) {
            storageQueue.cancelRunnable(arrayList.get(a));
        }
        tasks.remove(guid);
    }

    @UiThread
    public void completeTaskForGuid(Runnable runnable, int guid) {
        ArrayList<Runnable> arrayList = tasks.get(guid);
        if (arrayList == null) {
            return;
        }
        arrayList.remove(runnable);
        if (arrayList.isEmpty()) {
            tasks.remove(guid);
        }
    }

    public long getDatabaseSize() {
        long size = 0;
        if (cacheFile != null) {
            size += cacheFile.length();
        }
        if (shmCacheFile != null) {
            size += shmCacheFile.length();
        }
        /*if (walCacheFile != null) {
            size += walCacheFile.length();
        }*/
        return size;
    }

    public void openDatabase(int openTries) {
        if (!NativeLoader.loaded()) {
            int tryCount = 0;
            while (!NativeLoader.loaded()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                tryCount++;
                if (tryCount > 5) {
                    break;
                }
            }
        }
        File filesDir = ApplicationLoader.getFilesDirFixed();
        if (currentAccount != 0) {
            filesDir = new File(filesDir, "account" + currentAccount + "/");
            filesDir.mkdirs();
        }
        cacheFile = new File(filesDir, "cache4.db");
        walCacheFile = new File(filesDir, "cache4.db-wal");
        shmCacheFile = new File(filesDir, "cache4.db-shm");

        boolean createTable = false;

        databaseCreated = false;
        if (!cacheFile.exists()) {
            createTable = true;
        }
        try {
            database = new SQLiteDatabase(cacheFile.getPath());
            database.executeFast("PRAGMA secure_delete = ON").stepThis().dispose();
            database.executeFast("PRAGMA temp_store = MEMORY").stepThis().dispose();
            database.executeFast("PRAGMA journal_mode = WAL").stepThis().dispose();
            database.executeFast("PRAGMA journal_size_limit = 10485760").stepThis().dispose();

            if (createTable) {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.d("create new database");
                }
                createTables(database);
            } else {
                int version = database.executeInt("PRAGMA user_version");
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.d("current db version = " + version);
                }
                if (version == 0) {
                    throw new Exception("malformed");
                }
                try {
                    SQLiteCursor cursor = database.queryFinalized("SELECT seq, pts, date, qts, lsv, sg, pbytes FROM params WHERE id = 1");
                    if (cursor.next()) {
                        lastSeqValue = cursor.intValue(0);
                        lastPtsValue = cursor.intValue(1);
                        lastDateValue = cursor.intValue(2);
                        lastQtsValue = cursor.intValue(3);
                        lastSecretVersion = cursor.intValue(4);
                        secretG = cursor.intValue(5);
                        if (cursor.isNull(6)) {
                            secretPBytes = null;
                        } else {
                            secretPBytes = cursor.byteArrayValue(6);
                            if (secretPBytes != null && secretPBytes.length == 1) {
                                secretPBytes = null;
                            }
                        }
                    }
                    cursor.dispose();
                } catch (Exception e) {
                    FileLog.e(e);
                    if (e.getMessage() != null && e.getMessage().contains("malformed")) {
                        throw new RuntimeException("malformed");
                    }
                    try {
                        database.executeFast("CREATE TABLE IF NOT EXISTS params(id INTEGER PRIMARY KEY, seq INTEGER, pts INTEGER, date INTEGER, qts INTEGER, lsv INTEGER, sg INTEGER, pbytes BLOB)").stepThis().dispose();
                        database.executeFast("INSERT INTO params VALUES(1, 0, 0, 0, 0, 0, 0, NULL)").stepThis().dispose();
                    } catch (Exception e2) {
                        FileLog.e(e2);
                    }
                }
                if (version < LAST_DB_VERSION) {
                    try {
                        updateDbToLastVersion(version);
                    } catch (Exception e) {
                        if (BuildVars.DEBUG_PRIVATE_VERSION) {
                            throw e;
                        }
                        FileLog.e(e);
                        throw new RuntimeException("malformed");
                    }
                }
            }
            databaseCreated = true;
        } catch (Exception e) {
            FileLog.e(e);
            if (openTries < 3 && e.getMessage() != null && e.getMessage().contains("malformed")) {
                if (openTries == 2) {
                    cleanupInternal(true);
                    clearLoadingDialogsOffsets();
                } else {
                    cleanupInternal(false);
                }
                openDatabase(openTries == 1 ? 2 : 3);
                return;
            }
        }

        AndroidUtilities.runOnUIThread(() -> {
            if (databaseMigrationInProgress) {
                databaseMigrationInProgress = false;
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.onDatabaseMigration, false);
            }
        });
        loadDialogFilters();
        loadUnreadMessages();
        loadPendingTasks();
        try {
            openSync.countDown();
        } catch (Throwable ignore) {

        }

        AndroidUtilities.runOnUIThread(() -> {
            //TODO add progress view and uncomment
            showClearDatabaseAlert = false;//getDatabaseSize() > 150 * 1024 * 1024;
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.onDatabaseOpened);
        });
    }

    private void clearLoadingDialogsOffsets() {
        for (int a = 0; a < 2; a++) {
            getUserConfig().setDialogsLoadOffset(a, 0, 0, 0, 0, 0, 0);
            getUserConfig().setTotalDialogsCount(a, 0);
        }
        getUserConfig().saveConfig(false);
    }

    private boolean recoverDatabase() {
        database.close();
        boolean restored = DatabaseMigrationHelper.recoverDatabase(cacheFile, walCacheFile, shmCacheFile, currentAccount);
        FileLog.e("Database restored = " + restored);
        if (restored) {
            try {
                database = new SQLiteDatabase(cacheFile.getPath());
                database.executeFast("PRAGMA secure_delete = ON").stepThis().dispose();
                database.executeFast("PRAGMA temp_store = MEMORY").stepThis().dispose();
                database.executeFast("PRAGMA journal_mode = WAL").stepThis().dispose();
                database.executeFast("PRAGMA journal_size_limit = 10485760").stepThis().dispose();
            } catch (SQLiteException e) {
                FileLog.e(new Exception(e));
                restored = false;
            }
        }
        if (!restored) {
            cleanupInternal(true);
            openDatabase(1);
            restored = databaseCreated;
            FileLog.e("Try create new database = " + restored);
        }
        if (restored) {
            reset();
        }
        return restored;
    }

    public final static String[] DATABASE_TABLES = new String[] {
            "messages_holes",
            "media_holes_v2",
            "scheduled_messages_v2",
            "quick_replies",
            "messages_v2",
            "download_queue",
            "user_contacts_v7",
            "user_phones_v7",
            "dialogs",
            "dialog_filter",
            "dialog_filter_ep",
            "dialog_filter_pin_v2",
            "randoms_v2",
            "enc_tasks_v4",
            "messages_seq",
            "params",
            "media_v4",
            "bot_keyboard",
            "bot_keyboard_topics",
            "chat_settings_v2",
            "user_settings",
            "chat_pinned_v2",
            "chat_pinned_count",
            "chat_hints",
            "botcache",
            "users_data",
            "users",
            "chats",
            "enc_chats",
            "channel_users_v2",
            "channel_admins_v3",
            "contacts",
            "dialog_photos",
            "dialog_settings",
            "web_recent_v3",
            "stickers_v2",
            "stickers_featured",
            "stickers_dice",
            "stickersets",
            "hashtag_recent_v2",
            "webpage_pending_v2",
            "sent_files_v2",
            "search_recent",
            "media_counts_v2",
            "keyvalue",
            "bot_info_v2",
            "pending_tasks",
            "requested_holes",
            "sharing_locations",
            "shortcut_widget",
            "emoji_keywords_v2",
            "emoji_keywords_info_v2",
            "wallpapers2",
            "unread_push_messages",
            "polls_v2",
            "reactions",
            "reaction_mentions",
            "downloading_documents",
            "animated_emoji",
            "attach_menu_bots",
            "premium_promo",
            "emoji_statuses",
            "messages_holes_topics",
            "messages_topics",
            "saved_dialogs",
            "media_topics",
            "media_holes_topics",
            "topics",
            "media_counts_topics",
            "reaction_mentions_topics",
            "emoji_groups",
            "poll_votes_mentions",
            "poll_votes_mentions_topics",
            "ephemeral_messages"
    };

    public static void createTables(SQLiteDatabase database) throws SQLiteException {
        database.executeFast("CREATE TABLE messages_holes(uid INTEGER, start INTEGER, end INTEGER, PRIMARY KEY(uid, start));").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS uid_end_messages_holes_4_dialogs ON messages_holes(uid, end);").stepThis().dispose();

        database.executeFast("CREATE TABLE media_holes_v2(uid INTEGER, type INTEGER, start INTEGER, end INTEGER, PRIMARY KEY(uid, type, start));").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS uid_end_media_holes_v2 ON media_holes_v2(uid, type, end);").stepThis().dispose();

        database.executeFast("CREATE TABLE scheduled_messages_v2(mid INTEGER, uid INTEGER, send_state INTEGER, date INTEGER, data BLOB, ttl INTEGER, replydata BLOB, reply_to_message_id INTEGER, PRIMARY KEY(mid, uid))").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS send_state_idx_scheduled_messages_v2 ON scheduled_messages_v2(mid, send_state, date);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS uid_date_idx_scheduled_messages_v2 ON scheduled_messages_v2(uid, date);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS reply_to_idx_scheduled_messages_v2 ON scheduled_messages_v2(mid, reply_to_message_id);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS idx_to_reply_scheduled_messages_v2 ON scheduled_messages_v2(reply_to_message_id, mid);").stepThis().dispose();

        database.executeFast("CREATE TABLE messages_v2(mid INTEGER, uid INTEGER, read_state INTEGER, send_state INTEGER, date INTEGER, data BLOB, out INTEGER, ttl INTEGER, media INTEGER, replydata BLOB, imp INTEGER, mention INTEGER, forwards INTEGER, replies_data BLOB, thread_reply_id INTEGER, is_channel INTEGER, reply_to_message_id INTEGER, custom_params BLOB, group_id INTEGER, reply_to_story_id INTEGER, PRIMARY KEY(mid, uid))").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS uid_mid_read_out_idx_messages_v2 ON messages_v2(uid, mid, read_state, out);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS uid_date_mid_idx_messages_v2 ON messages_v2(uid, date, mid);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS mid_out_idx_messages_v2 ON messages_v2(mid, out);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS task_idx_messages_v2 ON messages_v2(uid, out, read_state, ttl, date, send_state);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS send_state_idx_messages_v2 ON messages_v2(mid, send_state, date);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS uid_mention_idx_messages_v2 ON messages_v2(uid, mention, read_state);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS is_channel_idx_messages_v2 ON messages_v2(mid, is_channel);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS reply_to_idx_messages_v2 ON messages_v2(mid, reply_to_message_id);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS idx_to_reply_messages_v2 ON messages_v2(reply_to_message_id, mid);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS uid_mid_groupid_messages_v2 ON messages_v2(uid, mid, group_id);").stepThis().dispose();

        database.executeFast("CREATE TABLE saved_dialogs(did INTEGER, date INTEGER, last_mid INTEGER, pinned INTEGER, flags INTEGER, folder_id INTEGER, last_mid_group INTEGER, count INTEGER, forumChatId INTEGER, unread_count INTEGER, max_read_id INTEGER, read_outbox INTEGER, PRIMARY KEY (did, forumChatId))").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS date_idx_4_saved_dialogs ON saved_dialogs(date);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS last_mid_idx_4_saved_dialogs ON saved_dialogs(last_mid);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS folder_id_idx_4_saved_dialogs ON saved_dialogs(folder_id);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS flags_idx_4_saved_dialogs ON saved_dialogs(flags);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS forum_idx_dialogs ON saved_dialogs(forumChatId);").stepThis().dispose();

        database.executeFast("CREATE TABLE download_queue(uid INTEGER, type INTEGER, date INTEGER, data BLOB, parent TEXT, PRIMARY KEY (uid, type));").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS type_date_idx_download_queue ON download_queue(type, date);").stepThis().dispose();

        database.executeFast("CREATE TABLE user_contacts_v7(key TEXT PRIMARY KEY, uid INTEGER, fname TEXT, sname TEXT, imported INTEGER)").stepThis().dispose();
        database.executeFast("CREATE TABLE user_phones_v7(key TEXT, phone TEXT, sphone TEXT, deleted INTEGER, PRIMARY KEY (key, phone))").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS sphone_deleted_idx_user_phones ON user_phones_v7(sphone, deleted);").stepThis().dispose();

        database.executeFast("CREATE TABLE dialogs(did INTEGER PRIMARY KEY, date INTEGER, unread_count INTEGER, last_mid INTEGER, inbox_max INTEGER, outbox_max INTEGER, last_mid_i INTEGER, unread_count_i INTEGER, pts INTEGER, date_i INTEGER, pinned INTEGER, flags INTEGER, folder_id INTEGER, data BLOB, unread_reactions INTEGER, last_mid_group INTEGER, ttl_period INTEGER, unread_poll_votes INTEGER)").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS date_idx_4_dialogs ON dialogs(date);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS last_mid_idx_4_dialogs ON dialogs(last_mid);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS unread_count_idx_dialogs ON dialogs(unread_count);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS last_mid_i_idx_dialogs ON dialogs(last_mid_i);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS unread_count_i_idx_dialogs ON dialogs(unread_count_i);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS folder_id_idx_4_dialogs ON dialogs(folder_id);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS flags_idx_4_dialogs ON dialogs(flags);").stepThis().dispose();

        database.executeFast("CREATE TABLE dialog_filter(id INTEGER PRIMARY KEY, ord INTEGER, unread_count INTEGER, flags INTEGER, title TEXT, color INTEGER DEFAULT -1, entities BLOB, noanimate INTEGER)").stepThis().dispose();
        database.executeFast("CREATE TABLE dialog_filter_ep(id INTEGER, peer INTEGER, PRIMARY KEY (id, peer))").stepThis().dispose();
        database.executeFast("CREATE TABLE dialog_filter_pin_v2(id INTEGER, peer INTEGER, pin INTEGER, PRIMARY KEY (id, peer))").stepThis().dispose();

        database.executeFast("CREATE TABLE randoms_v2(random_id INTEGER, mid INTEGER, uid INTEGER, PRIMARY KEY (random_id, mid, uid))").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS mid_idx_randoms_v2 ON randoms_v2(mid, uid);").stepThis().dispose();

        database.executeFast("CREATE TABLE enc_tasks_v4(mid INTEGER, uid INTEGER, date INTEGER, media INTEGER, PRIMARY KEY(mid, uid, media))").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS date_idx_enc_tasks_v4 ON enc_tasks_v4(date);").stepThis().dispose();

        database.executeFast("CREATE TABLE messages_seq(mid INTEGER PRIMARY KEY, seq_in INTEGER, seq_out INTEGER);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS seq_idx_messages_seq ON messages_seq(seq_in, seq_out);").stepThis().dispose();

        database.executeFast("CREATE TABLE params(id INTEGER PRIMARY KEY, seq INTEGER, pts INTEGER, date INTEGER, qts INTEGER, lsv INTEGER, sg INTEGER, pbytes BLOB)").stepThis().dispose();
        database.executeFast("INSERT INTO params VALUES(1, 0, 0, 0, 0, 0, 0, NULL)").stepThis().dispose();

        database.executeFast("CREATE TABLE media_v4(mid INTEGER, uid INTEGER, date INTEGER, type INTEGER, data BLOB, PRIMARY KEY(mid, uid, type))").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS uid_mid_type_date_idx_media_v4 ON media_v4(uid, mid, type, date);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS uid_type_date_mid_idx_media_v4 ON media_v4(uid, type, date DESC, mid DESC);").stepThis().dispose();

        database.executeFast("CREATE TABLE bot_keyboard(uid INTEGER PRIMARY KEY, mid INTEGER, info BLOB)").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS bot_keyboard_idx_mid_v2 ON bot_keyboard(mid, uid);").stepThis().dispose();

        database.executeFast("CREATE TABLE bot_keyboard_topics(uid INTEGER, tid INTEGER, mid INTEGER, info BLOB, PRIMARY KEY(uid, tid))").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS bot_keyboard_topics_idx_mid_v2 ON bot_keyboard_topics(mid, uid, tid);").stepThis().dispose();

        database.executeFast("CREATE TABLE chat_settings_v2(uid INTEGER PRIMARY KEY, info BLOB, pinned INTEGER, online INTEGER, inviter INTEGER, links INTEGER, participants_count INTEGER)").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS chat_settings_pinned_idx ON chat_settings_v2(uid, pinned) WHERE pinned != 0;").stepThis().dispose();

        database.executeFast("CREATE TABLE user_settings(uid INTEGER PRIMARY KEY, info BLOB, pinned INTEGER)").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS user_settings_pinned_idx ON user_settings(uid, pinned) WHERE pinned != 0;").stepThis().dispose();

        database.executeFast("CREATE TABLE chat_pinned_v2(uid INTEGER, mid INTEGER, data BLOB, PRIMARY KEY (uid, mid));").stepThis().dispose();
        database.executeFast("CREATE TABLE chat_pinned_count(uid INTEGER PRIMARY KEY, count INTEGER, end INTEGER);").stepThis().dispose();

        database.executeFast("CREATE TABLE chat_hints(did INTEGER, type INTEGER, rating REAL, date INTEGER, PRIMARY KEY(did, type))").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS chat_hints_rating_idx ON chat_hints(rating);").stepThis().dispose();

        database.executeFast("CREATE TABLE botcache(id TEXT PRIMARY KEY, date INTEGER, data BLOB)").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS botcache_date_idx ON botcache(date);").stepThis().dispose();

        database.executeFast("CREATE TABLE users_data(uid INTEGER PRIMARY KEY, about TEXT)").stepThis().dispose();
        database.executeFast("CREATE TABLE users(uid INTEGER PRIMARY KEY, name TEXT, status INTEGER, data BLOB)").stepThis().dispose();
        database.executeFast("CREATE TABLE chats(uid INTEGER PRIMARY KEY, name TEXT, data BLOB)").stepThis().dispose();
        database.executeFast("CREATE TABLE enc_chats(uid INTEGER PRIMARY KEY, user INTEGER, name TEXT, data BLOB, g BLOB, authkey BLOB, ttl INTEGER, layer INTEGER, seq_in INTEGER, seq_out INTEGER, use_count INTEGER, exchange_id INTEGER, key_date INTEGER, fprint INTEGER, fauthkey BLOB, khash BLOB, in_seq_no INTEGER, admin_id INTEGER, mtproto_seq INTEGER)").stepThis().dispose();
        database.executeFast("CREATE TABLE channel_users_v2(did INTEGER, uid INTEGER, date INTEGER, data BLOB, PRIMARY KEY(did, uid))").stepThis().dispose();
        database.executeFast("CREATE TABLE channel_admins_v3(did INTEGER, uid INTEGER, data BLOB, PRIMARY KEY(did, uid))").stepThis().dispose();
        database.executeFast("CREATE TABLE contacts(uid INTEGER PRIMARY KEY, mutual INTEGER)").stepThis().dispose();
        database.executeFast("CREATE TABLE dialog_photos(uid INTEGER, id INTEGER, num INTEGER, data BLOB, PRIMARY KEY (uid, id))").stepThis().dispose();
        database.executeFast("CREATE TABLE dialog_photos_count(uid INTEGER PRIMARY KEY, count INTEGER)").stepThis().dispose();
        database.executeFast("CREATE TABLE dialog_settings(did INTEGER PRIMARY KEY, flags INTEGER);").stepThis().dispose();
        database.executeFast("CREATE TABLE web_recent_v3(id TEXT, type INTEGER, image_url TEXT, thumb_url TEXT, local_url TEXT, width INTEGER, height INTEGER, size INTEGER, date INTEGER, document BLOB, PRIMARY KEY (id, type));").stepThis().dispose();
        database.executeFast("CREATE TABLE stickers_v2(id INTEGER PRIMARY KEY, data BLOB, date INTEGER, hash INTEGER);").stepThis().dispose();
        database.executeFast("CREATE TABLE stickers_featured(id INTEGER PRIMARY KEY, data BLOB, unread BLOB, date INTEGER, hash INTEGER, premium INTEGER, emoji INTEGER);").stepThis().dispose();
        database.executeFast("CREATE TABLE stickers_dice(emoji TEXT PRIMARY KEY, data BLOB, date INTEGER);").stepThis().dispose();
        database.executeFast("CREATE TABLE hashtag_recent_v2(id TEXT PRIMARY KEY, date INTEGER);").stepThis().dispose();
        database.executeFast("CREATE TABLE webpage_pending_v2(id INTEGER, mid INTEGER, uid INTEGER, PRIMARY KEY (id, mid, uid));").stepThis().dispose();
        database.executeFast("CREATE TABLE sent_files_v2(uid TEXT, type INTEGER, data BLOB, parent TEXT, PRIMARY KEY (uid, type))").stepThis().dispose();
        database.executeFast("CREATE TABLE search_recent(did INTEGER PRIMARY KEY, date INTEGER);").stepThis().dispose();
        database.executeFast("CREATE TABLE media_counts_v2(uid INTEGER, type INTEGER, count INTEGER, old INTEGER, PRIMARY KEY(uid, type))").stepThis().dispose();
        database.executeFast("CREATE TABLE keyvalue(id TEXT PRIMARY KEY, value TEXT)").stepThis().dispose();
        database.executeFast("CREATE TABLE bot_info_v2(uid INTEGER, dialogId INTEGER, info BLOB, PRIMARY KEY(uid, dialogId))").stepThis().dispose();
        database.executeFast("CREATE TABLE pending_tasks(id INTEGER PRIMARY KEY, data BLOB);").stepThis().dispose();
        database.executeFast("CREATE TABLE requested_holes(uid INTEGER, seq_out_start INTEGER, seq_out_end INTEGER, PRIMARY KEY (uid, seq_out_start, seq_out_end));").stepThis().dispose();
        database.executeFast("CREATE TABLE sharing_locations(uid INTEGER PRIMARY KEY, mid INTEGER, date INTEGER, period INTEGER, message BLOB, proximity INTEGER);").stepThis().dispose();

        database.executeFast("CREATE TABLE stickersets2(id INTEGER PRIMATE KEY, data BLOB, hash INTEGER, date INTEGER, short_name TEXT);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS stickersets2_id_index ON stickersets2(id);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS stickersets2_id_short_name ON stickersets2(id, short_name);").stepThis().dispose();

        database.executeFast("CREATE INDEX IF NOT EXISTS stickers_featured_emoji_index ON stickers_featured(emoji);").stepThis().dispose();

        database.executeFast("CREATE TABLE shortcut_widget(id INTEGER, did INTEGER, ord INTEGER, PRIMARY KEY (id, did));").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS shortcut_widget_did ON shortcut_widget(did);").stepThis().dispose();

        database.executeFast("CREATE TABLE emoji_keywords_v2(lang TEXT, keyword TEXT, emoji TEXT, PRIMARY KEY(lang, keyword, emoji));").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS emoji_keywords_v2_keyword ON emoji_keywords_v2(keyword);").stepThis().dispose();
        database.executeFast("CREATE TABLE emoji_keywords_info_v2(lang TEXT PRIMARY KEY, alias TEXT, version INTEGER, date INTEGER);").stepThis().dispose();

        database.executeFast("CREATE TABLE wallpapers2(uid INTEGER PRIMARY KEY, data BLOB, num INTEGER)").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS wallpapers_num ON wallpapers2(num);").stepThis().dispose();

        database.executeFast("CREATE TABLE unread_push_messages(uid INTEGER, mid INTEGER, random INTEGER, date INTEGER, data BLOB, fm TEXT, name TEXT, uname TEXT, flags INTEGER, topicId INTEGER, is_reaction INTEGER, PRIMARY KEY(uid, mid))").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS unread_push_messages_idx_date ON unread_push_messages(date);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS unread_push_messages_idx_random ON unread_push_messages(random);").stepThis().dispose();

        database.executeFast("CREATE TABLE polls_v2(mid INTEGER, uid INTEGER, id INTEGER, PRIMARY KEY (mid, uid));").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS polls_id_v2 ON polls_v2(id);").stepThis().dispose();

        database.executeFast("CREATE TABLE reactions(data BLOB, hash INTEGER, date INTEGER);").stepThis().dispose();
        database.executeFast("CREATE TABLE reaction_mentions(message_id INTEGER, state INTEGER, dialog_id INTEGER, PRIMARY KEY(message_id, dialog_id))").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS reaction_mentions_did ON reaction_mentions(dialog_id);").stepThis().dispose();

        database.executeFast("CREATE TABLE downloading_documents(data BLOB, hash INTEGER, id INTEGER, state INTEGER, date INTEGER, PRIMARY KEY(hash, id));").stepThis().dispose();
        database.executeFast("CREATE TABLE animated_emoji(document_id INTEGER PRIMARY KEY, data BLOB);").stepThis().dispose();

        database.executeFast("CREATE TABLE attach_menu_bots(data BLOB, hash INTEGER, date INTEGER);").stepThis().dispose();

        database.executeFast("CREATE TABLE premium_promo(data BLOB, date INTEGER);").stepThis().dispose();
        database.executeFast("CREATE TABLE emoji_statuses(data BLOB, type INTEGER);").stepThis().dispose();

        database.executeFast("CREATE TABLE messages_holes_topics(uid INTEGER, topic_id INTEGER, start INTEGER, end INTEGER, PRIMARY KEY(uid, topic_id, start));").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS uid_end_messages_holes_4_topics ON messages_holes_topics(uid, topic_id, end);").stepThis().dispose();

        database.executeFast("CREATE TABLE messages_topics(mid INTEGER, uid INTEGER, topic_id INTEGER, read_state INTEGER, send_state INTEGER, date INTEGER, data BLOB, out INTEGER, ttl INTEGER, media INTEGER, replydata BLOB, imp INTEGER, mention INTEGER, forwards INTEGER, replies_data BLOB, thread_reply_id INTEGER, is_channel INTEGER, reply_to_message_id INTEGER, custom_params BLOB, reply_to_story_id INTEGER, PRIMARY KEY(mid, topic_id, uid))").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS uid_date_mid_idx_messages_topics ON messages_topics(uid, date, mid);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS mid_out_idx_messages_topics ON messages_topics(mid, out);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS task_idx_messages_topics ON messages_topics(uid, out, read_state, ttl, date, send_state);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS send_state_idx_messages_topics ON messages_topics(mid, send_state, date);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS is_channel_idx_messages_topics ON messages_topics(mid, is_channel);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS reply_to_idx_messages_topics ON messages_topics(mid, reply_to_message_id);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS idx_to_reply_messages_topics ON messages_topics(reply_to_message_id, mid);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS mid_uid_messages_topics ON messages_topics(mid, uid);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS uid_mid_read_out_idx_messages_topics ON messages_topics(uid, topic_id, mid, read_state, out);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS uid_mention_idx_messages_topics ON messages_topics(uid, topic_id, mention, read_state);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS uid_topic_id_messages_topics ON messages_topics(uid, topic_id);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS uid_topic_id_date_mid_messages_topics ON messages_topics(uid, topic_id, date, mid);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS uid_topic_id_mid_messages_topics ON messages_topics(uid, topic_id, mid);").stepThis().dispose();

        database.executeFast("CREATE TABLE media_topics(mid INTEGER, uid INTEGER, topic_id INTEGER, date INTEGER, type INTEGER, data BLOB, PRIMARY KEY(mid, uid, topic_id, type))").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS uid_mid_type_date_idx_media_topics ON media_topics(uid, topic_id, mid, type, date);").stepThis().dispose();

        database.executeFast("CREATE TABLE media_holes_topics(uid INTEGER, topic_id INTEGER, type INTEGER, start INTEGER, end INTEGER, PRIMARY KEY(uid, topic_id, type, start));").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS uid_end_media_holes_topics ON media_holes_topics(uid, topic_id, type, end);").stepThis().dispose();

        database.executeFast("CREATE TABLE topics(did INTEGER, topic_id INTEGER, data BLOB, top_message INTEGER, topic_message BLOB, unread_count INTEGER, max_read_id INTEGER, unread_mentions INTEGER, unread_reactions INTEGER, read_outbox INTEGER, pinned INTEGER, total_messages_count INTEGER, hidden INTEGER, edit_date INTEGER, nopaid_messages_exception INTEGER, unread_poll_votes INTEGER, PRIMARY KEY(did, topic_id));").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS did_top_message_topics ON topics(did, top_message);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS did_topics ON topics(did);").stepThis().dispose();

        database.executeFast("CREATE TABLE media_counts_topics(uid INTEGER, topic_id INTEGER, type INTEGER, count INTEGER, old INTEGER, PRIMARY KEY(uid, topic_id, type))").stepThis().dispose();

        database.executeFast("CREATE TABLE reaction_mentions_topics(message_id INTEGER, state INTEGER, dialog_id INTEGER, topic_id INTEGER, PRIMARY KEY(message_id, dialog_id, topic_id))").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS reaction_mentions_topics_did ON reaction_mentions_topics(dialog_id, topic_id);").stepThis().dispose();

        database.executeFast("CREATE TABLE emoji_groups(type INTEGER PRIMARY KEY, data BLOB)").stepThis().dispose();
        database.executeFast("CREATE TABLE app_config(data BLOB)").stepThis().dispose();
        database.executeFast("CREATE TABLE web_browser_settings(data BLOB)").stepThis().dispose();
        database.executeFast("CREATE TABLE effects(data BLOB)").stepThis().dispose();

        database.executeFast("CREATE TABLE stories (dialog_id INTEGER, story_id INTEGER, data BLOB, custom_params BLOB, PRIMARY KEY (dialog_id, story_id));").stepThis().dispose();
        database.executeFast("CREATE TABLE stories_counter (dialog_id INTEGER PRIMARY KEY, count INTEGER, max_read INTEGER);").stepThis().dispose();

        database.executeFast("CREATE TABLE profile_stories (dialog_id INTEGER, story_id INTEGER, data BLOB, type INTEGER, seen INTEGER, pin INTEGER, PRIMARY KEY(dialog_id, story_id, type));").stepThis().dispose();
        database.executeFast("CREATE TABLE profile_stories_albums (dialog_id INTEGER, album_id INTEGER, order_index INTEGER, data BLOB, PRIMARY KEY(dialog_id, album_id));").stepThis().dispose();
        database.executeFast("CREATE TABLE profile_stories_albums_links (dialog_id INTEGER, album_id INTEGER, story_id INTEGER, order_index INTEGER, PRIMARY KEY (dialog_id, album_id, story_id));").stepThis().dispose();

        database.executeFast("CREATE TABLE story_drafts (id INTEGER PRIMARY KEY, date INTEGER, data BLOB, type INTEGER);").stepThis().dispose();

        database.executeFast("CREATE TABLE story_pushes (uid INTEGER, sid INTEGER, date INTEGER, localName TEXT, flags INTEGER, expire_date INTEGER, live INTEGER, PRIMARY KEY(uid, sid));").stepThis().dispose();

        database.executeFast("CREATE TABLE unconfirmed_auth (data BLOB);").stepThis().dispose();

        database.executeFast("CREATE TABLE saved_reaction_tags (topic_id INTEGER PRIMARY KEY, data BLOB);").stepThis().dispose();

        database.executeFast("CREATE TABLE tag_message_id(mid INTEGER, topic_id INTEGER, tag INTEGER, text TEXT);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS tag_idx_tag_message_id ON tag_message_id(tag);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS tag_text_idx_tag_message_id ON tag_message_id(tag, text COLLATE NOCASE);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS tag_topic_idx_tag_message_id ON tag_message_id(topic_id, tag);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS tag_topic_text_idx_tag_message_id ON tag_message_id(topic_id, tag, text COLLATE NOCASE);").stepThis().dispose();

        database.executeFast("CREATE TABLE business_replies(topic_id INTEGER PRIMARY KEY, name TEXT, order_value INTEGER, count INTEGER);").stepThis().dispose();
        database.executeFast("CREATE TABLE quick_replies_messages(mid INTEGER, topic_id INTEGER, send_state INTEGER, date INTEGER, data BLOB, ttl INTEGER, replydata BLOB, reply_to_message_id INTEGER, PRIMARY KEY(mid, topic_id))").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS send_state_idx_quick_replies_messages ON quick_replies_messages(mid, send_state, date);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS topic_date_idx_quick_replies_messages ON quick_replies_messages(topic_id, date);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS reply_to_idx_quick_replies_messages ON quick_replies_messages(mid, reply_to_message_id);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS idx_to_reply_quick_replies_messages ON quick_replies_messages(reply_to_message_id, mid);").stepThis().dispose();

        database.executeFast("CREATE TABLE welcome_messages(mid INTEGER, dialog_id INTEGER, send_state INTEGER, date INTEGER, data BLOB, ttl INTEGER, replydata BLOB, reply_to_message_id INTEGER, PRIMARY KEY(mid, dialog_id))").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS send_state_idx_welcome_messages ON welcome_messages(mid, send_state, date);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS dialog_date_idx_welcome_messages ON welcome_messages(dialog_id, date);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS reply_to_idx_welcome_messages ON welcome_messages(mid, reply_to_message_id);").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS idx_to_reply_welcome_messages ON welcome_messages(reply_to_message_id, mid);").stepThis().dispose();

        database.executeFast("CREATE TABLE business_links(data BLOB, order_value INTEGER);").stepThis().dispose();
        database.executeFast("CREATE TABLE fact_checks(hash INTEGER PRIMARY KEY, data BLOB, expires INTEGER);").stepThis().dispose();
        database.executeFast("CREATE TABLE popular_bots(uid INTEGER PRIMARY KEY, time INTEGER, offset TEXT, pos INTEGER);").stepThis().dispose();

        database.executeFast("CREATE TABLE star_gifts2(id INTEGER PRIMARY KEY, data BLOB, hash INTEGER, time INTEGER, pos INTEGER);").stepThis().dispose();

        database.executeFast("CREATE TABLE gift_themes (slug TEXT PRIMARY KEY, data BLOB);").stepThis().dispose();

        database.executeFast("CREATE TABLE poll_votes_mentions(message_id INTEGER, state INTEGER, dialog_id INTEGER, PRIMARY KEY(message_id, dialog_id))").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS poll_votes_mentions_did ON poll_votes_mentions(dialog_id);").stepThis().dispose();
        database.executeFast("CREATE TABLE poll_votes_mentions_topics(message_id INTEGER, state INTEGER, dialog_id INTEGER, topic_id INTEGER, PRIMARY KEY(message_id, dialog_id, topic_id))").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS poll_votes_mentions_topics_did ON poll_votes_mentions_topics(dialog_id, topic_id);").stepThis().dispose();

        database.executeFast("CREATE TABLE ephemeral_messages (id INTEGER, dialog_id INTEGER, topic_id INTEGER, date INTEGER, data BLOB, PRIMARY KEY(dialog_id, id));").stepThis().dispose();
        database.executeFast("CREATE INDEX IF NOT EXISTS ephemeral_messages_date_idx ON ephemeral_messages(date);").stepThis().dispose();

        database.executeFast("PRAGMA user_version = " + MessagesStorage.LAST_DB_VERSION).stepThis().dispose();

    }

    public boolean isDatabaseMigrationInProgress() {
        return databaseMigrationInProgress;
    }

    private void updateDbToLastVersion(int currentVersion) throws Exception {
        AndroidUtilities.runOnUIThread(() -> {
            databaseMigrationInProgress = true;
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.onDatabaseMigration, true);
        });

        int version = currentVersion;
        FileLog.d("MessagesStorage start db migration from " + version + " to " + LAST_DB_VERSION);
        version = DatabaseMigrationHelper.migrate(MessagesStorage.this, version);

        FileLog.d("MessagesStorage db migration finished to varsion " + version);
        AndroidUtilities.runOnUIThread(() -> {
            databaseMigrationInProgress = false;
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.onDatabaseMigration, false);
        });
    }

    private void cleanupInternal(boolean deleteFiles) {
        if (deleteFiles) {
            reset();
        } else {
            clearDatabaseValues();
        }
        if (database != null) {
            database.close();
            database = null;
        }
        if (deleteFiles) {
            if (cacheFile != null) {
                cacheFile.delete();
                cacheFile = null;
            }
            if (walCacheFile != null) {
                walCacheFile.delete();
                walCacheFile = null;
            }
            if (shmCacheFile != null) {
                shmCacheFile.delete();
                shmCacheFile = null;
            }

        }
    }

    public void clearDatabaseValues() {
        lastDateValue = 0;
        lastSeqValue = 0;
        lastPtsValue = 0;
        lastQtsValue = 0;
        lastSecretVersion = 0;
        mainUnreadCount = 0;
        archiveUnreadCount = 0;
        pendingMainUnreadCount = 0;
        pendingArchiveUnreadCount = 0;
        dialogFilters.clear();
        dialogFiltersMap.clear();
        unknownDialogsIds.clear();

        lastSavedSeq = 0;
        lastSavedPts = 0;
        lastSavedDate = 0;
        lastSavedQts = 0;

        secretPBytes = null;
        secretG = 0;
    }

    public void cleanup(boolean isLogin) {
        storageQueue.postRunnable(() -> {
            cleanupInternal(true);
            openDatabase(1);
            if (isLogin) {
                Utilities.stageQueue.postRunnable(() -> getMessagesController().getDifference());
            }
        });
    }

    public void saveSecretParams(int lsv, int sg, byte[] pbytes) {
        storageQueue.postRunnable(() -> {
            try {
                SQLitePreparedStatement state = database.executeFast("UPDATE params SET lsv = ?, sg = ?, pbytes = ? WHERE id = 1");
                state.bindInteger(1, lsv);
                state.bindInteger(2, sg);
                NativeByteBuffer data = new NativeByteBuffer(pbytes != null ? pbytes.length : 1);
                if (pbytes != null) {
                    data.writeBytes(pbytes);
                }
                state.bindByteBuffer(3, data);
                state.step();
                state.dispose();
                data.reuse();
            } catch (Exception e) {
                checkSQLException(e);
            }
        });
    }

    boolean tryRecover;

    public void checkSQLException(Throwable e) {
        checkSQLException(e, true);
    }

    private void checkSQLException(Throwable e, boolean logToAppCenter) {
        if (e instanceof SQLiteException && e.getMessage() != null && e.getMessage().contains("is malformed") && !tryRecover) {
            tryRecover = true;
            FileLog.e("disk image malformed detected, try recover");
            if (recoverDatabase()) {
                tryRecover = false;
                clearLoadingDialogsOffsets();
                AndroidUtilities.runOnUIThread(() -> {
                   getNotificationCenter().postNotificationName(NotificationCenter.onDatabaseReset);
                });
                FileLog.e(new Exception("database restored!!"));
            } else {
                FileLog.e(new Exception(e), logToAppCenter);
            }
        } else {
            FileLog.e(e, logToAppCenter);
        }
    }

    public void fixNotificationSettings() {
        storageQueue.postRunnable(() -> {
            try {
                LongSparseArray<Long> ids = new LongSparseArray<>();
                SharedPreferences preferences = MessagesController.getNotificationsSettings(currentAccount);
                Map<String, ?> values = preferences.getAll();
                for (Map.Entry<String, ?> entry : values.entrySet()) {
                    String key = entry.getKey();
                    if (key.startsWith("notify2_")) {
                        Integer value = (Integer) entry.getValue();
                        if (value == 2 || value == 3) {
                            key = key.replace("notify2_", "");
                            long flags;
                            if (value == 2) {
                                flags = 1;
                            } else {
                                Integer time = (Integer) values.get("notifyuntil_" + key);
                                if (time != null) {
                                    flags = ((long) time << 32) | 1;
                                } else {
                                    flags = 1;
                                }
                            }
                            try {
                                ids.put(Long.parseLong(key), flags);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                try {
                    database.beginTransaction();
                    SQLitePreparedStatement state = database.executeFast("REPLACE INTO dialog_settings VALUES(?, ?)");
                    for (int a = 0; a < ids.size(); a++) {
                        state.requery();
                        state.bindLong(1, ids.keyAt(a));
                        state.bindLong(2, ids.valueAt(a));
                        state.step();
                    }
                    state.dispose();
                    database.commitTransaction();
                } catch (Exception e) {
                    checkSQLException(e);
                }
            } catch (Throwable e) {
                checkSQLException(e);
            }
        });
    }

    public long createPendingTask(NativeByteBuffer data) {
        if (data == null) {
            return 0;
        }
        long id = lastTaskId.getAndAdd(1);
        storageQueue.postRunnable(() -> {
            try {
                SQLitePreparedStatement state = database.executeFast("REPLACE INTO pending_tasks VALUES(?, ?)");
                state.bindLong(1, id);
                state.bindByteBuffer(2, data);
                state.step();
                state.dispose();
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                data.reuse();
            }
        });
        return id;
    }

    public void removePendingTask(long id) {
        storageQueue.postRunnable(() -> {
            try {
                database.executeFast("DELETE FROM pending_tasks WHERE id = " + id).stepThis().dispose();
            } catch (Exception e) {
                checkSQLException(e);
            }
        });
    }

    private void loadPendingTasks() {
        storageQueue.postRunnable(() -> {
            try {
                SQLiteCursor cursor = database.queryFinalized("SELECT id, data FROM pending_tasks WHERE 1");
                while (cursor.next()) {
                    long taskId = cursor.longValue(0);
                    NativeByteBuffer data = cursor.byteBufferValue(1);
                    if (data != null) {
                        int type = data.readInt32(false);
                        switch (type) {
                            case 0: {
                                TLRPC.Chat chat = TLRPC.Chat.TLdeserialize(data, data.readInt32(false), false);
                                if (chat != null) {
                                    Utilities.stageQueue.postRunnable(() -> getMessagesController().loadUnknownChannel(chat, taskId));
                                }
                                break;
                            }
                            case 1: {
                                long channelId = data.readInt32(false);
                                int newDialogType = data.readInt32(false);
                                Utilities.stageQueue.postRunnable(() -> getMessagesController().getChannelDifference(channelId, newDialogType, taskId, null));
                                break;
                            }
                            case 2:
                            case 5:
                            case 8:
                            case 10:
                            case 14: {
                                TLRPC.Dialog dialog = new TLRPC.TL_dialog();
                                dialog.id = data.readInt64(false);
                                dialog.top_message = data.readInt32(false);
                                dialog.read_inbox_max_id = data.readInt32(false);
                                dialog.read_outbox_max_id = data.readInt32(false);
                                dialog.unread_count = data.readInt32(false);
                                dialog.last_message_date = data.readInt32(false);
                                dialog.pts = data.readInt32(false);
                                dialog.flags = data.readInt32(false);
                                if (type >= 5) {
                                    dialog.pinned = data.readBool(false);
                                    dialog.pinnedNum = data.readInt32(false);
                                }
                                if (type >= 8) {
                                    dialog.unread_mentions_count = data.readInt32(false);
                                }
                                if (type >= 10) {
                                    dialog.unread_mark = data.readBool(false);
                                }
                                if (type >= 14) {
                                    dialog.folder_id = data.readInt32(false);
                                }
                                TLRPC.InputPeer peer = TLRPC.InputPeer.TLdeserialize(data, data.readInt32(false), false);
                                AndroidUtilities.runOnUIThread(() -> getMessagesController().checkLastDialogMessage(dialog, peer, taskId));
                                break;
                            }
                            case 3: {
                                long random_id = data.readInt64(false);
                                TLRPC.InputPeer peer = TLRPC.InputPeer.TLdeserialize(data, data.readInt32(false), false);
                                TLRPC.TL_inputMediaGame game = (TLRPC.TL_inputMediaGame) TLRPC.InputMedia.TLdeserialize(data, data.readInt32(false), false);
                                getSendMessagesHelper().sendGame(peer, game, random_id, taskId);
                                break;
                            }
                            case 4: {
                                long did = data.readInt64(false);
                                boolean pin = data.readBool(false);
                                TLRPC.InputPeer peer = TLRPC.InputPeer.TLdeserialize(data, data.readInt32(false), false);
                                AndroidUtilities.runOnUIThread(() -> getMessagesController().pinDialog(did, pin, peer, taskId));
                                break;
                            }
                            case 6: {
                                long channelId = data.readInt32(false);
                                int newDialogType = data.readInt32(false);
                                TLRPC.InputChannel inputChannel = TLRPC.InputChannel.TLdeserialize(data, data.readInt32(false), false);
                                Utilities.stageQueue.postRunnable(() -> getMessagesController().getChannelDifference(channelId, newDialogType, taskId, inputChannel));
                                break;
                            }
                            case 25: {
                                long channelId = data.readInt64(false);
                                int newDialogType = data.readInt32(false);
                                TLRPC.InputChannel inputChannel = TLRPC.InputChannel.TLdeserialize(data, data.readInt32(false), false);
                                Utilities.stageQueue.postRunnable(() -> getMessagesController().getChannelDifference(channelId, newDialogType, taskId, inputChannel));
                                break;
                            }
                            case 7: {
                                long channelId = data.readInt32(false);
                                int constructor = data.readInt32(false);
                                TLObject request = TLRPC.TL_messages_deleteMessages.TLdeserialize(data, constructor, false);
                                if (request == null) {
                                    request = TLRPC.TL_channels_deleteMessages.TLdeserialize(data, constructor, false);
                                }
                                if (request == null) {
                                    removePendingTask(taskId);
                                } else {
                                    TLObject finalRequest = request;
                                    AndroidUtilities.runOnUIThread(() -> getMessagesController().deleteMessages(null, null, null, -channelId, true, 0, false, taskId, finalRequest, 0));
                                }
                                break;
                            }
                            case 24: {
                                long dialogId = data.readInt64(false);
                                int constructor = data.readInt32(false);
                                TLObject request = TLRPC.TL_messages_deleteMessages.TLdeserialize(data, constructor, false);
                                if (request == null) {
                                    request = TLRPC.TL_channels_deleteMessages.TLdeserialize(data, constructor, false);
                                }
                                if (request == null) {
                                    removePendingTask(taskId);
                                } else {
                                    TLObject finalRequest = request;
                                    AndroidUtilities.runOnUIThread(() -> getMessagesController().deleteMessages(null, null, null, dialogId, true, 0, false, taskId, finalRequest, 0));
                                }
                                break;
                            }
                            case 103: {
                                long dialogId = data.readInt64(false);
                                int topicId = data.readInt32(false);
                                int constructor = data.readInt32(false);
                                TLObject request = TLRPC.TL_messages_deleteMessages.TLdeserialize(data, constructor, false);
                                if (request == null) {
                                    request = TLRPC.TL_channels_deleteMessages.TLdeserialize(data, constructor, false);
                                }
                                if (request == null) {
                                    removePendingTask(taskId);
                                } else {
                                    TLObject finalRequest = request;
                                    AndroidUtilities.runOnUIThread(() -> getMessagesController().deleteMessages(null, null, null, dialogId, true, 0, false, taskId, finalRequest, topicId));
                                }
                                break;
                            }
                            case 9: {
                                long did = data.readInt64(false);
                                TLRPC.InputPeer peer = TLRPC.InputPeer.TLdeserialize(data, data.readInt32(false), false);
                                AndroidUtilities.runOnUIThread(() -> getMessagesController().markDialogAsUnread(did, peer, taskId));
                                break;
                            }
                            case 11: {
                                TLRPC.InputChannel inputChannel;
                                int mid = data.readInt32(false);
                                long channelId = data.readInt32(false);
                                int ttl = data.readInt32(false);
                                if (channelId != 0) {
                                    inputChannel = TLRPC.InputChannel.TLdeserialize(data, data.readInt32(false), false);
                                } else {
                                    inputChannel = null;
                                }
                                AndroidUtilities.runOnUIThread(() -> getMessagesController().markMessageAsRead2(-channelId, mid, inputChannel, ttl, taskId));
                                break;
                            }
                            case 101:
                            case 23: {
                                TLRPC.InputChannel inputChannel;
                                long dialogId = data.readInt64(false);
                                int mid = data.readInt32(false);
                                int ttl = data.readInt32(false);
                                if (!DialogObject.isEncryptedDialog(dialogId) && DialogObject.isChatDialog(dialogId) && data.hasRemaining()) {
                                    inputChannel = TLRPC.InputChannel.TLdeserialize(data, data.readInt32(false), false);
                                } else {
                                    inputChannel = null;
                                }
                                AndroidUtilities.runOnUIThread(() -> getMessagesController().markMessageAsRead2(dialogId, mid, inputChannel, ttl, taskId, type == 23));
                                break;
                            }
                            case 12:
                            case 19:
                            case 20:
                                removePendingTask(taskId);
                                break;
                            case 21: {
                                Theme.OverrideWallpaperInfo info = new Theme.OverrideWallpaperInfo();
                                long id = data.readInt64(false);
                                info.isBlurred = data.readBool(false);
                                info.isMotion = data.readBool(false);
                                info.color = data.readInt32(false);
                                info.gradientColor1 = data.readInt32(false);
                                info.rotation = data.readInt32(false);
                                info.intensity = (float) data.readDouble(false);
                                boolean install = data.readBool(false);
                                info.slug = data.readString(false);
                                info.originalFileName = data.readString(false);
                                AndroidUtilities.runOnUIThread(() -> getMessagesController().saveWallpaperToServer(null, info, install, taskId));
                                break;
                            }
                            case 13: {
                                long did = data.readInt64(false);
                                boolean first = data.readBool(false);
                                int onlyHistory = data.readInt32(false);
                                int maxIdDelete = data.readInt32(false);
                                boolean revoke = data.readBool(false);
                                TLRPC.InputPeer inputPeer = TLRPC.InputPeer.TLdeserialize(data, data.readInt32(false), false);
                                AndroidUtilities.runOnUIThread(() -> getMessagesController().deleteDialog(did, first ? 1 : 0, onlyHistory, maxIdDelete, revoke, inputPeer, taskId));
                                break;
                            }
                            case 15: {
                                TLRPC.InputPeer inputPeer = TLRPC.InputPeer.TLdeserialize(data, data.readInt32(false), false);
                                Utilities.stageQueue.postRunnable(() -> getMessagesController().loadUnknownDialog(inputPeer, taskId));
                                break;
                            }
                            case 16: {
                                int folderId = data.readInt32(false);
                                int count = data.readInt32(false);
                                ArrayList<TLRPC.InputDialogPeer> peers = new ArrayList<>();
                                for (int a = 0; a < count; a++) {
                                    TLRPC.InputDialogPeer inputPeer = TLRPC.InputDialogPeer.TLdeserialize(data, data.readInt32(false), false);
                                    peers.add(inputPeer);
                                }
                                AndroidUtilities.runOnUIThread(() -> getMessagesController().reorderPinnedDialogs(folderId, peers, taskId));
                                break;
                            }
                            case 17: {
                                int folderId = data.readInt32(false);
                                int count = data.readInt32(false);
                                ArrayList<TLRPC.TL_inputFolderPeer> peers = new ArrayList<>();
                                for (int a = 0; a < count; a++) {
                                    TLRPC.TL_inputFolderPeer inputPeer = TLRPC.TL_inputFolderPeer.TLdeserialize(data, data.readInt32(false), false);
                                    peers.add(inputPeer);
                                }
                                AndroidUtilities.runOnUIThread(() -> getMessagesController().addDialogToFolder(null, folderId, -1, peers, taskId));
                                break;
                            }
                            case 18: {
                                long dialogId = data.readInt64(false);
                                data.readInt32(false);
                                int constructor = data.readInt32(false);
                                TLObject request = TLRPC.TL_messages_deleteScheduledMessages.TLdeserialize(data, constructor, false);
                                if (request == null) {
                                    removePendingTask(taskId);
                                } else {
                                    AndroidUtilities.runOnUIThread(() -> getMessagesController().deleteMessages(null, null, null, dialogId, true, ChatActivity.MODE_SCHEDULED, false, taskId, request, 0));
                                }
                                break;
                            }
                            case 22: {
                                TLRPC.InputPeer inputPeer = TLRPC.InputPeer.TLdeserialize(data, data.readInt32(false), false);
                                AndroidUtilities.runOnUIThread(() -> getMessagesController().reloadMentionsCountForChannel(inputPeer, taskId));
                                break;
                            }
                            case 100: {
                                final int chatId = data.readInt32(false);
                                final boolean revoke = data.readBool(false);
                                AndroidUtilities.runOnUIThread(() -> getSecretChatHelper().declineSecretChat(chatId, revoke, taskId));
                                break;
                            }
                            case 102: {
                                long dialogId = data.readInt64(false);
                                int mid = data.readInt32(false);
                                AndroidUtilities.runOnUIThread(() -> getMessagesController().doDeleteShowOnceTask(taskId, dialogId, mid));
                                break;
                            }
                        }
                        data.reuse();
                    }
                }
                cursor.dispose();
            } catch (Exception e) {
                checkSQLException(e);
            }
        });
    }

    public void saveChannelPts(long channelId, int pts) {
        storageQueue.postRunnable(() -> {
            try {
                SQLitePreparedStatement state = database.executeFast("UPDATE dialogs SET pts = ? WHERE did = ?");
                state.bindInteger(1, pts);
                state.bindLong(2, -channelId);
                state.step();
                state.dispose();
            } catch (Exception e) {
                checkSQLException(e);
            }
        });
    }

    private void saveDiffParamsInternal(int seq, int pts, int date, int qts) {
        try {
            if (lastSavedSeq == seq && lastSavedPts == pts && lastSavedDate == date && lastQtsValue == qts) {
                return;
            }
            SQLitePreparedStatement state = database.executeFast("UPDATE params SET seq = ?, pts = ?, date = ?, qts = ? WHERE id = 1");
            state.bindInteger(1, seq);
            state.bindInteger(2, pts);
            state.bindInteger(3, date);
            state.bindInteger(4, qts);
            state.step();
            state.dispose();
            lastSavedSeq = seq;
            lastSavedPts = pts;
            lastSavedDate = date;
            lastSavedQts = qts;
        } catch (Exception e) {
            checkSQLException(e);
        }
    }

    public void saveDiffParams(int seq, int pts, int date, int qts) {
        storageQueue.postRunnable(() -> saveDiffParamsInternal(seq, pts, date, qts));
    }

    public void updateMutedDialogsFiltersCounters() {
        storageQueue.postRunnable(() -> resetAllUnreadCounters(true));
    }

    public void setDialogFlags(long did, long flags) {
        storageQueue.postRunnable(() -> {
            try {
                int oldFlags = 0;
                SQLiteCursor cursor = database.queryFinalized("SELECT flags FROM dialog_settings WHERE did = " + did);
                if (cursor.next()) {
                    oldFlags = cursor.intValue(0);
                }
                cursor.dispose();
                if (flags == oldFlags) {
                    return;
                }
                database.executeFast(String.format(Locale.US, "REPLACE INTO dialog_settings VALUES(%d, %d)", did, flags)).stepThis().dispose();
                resetAllUnreadCounters(true);
            } catch (Exception e) {
                checkSQLException(e);
            }
        });
    }

    public void putStoryPushMessage(NotificationsController.StoryNotification push) {
        storageQueue.postRunnable(() -> {
            try {
                database.executeFast("DELETE FROM story_pushes WHERE uid = " + push.dialogId).stepThis().dispose();
                SQLitePreparedStatement state = database.executeFast("REPLACE INTO story_pushes VALUES(?, ?, ?, ?, ?, ?)");
                for (Map.Entry<Integer, Pair<Long, Long>> e : push.dateByIds.entrySet()) {
                    int id = e.getKey();
                    long date = e.getValue().first;
                    long expire_date = e.getValue().second;
                    state.requery();
                    state.bindLong(1, push.dialogId);
                    state.bindInteger(2, id);
                    state.bindLong(3, date);
                    if (push.localName == null) {
                        push.localName = "";
                    }
                    state.bindString(4, push.localName);
                    state.bindInteger(5, push.hidden ? 1 : 0);
                    state.bindLong(6, expire_date);
                    state.step();
                }
                state.dispose();
            } catch (Exception e) {
                checkSQLException(e);
            }
        });
    }

    public void deleteStoryPushMessage(long dialogId) {
        storageQueue.postRunnable(() -> {
            try {
                database.executeFast("DELETE FROM story_pushes WHERE uid = " + dialogId).stepThis().dispose();
            } catch (Exception e) {
                checkSQLException(e);
            }
        });
    }

    public void deleteAllStoryPushMessages() {
        storageQueue.postRunnable(() -> {
            try {
                database.executeFast("DELETE FROM story_pushes").stepThis().dispose();
            } catch (Exception e) {
                checkSQLException(e);
            }
        });
    }

    public void deleteAllStoryReactionPushMessages() {
        storageQueue.postRunnable(() -> {
            try {
                database.executeFast("DELETE FROM unread_push_messages WHERE is_reaction = 2").stepThis().dispose();
            } catch (Exception e) {
                checkSQLException(e);
            }
        });
    }

    public void putPushMessage(MessageObject message) {
        storageQueue.postRunnable(() -> {
            try {
                NativeByteBuffer data = new NativeByteBuffer(message.messageOwner.getObjectSize());
                message.messageOwner.serializeToStream(data);

                int flags = 0;
                if (message.localType == 2) {
                    flags |= 1;
                }
                if (message.localChannel) {
                    flags |= 2;
                }

                SQLitePreparedStatement state = database.executeFast("REPLACE INTO unread_push_messages VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                state.requery();
                state.bindLong(1, message.getDialogId());
                state.bindInteger(2, message.getId());
                state.bindLong(3, message.messageOwner.random_id);
                state.bindInteger(4, message.messageOwner.date);
                state.bindByteBuffer(5, data);
                if (message.messageText == null) {
                    state.bindNull(6);
                } else {
                    state.bindString(6, message.messageText.toString());
                }
                if (message.localName == null) {
                    state.bindNull(7);
                } else {
                    state.bindString(7, message.localName);
                }
                if (message.localUserName == null) {
                    state.bindNull(8);
                } else {
                    state.bindString(8, message.localUserName);
                }
                state.bindInteger(9, flags);
                state.bindLong(10, MessageObject.getTopicId(currentAccount, message.messageOwner, false));
                state.bindInteger(11, (message.isReactionPush ? 1 : 0) + (message.isStoryReactionPush ? 1 : 0));
                state.step();

                data.reuse();
                state.dispose();
            } catch (Exception e) {
                checkSQLException(e);
            }
        });
    }

    public void clearLocalDatabase() {
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            SQLitePreparedStatement state5 = null;
            SQLitePreparedStatement state6 = null;
            try {
                ArrayList<Long> dialogsToCleanup = new ArrayList<>();

                database.executeFast("DELETE FROM ephemeral_messages").stepThis().dispose();
                database.executeFast("DELETE FROM poll_votes_mentions").stepThis().dispose();
                database.executeFast("DELETE FROM poll_votes_mentions_topics").stepThis().dispose();
                database.executeFast("DELETE FROM reaction_mentions").stepThis().dispose();
                database.executeFast("DELETE FROM reaction_mentions_topics").stepThis().dispose();
                database.executeFast("DELETE FROM downloading_documents").stepThis().dispose();
                database.executeFast("DELETE FROM attach_menu_bots").stepThis().dispose();
                database.executeFast("DELETE FROM animated_emoji").stepThis().dispose();
                database.executeFast("DELETE FROM stickers_v2").stepThis().dispose();
                database.executeFast("DELETE FROM stickersets2").stepThis().dispose();
                database.executeFast("DELETE FROM messages_holes_topics").stepThis().dispose();
                database.executeFast("DELETE FROM messages_topics").stepThis().dispose();
                database.executeFast("DELETE FROM saved_dialogs").stepThis().dispose();
                database.executeFast("DELETE FROM topics").stepThis().dispose();
                database.executeFast("DELETE FROM media_holes_topics").stepThis().dispose();
                database.executeFast("DELETE FROM media_topics").stepThis().dispose();
                database.executeFast("DELETE FROM media_counts_topics").stepThis().dispose();
                database.executeFast("DELETE FROM chat_pinned_v2").stepThis().dispose();
                database.executeFast("DELETE FROM chat_pinned_count").stepThis().dispose();
                database.executeFast("DELETE FROM profile_stories").stepThis().dispose();
                database.executeFast("DELETE FROM profile_stories_albums").stepThis().dispose();
                database.executeFast("DELETE FROM profile_stories_albums_links").stepThis().dispose();
                database.executeFast("DELETE FROM story_pushes").stepThis().dispose();
                database.executeFast("DELETE FROM dialog_photos").stepThis().dispose();
                database.executeFast("DELETE FROM dialog_photos_count").stepThis().dispose();
                database.executeFast("DELETE FROM saved_reaction_tags").stepThis().dispose();
                database.executeFast("DELETE FROM business_replies").stepThis().dispose();
                database.executeFast("DELETE FROM quick_replies_messages").stepThis().dispose();
                database.executeFast("DELETE FROM welcome_messages").stepThis().dispose();
                database.executeFast("DELETE FROM effects").stepThis().dispose();
                database.executeFast("DELETE FROM app_config").stepThis().dispose();
                database.executeFast("DELETE FROM star_gifts2").stepThis().dispose();
                database.executeFast("DELETE FROM premium_promo").stepThis().dispose();
                database.executeFast("DELETE FROM media_counts_v2").stepThis().dispose();
                database.executeFast("DELETE FROM media_v4").stepThis().dispose();


                cursor = database.queryFinalized("SELECT did FROM dialogs WHERE 1");
                while (cursor.next()) {
                    long did = cursor.longValue(0);
                    if (!DialogObject.isEncryptedDialog(did)) {
                        dialogsToCleanup.add(did);
                    }
                }
                cursor.dispose();
                cursor = null;

                state5 = database.executeFast("REPLACE INTO messages_holes VALUES(?, ?, ?)");
                state6 = database.executeFast("REPLACE INTO media_holes_v2 VALUES(?, ?, ?, ?)");

                database.beginTransaction();
                for (int a = 0; a < dialogsToCleanup.size(); a++) {
                    Long did = dialogsToCleanup.get(a);
                    int messagesCount = 0;
                    cursor = database.queryFinalized("SELECT COUNT(mid) FROM messages_v2 WHERE uid = " + did);
                    if (cursor.next()) {
                        messagesCount = cursor.intValue(0);
                    }
                    cursor.dispose();
                    if (messagesCount <= 2) {
                        continue;
                    }

                    cursor = database.queryFinalized("SELECT last_mid_i, last_mid FROM dialogs WHERE did = " + did);
                    int messageId = -1;
                    if (cursor.next()) {
                        long last_mid_i = cursor.longValue(0);
                        long last_mid = cursor.longValue(1);
                        SQLiteCursor cursor2 = database.queryFinalized("SELECT data FROM messages_v2 WHERE uid = " + did + " AND mid IN (" + last_mid_i + "," + last_mid + ")");
                        try {
                            while (cursor2.next()) {
                                NativeByteBuffer data = cursor2.byteBufferValue(0);
                                if (data != null) {
                                    TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                                    if (message != null) {
                                        messageId = message.id;
                                        message.readAttachPath(data, UserConfig.getInstance(currentAccount).clientUserId);
                                    }
                                    data.reuse();
                                }
                            }
                        } catch (Exception e) {
                            checkSQLException(e);
                        }
                        cursor2.dispose();

                        database.executeFast("DELETE FROM messages_v2 WHERE uid = " + did + " AND mid != " + last_mid_i + " AND mid != " + last_mid).stepThis().dispose();
                        database.executeFast("DELETE FROM messages_holes WHERE uid = " + did).stepThis().dispose();
                        database.executeFast("DELETE FROM bot_keyboard WHERE uid = " + did).stepThis().dispose();
                        database.executeFast("DELETE FROM bot_keyboard_topics WHERE uid = " + did).stepThis().dispose();
                        database.executeFast("DELETE FROM media_counts_v2 WHERE uid = " + did).stepThis().dispose();
                        database.executeFast("DELETE FROM media_v4 WHERE uid = " + did).stepThis().dispose();
                        database.executeFast("DELETE FROM media_holes_v2 WHERE uid = " + did).stepThis().dispose();
                        MediaDataController.getInstance(currentAccount).clearBotKeyboard(did);
                        if (messageId != -1) {
                            MessagesStorage.createFirstHoles(did, state5, state6, messageId, 0);
                        }
                    }
                    cursor.dispose();
                    cursor = null;
                }

                state5.dispose();
                state6.dispose();
                state5 = null;
                state6 = null;
                database.commitTransaction();
                database.executeFast("PRAGMA journal_size_limit = 0").stepThis().dispose();
                database.executeFast("VACUUM").stepThis().dispose();
                database.executeFast("PRAGMA journal_size_limit = -1").stepThis().dispose();

                getMessagesController().getTopicsController().databaseCleared();
                AndroidUtilities.runOnUIThread(() -> {
                    getMessagesController().getSavedMessagesController().cleanup();
                });
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (database != null) {
                    database.commitTransaction();
                }
                if (state5 != null) {
                    state5.dispose();
                }
                if (state6 != null) {
                    state6.dispose();
                }
                if (cursor != null) {
                    cursor.dispose();
                }
                reset();
            }
        });
    }

    public void updateRanksInLastMessages(long dialogId, long userId, String rank) {
        storageQueue.postRunnable(() -> {
            final ArrayList<Pair<Integer, TLRPC.Message>> messagesToUpdate = new ArrayList<>();
            SQLiteCursor cursor = null;
            SQLitePreparedStatement state = null;
            try {
                cursor = database.queryFinalized(String.format(Locale.US, "SELECT mid, data FROM messages_v2 WHERE uid = %s ORDER BY date DESC LIMIT 20", dialogId));
                while (cursor.next()) {
                    final int messageId = cursor.intValue(0);
                    final NativeByteBuffer data = cursor.byteBufferValue(1);
                    final TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                    if (message != null) {
                        message.readAttachPath(data, UserConfig.getInstance(currentAccount).clientUserId);
                    }
                    if (DialogObject.getPeerDialogId(message.from_id) == userId) {
                        message.flags2 |= TLObject.FLAG_12;
                        message.from_rank = rank;
                        messagesToUpdate.add(new Pair<>(messageId, message));
                    }
                }
                if (cursor != null) {
                    cursor.dispose();
                }
                for (int i = 0; i < messagesToUpdate.size(); ++i) {
                    final int messageId = messagesToUpdate.get(i).first;
                    final TLRPC.Message message = messagesToUpdate.get(i).second;
                    state = database.executeFast("UPDATE messages_v2 SET data = ? WHERE mid = ? AND uid = ?");
                    state.requery();
                    NativeByteBuffer data = new NativeByteBuffer(message.getObjectSize());
                    MessageObject.normalizeFlags(message);
                    message.serializeToStream(data);
                    state.bindByteBuffer(1, data);
                    state.bindInteger(2, messageId);
                    state.bindLong(3, dialogId);
                    state.step();
                    state.dispose();
                    state = null;
                    data.reuse();
                }
            } catch (Exception e) {
                FileLog.e(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
            final boolean updated = messagesToUpdate.size() > 0;
            if (updated) {
                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, 0);
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updatedChatRanks, -dialogId, userId, rank);
                });
            }
        });
    }

    public void saveTopics(long dialogId, List<TLRPC.TL_forumTopic> topics, boolean replace, boolean useQueue, int date) {
        if (useQueue) {
            storageQueue.postRunnable(() -> {
                saveTopicsInternal(dialogId, topics, replace, true, date);
            });
        } else {
            saveTopicsInternal(dialogId, topics, replace, false, date);
        }
    }

    private void saveTopicsInternal(long dialogId, List<TLRPC.TL_forumTopic> topics, boolean replace, boolean inTransaction, int date) {
        SQLitePreparedStatement state = null;
        try {
            HashSet<Integer> existingTopics = new HashSet<>();
            HashMap<Integer, Integer> pinnedValues = new HashMap<>();
            for (int i = 0; i < topics.size(); i++) {
                TLRPC.TL_forumTopic topic = topics.get(i);
                SQLiteCursor cursor = database.queryFinalized("SELECT did, pinned FROM topics WHERE did = " + dialogId + " AND topic_id = " + topic.id);
                boolean exist = cursor.next();
                if (exist) {
                    pinnedValues.put(i, cursor.intValue(2));
                }
                cursor.dispose();
                cursor = null;
                if (exist) {
                    existingTopics.add(i);
                }
            }
            if (replace) {
                database.executeFast("DELETE FROM topics WHERE did = " + dialogId).stepThis().dispose();
            }
            state = database.executeFast("REPLACE INTO topics VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            if (inTransaction) {
                database.beginTransaction();
            }

            for (int i = 0; i < topics.size(); i++) {
                TLRPC.TL_forumTopic topic = topics.get(i);
                long topicId = isMonoForum(dialogId) ? DialogObject.getPeerDialogId(topic.from_id): topic.id;
                boolean exist = existingTopics.contains(i);

                state.requery();
                state.bindLong(1, dialogId);
                state.bindLong(2, topicId);
                NativeByteBuffer data = new NativeByteBuffer(topic.getObjectSize());
                topic.serializeToStream(data);

                state.bindByteBuffer(3, data);
                state.bindInteger(4, topic.top_message);

                NativeByteBuffer messageData = new NativeByteBuffer(topic.topicStartMessage.getObjectSize());
                topic.topicStartMessage.serializeToStream(messageData);
                state.bindByteBuffer(5, messageData);
                state.bindInteger(6, topic.unread_count);
                state.bindInteger(7, topic.read_inbox_max_id);
                state.bindInteger(8, topic.unread_mentions_count);
                state.bindInteger(9, topic.unread_reactions_count);
                state.bindInteger(10, topic.read_outbox_max_id);
                if (topic.isShort && pinnedValues.containsKey(i)) {
                    state.bindInteger(11, pinnedValues.get(i));
                } else {
                    state.bindInteger(11, topic.pinned ? 1 + topic.pinnedOrder : 0);
                }
                state.bindInteger(12, topic.totalMessagesCount);
                state.bindInteger(13, topic.hidden ? 1 : 0);
                state.bindInteger(14, date);
                state.bindInteger(15, topic.nopaid_messages_exception ? 1 : 0);
                state.bindInteger(16, topic.unread_poll_votes_count);

                state.step();
                messageData.reuse();
                data.reuse();

                if (exist) {
                    closeHolesInTable("messages_holes_topics", dialogId, topic.top_message, topic.top_message, topicId);
                    closeHolesInMedia(dialogId, topic.top_message, topic.top_message, -1, 0);
                } else {
                    database.executeFast(String.format(Locale.ENGLISH, "DELETE FROM messages_holes_topics WHERE uid = %d AND topic_id = %d", dialogId, topicId)).stepThis().dispose();
                    database.executeFast(String.format(Locale.ENGLISH, "DELETE FROM media_holes_topics WHERE uid = %d AND topic_id = %d", dialogId, topicId)).stepThis().dispose();
                    database.executeFast(String.format(Locale.ENGLISH, "DELETE FROM messages_topics WHERE uid = %d AND topic_id = %d", dialogId, topicId)).stepThis().dispose();
                    database.executeFast(String.format(Locale.ENGLISH, "DELETE FROM media_topics WHERE uid = %d AND topic_id = %d", dialogId, topicId)).stepThis().dispose();

                    SQLitePreparedStatement state_holes = database.executeFast("REPLACE INTO messages_holes_topics VALUES(?, ?, ?, ?)");
                    SQLitePreparedStatement state_media_holes = database.executeFast("REPLACE INTO media_holes_topics VALUES(?, ?, ?, ?, ?)");
                    createFirstHoles(dialogId, state_holes, state_media_holes, topic.top_message, topicId);
                    state_holes.dispose();
                    state_holes.dispose();
                }
            }
            resetAllUnreadCounters(false);

        } catch (Exception e) {
            checkSQLException(e);

        } finally {
            if (state != null) {
                state.dispose();
            }
            database.commitTransaction();
        }
    }

    public void updateTopicData(long dialogId, TLRPC.TL_forumTopic fromTopic, int flags) {
        updateTopicData(dialogId, fromTopic, flags, getConnectionsManager().getCurrentTime());
    }

    public void updateTopicData(long dialogId, TLRPC.TL_forumTopic fromTopic, int flags, int date) {
        if (fromTopic == null) {
            return;
        }
        storageQueue.postRunnable(() -> {
            SQLitePreparedStatement state = null;
            SQLiteCursor cursor = null;
            try {
                if ((flags & TopicsController.TOPIC_FLAG_TOTAL_MESSAGES_COUNT) != 0) {
                    state = database.executeFast("UPDATE topics SET total_messages_count = ? WHERE did = ? AND topic_id = ?");
                    state.requery();
                    state.bindInteger(1, fromTopic.totalMessagesCount);
                    state.bindLong(2, dialogId);
                    state.bindLong(3, isMonoForum(dialogId) ? DialogObject.getPeerDialogId(fromTopic.from_id) : fromTopic.id);
                    state.step();
                    state.dispose();
                    if (flags == TopicsController.TOPIC_FLAG_TOTAL_MESSAGES_COUNT) {
                        return;
                    }
                }
                int currentEditDate = 0;
                TLRPC.TL_forumTopic topicToUpdate = null;
                cursor = database.queryFinalized(String.format(Locale.US, "SELECT data, edit_date FROM topics WHERE did = %d AND topic_id = %d", dialogId, fromTopic.id));
                if (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(0);
                    currentEditDate = cursor.intValue(1);
                    if (data != null) {
                        topicToUpdate = TLRPC.TL_forumTopic.TLdeserialize(data, data.readInt32(true), true);
                        data.reuse();
                    }
                }
                cursor.dispose();
                cursor = null;

                if (topicToUpdate != null && (currentEditDate == 0 || currentEditDate <= date)) {
                    if ((flags & TopicsController.TOPIC_FLAG_TITLE) != 0) {
                        topicToUpdate.title = fromTopic.title;
                    }
                    if ((flags & TopicsController.TOPIC_FLAG_ICON) != 0) {
                        topicToUpdate.icon_emoji_id = fromTopic.icon_emoji_id;
                        topicToUpdate.flags |= 1;
                    }
                    if ((flags & TopicsController.TOPIC_FLAG_PIN) != 0) {
                        topicToUpdate.pinned = fromTopic.pinned;
                        topicToUpdate.pinnedOrder = fromTopic.pinnedOrder;
                    }
                    int pinnedOrder = topicToUpdate.pinned ? 1 + topicToUpdate.pinnedOrder : 0;
                    if ((flags & TopicsController.TOPIC_FLAG_CLOSE) != 0) {
                        topicToUpdate.closed = fromTopic.closed;
                    }
                    if ((flags & TopicsController.TOPIC_FLAG_HIDE) != 0) {
                        topicToUpdate.hidden = fromTopic.hidden;
                    }
                    state = database.executeFast("UPDATE topics SET data = ?, pinned = ?, hidden = ?, edit_date = ? WHERE did = ? AND topic_id = ?");
                    database.beginTransaction();
                    NativeByteBuffer data = new NativeByteBuffer(topicToUpdate.getObjectSize());
                    topicToUpdate.serializeToStream(data);
                    state.bindByteBuffer(1, data);
                    state.bindInteger(2, pinnedOrder);
                    state.bindInteger(3, topicToUpdate.hidden ? 1 : 0);
                    state.bindInteger(4, date);
                    state.bindLong(5, dialogId);
                    state.bindLong(6, topicToUpdate.id);
                    state.step();
                    data.reuse();

                    int finalFlags = flags;
                    AndroidUtilities.runOnUIThread(() -> {
                        getMessagesController().getTopicsController().updateTopicInUi(dialogId, fromTopic, finalFlags);
                    });
                }
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (state != null) {
                    state.dispose();
                }
                if (cursor != null) {
                    cursor.dispose();
                }
                database.commitTransaction();
            }
        });
    }

    public void loadTopics(long dialogId, Consumer<ArrayList<TLRPC.TL_forumTopic>> callback) {
        storageQueue.postRunnable(() -> {
            ArrayList<TLRPC.TL_forumTopic> topics = null;
            SQLiteCursor cursor = null;
            try {
                cursor = database.queryFinalized(String.format(Locale.US, "SELECT top_message, data, topic_message, unread_count, max_read_id, unread_mentions, unread_reactions, read_outbox, pinned, total_messages_count, nopaid_messages_exception, unread_poll_votes FROM topics WHERE did = %d ORDER BY pinned ASC", dialogId));

                SparseArray<ArrayList<TLRPC.TL_forumTopic>> topicsByTopMessageId = null;
                HashSet<Integer> topMessageIds = null;
                while (cursor.next()) {
                    if (topics == null) {
                        topics = new ArrayList<>();
                        topicsByTopMessageId = new SparseArray<>();
                        topMessageIds = new HashSet<>();
                    }
                    int topMessageId = cursor.intValue(0);
                    NativeByteBuffer data = cursor.byteBufferValue(1);
                    if (data != null) {
                        TLRPC.TL_forumTopic topic = TLRPC.TL_forumTopic.TLdeserialize(data, data.readInt32(false), false);
                        if (topic != null) {
                            topic.top_message = topMessageId;
                            ArrayList<TLRPC.TL_forumTopic> topicsListByTopMessageId = topicsByTopMessageId.get(topMessageId);
                            if (topicsListByTopMessageId == null) {
                                topicsListByTopMessageId = new ArrayList<>();
                                topicsByTopMessageId.put(topMessageId, topicsListByTopMessageId);
                            }
                            topicsListByTopMessageId.add(topic);
                            topMessageIds.add(topMessageId);
                            topics.add(topic);

                            NativeByteBuffer data2 = cursor.byteBufferValue(2);
                            //if (data2 != null) {
                                topic.topicStartMessage = TLRPC.Message.TLdeserialize(data2, data2.readInt32(false), false);
                                if (data2 != null) {
                                    data2.reuse();
                                }
                           // }
                            topic.unread_count = cursor.intValue(3);
                            topic.read_inbox_max_id = cursor.intValue(4);
                            topic.unread_mentions_count = cursor.intValue(5);
                            topic.unread_reactions_count = cursor.intValue(6);
                            topic.read_outbox_max_id = cursor.intValue(7);
                            topic.pinnedOrder = cursor.intValue(8) - 1;
                            topic.pinned = topic.pinnedOrder >= 0;
                            topic.totalMessagesCount = cursor.intValue(9);
                            topic.nopaid_messages_exception = cursor.intValue(10) != 0;
                            topic.unread_poll_votes_count = cursor.intValue(11);
                        }

                        data.reuse();
                    }
                }
                ArrayList<Long> usersToLoad = new ArrayList<>();
                ArrayList<Long> chatsToLoad = new ArrayList<>();
                LongSparseArray<SparseArray<ArrayList<TLRPC.Message>>> replyMessageOwners = new LongSparseArray<>();
                LongSparseArray<ArrayList<Integer>> dialogReplyMessagesIds = new LongSparseArray<>();


                if (topics != null && !topics.isEmpty()) {
                    SQLiteCursor cursor2 = database.queryFinalized("SELECT mid, data, replydata FROM messages_v2 WHERE uid = " + dialogId + " AND mid IN (" + TextUtils.join(",", topMessageIds) + ")");
                    while (cursor2.next()) {
                        int messageId = cursor2.intValue(0);
                        NativeByteBuffer data = cursor2.byteBufferValue(1);
                        if (data != null) {
                            TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                            if (message != null) {
                                message.readAttachPath(data, UserConfig.getInstance(currentAccount).clientUserId);
                            }
                            data.reuse();

                            topMessageIds.remove(messageId);
                            ArrayList<TLRPC.TL_forumTopic> topicsList = topicsByTopMessageId.get(messageId);
                            if (topicsList != null) {
                                for (int i = 0; i < topicsList.size(); i++) {
                                    topicsList.get(i).topMessage = message;
                                }
                            }

                            addUsersAndChatsFromMessage(message, usersToLoad, chatsToLoad, null);

                            try {
                                if (message != null && message.reply_to != null && message.reply_to.reply_to_msg_id != 0 && isMessageActionTypeWithReply(message.action)) {
                                    if (!cursor2.isNull(2)) {
                                        NativeByteBuffer data2 = cursor2.byteBufferValue(2);
                                        if (data2 != null) {
                                            message.replyMessage = TLRPC.Message.TLdeserialize(data2, data2.readInt32(false), false);
                                            message.replyMessage.readAttachPath(data2, getUserConfig().clientUserId);
                                            data2.reuse();
                                            if (message.replyMessage != null) {
                                                addUsersAndChatsFromMessage(message.replyMessage, usersToLoad, chatsToLoad, null);
                                            }
                                        }
                                    }
                                    if (message.replyMessage == null) {
                                        addReplyMessages(message, replyMessageOwners, dialogReplyMessagesIds);
                                    }
                                }
                            } catch (Exception e) {
                                checkSQLException(e);
                            }
                        }
                    }

                    cursor2.dispose();
                    if (!topMessageIds.isEmpty()) {
                        cursor2 = database.queryFinalized("SELECT mid, data FROM messages_topics WHERE uid = " + dialogId + " AND mid IN (" + TextUtils.join(",", topMessageIds) + ")");
                        try {
                            while (cursor2.next()) {
                                int messageId = cursor2.intValue(0);
                                NativeByteBuffer data = cursor2.byteBufferValue(1);
                                if (data != null) {
                                    TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                                    if (message != null) {
                                        message.readAttachPath(data, UserConfig.getInstance(currentAccount).clientUserId);
                                    }
                                    data.reuse();

                                    topMessageIds.remove(messageId);
                                    addUsersAndChatsFromMessage(message, usersToLoad, chatsToLoad, null);

                                    ArrayList<TLRPC.TL_forumTopic> topicsList = topicsByTopMessageId.get(messageId);
                                    if (topicsList != null) {
                                        for (int i = 0; i < topicsList.size(); i++) {
                                            topicsList.get(i).topMessage = message;
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            checkSQLException(e);
                        }
                    }
                    for (TLRPC.TL_forumTopic topic : topics) {
                        long fromId = DialogObject.getPeerDialogId(topic.from_id);
                        if (fromId == 0) {
                            continue;
                        }
                        if (fromId > 0) {
                            if (!usersToLoad.contains(fromId)) {
                                usersToLoad.add(fromId);
                            }
                        } else {
                            if (!chatsToLoad.contains(fromId)) {
                                chatsToLoad.add(fromId);
                            }
                        }
                    }

                    loadReplyMessages(replyMessageOwners, dialogReplyMessagesIds, usersToLoad, chatsToLoad, 0);

                    ArrayList<TLRPC.Chat> chats = new ArrayList<>();
                    ArrayList<TLRPC.User> users = new ArrayList<>();
                    if (!chatsToLoad.isEmpty()) {
                        getChatsInternal(TextUtils.join(",", chatsToLoad), chats);
                    }
                    if (!usersToLoad.isEmpty()) {
                        getUsersInternal(usersToLoad, users);
                    }

                    AndroidUtilities.runOnUIThread(() -> {
                        if (!users.isEmpty()) {
                            getMessagesController().putUsers(users, true);
                        }
                        if (!chats.isEmpty()) {
                            getMessagesController().putChats(chats, true);
                        }
                    });

                    loadGroupedMessagesForTopics(dialogId, topics);
                }

            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
            callback.accept(topics);
        });
    }

    public void loadGroupedMessagesForTopicUpdates(ArrayList<TopicsController.TopicUpdate> topics) {
        if (topics == null) {
            return;
        }
        try {
            LongSparseArray<ArrayList<TopicsController.TopicUpdate>> topicsByGroupedId = new LongSparseArray<>();

            for (int i = 0; i < topics.size(); i++) {
                if (topics.get(i).reloadTopic || topics.get(i).onlyCounters || topics.get(i).topMessage == null) {
                    continue;
                }
                long groupId = topics.get(i).topMessage.grouped_id;
                if (groupId != 0) {
                    ArrayList<TopicsController.TopicUpdate> array = topicsByGroupedId.get(groupId);
                    if (array == null) {
                        array = new ArrayList<>();
                        topicsByGroupedId.put(groupId, array);
                    }
                    array.add(topics.get(i));
                }
            }
            for (int i = 0; i < topicsByGroupedId.size(); i++) {
                long groupId = topicsByGroupedId.keyAt(i);
                ArrayList<TopicsController.TopicUpdate> topicsToUpdate = topicsByGroupedId.valueAt(i);
                SQLiteCursor cursor = database.queryFinalized(String.format(Locale.US, "SELECT data FROM messages_v2 WHERE uid = %s AND group_id = %s ORDER BY date DESC", topicsToUpdate.get(0).dialogId, groupId));

                ArrayList<MessageObject> messageObjects = null;
                while (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(0);
                    TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                    if (message != null) {
                        message.readAttachPath(data, UserConfig.getInstance(currentAccount).clientUserId);
                    }
                    if (messageObjects == null) {
                        messageObjects = new ArrayList<>();
                    }
                    messageObjects.add(new MessageObject(currentAccount, message, false, false));
                }
                cursor.dispose();
                for (int k = 0; k < topicsToUpdate.size(); k++) {
                    topicsToUpdate.get(k).groupedMessages = messageObjects;
                }

            }
        } catch (Throwable e) {
            checkSQLException(e);
        }
    }

    public void loadGroupedMessagesForTopics(long dialogId, ArrayList<TLRPC.TL_forumTopic> topics) {
        if (topics == null) {
            return;
        }
        try {

            LongSparseArray<ArrayList<TLRPC.TL_forumTopic>> topicsByGroupedId = new LongSparseArray<>();

            for (int i = 0; i < topics.size(); i++) {
                if (topics.get(i).topMessage == null) {
                    continue;
                }
                long groupId = topics.get(i).topMessage.grouped_id;
                if (groupId != 0) {
                    ArrayList<TLRPC.TL_forumTopic> array = topicsByGroupedId.get(groupId);
                    if (array == null) {
                        array = new ArrayList<>();
                        topicsByGroupedId.put(groupId, array);
                    }
                    array.add(topics.get(i));
                }
            }
            for (int i = 0; i < topicsByGroupedId.size(); i++) {
                long groupId = topicsByGroupedId.keyAt(i);
                ArrayList<TLRPC.TL_forumTopic> topicsToUpdate = topicsByGroupedId.valueAt(i);
                SQLiteCursor cursor = database.queryFinalized(String.format(Locale.US, "SELECT data FROM messages_v2 WHERE uid = %s AND group_id = %s ORDER BY date DESC", dialogId, groupId));

                ArrayList<MessageObject> messageObjects = null;
                while (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(0);
                    TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                    if (message != null) {
                        message.readAttachPath(data, UserConfig.getInstance(currentAccount).clientUserId);
                    }
                    if (messageObjects == null) {
                        messageObjects = new ArrayList<>();
                    }
                    messageObjects.add(new MessageObject(currentAccount, message, false, false));
                }
                cursor.dispose();
                for (int k = 0; k < topicsToUpdate.size(); k++) {
                    topicsToUpdate.get(k).groupedMessages = messageObjects;
                }

            }
        } catch (Throwable e) {
            checkSQLException(e);
        }

    }

    public void getSavedDialogMaxMessageId(long dialog_id, IntCallback callback) {
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            int[] max = new int[1];
            try {
                final long selfId = getUserConfig().getClientUserId();
                cursor = database.queryFinalized("SELECT MAX(mid) FROM messages_topics WHERE uid = ? AND topic_id = ?", selfId, dialog_id);
                if (cursor.next()) {
                    max[0] = cursor.intValue(0);
                }
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
            AndroidUtilities.runOnUIThread(() -> callback.run(max[0]));
        });
    }

    public void deleteSavedDialog(long did) {
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            try {
                long selfId = getUserConfig().getClientUserId();
                cursor = database.queryFinalized("SELECT mid FROM messages_topics WHERE uid = ? AND topic_id = ?", selfId, did);
                ArrayList<Integer> mids = new ArrayList<>();
                while (cursor.next()) {
                    final int mid = cursor.intValue(0);
                    mids.add(mid);
                }
                cursor.dispose();
                cursor = null;
                cursor = database.queryFinalized("SELECT mid, data FROM messages_v2 WHERE uid = ?", selfId);
                while (cursor.next()) {
                    final int mid = cursor.intValue(0);
                    if (mids.contains(mid)) continue;
                    NativeByteBuffer buffer = cursor.byteBufferValue(1);
                    TLRPC.Message message = TLRPC.Message.TLdeserialize(buffer, buffer.readInt32(false), false);
                    if (MessageObject.getSavedDialogId(selfId, message) == did) {
                        mids.add(mid);
                    }
                    buffer.reuse();
                }
                cursor.dispose();
                cursor = null;
                if (!mids.isEmpty()) {
                    markMessagesAsDeletedInternal(selfId, mids, true, 0, 0);
                    updateDialogsWithDeletedMessages(selfId, -selfId, mids, null);
                    AndroidUtilities.runOnUIThread(() -> {
                        getMessagesController().markDialogMessageAsDeleted(selfId, mids);
                        getNotificationCenter().postNotificationName(NotificationCenter.messagesDeleted, mids, 0L, false);
                    });
                }
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
        });
    }

    public void removeAllTopics(long dialogId) {
        executeInStorageQueue(() -> {
            try {
                database.executeFast(String.format(Locale.US, "DELETE FROM topics WHERE did = %d", dialogId)).stepThis().dispose();
            } catch (SQLiteException e) {
                e.printStackTrace();
            }
        });
    }

    public void removeTopic(long dialogId, long topicId) {
        storageQueue.postRunnable(() -> {
            try {
                database.executeFast(String.format(Locale.US, "DELETE FROM topics WHERE did = %d AND topic_id = %d", dialogId, topicId)).stepThis().dispose();
                database.executeFast(String.format(Locale.US,
                    "DELETE FROM messages_v2 WHERE uid = %d AND mid IN (" +
                            "SELECT mid FROM messages_topics WHERE uid = %d AND topic_id = %d" +
                            ")",
                    dialogId, dialogId, topicId)).stepThis().dispose();
                database.executeFast(String.format(Locale.US, "DELETE FROM messages_topics WHERE uid = %d AND topic_id = %d", dialogId, topicId)).stepThis().dispose();
            } catch (SQLiteException e) {
                e.printStackTrace();
            }
        });
    }

    public void removeTopics(long dialogId, ArrayList<Long> topicIds) {
        storageQueue.postRunnable(() -> {
            try {
                String topics = TextUtils.join(", ", topicIds);
                database.executeFast(String.format(Locale.US, "DELETE FROM topics WHERE did = %d AND topic_id IN (%s)", dialogId, topics)).stepThis().dispose();
                try {
                    database.executeFast(String.format(Locale.US,
                        "DELETE FROM messages_v2 WHERE uid = %d AND mid IN (" +
                                "SELECT mid FROM messages_topics WHERE uid = %d AND topic_id IN (%s)" +
                                ")",
                        dialogId, dialogId, topics)).stepThis().dispose();
                } catch (SQLiteException e) {
                    e.printStackTrace();
                }
                database.executeFast(String.format(Locale.US, "DELETE FROM messages_topics WHERE uid = %d AND topic_id IN (%s)", dialogId, topics)).stepThis().dispose();
            } catch (SQLiteException e) {
                e.printStackTrace();
            }
        });
    }

    public void updateTopicsWithReadMessages(HashMap<TopicKey, Integer> topicsReadOutbox) {
        storageQueue.postRunnable(() -> {
            for (TopicKey topicKey : topicsReadOutbox.keySet()) {
                int value = topicsReadOutbox.get(topicKey);
                try {
                    database.executeFast(String.format(Locale.US, "UPDATE topics SET read_outbox = max((SELECT read_outbox FROM topics WHERE did = %d AND topic_id = %d), %d) WHERE did = %d AND topic_id = %d", topicKey.dialogId, topicKey.topicId, value, topicKey.dialogId, topicKey.topicId)).stepThis().dispose();
                } catch (SQLiteException e) {
                   checkSQLException(e);
                }
            }
        });
    }

    public void setDialogTtl(long did, int ttl) {
        storageQueue.postRunnable(() -> {
            try {
                database.executeFast(String.format(Locale.US, "UPDATE dialogs SET ttl_period = %d WHERE did = %d", ttl, did)).stepThis().dispose();
            } catch (SQLiteException e) {
                checkSQLException(e);
            }
        });
    }

    public ArrayList<File> getDatabaseFiles() {
        ArrayList<File> files = new ArrayList<>();
        files.add(cacheFile);
        files.add(walCacheFile);
        files.add(shmCacheFile);
        return files;
    }

    public void reset() {
        clearDatabaseValues();

        AndroidUtilities.runOnUIThread(() -> {
            for (int a = 0; a < 2; a++) {
                getUserConfig().setDialogsLoadOffset(a, 0, 0, 0, 0, 0, 0);
                getUserConfig().setTotalDialogsCount(a, 0);
            }
            getUserConfig().clearFilters();
            getUserConfig().clearPinnedDialogsLoaded();

            getNotificationCenter().postNotificationName(NotificationCenter.didClearDatabase);
            getMediaDataController().loadAttachMenuBots(false, true);
            getNotificationCenter().postNotificationName(NotificationCenter.onDatabaseReset);
            
            getMessagesController().getStoriesController().cleanup();
        });
    }

    public void fullReset() {
        storageQueue.postRunnable(() -> {
            cleanupInternal(true);
            clearLoadingDialogsOffsets();
            openDatabase(1);
            AndroidUtilities.runOnUIThread(() -> {
                getNotificationCenter().postNotificationName(NotificationCenter.onDatabaseReset);
                getNotificationCenter().postNotificationName(NotificationCenter.didClearDatabase);
                getMessagesController().getSavedMessagesController().cleanup();
            });
        });

    }

    private static class ReadDialog {
        public int lastMid;
        public int date;
        public int unreadCount;
    }

    public void readAllDialogs(int folderId) {
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            try {
                ArrayList<Long> usersToLoad = new ArrayList<>();
                ArrayList<Long> chatsToLoad = new ArrayList<>();
                ArrayList<Integer> encryptedChatIds = new ArrayList<>();

                LongSparseArray<ReadDialog> dialogs = new LongSparseArray<>();
                if (folderId >= 0) {
                    cursor = database.queryFinalized(String.format(Locale.US, "SELECT did, last_mid, unread_count, date FROM dialogs WHERE unread_count > 0 AND folder_id = %1$d", folderId));
                } else {
                    cursor = database.queryFinalized("SELECT did, last_mid, unread_count, date FROM dialogs WHERE unread_count > 0");
                }
                while (cursor.next()) {
                    long did = cursor.longValue(0);
                    if (DialogObject.isFolderDialogId(did)) {
                        continue;
                    }
                    ReadDialog dialog = new ReadDialog();
                    dialog.lastMid = cursor.intValue(1);
                    dialog.unreadCount = cursor.intValue(2);
                    dialog.date = cursor.intValue(3);

                    dialogs.put(did, dialog);
                    if (!DialogObject.isEncryptedDialog(did)) {
                        if (DialogObject.isChatDialog(did)) {
                            if (!chatsToLoad.contains(-did)) {
                                chatsToLoad.add(-did);
                            }
                        } else {
                            if (!usersToLoad.contains(did)) {
                                usersToLoad.add(did);
                            }
                        }
                    } else {
                        int encryptedChatId = DialogObject.getEncryptedChatId(did);
                        if (!encryptedChatIds.contains(encryptedChatId)) {
                            encryptedChatIds.add(encryptedChatId);
                        }
                    }
                }
                cursor.dispose();
                cursor = null;

                ArrayList<TLRPC.User> users = new ArrayList<>();
                ArrayList<TLRPC.Chat> chats = new ArrayList<>();
                ArrayList<TLRPC.EncryptedChat> encryptedChats = new ArrayList<>();
                if (!encryptedChatIds.isEmpty()) {
                    getEncryptedChatsInternal(TextUtils.join(",", encryptedChatIds), encryptedChats, usersToLoad);
                }
                if (!usersToLoad.isEmpty()) {
                    getUsersInternal(usersToLoad, users);
                }
                if (!chatsToLoad.isEmpty()) {
                    getChatsInternal(TextUtils.join(",", chatsToLoad), chats);
                }
                AndroidUtilities.runOnUIThread(() -> {
                    getMessagesController().putUsers(users, true);
                    getMessagesController().putChats(chats, true);
                    getMessagesController().putEncryptedChats(encryptedChats, true);
                    for (int a = 0; a < dialogs.size(); a++) {
                        long did = dialogs.keyAt(a);
                        ReadDialog dialog = dialogs.valueAt(a);
                        if (getMessagesController().isForum(did) || isForum(did, FORUM_TYPE_BOT) || getMessagesController().isMonoForumWithManageRights(did)) {
                            getMessagesController().markAllTopicsAsRead(did);
                        }
                        getMessagesController().markDialogAsRead(did, dialog.lastMid, dialog.lastMid, dialog.date, false, 0, dialog.unreadCount, true, 0);
                    }
                });
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
        });
    }

    private TLRPC.messages_Dialogs loadDialogsByIds(String ids, ArrayList<Long> usersToLoad, ArrayList<Long> chatsToLoad, ArrayList<Integer> encryptedToLoad) throws Exception {
        TLRPC.messages_Dialogs dialogs = new TLRPC.TL_messages_dialogs();
        LongSparseArray<TLRPC.Message> replyMessageOwners = new LongSparseArray<>();
        LongSparseArray<Long> groupsToLoad = new LongSparseArray<>();
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized(String.format(Locale.US, "SELECT d.did, d.last_mid, d.unread_count, d.date, m.data, m.read_state, m.mid, m.send_state, s.flags, m.date, d.pts, d.inbox_max, d.outbox_max, m.replydata, d.pinned, d.unread_count_i, d.flags, d.folder_id, d.data, d.unread_reactions, d.last_mid_group, d.ttl_period, d.unread_poll_votes FROM dialogs as d LEFT JOIN messages_v2 as m ON d.last_mid = m.mid AND d.did = m.uid LEFT JOIN dialog_settings as s ON d.did = s.did WHERE d.did IN (%s) ORDER BY d.pinned DESC, d.date DESC", ids));
            while (cursor.next()) {
                long dialogId = cursor.longValue(0);
                TLRPC.Dialog dialog = new TLRPC.TL_dialog();
                dialog.id = dialogId;
                dialog.top_message = cursor.intValue(1);
                dialog.unread_count = cursor.intValue(2);
                dialog.last_message_date = cursor.intValue(3);
                dialog.pts = cursor.intValue(10);
                dialog.flags = dialog.pts == 0 || DialogObject.isUserDialog(dialog.id) ? 0 : 1;
                dialog.read_inbox_max_id = cursor.intValue(11);
                dialog.read_outbox_max_id = cursor.intValue(12);
                dialog.pinnedNum = cursor.intValue(14);
                dialog.pinned = dialog.pinnedNum != 0;
                dialog.unread_mentions_count = cursor.intValue(15);
                int dialog_flags = cursor.intValue(16);
                dialog.unread_mark = (dialog_flags & 1) != 0;
                dialog.view_forum_as_messages = (dialog_flags & 64) != 0;
                long flags = cursor.longValue(8);
                int low_flags = (int) flags;
                dialog.notify_settings = new TLRPC.TL_peerNotifySettings();
                if ((low_flags & 1) != 0) {
                    dialog.notify_settings.mute_until = (int) (flags >> 32);
                    if (dialog.notify_settings.mute_until == 0) {
                        dialog.notify_settings.mute_until = Integer.MAX_VALUE;
                    }
                }
                dialog.folder_id = cursor.intValue(17);
                dialog.unread_reactions_count = cursor.intValue(19);
                long groupMessagesId = cursor.longValue(20);
                if (groupMessagesId != 0) {
                    groupsToLoad.append(dialogId, groupMessagesId);
                }
                dialog.ttl_period = cursor.intValue(21);
                dialog.unread_poll_votes_count = cursor.intValue(22);
                dialogs.dialogs.add(dialog);

                NativeByteBuffer data = cursor.byteBufferValue(4);
                if (data != null) {
                    TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                    if (message != null) {
                        message.readAttachPath(data, getUserConfig().clientUserId);
                        data.reuse();
                        MessageObject.setUnreadFlags(message, cursor.intValue(5));
                        message.id = cursor.intValue(6);
                        int date = cursor.intValue(9);
                        if (date != 0) {
                            dialog.last_message_date = date;
                        }
                        message.send_state = cursor.intValue(7);
                        message.dialog_id = dialog.id;
                        dialogs.messages.add(message);

                        addUsersAndChatsFromMessage(message, usersToLoad, chatsToLoad, null);

                        try {
                            if (message.reply_to != null && message.reply_to.reply_to_msg_id != 0 && isMessageActionTypeWithReply(message.action)) {
                                if (!cursor.isNull(13)) {
                                    NativeByteBuffer data2 = cursor.byteBufferValue(13);
                                    if (data2 != null) {
                                        message.replyMessage = TLRPC.Message.TLdeserialize(data2, data2.readInt32(false), false);
                                        message.replyMessage.readAttachPath(data2, getUserConfig().clientUserId);
                                        data2.reuse();
                                        if (message.replyMessage != null) {
                                            addUsersAndChatsFromMessage(message.replyMessage, usersToLoad, chatsToLoad, null);
                                        }
                                    }
                                }
                                if (message.replyMessage == null) {
                                    replyMessageOwners.put(dialog.id, message);
                                }
                            }
                        } catch (Exception e) {
                            checkSQLException(e);
                        }
                    } else {
                        data.reuse();
                    }
                }
                if (!DialogObject.isEncryptedDialog(dialogId)) {
                    if (dialog.read_inbox_max_id > dialog.top_message) {
                        dialog.read_inbox_max_id = 0;
                    }
                }
                if (DialogObject.isEncryptedDialog(dialogId)) {
                    int encryptedChatId = DialogObject.getEncryptedChatId(dialogId);
                    if (!encryptedToLoad.contains(encryptedChatId)) {
                        encryptedToLoad.add(encryptedChatId);
                    }
                } else if (DialogObject.isUserDialog(dialogId)) {
                    if (!usersToLoad.contains(dialogId)) {
                        usersToLoad.add(dialogId);
                    }
                } else {
                    if (!chatsToLoad.contains(-dialogId)) {
                        chatsToLoad.add(-dialogId);
                    }
                }
            }
            cursor.dispose();
            cursor = null;

            if (!groupsToLoad.isEmpty()) {
                StringBuilder whereClause = new StringBuilder();
                for (int i = 0; i < groupsToLoad.size(); ++i) {
                    whereClause.append("uid = ").append(groupsToLoad.keyAt(i)).append(" AND group_id = ").append(groupsToLoad.valueAt(i));
                    if (i + 1 < groupsToLoad.size()) {
                        whereClause.append(" OR ");
                    }
                }
                cursor = database.queryFinalized(String.format(Locale.US, "SELECT uid, data, read_state, mid, send_state, date, replydata FROM messages_v2 WHERE %s ORDER BY date DESC", whereClause));
                int count = 0;
                while (cursor.next()) {
                    count++;
                    long dialogId = cursor.longValue(0);
                    TLRPC.Dialog dialog = null;
                    for (int i = 0; i < dialogs.dialogs.size(); ++i) {
                        TLRPC.Dialog d = dialogs.dialogs.get(i);
                        if (d != null && d.id == dialogId) {
                            dialog = d;
                            break;
                        }
                    }
                    if (dialog == null) {
                        continue;
                    }
                    NativeByteBuffer data = cursor.byteBufferValue(1);
                    if (data != null) {
                        TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                        if (message != null) {
                            message.readAttachPath(data, getUserConfig().clientUserId);
                            data.reuse();
                            MessageObject.setUnreadFlags(message, cursor.intValue(2));
                            message.id = cursor.intValue(3);
                            int date = cursor.intValue(5);
                            if (date != 0) {
                                dialog.last_message_date = date;
                            }
                            message.send_state = cursor.intValue(4);
                            message.dialog_id = dialog.id;
                            dialogs.messages.add(message);

                            addUsersAndChatsFromMessage(message, usersToLoad, chatsToLoad, null);

                            try {
                                if (message.reply_to != null && message.reply_to.reply_to_msg_id != 0 && isMessageActionTypeWithReply(message.action)) {
                                    if (!cursor.isNull(6)) {
                                        NativeByteBuffer data2 = cursor.byteBufferValue(6);
                                        if (data2 != null) {
                                            message.replyMessage = TLRPC.Message.TLdeserialize(data2, data2.readInt32(false), false);
                                            message.replyMessage.readAttachPath(data2, getUserConfig().clientUserId);
                                            data2.reuse();
                                            if (message.replyMessage != null) {
                                                addUsersAndChatsFromMessage(message.replyMessage, usersToLoad, chatsToLoad, null);
                                            }
                                        }
                                    }
                                    if (message.replyMessage == null) {
                                        replyMessageOwners.put(dialog.id, message);
                                    }
                                }
                            } catch (Exception e) {
                                checkSQLException(e);
                            }
                        } else {
                            data.reuse();
                        }
                    }
                }
                cursor.dispose();
            }

            if (!replyMessageOwners.isEmpty()) {
                for (int a = 0, N = replyMessageOwners.size(); a < N; a++) {
                    long dialogId = replyMessageOwners.keyAt(a);
                    TLRPC.Message ownerMessage = replyMessageOwners.valueAt(a);
                    SQLiteCursor replyCursor = database.queryFinalized(String.format(Locale.US, "SELECT data, mid, date, uid FROM messages_v2 WHERE mid = %d and uid = %d", ownerMessage.id, dialogId));
                    while (replyCursor.next()) {
                        NativeByteBuffer data = replyCursor.byteBufferValue(0);
                        if (data != null) {
                            TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                            message.readAttachPath(data, getUserConfig().clientUserId);
                            data.reuse();
                            message.id = replyCursor.intValue(1);
                            message.date = replyCursor.intValue(2);
                            message.dialog_id = replyCursor.longValue(3);

                            addUsersAndChatsFromMessage(message, usersToLoad, chatsToLoad, null);

                            ownerMessage.replyMessage = message;
                            message.dialog_id = ownerMessage.dialog_id;
                        }
                    }
                    replyCursor.dispose();
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
        return dialogs;
    }

    private void loadDialogFilters() {
        storageQueue.postRunnable(() -> {
            SQLiteCursor filtersCursor = null;
            SQLitePreparedStatement state = null;
            try {
                ArrayList<Long> usersToLoad = new ArrayList<>();
                ArrayList<Long> chatsToLoad = new ArrayList<>();
                ArrayList<Integer> encryptedToLoad = new ArrayList<>();
                ArrayList<Long> dialogsToLoad = new ArrayList<>();
                SparseArray<MessagesController.DialogFilter> filtersById = new SparseArray<>();

                usersToLoad.add(getUserConfig().getClientUserId());

                filtersCursor = database.queryFinalized("SELECT id, ord, unread_count, flags, title, color, entities, noanimate FROM dialog_filter WHERE 1");

                boolean updateCounters = false;
                boolean hasDefaultFilter = false;
                while (filtersCursor.next()) {
                    MessagesController.DialogFilter filter = new MessagesController.DialogFilter();
                    filter.id = filtersCursor.intValue(0);
                    filter.order = filtersCursor.intValue(1);
                    filter.pendingUnreadCount = filter.unreadCount = -1;//filtersCursor.intValue(2);
                    filter.flags = filtersCursor.intValue(3);
                    filter.name = filtersCursor.stringValue(4);
                    filter.color = filtersCursor.intValue(5);
                    filter.entities = new ArrayList<>();
                    NativeByteBuffer buff = filtersCursor.byteBufferValue(6);
                    if (buff != null) {
                        filter.entities = Vector.deserialize(buff, TLRPC.MessageEntity::TLdeserialize, false);
                        buff.reuse();
                    }
                    filter.title_noanimate = filtersCursor.intValue(7) == 1;
                    dialogFilters.add(filter);
                    dialogFiltersMap.put(filter.id, filter);
                    filtersById.put(filter.id, filter);
                    if (filter.pendingUnreadCount < 0) {
                        updateCounters = true;
                    }

                    for (int a = 0; a < 2; a++) {
                        SQLiteCursor cursor2;
                        if (a == 0) {
                            cursor2 = database.queryFinalized("SELECT peer, pin FROM dialog_filter_pin_v2 WHERE id = " + filter.id);
                        } else {
                            cursor2 = database.queryFinalized("SELECT peer FROM dialog_filter_ep WHERE id = " + filter.id);
                        }
                        while (cursor2.next()) {
                            long did = cursor2.longValue(0);
                            if (a == 0) {
                                if (!DialogObject.isEncryptedDialog(did)) {
                                    filter.alwaysShow.add(did);
                                }
                                int pin = cursor2.intValue(1);
                                if (pin != Integer.MIN_VALUE) {
                                    filter.pinnedDialogs.put(did, pin);
                                    if (!dialogsToLoad.contains(did)) {
                                        dialogsToLoad.add(did);
                                    }
                                }
                            } else {
                                if (!DialogObject.isEncryptedDialog(did)) {
                                    filter.neverShow.add(did);
                                }
                            }
                            if (DialogObject.isChatDialog(did)) {
                                if (!chatsToLoad.contains(-did)) {
                                    chatsToLoad.add(-did);
                                }
                            } else if (DialogObject.isUserDialog(did)) {
                                if (!usersToLoad.contains(did)) {
                                    usersToLoad.add(did);
                                }
                            } else {
                                int encryptedChatId = DialogObject.getEncryptedChatId(did);
                                if (!encryptedToLoad.contains(encryptedChatId)) {
                                    encryptedToLoad.add(encryptedChatId);
                                }
                            }
                        }
                        cursor2.dispose();
                    }
                    if (filter.id == 0) {
                        hasDefaultFilter = true;
                    }
                }
                filtersCursor.dispose();
                filtersCursor = null;

                if (!hasDefaultFilter) {
                    MessagesController.DialogFilter filter = new MessagesController.DialogFilter();
                    filter.id = 0;
                    filter.order = 0;
                    filter.color = -1;
                    filter.name = "ALL_CHATS";
                    for (int i = 0; i < dialogFilters.size(); i++) {
                        dialogFilters.get(i).order++;
                    }
                    dialogFilters.add(filter);
                    dialogFiltersMap.put(filter.id, filter);
                    filtersById.put(filter.id, filter);

                    state = database.executeFast("REPLACE INTO dialog_filter VALUES(?, ?, ?, ?, ?, ?, ?, ?)");
                    state.bindInteger(1, filter.id);
                    state.bindInteger(2, filter.order);
                    state.bindInteger(3, filter.unreadCount);
                    state.bindInteger(4, filter.flags);
                    state.bindString(5, filter.name);
                    state.bindInteger(6, filter.color);
                    final Vector<TLRPC.MessageEntity> entitiesVector = new Vector<>(TLRPC.MessageEntity::TLdeserialize);
                    final NativeByteBuffer entitiesBuffer = new NativeByteBuffer(entitiesVector.getObjectSize());
                    entitiesVector.serializeToStream(entitiesBuffer);
                    state.bindByteBuffer(7, entitiesBuffer);
                    state.bindInteger(8, filter.title_noanimate ? 1 : 0);
                    state.stepThis().dispose();
                    state = null;
                    entitiesBuffer.reuse();
                }

                Collections.sort(dialogFilters, (o1, o2) -> {
                    if (o1.order > o2.order) {
                        return 1;
                    } else if (o1.order < o2.order) {
                        return -1;
                    }
                    return 0;
                });

                if (updateCounters) {
                    calcUnreadCounters(true);
                }

                TLRPC.messages_Dialogs dialogs;
                if (!dialogsToLoad.isEmpty()) {
                    dialogs = loadDialogsByIds(TextUtils.join(",", dialogsToLoad), usersToLoad, chatsToLoad, encryptedToLoad);
                } else {
                    dialogs = new TLRPC.TL_messages_dialogs();
                }

                ArrayList<TLRPC.User> users = new ArrayList<>();
                ArrayList<TLRPC.Chat> chats = new ArrayList<>();
                ArrayList<TLRPC.EncryptedChat> encryptedChats = new ArrayList<>();

                if (!encryptedToLoad.isEmpty()) {
                    getEncryptedChatsInternal(TextUtils.join(",", encryptedToLoad), encryptedChats, usersToLoad);
                }
                if (!usersToLoad.isEmpty()) {
                    getUsersInternal(usersToLoad, users);
                }
                if (!chatsToLoad.isEmpty()) {
                    getChatsInternal(TextUtils.join(",", chatsToLoad), chats);
                }

                getMessagesController().processLoadedDialogFilters(new ArrayList<>(dialogFilters), dialogs, null, users, chats, encryptedChats, 0, null);
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (filtersCursor != null) {
                    filtersCursor.dispose();
                }
                if (state != null) {
                    state.dispose();
                }
            }
        });
    }

    private int[][] contacts = new int[][]{new int[2], new int[2]};
    private int[][] nonContacts = new int[][]{new int[2], new int[2]};
    private int[][] bots = new int[][]{new int[2], new int[2]};
    private int[][] channels = new int[][]{new int[2], new int[2]};
    private int[][] groups = new int[][]{new int[2], new int[2]};
    private int[][] communities = new int[][]{new int[2], new int[2]};
    private int[] mentionChannels = new int[2];
    private int[] mentionGroups = new int[2];
    private LongSparseArray<Integer> dialogsWithMentions = new LongSparseArray<>();
    private LongSparseArray<Integer> dialogsWithUnread = new LongSparseArray<>();

    private void calcUnreadCounters(boolean apply) {
        SQLiteCursor cursor = null;
        try {
            for (int a = 0; a < 2; a++) {
                for (int b = 0; b < 2; b++) {
                    contacts[a][b] = nonContacts[a][b] = bots[a][b] = channels[a][b] = groups[a][b] = communities[a][b] = 0;
                }
            }
            dialogsWithMentions.clear();
            dialogsWithUnread.clear();

            ArrayList<TLRPC.User> users = new ArrayList<>();
            ArrayList<TLRPC.User> encUsers = new ArrayList<>();
            ArrayList<TLRPC.Chat> chats = new ArrayList<>();
            ArrayList<Long> usersToLoad = new ArrayList<>();
            HashSet<Long> chatsToLoad = new HashSet<>();
            ArrayList<Integer> encryptedToLoad = new ArrayList<>();
            LongSparseIntArray dialogsByFolders = new LongSparseIntArray();

            LongSparseIntArray forumUnreadCount = new LongSparseIntArray();
            cursor = database.queryFinalized("SELECT DISTINCT did FROM topics WHERE unread_count > 0 OR unread_mentions > 0");
            while (cursor.next()) {
                long dialogId = cursor.longValue(0);
                if (isForum(dialogId, FORUM_TYPE_CHAT | FORUM_TYPE_BOT | FORUM_TYPE_DIRECT)) {
                    forumUnreadCount.put(dialogId, 1);
                }
            }
            cursor.dispose();
/*
            LongSparseIntArray monoForumUnreadCount = new LongSparseIntArray();
            cursor = database.queryFinalized("SELECT DISTINCT forumChatId FROM saved_dialogs WHERE unread_count > 0");
            while (cursor.next()) {
                long dialogId = cursor.longValue(0);
                if (isMonoForum(dialogId)) {
                    monoForumUnreadCount.put(dialogId, 1);
                }
            }
            cursor.dispose();
*/
            cursor = database.queryFinalized("SELECT did, folder_id, unread_count, unread_count_i FROM dialogs WHERE unread_count > 0 OR flags > 0 UNION ALL " +
                    "SELECT did, folder_id, unread_count, unread_count_i FROM dialogs WHERE unread_count_i > 0");
            while (cursor.next()) {
                int folderId = cursor.intValue(1);
                long did = cursor.longValue(0);
                int unread;
                int mentions = 0;
                if (isForum(did, FORUM_TYPE_CHAT | FORUM_TYPE_BOT | FORUM_TYPE_DIRECT)) {
                    unread = forumUnreadCount.get(did, 0);
                    if (unread == 0) {
                        continue;
                    }
                } else {
                    unread = cursor.intValue(2);
                    mentions = cursor.intValue(3);
                }
                if (unread > 0) {
                    dialogsWithUnread.put(did, unread);
                }
                if (mentions > 0) {
                    dialogsWithMentions.put(did, mentions);
                }
                /*if (BuildVars.DEBUG_VERSION) {
                    FileLog.d("unread chat " + did + " counters = " + unread + " and " + mentions);
                }*/
                dialogsByFolders.put(did, folderId);
                if (DialogObject.isEncryptedDialog(did)) {
                    int encryptedChatId = DialogObject.getEncryptedChatId(did);
                    if (!encryptedToLoad.contains(encryptedChatId)) {
                        encryptedToLoad.add(encryptedChatId);
                    }
                } else if (DialogObject.isUserDialog(did)) {
                    if (!usersToLoad.contains(did)) {
                        usersToLoad.add(did);
                    }
                } else {
                    chatsToLoad.add(-did);
                }
            }
            cursor.dispose();
            cursor = null;
            LongSparseArray<TLRPC.User> usersDict = new LongSparseArray<>();
            LongSparseArray<TLRPC.Chat> chatsDict = new LongSparseArray<>();
            LongSparseArray<TLRPC.User> encUsersDict = new LongSparseArray<>();
            LongSparseIntArray encryptedChatsByUsersCount = new LongSparseIntArray();
            LongSparseArray<Boolean> mutedDialogs = new LongSparseArray<>();
            LongSparseArray<Boolean> archivedDialogs = new LongSparseArray<>();
            if (!usersToLoad.isEmpty()) {
                getUsersInternal(usersToLoad, users, true);
                for (int a = 0, N = users.size(); a < N; a++) {
                    TLRPC.User user = users.get(a);
                    boolean muted = getMessagesController().isDialogMuted(user.id, 0);
                    int idx1 = dialogsByFolders.get(user.id);
                    int idx2 = muted ? 1 : 0;
                    if (muted) {
                        mutedDialogs.put(user.id, true);
                    }
                    if (idx1 == 1) {
                        archivedDialogs.put(user.id, true);
                    }
                    if (isUserCollapsedInCommunity(chatsDict, user)) {
                        communities[idx1][idx2]++;
                    } else if (user.bot) {
                        bots[idx1][idx2]++;
                    } else if (user.self || user.contact) {
                        contacts[idx1][idx2]++;
                    } else {
                        nonContacts[idx1][idx2]++;
                    }
                    usersDict.put(user.id, user);
                }
            }
            if (!encryptedToLoad.isEmpty()) {
                ArrayList<Long> encUsersToLoad = new ArrayList<>();
                ArrayList<TLRPC.EncryptedChat> encryptedChats = new ArrayList<>();
                getEncryptedChatsInternal(TextUtils.join(",", encryptedToLoad), encryptedChats, encUsersToLoad);
                if (!encUsersToLoad.isEmpty()) {
                    getUsersInternal(encUsersToLoad, encUsers, true);
                    for (int a = 0, N = encUsers.size(); a < N; a++) {
                        TLRPC.User user = encUsers.get(a);
                        encUsersDict.put(user.id, user);
                    }
                    for (int a = 0, N = encryptedChats.size(); a < N; a++) {
                        TLRPC.EncryptedChat encryptedChat = encryptedChats.get(a);
                        TLRPC.User user = encUsersDict.get(encryptedChat.user_id);
                        if (user == null) {
                            continue;
                        }
                        long did = DialogObject.makeEncryptedDialogId(encryptedChat.id);
                        boolean muted = getMessagesController().isDialogMuted(did, 0);
                        int idx1 = dialogsByFolders.get(did);
                        int idx2 = muted ? 1 : 0;
                        if (muted) {
                            mutedDialogs.put(user.id, true);
                        }
                        if (idx1 == 1) {
                            archivedDialogs.put(user.id, true);
                        }
                        if (user.self || user.contact) {
                            contacts[idx1][idx2]++;
                        } else {
                            nonContacts[idx1][idx2]++;
                        }
                        int count = encryptedChatsByUsersCount.get(user.id, 0);
                        encryptedChatsByUsersCount.put(user.id, count + 1);
                    }
                }
            }
            if (!chatsToLoad.isEmpty()) {
                getChatsInternal(TextUtils.join(",", chatsToLoad), chats);
                for (int a = 0, N = chats.size(); a < N; a++) {
                    TLRPC.Chat chat = chats.get(a);
                    if (!chatsToLoad.contains(chat.id)) {
                        continue;
                    }
                    if (chat.migrated_to instanceof TLRPC.TL_inputChannel || ChatObject.isNotInChat(chat) || ChatObject.isCommunity(chat)) {
                        dialogsWithUnread.remove(-chat.id);
                        dialogsWithMentions.remove(-chat.id);
                        continue;
                    }
                    boolean muted = getMessagesController().isDialogMuted(-chat.id, 0, chat);
                    int idx1 = dialogsByFolders.get(-chat.id);
                    int idx2 = muted && dialogsWithMentions.indexOfKey(-chat.id) < 0 ? 1 : 0;
                    if (muted) {
                        mutedDialogs.put(-chat.id, true);
                    }
                    if (idx1 == 1) {
                        archivedDialogs.put(-chat.id, true);
                    }

                    if (ChatObject.isCommunity(chat)) {

                    } else if (isChatCollapsedInCommunity(chatsDict, chat)) {
                        communities[idx1][idx2]++;
                    } else if (ChatObject.isChannel(chat) && !chat.megagroup) {
                        channels[idx1][idx2]++;
                    } else {
                        groups[idx1][idx2]++;
                    }
                    chatsDict.put(chat.id, chat);
                }
            }
            /*if (BuildVars.DEBUG_VERSION) {
                for (int b = 0; b < 2; b++) {
                    FileLog.d("contacts = " + contacts[b][0] + ", " + contacts[b][1]);
                    FileLog.d("nonContacts = " + nonContacts[b][0] + ", " + nonContacts[b][1]);
                    FileLog.d("groups = " + groups[b][0] + ", " + groups[b][1]);
                    FileLog.d("channels = " + channels[b][0] + ", " + channels[b][1]);
                    FileLog.d("bots = " + bots[b][0] + ", " + bots[b][1]);
                }
            }*/
            for (int a = 0, N = dialogFilters.size(); a < N + 2; a++) {
                final boolean isFilter = a < N;
                final boolean isMain = a == N;
                final boolean isArchive = a == N + 1;

                MessagesController.DialogFilter filter;
                int flags;
                if (a < N) {
                    filter = dialogFilters.get(a);
                    if (filter.pendingUnreadCount >= 0) {
                        continue;
                    }
                    flags = filter.flags;
                } else {
                    filter = null;
                    flags = MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS;
                    if (a == N) {
                        if (!getNotificationsController().showBadgeMuted) {
                            flags |= MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED;
                        }
                        flags |= MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED;
                    } else {
                        flags |= MessagesController.DIALOG_FILTER_FLAG_ONLY_ARCHIVED;
                    }
                }
                int unreadCount = 0;
                if ((flags & MessagesController.DIALOG_FILTER_FLAG_CONTACTS) != 0) {
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_ONLY_ARCHIVED) == 0) {
                        unreadCount += contacts[0][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount += contacts[0][1];
                        }
                    }
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) == 0) {
                        unreadCount += contacts[1][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount += contacts[1][1];
                        }
                    }
                }
                if ((flags & MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS) != 0) {
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_ONLY_ARCHIVED) == 0) {
                        unreadCount += nonContacts[0][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount += nonContacts[0][1];
                        }
                    }
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) == 0) {
                        unreadCount += nonContacts[1][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount += nonContacts[1][1];
                        }
                    }
                }
                if ((flags & MessagesController.DIALOG_FILTER_FLAG_GROUPS) != 0) {
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_ONLY_ARCHIVED) == 0) {
                        unreadCount += groups[0][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount += groups[0][1];
                        }
                    }
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) == 0) {
                        unreadCount += groups[1][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount += groups[1][1];
                        }
                    }
                }
                if ((flags & MessagesController.DIALOG_FILTER_FLAG_CHANNELS) != 0) {
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_ONLY_ARCHIVED) == 0) {
                        unreadCount += channels[0][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount += channels[0][1];
                        }
                    }
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) == 0) {
                        unreadCount += channels[1][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount += channels[1][1];
                        }
                    }
                }
                if ((flags & MessagesController.DIALOG_FILTER_FLAG_BOTS) != 0) {
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_ONLY_ARCHIVED) == 0) {
                        unreadCount += bots[0][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount += bots[0][1];
                        }
                    }
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) == 0) {
                        unreadCount += bots[1][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount += bots[1][1];
                        }
                    }
                }

                if (!isArchive && (flags & MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS) == MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS) {
                    unreadCount += communities[0][0];
                    unreadCount += communities[1][0];
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                        unreadCount += communities[0][1];
                        unreadCount += communities[1][1];
                    }
                }

                if (filter != null) {
                    for (int b = 0, N2 = filter.alwaysShow.size(); b < N2; b++) {
                        long did = filter.alwaysShow.get(b);
                        if (DialogObject.isUserDialog(did)) {
                            for (int i = 0; i < 2; i++) {
                                LongSparseArray<TLRPC.User> dict = i == 0 ? usersDict : encUsersDict;
                                TLRPC.User user = dict.get(did);
                                if (user != null) {
                                    int count;
                                    if (i == 0) {
                                        count = 1;
                                    } else {
                                        count = encryptedChatsByUsersCount.get(did, 0);
                                        if (count == 0) {
                                            continue;
                                        }
                                    }
                                    int flag;
                                    if (user.bot) {
                                        flag = MessagesController.DIALOG_FILTER_FLAG_BOTS;
                                    } else if (user.self || user.contact) {
                                        flag = MessagesController.DIALOG_FILTER_FLAG_CONTACTS;
                                    } else {
                                        flag = MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS;
                                    }
                                    if ((flags & flag) == 0) {
                                        unreadCount += count;
                                    } else if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) != 0 && mutedDialogs.indexOfKey(user.id) >= 0) {
                                        unreadCount += count;
                                    } else if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) != 0 && archivedDialogs.indexOfKey(user.id) >= 0) {
                                        unreadCount += count;
                                    }
                                }
                            }
                        } else {
                            TLRPC.Chat chat = chatsDict.get(-did);
                            if (chat != null) {
                                int flag;
                                if (ChatObject.isChannel(chat) && !chat.megagroup) {
                                    flag = MessagesController.DIALOG_FILTER_FLAG_CHANNELS;
                                } else {
                                    flag = MessagesController.DIALOG_FILTER_FLAG_GROUPS;
                                }
                                if ((flags & flag) == 0) {
                                    unreadCount++;
                                } else if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) != 0 && mutedDialogs.indexOfKey(-chat.id) >= 0 && dialogsWithMentions.indexOfKey(-chat.id) < 0) {
                                    unreadCount++;
                                } else if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) != 0 && archivedDialogs.indexOfKey(-chat.id) >= 0) {
                                    unreadCount++;
                                }
                            }
                        }
                    }
                    for (int b = 0, N2 = filter.neverShow.size(); b < N2; b++) {
                        long did = filter.neverShow.get(b);
                        if (DialogObject.isUserDialog(did)) {
                            for (int i = 0; i < 2; i++) {
                                LongSparseArray<TLRPC.User> dict = i == 0 ? usersDict : encUsersDict;
                                TLRPC.User user = dict.get(did);
                                if (user != null) {
                                    int count;
                                    if (i == 0) {
                                        count = 1;
                                    } else {
                                        count = encryptedChatsByUsersCount.get(did, 0);
                                        if (count == 0) {
                                            continue;
                                        }
                                    }
                                    int flag;
                                    if (user.bot) {
                                        flag = MessagesController.DIALOG_FILTER_FLAG_BOTS;
                                    } else if (user.self || user.contact) {
                                        flag = MessagesController.DIALOG_FILTER_FLAG_CONTACTS;
                                    } else {
                                        flag = MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS;
                                    }
                                    if ((flags & flag) != 0) {
                                        if (((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) == 0 || archivedDialogs.indexOfKey(user.id) < 0) &&
                                                ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0 || mutedDialogs.indexOfKey(user.id) < 0)) {
                                            unreadCount -= count;
                                        }
                                    }
                                }
                            }
                        } else {
                            TLRPC.Chat chat = chatsDict.get(-did);
                            if (chat != null) {
                                int flag;
                                if (ChatObject.isChannel(chat) && !chat.megagroup) {
                                    flag = MessagesController.DIALOG_FILTER_FLAG_CHANNELS;
                                } else {
                                    flag = MessagesController.DIALOG_FILTER_FLAG_GROUPS;
                                }
                                if ((flags & flag) != 0) {
                                    if (((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) == 0 || archivedDialogs.indexOfKey(-chat.id) < 0) &&
                                            ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0 || mutedDialogs.indexOfKey(-chat.id) < 0 || dialogsWithMentions.indexOfKey(-chat.id) >= 0)) {
                                        unreadCount--;
                                    }
                                }
                            }
                        }
                    }
                    filter.pendingUnreadCount = unreadCount;
                    /*if (BuildVars.DEBUG_VERSION) {
                        FileLog.d("filter " + filter.name + " flags = " + filter.flags + " unread count = " + filter.pendingUnreadCount);
                    }*/
                    if (apply) {
                        filter.unreadCount = unreadCount;
                    }
                } else if (isMain) {
                    pendingMainUnreadCount = unreadCount;
                    if (apply) {
                        mainUnreadCount = unreadCount;
                    }
                } else if (isArchive) {
                    pendingArchiveUnreadCount = unreadCount;
                    if (apply) {
                        archiveUnreadCount = unreadCount;
                    }
                }
            }
        } catch (Exception e) {
            checkSQLException(e);
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
    }

    private void saveDialogFilterInternal(MessagesController.DialogFilter filter, boolean atBegin, boolean peers) {
        SQLitePreparedStatement state = null;
        try {
            if (!dialogFilters.contains(filter)) {
                if (atBegin) {
                    if (dialogFilters.get(0).isDefault()) {
                        dialogFilters.add(1, filter);
                    } else {
                        dialogFilters.add(0, filter);
                    }
                } else {
                    dialogFilters.add(filter);
                }
                dialogFiltersMap.put(filter.id, filter);
            }

            state = database.executeFast("REPLACE INTO dialog_filter VALUES(?, ?, ?, ?, ?, ?, ?, ?)");
            state.bindInteger(1, filter.id);
            state.bindInteger(2, filter.order);
            state.bindInteger(3, filter.unreadCount);
            state.bindInteger(4, filter.flags);
            state.bindString(5, filter.id == 0 ? "ALL_CHATS" : filter.name);
            state.bindInteger(6, filter.color);
            final Vector<TLRPC.MessageEntity> entitiesVector = new Vector<>(TLRPC.MessageEntity::TLdeserialize);
            entitiesVector.objects.addAll(filter.entities);
            final NativeByteBuffer entitiesBuffer = new NativeByteBuffer(entitiesVector.getObjectSize());
            entitiesVector.serializeToStream(entitiesBuffer);
            state.bindByteBuffer(7, entitiesBuffer);
            state.bindInteger(8, filter.title_noanimate ? 1 : 0);
            state.step();
            state.dispose();
            entitiesBuffer.reuse();
            state = null;
            if (peers) {
                database.executeFast("DELETE FROM dialog_filter_ep WHERE id = " + filter.id).stepThis().dispose();
                database.executeFast("DELETE FROM dialog_filter_pin_v2 WHERE id = " + filter.id).stepThis().dispose();
                database.beginTransaction();
                state = database.executeFast("REPLACE INTO dialog_filter_pin_v2 VALUES(?, ?, ?)");
                for (int a = 0, N = filter.alwaysShow.size(); a < N; a++) {
                    long key = filter.alwaysShow.get(a);
                    state.requery();
                    state.bindInteger(1, filter.id);
                    state.bindLong(2, key);
                    state.bindInteger(3, filter.pinnedDialogs.get(key, Integer.MIN_VALUE));
                    state.step();
                }
                for (int a = 0, N = filter.pinnedDialogs.size(); a < N; a++) {
                    long key = filter.pinnedDialogs.keyAt(a);
                    if (!DialogObject.isEncryptedDialog(key)) {
                        continue;
                    }
                    state.requery();
                    state.bindInteger(1, filter.id);
                    state.bindLong(2, key);
                    state.bindInteger(3, filter.pinnedDialogs.valueAt(a));
                    state.step();
                }
                state.dispose();
                state = null;

                state = database.executeFast("REPLACE INTO dialog_filter_ep VALUES(?, ?)");
                for (int a = 0, N = filter.neverShow.size(); a < N; a++) {
                    state.requery();
                    state.bindInteger(1, filter.id);
                    state.bindLong(2, filter.neverShow.get(a));
                    state.step();
                }
                state.dispose();
                state = null;
                database.commitTransaction();
            }
        } catch (Exception e) {
            checkSQLException(e);
        } finally {
            if (database != null) {
                database.commitTransaction();
            }
            if (state != null) {
                state.dispose();
            }
        }
    }

    private ArrayList<Long> toPeerIds(ArrayList<TLRPC.InputPeer> inputPeers) {
        ArrayList<Long> array = new ArrayList<Long>();
        if (inputPeers == null) {
            return array;
        }
        final int count = inputPeers.size();
        for (int i = 0; i < count; ++i) {
            TLRPC.InputPeer peer = inputPeers.get(i);
            if (peer == null) {
                continue;
            }
            long id;
            if (peer.user_id != 0) {
                id = peer.user_id;
            } else {
                id = -(peer.chat_id != 0 ? peer.chat_id : peer.channel_id);
            }
            array.add(id);
        }
        return array;
    }

    public void checkLoadedRemoteFilters(ArrayList<TLRPC.DialogFilter> vector, Runnable onDone) {
        storageQueue.postRunnable(() -> {
            try {
                SparseArray<MessagesController.DialogFilter> filtersToDelete = new SparseArray<>();
                for (int a = 0, N = dialogFilters.size(); a < N; a++) {
                    MessagesController.DialogFilter filter = dialogFilters.get(a);
                    filtersToDelete.put(filter.id, filter);
                }
                ArrayList<Integer> filtersOrder = new ArrayList<>();

                ArrayList<Long> usersToLoad = new ArrayList<>();
                HashMap<Long, TLRPC.InputPeer> usersToLoadMap = new HashMap<>();
                ArrayList<Long> chatsToLoad = new ArrayList<>();
                HashMap<Long, TLRPC.InputPeer> chatsToLoadMap = new HashMap<>();
                ArrayList<Long> dialogsToLoad = new ArrayList<>();
                HashMap<Long, TLRPC.InputPeer> dialogsToLoadMap = new HashMap<>();

                ArrayList<MessagesController.DialogFilter> filtersToSave = new ArrayList<>();
                HashMap<Integer, HashSet<Long>> filterDialogRemovals = new HashMap<>();
                HashSet<Integer> filtersUnreadCounterReset = new HashSet<>();
                for (int a = 0, N = vector.size(); a < N; a++) {
                    TLRPC.DialogFilter newFilter = (TLRPC.DialogFilter) vector.get(a);
                    filtersOrder.add(newFilter.id);
                    int newFlags = 0;
                    if (newFilter.contacts) {
                        newFlags |= MessagesController.DIALOG_FILTER_FLAG_CONTACTS;
                    }
                    if (newFilter.non_contacts) {
                        newFlags |= MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS;
                    }
                    if (newFilter.groups) {
                        newFlags |= MessagesController.DIALOG_FILTER_FLAG_GROUPS;
                    }
                    if (newFilter.broadcasts) {
                        newFlags |= MessagesController.DIALOG_FILTER_FLAG_CHANNELS;
                    }
                    if (newFilter.bots) {
                        newFlags |= MessagesController.DIALOG_FILTER_FLAG_BOTS;
                    }
                    if (newFilter.exclude_muted) {
                        newFlags |= MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED;
                    }
                    if (newFilter.exclude_read) {
                        newFlags |= MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_READ;
                    }
                    if (newFilter.exclude_archived) {
                        newFlags |= MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED;
                    }
                    if (newFilter instanceof TLRPC.TL_dialogFilterChatlist) {
                        newFlags |= MessagesController.DIALOG_FILTER_FLAG_CHATLIST;
                        if (newFilter.has_my_invites) {
                            newFlags |= MessagesController.DIALOG_FILTER_FLAG_CHATLIST_ADMIN;
                        }
                    }

                    MessagesController.DialogFilter filter = dialogFiltersMap.get(newFilter.id);
                    if (filter != null) {
                        filtersToDelete.remove(newFilter.id);
                        boolean changed = false;
                        boolean unreadChanged = false;
                        if (!TextUtils.equals(filter.name, newFilter.title.text) || !MediaDataController.entitiesEqual(filter.entities, newFilter.title.entities)) {
                            changed = true;
                            filter.name = newFilter.title.text;
                            filter.entities = newFilter.title.entities;
                        }
                        if (filter.title_noanimate != newFilter.title_noanimate) {
                            changed = true;
                            filter.title_noanimate= newFilter.title_noanimate;
                        }
                        final int color = (newFilter.flags & 134217728) != 0 ? newFilter.color : -1;
                        if (filter.color != color) {
                            filter.color = color;
                            changed = true;
                        }
                        if (filter.flags != newFlags) {
                            filter.flags = newFlags;
                            changed = true;
                            unreadChanged = true;
                        }

                        HashSet<Long> existingIds = new HashSet<>(filter.alwaysShow);
                        existingIds.addAll(filter.neverShow);
                        HashSet<Long> existingDialogsIds = new HashSet<>();

                        LinkedHashMap<Integer, Long> secretChatsMap = null;
                        if (filter.pinnedDialogs.size() != 0) {
                            ArrayList<Long> pinArray = new ArrayList<>();
                            boolean hasSecret = false;
                            for (int c = 0, N2 = filter.pinnedDialogs.size(); c < N2; c++) {
                                long did = filter.pinnedDialogs.keyAt(c);
                                if (DialogObject.isEncryptedDialog(did)) {
                                    hasSecret = true;
                                }
                                pinArray.add(did);
                            }
                            if (hasSecret) {
                                secretChatsMap = new LinkedHashMap<>();
                                LongSparseIntArray pinnedDialogs = filter.pinnedDialogs;
                                Collections.sort(pinArray, (o1, o2) -> {
                                    int idx1 = pinnedDialogs.get(o1);
                                    int idx2 = pinnedDialogs.get(o2);
                                    if (idx1 > idx2) {
                                        return 1;
                                    } else if (idx1 < idx2) {
                                        return -1;
                                    }
                                    return 0;
                                });
                                for (int c = 0, N2 = pinArray.size(); c < N2; c++) {
                                    long did = pinArray.get(c);
                                    if (!DialogObject.isEncryptedDialog(did)) {
                                        continue;
                                    }
                                    secretChatsMap.put(c, did);
                                }
                            }
                        }
                        for (int c = 0, N2 = filter.pinnedDialogs.size(); c < N2; c++) {
                            long did = filter.pinnedDialogs.keyAt(c);
                            if (DialogObject.isEncryptedDialog(did)) {
                                continue;
                            }
                            existingDialogsIds.add(did);
                            existingIds.remove(did);
                        }

                        filter.pinnedDialogs.clear();
                        for (int b = 0, N2 = newFilter.pinned_peers.size(); b < N2; b++) {
                            TLRPC.InputPeer peer = newFilter.pinned_peers.get(b);
                            Long id;
                            if (peer.user_id != 0) {
                                id = peer.user_id;
                            } else {
                                id = -(peer.chat_id != 0 ? peer.chat_id : peer.channel_id);
                            }
                            int index = filter.pinnedDialogs.size();
                            if (secretChatsMap != null) {
                                Long did;
                                while ((did = secretChatsMap.remove(index)) != null) {
                                    filter.pinnedDialogs.put(did, index);
                                    index++;
                                }
                            }
                            filter.pinnedDialogs.put(id, index);
                            existingIds.remove(id);
                            if (!existingDialogsIds.remove(id)) {
                                changed = true;
                                if (!dialogsToLoadMap.containsKey(id)) {
                                    dialogsToLoad.add(id);
                                    dialogsToLoadMap.put(id, peer);
                                }
                            }
                        }
                        if (secretChatsMap != null) {
                            for (LinkedHashMap.Entry<Integer, Long> entry : secretChatsMap.entrySet()) {
                                filter.pinnedDialogs.put(entry.getValue(), filter.pinnedDialogs.size());
                            }
                        }

                        for (int c = 0; c < 2; c++) {
                            ArrayList<Long> fromArray = toPeerIds(c == 0 ? newFilter.include_peers : newFilter.exclude_peers);
                            ArrayList<Long> toArray = c == 0 ? filter.alwaysShow : filter.neverShow;

                            if (c == 0) {
                                // put pinned_peers into include_peers (alwaysShow)
                                ArrayList<Long> pinnedArray = toPeerIds(newFilter.pinned_peers);
                                for (int i = 0; i < pinnedArray.size(); ++i) {
                                    fromArray.remove(pinnedArray.get(i));
                                }
                                fromArray.addAll(0, pinnedArray);
                            }

                            final int fromArrayCount = fromArray.size();
                            boolean isDifferent = fromArray.size() != toArray.size();
                            if (!isDifferent) {
                                for (int i = 0; i < fromArrayCount; ++i) {
                                    if (!toArray.contains(fromArray.get(i))) {
                                        isDifferent = true;
                                        break;
                                    }
                                }
                            }

                            if (isDifferent) {
                                unreadChanged = true;
                                changed = true;
                                if (c == 0) {
                                    filter.alwaysShow = fromArray;
                                } else {
                                    filter.neverShow = fromArray;
                                }
                            }
                        }
                        if (!existingDialogsIds.isEmpty()) {
                            filterDialogRemovals.put(filter.id, existingDialogsIds);
                            changed = true;
                        }
                        if (changed) {
                            filtersToSave.add(filter);
                        }
                        if (unreadChanged) {
                            filtersUnreadCounterReset.add(filter.id);
                        }
                    } else {
                        filter = new MessagesController.DialogFilter();
                        filter.id = newFilter.id;
                        filter.flags = newFlags;
                        filter.name = newFilter.title.text;
                        filter.entities = newFilter.title.entities;
                        filter.title_noanimate = newFilter.title_noanimate;
                        filter.color = (newFilter.flags & 134217728) != 0 ? newFilter.color : -1;
                        filter.pendingUnreadCount = -1;
                        for (int c = 0; c < 2; c++) {
                            if (c == 0) {
                                for (int b = 0, N2 = newFilter.pinned_peers.size(); b < N2; b++) {
                                    TLRPC.InputPeer peer = newFilter.pinned_peers.get(b);
                                    Long id;
                                    if (peer.user_id != 0) {
                                        id = peer.user_id;
                                    } else {
                                        id = -(peer.chat_id != 0 ? peer.chat_id : peer.channel_id);
                                    }
                                    if (!filter.alwaysShow.contains(id)) {
                                        filter.alwaysShow.add(id);
                                    }
                                    filter.pinnedDialogs.put(id, filter.pinnedDialogs.size() + 1);
                                    if (!dialogsToLoadMap.containsKey(id)) {
                                        dialogsToLoad.add(id);
                                        dialogsToLoadMap.put(id, peer);
                                    }
                                }
                            }
                            ArrayList<TLRPC.InputPeer> fromArray = c == 0 ? newFilter.include_peers : newFilter.exclude_peers;
                            ArrayList<Long> toArray = c == 0 ? filter.alwaysShow : filter.neverShow;
                            for (int b = 0, N2 = fromArray.size(); b < N2; b++) {
                                TLRPC.InputPeer peer = fromArray.get(b);
                                if (peer.user_id != 0) {
                                    Long uid = peer.user_id;
                                    if (!toArray.contains(uid)) {
                                        toArray.add(uid);
                                    }
                                    if (!usersToLoadMap.containsKey(uid)) {
                                        usersToLoad.add(uid);
                                        usersToLoadMap.put(uid, peer);
                                    }
                                } else {
                                    Long chatId = peer.chat_id != 0 ? peer.chat_id : peer.channel_id;
                                    Long dialogId = -chatId;
                                    if (!toArray.contains(dialogId)) {
                                        toArray.add(dialogId);
                                    }
                                    if (!chatsToLoadMap.containsKey(chatId)) {
                                        chatsToLoad.add(chatId);
                                        chatsToLoadMap.put(chatId, peer);
                                    }
                                }
                            }
                        }
                        filtersToSave.add(filter);
                    }
                }

                TLRPC.messages_Dialogs dialogs;
                if (!dialogsToLoad.isEmpty()) {
                    dialogs = loadDialogsByIds(TextUtils.join(",", dialogsToLoad), usersToLoad, chatsToLoad, new ArrayList<>());
                    for (int a = 0, N = dialogs.dialogs.size(); a < N; a++) {
                        TLRPC.Dialog dialog = dialogs.dialogs.get(a);
                        dialogsToLoadMap.remove(dialog.id);
                    }
                } else {
                    dialogs = new TLRPC.TL_messages_dialogs();
                }
                ArrayList<TLRPC.User> users = new ArrayList<>();
                if (!usersToLoad.isEmpty()) {
                    getUsersInternal(usersToLoad, users);
                    for (int a = 0, N = users.size(); a < N; a++) {
                        TLRPC.User user = users.get(a);
                        usersToLoadMap.remove(user.id);
                    }
                }
                ArrayList<TLRPC.Chat> chats = new ArrayList<>();
                if (!chatsToLoad.isEmpty()) {
                    getChatsInternal(TextUtils.join(",", chatsToLoad), chats);
                    for (int a = 0, N = chats.size(); a < N; a++) {
                        TLRPC.Chat chat = chats.get(a);
                        chatsToLoadMap.remove(chat.id);
                    }
                }

                if (usersToLoadMap.isEmpty() && chatsToLoadMap.isEmpty() && dialogsToLoadMap.isEmpty()) {
                    processLoadedFilterPeersInternal(dialogs, null, users, chats, filtersToSave, filtersToDelete, filtersOrder, filterDialogRemovals, filtersUnreadCounterReset, onDone);
                } else {
                    getMessagesController().loadFilterPeers(dialogsToLoadMap, usersToLoadMap, chatsToLoadMap, dialogs, new TLRPC.TL_messages_dialogs(), users, chats, filtersToSave, filtersToDelete, filtersOrder, filterDialogRemovals, filtersUnreadCounterReset, onDone);
                }
            } catch (Exception e) {
                checkSQLException(e);
            }
        });
    }

    private void processLoadedFilterPeersInternal(TLRPC.messages_Dialogs pinnedDialogs, TLRPC.messages_Dialogs pinnedRemoteDialogs, ArrayList<TLRPC.User> users, ArrayList<TLRPC.Chat> chats, ArrayList<MessagesController.DialogFilter> filtersToSave, SparseArray<MessagesController.DialogFilter> filtersToDelete, ArrayList<Integer> filtersOrder, HashMap<Integer, HashSet<Long>> filterDialogRemovals, HashSet<Integer> filtersUnreadCounterReset, Runnable onDone) {
        boolean anythingChanged = false;
        putUsersAndChats(users, chats, true, false);
        for (int a = 0, N = filtersToDelete.size(); a < N; a++) {
            deleteDialogFilterInternal(filtersToDelete.valueAt(a));
            anythingChanged = true;
        }
        for (Integer id : filtersUnreadCounterReset) {
            MessagesController.DialogFilter filter = dialogFiltersMap.get(id);
            if (filter == null) {
                continue;
            }
            filter.pendingUnreadCount = -1;
        }
        for (HashMap.Entry<Integer, HashSet<Long>> entry : filterDialogRemovals.entrySet()) {
            MessagesController.DialogFilter filter = dialogFiltersMap.get(entry.getKey());
            if (filter == null) {
                continue;
            }
            HashSet<Long> set = entry.getValue();
            for (Long id : set) {
                filter.pinnedDialogs.delete(id);
            }
            anythingChanged = true;
        }
        for (int a = 0, N = filtersToSave.size(); a < N; a++) {
            saveDialogFilterInternal(filtersToSave.get(a), false, true);
            anythingChanged = true;
        }
        boolean orderChanged = false;
        for (int a = 0, N = dialogFilters.size(); a < N; a++) {
            MessagesController.DialogFilter filter = dialogFilters.get(a);
            int order = filtersOrder.indexOf(filter.id);
            if (filter.order != order) {
                filter.order = order;
                anythingChanged = true;
                orderChanged = true;
            }
        }
        if (orderChanged) {
            Collections.sort(dialogFilters, (o1, o2) -> {
                if (o1.order > o2.order) {
                    return 1;
                } else if (o1.order < o2.order) {
                    return -1;
                }
                return 0;
            });
            saveDialogFiltersOrderInternal();
        }
        int remote = anythingChanged ? 1 : 2;
        calcUnreadCounters(true);
        getMessagesController().processLoadedDialogFilters(new ArrayList<>(dialogFilters), pinnedDialogs, pinnedRemoteDialogs, users, chats, null, remote, onDone);
    }

    protected void processLoadedFilterPeers(TLRPC.messages_Dialogs pinnedDialogs, TLRPC.messages_Dialogs pinnedRemoteDialogs, ArrayList<TLRPC.User> users, ArrayList<TLRPC.Chat> chats, ArrayList<MessagesController.DialogFilter> filtersToSave, SparseArray<MessagesController.DialogFilter> filtersToDelete, ArrayList<Integer> filtersOrder, HashMap<Integer, HashSet<Long>> filterDialogRemovals, HashSet<Integer> filtersUnreadCounterReset, Runnable onDone) {
        storageQueue.postRunnable(() -> processLoadedFilterPeersInternal(pinnedDialogs, pinnedRemoteDialogs, users, chats, filtersToSave, filtersToDelete, filtersOrder, filterDialogRemovals, filtersUnreadCounterReset, onDone));
    }

    private void deleteDialogFilterInternal(MessagesController.DialogFilter filter) {
        try {
            dialogFilters.remove(filter);
            dialogFiltersMap.remove(filter.id);
            database.executeFast("DELETE FROM dialog_filter WHERE id = " + filter.id).stepThis().dispose();
            database.executeFast("DELETE FROM dialog_filter_ep WHERE id = " + filter.id).stepThis().dispose();
            database.executeFast("DELETE FROM dialog_filter_pin_v2 WHERE id = " + filter.id).stepThis().dispose();
        } catch (Exception e) {
            checkSQLException(e);
        }
    }

    public void deleteDialogFilter(MessagesController.DialogFilter filter) {
        storageQueue.postRunnable(() -> deleteDialogFilterInternal(filter));
    }

    public void saveDialogFilter(MessagesController.DialogFilter filter, boolean atBegin, boolean peers) {
        storageQueue.postRunnable(() -> {
            saveDialogFilterInternal(filter, atBegin, peers);
            calcUnreadCounters(false);
            AndroidUtilities.runOnUIThread(() -> {
                ArrayList<MessagesController.DialogFilter> filters = getMessagesController().dialogFilters;
                for (int a = 0, N = filters.size(); a < N; a++) {
                    filters.get(a).unreadCount = filters.get(a).pendingUnreadCount;
                }
                mainUnreadCount = pendingMainUnreadCount;
                archiveUnreadCount = pendingArchiveUnreadCount;
                getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_READ_DIALOG_MESSAGE);
            });
        });
    }

    public void saveDialogFiltersOrderInternal() {
        SQLitePreparedStatement state = null;
        try {
            state = database.executeFast("UPDATE dialog_filter SET ord = ?, flags = ? WHERE id = ?");
            for (int a = 0, N = dialogFilters.size(); a < N; a++) {
                MessagesController.DialogFilter filter = dialogFilters.get(a);
                state.requery();
                state.bindInteger(1, filter.order);
                state.bindInteger(2, filter.flags);
                state.bindInteger(3, filter.id);
                state.step();
            }
            state.dispose();
            state = null;
        } catch (Exception e) {
            checkSQLException(e);
        } finally {
            if (state != null) {
                state.dispose();
            }
        }
    }

    public void saveDialogFiltersOrder() {
        ArrayList<MessagesController.DialogFilter> filtersFinal = new ArrayList<>(getMessagesController().dialogFilters);
        storageQueue.postRunnable(() -> {
            dialogFilters.clear();
            dialogFiltersMap.clear();
            dialogFilters.addAll(filtersFinal);
            for (int i = 0; i < filtersFinal.size(); i++) {
                filtersFinal.get(i).order = i;
                dialogFiltersMap.put(filtersFinal.get(i).id, filtersFinal.get(i));
            }
            saveDialogFiltersOrderInternal();
        });
    }

    protected static void addReplyMessages(TLRPC.Message message, LongSparseArray<SparseArray<ArrayList<TLRPC.Message>>> replyMessageOwners, LongSparseArray<ArrayList<Integer>> dialogReplyMessagesIds) {
        int messageId = message.reply_to.reply_to_msg_id;
        long dialogId = (message.flags & 1073741824) != 0 ? message.quick_reply_shortcut_id : MessageObject.getReplyToDialogId(message);
        SparseArray<ArrayList<TLRPC.Message>> sparseArray = replyMessageOwners.get(dialogId);
        ArrayList<Integer> ids = dialogReplyMessagesIds.get(dialogId);
        if (sparseArray == null) {
            sparseArray = new SparseArray<>();
            replyMessageOwners.put(dialogId, sparseArray);
        }
        if (ids == null) {
            ids = new ArrayList<>();
            dialogReplyMessagesIds.put(dialogId, ids);
        }
        ArrayList<TLRPC.Message> arrayList = sparseArray.get(message.reply_to.reply_to_msg_id);
        if (arrayList == null) {
            arrayList = new ArrayList<>();
            sparseArray.put(message.reply_to.reply_to_msg_id, arrayList);
            if (!ids.contains(message.reply_to.reply_to_msg_id)) {
                ids.add(message.reply_to.reply_to_msg_id);
            }
        }
        arrayList.add(message);
    }

    protected void loadReplyMessages(LongSparseArray<SparseArray<ArrayList<TLRPC.Message>>> replyMessageOwners, LongSparseArray<ArrayList<Integer>> dialogReplyMessagesIds, ArrayList<Long> usersToLoad, ArrayList<Long> chatsToLoad, int mode) throws SQLiteException {
        if (replyMessageOwners.isEmpty()) {
            return;
        }

        final boolean scheduled = mode == ChatActivity.MODE_SCHEDULED;
        final boolean quickReplies = mode == ChatActivity.MODE_QUICK_REPLIES;
        final boolean welcomeMessages = mode == ChatActivity.MODE_WELCOME_MESSAGES;
        final long selfId = getUserConfig().getClientUserId();

        for (int b = 0, N2 = replyMessageOwners.size(); b < N2; b++) {
            long dialogId = replyMessageOwners.keyAt(b);
            SparseArray<ArrayList<TLRPC.Message>> owners = replyMessageOwners.valueAt(b);
            ArrayList<Integer> ids = dialogReplyMessagesIds.get(dialogId);
            if (ids == null) {
                continue;
            }
            SQLiteCursor cursor = null;
            try {
                for (int i = 0; i < 2; i++) {
                    if (i == 1 && !scheduled) {
                        continue;
                    }
                    boolean findInScheduled = i == 1;
                    if (welcomeMessages) {
                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT data, mid, date, dialog_id FROM welcome_messages WHERE mid IN(%s) AND dialog_id = %d", TextUtils.join(",", ids), dialogId));
                    } else if (quickReplies) {
                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT data, mid, date, topic_id FROM quick_replies_messages WHERE mid IN(%s) AND topic_id = %d", TextUtils.join(",", ids), dialogId));
                    } else if (findInScheduled) {
                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT data, mid, date, uid FROM scheduled_messages_v2 WHERE mid IN(%s) AND uid = %d", TextUtils.join(",", ids), dialogId));
                    } else {
                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT data, mid, date, uid FROM messages_v2 WHERE mid IN(%s) AND uid = %d", TextUtils.join(",", ids), dialogId));
                    }
                    while (cursor.next()) {
                        NativeByteBuffer data = cursor.byteBufferValue(0);
                        if (data != null) {
                            TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                            message.readAttachPath(data, getUserConfig().clientUserId);
                            data.reuse();
                            message.id = cursor.intValue(1);
                            message.date = cursor.intValue(2);
                            if (quickReplies) {
                                message.dialog_id = selfId;
                                message.flags |= 1073741824;
                                message.quick_reply_shortcut_id = cursor.intValue(3);
                            } else {
                                message.dialog_id = cursor.longValue(3);
                            }

                            addUsersAndChatsFromMessage(message, usersToLoad, chatsToLoad, null);

                            ArrayList<TLRPC.Message> arrayList = owners.get(message.id);
                            if (arrayList != null) {
                                for (int a = 0, N = arrayList.size(); a < N; a++) {
                                    TLRPC.Message m = arrayList.get(a);
                                    m.replyMessage = message;
                                    MessageObject.getDialogId(message);
                                }
                            }
                        }
                    }
                    cursor.dispose();
                }
            } catch (Exception e) {
                throw e;
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
        }
    }

    public void loadUnreadMessages() {
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            int magic = 0;
            try {
                ArrayList<Long> usersToLoad = new ArrayList<>();
                ArrayList<Long> chatsToLoad = new ArrayList<>();
                ArrayList<Integer> encryptedChatIds = new ArrayList<>();

                LongSparseArray<Integer> pushDialogs = new LongSparseArray<>();
                cursor = database.queryFinalized("SELECT d.did, d.unread_count, s.flags FROM dialogs as d LEFT JOIN dialog_settings as s ON d.did = s.did WHERE d.unread_count > 0");
                StringBuilder ids = new StringBuilder();
                int currentTime = getConnectionsManager().getCurrentTime();
                while (cursor.next()) {
                    long flags = cursor.longValue(2);
                    boolean muted = (flags & 1) != 0;
                    int mutedUntil = (int) (flags >> 32);
                    if (cursor.isNull(2) || !muted || mutedUntil != 0 && mutedUntil < currentTime) {
                        long did = cursor.longValue(0);
                        if (DialogObject.isFolderDialogId(did)) {
                            continue;
                        }
                        int count = cursor.intValue(1);
                        pushDialogs.put(did, count);
                        if (ids.length() != 0) {
                            ids.append(",");
                        }
                        ids.append(did);
                        if (DialogObject.isEncryptedDialog(did)) {
                            int encryptedChatId = DialogObject.getEncryptedChatId(did);
                            if (!encryptedChatIds.contains(encryptedChatId)) {
                                encryptedChatIds.add(encryptedChatId);
                            }
                        } else if (DialogObject.isUserDialog(did)) {
                            if (!usersToLoad.contains(did)) {
                                usersToLoad.add(did);
                            }
                        } else {
                            if (!chatsToLoad.contains(-did)) {
                                chatsToLoad.add(-did);
                            }
                        }
                    }
                }
                cursor.dispose();
                cursor = null;

                LongSparseArray<SparseArray<ArrayList<TLRPC.Message>>> replyMessageOwners = new LongSparseArray<>();
                LongSparseArray<ArrayList<Integer>> dialogReplyMessagesIds = new LongSparseArray<>();
                ArrayList<TLRPC.Message> messages = new ArrayList<>();
                ArrayList<MessageObject> pushMessages = new ArrayList<>();
                ArrayList<TLRPC.User> users = new ArrayList<>();
                ArrayList<TLRPC.Chat> chats = new ArrayList<>();
                ArrayList<TLRPC.EncryptedChat> encryptedChats = new ArrayList<>();
                int maxDate = 0;
                if (ids.length() > 0) {
                    cursor = database.queryFinalized("SELECT read_state, data, send_state, mid, date, uid, replydata FROM messages_v2 WHERE uid IN (" + ids.toString() + ") AND out = 0 AND read_state IN(0,2) ORDER BY date DESC LIMIT 50");
                    while (cursor.next()) {
                        NativeByteBuffer data = cursor.byteBufferValue(1);
                        if (data != null) {
                            magic = data.readInt32(false);
                            TLRPC.Message message = TLRPC.Message.TLdeserialize(data, magic, false);
                            message.readAttachPath(data, getUserConfig().clientUserId);
                            data.reuse();
                            MessageObject.setUnreadFlags(message, cursor.intValue(0));
                            message.id = cursor.intValue(3);
                            message.date = cursor.intValue(4);
                            message.dialog_id = cursor.longValue(5);
                            messages.add(message);
                            maxDate = Math.max(maxDate, message.date);

                            addUsersAndChatsFromMessage(message, usersToLoad, chatsToLoad, null);
                            message.send_state = cursor.intValue(2);
                            if (message.peer_id.channel_id == 0 && !MessageObject.isUnread(message) && !DialogObject.isEncryptedDialog(message.dialog_id) || message.id > 0) {
                                message.send_state = 0;
                            }
                            if (DialogObject.isEncryptedDialog(message.dialog_id) && !cursor.isNull(5)) {
                                message.random_id = cursor.longValue(5);
                            }

                            try {
                                if (message.reply_to != null && message.reply_to.reply_to_msg_id != 0 && isMessageActionTypeWithReply(message.action)) {
                                    if (!cursor.isNull(6)) {
                                        data = cursor.byteBufferValue(6);
                                        if (data != null) {
                                            message.replyMessage = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                                            message.replyMessage.readAttachPath(data, getUserConfig().clientUserId);
                                            data.reuse();
                                            if (message.replyMessage != null) {
                                                addUsersAndChatsFromMessage(message.replyMessage, usersToLoad, chatsToLoad, null);
                                            }
                                        }
                                    }
                                    if (message.replyMessage == null) {
                                        addReplyMessages(message, replyMessageOwners, dialogReplyMessagesIds);
                                    }
                                }
                            } catch (Exception e) {
                                checkSQLException(e);
                            }
                        }
                    }
                    cursor.dispose();
                    cursor = null;

                    database.executeFast("DELETE FROM unread_push_messages WHERE date <= " + maxDate).stepThis().dispose();
                    cursor = database.queryFinalized("SELECT data, mid, date, uid, random, fm, name, uname, flags, topicId, is_reaction FROM unread_push_messages WHERE 1 ORDER BY date DESC LIMIT 50");
                    while (cursor.next()) {
                        NativeByteBuffer data = cursor.byteBufferValue(0);
                        if (data != null) {
                            TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                            data.reuse();
                            message.id = cursor.intValue(1);
                            message.date = cursor.intValue(2);
                            message.dialog_id = cursor.longValue(3);
                            message.random_id = cursor.longValue(4);
                            String messageText = cursor.isNull(5) ? null : cursor.stringValue(5);
                            String name = cursor.isNull(6) ? null : cursor.stringValue(6);
                            String userName = cursor.isNull(7) ? null : cursor.stringValue(7);
                            int flags = cursor.intValue(8);
                            int topicId = cursor.intValue(9);
                            if (MessageObject.getFromChatId(message) == 0) {
                                if (DialogObject.isUserDialog(message.dialog_id)) {
                                    message.from_id = new TLRPC.TL_peerUser();
                                    message.from_id.user_id = message.dialog_id;
                                }
                            }
                            if (DialogObject.isUserDialog(message.dialog_id)) {
                                if (!usersToLoad.contains(message.dialog_id)) {
                                    usersToLoad.add(message.dialog_id);
                                }
                            } else if (DialogObject.isChatDialog(message.dialog_id)) {
                                if (!chatsToLoad.contains(-message.dialog_id)) {
                                    chatsToLoad.add(-message.dialog_id);
                                }
                            }
                            if (topicId != 0) {
                                message.reply_to = new TLRPC.TL_messageReplyHeader();
                                message.reply_to.forum_topic = true;
                                message.reply_to.reply_to_top_id = topicId;
                            }

                            MessageObject messageObject = new MessageObject(currentAccount, message, messageText, name, userName, (flags & 1) != 0, (flags & 2) != 0, (message.flags & 0x80000000) != 0, false);
                            final int is_reaction = cursor.intValue(10);
                            messageObject.isReactionPush = is_reaction == 1;
                            messageObject.isStoryReactionPush = is_reaction == 2;
                            pushMessages.add(messageObject);
                            addUsersAndChatsFromMessage(message, usersToLoad, chatsToLoad, null);
                        }
                    }
                    cursor.dispose();
                    cursor = null;

                    loadReplyMessages(replyMessageOwners, dialogReplyMessagesIds, usersToLoad, chatsToLoad, 0);

                    if (!encryptedChatIds.isEmpty()) {
                        getEncryptedChatsInternal(TextUtils.join(",", encryptedChatIds), encryptedChats, usersToLoad);
                    }

                    if (!usersToLoad.isEmpty()) {
                        getUsersInternal(usersToLoad, users);
                    }

                    if (!chatsToLoad.isEmpty()) {
                        getChatsInternal(TextUtils.join(",", chatsToLoad), chats);
                        for (int a = 0; a < chats.size(); a++) {
                            TLRPC.Chat chat = chats.get(a);
                            if (chat != null && (ChatObject.isNotInChat(chat) || chat.min || chat.migrated_to != null)) {
                                long did = -chat.id;
                                database.executeFast("UPDATE dialogs SET unread_count = 0 WHERE did = " + did).stepThis().dispose();
                                database.executeFast(String.format(Locale.US, "UPDATE messages_v2 SET read_state = 3 WHERE uid = %d AND mid > 0 AND read_state IN(0,2) AND out = 0", did)).stepThis().dispose();
                                chats.remove(a);
                                a--;
                                pushDialogs.remove(did);
                                for (int b = 0; b < messages.size(); b++) {
                                    TLRPC.Message message = messages.get(b);
                                    if (message.dialog_id == did) {
                                        messages.remove(b);
                                        b--;
                                    }
                                }
                            }
                        }
                    }
                }
                Collections.reverse(messages);

                usersToLoad.clear();
                chatsToLoad.clear();
                cursor = database.queryFinalized("SELECT uid, sid, date, expire_date, localName, flags FROM story_pushes");
                HashMap<Long, NotificationsController.StoryNotification> storyPushes = new HashMap<>();
                while (cursor.next()) {
                    long dialogId = cursor.longValue(0);
                    if (dialogId >= 0) {
                        if (!usersToLoad.contains(dialogId)) {
                            usersToLoad.add(dialogId);
                        }
                    } else {
                        if (!chatsToLoad.contains(dialogId)) {
                            chatsToLoad.add(dialogId);
                        }
                    }
                    int id = cursor.intValue(1);
                    long date = cursor.longValue(2);
                    long expire_date = cursor.longValue(3);
                    String localName = cursor.stringValue(4);
                    int flags = cursor.intValue(5);
                    NotificationsController.StoryNotification notification = storyPushes.get(dialogId);
                    if (notification != null) {
                        notification.dateByIds.put(id, new Pair<>(date, expire_date));
                        notification.date = notification.getLeastDate();
                        notification.hidden |= (flags & 1) != 0;
                        if (!TextUtils.isEmpty(localName)) {
                            notification.localName = localName;
                        }
                    } else {
                        notification = new NotificationsController.StoryNotification(dialogId, localName, id, date, expire_date);
                        notification.hidden = (flags & 1) != 0;
                        storyPushes.put(dialogId, notification);
                    }
                }
                cursor.dispose();
                cursor = null;

                if (!usersToLoad.isEmpty()) {
                    getUsersInternal(usersToLoad, users);
                }

                if (!chatsToLoad.isEmpty()) {
                    getChatsInternal(TextUtils.join(",", chatsToLoad), chats);
                }

                AndroidUtilities.runOnUIThread(() -> getNotificationsController().processLoadedUnreadMessages(pushDialogs, messages, pushMessages, users, chats, encryptedChats, storyPushes.values()));
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
        });
    }

    public void putWallpapers(ArrayList<TLRPC.WallPaper> wallPapers, int action) {
        storageQueue.postRunnable(() -> {
            SQLitePreparedStatement state = null;
            try {
                if (action == 1) {
                    database.executeFast("DELETE FROM wallpapers2 WHERE num >= -1").stepThis().dispose();
                }
                database.beginTransaction();

                if (action != 0) {
                    state = database.executeFast("REPLACE INTO wallpapers2 VALUES(?, ?, ?)");
                } else {
                    state = database.executeFast("UPDATE wallpapers2 SET data = ? WHERE uid = ?");
                }
                for (int a = 0, N = wallPapers.size(); a < N; a++) {
                    TLRPC.WallPaper wallPaper = (TLRPC.WallPaper) wallPapers.get(a);
                    state.requery();
                    NativeByteBuffer data = new NativeByteBuffer(wallPaper.getObjectSize());
                    wallPaper.serializeToStream(data);
                    if (action != 0) {
                        state.bindLong(1, wallPaper.id);
                        state.bindByteBuffer(2, data);
                        if (action < 0) {
                            state.bindInteger(3, action);
                        } else {
                            state.bindInteger(3, action == 2 ? -1 : a);
                        }
                    } else {
                        state.bindByteBuffer(1, data);
                        state.bindLong(2, wallPaper.id);
                    }
                    state.step();
                    data.reuse();
                }
                state.dispose();
                state = null;
                database.commitTransaction();
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (database != null) {
                    database.commitTransaction();
                }
                if (state != null) {
                    state.dispose();
                }
            }
        });
    }

    public void deleteWallpaper(long id) {
        storageQueue.postRunnable(() -> {
            try {
                database.executeFast("DELETE FROM wallpapers2 WHERE uid = " + id).stepThis().dispose();
            } catch (Exception e) {
                checkSQLException(e);
            }
        });
    }

    public void getWallpapers() {
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            try {
                cursor = database.queryFinalized("SELECT data FROM wallpapers2 WHERE 1 ORDER BY num ASC");
                ArrayList<TLRPC.WallPaper> wallPapers = new ArrayList<>();
                while (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(0);
                    if (data != null) {
                        TLRPC.WallPaper wallPaper = TLRPC.WallPaper.TLdeserialize(data, data.readInt32(false), false);
                        data.reuse();
                        if (wallPaper != null) {
                            wallPapers.add(wallPaper);
                        }
                    }
                }
                AndroidUtilities.runOnUIThread(() -> NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.wallpapersDidLoad, wallPapers));
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
        });
    }

    public void addRecentLocalFile(String imageUrl, String localUrl, TLRPC.Document document) {
        if (imageUrl == null || imageUrl.length() == 0 || ((localUrl == null || localUrl.length() == 0) && document == null)) {
            return;
        }
        storageQueue.postRunnable(() -> {
            SQLitePreparedStatement state = null;
            try {
                if (document != null) {
                    state = database.executeFast("UPDATE web_recent_v3 SET document = ? WHERE image_url = ?");
                    state.requery();
                    NativeByteBuffer data = new NativeByteBuffer(document.getObjectSize());
                    document.serializeToStream(data);
                    state.bindByteBuffer(1, data);
                    state.bindString(2, imageUrl);
                    state.step();
                    state.dispose();
                    data.reuse();
                } else {
                    state = database.executeFast("UPDATE web_recent_v3 SET local_url = ? WHERE image_url = ?");
                    state.requery();
                    state.bindString(1, localUrl);
                    state.bindString(2, imageUrl);
                    state.step();
                    state.dispose();
                }
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (state != null) {
                    state.dispose();
                }
            }
        });
    }

    public void deleteAllReactionsFromChat(long dialogId, long fromId, int msgId) {
        executeInStorageQueue(() -> {
            SQLiteCursor cursor = null;
            SQLitePreparedStatement state = null;
            try {
                final SparseArray<TLRPC.TL_messageReactions> updatedReactions = new SparseArray<>();
                for (int s = 0; s < 2; s++) {
                    if (msgId != 0) {
                        if (s == 0) {
                            cursor = database.queryFinalized("SELECT data FROM messages_v2 WHERE uid = " + dialogId + " AND mid = " + msgId);
                        } else {
                            cursor = database.queryFinalized("SELECT data FROM messages_topics WHERE uid = " + dialogId + " AND mid = " + msgId);
                        }
                    } else {
                        if (s == 0) {
                            cursor = database.queryFinalized("SELECT data FROM messages_v2 WHERE uid = " + dialogId + " ORDER BY mid DESC LIMIT 500");
                        } else {
                            cursor = database.queryFinalized("SELECT data FROM messages_topics WHERE uid = " + dialogId + " ORDER BY mid DESC LIMIT 500");
                        }
                    }

                    ArrayList<TLRPC.Message> messagesToRewrite = new ArrayList<>();
                    while (cursor.next()) {
                        NativeByteBuffer data = cursor.byteBufferValue(0);
                        if (data != null) {
                            boolean updated = false;
                            TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                            if (message != null) {
                                message.readAttachPath(data, getUserConfig().clientUserId);
                                if (message.reactions != null && message.reactions.recent_reactions != null) {
                                    for (int i = 0; i < message.reactions.recent_reactions.size(); i++) {
                                        final TLRPC.MessagePeerReaction reaction = message.reactions.recent_reactions.get(i);
                                        if (MessageObject.getPeerId(reaction.peer_id) == fromId) {
                                            updated = true;
                                            message.reactions.recent_reactions.remove(i);
                                            i--;

                                            if (message.reactions.results != null) {
                                                for (int a = 0; a < message.reactions.results.size(); a++) {
                                                    final TLRPC.ReactionCount reactionCount = message.reactions.results.get(a);
                                                    if (ReactionsUtils.compare(reaction.reaction, reactionCount.reaction)) {
                                                        reactionCount.count--;
                                                        if (reactionCount.count <= 0) {
                                                            message.reactions.results.remove(a);
                                                            a--;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (updated) {
                                    messagesToRewrite.add(message);
                                    updatedReactions.put(message.id, message.reactions);
                                }
                            }
                            data.reuse();
                        }
                    }
                    cursor.dispose();
                    cursor = null;

                    if (!messagesToRewrite.isEmpty()) {
                        database.beginTransaction();
                        for (TLRPC.Message message : messagesToRewrite) {
                            if (s == 0) {
                                state = database.executeFast("UPDATE messages_v2 SET data = ? WHERE mid = ? AND uid = ?");
                            } else {
                                state = database.executeFast("UPDATE messages_topics SET data = ? WHERE mid = ? AND uid = ?");
                            }

                            MessageObject.normalizeFlags(message);
                            NativeByteBuffer data2 = new NativeByteBuffer(message.getObjectSize());
                            message.serializeToStream(data2);

                            state.requery();
                            state.bindByteBuffer(1, data2);
                            state.bindInteger(2, message.id);
                            state.bindLong(3, dialogId);
                            state.step();
                            data2.reuse();
                            state.dispose();
                            state = null;
                        }
                        database.commitTransaction();
                    }
                }
                AndroidUtilities.runOnUIThread(() -> {
                    if (updatedReactions.size() != 0) {
                        for (int a = 0, N = Math.min(updatedReactions.size(), 100); a < N; a++) {
                            final int messageId = updatedReactions.keyAt(a);
                            final TLRPC.TL_messageReactions messageReactions = updatedReactions.valueAt(a);
                            getNotificationCenter().postNotificationName(NotificationCenter.didUpdateReactions, dialogId, messageId, messageReactions);
                        }
                    }
                });
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (database != null) {
                    database.commitTransaction();
                }
                if (cursor != null) {
                    cursor.dispose();
                }
                if (state != null) {
                    state.dispose();
                }
            }
        });
    }

    public void deleteUserChatHistory(long dialogId, long fromId) {
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            try {
                ArrayList<Integer> mids = new ArrayList<>();
                cursor = database.queryFinalized("SELECT data FROM messages_v2 WHERE uid = " + dialogId);
                ArrayList<File> filesToDelete = new ArrayList<>();
                ArrayList<String> namesToDelete = new ArrayList<>();
                ArrayList<Pair<Long, Integer>> idsToDelete = new ArrayList<>();
                try {
                    while (cursor.next()) {
                        NativeByteBuffer data = cursor.byteBufferValue(0);
                        if (data != null) {
                            TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                            if (message != null) {
                                message.readAttachPath(data, getUserConfig().clientUserId);
                                if (UserObject.isReplyUser(dialogId) && MessageObject.getPeerId(message.fwd_from.from_id) == fromId || MessageObject.getFromChatId(message) == fromId && message.id != 1) {
                                    mids.add(message.id);
                                    addFilesToDelete(message, filesToDelete, idsToDelete, namesToDelete, false);
                                }
                            }
                            data.reuse();
                        }
                    }
                } catch (Exception e) {
                    checkSQLException(e);
                }
                cursor.dispose();
                cursor = null;
                deleteFromDownloadQueue(idsToDelete, true);
                AndroidUtilities.runOnUIThread(() -> {
                    getFileLoader().cancelLoadFiles(namesToDelete);
                    getMessagesController().markDialogMessageAsDeleted(dialogId, mids);
                });
                markMessagesAsDeletedInternal(dialogId, mids, false, 0, 0);
                updateDialogsWithDeletedMessagesInternal(dialogId, DialogObject.isChatDialog(dialogId) ? -dialogId : 0, mids, null);
                getFileLoader().deleteFiles(filesToDelete, 0);
                if (!mids.isEmpty()) {
                    AndroidUtilities.runOnUIThread(() -> getNotificationCenter().postNotificationName(NotificationCenter.messagesDeleted, mids, DialogObject.isChatDialog(dialogId) ? -dialogId : 0, false));
                }
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
        });
    }

    private boolean addFilesToDelete(TLRPC.Message message, ArrayList<File> filesToDelete, ArrayList<Pair<Long, Integer>> ids, ArrayList<String> namesToDelete, boolean forceCache) {
        if (message == null) {
            return false;
        }
        int type = 0;
        long id = 0;
        TLRPC.Document document = MessageObject.getDocument(message);
        TLRPC.Photo photo = MessageObject.getPhoto(message);
        if (MessageObject.isVoiceMessage(message)) {
            if (document == null) {
                return false;
            }
            if (getMediaDataController().ringtoneDataStore.contains(document.id)) {
                return false;
            }
            id = document.id;
            type = DownloadController.AUTODOWNLOAD_TYPE_AUDIO;
        } else if (MessageObject.isStickerMessage(message) || MessageObject.isAnimatedStickerMessage(message)) {
            if (document == null) {
                return false;
            }
            id = document.id;
            type = DownloadController.AUTODOWNLOAD_TYPE_PHOTO;
        } else if (MessageObject.isVideoMessage(message) || MessageObject.isRoundVideoMessage(message) || MessageObject.isGifMessage(message)) {
            if (document == null) {
                return false;
            }
            id = document.id;
            type = DownloadController.AUTODOWNLOAD_TYPE_VIDEO;
        } else if (document != null) {
            if (getMediaDataController().ringtoneDataStore.contains(document.id)) {
                return false;
            }
            id = document.id;
            type = DownloadController.AUTODOWNLOAD_TYPE_DOCUMENT;
        } else if (photo != null) {
            TLRPC.PhotoSize photoSize = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, AndroidUtilities.getPhotoSize());
            if (photoSize != null) {
                id = photo.id;
                type = DownloadController.AUTODOWNLOAD_TYPE_PHOTO;
            }
        }
        if (id != 0) {
            ids.add(new Pair<>(id, type));
        }
        if (photo != null) {
            for (int a = 0, N = photo.sizes.size(); a < N; a++) {
                TLRPC.PhotoSize photoSize = photo.sizes.get(a);
                String name = FileLoader.getAttachFileName(photoSize);
                if (!TextUtils.isEmpty(name)) {
                    namesToDelete.add(name);
                }
                File file = getFileLoader().getPathToAttach(photoSize, forceCache);
                if (file.toString().length() > 0) {
                    filesToDelete.add(file);
                }
            }
            return true;
        } else if (document != null) {
            String name = FileLoader.getAttachFileName(document);
            if (!TextUtils.isEmpty(name)) {
                namesToDelete.add(name);
            }
            File file = getFileLoader().getPathToAttach(document, forceCache);
            if (file.toString().length() > 0) {
                filesToDelete.add(file);
            }
            for (int a = 0, N = document.thumbs.size(); a < N; a++) {
                TLRPC.PhotoSize photoSize = document.thumbs.get(a);
                file = getFileLoader().getPathToAttach(photoSize);
                if (file.toString().length() > 0) {
                    filesToDelete.add(file);
                }
            }
            return true;
        }
        return false;
    }

    public void deleteDialog(long did, int messagesOnly) {
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            SQLiteCursor cursor2 = null;
            SQLitePreparedStatement state5 = null;
            SQLitePreparedStatement state6 = null;
            try {
                if (messagesOnly == 3) {
                    int lastMid = -1;
                    cursor = database.queryFinalized("SELECT last_mid FROM dialogs WHERE did = " + did);
                    if (cursor.next()) {
                        lastMid = cursor.intValue(0);
                    }
                    cursor.dispose();
                    cursor = null;
                    if (lastMid != 0) {
                        return;
                    }
                }
                if (DialogObject.isEncryptedDialog(did) || messagesOnly == 2) {
                    cursor = database.queryFinalized("SELECT data FROM messages_v2 WHERE uid = " + did);
                    ArrayList<File> filesToDelete = new ArrayList<>();
                    ArrayList<String> namesToDelete = new ArrayList<>();
                    ArrayList<Pair<Long, Integer>> idsToDelete = new ArrayList<>();
                    try {
                        while (cursor.next()) {
                            NativeByteBuffer data = cursor.byteBufferValue(0);
                            if (data != null) {
                                TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                                message.readAttachPath(data, getUserConfig().clientUserId);
                                data.reuse();
                                addFilesToDelete(message, filesToDelete, idsToDelete, namesToDelete, false);
                            }
                        }
                    } catch (Exception e) {
                        checkSQLException(e);
                    }
                    cursor.dispose();
                    cursor = null;
                    deleteFromDownloadQueue(idsToDelete, true);
                    AndroidUtilities.runOnUIThread(() -> getFileLoader().cancelLoadFiles(namesToDelete));
                    getFileLoader().deleteFiles(filesToDelete, messagesOnly);
                }

                if (messagesOnly == 0 || messagesOnly == 3) {
                    database.executeFast("DELETE FROM dialogs WHERE did = " + did).stepThis().dispose();
                    database.executeFast("DELETE FROM chat_pinned_v2 WHERE uid = " + did).stepThis().dispose();
                    database.executeFast("DELETE FROM chat_pinned_count WHERE uid = " + did).stepThis().dispose();
                    database.executeFast("DELETE FROM channel_users_v2 WHERE did = " + did).stepThis().dispose();
                    database.executeFast("DELETE FROM search_recent WHERE did = " + did).stepThis().dispose();
                    if (!DialogObject.isEncryptedDialog(did)) {
                        if (DialogObject.isChatDialog(did)) {
                            database.executeFast("DELETE FROM chat_settings_v2 WHERE uid = " + (-did)).stepThis().dispose();
                        }
                    } else {
                        database.executeFast("DELETE FROM enc_chats WHERE uid = " + DialogObject.getEncryptedChatId(did)).stepThis().dispose();
                    }
                } else if (messagesOnly == 2) {
                    cursor = database.queryFinalized("SELECT last_mid_i, last_mid FROM dialogs WHERE did = " + did);
                    int messageId = -1;
                    if (cursor.next()) {
                        long last_mid_i = cursor.longValue(0);
                        long last_mid = cursor.longValue(1);
                        cursor2 = database.queryFinalized("SELECT data FROM messages_v2 WHERE uid = " + did + " AND mid IN (" + last_mid_i + "," + last_mid + ")");
                        try {
                            while (cursor2.next()) {
                                NativeByteBuffer data = cursor2.byteBufferValue(0);
                                if (data != null) {
                                    TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                                    if (message != null) {
                                        message.readAttachPath(data, getUserConfig().clientUserId);
                                    }
                                    data.reuse();
                                    if (message != null) {
                                        messageId = message.id;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            checkSQLException(e);
                        }
                        cursor2.dispose();
                        cursor2 = null;

                        database.executeFast("DELETE FROM messages_v2 WHERE uid = " + did + " AND mid != " + last_mid_i + " AND mid != " + last_mid).stepThis().dispose();
                        database.executeFast("DELETE FROM messages_topics WHERE uid = " + did + " AND mid != " + last_mid_i + " AND mid != " + last_mid).stepThis().dispose();
                        database.executeFast("DELETE FROM messages_holes WHERE uid = " + did).stepThis().dispose();
                        database.executeFast("DELETE FROM bot_keyboard WHERE uid = " + did).stepThis().dispose();
                        database.executeFast("DELETE FROM bot_keyboard_topics WHERE uid = " + did).stepThis().dispose();
                        database.executeFast("DELETE FROM media_counts_v2 WHERE uid = " + did).stepThis().dispose();
                        database.executeFast("DELETE FROM media_v4 WHERE uid = " + did).stepThis().dispose();
                        database.executeFast("DELETE FROM media_holes_v2 WHERE uid = " + did).stepThis().dispose();
                        getMediaDataController().clearBotKeyboard(did);

                        state5 = database.executeFast("REPLACE INTO messages_holes VALUES(?, ?, ?)");
                        state6 = database.executeFast("REPLACE INTO media_holes_v2 VALUES(?, ?, ?, ?)");
                        if (messageId != -1) {
                            createFirstHoles(did, state5, state6, messageId, 0);
                        }
                        state5.dispose();
                        state5 = null;
                        state6.dispose();
                        state6 = null;
                        updateWidgets(did);
                    }
                    cursor.dispose();
                    cursor = null;
                    return;
                }

                database.executeFast("UPDATE dialogs SET unread_count = 0, unread_count_i = 0 WHERE did = " + did).stepThis().dispose();
                database.executeFast("DELETE FROM messages_v2 WHERE uid = " + did).stepThis().dispose();
                database.executeFast("DELETE FROM messages_topics WHERE uid = " + did).stepThis().dispose();
                database.executeFast("DELETE FROM bot_keyboard WHERE uid = " + did).stepThis().dispose();
                database.executeFast("DELETE FROM bot_keyboard_topics WHERE uid = " + did).stepThis().dispose();
                database.executeFast("DELETE FROM media_counts_v2 WHERE uid = " + did).stepThis().dispose();
                database.executeFast("DELETE FROM media_v4 WHERE uid = " + did).stepThis().dispose();
                database.executeFast("DELETE FROM messages_holes WHERE uid = " + did).stepThis().dispose();
                database.executeFast("DELETE FROM media_holes_v2 WHERE uid = " + did).stepThis().dispose();
                getMediaDataController().clearBotKeyboard(did);
                AndroidUtilities.runOnUIThread(() -> getNotificationCenter().postNotificationName(NotificationCenter.needReloadRecentDialogsSearch));
                resetAllUnreadCounters(false);
                updateWidgets(did);
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
                if (cursor2 != null) {
                    cursor2.dispose();
                }
                if (state5 != null) {
                    state5.dispose();
                }
                if (state6 != null) {
                    state6.dispose();
                }
            }
        });
    }

    public void onDeleteQueryComplete(long did) {
        storageQueue.postRunnable(() -> {
            try {
                database.executeFast("DELETE FROM media_counts_v2 WHERE uid = " + did).stepThis().dispose();
            } catch (Exception e) {
                checkSQLException(e);
            }
        });
    }

    public void clearUserPhotos(long dialogId) {
        storageQueue.postRunnable(() -> {
            try {
                database.executeFast("DELETE FROM dialog_photos WHERE uid = " + dialogId).stepThis().dispose();
                database.executeFast("DELETE FROM dialog_photos_count WHERE uid = " + dialogId).stepThis().dispose();
            } catch (Exception e) {
                checkSQLException(e);
            }
        });
    }

    public void clearUserPhoto(long dialogId, long pid) {
        storageQueue.postRunnable(() -> {
            try {
                database.executeFast("DELETE FROM dialog_photos WHERE uid = " + dialogId + " AND id = " + pid).stepThis().dispose();
                database.executeFast("UPDATE dialog_photos_count SET count = count - 1 WHERE uid = " + dialogId + " AND count > 0").stepThis().dispose();
            } catch (Exception e) {
                checkSQLException(e);
            }
        });
    }

    public void resetDialogs(TLRPC.messages_Dialogs dialogsRes, int messagesCount, int seq, int newPts, int date, int qts, LongSparseArray<TLRPC.Dialog> new_dialogs_dict, LongSparseArray<ArrayList<MessageObject>> new_dialogMessage, TLRPC.Message lastMessage, int dialogsCount) {
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            try {
                int maxPinnedNum = 0;

                ArrayList<Long> dids = new ArrayList<>();

                int totalPinnedCount = dialogsRes.dialogs.size() - dialogsCount;
                LongSparseIntArray oldPinnedDialogNums = new LongSparseIntArray();
                ArrayList<Long> oldPinnedOrder = new ArrayList<>();
                ArrayList<Long> orderArrayList = new ArrayList<>();

                for (int a = dialogsCount; a < dialogsRes.dialogs.size(); a++) {
                    TLRPC.Dialog dialog = dialogsRes.dialogs.get(a);
                    orderArrayList.add(dialog.id);
                }

                cursor = database.queryFinalized("SELECT did, pinned FROM dialogs WHERE 1");
                while (cursor.next()) {
                    long did = cursor.longValue(0);
                    int pinnedNum = cursor.intValue(1);
                    if (!DialogObject.isEncryptedDialog(did)) {
                        dids.add(did);
                        if (pinnedNum > 0) {
                            maxPinnedNum = Math.max(pinnedNum, maxPinnedNum);
                            oldPinnedDialogNums.put(did, pinnedNum);
                            oldPinnedOrder.add(did);
                        }
                    }
                }
                Collections.sort(oldPinnedOrder, (o1, o2) -> {
                    int val1 = oldPinnedDialogNums.get(o1);
                    int val2 = oldPinnedDialogNums.get(o2);
                    if (val1 < val2) {
                        return 1;
                    } else if (val1 > val2) {
                        return -1;
                    }
                    return 0;
                });
                while (oldPinnedOrder.size() < totalPinnedCount) {
                    oldPinnedOrder.add(0, 0L);
                }
                cursor.dispose();
                cursor = null;
                String ids = "(" + TextUtils.join(",", dids) + ")";

                database.beginTransaction();
                database.executeFast("DELETE FROM chat_pinned_count WHERE uid IN " + ids).stepThis().dispose();
                database.executeFast("DELETE FROM chat_pinned_v2 WHERE uid IN " + ids).stepThis().dispose();
                database.executeFast("DELETE FROM dialogs WHERE did IN " + ids).stepThis().dispose();
                database.executeFast("DELETE FROM messages_v2 WHERE uid IN " + ids).stepThis().dispose();
                database.executeFast("DELETE FROM polls_v2 WHERE 1").stepThis().dispose();
                database.executeFast("DELETE FROM bot_keyboard WHERE uid IN " + ids).stepThis().dispose();
                database.executeFast("DELETE FROM bot_keyboard_topics WHERE uid IN " + ids).stepThis().dispose();
                database.executeFast("DELETE FROM media_v4 WHERE uid IN " + ids).stepThis().dispose();
                database.executeFast("DELETE FROM messages_holes WHERE uid IN " + ids).stepThis().dispose();
                database.executeFast("DELETE FROM media_holes_v2 WHERE uid IN " + ids).stepThis().dispose();
                database.commitTransaction();

                for (int a = 0; a < totalPinnedCount; a++) {
                    TLRPC.Dialog dialog = dialogsRes.dialogs.get(dialogsCount + a);
                    if (dialog instanceof TLRPC.TL_dialog && !dialog.pinned) {
                        continue;
                    }
                    int oldIdx = oldPinnedOrder.indexOf(dialog.id);
                    int newIdx = orderArrayList.indexOf(dialog.id);
                    if (oldIdx != -1 && newIdx != -1) {
                        if (oldIdx == newIdx) {
                            int oldNum = oldPinnedDialogNums.get(dialog.id, -1);
                            if (oldNum != -1) {
                                dialog.pinnedNum = oldNum;
                            }
                        } else {
                            long oldDid = oldPinnedOrder.get(newIdx);
                            int oldNum = oldPinnedDialogNums.get(oldDid, -1);
                            if (oldNum != -1) {
                                dialog.pinnedNum = oldNum;
                            }
                        }
                    }
                    if (dialog.pinnedNum == 0) {
                        dialog.pinnedNum = (totalPinnedCount - a) + maxPinnedNum;
                    }
                }

                putDialogsInternal(dialogsRes, 0);
                saveDiffParamsInternal(seq, newPts, date, qts);

                int totalDialogsLoadCount = getUserConfig().getTotalDialogsCount(0);
                int dialogsLoadOffsetId;
                int dialogsLoadOffsetDate;
                long dialogsLoadOffsetChannelId = 0;
                long dialogsLoadOffsetChatId = 0;
                long dialogsLoadOffsetUserId = 0;
                long dialogsLoadOffsetAccess = 0;

                totalDialogsLoadCount += dialogsRes.dialogs.size();
                dialogsLoadOffsetId = lastMessage.id;
                dialogsLoadOffsetDate = lastMessage.date;
                if (lastMessage.peer_id.channel_id != 0) {
                    dialogsLoadOffsetChannelId = lastMessage.peer_id.channel_id;
                    dialogsLoadOffsetChatId = 0;
                    dialogsLoadOffsetUserId = 0;
                    for (int a = 0; a < dialogsRes.chats.size(); a++) {
                        TLRPC.Chat chat = dialogsRes.chats.get(a);
                        if (chat.id == dialogsLoadOffsetChannelId) {
                            dialogsLoadOffsetAccess = chat.access_hash;
                            break;
                        }
                    }
                } else if (lastMessage.peer_id.chat_id != 0) {
                    dialogsLoadOffsetChatId = lastMessage.peer_id.chat_id;
                    dialogsLoadOffsetChannelId = 0;
                    dialogsLoadOffsetUserId = 0;
                    for (int a = 0; a < dialogsRes.chats.size(); a++) {
                        TLRPC.Chat chat = dialogsRes.chats.get(a);
                        if (chat.id == dialogsLoadOffsetChatId) {
                            dialogsLoadOffsetAccess = chat.access_hash;
                            break;
                        }
                    }
                } else if (lastMessage.peer_id.user_id != 0) {
                    dialogsLoadOffsetUserId = lastMessage.peer_id.user_id;
                    dialogsLoadOffsetChatId = 0;
                    dialogsLoadOffsetChannelId = 0;
                    for (int a = 0; a < dialogsRes.users.size(); a++) {
                        TLRPC.User user = dialogsRes.users.get(a);
                        if (user.id == dialogsLoadOffsetUserId) {
                            dialogsLoadOffsetAccess = user.access_hash;
                            break;
                        }
                    }
                }
                for (int a = 0; a < 2; a++) {
                    getUserConfig().setDialogsLoadOffset(a,
                            dialogsLoadOffsetId,
                            dialogsLoadOffsetDate,
                            dialogsLoadOffsetUserId,
                            dialogsLoadOffsetChatId,
                            dialogsLoadOffsetChannelId,
                            dialogsLoadOffsetAccess);
                    getUserConfig().setTotalDialogsCount(a, totalDialogsLoadCount);
                }
                getUserConfig().draftsLoaded = false;
                getUserConfig().saveConfig(false);
                getMessagesController().completeDialogsReset(dialogsRes, messagesCount, seq, newPts, date, qts, new_dialogs_dict, new_dialogMessage, lastMessage);
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (database != null) {
                    database.commitTransaction();
                }
                if (cursor != null) {
                    cursor.dispose();
                }
            }
        });
    }

    public void emptyMessagesMedia(long dialogId, ArrayList<Integer> mids) {
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            SQLitePreparedStatement state = null;
            SQLitePreparedStatement state_saved = null;
            try {
                ArrayList<File> filesToDelete = new ArrayList<>();
                ArrayList<String> namesToDelete = new ArrayList<>();
                ArrayList<Pair<Long, Integer>> idsToDelete = new ArrayList<>();
                ArrayList<TLRPC.Message> messages = new ArrayList<>();
                ArrayList<TLRPC.Message> changedSavedMessages = null;
                cursor = database.queryFinalized(String.format(Locale.US, "SELECT data, mid, date, uid, custom_params FROM messages_v2 WHERE mid IN (%s) AND uid = %d", TextUtils.join(",", mids), dialogId));
                while (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(0);
                    if (data != null) {
                        TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                        message.readAttachPath(data, getUserConfig().clientUserId);
                        data.reuse();
                        if (message.media != null) {
                            if (!addFilesToDelete(message, filesToDelete, idsToDelete, namesToDelete, true)) {
                                continue;
                            } else {
                                if (message.media.document != null) {
                                    message.media.document = new TLRPC.TL_documentEmpty();
                                } else if (message.media.photo != null) {
                                    message.media.photo = new TLRPC.TL_photoEmpty();
                                }
                            }
                            message.media.flags = message.media.flags & ~1;
                            message.id = cursor.intValue(1);
                            message.date = cursor.intValue(2);
                            message.dialog_id = cursor.longValue(3);
                            NativeByteBuffer customParams = cursor.byteBufferValue(4);
                            if (customParams != null) {
                                MessageCustomParamsHelper.readLocalParams(message, customParams);
                                customParams.reuse();
                            }
                            messages.add(message);
                        }
                    }
                }
                cursor.dispose();
                cursor = null;
                deleteFromDownloadQueue(idsToDelete, true);
                if (!messages.isEmpty()) {
                    state = database.executeFast("REPLACE INTO messages_v2 VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?)");
                    for (int a = 0; a < messages.size(); a++) {
                        TLRPC.Message message = messages.get(a);

                        MessageObject.normalizeFlags(message);
                        NativeByteBuffer data = new NativeByteBuffer(message.getObjectSize());
                        message.serializeToStream(data);

                        state.requery();
                        state.bindInteger(1, message.id);
                        state.bindLong(2, message.dialog_id);
                        state.bindInteger(3, MessageObject.getUnreadFlags(message));
                        state.bindInteger(4, message.send_state);
                        state.bindInteger(5, message.date);
                        state.bindByteBuffer(6, data);
                        state.bindInteger(7, (MessageObject.isOut(message) || message.from_scheduled ? 1 : 0));
                        state.bindInteger(8, message.ttl);
                        if ((message.flags & TLRPC.MESSAGE_FLAG_HAS_VIEWS) != 0) {
                            state.bindInteger(9, message.views);
                        } else {
                            state.bindInteger(9, getMessageMediaType(message));
                        }
                        NativeByteBuffer storyData = null;
                        if (message.replyStory != null) {
                            storyData = new NativeByteBuffer(message.replyStory.getObjectSize());
                            message.replyStory.serializeToStream(storyData);
                            state.bindByteBuffer(10, storyData);
                        } else {
                            state.bindNull(10);
                        }
                        int flags = 0;
                        if (message.stickerVerified == 0) {
                            flags |= 1;
                        } else if (message.stickerVerified == 2) {
                            flags |= 2;
                        }
                        state.bindInteger(11, flags);
                        state.bindInteger(12, message.mentioned ? 1 : 0);
                        state.bindInteger(13, message.forwards);
                        NativeByteBuffer repliesData = null;
                        if (message.replies != null) {
                            repliesData = new NativeByteBuffer(message.replies.getObjectSize());
                            message.replies.serializeToStream(repliesData);
                            state.bindByteBuffer(14, repliesData);
                        } else {
                            state.bindNull(14);
                        }
                        if (message.reply_to != null) {
                            state.bindInteger(15, message.reply_to.reply_to_top_id != 0 ? message.reply_to.reply_to_top_id : message.reply_to.reply_to_msg_id);
                        } else {
                            state.bindInteger(15, 0);
                        }
                        state.bindLong(16, MessageObject.getChannelId(message));
                        NativeByteBuffer customParams = MessageCustomParamsHelper.writeLocalParams(message);
                        if (customParams != null) {
                            state.bindByteBuffer(16, customParams);
                        } else {
                            state.bindNull(17);
                        }
                        if ((message.flags & 131072) != 0) {
                            state.bindLong(18, message.grouped_id);
                        } else {
                            state.bindNull(18);
                        }
                        if (message.reply_to != null) {
                            state.bindInteger(19, message.reply_to.story_id);
                        } else {
                            state.bindInteger(19, 0);
                        }
                        state.step();
                        data.reuse();
                        if (repliesData != null) {
                            repliesData.reuse();
                        }
                        if (customParams != null) {
                            customParams.reuse();
                        }
                        if (storyData != null) {
                            storyData.reuse();
                        }
                    }
                    if (state != null) {
                        state.dispose();
                        state = null;
                    }
                    AndroidUtilities.runOnUIThread(() -> {
                        for (int a = 0; a < messages.size(); a++) {
                            getNotificationCenter().postNotificationName(NotificationCenter.updateMessageMedia, messages.get(a));
                        }
                    });
                }
                AndroidUtilities.runOnUIThread(() -> getFileLoader().cancelLoadFiles(namesToDelete));
                getFileLoader().deleteFiles(filesToDelete, 0);
                if (changedSavedMessages != null) {
                    final ArrayList<TLRPC.Message> finalChangedSavedMessages = changedSavedMessages;
                    AndroidUtilities.runOnUIThread(() -> {
                        if (getMessagesController().getSavedMessagesController().updateSavedDialogs(finalChangedSavedMessages)) {
                            getMessagesController().getSavedMessagesController().update();
                        }
                    });
                }
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
                if (state != null) {
                    state.dispose();
                }
            }
        });
    }

    public void toggleTodo(long dialogId, int messageId, int taskId, boolean enable, long send_as) {
        final long myself = getUserConfig().getClientUserId();
        final int date = getConnectionsManager().getCurrentTime();
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            SQLitePreparedStatement state = null;
            try {
                cursor = database.queryFinalized("SELECT data FROM messages_v2 WHERE uid = " + dialogId + " AND mid = " + messageId);
                if (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(0);
                    if (data != null) {
                        final TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), true);
                        message.readAttachPath(data, myself);
                        data.reuse();
                        cursor.dispose();
                        cursor = null;

                        if (message.media instanceof TLRPC.TL_messageMediaToDo) {
                            final TLRPC.TL_messageMediaToDo mediaTodo = (TLRPC.TL_messageMediaToDo) message.media;
                            MessageObject.toggleTodo(currentAccount, send_as, mediaTodo, taskId, enable, date);

                            state = database.executeFast("UPDATE messages_v2 SET data = ? WHERE mid = ? AND uid = ?");
                            state.requery();
                            data = new NativeByteBuffer(message.getObjectSize());
                            MessageObject.normalizeFlags(message);
                            message.serializeToStream(data);
                            state.bindByteBuffer(1, data);
                            state.bindInteger(2, messageId);
                            state.bindLong(3, dialogId);
                            state.step();
                            state.dispose();
                            state = null;
                            data.reuse();
                        }
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                    cursor = null;
                }
                if (state != null) {
                    state.dispose();
                    state = null;
                }
            }
            if (isForum(dialogId, FORUM_TYPE_DIRECT | FORUM_TYPE_CHAT)) {
                try {
                    cursor = database.queryFinalized("SELECT data FROM messages_topics WHERE uid = " + dialogId + " AND mid = " + messageId);
                    if (cursor.next()) {
                        NativeByteBuffer data = cursor.byteBufferValue(0);
                        if (data != null) {
                            final TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                            message.readAttachPath(data, myself);
                            data.reuse();

                            if (message.media instanceof TLRPC.TL_messageMediaToDo) {
                                final TLRPC.TL_messageMediaToDo mediaTodo = (TLRPC.TL_messageMediaToDo) message.media;
                                MessageObject.toggleTodo(currentAccount, send_as, mediaTodo, taskId, enable, date);
                                cursor.dispose();
                                cursor = null;

                                state = database.executeFast("UPDATE messages_topics SET data = ? WHERE mid = ? AND uid = ?");
                                state.requery();
                                data = new NativeByteBuffer(message.getObjectSize());
                                message.serializeToStream(data);
                                state.bindByteBuffer(1, data);
                                state.bindInteger(2, messageId);
                                state.bindLong(3, dialogId);
                                state.step();
                                state.dispose();
                                state = null;
                                data.reuse();
                            }
                        }
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                } finally {
                    if (cursor != null) {
                        cursor.dispose();
                        cursor = null;
                    }
                    if (state != null) {
                        state.dispose();
                        state = null;
                    }
                }
            }
        });
    }

    public void updateMessagePollResults(long pollId, TLRPC.Poll poll, TLRPC.PollResults results) {
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            try {
                LongSparseArray<ArrayList<Integer>> dialogs = null;
                cursor = database.queryFinalized(String.format(Locale.US, "SELECT uid, mid FROM polls_v2 WHERE id = %d", pollId));
                while (cursor.next()) {
                    long dialogId = cursor.longValue(0);
                    if (dialogs == null) {
                        dialogs = new LongSparseArray<>();
                    }
                    ArrayList<Integer> mids = dialogs.get(dialogId);
                    if (mids == null) {
                        mids = new ArrayList<>();
                        dialogs.put(dialogId, mids);
                    }
                    mids.add(cursor.intValue(1));
                }
                cursor.dispose();
                cursor = null;
                if (dialogs != null) {
                    database.beginTransaction();
                    SQLitePreparedStatement state = database.executeFast("UPDATE messages_v2 SET data = ? WHERE mid = ? AND uid = ?");
                    SQLitePreparedStatement state_topics = database.executeFast("UPDATE messages_topics SET data = ? WHERE mid = ? AND uid = ?");
                    for (int b = 0, N2 = dialogs.size(); b < N2; b++) {
                        long dialogId = dialogs.keyAt(b);
                        ArrayList<Integer> mids = dialogs.valueAt(b);
                        for (int a = 0, N = mids.size(); a < N; a++) {
                            Integer mid = mids.get(a);
                            boolean foundMessage = false;
                            for (int k = 0; k < 2; k++) {
                                boolean isTopic = k == 1;
                                SQLitePreparedStatement currentState;
                                if (isTopic) {
                                    cursor = database.queryFinalized(String.format(Locale.US, "SELECT data FROM messages_topics WHERE mid = %d AND uid = %d", mid, dialogId));
                                    currentState = state_topics;
                                } else {
                                    cursor = database.queryFinalized(String.format(Locale.US, "SELECT data FROM messages_v2 WHERE mid = %d AND uid = %d", mid, dialogId));
                                    currentState = state;
                                }
                                if (cursor.next()) {
                                    NativeByteBuffer data = cursor.byteBufferValue(0);
                                    if (data != null) {
                                        TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                                        message.readAttachPath(data, getUserConfig().clientUserId);
                                        data.reuse();
                                        if (message.media instanceof TLRPC.TL_messageMediaPoll) {
                                            TLRPC.TL_messageMediaPoll media = (TLRPC.TL_messageMediaPoll) message.media;
                                            if (poll != null) {
                                                media.poll = poll;
                                            }
                                            if (results != null) {
                                                MessageObject.updatePollResults(media, results);
                                            }

                                            MessageObject.normalizeFlags(message);
                                            data = new NativeByteBuffer(message.getObjectSize());
                                            message.serializeToStream(data);
                                            currentState.requery();
                                            currentState.bindByteBuffer(1, data);
                                            currentState.bindInteger(2, mid);
                                            currentState.bindLong(3, dialogId);
                                            currentState.step();
                                            data.reuse();
                                        }
                                    }
                                    foundMessage = true;
                                }
                                cursor.dispose();
                            }
                            if (!foundMessage) {
                                database.executeFast(String.format(Locale.US, "DELETE FROM polls_v2 WHERE mid = %d AND uid = %d", mid, dialogId)).stepThis().dispose();
                            }
                        }
                    }
                    state.dispose();
                    state_topics.dispose();

                    SQLitePreparedStatement state_media = database.executeFast("UPDATE media_v4 SET data = ? WHERE mid = ? AND uid = ?");
                    SQLitePreparedStatement state_media_topics = database.executeFast("UPDATE media_topics SET data = ? WHERE mid = ? AND uid = ? AND topic_id = ?");
                    for (int b = 0, N2 = dialogs.size(); b < N2; b++) {
                        long dialogId = dialogs.keyAt(b);
                        ArrayList<Integer> mids = dialogs.valueAt(b);
                        for (int a = 0, N = mids.size(); a < N; a++) {
                            Integer mid = mids.get(a);
                            for (int k = 0; k < 1; k++) {
                                boolean isTopic = k == 1;
                                SQLitePreparedStatement currentState;
                                if (isTopic) {
                                    cursor = database.queryFinalized(String.format(Locale.US, "SELECT data, topic_id FROM media_topics WHERE mid = %d AND uid = %d", mid, dialogId));
                                    currentState = state_media_topics;
                                } else {
                                    cursor = database.queryFinalized(String.format(Locale.US, "SELECT data FROM media_v4 WHERE mid = %d AND uid = %d", mid, dialogId));
                                    currentState = state_media;
                                }
                                if (cursor.next()) {
                                    NativeByteBuffer data = cursor.byteBufferValue(0);
                                    long topicId = 0;
                                    if (isTopic) {
                                        topicId = cursor.longValue(1);
                                    }
                                    if (data != null) {
                                        TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                                        message.readAttachPath(data, getUserConfig().clientUserId);
                                        data.reuse();
                                        if (message.media instanceof TLRPC.TL_messageMediaPoll) {
                                            TLRPC.TL_messageMediaPoll media = (TLRPC.TL_messageMediaPoll) message.media;
                                            if (poll != null) {
                                                media.poll = poll;
                                            }
                                            if (results != null) {
                                                MessageObject.updatePollResults(media, results);
                                            }

                                            MessageObject.normalizeFlags(message);
                                            data = new NativeByteBuffer(message.getObjectSize());
                                            message.serializeToStream(data);
                                            currentState.requery();
                                            currentState.bindByteBuffer(1, data);
                                            currentState.bindInteger(2, mid);
                                            currentState.bindLong(3, dialogId);
                                            if (isTopic) {
                                                currentState.bindLong(4, topicId);
                                            }
                                            currentState.step();
                                            data.reuse();
                                        }
                                    }
                                }
                                cursor.dispose();
                            }
                        }
                    }
                    state_media.dispose();
                    state_media_topics.dispose();

                    database.commitTransaction();
                }
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
        });
    }

    public void searchSavedByTag(TLRPC.Reaction tag, long topic_id, String query, int limit, int offset, Utilities.Callback4<ArrayList<MessageObject>, ArrayList<TLRPC.User>, ArrayList<TLRPC.Chat>, ArrayList<TLRPC.Document>> done, boolean includeGroups) {
        if (done == null) {
            return;
        }
        storageQueue.postRunnable(() -> {
            SQLitePreparedStatement state = null;
            SQLiteCursor cursor = null;
            SQLiteCursor cursor_groups = null;
            try {
                final long selfId = getUserConfig().getClientUserId();
                state = database.executeFast("SELECT m.data, m.replydata, m.group_id FROM messages_v2 m INNER JOIN tag_message_id t ON m.mid = t.mid WHERE m.uid = ? AND t.tag = ?" + (!TextUtils.isEmpty(query) ? " AND t.text LIKE '%' || ? || '%'" : "") + (topic_id != 0 ? " AND topic_id = ? "  : "") + " ORDER BY m.mid DESC LIMIT ? OFFSET ?");

                ArrayList<TLRPC.User> users = new ArrayList<>();
//                ArrayList<TLRPC.User> encUsers = new ArrayList<>();
                ArrayList<TLRPC.Chat> chats = new ArrayList<>();
                ArrayList<Long> animatedEmojiToLoad = new ArrayList<>();
                ArrayList<Long> usersToLoad = new ArrayList<>();
                ArrayList<Long> chatsToLoad = new ArrayList<>();
                ArrayList<TLRPC.Document> animatedEmoji = new ArrayList<>();
//                LongSparseArray<SparseArray<ArrayList<TLRPC.Message>>> replyMessageOwners = new LongSparseArray<>();
//                LongSparseArray<ArrayList<Integer>> dialogReplyMessagesIds = new LongSparseArray<>();

                int pointer = 1;
                state.bindLong(pointer++, selfId);
                long hash = 0;
                if (tag instanceof TLRPC.TL_reactionEmoji) {
                    hash = ((TLRPC.TL_reactionEmoji) tag).emoticon.hashCode();
                } else if (tag instanceof TLRPC.TL_reactionCustomEmoji) {
                    hash = ((TLRPC.TL_reactionCustomEmoji) tag).document_id;
                }
                state.bindLong(pointer++, hash);
                if (!TextUtils.isEmpty(query)) {
                    String q = LocaleController.getInstance().getTranslitString(query);
                    if (q == null) q = "";
                    state.bindString(pointer++, q);
                }
                if (topic_id != 0) {
                    state.bindLong(pointer++, topic_id);
                }
                state.bindInteger(pointer++, limit);
                state.bindInteger(pointer++, offset);

                cursor = state.query(new Object[] {});
                state = null;

                ArrayList<MessageObject> messageObjects = new ArrayList<>();
                while (cursor.next()) {
                    long group_id = cursor.longValue(2);
                    if (group_id != 0 && includeGroups) {
                        cursor_groups = database.queryFinalized("SELECT data, replydata, group_id FROM messages_v2 WHERE uid = ? AND group_id = ? ORDER BY mid DESC", selfId, group_id);
                        ArrayList<MessageObject> groupmessages = new ArrayList<>();
                        while (cursor_groups.next()) {
                            NativeByteBuffer data = cursor_groups.byteBufferValue(0);
                            TLRPC.Message groupmessage = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                            groupmessage.readAttachPath(data, selfId);
                            data.reuse();
                            addUsersAndChatsFromMessage(groupmessage, usersToLoad, chatsToLoad, animatedEmojiToLoad);
                            MessageObject messageObject = new MessageObject(currentAccount, groupmessage, null, null, null, null, null, true, true, 0, false, false, true);
                            if (groupmessage.reactions != null) {
                                messageObject.isPrimaryGroupMessage = true;
                            }
                            groupmessages.add(messageObject);
                        }
                        cursor_groups.dispose();
                        messageObjects.addAll(groupmessages);
                    } else {
                        NativeByteBuffer data = cursor.byteBufferValue(0);
                        if (data == null) continue;
                        TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                        if (message != null) {
                            message.readAttachPath(data, selfId);
                            data.reuse();
                            addUsersAndChatsFromMessage(message, usersToLoad, chatsToLoad, animatedEmojiToLoad);
                            if (message.reply_to != null && (message.reply_to.reply_to_msg_id != 0 || message.reply_to.reply_to_random_id != 0)) {
                                if (!cursor.isNull(1)) {
                                    data = cursor.byteBufferValue(1);
                                    if (data != null) {
                                        message.replyMessage = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                                        message.replyMessage.readAttachPath(data, selfId);
                                        data.reuse();
                                        if (message.replyMessage != null) {
                                            addUsersAndChatsFromMessage(message.replyMessage, usersToLoad, chatsToLoad, animatedEmojiToLoad);
                                        }
                                    }
                                }
                            }
                            MessageObject messageObject = new MessageObject(currentAccount, message, null, null, null, null, null, true, true, 0, false, false, true);
                            messageObjects.add(messageObject);
                        }
                    }
                }
                cursor.dispose();

//                loadReplyMessages(replyMessageOwners, dialogReplyMessagesIds, usersToLoad, chatsToLoad, false);

                if (!usersToLoad.isEmpty()) {
                    getUsersInternal(usersToLoad, users);
                }
                if (!chatsToLoad.isEmpty()) {
                    getChatsInternal(TextUtils.join(",", chatsToLoad), chats);
                }
                if (!animatedEmojiToLoad.isEmpty()) {
                    getAnimatedEmoji(TextUtils.join(",", animatedEmojiToLoad), animatedEmoji);
                }

                AndroidUtilities.runOnUIThread(() -> {
                    done.run(messageObjects, users, chats, animatedEmoji);
                });

            } catch (Exception e) {
                FileLog.e(e);
            } finally {
                if (state != null) {
                    state.dispose();
                }
                if (cursor != null) {
                    cursor.dispose();
                }
                if (cursor_groups != null) {
                    cursor_groups.dispose();
                }
            }
        });
    }

    public void updateMessageReactions(long dialogId, int msgId, TLRPC.TL_messageReactions reactions) {
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            try {
                final long selfId = getUserConfig().getClientUserId();
                TLRPC.TL_messageReactions pastReactions = null;
                long topicId = 0;
                database.beginTransaction();
                for (int i = 0; i < 2; i++) {
                    if (i == 0) {
                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT data FROM messages_v2 WHERE mid = %d AND uid = %d", msgId, dialogId));
                    } else {
                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT data FROM messages_topics WHERE mid = %d AND uid = %d", msgId, dialogId));
                    }
                    if (cursor.next()) {
                        NativeByteBuffer data = cursor.byteBufferValue(0);
                        if (data != null) {
                            TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                            if (message != null) {
                                message.readAttachPath(data, getUserConfig().clientUserId);
                                data.reuse();
                                if (pastReactions == null) {
                                    pastReactions = message.reactions;
                                    topicId = MessageObject.getSavedDialogId(selfId, message);
                                }
                                MessageObject.updateReactions(message, reactions);
                                SQLitePreparedStatement state;
                                if (i == 0) {
                                    state = database.executeFast("UPDATE messages_v2 SET data = ? WHERE mid = ? AND uid = ?");
                                } else {
                                    state = database.executeFast("UPDATE messages_topics SET data = ? WHERE mid = ? AND uid = ?");
                                }
                                MessageObject.normalizeFlags(message);
                                NativeByteBuffer data2 = new NativeByteBuffer(message.getObjectSize());
                                message.serializeToStream(data2);
                                state.requery();
                                state.bindByteBuffer(1, data2);
                                state.bindInteger(2, msgId);
                                state.bindLong(3, dialogId);
                                state.step();
                                data2.reuse();
                                state.dispose();
                                if (selfId == dialogId) {
                                    database.executeFast(String.format(Locale.US, "DELETE FROM tag_message_id WHERE mid = %d", message.id)).stepThis().dispose();
                                    SQLitePreparedStatement state_tag_message = database.executeFast("REPLACE INTO tag_message_id VALUES(?, ?, ?, ?)");
                                    bindMessageTags(state_tag_message, message);
                                    state_tag_message.dispose();
                                }
                            } else {
                                data.reuse();
                            }
                        }
                    }
                    cursor.dispose();
                    cursor = null;
                }
                database.commitTransaction();
                if (dialogId == selfId && pastReactions != null) {
                    onReactionsUpdate(topicId, pastReactions, reactions);
                }
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (database != null) {
                    database.commitTransaction();
                }
                if (cursor != null) {
                    cursor.dispose();
                }
            }
        });
    }

    private class SavedReactionsUpdate {
        long topic_id;
        TLRPC.TL_messageReactions old;
        TLRPC.TL_messageReactions last;
        public SavedReactionsUpdate(long selfId, TLRPC.Message oldMessage, TLRPC.Message newMessage) {
            topic_id = MessageObject.getSavedDialogId(selfId, newMessage);
            old = oldMessage.reactions;
            last = newMessage.reactions;
        }
    }

    private void onReactionsUpdate(ArrayList<SavedReactionsUpdate> reactionUpdates) {
        if (reactionUpdates == null || reactionUpdates.isEmpty()) return;
        AndroidUtilities.runOnUIThread(() -> {
            boolean updated = false;
            HashSet<Long> topicIds = new HashSet<>();
            LongSparseArray<ReactionsLayoutInBubble.VisibleReaction> oldTags = new LongSparseArray<>();
            LongSparseArray<ReactionsLayoutInBubble.VisibleReaction> newTags = new LongSparseArray<>();
            for (int i = 0; i < reactionUpdates.size(); ++i) {
                SavedReactionsUpdate pair = reactionUpdates.get(i);
                TLRPC.TL_messageReactions a = pair.old;
                TLRPC.TL_messageReactions b = pair.last;

                oldTags.clear();
                newTags.clear();

                if (a != null && a.results != null && a.reactions_as_tags) {
                    for (int j = 0; j < a.results.size(); ++j) {
                        ReactionsLayoutInBubble.VisibleReaction reaction = ReactionsLayoutInBubble.VisibleReaction.fromTL(a.results.get(j).reaction);
                        if (reaction != null) {
                            oldTags.put(reaction.hash, reaction);
                        }
                    }
                }
                if (b != null && b.results != null && b.reactions_as_tags) {
                    for (int j = 0; j < b.results.size(); ++j) {
                        ReactionsLayoutInBubble.VisibleReaction reaction = ReactionsLayoutInBubble.VisibleReaction.fromTL(b.results.get(j).reaction);
                        if (reaction != null) {
                            newTags.put(reaction.hash, reaction);
                        }
                    }
                }
                // delete reactions
                for (int j = 0; j < oldTags.size(); ++j) {
                    long hash = oldTags.keyAt(j);
                    ReactionsLayoutInBubble.VisibleReaction reaction = oldTags.valueAt(j);
                    if (!newTags.containsKey(hash)) {
                        if (getMessagesController().updateSavedReactionTags(pair.topic_id, reaction, false, false)) {
                            updated = true;
                            topicIds.add(pair.topic_id);
                        }
                    }
                }
                // add new reactions
                for (int j = 0; j < newTags.size(); ++j) {
                    long hash = newTags.keyAt(j);
                    ReactionsLayoutInBubble.VisibleReaction reaction = newTags.valueAt(j);
                    if (!oldTags.containsKey(hash)) {
                        if (getMessagesController().updateSavedReactionTags(pair.topic_id, reaction, true, false)) {
                            updated = true;
                            topicIds.add(pair.topic_id);
                        }
                    }
                }
            }
            if (updated && !topicIds.isEmpty()) {
                getMessagesController().updateSavedReactionTags(topicIds);
            }
        });
    }

    private void onReactionsUpdate(long topic_id, TLRPC.TL_messageReactions a, TLRPC.TL_messageReactions b) {
        if (a == null || a.results == null || a != null && a.results != null && a.results.isEmpty() && b != null && b.results.isEmpty()) {
            return;
        }
        AndroidUtilities.runOnUIThread(() -> {
            LongSparseArray<ReactionsLayoutInBubble.VisibleReaction> oldTags = new LongSparseArray<>();
            LongSparseArray<ReactionsLayoutInBubble.VisibleReaction> newTags = new LongSparseArray<>();
            if (a != null && a.results != null && a.reactions_as_tags) {
                for (int i = 0; i < a.results.size(); ++i) {
                    ReactionsLayoutInBubble.VisibleReaction reaction = ReactionsLayoutInBubble.VisibleReaction.fromTL(a.results.get(i).reaction);
                    oldTags.put(reaction.hash, reaction);
                }
            }
            if (b != null && b.results != null && b.reactions_as_tags) {
                for (int i = 0; i < b.results.size(); ++i) {
                    ReactionsLayoutInBubble.VisibleReaction reaction = ReactionsLayoutInBubble.VisibleReaction.fromTL(b.results.get(i).reaction);
                    newTags.put(reaction.hash, reaction);
                }
            }
            boolean updated = false;
            // delete reactions
            for (int i = 0; i < oldTags.size(); ++i) {
                long hash = oldTags.keyAt(i);
                ReactionsLayoutInBubble.VisibleReaction reaction = oldTags.valueAt(i);
                if (!newTags.containsKey(hash)) {
                    updated = getMessagesController().updateSavedReactionTags(topic_id, reaction, false, false) || updated;
                }
            }
            // add new reactions
            for (int i = 0; i < newTags.size(); ++i) {
                long hash = newTags.keyAt(i);
                ReactionsLayoutInBubble.VisibleReaction reaction = newTags.valueAt(i);
                if (!oldTags.containsKey(hash)) {
                    updated = getMessagesController().updateSavedReactionTags(topic_id, reaction, true, false) || updated;
                }
            }
            if (updated) {
                if (topic_id != 0) {
                    getMessagesController().updateSavedReactionTags(0);
                }
                getMessagesController().updateSavedReactionTags(topic_id);
            }
        });
    }

    private void bindMessageTags(SQLitePreparedStatement state, TLRPC.Message message) throws SQLiteException {
        long selfId = getUserConfig().getClientUserId();
        if (message.reactions != null && message.reactions.reactions_as_tags && message.reactions.results != null && !message.reactions.results.isEmpty()) {
            final String text = LocaleController.getInstance().getTranslitString(message.message == null ? "" : message.message);
            for (TLRPC.ReactionCount result : message.reactions.results) {
                if (result.reaction instanceof TLRPC.TL_reactionEmoji || result.reaction instanceof TLRPC.TL_reactionCustomEmoji) {
                    state.requery();
                    state.bindLong(1, message.id);
                    state.bindLong(2, MessageObject.getSavedDialogId(selfId, message));
                    long hash = 0;
                    if (result.reaction instanceof TLRPC.TL_reactionEmoji) {
                        hash = ((TLRPC.TL_reactionEmoji) result.reaction).emoticon.hashCode();
                    } else if (result.reaction instanceof TLRPC.TL_reactionCustomEmoji) {
                        hash = ((TLRPC.TL_reactionCustomEmoji) result.reaction).document_id;
                    }
                    state.bindLong(3, hash);
                    state.bindString(4, text == null ? "" : text);
                    state.step();
                }
            }
        }
    }

    public void updateMessageVoiceTranscriptionOpen(long dialogId, int msgId, TLRPC.Message saveFromMessage) {
        storageQueue.postRunnable(() -> {
            SQLitePreparedStatement state = null;
            try {
                database.beginTransaction();
                TLRPC.Message message = getMessageWithCustomParamsOnlyInternal(msgId, dialogId);
                message.voiceTranscriptionOpen = saveFromMessage.voiceTranscriptionOpen;
                message.voiceTranscriptionRated = saveFromMessage.voiceTranscriptionRated;
                message.voiceTranscriptionFinal = saveFromMessage.voiceTranscriptionFinal;
                message.voiceTranscriptionForce = saveFromMessage.voiceTranscriptionForce;
                message.voiceTranscriptionId = saveFromMessage.voiceTranscriptionId;

                for (int i = 0; i < 2; i++) {
                    if (i == 0) {
                        state = database.executeFast("UPDATE messages_v2 SET custom_params = ? WHERE mid = ? AND uid = ?");
                    } else {
                        state = database.executeFast("UPDATE messages_topics SET custom_params = ? WHERE mid = ? AND uid = ?");
                    }
                    state.requery();
                    NativeByteBuffer nativeByteBuffer = MessageCustomParamsHelper.writeLocalParams(message);
                    if (nativeByteBuffer != null) {
                        state.bindByteBuffer(1, nativeByteBuffer);
                    } else {
                        state.bindNull(1);
                    }
                    state.bindInteger(2, msgId);
                    state.bindLong(3, dialogId);
                    state.step();
                    state.dispose();
                    state = null;
                    if (nativeByteBuffer != null) {
                        nativeByteBuffer.reuse();
                    }
                }
                database.commitTransaction();
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (database != null) {
                    database.commitTransaction();
                }
                if (state != null) {
                    state.dispose();
                }
            }
        });
    }

    public void updateMessageVoiceTranscription(long dialogId, int messageId, String text, long transcriptionId, boolean isFinal) {
        storageQueue.postRunnable(() -> {
            SQLitePreparedStatement state = null;
            try {
                database.beginTransaction();
                TLRPC.Message message = getMessageWithCustomParamsOnlyInternal(messageId, dialogId);
                message.voiceTranscriptionFinal = isFinal;
                message.voiceTranscriptionId = transcriptionId;
                message.voiceTranscription = text;

                state = database.executeFast("UPDATE messages_v2 SET custom_params = ? WHERE mid = ? AND uid = ?");
                state.requery();
                NativeByteBuffer nativeByteBuffer = MessageCustomParamsHelper.writeLocalParams(message);
                if (nativeByteBuffer != null) {
                    state.bindByteBuffer(1, nativeByteBuffer);
                } else {
                    state.bindNull(1);
                }
                state.bindInteger(2, messageId);
                state.bindLong(3, dialogId);
                state.step();
                state.dispose();
                state = null;
                database.commitTransaction();
                if (nativeByteBuffer != null) {
                    nativeByteBuffer.reuse();
                }
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (database != null) {
                    database.commitTransaction();
                }
                if (state != null) {
                    state.dispose();
                }
            }
        });
    }

    public void updateMessageVoiceTranscription(long dialogId, int messageId, String text, TLRPC.Message saveFromMessage) {
        storageQueue.postRunnable(() -> {
            SQLitePreparedStatement state = null;
            try {
                database.beginTransaction();
                TLRPC.Message message = getMessageWithCustomParamsOnlyInternal(messageId, dialogId);
                message.voiceTranscriptionOpen = saveFromMessage.voiceTranscriptionOpen;
                message.voiceTranscriptionRated = saveFromMessage.voiceTranscriptionRated;
                message.voiceTranscriptionFinal = saveFromMessage.voiceTranscriptionFinal;
                message.voiceTranscriptionForce = saveFromMessage.voiceTranscriptionForce;
                message.voiceTranscriptionId = saveFromMessage.voiceTranscriptionId;
                message.voiceTranscription = text;

                for (int i = 0; i < 2; i++) {
                    if (i == 0) {
                        state = database.executeFast("UPDATE messages_v2 SET custom_params = ? WHERE mid = ? AND uid = ?");
                    } else {
                        state = database.executeFast("UPDATE messages_topics SET custom_params = ? WHERE mid = ? AND uid = ?");
                    }
                    state.requery();
                    NativeByteBuffer nativeByteBuffer = MessageCustomParamsHelper.writeLocalParams(message);
                    if (nativeByteBuffer != null) {
                        state.bindByteBuffer(1, nativeByteBuffer);
                    } else {
                        state.bindNull(1);
                    }
                    state.bindInteger(2, messageId);
                    state.bindLong(3, dialogId);
                    state.step();
                    state.dispose();
                    state = null;
                    database.commitTransaction();
                    if (nativeByteBuffer != null) {
                        nativeByteBuffer.reuse();
                    }
                }
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (database != null) {
                    database.commitTransaction();
                }
                if (state != null) {
                    state.dispose();
                }
            }
        });
    }

    public void updateMessageCustomParams(long dialogId, TLRPC.Message saveFromMessage) {
        storageQueue.postRunnable(() -> {
            SQLitePreparedStatement state = null;
            try {
                database.beginTransaction();
                TLRPC.Message message = getMessageWithCustomParamsOnlyInternal(saveFromMessage.id, dialogId);
                MessageCustomParamsHelper.copyParams(saveFromMessage, message);

                for (int i = 0; i < 2; i++) {
                    if (i == 0) {
                        state = database.executeFast("UPDATE messages_v2 SET custom_params = ? WHERE mid = ? AND uid = ?");
                    } else {
                        state = database.executeFast("UPDATE messages_topics SET custom_params = ? WHERE mid = ? AND uid = ?");
                    }
                    state.requery();
                    NativeByteBuffer nativeByteBuffer = MessageCustomParamsHelper.writeLocalParams(message);
                    if (nativeByteBuffer != null) {
                        state.bindByteBuffer(1, nativeByteBuffer);
                    } else {
                        state.bindNull(1);
                    }
                    state.bindInteger(2, saveFromMessage.id);
                    state.bindLong(3, dialogId);
                    state.step();
                    state.dispose();
                    state = null;
                    if (nativeByteBuffer != null) {
                        nativeByteBuffer.reuse();
                    }
                }
                database.commitTransaction();
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (database != null) {
                    database.commitTransaction();
                }
                if (state != null) {
                    state.dispose();
                }
            }
        });
    }

    public TLRPC.Message getMessageWithCustomParamsOnlyInternal(int messageId, long dialogId) {
        TLRPC.Message message = new TLRPC.TL_message();
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized("SELECT custom_params FROM messages_v2 WHERE mid = ? AND uid = ?", messageId, dialogId);
            boolean read = false;
            if (cursor.next()) {
                MessageCustomParamsHelper.readLocalParams(message, cursor.byteBufferValue(0));
                read = true;
            }
            cursor.dispose();
            cursor = null;
            if (!read) {
                cursor = database.queryFinalized("SELECT custom_params FROM messages_topics WHERE mid = ? AND uid = ?", messageId, dialogId);
                if (cursor.next()) {
                    MessageCustomParamsHelper.readLocalParams(message, cursor.byteBufferValue(0));
                    read = true;
                }
                cursor.dispose();
                cursor = null;
            }
        } catch (SQLiteException e) {
            checkSQLException(e);
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
        return message;
    }

    public void getNewTask(LongSparseArray<ArrayList<Integer>> oldTask, LongSparseArray<ArrayList<Integer>> oldTaskMedia) {
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            try {
                if (oldTask != null) {
                    for (int a = 0, N = oldTask.size(); a < N; a++) {
                        database.executeFast(String.format(Locale.US, "DELETE FROM enc_tasks_v4 WHERE mid IN(%s) AND uid = %d AND media = 0", TextUtils.join(",", oldTask.valueAt(a)), oldTask.keyAt(a))).stepThis().dispose();
                    }
                }
                if (oldTaskMedia != null) {
                    for (int a = 0, N = oldTaskMedia.size(); a < N; a++) {
                        database.executeFast(String.format(Locale.US, "DELETE FROM enc_tasks_v4 WHERE mid IN(%s) AND uid = %d AND media = 1", TextUtils.join(",", oldTaskMedia.valueAt(a)), oldTaskMedia.keyAt(a))).stepThis().dispose();
                    }
                }
                int date = 0;
                LongSparseArray<ArrayList<Integer>> newTask = null;
                LongSparseArray<ArrayList<Integer>> newTaskMedia = null;
                cursor = database.queryFinalized("SELECT mid, date, media, uid FROM enc_tasks_v4 WHERE date = (SELECT min(date) FROM enc_tasks_v4)");
                while (cursor.next()) {
                    int mid = cursor.intValue(0);
                    date = cursor.intValue(1);
                    int isMedia = cursor.intValue(2);
                    long uid = cursor.longValue(3);
                    boolean media;
                    if (isMedia == -1) {
                        media = mid > 0;
                    } else {
                        media = isMedia != 0;
                    }
                    LongSparseArray<ArrayList<Integer>> task;
                    if (media) {
                        if (newTaskMedia == null) {
                            newTaskMedia = new LongSparseArray<>();
                        }
                        task = newTaskMedia;
                    } else {
                        if (newTask == null) {
                            newTask = new LongSparseArray<>();
                        }
                        task = newTask;
                    }
                    ArrayList<Integer> arr = task.get(uid);
                    if (arr == null) {
                        arr = new ArrayList<>();
                        task.put(uid, arr);
                    }
                    arr.add(mid);
                }
                cursor.dispose();
                cursor = null;
                getMessagesController().processLoadedDeleteTask(date, newTask, newTaskMedia);
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
        });
    }

    public void markMentionMessageAsRead(long dialogId, int messageId, long did) {
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            try {
                database.executeFast(String.format(Locale.US, "UPDATE messages_v2 SET read_state = read_state | 2 WHERE mid = %d AND uid = %d", messageId, dialogId)).stepThis().dispose();
                cursor = database.queryFinalized("SELECT unread_count_i FROM dialogs WHERE did = " + did);
                int old_mentions_count = 0;
                if (cursor.next()) {
                    old_mentions_count = Math.max(0, cursor.intValue(0) - 1);
                }
                cursor.dispose();
                cursor = null;
                database.executeFast(String.format(Locale.US, "UPDATE dialogs SET unread_count_i = %d WHERE did = %d", old_mentions_count, did)).stepThis().dispose();
                LongSparseIntArray sparseArray = new LongSparseIntArray(1);
                sparseArray.put(did, old_mentions_count);
                if (old_mentions_count == 0) {
                    updateFiltersReadCounter(null, sparseArray, true);
                }
                getMessagesController().processDialogsUpdateRead(null, sparseArray);

                database.executeFast(String.format(Locale.US, "UPDATE messages_topics SET read_state = read_state | 2 WHERE mid = %d AND uid = %d", messageId, dialogId)).stepThis().dispose();
                cursor = database.queryFinalized(String.format(Locale.US, "SELECT data FROM messages_topics WHERE mid = %d AND uid = %d", messageId, dialogId));
                long topicId = 0;
                while (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(0);
                    if (data != null) {
                        TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                        data.reuse();
                        topicId = MessageObject.getTopicId(currentAccount, message, getForumTypeFlags(dialogId));
                    }
                }
                cursor.dispose();
                cursor = null;

                if (topicId != 0) {
                    int topicMentionsCount = 0;
                    cursor = database.queryFinalized(String.format(Locale.US, "SELECT unread_mentions FROM topics WHERE did = %d AND topic_id = %d", did, topicId));
                    if (cursor.next()) {
                        topicMentionsCount = Math.max(0, cursor.intValue(0) - 1);
                    }
                    cursor.dispose();
                    cursor = null;

                    database.executeFast(String.format(Locale.US, "UPDATE topics SET unread_mentions = %d WHERE did = %d AND topic_id = %d",topicMentionsCount, dialogId, topicId)).stepThis().dispose();

                    getMessagesController().getTopicsController().updateMentionsUnread(dialogId, topicId, topicMentionsCount);
                }

            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
        });
    }

    public void markMessageAsMention(long dialogId, int mid) {
        storageQueue.postRunnable(() -> {
            try {
                database.executeFast(String.format(Locale.US, "UPDATE messages_v2 SET mention = 1, read_state = read_state & ~2 WHERE mid = %d AND uid = %d", mid, dialogId)).stepThis().dispose();
            } catch (Exception e) {
                checkSQLException(e);
            }
        });
    }

    public void resetMentionsCount(long did, long topicId, int count) {
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            try {
                if (topicId == 0) {
                    int prevUnreadCount = 0;
                    cursor = database.queryFinalized("SELECT unread_count_i FROM dialogs WHERE did = " + did);
                    if (cursor.next()) {
                        prevUnreadCount = cursor.intValue(0);
                    }
                    cursor.dispose();
                    cursor = null;
                    if (prevUnreadCount != 0 || count != 0) {
                        if (count == 0) {
                            database.executeFast(String.format(Locale.US, "UPDATE messages_v2 SET read_state = read_state | 2 WHERE uid = %d AND mention = 1 AND read_state IN(0, 1)", did)).stepThis().dispose();
                        }
                        database.executeFast(String.format(Locale.US, "UPDATE dialogs SET unread_count_i = %d WHERE did = %d", count, did)).stepThis().dispose();
                        LongSparseIntArray sparseArray = new LongSparseIntArray(1);
                        sparseArray.put(did, count);
                        getMessagesController().processDialogsUpdateRead(null, sparseArray);
                        if (count == 0) {
                            updateFiltersReadCounter(null, sparseArray, true);
                        }
                    }
                } else {
                    database.executeFast(String.format(Locale.US, "UPDATE topics SET unread_mentions = %d WHERE did = %d AND topic_id = %d", count, did, topicId)).stepThis().dispose();
                    TopicsController.TopicUpdate topicUpdate = new TopicsController.TopicUpdate();
                    topicUpdate.dialogId = did;
                    topicUpdate.topicId = topicId;
                    topicUpdate.onlyCounters = true;
                    topicUpdate.unreadMentions = count;
                    topicUpdate.unreadCount = -1;
                    getMessagesController().getTopicsController().processUpdate(Collections.singletonList(topicUpdate));
                }
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
        });
    }

    public void createTaskForMid(long dialogId, int messageId, int time, int readTime, int ttl, boolean inner) {
        storageQueue.postRunnable(() -> {
            SQLitePreparedStatement state = null;
            try {
                int minDate = Math.max(time, readTime) + ttl;
                SparseArray<ArrayList<Integer>> messages = new SparseArray<>();
                ArrayList<Integer> midsArray = new ArrayList<>();

                midsArray.add(messageId);
                messages.put(minDate, midsArray);

                AndroidUtilities.runOnUIThread(() -> {
                    if (!inner) {
                        markMessagesContentAsRead(dialogId, midsArray, 0, 0);
                    }
                    getNotificationCenter().postNotificationName(NotificationCenter.messagesReadContent, dialogId, midsArray);
                });

                state = database.executeFast("REPLACE INTO enc_tasks_v4 VALUES(?, ?, ?, ?)");
                for (int a = 0; a < messages.size(); a++) {
                    int key = messages.keyAt(a);
                    ArrayList<Integer> arr = messages.get(key);
                    for (int b = 0; b < arr.size(); b++) {
                        state.requery();
                        state.bindInteger(1, arr.get(b));
                        state.bindLong(2, dialogId);
                        state.bindInteger(3, key);
                        state.bindInteger(4, 1);
                        state.step();
                    }
                }
                state.dispose();
                state = null;
                database.executeFast(String.format(Locale.US, "UPDATE messages_v2 SET ttl = 0 WHERE mid = %d AND uid = %d", messageId, dialogId)).stepThis().dispose();
                getMessagesController().didAddedNewTask(minDate, dialogId, messages);
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (state != null) {
                    state.dispose();
                }
            }
        });
    }

    private void createTaskForSecretMedia(long dialogId, SparseArray<ArrayList<Integer>> messages) {
        SQLiteCursor cursor = null;
        SQLitePreparedStatement state = null;
        try {
            int minDate = Integer.MAX_VALUE;

//                if (random_ids != null) {
//                    AndroidUtilities.runOnUIThread(() -> {
//                        markMessagesContentAsRead(dialogId, mids, 0, 0);
//                        getNotificationCenter().postNotificationName(NotificationCenter.messagesReadContent, dialogId, mids);
//                    });
//                }

            ArrayList<Integer> mids = new ArrayList<>();
            if (messages.size() != 0) {
                database.beginTransaction();
                state = database.executeFast("REPLACE INTO enc_tasks_v4 VALUES(?, ?, ?, ?)");
                for (int a = 0; a < messages.size(); a++) {
                    int key = messages.keyAt(a);
                    ArrayList<Integer> arr = messages.get(key);
                    for (int b = 0; b < arr.size(); b++) {
                        int date = arr.get(b);
                        state.requery();
                        state.bindInteger(1, date);
                        state.bindLong(2, dialogId);
                        state.bindInteger(3, key);
                        state.bindInteger(4, 1);
                        minDate = Math.min(minDate, date);
                        state.step();
                        mids.add(arr.get(b));
                    }
                }
                state.dispose();
                state = null;
                database.commitTransaction();
                database.executeFast(String.format(Locale.US, "UPDATE messages_v2 SET ttl = 0 WHERE uid = %d AND mid IN(%s)", dialogId, TextUtils.join(", ", mids))).stepThis().dispose();
                getMessagesController().didAddedNewTask(minDate, dialogId, messages);
            }
        } catch (Exception e) {
            checkSQLException(e);
        } finally {
            if (database != null) {
                database.commitTransaction();
            }
            if (state != null) {
                state.dispose();
            }
            if (cursor != null) {
                cursor.dispose();
            }
        }
    }

    public void createTaskForSecretChat(int chatId, int time, int readTime, int isOut, ArrayList<Long> random_ids) {
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            SQLitePreparedStatement state = null;
            try {
                long dialogId = DialogObject.makeEncryptedDialogId(chatId);
                int minDate = Integer.MAX_VALUE;
                SparseArray<ArrayList<Integer>> messages = new SparseArray<>();
                ArrayList<Integer> midsArray = new ArrayList<>();
                StringBuilder mids = new StringBuilder();
                if (random_ids == null) {
                    cursor = database.queryFinalized(String.format(Locale.US, "SELECT mid, ttl FROM messages_v2 WHERE uid = %d AND out = %d AND read_state > 0 AND ttl > 0 AND date <= %d AND send_state = 0 AND media != 1", dialogId, isOut, time));
                } else {
                    String ids = TextUtils.join(",", random_ids);
                    cursor = database.queryFinalized(String.format(Locale.US, "SELECT m.mid, m.ttl FROM messages_v2 as m INNER JOIN randoms_v2 as r ON m.mid = r.mid AND m.uid = r.uid WHERE r.random_id IN (%s)", ids));
                }
                while (cursor.next()) {
                    int ttl = cursor.intValue(1);
                    int mid = cursor.intValue(0);
                    if (random_ids != null) {
                        midsArray.add(mid);
                    }
                    if (ttl <= 0) {
                        continue;
                    }
                    int date = Math.max(time, readTime) + ttl;
                    minDate = Math.min(minDate, date);
                    ArrayList<Integer> arr = messages.get(date);
                    if (arr == null) {
                        arr = new ArrayList<>();
                        messages.put(date, arr);
                    }
                    if (mids.length() != 0) {
                        mids.append(",");
                    }
                    mids.append(mid);
                    arr.add(mid);
                }
                cursor.dispose();
                cursor = null;

                if (random_ids != null) {
                    AndroidUtilities.runOnUIThread(() -> {
                        markMessagesContentAsRead(dialogId, midsArray, 0, 0);
                        getNotificationCenter().postNotificationName(NotificationCenter.messagesReadContent, dialogId, midsArray);
                    });
                }

                if (messages.size() != 0) {
                    database.beginTransaction();
                    state = database.executeFast("REPLACE INTO enc_tasks_v4 VALUES(?, ?, ?, ?)");
                    for (int a = 0; a < messages.size(); a++) {
                        int key = messages.keyAt(a);
                        ArrayList<Integer> arr = messages.get(key);
                        for (int b = 0; b < arr.size(); b++) {
                            state.requery();
                            state.bindInteger(1, arr.get(b));
                            state.bindLong(2, dialogId);
                            state.bindInteger(3, key);
                            state.bindInteger(4, 0);
                            state.step();
                        }
                    }
                    state.dispose();
                    state = null;
                    database.commitTransaction();
                    database.executeFast(String.format(Locale.US, "UPDATE messages_v2 SET ttl = 0 WHERE mid IN(%s) AND uid = %d", mids.toString(), dialogId)).stepThis().dispose();
                    getMessagesController().didAddedNewTask(minDate, dialogId, messages);
                }
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (database != null) {
                    database.commitTransaction();
                }
                if (state != null) {
                    state.dispose();
                }
                if (cursor != null) {
                    cursor.dispose();
                }
            }
        });
    }

    private void updateFiltersReadCounter(LongSparseIntArray dialogsToUpdate, LongSparseIntArray dialogsToUpdateMentions, boolean read) throws Exception {
        if ((dialogsToUpdate == null || dialogsToUpdate.size() == 0) && (dialogsToUpdateMentions == null || dialogsToUpdateMentions.size() == 0)) {
            return;
        }
        for (int a = 0; a < 2; a++) {
            for (int b = 0; b < 2; b++) {
                contacts[a][b] = nonContacts[a][b] = bots[a][b] = channels[a][b] = groups[a][b] = communities[a][b] = 0;
            }
            mentionChannels[a] = mentionGroups[a] = 0;
        }

        ArrayList<TLRPC.User> users = new ArrayList<>();
        ArrayList<TLRPC.User> encUsers = new ArrayList<>();
        ArrayList<TLRPC.Chat> chats = new ArrayList<>();
        ArrayList<Long> usersToLoad = new ArrayList<>();
        HashSet<Long> chatsToLoad = new HashSet<>();
        ArrayList<Integer> encryptedToLoad = new ArrayList<>();
        LongSparseArray<Integer> dialogsByFolders = new LongSparseArray<>();
        LongSparseArray<Integer> newUnreadDialogs = new LongSparseArray<>();

        for (int b = 0; b < 2; b++) {
            LongSparseIntArray array = b == 0 ? dialogsToUpdate : dialogsToUpdateMentions;
            if (array == null) {
                continue;
            }
            for (int a = 0; a < array.size(); a++) {
                Integer count = array.valueAt(a);
                if (read && count != 0 || !read && count == 0) {
                    continue;
                }
                long did = array.keyAt(a);
                if (read) {
                    if (b == 0) {
                        dialogsWithUnread.remove(did);
                        /*if (BuildVars.DEBUG_VERSION) {
                            FileLog.d("read remove = " + did);
                        }*/
                    } else {
                        dialogsWithMentions.remove(did);
                        /*if (BuildVars.DEBUG_VERSION) {
                            FileLog.d("mention remove = " + did);
                        }*/
                    }
                } else {
                    if (dialogsWithMentions.indexOfKey(did) < 0 && dialogsWithUnread.indexOfKey(did) < 0) {
                        newUnreadDialogs.put(did, count);
                    }
                    if (b == 0) {
                        dialogsWithUnread.put(did, count);
                        /*if (BuildVars.DEBUG_VERSION) {
                            FileLog.d("read add = " + did);
                        }*/
                    } else {
                        dialogsWithMentions.put(did, count);
                        /*if (BuildVars.DEBUG_VERSION) {
                            FileLog.d("mention add = " + did);
                        }*/
                    }
                }

                if (dialogsByFolders.indexOfKey(did) < 0) {
                    SQLiteCursor cursor = database.queryFinalized("SELECT folder_id FROM dialogs WHERE did = " + did);
                    int folderId = 0;
                    if (cursor.next()) {
                        folderId = cursor.intValue(0);
                    }
                    cursor.dispose();
                    dialogsByFolders.put(did, folderId);
                }

                if (DialogObject.isEncryptedDialog(did)) {
                    int encryptedChatId = DialogObject.getEncryptedChatId(did);
                    if (!encryptedToLoad.contains(encryptedChatId)) {
                        encryptedToLoad.add(encryptedChatId);
                    }
                } else if (DialogObject.isUserDialog(did)) {
                    if (!usersToLoad.contains(did)) {
                        usersToLoad.add(did);
                    }
                } else {
                    chatsToLoad.add(-did);
                }
            }
        }
        LongSparseArray<TLRPC.User> usersDict = new LongSparseArray<>();
        LongSparseArray<TLRPC.Chat> chatsDict = new LongSparseArray<>();
        LongSparseArray<TLRPC.User> encUsersDict = new LongSparseArray<>();
        LongSparseArray<Integer> encryptedChatsByUsersCount = new LongSparseArray<>();
        LongSparseArray<Boolean> mutedDialogs = new LongSparseArray<>();
        LongSparseArray<Boolean> archivedDialogs = new LongSparseArray<>();
        if (!usersToLoad.isEmpty()) {
            getUsersInternal(usersToLoad, users);
            for (int a = 0, N = users.size(); a < N; a++) {
                TLRPC.User user = users.get(a);
                boolean muted = getMessagesController().isDialogMuted(user.id, 0);
                Integer folderId = dialogsByFolders.get(user.id);
                int idx1 = folderId == null || folderId < 0 || folderId > 1 ? 0 : folderId;
                int idx2 = muted ? 1 : 0;
                if (muted) {
                    mutedDialogs.put(user.id, true);
                }
                if (idx1 == 1) {
                    archivedDialogs.put(user.id, true);
                }
                if (isUserCollapsedInCommunity(chatsDict, user)) {
                    communities[idx1][idx2]++;
                } else if (user.bot) {
                    bots[idx1][idx2]++;
                } else if (user.self || user.contact) {
                    contacts[idx1][idx2]++;
                } else {
                    nonContacts[idx1][idx2]++;
                }
                usersDict.put(user.id, user);
            }
        }
        if (!encryptedToLoad.isEmpty()) {
            ArrayList<Long> encUsersToLoad = new ArrayList<>();
            ArrayList<TLRPC.EncryptedChat> encryptedChats = new ArrayList<>();
            getEncryptedChatsInternal(TextUtils.join(",", encryptedToLoad), encryptedChats, encUsersToLoad);
            if (!encUsersToLoad.isEmpty()) {
                getUsersInternal(encUsersToLoad, encUsers);
                for (int a = 0, N = encUsers.size(); a < N; a++) {
                    TLRPC.User user = encUsers.get(a);
                    encUsersDict.put(user.id, user);
                }
                for (int a = 0, N = encryptedChats.size(); a < N; a++) {
                    TLRPC.EncryptedChat encryptedChat = encryptedChats.get(a);
                    TLRPC.User user = encUsersDict.get(encryptedChat.user_id);
                    if (user == null) {
                        continue;
                    }
                    long did = DialogObject.makeEncryptedDialogId(encryptedChat.id);
                    boolean muted = getMessagesController().isDialogMuted(did, 0);
                    Integer folderId = dialogsByFolders.get(did);
                    int idx1 = folderId == null || folderId < 0 || folderId > 1 ? 0 : folderId;
                    int idx2 = muted ? 1 : 0;
                    if (muted) {
                        mutedDialogs.put(user.id, true);
                    }
                    if (idx1 == 1) {
                        archivedDialogs.put(user.id, true);
                    }
                    if (user.self || user.contact) {
                        contacts[idx1][idx2]++;
                    } else {
                        nonContacts[idx1][idx2]++;
                    }
                    int count = encryptedChatsByUsersCount.get(user.id, 0);
                    encryptedChatsByUsersCount.put(user.id, count + 1);
                }
            }
        }
        if (!chatsToLoad.isEmpty()) {
            getChatsInternal(TextUtils.join(",", chatsToLoad), chats);
            for (int a = 0, N = chats.size(); a < N; a++) {
                TLRPC.Chat chat = chats.get(a);
                if (!chatsToLoad.contains(chat.id)) {
                    continue;
                }

                if (chat.migrated_to instanceof TLRPC.TL_inputChannel || ChatObject.isNotInChat(chat) || ChatObject.isCommunity(chat)) {
                    continue;
                }
                boolean muted = getMessagesController().isDialogMuted(-chat.id, 0, chat);
                boolean hasUnread = dialogsWithUnread.indexOfKey(-chat.id) >= 0;
                boolean hasMention = dialogsWithMentions.indexOfKey(-chat.id) >= 0;
                Integer folderId = dialogsByFolders.get(-chat.id);
                int idx1 = folderId == null || folderId < 0 || folderId > 1 ? 0 : folderId;
                int idx2 = muted ? 1 : 0;
                if (muted) {
                    mutedDialogs.put(-chat.id, true);
                }
                if (idx1 == 1) {
                    archivedDialogs.put(-chat.id, true);
                }

                if (ChatObject.isCommunity(chat)) {

                } else if (isChatCollapsedInCommunity(chatsDict, chat)) {
                    communities[idx1][idx2]++;
                } else {
                    if (muted && dialogsToUpdateMentions != null && dialogsToUpdateMentions.indexOfKey(-chat.id) >= 0) {
                        if (ChatObject.isChannel(chat) && !chat.megagroup) {
                            mentionChannels[idx1]++;
                        } else {
                            mentionGroups[idx1]++;
                        }
                    }
                    if (read && !hasUnread && !hasMention || !read && newUnreadDialogs.indexOfKey(-chat.id) >= 0) {
                        if (ChatObject.isChannel(chat) && !chat.megagroup) {
                            channels[idx1][idx2]++;
                        } else {
                            groups[idx1][idx2]++;
                        }
                    }
                }
                chatsDict.put(chat.id, chat);
            }
        }
        /*if (BuildVars.DEBUG_VERSION) {
            for (int b = 0; b < 2; b++) {
                FileLog.d("read = " + read + " contacts = " + contacts[b][0] + ", " + contacts[b][1]);
                FileLog.d("read = " + read + " nonContacts = " + nonContacts[b][0] + ", " + nonContacts[b][1]);
                FileLog.d("read = " + read + " groups = " + groups[b][0] + ", " + groups[b][1]);
                FileLog.d("read = " + read + " channels = " + channels[b][0] + ", " + channels[b][1]);
                FileLog.d("read = " + read + " bots = " + bots[b][0] + ", " + bots[b][1]);
            }
        }*/

        for (int a = 0, N = dialogFilters.size(); a < N + 2; a++) {
            final boolean isFilter = a < N;
            final boolean isMain = a == N;
            final boolean isArchive = a == N + 1;

            int unreadCount;
            MessagesController.DialogFilter filter;
            int flags;
            if (a < N) {
                filter = dialogFilters.get(a);
                if (filter.pendingUnreadCount < 0) {
                    continue;
                }
                unreadCount = filter.pendingUnreadCount;
                flags = filter.flags;
            } else {
                filter = null;
                flags = MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS;
                if (a == N) {
                    unreadCount = pendingMainUnreadCount;
                    flags |= MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED;
                    if (!getNotificationsController().showBadgeMuted) {
                        flags |= MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED;
                    }
                } else {
                    unreadCount = pendingArchiveUnreadCount;
                    flags |= MessagesController.DIALOG_FILTER_FLAG_ONLY_ARCHIVED;
                }
            }
            if (read) {
                if ((flags & MessagesController.DIALOG_FILTER_FLAG_CONTACTS) != 0) {
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_ONLY_ARCHIVED) == 0) {
                        unreadCount -= contacts[0][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount -= contacts[0][1];
                        }
                    }
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) == 0) {
                        unreadCount -= contacts[1][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount -= contacts[1][1];
                        }
                    }
                }
                if ((flags & MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS) != 0) {
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_ONLY_ARCHIVED) == 0) {
                        unreadCount -= nonContacts[0][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount -= nonContacts[0][1];
                        }
                    }
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) == 0) {
                        unreadCount -= nonContacts[1][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount -= nonContacts[1][1];
                        }
                    }
                }
                if ((flags & MessagesController.DIALOG_FILTER_FLAG_GROUPS) != 0) {
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_ONLY_ARCHIVED) == 0) {
                        unreadCount -= groups[0][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount -= groups[0][1];
                        } else {
                            unreadCount -= mentionGroups[0];
                        }
                    }
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) == 0) {
                        unreadCount -= groups[1][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount -= groups[1][1];
                        } else {
                            unreadCount -= mentionGroups[1];
                        }
                    }
                }
                if ((flags & MessagesController.DIALOG_FILTER_FLAG_CHANNELS) != 0) {
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_ONLY_ARCHIVED) == 0) {
                        unreadCount -= channels[0][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount -= channels[0][1];
                        } else {
                            unreadCount -= mentionChannels[0];
                        }
                    }
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) == 0) {
                        unreadCount -= channels[1][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount -= channels[1][1];
                        } else {
                            unreadCount -= mentionChannels[1];
                        }
                    }
                }
                if ((flags & MessagesController.DIALOG_FILTER_FLAG_BOTS) != 0) {
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_ONLY_ARCHIVED) == 0) {
                        unreadCount -= bots[0][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount -= bots[0][1];
                        }
                    }
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) == 0) {
                        unreadCount -= bots[1][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount -= bots[1][1];
                        }
                    }
                }

                if (!isArchive && (flags & MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS) == MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS) {
                    unreadCount -= communities[0][0];
                    unreadCount -= communities[1][0];
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                        unreadCount -= communities[0][1];
                        unreadCount -= communities[1][1];
                    }
                }

                if (filter != null) {
                    for (int b = 0, N2 = filter.alwaysShow.size(); b < N2; b++) {
                        long did = filter.alwaysShow.get(b);
                        if (DialogObject.isUserDialog(did)) {
                            for (int i = 0; i < 2; i++) {
                                LongSparseArray<TLRPC.User> dict = i == 0 ? usersDict : encUsersDict;
                                TLRPC.User user = dict.get(did);
                                if (user != null) {
                                    int count;
                                    if (i == 0) {
                                        count = 1;
                                    } else {
                                        count = encryptedChatsByUsersCount.get(did, 0);
                                        if (count == 0) {
                                            continue;
                                        }
                                    }
                                    int flag;
                                    if (user.bot) {
                                        flag = MessagesController.DIALOG_FILTER_FLAG_BOTS;
                                    } else if (user.self || user.contact) {
                                        flag = MessagesController.DIALOG_FILTER_FLAG_CONTACTS;
                                    } else {
                                        flag = MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS;
                                    }
                                    if ((flags & flag) == 0) {
                                        unreadCount -= count;
                                    } else if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) != 0 && mutedDialogs.indexOfKey(user.id) >= 0) {
                                        unreadCount -= count;
                                    } else if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) != 0 && archivedDialogs.indexOfKey(user.id) >= 0) {
                                        unreadCount -= count;
                                    }
                                }
                            }
                        } else {
                            TLRPC.Chat chat = chatsDict.get(-did);
                            if (chat != null) {
                                int flag;
                                if (ChatObject.isChannel(chat) && !chat.megagroup) {
                                    flag = MessagesController.DIALOG_FILTER_FLAG_CHANNELS;
                                } else {
                                    flag = MessagesController.DIALOG_FILTER_FLAG_GROUPS;
                                }
                                if ((flags & flag) == 0) {
                                    unreadCount--;
                                } else if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) != 0 && mutedDialogs.indexOfKey(-chat.id) >= 0 && dialogsWithMentions.indexOfKey(-chat.id) < 0) {
                                    unreadCount--;
                                } else if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) != 0 && archivedDialogs.indexOfKey(-chat.id) >= 0) {
                                    unreadCount--;
                                }
                            }
                        }
                    }
                    for (int b = 0, N2 = filter.neverShow.size(); b < N2; b++) {
                        long did = filter.neverShow.get(b);
                        if (dialogsToUpdateMentions != null && dialogsToUpdateMentions.indexOfKey(did) >= 0 && mutedDialogs.indexOfKey(did) < 0) {
                            continue;
                        }
                        if (DialogObject.isUserDialog(did)) {
                            for (int i = 0; i < 2; i++) {
                                LongSparseArray<TLRPC.User> dict = i == 0 ? usersDict : encUsersDict;
                                TLRPC.User user = dict.get(did);
                                if (user != null) {
                                    int count;
                                    if (i == 0) {
                                        count = 1;
                                    } else {
                                        count = encryptedChatsByUsersCount.get(did, 0);
                                        if (count == 0) {
                                            continue;
                                        }
                                    }
                                    int flag;
                                    if (user.bot) {
                                        flag = MessagesController.DIALOG_FILTER_FLAG_BOTS;
                                    } else if (user.self || user.contact) {
                                        flag = MessagesController.DIALOG_FILTER_FLAG_CONTACTS;
                                    } else {
                                        flag = MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS;
                                    }
                                    if ((flags & flag) != 0) {
                                        if (((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) == 0 || archivedDialogs.indexOfKey(user.id) < 0) &&
                                                ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0 || mutedDialogs.indexOfKey(user.id) < 0)) {
                                            unreadCount += count;
                                        }
                                    }
                                }
                            }
                        } else {
                            TLRPC.Chat chat = chatsDict.get(-did);
                            if (chat != null) {
                                int flag;
                                if (ChatObject.isChannel(chat) && !chat.megagroup) {
                                    flag = MessagesController.DIALOG_FILTER_FLAG_CHANNELS;
                                } else {
                                    flag = MessagesController.DIALOG_FILTER_FLAG_GROUPS;
                                }
                                if ((flags & flag) != 0) {
                                    if (((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) == 0 || archivedDialogs.indexOfKey(-chat.id) < 0) &&
                                            ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0 || mutedDialogs.indexOfKey(-chat.id) < 0 || dialogsWithMentions.indexOfKey(-chat.id) >= 0)) {
                                        unreadCount++;
                                    }
                                }
                            }
                        }
                    }
                }
                if (unreadCount < 0) {
                    unreadCount = 0;
                }
            } else {
                if ((flags & MessagesController.DIALOG_FILTER_FLAG_CONTACTS) != 0) {
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_ONLY_ARCHIVED) == 0) {
                        unreadCount += contacts[0][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount += contacts[0][1];
                        }
                    }
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) == 0) {
                        unreadCount += contacts[1][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount += contacts[1][1];
                        }
                    }
                }
                if ((flags & MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS) != 0) {
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_ONLY_ARCHIVED) == 0) {
                        unreadCount += nonContacts[0][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount += nonContacts[0][1];
                        }
                    }
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) == 0) {
                        unreadCount += nonContacts[1][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount += nonContacts[1][1];
                        }
                    }
                }
                if ((flags & MessagesController.DIALOG_FILTER_FLAG_GROUPS) != 0) {
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_ONLY_ARCHIVED) == 0) {
                        unreadCount += groups[0][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount += groups[0][1];
                        } else {
                            unreadCount += mentionGroups[0];
                        }
                    }
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) == 0) {
                        unreadCount += groups[1][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount += groups[1][1];
                        } else {
                            unreadCount += mentionGroups[1];
                        }
                    }
                }
                if ((flags & MessagesController.DIALOG_FILTER_FLAG_CHANNELS) != 0) {
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_ONLY_ARCHIVED) == 0) {
                        unreadCount += channels[0][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount += channels[0][1];
                        } else {
                            unreadCount += mentionChannels[0];
                        }
                    }
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) == 0) {
                        unreadCount += channels[1][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount += channels[1][1];
                        } else {
                            unreadCount += mentionChannels[1];
                        }
                    }
                }
                if ((flags & MessagesController.DIALOG_FILTER_FLAG_BOTS) != 0) {
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_ONLY_ARCHIVED) == 0) {
                        unreadCount += bots[0][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount += bots[0][1];
                        }
                    }
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) == 0) {
                        unreadCount += bots[1][0];
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                            unreadCount += bots[1][1];
                        }
                    }
                }

                if (!isArchive && (flags & MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS) == MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS) {
                    unreadCount += communities[0][0];
                    unreadCount += communities[1][0];
                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) == 0) {
                        unreadCount += communities[0][1];
                        unreadCount += communities[1][1];
                    }
                }

                if (filter != null) {
                    if (!filter.alwaysShow.isEmpty()) {
                        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) != 0 && dialogsToUpdateMentions != null) {
                            for (int b = 0, N2 = dialogsToUpdateMentions.size(); b < N2; b++) {
                                long did = dialogsToUpdateMentions.keyAt(b);
                                TLRPC.Chat chat = chatsDict.get(-did);
                                if (ChatObject.isChannel(chat) && !chat.megagroup) {
                                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_CHANNELS) == 0) {
                                        continue;
                                    }
                                } else {
                                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_GROUPS) == 0) {
                                        continue;
                                    }
                                }
                                if (mutedDialogs.indexOfKey(did) >= 0 && filter.alwaysShow.contains(did)) {
                                    unreadCount--;
                                }
                            }
                        }
                        for (int b = 0, N2 = filter.alwaysShow.size(); b < N2; b++) {
                            long did = filter.alwaysShow.get(b);
                            if (newUnreadDialogs.indexOfKey(did) < 0) {
                                continue;
                            }
                            if (DialogObject.isUserDialog(did)) {
                                TLRPC.User user = usersDict.get(did);
                                if (user != null) {
                                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) != 0 && mutedDialogs.indexOfKey(user.id) >= 0) {
                                        unreadCount++;
                                    } else {
                                        if (user.bot) {
                                            if ((flags & MessagesController.DIALOG_FILTER_FLAG_BOTS) == 0) {
                                                unreadCount++;
                                            }
                                        } else if (user.self || user.contact) {
                                            if ((flags & MessagesController.DIALOG_FILTER_FLAG_CONTACTS) == 0) {
                                                unreadCount++;
                                            }
                                        } else {
                                            if ((flags & MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS) == 0) {
                                                unreadCount++;
                                            }
                                        }
                                    }
                                }
                                user = encUsersDict.get(did);
                                if (user != null) {
                                    int count = encryptedChatsByUsersCount.get(did, 0);
                                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) != 0 && mutedDialogs.indexOfKey(user.id) >= 0) {
                                        unreadCount += count;
                                    } else {
                                        if (user.bot) {
                                            if ((flags & MessagesController.DIALOG_FILTER_FLAG_BOTS) == 0) {
                                                unreadCount += count;
                                            }
                                        } else if (user.self || user.contact) {
                                            if ((flags & MessagesController.DIALOG_FILTER_FLAG_CONTACTS) == 0) {
                                                unreadCount += count;
                                            }
                                        } else {
                                            if ((flags & MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS) == 0) {
                                                unreadCount += count;
                                            }
                                        }
                                    }
                                }
                            } else {
                                TLRPC.Chat chat = chatsDict.get(-did);
                                if (chat != null) {
                                    if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) != 0 && mutedDialogs.indexOfKey(-chat.id) >= 0) {
                                        unreadCount++;
                                    } else {
                                        if (ChatObject.isChannel(chat) && !chat.megagroup) {
                                            if ((flags & MessagesController.DIALOG_FILTER_FLAG_CHANNELS) == 0) {
                                                unreadCount++;
                                            }
                                        } else {
                                            if ((flags & MessagesController.DIALOG_FILTER_FLAG_GROUPS) == 0) {
                                                unreadCount++;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    for (int b = 0, N2 = filter.neverShow.size(); b < N2; b++) {
                        long did = filter.neverShow.get(b);
                        if (DialogObject.isUserDialog(did)) {
                            TLRPC.User user = usersDict.get(did);
                            if (user != null) {
                                unreadCount--;
                            }
                            user = encUsersDict.get(did);
                            if (user != null) {
                                unreadCount -= encryptedChatsByUsersCount.get(did, 0);
                            }
                        } else {
                            TLRPC.Chat chat = chatsDict.get(-did);
                            if (chat != null) {
                                unreadCount--;
                            }
                        }
                    }
                }
            }
            if (filter != null) {
                filter.pendingUnreadCount = unreadCount;
                /*if (BuildVars.DEBUG_VERSION) {
                    FileLog.d("filter " + filter.name + " flags = " + flags + " read = " + read + " unread count = " + filter.pendingUnreadCount);
                }*/
            } else if (a == N) {
                pendingMainUnreadCount = unreadCount;
            } else if (isArchive) {
                pendingArchiveUnreadCount = unreadCount;
            }
        }
        AndroidUtilities.runOnUIThread(() -> {
            ArrayList<MessagesController.DialogFilter> filters = getMessagesController().dialogFilters;
            for (int a = 0, N = filters.size(); a < N; a++) {
                filters.get(a).unreadCount = filters.get(a).pendingUnreadCount;
            }
            mainUnreadCount = pendingMainUnreadCount;
            archiveUnreadCount = pendingArchiveUnreadCount;
        });
    }

    private boolean isUserCollapsedInCommunity(LongSparseArray<TLRPC.Chat> chatsDict, TLRPC.User user) {
        if (user.linked_community_id != 0) {
            TLRPC.Chat community = chatsDict.get(user.linked_community_id);
            if (community == null) {
                community = getChat(user.linked_community_id);
                chatsDict.put(user.linked_community_id, community);
            }
            return community != null && community.collapsed_in_dialogs;
        }
        return false;
    }

    private boolean isChatCollapsedInCommunity(LongSparseArray<TLRPC.Chat> chatsDict, TLRPC.Chat chat) {
        if (chat.linked_community_id != 0) {
            TLRPC.Chat community = chatsDict.get(chat.linked_community_id);
            if (community == null) {
                community = getChat(chat.linked_community_id);
                chatsDict.put(chat.linked_community_id, community);
            }
            return community != null && community.collapsed_in_dialogs;
        }
        return false;
    }


    private void updateDialogsWithReadMessagesInternal(ArrayList<Integer> messages, LongSparseIntArray inbox, LongSparseIntArray outbox, LongSparseArray<ArrayList<Integer>> mentions, LongSparseIntArray stillUnreadMessagesCount) {
        try {
            LongSparseIntArray dialogsToUpdate = new LongSparseIntArray();
            LongSparseIntArray dialogsToUpdateMentions = new LongSparseIntArray();
            ArrayList<Long> channelMentionsToReload = new ArrayList<>();

            if (!isEmpty(messages)) {
                String ids = TextUtils.join(",", messages);
                SQLiteCursor cursor = database.queryFinalized(String.format(Locale.US, "SELECT uid, read_state, out FROM messages_v2 WHERE mid IN(%s) AND is_channel = 0", ids));
                while (cursor.next()) {
                    int out = cursor.intValue(2);
                    if (out != 0) {
                        continue;
                    }
                    int read_state = cursor.intValue(1);
                    if (read_state != 0) {
                        continue;
                    }
                    long uid = cursor.longValue(0);
                    int currentCount = dialogsToUpdate.get(uid);
                    if (currentCount == 0) {
                        dialogsToUpdate.put(uid, 1);
                    } else {
                        dialogsToUpdate.put(uid, currentCount + 1);
                    }
                }
                cursor.dispose();
            } else {
                if (!isEmpty(inbox)) {
                    for (int b = 0; b < inbox.size(); b++) {
                        long key = inbox.keyAt(b);
                        int messageId = inbox.get(key);
                        int stillUnread = stillUnreadMessagesCount == null ? -2 : stillUnreadMessagesCount.get(key, -2);

                        if (stillUnread >= 0) {
                            dialogsToUpdate.put(key, stillUnread);
                            if (BuildVars.DEBUG_VERSION) {
                                FileLog.d(key + " update unread messages count by still unread " + stillUnread);
                            }
                        } else {
                            boolean canCountByMessageId = true;

                            if (stillUnreadMessagesCount != null && stillUnread != -2) {
                                SQLiteCursor checkHolesCursor = database.queryFinalized(String.format(Locale.US, "SELECT start, end FROM messages_holes WHERE uid = %d AND end > %d", key, messageId));
                                while (checkHolesCursor.next()) {
                                    canCountByMessageId = false;
                                }
                                checkHolesCursor.dispose();
                            }

                            if (canCountByMessageId) {
                                SQLiteCursor cursor = database.queryFinalized(String.format(Locale.US, "SELECT COUNT(mid) FROM messages_v2 WHERE uid = %d AND mid > %d AND read_state IN(0,2) AND out = 0", key, messageId));
                                if (cursor.next()) {
                                    int unread = cursor.intValue(0);
                                    dialogsToUpdate.put(key, unread);
                                    if (BuildVars.DEBUG_VERSION) {
                                        FileLog.d(key + " update unread messages count " + unread);
                                    }
                                } else {
                                    if (BuildVars.DEBUG_VERSION) {
                                        FileLog.d(key + " can't update unread messages count cursor trouble");
                                    }
                                }
                                cursor.dispose();
                            } else {
                                if (BuildVars.DEBUG_VERSION) {
                                    FileLog.d(key + " can't update unread messages count");
                                }
                            }
                        }

                        int oldMaxId = 0;
                        SQLiteCursor cursor = database.queryFinalized("SELECT inbox_max FROM dialogs WHERE did = " + key);
                        if (cursor.next()) {
                            oldMaxId = cursor.intValue(0);
                        }
                        cursor.dispose();
                        cursor = null;

                        FileLog.d(key + " set inbox max " + messageId);
                        SQLitePreparedStatement state = database.executeFast("UPDATE dialogs SET inbox_max = max((SELECT inbox_max FROM dialogs WHERE did = ?), ?) WHERE did = ?");
                        state.requery();
                        state.bindLong(1, key);
                        state.bindInteger(2, messageId);
                        state.bindLong(3, key);
                        state.step();
                        state.dispose();

                        if (isForum(key, FORUM_TYPE_DIRECT | FORUM_TYPE_CHAT_TABS | FORUM_TYPE_BOT)) {
                            updateTopicsWithReadFromAllInternal(key, oldMaxId, messageId);
                        }
                    }
                }
                if (!isEmpty(mentions)) {
                    for (int b = 0, N = mentions.size(); b < N; b++) {
                        ArrayList<Integer> arrayList = mentions.valueAt(b);
                        ArrayList<Integer> notFoundMentions = new ArrayList<>(arrayList);
                        String ids = TextUtils.join(",", arrayList);
                        long channelId = 0;
                        SQLiteCursor cursor = database.queryFinalized(String.format(Locale.US, "SELECT uid, read_state, out, mention, mid, is_channel FROM messages_v2 WHERE mid IN(%s)", ids));
                        while (cursor.next()) {
                            long did = cursor.longValue(0);
                            notFoundMentions.remove((Integer) cursor.intValue(4));
                            if (cursor.intValue(1) < 2 && cursor.intValue(2) == 0 && cursor.intValue(3) == 1) {
                                int unread_count = dialogsToUpdateMentions.get(did, -1);
                                if (unread_count < 0) {
                                    SQLiteCursor cursor2 = database.queryFinalized("SELECT unread_count_i FROM dialogs WHERE did = " + did);
                                    int old_mentions_count = 0;
                                    if (cursor2.next()) {
                                        old_mentions_count = cursor2.intValue(0);
                                    }
                                    cursor2.dispose();
                                    dialogsToUpdateMentions.put(did, Math.max(0, old_mentions_count - 1));
                                } else {
                                    dialogsToUpdateMentions.put(did, Math.max(0, unread_count - 1));
                                }
                            }
                            channelId = cursor.longValue(5);
                        }
                        cursor.dispose();
                        if (!notFoundMentions.isEmpty() && channelId != 0) {
                            if (!channelMentionsToReload.contains(channelId)) {
                                channelMentionsToReload.add(channelId);
                            }
                        }
                    }
                }
                if (!isEmpty(outbox)) {
                    for (int b = 0; b < outbox.size(); b++) {
                        long key = outbox.keyAt(b);
                        int messageId = outbox.get(key);
                        SQLitePreparedStatement state = database.executeFast("UPDATE dialogs SET outbox_max = max((SELECT outbox_max FROM dialogs WHERE did = ?), ?) WHERE did = ?");
                        state.requery();
                        state.bindLong(1, key);
                        state.bindInteger(2, messageId);
                        state.bindLong(3, key);
                        state.step();
                        state.dispose();
                    }
                }
            }

            if (dialogsToUpdate.size() > 0 || dialogsToUpdateMentions.size() > 0) {
                database.beginTransaction();
                if (dialogsToUpdate.size() > 0) {
                    ArrayList<Long> dids = new ArrayList<>();
                    SQLitePreparedStatement state = database.executeFast("UPDATE dialogs SET unread_count = ? WHERE did = ?");
                    for (int a = 0; a < dialogsToUpdate.size(); a++) {
                        long did = dialogsToUpdate.keyAt(a);
                        if (isForum(did, FORUM_TYPE_CHAT | FORUM_TYPE_BOT | FORUM_TYPE_DIRECT)) {
                            dialogsToUpdate.removeAt(a);
                            a--;
                            continue;
                        }
                        int prevUnreadCount = 0;
                        int newCount = dialogsToUpdate.valueAt(a);
                        SQLiteCursor cursor = database.queryFinalized("SELECT unread_count FROM dialogs WHERE did = " + did);
                        if (cursor.next()) {
                            prevUnreadCount = cursor.intValue(0);
                        }
                        cursor.dispose();
                        if (prevUnreadCount == newCount) {
                            dialogsToUpdate.removeAt(a);
                            a--;
                            continue;
                        }

                        state.requery();
                        state.bindInteger(1, newCount);
                        state.bindLong(2, did);
                        state.step();
                        dids.add(did);
                    }
                    state.dispose();
                    updateWidgets(dids);
                }
                if (dialogsToUpdateMentions.size() > 0) {
                    SQLitePreparedStatement state = database.executeFast("UPDATE dialogs SET unread_count_i = ? WHERE did = ?");
                    for (int a = 0; a < dialogsToUpdateMentions.size(); a++) {
                        long did = dialogsToUpdateMentions.keyAt(a);
                        if (isForum(did, FORUM_TYPE_CHAT | FORUM_TYPE_BOT | FORUM_TYPE_DIRECT)) {
                            dialogsToUpdateMentions.removeAt(a);
                            a--;
                            continue;
                        }
                        state.requery();
                        state.bindInteger(1, dialogsToUpdateMentions.valueAt(a));
                        state.bindLong(2, did);
                        state.step();
                    }
                    state.dispose();
                }
                database.commitTransaction();
            }
            updateFiltersReadCounter(dialogsToUpdate, dialogsToUpdateMentions, true);

            getMessagesController().processDialogsUpdateRead(dialogsToUpdate, dialogsToUpdateMentions);
            if (!channelMentionsToReload.isEmpty()) {
                getMessagesController().reloadMentionsCountForChannels(channelMentionsToReload);
            }
        } catch (Exception e) {
            checkSQLException(e);
        }
    }

    private static boolean isEmpty(SparseArray<?> array) {
        return array == null || array.size() == 0;
    }

    private static boolean isEmpty(LongSparseIntArray array) {
        return array == null || array.size() == 0;
    }

    private static boolean isEmpty(List<?> array) {
        return array == null || array.isEmpty();
    }

    private static boolean isEmpty(SparseIntArray array) {
        return array == null || array.size() == 0;
    }

    private static boolean isEmpty(LongSparseArray<?> array) {
        return array == null || array.size() == 0;
    }

    public void updateDialogsWithReadMessages(LongSparseIntArray inbox, LongSparseIntArray outbox, LongSparseArray<ArrayList<Integer>> mentions, LongSparseIntArray stillUnread, boolean useQueue) {
        if (isEmpty(inbox) && isEmpty(outbox) && isEmpty(mentions) && isEmpty(stillUnread)) {
            return;
        }
        if (useQueue) {
            storageQueue.postRunnable(() -> updateDialogsWithReadMessagesInternal(null, inbox, outbox, mentions, stillUnread));
        } else {
            updateDialogsWithReadMessagesInternal(null, inbox, outbox, mentions, stillUnread);
        }
    }

    public void updateChatParticipants(TLRPC.ChatParticipants participants) {
        if (participants == null) {
            return;
        }
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            try {
                cursor = database.queryFinalized("SELECT info, pinned, online, inviter FROM chat_settings_v2 WHERE uid = " + participants.chat_id);
                TLRPC.ChatFull info = null;
                ArrayList<TLRPC.User> loadedUsers = new ArrayList<>();
                if (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(0);
                    if (data != null) {
                        info = TLRPC.ChatFull.TLdeserialize(data, data.readInt32(false), false);
                        data.reuse();
                        info.pinned_msg_id = cursor.intValue(1);
                        info.online_count = cursor.intValue(2);
                        info.inviterId = cursor.longValue(3);
                    }
                }
                cursor.dispose();
                cursor = null;
                if (info instanceof TLRPC.TL_chatFull) {
                    info.participants = participants;
                    TLRPC.ChatFull finalInfo = info;
                    AndroidUtilities.runOnUIThread(() -> getNotificationCenter().postNotificationName(NotificationCenter.chatInfoDidLoad, finalInfo, 0, false, false));

                    SQLitePreparedStatement state = database.executeFast("REPLACE INTO chat_settings_v2 VALUES(?, ?, ?, ?, ?, ?, ?)");
                    NativeByteBuffer data = new NativeByteBuffer(info.getObjectSize());
                    info.serializeToStream(data);
                    state.bindLong(1, info.id);
                    state.bindByteBuffer(2, data);
                    state.bindInteger(3, info.pinned_msg_id);
                    state.bindInteger(4, info.online_count);
                    state.bindLong(5, info.inviterId);
                    state.bindInteger(6, info.invitesCount);
                    state.bindInteger(7, info.participants_count);
                    state.step();
                    state.dispose();
                    data.reuse();
                }
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
        });
    }

    public void loadChannelAdmins(long chatId) {
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            try {
                cursor = database.queryFinalized("SELECT uid, data FROM channel_admins_v3 WHERE did = " + chatId);
                LongSparseArray<TLRPC.ChannelParticipant> ids = new LongSparseArray<>();
                while (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(1);
                    if (data != null) {
                        TLRPC.ChannelParticipant participant = TLRPC.ChannelParticipant.TLdeserialize(data, data.readInt32(false), false);
                        data.reuse();
                        if (participant != null) {
                            ids.put(cursor.longValue(0), participant);
                        }
                    }
                }
                cursor.dispose();
                cursor = null;
                getMessagesController().processLoadedChannelAdmins(ids, chatId, true);
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
        });
    }

    public void putChannelAdmins(long chatId, LongSparseArray<TLRPC.ChannelParticipant> ids) {
        storageQueue.postRunnable(() -> {
            SQLitePreparedStatement state = null;
            try {
                database.executeFast("DELETE FROM channel_admins_v3 WHERE did = " + chatId).stepThis().dispose();
                database.beginTransaction();
                state = database.executeFast("REPLACE INTO channel_admins_v3 VALUES(?, ?, ?)");
                NativeByteBuffer data;
                for (int a = 0; a < ids.size(); a++) {
                    state.requery();
                    state.bindLong(1, chatId);
                    state.bindLong(2, ids.keyAt(a));
                    TLRPC.ChannelParticipant participant = ids.valueAt(a);
                    data = new NativeByteBuffer(participant.getObjectSize());
                    participant.serializeToStream(data);
                    state.bindByteBuffer(3, data);
                    state.step();
                    data.reuse();
                }
                state.dispose();
                state = null;
                database.commitTransaction();
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (database != null) {
                    database.commitTransaction();
                }
                if (state != null) {
                    state.dispose();
                }
            }
        });
    }

    public void updateChannelUsers(long channelId, ArrayList<TLRPC.ChannelParticipant> participants) {
        storageQueue.postRunnable(() -> {
            SQLitePreparedStatement state = null;
            try {
                long did = -channelId;
                database.executeFast("DELETE FROM channel_users_v2 WHERE did = " + did).stepThis().dispose();
                database.beginTransaction();
                state = database.executeFast("REPLACE INTO channel_users_v2 VALUES(?, ?, ?, ?)");
                NativeByteBuffer data;
                int date = (int) (System.currentTimeMillis() / 1000);
                for (int a = 0; a < participants.size(); a++) {
                    TLRPC.ChannelParticipant participant = participants.get(a);
                    state.requery();
                    state.bindLong(1, did);
                    state.bindLong(2, MessageObject.getPeerId(participant.peer));
                    state.bindInteger(3, date);
                    data = new NativeByteBuffer(participant.getObjectSize());
                    participant.serializeToStream(data);
                    state.bindByteBuffer(4, data);
                    state.step();
                    data.reuse();
                    date--;
                }
                state.dispose();
                state = null;
                database.commitTransaction();
                loadChatInfo(channelId, true, null, false, true);
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (database != null) {
                    database.commitTransaction();
                }
                if (state != null) {
                    state.dispose();
                }
            }
        });
    }

    public void saveBotCache(String key, TLObject result) {
        if (result == null || TextUtils.isEmpty(key)) {
            return;
        }
        storageQueue.postRunnable(() -> {
            SQLitePreparedStatement state = null;
            try {
                int currentDate = getConnectionsManager().getCurrentTime();
                if (result instanceof TLRPC.TL_messages_botCallbackAnswer) {
                    currentDate += ((TLRPC.TL_messages_botCallbackAnswer) result).cache_time;
                } else if (result instanceof TLRPC.TL_messages_botResults) {
                    currentDate += ((TLRPC.TL_messages_botResults) result).cache_time;
                }
                state = database.executeFast("REPLACE INTO botcache VALUES(?, ?, ?)");
                NativeByteBuffer data = new NativeByteBuffer(result.getObjectSize());
                result.serializeToStream(data);
                state.bindString(1, key);
                state.bindInteger(2, currentDate);
                state.bindByteBuffer(3, data);
                state.step();
                state.dispose();
                state = null;
                data.reuse();
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (state !=  null) {
                    state.dispose();
                }
            }
        });
    }

    public void getBotCache(String key, RequestDelegate requestDelegate) {
        if (key == null || requestDelegate == null) {
            return;
        }
        int currentDate = getConnectionsManager().getCurrentTime();
        storageQueue.postRunnable(() -> {
            TLObject result = null;
            SQLiteCursor cursor = null;
            try {
                database.executeFast("DELETE FROM botcache WHERE date < " + currentDate).stepThis().dispose();
                cursor = database.queryFinalized("SELECT data FROM botcache WHERE id = ?", key);
                if (cursor.next()) {
                    try {
                        NativeByteBuffer data = cursor.byteBufferValue(0);
                        if (data != null) {
                            int constructor = data.readInt32(false);
                            if (constructor == TLRPC.TL_messages_botCallbackAnswer.constructor) {
                                result = TLRPC.TL_messages_botCallbackAnswer.TLdeserialize(data, constructor, false);
                            } else {
                                result = TLRPC.messages_BotResults.TLdeserialize(data, constructor, false);
                            }
                            data.reuse();
                        }
                    } catch (Exception e) {
                        checkSQLException(e);
                    }
                }
                cursor.dispose();
                cursor = null;
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                requestDelegate.run(result, null);
                if (cursor != null) {
                    cursor.dispose();
                }
            }
        });
    }

    public ArrayList<TLRPC.UserFull> loadUserInfos(HashSet<Long> uids) {
        ArrayList<TLRPC.UserFull> arrayList = new ArrayList<>();
        try {
            String ids = TextUtils.join(",", uids);
            SQLiteCursor cursor = database.queryFinalized("SELECT info, pinned FROM user_settings WHERE uid IN(" + ids + ")");
            while (cursor.next()) {
                NativeByteBuffer data = cursor.byteBufferValue(0);
                if (data != null) {
                    TLRPC.UserFull info = TLRPC.UserFull.TLdeserialize(data, data.readInt32(false), false);
                    info.pinned_msg_id = cursor.intValue(1);
                    arrayList.add(info);
                    data.reuse();

                }
            }
            cursor.dispose();
            cursor = null;
        } catch (Exception e) {
            checkSQLException(e);
        }
        return arrayList;
    }

    public void loadUserInfo(TLRPC.User user, boolean force, int classGuid, int fromMessageId) {
        if (user == null) {
            return;
        }
        storageQueue.postRunnable(() -> {
            HashMap<Integer, MessageObject> pinnedMessagesMap = new HashMap<>();
            ArrayList<Integer> pinnedMessages = new ArrayList<>();
            int totalPinnedCount = 0;
            boolean pinnedEndReached = false;

            TLRPC.UserFull info = null;
            SQLiteCursor cursor = null;
            try {
                cursor = database.queryFinalized("SELECT info, pinned FROM user_settings WHERE uid = " + user.id);
                if (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(0);
                    if (data != null) {
                        info = TLRPC.UserFull.TLdeserialize(data, data.readInt32(false), false);
                        info.pinned_msg_id = cursor.intValue(1);
                        data.reuse();
                    }
                }
                cursor.dispose();
                cursor = null;

                cursor = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT mid FROM chat_pinned_v2 WHERE uid = %d ORDER BY mid DESC", user.id));
                while (cursor.next()) {
                    int id = cursor.intValue(0);
                    pinnedMessages.add(id);
                    pinnedMessagesMap.put(id, null);
                }
                cursor.dispose();
                cursor = null;

                cursor = database.queryFinalized("SELECT count, end FROM chat_pinned_count WHERE uid = " + user.id);
                if (cursor.next()) {
                    totalPinnedCount = cursor.intValue(0);
                    pinnedEndReached = cursor.intValue(1) != 0;
                }
                cursor.dispose();
                cursor = null;

                if (info != null && info.pinned_msg_id != 0) {
                    if (pinnedMessages.isEmpty() || info.pinned_msg_id > pinnedMessages.get(0)) {
                        pinnedMessages.clear();
                        pinnedMessages.add(info.pinned_msg_id);
                        pinnedMessagesMap.put(info.pinned_msg_id, null);
                    }
                }
                if (!pinnedMessages.isEmpty()) {
                    ArrayList<MessageObject> messageObjects = getMediaDataController().loadPinnedMessages(user.id, 0, pinnedMessages, false);
                    if (messageObjects != null) {
                        for (int a = 0, N = messageObjects.size(); a < N; a++) {
                            MessageObject messageObject = messageObjects.get(a);
                            pinnedMessagesMap.put(messageObject.getId(), messageObject);
                        }
                    }
                }
                ArrayList<Long> chatsToLoad = new ArrayList<>();
                if (info != null && (info.flags2 & 64) != 0 && info.personal_channel_id != 0) {
                    chatsToLoad.add(info.personal_channel_id);
                }
                if (!chatsToLoad.isEmpty()) {
                    ArrayList<TLRPC.Chat> chats = new ArrayList<>();
                    getChatsInternal(TextUtils.join(",", chatsToLoad), chats);
                    AndroidUtilities.runOnUIThread(() -> {
                        getMessagesController().putChats(chats, true);
                    });
                }
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                getMessagesController().processUserInfo(user, info, true, force, classGuid, pinnedMessages, pinnedMessagesMap, totalPinnedCount, pinnedEndReached);
                if (cursor != null) {
                    cursor.dispose();
                }
            }
        });
    }

    public void updateUserInfo(TLRPC.UserFull info, boolean ifExist) {
        storageQueue.postRunnable(() -> {
            long id = info.user != null ? info.user.id : info.id;
            SQLiteCursor cursor = null;
            SQLitePreparedStatement state = null;
            try {
                if (ifExist) {
                    cursor = database.queryFinalized("SELECT uid FROM user_settings WHERE uid = " + id);
                    boolean exist = cursor.next();
                    cursor.dispose();
                    cursor = null;
                    if (!exist) {
                        return;
                    }
                }
                state = database.executeFast("REPLACE INTO user_settings VALUES(?, ?, ?)");
                NativeByteBuffer data = new NativeByteBuffer(info.getObjectSize());
                info.serializeToStream(data);
                state.bindLong(1, id);
                state.bindByteBuffer(2, data);
                state.bindInteger(3, info.pinned_msg_id);
                state.step();
                state.dispose();
                state = null;
                data.reuse();
                if ((info.flags & 2048) != 0) {
                    state = database.executeFast("UPDATE dialogs SET folder_id = ? WHERE did = ?");
                    state.bindInteger(1, info.folder_id);
                    state.bindLong(2, id);
                    state.step();
                    state.dispose();
                    state = null;
                    unknownDialogsIds.remove(id);
                }
                if ((info.flags & 16384) != 0) {
                    state = database.executeFast("UPDATE dialogs SET ttl_period = ? WHERE did = ?");
                    state.bindInteger(1, info.ttl_period);
                    state.bindLong(2, id);
                    state.step();
                    state.dispose();
                    state = null;
                }
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (state != null) {
                    state.dispose();
                }
                if (cursor != null) {
                    cursor.dispose();
                }
            }
        });
    }

    public void updateUserInfoContactBlocked(long userId, TL_account.RequirementToContact value) {
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            SQLitePreparedStatement state = null;
            try {
                TLRPC.UserFull userFull = null;
                cursor = database.queryFinalized("SELECT uid, info, pinned FROM user_settings WHERE uid = " + userId);
                boolean exist = cursor.next();
                if (exist) {
                    NativeByteBuffer data = cursor.byteBufferValue(1);
                    userFull = TLRPC.UserFull.TLdeserialize(data, data.readInt32(true), true);
                    if (userFull != null) {
                        userFull.pinned_msg_id = cursor.intValue(2);
                    }
                    data.reuse();
                }
                cursor.dispose();
                cursor = null;
                if (!exist || userFull == null) {
                    return;
                }
                if (!UserObject.applyRequirementToContact(userFull, value)) {
                    return;
                }
                state = database.executeFast("REPLACE INTO user_settings VALUES(?, ?, ?)");
                NativeByteBuffer data = new NativeByteBuffer(userFull.getObjectSize());
                userFull.serializeToStream(data);
                state.bindLong(1, userId);
                state.bindByteBuffer(2, data);
                state.bindInteger(3, userFull.pinned_msg_id);
                state.step();
                state.dispose();
                state = null;
                data.reuse();
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (state != null) {
                    state.dispose();
                }
                if (cursor != null) {
                    cursor.dispose();
                }
            }
        });
    }

    public void saveChatInviter(long chatId, long inviterId) {
        storageQueue.postRunnable(() -> {
            SQLitePreparedStatement state = null;
            try {
                state = database.executeFast("UPDATE chat_settings_v2 SET inviter = ? WHERE uid = ?");
                state.requery();
                state.bindLong(1, inviterId);
                state.bindLong(2, chatId);
                state.step();
                state.dispose();
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (state != null) {
                    state.dispose();
                }
            }
        });
    }

    public void saveChatLinksCount(long chatId, int linksCount) {
        storageQueue.postRunnable(() -> {
            SQLitePreparedStatement state = null;
            try {
                state = database.executeFast("UPDATE chat_settings_v2 SET links = ? WHERE uid = ?");
                state.requery();
                state.bindInteger(1, linksCount);
                state.bindLong(2, chatId);
                state.step();
                state.dispose();
                state = null;
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (state != null) {
                    state.dispose();
                }
            }
        });
    }

    public void updateChatInfo(TLRPC.ChatFull info, boolean ifExist) {
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            SQLitePreparedStatement state = null;
            try {
                int currentOnline = -1;
                int inviter = 0;
                int links = 0;
                cursor = database.queryFinalized("SELECT online, inviter, links FROM chat_settings_v2 WHERE uid = " + info.id);
                if (cursor.next()) {
                    currentOnline = cursor.intValue(0);
                    info.inviterId = cursor.longValue(1);
                    links = cursor.intValue(2);
                }
                cursor.dispose();
                cursor = null;
                if (ifExist && currentOnline == -1) {
                    return;
                }

                if (currentOnline >= 0 && (info.flags & 8192) == 0) {
                    info.online_count = currentOnline;
                }

                if (links >= 0) {
                    info.invitesCount = links;
                }

                state = database.executeFast("REPLACE INTO chat_settings_v2 VALUES(?, ?, ?, ?, ?, ?, ?)");
                NativeByteBuffer data = new NativeByteBuffer(info.getObjectSize());
                info.serializeToStream(data);
                state.bindLong(1, info.id);
                state.bindByteBuffer(2, data);
                state.bindInteger(3, info.pinned_msg_id);
                state.bindInteger(4, info.online_count);
                state.bindLong(5, info.inviterId);
                state.bindInteger(6, info.invitesCount);
                state.bindInteger(7, info.participants_count);
                state.step();
                state.dispose();
                state = null;
                data.reuse();

                if (info instanceof TLRPC.TL_channelFull) {
                    cursor = database.queryFinalized("SELECT inbox_max, outbox_max FROM dialogs WHERE did = " + (-info.id));
                    if (cursor.next()) {
                        int inbox_max = cursor.intValue(0);
                        if (inbox_max < info.read_inbox_max_id) {
                            int outbox_max = cursor.intValue(1);

                            state = database.executeFast("UPDATE dialogs SET unread_count = ?, inbox_max = ?, outbox_max = ? WHERE did = ?");
                            state.bindInteger(1, info.unread_count);
                            state.bindInteger(2, info.read_inbox_max_id);
                            state.bindInteger(3, Math.max(outbox_max, info.read_outbox_max_id));
                            state.bindLong(4, -info.id);
                            state.step();
                            state.dispose();
                        }
                    }
                    cursor.dispose();
                    cursor = null;
                }
                if ((info.flags & 2048) != 0) {
                    state = database.executeFast("UPDATE dialogs SET folder_id = ? WHERE did = ?");
                    state.bindInteger(1, info.folder_id);
                    state.bindLong(2, -info.id);
                    state.step();
                    state.dispose();
                    state = null;
                    unknownDialogsIds.remove(-info.id);
                }

                state = database.executeFast("UPDATE dialogs SET ttl_period = ? WHERE did = ?");
                state.bindInteger(1, info.ttl_period);
                state.bindLong(2, -info.id);
                state.step();
                state.dispose();
                state = null;
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
                if (state != null) {
                    state.dispose();
                }
            }
        });
    }

    public void updateChatOnlineCount(long channelId, int onlineCount) {
        storageQueue.postRunnable(() -> {
            SQLitePreparedStatement state = null;
            try {
                state = database.executeFast("UPDATE chat_settings_v2 SET online = ? WHERE uid = ?");
                state.requery();
                state.bindInteger(1, onlineCount);
                state.bindLong(2, channelId);
                state.step();
                state.dispose();
                state = null;
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (state != null) {
                    state.dispose();
                }
            }
        });
    }

    public void updatePinnedMessages(long dialogId, ArrayList<Integer> ids, boolean pin, int totalCount, int maxId, boolean end, HashMap<Integer, MessageObject> messages) {
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            SQLiteCursor cursor2 = null;
            SQLitePreparedStatement state = null;
            try {
                if (pin) {
                    database.beginTransaction();
                    int alreadyAdded = 0;
                    boolean endReached;
                    if (messages != null) {
                        if (maxId == 0) {
                            database.executeFast("DELETE FROM chat_pinned_v2 WHERE uid = " + dialogId).stepThis().dispose();
                        }
                    } else {
                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT COUNT(mid) FROM chat_pinned_v2 WHERE uid = %d AND mid IN (%s)", dialogId, TextUtils.join(",", ids)));
                        alreadyAdded = cursor.next() ? cursor.intValue(0) : 0;
                        cursor.dispose();
                        cursor = null;
                    }
                    state = database.executeFast("REPLACE INTO chat_pinned_v2 VALUES(?, ?, ?)");
                    for (int a = 0, N = ids.size(); a < N; a++) {
                        Integer id = ids.get(a);
                        state.requery();
                        state.bindLong(1, dialogId);
                        state.bindInteger(2, id);
                        MessageObject message = null;
                        if (messages != null) {
                            message = messages.get(id);
                        }
                        NativeByteBuffer data = null;
                        if (message != null) {
                            data = new NativeByteBuffer(message.messageOwner.getObjectSize());
                            message.messageOwner.serializeToStream(data);
                            state.bindByteBuffer(3, data);
                        } else {
                            state.bindNull(3);
                        }
                        state.step();
                        if (data != null) {
                            data.reuse();
                        }
                    }
                    state.dispose();
                    state = null;
                    database.commitTransaction();

                    cursor = database.queryFinalized(String.format(Locale.US, "SELECT COUNT(mid) FROM chat_pinned_v2 WHERE uid = %d", dialogId));
                    int newCount1 = cursor.next() ? cursor.intValue(0) : 0;
                    cursor.dispose();
                    cursor = null;

                    int newCount;
                    if (messages != null) {
                        newCount = Math.max(totalCount, newCount1);
                        endReached = end;
                    } else {
                        cursor2 = database.queryFinalized(String.format(Locale.US, "SELECT count, end FROM chat_pinned_count WHERE uid = %d", dialogId));
                        int newCount2;
                        if (cursor2.next()) {
                            newCount2 = cursor2.intValue(0);
                            endReached = cursor2.intValue(1) != 0;
                        } else {
                            newCount2 = 0;
                            endReached = false;
                        }
                        cursor2.dispose();
                        cursor2 = null;
                        newCount = Math.max(newCount2 + (ids.size() - alreadyAdded), newCount1);
                    }

                    state = database.executeFast("REPLACE INTO chat_pinned_count VALUES(?, ?, ?)");
                    state.requery();
                    state.bindLong(1, dialogId);
                    state.bindInteger(2, newCount);
                    state.bindInteger(3, endReached ? 1 : 0);
                    state.step();
                    state.dispose();
                    state = null;

                    AndroidUtilities.runOnUIThread(() -> getNotificationCenter().postNotificationName(NotificationCenter.didLoadPinnedMessages, dialogId, ids, true, null, messages, maxId, newCount, endReached));
                } else {
                    int newCount;
                    boolean endReached;
                    if (ids == null) {
                        database.executeFast("DELETE FROM chat_pinned_v2 WHERE uid = " + dialogId).stepThis().dispose();
                        if (DialogObject.isChatDialog(dialogId)) {
                            database.executeFast(String.format(Locale.US, "UPDATE chat_settings_v2 SET pinned = 0 WHERE uid = %d", -dialogId)).stepThis().dispose();
                        } else {
                            database.executeFast(String.format(Locale.US, "UPDATE user_settings SET pinned = 0 WHERE uid = %d", dialogId)).stepThis().dispose();
                        }
                        newCount = 0;
                        endReached = true;
                    } else {
                        String idsStr = TextUtils.join(",", ids);
                        if (DialogObject.isChatDialog(dialogId)) {
                            database.executeFast(String.format(Locale.US, "UPDATE chat_settings_v2 SET pinned = 0 WHERE uid = %d AND pinned IN (%s)", -dialogId, idsStr)).stepThis().dispose();
                        } else {
                            database.executeFast(String.format(Locale.US, "UPDATE user_settings SET pinned = 0 WHERE uid = %d AND pinned IN (%s)", dialogId, idsStr)).stepThis().dispose();
                        }

                        database.executeFast(String.format(Locale.US, "DELETE FROM chat_pinned_v2 WHERE uid = %d AND mid IN(%s)", dialogId, idsStr)).stepThis().dispose();

                        cursor = database.queryFinalized("SELECT changes()");
                        int updatedCount = cursor.next() ? cursor.intValue(0) : 0;
                        cursor.dispose();
                        cursor = null;

                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT COUNT(mid) FROM chat_pinned_v2 WHERE uid = %d", dialogId));
                        int newCount1 = cursor.next() ? cursor.intValue(0) : 0;
                        cursor.dispose();
                        cursor = null;

                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT count, end FROM chat_pinned_count WHERE uid = %d", dialogId));
                        int newCount2;
                        if (cursor.next()) {
                            newCount2 = Math.max(0, cursor.intValue(0) - updatedCount);
                            endReached = cursor.intValue(1) != 0;
                        } else {
                            newCount2 = 0;
                            endReached = false;
                        }
                        cursor.dispose();
                        cursor = null;
                        newCount = Math.max(newCount1, newCount2);
                    }

                    state = database.executeFast("REPLACE INTO chat_pinned_count VALUES(?, ?, ?)");
                    state.requery();
                    state.bindLong(1, dialogId);
                    state.bindInteger(2, newCount);
                    state.bindInteger(3, endReached ? 1 : 0);
                    state.step();
                    state.dispose();
                    state = null;

                    AndroidUtilities.runOnUIThread(() -> getNotificationCenter().postNotificationName(NotificationCenter.didLoadPinnedMessages, dialogId, ids, false, null, messages, maxId, newCount, endReached));
                }
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (database != null) {
                    database.commitTransaction();
                }
                if (cursor != null) {
                    cursor.dispose();
                }
                if (state != null) {
                    state.dispose();
                }
            }
        });
    }

    public void updateChatInfo(long chatId, long userId, int what, long invited_id, int version) {
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            try {
                cursor = database.queryFinalized("SELECT info, pinned, online, inviter FROM chat_settings_v2 WHERE uid = " + chatId);
                TLRPC.ChatFull info = null;
                ArrayList<TLRPC.User> loadedUsers = new ArrayList<>();
                if (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(0);
                    if (data != null) {
                        info = TLRPC.ChatFull.TLdeserialize(data, data.readInt32(false), false);
                        data.reuse();
                        info.pinned_msg_id = cursor.intValue(1);
                        info.online_count = cursor.intValue(2);
                        info.inviterId = cursor.longValue(3);
                    }
                }
                cursor.dispose();
                cursor = null;
                if (info instanceof TLRPC.TL_chatFull) {
                    if (what == 1) {
                        for (int a = 0; a < info.participants.participants.size(); a++) {
                            TLRPC.ChatParticipant participant = info.participants.participants.get(a);
                            if (participant.user_id == userId) {
                                info.participants.participants.remove(a);
                                break;
                            }
                        }
                    } else if (what == 0) {
                        for (TLRPC.ChatParticipant part : info.participants.participants) {
                            if (part.user_id == userId) {
                                return;
                            }
                        }
                        TLRPC.TL_chatParticipant participant = new TLRPC.TL_chatParticipant();
                        participant.user_id = userId;
                        participant.inviter_id = invited_id;
                        participant.date = getConnectionsManager().getCurrentTime();
                        info.participants.participants.add(participant);
                    } else if (what == 2) {
                        for (int a = 0; a < info.participants.participants.size(); a++) {
                            TLRPC.ChatParticipant participant = info.participants.participants.get(a);
                            if (participant.user_id == userId) {
                                TLRPC.ChatParticipant newParticipant;
                                if (invited_id == 1) {
                                    newParticipant = new TLRPC.TL_chatParticipantAdmin();
                                } else {
                                    newParticipant = new TLRPC.TL_chatParticipant();
                                }
                                newParticipant.user_id = participant.user_id;
                                newParticipant.date = participant.date;
                                newParticipant.inviter_id = participant.inviter_id;
                                info.participants.participants.set(a, newParticipant);
                                break;
                            }
                        }
                    }
                    info.participants.version = version;

                    TLRPC.ChatFull finalInfo = info;
                    AndroidUtilities.runOnUIThread(() -> getNotificationCenter().postNotificationName(NotificationCenter.chatInfoDidLoad, finalInfo, 0, false, false));

                    SQLitePreparedStatement state = database.executeFast("REPLACE INTO chat_settings_v2 VALUES(?, ?, ?, ?, ?, ?, ?)");
                    NativeByteBuffer data = new NativeByteBuffer(info.getObjectSize());
                    info.serializeToStream(data);
                    state.bindLong(1, chatId);
                    state.bindByteBuffer(2, data);
                    state.bindInteger(3, info.pinned_msg_id);
                    state.bindInteger(4, info.online_count);
                    state.bindLong(5, info.inviterId);
                    state.bindInteger(6, info.invitesCount);
                    state.bindInteger(7, info.participants_count);
                    state.step();
                    state.dispose();
                    data.reuse();
                }
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
        });
    }

    public boolean isMigratedChat(long chatId) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        boolean[] result = new boolean[1];
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            try {
                cursor = database.queryFinalized("SELECT info FROM chat_settings_v2 WHERE uid = " + chatId);
                TLRPC.ChatFull info = null;
                ArrayList<TLRPC.User> loadedUsers = new ArrayList<>();
                if (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(0);
                    if (data != null) {
                        info = TLRPC.ChatFull.TLdeserialize(data, data.readInt32(false), false);
                        data.reuse();
                    }
                }
                cursor.dispose();
                cursor = null;
                result[0] = info instanceof TLRPC.TL_channelFull && info.migrated_from_chat_id != 0;
                countDownLatch.countDown();
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
                countDownLatch.countDown();
            }
        });
        try {
            countDownLatch.await();
        } catch (Exception e) {
            checkSQLException(e);
        }
        return result[0];
    }

    public TLRPC.Message getMessage(long dialogId, long msgId) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicReference<TLRPC.Message> ref = new AtomicReference<>();
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            try {
                cursor = database.queryFinalized("SELECT data FROM messages_v2 WHERE uid = " + dialogId + " AND mid = " + msgId + " LIMIT 1");
                while (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(0);
                    if (data != null) {
                        TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                        data.reuse();
                        ref.set(message);
                    }
                }
                cursor.dispose();
                cursor = null;
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
                countDownLatch.countDown();
            }
        });
        try {
            countDownLatch.await();
        } catch (Exception e) {
            checkSQLException(e);
        }
        return ref.get();
    }

    private TLRPC.Message getMessageInternal(long dialogId, long msgId) {
        SQLiteCursor cursor = null;
        TLRPC.Message result = null;
        try {
            cursor = database.queryFinalized("SELECT data FROM messages_v2 WHERE uid = " + dialogId + " AND mid = " + msgId + " LIMIT 1");
            while (cursor.next()) {
                NativeByteBuffer data = cursor.byteBufferValue(0);
                if (data != null) {
                    TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                    if (message != null) {
                        message.readAttachPath(data, getUserConfig().clientUserId);
                    }
                    data.reuse();

                    result = message;
                }
            }
            cursor.dispose();
            cursor = null;
        } catch (Exception e) {
            checkSQLException(e);
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
        return result;
    }



    public boolean hasInviteMeMessage(long chatId) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        boolean[] result = new boolean[1];
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            try {
                long selfId = getUserConfig().getClientUserId();
                cursor = database.queryFinalized("SELECT data FROM messages_v2 WHERE uid = " + -chatId + " AND out = 0 ORDER BY mid DESC LIMIT 100");
                while (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(0);
                    if (data != null) {
                        TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                        data.reuse();
                        if (message.action instanceof TLRPC.TL_messageActionChatAddUser && message.action.users.contains(selfId)) {
                            result[0] = true;
                            break;
                        }
                    }
                }
                cursor.dispose();
                cursor = null;
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
                countDownLatch.countDown();
            }
        });
        try {
            countDownLatch.await();
        } catch (Exception e) {
            checkSQLException(e);
        }
        return result[0];
    }

    public HashMap<Long, Integer> getSmallGroupsParticipantsCount() {
        HashMap<Long, Integer> result = new HashMap<>();

        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized("SELECT uid, info, participants_count FROM chat_settings_v2 WHERE participants_count > 1");
            while (cursor.next()) {
                TLRPC.ChatFull info = null;
                long id = cursor.longValue(0);
                NativeByteBuffer data = cursor.byteBufferValue(1);
                int participants_count = cursor.intValue(2);
                if (data != null) {
                    info = TLRPC.ChatFull.TLdeserialize(data, data.readInt32(false), false);
                    data.reuse();
                    // legacy groups already contain participants_count in TLRPC.Chat and not need to load chatfull
                    if (info instanceof TLRPC.TL_channelFull) {
                        result.put(id, participants_count);
                    }
                }
            }
            cursor.dispose();
            cursor = null;
        } catch (Exception e) {
            checkSQLException(e);
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
        return result;
    }

    private TLRPC.ChatFull loadChatInfoInternal(long chatId, boolean isChannel, boolean force, boolean byChannelUsers, int fromMessageId) {
        TLRPC.ChatFull info = null;
        ArrayList<TLRPC.User> loadedUsers = new ArrayList<>();
        ArrayList<TLRPC.Chat> loadedChats = new ArrayList<>();

        HashMap<Integer, MessageObject> pinnedMessagesMap = new HashMap<>();
        ArrayList<Integer> pinnedMessages = new ArrayList<>();
        int totalPinnedCount = 0;
        boolean pinnedEndReached = false;

        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized("SELECT info, pinned, online, inviter, links FROM chat_settings_v2 WHERE uid = " + chatId);
            if (cursor.next()) {
                NativeByteBuffer data = cursor.byteBufferValue(0);
                if (data != null) {
                    info = TLRPC.ChatFull.TLdeserialize(data, data.readInt32(false), false);
                    data.reuse();
                    info.pinned_msg_id = cursor.intValue(1);
                    info.online_count = cursor.intValue(2);
                    info.inviterId = cursor.longValue(3);
                    info.invitesCount = cursor.intValue(4);
                }
            }
            cursor.dispose();
            cursor = null;

            ArrayList<Long> usersToLoad = new ArrayList<>();
            ArrayList<Long> chatsToLoad = new ArrayList<>();
            if (info instanceof TLRPC.TL_communityFull) {
                if (info.linked_peers != null) {
                    for (int a = 0, N = info.linked_peers.size(); a < N; a++) {
                        final TL_communities.CommunityPeer c = info.linked_peers.get(a);
                        final long peerId = DialogObject.getPeerDialogId(c.peer);
                        if (peerId > 0) {
                            usersToLoad.add(peerId);
                        } else {
                            chatsToLoad.add(-peerId);
                        }
                    }
                }
            } else if (info instanceof TLRPC.TL_chatFull) {
                for (int a = 0; a < info.participants.participants.size(); a++) {
                    TLRPC.ChatParticipant c = info.participants.participants.get(a);
                    usersToLoad.add(c.user_id);
                }
            } else if (info instanceof TLRPC.TL_channelFull) {
                cursor = database.queryFinalized("SELECT us.data, us.status, cu.data, cu.date FROM channel_users_v2 as cu LEFT JOIN users as us ON us.uid = cu.uid WHERE cu.did = " + (-chatId) + " ORDER BY cu.date DESC");
                info.participants = new TLRPC.TL_chatParticipants();
                while (cursor.next()) {
                    try {
                        TLRPC.User user = null;
                        TLRPC.ChannelParticipant participant = null;
                        NativeByteBuffer data = cursor.byteBufferValue(0);
                        if (data != null) {
                            user = TLRPC.User.TLdeserialize(data, data.readInt32(false), false);
                            data.reuse();
                        }
                        data = cursor.byteBufferValue(2);
                        if (data != null) {
                            participant = TLRPC.ChannelParticipant.TLdeserialize(data, data.readInt32(false), false);
                            data.reuse();
                        }
                        if (participant != null && participant.user_id == getUserConfig().clientUserId) {
                            user = getUserConfig().getCurrentUser();
                        }
                        if (user != null && participant != null) {
                            if (user.status != null) {
                                user.status.expires = cursor.intValue(1);
                            }
                            loadedUsers.add(user);
                            participant.date = cursor.intValue(3);
                            TLRPC.TL_chatChannelParticipant chatChannelParticipant = new TLRPC.TL_chatChannelParticipant();
                            chatChannelParticipant.user_id = MessageObject.getPeerId(participant.peer);
                            chatChannelParticipant.date = participant.date;
                            chatChannelParticipant.inviter_id = participant.inviter_id;
                            chatChannelParticipant.channelParticipant = participant;
                            info.participants.participants.add(chatChannelParticipant);
                        }
                    } catch (Exception e) {
                        checkSQLException(e);
                    }
                }
                cursor.dispose();
                cursor = null;
                for (int a = 0; a < info.bot_info.size(); a++) {
                    TL_bots.BotInfo botInfo = info.bot_info.get(a);
                    usersToLoad.add(botInfo.user_id);
                }
            }
            if (info != null && info.inviterId != 0) {
                usersToLoad.add(info.inviterId);
            }
            if (info != null && info.recent_requesters != null && !info.recent_requesters.isEmpty()) {
                for (int i = 0; i < Math.min(3, info.recent_requesters.size()); ++i) {
                    usersToLoad.add(info.recent_requesters.get(info.recent_requesters.size() - 1 - i));
                }
            }
            getUsersInternal(usersToLoad, loadedUsers);
            getChatsInternal(TextUtils.join(",", chatsToLoad), loadedChats);

            cursor = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT mid FROM chat_pinned_v2 WHERE uid = %d ORDER BY mid DESC", -chatId));
            while (cursor.next()) {
                int id = cursor.intValue(0);
                pinnedMessages.add(id);
                pinnedMessagesMap.put(id, null);
            }
            cursor.dispose();
            cursor = null;
            cursor = database.queryFinalized("SELECT count, end FROM chat_pinned_count WHERE uid = " + (-chatId));
            if (cursor.next()) {
                totalPinnedCount = cursor.intValue(0);
                pinnedEndReached = cursor.intValue(1) != 0;
            }
            cursor.dispose();
            cursor = null;

            if (info != null && info.pinned_msg_id != 0) {
                if (pinnedMessages.isEmpty() || info.pinned_msg_id > pinnedMessages.get(0)) {
                    pinnedMessages.clear();
                    pinnedMessages.add(info.pinned_msg_id);
                    pinnedMessagesMap.put(info.pinned_msg_id, null);
                }
            }
            if (!pinnedMessages.isEmpty()) {
                ArrayList<MessageObject> messageObjects = getMediaDataController().loadPinnedMessages(-chatId, isChannel ? chatId : 0, pinnedMessages, false);
                if (messageObjects != null) {
                    for (int a = 0, N = messageObjects.size(); a < N; a++) {
                        MessageObject messageObject = messageObjects.get(a);
                        pinnedMessagesMap.put(messageObject.getId(), messageObject);
                    }
                }
            }
        } catch (Exception e) {
            checkSQLException(e);
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
            getMessagesController().processChatInfo(chatId, info, loadedUsers, loadedChats, true, force, byChannelUsers, pinnedMessages, pinnedMessagesMap, totalPinnedCount, pinnedEndReached);
        }
        return info;
    }

    public TLRPC.ChatFull loadChatInfo(long chatId, boolean isChannel, CountDownLatch countDownLatch, boolean force, boolean byChannelUsers) {
        return loadChatInfo(chatId, isChannel, countDownLatch, force, byChannelUsers, 0);
    }

    public TLRPC.ChatFull loadChatInfo(long chatId, boolean isChannel, CountDownLatch countDownLatch, boolean force, boolean byChannelUsers, int fromMessageId) {
        TLRPC.ChatFull[] result = new TLRPC.ChatFull[1];
        storageQueue.postRunnable(() -> {
            result[0] = loadChatInfoInternal(chatId, isChannel, force, byChannelUsers, fromMessageId);
            if (countDownLatch != null) {
                countDownLatch.countDown();
            }
        });
        if (countDownLatch != null) {
            try {
                countDownLatch.await();
            } catch (Throwable ignore) {

            }
        }
        return result[0];
    }

    public TLRPC.ChatFull loadChatInfoInQueue(long chatId, boolean isChannel, boolean force, boolean byChannelUsers, int fromMessageId) {
        return loadChatInfoInternal(chatId, isChannel, force, byChannelUsers, fromMessageId);
    }


    public void processPendingRead(long dialogId, int maxPositiveId, int maxNegativeId, int scheduledCount) {
        int maxDate = lastSavedDate;
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            SQLitePreparedStatement state = null;
            try {
                int currentMaxId = 0;
                int unreadCount = 0;
                long last_mid = 0;
                int prevUnreadCount = 0;
                cursor = database.queryFinalized("SELECT unread_count, inbox_max, last_mid FROM dialogs WHERE did = " + dialogId);
                if (cursor.next()) {
                    prevUnreadCount = unreadCount = cursor.intValue(0);
                    currentMaxId = cursor.intValue(1);
                    last_mid = cursor.longValue(2);
                }
                cursor.dispose();
                cursor = null;
                int oldMaxId = currentMaxId;

                database.beginTransaction();

                if (!DialogObject.isEncryptedDialog(dialogId)) {
                    currentMaxId = Math.max(currentMaxId, maxPositiveId);

                    state = database.executeFast("UPDATE messages_v2 SET read_state = read_state | 1 WHERE uid = ? AND mid <= ? AND read_state IN(0,2) AND out = 0");
                    state.requery();
                    state.bindLong(1, dialogId);
                    state.bindInteger(2, currentMaxId);
                    state.step();
                    state.dispose();

                    if (currentMaxId >= last_mid) {
                        unreadCount = 0;
                    } else {
                        int updatedCount = 0;
                        cursor = database.queryFinalized("SELECT changes()");
                        if (cursor.next()) {
                            updatedCount = cursor.intValue(0) + scheduledCount;
                        }
                        cursor.dispose();
                        unreadCount = Math.max(0, unreadCount - updatedCount);
                    }

                    state = database.executeFast("DELETE FROM unread_push_messages WHERE uid = ? AND mid <= ?");
                    state.requery();
                    state.bindLong(1, dialogId);
                    state.bindInteger(2, currentMaxId);
                    state.step();
                    state.dispose();

                    state = database.executeFast("DELETE FROM unread_push_messages WHERE uid = ? AND date <= ?");
                    state.requery();
                    state.bindLong(1, dialogId);
                    state.bindInteger(2, maxDate);
                    state.step();
                    state.dispose();
                    state = null;
                } else {
                    currentMaxId = maxNegativeId;

                    state = database.executeFast("UPDATE messages_v2 SET read_state = read_state | 1 WHERE uid = ? AND mid >= ? AND read_state IN(0,2) AND out = 0");
                    state.requery();
                    state.bindLong(1, dialogId);
                    state.bindInteger(2, currentMaxId);
                    state.step();
                    state.dispose();
                    state = null;

                    if (currentMaxId <= last_mid) {
                        unreadCount = 0;
                    } else {
                        int updatedCount = 0;
                        cursor = database.queryFinalized("SELECT changes()");
                        if (cursor.next()) {
                            updatedCount = cursor.intValue(0) + scheduledCount;
                        }
                        cursor.dispose();
                        unreadCount = Math.max(0, unreadCount - updatedCount);
                    }
                }

                state = database.executeFast("UPDATE dialogs SET unread_count = ?, inbox_max = ? WHERE did = ?");
                state.requery();
                state.bindInteger(1, unreadCount);
                state.bindInteger(2, currentMaxId);
                state.bindLong(3, dialogId);
                state.step();
                state.dispose();
                state = null;

                database.commitTransaction();

                if (isForum(dialogId, FORUM_TYPE_DIRECT | FORUM_TYPE_CHAT_TABS | FORUM_TYPE_BOT)) {
                    updateTopicsWithReadFromAllInternal(dialogId, oldMaxId, currentMaxId);
                }

                //TODO topics maybe read all topics when all messages read
                if (prevUnreadCount != 0 && unreadCount == 0 && !isForum(dialogId, FORUM_TYPE_CHAT | FORUM_TYPE_BOT | FORUM_TYPE_DIRECT)) {
                    LongSparseIntArray dialogsToUpdate = new LongSparseIntArray();
                    dialogsToUpdate.put(dialogId, unreadCount);
                    updateFiltersReadCounter(dialogsToUpdate, null, true);
                }
                updateWidgets(dialogId);
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (state != null) {
                    state.dispose();
                }
                if (cursor != null) {
                    cursor.dispose();
                }
                if (database != null) {
                    database.commitTransaction();
                }
            }
        });
    }

    private void updateTopicsWithReadFromAllInternal(long dialogId, long oldMessageId, long newMessageId) {
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized(String.format(Locale.US, "SELECT topic_id FROM topics WHERE did = %d AND max_read_id < %d AND (top_message > %d OR unread_count > 0)", dialogId, newMessageId, oldMessageId));
            while (cursor.next()) {
                long topicId = cursor.longValue(0);
                updateRepliesMaxReadIdInternal(dialogId, topicId, (int) newMessageId, -1);
            }
        } catch (Exception e) {
            checkSQLException(e);
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
    }

    public void putContacts(ArrayList<TLRPC.TL_contact> contacts, boolean deleteAll) {
        if (contacts.isEmpty() && !deleteAll) {
            return;
        }
        ArrayList<TLRPC.TL_contact> contactsCopy = new ArrayList<>(contacts);
        storageQueue.postRunnable(() -> {
            SQLitePreparedStatement state = null;
            try {
                if (deleteAll) {
                    database.executeFast("DELETE FROM contacts WHERE 1").stepThis().dispose();
                }
                database.beginTransaction();
                state = database.executeFast("REPLACE INTO contacts VALUES(?, ?)");
                for (int a = 0; a < contactsCopy.size(); a++) {
                    TLRPC.TL_contact contact = contactsCopy.get(a);
                    state.requery();
                    state.bindLong(1, contact.user_id);
                    state.bindInteger(2, contact.mutual ? 1 : 0);
                    state.step();
                }
                state.dispose();
                state = null;
                database.commitTransaction();
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (state != null) {
                    state.dispose();
                }
                if (database != null) {
                    database.commitTransaction();
                }
            }
        });
    }

    public void deleteContacts(ArrayList<Long> uids) {
        if (uids == null || uids.isEmpty()) {
            return;
        }
        storageQueue.postRunnable(() -> {
            try {
                String ids = TextUtils.join(",", uids);
                database.executeFast("DELETE FROM contacts WHERE uid IN(" + ids + ")").stepThis().dispose();
            } catch (Exception e) {
                checkSQLException(e);
            }
        });
    }

    public void applyPhoneBookUpdates(String adds, String deletes) {
        if (TextUtils.isEmpty(adds)) {
            return;
        }
        storageQueue.postRunnable(() -> {
            try {
                if (adds.length() != 0) {
                    database.executeFast(String.format(Locale.US, "UPDATE user_phones_v7 SET deleted = 0 WHERE sphone IN(%s)", adds)).stepThis().dispose();
                }
                if (deletes.length() != 0) {
                    database.executeFast(String.format(Locale.US, "UPDATE user_phones_v7 SET deleted = 1 WHERE sphone IN(%s)", deletes)).stepThis().dispose();
                }
            } catch (Exception e) {
                checkSQLException(e);
            }
        });
    }

    public void putCachedPhoneBook(HashMap<String, ContactsController.Contact> contactHashMap, boolean migrate, boolean delete) {
        if (contactHashMap == null || contactHashMap.isEmpty() && !migrate && !delete) {
            return;
        }
        storageQueue.postRunnable(() -> {
            SQLitePreparedStatement state = null;
            SQLitePreparedStatement state2 = null;
            try {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.d(currentAccount + " save contacts to db " + contactHashMap.size());
                }
                database.executeFast("DELETE FROM user_contacts_v7 WHERE 1").stepThis().dispose();
                database.executeFast("DELETE FROM user_phones_v7 WHERE 1").stepThis().dispose();

                database.beginTransaction();
                state = database.executeFast("REPLACE INTO user_contacts_v7 VALUES(?, ?, ?, ?, ?)");
                state2 = database.executeFast("REPLACE INTO user_phones_v7 VALUES(?, ?, ?, ?)");
                for (HashMap.Entry<String, ContactsController.Contact> entry : contactHashMap.entrySet()) {
                    ContactsController.Contact contact = entry.getValue();
                    if (contact.phones.isEmpty() || contact.shortPhones.isEmpty()) {
                        continue;
                    }
                    state.requery();
                    state.bindString(1, contact.key);
                    state.bindInteger(2, contact.contact_id);
                    state.bindString(3, contact.first_name);
                    state.bindString(4, contact.last_name);
                    state.bindInteger(5, contact.imported);
                    state.step();
                    for (int a = 0; a < contact.phones.size(); a++) {
                        state2.requery();
                        state2.bindString(1, contact.key);
                        state2.bindString(2, contact.phones.get(a));
                        state2.bindString(3, contact.shortPhones.get(a));
                        state2.bindInteger(4, contact.phoneDeleted.get(a));
                        state2.step();
                    }
                }
                state.dispose();
                state = null;
                state2.dispose();
                state2 = null;
                database.commitTransaction();
                if (migrate) {
                    database.executeFast("DROP TABLE IF EXISTS user_contacts_v6;").stepThis().dispose();
                    database.executeFast("DROP TABLE IF EXISTS user_phones_v6;").stepThis().dispose();
                    getCachedPhoneBook(false);
                }
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (state != null) {
                    state.dispose();
                }
                if (state2 != null) {
                    state2.dispose();
                }
                if (database != null) {
                    database.commitTransaction();
                }
            }
        });
    }

    public void getCachedPhoneBook(boolean byError) {
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            try {
                cursor = database.queryFinalized("SELECT name FROM sqlite_master WHERE type='table' AND name='user_contacts_v6'");
                boolean migrate = cursor.next();
                cursor.dispose();
                cursor = null;
                if (migrate) {
                    int count = 16;
                    cursor = database.queryFinalized("SELECT COUNT(uid) FROM user_contacts_v6 WHERE 1");
                    if (cursor.next()) {
                        count = Math.min(5000, cursor.intValue(0));
                    }
                    cursor.dispose();

                    SparseArray<ContactsController.Contact> contactHashMap = new SparseArray<>(count);
                    cursor = database.queryFinalized("SELECT us.uid, us.fname, us.sname, up.phone, up.sphone, up.deleted, us.imported FROM user_contacts_v6 as us LEFT JOIN user_phones_v6 as up ON us.uid = up.uid WHERE 1");
                    while (cursor.next()) {
                        int uid = cursor.intValue(0);
                        ContactsController.Contact contact = contactHashMap.get(uid);
                        if (contact == null) {
                            contact = new ContactsController.Contact();
                            contact.first_name = cursor.stringValue(1);
                            contact.last_name = cursor.stringValue(2);
                            contact.imported = cursor.intValue(6);
                            if (contact.first_name == null) {
                                contact.first_name = "";
                            }
                            if (contact.last_name == null) {
                                contact.last_name = "";
                            }
                            contact.contact_id = uid;
                            contactHashMap.put(uid, contact);
                        }
                        String phone = cursor.stringValue(3);
                        if (phone == null) {
                            continue;
                        }
                        contact.phones.add(phone);
                        String sphone = cursor.stringValue(4);
                        if (sphone == null) {
                            continue;
                        }
                        if (sphone.length() == 8 && phone.length() != 8) {
                            sphone = PhoneFormat.stripExceptNumbers(phone);
                        }
                        contact.shortPhones.add(sphone);
                        contact.phoneDeleted.add(cursor.intValue(5));
                        contact.phoneTypes.add("");
                        if (contactHashMap.size() == 5000) {
                            break;
                        }
                    }
                    cursor.dispose();
                    cursor = null;
                    getContactsController().migratePhoneBookToV7(contactHashMap);
                    return;
                }
            } catch (Throwable e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }

            int count = 16;
            int currentContactsCount = 0;
            int start = 0;
            try {
                cursor = database.queryFinalized("SELECT COUNT(key) FROM user_contacts_v7 WHERE 1");
                if (cursor.next()) {
                    currentContactsCount = cursor.intValue(0);
                    count = Math.min(5000, currentContactsCount);
                    if (currentContactsCount > 5000) {
                        start = currentContactsCount - 5000;
                    }
                    if (BuildVars.LOGS_ENABLED) {
                        FileLog.d(currentAccount + " current cached contacts count = " + currentContactsCount);
                    }
                }
            } catch (Throwable e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }

            HashMap<String, ContactsController.Contact> contactHashMap = new HashMap<>(count);
            try {
                if (start != 0) {
                    cursor = database.queryFinalized("SELECT us.key, us.uid, us.fname, us.sname, up.phone, up.sphone, up.deleted, us.imported FROM user_contacts_v7 as us LEFT JOIN user_phones_v7 as up ON us.key = up.key WHERE 1 LIMIT " + 0 + "," + currentContactsCount);
                } else {
                    cursor = database.queryFinalized("SELECT us.key, us.uid, us.fname, us.sname, up.phone, up.sphone, up.deleted, us.imported FROM user_contacts_v7 as us LEFT JOIN user_phones_v7 as up ON us.key = up.key WHERE 1");
                }
                while (cursor.next()) {
                    String key = cursor.stringValue(0);
                    ContactsController.Contact contact = contactHashMap.get(key);
                    if (contact == null) {
                        contact = new ContactsController.Contact();
                        contact.contact_id = cursor.intValue(1);
                        contact.first_name = cursor.stringValue(2);
                        contact.last_name = cursor.stringValue(3);
                        contact.imported = cursor.intValue(7);
                        if (contact.first_name == null) {
                            contact.first_name = "";
                        }
                        if (contact.last_name == null) {
                            contact.last_name = "";
                        }
                        contactHashMap.put(key, contact);
                    }
                    String phone = cursor.stringValue(4);
                    if (phone == null) {
                        continue;
                    }
                    contact.phones.add(phone);
                    String sphone = cursor.stringValue(5);
                    if (sphone == null) {
                        continue;
                    }
                    if (sphone.length() == 8 && phone.length() != 8) {
                        sphone = PhoneFormat.stripExceptNumbers(phone);
                    }
                    contact.shortPhones.add(sphone);
                    contact.phoneDeleted.add(cursor.intValue(6));
                    contact.phoneTypes.add("");
                    if (contactHashMap.size() == 5000) {
                        break;
                    }
                }
                cursor.dispose();
                cursor = null;
            } catch (Exception e) {
                contactHashMap.clear();
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
            getContactsController().performSyncPhoneBook(contactHashMap, true, true, false, false, !byError, false);
        });
    }

    public void getContacts() {
        storageQueue.postRunnable(() -> {
            ArrayList<TLRPC.TL_contact> contacts = new ArrayList<>();
            ArrayList<TLRPC.User> users = new ArrayList<>();
            SQLiteCursor cursor = null;
            try {
                cursor = database.queryFinalized("SELECT * FROM contacts WHERE 1");
                ArrayList<Long> uids = new ArrayList<>();
                while (cursor.next()) {
                    long userId = cursor.intValue(0);
                    TLRPC.TL_contact contact = new TLRPC.TL_contact();
                    contact.user_id = userId;
                    contact.mutual = cursor.intValue(1) == 1;
                    contacts.add(contact);
                    uids.add(contact.user_id);
                }
                cursor.dispose();
                cursor = null;

                if (!uids.isEmpty()) {
                    getUsersInternal(uids, users);
                }
            } catch (Exception e) {
                contacts.clear();
                users.clear();
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
            getContactsController().processLoadedContacts(contacts, users, 1);
        });
    }

    public void getUnsentMessages(int count) {
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            SQLiteCursor cursor2 = null;
            try {
                SparseArray<TLRPC.Message> messageHashMap = new SparseArray<>();
                ArrayList<TLRPC.Message> messages = new ArrayList<>();
                ArrayList<TLRPC.Message> scheduledMessages = new ArrayList<>();
                ArrayList<TLRPC.User> users = new ArrayList<>();
                ArrayList<TLRPC.Chat> chats = new ArrayList<>();
                ArrayList<TLRPC.EncryptedChat> encryptedChats = new ArrayList<>();

                ArrayList<Long> usersToLoad = new ArrayList<>();
                ArrayList<Long> chatsToLoad = new ArrayList<>();
                ArrayList<Integer> encryptedChatIds = new ArrayList<>();
                ArrayList<TLRPC.Message> toDelete = new ArrayList<>();

                cursor = database.queryFinalized("SELECT m.read_state, m.data, m.send_state, m.mid, m.date, r.random_id, m.uid, s.seq_in, s.seq_out, m.ttl FROM messages_v2 as m LEFT JOIN randoms_v2 as r ON r.mid = m.mid AND r.uid = m.uid LEFT JOIN messages_seq as s ON m.mid = s.mid WHERE (m.mid < 0 AND m.send_state = 1) OR (m.mid > 0 AND m.send_state = 3) ORDER BY m.mid DESC LIMIT " + count);
                while (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(1);
                    if (data != null) {
                        TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                        message.send_state = cursor.intValue(2);
                        message.readAttachPath(data, getUserConfig().clientUserId);
                        data.reuse();
                        if (messageHashMap.indexOfKey(message.id) < 0) {
                            MessageObject.setUnreadFlags(message, cursor.intValue(0));
                            message.id = cursor.intValue(3);
                            message.date = cursor.intValue(4);
                            if (!cursor.isNull(5)) {
                                message.random_id = cursor.longValue(5);
                            }
                            message.dialog_id = cursor.longValue(6);
                            message.seq_in = cursor.intValue(7);
                            message.seq_out = cursor.intValue(8);
                            message.ttl = cursor.intValue(9);
                            if (message.media instanceof TLRPC.TL_messageMediaPaidMedia) {
                                toDelete.add(message); // TODO: actually send again
                            } else {
                                messages.add(message);
                            }
                            messageHashMap.put(message.id, message);

                            if (DialogObject.isEncryptedDialog(message.dialog_id)) {
                                int encryptedChatId = DialogObject.getEncryptedChatId(message.dialog_id);
                                if (!encryptedChatIds.contains(encryptedChatId)) {
                                    encryptedChatIds.add(encryptedChatId);
                                }
                            } else if (DialogObject.isUserDialog(message.dialog_id)) {
                                if (!usersToLoad.contains(message.dialog_id)) {
                                    usersToLoad.add(message.dialog_id);
                                }
                            } else {
                                if (!chatsToLoad.contains(-message.dialog_id)) {
                                    chatsToLoad.add(-message.dialog_id);
                                }
                            }

                            addUsersAndChatsFromMessage(message, usersToLoad, chatsToLoad, null);

                            if (message.send_state != 3 && (message.peer_id.channel_id == 0 && !MessageObject.isUnread(message) && !DialogObject.isEncryptedDialog(message.dialog_id) || message.id > 0)) {
                                message.send_state = 0;
                            }
                        }
                    }
                }
                cursor.dispose();
                cursor = null;

                if (!toDelete.isEmpty()) {
                    for (TLRPC.Message msg : toDelete) {
                        database.executeFast("DELETE FROM messages_v2 WHERE uid = " + msg.dialog_id + " AND mid = " + msg.id).stepThis().dispose();
                    }
                }

                cursor = database.queryFinalized("SELECT m.data, m.send_state, m.mid, m.date, r.random_id, m.uid, m.ttl FROM scheduled_messages_v2 as m LEFT JOIN randoms_v2 as r ON r.mid = m.mid AND r.uid = m.uid WHERE (m.mid < 0 AND m.send_state = 1) OR (m.mid > 0 AND m.send_state = 3) ORDER BY date ASC");
                while (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(0);
                    if (data != null) {
                        TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                        message.send_state = cursor.intValue(1);
                        message.readAttachPath(data, getUserConfig().clientUserId);
                        data.reuse();
                        if (messageHashMap.indexOfKey(message.id) < 0) {
                            message.id = cursor.intValue(2);
                            message.date = cursor.intValue(3);
                            if (!cursor.isNull(4)) {
                                message.random_id = cursor.longValue(4);
                            }
                            message.dialog_id = cursor.longValue(5);
                            message.ttl = cursor.intValue(6);
                            scheduledMessages.add(message);
                            messageHashMap.put(message.id, message);

                            if (DialogObject.isEncryptedDialog(message.dialog_id)) {
                                int encryptedChatId = DialogObject.getEncryptedChatId(message.dialog_id);
                                if (!encryptedChatIds.contains(encryptedChatId)) {
                                    encryptedChatIds.add(encryptedChatId);
                                }
                            } else if (DialogObject.isUserDialog(message.dialog_id)) {
                                if (!usersToLoad.contains(message.dialog_id)) {
                                    usersToLoad.add(message.dialog_id);
                                }
                            } else {
                                if (!chatsToLoad.contains(-message.dialog_id)) {
                                    chatsToLoad.add(-message.dialog_id);
                                }
                            }

                            addUsersAndChatsFromMessage(message, usersToLoad, chatsToLoad, null);

                            if (message.send_state != 3 && (message.peer_id.channel_id == 0 && !MessageObject.isUnread(message) && !DialogObject.isEncryptedDialog(message.dialog_id) || message.id > 0)) {
                                message.send_state = 0;
                            }
                        }
                    }
                }
                cursor.dispose();
                cursor = null;

                final long selfId = getUserConfig().getClientUserId();
                cursor = database.queryFinalized("SELECT m.data, m.send_state, m.mid, m.date, m.topic_id, m.ttl FROM quick_replies_messages as m WHERE (m.mid < 0 AND m.send_state = 1) OR (m.mid > 0 AND m.send_state = 3) ORDER BY mid DESC");
                while (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(0);
                    if (data != null) {
                        TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                        message.send_state = cursor.intValue(1);
                        message.readAttachPath(data, getUserConfig().clientUserId);
                        data.reuse();
                        if (messageHashMap.indexOfKey(message.id) < 0) {
                            message.id = cursor.intValue(2);
                            String topicName = null;
                            int topic_id = cursor.intValue(4);
                            cursor2 = database.queryFinalized("SELECT name FROM business_replies WHERE topic_id = ?", topic_id);
                            if (cursor2.next()) {
                                topicName = cursor2.stringValue(1);
                            }
                            cursor2.dispose();
                            if (topicName == null) {
                                database.executeFast("DELETE FROM quick_replies_messages WHERE mid = " + message.id + " AND topic_id = " + topic_id).stepThis().dispose();
                                continue;
                            }
                            TLRPC.TL_inputQuickReplyShortcut shortcut = new TLRPC.TL_inputQuickReplyShortcut();
                            shortcut.shortcut = topicName;
                            message.quick_reply_shortcut = shortcut;
                            message.quick_reply_shortcut_id = topic_id;
                            if (topic_id != 0) {
                                message.flags |= 1073741824;
                            }
                            message.date = cursor.intValue(3);
                            message.ttl = cursor.intValue(5);
                            scheduledMessages.add(message);
                            messageHashMap.put(message.id, message);

                            if (DialogObject.isEncryptedDialog(message.dialog_id)) {
                                int encryptedChatId = DialogObject.getEncryptedChatId(message.dialog_id);
                                if (!encryptedChatIds.contains(encryptedChatId)) {
                                    encryptedChatIds.add(encryptedChatId);
                                }
                            } else if (DialogObject.isUserDialog(message.dialog_id)) {
                                if (!usersToLoad.contains(message.dialog_id)) {
                                    usersToLoad.add(message.dialog_id);
                                }
                            } else {
                                if (!chatsToLoad.contains(-message.dialog_id)) {
                                    chatsToLoad.add(-message.dialog_id);
                                }
                            }

                            addUsersAndChatsFromMessage(message, usersToLoad, chatsToLoad, null);

                            if (message.send_state != 3 && (message.peer_id.channel_id == 0 && !MessageObject.isUnread(message) && !DialogObject.isEncryptedDialog(message.dialog_id) || message.id > 0)) {
                                message.send_state = 0;
                            }
                        }
                    }
                }
                cursor.dispose();
                cursor = null;

                if (!encryptedChatIds.isEmpty()) {
                    getEncryptedChatsInternal(TextUtils.join(",", encryptedChatIds), encryptedChats, usersToLoad);
                }

                if (!usersToLoad.isEmpty()) {
                    getUsersInternal(usersToLoad, users);
                }

                if (!chatsToLoad.isEmpty()) {
                    StringBuilder stringToLoad = new StringBuilder();
                    for (int a = 0; a < chatsToLoad.size(); a++) {
                        Long cid = chatsToLoad.get(a);
                        if (stringToLoad.length() != 0) {
                            stringToLoad.append(",");
                        }
                        stringToLoad.append(cid);
                    }
                    getChatsInternal(stringToLoad.toString(), chats);
                }

                getSendMessagesHelper().processUnsentMessages(messages, scheduledMessages, users, chats, encryptedChats);
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
        });
    }

    public boolean checkMessageByRandomId(long random_id) {
        boolean[] result = new boolean[1];
        CountDownLatch countDownLatch = new CountDownLatch(1);
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            try {
                cursor = database.queryFinalized(String.format(Locale.US, "SELECT random_id FROM randoms_v2 WHERE random_id = %d", random_id));
                if (cursor.next()) {
                    result[0] = true;
                }
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
            countDownLatch.countDown();
        });
        try {
            countDownLatch.await();
        } catch (Exception e) {
            checkSQLException(e);
        }
        return result[0];
    }

    public boolean checkMessageId(long dialogId, int mid) {
        boolean[] result = new boolean[1];
        CountDownLatch countDownLatch = new CountDownLatch(1);
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            try {
                cursor = database.queryFinalized(String.format(Locale.US, "SELECT mid FROM messages_v2 WHERE uid = %d AND mid = %d", dialogId, mid));
                if (cursor.next()) {
                    result[0] = true;
                }
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
            countDownLatch.countDown();
        });
        try {
            countDownLatch.await();
        } catch (Exception e) {
            checkSQLException(e);
        }
        return result[0];
    }

    public void getUnreadMention(long dialog_id, long topicId, IntCallback callback) {
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            try {
                int result;
                if (topicId != 0) {
                    cursor = database.queryFinalized(String.format(Locale.US, "SELECT MIN(mid) FROM messages_topics WHERE uid = %d AND topic_id = %d AND mention = 1 AND read_state IN(0, 1)", dialog_id, topicId));
                } else {
                    cursor = database.queryFinalized(String.format(Locale.US, "SELECT MIN(mid) FROM messages_v2 WHERE uid = %d AND mention = 1 AND read_state IN(0, 1)", dialog_id));
                }
                if (cursor.next()) {
                    result = cursor.intValue(0);
                } else {
                    result = 0;
                }
                cursor.dispose();
                AndroidUtilities.runOnUIThread(() -> callback.run(result));
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
        });
    }

    public void getMessagesCount(long dialog_id, IntCallback callback) {
        storageQueue.postRunnable(() -> {
            SQLiteCursor cursor = null;
            try {
                int result;
                cursor = database.queryFinalized(String.format(Locale.US, "SELECT COUNT(mid) FROM messages_v2 WHERE uid = %d", dialog_id));
                if (cursor.next()) {
                    result = cursor.intValue(0);
                } else {
                    result = 0;
                }
                cursor.dispose();
                AndroidUtilities.runOnUIThread(() -> callback.run(result));
            } catch (Exception e) {
                checkSQLException(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                    cursor = null;
                }
            }
        });
    }

    public Runnable getMessagesInternal(long dialogId, long mergeDialogId, int count, int max_id, int offset_date, int minDate, int classGuid, int load_type, int mode, long threadMessageId, int loadIndex, boolean processMessages, boolean isTopic, Timer loaderLogger) {
        TLRPC.TL_messages_messages res = new TLRPC.TL_messages_messages();
        long currentUserId = getUserConfig().clientUserId;
        int count_unread = 0;
        int mentions_unread = 0;
        int count_query = count;
        int offset_query = 0;
        int min_unread_id = 0;
        int last_message_id = 0;
        boolean queryFromServer = false;
        int max_unread_date = 0;
        int messageMaxId = max_id;
        int max_id_query = max_id;
        boolean unreadCountIsLocal = false;
        int max_id_override = max_id;
        boolean isEnd = false;
        int num = dialogId == 777000 ? 10 : 1;
        int messagesCount = 0;
        int totalMessagesCount = 0;
        int serviceUnreadCount = 0;
        long startLoadTime = SystemClock.elapsedRealtime();
        SQLiteCursor cursor = null;
        final boolean scheduled = mode == ChatActivity.MODE_SCHEDULED;
        final boolean quickReplies = mode == ChatActivity.MODE_QUICK_REPLIES;
        final boolean welcomeMessages = mode == ChatActivity.MODE_WELCOME_MESSAGES;
        try {
            ArrayList<Long> usersToLoad = new ArrayList<>();
            ArrayList<Long> chatsToLoad = new ArrayList<>();
            ArrayList<Long> animatedEmojiToLoad = new ArrayList<>();
            LongSparseArray<SparseArray<ArrayList<TLRPC.Message>>> replyMessageOwners = new LongSparseArray<>();
            LongSparseArray<ArrayList<Integer>> dialogReplyMessagesIds = new LongSparseArray<>();
            LongSparseArray<ArrayList<TLRPC.Message>> replyMessageRandomOwners = new LongSparseArray<>();
            ArrayList<Long> replyMessageRandomIds = new ArrayList<>();
            final String messageSelect;
            if (threadMessageId != 0) {
                messageSelect = "SELECT m.read_state, m.data, m.send_state, m.mid, m.date, r.random_id, m.replydata, m.media, m.ttl, m.mention, m.imp, m.forwards, m.replies_data, m.custom_params, m.reply_to_story_id FROM messages_topics as m LEFT JOIN randoms_v2 as r ON r.mid = m.mid AND r.uid = m.uid";
            } else {
                messageSelect = "SELECT m.read_state, m.data, m.send_state, m.mid, m.date, r.random_id, m.replydata, m.media, m.ttl, m.mention, m.imp, m.forwards, m.replies_data, m.custom_params, m.reply_to_story_id FROM messages_v2 as m LEFT JOIN randoms_v2 as r ON r.mid = m.mid AND r.uid = m.uid";
            }
            if (scheduled) {
                isEnd = true;
                cursor = database.queryFinalized(String.format(Locale.US, "SELECT m.data, m.send_state, m.mid, m.date, r.random_id, m.replydata, m.ttl FROM scheduled_messages_v2 as m LEFT JOIN randoms_v2 as r ON r.mid = m.mid AND r.uid = m.uid WHERE m.uid = %d ORDER BY m.date DESC", dialogId));
                while (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(0);
                    if (data != null) {
                        TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                        message.send_state = cursor.intValue(1);
                        message.id = cursor.intValue(2);
                        if (message.id > 0 && message.send_state != 0 && message.send_state != 3) {
                            message.send_state = 0;
                        }
                        if (dialogId == currentUserId) {
                            message.out = true;
                            message.unread = false;
                        } else {
                            message.unread = true;
                        }
                        message.readAttachPath(data, currentUserId);
                        data.reuse();
                        message.date = cursor.intValue(3);
                        message.dialog_id = dialogId;
                        if (message.ttl == 0) {
                            message.ttl = cursor.intValue(6);
                        }
                        res.messages.add(message);

                        addUsersAndChatsFromMessage(message, usersToLoad, chatsToLoad, animatedEmojiToLoad);

                        if (message.reply_to != null && (message.reply_to.reply_to_msg_id != 0 || message.reply_to.reply_to_random_id != 0)) {
                            if (!cursor.isNull(5)) {
                                data = cursor.byteBufferValue(5);
                                if (data != null) {
                                    message.replyMessage = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                                    message.replyMessage.readAttachPath(data, currentUserId);
                                    data.reuse();
                                    if (message.replyMessage != null) {
                                        addUsersAndChatsFromMessage(message.replyMessage, usersToLoad, chatsToLoad, animatedEmojiToLoad);
                                    }
                                }
                            }
                            if (message.replyMessage == null) {
                                if (message.reply_to.reply_to_msg_id != 0) {
                                    addReplyMessages(message, replyMessageOwners, dialogReplyMessagesIds);
                                } else {
                                    ArrayList<TLRPC.Message> messages = replyMessageRandomOwners.get(message.reply_to.reply_to_random_id);
                                    if (messages == null) {
                                        messages = new ArrayList<>();
                                        replyMessageRandomOwners.put(message.reply_to.reply_to_random_id, messages);
                                    }
                                    if (!replyMessageRandomIds.contains(message.reply_to.reply_to_random_id)) {
                                        replyMessageRandomIds.add(message.reply_to.reply_to_random_id);
                                    }
                                    messages.add(message);
                                }
                            }
                        }
                    }
                }
                cursor.dispose();
                cursor = null;
            } else if (welcomeMessages) {
                isEnd = true;
                cursor = database.queryFinalized("SELECT m.data, m.send_state, m.mid, m.date, m.replydata, m.ttl FROM welcome_messages as m WHERE m.dialog_id = ? ORDER BY m.mid DESC", dialogId);
                while (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(0);
                    if (data != null) {
                        TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                        message.send_state = cursor.intValue(1);
                        message.id = cursor.intValue(2);
                        if (message.id < 0) {
                            continue;
                        }
                        if (message.id > 0 && message.send_state != 0 && message.send_state != 3) {
                            message.send_state = 0;
                        }
                        if (dialogId == currentUserId) {
                            message.out = true;
                            message.unread = false;
                        } else {
                            message.unread = true;
                        }
                        message.readAttachPath(data, currentUserId);
                        data.reuse();
                        message.date = cursor.intValue(3);
                        message.dialog_id = dialogId;
                        message.ephemeralReceiverBotId = -1;
                        if (message.ttl == 0) {
                            message.ttl = cursor.intValue(5);
                        }
                        res.messages.add(message);

                        addUsersAndChatsFromMessage(message, usersToLoad, chatsToLoad, animatedEmojiToLoad);

                        if (message.reply_to != null && (message.reply_to.reply_to_msg_id != 0 || message.reply_to.reply_to_random_id != 0)) {
                            if (!cursor.isNull(4)) {
                                data = cursor.byteBufferValue(4);
                                if (data != null) {
                                    message.replyMessage = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                                    message.replyMessage.readAttachPath(data, currentUserId);
                                    data.reuse();
                                    if (message.replyMessage != null) {
                                        addUsersAndChatsFromMessage(message.replyMessage, usersToLoad, chatsToLoad, animatedEmojiToLoad);
                                    }
                                }
                            }
                            if (message.replyMessage == null) {
                                if (message.reply_to.reply_to_msg_id != 0) {
                                    addReplyMessages(message, replyMessageOwners, dialogReplyMessagesIds);
                                }
                            }
                        }
                    }
                }
                cursor.dispose();
                cursor = null;

            } else if (quickReplies) {
                isEnd = true;
                if (threadMessageId != 0) {
                    cursor = database.queryFinalized("SELECT m.data, m.send_state, m.mid, m.date, m.replydata, m.ttl FROM quick_replies_messages as m WHERE m.topic_id = ? ORDER BY m.mid DESC", threadMessageId);
                    while (cursor.next()) {
                        NativeByteBuffer data = cursor.byteBufferValue(0);
                        if (data != null) {
                            TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                            message.send_state = cursor.intValue(1);
                            message.id = cursor.intValue(2);
                            if (message.id > 0 && message.send_state != 0 && message.send_state != 3) {
                                message.send_state = 0;
                            }
                            if (dialogId == currentUserId) {
                                message.out = true;
                                message.unread = false;
                            } else {
                                message.unread = true;
                            }
                            message.readAttachPath(data, currentUserId);
                            data.reuse();
                            message.date = cursor.intValue(3);
                            message.dialog_id = dialogId;
                            if (message.ttl == 0) {
                                message.ttl = cursor.intValue(5);
                            }
                            res.messages.add(message);

                            addUsersAndChatsFromMessage(message, usersToLoad, chatsToLoad, animatedEmojiToLoad);

                            if (message.reply_to != null && (message.reply_to.reply_to_msg_id != 0 || message.reply_to.reply_to_random_id != 0)) {
                                if (!cursor.isNull(4)) {
                                    data = cursor.byteBufferValue(4);
                                    if (data != null) {
                                        message.replyMessage = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                                        message.replyMessage.readAttachPath(data, currentUserId);
                                        data.reuse();
                                        if (message.replyMessage != null) {
                                            addUsersAndChatsFromMessage(message.replyMessage, usersToLoad, chatsToLoad, animatedEmojiToLoad);
                                        }
                                    }
                                }
                                if (message.replyMessage == null) {
                                    if (message.reply_to.reply_to_msg_id != 0) {
                                        addReplyMessages(message, replyMessageOwners, dialogReplyMessagesIds);
                                    }
                                }
                            }
                        }
                    }
                    cursor.dispose();
                    cursor = null;
                }
            } else {
                boolean withEphemeralMessages = false;

                if (!DialogObject.isEncryptedDialog(dialogId)) {
                    if (load_type == LOAD_AROUND_MESSAGE && minDate == 0) {
                        if (threadMessageId == 0) {
                            cursor = database.queryFinalized("SELECT inbox_max, unread_count, date, unread_count_i FROM dialogs WHERE did = " + dialogId);
                            if (cursor.next()) {
                                min_unread_id = Math.max(1, cursor.intValue(0)) + 1;
                                count_unread = cursor.intValue(1);
                                max_unread_date = cursor.intValue(2);
                                mentions_unread = cursor.intValue(3);
                            }
                            cursor.dispose();
                            cursor = null;
                        } else {
                            cursor = database.queryFinalized(String.format(Locale.US, "SELECT unread_count, unread_mentions FROM topics WHERE did = %d AND topic_id = %d", dialogId, threadMessageId));
                            if (cursor.next()) {
                                count_unread = cursor.intValue(0);
                                mentions_unread = cursor.intValue(1);
                            }
                            cursor.dispose();
                            cursor = null;
                        }
                    } else if (load_type != LOAD_FORWARD && load_type != LOAD_AROUND_MESSAGE && load_type != LOAD_AROUND_DATE && minDate == 0) {
                        if (load_type == LOAD_FROM_UNREAD) {
                            withEphemeralMessages = true;
                            if (threadMessageId == 0) {
                                cursor = database.queryFinalized("SELECT inbox_max, unread_count, date, unread_count_i FROM dialogs WHERE did = " + dialogId);
                                if (cursor.next()) {
                                    messageMaxId = max_id_query = min_unread_id = Math.max(1, cursor.intValue(0));
                                    count_unread = cursor.intValue(1);
                                    max_unread_date = cursor.intValue(2);
                                    mentions_unread = cursor.intValue(3);
                                    queryFromServer = true;
                                    if (dialogId == currentUserId) {
                                        count_unread = 0;
                                    }
                                }
                                cursor.dispose();
                                cursor = null;
                            } else {
                                cursor = database.queryFinalized(String.format(Locale.US, "SELECT max_read_id, unread_count, unread_mentions FROM topics WHERE did = %d AND topic_id = %d", dialogId, threadMessageId));
                                if (cursor.next()) {
                                    messageMaxId = max_id_query = min_unread_id = Math.max(1, cursor.intValue(0));
                                    count_unread = cursor.intValue(1);
                                    mentions_unread = cursor.intValue(2);
                                }
                                cursor.dispose();
                                cursor = null;
                                queryFromServer = true;
                            }
                            if (!queryFromServer) {
                                if (threadMessageId != 0) {
                                    cursor = database.queryFinalized(String.format(Locale.US, "SELECT min(mid), max(date) FROM messages_topics WHERE uid = %d AND topic_id = %d AND out = 0 AND read_state IN(0,2) AND mid > 0", dialogId, threadMessageId));
                                } else {
                                    cursor = database.queryFinalized(String.format(Locale.US, "SELECT min(mid), max(date) FROM messages_v2 WHERE uid = %d AND out = 0 AND read_state IN(0,2) AND mid > 0", dialogId));
                                }
                                if (cursor.next()) {
                                    min_unread_id = cursor.intValue(0);
                                    max_unread_date = cursor.intValue(1);
                                }
                                cursor.dispose();
                                cursor = null;
                                if (min_unread_id != 0) {
                                    if (threadMessageId != 0) {
                                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT COUNT(*) FROM messages_topics WHERE uid = %d AND topic_id = %d AND mid >= %d AND out = 0 AND read_state IN(0,2)", dialogId, threadMessageId, min_unread_id));
                                    } else {
                                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT COUNT(*) FROM messages_v2 WHERE uid = %d AND mid >= %d AND out = 0 AND read_state IN(0,2)", dialogId, min_unread_id));
                                    }
                                    if (cursor.next()) {
                                        count_unread = cursor.intValue(0);
                                    }
                                    cursor.dispose();
                                    cursor = null;
                                }
                            } else if (max_id_query == 0) {
                                int existingUnreadCount = 0;
                                if (threadMessageId != 0) {
                                    cursor = database.queryFinalized(String.format(Locale.US, "SELECT COUNT(*) FROM messages_topics WHERE uid = %d AND topic_id = %d AND mid > 0 AND out = 0 AND read_state IN(0,2)", dialogId, threadMessageId));
                                } else {
                                    cursor = database.queryFinalized(String.format(Locale.US, "SELECT COUNT(*) FROM messages_v2 WHERE uid = %d AND mid > 0 AND out = 0 AND read_state IN(0,2)", dialogId));
                                }
                                if (cursor.next()) {
                                    existingUnreadCount = cursor.intValue(0);
                                }
                                cursor.dispose();
                                cursor = null;
                                if (existingUnreadCount == count_unread) {
                                    if (threadMessageId != 0) {
                                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT min(mid) FROM messages_topics WHERE uid = %d AND topic_id = %d AND out = 0 AND read_state IN(0,2) AND mid > 0", dialogId, threadMessageId));
                                    } else {
                                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT min(mid) FROM messages_v2 WHERE uid = %d AND out = 0 AND read_state IN(0,2) AND mid > 0", dialogId));
                                    }
                                    if (cursor.next()) {
                                        messageMaxId = max_id_query = min_unread_id = cursor.intValue(0);
                                    }
                                    cursor.dispose();
                                    cursor = null;
                                }
                            } else {
                                if (threadMessageId != 0) {
                                    cursor = database.queryFinalized(String.format(Locale.US, "SELECT start, end FROM messages_holes_topics WHERE uid = %d AND topic_id = %d AND start < %d AND end > %d", dialogId, threadMessageId, max_id_query, max_id_query));
                                } else {
                                    cursor = database.queryFinalized(String.format(Locale.US, "SELECT start, end FROM messages_holes WHERE uid = %d AND start < %d AND end > %d", dialogId, max_id_query, max_id_query));
                                }
                                boolean containMessage = !cursor.next();
                                cursor.dispose();
                                cursor = null;

                                if (containMessage) {
                                    if (threadMessageId != 0) {
                                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT min(mid) FROM messages_topics WHERE uid = %d AND topic_id = %d AND out = 0 AND read_state IN(0,2) AND mid > %d", dialogId, threadMessageId, max_id_query));
                                    } else {
                                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT min(mid) FROM messages_v2 WHERE uid = %d AND out = 0 AND read_state IN(0,2) AND mid > %d", dialogId, max_id_query));
                                    }
                                    if (cursor.next()) {
                                        messageMaxId = max_id_query = cursor.intValue(0);
                                    }
                                    cursor.dispose();
                                    cursor = null;
                                }
                            }
                        }

                        if (count_query > count_unread || count_unread < num) {
                            count_query = Math.max(count_query, count_unread + 10);
                            if (count_unread < num) {
                                serviceUnreadCount = count_unread;
                                count_unread = 0;
                                min_unread_id = 0;
                                messageMaxId = 0;
                                last_message_id = 0;
                                queryFromServer = false;
                            }
                        } else {
                            offset_query = count_unread - count_query;
                            count_query += 10;
                        }
                    }

                    if (threadMessageId != 0) {
                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT start FROM messages_holes_topics WHERE uid = %d AND topic_id = %d AND start IN (0, 1)", dialogId, threadMessageId));
                    } else {
                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT start FROM messages_holes WHERE uid = %d AND start IN (0, 1)", dialogId));
                    }

                    if (cursor.next()) {
                        isEnd = cursor.intValue(0) == 1;
                    } else {
                        cursor.dispose();
                        cursor = null;
                        if (threadMessageId != 0) {
                            cursor = database.queryFinalized(String.format(Locale.US, "SELECT min(mid) FROM messages_topics WHERE uid = %d AND topic_id = %d AND mid > 0", dialogId, threadMessageId));
                        } else {
                            cursor = database.queryFinalized(String.format(Locale.US, "SELECT min(mid) FROM messages_v2 WHERE uid = %d AND mid > 0", dialogId));
                        }
                        if (cursor.next()) {
                            int mid = cursor.intValue(0);
                            if (mid != 0) {
                                SQLitePreparedStatement state;
                                if (threadMessageId != 0) {
                                    state = database.executeFast("REPLACE INTO messages_holes_topics VALUES(?, ?, ?, ?)");
                                } else {
                                    state = database.executeFast("REPLACE INTO messages_holes VALUES(?, ?, ?)");
                                }
                                int pointer = 1;
                                state.requery();
                                state.bindLong(pointer++, dialogId);
                                if (threadMessageId != 0) {
                                    state.bindLong(pointer++, threadMessageId);
                                }
                                state.bindInteger(pointer++, 0);
                                state.bindInteger(pointer++, mid);
                                state.step();
                                state.dispose();
                            }
                        }
                    }
                    cursor.dispose();
                    cursor = null;

                    if (load_type == LOAD_AROUND_MESSAGE || load_type == LOAD_AROUND_DATE || queryFromServer && load_type == LOAD_FROM_UNREAD) {
                        if (threadMessageId != 0) {
                            cursor = database.queryFinalized(String.format(Locale.US, "SELECT max(mid) FROM messages_topics WHERE uid = %d AND topic_id = %d AND mid > 0", dialogId, threadMessageId));
                        } else {
                            cursor = database.queryFinalized(String.format(Locale.US, "SELECT max(mid) FROM messages_v2 WHERE uid = %d AND mid > 0", dialogId));
                        }
                        if (cursor.next()) {
                            last_message_id = cursor.intValue(0);
                        }
                        cursor.dispose();
                        cursor = null;

                        if (load_type == LOAD_AROUND_DATE && offset_date != 0) {
                            int startMid;
                            int endMid;

                            if (threadMessageId != 0) {
                                cursor = database.queryFinalized(String.format(Locale.US, "SELECT max(mid) FROM messages_topics WHERE uid = %d AND topic_id = %d AND date <= %d AND mid > 0", dialogId, threadMessageId, offset_date));
                            } else {
                                cursor = database.queryFinalized(String.format(Locale.US, "SELECT max(mid) FROM messages_v2 WHERE uid = %d AND date <= %d AND mid > 0", dialogId, offset_date));
                            }
                            if (cursor.next()) {
                                startMid = cursor.intValue(0);
                            } else {
                                startMid = -1;
                            }
                            cursor.dispose();
                            cursor = null;
                            if (threadMessageId != 0) {
                                cursor = database.queryFinalized(String.format(Locale.US, "SELECT min(mid) FROM messages_topics WHERE uid = %d AND topic_id = %d AND date >= %d AND mid > 0", dialogId, threadMessageId, offset_date));
                            } else {
                                cursor = database.queryFinalized(String.format(Locale.US, "SELECT min(mid) FROM messages_v2 WHERE uid = %d AND date >= %d AND mid > 0", dialogId, offset_date));
                            }
                            if (cursor.next()) {
                                endMid = cursor.intValue(0);
                            } else {
                                endMid = -1;
                            }
                            cursor.dispose();
                            cursor = null;
                            if (startMid != -1 && endMid != -1) {
                                if (startMid == endMid) {
                                    max_id_query = startMid;
                                } else {
                                    if (threadMessageId != 0) {
                                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT start FROM messages_holes_topics WHERE uid = %d AND topic_id = %d AND start <= %d AND end > %d", dialogId, threadMessageId, startMid, startMid));
                                    } else {
                                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT start FROM messages_holes WHERE uid = %d AND start <= %d AND end > %d", dialogId, startMid, startMid));
                                    }
                                    if (cursor.next()) {
                                        startMid = -1;
                                    }
                                    cursor.dispose();
                                    cursor = null;
                                    if (startMid != -1) {
                                        if (threadMessageId != 0) {
                                            cursor = database.queryFinalized(String.format(Locale.US, "SELECT start FROM messages_holes_topics WHERE uid = %d AND topic_id = %d AND start <= %d AND end > %d", dialogId, threadMessageId, endMid, endMid));
                                        } else {
                                            cursor = database.queryFinalized(String.format(Locale.US, "SELECT start FROM messages_holes WHERE uid = %d AND start <= %d AND end > %d", dialogId, endMid, endMid));
                                        }
                                        if (cursor.next()) {
                                            endMid = -1;
                                        }
                                        cursor.dispose();
                                        cursor = null;
                                        if (endMid != -1) {
                                            max_id_override = endMid;
                                            messageMaxId = max_id_query = endMid;
                                        }
                                    }
                                }
                            }
                        }


                        boolean containMessage = max_id_query != 0;
                        if (containMessage) {
                            if (threadMessageId != 0) {
                                cursor = database.queryFinalized(String.format(Locale.US, "SELECT start FROM messages_holes_topics WHERE uid = %d AND topic_id = %d AND start < %d AND end > %d", dialogId, threadMessageId, max_id_query, max_id_query));
                            } else {
                                cursor = database.queryFinalized(String.format(Locale.US, "SELECT start FROM messages_holes WHERE uid = %d AND start < %d AND end > %d", dialogId, max_id_query, max_id_query));
                            }
                            if (cursor.next()) {
                                containMessage = false;
                            }

                            cursor.dispose();
                            cursor = null;
                        }

                        if (containMessage) {
                            int holeMessageMaxId = 0;
                            int holeMessageMinId = 1;
                            if (threadMessageId != 0) {
                                cursor = database.queryFinalized(String.format(Locale.US, "SELECT start FROM messages_holes_topics WHERE uid = %d AND topic_id = %d AND start >= %d ORDER BY start ASC LIMIT 1", dialogId, threadMessageId, max_id_query));
                            } else {
                                cursor = database.queryFinalized(String.format(Locale.US, "SELECT start FROM messages_holes WHERE uid = %d AND start >= %d ORDER BY start ASC LIMIT 1", dialogId, max_id_query));
                            }
                            if (cursor.next()) {
                                holeMessageMaxId = cursor.intValue(0);
                            }
                            cursor.dispose();
                            cursor = null;
                            if (threadMessageId != 0) {
                                cursor = database.queryFinalized(String.format(Locale.US, "SELECT end FROM messages_holes_topics WHERE uid = %d AND topic_id = %d AND end <= %d ORDER BY end DESC LIMIT 1", dialogId, threadMessageId, max_id_query));
                            } else {
                                cursor = database.queryFinalized(String.format(Locale.US, "SELECT end FROM messages_holes WHERE uid = %d AND end <= %d ORDER BY end DESC LIMIT 1", dialogId, max_id_query));
                            }
                            if (cursor.next()) {
                                holeMessageMinId = cursor.intValue(0);
                            }
                            cursor.dispose();
                            cursor = null;
                            if (holeMessageMaxId != 0 || holeMessageMinId != 1) {
                                if (holeMessageMaxId == 0) {
                                    holeMessageMaxId = 1000000000;
                                }
                                if (threadMessageId != 0) {
                                    cursor = database.queryFinalized(String.format(Locale.US, "SELECT * FROM (" + messageSelect + " WHERE m.uid = %d AND m.topic_id = %d AND m.mid <= %d AND (m.mid >= %d OR m.mid < 0) ORDER BY m.date DESC, m.mid DESC LIMIT %d) UNION " +
                                            "SELECT * FROM (" + messageSelect + " WHERE m.uid = %d AND m.topic_id = %d AND m.mid > %d AND (m.mid <= %d OR m.mid < 0) ORDER BY m.date ASC, m.mid ASC LIMIT %d)", dialogId, threadMessageId, messageMaxId, holeMessageMinId, count_query / 2, dialogId, threadMessageId, messageMaxId, holeMessageMaxId, count_query / 2));
                                } else {
                                    cursor = database.queryFinalized(String.format(Locale.US, "SELECT * FROM (" + messageSelect + " WHERE m.uid = %d AND m.mid <= %d AND (m.mid >= %d OR m.mid < 0) ORDER BY m.date DESC, m.mid DESC LIMIT %d) UNION " +
                                            "SELECT * FROM (" + messageSelect + " WHERE m.uid = %d AND m.mid > %d AND (m.mid <= %d OR m.mid < 0) ORDER BY m.date ASC, m.mid ASC LIMIT %d)", dialogId, messageMaxId, holeMessageMinId, count_query / 2, dialogId, messageMaxId, holeMessageMaxId, count_query / 2));
                                }
                            } else {
                                if (threadMessageId != 0) {
                                    cursor = database.queryFinalized(String.format(Locale.US, "SELECT * FROM (" + messageSelect + " WHERE m.uid = %d AND m.topic_id = %d AND m.mid <= %d ORDER BY m.date DESC, m.mid DESC LIMIT %d) UNION " +
                                            "SELECT * FROM (" + messageSelect + " WHERE m.uid = %d AND m.topic_id = %d AND m.mid > %d ORDER BY m.date ASC, m.mid ASC LIMIT %d)", dialogId, threadMessageId, messageMaxId, count_query / 2, dialogId, threadMessageId, messageMaxId, count_query / 2));
                                } else {
                                    cursor = database.queryFinalized(String.format(Locale.US, "SELECT * FROM (" + messageSelect + " WHERE m.uid = %d AND m.mid <= %d ORDER BY m.date DESC, m.mid DESC LIMIT %d) UNION " +
                                            "SELECT * FROM (" + messageSelect + " WHERE m.uid = %d AND m.mid > %d ORDER BY m.date ASC, m.mid ASC LIMIT %d)", dialogId, messageMaxId, count_query / 2, dialogId, messageMaxId, count_query / 2));
                                }
                            }
                        } else {
                            if (load_type == LOAD_FROM_UNREAD) {
                                int existingUnreadCount = 0;
                                if (threadMessageId != 0) {
                                    cursor = database.queryFinalized(String.format(Locale.US, "SELECT COUNT(*) FROM messages_topics WHERE uid = %d AND topic_id = %d AND mid != 0 AND out = 0 AND read_state IN(0,2)", dialogId, threadMessageId));
                                } else {
                                    cursor = database.queryFinalized(String.format(Locale.US, "SELECT COUNT(*) FROM messages_v2 WHERE uid = %d AND mid != 0 AND out = 0 AND read_state IN(0,2)", dialogId));
                                }
                                if (cursor.next()) {
                                    existingUnreadCount = cursor.intValue(0);
                                }
                                cursor.dispose();
                                cursor = null;
                                if (existingUnreadCount == count_unread) {
                                    unreadCountIsLocal = true;
                                    if (threadMessageId != 0) {
                                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT * FROM (" + messageSelect + " WHERE m.uid = %d AND m.topic_id = %d AND m.mid <= %d ORDER BY m.date DESC, m.mid DESC LIMIT %d) UNION " +
                                                "SELECT * FROM (" + messageSelect + " WHERE m.uid = %d AND m.topic_id = %d AND m.mid > %d ORDER BY m.date ASC, m.mid ASC LIMIT %d)", dialogId, threadMessageId, messageMaxId, count_query / 2, dialogId, threadMessageId, messageMaxId, count_query / 2));
                                    } else {
                                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT * FROM (" + messageSelect + " WHERE m.uid = %d AND m.mid <= %d ORDER BY m.date DESC, m.mid DESC LIMIT %d) UNION " +
                                                "SELECT * FROM (" + messageSelect + " WHERE m.uid = %d AND m.mid > %d ORDER BY m.date ASC, m.mid ASC LIMIT %d)", dialogId, messageMaxId, count_query / 2, dialogId, messageMaxId, count_query / 2));
                                    }
                                }
                            }
                        }
                    } else if (load_type == LOAD_FORWARD) {
                        int holeMessageId = 0;
                        if (threadMessageId != 0) {
                            cursor = database.queryFinalized(String.format(Locale.US, "SELECT start, end FROM messages_holes_topics WHERE uid = %d AND topic_id = %d AND (start >= %d AND start != 1 AND end != 1 OR start < %d AND end > %d) ORDER BY start ASC LIMIT 1", dialogId, threadMessageId, max_id, max_id, max_id));
                        } else {
                            cursor = database.queryFinalized(String.format(Locale.US, "SELECT start, end FROM messages_holes WHERE uid = %d AND (start >= %d AND start != 1 AND end != 1 OR start < %d AND end > %d) ORDER BY start ASC LIMIT 1", dialogId, max_id, max_id, max_id));
                        }
                        if (cursor.next()) {
                            holeMessageId = cursor.intValue(0);
                        }
                        cursor.dispose();
                        cursor = null;
                        if (threadMessageId != 0) {
                            if (holeMessageId != 0) {
                                cursor = database.queryFinalized(String.format(Locale.US, "" + messageSelect + " WHERE m.uid = %d AND m.topic_id = %d AND m.date >= %d AND m.mid > %d AND m.mid <= %d ORDER BY m.date ASC, m.mid ASC LIMIT %d", dialogId, threadMessageId, minDate, messageMaxId, holeMessageId, count_query));
                            } else {
                                cursor = database.queryFinalized(String.format(Locale.US, "" + messageSelect + " WHERE m.uid = %d AND m.topic_id = %d AND m.date >= %d AND m.mid > %d ORDER BY m.date ASC, m.mid ASC LIMIT %d", dialogId, threadMessageId, minDate, messageMaxId, count_query));
                            }
                        } else {
                            if (holeMessageId != 0) {
                                cursor = database.queryFinalized(String.format(Locale.US, "" + messageSelect + " WHERE m.uid = %d AND m.date >= %d AND m.mid > %d AND m.mid <= %d ORDER BY m.date ASC, m.mid ASC LIMIT %d", dialogId, minDate, messageMaxId, holeMessageId, count_query));
                            } else {
                                cursor = database.queryFinalized(String.format(Locale.US, "" + messageSelect + " WHERE m.uid = %d AND m.date >= %d AND m.mid > %d ORDER BY m.date ASC, m.mid ASC LIMIT %d", dialogId, minDate, messageMaxId, count_query));
                            }
                        }
                    } else if (minDate != 0) {
                        if (messageMaxId != 0) {
                            int holeMessageId = 0;
                            if (threadMessageId != 0) {
                                cursor = database.queryFinalized(String.format(Locale.US, "SELECT end FROM messages_holes_topics WHERE uid = %d AND topic_id = %d AND end <= %d ORDER BY end DESC LIMIT 1", dialogId, threadMessageId, max_id));
                            } else {
                                cursor = database.queryFinalized(String.format(Locale.US, "SELECT end FROM messages_holes WHERE uid = %d AND end <= %d ORDER BY end DESC LIMIT 1", dialogId, max_id));
                            }

                            if (cursor.next()) {
                                holeMessageId = cursor.intValue(0);
                            }
                            cursor.dispose();
                            cursor = null;
                            if (threadMessageId != 0) {
                                if (holeMessageId != 0) {
                                    cursor = database.queryFinalized(String.format(Locale.US, "" + messageSelect + " WHERE m.uid = %d AND m.topic_id = %d AND m.date <= %d AND m.mid < %d AND (m.mid >= %d OR m.mid < 0) ORDER BY m.date DESC, m.mid DESC LIMIT %d", dialogId, threadMessageId, minDate, messageMaxId, holeMessageId, count_query));
                                } else {
                                    cursor = database.queryFinalized(String.format(Locale.US, "" + messageSelect + " WHERE m.uid = %d AND m.topic_id = %d AND m.date <= %d AND m.mid < %d ORDER BY m.date DESC, m.mid DESC LIMIT %d", dialogId, threadMessageId, minDate, messageMaxId, count_query));
                                }
                            } else {
                                if (holeMessageId != 0) {
                                    cursor = database.queryFinalized(String.format(Locale.US, "" + messageSelect + " WHERE m.uid = %d AND m.date <= %d AND m.mid < %d AND (m.mid >= %d OR m.mid < 0) ORDER BY m.date DESC, m.mid DESC LIMIT %d", dialogId, minDate, messageMaxId, holeMessageId, count_query));
                                } else {
                                    cursor = database.queryFinalized(String.format(Locale.US, "" + messageSelect + " WHERE m.uid = %d AND m.date <= %d AND m.mid < %d ORDER BY m.date DESC, m.mid DESC LIMIT %d", dialogId, minDate, messageMaxId, count_query));
                                }
                            }
                        } else {
                            if (threadMessageId != 0) {
                                cursor = database.queryFinalized(String.format(Locale.US, "" + messageSelect + " WHERE m.uid = %d AND m.topic_id = %d AND m.date <= %d ORDER BY m.date DESC, m.mid DESC LIMIT %d,%d", dialogId, threadMessageId, minDate, offset_query, count_query));
                            } else {
                                cursor = database.queryFinalized(String.format(Locale.US, "" + messageSelect + " WHERE m.uid = %d AND m.date <= %d ORDER BY m.date DESC, m.mid DESC LIMIT %d,%d", dialogId, minDate, offset_query, count_query));
                            }
                        }
                    } else {
                        if (threadMessageId != 0) {
                            cursor = database.queryFinalized(String.format(Locale.US, "SELECT max(mid) FROM messages_topics WHERE uid = %d AND topic_id = %d AND mid > 0", dialogId, threadMessageId));
                        } else {
                            cursor = database.queryFinalized(String.format(Locale.US, "SELECT max(mid) FROM messages_v2 WHERE uid = %d AND mid > 0", dialogId));
                        }
                        if (cursor.next()) {
                            last_message_id = cursor.intValue(0);
                        }
                        cursor.dispose();
                        cursor = null;

                        int holeMessageId = 0;
                        if (threadMessageId != 0) {
                            cursor = database.queryFinalized(String.format(Locale.US, "SELECT max(end) FROM messages_holes_topics WHERE uid = %d AND topic_id = %d", dialogId, threadMessageId));
                        } else {
                            cursor = database.queryFinalized(String.format(Locale.US, "SELECT max(end) FROM messages_holes WHERE uid = %d", dialogId));
                        }
                        if (cursor.next()) {
                            holeMessageId = cursor.intValue(0);
                        }

                        cursor.dispose();
                        cursor = null;
                        if (threadMessageId != 0) {
                            if (holeMessageId != 0) {
                                cursor = database.queryFinalized(String.format(Locale.US, "" + messageSelect + " WHERE m.uid = %d AND m.topic_id = %d AND (m.mid >= %d OR m.mid < 0) ORDER BY m.date DESC, m.mid DESC LIMIT %d,%d", dialogId, threadMessageId, holeMessageId, offset_query, count_query));
                            } else {
                                cursor = database.queryFinalized(String.format(Locale.US, "" + messageSelect + " WHERE m.uid = %d AND m.topic_id = %d ORDER BY m.date DESC, m.mid DESC LIMIT %d,%d", dialogId, threadMessageId, offset_query, count_query));
                            }
                        } else {
                            if (holeMessageId != 0) {
                                cursor = database.queryFinalized(String.format(Locale.US, "" + messageSelect + " WHERE m.uid = %d AND (m.mid >= %d OR m.mid < 0) ORDER BY m.date DESC, m.mid DESC LIMIT %d,%d", dialogId, holeMessageId, offset_query, count_query));
                            } else {
                                cursor = database.queryFinalized(String.format(Locale.US, "" + messageSelect + " WHERE m.uid = %d ORDER BY m.date DESC, m.mid DESC LIMIT %d,%d", dialogId, offset_query, count_query));
                            }
                        }
                    }
                } else {
                    isEnd = true;

                    if (load_type == LOAD_AROUND_MESSAGE && minDate == 0) {
                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT min(mid) FROM messages_v2 WHERE uid = %d AND mid < 0", dialogId));
                        if (cursor.next()) {
                            min_unread_id = cursor.intValue(0);
                        }
                        cursor.dispose();
                        cursor = null;

                        int min_unread_id2 = 0;
                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT max(mid), max(date) FROM messages_v2 WHERE uid = %d AND out = 0 AND read_state IN(0,2) AND mid < 0", dialogId));
                        if (cursor.next()) {
                            min_unread_id2 = cursor.intValue(0);
                            max_unread_date = cursor.intValue(1);
                        }
                        cursor.dispose();
                        cursor = null;
                        if (min_unread_id2 != 0) {
                            min_unread_id = min_unread_id2;
                            cursor = database.queryFinalized(String.format(Locale.US, "SELECT COUNT(*) FROM messages_v2 WHERE uid = %d AND mid <= %d AND out = 0 AND read_state IN(0,2)", dialogId, min_unread_id2));
                            if (cursor.next()) {
                                count_unread = cursor.intValue(0);
                            }
                            cursor.dispose();
                            cursor = null;
                        }
                    }

                    if (load_type == LOAD_AROUND_MESSAGE || load_type == LOAD_AROUND_DATE) {
                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT min(mid) FROM messages_v2 WHERE uid = %d AND mid < 0", dialogId));
                        if (cursor.next()) {
                            last_message_id = cursor.intValue(0);
                        }
                        cursor.dispose();
                        cursor = null;

                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT * FROM (" + messageSelect + " WHERE m.uid = %d AND m.mid <= %d ORDER BY m.mid DESC LIMIT %d) UNION " +
                                "SELECT * FROM (" + messageSelect + " WHERE m.uid = %d AND m.mid > %d ORDER BY m.mid ASC LIMIT %d)", dialogId, messageMaxId, count_query / 2, dialogId, messageMaxId, count_query / 2));
                    } else if (load_type == LOAD_FORWARD) {
                        cursor = database.queryFinalized(String.format(Locale.US, "" + messageSelect + " WHERE m.uid = %d AND m.mid < %d ORDER BY m.mid DESC LIMIT %d", dialogId, max_id, count_query));
                    } else if (minDate != 0) {
                        if (max_id != 0) {
                            cursor = database.queryFinalized(String.format(Locale.US, "" + messageSelect + " WHERE m.uid = %d AND m.mid > %d ORDER BY m.mid ASC LIMIT %d", dialogId, max_id, count_query));
                        } else {
                            cursor = database.queryFinalized(String.format(Locale.US, "" + messageSelect + " WHERE m.uid = %d AND m.date <= %d ORDER BY m.mid ASC LIMIT %d,%d", dialogId, minDate, offset_query, count_query));
                        }
                    } else {
                        if (load_type == LOAD_FROM_UNREAD) {
                            cursor = database.queryFinalized(String.format(Locale.US, "SELECT min(mid) FROM messages_v2 WHERE uid = %d AND mid < 0", dialogId));
                            if (cursor.next()) {
                                last_message_id = cursor.intValue(0);
                            }
                            cursor.dispose();
                            cursor = null;

                            cursor = database.queryFinalized(String.format(Locale.US, "SELECT max(mid), max(date) FROM messages_v2 WHERE uid = %d AND out = 0 AND read_state IN(0,2) AND mid < 0", dialogId));
                            if (cursor.next()) {
                                min_unread_id = cursor.intValue(0);
                                max_unread_date = cursor.intValue(1);
                            }
                            cursor.dispose();
                            cursor = null;
                            if (min_unread_id != 0) {
                                cursor = database.queryFinalized(String.format(Locale.US, "SELECT COUNT(*) FROM messages_v2 WHERE uid = %d AND mid <= %d AND out = 0 AND read_state IN(0,2)", dialogId, min_unread_id));
                                if (cursor.next()) {
                                    count_unread = cursor.intValue(0);
                                }
                                cursor.dispose();
                                cursor = null;
                            }
                        }

                        if (count_query > count_unread || count_unread < num) {
                            count_query = Math.max(count_query, count_unread + 10);
                            if (count_unread < num) {
                                count_unread = 0;
                                min_unread_id = 0;
                                last_message_id = 0;
                            }
                        } else {
                            offset_query = count_unread - count_query;
                            count_query += 10;
                        }

                        cursor = database.queryFinalized(String.format(Locale.US, "" + messageSelect + " WHERE m.uid = %d ORDER BY m.mid ASC LIMIT %d,%d", dialogId, offset_query, count_query));
                    }
                }
                int minId = Integer.MAX_VALUE;
                int maxId = Integer.MIN_VALUE;
                ArrayList<Long> messageIdsToFix = null;

                if (cursor != null) {
                    while (cursor.next()) {
                        messagesCount++;
                        if (!processMessages) {
                            continue;
                        }
                        NativeByteBuffer data = cursor.byteBufferValue(1);
                        if (data == null) {
                            continue;
                        }

                        TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                        message.send_state = cursor.intValue(2);
                        long fullMid = cursor.longValue(3);
                        message.id = (int) fullMid;
                        if ((fullMid & 0xffffffff00000000L) == 0xffffffff00000000L && message.id > 0) {
                            if (messageIdsToFix == null) {
                                messageIdsToFix = new ArrayList<>();
                            }
                            messageIdsToFix.add(fullMid);
                        }
                        if (message.id > 0 && message.send_state != 0 && message.send_state != 3) {
                            message.send_state = 0;
                        }
                        if (dialogId == currentUserId) {
                            message.out = true;
                        }
                        message.readAttachPath(data, currentUserId);
                        data.reuse();
                        MessageObject.setUnreadFlags(message, cursor.intValue(0));
                        if (message.id > 0) {
                            minId = Math.min(message.id, minId);
                            maxId = Math.max(message.id, maxId);
                        }
                        message.date = cursor.intValue(4);
                        message.dialog_id = dialogId;
                        if ((message.flags & TLRPC.MESSAGE_FLAG_HAS_VIEWS) != 0) {
                            message.views = cursor.intValue(7);
                            message.forwards = cursor.intValue(11);
                        }
                        NativeByteBuffer repliesData = cursor.byteBufferValue(12);
                        if (repliesData != null) {
                            TLRPC.MessageReplies replies = TLRPC.MessageReplies.TLdeserialize(repliesData, repliesData.readInt32(false), false);
                            if (replies != null) {
                                message.replies = replies;
                            }
                            repliesData.reuse();
                        }
                        if (!DialogObject.isEncryptedDialog(dialogId) && message.ttl == 0) {
                            message.ttl = cursor.intValue(8);
                        }
                        if (cursor.intValue(9) != 0) {
                            message.mentioned = true;
                        }
                        int flags = cursor.intValue(10);
                        if ((flags & 1) != 0) {
                            message.stickerVerified = 0;
                        } else if ((flags & 2) != 0) {
                            message.stickerVerified = 2;
                        }
                        NativeByteBuffer customParams = cursor.byteBufferValue(13);
                        if (customParams != null) {
                            MessageCustomParamsHelper.readLocalParams(message, customParams);
                            customParams.reuse();
                        }
                        res.messages.add(message);

                        addUsersAndChatsFromMessage(message, usersToLoad, chatsToLoad, animatedEmojiToLoad);

                        if (message.reply_to != null) {
                            if ((message.reply_to.reply_to_msg_id != 0 || message.reply_to.reply_to_random_id != 0)) {
                                if (!cursor.isNull(6)) {
                                    data = cursor.byteBufferValue(6);
                                    if (data != null) {
                                        message.replyMessage = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                                        message.replyMessage.readAttachPath(data, currentUserId);
                                        data.reuse();
                                        if (message.replyMessage != null) {
                                            addUsersAndChatsFromMessage(message.replyMessage, usersToLoad, chatsToLoad, animatedEmojiToLoad);
                                        }
                                    }
                                }
                            } else if (message.reply_to.story_id != 0) {
                                if (!cursor.isNull(6)) {
                                    data = cursor.byteBufferValue(6);
                                    if (data != null) {
                                        message.replyStory = TL_stories.StoryItem.TLdeserialize(data, data.readInt32(false), false);
                                        if (message.replyStory != null && message.replyStory.fwd_from != null) {
                                            addLoadPeerInfo(message.replyStory.fwd_from.from, usersToLoad, chatsToLoad);
                                        }
                                        data.reuse();
                                    }
                                }
                            }
                            if (message.replyMessage == null) {
                                if (message.reply_to.reply_to_msg_id != 0) {
                                    addReplyMessages(message, replyMessageOwners, dialogReplyMessagesIds);
                                } else {
                                    ArrayList<TLRPC.Message> messages = replyMessageRandomOwners.get(message.reply_to.reply_to_random_id);
                                    if (messages == null) {
                                        messages = new ArrayList<>();
                                        replyMessageRandomOwners.put(message.reply_to.reply_to_random_id, messages);
                                    }
                                    if (!replyMessageRandomIds.contains(message.reply_to.reply_to_random_id)) {
                                        replyMessageRandomIds.add(message.reply_to.reply_to_random_id);
                                    }
                                    messages.add(message);
                                }
                            }
                        }
                        if (DialogObject.isEncryptedDialog(dialogId) && !cursor.isNull(5)) {
                            message.random_id = cursor.longValue(5);
                        }
                        if (MessageObject.isSecretMedia(message)) {
                            try {
                                SQLiteCursor cursor2 = database.queryFinalized(String.format(Locale.US, "SELECT date FROM enc_tasks_v4 WHERE mid = %d AND uid = %d AND media = 1", message.id, MessageObject.getDialogId(message)));
                                if (cursor2.next()) {
                                    message.destroyTime = cursor2.intValue(0);
                                }
                                cursor2.dispose();
                            } catch (Exception e) {
                                checkSQLException(e);
                            }
                        }
                    }
                    cursor.dispose();
                    cursor = null;
                }

                if (withEphemeralMessages && dialogId < 0) {
                    ArrayList<TL_ephemeral.EphemeralMessage> ephemeralMessages = getEphemeralMessagesInternal(dialogId, threadMessageId);
                    if (ephemeralMessages != null && !ephemeralMessages.isEmpty()) {
                        final SparseArray<Object> containedMessages = new SparseArray<>(res.messages.size());
                        for (TLRPC.Message message : res.messages) {
                            containedMessages.append(message.id, new Object());
                        }
                        for (TL_ephemeral.EphemeralMessage ephemeralMessage : ephemeralMessages) {
                            TLRPC.Message convetedEphemeralMessage = EphemeralMessagesHelper.convertEphemeralToFakeDefault(ephemeralMessage);
                            if (containedMessages.indexOfKey(convetedEphemeralMessage.id) >= 0) {
                                continue;
                            }
                            addUsersAndChatsFromMessage(convetedEphemeralMessage, usersToLoad, chatsToLoad, animatedEmojiToLoad);
                            res.messages.add(convetedEphemeralMessage);
                            messagesCount++;
                        }
                    }
                }

                Collections.sort(res.messages, (lhs, rhs) -> {
                    if (MessageObject.isEphemeralMessageId(lhs.id) || MessageObject.isEphemeralMessageId(rhs.id)) {
                        if (lhs.date > rhs.date) {
                            return -1;
                        } else if (lhs.date < rhs.date) {
                            return 1;
                        } else if (MessageObject.isEphemeralMessageId(lhs.id) && !MessageObject.isEphemeralMessageId(rhs.id)) {
                            return -1;
                        } else if (!MessageObject.isEphemeralMessageId(lhs.id) && MessageObject.isEphemeralMessageId(rhs.id)) {
                            return 1;
                        } else if (lhs.id > rhs.id) {
                            return 1;
                        } else if (lhs.id < rhs.id) {
                            return -1;
                        }
                        return 0;
                    }

                    if (lhs.id > 0 && rhs.id > 0) {
                 xœ @è¿xœì}ûsÛF’ğï÷W ®Úµ¡Kò#[JÉ’ëV’‰rnëj‹„5	p	P¶¾Kîoÿ¦ç…y?@J²M¥bÀôôÌôôt÷t÷$	.ÅEÒ›\Õƒ"K¶“9şc-ùßÿHe7‹y™<^iıì÷$ŸÔ¹üUplãS‹O’o¿¥ÍÂ/WÓİ1†È1î:şfi“SlàÏÕw7ğ*¶øŸRhOth¿¯½üí!àüÍ^‘NªËwçÿÊÇÍ ¨÷ËñüfÖäyŞËğ?Ùš­'E™N’ÓY:¯óƒ²Ù™ÏÓ›$-ÇWÕüuQfEyY'[I>»Ê§ù<üšOÆÕ4ßÁÔ§£ÁeŞìHÚVÍãˆ+M|³•”‹ÉH_~3¨‹ÿ—÷ÖĞ¤;WÂE5OzEÙ$)ÂöÉKôšÄ¼LóºN/s½øî;ß¤OŞïHÍ„B@`%x¨Ó½ÔÒ?yl+>~êA†à)ˆ<Zj}Dµğ0ŒÀh(QUOI?G¼ú`_¤¡ŒFªŸ!’Éç¨—|Êû†¾zúE­4¸.ÒÑyÕŒ
©©öĞÑ4:ŒÌBF‡ŒH	ãª¼ÎÑÒÒ†j+QÕoóÉ,Ÿp•y;jÃêMú1ßË/ÒÅ¤ÑĞ'(i–Õù¼Ş)³İ«´©ßÌ«)…Ğ³áØOPeXV)š£1Ôc?Ò²˜¦PgZı« 1‘W¬ˆ¾u”@š¹¢ÿ­ÇZ™Oo‚ú8jnfhî¶’Íä·ßéÉSíÉp¦/òùŒöi>GÓ
¾Y”ó<Ív«EÙÔ‡Õ8EÄÏ¥¡A|y:knzV.Ìû¦7-J´†_!šO?#:á6"ú^lË/œ ¡³>OòtŞóÌƒ@a¤zkÄl‡úLéó²‰GÛÀéá¥“†ã­c§?\§yÙUYM ³±îYğ}sŸ	;‚ã{(ãÅ¼FÛÜV‚$“ô<­ó!HØePŸ³Şi3G;É í…h=÷01æƒ³Ó~òètÿpw˜ì¾;;"C‚Ü›“wGŒ¯Ö£ëä×·û'ûÉóÛ¿dÉÎñ^B;„¬ãß€î¨†=?98î=é£~ÔOZÃ>íĞ]‚ş Ì?7î…àO
m©ÒÉ"ï=ñ‚¢úë–S$„â`6>YµC‹öÖh‡³¢UuîZYœ:`ƒ³²õ[¤°¦šãÚDeøÍ¨;İõe!=bçïŠ=Uş…7Æy>›ÜĞ)>IË¬š¾ûT¢½Í½·.OĞSĞ‘X3LA(Ç?‘45Ì1@·˜ĞÉOÌIÓ:™#B=Ş?IşëİÁ±ÄeÑ»iòîÕŸÉÿ‹‰|° Oà_²NÄVåÿ¥º"º=kŠI=øWU”½G}ôLƒ¬^3-„OWÅ$\ÇiS\ç¯ošüõââ	<0	'şsş‚­»º‡kHß6İKz>f9l
<‰=2AğÿĞ8RI67z)Zkı„ük§V¦Z@Í¦IÇWïÓæŠÂD£Ò¶S‹¡m/Ü£Uî¸”=V'ôª!U1³$\–1NƒI—Š1íªQX[›Tåer}i{ÏH÷6İÃæŠÃ¢n^I²¤ìÖÛ-¬4nÜ¬£kåy>­®so}lëàÈêŸ&kcÊ€"/©
[ŠÀúÀá…˜2 ªqDZİ<L!A ‘ VWª.&éeü†ä„ça
«
€ı1šÖ—²©!ÄÄ°j}UbÙĞ\Û°gûå¤u¤ÕO]„QÚ9¢¿côà–[ˆxí4½sÊë!½!0_€æ†waØö˜ğA„B¶qÄÑ²Z¯¥Áv¯ß2Ù€Y	§é—MZuúD˜˜º'N™ &lKß!	ÃµL«Lå‹±N´O8%94%xGâG©ePæñ‹¢Ã×"Şûx‹&ÁK€¶FĞÀOüh¶H7: [ªK<ÿ$¬¼m;ÀÆy¡–±¦ıº¯·híÕïÉ8mÆWIoÿó8ŸaİM;:ñÛ[øèÙ_ã9¶¿ÖÆGcã«|üñô—ChO$Òß‰É~r£àŞj®UîæÍÂhñ¿ îëE1É>¤¨[‡ï~>íï¼>ÜßSá¿ARûau9ÈzØ â•›4Åx×£ä»¤wzS7ùtwR?òI:«s´²Ó	|Ò[K'H…70¹CôdÕx„9+YÛˆáèFÆ—} æª/^<y‚Ï+Ñ¼\ãü¬µ†Í_cxÃtÑ-C-c«®ø×İ@NÄ °:÷RúJß]çóy‘åìkj4­èc¥FQ2ş}QR4¥Ãøz’Ö·5°ïááˆÎŒTã¼ªµ–	Íñ¯ñıÅ¾Ì¾V+¸SŸt€£/«ı/õ±”¿çEMòéZXâhÒÑ¤­ñı_1s«Ñ2Ë“<Ã†ë|~™ï1ªª°™l_<à5!øC¶Â#zJ†ïöŞ¡uú¨1G•U"hR¢µ“EY¦çHÙ³?ğ	ß¾[—ğëƒx±è'ğÅe>íü÷èÃÎáÙ>~	ïÑl×?/
ò5Ö0ù?€ïA™åŸ5ö„Ïvan›7†ØnU6ój2É‹ÌæÕ=‡E›gÂ]‹ö/¥ÊZéVD?©..ê¼kF3_äR·ÔÑ7P}_#¤¾ZúíöÑ¬¢µ,óI_Xı„“K?yÌÇ¯o\}Á¶=6çô‡Æ‰E†O¤¢¿~/=ŞÚù šÜj±L~2RïÜĞ£p2iê;L´ÍW:ùíŒƒp¨Ùm…Å1ùı„!hi6e/Ÿ£}-c¾ÿl.T¿š-Î'Å8¹®ä­‰KÄ@˜€¬Ô×´œ½j¼ ”¶“ŒşU‹;’3Š&ß%‚ƒE/kæª°±¼ÍÛ°U’	D£KDÄªÈp%VÅ„š¡‡ªÁ0ÔX¨
[Ó`œ¹PVäç}à¦AöF±¶ö¤;!PÿY.ó%kNğSaÏÀç:êƒÂÉcf‡~Ş _³â–pím@7…ióh³ÒXZì6›>ïY>åˆ-"~|Ú¤ãÃy:–š¹KaİÌ)8›ÆÌ–%ãŸ
_f2aqU¿ÅúLĞ÷…¿%şL%Ô=şC`ÕXX,|ŠY+FATä„¯)‹e(iü³•Y)}a.sSaHñÛÁ0­?&Í:,Hü+=±bP¬Ó¦šƒDÄ‘z‚¢Ík&¾ª&Á=fÇúQÏ$ZT²ªÌ{jaÑŞèŒö#*—:•Q=q¡îı¼Ï©C¢NEÔ@(A% ¢7NuHÕ“qH7—¢/@3FÑ ‚™+iBí„é|Á2yFiRdIôo»À6‹SÄôr4×=qıÄ‘´y‹ä²Bş9/ø…$¶Ş£=$÷‰(PÃösÁÛcıÑº|6¼*j$ƒÚ˜aàîãßy~÷Œœñ£Z¿ÉÓıãáèÍÁáşhø÷û£÷oßßµÖÑÀZ;g{Pk=ªÖ‡ƒ½}¨µUc8Ú?Ş=ùÇûáşª¿«Tÿi<ÖRıgğ{ğóÛÑ/g;‡Ã Ï—!áó¢C|~x)Qq~şŸc…¶2&·ÏÒæŠl|ÀEª1Ş‚]‹t¿ı†¿4Õaõ)Ÿï¦°y™Õ¿ÍUïQÚ4àÑøH“€©~'Ëõ-©cm¯úTâU4–#®ütÌ{‡tÁÅ¤¡èÓvİÍl„>‚h¹ëÑŞ3<TF	£Hä_—´hV„–ÒoĞî•ÂÙ¼¹wŠÿüKıŸÄÍÛ’¿dH×ÓƒCäöZ‘—k7ÔSÊğĞA]W½$ğF³JW	(a¬/­¢D‹¼çÕÅoxÈ,¥E¦¹…“5ğ?Oş‰:ÛëyÀAëhS£¿İØ
a AH¿¿ªšjESXİüğàê÷¶ÑˆvZiıŸ-íÖx%úTÜXÅøüúÏ·ÕcÍ•É&u×–Øó€ÿt¨Âƒ‰G†—~J‹FVmı½òœd©ÖG$ˆv”…­‹Écóâœ°èº˜ºñk‘¡ı•ëºR,Xk;Do·áuí>„/ˆ-¤šä¦à£µP¼L8¨û=nÕzÊJFÊ4†ú¶­ÑS‚íäFä€ô\;x\ñ¸wp:<8F0ÿÃúªš7Hò4˜©9ö¯Û|ñ@uµüÁ¨
wsi„üG×:‡à 0é|p]ÇînÖ"wÄÁ6]3ÏÎÇø½S;e6GÄÛ
WH¥|W±-Üª°âª³!ö£´D{Ç<IÕ[Ú7 WĞ½¬‡^"y9fÏæƒ´}§hš}8ñH+’WJ·¹Náö¾Q‘”US\ÜpÄ?ù§=´,à¤è-¥pÍ)°r2(²ÁMÃèU·¤.åÃÂsºÿlÑ¾³N’õK:U ëËßó{­ÔFp_åp–u:Æ½0_€AÁÈq°Ğ¬!(„A¾Ÿç ×g8²[Ü‰ßÿ–¡“ı÷‡;»ûˆÉßiáCÛÓŞOış[SÍGPü»B;ë:Ï1§¶	¿ä£ó¢Ì(sè­÷Û‘ğÕ«·áˆüÔÁoö›BA>†Á72WWô€æo‡½†³Zö´ó:Ú1QÕ&nuƒÖÿØV1h¬Íâo¹sYñ·ƒŞå!è|(!QeŠcÑğE1®¦Ó¢q¬é»3âµæN;Ó»óçê¸Õ%ÂKH´*û³nõr¸~j~”ÛÄsS	.ÛÄ­±=BZª¤/iÒŠ›ñSt;e¨C"¸†I¡* Áã3À
ì]n!Ÿ…Î½;ÙÛ?I^ÿ#©æY²sºûH`…KFaN.Æ­´á®¨Ÿ{	º³Œ‘ÜY”K ÙJÀµG+Ó†ÖñT‹'öxêBL"P[I¬‹†æÔ(ìÙ4Xİšxâ>†ò‰nË ñ±dç-gé ì ûH‰V8šºøí£š„İí }ìºhnø `÷íÎğt©èhm59„Å¤^T¤Qïûv-ÍŠ²Ì³doÿtNsügrxpt0D¢Ñú“TÉJÌcEò%ˆYpâh*Äû÷íş¤0Ò¸:×'+n›¤`íq&†»Ğ}õŠPì«8j ¨k:°/P‚V8+ÚJih¥&ZŒÍı î ÔC Éc ÑÅ~ù–7Œãè
	Gl…Ó aqÏÓ»„ıô‹[Îkçaí˜›1ûğu›û¸Ğ(V:„H™Â£ìâ¨Ç‰	¨9âÅL±bv,îãÈ)Ì‹·yd†ë|¥Z~õ8V9†¿Iö;ü9vcXÛLD´}Æ£\§Ü…®«²ı•ª×ÌB€˜-ª…=ü*ôƒÎÊ^ï¬EkĞ~™2Xs¾3ıXV‹a“öª¿ï‡;»vøËœc…æ¥æayE"nÑ¶›Á'pÄúN=ÄlAÃ¬>dË€ÀOÓ^ƒêÚé³	Â%ÓğTê>ŸÑ´Mm6§6§˜Û©ÎKá!¶öH–¢´N²äpÿÍĞØ©ÅBÊî„qT²;e‹Í)ÈV§A¨İÉIX+Ø¿Úæ~0QN0f
Ş³»0³ñºlì8cQæÌõÊfÍˆ"v›| ¤8îIÚ˜y;P’Y9¾lªó¸ŒH½E+‹‘}‹Ö–XxAŠF=˜-šï4şµUŠôG~ê
{ü*³¶QÕ·*/
DCƒñ¤X]7–‡¬Ê:g!Ğ‰ªM¦Îğ3WNJg^¸ç[nj ğÂ%A$­¹Ú~7õÜö
Å±äÜj±ÛÕìIíœi»xÀ¾²œ¦ŞÕK¦ÃÃËçUo,Ël<×>c<äAxâıÎ®BPÌîB~k7C¾*®ùÜQÅ¢ì<¶%±È[T@.9«÷Õ7@ª\›	·ÕcË£˜ÉiQÖ „CbPaB3Ë»ñø‹˜snWûÛ%]Û[aVz z^gõ_¡Åß‚N”¥¿›•ÿÁÿÛó4qJÁn¢aŸ5W¢ÈL!$RNùÄQ„–­ÂÒÅÈnwn5·ßR|¦7˜ˆjÎsI{Èœ7FNmCÜjµ ¹€ÌÊ$âGĞA´„  Í*ë´8Û;ÑÚw+¸ûBfœŠÎ*ÁJrÏü"˜ºÆü­Ä„HÁr@€-&€7áó@·iÚ0™ê;iX@B%Ìì”ØføR®9+ÄòtŠµ]o  áào•Ttçöíû^èŞ&ÑåCñyÊâ·;/@q«ë÷µÅÒô°[¯[¿5s’İKœCòëß@H;ÍÿM9€ôd­o7xê—‹™ºoÂG_Ë®èægï÷v†ûI^GÄætˆøÂ¿GÜ0‚¸ ü]-òÑ7õıRÆ^—ù9mfs´À3x eøÉÄKŒ*€Æ€`TeC¨‚ªƒÖ#¨åk¤æÇüfÄ{õ*YüfzÚÂSŠ¢ ZÏh-a$ƒê=g­™¸˜›[`r&TÎü¡Z¤6,Á"!éÖp¸¤²9(FÊ´Ÿ·ˆ+úMSš·ßP2e'·Ú9‰áH½«áeJo!©ñ¿ú@¾Šİ:ñ„şœÒKJáWÍH.,ûK¶†²êäTE%iøÀÆÓf8<´²ñ?×nšÉ²lˆâ¹‘üæÏI¢‡é²"ÅD:ÁX–L1BZY	¡ş!iß“É¹«´–^Ò‹Á$//›«¤¿5.¥‹æj„>ñZ&å6ô\H‚ï `ÏoŞ¢Oztã¤®bÁQ­‰ç—ähºƒĞ=£ß®Ë~°ôŸã,äˆ=€ÎSyıºÀÙiéßR£ééDšÁÕ^´>erÀÃï75an8¦£j>:R		Oı˜˜+ŒmºSÈ“7GŸwhğ©³Á‹ZàùÈÖ®òºCóÏœÍó¦´«.bsƒTS4m9ëV[c
Ø&¤Á§9bs ¾ëŸE±(LMz‹.dmÓB®¦ŸjM+@â0P)ÁÕô3­iV;¨M#!lB0¹8¹Œ€¦ŒºvûA¨Dş,Z4zojyÑÁÔòÃ­™Zpr’¿Q”„Í(L”|ÒoÛ#&ĞÇ‹ëL
¥Ä Œ‡€â/˜Pò1y˜™+PúyÖ¿8é#"¶‡µô¼£Ñjı…(œGš­ÜnÂZ¯7İ¯Ÿº_?3¿şèEMŒ}H„V3æçÅlı6Å{UîUe³ÿ¹¨%ı!Nà9V Lm¢Z.FkyÇoîimÈ›Iæ>“|œM—Ù›i½
Ä×îÊa¸'Œ‰1¡;“ Hœ/££H$;€2Ç s]a¤"mIMnÍ¯wÎn}ßÆh‘•;zsX ú$³·Só:	ôXD’˜–•˜YXZ^÷Å°Ü›L6!5ÿ®uÛQ•È3ÆÇK}÷J“#‹‰º¿M›Ô,[Êm-ElŞ®ÕëÖÓtL|Û`1jæŸU'¤õ2|)ãØÌÈ %?pËfø\Es€t„›‡u’v´c®„Öc3,+!1ì9«Œølk	|-UIƒÎ_F‡î|ËìfËî'--àÙë›B®,Ş÷cñVŸD8³µfoÍ‘-ê?£×ÛƒeùÁ²,7h`†İLËêá¦ÇìÂRòëò}Kİ	7û	Æçæé||uœNs¼S˜Óh~zú`ö~0{_Ks”ÙûyŒÙûE¤Ùû‡h³÷ßâÍŞÜVc÷3ñm¾×7ºZ¾7;[¾Ÿ®Æòı,Öòı<Êòı¢ƒåû‡N–ï¿-ß–š6K¶×Hk—ÿ |™†p(môi:LÊcæÕ%d<OXCKı6PÕ[Cp8²ÅtÃØì›²+WÚ7dv®ıŒ×ÆMåyõyD.X†ñ\†øY /|—7(û€<dì.ôÈ±û‡3³&b4oá¸®·„E2Ù/¦á•[ÃAHá[ªâ‰u¼•Ñ¢El?¼‘gê< ÿ»Lğüˆ!}ÑÒ|3Í^…SÚºF=³j2]WHj
@Û‡ãeîü£À,RÔFzuá4ª+ŠİFD‰T{½(€°RsªèIÏEôXÈûà¢˜#6]"øbVXåÕüpò-µ7Eåe¦B4Z¼yËx06Ìß8Û%s4w~£à÷(Q·¾ß]áˆ{"~ùòå#ÓğÂÿŒ}d/¢†–U’#x¸©ôImmQHä¡5hºÈËXùeòİw…i´y˜*ïùR`kºÌÓBB| ÜóÚ¸ÂÕjGG˜c4Û’äÑS€ŞTÔˆ°&_ık2Äò 9š?ıÑ¦‹—<}m>mÿ/ñH~)ĞìtP*ÜØ›Ûà4ÅœHãOrÆ©\TpjğµœoŠ÷uÎÊ1“ùÜj-i®æÕ§:i‘“O*èéVkğ÷@ÙO/VpçAÆX.çH")‘Î<I’I‘(‹5gEb£Òß˜ÿñ=dZ”&Ú½…{ºÉª'£$:@`‡!fá]ÆiÈÌ™X¹×Ë¹ù"mäŒÌaûğ~oæfE^	a”xBó'Ñ6ÛúŠÔàÆZ…ÂTüà¯•+Ê€¦=û‰»-å÷·Éÿ…`>pëÍz³ĞÉ¥ú$WJä<BaıbÉVZhaã$Âà3¹¹V9j:MØ†Ï§§aNƒñ^ÁÔÓ½€âàn7:eX ÅŠ_œ²·›æPUãéY¢N²1áºO²ñ'AÇºyÕ|†©_nDœ_òÅPÎ›]ÔOµLET‹;Å¯Oò1~&7¶) !hV ‘­|úôñ¢<ù)y¼Î?â²¯¢Ğ!wˆD¯_óüãj['ÈruFîI\W«Ánƒ`gà*®LÁ¶nhNÓs,,ÔO‹L‹1è0İnN”Å™àë7Õ|1İM¿9(¯Ñ*u²§/-Ñpc0ÚYC„Ásj/¿H‘ó:sõIqyÕÔ²&Ÿvx ~–Ìñ?$#İ5’s#T«È'ª“Ò¾šC3Ñ¬ÖÛÊ¯Û¯Ê+«½Ó&!Îd–}v•Šq‡Ò2ºKé+Òb´:jí8®RGğëw–‹Xt5ä{Y€£s¼´FtÅ–Bº´îŒ¿7¬4±Ø²Êºdo}‹®mC/±“ \7o¬?}jùŠá½Åz`;?÷E½ëï÷/~«ÆºX—ìPÜ¿„OÂ×nmdĞïåø‹Ye‡zÛhK Y
'¹/K É3×k${Í€â¦:æşò&3 ñÖ¹cócşƒ›÷ğÄ e|÷6·n(Ñæ=ŒQ„e‡V4E3É)™‘~ã«Ëì+cş#¼îù¼J³1ZV¬>m°ÅE‘gûaš_¦—ój1c øƒph¡LFeÕŒràBü49#•@‘G=«Êêô(Ş3ö Ãx\#B¥úÔ¸ı+mT¬6’o©`µtÑˆŠì¤(?"Qw“$İ[^›ww¢ŸZÂŒº¯‹æSQç$¡şUZÃµ2â´YĞow~m<	¾½@–q5.Ê¢¹Ñ‡E|ÕuXT4W5@~U!v<l’¿½¥c²zJ|y—ë¿c+ÑàvìíúóÍVÛÙ•Ì¶q–W1»›/¯ìlcµÄ1Û®TŸö2ú˜MîàóÙŒ<gSñŒ:gĞÛR~›ü_ºá“Ô‚¦³´l¨Ã¤ÍÍ…­
–´7«@Ÿj´ùû=,‚=«ËAşeÉœõÍõ'/{ûªM'áfıÉláÔIdvŸ N§";ÀuLofLÚƒ*Å=»ÿ4¡­ÿÈç¡iO,v‹§	õ!?NĞéMi2ë5ñ,cƒÃìaŒÕÇ IeY›¼5ÛûÀ««üXVŸÊ Ç-Yuİ:.Ã^"…ĞFëû,Øö óú¾â!ò-=¿;ÈìÎ®º—p;÷nU6ój2¯íŠ½Ío6ÀaGÉ’`á´ñy>­®s#î¸ÑÇ#ì}Ò4|£M¬Šñ-£ú	=­5úœ9ïÿ¥èjW ‡^ök7SİÃ9’Nw·ãvf“
%]õû ÿµ˜ÙCïûeÅ-W…­0µ3
K‚ó,‘qú»fãÔ©7ÒÇ·©‡ÙWp`„ æ)ätŠ¶
~Ö›ä´2tœæ«Û‹¿ =í u1mª9í[A
›{¤.UÊáØk°:[ÙÜ¡´ÿñ:&Ÿ|(_ìÈ1§;`§’‡}N,ûÜŸuŸû#mò±4U›ññ&K‡¦îä¼:Ls“¡K`™Z;¸Sy[CAoKçuşÜ(ÖA'í,ŸrŞNsÕñ0Gği[fËSÒ	‚~TÍuágs–*ôWä !ÜÓÌ9WÀ5C9¦îI°Äiu_¡3wğ3NîÀıU<¬ØvĞt– Z²gÌ"©†å^dÅÍŒ\ŞySÏ¢Y4ë©íğï5TbÏV±›)Ğ•ù„8¢}/“Fß²†î„3jÂf‚şc˜ı‘ÄÍ;e PÂ¥ÏPF
%œ™ş‘Ä;K>ß)JI(ÌÄ)Gââ/J¨º5”.ù[ø¶-zÓ»e‹ß°%Ü®%İ¬ÕŞªÅnÔjoÓ¢7i	·hµ7hÉ·gaîÔ&HıCÊnzª_Îƒ¤W«Ò}W+¾q'êqDF	NTüê*û§«Eòe³¼=±1¯‹”K¬r¡Š8Ñ\¡=`nAºkÊ.)×g	8mLù&%äê;Õ,`ËuÅ«±‹òÔŠ¦ä„RE~­ZóyHMr	ŸZõ…‹*ÊFº±O­ûƒ¯Y-eåVÒ«¯ª9Rä{-àímÈŠôE‡åƒ"_8¨-‘¿…`!$¼4…\>ĞÄ)Â˜úÒ¸b]KV„å%åuïj’p[@x—ƒx£6,®UO}…‹õp-à]¼rˆF%¸àáæØŸ9‘|Ë¤Öum‡‰‰_¢väÊ@³S* 9M?1Ç¥´F´OEs5œ§eMR Š}Òe GV¸Lv_¥ğ±ÛÛIË®ƒ» TÑopDáyùÌ@–˜&&ê@×\âï>èëEÔ«È¯Nøª&º}êãp\5â—œÊ(Ú”«>ˆ¨*|ƒt	_¢-j˜@ñ[:*åJSnpÿê6@#gvoæÕR\÷„kƒ^ ÛUs3ËÛY‚
İ¯h[.IÌÚ¾ôa‹üÙ¯r@YÅTeÜßE±Ñ±ı*L0j,zş¶]¯¾¥íœ~¯[è~Ò/Õ¨'ş˜aÏú†Ü…½1òÏ>Ø4VW]´Kïù’åğµ_âÀ3Ô'ë}LÏd&°£ãğª¨{k¾10gÙ‘]ÜCô„ºh§"kÏ¢;s_™ao;ÍŸ+86Ë'yc`o­¥é}ZG~B“t ¥È„íª1‹ĞµBvV)\¾İãeåÅg–…,H²ÒQrGìÒîO"éjõr o?9_Áy{Ñ¼P³—ÌĞ3
ÄØËûeófo?½Æ*IÖëü\HìkÔù¸*ãâíM.Ü–ea«¡‰áŠç®L²é£‘g+NòMCâŸ	{^ìxĞÜ•ĞìÓw·~¥¨gˆCåô¸éĞQ	ŞÇˆ—Íe&ÈD»îÂœ]H#Ëİ¡ wd)ë‚·»»Ü¥uYï¾7eëö(â¬š"–ïÛm™áÅîß¬ğ¿÷0./ß/˜tÕ§§3°5£Á	 €äİÉŞşIòúDHİÛ?İ¥Ğn:ĞC\(òØptèO2Bò76ÙV®: }õ}Sj›Íy6µD©LÇ—¨ñ¹mó‘gF6³hü±5õº=Ê³"e·“[†/îÇ{RDkUã)´•É©eÚñ0€Bò„ÁO‡ˆOı¡ÈòjÂ4£¿µzU1Î;Ô;©eÒ(6/ÉïàÖ"ÙÕº
Xß¿ÚJãÓfë°M~~ñ†×Y›NQ‚•x6hjÒØæõ®Æ©‹_Á±ÿqc¥fYèó“Ï?<!…äÍˆ1±Ğ-Ûûå–½r ”èìpúnºŒ >›Wc4"ò>R÷ÈH»¦nd\›š@<jƒ.0¥Ñxô$ÏèWµĞÇÆì§ìŞ/²ŠLW·Ğ/S¼Í8``¬Şc``z?ôQ£ùÛoÆÁ3òÙÎ•Ïv­Ürj‚Mçb£‡ö2ßÁ0«q';LìélÕL)zm4¦±s×¼Š›P,ÄUÛã»¦0lS›N_õ±Ó;¹,	‚>Ú%ìpÀ|Òõk~ş«z`ÿ9Åî…Xé çZôõvò‰~§Y©¿ÿúÓ:kltÒê$F¹Íˆ+P«3Y
Ùğš¡ß³3L˜XéC-Í€EÃUIH3]oPÅßºÃqù˜ßì€İÑ"Ê©Õv»5@³›}ÈÀ¨5¬^İ1Z$zÍ´uíG¦ãC8Sì±ù•A÷©+‚)İ'ÂÒV"=İRhqs¤úV'·4¨H\9´s¸Û?ˆ³ë±´“ÖS	¨ñ^7º|ÓE¸!Ì7[„ç°7Ğ¿öUE¥>†¬ÓíŞ¬øIïæÂeÇÀY_pèçiú8s˜b§Ù¼fñ)ˆĞÓQŠ©±ÏÇÄFIPb—:r:L<.G6(‘¦_´dl~P(F![5¸¬:S(&· „¦Aúâû´¹¢°i¬5R.
¸õn<)Ğyf‹¼VKx&R6lqR•SBsk1à˜F¦¡e‘n”âæÌ£+2Ã ğW÷ÄbqÇ2Å&i€ÆœÛ/&Ç>—öÀÅí‰ÙµGÎ`íë ê"»3dRŸ
§¿¬ê\œô´…hktıt‰†L)£ùP;²F³bc@†í´Š¬Å”°£ ‡‚œ¤µ‡Ä£r§±µèÏ<…}mÍ¤f¬å==o?
NU/WNÑ[–ä­…ê7ûÊXƒ‹î§­ºèÅÎäíŸlu~£Cï7ºuc…ıßp€{Ç²İ©Óén %_ı»hì>Wïl4=®šâ¢§ h7L0š"m]|ïÒ¿DİÉà‚(´t³_ÉvYó©şÃYUôÛ¸"A‚½ÜILfèwìÂa?°¯®óù§yoY*Ë|ÂoU‚ÒÅJä2¦zDßíÀr$)«êè]ì	ññE»	aClág6§¤*÷ªòŞøÑ@ÛåuY–Üõe—Fƒç/ı3èí¬€,ÅXĞY@°…ˆBép3Nw¤»Á—ÒÜxÃ^ä±‚(Áò¨ËÀÍ‡,Dw¼ã,¿Ç¸Á4ÜLôCd³è™××øo©-İ´pKWD—Wé<»»–FM5+Æõê”ÔL\]©&ĞÊúmÏÕ[î†:®»¼…QÔ{„Š$ÆId‘İf‡n‹ mCw—OÛºC¢§äx7ƒ*Ò~‡&µ6ñ?„¢¶eçeğ¡}]5§\«G6w€jŠ«GL:Ïu¬ßG‚Z¶I\[še;“I¯•âjô¡ZÄÌéõL¡b=Ñ§Tå‹^›tH-¶Îóºä™­YbÌLHú»¬ÀpNO¿(AïºÕyÓ%s£ŠI?çòıÃìî#}|Œ/ "Ûı‹ı€ğß†ñš-¨–ŞÆöÑzäÆX­ği_‹æj‡ºd,#hï± j§f}¢Òè(İ‰&Lâ]1¼q¬îœƒd,oK9ÖĞZ±7\–ŞBä’¥m)VqcŠIÄw°²ªÅ{§‡=¥k	IVóÿªŠ’ù>Mº2Bn‚0*Öqäbî+=çà?F^´8ÿ‰âÓX;D“vµC¾€uæÏ¶[H@:Œï‡"ÿ¤»®¿Á?·WÀ÷5'Ë×oªù'´õY+ˆ^2”JNòÙñmƒ>hCìk„g76/Sì–¤¼`H™ŞÑÆnÍ‡õA:T1)Vu9tp4G/³A $ŒÃí4¦ÅM©—ü4ıÜë±CtüÌq€®œí¬Á] qG?P,7†ò†AÁV¦YN²úˆõ™ÛıP^R¢û“(ä¸TóÃ¢$?¯~¸nÁ„øP°Ê´öãà üP‚“äÅC’õ~Ûxpev2Ìct‹›}2VÑŸvFõYª¶;œY	ÍAØBôœ»Op,‡Ü*³á,|¥üæ‚BUX|Ÿ\‡õx	ÆÃA¬€÷pX÷Ä~Ä©zà@îbÃõÀ„„–eB6YQm$ŠÍ	ĞQ7¯(&B1^‚‡0¡,Ä!à¸
ƒŞ‰©lZ¹Êf0[Y‚-ğ„G¤Úşç¢ö]ijP!¸•ı¹šsÅf%tîy4ÑŒà§Ê®ş ×NiÀÚÃ0'U(+¼Š^Ä+ÖåŠ6y¦)¾·S(qî¡]ıi7=¡`Ş)ÍrÈpºıÍÃğ4.°9ŸÉO[î!½b¦œBN{1¹œôfÀÖ×Œ¤ ÃgsÃ»W[®šR¥4!‰’Vš*µo…JÖïÉ‹»š"’£S7è¸¾WWÙ’kÔt¸Bß†,$¦póø²ëĞ œÑïw[*Õøã›æ¸•Ä[ cÔÊˆüœ·[è˜æcÿÊ
Mˆ`kÎ…ïsğuïÙÚ„…8ö­<­3™]¢–¹S:ÉÂ{%ôlÃÑ58Ê)´‡PHæ0 rÔn®P<ÈĞ;ØBbÄ’™n'u•ã˜şöU‡•%=Ã|Ò·­‘€qk#oüd™l'›İ¸˜mfCdÿ0µ;¦Üê²|?bÇl·°mÓ¶2j¦İÒµY.Ó»(A”Ë>ëE˜Ë¾Ú÷ KĞÍ‰rO×«ê^ê±…Í „‰Ëwnz0¡î&p®twg¥2®cöP—éøñÀhŒòm¹œ‡%%î”¥ŸO@Ì¸oe9 ¶O~NYÒeà[¨âı¹(áÁ.¾ø@èHÜ‘bˆ3¶9¿è7Eı®FB§ß¼;9;ÿñ~´ûvg˜ü&>yıNy°wp²¿;4jÿ÷`U[ÆNsÏærìN'}·~İ¹1Å¿#„°İ8b'
3ÍØe#‡¿‰M^ÚjyƒcCòÉ*Q2Š‚M˜¨¢TŠ’TV>Úä,šàèAOÂ
¿4r‡rÊ±Æ
 
Ã¤ÆÒ°Y.rx3j“”SC%	~ÃŞ±3˜Y”	â^ØÜAˆ¯‚À €ÍWN²½%Á°­Cc³óL·»õ¤3Ä¼tK–¯ú^“½Í(£"áÇoŠ#¸ifñ“)1+Ò_„ƒcĞ¥7HrrQÓ•ú-»
J‹ûı÷Ótş11‰€u’ÖÀÍÆJÆf}é±‘×èøìÒ»ÔË.ğ5&­Ğ˜¼2G‰Ÿ=—g*8
2ü¬ùÃœÃ¼½

\Bb’ÇêâàLØJˆ8ÂÍ”bÊQçrLş*( bÙåØàƒJóÀŞÃ•,æ®´ûã™„¨yòYï‘wT••¬ç¹3H-Ÿh»$’-Cp•B‡Ú¿¨chÛøÅdNº·Ä2¬Ç&2İ6w­µë`º)YB–ÈÖÄÖl¬ï‡­0gKj2‹…:¯Å$ûÎëÁŞşë³ŸGïO> ycôaÿäôàİ1¾±Ì8Î8î7Å$?¬.YïQÏmNZÃJi¢•› ìÍÔìwè´‘µƒÓ	£yØà5[fø]y2^¸l¶üóáÁéÛV@ä2QÅŞg¬‚]P*<š¶Ë“ßåd‚mM ·¾i¸4s·×O,9DËFÇ~<íÀ»¶„3I~3Y½¥:êZ7L”#(€Í<Aq!NduòeÙ{l“=úÚ8ô­½4­Éò4Ïë¼Á{öë4»Ì.ó<³¥İ³M÷ìøc´Ü;ªå‚œï¿GÀQØª"´OxHÃVEıS²ñ›G^2êë¦úXã{7«eTÎâ‹Ş#`Id[¤×TêkÀ¸[9YVg³Ìu5•?÷‰’”	—5é•fu•”¯ªÄ¶*{•cr7küÀ¢ÈaPXşyğL™hiNØº<@„Ù!áÉõ	ãcˆ*}'qÅÆpXwew@gTæI33h‰Qf†X{õ	ç-Z!3»%æ#†nšå;Ş©‰9ºO·Óí¶CÍÇSuÓÏ.¥17ˆH«hãeä€ãZò`ÓT¹}-y9ø„1‡¸zØÎÃX= í’-+>K{×üZŠ£Wm3\úü•RáÃñë=¿š¿¸ÍÓWî ;6ëæfxE‹ñİÓ\ä…Cä|‹0¦xrP¹ñÜáé%mÇ³Pj;“_{²EpL“'ä­ï^ÄÙ8ã{_ÃÑøùÄİf^Óû¹ç
EÿğÌ5Ú‚ŠŒe½BÄ™^\zEïÙiÓçÏ&7#P®3Ñ1ø›ñSf{üÁA9)ÊüE®æûÆ -¦	Z~h‘Ê7 aKû-H†´_ĞŠ¼¸!Yº½×ØˆoN¨Ñ¹D˜xsğ…æ±ágvè$dÓY¼liº"HOñî½"h©LïA[6oÒf9Ÿ-ÇHj,òÌyæ‹ÇíÃ†ìhP·ršpÆ¦tÛêÁóTÔ–uo9áı½\2ßyœ/tŸ¸»ıĞˆÔƒ¯8µšœÅgnã†ïnÓšy>Í•0í‹¬:®š3!Qà6á ;z¯çQZl+ÇÕ!âÊ´1jİ¨²œZŸš+rLÁ¢=»ÿ·Œ¢Ø§Q>»B;Õ<öÙ_¼¹òÄlÀÜŞÌ«4nÊ"®2C(jÄN™¡Qû5Ÿ 
îªîBÄ~(VşK\ûñMê`eÇŞ‰ÄUãACòÓvÆcb¿›²á¿¤ç’Oº*dÀo]î,†aô¯0ãØ‡_µ¦UÇy/Õ)®ßæ“Y>G«¸¼ÎçÍ›ôc¾—_¤‹	şmÉğ›Õ|¹øôN:>Zş:„¾ºV`şg¤ÀçöxYKØ‡¯qÆê\95¤Xu×E=Íªil-Æ¹"«µ×RÅuä«‹©3«&“ØN5ˆG9ÁZQ:í«½0ÔÅTáW‹†_UÙw|Ù)ww—h§–«½O‹ù+ûI{'j¶šóÍy/Ÿ }©Oa1Sêvˆµ]®CğË½à±ºÑí‡¥z•'kF›2lÓ1HJ»zoÑÜŞíí~İ?Ü}w´?:Ú?=İùyÿT•æTU ¾¿,ö€3>,‡ë/g»ì¿?<Pe£ªÈ N]HûVßƒ[Ìí1ïK¸¼ç¢Ò{S÷÷ößìœ=1Ãbš¾¡pŸû¾Dÿî8ûî»"Pó­/U­·pˆğ¹ØÍúr@ÖÏ(£g„Fµ˜ŒØÑ¸S‡¬é¸Ã:y|º»UYæx/­Ò2½d><»í‡ñ‘ÂFä¶<İõ¡.„	µUXkSqX{Û' æ‹~§‡FnÅÎ/ã.ëuÇâ›[Á‚&´ƒ9ùvg÷¸;î÷­iùäÛ­ÿK6y_<„qŠãâ&dR™ÀX#==[LpÂz;?9İ}»¿wv¸¿÷Ò*½öÙùàñoÆDi&+$—!~’6@7P÷'¬¬:Ÿ\à]@½µX‚pq«J°6„lj¥ªø@C¬­b…0‹«Á	SSÌÖX˜šİıäàxøMB[ëÃÎáÙşiï§~¢üw|vxU`”ış{sÛ­¸Ğ4ı¬Ár£k&»oËY@€Û¶8)üÇc=¾ãBaÁu«/–ÁV~©wñù¬¬3`apƒ>áNR»!ŒFˆ¹ÛˆÑº)‰†é	8qÀ1¯O`05À*ÇŒ†¼ó~İWµªW¬ûÍàŞËåé»¸6ŠWúh|Çà’Ã`‰§2Ê»‘ÏF0tğL ”µ™:ØP›å	U7·»Ùª¤‘tñ`Ú\Âƒ,
€°ù]Ì?ooÓ6†úüj[}“‚"ò²‰ŒÇ\§m…îY"3­Âgµ%ˆH8–¼LÑ¨4ÒZ›Ãá°¡6oYm²ƒˆ•}£zxÃ®—¢mßÈådMJEr)Sx€“åÀ¯¥0ßÉŸn¥õÈK"D¿he‰
í"šŸ°EœBêC(ØŸ¸Nó§®yo[º&ìZ$lsóòõƒ|ı _ß‹|ı…J‡‚a¼`húÖ-šj´-„Â°
>‘0Jd¢Î8q0ŠË)øA´zƒYAç‡_£Åä½©NR¸çù¼J³1’|N™ Ål·pIâe®RéÁÆ×
¬¢ÙûO!³âXøÔƒYøAlUÊƒØjªúeŠ­­{b'§×¦Ì@ß¶¡ØÂæb5é‰`-æÎ…lc1ëæƒJğ`+~°¥
ÂŸÛVå–”„_Q>HOèkÁŠƒYMP"/ÂdÉI[A•']é„jáÕ³WÛ74^ğÚÀ¸ YÄuñ†irg„âÍùÀ²: ¨Ñ”¥8b9rÄÔ7ÍÎØyÇ`'l{ri@mØ3¤|‚‰ÕíÎØîCÓfmÒ(Ãİ¥m@ÕQ:Ó²L‘
FwECö+	ç“©5`®”Uh¨òÑê
æ–H£©; GW¯s‚Öf×HãoÓú
ÍÙ+Eõ÷ü¦Ÿ({^5,faÇê˜5}uŞ[Ë@@x›¬dŒvŒxªPÕ¾RN…AA İwÂáa}	ç …ÔDÓ•¡ÔbÔº9®`qÑa:!)Ÿ:’nè¼.ƒ±¿²ım<NSúÉ£#Q{4¬ãüKKÙJ¡«€º<e‘c¨Gjš-
Ù0YË€LÚL$	;`‰_y0î%[¨›pÀ«à©ÎD%äMXã£Çˆd›‘ÙĞæ€ÙoIN$Ò7kK]ÌèAîÿğ¿!¼Ùƒªëˆ_‹¢?St¥üÍ-hñ¤-°¯Gÿ†Œ.ÆÑq´Õ¡yßãG³¼ÌĞ’Œê”9UşÆsÊ
‰*÷òIŞä4¢Š.İÁÑÎ0*÷yZ²’…®VZŸ}U‘ıŞcĞHLP@¢bIFL—Ö§3å¤À¢²IP¯Tò  ×_æ]p!="NÌˆ(¤ûÕíC"ÁP=lúñˆ9ü€"äøGMprUšÿh*l¸Ğz„3õÃ·eŸ¶“Ïá-şlP]˜F,°™ŠàBmƒ…©ˆaàL	h#& ‘¶IFPLÈ%5\†Ø@’€`R²]Sk7tó)¤&ÇyñşâmÙiov'y:§©ÔdNVÔïÚµ´&&R»˜WÓQ„ˆŞÈdÀZæùRà:Ñ/ÚL$»jÔ@‘Ê£¸gÃĞ°3¢X¿6NV*Í,®Ÿ<^÷,y*Ø2B•Ã¬šEy^}†›İfÍÆ,`~E:öÎ=±8¯Äp´c	Ÿå`:ä¦ök§Y•…t,›tÅ`—İGØ!h†Ú
4¬P‚ô¶!ü+–RDI®¹#«Xjá• cùhHÒ-ãP+¨u5\Â¥("XkñÙ;dì-¤Ğ~´Ü&"õs0ÉËËæª·æ»MHGuÎ@µè=ê›ôˆp„tˆâöÖAtPÑ×™*"„P\¥‹ 
Åˆt'aŠW”XR’ÀP"ÅÕ[“!éô'×°‘0ÉQÚ–CeF!M
è(g¤€iv’ŸĞüHí“*¼-Jğ6ºÇÓ'Ù®h±kŠ‡S¸R t1N ÓÆ9V&€,Øõ¸À©
ÚAÂ™¦eÕBë•—9t Zq«Íôâ8!ÛÓ Z4!TÀ–mÉ
øÒÕb1ë‚ 6œ}CñY	qp€ß}:ÑúÈSÎA.-¸g:r‹·v`Óq[Ñ³(`³ªyd £fÒ>‡ìMçY´éù¦§$q)âı{h;i¯NŒÓr'c„6¬dWm_’-éÀ8l†”3æĞ3R±8ÏÑ—(YÇÂ±Ï’QuaXcı…w “¡Tƒ:*"Ñú‹ˆ¹[sa­t58Qº‹Z7Hyq¤÷2ƒÒ\µ–£%Iâ®lç:¶w`@·/4ÕÅHXÏêï¸ÒZˆÜNä)~±hX[Wû¢#E›€ Ó¯à£|)ğÙÈ}š¢“ó#Ím!öÜÁ8Ú¨¤aÖÑ¾¤vG–CŒ­ÊÊ$CíjpR ¬Öödl†*D¤îÒáÌêNìMv„µs«{¶5Ù¥YóİB!¢ë­pÙû š0™Á½IC)z(¡æ§ß~@Ãz¸¾&áad+˜Ó-Ş¨/’G­˜%ZBŠgŠ¶uå ÉçiSÍ¹ è#u ?æ7§h¬×ìÛÂøtULrDOô³ÁUŠô6Ÿ5ËDC1†ÙÇ’m¡òÎ
×Ø.š×m„ù±“PDL‘qbD…Ìs…	wĞ9§4ŠúÂœtÎ!zfıkwÖ¢¦A¡ÙÛizçAŒÑ«°¹Ö/V|ôäĞƒb÷UÖëşš±¢îñg¥Ûå¨g§í½¨ønÓ5É.E7¢ë§B<íÁqï/õšv'ª¢L5iª·&ßnÃœ®İ(#t›İ:ÖäŒ¯fNêU]çü¸*HÁ¸ê–(oc5ÄLÈŠJ9ƒy>­Ğ–×£´µæ¸‘•`ƒ¹¬ï0:Š³Â	×šë!
q  8¢ºX}Xñ›ş Ö”ŸĞE+–UŒ¶Ëx­S,¶>‚ôÀúØ×š]ÙÈû¿P›&,ãF+6­B	?ì	<äásÏ‚–"LRüT„­ÒúÊu"ç;áA²¼fçÚ¤+‘‰„r_•ƒ¨P±Ac6‚ÂxËO>üÖö€ƒà¨¹…2S‹Ø%,î£.õ)àCJø0À‰*Ø7‹Â·ƒÜ²î ~pLÄ—?Û"Ím@G »œ ñÛ	j‡?26)?à"D^up6Ï¯‹jQÕu%ìe‚=ĞéÎ(â%Áİ°úÉ£aX"Äˆ]5~ái˜t_€¨°ƒHVÔµ+v?bC‰r’Ñ]ÉBåXÄMYç…«v"pÌÃĞÃ@?ÙyaûñYşLØˆ¯Çìyø‰Û£ŸF™/Ä*ãPÖVcæuz±v„yKº»äDfÑßİé±deõdĞ.şH¸£siÇJ-Ñ&!­—Şóƒ¯Üñ`†ø¢Í&ziæ«´GXÌƒaÂş¥ŸeŞ.ûSìqçä³°ÒÑ²ZóÇİY>©%L—(‚|Ivg^µèıùŒ <\AÖ¯"]c¯c™q„6çSOá^/¯Ñ¬Ô’"øÇ?XRŒëz){Êÿ  ÿÿì}ùoG²ğïï¯ |ù™Q,ÙÎîçØ‰I„È’cQ‚"ÇÖÄ©ğ°#¼ä]}VßÕ3¤,;,6Öpºúª®«ëhmO):È„ÆfDnÒ˜ÒşÀ«§ÑÁWZ;Ï¦Bï¿xê¸_Ğı|±'Å?nBŞòãY‡=)hPºcÌàÏõï‰¸˜`­L0°¼›‰U¾’!‘¨|/“rÌ2Å;%mœÁ¶I™I¾“ÀLÁ÷ &@Ù¿²rà¯ÅL¥Tb‚Öˆè_rS®%ŠØ74éÏÙÍ#?vKe•!Øbc²¡hL®¬aL7©ÿâı7íÄ¹k){è.Ë(²b a`Oi¢ßú..;÷Ÿ¹H%ioF5·æ[vı‘Pšc¢½(ñì'·Öi¢mné^ĞŞÆX(-RKßÈ[ìçgÚBh‘åjü´ñOŸ­ÃÙ²,¶›ÕSÍ°r–ob“½yVŒäÓºI¶„òú€*-cğq0)ğzÛßĞ²ëĞŒ©²AğØÙhÃ¡Y@dB»p¡z«×œ ³ÿ<^(¢.Ó%bà„VQ—xl#ªƒªÔ„$ÜÎ±oKCÈô¾ø°€ïˆÆğÚVÅäŠ½+%}Ó1e”ĞBxœö:š¬³–p2lS¢A¯nÅ):.lUêz¯FV

[­eÜ´±ÓhC\•Ä—€ÃÅ»°pwk
ÿ¬…ë¯ïjñQÓŞ~©-jzëjÏf“j8íˆl…|[;±º¤ğà•Ïg.íMGóë+†¯"…©Èj7ß®œoBàÆ•üF®½øô´RÅúı#œï´=äÑ¼ª¦‹‹™ê`‘~Õ‹ÔºÕ£Uƒ•Ù›âÕ×Öänöv>[]Ò=­n„ìy¡ªq÷Reã‚¹ÊâŸÇW2Şæ¯ìº¥«¬ykI$ó¥^œòîóÒüHØÅÎã›KäUP h…è1í	ÕË‹#ÑÉr@«Uíëd2´ÏcÉ¢k†ÏOÒÆB(M×4ÍÑk0€õÏíÒnğŠ}‚ßÉ<Ô(óã|â7zê5zê|BL^DÈ±Ô,Á“œVw¿¨k¯ôh7–»ÅF
i7³1£ ÍÆ§ğxkPšĞò{_²©e7kMy2ƒújá æUØ“HIGx8ÎÚ45îŞ‘`²ä3{{›HèöËQ½¬^Î««á¼Ÿ‚nÆ7¡eÀ¿â@#—Ñæ<¤gMœ>Ô¹ÚœêÉ/<iˆ^ 6³P¡üÁT­‰mª§,òKªŞŠhdPl

ä(oÖ8ª‡¶ÔğäH¸¬:Uè©\UZˆ.7ĞAĞÈw@rPŠ#i³[Ipı›±ÿ¯@`OP-İoÎü`ŒKmË~îßÏWs÷ğbÛÁg£€ä¤ûÊóøø¦f—Â;¯"7®mYÃZçKØAKT²÷á CŞÂxA{ÒP¶Rª^¢dËwHş`-‹º\N2¨IôØÑÎ?”ºĞ;=İû±7øáhïÇÁO{§ƒ×‡½_OiÄŸ<º÷uõ!™S±ˆâÄz{«MGE‰ãgÆ³ìñb=Ò¼GÈF!fÏ$œÉõ)4¥zZXı¤Ìˆ4Ñ¢¨  ßÈ¨‡‘F9dXĞà˜­¦é¦™îÆX8I¼£‹e=zWÍ_³{SW4éPtñg’¹,‰®vÉ]ín„Spğë Z¦t"…k Ê”¨Ş-Ë;×p
êjÑèd³vÔcít“;Øµ¸ jxª¡µ¤ÑÖp¨iĞ>Ò±v)0Sèô—ˆl
°şhhêŞŠauö»'‰O.ÃÜzØfêÔQ©„+©zÂ$ø7L«	*ëYr>G+Æ>._çC^‡VBßGoª&WÕ|ûÃ¼^Vü²D¼&èBâBÁo„#áóáŞÊBµóÈ‡;ş¹K·¤QFßŒÂ•V5Î–"ÇÑ4\9ŞÇ­'<\®úÈx‘D³”Àë†9d9Se«²J;Ö¤w­	ÀmÚöoÄşR=Òs²ëPhNºï>JËá[e-h_U›Ûi°à—zN™¥´í¦\¼§Ûò‚Í)%Ã©yÕÉƒÃñC0Ûã¢ØÁàc­<+À¤ E½îBï²yîßEEyŠ/U®lÔ4SÚAdVu^EQÏİ£?Œ‡!ß*”Ôv$½¸´QĞÕ¡wuÁhş|8!;<(Œb ©ÇÔjB9šÚ£Ó;”¹s™aobt„ŸcDÙ)AÓœ£Én4ñ‡ğ°Ì¤€ğh=uN\ğH²~L³ÕÊÆ9Šd#!†­¤úŠƒÔÊ!·Â­éh.[„P}}\ÈtDeB–ÎAR“#‘÷ê]‘™İ#ò# {¼Ó–è	¼o²/gcRÁr90…¿ùş6ÂŸ„Æq–ÃÅ;ò©´šPb5‰Ïƒ\§Ëá`é,G|Ş˜å˜æÍY?‡å Úºfw€}”ÖUq³z^ÖÓƒjR-«~}	S/†Ë‹mörËú¡ù4ˆ.øSÂO’÷—³4úŒ¿b_b¼lBÁxş)¸ÛØŞÕ£SLÿëYg+±"Ö*æp‚~¦ÄçÏ”iŞüL90vÍÂlÃoÛdI}Á¿•¢aå¯ÕùK®$ät¶Õù•°j×^5élBúS_V k¤ºy£i¤º)(iÕ-ôvlY€¢$E]A¼•âšzK¯ªjÎ†¹=–z–ß ÿ…«ÙbyÏrmåÏàæÉ ÓŠ»l/^§Ã·¼¼—¥‘Üº×ùª³ó¸óßox×ì×9$«zS8p|˜Q6Á¡1ï
ézz—Ú	v; [ü0ƒXÏC$sÖ“IØË¹c-'‘n_Ì–3¼Ä´f³ÑŠû^·TG’]*S%f’.ğÕ®	u¾-÷t>˜ÎŒoIöÊÕ´Ö«„¶WşFt„3y¯ëq5{9^3Aú—ÕpR/¯Ÿw~çÿ¨¹ş€uô‹ú)n'6¥ËC¯FKÓUAn<&şï³yÍ³¤{ıÃlŞ¿X]›ÎÎˆ"eZÁ àA[ô~[ı‘ï«i†-x’ˆCuÅÿ?€üg"n›˜mİz=«G•|I2pi`<Ö].D4?òúTr{ï¬rpòëñÑÉŞÁ ÿ?/{ƒ½³ƒÃ“<HsäªiâBñRĞ¶ÑVÓ7Z¶¥øÅÀƒ¤wGN…Œ»'@İO÷¦õ%cZãH“¿/:éÿM·‘,Ê&¾bDvLşúÇúÍíÛæ×‡½¿É6ëîJ2ÁntñNöÏ^ôûõŒ°`ñ¯3ÁIù¿É¤9Ã±)ö'³EµXêO­—ÜQ7áİÎŞt<ŸÕcÈlÄÍ„I>mğèiğQÊ!—Ä€(ˆÏ6<yl²	aE¸m%èğÿµ¤"“z6bÈíÁÿz Ú ÛÈçq”ÛXBÎÜ…ã¦ÁE5šMEgr6j¹:¸ù³Nt3lxÔVH‡ÇâÂj„'ø X_Ä–ğtò?„ƒág–a3‚óœo*Ì0
 õJ‹wšğèiÅ£À‘Ù0šû£p:ØT–½‹´GÖ1B=ƒúsZèG¤Kª™øèq·swÁĞp ©Ü\Ó˜®·nüzF3pc‡lp”î¤Ô+aJBê(á¤\lÆùSø'´¤b˜³¹ÛõŒIgğl¢¾š“„(áŠM}ï:¢d<ö5Èx¦…\ò3øĞ‡¥naé£3M‰½åˆ£Ä.dÄaÚ>yåã¸¤ñ‰[ÂÌ¸LØÙÂ2cëñŞx\!ƒãŞ>èŠ¤&«îšË')îìH“@¤ÅW7ÔñØ>iÙ£*?ŒvàĞ«,<õe ºxÉÂ“Æ½…B‰h¤×t³ÇÓjÆ²İ¹à ËG*ù¥øşdiwx“òl)¤D€r“D*ÌøqGíY6Šù7Qş¼“¶‡$ïDGtöò`¯ßÓc9íõ;ò{_¤“¨ÌÕìä&/gô×€Ç;øm€ßIßè±hEñàÓÃg«åÀà yş¸›J0â2JÎDK™É˜Aél½ò«åpb8„no×M½Ì'àÍÀÜ
Ğü39¤’VâÂ‰¼•üNíOûƒ[Šì®©|•¼MFl’¨2—H—ßˆæ%ä³©@°ĞÕŒ%•MŸ§²äæÙ(l‡Ú<8AŒ3 f¿‹¦Dâ	Me”ˆ1UÀ‡ˆ
$¾šMÆçd¾V'!÷½ÊTıÁ8b  L¿›”ßùláö”d
ÌVxZÿØzĞÔeMÀBëë¶ÛÍÁ[ÂÔ FóqË«Áõœ‰'&C.ªÑ;v*–‡Ó÷.ß€L^*‡1-…:%}ºHíê¬–_âdĞŠVƒ  Î®_ê5 Â€ôxŒgğq*­'éTeCæœ‘BÒ0>Rü}³‹öWê NJt+^¼ùĞ,ø÷§lá$ÁS7­¨ÓÇ@Î)RA''kã8“t8¾ ñ­¿S/x¦f±ğ?œ¼:{!nöÚëwşÄo¾?q^¾bÜ/vã^éf½¤,	“‹^DãĞÈ}Ö~ßÖk>ZÂ`E£4~¯Ës„Gš+!DhKDYœqqË2*Ö¢Y¼çİ¢£$3b†ıŸ³çÏôA³ò™ÌM¾û`"«Pïà°ß;¸GÊ’RgÊ‚Kœ¶aÿ\M_`
wÆ³ãÙòeY=Š4•–HÃGï,âüIÛáív}Iè>õøzxH¯	ĞÒà~7J#¿›x´ñ< ¡®yJ€Gk‰J,ıãnHÆ¼O'8É^øâ}ÓM()IÙ0rŒ%¢ÍQö`9G{ıg'Ùãî¦64Ø«r+o¶ÁÈ†qY94Zôè^ınC|e™x¼½ïĞÓ´??¶‡m7şQI,C
ĞcJdU
À7Mp,øgÛîÿÕöÈæ»øÿİ­õSS›%¡î<h;õÖv[CxØgĞ;)Mè=ƒ½›xrdí›Bq§ö—ñ›a§Én}>wşÕB—­¦ï¦³SAİ¡<¤ÑöŠLh±ëMG–ÊßJº·KÉËÎ4/ì,p1Dé\­4¹7s—1"š+q'‰<,ùÁŸ‹kñÇâpzVÓn`HÅO­k•|åÓâª§+êTVáI‹çl{İStß“«Ñî²Æ¾¦Aw~Ş}_ä®Od—ÂµïÌıM¸–§s*í™0_3iBdÉŞ']Ò¼0·•éû09L6Âzoß!ƒWöC1!µÇ¸øÃü•S'YJ+X]*ô!<
^¼°<Ş•ÜLYìßC%{E+^xem¿ñô‘î|À$Í—<g ô‰6ÍSº±OÏ‹Á*ÇüÄç© Hˆlût‡ÁÔxté­ı¼Ï'U&}DÜ…ç¦jô21iTDYÊY[ÉjÜƒ(,¥œòLjàº 
X#Z¥^ÉµeŒÜ¡z”BH\YzÜ«+;—‚£àd 0ÖØ(×qÁfŞßáŠ-€„I…˜¤©ô¦7€G´± >ãÃh4Š úQ‡Z¨ônX¯µb‡;¿ïïIºÑK†õ}¼€I0|JÖ…§ …9·0ş<`‡Êşá>^›³å¥Å_ç‹íƒŞ÷g?^¾:|½×ï^÷^ƒqÂŸP&ó"×[LÆ(	-îİåT¸ ¸µiŒSj¿;øüİ´cè³8­Wbö³¢QOî…9+s¨‰s`ãZC8„Ä19»şJ7õ¨ÆÖBpÜZõ¸ëol8ßt©»“L#-*-	¯	o¬›ó¡°(”¼wWsŠÀ<RŠGÖÂĞ>w¤ú—Æ.±R19>öÀ’'å…Mıƒ»ÆrÏú8Ì5b^ä¦/jÔ-P£,ÀeJ2v[®ZJñ‘`¡˜:ÕZ„(L1(ÈpÖ®|D˜Ø$kÄv£±><fNĞ?Z¬•‰p"gòå–Í@NÅTZE;¡Û9æ£ÇjB:O;ÇÉH>S“IG#ÀìşôŠI°“G8ıéğ ˆÒ3:—3ÚeÿåõdÎa2»ì¿éÙÀÃ#VÜâ+`ˆ¹œç|¢XÿÊ¯ö«LÎiÉÛOÕ“µÇk*H³¹´—CoÜ îâ°„¯d}aòÛÉş::|qØïìÜíÂ2¬›àF6Òêi6°¾ªG¬c‰ù´ÃÍ0j½.Ã_%ó¥µÕc‡Iä
‹ÕAÊá±ß'ö´YdˆT=(J‚ÁT +ø‚ë,­ >â¤%&¾}”»ß/)7/àˆ×¢Å¨¶å]‰dôMY˜Û†‘IPyvöÓpqñbx¥o•»Êh+ï©8öE>…Ë¹·Ô"íZ=ÅwÕêi|gWøSãÖ}±Í%ÍÅp‚o”^W×øÂW“_ß^¾êœBÂ=N¼Ìc©†4´]²9-±ŞÖìûÑÚÙ÷ãM²ï,ŞeLñ\ôÌU1;UÔB¥’şğ?(Îå‰‰İNÁ¼Óú¬íbÈ‰Ç–U³Ñìò²^¢±Q©‡SôqÜÅ_±el3\¼R††—Í¶ë›FãÉËàvüGáø© @~fdñ¬€(±àá«ùlÄŞKÇGÆ®‘zxV¬gAì½kå3İJK_`üÁ½÷ó¶¥Læ<äŞmÁÜÂHá¥¥œ¯¦'Ó³ÃşÂ¶ [ıóhz¶Éê½ˆº[ì½Ö“áù¤ò§Â¢1Dûµ³[7h¦bËßVãÓá{ƒ;Ù\3|ï%öò{.~ŞÁ}Ö	u×bYÓ–ï¾Y°~+Ç—¿PV²j1‚a5YÌ0ıÕ—£‹ÎVïQuÅÉ˜—]“'8ıåH²…iÏ_bŸ'×N+~Ct2´VøãÖ•‘ëØÄU¯v°Tß´Dq¹„z@ï'f.Ö›b˜Ùn2l8_gÂÊvË®ëAåÆÊvÉ¶ßZ
<zú»üÎgòØä°KÎŠ8RØÙp„ôo1ØÙz„üyQ\u‚ò(KSÈõ'/|Èİ¥ï“h½y…½SB¢ûhİuå÷“r“æ÷%µüø„ÀÇöy6×üù šÔï™^Ÿï
>/‡õœçõ2&<)ş¼
Ã~Ö	wêó¯â7y†Ö¹ê<I®Ib!k:
ĞÖÕö›z¾Xv;WÛ"ƒt:ïrà6Šâ_Rñ»š×ïáŞ³•
¤´
8uÃÁ(xÒ:õøğü>ÏèŸN¾uó‹åGd^Íût9Ôºû·¿GUÆŸ6ö1ê›¹({m Ê7Á¯§³eıæz°¨–ËzúváN
óONåøØ"8~~$ıvæ•*t"ÿûÿ‚İç;ì¢i¶AGëé^‰¿ p{d6¢ùYêúãïúªËÙoµØÖ~m¼¬—“Ê4â¦{Í&œşánø»o=òdLaÆ8ÉøoŸdWC´Ís`•u+’âøs˜æÎ7¦M	Â–c>_Ø©ß“ÉÂİ2»†…oc4\3¹0¹üËÒ¾¼ZŠEG	ˆdÁA‚ÏiÇH .9Dp;wÏÏáØfËxBÿ@ĞíG9w#(^‚ºG?¿¨Çãjj>;Ç“ÑLqÄ¼K-Øñ-ÙT‡’Ç£ÈÅ'÷ŸÜèN^î‹ÄJıÃşQ/'-¹&B×IîŸ—öù¨eŸûG'§ÅıWËN:<Hö)¤`á`¸Æt%^¶T´:Ÿ0q…EÈ9+j°SªgW‡X:tóÃjQı²ªV•yL|Òµ-«L}o.gcö—Ë–ÈOJ_šåÅƒ6cóÆdÆƒİ¿"ÿ|nïß~¨ÔogGÃ…WÉÊ"~±À‰ß´u«^ô.¯–×şñ¼Z®æSãL5+·Õb9›3¸ü7^öõÕj:åÆmÁK±#©'í³·<á÷™©=ô84Ê¹—#½«‹ê²š'YG•úyÛıã¬Ú“Îh8™œGï6…1úŸÒßçpzŠğ#(^Åæém‰#	¨©€‡åÉ¿œ“Èl_Åk‹—„kåÉvªÙ“`{g3„²¸x›ØÃa®>Ç»£‚éëz°œ¥¤ÈWğÍOÔnŞÄzÎDuªÿö¡Ô¼¼†œQJ{,H9°nÏ|HÎâ`´Ô?’2+gö_%ê.ÄÇ¢†[¸Í¶,i]TDÈDãCÖXpúp^ÜıÍ+˜İMì XµÈµ§èågâ/(‡
#Aâ†º]§›ÒÏ¹‹å|²Íbhõ7»è¹µµ–¨PGäua<†¯}Uı Ëf
­çÃ«¨¸€Ç\Œ¸1}cùvæ¶; ©1‘&ÏH-Åš y€Âo.²?wùÃíMH!‘?GYdí<J LOôj^½/æ6AnÈ!!–S–&HñœÆa‚ê‰mDÚZD§|JSF±6M‰ö¦£‹Ù¼ÿ]t¡è|7Hò}~tz´!âÁÂ…œ¼^‚ã‘(|1­”4â•>ëVC½(¿V“Ñì²{±8…›t-oÈ“‚ìª M´…>³ĞÑOu J‚9«Î`İP½M(^ú:7ó®4İf³ÕÒiÂŞ„©ªÜíD
ô}=œÏ–b8¢YV_5Y*×B#&ÌM›St¸@îˆSt-q3‹vúË“ä^2Ş;d””_p˜.1ËÊrr¬$x¶k7Éóêm=MxI:5ŒpPŞáñiïU¿sòªc…çéÅ1”[:‡Ô‡îêà§®¬¡ğïÉ¾Ä÷­WBÎ:qÖà ìöAHc§;.– $>ÀA;E<)	ÑOäK¦á¨h¶2ï}Ûùúk™ş¤ <Ê–Ş6múY¡ıqªè§ø>n0 –œ†¬ÓğMùFëº7I¹¼S\ôj`Ì‹û¬Àñ†²úYÂLÎNŸšh<İÔy¡ªàÅá¶znEºîsÚ1Mb?ÜA0ô%w13ğ¿H;[>B­Ñ÷±Cpl‡‡¡ˆiyíª&WÕ|; µ%E:=,±`µJö—ÄŞèzL¼ë4»}.'ƒãI"Ó#!é<>2ËôÈF2¯.gï«±X~4ğ!ğİT¼ZÍÎÔrµšÃÚDÄ¢‡b½ü¤ ã >	“İîThz08+¶–
ËdøS4Ö 	Tî‹'¡GcÁ ršÁVÌ²và¤ª&PQ£tgß£	5"šk˜QH[TWFSL÷œÊ^P–ãÌi¢ñˆÖéyÎÔ$aö#ûioƒâ¬m­´ëœÔ'¤ÑK¿	ÈiwÂ‹Uáiëÿî¤ü ,íÃvîú\3ñÂ\+¹{ûµà‡—Õ–ÿå¶Œ+Q½Ë½ë&÷9<n•c¤I[“?dŞøa‰¨-‡óóµ‹-ëF²Ìÿ“6ôzıH|ĞûU©ü30ßñT'âŸÍŞ‚.İƒ‘~\¹çÒ>!' uÔãn‹¥·5O;Î2•"ËA©ğ¾¤Z8o<Ú[Ê…Ú:‚ÍÆä¯Á Û°ØÛü›ØæßØ6C[µÃ¿ÅinÖ´b>ÂæJNa×z#‚¤,¿ÅˆbÜ.!æX…5 úà8æØâ²]<ÑĞù÷³åÏÕõùl8o©ôNÛ³7Z Šmu>ä~‘T	Ğ¯şXO_lÿ6«§[w»wcè$BñM˜:Iş¬¤K—l·şßâ'J*=“èı7@šñ½{|ûõ‚M:‘ŸdM#³F­k€_ÌdÎ(õ¿ÄÕƒKÛ¢·Ë!ZëÚĞ„»µÊÍedÕ}yÓZ M„î›S&’|lİø™‡Êıt/r}Bf[Éş‰W€ÜÙ¼Z¬&Ë´ê&¤¯}"‰È\`'­§B4b“b´qZñ³±x1œßªP¯}¡mõk&ı³Õïìvş»³ûˆıß7øÿ9—ñ¹dtA6¤ÑA£¨$v·s?©Ü‰j(OxÁ£¹ŸÃ_Y˜'¯z¯:ßÿhsĞ;İ—©í>¸VSôîU§*ø£Ü—İÈ|…ÙÜw´û$Ñn)o.dB¼nºı“'ı£qµ¨æ5ßÈxäs¤PhĞ% YOà>6‡ßšD0Ôˆ}
w	¹˜vF¥78åêE½$É¹0+jd—f¨qÚJo­[S å	-µ¸â‹®ùKxßFP%qj‡Ëú}õıõ²ú~õæM5ÓĞ§ô\ÿO\©Òåô"HRÄ$Œm‹F„i½·làiƒşrÄ †Öw·¢5UÄ*L±ÜU<mŸF|¹FW:$äÈˆJa%ocB“cù /P–3>Ö¢8Ë¶!*ÄG2iÄÏZ„,,E2; ñE$õ¤%õÄ4kØR¥¤uÒÛuÚÍÚçµfQnH£ÄØ&ÄÉò‹ì¨æ¢Ùò9œ¿“±·8­¦ãŞ|>›oïA…”8€P^<È\è³=ŸàñeİìÔqøşÖpæ «DJµªÖ!#ã¿¬êÑ;Ò© êõ*F²1\BÂ÷õòzûÅÉAoğËÙáşÏp<ìĞÇä*q{—÷äòuóüÚ;Ú?yÑ¼èîıDä˜pˆÙ´”4kàÙËƒ½~¯#]?9íõ;†hu	³‹ƒšgØlä.’©»~tœëSxláÄÙe¢díúzWçw@J‡¯®¥kdeÛÏ,‘Áşx¦™5/ÛéşO½ƒ³£ŞÁz—lÁíx5©Æ&‘äû]âŠ­nŸÖ9ß[8Ë5LF^	Ü’	¹ÄynOšãÛp]ÿ,şµ^^¼Î‡——/ŞJ3rßíM&³Õøå¼Uo<®>˜|×Wşá¢š¼9”w®gL ÜŸMßÔo¥¥—‘¥)ËVÍ­ŒåSô xÂ¢ªÅşoƒ¬?ì/˜À•ÅÛ×`à*s÷ÛÎıûu:U¸¼\òÈºgZ5¾‹NñİÎô·8w#†ˆö•\Œn‡	Iâ¡B-è°QâÉÉ¥XÖ pá~)†.õÄü¬÷nÄUŸˆj“L÷©%ªBë½år8b'{y!áŠó—àèAÑo‹
Â6#áwïsì¾·ñ6ø´qó‘¼1Ñ$á¡i“¼ŸlÕ¡¨.çdí%{Zı¾%îåİÂ¢ú}POÍ¿g«es²Ì»0kI»9åÙ$ñdƒwê²+²DK¡‚ê—Ä ¦İ®\Pj”ZòØçÁ8¤CÙ¬„èV^ˆğïÿÈ¤„
m¡=¶rcÛ(-Šèû{c£–KÜÌ&cu6­>¨u‚;­µèoú¾ EP’EàÔuŞØI7,3C†A{!ƒs ›\Ğ%Œœ!^|C­kHª KjÊäŠnH.”M`WãµÜ>WJ®úIã<Ò˜˜Ê5dŒ–ÊÆn°òÑ}Âò§L¶şÇ?ÌÙ‚)xuîn†YHY¡>—ÒÅÄ?ÂíDşg à¤%z»‹N‘AàEÿ{r	cÒ’ØH‹Ğ?R¥ô¿¸—İ©BAí$oª¡ó¬òÔvîe3í×
÷‰½‚á[Q×õsâJ
X=Ù„cëQ£\øPjZMBKŠZnÄgÉ¿Rº1’e† ¤"×?Ód®HßÎ~Jğ„Ú5¯€|ŞœÅ¶ú/lø·ó¡q^dø™s3…éT‰;ÙŸR>ïQÈW_Ş?7”Ì× Œä¤D¤"Ej¦8x‚±0^øX–“|†¤5ó1rX`3'‹}µÖ+ªlJ*ÓYÂòcd1‹ª´‘ÇÄ¢x’ULCkªkü9_›Úx}1Äw?rÇ´yD0ˆ*™c\Øı‚k±ÿê‹TŞuì­¦ÿ>8ğ—­Çç˜ˆ§ælœ‰„ï]o5úR¦è3–ô¶zÌzß¸1º’úW™öœüz·ìó‡¾é‹MÌ6<1ÌRÕã#JWŞ‹ôşä%ı‘Y.‘`· 4}ø‹ÆJt Xû¨=Ô+5ÕS&=løS2ö‡­Æ÷JÚ
`
›f>Ê¦.C÷DSÜ-X›Ì†¬ä¾'<–3gLj{4wÉg3æsÒÓÍˆ+iXn§…ZóğÂ¢‰ÜŸ´ñÎ‚½K¾ÛT<Wl²ú /ş—Çƒómû1Îi‹†Ğa>¥G^Ê$
wToˆàTCt¸ÁnÖ¾l·#byœ„èO‡õ.GäÚ±ß¿şÚ;Û»&.¹„|P—w]T„H7à‰ã5<-Ì’D&G)Ë€°Îa‚ç€=NOMú£òr3¬CØá	°›İì nÆ9<dnO–#Yoã#µF“å_º—$³¿l3 ‡ä=$È×éÒ
8®µf“L˜¼¸ı¼x¢·”·ƒdËÕÿBn‚ÏFÉÍ¦“/9}9Päu†§õúÂÃoÓ¡
…ÿyi8xÓˆ'<Ÿ6	^Ë??¬^;†AA{'ì‡M€ñ«[By×AMóÖ(ñ¦iH6ap‰9¿%fÄü'Ã··g“.R?e2÷É¼à´<x‰{*}òğ-áæ.­Ì:µ=Œ(,Ú*ï;l·ãGøÉÊy»1Í­*§of†òËNt ƒ×´-tğòVeëj–p–³W¹f>¡ÃLÊD}«OsÎ/.˜“ÄåÊBZÿö'ÙÖ‰"×¬Âwß³şj,CPÓ}8iÉ§Ê,¬Â‡?ÛÈîëªÚ¸Kš}Í(¢)™Ù¨YÈˆÑ£qj¦“¹º–û±Ñ1¸»Šñ@[bÑÆÖ $¥¦[FQÜñØKzU¯(İO[Ù‹Z••ÕpLr?î,Á	|5î1šÈç1¦‘%>ön8ÒÃ§ìõ’lF‡"¦ÂÕ^!°	~}´l*ÈÌ¦“kÀéÕÍÕz5]†–ˆ˜Ør9UõFä$_.áCÆ‡ìÂĞÁUjâ
*3™MãM5(ç1LJ^™Ê"’;Í·åŒÈ¼ß‘P¶tWõÜ+Ì®×>•&+
?æò¾Èöi_SËQ[
äşÚ­³5+ü‘FWÚXU?ÅÎË	õáÖ~òèÉA=Rév³E"áùXç2üò¨Í$jè)ñú@ò[O$T	ŞÉl8®ÆœD{+\$Co:}ƒ!.,jÑjy1œÔ"ã–š-©íáË
­Ó[Ài”iL Qv‹í7õ|±L‡—Vô¿,Á¿SJ1Ş¦&Ôz6]²ƒÉ§’L€¥•3²gàˆã—l 3ƒgÁ‰¥Á¤Ë0òàÿ|øêm<2× vW³åŒºØ+ÓÂêœ¿jĞó”¼Í+ÓÂíyšš3Y²†;øp%ëGªDfO‘œŸ#†6=J“¢6%q¾ğH?NNg5ò–5©E¥àg5‹¤xB(®ˆµ)¾%`$™[)øWé ì!B{“‹Ûr¶x–."çŒ)ÁÆ¬^XİC9D{‹W"K¡[H”½â;Ù©§ç³?ºÀ/³Õ’ÿä¼®¦£ùõ*&¶nğÚa>´ KkgØ–»šB¢ÅÁÕjqáÚs];ÉÓœ¾vŞáù?Ï¡˜#MLçqI‰ŞU×¬©h#
Çh".½£Û€HÄÚEÚ¬'•0_&µ®è?;;Öz¡Ï;ğâÉ_PÛÃã­İ]QÛa,ßİ.¬†U'b!o®Ãú†«øâoK.Û¹¯¬S=ô©½>â|ÏGeÅç…8+•!­lôù`íkİiæò­yÔk$z«}š›Üõ¡ØuĞã¼–FËË  ÊŒge‘º¾«z
ªNì÷£Êu§føÇ@F[úíMÉîÄ!,„£âªøTı•A‘6V7Ÿb5©ç«–³Õ2_dnñÒ¦¬#aoF	U ÚCÉ[¶ÊW¬ºä6uƒXšìÓ–d‚q ËşÙ	×¾ôošôb6YìÉK¦,Æ\xQ.•©pnAëÉ^º\NRa…¡9	çÖî9—qóóÏJ E†êUô€QY©õBó6«û¼°9@âU?åIl²‡Ÿ1ÑTÀ(Õ	å%½Qy_…Ôo>óJ¢pÁ§X(Jü¿§öÍ{=«Gê^İ¢{Dzç*õJ¢á¿Ñ5ø²œàf,VìÅóÎrö‚Í)ÜíªT˜âïEdÙìY=Ò”ÚŒœ/Ñ‡ªÅòi†Nt	©ZS¿ö^¼Ê<i2¯ØJgƒÅ!x%è‚6rx îI°„›ay„Ò+ç‡°Íòç»çÚ(|/bt„©…ÜĞ‹Â”†Wb¡@š38ÒL¿´TËáÛÉ–ªœk<v ”³š–±ŒÊZT{¹MÈtë’zxö¦ã9ã;&Aî|5=™ö/ ±‚”®öZ;ß¹œuHÜˆœXz]$èÛüîr–Wï1ïvrÂEMxªĞ!$vUYX¤G±hÀrò\‘iN™"ò9•¯Â'Í®¿LÀéERíÇƒìL?ĞQ¸xWÚÍZr1v‹µ•z1I¹¥-6*šÄHc	O‡¥F&§m„GŒéøùbCUp+X¿UÌ/š·
ÅZ=7ı?ÿØ¿ÿøçâ—êt‰TŠøÅ}Şà)>¹{äŒæ§—›PãñmxüEjzt?àÊÂ«“
`fi§[#·Ñr.ağ(;Š´>˜­H2G8ø8ùÀ¹.İ²Õç=Øk/×İ!"­V>ÄEoÁÏˆ÷(¦7â9Švô)Šø9›’ı®³ (©
Ìy±Ì|•Lr¹Ò§îoÅ§™µ@jÀÊôŞ5}kz×"T%Fğd8—MeÄì²‚†?Ìæ§ûs),6?aF_Şò1‚Èi©}yŸ$#¿ùİä±Åˆ™B¦ŒcEiåRéu°o’ßH#O„ß„¶cà:í[Ò¶kË.¾8òãï¯_‰bÈgGø÷*AÑ]9]Y¦ÍM–5¶˜uÍ_;õÜ`³ÃcaDëpÚ
EQY=
ô‹İ½é%Õ@ƒ'¢švT­…”œ’·˜çx«<½¼9¥Gõ+xÔ@„€…‰]×»T~áo1Z™xàáÖ25™¬ûgˆI«Æe\ÚÅEqçøÊç‘#w¹Ï¡PÌAËgËúMÍ¸=cÜûU¨’Ä(şáxxYmù_êbZ’f*føà(c"„GïS‚İ@×Öô] „­õÿ?<HÒ£Ì{9‡ Ôñ¸˜àx%ToÈúÕAb´yå³)¬àÙÍ–Õˆ- `Âc¾š/W‹]À`Ï°bŸéµfÏ:@Z×>’a™!'¹köÒ~W ö‚#Œóùl8±…ğêæAyº·?yzœÖíe†ı“³ã>WPóy›Oîcã'7ıÂ}†óšRB…·Ç\¢Õ* §6×_ÕŠ¯³»‚¿p±EÒ™”ºyUôôŞzù#ÜåË/«æ•HU:1Ëë«JË«àr<¢Ù¤ßÑåşÜsi¯˜›§Ú$Ù&5Sõ˜L-/^.gcyã²äs6)R
	Í:œå³µtA¶Öbx*ğ¤œµ,‡oùu_9H>Ò{Ø¨Í‘ÏÙm)NRTQ‰Xí*.
ËÕQ÷FËú}½¼Ş~qrĞœîÿÔ;8;ê¤@adMBûåìpÿç”’?ì¦ Ê4Ú%8ô×ŞÑşÉ‹ŞàEïôtïG.±²¡ïåZ(Ô›I´#P(œj‡¤…¥CuèŞ­¶Kñ__í6ëôc5Êögg¸(pP±ÿúë@7^ÒµbøC”ÉOÌşÜa¦%Jˆ4fÄ›»ÄEæ¤Aj«Ú4t+'üÃÚã-àq{Ò·Ch4¡ë“4TÕ˜Û14WOµB'F7wœa=]ùù4è9ò[è}L,Ch¨Ñc‡éKâh†í8›Ì/ˆÈ¥›aPØI%F4ıI%ìIIÃv¥ğ&L”Œ€c,Ká¶'˜ÂÀøÄl’ñDCL#6E–btãƒXã]ZhiÍ¸r+æRtµÛ:¹Â(BøUT2(q*º1ÇdÍÕØª)ë¸§WÛ(kÚEœ`ıw¦íÍáë¤£kÁ*bä_sÂZî¬µ+$¢¯Ë‰È¿å°ÌÑ’Õ5“„E«Ø«é»éìÃi7.mIhyx‡Ó¾HW×À%ôÿ§HbÏAİğ-†ïÍf~}P~á÷Ópqñbxõ”ÏûgˆSeh#ylgªUã_¶sÓ¬2Ï™ò<ÙYŸ™;Bó³Èód­iäå°sÎ©s‡>çY´šÂlü’·ù±}r[ÅÛÏê¨Äéæê˜ğ\Ú´ÆèZ'İXŒÀÑÑòêí|çÏ„ÉMRm9Ç‚öàY“?¬ˆ~0ÁˆÜ­pi*R\*ñb­BEÒ]ñÖÍÌóx'U(ùønÁSŸ6‡ØW—³÷ÕÖ–<«÷:iÏÁøu¶G	ş¢%Nğ”;>ğVŞÈ´%!áOÜÙR{B¤—KÕœ¿cQ•l5Á CŠK<¸U·n›‡÷’5£ñ£¾º1Uw]qÛO$wF³€Ñ=gáq Gù¿p×ÿW~ğDmJ¸Â|Òn¸jÆhŸÆë¼§æúïÿÜ¿¿á`Ï}óªÙ¬aˆñ_ã¿põÑÊÇQ/œl;yútu—Ê| ^|¤…É°ÕILÓçÄmÊÿn0vÙM8 ¯yŒ¹âqÚğ8M¹´r²Ï‡;â	P$KƒŸ¤h‚ñø,ê«‰um ‹%ï®-ØÓ¶2 ú¸3ä²KÑY‚‡‹=\á;ĞBµ´
ìå°…ºĞZ‘ùØ=¥äo÷‰ˆ<PD8¬¯r¶hõG£§]ÆáQc*ğ™ğHk:BnÊ	!êÉó&=|‚DEƒZÊN>UûŸ£&ÿ\]+KV·?U…Ô«tùY)¥¹Ù=Ok¤î›OU!5×Nœ<ˆ1nÆTØ/Ò˜|’ÒXÃôğĞå “VPäÎ¶HË°’Çé¨š½‘«Ü?R7{üEû<Âoµ "±eÜÚ'Ğå,;ÏTí^8gU<b{öFš?äRù®µeı™ãë	pª ‘—€Da ûæ‡Ù|uÙ¿¾ª~˜ßÊ‹¿&jòĞ Ixj=ÄøæY>½52å˜6l³{
íU¾®à’¥,ÅTjjÁöĞÃÿäQ12T—ú†bWIã˜°5ìfm-m_ÍÍ6·Ù^óñ5Ÿªd]T@ßo×\‚aÿyÚyÌş¿ÃæÉh²HIÑÕ£ˆÊè#ë¶‰;ê9+ö-EìÕäZJGªšÇ„WìîÌ$×ï°X­à	)£ÅGŸˆs5TîQb\•êh1sõÜô>Ç« ²;¾¦)µßè$Mn9ÉDíµÌÒK°nğšæ¬\~¤ÂÁY$eìÂO›Û€uÑ6TúríÇ6<zWâñ×VºY);?Œé51} ¹¿máÎ`±„¦qÊÇ"ğ­&Z¯îÏN—LšºäzpLº/ÌXÆ¹#Töla]‚”åñ«h=ÀVÿÊ«°h&Aª`à†s¥ÃS^Õ(m\]È[–ùìò`öa
u™D=[ËîÀDÄh?°tAÅ'ˆòéarÈ–eÓ!¬ İm9Æ’!¬>ˆºZfË \ÏôPCã;Àuœ?z3“¬¶¼¢khû
Pk«Ê"H¢˜ĞİÎıNÜkÖn6ƒˆ]R}-í¼‹ì÷”Ødõ†‘Wv›{ã¢¦\c2°ğÑÕáŠ§mRmµÍÀûÎCÃ"Çi&XHıQIÖÃåÅöåğ­]¿’Gƒéã1–¬iáuöVƒİ!€5¡YaóEËÁk©;›R6ã8òV^äÆ>˜’¼‡“Mßø$fİbDõ“>¾v k½x1›Î¸©“ı¥­dÛˆ=G®áº*D4.½s9œ%Jà–ˆqÆVc«ØƒgåÍ½RöÔÌEãT2ŒÛˆ	S”ĞnxZÒo¢1Ç@ìY–§ãŠ˜/Ód_´¶‚Ã3âŒïÀ¾œ9™2á¾›ƒŸ ÒÙú°g1ÀxC5Ôİ$T†¿ß¯êÉøõp¾Ø>è}öãàå«Ã×Œ^÷^ƒb™Úó¼R,×·ÛL@ÛJ&Ÿ¸Çu;±ôêŠ|Áå²Hç÷Ù@¢µ¶ÿ>öÈÿN#ûs}^aOŠ HJ zªüeÒzÂ‰K`“–6’Äb>t¤û°µŞíF¶EI#ÎÓ[>
pzëÇæ” Qy¹È|E Œ¶|”<».-#˜©ŒåÀHA”ü$c¥@ÄÊ^Sgœ(-Ø&$¸‡	·ÄTÀR¦ªÄç©)"H†ş<ó‘ÁÜ »¸Hk¿²˜œ)Ô°ƒgÒäj@æ@Òà@É^ş9¯XÌ)i–´•âf"=ãJîGÏ¥ÌÈÑ9ŒÆƒä÷Š¬•vÁH“àr`Å ÑX(vî	“×.ñ²HãP‚¯W§L†©şìx4-™BeË­Ñœ5ş!ßÇo«Ã7ÇU5Ş²®#Ú;gH1Ë~$=jYÔ|0i›ZIĞU.›h‹‚\ğPc.r·ÈåDlO³ª#ÖÉ€¡Ó’}¦o¯êé”g¾zÊ)Õ5À:_qŸ Uç‹ş®Ò=h³¹A½t=·&k5¯zÒ"cß?1ğd½¶–3‚3/ÒÃj~1
íÔ#—€õ“İ‹LÎà¨&…—wà~xd…±½!™¦0jˆ×”ü¡îğ‹Œ;:ùh«I¡ôçÇÚÕ	îºGŠbh>N˜´•œ†÷;çAêh]»)Û³İ&§k™/Ó*x[³èİ´ ~¢ôéùYÖ5¶²€ßSìnåÂÏ–&úÏ†s?gèmÜFwVÄ†Ûi	²7=ğ¬)t‡g`“ˆÅ;ß?*˜gœJà¼/‘”:<sër_¼ãÒ@x¸Ú±*æáy}EzºÒ+ˆ¦F·:9+âW kcvŠm„¹s@İ‹‹¶ ™.%P.r÷Ñ^ùEK2Â«A.¬èAH_o©/ wâçÙûÄrkOQpğpFfWYi ½³­â2ËBˆLÈyÖÌ‚®trõ/§ÀãUb,[Zˆ…ÔÎ{û=F?û'’®Š% >üzïè¬wºõ]·#ÿ—ô»…G[cFÒóĞ9ªÊ3CÌCößtA=wCâ04aV” pCŒVg(€õŒÏåŒ!¸›Ğs=‡¹î²ÿÒæ
b–˜é95B)=_íĞÛÌ&™ØNü¬I3ä×¤˜¯ü˜$°¿_ö;;w»F:Èé6zª¥J¤5í†Ê$~Ä:—2kõĞ"õéô?†<ßÔ)Y˜"İÕiàê\©®ûÕ<§î°Ödw8¢4jnO	bFÆCZ]È#~hŠAP4tõä‘¬i š7ó.Éñã·%±0õUi¤¿€¯Ü+r4ÇÏ»ù	©8úîµXË‰+H¹pß\ìñÊ=Nâ–êZqœø„´­djØ`Ë[­eƒÇ?U‹€şùê[’â{:×'­v%hK[ÅëpYÍ‡ËÙ¥Tªå+¬5q÷ïmõ…:HŞ¦šl_ÇEZBˆghh‚Wş}Ô8K±U9ìUî>ê_”?œŸ‘ògñÃVú_‰çbÚnĞµR[«–Ö¡Z>ş¢Z;ÜÀİqñµÖM]7RI‰ƒó×‘{¯E²\fÕsjü¯ñ¡U.Å"¬ÿ÷kı(‹Q‹‰­ğøhI ×q‰¿¨~–¼#®<_çâ+İ¢$I××Ş3‚;îÈ©íÜ-Añ¬ÆÕÌi/?LË©°èNCÂ‡"’eeÎÓ÷‘tPâk©&…ÓÌŸgş‹SQ;èÓ)H
WÑŸvnÆÅ[pV¶çN	·ˆQ€«ùlÄŞËSşª™úğõE­‚V¦½ÿ  ÿÿì}ks·±è÷û+F¬ŠkYZ¯EJ–Å¢Š¦(›'¥ˆT|R©ÔÖ’;”ÆÚWvvE©n|ûEãLhÌ,v„r"îÌ 4€îF£©PÉĞ A}¡17‡‡Ò6`»l#5Ü©äCuP¨“Õe3[Œ®š08‘qÓû¢û2ÏŞY Gıç¿ì´ÅŸîD"báĞ{Ñƒû÷+òvb´‚âDö“œÖŠ÷Õ¶•u0{ƒ©šqgPU¾ìĞ—˜0@¿¹&¡ÀòÖ¼ë‚A°«7!·ß£$Ëç«¿JÁÅxbMÇ•È	EyÇ]€ö'Ë£­\Ö½P8àæÄºÿ¥³ÁÔªb½£}ôâó&Ô³±óbï*’‰	SïyUïÓòÿ†£;tZ‡P"')ße“=•aœ;8-—Ëåi5.½Ù$S¬†s^bäuW2/éeê·•ì¤ûkY®ÖË™QÇT§ÄZÇXıM„1™øÁåUäsXm1ÜĞƒzıÖ€/5Èèqud¢PËD F•<ùK"ßèy¤+ábY}„¶>2‰OR y’ÿ¥Z½w¥–ZÅE9˜çËêàÜ¤Û	HE8Rø9JPıFşb&ËT€oP_{ülş«UùzY.FK¶} ¬  Ç_ˆé	Üz5ó´J¬ìÏà½)IŞVâGX”½`)„¿ÖU$+;(UoÂŞ§p©¯ş¾[Î×‹m¥'ï;økştNa¸¶ÂBH´©ú/÷ÿ·'&6^—a,ÿ**Ù vıUšÎº¤9U¥ûÒXaÖ§&ÅÖ=^B4ú"šR•6f+ØÂCìWnÿÎ:-ä"#ñ—Gïÿ}o/S—ÓÊf³Í¬ğã×óQdĞĞBz”<ug–Š<GóÜ\D»æ$/|â—<’Ohƒeñ¦¦´äü1	[LA@”eul–UNlq¶ï£0ªYŒ€á0¼FRgb÷W’¥yû'N¡rÖ„DåW”¥\å{ù)®u\ˆ®°W”öOB]_\ƒl$™6¿283·¨¬Å[İy*ß/CtƒS·¨;•{@ê.tx0‚ñÀ°©ñÀÃÈ>APßé@äešì\bÓ¯4ÔåÌz(ªŒ‹\ªÙùüÓp:ú?æë•õKø‘ûÍ+xÂåşÇ|2.—CÑ=™vJ}¿”ÃÚ†à±ğdµšå²šÛm€ßíğãœj]‘kÄ–xq|øâ¬øŸWG'#e¯¦Å««°ä‚Ñ®:TkıÄíLqtZœ¼=>VL—Wp/1İ…Bİ&&šm+75QìkY¿-õâ¡¼à3¡/sŒz/@è4_Šê…óÃ#&ö»˜cî=eOTŸ0†ÑÛy%¶¹©Õ'"2ääV³G$—rñÔX<Ùtn5Z4Zà´ÿÎ¤]F§+h€¦åÛ‰ååË²Mü©Ótæ™RÃ@–)M^ß,‹&¿ZÍŠ¡ædENÙ4¥âˆÅh]ßç €He1Ç·f:ìÃ0AmeF	y@j¦Û
V_pÉÃ¯ğçpu7Ê
ëûß“1]Uíp~wÂc-7†A‘šÔ€s5|AY%Õ=õ›{”‘ÑÂ+öH_AÎ ¼UYûcU^1š°\O‡#+LĞãG	HšóS©„Û-„—ÔwÎR9×ÖŠP”³î|°1òëÆô|¶èÇ}Ìí…&‡Ø¹td]7D¤ğ†Fó<HYj şÇøû:“/c+–Êo #@Ü¿4Åé¦S›¦Ù¹kvS³&øDŠÌ•:©e3nb èÎIGª’9 •±Çç(^Ûf¼Š˜ú2™š,Â#àŸ ãØÊï¤|`òäVĞ7<›¸ÎÖaŸÀ‚¨÷gâX©a<[’¾}òíÛ'Ù¾Ød#ˆdÜÄ„”o.Ğ)d‘ls…‰3ª1aça²ê(+8Wp ²­}è~aÀFÎ2Á(‘¼æ¢g¬2¿>ÆÒÌÃ¬ÊœuqÏZ¤¶r,QŠ]Ó°‘üQD:gmÓ¹¯I½³«rıg~÷"¼øEI,m—#TD/$…ˆ‡ßgì÷ê}¹,&#†zåwk¿§æuš'ÖYÍF‹c½-qi±µ­80…C^µ­ßnq¥‹¾D	VTwUˆñ‰ƒ÷‹|±¢xõ¦ ç²%Œíìw/Gsm­ĞÙ¥®¡şToõíbhÌ¹tl¥½‚‚h°¢—…Ø2õeU¢	¨Û´¹•ÒpRÖPœ_:&»\Î2ÚšJ	Zk\ÎÙŒØŒ*ÅğxŠsu<%ŞB®..’Ôs7z&€²ésšª¯İù`7vÍ=#`º¿bôœ€õT‰œbªCòyJ«3”°õC‹³ƒéIŞùÊõœ!ğ!æ¦:ÿZ|6*·ø’¶±UÃî;=ĞÛ}ïÖÑÁ¡«¶ğ—êfºw´m}7;àO	}±ÅäT_øzĞ}qæ^5ËÆ›Í³)F†âbcØ°È	kÛîšÙê!ÿùOqÏÖ$>¢Q¢i+›k.$+«eùÅÎÕ¸Y;WeÒº>ŸT4‹Ö#pŞ„«4;šÉ\Ú"±¯pª"Ûßš[}5wDbĞÅ”?4'š©(f”#||æ‰•'¸Ïùöt>.ûv|+Y"MÊ¶aÛì.ìLvÄ¯Z[“0`klõf=›Î'jF¢éá¬i0Cw(‡¤’DUrmCHÊœ´\[pkòI…Ó$2åMSî¸qv¹7Ù”Ûø#fXæèŒÑ!‹ğ´…i°±W8‰²sìÄ)¯pÒtç x=ª–'v <+»x–˜vã½Ëjøv¬3Ë5,”óõª¯ò5Æ`­„,?H·¡¯m’*":½n®2(-”YúqŞæZéÑôElŸ¤oàíæšG8R.?“Yİ³ ÀŒuÓ¹5D¨J=–›×XÑºwÿ÷A¿xğ[¼m(~¯Å•ìØÍÖC:L5Q?»¹cûçÎ¿RÁæ²ºÀM˜øì>ÊïÖƒİÊUFQ²}Yq"ìÈ/_TVx¹=•“j_ØÓ(->Ú·ùXße‘ñá%¯d§7(ñ\nûË¹–—óéóùÕlÂîâ8á` 9‘Rc‰LÉ£±p¥Í.Ê	üäÓĞs0ë+ıÊÖ®ëyÓÕ°Ù€ŸHÄG$œÃĞp.ZÕ¯l§-Ì¿œ‰¦¶—–Î˜×—ä‰Æù%=}Nzr Àùã[çÏU0:óHblNï‹lŸ¯`–m5áTÚØaÒ{)™-šEp·	0’6>
2î÷€êàïÄº.ı¼áêŒá1Q®=!e‡¾¥SQvOAI¢F™)';°a¿k””’7JÒrÁJ’È{›’7—92Ó­s™"›~Ÿ4?ühŒPRÔÎ¸#çX¢nSô§‹òåz)#òîônlÇC½Ínµ‰i“Ÿ›é%Uf¯"A™ìÏn-¶Í]¿“³Ó¼oÈ¾~Š]3DïšüK¥ë¹r÷¡s/³™ËŸXmnzøéeõéí¬^/ó%¿kd‹Ø2Ú4Ó¶næBYÑ8Ö-5Asğ&òb;Z€|s³™f+nvQÎ/>æ¼³Vç‡ºÙ%@®‡«“rönõ>¨ÒFª°Î	[gsPõÕYÇûÿ8|¹ù×Ô­GçwÓ„{ácò÷ìF†ª€_®ÿhäáééşO‡ÃÇû?Ş?¾<|~´ï/
lYç³òg¶N #ß2ŠÁ
~ôµ†GRî Ÿ®ŞKÿAa¥Š_öh~U†»+¹Q5	ã“'«¦·õûw+™/BdŠï#c¥pµp$›ê?ÂVş’;&NYooÚ¶èàoeF<üç"[ÿÕõš¾¡d$r1Nçœ…µÙlğÿ4Z	eÌ-æˆ Ùş©©Ï¤ò»û÷1[l£››5=6ûnò5¼->ö×±›s…¢´+…%ík·U³Âp§rRµgv<Ë–à&oeè*”Îà÷€j¥nœD¬Xô‡Ì&™¤»£½Ù‰m(…2˜ÑMÍ®¸jÃ>‘ip~å±R(¸=RŞV†’·ÏYÊä´}ÑéjÙrªö¦Š»kF"‰ø±exö×‡§ÃƒWoOÎ¢±äHJé(Cw!Ñ»œ=÷ñ–RŒ>Œ].ÂËãÁ‰À*Ñ¢-R@r…¨ŒËPHò…émmI~çßMœä‘*Gâb*í2CÒD‚Üáş.ƒÀTßµyMô;0ƒ¦ë‹ Ñµˆ$«O²ù\ßrfZ³öĞ˜Œ6Æ3›9·¬ĞÊjèwÈØİ¿<:@«.Š‹É¨®Ğ)°Vu©s€Ç\ò•XÈ]˜ QµÛ™rTÍVš„A¯èP’&®úœ–íV‚Î--€Ö }½Àî%¹RæbÂæ#¦•©fGFAs„ègš^m7áù»bƒ¢sÇDÂÑieÑaÒÒİ¦IàŠO[ŸOrwAi¬ğàW¯ ÷ô[øÅ­&¶!KO¬´=+G>@ëÿà¼æook¾®RtË…"	õšN5Æ ïä|è_wh>Ä078+tú.ì©ˆz‡vÏhÓPN·ğkŞÎ/c÷üu¯àxBO(TçJØ\J\ÚÒMÅ}\›}æ­06 ÎÄÜQLbµ`Bônâ
ÑiS
™Kî	Õ¾úªĞ¿—<ª„ÜšCæøâN<‰+ˆw
öª÷WÀí«ÅŒeæİ&äşà"2|ÍêŠJ©D|ÅÃîÏ€ô¯‹@<ß°Éø1÷Ã³ à¥d1»Üı“EGŒâ±‚¤lô5Ê²²/B¡Å§¿y-N >®u·,Ë°E™Zyú[¶ôøâM-?\_„ÎYÔªt1vÖ5X«Ùì"—Lïz×º*¤5¿9äI£î(âZ éÚ°ı‚ªÖñbMK³‘íÎníİv|Ãúö/—Ãş8û×Z\›İÁBzı#ïàêZ êËíá/âİFÄ]"‘'oƒr%Ê'_™iTß¬e ,fÄ<«Ş@æV\¬5â¯ù%…ÀÍ—^Ği |ádA(,â˜ªsw5_¦İÃr4)+Şùİ¶x{¨t[9•Y;r3H"lå¸o0zÆëuYÜ”!µ‚·9È°3’}HğFj\
†Lµïà¥ Ù² »ÖÜ×—·p!º*ó¦íÕfôçAÙäûhŞNÿ07¡ë„Ğõ~€^>ÜÆuÂ—K„M\"d}C¹ŸÏĞá²IMWd_ËØÉî¿ï‚ Õ‚Uå¿bávpÚ-õO¬—æ1‘Æ‹XwQzçväÍŞ6ÖéÓô‡ĞÊŞÙ›•/„ïlá“‡Š;}ô_Oö66I¢÷åp3×!¹ÑE²¦êº‘~Sd`CøL\-mêÊ(ÿ6"ïn©ÕŞëæ¾¥J—û¦üë¢Û»-ºşÂßß$æßaù+àË]Öïú.Ëòh[–‹Éè¢”±.?UõªÆ#>ÚiŞÄËcOdÇl¾„ˆ{"}¥‰Ây¾œÆŒÚ'ÂGBÂõ(‘ç€£‡šLôtáä;OGkìfø[Sîÿ4ödj§SÙ-$yC#ÍÅš¡búz´Mñ+p× l(W—¥Kôh¸]
DŠÚ2‹`^;[:	Ş €»9ß•+•BTgÇey¿G
†npA!dşº´NÊíi¥\Öˆş8SÈ6„yäœhÒYmÄ°34‘3#£`cGzİ—ïª™—÷½CÒ3Gø|zña7cÍ($o€ÛèöìF'‹7R-”§
nãüwlH¢S~†€5y¼e¦©³9£
åhÊ³C!|ş!‡³'³@ŒOGMòSM´œ±'L,êrryD‚‹°F]şUâò¯‚TlìŸğŸbqUÍÓ³j¿#Å—©íÎ>m,˜3ñª'óûí_Èü@šCN¥ùr=·+gŠÍ8'y+k¼Q¯¦¸,§ó%Ÿ×%àïx$DlÄˆDØB1Tç'JzÁx^6(	™jêèfÀ,Kiÿ= `EvG5;OÕ2şê«æ*á“ i‹øP¯‘mÒQ„Ó`t‡ÑS+†vhÚœ6U¡6¼d÷y€“J—63„ÍZ±Å©f…ñ˜_;6já*q­yŒ+4"pˆC[ûÚ‘è´àÕ&¸œá|3)Şv2¦Ô·8´vœÑ&›î¹t¦ª_­Wz›Y'³Áå’‰ì5“ÇëI9.;ÅFqºwµšDV‰æq"°üW¡¸ò?:üå4MÿH=úX•W~†Uh4$Ô#à/­ÀüÀÉÍá$g-7D88è~~.å¸ ŞÄ–kAğ²Ğ¦ˆâü˜œhÀDFUÊM)Rw¤ÉÂ7H
FÆô0ŒĞY³¤Y¬gôğÎ?±fª^UØ9aä²*Ór’ÎÁ úH2
¤‰h¾_İÄîÆ(0Ù…Èd„Yê Œ®FËF¢UûVnUÖÙ;°Š[$ªâOmÀJX¶Ø}P³¹õ¬æ;l¾4”Ú~>dR>¨ÿ€#„º%`+3ùİ“È'Óú]$şn'ÖÚ-©ìKfËJZ”âtB7dé²²¦_r6¼[_s÷ˆçá†Ì³ópçÁw»´Ó]xf´&g9_/Êqâ¦‰rA‰÷ÎíS.Ü¥]uÍy"¡ fm–’Çâ¹ÚÃsidÈ3ÕZd€˜Ö*Ö¦·ïş¿b÷ñ÷~ûèÛÇiîYCCÃkIØ²äİ^¢*h,ıÅh¶?Vú³¹«"._·	%O¿	å6uœ
iª·_}UÜ”b‘7KjEß’İ­?>º&·öü®Äu¡-½¿ó,tvÉ&:»­µZˆ52±élo°³/ySF¤¸ƒ¨àğZÈVlYõI(»düş^2Ï‘iíoÃŒ¯ÙôØa”èk¾Å/æÓiµò®?±N£fvùyT¿9Zğ¤­ıÂ·‘oåYU}¼À€k’L`˜·X¡xñA1ƒáõQ=æÏÅø`±^A–Ù%·L€?²¤%lÜ®&o€B^ÌxõÔ,tó!Ào>ø#kïW”Fşé|	,¬öíş1Â¸„t¿ğÿ™«(§­½b¤^Ğï¬tû
JÀµ>/çÕØ$R]®g¯foÎ¸ÁŸ4@br2_U—ÕÅ¶ãA	¯·Ím•ì'£iÙk~9pÍ¹jÑ¡ºï"Ü±GØî›±¤c¶B	^&ˆ™¸˜Şòó×øE"Ö\,ÇÏÛF+_&Œœ8/Efpë:¶î5'‚Ò¡-B““NµJÛ¥’#(Æ”ZJy_ÂIcM(Évö.	xTşš±ÊoƒÎ	cÔr}ì¯V£‹÷¯G«÷u/HûÊÀ(…²ˆä	(íçÙNnÀ
‡s½ßxâu>˜¥Ê!~úºrÛòŠo¾)Î^=ŸÏÊr\	€ªúoëêâÃĞ‰`øÈ˜cÍÙÁèRÙVrû­€¥0š|Æ(…5å³­{h¼„ÇQ¥-Ñr2dx0Y)ÏT»c¢*	
jß\<uŸÎÇ¥¾ıàğûJ´ÁÙçánO8nÑ¸Œ®q46{SÂ»$!!×#ÉYS¿®fLHé¶-Û/ó4	—“|~¶ã†ã`¥İ‚5é^†%ê‹jRÏßÊ;mş•X8şKˆò3ºÅär½Ï‹j&6g¢"L0„â}Å¨nÁ*+aBÚük’¡Å/õ¤ïÚŠˆV }(bVVZb¥+¼`cX•‚šÈ/æc7Y±r=·g6Q³ûáÔÈ‘™ûºªÒuµÀğÕ¶^—fUÇ…YAnı“*ÛÔ»*Ï‚’æÖ\ê-Z|©’f¸8³İ”1£v‚±’zÊ÷ è‡á@¾ÏÄÖÕêóàå«ç‡ÃÓƒŸŸ¿=>|÷o%zT|a‡!şííÑÁ_‡ ¾=:<MA½*'L¢ÖÇÇ(à_^½<JÛ+6ĞVbT¶w*A-A›ZAÃË“·ÇÇpùRI]âzÃkM•…®™HD•LSi¶ŸRÒØŞS¹®R~¡\{Ä¯{,ÿˆV^ö¨¬Í:¥üt|tú³ç$İXOˆÏ‘=:Ûò8²î¤²C5_å¹€¨ÒÒDw;R.Y®É‰D•lg’ÀHìK‹œË¿“Ùµg¼UfØ
'@=Ê»¾qXSó-írïÄcŠ/#uQ3nieà5C±“hJ‹è·LnÆ_ÃÈ—Œƒöt#ün Ğ«|©ÅÕñ¡#]¦¶Ë˜sÚBÂ&9/‡;”& _øïâ¿Vø”û5úª°ÑEb üMpäÀ:Cø²4ÆcËêİ®ü_Â•õ„aÊ_˜òí0åo¾Áêh+ŠS¥Ğ7yüš°gùÉÅøºu…‘¸Ë¥aªèï^ñ`ËòîË!°mä¼o_ÄÛ;¾ğB(w”&<oøÂ]^ø…nâ„
%‡6P³gæwLÂõï (ò[nÃp%×Ààåşÿ9Ët¬aåî]Sñ“^Wfááşy^0Ó;y	ëY±¥—©l«xâ¿Ù
ß¯7YEUs*¿m‡ªú¶TúÇ¯öŸÜ?øë/ûo'ró™›ñÆdßºTw€şõ^âjÛ4 XHà×;9@ÃVĞùq˜òÂAim2((„d¾ŠŞ¥™¸(2:qJælÚ4¦ÒJŸQKúm¢õÅ«7$¬ò|!¬>Ø êäğd â¡Îº!Ôí¿yõöä¹ºÄƒØzÙ¹`øöäÍáşsü	’’ç€†;É€ky¯	“ÑÍñ6Æ_yÓx÷v×Wˆ3ct8¡U¿ùÆY#àç|>ºø ô|¦“¤ÔÛaõ¤ã}Nµ]V½@tv:YÖ«b=DPj?TµGK6ø±:¥ê#¯*h=cŒ	ÆK`úËdş¼œ”ÂS“fƒn>‚Ç^1c«¬ ×£j)İ.ä–ÛÛ+ªq'˜9ÑÜÃ„:oJÁzk!ÙíA¬ş@üæª„fğ¬gÍ~3nT¾˜0Õˆ~±nÖìÏ¿j«A¢7ö>w*&YUIAÓ9 –kcumN”¨IÖuûI¦±Üïb†Wî—aC+U$;ş¬šfœ”7?ïüµüÜ/¼İ>_±Ççs¦í¿ÌºPÁ"Â‘q<++¶Û½éŸR>NÁ·1©ÅĞE æE·k¯/„²9!óØ	›×,…Qøò·5É«Ñ;;,º¹µ;:)zİÿ¬ü´³×zğë¼šõ¶ú[}èİ6ügÛ"ó¶7|$ìR¯ŸôÌÎz· ö8—«é%à{&Ò¶ûí=ó­ûâ[™X´>ã45ãËå Ñ(VË$ÊÜ¡Ä²DH7Iy(Ñ,:¥ú"C?ä»`TUBg)óëh$.Ş›ÎÑ¹§Üœû%¬V“¾R¨¹»F¸KôîEíVE±‚¤á–fœNÒ^+ªdz¯„ÂuûıÉñbQÅ%ó‰’«¯Û¡EÓ"îÛâO_X–ÓÄ&Ò
v¿ˆ˜/îÑ ¯P<:O-vÑxBŒ]±Ûñ ¸zp`ÿ®÷şÛ,Y€‚æ`G„¥ñ¶,j¹ƒ²AÃö ü÷š­¨ÈÇfg´E‘Ç­ÉZ’TÏ&»ĞfÁnÑId°0ûíÛÓRîh*"†Æcnû&ËÂ‹86E®y¯ßÏWsKlÈ­æ78Xğ7÷òö:g)ØHÁ…#Ò^R*QÅÒ¥fcòùübÍW²‘i×lm¬^Qê±1¬*À(b­—Ù¸mP	¾œlzb6ğõÔû´í°Ó‘ãOôc#ˆPKŒ]áÚĞu)„‘FoèHiCâæk¾gîMÍïœa[ÕÜåÏlº×÷4szÈ¤‘¶ò¸gKZ¹o«qû®¦8OÚ£)•r( &?&vBòÀús9Yğ(£1?Òˆ§j¥‘‚ú¥MR¿Ğb8a%èôJdàë‚â)åávÄ¨Á.~ª)Ğ#â!ÇêÍ=›oÈˆÍTô
kˆ¦­¢®Ì™1Ä{t7ršÖ)­øÓëz†•8õ£2Yó3/¨¾fYØ`eg2N©vAÍbH=‡±0~¡‹Éª™ÌtÁ™TŸ/÷übôOã‰À±ÒDê×_oYÙ¢¡Ø¡rÅ%„XœİwÿşÆ×‘Ê%²Æ³³™¯£÷ªpÍ•G¤9I[(±ÏQáHRØ’™ Öf5›•‘ëû[¹"i
ì¯T!Gµ=í{dV&
Øµ½)Í¡Æ¹Ï>)¤×šjF@»¨YÉUT\ã.£!š‚X¿õyûš[©UrzÈC%|Öç÷CÉüòñ¬šÏ?§£OŞ'•ø½XÕâ€):ñY, ®;‚xÂp:d—hIn£İ¿'bÔ¥S(z¤{ÜJİ22'Å¢W%‹+Zòàô)´Áô£LL# ¾íÂº×ƒÅŠrÜA€|×eåñ©ıä€ …<™‰=îÜìªŞŞ4&Æ:4ìò°½ÚÈvm¿ç†ó$jŞ™5Ü²ÿßuoü{}éÙróüy;pçÁ&Â[BæhY7)Uı!£æÕå4z‡¢NÀü–¶rn’Éí<¾.·CQ§µbs;ßµŞ;ßÓª®gfó«™§{4®yjcî‰ôLšì[ÊÑÙú”(àF"“¹½¾ºŸÊÛöc£U¾(7—±‹H¥.±+·Éw¬J(xŒ¾Á$¢Ü$*Iú˜êB¿r“0«r‹™^TIÓ²¶ƒ£iph}°'¬U1#p¢µĞ¥‰¶[íš.Zãa#i£U¡©;¤‘¦5DfØÑUâk)‹†œL­
9óN£….™uP`™é£õZQ»dëé„;P’
²$¾»ö-×tºÜ;•‘y!yôç%œRàZåø¡vìšrY·ÁM<ä ”kHqİ¦£±Œ×ª´"sL„­û}Û21¶*íd«r½‰²UA€´K˜­
qRA¶_,äL¢¼™M]JuI·­J*í¶ît÷ôÛ&i¸u“IE”Cq¨ÉºÛÀNäîV¥uoUZæòVåZsz«âCh›Û[" ½”¢e¦á`ß"«óö„k\l’“§6n&éÙ\:qURv‰aÃÂ«eµ*ËB‚Lç¶Ña!å-òÖÛ:{ºn:‰’{íôRQÎ'—Ùõkê»Í,oî÷Fà™ÕuÏï(9JÆ€ƒbR?ƒsIÎÀ2ŒIn3Ğ à(a›Å8“İÎL ¯
9¼éJª'm-®ó-­Ó}ÉJR¯
1Y½éarr“×«âÑKZ{¿2=™½SøMğ×Í{Ñöø^ò£N¥} …óL3õÅh¶?Ö»jîŞ¬‘mÌD,JÔQ¬Z›Ÿ!8q>S]yØÎÖ ôh3iÀCÍdEÅ€Pn|›Çò˜›Şi¹üX]”¶ïğUÚç_ü,R³LÊiC‡Dú¥Ch'‡î¹ß*
4©ËHÏøõa#2µcëÛ²XêA’ò
’—âÜV”¿ñ.oü>£ä÷$%÷ÃYÀ¦Q]åRü] íò¶%‰Wµ7Démpİ~ÒÃF@¥xğ?!^„PAMøíµ1»5‹ï<nÃw,ûi±é‰ÙaíÃßj5abİ²šËi·ø—ïOÙ‹N\±)ÙGF0ç-gâs4€[ÒR—`&ÀS7½]¥íN÷a¨íİ¤¶íúâ	uŒA6§¾%èGéãµ]•²êı¨s/G«÷ö°ç¼è6¤ÄùÃq›K‡¹˜§×ºÙ"2˜_îQ1 	[„ÚŒ¹Øqo‡.¤1ş×Ó¢ÁƒUÊšáÉÛˆ¢J·h`tÛˆ¾%2ğ.¯?©­ƒ\!Ò–ï/åùk‚òÕ¬`f2w›ø”¤Ì®üc¶—gôwãëÙéRŞ"S•l_çõq†m€m¼µİzØvå*HÉ5—²ËUı÷Ñ¤«èŸgsĞf‘ÅsPT¡ğÇSıl0¿´ÌÂ7qÖ1gÙÎÜÈk²…MŒSÖK7â©t3x&şyâ¼åaÕøH·JÍæLLÇæ[érB8ªv°POÄ~¼×òJÇˆ%vR"\X•=KÕç½¹ÆL.éOÕ"~,¼*†ÆgßW ÕÚl÷0wrŠ†Çˆ‚
ÅµÈHPÔ“<|JÜæ@|~ïäEî£Ç	=4Vyµ9TsmÚx<.Ç'åÕƒåçú¢#©¡§Q§.còÀ…0Eštèípÿ²GèàÑª\Vó¥œ½WTò‘ÇBÊÏ§ŒOlÔ±‘\½¯&eÑSŸŞêRœŒßj("ÔAœª*İ‹«’aİf$ÚâIµ³Â¨B:æ‹ôÅr>}>¿š„ò·u¹.{N`¬X$¬ıÙx9¯Æ³ºZùÔr={5{{tÆUe=È²WÈ˜–Ç<ïàøÉ#uõœÈ[¡¡ù@d×9 /¬Wè~£	À‘˜îeFbÊ¼±@:xA˜ø¥……A^[ø=íp}âöéb´¬K6Tî¶RÔü§ø[È<Í¢Ñ:, Óc³ë-øb9¿`Ï¥g¥ óV0©¾İz x@øWskQ‚ûÉ5#•¨e÷eÒˆïñ§Ú[&üªKùA®,:¹é· á(ärC¾º_lmÙùœ1g¯9Áà
›5Sñ„}Á™²£ÁÃéGlÆœ©VïõÒXÖ
×±ÔûMG°¨dæÆ¨Ÿa‰ÜÁL6üõîÃ†5OcIëhBû]y ¢/ÅÔX‚Àa#İ¢uèE‡[‘¬E1Ÿù‘»½0ß4vMŒ!¹ö`ÿ&ÖÑ»ÕWáa«¦l^Î&^° 86&Î> ÒÚ&>3”vóšlßœA‰M%N¶±vH$âá(ÖñœL>#ÇÚ"v‰'İH;IMDCBàĞeô³ôtD5é_ŠJÎ¿XŸOªŞ[öÏGÆe h0?0®Ãe}8I¢ÜCsĞô-’ì^Ágs8Ÿ4_ò“Bèe9ÿZ‰—öØ¹Á
¸`âö*ĞEèëæ§X¬K;yÂøÄ£ Úà\ÜİÆ üGÕ¬U€b>ãòÓ¿„¦ÂÜËxıƒ±úgaÒôïëHí
ĞÁ¯S=thü?GCv×^(BEU³B£W>Ê*Itçİº¬W0²!TâY»‡5 qÙÙå<¡ï.aoÉîHÃ¾@š_òó6—n%Ò2TMz ›&Q‘é)º ƒõHK2\;¿¿«6E—)­§«ün¦@ª.Wó1*>ŠH¼^!BnàıÜ®5yŠ¿äÈÙ¹ƒÁü›´m6„–rşz9ÿT1Yö3Hëïñ¸àQ5A¤‘…5Ûo<¤ ØÆ‘µšç êı—£û{üã|uÀÏüÙ¸o Hbi2…wŸêõz-UœˆØñj=c«##^õ$>Í¥°Á#AKZ¥jãf|>ÉCªgˆª÷†T3©çpºX}Æï°¬‰(™x:–K‘gâá(ˆXöÄ°X¸pUüµ¸\4ÏAYŞ@4ZAÌÕDA”ÆĞ©k'Pi¯e)BÕÏE)şyÚB-ã,jE¶™ /ÃøCR°¾}\÷[%ÚßˆGİF^t¶š-Ö«—›ëq×mb÷Í)ËmIáı|@B²ZˆNJEå†Ê#/Qq<I½OW£%HB­!ÙªN’Và1"mèw Õšëñ`YNÊQ]‡çÑP3IÁÑBh@ì	s±üvVı{İØ\‹šdŒ«†şËñşŒë•pÍA»S1‰u¯™Ò± Up’0ìçş}”ÈĞ,êŸ©¶Û ¥!Q}lãÃÌ½S'ë<ïÖ¤Š³4ÓßŸªåèj„J±Ñ
Å;õGÈäÛ€N˜}ó%rì&z¢á+Õ@ğb1¬"Óà¢›/üšZ]å¶nSò¦¬×“:lJ=úé†Èó´®œº‚¨âËøİaºİ¤„<<b>ŞƒN®¶àÿÒõ!Íê«äÁ

FŠã mN„XÃœ,´È‚røçŒK†ï{9Ó–@‰±,CTÏÒ¿¨B5,Ã/y8XÊ½™I¬.û÷dæó-Ä4×Z%½P Ÿ¸j 4õ©2ù~|¢ÂOñMgº$ş¥£SîdŸoƒ¿‹)\øÄ0d
~Ï}°‘…ÿ”6Bl¤@@Rc#Š7Aõ#ğ×K–2+›õ¹¿n.$â62;óÈG«2x!†¶fª.¯ÆÑ5UBk!xò[ƒ‘~‡s]›®—ëÑ’ÇR£Ó$ïWAò~µ´e°Ö	æ×Ø:M€ˆä¬…!|
š5¾ Ø·ûìSôîõ<mâJ×LéQ€Ò»»mİàÅwI\OîÛ\¡ÊFxy›5(/€6¶»ZÜ'm–I‡Ñæ˜= àƒÑjµ¬Î×«¸©UD	ÂK)ğùDà!ÅÅ•ÿ¾ú˜Óå´„ªY@«Îù Ğ ±Ï	iš}¯_÷™+¨Ò»qpùLA•0;ÎWKPca¨Tîıµ3UZ2†doéüÀFBw¾ Jş Êæã0åm;·P%µëZ0‹ø€:
¹\u›£XoÖŞ˜Â7ŸÛ½>í'Ö]§
-1â§}1Í(ÄIğR54ÆTq¡ºÑqÛÁ^m>İ­>fZÛu0ºK±¤P_Ş“³’ ELL–¼4;OŠ‚Äg)rBœ½läác\D£¹
ß-®18Ÿr‘È­È÷Áà¾ñ£p¥‡®¡0œé‰/YÆİ†;¼‰ Ğ
LÀˆAVş!lR1Ü•³î$TÍZ’˜¹–^0øš’/iÿš/Ó$‘ØqH)y±?y#ª„C•Ñ&Bb±ˆ©+€XC—	Â§Š¿…¼ê9§‰¿¢ÆüN<\{Ó}{Ğjå£g"xööt{¨‘u!êT ÜGû*åYºÁ=o=ÓÊÙ¯³N7DûtÉ¦ÎN…¸Há.E¾Ù.S®ƒ<ÉÎ|2¢Õç)wæ——u¹ËÈ;ÊO\ğŸ/G—«Ö+ø£¼àU­0Ğìş«óïK7QË—REJ‚bü°ùÊÿÁÿ-pÎAI’³·'›fÄ&Ó À?òu`Ğ¢:÷Ø†ğWü—¢}ù´?Ømz˜æMVí®q=mÕˆŞÛ%#†ÖfG“u¹VL˜(±‚R³áÂÅ3qB?¸8ûgõ/ÓËåç@}—Ï@bdB!C;Ş“aªÃa›G®,Ø™üÍz6OJÔKXL“d’õPyŒµç8,_ÓWå{úĞ‡³‹åçÅªänd{ŸÓü$$ò¹q„?œt‹Ãp‡G¼ğ5ZQˆCuZªêbÈÜ/ÎŠSïÑÊ>‡‚tì£
—Õ;AØ	nÆŸÑäLæKy-£lXsĞèSíŞ„m€Jàûï>£Íü"¿ºš™Ùóá‘m"T-Œ7V;µ¡º¤Vi,2<ÙXÔp§p?”Œı¸i î ÒsfÎíèİªQzÁòÃ//§	rƒQ2½ƒÍú#„Š°ë‚óZ.nØD*ËŒh'’'P4±ó ğI ğ†™ä×£jÉ7KŸ¯"½Ò$’yàµàèI‘5Õá xpËp–b>„I9=<><8+Æß«ÏşQÁOào;¬ü1s¦ğïşåo¹G4üâ•¦V‚Ù~Q‹$²ÄÛ,V5üSÍÎçŸ†ÓÑ'ø1_¯ô¯©Ğ@ˆ&Øç`kÖèÌ°‚'6ûƒ¯Ó¡è´ª)¿×GD{p" <1Áƒ­:`¼À-j‘}@1Îã¡Åñá‹³â^šq~Ü…WÓâÕ‰ÕS'<¤G/²ÖO¼h4G§D’±àËlÉ3ÉG4_‹6°šÿ+Ø$ˆ@6¯Ş<?|SüøÃâùáéšEş£8>zytÆ>îó(7—€?¶ú°ÈC!ªdh1é¨>KFãºæ6§¨ÿrb/LZŠ›"ğŒ ¿ëòü­Åq@ÉWV|ÏT4%‰Láüğd(û]*œ‰8À	<T5Ï ¶ó=ÉÖ¦‘Ån,ÒFJ`çú…@-ƒš¾ıPÁFr2A±G,×gUOØ_ãRg›ìÉ­)Ö ğíÃİ½ÁÖhœœe	JVŠ6|\Îd_’¦9pÀ7©³Xy4ZÁ×Í:½¾»+Ó1`VÓKEPFº®è9ÖsšÌ JS‚Â§¤}"ÊúÒØFèF<âr¢XKjÉ„áÈ¯AO*ù–!L@“#Ò	ÀfÃ„Xâ	Ağ=Ñ‡!gFM0Ó`ü°ÙAŒŸÈš*´Hf©	ÑØŞ.±oŸÄ<Ëo9¢´ #8s£¿”s8FF
 "`^'ë)Vÿµ¾…9ï^Thv—fX×ĞÎ·1“Ğ­	ÁHÍc£şcò2–À
ÂøU±³MÑÇª¼bì`¹GµØÙøøQ
¢…ãÈHI1N™Ì¯4:à ¸-@%0›¯ªËÏFÄôÈ(ˆOø'§ò‹©â™ZMO4SDïË`º^•C¶Bª‰TO@f§¦‡©[†UEy¥‹˜§­ùI’€Y¢~cñG^üúhŞ‰ ã+ö}`Å%üİIæ=ô5ƒš9‡‡UÌÚ{VŒI¯ü Ã
­J¤óQsÔ³Ô4e¯ê_#Ş„"zB1wFùL”ÿ-¡Õ\Â7ø*óLãR¹G4ê™>ÄÈç›>ÂØ©%3\Ö÷xµ]¼=Z½—İñÕĞ–špˆÈ;M)µ«‘)|ËW²“.°ßXÄßR2°©A¢„-ÆÔUáÂ.QÆ(™®.P™•V<"ÕÂ?]ÏpP^Œj`Œîû€Æ*9Œ‘å(v›ŠÈLmR+ÄÈ*±„zå„¬'úNABkô”©ímå¦õ;Û%¼ª_Ú~óZÂókÏ¹Ÿ¤ğ±;è«ŒfA€‚’Úİˆş(v
uSÀÌT&ÙÅAöK2QŞTy·£f)ÕŒ ïv¦À~Q#!’d¿4ÖóË|†ƒÂşuÚ£oæÜ¤if»¯é_qœ™*I†[ç¶ÑÄæe§
)ï_Of ’6ĞôWÔˆØXIGÉÎïY×KzZ'µ¿çi“´á„£R"ÜGXgÙ¦NiQíei1%UDMÑ	!Ãó³“#LŸ~è~AU«ó™ó*Œ×,‰ûÀ@jòátÔ=Çƒ!g-4Üx“Šo&CÄ<åº‚óò±“‰99¸‰wvÆŞÑ“ä bÜ|¥í²Ğo8>"zRa˜ğãºâ÷HWïËey0±$U‘Îû¥FÍÃ#xªâ›lö@œPè)¿L(ÖX£$=ím‰ğ•[Ûê´4¸¬–õJ?ÚâÖ\•5Ä¾®K¶,)4®*î;$QÖ46˜âÕ›¢}ÆÖğ›îÆ.kn)ÂU'	<¶ÍW„ÍŠe¢ÑÎÍBlûa…ñ§Ú²»Pf[}=1=,ßƒWoOÎâ<8×
{ÿ~|6¤±F¶”\Û„8Äô#š”O•Ğ:ÒŒ½ßìŠÙøe·Cábœ­Wà¹YŸ@RVV(æÒ&ëŸ³şázíŒtzv ¹¶+·¢ôUm¡ø…rÊ_(y
`(í”À»%°=`TLÕJE”Á±fD…0”ÎJa(´3:I9»6Á€¹
b"­éª†ÒQE,Å‘š¢ë‡¡Üy±ê¤§'ş.”\U1åòëlgm1”»£1õæf´ÆP:j¡\—öÊ&5È‰tÓ±’§>Î¯‘÷õ¦UÉPnXeÓZö.ªe(íÔË´şå™çY+_Ó9¥òÁ¢
4õ‡»®èË)¶—`L»…•JÃ=pFğjC!…İfug“rönõ>ùÔ÷hº¥ÆmZ6¥6O¹â!ô"©eìïï¢&™´qoš^İg×8Ë ¼±à’İ÷6@~LîR
\ù•Ç$÷üë ı¾#ôJÈAŸN¬Ô††‡¼/<Ğ@ëNÇùÌ$*?:éı©ŞŞêgå§„³©¿Î«Yo«Ïy½Şu¸"Qã§¨GÔ„ÍcÿN;KKÛrÖ&÷½³Â`şİ2y#á%Š)ò,‹"Œ;g”£¥]¡œ’†İ?¹1+M¶ö)†õ£ã†3Î
“"ˆ‡†TËXÚ‰Ê´£"=ô­¶MŒŠM_º¡%PÑ¦qÉœ¤[ş¥vtrÉ¨K£øí¾ªÁ‘Ç’q0t¯m¦Féqº£vø¡¾Ö"ñ§ônÙ’¥[üª»åH¦ªü!©~¨ ş‚íÆÀØÿëğ(Y±®Qß¦3æAqCXèœ°Ñ0PLv1Ïîaõ 7O€ƒ¯œt F¨¬­$}Õ¶¹á·$Ø‹e9­ÖS’ÿŸ‹ )GŞÌm;Ÿ\¿4ùÄZ8ÀÑ¹Vhv9¯{¸–Aâ pn"ÎgN`£År~ÁÛ‚„š¨&Ñıé[1 Tü';`GêÔô?«åÚ?SÏûşõçfØ2wNò3NpÂŸ¹ÃGS;lõ@Ql¬36»óàG7 8mio‚éÀïFÂúöX?ü<Ÿ”uO]O÷e¡×Ë’	ìåø.=À«W/¿M¼,¢’I… ^ÍÕ;šA@«ùU]˜Uh†.à–%ìíáÄãh+.¦Y¾¡²Êy5‹P“â£û÷û…K„xX,Ñôº)Huİ ûk_ªìš	@owŠgìO
ûªŸX½Y£^•G¹¢9Ç¹àçŒs ²ìàåáó£ıáÙ?^¹Õû´ÉJÄLb3ÁW6VµôlPf$°9+îÌxÕôo{ßf.3@àµÔœºcAB®pË)ÈLo1çğ#MÆM–åj½œá<'45úX”‚dÄ+KêôÑìş§ÅVqß8ê TÖ¾ø‹Ÿã}D„âkÛîzù©¼X3’<ªW½­·¯ŸïŸêŞ)“ŸgN×Ÿa6`!k!]ıwÊô™±L¡=)bä‡úšïl~ºbLdÊïÛïù ù^°ZÛ×‚ÑÏùVßíG§D|ío÷m„o†iğJ*E¥Å”Û‘t1şğ$©ÒÇ²úÍqâµX«¸¦æÔ¨(£œBíFIˆ,Àâx5µİòj‰4/yuì˜ ÕŞƒ¨Ö¦{­j‚ûtnÕ¨şªgM¯Iãyù®š-G³Z˜nø‹¤ªƒ¨2º'§ÖØ/àZé†‘ª–Æjc†,åpB-sR×^LG>²íƒüêœ§5£o5şZ~æÇxÛÒü
úÄßZ&:á¯DL4º~†hN
(Âù±$<Ÿ5äÌ{ş©6ª$hœE¿9|}¼pXœ½r¬•yxŠÓŞ³~úBÑaÏØ¯¶1ÖîÓ)B×ÔÇénÿ‹tE?€}|„ö$Ò„E+ÉÃ5•¬Æ"m(ò˜3Õ¢;˜¦òÒĞei§Üî78¥ÖÛñÕìy9)WåY5-ñ,z1Úè›ˆGÂ¤âê˜mx(hª
ˆ^~ªjÌŠkkğo]€Yåz/XÏ>ÌæW3%ÍŒ!ÑÌtş±´‚YEîU¹@ÃÏ’Ñ¸ÜäNøB3}¾QE£Ê9ë¤n7t‡¨#:q]Ö–ÓšİÌ€ßÕyŒğO¼3Ö³zÈ¸ÚİruİÍpe…7K¹¯\Ÿ]•ù’rAíLûÃh~QNşîºäµ¡£ÖŞüê˜¢Vş±	»¡)Q£FRÿhˆm7<NŠú¹Ğ<ĞêœwãS˜é¦âvñåhõ~0}êÙ‚tßş(fœÏ½k6ıÕ˜	÷çóÑr|6?}Ô¦ÁéÀ½ reu…îşxªŸæ—fğıæ‰äL¨kaí.ÙaqÿB^]iÃY¢u¹‚Å¾pt1¸L™sˆ°³Ãó•nOõ;v–P%âlY}z;«×‹Å|¹*Ç¼åÈAK%3°R"á8õDªuzÿ  ÿÿì=is7²ß÷WŒU•]ªLs-Ù“ØVBK²Ã·ºDg7•J±hr$M™äÈÒk“÷Û× ˜ĞÃC‡í©D&‡@h îFBË6.—dD©§ÒÏÅ£İóÖ´¥%ßA †©-ßjŞIõ@P´†„<B/â~Âe úã&âQ´¨'M[ ®ÄX+ßz5²„N<m&]YqÌv)ãÏ?u/yÂ»‚¿Ãùˆ1Jêe±†¿kV._ˆ5¿oe(MIä÷ÏÎÚ¯÷{¯Ú¯{?·Ïz¿töÿ}&¢l²îªJ–´@.Ì‡gyC„é›PÌ²Á»túÛ€çY:$ÏMüY¹ˆ3ƒõ	4ä­¦¶áB[şaS _b$CÆ¾5×b ·KLœıÈPç*”Yæİ“:ÎŞ)ªîã4:T&Túy [»z6]ˆÀ
Ó ­GÍ„‹da4¦¢º-²xCÎ‰±I
-3ã0¨ú5B¼!å×øc¼ÜQÉ5 ÕÉb…ïßz‚Ã»*í4åæá’]:¿õhëáÓmZÜdo×¾-§G¬H‡ëÁ®X²¡Àœ×¿dŸ"K–§Ç^ó{º¢V±›q@f73èOÚÃ¡\’İÜfú©eu(ü±[eƒQ‡Cõá™óAyÜD””,r eŸcM'Ë“(wëğÙ¨'¼éÌC¹ş9÷c„Î¶ê“(ƒf[ùÏ—‹‹ê‡f]&Ğ]jí;ÂmH:ˆâèzBCœÓ›z;TTYn‡–0ÔÕÛKŠX°/ÎNMî'Õ©_täÈw«Æv<îu“Ğwe“†õÃrCŠÄP,%	¸ÕÄËœFäñµ^ny'Ww‹¨«<ÂáE«W…KîïĞÅ%ÿô"i0da•²fø@êmDQe¹XÂXn#:pø.”ÈhÁoõú?VqŸ6Ê‹”‚w&]nß¹aßso|ƒ‰XûsŒ$˜ÍŠ ôÁV„û£p—{w£aÃ iòêu„µ¼r-KØZ·—²Hœ™«ÖTûCs‚(?íæãñ|’Í>1tè‹º¤eÏ=?7èïÆ#=3ùUm053Ro](O4”JäŞÚ°¾µaY)¦êMs@úñ7ÿ‹W;3TmxßkxW¨ç`¸6hùêa«\àúúÙw³×¸·ßF‚ª ›XD¿êwK“íà)¬HM~û¸N›n‡õ«šåÖÔ^ó èíTltG8‡ğDÊ"Úğ€ßü„<n/o§7¤ëU±Úd‹zÿ”™ºÔÇA‘7zX)jëj„¶Ê	¯ö¢}âR/'ı~¤Ú…¾s·0Šq2}ª¿ÚÈ|Z1) ¥r ¸ÙµH CÜlˆ£ñ°O¾ËŸºÒ¬m‘J¿íÖ5êğnê.”9O^ Ğ®Ã4€Ğ,Q|ŸµkÌÇï¯a+í?#ğ|n« ğ#tóï°ËhÌE¡y=?ìb\ôˆCÃİ:şS†¼EÔãªı(~DR±ó9n³a{8dü_ú±Ëà¸¡‡Şx…÷'©Y!u^Hâ5\¡¬¯Ws' †ZÇXVØê¡
«Vi­0I-›|d¦)[yíÑH³ìÁf7Ü`œ·°w›Ï³MQwènëô!ıU÷šÛ0V!:ô) ™Fó|-(Šm Bı# "Fè~€$ú`SP™ôµ èQ|Æİl“šÒp¿X;QRF c>ØQO ï†oèßÄ_şòá]»­© Sš Z«> u½
¹¦@¶ñ¬2æªWGi×Û)=Ş0<( ¬…W ›GãÖ/x³D~j“LÇqPìÆƒòM '¼îRôYxÛ#“¹ŞClu‡LùIEj® •íao‹&8gí²¿íŞ±Ñ‹fßÖÉËàßb
‡Kl3xbË*´$¯qË©Ñ’=6jn½èX	¸â0ˆª\€«Q-OøQ‡tµfw=ÔB,ßÕ…ÚÕ:TdÃv×=è	¤³õ*å±ÀRÙ„‰¯Œ¢üÅ‰(ïsƒº¡bxêšçTĞpÌ·
j¨¼Ú€2ËŠ!G]ì×‹ÌRŠpá.=I¡Z”—êe_¯€-¿¯j9=Qúhzá6“#ö¯XúŞ&Ï“£ .<ÈLŒ
hÈ#VåjËêœ=® İÀ1}YıæÚíÄn6Ú±*Q÷]xœ™;iF]õ_T£g!„Ã2Šò1cƒ€ÓsX‰¥K‚A[XÒeíE×µC4íÅm€ÿºÂıÏ×^%Íl`Vƒ¬fÉíEf<kë&êla3dšÃSå¹oX.àMå(c¢ÏË2kÚ9åxÀ­ÓÀ24V‡EÔÙòÊ×Æ.St¸õ‘zÛâ”æÎÇ</¦Z6Ä‡Å|nxi„b4eä#tÇS3ÔÍdËŸ-ÂL9%ª‰XüiAÊA‚¼x’uIµàà>†ÇQ9à+d	‘á{PHå(Ÿu&¼@Ùñ	>´ÆÙÅ”­¸!xçƒIÃ•—¼;à	½"¤ ïÖm’.Àwã—OŒ­çUÀƒ‹i{lÓ01-pÃZqãş»T4é0kĞ*?Šº—YÁï"cZ½;¥}D´UÚç¥y1q>@Fİ9Å"İN˜,ÍÎ?ÉŒ	d¥n&*6§…_«q»ƒØV³j	Â^iÃPÍª…Ïùz³ãH½ÆNògI‡}ttÜM:GI cÌÀ£NzœERgóÅ€¿ñeÇæì³y4Í(õ=ÂÁ„•sˆIèü‚Ç™vHìF9qf$lÌe÷3šo¡¶:­ş,äœÛKBÈ9xŒ…æBˆi –“×kÈ¹ÛËË¹!‘.b¡¤^â«ğôë•B°ë! 9;/L1—=ì–?Ä"æ÷Ô3Õß <ô›;Ş“H°µÕ\ØÉ1“oëùƒˆ•–ı$´YìR,^W˜ßT\üºÖ|Ba;aØı"ù?ÔÊÀ«¢ßñÈ‰&œI¸êÎã‚A%îa¢%>·R“tótõ—,ı(ÄÑ¶J_ T6åô+™ıJf-ˆ·˜Ì"+ ïæÖ|‚^fDB‹Vı²)í@:=‰¢”àyb­:ìÚV:Áe£íæ¶ª ”‘Á†ª‘ÈGeÆ$6î æöÑü±!€iO1¬íNûàøuïUç »*¢îÿg÷àÍŞ~ïğMw/ìV&á_¥“a6¹0ĞH2‘ôîÍ:`1ÄA4ˆ0äá-µV‘9Ü‘(’'cvía«ìœ¼¾Jü©E#]Ü«Höáq?›Øó!ëÚ?Ø0úÓÁeö!E+¶+¿ÙuY7À#,cÔˆ—İæÂá26Åüá¨?NÕ’-‘`Œ+jÏûˆ€€Ì Ü½ÃöÙ¿z§ûí½Ü2Š¦©Í&0b'\×b°^<å_%FùÍp]kÕ`¡g˜ù‰~}G\i”)W%~ÛP¾•·óe,XüDÖ1ÁZÜFtTbšµ‰Ø#¥Îµœ|;¨Lò¶Xr73uõ0vÏ°*[c$3î¥è!eëñlÒü`Öú„õ		”¹Ğ#‡ı?d—lç‘^vcŞ#lòû1HÈKNø¾õ;ÍdÅaû?bbS¨)Ì4]‚ĞÍô¸$YYÁÆòÛÃß©S7DÖíğ O}GºÍ(ãh9ÂFëÿqöi2Ğ2¨ˆ´dù<À¬šôÒğUg(Ñô&døúM1Ë´ŒEÉ³ü"]p….ëÅ¨á İ¿¾ñÂtCË‡µrá@-ŞËC]!+]mGµÌòâ®DÎíÚªEºÚÿv·l—à‘“c5ö8±¨A(¸p½—œp¬ì¯âĞ²ËXëB2®Ö§ßı÷á_eÉkõÎ’¬r®‰lÙ‹ƒpPëŒ…§¦}şÍP³ÚhXŒª©šP6ÕÃzÔ=c\(¤(P˜Şqx‹QÂÏŠ	³éUKm L<ˆuº¡ÿ±ŸY)!Wp^ ”œÍq€˜Ë<'³‚³}œzTnˆµPï«Yqk¨7=!å,r7ÛxPbmöŠ5ôUæÇ3a„M¸b\ÜŞÍ-Ö
¶¹…¢üB`Ã¿åî†8Y+İÚeclwtYÅxoê!êío€'6„HCöÿYûwƒ8Ë±ù'ûÊH'‹<ÏÖ0Ğ›@éšSN ¿ø<=Úmçó˜@96Úôí>£¤Ú<5‰¯êîğÊ‡Š‡ï¡¢,ÈméÑ=(ÇYj|ef‚…æöïğCØê]âD”„k÷ \©¼mQlzñ)P¨\rÍÁqÃèÉÂ3X]U¬gÊ)¸80q~êNşh+(x‚°£(CoÃŞDuÌq¼Ù¡!½7ÍDE‹¬·ĞX• “C¢¾¢êzY—Æ1âvXdÑ­*6ááÔ@ ,HdïEÉk§Ğ¬"´/“šëExjìºÎ‘d’<•_i³fAğLŸËgÓ.GšV§ÊÍÎ¯9Áüòo~~g)ñ+{)
Brµf"\.>šæÂƒmLıæ£oşÌ:=ƒ«ûÉ µ
9šJ¬4§ùÍÊiOFŸ”—\À‰“¯Å"¿)d43ø°›OÎ³@{G&pRŒoòÌ‘£Œ½Í%´Ø•Ä[ÁÑ
±38[ìí¸±ÙšåùGÖ±>î/]’lµº$t‘¹wÛö0;ÓÿêgÖ%áàiçÕE§­Bø‚™u‚ÇÙ-£oU7ĞZ*­-­S§¢t7›RJŸ$tŞùï†@ó¶§zõğõÂcUŒ2Õ95CÕÙ”¿´Øvè
ùu›‡3Ÿ[£tr1»db†/¡sÙ1ÄV	xDIåÛ-Ğè(X*÷
üîÜ©‹bBÄ‘Cğm;ùUÀÙ*ál»ı2òE—ÍqX_
àjıÊÚXKSn* L‚Ü){Ì‡N4nèØÙUZ¤ÌsiO"p{Ø¿ši³Wñî”ƒÛQtY|•=qUûc†ûÙñ¬.MÕàLjÙ"÷ÇáÙCÒh¤‘äøtoÿ4yù«(¾·¶›t;İäÛ‡ıaHâWÈËô3Ø'ï©O­Ğ'tGtâ°^‰E@€i*«0EÆDÚ…µ"[WóYƒ£¼sÉ5j˜o§”O£=˜e²Ù'iCÖëşz²ß{yÜíîÿï›ı³nïdŸÍ ¤l±Øx’Ö{Ö/­£ãılR4À¿Ş«×e…²	áÊã?sú£	F»‰D¨‹º7gû§g½ã£ƒ_y¤N¼ d’b÷r~´”èYUO_Ÿ¿9‘]e³X§êîÏí££ıQù:Çt/Ò±öŞœî1ahöÂŒtÚ¬Êªp6mô—èıéâKkİ(¿gå%ÚğfV¼=XqUšPÆ§¬ƒ°½ıWí7İº«ûÕñé¿Û§{!ã%KÈì`A&÷íşdË¹.Î†‚øw şİúxVƒÊ‘Ú*’³ó³¥°%ÆúOgÅ¿3Æ¦+VŸsğ–¸ƒ•ÂĞSÕ×SÅ+®z¿3Ü„d¨[‡íÿôxâaRåIŸçx¶pLª˜ó5/Õë¾Êµ¼Ì‹[ß`ğïß
3ŞÕ%ÅPïºR"ëâ+Êç!§®m>üîÉÖ£Ï¥¾ ¾'6$‚LÔ¿+GòŸĞıˆ\Èßä’‡g©eï —~mm¹È)k-¾Mà	lx‚)±Ê«4yz„u­$á0âŒU	ç †iàâ£¸Ä*ı:G€fF§—k&USrAÈ*bê…d¶íA½¬8;ò/¥ FZ0ğ³Òiñ6‚Œ€ê…?e(ÎxÉA B'1À©‚'9¿€>‚ıag2Lÿ8>ol<{öÌ"L‘Uı8E£±‚†Šù[1/˜ûÉ#2‹£FqÎöËĞo
É½ë”f>ùAªÕb"£ècyœ¼ç«y»nô%~SffHåoª Äë
Œ(c®FéI8ÂQTrÙÛFÔ;»OÔv·í1) ÄòÂÂƒæ<.·ñ[ıCÜ W5NM~ª*gP¾iu†©N-Ü´OfMíƒKå£méáßL°0@ØOºZÑµ«ä9_¢ê‰K¯êñ¯õ,Å®Øg2Ü$ª±ĞpÍK‹Ó­Î"‚Ç¨ÚJÿ¸Ê¦)m'dNÇQ¹Ë(ÉÔãç£*fé$…°Ê¢0rÀÇwM‹Yj‰SœŸòû{ÊĞ(ê«éïÆO@gîòHğ{İ7’,ò!mŠ«lÆáü$èò*æg1†Ò|"Œ!½;8ÖşÊk…Š±²¦"êúXÙ’…V.DÖèìúYXÑø,XØÏ„[»5¬ŒiJT¾ùRY™<IÀMñ2Ë\ğ°áZ	¤Ë;úòÙazÑ=ÍçW"Â]›uÅozntx¤ñ‰ù$gDj>¦vCòWPÇdQf‡Ë•QöZœ)MS„°TEcõXU£“´¦âëÁ ÊÉpÀîÈ÷¬ã sŸú›¯—şƒÎ}«íSıN«L®ñ€££ï^ş®¾è»øB#îİ{q–gßÍÃ(A’¢!¼ÃhP™a&”†¼fl×x6µêL¢!dÅòpÌÚ}–Âlšü¸QÜüÃ™A%®Q5Oÿµ(VşÆâ<áEü¹lîíµÚÊÜ¶4—Õ[£Ğ÷¾%XßyK*-Z Ã¿ğ§?Ÿ]¾K?ÁÇÙlÅTquåñ¾5ê5Šô}/›¨Où|&ÁõäÒ}ßJÿ ÷à‹”Ç‡zßb{põŸÏ¯XOy¡s£Ñw—ıâ>d“ ä¼WÃ1û.`ŒgWÓ|–Ã¯Ble³Ğ¢k¿`T‡1Y§Éÿwä{;OäHAÑéVÈ¹ï[QI×™å¯Ò.(Òîg{cã»R[îÆÆI?#ô Òö;šuF+”õoıßıDôVjnõ}ÏŠ.|ÈnOşÊ~—Yì¹jKcıf•2a6$Œ§o×€§[vıFÒ¸Û¯Î€WÌÙ[öŒŠÑ1¸îZG/
#êá¦ú¾3 £ßË§½·öRâŞïö¿
‡qI=Æ&y!=¦Bbœr3÷„Zó€ï¨ ßˆ@ø¾Æob÷‹1
ä+ŸŠ¡ÜRò G«!‰A5ŠË|ÊÄçFÙÂÎN²£b”U˜Th­â-ò2†nİ™¥=Ÿy)ŸÏgói
‹¹wÎ¸–tÊe´äE-aFwÉyC÷@ñƒ"Ï©–f0´=%@á:%¡ˆ¢ì> Ú…]±a4¯>®B.T$†T‡ è{ÌEİÚí(“uıöÜ‡óª/âÑQàå<ï¹ˆıÁÌL» ´£ªV´ÑÀ[ßôõy“‘ğÔi@×_åÓôb
øØÍGù”¿ï^¦Œ‘ƒvx%¿r"²>[(ŒI1µ	!ä=ißÙ¦@P:l´D¶•³Î/ûå§¯æ×Ô*W}6‡Ğ¯/Ëä£t&{øÆ7,°…@§k˜~Áf #ÉmWÈª :ÚÆá–wªØâ3DßŞŠ%*01È×&ÉR=h†¾Du Q6Š›ûYÃj¤™4F—ìïô²@¢„©1³"Â'à9ä}C1;üÉ--x;Txñ´QFaG‹õ—{:ûÕY
õë\nvK¾œËFĞÏBË.\—,ñ@4®q ZŞ¢ËÜ2Ô­ïgÔ©õ¾­çº_½•P×B·oxdpí¿q'0ìü0€kQƒkğ/¦I®d~sè>éÈXrÙdB·ı¯ôD5ÛôÆeĞÈ^0'¾ºÜÙKˆ¯n#øs«¯B˜üê6xêÛZ~Ö®(²"/†UËÁ÷uQx%óâò‡!önJ¥úÓJÛâ‹¯ªCsæ¨Ïú«#t&§ 7Ó–ud’Ğq6Ùãf)2©Ó#/!àÇ*Z$Âäbá£ú£v>cÅûÒ•}3LÚG{"ÊÕıõ9|İhHÒRÈqfZ_‡sÓP'I9|Q‹¾µ¥
@¦[ºwkSÁ£2hhì>Cö*RüŠD…§Üª4Ÿ\ûäî1şu–_eµãÄ–Ñá °!ÌLæalj“äl2˜ò<ª´>—!d§ò}*Ãïl_U_öxÛü7‘5º§ŠøŞù‘Rv•„˜&¶ı’ÏÒ•Ìœl%3·PW0sW¬İŞhØš;äuuöÊB¾—õçoÊ¶c+[÷_Éép's(;ú*—!ä
¹×ı½Ë‡¢VC”	İ_eéhˆ@ñSÀV2gëNƒUvÊ+„áç¯£¦ÛzBü¢¶H…Ò}pö¡W°ôú sösÉ|#súˆUk¥ô‘\ ÿ¥'ß°Å™ŞÊZqàµò“Áƒ`µN¢2?Sç›ıõƒÙ±#Ó sgKôCêìo`PNÚlw"~ŒÍC<•µ‘û°?»l1f®QÁî}µÖÉ- ­Â£r„Êûi—%"3gouï>”F@µË
&ü1Ù
ÒC=ß¥gLuÖ6í8m2ã[Ñ)B*mÇ¦©¬šª²TdºL,×Ñçj[ĞÊk;>ÀŞˆ
ƒwİi€'é
$ƒ‡œŒ€x^AÒÅ*­`ÕÊ«	/ò“%É$,H—ÖC„¾H¢‚ÈÜ ®CC¾Òƒk¡¥ôî—¤Æıé;©Óg|»€,ÆqIJë Ì¡È%Ú™œü¸dÁı-.#‚’î—t'^‹‘7:úE„Ê0„$	İZRV¤£²ñ„«;dâ¸è¶˜‚U[¨¼*ï’çÉ6û7GÑriÁ‡Ìª¼ã·5Õ}Ë;YêïWX¡¸òø{¾×ÆI¢Éˆ»*«¸7Ì+VvÜÈ	6™Ø%O ”;(·ämf
°Ë+ôù-E£?.åvC¯1‡mÛ¼Û¸œV %]çúºaÉ+±–õh)”–]“•7Â¾@®AUÙ;Hn`Ÿ1@Iš§™WÔ1«(‡cs•–²	^³=c¼îå	¶$LŒ[„˜g[y`d;À¤Ye "ŒÖål7•Ù§k¿W)7Â‘çz&ÎõŒëqèÚ5;Y#¨ôA³Ól³%Tÿ¬/|v5¸FÛ t#©Õ™
wÏ*N’äibŒp¬Èmûãj(GpÌîeíC3´ÆkúÖ¯^‚GSZèL`@«^ú…Z8Úà£µ`Ráş¦‰C¤hïŒï[ÑÓ4´›3\§ı1§{$í‡Ñş– ¾×§ÿˆÓWªIƒz^e£ô ¿h¥“‘®v kZ7qˆªT‚¸ü«o•&>­ƒà;„€ãÜÑÆÕlá‹:»ä{l´%-Ü#ê·ÛŠ¾G™V«Üú‘a&Õ;õp‹ËÑxU Ú¾h×ïŒL@@
ªŠ$?`æÏjµ&©%Ó­-r…Öì”åê\õã'Ì_>Ê¹Şá“îï——±ÔM½ºƒ1/^â÷.Éı˜xZë.C¾®yØD.åkq´ÛøÀ2ÀAW{}ÿ…³K«`ÖÅÅ­å.ÕdT†¬HĞf„›(C¹6£ØŞà³¹¼‰Íê¶é1¨YÙF†gYókj“[hwÃ³®~£F=”MoW¾ËjLi7éÖ7zãºí%ßôF‰_ğÄExâÛ‹p[\ağ¯æ³×Ùù‚³ñˆ2‚>ÿÌà¯Ù'·FÑ°âÙv¥³|QÙ/^Taq‡
§;¢õ^ã*—çƒ¶É°½»ŸtºÇÉSOô9áişÎ?6“71—Q®ÀD‘ş2ló1ŸyÆÊ
Üü\ê“»<n
÷f’±<*b^Åèk!O„ÚäØÃz¡Fêé°ÇÊFl®iÊë0™Â%½•Ôí·àS«Í/Üøê©¥œ2à’ÔSeùZÊ)T1µTLÙZ¤(v•NÕdø,`µ(5Á‡¨ók¾n‚Ø“ŒwFyh“ãÒ_r—uø-;¾$ßG;;C¢ğ2$3.ˆ+ÍF1S…ßzàÔ4>¾_"Îvê‰²ôÕKLƒF/â®İC#0yáş¶Ê+EaQ#){D=›"R_Üó/rˆè—µ¥˜õ\°U
T\¡§óÉñäM§{	3"w¤ÚÁğ#?jp]€Şy Ç½ëtw]Œ°õ•³MoXõg‡wÖmPAã$Y)I“©ü`vSœO†Z•1?÷'Ccû Ï¯à3 Q|n?©—î€ı
Zİ¶ymƒ\†ªrfa¯ÈéÛa¼ŸÚ£·óqÁ¶ME;ûÏi=”ÊÌ,â­²ŞNÒçÕëxŒa-;—¼q	90qAHÎ¥ÁBC©'µTÏX}¾M/²	É#tî~ÁÅ™=v\v÷ÅIy5ÍÏÙ–îb=ÑYÄî|ÃT@sŞ²{™lU[ÕY}d‘ÊÓ	CºÿUd,Ì@DT„¦L€`%(aı‰Q©¥–Õ}U´-Wk‹ÿÅ³ÜTk=j&hoÙ‘*Ü’åpy iÑĞ,ï°Ô’f#‚ĞÄ
y%¥À f{Å¤°üã¿†¿®ğ«¼×ÛkÇÙÌC°0Z(	Ñk ql“óq:}'v¸pRï¦°(t5.CP\–Y¥m%™Ø~3LO÷öO“—¿&ùtaë!8YÒ>Û5l€–Í]#Ô;7 à(½’‡)òë+°>¡ÇWŠ‡Ú½¢Y.Æ³Ä’Èc¤¡ĞhOó±èırvm!Tnˆû·j+¾½?€n6$ãePAT9cÓÿ@RE@=ÅâàÕ—Ù§«TœÓüc‘¸2Y9Xe)ZÒLŠ44“«l‚’§5½Q6yW@¤Lç÷ß^‚ú„ª–åÕrÁ8TnÈ‚àœòñ
ŠòÂoÍxşV‹†!:ö™eeY~ÒÓÍ§]I¥1Nh/sİÿóŸÉ,æ?°åÃú‘ºK$+Dı²¯OßŠ¬{S¶Ğ>ÀbdÜ—ÈAÈz*L7½½€â<†s¢";w
]whäX„<0ƒäÁ®–5± ˜
¨s¡_ÆGí³ÚT3?Cø/HUù À>c9lD.‰P¶[ÑÍ?_˜Øİı¹İõr%ÌÛÄŞœğ^·ıòl1Ši4·/Ö!±–â7²[éjèHé}ĞˆÌ•™_HÅél½Íg=ÛYú±Î0^ÇÇ`3ÎÕÕ~57W;o¥ÂCÃ#·(/€ª€·é0KhsÆƒ0¤§éÕèSÃrÁÀoVñªu›N²‰„hùÏ?éúŸ@'qÆş_Äëş8=äÓ…ûp6¿¸H™7<a2CûŠQq†ò…Äø'#Û“âc:]Î^:Jg©€>CÀdpB\¥ßÈ^’+BÒà¿K€÷4ÅÅ4[²?R£Åjf`%ë?=2äCw;T9ƒÒéy&ŒWW[XŞP)Â ¯úÓş8´'!8| @‚Á
’Ö³`/Å,‡ ©…à'IÅ`Ô/Š„›	ı+5™hYÌš¢gè¯ÒŠÇPŸØMhàùyÈôÙ%ººÚL}·zïréªXKó/[%µcu9³œ\éª8F‘:şN§Ù0uÇ®&@!J©$wÉM/³xö£j±ríår37p&0uB%Ÿ·Êï
rmŠá´¡^n&9:şŸ/$Ûşé.jMœÅ‘ÛreíæC&„¹w¢7“EÊUm÷7ë5¨BäÒ°ÃÓæ†BÑ7’û(g°¡zòÂÔWû
ëîòÒ
}xáüõª:í¯¿ı?   ÿÿ Êoo   ÿÿ —ğØ8