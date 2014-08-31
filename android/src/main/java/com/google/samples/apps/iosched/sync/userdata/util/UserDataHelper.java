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

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.google.api.client.util.Charsets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.sync.userdata.UserAction;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class UserDataHelper {

    private static final String TAG = makeLogTag(UserDataHelper.class);

    // Constants related to the JSON serialization:
    static final String JSON_STARRED_SESSIONS_KEY = "starred_sessions";

    static public String toSessionsString(Set<String> sessionIds) {
        JsonArray array = new JsonArray();
        for (String sessionId: sessionIds) {
            array.add(new JsonPrimitive(sessionId));
        }
        JsonObject obj = new JsonObject();
        obj.add(JSON_STARRED_SESSIONS_KEY, array);
        return obj.toString();
    }

    static public byte[] toByteArray(Set<String> sessionIds) {
        return toSessionsString(sessionIds).getBytes(Charsets.UTF_8);
    }

    static public Set<String> fromString(String str) {
        TreeSet<String> result = new TreeSet<String>();
        if (str == null || str.isEmpty()) {
            return result;
        }
        try {
            JsonReader reader = new JsonReader(new StringReader(str));
            reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName();
                if (JSON_STARRED_SESSIONS_KEY.equals(key)) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        result.add(reader.nextString());
                    }
                    reader.endArray();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            reader.close();
        } catch (Exception ex) {
            Log.w(TAG, "Ignoring invalid remote content.", ex);
            return null;
        }
        return result;
    }

    static public Set<String> getLocalStarredSessionIDs(Context context) {
        Cursor sessionsCursor = context.getContentResolver().query(
                ScheduleContract.MySchedule.CONTENT_URI,
                new String[]{ScheduleContract.MySchedule.SESSION_ID}, null, null, null);
        Set<String> starredSessions = new HashSet<String>();
        while (sessionsCursor.moveToNext()) {
            starredSessions.add(sessionsCursor.getString(0));
        }
        sessionsCursor.close();
        return starredSessions;
    }

    static public void setLocalStarredSessions(Context context, Set<String> sessionIds, String accountName) {
        // first clear all stars
        context.getContentResolver().delete(ScheduleContract.MySchedule.CONTENT_URI,
                ScheduleContract.MySchedule.MY_SCHEDULE_ACCOUNT_NAME +" = ?", new String[]{accountName});

        // now add only the ones in sessionIds
        ArrayList<UserAction> actions = new ArrayList<UserAction>();
        for (String sessionId: sessionIds) {
            UserAction action = new UserAction();
            action.type = UserAction.TYPE.ADD_STAR;
            action.sessionId = sessionId;
            actions.add(action);
        }
        UserActionHelper.updateContentProvider(context, actions, accountName);
    }
}
