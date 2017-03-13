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

package com.weebly.opus1269.clipman.ui.helpers;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Device;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.msg.Msg;
import com.weebly.opus1269.clipman.ui.devices.DevicesActivity;
import com.weebly.opus1269.clipman.ui.main.MainActivity;

/**
 * Static class to manage our {@link android.app.Notification} objects
 */
public class NotificationHelper {

    private static final int ID_COPY = 10;
    private static final int ID_DEVICE = 20;

    // keep track of number of clipboard changes received.
    private static int sCount;

    private NotificationHelper() {
    }

    ///////////////////////////////////////////////////////////////////////////
    // Public methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Display notification on a clipboard change
     *
     * @param clipItem the {@link ClipItem} to display notification for
     */
    public static void show(ClipItem clipItem) {
        if ((clipItem == null) ||
                TextUtils.isEmpty(clipItem.getText()) ||
                App.isMainActivityVisible() ||
                (clipItem.isRemote() && !Prefs.isNotifyRemote()) ||
                (!clipItem.isRemote() && !Prefs.isNotifyLocal())) {
            return;
        }

        final String clipText = clipItem.getText();
        final int id = ID_COPY;
        final Context context = App.getContext();
        PendingIntent pendingIntent;

        final Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(ClipItem.INTENT_EXTRA_CLIP_ITEM, clipItem);

        final TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(intent);
        // Gets a PendingIntent containing the entire back stack
        pendingIntent =
                stackBuilder.getPendingIntent(12345, PendingIntent.FLAG_UPDATE_CURRENT);

        // remote vs. local settings
        final int largeIcon;
        final String titleText;
        if (clipItem.isRemote()) {
            largeIcon = R.drawable.lic_remote_copy;
            titleText = context.getString(R.string.clip_notification_remote_fmt, clipItem.getDevice());
        } else {
            largeIcon = R.drawable.lic_local_copy;
            titleText = context.getString(R.string.clip_notification_local);
        }

        final NotificationCompat.Builder builder =
                getBuilder(pendingIntent, largeIcon, titleText);
        builder.setContentText(clipText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(clipText))
                .setWhen(clipItem.getTime());

        sCount++;
        if(sCount > 1) {
            builder.setSubText(context.getString(R.string.clip_notification_count_fmt, sCount));
        }

        // notification deleted (cleared, swiped, etc) action
        // does not get called on tap if autocancel is true
        pendingIntent =
                NotificationReceiver
                        .getPendingIntent(AppUtils.DELETE_NOTIFICATION_ACTION, id, null);
        builder.setDeleteIntent(pendingIntent);

        // Web Search action
        pendingIntent =
                NotificationReceiver
                        .getPendingIntent(AppUtils.SEARCH_ACTION, id, clipItem);
        builder.addAction(R.drawable.ic_search, context.getString(R.string.action_search), pendingIntent);

        // Share action
        pendingIntent =
                NotificationReceiver
                        .getPendingIntent(AppUtils.SHARE_ACTION, id, clipItem);
         builder.addAction(R.drawable.ic_share, context.getString(R.string.action_share) + " ...", pendingIntent);

        final NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, builder.build());
    }

    /**
     * Display notification on remote device added or removed
     *
     * @param action Added or removed
     * @param device remote device
     */
    public static void show(String action, CharSequence device) {
        if (TextUtils.isEmpty(action) || TextUtils.isEmpty(device) ||
                App.isDevicesActivityVisible() ||
                (action.equals(Msg.ACTION_DEVICE_ADDED) && !Prefs.isNotifyDeviceAdded()) ||
                (action.equals(Msg.ACTION_DEVICE_REMOVED) && !Prefs.isNotifyDeviceRemoved())) {
            return;
        }

        final Context context = App.getContext();
        final Intent intent = new Intent(context, DevicesActivity.class);

        // stupid android: http://stackoverflow.com/a/36110709/4468645
        final TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack i.e. MainActivity.class
        stackBuilder.addParentStack(DevicesActivity.class);
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(intent);
        // Gets a PendingIntent containing the entire back stack
        final PendingIntent pendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // added vs. removed device settings
        final int largeIcon;
        final String titleText;
        if (action.equals(Msg.ACTION_DEVICE_ADDED)) {
            largeIcon = R.drawable.lic_add_device;
            titleText = context.getString(R.string.device_added);
        } else {
            largeIcon = R.drawable.lic_remove_device;
            titleText = context.getString(R.string.device_removed);
        }

        final NotificationCompat.Builder builder =
                getBuilder(pendingIntent, largeIcon, titleText);
        builder.setContentText(device)
                .setWhen(System.currentTimeMillis());

         final NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(ID_DEVICE, builder.build());
    }

    /**
     * Remove our {@link ClipItem} notifications
     */
    public static void removeClips() {
        final Context context = App.getContext();
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(ID_COPY);
        resetCount();
    }

    /**
     * Remove our {@link Device} notifications
     */
    public static void removeDevices() {
        final Context context = App.getContext();
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(ID_DEVICE);
    }

    /**
     * Remove all our Notifications
     */
    public static void removeAll() {
        removeClips();
        removeDevices();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Reset count
     */
    private static void resetCount() {
        sCount = 0;
    }

    @SuppressWarnings("SameReturnValue")
    private static int getSmallIcon() {
        return R.drawable.ic_notification;
//        boolean useWhiteIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
//        return useWhiteIcon ? R.drawable.ic_notification : R.mipmap.ic_launcher;
    }

    private static Bitmap getLargeIcon(int id) {
        return BitmapFactory.decodeResource(App.getContext().getResources(), id);
    }

    private static NotificationCompat.Builder
    getBuilder(PendingIntent pInt, int largeIcon, String titleText) {
        final Context context = App.getContext();
        final NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context);

        builder.setContentIntent(pInt)
                .setLargeIcon(getLargeIcon(largeIcon))
                .setSmallIcon(getSmallIcon())
                .setContentTitle(titleText)
                .setTicker(titleText)
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setShowWhen(true)
                .setOnlyAlertOnce(Prefs.isAudibleOnce())
                .setAutoCancel(true);

        final Uri sound = Prefs.getNotificationSound();
        if (sound != null) {
            builder.setSound(sound);
        }

        return builder;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Inner classes
    ///////////////////////////////////////////////////////////////////////////

    /**
     * {@link BroadcastReceiver} to handle notification actions
     */
    public static class NotificationReceiver extends BroadcastReceiver {
        public static final String TAG = "NotificationReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final ClipItem item;
            final int notID = intent.getIntExtra(AppUtils.INTENT_EXTRA_NOTIFICATION_ID, -1);

            if (AppUtils.DELETE_NOTIFICATION_ACTION.equals(action)) {
                resetCount();
            } else if(AppUtils.SEARCH_ACTION.equals(action)) {
                item = (ClipItem) intent.getSerializableExtra(ClipItem.INTENT_EXTRA_CLIP_ITEM);
                // search the web for the clip text
                AppUtils.performWebSearch(item.getText());

                cancelNotification(notID);
                // collapse notifications
                context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            } else if(AppUtils.SHARE_ACTION.equals(action)){
                item = (ClipItem) intent.getSerializableExtra(ClipItem.INTENT_EXTRA_CLIP_ITEM);
                // share the clip text with other apps
                item.doShare(null);

                cancelNotification(notID);
                // collapse notifications
                context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
        }

        /**
         * Get a pending intent for this receiver
         * @param action An action we know about
         * @param notificationId The id of the source notification
         * @param clipItem The id {@link ClipItem}
         * @return a {@link PendingIntent}
         *
         */
        public static PendingIntent getPendingIntent(String action, int notificationId, ClipItem clipItem) {
            final Context context = App.getContext();
            final Intent intent = new Intent(context, NotificationReceiver.class);
            intent.setAction(action);
            intent.putExtra(AppUtils.INTENT_EXTRA_NOTIFICATION_ID, notificationId);
            intent.putExtra(ClipItem.INTENT_EXTRA_CLIP_ITEM, clipItem);
            return PendingIntent.getBroadcast(context, 12345, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        private static void cancelNotification(int notificationId) {
            if (notificationId != -1) {
                // cancel notification
                final NotificationManager notificationManager =
                        (NotificationManager) App.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(notificationId);
                resetCount();
            }
        }
    }

}
