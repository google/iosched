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

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.google.samples.apps.iosched.appwidget.ScheduleWidgetProvider;
import com.google.samples.apps.iosched.gcm.ServerUtilities;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.provider.ScheduleContract.MySchedule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;


/**
 * Helper class that syncs starred sessions data in a Drive's AppData folder.
 *
 * Protocode:
 *
 *   // when user clicks on "star":
 *   session UI: run updateSession()
 *   this.updateSession():
 *     send addstar/removestar to contentProvider
 *     send broadcast to update any dependent UI
 *     save user actions as pending in shared preferences
 *
 *   // on sync
 *   syncadapter: call this.sync()
 *   this.sync():
 *     fetch remote content
 *     if pending actions:
 *       apply to content and update remote
 *     if modified content != last synced content:
 *       update contentProvider
 *       send broadcast to update any dependent UI
 *
 *
 */
public abstract class AbstractUserDataSyncHelper {
    private static final String TAG = makeLogTag(AbstractUserDataSyncHelper.class);

    protected Context mContext;
    protected String mAccountName;

    public AbstractUserDataSyncHelper(Context context, String accountName) {
        this.mContext = context;
        this.mAccountName = accountName;
    }

    protected abstract boolean syncImpl(List<UserAction> actions, boolean hasPendingLocalData);

    /**
     * Create a copy of current pending actions and delegate the
     * proper sync'ing to the concrete subclass on the method syncImpl.
     *
     */
    public boolean sync() {
        // get data pending sync:
        Cursor scheduleData = mContext.getContentResolver().query(
                MySchedule.buildMyScheduleUri(mContext, mAccountName), MyScheduleQuery.PROJECTION,
                null, null, null);

        if (scheduleData == null) {
            return false;
        }

        // Although we have a dirty flag per item, we need all schedule to sync, because it's all
        // sync'ed at once to a file on AppData folder. We only use the dirty flag to decide if
        // the local content was changed or not. If it was, we replace the remote content.
        boolean hasPendingLocalData = false;
        ArrayList<UserAction> actions = new ArrayList<UserAction>();
        while (scheduleData.moveToNext()) {

            UserAction userAction = new UserAction();
            userAction.sessionId = scheduleData.getString(MyScheduleQuery.SESSION_ID);
            Integer inSchedule = scheduleData.getInt(MyScheduleQuery.IN_SCHEDULE);
            if (inSchedule == 0) {
                userAction.type = UserAction.TYPE.REMOVE_STAR;
            } else {
                userAction.type = UserAction.TYPE.ADD_STAR;
            }
            userAction.requiresSync = scheduleData.getInt(MyScheduleQuery.DIRTY_FLAG) == 1;
            actions.add(userAction);
            if (!hasPendingLocalData && userAction.requiresSync) {
                hasPendingLocalData = true;
            }
        }
        scheduleData.close();

        Log.d(TAG, "Starting Drive AppData sync. hasPendingData = " + hasPendingLocalData);

        boolean dataChanged = syncImpl(actions, hasPendingLocalData);

        if (hasPendingLocalData) {
            resetDirtyFlag(actions);

            // Notify other devices via GCM
            ServerUtilities.notifyUserDataChanged(mContext);
        }
        if (dataChanged) {
            LOGD(TAG, "Notifying changes on paths related to user data on Content Resolver.");
            ContentResolver resolver = mContext.getContentResolver();
            for (String path : ScheduleContract.USER_DATA_RELATED_PATHS) {
                Uri uri = ScheduleContract.BASE_CONTENT_URI.buildUpon().appendPath(path).build();
                resolver.notifyChange(uri, null);
            }
            mContext.sendBroadcast(ScheduleWidgetProvider.getRefreshBroadcastIntent(mContext, false));
        }
        return dataChanged;
    }

    private void resetDirtyFlag(ArrayList<UserAction> actions) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        for (UserAction action: actions) {
            ContentProviderOperation op = ContentProviderOperation.newUpdate(
                    ScheduleContract.addCallerIsSyncAdapterParameter(
                            MySchedule.buildMyScheduleUri(mContext, mAccountName)))
                    .withSelection(MySchedule.SESSION_ID + "=? AND " +
                                    MySchedule.MY_SCHEDULE_IN_SCHEDULE + "=?",
                            new String[]{action.sessionId,
                                    action.type == UserAction.TYPE.ADD_STAR ? "1" : "0"})
                    .withValue(MySchedule.MY_SCHEDULE_DIRTY_FLAG, 0)
                    .build();
            LOGD(TAG, op.toString());
            ops.add(op);
        }
        try {
            ContentProviderResult[] result = mContext.getContentResolver().applyBatch(
                    ScheduleContract.CONTENT_AUTHORITY, ops);
            LOGD(TAG, "Result of cleaning dirty flags is "+ Arrays.toString(result));
        } catch (Exception ex) {
            LOGW(TAG, "Could not update dirty flags. Ignoring.", ex);
        }
    }

    private interface MyScheduleQuery {

        String[] PROJECTION = {
                MySchedule.SESSION_ID,
                MySchedule.MY_SCHEDULE_IN_SCHEDULE,
                MySchedule.MY_SCHEDULE_DIRTY_FLAG,
        };

        int SESSION_ID = 0;
        int IN_SCHEDULE= 1;
        int DIRTY_FLAG = 2;
    }

}