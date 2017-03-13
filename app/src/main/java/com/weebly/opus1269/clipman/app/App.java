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

package com.weebly.opus1269.clipman.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.weebly.opus1269.clipman.logs.LogDBHelper;
import com.weebly.opus1269.clipman.model.ClipDatabaseHelper;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.ui.devices.DevicesActivity;
import com.weebly.opus1269.clipman.ui.main.MainActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extend the App class so we can get a {@link Context} anywhere
 */
public class App extends Application implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = "App";

    // OK, because App won't go away until killed
    @SuppressLint("StaticFieldLeak")
    private static Context sContext = null;
    @SuppressLint("StaticFieldLeak")
    private static ClipDatabaseHelper sClipDb = null;
    @SuppressLint("StaticFieldLeak")
    private static LogDBHelper sLogDb = null;

    private static boolean sIsMainActivityVisible = false;
    private static boolean sIsDevicesActivityVisible = false;

    /**
     * Maps between an activity class name and the list of currently running
     * AsyncTasks that were spawned while it was active.
     */
    private final Map<String, List<CustomAsyncTask<?,?,?>>> mActivityTaskMap;

    public App() {
        mActivityTaskMap = new HashMap<>();
    }

    public static Context getContext() {
        return sContext;
    }

    public static ClipDatabaseHelper getDbHelper() {
        return sClipDb;
    }

   public static LogDBHelper getLogDbHelper() {
        return sLogDb;
    }

    public static boolean isMainActivityVisible() {
        return sIsMainActivityVisible;
    }

    public static boolean isDevicesActivityVisible() {
        return sIsDevicesActivityVisible;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sContext = this;

        sClipDb = new ClipDatabaseHelper(sContext);
        sClipDb.getWritableDatabase();

        sLogDb = new LogDBHelper(sContext);
        sLogDb.getWritableDatabase();

        // save version info. to the preferences database
        try {
            final PackageInfo pInfo =
                    getPackageManager().getPackageInfo(getPackageName(), 0);
            Prefs.setVersionName(pInfo.versionName);
            Prefs.setVersionCode(pInfo.versionCode);
        } catch (final PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Version info not found: " + e.getMessage());
        }

        // Register to be notified of activity state changes
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivityResumed(Activity activity) {
        if (activity instanceof MainActivity) {
            sIsMainActivityVisible = true;
        } else if (activity instanceof DevicesActivity) {
            sIsDevicesActivityVisible = true;
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (activity instanceof MainActivity) {
            sIsMainActivityVisible = false;
        } else if (activity instanceof DevicesActivity) {
            sIsDevicesActivityVisible = false;
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}

    /**
     * These methods are part of a solution to the problem of
     * screen orientation/Activity destruction during lengthy Async tasks.
     * https://fattybeagle.com/2011/02/15/android-asynctasks-during-a-screen-rotation-part-ii/
     */

    public void removeTask(CustomAsyncTask<?,?,?> task) {
        for (Map.Entry<String, List<CustomAsyncTask<?,?,?>>> entry : mActivityTaskMap.entrySet()) {
            List<CustomAsyncTask<?,?,?>> tasks = entry.getValue();
            for (int i = 0; i < tasks.size(); i++) {
                if (tasks.get(i) == task) {
                    tasks.remove(i);
                    break;
                }
            }

            if (tasks.size() == 0) {
                mActivityTaskMap.remove(entry.getKey());
                return;
            }
        }
    }

    public void addTask(Activity activity, CustomAsyncTask<?,?,?> task) {
        String key = activity.getClass().getCanonicalName();
        List<CustomAsyncTask<?,?,?>> tasks = mActivityTaskMap.get(key);
        if (tasks == null) {
            tasks = new ArrayList<>();
            mActivityTaskMap.put(key, tasks);
        }

        tasks.add(task);
    }

    public void detach(Activity activity) {
        List<CustomAsyncTask<?,?,?>> tasks = mActivityTaskMap.get(activity.getClass().getCanonicalName());
        if (tasks != null) {
            for (CustomAsyncTask<?,?,?> task : tasks) {
                task.setActivity(null);
            }
        }
    }

    public void attach(Activity activity) {
        List<CustomAsyncTask<?,?,?>> tasks = mActivityTaskMap.get(activity.getClass().getCanonicalName());
        if (tasks != null) {
            for (CustomAsyncTask<?,?,?> task : tasks) {
                task.setActivity(activity);
            }
        }
    }
}