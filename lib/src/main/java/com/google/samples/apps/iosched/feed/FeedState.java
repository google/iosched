/*
 * Copyright (c) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.samples.apps.iosched.feed;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.samples.apps.iosched.lib.BuildConfig;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Holds data relevant to the state of the Feed page. Accessible globally
 * so all Activities know whether to badge the Feed bottom-nav icon.
 */
public class FeedState {

    private static final String TAG = makeLogTag(FeedState.class);
    private static FeedState sFeedState;

    /**
     * Keeps track of whether the Feed page is currently visible. This could be used for determining
     * whether to badge the Feed icon.
     */
    private boolean inFeedPage = false;

    private FeedState() {
    }

    public static FeedState getInstance() {
        if (sFeedState == null) {
            sFeedState = new FeedState();
        }
        return sFeedState;
    }

    /**
     * Sets the persistent value indicating whether there is a new Feed item.
     *
     * @param context Context to be used to edit the {@link android.content.SharedPreferences}.
     * @param newItem True if there is a new Feed item.
     */
    public void updateNewFeedItem(final Context context, final boolean newItem) {
        // If the user is on the feed page and there is a new Feed item, do not mark that there
        // is a new Feed item.
        if (!(inFeedPage && newItem)) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().putBoolean(BuildConfig.NEW_FEED_ITEM, newItem).apply();
        } else {
            LOGD(TAG, "Since the user is on the feed page, ignore the new Feed item");
        }
    }

    /**
     * Checks whether there is a new Feed item.
     *
     * @param context Context to be used to read the {@link android.content.SharedPreferences}.
     * @return True if there is a new Feed item.
     */
    public boolean isNewFeedItem(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(BuildConfig.NEW_FEED_ITEM, false);
    }

    /**
     * User has entered the Feed page;
     */
    public void enterFeedPage() {
        inFeedPage = true;
    }

    /**
     * User has exited the Feed page.
     */
    public void exitFeedPage() {
        inFeedPage = false;
    }
}
