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

package com.weebly.opus1269.clipman.ui.devices;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.App;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.Device;
import com.weebly.opus1269.clipman.model.Devices;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.ui.helpers.DrawableHelper;

/**
 * Bridge between the Devices RecyclerView and the Devices class
 */
public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.DeviceViewHolder> {

    ///////////////////////////////////////////////////////////////////////////
    // superclass overrides
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final Context context = parent.getContext();
        final LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        final View view = inflater.inflate(R.layout.device_row, parent, false);

        // Return a new holder instance
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DeviceViewHolder holder, int position) {
        final Context context = App.getContext();

        tintIcons(holder);

        // Get the data model based on position
        final Device device = Devices.get(position);

        final String desc =
                context.getString(R.string.device_nickname_fmt, device.getNickname()) + '\n' +
                context.getString(R.string.device_model_fmt, device.getModel()) + '\n' +
                context.getString(R.string.device_SN_fmt, device.getSN()) + '\n' +
                context.getString(R.string.device_OS_fmt, device.getOS());
        final TextView deviceTextView = holder.deviceTextView;
        deviceTextView.setText(desc);

        final CharSequence value = AppUtils.getRelativeDisplayTime(device.getLastSeen());
        final TextView lastSeenTextView = holder.lastSeenTextView;
        lastSeenTextView.setText(context.getString(R.string.device_last_seen_fmt, value));

        holder.forgetButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Devices.remove(device);
                    }
                }
        );
    }

    @Override
    public int getItemCount() {
        return Devices.getCount();
    }

    /**
     * Color the Vector Drawables based on theme
     *
     * @param holder ClipViewHolder
     */
    private void tintIcons(DevicesAdapter.DeviceViewHolder holder) {
        final int color;

        if (Prefs.isLightTheme()) {
            color = android.R.color.primary_text_light;
        } else {
            color = android.R.color.primary_text_dark;
        }

        DrawableHelper
                .withContext(App.getContext())
                .withColor(color)
                .withDrawable(R.drawable.ic_clear_black_24dp)
                .tint()
                .applyTo(holder.forgetButton);
    }

    ///////////////////////////////////////////////////////////////////////////
    // inner classes
    ///////////////////////////////////////////////////////////////////////////

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
       final TextView lastSeenTextView;
        final TextView deviceTextView;
        final ImageButton forgetButton;

        DeviceViewHolder(View view) {
            super(view);

            lastSeenTextView = (TextView) view.findViewById(R.id.lastSeenDate);
            deviceTextView = (TextView) view.findViewById(R.id.deviceText);
            forgetButton = (ImageButton) view.findViewById(R.id.forgetButton);
        }
    }
}
