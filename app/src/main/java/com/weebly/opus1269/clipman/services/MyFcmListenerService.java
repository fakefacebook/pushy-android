/*
 *
 * Copyright 2016 Michael A Updike
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

import android.net.Uri;
import android.os.SystemClock;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Device;
import com.weebly.opus1269.clipman.model.Devices;
import com.weebly.opus1269.clipman.msg.MessagingClient;
import com.weebly.opus1269.clipman.msg.Msg;
import com.weebly.opus1269.clipman.ui.helpers.NotificationHelper;

import java.util.Map;

/**
 * A service that listens for messages from firebase
 */
public class MyFcmListenerService extends FirebaseMessagingService {
    private static final String TAG = "MyFcmListenerService";

    /**
     * Called when message is received from one of our devices.
     *
     * @param message message sent from fcm.
     */
    @Override
    public void onMessageReceived(RemoteMessage message) {
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options

        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ

        Map<String, String> data = message.getData();

        final String action = data.get(Msg.ACTION);

        // decode message text
        final String msg = data.get(Msg.MESSAGE);
        if (msg != null) {
            data.put(Msg.MESSAGE, Uri.decode(msg));
        }

        final String deviceModel = data.get(Msg.DEVICE_MODEL);
        final String deviceSN = data.get(Msg.DEVICE_SN);
        final String deviceOS = data.get(Msg.DEVICE_OS);
        final String deviceNickname = data.get(Msg.DEVICE_NICKNAME);
        final Device device = new Device(deviceModel, deviceSN, deviceOS, deviceNickname);

        if (device.getUniqueName().equals(Device.getMyUniqueName())) {
            // ignore our own messages
            return;
        }

         switch (action) {
            case Msg.ACTION_MESSAGE:
                AppUtils.logD(TAG, "ACTION_MESSAGE");
                // normal message, copy to clipboard
                Devices.add(device, false);
                copyToClipboard(data, device);
                break;
            case Msg.ACTION_PING:
                // We were pinged
                AppUtils.logD(TAG, "ACTION_PING");
                MessagingClient.sendPingResponse();
                break;
            case Msg.ACTION_PING_RESPONSE:
                // Response to a ping
                Devices.add(device, true);
                AppUtils.logD(TAG, "ACTION_PING_RESPONSE");
                AppUtils.logD(TAG, device.getDisplayName() + " told me he is around.");
                break;
            case Msg.ACTION_DEVICE_ADDED:
                // A new device was added
                AppUtils.logD(TAG, "ACTION_DEVICE_ADDED");
                Devices.add(device, true);
                NotificationHelper.show(action, device.getDisplayName());
                break;
            case Msg.ACTION_DEVICE_REMOVED:
                // A device was removed
                AppUtils.logD(TAG, "ACTION_DEVICE_REMOVED");
                Devices.remove(device);
                NotificationHelper.show(action, device.getDisplayName());
                break;
            default:
                AppUtils.logE(TAG, "Unknown message type received.");
                break;
        }

        // slow down the message stream
        SystemClock.sleep(250);
    }

    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();

        AppUtils.logD(TAG, "onDeletedMessages called");
    }

    @Override
    public void onMessageSent(String msgId) {
        super.onMessageSent(msgId);

        AppUtils.logD(TAG, "onMessageSent called: " + msgId);
    }

    @Override
    public void onSendError(String msgId, Exception ex) {
        super.onSendError(msgId, ex);

        AppUtils.logEx(TAG, "Error from Fcm ", ex);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods
    ///////////////////////////////////////////////////////////////////////////

    private static void copyToClipboard(Map<String, String> data, Device device) {
        final String message = data.get(Msg.MESSAGE);
        final String favString = data.get(Msg.FAV);
        final Boolean fav = "1".equals(favString);

        // add device
        Devices.add(device, false);

        // add to clipboard
        final ClipItem clipItem = new ClipItem(message);
        clipItem.setRemote(true);
        clipItem.setDevice(device.getDisplayName());
        clipItem.setFav(fav);
        clipItem.copyToClipboard();
    }

}
