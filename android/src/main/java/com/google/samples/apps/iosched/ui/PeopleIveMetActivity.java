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

import android.content.Intent;
import android.os.Bundle;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.util.PrefUtils;

public class PeopleIveMetActivity extends BaseActivity {

    private static final String FRAGMENT_PEOPLE_IVE_MET = "people_ive_met";
    public static final int REQUEST_RESOLUTION_FOR_RESULT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.activity_people_ive_met);

        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, PeopleIveMetFragment.newInstance(),
                            FRAGMENT_PEOPLE_IVE_MET)
                    .commit();
        }

        overridePendingTransition(0, 0);
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_PEOPLE_IVE_MET;
    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();
        // If the user is attending remotely, redirect them to 'Explore'
        if (!PrefUtils.isAttendeeAtVenue(this)) {
            startActivity(new Intent(this, BrowseSessionsActivity.class));
            finish();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLUTION_FOR_RESULT) {
            PeopleIveMetFragment fragment = (PeopleIveMetFragment) getFragmentManager()
                    .findFragmentByTag(FRAGMENT_PEOPLE_IVE_MET);
            if (resultCode == RESULT_OK) {
                fragment.retry();
            } else {
                fragment.showApiError();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
