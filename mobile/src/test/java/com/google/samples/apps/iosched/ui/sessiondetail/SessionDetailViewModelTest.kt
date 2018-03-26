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
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.data.userevent.UserEventDataSource
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionUseCase
import com.google.samples.apps.iosched.shared.domain.users.ReservationActionUseCase
import com.google.samples.apps.iosched.shared.domain.users.StarEventUseCase
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_1
import com.google.samples.apps.iosched.test.util.LiveDataTestUtil
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import com.google.samples.apps.iosched.test.util.fakes.FakeSignInViewModelDelegate
import com.google.samples.apps.iosched.test.util.fakes.FakeStarEventUseCase
import com.google.samples.apps.iosched.test.util.time.FixedTimeExecutorRule
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.google.samples.apps.iosched.ui.schedule.day.TestUserEventDataSource
import com.google.samples.apps.iosched.util.SetIntervalLiveData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.threeten.bp.Duration

/**
 * Unit tests for the [SessionDetailViewModel].
 */
class SessionDetailViewModelTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule var syncTaskExecutorRule = SyncTaskExecutorRule()

    // Allows explicit setting of "now"
    @get:Rule var fixedTimeExecutorRule = FixedTimeExecutorRule()

    private lateinit var viewModel: SessionDetailViewModel
    private val testSession = TestData.session0

    @Before fun setup() {
        viewModel = createSessionDetailViewModel()
        viewModel.loadSessionById(testSession.id)
    }

    @Test
    fun testDataIsLoaded_observablesUpdated() {
        assertEquals(testSession, LiveDataTestUtil.getValue(viewModel.session))
    }

    @Test
    fun testCheckPlayable_currentSessionNull() {
        assertFalse(viewModel.checkPlayable(null))
    }

    @Test
    fun testCheckPlayable_currentSessionBlankUrl() {
        assertFalse(viewModel.checkPlayable(createSessionWithUrl("  ")))
    }

    @Test
    fun testCheckPlayable_currentSessionHasUrl() {
        val rickSession = createSessionWithUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        assertTrue(viewModel.checkPlayable(rickSession))
    }

    @Test
    fun testOnPlayVideo_createsEventForVideo() {
        viewModel.loadSessionById(TestData.sessionWithYoutubeUrl.id)
        LiveDataTestUtil.getValue(viewModel.session)

        viewModel.onPlayVideo()
        assertEquals(
                TestData.sessionWithYoutubeUrl.youTubeUrl,
            LiveDataTestUtil.getValue(viewModel.navigateToYouTubeAction)?.peekContent()
        )
    }

    @Test
    fun testStartsInTenMinutes_thenHasNullTimeUntilStart() {
        fixedTimeExecutorRule.time = testSession.startTime.minusMinutes(10).toInstant()
        forceTimeUntilStartIntervalUpdate()
        assertEquals(null, LiveDataTestUtil.getValue(viewModel.timeUntilStart))
    }

    @Test
    fun testStartsIn5Minutes_thenHasDurationTimeUntilStart() {
        fixedTimeExecutorRule.time = testSession.startTime.minusMinutes(5).toInstant()
        forceTimeUntilStartIntervalUpdate()
        assertEquals(Duration.ofMinutes(5), LiveDataTestUtil.getValue(viewModel.timeUntilStart))
    }

    @Test
    fun testStartsIn1Minutes_thenHasDurationTimeUntilStart() {
        fixedTimeExecutorRule.time = testSession.startTime.minusMinutes(1).toInstant()
        forceTimeUntilStartIntervalUpdate()
        assertEquals(Duration.ofMinutes(1), LiveDataTestUtil.getValue(viewModel.timeUntilStart))
    }

    @Test
    fun testStartsIn0Minutes_thenHasNullTimeUntilStart() {
        fixedTimeExecutorRule.time = testSession.startTime.minusSeconds(30).toInstant()
        forceTimeUntilStartIntervalUpdate()
        assertEquals(null, LiveDataTestUtil.getValue(viewModel.timeUntilStart))
    }

    @Test
    fun testStarts10MinutesAgo_thenHasNullTimeUntilStart() {
        fixedTimeExecutorRule.time = testSession.startTime.plusMinutes(10).toInstant()
        forceTimeUntilStartIntervalUpdate()
        assertEquals(null, LiveDataTestUtil.getValue(viewModel.timeUntilStart))
    }

    @Test fun testOnPlayVideo_doesNotCreateEventForVideo() {
        val sessionWithoutYoutubeUrl = testSession
        val vm = createSessionDetailViewModel()

        // This loads the session and forces vm.session to be set before calling onPlayVideo
        vm.loadSessionById(sessionWithoutYoutubeUrl.id)
        LiveDataTestUtil.getValue(vm.session)

        vm.onPlayVideo()
        assertNull(LiveDataTestUtil.getValue(vm.navigateToYouTubeAction))
    }

    // TODO: Add a test for onReservationClicked


    private fun createSessionDetailViewModel(
            signInViewModelPlugin: SignInViewModelDelegate = FakeSignInViewModelDelegate(),
            loadUserSessionUseCase: LoadUserSessionUseCase = createTestLoadUserSessionUseCase(),
            reservationActionUseCase: ReservationActionUseCase = createReservationActionUseCase(),
            starEventUseCase: StarEventUseCase = FakeStarEventUseCase()
    ): SessionDetailViewModel {
        return SessionDetailViewModel(signInViewModelPlugin, loadUserSessionUseCase,
                starEventUseCase, reservationActionUseCase)
    }

    private fun forceTimeUntilStartIntervalUpdate() {
        (viewModel.timeUntilStart as SetIntervalLiveData<*, *>).updateValue()
    }

    private fun createSessionWithUrl(youtubeUrl: String) =
        Session(
            id = "0", title = "Session 0", abstract = "",
            startTime = DAY_1.start,
            endTime = DAY_1.end, room = TestData.room,
            sessionUrl = "", liveStreamUrl = "", youTubeUrl = youtubeUrl, photoUrl = "",
            tags = listOf(TestData.androidTag, TestData.webTag),
            displayTags = listOf(TestData.androidTag, TestData.webTag),
            speakers = setOf(TestData.speaker), relatedSessions = emptySet()

        )
    private fun createTestLoadUserSessionUseCase(
            userEventDataSource: UserEventDataSource = TestUserEventDataSource()
    ): LoadUserSessionUseCase {
        val sessionRepository = DefaultSessionRepository(TestDataRepository)
        val userEventRepository = DefaultSessionAndUserEventRepository(
                userEventDataSource, sessionRepository)
        return LoadUserSessionUseCase(userEventRepository)
    }

    private fun createReservationActionUseCase() = object: ReservationActionUseCase(
            DefaultSessionAndUserEventRepository(
                    TestUserEventDataSource(), DefaultSessionRepository(TestDataRepository))) {}
}
