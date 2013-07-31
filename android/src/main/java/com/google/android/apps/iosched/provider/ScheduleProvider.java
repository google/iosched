/*
 * Copyright 2012 Google Inc.
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

package com.google.android.apps.iosched.provider;

import com.google.android.apps.iosched.appwidget.ScheduleWidgetProvider;
import com.google.android.apps.iosched.provider.ScheduleContract.Announcements;
import com.google.android.apps.iosched.provider.ScheduleContract.Blocks;
import com.google.android.apps.iosched.provider.ScheduleContract.Feedback;
import com.google.android.apps.iosched.provider.ScheduleContract.MapMarkers;
import com.google.android.apps.iosched.provider.ScheduleContract.MapTiles;
import com.google.android.apps.iosched.provider.ScheduleContract.Rooms;
import com.google.android.apps.iosched.provider.ScheduleContract.SearchSuggest;
import com.google.android.apps.iosched.provider.ScheduleContract.Sessions;
import com.google.android.apps.iosched.provider.ScheduleContract.Speakers;
import com.google.android.apps.iosched.provider.ScheduleContract.Tracks;
import com.google.android.apps.iosched.provider.ScheduleContract.Sandbox;
import com.google.android.apps.iosched.provider.ScheduleDatabase.SessionsSearchColumns;
import com.google.android.apps.iosched.provider.ScheduleDatabase.SessionsSpeakers;
import com.google.android.apps.iosched.provider.ScheduleDatabase.SessionsTracks;
import com.google.android.apps.iosched.provider.ScheduleDatabase.Tables;
import com.google.android.apps.iosched.util.SelectionBuilder;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.text.TextUtils;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.android.apps.iosched.util.LogUtils.LOGV;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Provider that stores {@link ScheduleContract} data. Data is usually inserted
 * by {@link com.google.android.apps.iosched.sync.SyncHelper}, and queried by various
 * {@link Activity} instances.
 */
public class ScheduleProvider extends ContentProvider {
    private static final String TAG = makeLogTag(ScheduleProvider.class);

    private ScheduleDatabase mOpenHelper;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final int BLOCKS = 100;
    private static final int BLOCKS_BETWEEN = 101;
    private static final int BLOCKS_ID = 102;
    private static final int BLOCKS_ID_SESSIONS = 103;
    private static final int BLOCKS_ID_SESSIONS_STARRED = 104;

    private static final int TRACKS = 200;
    private static final int TRACKS_ID = 201;
    private static final int TRACKS_ID_SESSIONS = 202;
    private static final int TRACKS_ID_SANDBOX = 203;

    private static final int ROOMS = 300;
    private static final int ROOMS_ID = 301;
    private static final int ROOMS_ID_SESSIONS = 302;

    private static final int SESSIONS = 400;
    private static final int SESSIONS_STARRED = 401;
    private static final int SESSIONS_WITH_TRACK = 402;
    private static final int SESSIONS_SEARCH = 403;
    private static final int SESSIONS_AT = 404;
    private static final int SESSIONS_ID = 405;
    private static final int SESSIONS_ID_SPEAKERS = 406;
    private static final int SESSIONS_ID_TRACKS = 407;
    private static final int SESSIONS_ID_WITH_TRACK = 408;
    private static final int SESSIONS_ROOM_AFTER = 410;

    private static final int SPEAKERS = 500;
    private static final int SPEAKERS_ID = 501;
    private static final int SPEAKERS_ID_SESSIONS = 502;

    private static final int SANDBOX = 600;
    private static final int SANDBOX_SEARCH = 603;
    private static final int SANDBOX_ID = 604;

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
        matcher.addURI(authority, "blocks/*/sessions", BLOCKS_ID_SESSIONS);
        matcher.addURI(authority, "blocks/*/sessions/starred", BLOCKS_ID_SESSIONS_STARRED);

        matcher.addURI(authority, "tracks", TRACKS);
        matcher.addURI(authority, "tracks/*", TRACKS_ID);
        matcher.addURI(authority, "tracks/*/sessions", TRACKS_ID_SESSIONS);
        matcher.addURI(authority, "tracks/*/sandbox", TRACKS_ID_SANDBOX);

        matcher.addURI(authority, "rooms", ROOMS);
        matcher.addURI(authority, "rooms/*", ROOMS_ID);
        matcher.addURI(authority, "rooms/*/sessions", ROOMS_ID_SESSIONS);

