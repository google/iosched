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

import android.app.SearchManager;
import android.graphics.Color;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.google.samples.apps.iosched.io.model.Block;
import com.google.samples.apps.iosched.util.ParserUtils;

import java.util.List;

/**
 * Contract class for interacting with {@link ScheduleProvider}. Unless otherwise noted, all
 * time-based fields are milliseconds since epoch and can be compared against
 * {@link System#currentTimeMillis()}.
 * <p>
 * The backing {@link android.content.ContentProvider} assumes that {@link android.net.Uri}
 * are generated using stronger {@link java.lang.String} identifiers, instead of
 * {@code int} {@link android.provider.BaseColumns#_ID} values, which are prone to shuffle during
 * sync.
 */
public final class ScheduleContract {

    public static final String CONTENT_TYPE_APP_BASE = "androidito-jz.";

    public static final String CONTENT_TYPE_BASE = "vnd.android.cursor.dir/vnd."
            + CONTENT_TYPE_APP_BASE;

    public static final String CONTENT_ITEM_TYPE_BASE = "vnd.android.cursor.item/vnd."
            + CONTENT_TYPE_APP_BASE;

    public interface SyncColumns {

        /** Last time this entry was updated or synchronized. */
        String UPDATED = "updated";
    }

    interface BlocksColumns {
        String BLOCK_ID = "block_id";
        String BLOCK_TITLE = "block_title";
        String BLOCK_START = "block_start";
        String BLOCK_END = "block_end";
        String BLOCK_TYPE = "block_type";
        String BLOCK_SUBTITLE = "block_subtitle";
        String BLOCK_META = "block_meta";
    }

    interface TagsColumns {
        /**
         * Tag category. For example, the tags that identify what topic a session pertains
         * to might belong to the "TOPIC" category; the tags that identify what type a session
         * is (codelab, office hours, etc) might belong to the "TYPE" category.
         */
        String TAG_CATEGORY = "tag_category";
        /** Tag name. For example, "Android". */
        String TAG_NAME = "tag_name";
        /** Tag's order in its category (for sorting). */
        String TAG_ORDER_IN_CATEGORY = "tag_order_in_category";
        /** Tag's color, in integer format. */
        String TAG_COLOR = "tag_color";
        /** Tag abstract. Short summary describing tag. */
        String TAG_ABSTRACT = "tag_abstract";
    }

    interface TracksColumns {
        String TRACK_ID = "track_id";
        String TRACK_NAME = "track_name";
        String TRACK_COLOR = "track_color";
        String TRACK_ABSTRACT = "track_abstract";
    }

    interface RoomsColumns {
        String ROOM_ID = "room_id";
        String ROOM_NAME = "room_name";
        String ROOM_FLOOR = "room_floor";
    }

    interface MyScheduleColumns {

        String SESSION_ID = SessionsColumns.SESSION_ID;
        /** Account name for which the session is starred (in my schedule) */
        String MY_SCHEDULE_ACCOUNT_NAME = "account_name";
        /**
         * Indicate if last operation was "add" (true) or "remove" (false). Since uniqueness is
         * given by seesion_id+account_name, this field can be used as a way to find removals and
         * sync them with the cloud
         */
        String MY_SCHEDULE_IN_SCHEDULE = "in_schedule";
        /** Flag to indicate if the corresponding in_my_schedule item needs to be synced */
        String MY_SCHEDULE_DIRTY_FLAG = "dirty";
    }

    interface MyFeedbackSubmittedColumns {

        String SESSION_ID = SessionsColumns.SESSION_ID;
        /** Account name for which the session has had feedback submitted. */
        String MY_FEEDBACK_SUBMITTED_ACCOUNT_NAME = "account_name";
        /** Flag to indicate if the corresponding item needs to be synced */
        String MY_FEEDBACK_SUBMITTED_DIRTY_FLAG = "dirty";
    }

    interface SessionsColumns {

