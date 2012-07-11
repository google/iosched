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
import com.google.android.apps.iosched.provider.ScheduleContract.Speakers;
import com.google.android.apps.iosched.provider.ScheduleContract.SyncColumns;
import com.google.android.apps.iosched.util.Lists;
import com.google.android.apps.iosched.util.SpreadsheetEntry;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

import static com.google.android.apps.iosched.util.ParserUtils.AtomTags.ENTRY;
import static com.google.android.apps.iosched.util.ParserUtils.queryItemUpdated;
import static com.google.android.apps.iosched.util.ParserUtils.sanitizeId;
import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

/**
 * Handle a remote {@link XmlPullParser} that defines a set of {@link Speakers}
 * entries. Assumes that the remote source is a Google Spreadsheet.
 */
public class RemoteSpeakersHandler extends XmlHandler {
    private static final String TAG = "SpeakersHandler";

    public RemoteSpeakersHandler() {
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

                final String speakerId = sanitizeId(entry.get(Columns.SPEAKER_TITLE), true);
                final Uri speakerUri = Speakers.buildSpeakerUri(speakerId);

                // Check for existing details, only update when changed
                final long localUpdated = queryItemUpdated(speakerUri, resolver);
                final long serverUpdated = entry.getUpdated();
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "found speaker " + entry.toString());
                    Log.v(TAG, "found localUpdated=" + localUpdated + ", server=" + serverUpdated);
                }
                if (localUpdated >= serverUpdated) continue;

                // Clear any existing values for this speaker, treating the
                // incoming details as authoritative.
                batch.add(ContentProviderOperation.newDelete(speakerUri).build());

                final ContentProviderOperation.Builder builder = ContentProviderOperation
                        .newInsert(Speakers.CONTENT_URI);

                builder.withValue(SyncColumns.UPDATED, serverUpdated);
                builder.withValue(Speakers.SPEAKER_ID, speakerId);
                builder.withValue(Speakers.SPEAKER_NAME, entry.get(Columns.SPEAKER_TITLE));
                builder.withValue(Speakers.SPEAKER_IMAGE_URL, entry.get(Columns.SPEAKER_IMAGE_URL));
                builder.withValue(Speakers.SPEAKER_COMPANY, entry.get(Columns.SPEAKER_COMPANY));
                builder.withValue(Speakers.SPEAKER_ABSTRACT, entry.get(Columns.SPEAKER_ABSTRACT));
                builder.withValue(Speakers.SPEAKER_URL, entry.get(Columns.SPEAKER_URL));

                // Normal speaker details ready, write to provider
                batch.add(builder.build());
            }
        }

        return batch;
    }

    /** Columns coming from remote spreadsheet. */
    private interface Columns {
        String SPEAKER_TITLE = "speakertitle";
        String SPEAKER_IMAGE_URL = "speakerimageurl";
        String SPEAKER_COMPANY = "speakercompany";
        String SPEAKER_ABSTRACT = "speakerabstract";
        String SPEAKER_URL = "speakerurl";

        // speaker_title: Aaron Koblin
        // speaker_image_url: http://path/to/image.png
        // speaker_company: Google
        // speaker_abstract: Aaron takes social and infrastructural data and uses...
        // speaker_url: http://profiles.google.com/...

    }
}