        matcher.addURI(authority, "sessions", SESSIONS);
        matcher.addURI(authority, "sessions/starred", SESSIONS_STARRED);
        matcher.addURI(authority, "sessions/with_track", SESSIONS_WITH_TRACK);
        matcher.addURI(authority, "sessions/search/*", SESSIONS_SEARCH);
        matcher.addURI(authority, "sessions/at/*", SESSIONS_AT);
        matcher.addURI(authority, "sessions/room/*/after/*", SESSIONS_ROOM_AFTER);
        matcher.addURI(authority, "sessions/*", SESSIONS_ID);
        matcher.addURI(authority, "sessions/*/speakers", SESSIONS_ID_SPEAKERS);
        matcher.addURI(authority, "sessions/*/tracks", SESSIONS_ID_TRACKS);
        matcher.addURI(authority, "sessions/*/with_track", SESSIONS_ID_WITH_TRACK);

        matcher.addURI(authority, "speakers", SPEAKERS);
        matcher.addURI(authority, "speakers/*", SPEAKERS_ID);
        matcher.addURI(authority, "speakers/*/sessions", SPEAKERS_ID_SESSIONS);

        matcher.addURI(authority, "sandbox", SANDBOX);
        matcher.addURI(authority, "sandbox/search/*", SANDBOX_SEARCH);
        matcher.addURI(authority, "sandbox/*", SANDBOX_ID);

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
            case BLOCKS_ID_SESSIONS:
                return Sessions.CONTENT_TYPE;
            case BLOCKS_ID_SESSIONS_STARRED:
                return Sessions.CONTENT_TYPE;
            case TRACKS:
                return Tracks.CONTENT_TYPE;
            case TRACKS_ID:
                return Tracks.CONTENT_ITEM_TYPE;
            case TRACKS_ID_SESSIONS:
                return Sessions.CONTENT_TYPE;
            case TRACKS_ID_SANDBOX:
                return Sandbox.CONTENT_TYPE;
            case ROOMS:
                return Rooms.CONTENT_TYPE;
            case ROOMS_ID:
                return Rooms.CONTENT_ITEM_TYPE;
            case ROOMS_ID_SESSIONS:
                return Sessions.CONTENT_TYPE;
            case SESSIONS:
                return Sessions.CONTENT_TYPE;
            case SESSIONS_STARRED:
                return Sessions.CONTENT_TYPE;
            case SESSIONS_WITH_TRACK:
                return Sessions.CONTENT_TYPE;
            case SESSIONS_SEARCH:
                return Sessions.CONTENT_TYPE;
            case SESSIONS_AT:
                return Sessions.CONTENT_TYPE;
            case SESSIONS_ID:
                return Sessions.CONTENT_ITEM_TYPE;
            case SESSIONS_ID_SPEAKERS:
                return Speakers.CONTENT_TYPE;
            case SESSIONS_ID_TRACKS:
                return Tracks.CONTENT_TYPE;
            case SESSIONS_ID_WITH_TRACK:
                return Sessions.CONTENT_TYPE;
            case SESSIONS_ROOM_AFTER:
            	return Sessions.CONTENT_TYPE;
            case SPEAKERS:
                return Speakers.CONTENT_TYPE;
            case SPEAKERS_ID:
                return Speakers.CONTENT_ITEM_TYPE;
            case SPEAKERS_ID_SESSIONS:
                return Sessions.CONTENT_TYPE;
            case SANDBOX:
                return Sandbox.CONTENT_TYPE;
            case SANDBOX_SEARCH:
                return ScheduleContract.Sandbox.CONTENT_TYPE;
            case SANDBOX_ID:
                return ScheduleContract.Sandbox.CONTENT_ITEM_TYPE;
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

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        LOGV(TAG, "query(uri=" + uri + ", proj=" + Arrays.toString(projection) + ")");
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        String uriFilter = uri.getQueryParameter(Sessions.QUERY_PARAMETER_FILTER);
        final int match = sUriMatcher.match(uri);
        switch (match) {
            default: {
                // Most cases are handled with simple SelectionBuilder
                final SelectionBuilder builder = buildExpandedSelection(uri, match);

                // If a special filter was specified, try to apply it
                if (!TextUtils.isEmpty(uriFilter)) {
                    if (Sessions.QUERY_VALUE_FILTER_SESSIONS_CODELABS_ONLY.equals(uriFilter)) {
                        builder.where(Sessions.SESSION_TYPE + " NOT IN ('"
                                + Sessions.SESSION_TYPE_OFFICE_HOURS + "','"
                                + Sessions.SESSION_TYPE_KEYNOTE + "')");
                    } else if (Sessions.QUERY_VALUE_FILTER_OFFICE_HOURS_ONLY.equals(uriFilter)) {
                        builder.where(Sessions.SESSION_TYPE + " = ?",
                                Sessions.SESSION_TYPE_OFFICE_HOURS);
                    }
                }

                return builder.where(selection, selectionArgs).query(db, projection, sortOrder);
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
                return builder.query(db, projection, null, null, SearchSuggest.DEFAULT_SORT, limit);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        LOGV(TAG, "insert(uri=" + uri + ", values=" + values.toString() + ")");
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        boolean syncToNetwork = !ScheduleContract.hasCallerIsSyncAdapterParameter(uri);
        switch (match) {
            case BLOCKS: {
                db.insertOrThrow(Tables.BLOCKS, null, values);
                notifyChange(uri, syncToNetwork);
                return Blocks.buildBlockUri(values.getAsString(Blocks.BLOCK_ID));
            }
            case TRACKS: {
                db.insertOrThrow(Tables.TRACKS, null, values);
                notifyChange(uri, syncToNetwork);
                return Tracks.buildTrackUri(values.getAsString(Tracks.TRACK_ID));
            }
            case ROOMS: {
                db.insertOrThrow(Tables.ROOMS, null, values);
                notifyChange(uri, syncToNetwork);
                return Rooms.buildRoomUri(values.getAsString(Rooms.ROOM_ID));
            }
            case SESSIONS: {
                db.insertOrThrow(Tables.SESSIONS, null, values);
                notifyChange(uri, syncToNetwork);
                return Sessions.buildSessionUri(values.getAsString(Sessions.SESSION_ID));
            }
            case SESSIONS_ID_SPEAKERS: {
                db.insertOrThrow(Tables.SESSIONS_SPEAKERS, null, values);
                notifyChange(uri, syncToNetwork);
                return Speakers.buildSpeakerUri(values.getAsString(SessionsSpeakers.SPEAKER_ID));
            }
            case SESSIONS_ID_TRACKS: {
                db.insertOrThrow(Tables.SESSIONS_TRACKS, null, values);
                notifyChange(uri, syncToNetwork);
                return Tracks.buildTrackUri(values.getAsString(SessionsTracks.TRACK_ID));
            }
            case SPEAKERS: {
                db.insertOrThrow(Tables.SPEAKERS, null, values);
                notifyChange(uri, syncToNetwork);
                return Speakers.buildSpeakerUri(values.getAsString(Speakers.SPEAKER_ID));
            }
            case SANDBOX: {
                db.insertOrThrow(Tables.SANDBOX, null, values);
                notifyChange(uri, syncToNetwork);
                return Sandbox.buildCompanyUri(values.getAsString(Sandbox.COMPANY_ID));
            }
            case ANNOUNCEMENTS: {
                db.insertOrThrow(Tables.ANNOUNCEMENTS, null, values);
                notifyChange(uri, syncToNetwork);
                return Announcements.buildAnnouncementUri(values
                        .getAsString(Announcements.ANNOUNCEMENT_ID));
            }
            case SEARCH_SUGGEST: {
                db.insertOrThrow(Tables.SEARCH_SUGGEST, null, values);
                notifyChange(uri, syncToNetwork);
                return SearchSuggest.CONTENT_URI;
            }
            case MAPMARKERS: {
                db.insertOrThrow(Tables.MAPMARKERS, null, values);
                notifyChange(uri, syncToNetwork);
                return MapMarkers.buildMarkerUri(values.getAsString(MapMarkers.MARKER_ID));
            }
            case MAPTILES: {
                db.insertOrThrow(Tables.MAPTILES, null, values);
                notifyChange(uri, syncToNetwork);
                return MapTiles.buildFloorUri(values.getAsString(MapTiles.TILE_FLOOR));
            }
            case FEEDBACK_FOR_SESSION: {
                db.insertOrThrow(Tables.FEEDBACK, null, values);
                notifyChange(uri, syncToNetwork);
                return Feedback.buildFeedbackUri(values.getAsString(Feedback.SESSION_ID));
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        LOGV(TAG, "update(uri=" + uri + ", values=" + values.toString() + ")");
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        if (match == SEARCH_INDEX) {
            // update the search index
            ScheduleDatabase.updateSessionSearchIndex(db);
            return 1;
        }

        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs).update(db, values);
        boolean syncToNetwork = !ScheduleContract.hasCallerIsSyncAdapterParameter(uri);
        notifyChange(uri, syncToNetwork);
        return retVal;
    }

    /** {@inheritDoc} */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        LOGV(TAG, "delete(uri=" + uri + ")");
        if (uri == ScheduleContract.BASE_CONTENT_URI) {
            // Handle whole database deletes (e.g. when signing out)
            deleteDatabase();
            notifyChange(uri, false);
            return 1;
        }
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs).delete(db);
        notifyChange(uri, !ScheduleContract.hasCallerIsSyncAdapterParameter(uri));
        return retVal;
    }

