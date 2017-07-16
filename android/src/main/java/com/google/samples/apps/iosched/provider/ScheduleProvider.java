/*
 * Copyright 2015 Google Inc. All rights reserved.
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

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.appwidget.ScheduleWidgetProvider;
import com.google.samples.apps.iosched.provider.ScheduleContract.Announcements;
import com.google.samples.apps.iosched.provider.ScheduleContract.Blocks;
import com.google.samples.apps.iosched.provider.ScheduleContract.Feedback;
import com.google.samples.apps.iosched.provider.ScheduleContract.HashtagColumns;
import com.google.samples.apps.iosched.provider.ScheduleContract.Hashtags;
import com.google.samples.apps.iosched.provider.ScheduleContract.MapMarkers;
import com.google.samples.apps.iosched.provider.ScheduleContract.MapTiles;
import com.google.samples.apps.iosched.provider.ScheduleContract.MyFeedbackSubmitted;
import com.google.samples.apps.iosched.provider.ScheduleContract.MySchedule;
import com.google.samples.apps.iosched.provider.ScheduleContract.MyScheduleColumns;
import com.google.samples.apps.iosched.provider.ScheduleContract.MyViewedVideos;
import com.google.samples.apps.iosched.provider.ScheduleContract.Rooms;
import com.google.samples.apps.iosched.provider.ScheduleContract.SearchSuggest;
import com.google.samples.apps.iosched.provider.ScheduleContract.SearchTopicsSessions;
import com.google.samples.apps.iosched.provider.ScheduleContract.Sessions;
import com.google.samples.apps.iosched.provider.ScheduleContract.Speakers;
import com.google.samples.apps.iosched.provider.ScheduleContract.Tags;
import com.google.samples.apps.iosched.provider.ScheduleContract.VideoColumns;
import com.google.samples.apps.iosched.provider.ScheduleContract.Videos;
import com.google.samples.apps.iosched.provider.ScheduleDatabase.SessionsSearchColumns;
import com.google.samples.apps.iosched.provider.ScheduleDatabase.SessionsSpeakers;
import com.google.samples.apps.iosched.provider.ScheduleDatabase.Tables;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.SelectionBuilder;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.LOGV;
import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * {@link android.content.ContentProvider} that stores {@link ScheduleContract} data. Data is
 * usually inserted by {@link com.google.samples.apps.iosched.sync.SyncHelper}, and queried using
 * {@link android.app.LoaderManager} pattern.
 */
public class ScheduleProvider extends ContentProvider {

    private static final String TAG = makeLogTag(ScheduleProvider.class);

    private ScheduleDatabase mOpenHelper;

    private ScheduleProviderUriMatcher mUriMatcher;

    /**
     * Providing important state information to be included in bug reports.
     *
     * !!! Remember !!! Any important data logged to {@code writer} shouldn't contain personally
     * identifiable information as it can be seen in bugreports.
     */
    @Override
    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        Context context = getContext();

