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

package com.google.samples.apps.iosched.testutils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.util.TimeUtils;

import org.mockito.BDDMockito;
import org.powermock.api.mockito.PowerMockito;

import static org.mockito.Mockito.when;

public class SettingsMockContext {

    /**
     * Test classes calling this must use {@code @RunWith(PowerMockRunner.class)} and {@code
     * @PrepareForTest(PreferenceManager.class)}.
     */
    public static void initMockContextForAttendingVenueSetting(boolean attending, Context context,
            SharedPreferences prefs) {
        PowerMockito.mockStatic(PreferenceManager.class);
        BDDMockito.given(PreferenceManager.getDefaultSharedPreferences(context)).willReturn(prefs);
        when(context.getPackageName()).thenReturn("NAME");
        when(prefs.getBoolean(SettingsUtils.PREF_ATTENDEE_AT_VENUE, true)).thenReturn(attending);
    }

    /**
     * Test classes calling this must use {@code @RunWith(PowerMockRunner.class)} and {@code
     * @PrepareForTest(TimeUtils.class)}.
     */
    public static void initMockContextForCurrentTime(Long currentTime, Context context) {
        PowerMockito.mockStatic(TimeUtils.class);
        BDDMockito.given(TimeUtils.getCurrentTime(context)).willReturn(currentTime);
    }
}