    private void notifyChange(Uri uri, boolean syncToNetwork) {
        Context context = getContext();
        context.getContentResolver().notifyChange(uri, null, syncToNetwork);

        // Widgets can't register content observers so we refresh widgets separately.
        context.sendBroadcast(ScheduleWidgetProvider.getRefreshBroadcastIntent(context, false));
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
            case TRACKS: {
                return builder.table(Tables.TRACKS);
            }
            case TRACKS_ID: {
                final String trackId = Tracks.getTrackId(uri);
                return builder.table(Tables.TRACKS)
                        .where(Tracks.TRACK_ID + "=?", trackId);
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
            case SESSIONS_ID_TRACKS: {
                final String sessionId = Sessions.getSessionId(uri);
                return builder.table(Tables.SESSIONS_TRACKS)
                        .where(Sessions.SESSION_ID + "=?", sessionId);
            }
            case SPEAKERS: {
                return builder.table(Tables.SPEAKERS);
            }
            case SPEAKERS_ID: {
                final String speakerId = Speakers.getSpeakerId(uri);
                return builder.table(Tables.SPEAKERS)
                        .where(Speakers.SPEAKER_ID + "=?", speakerId);
            }
            case SANDBOX: {
                return builder.table(Tables.SANDBOX);
            }
            case SANDBOX_ID: {
                final String companyId = ScheduleContract.Sandbox.getCompanyId(uri);
                return builder.table(Tables.SANDBOX)
                        .where(Sandbox.COMPANY_ID + "=?", companyId);
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
                        .where(MapMarkers.MARKER_ID+ "=?", markerId);
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
            default: {
                throw new UnsupportedOperationException("Unknown uri for " + match + ": " + uri);
            }
        }
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
                return builder
                        .table(Tables.BLOCKS)
                        .map(Blocks.SESSIONS_COUNT, Subquery.BLOCK_SESSIONS_COUNT)
                        .map(Blocks.NUM_STARRED_SESSIONS, Subquery.BLOCK_NUM_STARRED_SESSIONS)
                        .map(Blocks.NUM_LIVESTREAMED_SESSIONS,
                                Subquery.BLOCK_NUM_LIVESTREAMED_SESSIONS)
                        .map(Blocks.STARRED_SESSION_ID, Subquery.BLOCK_STARRED_SESSION_ID)
                        .map(Blocks.STARRED_SESSION_TITLE, Subquery.BLOCK_STARRED_SESSION_TITLE)
                        .map(Blocks.STARRED_SESSION_HASHTAGS,
                                Subquery.BLOCK_STARRED_SESSION_HASHTAGS)
                        .map(Blocks.STARRED_SESSION_URL, Subquery.BLOCK_STARRED_SESSION_URL)
                        .map(Blocks.STARRED_SESSION_LIVESTREAM_URL,
                                Subquery.BLOCK_STARRED_SESSION_LIVESTREAM_URL)
                        .map(Blocks.STARRED_SESSION_ROOM_NAME,
                                Subquery.BLOCK_STARRED_SESSION_ROOM_NAME)
                        .map(Blocks.STARRED_SESSION_ROOM_ID, Subquery.BLOCK_STARRED_SESSION_ROOM_ID);
            }
            case BLOCKS_BETWEEN: {
                final List<String> segments = uri.getPathSegments();
                final String startTime = segments.get(2);
                final String endTime = segments.get(3);
                return builder.table(Tables.BLOCKS)
                        .map(Blocks.SESSIONS_COUNT, Subquery.BLOCK_SESSIONS_COUNT)
                        .map(Blocks.NUM_STARRED_SESSIONS, Subquery.BLOCK_NUM_STARRED_SESSIONS)
                        .map(Blocks.NUM_LIVESTREAMED_SESSIONS,
                                Subquery.BLOCK_NUM_LIVESTREAMED_SESSIONS)
                        .where(Blocks.BLOCK_START + ">=?", startTime)
                        .where(Blocks.BLOCK_START + "<=?", endTime);
            }
            case BLOCKS_ID: {
                final String blockId = Blocks.getBlockId(uri);
                return builder.table(Tables.BLOCKS)
                        .map(Blocks.SESSIONS_COUNT, Subquery.BLOCK_SESSIONS_COUNT)
                        .map(Blocks.NUM_STARRED_SESSIONS, Subquery.BLOCK_NUM_STARRED_SESSIONS)
                        .map(Blocks.NUM_LIVESTREAMED_SESSIONS,
                                Subquery.BLOCK_NUM_LIVESTREAMED_SESSIONS)
                        .where(Blocks.BLOCK_ID + "=?", blockId);
            }
            case BLOCKS_ID_SESSIONS: {
                final String blockId = Blocks.getBlockId(uri);
                return builder.table(Tables.SESSIONS_JOIN_BLOCKS_ROOMS)
                        .map(Blocks.SESSIONS_COUNT, Subquery.BLOCK_SESSIONS_COUNT)
                        .map(Blocks.NUM_STARRED_SESSIONS, Subquery.BLOCK_NUM_STARRED_SESSIONS)
                        .map(Blocks.NUM_LIVESTREAMED_SESSIONS,
                                Subquery.BLOCK_NUM_LIVESTREAMED_SESSIONS)
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .where(Qualified.SESSIONS_BLOCK_ID + "=?", blockId);
            }
            case BLOCKS_ID_SESSIONS_STARRED: {
                final String blockId = Blocks.getBlockId(uri);
                return builder.table(Tables.SESSIONS_JOIN_BLOCKS_ROOMS)
                        .map(Blocks.SESSIONS_COUNT, Subquery.BLOCK_SESSIONS_COUNT)
                        .map(Blocks.NUM_STARRED_SESSIONS, Subquery.BLOCK_NUM_STARRED_SESSIONS)
                        .map(Blocks.NUM_LIVESTREAMED_SESSIONS,
                                Subquery.BLOCK_NUM_LIVESTREAMED_SESSIONS)
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .where(Qualified.SESSIONS_BLOCK_ID + "=?", blockId)
                        .where(Qualified.SESSIONS_STARRED + "=1");
            }
            case TRACKS: {
                return builder.table(Tables.TRACKS)
                        .map(Tracks.SESSIONS_COUNT, Subquery.TRACK_SESSIONS_COUNT)
                        .map(Tracks.OFFICE_HOURS_COUNT, Subquery.TRACK_OFFICE_HOURS_COUNT)
                        .map(Tracks.SANDBOX_COUNT, Subquery.TRACK_SANDBOX_COUNT);
            }
            case TRACKS_ID: {
                final String trackId = Tracks.getTrackId(uri);
                return builder.table(Tables.TRACKS)
                        .where(Tracks.TRACK_ID + "=?", trackId);
            }
            case TRACKS_ID_SESSIONS: {
                final String trackId = Tracks.getTrackId(uri);
                return builder.table(Tables.SESSIONS_TRACKS_JOIN_SESSIONS_BLOCKS_ROOMS)
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .where(Qualified.SESSIONS_TRACKS_TRACK_ID + "=?", trackId);
            }
            case TRACKS_ID_SANDBOX: {
                final String trackId = Tracks.getTrackId(uri);
                return builder.table(Tables.SANDBOX_JOIN_TRACKS_BLOCKS_ROOMS)
                        .mapToTable(ScheduleContract.Sandbox._ID, Tables.SANDBOX)
                        .mapToTable(Sandbox.TRACK_ID, Tables.SANDBOX)
                        .mapToTable(ScheduleContract.Sandbox.BLOCK_ID, Tables.BLOCKS)
                        .mapToTable(Sandbox.ROOM_ID, Tables.ROOMS)
                        .where(Qualified.SANDBOX_TRACK_ID + "=?", trackId);
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
                return builder.table(Tables.SESSIONS_JOIN_BLOCKS_ROOMS)
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .where(Qualified.SESSIONS_ROOM_ID + "=?", roomId);
            }
            case SESSIONS: {
                return builder.table(Tables.SESSIONS_JOIN_BLOCKS_ROOMS)
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS);
            }
            case SESSIONS_STARRED: {
                return builder.table(Tables.SESSIONS_JOIN_BLOCKS_ROOMS)
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .where(Sessions.SESSION_STARRED + "=1");
            }
            case SESSIONS_WITH_TRACK: {
                return builder.table(Tables.SESSIONS_JOIN_TRACKS_JOIN_BLOCKS)
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
                        .mapToTable(Tracks.TRACK_ID, Tables.TRACKS)
                        .mapToTable(Tracks.TRACK_NAME, Tables.TRACKS)
                        .mapToTable(Blocks.BLOCK_ID, Tables.BLOCKS);
            }
            case SESSIONS_ID_WITH_TRACK: {
                final String sessionId = Sessions.getSessionId(uri);
                return builder.table(Tables.SESSIONS_JOIN_TRACKS_JOIN_BLOCKS)
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
                        .mapToTable(Tracks.TRACK_ID, Tables.TRACKS)
                        .mapToTable(Tracks.TRACK_NAME, Tables.TRACKS)
                        .mapToTable(Blocks.BLOCK_ID, Tables.BLOCKS)
                        .where(Qualified.SESSIONS_SESSION_ID + "=?", sessionId);
            }
            case SESSIONS_SEARCH: {
                final String query = Sessions.getSearchQuery(uri);
                return builder.table(Tables.SESSIONS_SEARCH_JOIN_SESSIONS_BLOCKS_ROOMS)
                        .map(Sessions.SEARCH_SNIPPET, Subquery.SESSIONS_SNIPPET)
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .where(SessionsSearchColumns.BODY + " MATCH ?", query);
            }
            case SESSIONS_AT: {
                final List<String> segments = uri.getPathSegments();
                final String time = segments.get(2);
                return builder.table(Tables.SESSIONS_JOIN_BLOCKS_ROOMS)
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .where(Sessions.BLOCK_START + "<=?", time)
                        .where(Sessions.BLOCK_END + ">=?", time);
            }
            case SESSIONS_ID: {
                final String sessionId = Sessions.getSessionId(uri);
                return builder.table(Tables.SESSIONS_JOIN_BLOCKS_ROOMS)
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .where(Qualified.SESSIONS_SESSION_ID + "=?", sessionId);
            }
            case SESSIONS_ID_SPEAKERS: {
                final String sessionId = Sessions.getSessionId(uri);
                return builder.table(Tables.SESSIONS_SPEAKERS_JOIN_SPEAKERS)
                        .mapToTable(Speakers._ID, Tables.SPEAKERS)
                        .mapToTable(Speakers.SPEAKER_ID, Tables.SPEAKERS)
                        .where(Qualified.SESSIONS_SPEAKERS_SESSION_ID + "=?", sessionId);
            }
            case SESSIONS_ID_TRACKS: {
                final String sessionId = Sessions.getSessionId(uri);
                return builder.table(Tables.SESSIONS_TRACKS_JOIN_TRACKS)
                        .mapToTable(Tracks._ID, Tables.TRACKS)
                        .mapToTable(Tracks.TRACK_ID, Tables.TRACKS)
                        .where(Qualified.SESSIONS_TRACKS_SESSION_ID + "=?", sessionId);
            }
            case SESSIONS_ROOM_AFTER: {
				final String room = Sessions.getRoom(uri);
				final String time = Sessions.getAfter(uri);
				return builder.table(Tables.SESSIONS_JOIN_BLOCKS_ROOMS)
						.mapToTable(Sessions._ID, Tables.SESSIONS)
						.mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
						.mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
						.where(Qualified.SESSIONS_ROOM_ID+ "=?", room)
						.where("("+Sessions.BLOCK_START + "<= ? AND "+Sessions.BLOCK_END+">= ?) OR ("+Sessions.BLOCK_START+" >= ?)", time,time,time);
           }
            case SPEAKERS: {
                return builder.table(Tables.SPEAKERS);
            }
            case SPEAKERS_ID: {
                final String speakerId = Speakers.getSpeakerId(uri);
                return builder.table(Tables.SPEAKERS)
                        .where(Speakers.SPEAKER_ID + "=?", speakerId);
            }
            case SPEAKERS_ID_SESSIONS: {
                final String speakerId = Speakers.getSpeakerId(uri);
                return builder.table(Tables.SESSIONS_SPEAKERS_JOIN_SESSIONS_BLOCKS_ROOMS)
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .where(Qualified.SESSIONS_SPEAKERS_SPEAKER_ID + "=?", speakerId);
            }
            case SANDBOX: {
                return builder.table(Tables.SANDBOX_JOIN_TRACKS_BLOCKS_ROOMS)
                        .mapToTable(Sandbox._ID, Tables.SANDBOX)
                        .mapToTable(Sandbox.TRACK_ID, Tables.SANDBOX)
                        .mapToTable(Sandbox.BLOCK_ID, Tables.SANDBOX)
                        .mapToTable(Sandbox.ROOM_ID, Tables.SANDBOX);
            }
            case SANDBOX_ID: {
                final String companyId = ScheduleContract.Sandbox.getCompanyId(uri);
                return builder.table(Tables.SANDBOX_JOIN_TRACKS_BLOCKS_ROOMS)
                        .mapToTable(ScheduleContract.Sandbox._ID, Tables.SANDBOX)
                        .mapToTable(Sandbox.TRACK_ID, Tables.SANDBOX)
                        .mapToTable(Sandbox.BLOCK_ID, Tables.SANDBOX)
                        .mapToTable(Sandbox.ROOM_ID, Tables.SANDBOX)
                        .where(Sandbox.COMPANY_ID + "=?", companyId);
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
                final String roomId = MapMarkers.getMarkerId(uri);
                return builder.table(Tables.MAPMARKERS)
                        .where(MapMarkers.MARKER_ID+ "=?", roomId);
            }
            case MAPTILES: {
                return builder.table(Tables.MAPTILES);
            }
            case MAPTILES_FLOOR: {
                final String floor = MapTiles.getFloorId(uri);
                return builder.table(Tables.MAPTILES)
                        .where(MapTiles.TILE_FLOOR+ "=?", floor);
            }
            case FEEDBACK_FOR_SESSION: {
                final String sessionId = Feedback.getSessionId(uri);
                return builder.table(Tables.FEEDBACK)

                        .where(Feedback.SESSION_ID + "=?", sessionId);
            }
            case FEEDBACK_ALL: {
                return builder.table(Tables.FEEDBACK);
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
        String BLOCK_SESSIONS_COUNT = "(SELECT COUNT(" + Qualified.SESSIONS_SESSION_ID + ") FROM "
                + Tables.SESSIONS + " WHERE " + Qualified.SESSIONS_BLOCK_ID + "="
                + Qualified.BLOCKS_BLOCK_ID + ")";

        String BLOCK_NUM_STARRED_SESSIONS = "(SELECT COUNT(1) FROM "
                + Tables.SESSIONS + " WHERE " + Qualified.SESSIONS_BLOCK_ID + "="
                + Qualified.BLOCKS_BLOCK_ID + " AND " + Qualified.SESSIONS_STARRED + "=1)";

        String BLOCK_NUM_LIVESTREAMED_SESSIONS = "(SELECT COUNT(1) FROM "
                + Tables.SESSIONS + " WHERE " + Qualified.SESSIONS_BLOCK_ID + "="
                + Qualified.BLOCKS_BLOCK_ID
                + " AND IFNULL(" + Qualified.SESSIONS_LIVESTREAM_URL + ",'')!='')";

        String BLOCK_STARRED_SESSION_ID = "(SELECT " + Qualified.SESSIONS_SESSION_ID + " FROM "
                + Tables.SESSIONS + " WHERE " + Qualified.SESSIONS_BLOCK_ID + "="
                + Qualified.BLOCKS_BLOCK_ID + " AND " + Qualified.SESSIONS_STARRED + "=1 "
                + "ORDER BY " + Qualified.SESSIONS_TITLE + ")";

        String BLOCK_STARRED_SESSION_TITLE = "(SELECT " + Qualified.SESSIONS_TITLE + " FROM "
                + Tables.SESSIONS + " WHERE " + Qualified.SESSIONS_BLOCK_ID + "="
                + Qualified.BLOCKS_BLOCK_ID + " AND " + Qualified.SESSIONS_STARRED + "=1 "
                + "ORDER BY " + Qualified.SESSIONS_TITLE + ")";

        String BLOCK_STARRED_SESSION_HASHTAGS = "(SELECT " + Qualified.SESSIONS_HASHTAGS + " FROM "
                + Tables.SESSIONS + " WHERE " + Qualified.SESSIONS_BLOCK_ID + "="
                + Qualified.BLOCKS_BLOCK_ID + " AND " + Qualified.SESSIONS_STARRED + "=1 "
                + "ORDER BY " + Qualified.SESSIONS_TITLE + ")";

        String BLOCK_STARRED_SESSION_URL = "(SELECT " + Qualified.SESSIONS_URL + " FROM "
                + Tables.SESSIONS + " WHERE " + Qualified.SESSIONS_BLOCK_ID + "="
                + Qualified.BLOCKS_BLOCK_ID + " AND " + Qualified.SESSIONS_STARRED + "=1 "
                + "ORDER BY " + Qualified.SESSIONS_TITLE + ")";

        String BLOCK_STARRED_SESSION_LIVESTREAM_URL = "(SELECT "
                + Qualified.SESSIONS_LIVESTREAM_URL
                + " FROM "
                + Tables.SESSIONS + " WHERE " + Qualified.SESSIONS_BLOCK_ID + "="
                + Qualified.BLOCKS_BLOCK_ID + " AND " + Qualified.SESSIONS_STARRED + "=1 "
                + "ORDER BY " + Qualified.SESSIONS_TITLE + ")";

        String BLOCK_STARRED_SESSION_ROOM_NAME = "(SELECT " + Qualified.ROOMS_ROOM_NAME + " FROM "
                + Tables.SESSIONS_JOIN_ROOMS + " WHERE " + Qualified.SESSIONS_BLOCK_ID + "="
                + Qualified.BLOCKS_BLOCK_ID + " AND " + Qualified.SESSIONS_STARRED + "=1 "
                + "ORDER BY " + Qualified.SESSIONS_TITLE + ")";

        String BLOCK_STARRED_SESSION_ROOM_ID = "(SELECT " + Qualified.ROOMS_ROOM_ID + " FROM "
                + Tables.SESSIONS_JOIN_ROOMS + " WHERE " + Qualified.SESSIONS_BLOCK_ID + "="
                + Qualified.BLOCKS_BLOCK_ID + " AND " + Qualified.SESSIONS_STARRED + "=1 "
                + "ORDER BY " + Qualified.SESSIONS_TITLE + ")";

        String TRACK_SESSIONS_COUNT = "(SELECT COUNT(" + Qualified.SESSIONS_TRACKS_SESSION_ID
                + ") FROM " + Tables.SESSIONS_TRACKS + " INNER JOIN " + Tables.SESSIONS + " ON "
                + Qualified.SESSIONS_SESSION_ID + "=" + Qualified.SESSIONS_TRACKS_SESSION_ID + " WHERE "
                + Qualified.SESSIONS_TRACKS_TRACK_ID + "=" + Qualified.TRACKS_TRACK_ID
                + " AND " + Qualified.SESSIONS_SESSION_TYPE + " != \"" + Sessions.SESSION_TYPE_OFFICE_HOURS + "\")";

        String TRACK_OFFICE_HOURS_COUNT = "(SELECT COUNT(" + Qualified.SESSIONS_TRACKS_SESSION_ID
                + ") FROM " + Tables.SESSIONS_TRACKS + " INNER JOIN " + Tables.SESSIONS + " ON "
                + Qualified.SESSIONS_SESSION_ID + "=" + Qualified.SESSIONS_TRACKS_SESSION_ID + " WHERE "
                + Qualified.SESSIONS_TRACKS_TRACK_ID + "=" + Qualified.TRACKS_TRACK_ID
                + " AND " + Qualified.SESSIONS_SESSION_TYPE + " = \"" + Sessions.SESSION_TYPE_OFFICE_HOURS + "\")";

        String TRACK_SANDBOX_COUNT = "(SELECT COUNT(" + Qualified.SANDBOX_COMPANY_ID + ") FROM "
                + Tables.SANDBOX + " WHERE " + Qualified.SANDBOX_TRACK_ID + "="
                + Qualified.TRACKS_TRACK_ID + ")";

        String SESSIONS_SNIPPET = "snippet(" + Tables.SESSIONS_SEARCH + ",'{','}','\u2026')";
    }