        /** Unique string identifying this session. */
        String SESSION_ID = "session_id";
        /** The type of session (session, keynote, codelab, etc). */
        String SESSION_TYPE = "session_type";
        /** Difficulty level of the session. */
        String SESSION_LEVEL = "session_level";
        /** Start time of this track. */
        String SESSION_START = "session_start";
        /** End time of this track. */
        String SESSION_END = "session_end";
        /** Title describing this track. */
        String SESSION_TITLE = "session_title";
        /** Body of text explaining this session in detail. */
        String SESSION_ABSTRACT = "session_abstract";
        /** Requirements that attendees should meet. */
        String SESSION_REQUIREMENTS = "session_requirements";
        /** Kewords/tags for this session. */
        String SESSION_KEYWORDS = "session_keywords";
        /** Hashtag for this session. */
        String SESSION_HASHTAG = "session_hashtag";
        /** Full URL to session online. */
        String SESSION_URL = "session_url";
        /** Full URL to YouTube. */
        String SESSION_YOUTUBE_URL = "session_youtube_url";
        /** Full URL to PDF. */
        String SESSION_PDF_URL = "session_pdf_url";
        /** Full URL to official session notes. */
        String SESSION_NOTES_URL = "session_notes_url";
        /** User-specific flag indicating starred status. */
        String SESSION_IN_MY_SCHEDULE = "session_in_my_schedule";
        /** Key for session Calendar event. (Used in ICS or above) */
        String SESSION_CAL_EVENT_ID = "session_cal_event_id";
        /** The YouTube live stream URL. */
        String SESSION_LIVESTREAM_ID = "session_livestream_url";
        /** The Moderator URL. */
        String SESSION_MODERATOR_URL = "session_moderator_url";
        String SESSION_TRACKS = "session_tracks";
        /** The set of tags the session has. This is a comma-separated list of tags. */
        String SESSION_TAGS = "session_tags";
        /** The names of the speakers on this session, formatted for display. */
        String SESSION_SPEAKER_NAMES = "session_speaker_names";
        /** The order (for sorting) of this session's type. */
        String SESSION_GROUPING_ORDER = "session_grouping_order";
        /** The hashcode of the data used to create this record. */
        String SESSION_IMPORT_HASHCODE = "session_import_hashcode";
        /** The session's main tag. */
        String SESSION_MAIN_TAG = "session_main_tag";
        /** User-specific flag indicating starred status. */
        String SESSION_STARRED = "session_starred";
        /** The session's branding color. */
        String SESSION_COLOR = "session_color";
        /** The session's captions URL (for livestreamed sessions). */
        String SESSION_CAPTIONS_URL = "session_captions_url";
        /** The session interval when using the interval counter query. */
        String SESSION_INTERVAL_COUNT = "session_interval_count";
        /** The session's photo URL. */
        String SESSION_PHOTO_URL = "session_photo_url";
        /** The session's related content (videos and call to action links). */
        String SESSION_RELATED_CONTENT = "session_related_content";
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
        String ANNOUNCEMENT_ID = "announcement_id";
        String ANNOUNCEMENT_TITLE = "announcement_title";
        String ANNOUNCEMENT_SUMMARY = "announcement_summary";
        String ANNOUNCEMENT_TRACKS = "announcement_tracks";
        String ANNOUNCEMENT_ACTIVITY_JSON = "announcement_activity_json";
        String ANNOUNCEMENT_URL = "announcement_url";
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
    }

    interface FeedbackColumns {

        String SESSION_ID = "session_id";
        String SESSION_RATING = "feedback_session_rating";
        String ANSWER_RELEVANCE = "feedback_answer_q1";
        String ANSWER_CONTENT = "feedback_answer_q2";
        String ANSWER_SPEAKER = "feedback_answer_q3";
        String COMMENTS = "feedback_comments";
        String SYNCED = "synced";
    }

    interface MapTileColumns {

        /** Floor * */
        String TILE_FLOOR = "map_tile_floor";
        /** Filename * */
        String TILE_FILE = "map_tile_file";
        /** Url * */
        String TILE_URL = "map_tile_url";
    }

    interface HashtagColumns {

        /** Hashtags */
        String HASHTAG_NAME = "hashtag_name";
        /** Description about this hashtag. */
        String HASHTAG_DESCRIPTION = "hashtag_description";
        /** Text color for this hashtag. */
        String HASHTAG_COLOR = "hashtag_color";
        /** Ordering of this hashtag. */
        String HASHTAG_ORDER = "hashtag_order";
    }

