/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.shared.analytics

import android.app.Activity

/** Analytics API surface */
interface AnalyticsHelper {
    /** Record a screen view */
    fun sendScreenView(screenName: String, activity: Activity)

    /** Record a UI event, e.g. user clicks a button */
    fun logUiEvent(itemId: String, action: String)

    /** Set the user signed in property */
    fun setUserSignedIn(isSignedIn: Boolean)
}

/** Actions that should be used when sending analytics events */
object AnalyticsActions {
    // UI Actions
    const val STARRED = "Bookmarked"
    const val CLICK = "Clicked"
    const val FEEDBACK = "Provided Feedback"
    const val ADD_EVENT = "Added Event"

    const val UPDATE_FILTERS = "Updated Filters"
    const val MAP_MARKER_SELECT = "Selected Map Marker"
    const val MAP_MARKER_DETAILS = "Viewed Map Marker Details"
    const val WIFI_CONNECT = "Connected to Wifi"
    const val FILTERS_UPDATED = "Updated filters"
    const val YOUTUBE_LINK = "Youtube link click"
    const val SHARE = "Share"
    const val SEARCH_QUERY_SUBMIT = "Submitted search query"
    const val SEARCH_RESULT_CLICK = "Clicked on search result"

    const val USER_ATTENDEE_DIALOG_NOT_SHOWN = "Bypassed user attendee dialog"

    // Settings Actions
    const val ENABLE = "Enabled"
    const val DISABLE = "Disabled"
}
