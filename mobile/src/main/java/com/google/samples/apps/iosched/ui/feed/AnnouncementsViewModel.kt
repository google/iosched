/*
 * Copyright 2020 Google LLC
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

package com.google.samples.apps.iosched.ui.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.samples.apps.iosched.model.Announcement
import com.google.samples.apps.iosched.shared.domain.feed.LoadAnnouncementsUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCaseLegacy
import com.google.samples.apps.iosched.shared.result.Result.Loading
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.successOr
import com.google.samples.apps.iosched.shared.util.map
import com.google.samples.apps.iosched.shared.time.TimeProvider
import com.google.samples.apps.iosched.shared.util.TimeUtils
import org.threeten.bp.ZoneId
import javax.inject.Inject

class AnnouncementsViewModel @Inject constructor(
    loadAnnouncementsUseCase: LoadAnnouncementsUseCase,
    getTimeZoneUseCaseLegacy: GetTimeZoneUseCaseLegacy, // TODO(COROUTINES): Migrate to non-legacy
    timeProvider: TimeProvider
) : ViewModel() {
    val announcements: LiveData<List<Any>>
    val timeZoneId: LiveData<ZoneId>

    init {
        val preferConferenceTimeZoneResult = MutableLiveData<Result<Boolean>>()
        getTimeZoneUseCaseLegacy(Unit, preferConferenceTimeZoneResult)
        timeZoneId = preferConferenceTimeZoneResult.map {
            if (it.successOr(true)) TimeUtils.CONFERENCE_TIMEZONE else ZoneId.systemDefault()
        }

        val loadAnnouncementsResult = MutableLiveData<Result<List<Announcement>>>()
        loadAnnouncementsUseCase(timeProvider.now(), loadAnnouncementsResult)
        announcements = loadAnnouncementsResult.map {
            if (it is Loading) {
                listOf(LoadingIndicator)
            } else {
                val items = it.successOr(emptyList())
                if (items.isNotEmpty()) items else listOf(AnnouncementsEmpty)
            }
        }
    }
}
