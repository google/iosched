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

package com.google.samples.apps.iosched.provider;

import com.google.samples.apps.iosched.appwidget.ScheduleWidgetProvider;
import com.google.samples.apps.iosched.provider.ScheduleContract.Announcements;
import com.google.samples.apps.iosched.provider.ScheduleContract.PeopleIveMet;
import com.google.samples.apps.iosched.provider.ScheduleContract.Blocks;
import com.google.samples.apps.iosched.provider.ScheduleContract.Experts;
import com.google.samples.apps.iosched.provider.ScheduleContract.Feedback;
import com.google.samples.apps.iosched.provider.ScheduleContract.MapMarkers;
import com.google.samples.apps.iosched.provider.ScheduleContract.MapTiles;
import com.google.samples.apps.iosched.provider.ScheduleContract.Rooms;
import com.google.samples.apps.iosched.provider.ScheduleContract.SearchSuggest;
import com.google.samples.apps.iosched.provider.ScheduleContract.Sessions;
import com.google.samples.apps.iosched.provider.ScheduleContract.Speakers;
import com.google.samples.apps.iosched.provider.ScheduleContract.Tags;
import com.google.samples.apps.iosched.provider.ScheduleDatabase.SessionsSearchColumns;
import com.google.samples.apps.iosched.provider.ScheduleDatabase.SessionsSpeakers;
import com.google.samples.apps.iosched.provider.ScheduleDatabase.Tables;
import com.google.samples.apps.iosched.util.SelectionBuilder;

import android.app.Activity;
import android.app.SearchManager;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.appwidget.ScheduleWidgetProvider;
import com.google.samples.apps.iosched.provider.ScheduleContract.*;
import com.google.samples.apps.iosched.provider.ScheduleDatabase.SessionsSearchColumns;
import com.google.samples.apps.iosched.provider.ScheduleDatabase.SessionsSpeakers;
import com.google.samples.apps.iosched.provider.ScheduleDatabase.Tables;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.SelectionBuilder;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGV;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Provider that stores {@link ScheduleContract} data. Data is usually inserted
 * by {@link com.google.samples.apps.iosched.sync.SyncHelper}, and queried by various
 * {@link Activity} instances.
 */
public class ScheduleProvider extends ContentProvider {
    private static final String TAG = makeLogTag(ScheduleProvider.class);

    private ScheduleDatabase mOpenHelper;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final int BLOCKS = 100;
    private static final int BLOCKS_BETWEEN = 101;
    private static final int BLOCKS_ID = 102;

    private static final int TAGS = 200;
    private static final int TAGS_ID = 201;

    private static final int ROOMS = 300;
    private static final int ROOMS_ID = 301;
    private static final int ROOMS_ID_SESSIONS = 302;

    private static final int SESSIONS = 400;
    private static final int SESSIONS_MY_SCHEDULE = 401;
    private static final int SESSIONS_SEARCH = 403;
    private static final int SESSIONS_AT = 404;
    private static final int SESSIONS_ID = 405;
    private static final int SESSIONS_ID_SPEAKERS = 406;
    private static final int SESSIONS_ID_TAGS = 407;
    private static final int SESSIONS_ROOM_AFTER = 408;
    private static final int SESSIONS_UNSCHEDULED = 409;
    private static final int SESSIONS_COUNTER = 410;

    private static final int SPEAKERS = 500;
    private static final int SPEAKERS_ID = 501;
    private static final int SPEAKERS_ID_SESSIONS = 502;

    private static final int MY_SCHEDULE = 600;

    private static final int ANNOUNCEMENTS = 700;
    private static final int ANNOUNCEMENTS_ID = 701;

    private static final int SEARCH_SUGGEST = 800;
    private static final int SEARCH_INDEX = 801;

    private static final int MAPMARKERS = 900;
    private static final int MAPMARKERS_FLOOR = 901;
    private static final int MAPMARKERS_ID = 902;

    private static final int MAPTILES = 1000;
    private static final int MAPTILES_FLOOR = 1001;

    private static final int FEEDBACK_ALL = 1002;
    private static final int FEEDBACK_FOR_SESSION = 1003;

    private static final int EXPERTS = 1100;
    private static final int EXPERTS_ID = 1101;
    private static final int HASHTAGS = 1200;
    private static final int HASHTAGS_NAME = 1201;

    private static final int PEOPLE_IVE_MET = 1250;
    private static final int PEOPLE_IVE_MET_ID = 1251;

    private static final int VIDEOS = 1300;
    private static final int VIDEOS_ID = 1301;

