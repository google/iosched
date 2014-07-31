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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.TaskStackBuilder;

/**
 * Helper 'proxy' activity that simply accepts an activity intent and synthesize a back-stack
 * for it, per Android's design guidelines for navigation from widgets and notifications.
 */
public class TaskStackBuilderProxyActivity extends Activity {
    private static final String EXTRA_INTENTS = "com.google.samples.apps.iosched.extra.INTENTS";

    public static Intent getTemplate(Context context) {
        return new Intent(context, TaskStackBuilderProxyActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    public static Intent getFillIntent(Intent... intents) {
        return new Intent().putExtra(EXTRA_INTENTS, intents);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TaskStackBuilder builder = TaskStackBuilder.create(this);
        Intent proxyIntent = getIntent();
        if (!proxyIntent.hasExtra(EXTRA_INTENTS)) {
            finish();
            return;
        }

        for (Parcelable parcelable : proxyIntent.getParcelableArrayExtra(EXTRA_INTENTS)) {
            builder.addNextIntent((Intent) parcelable);
        }

        builder.startActivities();
        finish();
    }
}