    /**
     * {@link ScheduleContract} fields that are fully qualified with a specific
     * parent {@link Tables}. Used when needed to work around SQL ambiguity.
     */
    private interface Qualified {
        String SESSIONS_SESSION_ID = Tables.SESSIONS + "." + Sessions.SESSION_ID;
        String SESSIONS_SESSION_TYPE = Tables.SESSIONS+ "." + Sessions.SESSION_TYPE;
        String SESSIONS_BLOCK_ID = Tables.SESSIONS + "." + Sessions.BLOCK_ID;
        String SESSIONS_ROOM_ID = Tables.SESSIONS + "." + Sessions.ROOM_ID;

        String SESSIONS_TRACKS_SESSION_ID = Tables.SESSIONS_TRACKS + "."
                + SessionsTracks.SESSION_ID;
        String SESSIONS_TRACKS_TRACK_ID = Tables.SESSIONS_TRACKS + "."
                + SessionsTracks.TRACK_ID;

        String SESSIONS_SPEAKERS_SESSION_ID = Tables.SESSIONS_SPEAKERS + "."
                + SessionsSpeakers.SESSION_ID;
        String SESSIONS_SPEAKERS_SPEAKER_ID = Tables.SESSIONS_SPEAKERS + "."
                + SessionsSpeakers.SPEAKER_ID;

