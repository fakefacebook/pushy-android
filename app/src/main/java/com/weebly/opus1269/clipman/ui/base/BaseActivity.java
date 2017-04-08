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

package com.weebly.opus1269.clipman.ui.base;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.model.Analytics;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.ui.helpers.MenuTintHelper;

import java.text.Collator;

/**
 * This Activity handles lots of the basic stuff. Make sure you use a standard
 * naming convention for you Activities views and actions. Extend from this.
 */

public abstract class BaseActivity extends AppCompatActivity implements
        SearchView.OnQueryTextListener {

     protected String TAG = this.getClass().getSimpleName();

    // Required to support Vector Drawables on pre-marshmallow devices
    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    protected int mLayoutID = -1;
    protected int mOptionsMenuID = -1;
    protected Menu mOptionsMenu = null;
    protected boolean mHomeUpEnabled = true;
    private Tracker mTracker;

    /**
     * saved instance state
     */

    private static final String STATE_QUERY_STRING = "query";
    protected String mQueryString = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTheme();

        super.onCreate(savedInstanceState);

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            // Restore value of members from saved state
            restoreInstanceState(savedInstanceState);
        }

        if (mLayoutID != -1) {
            setContentView(mLayoutID);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(mHomeUpEnabled);
        }

        mTracker = Analytics.INSTANCE.getTracker();

    }

    @Override
    protected void onResume() {
        super.onResume();

        mTracker.setScreenName(TAG);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        ((App) getApplication()).detach(this);

        outState.putString(STATE_QUERY_STRING, mQueryString);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        ((App) getApplication()).attach(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (mOptionsMenuID != -1) {
            getMenuInflater().inflate(mOptionsMenuID, menu);

            mOptionsMenu = menu;

            tintMenuItems();

            if (mOptionsMenu.findItem(R.id.action_search) != null) {
                setupSearch();
            }
        }

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Override to restore additional state
     *
     * @param savedInstanceState our state
     */
    protected void restoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mQueryString = savedInstanceState.getString(STATE_QUERY_STRING);
        }
    }

    /**
     * Override to implement business logic
     *
     * @param queryString String to search for
     * @return true if the new String differs from the current one
     */
    protected boolean setQueryString(String queryString) {
        boolean ret = false;
        if (!Collator.getInstance().equals(mQueryString, queryString)) {
            mQueryString = queryString;
            ret = true;
        }
        return ret;
    }

    protected void setFabVisibility(boolean show) {
        final FloatingActionButton fab =
                (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            if (show) {
                fab.show();
            } else {
                fab.hide();
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implement SearchView listeners
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public boolean onQueryTextSubmit(String query) {
        setQueryString(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        setQueryString(newText);
        return true;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Apply the user selected theme
     */
    private  void setTheme() {
        if (Prefs.isDarkTheme()) {
            this.setTheme(R.style.AppThemeDark);
        } else {
            this.setTheme(R.style.AppThemeLight);
        }
    }

    /**
     * Color the icons white for all API versions
     */
    private void tintMenuItems() {
        if (mOptionsMenu != null) {
            final int color = ContextCompat.getColor(this, R.color.icons);
            MenuTintHelper.on(mOptionsMenu)
                    .setMenuItemIconColor(color)
                    .apply(this);
        }
    }

    /**
     * Initialize the {@link android.support.v7.view.menu.ActionMenuItemView} for search
     */
    private void setupSearch() {
        final MenuItem searchItem = mOptionsMenu.findItem(R.id.action_search);
        if (searchItem != null) {
            final SearchManager searchManager =
                    (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            final SearchView searchView = (SearchView) searchItem.getActionView();
            searchView.setSearchableInfo(
                    searchManager.getSearchableInfo(getComponentName()));
            searchView.setOnQueryTextListener(this);
            final String localQueryString = mQueryString;

            // SearchView OnClose listener does not work
            // http://stackoverflow.com/a/12975254/4468645
            MenuItemCompat.setOnActionExpandListener(searchItem,
                    new MenuItemCompat.OnActionExpandListener() {
                        @Override
                        public boolean onMenuItemActionExpand(MenuItem menuItem) {
                            // Return true to allow the action view to expand
                            return true;
                        }

                        @Override
                        public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                            // Return true to allow the action view to collapse
                            // When the action view is collapsed, reset the query
                            setQueryString("");
                            // refresh menu, SearchAction does something funky to it
                            // after a rotate
                            mOptionsMenu.clear();
                            onCreateOptionsMenu(mOptionsMenu);
                            return true;
                        }
                    });

            if (!TextUtils.isEmpty(mQueryString)) {
                // http://stackoverflow.com/a/32397014/4468645
                // moved expandActionView out of run.
                // did not always work.
                MenuItemCompat.expandActionView(searchItem);
                searchView.post(new Runnable() {
                    @Override
                    public void run() {
                        searchView.setQuery(localQueryString, true);
                    }
                });
            }
        }
    }
}
