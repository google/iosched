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

package com.google.samples.apps.iosched.util;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.TaskStackBuilder;

/**
 * Utility methods for activities management.
 */
public class ActivityUtils {

    /**
     * Enables back navigation for activities that are launched from the NavBar. See {@code
     * AndroidManifest.xml} to find out the parent activity names for each activity.
     *
     * @param intent
     */
    public static void createBackStack(Activity activity, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            TaskStackBuilder builder = TaskStackBuilder.create(activity);
            builder.addNextIntentWithParentStack(intent);
            builder.startActivities();
        } else {
            activity.startActivity(intent);
            activity.finish();
        }
    }


}
