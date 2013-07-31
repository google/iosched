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

import android.accounts.Account;
import android.content.ContentResolver;

import com.google.android.apps.iosched.provider.ScheduleContract.AnnouncementsColumns;
import com.google.android.apps.iosched.provider.ScheduleContract.Blocks;
import com.google.android.apps.iosched.provider.ScheduleContract.BlocksColumns;
import com.google.android.apps.iosched.provider.ScheduleContract.FeedbackColumns;
import com.google.android.apps.iosched.provider.ScheduleContract.MapMarkerColumns;
import com.google.android.apps.iosched.provider.ScheduleContract.MapTileColumns;
import com.google.android.apps.iosched.provider.ScheduleContract.Rooms;
import com.google.android.apps.iosched.provider.ScheduleContract.RoomsColumns;
import com.google.android.apps.iosched.provider.ScheduleContract.Sessions;
import com.google.android.apps.iosched.provider.ScheduleContract.SessionsColumns;
import com.google.android.apps.iosched.provider.ScheduleContract.Speakers;
import com.google.android.apps.iosched.provider.ScheduleContract.SpeakersColumns;
import com.google.android.apps.iosched.provider.ScheduleContract.SyncColumns;
import com.google.android.apps.iosched.provider.ScheduleContract.Tracks;
import com.google.android.apps.iosched.provider.ScheduleContract.TracksColumns;
import com.google.android.apps.iosched.provider.ScheduleContract.Sandbox;

import android.app.SearchManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import com.google.android.apps.iosched.sync.SyncHelper;
import com.google.android.apps.iosched.util.AccountUtils;

import static com.google.android.apps.iosched.util.LogUtils.*;

/**
 * Helper for managing {@link SQLiteDatabase} that stores data for
 * {@link ScheduleProvider}.
 */
public class ScheduleDatabase extends SQLiteOpenHelper {
    private static final String TAG = makeLogTag(ScheduleDatabase.class);

    private static final String DATABASE_NAME = "schedule.db";

    // NOTE: carefully update onUpgrade() when bumping database versions to make
    // sure user data is saved.

    private static final int VER_2013_LAUNCH = 104;  // 1.0
    private static final int VER_2013_RM2 = 105;  // 1.1
    private static final int DATABASE_VERSION = VER_2013_RM2;

    private final Context mContext;

    interface Tables {
        String BLOCKS = "blocks";
        String TRACKS = "tracks";
        String ROOMS = "rooms";
        String SESSIONS = "sessions";
        String SPEAKERS = "speakers";
        String SESSIONS_SPEAKERS = "sessions_speakers";
        String SESSIONS_TRACKS = "sessions_tracks";
        String SANDBOX = "sandbox";
        String ANNOUNCEMENTS = "announcements";
        String MAPMARKERS = "mapmarkers";
        String MAPTILES = "mapoverlays";
        String FEEDBACK = "feedback";


        String SESSIONS_SEARCH = "sessions_search";

        String SEARCH_SUGGEST = "search_suggest";

        String SESSIONS_JOIN_BLOCKS_ROOMS = "sessions "
                + "LEFT OUTER JOIN blocks ON sessions.block_id=blocks.block_id "
                + "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id";

        String SESSIONS_JOIN_ROOMS = "sessions "
                + "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id";

        String SANDBOX_JOIN_TRACKS_BLOCKS_ROOMS = "sandbox "
                + "LEFT OUTER JOIN tracks ON sandbox.track_id=tracks.track_id "
                + "LEFT OUTER JOIN blocks ON sandbox.block_id=blocks.block_id "
                + "LEFT OUTER JOIN rooms ON sandbox.room_id=rooms.room_id";

        String SESSIONS_SPEAKERS_JOIN_SPEAKERS = "sessions_speakers "
                + "LEFT OUTER JOIN speakers ON sessions_speakers.speaker_id=speakers.speaker_id";

        String SESSIONS_SPEAKERS_JOIN_SESSIONS_BLOCKS_ROOMS = "sessions_speakers "
                + "LEFT OUTER JOIN sessions ON sessions_speakers.session_id=sessions.session_id "
                + "LEFT OUTER JOIN blocks ON sessions.block_id=blocks.block_id "
                + "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id";

        String SESSIONS_TRACKS_JOIN_TRACKS = "sessions_tracks "
                + "LEFT OUTER JOIN tracks ON sessions_tracks.track_id=tracks.track_id";

