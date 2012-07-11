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

package com.google.android.apps.iosched.io;

import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.provider.ScheduleContract.Sessions;
import com.google.android.apps.iosched.provider.ScheduleContract.SyncColumns;
import com.google.android.apps.iosched.provider.ScheduleDatabase.SessionsSpeakers;
import com.google.android.apps.iosched.provider.ScheduleDatabase.SessionsTracks;
import com.google.android.apps.iosched.util.Lists;
import com.google.android.apps.iosched.util.ParserUtils;
import com.google.android.apps.iosched.util.SpreadsheetEntry;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.Time;
import android.util.Log;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import static com.google.android.apps.iosched.util.ParserUtils.AtomTags.ENTRY;
import static com.google.android.apps.iosched.util.ParserUtils.sanitizeId;
import static com.google.android.apps.iosched.util.ParserUtils.splitComma;
import static com.google.android.apps.iosched.util.ParserUtils.translateTrackIdAlias;
import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

/**
 * Handle a remote {@link XmlPullParser} that defines a set of {@link Sessions}
 * entries. Assumes that the remote source is a Google Spreadsheet.
 */
public class RemoteSessionsHandler extends XmlHandler {
    private static final String TAG = "SessionsHandler";

    /**
     * Custom format used internally that matches expected concatenation of
     * {@link Columns#SESSION_DATE} and {@link Columns#SESSION_TIME}.
     */
    private static final SimpleDateFormat sTimeFormat = new SimpleDateFormat(
            "EEEE MMM d yyyy h:mma Z", Locale.US);

    public RemoteSessionsHandler() {
        super(ScheduleContract.CONTENT_AUTHORITY);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<ContentProviderOperation> parse(XmlPullParser parser, ContentResolver resolver)
            throws XmlPullParserException, IOException {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

        // Walk document, parsing any incoming entries
        int type;
        while ((type = parser.next()) != END_DOCUMENT) {
            if (type == START_TAG && ENTRY.equals(parser.getName())) {
                // Process single spreadsheet row at a time
                final SpreadsheetEntry entry = SpreadsheetEntry.fromParser(parser);

                final String sessionId = sanitizeId(entry.get(Columns.SESSION_TITLE));
                final Uri sessionUri = Sessions.buildSessionUri(sessionId);

                // Check for existing details, only update when changed
                final ContentValues values = querySessionDetails(sessionUri, resolver);
                final long localUpdated = values.getAsLong(SyncColumns.UPDATED);
                final long serverUpdated = entry.getUpdated();
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "found session " + entry.toString());
                    Log.v(TAG, "found localUpdated=" + localUpdated + ", server=" + serverUpdated);
                }
                if (localUpdated >= serverUpdated) continue;

                final Uri sessionTracksUri = Sessions.buildTracksDirUri(sessionId);
                final Uri sessionSpeakersUri = Sessions.buildSpeakersDirUri(sessionId);

                // Clear any existing values for this session, treating the
                // incoming details as authoritative.
                batch.add(ContentProviderOperation.newDelete(sessionUri).build());
                batch.add(ContentProviderOperation.newDelete(sessionTracksUri).build());
                batch.add(ContentProviderOperation.newDelete(sessionSpeakersUri).build());

                final ContentProviderOperation.Builder builder = ContentProviderOperation
                        .newInsert(Sessions.CONTENT_URI);

                builder.withValue(SyncColumns.UPDATED, serverUpdated);
                builder.withValue(Sessions.SESSION_ID, sessionId);
                builder.withValue(Sessions.SESSION_LEVEL, entry.get(Columns.SESSION_LEVEL));
                builder.withValue(Sessions.SESSION_TITLE, entry.get(Columns.SESSION_TITLE));
                builder.withValue(Sessions.SESSION_ABSTRACT, entry.get(Columns.SESSION_ABSTRACT));
                builder.withValue(Sessions.SESSION_REQUIREMENTS, entry.get(Columns.SESSION_REQUIREMENTS));
                builder.withValue(Sessions.SESSION_KEYWORDS, entry.get(Columns.SESSION_TAGS));
                builder.withValue(Sessions.SESSION_HASHTAG, entry.get(Columns.SESSION_HASHTAG));
                builder.withValue(Sessions.SESSION_SLUG, entry.get(Columns.SESSION_SLUG));
                builder.withValue(Sessions.SESSION_URL, entry.get(Columns.SESSION_URL));
                builder.withValue(Sessions.SESSION_MODERATOR_URL, entry.get(Columns.SESSION_MODERATOR_URL));
                builder.withValue(Sessions.SESSION_YOUTUBE_URL, entry.get(Columns.SESSION_YOUTUBE_URL));
                builder.withValue(Sessions.SESSION_PDF_URL, entry.get(Columns.SESSION_PDF_URL));
                builder.withValue(Sessions.SESSION_FEEDBACK_URL, entry.get(Columns.SESSION_FEEDBACK_URL));
                builder.withValue(Sessions.SESSION_NOTES_URL, entry.get(Columns.SESSION_NOTES_URL));

                // Inherit starred value from previous row
                if (values.containsKey(Sessions.SESSION_STARRED)) {
                    builder.withValue(Sessions.SESSION_STARRED,
                            values.getAsInteger(Sessions.SESSION_STARRED));
                }

                // Parse time string from two columns, which is pretty ugly code
                // since it assumes the column format is "Wednesday May 19" and
                // "10:45am-11:45am". Future spreadsheets should use RFC 3339.
                final String date = entry.get(Columns.SESSION_DATE);
                final String time = entry.get(Columns.SESSION_TIME);
                final int timeSplit = time.indexOf("-");
                if (timeSplit == -1) {
                    throw new HandlerException("Expecting " + Columns.SESSION_TIME
                            + " to express span");
                }

                final long startTime = parseTime(date, time.substring(0, timeSplit));
                final long endTime = parseTime(date, time.substring(timeSplit + 1));

                final String blockId = ParserUtils.findOrCreateBlock(
                        ParserUtils.BLOCK_TITLE_BREAKOUT_SESSIONS,
                        ParserUtils.BLOCK_TYPE_SESSION,
                        startTime, endTime, batch, resolver);
                builder.withValue(Sessions.BLOCK_ID, blockId);

                // Assign room
                final String roomId = sanitizeId(entry.get(Columns.SESSION_ROOM));
                builder.withValue(Sessions.ROOM_ID, roomId);

                // Normal session details ready, write to provider
                batch.add(builder.build());

                // Assign tracks
                final String[] tracks = splitComma(entry.get(Columns.SESSION_TRACK));
                for (String track : tracks) {
                    final String trackId = translateTrackIdAlias(sanitizeId(track));
                    batch.add(ContentProviderOperation.newInsert(sessionTracksUri)
                            .withValue(SessionsTracks.SESSION_ID, sessionId)
                            .withValue(SessionsTracks.TRACK_ID, trackId).build());
                }

                // Assign speakers
                final String[] speakers = splitComma(entry.get(Columns.SESSION_SPEAKERS));
                for (String speaker : speakers) {
                    final String speakerId = sanitizeId(speaker, true);
                    batch.add(ContentProviderOperation.newInsert(sessionSpeakersUri)
                            .withValue(SessionsSpeakers.SESSION_ID, sessionId)
                            .withValue(SessionsSpeakers.SPEAKER_ID, speakerId).build());
                }
            }
        }

