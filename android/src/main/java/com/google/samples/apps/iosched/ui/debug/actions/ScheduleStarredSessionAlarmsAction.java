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

package com.google.samples.apps.iosched.ui.debug.actions;

import android.content.Context;
import android.content.Intent;

import com.google.samples.apps.iosched.service.SessionAlarmService;
import com.google.samples.apps.iosched.ui.debug.DebugAction;

/**
 * Schedules a feedback notification for 1 minute in the future.
 */
public class ScheduleStarredSessionAlarmsAction implements DebugAction {

    @Override
    public void run(Context context, Callback callback) {
        Intent intent = new Intent(
                SessionAlarmService.ACTION_SCHEDULE_ALL_STARRED_BLOCKS,
                null, context, SessionAlarmService.class);
        context.startService(intent);
    }

    @Override
    public String getLabel() {
        return "Schedule session notifications";
    }
}
