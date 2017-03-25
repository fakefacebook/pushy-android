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

package com.weebly.opus1269.clipman.msg;

import android.net.Uri;
import android.os.AsyncTask;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.backend.messaging.Messaging;
import com.weebly.opus1269.clipman.backend.messaging.model.EndpointRet;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Device;
import com.weebly.opus1269.clipman.model.Devices;
import com.weebly.opus1269.clipman.model.Prefs;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * This helper class is the interface to our gae Messaging endpoint
 */
public class MessagingClient extends Endpoint{
    private static final String TAG = "MessagingClient";

    /** {@value} */
    private static final String ERROR_SEND =
        App.getContext().getString(R.string.err_send);

    private MessagingClient() {
    }

    /**
     * Send contents of {@link ClipItem}
     * @param clipItem - contents to send
     */
    public static void send(ClipItem clipItem) {
        if (notSignedIn() || !Prefs.isPushClipboard()) {
            return;
        }

        String message = clipItem.getText();
        if (message.length() > Msg.MAX_MSG_LEN) {
            // 4KB limit with FCM - server will do final limiting
            message = message.substring(0, Msg.MAX_MSG_LEN - 1);
        }
        final String favString = clipItem.isFav() ? "1" : "0";

        JSONObject data = getJSONData(Msg.ACTION_MESSAGE, message);
        try {
            data.put(Msg.FAV, favString);
        } catch (JSONException ex) {
            Log.logEx(TAG, "", ex);
            data = null;
        }

        if (data != null) {
            new MessagingAsyncTask().execute(data);
        }
    }

    /** Notify of our addition */
    static void sendDeviceAdded() {
        if (notSignedIn()) {
            return;
        }

        JSONObject data =
            getJSONData(Msg.ACTION_DEVICE_ADDED, Msg.MSG_DEVICE_ADDED);
        if (data != null) {
            new MessagingAsyncTask().execute(data);
        }
    }

    /** Notify of our removal */
    public static void sendDeviceRemoved() {
        if (notSignedIn()) {
            return;
        }

        JSONObject data =
            getJSONData(Msg.ACTION_DEVICE_REMOVED, Msg.MSG_DEVICE_REMOVED);
        if (data != null) {
            new MessagingAsyncTask().execute(data);
        }
    }

    /** Ping others */
    public static void sendPing() {
        if (notSignedIn()) {
            return;
        }

        JSONObject data = getJSONData(Msg.ACTION_PING, Msg.MSG_PING);
        if (data != null) {
            new MessagingAsyncTask().execute(data);
        }
    }

    /** Respond to ping */
    public static void sendPingResponse() {
        if (notSignedIn()) {
            return;
        }

        JSONObject data =
            getJSONData(Msg.ACTION_PING_RESPONSE, Msg.MSG_PING_RESPONSE);
        if (data != null) {
            new MessagingAsyncTask().execute(data);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Get an authorized connection to the MessagingEndpoint
     * @param credential - authorization for current user
     * @return Connection to MessagingEndpoint on server
     */
    private static Messaging getMessagingService(GoogleCredential credential) {
        final Messaging.Builder builder =
            new Messaging.Builder(new NetHttpTransport(),
                new AndroidJsonFactory(), credential);

        builder.setApplicationName(AppUtils.getApplicationName());

        // for development purposes
        setLocalServer(builder);

        return builder.build();
    }

    /**
     * Build the message data object
     * @param action the message action
     * @param message the message text
     * @return a JSON data object
     */
    private static JSONObject getJSONData(String action, String message) {
        JSONObject data;
        try {
            data = new JSONObject();
            data.put(Msg.ACTION, action);
            data.put(Msg.MESSAGE, Uri.encode(message));
            data.put(Msg.DEVICE_MODEL, Device.getMyDevice().getModel());
            data.put(Msg.DEVICE_SN, Device.getMyDevice().getSN());
            data.put(Msg.DEVICE_OS, Device.getMyDevice().getOS());
            data.put(Msg.DEVICE_NICKNAME, Device.getMyDevice().getNickname());
        } catch(JSONException ex) {
            Log.logEx(TAG, "", ex);
            data = null;
        }
        return data;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Inner classes
    ///////////////////////////////////////////////////////////////////////////

    /**
     * AsyncTask to call gae Messaging Endpoint
     */
    private static class MessagingAsyncTask
        extends AsyncTask<JSONObject, Void, EndpointRet> {
        String mAction;

        MessagingAsyncTask() {}

        @Override
        protected EndpointRet doInBackground(JSONObject... params) {
            EndpointRet ret = new EndpointRet();
            ret.setSuccess(false);
            ret.setReason(Msg.ERROR_UNKNOWN);

            final JSONObject data = params[0];

            try {
                mAction = data.getString(Msg.ACTION);

                final String jsonString = data.toString();

                final GoogleCredential credential = getCredential(null);
                if (credential == null) {
                    ret.setReason(Log.logE(TAG, Msg.ERROR_CREDENTIAL));
                    return ret;
                }

                final Messaging msgService = getMessagingService(credential);

                // call server
                final String regToken = getRegToken();
                final Boolean highPriority = Prefs.isHighPriority();
                ret = msgService.send(regToken, jsonString, highPriority)
                    .execute();
                if (ret.getSuccess()) {
                    Log.logD(TAG, "Message sent to server: " + mAction);
                } else {
                    ret.setReason(
                        Log.logE(TAG, ERROR_SEND + ": " + ret.getReason()));
                }
            } catch (IOException|JSONException ex) {
                ret.setReason(Log.logEx(TAG, ERROR_SEND, ex));
            }
            return ret;
        }

        @Override
        protected void onPostExecute(EndpointRet ret) {
            if (Msg.ACTION_DEVICE_REMOVED.equals(mAction)) {
                // remove device notification
                // SignInActivity will be notified that it can now sign-out
                Devices.notifyMyDeviceUnregistered();
            }
        }
    }
}
