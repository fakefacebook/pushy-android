
package com.weebly.opus1269.clipman.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;

/**
 * This class is part of a solution to the problem of
 * screen orientation/Activity destruction during lengthy Async tasks.
 * see: https://fattybeagle.com/2011/02/15/android-asynctasks-during-a-screen-rotation-part-ii/
 */

public abstract class CustomAsyncTask<TParams, TProgress, TResult>
    extends ThreadedAsyncTask<TParams, TProgress, TResult> {
    protected static final String NO_ACTIVITY =
        "AsyncTask finished while no Activity was attached.";

    private final App mApp;
    protected Activity mActivity;
    protected ProgressDialog mProgress;
    protected String mProgressMessage;

    public CustomAsyncTask(Activity activity) {
        mActivity = activity;
        mApp = (App) mActivity.getApplication();
    }

    public void setActivity(Activity activity) {
        mActivity = activity;
        if (mActivity == null) {
            onActivityDetached();
        }
        else {
            onActivityAttached();
        }
    }

    private void onActivityAttached() {
        if (mProgress == null) {
            showProgressDialog();
        }
    }

    private void onActivityDetached() {
        dismissProgressDialog();
    }

    /**
     * Optionally, display progress dialog
     */
    private void showProgressDialog() {
        if (mProgressMessage != null) {
            mProgress = new ProgressDialog(mActivity);
            mProgress.setMessage(mProgressMessage);
            mProgress.setCancelable(true);
            mProgress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    cancel(true);
                }
            });

            mProgress.show();
        }
    }

    /**
     * Optionally, dismiss progress dialog
     */
    protected void dismissProgressDialog() {
        if (mProgress != null) {
            mProgress.dismiss();
            mProgress = null;
        }
    }

    @Override
    protected void onPreExecute() {
        mApp.addTask(mActivity, this);
        showProgressDialog();
    }

    @Override
    protected void onPostExecute(TResult result) {
        mApp.removeTask(this);
    }

    @Override
    protected void onCancelled() {
        mApp.removeTask(this);
    }
}