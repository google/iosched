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

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Keeps track of all devices nearby.
 *
 * Posts notifications when a new device is near, or if an old device is not
 * longer nearby.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NearbyDeviceManager {
    private String TAG = makeLogTag("NearbyDeviceManager");

    private BluetoothAdapter mBluetoothAdapter;
    private Timer mExpireTimer;
    private Timer mSearchTimer;
    private Handler mQueryHandler;
    private boolean mIsSearching = false;

    private NearbyDeviceAdapter mNearbyDeviceAdapter;

    private ArrayList<NearbyDevice> mDeviceBatchList;

    private Activity mActivity;

    private boolean mIsQueuing = false;
    // How often we should batch requests for metadata.
    private int QUERY_PERIOD = 500;
    // How often to search for new devices (ms).
    private int SEARCH_PERIOD = 5000;
    // How often to check for expired devices.
    private int EXPIRE_PERIOD = 3000;
    // How much time has to pass with a nearby device not being discovered before
    // we declare it gone.
    public static int MAX_INACTIVE_TIME = 10000;

    public NearbyDeviceManager(Activity activity, BluetoothAdapter adapter) {
        mBluetoothAdapter = adapter;
        mDeviceBatchList = new ArrayList<NearbyDevice>();
        mNearbyDeviceAdapter = new NearbyDeviceAdapter(activity);
        mQueryHandler = new Handler();
        mActivity = activity;
    }

    public boolean isEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    // Begins searching for nearby devices if not already searching. If the adapter is disabled,
    // does nothing.
    public void startSearchingForDevices() {
        if (mIsSearching || !mBluetoothAdapter.isEnabled()) {
            return;
        }
        mIsSearching = true;

        // Start a timer to do scans.
        if (mSearchTimer == null) {
            mSearchTimer = new Timer();
            mSearchTimer.scheduleAtFixedRate(new SearchTask(), 0, SEARCH_PERIOD);
        }

        // Start a timer to check for expired devices.
        if (mExpireTimer == null) {
            mExpireTimer = new Timer();
            mExpireTimer.scheduleAtFixedRate(new ExpireTask(), 0, EXPIRE_PERIOD);
        }
    }

    public void stopSearchingForDevices() {
        if (!mIsSearching) {
            return;
        }
        mIsSearching = false;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);

        // Stop expired device timer.
        mExpireTimer.cancel();
        mExpireTimer.purge();
        mExpireTimer = null;
        mSearchTimer.cancel();
        mSearchTimer.purge();
        mSearchTimer = null;
    }

    public NearbyDeviceAdapter getAdapter() {
        return mNearbyDeviceAdapter;
    }

    private class SearchTask extends TimerTask {

        @Override
        public void run() {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            boolean result = mBluetoothAdapter.startLeScan(mLeScanCallback);
            if (!result) {
                Log.e(TAG, "startLeScan failed.");
            }
        }
    }

    private class ExpireTask extends TimerTask {
        @Override
        public void run() {
            mNearbyDeviceAdapter.removeExpiredDevices();
        }
    }

    private Runnable mBatchMetadataRunnable = new Runnable () {
        @Override
        public void run() {
            batchFetchMetaData();
            mIsQueuing = false;
        }
    };

    private void batchFetchMetaData() {
        if(mDeviceBatchList.size() > 0) {
            MetadataResolver.getBatchMetadata(mDeviceBatchList);
            mDeviceBatchList = new ArrayList<NearbyDevice>(); // Clear out the list
        }
    }

    // NearbyDevice scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int RSSI, byte[] scanRecord) {
            Log.i(TAG, String.format("onLeScan: %s, RSSI: %d", device.getName(), RSSI));

            if (device.getName() == null) {
                return;
            }
            NearbyDevice candidateNearbyDevice = new NearbyDevice(device, RSSI);
            handleDeviceFound(candidateNearbyDevice);
        }
    };

    private void handleDeviceFound(NearbyDevice candidateNearbyDevice) {
        NearbyDevice nearbyDevice = mNearbyDeviceAdapter.getExistingDevice(candidateNearbyDevice);

        // Check if this is a new device.
        if (nearbyDevice != null) {
            // For existing devices, update their RSSI.
            nearbyDevice.updateLastSeen(candidateNearbyDevice.getLastRSSI());
            mNearbyDeviceAdapter.updateListUI();
        } else {
            // For new devices, add the device to the adapter.
            nearbyDevice = candidateNearbyDevice;
            if (nearbyDevice.isBroadcastingUrl()) {
                if (!mIsQueuing) {
                    mIsQueuing = true;
                    // We wait QUERY_PERIOD ms to see if any other devices are discovered so we can batch.
                    mQueryHandler.postAtTime(mBatchMetadataRunnable, QUERY_PERIOD);
                }
                // Add the device to the queue of devices to look for.
                mDeviceBatchList.add(nearbyDevice);
                mNearbyDeviceAdapter.addDevice(nearbyDevice);
            }
        }
    }
}
