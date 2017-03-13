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

package com.weebly.opus1269.clipman.logs;

import android.content.ContentValues;

import org.joda.time.DateTime;

/**
 * This class represents a log of a message received from the fcm server
 */

public class LogItemFcm {
    private final String mMethod;
    private final String mMsgID;
    private final DateTime mDate;
    private final String mExtra;

    public LogItemFcm(String method, String msgID, DateTime date, String extra) {
        mMethod = method;
        mMsgID = msgID;
        mDate = date;
        mExtra = extra;
    }

    public ContentValues getContentValues() {
         final ContentValues cv = new ContentValues();
        cv.put(LogDBHelper.COL_MSG_ID, mMsgID);
        cv.put(LogDBHelper.COL_DATE, mDate.getMillis());
        cv.put(LogDBHelper.COL_METHOD, mMethod);
        cv.put(LogDBHelper.COL_EXTRA, mExtra);

        return cv;
    }
}
