/*
 * Copyright 2011 Google Inc.
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

import com.google.android.apps.iosched.provider.ScheduleContract.Blocks;
import com.google.android.apps.iosched.provider.ScheduleContract.BlocksColumns;
import com.google.android.apps.iosched.provider.ScheduleContract.Rooms;
import com.google.android.apps.iosched.provider.ScheduleContract.RoomsColumns;
import com.google.android.apps.iosched.provider.ScheduleContract.Sessions;
import com.google.android.apps.iosched.provider.ScheduleContract.SessionsColumns;
import com.google.android.apps.iosched.provider.ScheduleContract.Speakers;
import com.google.android.apps.iosched.provider.ScheduleContract.SpeakersColumns;
import com.google.android.apps.iosched.provider.ScheduleContract.SyncColumns;
import com.google.android.apps.iosched.provider.ScheduleContract.Tracks;
import com.google.android.apps.iosched.provider.ScheduleContract.TracksColumns;
import com.google.android.apps.iosched.provider.ScheduleContract.Vendors;
import com.google.android.apps.iosched.provider.ScheduleContract.VendorsColumns;

import android.app.SearchManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Helper for managing {@link SQLiteDatabase} that stores data for
 * {@link ScheduleProvider}.
 */
public class ScheduleDatabase extends SQLiteOpenHelper {
    private static final String TAG = "ScheduleDatabase";

    private static final String DATABASE_NAME = "schedule.db";

    // NOTE: carefully update onUpgrade() when bumping database versions to make
    // sure user data is saved.

    private static final int VER_LAUNCH = 21;
    private static final int VER_SESSION_FEEDBACK_URL = 22;
    private static final int VER_SESSION_NOTES_URL_SLUG = 23;

    private static final int DATABASE_VERSION = VER_SESSION_NOTES_URL_SLUG;

    interface Tables {
        String BLOCKS = "blocks";
        String TRACKS = "tracks";
        String ROOMS = "rooms";
        String SESSIONS = "sessions";
        String SPEAKERS = "speakers";
        String SESSIONS_SPEAKERS = "sessions_speakers";
        String SESSIONS_TRACKS = "sessions_tracks";
        String VENDORS = "vendors";

        String SESSIONS_SEARCH = "sessions_search";
        String VENDORS_SEARCH = "vendors_search";

        String SEARCH_SUGGEST = "search_suggest";

        String SESSIONS_JOIN_BLOCKS_ROOMS = "sessions "
                + "LEFT OUTER JOIN blocks ON sessions.block_id=blocks.block_id "
                + "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id";

        String VENDORS_JOIN_TRACKS = "vendors "
                + "LEFT OUTER JOIN tracks ON vendors.track_id=tracks.track_id";

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

        String VENDORS_SEARCH_JOIN_VENDORS_TRACKS = "vendors_search "
                + "LEFT OUTER JOIN vendors ON vendors_search.vendor_id=vendors.vendor_id "
                + "LEFT OUTER JOIN tracks ON vendors.track_id=tracks.track_id";

    }

    private interface Triggers {
        String SESSIONS_SEARCH_INSERT = "sessions_search_insert";
        String SESSIONS_SEARCH_DELETE = "sessions_search_delete";
        String SESSIONS_SEARCH_UPDATE = "sessions_search_update";

        String VENDORS_SEARCH_INSERT = "vendors_search_insert";
        String VENDORS_SEARCH_DELETE = "vendors_search_delete";
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

    interface VendorsSearchColumns {
        String VENDOR_ID = "vendor_id";
        String BODY = "body";
    }

    /** Fully-qualified field names. */
    private interface Qualified {
        String SESSIONS_SEARCH_SESSION_ID = Tables.SESSIONS_SEARCH + "."
                + SessionsSearchColumns.SESSION_ID;
        String VENDORS_SEARCH_VENDOR_ID = Tables.VENDORS_SEARCH + "."
                + VendorsSearchColumns.VENDOR_ID;

        String SESSIONS_SEARCH = Tables.SESSIONS_SEARCH + "(" + SessionsSearchColumns.SESSION_ID
                + "," + SessionsSearchColumns.BODY + ")";
        String VENDORS_SEARCH = Tables.VENDORS_SEARCH + "(" + VendorsSearchColumns.VENDOR_ID + ","
                + VendorsSearchColumns.BODY + ")";
    }

    /** {@code REFERENCES} clauses. */
    private interface References {
        String BLOCK_ID = "REFERENCES " + Tables.BLOCKS + "(" + Blocks.BLOCK_ID + ")";
        String TRACK_ID = "REFERENCES " + Tables.TRACKS + "(" + Tracks.TRACK_ID + ")";
        String ROOM_ID = "REFERENCES " + Tables.ROOMS + "(" + Rooms.ROOM_ID + ")";
        String SESSION_ID = "REFERENCES " + Tables.SESSIONS + "(" + Sessions.SESSION_ID + ")";
        String SPEAKER_ID = "REFERENCES " + Tables.SPEAKERS + "(" + Speakers.SPEAKER_ID + ")";
        String VENDOR_ID = "REFERENCES " + Tables.VENDORS + "(" + Vendors.VENDOR_ID + ")";
    }

