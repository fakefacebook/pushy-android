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

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.backend.registration.model.EndpointRet;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.msg.RegistrationClient;

/**
 * This {@link FirebaseInstanceIdService} listens for changes to the regToken
 */
public class MyInstanceIDListenerService extends FirebaseInstanceIdService {
    private static final String TAG = "MyInstanceIDLS";

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is also called
     * when the InstanceID token is initially generated, so this is where
     * you retrieve the token.
     */
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        AppUtils.logD(TAG, "Refreshed token: " + refreshedToken);

        if (Prefs.isDeviceRegistered()) {
            final EndpointRet ret = RegistrationClient.refresh(refreshedToken);
            if (!ret.getSuccess()) {
                Log.e(TAG, ret.getReason());
            }
        } else {
            // first time save it. still not registered though
            Prefs.setRegToken(refreshedToken);
        }
    }
}
