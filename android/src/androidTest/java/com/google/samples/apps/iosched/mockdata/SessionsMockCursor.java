package com.google.samples.apps.iosched.mockdata;

import android.database.Cursor;
import android.database.MatrixCursor;

/**
 * This has methods to create stub cursors for sessions. To generate different mock cursors, refer
 * to {@link com.google.samples.apps.iosched.debug
 * .OutputMockData#generateMatrixCursorCodeForCurrentRow(Cursor)}.
 */
public class SessionsMockCursor {

    public static final String FAKE_TITLE = "FAKE TITLE";

    public static final String FAKE_TITLE_KEYNOTE = "Keynote";

    public static final String EMPTY = "";

    private static final String TRUE = "1";

    private static final String FALSE = "0";

    public static final String FAKE_TAG = TagMetadataMockCursor.TAG_NAME;

    public static final String FAKE_LIVESTREAM_ID = "123456";

    public static final String FAKE_YOUTUBE_URL = "1gaewgtatwte456";

    public static final long START_SESSION = 1432936800000L;

    public static final long END_SESSION = 1432938600000L;

    public static final String FAKE_ROOM_ID = "fewsgtewtw";

    public static Cursor getCursorForKeynoteSession() {
        return getCursorForSession(true, false, true);
    }

    public static Cursor getCursorForSessionInSchedule() {
        return getCursorForSession(false, true, false);
    }

    public static Cursor getCursorForSessionNotInSchedule() {
        return getCursorForSession(false, false, false);
    }

    public static Cursor getCursorForSessionWithLiveStream() {
        return getCursorForSession(false, false, true);
    }

    public static Cursor getCursorForSessionWithoutLiveStream() {
        return getCursorForSession(false, false, false);
    }

    public static Cursor getCursorForSessionFeedback() {
        String[] data = {FAKE_TITLE, ""};
        String[] columns = {"session_title", "session_speaker_names"};
        MatrixCursor matrixCursor = new MatrixCursor(columns);
        matrixCursor.addRow(data);
        return matrixCursor;
    }

    private static Cursor getCursorForSession(boolean keynote, boolean inSchedule,
            boolean hasLiveStream) {
        String[] data = {"" + START_SESSION, "" + END_SESSION, "null",
                keynote ? FAKE_TITLE_KEYNOTE : FAKE_TITLE,
                "Learn how to get your video content and reviews surfaced in Google Search on" +
                        " mobile. This talk will cover website annotation and app deep linking." +
                        " (Note: This Sandbox talk will be offered twice throughout the event." +
                        " Check the schedule to confirm timings.)", "null",
                inSchedule ? TRUE : FALSE,
                keynote ? "keynote" : "cloud",
                "https://events.google.com/io2015/schedule#/b7e49d26-86e4-e411-b87f-00155d5066d7",
                hasLiveStream ? FAKE_YOUTUBE_URL : EMPTY, "null", "null",
                hasLiveStream ? FAKE_LIVESTREAM_ID : EMPTY, "null", FAKE_ROOM_ID,
                "Earn & Engage Talk (L2)", "-14235942",
                "https://storage.googleapis.com/io2015-data.appspot.com/images/sessions/"
                        + "__w-200-400-600-800-1000__/b7e49d26-86e4-e411-b87f-00155d5066d7.jpg",
                "null",
                keynote ? "FLAG_KEYNOTE" :
                        FAKE_TAG + ",TRACK_CLOUD,TRACK_ANDROID," +
                                "TRACK_SEARCH",
                keynote ? EMPTY : SpeakersMockCursor.FAKE_SPEAKER, FAKE_TAG};
        String[] columns = {"session_start", "session_end", "session_level", "session_title",
                "session_abstract", "session_requirements", "session_in_my_schedule",
                "session_hashtag", "session_url", "session_youtube_url", "session_pdf_url",
                "session_notes_url", "session_livestream_url", "session_moderator_url",
                "room_id", "room_name", "session_color", "session_photo_url",
                "session_related_content", "session_tags", "session_speaker_names", "session_main_tag"};
        MatrixCursor matrixCursor = new MatrixCursor(columns);
        matrixCursor.addRow(data);
        return matrixCursor;
    }
}