        String SANDBOX_COMPANY_ID = Tables.SANDBOX + "." + Sandbox.COMPANY_ID;
        String SANDBOX_TRACK_ID = Tables.SANDBOX + "." + Sandbox.TRACK_ID;

        String SESSIONS_STARRED = Tables.SESSIONS + "." + Sessions.SESSION_STARRED;
        String SESSIONS_TITLE = Tables.SESSIONS + "." + Sessions.SESSION_TITLE;
        String SESSIONS_HASHTAGS = Tables.SESSIONS + "." + Sessions.SESSION_HASHTAGS;
        String SESSIONS_URL = Tables.SESSIONS + "." + Sessions.SESSION_URL;

        String SESSIONS_LIVESTREAM_URL = Tables.SESSIONS + "." + Sessions.SESSION_LIVESTREAM_URL;

        String ROOMS_ROOM_NAME = Tables.ROOMS + "." + Rooms.ROOM_NAME;
        String ROOMS_ROOM_ID = Tables.ROOMS + "." + Rooms.ROOM_ID;

        String TRACKS_TRACK_ID = Tables.TRACKS + "." + Tracks.TRACK_ID;
        String BLOCKS_BLOCK_ID = Tables.BLOCKS + "." + Blocks.BLOCK_ID;
    }
}
