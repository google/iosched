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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.androidtest.util.LiveDataTestUtil
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.TestDataRepository
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.data.userevent.SessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.data.userevent.UserEventDataSource
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.StarReserveNotificationAlarmUpdater
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.domain.users.ReservationActionUseCase
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.RequestAction
import com.google.samples.apps.iosched.shared.domain.users.StarEventAndNotifyUseCase
import com.google.samples.apps.iosched.shared.notifications.SessionAlarmManager
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.time.DefaultTimeProvider
import com.google.samples.apps.iosched.shared.time.TimeProvider
import com.google.samples.apps.iosched.shared.util.NetworkUtils
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDays
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.data.runBlockingTest
import com.google.samples.apps.iosched.test.util.fakes.FakeAnalyticsHelper
import com.google.samples.apps.iosched.test.util.fakes.FakePreferenceStorage
import com.google.samples.apps.iosched.test.util.fakes.FakeSignInViewModelDelegate
import com.google.samples.apps.iosched.test.util.fakes.FakeStarEventUseCase
import com.google.samples.apps.iosched.test.util.time.FixedTimeExecutorRule
import com.google.samples.apps.iosched.ui.SnackbarMessage
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.reservation.RemoveReservationDialogParameters
import com.google.samples.apps.iosched.ui.schedule.TestUserEventDataSource
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doNothing
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.validateMockitoUsage
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private const val TEN_SECONDS = 10_000L

/**
 * Unit tests for the [SessionDetailViewModel].
 */
class SessionDetailViewModelTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Allows explicit setting of "now"
    @get:Rule var fixedTimeExecutorRule = FixedTimeExecutorRule()

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var viewModel: SessionDetailViewModel
    private val testSession = TestData.session0

    private lateinit var mockNetworkUtils: NetworkUtils

    @Before
    fun setup() {
        mockNetworkUtils = mock {
            on { hasNetworkConnection() }.doReturn(true)
        }

        viewModel = createSessionDetailViewModel()
        viewModel.setSessionId(testSession.id)
    }

    @After
    fun tearDown() {
        validateMockitoUsage()
    }

    @Test
    fun testAnonymous_dataReady() = coroutineRule.runBlockingTest {
        // Even with a session ID set, data is null if no user is available
        assertNotEquals(null, LiveDataTestUtil.getValue(viewModel.session))
    }

    @Test
    fun testDataIsLoaded_authReady() = coroutineRule.runBlockingTest {
        val vm = createSessionDetailViewModelWithAuthEnabled()
        vm.setSessionId(testSession.id)

        assertEquals(testSession, LiveDataTestUtil.getValue(vm.session))
    }

    @Test
    fun testOnPlayVideo_createsEventForVideo() = coroutineRule.runBlockingTest {
        val vm = createSessionDetailViewModelWithAuthEnabled()

        vm.setSessionId(TestData.sessionWithYoutubeUrl.id)

        LiveDataTestUtil.getValue(vm.session)

        vm.onPlayVideo()
        assertEquals(
            TestData.sessionWithYoutubeUrl.youTubeUrl,
            LiveDataTestUtil.getValue(vm.navigateToYouTubeAction)?.peekContent()
        )
    }

    @Test
    fun testReserveEvent() = coroutineRule.runBlockingTest {
        val mockRepository = mock<SessionAndUserEventRepository>()
        val mockAlarmUpdater = mock<StarReserveNotificationAlarmUpdater>()
        val reservationActionUseCase = ReservationActionUseCase(
            mockRepository, mockAlarmUpdater, coroutineRule.testDispatcher)
        val signInDelegate = FakeSignInViewModelDelegate()
        // The session isn't reservable from one hour before the session.
        // So making now as two hours before
        val now = TestData.session0.startTime.minusHours(2).toInstant()
        val mockTime = mock<TimeProvider> {
            on { now() }.doReturn(now)
        }
        val viewModel = createSessionDetailViewModel(
            reservationActionUseCase = reservationActionUseCase,
            signInViewModelPlugin = signInDelegate,
            timeProvider = mockTime
        )
        val testUid = "testUid"
        // Kick off the viewmodel by loading a user.
        signInDelegate.loadUser(testUid)
        viewModel.setSessionId(TestData.session3.id)
        LiveDataTestUtil.getValue(viewModel.session)
        LiveDataTestUtil.getValue(viewModel.userEvent)
        LiveDataTestUtil.getValue(viewModel.isReservationDeniedByCutoff)

        viewModel.onReservationClicked()

        verify(mockRepository).changeReservation(
            eq(testUid), eq(TestData.session3.id), any<RequestAction>()
        )
    }

    @Test
    fun testReserveEvent_notLoggedIn() = coroutineRule.runBlockingTest {
        // Create test use cases with test data
        val signInDelegate = FakeSignInViewModelDelegate()
        signInDelegate.injectIsSignedIn = false

        val viewModel = createSessionDetailViewModel(signInViewModelPlugin = signInDelegate)

        viewModel.onReservationClicked()

        val event: Event<SnackbarMessage>? = LiveDataTestUtil.getValue(viewModel.snackBarMessage)
        // TODO change with actual resource used
        Assert.assertThat(
            event?.getContentIfNotHandled()?.messageId,
            `is`(not(equalTo(R.string.reservation_request_succeeded)))
        )

        // Then the sign in dialog should ne shown
        val signInEvent = LiveDataTestUtil.getValue(viewModel.navigateToSignInDialogAction)
        Assert.assertNotNull(signInEvent?.getContentIfNotHandled())
    }

    @Test
    fun testReserveEvent_noInternet() = coroutineRule.runBlockingTest {
        // Create test use cases with test data
        val signInDelegate = FakeSignInViewModelDelegate()
        signInDelegate.injectIsSignedIn = false

        val networkUtils: NetworkUtils = mock {
            on { hasNetworkConnection() }.doReturn(false)
        }

        val viewModel = createSessionDetailViewModel(networkUtils = networkUtils)

        viewModel.onReservationClicked()

        val event: Event<SnackbarMessage>? = LiveDataTestUtil.getValue(viewModel.snackBarMessage)
        Assert.assertThat(
            event?.getContentIfNotHandled()?.messageId,
            `is`(equalTo(R.string.no_network_connection))
        )
    }

    @Test
    fun testCancelEvent() = coroutineRule.runBlockingTest {
        val signInDelegate = FakeSignInViewModelDelegate()
        // The session isn't reservable from one hour before the session.
        // So making now as two hours before
        val now = TestData.session0.startTime.minusHours(2).toInstant()
        val mockTime = mock<TimeProvider> {
            on { now() }.doReturn(now)
        }
        val viewModel = createSessionDetailViewModel(
            signInViewModelPlugin = signInDelegate,
            timeProvider = mockTime
        )
        viewModel.setSessionId(TestData.session1.id)
        val testUid = "testUid"
        // Kick off the viewmodel by loading a user.
        signInDelegate.loadUser(testUid)
        viewModel.setSessionId(TestData.session1.id)
        LiveDataTestUtil.getValue(viewModel.session)
        LiveDataTestUtil.getValue(viewModel.userEvent)
        LiveDataTestUtil.getValue(viewModel.isReservationDeniedByCutoff)

        viewModel.onReservationClicked()

        val parameters = LiveDataTestUtil.getValue(
            viewModel.navigateToRemoveReservationDialogAction
        )
            ?.getContentIfNotHandled()
        assertThat(
            parameters, `is`(
                RemoveReservationDialogParameters(
                    testUid,
                    TestData.session1.id,
                    TestData.session1.title
                )
            )
        )
    }

    @Test
    fun testStartsInTenMinutes_thenHasNullTimeUntilStart() = coroutineRule.runBlockingTest {
        val vm = createSessionDetailViewModelWithAuthEnabled()
        fixedTimeExecutorRule.time = testSession.startTime.minusMinutes(10).toInstant()
        coroutineRule.testDispatcher.advanceTimeBy(TEN_SECONDS)
        assertEquals(null, LiveDataTestUtil.getValue(vm.timeUntilStart))
    }

//  TODO:(seanmcq) fix
//    @Test
//    fun testStartsIn5Minutes_thenHasDurationTimeUntilStart() {
//        val vm = createSessionDetailViewModelWithAuthEnabled()
//        fixedTimeExecutorRule.time = testSession.startTime.minusMinutes(5).toInstant()
//        forceTimeUntilStartIntervalUpdate(vm)
//        assertEquals(Duration.ofMinutes(5), LiveDataTestUtil.getValue(vm.timeUntilStart))
//    }

