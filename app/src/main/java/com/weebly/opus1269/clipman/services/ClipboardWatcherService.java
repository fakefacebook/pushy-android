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

package com.weebly.opus1269.clipman.services;

import android.app.Service;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.ClipContentProvider;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.model.User;
import com.weebly.opus1269.clipman.msg.MessagingClient;
import com.weebly.opus1269.clipman.ui.helpers.NotificationHelper;

/**
 * An app private {@link Service} to listen for changes to the clipboard, persist them
 * to storage using {@link ClipContentProvider} and push them to registered fcm devices
 */
public class ClipboardWatcherService extends Service implements
        ClipboardManager.OnPrimaryClipChangedListener {
    private static final String TAG = "ClipboardWatcherService";

    private static final String EXTRA_ON_BOOT = "on_boot";

    // The fastest we will process identical local copies
    private static final long MIN_TIME_MILLIS = 200;

    // The last text we read
    private String mLastText;

    // The last time we read
    private long mLastTime;

    // ye olde ClipboardManager
    private ClipboardManager mClipboard = null;

    /**
     * Start ourselves
     *
     * @param onBoot true if called on device boot
     *
     */
    public static void startService(Boolean onBoot) {
        if (Prefs.isMonitorClipboard()
                && !AppUtils.isMyServiceRunning(ClipboardWatcherService.class)) {
            // only start if the user has allowed it and we are not running
            final Context context = App.getContext();
            final Intent intent = new Intent(context, ClipboardWatcherService.class);
            intent.putExtra(EXTRA_ON_BOOT, onBoot);
            context.startService(intent);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Superclass overrides
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate() {
        mClipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        mClipboard.addPrimaryClipChangedListener(this);
        mLastText = "";
        mLastTime = System.currentTimeMillis();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mLastText = "";
        mLastTime = System.currentTimeMillis();

        if (intent != null) {
            final boolean onBoot = intent.getBooleanExtra(EXTRA_ON_BOOT, false);
            if (!onBoot) {
                // don't process on boot
                processClipboard(true);
            }
        } else {
            processClipboard(true);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mClipboard.removePrimaryClipChangedListener(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Unimplemented onBind method in: " + TAG);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implement ClipboardManager.OnPrimaryClipChangedListener
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onPrimaryClipChanged() {
        processClipboard(false);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Read the clipboard, then write to database asynchronously.
     * DO NOT read clipboard in AsyncTask, you will regret it my boy.
     *
     * @param onNewOnly if true only update database if the current contents don't exit
     *
     */
    private void processClipboard(boolean onNewOnly) {
        final ClipItem clipItem =
                ClipItem.getFromClipboard(mClipboard);
        final long now = System.currentTimeMillis();
        final long deltaTime = now - mLastTime;
        mLastTime = now;

         if ((clipItem == null) || TextUtils.isEmpty(clipItem.getText())) {
            mLastText = "";
            return;
        }

        if (!clipItem.isRemote() && mLastText.equals(clipItem.getText())) {
            if (deltaTime > MIN_TIME_MILLIS) {
                // only handle identical local copies this fast
                // some apps (at least Chrome) write to clipboard twice.
                new StoreClipAsyncTask(clipItem).execute(onNewOnly);
            }
        } else {
            // normal situation, fire away
            new StoreClipAsyncTask(clipItem).execute(onNewOnly);
        }
        mLastText = clipItem.getText();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Inner classes
    ///////////////////////////////////////////////////////////////////////////

    /**
     * AsyncTask to write to the Clip database
     *
     */
    private class StoreClipAsyncTask extends AsyncTask<Boolean, Void, Void> {
        final ClipItem mClipItem;
        boolean mResult;

        StoreClipAsyncTask(ClipItem clipItem) {
            mClipItem = clipItem;
        }

        @Override
        protected Void doInBackground(Boolean... params) {
            final Boolean onNewOnly = params[0];
            mResult =
                    ClipContentProvider.insert(ClipboardWatcherService.this,
                        mClipItem, onNewOnly);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (mResult) {
                // display notification if requested by user
                NotificationHelper.show(mClipItem);

                if (!mClipItem.isRemote() && User.INSTANCE.isLoggedIn() &&
                    Prefs.isPushClipboard() && Prefs.isAutoSend()) {
                    // send local copy to server for delivery
                    MessagingClient.send(mClipItem);
                }
            }
        }
    }
}
