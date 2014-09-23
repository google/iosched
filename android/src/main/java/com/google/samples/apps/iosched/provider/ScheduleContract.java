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

import android.app.SearchManager;
import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.ParserUtils;

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
     * Query parameter to create a distinct query.
     */
    public static final String QUERY_PARAMETER_DISTINCT = "distinct";
    public static final String OVERRIDE_ACCOUNTNAME_PARAMETER = "overrideAccount";

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
        /** Extra subtitle for the block. */
        String BLOCK_SUBTITLE = "block_subtitle";
    }

    interface TagsColumns {
        /** Unique string identifying this tag. For example, "TOPIC_ANDROID", "TYPE_CODELAB" */
        String TAG_ID = "tag_id";
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

    interface RoomsColumns {
        /** Unique string identifying this room. */
        String ROOM_ID = "room_id";
        /** Name describing this room. */
        String ROOM_NAME = "room_name";
        /** Building floor this room exists on. */
        String ROOM_FLOOR = "room_floor";
    }

    interface MyScheduleColumns {
        String SESSION_ID = SessionsColumns.SESSION_ID;
        /** Account name for which the session is starred (in my schedule) */
        String MY_SCHEDULE_ACCOUNT_NAME = "account_name";
        /** Indicate if last operation was "add" (true) or "remove" (false). Since uniqueness is
         * given by seesion_id+account_name, this field can be used as a way to find removals and
         * sync them with the cloud */
        String MY_SCHEDULE_IN_SCHEDULE = "in_schedule";
        /** Flag to indicate if the corresponding in_my_schedule item needs to be synced */
        String MY_SCHEDULE_DIRTY_FLAG = "dirty";
    }

    interface SessionsColumns {
        /** Unique string identifying this session. */
        String SESSION_ID = "session_id";
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
        String SESSION_LIVESTREAM_URL = "session_livestream_url";
        /** The Moderator URL. */
        String SESSION_MODERATOR_URL = "session_moderator_url";
        /** The set of tags the session has. This is a comma-separated list of tags.*/
        String SESSION_TAGS = "session_tags";
        /** The names of the speakers on this session, formatted for display. */
        String SESSION_SPEAKER_NAMES = "session_speaker_names";
        /** The order (for sorting) of this session's type. */
        String SESSION_GROUPING_ORDER = "session_grouping_order";
        /** The hashcode of the data used to create this record. */
        String SESSION_IMPORT_HASHCODE = "session_import_hashcode";
        /** The session's main tag. */
        String SESSION_MAIN_TAG = "session_main_tag";
        /** The session's branding color. */
        String SESSION_COLOR = "session_color";
        /** The session's captions URL (for livestreamed sessions). */
        String SESSION_CAPTIONS_URL = "session_captions_url";
        /** The session interval when using the interval counter query. */
        String SESSION_INTERVAL_COUNT= "session_interval_count";
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
        /** The hashcode of the data used to create this record. */
        String SPEAKER_IMPORT_HASHCODE = "speaker_import_hashcode";
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
        /** Floor **/
        String TILE_FLOOR = "map_tile_floor";
        /** Filename **/
        String TILE_FILE = "map_tile_file";
        /** Url **/
        String TILE_URL = "map_tile_url";
    }

    interface ExpertsColumns {
        /** Unique string identifying this expert. */
        String EXPERT_ID = "expert_id";
        /** Name of this expert. */
        String EXPERT_NAME = "expert_name";
        /** Profile photo of this expert. */
        String EXPERT_IMAGE_URL = "expert_image_url";
        /** Title of this expert. */
        String EXPERT_TITLE = "expert_title";
        /** Body of text describing this expert in detail. */
        String EXPERT_ABSTRACT = "expert_abstract";
        /** Full URL to the expert's profile. */
        String EXPERT_URL = "expert_url";
        /** Country code of this expert. */
        String EXPERT_COUNTRY = "expert_country";
        /** City of this expert. */
        String EXPERT_CITY = "expert_city";
        /** Whether the expert is attending the I/O this year. */
        String EXPERT_ATTENDING = "expert_attending";
        /** The hashcode of the data used to create this record. */
        String EXPERT_IMPORT_HASHCODE = "expert_import_hashcode";
    }

    interface PartnersColumns {
        /** Unique string identifying this partner. */
        String PARTNER_ID = "partner_id";
        /** Name of this partner. */
        String PARTNER_NAME = "partner_name";
        /** Description of this partner. */
        String PARTNER_DESC = "partner_desc";
        /** Website URL for this partner. */
        String PARTNER_WEBSITE_URL = "partner_website_url";
        /** Logo URL for this partner. */
        String PARTNER_LOGO_URL = "partner_logo_url";
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

    interface PeopleIveMetColumns {
        /** Google+ ID of the person. */
        String PERSON_ID = "person_id";
        /** Time when the badge of this person was scanned. */
        String PERSON_TIMESTAMP = "person_timestamp";
        /** Name of the person. */
        String PERSON_NAME = "person_name";
        /** URL of profile icon of this person. */
        String PERSON_IMAGE_URL = "person_image_url";
        /** Note about this person. */
        String PERSON_NOTE = "person_note";
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

    public static final String CONTENT_AUTHORITY = "com.google.samples.apps.iosched";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    private static final String PATH_BLOCKS = "blocks";
    private static final String PATH_AFTER = "after";
    private static final String PATH_TAGS = "tags";
    private static final String PATH_ROOM = "room";
    private static final String PATH_UNSCHEDULED = "unscheduled";
    private static final String PATH_ROOMS = "rooms";
    private static final String PATH_SESSIONS = "sessions";
    private static final String PATH_FEEDBACK = "feedback";
    private static final String PATH_MY_SCHEDULE = "my_schedule";
    private static final String PATH_SESSIONS_COUNTER = "counter";
    private static final String PATH_SPEAKERS = "speakers";
    private static final String PATH_ANNOUNCEMENTS = "announcements";
    private static final String PATH_MAP_MARKERS = "mapmarkers";
    private static final String PATH_MAP_FLOOR = "floor";
    private static final String PATH_MAP_TILES= "maptiles";
    private static final String PATH_HASHTAGS = "hashtags";
    private static final String PATH_VIDEOS = "videos";
    private static final String PATH_SEARCH = "search";
    private static final String PATH_SEARCH_SUGGEST = "search_suggest_query";
    private static final String PATH_SEARCH_INDEX = "search_index";
    private static final String PATH_EXPERTS = "experts";
    private static final String PATH_PARTNERS = "partners";
    private static final String PATH_PEOPLE_IVE_MET = "people_ive_met";

    public static final String[] TOP_LEVEL_PATHS = {
            PATH_BLOCKS,
            PATH_TAGS,
            PATH_ROOMS,
            PATH_SESSIONS,
            PATH_FEEDBACK,
            PATH_MY_SCHEDULE,
            PATH_SPEAKERS,
            PATH_ANNOUNCEMENTS,
            PATH_MAP_MARKERS,
            PATH_MAP_FLOOR,
            PATH_MAP_MARKERS,
            PATH_MAP_TILES,
            PATH_HASHTAGS,
            PATH_VIDEOS,
            PATH_EXPERTS,
            PATH_PARTNERS,
            PATH_PEOPLE_IVE_MET
    };

    public static final String[] USER_DATA_RELATED_PATHS = {
            PATH_SESSIONS,
            PATH_MY_SCHEDULE
    };

    /**
     * Blocks are generic timeslots.
     */
    public static class Blocks implements BlocksColumns, BaseColumns {
        public static final String BLOCK_TYPE_FREE = "free";
        public static final String BLOCK_TYPE_BREAK = "break";
        public static final String BLOCK_TYPE_KEYNOTE = "keynote";

        public static final boolean isValidBlockType(String type) {
            return BLOCK_TYPE_FREE.equals(type) ||  BLOCK_TYPE_BREAK.equals(type)
                    || BLOCK_TYPE_KEYNOTE.equals(type);
        }

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_BLOCKS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.iosched2014.block";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.iosched2014.block";

        /** "ORDER BY" clauses. */
        public static final String DEFAULT_SORT = BlocksColumns.BLOCK_START + " ASC, "
                + BlocksColumns.BLOCK_END + " ASC";

        /** Build {@link Uri} for requested {@link #BLOCK_ID}. */
        public static Uri buildBlockUri(String blockId) {
            return CONTENT_URI.buildUpon().appendPath(blockId).build();
        }

        /** Read {@link #BLOCK_ID} from {@link Blocks} {@link Uri}. */
        public static String getBlockId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        /**
         * Generate a {@link #BLOCK_ID} that will always match the requested
         * {@link Blocks} details.
         * @param startTime the block start time, in milliseconds since Epoch UTC
         * @param endTime the block end time, in milliseconds since Epoch UTF
         */
        public static String generateBlockId(long startTime, long endTime) {
            startTime /= DateUtils.SECOND_IN_MILLIS;
            endTime /= DateUtils.SECOND_IN_MILLIS;
            return ParserUtils.sanitizeId(startTime + "-" + endTime);
        }
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

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.iosched2014.tag";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.iosched2014.tag";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = TagsColumns.TAG_ORDER_IN_CATEGORY;

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

        /** Read {@link #TAG_ID} from {@link Tags} {@link Uri}. */
        public static String getTagId(Uri uri) {
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

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.iosched2014.myschedule";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.iosched2014.myschedule";

        /**
         * Build {@link Uri} that references all My Schedule for the current user.
         */
        public static Uri buildMyScheduleUri(Context context) {
            return buildMyScheduleUri(context, null);
        }
        public static Uri buildMyScheduleUri(Context context, String accountName) {
            if (accountName == null) {
                accountName = AccountUtils.getActiveAccountName(context);
            }
            return addOverrideAccountName(CONTENT_URI, accountName);
        }

    }

    /**
     * Rooms are physical locations at the conference venue.
     */
    public static class Rooms implements RoomsColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ROOMS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.iosched2014.room";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.iosched2014.room";

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

    public static class Feedback implements BaseColumns, FeedbackColumns, SyncColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_FEEDBACK).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.iosched2014.session_feedback";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.iosched2014.session_feedback";

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

    /**
     * Each session has zero or more {@link Tags}, a {@link Rooms},
     * zero or more {@link Speakers}.
     */
    public static class Sessions implements SessionsColumns, RoomsColumns,
            SyncColumns, BaseColumns {
        public static final String QUERY_PARAMETER_TAG_FILTER = "filter";

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SESSIONS).build();
        public static final Uri CONTENT_MY_SCHEDULE_URI =
                CONTENT_URI.buildUpon().appendPath(PATH_MY_SCHEDULE).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.iosched2014.session";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.iosched2014.session";

        public static final String ROOM_ID = "room_id";

        public static final String SEARCH_SNIPPET = "search_snippet";

        public static final String HAS_GIVEN_FEEDBACK = "has_given_feedback";

        // ORDER BY clauses
        public static final String SORT_BY_TYPE_THEN_TIME = SESSION_GROUPING_ORDER + " ASC,"
                + SESSION_START + " ASC," + SESSION_TITLE + " COLLATE NOCASE ASC";
        public static final String SORT_BY_TIME = SESSION_START + " ASC,"
                + SESSION_TITLE + " COLLATE NOCASE ASC";

        public static final String LIVESTREAM_SELECTION =
                SESSION_LIVESTREAM_URL + " is not null AND " + SESSION_LIVESTREAM_URL + "!=''";

        // Used to fetch sessions starting within a specific time interval
        public static final String STARTING_AT_TIME_INTERVAL_SELECTION =
                SESSION_START + " >= ? and " + SESSION_START + " <= ?";

        // Used to fetch sessions for a particular time
        public static final String AT_TIME_SELECTION =
                SESSION_START + " <= ? and " + SESSION_END + " >= ?";

        // Builds selectionArgs for {@link STARTING_AT_TIME_INTERVAL_SELECTION}
        public static String[] buildAtTimeIntervalArgs(long intervalStart, long intervalEnd) {
            return new String[] { String.valueOf(intervalStart), String.valueOf(intervalEnd) };
        }

        // Builds selectionArgs for {@link AT_TIME_SELECTION}
        public static String[] buildAtTimeSelectionArgs(long time) {
            final String timeString = String.valueOf(time);
            return new String[] { timeString, timeString };
        }

        // Used to fetch upcoming sessions
        public static final String UPCOMING_LIVE_SELECTION = SESSION_START + " > ?";

        // Builds selectionArgs for {@link UPCOMING_LIVE_SELECTION}
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

        /** Build {@link Uri} that references sessions in a room that have begun after the requested time **/
        public static Uri buildSessionsInRoomAfterUri(String room, long time) {
            return CONTENT_URI.buildUpon().appendPath(PATH_ROOM).appendPath(room).appendPath(PATH_AFTER)
                    .appendPath(String.valueOf(time)).build();
        }

        /** Build {@link Uri} that references sessions not in user's schedule that happen in the specified interval **/
        public static Uri buildUnscheduledSessionsInInterval(long start, long end) {
            String interval = start+"-"+end;
            return CONTENT_URI.buildUpon().appendPath(PATH_UNSCHEDULED).appendPath(interval).build();
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
            if (segments.size() == 3 && segments.get(2).indexOf('-') > 0 ) {
                String[] interval = segments.get(2).split("-");
                return new long[]{Long.parseLong(interval[0]), Long.parseLong(interval[1])};
            }
            return null;
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
            List<String> segments = uri.getPathSegments();
            if (2 < segments.size()) {
                return segments.get(2);
            }
            return null;
        }

        public static boolean hasFilterParam(Uri uri) {
            return uri != null && uri.getQueryParameter(QUERY_PARAMETER_TAG_FILTER) != null;
        }

        /** Build {@link Uri} that references all sessions that have ALL of the indicated tags. */
        public static Uri buildTagFilterUri(String[] requiredTags) {
            StringBuilder sb = new StringBuilder();
            for (String tag : requiredTags) {
                if (TextUtils.isEmpty(tag)) continue;
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(tag.trim());
            }
            if (sb.length() == 0) {
                // equivalent to "all sessions"
                return CONTENT_URI;
            } else {
                // filter by the given set of tags
                return CONTENT_URI.buildUpon().appendQueryParameter(QUERY_PARAMETER_TAG_FILTER,
                        sb.toString()).build();
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

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.iosched2014.speaker";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.iosched2014.speaker";

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
     * Announcements of breaking news
     */
    public static class Announcements implements AnnouncementsColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ANNOUNCEMENTS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.iosched2014.announcement";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.iosched2014.announcement";

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

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.iosched2014.maptiles";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.iosched2014.maptiles";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = MapTileColumns.TILE_FLOOR + " ASC";


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

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.iosched2014.mapmarker";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.iosched2014.mapmarker";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = MapMarkerColumns.MARKER_FLOOR
                + " ASC, " + MapMarkerColumns.MARKER_ID + " ASC";

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

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.iosched2014.hashtags";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.iosched2014.hashtags";

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

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.iosched2014.videos";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.iosched2014.videos";

        public static final String DEFAULT_SORT = VideoColumns.VIDEO_YEAR + " DESC, "
                + VideoColumns.VIDEO_TOPIC + " ASC, " + VideoColumns.VIDEO_TITLE + " ASC";

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

    /**
     * Experts are individual people. Independent from the other data models.
     */
    public static class Experts implements ExpertsColumns, SyncColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_EXPERTS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.iosched2014.expert";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.iosched2014.expert";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = ExpertsColumns.EXPERT_NAME
                + " COLLATE NOCASE ASC";

        /** Build {@link Uri} for requested {@link #EXPERT_ID}. */
        public static Uri buildExpertUri(String expertId) {
            return CONTENT_URI.buildUpon().appendPath(expertId).build();
        }

        public static String getExpertId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    /**
     * Partners are companies presenting at IO. This model is independent from other data models.
     */
    public static class Partners implements PartnersColumns, SyncColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_PARTNERS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.iosched2014.partner";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.iosched2014.partner";

        /** Build {@link Uri} for requested {@link #PARTNER_ID}. */
        public static Uri buildPartnerUri(String partnerId) {
            return CONTENT_URI.buildUpon().appendPath(partnerId).build();
        }

        public static String getPartnerId(Uri uri) { return uri.getPathSegments().get(1); }
    }

    /**
     * Each record of PeopleIveMet is collected by scanning their badges. This model is independent
     * from other models.
     */
    public static class PeopleIveMet implements PeopleIveMetColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_PEOPLE_IVE_MET).build();

        public static final String DEFAULT_SORT = PeopleIveMetColumns.PERSON_TIMESTAMP + " DESC";

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.iosched2014.people_ive_met";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.iosched2014.people_ive_met";

        public static Uri buildPersonUri(String personId) {
            return CONTENT_URI.buildUpon().appendPath(personId).build();
        }

        public static String getPersonId(Uri uri) {
            return uri.getPathSegments().get(1);
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
     * Adds an account override parameter to the URI.
     * The override parameter instructs the Content Provider to ignore the currently logged in
     * account and use the provided account when fetching account-specific data
     * (such as sessions in My Schedule).
     *
     */
    public static Uri addOverrideAccountName(Uri uri, String accountName) {
        return uri.buildUpon().appendQueryParameter(
                OVERRIDE_ACCOUNTNAME_PARAMETER, accountName).build();
    }

    public static String getOverrideAccountName(Uri uri) {
        return uri.getQueryParameter(OVERRIDE_ACCOUNTNAME_PARAMETER);
    }

    private ScheduleContract() {
    }
}
