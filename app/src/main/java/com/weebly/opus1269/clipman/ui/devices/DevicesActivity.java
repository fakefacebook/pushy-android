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

package com.weebly.opus1269.clipman.ui.devices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.Devices;
import com.weebly.opus1269.clipman.msg.MessagingClient;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;
import com.weebly.opus1269.clipman.ui.helpers.NotificationHelper;

/**
 * Activity to manage our connected devices
 */
public class DevicesActivity extends BaseActivity {

    // Adapter being used to display the list's data
    private DevicesAdapter mAdapter = null;

    private BroadcastReceiver mDevicesReceiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        mLayoutID = R.layout.activity_devices;

        super.onCreate(savedInstanceState);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doRefresh();
                }
            });
        }

        setupRecyclerView();

        setupDevicesBroadcastReceiver();
     }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mDevicesReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register mDevicesReceiver to receive Device notifications.
        LocalBroadcastManager.getInstance(this).registerReceiver(mDevicesReceiver,
                new IntentFilter(Devices.INTENT_FILTER));

        NotificationHelper.removeDevices();

        // ping devices
        MessagingClient.sendPing();

        // so relative dates get updated
        mAdapter.notifyDataSetChanged();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        mOptionsMenuID = R.menu.menu_devices;

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
            default:
                processed = false;
                break;
        }

        return processed || super.onOptionsItemSelected(item);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods
    ///////////////////////////////////////////////////////////////////////////

    private void setupRecyclerView() {
        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.deviceList);
        if (recyclerView != null) {
            mAdapter = new DevicesAdapter();
            recyclerView.setAdapter(mAdapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    private void setupDevicesBroadcastReceiver() {
        // handler for received Intents for the "devices" event
        mDevicesReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                notifyAdapter(intent);
            }

            private void notifyAdapter(Intent intent) {
                final Bundle bundle = intent.getBundleExtra(Devices.BUNDLE);
                final String action = bundle.getString(Devices.ACTION);
                final int pos = bundle.getInt(Devices.POS);
                if (action == null) {
                    return;
                }

                AppUtils.logD(TAG, "Devices change: " + action + " pos: " + pos);

                switch (action) {
                    case Devices.ACTION_ADD:
                        mAdapter.notifyItemInserted(pos);
                        break;
                   case Devices.ACTION_CHANGE:
                        mAdapter.notifyItemChanged(pos);
                        break;
                    case Devices.ACTION_REMOVE:
                        mAdapter.notifyItemRemoved(pos);
                        break;
                    case Devices.ACTION_CLEAR:
                        mAdapter.notifyDataSetChanged();
                        break;
                    default:
                        break;
                }
            }
        };
    }

    private void doRefresh() {
        mAdapter.notifyDataSetChanged();
        MessagingClient.sendPing();
    }

}
