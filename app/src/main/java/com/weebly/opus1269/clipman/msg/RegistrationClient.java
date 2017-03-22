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

import android.text.TextUtils;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.firebase.iid.FirebaseInstanceId;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.backend.registration.Registration;
import com.weebly.opus1269.clipman.backend.registration.model.EndpointRet;
import com.weebly.opus1269.clipman.model.Device;
import com.weebly.opus1269.clipman.model.Prefs;

import java.io.IOException;

/**
 * This helper class is the interface to our gae Registration Endpoint
 */
public class RegistrationClient extends Endpoint {
    private static final String TAG = "RegistrationClient";

    private RegistrationClient() {
    }

    /**
     * Register {@link Device} with server
     * @param idToken - authorization token
     * @return getSuccess() false on error
     */
    public static EndpointRet register(String idToken) {
        EndpointRet ret = new EndpointRet();
        ret.setSuccess(false);
        ret.setReason(Msg.ERROR_UNKNOWN);

        if (Prefs.isDeviceRegistered()) {
            Log.logD(TAG, "Already registered.");
            ret.setSuccess(true);
            return ret;
        }

        final String regToken = getRegToken();
        if (TextUtils.isEmpty(regToken)) {
            ret.setReason(Log.logE(TAG, Msg.ERROR_INVALID_REGID));
            return ret;
        }

        boolean isRegistered = false;
        try {
            final GoogleCredential credential = getCredential(idToken);
            if (credential == null) {
                ret.setReason(Log.logE(TAG, Msg.ERROR_CREDENTIAL));
                return ret;
            }

            // call server
            final Registration regService = getRegistrationService(credential);
            ret = regService.register(regToken).execute();
            if (ret.getSuccess()) {
                isRegistered = true;
            } else {
                ret.setReason(Log.logE(TAG,
                    Msg.ERROR_REGISTER + " " + ret.getReason()));
            }
        } catch (final IOException e) {
            ret.setReason(Log.logEx(TAG, Msg.ERROR_REGISTER, e));
        } finally {
            saveValues(isRegistered, regToken);
        }

        return ret;
    }

    /**
     * Unregister {@link Device} with server
     * @return getSuccess() false on error
     */
    public static EndpointRet unregister() {
        EndpointRet ret = new EndpointRet();
        ret.setSuccess(false);
        ret.setReason(Msg.ERROR_UNKNOWN);

        if (!Prefs.isDeviceRegistered()) {
            Log.logD(TAG, Msg.ERROR_NOT_REGISTERED);
            ret.setSuccess(true);
            return ret;
        }

        try {
            // we will sign out regardless, so reset
            // TODO is this right?
            saveValues(false, "");

            final String regToken = getRegToken();
            if (TextUtils.isEmpty(regToken)) {
                ret.setReason(Log.logE(TAG, Msg.ERROR_INVALID_REGID));
                return ret;
            }

            final GoogleCredential credential = getCredential(null);
            if (credential == null) {
                ret.setReason(Log.logE(TAG, Msg.ERROR_CREDENTIAL));
                return ret;
            }

            // call server
            final Registration regService = getRegistrationService(credential);
            ret = regService.unregister(regToken).execute();
            if (!ret.getSuccess()) {
                ret.setReason(Log.logE(TAG, Msg.ERROR_UNREGISTER +
                    " " + ret.getReason()));
            }
        } catch (final IOException e) {
            ret.setReason(Log.logEx(TAG, Msg.ERROR_UNREGISTER, e));
        }

        return ret;
    }

    /**
     * Update registration on server when RegToken changes
     * @param newRegToken refreshed {@link FirebaseInstanceId}
     * @return getSuccess() false on error
     */
    public static EndpointRet refresh(String newRegToken) {
        EndpointRet ret = new EndpointRet();
        ret.setSuccess(false);
        ret.setReason(Msg.ERROR_UNKNOWN);

        if (!Prefs.isDeviceRegistered()) {
            Log.logD(TAG, Msg.ERROR_NOT_REGISTERED);
            ret.setSuccess(true);
            return ret;
        }

        final String oldRegToken = Prefs.getRegToken();

        try {
            if (newRegToken.equals(oldRegToken)) {
                ret.setSuccess(true);
                return ret;
            }

            final GoogleCredential credential = getCredential(null);
            if (credential == null) {
                ret.setReason(Log.logE(TAG, Msg.ERROR_CREDENTIAL));
                return ret;
            }

            // call server
            final Registration regService = getRegistrationService(credential);
            ret = regService.refresh(newRegToken, oldRegToken).execute();

            // update prefs
            Prefs.setDeviceRegistered(ret.getSuccess());
            Prefs.setRegToken(newRegToken);
            if (ret.getSuccess()) {
                Log.logD(TAG, "Token refreshed\nnew: " +
                    newRegToken + "\nold: " + oldRegToken);
            } else {
                ret.setReason(
                    Log.logE(TAG, Msg.ERROR_REFRESH + " " + ret.getReason()));
            }
        } catch (final IOException e) {
            ret.setReason(Log.logEx(TAG, Msg.ERROR_REFRESH, e));
            return ret;
        }

        return ret;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Get an authorized connection to the RegistrationEndpoint
     * @param credential - authorization for current user
     * @return Connection to RegistrationEndpoint on server
     */
    private static Registration
    getRegistrationService(GoogleCredential credential) {
        final Registration.Builder builder =
            new Registration.Builder(new NetHttpTransport(),
                new AndroidJsonFactory(), credential);

        builder.setApplicationName(AppUtils.getApplicationName());

        // for development purposes
        setLocalServer(builder);

        return builder.build();
    }

    /**
     * Store registration state
     * @param registered - true if registered with server
     * @param regToken   - {@link FirebaseInstanceId} we registered with
     */
    private static void saveValues(boolean registered, String regToken) {
        Prefs.setDeviceRegistered(registered);
        Prefs.setRegToken(regToken);
    }

}