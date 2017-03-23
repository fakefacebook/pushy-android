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
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
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

    private static final String ERROR_REGISTER =
        App.getContext().getString(R.string.err_register);
    private static final String ERROR_UNREGISTER =
        App.getContext().getString(R.string.err_unregister);
    private static final String ERROR_INVALID_REGID =
        App.getContext().getString(R.string.err_invalid_regid);

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
            ret.setReason(Log.logE(TAG, ERROR_INVALID_REGID));
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
                    ERROR_REGISTER + " " + ret.getReason()));
            }
        } catch (final IOException ex) {
            ret.setReason(Log.logEx(TAG, ERROR_REGISTER, ex));
        } finally {
            saveValues(isRegistered);
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
            saveValues(false);

            final String regToken = getRegToken();
            if (TextUtils.isEmpty(regToken)) {
                ret.setReason(Log.logE(TAG, ERROR_INVALID_REGID));
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
                ret.setReason(Log.logE(TAG, ERROR_UNREGISTER +
                    " " + ret.getReason()));
            }
        } catch (final IOException ex) {
            ret.setReason(Log.logEx(TAG, ERROR_UNREGISTER, ex));
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
     */
    private static void saveValues(boolean registered) {
        Prefs.setDeviceRegistered(registered);
    }
}