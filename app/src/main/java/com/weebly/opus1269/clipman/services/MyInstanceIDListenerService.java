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

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Trigger;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.weebly.opus1269.clipman.app.App;

/**
 * This {@link FirebaseInstanceIdService} listens for changes to the regToken
 */
public class MyInstanceIDListenerService extends FirebaseInstanceIdService {

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is also called
     * when the InstanceID token is initially generated, so this is where
     * you retrieve the token. Use JobService
     * @see <a href="https://goo.gl/wYr4o2">firebase-jobdispatcher-android</a>
     */
    @Override
    public void onTokenRefresh() {
        // Use Job Service
        FirebaseJobDispatcher dispatcher =
            new FirebaseJobDispatcher(new GooglePlayDriver(App.getContext()));

        Job myJob = dispatcher.newJobBuilder()
            .setService(RefreshTokenJobService.class)
            .setTrigger(Trigger.executionWindow(0, 0))
            .setRecurring(false)
            .setTag(RefreshTokenJobService.TAG)
            .build();

        dispatcher.mustSchedule(myJob);
    }
}
