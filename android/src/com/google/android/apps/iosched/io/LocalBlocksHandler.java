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
import com.google.android.apps.iosched.provider.ScheduleContract.Blocks;
import com.google.android.apps.iosched.util.Lists;
import com.google.android.apps.iosched.util.ParserUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;

import java.io.IOException;
import java.util.ArrayList;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

public class LocalBlocksHandler extends XmlHandler {

    public LocalBlocksHandler() {
        super(ScheduleContract.CONTENT_AUTHORITY);
    }

    @Override
    public ArrayList<ContentProviderOperation> parse(XmlPullParser parser, ContentResolver resolver)
            throws XmlPullParserException, IOException {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

        // Clear any existing static blocks, as they may have been updated.
        final String selection = Blocks.BLOCK_TYPE + "=? OR " + Blocks.BLOCK_TYPE +"=?";
        final String[] selectionArgs = {
                ParserUtils.BLOCK_TYPE_FOOD,
                ParserUtils.BLOCK_TYPE_OFFICE_HOURS
        };
        batch.add(ContentProviderOperation.newDelete(Blocks.CONTENT_URI)
                .withSelection(selection, selectionArgs).build());

        int type;
        while ((type = parser.next()) != END_DOCUMENT) {
            if (type == START_TAG && Tags.BLOCK.equals(parser.getName())) {
                batch.add(parseBlock(parser));
            }
        }

        return batch;
    }

    private static ContentProviderOperation parseBlock(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        final int depth = parser.getDepth();
        final ContentProviderOperation.Builder builder = ContentProviderOperation
                .newInsert(Blocks.CONTENT_URI);

        String title = null;
        long startTime = -1;
        long endTime = -1;
        String blockType = null;

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
                if (Tags.TITLE.equals(tag)) {
                    title = text;
                } else if (Tags.START.equals(tag)) {
                    startTime = ParserUtils.parseTime(text);
                } else if (Tags.END.equals(tag)) {
                    endTime = ParserUtils.parseTime(text);
                } else if (Tags.TYPE.equals(tag)) {
                    blockType = text;
                }
            }
        }

        final String blockId = Blocks.generateBlockId(startTime, endTime);

        builder.withValue(Blocks.BLOCK_ID, blockId);
        builder.withValue(Blocks.BLOCK_TITLE, title);
        builder.withValue(Blocks.BLOCK_START, startTime);
        builder.withValue(Blocks.BLOCK_END, endTime);
        builder.withValue(Blocks.BLOCK_TYPE, blockType);

        return builder.build();
    }

    interface Tags {
        String BLOCK = "block";
        String TITLE = "title";
        String START = "start";
        String END = "end";
        String TYPE = "type";
    }
}
