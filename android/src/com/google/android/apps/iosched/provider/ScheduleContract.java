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

import com.google.android.apps.iosched.util.ParserUtils;

import android.app.SearchManager;
import android.graphics.Color;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.text.format.DateUtils;

import java.util.List;

/**
 * Contract class for interacting with {@link ScheduleProvider}. Unless
 * otherwise noted, all time-based fields are milliseconds since epoch and can
 * be compared against {@link System#currentTimeMillis()}.
 * <p>
 * The backing {@link android.content.ContentProvider} assumes that {@link Uri}
 * are generated using stronger {@link String} identifiers, instead of
 * {@code int} {@link BaseColumns#_ID} values, which are prone to shuffle during
 * sync.
 */
public class ScheduleContract {

    /**
     * Special value for {@link SyncColumns#UPDATED} indicating that an entry
     * has never been updated, or doesn't exist yet.
     */
    public static final long UPDATED_NEVER = -2;

    /**
     * Special value for {@link SyncColumns#UPDATED} indicating that the last
     * update time is unknown, usually when inserted from a local file source.
     */
    public static final long UPDATED_UNKNOWN = -1;

    public interface SyncColumns {
        /** Last time this entry was updated or synchronized. */
        String UPDATED = "updated";
    }

    interface BlocksColumns {
        /** Unique string identifying this block of time. */
        String BLOCK_ID = "block_id";
        /** Title describing this block of time. */
        String BLOCK_TITLE = "block_title";
        /** Time when this block starts. */
        String BLOCK_START = "block_start";
        /** Time when this block ends. */
        String BLOCK_END = "block_end";
        /** Type describing this block. */
        String BLOCK_TYPE = "block_type";
        /** Extra string metadata for the block. */
        String BLOCK_META = "block_meta";
    }

    interface TracksColumns {
        /** Unique string identifying this track. */
        String TRACK_ID = "track_id";
        /** Name describing this track. */
        String TRACK_NAME = "track_name";
        /** Color used to identify this track, in {@link Color#argb} format. */
        String TRACK_COLOR = "track_color";
        /** Body of text explaining this track in detail. */
        String TRACK_ABSTRACT = "track_abstract";
    }

    interface RoomsColumns {
        /** Unique string identifying this room. */
        String ROOM_ID = "room_id";
        /** Name describing this room. */
        String ROOM_NAME = "room_name";
        /** Building floor this room exists on. */
        String ROOM_FLOOR = "room_floor";
    }

    interface SessionsColumns {
        /** Unique string identifying this session. */
        String SESSION_ID = "session_id";
        /** The type of session (session, keynote, codelab, etc). */
        String SESSION_TYPE = "session_type";
        /** Difficulty level of the session. */
        String SESSION_LEVEL = "session_level";
        /** Title describing this track. */
        String SESSION_TITLE = "session_title";
        /** Body of text explaining this session in detail. */
        String SESSION_ABSTRACT = "session_abstract";
        /** Requirements that attendees should meet. */
        String SESSION_REQUIREMENTS = "session_requirements";
        /** Keywords/tags for this session. */
        String SESSION_TAGS = "session_keywords";
        /** Hashtag for this session. */
        String SESSION_HASHTAGS = "session_hashtag";
        /** Full URL to session online. */
        String SESSION_URL = "session_url";
        /** Full URL to YouTube. */
        String SESSION_YOUTUBE_URL = "session_youtube_url";
        /** Full URL to PDF. */
        String SESSION_PDF_URL = "session_pdf_url";
        /** Full URL to official session notes. */
        String SESSION_NOTES_URL = "session_notes_url";
        /** User-specific flag indicating starred status. */
        String SESSION_STARRED = "session_starred";
        /** Key for session Calendar event. (Used in ICS or above) */
        String SESSION_CAL_EVENT_ID = "session_cal_event_id";
        /** The YouTube live stream URL. */
        String SESSION_LIVESTREAM_URL = "session_livestream_url";
    }

    interface SpeakersColumns {
        /** Unique string identifying this speaker. */
        String SPEAKER_ID = "speaker_id";
        /** Name of this speaker. */
        String SPEAKER_NAME = "speaker_name";
        /** Profile photo of this speaker. */
        String SPEAKER_IMAGE_URL = "speaker_image_url";
        /** Company this speaker works for. */
        String SPEAKER_COMPANY = "speaker_company";
        /** Body of text describing this speaker in detail. */
        String SPEAKER_ABSTRACT = "speaker_abstract";
        /** Full URL to the speaker's profile. */
        String SPEAKER_URL = "speaker_url";
    }

