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

import android.content.ContentValues;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.ThreadedAsyncTask;
import com.weebly.opus1269.clipman.model.ClipContract;
import com.weebly.opus1269.clipman.model.ClipItem;

/**
 * Handle swipe to dismiss on the RecyclerView
 */
class ClipItemTouchHelper extends ItemTouchHelper.SimpleCallback {
    @SuppressWarnings("unused")
    private static final String TAG = "ClipItemTouchHelper";

    // Activity we are in
    private final MainActivity mActivity;

    // Item that may be undone
    private UndoItem mUndoItem;

    ClipItemTouchHelper(MainActivity activity) {
        super(0, ItemTouchHelper.RIGHT);

        mActivity = activity;
        mUndoItem = null;
    }

    private static void drawBackground(RecyclerView.ViewHolder viewHolder, float dX, int actionState) {
        final View backgroundView = ((ClipCursorAdapter.ClipViewHolder) viewHolder).clipBackground;

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            //noinspection NumericCastThatLosesPrecision
            backgroundView.setRight((int) Math.max(dX, 0));
        }
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        // This is for drag and drop. We don't support this
        return false;
    }

    @Override
    public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
        if (direction == ItemTouchHelper.RIGHT) {
            // delete item
            final ClipCursorAdapter.ClipViewHolder holder = (ClipCursorAdapter.ClipViewHolder) viewHolder;

            final int selectedPos = mActivity.getClipLoaderManager().getAdapter().getSelectedPos();
            mUndoItem = new UndoItem(holder.clipItem, selectedPos, holder.itemView.isSelected());
            deleteRow(holder);

            final Snackbar snack = Snackbar
                    .make(mActivity.findViewById(R.id.fab), R.string.deleted_1_item, Snackbar.LENGTH_LONG)
                    .setAction("UNDO", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mUndoItem != null) {
                                mUndoItem.undo();
                                mUndoItem = null;
                            }
                        }
                    });

            snack.show();
        }
    }

    /**
     * Delegate to the  ItemTouchUiUtil class so we can separate the itemView into a foreground
     * and background view.
     * http://developer.android.com/reference/android/support/v7/widget/helper/ItemTouchUIUtil.html
     */

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        if (viewHolder != null) {
            final View foregroundView = ((ClipCursorAdapter.ClipViewHolder) viewHolder).clipForeground;

            getDefaultUIUtil().onSelected(foregroundView);
        }
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        final View backgroundView = ((ClipCursorAdapter.ClipViewHolder) viewHolder).clipBackground;
        final View foregroundView = ((ClipCursorAdapter.ClipViewHolder) viewHolder).clipForeground;

        // TODO: should animate out instead. how?
        backgroundView.setRight(0);

        getDefaultUIUtil().clearView(foregroundView);
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView,
                            RecyclerView.ViewHolder viewHolder, float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {
        final View foregroundView = ((ClipCursorAdapter.ClipViewHolder) viewHolder).clipForeground;

        drawBackground(viewHolder, dX, actionState);

        getDefaultUIUtil().onDraw(c, recyclerView, foregroundView, dX, dY,
                actionState, isCurrentlyActive);
    }

    @Override
    public void onChildDrawOver(Canvas c, RecyclerView recyclerView,
                                RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                int actionState, boolean isCurrentlyActive) {
        final View foregroundView = ((ClipCursorAdapter.ClipViewHolder) viewHolder).clipForeground;

        drawBackground(viewHolder, dX, actionState);

        getDefaultUIUtil().onDrawOver(c, recyclerView, foregroundView, dX, dY,
                actionState, isCurrentlyActive);
    }

    private void deleteRow(ClipCursorAdapter.ClipViewHolder holder) {
        new DeleteAsyncTask().executeMe(holder.itemID);
    }

    private class UndoItem {
        final int mPos;
        private final ClipItem mClipItem;
        private final boolean mIsSelected;

        private UndoItem(ClipItem clipItem, int pos, boolean isSelected) {
            mClipItem = clipItem;
            mPos = pos;
            mIsSelected = isSelected;
        }

        private void undo() {
            final ContentValues cv = mClipItem.getContentValues();
            mActivity.getContentResolver().insert(ClipContract.Clip.CONTENT_URI, cv);

            if (mIsSelected) {
                // little hack to make sure item is selected if it was when deleted
                mActivity.getClipLoaderManager().getAdapter().setSelectedItemID(-1L);
                mActivity.getClipLoaderManager().getAdapter().setSelectedPos(mPos);
            }
        }
    }

    // inner class to handle deletes asynchronously
    private class DeleteAsyncTask extends ThreadedAsyncTask<Object, Void, Void> {

        @SuppressWarnings("OverloadedVarargsMethod")
        @Override
        protected Void doInBackground(Object... params) {
            final long id = (long) params[0];
            final String selection = ClipContract.Clip._ID + "=" + id;
            mActivity.getContentResolver().delete(ClipContract.Clip.CONTENT_URI, selection, null);

            return null;
        }
    }
}
