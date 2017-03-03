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

package com.google.samples.apps.iosched.welcome;

import android.content.Context;
import android.view.View;

/**
 * Stubbing out but don't implement until second release.
 * TODO(28358606): Delete this class.
 */
public class AnnouncementFragment extends WelcomeFragment {
    public static final String ANNOUNCEMENT_URL_1 = "https://goo.gl/SsAhv";

    @Override
    public boolean shouldDisplay(final Context context) {
        return false;
    }

    @Override
    protected String getPrimaryButtonText() {
        return null;
    }

    @Override
    protected String getSecondaryButtonText() {
        return "Never";
    }

    @Override
    protected View.OnClickListener getPrimaryButtonListener() {
        return null;
    }

    @Override
    protected View.OnClickListener getSecondaryButtonListener() {
        return null;
    }
}
