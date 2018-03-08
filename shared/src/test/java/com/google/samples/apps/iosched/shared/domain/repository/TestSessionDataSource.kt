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

package com.google.samples.apps.iosched.shared.domain.repository

import com.google.samples.apps.iosched.shared.data.ConferenceDataSource
import com.google.samples.apps.iosched.shared.model.ConferenceData
import com.google.samples.apps.iosched.shared.model.TestData.androidTag
import com.google.samples.apps.iosched.shared.model.TestData.session0
import com.google.samples.apps.iosched.shared.model.TestData.session1
import com.google.samples.apps.iosched.shared.model.TestData.session2
import com.google.samples.apps.iosched.shared.model.TestData.session3
import com.google.samples.apps.iosched.shared.model.TestData.speaker
import com.google.samples.apps.iosched.shared.model.TestData.webTag

/**
 * Generates dummy session data to be used in tests.
 */
object TestSessionDataSource : ConferenceDataSource {
    override fun getConferenceData(): ConferenceData? {
        return conferenceData
    }

    override fun getOfflineConferenceData(): ConferenceData? {
        return conferenceData
    }

    private val conferenceData = ConferenceData(
            sessions = listOf(session0, session1, session2, session3),
            tags = listOf(androidTag, webTag),
            blocks = emptyList(),
            speakers = listOf(speaker),
            rooms = emptyList(),
            version = 42
    )
}
