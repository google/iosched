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

package com.google.samples.apps.iosched.ui.sessiondetail

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.model.Speaker
import com.google.samples.apps.iosched.shared.model.Tag
import org.threeten.bp.ZonedDateTime

/**
 * Loads [Session] data and exposes it to the session detail view.
 */
class SessionDetailViewModel(sessionId: String) : ViewModel() {
    private val session = MutableLiveData<Session>()
    init {
        // TODO Connect with UseCase to get data from data layer

        val androidTag = Tag(id = "1", category = "Technology", name = "Android", color = "#F30F30")

        val webTag = Tag(id = "2", category = "Technology", name = "Web", color = "#F30F30")

        val dummySpeaker = Speaker(id = "1", name = "Troy McClure", imageUrl = "",
                company = "", abstract = "", gPlusUrl = "", twitterUrl = "")

        val dummySession = Session(id = "1", startTime = ZonedDateTime.now(),
                endTime = ZonedDateTime.now().plusHours(1),
                title = "Fuchsia", abstract = "", sessionUrl = "", liveStreamUrl = "",
                youTubeUrl = "", tags = listOf(androidTag, webTag), speakers = setOf(dummySpeaker),
                photoUrl = "", relatedSessions = emptySet())

        session.value = dummySession
    }
}