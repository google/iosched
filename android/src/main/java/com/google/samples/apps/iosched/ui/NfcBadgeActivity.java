/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.ui;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.TaskStackBuilder;
import android.widget.Toast;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.util.AnalyticsManager;

import java.util.Arrays;

import static com.google.samples.apps.iosched.util.LogUtils.*;

public class NfcBadgeActivity extends Activity {
    private static final String TAG = makeLogTag(NfcBadgeActivity.class);
    private static final String ATTENDEE_URL_PREFIX = "\u0004plus.google.com/";

    // For debug purposes
    public static final String ACTION_SIMULATE = "com.google.samples.apps.iosched.ACTION_SIMULATE";

    @Override
    public void onStart() {
        super.onStart();
        AnalyticsManager.initializeAnalyticsTracker(getApplicationContext());
        // Check for NFC data
        Intent i = getIntent();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(i.getAction())) {
            LOGI(TAG, "Badge detected");
            /* [ANALYTICS:EVENT]
             * TRIGGER:   Scan another attendee's badge.
             * CATEGORY:  'NFC'
             * ACTION:    'Read'
             * LABEL:     'Badge'. Badge info IS NOT collected.
             * [/ANALYTICS]
             */
            AnalyticsManager.sendEvent("NFC", "Read", "Badge");
            readTag((Tag) i.getParcelableExtra(NfcAdapter.EXTRA_TAG));
        } else if (ACTION_SIMULATE.equals(i.getAction()) && Config.IS_DOGFOOD_BUILD) {
            String simulatedUrl = i.getDataString();
            LOGD(TAG, "Simulating badge scanning with URL " + simulatedUrl);
            // replace https by Unicode character 4, as per normal badge encoding rules
            recordBadge(simulatedUrl.replace("https://", "\u0004"));
        } else {
            LOGW(TAG, "Invalid action in Intent to NfcBadgeActivity: " + i.getAction());
        }
        finish();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    private void readTag(Tag t) {
        byte[] id = t.getId();

        // get NDEF tag details
        Ndef ndefTag = Ndef.get(t);

        // get NDEF message details
        NdefMessage ndefMesg = ndefTag.getCachedNdefMessage();
        if (ndefMesg == null) {
            return;
        }
        NdefRecord[] ndefRecords = ndefMesg.getRecords();
        if (ndefRecords == null) {
            return;
        }
        for (NdefRecord record : ndefRecords) {
            short tnf = record.getTnf();
            String type = new String(record.getType());
            if (tnf == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(type.getBytes(), NdefRecord.RTD_URI)) {
                String url = new String(record.getPayload());
                recordBadge(url);
            }
        }
    }

    private void recordBadge(String url) {
        LOGD(TAG, "Recording badge, URL " + url);
        if (url.startsWith(ATTENDEE_URL_PREFIX)) {
            addToHistory(url);
            Intent i = new Intent(this, PeopleIveMetActivity.class);
            TaskStackBuilder
                .create(getApplicationContext())
                .addNextIntent(i)
                .startActivities();
            return;
        } else {
            LOGD(TAG, "URL in badge (" + url + ") does not start with prefix URL ("
                    + ATTENDEE_URL_PREFIX + ")");
            LOGW(TAG, "Invalid URL in badge: " + url);
        }
    }

    private void addToHistory(String url) {
        Uri uri = Uri.parse(url);
        final String plusId = uri.getLastPathSegment();
        if (!isValidPlusId(plusId)) {
            LOGE(TAG, "Unknown ID format: " + plusId);
            return;
        }
        if (isDummyPlusId(plusId)) {
            Toast.makeText(this, R.string.people_ive_met_dummy_badge, Toast.LENGTH_SHORT).show();
            return;
        }
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Cursor cursor = null;
                try {
                    cursor = getContentResolver().query(
                            ScheduleContract.PeopleIveMet.buildPersonUri(plusId),
                            new String[]{ScheduleContract.PeopleIveMet.PERSON_ID},
                            null, null, null);
                    if (0 < cursor.getCount()) {
                        ContentValues values = new ContentValues();
                        values.put(ScheduleContract.PeopleIveMet.PERSON_TIMESTAMP,
                                System.currentTimeMillis());
                        getContentResolver().update(
                                ScheduleContract.PeopleIveMet.buildPersonUri(plusId),
                                values, null, null);
                    } else {
                        ContentValues values = new ContentValues();
                        values.put(ScheduleContract.PeopleIveMet.PERSON_ID, plusId);
                        values.put(ScheduleContract.PeopleIveMet.PERSON_TIMESTAMP,
                                System.currentTimeMillis());
                        values.put(ScheduleContract.PeopleIveMet.PERSON_IMAGE_URL, (String) null);
                        values.put(ScheduleContract.PeopleIveMet.PERSON_NAME, (String) null);
                        getContentResolver().insert(
                                ScheduleContract.PeopleIveMet.CONTENT_URI, values);
                    }
                } finally {
                    if (null != cursor) {
                        cursor.close();
                    }
                }
                return null;
            }
        }.execute();
    }

    private boolean isDummyPlusId(String plusId) {
        return plusId.startsWith("556655665566");
    }

    private boolean isValidPlusId(String id) {
        return id.matches("^([0-9]{15,}|\\+\\w+)$");
    }

}