    interface VendorsColumns {
        /** Unique string identifying this vendor. */
        String VENDOR_ID = "vendor_id";
        /** Name of this vendor. */
        String VENDOR_NAME = "vendor_name";
        /** Location or city this vendor is based in. */
        String VENDOR_LOCATION = "vendor_location";
        /** Body of text describing this vendor. */
        String VENDOR_DESC = "vendor_desc";
        /** Link to vendor online. */
        String VENDOR_URL = "vendor_url";
        /** Body of text describing the product of this vendor. */
        String VENDOR_PRODUCT_DESC = "vendor_product_desc";
        /** Link to vendor logo. */
        String VENDOR_LOGO_URL = "vendor_logo_url";
        /** User-specific flag indicating starred status. */
        String VENDOR_STARRED = "vendor_starred";
    }

    interface AnnouncementsColumns {
        /** Unique string identifying this announcment. */
        String ANNOUNCEMENT_ID = "announcement_id";
        /** Title of the announcement. */
        String ANNOUNCEMENT_TITLE = "announcement_title";
        /** Summary of the announcement. */
        String ANNOUNCEMENT_SUMMARY = "announcement_summary";
        /** Track announcement belongs to. */
        String ANNOUNCEMENT_TRACKS = "announcement_tracks";
        /** Full URL for the announcement. */
        String ANNOUNCEMENT_URL = "announcement_url";
        /** Date of the announcement. */
        String ANNOUNCEMENT_DATE = "announcement_date";
    }

    public static final String CONTENT_AUTHORITY = "com.google.android.apps.iosched";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    private static final String PATH_BLOCKS = "blocks";
    private static final String PATH_AT = "at";
    private static final String PATH_BETWEEN = "between";
    private static final String PATH_TRACKS = "tracks";
    private static final String PATH_ROOMS = "rooms";
    private static final String PATH_SESSIONS = "sessions";
    private static final String PATH_WITH_TRACK = "with_track";
    private static final String PATH_STARRED = "starred";
    private static final String PATH_SPEAKERS = "speakers";
    private static final String PATH_VENDORS = "vendors";
    private static final String PATH_ANNOUNCEMENTS = "announcements";
    private static final String PATH_SEARCH = "search";
    private static final String PATH_SEARCH_SUGGEST = "search_suggest_query";

    /**
     * Blocks are generic timeslots that {@link Sessions} and other related
     * events fall into.
     */
    public static class Blocks implements BlocksColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_BLOCKS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.iosched.block";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.iosched.block";

        /** Count of {@link Sessions} inside given block. */
        public static final String SESSIONS_COUNT = "sessions_count";

        /**
         * Flag indicating the number of sessions inside this block that have
         * {@link Sessions#SESSION_STARRED} set.
         */
        public static final String NUM_STARRED_SESSIONS = "num_starred_sessions";

        /**
         * The {@link Sessions#SESSION_ID} of the first starred session in this
         * block.
         */
        public static final String STARRED_SESSION_ID = "starred_session_id";

        /**
         * The {@link Sessions#SESSION_TITLE} of the first starred session in
         * this block.
         */
        public static final String STARRED_SESSION_TITLE = "starred_session_title";

        /**
         * The {@link Sessions#SESSION_LIVESTREAM_URL} of the first starred
         * session in this block.
         */
        public static final String STARRED_SESSION_LIVESTREAM_URL =
                "starred_session_livestream_url";

        /**
         * The {@link Rooms#ROOM_NAME} of the first starred session in this
         * block.
         */
        public static final String STARRED_SESSION_ROOM_NAME = "starred_session_room_name";

        /**
         * The {@link Rooms#ROOM_ID} of the first starred session in this block.
         */
        public static final String STARRED_SESSION_ROOM_ID = "starred_session_room_id";

        /**
         * The {@link Sessions#SESSION_HASHTAGS} of the first starred session in
         * this block.
         */
        public static final String STARRED_SESSION_HASHTAGS = "starred_session_hashtags";

        /**
         * The {@link Sessions#SESSION_URL} of the first starred session in this
         * block.
         */
        public static final String STARRED_SESSION_URL = "starred_session_url";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = BlocksColumns.BLOCK_START + " ASC, "
                + BlocksColumns.BLOCK_END + " ASC";

