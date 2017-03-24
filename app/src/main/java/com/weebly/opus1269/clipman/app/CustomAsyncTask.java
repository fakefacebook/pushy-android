
package com.weebly.opus1269.clipman.app;

import android.app.Activity;
import android.os.AsyncTask;

/**
 * This class is part of a solution to the problem of
 * screen orientation/Activity destruction during lengthy Async tasks.
 * see: https://fattybeagle.com/2011/02/15/android-asynctasks-during-a-screen-rotation-part-ii/
 */

public abstract class CustomAsyncTask<TParams, TProgress, TResult>
    extends AsyncTask<TParams, TProgress, TResult> {
    protected static final String NO_ACTIVITY =
        "AsyncTask finished while no Activity was attached.";

    private final App mApp;
    protected Activity mActivity;

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

    protected void onActivityAttached() {}

    protected void onActivityDetached() {}

    @Override
    protected void onPreExecute() {
        mApp.addTask(mActivity, this);
    }

    @Override
    protected void onPostExecute(TResult result) {
        mApp.removeTask(this);
    }

    @Override
    protected void onCancelled() {
        mApp.removeTask(this);
    }

//      Example usage
//    private static class DoBackgroundTask extends CustomAsyncTask<Void, Integer, Void> {
//        private static final String TAG = "DoBackgroundTask";
//
//        private ProgressDialog mProgress;
//        private int mCurrProgress;
//
//        public DoBackgroundTask(SignInActivity activity) {
//            super(activity);
//        }
//
//        @Override
//        protected void onPreExecute() {
//            must call
//            super.onPreExecute();
//            showProgressDialog();
//        }
//
//        @Override
//        protected void onActivityDetached() {
//            if (mProgress != null) {
//                mProgress.dismiss();
//                mProgress = null;
//            }
//        }
//
//        @Override
//        protected void onActivityAttached() {
//            showProgressDialog();
//        }
//
//        private void showProgressDialog() {
//            mProgress = new ProgressDialog(mActivity);
//            mProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//            mProgress.setMessage("Doing stuff...");
//            mProgress.setCancelable(true);
//            mProgress.setOnCancelListener(new DialogInterface.OnCancelListener() {
//                @Override
//                public void onCancel(DialogInterface dialog) {
//                    cancel(true);
//                }
//            });
//
//            mProgress.show();
//            mProgress.setProgress(mCurrProgress);
//        }
//
//        @Override
//        protected Void doInBackground(Void... params) {
//            try {
//                for (int i = 0; i < 100; i+=10) {
//                    Thread.sleep(1000);
//                    this.publishProgress(i);
//                }
//
//            }
//            catch (InterruptedException e) {
//            }
//
//            return null;
//        }
//
//        @Override
//        protected void onProgressUpdate(Integer... progress) {
//            mCurrProgress = progress[0];
//            if (mActivity != null) {
//                mProgress.setProgress(mCurrProgress);
//            }
//            else {
//                AppUtils.logD(TAG, "Progress updated while no Activity was attached.");
//            }
//        }
//
//        @Override
//        protected void onPostExecute(Void result) {
//            must call
//            super.onPostExecute(result);
//
//            if (mActivity != null) {
//                mProgress.dismiss();
//                Toast.makeText(mActivity, "AsyncTask finished", Toast.LENGTH_LONG).show();
//            }
//            else {
//                AppUtils.logD(TAG, "AsyncTask finished while no Activity was attached.");
//            }
//        }
//    }

}