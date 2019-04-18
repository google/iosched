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

package com.google.samples.apps.iosched.shared.data

import com.google.samples.apps.iosched.model.Announcement
import com.google.samples.apps.iosched.shared.data.feed.AnnouncementDataSource
import com.google.samples.apps.iosched.shared.util.TimeUtils

/**
 * Returns hardcoded data for development and testing.
 */
object FakeAnnouncementDataSource : AnnouncementDataSource {
    private val feedItem1 = Announcement(id = "0", title = "Item 1", message = "First item",
            timestamp = TimeUtils.ConferenceDays[0].start, imageUrl = "", color = 0,
            category = "", priority = false, emergency = false)

    private val feedItem2 = Announcement(id = "1", title = "Item 2", message = "Second item",
            timestamp = TimeUtils.ConferenceDays[0].end, imageUrl = "", color = 0,
            category = "", priority = true, emergency = true)

    private val feedItem3 = Announcement(id = "2", title = "Item 3", message = "Third item",
            timestamp = TimeUtils.ConferenceDays[1].start, imageUrl = "", color = 0,
            category = "", priority = false, emergency = false)

    private val feedItem4 = Announcement(id = "3", title = "Item 4", message = "Fourth item",
            timestamp = TimeUtils.ConferenceDays[1].end, imageUrl = "", color = 0,
            category = "", priority = false, emergency = false)

    private val feed = listOf(feedItem1, feedItem2, feedItem3, feedItem4)

    override fun getAnnouncements(): List<Announcement> = feed
}
