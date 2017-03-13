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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.weebly.opus1269.clipman.model.ClipContentProvider;
import com.weebly.opus1269.clipman.model.Prefs;

/**
 * This {@link BroadcastReceiver} starts the {@link ClipboardWatcherService} at boot time
 * and adds a daily alarm to clean-up the database
 *
 * Requires android.permission.RECEIVE_BOOT_COMPLETED
 */
public class AutoStartReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //noinspection CallToStringEquals
        if (!"android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
          return;
        }

        setupClipboardWatcher();

        setupDeleteOldAlarm(context);
     }

    /**
     * Start service to monitor Clipboard for changes
     *
     */
    private static void setupClipboardWatcher() {
        if (Prefs.isMonitorStartup()) {
            ClipboardWatcherService.startService(true);
        }
    }

    /**
     * Add daily alarm to cleanup database of old entries
     *
     * @param context a Context
     */
    private static void setupDeleteOldAlarm(Context context) {
        final AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        final Intent intent = new Intent(context, AlarmReceiver.class);
        final PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        // setup daily alarm
        alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, AlarmManager.INTERVAL_DAY,
                AlarmManager.INTERVAL_DAY, alarmIntent);

        // run now
        ClipContentProvider.deleteOldItems();
    }
}