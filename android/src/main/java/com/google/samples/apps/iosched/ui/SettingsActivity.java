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

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v4.content.IntentCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.service.SessionCalendarService;
import com.google.samples.apps.iosched.util.PrefUtils;

/**
 * Activity for customizing app settings.
 */
public class SettingsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = getActionBarToolbar();
        toolbar.setTitle(R.string.title_settings);
        toolbar.setNavigationIcon(R.drawable.ic_up);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateUpToFromChild(SettingsActivity.this,
                        IntentCompat.makeMainActivity(new ComponentName(SettingsActivity.this,
                                BrowseSessionsActivity.class)));
            }
        });

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new SettingsFragment())
                    .commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        public SettingsFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setupSimplePreferencesScreen();
            PrefUtils.registerOnSharedPreferenceChangeListener(getActivity(), this);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            PrefUtils.unregisterOnSharedPreferenceChangeListener(getActivity(), this);
        }

        private void setupSimplePreferencesScreen() {
            // Add 'general' preferences.
            addPreferencesFromResource(R.xml.preferences);
            if (PrefUtils.hasEnabledBle(getActivity())) {
                addPreferencesFromResource(R.xml.ble_preferences);
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (PrefUtils.PREF_SYNC_CALENDAR.equals(key)) {
                Intent intent;
                if (PrefUtils.shouldSyncCalendar(getActivity())) {
                    // Add all calendar entries
                    intent = new Intent(SessionCalendarService.ACTION_UPDATE_ALL_SESSIONS_CALENDAR);
                } else {
                    // Remove all calendar entries
                    intent = new Intent(SessionCalendarService.ACTION_CLEAR_ALL_SESSIONS_CALENDAR);
                }

                intent.setClass(getActivity(), SessionCalendarService.class);
                getActivity().startService(intent);
            }
        }
    }
}
