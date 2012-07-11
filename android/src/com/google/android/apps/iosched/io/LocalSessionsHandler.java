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
import com.google.android.apps.iosched.provider.ScheduleContract.Rooms;
import com.google.android.apps.iosched.provider.ScheduleContract.Sessions;
import com.google.android.apps.iosched.provider.ScheduleContract.Tracks;
import com.google.android.apps.iosched.provider.ScheduleDatabase.SessionsTracks;
import com.google.android.apps.iosched.util.Lists;
import com.google.android.apps.iosched.util.ParserUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

public class LocalSessionsHandler extends XmlHandler {

    public LocalSessionsHandler() {
        super(ScheduleContract.CONTENT_AUTHORITY);
    }

    @Override
    public ArrayList<ContentProviderOperation> parse(XmlPullParser parser, ContentResolver resolver)
            throws XmlPullParserException, IOException {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

        int type;
        while ((type = parser.next()) != END_DOCUMENT) {
            if (type == START_TAG && Tags.SESSION.equals(parser.getName())) {
                parseSession(parser, batch, resolver);
            }
        }

        return batch;
    }

    private static void parseSession(XmlPullParser parser,
            ArrayList<ContentProviderOperation> batch, ContentResolver resolver)
            throws XmlPullParserException, IOException {
        final int depth = parser.getDepth();
        ContentProviderOperation.Builder builder = ContentProviderOperation
                .newInsert(Sessions.CONTENT_URI);
        builder.withValue(Sessions.UPDATED, 0);

        long startTime = -1;
        long endTime = -1;
        String title = null;
        String sessionId = null;
        String trackId = null;

        String tag = null;
        int type;
        while (((type = parser.next()) != END_TAG ||
                parser.getDepth() > depth) && type != END_DOCUMENT) {
            if (type == START_TAG) {
                tag = parser.getName();
            } else if (type == END_TAG) {
                tag = null;
            } else if (type == TEXT) {
                final String text = parser.getText();
                if (Tags.START.equals(tag)) {
                    startTime = ParserUtils.parseTime(text);
                } else if (Tags.END.equals(tag)) {
                    endTime = ParserUtils.parseTime(text);
                } else if (Tags.ROOM.equals(tag)) {
                    final String roomId = Rooms.generateRoomId(text);
                    builder.withValue(Sessions.ROOM_ID, roomId);
                } else if (Tags.TRACK.equals(tag)) {
                    trackId = Tracks.generateTrackId(text);
                } else if (Tags.ID.equals(tag)) {
                    sessionId = text;
                } else if (Tags.TITLE.equals(tag)) {
                    title = text;
                } else if (Tags.ABSTRACT.equals(tag)) {
                    builder.withValue(Sessions.SESSION_ABSTRACT, text);
                }
            }
        }

        if (sessionId == null) {
            sessionId = Sessions.generateSessionId(title);
        }

        builder.withValue(Sessions.SESSION_ID, sessionId);
        builder.withValue(Sessions.SESSION_TITLE, title);

        // Use empty strings to make sure SQLite search trigger has valid data
        // for updating search index.
        builder.withValue(Sessions.SESSION_ABSTRACT, "");
        builder.withValue(Sessions.SESSION_REQUIREMENTS, "");
        builder.withValue(Sessions.SESSION_KEYWORDS, "");

        final String blockId = ParserUtils.findBlock(title, startTime, endTime);
        builder.withValue(Sessions.BLOCK_ID, blockId);

        // Propagate any existing starred value
        final Uri sessionUri = Sessions.buildSessionUri(sessionId);
        final int starred = querySessionStarred(sessionUri, resolver);
        if (starred != -1) {
            builder.withValue(Sessions.SESSION_STARRED, starred);
        }

        batch.add(builder.build());

        if (trackId != null) {
            // TODO: support parsing multiple tracks per session
            final Uri sessionTracks = Sessions.buildTracksDirUri(sessionId);
            batch.add(ContentProviderOperation.newInsert(sessionTracks)
                    .withValue(SessionsTracks.SESSION_ID, sessionId)
                    .withValue(SessionsTracks.TRACK_ID, trackId).build());
        }
    }

    public static int querySessionStarred(Uri uri, ContentResolver resolver) {
        final String[] projection = { Sessions.SESSION_STARRED };
        final Cursor cursor = resolver.query(uri, projection, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            } else {
                return -1;
            }
        } finally {
            cursor.close();
        }
    }

    interface Tags {
        String SESSION = "session";
        String ID = "id";
        String START = "start";
        String END = "end";
        String ROOM = "room";
        String TRACK = "track";
        String TITLE = "title";
        String ABSTRACT = "abstract";
    }
}
