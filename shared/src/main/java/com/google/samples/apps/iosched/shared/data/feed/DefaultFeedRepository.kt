/*
 * Copyright 2019 Google LLC
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

package com.google.samples.apps.iosched.shared.data.feed

import com.google.samples.apps.iosched.model.Announcement
import com.google.samples.apps.iosched.model.Moment
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single point of access to feed data for the presentation layer.
 */
interface FeedRepository {
    fun getAnnouncements(): List<Announcement>
    fun getMoments(): List<Moment>
}

@Singleton
open class DefaultFeedRepository @Inject constructor(
    private val announcementDataSource: AnnouncementDataSource,
    private val momentDataSource: MomentDataSource
) : FeedRepository {

    override fun getAnnouncements(): List<Announcement> = announcementDataSource.getAnnouncements()

    override fun getMoments(): List<Moment> = momentDataSource.getMoments()
}
