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

package com.google.samples.apps.iosched.util;

import android.app.Activity;
import android.app.ActivityManager;
import android.graphics.Bitmap;

import com.google.samples.apps.iosched.lib.R;

/**
 * Helper class that applies the proper icon, title and background color to recent tasks list.
 */
public class RecentTasksStyler {

    private static Bitmap sIcon = null;

    private RecentTasksStyler() { }

    public static void styleRecentTasksEntry(Activity activity) {
        final String label = activity.getString(activity.getApplicationInfo().labelRes);
        final int colorPrimary =
                UIUtils.getThemeColor(activity, R.attr.colorPrimary, R.color.theme_primary);
        if (sIcon == null) {
            // Cache to avoid decoding the same bitmap on every Activity change
            sIcon = UIUtils.drawableToBitmap(activity, R.drawable.ic_recents_logo);
        }
        activity.setTaskDescription(
                new ActivityManager.TaskDescription(label, sIcon, colorPrimary));
    }
}