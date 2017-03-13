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

import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.androidessence.recyclerviewcursoradapter.RecyclerViewCursorAdapter;
import com.androidessence.recyclerviewcursoradapter.RecyclerViewCursorViewHolder;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.ClipContract;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.ui.helpers.DrawableHelper;

/**
 * Bridge between the main RecyclerView and the Clips.db database
 */
class ClipCursorAdapter extends RecyclerViewCursorAdapter<ClipCursorAdapter.ClipViewHolder> {

    // The currently selected position in the list
    private int mSelectedPos = 0;

    // The database _ID of the selection list item
    private long mSelectedItemID = -1L;

    private final MainActivity mActivity;

    ClipCursorAdapter(MainActivity activity) {
        super(activity);

        mActivity = activity;

        // needed to allow animations to run
        setHasStableIds(true);

        setupCursorAdapter(null, 0, R.layout.clip_row, false);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Superclass overrides
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Returns the ViewHolder to use for this adapter.
     */
    @Override
    public ClipViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = mCursorAdapter.newView(mContext, mCursorAdapter.getCursor(), parent);
        final ClipViewHolder holder = new ClipViewHolder(view);

        holder.itemView.setOnClickListener(mActivity.getClipLoaderManager());
        holder.favCheckBox.setOnClickListener(mActivity.getClipLoaderManager());
        holder.copyButton.setOnClickListener(mActivity.getClipLoaderManager());

        return holder;
    }

    /**
     * Moves the Cursor of the CursorAdapter to the appropriate position and binds the view for
     * that item.
     */
    @Override
    public void onBindViewHolder(ClipViewHolder holder, int position) {
        // Move cursor to this position
        mCursorAdapter.getCursor().moveToPosition(position);

        // Set the ViewHolder
        setViewHolder(holder);

        // Bind this view
        mCursorAdapter.bindView(null, mContext, mCursorAdapter.getCursor());

        // color the icons
        tintIcons(holder);

        if (AppUtils.isDualPane()) {
            // set selected state of the view
            if (getSelectedPos() == position) {
                if (!holder.itemView.isSelected()) {
                    holder.itemView.setSelected(true);
                }
            } else {
                holder.itemView.setSelected(false);
            }
        }
    }

    @Override
    // needed to allow animations to run
    public long getItemId(int position) {
        return mCursorAdapter.getItemId(position);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Package private methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * We are a delegate for the savedInstanceState of the {@link MainActivity}
     * Here we are restoring the selected state
     * @param pos selected position in list
     * @param id DB id of selected row
     */
    void restoreSelection(int pos, long id) {
        mSelectedPos = pos;
        mSelectedItemID = id;
    }

    int getPosFromItemID(long itemID) {
        int pos = -1;

        if (itemID == -1L) {
            return pos;
        }

        final Cursor cursor = mCursorAdapter.getCursor();
        if (cursor.moveToFirst()) {
            final int colIndex = cursor.getColumnIndex(ClipContract.Clip._ID);
            while (!cursor.isAfterLast()) {
                if (cursor.getLong(colIndex) == itemID) {
                    pos = cursor.getPosition();
                    break;
                }
                cursor.moveToNext();
            }
        }

        return pos;
    }

    int getSelectedPos() {
        return mSelectedPos;
    }

    void setSelectedPos(int position) {
        if (mSelectedPos == position) {
            return;
        }

        if (position < 0) {
            mSelectedPos = -1;
            mSelectedItemID = -1L;
        } else {
            notifyItemChanged(mSelectedPos);
            mSelectedPos = position;
            notifyItemChanged(mSelectedPos);
        }
    }

    long getSelectedItemID() {
        return mSelectedItemID;
    }

    void setSelectedItemID(long itemID) {
        mSelectedItemID = itemID;
        setSelectedPos(getPosFromItemID(mSelectedItemID));
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Color the Vector Drawables based on theme and fav state
     *
     * @param holder ClipViewHolder
     */
    private void tintIcons(ClipViewHolder holder) {
        final int color;
        final int drawableFav;
        final int colorFav;

        if (Prefs.isLightTheme()) {
            color = android.R.color.primary_text_light;
        } else {
            color = android.R.color.primary_text_dark;
        }

        if (holder.favCheckBox.isChecked()) {
            drawableFav = R.drawable.ic_favorite_black_24dp;
            colorFav = R.color.red_500_translucent;
        } else {
            drawableFav = R.drawable.ic_favorite_border_black_24dp;
            colorFav = color;
        }

        DrawableHelper
                .withContext(mActivity)
                .withColor(color)
                .withDrawable(R.drawable.ic_content_copy_black_24dp)
                .tint()
                .applyTo(holder.copyButton);

        DrawableHelper
                .withContext(mActivity)
                .withColor(colorFav)
                .withDrawable(drawableFav)
                .tint()
                .applyToDrawableLeft(holder.favCheckBox);
    }

    ///////////////////////////////////////////////////////////////////////////
    // inner classes
    ///////////////////////////////////////////////////////////////////////////

    /**
     * ViewHolder inner class used to display the clip in the RecyclerView.
     */
    @SuppressWarnings({"InstanceVariableNamingConvention", "PublicField", "PublicInnerClass"})
    static class ClipViewHolder extends RecyclerViewCursorViewHolder {

        final RelativeLayout clipBackground;
        final RelativeLayout clipForeground;
        final CheckBox favCheckBox;
        final TextView dateText;
        final ImageButton copyButton;
        final TextView clipText;
        ClipItem clipItem;
        long itemID;

        ClipViewHolder(View view) {
            super(view);

            clipBackground = (RelativeLayout) view.findViewById(R.id.clipBackground);
            clipForeground = (RelativeLayout) view.findViewById(R.id.clipForeground);
            favCheckBox = (CheckBox) view.findViewById(R.id.favCheckBox);
            dateText = (TextView) view.findViewById(R.id.dateText);
            clipText = (TextView) view.findViewById(R.id.clipText);
            copyButton = (ImageButton) view.findViewById(R.id.copyButton);
            clipItem = null;
            itemID = -1L;

            itemView.setTag(this);
            favCheckBox.setTag(this);
            copyButton.setTag(this);
        }

        @Override
        public void bindCursor(final Cursor cursor) {
            clipItem = new ClipItem(cursor);

            itemID = cursor.getLong(cursor.getColumnIndex(ClipContract.Clip._ID));

            clipText.setText(clipItem.getText());
            favCheckBox.setChecked(clipItem.isFav());

            long time = clipItem.getTime();
            final CharSequence value = AppUtils.getRelativeDisplayTime(clipItem.getDate());
            dateText.setText(value);
            dateText.setTag(time);
        }
    }
}
