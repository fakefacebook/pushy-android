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

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Static getters and setters for preferences
 */
public class Prefs {

    private static final String DEF_THEME =
            App.getContext().getString(R.string.ar_theme_light_value);
    private static final String DEF_DURATION =
            App.getContext().getString(R.string.ar_duration_forever_value);
    private static final String DEF_RINGTONE =
            Settings.System.DEFAULT_NOTIFICATION_URI.toString();

    // app notification types
    private static final String NOTIFY_REMOTE_COPY =
            App.getContext().getString(R.string.ar_not_remote_value);
    private static final String NOTIFY_LOCAL_COPY =
            App.getContext().getString(R.string.ar_not_local_value);
    private static final String NOTIFY_DEVICE_ADDED =
            App.getContext().getString(R.string.ar_not_dev_added_value);
    private static final String NOTIFY_DEVICE_REMOVED =
            App.getContext().getString(R.string.ar_not_dev_removed_value);

    private static final String[] DEF_NOTIFY_VALUES =
            App.getContext().getResources().getStringArray(
                R.array.pref_not_types_default_values);
    private static final Set<String> DEF_NOTIFICATIONS =
            new HashSet<>(Arrays.asList(DEF_NOTIFY_VALUES));

    // Preferences that are not set through the SettingsActivity
    private static final String PREF_VERSION_NAME = "prefVersionName";
    private static final String PREF_VERSION_CODE = "prefVersionCode";
    private static final String PREF_FAV_FILTER = "prefFavFilter";
    private static final String PREF_SORT_TYPE = "prefSortType";
    private static final String PREF_DEVICE_REGISTERED = "prefDeviceRegistered";
    private static final String PREF_DEVICES = "prefDevices";

    private Prefs() {
    }

    ///////////////////////////////////////////////////////////////////////////
    // Set from UI
    ///////////////////////////////////////////////////////////////////////////

    public static boolean isMonitorClipboard() {
        final Context context = App.getContext();
        final String key = context.getResources().getString(R.string.key_pref_monitor_clipboard);
        return get(key, true);
    }

    public static boolean isMonitorStartup() {
        final Context context = App.getContext();
        final String key = context.getResources().getString(R.string.key_pref_monitor_startup);
        return get(key, true);
    }

    public static boolean isPushClipboard() {
        final Context context = App.getContext();
        final String key = context.getResources().getString(R.string.key_pref_push_clipboard);
        return get(key, true);
    }

    public static boolean isAutoSend() {
        final Context context = App.getContext();
        final String key = context.getResources().getString(R.string.key_pref_auto_clipboard);
        return get(key, true);
    }

    public static boolean isAllowReceive() {
        final Context context = App.getContext();
        final String key = context.getResources().getString(R.string.key_pref_receive_clipboard);
        return get(key, true);
    }

    public static String getDeviceNickname() {
        final Context context = App.getContext();
        final String key = context.getResources().getString(R.string.key_pref_nickname);
        return get(key, "");
    }

    static String getDuration() {
        final Context context = App.getContext();
        final String key = context.getResources().getString(R.string.key_pref_duration);
        return get(key, DEF_DURATION);
    }

    private static String getTheme() {
        final Context context = App.getContext();
        final String key = context.getResources().getString(R.string.key_pref_theme);
        return get(key, DEF_THEME);
    }

    public static boolean isDarkTheme() {
        final Context context = App.getContext();
        return context.getString(R.string.ar_theme_dark_value).equals(getTheme());
    }

    public static boolean isLightTheme() {
        final Context context = App.getContext();
        return context.getString(R.string.ar_theme_light_value).equals(getTheme());
    }

    public static boolean notNotifications() {
        final Context context = App.getContext();
        final String key = context.getResources().getString(R.string.key_pref_notifications);
        return !get(key, true);
    }

    public static boolean isNotifyLocal() {
        return isNotifyEnabled(NOTIFY_LOCAL_COPY);
    }

    public static boolean isNotifyRemote() {
        return isNotifyEnabled(NOTIFY_REMOTE_COPY);
    }

    public static boolean isNotifyDeviceAdded() {
        return isNotifyEnabled(NOTIFY_DEVICE_ADDED);
    }

    public static boolean isNotifyDeviceRemoved() {
        return isNotifyEnabled(NOTIFY_DEVICE_REMOVED);
    }

    private static boolean isNotifyEnabled(String value) {
        if (notNotifications()) {
            return false;
        }
        final Context context = App.getContext();
        final String key = context.getResources().getString(R.string.key_pref_not_types);
        final SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        final Set<String> values =
            preferences.getStringSet(key, DEF_NOTIFICATIONS);
        return values.contains(value);
    }

    public static boolean isAudibleOnce() {
        final Context context = App.getContext();
        final String key = context.getResources().getString(R.string.key_pref_not_audible_once);
        return get(key, true);
    }

    @Nullable
    public static Uri getNotificationSound() {
        Uri ret = null;
        final String value = getRingtone();
         if (!TextUtils.isEmpty(value)) {
            ret = Uri.parse(value);
        }
        return ret;
    }

    public static String getRingtone() {
        final Context context = App.getContext();
        final String key = context.getResources().getString(R.string.key_pref_ringtone);
        return get(key, DEF_RINGTONE);
    }

    public static void setRingtone(String value) {
        final Context context = App.getContext();
        final String key = context.getResources().getString(R.string.key_pref_ringtone);
        set(key, value);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Not set from Settings UI
    ///////////////////////////////////////////////////////////////////////////

    public static void setVersionName(String value) {
        set(PREF_VERSION_NAME, value);
    }

    @SuppressWarnings("unused")
    public static String getVersionName() {
        return get(PREF_VERSION_NAME, "");
    }

    public static void setVersionCode(int value) {
        set(PREF_VERSION_CODE, value);
    }

    @SuppressWarnings("unused")
    public static int getVersionCode() {
        return get(PREF_VERSION_CODE, 0);
    }

    public static void setSortType(int value) {
        set(PREF_SORT_TYPE, value);
    }

    public static int getSortType() {
        return get(PREF_SORT_TYPE, 0);
    }

    public static void setFavFilter(Boolean value) {
        set(PREF_FAV_FILTER, value);
    }

    public static boolean isFavFilter() {
        return get(PREF_FAV_FILTER, false);
    }


    public static void setDeviceRegistered(Boolean value) {
        set(PREF_DEVICE_REGISTERED, value);
    }

    public static boolean isDeviceRegistered() {
        return get(PREF_DEVICE_REGISTERED, false);
    }

    public static void setDevices(String value) {
        set(PREF_DEVICES, value);
    }

    public static String getDevices() {
        return get(PREF_DEVICES, "");
    }

    static void set(String key, String value) {
        final SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(App.getContext());
        preferences.edit()
                .putString(key, value)
                .apply();
    }

    @SuppressWarnings("SameParameterValue")
    private static void set(String key, boolean value) {
        final SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(App.getContext());
        preferences.edit()
                .putBoolean(key, value)
                .apply();
    }

    private static void set(String key, int value) {
        final SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(App.getContext());
        preferences.edit()
                .putInt(key, value)
                .apply();
    }

    static String get(String key, String defValue) {
        final SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(App.getContext());
        return preferences.getString(key, defValue);
    }

    private static boolean get(String key, boolean defValue) {
        final SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(App.getContext());
        return preferences.getBoolean(key, defValue);
    }

    @SuppressWarnings("SameParameterValue")
    private static int get(String key, int defValue) {
        final SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(App.getContext());
        return preferences.getInt(key, defValue);
    }
}

