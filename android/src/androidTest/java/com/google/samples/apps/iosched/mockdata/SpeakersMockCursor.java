package com.google.samples.apps.iosched.mockdata;

import android.database.MatrixCursor;

/**
 * This has methods to create stub cursors for speakers. To generate different mock cursors, refer
 * to {@link com.google.samples.apps.iosched.debug
 * .OutputMockData#generateMatrixCursorCodeForCurrentRow(Cursor)}.
 */
public class SpeakersMockCursor {

    public static final String FAKE_SPEAKER = "Satyajeet Salgar";

    public static final String FAKE_SPEAKER_URL = "http://www.google.com/fakespeaker";

    public static MatrixCursor getCursorForSingleSpeaker() {
        String[] data = {FAKE_SPEAKER,
                "https://storage.googleapis.com/io2015-data.appspot.com/images/speakers/__w-200-" +
                        "400-600-800-1000__/45cc3a01-d2d4-e411-b87f-00155d5066d7.jpg", "Google",
                "Satyajeet is a Product Manager on the Search team. He works on trying to " +
                        "understand" +
                        " all of the worlds media through the Knowledge Graph, and building " +
                        "interesting features based on this knowledge in Search, Now and across " +
                        "Google. He's previously held product management and partnerships roles " +
                        "on " +
                        "YouTube, Games, Ads and Payments. ", FAKE_SPEAKER_URL,
                "https://plus.google.com/+SatyajeetSalgar", "http://www.twitter.com/salgar"};
        String[] columns =
                {"speaker_name", "speaker_image_url", "speaker_company", "speaker_abstract",
                        "speaker_url", "plusone_url", "twitter_url"};
        MatrixCursor matrixCursor = new MatrixCursor(columns);
        matrixCursor.addRow(data);
        return matrixCursor;
    }

    public static MatrixCursor getCursorForNoSpeaker() {
        String[] data = {"", "", "", "", "", "", ""};
        String[] columns =
                {"speaker_name", "speaker_image_url", "speaker_company", "speaker_abstract",
                        "speaker_url", "plusone_url", "twitter_url"};
        MatrixCursor matrixCursor = new MatrixCursor(columns);
        matrixCursor.addRow(data);
        return matrixCursor;
    }
}
