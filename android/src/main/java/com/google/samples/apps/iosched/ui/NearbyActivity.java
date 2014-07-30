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

package com.google.samples.apps.iosched.ui;

import android.annotation.TargetApi;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.nearby.MetadataResolver;
import com.google.samples.apps.iosched.nearby.NearbyDeviceManager;
import com.google.samples.apps.iosched.util.PrefUtils;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * An activity that handles BLE scanning. It includes a MenuItem that, when clicked, presents the
 * user a list of nearby BLE devices. Hooks are provided for managing the presentation.
 *
 * The Nearby button is only shown if the attendee is attending in-person.
 *
 * This activity requires API level 18 because it utilizes BLE.
 */
public abstract class NearbyActivity extends BaseActivity implements NearbyFragment.Callbacks {
    private static final String TAG = makeLogTag(NearbyActivity.class);
    private static final int REQUEST_ENABLE_BT = 500;
    private static final int REQUEST_ENABLE_NEARBY = 501;
    protected static final String NEARBY_FRAGMENT_TAG = "NEARBY_FRAGMENT";

    private boolean mNearbyCapable = false;
    private boolean mShouldShowNearbyFragment = false;
    private NearbyDeviceManager mDeviceManager;
    private Button mNearbyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && PrefUtils.isAttendeeAtVenue(this)) {
            mNearbyCapable = initNearby();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mNearbyCapable && PrefUtils.hasEnabledBle(this)) {
            mDeviceManager.startSearchingForDevices();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (mShouldShowNearbyFragment) {
            mShouldShowNearbyFragment = false;
            handleNearbyClick();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK &&
                (requestCode == REQUEST_ENABLE_BT || requestCode == REQUEST_ENABLE_NEARBY)) {
            mShouldShowNearbyFragment = true;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        if (mNearbyCapable) {
            mDeviceManager.stopSearchingForDevices();
        }
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mNearbyCapable) {
            getMenuInflater().inflate(R.menu.nearby, menu);
            MenuItem nearbyItem = menu.findItem(R.id.menu_nearby);
            mNearbyButton =
                    (Button) nearbyItem.getActionView().findViewById(R.id.nearby_action_button);
            mNearbyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    handleNearbyClick();
                }
            });
            updateNearbyButton();
        }
        return super.onCreateOptionsMenu(menu);
    }

    protected abstract void showNearbyFragment(String tag);

    // Handles a click on the Nearby menu item. Recognizes several states:
    // 1. If the NearbyFragment is currently visible, does nothing.
    // 2. If the user has not enabled Nearby, presents them with a legalese screen asking them to
    // opt in.
    // 3. If the device's Bluetooth is disabled, ask the user to enable it.
    // 4. Otherwise, shows the fragment.
    private void handleNearbyClick() {
        final FragmentManager manager = getFragmentManager();
        Fragment fragment = manager.findFragmentByTag(NEARBY_FRAGMENT_TAG);
        if (fragment != null && fragment.isVisible()) {
            return;
        }
        if (!PrefUtils.hasEnabledBle(this)) {
            // If the user has not enabled Nearby, present them with a legalese screen asking them
            // to do so.
            Intent intent = new Intent(this, NearbyEulaActivity.class);
            startActivityForResult(intent, REQUEST_ENABLE_NEARBY);
            return;
        }
        if (!mDeviceManager.isEnabled()) {
            // If the user's bluetooth is not enabled, ask them politely to turn it on. Beginning
            // to search for devices should happen in onStart?
            DialogFragment dialogFragment = new EnableBluetoothDialogFragment();
            dialogFragment.show(getFragmentManager(), "EnableBluetoothDialogFragment");
            return;
        }
        // Otherwise, the user has opted into BLE and their Bluetooth device is turned on!
        showNearbyFragment(NEARBY_FRAGMENT_TAG);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean initNearby() {
        PackageManager pm = getPackageManager();
        if (pm != null && pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
            BluetoothAdapter adapter = manager.getAdapter();
            if (adapter != null) {
                MetadataResolver.initialize(this);
                mDeviceManager = new NearbyDeviceManager(this, adapter);
                mDeviceManager.getAdapter().registerDataSetObserver(new DataSetObserver() {
                    @Override
                    public void onChanged() {
                        updateNearbyButton();
                    }
                });
                return true;
            }
        }
        return false;
    }

    // Updates the nearby button's text to be the number of devices found. If no devices have been
    // found, the text is set to "Nearby".
    private void updateNearbyButton() {
        int count = mDeviceManager.getAdapter().getCount();
        final String text = (count == 0 ? "" : (String.valueOf(count) + " "))
                + getResources().getString(R.string.map_nearby_button);
        if (mNearbyButton != null) {
            mNearbyButton.setText(text);
            mNearbyButton.setSelected(count > 0);
        }
    }

    public NearbyDeviceManager getNearbyDeviceManager() {
        return mDeviceManager;
    }
}
