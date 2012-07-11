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
import com.google.android.apps.iosched.provider.ScheduleContract.SyncColumns;
import com.google.android.apps.iosched.provider.ScheduleContract.Vendors;
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
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

import static com.google.android.apps.iosched.util.ParserUtils.AtomTags.ENTRY;
import static com.google.android.apps.iosched.util.ParserUtils.sanitizeId;
import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

/**
 * Handle a remote {@link XmlPullParser} that defines a set of {@link Vendors}
 * entries. Assumes that the remote source is a Google Spreadsheet.
 */
public class RemoteVendorsHandler extends XmlHandler {
    private static final String TAG = "VendorsHandler";

    public RemoteVendorsHandler() {
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

                final String vendorId = sanitizeId(entry.get(Columns.COMPANY_NAME));
                final Uri vendorUri = Vendors.buildVendorUri(vendorId);

                // Check for existing details, only update when changed
                final ContentValues values = queryVendorDetails(vendorUri, resolver);
                final long localUpdated = values.getAsLong(SyncColumns.UPDATED);
                final long serverUpdated = entry.getUpdated();
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "found vendor " + entry.toString());
                    Log.v(TAG, "found localUpdated=" + localUpdated + ", server=" + serverUpdated);
                }
                if (localUpdated >= serverUpdated) continue;

                // Clear any existing values for this vendor, treating the
                // incoming details as authoritative.
                batch.add(ContentProviderOperation.newDelete(vendorUri).build());

                final ContentProviderOperation.Builder builder = ContentProviderOperation
                        .newInsert(Vendors.CONTENT_URI);

                builder.withValue(SyncColumns.UPDATED, serverUpdated);
                builder.withValue(Vendors.VENDOR_ID, vendorId);
                builder.withValue(Vendors.VENDOR_NAME, entry.get(Columns.COMPANY_NAME));
                builder.withValue(Vendors.VENDOR_LOCATION, entry.get(Columns.COMPANY_LOCATION));
                builder.withValue(Vendors.VENDOR_DESC, entry.get(Columns.COMPANY_DESC));
                builder.withValue(Vendors.VENDOR_URL, entry.get(Columns.COMPANY_URL));
                builder.withValue(Vendors.VENDOR_LOGO_URL, entry.get(Columns.COMPANY_LOGO));
                builder.withValue(Vendors.VENDOR_PRODUCT_DESC,
                        entry.get(Columns.COMPANY_PRODUCT_DESC));

                // Inherit starred value from previous row
                if (values.containsKey(Vendors.VENDOR_STARRED)) {
                    builder.withValue(Vendors.VENDOR_STARRED,
                            values.getAsInteger(Vendors.VENDOR_STARRED));
                }

                // Assign track
                final String trackId = ParserUtils.translateTrackIdAlias(sanitizeId(entry
                        .get(Columns.COMPANY_POD)));
                builder.withValue(Vendors.TRACK_ID, trackId);

                // Normal vendor details ready, write to provider
                batch.add(builder.build());
            }
        }

        return batch;
    }

    private static ContentValues queryVendorDetails(Uri uri, ContentResolver resolver) {
        final ContentValues values = new ContentValues();
        final Cursor cursor = resolver.query(uri, VendorsQuery.PROJECTION, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                values.put(SyncColumns.UPDATED, cursor.getLong(VendorsQuery.UPDATED));
                values.put(Vendors.VENDOR_STARRED, cursor.getInt(VendorsQuery.STARRED));
            } else {
                values.put(SyncColumns.UPDATED, ScheduleContract.UPDATED_NEVER);
            }
        } finally {
            cursor.close();
        }
        return values;
    }

    private interface VendorsQuery {
        String[] PROJECTION = {
                SyncColumns.UPDATED,
                Vendors.VENDOR_STARRED,
        };

        int UPDATED = 0;
        int STARRED = 1;
    }

    /** Columns coming from remote spreadsheet. */
    private interface Columns {
        String COMPANY_NAME = "companyname";
        String COMPANY_LOCATION = "companylocation";
        String COMPANY_DESC = "companydesc";
        String COMPANY_URL = "companyurl";
        String COMPANY_PRODUCT_DESC = "companyproductdesc";
        String COMPANY_LOGO = "companylogo";
        String COMPANY_POD = "companypod";

        // company_name: 280 North, Inc.
        // company_location: San Francisco, California
        // company_desc: Creators of 280 Slides, a web based presentation
        // company_url: www.280north.com
        // company_product_desc: 280 Slides relies on the Google AJAX APIs to provide
        // company_logo: 280north.png
        // company_pod: Google APIs
        // company_tags:

    }
}