//  TODO:(seanmcq) fix
//    @Test
//    fun testStartsIn1Minutes_thenHasDurationTimeUntilStart() {
//        val vm = createSessionDetailViewModelWithAuthEnabled()
//        fixedTimeExecutorRule.time = testSession.startTime.minusMinutes(1).toInstant()
//        forceTimeUntilStartIntervalUpdate(vm)
//        vm.session.observeForever() {}
//        assertEquals(Duration.ofMinutes(1), LiveDataTestUtil.getValue(vm.timeUntilStart))
//    }

    @Test
    fun testStartsIn0Minutes_thenHasNullTimeUntilStart() = coroutineRule.runBlockingTest {
        val vm = createSessionDetailViewModelWithAuthEnabled()
        fixedTimeExecutorRule.time = testSession.startTime.minusSeconds(30).toInstant()
        coroutineRule.testDispatcher.advanceTimeBy(TEN_SECONDS)
        assertEquals(null, LiveDataTestUtil.getValue(vm.timeUntilStart))
    }

    @Test
    fun testStarts10MinutesAgo_thenHasNullTimeUntilStart() = coroutineRule.runBlockingTest {
        val vm = createSessionDetailViewModelWithAuthEnabled()
        fixedTimeExecutorRule.time = testSession.startTime.plusMinutes(10).toInstant()
        coroutineRule.testDispatcher.advanceTimeBy(TEN_SECONDS)
        assertEquals(null, LiveDataTestUtil.getValue(vm.timeUntilStart))
    }

//    TODO: (tiem) fix
//    @Test
//    fun testSessionStartsIn61Minutes_thenReservationIsNotDisabled() {
//        val vm = createSessionDetailViewModelWithAuthEnabled()
//        fixedTimeExecutorRule.time = testSession.startTime.minusMinutes(61).toInstant()
//        forceTimeUntilStartIntervalUpdate(vm)
//        assertFalse(LiveDataTestUtil.getValue(viewModel.isReservationDisabled)!!)
//    }

//    TODO: (tiem) fix
//    @Test
//    fun testSessionStartsIn60Minutes_thenReservationIsDisabled() {
//        val vm = createSessionDetailViewModelWithAuthEnabled()
//        fixedTimeExecutorRule.time = testSession.startTime.minusMinutes(60).toInstant()
//        forceTimeUntilStartIntervalUpdate(vm)
//        assertTrue(LiveDataTestUtil.getValue(viewModel.isReservationDisabled)!!)
//    }

//    TODO: (tiem) fix
//    @Test
//    fun testSessionStartsNow_thenReservationIsDisabled() {
//        val vm = createSessionDetailViewModelWithAuthEnabled()
//        fixedTimeExecutorRule.time = testSession.startTime.toInstant()
//        forceTimeUntilStartIntervalUpdate(vm)
//        assertTrue(LiveDataTestUtil.getValue(viewModel.isReservationDisabled)!!)
//    }

