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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.service.SessionCalendarService;
import com.google.samples.apps.iosched.util.PrefUtils;

/**
 * Activity for customizing app settings.
 */
public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupSimplePreferencesScreen();
        PrefUtils.registerOnSharedPreferenceChangeListener(this, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PrefUtils.unrgisterOnSharedPreferenceChangeListener(this, this);
    }

    private void setupSimplePreferencesScreen() {
        // Add 'general' preferences.
        addPreferencesFromResource(R.xml.preferences);
        if (PrefUtils.hasEnabledBle(this)) {
            addPreferencesFromResource(R.xml.ble_preferences);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (PrefUtils.PREF_SYNC_CALENDAR.equals(key)) {
            boolean shouldSyncCalendar = PrefUtils.shouldSyncCalendar(this);

            Intent intent;
            if (PrefUtils.shouldSyncCalendar(this)) {
                // Add all calendar entries
                intent = new Intent(SessionCalendarService.ACTION_UPDATE_ALL_SESSIONS_CALENDAR);
            } else {
                // Remove all calendar entries
                intent = new Intent(SessionCalendarService.ACTION_CLEAR_ALL_SESSIONS_CALENDAR);
            }

            intent.setClass(this, SessionCalendarService.class);
            startService(intent);
        }
    }
}
