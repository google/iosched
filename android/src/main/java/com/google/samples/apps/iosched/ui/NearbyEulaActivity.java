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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.util.PrefUtils;

/**
 * An activity handling the display of the BLE legal agreement.
 */
public class NearbyEulaActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_nearby_eula);

        findViewById(R.id.button_decline).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PrefUtils.setBleStatus(NearbyEulaActivity.this, false);
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        findViewById(R.id.button_accept).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PrefUtils.setBleStatus(NearbyEulaActivity.this, true);
                setResult(RESULT_OK);
                finish();
            }
        });

    }
}