    interface VideoColumns {

        /** Unique string identifying this video. */
        String VIDEO_ID = "video_id";
        /** Year of the video (e.g. 2014, 2013, ...). */
        String VIDEO_YEAR = "video_year";
        /** Title of the video. */
        String VIDEO_TITLE = "video_title";
        /** Description of the video. */
        String VIDEO_DESC = "video_desc";
        /** Youtube video ID (just the alphanumeric string, not the whole URL). */
        String VIDEO_VID = "video_vid";
        /** Topic (e.g. "Android"). */
        String VIDEO_TOPIC = "video_topic";
        /** Speaker(s) (e.g. "Lauren Ipsum"). */
        String VIDEO_SPEAKERS = "video_speakers";
        /** Thumbnail url. */
        String VIDEO_THUMBNAIL_URL = "video_thumbnail_url";
        /** Import hashcode. */
        String VIDEO_IMPORT_HASHCODE = "video_import_hashcode";
    }

    public static final String CONTENT_AUTHORITY = "no.java.schedule";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    private static final String PATH_AT = "at";
    private static final String PATH_BETWEEN = "between";
    private static final String PATH_TRACKS = "tracks";
    private static final String PATH_WITH_TRACK = "with_track";
    private static final String PATH_STARRED = "starred";
    private static final String PATH_VENDORS = "vendors";
    private static final String PATH_BLOCKS = "blocks";
    private static final String PATH_AFTER = "after";
    private static final String PATH_TAGS = "tags";
    private static final String PATH_ROOM = "room";
    private static final String PATH_UNSCHEDULED = "unscheduled";
    private static final String PATH_ROOMS = "rooms";
    private static final String PATH_SESSIONS = "sessions";
    private static final String PATH_FEEDBACK = "feedback";
    private static final String PATH_MY_SCHEDULE = "my_schedule";
    private static final String PATH_MY_FEEDBACK_SUBMITTED = "my_feedback_submitted";
    private static final String PATH_SESSIONS_COUNTER = "counter";
    private static final String PATH_SPEAKERS = "speakers";
    private static final String PATH_ANNOUNCEMENTS = "announcements";
    private static final String PATH_MAP_MARKERS = "mapmarkers";
    private static final String PATH_MAP_FLOOR = "floor";
    private static final String PATH_MAP_TILES = "maptiles";
    private static final String PATH_HASHTAGS = "hashtags";
    private static final String PATH_VIDEOS = "videos";
    private static final String PATH_SEARCH = "search";
    private static final String PATH_SEARCH_SUGGEST = "search_suggest_query";
    private static final String PATH_SEARCH_INDEX = "search_index";
    private static final String PATH_PEOPLE_IVE_MET = "people_ive_met";

    public static final String[] TOP_LEVEL_PATHS = {
            PATH_BLOCKS,
            PATH_TAGS,
            PATH_TRACKS,
            PATH_ROOMS,
            PATH_SESSIONS,
            PATH_FEEDBACK,
            PATH_STARRED,
            PATH_MY_SCHEDULE,
            PATH_SPEAKERS,
            PATH_ANNOUNCEMENTS,
            PATH_VENDORS,
            PATH_MAP_MARKERS,
            PATH_MAP_FLOOR,
            PATH_MAP_MARKERS,
            PATH_MAP_TILES,
            PATH_HASHTAGS,
            PATH_VIDEOS,
            PATH_PEOPLE_IVE_MET
    };

    public static final String[] USER_DATA_RELATED_PATHS = {
            PATH_SESSIONS,
            PATH_MY_SCHEDULE
    };

    public static String makeContentType(String id) {
        if (id != null) {
            return CONTENT_TYPE_BASE + id;
        } else {
            return null;
        }
    }

    public static String makeContentItemType(String id) {
        if (id != null) {
            return CONTENT_ITEM_TYPE_BASE + id;
        } else {
            return null;
        }
    }

