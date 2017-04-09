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

package com.weebly.opus1269.clipman.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompatDividers;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.model.User;
import com.weebly.opus1269.clipman.msg.MessagingClient;
import com.weebly.opus1269.clipman.msg.RegistrationClient;
import com.weebly.opus1269.clipman.services.ClipboardWatcherService;
import com.weebly.opus1269.clipman.ui.helpers.NotificationHelper;
import com.weebly.opus1269.clipman.ui.main.MainActivity;

/**
 * Fragment for app Preferences.
 * Supports Material design through {@link PreferenceFragmentCompatDividers}
 */
public class SettingsFragment extends PreferenceFragmentCompatDividers
    implements SharedPreferences.OnSharedPreferenceChangeListener  {

    private static final int REQUEST_CODE_ALERT_RINGTONE = 5;

    private String mRingtoneKey;
    private String mNicknameKey;

    ///////////////////////////////////////////////////////////////////////////
    // Superclass overrides
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreatePreferencesFix(Bundle bundle, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey);

        // listen for preference changes
        PreferenceManager
            .getDefaultSharedPreferences(getContext())
            .registerOnSharedPreferenceChangeListener(this);

        mRingtoneKey = getActivity().getResources().getString(R.string.key_pref_ringtone);
        mNicknameKey = getActivity().getResources().getString(R.string.key_pref_nickname);

        setRingtoneSummary();
        setNicknameSummary();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // stop listening for preference changes
        PreferenceManager.getDefaultSharedPreferences(getContext())
            .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        try {
            return super.onCreateView(inflater, container, savedInstanceState);
        } finally {
            setDividerPreferences(
                DIVIDER_PADDING_CHILD |
                DIVIDER_CATEGORY_AFTER_LAST |
                DIVIDER_CATEGORY_BETWEEN);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {

        if (preference.getKey().equals(mRingtoneKey)) {
            // support library doesn't implement RingtonePreference.
            // need to do it ourselves
            // see: https://code.google.com/p/android/issues/detail?id=183255
            final Intent intent =
                new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(
                RingtoneManager.EXTRA_RINGTONE_TYPE,
                RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                    Settings.System.DEFAULT_NOTIFICATION_URI);

            final String existingValue = Prefs.getRingtone();
            if (existingValue != null) {
                if (existingValue.isEmpty()) {
                    // Select "Silent"
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                        (Uri) null);
                } else {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                            Uri.parse(existingValue));
                }
            } else {
                // No ringtone has been selected, set to the default
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                        Settings.System.DEFAULT_NOTIFICATION_URI);
            }

            startActivityForResult(intent, REQUEST_CODE_ALERT_RINGTONE);

            return true;
        }

        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == REQUEST_CODE_ALERT_RINGTONE) && (data != null)) {
            final Uri ringtone =
                data.getParcelableExtra(
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (ringtone != null) {
                Prefs.setRingtone(ringtone.toString());
            } else {
                // "Silent" was selected
                Prefs.setRingtone("");
            }
            setRingtoneSummary();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implement: SharedPreferences.OnSharedPreferenceChangeListener
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void
    onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        final String keyNickname =
            getResources().getString(R.string.key_pref_nickname);
        final String keyMonitor =
            getResources().getString(R.string.key_pref_monitor_clipboard);
        final String keyTheme =
            getResources().getString(R.string.key_pref_theme);
        final String keyNotifications =
            getResources().getString(R.string.key_pref_notifications);
        final String keyReceive =
            getResources().getString(R.string.key_pref_receive_msg);
        final Activity activity = getActivity();

        if (key.equals(keyNickname)) {
            setNicknameSummary();
            MessagingClient.sendPing();
        } else if (key.equals(keyMonitor)) {
            // start or stop clipboard service as needed
            if (Prefs.isMonitorClipboard()) {
                ClipboardWatcherService.startService(false);
            } else {
                final Intent intent =
                    new Intent(activity, ClipboardWatcherService.class);
                activity.stopService(intent);
            }
        } else if (key.equals(keyTheme)) {
            // recreate the stack so the theme is updated everywhere
            // http://stackoverflow.com/a/28799124/4468645
            TaskStackBuilder.create(activity)
                .addNextIntent(new Intent(activity, MainActivity.class))
                .addNextIntent(activity.getIntent())
                .startActivities();
        } else if (key.equals(keyNotifications)) {
            if (Prefs.notNotifications()) {
                // remove any currently displayed Notifications
                NotificationHelper.removeAll();
            }
        }  else if (key.equals(keyReceive)) {
            if (User.INSTANCE.isLoggedIn()) {
                if (Prefs.isAllowReceive()) {
                    // register
                    new RegistrationClient
                        .RegisterAsyncTask(getActivity(), null)
                        .executeMe();
                } else {
                    // unregister
                    new RegistrationClient
                        .UnregisterAsyncTask(getActivity())
                        .executeMe();
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Update the Ringtone summary text
     */
    private void setRingtoneSummary() {
        final Preference preference = findPreference(mRingtoneKey);
        final String value = Prefs.getRingtone();
        final String title;
        if (TextUtils.isEmpty(value)) {
            title = "Silent";
        } else {
            final Uri uri = Uri.parse(value);
            final Ringtone ringtone =
                RingtoneManager.getRingtone(getActivity(), uri);
            title = ringtone.getTitle(getActivity());
        }
        preference.setSummary(title);
    }

    /**
     * Update the Nickname summary text
     */
    private void setNicknameSummary() {
        final Preference preference = findPreference(mNicknameKey);
        String value = Prefs.getDeviceNickname();
        if (TextUtils.isEmpty(value)) {
            value = getResources().getString(R.string.pref_nickname_hint);
        }
        preference.setSummary(value);
    }
}
