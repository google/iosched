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
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.util.PrefUtils;

public class WelcomeActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_welcome);

        findViewById(R.id.button_accept).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PrefUtils.markTosAccepted(WelcomeActivity.this);
                Intent intent = new Intent(WelcomeActivity.this, BrowseSessionsActivity.class);
                startActivity(intent);
                finish();
            }
        });

        findViewById(R.id.button_decline).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        // Shows the debug warning, if this is a debug build and the warning has not been shown yet
        if (Config.IS_DOGFOOD_BUILD && !PrefUtils.wasDebugWarningShown(this)) {
            new AlertDialog.Builder(this)
                    .setTitle(Config.DOGFOOD_BUILD_WARNING_TITLE)
                    .setMessage(Config.DOGFOOD_BUILD_WARNING_TEXT)
                    .setPositiveButton(android.R.string.ok, null).show();
            PrefUtils.markDebugWarningShown(this);
        }
    }
}