        String SESSIONS_TRACKS_JOIN_SESSIONS_BLOCKS_ROOMS = "sessions_tracks "
                + "LEFT OUTER JOIN sessions ON sessions_tracks.session_id=sessions.session_id "
                + "LEFT OUTER JOIN blocks ON sessions.block_id=blocks.block_id "
                + "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id";

        String SESSIONS_SEARCH_JOIN_SESSIONS_BLOCKS_ROOMS = "sessions_search "
                + "LEFT OUTER JOIN sessions ON sessions_search.session_id=sessions.session_id "
                + "LEFT OUTER JOIN blocks ON sessions.block_id=blocks.block_id "
                + "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id";

        String SESSIONS_JOIN_TRACKS_JOIN_BLOCKS = "sessions "
                + "LEFT OUTER JOIN sessions_tracks ON "
                        + "sessions_tracks.session_id=sessions.session_id "
                + "LEFT OUTER JOIN tracks ON tracks.track_id=sessions_tracks.track_id "
                + "LEFT OUTER JOIN blocks ON sessions.block_id=blocks.block_id";

        String BLOCKS_JOIN_SESSIONS = "blocks "
                + "LEFT OUTER JOIN sessions ON blocks.block_id=sessions.block_id";

        String MAPMARKERS_JOIN_TRACKS = "mapmarkers "
                + "LEFT OUTER JOIN tracks ON tracks.track_id=mapmarkers.track_id ";
    }

    private interface Triggers {
        // Deletes from session_tracks and sessions_speakers when corresponding sessions
        // are deleted.
        String SESSIONS_TRACKS_DELETE = "sessions_tracks_delete";
        String SESSIONS_SPEAKERS_DELETE = "sessions_speakers_delete";
        String SESSIONS_FEEDBACK_DELETE = "sessions_feedback_delete";
    }

    public interface SessionsSpeakers {
        String SESSION_ID = "session_id";
        String SPEAKER_ID = "speaker_id";
    }

    public interface SessionsTracks {
        String SESSION_ID = "session_id";
        String TRACK_ID = "track_id";
    }

    interface SessionsSearchColumns {
        String SESSION_ID = "session_id";
        String BODY = "body";
    }

    /** Fully-qualified field names. */
    private interface Qualified {
        String SESSIONS_SEARCH = Tables.SESSIONS_SEARCH + "(" + SessionsSearchColumns.SESSION_ID
                + "," + SessionsSearchColumns.BODY + ")";

        String SESSIONS_TRACKS_SESSION_ID = Tables.SESSIONS_TRACKS + "."
                + SessionsTracks.SESSION_ID;

        String SESSIONS_SPEAKERS_SESSION_ID = Tables.SESSIONS_SPEAKERS+ "."
                + SessionsSpeakers.SESSION_ID;

        String SESSIONS_SPEAKERS_SPEAKER_ID = Tables.SESSIONS_SPEAKERS+ "."
                + SessionsSpeakers.SPEAKER_ID;

        String SPEAKERS_SPEAKER_ID = Tables.SPEAKERS + "." + Speakers.SPEAKER_ID;

        String FEEDBACK_SESSION_ID = Tables.FEEDBACK + "." + FeedbackColumns.SESSION_ID;
    }

    /** {@code REFERENCES} clauses. */
    private interface References {
        String BLOCK_ID = "REFERENCES " + Tables.BLOCKS + "(" + Blocks.BLOCK_ID + ")";
        String TRACK_ID = "REFERENCES " + Tables.TRACKS + "(" + Tracks.TRACK_ID + ")";
        String ROOM_ID = "REFERENCES " + Tables.ROOMS + "(" + Rooms.ROOM_ID + ")";
        String SESSION_ID = "REFERENCES " + Tables.SESSIONS + "(" + Sessions.SESSION_ID + ")";
        String SPEAKER_ID = "REFERENCES " + Tables.SPEAKERS + "(" + Speakers.SPEAKER_ID + ")";
    }

