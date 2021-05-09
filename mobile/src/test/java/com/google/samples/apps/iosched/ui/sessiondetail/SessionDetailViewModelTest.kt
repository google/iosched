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
import androidx.lifecycle.SavedStateHandle
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.androidtest.util.LiveDataTestUtil
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.TestDataRepository
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.data.prefs.PreferenceStorage
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.data.userevent.SessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.data.userevent.UserEventDataSource
import com.google.samples.apps.iosched.shared.domain.prefs.StopSnackbarActionUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.StarReserveNotificationAlarmUpdater
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.domain.users.ReservationActionUseCase
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.RequestAction
import com.google.samples.apps.iosched.shared.domain.users.StarEventAndNotifyUseCase
import com.google.samples.apps.iosched.shared.notifications.SessionAlarmManager
import com.google.samples.apps.iosched.shared.time.DefaultTimeProvider
import com.google.samples.apps.iosched.shared.time.TimeProvider
import com.google.samples.apps.iosched.shared.util.NetworkUtils
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDays
import com.google.samples.apps.iosched.test.data.CoroutineScope
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.data.TestData.sessionWithYoutubeUrl
import com.google.samples.apps.iosched.test.data.runBlockingTest
import com.google.samples.apps.iosched.test.util.fakes.FakeAnalyticsHelper
import com.google.samples.apps.iosched.test.util.fakes.FakePreferenceStorage
import com.google.samples.apps.iosched.test.util.fakes.FakeSignInViewModelDelegate
import com.google.samples.apps.iosched.test.util.fakes.FakeStarEventUseCase
import com.google.samples.apps.iosched.test.util.time.FixedTimeExecutorRule
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.reservation.RemoveReservationDialogParameters
import com.google.samples.apps.iosched.ui.schedule.TestUserEventDataSource
import com.google.samples.apps.iosched.ui.sessioncommon.DefaultOnSessionStarClickDelegate
import com.google.samples.apps.iosched.ui.sessioncommon.OnSessionStarClickDelegate
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailNavigationAction.NavigateToSignInDialogAction
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailNavigationAction.NavigateToYoutube
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailNavigationAction.RemoveReservationDialogAction
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doNothing
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.validateMockitoUsage
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.threeten.bp.Duration

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
    }

    @After
    fun tearDown() {
        validateMockitoUsage()
    }

    @Test
    fun testAnonymous_dataReady() = coroutineRule.runBlockingTest {
        // Session should be loaded without a user
        assertNotNull(viewModel.session.first { it != null })
    }

    @Test
    fun testDataIsLoaded_authReady() = coroutineRule.runBlockingTest {
        val vm = createSessionDetailViewModelWithAuthEnabled()

        assertEquals(testSession, vm.session.first())
    }

    @Test
    fun testOnPlayVideo_createsEventForVideo() = coroutineRule.runBlockingTest {
        val vm = createSessionDetailViewModelWithAuthEnabled(sessionId = sessionWithYoutubeUrl.id)

        // Observe to load a session
        vm.session.first()

        // User opens video
        vm.onPlayVideo()

        assertEquals(
            TestData.sessionWithYoutubeUrl.youTubeUrl,
            (vm.navigationActions.first() as NavigateToYoutube).videoId
        )
    }

    @Test
    fun testReserveEvent() = coroutineRule.runBlockingTest {
        val mockRepository = mock<SessionAndUserEventRepository>()
        val mockAlarmUpdater = mock<StarReserveNotificationAlarmUpdater>()
        val reservationActionUseCase = ReservationActionUseCase(
            mockRepository, mockAlarmUpdater, coroutineRule.testDispatcher
        )
        val signInDelegate = FakeSignInViewModelDelegate()
        // The session isn't reservable from one hour before the session.
        // So making now as two hours before
        val now = TestData.session0.startTime.minusHours(2).toInstant()
        val mockTime = mock<TimeProvider> {
            on { now() }.doReturn(now)
        }
        val viewModel = createSessionDetailViewModel(
            sessionId = TestData.session3.id,
            reservationActionUseCase = reservationActionUseCase,
            signInViewModelPlugin = signInDelegate,
            timeProvider = mockTime
        )
        val testUid = "testUid"
        // Kick off the viewmodel by loading a user.
        signInDelegate.loadUser(testUid)

        // Trigger data load
        viewModel.userEvent.first()
        viewModel.session.first()

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

        // TODO: Fix "This job has not completed yet"
        // assertNull(viewModel.snackBarMessage.firstOrNull())

        // Then the sign in dialog should be shown
        assertTrue(viewModel.navigationActions.first() is NavigateToSignInDialogAction)
    }

    @Test
    fun testReserveEvent_noInternet() = coroutineRule.runBlockingTest {
        // Create test use cases with test data
        val signInDelegate = FakeSignInViewModelDelegate()
        signInDelegate.injectIsSignedIn = false

        val networkUtils: NetworkUtils = mock {
            on { hasNetworkConnection() }.doReturn(false)
        }

        val snackbarMessageManager = createSnackbarMessageManager()
        val viewModel = createSessionDetailViewModel(
            networkUtils = networkUtils,
            snackbarMessageManager = snackbarMessageManager
        )

        viewModel.onReservationClicked()

        val message = snackbarMessageManager.currentSnackbar.value
        assertEquals(message?.messageId, R.string.no_network_connection)
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
            sessionId = TestData.session1.id,
            signInViewModelPlugin = signInDelegate,
            timeProvider = mockTime
        )
        val testUid = "testUid"
        // Kick off the viewmodel by loading a user.
        signInDelegate.loadUser(testUid)

        // Trigger data load
        viewModel.userEvent.first()
        viewModel.session.first()

        viewModel.onReservationClicked()

        val navigationAction = viewModel.navigationActions.first()
        assertEquals(
            RemoveReservationDialogParameters(
                testUid,
                TestData.session1.id,
                TestData.session1.title
            ),
            (navigationAction as RemoveReservationDialogAction).params
        )
    }

    @Test
    fun testStartsInTenMinutes_thenHasNullTimeUntilStart() = coroutineRule.runBlockingTest {
        val vm = createSessionDetailViewModelWithAuthEnabled()
        fixedTimeExecutorRule.time = testSession.startTime.minusMinutes(10).toInstant()
        coroutineRule.testDispatcher.advanceTimeBy(TEN_SECONDS)
        assertNull(LiveDataTestUtil.getValue(vm.timeUntilStart))
    }

    @Test
    fun testStartsIn5Minutes_thenHasDurationTimeUntilStart() = coroutineRule.runBlockingTest {
        val viewmodel = createSessionDetailViewModelWithAuthEnabled()
        fixedTimeExecutorRule.time = testSession.startTime.minusMinutes(5).toInstant()
        assertEquals(Duration.ofMinutes(5), LiveDataTestUtil.getValue(viewmodel.timeUntilStart))
    }

    /** Enable when timeUntilStart is a StateFlow again https://issuetracker.google.com/184935697
     @InternalCoroutinesApi
     @Test
     fun testStartsIn5Minutes_timeUntilStartUpdatesAfter10s() {
     // Set the initial time to T-5m
     fixedTimeExecutorRule.time = testSession.startTime.minusMinutes(5).toInstant()

     val updates = mutableListOf<Duration?>()

     coroutineRule.testDispatcher.runBlockingTest {
     val viewmodel = createSessionDetailViewModelWithAuthEnabled()

     // Start collecting updates
     val job = launch {
     viewmodel.timeUntilStart.toList(updates)
     }
     // Set the time to T-4
     fixedTimeExecutorRule.time = testSession.startTime.minusMinutes(4).toInstant()

     // Advance the time so the loop generates a new update
     advanceTimeBy(TEN_SECONDS)

     // Set the time to T+1
     fixedTimeExecutorRule.time = testSession.startTime.plusMinutes(1).toInstant()

     // Advance the time so the loop generates a new update
     advanceTimeBy(TEN_SECONDS)

     // Stop collecting
     job.cancel()
     }
     assertEquals(Duration.ofMinutes(5), updates[0])
     assertEquals(Duration.ofMinutes(4), updates[1])
     assertEquals(null, updates[2])
     }
     */

    /** TODO: Use .first() instead of LiveDataTestUtil when isReservationDeniedByCutoff is a
     * StateFlow https://issuetracker.google.com/184935697 */
    @Test
    fun testSessionStartsIn61Minutes_thenReservationIsEnabled() =
        coroutineRule.runBlockingTest {
            fixedTimeExecutorRule.time = testSession.startTime.minusMinutes(61).toInstant()
            val reservationDisabled =
                LiveDataTestUtil.getValue(viewModel.isReservationDeniedByCutoff)
            assertEquals(false, reservationDisabled)
        }

    @Test
    fun testSessionStartsIn60Minutes_thenReservationIsDisabled() =
        coroutineRule.runBlockingTest {
            fixedTimeExecutorRule.time = testSession.startTime.minusMinutes(59).toInstant()
            val reservationDisabled =
                LiveDataTestUtil.getValue(viewModel.isReservationDeniedByCutoff)
            assertTrue(reservationDisabled == true)
        }

    @Test
    fun testSessionStartsNow_thenReservationIsDisabled() =
        coroutineRule.runBlockingTest {
            fixedTimeExecutorRule.time = testSession.startTime.toInstant()
            val reservationDisabled =
                LiveDataTestUtil.getValue(viewModel.isReservationDeniedByCutoff)
            assertTrue(reservationDisabled == true)
        }

    @Test
    fun testSessionStarted1MinuteAgo_thenReservationIsDisabled() =
        coroutineRule.runBlockingTest {
            fixedTimeExecutorRule.time = testSession.startTime.plusMinutes(1).toInstant()
            val reservationDisabled =
                LiveDataTestUtil.getValue(viewModel.isReservationDeniedByCutoff)
            assertTrue(reservationDisabled == true)
        }

    // TODO: Add a test for onReservationClicked

    private fun createSessionDetailViewModelWithAuthEnabled(
        sessionId: String? = testSession.id
    ): SessionDetailViewModel {
        // If session ID and user are available, session data can be loaded
        val signInViewModelPlugin = FakeSignInViewModelDelegate()
        signInViewModelPlugin.loadUser("123")
        return createSessionDetailViewModel(
            sessionId = sessionId,
            signInViewModelPlugin = signInViewModelPlugin
        )
    }

    private fun createSessionDetailViewModel(
        sessionId: String? = testSession.id,
        signInViewModelPlugin: SignInViewModelDelegate = FakeSignInViewModelDelegate(),
        loadUserSessionUseCase: LoadUserSessionUseCase = createTestLoadUserSessionUseCase(),
        loadRelatedSessionsUseCase: LoadUserSessionsUseCase =
            createTestLoadUserSessionsUseCase(),
        reservationActionUseCase: ReservationActionUseCase = createReservationActionUseCase(),
        starEventUseCase: StarEventAndNotifyUseCase =
            FakeStarEventUseCase(coroutineRule.testDispatcher),
        getTimeZoneUseCase: GetTimeZoneUseCase = createGetTimeZoneUseCase(),
        networkUtils: NetworkUtils = mockNetworkUtils,
        timeProvider: TimeProvider = DefaultTimeProvider,
        analyticsHelper: AnalyticsHelper = FakeAnalyticsHelper(),
        snackbarMessageManager: SnackbarMessageManager = createSnackbarMessageManager(),
        isReservationEnabledByRemoteConfig: Boolean = true,
        onSessionStarClickDelegate: OnSessionStarClickDelegate = createOnSessionStarClickDelegate()
    ): SessionDetailViewModel {
        return SessionDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("session_id" to sessionId)),
            signInViewModelDelegate = signInViewModelPlugin,
            loadUserSessionUseCase = loadUserSessionUseCase,
            loadRelatedSessionUseCase = loadRelatedSessionsUseCase,
            reservationActionUseCase = reservationActionUseCase,
            getTimeZoneUseCase = getTimeZoneUseCase,
            timeProvider = timeProvider,
            networkUtils = networkUtils,
            analyticsHelper = analyticsHelper,
            snackbarMessageManager = snackbarMessageManager,
            onSessionStarClickDelegate = onSessionStarClickDelegate,
            isReservationEnabledByRemoteConfig = isReservationEnabledByRemoteConfig
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

    private fun createSnackbarMessageManager(
        preferenceStorage: PreferenceStorage = FakePreferenceStorage()
    ): SnackbarMessageManager {
        return SnackbarMessageManager(
            preferenceStorage,
            coroutineRule.CoroutineScope(),
            StopSnackbarActionUseCase(preferenceStorage, coroutineRule.testDispatcher)
        )
    }

    private fun createOnSessionStarClickDelegate(
        signInViewModelDelegate: SignInViewModelDelegate = FakeSignInViewModelDelegate(),
        starEventUseCase: StarEventAndNotifyUseCase = createStarEventUseCase(),
        snackbarMessageManager: SnackbarMessageManager = createSnackbarMessageManager(),
        analyticsHelper: AnalyticsHelper = FakeAnalyticsHelper()
    ): OnSessionStarClickDelegate {
        return DefaultOnSessionStarClickDelegate(
            signInViewModelDelegate,
            starEventUseCase,
            snackbarMessageManager,
            analyticsHelper,
            coroutineRule.CoroutineScope(),
            coroutineRule.testDispatcher
        )
    }

    private fun createStarEventUseCase() = FakeStarEventUseCase(coroutineRule.testDispatcher)
}
