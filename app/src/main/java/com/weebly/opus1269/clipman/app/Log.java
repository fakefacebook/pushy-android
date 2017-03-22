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

import android.text.TextUtils;

import com.weebly.opus1269.clipman.BuildConfig;

/**
 * Message logger Class
 */
public class Log {
    /** @value */
    private static final String MY_APP = "MyApp ";

    /**
     * Log a debug message
     * @param tag     Class we are from
     * @param message message to log
     */
    public static void logD(String tag, String message) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d(MY_APP + tag, message);
        }
    }

    /**
     * Log an error message
     * @param tag     Class we are from
     * @param message message to log
     * @return The message
     */
    public static String logE(String tag, String message) {
        android.util.Log.e(MY_APP + tag, message);
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
        String msg = "";
        if (!TextUtils.isEmpty(message)) {
            msg = message + ": ";
        }
        msg+= e.getLocalizedMessage();
        android.util.Log.e(MY_APP + tag, msg);
        android.util.Log.e(MY_APP + tag, e.toString());
        return msg;
    }
}