        public static final String EMPTY_SESSIONS_SELECTION = "(" + BLOCK_TYPE
                + " = '" + ParserUtils.BLOCK_TYPE_SESSION + "' OR " + BLOCK_TYPE
                + " = '" + ParserUtils.BLOCK_TYPE_CODE_LAB + "') AND "
                + SESSIONS_COUNT + " = 0";

        /** Build {@link Uri} for requested {@link #BLOCK_ID}. */
        public static Uri buildBlockUri(String blockId) {
            return CONTENT_URI.buildUpon().appendPath(blockId).build();
        }

        /**
         * Build {@link Uri} that references any {@link Sessions} associated
         * with the requested {@link #BLOCK_ID}.
         */
        public static Uri buildSessionsUri(String blockId) {
            return CONTENT_URI.buildUpon().appendPath(blockId).appendPath(PATH_SESSIONS).build();
        }

        /**
         * Build {@link Uri} that references starred {@link Sessions} associated
         * with the requested {@link #BLOCK_ID}.
         */
        public static Uri buildStarredSessionsUri(String blockId) {
            return CONTENT_URI.buildUpon().appendPath(blockId).appendPath(PATH_SESSIONS)
                    .appendPath(PATH_STARRED).build();
        }

        /**
         * Build {@link Uri} that references any {@link Blocks} that occur
         * between the requested time boundaries.
         */
        public static Uri buildBlocksBetweenDirUri(long startTime, long endTime) {
            return CONTENT_URI.buildUpon().appendPath(PATH_BETWEEN).appendPath(
                    String.valueOf(startTime)).appendPath(String.valueOf(endTime)).build();
        }

