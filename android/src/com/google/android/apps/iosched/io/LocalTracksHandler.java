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
import com.google.android.apps.iosched.provider.ScheduleContract.Tracks;
import com.google.android.apps.iosched.util.Lists;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.graphics.Color;

import java.io.IOException;
import java.util.ArrayList;

import static com.google.android.apps.iosched.util.ParserUtils.sanitizeId;
import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

public class LocalTracksHandler extends XmlHandler {

    public LocalTracksHandler() {
        super(ScheduleContract.CONTENT_AUTHORITY);
    }

    @Override
    public ArrayList<ContentProviderOperation> parse(XmlPullParser parser, ContentResolver resolver)
            throws XmlPullParserException, IOException {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
        batch.add(ContentProviderOperation.newDelete(Tracks.CONTENT_URI).build());

        int type;
        while ((type = parser.next()) != END_DOCUMENT) {
            if (type == START_TAG && Tags.TRACK.equals(parser.getName())) {
                batch.add(parseTrack(parser));
            }
        }

        return batch;
    }

    private static ContentProviderOperation parseTrack(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        final int depth = parser.getDepth();
        final ContentProviderOperation.Builder builder = ContentProviderOperation
                .newInsert(Tracks.CONTENT_URI);

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
                if (Tags.NAME.equals(tag)) {
                    final String trackId = sanitizeId(text);
                    builder.withValue(Tracks.TRACK_ID, trackId);
                    builder.withValue(Tracks.TRACK_NAME, text);
                } else if (Tags.COLOR.equals(tag)) {
                    final int color = Color.parseColor(text);
                    builder.withValue(Tracks.TRACK_COLOR, color);
                } else if (Tags.ABSTRACT.equals(tag)) {
                    builder.withValue(Tracks.TRACK_ABSTRACT, text);
                }
            }
        }

        return builder.build();
    }

    interface Tags {
        String TRACK = "track";
        String NAME = "name";
        String COLOR = "color";
        String ABSTRACT = "abstract";
    }
}
