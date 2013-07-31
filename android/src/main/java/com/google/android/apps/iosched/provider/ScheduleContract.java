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
        /** The level (1 being primary, 2 being secondary) of the track. */
        String TRACK_LEVEL = "track_level";
        /** The sort order of the track within the level. */
        String TRACK_ORDER_IN_LEVEL = "track_order_in_level";
        /** Type of meta-track this is, or 0 if not meta. */
        String TRACK_META = "track_is_meta";
        /** Type of track. */
        String TRACK_ABSTRACT = "track_abstract";
        /** Hashtag for track. */
        String TRACK_HASHTAG = "track_hashtag";
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
        /** Kewords/tags for this session. */
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
        /** The Moderator URL. */
        String SESSION_MODERATOR_URL = "session_moderator_url";
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

    interface SandboxColumns {
        /** Unique string identifying this sandbox company. */
        String COMPANY_ID = "company_id";
        /** Name of this sandbox company. */
        String COMPANY_NAME = "company_name";
        /** Body of text describing this sandbox company. */
        String COMPANY_DESC = "company_desc";
        /** Link to sandbox company online. */
        String COMPANY_URL = "company_url";
        /** Link to sandbox company logo. */
        String COMPANY_LOGO_URL = "company_logo_url";
    }

    interface AnnouncementsColumns {
        /** Unique string identifying this announcment. */
        String ANNOUNCEMENT_ID = "announcement_id";
        /** Title of the announcement. */
        String ANNOUNCEMENT_TITLE = "announcement_title";
        /** Google+ activity JSON for the announcement. */
        String ANNOUNCEMENT_ACTIVITY_JSON = "announcement_activity_json";
        /** Full URL for the announcement. */
        String ANNOUNCEMENT_URL = "announcement_url";
        /** Date of the announcement. */
        String ANNOUNCEMENT_DATE = "announcement_date";
    }

    interface MapMarkerColumns {
        /** Unique string identifying this marker. */
        String MARKER_ID = "map_marker_id";
        /** Type of marker. */
        String MARKER_TYPE = "map_marker_type";
        /** Latitudinal position of marker. */
        String MARKER_LATITUDE = "map_marker_latitude";
        /** Longitudinal position of marker. */
        String MARKER_LONGITUDE = "map_marker_longitude";
        /** Label (title) for this marker. */
        String MARKER_LABEL = "map_marker_label";
        /** Building floor this marker is on. */
        String MARKER_FLOOR = "map_marker_floor";
        /** Track of sandbox marker */
        String MARKER_TRACK = "track_id";
    }

    interface FeedbackColumns {
        String SESSION_ID = "session_id";
        String SESSION_RATING = "feedback_session_rating";
        String ANSWER_RELEVANCE = "feedback_answer_q1";
        String ANSWER_CONTENT = "feedback_answer_q2";
        String ANSWER_SPEAKER = "feedback_answer_q3";
        String ANSWER_WILLUSE = "feedback_answer_q4";
        String COMMENTS = "feedback_comments";
    }

    interface MapTileColumns {
        /** Floor **/
        String TILE_FLOOR = "map_tile_floor";
        /** Filename **/
        String TILE_FILE = "map_tile_file";
        /** Url **/
        String TILE_URL = "map_tile_url";
    }

    public static final String CONTENT_AUTHORITY = "com.google.android.apps.iosched";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    private static final String PATH_BLOCKS = "blocks";
    private static final String PATH_AT = "at";
    private static final String PATH_AFTER = "after";
    private static final String PATH_BETWEEN = "between";
    private static final String PATH_TRACKS = "tracks";
    private static final String PATH_ROOM = "room";
    private static final String PATH_ROOMS = "rooms";
    private static final String PATH_SESSIONS = "sessions";
    private static final String PATH_FEEDBACK = "feedback";
    private static final String PATH_WITH_TRACK = "with_track";
    private static final String PATH_STARRED = "starred";
    private static final String PATH_SPEAKERS = "speakers";
    private static final String PATH_SANDBOX = "sandbox";
    private static final String PATH_ANNOUNCEMENTS = "announcements";
    private static final String PATH_MAP_MARKERS = "mapmarkers";
    private static final String PATH_MAP_FLOOR = "floor";
    private static final String PATH_MAP_TILES= "maptiles";
    private static final String PATH_SEARCH = "search";
    private static final String PATH_SEARCH_SUGGEST = "search_suggest_query";
    private static final String PATH_SEARCH_INDEX = "search_index";

    /**
     * Blocks are generic timeslots that {@link Sessions} and other related
     * events fall into.
     */
    public static class Blocks implements BlocksColumns, BaseColumns {
        public static final String BLOCK_TYPE_GENERIC = "generic";
        public static final String BLOCK_TYPE_FOOD = "food";
        public static final String BLOCK_TYPE_SESSION = "session";
        public static final String BLOCK_TYPE_CODELAB = "codelab";
        public static final String BLOCK_TYPE_KEYNOTE = "keynote";
        public static final String BLOCK_TYPE_OFFICE_HOURS = "officehours";
        public static final String BLOCK_TYPE_SANDBOX = "sandbox_only";


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
         * Flag indicating the number of sessions inside this block that have a
         * {@link Sessions#SESSION_LIVESTREAM_URL} set.
         */
        public static final String NUM_LIVESTREAMED_SESSIONS = "num_livestreamed_sessions";

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

        public static final String EMPTY_SESSIONS_SELECTION = BLOCK_TYPE + " IN ('"
                + Blocks.BLOCK_TYPE_SESSION + "','"
                + Blocks.BLOCK_TYPE_CODELAB + "','"
                + Blocks.BLOCK_TYPE_OFFICE_HOURS + "') AND "
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
     * Tracks are overall categories for {@link Sessions} and {@link com.google.android.apps.iosched.provider.ScheduleContract.Sandbox},
     * such as "Android" or "Enterprise."
     */
    public static class Tracks implements TracksColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TRACKS).build();

        public static final int TRACK_META_NONE = 0;
        public static final int TRACK_META_SESSIONS_ONLY = 1;
        public static final int TRACK_META_SANDBOX_OFFICE_HOURS_ONLY = 2;
        public static final int TRACK_META_OFFICE_HOURS_ONLY = 3;

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.iosched.track";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.iosched.track";

        /** "All tracks" ID. */
        public static final String ALL_TRACK_ID = "all";

        /** Count of {@link Sessions} inside given track that aren't office hours. */
        public static final String SESSIONS_COUNT = "sessions_count";
        /** Count of {@link Sessions} inside given track that are office hours. */
        public static final String OFFICE_HOURS_COUNT = "office_hours_count";
        /** Count of {@link com.google.android.apps.iosched.provider.ScheduleContract.Sandbox} inside given track. */
        public static final String SANDBOX_COUNT = "sandbox_count";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = TracksColumns.TRACK_LEVEL + ", "
                + TracksColumns.TRACK_ORDER_IN_LEVEL + ", "
                + TracksColumns.TRACK_NAME;

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
         * Build {@link Uri} that references any {@link com.google.android.apps.iosched.provider.ScheduleContract.Sandbox} associated with
         * the requested {@link #TRACK_ID}.
         */
        public static Uri buildSandboxUri(String trackId) {
            return CONTENT_URI.buildUpon().appendPath(trackId).appendPath(PATH_SANDBOX).build();
        }

        /** Read {@link #TRACK_ID} from {@link Tracks} {@link Uri}. */
        public static String getTrackId(Uri uri) {
            return uri.getPathSegments().get(1);
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

    public static class Feedback implements BaseColumns, FeedbackColumns, SyncColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_FEEDBACK).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.iosched.session_feedback";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.iosched.session_feedback";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = BaseColumns._ID + " ASC, ";

        /** Build {@link Uri} to feedback for given session. */
        public static Uri buildFeedbackUri(String sessionId) {
            return CONTENT_URI.buildUpon().appendPath(sessionId).build();
        }

        /** Read {@link #SESSION_ID} from {@link Feedback} {@link Uri}. */
        public static String getSessionId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static class Sessions implements SessionsColumns, BlocksColumns, RoomsColumns,
            SyncColumns, BaseColumns {
        public static final String SESSION_TYPE_SESSION = "SESSION";
        public static final String SESSION_TYPE_CODELAB = "CODE_LAB";
        public static final String SESSION_TYPE_KEYNOTE = "KEYNOTE";
        public static final String SESSION_TYPE_OFFICE_HOURS = "OFFICE_HOURS";
        public static final String SESSION_TYPE_SANDBOX = "DEVELOPER_SANDBOX";

        public static final String QUERY_PARAMETER_FILTER = "filter";
        public static final String QUERY_VALUE_FILTER_SESSIONS_CODELABS_ONLY
                = "sessions_codelabs_only"; // excludes keynote and office hours
        public static final String QUERY_VALUE_FILTER_OFFICE_HOURS_ONLY = "office_hours_only";

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

        public static Uri buildSearchUri(String query) {
            return CONTENT_URI.buildUpon().appendPath(PATH_SEARCH).appendPath(query).build();
        }

        public static boolean isSearchUri(Uri uri) {
            List<String> pathSegments = uri.getPathSegments();
            return pathSegments.size() >= 2 && PATH_SEARCH.equals(pathSegments.get(1));
        }

        /** Build {@link Uri} that references sessions in a room that have begun after the requested time **/
        public static Uri buildSessionsInRoomAfterUri(String room,long time) {
            return CONTENT_URI.buildUpon().appendPath(PATH_ROOM).appendPath(room).appendPath(PATH_AFTER)
                    .appendPath(String.valueOf(time)).build();
        }

        public static String getRoom(Uri uri){
            return uri.getPathSegments().get(2);
        }

        public static String getAfter(Uri uri){
            return uri.getPathSegments().get(4);
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
     * Each sandbox company is a company appearing at the conference that may be
     * associated with a specific {@link Tracks} and time block.
     */
    public static class Sandbox implements SandboxColumns, SyncColumns, BlocksColumns, RoomsColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SANDBOX).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.iosched.sandbox";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.iosched.sandbox";

        /** {@link Tracks#TRACK_ID} that this sandbox company belongs to. */
        public static final String TRACK_ID = "track_id";

        // Used to fetch sandbox companies at a particular time
        public static final String AT_TIME_IN_ROOM_SELECTION =
                BLOCK_START + " < ? and " + BLOCK_END + " " + "> ? and " + " SANDBOX.ROOM_ID = ?";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = SandboxColumns.COMPANY_NAME
                + " COLLATE NOCASE ASC";

        // Builds selectionArgs for {@link AT_TIME_SELECTION}
        public static String[] buildAtTimeInRoomSelectionArgs(long time, String roomId) {
            final String timeString = String.valueOf(time);
            return new String[] { timeString, timeString, roomId };
        }

        /** Build {@link Uri} for requested {@link #COMPANY_ID}. */
        public static Uri buildCompanyUri(String companyId) {
            return CONTENT_URI.buildUpon().appendPath(companyId).build();
        }

        /** Read {@link #COMPANY_ID} from {@link com.google.android.apps.iosched.provider.ScheduleContract.Sandbox} {@link Uri}. */
        public static String getCompanyId(Uri uri) {
            return uri.getPathSegments().get(1);
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
                + " COLLATE NOCASE DESC";

        /** Build {@link Uri} for requested {@link #ANNOUNCEMENT_ID}. */
        public static Uri buildAnnouncementUri(String announcementId) {
            return CONTENT_URI.buildUpon().appendPath(announcementId).build();
        }

        /**
         * Read {@link #ANNOUNCEMENT_ID} from {@link Announcements} {@link Uri}.
         */
        public static String getAnnouncementId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    /**
     * TileProvider entries are used to create an overlay provider for the map.
     */
    public static class MapTiles implements MapTileColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_MAP_TILES).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.iosched.maptiles";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.iosched.maptiles";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = MapTileColumns.TILE_FLOOR + " ASC";


        /** Build {@link Uri} for all overlay zoom entries */
        public static Uri buildUri() {
            return CONTENT_URI;
        }

        /** Build {@link Uri} for requested floor. */
        public static Uri buildFloorUri(String floor) {
            return CONTENT_URI.buildUpon()
                    .appendPath(String.valueOf(floor)).build();
        }

        /** Read floor from {@link MapMarkers} {@link Uri}. */
        public static String getFloorId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    /**
     * Markers refer to marked positions on the map.
     */
    public static class MapMarkers implements MapMarkerColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_MAP_MARKERS).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.iosched.mapmarker";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.iosched.mapmarker";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = MapMarkerColumns.MARKER_FLOOR
                + " ASC, " + MapMarkerColumns.MARKER_ID + " ASC";

        /** Build {@link Uri} for requested {@link #MARKER_ID}. */
        public static Uri buildMarkerUri(String markerId) {
            return CONTENT_URI.buildUpon().appendPath(markerId).build();
        }

        /** Build {@link Uri} for all markers */
        public static Uri buildMarkerUri() {
            return CONTENT_URI;
        }

        /** Build {@link Uri} for requested {@link #MARKER_ID}. */
        public static Uri buildFloorUri(int floor) {
            return CONTENT_URI.buildUpon().appendPath(PATH_MAP_FLOOR)
                    .appendPath("" + floor).build();
        }

        /** Read {@link #MARKER_ID} from {@link MapMarkers} {@link Uri}. */
        public static String getMarkerId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        /** Read {@link #FLOOR} from {@link MapMarkers} {@link Uri}. */
        public static String getMarkerFloor(Uri uri) {
            return uri.getPathSegments().get(2);
        }

    }

    public static class SearchSuggest {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SEARCH_SUGGEST).build();

        public static final String DEFAULT_SORT = SearchManager.SUGGEST_COLUMN_TEXT_1
                + " COLLATE NOCASE ASC";
    }

    public static class SearchIndex {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SEARCH_INDEX).build();
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