//    TODO: (tiem) fix
//    @Test
//    fun testSessionStarted1MinuteAgo_thenReservationIsDisabled() {
//        val vm = createSessionDetailViewModelWithAuthEnabled()
//        fixedTimeExecutorRule.time = testSession.startTime.plusMinutes(1).toInstant()
//        forceTimeUntilStartIntervalUpdate(vm)
//        assertTrue(LiveDataTestUtil.getValue(viewModel.isReservationDisabled)!!)
//    }

    @Test
    fun testOnPlayVideo_doesNotCreateEventForVideo() = coroutineRule.runBlockingTest {
        val sessionWithoutYoutubeUrl = testSession
        val vm = createSessionDetailViewModelWithAuthEnabled()

        // This loads the session and forces vm.session to be set before calling onPlayVideo
        vm.setSessionId(sessionWithoutYoutubeUrl.id)
        LiveDataTestUtil.getValue(vm.session)

        vm.onPlayVideo()
        assertNull(LiveDataTestUtil.getValue(vm.navigateToYouTubeAction))
    }

    // TODO: Add a test for onReservationClicked

    private fun createSessionDetailViewModelWithAuthEnabled(): SessionDetailViewModel {
        // If session ID and user are available, session data can be loaded
        val signInViewModelPlugin = FakeSignInViewModelDelegate()
        signInViewModelPlugin.loadUser("123")
        return createSessionDetailViewModel(signInViewModelPlugin = signInViewModelPlugin)
    }

    private fun createSessionDetailViewModel(
        signInViewModelPlugin: SignInViewModelDelegate = FakeSignInViewModelDelegate(),
        loadUserSessionUseCase: LoadUserSessionUseCase = createTestLoadUserSessionUseCase(),
        loadRelatedSessionsUseCase: LoadUserSessionsUseCase =
            createTestLoadUserSessionsUseCase(),
        reservationActionUseCase: ReservationActionUseCase = createReservationActionUseCase(),
        starEventUseCase: StarEventAndNotifyUseCase =
            FakeStarEventUseCase(coroutineRule.testDispatcher),
        getTimeZoneUseCase: GetTimeZoneUseCase = createGetTimeZoneUseCase(),
        snackbarMessageManager: SnackbarMessageManager =
            SnackbarMessageManager(FakePreferenceStorage()),
        networkUtils: NetworkUtils = mockNetworkUtils,
        timeProvider: TimeProvider = DefaultTimeProvider,
        analyticsHelper: AnalyticsHelper = FakeAnalyticsHelper(),
        isReservationEnabledByRemoteConfig: Boolean = true,
        defaultDispatcher: CoroutineDispatcher = coroutineRule.testDispatcher
    ): SessionDetailViewModel {
        return SessionDetailViewModel(
            signInViewModelPlugin, loadUserSessionUseCase, loadRelatedSessionsUseCase,
            starEventUseCase, reservationActionUseCase, getTimeZoneUseCase, snackbarMessageManager,
            timeProvider, networkUtils, analyticsHelper, isReservationEnabledByRemoteConfig,
            defaultDispatcher
        )
    }

    private fun createSessionWithUrl(youtubeUrl: String) =
        Session(
            id = "0", title = "Session 0", description = "",
            startTime = ConferenceDays.first().start,
            endTime = ConferenceDays.first().end, room = TestData.room, isLivestream = false,
            sessionUrl = "", youTubeUrl = youtubeUrl, photoUrl = "", doryLink = "",
            tags = listOf(TestData.androidTag, TestData.webTag),
            displayTags = listOf(TestData.androidTag, TestData.webTag),
            speakers = setOf(TestData.speaker1), relatedSessions = emptySet()
        )

    private fun createTestLoadUserSessionUseCase(
        userEventDataSource: UserEventDataSource = TestUserEventDataSource()
    ): LoadUserSessionUseCase {
        val sessionRepository = DefaultSessionRepository(TestDataRepository)
        val userEventRepository = DefaultSessionAndUserEventRepository(
            userEventDataSource,
            sessionRepository
        )
        return LoadUserSessionUseCase(userEventRepository, coroutineRule.testDispatcher)
    }

    private fun createTestLoadUserSessionsUseCase(
        userEventDataSource: UserEventDataSource = TestUserEventDataSource()
    ): LoadUserSessionsUseCase {
        val sessionRepository = DefaultSessionRepository(TestDataRepository)
        val userEventRepository = DefaultSessionAndUserEventRepository(
            userEventDataSource,
            sessionRepository
        )
        return LoadUserSessionsUseCase(userEventRepository, TestCoroutineDispatcher())
    }

    private fun createFakeUpdater(): StarReserveNotificationAlarmUpdater {
        val alarmManager: SessionAlarmManager = mock()
        doNothing().whenever(alarmManager).cancelAlarmForSession(any())
        return StarReserveNotificationAlarmUpdater(alarmManager)
    }

    private fun createReservationActionUseCase() = object : ReservationActionUseCase(
        DefaultSessionAndUserEventRepository(
            TestUserEventDataSource(), DefaultSessionRepository(TestDataRepository)
        ),
        createFakeUpdater(),
        coroutineRule.testDispatcher
    ) {}

    private fun createGetTimeZoneUseCase() =
        GetTimeZoneUseCase(FakePreferenceStorage(), coroutineRule.testDispatcher)
}