    private interface Subquery {
        /**
         * Subquery used to build the {@link SessionsSearchColumns#BODY} string
         * used for indexing {@link Sessions} content.
         */
        String SESSIONS_BODY = "(new." + Sessions.SESSION_TITLE
                + "||'; '||new." + Sessions.SESSION_ABSTRACT
                + "||'; '||" + "coalesce(new." + Sessions.SESSION_KEYWORDS + ", '')"
                + ")";

        /**
         * Subquery used to build the {@link VendorsSearchColumns#BODY} string
         * used for indexing {@link Vendors} content.
         */
        String VENDORS_BODY = "(new." + Vendors.VENDOR_NAME
                + "||'; '||new." + Vendors.VENDOR_DESC
                + "||'; '||new." + Vendors.VENDOR_PRODUCT_DESC + ")";
    }

    public ScheduleDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
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
                + "UNIQUE (" + BlocksColumns.BLOCK_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.TRACKS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + TracksColumns.TRACK_ID + " TEXT NOT NULL,"
                + TracksColumns.TRACK_NAME + " TEXT,"
                + TracksColumns.TRACK_COLOR + " INTEGER,"
                + TracksColumns.TRACK_ABSTRACT + " TEXT,"
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
                + SessionsColumns.SESSION_LEVEL + " TEXT,"
                + SessionsColumns.SESSION_TITLE + " TEXT,"
                + SessionsColumns.SESSION_ABSTRACT + " TEXT,"
                + SessionsColumns.SESSION_REQUIREMENTS + " TEXT,"
                + SessionsColumns.SESSION_KEYWORDS + " TEXT,"
                + SessionsColumns.SESSION_HASHTAG + " TEXT,"
                + SessionsColumns.SESSION_SLUG + " TEXT,"
                + SessionsColumns.SESSION_URL + " TEXT,"
                + SessionsColumns.SESSION_MODERATOR_URL + " TEXT,"
                + SessionsColumns.SESSION_YOUTUBE_URL + " TEXT,"
                + SessionsColumns.SESSION_PDF_URL + " TEXT,"
                + SessionsColumns.SESSION_FEEDBACK_URL + " TEXT,"
                + SessionsColumns.SESSION_NOTES_URL + " TEXT,"
                + SessionsColumns.SESSION_STARRED + " INTEGER NOT NULL DEFAULT 0,"
                + "UNIQUE (" + SessionsColumns.SESSION_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.SPEAKERS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SyncColumns.UPDATED + " INTEGER NOT NULL,"
                + SpeakersColumns.SPEAKER_ID + " TEXT NOT NULL,"
                + SpeakersColumns.SPEAKER_NAME + " TEXT,"
                + SpeakersColumns.SPEAKER_IMAGE_URL + " TEXT,"
                + SpeakersColumns.SPEAKER_COMPANY + " TEXT,"
                + SpeakersColumns.SPEAKER_ABSTRACT + " TEXT,"
                + SpeakersColumns.SPEAKER_URL+ " TEXT,"
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
                + SessionsTracks.TRACK_ID + " TEXT NOT NULL " + References.TRACK_ID + ","
                + "UNIQUE (" + SessionsTracks.SESSION_ID + ","
                        + SessionsTracks.TRACK_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.VENDORS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SyncColumns.UPDATED + " INTEGER NOT NULL,"
                + VendorsColumns.VENDOR_ID + " TEXT NOT NULL,"
                + Vendors.TRACK_ID + " TEXT " + References.TRACK_ID + ","
                + VendorsColumns.VENDOR_NAME + " TEXT,"
                + VendorsColumns.VENDOR_LOCATION + " TEXT,"
                + VendorsColumns.VENDOR_DESC + " TEXT,"
                + VendorsColumns.VENDOR_URL + " TEXT,"
                + VendorsColumns.VENDOR_PRODUCT_DESC + " TEXT,"
                + VendorsColumns.VENDOR_LOGO_URL + " TEXT,"
                + VendorsColumns.VENDOR_STARRED + " INTEGER,"
                + "UNIQUE (" + VendorsColumns.VENDOR_ID + ") ON CONFLICT REPLACE)");

        createSessionsSearch(db);
        createVendorsSearch(db);

