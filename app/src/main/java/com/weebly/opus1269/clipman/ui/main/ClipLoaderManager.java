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

package com.weebly.opus1269.clipman.ui.main;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.View;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.ClipContract;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Device;

/**
 * This class manages most everything related to the main RecyclerView
 *
 */

class ClipLoaderManager implements
        LoaderManager.LoaderCallbacks<Cursor>,
        View.OnClickListener {

    private final MainActivity mMainActivity;

    // Adapter being used to display the list's data
    private ClipCursorAdapter mAdapter = null;

     ClipLoaderManager(MainActivity activity) {
        mMainActivity = activity;

        setupRecyclerView();

        // Prepare the loader. Either re-connect with an existing one, or start a new one.
        //noinspection ThisEscapedInObjectConstruction
        mMainActivity.getSupportLoaderManager().initLoader(0, null, this);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implement LoaderManager.LoaderCallbacks<Cursor>
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Retrieve all columns
        final String[] projection = ClipContract.Clip.FULL_PROJECTION;
        final String queryString = mMainActivity.getQueryString();

        String selection = "(" +
                "(" + ClipContract.Clip.COL_TEXT + " NOTNULL) AND (" +
                ClipContract.Clip.COL_TEXT + " != '' )";

        if (mMainActivity.getFavFilter()) {
            // filter by favorite setting selected
            selection = selection + " AND (" + ClipContract.Clip.COL_FAV + " == 1 )";
        }

        String[] selectionArgs = null;
        if (!TextUtils.isEmpty(queryString)) {
            // filter by search query
            selection = selection + " AND (" + ClipContract.Clip.COL_TEXT + " LIKE ? )";
            selectionArgs = new String[1];
            selectionArgs[0] = "%" + queryString + "%";

        }
        selection = selection + ")";

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(
                mMainActivity,
                ClipContract.Clip.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                ClipContract.getSortOrder());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);

        if (data == null) {
            return;
        }

        if (AppUtils.isDualPane()) {
            // Update the selected item and ClipViewer text.
            // Can't create a new fragment here.
            // see: http://goo.gl/IFQkPc
            if (data.getCount() == 0) {
                mAdapter.setSelectedItemID(-1L);
                mMainActivity.startOrUpdateClipViewer(new ClipItem());
            } else {
                int pos;
                if (mAdapter.getSelectedItemID() == -1L) {
                    pos = mAdapter.getSelectedPos();
                } else {
                    pos = mAdapter.getPosFromItemID(mAdapter.getSelectedItemID());
                }
                pos = Math.max(0, pos);
                data.moveToPosition(pos);
                final int index = data.getColumnIndex(ClipContract.Clip._ID);
                mAdapter.setSelectedItemID(data.getLong(index));
                mMainActivity.startOrUpdateClipViewer(new ClipItem(data));
            }
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implement View.OnClickListener
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onClick(View v) {
        final ClipCursorAdapter.ClipViewHolder holder;
        final int id = v.getId();

        switch (id) {
            case R.id.clipRow:
                holder = (ClipCursorAdapter.ClipViewHolder) v.getTag();
                onItemViewClicked(holder);
                break;
            case R.id.favCheckBox:
                holder = (ClipCursorAdapter.ClipViewHolder) v.getTag();
                onFavClicked(holder);
                break;
            case R.id.copyButton:
                holder = (ClipCursorAdapter.ClipViewHolder) v.getTag();
                onCopyClicked(holder);
                break;
            default:
                break;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Package private methods
    ///////////////////////////////////////////////////////////////////////////

    ClipCursorAdapter getAdapter() {
        return  mAdapter;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods
    ///////////////////////////////////////////////////////////////////////////

    private void onItemViewClicked(ClipCursorAdapter.ClipViewHolder holder) {
        getAdapter().setSelectedItemID(holder.itemID);
        mMainActivity.startOrUpdateClipViewer(holder.clipItem);
    }

    private void onFavClicked(ClipCursorAdapter.ClipViewHolder holder) {
        final boolean checked = holder.favCheckBox.isChecked();
        final long fav = checked ? 1 : 0;
        final Uri uri = ContentUris.withAppendedId(ClipContract.Clip.CONTENT_URI, holder.itemID);
        final ContentValues cv = new ContentValues();

        holder.clipItem.setFav(checked);

        cv.put(ClipContract.Clip.COL_FAV, fav);
        mMainActivity.getContentResolver().update(uri, cv, null, null);
    }

    private void onCopyClicked(ClipCursorAdapter.ClipViewHolder holder) {
        holder.clipItem.setRemote(false);
        holder.clipItem.setDevice(Device.getMyName());
        holder.clipItem.copyToClipboard();
        Snackbar.make(holder.itemView, R.string.clipboard_copy, Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Initialize the main {@link RecyclerView} and connect it to its {@link ClipCursorAdapter}
     */
    private void setupRecyclerView() {
        final RecyclerView recyclerView = (RecyclerView) mMainActivity.findViewById(R.id.clipList);

        // required for animations to work?
        recyclerView.setHasFixedSize(true);

        // Create an empty adapter we will use to display the loaded data.
        // We pass null for the cursor, then update it in onLoadFinished()
        // Need to pass Activity for context for all UI stuff to work
        mAdapter = new ClipCursorAdapter(mMainActivity);
        recyclerView.setAdapter(mAdapter);

        // handle touch events on the RecyclerView
        final ItemTouchHelper.Callback callback = new ClipItemTouchHelper(mMainActivity);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(recyclerView);
    }

}
