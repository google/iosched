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

import android.os.Bundle;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.util.AnalyticsManager;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class SocialActivity extends BaseActivity {
    private static final String TAG = makeLogTag(SocialActivity.class);
    private static final String SCREEN_LABEL = "Social";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.activity_social);

        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, HashtagsFragment.newInstance())
                    .commit();
        }

        /* [ANALYTICS:SCREEN]
         * TRIGGER:   View the Social screen
         * LABEL:     'Social'
         * [/ANALYTICS]
         */
        AnalyticsManager.sendScreenView(SCREEN_LABEL);
        LOGD("Tracker", SCREEN_LABEL);

        overridePendingTransition(0, 0);
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_SOCIAL;
    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();
    }
}