    private static final int PARTNERS = 1400;
    private static final int PARTNERS_ID = 1401;


    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri}
     * variations supported by this {@link ContentProvider}.
     */
    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = ScheduleContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, "blocks", BLOCKS);
        matcher.addURI(authority, "blocks/between/*/*", BLOCKS_BETWEEN);
        matcher.addURI(authority, "blocks/*", BLOCKS_ID);

        matcher.addURI(authority, "tags", TAGS);
        matcher.addURI(authority, "tags/*", TAGS_ID);

        matcher.addURI(authority, "rooms", ROOMS);
        matcher.addURI(authority, "rooms/*", ROOMS_ID);
        matcher.addURI(authority, "rooms/*/sessions", ROOMS_ID_SESSIONS);

        matcher.addURI(authority, "sessions", SESSIONS);
        matcher.addURI(authority, "sessions/my_schedule", SESSIONS_MY_SCHEDULE);
        matcher.addURI(authority, "sessions/search/*", SESSIONS_SEARCH);
        matcher.addURI(authority, "sessions/at/*", SESSIONS_AT);
        matcher.addURI(authority, "sessions/unscheduled/*", SESSIONS_UNSCHEDULED);
        matcher.addURI(authority, "sessions/room/*/after/*", SESSIONS_ROOM_AFTER);
        matcher.addURI(authority, "sessions/counter", SESSIONS_COUNTER);
        matcher.addURI(authority, "sessions/*", SESSIONS_ID);
        matcher.addURI(authority, "sessions/*/speakers", SESSIONS_ID_SPEAKERS);
        matcher.addURI(authority, "sessions/*/tags", SESSIONS_ID_TAGS);

        matcher.addURI(authority, "my_schedule", MY_SCHEDULE);

        matcher.addURI(authority, "speakers", SPEAKERS);
        matcher.addURI(authority, "speakers/*", SPEAKERS_ID);
        matcher.addURI(authority, "speakers/*/sessions", SPEAKERS_ID_SESSIONS);

        matcher.addURI(authority, "announcements", ANNOUNCEMENTS);
        matcher.addURI(authority, "announcements/*", ANNOUNCEMENTS_ID);

        matcher.addURI(authority, "search_suggest_query", SEARCH_SUGGEST);
        matcher.addURI(authority, "search_index", SEARCH_INDEX); // 'update' only

        matcher.addURI(authority, "mapmarkers", MAPMARKERS);
        matcher.addURI(authority, "mapmarkers/floor/*", MAPMARKERS_FLOOR);
        matcher.addURI(authority, "mapmarkers/*", MAPMARKERS_ID);

        matcher.addURI(authority, "maptiles", MAPTILES);
        matcher.addURI(authority, "maptiles/*", MAPTILES_FLOOR);

        matcher.addURI(authority, "feedback/*", FEEDBACK_FOR_SESSION);
        matcher.addURI(authority, "feedback*", FEEDBACK_ALL);
        matcher.addURI(authority, "feedback", FEEDBACK_ALL);

        matcher.addURI(authority, "experts/", EXPERTS);
        matcher.addURI(authority, "experts/*", EXPERTS_ID);

        matcher.addURI(authority, "hashtags", HASHTAGS);
        matcher.addURI(authority, "hashtags/*", HASHTAGS_NAME);

        matcher.addURI(authority, "people_ive_met/", PEOPLE_IVE_MET);
        matcher.addURI(authority, "people_ive_met/*", PEOPLE_IVE_MET_ID);

        matcher.addURI(authority, "videos", VIDEOS);
        matcher.addURI(authority, "videos/*", VIDEOS_ID);

        matcher.addURI(authority, "partners", PARTNERS);
        matcher.addURI(authority, "partners/*", PARTNERS_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new ScheduleDatabase(getContext());
        return true;
    }

    private void deleteDatabase() {
        // TODO: wait for content provider operations to finish, then tear down
        mOpenHelper.close();
        Context context = getContext();
        ScheduleDatabase.deleteDatabase(context);
        mOpenHelper = new ScheduleDatabase(getContext());
    }

    /** {@inheritDoc} */
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case BLOCKS:
                return Blocks.CONTENT_TYPE;
            case BLOCKS_BETWEEN:
                return Blocks.CONTENT_TYPE;
            case BLOCKS_ID:
                return Blocks.CONTENT_ITEM_TYPE;
            case TAGS:
                return Tags.CONTENT_TYPE;
            case TAGS_ID:
                return Tags.CONTENT_TYPE;
            case ROOMS:
                return Rooms.CONTENT_TYPE;
            case ROOMS_ID:
                return Rooms.CONTENT_ITEM_TYPE;
            case ROOMS_ID_SESSIONS:
                return Sessions.CONTENT_TYPE;
            case SESSIONS:
                return Sessions.CONTENT_TYPE;
            case SESSIONS_MY_SCHEDULE:
                return Sessions.CONTENT_TYPE;
            case SESSIONS_UNSCHEDULED:
                return Sessions.CONTENT_TYPE;
            case SESSIONS_SEARCH:
                return Sessions.CONTENT_TYPE;
            case SESSIONS_AT:
                return Sessions.CONTENT_TYPE;
            case SESSIONS_ID:
                return Sessions.CONTENT_ITEM_TYPE;
            case SESSIONS_ID_SPEAKERS:
                return Speakers.CONTENT_TYPE;
            case SESSIONS_ID_TAGS:
                return Tags.CONTENT_TYPE;
            case SESSIONS_ROOM_AFTER:
            	return Sessions.CONTENT_TYPE;
            case MY_SCHEDULE:
                return MySchedule.CONTENT_TYPE;
            case SPEAKERS:
                return Speakers.CONTENT_TYPE;
            case SPEAKERS_ID:
                return Speakers.CONTENT_ITEM_TYPE;
            case SPEAKERS_ID_SESSIONS:
                return Sessions.CONTENT_TYPE;
            case ANNOUNCEMENTS:
                return Announcements.CONTENT_TYPE;
            case ANNOUNCEMENTS_ID:
                return Announcements.CONTENT_ITEM_TYPE;
            case MAPMARKERS:
            	return MapMarkers.CONTENT_TYPE;
            case MAPMARKERS_FLOOR:
            	return MapMarkers.CONTENT_TYPE;
            case MAPMARKERS_ID:
            	return MapMarkers.CONTENT_ITEM_TYPE;
            case MAPTILES:
            	return MapTiles.CONTENT_TYPE;
            case MAPTILES_FLOOR:
            	return MapTiles.CONTENT_ITEM_TYPE;
            case FEEDBACK_FOR_SESSION:
                return Feedback.CONTENT_ITEM_TYPE;
            case FEEDBACK_ALL:
                return Feedback.CONTENT_TYPE;
            case EXPERTS:
                return Experts.CONTENT_TYPE;
            case EXPERTS_ID:
                return Experts.CONTENT_ITEM_TYPE;
            case PEOPLE_IVE_MET:
                return ScheduleContract.PeopleIveMet.CONTENT_TYPE;
            case PEOPLE_IVE_MET_ID:
                return ScheduleContract.PeopleIveMet.CONTENT_ITEM_TYPE;
            case HASHTAGS:
                return Hashtags.CONTENT_TYPE;
            case HASHTAGS_NAME:
                return Hashtags.CONTENT_ITEM_TYPE;
            case VIDEOS:
                return Videos.CONTENT_TYPE;
            case VIDEOS_ID:
                return Videos.CONTENT_ITEM_TYPE;
            case PARTNERS:
                return Partners.CONTENT_TYPE;
            case PARTNERS_ID:
                return Partners.CONTENT_ITEM_TYPE;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    /** Returns a tuple of question marks. For example, if count is 3, returns "(?,?,?)". */
    private String makeQuestionMarkTuple(int count) {
        if (count < 1) {
            return "()";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(?");
        for (int i = 1; i < count; i++) {
            stringBuilder.append(",?");
        }
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    /** Adds the tags filter query parameter to the given builder. */
    private void addTagsFilter(SelectionBuilder builder, String tagsFilter) {
        // Note: for context, remember that session queries are done on a join of sessions
        // and the sessions_tags relationship table, and are GROUP'ed BY the session ID.
        String[] requiredTags = tagsFilter.split(",");
        if (requiredTags.length == 0) {
            // filtering by 0 tags -- no-op
            return;
        } else if (requiredTags.length == 1) {
            // filtering by only one tag, so a simple WHERE clause suffices
            builder.where(Tags.TAG_ID + "=?", requiredTags[0]);
        } else {
            // Filtering by multiple tags, so we must add a WHERE clause with an IN operator,
            // and add a HAVING statement to exclude groups that fall short of the number
            // of required tags. For example, if requiredTags is { "X", "Y", "Z" }, and a certain
            // session only has tags "X" and "Y", it will be excluded by the HAVING statement.
            String questionMarkTuple = makeQuestionMarkTuple(requiredTags.length);
            builder.where(Tags.TAG_ID + " IN " + questionMarkTuple, requiredTags);
            builder.having("COUNT(" + Qualified.SESSIONS_SESSION_ID + ") >= " + requiredTags.length);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {

        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        String tagsFilter = uri.getQueryParameter(Sessions.QUERY_PARAMETER_TAG_FILTER);
        final int match = sUriMatcher.match(uri);

        // avoid the expensive string concatenation below if not loggable
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            LOGV(TAG, "uri=" + uri + " match=" + match + " proj=" + Arrays.toString(projection) +
                    " selection=" + selection + " args=" + Arrays.toString(selectionArgs) + ")");
        }


        switch (match) {
            default: {
                // Most cases are handled with simple SelectionBuilder
                final SelectionBuilder builder = buildExpandedSelection(uri, match);

                // If a special filter was specified, try to apply it
                if (!TextUtils.isEmpty(tagsFilter)) {
                    addTagsFilter(builder, tagsFilter);
                }

                boolean distinct = !TextUtils.isEmpty(
                        uri.getQueryParameter(ScheduleContract.QUERY_PARAMETER_DISTINCT));

                Cursor cursor = builder
                        .where(selection, selectionArgs)
                        .query(db, distinct, projection, sortOrder, null);
                Context context = getContext();
                if (null != context) {
                    cursor.setNotificationUri(context.getContentResolver(), uri);
                }
                return cursor;
            }
            case SEARCH_SUGGEST: {
                final SelectionBuilder builder = new SelectionBuilder();

                // Adjust incoming query to become SQL text match
                selectionArgs[0] = selectionArgs[0] + "%";
                builder.table(Tables.SEARCH_SUGGEST);
                builder.where(selection, selectionArgs);
                builder.map(SearchManager.SUGGEST_COLUMN_QUERY,
                        SearchManager.SUGGEST_COLUMN_TEXT_1);

                projection = new String[] {
                        BaseColumns._ID,
                        SearchManager.SUGGEST_COLUMN_TEXT_1,
                        SearchManager.SUGGEST_COLUMN_QUERY
                };

                final String limit = uri.getQueryParameter(SearchManager.SUGGEST_PARAMETER_LIMIT);
                return builder.query(db, false, projection, SearchSuggest.DEFAULT_SORT, limit);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        LOGV(TAG, "insert(uri=" + uri + ", values=" + values.toString()
                + ", account=" + getCurrentAccountName(uri, false) + ")");
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        boolean syncToNetwork = !ScheduleContract.hasCallerIsSyncAdapterParameter(uri);
        switch (match) {
            case BLOCKS: {
                db.insertOrThrow(Tables.BLOCKS, null, values);
                notifyChange(uri);
                return Blocks.buildBlockUri(values.getAsString(Blocks.BLOCK_ID));
            }
            case TAGS: {
                db.insertOrThrow(Tables.TAGS, null, values);
                notifyChange(uri);
                return Tags.buildTagUri(values.getAsString(Tags.TAG_ID));
            }
            case ROOMS: {
                db.insertOrThrow(Tables.ROOMS, null, values);
                notifyChange(uri);
                return Rooms.buildRoomUri(values.getAsString(Rooms.ROOM_ID));
            }
            case SESSIONS: {
                db.insertOrThrow(Tables.SESSIONS, null, values);
                notifyChange(uri);
                return Sessions.buildSessionUri(values.getAsString(Sessions.SESSION_ID));
            }
            case SESSIONS_ID_SPEAKERS: {
                db.insertOrThrow(Tables.SESSIONS_SPEAKERS, null, values);
                notifyChange(uri);
                return Speakers.buildSpeakerUri(values.getAsString(SessionsSpeakers.SPEAKER_ID));
            }
            case SESSIONS_ID_TAGS: {
                db.insertOrThrow(Tables.SESSIONS_TAGS, null, values);
                notifyChange(uri);
                return Tags.buildTagUri(values.getAsString(Tags.TAG_ID));
            }
            case MY_SCHEDULE: {
                values.put(MySchedule.MY_SCHEDULE_ACCOUNT_NAME, getCurrentAccountName(uri, false));
                db.insertOrThrow(Tables.MY_SCHEDULE, null, values);
                notifyChange(uri);
                return Sessions.buildSessionUri(values.getAsString(
                        ScheduleContract.MyScheduleColumns.SESSION_ID));
            }
            case SPEAKERS: {
                db.insertOrThrow(Tables.SPEAKERS, null, values);
                notifyChange(uri);
                return Speakers.buildSpeakerUri(values.getAsString(Speakers.SPEAKER_ID));
            }
            case ANNOUNCEMENTS: {
                db.insertOrThrow(Tables.ANNOUNCEMENTS, null, values);
                notifyChange(uri);
                return Announcements.buildAnnouncementUri(values
                        .getAsString(Announcements.ANNOUNCEMENT_ID));
            }
            case SEARCH_SUGGEST: {
                db.insertOrThrow(Tables.SEARCH_SUGGEST, null, values);
                notifyChange(uri);
                return SearchSuggest.CONTENT_URI;
            }
            case MAPMARKERS: {
                db.insertOrThrow(Tables.MAPMARKERS, null, values);
                notifyChange(uri);
                return MapMarkers.buildMarkerUri(values.getAsString(MapMarkers.MARKER_ID));
            }
            case MAPTILES: {
                db.insertOrThrow(Tables.MAPTILES, null, values);
                notifyChange(uri);
                return MapTiles.buildFloorUri(values.getAsString(MapTiles.TILE_FLOOR));
            }
            case FEEDBACK_FOR_SESSION: {
                db.insertOrThrow(Tables.FEEDBACK, null, values);
                notifyChange(uri);
                return Feedback.buildFeedbackUri(values.getAsString(Feedback.SESSION_ID));
            }
            case EXPERTS: {
                db.insertOrThrow(Tables.EXPERTS, null, values);
                notifyChange(uri);
                return Experts.buildExpertUri(values.getAsString(Experts.EXPERT_ID));
            }
            case HASHTAGS: {
                db.insertOrThrow(Tables.HASHTAGS, null, values);
                notifyChange(uri);
                return Hashtags.buildHashtagUri(values.getAsString(Hashtags.HASHTAG_NAME));
            }
            case PEOPLE_IVE_MET: {
                db.insertOrThrow(Tables.PEOPLE_IVE_MET, null, values);
                notifyChange(uri);
                return ScheduleContract.PeopleIveMet.buildPersonUri(values.getAsString(PeopleIveMet.PERSON_ID));
            }
            case VIDEOS: {
                db.insertOrThrow(Tables.VIDEOS, null, values);
                notifyChange(uri);
                return Videos.buildVideoUri(values.getAsString(Videos.VIDEO_ID));
            }
            case PARTNERS: {
                db.insertOrThrow(Tables.PARTNERS, null, values);
                notifyChange(uri);
                return Partners.buildPartnerUri(values.getAsString(Partners.PARTNER_ID));
            }
            default: {
                throw new UnsupportedOperationException("Unknown insert uri: " + uri);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        String accountName = getCurrentAccountName(uri, false);
        LOGV(TAG, "update(uri=" + uri + ", values=" + values.toString()
                + ", account=" + accountName + ")");
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        if (match == SEARCH_INDEX) {
            // update the search index
            ScheduleDatabase.updateSessionSearchIndex(db);
            return 1;
        }

        final SelectionBuilder builder = buildSimpleSelection(uri);
        if (match == MY_SCHEDULE) {
            values.remove(MySchedule.MY_SCHEDULE_ACCOUNT_NAME);
            builder.where(MySchedule.MY_SCHEDULE_ACCOUNT_NAME + "=?", accountName);
        }
        int retVal = builder.where(selection, selectionArgs).update(db, values);
        notifyChange(uri);
        return retVal;
    }

    /** {@inheritDoc} */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        String accountName = getCurrentAccountName(uri, false);
        LOGV(TAG, "delete(uri=" + uri + ", account=" + accountName + ")");
        if (uri.equals(ScheduleContract.BASE_CONTENT_URI)) {
            // Handle whole database deletes (e.g. when signing out)
            deleteDatabase();
            notifyChange(uri);
            return 1;
        }
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        final int match = sUriMatcher.match(uri);
        if (match == MY_SCHEDULE) {
            builder.where(MySchedule.MY_SCHEDULE_ACCOUNT_NAME + "=?", accountName);
        }
        int retVal = builder.where(selection, selectionArgs).delete(db);
        notifyChange(uri);
        return retVal;
    }

    private void notifyChange(Uri uri) {
        // We only notify changes if the caller is not the sync adapter.
        // The sync adapter has the responsibility of notifying changes (it can do so
        // more intelligently than we can -- for example, doing it only once at the end
        // of the sync instead of issuing thousands of notifications for each record).
        if (!ScheduleContract.hasCallerIsSyncAdapterParameter(uri)) {
            Context context = getContext();
            context.getContentResolver().notifyChange(uri, null);

            // Widgets can't register content observers so we refresh widgets separately.
            context.sendBroadcast(ScheduleWidgetProvider.getRefreshBroadcastIntent(context, false));
        }
    }

    /**
     * Apply the given set of {@link ContentProviderOperation}, executing inside
     * a {@link SQLiteDatabase} transaction. All changes will be rolled back if
     * any single one fails.
     */
    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            final int numOperations = operations.size();
            final ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                results[i] = operations.get(i).apply(this, results, i);
            }
            db.setTransactionSuccessful();
            return results;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Build a simple {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually enough to support {@link #insert},
     * {@link #update}, and {@link #delete} operations.
     */
    private SelectionBuilder buildSimpleSelection(Uri uri) {
        final SelectionBuilder builder = new SelectionBuilder();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case BLOCKS: {
                return builder.table(Tables.BLOCKS);
            }
            case BLOCKS_ID: {
                final String blockId = Blocks.getBlockId(uri);
                return builder.table(Tables.BLOCKS)
                        .where(Blocks.BLOCK_ID + "=?", blockId);
            }
            case TAGS: {
                return builder.table(Tables.TAGS);
            }
            case TAGS_ID: {
                final String tagId = Tags.getTagId(uri);
                return builder.table(Tables.TAGS)
                        .where(Tags.TAG_ID + "=?", tagId);
            }
            case ROOMS: {
                return builder.table(Tables.ROOMS);
            }
            case ROOMS_ID: {
                final String roomId = Rooms.getRoomId(uri);
                return builder.table(Tables.ROOMS)
                        .where(Rooms.ROOM_ID + "=?", roomId);
            }
            case SESSIONS: {
                return builder.table(Tables.SESSIONS);
            }
            case SESSIONS_ID: {
                final String sessionId = Sessions.getSessionId(uri);
                return builder.table(Tables.SESSIONS)
                        .where(Sessions.SESSION_ID + "=?", sessionId);
            }
            case SESSIONS_ID_SPEAKERS: {
                final String sessionId = Sessions.getSessionId(uri);
                return builder.table(Tables.SESSIONS_SPEAKERS)
                        .where(Sessions.SESSION_ID + "=?", sessionId);
            }
            case SESSIONS_ID_TAGS: {
                final String sessionId = Sessions.getSessionId(uri);
                return builder.table(Tables.SESSIONS_TAGS)
                        .where(Sessions.SESSION_ID + "=?", sessionId);
            }
            case SESSIONS_MY_SCHEDULE: {
                final String sessionId = Sessions.getSessionId(uri);
                return builder.table(Tables.MY_SCHEDULE)
                        .where(ScheduleContract.MyScheduleColumns.SESSION_ID + "=?", sessionId);
            }
            case MY_SCHEDULE: {
                return builder.table(Tables.MY_SCHEDULE)
                    .where(MySchedule.MY_SCHEDULE_ACCOUNT_NAME + "=?", getCurrentAccountName(uri, false));
            }
            case SPEAKERS: {
                return builder.table(Tables.SPEAKERS);
            }
            case SPEAKERS_ID: {
                final String speakerId = Speakers.getSpeakerId(uri);
                return builder.table(Tables.SPEAKERS)
                        .where(Speakers.SPEAKER_ID + "=?", speakerId);
            }
            case ANNOUNCEMENTS: {
                return builder.table(Tables.ANNOUNCEMENTS);
            }
            case ANNOUNCEMENTS_ID: {
                final String announcementId = Announcements.getAnnouncementId(uri);
                return builder.table(Tables.ANNOUNCEMENTS)
                        .where(Announcements.ANNOUNCEMENT_ID + "=?", announcementId);
            }
            case MAPMARKERS: {
                return builder.table(Tables.MAPMARKERS);
            }
            case MAPMARKERS_FLOOR: {
                final String floor = MapMarkers.getMarkerFloor(uri);
                return builder.table(Tables.MAPMARKERS)
                        .where(MapMarkers.MARKER_FLOOR+ "=?", floor);
            }
            case MAPMARKERS_ID: {
                final String markerId = MapMarkers.getMarkerId(uri);
                return builder.table(Tables.MAPMARKERS)
                        .where(MapMarkers.MARKER_ID + "=?", markerId);
            }
            case MAPTILES: {
                return builder.table(Tables.MAPTILES);
            }
            case MAPTILES_FLOOR: {
                final String  floor = MapTiles.getFloorId(uri);
                return builder.table(Tables.MAPTILES)
                        .where(MapTiles.TILE_FLOOR+ "=?", floor);
            }
            case SEARCH_SUGGEST: {
                return builder.table(Tables.SEARCH_SUGGEST);
            }
            case FEEDBACK_FOR_SESSION: {
                final String session_id  = Feedback.getSessionId(uri);
                return builder.table(Tables.FEEDBACK)
                        .where(Feedback.SESSION_ID + "=?", session_id);
            }
            case FEEDBACK_ALL: {
                return builder.table(Tables.FEEDBACK);
            }
            case EXPERTS: {
                return builder.table(Tables.EXPERTS);
            }
            case EXPERTS_ID: {
                String expertId = Experts.getExpertId(uri);
                return builder.table(Tables.EXPERTS)
                        .where(Experts.EXPERT_ID + "= ?", expertId);
            }
            case HASHTAGS: {
                return builder.table(Tables.HASHTAGS);
            }
            case HASHTAGS_NAME: {
                final String hashtagName = Hashtags.getHashtagName(uri);
                return builder.table(Tables.HASHTAGS)
                        .where(Hashtags.HASHTAG_NAME + "=?", hashtagName);
            }
            case PEOPLE_IVE_MET: {
                return builder.table(Tables.PEOPLE_IVE_MET);
            }
            case PEOPLE_IVE_MET_ID: {
                String personId = ScheduleContract.PeopleIveMet.getPersonId(uri);
                return builder.table(Tables.PEOPLE_IVE_MET)
                        .where(PeopleIveMet.PERSON_ID + "=?", personId);
            }
            case VIDEOS: {
                return builder.table(Tables.VIDEOS);
            }
            case VIDEOS_ID: {
                final String videoId = Videos.getVideoId(uri);
                return builder.table(Tables.VIDEOS).where(Videos.VIDEO_ID + "=?", videoId);
            }
            case PARTNERS: {
                return builder.table(Tables.PARTNERS);
            }
            case PARTNERS_ID: {
                final String partnerId = Partners.getPartnerId(uri);
                return builder.table(Tables.PARTNERS).where(Partners.PARTNER_ID + "=?", partnerId);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri for " + match + ": " + uri);
            }
        }
    }

    private String getCurrentAccountName(Uri uri, boolean sanitize) {
        String accountName = ScheduleContract.getOverrideAccountName(uri);
        if (accountName == null) {
            accountName = AccountUtils.getActiveAccountName(getContext());
        }
        if (sanitize) {
            // sanitize accountName when concatenating (http://xkcd.com/327/)
            accountName = (accountName != null) ? accountName.replace("'", "''") : null;
        }
        return accountName;
    }

    /**
     * Build an advanced {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually only used by {@link #query}, since it
     * performs table joins useful for {@link Cursor} data.
     */
    private SelectionBuilder buildExpandedSelection(Uri uri, int match) {
        final SelectionBuilder builder = new SelectionBuilder();
        switch (match) {
            case BLOCKS: {
                return builder.table(Tables.BLOCKS);
            }
            case BLOCKS_BETWEEN: {
                final List<String> segments = uri.getPathSegments();
                final String startTime = segments.get(2);
                final String endTime = segments.get(3);
                return builder.table(Tables.BLOCKS)
                        .where(Blocks.BLOCK_START + ">=?", startTime)
                        .where(Blocks.BLOCK_START + "<=?", endTime);
            }
            case BLOCKS_ID: {
                final String blockId = Blocks.getBlockId(uri);
                return builder.table(Tables.BLOCKS)
                        .where(Blocks.BLOCK_ID + "=?", blockId);
            }
            case TAGS: {
                return builder.table(Tables.TAGS);
            }
            case TAGS_ID: {
                final String tagId = Tags.getTagId(uri);
                return builder.table(Tables.TAGS)
                        .where(Tags.TAG_ID + "=?", tagId);
            }
            case ROOMS: {
                return builder.table(Tables.ROOMS);
            }
            case ROOMS_ID: {
                final String roomId = Rooms.getRoomId(uri);
                return builder.table(Tables.ROOMS)
                        .where(Rooms.ROOM_ID + "=?", roomId);
            }
            case ROOMS_ID_SESSIONS: {
                final String roomId = Rooms.getRoomId(uri);
                return builder.table(Tables.SESSIONS_JOIN_ROOMS)
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .where(Qualified.SESSIONS_ROOM_ID + "=?", roomId);
            }
            case SESSIONS: {
                // We query sessions on the joined table of sessions with rooms and tags.
                // Since there may be more than one tag per session, we GROUP BY session ID.
                // The starred sessions ("my schedule") are associated with a user, so we
                // use the current user to select them properly
                return builder.table(Tables.SESSIONS_JOIN_ROOMS_TAGS, getCurrentAccountName(uri, true))
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
                        .map(Sessions.SESSION_IN_MY_SCHEDULE, "IFNULL(in_schedule, 0)")
                        .groupBy(Qualified.SESSIONS_SESSION_ID);
            }
            case SESSIONS_COUNTER: {
                return builder.table(Tables.SESSIONS_JOIN_MYSCHEDULE, getCurrentAccountName(uri, true))
                        .map(Sessions.SESSION_INTERVAL_COUNT, "count(1)")
                        .map(Sessions.SESSION_IN_MY_SCHEDULE, "IFNULL(in_schedule, 0)")
                        .groupBy(Sessions.SESSION_START + ", " + Sessions.SESSION_END);
            }
            case SESSIONS_MY_SCHEDULE: {
                return builder.table(Tables.SESSIONS_JOIN_ROOMS_TAGS_FEEDBACK_MYSCHEDULE, getCurrentAccountName(uri, true))
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
                        .map(Sessions.HAS_GIVEN_FEEDBACK, Subquery.SESSION_HAS_GIVEN_FEEDBACK)
                        .map(Sessions.SESSION_IN_MY_SCHEDULE, "IFNULL(in_schedule, 0)")
                        .where("( " + Sessions.SESSION_IN_MY_SCHEDULE + "=1 OR " +
                                Sessions.SESSION_TAGS +
                                " LIKE '%" + Config.Tags.SPECIAL_KEYNOTE + "%' )")
                        .groupBy(Qualified.SESSIONS_SESSION_ID);
            }
            case SESSIONS_UNSCHEDULED: {
                final long[] interval = Sessions.getInterval(uri);
                return builder.table(Tables.SESSIONS_JOIN_ROOMS_TAGS_FEEDBACK_MYSCHEDULE, getCurrentAccountName(uri, true))
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
                        .map(Sessions.SESSION_IN_MY_SCHEDULE, "IFNULL(in_schedule, 0)")
                        .where(Sessions.SESSION_IN_MY_SCHEDULE + "=0")
                        .where(Sessions.SESSION_START + ">=?", String.valueOf(interval[0]))
                        .where(Sessions.SESSION_START + "<?", String.valueOf(interval[1]))
                        .groupBy(Qualified.SESSIONS_SESSION_ID);
            }
            case SESSIONS_SEARCH: {
                final String query = Sessions.getSearchQuery(uri);
                return builder.table(Tables.SESSIONS_SEARCH_JOIN_SESSIONS_ROOMS, getCurrentAccountName(uri, true))
                        .map(Sessions.SEARCH_SNIPPET, Subquery.SESSIONS_SNIPPET)
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .map(Sessions.SESSION_IN_MY_SCHEDULE, "IFNULL(in_schedule, 0)")
                        .where(SessionsSearchColumns.BODY + " MATCH ?", query);
            }
            case SESSIONS_AT: {
                final List<String> segments = uri.getPathSegments();
                final String time = segments.get(2);
                return builder.table(Tables.SESSIONS_JOIN_ROOMS, getCurrentAccountName(uri, true))
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .where(Sessions.SESSION_START + "<=?", time)
                        .where(Sessions.SESSION_END + ">=?", time);
            }
            case SESSIONS_ID: {
                final String sessionId = Sessions.getSessionId(uri);
                return builder.table(Tables.SESSIONS_JOIN_ROOMS, getCurrentAccountName(uri, true))
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
                        .map(Sessions.SESSION_IN_MY_SCHEDULE, "IFNULL(in_schedule, 0)")
                        .where(Qualified.SESSIONS_SESSION_ID + "=?", sessionId);
            }
            case SESSIONS_ID_SPEAKERS: {
                final String sessionId = Sessions.getSessionId(uri);
                return builder.table(Tables.SESSIONS_SPEAKERS_JOIN_SPEAKERS)
                        .mapToTable(Speakers._ID, Tables.SPEAKERS)
                        .mapToTable(Speakers.SPEAKER_ID, Tables.SPEAKERS)
                        .where(Qualified.SESSIONS_SPEAKERS_SESSION_ID + "=?", sessionId);
            }
            case SESSIONS_ID_TAGS: {
                final String sessionId = Sessions.getSessionId(uri);
                return builder.table(Tables.SESSIONS_TAGS_JOIN_TAGS)
                        .mapToTable(Tags._ID, Tables.TAGS)
                        .mapToTable(Tags.TAG_ID, Tables.TAGS)
                        .where(Qualified.SESSIONS_TAGS_SESSION_ID + "=?", sessionId);
            }
            case SESSIONS_ROOM_AFTER: {
				final String room = Sessions.getRoom(uri);
                final String time = Sessions.getAfter(uri);
				return builder.table(Tables.SESSIONS_JOIN_ROOMS, getCurrentAccountName(uri, true))
						.mapToTable(Sessions._ID, Tables.SESSIONS)
						.mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
						.where(Qualified.SESSIONS_ROOM_ID+ "=?", room)
						.where("("+Sessions.SESSION_START + "<= ? AND "+Sessions.SESSION_END+
                                " >= ?) OR ("+Sessions.SESSION_START + " >= ?)", time,time,time);
           }
            case SPEAKERS: {
                return builder.table(Tables.SPEAKERS);
            }
            case MY_SCHEDULE: {
                // force a where condition to avoid leaking schedule info to another account
                // Note that, since SelectionBuilder always join multiple where calls using AND,
                // even if malicious code specifying additional conditions on account_name won't
                // be able to fetch data from a different account.
                return builder.table(Tables.MY_SCHEDULE)
                        .where(MySchedule.MY_SCHEDULE_ACCOUNT_NAME + "=?", getCurrentAccountName(uri, true));
            }
            case SPEAKERS_ID: {
                final String speakerId = Speakers.getSpeakerId(uri);
                return builder.table(Tables.SPEAKERS)
                        .where(Speakers.SPEAKER_ID + "=?", speakerId);
            }
            case SPEAKERS_ID_SESSIONS: {
                final String speakerId = Speakers.getSpeakerId(uri);
                return builder.table(Tables.SESSIONS_SPEAKERS_JOIN_SESSIONS_ROOMS)
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .where(Qualified.SESSIONS_SPEAKERS_SPEAKER_ID + "=?", speakerId);
            }
            case ANNOUNCEMENTS: {
                return builder.table(Tables.ANNOUNCEMENTS);
            }
            case ANNOUNCEMENTS_ID: {
                final String announcementId = Announcements.getAnnouncementId(uri);
                return builder.table(Tables.ANNOUNCEMENTS)
                        .where(Announcements.ANNOUNCEMENT_ID + "=?", announcementId);
            }
            case MAPMARKERS: {
                return builder.table(Tables.MAPMARKERS);
            }
            case MAPMARKERS_FLOOR: {
                final String floor = MapMarkers.getMarkerFloor(uri);
                return builder.table(Tables.MAPMARKERS)
                        .where(MapMarkers.MARKER_FLOOR + "=?", floor);
            }
            case MAPMARKERS_ID: {
                final String roomId = MapMarkers.getMarkerId(uri);
                return builder.table(Tables.MAPMARKERS)
                        .where(MapMarkers.MARKER_ID + "=?", roomId);
            }
            case MAPTILES: {
                return builder.table(Tables.MAPTILES);
            }
            case MAPTILES_FLOOR: {
                final String floor = MapTiles.getFloorId(uri);
                return builder.table(Tables.MAPTILES)
                        .where(MapTiles.TILE_FLOOR + "=?", floor);
            }
            case FEEDBACK_FOR_SESSION: {
                final String sessionId = Feedback.getSessionId(uri);
                return builder.table(Tables.FEEDBACK)

                        .where(Feedback.SESSION_ID + "=?", sessionId);
            }
            case FEEDBACK_ALL: {
                return builder.table(Tables.FEEDBACK);
            }
            case EXPERTS: {
                return builder.table(Tables.EXPERTS);
            }
            case EXPERTS_ID: {
                String expertId = Experts.getExpertId(uri);
                return builder.table(Tables.EXPERTS)
                        .where(Experts.EXPERT_ID + "= ?", expertId);
            }
            case HASHTAGS: {
                return builder.table(Tables.HASHTAGS);
            }
            case HASHTAGS_NAME: {
                final String hashtagName = Hashtags.getHashtagName(uri);
                return builder.table(Tables.HASHTAGS)
                        .where(HashtagColumns.HASHTAG_NAME + "=?", hashtagName);
            }
            case PEOPLE_IVE_MET: {
                return builder.table(Tables.PEOPLE_IVE_MET);
            }
            case PEOPLE_IVE_MET_ID: {
                String personId = ScheduleContract.PeopleIveMet.getPersonId(uri);
                return builder.table(Tables.PEOPLE_IVE_MET)
                        .where(PeopleIveMet.PERSON_ID + "=?", personId);
            }
            case VIDEOS: {
                return builder.table(Tables.VIDEOS);
            }
            case VIDEOS_ID: {
                final String videoId = Videos.getVideoId(uri);
                return builder.table(Tables.VIDEOS)
                        .where(VideoColumns.VIDEO_ID + "=?", videoId);
            }
            case PARTNERS: {
                return builder.table(Tables.PARTNERS);
            }
            case PARTNERS_ID: {
                final String partnerId = Partners.getPartnerId(uri);
                return builder.table(Tables.PARTNERS).where(Partners.PARTNER_ID + "=?", partnerId);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    private interface Subquery {

        String SESSION_HAS_GIVEN_FEEDBACK = "(SELECT COUNT(1) FROM "
                + Tables.FEEDBACK + " WHERE " + Qualified.FEEDBACK_SESSION_ID + "="
                + Qualified.SESSIONS_SESSION_ID + ")";
        String SESSIONS_SNIPPET = "snippet(" + Tables.SESSIONS_SEARCH + ",'{','}','\u2026')";
    }

    /**
     * {@link ScheduleContract} fields that are fully qualified with a specific
     * parent {@link Tables}. Used when needed to work around SQL ambiguity.
     */
    private interface Qualified {
        String SESSIONS_SESSION_ID = Tables.SESSIONS + "." + Sessions.SESSION_ID;
        String SESSIONS_ROOM_ID = Tables.SESSIONS + "." + Sessions.ROOM_ID;

        String SESSIONS_TAGS_SESSION_ID = Tables.SESSIONS_TAGS + "."
                + ScheduleDatabase.SessionsTags.SESSION_ID;

        String SESSIONS_SPEAKERS_SESSION_ID = Tables.SESSIONS_SPEAKERS + "."
                + SessionsSpeakers.SESSION_ID;
        String SESSIONS_SPEAKERS_SPEAKER_ID = Tables.SESSIONS_SPEAKERS + "."
                + SessionsSpeakers.SPEAKER_ID;
        String FEEDBACK_SESSION_ID = Tables.FEEDBACK + "." + Feedback.SESSION_ID;
    }
}
