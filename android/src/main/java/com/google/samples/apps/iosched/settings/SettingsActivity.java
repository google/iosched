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

package com.google.samples.apps.iosched.settings;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.google.samples.apps.iosched.BuildConfig;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.messaging.MessagingRegistrationWithGCM;
import com.google.samples.apps.iosched.navigation.NavigationModel;
import com.google.samples.apps.iosched.service.SessionCalendarService;
import com.google.samples.apps.iosched.ui.BaseActivity;
import com.google.samples.apps.iosched.ui.widget.DrawShadowFrameLayout;
import com.google.samples.apps.iosched.util.PermissionsUtils;
import com.google.samples.apps.iosched.util.UIUtils;

/**
 * Activity for customizing app settings.
 */
public class SettingsActivity extends BaseActivity {
    public static final String[] CALENDAR_PERMISSIONS =
            new String[]{Manifest.permission.WRITE_CALENDAR};
    /**
     * Indicates a permission request because the Calendar sync feature is being turned off. The
     * Calendar needs to be cleared and therefore the access is needed to remove entries.
     */
    private static final int REQUEST_PERMISSION_REQUEST_CODE_DISABLE_CALENDAR = 222;
    /**
     * Indicates a permission request because the Calendar sync feature is being turned on.
     */
    private static final int REQUEST_PERMISSION_REQUEST_CODE_ENABLE_CALENDAR = 111;

    @Override
    protected NavigationModel.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationModel.NavigationItemEnum.SETTINGS;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_act);
        overridePendingTransition(0, 0);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
            @NonNull final String[] permissions,
            @NonNull final int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // The only permission that is granted is the calendar one so a full sync is needed
            // when the permission is granted. This clears the schedule when sync is turned off
            // and updates it when it is turned on.
            scheduleCalendarSync(this);
        } else {
            int messageResId = (requestCode == REQUEST_PERMISSION_REQUEST_CODE_DISABLE_CALENDAR ?
                    R.string.pref_sync_with_calendar_permission_denied_during_disable :
                    R.string.pref_sync_with_calendar_permission_denied_during_enable);
            PermissionsUtils.displayPermissionDeniedSnackbar(this, messageResId);
        }
    }

    static void scheduleCalendarSync(Activity activity) {
        Intent intent;
        if (SettingsUtils.shouldSyncCalendar(activity)) {
            // Add all calendar entries
            intent = new Intent(SessionCalendarService.ACTION_UPDATE_ALL_SESSIONS_CALENDAR);
        } else {
            // Remove all calendar entries
            intent = new Intent(SessionCalendarService.ACTION_CLEAR_ALL_SESSIONS_CALENDAR);
        }

        intent.setClass(activity, SessionCalendarService.class);
        activity.startService(intent);
    }

    /**
     * The Fragment is added via the R.layout.settings_act layout xml.
     */
    public static class SettingsFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        public SettingsFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            SettingsUtils.registerOnSharedPreferenceChangeListener(getActivity(), this);
        }

        @Override
        public RecyclerView onCreateRecyclerView(final LayoutInflater inflater,
                final ViewGroup parent,
                final Bundle savedInstanceState) {
            // Override the default list which has horizontal padding. Instead place padding on
            // the preference items for nicer touch feedback.
            final RecyclerView prefList =
                    (RecyclerView) inflater.inflate(R.layout.settings_list, parent, false);
            prefList.setHasFixedSize(true);
            return prefList;
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            addPreferencesFromResource(R.xml.settings_prefs);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            SettingsUtils.unregisterOnSharedPreferenceChangeListener(getActivity(), this);
        }

        @Override
        public void onResume() {
            super.onResume();

            // configure the fragment's top clearance to take our overlaid controls (Action Bar
            // and spinner box) into account.
            int actionBarSize = UIUtils.calculateActionBarSize(getActivity());
            DrawShadowFrameLayout drawShadowFrameLayout =
                    (DrawShadowFrameLayout) getActivity().findViewById(R.id.main_content);
            if (drawShadowFrameLayout != null) {
                drawShadowFrameLayout.setShadowTopOffset(actionBarSize);
            }
            setContentTopClearance(actionBarSize);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            // The Calendar Sync requires checking the Calendar permission.
            if (SettingsUtils.PREF_SYNC_CALENDAR.equals(key)) {
                // Request permission when it doesn't exist, saving the information about whether
                // the request enabled or disabled the sync via the requestCode.
                if (!PermissionsUtils.permissionsAlreadyGranted(getActivity(), CALENDAR_PERMISSIONS)) {
                    boolean shouldSync = SettingsUtils.shouldSyncCalendar(getActivity());
                    int requestCode = shouldSync ? REQUEST_PERMISSION_REQUEST_CODE_ENABLE_CALENDAR :
                            REQUEST_PERMISSION_REQUEST_CODE_DISABLE_CALENDAR;
                    ActivityCompat.requestPermissions(getActivity(),
                            CALENDAR_PERMISSIONS, requestCode);
                    return;
                }
                scheduleCalendarSync(getActivity());
            } else if (BuildConfig.PREF_CONF_MESSAGES_ENABLED.equals(key) ||
                    BuildConfig.PREF_ATTENDEE_AT_VENUE.equals(key)) {
                // This will activate re-registering with the correct GCM topic(s).
                new MessagingRegistrationWithGCM(getActivity()).registerDevice();

            }
        }

        private void setContentTopClearance(int clearance) {
            if (getView() != null) {
                getView().setPadding(getView().getPaddingLeft(), clearance,
                        getView().getPaddingRight(), getView().getPaddingBottom());
            }
        }
    }
}
