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

import android.net.Uri;
import android.provider.BaseColumns;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;

/**
 * The contract between the {@link ClipContentProvider} and applications.
 */
public class ClipContract {

    // The authority for the clip provider
    static final String AUTHORITY = AppUtils.PACKAGE_NAME;

    // A content:// style uri to the authority for the clip provider
    private static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    private ClipContract() {
    }

    static String getDefaultSortOrder() {
        final String[] sorts =
                App.getContext().getResources().getStringArray(R.array.sort_type_values);
        return sorts[0];
    }

    public static String getSortOrder() {
        final String[] sorts =
                App.getContext().getResources().getStringArray(R.array.sort_type_values);
        return sorts[Prefs.getSortType()];
    }

    /* Inner class that defines the Clip table */
    @SuppressWarnings({"PublicInnerClass", "StaticInheritance", "SuperClassHasFrequentlyUsedInheritors"})
    public static class Clip implements BaseColumns {
        public static final Uri CONTENT_URI = Uri.parse(AUTHORITY_URI + "/clip");
        static final String TABLE_NAME = "clip";
        public static final String COL_TEXT = "text";
        static final String COL_DATE = "date";
        public static final String COL_FAV = "fav";
        static final String COL_REMOTE = "remote";
        static final String COL_DEVICE = "device";

        public static final String[] FULL_PROJECTION = {
                ClipContract.Clip._ID,
                ClipContract.Clip.COL_TEXT,
                ClipContract.Clip.COL_DATE,
                ClipContract.Clip.COL_FAV,
                ClipContract.Clip.COL_REMOTE,
                ClipContract.Clip.COL_DEVICE
        };

    }
}
