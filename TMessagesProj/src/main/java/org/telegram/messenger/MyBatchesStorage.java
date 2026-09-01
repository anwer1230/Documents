/*
 * This is the source code of Telegram for Android v. 12.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2024.
 */

package org.telegram.messenger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MyBatchesStorage - Persistent SQLite Database Storage for Sent Message Batches
 * 
 * Manages:
 * 1. Recording batches with targets, message IDs, and sending status.
 * 2. Synchronized Batch Editing: modifies messages across all target chats via TLRPC.TL_messages_editMessage.
 * 3. Total Batch Revocation/Deletion: deletes and revokes messages from all groups via TLRPC.TL_messages_deleteMessages.
 */
public class MyBatchesStorage extends BaseController {

    private static volatile MyBatchesStorage[] Instance = new MyBatchesStorage[UserConfig.MAX_ACCOUNT_COUNT];

    public static MyBatchesStorage getInstance(int num) {
        MyBatchesStorage localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (MyBatchesStorage.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new MyBatchesStorage(num);
                }
            }
        }
        return localInstance;
    }

    public static class BatchTarget {
        public long chatId;
        public int messageId;
        public String chatTitle;
        public String status;
    }

    public static class BatchRecord {
        public String batchId;
        public String text;
        public List<String> mediaUrls = new ArrayList<>();
        public long createdAt;
        public int totalChats;
        public int sentCount;
        public int failedCount;
        public String status;
        public List<BatchTarget> targets = new ArrayList<>();
    }

    public interface BatchActionCallback {
        void onSuccess(String batchId, int affectedChats);
        void onError(String batchId, String error);
    }

    private static final String DATABASE_NAME = "telegram_batches.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_BATCHES = "batches";
    private static final String COLUMN_BATCH_ID = "batch_id";
    private static final String COLUMN_TEXT = "text";
    private static final String COLUMN_MEDIA = "media";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_TOTAL_CHATS = "total_chats";
    private static final String COLUMN_SENT_COUNT = "sent_count";
    private static final String COLUMN_FAILED_COUNT = "failed_count";
    private static final String COLUMN_STATUS = "status";

    private static final String TABLE_MESSAGES = "batch_messages";
    private static final String COLUMN_MSG_ID = "msg_id";
    private static final String COLUMN_CHAT_ID = "chat_id";
    private static final String COLUMN_TARGET_TITLE = "target_title";
    private static final String COLUMN_MESSAGE_ID = "message_id";
    private static final String COLUMN_MSG_STATUS = "status";

    private final DatabaseHelper dbHelper;

    private static class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_BATCHES + " (" +
                    COLUMN_BATCH_ID + " TEXT PRIMARY KEY, " +
                    COLUMN_TEXT + " TEXT, " +
                    COLUMN_MEDIA + " TEXT, " +
                    COLUMN_CREATED_AT + " INTEGER, " +
                    COLUMN_TOTAL_CHATS + " INTEGER, " +
                    COLUMN_SENT_COUNT + " INTEGER, " +
                    COLUMN_FAILED_COUNT + " INTEGER, " +
                    COLUMN_STATUS + " TEXT)");

            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MESSAGES + " (" +
                    COLUMN_MSG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_BATCH_ID + " TEXT, " +
                    COLUMN_CHAT_ID + " INTEGER, " +
                    COLUMN_TARGET_TITLE + " TEXT, " +
                    COLUMN_MESSAGE_ID + " INTEGER, " +
                    COLUMN_MSG_STATUS + " TEXT)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BATCHES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
            onCreate(db);
        }
    }

    public MyBatchesStorage(int num) {
        super(num);
        this.dbHelper = new DatabaseHelper(ApplicationLoader.applicationContext);
    }

    // =========================================================================
    // 1. Persistence & Record Operations
    // =========================================================================

    public synchronized void recordBatchMessage(String batchId, String text, long chatId, int messageId, String chatTitle) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // Insert or ignore parent batch
            ContentValues batchValues = new ContentValues();
            batchValues.put(COLUMN_BATCH_ID, batchId);
            batchValues.put(COLUMN_TEXT, text);
            batchValues.put(COLUMN_CREATED_AT, System.currentTimeMillis());
            batchValues.put(COLUMN_STATUS, "completed");
            db.insertWithOnConflict(TABLE_BATCHES, null, batchValues, SQLiteDatabase.CONFLICT_IGNORE);

            // Insert message link
            ContentValues msgValues = new ContentValues();
            msgValues.put(COLUMN_BATCH_ID, batchId);
            msgValues.put(COLUMN_CHAT_ID, chatId);
            msgValues.put(COLUMN_TARGET_TITLE, chatTitle != null ? chatTitle : String.valueOf(chatId));
            msgValues.put(COLUMN_MESSAGE_ID, messageId);
            msgValues.put(COLUMN_MSG_STATUS, "sent");
            db.insert(TABLE_MESSAGES, null, msgValues);

            // Update stats
            db.execSQL("UPDATE " + TABLE_BATCHES + " SET " +
                    COLUMN_TOTAL_CHATS + " = (SELECT COUNT(*) FROM " + TABLE_MESSAGES + " WHERE " + COLUMN_BATCH_ID + " = ?), " +
                    COLUMN_SENT_COUNT + " = (SELECT COUNT(*) FROM " + TABLE_MESSAGES + " WHERE " + COLUMN_BATCH_ID + " = ? AND " + COLUMN_MSG_STATUS + " = 'sent') " +
                    "WHERE " + COLUMN_BATCH_ID + " = ?", new Object[]{batchId, batchId, batchId});

        } catch (Exception e) {
            FileLog.e("MyBatchesStorage: Error recording batch message: " + e.getMessage());
        }
    }

    public synchronized List<BatchRecord> getAllBatches() {
        List<BatchRecord> records = new ArrayList<>();
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_BATCHES + " ORDER BY " + COLUMN_CREATED_AT + " DESC", null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    BatchRecord r = new BatchRecord();
                    r.batchId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BATCH_ID));
                    r.text = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEXT));
                    r.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT));
                    r.totalChats = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_CHATS));
                    r.sentCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SENT_COUNT));
                    r.failedCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FAILED_COUNT));
                    r.status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS));

                    // Load targets
                    Cursor msgCursor = db.rawQuery("SELECT * FROM " + TABLE_MESSAGES + " WHERE " + COLUMN_BATCH_ID + " = ?", new String[]{r.batchId});
                    if (msgCursor != null) {
                        while (msgCursor.moveToNext()) {
                            BatchTarget t = new BatchTarget();
                            t.chatId = msgCursor.getLong(msgCursor.getColumnIndexOrThrow(COLUMN_CHAT_ID));
                            t.chatTitle = msgCursor.getString(msgCursor.getColumnIndexOrThrow(COLUMN_TARGET_TITLE));
                            t.messageId = msgCursor.getInt(msgCursor.getColumnIndexOrThrow(COLUMN_MESSAGE_ID));
                            t.status = msgCursor.getString(msgCursor.getColumnIndexOrThrow(COLUMN_MSG_STATUS));
                            r.targets.add(t);
                        }
                        msgCursor.close();
                    }
                    records.add(r);
                }
                cursor.close();
            }
        } catch (Exception e) {
            FileLog.e("MyBatchesStorage: Error getting batches: " + e.getMessage());
        }
        return records;
    }

    // =========================================================================
    // 2. Synchronized Batch Edit (TL_messages_editMessage across all target chats)
    // =========================================================================

    public void editBatch(String batchId, String newText, BatchActionCallback callback) {
        if (batchId == null || newText == null || newText.trim().isEmpty()) {
            if (callback != null) callback.onError(batchId, "Invalid parameters");
            return;
        }

        Utilities.globalQueue.postRunnable(() -> {
            List<BatchTarget> targets = new ArrayList<>();
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_MESSAGES + " WHERE " + COLUMN_BATCH_ID + " = ?", new String[]{batchId});
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        BatchTarget t = new BatchTarget();
                        t.chatId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CHAT_ID));
                        t.messageId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_ID));
                        targets.add(t);
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                FileLog.e("MyBatchesStorage: Edit batch query error: " + e.getMessage());
            }

            if (targets.isEmpty()) {
                if (callback != null) {
                    AndroidUtilities.runOnUIThread(() -> callback.onError(batchId, "Batch has no messages"));
                }
                return;
            }

            int successCount = 0;
            for (BatchTarget t : targets) {
                TLRPC.TL_messages_editMessage req = new TLRPC.TL_messages_editMessage();
                req.id = t.messageId;
                req.message = newText.trim();
                req.peer = MessagesController.getInstance(currentAccount).getInputPeer(t.chatId);
                req.no_webpage = true;

                ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
                    if (error != null) {
                        FileLog.e("MyBatchesStorage: Failed to edit message in " + t.chatId + " -> " + error.text);
                    }
                });
                successCount++;
            }

            // Update text in DB
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_TEXT, newText.trim());
                db.update(TABLE_BATCHES, cv, COLUMN_BATCH_ID + " = ?", new String[]{batchId});
            } catch (Exception ignored) {}

            final int finalSuccess = successCount;
            if (callback != null) {
                AndroidUtilities.runOnUIThread(() -> callback.onSuccess(batchId, finalSuccess));
            }
        });
    }

    // =========================================================================
    // 3. Batch Revoke & Delete (TL_messages_deleteMessages)
    // =========================================================================

    public void deleteBatch(String batchId, BatchActionCallback callback) {
        if (batchId == null) return;

        Utilities.globalQueue.postRunnable(() -> {
            Map<Long, ArrayList<Integer>> chatToMsgIds = new HashMap<>();

            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor = db.rawQuery("SELECT " + COLUMN_CHAT_ID + ", " + COLUMN_MESSAGE_ID + " FROM " + TABLE_MESSAGES + " WHERE " + COLUMN_BATCH_ID + " = ?", new String[]{batchId});
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        long chatId = cursor.getLong(0);
                        int msgId = cursor.getInt(1);
                        if (!chatToMsgIds.containsKey(chatId)) {
                            chatToMsgIds.put(chatId, new ArrayList<>());
                        }
                        chatToMsgIds.get(chatId).add(msgId);
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                FileLog.e("MyBatchesStorage: Delete batch query error: " + e.getMessage());
            }

            for (Map.Entry<Long, ArrayList<Integer>> entry : chatToMsgIds.entrySet()) {
                long chatId = entry.getKey();
                ArrayList<Integer> ids = entry.getValue();

                TLRPC.TL_messages_deleteMessages req = new TLRPC.TL_messages_deleteMessages();
                req.id = ids;
                req.revoke = true; // Delete for all users in the group

                ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
                    if (error != null) {
                        FileLog.e("MyBatchesStorage: Failed to revoke messages in " + chatId + " -> " + error.text);
                    }
                });
            }

            // Remove from local database
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.delete(TABLE_MESSAGES, COLUMN_BATCH_ID + " = ?", new String[]{batchId});
                db.delete(TABLE_BATCHES, COLUMN_BATCH_ID + " = ?", new String[]{batchId});
            } catch (Exception ignored) {}

            if (callback != null) {
                AndroidUtilities.runOnUIThread(() -> callback.onSuccess(batchId, chatToMsgIds.size()));
            }
        });
    }
}
