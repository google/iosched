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

package com.google.samples.apps.iosched.io;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.io.model.Session;
import com.google.samples.apps.iosched.io.model.Speaker;
import com.google.samples.apps.iosched.io.model.Tag;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.provider.ScheduleContractHelper;
import com.google.samples.apps.iosched.provider.ScheduleDatabase;
import com.google.samples.apps.iosched.util.TimeUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.*;

public class SessionsHandler extends JSONHandler {
    private static final String TAG = makeLogTag(SessionsHandler.class);
    private HashMap<String, Session> mSessions = new HashMap<String, Session>();
    private HashMap<String, Tag> mTagMap = null;
    private HashMap<String, Speaker> mSpeakerMap = null;
    private int mDefaultSessionColor;


    public SessionsHandler(Context context) {
        super(context);
        mDefaultSessionColor = ContextCompat.getColor(mContext, R.color.default_session_color);
    }

    @Override
    public void process(JsonElement element) {
        for (Session session : new Gson().fromJson(element, Session[].class)) {
            mSessions.put(session.id, session);
        }
    }

    @Override
    public void makeContentProviderOperations(ArrayList<ContentProviderOperation> list) {
        Uri uri = ScheduleContractHelper.setUriAsCalledFromSyncAdapter(
                ScheduleContract.Sessions.CONTENT_URI);

        // build a map of session to session import hashcode so we know what to update,
        // what to insert, and what to delete
        HashMap<String, String> sessionHashCodes = loadSessionHashCodes();
        boolean incrementalUpdate = (sessionHashCodes != null) && (sessionHashCodes.size() > 0);

        // set of sessions that we want to keep after the sync
        HashSet<String> sessionsToKeep = new HashSet<String>();

        if (incrementalUpdate) {
            LOGD(TAG, "Doing incremental update for sessions.");
        } else {
            LOGD(TAG, "Doing full (non-incremental) update for sessions.");
            list.add(ContentProviderOperation.newDelete(uri).build());
        }

        int updatedSessions = 0;
        for (Session session : mSessions.values()) {
            // Set the session grouping order in the object, so it can be used in hash calculation
            session.groupingOrder = computeTypeOrder(session);

            // compute the incoming session's hashcode to figure out if we need to update
            String hashCode = session.getImportHashCode();
            sessionsToKeep.add(session.id);

            // add session, if necessary
            if (!incrementalUpdate || !sessionHashCodes.containsKey(session.id) ||
                        !sessionHashCodes.get(session.id).equals(hashCode)) {
                ++updatedSessions;
                boolean isNew = !incrementalUpdate || !sessionHashCodes.containsKey(session.id);
                buildSession(isNew, session, list);

                // add relationships to speakers and track
                buildSessionSpeakerMapping(session, list);
                buildTagsMapping(session, list);
            }
        }

        int deletedSessions = 0;
        if (incrementalUpdate) {
            for (String sessionId : sessionHashCodes.keySet()) {
                if (!sessionsToKeep.contains(sessionId)) {
                    buildDeleteOperation(sessionId, list);
                    ++deletedSessions;
                }
            }
        }

        LOGD(TAG, "Sessions: " + (incrementalUpdate ? "INCREMENTAL" : "FULL") + " update. " +
                updatedSessions + " to update, " + deletedSessions + " to delete. New total: " +
                mSessions.size());
    }

    private void buildDeleteOperation(String sessionId, List<ContentProviderOperation> list) {
        Uri sessionUri = ScheduleContractHelper.setUriAsCalledFromSyncAdapter(
                ScheduleContract.Sessions.buildSessionUri(sessionId));
        list.add(ContentProviderOperation.newDelete(sessionUri).build());
    }

