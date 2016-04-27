package com.google.samples.apps.iosched.provider;

/**
 * The list of {@code Uri}s recognised by the {@code ContentProvider} of the app.
 * <p/>
 * It is important to order them in the order that follows {@link android.content.UriMatcher}
 * matching rules: wildcard {@code *} applies to one segment only and it processes matching per
 * segment in a tree manner over the list of {@code Uri} in the order they are added. The first
 * rule means that {@code sessions / *} would not match {@code sessions / id / room}. The second
 * rule is more subtle and means that if Uris are in the  order {@code * / room / counter} and
 * {@code sessions / room / time}, then {@code speaker / room / time} will not match anything,
 * because the {@code UriMatcher} will follow the path of the first  and will fail at the third
 * segment.
 */
public enum ScheduleUriEnum {
    BLOCKS(100, "blocks", ScheduleContract.Blocks.CONTENT_TYPE_ID, false, ScheduleDatabase.Tables.BLOCKS),
    BLOCKS_BETWEEN(101, "blocks/between/*/*", ScheduleContract.Blocks.CONTENT_TYPE_ID, false, null),
    BLOCKS_ID(102, "blocks/*", ScheduleContract.Blocks.CONTENT_TYPE_ID, true, null),
    BLOCKS_ID_SESSIONS(103, "blocks/*/sessions", ScheduleContract.Blocks.CONTENT_TYPE_ID, true, null),
    BLOCKS_ID_SESSIONS_STARRED(104, "blocks/*/sessions/starred", ScheduleContract.Blocks.CONTENT_TYPE_ID, true, null),
    TAGS(200, "tags", ScheduleContract.Tags.CONTENT_TYPE_ID, false, ScheduleDatabase.Tables.TAGS),
    TAGS_ID(201, "tags/*", ScheduleContract.Tags.CONTENT_TYPE_ID, false, null),
    TRACKS(300, "tracks", ScheduleContract.Tracks.CONTENT_TYPE_ID, false, ScheduleDatabase.Tables.TRACKS),
    TRACKS_ID(301, "tracks/*", ScheduleContract.Tracks.CONTENT_TYPE_ID, true, null),
    TRACKS_ID_SESSIONS(302, "tracks/*/sessions", ScheduleContract.Tracks.CONTENT_TYPE_ID, false, null),
    TRACKS_ID_VENDORS(303, "tracks/*/vendors", ScheduleContract.Tracks.CONTENT_TYPE_ID, false, null),

    ROOMS(400, "rooms", ScheduleContract.Rooms.CONTENT_TYPE_ID, false, ScheduleDatabase.Tables.ROOMS),
    ROOMS_ID(401, "rooms/*", ScheduleContract.Rooms.CONTENT_TYPE_ID, true, null),
    ROOMS_ID_SESSIONS(402, "rooms/*/sessions", ScheduleContract.Sessions.CONTENT_TYPE_ID, false, null),

    SESSIONS(500, "sessions", ScheduleContract.Sessions.CONTENT_TYPE_ID, false, ScheduleDatabase.Tables.SESSIONS),
    SESSIONS_MY_SCHEDULE(501, "sessions/my_schedule", ScheduleContract.Sessions.CONTENT_TYPE_ID, false, null),
    SESSIONS_WITH_TRACK(502, "sessions/with_track", ScheduleContract.Sessions.CONTENT_TYPE_ID, false,
            ScheduleDatabase.Tables.SESSIONS_JOIN_TRACKS_JOIN_BLOCKS),
    SESSIONS_SEARCH(503, "sessions/search/*", ScheduleContract.Sessions.CONTENT_TYPE_ID, false, null),
    SESSIONS_AT(504, "sessions/at/*", ScheduleContract.Sessions.CONTENT_TYPE_ID, false, null),
    SESSIONS_AFTER(511, "sessions/after/*", ScheduleContract.Sessions.CONTENT_TYPE_ID, false, null),
    SESSIONS_ROOM_AFTER(508, "sessions/room/*/after/*", ScheduleContract.Sessions.CONTENT_TYPE_ID, false, null),
    SESSIONS_UNSCHEDULED(509, "sessions/unscheduled/*", ScheduleContract.Sessions.CONTENT_TYPE_ID, false, null),
    SESSIONS_COUNTER(510, "sessions/counter", null, true, null),
    SESSIONS_ID(505, "sessions/*", ScheduleContract.Sessions.CONTENT_TYPE_ID, true, null),
    SESSIONS_ID_SPEAKERS(506, "sessions/*/speakers", ScheduleContract.Speakers.CONTENT_TYPE_ID, false,
            ScheduleDatabase.Tables.SESSIONS_SPEAKERS),
    SESSIONS_ID_TAGS(507, "sessions/*/tags", ScheduleContract.Tags.CONTENT_TYPE_ID, false, ScheduleDatabase.Tables.SESSIONS_TAGS),
    SESSIONS_ID_TRACKS(508, "sessions/*/tracks", ScheduleContract.Tracks.CONTENT_TYPE_ID, false, ScheduleDatabase.Tables.SESSIONS_TRACKS),
    SESSIONS_ID_WITH_TRACK(509, "sessions/*/with_track", ScheduleContract.Tracks.CONTENT_TYPE_ID, false, ScheduleDatabase.Tables.SESSIONS_JOIN_TRACKS_JOIN_BLOCKS),
    SESSIONS_STARRED(510, "sessions/starred", ScheduleContract.Tracks.CONTENT_TYPE_ID, false, ScheduleDatabase.Tables.SESSIONS_JOIN_BLOCKS_ROOMS),

