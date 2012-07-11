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
import com.google.android.apps.iosched.provider.ScheduleContract.Speakers;
import com.google.android.apps.iosched.provider.ScheduleContract.Vendors;
import com.google.android.apps.iosched.util.Lists;
import com.google.android.apps.iosched.util.Maps;
import com.google.android.apps.iosched.util.ParserUtils;
import com.google.android.apps.iosched.util.WorksheetEntry;

import org.apache.http.client.methods.HttpGet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import static com.google.android.apps.iosched.util.ParserUtils.AtomTags.ENTRY;
import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

public class RemoteWorksheetsHandler extends XmlHandler {
    private static final String TAG = "WorksheetsHandler";

    private RemoteExecutor mExecutor;

    public RemoteWorksheetsHandler(RemoteExecutor executor) {
        super(ScheduleContract.CONTENT_AUTHORITY);
        mExecutor = executor;
    }

    @Override
    public ArrayList<ContentProviderOperation> parse(XmlPullParser parser, ContentResolver resolver)
            throws XmlPullParserException, IOException {
        final HashMap<String, WorksheetEntry> sheets = Maps.newHashMap();

        // walk response, collecting all known spreadsheets
        int type;
        while ((type = parser.next()) != END_DOCUMENT) {
            if (type == START_TAG && ENTRY.equals(parser.getName())) {
                final WorksheetEntry entry = WorksheetEntry.fromParser(parser);
                Log.d(TAG, "found worksheet " + entry.toString());
                sheets.put(entry.getTitle(), entry);
            }
        }

        // consider updating each spreadsheet based on update timestamp
        considerUpdate(sheets, Worksheets.SESSIONS, Sessions.CONTENT_URI, resolver);
        considerUpdate(sheets, Worksheets.SPEAKERS, Speakers.CONTENT_URI, resolver);
        considerUpdate(sheets, Worksheets.VENDORS, Vendors.CONTENT_URI, resolver);

        return Lists.newArrayList();
    }

    private void considerUpdate(HashMap<String, WorksheetEntry> sheets, String sheetName,
            Uri targetDir, ContentResolver resolver) throws HandlerException {
        final WorksheetEntry entry = sheets.get(sheetName);
        if (entry == null) {
            // Silently ignore missing spreadsheets to allow sync to continue.
            Log.w(TAG, "Missing '" + sheetName + "' worksheet data");
            return;
//            throw new HandlerException("Missing '" + sheetName + "' worksheet data");
        }

        final long localUpdated = ParserUtils.queryDirUpdated(targetDir, resolver);
        final long serverUpdated = entry.getUpdated();
        Log.d(TAG, "considerUpdate() for " + entry.getTitle() + " found localUpdated="
                + localUpdated + ", server=" + serverUpdated);
        if (localUpdated >= serverUpdated) return;

        final HttpGet request = new HttpGet(entry.getListFeed());
        final XmlHandler handler = createRemoteHandler(entry);
        mExecutor.execute(request, handler);
    }

    private XmlHandler createRemoteHandler(WorksheetEntry entry) {
        final String title = entry.getTitle();
        if (Worksheets.SESSIONS.equals(title)) {
            return new RemoteSessionsHandler();
        } else if (Worksheets.SPEAKERS.equals(title)) {
            return new RemoteSpeakersHandler();
        } else if (Worksheets.VENDORS.equals(title)) {
            return new RemoteVendorsHandler();
        } else {
            throw new IllegalArgumentException("Unknown worksheet type");
        }
    }

    interface Worksheets {
        String SESSIONS = "sessions";
        String SPEAKERS = "speakers";
        String VENDORS = "sandbox";
    }
}
