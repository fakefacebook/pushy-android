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

package com.weebly.opus1269.clipman.model;

import android.content.ClipboardManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.weebly.opus1269.clipman.R;

/**
 * A helper class to manage the Clips.db database creation and version management.
 */
public class ClipDatabaseHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Clips.db";

    private static final String TEXT = " TEXT";
    private static final String INTEGER = " INTEGER";
    private static final String SQL_CREATE_ENTRIES = "CREATE TABLE " +
            ClipContract.Clip.TABLE_NAME + " (" +
            ClipContract.Clip._ID + " INTEGER PRIMARY KEY" + "," +
            ClipContract.Clip.COL_TEXT + TEXT + " UNIQUE " + "," +
            ClipContract.Clip.COL_DATE + INTEGER + "," +
            ClipContract.Clip.COL_FAV + INTEGER + "," +
            ClipContract.Clip.COL_REMOTE + INTEGER + "," +
            ClipContract.Clip.COL_DEVICE + TEXT +
            " );";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + ClipContract.Clip.TABLE_NAME;

    private final Context mContext;

    public ClipDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);

        initDbRows(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    /**
     * Initialize the database with some app information and what is on the clipboard
     *
     * @param db the Clips.db database
     */
    private void initDbRows(SQLiteDatabase db) {
        // create a row from the clipboard
        final ClipboardManager clipboard =
                (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipItem item = ClipItem.getFromClipboard(clipboard);
        if (item != null) {
            db.replace(ClipContract.Clip.TABLE_NAME, null, item.getContentValues());
        }

        // create some informative entries

        item = new ClipItem();
        item.setText(mContext.getString(R.string.default_clip_5));
        item.setFav(true);
        long time = item.getTime();
        time = time + 1;
        item.setDate(time);
        db.replace(ClipContract.Clip.TABLE_NAME, null, item.getContentValues());

        item = new ClipItem();
        item.setText(mContext.getString(R.string.default_clip_4));
        item.setFav(false);
        time = time + 1;
        item.setDate(time);
        db.replace(ClipContract.Clip.TABLE_NAME, null, item.getContentValues());

        item = new ClipItem();
        item.setText(mContext.getString(R.string.default_clip_3));
        item.setFav(true);
        time = time + 1;
        item.setDate(time);
        db.replace(ClipContract.Clip.TABLE_NAME, null, item.getContentValues());

        item = new ClipItem();
        item.setText(mContext.getString(R.string.default_clip_2));
        item.setFav(true);
        time = time + 1;
        item.setDate(time);
        db.replace(ClipContract.Clip.TABLE_NAME, null, item.getContentValues());

        item = new ClipItem();
        item.setText(mContext.getString(R.string.default_clip_1));
        item.setFav(true);
        time = time + 1;
        item.setDate(time);
        db.replace(ClipContract.Clip.TABLE_NAME, null, item.getContentValues());
    }
}