    SPEAKERS(600, "speakers", ScheduleContract.Speakers.CONTENT_TYPE_ID, false, ScheduleDatabase.Tables.SPEAKERS),
    SPEAKERS_ID(601, "speakers/*", ScheduleContract.Speakers.CONTENT_TYPE_ID, true, null),
    SPEAKERS_ID_SESSIONS(602, "speakers/*/sessions", ScheduleContract.Sessions.CONTENT_TYPE_ID, false, null),

    VENDORS(700, "vendors", ScheduleContract.Vendors.CONTENT_TYPE_ID, false, ScheduleDatabase.Tables.VENDORS),
    VENDORS_STARRED(701, "vendors/starred", ScheduleContract.Vendors.CONTENT_TYPE_ID, false, ScheduleDatabase.Tables.VENDORS_JOIN_TRACKS),
    VENDORS_SEARCH(702, "vendors/search/*", ScheduleContract.Vendors.CONTENT_TYPE_ID, false, null),
    VENDORS_ID(703, "vendors/*", ScheduleContract.Vendors.CONTENT_TYPE_ID, true, null),

    MY_SCHEDULE(800, "my_schedule", ScheduleContract.MySchedule.CONTENT_TYPE_ID, false, null),
    MY_VIEWED_VIDEOS(801, "my_viewed_videos", ScheduleContract.MyViewedVideos.CONTENT_TYPE_ID, false, null),
    MY_FEEDBACK_SUBMITTED(802, "my_feedback_submitted", ScheduleContract.MyFeedbackSubmitted.CONTENT_TYPE_ID, false, null),

    ANNOUNCEMENTS(900, "announcements", ScheduleContract.Announcements.CONTENT_TYPE_ID, false,
            ScheduleDatabase.Tables.ANNOUNCEMENTS),
    ANNOUNCEMENTS_ID(901, "announcements/*", ScheduleContract.Announcements.CONTENT_TYPE_ID, true, null),
    SEARCH_SUGGEST(1000, "search_suggest_query", null, false, ScheduleDatabase.Tables.SEARCH_SUGGEST),
    SEARCH_INDEX(1001, "search_index", null, false, null),// update only
    MAPMARKERS(1100, "mapmarkers", ScheduleContract.MapMarkers.CONTENT_TYPE_ID, false, ScheduleDatabase.Tables.MAPMARKERS),
    MAPMARKERS_FLOOR(1101, "mapmarkers/floor/*", ScheduleContract.MapMarkers.CONTENT_TYPE_ID, false, null),
    MAPMARKERS_ID(1102, "mapmarkers/*", ScheduleContract.MapMarkers.CONTENT_TYPE_ID, true, null),
    MAPTILES(1200, "maptiles", ScheduleContract.MapTiles.CONTENT_TYPE_ID, false, ScheduleDatabase.Tables.MAPTILES),
    MAPTILES_FLOOR(1201, "maptiles/*", ScheduleContract.MapTiles.CONTENT_TYPE_ID, true, null),
    FEEDBACK_ALL(1202, "feedback", ScheduleContract.Feedback.CONTENT_TYPE_ID, false, null),
    FEEDBACK_FOR_SESSION(1203, "feedback/*", ScheduleContract.Feedback.CONTENT_TYPE_ID, true, ScheduleDatabase.Tables.FEEDBACK),
    HASHTAGS(1300, "hashtags", ScheduleContract.Hashtags.CONTENT_TYPE_ID, false, ScheduleDatabase.Tables.HASHTAGS),
    HASHTAGS_NAME(1301, "hashtags/*", ScheduleContract.Hashtags.CONTENT_TYPE_ID, true, null),

    VIDEOS(1400, "videos", ScheduleContract.Videos.CONTENT_TYPE_ID, false, ScheduleDatabase.Tables.VIDEOS),
    VIDEOS_ID(1401, "videos/*", ScheduleContract.Videos.CONTENT_TYPE_ID, true, null),
    SEARCH_TOPICS_SESSIONS(1500, "search_topics_sessions",
            ScheduleContract.SearchTopicsSessions.CONTENT_TYPE_ID, false, null /*virtual table*/);
    public int code;

    /**
     * The path to the {@link android.content.UriMatcher} will use to match. * may be used as a
     * wild card for any text, and # may be used as a wild card for numbers.
     */
    public String path;

    public String contentType;

    public String table;

    ScheduleUriEnum(int code, String path, String contentTypeId, boolean item, String table) {
        this.code = code;
        this.path = path;
        this.contentType = item ? ScheduleContract.makeContentItemType(contentTypeId)
                : ScheduleContract.makeContentType(contentTypeId);
        this.table = table;
    }


}