    public static Uri addCallerIsSyncAdapterParameter(Uri uri) {
        return uri.buildUpon().appendQueryParameter(
                ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
    }

    public static boolean hasCallerIsSyncAdapterParameter(Uri uri) {
        return TextUtils.equals("true",
                uri.getQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER));
    }

    /**
     * Blocks are generic timeslots.
     */
    public static class Blocks implements BlocksColumns, BaseColumns {
        public static final String BLOCK_TYPE_FREE = "free";
        public static final String BLOCK_TYPE_BREAK = "break";
        public static final String BLOCK_TYPE_KEYNOTE = "keynote";

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_BLOCKS).build();

        public static final boolean isValidBlockType(String type) {
            return BLOCK_TYPE_FREE.equals(type) || BLOCK_TYPE_BREAK.equals(type)
                    || BLOCK_TYPE_KEYNOTE.equals(type);
        }

        public static final String SESSIONS_COUNT = "sessions_count";
        public static final String CONTENT_TYPE_ID = "block";
        public static final String NUM_STARRED_SESSIONS = "num_starred_sessions";
        public static final String STARRED_SESSION_ID = "starred_session_id";
        public static final String STARRED_SESSION_TITLE = "starred_session_title";
        public static final String STARRED_SESSION_LIVESTREAM_URL =
                "starred_session_livestream_url";
        public static final String STARRED_SESSION_ROOM_NAME = "starred_session_room_name";
        public static final String STARRED_SESSION_ROOM_ID = "starred_session_room_id";
        public static final String STARRED_SESSION_HASHTAGS = "starred_session_hashtags";
        public static final String STARRED_SESSION_URL = "starred_session_url";

        public static final String DEFAULT_SORT = BlocksColumns.BLOCK_START + " ASC, "
                + BlocksColumns.BLOCK_END + " ASC";

        /** Build {@link Uri} for requested {@link #BLOCK_ID}. */
        public static Uri buildBlockUri(String blockId) {
            return CONTENT_URI.buildUpon().appendPath(blockId).build();
        }
        public static Uri buildStarredSessionsUri(String blockId) {
            return CONTENT_URI.buildUpon().appendPath(blockId).appendPath(PATH_SESSIONS)
                    .appendPath(PATH_STARRED).build();
        }

        public static Uri buildSessionsUri(String blockId) {
            return CONTENT_URI.buildUpon().appendPath(blockId).appendPath(PATH_SESSIONS).build();
        }

        /** Read {@link #BLOCK_ID} from {@link Blocks} {@link Uri}. */
        public static String getBlockId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        /**
         * Generate a {@link #BLOCK_ID} that will always match the requested
         * {@link Blocks} details.
         *
         * @param startTime the block start time, in milliseconds since Epoch UTC
         * @param endTime   the block end time, in milliseconds since Epoch UTF
         */
        public static String generateBlockId(long startTime, long endTime) {
            startTime /= DateUtils.SECOND_IN_MILLIS;
            endTime /= DateUtils.SECOND_IN_MILLIS;
            return ParserUtils.sanitizeId(startTime + "-" + endTime);
        }

