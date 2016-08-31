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
import android.text.TextUtils;
import android.text.format.Time;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.io.model.Session;
import com.google.samples.apps.iosched.io.model.Speaker;
import com.google.samples.apps.iosched.io.model.Tag;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.provider.ScheduleContractHelper;
import com.google.samples.apps.iosched.provider.ScheduleDatabase;
import com.google.samples.apps.iosched.sync.SyncHelper;
import com.google.samples.apps.iosched.util.ParserUtils;
import com.google.samples.apps.iosched.util.TimeUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import no.java.schedule.v2.R;
import no.java.schedule.v2.io.model.Constants;
import no.java.schedule.v2.io.model.EMSItem;
import no.java.schedule.v2.io.model.EMSLink;
import no.java.schedule.v2.io.model.JZDate;
import no.java.schedule.v2.io.model.JZLabel;
import no.java.schedule.v2.io.model.JZSessionsResponse;
import no.java.schedule.v2.io.model.JZSessionsResult;
import no.java.schedule.v2.io.model.JZSlotsResponse;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.LOGI;
import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class SessionsHandler extends JSONHandler {
    private static final String TAG = makeLogTag(SessionsHandler.class);
    private HashMap<String, Session> mSessions = new HashMap<String, Session>();
    private HashMap<String, Tag> mTagMap = null;
    private HashMap<String, Speaker> mSpeakerMap = null;
    private int mDefaultSessionColor;

    private static final String EVENT_TYPE_KEYNOTE = "keynote";
    private static final String EVENT_TYPE_CODELAB = "codelab";

    private static final int PARSE_FLAG_FORCE_SCHEDULE_REMOVE = 1;
    private static final int PARSE_FLAG_FORCE_SCHEDULE_ADD = 2;

    private static final Time sTime = new Time();

    private static final Pattern sRemoveSpeakerIdPrefixPattern = Pattern.compile(".*//");

    private boolean mLocal;
    private boolean mThrowIfNoAuthToken;
    private Map<String, EMSItem> mBlocks;
    private Map<String, EMSItem> mRooms;

    public SessionsHandler(Context context, boolean local, boolean throwIfNoAuthToken) {
        super(context);
        mDefaultSessionColor = mContext.getResources().getColor(R.color.default_session_color);
        mLocal = local;
        mThrowIfNoAuthToken = throwIfNoAuthToken;
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
            for (int i = 0; i < session.speakers.length; ++i) {
                if (mSpeakerMap.containsKey(session.speakers[i])) {
                    mStringBuilder
                            .append(i == 0 ? "" : i == session.speakers.length - 1 ? " and " : ", ")
                            .append(mSpeakerMap.get(session.speakers[i]).name.trim());
                } else {
                    LOGW(TAG, "Unknown speaker ID " + session.speakers[i] + " in session " + session.id);
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
            LOGD(TAG, "Ignoring invalid formatted session color: " + session.color);
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
                .withValue(ScheduleContract.Sessions.SESSION_SPEAKER_NAMES, speakerNames)
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
                .withValue(ScheduleContract.Sessions.SESSION_STARRED, session.starred)
                // Disabled since this isn't being used by this app.
                // .withValue(ScheduleContract.Sessions.SESSION_RELATED_CONTENT, session.relatedContent)
                .withValue(ScheduleContract.Sessions.SESSION_COLOR, color);
        list.add(builder.build());
    }

    private int computeTypeOrder(Session session) {
        int order = Integer.MAX_VALUE;
        int keynoteOrder = -1;
        if (mTagMap == null) {
            throw new IllegalStateException("Attempt to compute type order without tag map.");
        }
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
        for (String tag : session.tags) {
            list.add(ContentProviderOperation.newInsert(uri)
                    .withValue(ScheduleDatabase.SessionsTags.SESSION_ID, session.id)
                    .withValue(ScheduleDatabase.SessionsTags.TAG_ID, tag).build());
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
    }

    ;

    public ArrayList<ContentProviderOperation> parse(String json)
            throws IOException {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

        JZSessionsResponse collectionResponse = new Gson().fromJson(json, JZSessionsResponse.class);


        LOGI(TAG, "Updating sessions data");

        // by default retain locally starred if local sync
        boolean retainLocallyStarredSessions = true; //mLocal;

        if (collectionResponse.error != null && collectionResponse.error.isJsonPrimitive()) {
            String errorMessageLower = collectionResponse.error.getAsString().toLowerCase();

            if (!mLocal && (errorMessageLower.contains("no profile")
                    || errorMessageLower.contains("no auth token"))) {
                // There was some authentication issue; retain locally starred sessions.
                retainLocallyStarredSessions = true;
                LOGW(TAG, "The user has no developers.google.com profile or this call is "
                        + "not authenticated. Retaining locally starred sessions.");
            }

            if (mThrowIfNoAuthToken && errorMessageLower.contains("no auth token")) {
                throw new HandlerException.UnauthorizedException("No auth token but we tried "
                        + "authenticating. Need to invalidate the auth token.");
            }
        }

        Set<String> starredSessionIds = new HashSet<String>();
        // Collect the list of current starred sessions
        Cursor starredSessionsCursor = mContext.getContentResolver().query(
                ScheduleContract.Sessions.CONTENT_STARRED_URI,
                new String[]{ScheduleContract.Sessions.SESSION_ID},
                null, null, null);
        while (starredSessionsCursor.moveToNext()) {
            starredSessionIds.add(starredSessionsCursor.getString(0));
            LOGD(TAG, "session that has been starred was added here!");
        }
        starredSessionsCursor.close();

        // Clear out existing sessions
        batch.add(ContentProviderOperation
                .newDelete(ScheduleContract.addCallerIsSyncAdapterParameter(
                        ScheduleContract.Sessions.CONTENT_URI))
                .build());

        // Maintain a list of created block IDs
        Set<String> blockIds = new HashSet<String>();

        List<JZSessionsResult> sessions = toJZSessionResultList(collectionResponse.collection.items);

        for (JZSessionsResult event : sessions) {

            int flags = 0;
            boolean starred = starredSessionIds.contains(event.id);
            if (retainLocallyStarredSessions) {
                flags = (starredSessionIds.contains(event.id)
                        ? PARSE_FLAG_FORCE_SCHEDULE_ADD
                        : PARSE_FLAG_FORCE_SCHEDULE_REMOVE);
            }

            String sessionId = event.id;
            if (TextUtils.isEmpty(sessionId)) {
                LOGW(TAG, "Found session with empty ID in API response.");
                continue;
            }

            // Session title  - fix special titles
            String sessionTitle = event.title;

            // Whether or not it's in the schedule
            boolean inSchedule = "Y".equals(event.attending);
            if ((flags & PARSE_FLAG_FORCE_SCHEDULE_ADD) != 0
                    || (flags & PARSE_FLAG_FORCE_SCHEDULE_REMOVE) != 0) {
                inSchedule = (flags & PARSE_FLAG_FORCE_SCHEDULE_ADD) != 0;
            }

            String hashtags = "";
            String prereqs = "";

            String youtubeUrl = null;

            populateStartEndTime(event);
            parseSpeakers(event, batch);
            //populateRoom(event);

            long sessionStartTime = 0;
            long sessionEndTime = 0;      //TODO handle sessions without timeslot

            long originalSessionEndTime = 1;
            long originalSessionStartTime = 1;


            if (event.start != null && event.end != null) {
                originalSessionStartTime = event.start.millis();
                originalSessionEndTime = event.end.millis();

                sessionStartTime = event.start.millis();//parseTime(event.start_date, event.start_time);
                sessionEndTime = event.end.millis();//event.end_date, event.end_time);
            }

            if (Constants.LIGHTNINGTALK.equals(event.format)) {
                sessionStartTime = snapStartTime(sessionStartTime);
                sessionEndTime = snapEndTime(sessionEndTime);

                if ((sessionEndTime - sessionStartTime) > 1000 * 60 * 61) {
                    sessionEndTime = sessionStartTime + 1000 * 60 * 60;
                }
            }

            if (!Constants.WORKSHOP.equals(event.format)
                    && event.state.equals(Constants.SESSION_STATE_APPROVED)) {
                int color = mDefaultSessionColor;

                // Insert session info
                final ContentProviderOperation.Builder builder = ContentProviderOperation
                        .newInsert(ScheduleContract
                                .addCallerIsSyncAdapterParameter(ScheduleContract.Sessions.CONTENT_URI))
                        .withValue(ScheduleContract.SyncColumns.UPDATED, System.currentTimeMillis())
                        .withValue(ScheduleContract.Sessions.SESSION_ID, sessionId)
                        .withValue(ScheduleContract.Sessions.SESSION_LEVEL, null)            // Not available
                        .withValue(ScheduleContract.Sessions.SESSION_TITLE, sessionTitle)
                        .withValue(ScheduleContract.Sessions.SESSION_ABSTRACT, event.bodyHtml)
                        .withValue(ScheduleContract.Sessions.SESSION_AUDIENCE, event.audience)
                        .withValue(ScheduleContract.Sessions.SESSION_START, originalSessionStartTime)
                        .withValue(ScheduleContract.Sessions.SESSION_END, originalSessionEndTime)
                        .withValue(ScheduleContract.Sessions.SESSION_TAGS, event.labelstrings())
                        .withValue(ScheduleContract.Sessions.SESSION_FORMAT, event.format)
                        // .withValue(ScheduleContract.Sessions.SESSION_SPEAKER_NAMES, speakerNames)
                        .withValue(ScheduleContract.Sessions.SESSION_KEYWORDS, null)             // Not available
                        .withValue(ScheduleContract.Sessions.SESSION_URL, String.valueOf(event.sessionHtmlUrl))
                        .withValue(ScheduleContract.Sessions.SESSION_LIVESTREAM_ID, "")
                        .withValue(ScheduleContract.Sessions.SESSION_MODERATOR_URL, null)    // Not available
                        .withValue(ScheduleContract.Sessions.SESSION_REQUIREMENTS, null)     // Not available
                        .withValue(ScheduleContract.Sessions.SESSION_YOUTUBE_URL, youtubeUrl)
                        .withValue(ScheduleContract.Sessions.SESSION_PDF_URL, null)          // Not available
                        .withValue(ScheduleContract.Sessions.SESSION_NOTES_URL, null)        // Not available
                        .withValue(ScheduleContract.Sessions.SESSION_STARRED, starred)
                        .withValue(ScheduleContract.Sessions.ROOM_ID, ParserUtils.sanitizeId(event.room))
                        .withValue(ScheduleContract.Sessions.SESSION_COLOR, color);


                long blockStart = snapStartTime(sessionStartTime);
                long blockEnd = snapEndTime(sessionEndTime);

                String blockId = ScheduleContract.Blocks.generateBlockId(blockStart, blockEnd);
                if (blockId != null && !blockIds.contains(blockId)) { // TODO add support for fetching blocks and inserting
                    String blockType;
                    String blockTitle;
                    if (EVENT_TYPE_KEYNOTE.equals(event.format)) {
                        blockType = ParserUtils.BLOCK_TYPE_KEYNOTE;
                        blockTitle = mContext.getString(R.string.schedule_block_title_keynote);
                    } else if (EVENT_TYPE_CODELAB.equals(event.format)) {
                        blockType = ParserUtils.BLOCK_TYPE_CODE_LAB;
                        blockTitle = mContext
                                .getString(R.string.schedule_block_title_code_labs);
                    } else {
                        blockType = ParserUtils.BLOCK_TYPE_SESSION;
                        blockTitle = mContext.getString(R.string.schedule_block_title_sessions);
                    }

                    batch.add(ContentProviderOperation
                            .newInsert(ScheduleContract.Blocks.CONTENT_URI)
                            .withValue(ScheduleContract.Blocks.BLOCK_ID, blockId)
                            .withValue(ScheduleContract.Blocks.BLOCK_TYPE, blockType)
                            .withValue(ScheduleContract.Blocks.BLOCK_TITLE, blockTitle)
                            .withValue(ScheduleContract.Blocks.BLOCK_START, sessionStartTime)
                            .withValue(ScheduleContract.Blocks.BLOCK_END, sessionEndTime)
                            .build());
                    blockIds.add(blockId);
                }

                builder.withValue(ScheduleContract.Sessions.BLOCK_ID, blockId);
                batch.add(builder.build());

                // Replace all session speakers
                final Uri sessionSpeakersUri = ScheduleContract.Sessions.buildSpeakersDirUri(sessionId);
                batch.add(ContentProviderOperation
                        .newDelete(ScheduleContract
                                .addCallerIsSyncAdapterParameter(sessionSpeakersUri))
                        .build());
                if (event.speakers != null) {
                    for (String speakerId : event.speakers) {
                        batch.add(ContentProviderOperation.newInsert(sessionSpeakersUri)
                                .withValue(ScheduleDatabase.SessionsSpeakers.SESSION_ID, sessionId)
                                .withValue(ScheduleDatabase.SessionsSpeakers.SPEAKER_ID, speakerId).build());
                    }
                }

                // Replace all session tracks
                final Uri sessionTracksUri = ScheduleContract.addCallerIsSyncAdapterParameter(
                        ScheduleContract.Sessions.buildTracksDirUri(sessionId));
                batch.add(ContentProviderOperation.newDelete(sessionTracksUri).build());
                if (event.labels != null) {
                    for (JZLabel trackName : event.labels) {
                        String trackId = ScheduleContract.Tracks.generateTrackId(trackName.id);
                        batch.add(ContentProviderOperation.newInsert(sessionTracksUri)
                                .withValue(ScheduleDatabase.SessionsTracks.SESSION_ID, sessionId)
                                .withValue(ScheduleDatabase.SessionsTracks.TRACK_ID, trackId).build());
                    }
                }

                final Uri tagUri = ScheduleContractHelper.setUriAsCalledFromSyncAdapter(
                        ScheduleContract.Tags.CONTENT_URI);
                if (event.labelstrings() != null) {
                    String[] tagIds = event.labelstrings().split(",");
                    for (String tag : tagIds) {
                        batch.add(ContentProviderOperation.newInsert(tagUri)
                                .withValue(ScheduleContract.Tags.TAG_CATEGORY,
                                        tag.startsWith("topic:") ? "TOPIC" :
                                                tag.startsWith("type:") ? "TYPE"
                                                        : "THEME")
                                .withValue(ScheduleContract.Tags.TAG_NAME, tag).build());
                    }
                }
            }
        }
        return batch;
    }

    private void parseSpeakers(final JZSessionsResult pEvent, final ArrayList<ContentProviderOperation> pBatch) {
        Map<String, EMSItem> speakers = null;
        for (EMSLink speakerLink : pEvent.speakerList) {
            speakers = load(speakerLink.href);

            for (EMSItem speaker : speakers.values()) {
                pBatch.add(ContentProviderOperation
                        .newInsert(ScheduleContract
                                .addCallerIsSyncAdapterParameter(ScheduleContract.Speakers.CONTENT_URI))
                        .withValue(ScheduleContract.SyncColumns.UPDATED, System.currentTimeMillis())
                        .withValue(ScheduleContract.Speakers.SPEAKER_ID, speaker.href.toString())
                        .withValue(ScheduleContract.Speakers.SPEAKER_NAME, speaker.getValue("name"))
                        .withValue(ScheduleContract.Speakers.SPEAKER_ABSTRACT, speaker.getValue("bio"))
                        .withValue(ScheduleContract.Speakers.SPEAKER_IMAGE_URL, speaker.getLinkHref("photo"))
                        .withValue(ScheduleContract.Speakers.SPEAKER_URL, "")//TODO
                        .build());
            }


            if (speakers != null) {
                if(pEvent.speakers == null) {
                    pEvent.speakers = new HashSet<String>();
                }

                pEvent.speakers.addAll(speakers.keySet());
            }
        }
    }

    private void populateStartEndTime(final JZSessionsResult pEvent) {

        if (mBlocks == null) {
            mBlocks = loadBlocks();
        }

        EMSItem timeslot = mBlocks.get(pEvent.timeslot);
        if (timeslot != null) {
            pEvent.start = new JZDate(timeslot.getValue("start"));
            pEvent.end = new JZDate(timeslot.getValue("end"));
        } else {
            LOGE(this.getClass().getName(), "unknown block: " + pEvent.timeslot);
        }


    }

    private Map<String, EMSItem> loadBlocks() {
        return load(Config.EMS_SLOTS);
    }

    private Map<String, EMSItem> load(String url) {

        Map<String, EMSItem> result = new HashMap<String, EMSItem>();

        try {

            String json;

            if (SyncHelper.isFirstRun(mContext)) {
                json = SyncHelper.getLocalResource(mContext, url);
            } else if (SyncHelper.isOnline(mContext)) {
                json = SyncHelper.getHttpResource(url);
            } else {
                return result;
            }

            if (json == null) {
                return result;
            }

            JZSlotsResponse slotResponse = new Gson().fromJson(json, JZSlotsResponse.class);

            for (EMSItem slot : slotResponse.collection.items) {
                result.put(slot.href.toString(), slot);
            }

        } catch (MalformedURLException e) {
            LOGE(this.getClass().getName(), e.getMessage());

        } catch (IOException e) {
            LOGE(this.getClass().getName(), e.getMessage());
        }

        return result;

    }


    static List<JZSessionsResult> toJZSessionResultList(final EMSItem[] pItems) {

        List<JZSessionsResult> result = new ArrayList<JZSessionsResult>(pItems.length);

        for (EMSItem item : pItems) {
            result.add(JZSessionsResult.from(item));
        }

        return result;
    }

    private static long snapStartTime(final long pSessionStartTime) {

        Date date = new Date(pSessionStartTime);
        int minutes = (date.getHours() - 9) * 60 + (date.getMinutes() - 0);

        int offset = minutes % (60 + 20);
        date.setMinutes(date.getMinutes() - offset);
        return date.getTime();


    }

    private static long snapEndTime(final long pSessionEndTime) {

        Date date = new Date(pSessionEndTime);
        int minutes = (date.getHours() - 9) * 60 + (date.getMinutes() + 0);

        int offset = minutes % (60 + 20);
        date.setMinutes(date.getMinutes() + 60 - offset);
        return date.getTime();


    }

    public static final String[] MYSCHEDULE_PROJECTION = {
            BaseColumns._ID,
            ScheduleContract.MySchedule.SESSION_ID,
            ScheduleContract.MySchedule.MY_SCHEDULE_IN_SCHEDULE,
    };
}