        /** Read {@link #BLOCK_ID} from {@link Blocks} {@link Uri}. */
        public static String getBlockId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        /**
         * Generate a {@link #BLOCK_ID} that will always match the requested
         * {@link Blocks} details.
         */
        public static String generateBlockId(long startTime, long endTime) {
            startTime /= DateUtils.SECOND_IN_MILLIS;
            endTime /= DateUtils.SECOND_IN_MILLIS;
            return ParserUtils.sanitizeId(startTime + "-" + endTime);
        }
    }

    /**
     * Tracks are overall categories for {@link Sessions} and {@link Vendors},
     * such as "Android" or "Enterprise."
     */
    public static class Tracks implements TracksColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TRACKS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.iosched.track";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.iosched.track";

        /** "All tracks" ID. */
        public static final String ALL_TRACK_ID = "all";
        public static final String CODELABS_TRACK_ID = generateTrackId("Code Labs");
        public static final String TECH_TALK_TRACK_ID = generateTrackId("Tech Talk");

        /** Count of {@link Sessions} inside given track. */
        public static final String SESSIONS_COUNT = "sessions_count";
        /** Count of {@link Vendors} inside given track. */
        public static final String VENDORS_COUNT = "vendors_count";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = TracksColumns.TRACK_NAME + " ASC";

        /** Build {@link Uri} for requested {@link #TRACK_ID}. */
        public static Uri buildTrackUri(String trackId) {
            return CONTENT_URI.buildUpon().appendPath(trackId).build();
        }

        /**
         * Build {@link Uri} that references any {@link Sessions} associated
         * with the requested {@link #TRACK_ID}.
         */
        public static Uri buildSessionsUri(String trackId) {
            return CONTENT_URI.buildUpon().appendPath(trackId).appendPath(PATH_SESSIONS).build();
        }

        /**
         * Build {@link Uri} that references any {@link Vendors} associated with
         * the requested {@link #TRACK_ID}.
         */
        public static Uri buildVendorsUri(String trackId) {
            return CONTENT_URI.buildUpon().appendPath(trackId).appendPath(PATH_VENDORS).build();
        }

        /** Read {@link #TRACK_ID} from {@link Tracks} {@link Uri}. */
        public static String getTrackId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        /**
         * Generate a {@link #TRACK_ID} that will always match the requested
         * {@link Tracks} details.
         */
        public static String generateTrackId(String name) {
            return ParserUtils.sanitizeId(name);
        }
    }

    /**
     * Rooms are physical locations at the conference venue.
     */
    public static class Rooms implements RoomsColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ROOMS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.iosched.room";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.iosched.room";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = RoomsColumns.ROOM_FLOOR + " ASC, "
                + RoomsColumns.ROOM_NAME + " COLLATE NOCASE ASC";

        /** Build {@link Uri} for requested {@link #ROOM_ID}. */
        public static Uri buildRoomUri(String roomId) {
            return CONTENT_URI.buildUpon().appendPath(roomId).build();
        }

        /**
         * Build {@link Uri} that references any {@link Sessions} associated
         * with the requested {@link #ROOM_ID}.
         */
        public static Uri buildSessionsDirUri(String roomId) {
            return CONTENT_URI.buildUpon().appendPath(roomId).appendPath(PATH_SESSIONS).build();
        }

        /** Read {@link #ROOM_ID} from {@link Rooms} {@link Uri}. */
        public static String getRoomId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    /**
     * Each session is a block of time that has a {@link Tracks}, a
     * {@link Rooms}, and zero or more {@link Speakers}.
     */
    public static class Sessions implements SessionsColumns, BlocksColumns, RoomsColumns,
            SyncColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SESSIONS).build();
        public static final Uri CONTENT_STARRED_URI =
                CONTENT_URI.buildUpon().appendPath(PATH_STARRED).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.iosched.session";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.iosched.session";

        public static final String BLOCK_ID = "block_id";
        public static final String ROOM_ID = "room_id";

        public static final String SEARCH_SNIPPET = "search_snippet";

        // TODO: shortcut primary track to offer sub-sorting here
        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = BlocksColumns.BLOCK_START + " ASC,"
                + SessionsColumns.SESSION_TITLE + " COLLATE NOCASE ASC";

        public static final String LIVESTREAM_SELECTION =
                SESSION_LIVESTREAM_URL + " is not null AND " + SESSION_LIVESTREAM_URL + "!=''";

        // Used to fetch sessions for a particular time
        public static final String AT_TIME_SELECTION =
                BLOCK_START + " < ? and " + BLOCK_END + " " + "> ?";

        // Builds selectionArgs for {@link AT_TIME_SELECTION}
        public static String[] buildAtTimeSelectionArgs(long time) {
            final String timeString = String.valueOf(time);
            return new String[] { timeString, timeString };
        }

        // Used to fetch upcoming sessions
        public static final String UPCOMING_SELECTION =
                BLOCK_START + " = (select min(" + BLOCK_START + ") from " +
                ScheduleDatabase.Tables.BLOCKS_JOIN_SESSIONS + " where " + LIVESTREAM_SELECTION +
                " and " + BLOCK_START + " >" + " ?)";

        // Builds selectionArgs for {@link UPCOMING_SELECTION}
        public static String[] buildUpcomingSelectionArgs(long minTime) {
            return new String[] { String.valueOf(minTime) };
        }

        /** Build {@link Uri} for requested {@link #SESSION_ID}. */
        public static Uri buildSessionUri(String sessionId) {
            return CONTENT_URI.buildUpon().appendPath(sessionId).build();
        }

        /**
         * Build {@link Uri} that references any {@link Speakers} associated
         * with the requested {@link #SESSION_ID}.
         */
        public static Uri buildSpeakersDirUri(String sessionId) {
            return CONTENT_URI.buildUpon().appendPath(sessionId).appendPath(PATH_SPEAKERS).build();
        }

        /**
         * Build {@link Uri} that includes track detail with list of sessions.
         */
        public static Uri buildWithTracksUri() {
            return CONTENT_URI.buildUpon().appendPath(PATH_WITH_TRACK).build();
        }

        /**
         * Build {@link Uri} that includes track detail for a specific session.
         */
        public static Uri buildWithTracksUri(String sessionId) {
            return CONTENT_URI.buildUpon().appendPath(sessionId)
                    .appendPath(PATH_WITH_TRACK).build();
        }

        /**
         * Build {@link Uri} that references any {@link Tracks} associated with
         * the requested {@link #SESSION_ID}.
         */
        public static Uri buildTracksDirUri(String sessionId) {
            return CONTENT_URI.buildUpon().appendPath(sessionId).appendPath(PATH_TRACKS).build();
        }

        public static Uri buildSessionsAtDirUri(long time) {
            return CONTENT_URI.buildUpon().appendPath(PATH_AT).appendPath(String.valueOf(time))
                    .build();
        }

        public static Uri buildSearchUri(String query) {
            return CONTENT_URI.buildUpon().appendPath(PATH_SEARCH).appendPath(query).build();
        }

        public static boolean isSearchUri(Uri uri) {
            List<String> pathSegments = uri.getPathSegments();
            return pathSegments.size() >= 2 && PATH_SEARCH.equals(pathSegments.get(1));
        }

        /** Read {@link #SESSION_ID} from {@link Sessions} {@link Uri}. */
        public static String getSessionId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static String getSearchQuery(Uri uri) {
            return uri.getPathSegments().get(2);
        }
    }

    /**
     * Speakers are individual people that lead {@link Sessions}.
     */
    public static class Speakers implements SpeakersColumns, SyncColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SPEAKERS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.iosched.speaker";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.iosched.speaker";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = SpeakersColumns.SPEAKER_NAME
                + " COLLATE NOCASE ASC";

        /** Build {@link Uri} for requested {@link #SPEAKER_ID}. */
        public static Uri buildSpeakerUri(String speakerId) {
            return CONTENT_URI.buildUpon().appendPath(speakerId).build();
        }

        /**
         * Build {@link Uri} that references any {@link Sessions} associated
         * with the requested {@link #SPEAKER_ID}.
         */
        public static Uri buildSessionsDirUri(String speakerId) {
            return CONTENT_URI.buildUpon().appendPath(speakerId).appendPath(PATH_SESSIONS).build();
        }

        /** Read {@link #SPEAKER_ID} from {@link Speakers} {@link Uri}. */
        public static String getSpeakerId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    /**
     * Each vendor is a company appearing at the conference that may be
     * associated with a specific {@link Tracks}.
     */
    public static class Vendors implements VendorsColumns, SyncColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_VENDORS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.iosched.vendor";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.iosched.vendor";

        /** {@link Tracks#TRACK_ID} that this vendor belongs to. */
        public static final String TRACK_ID = "track_id";

        public static final String SEARCH_SNIPPET = "search_snippet";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = VendorsColumns.VENDOR_NAME
                + " COLLATE NOCASE ASC";

        /** Build {@link Uri} for requested {@link #VENDOR_ID}. */
        public static Uri buildVendorUri(String vendorId) {
            return CONTENT_URI.buildUpon().appendPath(vendorId).build();
        }

        public static Uri buildSearchUri(String query) {
            return CONTENT_URI.buildUpon().appendPath(PATH_SEARCH).appendPath(query).build();
        }

        public static boolean isSearchUri(Uri uri) {
            List<String> pathSegments = uri.getPathSegments();
            return pathSegments.size() >= 2 && PATH_SEARCH.equals(pathSegments.get(1));
        }

        /** Read {@link #VENDOR_ID} from {@link Vendors} {@link Uri}. */
        public static String getVendorId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static String getSearchQuery(Uri uri) {
            return uri.getPathSegments().get(2);
        }

        /**
         * Generate a {@link #VENDOR_ID} that will always match the requested
         * {@link Vendors} details.
         */
        public static String generateVendorId(String companyName) {
            return ParserUtils.sanitizeId(companyName);
        }
    }

    /**
     * Announcements of breaking news
     */
    public static class Announcements implements AnnouncementsColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ANNOUNCEMENTS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.iosched.announcement";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.iosched.announcement";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = AnnouncementsColumns.ANNOUNCEMENT_DATE
                + " COLLATE NOCASE ASC";

        /** Build {@link Uri} for requested {@link #ANNOUNCEMENT_ID}. */
        public static Uri buildAnnouncementUri(String announcementId) {
            return CONTENT_URI.buildUpon().appendPath(announcementId).build();
        }

        /**
         * Build {@link Uri} that references any {@link Announcements}
         * associated with the requested {@link #ANNOUNCEMENT_ID}.
         */
        public static Uri buildAnnouncementsDirUri(String announcementId) {
            return CONTENT_URI.buildUpon().appendPath(announcementId)
                    .appendPath(PATH_ANNOUNCEMENTS).build();
        }

        /**
         * Read {@link #ANNOUNCEMENT_ID} from {@link Announcements} {@link Uri}.
         */
        public static String getAnnouncementId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static class SearchSuggest {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SEARCH_SUGGEST).build();

        public static final String DEFAULT_SORT = SearchManager.SUGGEST_COLUMN_TEXT_1
                + " COLLATE NOCASE ASC";
    }

    public static Uri addCallerIsSyncAdapterParameter(Uri uri) {
        return uri.buildUpon().appendQueryParameter(
                ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
    }

    public static boolean hasCallerIsSyncAdapterParameter(Uri uri) {
        return TextUtils.equals("true",
                uri.getQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER));
    }

    private ScheduleContract() {
    }
}
