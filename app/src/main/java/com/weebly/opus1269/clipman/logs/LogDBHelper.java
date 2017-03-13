/*
 *
 * Copyright 2017 Michael A Updike
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.weebly.opus1269.clipman.logs;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.firebase.messaging.RemoteMessage;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.msg.Msg;

import org.joda.time.DateTime;

/**
 * A helper class to manage the Logs.db.
 */
public class LogDBHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Logs.db";

    private static final String _ID = "_id";
    private static final String _COUNT = "_count";

    static final String COL_MSG_ID = "msg_id";
    static final String COL_DATE = "date";
    static final String COL_ACTION = "action";
    static final String COL_METHOD = "method";
    static final String COL_EXTRA = "extra";

    public static final String DEF_SORT = "date DESC";

    private static final String TEXT = " TEXT";
    private static final String INTEGER = " INTEGER";

    private static final String TABLE_UPSTREAM = "upstream";
    private static final String SQL_CREATE_UPSTREAM_TABLE = "CREATE TABLE " +
            TABLE_UPSTREAM + " (" +
            _ID + " INTEGER PRIMARY KEY" + "," +
            COL_MSG_ID + TEXT + " UNIQUE " + "," +
            COL_DATE + INTEGER + "," +
            COL_ACTION + TEXT +
            " );";
    private static final String SQL_DELETE_UPSTREAM_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_UPSTREAM;

    public static final String TABLE_FCM = "fcm";
    private static final String SQL_CREATE_FCM_TABLE = "CREATE TABLE " +
            TABLE_FCM + " (" +
            _ID + " INTEGER PRIMARY KEY" + "," +
            COL_METHOD + TEXT +  "," +
            COL_MSG_ID + TEXT + "," +
            COL_DATE + INTEGER + "," +
            COL_EXTRA + TEXT +
            " );";
    private static final String SQL_DELETE_FCM_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_FCM;

    public LogDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_UPSTREAM_TABLE);
        db.execSQL(SQL_CREATE_FCM_TABLE);
     }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        deleteTables();
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public static void logUpstreamMsg(RemoteMessage msg) {
        String action =  msg.getData().get(Msg.ACTION);
        if (Msg.ACTION_MESSAGE.equals(action)) {
            action = "clipboard_sent";
        }
        final LogItemUpstream logItem =
                new LogItemUpstream(msg.getMessageId(), new DateTime(), action);
        LogDBHelper.insert(TABLE_UPSTREAM, logItem.getContentValues());
    }

    /**
     * this will insert or update as needed.
     * If it updates, it will be a new PRIMARY_KEY
     * @param values row to add
     * @return number of rows added
     */
    public static long insert(String table, ContentValues values) {
        final SQLiteDatabase db = App.getLogDbHelper().getWritableDatabase();
        return db.replace(table, null, values);
    }

    public static void emptyTables() {
        final SQLiteDatabase db = App.getLogDbHelper().getWritableDatabase();
        db.execSQL("delete from "+ TABLE_UPSTREAM);
        db.execSQL("delete from "+ TABLE_FCM);
    }


    private static void deleteTables() {
        final SQLiteDatabase db = App.getLogDbHelper().getWritableDatabase();
        db.execSQL(SQL_DELETE_UPSTREAM_TABLE);
        db.execSQL(SQL_DELETE_FCM_TABLE);
    }
}
