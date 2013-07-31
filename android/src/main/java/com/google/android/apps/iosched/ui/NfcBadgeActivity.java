/*
 * Copyright 2013 Google Inc.
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

package com.google.android.apps.iosched.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import com.google.analytics.tracking.android.EasyTracker;

import java.util.Arrays;

import static com.google.android.apps.iosched.util.LogUtils.LOGI;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

public class NfcBadgeActivity extends Activity {
    private static final String TAG = makeLogTag(NfcBadgeActivity.class);
    private static final String ATTENDEE_URL_PREFIX = "\u0004plus.google.com/";

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getInstance().activityStart(this);
        // Check for NFC data
        Intent i = getIntent();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(i.getAction())) {
            LOGI(TAG, "Badge detected");
            EasyTracker.getTracker().sendEvent("NFC", "Read", "Badge", null);
            readTag((Tag) i.getParcelableExtra(NfcAdapter.EXTRA_TAG));
        }
        finish();
    }

    @Override
    public void onStop() {
        super.onStop();
        EasyTracker.getInstance().activityStop(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EasyTracker.getInstance().setContext(this);
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
                if (url.startsWith(ATTENDEE_URL_PREFIX)) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse("https://" + url.substring(1)));
                    startActivity(i);
                    return;
                }
            }
        }
    }
}
