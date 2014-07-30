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

package com.google.samples.apps.iosched.sync.userdata;

import android.content.Context;

import com.google.samples.apps.iosched.sync.userdata.http.HTTPUserDataSyncHelper;


/**
 * A simple factory to isolate the decision of which synchelper should be used.
 *
 * Currently, the HTTP sync helper is always returned, because of the early stage of
 * the GMS version. In the future, this can be changed.
 *
**/

public class UserDataSyncHelperFactory {
    public static AbstractUserDataSyncHelper buildSyncHelper(Context context, String accountName) {
        return new HTTPUserDataSyncHelper(context, accountName);
    }
}