    private HashMap<String, String> loadSessionHashCodes() {
        Uri uri = ScheduleContractHelper.setUriAsCalledFromSyncAdapter(
                ScheduleContract.Sessions.CONTENT_URI);
        LOGD(TAG, "Loading session hashcodes for session import optimization.");
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(uri, SessionHashcodeQuery.PROJECTION,
                    null, null, null);
            if (cursor == null || cursor.getCount() < 1) {
                LOGW(TAG, "Warning: failed to load session hashcodes. Not optimizing session import.");
                return null;
            }
            HashMap<String, String> hashcodeMap = new HashMap<String, String>();
            if (cursor.moveToFirst()) {
                do {
                    String sessionId = cursor.getString(SessionHashcodeQuery.SESSION_ID);
                    String hashcode = cursor.getString(SessionHashcodeQuery.SESSION_IMPORT_HASHCODE);
                    hashcodeMap.put(sessionId, hashcode == null ? "" : hashcode);
                } while (cursor.moveToNext());
            }
            LOGD(TAG, "Session hashcodes loaded for " + hashcodeMap.size() + " sessions.");
            return hashcodeMap;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    StringBuilder mStringBuilder = new StringBuilder();

    private void buildSession(boolean isInsert,
                              Session session, ArrayList<ContentProviderOperation> list) {
        ContentProviderOperation.Builder builder;
        Uri allSessionsUri = ScheduleContractHelper
                .setUriAsCalledFromSyncAdapter(ScheduleContract.Sessions.CONTENT_URI);
        Uri thisSessionUri = ScheduleContractHelper
                .setUriAsCalledFromSyncAdapter(ScheduleContract.Sessions.buildSessionUri(
                        session.id));

        if (isInsert) {
            builder = ContentProviderOperation.newInsert(allSessionsUri);
        } else {
            builder = ContentProviderOperation.newUpdate(thisSessionUri);
        }

        String speakerNames = "";
        if (mSpeakerMap != null) {
            // build human-readable list of speakers
            mStringBuilder.setLength(0);
            if (session.speakers != null) {
                for (int i = 0; i < session.speakers.length; ++i) {
                    if (mSpeakerMap.containsKey(session.speakers[i])) {
                        mStringBuilder
                                .append(i == 0 ? "" :
                                        i == session.speakers.length - 1 ? " and " : ", ")
                                .append(mSpeakerMap.get(session.speakers[i]).name.trim());
                    } else {
                        LOGW(TAG, "Unknown speaker ID " + session.speakers[i] + " in session " +
                                session.id);
                    }
                }
            }
            speakerNames = mStringBuilder.toString();
        } else {
            LOGE(TAG, "Can't build speaker names -- speaker map is null.");
        }

        int color = mDefaultSessionColor;
        try {
            if (!TextUtils.isEmpty(session.color)) {
                color = Color.parseColor(session.color);
            }
        } catch (IllegalArgumentException ex) {
            LOGD(TAG, "Ignoring invalid formatted session color: "+session.color);
        }

        builder.withValue(ScheduleContract.SyncColumns.UPDATED, System.currentTimeMillis())
                .withValue(ScheduleContract.Sessions.SESSION_ID, session.id)
                .withValue(ScheduleContract.Sessions.SESSION_LEVEL, null)            // Not available
                .withValue(ScheduleContract.Sessions.SESSION_TITLE, session.title)
                .withValue(ScheduleContract.Sessions.SESSION_ABSTRACT, session.description)
                .withValue(ScheduleContract.Sessions.SESSION_HASHTAG, session.hashtag)
                .withValue(ScheduleContract.Sessions.SESSION_START, TimeUtils.timestampToMillis(session.startTimestamp, 0))
                .withValue(ScheduleContract.Sessions.SESSION_END, TimeUtils.timestampToMillis(session.endTimestamp, 0))
                .withValue(ScheduleContract.Sessions.SESSION_TAGS, session.makeTagsList())
                        // Note: we store this comma-separated list of tags IN ADDITION
                        // to storing the tags in proper relational format (in the sessions_tags
                        // relationship table). This is because when querying for sessions,
                        // we don't want to incur the performance penalty of having to do a
                        // subquery for every record to figure out the list of tags of each session.
                .withValue(ScheduleContract.Sessions.SESSION_SPEAKER_NAMES, speakerNames)
                        // Note: we store the human-readable list of speakers (which is redundant
                        // with the sessions_speakers relationship table) so that we can
                        // display it easily in lists without having to make an additional DB query
                        // (or another join) for each record.
                .withValue(ScheduleContract.Sessions.SESSION_KEYWORDS, null)             // Not available
                .withValue(ScheduleContract.Sessions.SESSION_URL, session.url)
                .withValue(ScheduleContract.Sessions.SESSION_LIVESTREAM_ID,
                        session.isLivestream ? session.youtubeUrl : null)
                .withValue(ScheduleContract.Sessions.SESSION_MODERATOR_URL, null)    // Not available
                .withValue(ScheduleContract.Sessions.SESSION_REQUIREMENTS, null)     // Not available
                .withValue(ScheduleContract.Sessions.SESSION_YOUTUBE_URL,
                        session.isLivestream ? null : session.youtubeUrl)
                .withValue(ScheduleContract.Sessions.SESSION_PDF_URL, null)          // Not available
                .withValue(ScheduleContract.Sessions.SESSION_NOTES_URL, null)        // Not available
                .withValue(ScheduleContract.Sessions.ROOM_ID, session.room)
                .withValue(ScheduleContract.Sessions.SESSION_GROUPING_ORDER, session.groupingOrder)
                .withValue(ScheduleContract.Sessions.SESSION_IMPORT_HASHCODE,
                        session.getImportHashCode())
                .withValue(ScheduleContract.Sessions.SESSION_MAIN_TAG, session.mainTag)
                .withValue(ScheduleContract.Sessions.SESSION_CAPTIONS_URL, session.captionsUrl)
                .withValue(ScheduleContract.Sessions.SESSION_PHOTO_URL, session.photoUrl)
                // Disabled since this isn't being used by this app.
                // .withValue(ScheduleContract.Sessions.SESSION_RELATED_CONTENT, session.relatedContent)
                .withValue(ScheduleContract.Sessions.SESSION_COLOR, color);
        list.add(builder.build());
    }

    // The type order of a session is the order# (in its category) of the tag that indicates
    // its type. So if we sort sessions by type order, they will be neatly grouped by type,
    // with the types appearing in the order given by the tag category that represents the
    // concept of session type.
    private int computeTypeOrder(Session session) {
        int order = Integer.MAX_VALUE;
        int keynoteOrder = -1;
        if (mTagMap == null) {
            throw new IllegalStateException("Attempt to compute type order without tag map.");
        }

        if (session.tags != null) {
            for (String tagId : session.tags) {
                if (Config.Tags.SPECIAL_KEYNOTE.equals(tagId)) {
                    return keynoteOrder;
                }
                Tag tag = mTagMap.get(tagId);
                if (tag != null && Config.Tags.SESSION_GROUPING_TAG_CATEGORY.equals(tag.category)) {
                    if (tag.order_in_category < order) {
                        order = tag.order_in_category;
                    }
                }
            }
        }
        return order;
    }

    private void buildSessionSpeakerMapping(Session session,
                                            ArrayList<ContentProviderOperation> list) {
        final Uri uri = ScheduleContractHelper.setUriAsCalledFromSyncAdapter(
                ScheduleContract.Sessions.buildSpeakersDirUri(session.id));

        // delete any existing relationship between this session and speakers
        list.add(ContentProviderOperation.newDelete(uri).build());

        // add relationship records to indicate the speakers for this session
        if (session.speakers != null) {
            for (String speakerId : session.speakers) {
                list.add(ContentProviderOperation.newInsert(uri)
                        .withValue(ScheduleDatabase.SessionsSpeakers.SESSION_ID, session.id)
                        .withValue(ScheduleDatabase.SessionsSpeakers.SPEAKER_ID, speakerId)
                        .build());
            }
        }
    }

    private void buildTagsMapping(Session session, ArrayList<ContentProviderOperation> list) {
        final Uri uri = ScheduleContractHelper.setUriAsCalledFromSyncAdapter(
                ScheduleContract.Sessions.buildTagsDirUri(session.id));

        // delete any existing mappings
        list.add(ContentProviderOperation.newDelete(uri).build());

        // add a mapping (a session+tag tuple) for each tag in the session
        if (session.tags != null) {
            for (String tag : session.tags) {
                list.add(ContentProviderOperation.newInsert(uri)
                                                 .withValue(
                                                         ScheduleDatabase.SessionsTags.SESSION_ID,
                                                         session.id)
                                                 .withValue(ScheduleDatabase.SessionsTags.TAG_ID,
                                                         tag).build());
            }
        }
    }

    public void setTagMap(HashMap<String, Tag> tagMap) {
        mTagMap = tagMap;
    }
    public void setSpeakerMap(HashMap<String, Speaker> speakerMap) {
        mSpeakerMap = speakerMap;
    }

    private interface SessionHashcodeQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                ScheduleContract.Sessions.SESSION_ID,
                ScheduleContract.Sessions.SESSION_IMPORT_HASHCODE
        };
        int _ID = 0;
        int SESSION_ID = 1;
        int SESSION_IMPORT_HASHCODE = 2;
    };
}
