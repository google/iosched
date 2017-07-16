/*
 * Copyright (c) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.samples.apps.iosched.map;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.service.LocalRefreshingBootstrapService;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Activity that contains an {@link EditorMapFragment} and displays messages for
 * marker clicks and other events on screen.
 * Bootstrap data is always refreshed from the sdcard, see {@link LocalRefreshingBootstrapService}.
 * In contrast to {@link MapActivity}, this activity stands on its own and does not link
 * back to any other parts of the app.
 */
public class MapEditorActivity extends AppCompatActivity implements EditorMapFragment.Callbacks {

    private static final String TAG = makeLogTag(MapActivity.class);

    private TextView mMessageView;
    private EditorMapFragment mMapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.map_editor_act);
        mMessageView = (TextView) findViewById(R.id.map_editor_message);

        // Force a reload of bootstrap data from a local file on the device.
        LocalRefreshingBootstrapService.startDataBootstrap(getApplicationContext());

        // Add the map.
        if (mMapFragment == null) {
            mMapFragment = EditorMapFragment.newInstance();
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container_map_editor, mMapFragment, "map")
                    .commit();
        }

        // Add a checkbox to make map elements draggable.
        CheckBox isDraggableCheckbox = (CheckBox) findViewById(R.id.map_editor_draggable);
        isDraggableCheckbox.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        mMapFragment.setElementsDraggable(isChecked);
                        final String message = isChecked ? "Markers are now draggable."
                                : "Markers are no longer draggable";
                        showMessage(message);
                    }
                });

    }

    @Override
    public void onInfoHide() {
        showMessage(getString(R.string.map_editor_intro));
    }

    @Override
    public void onInfoShowVenue() {
        showMessage("Venue Marker");
    }

    @Override
    public void onInfoShowTitle(String label, int roomType) {
        showMessage(label);
    }

    @Override
    public void onInfoShowSessionlist(String roomId, String roomTitle, int roomType) {
        showMessage(String.format("%s (%s) + session list", roomTitle, roomId));
    }

    @Override
    public void onInfoShowFirstSessionTitle(String roomId, String roomTitle, int roomType) {
        showMessage(String.format("%s (%s) + first session title", roomTitle, roomId));
    }

    @Override
    public void onLogMessage(String message) {
        showMessage(message);
    }

    /**
     * Logs the message as a warning and to the screen.
     */
    private void showMessage(String message) {
        // Log as warning to ensure it is not filtered out.
        LOGW(TAG, message);
        mMessageView.setText(message);
    }
}
