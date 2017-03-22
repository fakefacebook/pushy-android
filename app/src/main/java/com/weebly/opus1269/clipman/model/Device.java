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

package com.weebly.opus1269.clipman.model;

import android.os.Build;
import android.text.TextUtils;

import com.weebly.opus1269.clipman.app.AppUtils;

import org.joda.time.DateTime;
import org.json.JSONException;

/**
 * Immutable Class that represents a (hopefully) unique device
 * Emulators don't have good names and serial numbers
 *
 */
public class Device {
    private static final String TAG = "Device";

    // Only use primitives because we go to Preferences as a list of devices

    // These three should be unique (not for emulators)
    private final String mModel;
    private final String mSN;
    private final String mOS;

    private final String mNickname;
    private final long mLastSeen;

    public Device(String model, String sn, String os, String nickname) {
        mModel = model;
        mSN = sn;
        mOS = os;
        mNickname = nickname;
        mLastSeen = new DateTime().getMillis();
    }

    public static Device getMyDevice() {
        return new Device(getMyModel(), getMySN(), getMyOS(), Prefs.getDeviceNickname());
    }

    public static String getMyName() {
        String myName = getMyNickname();
        if (TextUtils.isEmpty(myName)) {
            myName = getMyModel() + " - " + getMySN() + " - " + getMyOS();
        }
        return myName;
    }

    public static String getMyModel() {
        String value;
        final String manufacturer = Build.MANUFACTURER;
        final String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            value = AppUtils.capitalize(model);
        } else {
            value = AppUtils.capitalize(manufacturer) + " " + model;
        }
        if (value.startsWith("Htc ")) {
            // special case for HTC
            value = value.replaceFirst("Htc ", "HTC ");
        }
        return value;
    }

    // TODO look into SERIAL
    private static String getMySN() {
        return Build.SERIAL;
    }

    private static String getMyOS() {
        return "Android";
    }

    private static String getMyNickname() {
        return Prefs.getDeviceNickname();
    }

    public static String getMyUniqueName() {
        return getMyModel() + " - " + getMySN() + " - " + getMyOS();
    }

    public String getModel() {
        return mModel;
    }

    public String getSN() {
        return mSN;
    }

    public String getOS() {
        return mOS;
    }

    public String getNickname() {
        return mNickname;
    }

    public DateTime getLastSeen() {
        return new DateTime(mLastSeen);
    }

    public String getDisplayName() {
        String name = getNickname();
        if (TextUtils.isEmpty(name)) {
            name = getModel() + " - " + getSN() + " - " + getOS();
        }
        return name;
    }

    public String getUniqueName() {
        return getModel() + " - " + getSN() + " - " + getOS();
    }
}
