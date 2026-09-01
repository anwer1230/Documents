package org.telegram.messenger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/**
 * MyMessagesStorage
 * Local SQLite storage for tracking sent batches, scheduled campaigns, and sent message IDs.
 */
public class MyMessagesStorage {

    private static final String DATABASE_NAME = "tg_custom_messages.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_BATCHES = "batches";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_TEXT = "text";
    public static final String COLUMN_TARGET_COUNT = "target_count";
    public static final String COLUMN_SUCCESS_COUNT = "success_count";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_PROTECTION_MODE = "protection_mode";
    public static final String COLUMN_CREATED_AT = "created_at";

    private static volatile MyMessagesStorage instance;
    private final DatabaseHelper dbHelper;

    public static MyMessagesStorage getInstance(Context context) {
        if (instance == null) {
            synchronized (MyMessagesStorage.class) {
                if (instance == null) {
                    instance = new MyMessagesStorage(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private MyMessagesStorage(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public static class BatchRecord {
        public long id;
        public String title;
        public String text;
        public int targetCount;
        public int successCount;
        public String status;
        public String protectionMode;
        public long createdAt;
    }

    public synchronized void insertBatch(BatchRecord record) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, record.id);
        values.put(COLUMN_TITLE, record.title);
        values.put(COLUMN_TEXT, record.text);
        values.put(COLUMN_TARGET_COUNT, record.targetCount);
        values.put(COLUMN_SUCCESS_COUNT, record.successCount);
        values.put(COLUMN_STATUS, record.status);
        values.put(COLUMN_PROTECTION_MODE, record.protectionMode);
        values.put(COLUMN_CREATED_AT, record.createdAt);

        db.insertWithOnConflict(TABLE_BATCHES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public synchronized List<BatchRecord> getAllBatches() {
        List<BatchRecord> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_BATCHES, null, null, null, null, null, COLUMN_CREATED_AT + " DESC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                BatchRecord r = new BatchRecord();
                r.id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                r.title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                r.text = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEXT));
                r.targetCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TARGET_COUNT));
                r.successCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SUCCESS_COUNT));
                r.status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS));
                r.protectionMode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROTECTION_MODE));
                r.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT));
                list.add(r);
            }
            cursor.close();
        }
        return list;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String createTable = "CREATE TABLE " + TABLE_BATCHES + " ("
                    + COLUMN_ID + " INTEGER PRIMARY KEY, "
                    + COLUMN_TITLE + " TEXT, "
                    + COLUMN_TEXT + " TEXT, "
                    + COLUMN_TARGET_COUNT + " INTEGER, "
                    + COLUMN_SUCCESS_COUNT + " INTEGER, "
                    + COLUMN_STATUS + " TEXT, "
                    + COLUMN_PROTECTION_MODE + " TEXT, "
                    + COLUMN_CREATED_AT + " INTEGER);";
            db.execSQL(createTable);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BATCHES);
            onCreate(db);
        }
    }
}
