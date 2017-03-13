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

package com.weebly.opus1269.clipman.ui.main;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.ClipContentProvider;
import com.weebly.opus1269.clipman.model.ClipItem;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.model.User;
import com.weebly.opus1269.clipman.services.ClipboardWatcherService;
import com.weebly.opus1269.clipman.ui.devices.DevicesActivity;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.clipviewer.ClipViewerActivity;
import com.weebly.opus1269.clipman.ui.clipviewer.ClipViewerFragment;
import com.weebly.opus1269.clipman.ui.help.HelpActivity;
import com.weebly.opus1269.clipman.ui.helpers.MenuTintHelper;
import com.weebly.opus1269.clipman.ui.helpers.NotificationHelper;
import com.weebly.opus1269.clipman.ui.settings.SettingsActivity;
import com.weebly.opus1269.clipman.ui.signin.SignInActivity;

/**
 * Thia is the top level Activity for the app
 */

public class MainActivity extends BaseActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        View.OnLayoutChangeListener,
        ClipViewerFragment.OnClipChanged,
        DeleteDialogFragment.DeleteDialogListener,
        SortTypeDialogFragment.SortTypeDialogListener {

    private ClipLoaderManager mLoaderManager;

    // items from last delete operation
    private ContentValues[] mUndoItems = null;

    /**
     * saved preferences
     */

    // AppBar setting for fav filter
    private Boolean mFavFilter = false;

    /**
     * saved instance state
     */

    // The currently selected position in the list, delegated to ClipCursorAdapter
    private static final String STATE_POS = "pos";

    // The database _ID of the selection list item, delegated to ClipCursorAdapter
    private static final String STATE_ITEM_ID = "item_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        mLayoutID = R.layout.activity_main;

        // We are the big dog, no need for this
        mHomeUpEnabled = false;

        super.onCreate(savedInstanceState);

        // handles the adapter and RecyclerView
        mLoaderManager = new ClipLoaderManager(this);

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            // Restore value of members from saved state
            final int pos = savedInstanceState.getInt(STATE_POS);
            final long id = savedInstanceState.getLong(STATE_ITEM_ID);

            mLoaderManager.getAdapter().restoreSelection(pos, id);
        }

        mFavFilter = Prefs.isFavFilter();

        // start if needed
        ClipboardWatcherService.startService(false);

        setupNavigationView();

        if (AppUtils.isDualPane()) {
            // create the clip viewer for the two pane option
            final ClipViewerFragment fragment =
                    ClipViewerFragment.newInstance(new ClipItem(), "");
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.clip_viewer_container, fragment)
                    .commit();
        }

        setFabVisibility(false);

        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        final NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        handleIntent();
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateNavHeader();

        NotificationHelper.removeClips();

        // so relative dates get updated
        mLoaderManager.getAdapter().notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // save persistent state
        Prefs.setFavFilter(mFavFilter);

        mUndoItems = null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(STATE_POS, mLoaderManager.getAdapter().getSelectedPos());
        outState.putLong(STATE_ITEM_ID, mLoaderManager.getAdapter().getSelectedItemID());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final boolean ret;

        mOptionsMenuID = R.menu.menu_main;

        ret = super.onCreateOptionsMenu(menu);

        setPrefFavFilter(mFavFilter, false);

        return ret;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean processed = true;

        final int id = item.getItemId();
        switch (id) {
            case R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                setQueryString("");
                break;
             case R.id.action_fav_filter:
                 // toggle
                setPrefFavFilter(!mFavFilter, true);
                break;
            case R.id.action_delete:
                showDeleteDialog();
                break;
            case R.id.action_sort:
                showSortTypeDialog();
                break;
            case R.id.action_settings:
                startActivity(SettingsActivity.class);
                break;
            case R.id.action_help:
                startActivity(HelpActivity.class);
                break;
            default:
                processed = false;
                break;
        }

        return processed || super.onOptionsItemSelected(item);
    }

    @Override
    protected boolean setQueryString(String queryString) {
        boolean ret = false;
        if(super.setQueryString(queryString)) {
            getSupportLoaderManager().restartLoader(0, null, mLoaderManager);
            ret = true;
        }
        return ret;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implement NavigationView.OnNavigationItemSelectedListener
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        final int id = item.getItemId();

        switch (id) {
            case R.id.nav_account:
                startActivity(SignInActivity.class);
                break;
            case R.id.nav_devices:
                startActivity(DevicesActivity.class);
                break;
            case R.id.nav_settings:
                startActivity(SettingsActivity.class);
                break;
            case R.id.nav_help:
                startActivity(HelpActivity.class);
                break;
            default:
                break;
        }

        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implement View.OnLayoutChangeListener
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Set NavigationView header aspect ratio to 16:9
     */
    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
                               int oldTop, int oldRight, int oldBottom) {
        final NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

        if (v.equals(navigationView)) {
            final int oldWidth = oldRight - oldLeft;
            final int width = right - left;
            final View hView = navigationView.getHeaderView(0);
            if ((hView != null) && (oldWidth != width)) {
                hView.getLayoutParams().height = Math.round((9.0F / 16.0F) * width);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implement ClipViewerFragment.OnClipChanged
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onClipChanged(ClipItem clipItem) {
        setFabVisibility(!TextUtils.isEmpty(clipItem.getText()));
        setTitle();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implement DeleteDialogFragment.DeleteDialogListener
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onDeleteDialogPositiveClick(Boolean deleteFavs) {
        // save items for undo
        mUndoItems = ClipContentProvider.getAll(this, deleteFavs);

        final int nRows = ClipContentProvider.deleteAll(this, deleteFavs);

        String message = nRows + getResources().getString(R.string.items_deleted);
        switch (nRows) {
            case 0:
                message = getResources().getString(R.string.item_delete_empty);
                break;
            case 1:
                message = getResources().getString(R.string.item_deleted_one);
                break;
            default:
                break;
        }
        final Snackbar snack =
                Snackbar.make(findViewById(R.id.fab), message, Snackbar.LENGTH_LONG);
        if (nRows > 0) {
            snack.setAction("UNDO", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ClipContentProvider.insert(MainActivity.this, mUndoItems);
                        }
                    })
                    .addCallback(new Snackbar.Callback() {

                        @Override
                        public void onDismissed(Snackbar snackbar, int event) {
                            mUndoItems = null;
                        }

                        @Override
                        public void onShown(Snackbar snackbar) {
                        }
                    });
        }
        snack.show();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implement SortTypeDialogFragment.SortTypeDialogListener
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onSortTypeSelected() {
        getSupportLoaderManager().restartLoader(0, null, mLoaderManager);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Package private methods
    ///////////////////////////////////////////////////////////////////////////

    Boolean getFavFilter() {
        return mFavFilter;
    }

    String getQueryString() {
        return mQueryString;
    }

    ClipLoaderManager getClipLoaderManager() {
        return mLoaderManager;
    }

    /**
     * Start the {@link ClipViewerActivity} or update the {@link ClipViewerFragment}
     *
     * @param clipItem item to display
     */
    void startOrUpdateClipViewer(ClipItem clipItem) {
        if (AppUtils.isDualPane()) {
            final ClipViewerFragment fragment = getClipViewerFragment();
            if (fragment != null) {
                fragment.setClipItem(clipItem);
                fragment.setHighlightText(mQueryString);
            }
        } else {
            final Intent intent = new Intent(this, ClipViewerActivity.class);
            intent.putExtra(ClipViewerFragment.ARG_CLIP_ITEM, clipItem);
            intent.putExtra(ClipViewerFragment.ARG_HIGHLIGHT, mQueryString);
            startActivity(intent);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Process intents we know about
     */
    private void handleIntent() {
        final Intent intent = getIntent();
        final String action = intent.getAction();
        final String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && (type != null)) {
            if (ClipItem.TEXT_PLAIN.equals(type)) {
                final String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (!TextUtils.isEmpty(sharedText)) {
                    final ClipItem item = new ClipItem(sharedText);
                    ClipContentProvider.insert(this, item);
                    startOrUpdateClipViewer(item);
                }
            }
        } else if (intent.hasExtra(ClipItem.INTENT_EXTRA_CLIP_ITEM)) {
            final ClipItem item =
                    (ClipItem) intent.getSerializableExtra(ClipItem.INTENT_EXTRA_CLIP_ITEM);
            intent.removeExtra(ClipItem.INTENT_EXTRA_CLIP_ITEM);
            startOrUpdateClipViewer(item);
        }
    }

    /**
     * Start an {@link android.app.Activity}
     * @param cls Class of Activity
     */
    private void startActivity(Class cls) {
        final Intent intent = new Intent(this, cls);
        startActivity(intent);
    }

    /**
     * Show the {@link DeleteDialogFragment} for verifying delete all
     */
    private void showDeleteDialog() {
        final DialogFragment dialog = new DeleteDialogFragment();
        dialog.show(getSupportFragmentManager(), "DeleteDialogFragment");
    }

    /**
     * Show the {@link SortTypeDialogFragment} for selecting list sort type
     */
    private void showSortTypeDialog() {
        final DialogFragment dialog = new SortTypeDialogFragment();
        dialog.show(getSupportFragmentManager(), "SortTypeDialogFragment");
    }

    /**
     * Initialize the NavigationView
     *
     */
    private void setupNavigationView() {
        final NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView == null) {
            return;
        }
        navigationView.addOnLayoutChangeListener(this);

        // Handle click on header
        final View hView = navigationView.getHeaderView(0);
        hView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                if (drawer != null) {
                    drawer.closeDrawer(GravityCompat.START);
                }
                startActivity(SignInActivity.class);
            }
        });

    }

    private void setTitle() {
        final ClipItem clipItem = getClipItemClone();
        if (AppUtils.isDualPane()) {
            if (clipItem.isRemote()) {
                setTitle(getString(R.string.title_activity_main_remote_fmt, clipItem.getDevice()));
            } else {
                setTitle(getString(R.string.title_activity_main_local));
            }
        }
        else {
            setTitle(getString(R.string.app_name));
        }
    }

    private ClipViewerFragment getClipViewerFragment() {
        return (ClipViewerFragment) getSupportFragmentManager()
                .findFragmentById(R.id.clip_viewer_container);
    }

    private ClipItem getClipItemClone() {
        return getClipViewerFragment().getClipItemClone();
    }

    private void setPrefFavFilter(boolean prefFavFilter, boolean restart) {
        mFavFilter = prefFavFilter;

        if (mOptionsMenu != null) {
            final MenuItem menuItem = mOptionsMenu.findItem(R.id.action_fav_filter);
            int colorID;
            if (mFavFilter) {
                menuItem.setIcon(R.drawable.ic_favorite_black_24dp);
                menuItem.setTitle(R.string.action_show_all);
                colorID = R.color.red_500_translucent;
            } else {
                menuItem.setIcon(R.drawable.ic_favorite_border_black_24dp);
                menuItem.setTitle(R.string.action_show_favs);
                colorID = R.color.icons;
           }

            final int color = ContextCompat.getColor(this, colorID);
            MenuTintHelper.colorMenuItem(menuItem, color, 255);
        }

        if (restart) {
            getSupportLoaderManager().restartLoader(0, null, mLoaderManager);
        }
    }

    /**
     * Set Nav Header based on sign-in state
     */
    private void updateNavHeader() {
        final NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView == null) {
            return;
        }
        final View hView = navigationView.getHeaderView(0);
        if (hView == null) {
            return;
        }

        // set Devices menu state
        final Menu menu = navigationView.getMenu();
        final MenuItem deviceItem = menu.findItem(R.id.nav_devices);
        deviceItem.setEnabled(Prefs.isDeviceRegistered());

        User.INSTANCE.setNavigationHeaderView(hView);
    }

}