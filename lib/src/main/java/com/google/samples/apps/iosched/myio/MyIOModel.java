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

package com.google.samples.apps.iosched.myio;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.samples.apps.iosched.model.ScheduleHelper;
import com.google.samples.apps.iosched.myschedule.MyScheduleModel;
import com.google.samples.apps.iosched.util.SessionsHelper;

public class MyIOModel extends MyScheduleModel {

    public MyIOModel(@NonNull final ScheduleHelper scheduleHelper,
            @NonNull final SessionsHelper sessionsHelper,
            @NonNull final Context context) {
        super(scheduleHelper, sessionsHelper, context);
    }

    @Override
    public void deliverUserAction(final MyScheduleUserActionEnum action,
            @Nullable final Bundle args,
            final UserActionCallback<MyScheduleUserActionEnum> callback) {
        switch (action) {
            case SESSION_STAR:
            case SESSION_UNSTAR:
                // We don't want starring or unstarring here
                return;
            default:
                super.deliverUserAction(action, args, callback);
        }
    }
}