        public static final String EMPTY_SESSIONS_SELECTION = "(" + BLOCK_TYPE
                + " = '" + ParserUtils.BLOCK_TYPE_SESSION + "' OR " + BLOCK_TYPE
                + " = '" + ParserUtils.BLOCK_TYPE_CODE_LAB + "') AND "
                + SESSIONS_COUNT + " = 0";
    }

    /**
     * Tags represent Session classifications. A session can have many tags. Tags can indicate,
     * for example, what product a session pertains to (Android, Chrome, ...), what type
     * of session it is (session, codelab, office hours, ...) and what overall event theme
     * it falls under (Design, Develop, Distribute), amongst others.
     */
    public static class Tags implements TagsColumns, BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TAGS).build();

        public static final String CONTENT_TYPE_ID = "tag";

        // Used for tag search projection.
        public static final String TAG_ORDER_BY_CATEGORY = Tags.TAG_ORDER_IN_CATEGORY + " ASC";

        /**
         * Build {@link Uri} that references all tags.
         */
        public static Uri buildTagsUri() {
            return CONTENT_URI;
        }

        /** Build a {@link Uri} that references a given tag. */
        public static Uri buildTagUri(String tagId) {
            return CONTENT_URI.buildUpon().appendPath(tagId).build();
        }

        public static String getTagName(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    /**
     * MySchedule represent the sessions that the user has starred/added to the "my schedule".
     * Each row of MySchedule represents one session in one account's my schedule.
     */
    public static class MySchedule implements MyScheduleColumns, BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_MY_SCHEDULE).build();

        public static final String CONTENT_TYPE_ID = "myschedule";

        public static Uri buildMyScheduleUri(String accountName) {
            return ScheduleContractHelper.addOverrideAccountName(CONTENT_URI, accountName);
        }

    }

    /**
     * MyFeedbackSubmitted represent the sessions that which the user has submitted feedback.
     * Each row of MyFeedbackSubmitted represents one session for which feedback was submitted by
     * one account.
     */
    public static class MyFeedbackSubmitted implements MyFeedbackSubmittedColumns, BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_MY_FEEDBACK_SUBMITTED).build();

        public static final String CONTENT_TYPE_ID = "myfeedbacksubmitted";

        public static Uri buildMyFeedbackSubmittedUri(String accountName) {
            return ScheduleContractHelper.addOverrideAccountName(CONTENT_URI, accountName);
        }

    }

    /**
     * Tracks are overall categories for {@link Sessions} and {@link Vendors},
     * such as "Android" or "Enterprise."
     */
    public static class Tracks implements TracksColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TRACKS).build();
        public static final String CONTENT_TYPE_ID = "track";

        /** "All tracks" ID. */
        public static final String ALL_TRACK_ID = "all";
        public static final String CODELABS_TRACK_ID = generateTrackId("Code Labs");
        public static final String TECH_TALK_TRACK_ID = generateTrackId("Tech Talk");

        public static final String SESSIONS_COUNT = "sessions_count";
        public static final String VENDORS_COUNT = "vendors_count";

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

    public static class Rooms implements RoomsColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ROOMS).build();

        public static final String CONTENT_TYPE_ID = "room";

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

    public static class Feedback implements BaseColumns, FeedbackColumns, SyncColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_FEEDBACK).build();

        public static final String CONTENT_TYPE_ID = "session_feedback";

        /** Build {@link Uri} to feedback for given session. */
        public static Uri buildFeedbackUri(String sessionId) {
            return CONTENT_URI.buildUpon().appendPath(sessionId).build();
        }

        /** Read {@link #SESSION_ID} from {@link Feedback} {@link Uri}. */
        public static String getSessionId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
        public static String getSearchQuery(Uri uri) {
            return uri.getPathSegments().get(2);
        }

    }

    /**
     * Each session has zero or more {@link Tags}, a {@link Rooms},
     * zero or more {@link Speakers}.
     */
    public static class Sessions implements SessionsColumns, RoomsColumns, BlocksColumns,
            SyncColumns, BaseColumns {

        public static final String QUERY_PARAMETER_TAG_FILTER = "filter";
        public static final String QUERY_PARAMETER_CATEGORIES = "categories";

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SESSIONS).build();
        public static final Uri CONTENT_STARRED_URI =
                CONTENT_URI.buildUpon().appendPath(PATH_STARRED).build();

        public static final Uri CONTENT_MY_SCHEDULE_URI =
                CONTENT_URI.buildUpon().appendPath(PATH_MY_SCHEDULE).build();

        public static final String CONTENT_TYPE_ID = "session";
        public static final String BLOCK_ID = "block_id";
        public static final String ROOM_ID = "room_id";
        public static final String START = "session_start";
        public static final String END = "session_end";

        public static final String SEARCH_SNIPPET = "search_snippet";
        public static final String HAS_GIVEN_FEEDBACK = "has_given_feedback";

        public static final String DEFAULT_SORT = BlocksColumns.BLOCK_START + " ASC,"
                + SessionsColumns.SESSION_TITLE + " COLLATE NOCASE ASC";

        public static final String BLOCK_SESSION_SORT =
                ROOM_NAME + " ASC,"+
                        SessionsColumns.SESSION_START + " ASC";

        // ORDER BY clauses
        public static final String SORT_BY_TYPE_THEN_TIME = SESSION_GROUPING_ORDER + " ASC,"
                + BLOCK_START + " ASC," + SESSION_TITLE + " COLLATE NOCASE ASC";

        public static final String LIVESTREAM_SELECTION =
                SESSION_LIVESTREAM_ID + " is not null AND " + SESSION_LIVESTREAM_ID + "!=''";

        public static final String LIVESTREAM_OR_YOUTUBE_URL_SELECTION = "(" +
                SESSION_LIVESTREAM_ID + " is not null AND " + SESSION_LIVESTREAM_ID +
                "!='') OR (" +
                SESSION_YOUTUBE_URL + " is not null AND " + SESSION_YOUTUBE_URL + " != '')";

        // Used to fetch sessions starting within a specific time interval

        public static final String STARTING_AT_TIME_INTERVAL_SELECTION =
                BLOCK_START + " < ? and " +  BLOCK_END + " " + "> ?";

        // Used to fetch upcoming sessions
        public static final String UPCOMING_SELECTION =
                BlocksColumns.BLOCK_START + " = (select min(" +  BlocksColumns.BLOCK_START + ") from " +
                        ScheduleDatabase.Tables.BLOCKS_JOIN_SESSIONS + " where " + LIVESTREAM_SELECTION +
                        " and " +  BLOCK_START + " >" + " ?)";

        // Builds selectionArgs for {@link STARTING_AT_TIME_INTERVAL_SELECTION}
        public static String[] buildAtTimeIntervalArgs(long intervalStart, long intervalEnd) {
            return new String[]{String.valueOf(intervalStart),
                    String.valueOf(intervalEnd)};
        }

        public static Uri buildTracksDirUri(String sessionId) {
            return CONTENT_URI.buildUpon().appendPath(sessionId).appendPath(PATH_TRACKS).build();
        }

        // Builds selectionArgs for {@link AT_TIME_SELECTION}
        public static String[] buildAtTimeSelectionArgs(long time) {
            final String timeString = String.valueOf(time);
            return new String[]{timeString, timeString};
        }

        // Used to fetch upcoming sessions
        public static final String UPCOMING_LIVE_SELECTION = SESSION_START + " > ?";

        // Builds selectionArgs for {@link UPCOMING_LIVE_SELECTION}
        public static String[] buildUpcomingSelectionArgs(long minTime) {
            return new String[]{String.valueOf(minTime)};
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
         * Build {@link Uri} that references any {@link Tags} associated with
         * the requested {@link #SESSION_ID}.
         */
        public static Uri buildTagsDirUri(String sessionId) {
            return CONTENT_URI.buildUpon().appendPath(sessionId).appendPath(PATH_TAGS).build();
        }

        /**
         * Build {@link Uri} that references sessions that match the query. The query can be
         * multiple words separated with spaces.
         *
         * @param query The query. Can be multiple words separated by spaces.
         * @return {@link Uri} to the sessions
         */
        public static Uri buildSearchUri(String query) {
            if (null == query) {
                query = "";
            }
            // convert "lorem ipsum dolor sit" to "lorem* ipsum* dolor* sit*"
            query = query.replaceAll(" +", " *") + "*";
            return CONTENT_URI.buildUpon()
                    .appendPath(PATH_SEARCH).appendPath(query).build();
        }

        public static boolean isSearchUri(Uri uri) {
            List<String> pathSegments = uri.getPathSegments();
            return pathSegments.size() >= 2 && PATH_SEARCH.equals(pathSegments.get(1));
        }

        /**
         * Build {@link Uri} that references sessions in a room that have begun after the requested
         * time *
         */
        public static Uri buildSessionsInRoomAfterUri(String room, long time) {
            return CONTENT_URI.buildUpon().appendPath(PATH_ROOM).appendPath(room)
                    .appendPath(PATH_AFTER)
                    .appendPath(String.valueOf(time)).build();
        }

        /**
         * Build {@link Uri} that references sessions that have begun after the requested time.
         */
        public static Uri buildSessionsAfterUri(long time) {
            return CONTENT_URI.buildUpon().appendPath(PATH_AFTER)
                    .appendPath(String.valueOf(time)).build();
        }

        /**
         * Build {@link Uri} that references sessions not in user's schedule that happen in the
         * specified interval *
         */
        public static Uri buildUnscheduledSessionsInInterval(long start, long end) {
            String interval = start + "-" + end;
            return CONTENT_URI.buildUpon().appendPath(PATH_UNSCHEDULED).appendPath(interval)
                    .build();
        }

        public static boolean isUnscheduledSessionsInInterval(Uri uri) {
            return uri != null && uri.toString().startsWith(
                    CONTENT_URI.buildUpon().appendPath(PATH_UNSCHEDULED).toString());
        }

        public static long[] getInterval(Uri uri) {
            if (uri == null) {
                return null;
            }
            List<String> segments = uri.getPathSegments();
            if (segments.size() == 3 && segments.get(2).indexOf('-') > 0) {
                String[] interval = segments.get(2).split("-");
                return new long[]{Long.parseLong(interval[0]), Long.parseLong(interval[1])};
            }
            return null;
        }

        public static String getRoom(Uri uri) {
            return uri.getPathSegments().get(2);
        }

        public static String getAfterForRoom(Uri uri) {
            return uri.getPathSegments().get(4);
        }

        public static String getAfter(Uri uri) {
            return uri.getPathSegments().get(2);
        }


        /** Read {@link #SESSION_ID} from {@link Sessions} {@link Uri}. */
        public static String getSessionId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static String getSearchQuery(Uri uri) {
            List<String> segments = uri.getPathSegments();
            if (2 < segments.size()) {
                return segments.get(2);
            }
            return null;
        }

        public static boolean hasFilterParam(Uri uri) {
            return uri != null && uri.getQueryParameter(QUERY_PARAMETER_TAG_FILTER) != null;
        }

        /**
         * Build {@link Uri} that references all sessions that have ALL of the indicated tags.
         * @param contentUri The base Uri that is used for adding the required tags.
         * @param requiredTags The tags that are used for creating the query parameter.
         * @return uri The uri updated to include the indicated tags.
         */
        @Deprecated
        public static Uri buildTagFilterUri(Uri contentUri, String[] requiredTags) {
            return buildCategoryTagFilterUri(contentUri, requiredTags,
                    requiredTags == null ? 0 : requiredTags.length);
        }

        /** Build {@link Uri} that references all sessions that have ALL of the indicated tags. */
        @Deprecated
        public static Uri buildTagFilterUri(String[] requiredTags) {
            return buildTagFilterUri(CONTENT_URI, requiredTags);
        }

        /**
         * Build {@link Uri} that references all sessions that have the following tags and
         * satisfy the requirement of containing ALL the categories
         * @param contentUri The base Uri that is used for adding the query parameters.
         * @param tags The various tags that can include topics, themes as well as types.
         * @param categories The number of categories that are required. At most this can be 3,
         *                   since a session can belong only to one type + topic + theme.
         * @return Uri representing the query parameters for the filter as well as the categories.
         */
        public static Uri buildCategoryTagFilterUri(Uri contentUri, String[] tags, int categories) {
            StringBuilder sb = new StringBuilder();
            for (String tag : tags) {
                if (TextUtils.isEmpty(tag)) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(tag.trim());
            }
            if (sb.length() == 0) {
                return contentUri;
            } else {
                return contentUri.buildUpon()
                        .appendQueryParameter(QUERY_PARAMETER_TAG_FILTER, sb.toString())
                        .appendQueryParameter(QUERY_PARAMETER_CATEGORIES,
                                String.valueOf(categories))
                        .build();
            }
        }

        /** Build {@link Uri} that counts sessions by start/end intervals. */
        public static Uri buildCounterByIntervalUri() {
            return CONTENT_URI.buildUpon().appendPath(PATH_SESSIONS_COUNTER).build();
        }
    }

    /**
     * Speakers are individual people that lead {@link Sessions}.
     */
    public static class Speakers implements SpeakersColumns, SyncColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SPEAKERS).build();

        public static final String CONTENT_TYPE_ID = "speaker";

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
        public static final String CONTENT_TYPE_ID = "vendor";

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

    public static class Announcements implements AnnouncementsColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ANNOUNCEMENTS).build();

        public static final String CONTENT_TYPE_ID = "announcement";

        public static final String DEFAULT_SORT = AnnouncementsColumns.ANNOUNCEMENT_DATE
                + " COLLATE NOCASE ASC";

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

        public static final String CONTENT_TYPE_ID = "maptiles";

        /** Build {@link Uri} for all overlay zoom entries. */
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

        public static final String CONTENT_TYPE_ID = "mapmarker";

        /** Build {@link Uri} for requested {@link #MARKER_ID}. */
        public static Uri buildMarkerUri(String markerId) {
            return CONTENT_URI.buildUpon().appendPath(markerId).build();
        }

        /** Build {@link Uri} for all markers. */
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

        /** Read FLOOR from {@link MapMarkers} {@link Uri}. */
        public static String getMarkerFloor(Uri uri) {
            return uri.getPathSegments().get(2);
        }

    }

    /**
     * Hashtags are used for Google+ search. This model is independent from other models.
     */
    public static class Hashtags implements HashtagColumns, BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_HASHTAGS).build();

        public static final String CONTENT_TYPE_ID = "hashtags";

        /** Build {@link Uri} for requested hashtag. */
        public static Uri buildHashtagUri(String hashtag) {
            return CONTENT_URI.buildUpon().appendPath(hashtag).build();
        }

        /** Read hashtag from {@link Hashtags} {@link Uri}. */
        public static String getHashtagName(Uri uri) {
            return uri.getPathSegments().get(1);
        }

    }

    /**
     * Videos are displayed in the Video Library. They are links to Youtube plus metadata.
     */
    public static class Videos implements VideoColumns, BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_VIDEOS).build();

        public static final String CONTENT_TYPE_ID = "videos";

        public static final String DEFAULT_SORT = VideoColumns.VIDEO_TOPIC + " ASC, "
                + VideoColumns.VIDEO_YEAR + " DESC, " + VideoColumns.VIDEO_TITLE + " ASC";

        /** Build {@link Uri} for given video. */
        public static Uri buildVideoUri(String videoId) {
            return CONTENT_URI.buildUpon().appendPath(videoId).build();
        }

        /** Return video ID given URI. */
        public static String getVideoId(Uri uri) {
            return uri.getPathSegments().get(1);
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

    public static class SearchTopicsSessions {
        public static final String PATH_SEARCH_TOPICS_SESSIONS = "search_topics_sessions";

        public static final String CONTENT_TYPE_ID = "search_topics_sessions";

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SEARCH_TOPICS_SESSIONS).build();

        public static final String TOPIC_TAG_SELECTION = Tags.TAG_CATEGORY + "= ? and " +
                Tags.TAG_NAME + " like ?";

        public static final String[] TOPIC_TAG_PROJECTION = {
                BaseColumns._ID,
                Tags.TAG_NAME,
        };

        public static final String[] SEARCH_SESSIONS_PROJECTION = {
                BaseColumns._ID,
                ScheduleContract.Sessions.SESSION_ID,
                ScheduleContract.Sessions.SEARCH_SNIPPET
        };

        public static final String[] DEFAULT_PROJECTION = new String[] {
                BaseColumns._ID,
                SearchTopicSessionsColumns.TAG_OR_SESSION_ID,
                SearchTopicSessionsColumns.SEARCH_SNIPPET,
                SearchTopicSessionsColumns.IS_TOPIC_TAG,
        };
    }

    /**
     * Columns for an in memory table created on query using
     * the Tags table and the SearchSessions table.
     */
    public interface SearchTopicSessionsColumns extends BaseColumns {
        /* This column contains either a tag_id or a session_id */
        String TAG_OR_SESSION_ID = "tag_or_session_id";
        /* This column contains the search snippet to be shown to the user.*/
        String SEARCH_SNIPPET = "search_snippet";
        /* Indicates whether this row is a topic tag or a session_id. */
        String IS_TOPIC_TAG = "is_topic_tag";
    }

    private ScheduleContract() {
    }
}
