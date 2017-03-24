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

package com.weebly.opus1269.clipman.ui.signin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.model.Devices;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.model.User;
import com.weebly.opus1269.clipman.msg.MessagingClient;
import com.weebly.opus1269.clipman.msg.RegistrationClient;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.helpers.ProgressDialogHelper;

/**
 * This Activity handles account selection, sign-in, and authorization
 */
public class SignInActivity extends BaseActivity implements
    GoogleApiClient.OnConnectionFailedListener,
    View.OnClickListener {

    /**
     * Result of Google signIn attempt - {@value}
     */
    private static final int RC_SIGN_IN = 9001;

    /**
     * Google authorization
     */
    private GoogleApiClient mGoogleApiClient = null;

    /**
     * Firebase authorization
     */
    private FirebaseAuth mAuth = null;

    /**
     * To be notified of device removal
     */
    private BroadcastReceiver mDevicesReceiver = null;

    /**
     * Flag to indicate if sign-out includes revocation of app access
     */
    private boolean mIsRevoke = false;

    // saved state
    /**
     * Error message related to SignIn or SignOut
     */
    private String mErrorMessage = null;
    /**
     * Saved state: {@value}
     */
    private static final String STATE_ERROR = "error";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        mLayoutID = R.layout.activity_sign_in;

        super.onCreate(savedInstanceState);

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            // Restore value of members from saved state
            restoreInstanceState(savedInstanceState);
        }

        setFabVisibility(false);

        setupButtons();

        setupDevicesBroadcastReceiver();

        mAuth = FirebaseAuth.getInstance();

        setupGoogleSignIn();

        if (User.INSTANCE.isLoggedIn()) {
            attemptSilentSignIn();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager
            .getInstance(this)
            .registerReceiver(mDevicesReceiver,
            new IntentFilter(Devices.INTENT_FILTER));
        updateView();
    }

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager
            .getInstance(this)
            .unregisterReceiver(mDevicesReceiver);
        dismissProgressDialog();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the current state
        outState.putString(STATE_ERROR, mErrorMessage);
    }

    @Override
    protected void restoreInstanceState(Bundle savedInstanceState) {
        super.restoreInstanceState(savedInstanceState);

        mErrorMessage = savedInstanceState.getString(STATE_ERROR);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mOptionsMenuID = R.menu.menu_signin;

        super.onCreateOptionsMenu(menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean processed = true;

        final int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_help:
                showHelpDialog();
                break;
            default:
                processed = false;
                break;
        }

        return processed || super.onOptionsItemSelected(item);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from
        // GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            final GoogleSignInResult result =
                Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implement View.OnClickListener
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                onSignInClicked();
                break;
            case R.id.sign_out_button:
                onSignOutClicked();
                break;
            case R.id.revoke_access_button:
                onRevokeAccessClicked();
                break;
            default:
                break;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implement GoogleApiClient.OnConnectionFailedListener
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs
        // (including Sign-In) will not be available.
        Log.logE(TAG,
            "onConnectionFailed: " + connectionResult.getErrorMessage());
        mErrorMessage = getString(R.string.error_connection);
        dismissProgressDialog();
    }

    ///////////////////////////////////////////////////////////////////////////
    // public methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Should we revoke access at signout
     * @return true if revoke
     */
    public boolean isRevoke() {
        return mIsRevoke;
    }

    /**
     * SignOut of Google and Firebase
     */
    public void doSignOut() {
        if (mGoogleApiClient.isConnected()) {
            Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            FirebaseAuth.getInstance().signOut();
                            clearUser();
                        } else {
                            mErrorMessage =
                                getString(R.string.sign_out_err_fmt,
                                    status.getStatusMessage());
                            updateView();
                        }
                    }
                });
        } else {
            mErrorMessage = getString(R.string.sign_out_err_fmt, "");
            updateView();
        }
    }

    /**
     * Revoke access to app for this {@link User}
     */
    public void doRevoke() {
        if (mGoogleApiClient.isConnected()) {
            Auth.GoogleSignInApi
                .revokeAccess(mGoogleApiClient)
                .setResultCallback(
                    new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            if (status.isSuccess()) {
                                FirebaseAuth.getInstance().signOut();
                                clearUser();
                            } else {
                                mErrorMessage =
                                    getString(R.string.revoke_err_fmt,
                                        status.getStatusMessage());
                                updateView();
                            }
                        }
                    });
        } else {
            mErrorMessage = getString(R.string.revoke_err_fmt, "");
            updateView();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // private methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Show a dialog with rationale for signin
     */
    private void showHelpDialog() {
        DialogFragment dialogFragment = new HelpDialogFragment();
        dialogFragment.show(getSupportFragmentManager(), "HelpDialogFragment");
    }

    /**
     * Initialize  Buttons
     */
    private void setupButtons() {
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.revoke_access_button).setOnClickListener(this);

        final SignInButton signInButton =
            (SignInButton) findViewById(R.id.sign_in_button);
        if (signInButton != null) {
            signInButton.setStyle(
                SignInButton.SIZE_WIDE,
                SignInButton.COLOR_AUTO);
        }
    }

    /**
     * Initialize  {@link Devices} {@link BroadcastReceiver}
     */
    private void setupDevicesBroadcastReceiver() {
        // handler for received Intents for the "devices" event
        // we want to know when our device is removed so we can finish
        // logout
        mDevicesReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final Bundle bundle = intent.getBundleExtra(Devices.BUNDLE);
                final String action = bundle.getString(Devices.ACTION);

                if ((action != null) &&
                    action.equals(Devices.ACTION_MY_DEVICE)) {
                    doUnregister();
                }
            }
        };
    }

    /**
     * Initialize Google SignIn
     */
    private void setupGoogleSignIn() {
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        final GoogleSignInOptions gso =
            new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestIdToken(getString(R.string.default_web_client_id))
                .build();

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
            .enableAutoManage(this, this)
            .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
            .build();
    }

    /**
     * Try to signin with cached credentials or cross-device single sign-on
     */
    private void attemptSilentSignIn() {
        final OptionalPendingResult<GoogleSignInResult> opr =
            Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);

        if (opr.isDone()) {
            // If the user's cached credentials are valid,
            // the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            handleSignInResult(opr.get());
        } else {
            // If the user has not previously signed in on this device
            // or the sign-in has expired,
            // this asynchronous branch will attempt to sign in the user
            // silently.  Cross-device single sign-on will occur in this branch.
            showProgressDialog(getString(R.string.signing_in));
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(@NonNull GoogleSignInResult r) {
                    dismissProgressDialog();
                    handleSignInResult(r);
                }
            });
        }
    }

    /**
     * All SignIn attempts will come through here
     * @param result The {@link GoogleSignInResult} of any SignIn attempt
     */
    private void handleSignInResult(GoogleSignInResult result) {
        mErrorMessage = "";
        if (result.isSuccess()) {
            // Authenticate with Firebase
            final GoogleSignInAccount account = result.getSignInAccount();
            firebaseAuthWithGoogle(account);

            // User is signed in
            User.INSTANCE.set(account);
            updateView();

            assert account != null;
            final String idToken = account.getIdToken();
            if (!Prefs.isDeviceRegistered() && !TextUtils.isEmpty(idToken)) {
                // register with server
                new RegistrationClient.RegisterAsyncTask(
                    SignInActivity.this, idToken).execute();
            }
        } else {
            // reset info.
            mErrorMessage =
                getString(R.string.sign_in_err_fmt, result.getStatus().toString());
            Log.logE(TAG, mErrorMessage);
            clearUser();
        }
    }

    /**
     * Authorize with Firebase as well
     * @param acct - user account info. Result of successful Google signIn
     */
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential =
            GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    Log.logD(TAG, "signInWithCredential:onComplete: " +
                        task.isSuccessful());

                    // If sign in fails, display a message to the user.
                    // If sign in succeeds the auth state listener will be
                    // notified and logic to handle the
                    // signed in user can be handled in the listener.
                    if (!task.isSuccessful()) {
                        Log.logEx(TAG, "signInWithCredential",
                            task.getException());
                    }
                    // success
                }
            });
    }

    /**
     * Event: SignIn button clicked
     */
    private void onSignInClicked() {
        final Intent signInIntent =
            Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    /**
     * Event: SignOut button clicked
     */
    private void onSignOutClicked() {
        mIsRevoke = false;
        if (Prefs.isPushClipboard()) {
            // also handles sign-out
            MessagingClient.sendDeviceRemoved();
        } else {
            doSignOut();
        }
    }

    /**
     * Event: Revoke access button clicked
     */
    private void onRevokeAccessClicked() {
        mIsRevoke = true;
        if (Prefs.isPushClipboard()) {
            // also handles revocation
            MessagingClient.sendDeviceRemoved();
        } else {
            doRevoke();
        }
    }

    /**
     * Unregister with fcm. This will also perform the sign-out or revoke
     */
    private void doUnregister() {
        new RegistrationClient.UnregisterAsyncTask(
            SignInActivity.this).execute();
    }

    /**
     * Remove all {@link User} info.
     */
    private void clearUser() {
        User.INSTANCE.clear();
        updateView();
    }

    /**
     * Set the state of all the UI elements
     */
    private void updateView() {
        final View signInView = findViewById(R.id.sign_in);
        final View signOutView = findViewById(R.id.sign_out_and_disconnect);
        final TextView userNameView = (TextView) findViewById(R.id.user_name);
        final TextView emailView = (TextView) findViewById(R.id.email);
        final TextView errorView = (TextView) findViewById(R.id.error_message);
        if (signInView == null || signOutView == null) {
            // hmm
            return;
        }

        if (User.INSTANCE.isLoggedIn()) {
            signInView.setVisibility(View.GONE);
            signOutView.setVisibility(View.VISIBLE);
            userNameView.setText(User.INSTANCE.getName());
            emailView.setVisibility(View.VISIBLE);
            emailView.setText(User.INSTANCE.getEmail());
        } else {
            signInView.setVisibility(View.VISIBLE);
            signOutView.setVisibility(View.GONE);
            userNameView.setText(getString(R.string.signed_out));
            emailView.setVisibility(View.GONE);
            emailView.setText("");
        }
        errorView.setText(mErrorMessage);
    }

    /**
     * Display progress dialod
     * @param message - message to display
     */
    private void showProgressDialog(String message) {
        ProgressDialogHelper.show(this, message);
    }

    /**
     * Remove progress dialod
     */
    private void dismissProgressDialog() {
        ProgressDialogHelper.dismiss();
    }
}