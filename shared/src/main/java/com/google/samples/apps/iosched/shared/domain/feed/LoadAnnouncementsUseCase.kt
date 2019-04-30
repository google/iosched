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

package com.google.samples.apps.iosched.shared.domain.feed

import com.google.samples.apps.iosched.model.Announcement
import com.google.samples.apps.iosched.shared.data.feed.FeedRepository
import com.google.samples.apps.iosched.shared.domain.UseCase
import com.google.samples.apps.iosched.shared.time.TimeProvider
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import javax.inject.Inject

/**
 * Loads all feed items into a list.
 */
open class LoadAnnouncementsUseCase @Inject constructor(
    private val repository: FeedRepository,
    private val timeProvider: TimeProvider
) : UseCase<Unit, List<Announcement>>() {

    override fun execute(parameters: Unit): List<Announcement> {
        val announcements = repository.getAnnouncements()
        val now = ZonedDateTime.ofInstant(timeProvider.now(), ZoneId.systemDefault())
        return announcements.filter {
            now.isAfter(it.timestamp)
        }
    }
}