        // Using try/catch block in case there are issues retrieving information to log.
        try {
            // Calling append in multiple calls is typically better than creating net new strings to
            // pass to method invocations.
            writer.print("Last sync attempted: ");
            writer.println(new java.util.Date(SettingsUtils.getLastSyncAttemptedTime(context)));
            writer.print("Last sync successful: ");
            writer.println(new java.util.Date(SettingsUtils.getLastSyncSucceededTime(context)));
            writer.print("Current sync interval: ");
            writer.println(SettingsUtils.getCurSyncInterval(context));
            writer.print("Is an account active: ");
            writer.println(AccountUtils.hasActiveAccount(context));
            boolean canGetAuthToken = !TextUtils.isEmpty(AccountUtils.getAuthToken(context));
            writer.print("Can an auth token be retrieved: ");
            writer.println(canGetAuthToken);

        } catch (Exception exception) {
            writer.append("Exception while dumping state: ");
            exception.printStackTrace(writer);
        }
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new ScheduleDatabase(getContext());
        mUriMatcher = new ScheduleProviderUriMatcher();
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
        ScheduleUriEnum matchingUriEnum = mUriMatcher.matchUri(uri);
        return matchingUriEnum.contentType;
    }

    /**
     * Returns a tuple of question marks. For example, if {@code count} is 3, returns "(?,?,?)".
     */
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

    /**
     * Adds the {@code tagsFilter} query parameter to the given {@code builder}. This query
     * parameter is used by the {@link com.google.samples.apps.iosched.explore.ExploreSessionsActivity}
     * when the user makes a selection containing multiple filters.
     */
    private void addTagsFilter(SelectionBuilder builder, String tagsFilter, String numCategories) {
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
            int categories = 1;
            if (numCategories != null && TextUtils.isDigitsOnly(numCategories)) {
                try {
                    categories = Integer.parseInt(numCategories);
                    LOGD(TAG, "Categories being used " + categories);
                } catch (Exception ex) {
                    LOGE(TAG, "exception parsing categories ", ex);
                }
            }
            String questionMarkTuple = makeQuestionMarkTuple(requiredTags.length);
            builder.where(Tags.TAG_ID + " IN " + questionMarkTuple, requiredTags);
            builder.having(
                    "COUNT(" + Qualified.SESSIONS_SESSION_ID + ") >= " + categories);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        String tagsFilter = uri.getQueryParameter(Sessions.QUERY_PARAMETER_TAG_FILTER);
        String categories = uri.getQueryParameter(Sessions.QUERY_PARAMETER_CATEGORIES);

        ScheduleUriEnum matchingUriEnum = mUriMatcher.matchUri(uri);

        // Avoid the expensive string concatenation below if not loggable.
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            LOGV(TAG, "uri=" + uri + " code=" + matchingUriEnum.code + " proj=" +
                    Arrays.toString(projection) + " selection=" + selection + " args="
                    + Arrays.toString(selectionArgs) + ")");
        }

        switch (matchingUriEnum) {
            default: {
                // Most cases are handled with simple SelectionBuilder.
                final SelectionBuilder builder = buildExpandedSelection(uri, matchingUriEnum.code);

                // If a special filter was specified, try to apply it.
                if (!TextUtils.isEmpty(tagsFilter) && !TextUtils.isEmpty(categories)) {
                    addTagsFilter(builder, tagsFilter, categories);
                }

                boolean distinct = ScheduleContractHelper.isQueryDistinct(uri);

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

                // Adjust incoming query to become SQL text match.
                selectionArgs[0] = selectionArgs[0] + "%";
                builder.table(Tables.SEARCH_SUGGEST);
                builder.where(selection, selectionArgs);
                builder.map(SearchManager.SUGGEST_COLUMN_QUERY,
                        SearchManager.SUGGEST_COLUMN_TEXT_1);

                projection = new String[]{
                        BaseColumns._ID,
                        SearchManager.SUGGEST_COLUMN_TEXT_1,
                        SearchManager.SUGGEST_COLUMN_QUERY
                };

                final String limit = uri.getQueryParameter(SearchManager.SUGGEST_PARAMETER_LIMIT);
                return builder.query(db, false, projection, SearchSuggest.DEFAULT_SORT, limit);
            }
            case SEARCH_TOPICS_SESSIONS: {
                if (selectionArgs == null || selectionArgs.length == 0) {
                    return createMergedSearchCursor(null, null);
                }
                String selectionArg = selectionArgs[0] == null ? "" : selectionArgs[0];
                // First we query the Tags table to find any tags that match the given query
                Cursor tags = query(Tags.CONTENT_URI, SearchTopicsSessions.TOPIC_TAG_PROJECTION,
                        SearchTopicsSessions.TOPIC_TAG_SELECTION,
                        new String[] {Config.Tags.CATEGORY_TRACK, selectionArg + "%"},
                        SearchTopicsSessions.TOPIC_TAG_SORT);
                // Then we query the sessions_search table and get a list of sessions that match
                // the given keywords.
                Cursor search = null;
                if (selectionArgs[0] != null) { // dont query if there was no selectionArg.
                    search = query(ScheduleContract.Sessions.buildSearchUri(selectionArg),
                            SearchTopicsSessions.SEARCH_SESSIONS_PROJECTION,
                            null, null,
                            ScheduleContract.Sessions.SORT_BY_TYPE_THEN_TIME);
                }
                // Now that we have two cursors, we merge the cursors and return a unified view
                // of the two result sets.
                return createMergedSearchCursor(tags, search);
            }
        }
    }

    /**
     * Create a {@link MatrixCursor} given the tags and search cursors.
     * @param tags Cursor with the projection {@link SearchTopicsSessions#TOPIC_TAG_PROJECTION}.
     * @param search Cursor with the projection
     *              {@link SearchTopicsSessions#SEARCH_SESSIONS_PROJECTION}.
     * @return Returns a MatrixCursor always with {@link SearchTopicsSessions#DEFAULT_PROJECTION}
     */
    private Cursor createMergedSearchCursor(Cursor tags, Cursor search) {
        // How big should our MatrixCursor be?
        int maxCount = (tags == null ? 0 : tags.getCount()) +
                (search == null ? 0 : search.getCount());

        MatrixCursor matrixCursor = new MatrixCursor(
                SearchTopicsSessions.DEFAULT_PROJECTION, maxCount);

        // Iterate over the tags cursor and add rows.
        if (tags != null && tags.moveToFirst()) {
            do {
                matrixCursor.addRow(
                        new Object[]{
                                tags.getLong(0),
                                tags.getString(1), /*tag_id*/
                                "{" + tags.getString(2) + "}", /*search_snippet*/
                                1}); /*is_topic_tag*/
            } while (tags.moveToNext());
        }
        // Iterate over the search cursor and add rows.
        if (search != null && search.moveToFirst()) {
            do {
                matrixCursor.addRow(
                        new Object[]{
                                search.getLong(0),
                                search.getString(1),
                                search.getString(2), /*search_snippet*/
                                0}); /*is_topic_tag*/
            } while (search.moveToNext());
        }
        return matrixCursor;
    }

    /** {@inheritDoc} */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        LOGV(TAG, "insert(uri=" + uri + ", values=" + values.toString()
                + ", account=" + getCurrentAccountName(uri, false) + ")");
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        ScheduleUriEnum matchingUriEnum = mUriMatcher.matchUri(uri);
        if (matchingUriEnum.table != null) {
            try {
                db.insertOrThrow(matchingUriEnum.table, null, values);
                notifyChange(uri);
            } catch (SQLiteConstraintException exception) {
                // Leaving this here as it's handy to to breakpoint on this throw when debugging a
                // bootstrap file issue.
                throw exception;
            }
        }

        switch (matchingUriEnum) {
            case BLOCKS: {
                return Blocks.buildBlockUri(values.getAsString(Blocks.BLOCK_ID));
            }
            case CARDS: {
                return ScheduleContract.Cards.buildCardUri(values.getAsString(
                        ScheduleContract.Cards.CARD_ID));
            }
            case TAGS: {
                return Tags.buildTagUri(values.getAsString(Tags.TAG_ID));
            }
            case ROOMS: {
                return Rooms.buildRoomUri(values.getAsString(Rooms.ROOM_ID));
            }
            case SESSIONS: {
                return Sessions.buildSessionUri(values.getAsString(Sessions.SESSION_ID));
            }
            case SESSIONS_ID_SPEAKERS: {
                return Speakers.buildSpeakerUri(values.getAsString(SessionsSpeakers.SPEAKER_ID));
            }
            case SESSIONS_ID_TAGS: {
                return Tags.buildTagUri(values.getAsString(Tags.TAG_ID));
            }
            case MY_SCHEDULE: {
                values.put(MySchedule.MY_SCHEDULE_ACCOUNT_NAME, getCurrentAccountName(uri, false));
                db.insertOrThrow(Tables.MY_SCHEDULE, null, values);
                notifyChange(uri);
                Uri sessionUri = Sessions.buildSessionUri(
                        values.getAsString(MyScheduleColumns.SESSION_ID));
                notifyChange(sessionUri);
                return sessionUri;
            }
            case MY_VIEWED_VIDEOS: {
                values.put(MyViewedVideos.MY_VIEWED_VIDEOS_ACCOUNT_NAME,
                        getCurrentAccountName(uri, false));
                db.insertOrThrow(Tables.MY_VIEWED_VIDEO, null, values);
                notifyChange(uri);
                Uri videoUri = Videos.buildVideoUri(
                        values.getAsString(MyViewedVideos.VIDEO_ID));
                notifyChange(videoUri);
                return videoUri;
            }
            case MY_FEEDBACK_SUBMITTED: {
                values.put(MyFeedbackSubmitted.MY_FEEDBACK_SUBMITTED_ACCOUNT_NAME,
                        getCurrentAccountName(uri, false));
                db.insertOrThrow(Tables.MY_FEEDBACK_SUBMITTED, null, values);
                notifyChange(uri);
                Uri sessionUri = Sessions.buildSessionUri(
                        values.getAsString(MyFeedbackSubmitted.SESSION_ID));
                notifyChange(sessionUri);
                return sessionUri;
            }
            case SPEAKERS: {
                return Speakers.buildSpeakerUri(values.getAsString(Speakers.SPEAKER_ID));
            }
            case ANNOUNCEMENTS: {
                return Announcements.buildAnnouncementUri(values
                        .getAsString(Announcements.ANNOUNCEMENT_ID));
            }
            case SEARCH_SUGGEST: {
                return SearchSuggest.CONTENT_URI;
            }
            case MAPMARKERS: {
                return MapMarkers.buildMarkerUri(values.getAsString(MapMarkers.MARKER_ID));
            }
            case MAPTILES: {
                return MapTiles.buildFloorUri(values.getAsString(MapTiles.TILE_FLOOR));
            }
            case FEEDBACK_FOR_SESSION: {
                return Feedback.buildFeedbackUri(values.getAsString(Feedback.SESSION_ID));
            }
            case HASHTAGS: {
                return Hashtags.buildHashtagUri(values.getAsString(Hashtags.HASHTAG_NAME));
            }
            case VIDEOS: {
                return Videos.buildVideoUri(values.getAsString(Videos.VIDEO_ID));
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
        ScheduleUriEnum matchingUriEnum = mUriMatcher.matchUri(uri);
        if (matchingUriEnum == ScheduleUriEnum.SEARCH_INDEX) {
            // update the search index
            ScheduleDatabase.updateSessionSearchIndex(db);
            return 1;
        }

        final SelectionBuilder builder = buildSimpleSelection(uri);
        if (matchingUriEnum == ScheduleUriEnum.MY_SCHEDULE) {
            values.remove(MySchedule.MY_SCHEDULE_ACCOUNT_NAME);
            builder.where(MySchedule.MY_SCHEDULE_ACCOUNT_NAME + "=?", accountName);
        }
        if (matchingUriEnum == ScheduleUriEnum.MY_VIEWED_VIDEOS) {
            values.remove(MyViewedVideos.MY_VIEWED_VIDEOS_ACCOUNT_NAME);
            builder.where(MyViewedVideos.MY_VIEWED_VIDEOS_ACCOUNT_NAME + "=?", accountName);
        }
        if (matchingUriEnum == ScheduleUriEnum.MY_FEEDBACK_SUBMITTED) {
            values.remove(MyFeedbackSubmitted.MY_FEEDBACK_SUBMITTED_ACCOUNT_NAME);
            builder.where(MyFeedbackSubmitted.MY_FEEDBACK_SUBMITTED_ACCOUNT_NAME + "=?",
                    accountName);
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
        if (uri == ScheduleContract.BASE_CONTENT_URI) {
            // Handle whole database deletes (e.g. when signing out)
            deleteDatabase();
            notifyChange(uri);
            return 1;
        }
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        ScheduleUriEnum matchingUriEnum = mUriMatcher.matchUri(uri);
        if (matchingUriEnum == ScheduleUriEnum.MY_SCHEDULE) {
            builder.where(MySchedule.MY_SCHEDULE_ACCOUNT_NAME + "=?", accountName);
        }
        if (matchingUriEnum == ScheduleUriEnum.MY_VIEWED_VIDEOS) {
            builder.where(MyViewedVideos.MY_VIEWED_VIDEOS_ACCOUNT_NAME + "=?", accountName);
        }
        if (matchingUriEnum == ScheduleUriEnum.MY_FEEDBACK_SUBMITTED) {
            builder.where(
                    MyFeedbackSubmitted.MY_FEEDBACK_SUBMITTED_ACCOUNT_NAME + "=?", accountName);
        }

        int retVal = builder.where(selection, selectionArgs).delete(db);
        notifyChange(uri);
        return retVal;
    }

    /**
     * Notifies the system that the given {@code uri} data has changed.
     * <p/>
     * We only notify changes if the uri wasn't called by the sync adapter, to avoid issuing a large
     * amount of notifications while doing a sync. The
     * {@link com.google.samples.apps.iosched.sync.ConferenceDataHandler} notifies all top level
     * conference paths once the conference data sync is done, and the
     * {@link com.google.samples.apps.iosched.sync.userdata.AbstractUserDataSyncHelper} notifies all
     * user data related paths once the user data sync is done.
     */
    private void notifyChange(Uri uri) {
        if (!ScheduleContractHelper.isUriCalledFromSyncAdapter(uri)) {
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
        ScheduleUriEnum matchingUriEnum = mUriMatcher.matchUri(uri);
        // The main Uris, corresponding to the root of each type of Uri, do not have any selection
        // criteria so the full table is used. The others apply a selection criteria.
        switch (matchingUriEnum) {
            case BLOCKS:
            case CARDS:
            case TAGS:
            case ROOMS:
            case SESSIONS:
            case SPEAKERS:
            case ANNOUNCEMENTS:
            case MAPMARKERS:
            case MAPTILES:
            case SEARCH_SUGGEST:
            case HASHTAGS:
            case VIDEOS:
                return builder.table(matchingUriEnum.table);
            case BLOCKS_ID: {
                final String blockId = Blocks.getBlockId(uri);
                return builder.table(Tables.BLOCKS)
                        .where(Blocks.BLOCK_ID + "=?", blockId);
            }
            case TAGS_ID: {
                final String tagId = Tags.getTagId(uri);
                return builder.table(Tables.TAGS)
                        .where(Tags.TAG_ID + "=?", tagId);
            }
            case ROOMS_ID: {
                final String roomId = Rooms.getRoomId(uri);
                return builder.table(Tables.ROOMS)
                        .where(Rooms.ROOM_ID + "=?", roomId);
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
                        .where(MySchedule.MY_SCHEDULE_ACCOUNT_NAME + "=?",
                                getCurrentAccountName(uri, false));
            }
            case MY_VIEWED_VIDEOS: {
                return builder.table(Tables.MY_VIEWED_VIDEO)
                        .where(MyViewedVideos.MY_VIEWED_VIDEOS_ACCOUNT_NAME + "=?",
                                getCurrentAccountName(uri, false));
            }
            case MY_FEEDBACK_SUBMITTED: {
                return builder.table(Tables.MY_FEEDBACK_SUBMITTED)
                        .where(MyFeedbackSubmitted.MY_FEEDBACK_SUBMITTED_ACCOUNT_NAME + "=?",
                                getCurrentAccountName(uri, false));
            }
            case SPEAKERS_ID: {
                final String speakerId = Speakers.getSpeakerId(uri);
                return builder.table(Tables.SPEAKERS)
                        .where(Speakers.SPEAKER_ID + "=?", speakerId);
            }
            case ANNOUNCEMENTS_ID: {
                final String announcementId = Announcements.getAnnouncementId(uri);
                return builder.table(Tables.ANNOUNCEMENTS)
                        .where(Announcements.ANNOUNCEMENT_ID + "=?", announcementId);
            }
            case MAPMARKERS_FLOOR: {
                final String floor = MapMarkers.getMarkerFloor(uri);
                return builder.table(Tables.MAPMARKERS)
                        .where(MapMarkers.MARKER_FLOOR + "=?", floor);
            }
            case MAPMARKERS_ID: {
                final String markerId = MapMarkers.getMarkerId(uri);
                return builder.table(Tables.MAPMARKERS)
                        .where(MapMarkers.MARKER_ID + "=?", markerId);
            }
            case MAPTILES_FLOOR: {
                final String floor = MapTiles.getFloorId(uri);
                return builder.table(Tables.MAPTILES)
                        .where(MapTiles.TILE_FLOOR + "=?", floor);
            }
            case FEEDBACK_FOR_SESSION: {
                final String session_id = Feedback.getSessionId(uri);
                return builder.table(Tables.FEEDBACK)
                        .where(Feedback.SESSION_ID + "=?", session_id);
            }
            case FEEDBACK_ALL: {
                return builder.table(Tables.FEEDBACK);
            }
            case HASHTAGS_NAME: {
                final String hashtagName = Hashtags.getHashtagName(uri);
                return builder.table(Tables.HASHTAGS)
                        .where(Hashtags.HASHTAG_NAME + "=?", hashtagName);
            }
            case VIDEOS_ID: {
                final String videoId = Videos.getVideoId(uri);
                return builder.table(Tables.VIDEOS).where(Videos.VIDEO_ID + "=?", videoId);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri for " + uri);
            }
        }
    }

    private String getCurrentAccountName(Uri uri, boolean sanitize) {
        String accountName = ScheduleContractHelper.getOverrideAccountName(uri);
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
        ScheduleUriEnum matchingUriEnum = mUriMatcher.matchCode(match);
        if (matchingUriEnum == null) {
            throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        switch (matchingUriEnum) {
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
            case CARDS: {
                return builder.table(Tables.CARDS);
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
                return builder.table(Tables.SESSIONS_JOIN_ROOMS, getCurrentAccountName(uri, true))
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .where(Qualified.SESSIONS_ROOM_ID + "=?", roomId)
                        .groupBy(Qualified.SESSIONS_SESSION_ID);
            }
            case SESSIONS: {
                // We query sessions on the joined table of sessions with rooms and tags.
                // Since there may be more than one tag per session, we GROUP BY session ID.
                // The starred sessions ("my schedule") are associated with a user, so we
                // use the current user to select them properly
                return builder
                        .table(Tables.SESSIONS_JOIN_ROOMS_TAGS, getCurrentAccountName(uri, true))
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
                        .map(Sessions.SESSION_IN_MY_SCHEDULE, "IFNULL(in_schedule, 0)")
                        .groupBy(Qualified.SESSIONS_SESSION_ID);
            }
            case SESSIONS_COUNTER: {
                return builder
                        .table(Tables.SESSIONS_JOIN_MYSCHEDULE, getCurrentAccountName(uri, true))
                        .map(Sessions.SESSION_INTERVAL_COUNT, "count(1)")
                        .map(Sessions.SESSION_IN_MY_SCHEDULE, "IFNULL(in_schedule, 0)")
                        .groupBy(Sessions.SESSION_START + ", " + Sessions.SESSION_END);
            }
            case SESSIONS_MY_SCHEDULE: {
                return builder.table(Tables.SESSIONS_JOIN_ROOMS_TAGS_FEEDBACK_MYSCHEDULE,
                        getCurrentAccountName(uri, true))
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
                return builder.table(Tables.SESSIONS_JOIN_ROOMS_TAGS_FEEDBACK_MYSCHEDULE,
                        getCurrentAccountName(uri, true))
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
                return builder.table(Tables.SESSIONS_SEARCH_JOIN_SESSIONS_ROOMS,
                        getCurrentAccountName(uri, true))
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
                final String time = Sessions.getAfterForRoom(uri);
                return builder.table(Tables.SESSIONS_JOIN_ROOMS_TAGS, getCurrentAccountName(uri, true))
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
                        .where(Qualified.SESSIONS_ROOM_ID + "=?", room)
                        .where("(" + Sessions.SESSION_START + "<= ? AND " + Sessions.SESSION_END +
                                        " >= ?) OR (" + Sessions.SESSION_START + " >= ?)", time,
                                time,
                                time)
                        .map(Sessions.SESSION_IN_MY_SCHEDULE, "IFNULL(in_schedule, 0)")
                        .groupBy(Qualified.SESSIONS_SESSION_ID);
            }
            case SESSIONS_AFTER: {
                final String time = Sessions.getAfter(uri);
                return builder.table(Tables.SESSIONS_JOIN_ROOMS_TAGS, getCurrentAccountName(uri, true))
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .map(Sessions.SESSION_IN_MY_SCHEDULE, "IFNULL(in_schedule, 0)")
                        .where("(" + Sessions.SESSION_START + "<= ? AND " + Sessions.SESSION_END +
                                        " >= ?) OR (" + Sessions.SESSION_START + " >= ?)", time,
                                time, time)
                        .groupBy(Qualified.SESSIONS_SESSION_ID);
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
                        .where(MySchedule.MY_SCHEDULE_ACCOUNT_NAME + "=?",
                                getCurrentAccountName(uri, true));
            }
            case MY_FEEDBACK_SUBMITTED: {
                // force a where condition to avoid leaking schedule info to another account
                // Note that, since SelectionBuilder always join multiple where calls using AND,
                // even if malicious code specifying additional conditions on account_name won't
                // be able to fetch data from a different account.
                return builder.table(Tables.MY_FEEDBACK_SUBMITTED)
                        .where(MyFeedbackSubmitted.MY_FEEDBACK_SUBMITTED_ACCOUNT_NAME + "=?",
                                getCurrentAccountName(uri, true));
            }
            case MY_VIEWED_VIDEOS: {
                // force a where condition to avoid leaking schedule info to another account
                // Note that, since SelectionBuilder always join multiple where calls using AND,
                // even if malicious code specifying additional conditions on account_name won't
                // be able to fetch data from a different account.
                return builder.table(Tables.MY_VIEWED_VIDEO)
                        .where(MyViewedVideos.MY_VIEWED_VIDEOS_ACCOUNT_NAME + "=?",
                                getCurrentAccountName(uri, true));
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
            case HASHTAGS: {
                return builder.table(Tables.HASHTAGS);
            }
            case HASHTAGS_NAME: {
                final String hashtagName = Hashtags.getHashtagName(uri);
                return builder.table(Tables.HASHTAGS)
                        .where(HashtagColumns.HASHTAG_NAME + "=?", hashtagName);
            }
            case VIDEOS: {
                return builder.table(Tables.VIDEOS);
            }
            case VIDEOS_ID: {
                final String videoId = Videos.getVideoId(uri);
                return builder.table(Tables.VIDEOS)
                        .where(VideoColumns.VIDEO_ID + "=?", videoId);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        throw new UnsupportedOperationException("openFile is not supported for " + uri);
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
