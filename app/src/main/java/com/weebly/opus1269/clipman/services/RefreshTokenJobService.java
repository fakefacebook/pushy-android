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

import android.os.Handler;
import android.os.Looper;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.backend.registration.model.EndpointRet;
import com.weebly.opus1269.clipman.model.User;
import com.weebly.opus1269.clipman.msg.RegistrationClient;

/**
 * JobService to handle {@link FirebaseInstanceIdService} refreshToken()
 */
public class RefreshTokenJobService extends JobService {
    public static final String TAG = "RefreshTokenJobService";

    @Override
    public boolean onStartJob(JobParameters job) {
        Boolean ret = false;
        if (User.INSTANCE.isLoggedIn()) {
            ret = true;
            // Make sure we have looper
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {

                @Override
                public void run() {
                    // Get updated InstanceID token.
                    String refreshedToken =
                        FirebaseInstanceId.getInstance().getToken();
                    Log.logD(TAG, "Refreshed token: " + refreshedToken);
                    final EndpointRet ret =
                        RegistrationClient.register(refreshedToken);
                    if (!ret.getSuccess()) {
                        Log.logE(TAG, ret.getReason());
                    }
                }
            });
        }

        return ret; // Answers the question: "Is there still work going on?"
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return false; // Answers the question: "Should this job be retried?"
    }
}
