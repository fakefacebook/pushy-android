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

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.view.View;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;

import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;

import java.io.Serializable;

/**
 * This class represents the data for a single clipboard entry
 */
public class ClipItem implements Serializable {

    private static final String DESC_LABEL = "opus1269 was here";
    private static final String REMOTE_DESC_LABEL = "From Remote Copy";
    public static final String TEXT_PLAIN = "text/plain";

    private String mText;
    private DateTime mDate;
    private Boolean mFav;
    // Is this a copy from another device
    private Boolean mRemote;
    private String mDevice;

    public ClipItem() {
        init();
    }

    public ClipItem(String text) {
        init();
        mText = text;
    }

    public ClipItem(String text, ReadableInstant date, Boolean fav,
                    Boolean remote, String device) {
        mText = text;
        mDate = new DateTime(date.getMillis());
        mFav = fav;
        mRemote = remote;
        mDevice = device;
    }

    public ClipItem(Cursor cursor) {
        init();
        int idx = cursor.getColumnIndex(ClipContract.Clip.COL_TEXT);
        mText = cursor.getString(idx);
        idx = cursor.getColumnIndex(ClipContract.Clip.COL_DATE);
        mDate = new DateTime(cursor.getLong(idx));
        idx = cursor.getColumnIndex(ClipContract.Clip.COL_FAV);
        final long fav = cursor.getLong(idx);
        mFav = fav != 0L;
        idx = cursor.getColumnIndex(ClipContract.Clip.COL_REMOTE);
        final long remote = cursor.getLong(idx);
        mRemote = remote != 0L;
        idx = cursor.getColumnIndex(ClipContract.Clip.COL_DEVICE);
        mDevice = cursor.getString(idx);
    }

    public ClipItem(ClipItem clipItem) {
        init();
        mText = clipItem.getText();
        mDate = new DateTime(clipItem.getDate().getMillis());
        mFav = clipItem.isFav();
        mRemote = clipItem.isRemote();
        mDevice = clipItem.getDevice();
    }

    /**
     * Get the text on the Clipboard as a ClipItem
     * @param clipboard The {@link ClipboardManager}
     * @return Clipboard content as ClipItem
     */
    @Nullable
    public static ClipItem getFromClipboard(ClipboardManager clipboard) {
        final ClipData clipData = clipboard.getPrimaryClip();
        if (clipData == null) {
            return null;
        }

        final ClipData.Item item = clipData.getItemAt(0);

        CharSequence clipText = item.getText();
        if (clipText == null) {
            // If the Uri contains something, just coerce it to text
            if (item.getUri() != null) {
                clipText = item.coerceToText(App.getContext());
            }
        }

        // parse the description for special instructions
        Boolean remote = false;
        String sourceDevice = Device.getMyName();
        final ClipDescription desc = clipData.getDescription();
        // set fav state if the copy is from us
        final Boolean fav = parseFav(desc);
        // set remote state if this is a remote copy
        final String parse = parseRemote(desc);
        if (!parse.isEmpty()) {
            remote = true;
            sourceDevice = parse;
        }

        ClipItem clipItem = null;
        if ((clipText != null) && (TextUtils.getTrimmedLength(clipText) > 0)) {
            clipItem = new ClipItem(String.valueOf(clipText));
            clipItem.setFav(fav);
            clipItem.setRemote(remote);
            clipItem.setDevice(sourceDevice);
        }

        return clipItem;
    }

    /**
     * Parse the fav state from the {@link ClipDescription}
     * @param desc The item's {@link ClipDescription}
     * @return The fav state
     */
    private static boolean parseFav(ClipDescription desc) {
        boolean fav = false;

        String label = (String) desc.getLabel();
        if (!TextUtils.isEmpty(label) && label.contains(DESC_LABEL)) {
            final int index = label.indexOf('[');
            if (index != -1) {
                label = label.substring(index + 1, index + 2);
                fav = Integer.parseInt(label) != 0;
            }
        }
        return fav;
    }

