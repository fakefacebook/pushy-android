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

package com.weebly.opus1269.clipman.ui.help;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.weebly.opus1269.clipman.BuildConfig;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.model.Device;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.helpers.DrawableHelper;
import com.weebly.opus1269.clipman.ui.views.VectorDrawableTextView;

/**
 * This Activity handles the display of help & feedback about the app
 */
public class HelpActivity extends BaseActivity {

    private DialogFragment mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        mLayoutID = R.layout.activity_help;

        super.onCreate(savedInstanceState);

        TextView release = (TextView) findViewById(R.id.docRelease);
        release.setTag(getResources()
            .getString(R.string.help_doc_releas_tag_fmt,
                Prefs.getVersionName()));

        // color the TextView icons
        tintLeftDrawables();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        mOptionsMenuID = R.menu.menu_help;

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean processed = true;

        final int id = item.getItemId();
        switch (id) {
            case R.id.action_view_store:
                showInPlayStore();
                break;
            case R.id.action_version:
                showVersionDialog();
                break;
            case R.id.action_licenses:
                AppUtils.showWebUrl(
                    getString(R.string.help_licenses_path));
                break;
            default:
                processed = false;
                break;
        }

        return processed || super.onOptionsItemSelected(item);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Public methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Handle click on Help and feedback items
     * @param v the TextView that was clicked
     */
    public void onItemClicked(View v) {

        final TextView textView = (TextView) v;

        final int id = v.getId();
        switch (id) {
            case R.id.emailTranslate:
            case R.id.emailGeneral:
                emailMe((String) textView.getTag(), null);
                break;
            case R.id.emailQuestion:
            case R.id.emailBug:
            case R.id.emailFeature:
                emailMe((String) textView.getTag(), getEmailBody());
                break;
            case R.id.githubIssue:
            case R.id.docApp:
            case R.id.docRelease:
            case R.id.docSource:
                AppUtils.showWebUrl((String) textView.getTag());
                break;
            case R.id.license:
                mDialog.dismiss();
                break;
            default:
                break;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Send an email
     * @param subject email Subject
     * @param body Email Body
     */
    private void emailMe(String subject, String body) {
        final Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + AppUtils.EMAIL_ADDRESS));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        if(!TextUtils.isEmpty(body)) {
            intent.putExtra(Intent.EXTRA_TEXT, body);
        }
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    /**
     * Get system info. for body of support requests
     */
    private String getEmailBody() {
      return  "Clip Man Version: " + BuildConfig.VERSION_NAME + '\n' +
              "Android Version: " + Build.VERSION.RELEASE + '\n' +
              "Device: " + Device.getMyModel() + " \n \n \n";
    }

    /**
     * Show the {@link App} in the play store
     */
    private void showInPlayStore() {
        try {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(AppUtils.PLAY_STORE));
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException ignored) {
            Log.logD(TAG, "Could not open app in play store, trying web.");
            AppUtils.showWebUrl(AppUtils.PLAY_STORE_WEB);
        }
    }

    /**
     * Show the {@link VersionDialogFragment}
     */
    private void showVersionDialog() {
        mDialog = new VersionDialogFragment();
        mDialog.show(getSupportFragmentManager(), "VersionDialogFragment");
    }

    /**
     * color the LeftDrawables in all our {@link VectorDrawableTextView} views
     */
    private void tintLeftDrawables() {

        int color;
        if (Prefs.isLightTheme()) {
            color = R.color.deep_teal_500;
        } else {
            color = R.color.deep_teal_200;
        }

        DrawableHelper drawableHelper = DrawableHelper
                .withContext(this)
                .withColor(color)
                .withDrawable(R.drawable.ic_email_black_24dp)
                .tint();
        tintLeftDrawable(drawableHelper, R.id.emailQuestion);
        tintLeftDrawable(drawableHelper, R.id.emailBug);
        tintLeftDrawable(drawableHelper, R.id.emailFeature);
        tintLeftDrawable(drawableHelper, R.id.emailTranslate);
        tintLeftDrawable(drawableHelper, R.id.emailGeneral);

        drawableHelper = DrawableHelper
                .withContext(this)
                .withColor(color)
                .withDrawable(R.drawable.github_circle)
                .tint();
        tintLeftDrawable(drawableHelper, R.id.githubIssue);
        tintLeftDrawable(drawableHelper, R.id.docApp);
        tintLeftDrawable(drawableHelper, R.id.docRelease);
        tintLeftDrawable(drawableHelper, R.id.docSource);
    }

    /**
     * Color the leftDrawable in a {@link VectorDrawableTextView}
     * @param drawableHelper helper class
     * @param idView id of VectorDrawableTextView
     */
    private void tintLeftDrawable(DrawableHelper drawableHelper, int idView) {
        final TextView textView = (TextView) findViewById(idView);
        if (textView != null) {
            Drawable drawable = drawableHelper.get();
            textView.setCompoundDrawablesWithIntrinsicBounds(
                drawable, null, null, null);
        }
    }
}

