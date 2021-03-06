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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.weebly.opus1269.clipman.app.App;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Static Class to manage the collection of registered {@link Device} objects.
 * Register a {@link LocalBroadcastManager} with
 * Devices.INTENT_FILTER to be notified of changes.
 */
public class Devices {

    /** @value */
    public static final String INTENT_FILTER = "devicesIntent";
    /** @value */
    public static final String BUNDLE = "bundleDevices";
    /** @value */
    public static final String ACTION = "actionDevices";
    /** @value */
    public static final String ACTION_UPDATE = "updateDevices";
    /** @value */
    public static final String ACTION_MY_DEVICE_REMOVED = "myDeviceRemoved";
    /** @value */
    public static final String ACTION_MY_DEVICE_UNREGISTERED =
        "myDeviceUnregistered";

    @SuppressWarnings("StaticNonFinalField")
    private static List<Device> sDevices = load();

    private Devices() {}

    /**
     * Save list to persistant storage
     * @param broadcast broadcast result to listeners if true
     */
    private static void save(Boolean broadcast) {
        final Comparator<Device> cmp = new Comparator<Device>() {
            @Override
            public int compare(Device lhs, Device rhs) {
                // newest first
                return ((Long) rhs.getLastSeen().getMillis())
                    .compareTo(lhs.getLastSeen().getMillis());
            }
        };
        // sort by lastSeen
        Collections.sort(sDevices, cmp);

        // persist
        final Gson gson = new Gson();
        final String devicesString = gson.toJson(sDevices);
        Prefs.setDevices(devicesString);

        if (broadcast) {
            // let listeners know
            _sendBroadcast(ACTION_UPDATE);
        }
    }

    /**
     * Load list from persistant storage
     * @return List<Device> the list of {@link Device} objects
     */
    private static List<Device> load() {
        final String devicesString = Prefs.getDevices();

        if (devicesString.isEmpty()) {
            sDevices = new ArrayList<>(0);
        } else {
            final Gson gson = new Gson();
            final Type type = new TypeToken<ArrayList<Device>>() {
            }.getType();
            sDevices = gson.fromJson(devicesString, type);
        }
        return sDevices;
    }

    /**
     * Add or update {@link Device} as needed. Broadcast change if requested.
     * @param dev The {@link Device} to add or update
     * @param broadcast broadcast result to listeners if true
     */
    public static void add(Device dev, Boolean broadcast) {
        if (dev != null) {
            int i = 0;
            for (final Device device : sDevices) {
                if (dev.getUniqueName().equals(device.getUniqueName())) {
                    // found, nickname or lastSeen probably changed,
                    // update device
                    sDevices.set(i, dev);
                    save(broadcast);
                    return;
                }
                i++;
            }
            sDevices.add(dev);
            save(broadcast);
        }
    }

    /**
     * Remove the given {@link Device}
     * @param dev The {@link Device} to remove
     */
    public static void remove(Device dev) {
        if (dev != null) {
            for (final Iterator<Device> i = sDevices.iterator(); i.hasNext();) {
                final Device device = i.next();
                if (dev.getUniqueName().equals(device.getUniqueName())) {
                    i.remove();
                    save(true);
                    break;
                }
            }
        }
    }

    /**
     * Remove all devices
     */
    static void clear() {
        sDevices.clear();
        save(true);
    }

    /**
     * Notify listeners that our {@link Device} was removed
     */
    public static void notifyMyDeviceRemoved() {
        clear();
        _sendBroadcast(ACTION_MY_DEVICE_REMOVED);
    }

    /**
     * Notify listeners that our {@link Device} was unregistered
     */
    public static void notifyMyDeviceUnregistered() {
        clear();
        _sendBroadcast(ACTION_MY_DEVICE_UNREGISTERED);
    }

    /**
     * Get the {@link Device} at the given position
     * @param pos Position in list
     * @return A {@link Device}
     */
    public static Device get(int pos) {
        return sDevices.get(pos);
    }

    /**
     * Get the number of {@link Device} objects in the list
     * @return the number of {@link Device} objects in the list
     */
    public static int getCount() {
        return sDevices.size();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Broadcast changes to listeners
     * @param action the type of the change
     */
    private static void _sendBroadcast(String action) {
        final Intent intent = new Intent(INTENT_FILTER);
        final Bundle bundle = new Bundle();
        bundle.putString(ACTION, action);
        intent.putExtra(BUNDLE, bundle);
        LocalBroadcastManager
            .getInstance(App.getContext())
            .sendBroadcast(intent);
    }
}
