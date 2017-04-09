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

package com.weebly.opus1269.clipman.model;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;

/**
 * Singleton for Google Analytics tracking.
 * @see <a href="https://goo.gl/VUowF7">Android Analytics</a>
 */
public enum Analytics {
    INSTANCE;

    /**
     * Google Analytics tracking ID
     */
    private static final String TRACKING_ID = "UA-61314754-3";

    private static final String CAT_MSG = "message";
    private static final String CAT_REG = "register";
    private static final String SENT = "sent";
    private static final String RECEIVED = "received";
    private static final String REGISTERED = "registered";
    private static final String UNREGISTERED = "unregistered";
    private static final String NO_SCREEN = "none";

    /**
     * Google Analytics tracker
     */
    private Tracker mTracker;

    /**
     * Get a {@link Tracker}
     * @return tracker
     */
    synchronized public Tracker getTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics =
                GoogleAnalytics.getInstance(App.getContext());
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            mTracker = analytics.newTracker(TRACKING_ID);
            mTracker.setAppName(AppUtils.getApplicationName());
            mTracker.setAppVersion(Prefs.getVersionName());
        }
        return mTracker;
    }

    /**
     * Message sent event
     */
    public void sent() {
        getTracker().setScreenName(NO_SCREEN);
        getTracker().send(new HitBuilders.EventBuilder()
            .setCategory(CAT_MSG)
            .setAction(SENT)
            .build());
    }

    /**
     * Message received event
     */
    public void received() {
        getTracker().setScreenName(NO_SCREEN);
        getTracker().send(new HitBuilders.EventBuilder()
            .setCategory(CAT_MSG)
            .setAction(RECEIVED)
            .build());
    }

    /**
     * Device registered event.
     */
    public void registered() {
        getTracker().setScreenName(NO_SCREEN);
        getTracker().send(new HitBuilders.EventBuilder()
            .setCategory(CAT_REG)
            .setAction(REGISTERED)
            .build());
    }

    /**
     * Device unregistered event.
     */
    public void unregistered() {
        getTracker().setScreenName(NO_SCREEN);
        getTracker().send(new HitBuilders.EventBuilder()
            .setCategory(CAT_REG)
            .setAction(UNREGISTERED)
            .build());
    }
}
