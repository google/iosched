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

package com.google.android.apps.iosched.util;

import com.google.android.apps.iosched.R;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;

/**
 * Helper class for the Catch Notes integration, based on example code at
 * {@link https://github.com/catch/docs-api/}.
 */
public class CatchNotesHelper {
    private static final String TAG = "CatchNotesHelper";

    // Intent actions
    public static final String ACTION_ADD = "com.catchnotes.intent.action.ADD";
    public static final String ACTION_VIEW = "com.catchnotes.intent.action.VIEW";

    // Intent extras for ACTION_ADD
    public static final String EXTRA_SOURCE = "com.catchnotes.intent.extra.SOURCE";
    public static final String EXTRA_SOURCE_URL = "com.catchnotes.intent.extra.SOURCE_URL";

    // Intent extras for ACTION_VIEW
    public static final String EXTRA_VIEW_FILTER = "com.catchnotes.intent.extra.VIEW_FILTER";

    // Note: "3banana" was the original name of Catch Notes. Though it has been
    // rebranded, the package name must persist.
    private static final String NOTES_PACKAGE_NAME = "com.threebanana.notes";
    private static final String NOTES_MARKET_URI = "http://market.android.com/details?id="
            + NOTES_PACKAGE_NAME;

    private static final int NOTES_MIN_VERSION_CODE = 54;

    private final Context mContext;

    public CatchNotesHelper(Context context) {
        mContext = context;
    }

    public Intent createNoteIntent(String message) {
        if (!isNotesInstalledAndMinimumVersion()) {
            return notesMarketIntent();
        }

        Intent intent = new Intent();
        intent.setAction(ACTION_ADD);
        intent.putExtra(Intent.EXTRA_TEXT, message);
        intent.putExtra(EXTRA_SOURCE, mContext.getString(R.string.app_name));
        intent.putExtra(EXTRA_SOURCE_URL, "http://www.google.com/io/");
        intent.putExtra(Intent.EXTRA_TITLE, mContext.getString(R.string.app_name));
        return intent;
    }

    public Intent viewNotesIntent(String tag) {
        if (!isNotesInstalledAndMinimumVersion()) {
            return notesMarketIntent();
        }

        if (!tag.startsWith("#")) {
            tag = "#" + tag;
        }

        Intent intent = new Intent();
        intent.setAction(ACTION_VIEW);
        intent.putExtra(EXTRA_VIEW_FILTER, tag);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    /**
     * Returns the installation status of Catch Notes.
     */
    public boolean isNotesInstalledAndMinimumVersion() {
        try {
            PackageInfo packageInfo = mContext.getPackageManager()
                    .getPackageInfo(NOTES_PACKAGE_NAME, PackageManager.GET_ACTIVITIES);

            if (packageInfo.versionCode < NOTES_MIN_VERSION_CODE) {
                return false;
            }
        } catch (NameNotFoundException e) {
            return false;
        }

        return true;
    }

    public Intent notesMarketIntent() {
        Uri uri = Uri.parse(NOTES_MARKET_URI);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        return intent;
    }
}
