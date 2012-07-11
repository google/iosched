/*
 * Copyright 2012 Google Inc.
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

package com.google.analytics.tracking.android;

import android.app.Activity;
import android.content.Context;

/**
 * Temporarily just a stub.
 */
public class EasyTracker {
    public static EasyTracker getTracker() {
        return new EasyTracker();
    }

    public void trackView(String s) {
    }

    public void trackActivityStart(Activity activity) {
    }

    public void trackActivityStop(Activity activity) {
    }

    public void setContext(Context context) {
    }

    public void trackEvent(String s1, String s2, String s3, long l) {
    }

    public void dispatch() {
    }
}
