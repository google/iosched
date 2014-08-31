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
package com.google.samples.apps.iosched.sync.userdata.util;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.util.Log;

import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.sync.userdata.UserAction;

import java.util.ArrayList;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class UserActionHelper {
    private static final String TAG = makeLogTag(UserActionHelper.class);

    static public void updateContentProvider(Context context, List<UserAction> userActions, String account) {
        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
        for (UserAction action: userActions) {
            batch.add(createUpdateOperation(context, action, account));
        }
        try {
            context.getContentResolver().applyBatch(ScheduleContract.CONTENT_AUTHORITY, batch);
        } catch (RemoteException e) {
            Log.e(TAG, "Could not apply operations", e);
        } catch (OperationApplicationException e) {
            Log.e(TAG, "Could not apply operations", e);
        }
    }

    static private ContentProviderOperation createUpdateOperation(Context context, UserAction action, String account) {
        if (action.type == UserAction.TYPE.ADD_STAR) {
            return ContentProviderOperation
                    .newInsert(
                            ScheduleContract.addOverrideAccountName(
                                    ScheduleContract.MySchedule.CONTENT_URI, account))
                    .withValue(ScheduleContract.MySchedule.MY_SCHEDULE_DIRTY_FLAG, "0")
                    .withValue(ScheduleContract.MySchedule.SESSION_ID, action.sessionId)
                    .build();
        } else {
            return ContentProviderOperation
                    .newDelete(
                            ScheduleContract.addOverrideAccountName(
                                    ScheduleContract.MySchedule.CONTENT_URI, account))
                    .withSelection(
                            ScheduleContract.MySchedule.SESSION_ID + " = ? AND " +
                            ScheduleContract.MySchedule.MY_SCHEDULE_ACCOUNT_NAME + " = ? ",
                            new String[]{action.sessionId, account}
                    )
                    .build();
        }
    }
}