    /**
     * Parse the {@link ClipDescription} to see if it is from one of our
     * remote devices
     * @param desc The item's {@link ClipDescription}
     * @return The remote device name or "" if a local copy
     */
    private static String parseRemote(ClipDescription desc) {
        String device = "";

        final String label = (String) desc.getLabel();
        if (!TextUtils.isEmpty(label) && label.contains(REMOTE_DESC_LABEL)) {
            final int idxStart = label.indexOf('(');
            final int idxStop = label.indexOf(')');
            device = label.substring(idxStart + 1, idxStop);
        }
        return device;
    }


    public String getText() {
        return mText;
    }

    public void setText(String text) {
        mText = text;
    }

    public DateTime getDate() {
        return new DateTime(mDate.getMillis());
    }

    @SuppressWarnings("unused")
    public void setDate(ReadableInstant date) {
        mDate = new DateTime(date.getMillis());
    }

    void setDate(long date) {
        mDate = new DateTime(date);
    }

    public long getTime() {
        return mDate.getMillis();
    }

    public boolean isFav() {
        return mFav;
    }

    public void setFav(Boolean fav) {
        mFav = fav;
    }

    public boolean isRemote() {
        return mRemote;
    }

    public void setRemote(Boolean remote) {
        mRemote = remote;
    }

    public String getDevice() {
        return mDevice;
    }

    public void setDevice(String device) {
        mDevice = device;
    }

    /**
     * Initialize the members
     */
    private void init() {
        mText = "";
        mDate = new DateTime();
        mFav = false;
        mRemote = false;
        mDevice = Device.getMyName();
    }

    /**
     * Get the ClipItem as a {@link ContentValues object}
     * @return ClipItem as {@link ContentValues object}
     */
    public ContentValues getContentValues() {
        final long fav = mFav ? 1L : 0L;
        final long remote = mRemote ? 1L : 0L;
        final ContentValues cv = new ContentValues();
        cv.put(ClipContract.Clip.COL_TEXT, mText);
        cv.put(ClipContract.Clip.COL_DATE, mDate.getMillis());
        cv.put(ClipContract.Clip.COL_FAV, fav);
        cv.put(ClipContract.Clip.COL_REMOTE, remote);
        cv.put(ClipContract.Clip.COL_DEVICE, mDevice);

        return cv;
    }

    /**
     * Copy the ClipItem to the clipboard
     */
    public void copyToClipboard() {
        // Make sure we have looper
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {

            @Override
            public void run() {
                final ClipboardManager clipboard = (ClipboardManager) App
                    .getContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE);
                final long fav = mFav ? 1L : 0L;

                // add a label with the fav value so we can maintain the state
                CharSequence label =
                    DESC_LABEL + "[" + Long.toString(fav) + "]\n";
                if (mRemote) {
                    // add label indicating this is from a remote device
                    label = label + REMOTE_DESC_LABEL + "(" + mDevice + ")";
                }

                final ClipData clip = ClipData.newPlainText(label, mText);
                clipboard.setPrimaryClip(clip);
            }
        });
    }

    /**
     * Share the ClipItem with other apps
     * @param view The {@link View} that is requesting the share
     */
    public void doShare(View view) {
        if (TextUtils.isEmpty(mText)) {
            if (view != null) {
                Snackbar
                    .make(view, R.string.no_share, Snackbar.LENGTH_SHORT)
                    .show();
            }
            return;
        }

        Context context;
        if (view != null) {
            context = view.getContext();
        } else {
            context = App.getContext();
        }

        final long fav = mFav ? 1L : 0L;
        // add a label with the fav value so our watcher can maintain the state
        CharSequence label = DESC_LABEL;
        label = label + Long.toString(fav);

        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, mText);
        intent.putExtra(Intent.EXTRA_TITLE, label);
        intent.setType(TEXT_PLAIN);
        final Intent sendIntent = Intent.createChooser(intent,
            context.getResources().getString(R.string.share_text_to));
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (sendIntent.resolveActivity(context.getPackageManager()) != null) {
            // Verify that the intent will resolve to an activity
            context.startActivity(sendIntent);
        }
    }
}