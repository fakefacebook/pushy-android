/*
 *
 * Copyright 2016 Michael A Updike
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

package com.weebly.opus1269.clipman.msg;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.CustomAsyncTask;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.backend.registration.Registration;
import com.weebly.opus1269.clipman.backend.registration.model.EndpointRet;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.ui.signin.SignInActivity;

import java.io.IOException;

/**
 * This helper class is the interface to our gae Registration Endpoint
 */
public class RegistrationClient extends Endpoint {
    private static final String TAG = "RegistrationClient";

    private static final String ERROR_REGISTER =
        App.getContext().getString(R.string.err_register);
    private static final String ERROR_UNREGISTER =
        App.getContext().getString(R.string.err_unregister);
    private static final String ERROR_INVALID_REGID =
        App.getContext().getString(R.string.err_invalid_regid);

    private RegistrationClient() {}

    /**
     * Register with server
     * @param idToken - authorization token
     * @return getSuccess() false on error
     */
    public static EndpointRet register(String idToken) {
        EndpointRet ret = new EndpointRet();
        ret.setSuccess(false);
        ret.setReason(Msg.ERROR_UNKNOWN);

        if (notSignedIn()) {
            ret.setSuccess(true);
            return ret;
        } else if (Prefs.isDeviceRegistered()) {
            Log.logD(TAG, "Already registered.");
            ret.setSuccess(true);
            return ret;
        } else if (!Prefs.isAllowReceive()) {
            Log.logD(TAG, "User doesn't want to receive messasges.");
            ret.setSuccess(true);
            return ret;
        }

        final String regToken = getRegToken();
        if (TextUtils.isEmpty(regToken)) {
            ret.setReason(Log.logE(TAG, ERROR_INVALID_REGID));
            return ret;
        }

        boolean isRegistered = false;
        try {
            final GoogleCredential credential = getCredential(idToken);
            if (credential == null) {
                ret.setReason(Log.logE(TAG, Msg.ERROR_CREDENTIAL));
                return ret;
            }

            // call server
            final Registration regService = getRegistrationService(credential);
            ret = regService.register(regToken).execute();
            if (ret.getSuccess()) {
                isRegistered = true;
            } else {
                ret.setReason(Log.logE(TAG,
                    ERROR_REGISTER + " " + ret.getReason()));
            }
        } catch (final IOException ex) {
            ret.setReason(Log.logEx(TAG, ERROR_REGISTER, ex));
        } finally {
            Prefs.setDeviceRegistered(isRegistered);
        }

        return ret;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Unregister with server
     * @return getSuccess() false on error
     */
    private static EndpointRet unregister() {
        EndpointRet ret = new EndpointRet();
        ret.setSuccess(false);
        ret.setReason(Msg.ERROR_UNKNOWN);

        if (notSignedIn()) {
            ret.setSuccess(true);
            return ret;
        } else if (!Prefs.isDeviceRegistered()) {
            Log.logD(TAG, Msg.ERROR_NOT_REGISTERED);
            ret.setSuccess(true);
            return ret;
        }

        boolean isRegistered = true;
        try {
             final String regToken = getRegToken();
            if (TextUtils.isEmpty(regToken)) {
                ret.setReason(Log.logE(TAG, ERROR_INVALID_REGID));
                return ret;
            }

            final GoogleCredential credential = getCredential(null);
            if (credential == null) {
                ret.setReason(Log.logE(TAG, Msg.ERROR_CREDENTIAL));
                return ret;
            }

            // call server
            final Registration regService = getRegistrationService(credential);
            ret = regService.unregister(regToken).execute();
            if (ret.getSuccess()) {
                isRegistered = false;
            } else {
                ret.setReason(Log.logE(TAG, ERROR_UNREGISTER +
                    " " + ret.getReason()));
            }
        } catch (final IOException ex) {
            ret.setReason(Log.logEx(TAG, ERROR_UNREGISTER, ex));
        }  finally {
            Prefs.setDeviceRegistered(isRegistered);
        }

        return ret;
    }

    /**
     * Get an authorized connection to the RegistrationEndpoint
     * @param credential - authorization for current user
     * @return Connection to RegistrationEndpoint on server
     */
    private static Registration
    getRegistrationService(GoogleCredential credential) {
        final Registration.Builder builder =
            new Registration.Builder(new NetHttpTransport(),
                new AndroidJsonFactory(), credential);

        builder.setApplicationName(AppUtils.getApplicationName());

        // for development purposes
        setLocalServer(builder);

        return builder.build();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Inner Classes
    ///////////////////////////////////////////////////////////////////////////

    /**
     * AsyncTask to register our with the server.
     */
    public static class RegisterAsyncTask extends
        CustomAsyncTask<Void, Void, String> {

        private ProgressDialog mProgress;
        private final String mIdToken;

        public RegisterAsyncTask(Activity activity, String idToken) {
            super(activity);
            mIdToken = idToken;
        }

        @Override
        protected void onPreExecute() {
            // must call
            super.onPreExecute();
            showProgressDialog();
        }

        @Override
        protected void onActivityDetached() {
            if (mProgress != null) {
                mProgress.dismiss();
                mProgress = null;
            }
        }

        @Override
        protected void onActivityAttached() {
            if (mProgress == null) {
                showProgressDialog();
            }
        }

        private void showProgressDialog() {
            mProgress = new ProgressDialog(mActivity);
            mProgress.setMessage(
                mActivity.getString(R.string.registering));
            mProgress.setCancelable(true);
            mProgress
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        cancel(true);
                    }
                });

            mProgress.show();
        }

