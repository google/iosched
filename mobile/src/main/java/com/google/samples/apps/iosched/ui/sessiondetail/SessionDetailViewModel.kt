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

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import com.google.samples.apps.iosched.shared.model.Room
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.model.Speaker
import com.google.samples.apps.iosched.shared.model.Tag
import com.google.samples.apps.iosched.shared.util.TimeUtils
import org.threeten.bp.ZonedDateTime

/**
 * Loads [Session] data and exposes it to the session detail view.
 */
class SessionDetailViewModel(sessionId: String) : ViewModel() {
    val session: MutableLiveData<Session> = MutableLiveData()
    val timeString: LiveData<String>

    init {
        // TODO Connect with UseCase to get data from data layer

        val androidTag = Tag(id = "1", category = "TRACK", orderInCategory = 0,
                name = "Android", color = 0xFFAED581.toInt())

        val webTag = Tag(id = "2", category = "TRACK", orderInCategory = 1, name = "Web",
                color = 0xFFFFF176.toInt())

        val speakerSet = HashSet<Speaker>().apply {
            add(Speaker(id = "1", name = "Troy McClure", imageUrl = "",
                    company = "Google", abstract = "Hi I'm Troy McClure", gPlusUrl = "",
                    twitterUrl = ""))
            add(Speaker(id = "2", name = "Ally McBeal", imageUrl = "",
                    company = "Google", abstract = "Hi I'm a lawyer", gPlusUrl = "",
                    twitterUrl = ""))
            add(Speaker(id = "3", name = "Ziggy Stardust", imageUrl = "",
                    company = "Google", abstract = "Hi I'm David Bowie.", gPlusUrl = "",
                    twitterUrl = ""))
            add(Speaker(id = "4", name = "Tiem Song", imageUrl = "",
                    company = "Google", abstract = "Hi I'm an Android DPE", gPlusUrl = "",
                    twitterUrl = ""))
            add(Speaker(id = "5", name = "Lyla Fujiwara", imageUrl = "",
                    company = "Google", abstract = "Hi I'm an Android DA", gPlusUrl = "",
                    twitterUrl = ""))
        }

        val room = Room(id = "1", name = "Tent 1", capacity = 40)

        val dummySession = Session(id = "1", startTime = ZonedDateTime.now(),
                endTime = ZonedDateTime.now().plusHours(1),
                title = "Fuchsia", abstract = "Come learn about the hottest, newest OS",
                room = room, sessionUrl = "", liveStreamUrl = "",
                youTubeUrl = "", tags = listOf(androidTag, webTag), speakers = speakerSet,
                photoUrl = "", relatedSessions = emptySet())

        session.value = dummySession

        timeString = Transformations.map(session, { ses ->
            TimeUtils.timeString(ses.startTime, ses.endTime)
        })
    }
}
