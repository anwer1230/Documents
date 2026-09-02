package org.telegram.messenger;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/**
 * MessagesStorage - Replicates DrKLO/Telegram MessagesStorage.java
 * Provides per-account isolated SQLite local persistence for chats, messages,
 * users, sequence states (pts, qts, seq), and cloud drafts.
 */
public class MessagesStorage {
    private static final MessagesStorage[] Instance = new MessagesStorage[UserConfig.MAX_ACCOUNT_COUNT];
    private final int currentAccount;
    private SQLiteDatabase database;

    public static MessagesStorage getInstance(int num) {
        if (num < 0 || num >= UserConfig.MAX_ACCOUNT_COUNT) {
            num = 0;
        }
        MessagesStorage local = Instance[num];
        if (local == null) {
            synchronized (MessagesStorage.class) {
                local = Instance[num];
                if (local == null) {
                    Instance[num] = local = new MessagesStorage(num);
                }
            }
        }
        return local;
    }

    private MessagesStorage(int account) {
        currentAccount = account;
        openDatabase();
    }

    private void openDatabase() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) return;
        try {
            SQLiteOpenHelper helper = new SQLiteOpenHelper(context, "tmessages" + currentAccount + ".db", null, 1) {
                @Override
                public void onCreate(SQLiteDatabase db) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS dialogs (did INTEGER PRIMARY KEY, date INTEGER, unread_count INTEGER, last_mid INTEGER, pinned INTEGER, flags INTEGER)");
                    db.execSQL("CREATE TABLE IF NOT EXISTS messages (mid INTEGER PRIMARY KEY, did INTEGER, uid INTEGER, date INTEGER, data TEXT, read_state INTEGER, send_state INTEGER)");
                    db.execSQL("CREATE TABLE IF NOT EXISTS users (uid INTEGER PRIMARY KEY, first_name TEXT, last_name TEXT, username TEXT, phone TEXT, status INTEGER)");
                    db.execSQL("CREATE TABLE IF NOT EXISTS chats (cid INTEGER PRIMARY KEY, title TEXT, count INTEGER, flags INTEGER)");
                    db.execSQL("CREATE TABLE IF NOT EXISTS params (key TEXT PRIMARY KEY, val TEXT)");
                }

                @Override
                public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
            };
            database = helper.getWritableDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void putPts(int pts, int qts, int seq, int date) {
        if (database == null) return;
        try {
            database.execSQL("INSERT OR REPLACE INTO params (key, val) VALUES ('pts', ?)", new Object[]{String.valueOf(pts)});
            database.execSQL("INSERT OR REPLACE INTO params (key, val) VALUES ('qts', ?)", new Object[]{String.valueOf(qts)});
            database.execSQL("INSERT OR REPLACE INTO params (key, val) VALUES ('seq', ?)", new Object[]{String.valueOf(seq)});
            database.execSQL("INSERT OR REPLACE INTO params (key, val) VALUES ('date', ?)", new Object[]{String.valueOf(date)});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cleanUp() {
        if (database != null) {
            try {
                database.execSQL("DELETE FROM dialogs");
                database.execSQL("DELETE FROM messages");
                database.execSQL("DELETE FROM users");
                database.execSQL("DELETE FROM chats");
                database.execSQL("DELETE FROM params");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
