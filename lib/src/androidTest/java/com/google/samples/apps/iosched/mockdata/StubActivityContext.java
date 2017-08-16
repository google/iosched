/*
 * Copyright 2017 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.mockdata;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;

/**
 * An object allowing to use a non Activity context and then to add an Activity context to be used
 * with {@link Context#startActivity(Intent)}.
 */
public class StubActivityContext extends ContextWrapper {

    private Activity mActivity;

    public StubActivityContext(Context context) {
        super(context);
    }

    public void setActivityContext(Activity activity) {
        mActivity = activity;
    }

    @Override
    public void startActivity(Intent intent) {
        if (mActivity != null) {
            mActivity.startActivity(intent);
        }
    }
}