    public ScheduleDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.BLOCKS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + BlocksColumns.BLOCK_ID + " TEXT NOT NULL,"
                + BlocksColumns.BLOCK_TITLE + " TEXT NOT NULL,"
                + BlocksColumns.BLOCK_START + " INTEGER NOT NULL,"
                + BlocksColumns.BLOCK_END + " INTEGER NOT NULL,"
                + BlocksColumns.BLOCK_TYPE + " TEXT,"
                + BlocksColumns.BLOCK_META + " TEXT,"
                + "UNIQUE (" + BlocksColumns.BLOCK_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.TRACKS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + TracksColumns.TRACK_ID + " TEXT NOT NULL,"
                + TracksColumns.TRACK_NAME + " TEXT,"
                + TracksColumns.TRACK_COLOR + " INTEGER,"
                + TracksColumns.TRACK_LEVEL + " INTEGER,"
                + TracksColumns.TRACK_ORDER_IN_LEVEL + " INTEGER,"
                + TracksColumns.TRACK_META + " INTEGER NOT NULL DEFAULT 0,"
                + TracksColumns.TRACK_ABSTRACT + " TEXT,"
                + TracksColumns.TRACK_HASHTAG + " TEXT,"
                + "UNIQUE (" + TracksColumns.TRACK_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.ROOMS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + RoomsColumns.ROOM_ID + " TEXT NOT NULL,"
                + RoomsColumns.ROOM_NAME + " TEXT,"
                + RoomsColumns.ROOM_FLOOR + " TEXT,"
                + "UNIQUE (" + RoomsColumns.ROOM_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.SESSIONS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SyncColumns.UPDATED + " INTEGER NOT NULL,"
                + SessionsColumns.SESSION_ID + " TEXT NOT NULL,"
                + Sessions.BLOCK_ID + " TEXT " + References.BLOCK_ID + ","
                + Sessions.ROOM_ID + " TEXT " + References.ROOM_ID + ","
                + SessionsColumns.SESSION_TYPE + " TEXT,"
                + SessionsColumns.SESSION_LEVEL + " TEXT,"
                + SessionsColumns.SESSION_TITLE + " TEXT,"
                + SessionsColumns.SESSION_ABSTRACT + " TEXT,"
                + SessionsColumns.SESSION_REQUIREMENTS + " TEXT,"
                + SessionsColumns.SESSION_TAGS + " TEXT,"
                + SessionsColumns.SESSION_HASHTAGS + " TEXT,"
                + SessionsColumns.SESSION_URL + " TEXT,"
                + SessionsColumns.SESSION_YOUTUBE_URL + " TEXT,"
                + SessionsColumns.SESSION_MODERATOR_URL + " TEXT,"
                + SessionsColumns.SESSION_PDF_URL + " TEXT,"
                + SessionsColumns.SESSION_NOTES_URL + " TEXT,"
                + SessionsColumns.SESSION_STARRED + " INTEGER NOT NULL DEFAULT 0,"
                + SessionsColumns.SESSION_CAL_EVENT_ID + " INTEGER,"
                + SessionsColumns.SESSION_LIVESTREAM_URL + " TEXT,"
                + "UNIQUE (" + SessionsColumns.SESSION_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.SPEAKERS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SyncColumns.UPDATED + " INTEGER NOT NULL,"
                + SpeakersColumns.SPEAKER_ID + " TEXT NOT NULL,"
                + SpeakersColumns.SPEAKER_NAME + " TEXT,"
                + SpeakersColumns.SPEAKER_IMAGE_URL + " TEXT,"
                + SpeakersColumns.SPEAKER_COMPANY + " TEXT,"
                + SpeakersColumns.SPEAKER_ABSTRACT + " TEXT,"
                + SpeakersColumns.SPEAKER_URL + " TEXT,"
                + "UNIQUE (" + SpeakersColumns.SPEAKER_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.SESSIONS_SPEAKERS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SessionsSpeakers.SESSION_ID + " TEXT NOT NULL " + References.SESSION_ID + ","
                + SessionsSpeakers.SPEAKER_ID + " TEXT NOT NULL " + References.SPEAKER_ID + ","
                + "UNIQUE (" + SessionsSpeakers.SESSION_ID + ","
                        + SessionsSpeakers.SPEAKER_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.SESSIONS_TRACKS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SessionsTracks.SESSION_ID + " TEXT NOT NULL " + References.SESSION_ID + ","
                + SessionsTracks.TRACK_ID + " TEXT NOT NULL " + References.TRACK_ID
                + ")");

        db.execSQL("CREATE TABLE " + Tables.SANDBOX + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SyncColumns.UPDATED + " INTEGER NOT NULL,"
                + ScheduleContract.SandboxColumns.COMPANY_ID + " TEXT NOT NULL,"
                + Sandbox.TRACK_ID + " TEXT " + References.TRACK_ID + ","
                + Sandbox.BLOCK_ID + " TEXT " + References.BLOCK_ID + ","
                + Sandbox.ROOM_ID + " TEXT " + References.ROOM_ID + ","
                + ScheduleContract.SandboxColumns.COMPANY_NAME + " TEXT,"
                + ScheduleContract.SandboxColumns.COMPANY_DESC + " TEXT,"
                + ScheduleContract.SandboxColumns.COMPANY_URL + " TEXT,"
                + ScheduleContract.SandboxColumns.COMPANY_LOGO_URL + " TEXT,"
                + "UNIQUE (" + ScheduleContract.SandboxColumns.COMPANY_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.ANNOUNCEMENTS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SyncColumns.UPDATED + " INTEGER NOT NULL,"
                + AnnouncementsColumns.ANNOUNCEMENT_ID + " TEXT,"
                + AnnouncementsColumns.ANNOUNCEMENT_TITLE + " TEXT NOT NULL,"
                + AnnouncementsColumns.ANNOUNCEMENT_ACTIVITY_JSON + " BLOB,"
                + AnnouncementsColumns.ANNOUNCEMENT_URL + " TEXT,"
                + AnnouncementsColumns.ANNOUNCEMENT_DATE + " INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE " + Tables.MAPTILES + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + MapTileColumns.TILE_FLOOR+ " INTEGER NOT NULL,"
                + MapTileColumns.TILE_FILE+ " TEXT NOT NULL,"
                + MapTileColumns.TILE_URL+ " TEXT NOT NULL,"
                + "UNIQUE (" + MapTileColumns.TILE_FLOOR+ ") ON CONFLICT REPLACE)");

        doMigration2013RM2(db);

        // Full-text search index. Update using updateSessionSearchIndex method.
        // Use the porter tokenizer for simple stemming, so that "frustration" matches "frustrated."
        db.execSQL("CREATE VIRTUAL TABLE " + Tables.SESSIONS_SEARCH + " USING fts3("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SessionsSearchColumns.BODY + " TEXT NOT NULL,"
                + SessionsSearchColumns.SESSION_ID
                        + " TEXT NOT NULL " + References.SESSION_ID + ","
                + "UNIQUE (" + SessionsSearchColumns.SESSION_ID + ") ON CONFLICT REPLACE,"
                + "tokenize=porter)");

        // Search suggestions
        db.execSQL("CREATE TABLE " + Tables.SEARCH_SUGGEST + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SearchManager.SUGGEST_COLUMN_TEXT_1 + " TEXT NOT NULL)");

        // Session deletion triggers
        db.execSQL("CREATE TRIGGER " + Triggers.SESSIONS_TRACKS_DELETE + " AFTER DELETE ON "
                + Tables.SESSIONS + " BEGIN DELETE FROM " + Tables.SESSIONS_TRACKS + " "
                + " WHERE " + Qualified.SESSIONS_TRACKS_SESSION_ID + "=old." + Sessions.SESSION_ID
                + ";" + " END;");

        db.execSQL("CREATE TRIGGER " + Triggers.SESSIONS_SPEAKERS_DELETE + " AFTER DELETE ON "
                + Tables.SESSIONS + " BEGIN DELETE FROM " + Tables.SESSIONS_SPEAKERS + " "
                + " WHERE " + Qualified.SESSIONS_SPEAKERS_SESSION_ID + "=old." + Sessions.SESSION_ID
                + ";" + " END;");
    }

    private void doMigration2013RM2(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.FEEDBACK + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SyncColumns.UPDATED + " INTEGER NOT NULL,"
                + Sessions.SESSION_ID + " TEXT " + References.SESSION_ID + ","
                + FeedbackColumns.SESSION_RATING + " INTEGER NOT NULL,"
                + FeedbackColumns.ANSWER_RELEVANCE + " INTEGER NOT NULL,"
                + FeedbackColumns.ANSWER_CONTENT + " INTEGER NOT NULL,"
                + FeedbackColumns.ANSWER_SPEAKER + " INTEGER NOT NULL,"
                + FeedbackColumns.ANSWER_WILLUSE + " INTEGER NOT NULL,"
                + FeedbackColumns.COMMENTS + " TEXT)");

        db.execSQL("CREATE TRIGGER " + Triggers.SESSIONS_FEEDBACK_DELETE + " AFTER DELETE ON "
                + Tables.SESSIONS + " BEGIN DELETE FROM " + Tables.FEEDBACK + " "
                + " WHERE " + Qualified.FEEDBACK_SESSION_ID + "=old." + Sessions.SESSION_ID
                + ";" + " END;");

        db.execSQL("CREATE TABLE " + Tables.MAPMARKERS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + MapMarkerColumns.MARKER_ID+ " TEXT NOT NULL,"
                + MapMarkerColumns.MARKER_TYPE+ " TEXT NOT NULL,"
                + MapMarkerColumns.MARKER_LATITUDE+ " DOUBLE NOT NULL,"
                + MapMarkerColumns.MARKER_LONGITUDE+ " DOUBLE NOT NULL,"
                + MapMarkerColumns.MARKER_LABEL+ " TEXT,"
                + MapMarkerColumns.MARKER_FLOOR+ " INTEGER NOT NULL,"
                + MapMarkerColumns.MARKER_TRACK+ " TEXT,"
                + "UNIQUE (" + MapMarkerColumns.MARKER_ID + ") ON CONFLICT REPLACE)");
    }

    /**
     * Updates the session search index. This should be done sparingly, as the queries are rather
     * complex.
     */
    static void updateSessionSearchIndex(SQLiteDatabase db) {
        db.execSQL("DELETE FROM " + Tables.SESSIONS_SEARCH);

        db.execSQL("INSERT INTO " + Qualified.SESSIONS_SEARCH
                + " SELECT s." + Sessions.SESSION_ID + ",("

                // Full text body
                + Sessions.SESSION_TITLE + "||'; '||"
                + Sessions.SESSION_ABSTRACT + "||'; '||"
                + "IFNULL(" + Sessions.SESSION_TAGS + ",'')||'; '||"
                + "IFNULL(GROUP_CONCAT(t." + Speakers.SPEAKER_NAME + ",' '),'')||'; '||"
                + "'')"

                + " FROM " + Tables.SESSIONS + " s "
                + " LEFT OUTER JOIN"

                // Subquery resulting in session_id, speaker_id, speaker_name
                + "(SELECT " + Sessions.SESSION_ID + "," + Qualified.SPEAKERS_SPEAKER_ID
                + "," + Speakers.SPEAKER_NAME
                + " FROM " + Tables.SESSIONS_SPEAKERS
                + " INNER JOIN " + Tables.SPEAKERS
                + " ON " + Qualified.SESSIONS_SPEAKERS_SPEAKER_ID + "="
                + Qualified.SPEAKERS_SPEAKER_ID
                + ") t"

                // Grand finale
                + " ON s." + Sessions.SESSION_ID + "=t." + Sessions.SESSION_ID
                + " GROUP BY s." + Sessions.SESSION_ID);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        LOGD(TAG, "onUpgrade() from " + oldVersion + " to " + newVersion);

        // Cancel any sync currently in progress
        Account account = AccountUtils.getChosenAccount(mContext);
        if (account != null) {
            LOGI(TAG, "Cancelling any pending syncs for for account");
            ContentResolver.cancelSync(account, ScheduleContract.CONTENT_AUTHORITY);
        }

        // NOTE: This switch statement is designed to handle cascading database
        // updates, starting at the current version and falling through to all
        // future upgrade cases. Only use "break;" when you want to drop and
        // recreate the entire database.
        int version = oldVersion;

        switch (version) {
            // Note: Data from prior years not preserved.
            case VER_2013_LAUNCH:
                LOGI(TAG, "Performing migration for DB version " + version);
                // Reset BLOCKS table
                db.execSQL("DELETE FROM " + Tables.BLOCKS);
                // Reset MapMarkers table
                db.execSQL("DROP TABLE IF EXISTS " + Tables.MAPMARKERS);
                // Apply new schema changes
                doMigration2013RM2(db);
            case VER_2013_RM2:
                version = VER_2013_RM2;
                LOGI(TAG, "DB at version " + version);
                // Current version, no further action necessary
        }

        LOGD(TAG, "after upgrade logic, at version " + version);
        if (version != DATABASE_VERSION) {
            LOGW(TAG, "Destroying old data during upgrade");

            db.execSQL("DROP TABLE IF EXISTS " + Tables.BLOCKS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.TRACKS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.ROOMS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SPEAKERS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS_SPEAKERS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS_TRACKS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SANDBOX);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.ANNOUNCEMENTS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.FEEDBACK);

            db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SESSIONS_TRACKS_DELETE);
            db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SESSIONS_SPEAKERS_DELETE);
            db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SESSIONS_FEEDBACK_DELETE);

            db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS_SEARCH);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SEARCH_SUGGEST);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.MAPMARKERS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.MAPTILES);

            onCreate(db);
        }

        if (account != null) {
            LOGI(TAG, "DB upgrade complete. Requesting resync.");
            SyncHelper.requestManualSync(account);
        }
    }

    public static void deleteDatabase(Context context) {
        context.deleteDatabase(DATABASE_NAME);
    }
}