        @Override
        protected String doInBackground(Void... params) {
            String error = "";
            // register device with the server - blocks
            EndpointRet ret = RegistrationClient.register(mIdToken);
            if(!ret.getSuccess()) {
                error = ret.getReason();
            }
            return error;
        }

        @Override
        protected void onPostExecute(String error) {
            // must call
            super.onPostExecute(error);

            if (mActivity != null) {
                mProgress.dismiss();
                if (!TextUtils.isEmpty(error)) {
                    // failed to register Device with server
                    if (mActivity instanceof SignInActivity) {
                        ((SignInActivity) mActivity).doSignOut();
                    }
                    new AlertDialog.Builder(mActivity)
                        .setTitle(R.string.err_register)
                        .setMessage(error)
                        .setPositiveButton(R.string.button_dismiss, null)
                        .show();
                } else {
                    // let others know we are here
                    MessagingClient.sendDeviceAdded();
                }
            } else {
                Log.logD(TAG, NO_ACTIVITY);
            }
        }

    }

    /**
     * AsyncTask to unregister from server.
     * Also, optionally sign-out or revoke access on success
     */
    public static class UnregisterAsyncTask extends
        CustomAsyncTask<Void, Void, String> {

        private ProgressDialog mProgress;

        public UnregisterAsyncTask(Activity activity) {
            super(activity);
        }

        @Override
        protected void onPreExecute() {
            // must call
            super.onPreExecute();
            showProgressDialog();
        }

        @Override
        protected void onActivityDetached() {
            if (mProgress != null) {
                mProgress.dismiss();
                mProgress = null;
            }
        }

        @Override
        protected void onActivityAttached() {
            if (mProgress == null) {
                showProgressDialog();
            }
        }

        private void showProgressDialog() {
            mProgress = new ProgressDialog(mActivity);
            mProgress.setMessage(
                mActivity.getString(R.string.unregistering));
            mProgress.setCancelable(true);
            mProgress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    cancel(true);
                }
            });

            mProgress.show();
        }

        @Override
        protected String doInBackground(Void... params) {
            String error = "";
            // unregister with the server - blocks
            EndpointRet ret = RegistrationClient.unregister();
            if (!ret.getSuccess()) {
                error = ret.getReason();
            }
            return error;
        }

        @Override
        protected void onPostExecute(String error) {
            // must call
            super.onPostExecute(error);

            if (mActivity != null) {
                mProgress.dismiss();
                if (!TextUtils.isEmpty(error)) {
                    // failed to unregister Device
                    MessagingClient.sendDeviceAdded();
                    new AlertDialog.Builder(mActivity)
                        .setTitle(R.string.err_unregister)
                        .setMessage(error)
                        .setPositiveButton(R.string.button_dismiss, null)
                        .show();
                } else {
                    if (mActivity instanceof SignInActivity) {
                        SignInActivity act = (SignInActivity) mActivity;
                        if (act.isRevoke()) {
                            act.doRevoke();
                        } else {
                            act.doSignOut();
                        }
                    }
                }
            } else {
                Log.logD(TAG, NO_ACTIVITY);
            }
        }
    }
}