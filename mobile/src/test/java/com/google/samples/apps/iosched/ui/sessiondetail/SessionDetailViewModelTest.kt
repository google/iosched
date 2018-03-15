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

@file:Suppress("FunctionName")

package com.google.samples.apps.iosched.ui.sessiondetail

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.iosched.model.TestData
import com.google.samples.apps.iosched.model.TestDataRepository
import com.google.samples.apps.iosched.shared.data.session.SessionRepository
import com.google.samples.apps.iosched.shared.domain.sessions.LoadSessionUseCase
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.test.util.LiveDataTestUtil
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for the [SessionDetailViewModel].
 */
class SessionDetailViewModelTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule var syncTaskExecutorRule = SyncTaskExecutorRule()

    private lateinit var viewModel: SessionDetailViewModel
    private val testSession = TestData.session0

    @Before fun setup() {
        viewModel = SessionDetailViewModel(createUseCase(testSession))
        viewModel.loadSessionById(testSession.id)
    }

    @Test fun testDataIsLoaded_observablesUpdated() {
        assertEquals(testSession, LiveDataTestUtil.getValue(viewModel.session))
    }

    @Test fun testCheckPlayable_currentSessionNull() {
        assertFalse(viewModel.checkPlayable(null))
    }

    @Test fun testCheckPlayable_currentSessionBlankUrl() {
        assertFalse(viewModel.checkPlayable(createSessionWithUrl("  ")))
    }

    @Test fun testCheckPlayable_currentSessionHasUrl() {
        val rickSession = createSessionWithUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        assertTrue(viewModel.checkPlayable(rickSession))
    }

    @Test fun testOnPlayVideo_createsEventForVideo() {
        val sessionWithUrl = createSessionWithUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        val vm = SessionDetailViewModel(createUseCase(sessionWithUrl))

        // This loads the session and forces vm.session to be set before calling onPlayVideo
        vm.loadSessionById(sessionWithUrl.id)
        LiveDataTestUtil.getValue(vm.session)

        vm.onPlayVideo()
        assertEquals(sessionWithUrl.youTubeUrl,
                LiveDataTestUtil.getValue(vm.navigateToYouTubeAction)?.peekContent())
    }

    @Test fun testOnPlayVideo_doesNotcreateEventForVideo() {
        val sessionWithUrl = createSessionWithUrl("")
        val vm = SessionDetailViewModel(createUseCase(sessionWithUrl))

        // This loads the session and forces vm.session to be set before calling onPlayVideo
        vm.loadSessionById(sessionWithUrl.id)
        LiveDataTestUtil.getValue(vm.session)

        vm.onPlayVideo()
        assertNull(LiveDataTestUtil.getValue(vm.navigateToYouTubeAction))
    }

    /**
     * Creates a use case that will return the provided session.
     */
    private fun createUseCase(session: Session): LoadSessionUseCase {
        return object : LoadSessionUseCase(SessionRepository(TestDataRepository)) {
            override fun execute(parameters: String) = session
        }
    }

    private fun createSessionWithUrl(youtubeUrl: String) =
            Session(id = "0", title = "Session 0", abstract = "",
                    startTime = TimeUtils.ConferenceDay.DAY_1.start,
                    endTime = TimeUtils.ConferenceDay.DAY_1.end, room = TestData.room,
                    sessionUrl = "", liveStreamUrl = "", youTubeUrl = youtubeUrl, photoUrl = "",
                    tags = listOf(TestData.androidTag, TestData.webTag),
                    speakers = setOf(TestData.speaker), relatedSessions = emptySet())
}
