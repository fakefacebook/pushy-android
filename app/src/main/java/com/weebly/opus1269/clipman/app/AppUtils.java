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

import android.app.ActivityManager;
import android.app.SearchManager;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;

import com.weebly.opus1269.clipman.BuildConfig;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Devices;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.msg.MessagingClient;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * General static constants utility methods
 */
public class AppUtils {
    private static final String TAG = "AppUtils";

    public static final String PACKAGE_NAME = BuildConfig.APPLICATION_ID;
    public static final String PACKAGE_PATH = PACKAGE_NAME + '.';

    public static final String PLAY_STORE = "market://details?id=" + PACKAGE_NAME;
    public static final String PLAY_STORE_WEB =
            "https://play.google.com/store/apps/details?id=" + PACKAGE_NAME;

    public static final String EMAIL_ADDRESS = "clipmanapp@gmail.com";

    // Intent constants
    public static final String SEARCH_ACTION =
            PACKAGE_PATH + "SEARCH_ACTION";
    public static final String SHARE_ACTION =
            PACKAGE_PATH + "SHARE_ACTION";
    public static final String DELETE_NOTIFICATION_ACTION =
            PACKAGE_PATH + "DELETE_NOTIFICATION_ACTION";
    public static final String INTENT_EXTRA_NOTIFICATION_ID =
            PACKAGE_PATH + "NOTIFICATION_ID";

    private static final String MY_APP = "MyApp ";

    private AppUtils() {
    }

    /**
     * Log a debug message
     * @param tag Class we are from
     * @param message message to log
     */
    public static void logD(String tag, String message) {
        if (BuildConfig.DEBUG) {
            Log.d(MY_APP + tag, message);
        }
    }

    /**
     * Log an error message
     * @param tag     Class we are from
     * @param message message to log
     * @return The message
     */
    public static String logE(String tag, String message) {
        Log.e(MY_APP + tag, message);
        return message;
    }

    /**
     * Log an {@link Exception}
     * @param tag     Class we are from
     * @param message message to log
     * @param e       Exception to log
     * @return The message
     */
    public static String logEx(String tag, String message, Exception e) {
        final String msg = message + ": " + e.getLocalizedMessage();
        Log.e(MY_APP + tag, msg);
        Log.e(MY_APP + tag, e.toString());
        return msg;
    }

    /**
     * Convert device density to pixels
     * @param context A Context
     * @param dipValue Value to convert
     * @return Value in pixels
     */
    public static int dp2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        //noinspection NumericCastThatLosesPrecision,MagicNumber
        return (int) ((dipValue * scale) + 0.5F);
    }

    /**
     * Convert pixels to device density
     * @param context A Context
     * @param pxValue Value to convert
     * @return Value in device density
     */
    @SuppressWarnings("unused")
    public static int px2dp(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        //noinspection NumericCastThatLosesPrecision,MagicNumber
        return (int) ((pxValue / scale) + 0.5F);
    }

    /**
     * Get the app name
     * @return app name
     */
    public static String getApplicationName() {
        final Context context = App.getContext();
        final int stringId = context.getApplicationInfo().labelRes;
        return context.getString(stringId);
    }

    /**
     * Check if the MainActivity is in dual pane mode
     * @return boolean
     */
    public static boolean isDualPane() {
        return App.getContext().getResources().getBoolean(R.bool.dual_pane);
    }

    /**
     * Check if a service is running
     * @see <a href="https://goo.gl/55RFa6">Stack Overflow</a>
     * @param serviceClass Class name of Service
     * @return boolean
     */
    public static boolean isMyServiceRunning(Class<?> serviceClass) {
        final Context context = App.getContext();
        final ActivityManager manager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        boolean ret = false;
        for (final ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            //noinspection CallToStringEquals
            if (serviceClass.getName().equals(service.service.getClassName())) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    /**
     * Launch an {@link Intent} to show a {@link Uri}
     * @param uri A String that is a valid Web Url
     * @return true on success
     */
    public static Boolean showWebUrl(String uri) {
        Boolean ret = false;

        if (Patterns.WEB_URL.matcher(uri).matches()) {
            ret = true;
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse(uri));
            try {
                App.getContext().startActivity(intent);
            } catch (Exception e) {
                logEx(TAG, e.getMessage(), e);
                ret = false;
            }
        }
        return ret;
    }

    /**
     * Launch an {@link Intent} to search the web
     * @param text A String to search for
     * @return true on success
     */
    public static Boolean performWebSearch(String text) {
        boolean ret = true;
        final Context context = App.getContext();

        if (TextUtils.isEmpty(text)) {
            ret = false;
        } else {
            final Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
            intent.putExtra(SearchManager.QUERY, text);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                logEx(TAG, e.getMessage(), e);
                ret = false;
            }
        }
        return ret;
    }

    /**
     * Get a time string relative to now
     * @param date A {@link DateTime}
     * @return CharSequence time
     */
    public static CharSequence getRelativeDisplayTime(DateTime date) {
        final Context context = App.getContext();
        final CharSequence value;
        long now = System.currentTimeMillis();
        long time = date.getMillis();
        long delta = now - time;

        if (delta <= DateUtils.SECOND_IN_MILLIS) {
            DateTimeFormatter fmt =
                    DateTimeFormat.forPattern(context.getString(R.string.joda_time_fmt_pattern));
            value = context.getString(R.string.now_fmt,date.toString(fmt));
        } else {
            value =
                DateUtils.getRelativeDateTimeString(context, time,
                    DateUtils.SECOND_IN_MILLIS, DateUtils.DAY_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_ALL);
        }
         return value;
    }


    /**
     * Send the clipboard contents to our {@link Devices}
     */
    public static void sendClipboardContents(View view) {
        ClipboardManager clipboardManager = (ClipboardManager)
            App.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        final ClipItem clipItem = ClipItem.getFromClipboard(clipboardManager);
        if (clipItem != null) {
            if (Prefs.isPushClipboard()) {
                MessagingClient.send(clipItem);
                if (view != null) {
                    Snackbar.make(view,
                        R.string.clipboard_sent, Snackbar.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * Capitalize a {@link String}
     * @param s String to captialize
     * @return capitalized String
     */
    public static String capitalize(String s) {
        if (TextUtils.isEmpty(s)) {
            return "";
        }
        final char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }
}