        db.execSQL("CREATE TABLE " + Tables.SEARCH_SUGGEST + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SearchManager.SUGGEST_COLUMN_TEXT_1 + " TEXT NOT NULL)");

    }

    /**
     * Create triggers that automatically build {@link Tables#SESSIONS_SEARCH}
     * as values are changed in {@link Tables#SESSIONS}.
     */
    private static void createSessionsSearch(SQLiteDatabase db) {
        // Using the "porter" tokenizer for simple stemming, so that
        // "frustration" matches "frustrated."

        db.execSQL("CREATE VIRTUAL TABLE " + Tables.SESSIONS_SEARCH + " USING fts3("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SessionsSearchColumns.BODY + " TEXT NOT NULL,"
                + SessionsSearchColumns.SESSION_ID
                        + " TEXT NOT NULL " + References.SESSION_ID + ","
                + "UNIQUE (" + SessionsSearchColumns.SESSION_ID + ") ON CONFLICT REPLACE,"
                + "tokenize=porter)");

        // TODO: handle null fields in body, which cause trigger to fail
        // TODO: implement update trigger, not currently exercised

        db.execSQL("CREATE TRIGGER " + Triggers.SESSIONS_SEARCH_INSERT + " AFTER INSERT ON "
                + Tables.SESSIONS + " BEGIN INSERT INTO " + Qualified.SESSIONS_SEARCH + " "
                + " VALUES(new." + Sessions.SESSION_ID + ", " + Subquery.SESSIONS_BODY + ");"
                + " END;");

        db.execSQL("CREATE TRIGGER " + Triggers.SESSIONS_SEARCH_DELETE + " AFTER DELETE ON "
                + Tables.SESSIONS + " BEGIN DELETE FROM " + Tables.SESSIONS_SEARCH + " "
                + " WHERE " + Qualified.SESSIONS_SEARCH_SESSION_ID + "=old." + Sessions.SESSION_ID
                + ";" + " END;");

        db.execSQL("CREATE TRIGGER " + Triggers.SESSIONS_SEARCH_UPDATE 
                + " AFTER UPDATE ON " + Tables.SESSIONS
                + " BEGIN UPDATE sessions_search SET " + SessionsSearchColumns.BODY  + " = "
                + Subquery.SESSIONS_BODY + " WHERE session_id = old.session_id"
                + "; END;");

    }

    /**
     * Create triggers that automatically build {@link Tables#VENDORS_SEARCH} as
     * values are changed in {@link Tables#VENDORS}.
     */
    private static void createVendorsSearch(SQLiteDatabase db) {
        // Using the "porter" tokenizer for simple stemming, so that
        // "frustration" matches "frustrated."

        db.execSQL("CREATE VIRTUAL TABLE " + Tables.VENDORS_SEARCH + " USING fts3("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + VendorsSearchColumns.BODY + " TEXT NOT NULL,"
                + VendorsSearchColumns.VENDOR_ID
                        + " TEXT NOT NULL " + References.VENDOR_ID + ","
                + "UNIQUE (" + VendorsSearchColumns.VENDOR_ID + ") ON CONFLICT REPLACE,"
                + "tokenize=porter)");

        // TODO: handle null fields in body, which cause trigger to fail
        // TODO: implement update trigger, not currently exercised

        db.execSQL("CREATE TRIGGER " + Triggers.VENDORS_SEARCH_INSERT + " AFTER INSERT ON "
                + Tables.VENDORS + " BEGIN INSERT INTO " + Qualified.VENDORS_SEARCH + " "
                + " VALUES(new." + Vendors.VENDOR_ID + ", " + Subquery.VENDORS_BODY + ");"
                + " END;");

        db.execSQL("CREATE TRIGGER " + Triggers.VENDORS_SEARCH_DELETE + " AFTER DELETE ON "
                + Tables.VENDORS + " BEGIN DELETE FROM " + Tables.VENDORS_SEARCH + " "
                + " WHERE " + Qualified.VENDORS_SEARCH_VENDOR_ID + "=old." + Vendors.VENDOR_ID
                + ";" + " END;");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade() from " + oldVersion + " to " + newVersion);

        // NOTE: This switch statement is designed to handle cascading database
        // updates, starting at the current version and falling through to all
        // future upgrade cases. Only use "break;" when you want to drop and
        // recreate the entire database.
        int version = oldVersion;

        switch (version) {
            case VER_LAUNCH:
                // Version 22 added column for session feedback URL.
                db.execSQL("ALTER TABLE " + Tables.SESSIONS + " ADD COLUMN "
                        + SessionsColumns.SESSION_FEEDBACK_URL + " TEXT");
                version = VER_SESSION_FEEDBACK_URL;

            case VER_SESSION_FEEDBACK_URL:
                // Version 23 added columns for session official notes URL and slug.
                db.execSQL("ALTER TABLE " + Tables.SESSIONS + " ADD COLUMN "
                        + SessionsColumns.SESSION_NOTES_URL + " TEXT");
                db.execSQL("ALTER TABLE " + Tables.SESSIONS + " ADD COLUMN "
                        + SessionsColumns.SESSION_SLUG + " TEXT");
                version = VER_SESSION_NOTES_URL_SLUG;
        }

        Log.d(TAG, "after upgrade logic, at version " + version);
        if (version != DATABASE_VERSION) {
            Log.w(TAG, "Destroying old data during upgrade");

            db.execSQL("DROP TABLE IF EXISTS " + Tables.BLOCKS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.TRACKS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.ROOMS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SPEAKERS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS_SPEAKERS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS_TRACKS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.VENDORS);

            db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SESSIONS_SEARCH_INSERT);
            db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SESSIONS_SEARCH_DELETE);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS_SEARCH);

            db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.VENDORS_SEARCH_INSERT);
            db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.VENDORS_SEARCH_DELETE);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.VENDORS_SEARCH);

            db.execSQL("DROP TABLE IF EXISTS " + Tables.SEARCH_SUGGEST);

            onCreate(db);
        }
    }
}
