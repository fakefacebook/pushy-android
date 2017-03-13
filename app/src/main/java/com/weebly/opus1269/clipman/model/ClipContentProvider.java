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

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;

import org.joda.time.DateTime;

/**
 * App private {@link ContentProvider} for the saved {@link ClipItem}
 */
@SuppressWarnings("ConstantConditions")
public class ClipContentProvider extends ContentProvider {
    private static final String TAG = "ClipContentProvider";

    private static final String UNKNOWN_URI = "Unknown URI: ";

    // used for the UriMatcher
    private static final int CLIP = 10;
    private static final int CLIP_ID = 20;
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URI_MATCHER.addURI(ClipContract.AUTHORITY, "clip", CLIP);
        URI_MATCHER.addURI(ClipContract.AUTHORITY, "clip/#", CLIP_ID);
    }

    /**
     * Add the contents of the ClipItem to the clipboard, optionally only if item text is new
     *
     * @param context   a {@link Context}
     * @param clipItem the {@link ClipItem} to insert
     * @param onNewOnly only insert if item text is not in database if true
     * @return boolean true if inserted
     */
    public static boolean insert(Context context, ClipItem clipItem, Boolean onNewOnly) {
        if ((clipItem == null) || TextUtils.isEmpty(clipItem.getText())) {
            return false;
        }

        if (onNewOnly) {
            // query for existence and skip insert if it does
            final String[] projection = {ClipContract.Clip.COL_TEXT};
            final String selection =
                    "(" + ClipContract.Clip.COL_TEXT + " == ? )";
            final String[] selectionArgs = {clipItem.getText()};

            final Cursor cursor = context.getContentResolver().query(ClipContract.Clip.CONTENT_URI,
                    projection, selection, selectionArgs, null);
            if (cursor.getCount() != 0) {
                // already in database, we are done
                cursor.close();
                return false;

            }
            cursor.close();
        }

        // do it
        insert(context, clipItem);

        return true;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static Uri insert(Context context, ClipItem item) {
        return context.getContentResolver().insert(ClipContract.Clip.CONTENT_URI, item.getContentValues());
    }

    @SuppressWarnings("UnusedReturnValue")
    public static int insert(Context context, ContentValues[] items) {
        return context.getContentResolver().bulkInsert(ClipContract.Clip.CONTENT_URI, items);
    }


    /**
     * Get the non-favorite and optionally favorite rows in the database
     *
     * @param context     our {@link Context}
     * @param includeFavs flag to indicate if favorites should be retrieved too
     * @return Array of {@link ContentValues}
     */
    public static ContentValues[] getAll(Context context, Boolean includeFavs) {
        final String[] projection = ClipContract.Clip.FULL_PROJECTION;

        // Select all non-favorites
        String selection = "(" + ClipContract.Clip.COL_FAV + " == 0 " + ")";

        if (includeFavs) {
            // select all favorites too
            selection = selection + " OR (" + ClipContract.Clip.COL_FAV + " == 1 )";
        }

        final Cursor cursor = context.getContentResolver().query(ClipContract.Clip.CONTENT_URI, projection, selection, null, null);
        final ContentValues[] array = new ContentValues[cursor.getCount()];
        int count = 0;
        while (cursor.moveToNext()) {
            //noinspection ObjectAllocationInLoop
            final ContentValues cv = new ContentValues();
            cv.put(ClipContract.Clip.COL_TEXT, cursor.getString(cursor.getColumnIndex(ClipContract.Clip.COL_TEXT)));
            cv.put(ClipContract.Clip.COL_DATE, cursor.getLong(cursor.getColumnIndex(ClipContract.Clip.COL_DATE)));
            cv.put(ClipContract.Clip.COL_FAV, cursor.getLong(cursor.getColumnIndex(ClipContract.Clip.COL_FAV)));
            cv.put(ClipContract.Clip.COL_REMOTE, cursor.getLong(cursor.getColumnIndex(ClipContract.Clip.COL_REMOTE)));
            cv.put(ClipContract.Clip.COL_DEVICE, cursor.getString(cursor.getColumnIndex(ClipContract.Clip.COL_DEVICE)));
            array[count] = cv;
            count++;
        }
        cursor.close();

        return array;
    }

    /**
     * Delete all non-favorite and optionally favorite rows
     *
     * @param context    a context
     * @param deleteFavs flag to indicate if favorites should be deleted
     * @return Number of rows deleted
     */
    public static int deleteAll(Context context, Boolean deleteFavs) {
        // Select all non-favorites
        String selection = "(" + ClipContract.Clip.COL_FAV + " == 0 " + ")";

        if (deleteFavs) {
            // select all favorites too
            selection = selection + " OR (" + ClipContract.Clip.COL_FAV + " == 1 )";
        }

        return context.getContentResolver().delete(ClipContract.Clip.CONTENT_URI, selection, null);
    }

    /**
     * Delete rows older than the storage duration
     *
     * @return Number of rows deleted
     */
    public static int deleteOldItems() {
        final Context context = App.getContext();

        final String value = Prefs.getDuration();
        if (value.equals(context.getString(R.string.ar_duration_forever))) {
            return 0;
        }

        DateTime today = DateTime.now();
        today = today.withTimeAtStartOfDay();
        DateTime deleteDate = today;
        switch (value) {
            case "day":
                deleteDate = deleteDate.minusDays(1);
                break;
            case "week":
                deleteDate = deleteDate.minusWeeks(1);
                break;
            case "month":
                deleteDate = deleteDate.minusMonths(1);
                break;
            case "year":
                deleteDate = deleteDate.minusYears(1);
                break;
            default:
                return 0;
        }

        final long deleteTime = deleteDate.getMillis();

        // Select all non-favorites older than the calculated time
        final String selection = "(" + ClipContract.Clip.COL_FAV + " == 0 " + ")" +
                " AND (" + ClipContract.Clip.COL_DATE + " < " + deleteTime + ")";

        return context.getContentResolver().delete(ClipContract.Clip.CONTENT_URI, selection, null);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        String newSelection = selection;
        String newSortOrder = sortOrder;
        final SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        final int uriType = URI_MATCHER.match(uri);
        switch (uriType) {
            case CLIP:
                queryBuilder.setTables(ClipContract.Clip.TABLE_NAME);
                if (TextUtils.isEmpty(sortOrder)) {
                    newSortOrder = ClipContract.getDefaultSortOrder();
                }
                break;
            case CLIP_ID:
                queryBuilder.setTables(ClipContract.Clip.TABLE_NAME);
                if (TextUtils.isEmpty(sortOrder)) {
                    newSortOrder = ClipContract.getDefaultSortOrder();
                }
                /*
                 * Because this URI was for a single row, the _ID value part is
                 * present. Get the last path segment from the URI; this is the _ID value.
                 * Then, append the value to the WHERE clause for the query
                 */
                newSelection = newSelection + "and _ID = " + uri.getLastPathSegment();
                break;
            default:
                throw new IllegalArgumentException(UNKNOWN_URI + uri);
        }

        // Do the query
        final SQLiteDatabase db = App.getDbHelper().getReadableDatabase();
        final Cursor cursor = queryBuilder.query(
                db,
                projection,
                newSelection,
                selectionArgs,
                null,
                null,
                newSortOrder);

        // set notifier
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        throw new UnsupportedOperationException("Unimplemented method: " + TAG);
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {

        Uri newUri = uri;
        final String table;

        final int uriType = URI_MATCHER.match(uri);
        switch (uriType) {
            case CLIP:
                table = ClipContract.Clip.TABLE_NAME;
                break;
            case CLIP_ID:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
            default:
                throw new IllegalArgumentException(UNKNOWN_URI + uri);
        }

        // this will insert or update as needed.
        // If it updates, it will be a new PRIMARY_KEY
        final SQLiteDatabase db = App.getDbHelper().getWritableDatabase();
        final long row = db.replace(table, null, values);

        if (row != -1) {
            newUri = ContentUris.withAppendedId(uri, row);

            AppUtils.logD(TAG, "Added row from insert: " + row);

            getContext().getContentResolver().notifyChange(newUri, null);
        }

        return newUri;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        int insertCount = 0;
        final int uriType = URI_MATCHER.match(uri);
        final SQLiteDatabase db = App.getDbHelper().getWritableDatabase();

        switch (uriType) {
            case CLIP:
                db.beginTransaction();
                for (final ContentValues value : values) {
                    final long id = db.insert(ClipContract.Clip.TABLE_NAME, null, value);
                    if (id > 0) {
                        insertCount++;
                    }
                }
                db.setTransactionSuccessful();
                db.endTransaction();
                break;
            case CLIP_ID:
                throw new IllegalArgumentException(UNKNOWN_URI + uri);
            default:
                throw new IllegalArgumentException(UNKNOWN_URI + uri);
        }

        AppUtils.logD(TAG, "Bulk insert rows: " + insertCount);

        getContext().getContentResolver().notifyChange(uri, null);

        return insertCount;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {

        final int uriType = URI_MATCHER.match(uri);
        final String table;
        String newSelection = selection;

        switch (uriType) {
            case CLIP:
                table = ClipContract.Clip.TABLE_NAME;
                break;
            case CLIP_ID:
                table = ClipContract.Clip.TABLE_NAME;
                final String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    newSelection = ClipContract.Clip._ID + "=" + id;
                } else {
                    newSelection = selection + ClipContract.Clip._ID + "=" + id;
                }
                break;
            default:
                throw new IllegalArgumentException(UNKNOWN_URI + uri);
        }

        // do the delete
        final SQLiteDatabase db = App.getDbHelper().getWritableDatabase();
        final int rowsDeleted = db.delete(
                table,
                newSelection,
                selectionArgs);

        AppUtils.logD(TAG, "Deleted rows: " + rowsDeleted);

        getContext().getContentResolver().notifyChange(uri, null);

        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final String table;
        String newSelection = selection;

        final int uriType = URI_MATCHER.match(uri);
        switch (uriType) {
            case CLIP:
                table = ClipContract.Clip.TABLE_NAME;
                break;
            case CLIP_ID:
                table = ClipContract.Clip.TABLE_NAME;
                final String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    newSelection = ClipContract.Clip._ID + "=" + id;
                } else {
                    newSelection = newSelection + " and " + ClipContract.Clip._ID + "=" + id;
                }
                break;
            default:
                throw new IllegalArgumentException(UNKNOWN_URI + uri);
        }

        // do the update
        final SQLiteDatabase db = App.getDbHelper().getWritableDatabase();
        final int rowsUpdated = db.update(
                table,
                values,
                newSelection,
                selectionArgs);

        getContext().getContentResolver().notifyChange(uri, null);

        AppUtils.logD(TAG, "Updated rows: " + rowsUpdated);

        return rowsUpdated;
    }
}
