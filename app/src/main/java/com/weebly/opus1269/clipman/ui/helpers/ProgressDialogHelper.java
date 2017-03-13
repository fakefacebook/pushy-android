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

package com.weebly.opus1269.clipman.ui.helpers;

import android.app.ProgressDialog;
import android.content.Context;

/**
 * Helper class for managing a model progress dialog
 */
public class ProgressDialogHelper {

    private static ProgressDialog mDialog;

    private ProgressDialogHelper() {
    }

    private static void create(Context context) {
        mDialog = new ProgressDialog(context);
        mDialog.setIndeterminate(true);
        mDialog.setCancelable(false);
        mDialog.setCanceledOnTouchOutside(false);
    }

    public static void show(Context context, String msg) {
        if (mDialog == null) {
            create(context);
        }
        mDialog.setMessage(msg);
        mDialog.show();
    }

    public static void hide() {
        if ((mDialog != null) && mDialog.isShowing()) {
            mDialog.hide();
        }
    }

    public static void dismiss() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }
}
