/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.nearby;

import android.app.Activity;
import android.os.Handler;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.samples.apps.iosched.R;

import java.util.*;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Adapter class for building views related to BLE devices.
 */
public class NearbyDeviceAdapter extends BaseAdapter {
    String TAG = makeLogTag("NearbyDeviceAdapter");

    private ArrayList<NearbyDevice> mNearbyDevices;
    private Activity mActivity;

    private Handler mHandler = new Handler();

    private long mLastChangeRequestTime = 0;
    private long NOTIFY_DELAY = 300;

    NearbyDeviceAdapter(Activity activity) {
        mNearbyDevices = new ArrayList<NearbyDevice>();
        mActivity = activity;
    }
    @Override
    public int getCount() {
        return mNearbyDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return mNearbyDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        NearbyDevice device = mNearbyDevices.get(i);
        return System.identityHashCode(device);
    }

    @Override
    public View getView(int i, View view, ViewGroup container) {
        if (view == null) {
            view = mActivity.getLayoutInflater().inflate(R.layout.ble_listitem_device, null);
        }

        NearbyDevice device = mNearbyDevices.get(i);

        DeviceMetadata deviceMetadata = device.getInfo();
        TextView infoView;

        infoView = (TextView) view.findViewById(R.id.title);
        if (deviceMetadata != null) {
            String title = deviceMetadata.title;
            if (!TextUtils.isEmpty(title)) {
                infoView.setText(Html.fromHtml(deviceMetadata.title));
                infoView.setVisibility(View.VISIBLE);
            } else {
                infoView.setVisibility(View.GONE);
            }
        } else {
            infoView.setText(R.string.loading);
            infoView.setVisibility(View.VISIBLE);
        }

        infoView = (TextView) view.findViewById(R.id.url);
        if (deviceMetadata != null) {
            infoView.setText(deviceMetadata.siteUrl);
        } else {
            infoView.setText(device.getUrl());
        }

        infoView = (TextView) view.findViewById(R.id.description);
        if (deviceMetadata != null) {
            String description = deviceMetadata.description;
            if (!TextUtils.isEmpty(description)) {
                infoView.setText(Html.fromHtml(deviceMetadata.description));
                infoView.setVisibility(View.VISIBLE);
            } else {
                infoView.setVisibility(View.GONE);
            }
        } else {
            infoView.setVisibility(View.INVISIBLE);
        }

        ImageView iconView = (ImageView) view.findViewById(R.id.icon);
        if (deviceMetadata != null) {
            iconView.setImageBitmap(deviceMetadata.icon);
        } else {
            iconView.setImageResource(R.drawable.empty_nearby_icon);
        }

        return view;
    }

    public void addDevice(final NearbyDevice device) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNearbyDevices.add(device);
                device.setAdapter(NearbyDeviceAdapter.this);
                queueChangedNotification();
            }
        });
    }

    public NearbyDevice getExistingDevice(NearbyDevice candidateDevice) {
        for (NearbyDevice device : mNearbyDevices) {
            if (device.getUrl().equals(candidateDevice.getUrl())) {
                return device;
            }
        }
        return null;
    }

    public ArrayList<NearbyDevice> removeExpiredDevices() {
        // Get a list of devices that we need to remove.
        ArrayList<NearbyDevice> toRemove = new ArrayList<NearbyDevice>();
        for (NearbyDevice device : mNearbyDevices) {
            if (device.isLastSeenAfter(NearbyDeviceManager.MAX_INACTIVE_TIME)) {
                toRemove.add(device);
            }
        }

        // Remove those devices from the list and notify the listener.
        for (final NearbyDevice device : toRemove) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mNearbyDevices.remove(device);
                    queueChangedNotification();
                }
            });
        }
        return toRemove;
    }

    public void updateListUI() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                queueChangedNotification();
            }
        });
    }

    private Runnable mNotifyRunnable = new Runnable() {
        @Override
        public void run() {
            notifyDataSetChanged();
        }
    };

    public void queueChangedNotification() {
        long now = System.currentTimeMillis();
        // If a notification was recently issued, create a pending notification.
        if (now - mLastChangeRequestTime < NOTIFY_DELAY) {
            // Ignore if there's a pending timer already.
            mHandler.removeCallbacks(mNotifyRunnable);
            mHandler.postDelayed(mNotifyRunnable, NOTIFY_DELAY);
        } else {
            // Otherwise, if there's no active timer, notify immediately.
            Log.i(TAG, "queueChangedNotification: Immediately notifying.");
            notifyDataSetChanged();
        }
    }

    @Override
    public void notifyDataSetChanged() {
        Log.i(TAG, "queueChangedNotification: notifyDataSetChanged");
        Collections.sort(mNearbyDevices, mRssiComparator);

        super.notifyDataSetChanged();

        // Cancel the pending notification timer if there is one.
        mHandler.removeCallbacks(mNotifyRunnable);
        mLastChangeRequestTime = System.currentTimeMillis();
    }


    private Comparator<NearbyDevice> mRssiComparator = new Comparator<NearbyDevice>() {
        @Override
        public int compare(NearbyDevice lhs, NearbyDevice rhs) {
            return rhs.getAverageRSSI() - lhs.getAverageRSSI();
        }
    };
}
