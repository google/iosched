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

package com.google.samples.apps.iosched.shared.data.userevent

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.iosched.androidtest.util.LiveDataTestUtil
import com.google.samples.apps.iosched.model.ConferenceDay
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.domain.repository.TestUserEventDataSource
import com.google.samples.apps.iosched.shared.model.TestDataRepository
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.SyncExecutorRule
import com.google.samples.apps.iosched.test.data.TestData
import org.hamcrest.Matchers.equalTo
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsInstanceOf
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test

/**
 * Unit test for [DefaultSessionAndUserEventRepository].
 */
class DefaultSessionAndUserEventRepositoryTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule
    var syncExecutorRule = SyncExecutorRule()

    @Test
    fun observableUserEvents_areMappedCorrectly() {
        val repository = object : DefaultSessionAndUserEventRepository(
            userEventDataSource = TestUserEventDataSource(),
            sessionRepository = DefaultSessionRepository(TestDataRepository)
        ) {
            override fun getConferenceDays(): List<ConferenceDay> = TestData.TestConferenceDays
        }

        val userEvents = LiveDataTestUtil.getValue(repository.getObservableUserEvents("user"))

        assertThat(userEvents, `is`(IsInstanceOf(Result.Success::class.java)))

        assertThat(
            (userEvents as Result.Success).data.userSessionsPerDay.keys.size,
            `is`(equalTo(TestData.sessionsMap.keys.size))
        )

        val sessionsFirstDay: List<UserSession>? =
            userEvents.data.userSessionsPerDay[TestData.TestConferenceDays.first()]

        // Starred session
        assertThat(
            sessionsFirstDay?.get(0)?.userEvent?.isStarred,
            `is`(equalTo(TestData.userEvents[0].isStarred))
        )

        // Non-starred session
        assertThat(
            sessionsFirstDay?.get(1)?.userEvent?.isStarred,
            `is`(equalTo(TestData.userEvents[1].isStarred))
        )

        // Session info gets merged too
        assertThat(sessionsFirstDay?.get(0)?.session, `is`(equalTo(TestData.session0)))
    }

    // TODO: Test error cases

    // TODO: Test updateIsStarred

    // TODO: Test changeReservation

    // TODO: mapUserDataAndSessions with allDataSynced = true

    // TODO: mapUserDataAndSessions with Result.Error

    // TODO: mapUserDataAndSessions are sorted

    // TODO: Test changeReservation returns SwapAction
}