        return batch;
    }

    /**
     * Parse the given date and time coming from spreadsheet. This is tightly
     * tied to a specific format. Ideally, if the source used use RFC 3339 we
     * could parse quickly using {@link Time#parse3339}.
     * <p>
     * Internally assumes PST time zone and year 2011.
     *
     * @param date String of format "Wednesday May 19", usually read from
     *            {@link Columns#SESSION_DATE}.
     * @param time String of format "10:45am", usually after splitting
     *            {@link Columns#SESSION_TIME}.
     */
    private static long parseTime(String date, String time) throws HandlerException {
        final String composed = String.format("%s 2011 %s -0700", date, time);
        try {
            return sTimeFormat.parse(composed).getTime();
        } catch (java.text.ParseException e) {
            throw new HandlerException("Problem parsing timestamp", e);
        }
    }

    private static ContentValues querySessionDetails(Uri uri, ContentResolver resolver) {
        final ContentValues values = new ContentValues();
        final Cursor cursor = resolver.query(uri, SessionsQuery.PROJECTION, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                values.put(SyncColumns.UPDATED, cursor.getLong(SessionsQuery.UPDATED));
                values.put(Sessions.SESSION_STARRED, cursor.getInt(SessionsQuery.STARRED));
            } else {
                values.put(SyncColumns.UPDATED, ScheduleContract.UPDATED_NEVER);
            }
        } finally {
            cursor.close();
        }
        return values;
    }

    private interface SessionsQuery {
        String[] PROJECTION = {
                SyncColumns.UPDATED,
                Sessions.SESSION_STARRED,
        };

        int UPDATED = 0;
        int STARRED = 1;
    }

    /** Columns coming from remote spreadsheet. */
    private interface Columns {
        String SESSION_DATE = "sessiondate";
        String SESSION_TIME = "sessiontime";
        String SESSION_ROOM = "sessionroom";
        String SESSION_TRACK = "sessiontrack";
        String SESSION_LEVEL = "sessionlevel";
        String SESSION_TITLE = "sessiontitle";
        String SESSION_TAGS = "sessiontags";
        String SESSION_HASHTAG = "sessionhashtag";
        String SESSION_SLUG = "sessionslug";
        String SESSION_SPEAKERS = "sessionspeakers";
        String SESSION_ABSTRACT = "sessionabstract";
        String SESSION_REQUIREMENTS = "sessionrequirements";
        String SESSION_URL = "sessionurl";
        String SESSION_MODERATOR_URL = "sessionmoderatorurl";
        String SESSION_YOUTUBE_URL = "sessionyoutubeurl";
        String SESSION_PDF_URL = "sessionpdfurl";
        String SESSION_FEEDBACK_URL = "sessionfeedbackurl";
        String SESSION_NOTES_URL = "sessionnotesurl";

        // session_date: Wednesday May 19
        // session_time: 10:45am-11:45am
        // session_room: 6
        // session_track: Enterprise, App Engine
        // session_level: 201
        // session_title: Run corporate applications on Google App Engine?  Yes we do.
        // session_slug: run-corporate-applications
        // session_tags: Enterprise, SaaS, PaaS, Hosting, App Engine, Java
        // session_speakers: Ben Fried, John Smith
        // session_abstract: And you can too! Come hear Google's CIO Ben Fried describe...
        // session_requirements: None
        // session_url: http://www.google.com/events/io/2011/foo
        // session_hashtag: #io11android1
        // session_youtube_url
        // session_pdf_url
        // session_feedback_url
        // session_moderator_url
        // session_notes_url
    }
}
