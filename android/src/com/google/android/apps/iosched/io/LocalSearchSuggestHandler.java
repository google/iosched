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
import com.google.android.apps.iosched.provider.ScheduleContract.SearchSuggest;
import com.google.android.apps.iosched.util.Lists;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.SearchManager;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;

import java.io.IOException;
import java.util.ArrayList;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

public class LocalSearchSuggestHandler extends XmlHandler {

    public LocalSearchSuggestHandler() {
        super(ScheduleContract.CONTENT_AUTHORITY);
    }

    @Override
    public ArrayList<ContentProviderOperation> parse(XmlPullParser parser, ContentResolver resolver)
            throws XmlPullParserException, IOException {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

        // Clear any existing suggestion words
        batch.add(ContentProviderOperation.newDelete(SearchSuggest.CONTENT_URI).build());

        String tag = null;
        int type;
        while ((type = parser.next()) != END_DOCUMENT) {
            if (type == START_TAG) {
                tag = parser.getName();
            } else if (type == END_TAG) {
                tag = null;
            } else if (type == TEXT) {
                final String text = parser.getText();
                if (Tags.WORD.equals(tag)) {
                    // Insert word as search suggestion
                    batch.add(ContentProviderOperation.newInsert(SearchSuggest.CONTENT_URI)
                            .withValue(SearchManager.SUGGEST_COLUMN_TEXT_1, text).build());
                }
            }
        }

        return batch;
    }

    private interface Tags {
        String WORD = "word";
    }
}
